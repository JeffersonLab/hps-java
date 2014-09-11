package org.hps.conditions.svt;

import java.util.Collection;
import java.util.List;

import org.hps.conditions.svt.SvtChannel.SvtChannelCollection;
import org.hps.conditions.svt.SvtDaqMapping.SvtDaqMappingCollection;
import org.hps.conditions.svt.SvtT0Shift.SvtT0ShiftCollection;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.geometry.Detector;
import org.hps.util.Pair;

/**
 * This class puts {@link SvtConditions} data onto <code>HpsSiSensor</code> objects.
 *
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public final class SvtDetectorSetup {

    /**
     * Load conditions data onto a detector object. This method is analogous to
     * {@link org.lcsim.hps.recon.tracking.SvtUtils#setup(Detector)}.
     * @param detector The detector object.
     * @param conditions The conditions object.
     */
    // FIXME: Change this to use a Subdetector instead of the Detector.
    public void load(Detector detector, SvtConditions conditions) {

        // Find sensor objects.
        List<HpsSiSensor> sensors = detector.getDetectorElement().findDescendants(HpsSiSensor.class);
        SvtChannelCollection channelMap = conditions.getChannelMap();
        SvtDaqMappingCollection daqMap = conditions.getDaqMap();
        SvtT0ShiftCollection t0Shifts = conditions.getT0Shifts();

        // Loop over sensors.
        for (HpsSiSensor sensor : sensors) {

            // Reset possible existing conditions data on sensor.
            sensor.reset();

            // Get the layer number.  The layer number will range from 1-12;
            int layerNumber = sensor.getLayerNumber();
            
            // Get the module ID number.  The sensors in the first three layers
            // of the SVT are assigned a module ID = 0 if they are in the top 
            // volume and 1 if they are on the bottom.  For layers 4-6, the 
            // assigned module ID is 0 and 2 for top and 1 and 3 for bottom
            // depending on whether the sensor is on the hole or slot side of
            // the half-module.
            int moduleNumber = sensor.getModuleNumber();

            // Get DAQ pair (FEB ID, FEB Hybrid ID) corresponding to this sensor
            Pair<Integer, Integer> daqPair = null;
            String SvtHalf = SvtDaqMappingCollection.TOP_HALF;
            if (sensor.isBottomLayer()) {
                SvtHalf = SvtDaqMappingCollection.BOTTOM_HALF;
            }
            daqPair = daqMap.getDaqPair(SvtHalf, layerNumber, moduleNumber);
            if (daqPair == null) {
                throw new RuntimeException("Failed to find DAQ pair for sensor: " + sensor.getName());
            }

            // Set the FEB ID of the sensor
            sensor.setFebID(daqPair.getFirstElement());
           
            // Set the FEB Hybrid ID of the sensor
            sensor.setFebHybridID(daqPair.getSecondElement());

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

                /*
                sensor.setGain(channelNumber, constants.getGain().getGain());
                sensor.setTimeOffset(channelNumber, constants.getGain().getOffset());
                sensor.setNoise(channelNumber, constants.getCalibration().getNoise());
                sensor.setPedestal(channelNumber, constants.getCalibration().getPedestal());
                sensor.setPulseParameters(channelNumber, constants.getPulseParameters().toArray());
                */
            }

            // Set the time shift for the sensor.
            //SvtTimeShift sensorTimeShift = timeShifts.find(daqPair).get(0);
            //sensor.setTimeShift(sensorTimeShift.getTimeShift());
        }
    }
}
