package tests;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exceptions.NotEnoughMemory;
import exceptions.PolicyNotDetected;
import models.ConcreteObserver;
import models.Controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
public class ObserverTests {

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
        controller.registerAction("sleepAction", sleep, 50);
    }

    @AfterAll
    public void closing() {
        controller.getES().shutdown();
    }

    @Test
    public void ObserverGetsTime() throws NotEnoughMemory, PolicyNotDetected, InterruptedException {
        ConcreteObserver observer = new ConcreteObserver();


        for(int i = 0; i < controller.getNInvokers(); i++) {
            controller.getInvokers()[i].addObserver(observer);
        }
        
        String res = controller.invoke("sleepAction", 3, 1);
        assertEquals("Done!", res);
        assertEquals(3, observer.getReceivedMetricData().getExecutionTime() / 1000);
    }

    @Test
    public void ObserverGetsInvoker() throws NotEnoughMemory, PolicyNotDetected, InterruptedException {
        ConcreteObserver observer = new ConcreteObserver();


        for(int i = 0; i < controller.getNInvokers(); i++) {
            controller.getInvokers()[i].addObserver(observer);
        }
        
        String res = controller.invoke("sleepAction", 1, 1);
        assertEquals("Done!", res);
        assertEquals(controller.getInvokers()[0], observer.getReceivedMetricData().getInvokerUsed());
    }

    @Test
    public void ObserverGetsMemoryUsage() throws NotEnoughMemory, PolicyNotDetected, InterruptedException {
        ConcreteObserver observer = new ConcreteObserver();


        for(int i = 0; i < controller.getNInvokers(); i++) {
            controller.getInvokers()[i].addObserver(observer);
        }
        
        String res = controller.invoke("sleepAction", 1, 1);
        assertEquals("Done!", res);
        assertEquals(50, observer.getReceivedMetricData().getMemoryUsage());
    }

    @Test
    public void calculateMaxTimeAction() throws InterruptedException, PolicyNotDetected, NotEnoughMemory {
        ConcreteObserver observer = new ConcreteObserver();

        for (int i = 0; i < controller.getNInvokers(); i++) {
            controller.getInvokers()[i].addObserver(observer);
        }

        String res = controller.invoke("sleepAction", 2, 1);
        int maxTime = controller.calculateMaxTimeAction();
        assertEquals(2, maxTime / 1000);
    }

    @Test
    public void calculateMinTimeAction() throws InterruptedException, PolicyNotDetected, NotEnoughMemory {
        ConcreteObserver observer = new ConcreteObserver();

        for (int i = 0; i < controller.getNInvokers(); i++) {
            controller.getInvokers()[i].addObserver(observer);
        }
        
        String res = controller.invoke("sleepAction", 2, 1);
        int minTime = controller.calculateMinTimeAction();
        assertEquals(2, minTime / 1000);
    }

    @Test
    public void calculateMeanTimeAction() throws InterruptedException, PolicyNotDetected, NotEnoughMemory {
        ConcreteObserver observer = new ConcreteObserver();

        for (int i = 0; i < controller.getNInvokers(); i++) {
            controller.getInvokers()[i].addObserver(observer);
        }

        String res = controller.invoke("sleepAction", 2, 1);
        float meanTime = controller.calculateMeanTimeAction();
        assertEquals(2, (int) meanTime / 1000);
    }

    @Test
    public void calculateAggregateTimeAction() throws InterruptedException, PolicyNotDetected, NotEnoughMemory {
        ConcreteObserver observer = new ConcreteObserver();

        for (int i = 0; i < controller.getNInvokers(); i++) {
            controller.getInvokers()[i].addObserver(observer);
        }

        String res = controller.invoke("sleepAction", 2, 1);
        int aggregateTime = controller.calculateAggregateTimeAction();
        assertEquals(2 * controller.getNInvokers(), aggregateTime / 1000);
    }

    @Test
    public void calculateMemoryForInvoker() throws InterruptedException, PolicyNotDetected, NotEnoughMemory {
        ConcreteObserver observer = new ConcreteObserver();

        for (int i = 0; i < controller.getNInvokers(); i++) {
            controller.getInvokers()[i].addObserver(observer);
        }

        String res = controller.invoke("sleepAction", 2, 1);
        List<Float> memoryForInvoker = controller.calculateMemoryForInvoker();
        assertEquals(controller.getNInvokers(), memoryForInvoker.size());
        assertTrue(memoryForInvoker.stream().allMatch(mem -> mem == 50));
    }
    

}
