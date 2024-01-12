package models;

import java.util.concurrent.*;

import exceptions.NotEnoughMemory;
import exceptions.PolicyNotDetected;

public class Invoker {
    private float availableMem;
    private int execFuncs;
    private Observer observer;
    private final ExecutorService executorService;
    private final Object memoryLock = new Object();



    /**
     * Constructor for Invoker
     * @param mem memory of the invoker in MB
     */
    public Invoker(float mem) {
        availableMem = mem;
        execFuncs = 0;
        executorService = Executors.newCachedThreadPool();
    }

    /**
     * Getter for the number of functions executed
     * @return number of executed functions in the invoker
     */
    public int getExecFuncs() {
        synchronized (memoryLock) {
            return execFuncs;
        }
    }

    /**
     * Increments the number of functions executed
     */
    public void addExecFunc() {
        synchronized (memoryLock) {
            execFuncs++;
        }
    }

    /**
     * Getter for the available memory of the invoker (in the moment that is called)
     * @return available memory of the invoker in MB
     */
    public float getAvailableMem() {
        synchronized (memoryLock) {
            return availableMem;
        }
    }

    /**
     * Acquires memory for the invoker
     * @param mem memory to acquire in MB
     * @throws NotEnoughMemory
     */
    public void acquireMemory(float mem) throws NotEnoughMemory {
        synchronized (memoryLock) {
            availableMem -= mem;
        }
    }

    /**
     * Releases memory for the invoker
     * @param mem memory to release in MB
     * @throws NotEnoughMemory
     */
    public void releaseMemory(float mem) throws NotEnoughMemory {
        synchronized (memoryLock) {
            availableMem += mem;
        }
    }

    /**
     * Getter for the observer
     * @return observer
     */
    public Observer getObserver() {
        return observer;
    }

    /**
     * Getter for the ExecutorService
     * @return ExecutorService
     */
    public ExecutorService getES() {
        return executorService;
    }

    /**
     * Runs a function in the invoker and registers the time taken in case the invoker has observers
     * @param <T> type of the parameter
     * @param <R> type of the result
     * @param action Action to be executed
     * @param funcParam Parameter of the action
     * @return The result of the function
     * @throws InterruptedException
     * @throws NotEnoughMemory
     */
    public <T, R> R runFunction(Action<T, R> action, T funcParam) throws InterruptedException, NotEnoughMemory {
            long startTime, endTime, executionTime;
            startTime = System.currentTimeMillis();
            R result = action.run(funcParam, this); // passem els parametres a la funcio a invocar
            endTime = System.currentTimeMillis();
            executionTime = endTime - startTime;

            if(observer != null) {
                MetricData metricData = new MetricData((int) executionTime, this, action.getActionSizeMB());
                notifyObserver(metricData);
            }
            
            return result;
    }

    /**
     * With the specific policy Manager of the Controller, selects the invoker/s to execute the function/s
     * @param <T> type of the parameter
     * @param <R> type of the result
     * @param cont Controller
     * @param action Action to be executed
     * @param actionParam Parameter of the action
     * @return The result of the function/s
     * @throws NotEnoughMemory
     * @throws PolicyNotDetected
     * @throws InterruptedException
     */
    public <T, R> R invoke(Controller cont, Action<T, R> action, T actionParam) throws NotEnoughMemory, PolicyNotDetected, InterruptedException {  //"public <T, R> R ..." fa mètode genèric
        synchronized (memoryLock) {
            PolicyManager pm = cont.getPolicyManager();
            return pm.selectInvokerWithPolicy(cont, action, actionParam, false);
        }
    }

    /**
     * With the specific policy Manager of the Controller, selects the invoker/s to execute the function/s asynchronously
     * @param <T> type of the parameter
     * @param <R> type of the result
     * @param cont Controller
     * @param action Action to be executed
     * @param actionParam Parameter of the action
     * @return The result of the function/s (Future)
     * @throws NotEnoughMemory
     * @throws PolicyNotDetected
     */
    public <T, R> Future<R> invoke_async(Controller cont, Action<T, R> action, T actionParam) throws NotEnoughMemory, PolicyNotDetected {
        return executorService.submit(() -> {
            try {
                PolicyManager pm = cont.getPolicyManager();
                return pm.selectInvokerWithPolicy(cont, action, actionParam, true);
            } catch (NotEnoughMemory e) {
                e.printStackTrace();
                throw new RuntimeException("Not enough Memory", e);
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException("Interruption emergency", e);
            }
        });
    }

    /**
     * Assigns an observer to the invoker
     * @param observer Observer to be added
     */
    public void addObserver(Observer observer) {
        this.observer = observer;
    }

    /**
     * Notifies the observer with the metric data
     * @param metricData Metric data to be sent to the observer
     */
    private void notifyObserver(MetricData metricData) {
        observer.updateMetrics(metricData);
    }
}
