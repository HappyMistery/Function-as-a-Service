package models;

import java.util.concurrent.*;
import java.util.function.Function;

import exceptions.NotEnoughMemory;
import exceptions.PolicyNotDetected;

public class Invoker {
    private float availableMem;
    private int execFuncs;
    private Observer observer;
    private final ExecutorService executorService;
    private final Object memoryLock = new Object();



    public Invoker(float mem) {
        availableMem = mem;
        execFuncs = 0;
        executorService = Executors.newCachedThreadPool();
    }

    public int getExecFuncs() {
        synchronized (memoryLock) {
            return execFuncs;
        }
    }

    public void addExecFunc() {
        synchronized (memoryLock) {
            execFuncs++;
        }
    }

    public float getAvailableMem() {
        synchronized (memoryLock) {
            return availableMem;
        }
    }

    public void acquireMemory(float mem) throws NotEnoughMemory {
        synchronized (memoryLock) {
            if (availableMem < mem) {
                throw new NotEnoughMemory("Not enough memory available");
            }
            availableMem -= mem;
        }
    }

    public void releaseMemory(float mem) throws NotEnoughMemory {
        synchronized (memoryLock) {
            if (availableMem < mem) {
                throw new NotEnoughMemory("Not enough memory available");
            }
            availableMem += mem;
        }
    }

    public Observer getObserver() {
        return observer;
    }

    public ExecutorService getES() {
        return executorService;
    }

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

    public <T, R> R invoke(Controller cont, Action<T, R> action, T actionParam, PolicyManager pm) throws NotEnoughMemory, PolicyNotDetected, InterruptedException {  //"public <T, R> R ..." fa mètode genèric
        synchronized (memoryLock) {
            return pm.selectInvokerWithPolicy(cont, action, actionParam, false);
        }
    }

    public <T, R> Future<R> invoke_async(Controller cont, Action<T, R> action, T actionParam, PolicyManager pm) throws NotEnoughMemory, PolicyNotDetected {
        return executorService.submit(() -> {
            try {
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

     // Método para registrar observadores
    public void addObserver(Observer observer) {
        this.observer = observer;
    }

     // Método para notificar cambios en las métricas
    private void notifyObserver(MetricData metricData) {
        observer.updateMetrics(metricData);
    }
}
