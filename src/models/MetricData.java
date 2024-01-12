package models;

public class MetricData {
    private int executionTime;
    private Invoker invokerUsed;
    private int memoryUsage;

    /**
     * Constructor for MetricData
     * @param executionTime time of execution of the action
     * @param invokerUsed invoker used to execute the action
     * @param memoryUsage memory used to execute the action
     */
    public MetricData(int executionTime, Invoker invokerUsed, int memoryUsage) {
        this.executionTime = executionTime;
        this.invokerUsed = invokerUsed;
        this.memoryUsage = memoryUsage;
    }

    /**
     * Getter for the execution time of the action
     * @return execution time of the action
     */
    public int getExecutionTime() {
        return executionTime;
    }

    /**
     * Getter for the invoker used to execute the action
     * @return invoker used to execute the action
     */
    public Invoker getInvokerUsed() {
        return invokerUsed;
    }

    /**
     * Getter for the memory used to execute the action
     * @return memory used to execute the action
     */
    public int getMemoryUsage() {
        return memoryUsage;
    }
}