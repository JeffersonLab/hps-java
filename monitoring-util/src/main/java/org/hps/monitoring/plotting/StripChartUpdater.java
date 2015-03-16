package org.hps.monitoring.plotting;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.data.time.DynamicTimeSeriesCollection;
import org.jfree.data.time.RegularTimePeriod;

/**
 * An abstract <tt>TimerTask</tt> to update a strip chart at a regular interval.
 */
public class StripChartUpdater {
    
    final JFreeChart chart;
    final DateAxis domainAxis;
    final DynamicTimeSeriesCollection dataset;
    final Timer timer;
    final TimerTask task;
    final Long rangeMillis;
    final ValueProvider valueProvider;
    long updateInterval;
        
    StripChartUpdater(JFreeChart chart, ValueProvider valueProvider, Long rangeMillis, RegularTimePeriod timeBase) {
        this.chart = chart;                        
        this.domainAxis = (DateAxis) chart.getXYPlot().getDomainAxis();
        this.dataset = (DynamicTimeSeriesCollection) chart.getXYPlot().getDataset();
        this.rangeMillis = rangeMillis;
        this.updateInterval = timeBase.getLastMillisecond() - timeBase.getFirstMillisecond();            
        this.valueProvider = valueProvider;
        timer = new Timer(chart.getTitle().getText() + " Timer");
        task = new TimerTask() {
    
            @Override
            public void run() {
                StripChartUpdater.this.chart.setNotify(false);
        
                dataset.advanceTime();
                long time = dataset.getNewestTime().getEnd().getTime();
                
                float values[] = StripChartUpdater.this.valueProvider.getValues();
                dataset.appendData(values);
        
                domainAxis.setRange(
                        new Date(time - StripChartUpdater.this.rangeMillis), 
                        new Date(time + updateInterval));

                StripChartUpdater.this.chart.setNotify(true);
                StripChartUpdater.this.chart.fireChartChanged();                  
            }            
        };
    }
    
    JFreeChart getChart() {
        return chart;
    }
     
    void start() {
        timer.scheduleAtFixedRate(task, 0, updateInterval);
    }        
    
    public void stop() {
        timer.cancel();
    }
}