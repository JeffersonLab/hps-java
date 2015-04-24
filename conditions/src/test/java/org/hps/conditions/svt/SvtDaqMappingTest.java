package org.hps.conditions.svt;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtDaqMapping.SvtDaqMappingCollection;

/**
 * This test checks if the SVT DAQ map was loaded with reasonable values and is being read correctly from the conditions
 * database.
 *
 * @author <a href="mailto:omoreno1@ucsc.edu">Omar Moreno</a>
 */
public final class SvtDaqMappingTest extends TestCase {

    /**
     * Total number of SVT sensors.
     */
    public static final int TOTAL_NUMBER_OF_SENSORS = 36;

    /**
     * Minimum FEB Hybrid ID.
     */
    public static final int MIN_FEB_HYBRID_ID = 0;

    /**
     * Maximum FEB Hybrid ID.
     */
    public static final int MAX_FEB_HYBRID_ID = 3;

    /**
     * Load the DAQ map from the database.
     *
     * @throws Exception if there is a test error
     */
    public void test() throws Exception {
        final DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
        conditionsManager.setDetector("HPS-Proposal2014-v7-2pt2", 0);
        final SvtDaqMappingCollection daqMappingCollection = conditionsManager.getCachedConditions(
                SvtDaqMappingCollection.class, "svt_daq_map").getCachedData();
        int totalSensors = 0;
        int febHybridID;
        // this.printDebug("");
        for (final SvtDaqMapping daqMapping : daqMappingCollection) {
            // this.printDebug("Sensor: \n" + daqMapping.toString());
            // Check that the FEB Hybrid ID is within the allowable limits
            febHybridID = daqMapping.getFebHybridID();
            assertTrue("FEB Hybrid ID is out of range!.", febHybridID >= MIN_FEB_HYBRID_ID
                    && febHybridID <= MAX_FEB_HYBRID_ID);
            totalSensors++;
        }
        // this.printDebug("Total number of sensors found: " + totalSensors);
        assertTrue(totalSensors == TOTAL_NUMBER_OF_SENSORS);

    }
}
