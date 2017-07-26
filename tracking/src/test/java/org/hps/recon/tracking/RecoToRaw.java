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

public class RecoToRaw extends TestCase {

    protected String testInputFileName = "hps_005772.0_recon_Rv4657-0-10000.slcio";
    protected String testOutputFileName = "raw_" + testInputFileName;
    protected String testURLBase = "http://www.lcsim.org/test/hps-java";
    protected long nEvents = -1;

    public void testClear() throws Exception {
        File inputFile = null;
        if (testURLBase == null) {
            inputFile = new File(testInputFileName);
        } else {
            URL testURL = new URL(testURLBase + "/" + testInputFileName);
            FileCache cache = new FileCache();
            inputFile = cache.getCachedFile(testURL);
        }

        File outputFile = new TestOutputFile(testOutputFileName);
        outputFile.getParentFile().mkdirs();

        LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(inputFile);

        loop.add(new RecoToRawDriver());
        loop.add(new LCIODriver(outputFile));
        try {
            loop.loop(nEvents);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        loop.dispose();
    }

    protected class RecoToRawDriver extends Driver {

        public RecoToRawDriver() {
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
            toRemove.add("TriggerTime");
            toRemove.add("SVTFittedRawTrackerHits");

            for (String s : toRemove) {
                event.remove(s);
            }
        }
    }

}
