package org.hps.conditions.svt;

import java.util.logging.Level;

import junit.framework.TestCase;

import org.hps.conditions.api.ConditionsRecord;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtTimingConstants.SvtTimingConstantsCollection;

/**
 * Read SVT timing constants from the conditions database.
 *
 * @author Jeremy McCormick, SLAC
 */
public class SvtTimingConstantsTest extends TestCase {

    /**
     * Just use a dummy detector.
     */
    private static final String DETECTOR = "HPS-dummy-detector";

    /**
     * This is a list of run start values to check.
     */
    private static final int[] RUNS = new int[] {4871, 5038, 5076, 5139, 5174, 5218, 5236, 5251, 5263, 5299, 5310,
            5375, 5400, 5533, 5558, 5575, 5596, 5601, 5603, 5610, 4871, 5038, 5076, 5139, 5174, 5218, 5236, 5251, 5263,
            5299, 5310, 5375, 5400, 5533, 5558, 5575, 5596, 5601, 5603, 5610, 5640, 5641, 5642, 5686, 5722, 5779};

    /**
     * Load SVT timing constants and print them out by run range.
     *
     * @throws Exception if any error occurs
     */
    public void testSvtTimingConstants() throws Exception {
        final DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        manager.setLogLevel(Level.SEVERE);
        // manager.setConnectionResource("/org/hps/conditions/config/jeremym_dev_connection.prop");
        for (final int run : RUNS) {
            manager.setDetector(DETECTOR, run);
            final SvtTimingConstantsCollection collection = manager.getCachedConditions(
                    SvtTimingConstantsCollection.class, "svt_timing_constants").getCachedData();
            final ConditionsRecord condi = manager.findConditionsRecords("svt_timing_constants").get(0);
            System.out.println("run_start: " + condi.getRunStart() + ", run_end: " + condi.getRunEnd()
                    + ", offset_phase: " + collection.get(0).getOffsetPhase() + ", offset_time: "
                    + collection.get(0).getOffsetTime());
        }
    }
}
