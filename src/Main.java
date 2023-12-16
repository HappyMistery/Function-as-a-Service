import java.util.Map;
import java.util.function.Function;
import exceptions.NotEnoughMemory;
import exceptions.PolicyNotDetected;
import models.Controller;

public class Main {
    public static void main(String[] args) throws NotEnoughMemory, PolicyNotDetected, InterruptedException {
        Controller controller = new Controller(4, 1024);
        Function<Map<String, Integer>, Integer> f = x -> x.get("x") + x.get("y");
        controller.registerAction("addAction", f, 256);
        int res = (int) controller.invoke("addAction", Map.of("x", 6, "y", 2), 2);
        System.out.println(res);
    }
}
