package models;

import java.util.List;
import java.util.ArrayList;
import exceptions.NotEnoughMemory;
import exceptions.PolicyNotDetected;

public class PolicyManager {

    /**
     * @param <T>
     * @param <R>
     * @param cont
     * @param action
     * @param actionParam
     * @param policy
     * @return
     * @throws NotEnoughMemory
     * @throws PolicyNotDetected
     * @throws InterruptedException
     */
    public static <T, R> R selectInvokerWithPolicy(Controller cont, Action action, T actionParam, int policy, boolean isAsync)
            throws NotEnoughMemory, PolicyNotDetected, InterruptedException {
        switch (policy) {
            case 1:
                return RoundRobin(cont, action, actionParam, isAsync);
            case 2:
                return GreedyGroup(cont, action, actionParam, isAsync);
            case 3:
                return UniformGroup(cont, action, actionParam, isAsync);
            case 4:
                return BigGroup(cont, action, actionParam, isAsync);
            default:
                throw new PolicyNotDetected("No s'ha trobat la política seleccionada");
        }
    }

    /**
     * @param <T>
     * @param <R>
     * @param action
     * @param actionParam
     * @param isAsync
     * @return
     * @throws NotEnoughMemory
     * @throws InterruptedException
     */
    public static <T, R> R RoundRobin(Controller cont, Action action, T actionParam, boolean isAsync) throws NotEnoughMemory, InterruptedException {
        Invoker[] invs = new Invoker[cont.getNInvokers()];
        int j = 0;

        if (actionParam instanceof List<?>) { // si ens passen una llista de parametres
            invs = checkForMemory(cont, action, actionParam, 0, 1, 1, isAsync); // comprovem que hi ha prou memoria per executar el grup de funcions
            int i = 0;

            int[] invExecfunctions = new int[cont.getNInvokers()];
            for (i = 0; i < invExecfunctions.length; i++) {
                if(isAsync) cont.getSemafor().acquire();
                invExecfunctions[i] = cont.getInvokers()[i].getExecFuncs();  // guardem el nombre de funcions executades per cada Invoker
                if(isAsync) cont.getSemafor().release();
            }
            
            // RoundRobin Policy - Distribueix uniformement les funcions entre els Invokers
            List<R> resFinal = new ArrayList<>(((List<?>) actionParam).size());

            for (i = 0; i < ((List<?>) actionParam).size(); i++) {
                if(isAsync) cont.getSemafor().acquire();
                if (invs[j].getAvailableMem() < action.getActionSizeMB()) {
                    continue;
                } else
                    resFinal.add((R) invs[j].runFunction(action, ((List<?>) actionParam).get(i)));    // afegim el resultat de la funcio a la llista de resultats
                if (j == invs.length - 1) {
                    j = 0; // si hem arribat al final de la llista de Invokers, tornem a començar
                    invs = checkForMemory(cont, action, actionParam, i + 1, 1, 1, isAsync); // comprovem que hi ha prou memoria per executar el grup de funcions
                } else {
                    j++;
                }
                if(isAsync) cont.getSemafor().release();
            }

            for (i = 0; i < cont.getNInvokers(); i++) {
                if(isAsync) cont.getSemafor().acquire();
                cont.getInvokers()[i].returnMem(action.getActionSizeMB() * (cont.getInvokers()[i].getExecFuncs() - invExecfunctions[i])); // tornem mem a l'Invoker
                if(isAsync) cont.getSemafor().release();
            }
            return (R) resFinal;
        }
        return soloFuncExec(cont, action, actionParam, isAsync);
    }

    /**
     * @param <T>
     * @param <R>
     * @param action
     * @param actionParam
     * @param isAsync
     * @return
     * @throws NotEnoughMemory
     * @throws InterruptedException
     */
    public static <T, R> R GreedyGroup(Controller cont, Action action, T actionParam, boolean isAsync) throws NotEnoughMemory, InterruptedException {
        int i = 0;
        int count = 0;
        Invoker[] invs = new Invoker[cont.getNInvokers()];
        if (actionParam instanceof List<?>) { // si ens passen una llista de parametres
            invs = checkForMemory(cont, action, actionParam, 0, 1, 2, isAsync); // comprovem que hi ha prou memoria per executar el grup de funcions

            // GreedyGroup Policy - Omple tant com pot un invoker abans de passar al següent
            List<R> resFinal = new ArrayList<>(((List<?>) actionParam).size());
            int j = 0;
            for (i = 0; i < invs.length; i++) {
                count = 0;
                if(isAsync) cont.getSemafor().acquire();
                while (invs[i].getAvailableMem() >= action.getActionSizeMB() && j < ((List<?>) actionParam).size()) {
                    resFinal.add((R) invs[i].runFunction(action, ((List<?>) actionParam).get(j)));  // afegim el resultat de la funcio a la llista de resultats
                    j++;
                    count++; // comptador de funcions executades per l'Invoker i
                }
                invs[i].returnMem(action.getActionSizeMB() * count); // tornem mem a l'Invoker
                if(isAsync) cont.getSemafor().release();
            }
            return (R) resFinal;
        }
        return soloFuncExec(cont, action, actionParam, isAsync);
    }

    /**
     * @param <T>
     * @param <R>
     * @param action
     * @param actionParam
     * @param isAsync
     * @return
     * @throws NotEnoughMemory
     * @throws InterruptedException
     */
    public static <T, R> R UniformGroup(Controller cont, Action action, T actionParam, boolean isAsync) throws NotEnoughMemory, InterruptedException {
        if (actionParam instanceof List<?>) { // si ens passen una llista de parametres
            int groupSize = 3;
            Invoker[] invs = new Invoker[cont.getNInvokers()];
            invs = checkForMemory(cont, action, actionParam, 0, groupSize, 3, isAsync); // comprovem que hi ha prou memoria per executar el grup de groupSize funcions
            int i = 0;

            int[] invExecfunctions = new int[cont.getNInvokers()];
            for (i = 0; i < invExecfunctions.length; i++) {
                if(isAsync) cont.getSemafor().acquire();
                invExecfunctions[i] = cont.getInvokers()[i].getExecFuncs();  // guardem el nombre de funcions executades per cada Invoker
                if(isAsync) cont.getSemafor().release();
            }
            
            List<R> resFinal = new ArrayList<>(((List<?>) actionParam).size());
            int numFuncs = ((List<?>) actionParam).size();
            int count = 0;
            int j = 0;
            while(count < numFuncs) {   //mentre tinguem funcions
                i = 0;
                while(i < groupSize && count < numFuncs) {  //fem grups de 3 funcions fins que ens quedem sense funcions
                    if(isAsync) cont.getSemafor().acquire();
                    resFinal.add((R) invs[j].runFunction(action, ((List<?>) actionParam).get(Math.min(count, numFuncs - 1))));
                    if(isAsync) cont.getSemafor().release();
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

            for (i = 0; i < cont.getNInvokers(); i++) {
                if(isAsync) cont.getSemafor().acquire();
                cont.getInvokers()[i].returnMem(action.getActionSizeMB() * (cont.getInvokers()[i].getExecFuncs() - invExecfunctions[i])); // tornem mem a l'Invoker
                if(isAsync) cont.getSemafor().release();
            }

            return (R) resFinal;
        }
        return soloFuncExec(cont, action, actionParam, isAsync);
    }

    /**
     * @param <T>
     * @param <R>
     * @param action
     * @param actionParam
     * @param isAsync
     * @return
     * @throws NotEnoughMemory
     * @throws InterruptedException
     */
    public static <T, R> R BigGroup(Controller cont, Action action, T actionParam, boolean isAsync) throws NotEnoughMemory, InterruptedException {
        if (actionParam instanceof List<?>) { // si ens passen una llista de parametres
            int groupSize = 3;
            Invoker[] invs = new Invoker[cont.getNInvokers()];
            invs = checkForMemory(cont, action, actionParam, 0, groupSize, 4, isAsync); // comprovem que hi ha prou memoria per executar el grup de funcions
            int i = 0;

            int[] invExecfunctions = new int[cont.getNInvokers()];
            for (i = 0; i < invExecfunctions.length; i++) {
                if(isAsync) cont.getSemafor().acquire();
                invExecfunctions[i] = cont.getInvokers()[i].getExecFuncs();  // guardem el nombre de funcions executades per cada Invoker
                if(isAsync) cont.getSemafor().release();
            }
            
            List<R> resFinal = new ArrayList<>(((List<?>) actionParam).size());
            int numFuncs = ((List<?>) actionParam).size();
            int count = 0;
            int j = 0;
            while(count < numFuncs) {   //mentre tinguem funcions
                if(isAsync) cont.getSemafor().acquire();
                while(invs[j].getAvailableMem() >= action.getActionSizeMB() * groupSize && count < numFuncs) {  //fem grups de 3 funcions fins que ens quedem sense funcions
                    i = 0;
                    while(i < groupSize && count < numFuncs) {
                        resFinal.add((R) invs[j].runFunction(action, ((List<?>) actionParam).get(Math.min(count, numFuncs - 1))));
                        count++;    //comptem les funcions que hem executat
                        i++;    //comptem quantes funcions té el grup actual (max 3)
                    }
                }
                if(isAsync) cont.getSemafor().release();
                if(j == invs.length - 1) {
                    j = 0; //si hem arribat al final de la llista de Invokers, tornem a començar
                    invs = checkForMemory(cont, action, actionParam, count, groupSize, 4, isAsync); // comprovem que hi ha prou memoria per executar el grup de groupSize funcions
                }
                else
                    j++;    //seguim omplint el següent invoker
            }

            for (i = 0; i < cont.getNInvokers(); i++) {
                if(isAsync) cont.getSemafor().acquire();
                cont.getInvokers()[i].returnMem(action.getActionSizeMB() * (cont.getInvokers()[i].getExecFuncs() - invExecfunctions[i])); // tornem mem a l'Invoker
                if(isAsync) cont.getSemafor().release();
            }

            return (R) resFinal;
            
        }
        return soloFuncExec(cont, action, actionParam, isAsync);
    }

    /**
     * @param <T>
     * @param <R>
     * @param cont
     * @param action
     * @param actionParam
     * @param nExecFuncs
     * @param groupSize
     * @param policy
     * @param isAsync
     * @return
     * @throws NotEnoughMemory
     * @throws InterruptedException
     */
    private static <T, R> Invoker[] checkForMemory(Controller cont, Action action, T actionParam, int nExecFuncs, int groupSize, int policy, boolean isAsync) throws NotEnoughMemory, InterruptedException {
        float foundMem = 0;
        float totalMemGroup = (action.getActionSizeMB() * ((List<?>) actionParam).size()) - (action.getActionSizeMB() * nExecFuncs);
        Invoker[] selectedInvokers = new Invoker[cont.getNInvokers()];
        int j = 0;

        for (int i = 0; i < cont.getNInvokers(); i++) { // busquem tots els invokers amb prou memòria per executar la
            // funcio
            if(isAsync) cont.getSemafor().acquire();
            if (cont.getInvokers()[i].getAvailableMem() < (action.getActionSizeMB() * groupSize)) {
                continue; // si l'Invoker no té prou mem per executar la funcio al menys groupSize cop(s), passem al següent
            } else {
                foundMem += cont.getInvokers()[i].getAvailableMem();
                selectedInvokers[j] = cont.getInvokers()[i];
                j++;
            }
            if(isAsync) cont.getSemafor().release();
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
     * @param <T>
     * @param <R>
     * @param cont
     * @param action
     * @param actionParam
     * @param isAsync
     * @return
     * @throws NotEnoughMemory
     * @throws InterruptedException
     */
    private static <T, R> R soloFuncExec(Controller cont, Action action, T actionParam, boolean isAsync) throws NotEnoughMemory, InterruptedException {
        int j = 0;
        if(isAsync) cont.getSemafor().acquire();
        while ((j < cont.getNInvokers()) && (cont.getInvokers()[j].getAvailableMem() < action.getActionSizeMB())) {
            j++; // busquem el 1r Invoker amb prou memòria per executar la funció
        }
        if (j >= cont.getNInvokers()) {
            throw new NotEnoughMemory(
                    "La funció que vols executar no pot ser executada per cap Invoker degut a la seva gran mida.");
        }

        Invoker inv = cont.getInvokers()[j]; // guardem l'Invoker que executarà la funcio
        R resFinal = (R) inv.runFunction(action, actionParam);
        inv.returnMem(action.getActionSizeMB()); // tornem mem a l'Invoker
        if(isAsync) cont.getSemafor().release();
        return resFinal;
    }
}
