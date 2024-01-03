package models;

import java.lang.reflect.Proxy;

public class DynamicProxy {
    public static <T, R> ActionProxy<T, R> createProxy(Controller controller, Action<T, R> action) {
        return (ActionProxy<T, R>) Proxy.newProxyInstance(
                ActionProxy.class.getClassLoader(),
                new Class<?>[]{ActionProxy.class},
                new ActionInvocationHandler<>(controller, action)
        );
    }
}
