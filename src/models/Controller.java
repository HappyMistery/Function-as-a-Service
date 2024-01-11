package models;

import exceptions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


public class Controller {
    private int nInvokers;
    private int totalSizeMB;
    private Map<String, Action> actions;
    private Invoker[] invokers;
    private List<MetricData> metricsList = new ArrayList<>();
    private PolicyManager policyManager;


     
    /**
     * @param nInv
     * @param tSizeMB
     */
    public Controller(int nInv, int tSizeMB){
        nInvokers = nInv;
        totalSizeMB = tSizeMB;
        actions = new HashMap<String, Action>();
        invokers = new Invoker[nInvokers];  //creem array d'Invoker.class de nInvokers elements
        for(int i = 0; i < nInvokers; i++) {
            invokers[i] = new Invoker(totalSizeMB/nInvokers);   //Inicialitzem cada Invoker de l'array
        }
    }


    public int getNInvokers() {
        return nInvokers;
    }

    public Invoker[] getInvokers() {
        return invokers;
    }

    public int getTotalSizeMB() {
        return totalSizeMB;
    }

    public PolicyManager getPolicyManager() {
        return policyManager;
    }

    /**
     * @param <T>
     * @param <R>
     * @param actionName
     * @param f
     * @param actionSizeMB
     */
    public <T, R> Action registerAction(String actionName, Function<T, R> f, int actionSizeMB) {  //"public <T, R> void ..." fa mètode genèric
        Action<T, R> action = new Action<>(actionName, f, actionSizeMB);    //creem una Action<T, R> amb la info proporcionada
        actions.put(actionName, action);    //afegim la accio al HashMap d'accions
        return action;
    }


    private PolicyManager specifPolicyManager(int policy) {
        switch (policy) {
            case 1:
                return new RoundRobin();
            case 2:
                return new GreedyGroup();
            case 3:
                return new UniformGroup();
            case 4:
                return new BigGroup();
            default:
                return null;
        }
    }

    /**
     * @param <T>
     * @param <R>
     * @param actionName
     * @param actionParam
     * @return
     * @throws NotEnoughMemory
     * @throws InterruptedException
     */
    public <T, R> R invoke(String actionName, T actionParam, int policy) throws NotEnoughMemory, PolicyNotDetected, InterruptedException {  //"public <T, R> R ..." fa mètode genèric
        Action<T, R> action = actions.get(actionName);    //obtenim la accio a executar
        policyManager = specifPolicyManager(policy);
        return invokers[0].invoke(this, action, actionParam, policyManager);    //invocar la accio amb el primer Invoker
    }

    public <T, R> Future<R> invoke_async(String actionName, T actionParam, int policy) throws NotEnoughMemory, PolicyNotDetected {
        Action<T, R> action = actions.get(actionName);    //obtenim la accio a executar
        policyManager = specifPolicyManager(policy);
        return invokers[0].invoke_async(this, action, actionParam, policyManager);    //invocar la accio amb el primer Invoker
    }

    public int calculateMaxTimeAction() {
        return Stream.of(invokers).map(invoker -> ((ConcreteObserver) invoker.getObserver()).getReceivedMetricData().getExecutionTime()).max(Comparator.naturalOrder()).orElse(0);
    }

    public int calculateMinTimeAction() {
        return Stream.of(invokers).map(invoker -> ((ConcreteObserver) invoker.getObserver()).getReceivedMetricData().getExecutionTime()).min(Comparator.naturalOrder()).orElse(0);
    }

    public float calculateMeanTimeAction() {
        return (float) Stream.of(invokers).mapToDouble(invoker -> ((ConcreteObserver) invoker.getObserver()).getReceivedMetricData().getExecutionTime()).average().orElse(0);
    }

    public int calculateAggregateTimeAction() {
        return Stream.of(invokers).mapToInt(invoker -> ((ConcreteObserver) invoker.getObserver()).getReceivedMetricData().getExecutionTime()).sum();
    }

    public List<Float> calculateMemoryForInvoker() {
        return Stream.of(invokers).map(invoker -> (float) ((ConcreteObserver) invoker.getObserver()).getReceivedMetricData().getMemoryUsage()).toList();
    }


    /**
     * Muestra las estadísticas de tiempo de ejecución de todas las acciones ejecutadas por cada invocador.
     * Incluye el tiempo máximo, mínimo y promedio de ejecución.
     */
    public <T, R> void displayExecutionTimeStats() {
        for(Action<T, R> action : actions.values()) {
            System.out.println("Action: " + action.getActionName() +
                    ", Max Time: " + this.calculateMaxTimeAction() +
                    ", Min Time: " + this.calculateMinTimeAction() +
                    ", Avg Time: " + this.calculateMeanTimeAction());
        }
    }
    

    /**
     * Muestra el tiempo total de ejecución por cada invocador.
     */
    public void displayExecutionTimeByInvoker(Observer observer) {
        
        Arrays.asList(invokers).stream().forEach(invoker -> {
            System.out.println("Invoker: " + invoker +
                    ", Total Execution Time: " + ((ConcreteObserver) observer).getReceivedMetricData().getExecutionTime());
        });
    }
}
