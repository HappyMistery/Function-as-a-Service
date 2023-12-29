package models;

import exceptions.NotEnoughMemory;
import exceptions.PolicyNotDetected;

import java.util.Map;
import java.util.HashMap;


public class MemoizationDecorator extends Controller {

    private final Map memoizationCache = new HashMap<>();
    private final Controller controller;

    public MemoizationDecorator(Controller controller) {
        super(controller.getNInvokers(), controller.getTotalSizeMB());
        this.controller = controller;
    }


    @Override
    public <T, R> R invoke(String actionName, T actionParam, int policy) throws NotEnoughMemory, PolicyNotDetected, InterruptedException {
        String cacheKey = actionName + "-" + actionParam.toString();    // Generar una unique key personalitzada para cada acció

        R memoizedResult = getFromCache(cacheKey);
        if (memoizedResult != null) {
            return memoizedResult;      //si el resultat està a la caché, retornar-lo
        }

        R result = controller.invoke(actionName, actionParam, policy);  //si no, calcular-lo i guardar-lo a la caché
        saveToCache(cacheKey, result);

        return result;
    }

    private <R> R getFromCache(String cacheKey) {
        R result = (R) memoizationCache.get(cacheKey);
        return result;
    }

    private <R> void saveToCache(String cacheKey, R result) {
        memoizationCache.put(cacheKey, result);
    }

}