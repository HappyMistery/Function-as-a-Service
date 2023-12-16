package models;

import exceptions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.concurrent.*;


public class Controller {
    private int nInvokers;
    private int totalSizeMB;
    private Map<String, Action> actions;
    private Invoker[] invokers;
    private ExecutorService executorService;
    private Semaphore semafor;
     
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
        executorService = java.util.concurrent.Executors.newFixedThreadPool(nInvokers);
        semafor = new Semaphore(nInvokers);
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

    public Semaphore getSemafor() {
        return semafor;
    }

    public ExecutorService getES() {
        return executorService;
    }

    /**
     * @param <T>
     * @param <R>
     * @param actionName
     * @param f
     * @param actionSizeMB
     */
    public <T, R> void registerAction(String actionName, Function<T, R> f, int actionSizeMB) {  //"public <T, R> void ..." fa mètode genèric
        Action<T, R> action = new Action<>(actionName, f, actionSizeMB);    //creem una Action<T, R> amb la info proporcionada
        actions.put(actionName, action);    //afegim la accio al HashMap d'accions
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
        Action action = actions.get(actionName);    //obtenim la accio a executar
        return PolicyManager.selectInvokerWithPolicy(this, action, actionParam, policy, false);
    }

    //* 
    public <T, R> CompletableFuture<R> invoke_async(String actionName, T actionParam, int policy) throws NotEnoughMemory, PolicyNotDetected {
        Action<T, R> action = actions.get(actionName);    //obtenim la accio a executar
        CompletableFuture<R> resultFuture = new CompletableFuture<>();
        executorService.submit(() -> {
            try{
            //semafor.acquire();
            R res = PolicyManager.selectInvokerWithPolicy(this, action, actionParam, policy, true);
            resultFuture.complete(res);
            //semafor.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return resultFuture;
            
    }
    //*/
}
