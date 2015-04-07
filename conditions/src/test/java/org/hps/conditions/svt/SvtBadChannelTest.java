package org.hps.conditions.svt;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

/**
 * This class tests that the correct bad channel conditions are found for the test run.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class SvtBadChannelTest extends TestCase {

    /**
     * This test file has a few events from each of the "good runs" of the 2012 Test Run.
     */
    private static final String TEST_FILE_URL = "http://www.lcsim.org/test/hps/conditions_test.slcio";

    /**
     * This is the number of bad channels in the conditions set that covers all run numbers.
     */
    private static final int BAD_CHANNELS_QA_ANSWER = 50;

    /**
     * Answer key for number of bad channels per run.
     */
    private static final Map<Integer, Integer> ANSWER_KEY = new HashMap<Integer, Integer>();

    /**
     * Setup the answer key.
     */
    static {
        ANSWER_KEY.put(1351, 441);
        ANSWER_KEY.put(1353, 473);
        ANSWER_KEY.put(1354, 474);
        ANSWER_KEY.put(1358, 344);
        ANSWER_KEY.put(1359, 468);
        ANSWER_KEY.put(1360, 468);
    }

    /**
     * Run the test.
     * @throws Exception if there is an event processing error
     */
    public void test() throws Exception {

        // Cache a data file from the www.
        final FileCache cache = new FileCache();
        final File testFile = cache.getCachedFile(new URL(TEST_FILE_URL));

        // Create the record loop.
        DatabaseConditionsManager.getInstance();
        final LCSimLoop loop = new LCSimLoop();

        // Configure the loop.
        loop.setLCIORecordSource(testFile);
        loop.add(new SvtBadChannelChecker());

        DatabaseConditionsManager.getInstance().setLogLevel(Level.OFF);

        // Run over all events.
        loop.loop(-1, null);
    }

    /**
     * This Driver will check the number of bad channels for a run against the answer key.
     */
    static class SvtBadChannelChecker extends Driver {

        /**
         * The current run number.
         */
        private int currentRun = -1;

        /**
         * This method will check the number of bad channels against the answer
         * key for the first event of a new run.
         * @param the LCSim event
         */
        public void process(final EventHeader event) {
            final int run = event.getRunNumber();
            if (run != currentRun) {
                currentRun = run;
                final Detector detector = event.getDetector();
                int badChannels = 0;
                final List<HpsSiSensor> sensors = detector.getDetectorElement()
                        .findDescendants(HpsSiSensor.class);
                for (final HpsSiSensor sensor : sensors) {
                    final int nchannels = sensor.getNumberOfChannels();
                    for (int i = 0; i < nchannels; i++) {
                        if (sensor.isBadChannel(i)) {
                            ++badChannels;
                        }
                    }
                }
                System.out.println("Run " + currentRun + " has " + badChannels + " SVT bad channels.");
                if (ANSWER_KEY.containsKey(currentRun)) {
                    final Integer badChannelsExpected = ANSWER_KEY.get(run);
                    TestCase.assertEquals("Wrong number of bad channels found.",
                            (int) badChannelsExpected, (int) badChannels);
                } else {
                    TestCase.assertEquals("Wrong number of bad channels found.",
                            (int) BAD_CHANNELS_QA_ANSWER, (int) badChannels);
                }
            }
        }
    }
}
