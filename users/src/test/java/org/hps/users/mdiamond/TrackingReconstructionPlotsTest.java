package org.hps.users.mdiamond;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;

import java.io.File;
import java.io.IOException;
//import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
//import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCIODriver;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * Test class to check MergeTrackCollections (ambiguity resolving).
 * 
 * @author Miriam Diamond <mdiamond@slac.stanford.edu>
 * @version $id: v1 05/30/2017$
 * 
 *          Optional step: creates reconstructed lcio from raw lcio. Always:
 *          reads reconstructed lcio, runs MergeTrackCollections to create new
 *          reconstructed lcio, makes plots from new lcio
 */
public class TrackingReconstructionPlotsTest extends TestCase {

    static final String testInput = "MCReconTest.slcio";
    // static final String testURLBase = "http://www.lcsim.org/test/hps-java";
    static final String testFileName = "MCReconTest.slcio";
    // static final String originalGoodName = "origGood";
    // static final String originalPartialName = "origPartial";


    private final int nEvents = 10;

    public void testMerging() throws Exception {
        // URL testURL = new URL(testURLBase + "/" + testFileName);
        // FileCache cache = new FileCache();
        File lcioInputFile;
        File trackFile;
        File outputFile = new TestOutputFile(testFileName.replaceAll(".slcio",
                "") + "_MergeTest2.slcio");
        outputFile.getParentFile().mkdirs(); // make sure the parent directory
                                             // exists


        // Run merging
        LCSimLoop loop2 = new LCSimLoop();
        loop2.setLCIORecordSource(trackFile);

        /*
         * MergeTrackCollectionsOld originalMerge = new
         * MergeTrackCollectionsOld();
         * originalMerge.setOutputCollectionName(originalGoodName);
         * originalMerge.setPartialTrackCollectionName(originalPartialName);
         * originalMerge.setRemoveCollections(false); loop2.add(originalMerge);
         */

        TrackingReconstructionPlots trp = new TrackingReconstructionPlots();
        // newMerge.setPartialTrackCollectionName(newPartialName);
        loop2.add(trp);

        // evaluate results
        loop2.add(new MergePlotsDriver());
        loop2.add(new LCIODriver(outputFile));
        loop2.loop(nEvents, null);
        loop2.dispose();


    }

}
