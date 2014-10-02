package org.hps;

import java.io.File;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.hps.recon.tracking.RawTrackerHitFitterDriver;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.job.JobControlManager;
import org.lcsim.util.Driver;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * Run the reconstruction on output from the readout simulation.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class MCReconTest extends TestCase {
        
    static final String fileLocation = "http://www.lcsim.org/test/hps-java/MCReconTest.slcio";
            
    public void testMCRecon() throws Exception {
        
        new TestOutputFile(this.getClass().getSimpleName()).mkdirs();
        
        File reconOutputFile = new TestOutputFile(this.getClass().getSimpleName() 
                + File.separator + this.getClass().getSimpleName() + "_recon");
        
        FileCache cache = new FileCache();
        File inputFile = cache.getCachedFile(new URL(fileLocation));
        
        System.out.println("Running MC recon on " + inputFile.getPath() + " ...");
        JobControlManager job = new JobControlManager();
        job.addVariableDefinition("outputFile", reconOutputFile.getPath());
        job.addInputFile(inputFile);
        job.setup("/org/hps/steering/test/MCReconTest.lcsim");
        ReconCheckDriver reconCheckDriver = new ReconCheckDriver();
        job.getLCSimLoop().add(reconCheckDriver);
        for (Driver driver : job.getDriverAdapter().getDriver().drivers()) {
            System.out.println(driver.getClass().getCanonicalName());
            if (driver instanceof RawTrackerHitFitterDriver) {
                ((RawTrackerHitFitterDriver)driver).setDebug(false);
            }
        }
        Logger.getLogger("org.freehep.math.minuit").setLevel(Level.OFF);
        long startMillis = System.currentTimeMillis();
        job.run();
        long elapsedMillis = System.currentTimeMillis() - startMillis;
        long nevents = job.getLCSimLoop().getTotalSupplied();
        System.out.println("MC recon processed " + job.getLCSimLoop().getTotalSupplied() + " events.");
        System.out.print("MC recon took " + ((double)elapsedMillis/1000L) + " seconds");
        System.out.println(" which is " + ((double)elapsedMillis / (double)nevents) + " ms per event.");
        job.getLCSimLoop().dispose();
    }          
    
    static class ReconCheckDriver extends Driver {
        
        int nTracks;
        int nClusters;        
        int nTrackerHits;
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
            System.out.println("  nTracks: " + nTracks);
            System.out.println("  nClusters: " + nClusters);
            System.out.println("  nTrackerHits: " + nTrackerHits);
            System.out.println("  nCalorimeterHits: " + nCalorimeterHits);
            System.out.println("  nReconstructedParticles: " + nReconstructedParticles);
        }
    }              
}
