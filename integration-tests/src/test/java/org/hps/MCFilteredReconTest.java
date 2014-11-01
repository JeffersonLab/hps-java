package org.hps;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.hps.readout.ecal.FADCEcalReadoutDriver;
import org.hps.users.meeg.FilterMCBunches;
import org.lcsim.job.JobControlManager;
import org.lcsim.util.Driver;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * <p>
 * Insert empty events into an input MC file using <code>FilterMCBunches</code> 
 * and then run the resulting output through the readout simulation and reconstruction.
 * <p>
 * See this wiki page:
 * <p>
 * <a href="https://confluence.slac.stanford.edu/display/hpsg/Running+Readout+Simulation">Running Readout Simulation</a>
 * <p>
 * under "Filter and space out events" for details about inserting empty events.
 * <p>
 * The test runs the filtering on 10 Aprime events, inserting 250 empty events in between in order to simulate a single readout window.
 * Then the filtered events are run through the readout simulation to run the triggering algorithms.  The acceptance is approximately 20%
 * so the 10 input events ends up as 2 events in the recon.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
// TODO: Remove noise from readout simulation Driver and add test assertions.
public class MCFilteredReconTest extends TestCase {
        
    final static String fileLocation = 
    		"http://www.lcsim.org/test/hps-java/MCFilteredReconTest/ap2.2gev075mev_SLIC-v04-00-00_Geant4-v10-00-02_QGSP_BERT_HPS-Proposal2014-v7-2pt2.slcio";
    
    // 250 bunches which is ~250 ns or time of readout window.  
    static final Integer EMPTY_EVENTS = 250;
    
    public void testMCFilteredRecon() throws Exception {
        
        // Create file output directory.
        new TestOutputFile(this.getClass().getSimpleName()).mkdirs();
        
        // Cache the MC event file to the local disk. 
        System.out.println("Downloading MC input file ...");
        FileCache cache = new FileCache();
        File inputFile = cache.getCachedFile(new URL(fileLocation));
                
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
                
        // 2) Run readout simulation.
        JobControlManager job = new JobControlManager();        
        File readoutOutputFile = new TestOutputFile(this.getClass().getSimpleName() + File.separator + this.getClass().getSimpleName() + "_readout");
        job.addVariableDefinition("outputFile", readoutOutputFile.getPath());
        job.addInputFile(filteredOutputFile);
        job.setup("/org/hps/steering/readout/HPS2014ReadoutToLcio.lcsim");
        for (Driver driver : job.getDriverAdapter().getDriver().drivers()) {
        	if (driver instanceof FADCEcalReadoutDriver) {
        		// Turn off noise in the readout driver.
        		((FADCEcalReadoutDriver)driver).setAddNoise(false);
        	}
        }
        job.run();                
        System.out.println("Created readout file " + readoutOutputFile.getPath());
                
        // 3) Run readout events through reconstruction.
        File reconOutputFile = new TestOutputFile(this.getClass().getSimpleName() + File.separator + this.getClass().getSimpleName() + "_recon");
        job = new JobControlManager();
        job.addVariableDefinition("outputFile", reconOutputFile.getPath());
        job.addInputFile(new File(readoutOutputFile.getPath() + ".slcio"));
        job.setup("/org/hps/steering/recon/HPS2014OfflineTruthRecon.lcsim");
        System.out.println("Running recon on filtered events ...");
        job.run();        
        System.out.println("Created recon file " + reconOutputFile.getPath() + ".slcio");
        
        System.out.println("Created " + job.getLCSimLoop().getTotalSupplied() + " recon output events.");
    }    
}
