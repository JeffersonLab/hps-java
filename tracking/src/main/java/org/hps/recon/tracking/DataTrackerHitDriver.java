package org.hps.recon.tracking;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOUtil;
import org.lcsim.recon.tracking.digitization.sisim.CDFSiSensorSim;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHit;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;

public class DataTrackerHitDriver extends Driver {

    // Debug switch for development.
    private boolean debug = false;
    
    // Subdetector name.
    private String subdetectorName = "Tracker";

    // Name of StripHit1D output collection.
    private String stripHitOutputCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    
    // Clustering parameters.
    private double clusterSeedThreshold = 4.0;
    private double clusterNeighborThreshold = 3.0;
    private double clusterThreshold = 4.0;
    private double meanTime = 0.0;
    private double timeWindow = 72.0;
    private double neighborDeltaT = 24.0;
    private int clusterMaxSize = 10;
    private int clusterCentralStripAveragingThreshold = 4;
    
    // Clustering errors by number of TrackerHits.
    private static final double clusterErrorMultiplier = 1.0;
    private double oneClusterErr = clusterErrorMultiplier / Math.sqrt(12.);
    private double twoClusterErr = clusterErrorMultiplier / 5.0;
    private double threeClusterErr = clusterErrorMultiplier / 3.0;
    private double fourClusterErr = clusterErrorMultiplier / 2.0;
    private double fiveClusterErr = clusterErrorMultiplier / 1.0;
    
    // Weight the hits in a cluster by charge? (if not, all hits have equal 
    // weight)
    private boolean useWeights = true;
    
    // Clusterer
    private StripMaker stripClusterer;

    // List of sensors to process
    private List<SiSensor> sensors;  

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setSubdetectorName(String subdetectorName) {
        this.subdetectorName = subdetectorName;
    }

    public void setStripHitOutputCollectionName(String stripHitOutputCollectionName) {
        this.stripHitOutputCollectionName = stripHitOutputCollectionName;
    }

    public void setClusterSeedThreshold(double clusterSeedThreshold) {
        this.clusterSeedThreshold = clusterSeedThreshold;
    }

    public void setClusterNeighborThreshold(double clusterNeighborThreshold) {
        this.clusterNeighborThreshold = clusterNeighborThreshold;
    }

    public void setClusterThreshold(double clusterThreshold) {
        this.clusterThreshold = clusterThreshold;
    }

    public void setMeanTime(double meanTime) {
        this.meanTime = meanTime;
    }

    public void setTimeWindow(double timeWindow) {
        this.timeWindow = timeWindow;
    }

    public void setNeighborDeltaT(double neighborDeltaT) {
        this.neighborDeltaT = neighborDeltaT;
    }

    public void setClusterMaxSize(int clusterMaxSize) {
        this.clusterMaxSize = clusterMaxSize;
    }

    public void setClusterCentralStripAveragingThreshold(int clusterCentralStripAveragingThreshold) {
        this.clusterCentralStripAveragingThreshold = clusterCentralStripAveragingThreshold;
    }

    public void setOneClusterErr(double oneClusterErr) {
        this.oneClusterErr = oneClusterErr;
    }

    public void setTwoClusterErr(double twoClusterErr) {
        this.twoClusterErr = twoClusterErr;
    }

    public void setThreeClusterErr(double threeClusterErr) {
        this.threeClusterErr = threeClusterErr;
    }

    public void setFourClusterErr(double fourClusterErr) {
        this.fourClusterErr = fourClusterErr;
    }

    public void setFiveClusterErr(double fiveClusterErr) {
        this.fiveClusterErr = fiveClusterErr;
    }

    public void setUseWeights(boolean useWeights) {
        this.useWeights = useWeights;
    }

    /// Constructor 
    public DataTrackerHitDriver() {
    }

    /**
     * Do initialization once we get a Detector.
     */
    @Override
    public void detectorChanged(Detector detector) {

        // Get the collection of sensors to process
        sensors = detector.getSubdetector("Tracker").getDetectorElement().findDescendants(SiSensor.class);
        
        // Create the sensor simulation.
        CDFSiSensorSim stripSim = new CDFSiSensorSim();

        // Create Strip clustering algorithm.
        NearestNeighborRMSClusterer stripClusteringAlgo = new NearestNeighborRMSClusterer();
        stripClusteringAlgo.setSeedThreshold(clusterSeedThreshold);
        stripClusteringAlgo.setNeighborThreshold(clusterNeighborThreshold);
        stripClusteringAlgo.setClusterThreshold(clusterThreshold);
        stripClusteringAlgo.setMeanTime(meanTime);
        stripClusteringAlgo.setTimeWindow(timeWindow);
        stripClusteringAlgo.setNeighborDeltaT(neighborDeltaT);

        stripClusterer = new StripMaker(stripSim, stripClusteringAlgo);
        stripClusterer.setMaxClusterSize(clusterMaxSize);
        stripClusterer.setCentralStripAveragingThreshold(clusterCentralStripAveragingThreshold);
        stripClusterer.setDebug(debug);

        // Set the cluster errors.
        DefaultSiliconResolutionModel model = new DefaultSiliconResolutionModel();

        model.setOneClusterErr(oneClusterErr);
        model.setTwoClusterErr(twoClusterErr);
        model.setThreeClusterErr(threeClusterErr);
        model.setFourClusterErr(fourClusterErr);
        model.setFiveClusterErr(fiveClusterErr);
        model.setUseWeights(useWeights);

        stripClusterer.setResolutionModel(model);
    }

    /**
     * Perform the digitization.
     */
    @Override
    public void process(EventHeader event) {

        // Create the collection of 1D strip hits    
        List<SiTrackerHit> stripHits1D = new ArrayList<SiTrackerHit>();

        // Cluster fitted raw hits
        for (SiSensor sensor : sensors) stripHits1D.addAll(stripClusterer.makeHits(sensor));

        // Put the clusters into the event 
        int flag = LCIOUtil.bitSet(0, 31, true);
        event.put(this.stripHitOutputCollectionName, stripHits1D, SiTrackerHitStrip1D.class, 0, toString());
    
        if (debug)
            System.out.println("[ DataTrackerHitDriver ] - " + this.stripHitOutputCollectionName + " has " + stripHits1D.size() + " hits.");
        
    }
}
