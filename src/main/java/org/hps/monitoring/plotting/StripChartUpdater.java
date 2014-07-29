package org.hps.monitoring.plotting;

import java.util.Timer;
import java.util.TimerTask;

import org.jfree.chart.JFreeChart;
import org.jfree.data.time.DynamicTimeSeriesCollection;

/**
 * An abstract <tt>TimerTask</tt> to update a strip chart at a regular interval.
 */
public abstract class StripChartUpdater extends TimerTask {

    DynamicTimeSeriesCollection dataset;
    long updateIntervalMillis = 1000;
    
    public StripChartUpdater() {        
    }
    
    public void setChart(JFreeChart chart) {
        this.dataset = (DynamicTimeSeriesCollection)chart.getXYPlot().getDataset();
    }
    
    public void setUpdateIntervalMillis(long updateIntervalMillis) {
        this.updateIntervalMillis = updateIntervalMillis;
    }

    public void run() {
        dataset.advanceTime();
        dataset.appendData(new float[] { nextValue() });
    }

    public void schedule(Timer timer) {
        timer.schedule(this, 0, updateIntervalMillis);
    }

    public abstract float nextValue();
}