package org.hps.recon.tracking;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOUtil;
import org.lcsim.recon.tracking.digitization.sisim.CDFSiSensorSim;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHit;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;

/**
 * 
 * @author Matt Graham
 */
// TODO: Add documentation about what this Driver does. --JM
public class DataTrackerHitDriver extends Driver {

    // Debug switch for development.

    private boolean debug = false;
    // Collection name.
    // private String readoutCollectionName = "TrackerHits";
    // Subdetector name.
    private String subdetectorName = "Tracker";
    // Name of RawTrackerHit output collection.
    // private String rawTrackerHitOutputCollectionName = "RawTrackerHitMaker_RawTrackerHits";
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
    // Various data lists required by digitization.
    private List<String> processPaths = new ArrayList<String>();
    private List<IDetectorElement> processDEs = new ArrayList<IDetectorElement>();
    private Set<SiSensor> processSensors = new HashSet<SiSensor>();
    // Digi class objects.
    // private SiDigitizer stripDigitizer;
    // private HPSFittedRawTrackerHitMaker hitMaker;
    private StripMaker stripClusterer;
    // private DumbShaperFit shaperFit;
    int[][] counts = new int[2][10];

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    // public void setReadoutCollectionName(String readoutCollectionName) {
    // this.readoutCollectionName = readoutCollectionName;
    // }
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

    /**
     * Creates a new instance of TrackerHitDriver.
     */
    public DataTrackerHitDriver() {
    }

    /**
     * Do initialization once we get a Detector.
     */
    @Override
    public void detectorChanged(Detector detector) {

        // Call sub-Driver's detectorChanged methods.
        super.detectorChanged(detector);

        // Process detectors specified by path, otherwise process entire
        // detector
        IDetectorElement deDetector = detector.getDetectorElement();

        for (String path : processPaths) {
            processDEs.add(deDetector.findDetectorElement(path));
        }

        if (processDEs.isEmpty()) {
            processDEs.add(deDetector);
        }

        for (IDetectorElement detectorElement : processDEs) {
            processSensors.addAll(detectorElement.findDescendants(SiSensor.class));
            // if (debug)
            // System.out.println("added " + processSensors.size() + " sensors");
        }

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

        // hitMaker=new HPSFittedRawTrackerHitMaker(shaperFit);
        // Create the clusterers and set hit-making parameters.
        stripClusterer = new StripMaker(stripSim, stripClusteringAlgo);

        stripClusterer.setMaxClusterSize(clusterMaxSize);
        stripClusterer.setCentralStripAveragingThreshold(clusterCentralStripAveragingThreshold);

        // Set the cluster errors.
        
        DefaultSiliconResolutionModel model = new DefaultSiliconResolutionModel();

        model.setOneClusterErr(oneClusterErr);
        model.setTwoClusterErr(twoClusterErr);
        model.setThreeClusterErr(threeClusterErr);
        model.setFourClusterErr(fourClusterErr);
        model.setFiveClusterErr(fiveClusterErr);
        
        stripClusterer.setResolutionModel(model);
        

        // Set the detector to process.
        processPaths.add(subdetectorName);
    }

    /**
     * Perform the digitization.
     */
    @Override
    public void process(EventHeader event) {
        // Call sub-Driver processing.
        // super.process(event);

        // Make new lists for output.
        // List<HPSFittedRawTrackerHit> rawHits = new ArrayList<HPSFittedRawTrackerHit>();
        List<SiTrackerHit> stripHits1D = new ArrayList<SiTrackerHit>();

        // // Make HPS hits.
        // for (SiSensor sensor : processSensors) {
        // rawHits.addAll(hitMaker.makeHits(sensor));
        // }

        // Make strip hits.
        for (SiSensor sensor : processSensors) {
            stripHits1D.addAll(stripClusterer.makeHits(sensor));
        }

        // Debug prints.
        if (debug) {
            // List<SimTrackerHit> simHits = event.get(SimTrackerHit.class,
            // this.readoutCollectionName);
            // System.out.println("SimTrackerHit collection " + this.readoutCollectionName +
            // " has " + simHits.size() + " hits.");
            // System.out.println("RawTrackerHit collection " +
            // this.rawTrackerHitOutputCollectionName + " has " + rawHits.size() + " hits.");
            System.out.println("TrackerHit collection " + this.stripHitOutputCollectionName + " has " + stripHits1D.size() + " hits.");
        }

        // Put output hits into collection.
        int flag = LCIOUtil.bitSet(0, 31, true); // Turn on 64-bit cell ID.
        // event.put(this.rawTrackerHitOutputCollectionName, rawHits, RawTrackerHit.class, flag,
        // toString());
        event.put(this.stripHitOutputCollectionName, stripHits1D, SiTrackerHitStrip1D.class, 0, toString());
        if (debug) {
            System.out.println("[ DataTrackerHitDriver ] - " + this.stripHitOutputCollectionName + " has " + stripHits1D.size() + " hits.");
        }
    }

    @Override
    public void endOfData() {
        if (debug) {
            for (int mod = 0; mod < 2; mod++) {
                for (int layer = 0; layer < 10; layer++) {
                    System.out.format("mod %d, layer %d, count %d\n", mod, layer, counts[mod][layer]);
                }
            }
        }
    }
}
