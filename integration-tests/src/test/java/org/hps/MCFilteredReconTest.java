package org.hps;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.hps.users.meeg.FilterMCBunches;
import org.lcsim.job.JobControlManager;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * <p>
 * Insert empty events into an input MC file using <code>FilterMCBunches</code> 
 * and then run the resulting output through the readout simulation and reconstruction.
 * <p>
 * The original name of the MC input file was:
 * <p>
 * ap075mev_egs_tri_2.2gev_0.00125x0_200na_5e5b_30mr_001_SLIC-v04-00-00_Geant4-v10-00-02_QGSP_BERT_HPS-Proposal2014-v8-2pt2.slcio
 * <p>
 * See this wiki page:
 * <p>
 * <a href="https://confluence.slac.stanford.edu/display/hpsg/Running+Readout+Simulation">Running Readout Simulation</a>
 * <p>
 * under "Filter and space out events" for details about inserting empty events.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class MCFilteredReconTest extends TestCase {
    
    final static String fileLocation = "http://www.lcsim.org/test/hps-java/MCFilteredReconTest.slcio"; 
    
    static final Integer EMPTY_EVENTS = 250;
    
    public void testMCFilteredRecon() throws Exception {
        
        // Create file output directory.
        new TestOutputFile(this.getClass().getSimpleName()).mkdirs();
        
        // Cache the MC event file to the local disk. 
        System.out.println("Downloading MC input file ...");
        FileCache cache = new FileCache();
        File inputFile = cache.getCachedFile(new URL(fileLocation));
        
        // Get the number of events in the MC input file.
        long nMC = countEvents(inputFile);
        
        // 1) Filter MC events to insert 250 empty events between Aprime events.
        List<String> args = new ArrayList<String>();
        args.add(inputFile.getPath());
        File filteredOutputFile = new TestOutputFile(this.getClass().getSimpleName() 
                + File.separator + this.getClass().getSimpleName() + "_filtered.slcio");
        args.add(filteredOutputFile.getPath());
        args.add("-e");
        args.add(EMPTY_EVENTS.toString());               
        System.out.println("Running FilterMCBunches.main on " + inputFile.getPath() + " with ");
        FilterMCBunches.main(args.toArray(new String[]{}));
        System.out.print("Created filtered MC file " + filteredOutputFile.getPath());
        
        // Get number of events in filtered output file.
        long nFiltered = countEvents(filteredOutputFile);
        
        // 2) Run readout simulation.
        JobControlManager job = new JobControlManager();        
        File readoutOutputFile = new TestOutputFile(this.getClass().getSimpleName() + File.separator + this.getClass().getSimpleName() + "_readout");
        job.addVariableDefinition("outputFile", readoutOutputFile.getPath());
        job.addInputFile(filteredOutputFile);
        job.setup("/org/hps/steering/readout/HPS2014ReadoutToLcio.lcsim");
        job.run();                
        System.out.println("Created readout file " + readoutOutputFile.getPath());
        
        // Get number of events created by readout simulation.
        long nReadout = countEvents(new File(readoutOutputFile.getPath() + ".slcio"));
        
        // 3) Run readout events through reconstruction.
        File reconOutputFile = new TestOutputFile(this.getClass().getSimpleName() + File.separator + this.getClass().getSimpleName() + "_recon");
        job = new JobControlManager();
        job.addVariableDefinition("outputFile", reconOutputFile.getPath());
        job.addInputFile(new File(readoutOutputFile.getPath() + ".slcio"));
        job.setup("/org/hps/steering/recon/HPS2014OfflineTruthRecon.lcsim");
        System.out.println("Running recon on filtered events ...");
        job.run();        
        long nRecon = job.getLCSimLoop().getTotalSupplied();
        System.out.println("Created recon file " + reconOutputFile.getPath() + ".slcio");
        
        System.out.println("---------------------------------------------------");
        System.out.println("Job summary ...");
        System.out.println("  MC input events: " + nMC);
        System.out.println("  filtered output events: " + nFiltered);
        System.out.println("  readout output events: " + nReadout);
        System.out.println("  recon output events: " + nRecon);
        System.out.println("  nRecon / nMC = " + (double)nRecon / (double)nMC);
        System.out.println("---------------------------------------------------");
        System.out.println();
        System.out.println("Done!");        
    }
    
    public long countEvents(File file) throws IOException {
        LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(file);
        loop.loop(-1, null);
        return loop.getTotalSupplied();
    }

}
