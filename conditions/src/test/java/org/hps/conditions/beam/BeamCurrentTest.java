package org.hps.conditions.beam;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.hps.conditions.beam.BeamCurrent.BeamCurrentCollection;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

/**
 * This test checks the Test Run beam current values by run.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class BeamCurrentTest extends TestCase {

    /**
     * This test file has a few events from the "good runs" of the Test Run.
     */
    private static final String URL = "http://www.lcsim.org/test/hps-java/ConditionsTest.slcio";

    /** Answer key for beam current by run. */
    private static final Map<Integer, Double> ANSWER_KEY = new HashMap<Integer, Double>();

    /** Setup the beam current answer key by run. */
    static {
        ANSWER_KEY.put(1349, 54879.7343788147);
        ANSWER_KEY.put(1351, 26928.0426635742);
        ANSWER_KEY.put(1353, 204325.132622242);
        ANSWER_KEY.put(1354, 148839.141475141);
        ANSWER_KEY.put(1358, 92523.9428218845);
        ANSWER_KEY.put(1359, 91761.4541434497);
        ANSWER_KEY.put(1360, 209883.979889035);
        ANSWER_KEY.put(1362, 110298.553449392);
        ANSWER_KEY.put(1363, 8556.8459701538);
    }

    /**
     * Run the test.
     *
     * @throws Exception if there is an event processing error
     */
    public void test() throws Exception {

        DatabaseConditionsManager.getInstance();

        // Cache file locally from URL.
        final FileCache cache = new FileCache();
        final File testFile = cache.getCachedFile(new URL(URL));

        // Create the LCSimLoop.
        final LCSimLoop loop = new LCSimLoop();

        // Configure and run the loop.
        loop.setLCIORecordSource(testFile);
        loop.add(new BeamCurrentChecker());
        loop.loop(-1, null);
    }

    /**
     * This Driver will check the beam current for a run against the answer key.
     */
    static class BeamCurrentChecker extends Driver {

        /**
         * The current run number.
         */
        private int currentRun = Integer.MIN_VALUE;

        /**
         * This method will check the beam current against the answer key for the first event of a new run.
         *
         * @param the LCSim event
         */
        @Override
        protected void process(final EventHeader event) {
            if (this.currentRun != event.getRunNumber()) {
                this.currentRun = event.getRunNumber();
                final BeamCurrentCollection collection = DatabaseConditionsManager.getInstance()
                        .getCachedConditions(BeamCurrentCollection.class, "beam_current").getCachedData();
                final BeamCurrent beamCurrent = collection.iterator().next();
                System.out.println("Run " + event.getRunNumber() + " has integrated beam current "
                        + beamCurrent.getIntegratedBeamCurrent() + " nC.");
                assertEquals("Wrong beam current for run.", ANSWER_KEY.get(this.currentRun),
                        beamCurrent.getIntegratedBeamCurrent());
            }
        }
    }
}
