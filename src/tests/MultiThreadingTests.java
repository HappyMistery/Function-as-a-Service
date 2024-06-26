package tests;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import exceptions.*;
import models.Controller;

import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(Lifecycle.PER_CLASS)
public class MultiThreadingTests {

    Controller controller;

    @BeforeEach
    public void setUp() {
        controller = new Controller(4, 1024);
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
    public void SleepSync() throws NotEnoughMemory, PolicyNotDetected, InterruptedException, ExecutionException {
        
        long startTime = System.currentTimeMillis();
        String res1 = controller.invoke("sleepAction", 1, 1);
        String res2 = controller.invoke("sleepAction", 1, 2);
        String res3 = controller.invoke("sleepAction", 1, 3);
        long endTime = System.currentTimeMillis();

        assertEquals("Done!", res1);
        assertEquals("Done!", res2);
        assertEquals("Done!", res3);

        assertEquals(3, (endTime - startTime) / 1000);
    }

    @Test
    public void SleepAsyncSolo() throws NotEnoughMemory, PolicyNotDetected, InterruptedException, ExecutionException {
        
        long startTime = System.currentTimeMillis();
        Future<String> res1 = controller.invoke_async("sleepAction", 1, 1);
        Future<String> res2 = controller.invoke_async("sleepAction", 1, 1);
        Future<String> res3 = controller.invoke_async("sleepAction", 1, 1);

        assertEquals("Done!", res1.get());
        assertEquals("Done!", res2.get());
        assertEquals("Done!", res3.get());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[0].getAvailableMem());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[1].getAvailableMem());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[2].getAvailableMem());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[3].getAvailableMem());

        long endTime = System.currentTimeMillis();
        assertEquals(1, (endTime - startTime) / 1000);
    }

    @Test
    public void SleepAsyncGroup() throws NotEnoughMemory, PolicyNotDetected, InterruptedException, ExecutionException {
        List<Integer> segons = Arrays.asList(1, 1, 1, 1);
        Future<String> res1, res2, res3, res4;
    
        long startTime = System.currentTimeMillis();
        res1 = controller.invoke_async("sleepAction", segons, 1);
        res2 = controller.invoke_async("sleepAction", segons, 2);
        res3 = controller.invoke_async("sleepAction", segons, 3);
        res4 = controller.invoke_async("sleepAction", segons, 4);

        assertEquals(Arrays.asList("Done!", "Done!", "Done!", "Done!"), res1.get());
        assertEquals(Arrays.asList("Done!", "Done!", "Done!", "Done!"), res2.get());
        assertEquals(Arrays.asList("Done!", "Done!", "Done!", "Done!"), res3.get());
        assertEquals(Arrays.asList("Done!", "Done!", "Done!", "Done!"), res4.get());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[0].getAvailableMem());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[1].getAvailableMem());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[2].getAvailableMem());
        assertEquals(controller.getTotalSizeMB()/controller.getNInvokers(), controller.getInvokers()[3].getAvailableMem());

        long endTime = System.currentTimeMillis();
        assertEquals(4, (endTime - startTime) / 1000);
    }
}
