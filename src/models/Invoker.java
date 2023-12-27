package models;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

public class Invoker {

    private float availableMem;
    private int execFuncs;
    private Semaphore sem;
    private Observer observer;


    public Invoker(float mem) {
        availableMem = mem;
        execFuncs = 0;
        sem = new Semaphore(1);
    }

    public int getExecFuncs() {
        return execFuncs;
    }

    public float getAvailableMem() {
        return availableMem;
    }

    public Semaphore getSem() {
        return sem;
    }

    public Observer getObserver() {
        return observer;
    }

    public <T, R> R runFunction(Action<T, R> action, T funcParam) throws InterruptedException {
        long startTime, endTime, executionTime;
        availableMem -= action.getActionSizeMB(); // treiem mem disponible de l'Invoker
        Function<T, R> function = (Function<T, R>) action.getFunction(); // obtenim la funcio a invocar
        startTime = System.currentTimeMillis();
        R result = function.apply(funcParam); // passem els parametres a la funcio a invocar
        endTime = System.currentTimeMillis();
        executionTime = endTime - startTime;
        availableMem += action.getActionSizeMB(); // tornem mem disponible de l'Invoker
        execFuncs++; // augmentem el comptador de funcions executades per l'Invoker

        if(observer != null) {
            MetricData metricData = new MetricData((int) executionTime, this, action.getActionSizeMB());
            notifyObserver(metricData);
        }
        
        return result;
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
