package models;

import exceptions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;



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
    public <T, R> R invoke(String actionName, T actionParam) throws NotEnoughMemory {  //"public <T, R> R ..." fa mètode genèric
        Action action = actions.get(actionName);    //obtenim la accio a executar
        if(actionParam instanceof List<?>) {    //si ens passen una llista de parametres
            List<R> resFinal = new ArrayList<>(((List<?>) actionParam).size());
            int j = 0;
            Invoker[] selectedInvokers = selectInvoker(action, actionParam);    //seleccionem els Invokers que executaran les funcions
            for(int i = 0; i < selectedInvokers.length; i++) {
                while(selectedInvokers[i].getAvailableMem() >= action.getActionSizeMB() && j < ((List<?>) actionParam).size()) {
                    R res = (R) selectedInvokers[i].runFunction(action, ((List<?>) actionParam).get(j));
                    j++;
                    resFinal.add(res);
                }
                selectedInvokers[i].setAvailableMem(action.getActionSizeMB()); //tornem mem a l'Invoker
            }
            return (R) resFinal;
        }

        Invoker selectedInv = selectInvoker(action, actionParam)[0];    //seleccionem l'Invoker que executarà la funcio
        R res = (R) selectedInv.runFunction(action, actionParam);
        selectedInv.setAvailableMem(action.getActionSizeMB()); //tornem mem a l'Invoker
        return res;
    }

    /* 
    public <T, R> ResultFuture<R> invoke_async(String actionName, T actionParam) throws NotEnoughMemory {
        Action<T, R> action = actions.get(actionName);    //obtenim la accio a executar
        ResultFuture<R> resultFuture = new ResultFuture<>();
        executorService.submit(() -> {
            semafor.acquire();
            Invoker selectedInv = selectInvoker(action, actionParam)[0];    //seleccionem l'Invoker que executarà la funcio
            R res = (R) selectedInv.runFunction(action, actionParam);
            selectedInv.setAvailableMem(action.getActionSizeMB()); //tornem mem a l'Invoker
            resultFuture.setResult(res);
            semafor.release();
        });
        executorService.shutdown();

        return resultFuture;
            
    }
    */


    /*
    if(actionParam instanceof List<?>) {    //si ens passen una llista de parametres
            List<R> resFinal = new ArrayList<>(((List<?>) actionParam).size());
            int j = 0;
            Invoker[] selectedInvokers = selectInvoker(action, actionParam);    //seleccionem els Invokers que executaran les funcions
            for(int i = 0; i < selectedInvokers.length; i++) {
                executorService.submit(() -> {
                    semafor.acquire();
                    while(selectedInvokers[i].getAvailableMem() >= action.getActionSizeMB() && j < ((List<?>) actionParam).size()) {
                        R res = (R) selectedInvokers[i].runFunction(action, ((List<?>) actionParam).get(j));
                        j++;
                        resultFuture.setResult(res);
                    }
                    selectedInvokers[i].setAvailableMem(action.getActionSizeMB()); //tornem mem a l'Invoker
                    semafor.release();
                });
            }
        }
     */

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
