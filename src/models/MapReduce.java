package models;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class MapReduce {

    private final int parts;

    public MapReduce() {
        parts = 5;
        executorService = Executors.newFixedThreadPool(parts);
    }

    private ExecutorService executorService;
    /**
     * Comptador de paraules diferenciant entre aquestes mitjançant un enfocament Mapreduce.
     *
     * @param text
     * @param parts
     * @return
     */
    public Map<String, Integer> wordCount(String text) {
    List<Future<Map<String, Integer>>> futures = new ArrayList<>();
    List<List<String>> partitions = makePartitions(text);

    // Fase Map
    for (List<String> partition : partitions) {
        Future<Map<String, Integer>> future = executorService.submit(() -> {
            Map<String, Integer> localMap = new HashMap<>();
            for (String word : partition) {
                localMap.merge(word, 1, Integer::sum);
            }
            return localMap;
        });
        futures.add(future);
    }

    // Fase Reduce
    Map<String, Integer> result = new HashMap<>();
    for (Future<Map<String, Integer>> future : futures) {
        try {
            Map<String, Integer> partitionResult = future.get();
            result = mergeMaps(result, partitionResult);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    return result;
}


    /**
     * Comptador de paraules totals d'un text amb enfocament Mapreduce.
     *
     * @param text
     * @param parts
     * @return
     */
    public Map<String,Integer> countWords(String text) {
        List<List<String>> partitions = makePartitions(text);

        // Fase Map
        List<Future<Integer>> futures = new ArrayList<>();
        for (List<String> partition : partitions) {
            Future<Integer> future = executorService.submit(() -> partition.size());
            futures.add(future);
        }

        // Fase Reduce
        AtomicInteger totalCmpt = new AtomicInteger(0);
        futures.stream().forEach(future -> {
              
            try {
                totalCmpt.addAndGet(future.get());
            } catch (Exception e) {
                e.printStackTrace();
            }

        });
    
        Map<String, Integer> result = new HashMap<>();
        result.put("totalWords", totalCmpt.get());

        return result;

        
    }

    /**
     * Combina dos mapes
     *
     * @param map1 El primer mapa a combinar.
     * @param map2 El segundo mapa a combinar.
     * @return Un nuevo mapa resultante de la combinación de map1 y map2.
     */
    public Map<String, Integer> mergeMaps(Map<String, Integer> map1, Map<String, Integer> map2) {
        Map<String, Integer> result = new HashMap<>(map1);

        for (Map.Entry<String, Integer> entry : map2.entrySet()) {
            result.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }

        return result;
    }

    /**
     * Divide un texto en partes iguales, cada una destinada a ser procesada por un hilo.
     *
     * @param text El texto a dividir.
     * @param threads El número de partes en las que dividir el texto.
     * @return Una lista de listas de palabras, donde cada lista interna representa una parte del texto.
     */
    private List<List<String>> makePartitions(String text){
        List<String> total = Arrays.stream(text.replaceAll("[^A-Za-z]+", " ")
            .split("\\s+"))
            .filter(word -> !word.isEmpty())
            .toList();
        int wordsPerThread = total.size() / parts;
        List<List<String>> partitions = new ArrayList<>();
        for (int i = 0; i < parts; i++) {
            int start = i * wordsPerThread;
            int end = (i + 1) * wordsPerThread;
            List<String> partition = new ArrayList<>();
            for (int j = start; j < end; j++) {
                String word = total.get(j);
                partition.add(word);
                if(i==parts-1 && j==end-1){
                    for (int k = end; k < total.size(); k++) {
                        word = total.get(k);
                        partition.add(word);
                    }
                }
            }
            partitions.add(partition);
        }
        return partitions;
    }
}