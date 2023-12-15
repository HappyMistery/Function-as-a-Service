package models;

import java.util.function.Function;

public class Invoker {

    private float availableMem;
    private int execFuncs;

    public Invoker(float mem) {
        availableMem = mem;
        execFuncs = 0;
    }

    public int getExecFuncs() {
        return execFuncs;
    }

    public float getAvailableMem() {
        return availableMem;
    }

    public void setAvailableMem(float mem) {
        availableMem = mem;
    }

    public <T, R> R runFunction(Action<T, R> action, T funcParam) {
        availableMem -= action.getActionSizeMB(); // treiem mem disponible de l'Invoker
        Function<T, R> function = (Function<T, R>) action.getFunction(); // obtenim la funcio a invocar
        R result = function.apply(funcParam); // passem els parametres a la funcio a invocar
        execFuncs++; // augmentem el comptador de funcions executades per l'Invoker
        return result;
    }
}
