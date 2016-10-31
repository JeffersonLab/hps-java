package org.hps.users.omoreno;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hep.aida.IHistogramFactory;
import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.ITree;
import hep.aida.ref.rootwriter.RootFileStore;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;

/**
 *
 */
public class SvtHitPerformance extends TriggerFilter {

    //---------------//
    //   Constants   //
    //---------------//
    private static final String SUBDETECTOR_NAME = "Tracker";

    //--------------//
    //--------------//

    // Container for HPS sensor objects
    private List<HpsSiSensor> sensors = null;
    private Map<HpsSiSensor, MutableInt> hitMultiplicityMap     = new HashMap<HpsSiSensor, MutableInt>();
    private Map<HpsSiSensor, MutableInt> clusterMultiplicityMap = new HashMap<HpsSiSensor, MutableInt>();

    //-----------------//
    //   Collections   //
    //-----------------//
    private static final String RAW_HIT_COLLECTION = "SVTRawTrackerHits";
    private static final String CLUSTER_COLLECTION = "StripClusterer_SiTrackerHitStrip1D";

    //--------------//
    //--------------//

    //--------------//
    //   Plotting   //
    //--------------//
    private ITree tree = null; 
    private IHistogramFactory histogramFactory = null; 

    // Raw hits per sensor 
    private Map<HpsSiSensor, IHistogram1D> rawHitPlots = new HashMap<HpsSiSensor, IHistogram1D>();
    
    // Clusters per sensor
    private Map<HpsSiSensor, IHistogram1D> clusterMultPlots = new HashMap<HpsSiSensor, IHistogram1D>();

    //--------------//
    //--------------//

    public void detectorChanged(Detector detector) { 

        // Instantiate the tree and histogram factory
        tree = IAnalysisFactory.create().createTreeFactory().create();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);

        // Get the HpsSiSensor objects from the tracker detector element
        sensors = detector.getSubdetector(SUBDETECTOR_NAME).getDetectorElement().findDescendants(HpsSiSensor.class);

        // Associate plots with each of the sensors.
        for (HpsSiSensor sensor : sensors) {

            String sensorName = sensor.getName();

            // Book a histogram of the raw hit multiplicity for each of the sensors. 
            this.rawHitPlots.put(sensor, 
                    histogramFactory.createHistogram1D(sensorName + " - Raw Hit Multiplicity", 101, 0, 100));
            this.clusterMultPlots.put(sensor, 
                    histogramFactory.createHistogram1D(sensorName + " - Cluster Multiplicity", 101, 0, 100));
            
            this.hitMultiplicityMap.put(sensor, new MutableInt());
            this.clusterMultiplicityMap.put(sensor, new MutableInt());
        }
    }

    public void process(EventHeader event) { 

        super.process(event);
        
        if (!triggerFound) return; 
        
        // If the event doesn't have a collection of raw hits, skip it. This 
        // should never happen.  If an event doesn't have any raw hits 
        // from the SVT, an empty collection is still placed into the event.
        if (!event.hasCollection(RawTrackerHit.class, RAW_HIT_COLLECTION)) return;

        // Get the list of raw hits from the event. 
        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, RAW_HIT_COLLECTION);

        // Reset the map keeping count of the hits per sensor.
        this.resetMultiplicityMaps();

        // Loop through all of the hits in the event and calculate various quantities.
        for (RawTrackerHit rawHit : rawHits) { 

            // Get the sensor associated with this hit.
            HpsSiSensor sensor = (HpsSiSensor) rawHit.getDetectorElement();

            // Increament the hit count on that sensor
            this.hitMultiplicityMap.get(sensor).increment();
        }

        // Fill the hit multiplicity plots.
        for (Map.Entry<HpsSiSensor, MutableInt> entry : this.hitMultiplicityMap.entrySet()) { 
            rawHitPlots.get(entry.getKey()).fill(entry.getValue().get());
        }
        
        // Check if the event has a collection of clusters.  If not, stop processing
        // the event.
        if (!event.hasCollection(SiTrackerHitStrip1D.class, CLUSTER_COLLECTION)) return;

        // Get the collection of clusters from the event.
        List<SiTrackerHitStrip1D> clusters = event.get(SiTrackerHitStrip1D.class, CLUSTER_COLLECTION);
        
        // Loop through all of the clusters in the event and calculate various quantities.
        for (SiTrackerHitStrip1D cluster : clusters) { 

            // Get the sensor associated with this cluster.
            HpsSiSensor sensor = (HpsSiSensor) cluster.getRawHits().get(0).getDetectorElement();
            
            // Increment the cluster count on that sensor
            this.clusterMultiplicityMap.get(sensor).increment();
        }
        
        // Fill the cluster multiplicity plots.
        for (Map.Entry<HpsSiSensor, MutableInt> entry : this.clusterMultiplicityMap.entrySet()) { 
            clusterMultPlots.get(entry.getKey()).fill(entry.getValue().get());
        }
    }

    @Override
    public void endOfData(){
        String rootFile = "hit_performance.root";
        RootFileStore store = new RootFileStore(rootFile);
        try {
            store.open();
            store.add(tree);
            store.close(); 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void resetMultiplicityMaps() { 
        for (Map.Entry<HpsSiSensor, MutableInt> entry : this.hitMultiplicityMap.entrySet()) { 
            entry.getValue().reset();;
        }
        for (Map.Entry<HpsSiSensor, MutableInt> entry : this.clusterMultiplicityMap.entrySet()) { 
            entry.getValue().reset();;
        }
    }

    class MutableInt { 
        int value = 0; 
        public void increment() { ++value; }
        public int get() { return value; }
        public void reset() { value = 0; }
    }
}
