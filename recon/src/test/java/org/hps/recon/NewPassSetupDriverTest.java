/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon;

import java.io.File;
import java.net.URL;
import junit.framework.TestCase;
import org.hps.recon.particle.HpsReconParticleDriver;
import org.hps.recon.tracking.TrackDataDriver;
import org.hps.recon.tracking.gbl.GBLRefitterDriver;
import org.lcsim.job.EventMarkerDriver;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCIODriver;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil;

/**
 *
 * @author ngraf
 */
public class NewPassSetupDriverTest extends TestCase {

    static final String testURLBase = "http://www.lcsim.org/test/hps-java/";
    static final String testFileName = "run5772_pass6_V0CandidateSkim.slcio";
    private final int nEvents = 10;
    
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

    public void testIt() throws Exception {
        File outputDir = new TestUtil.TestOutputFile(this.getClass().getSimpleName());
        outputDir.mkdir();
        File outputFile = new TestUtil.TestOutputFile("Pass7Test.slcio");
        File lcioInputFile = null;
        URL testURL = new URL(testURLBase + "/" + testFileName);
        FileCache cache = new FileCache();
        lcioInputFile = cache.getCachedFile(testURL);
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
        loop.loop(nEvents);
        loop.dispose();
    }

}
