package models;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MapReduce {

    private final int parts;

    /**
     * Creates a new MapReduce object that will divide the texts in 5 parts (5 threads).
     */
    public MapReduce() {
        parts = 5;
        executorService = Executors.newFixedThreadPool(parts);
    }

    private ExecutorService executorService;
    /**
     * Counts the number of times each word appears in a text using a MapReduce approach.
     * @param text The text to count the words from.
     * @return A map containing the words as keys and the number of times they appear as values.
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
     * Counts the total number of words in a text using a MapReduce approach.
     * @param text The text to count the words from.
     * @return A map containing the total number of words.
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
     * Merges two maps by summing the values of the keys that are present in both maps.
     * @param map1 The first map.
     * @param map2 The second map.
     * @return A map containing the merged values.
     */
    public Map<String, Integer> mergeMaps(Map<String, Integer> map1, Map<String, Integer> map2) {
        Map<String, Integer> result = new HashMap<>(map1);

        for (Map.Entry<String, Integer> entry : map2.entrySet()) {
            result.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }

        return result;
    }

    /**
     * Divides the text in 5 parts (5 threads).
     * @param text The text to divide.
     * @return A list containing the 5 parts of the text.
     */
    private List<List<String>> makePartitions(String text){
        List<String> total = Arrays.stream(text.replaceAll("[^A-Za-z]", " ")
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