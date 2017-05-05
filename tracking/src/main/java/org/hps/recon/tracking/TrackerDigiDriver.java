package org.hps.recon.tracking;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiTrackerModule;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOUtil;
import org.lcsim.recon.tracking.digitization.sisim.BasicReadoutChip;
import org.lcsim.recon.tracking.digitization.sisim.CDFSiSensorSim;
import org.lcsim.recon.tracking.digitization.sisim.NearestNeighborRMS;
import org.lcsim.recon.tracking.digitization.sisim.RawTrackerHitMaker;
import org.lcsim.recon.tracking.digitization.sisim.SiDigitizer;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHit;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.digitization.sisim.StripHitMaker;
import org.lcsim.recon.tracking.digitization.sisim.config.SimTrackerHitReadoutDriver;
import org.lcsim.util.Driver;

/**
 * This Driver runs the tracker digitization to create raw hits and strip hits from simulated data.
 * The output can be used by a track reconstruction algorithm like Seed Tracker.
 */
public class TrackerDigiDriver extends Driver {

    // Debug switch for development.

    private boolean debug = false;
    // Collection name.
    protected String readoutCollectionName = "TrackerHits";
    // Subdetector name.
    protected String subdetectorName = "Tracker";
    // Name of RawTrackerHit output collection.
    private String rawTrackerHitOutputCollectionName = "RawTrackerHitMaker_RawTrackerHits";
    // Name of StripHit1D output collection.
    private String stripHitOutputCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    // Readout parameters.
    protected double readoutNoiseIntercept = 270.0;
    protected double readoutNoiseSlope = 36.0;
    protected double readoutNoiseThreshold = 4.0;
    protected double readoutNeighborThreshold = 4.0;
    protected int readoutNBits = 10;
    protected int readoutDynamicRange = 40;
    // Clustering parameters.
    protected double clusterSeedThreshold = 4.0;
    protected double clusterNeighborThreshold = 3.0;
    protected double clusterThreshold = 4.0;
    protected int clusterMaxSize = 10;
    protected int clusterCentralStripAveragingThreshold = 4;
    // Clustering errors by number of TrackerHits.
    private static final double clusterErrorMultiplier = 1.0;
    protected double oneClusterErr = clusterErrorMultiplier / Math.sqrt(12.);
    protected double twoClusterErr = clusterErrorMultiplier / 5.0;
    protected double threeClusterErr = clusterErrorMultiplier / 3.0;
    protected double fourClusterErr = clusterErrorMultiplier / 2.0;
    protected double fiveClusterErr = clusterErrorMultiplier / 1.0;
    // Various data lists required by digitization.
    protected List<String> readouts = new ArrayList<String>();
    protected List<String> processPaths = new ArrayList<String>();
    private List<IDetectorElement> processDEs = new ArrayList<IDetectorElement>();
    private Set<SiSensor> processSensors = new HashSet<SiSensor>();
    private Set<SiTrackerModule> processModules = new HashSet<SiTrackerModule>();
    // Digi class objects.
    protected SiDigitizer stripDigitizer;
    protected StripHitMaker stripClusterer;

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setReadoutCollectionName(String readoutCollectionName) {
        this.readoutCollectionName = readoutCollectionName;
    }

    public void setSubdetectorName(String subdetectorName) {
        this.subdetectorName = subdetectorName;
    }

    public void setRawTrackerHitOutputCollectionName(String rawTrackerHitOutputCollectionName) {
        this.rawTrackerHitOutputCollectionName = rawTrackerHitOutputCollectionName;
    }

    public void setStripHitOutputCollectionName(String stripHitOutputCollectionName) {
        this.stripHitOutputCollectionName = stripHitOutputCollectionName;
    }

    public void setReadoutNoiseIntercept(double readoutNoiseIntercept) {
        this.readoutNoiseIntercept = readoutNoiseIntercept;
    }

    public void setReadoutNoiseSlope(double readoutNoiseSlope) {
        this.readoutNoiseSlope = readoutNoiseSlope;
    }

    public void setReadoutNeighborThreshold(double readoutNeighborThreshold) {
        this.readoutNeighborThreshold = readoutNeighborThreshold;
    }

    public void setReadoutNBits(int readoutNBits) {
        this.readoutNBits = readoutNBits;
    }

    public void setReadoutDynamicRange(int readoutDynamicRange) {
        this.readoutDynamicRange = readoutDynamicRange;
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
    public TrackerDigiDriver() {
    }

    /**
     * Initializes this Driver's objects with the job parameters.
     */
    protected void initialize() {

        // Create the sensor simulation.
        CDFSiSensorSim stripSim = new CDFSiSensorSim();

        // Create the readout chips and set the noise parameters.
        BasicReadoutChip stripReadout = new BasicReadoutChip();
        stripReadout.setNoiseIntercept(readoutNoiseIntercept);
        stripReadout.setNoiseSlope(readoutNoiseSlope);
        stripReadout.setNoiseThreshold(readoutNoiseThreshold);
        stripReadout.setNeighborThreshold(readoutNeighborThreshold);
        stripReadout.setNbits(readoutNBits);
        stripReadout.setDynamicRange(readoutDynamicRange);

        // Create the digitizer that produces the raw hits
        stripDigitizer = new RawTrackerHitMaker(stripSim, stripReadout);

        // Create Strip clustering algorithm.
        NearestNeighborRMS stripClusteringAlgo = new NearestNeighborRMS();
        stripClusteringAlgo.setSeedThreshold(clusterSeedThreshold);
        stripClusteringAlgo.setNeighborThreshold(clusterNeighborThreshold);
        stripClusteringAlgo.setClusterThreshold(clusterThreshold);

        // Create the clusterers and set hit-making parameters.
        stripClusterer = new StripHitMaker(stripSim, stripReadout, stripClusteringAlgo);
        stripClusterer.setMaxClusterSize(clusterMaxSize);
        stripClusterer.setCentralStripAveragingThreshold(clusterCentralStripAveragingThreshold);

        // Set the cluster errors.
        stripClusterer.SetOneClusterErr(oneClusterErr);
        stripClusterer.SetTwoClusterErr(twoClusterErr);
        stripClusterer.SetThreeClusterErr(threeClusterErr);
        stripClusterer.SetFourClusterErr(fourClusterErr);
        stripClusterer.SetFiveClusterErr(fiveClusterErr);

        // Set the readout to process.
        readouts.add(readoutCollectionName);

        // Set the detector to process.
        processPaths.add(subdetectorName);
    }

    /**
     * This is executed before detectorChanged and initialization of digitization objects is done
     * here.
     */
    @Override
    public void startOfData() {

        // At start of job, setup digitization objects needed by this Driver.
        initialize();

        // If readouts not already set, set them up.
        if (!readouts.isEmpty()) {
            System.out.println("Adding SimTrackerHitIdentifierReadoutDriver with readouts: " + readouts);
            super.add(new SimTrackerHitReadoutDriver(readouts));
        }
        super.startOfData();
        readouts.clear(); // FIXME Is this needed?
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
            processModules.addAll(detectorElement.findDescendants(SiTrackerModule.class));
            // if (debug)
            // System.out.println("added " + processModules.size() + " modules");
        }
    }

    /**
     * Perform the digitization.
     */
    @Override
    public void process(EventHeader event) {
        // Call sub-Driver processing.
        super.process(event);

        // Make new lists for output.
        List<RawTrackerHit> rawHits = new ArrayList<RawTrackerHit>();
        List<SiTrackerHit> stripHits1D = new ArrayList<SiTrackerHit>();

        if (event.hasCollection(SimTrackerHit.class, this.readoutCollectionName)) {
            // Make raw hits.
            for (SiSensor sensor : processSensors) {
                rawHits.addAll(stripDigitizer.makeHits(sensor));
            }

            // Make strip hits.
            for (SiSensor sensor : processSensors) {
                stripHits1D.addAll(stripClusterer.makeHits(sensor));
            }

            // Debug prints.
            if (debug) {
                if (event.hasCollection(SimTrackerHit.class, this.readoutCollectionName)) {
                    List<SimTrackerHit> simHits = event.get(SimTrackerHit.class, this.readoutCollectionName);
                    System.out.println("SimTrackerHit collection " + this.readoutCollectionName + " has " + simHits.size() + " hits.");
                    System.out.println("RawTrackerHit collection " + this.rawTrackerHitOutputCollectionName + " has " + rawHits.size() + " hits.");
                    System.out.println("TrackerHit collection " + this.stripHitOutputCollectionName + " has " + stripHits1D.size() + " hits.");
                } else {
                    System.out.println("SimTrackerHit collection " + this.readoutCollectionName + " not found.");
                }
            }
        }

        // Put output hits into collection.
        int flag = LCIOUtil.bitSet(0, 31, true); // Turn on 64-bit cell ID.
        //System.out.println("TrackerDigiDriver putting collection " + this.rawTrackerHitOutputCollectionName + " with readoutName " + readoutCollectionName);
        event.put(this.rawTrackerHitOutputCollectionName, rawHits, RawTrackerHit.class, flag, readoutCollectionName);
        //System.out.println("TrackerDigiDriver putting collection " + this.stripHitOutputCollectionName + " with readoutName " + readoutCollectionName);
        event.put(this.stripHitOutputCollectionName, stripHits1D, SiTrackerHitStrip1D.class, 0, readoutCollectionName);
    }
}