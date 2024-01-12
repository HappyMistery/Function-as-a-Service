package models;


import exceptions.NotEnoughMemory;
import java.util.ArrayList;
import java.util.List;

public class RoundRobin extends PolicyManager {

    /**
     * executes the functions across the invokers with the RoundRobin policy (distributes the functions uniformly between the Invokers)
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
            invs = checkForMemory(cont, action, actionParam, 0, 1, 1, isAsync); // comprovem que hi ha prou memoria per executar el grup de funcions
            int i = 0;
            
            // RoundRobin Policy - Distribueix uniformement les funcions entre els Invokers
            List<R> resFinal = new ArrayList<>(((List<T>) actionParam).size());
            int j = 0;
            for (i = 0; i < ((List<T>) actionParam).size(); i++) {
                if (invs[j].getAvailableMem() >= action.getActionSizeMB()) {
                    resFinal.add((R) invs[j].runFunction(action, ((List<T>) actionParam).get(i)));    // afegim el resultat de la funcio a la llista de resultats
                }

                if (j >= invs.length - 1) {
                    j = 0; // si hem arribat al final de la llista de Invokers, tornem a començar
                    invs = checkForMemory(cont, action, actionParam, i + 1, 1, 1, isAsync); // comprovem que hi ha prou memoria per executar el grup de funcions
                } else {
                    j++;
                }
            }
            return (R) resFinal;
        }
        return soloFuncExec(cont, action, actionParam, isAsync);
    }


    
}