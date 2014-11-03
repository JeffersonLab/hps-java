package org.hps.recon.tracking.apv25;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//===> import org.hps.conditions.deprecated.HPSSVTCalibrationConstants;
import org.lcsim.detector.IReadout;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.base.BaseRawTrackerHit;


/**
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
// TODO: Sandbox this class. 
public class TestRunDataProcessingModule extends DataProcessingModule {

    int nSamplesAboveThreshold = 1;    // Number of samples above noise threshold 
    int noiseThreshold = 3;            // Units of RMS noise

    boolean enablePileUpCut = true;
    boolean enableThresholdCut = true;

    /**
     * Default Ctor
     */
    public TestRunDataProcessingModule(){
    };

    /**
     * 
     */
    public void setNumberOfSamplesAboveThreshold(int nSamplesAboveThreshold){
        this.nSamplesAboveThreshold = nSamplesAboveThreshold;
    }

    /**
     * 
     */
    public void setNoiseThreshold(int noiseThreshold /* Noise RMS */){
        this.noiseThreshold = noiseThreshold;
    }

    /**
     *
     */
    public void setEnablePileUpCut(boolean enablePileUpCut){
        this.enablePileUpCut = enablePileUpCut;
    }

    /**
     *
     */
    public void setEnableThresholdCut(boolean enableThresholdCut){
        this.enableThresholdCut = enableThresholdCut;
    }

    protected List<RawTrackerHit> findRawHits(){

        List<RawTrackerHit> rawHits = new ArrayList<RawTrackerHit>();

        // Loop through all blocked data
        for(Map.Entry<SiSensor, SvtDataBlocks> sensor : sensorToDataBlocks.entrySet()){

            SvtDataBlocks blocks = sensor.getValue();

            for(int channel = 0; channel < 639; channel++){
            	
                // FIXME: Update to use the new conditions system at some point. 
            	//===> if(HPSSVTCalibrationConstants.isBadChannel(sensor.getKey(), channel)) continue;
            	
                short[] samples = blocks.getSamples(channel);  

                if(enableThresholdCut && !this.samplesAboveThreshold(sensor.getKey(), channel, samples)) continue;

                if(enablePileUpCut && !this.pileUpCut(samples)) continue;

                // Create a RawTrackerHit
                int sideNumber;
                int time = 0;
                if(sensor.getKey().hasElectrodesOnSide(ChargeCarrier.HOLE)){
                    sideNumber = ChargeCarrier.HOLE.charge();
                } else {
                    sideNumber = ChargeCarrier.ELECTRON.charge();
                }
                long cellID = sensor.getKey().makeStripId(channel, sideNumber).getValue();
                RawTrackerHit rawHit = new BaseRawTrackerHit(time, cellID, samples, new ArrayList<SimTrackerHit>(), sensor.getKey());
                rawHits.add(rawHit);
                
                // Add the raw hit to the sensor readout
        		IReadout readOut = sensor.getKey().getReadout();
        		readOut.addHit(rawHit);
            }
        }

        System.out.println(this.getClass().getSimpleName() + ": Number of RawTrackerHits created: " + rawHits.size());
        return rawHits;
    }

    /**
     * 
     */
    // FIXME: Update to use the new conditions system at some point. 
    private boolean samplesAboveThreshold(SiSensor sensor, int channel, short[] samples){
        
    	/* ===> 
    	// Number of samples above threshold
        int nSamplesAboveThreshold = 0;

        // Get the pedestal and noise for this channel
        double pedestal = HPSSVTCalibrationConstants.getPedestal(sensor, channel);
        double noise = HPSSVTCalibrationConstants.getNoise(sensor, channel);

        // Calculate the threshold
        int threshold = (int) (pedestal + noise*this.noiseThreshold);

        for(int index = 0; index < 6; index++){
            if(samples[index] >= threshold) nSamplesAboveThreshold++;
        }

        // If the prerequisite number of samples are above threshold return true
        if(nSamplesAboveThreshold >= this.nSamplesAboveThreshold ) return true;
        ===> */
        return false;
    }

    /**
     *
     */
    private boolean pileUpCut(short[] sample){
        if(sample[2] > sample[1] || sample[3] > sample[2]) return true;
        return false; 
    }
}
