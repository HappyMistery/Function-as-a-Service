package tests;

import org.junit.jupiter.api.*;

import exceptions.*;
import models.Controller;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
public class InvokersTests {

    Controller controller = new Controller(4, 1024);

    @BeforeEach
    public void setUp() {
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

    @AfterAll
    public void closing() {
        for(int i = 0; i < controller.getNInvokers(); i++) {
            controller.getInvokers()[i].getES().shutdown();
        }
    }

    @Test
    public void funcSolo() throws NotEnoughMemory, PolicyNotDetected, InterruptedException {
        int res = (int) controller.invoke("addAction", Map.of("x", 6, "y", 2), 1);
        assertEquals(8, res);
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[0].getAvailableMem());

        res = (int) controller.invoke("subAction", Map.of("x", 6, "y", 2), 2);
        assertEquals(4, res);
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[0].getAvailableMem());

        res = (int) controller.invoke("multAction", Map.of("x", 6, "y", 2), 3);
        assertEquals(12, res);
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[0].getAvailableMem());

        res = (int) controller.invoke("divAction", Map.of("x", 6, "y", 2), 4);
        assertEquals(3, res);
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[0].getAvailableMem());
    }

    @Test
    public void throwsNEMSolo() throws NotEnoughMemory, PolicyNotDetected {
        assertThrows(NotEnoughMemory.class, () -> {
            controller.invoke("addX2Action", Map.of("x", 6, "y", 2), 2);
        });
    }

    @Test
    public void funcGroup() throws NotEnoughMemory, PolicyNotDetected, InterruptedException {
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
        List<Integer> result = controller.invoke("addAction", input, 1);

        assertEquals(5, result.get(0));
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[0].getAvailableMem());
        assertEquals(10, result.get(1));
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[1].getAvailableMem());
        assertEquals(16, result.get(2));
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[2].getAvailableMem());
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
            controller.invoke("addAction", input, 2); // 4 invokers no tenen prou mem
        });
    }

    @Test
    public void OneInvokerExecsGroup() throws NotEnoughMemory, PolicyNotDetected, InterruptedException {
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
        List<Integer> res = controller2.invoke("addAction", input, 1); // 1 invoker fa 4 execs
        assertEquals(5, res.get(0));
        assertEquals(controller2.getTotalSizeMB()/controller2.getNInvokers(), controller2.getInvokers()[0].getAvailableMem());
        assertEquals(10, res.get(1));
        assertEquals(controller2.getTotalSizeMB()/controller2.getNInvokers(), controller2.getInvokers()[0].getAvailableMem());
        assertEquals(27, res.get(2));
        assertEquals(controller2.getTotalSizeMB()/controller2.getNInvokers(), controller2.getInvokers()[0].getAvailableMem());
        assertEquals(2, res.get(3));
        assertEquals(controller2.getTotalSizeMB()/controller2.getNInvokers(), controller2.getInvokers()[0].getAvailableMem());
    }


     @Test
    public void funcSoloAsync() throws NotEnoughMemory, PolicyNotDetected, InterruptedException, ExecutionException {
        Future<Integer> res = controller.invoke_async("addAction", Map.of("x", 6, "y", 2), 1);
        assertEquals(8, res.get());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[0].getAvailableMem());
        
        res = controller.invoke_async("subAction", Map.of("x", 6, "y", 2), 2);
        assertEquals(4, res.get());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[0].getAvailableMem());

        res = controller.invoke_async("multAction", Map.of("x", 6, "y", 2), 3);
        assertEquals(12, res.get());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[0].getAvailableMem());

        res = controller.invoke_async("divAction", Map.of("x", 6, "y", 2), 4);
        assertEquals(3, res.get());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[0].getAvailableMem());
    }

    @Test
    public void funcGroupAsync() throws NotEnoughMemory, PolicyNotDetected, InterruptedException, ExecutionException {
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
        Future<Integer> result = controller.invoke_async("addAction", input, 2);

        assertEquals(Arrays.asList(5, 10, 16), result.get());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[0].getAvailableMem());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[1].getAvailableMem());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[2].getAvailableMem());
    }
}
