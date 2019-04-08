package org.hps.readout.svt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.readout.SimTrackerHitReadoutDriver;
import org.hps.readout.util.collection.LCIOCollection;
import org.hps.readout.util.collection.LCIOCollectionFactory;
import org.hps.readout.util.collection.TriggeredLCIOData;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.HpsTestRunSiSensor;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.Subdetector;

/**
 * Class <code>SVTTrackerHitReadoutDriver</code> is an extension of
 * {@link org.hps.readout.SimTrackerHitReadoutDriver
 * SimTrackerHitReadoutDriver} that includes additional functionality
 * necessary for SVT data.
 * 
 * @author Kyle McCarty
 * @see org.hps.readout.SimTrackerHitReadoutDriver
 */
public class SVTTrackerHitReadoutDriver extends SimTrackerHitReadoutDriver {
    private Subdetector detector = null;
    private LCIOCollection<FpgaData> fpgaDataParams = null;
    
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
    
    @Override
    protected Collection<TriggeredLCIOData<?>> getOnTriggerData(double triggerTime) {
        // The base class already provides outputs the truth hits
        // associated with the output hits. The SVT also requires
        // that FPGA data be written to the output file. Get the
        // truth data from the superclass, so that it may be merged
        // with the FPGA data.
        Collection<TriggeredLCIOData<?>> superData = super.getOnTriggerData(triggerTime);
        
        // Get the FPGA data.
        List<FpgaData> fpgaData = new ArrayList<FpgaData>(makeFPGAData(detector).values());
        
        // Create the FPGA data collection.
        TriggeredLCIOData<FpgaData> fpgaCollection = new TriggeredLCIOData<FpgaData>(fpgaDataParams);
        fpgaCollection.getData().addAll(fpgaData);
        
        // Add the FPGA data to the output data from the superclass.
        List<TriggeredLCIOData<?>> collectionsList = new ArrayList<TriggeredLCIOData<?>>((superData == null ? 0 : superData.size()) + 1);
        collectionsList.add(fpgaCollection);
        
        // Return the collections list result.
        return collectionsList;
    }
    
    // TODO: SVT group should describe what this does.
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
}