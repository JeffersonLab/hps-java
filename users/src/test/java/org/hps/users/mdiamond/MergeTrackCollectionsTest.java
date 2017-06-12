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

import org.hps.conditions.ConditionsDriver;
import org.hps.recon.tracking.MergeTrackCollections;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
//import org.lcsim.recon.tracking.digitization.sisim.TrackerHitDriver;
import org.lcsim.recon.tracking.digitization.sisim.config.RawTrackerHitSensorSetup;
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
public class MergeTrackCollectionsTest extends TestCase {

    static final String testInput = "MCReconTest.slcio";
    // static final String testURLBase = "http://www.lcsim.org/test/hps-java";
    static final String testFileName = "MCReconTest.slcio";
    // static final String originalGoodName = "origGood";
    // static final String originalPartialName = "origPartial";
    static final String newGoodName = "MatchedTracksNew";
    // static final String newPartialName = "newPartial";
    static final String inputTracksName = "MatchedTracks";

    // whether to execute optional step
    private final boolean createFirstTracks = true;

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

        // Create tracks
        if (createFirstTracks) {
            trackFile = new TestOutputFile(
                    testFileName.replaceAll(".slcio", "") + "_MergeTest1.slcio");
            trackFile.getParentFile().mkdirs();
            LCSimLoop loop = new LCSimLoop();
            // lcioInputFile = cache.getCachedFile(testURL);
            lcioInputFile = new File(testInput);
            loop.setLCIORecordSource(lcioInputFile);

            ConditionsDriver condDriver = new ConditionsDriver();
            condDriver.setDetectorName("HPS-Proposal2014-v8-2pt2");
            condDriver.setRunNumber(0);
            condDriver.setFreeze(true);
            loop.add(condDriver);

            RawTrackerHitSensorSetup rthss = new RawTrackerHitSensorSetup();
            String[] temp = { "SVTRawTrackerHits" };
            rthss.setReadoutCollections(temp);
            loop.add(rthss);

            // RawTrackerHitFitterDriver rthfd = new
            // RawTrackerHitFitterDriver();
            // rthfd.setFitAlgorithm("Pileup");
            // rthfd.setUseTimestamps(false);
            // rthfd.setCorrectTimeOffset(true);
            // rthfd.setCorrectT0Shift(false);
            // rthfd.setUseTruthTime(false);
            // rthfd.setSubtractTOF(true);
            // rthfd.setSubtractTriggerTime(true);
            // rthfd.setCorrectChanT0(false);
            // rthfd.setDebug(false);
            // loop.add(rthfd);

            loop.add(new org.hps.recon.tracking.SimpleTrackerDigiDriver());

            // loop.add(new TrackerHitDriver());

            loop.add(new org.hps.recon.tracking.HelicalTrackHitDriver());
            loop.add(new org.hps.recon.tracking.TrackerReconDriver());

            // loop.add(new org.hps.recon.tracking.gbl.GBLOutputDriver());

            /*
             * <driver name="TrackDataDriver" /> <driver
             * name="ReconParticleDriver" />
             * 
             * <driver name="ReconParticleDriver"
             * type="org.hps.recon.particle.HpsReconParticleDriver">
             * <ecalClusterCollectionName
             * >EcalClustersCorr</ecalClusterCollectionName>
             * <trackCollectionNames>MatchedTracks
             * GBLTracks</trackCollectionNames> <isMC>true</isMC> </driver>
             */

            loop.add(new LCIODriver(trackFile));

            condDriver.initialize();

            loop.loop(nEvents, null);
            loop.dispose();
        } else {
            trackFile = new File("target/test-output/" + testFileName);
            trackFile.getParentFile().mkdirs();
        }

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

        MergeTrackCollections newMerge = new MergeTrackCollections();
        newMerge.setInputTrackCollectionName(inputTracksName);
        newMerge.setOutputCollectionName(newGoodName);
        // newMerge.setPartialTrackCollectionName(newPartialName);
        newMerge.setRemoveCollections(false);
        loop2.add(newMerge);

        // evaluate results
        loop2.add(new MergePlotsDriver());
        loop2.add(new LCIODriver(outputFile));
        loop2.loop(nEvents, null);
        loop2.dispose();

    }

    /*
     * static nested class that does plotting
     */
    static class MergePlotsDriver extends Driver {

        private AIDA aida = AIDA.defaultInstance();
        private IAnalysisFactory af = aida.analysisFactory();
        private final IHistogram1D numTracksNewGood = aida.histogram1D(
                "numTracksNewGood", 10, 0, 10);
        private final IHistogram1D numHitsNewGood = aida.histogram1D(
                "numHitsNewGood", 10, 0, 10);

        // private final IHistogram1D numHitsOrigGood = aida.histogram1D(
        // "numHitsOrigGood", 10, 0, 10);
        // private final IHistogram1D numHitsOrigPartial = aida.histogram1D(
        // "numHitsOrigPartial", 10, 0, 10);

        // private final IHistogram1D numHitsNewPartial = aida.histogram1D(
        // "numHitsNewPartial", 10, 0, 10);
        // private final IHistogram1D numTracksOrigGood = aida.histogram1D(
        // "numTracksOrigGood", 10, 0, 10);
        // private final IHistogram1D numTracksOrigPartial = aida.histogram1D(
        // "numTracksOrigPartial", 10, 0, 10);
        // private final IHistogram1D numTracksNewPartial = aida.histogram1D(
        // "numTracksNewPartial", 10, 0, 10);

        @Override
        protected void process(EventHeader event) {
            super.process(event);
            List<Track> newGoodTracks = event.get(Track.class, newGoodName);
            // List<Track> originalGoodTracks = event.get(Track.class,
            // originalGoodName);
            // List<Track> originalPartialTracks = event.get(Track.class,
            // originalPartialName);
            // List<Track> newPartialTracks = event.get(Track.class,
            // newPartialName);
            // System.out.printf("%s: found %d original good tracks\n",this.getName(),originalGoodTracks.size());

            /*
             * for (Track track : originalGoodTracks) { List<TrackerHit>
             * hitsOnTrack = track.getTrackerHits();
             * numHitsOrigGood.fill(hitsOnTrack.size()); }
             * numTracksOrigPartial.fill(originalPartialTracks.size()); for
             * (Track track : originalPartialTracks) { List<TrackerHit>
             * hitsOnTrack = track.getTrackerHits();
             * numHitsOrigPartial.fill(hitsOnTrack.size()); }
             * 
             * 
             * numTracksNewPartial.fill(newPartialTracks.size()); for (Track
             * track : newPartialTracks) { List<TrackerHit> hitsOnTrack =
             * track.getTrackerHits();
             * numHitsNewPartial.fill(hitsOnTrack.size()); }
             */

            numTracksNewGood.fill(newGoodTracks.size());
            for (Track track : newGoodTracks) {
                List<TrackerHit> hitsOnTrack = track.getTrackerHits();
                numHitsNewGood.fill(hitsOnTrack.size());
            }

        }

        protected void endOfData() {
            super.endOfData();

            File outputFile2 = new TestOutputFile(testFileName.replaceAll(
                    ".slcio", "") + "_MergeTest.aida");
            outputFile2.getParentFile().mkdirs();
            try {
                aida.saveAs(outputFile2);
            } catch (IOException ex) {
                Logger.getLogger(MergeTrackCollectionsTest.class.getName()).log(
                        Level.SEVERE, null, ex);
            }
        }

    }

}
