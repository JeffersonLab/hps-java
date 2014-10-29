package org.hps.conditions.svt;

import java.util.Collection;
import java.util.List;

import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.HpsTestRunSiSensor;

import org.hps.conditions.svt.SvtT0Shift.SvtT0ShiftCollection;
import org.hps.conditions.svt.TestRunSvtChannel.TestRunSvtChannelCollection;
import org.hps.conditions.svt.TestRunSvtDaqMapping.TestRunSvtDaqMappingCollection;
import org.hps.util.Pair;

/**
 * This class puts {@link TestRunSvtConditions} data onto 
 * <code>HpsTestRunSiSensor</code> objects.
 *
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public class TestRunSvtDetectorSetup {

    /**
     * Load conditions data onto a detector object.
     * 
     * @param  The detector object.
     * @param conditions The conditions object.
     */
	public void load(Subdetector subdetector, TestRunSvtConditions conditions){
	
        // Find sensor objects.
    	List<HpsSiSensor> sensors = subdetector.getDetectorElement().findDescendants(HpsSiSensor.class);
        System.out.println("Total sensors found: " + sensors.size());
    	
    	TestRunSvtChannelCollection channelMap = conditions.getChannelMap();
        TestRunSvtDaqMappingCollection daqMap = conditions.getDaqMap();
        SvtT0ShiftCollection t0Shifts = conditions.getT0Shifts();
		
        // Loop over sensors.
        for(HpsSiSensor sensor : sensors){
        	
        	HpsTestRunSiSensor testRunSensor = (HpsTestRunSiSensor) sensor; 
        	
            // Reset possible existing conditions data on sensor.
            sensor.reset();
        
            // Get DAQ pair (FPGA ID, Hybrid ID) corresponding to this sensor
            Pair<Integer, Integer> daqPair = daqMap.getDaqPair(sensor);
            if (daqPair == null) {
                throw new RuntimeException("Failed to find DAQ pair for sensor: " + sensor.getName());
            }
           
            // Set the FPGA ID of the sensor
            testRunSensor.setFpgaID(daqPair.getFirstElement());
        
            // Set the hybrid ID of the sensor
            testRunSensor.setHybridID(daqPair.getSecondElement());
            
            // Set the orientation of the sensor
            String orientation = daqMap.getOrientation(daqPair);
            if(orientation != null && orientation.contentEquals(TestRunSvtDaqMappingCollection.AXIAL)){
            	sensor.setAxial(true);
            } else if(orientation != null && orientation.contains(TestRunSvtDaqMappingCollection.STEREO)){
            	sensor.setStereo(true);
            }
        
            // Find all the channels for this sensor.
            Collection<TestRunSvtChannel> channels = channelMap.find(daqPair);
       
            
            // Loop over the channels of the sensor.
            for (TestRunSvtChannel channel : channels) {
            
            	// Get conditions data for this channel.
                ChannelConstants constants = conditions.getChannelConstants(channel);
                int channelNumber = channel.getChannel();
            
            
                //
                // Set conditions data for this channel on the sensor object:
                //
                
                // Check if the channel was flagged as bad
                if (constants.isBadChannel()) {
                    sensor.setBadChannel(channelNumber);
                }
                
                // Set the pedestal and noise of each of the samples for the 
                // channel
                double[] pedestal = new double[6];
                double[] noise = new double[6];
                for(int sampleN = 0; sampleN < HpsTestRunSiSensor.NUMBER_OF_SAMPLES; sampleN++){
                	pedestal[sampleN] = constants.getCalibration().getPedestal(sampleN);
                	noise[sampleN] = constants.getCalibration().getNoise(sampleN);
                }
                sensor.setPedestal(channelNumber, pedestal);
                sensor.setNoise(channelNumber, noise);
               
                // Set the gain and offset for the channel
                sensor.setGain(channelNumber, constants.getGain().getGain());
                sensor.setOffset(channelNumber, constants.getGain().getOffset());
               
                // Set the shape fit parameters
                sensor.setShapeFitParameters(channelNumber, constants.getShapeFitParameters().toArray());
            }
            
            // Set the t0 shift for the sensor.
            SvtT0Shift sensorT0Shift = t0Shifts.find(daqPair).get(0);
            sensor.setT0Shift(sensorT0Shift.getT0Shift());
        }
	}
}
