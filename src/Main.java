package src;
import java.util.Map;
import java.util.function.Function;

import src.exceptions.NotEnoughMemory;

public class Main {
    public static void main(String[] args) throws NotEnoughMemory {
        Controller controller = new Controller(4, 1024);
        Function<Map<String, Integer>, Integer> f = x -> x.get("x") + x.get("y");
        controller.registerAction("addAction", f, 256);
        int res = (int) controller.invoke("addAction", Map.of("x", 6, "y", 2));
        System.out.println(res);
    }
}
