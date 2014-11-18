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
 * This class checks that event processing works correctly for files that 
 * have multiple runs in them.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class RunNumberTest extends TestCase {

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
        new DatabaseConditionsManager();
        //DatabaseConditionsManager.getInstance().setLogLevel(Level.WARNING);
        LCSimLoop loop = new LCSimLoop();

        // Configure the loop.
        loop.setLCIORecordSource(testFile);
        RunNumberDriver runNumberDriver = new RunNumberDriver();
        loop.add(runNumberDriver);

        // Run over all events.
        loop.loop(-1, null);

        System.out.println("Done processing events!");

        // Print out unique runs.
        System.out.println("Unique run numbers in this job ...");
        for (int runNumber : runNumberDriver.uniqueRuns) {
            System.out.println(runNumber);
        }

        System.out.println();

        // Print out runs processed.
        System.out.println("Processed runs in order ...");
        for (int runNumber : runNumberDriver.runsProcessed) {
            System.out.println(runNumber);
        }

        // Check that correct number of runs was processed.
        assertEquals("Number of runs processed was incorrect.", NRUNS, runNumberDriver.nRuns);

        // Check that the number of unique runs was correct.
        assertEquals("Number of unique runs was incorrect.", NRUNS, runNumberDriver.uniqueRuns.size());

        // Check that detectorChanged was called the correct number of times.
        assertEquals("The detectorChanged method was called an incorrect number of times.", NRUNS, runNumberDriver.nDetectorChanged);
        
        // Check that there was a unique Detector created for each run.
        assertEquals("The number of Detector objects created was not correct.", NRUNS, runNumberDriver.uniqueDetectorObjects.size());
    }

    /**
     * Simple Driver to store information about runs processed.
     */
    static class RunNumberDriver extends Driver {

        int nRuns = 0;
        int nDetectorChanged = 0;
        List<Integer> runsProcessed = new ArrayList<Integer>();
        Set<Integer> uniqueRuns = new LinkedHashSet<Integer>();  
        Set<Detector> uniqueDetectorObjects = new HashSet<Detector>();
        static DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
        
        public void detectorChanged(Detector detector) {
            System.out.println("detectorChanged - detector " + detector.getDetectorName() + " and run #" + conditionsManager.getRun());
            int run = conditionsManager.getRun();
            uniqueRuns.add(run);
            runsProcessed.add(run);
            ++nRuns;
            ++nDetectorChanged;
            uniqueDetectorObjects.add(detector);
        }
    }
}
