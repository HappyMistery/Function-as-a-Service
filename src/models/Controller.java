package models;

import exceptions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;


public class Controller {
    private int nInvokers;
    private int totalSizeMB;
    private Map<String, Action> actions;
    private Invoker[] invokers;

    public Controller(int nInv, int tSizeMB){
        nInvokers = nInv;
        totalSizeMB = tSizeMB;
        actions = new HashMap<String, Action>();
        invokers = new Invoker[nInvokers];  //creem array d'Invoker.class de nInvokers elements
        for(int i = 0; i < nInvokers; i++) {
            invokers[i] = new Invoker(totalSizeMB/nInvokers);   //Inicialitzem cada Invoker de l'array
        }
    }

    public int getNInvokers() {
        return nInvokers;
    }

    public int getTotalSizeMB() {
        return totalSizeMB;
    }

    public <T, R> void registerAction(String actionName, Function<T, R> f, int actionSizeMB) {  //"public <T, R> void ..." fa mètode genèric
        Action<T, R> action = new Action<>(actionName, f, actionSizeMB);    //creem una Action<T, R> amb la info proporcionada
        actions.put(actionName, action);    //afegim la accio al HashMap d'accions
    }

    public <T, R> R invoke(String actionName, T actionParam) throws NotEnoughMemory {  //"public <T, R> R ..." fa mètode genèric
        if(actionParam instanceof List<?>) {    //si ens passen una llista de parametres
            float totalMemGroup = actions.get(actionName).getActionSizeMB() * ((List<?>) actionParam).size();
            float foundMem = 0;
            List<R> resFinal = new ArrayList<>(((List<?>) actionParam).size());
            for(int i = 0; i < nInvokers; i++) {
                if(invokers[i].getAvailableMem() < actions.get(actionName).getActionSizeMB()) {
                    continue;
                }
                else {
                    foundMem += invokers[i].getAvailableMem();
                }
            }

            if(foundMem < totalMemGroup) throw new NotEnoughMemory("Les funcions que vols executar no poden ser executades al complet.");

            int j = 0;
            for(int i = 0; i < nInvokers; i++) {
                if(invokers[i].getAvailableMem() < actions.get(actionName).getActionSizeMB()) {
                    continue;
                }
                else {
                    while(invokers[i].getAvailableMem() >= actions.get(actionName).getActionSizeMB() && j < ((List<?>) actionParam).size()) {
                        R res = (R) invokers[i].runFunction(actions.get(actionName), ((List<?>) actionParam).get(j));
                        j++;
                        resFinal.add(res);
                    }
                    invokers[i].setAvailableMem(actions.get(actionName).getActionSizeMB()); //tornem mem a l'Invoker
                }
            }
            return (R) resFinal;
        }
        Invoker selectedInv = selectInvoker(actions.get(actionName).getActionSizeMB());
        R res = (R) selectedInv.runFunction(actions.get(actionName), actionParam);
        selectedInv.setAvailableMem(actions.get(actionName).getActionSizeMB()); //tornem mem a l'Invoker
        return res;
    }

    public Invoker selectInvoker(float funcMem) throws NotEnoughMemory{
        int i = 0;
        while((i < nInvokers) && (invokers[i].getAvailableMem() < funcMem)) {
            i++;        //busquem un Invoker amb prou memòria per executar la funcio
        }
        if(i >= nInvokers)
            throw new NotEnoughMemory("La funció que vols executar no pot ser executada per cap Invoker degut a la seva gran mida.");
        return invokers[i];
    }
}
