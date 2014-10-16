package org.hps.conditions;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

/**
 * This class tests that {@link org.lcsim.hps.conditions.TestRunConditionsDriver} works correctly
 * by checking the number of runs it processes.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ConditionsDriverTest extends TestCase {

    // This test file has a few events from each of the "good runs" of the 2012 Test Run.
    private static final String fileLocation = "http://www.lcsim.org/test/hps-java/ConditionsTest.slcio";

    // Number of runs that should be processed in the job.
    static final int NRUNS = 9;

    /**
     * Run the test.
     * @throws Exception
     */
    public void test() throws Exception {

        // Cache a data file from the www.
        FileCache cache = new FileCache();
        File testFile = cache.getCachedFile(new URL(fileLocation));

        // Create the record loop.
        LCSimLoop loop = new LCSimLoop();

        // Configure the loop.
        loop.setLCIORecordSource(testFile);
        TestRunConditionsDriver conditionsDriver = new TestRunConditionsDriver();
        conditionsDriver.setLoadSvtConditions(false);
        loop.add(conditionsDriver);
        RunNumberDriver runNumberDriver = new RunNumberDriver();
        loop.add(runNumberDriver);

        // Turn off the log messages.
        // DatabaseConditionsManager.getInstance().setLogLevel(Level.OFF);

        // Run over all events.
        loop.loop(-1, null);

        System.out.println("Done processing events!");

        // Print out unique runs.
        System.out.println("Unique run numbers in this job ...");
        for (int runNumber : runNumberDriver.getUniqueRuns()) {
            System.out.println(runNumber);
        }

        System.out.println();

        // Print out runs processed.
        System.out.println("Processed runs in order ...");
        for (int runNumber : runNumberDriver.getRunsProcessed()) {
            System.out.println(runNumber);
        }

        // Check that correct number of runs was processed.
        assertEquals("Number of runs processed was incorrect.", NRUNS, runNumberDriver.getNumberOfRuns());

        // Check that the number of unique runs was correct.
        assertEquals("Number of unique runs was incorrect.", NRUNS, runNumberDriver.getUniqueRuns().size());
    }

    /**
     * Simple Driver to store information about runs processed.
     */
    static class RunNumberDriver extends Driver {

        int currentRun = -1;
        int nruns = 0;
        List<Integer> runsProcessed = new ArrayList<Integer>();
        Set<Integer> uniqueRuns = new LinkedHashSet<Integer>();

        public void process(EventHeader event) {
            int runNumber = event.getRunNumber();
            if (runNumber != currentRun) {
                currentRun = runNumber;
                uniqueRuns.add(currentRun);
                runsProcessed.add(currentRun);
                nruns++;
            }
        }

        int getNumberOfRuns() {
            return nruns;
        }

        List<Integer> getRunsProcessed() {
            return runsProcessed;
        }

        Set<Integer> getUniqueRuns() {
            return uniqueRuns;
        }
    }
}
