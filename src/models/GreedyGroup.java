package models;

import exceptions.NotEnoughMemory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;


public class GreedyGroup extends PolicyManager {


/**
     * executes the functions across the invokers with the GreedyGroup policy
     * @param <T>
     * @param <R>
     * @param cont
     * @param action
     * @param actionParam
     * @param isAsync
     * @return The list of results of the functions
     * @throws NotEnoughMemory
     * @throws InterruptedException
     */
    @Override
    public <T, R> R selectInvokerWithPolicy(Controller cont, Action<T, R> action, T actionParam, boolean isAsync) throws NotEnoughMemory, InterruptedException {
        if (actionParam instanceof List) { // si ens passen una llista de parametres
            Invoker[] invs = new Invoker[cont.getNInvokers()];
            invs = checkForMemory(cont, action, actionParam, 0, 1, 2, isAsync); // comprovem que hi ha prou memoria per executar el grup de funcions
            int i = 0;

            // GreedyGroup Policy - Omple tant com pot un invoker abans de passar al següent
            List<R> resFinal = new ArrayList<>(((List<T>) actionParam).size());
            int j = 0;
            for (i = 0; i < invs.length; i++) {
                if(isAsync) invs[i].getSem().acquire();
                while (invs[i].getAvailableMem() >= action.getActionSizeMB() && j < ((List<T>) actionParam).size()) {
                    resFinal.add((R) invs[i].runFunction(action, ((List<T>) actionParam).get(j)));  // afegim el resultat de la funcio a la llista de resultats
                    j++;
                }
                if(isAsync) invs[i].getSem().release();
            }
            return (R) resFinal;
        }
        return soloFuncExec(cont, action, actionParam, isAsync);
    }

    
}