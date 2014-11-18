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
 * This class tests that the correct bad channel conditions are found for the
 * test run.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtBadChannelTest extends TestCase {

    // This test file has a few events from each of the "good runs" of the 2012
    // Test Run.
    private static final String TEST_FILE_URL = "http://www.lcsim.org/test/hps/conditions_test.slcio";

    // This is the number of bad channels in the QA set across all runs.
    static int BAD_CHANNELS_QA_ANSWER = 50;

    // Answer key for number of bad channels by run.
    static Map<Integer, Integer> badChannelAnswerKey = new HashMap<Integer, Integer>();

    // Setup the bad channel answer key by run.
    static {
        badChannelAnswerKey.put(1351, 441);
        badChannelAnswerKey.put(1353, 473);
        badChannelAnswerKey.put(1354, 474);
        badChannelAnswerKey.put(1358, 344);
        badChannelAnswerKey.put(1359, 468);
        badChannelAnswerKey.put(1360, 468);
    }

    /**
     * Run the test.
     * @throws Exception
     */
    public void test() throws Exception {

        // Cache a data file from the www.
        FileCache cache = new FileCache();
        File testFile = cache.getCachedFile(new URL(TEST_FILE_URL));

        // Create the record loop.
        new DatabaseConditionsManager();
        LCSimLoop loop = new LCSimLoop();

        // Configure the loop.
        loop.setLCIORecordSource(testFile);
        loop.add(new SvtBadChannelChecker());

        DatabaseConditionsManager.getInstance().setLogLevel(Level.OFF);

        // Run over all events.
        loop.loop(-1, null);
    }

    /**
     * This Driver will check the number of bad channels for a run against the
     * answer key.
     * @author Jeremy McCormick <jeremym@slac.stanford.edu>
     */
    class SvtBadChannelChecker extends Driver {

        int _currentRun = -1;

        /**
         * This method will check the number of bad channels against the answer
         * key for the first event of a new run.
         */
        public void process(EventHeader event) {
            int run = event.getRunNumber();
            if (run != _currentRun) {
                _currentRun = run;
                Detector detector = event.getDetector();
                int badChannels = 0;
                List<HpsSiSensor> sensors = detector.getDetectorElement().findDescendants(HpsSiSensor.class);
                for (HpsSiSensor sensor : sensors) {
                    int nchannels = sensor.getNumberOfChannels();
                    for (int i = 0; i < nchannels; i++) {
                        if (sensor.isBadChannel(i))
                            ++badChannels;
                    }
                }
                System.out.println("Run " + _currentRun + " has " + badChannels + " SVT bad channels.");
                if (badChannelAnswerKey.containsKey(_currentRun)) {
                    Integer badChannelsExpected = badChannelAnswerKey.get(run);
                    TestCase.assertEquals("Wrong number of bad channels found.", (int) badChannelsExpected, (int) badChannels);
                } else {
                    TestCase.assertEquals("Wrong number of bad channels found.", (int) BAD_CHANNELS_QA_ANSWER, (int) badChannels);
                }
            }
        }
    }
}
