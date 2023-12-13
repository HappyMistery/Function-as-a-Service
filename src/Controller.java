package src;

import src.exceptions.*;
import java.util.HashMap;
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
        Invoker selectedInv = selectInvoker(actions.get(actionName).getActionSizeMB());
        return (R) selectedInv.runFunction(actions.get(actionName), actionParam);
    }

    public Invoker selectInvoker(float funcMem) throws NotEnoughMemory{
        int i = 0;
        while((i < nInvokers) && (invokers[i].getAvailableMem() < funcMem)) {
            i++;        //busquem un Invoker amb prou memòria per executar la funcio
        }
        if(i >= nInvokers)
            throw new NotEnoughMemory("La tens molt gran");
        return invokers[i];
    }
}
