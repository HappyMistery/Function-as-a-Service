package models;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class ActionInvocationHandler<T, R> implements InvocationHandler {
    private Controller controller;
    private Action<T, R> action;

    /**
     * Constructor for ActionInvocationHandler
     * @param controller
     * @param action
     */
    public ActionInvocationHandler(Controller controller, Action<T, R> action) {
        this.controller = controller;
        this.action = action;
    }

    /**
     * Delegates the execution of the action to the controller once it tries to execute the method "execute"
     * @param proxy proxy
     * @param method Function to execute
     * @param args argument of the function
     * @return result of the function
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("execute")) {
            return controller.invoke(action.getActionName(), args[0], 1);  
        } else {
            throw new UnsupportedOperationException("Método no soportado: " + method.getName());
        }
    }
}
