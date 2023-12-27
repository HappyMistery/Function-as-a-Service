package tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;

import exceptions.NotEnoughMemory;
import exceptions.PolicyNotDetected;

import java.util.function.Function;
import models.*;

public class DecoratorTests {
    Controller controller = new Controller(1, 1000);
    TimerDecorator timerDecorator  = new TimerDecorator(controller);

    @BeforeEach
    public void setUp() {
        Function<Integer, Integer> f = x -> calculateFactorial(x);
        timerDecorator.registerAction("factorial", f, 512);
        Function<Integer, String> sleep = s -> {
            try {
            Thread.sleep(s * 1000);
            return "Done!";
            } catch (InterruptedException e) {
            throw new RuntimeException(e);
            }
            };
        timerDecorator.registerAction("sleepAction", sleep, 50);
    }

    @Test
    public void testTime() throws InterruptedException, NotEnoughMemory, PolicyNotDetected {
        int res = timerDecorator.invoke("sleepAction", 5, 1);
        
        assertEquals(120, res);
    }


     private int calculateFactorial(int n) {
        if (n == 0 || n == 1) {
            return 1;
        } else {
            return n * calculateFactorial(n - 1);
        }
    }
}