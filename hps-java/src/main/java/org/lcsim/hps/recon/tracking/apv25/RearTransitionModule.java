package org.lcsim.hps.recon.tracking.apv25;

//--- java ---//
import java.util.ArrayList;
import java.util.List;

//--- org.lcsim ---//
import org.lcsim.event.EventHeader;
import org.lcsim.hps.recon.tracking.HPSSVTCalibrationConstants;
import org.lcsim.hps.recon.tracking.HPSSVTConstants;
import org.lcsim.util.Driver;

//--- hps-java ---//
import org.lcsim.hps.util.RandomGaussian;

//--- Constants ---//
import static org.lcsim.hps.recon.tracking.HPSSVTConstants.TOTAL_STRIPS_PER_SENSOR;

/**
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id: RearTransitionModule.java,v 1.2 2013/04/25 22:11:14 meeg Exp $
 */
public class RearTransitionModule extends Driver {

    String apv25AnalogDataCollectionName = "APV25AnalogData";
    String apv25DigitalDataCollectionName = "AVP25DigitalData";

    double adcVHighRef = 1000;   // mVolts
    double adcVLowRef = -1000;   // mVolts
    int    adcResolution = 14;   // bits
    double adcVoltageResolution = 1;  // mV
    int quantizationLevels = 256; 

    double resistorValue = 100;  // Ohms
    double inputStageGain = 1.5;
    
    boolean noiseless = false;

    /**
     * Default Ctor
     */
    public RearTransitionModule(){

        // Find the number of quantization levels
        int quantizationLevels = (int) Math.pow(2, adcResolution);

        // Find the ADC voltage resolution
        adcVoltageResolution = (adcVHighRef - adcVLowRef)/quantizationLevels; // mV
    }    

    /**
     * 
     */
    public void setResolution(int bits){
        adcResolution = bits;

        // Find the number of quantization levels
        quantizationLevels = (int) Math.pow(2, adcResolution);


        // Find the ADC voltage resolution
        adcVoltageResolution = (adcVHighRef - adcVLowRef)/quantizationLevels; // mV
    }

    /**
     * 
     */
    public void setADCSpan(double adcVHighRef, double adcVLowRef){
        this.adcVHighRef = adcVHighRef;
        this.adcVLowRef = adcVLowRef;

        // Find the ADC voltage resolution
        adcVoltageResolution = (adcVHighRef - adcVLowRef)/quantizationLevels; // mV
    }
    
    /**
     * Turn readout noise on/off
     */
    public void setNoiseless(boolean noiseless){
        this.noiseless = noiseless;
    }

    /**
     * 
     */
    @Override
        protected void process(EventHeader event){
            super.process(event);

            // If the event does not contain any analog data that needs to be digitized, skip the event
            if(!event.hasCollection(Apv25AnalogData.class, apv25AnalogDataCollectionName)) return;

            // Get the analog data from the event
            List<Apv25AnalogData> analogData = event.get(Apv25AnalogData.class, apv25AnalogDataCollectionName);

            // Create a list hold the digital data
            List<Apv25DigitalData> digitalData = new ArrayList<Apv25DigitalData>();

            // Amplify the analog data
            for(Apv25AnalogData analogDatum : analogData){

                // Make a hard copy of the APV25 analog output to avoid modification of the original
                double[] apv25Output = new double[140];
                System.arraycopy(analogDatum.getApv25AnalogOutput(), 0, apv25Output, 0, apv25Output.length);        

                for(int index = 0; index < apv25Output.length; index++){

                    // For now, don't digitize the header
                    if(index < 12) continue;

                    // Get the physical channel 
                    int physicalChannel = TOTAL_STRIPS_PER_SENSOR 
                        - (analogDatum.getApvNumber()*HPSSVTConstants.CHANNELS + (HPSSVTConstants.CHANNELS - 1) - (index - 12));

                    apv25Output[index] += 4; // mA
                    apv25Output[index] *= (HPSSVTConstants.MIP/HPSSVTConstants.MULTIPLEXER_GAIN);

                    // Digitize the signal 
                    apv25Output[index] *= HPSSVTCalibrationConstants.getGain(analogDatum.getSensor(), physicalChannel);

                    // Add pedestal and noise
                    double pedestal = HPSSVTCalibrationConstants.getPedestal(analogDatum.getSensor(), physicalChannel);
                    double noise = HPSSVTCalibrationConstants.getNoise(analogDatum.getSensor(), physicalChannel);
                    if(!noiseless)
                        apv25Output[index] += RandomGaussian.getGaussian(pedestal, noise);            
                    else
                        apv25Output[index] += pedestal;
                }

                // Add the digital data to the list
                digitalData.add(new Apv25DigitalData(analogDatum.getSensor(), analogDatum.getApvNumber(), apv25Output));
            }

            event.put(apv25DigitalDataCollectionName, digitalData, Apv25DigitalData.class, 0);
        }
}
