package org.hps.readout.svt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hps.readout.ReadoutDataManager;
import org.hps.readout.SLICDataReadoutDriver;
import org.hps.readout.util.collection.LCIOCollection;
import org.hps.readout.util.collection.LCIOCollectionFactory;
import org.hps.readout.util.collection.TriggeredLCIOData;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.HpsTestRunSiSensor;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.Subdetector;

public class SimTrackerHitReadoutDriver extends SLICDataReadoutDriver<SimTrackerHit> {
    private Subdetector detector = null;
    private LCIOCollection<FpgaData> fpgaDataParams = null;
    
    public SimTrackerHitReadoutDriver() {
        super(SimTrackerHit.class, 0xc0000000);
    }
    
    @Override
    public void detectorChanged(Detector detector) {
        this.detector = detector.getSubdetector("Tracker");
    }
    
    @Override
    public void startOfData() {
        // Run the superclass method.
        super.startOfData();
        
        // Create the LCSim collection parameters for the FPGA data.
        LCIOCollectionFactory.setCollectionName("FPGAData");
        LCIOCollectionFactory.setProductionDriver(this);
        fpgaDataParams = LCIOCollectionFactory.produceLCIOCollection(FpgaData.class);
    }
    
    protected Collection<TriggeredLCIOData<?>> getOnTriggerData(double triggerTime) {
        // Get the FPGA data.
        List<FpgaData> fpgaData = new ArrayList<FpgaData>(makeFPGAData(detector).values());
        
        // Create the FPGA data collection.
        TriggeredLCIOData<FpgaData> fpgaCollection = new TriggeredLCIOData<FpgaData>(fpgaDataParams);
        fpgaCollection.getData().addAll(fpgaData);
        
        // Get the truth hits in the indicated time range.
        Collection<SimTrackerHit> truthHits = ReadoutDataManager.getData(triggerTime - getReadoutWindowBefore(), triggerTime + getReadoutWindowAfter(), collectionName, SimTrackerHit.class);
        
        // MC particles need to be extracted from the truth hits
        // and included in the readout data to ensure that the
        // full truth chain is available.
        Set<MCParticle> truthParticles = new java.util.HashSet<MCParticle>();
        for(SimTrackerHit simHit : truthHits) {
            ReadoutDataManager.addParticleParents(simHit.getMCParticle(), truthParticles);
        }
        
        // Create the truth MC particle collection.
        LCIOCollectionFactory.setCollectionName("MCParticle");
        LCIOCollectionFactory.setProductionDriver(this);
        LCIOCollection<MCParticle> truthParticleCollection = LCIOCollectionFactory.produceLCIOCollection(MCParticle.class);
        TriggeredLCIOData<MCParticle> truthParticleData = new TriggeredLCIOData<MCParticle>(truthParticleCollection);
        truthParticleData.getData().addAll(truthParticles);
        
        // Create a general list for the collection.
        List<TriggeredLCIOData<?>> collectionsList = new ArrayList<TriggeredLCIOData<?>>(2);
        collectionsList.add(fpgaCollection);
        if(isPersistent()) { collectionsList.add(truthParticleData); }
        
        // Return the collections list result.
        return collectionsList;
    }
    
    private Map<Integer, FpgaData> makeFPGAData(Subdetector subdetector) {
        double[] temps = new double[HPSSVTConstants.TOTAL_HYBRIDS_PER_FPGA * HPSSVTConstants.TOTAL_TEMPS_PER_HYBRID];
        for(int i = 0; i < HPSSVTConstants.TOTAL_HYBRIDS_PER_FPGA * HPSSVTConstants.TOTAL_TEMPS_PER_HYBRID; i++) {
            temps[i] = 23.0;
        }
        
        Map<Integer, FpgaData> fpgaData = new HashMap<Integer, FpgaData>();
        List<HpsSiSensor> sensors = subdetector.getDetectorElement().findDescendants(HpsSiSensor.class);
        
        List<Integer> fpgaNumbers = new ArrayList<Integer>();
        for(HpsSiSensor sensor : sensors) {
            if(sensor instanceof HpsTestRunSiSensor && !fpgaNumbers.contains(((HpsTestRunSiSensor) sensor).getFpgaID())) {
                fpgaNumbers.add(((HpsTestRunSiSensor) sensor).getFpgaID());
            }
        }
        //===> for (Integer fpgaNumber : SvtUtils.getInstance().getFpgaNumbers()) {
        for(Integer fpgaNumber : fpgaNumbers) {
            fpgaData.put(fpgaNumber, new FpgaData(fpgaNumber, temps, 0));
        }
        
        return fpgaData;
    }
    
    @Override
    protected double getTimeNeededForLocalOutput() {
        return isPersistent() ? getReadoutWindowAfter() : 0;
    }
}