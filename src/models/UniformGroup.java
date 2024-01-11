package models;

import exceptions.NotEnoughMemory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;


public class UniformGroup extends PolicyManager {


/**
     * executes the functions across the invokers with the UniformGroup policy
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
            int groupSize = 3;
            Invoker[] invs = new Invoker[cont.getNInvokers()];
            invs = checkForMemory(cont, action, actionParam, 0, groupSize, 3, isAsync); // comprovem que hi ha prou memoria per executar el grup de groupSize funcions
            int i = 0;
            
            List<R> resFinal = new ArrayList<>(((List<T>) actionParam).size());
            int numFuncs = ((List<T>) actionParam).size();
            int count = 0;
            int j = 0;
            while(count < numFuncs) {   //mentre tinguem funcions
                i = 0;
                while(i < groupSize && count < numFuncs) {  //fem grups de 3 funcions fins que ens quedem sense funcions
                    if(isAsync) invs[j].getSem().acquire();
                    resFinal.add((R) invs[j].runFunction(action, ((List<T>) actionParam).get(Math.min(count, numFuncs - 1))));
                    if(isAsync) invs[j].getSem().release();
                    count++;    //comptem les funcions que hem executat
                    i++;    //comptem quantes funcions té el grup actual (max 3)
                }
                if(j == invs.length - 1) {
                    j = 0; //si hem arribat al final de la llista de Invokers, tornem a començar
                    invs = checkForMemory(cont, action, actionParam, count, groupSize, 3, isAsync); // comprovem que hi ha prou memoria per executar el grup de groupSize funcions
                }
                else
                    j++;    //seguim omplint el següent invoker
            }
            return (R) resFinal;
        }
        return soloFuncExec(cont, action, actionParam, isAsync);
    }

    
}