package org.hps;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.hps.users.meeg.FilterMCBunches;
import org.lcsim.job.JobControlManager;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * <p>
 * Insert empty bunches into an input MC file using <code>FilterMCBunches</code> 
 * and then run the resulting output through the reconstruction.
 * <p>
 * See this wiki page:
 * <p>
 * <a href="https://confluence.slac.stanford.edu/display/hpsg/Running+Readout+Simulation">Running Readout Simulation</a>
 * <p>
 * under "Filter and space out events" for details.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class MCFilteredReconTest extends TestCase {
    
    final static String fileLocation = "ftp://ftp-hps.slac.stanford.edu/hps/hps_data/hps_java_test_case_data/MCFilteredReconTest.slcio"; 
    
    public void testMCFilteredRecon() throws Exception {
        
        System.out.println("Downloading input file ...");
        FileCache cache = new FileCache();
        File inputFile = cache.getCachedFile(new URL(fileLocation));
        
        List<String> args = new ArrayList<String>();
        args.add(inputFile.getPath());
        File filteredFile = new TestOutputFile(this.getClass().getSimpleName() + "_filtered.slcio");
        args.add(filteredFile.getPath());
        args.add("-e");
        args.add("250");               
        System.out.println("Running FilterMCBunches.main ...");
        FilterMCBunches.main(args.toArray(new String[]{}));
        
        File reconOutputFile = new TestOutputFile(this.getClass().getSimpleName() + "_recon");
        JobControlManager job = new JobControlManager();
        job.addVariableDefinition("outputFile", reconOutputFile.getPath());
        job.addInputFile(filteredFile);
        job.setup("/org/hps/steering/recon/HPS2014OfflineTruthRecon.lcsim");
        System.out.println("Running recon on filtered events ...");
        job.run();
        System.out.println("Processed " + job.getLCSimLoop().getTotalSupplied() + " events in job.");
        
        System.out.println("Done!");
    }

}
