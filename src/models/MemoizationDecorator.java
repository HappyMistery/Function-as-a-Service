package models;

import exceptions.*;

import java.util.Map;
import java.util.HashMap;


public class MemoizationDecorator extends Controller {

    private final Map memoizationCache = new HashMap<>();
    private final Controller controller;

    /**
     * Constructor for MemoizationDecorator
     * @param controller controller to decorate
     */
    public MemoizationDecorator(Controller controller) {
        super(controller.getNInvokers(), controller.getTotalSizeMB());
        this.controller = controller;
    }


    /**
     * Invokes an action with a given parameter and a policy and uses memoization to save the result
     * @param <T> type of the parameter
     * @param <R> type of the result
     * @param actionName name of the action to invoke
     * @param actionParam parameter of the action to invoke
     * @param policy policy to apply
     * @return result of the action
     * @throws NotEnoughMemory
     * @throws PolicyNotDetected
     * @throws InterruptedException
     */
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

    /**
     * Gets the result of an action from the cache
     * @param <R> type of the result
     * @param cacheKey key of the cache
     * @return result of the action
     */
    private <R> R getFromCache(String cacheKey) {
        R result = (R) memoizationCache.get(cacheKey);
        return result;
    }

    private <R> void saveToCache(String cacheKey, R result) {
        memoizationCache.put(cacheKey, result);
    }

}