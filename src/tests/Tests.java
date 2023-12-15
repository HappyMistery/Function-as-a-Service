package tests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import exceptions.NotEnoughMemory;
import models.Controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class Tests {

    Controller controller = new Controller(4, 1024);

    @BeforeEach
    public void setUp() throws FileNotFoundException, IOException {
        Function<Map<String, Integer>, Integer> f;
        f = x -> x.get("x") + x.get("y");
        controller.registerAction("addAction", f, 256);
        f = x -> x.get("x") - x.get("y");
        controller.registerAction("subAction", f, 256);
         f = x -> x.get("x") * x.get("y");
        controller.registerAction("multAction", f, 256);
        f = x -> x.get("x") / x.get("y");
        controller.registerAction("divAction", f, 256);
    }
    
    @Test
    public void test_unaFuncCadaCop() throws NotEnoughMemory {
        int res = (int) controller.invoke("addAction", Map.of("x", 6, "y", 2));
        assertEquals(8, res);

        res = (int) controller.invoke("subAction", Map.of("x", 6, "y", 2));
        assertEquals(4, res);

        res = (int) controller.invoke("multAction", Map.of("x", 6, "y", 2));
        assertEquals(12, res);

        res = (int) controller.invoke("divAction", Map.of("x", 6, "y", 2));
        assertEquals(3, res);
    }

    @Test
    public void test_moltesFuncCadaCop() throws NotEnoughMemory {
        List<Map<String, Integer>> input = Arrays.asList(
            new HashMap<String, Integer>() {{
                put("x", 2);
                put("y", 3);
            }},
            new HashMap<String, Integer>() {{
                put("x", 9);
                put("y", 1);
            }},
            new HashMap<String, Integer>() {{
                put("x", 8);
                put("y", 8);
            }}
        );
        List<Integer> result = controller.invoke("addAction", input);
        System.out.println(result.toString());
        rtergerger
        gergergerg
        regergegr
    }
}
