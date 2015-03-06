package org.hps.conditions.svt;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.database.TableMetaData;
import org.hps.conditions.svt.TestRunSvtDaqMapping.TestRunSvtDaqMappingCollection;

/**
 * This test checks if the test run SVT DAQ map was loaded with reasonable
 * values and is being read correctly from the conditions database.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public class TestRunSvtDaqMappingTest extends TestCase {

    TableMetaData metaData = null;

    // --- Constants ---//
    // -----------------//

    // Total number of SVT sensors
    public static final int TOTAL_NUMBER_OF_SENSORS = 20;
    // Min and max values of the FPGA ID's
    public static final int MIN_FPGA_ID = 0;
    public static final int MAX_FPGA_ID = 6;
    // Min and max values of Hybrid ID's
    public static final int MIN_HYBRID_ID = 0;
    public static final int MAX_HYBRID_ID = 2;
    // Min and max layer number values
    public static final int MIN_LAYER_NUMBER = 1;
    public static final int MAX_LAYER_NUMBER = 10;

    public void test() throws Exception {

        DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
        conditionsManager.setXmlConfig("/org/hps/conditions/config/conditions_database_testrun_2012.xml");
        conditionsManager.setDetector("HPS-TestRun-v5", 1351);

        TestRunSvtDaqMappingCollection daqMappingCollection = conditionsManager.getCachedConditions(TestRunSvtDaqMappingCollection.class, "test_run_svt_daq_map").getCachedData();

        int totalSensors = 0;
        this.printDebug("");
        for (TestRunSvtDaqMapping daqMapping : daqMappingCollection) {

            this.printDebug("Sensor: \n" + daqMapping.toString());

            // Check that the FPGA ID is within the allowable limits
            int fpgaID = daqMapping.getFpgaID();
            assertTrue("FPGA ID " + fpgaID + " is out of range!", fpgaID >= MIN_FPGA_ID && fpgaID <= MAX_FPGA_ID);

            // Check that the Hybrid ID is within the allowable limits
            int hybridID = daqMapping.getHybridID();
            assertTrue("Hybrid ID " + hybridID + " is out of range!", hybridID >= MIN_HYBRID_ID && hybridID <= MAX_HYBRID_ID);

            // Check that the layer number is within the allowable limits
            int layerNumber = daqMapping.getLayerNumber();
            assertTrue("The layer number " + layerNumber + " is out of range!", layerNumber >= MIN_LAYER_NUMBER && layerNumber <= MAX_LAYER_NUMBER);

            totalSensors++;
        }

        this.printDebug("Total number of sensors found: " + totalSensors);
        assertTrue(totalSensors == TOTAL_NUMBER_OF_SENSORS);
    }

    private void printDebug(String debugMessage) {
        System.out.println("[ " + this.getClass().getSimpleName() + " ]: " + debugMessage);
    }
}
