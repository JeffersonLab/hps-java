package org.hps.recon.tracking.apv25;

//--- constants ---//
import static org.hps.conditions.deprecated.HPSSVTConstants.TOTAL_APV25_PER_HYBRID;

import org.hps.conditions.deprecated.HPSSVTCalibrationConstants;
import org.hps.conditions.deprecated.HPSSVTConstants;
//--- lcsim ---//
import org.lcsim.detector.tracker.silicon.SiSensor;

/**
 * 
 * @author Omar Moreno
 * @version $Id: SvtHalfModule.java,v 1.7 2013/04/25 22:11:14 meeg Exp $
 */
public class SvtHalfModule {

    private SiSensor sensor;
    private Apv25Full[] apv25 = new Apv25Full[5];
    
    public SvtHalfModule(SiSensor sensor){
        
        // Set the sensor associated with this half-module
        this.sensor = sensor;
        
        // Instantiate the APV25's
        for(int chip = 0; chip < TOTAL_APV25_PER_HYBRID; chip++){
            apv25[chip] = new Apv25Full();
            for(int channel = 0; channel < HPSSVTConstants.CHANNELS; channel++){
                int physicalChannel = 639 - (chip*128 + 127 - channel);
                
                // Mark all bad channels which were found during QA
                if(HPSSVTCalibrationConstants.isBadChannel(sensor, physicalChannel)){
                    apv25[chip].getChannel(channel).markAsBadChannel();
                }

                // Set the shaping time
                double tp = HPSSVTCalibrationConstants.getTShaping(sensor, physicalChannel);
                apv25[chip].getChannel(channel).setShapingTime(tp);
            }
        }
    }
    
    public SiSensor getSensor(){
        return sensor;
    }
    
    public Apv25Full getAPV25(int physicalChannel){
        return apv25[this.getAPV25Number(physicalChannel)];
    }
    
    public int getAPV25Number(int physicalChannel){
    	return (int) ((TOTAL_APV25_PER_HYBRID - 1) - Math.floor(physicalChannel/HPSSVTConstants.CHANNELS));
    }
    
    public Apv25Full[] getAllApv25s(){
        return apv25;
    }
}
