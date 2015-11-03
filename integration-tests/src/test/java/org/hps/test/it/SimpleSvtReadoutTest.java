package org.hps.test.it;

import java.io.File;
import java.util.List;

import junit.framework.TestCase;

import org.hps.data.test.TestDataUtility;
import org.hps.job.JobManager;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.util.Driver;


/**
 * Integration test to check the SVT readout.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 *
 */
public class SimpleSvtReadoutTest extends TestCase {
   
    static final File outputDir = new File("./target/test-output/" + SimpleSvtReadoutTest.class.getSimpleName());    
    static final File outputFile = new File(outputDir + File.separator + SimpleSvtReadoutTest.class.getSimpleName());
    
    // Collection Names
    static final String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    
	public void testSimpleSvtReadout() throws Exception { 
				
		File inputFile = new TestDataUtility().getTestData("ReadoutToLcioTest.slcio");
			        
        outputDir.mkdirs();
        if(!outputDir.exists()){ 
        	this.printDebug("Failed to create directory " + outputDir.getPath());
        	throw new RuntimeException("Failed to create output directory.");
        }
        
        FinalCheckDriver checker = new FinalCheckDriver();
        
        JobManager job = new JobManager();
        job.addInputFile(inputFile);
        job.addVariableDefinition("outputFile", outputFile.getPath());
        job.addVariableDefinition("detector", "HPS-Proposal2014-v8-2pt2");
        job.addVariableDefinition("run", "0");
        job.setup("/org/hps/steering/readout/HPS2014TruthReadoutToLcio.lcsim");        
        job.getLCSimLoop().add(checker);
        job.setNumberOfEvents(10000);
        job.run();
        
        this.printDebug("=========== Summary ===========");
        this.printDebug("");
        this.printDebug("Number of RawTrackerHits: " + checker.getTotalNumberOfRawTrackerHits());
        this.printDebug("");
        this.printDebug("===============================");
        
	}
	
	class FinalCheckDriver extends Driver { 
	
		private int totalRawTrackerHits = 0; 
		
		public void process(EventHeader event){
			if(!event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName)) return;
			List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);
		
			totalRawTrackerHits += rawHits.size();
		}
		
		public int getTotalNumberOfRawTrackerHits(){
			return totalRawTrackerHits;
		}
	}
	
	private void printDebug(String message){
		System.out.println("[ SimpleSvtReadoutTest ]: " + message);
	}
}
