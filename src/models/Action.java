package models;
import java.util.function.Function;

import exceptions.NotEnoughMemory;

public class Action<T, R> {
    private String actionName;
    private Function<T, R> function;
    private int actionSizeMB;


    /**
     * Constructor for Action
     * @param name name of the action
     * @param func function to be executed
     * @param sizeMB size of the action in MB
     */
    public Action(String name, Function<T, R> func, int sizeMB) {
        actionName = name;
        function = func;
        actionSizeMB = sizeMB;
    }

    /**
     * Getter for action name
     * @return action name
     */
    public String getActionName() {
        return actionName;
    }

    
    /**
     * Getter for function
     * @return function
     */
    public Function<T, R> getFunction() {
        return function;
    }


    /**
     * Setter for function
     * @param newF new function
     */
    public void setFunction(Function<T, R> newF) {
        function = newF;
    }


    /**
     * Getter for action size in MB
     * @return action size in MB
     */
    public int getActionSizeMB() {
        return actionSizeMB;
    }
    /**
     * Runs the function with the given parameter
     * @param funcParam parameter for the function
     * @param invoker invoker that runs the function
     * @return result of the function
     * @throws NotEnoughMemory
     */
    public R run(T funcParam, Invoker invoker) throws NotEnoughMemory {
        invoker.acquireMemory(actionSizeMB);
        R result = function.apply(funcParam);
        invoker.releaseMemory(actionSizeMB);
        invoker.addExecFunc();
        return result;
    }
}
