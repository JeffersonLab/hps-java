package org.hps.conditions.svt;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.TestRunSvtDaqMapping.TestRunSvtDaqMappingCollection;

/**
 * This test checks if the test run SVT DAQ map was loaded with reasonable
 * values and is being read correctly from the conditions database.
 *
 * @author <a href="mailto:omoreno1@ucsc.edu">Omar Moreno</a>
 */
public final class TestRunSvtDaqMappingTest extends TestCase {

    /**
     * The run number to use for the test.
     */
    private static final int RUN_NUMBER = 1351;

    /**
     * Total number of SVT sensors.
     */
    private static final int TOTAL_NUMBER_OF_SENSORS = 20;

    /**
     * Minimum FPGA ID.
     */
    private static final int MIN_FPGA_ID = 0;

    /**
     * Maximum FPGA ID.
     */
    private static final int MAX_FPGA_ID = 6;

    /**
     * Minimum Hybrid ID.
     */
    private static final int MIN_HYBRID_ID = 0;

    /**
     * Maximum Hybrid ID.
     */
    private static final int MAX_HYBRID_ID = 2;

    /**
     * Minimum layer number.
     */
    private static final int MIN_LAYER_NUMBER = 1;

    /**
     * Maximum layer number.
     */
    private static final int MAX_LAYER_NUMBER = 10;

    /**
     * Perform checks of SVT DAQ mapping for Test Run.
     * @throws Exception if there is a test or conditions error
     */
    public void test() throws Exception {

        final DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
        conditionsManager.setXmlConfig("/org/hps/conditions/config/conditions_database_testrun_2012.xml");
        conditionsManager.setDetector("HPS-TestRun-v5", RUN_NUMBER);

        final TestRunSvtDaqMappingCollection daqMappingCollection = conditionsManager.getCachedConditions(
                TestRunSvtDaqMappingCollection.class, "test_run_svt_daq_map").getCachedData();

        int totalSensors = 0;
        this.printDebug("");
        for (TestRunSvtDaqMapping daqMapping : daqMappingCollection) {

            this.printDebug("Sensor: \n" + daqMapping.toString());

            // Check that the FPGA ID is within the allowable limits
            final int fpgaID = daqMapping.getFpgaID();
            assertTrue("FPGA ID " + fpgaID + " is out of range!", fpgaID >= MIN_FPGA_ID && fpgaID <= MAX_FPGA_ID);

            // Check that the Hybrid ID is within the allowable limits
            final int hybridID = daqMapping.getHybridID();
            assertTrue("Hybrid ID " + hybridID + " is out of range!",
                    hybridID >= MIN_HYBRID_ID && hybridID <= MAX_HYBRID_ID);

            // Check that the layer number is within the allowable limits
            final int layerNumber = daqMapping.getLayerNumber();
            assertTrue("The layer number " + layerNumber + " is out of range!",
                    layerNumber >= MIN_LAYER_NUMBER && layerNumber <= MAX_LAYER_NUMBER);

            totalSensors++;
        }

        this.printDebug("Total number of sensors found: " + totalSensors);
        assertTrue(totalSensors == TOTAL_NUMBER_OF_SENSORS);
    }

    /**
     * Print debug message.
     * @param debugMessage the message
     */
    private void printDebug(final String debugMessage) {
        System.out.println("[ " + this.getClass().getSimpleName() + " ]: " + debugMessage);
    }
}
