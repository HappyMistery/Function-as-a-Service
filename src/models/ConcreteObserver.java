package models;

public class ConcreteObserver implements Observer {
    private MetricData receivedMetricData;

    @Override
    public void updateMetrics(MetricData metricData) {
        this.receivedMetricData = metricData;
    }

    public MetricData getReceivedMetricData() {
        return receivedMetricData;
    }
}