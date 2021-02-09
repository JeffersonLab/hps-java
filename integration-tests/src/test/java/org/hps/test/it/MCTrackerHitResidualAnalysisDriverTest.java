package org.hps.test.it;

import java.io.File;

import org.hps.analysis.MC.MCTrackerHitResidualAnalysisDriver;
import org.hps.util.test.TestOutputFile;
import org.hps.util.test.TestUtil;
import org.lcsim.util.loop.LCSimLoop;

import junit.framework.TestCase;

/**
 *
 * @author Norman A Graf
 */
public class MCTrackerHitResidualAnalysisDriverTest extends TestCase {

    private String fileName = "singleFullEnergyElectrons_SLIC-v05-00-00_Geant4-v10-01-02_QGSP_BERT_HPS-EngRun2015-Nominal-v2-fieldmap_minInteractions_1kEvents_recon_1Track_6Hits.slcio";
    private int nEvents = 1000;

    public void testIt() throws Exception {
        File inputFile = TestUtil.downloadTestFile(fileName);

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
