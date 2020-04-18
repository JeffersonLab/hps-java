package org.hps.recon.tracking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.lcsim.detector.DetectorElementStore;

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimTrackerHit;
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
    private String readoutCollectionName = "TrackerHits";
    // Subdetector name.
    private String subdetectorName = "Tracker";
    // Name of FittedTrackerHit output collection.
    private String fittedTrackerHitCollectionName = "SVTFittedRawTrackerHits";

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
    // weight the hits in a cluster by charge? (if not, all hits have equal weight)
    private boolean useWeights = true;
    // Various data lists required by digitization.
    private List<String> processPaths = new ArrayList<String>();
    private List<IDetectorElement> processDEs = new ArrayList<IDetectorElement>();
    private Set<SiSensor> processSensors = new HashSet<SiSensor>();
    // Digi class objects.
    // private SiDigitizer stripDigitizer;
    // private HPSFittedRawTrackerHitMaker hitMaker;
    private StripMaker stripClusterer;
    // private DumbShaperFit shaperFit;
    int[] counts = new int[14];

    // setup sensors so we can re-run from already processed LCIO files
    private boolean _setupSensors = false;

    /**
     * Sets up information associated with sensors that is lost when the event
     * is persisted to an LCIO file.
     *
     * @param b should be false when running the reconstruction from raw data.
     */
    public void setSetupSensors(boolean b) {
        _setupSensors = b;
    }

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

    public void setUseWeights(boolean useWeights) {
        this.useWeights = useWeights;
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
            processSensors.addAll(detectorElement.findDescendants(SiSensor.class)); // if (debug)
        }        // System.out.println("added " + processSensors.size() + " sensors");

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

        //cng
        if (_setupSensors) {
            setupSensors(event);
        }
        //cng
        // Make new lists for output.    
        List<SiTrackerHit> stripHits1D = new ArrayList<SiTrackerHit>();

        // Make strip hits.
        for (SiSensor sensor : processSensors) {
            stripHits1D.addAll(stripClusterer.makeHits(sensor));
        }

        // Debug prints.
        if (debug) {
            if (event.hasCollection(SimTrackerHit.class, this.readoutCollectionName)) {
                System.out.println("SimTrackerHit collection " + this.readoutCollectionName
                        + " has " + event.get(SimTrackerHit.class, this.readoutCollectionName).size() + " hits.");
            }
            if (event.hasCollection(FittedRawTrackerHit.class, fittedTrackerHitCollectionName)) {
                System.out.println("FittedRawTrackerHit collection "
                        + this.fittedTrackerHitCollectionName + " has " + event.get(LCRelation.class, fittedTrackerHitCollectionName).size() + " hits.");
            }
            System.out.println("TrackerHit collection " + this.stripHitOutputCollectionName + " has " + stripHits1D.size() + " hits.");
        }

        // Put output hits into collection.
        int flag = LCIOUtil.bitSet(0, 31, true); // Turn on 64-bit cell ID.        
        event.put(this.stripHitOutputCollectionName, stripHits1D, SiTrackerHitStrip1D.class, 0, toString());
        if (debug) {
            System.out.println("[ DataTrackerHitDriver ] - " + this.stripHitOutputCollectionName + " has " + stripHits1D.size() + " hits.");
        }
        for (SiTrackerHit stripHit : stripHits1D) {
            counts[((SiTrackerHitStrip1D) stripHit).getRawHits().get(0).getLayerNumber() - 1]++;
        }

    }

    @Override
    public void endOfData() {
        if (debug) {
            for (int layer = 0; layer < 14; layer++) {
                System.out.format("layer %d, count %d\n", layer, counts[layer]);
            }
        }
    }

    /**
     * Add missing information which is lost when writing events to disk and
     * reading back from an LCIO file.
     *
     * @param event The full EventHeader with persisted information
     */
    private void setupSensors(EventHeader event) {
        List<RawTrackerHit> rawTrackerHits = event.get(RawTrackerHit.class, "SVTRawTrackerHits");
        // Get the list of relations to fitted hits from the event
        List<LCRelation> fittedTrackerHits = event.get(LCRelation.class, "SVTFittedRawTrackerHits");
        // Map the fitted hits to their corresponding raw hits
        // Note that each RawTrackerHit may map to multiple FittedHits
        Map<RawTrackerHit, List<LCRelation>> fittedRawTrackerHitMap = new HashMap<RawTrackerHit, List<LCRelation>>();
        for (LCRelation fittedHit : fittedTrackerHits) {
            RawTrackerHit rth = FittedRawTrackerHit.getRawTrackerHit(fittedHit);
            if (fittedRawTrackerHitMap.containsKey(rth)) {
                fittedRawTrackerHitMap.get(rth).add(fittedHit);
            } else {
                fittedRawTrackerHitMap.put(rth, new ArrayList<LCRelation>());
                fittedRawTrackerHitMap.get(rth).add(fittedHit);
            }
        }
        EventHeader.LCMetaData meta = event.getMetaData(rawTrackerHits);
        // Get the ID dictionary and field information.
        IIdentifierDictionary dict = meta.getIDDecoder().getSubdetector().getDetectorElement().getIdentifierHelper().getIdentifierDictionary();
        int fieldIdx = dict.getFieldIndex("side");
        int sideIdx = dict.getFieldIndex("strip");
        for (RawTrackerHit hit : rawTrackerHits) {
            // The "side" and "strip" fields needs to be stripped from the ID for sensor lookup.
            IExpandedIdentifier expId = dict.unpack(hit.getIdentifier());
            expId.setValue(fieldIdx, 0);
            expId.setValue(sideIdx, 0);
            IIdentifier strippedId = dict.pack(expId);
            // Find the sensor DetectorElement.
            List<IDetectorElement> des = DetectorElementStore.getInstance().find(strippedId);
            if (des == null || des.size() == 0) {
                throw new RuntimeException("Failed to find any DetectorElements with stripped ID <0x" + Long.toHexString(strippedId.getValue()) + ">.");
            } else if (des.size() == 1) {
                hit.setDetectorElement((SiSensor) des.get(0));
            } else {
                // Use first sensor found, which should work unless there are sensors with duplicate IDs.
                for (IDetectorElement de : des) {
                    if (de instanceof SiSensor) {
                        hit.setDetectorElement((SiSensor) de);
                        break;
                    }
                }
            }
            // No sensor was found.
            if (hit.getDetectorElement() == null) {
                throw new RuntimeException("No sensor was found for hit with stripped ID <0x" + Long.toHexString(strippedId.getValue()) + ">.");
            }

            // it appears we need to add a FittedRawTrackerHit to the hit sensor readout...
            // see line 233 of RawTrackerHitFitterDriver
            //hit.getDetectorElement().getReadout().addHit(hth);
            // get the Relations for this RawTrackerHit
            List<LCRelation> fthList = fittedRawTrackerHitMap.get(hit);
            for (LCRelation fth : fthList) {
                RawTrackerHit rthDummy = FittedRawTrackerHit.getRawTrackerHit(fth);
                ShapeFitParameters shape = shapeFitParameter((GenericObject) fth.getTo());
                FittedRawTrackerHit frth = new FittedRawTrackerHit(rthDummy, shape);
                hit.getDetectorElement().getReadout().addHit(frth);
            }
        } //end of loop over RawTrackerHits
    }

    /**
     * Create a ShapeFitParameters object from its stored GenericObject
     *
     * @param o A Generic Object read back from an LCIO file
     * @return The ShapeFitParameters object corresponding to the GenericObject
     */
    private ShapeFitParameters shapeFitParameter(GenericObject o) {
        ShapeFitParameters sfp = new ShapeFitParameters();
        sfp.setT0(o.getDoubleVal(0));
        sfp.setT0Err(o.getDoubleVal(1));
        sfp.setAmp(o.getDoubleVal(2));
        sfp.setAmpErr(o.getDoubleVal(3));
        sfp.setChiProb(o.getDoubleVal(4));
        return sfp;
    }

}
