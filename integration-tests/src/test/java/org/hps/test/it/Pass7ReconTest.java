package org.hps.test.it;

import java.io.File;
import java.net.URL;
import junit.framework.TestCase;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil;
import org.hps.evio.EvioToLcio;
import org.hps.recon.NewPassSetupDriver;
import org.hps.recon.particle.HpsReconParticleDriver;
import org.hps.recon.tracking.TrackDataDriver;
import org.hps.recon.tracking.gbl.GBLRefitterDriver;
import org.lcsim.job.EventMarkerDriver;
import org.lcsim.util.loop.LCIODriver;

/**
 * Testing the concept of the 2015 Pass7 reconstruction Proposal is to run the
 * reconstruction starting from the existing lcio output from the previous pass.
 *
 * The ECal clusters are OK, keep them. The found tracks should be OK, keep
 * them. We will simply refit the GBL tracks using the latest geometry. All
 * ReconstructedParticle collections will be dropped and recreated.
 *
 * @author Norman A. Graf
 */
public class Pass7ReconTest extends TestCase {

    static final String testURLBase = "http://www.lcsim.org/test/hps-java/";
    static final String testEvioFileName = "run5772_integrationTest.evio";
    private final int nEvents = 100;

    String[] droppem = {
        "FinalStateParticles",
        "UnconstrainedV0Candidates",
        "UnconstrainedV0Vertices",
        "UnconstrainedMollerCandidates",
        "UnconstrainedMollerVertices",
        "TargetConstrainedV0Candidates",
        "TargetConstrainedV0Vertices",
        "TargetConstrainedMollerCandidates",
        "TargetConstrainedMollerVertices",
        "BeamspotConstrainedV0Candidates",
        "BeamspotConstrainedV0Vertices",
        "BeamspotConstrainedMollerCandidates",
        "BeamspotConstrainedMollerVertices"};

    public void testit() throws Exception {
        EngRun2015Recon();
        Pass7();
    }

    // start by reconstructing some events from scratch...
    public void EngRun2015Recon() throws Exception {
        URL testURL = new URL(testURLBase + "/" + testEvioFileName);
        FileCache cache = new FileCache();
        File inputFile = cache.getCachedFile(testURL);

        File outputDir = new TestUtil.TestOutputFile(this.getClass().getSimpleName());
        outputDir.mkdir();
        File outputFile = new TestUtil.TestOutputFile("Pass6Output");
        String args[] = {"-r", "-x", "/org/hps/steering/recon/EngineeringRun2015FullRecon.lcsim", "-d",
            "HPS-EngRun2015-Nominal-v5-fieldmap", "-D", "outputFile=" + outputFile.getPath(), "-n", Integer.toString(nEvents),
            inputFile.getPath()};
        System.out.println("Running EngRun2015ReconTest.main ...");
        System.out.println("writing to: " + outputFile.getPath());
        System.out.println("Processing "+nEvents+" events");
        long startTime = System.currentTimeMillis();
        EvioToLcio.main(args);
        long endTime = System.currentTimeMillis();
        System.out.println("That took " + (endTime - startTime) + " milliseconds");
    }

    public void Pass7() throws Exception {
        File outputDir = new TestUtil.TestOutputFile(this.getClass().getSimpleName());
        outputDir.mkdir();
        File outputFile = new TestUtil.TestOutputFile("Pass7Test.slcio");

        File lcioInputFile = new TestUtil.TestOutputFile("Pass6Output.slcio");
        LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(lcioInputFile);
        EventMarkerDriver emd = new EventMarkerDriver();
        emd.setEventInterval(1);
        loop.add(emd);
        NewPassSetupDriver npsd = new NewPassSetupDriver();
        npsd.setCollectionsToDrop(droppem);
        loop.add(npsd);
        loop.add(new GBLRefitterDriver());
        loop.add(new TrackDataDriver());
        HpsReconParticleDriver rpd = new HpsReconParticleDriver();
        rpd.setEcalClusterCollectionName("EcalClustersCorr");
        String[] trackCollections = {"GBLTracks"};
        rpd.setDisablePID(true);
        rpd.setTrackCollectionNames(trackCollections);
        loop.add(rpd);
        LCIODriver writer = new LCIODriver(outputFile);
        loop.add(writer);
        System.out.println("Processing "+nEvents+" events");
        long startTime = System.currentTimeMillis();
        loop.loop(nEvents);
        long endTime = System.currentTimeMillis();
        System.out.println("That took " + (endTime - startTime) + " milliseconds");

        loop.dispose();
    }

    //TODO develop analysis Driver to compare the two output lcio files.
}
