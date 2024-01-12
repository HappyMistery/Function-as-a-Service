package tests;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.TestInstance.Lifecycle;

import exceptions.*;
import models.Controller;

import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

@TestInstance(Lifecycle.PER_CLASS)
public class PolicyManagerTests {

    Controller controller;

    @BeforeEach
    public void setUp() throws FileNotFoundException, IOException {
        controller = new Controller(4, 1024);
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
        Function<Integer, String> sleep = s -> {
            try {
            Thread.sleep(s * 1000);
            return "Done!";
            } catch (InterruptedException e) {
            throw new RuntimeException(e);
            }
            };
        controller.registerAction("sleepAction", sleep, 50);
    }

    @AfterAll
    public void closing() {
        for(int i = 0; i < controller.getNInvokers(); i++) {
            controller.getInvokers()[i].getES().shutdown();
        }
    }

    @Test
    public void syncRoundRobin() throws NotEnoughMemory, PolicyNotDetected, InterruptedException {
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
    public void asyncRoundRobin() throws NotEnoughMemory, PolicyNotDetected, InterruptedException, ExecutionException {
        List<Integer> input = Arrays.asList(1, 1, 1, 1);
        Future<String> res1, res2, res3;

        long startTime = System.currentTimeMillis();
        res1 = controller.invoke_async("sleepAction", input, 1);
        res2 = controller.invoke_async("sleepAction", input, 1);
        res3 = controller.invoke_async("sleepAction", input, 1);

        assertEquals(Arrays.asList("Done!", "Done!", "Done!", "Done!"), res1.get());
        assertEquals(Arrays.asList("Done!", "Done!", "Done!", "Done!"), res2.get());
        assertEquals(Arrays.asList("Done!", "Done!", "Done!", "Done!"), res3.get());
        
        //funcions s'han repartit uniformement entre els 4 invokers
        assertEquals(3, controller.getInvokers()[0].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[0].getAvailableMem());
        assertEquals(3, controller.getInvokers()[1].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[1].getAvailableMem());
        assertEquals(3, controller.getInvokers()[2].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[2].getAvailableMem());
        assertEquals(3, controller.getInvokers()[3].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[3].getAvailableMem());
        
        long endTime = System.currentTimeMillis();
        assertEquals(4, (endTime - startTime) / 1000);
    }

    @Test
    public void syncGreedyGroup() throws NotEnoughMemory, PolicyNotDetected, InterruptedException {
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

        //els invokers s'han intentat emplenar abans de passar al següent
        assertEquals(5, controller.getInvokers()[0].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[0].getAvailableMem());
        assertEquals(0, controller.getInvokers()[1].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[1].getAvailableMem());
        assertEquals(0, controller.getInvokers()[2].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[2].getAvailableMem());
        assertEquals(0, controller.getInvokers()[3].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[3].getAvailableMem());
    }

    @Test
    public void asyncGreedyGroup() throws NotEnoughMemory, PolicyNotDetected, InterruptedException, ExecutionException {
        List<Integer> input = Arrays.asList(1, 1);
        Future<String> res1, res2, res3;

        long startTime = System.currentTimeMillis();
        res1 = controller.invoke_async("sleepAction", input, 2);
        res2 = controller.invoke_async("sleepAction", input, 2);
        res3 = controller.invoke_async("sleepAction", input, 2);

        assertEquals(Arrays.asList("Done!", "Done!"), res1.get());
        assertEquals(Arrays.asList("Done!", "Done!"), res2.get());
        assertEquals(Arrays.asList("Done!", "Done!"), res3.get());
        
        //funcions s'han repartit uniformement entre els 4 invokers
        assertEquals(6, controller.getInvokers()[0].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[0].getAvailableMem());
        assertEquals(0, controller.getInvokers()[1].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[1].getAvailableMem());
        assertEquals(0, controller.getInvokers()[2].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[2].getAvailableMem());
        assertEquals(0, controller.getInvokers()[3].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[3].getAvailableMem());
        
        long endTime = System.currentTimeMillis();
        assertEquals(2, (endTime - startTime) / 1000);
    }

    
    @Test
    public void syncUniformGroup() throws NotEnoughMemory, PolicyNotDetected, InterruptedException {
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
    public void asyncUniformGroup() throws NotEnoughMemory, PolicyNotDetected, InterruptedException, ExecutionException {
        List<Integer> input = Arrays.asList(1, 1, 1, 1, 1);
        Future<String> res1, res2, res3;

        long startTime = System.currentTimeMillis();
        res1 = controller.invoke_async("sleepAction", input, 3);
        res2 = controller.invoke_async("sleepAction", input, 3);
        res3 = controller.invoke_async("sleepAction", input, 3);

        assertEquals(Arrays.asList("Done!", "Done!", "Done!", "Done!", "Done!"), res1.get());
        assertEquals(Arrays.asList("Done!", "Done!", "Done!", "Done!", "Done!"), res2.get());
        assertEquals(Arrays.asList("Done!", "Done!", "Done!", "Done!", "Done!"), res3.get());
        
        //funcions s'han repartit uniformement entre els 4 invokers
        assertEquals(9, controller.getInvokers()[0].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[0].getAvailableMem());
        assertEquals(6, controller.getInvokers()[1].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[1].getAvailableMem());
        assertEquals(0, controller.getInvokers()[2].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[2].getAvailableMem());
        assertEquals(0, controller.getInvokers()[3].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[3].getAvailableMem());
        
        long endTime = System.currentTimeMillis();
        assertEquals(5, (endTime - startTime) / 1000);
    }

    @Test
    public void syncBigGroup() throws NotEnoughMemory, PolicyNotDetected, InterruptedException {
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
        assertEquals(11, controller2.getInvokers()[0].getExecFuncs());
        assertEquals(controller2.getTotalSizeMB()/controller2.getNInvokers(), controller2.getInvokers()[0].getAvailableMem());
        assertEquals(0, controller2.getInvokers()[1].getExecFuncs());
        assertEquals(controller2.getTotalSizeMB()/controller2.getNInvokers(), controller2.getInvokers()[1].getAvailableMem());
        assertEquals(0, controller2.getInvokers()[2].getExecFuncs());
        assertEquals(controller2.getTotalSizeMB()/controller2.getNInvokers(), controller2.getInvokers()[2].getAvailableMem());
        assertEquals(0, controller2.getInvokers()[3].getExecFuncs());
        assertEquals(controller2.getTotalSizeMB()/controller2.getNInvokers(), controller2.getInvokers()[3].getAvailableMem());
    }


    @Test
    public void asyncBigGroup() throws NotEnoughMemory, PolicyNotDetected, InterruptedException, ExecutionException {
        List<Integer> input = Arrays.asList(1, 1, 1, 1);
        Future<String> res1, res2, res3;

        long startTime = System.currentTimeMillis();
        res1 = controller.invoke_async("sleepAction", input, 4);
        res2 = controller.invoke_async("sleepAction", input, 4);
        res3 = controller.invoke_async("sleepAction", input, 4);

        assertEquals(Arrays.asList("Done!", "Done!", "Done!", "Done!"), res1.get());
        assertEquals(Arrays.asList("Done!", "Done!", "Done!", "Done!"), res2.get());
        assertEquals(Arrays.asList("Done!", "Done!", "Done!", "Done!"), res3.get());
        
        //funcions s'han repartit uniformement entre els 4 invokers
         assertEquals(12, controller.getInvokers()[0].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[0].getAvailableMem());
        assertEquals(0, controller.getInvokers()[1].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[1].getAvailableMem());
        assertEquals(0, controller.getInvokers()[2].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[2].getAvailableMem());
        assertEquals(0, controller.getInvokers()[3].getExecFuncs());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[3].getAvailableMem());
        
        long endTime = System.currentTimeMillis();
        assertEquals(4, (endTime - startTime) / 1000);
    }
}

