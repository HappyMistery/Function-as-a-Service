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

public class PolicyManagerTests {

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
    public void testRoundRobin() throws NotEnoughMemory, PolicyNotDetected {
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
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[0].getAvailableMem());
        assertEquals(1, controller.getInvokers()[1].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[1].getAvailableMem());
        assertEquals(1, controller.getInvokers()[2].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[2].getAvailableMem());
        assertEquals(1, controller.getInvokers()[3].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[3].getAvailableMem());
    }

    @Test
    public void testGreedyGroup() throws NotEnoughMemory, PolicyNotDetected {
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
        List<Integer> result = controller.invoke("add/2Action", input, 2);

        assertEquals(2, result.get(0));
        assertEquals(5, result.get(1));
        assertEquals(5, result.get(2));
        assertEquals(4, result.get(3));
        assertEquals(6, result.get(4));

        //els invokers s'han emplenat abans de passar al següent
        assertEquals(4, controller.getInvokers()[0].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[0].getAvailableMem());
        assertEquals(1, controller.getInvokers()[1].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[1].getAvailableMem());
        assertEquals(0, controller.getInvokers()[2].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[2].getAvailableMem());
        assertEquals(0, controller.getInvokers()[3].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[3].getAvailableMem());
    }

    @Test
    public void testUniformGroup() throws NotEnoughMemory, PolicyNotDetected {
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
                        put("x", 19);
                        put("y", 1);
                    }
                },
                new HashMap<String, Integer>() {
                    {
                        put("x", 0);
                        put("y", 9);
                    }
                },
                new HashMap<String, Integer>() {
                    {
                        put("x", 4);
                        put("y", 8);
                    }
                },
                new HashMap<String, Integer>() {
                    {
                        put("x", 9);
                        put("y", 3);
                    }
                });
        List<Integer> result = controller.invoke("add/2Action", input, 3);

        assertEquals(2, result.get(0));
        assertEquals(5, result.get(1));
        assertEquals(5, result.get(2));
        assertEquals(4, result.get(3));
        assertEquals(10, result.get(4));
        assertEquals(4, result.get(5));
        assertEquals(6, result.get(6));
        assertEquals(6, result.get(7));

        //els invokers reben grups de 3 en 3 (si es poden fer grups de 3) funcions fins que no queden funcions per executar
        assertEquals(3, controller.getInvokers()[0].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[0].getAvailableMem());
        assertEquals(3, controller.getInvokers()[1].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[1].getAvailableMem());
        assertEquals(2, controller.getInvokers()[2].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[2].getAvailableMem());
        assertEquals(0, controller.getInvokers()[3].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[3].getAvailableMem());
    }

    @Test
    public void testBigGroup() throws NotEnoughMemory, PolicyNotDetected {
        Controller controller2 = new Controller(4, 2048);
        Function<Map<String, Integer>, Integer> f = x -> (x.get("x") + x.get("y")) / 2;
        controller2.registerAction("add/2Action", f, 64);
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
                        put("x", 19);
                        put("y", 1);
                    }
                },
                new HashMap<String, Integer>() {
                    {
                        put("x", 0);
                        put("y", 9);
                    }
                },
                new HashMap<String, Integer>() {
                    {
                        put("x", 4);
                        put("y", 8);
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
                        put("x", 12);
                        put("y", 3);
                    }
                },
                new HashMap<String, Integer>() {
                    {
                        put("x", 5);
                        put("y", 6);
                    }
                },
                new HashMap<String, Integer>() {
                    {
                        put("x", 9);
                        put("y", 3);
                    }
                });
        List<Integer> result = controller2.invoke("add/2Action", input, 4);

        assertEquals(2, result.get(0));
        assertEquals(5, result.get(1));
        assertEquals(5, result.get(2));
        assertEquals(4, result.get(3));
        assertEquals(10, result.get(4));
        assertEquals(4, result.get(5));
        assertEquals(6, result.get(6));
        assertEquals(5, result.get(7));
        assertEquals(7, result.get(8));
        assertEquals(5, result.get(9));
        assertEquals(6, result.get(10));

        //els invokers es van omplint de grups de 3 en 3 (si es poden fer grups de 3) funcions abans de passar al següent invoker
        assertEquals(6, controller2.getInvokers()[0].getExecFuncs());
        assertEquals(controller2.getTotalSizeMB()/controller2.getNInvokers(), controller2.getInvokers()[0].getAvailableMem());
        assertEquals(5, controller2.getInvokers()[1].getExecFuncs());
        assertEquals(controller2.getTotalSizeMB()/controller2.getNInvokers(), controller2.getInvokers()[1].getAvailableMem());
        assertEquals(0, controller2.getInvokers()[2].getExecFuncs());
        assertEquals(controller2.getTotalSizeMB()/controller2.getNInvokers(), controller2.getInvokers()[2].getAvailableMem());
        assertEquals(0, controller2.getInvokers()[3].getExecFuncs());
        assertEquals(controller2.getTotalSizeMB()/controller2.getNInvokers(), controller2.getInvokers()[3].getAvailableMem());
    }

}
