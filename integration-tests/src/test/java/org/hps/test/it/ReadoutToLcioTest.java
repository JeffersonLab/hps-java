package org.hps.test.it;

import java.io.File;

import junit.framework.TestCase;

import org.hps.data.test.TestDataUtility;
import org.hps.job.JobManager;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * <p>
 * This test runs the readout simulation on pre-filtered MC events and writes LCIO.
 * <p>
 * The original name of the input MC file was:
 * <p>
 * ap2.2gev100mev_SLIC-v04-00-00_Geant4-v10-00-02_QGSP_BERT_HPS-Proposal2014-v8-2pt2.slcio
 * <p>
 * The <code>FilterMCBunches</code> utility was run using 500 empty events per input event.
 */
public class ReadoutToLcioTest extends TestCase {
    
    static final int nEvents = 10000;
    
    public void testReadoutToLcio() throws Exception {
        
        new TestOutputFile(this.getClass().getSimpleName()).mkdir();        
        File inputFile = new TestDataUtility().getTestData("ReadoutToLcioTest.slcio");
                
        JobManager job = new JobManager();
        job.addVariableDefinition("detector", "HPS-Proposal2014-v8-2pt2");
        job.addVariableDefinition("run", "0");
        job.addInputFile(inputFile);
        File outputFile = new TestOutputFile(this.getClass().getSimpleName() + File.separator + this.getClass().getSimpleName());
        job.addVariableDefinition("outputFile", outputFile.getPath());
        job.setup("/org/hps/steering/readout/HPS2014ReadoutToLcio.lcsim");
        job.setNumberOfEvents(nEvents);
        job.run();
    }

}
