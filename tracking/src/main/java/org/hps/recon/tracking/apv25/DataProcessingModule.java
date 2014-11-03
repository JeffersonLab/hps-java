package org.hps.recon.tracking.apv25;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//===> import org.hps.conditions.deprecated.SvtUtils;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
/**
 * 
 * @author Omar Moreno
 */
// TODO: Sandbox this class
public abstract class DataProcessingModule extends Driver {

    Map<SiSensor, SvtDataBlocks> sensorToDataBlocks = new HashMap<SiSensor, SvtDataBlocks>();
	
	// Collection Names
    String apv25DigitalDataCollectionName = "AVP25DigitalData";
    String rawTrackerHitsCollectionName = "SVTRawTrackerHits";
    
    int nSamples = 0;  // Number of samples which have been processed
    int totalSamples = 6; // Number of samples which are read out
    
    /**
     * Set the number of shaper signal samples to be readout
     */
    public void setNumberOfSamplesToReadOut(int totalSamples){
    	this.totalSamples = totalSamples;
    }
    
    protected abstract List<RawTrackerHit> findRawHits();
	

    /*public void detectorChanged(Detector detector){
        for(SiSensor sensor : SvtUtils.getInstance().getSensors()){
            sensorToDataBlocks.put(sensor, new SvtDataBlocks());
        }
    }*/
    
	@Override
	public void process(EventHeader event){
		
		// If the event does not contain any data to process, skip it
		if(!event.hasCollection(Apv25DigitalData.class, apv25DigitalDataCollectionName)) return;
		
		// Get the digital data from the event
		List<Apv25DigitalData> digitalData = event.get(Apv25DigitalData.class, apv25DigitalDataCollectionName);
		
		// Block the data together
		for(Apv25DigitalData digitalDatum : digitalData){
			SiSensor sensor = digitalDatum.getSensor();
            int apvN = digitalDatum.getApv();

            double[] apv25DigitalOutput = new double[128];
            System.arraycopy(digitalDatum.getSamples(), 0, apv25DigitalOutput, 0, apv25DigitalOutput.length);

            for(int channel = 0; channel < apv25DigitalOutput.length; channel++){

                // Calculate the physical number
                int physicalChannel = 639 - (apvN*128 + 127 - channel);

                sensorToDataBlocks.get(sensor).addSample(physicalChannel, nSamples, (short) apv25DigitalOutput[channel]);
            }
		}
		nSamples++;
		
		// If the expected number of samples has been collected, process the data
        if(nSamples == totalSamples){

            // Add RawTrackerHits to the event
            event.put(rawTrackerHitsCollectionName, this.findRawHits(), RawTrackerHit.class, 0);
            nSamples = 0;
        }
	}
	
	protected class SvtDataBlocks {
		
		Map<Integer /* sample number */, short[]> channelToSamples = new HashMap<Integer, short[]>();
		
		/**
		 * 
		 */
		public SvtDataBlocks(){
		}
		
		public void addSample(Integer physicalChannel, int sampleN, short value){
            if(!channelToSamples.containsKey(physicalChannel)) channelToSamples.put(physicalChannel, new short[6]);
            channelToSamples.get(physicalChannel)[sampleN] = value;
		}
		
	       /**
         * 
         */
        public short[] getSamples(int physicalChannel){
            return channelToSamples.get(physicalChannel);
        }
        
        /**
         * 
         */
        public String printSamples(int physicalChannel){
        	String sampleString = "[ ";
        	short[] samples = this.getSamples(physicalChannel);
        	for(int index = 0; index < samples.length -1; index++){
        		sampleString +=  samples[index] + ", ";
        	}
        	sampleString += samples[samples.length - 1] + "]";
			return sampleString;
        }
	}
	
}
