package org.hps.monitoring.subsys.et;

import org.hps.monitoring.MonitoringPlotFactory;
import org.hps.monitoring.record.etevent.EtEventProcessor;
import org.hps.monitoring.subsys.SystemStatisticsImpl;
import org.jlab.coda.et.EtEvent;
import org.lcsim.util.aida.AIDA;

/**
 * A basic set of strip charts for monitoring the ET system.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EtSystemStripCharts extends EtEventProcessor { 
        
    SystemStatisticsImpl stats = new SystemStatisticsImpl();               
    MonitoringPlotFactory plotFactory = (MonitoringPlotFactory) 
            AIDA.defaultInstance().analysisFactory().createPlotterFactory("ET System Monitoring");    
    
    /**
     * Setup the strip charts for ET system monitoring and start accumulating statistics.
     */
    @Override
    public void startJob() {

        plotFactory.createStripChart(
                "Event Rate", 
                "Event Count", 
                100,
                stats.new EventRateUpdater());
        
        plotFactory.createStripChart(
                "Average Event Rate", 
                "Event Count", 
                100,
                stats.new AverageEventRateUpdater());
        
        plotFactory.createStripChart(
                "Events in Tick",
                "Event Count", 
                100,
                stats.new EventsInTickUpdater());

        plotFactory.createStripChart(
                "Cumulative Events", 
                "Event Count", 
                100,
                stats.new CumulativeEventsUpdater());
        
        plotFactory.createStripChart(
                "Data Rate", 
                "Bytes", 
                100,
                stats.new DataRateUpdater());
        
        plotFactory.createStripChart(
                "Bytes in Tick", 
                "Bytes", 
                100,
                stats.new BytesInTickUpdater());
        
        plotFactory.createStripChart(
                "Average Megabytes", 
                "Megabytes", 
                100, 
                stats.new AverageMbUpdater());
        
        plotFactory.createStripChart(
                "Cumulative Megabytes", 
                "Megabytes", 
                100, 
                stats.new CumulativeMbUpdater());
        
        stats.start();               
    }
    
    @Override
    public void processEvent(EtEvent event) {
        stats.update(event.getLength());
    }    
    
    @Override
    public void endJob() {
        stats.stop();
    }          
}
