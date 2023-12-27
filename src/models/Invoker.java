package models;

import java.util.concurrent.Semaphore;
import java.util.function.Function;

public class Invoker {

    private float availableMem;
    private int execFuncs;
    private Semaphore sem;

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

    public <T, R> R runFunction(Action<T, R> action, T funcParam) throws InterruptedException {
        availableMem -= action.getActionSizeMB(); // treiem mem disponible de l'Invoker
        Function<T, R> function = (Function<T, R>) action.getFunction(); // obtenim la funcio a invocar
        R result = function.apply(funcParam); // passem els parametres a la funcio a invocar
        availableMem += action.getActionSizeMB(); // tornem mem disponible de l'Invoker
        execFuncs++; // augmentem el comptador de funcions executades per l'Invoker
        return result;
    }
}
