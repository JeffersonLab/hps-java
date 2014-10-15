package org.hps;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.hps.evio.TestRunEvioToLcio;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.util.Driver;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * This test performs event reconstruction on Test Run data for a large number of events, 
 * using the run 1351 input EVIO file.  This test will only run successfully if SLAC
 * NFS is available on the local machine.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class TestRunReconTest extends TestCase {

    static String inputFileName = "/nfs/slac/g/hps3/data/testrun/runs/evio/hps_001351.evio.0";
    
    public void testTestRunRecon() {       
        
        // Setup the test output directory.
        new TestOutputFile(getClass().getSimpleName()).mkdirs();
        
        // Run TestRunEvioToLcio using the standard org.lcsim steering file.
        File inputFile = new File(inputFileName);
        List<String> argList = new ArrayList<String>();
        argList.add("-r");
        argList.add("-x");
        argList.add("/org/hps/steering/recon/TestRunOfflineRecon.lcsim");
        argList.add("-d");
        argList.add("HPS-TestRun-v8-5");
        argList.add("-D");
        argList.add("runNumber=1351");
        argList.add("-D");
        File outputFile = new TestOutputFile(getClass().getSimpleName() + File.separator + getClass().getSimpleName() + "_recon");
        argList.add("outputFile=" + outputFile.getPath());
        argList.add(inputFile.getPath());
        System.out.println("Running TestRunEvioToLcio.main ...");
        TestRunEvioToLcio.main(argList.toArray(new String[] {}));
        
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
        System.out.println("Loop processed " + loop.getTotalSupplied() + " events");        
        System.out.println("Done!");
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
            System.out.println("  nTracks / nEvents = " + (double)nTracks / (double)nEvents);
            System.out.println("  nClusters / nEvents = " + (double)nClusters / (double)nEvents);
        }
    }              
}
