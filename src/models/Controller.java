package models;

import exceptions.*;

import java.util.HashMap;
import java.util.List;
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
        semafor = new Semaphore(totalSizeMB);
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
     */
    public <T, R> R invoke(String actionName, T actionParam, int policy) throws NotEnoughMemory, PolicyNotDetected {  //"public <T, R> R ..." fa mètode genèric
        Action action = actions.get(actionName);    //obtenim la accio a executar
        return PolicyManager.selectInvokerWithPolicy(this, action, actionParam, policy);
    }

    //* 
    public <T, R> CompletableFuture<R> invoke_async(String actionName, T actionParam, int policy) throws NotEnoughMemory, PolicyNotDetected {
        Action<T, R> action = actions.get(actionName);    //obtenim la accio a executar
        CompletableFuture<R> resultFuture = new CompletableFuture<>();
        executorService.submit(() -> {
            try{
            semafor.acquire();
            R res = PolicyManager.selectInvokerWithPolicy(this, action, actionParam, policy);
            resultFuture.complete(res);
            semafor.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        executorService.shutdown();

        return resultFuture;
            
    }
    //*/

    /**
     * @param <T>
     * @param <R>
     * @param action
     * @param actionParam
     * @return
     * @throws NotEnoughMemory
     */
    public <T, R> Invoker[] selectInvoker(Action action, T actionParam) throws NotEnoughMemory{
        int i = 0;
        Invoker[] invs = new Invoker[nInvokers];
        if(actionParam instanceof List<?>) {    //si ens passen una llista de parametres
            float totalMemGroup = action.getActionSizeMB() * ((List<?>) actionParam).size();
            float foundMem = 0;
            for(i = 0; i < nInvokers; i++) {
                if(invokers[i].getAvailableMem() < action.getActionSizeMB()) {
                    continue;   //si l'Invoker no té prou mem per executar la funcio al menys 1 cop, passem al següent
                }
                else {
                    foundMem += invokers[i].getAvailableMem();
                }
            }

            if(foundMem < totalMemGroup) throw new NotEnoughMemory("Les funcions que vols executar no poden ser executades al complet.");

            int j = 0;
            for(i = 0; i < nInvokers; i++) {
                if(invokers[i].getAvailableMem() < action.getActionSizeMB()) {
                    continue;
                }
                else {
                    invs[j] = invokers[i];
                    j++;
                }
            }
            return invs;
        }

        while((i < nInvokers) && (invokers[i].getAvailableMem() < action.getActionSizeMB())) {
            i++;        //busquem un Invoker amb prou memòria per executar la funcio
        }
        if(i >= nInvokers)
            throw new NotEnoughMemory("La funció que vols executar no pot ser executada per cap Invoker degut a la seva gran mida.");
        invs[0] = invokers[i];  //guardem l'Invoker que executarà la funcio
        return invs;
    }
}
