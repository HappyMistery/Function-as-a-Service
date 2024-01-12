package models;

public interface ActionProxy<T, R> {
    /**
     * Executes the action with the given input through a proxy.
     * @param input input of the action
     * @return result of the action
     * @throws Exception
     */
    R execute(T input) throws Exception;
    
}