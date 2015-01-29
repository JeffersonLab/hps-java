package org.hps.monitoring.subsys.et;

import org.hps.monitoring.plotting.MonitoringPlotFactory;
import org.hps.monitoring.subsys.SystemStatisticsImpl;
import org.hps.record.et.EtEventProcessor;
import org.jlab.coda.et.EtEvent;
import org.lcsim.util.aida.AIDA;

/**
 * A basic set of strip charts for monitoring the ET system.
 */
public final class EtSystemStripCharts extends EtEventProcessor {

    SystemStatisticsImpl stats = new SystemStatisticsImpl();
    MonitoringPlotFactory plotFactory = (MonitoringPlotFactory) AIDA.defaultInstance().analysisFactory().createPlotterFactory("ET System Monitoring");

    public EtSystemStripCharts() {
        stats.setTickLengthMillis(2000);
    }
    
    /**
     * Setup the strip charts for ET system monitoring and start accumulating statistics.
     */
    @Override
    public void startJob() {        
        plotFactory.createStripChart("Data Rate", "MB / second", 100, stats.new MegabytesPerSecondUpdater()); 
        plotFactory.createStripChart("Total Data", "Megabytes", 100, stats.new TotalMegabytesUpdater());      
        plotFactory.createStripChart("Event Rate", "Events / second",  100, stats.new EventsPerSecondUpdater());
        plotFactory.createStripChart("Total Events", "Number of Events", 100, stats.new TotalEventsUpdater());
        stats.start();
    }

    @Override
    public void process(EtEvent event) {
        stats.update(event.getLength());
    }

    @Override
    public void endJob() {
        stats.stop();
    }
}