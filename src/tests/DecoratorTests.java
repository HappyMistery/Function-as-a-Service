package tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import exceptions.NotEnoughMemory;
import exceptions.PolicyNotDetected;

import java.util.function.Function;
import models.*;

@TestInstance(Lifecycle.PER_CLASS)
public class DecoratorTests {
    Controller controller = new Controller(1, 1000);
    TimerDecorator timerDecorator  = new TimerDecorator(controller);
    MemoizationDecorator memoizationDecorator = new MemoizationDecorator(controller);

    @BeforeEach
    public void setUp() {
        Function<Integer, Integer> f = x -> calculateFactorial(x);
        controller.registerAction("factorial", f, 512);
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
        controller.getES().shutdown();
        controller = null;
    }

    private int calculateFactorial(int n) {
        if (n == 0 || n == 1) {
            return 1;
        } else {
            return n * calculateFactorial(n - 1);
        }
    }

    @Test
    public void testFactorialTime() throws InterruptedException, NotEnoughMemory, PolicyNotDetected {
        int res = timerDecorator.invoke("factorial", 10, 1);
        assertEquals(3628800, res);
    }

    @Test
    public void testSleepTime() throws InterruptedException, NotEnoughMemory, PolicyNotDetected {
        String res = timerDecorator.invoke("sleepAction", 2, 1);
        assertEquals("Done!", res);
    }

    @Test
    public void testFactorialMemo() throws InterruptedException, NotEnoughMemory, PolicyNotDetected {
        int res = memoizationDecorator.invoke("factorial", 10, 1);
        assertEquals(3628800, res);
    }

    @Test
    public void testFactorialTempMemo() throws InterruptedException, NotEnoughMemory, PolicyNotDetected {
        MemoizationDecorator memoizationDecorator2 = new MemoizationDecorator(timerDecorator);
        int res = memoizationDecorator2.invoke("factorial", 15, 1);
        assertEquals(2004310016, res);
    }
}