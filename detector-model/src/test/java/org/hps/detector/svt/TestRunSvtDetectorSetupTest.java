package org.hps.detector.svt;

import java.util.List;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.TestRunSvtConditions;
import org.lcsim.detector.tracker.silicon.HpsTestRunSiSensor;
import org.lcsim.geometry.Detector;

/**
 * This test loads {@link TestRunSvtConditions} data onto the detector and then checks that all channels of each sensor
 * have non-zero data values for applicable parameters.
 *
 * @author Omar Moreno, UCSC
 */
public final class TestRunSvtDetectorSetupTest extends TestCase {

    /**
     * Maximum channel number.
     */
    private static final int MAX_CHANNEL_NUMBER = 639;

    /**
     * Maximum FPGA ID.
     */
    private static final int MAX_FPGA_ID = 6;

    /**
     * Maximum Hybrid ID.
     */
    private static final int MAX_HYBRID_ID = 2;

    /**
     * Run number to use for test.
     */
    private static final int RUN_NUMBER = 1351;

    /**
     * Name of SVT subdetector.
     */
    private static final String SVT_SUBDETECTOR_NAME = "Tracker";

    /**
     * Total number of SVT sensors.
     */
    private static final int TOTAL_NUMBER_OF_SENSORS = 20;

    /**
     * Print debug message.
     * 
     * @param debugMessage the message
     */
    private void printDebug(final String debugMessage) {
        System.out.println(this.getClass().getSimpleName() + ":: " + debugMessage);
    }

    /**
     * Load SVT conditions data onto the detector and then perform basic checks of channel conditions data.
     * 
     * @throws Exception if there is a test error
     */
    public void test() throws Exception {

        final DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
        conditionsManager.setXmlConfig("/org/hps/conditions/config/conditions_database_testrun_2012.xml");
        conditionsManager.setDetector("HPS-TestRun-v5", RUN_NUMBER);

        // Get the detector.
        final Detector detector = conditionsManager.getCachedConditions(Detector.class, "compact.xml").getCachedData();

        // Check sensor data.
        final List<HpsTestRunSiSensor> sensors = detector.getSubdetector(SVT_SUBDETECTOR_NAME).getDetectorElement()
                .findDescendants(HpsTestRunSiSensor.class);

        // Check for correct number of sensors processed.
        this.printDebug("Total number of sensors found: " + sensors.size());
        assertTrue(sensors.size() == TOTAL_NUMBER_OF_SENSORS);

        // Loop over sensors.
        int totalSensors = 0;
        for (final HpsTestRunSiSensor sensor : sensors) {

            final int nChannels = sensor.getNumberOfChannels();
            assertTrue("The number of channels this sensor has is invalid", nChannels <= MAX_CHANNEL_NUMBER);

            this.printDebug(sensor.toString());

            // Check that the FEB ID as within the appropriate range
            final int fpgaID = sensor.getFpgaID();
            assertTrue("FPGA ID is invalid.  The FPGA ID should be less than " + MAX_FPGA_ID, fpgaID <= MAX_FPGA_ID);

            final int hybridID = sensor.getHybridID();
            assertTrue("Hybrid ID is invalid.  The Hybrid ID should be less than " + MAX_HYBRID_ID,
                    hybridID <= MAX_HYBRID_ID);

            for (int channel = 0; channel < nChannels; channel++) {

                //
                // Check that channel conditions values are not zero
                //
                for (int sampleN = 0; sampleN < 6; sampleN++) {
                    assertTrue("Pedestal sample " + sampleN + " is zero.", sensor.getPedestal(channel, sampleN) != 0);
                    assertTrue("Noise sample " + sampleN + " is zero.", sensor.getNoise(channel, sampleN) != 0);
                }
                assertTrue("Gain is zero.", sensor.getGain(channel) != 0);
                assertTrue("Shape fit parameters points to null.", sensor.getShapeFitParameters(channel) != null);

            }
            ++totalSensors;
        }
        System.out.println("Successfully loaded test run conditions data onto " + totalSensors + " SVT sensors!");
    }

}
