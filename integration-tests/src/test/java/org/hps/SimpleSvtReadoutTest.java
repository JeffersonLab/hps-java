package org.hps;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.job.JobControlManager;
import org.lcsim.util.Driver;
import org.lcsim.util.cache.FileCache;

import junit.framework.TestCase;


/**
 * Integration test to check the SVT readout.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 *
 */
public class SimpleSvtReadoutTest extends TestCase {

	
    static final String fileUrl = "http://www.lcsim.org/test/hps-java/ReadoutToLcioTest.slcio";
    static final File outputDir = new File("./target/test-output/" + SimpleSvtReadoutTest.class.getSimpleName());    
    static final File outputFile = new File(outputDir + File.separator + SimpleSvtReadoutTest.class.getSimpleName());
    
    // Collection Names
    static final String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    
	public void testSimpleSvtReadout() throws Exception { 
		
		this.printDebug("Retrieving test file from " + fileUrl);
	
        FileCache cache = new FileCache();
        File inputFile = cache.getCachedFile(new URL(fileUrl));
	        
        outputDir.mkdirs();
        if(!outputDir.exists()){ 
        	this.printDebug("Failed to create directory " + outputDir.getPath());
        	throw new RuntimeException("Failed to create output directory.");
        }
        
        FinalCheckDriver checker = new FinalCheckDriver();
        
        JobControlManager job = new JobControlManager();
        job.addInputFile(inputFile);
        job.addVariableDefinition("outputFile", outputFile.getPath());
        job.setup("/org/hps/steering/readout/HPS2014TruthReadoutToLcio.lcsim");
        job.setNumberOfEvents(10000);
        job.getLCSimLoop().add(checker);
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
