package models;

import exceptions.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.concurrent.*;


public class Controller {
    private int nInvokers;
    private int totalSizeMB;
    protected Map<String, Action> actions;
    protected Invoker[] invokers;
    private List<MetricData> metricsList = new ArrayList<>();
    private PolicyManager policyManager;

     
    /**
     * Constructor for Controller
     * @param nInv number of invokers
     * @param tSizeMB total size of the controller in MB (each invoker will have tSizeMB/nInv MB)
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

    /**
     * Getter for number of invokers
     * @return number of invokers
     */
    public int getNInvokers() {
        return nInvokers;
    }

    /**
     * Getter for invokers
     * @return array of invokers
     */
    public Invoker[] getInvokers() {
        return invokers;
    }

    /**
     * Getter for total size of the controller in MB
     * @return total size of the controller in MB
     */
    public int getTotalSizeMB() {
        return totalSizeMB;
    }

    /**
     * getter for the policyManager that is going to be used
     * @return policyManager
     */
    public PolicyManager getPolicyManager() {
        return policyManager;
    }

    /**
     * registers an action for the controller
     * @param <T> type of the parameter
     * @param <R> type of the result
     * @param actionName name of the action
     * @param f function that is contained in the action to execute
     * @param actionSizeMB size of the action in MB
     * @return Action that has been registered
     */
    public <T, R> Action<T, R> registerAction(String actionName, Function<T, R> f, int actionSizeMB) {  //"public <T, R> void ..." fa mètode genèric
        Action<T, R> action = new Action<>(actionName, f, actionSizeMB);    //creem una Action<T, R> amb la info proporcionada
        actions.put(actionName, action);    //afegim la accio al HashMap d'accions
        return action;
    }


    /**
     * Selects the PolicyManager with the policy specified
     * @param policy policy to be used
     * @return PolicyManager
     */
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
     * Invokes the action synchronously with the policy specified
     * @param <T> type of the parameter
     * @param <R> type of the result
     * @param actionName name of the action
     * @param actionParam parameter of the action
     * @return result of the action
     * @throws NotEnoughMemory
     * @throws InterruptedException
     */
    public <T, R> R invoke(String actionName, T actionParam, int policy) throws NotEnoughMemory, PolicyNotDetected, InterruptedException {  //"public <T, R> R ..." fa mètode genèric
        Action<T, R> action = actions.get(actionName);    //obtenim la accio a executar
        policyManager = specifPolicyManager(policy);
        return invokers[0].invoke(this, action, actionParam);    //invocar la accio amb el primer Invoker
    }

    /**
     * Invokes the action asynchronously with the policy specified
     * @param <T> type of the parameter
     * @param <R> type of the result
     * @param actionName name of the action
     * @param actionParam parameter of the action
     * @param policy policy to be used
     * @return result of the action (Future)
     * @throws NotEnoughMemory
     * @throws PolicyNotDetected
     */
    public <T, R> Future<R> invoke_async(String actionName, T actionParam, int policy) throws NotEnoughMemory, PolicyNotDetected {
        Action<T, R> action = actions.get(actionName);    //obtenim la accio a executar
        policyManager = specifPolicyManager(policy);
        return invokers[0].invoke_async(this, action, actionParam);    //invocar la accio amb el primer Invoker
    }

    /**
     * Calculates the maximum time taken to execute an action by an invoker
     * @return time in miliseconds
     */
    public int calculateMaxTimeAction() {
        return Stream.of(invokers).map(invoker -> ((ConcreteObserver) invoker.getObserver()).getReceivedMetricData().getExecutionTime()).max(Comparator.naturalOrder()).orElse(0);
    }

    /**
     * Calculates the minimum time taken to execute an action by an invoker
     * @return time in miliseconds
     */
    public int calculateMinTimeAction() {
        return Stream.of(invokers).map(invoker -> ((ConcreteObserver) invoker.getObserver()).getReceivedMetricData().getExecutionTime()).min(Comparator.naturalOrder()).orElse(0);
    }

    /**
     * Calculates the mean time taken to execute an action by an invoker
     * @return time in miliseconds
     */
    public float calculateMeanTimeAction() {
        return (float) Stream.of(invokers).mapToDouble(invoker -> ((ConcreteObserver) invoker.getObserver()).getReceivedMetricData().getExecutionTime()).average().orElse(0);
    }

    /**
     * Calculates the total time taken to execute all actions by all invokers
     * @return time in miliseconds
     */
    public int calculateAggregateTimeAction() {
        return Stream.of(invokers).mapToInt(invoker -> ((ConcreteObserver) invoker.getObserver()).getReceivedMetricData().getExecutionTime()).sum();
    }

    /**
     * Calculates the maximum memory used by an invoker
     * @return memory in MB
     */
    public List<Float> calculateMemoryForInvoker() {
        return Stream.of(invokers).map(invoker -> (float) ((ConcreteObserver) invoker.getObserver()).getReceivedMetricData().getMemoryUsage()).toList();
    }


    /**
     * prints the time statistics for each action
     * @param <T> type of the parameter
     * @param <R> type of the result
     */
    public <T, R> void printTimeStats() {
        for(Action<T, R> action : actions.values()) {
            System.out.println("Action: " + action.getActionName() +
                    ", Max Time: " + this.calculateMaxTimeAction() +
                    " ms, Min Time: " + this.calculateMinTimeAction() +
                    " ms, Avg Time: " + this.calculateMeanTimeAction() + " ms");
        }
    }
    

    /**
     * prints the total execution time of each invoker
     * @param observer observer that is going to be used
     */
    public void printInvokerExecutionTime() {
        
        Arrays.asList(invokers).stream().forEach(invoker -> {
            System.out.println("Invoker: " + invoker +
                    ", Total Execution Time: " + ((ConcreteObserver) invoker.getObserver()).getReceivedMetricData().getExecutionTime() + " ms");
        });
    }
}
