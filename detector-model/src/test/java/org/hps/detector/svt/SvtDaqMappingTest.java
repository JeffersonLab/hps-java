package org.hps.detector.svt;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtDaqMapping;
import org.hps.conditions.svt.SvtDaqMapping.SvtDaqMappingCollection;

/**
 * This test checks if the SVT DAQ map was loaded with reasonable values and is being read correctly from the conditions
 * database.
 *
 * @author Omar Moreno, SLAC National Accelerator Laboratory
 */
public final class SvtDaqMappingTest extends TestCase {

    /**
     * Maximum FEB Hybrid ID.
     */
    public static final int MAX_FEB_HYBRID_ID = 3;

    /**
     * Minimum FEB Hybrid ID.
     */
    public static final int MIN_FEB_HYBRID_ID = 0;

    /**
     * Total number of SVT sensors.
     */
    public static final int TOTAL_NUMBER_OF_SENSORS = 40;

    /**
     * Load the DAQ map from the database.
     *
     * @throws Exception if there is a test error
     */
    public void test() throws Exception {
        final DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
        conditionsManager.setDetector("HPS-PhysicsRun2019-v1-4pt5", 0);
        final SvtDaqMappingCollection daqMappingCollection = conditionsManager.getCachedConditions(
                SvtDaqMappingCollection.class, "svt_daq_map").getCachedData();
        int totalSensors = 0;
        int febHybridID;
        System.out.println("");;
        for (final SvtDaqMapping daqMapping : daqMappingCollection) {
            System.out.println("Sensor: \n" + daqMapping.toString());
            // Check that the FEB Hybrid ID is within the allowable limits
            febHybridID = daqMapping.getFebHybridID();
            assertTrue("FEB Hybrid ID is out of range!.", febHybridID >= MIN_FEB_HYBRID_ID
                    && febHybridID <= MAX_FEB_HYBRID_ID);
            totalSensors++;
        }
        System.out.println("Total number of sensors found: " + totalSensors);
        assertTrue(totalSensors == TOTAL_NUMBER_OF_SENSORS);

    }
}
