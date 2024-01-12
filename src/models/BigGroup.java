package models;

import exceptions.NotEnoughMemory;
import java.util.ArrayList;
import java.util.List;

public class BigGroup extends PolicyManager {

    /**
     * executes the functions across the invokers with the BigGroup policy (tries to fill the first invoker with a group of functions before passing to the next one)
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
            int groupSize = 3;
            Invoker[] invs = new Invoker[cont.getNInvokers()];
            invs = checkForMemory(cont, action, actionParam, 0, groupSize, 4, isAsync); // comprovem que hi ha prou memoria per executar el grup de funcions
            int i = 0;
            
            List<R> resFinal = new ArrayList<>(((List<T>) actionParam).size());
            int numFuncs = ((List<T>) actionParam).size();
            int count = 0;
            int j = 0;
            while(count < numFuncs) {   //mentre tinguem funcions
                while((invs[j].getAvailableMem() >= action.getActionSizeMB() * groupSize) &&(count < numFuncs)) {  //fem grups de 3 funcions fins que ens quedem sense funcions
                    i = 0;
                    while(i < groupSize && count < numFuncs) {
                        resFinal.add((R) invs[j].runFunction(action, ((List<T>) actionParam).get(Math.min(count, numFuncs - 1))));
                        count++;    //comptem les funcions que hem executat
                        i++;    //comptem quantes funcions té el grup actual (max 3)
                    }
                }
                if(j == invs.length - 1)
                    invs = checkForMemory(cont, action, actionParam, count, groupSize, 4, isAsync); // comprovem que hi ha prou memoria per executar el grup de groupSize funcions
                j++;
                j = j%invs.length;
            }
            return (R) resFinal;
        }
        return soloFuncExec(cont, action, actionParam, isAsync);
    }
    
}