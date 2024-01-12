package models;

import exceptions.NotEnoughMemory;
import java.util.ArrayList;
import java.util.List;


public class GreedyGroup extends PolicyManager {


    /**
     * executes the functions across the invokers with the GreedyGroup policy (tries to fill the first invoker with as many functions as possible before passing to the next one)
     * @param <T> type of the parameter
     * @param <R> type of the result
     * @param cont Controller
     * @param action Action to be executed
     * @param actionParam Parameter of the action
     * @param isAsync Boolean that indicates if the execution is asynchronous
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
                while (invs[i].getAvailableMem() >= action.getActionSizeMB() && j < ((List<T>) actionParam).size()) {
                    resFinal.add((R) invs[i].runFunction(action, ((List<T>) actionParam).get(j)));  // afegim el resultat de la funcio a la llista de resultats
                    j++;
                }
            }
            return (R) resFinal;
        }
        return soloFuncExec(cont, action, actionParam, isAsync);
    }

    
}