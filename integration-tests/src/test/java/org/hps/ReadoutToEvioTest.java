package org.hps;

import java.io.File;
import java.net.URL;
import java.util.logging.Level;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.job.JobManager;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * <p>
 * This test runs the readout simulation on pre-filtered MC events and writes EVIO.
 * <p>
 * The original name of the input MC file was:
 * <p>
 * ap2.2gev100mev_SLIC-v04-00-00_Geant4-v10-00-02_QGSP_BERT_HPS-Proposal2014-v8-2pt2.slcio
 * <p>
 * The <code>FilterMCBunches</code> utility was run using 500 empty events per input event.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ReadoutToEvioTest extends TestCase {
    
    static final int nEvents = 10000;
    
    public void testReadoutToEvio() throws Exception {
        new TestOutputFile(this.getClass().getSimpleName()).mkdir();
        
        FileCache cache = new FileCache();
        File inputFile = cache.getCachedFile(new URL("http://www.lcsim.org/test/hps-java/ReadoutToEvioTest.slcio"));
        
        JobManager job = new JobManager();
        DatabaseConditionsManager.getInstance().setLogLevel(Level.WARNING);
        job.addInputFile(inputFile);
        File outputFile = new TestOutputFile(this.getClass().getSimpleName() + File.separator + this.getClass().getSimpleName() + "_readout");
        job.addVariableDefinition("outputFile", outputFile.getPath());
        job.setup("/org/hps/steering/readout/HPS2014ReadoutToEvio.lcsim");
        job.setNumberOfEvents(nEvents);
        job.run();
    }
}
