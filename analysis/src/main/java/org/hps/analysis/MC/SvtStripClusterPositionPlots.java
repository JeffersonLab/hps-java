/**
 * This driver plots tracker hit positions and 1D strip cluster positions
 */
/**
 * @author mrsolt
 *
 */
package org.hps.analysis.MC;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogramFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.ITree;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimTrackerHit;
//import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class SvtStripClusterPositionPlots extends Driver {

    // Plotting
    protected AIDA aida = AIDA.defaultInstance();
    ITree tree; 
    IHistogramFactory histogramFactory; 

    //List of Sensors
    private List<HpsSiSensor> sensors = null; 
    
    Map<String, IHistogram1D> clusterHitPosZ = new HashMap<String,IHistogram1D>();
    Map<String, IHistogram1D> trackerHitPosZ = new HashMap<String,IHistogram1D>();
    Map<String, IHistogram2D> clusterHitPosXY = new HashMap<String,IHistogram2D>();
    Map<String, IHistogram2D> trackerHitPosXY = new HashMap<String,IHistogram2D>();
    
    //Histogram Settings
    double minX = -40;
    double maxX = 40;
    double minY = -20;
    double maxY = 20;
    double minZ = 0;
    double maxZ = 800;
    int nBins = 200;
    
    //Collection Strings
    private String trackerHitsCollectionName = "TrackerHits";
    private String stripClustersCollectionName = "StripClusterer_SiTrackerHitStrip1D";
   
    //Constants
    private static final String SUBDETECTOR_NAME = "Tracker";
    
    public void detectorChanged(Detector detector){

        aida.tree().cd("/");
        tree = aida.tree();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);
        
        // Get the HpsSiSensor objects from the tracker detector element
        sensors = detector.getSubdetector(SUBDETECTOR_NAME)
                          .getDetectorElement().findDescendants(HpsSiSensor.class);
   
        // If the detector element had no sensors associated with it, throw
        // an exception
        if (sensors.size() == 0) {
            throw new RuntimeException("No sensors were found in this detector.");
        }
        
        for(HpsSiSensor sensor:sensors){
            String name = sensor.getName();
            clusterHitPosXY.put(name,histogramFactory.createHistogram2D(name + " Cluster Hit Position XY", nBins, minX, maxX, nBins, minY, maxY));
            trackerHitPosXY.put(name,histogramFactory.createHistogram2D(name + " Tracker Hit Position XY", nBins, minX, maxX, nBins, minY, maxY));
            clusterHitPosZ.put(name,histogramFactory.createHistogram1D(name + " Cluster Hit Position Z", nBins, minZ, maxZ));
            trackerHitPosZ.put(name,histogramFactory.createHistogram1D(name + " Tracker Hit Position Z", nBins, minZ, maxZ));
        }

    }

    public void process(EventHeader event){
        aida.tree().cd("/");
        
        List<SimTrackerHit> trackerhits = event.get(SimTrackerHit.class,trackerHitsCollectionName);
        List<SiTrackerHitStrip1D> stripclusters = event.get(SiTrackerHitStrip1D.class,stripClustersCollectionName);

        for(SimTrackerHit hit:trackerhits){
            double[] pos = hit.getPosition();
            //HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement();
            HpsSiSensor sensor = (HpsSiSensor) hit.getDetectorElement();
            String name = sensor.getName();
            trackerHitPosZ.get(name).fill(pos[2]);
            trackerHitPosXY.get(name).fill(pos[0],pos[1]);
        }
        
        for(SiTrackerHitStrip1D hit:stripclusters){
            double[] pos = hit.getPosition();
            HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement();
            String name = sensor.getName();
            clusterHitPosZ.get(name).fill(pos[2]);
            clusterHitPosXY.get(name).fill(pos[0],pos[1]);
        }
    }
}