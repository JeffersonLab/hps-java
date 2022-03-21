package org.hps.monitoring.subsys.ecal;

import org.hps.monitoring.plotting.MonitoringPlotFactory;
import org.hps.monitoring.plotting.StripChartUpdater;
import org.hps.monitoring.plotting.ValueProvider;
import org.jfree.data.time.Second;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Example Driver for plotting a sub-system's data using a strip chart.
 */
//FIXME: This class doesn't belong in this package because it is not a sub-system monitor.
public class EcalStripChartTestDriver extends Driver {

    int eventInterval = 1000;
    static String collectionName = "EcalReadoutHits";

    MonitoringPlotFactory plotFactory = 
            (MonitoringPlotFactory) AIDA.defaultInstance().analysisFactory().createPlotterFactory("ECAL System Monitoring");

    EventHeader currentEvent;
    int hits;
    
    int events;
    double averageHits;
    
    StripChartUpdater updater;
       
    public void startOfData() {
        plotFactory.createStripChart(
                "Average ECAL Hits per " + eventInterval + " Events", 
                "Hits", 
                1, 
                new String[] { "Date" }, 
                1, 
                new Second(), 
                new AverageHitsProvider(), 
                20000L);        
    }

    public void process(EventHeader event) {
        int size = event.get(RawCalorimeterHit.class, collectionName).size();
        ++events;
        hits += size;
        if (event.getEventNumber() % eventInterval == 0) {
            averageHits = (double) hits / (double) events;
            hits = 0;
            events = 0;
        }
    }
    
    public void endOfData() {
        updater.stop();
    }
    
    class AverageHitsProvider implements ValueProvider {
        public float[] getValues() {
            return new float[] {(float) averageHits};
        }
    }
}
