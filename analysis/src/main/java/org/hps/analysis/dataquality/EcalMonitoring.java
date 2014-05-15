package org.hps.analysis.dataquality;

import hep.aida.IHistogram1D;
import java.util.HashMap;
import java.util.Map;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.geometry.Detector;


/**
 *
 * @author mgraham on Mar 28, 2014...just added empty (almost) file into svn
 * May 14, 2014 put some DQM template stuff in...ECal-ers should really fill in the guts 
 */

public class EcalMonitoring extends DataQualityMonitor {
    String readoutHitCollectionName="EcalReadoutHits";//these are in ADC counts
    String calibratedHitCollectionName="EcalCalHits";//these are in energy
    String clusterCollectionName = "EcalClusters";

    private Map<String, Double> monitoredQuantityMap = new HashMap<>();
    String[] ecalQuantNames = {"Good","Stuff","For","ECAL"};
    
     protected void detectorChanged(Detector detector) {
        System.out.println("EcalMonitoring::detectorChanged  Setting up the plotter");
        aida.tree().cd("/");

        //make some cool plots that will get saved in root format...2D is good too!
        IHistogram1D energy = aida.histogram1D("Cluster Energy", 25, 0, 2.5);
  
    }
     @Override
    public void process(EventHeader event) {
        /*  make sure everything is there */
        if (!event.hasCollection(RawCalorimeterHit.class, readoutHitCollectionName))
            return;
        
        
    }

    
    @Override
    public void dumpDQMData() {
        System.out.println("EcalMonitoring::endOfData filling DQM database");
    }

    @Override
    public void printDQMData() {
        System.out.println("EcalMonitoring::printDQMData");
       
        System.out.println("*******************************");
    }

    /**
     * Calculate the averages here and fill the map
     */
    @Override
    public void calculateEndOfRunQuantities() {
    }
    
}
