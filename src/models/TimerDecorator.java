package models;

import exceptions.*;
import java.util.function.Function;
import java.util.Arrays;

import java.util.concurrent.Future;
import java.util.stream.Stream;
import java.util.Comparator;
import java.util.List;

import java.util.concurrent.atomic.AtomicReference;

public class TimerDecorator extends Controller {

    private Controller controller;

    /**
     * Constructor for TimerDecorator
     * @param controller controller to decorate
     */
    public TimerDecorator(Controller controller) {
        super(controller.getNInvokers(), controller.getTotalSizeMB());
        this.controller = controller;
    }

    /**
     * Getter for number of invokers
     * @return number of invokers
     */
    public int getNInvokers() {
        return controller.getNInvokers();
    }

    /**
     * Getter for invokers
     * @return array of invokers
     */
    public Invoker[] getInvokers() {
        return controller.getInvokers();
    }

    /**
     * Getter for total size of the controller in MB
     * @return total size of the controller in MB
     */
    public int getTotalSizeMB() {
        return controller.getTotalSizeMB();
    }

    /**
     * getter for the policyManager that is going to be used
     * @return policyManager
     */
    public PolicyManager getPolicyManager() {
        return controller.getPolicyManager();
    }

    public <T, R> Action<T, R> registerAction(String actionName, Function<T, R> f, int actionSizeMB) {  //"public <T, R> void ..." fa mètode genèric
        Action<T, R> action = controller.registerAction(actionName, f, actionSizeMB);
        return action;
    }

    /**
     * Invokes an action with a given parameter and a policy and uses a timer to measure the time
     * @param <T> type of the parameter
     * @param <R> type of the result
     * @param actionName name of the action to invoke
     * @param actionParam parameter of the action to invoke
     * @param policy policy to apply
     * @return result of the action
     * @throws NotEnoughMemory
     * @throws PolicyNotDetected
     * @throws InterruptedException
     */
    @Override
    public <T, R> R invoke(String actionName, T actionParam, int policy) throws NotEnoughMemory, PolicyNotDetected, InterruptedException {
        AtomicReference<R> resultContainer = new AtomicReference<>();

        Thread threadInvoke = new Thread(() -> {
            try {
                R result = controller.invoke(actionName, actionParam, policy);
                resultContainer.set(result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Thread threadTimer = new Thread(() -> {
            long startTime = System.currentTimeMillis();
            long endTime;
            while (resultContainer.get() == null) {
                try {
                    Thread.sleep(1);  // Ajusta el tiempo de espera según sea necesario
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                endTime = System.currentTimeMillis();
                System.out.println("Time taken: " + (endTime - startTime) + " milliseconds");
            }
        });

        threadInvoke.start();
        threadTimer.start();

        threadInvoke.join();
        threadTimer.join();

        return (R) resultContainer.get();
    }

    /**
     * Invokes an action with a given parameter and a policy and uses a timer to measure the time (async)
     * @param <T> type of the parameter
     * @param <R> type of the result
     * @param actionName name of the action to invoke
     * @param actionParam parameter of the action to invoke
     * @param policy policy to apply
     * @return result of the action
     * @throws NotEnoughMemory
     * @throws PolicyNotDetected
     */
    @Override
    public <T, R> Future<R> invoke_async(String actionName, T actionParam, int policy) throws NotEnoughMemory, PolicyNotDetected {
        AtomicReference<R> resultContainer = new AtomicReference<>();

        Thread threadInvoke = new Thread(() -> {
            try {
                Future<R> result = controller.invoke_async(actionName, actionParam, policy);
                resultContainer.set((R)result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Thread threadTimer = new Thread(() -> {
            long startTime = System.currentTimeMillis();
            long endTime;
            while (resultContainer.get() == null) {
                try {
                    Thread.sleep(1);  // Ajusta el tiempo de espera según sea necesario
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                endTime = System.currentTimeMillis();
                System.out.println("Time taken: " + (endTime - startTime) + " milliseconds");
            }
        });

        threadInvoke.start();
        threadTimer.start();

        try {
            threadInvoke.join();
            threadTimer.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return (Future<R>) resultContainer.get();
    }

    /**
     * Calculates the maximum time taken to execute an action by an invoker
     * @return time in seconds
     */
    public int calculateMaxTimeAction() {
        return controller.calculateMaxTimeAction();
    }

    /**
     * Calculates the minimum time taken to execute an action by an invoker
     * @return time in seconds
     */
    public int calculateMinTimeAction() {
        return controller.calculateMinTimeAction();
    }

    /**
     * Calculates the mean time taken to execute an action by an invoker
     * @return time in seconds
     */
    public float calculateMeanTimeAction() {
        return controller.calculateMeanTimeAction();
    }

    /**
     * Calculates the total time taken to execute all actions by all invokers
     * @return time in seconds
     */
    public int calculateAggregateTimeAction() {
        return controller.calculateAggregateTimeAction();
    }

    /**
     * Calculates the maximum memory used by an invoker
     * @return memory in MB
     */
    public List<Float> calculateMemoryForInvoker() {
        return controller.calculateMemoryForInvoker();
    }

    /**
     * prints the time statistics for each action
     * @param <T> type of the parameter
     * @param <R> type of the result
     */
    public <T, R> void printTimeStats() {
        controller.printTimeStats();
    }
    

    /**
     * prints the total execution time of each invoker
     */
    public void printInvokerExecutionTime() {
        controller.printInvokerExecutionTime();
    }
}