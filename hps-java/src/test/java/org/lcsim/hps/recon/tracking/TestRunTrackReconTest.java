package org.lcsim.hps.recon.tracking;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.ITree;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.hps.conditions.CalibrationDriver;
import org.lcsim.hps.util.CompareHistograms;
import org.lcsim.recon.tracking.digitization.sisim.config.RawTrackerHitSensorSetup;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCIODriver;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * Test class to check hit positions and hit multiplicity associated with tracks.
 * @author phansson <phansson@slac.stanford.edu>
 * @version $id: $
 */
public class TestRunTrackReconTest extends TestCase {

    static final String testURLBase = "http://www.slac.stanford.edu/~phansson/files/hps_java_test/HPSTestRunv3/hps-java-1.7-SNAPSHOT-050113";
    static final String testFileName = "egs_5.5gev_0.016x0_500mb_recoil_recon_1_hpsTestRunTrackingTest.slcio";
    static final String testURLBaseCmp = "http://www.slac.stanford.edu/~phansson/files/hps_java_test/HPSTestRunv3/hps-java-1.7-SNAPSHOT-050113";
    static final String testFileNameCmp = "egs_5.5gev_0.016x0_500mb_recoil_recon_1_hpsTestRunTrackingTest.aida";
    static final String trackCollection = "MatchedTracks";
    static final boolean saveForReference = false;
    static final boolean cmpHistograms = true;
    private final int nEvents = 5000;

    public void testTrackRecon() throws Exception {

        File lcioInputFile = null;

        URL testURL = new URL(testURLBase + "/" + testFileName);
        FileCache cache = new FileCache();
        lcioInputFile = cache.getCachedFile(testURL);

        //Process and write out the file
        LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(lcioInputFile);
        loop.add(new MainTrackingDriver());
        File outputFile = new TestOutputFile(testFileName.replaceAll(".slcio", "") + "_hpsTestRunTrackingTest.slcio");
        outputFile.getParentFile().mkdirs(); //make sure the parent directory exists
        loop.add(new LCIODriver(outputFile));
        loop.loop(nEvents, null);
        loop.dispose();

        //Read LCIO back and test!
        LCSimLoop readLoop = new LCSimLoop();
        readLoop.add(new TrackReconTestDriver());
        readLoop.setLCIORecordSource(outputFile);
        readLoop.loop(nEvents, null);
        readLoop.dispose();
    }

    /*
     * static nested class that runs the drivers for standard HPS track recon
     */
    static class TrackReconTestDriver extends Driver {

        private AIDA aida = AIDA.defaultInstance();
        private IAnalysisFactory af = aida.analysisFactory();
        private final IHistogram1D hntracks = aida.histogram1D("hntracks", 10, 0, 10);
        private final IHistogram1D hnstereohits = aida.histogram1D("hnstereohits", 20, 0, 20);
        private final IHistogram1D hhitpositionx = aida.histogram1D("hhitpositionx", 50, 0, 800);
        private final IHistogram1D hhitpositiony = aida.histogram1D("hhitpositiony", 50, -100, 100);
        private final IHistogram1D hhitpositionz = aida.histogram1D("hhitpositionz", 50, -100, 100);
        private final List<String> histograms = Arrays.asList("hntracks", "hnstereohits", "hhitpositionx", "hhitpositiony", "hhitpositionz");
        static final double alpha = 0.05; // Type-I error rate           
        private int ntracks = 0;
        private int nevents = 0;
        //Test thresholds
        private final double ftracks_thr = 0.1;

        @Override
        protected void process(EventHeader event) {
            super.process(event);
            List<Track> tracks = event.get(Track.class, trackCollection);
            //System.out.printf("%s: found %d tracks\n",this.getName(),tracks.size());
            hntracks.fill(tracks.size());
            ntracks += tracks.size();
            for (Track track : tracks) {
                List<TrackerHit> hitsOnTrack = track.getTrackerHits();
                hnstereohits.fill(hitsOnTrack.size());
                for (TrackerHit hit : hitsOnTrack) {
                    double pos[] = hit.getPosition();
                    this.hhitpositionx.fill(pos[0]);
                    this.hhitpositiony.fill(pos[1]);
                    this.hhitpositionz.fill(pos[2]);
                }
            }

            ++nevents;
        }

        @Override
        protected void endOfData() {
            super.endOfData();

            if (saveForReference) {
                File outputFile = new TestOutputFile(testFileName.replaceAll(".slcio", "") + "_hpsTestRunTrackingTest.aida");
                try {
                    aida.saveAs(outputFile);
                } catch (IOException ex) {
                    Logger.getLogger(TestRunTrackReconTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            /*
             * Basic tests
             */
            assertTrue("Failed to find any tracks", ntracks > 0);
            double ftracks = ((double) ntracks) / (double) nevents;
            assertTrue("Failed to reconstruct more than " + this.ftracks_thr + " of tracks/event (" + ftracks + ")", ftracks > this.ftracks_thr);
            assertTrue("Failed to find any stereo hits", this.hnstereohits.mean() > 0.);



//            IPlotter plotter = af.createPlotterFactory().create();
//            plotter.createRegions(1, 3, 0);
//            plotter.setTitle("Nr of tracks");
//            plotter.style().statisticsBoxStyle().setVisible(false);
//            plotter.region(0).plot(hntracks);
//            plotter.show();

            if (cmpHistograms) {

                File aidaCmpInputFile = null;

                URL cmpURL;
                try {
                    cmpURL = new URL(testURLBaseCmp + "/" + testFileNameCmp);
                    FileCache cache = null;
                    try {
                        cache = new FileCache();
                    } catch (IOException ex) {
                        Logger.getLogger(TestRunTrackReconTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    try {
                        aidaCmpInputFile = cache.getCachedFile(cmpURL);
                    } catch (IOException ex) {
                        Logger.getLogger(TestRunTrackReconTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    ITree tree_cmp = null;
                    try {
                        tree_cmp = af.createTreeFactory().create(aidaCmpInputFile.getAbsolutePath());
                    } catch (IllegalArgumentException ex) {
                        Logger.getLogger(TestRunTrackReconTest.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(TestRunTrackReconTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if (tree_cmp == null) {
                        throw new RuntimeException("cannot create the ITree for the comparison file located at" + aidaCmpInputFile.getAbsolutePath());
                    }
                    for (String histname : histograms) {

                        IHistogram1D h_ref = (IHistogram1D) tree_cmp.find(histname);
                        IHistogram1D h_test = aida.histogram1D(histname);
                        boolean nullHypoIsRejected = CompareHistograms.instance().getTTest(alpha, h_test.mean(), h_ref.mean(), h_test.rms() * h_test.rms(), h_ref.rms() * h_ref.rms(), h_test.allEntries(), h_ref.allEntries());
                        double p_value = CompareHistograms.instance().getTTestPValue(h_test.mean(), h_ref.mean(), h_test.rms() * h_test.rms(), h_ref.rms() * h_ref.rms(), h_test.allEntries(), h_ref.allEntries());
                        System.out.printf("%s: %s %s T-Test (%.1f%s C.L.) with p-value=%.3f\n", TestRunTrackReconTest.class.getName(), histname, (nullHypoIsRejected ? "FAILED" : "PASSED"), (1 - alpha) * 100, "%", p_value);
                        assertTrue("Failed T-Test (" + (1 - alpha) * 100 + "% C.L. p-value=" + p_value + ") comparing histogram " + histname, !nullHypoIsRejected);
                        double ks_p_value = CompareHistograms.getKolmogorovPValue(h_test, h_ref);
                        boolean ksNullHypoIsRejected = (ks_p_value < alpha);
                        System.out.printf("%s: %s %s Kolmogorov-Smirnov test (%.1f%s C.L.) with p-value=%.3f\n", TestRunTrackReconTest.class.getName(), histname, (ksNullHypoIsRejected ? "FAILED" : "PASSED"), (1 - alpha) * 100, "%", ks_p_value);
                        assertTrue("Failed Kolmogorov-Smirnov test (" + (1 - alpha) * 100 + "% C.L. p-value=" + ks_p_value + ") comparing histogram " + histname, !ksNullHypoIsRejected);

                        //TODO: use a real two-sample Poisson test
//                        boolean entriesInconsistent = CompareHistograms.instance().getTTest(alpha, h_test.entries(), h_ref.entries(), h_test.entries(), h_ref.entries(), h_test.entries(), h_ref.entries()); 
//                        double p_value_entries = CompareHistograms.instance().getTTestPValue(h_test.entries(), h_ref.entries(), h_test.entries(), h_ref.entries(), h_test.entries(), h_ref.entries());                      
//                        System.out.printf("%s: %s entries are %s (N=%d Ref=%d for T-Test w/ %.1f%s C.L.)\n",TestRunTrackReconTest.class.getName(),histname,(entriesInconsistent?"INCONSISTENT":"CONSISTENT"),h_test.entries(),h_ref.entries(),(1-alpha)*100,"%");                      
//                        assertTrue("Failed T-Test ("+ (1-alpha)*100 + "% C.L. p-value=" + p_value_entries + ") on number of entries comparing histogram " + histname,!entriesInconsistent);

                    }


                } catch (MalformedURLException ex) {
                    Logger.getLogger(TestRunTrackReconTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }
    }
    
    private class MainTrackingDriver extends Driver {
        
        public MainTrackingDriver() {

            //Setup the sensors and calibrations
            CalibrationDriver calibDriver = new CalibrationDriver();
            //calibDriver.setRunNumber(1351); //not sure what should be done here!? -> FIX THIS!
            add(calibDriver);
            add(new RawTrackerHitSensorSetup());
            HPSRawTrackerHitFitterDriver hitfitter = new HPSRawTrackerHitFitterDriver();
            hitfitter.setFitAlgorithm("Analytic");
            hitfitter.setCorrectT0Shift(true);
            add(hitfitter);
            add(new DataTrackerHitDriver());
            HelicalTrackHitDriver hth_driver = new HelicalTrackHitDriver();
            hth_driver.setMaxSeperation(20.0);
            hth_driver.setTolerance(1.0);
            add(hth_driver);
            TrackerReconDriver track_recon_driver = new TrackerReconDriver();
            add(track_recon_driver);
        }

    }
}
