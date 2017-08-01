package org.hps.conditions;

import java.io.File;
import java.net.URL;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.geometry.Detector;
import org.lcsim.job.EventMarkerDriver;
import org.lcsim.util.Driver;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

/**
 * This is a basic test of using ConditionsDriver that doesn't actually check anything at the moment.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class ConditionsDriverTest extends TestCase {

    /**
     * This {@link org.lcsim.util.Driver} prints out when the conditions change.
     */
    static class CheckDriver extends Driver {

        /**
         * Hook for conditions system change.
         * 
         * @param detector the detector object
         */
        @Override
        public void detectorChanged(final Detector detector) {
            System.out.println("detectorChanged - detector " + detector.getDetectorName() + " and run #"
                    + DatabaseConditionsManager.getInstance().getRun());
        }
    }

    /**
     * The run number to use for the test.
     */
    private static final int RUN_NUMBER = 1351;

    /**
     * Test the {@link ConditionsDriver} on Test Run data.
     * 
     * @throws Exception if there is a test error or conditions error
     */
    public void testConditionsDriverTestRun() throws Exception {

        new DatabaseConditionsManager();

        final FileCache cache = new FileCache();
        final File inputFile = cache.getCachedFile(new URL("http://www.lcsim.org/test/hps-java/ConditionsTest.slcio"));

        final ConditionsDriver conditionsDriver = new ConditionsDriver();
        conditionsDriver.setDetectorName("HPS-TestRun-v5");
        conditionsDriver.setTag("test_run");
        conditionsDriver.setRunNumber(RUN_NUMBER);
        conditionsDriver.setFreeze(true);

        final LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(inputFile);
        conditionsDriver.initialize();
        loop.add(new EventMarkerDriver());
        loop.add(new CheckDriver());
        loop.loop(-1);
    }
}
