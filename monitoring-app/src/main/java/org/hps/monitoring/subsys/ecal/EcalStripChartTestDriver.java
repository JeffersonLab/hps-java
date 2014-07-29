package org.hps.monitoring.subsys.ecal;

import java.util.Date;
import java.util.TimerTask;

import org.hps.monitoring.plotting.MonitoringPlotFactory;
import org.hps.monitoring.plotting.StripChartUtil;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Proof of principle Driver for plotting a sub-system's data using a strip chart.
 */
public class EcalStripChartTestDriver extends Driver {
           
    int eventInterval = 1000;
    static String collectionName = "EcalReadoutHits";
    
    MonitoringPlotFactory plotFactory = (MonitoringPlotFactory) 
            AIDA.defaultInstance().analysisFactory().createPlotterFactory("ECAL System Monitoring");
    TimeSeries series;
    JFreeChart stripChart;
    TimerTask updateTask;
    EventHeader currentEvent;
    int hits;
    int events;
        
    public void startOfData() { 
        stripChart = plotFactory.createStripChart(
                "Average ECAL Hits per " + eventInterval + " Events", 
                "Hits", 
                99999999, /* max age */ 
                1000, /* max count */
                100000 /* range size */);
        series = StripChartUtil.getTimeSeries(stripChart);        
    }
    
    public void process(EventHeader event) {
        int size = event.get(RawCalorimeterHit.class, collectionName).size();
        ++events;
        hits += size;
        if (event.getEventNumber() % eventInterval == 0) {
            double averageHits = (double)hits / (double)events;
            series.add(new Millisecond(new Date()), averageHits);
            hits = 0;
            events = 0;
        } 
                
        //long millis = (long) ((double) event.getTimeStamp() / 1e6);
        //series.addOrUpdate(new Second(new Date(timestamp)), size);
        //series.addOrUpdate(new Millisecond(new Date(millis)), size);
    }
}
