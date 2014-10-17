package org.hps;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import junit.framework.TestCase;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.job.JobControlManager;
import org.lcsim.util.Driver;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil.TestOutputFile;

public class SimpleMCReconTest extends TestCase {

    static final int nEvents = 100;
    
	public void testSimpleMCReconTest() throws Exception {
		
        new TestOutputFile(this.getClass().getSimpleName()).mkdir();
		
        FileCache cache = new FileCache();
        File inputFile = cache.getCachedFile(new URL("http://www.lcsim.org/test/hps-java/SimpleMCReconTest.slcio"));
		
        // Run the reconstruction.
		JobControlManager job = new JobControlManager();
        File outputFile = new TestOutputFile(this.getClass().getSimpleName() + File.separator + this.getClass().getSimpleName() + "_recon");
        job.addVariableDefinition("outputFile", outputFile.getPath());
        job.addInputFile(inputFile);
        job.setup("/org/hps/steering/recon/SimpleMCRecon.lcsim");
        job.setNumberOfEvents(nEvents);
        job.run();
        
        // Read in the LCIO event file and print out summary information.
        System.out.println("Running ReconCheckDriver on output ...");
        LCSimLoop loop = new LCSimLoop();
        loop.add(new ReconCheckDriver());
        try {
            loop.setLCIORecordSource(new File(outputFile.getPath() + ".slcio"));
            loop.loop(-1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }                
        System.out.println("Loop processed " + loop.getTotalSupplied() + " events.");        
        System.out.println("Done!");
        
	}
	
	static class ReconCheckDriver extends Driver {
        
        int nTracks;
        int nClusters;        
        int nTrackerHits;
        int nSimCalorimeterHits;
        int nCalorimeterHits;
        int nReconstructedParticles;
        int nEvents;
        
        public void process(EventHeader event) {
            ++nEvents;
            if (event.hasCollection(Track.class, "MatchedTracks")) {
                nTracks += event.get(Track.class, "MatchedTracks").size();
            }
            if (event.hasCollection(Cluster.class, "EcalClusters")) {
                nClusters += event.get(Cluster.class, "EcalClusters").size();
            }
            if (event.hasCollection(TrackerHit.class, "RotatedHelicalTrackHits")) {
                nTrackerHits += event.get(TrackerHit.class, "RotatedHelicalTrackHits").size();
            }
            if (event.hasCollection(SimCalorimeterHit.class, "EcalHits")) {
                nSimCalorimeterHits += event.get(SimCalorimeterHit.class, "EcalHits").size();
            }
            if (event.hasCollection(CalorimeterHit.class, "EcalCalHits")) {
                nCalorimeterHits += event.get(CalorimeterHit.class, "EcalCalHits").size();
            }
            if (event.hasCollection(ReconstructedParticle.class, "FinalStateParticles")) {
                nReconstructedParticles += event.get(ReconstructedParticle.class, "FinalStateParticles").size();
            }
        }        
        
        public void endOfData() {
            System.out.println("ReconCheckDriver results ...");
            System.out.println("  nEvents: " + nEvents);
            System.out.println("  nSimCalorimeterHits: " + nSimCalorimeterHits);
            System.out.println("  nClusters: " + nClusters);
            System.out.println("  nTrackerHits: " + nTrackerHits);            
            System.out.println("  nTracks: " + nTracks);                                    
            System.out.println("  nReconstructedParticles: " + nReconstructedParticles);
            System.out.println("  < nTracks / nEvents > = " + (double)nTracks / (double)nEvents);
            System.out.println("  < nClusters / nEvents > = " + (double)nClusters / (double)nEvents);
        }
    }              
}