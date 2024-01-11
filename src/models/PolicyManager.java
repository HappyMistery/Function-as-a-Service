package models;

import java.util.List;
import exceptions.NotEnoughMemory;
import exceptions.PolicyNotDetected;

public abstract class PolicyManager {

    /**
     * selects the invoker to execute the functiondepending on the policy selected
     * @param <T>
     * @param <R>
     * @param cont
     * @param action
     * @param actionParam
     * @param policy
     * @param isAsync
     * @return The list of results of the functions
     * @throws NotEnoughMemory
     * @throws PolicyNotDetected
     * @throws InterruptedException
     */
    public abstract <T, R> R selectInvokerWithPolicy(Controller cont, Action<T, R> action, T actionParam, boolean isAsync) throws NotEnoughMemory, InterruptedException;


    /**
     * checks if there is enough memory across the invokers to execute the functions in groups of groupSize
     * @param <T>
     * @param <R>
     * @param cont
     * @param action
     * @param actionParam
     * @param nExecFuncs
     * @param groupSize
     * @param policy
     * @param isAsync
     * @return A list of all the invokers that can execute the functions
     * @throws NotEnoughMemory
     * @throws InterruptedException
     */
    protected static <T, R> Invoker[] checkForMemory(Controller cont, Action<T, R> action, T actionParam, int nExecFuncs, int groupSize, int policy, boolean isAsync) throws NotEnoughMemory, InterruptedException {
        float foundMem = 0;
        float totalMemGroup = (action.getActionSizeMB() * ((List<T>) actionParam).size()) - (action.getActionSizeMB() * nExecFuncs);
        Invoker[] selectedInvokers = new Invoker[cont.getNInvokers()];
        int j = 0;

        for (int i = 0; i < cont.getNInvokers(); i++) { // busquem tots els invokers amb prou memòria per executar la funció
            if (cont.getInvokers()[i].getAvailableMem() >= (action.getActionSizeMB() * groupSize)) {
                foundMem += cont.getInvokers()[i].getAvailableMem();
                selectedInvokers[j] = cont.getInvokers()[i];    //si te prou mem el seleccionem
                j++;
            }
        }
        if (foundMem < totalMemGroup) {
            switch (policy) {
                case 3:
                    throw new NotEnoughMemory("Les funcions que vols executar no poden ser executades amb la política \"UniformGroup\".");
                case 4:
                    throw new NotEnoughMemory("Les funcions que vols executar no poden ser executades amb la política \"BigGroup\".");
                default:
                    throw new NotEnoughMemory("Les funcions que vols executar no poden ser executades al complet.");
            }
        }
        return selectedInvokers;
    }

    /**
     * executes a single function on the first available invoker with enough memory
     * @param <T>
     * @param <R>
     * @param cont
     * @param action
     * @param actionParam
     * @param isAsync
     * @return The single result of the function
     * @throws NotEnoughMemory
     * @throws InterruptedException
     */
    protected static <T, R> R soloFuncExec(Controller cont, Action<T, R> action, T actionParam, boolean isAsync) throws NotEnoughMemory, InterruptedException {
        int j = 0;

        while ((j < cont.getNInvokers()) && (cont.getInvokers()[j].getAvailableMem() < action.getActionSizeMB())) j++;// busquem un invoker amb prou memòria per executar la funció
        if (j >= cont.getNInvokers()) {
            throw new NotEnoughMemory(
                    "La funció que vols executar no pot ser executada per cap Invoker degut a la seva gran mida.");
        }

        Invoker inv = cont.getInvokers()[j]; // guardem l'Invoker que executarà la funcio
        R resFinal = (R) inv.runFunction(action, actionParam);
        return resFinal;
    }
}
