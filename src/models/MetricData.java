package models;

public class MetricData {
    private int executionTime;
    private Invoker invokerUsed;
    private int memoryUsage;

    public MetricData(int executionTime, Invoker invokerUsed, int memoryUsage) {
        this.executionTime = executionTime;
        this.invokerUsed = invokerUsed;
        this.memoryUsage = memoryUsage;
    }

    public int getExecutionTime() {
        return executionTime;
    }

    public Invoker getInvokerUsed() {
        return invokerUsed;
    }

    public int getMemoryUsage() {
        return memoryUsage;
    }
}