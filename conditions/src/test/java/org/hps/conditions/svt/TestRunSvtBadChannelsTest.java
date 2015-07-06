package org.hps.conditions.svt;

import java.util.logging.Level;

import junit.framework.TestCase;

import org.hps.conditions.api.ConditionsObjectCollection;
import org.hps.conditions.api.ConditionsSeries;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtBadChannel.SvtBadChannelCollection;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

/**
 * This test reads Test Run bad channel collections from the database using a conditions series and checks that the
 * correct number of channels are flagged using several different runs.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class TestRunSvtBadChannelsTest extends TestCase {

    /*
     * mysql> select collection_id, count(*) from test_run_svt_bad_channels group by collection_id;
     * +---------------+----------+ | collection_id | count(*) | +---------------+----------+ | 1 | 50 | | 2 | 392 | | 3
     * | 427 | | 4 | 457 | | 5 | 298 | | 6 | 424 | | 7 | 424 | +---------------+----------+
     */
    /**
     * The bad channel count for each run.
     */
    private static int[] BAD_CHANNEL_COUNTS = {50, 392, 427, 457, 298, 424, 424};

    /*
     * mysql> select run_start, run_end, name from conditions where table_name like 'test_run_svt_bad_channels';
     * +-----------+---------+---------------------------+ | run_start | run_end | name |
     * +-----------+---------+---------------------------+ | 0 | 1365 | test_run_svt_bad_channels | | 1351 | 1351 |
     * test_run_svt_bad_channels | | 1353 | 1353 | test_run_svt_bad_channels | | 1354 | 1354 | test_run_svt_bad_channels
     * | | 1358 | 1358 | test_run_svt_bad_channels | | 1359 | 1359 | test_run_svt_bad_channels | | 1360 | 1360 |
     * test_run_svt_bad_channels | +-----------+---------+---------------------------+
     */
    /**
     * The run numbers to check.
     */
    private static final int[] RUN_NUMBERS = new int[] {0, 1351, 1353, 1354, 1358, 1359, 1360};

    /**
     * Test the bad channel numbers for various runs of the Test Run.
     * 
     * @throws ConditionsNotFoundException if there is a conditions error
     */
    public void testSvtBadChannels() throws ConditionsNotFoundException {

        final DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
        conditionsManager.setXmlConfig("/org/hps/conditions/config/conditions_database_testrun_2012.xml");
        conditionsManager.setLogLevel(Level.WARNING);

        for (int i = 0; i < RUN_NUMBERS.length; i++) {

            final int runNumber = RUN_NUMBERS[i];

            System.out.println("-------------");
            System.out.println("Run #" + runNumber);
            System.out.println("-------------");
            System.out.println();

            conditionsManager.setDetector("HPS-TestRun-v5", runNumber);

            final ConditionsSeries<SvtBadChannel, SvtBadChannelCollection> series = conditionsManager
                    .getConditionsSeries(SvtBadChannelCollection.class, "test_run_svt_bad_channels");

            int totalBadChannels = 0;
            for (final ConditionsObjectCollection<SvtBadChannel> collection : series) {
                // System.out.println(collection.getConditionsRecord());
                totalBadChannels += collection.size();
            }
            System.out.println("found " + totalBadChannels + " total bad channels");

            // The run 0 channels are for all runs.
            int expectedBadChannels = BAD_CHANNEL_COUNTS[0];
            if (runNumber != 0) {
                // Add bad channels from individual runs.
                expectedBadChannels += BAD_CHANNEL_COUNTS[i];
            }
            System.out.println("expected " + expectedBadChannels + " bad channels");

            assertEquals("Wrong number of bad channels for run #" + runNumber, expectedBadChannels, totalBadChannels);

            System.out.println();
        }
    }
}
