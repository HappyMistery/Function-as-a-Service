package tests;

import static org.junit.Assert.assertEquals;


import org.junit.jupiter.api.Test;
import java.util.function.Function;

import models.*;

public class ReflectionTests {
    
    Controller controller = new Controller(4, 1024);

    public class Calculator {
        public int add(int a, int b) {
            return a + b;
        }
    }
    
    @Test
    public void testReflection() throws Exception {
        Calculator calculator = new Calculator();

       
        Function<Integer, Integer> addFunction = x -> calculator.add(x, 3);
        Action<Integer, Integer> addAction = controller.registerAction("add", addFunction, 64);

        
        ActionProxy<Integer, Integer> proxy = DynamicProxy.createProxy(controller, addAction);

        
        int result = proxy.execute(5);
        assertEquals(8, result);
    }

}