package models;

import java.lang.reflect.Proxy;

public class DynamicProxy {

    /**
     * Creates a proxy for the given controller and action
     * @param <T> type of the parameter
     * @param <R> type of the result
     * @param controller controller
     * @param action action
     * @return the proxy that executes the action
     */ 
    public static <T, R> ActionProxy<T, R> createProxy(Controller controller, Action<T, R> action) {
        return (ActionProxy<T, R>) Proxy.newProxyInstance(
                ActionProxy.class.getClassLoader(),
                new Class<?>[]{ActionProxy.class},
                new ActionInvocationHandler<>(controller, action)
        );
    }
}
