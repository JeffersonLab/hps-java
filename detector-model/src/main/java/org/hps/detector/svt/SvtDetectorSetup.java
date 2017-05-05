package org.hps.detector.svt;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.AbstractSvtDaqMapping;
import org.hps.conditions.svt.ChannelConstants;
import org.hps.conditions.svt.SvtChannel;
import org.hps.conditions.svt.SvtDaqMapping;
import org.hps.conditions.svt.TestRunSvtDaqMapping;
import org.hps.conditions.svt.SvtChannel.SvtChannelCollection;
import org.hps.conditions.svt.SvtConditions;
import org.hps.conditions.svt.SvtDaqMapping.SvtDaqMappingCollection;
import org.hps.conditions.svt.SvtT0Shift;
import org.hps.conditions.svt.SvtT0Shift.SvtT0ShiftCollection;
import org.hps.conditions.svt.TestRunSvtChannel;
import org.hps.conditions.svt.TestRunSvtChannel.TestRunSvtChannelCollection;
import org.hps.conditions.svt.TestRunSvtConditions;
import org.hps.conditions.svt.TestRunSvtDaqMapping.TestRunSvtDaqMappingCollection;
import org.hps.conditions.svt.TestRunSvtT0Shift;
import org.hps.conditions.svt.TestRunSvtT0Shift.TestRunSvtT0ShiftCollection;
import org.hps.util.Pair;
import org.lcsim.conditions.ConditionsEvent;
import org.lcsim.conditions.ConditionsListener;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.HpsTestRunSiSensor;
import org.lcsim.geometry.compact.Subdetector;

/**
 * This class puts {@link SvtConditions} data onto <code>HpsSiSensor</code> objects.
 */
public final class SvtDetectorSetup implements ConditionsListener {

    /**
     * Initialize logger.
     */
    private static Logger LOGGER = Logger.getLogger(SvtDetectorSetup.class.getPackage().getName());

    /**
     * The number of noise samples.
     */
    private static final int NOISE_COUNT = 6;

    /**
     * The number of pedestals.
     */
    private static final int PEDESTAL_COUNT = 6;

    /**
     * Flag to enable/disable this class from within conditions manager.
     */
    private boolean enabled = true;

    /**
     * The name of the SVT subdetector in the detector model.
     */
    private String svtName = "Tracker";

    /**
     * Constructor that uses the default detector name.
     */
    public SvtDetectorSetup() {
    }

    /**
     * Constructor that takes name of SVT.
     *
     * @param svtName the name of the SVT subdetector
     */
    public SvtDetectorSetup(final String svtName) {
        this.svtName = svtName;
    }

    /**
     * Hook that activates this class when conditions change (new detector or run number).
     *
     * @param event the conditions event
     */
    @Override
    public void conditionsChanged(final ConditionsEvent event) {
        if (this.enabled) {
            final DatabaseConditionsManager manager = (DatabaseConditionsManager) event.getConditionsManager();
            final Subdetector subdetector = manager.getDetectorObject().getSubdetector(this.svtName);
            if (subdetector != null) {
                if (manager.isTestRun()) {
                    LOGGER.info("activating Test Run setup");
                    final TestRunSvtConditions svtConditions = manager.getCachedConditions(TestRunSvtConditions.class,
                            "test_run_svt_conditions").getCachedData();
                    this.loadTestRun(subdetector, svtConditions);
                } else {
                    LOGGER.info("activating default setup");
                    final SvtConditions svtConditions = manager.getCachedConditions(SvtConditions.class,
                            "svt_conditions").getCachedData();
                    this.loadDefault(subdetector, svtConditions);
                }
            } else {
                LOGGER.warning("no SVT detector was found so setup was NOT activated");
                this.enabled = false;
            }
        } else {
            LOGGER.config("disabled");
        }
    }

    /**
     * Load conditions data onto a detector object.
     *
     * @param subdetector the SVT subdetector object
     * @param conditions the conditions object
     */
    void loadDefault(final Subdetector subdetector, final SvtConditions conditions) {

        LOGGER.info("loading default SVT conditions onto subdetector " + subdetector.getName());

        // Find sensor objects.
        final List<HpsSiSensor> sensors = subdetector.getDetectorElement().findDescendants(HpsSiSensor.class);
        LOGGER.info("setting up " + sensors.size() + " SVT sensors");
        final SvtChannelCollection channelMap = conditions.getChannelMap();
        LOGGER.info("channel map has " + conditions.getChannelMap().size() + " entries");
        final SvtDaqMappingCollection daqMap = conditions.getDaqMap();
        final SvtT0ShiftCollection t0Shifts = conditions.getT0Shifts();

        // Loop over sensors.
        for (final HpsSiSensor sensor : sensors) {

            // Reset possible existing conditions data on sensor.
            sensor.reset();

            // Get DAQ pair (FEB ID, FEB Hybrid ID) corresponding to this sensor
            final Pair<Integer, Integer> daqPair = getDaqPair(daqMap, sensor);
            if (daqPair == null) {
                throw new RuntimeException("Failed to find DAQ pair for sensor: " + sensor.getName());
            }

            // Set the FEB ID of the sensor
            sensor.setFebID(daqPair.getFirstElement());

            // Set the FEB Hybrid ID of the sensor
            sensor.setFebHybridID(daqPair.getSecondElement());

            // Set the orientation of the sensor
            final String orientation = daqMap.getOrientation(daqPair);
            if (orientation != null && orientation.contentEquals(AbstractSvtDaqMapping.AXIAL)) {
                sensor.setAxial(true);
            } else if (orientation != null && orientation.contains(AbstractSvtDaqMapping.STEREO)) {
                sensor.setStereo(true);
            }

            // Find all the channels for this sensor.
            final Collection<SvtChannel> channels = channelMap.find(daqPair);

            // Loop over the channels of the sensor.
            for (final SvtChannel channel : channels) {

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
                throw new RuntimeException("Failed to find T0 shift for sensor: " + sensor.getName()
                        + ", FEB hybrid ID " + daqPair.getFirstElement() + ", FEB ID " + daqPair.getSecondElement());
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

        LOGGER.info("loading Test Run SVT conditions onto subdetector " + subdetector.getName());

        // Find sensor objects.
        final List<HpsSiSensor> sensors = subdetector.getDetectorElement().findDescendants(HpsSiSensor.class);
        LOGGER.info("setting up " + sensors.size() + " SVT sensors");
        final TestRunSvtChannelCollection channelMap = conditions.getChannelMap();
        LOGGER.info("channel map has " + channelMap.size() + " entries");
        final TestRunSvtDaqMappingCollection daqMap = conditions.getDaqMap();
        final TestRunSvtT0ShiftCollection t0Shifts = conditions.getT0Shifts();

        // Loop over sensors.
        for (final HpsSiSensor sensor : sensors) {

            // Reset possible existing conditions data on sensor.
            sensor.reset();

            // Get DAQ pair (FPGA ID, Hybrid ID) corresponding to this sensor
            final Pair<Integer, Integer> daqPair = SvtDetectorSetup.getTestRunDaqPair(daqMap, sensor);
            if (daqPair == null) {
                throw new RuntimeException("Failed to find DAQ pair for sensor: " + sensor.getName());
            }

            // Set the FPGA ID of the sensor
            ((HpsTestRunSiSensor) sensor).setFpgaID(daqPair.getFirstElement());

            // Set the hybrid ID of the sensor
            ((HpsTestRunSiSensor) sensor).setHybridID(daqPair.getSecondElement());

            // Set the orientation of the sensor
            final String orientation = daqMap.getOrientation(daqPair);
            if (orientation != null && orientation.contentEquals(AbstractSvtDaqMapping.AXIAL)) {
                sensor.setAxial(true);
            } else if (orientation != null && orientation.contains(AbstractSvtDaqMapping.STEREO)) {
                sensor.setStereo(true);
            }

            // Find all the channels for this sensor.
            final Collection<TestRunSvtChannel> channels = channelMap.find(daqPair);

            // Loop over the channels of the sensor.
            for (final TestRunSvtChannel channel : channels) {

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
            final TestRunSvtT0Shift sensorT0Shift = t0Shifts.getT0Shift(daqPair);
            sensor.setT0Shift(sensorT0Shift.getT0Shift());
        }
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
        LOGGER.setLevel(level);
        LOGGER.getHandlers()[0].setLevel(level);
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
     * Get a DAQ pair (FEB ID, FEB Hybrid ID) for the given {@link HpsSiSensor}.
     *
     * @param sensor a sensor of type {@link HpsSiSensor}
     * @return the DAQ pair associated with the sensor
     */
    static Pair<Integer, Integer> getDaqPair(SvtDaqMappingCollection daqMap, final HpsSiSensor sensor) {

        final String svtHalf = sensor.isTopLayer() ? AbstractSvtDaqMapping.TOP_HALF : AbstractSvtDaqMapping.BOTTOM_HALF;
        for (final SvtDaqMapping object : daqMap) {

            if (svtHalf.equals(object.getSvtHalf()) && object.getLayerNumber() == sensor.getLayerNumber()
                    && object.getSide().equals(sensor.getSide())) {

                return new Pair<Integer, Integer>(object.getFebID(), object.getFebHybridID());
            }
        }
        return null;
    }
    
    /**
     * Get a test run DAQ pair (FPGA and Hybrid ID) for the given {@linkplain HpsTestRunSiSensor}.
     *
     * @param sensor a sensor of type {@link HpsTestRunSiSensor}
     * @return the DAQ pair associated with the sensor
     */
    static Pair<Integer, Integer> getTestRunDaqPair(TestRunSvtDaqMappingCollection daqMap, final HpsSiSensor sensor) {

        final String svtHalf = sensor.isTopLayer() ? AbstractSvtDaqMapping.TOP_HALF : AbstractSvtDaqMapping.BOTTOM_HALF;
        for (final TestRunSvtDaqMapping daqMapping : daqMap) {

            if (svtHalf.equals(daqMapping.getSvtHalf()) && daqMapping.getLayerNumber() == sensor.getLayerNumber()) {

                return new Pair<Integer, Integer>(daqMapping.getFpgaID(), daqMapping.getHybridID());
            }
        }
        return null;
    }
    
}
