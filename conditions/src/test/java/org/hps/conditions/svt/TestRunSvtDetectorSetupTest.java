package org.hps.conditions.svt;

import java.util.List;

import junit.framework.TestCase;

import org.hps.conditions.DatabaseConditionsManager;
import org.lcsim.detector.tracker.silicon.HpsTestRunSiSensor;
import org.lcsim.geometry.Detector;

/**
 * This test loads {@link TestRunSvtConditions} data onto the detector and then
 * checks that all channels of each sensor have non-zero data values for
 * applicable parameters.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public class TestRunSvtDetectorSetupTest extends TestCase {

    // --- Constants ---//
    // -----------------//
    // TODO: Move all of these constants to their own class

    // Total number of SVT sensors
    public static final int TOTAL_NUMBER_OF_SENSORS = 20;
    // Max FEB ID
    public static final int MAX_FPGA_ID = 6;
    // Max FEB Hybrid ID
    public static final int MAX_HYBRID_ID = 2;
    // Max channel number
    public static final int MAX_CHANNEL_NUMBER = 639;
    // SVT Subdetector name
    public static final String SVT_SUBDETECTOR_NAME = "Tracker";

    /**
     * Load SVT conditions data onto the detector and perform basic checks
     * afterwards.
     */
    public void test() throws Exception {

        DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
        conditionsManager.configure("/org/hps/conditions/config/conditions_database_testrun_2012.xml");
        conditionsManager.setDetector("HPS-TestRun-v5", 1351);

        // Get the detector.
        Detector detector = conditionsManager.getCachedConditions(Detector.class, "compact.xml").getCachedData();

        // Get all test run SVT conditions.
        TestRunSvtConditions conditions = conditionsManager.getCachedConditions(TestRunSvtConditions.class, "svt_conditions").getCachedData();

        // Load the test run SVT conditions onto detector.
        TestRunSvtDetectorSetup loader = new TestRunSvtDetectorSetup();
        loader.load(detector.getSubdetector(SVT_SUBDETECTOR_NAME), conditions);

        // Check sensor data.
        List<HpsTestRunSiSensor> sensors = detector.getSubdetector(SVT_SUBDETECTOR_NAME).getDetectorElement().findDescendants(HpsTestRunSiSensor.class);

        // Check for correct number of sensors processed.
        this.printDebug("Total number of sensors found: " + sensors.size());
        assertTrue(sensors.size() == TOTAL_NUMBER_OF_SENSORS);

        // Loop over sensors.
        int totalSensors = 0;
        for (HpsTestRunSiSensor sensor : sensors) {

            int nChannels = sensor.getNumberOfChannels();
            assertTrue("The number of channels this sensor has is invalid", nChannels <= MAX_CHANNEL_NUMBER);

            this.printDebug(sensor.toString());

            // Check that the FEB ID as within the appropriate range
            int fpgaID = sensor.getFpgaID();
            assertTrue("FPGA ID is invalid.  The FPGA ID should be less than " + MAX_FPGA_ID, fpgaID <= MAX_FPGA_ID);

            int hybridID = sensor.getHybridID();
            assertTrue("Hybrid ID is invalid.  The Hybrid ID should be less than " + MAX_HYBRID_ID, hybridID <= MAX_HYBRID_ID);

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
        }
        System.out.println("Successfully loaded test run conditions data onto " + totalSensors + " SVT sensors!");
    }

    private void printDebug(String debugMessage) {
        System.out.println(this.getClass().getSimpleName() + ":: " + debugMessage);
    }

}
