package org.hps.monitoring.drivers.hodoscope;

import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import java.util.HashMap;
import java.util.Map;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 *
 * @author mgraham
 */
public class HodoscopePlots extends Driver {

    private static Map<String, IPlotter> plotters = new HashMap<String, IPlotter>();

    private static Map<String, IHistogram1D> occupancyPlots = new HashMap<String, IHistogram1D>();
    private static Map<String, int[]> occupancyMap = new HashMap<String, int[]>();
    private static final String SUBDETECTOR_NAME = "Tracker"; // CHANGE THIS to whatever the hodoscope is
    private String rawTrackerHitCollectionName = "SVTRawTrackerHits"; //CHANGE THIS to raw hodoscope hits
    private String triggerBankCollectionName = "TriggerBank";
    private String inputCollection = "EcalReadoutHits";
    private String clusterCollection = "EcalClusters";
    
       @Override
    protected void detectorChanged(Detector detector) {

        
    }

}
