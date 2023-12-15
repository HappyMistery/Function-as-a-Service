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
     */
    public static <T, R> R selectInvokerWithPolicy(Controller cont, Action action, T actionParam, int policy)
            throws NotEnoughMemory, PolicyNotDetected {
        switch (policy) {
            case 1:
                return RoundRobin(cont, action, actionParam);
            case 2:
                return GreedyGroup(cont, action, actionParam);
            case 3:
                return UniformGroup(cont, action, actionParam);
            case 4:
                return BigGroup(cont, action, actionParam);
            default:
                throw new PolicyNotDetected("No s'ha trobat la política seleccionada");
        }
    }

    /**
     * @param <T>
     * @param <R>
     * @param action
     * @param actionParam
     * @return
     * @throws NotEnoughMemory
     */
    public static <T, R> R RoundRobin(Controller cont, Action action, T actionParam) throws NotEnoughMemory {
        Invoker[] invs = new Invoker[cont.getNInvokers()];
        int j = 0;

        if (actionParam instanceof List<?>) { // si ens passen una llista de parametres
            checkForMemory(cont, action, actionParam, 1, 1); // comprovem que hi ha prou memoria per executar el grup de funcions
            // RoundRobin Policy - Distribueix uniformement les funcions entre els Invokers
            List<R> resFinal = new ArrayList<>(((List<?>) actionParam).size());

            for (int i = 0; i < ((List<?>) actionParam).size(); i++) {
                if (cont.getInvokers()[j].getAvailableMem() < action.getActionSizeMB()) {
                    continue;
                } else
                    resFinal.add((R) cont.getInvokers()[j].runFunction(action, ((List<?>) actionParam).get(i)));    // afegim el resultat de la funcio a la llista de resultats
                if (j == cont.getNInvokers() - 1) {
                    j = 0; // si hem arribat al final de la llista de Invokers, tornem a començar
                    checkForMemory(cont, action, actionParam, 1, 1); // comprovem que hi ha prou memoria per executar el grup de funcions
                } else {
                    j++;
                }
            }
            return (R) resFinal;
        }
        return soloFuncExec(cont, action, actionParam);
    }

    /**
     * @param <T>
     * @param <R>
     * @param action
     * @param actionParam
     * @return
     * @throws NotEnoughMemory
     */
    public static <T, R> R GreedyGroup(Controller cont, Action action, T actionParam) throws NotEnoughMemory {
        int i = 0;
        int count = 0;
        Invoker[] invs = new Invoker[cont.getNInvokers()];
        if (actionParam instanceof List<?>) { // si ens passen una llista de parametres
            checkForMemory(cont, action, actionParam, 1, 2); // comprovem que hi ha prou memoria per executar el grup de
            // funcions

            // GreedyGroup Policy - Omple tant com pot un invoker abans de passar al següent
            List<R> resFinal = new ArrayList<>(((List<?>) actionParam).size());
            int j = 0;
            for (i = 0; i < cont.getNInvokers(); i++) {
                if (cont.getInvokers()[i].getAvailableMem() < action.getActionSizeMB()) {
                    continue;
                } else {
                    invs[j] = cont.getInvokers()[i]; // agafem els invokers que puguin fer com a mínim 1 execució de la
                    // funció
                    j++;
                }
            }

            j = 0;
            for (i = 0; i < invs.length; i++) {
                count = 0;
                while (invs[i].getAvailableMem() >= action.getActionSizeMB() && j < ((List<?>) actionParam).size()) {
                    resFinal.add((R) invs[i].runFunction(action, ((List<?>) actionParam).get(j)));  // afegim el resultat de la funcio a la llista de resultats
                    j++;
                    count++; // comptador de funcions executades per l'Invoker i
                }
                invs[i].setAvailableMem(action.getActionSizeMB() * count); // tornem mem a l'Invoker
            }
            return (R) resFinal;
        }
        return soloFuncExec(cont, action, actionParam);
    }

    /**
     * @param <T>
     * @param <R>
     * @param action
     * @param actionParam
     * @return
     * @throws NotEnoughMemory
     */
    public static <T, R> R UniformGroup(Controller cont, Action action, T actionParam) throws NotEnoughMemory {
        if (actionParam instanceof List<?>) { // si ens passen una llista de parametres
            int groupSize = 3;
            checkForMemory(cont, action, actionParam, groupSize, 3); // comprovem que hi ha prou memoria per executar el grup de groupSize funcions
            List<R> resFinal = new ArrayList<>(((List<?>) actionParam).size());
            int numFuncs = ((List<?>) actionParam).size();
            int count = 0;
            int j = 0;
            int i = 0;
            while(count < numFuncs) {   //mentre tinguem funcions
                i = 0;
                while(i < groupSize && count < numFuncs) {
                    resFinal.add((R) cont.getInvokers()[j].runFunction(action, ((List<?>) actionParam).get(Math.min(count, numFuncs - 1))));
                    count++;
                    i++;
                }
                if(j == cont.getNInvokers() - 1) {
                    j = 0; //si hem arribat al final de la llista de Invokers, tornem a començar
                    checkForMemory(cont, action, actionParam, groupSize, 3); // comprovem que hi ha prou memoria per executar el grup de groupSize funcions
                }
                else
                    j++;
            }
            return (R) resFinal;
        }
        return soloFuncExec(cont, action, actionParam);
    }

    /**
     * @param <T>
     * @param <R>
     * @param action
     * @param actionParam
     * @return
     * @throws NotEnoughMemory
     */
    public static <T, R> R BigGroup(Controller cont, Action action, T actionParam) throws NotEnoughMemory {
        if (actionParam instanceof List<?>) { // si ens passen una llista de parametres
            int groupSize = 3;
            checkForMemory(cont, action, actionParam, 1, 4); // comprovem que hi ha prou memoria per executar el grup de funcions

            
        }
        return soloFuncExec(cont, action, actionParam);
    }

    /**
     * @param <T>
     * @param <R>
     * @param cont
     * @param action
     * @param actionParam
     * @throws NotEnoughMemory
     */
    private static <T, R> void checkForMemory(Controller cont, Action action, T actionParam, int groupSize, int policy) throws NotEnoughMemory {
        float foundMem = 0;
        float totalMemGroup = action.getActionSizeMB() * ((List<?>) actionParam).size();

        for (int i = 0; i < cont.getNInvokers(); i++) { // busquem tots els invokers amb prou memòria per executar la
            // funcio
            if (cont.getInvokers()[i].getAvailableMem() < (action.getActionSizeMB() * groupSize)) {
                continue; // si l'Invoker no té prou mem per executar la funcio al menys groupSize cop(s), passem al següent
            } else {
                foundMem += cont.getInvokers()[i].getAvailableMem();
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
    }

    /**
     * @param <T>
     * @param <R>
     * @param cont
     * @param action
     * @param actionParam
     * @return
     * @throws NotEnoughMemory
     */
    private static <T, R> R soloFuncExec(Controller cont, Action action, T actionParam) throws NotEnoughMemory {
        int j = 0;
        while ((j < cont.getNInvokers()) && (cont.getInvokers()[j].getAvailableMem() < action.getActionSizeMB())) {
            j++; // busquem el 1r Invoker amb prou memòria per executar la funció
        }
        if (j >= cont.getNInvokers()) {
            throw new NotEnoughMemory(
                    "La funció que vols executar no pot ser executada per cap Invoker degut a la seva gran mida.");
        }

        Invoker inv = cont.getInvokers()[j]; // guardem l'Invoker que executarà la funcio
        R resFinal = (R) inv.runFunction(action, actionParam);
        inv.setAvailableMem(action.getActionSizeMB()); // tornem mem a l'Invoker
        return resFinal;
    }
}
