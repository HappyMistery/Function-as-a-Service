package models;

public interface ActionProxy<T, R> {
    R execute(T input) throws Exception;
    
}