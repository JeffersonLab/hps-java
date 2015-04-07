package org.hps.conditions.svt;

import java.util.List;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.geometry.Detector;

/**
 * This test loads {@link SvtConditions} data onto the detector and then checks
 * that all channels of each sensor have non-zero data values for applicable
 * parameters.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 * @author <a href="mailto:omoreno1@ucsc.edu">Omar Moreno</a>
 */
// TODO: Update this test with more meaningful assertions.
public final class SvtDetectorSetupTest extends TestCase {

    /**
     * Total number of SVT sensors.
     */
    public static final int TOTAL_NUMBER_OF_SENSORS = 36;

    /**
     * Maximum FEB ID.
     */
    public static final int MAX_FEB_ID = 9;

    /**
     * Maximum FEB Hybrid ID.
     */
    public static final int MAX_FEB_HYBRID_ID = 3;

    /**
     * Maximum channel number.
     */
    public static final int MAX_CHANNEL_NUMBER = 639;

    /**
     * Name of SVT subdetector.
     */
    public static final String SVT_SUBDETECTOR_NAME = "Tracker";

    /**
     * Load SVT conditions data onto the detector and then perform basic checks.
     * @throws Exception if there is a test error
     */
    public void test() throws Exception {

        final DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
        conditionsManager.setDetector("HPS-Proposal2014-v7-2pt2", 0);

        // Get the detector.
        final Detector detector = conditionsManager.getCachedConditions(Detector.class, "compact.xml").getCachedData();

        // Get all SVT conditions.
        final SvtConditions conditions = conditionsManager.getCachedConditions(
                SvtConditions.class, "svt_conditions").getCachedData();

        // Load the SVT conditions onto detector.
        final SvtDetectorSetup loader = new SvtDetectorSetup("Tracker");
        loader.loadDefault(detector.getSubdetector(SVT_SUBDETECTOR_NAME), conditions);

        // Check sensor data.
        final List<HpsSiSensor> sensors = detector.getSubdetector(SVT_SUBDETECTOR_NAME).getDetectorElement()
                .findDescendants(HpsSiSensor.class);

        // Check for correct number of sensors processed.
        this.printDebug("Total number of sensors found: " + sensors.size());
        assertTrue(sensors.size() == TOTAL_NUMBER_OF_SENSORS);

        // Loop over sensors.
        int totalSensors = 0;
        for (HpsSiSensor sensor : sensors) {

            final int nChannels = sensor.getNumberOfChannels();
            assertTrue("The number of channels this sensor has is invalid", nChannels <= MAX_CHANNEL_NUMBER);

            this.printDebug(sensor.toString());

            // Check that the FEB ID as within the appropriate range
            final int febID = sensor.getFebID();
            assertTrue("FEB ID is invalid.  The FEB ID should be less than " + MAX_FEB_ID, febID <= MAX_FEB_ID);

            final int febHybridID = sensor.getFebHybridID();
            assertTrue("FEB Hybrid ID is invalid.  The FEB Hybrid ID should be less than "
                    + MAX_FEB_HYBRID_ID, febHybridID <= MAX_FEB_HYBRID_ID);

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

        System.out.println("Successfully loaded conditions data onto " + totalSensors + " SVT sensors!");
    }

    /**
     * Print debug message.
     * @param debugMessage the message
     */
    private void printDebug(String debugMessage) {
        System.out.println(this.getClass().getSimpleName() + ":: " + debugMessage);
    }
}
