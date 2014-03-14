package org.hps.conditions.svt;

import java.util.Collection;
import java.util.List;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.geometry.Detector;
import org.lcsim.hps.util.Pair;

/**
 * This class loads {@link SvtConditions} data onto <code>HpsSiSensor</code> objects.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtConditionsLoader {
    
    /**
     * Load conditions data onto a detector object.
     * This method is analogous to {@link org.lcsim.hps.recon.tracking.SvtUtils#setup(Detector)}.
     * @param detector The detector object.
     * @param conditions The conditions object.
     */
    public void load(Detector detector, SvtConditions conditions) {
        
        // Find sensor objects.        
        List<HpsSiSensor> sensors = detector.getDetectorElement().findDescendants(HpsSiSensor.class);
        SvtChannelCollection channelMap = conditions.getChannelMap();
        SvtDaqMap daqMap = conditions.getDaqMap();
        SvtTimeShiftCollection timeShifts = conditions.getTimeShifts();
        
        // Loop over sensors.
        for (HpsSiSensor sensor : sensors) {
            
            // Reset possible existing conditions data on sensor.
            sensor.reset();
            
            // Get the layer number.
            int layerNumber = sensor.getLayerNumber();
            
            // Get info from the DAQ map about this sensor. 
            Pair<Integer, Integer> daqPair = null;            
            int half = SvtDaqMap.TOP;
            if (sensor.isBottomLayer()) {
                half = SvtDaqMap.BOTTOM;
            }             
            daqPair = daqMap.get(half, layerNumber);
            if (daqPair == null) {
                throw new RuntimeException("Failed to find DAQ pair for sensor: " + sensor.getName());
            }
            
            // Set FPGA value from DAQ map.
            sensor.setFpgaNumber(daqPair.getFirstElement());
            
            // Set hybrid value from DAQ map.
            sensor.setHybridNumber(daqPair.getSecondElement());
            
            // Find all the channels for this sensor.
            Collection<SvtChannel> channels = channelMap.find(daqPair);
                        
            // Loop over the channels of the sensor.
            for (SvtChannel channel : channels) {
                // Get conditions data for this channel.
                ChannelConstants constants = conditions.getChannelConstants(channel);
                int channelNumber = channel.getChannel();
                
                //
                // Set conditions data for this channel on the sensor object:
                //
                if (constants.isBadChannel()) {
                    sensor.setBadChannel(channelNumber);
                }
                sensor.setGain(channelNumber, constants.getGain().getGain());
                sensor.setTimeOffset(channelNumber, constants.getGain().getOffset());
                sensor.setNoise(channelNumber, constants.getCalibration().getNoise());
                sensor.setPedestal(channelNumber, constants.getCalibration().getPedestal());
                sensor.setPulseParameters(channelNumber, constants.getPulseParameters().toArray());
            }
            
            // Set the time shift for the sensor.
            SvtTimeShift sensorTimeShift = timeShifts.find(daqPair).get(0);
            sensor.setTimeShift(sensorTimeShift.getTimeShift());
        }
    }
}
