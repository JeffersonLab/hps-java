package org.hps.recon.ecal;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

public class EcalGainDriver extends Driver{
    /**
     * ecalCollectionName "type" (must match detector-data) 
     */
    private final String ecalReadoutName = "EcalHits";

    private String inputHitsCollectionName = "EcalUncalHits";
    
    private String outputHitsCollectionName = "EcalCalHits";
    
    public void process(EventHeader event) {
        
        List<CalorimeterHit> hits = event.get(CalorimeterHit.class, inputHitsCollectionName);
        
        List<CalorimeterHit> newHits = new ArrayList<CalorimeterHit>();

        for (CalorimeterHit hit : hits) {
            double time = hit.getTime();
            double adcSum = hit.getRawEnergy(); //the "raw energy" is actually the adc sum.  
            long cellID = hit.getCellID();
            
            double energy =converter.adcToEnergy(adcSum, cellID);
           
            newHits.add(CalorimeterHitUtilities.create(energy, time, hit.getCellID()));
        }

        event.put(this.outputHitsCollectionName, newHits, CalorimeterHit.class, event.getMetaData(hits).getFlags(), ecalReadoutName);
        event.getMetaData(newHits).setTransient(true);
    }
    

    private EcalGain converter = new EcalGain();

    
    /**
     * Sets whether the driver should use the DAQ configuration from EvIO file for its parameters. If activated, the
     * converter will obtain gains, thresholds, pedestals, the window size, and the pulse integration window from the
     * EvIO file. This will replace and overwrite any manually defined settings.<br/>
     * <br/>
     * Note that if this setting is active, the driver will not output any data until a DAQ configuration has been read
     * from the data stream.
     * 
     * @param state - <code>true</code> indicates that the configuration should be read from the DAQ data in an EvIO
     *            file. Setting this to <code>false</code> will cause the driver to use its regular manually-defined
     *            settings and pull gains and pedestals from the conditions database.
     */
    public void setUseDAQConfig(boolean state) {
       // useDAQConfig = state;
        converter.setUseDAQConfig(state);
    }
    
    /**
     * Set to <code>true</code> to use the "2014" gain formula:<br/>
     * 
     * <pre>
     * channelGain * adcSum * gainFactor * readoutPeriod
     * </pre>
     * <p>
     * Set to <code>false</code> to use the gain formula for the Test Run:
     * 
     * <pre>
     * gain * adcSum * ECalUtils.MeV
     * </pre>
     * 
     * @param use2014Gain True to use 2014 gain formulation.
     */
    public void setUse2014Gain(boolean use2014Gain) {
        converter.setUse2014Gain(use2014Gain);
    }
    
    /**
     * Set a constant gain factor in the converter for all channels.
     * 
     * @param gain The constant gain value.
     */
    public void setGain(double gain) {
        converter.setGain(gain);
    }
    
    @Override
    public void detectorChanged(Detector detector) {

        // set the detector for the converter
        // FIXME: This method doesn't even need the detector object and does not use it.
        converter.setDetector(detector);

        // ECAL combined conditions object.
       // ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();
    }
    

    /**
     * Set the input {@link org.lcsim.event.CalorimeterHit} collection name,
     * 
     * @param ecalCollectionName The <code>CalorimeterHit</code> collection name.
     */
    public void setInputHitsCollectionName(String inputHitsCollectionName) {
        this.inputHitsCollectionName = inputHitsCollectionName;
    }
    /**
     * Set the output {@link org.lcsim.event.CalorimeterHit} collection name,
     * 
     * @param ecalCollectionName The <code>CalorimeterHit</code> collection name.
     */
    public void setOutputHitsCollectionName(String name){
        this.outputHitsCollectionName = name;
    }

}
