package org.hps.test.it;

import java.io.File;
import java.net.URL;

import org.hps.analysis.MC.MCTrackerHitResidualAnalysisDriver;
import org.hps.test.util.TestOutputFile;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

import junit.framework.TestCase;

/**
 *
 * @author Norman A Graf
 */
public class MCTrackerHitResidualAnalysisDriverTest extends TestCase {

    private String fileName = "singleFullEnergyElectrons_SLIC-v05-00-00_Geant4-v10-01-02_QGSP_BERT_HPS-EngRun2015-Nominal-v2-fieldmap_minInteractions_1kEvents_recon_1Track_6Hits.slcio";
    // private String fileName = "mu-_1.056GeV_slic-3.1.5_geant4-v9r6p1_QGSP_BERT_HPS-EngRun2015-Nominal-v1_fieldOff_++_reco.slcio";
    private int nEvents = 1000;
    
    public void testIt() throws Exception {
        
        FileCache cache = new FileCache();
        File inputFile = cache.getCachedFile(new URL("http://www.lcsim.org/test/hps-java/"+fileName));
        
        TestOutputFile outputFile = new TestOutputFile(MCTrackerHitResidualAnalysisDriverTest.class,
                "MCTrackerHitResidualAnalysisDriverTest.aida");
        
        LCSimLoop loop = new LCSimLoop();
        
        MCTrackerHitResidualAnalysisDriver analDriver = new MCTrackerHitResidualAnalysisDriver();
        analDriver.setAidaFileName(outputFile.getPath());
        loop.add(analDriver);

        loop.setLCIORecordSource(inputFile);
        loop.loop(nEvents);

        //System.out.println("Loop processed " + loop.getTotalSupplied() + " events.");
        //System.out.println("Done!");
    }
}
