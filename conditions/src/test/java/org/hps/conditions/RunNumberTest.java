package org.hps.conditions;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

/**
 * This class checks that event processing works correctly for files that have multiple runs in them.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class RunNumberTest extends TestCase {

    /**
     * Simple Driver to store information about runs processed.
     */
    static class RunNumberDriver extends Driver {

        /**
         * Reference to conditions manager.
         */
        private static DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();

        /**
         * Number of times {@link #detectorChanged(Detector)} was called.
         */
        private int nDetectorChanged = 0;

        /**
         * Number of runs processed.
         */
        private int nRuns = 0;

        /**
         * List of run numbers processed.
         */
        private final List<Integer> runsProcessed = new ArrayList<Integer>();

        /**
         * Set of unique detector objects.
         */
        private final Set<Detector> uniqueDetectorObjects = new HashSet<Detector>();

        /**
         * Set of unique run numbers processed.
         */
        private final Set<Integer> uniqueRuns = new LinkedHashSet<Integer>();

        /**
         * Hook for conditions changed used to test multiple run processing.
         */
        @Override
        public void detectorChanged(final Detector detector) {
            final int run = conditionsManager.getRun();
            this.uniqueRuns.add(run);
            this.runsProcessed.add(run);
            ++this.nRuns;
            ++this.nDetectorChanged;
            this.uniqueDetectorObjects.add(detector);
        }
    }

    /**
     * Number of runs that should be processed in the job.
     */
    private static final int RUN_COUNT = 9;

    /**
     * Test file with a few events from each of the "good runs" of the 2012 Test Run.
     */
    private static final String URL = "http://www.lcsim.org/test/hps-java/ConditionsTest.slcio";

    /**
     * Run the test.
     * 
     * @throws Exception if there is a test error
     */
    public void test() throws Exception {

        // Cache a data file from the www.
        final FileCache cache = new FileCache();
        final File testFile = cache.getCachedFile(new URL(URL));

        // Create the record loop.
        final DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        manager.setLogLevel(Level.WARNING);
        final LCSimLoop loop = new LCSimLoop();

        // Configure the loop.
        loop.setLCIORecordSource(testFile);
        final RunNumberDriver runNumberDriver = new RunNumberDriver();
        loop.add(runNumberDriver);

        // Run over all events.
        loop.loop(-1, null);

        System.out.println("Done processing events!");

        // Print out unique runs.
        System.out.println("Unique run numbers in this job ...");
        for (final int runNumber : runNumberDriver.uniqueRuns) {
            System.out.println(runNumber);
        }

        System.out.println();

        // Print out runs processed.
        System.out.println("Processed runs in order ...");
        for (final int runNumber : runNumberDriver.runsProcessed) {
            System.out.println(runNumber);
        }

        // Check that correct number of runs was processed.
        assertEquals("Number of runs processed was incorrect.", RUN_COUNT, runNumberDriver.nRuns);

        // Check that the number of unique runs was correct.
        assertEquals("Number of unique runs was incorrect.", RUN_COUNT, runNumberDriver.uniqueRuns.size());

        // Check that detectorChanged was called the correct number of times.
        assertEquals("The detectorChanged method was called an incorrect number of times.", RUN_COUNT,
                runNumberDriver.nDetectorChanged);

        // Check that there was a unique Detector created for each run.
        assertEquals("The number of Detector objects created was not correct.", RUN_COUNT,
                runNumberDriver.uniqueDetectorObjects.size());
    }
}
