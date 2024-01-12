package models;

public interface Observer {

    /**
     * Updates the metrics of the invokers
     * @param metricData metric data to update
     */
    public void updateMetrics(MetricData metricData);
}
