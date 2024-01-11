import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.function.Function;
import exceptions.NotEnoughMemory;
import exceptions.PolicyNotDetected;
import models.ConcreteObserver;
import models.Controller;
import models.MapReduce;

public class Main {
    public static void main(String[] args) throws NotEnoughMemory, PolicyNotDetected, InterruptedException {
        Controller controller = new Controller(4, 1024);
        ConcreteObserver observer = new ConcreteObserver();

         for(int i = 0; i < controller.getNInvokers(); i++) {
            controller.getInvokers()[i].addObserver(observer);
        }
        
        try {
            String txt = readFile();
            MapReduce cmpt = new MapReduce(4);
            System.out.println(cmpt.wordCount(txt,4));
            System.out.println(cmpt.countWords(txt,4));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //controller.displayExecutionTimeStats();
        //controller.displayExecutionTimeByInvoker(observer);

        controller.getES().shutdown();
    }

        public static String readFile() throws IOException {
        String txtRoute = "src/fitxers/The Liad by Homer.txt";

        BufferedReader reader = new BufferedReader(new FileReader(txtRoute));
        StringBuilder stringBuilder = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }
        reader.close();

        return stringBuilder.toString();
    }
        
}

