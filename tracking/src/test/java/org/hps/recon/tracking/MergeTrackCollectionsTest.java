package org.hps.recon.tracking;

import java.io.File;
//import java.net.URL;
import junit.framework.TestCase;

import org.hps.recon.tracking.MergeTrackCollections;
//import org.lcsim.recon.tracking.digitization.sisim.config.RawTrackerHitSensorSetup;
//import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCIODriver;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * Test class to check MergeTrackCollections (ambiguity resolving).
 * 
 * @author Miriam Diamond <mdiamond@slac.stanford.edu>
 * @version $id: v2 06/04/2017$
 * 
 * re-runs MergeTrackCollections on reco lcio, creates new reco lcio (outputFileName)
 * output track collection (newTracksName) may be same or different as old one
 */
public class MergeTrackCollectionsTest extends TestCase {

    // static final String testURLBase = "http://www.lcsim.org/test/hps-java";
    static final String testFileName = null;
    static final String outputFileName = testFileName.replaceAll(".slcio", "") + "_MergeTest.slcio";
    static final String newTracksName = "MatchedTracks";
    static final String inputTracksName = "MatchedTracks";

    private final int nEvents = 10;

    public void testMerging() throws Exception {
        // URL testURL = new URL(testURLBase + "/" + testFileName);
        // FileCache cache = new FileCache();

        if (testFileName == null)
            return;

        File outputFile = new TestOutputFile(outputFileName);
        outputFile.getParentFile().mkdirs();
        File trackFile = new File("target/test-output/" + testFileName);
        trackFile.getParentFile().mkdirs();

        // Run merging
        LCSimLoop loop2 = new LCSimLoop();
        loop2.setLCIORecordSource(trackFile);

        MergeTrackCollections newMerge = new MergeTrackCollections();
        newMerge.setInputTrackCollectionName(inputTracksName);
        newMerge.setOutputCollectionName(newTracksName);
        newMerge.setRemoveCollections(false);
        newMerge.setDoPlots(true);
        loop2.add(newMerge);

        loop2.add(new LCIODriver(outputFile));
        loop2.loop(nEvents, null);
        loop2.dispose();
    }
}
