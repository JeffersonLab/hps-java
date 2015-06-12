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
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 * @author <a href="mailto:omoreno1@ucsc.edu">Omar Moreno</a>
 */
public final class SvtDetectorSetup implements ConditionsListener {

    /**
     * The number of noise samples.
     */
    private static final int NOISE_COUNT = 6;

    /**
     * The number of pedestals.
     */
    private static final int PEDESTAL_COUNT = 6;

    /**
     * Initialize logger.
     */
    private static Logger logger = LogUtil.create(SvtDetectorSetup.class);

    /**
     * The name of the SVT subdetector in the detector model.
     */
    private String svtName = "Tracker";

    /**
     * Flag to enable/disable this class from within conditions manager.
     */
    private boolean enabled = true;

    /**
     * Constructor that takes name of SVT.
     *
     * @param svtName the name of the SVT subdetector
     */
    public SvtDetectorSetup(final String svtName) {
        this.svtName = svtName;
    }

    /**
     * Set the name of the SVT in the detector model.
     *
     * @param svtName the name of the SVt in the detector model.
     */
    public void setSvtName(final String svtName) {
        this.svtName = svtName;
    }

    /**
     * Set whether this class is enabled to be activated on conditions changes.
     *
     * @param enabled <code>true</code> to enable
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Set the log level.
     *
     * @param level the log level
     */
    public void setLogLevel(final Level level) {
        logger.setLevel(level);
        logger.getHandlers()[0].setLevel(level);
    }

    /**
     * Hook that activates this class when conditions change (new detector or
     * run number).
     *
     * @param event the conditions event
     */
    @Override
    public void conditionsChanged(final ConditionsEvent event) {
        if (enabled) {
            final DatabaseConditionsManager manager = (DatabaseConditionsManager) event.getConditionsManager();
            final Subdetector subdetector = manager.getDetectorObject().getSubdetector(svtName);
            if (subdetector != null) {
                if (manager.isTestRun()) {
                    final TestRunSvtConditions svtConditions = manager.getCachedConditions(TestRunSvtConditions.class,
                            "test_run_svt_conditions").getCachedData();
                    loadTestRun(subdetector, svtConditions);
                } else {
                    final SvtConditions svtConditions = manager.getCachedConditions(SvtConditions.class,
                            "svt_conditions").getCachedData();
                    loadDefault(subdetector, svtConditions);
                }
            } else {
                logger.warning("no SVT detector was found so SvtDetectorSetup was NOT activated");
                enabled = false;
            }
        } else {
            logger.config("disabled");
        }
    }

    /**
     * Load conditions data onto a detector object.
     *
     * @param subdetector the SVT subdetector object
     * @param conditions the conditions object
     */
    void loadDefault(final Subdetector subdetector, final SvtConditions conditions) {

        logger.info("loading SVT conditions onto subdetector " + subdetector.getName());

        // Find sensor objects.
        final List<HpsSiSensor> sensors = subdetector.getDetectorElement().findDescendants(HpsSiSensor.class);
        logger.info("setting up " + sensors.size() + " SVT sensors");
        final SvtChannelCollection channelMap = conditions.getChannelMap();
        logger.info("channel map has " + conditions.getChannelMap().size() + " entries");
        final SvtDaqMappingCollection daqMap = conditions.getDaqMap();
        final SvtT0ShiftCollection t0Shifts = conditions.getT0Shifts();

        // Loop over sensors.
        for (HpsSiSensor sensor : sensors) {

            // Reset possible existing conditions data on sensor.
            sensor.reset();

            // Get DAQ pair (FEB ID, FEB Hybrid ID) corresponding to this sensor
            final Pair<Integer, Integer> daqPair = daqMap.getDaqPair(sensor);
            if (daqPair == null) {
                throw new RuntimeException("Failed to find DAQ pair for sensor: " + sensor.getName());
            }

            // Set the FEB ID of the sensor
            sensor.setFebID(daqPair.getFirstElement());

            // Set the FEB Hybrid ID of the sensor
            sensor.setFebHybridID(daqPair.getSecondElement());

            // Set the orientation of the sensor
            final String orientation = daqMap.getOrientation(daqPair);
            if (orientation != null && orientation.contentEquals(SvtDaqMapping.AXIAL)) {
                sensor.setAxial(true);
            } else if (orientation != null && orientation.contains(SvtDaqMapping.STEREO)) {
                sensor.setStereo(true);
            }

            // Find all the channels for this sensor.
            final Collection<SvtChannel> channels = channelMap.find(daqPair);

            // Loop over the channels of the sensor.
            for (SvtChannel channel : channels) {

                // Get conditions data for this channel.
                final ChannelConstants constants = conditions.getChannelConstants(channel);
                final int channelNumber = channel.getChannel();

                //
                // Set conditions data for this channel on the sensor object:
                //
                // Check if the channel was flagged as bad
                if (constants.isBadChannel()) {
                    sensor.setBadChannel(channelNumber);
                }

                // Set the pedestal and noise of each of the samples for the
                // channel
                final double[] pedestal = new double[PEDESTAL_COUNT];
                final double[] noise = new double[NOISE_COUNT];
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
            final SvtT0Shift sensorT0Shift = t0Shifts.getT0Shift(daqPair);
            if (sensorT0Shift == null) {
                throw new RuntimeException("Failed to find T0 shift for sensor: " + sensor.getName() + ", FEB hybrid ID " + daqPair.getFirstElement() + ", FEB ID " + daqPair.getSecondElement());
            }
            sensor.setT0Shift(sensorT0Shift.getT0Shift());
        }
    }

    /**
     * Load conditions from Test Run detector.
     *
     * @param subdetector the SVT subdetector object
     * @param conditions the Test Run conditions
     */
    void loadTestRun(final Subdetector subdetector, final TestRunSvtConditions conditions) {

        logger.info("loading Test Run SVT conditions onto subdetector " + subdetector.getName());

        // Find sensor objects.
        final List<HpsSiSensor> sensors = subdetector.getDetectorElement().findDescendants(HpsSiSensor.class);
        logger.info("setting up " + sensors.size() + " SVT sensors");
        final TestRunSvtChannelCollection channelMap = conditions.getChannelMap();
        logger.info("channel map has " + channelMap.size() + " entries");
        final TestRunSvtDaqMappingCollection daqMap = conditions.getDaqMap();
        final TestRunSvtT0ShiftCollection t0Shifts = conditions.getT0Shifts();

        // Loop over sensors.
        for (HpsSiSensor sensor : sensors) {

            // Reset possible existing conditions data on sensor.
            sensor.reset();

            // Get DAQ pair (FPGA ID, Hybrid ID) corresponding to this sensor
            final Pair<Integer, Integer> daqPair = daqMap.getDaqPair(sensor);
            if (daqPair == null) {
                throw new RuntimeException("Failed to find DAQ pair for sensor: " + sensor.getName());
            }

            // Set the FPGA ID of the sensor
            ((HpsTestRunSiSensor) sensor).setFpgaID(daqPair.getFirstElement());

            // Set the hybrid ID of the sensor
            ((HpsTestRunSiSensor) sensor).setHybridID(daqPair.getSecondElement());

            // Set the orientation of the sensor
            final String orientation = daqMap.getOrientation(daqPair);
            if (orientation != null && orientation.contentEquals(TestRunSvtDaqMapping.AXIAL)) {
                sensor.setAxial(true);
            } else if (orientation != null && orientation.contains(TestRunSvtDaqMapping.STEREO)) {
                sensor.setStereo(true);
            }

            // Find all the channels for this sensor.
            final Collection<TestRunSvtChannel> channels = channelMap.find(daqPair);

            // Loop over the channels of the sensor.
            for (TestRunSvtChannel channel : channels) {

                // Get conditions data for this channel.
                final ChannelConstants constants = conditions.getChannelConstants(channel);
                final int channelNumber = channel.getChannel();

                //
                // Set conditions data for this channel on the sensor object:
                //
                // Check if the channel was flagged as bad
                if (constants.isBadChannel()) {
                    sensor.setBadChannel(channelNumber);
                }

                // Set the pedestal and noise of each of the samples for the
                // channel
                final double[] pedestal = new double[6];
                final double[] noise = new double[6];
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
            final TestRunSvtT0Shift sensorT0Shift = t0Shifts.getT0Shift(daqPair);
            sensor.setT0Shift(sensorT0Shift.getT0Shift());
        }
    }
}
