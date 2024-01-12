import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;

import exceptions.*;
import models.ConcreteObserver;
import models.Controller;
import models.MapReduce;
import models.TimerDecorator;

public class Main {
    public static void main(String[] args) throws NotEnoughMemory, PolicyNotDetected, InterruptedException, ExecutionException {
        Controller cont = new Controller(10, 1024);
        TimerDecorator controller = new TimerDecorator(cont);
        Controller cont2 = new Controller(10, 1024);
        TimerDecorator controller2 = new TimerDecorator(cont2);

        for(int i = 0; i < controller.getNInvokers(); i++) {
            ConcreteObserver observer = new ConcreteObserver();
            controller.getInvokers()[i].addObserver(observer);
            ConcreteObserver observer2 = new ConcreteObserver();
            controller2.getInvokers()[i].addObserver(observer2);
        }
        
        try {
            MapReduce cmpt = new MapReduce();
            Function<String, Map<String, Integer>> wordCount = x -> cmpt.wordCount(x);
            Function<String, Map<String, Integer>> countWords = x -> cmpt.countWords(x);

            Map<String, Integer> finalWC = new HashMap<String, Integer>();
            Map<String, Integer> finalCW = new HashMap<String, Integer>();

            controller.registerAction("wordCount", wordCount, 1);
            controller2.registerAction("countWords", countWords, 1);

            List<String> texts = new ArrayList<>();

            String txt0 = readFile("src/fitxers/The Liad.txt");
            texts.add(txt0);
            String txt1 = readFile("src/fitxers/Dracula.txt");
            texts.add(txt1);
            String txt2 = readFile("src/fitxers/Peter Pan.txt");
            texts.add(txt2);
            String txt3 = readFile("src/fitxers/Doctrina Christiana.txt");
            texts.add(txt3);
            String txt4 = readFile("src/fitxers/A year among the trees.txt");
            texts.add(txt4);
            String txt5 = readFile("src/fitxers/Don Quijote.txt");
            texts.add(txt5);
            String txt6 = readFile("src/fitxers/Moby Dick.txt");
            texts.add(txt6);
            String txt7 = readFile("src/fitxers/Frankenstein.txt");
            texts.add(txt7);
            String txt8 = readFile("src/fitxers/Leviathan.txt");
            texts.add(txt8);
            String txt9 = readFile("src/fitxers/Hamlet.txt");
            texts.add(txt9);

            Future<List<Map<String, Integer>>> resultatsWC = controller.invoke_async("wordCount", texts, 1);
            Future<List<Map<String, Integer>>> resultatsCW = controller2.invoke_async("countWords", texts, 1);
            
            for(Map<String, Integer> map : resultatsWC.get()){
                finalWC =  cmpt.mergeMaps(finalWC, map);
            }

            for(Map<String, Integer> map : resultatsCW.get()){
                finalCW = cmpt.mergeMaps(finalCW, map);
            }
            
            System.out.println(finalWC);

            System.out.println(finalCW);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        controller.printTimeStats();
        controller.printInvokerExecutionTime();
        controller2.printTimeStats();
        controller2.printInvokerExecutionTime();

        
        for(int i = 0; i < controller.getNInvokers(); i++) {
            controller.getInvokers()[i].getES().shutdown();
            controller2.getInvokers()[i].getES().shutdown();
        }
    }

        public static String readFile(String txtRoute) throws IOException {

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

