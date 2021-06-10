package org.hps.recon.tracking;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
//import org.lcsim.lcio.LCIOUtil;
import org.lcsim.recon.tracking.digitization.sisim.CDFSiSensorSim;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHit;
import org.lcsim.detector.tracker.silicon.SiStriplets;
//import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.tracker.silicon.SiTrackerIdentifierHelper;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;

import org.hps.readout.svt.HPSSVTConstants;

public class RawHitChecker extends Driver {
    
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

    //If flag is false do not save the StripCluster Collection if bigger than threshold (to remove monster events)
    private boolean saveMonsterEvents = true;
    
    //Threshold to decide if this is a monster event

    private int thresholdMonsterEvents = 50;
    
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

    public void setSaveMonsterEvents(boolean saveMonsterEvents) {
        this.saveMonsterEvents = saveMonsterEvents;
    }

    public void setThresholdMonsterEvents(int thresholdMonsterEvents) {
        this.thresholdMonsterEvents = thresholdMonsterEvents;
    }

    /**
     * Creates a new instance of TrackerHitDriver.
     */
    //public DataTrackerHitDriver() {
    //}

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
        for (SiSensor sensor : sensors)  {
            
            // Identifier helper (reset once per sensor)
            SiTrackerIdentifierHelper _sid_helper = (SiTrackerIdentifierHelper) sensor.getIdentifierHelper();
            HpsSiSensor hpsSensor = (HpsSiSensor) sensor;
            if (hpsSensor.getMillepedeId() > 4)
                continue;
            
            
            //stripHits1D.addAll(stripClusterer.makeHits(sensor));
            List<SiTrackerHit> hits = new ArrayList<SiTrackerHit>();
            //_sid_helper = (SiTrackerIdentifierHelper) sensor.getIdentifierHelper();
            
            List< LCRelation > fittedHits = sensor.getReadout().getHits(LCRelation.class);
            
            for (LCRelation fittedHit : fittedHits) {
                
                RawTrackerHit hit = FittedRawTrackerHit.getRawTrackerHit(fittedHit);
                IIdentifier id = hit.getIdentifier();
                int strip = _sid_helper.getElectrodeValue(id);
                
                //System.out.println("[ RawHitChecker ] - hit_strip=" +strip);
                //System.out.println("[ RawHitChecker ] - (u,v)=");

                // Get electrodes and check that they are striplets
                ChargeCarrier carrier = ChargeCarrier.getCarrier(_sid_helper.getSideValue(id));
                SiSensorElectrodes electrodes = ((SiSensor) hit.getDetectorElement()).getReadoutElectrodes(carrier);

                
                //Assume that is instance of striplets here.

                SiStriplets striplets = null;
                if (electrodes instanceof SiStriplets)
                    striplets = (SiStriplets) electrodes;
                
                if ( (strip == (HPSSVTConstants.TOTAL_STRIPS_PER_THIN_SENSOR - 1)) || strip == 0) continue;

                if (strip == 1 || strip == 2 || strip == 510  || strip == 509) {
                    System.out.println("PF::Debug::Sensor Name=" + sensor.getName());
                    System.out.println("[ RawHitChecker ] - hit_strip=" + strip);
                    System.out.println("[ RawHitChecker ] - hit cellID=" + hit.getCellID());
                    //Check a bit the electrode of this hit:
                    
                    System.out.println("[ RawHitChecker ] - electrode nAxes:" +electrodes.getNAxes());
                    System.out.println("[ RawHitChecker ] - electrode getNCells:" +electrodes.getNCells());
                    System.out.println("[ RawHitChecker ] - electrode getNRows:" +electrodes.getNCells(0));
                    System.out.println("[ RawHitChecker ] - electrode getNColumns:" +electrodes.getNCells(1));
                    System.out.println("[ RawHitChecker ] - electrode getPosition:" +electrodes.getCellPosition(strip).toString());
                    System.out.println("[ RawHitChecker ] - striplet  getRowNumber:" +striplets.getRowNumber(strip));
                    System.out.println("[ RawHitChecker ] - striplet  getColumnNumber:" +striplets.getColumnNumber(strip));
                    System.out.println("[ RawHitChecker ] - striplet  getStripCenter:" +striplets.getStripCenter(strip));
                    System.out.println("[ RawHitChecker ] - striplet  getStripLength:" +striplets.getStripLength(strip));
                    
                    
                }
                

                
            }
            
        }
        //System.out.println("[ DataTrackerHitDriver ] - " + this.stripHitOutputCollectionName + " has " + stripHits1D.size() + " hits.");
        
        
    }
}
