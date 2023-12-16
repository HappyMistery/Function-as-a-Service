package tests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import exceptions.NotEnoughMemory;
import exceptions.PolicyNotDetected;
import models.Controller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class MultiThreadingTests {

    Controller controller = new Controller(4, 1024);

    @BeforeEach
    public void setUp() {
        Function<Integer, String> sleep = s -> {
            try {
            Thread.sleep(s * 1000);
            return "Done!";
            } catch (InterruptedException e) {
            throw new RuntimeException(e);
            }
            };
        controller.registerAction("sleepAction", sleep, 10);
    }

    @Test
    public void SleepSync() throws NotEnoughMemory, PolicyNotDetected, InterruptedException, ExecutionException {
        long startTime = System.currentTimeMillis();

        String res1 = controller.invoke("sleepAction", 5, 1);
        String res2 = controller.invoke("sleepAction", 5, 2);
        String res3 = controller.invoke("sleepAction", 5, 3);
        assertEquals("Done!", res1);
        assertEquals("Done!", res2);
        assertEquals("Done!", res3);

        long endTime = System.currentTimeMillis();
        assertEquals(15, (endTime - startTime) / 1000);
    }

    @Test
    public void SleepAsync() throws NotEnoughMemory, PolicyNotDetected, InterruptedException, ExecutionException {
        long startTime = System.currentTimeMillis();

        CompletableFuture res1 = controller.invoke_async("sleepAction", 5, 1);
        CompletableFuture res2 = controller.invoke_async("sleepAction", 5, 2);
        CompletableFuture res3 = controller.invoke_async("sleepAction", 5, 3);
        assertEquals("Done!", res1.get());
        assertEquals("Done!", res2.get());
        assertEquals("Done!", res3.get());

        long endTime = System.currentTimeMillis();
        assertEquals(5, (endTime - startTime) / 1000);
    }
}
