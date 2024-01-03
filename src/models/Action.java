package models;
import java.util.function.Function;

public class Action<T, R> {
    private String actionName;
    private Function<T, R> function;
    private int actionSizeMB;

    public Action(String name, Function<T, R> func, int sizeMB) {
        actionName = name;
        function = func;
        actionSizeMB = sizeMB;
    }

    public String getActionName() {
        return actionName;
    }

    public Function<T, R> getFunction() {
        return function;
    }

    public void setFunction(Function<T, R> newF) {
        function = newF;
    }

    public int getActionSizeMB() {
        return actionSizeMB;
    }

}
