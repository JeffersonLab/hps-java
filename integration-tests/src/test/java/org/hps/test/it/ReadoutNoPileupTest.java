package org.hps.test.it;

import java.io.File;

import junit.framework.TestCase;

import org.hps.data.test.TestDataUtility;
import org.hps.job.JobManager;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * <p>
 * This test runs the readout simulation on MC events without simulated pile-up and writes LCIO.
 * <p>
 * According to the Confluence documentation for this task:
 * <p>
 * "Each event is treated independently, all detectors are reset between events".
 * <p>
 * <a href="https://confluence.slac.stanford.edu/display/hpsg/Running+Readout+Simulation">Running Readout Simulation</a>
 * <p>
 * The original name of the input MC file was:
 * <p>
 * ap2.2gev100mev_SLIC-v04-00-00_Geant4-v10-00-02_QGSP_BERT_HPS-Proposal2014-v8-2pt2.slcio
 * 
 */
public class ReadoutNoPileupTest extends TestCase {
    
    static final int nEvents = 100;
    
    public void testReadoutNoPileup() throws Exception {
        new TestOutputFile(this.getClass().getSimpleName()).mkdir();        
        File inputFile = new TestDataUtility().getTestData("ReadoutNoPileupTest.slcio");
        JobManager job = new JobManager();
        job.addInputFile(inputFile);
        job.addVariableDefinition("detector", "HPS-Proposal2014-v8-2pt2");
        job.addVariableDefinition("run", "0");
        File outputFile = new TestOutputFile(this.getClass().getSimpleName() + File.separator + this.getClass().getSimpleName() + "_readout");
        job.addVariableDefinition("outputFile", outputFile.getPath());
        job.setup("/org/hps/steering/readout/HPS2014ReadoutNoPileup.lcsim");
        job.setNumberOfEvents(nEvents);
        job.run();
    }
}
