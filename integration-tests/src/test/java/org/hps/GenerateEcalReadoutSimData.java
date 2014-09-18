package org.hps;

import java.io.File;

import junit.framework.TestCase;

import org.lcsim.job.JobControlManager;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * Run the ECAL readout simulation on current SLIC MC data.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class GenerateEcalReadoutSimData extends TestCase {
    
    // This file is tridents, AP and backgrounds merged together into one event file.
    static String dataPath = "/nfs/slac/g/hps3/data/testcase/ap050mev_egs_tri_6.6gev_0.0025x0_450na_5e5b_30mr_001_SLIC-v04-00-00_Geant4-v10-00-02_QGSP_BERT_HPS-Proposal2014-v8-6pt6.slcio";
    
    public void testGenerateReadoutSimData() throws Exception {
        
        // Generate ecal readout simulation data from an MC input file.
        JobControlManager job = new JobControlManager();
        job.addVariableDefinition("outputFile", new TestOutputFile("readout").getPath());
        job.addInputFile(new File(dataPath));
        job.setup("/org/hps/steering/readout/HPS2014TruthReadoutToLcio.lcsim");
        job.run();
        
        // Read in the readout file and print summary info.
        LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(new TestOutputFile("readout.slcio"));
        loop.loop(-1);
        System.out.println("# Events written: " + loop.getTotalSupplied());
    }      
}
