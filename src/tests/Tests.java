package tests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exceptions.NotEnoughMemory;
import exceptions.PolicyNotDetected;
import models.Controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class Tests {

    Controller controller = new Controller(4, 1024);

    @BeforeEach
    public void setUp() throws FileNotFoundException, IOException {
        Function<Map<String, Integer>, Integer> f;
        f = x -> x.get("x") + x.get("y");
        controller.registerAction("addAction", f, 256);
        f = x -> x.get("x") - x.get("y");
        controller.registerAction("subAction", f, 256);
        f = x -> x.get("x") * x.get("y");
        controller.registerAction("multAction", f, 256);
        f = x -> x.get("x") / x.get("y");
        controller.registerAction("divAction", f, 256);
        f = x -> (x.get("x") + x.get("y")) * 2;
        controller.registerAction("addX2Action", f, 1024);
        f = x -> (x.get("x") + x.get("y")) / 2;
        controller.registerAction("add/2Action", f, 64);
    }

    @Test
    public void funcSolo() throws NotEnoughMemory, PolicyNotDetected {
        int res = (int) controller.invoke("addAction", Map.of("x", 6, "y", 2), 1);
        assertEquals(8, res);

        res = (int) controller.invoke("subAction", Map.of("x", 6, "y", 2), 2);
        assertEquals(4, res);

        res = (int) controller.invoke("multAction", Map.of("x", 6, "y", 2), 1);
        assertEquals(12, res);

        res = (int) controller.invoke("divAction", Map.of("x", 6, "y", 2), 2);
        assertEquals(3, res);
    }

    @Test
    public void throwsNEMSolo() throws NotEnoughMemory, PolicyNotDetected {
        assertThrows(NotEnoughMemory.class, () -> {
            int res = (int) controller.invoke("addX2Action", Map.of("x", 6, "y", 2), 2);
        });
    }

    @Test
    public void funcGroup() throws NotEnoughMemory, PolicyNotDetected {
        List<Map<String, Integer>> input = Arrays.asList(
                new HashMap<String, Integer>() {
                    {
                        put("x", 2);
                        put("y", 3);
                    }
                },
                new HashMap<String, Integer>() {
                    {
                        put("x", 9);
                        put("y", 1);
                    }
                },
                new HashMap<String, Integer>() {
                    {
                        put("x", 8);
                        put("y", 8);
                    }
                });
        List<Integer> result = controller.invoke("addAction", input, 2);

        assertEquals(5, result.get(0));
        assertEquals(10, result.get(1));
        assertEquals(16, result.get(2));
    }

    @Test
    public void throwsNEMGroup() throws NotEnoughMemory, PolicyNotDetected {
        List<Map<String, Integer>> input = Arrays.asList(
                new HashMap<String, Integer>() {
                    {
                        put("x", 2);
                        put("y", 3);
                    }
                },
                new HashMap<String, Integer>() {
                    {
                        put("x", 9);
                        put("y", 1);
                    }
                },
                new HashMap<String, Integer>() {
                    {
                        put("x", 24);
                        put("y", 3);
                    }
                },
                new HashMap<String, Integer>() {
                    {
                        put("x", 1);
                        put("y", 1);
                    }
                },
                new HashMap<String, Integer>() {
                    {
                        put("x", 8);
                        put("y", 8);
                    }
                });
        assertThrows(NotEnoughMemory.class, () -> {
            List<Integer> res = controller.invoke("addAction", input, 2); // 4 invokers no tenen prou mem
        });
    }

    @Test
    public void OneInvokerExecsGroup() throws NotEnoughMemory, PolicyNotDetected {
        Controller controller2 = new Controller(1, 1024);
        Function<Map<String, Integer>, Integer> f = x -> x.get("x") + x.get("y");
        controller2.registerAction("addAction", f, 256);

        List<Map<String, Integer>> input = Arrays.asList(
                new HashMap<String, Integer>() {
                    {
                        put("x", 2);
                        put("y", 3);
                    }
                },
                new HashMap<String, Integer>() {
                    {
                        put("x", 9);
                        put("y", 1);
                    }
                },
                new HashMap<String, Integer>() {
                    {
                        put("x", 24);
                        put("y", 3);
                    }
                },
                new HashMap<String, Integer>() {
                    {
                        put("x", 1);
                        put("y", 1);
                    }
                });
        List<Integer> res = controller2.invoke("addAction", input, 2); // 1 invoker fa 4 execs
        assertEquals(5, res.get(0));
        assertEquals(10, res.get(1));
        assertEquals(27, res.get(2));
        assertEquals(2, res.get(3));

    }

    @Test
    public void roundRobin() throws NotEnoughMemory, PolicyNotDetected {
        List<Map<String, Integer>> input = Arrays.asList(
                new HashMap<String, Integer>() {
                    {
                        put("x", 2);
                        put("y", 3);
                    }
                },
                new HashMap<String, Integer>() {
                    {
                        put("x", 9);
                        put("y", 1);
                    }
                },
                new HashMap<String, Integer>() {
                    {
                        put("x", 5);
                        put("y", 5);
                    }
                },
                new HashMap<String, Integer>() {
                    {
                        put("x", 6);
                        put("y", 2);
                    }
                },
                new HashMap<String, Integer>() {
                    {
                        put("x", 9);
                        put("y", 3);
                    }
                });
        List<Integer> result = controller.invoke("add/2Action", input, 1);

        assertEquals(2, result.get(0));
        assertEquals(5, result.get(1));
        assertEquals(5, result.get(2));
        assertEquals(4, result.get(3));
        assertEquals(6, result.get(4));

        // funcions s'han repartit uniformement entre els 4 invokers
        assertEquals(2, controller.getInvokers()[0].getExecFuncs());
        assertEquals(1, controller.getInvokers()[1].getExecFuncs());
        assertEquals(1, controller.getInvokers()[2].getExecFuncs());
        assertEquals(1, controller.getInvokers()[3].getExecFuncs());
    }

}
