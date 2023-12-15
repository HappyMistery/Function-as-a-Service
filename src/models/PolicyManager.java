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
    public static <T, R> R selectInvokerWithPolicy(Controller cont, Action action, T actionParam, int policy) throws NotEnoughMemory, PolicyNotDetected{
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
    public static <T, R> R RoundRobin(Controller cont, Action action, T actionParam) throws NotEnoughMemory{
        Invoker[] invs = new Invoker[cont.getNInvokers()];
        if(actionParam instanceof List<?>) {    //si ens passen una llista de parametres
            checkForMemory(cont, action, actionParam);  //comprovem que hi ha prou memoria per executar el grup de funcions

            //RoundRobin Policy - Distribueix uniformement les funcions entre els Invokers
            int nFunc = ((List<?>) actionParam).size();
        }
        return null;

    }


    /**
     * @param <T>
     * @param <R>
     * @param action
     * @param actionParam
     * @return
     * @throws NotEnoughMemory
     */
    public static <T, R> R GreedyGroup(Controller cont, Action action, T actionParam) throws NotEnoughMemory{
        int i = 0;
        int count = 0;
        Invoker[] invs = new Invoker[cont.getNInvokers()];
        if(actionParam instanceof List<?>) {    //si ens passen una llista de parametres
            checkForMemory(cont, action, actionParam);  //comprovem que hi ha prou memoria per executar el grup de funcions

            //GreedyGroup Policy - Omple tant com pot un invoker abans de passar al següent
            List<R> resFinal = new ArrayList<>(((List<?>) actionParam).size());
            int j = 0;
            for(i = 0; i < cont.getNInvokers(); i++) {
                if(cont.getInvokers()[i].getAvailableMem() < action.getActionSizeMB()) {
                    continue;
                }
                else {
                    invs[j] = cont.getInvokers()[i];    //agafem els invokers que puguin fer com a mínim 1 execució de la funció
                    j++;
                }
            }

            j = 0;
            for(i = 0; i < invs.length; i++) {
                count = 0;
                while(invs[i].getAvailableMem() >= action.getActionSizeMB() && j < ((List<?>) actionParam).size()) {
                    R res = (R) invs[i].runFunction(action, ((List<?>) actionParam).get(j));
                    j++;
                    resFinal.add(res);  //afegim el resultat de la funcio a la llista de resultats
                    count++;    //comptador de funcions executades per l'Invoker i
                }
                invs[i].setAvailableMem(action.getActionSizeMB() * count); //tornem mem a l'Invoker
            }

            return (R) resFinal;
        }
        
        while((i < cont.getNInvokers()) && (cont.getInvokers()[i].getAvailableMem() < action.getActionSizeMB())) {
            i++;        //busquem el 1r Invoker amb prou memòria per executar la funcio
        }
        if(i >= cont.getNInvokers())
            throw new NotEnoughMemory("La funció que vols executar no pot ser executada per cap Invoker degut a la seva gran mida.");
        
        invs[0] = cont.getInvokers()[i];  //guardem l'Invoker que executarà la funcio
        R resFinal = (R) invs[0].runFunction(action, actionParam);
        invs[0].setAvailableMem(action.getActionSizeMB()); //tornem mem a l'Invoker
        return resFinal;
    }

    /**
     * @param <T>
     * @param <R>
     * @param action
     * @param actionParam
     * @return
     * @throws NotEnoughMemory
     */
    public static <T, R> R UniformGroup(Controller cont, Action action, T actionParam) throws NotEnoughMemory{
            return null;
        }


    /**
     * @param <T>
     * @param <R>
     * @param action
     * @param actionParam
     * @return
     * @throws NotEnoughMemory
     */
    public static <T, R> R BigGroup(Controller cont, Action action, T actionParam) throws NotEnoughMemory{
            return null;
    }


    private static <T, R> void checkForMemory(Controller cont, Action action, T actionParam) throws NotEnoughMemory {
        float foundMem = 0;
        float totalMemGroup = action.getActionSizeMB() * ((List<?>) actionParam).size();

        for(int i = 0; i < cont.getNInvokers(); i++) {  //busquem tots els invokers amb prou memòria per executar la funcio
                if(cont.getInvokers()[i].getAvailableMem() < action.getActionSizeMB()) {
                    continue;   //si l'Invoker no té prou mem per executar la funcio al menys 1 cop, passem al següent
                }
                else 
                    foundMem += cont.getInvokers()[i].getAvailableMem();  
            }
            if(foundMem < totalMemGroup) throw new NotEnoughMemory("Les funcions que vols executar no poden ser executades al complet.");
    }
}
