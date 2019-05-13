package org.hps.test.it;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hps.analysis.hodoscope.HodoscopePlotsDriver;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.detector.hodoscope.HodoscopeDetectorElement;
import org.hps.job.DatabaseConditionsManagerSetup;
import org.hps.job.JobManager;
import org.hps.test.util.TestOutputFile;
import org.hps.util.FilterMCBunches;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.Subdetector;
import org.lcsim.job.AidaSaveDriver;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

import hep.aida.ICloud1D;
import hep.aida.ICloud2D;
import hep.physics.vec.Hep3Vector;
import junit.framework.TestCase;

/**
 * Integration test for running filtering and readout on simulated Hodoscope data
 * and creating plots from the final readout data.
 * 
 * @author Jeremy McCormick
 */
public class HodoscopeReadoutTest extends TestCase {
    
    /** Name of remote test file with SLIC events */
    static final String TEST_FILE_URL = "http://www.lcsim.org/test/hps-java/slicHodoTestEvents.slcio";

    /** Name of test detector, which does not include SVT */
    private String DETECTOR_NAME = "HPS-HodoscopeTest-v1";
    
    /** Run number for conditions system initialization*/
    private Integer RUN_NUMBER = 1000000;
    
    /** Event spacing to use for filtering */
    private Integer EVENT_SPACING = 300;
        
    /** Set to true to run the filtering for spacing the events. */
    private static final boolean RUN_FILTERING = true;
    
    /** Set to true to run the readout simulation. */
    private static final boolean RUN_READOUT = true;
    
    /** Set to true to write ROOT output. */
    private static final boolean WRITE_ROOT = false;
    
    /** Set to true to include Driver which writes basic plots. */
    private static final boolean INCLUDE_BASIC_PLOTS = true;
    
    /** Set to true to include Driver which writes detailed plots from analysis module. */
    private static final boolean INCLUDE_DETAILED_PLOTS = true; 
    
    public static final AIDA aida = AIDA.defaultInstance();
   
    public void testHodoscopeReadout() throws IOException, ConditionsNotFoundException {

        // Get remote test file
        FileCache cache = new FileCache();
        File testFile = cache.getCachedFile(new URL(TEST_FILE_URL));

        // Event spacing
        File filteredFile = new TestOutputFile(HodoscopeReadoutTest.class, "slicHodoTestEvents_filt.slcio");
        if (RUN_FILTERING) {            
            String[] args = {testFile.getPath(), filteredFile.getPath(), "-e", EVENT_SPACING.toString()};
            FilterMCBunches.main(args);
        }
        
        // Setup conditions
        DatabaseConditionsManagerSetup cond = new DatabaseConditionsManagerSetup();
        cond.setDetectorName(DETECTOR_NAME);
        cond.setRun(RUN_NUMBER);
        cond.setFreeze(true);
        
        File outputFile = new TestOutputFile(HodoscopeReadoutTest.class, "HodoscopeReadoutTest");

        // Run readout
        if (RUN_READOUT) {
            JobManager mgr = new JobManager();
            mgr.setConditionsSetup(cond);
            mgr.addVariableDefinition("outputFile", outputFile.getPath());
            mgr.addInputFile(filteredFile);
            mgr.setup("/org/hps/steering/readout/SimpleHodoscopeTriggerNoTracker.lcsim");
            mgr.run();
        }

        // Run analysis drivers
        LCSimLoop loop = new LCSimLoop();
        DatabaseConditionsManager condMgr = DatabaseConditionsManager.getInstance();
        condMgr.setDetector(DETECTOR_NAME, RUN_NUMBER);
        condMgr.freeze();
        AidaSaveDriver aida = new AidaSaveDriver();
        aida.setOutputFileName(outputFile.getPath() + ".aida");
        if (INCLUDE_BASIC_PLOTS) {
            loop.add(new HodoAnalDriver());
        }
        if (INCLUDE_DETAILED_PLOTS) {
            loop.add(new HodoscopePlotsDriver());
        }
        loop.add(aida);
        if (WRITE_ROOT) {
            AidaSaveDriver root = new AidaSaveDriver();
            root.setOutputFileName(outputFile.getPath() + ".root");
            loop.add(root);
        }
        loop.setLCIORecordSource(new File(outputFile.getPath() + ".slcio"));
        loop.loop(-1);
    }
   
    /*
    Output collections:
    
    Output 7 objects of type CalorimeterHit to collection "EcalClustersGTPSimHits".
    Output 6 objects of type LCRelation to collection "HodoscopeTruthRelations".
    Output 8 objects of type RawTrackerHit to collection "EcalReadoutHits".
    Output 3 objects of type SimTrackerHit to collection "HodoscopeHits".
    Output 8 objects of type LCRelation to collection "EcalTruthRelations".
    Output 45 objects of type MCParticle to collection "MCParticle".
    Output 4 objects of type RawTrackerHit to collection "HodoscopeReadoutHits".
    Output 2 objects of type Cluster to collection "EcalClustersGTP".
    Output 14 objects of type SimCalorimeterHit to collection "EcalHits".
    Output 1 objects of type GenericObject to collection "TriggerBank".
    Output 14 objects of type SimTrackerHit to collection "TrackerHitsECal".
    Output 4 objects of type ReadoutTimestamp to collection "ReadoutTimestamps".
    */
    
    /*
    From Kyle:
    truth hit energies and geometric distributions
    digitized hit distributions and ADC count, 
    converted hit energies and positions
    */
    
    class HodoAnalDriver extends Driver {
        
        HodoAnalDriver() {
            add(new HodoHitsDriver());
            add(new EcalClustersGTPDriver());
            add(new EcalHitsDriver());
            add(new HodoscopeReadoutHitsDriver());
            add(new TrackerHitsECalDriver());
            add(new ReadoutTimestampsDriver());
            add(new MCParticleDriver());
        }
                        
        public void startOfData() {
            aida.tree().mkdir("/Collections");
            for (Driver driver : this.drivers()) {
                HodoDriver<?> hodoDriver = (HodoDriver<?>) driver;
                hodoDriver.mkdir();
            }
        }
        
        public void process(EventHeader event) {
            for (Driver driver : this.drivers()) {
                HodoDriver<?> hodoDriver = (HodoDriver<?>) driver;
                hodoDriver.cd();
                hodoDriver.loadColl(event);
                hodoDriver.process(event);
            }
        }        
    }
    
    abstract class HodoDriver<T> extends Driver {
        
        private String collName = null;
        private String dirName = null;
        private Class<T> klass = null;
        
        protected Subdetector subdet;
        protected HodoscopeDetectorElement hodo;
        protected IIdentifierHelper helper;
        protected volatile List<T> coll; 
        
        public HodoDriver(Class<T> klass, String collName) {
            this.collName = collName;
            this.dirName = "/Collections/" + this.collName;
            this.klass = klass;
        }
        
        public void mkdir() {
            aida.tree().mkdir(this.dirName);
        }
        
        public void cd() {
            aida.tree().cd(this.dirName);
        }               
        
        public List<T> getColl(EventHeader event) {
            return event.get(klass, collName);
        }
        
        public void loadColl(EventHeader event) {
            this.coll = getColl(event);
        }
        
        public void detectorChanged(Detector det) {
            subdet = det.getSubdetector("Hodoscope");
            hodo = (HodoscopeDetectorElement) subdet.getDetectorElement();
            helper = hodo.getIdentifierHelper();
        }
        
        public ICloud1D cloud1D(String name) {
            return aida.cloud1D(name);
        }
        
        public ICloud2D cloud2D(String name) {
            return aida.cloud2D(name);
        }
        
        abstract public void process(EventHeader event);
    }
    
    class HodoHitsDriver extends HodoDriver<SimTrackerHit> {
        
        public HodoHitsDriver() {
            super(SimTrackerHit.class, "HodoscopeHits");
        }
                        
        public void process(EventHeader event) {            
            cloud1D("Hit Count").fill(coll.size());
            for (SimTrackerHit hit : coll) {
                cloud1D("Hit Energy [MeV]").fill(hit.getdEdx() * 1000);
                cloud1D("Position X [mm]").fill(hit.getPosition()[0]);
                cloud1D("Position Y [mm]").fill(hit.getPosition()[1]);
                cloud1D("Position Z [mm]").fill(hit.getPosition()[2]);
                cloud1D("Time [ns]").fill(hit.getTime());
                cloud2D("Position XY [mm]").fill(hit.getPosition()[0], hit.getPosition()[1]);
                int layer = helper.getValue(hit.getIdentifier(), "layer");
                cloud1D("Layer").fill(layer);
            }
        }
    }
    
    class EcalClustersGTPDriver extends HodoDriver<Cluster> {
        public EcalClustersGTPDriver() {
            super(Cluster.class, "EcalClustersGTP");
        }
        
        public void process(EventHeader event) {
            cloud1D("Cluster Count").fill(coll.size());
            for (Cluster clus : coll) {
                cloud1D("Energy [GeV]").fill(clus.getEnergy());
                cloud1D("Position X [mm]").fill(clus.getPosition()[0]);
                cloud1D("Position Y [mm]").fill(clus.getPosition()[1]);
                cloud1D("Position Z [mm]").fill(clus.getPosition()[2]);
                cloud2D("Position XY [mm]").fill(clus.getPosition()[0], clus.getPosition()[1]);
                cloud1D("Cluster Size").fill(clus.getSize());
            }
        }
    }
    
    class EcalHitsDriver extends HodoDriver<CalorimeterHit> {
        public EcalHitsDriver() {
            super(CalorimeterHit.class, "EcalHits");
        }
        
        public void process(EventHeader event) {
            cloud1D("Hit Count").fill(coll.size());
            for (CalorimeterHit hit : coll) {
                cloud1D("Energy [MeV]").fill(hit.getCorrectedEnergy() * 1000);
                cloud1D("Position X [mm]").fill(hit.getPosition()[0]);
                cloud1D("Position Y [mm]").fill(hit.getPosition()[1]);
                cloud1D("Position Z [mm]").fill(hit.getPosition()[2]);
                cloud1D("Time [ns]").fill(hit.getTime());
            }            
        }
    }
    
    class HodoscopeReadoutHitsDriver extends HodoDriver<RawTrackerHit> {
                
        public HodoscopeReadoutHitsDriver() {
            super(RawTrackerHit.class, "HodoscopeReadoutHits");
        }
        
        public void process(EventHeader event) {
            cloud1D("Raw Hit Count").fill(coll.size());
            for (RawTrackerHit hit : coll) {
                Set<Integer> uniqADC = new HashSet<Integer>();
                for (Short adc : hit.getADCValues()) {
                    uniqADC.add(adc.intValue());                    
                    cloud1D("ADC Value").fill(adc.intValue());
                }
                for (Integer adc : uniqADC) {
                    cloud1D("Unique ADC Values by Event").fill(adc);
                }
            }
        }
    }
    
    class EcalReadoutHitsDriver extends HodoDriver<RawTrackerHit> {
        
        public EcalReadoutHitsDriver() {
            super(RawTrackerHit.class, "EcalReadoutHits");
        }
        
        public void process(EventHeader event) {
            cloud1D("Raw Hit Count").fill(coll.size());
            for (RawTrackerHit hit : coll) {
                Set<Integer> uniqADC = new HashSet<Integer>();
                for (Short adc : hit.getADCValues()) {
                    uniqADC.add(adc.intValue());                    
                    cloud1D("ADC Value").fill(adc.intValue());
                }
                for (Integer adc : uniqADC) {
                    cloud1D("Unique ADC Values by Event").fill(adc);
                }
            }
        }
    }
    
    class TrackerHitsECalDriver extends HodoDriver<SimTrackerHit> {
        public TrackerHitsECalDriver() {
            super(SimTrackerHit.class, "TrackerHitsECal");
        }
        
        public void process(EventHeader event) {
            cloud1D("Hit Count").fill(coll.size());
            for (SimTrackerHit hit : coll) {
                cloud1D("Position X [mm]").fill(hit.getPosition()[0]);
                cloud1D("Position Y [mm]").fill(hit.getPosition()[1]);
                //cloud1D("Position Z [mm]").fill(hit.getPosition()[2]);
                cloud2D("Position XY [mm]").fill(hit.getPosition()[0], hit.getPosition()[1]);
            }
        }
    }    
    
    class ReadoutTimestampsDriver extends HodoDriver<GenericObject> {
        
        private double maxTimestamp = -1;
        private double minTimestamp = 999999999;
        
        public ReadoutTimestampsDriver() {
            super(GenericObject.class, "ReadoutTimestamps");
        }
        
        public void process(EventHeader event) {
            cloud1D("Timestamp Count").fill(coll.size());
            for (GenericObject obj : coll) {                
                double timestamp = obj.getDoubleVal(0);
                cloud1D("Timestamp").fill(timestamp);
                if (timestamp > maxTimestamp) {
                    maxTimestamp = timestamp;
                } else if (timestamp < minTimestamp) {
                    minTimestamp = timestamp;
                }
            }
        }
        
        public void endOfData() {
            System.out.println("maxTimestamp = " + maxTimestamp);
            System.out.println("minTimestamp = " + minTimestamp);
        }
    }
    
    class MCParticleDriver extends HodoDriver<MCParticle> {
        
        public MCParticleDriver() {
            super(MCParticle.class, "MCParticle");
        }
        
        public void process(EventHeader event) {
            cloud1D("Particle Count").fill(coll.size());
            for (MCParticle particle : coll) {
                int pdgid = particle.getPDGID();
                double e = particle.getEnergy();
                if (particle.getGeneratorStatus() == MCParticle.FINAL_STATE) {
                    if (pdgid == 11) {
                        cloud1D("FS Electron Energy").fill(e);
                    } else if (pdgid == 22) {
                        cloud1D("FS Gamma Energy").fill(e);
                    }
                }
                Hep3Vector endp = particle.getEndPoint();
                cloud1D("Endpoint X").fill(endp.x());
                cloud1D("Endpoint Y").fill(endp.y());
                cloud1D("Endpoint Z").fill(endp.z());
                cloud2D("Endpoint XY").fill(endp.x(), endp.y());
                cloud2D("Endpoint ZY").fill(endp.z(), endp.y());
            }
        }
    }
}
