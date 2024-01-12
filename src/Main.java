import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
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
        Controller controller = new Controller(10, 1024);
        Controller controller2 = new Controller(10, 1024);

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

            /*
            List<Map<String, Integer>> resultatsWC = controller.invoke("wordCount", texts, 1);
            List<Map<String, Integer>> resultatsCW = controller2.invoke("countWords", texts, 1);
            
            for(Map<String, Integer> map : resultatsWC){
                finalWC =  cmpt.mergeMaps(finalWC, map);
            }

            for(Map<String, Integer> map : resultatsCW){
                finalCW = cmpt.mergeMaps(finalCW, map);
            }
            List<Map<String, Integer>> mapsList = new ArrayList<>();
            mapsList.add(finalCW);
            mapsList.add(finalWC);
            writeMapToFile(mapsList, "src/fitxers/ResultsMapReduce.txt");
            //*/
            
            ///* 
            Future<List<Map<String, Integer>>> resultatsWC = controller.invoke_async("wordCount", texts, 1);
            Future<List<Map<String, Integer>>> resultatsCW = controller2.invoke_async("countWords", texts, 1);
            
            for(Map<String, Integer> map : resultatsWC.get()){
                finalWC =  cmpt.mergeMaps(finalWC, map);
            }

            for(Map<String, Integer> map : resultatsCW.get()){
                finalCW = cmpt.mergeMaps(finalCW, map);
            }
            List<Map<String, Integer>> mapsList = new ArrayList<>();
            mapsList.add(finalCW);
            mapsList.add(finalWC);
            writeMapToFile(mapsList, "src/fitxers/ResultsMapReduce.txt");
            //*/

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
        
        public static void writeMapToFile(List<Map<String, Integer>> Lmap, String filePath) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                writer.write("RESULTATS MAP REDUCE\n");
                writer.append("\n\n");
                for (Map<String, Integer> map : Lmap) {
                    for (Map.Entry<String, Integer> entry : map.entrySet()) {
                        writer.append(entry.getKey() + "= " + entry.getValue());
                        writer.newLine();
                    }
                    writer.append("\n\n");
                }
            } catch (IOException e) {
                e.printStackTrace(); // Handle the exception according to your needs
            }
        }
}

