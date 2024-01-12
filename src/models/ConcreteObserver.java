package models;

public class ConcreteObserver implements Observer {
    private MetricData receivedMetricData;

    /**
     * Constructor for ConcreteObserver
     * @param metricData
     */
    @Override
    public void updateMetrics(MetricData metricData) {
        this.receivedMetricData = metricData;
    }

    /**
     * Getter for receivedMetricData
     * @return the receivedMetricData
     */
    public MetricData getReceivedMetricData() {
        return receivedMetricData;
    }
}