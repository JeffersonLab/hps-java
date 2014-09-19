package org.hps;

import java.io.File;

import junit.framework.TestCase;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.job.JobControlManager;
import org.lcsim.util.Driver;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * Run the reconstruction on output from the readout simulation.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class MCReconTest extends TestCase {
    
    File reconOutputFile = new TestOutputFile("recon");
    File inputFile = new File("/nfs/slac/g/hps3/data/testcase/MCReconTestInput.slcio");
    
    static final int TOTAL_CLUSTERS = 3960;        
    static final int TOTAL_TRACKER_HITS = 28129;
    static final int TOTAL_CALORIMETER_HITS = 61924;
       
    static final long TOTAL_RECON_EVENTS = 945;
    static final int TOTAL_TRACKS = 1990;
    static final int TOTAL_TRACKS_DELTA = 13;
    static final int TOTAL_TRACKS_LOWER = TOTAL_TRACKS - TOTAL_TRACKS_DELTA;
    static final int TOTAL_TRACKS_UPPER = TOTAL_TRACKS + TOTAL_TRACKS_DELTA;
    
    static final int TOTAL_RECONSTRUCTED_PARTICLES = 4341; 
    static final int TOTAL_RECONSTRUCTED_PARTICLES_DELTA = 9;
    static final int TOTAL_RECONSTRUCTED_PARTICLES_LOWER = TOTAL_RECONSTRUCTED_PARTICLES - TOTAL_RECONSTRUCTED_PARTICLES_DELTA;
    static final int TOTAL_RECONSTRUCTED_PARTICLES_UPPER = TOTAL_RECONSTRUCTED_PARTICLES + TOTAL_RECONSTRUCTED_PARTICLES_DELTA;
        
    public void testMCRecon() throws Exception {
        
        System.out.println("Running MC recon on " + inputFile.getPath() + " ...");
        JobControlManager job = new JobControlManager();
        job.addVariableDefinition("outputFile", reconOutputFile.getPath());
        job.addInputFile(inputFile);
        job.setup("/org/hps/steering/recon/HPS2014OfflineTruthRecon.lcsim");
        ReconCheckDriver reconCheckDriver = new ReconCheckDriver();
        job.getLCSimLoop().add(reconCheckDriver);
        long startMillis = System.currentTimeMillis();
        job.run();
        long elapsedMillis = System.currentTimeMillis() - startMillis;
        long nevents = job.getLCSimLoop().getTotalSupplied();
        System.out.println("MC recon processed " + job.getLCSimLoop().getTotalSupplied() + " events.");
        System.out.print("MC recon took " + ((double)elapsedMillis/1000L) + " seconds");
        System.out.println(" which is " + ((double)elapsedMillis / (double)nevents) + " ms per event.");
        job.getLCSimLoop().dispose();
                
        TestCase.assertEquals("Number of recon events processed was wrong.", TOTAL_RECON_EVENTS, nevents);     
                                
        assertEquals("Wrong number of tracker hits.", TOTAL_TRACKER_HITS, reconCheckDriver.nTrackerHits);
        assertEquals("Wrong number of calorimeter hits.", TOTAL_CALORIMETER_HITS, reconCheckDriver.nCalorimeterHits);
        assertEquals("Wrong number of clusters.", TOTAL_CLUSTERS, reconCheckDriver.nClusters);
        TestCase.assertTrue("Number of tracks not within acceptable range.", 
                (reconCheckDriver.nTracks >= TOTAL_TRACKS_LOWER && reconCheckDriver.nTracks <= TOTAL_TRACKS_UPPER));
        assertTrue("Number of reconstructed particles not within acceptable range.", 
                (reconCheckDriver.nReconstructedParticles >= TOTAL_RECONSTRUCTED_PARTICLES_LOWER 
                && reconCheckDriver.nReconstructedParticles <= TOTAL_RECONSTRUCTED_PARTICLES_UPPER));
    }          
    
    static class ReconCheckDriver extends Driver {
        
        int nTracks;
        int nClusters;        
        int nTrackerHits;
        int nCalorimeterHits;
        int nReconstructedParticles;
        int nEvents;
        
        public void process(EventHeader event) {
            //System.out.println("ReconCheckDriver - event #" + event.getEventNumber());
            ++nEvents;
            if (event.hasCollection(Track.class, "MatchedTracks")) {
                nTracks += event.get(Track.class, "MatchedTracks").size();
                //System.out.println("  MatchedTracks: " + event.get(Track.class, "MatchedTracks").size());
            }
            if (event.hasCollection(Cluster.class, "EcalClusters")) {
                nClusters += event.get(Cluster.class, "EcalClusters").size();
                //System.out.println("  EcalClusters: " + event.get(Cluster.class, "EcalClusters").size());
            }
            if (event.hasCollection(TrackerHit.class, "RotatedHelicalTrackHits")) {
                nTrackerHits += event.get(TrackerHit.class, "RotatedHelicalTrackHits").size();
            }
            if (event.hasCollection(CalorimeterHit.class, "EcalCalHits")) {
                nCalorimeterHits += event.get(CalorimeterHit.class, "EcalCalHits").size();
            }
            if (event.hasCollection(ReconstructedParticle.class, "FinalStateParticles")) {
                nReconstructedParticles += event.get(ReconstructedParticle.class, "FinalStateParticles").size();
                //System.out.println("  FinalStateParticles: " + event.get(ReconstructedParticle.class, "FinalStateParticles").size());
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
