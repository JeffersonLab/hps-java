package org.hps.recon.tracking;

import java.io.File;
import java.net.URL;

import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCIODriver;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil.TestOutputFile;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class RecoToRawMC extends TestCase {

    protected String testInputFileName = "target/test-output/ap_prompt_new.slcio";
    protected String testOutputFileName = "ap_prompt_new_raw.slcio";
    protected String testURLBase = null;
    protected long nEvents = 5;
    protected URL testURL;
    protected FileCache cache;
    protected Driver testTrackingDriver = null;

    public void testClearMC() throws Exception {
        File inputFile = null;
        if (testURLBase == null) {
            inputFile = new File(testInputFileName);

            //  inputFile.getParentFile().mkdirs();
        } else {
            URL testURL = new URL(testURLBase + "/" + testInputFileName);
            cache = new FileCache();
            inputFile = cache.getCachedFile(testURL);
        }

        File outputFile = new TestOutputFile(testOutputFileName);
        outputFile.getParentFile().mkdirs();

        LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(inputFile);

        loop.add(new RecoToRawMCDriver());
        loop.add(new LCIODriver(outputFile));
        try {
            //loop.skip(4);
            loop.loop(nEvents);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        loop.dispose();
    }

    protected class RecoToRawMCDriver extends Driver {

        public RecoToRawMCDriver() {
        }

        @Override
        public void process(EventHeader event) {
            super.process(event);
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
            toRemove.add("EcalCalHits");
            toRemove.add("EcalClusters");
            toRemove.add("EcalClustersCorr");
            toRemove.add("FinalStateParticles");
            toRemove.add("GBLKinkData");
            toRemove.add("GBLKinkDataRelations");
            toRemove.add("GBLTracks");
            toRemove.add("HelicalTrackHitRelations");
            toRemove.add("HelicalTrackHits");
            toRemove.add("MatchedToGBLTrackRelations");
            toRemove.add("MatchedTracks");
            toRemove.add("PartialTracks");
            toRemove.add("RotatedHelicalTrackHitRelations");
            toRemove.add("RotatedHelicalTrackHits");
            toRemove.add("TrackData");
            toRemove.add("TrackDataRelations");
            toRemove.add("TrackResiduals");
            toRemove.add("TrackResidualsRelations");
            toRemove.add("RFHits");
            toRemove.add("StripClusterer_SiTrackerHitStrip1D");
            toRemove.add("SVTShapeFitParameters");
            toRemove.add("SVTFittedRawTrackerHits");

            //toRemove.add("TriggerTime");

            toRemove.add("EcalClustersGTP");
            //toRemove.add("EcalHits");
            toRemove.add("HelicalTrackMCRelations");
            //toRemove.add("MCParticle");
            toRemove.add("RotatedHelicalTrackMCRelations");
            //toRemove.add("SVTTrueHitRelations");
            //toRemove.add("TrackerHits");
            //toRemove.add("TrackerHitsECal");

            for (String s : toRemove) {
                event.remove(s);
            }
        }
    }

}
