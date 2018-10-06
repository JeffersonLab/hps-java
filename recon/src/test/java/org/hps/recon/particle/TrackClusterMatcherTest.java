package org.hps.recon.particle;


//import hep.physics.vec.BasicHep3Vector;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.detector.svt.SvtDetectorSetup;
import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackUtils;
import org.hps.record.StandardCuts;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.recon.tracking.digitization.sisim.config.RawTrackerHitSensorSetup;
import org.lcsim.recon.tracking.digitization.sisim.config.ReadoutCleanupDriver;
import org.lcsim.util.Driver;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCIODriver;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil.TestOutputFile;

import junit.framework.TestCase;

public class TrackClusterMatcherTest extends TestCase {
    static final String testInput = "ap_recon_0000-new.slcio";
    static final String testURLBase = null;

    private final int nEvents = 10000;

    public void testMatcher() throws Exception {
        
        File inputFile = null;
        if (testURLBase == null) {
            inputFile = new File(testInput);
        } else {
            URL testURL = new URL(testURLBase + "/" + testInput);
            FileCache cache = new FileCache();
            inputFile = cache.getCachedFile(testURL);
        }

        String testOutputFileName = "MatcherTest_" + testInput;
        File outputFile = new TestOutputFile(testOutputFileName);
        outputFile.getParentFile().mkdirs();

        LCSimLoop loop2 = new LCSimLoop();
        loop2.setLCIORecordSource(inputFile);
        
        final DatabaseConditionsManager manager = new DatabaseConditionsManager();
        manager.addConditionsListener(new SvtDetectorSetup());

        RawTrackerHitSensorSetup rthss = new RawTrackerHitSensorSetup();
        String[] readoutColl = { "SVTRawTrackerHits" };
        rthss.setReadoutCollections(readoutColl);
        loop2.add(rthss);
        
        loop2.add(new ExtrapDriver());

        HpsReconParticleDriver hpsrpd = new HpsReconParticleDriver();
        hpsrpd.setEcalClusterCollectionName("EcalClustersCorr");
        hpsrpd.setTrackCollectionName("GBLTracks");
        hpsrpd.setBeamPositionX(0);
        hpsrpd.setBeamPositionY(0);
        hpsrpd.setBeamPositionZ(0.5);
        hpsrpd.setBeamSigmaX(0.125);
        hpsrpd.setBeamSigmaY(0.03);
        //hpsrpd.setTrackClusterMatchPlots(true);
        hpsrpd.setDisablePID(true);
        loop2.add(hpsrpd);
        
        ReadoutCleanupDriver rcd = new ReadoutCleanupDriver();
        loop2.add(rcd);

        loop2.add(new LCIODriver(outputFile));

        loop2.loop(nEvents, null);
        loop2.dispose();

    }
    
    protected class ExtrapDriver extends Driver {
        FieldMap bFieldMap = null;
        private String TRK_COLLECTION_NAME = "GBLTracks";
        
        @Override
        protected void detectorChanged(Detector detector) {
            bFieldMap = detector.getFieldMap();
        }
        @Override
        protected void process(EventHeader event) {
            List<Track> trackCollection = event.get(Track.class, TRK_COLLECTION_NAME);
            StandardCuts cuts = new StandardCuts();
            
            List<String> toRemove = new ArrayList<String>();
            toRemove.add("BeamspotConstrainedMollerCandidates");
            toRemove.add("BeamspotConstrainedMollerVertices");
            toRemove.add("BeamspotConstrainedV0Candidates");
            toRemove.add("BeamspotConstrainedV0Vertices");
            toRemove.add("TargetConstrainedMollerCandidates");
            toRemove.add("TargetConstrainedMollerVertices");
            toRemove.add("TargetConstrainedV0Candidates");
            toRemove.add("TargetConstrainedV0Vertices");
            toRemove.add("UnconstrainedMollerCandidates");
            toRemove.add("UnconstrainedMollerVertices");
            toRemove.add("UnconstrainedV0Candidates");
            toRemove.add("UnconstrainedV0Vertices");
            for (String s : toRemove) {
                event.remove(s);
            }
            
            List<Track> tracksToRemove = new ArrayList<Track>();
            
            for (Track track : trackCollection) {
                
                if (TrackType.isGBL(track.getType())) {
                    if (track.getChi2() > cuts.getMaxTrackChisq(track.getTrackerHits().size()))
                        tracksToRemove.add(track);

                    else {
                        TrackState oldStateEcal = TrackUtils.getTrackStateAtECal(track);
                        if (oldStateEcal != null) {
                            //System.out.printf("old state %s \n", new BasicHep3Vector(oldStateEcal.getReferencePoint()).toString());
                            track.getTrackStates().remove(oldStateEcal);
                        }

                        TrackState newStateEcal = TrackUtils.getTrackExtrapAtEcalRK(track, bFieldMap);

                        if (newStateEcal != null) {
                            //System.out.printf("    new state %s \n", new BasicHep3Vector(newStateEcal.getReferencePoint()).toString());
                            track.getTrackStates().add(newStateEcal);
                        }
                    }
                }
            }
            trackCollection.removeAll(tracksToRemove);
        }
    }

}
