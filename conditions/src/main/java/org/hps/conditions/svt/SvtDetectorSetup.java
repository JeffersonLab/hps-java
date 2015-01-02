package org.hps.conditions.svt;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtChannel.SvtChannelCollection;
import org.hps.conditions.svt.SvtDaqMapping.SvtDaqMappingCollection;
import org.hps.conditions.svt.SvtT0Shift.SvtT0ShiftCollection;
import org.hps.conditions.svt.TestRunSvtChannel.TestRunSvtChannelCollection;
import org.hps.conditions.svt.TestRunSvtDaqMapping.TestRunSvtDaqMappingCollection;
import org.hps.conditions.svt.TestRunSvtT0Shift.TestRunSvtT0ShiftCollection;
import org.hps.util.Pair;
import org.lcsim.conditions.ConditionsEvent;
import org.lcsim.conditions.ConditionsListener;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.HpsTestRunSiSensor;
import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.util.log.LogUtil;


/**
 * This class puts {@link SvtConditions} data onto <code>HpsSiSensor</code>
 * objects.
 *
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public final class SvtDetectorSetup implements ConditionsListener {

    private static Logger logger = LogUtil.create(SvtDetectorSetup.class); 
    
    private String svtName = "Tracker";
    private boolean enabled = true;
    
    public SvtDetectorSetup(String svtName) {
        this.svtName = svtName;
    }
    
    public void setSvtName(String svtName) {
        this.svtName = svtName;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public void setLogLevel(Level level) {
        logger.setLevel(level);
        logger.getHandlers()[0].setLevel(level);
    }
    
    @Override
    public void conditionsChanged(ConditionsEvent event) {
        if (enabled) {
            DatabaseConditionsManager manager = (DatabaseConditionsManager) event.getConditionsManager();
            Subdetector subdetector = manager.getDetectorObject().getSubdetector(svtName);
            if (manager.isTestRun()) {
                TestRunSvtConditions svtConditions = manager.getCachedConditions(TestRunSvtConditions.class, "test_run_svt_conditions").getCachedData();
                loadTestRun(subdetector, svtConditions);
            } else {
                SvtConditions svtConditions = manager.getCachedConditions(SvtConditions.class, "svt_conditions").getCachedData();
                loadDefault(subdetector, svtConditions);
            }
        } else {
            logger.config("disabled");
        }
    }
    
    /**
     * Load conditions data onto a detector object.
     * 
     * @param The detector object.
     * @param conditions The conditions object.
     */
    void loadDefault(Subdetector subdetector, SvtConditions conditions) {

        logger.info("loading SVT conditions onto subdetector " + subdetector.getName());
        
        // Find sensor objects.
        List<HpsSiSensor> sensors = subdetector.getDetectorElement().findDescendants(HpsSiSensor.class);
        logger.info("setting up " + sensors.size() + " SVT sensors");
        SvtChannelCollection channelMap = conditions.getChannelMap();
        logger.info("channel map has " + conditions.getChannelMap().size() + " entries");
        SvtDaqMappingCollection daqMap = conditions.getDaqMap();
        SvtT0ShiftCollection t0Shifts = conditions.getT0Shifts();

        // Loop over sensors.
        for (HpsSiSensor sensor : sensors) {

            // Reset possible existing conditions data on sensor.
            sensor.reset();

            // Get DAQ pair (FEB ID, FEB Hybrid ID) corresponding to this sensor
            Pair<Integer, Integer> daqPair = daqMap.getDaqPair(sensor);
            if (daqPair == null) {
                throw new RuntimeException("Failed to find DAQ pair for sensor: " + sensor.getName());
            }

            // Set the FEB ID of the sensor
            sensor.setFebID(daqPair.getFirstElement());

            // Set the FEB Hybrid ID of the sensor
            sensor.setFebHybridID(daqPair.getSecondElement());

            // Set the orientation of the sensor
            String orientation = daqMap.getOrientation(daqPair);
            if (orientation != null && orientation.contentEquals(SvtDaqMappingCollection.AXIAL)) {
                sensor.setAxial(true);
            } else if (orientation != null && orientation.contains(SvtDaqMappingCollection.STEREO)) {
                sensor.setStereo(true);
            }

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

                // Check if the channel was flagged as bad
                if (constants.isBadChannel()) {
                    sensor.setBadChannel(channelNumber);
                }

                // Set the pedestal and noise of each of the samples for the
                // channel
                double[] pedestal = new double[6];
                double[] noise = new double[6];
                for (int sampleN = 0; sampleN < HpsSiSensor.NUMBER_OF_SAMPLES; sampleN++) {
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
            SvtT0Shift sensorT0Shift = t0Shifts.getT0Shift(daqPair);
            sensor.setT0Shift(sensorT0Shift.getT0Shift());
        }
    }
    
    void loadTestRun(Subdetector subdetector, TestRunSvtConditions conditions) {

        logger.info("loading Test Run SVT conditions onto subdetector " + subdetector.getName());                       
        
        // Find sensor objects.
        List<HpsSiSensor> sensors = subdetector.getDetectorElement().findDescendants(HpsSiSensor.class);
        logger.info("setting up " + sensors.size() + " SVT sensors");
        TestRunSvtChannelCollection channelMap = conditions.getChannelMap();
        logger.info("channel map has " + channelMap.size() + " entries");
        TestRunSvtDaqMappingCollection daqMap = conditions.getDaqMap();
        TestRunSvtT0ShiftCollection t0Shifts = conditions.getT0Shifts();

        // Loop over sensors.
        for (HpsSiSensor sensor : sensors) {

            // Reset possible existing conditions data on sensor.
            sensor.reset();

            // Get DAQ pair (FPGA ID, Hybrid ID) corresponding to this sensor
            Pair<Integer, Integer> daqPair = daqMap.getDaqPair(sensor);
            if (daqPair == null) {
                throw new RuntimeException("Failed to find DAQ pair for sensor: " + sensor.getName());
            }

            // Set the FPGA ID of the sensor
            ((HpsTestRunSiSensor) sensor).setFpgaID(daqPair.getFirstElement());

            // Set the hybrid ID of the sensor
            ((HpsTestRunSiSensor) sensor).setHybridID(daqPair.getSecondElement());

            // Set the orientation of the sensor
            String orientation = daqMap.getOrientation(daqPair);
            if (orientation != null && orientation.contentEquals(TestRunSvtDaqMappingCollection.AXIAL)) {
                sensor.setAxial(true);
            } else if (orientation != null && orientation.contains(TestRunSvtDaqMappingCollection.STEREO)) {
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
                for (int sampleN = 0; sampleN < HpsTestRunSiSensor.NUMBER_OF_SAMPLES; sampleN++) {
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
            TestRunSvtT0Shift sensorT0Shift = t0Shifts.getT0Shift(daqPair);
            sensor.setT0Shift(sensorT0Shift.getT0Shift());
        }
    }
}
