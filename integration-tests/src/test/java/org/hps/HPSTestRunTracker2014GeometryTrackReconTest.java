package org.hps;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import junit.framework.TestCase;

import org.hps.users.jeremym.MockDataChallengeDiagnosticDriver;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.job.AidaSaveDriver;
import org.lcsim.job.JobControlManager;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

/**
 * This test runs the standard reconstruction on a small set of input events.
 * 
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 */
public class HPSTestRunTracker2014GeometryTrackReconTest extends TestCase {

    //static final String fileLocationBottom = "http://www.lcsim.org/test/hps-java/mu-_10GeV_SLIC-v04-00-00_Geant4-v10-00-02_QGSP_BERT_HPSTestRunTracker2014-v0-50-bottom-tracks.slcio";
    //static final String fileLocationTop = "http://www.lcsim.org/test/hps-java/mu-_10GeV_SLIC-v04-00-00_Geant4-v10-00-02_QGSP_BERT_HPSTestRunTracker2014-v0-50-top-tracks.slcio";
    //static final String fileLocationBottom = "http://www.lcsim.org/test/hps-java/HPSTestRunTracker2014GeometryTrackerRecon-bottom.slcio";
    //static final String fileLocationTop = "http://www.lcsim.org/test/hps-java/HPSTestRunTracker2014GeometryTrackerRecon-bottom.slcio";
    static final String fileLocationBottom = "http://www.slac.stanford.edu/~phansson/files/temp/mu-_10GeV_SLIC-v04-00-00_Geant4-v10-00-02_QGSP_BERT_HPSTestRunTracker2014-v0-50-bottom-tracks.slcio";
    static final String fileLocationTop = "http://www.slac.stanford.edu/~phansson/files/temp/mu-_10GeV_SLIC-v04-00-00_Geant4-v10-00-02_QGSP_BERT_HPSTestRunTracker2014-v0-50-top-tracks.slcio";
    //static final String fileLocation = "/Users/phansson/work/HPS/software/run/geomDev/mu-_10GeV_SLIC-v04-00-00_Geant4-v10-00-02_QGSP_BERT_HPSTestRunTracker2014-v0.slcio";
    //static final String fileLocation = "http://www.lcsim.org/test/hps-java/MockDataReconTest.slcio";
    
    static final String className = HPSTestRunTracker2014GeometryTrackReconTest.class.getSimpleName();
    static final File outputDir = new File("./target/test-output/" + className);
    static final File outputFile = new File(outputDir.getAbsolutePath() + File.separator + className);
    static final File reconFile = new File(outputFile.getAbsolutePath() + ".slcio");
    static final File aidaFile = new File(outputFile.getAbsolutePath() + ".aida");    

    static final String steeringResource = "/org/hps/steering/test/SVTTrackingRecon.lcsim";
    //static final String steeringResource = "/org/hps/steering/readout/HPSTrackingDefaults.lcsim";
    
    static final int expectedTracks = 50;
   
    static final String reconstructedParticleCollectionName = "MCParticle";
    static final String trackCollectionName = "MatchedTracks";

    AIDA aida = AIDA.defaultInstance();

    public void setUp() {
        clear();
    }
    
    private void clear() {
        // Delete files if they already exist.
        if (reconFile.exists())
            reconFile.delete();
        if (aidaFile.exists())
            aidaFile.delete();
        
        // Create output dir.
        outputDir.mkdirs();
        if (!outputDir.exists()) {
            throw new RuntimeException("Failed to create test output dir.");
        }
    }

    public void testTrackRecon() {
        runTrackReconBottom();
        checkOutput();
        //checkPlots();
        runTrackReconTop();
        checkOutput();
        //checkPlots();
        
    }
    
    private void runTrackReconBottom() {
        
        //setup dirs and files
        clear();
        
        // Run the reconstruction over input events.
        runRecon(fileLocationBottom);
    
    }
    
    private void runTrackReconTop() {
        
        //setup dirs and files
        clear();
        
        // Run the reconstruction over input events.
        runRecon(fileLocationTop);
    
    }
    
    

    private void runRecon(String fileLoc) {
        
        System.out.println("caching file ...");
        System.out.println(fileLoc);
        
        File mockDataFile = null;
        //File mockDataFile = new File(fileLocation);
        try {
            FileCache cache = new FileCache();
            mockDataFile = cache.getCachedFile(new URL(fileLoc));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("running recon using steering resource " + steeringResource);
        JobControlManager jobManager = new JobControlManager();
        jobManager.addVariableDefinition("outputFile", outputFile.getPath());
        jobManager.addInputFile(mockDataFile);
        jobManager.setup(steeringResource);
        jobManager.run();
    }

    private void checkOutput() {
        System.out.println("check output file ...");
         LCSimLoop loop = new LCSimLoop();
        loop.add(new CheckDriver());
        try {
            loop.setLCIORecordSource(reconFile);
            loop.loop(-1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    
    static class CheckDriver extends Driver {
        int ntracks;
        int nparticles;
        int nevents;
        
        public void process(EventHeader event) {
            ++nevents;
            ntracks += event.get(Track.class, trackCollectionName).size(); 
            nparticles += event.get(ReconstructedParticle.class, reconstructedParticleCollectionName).size();
        }
        
        public void endOfData() {
            System.out.println("CheckDriver got the following ...");
            System.out.println("  nevents = " + nevents);
            System.out.println("  ntracks = " + ntracks);
            System.out.println("  nparticles = " + nparticles);
            System.out.println("  <ntracks / nevents> = " + ((double)ntracks / (double)nevents));
            System.out.println("  <nparticles / nevents> = " + ((double)nparticles / (double)nevents));

            // check that there is one track per event
            assertTrue((Math.abs((double)ntracks / (double)nevents) - 1) < 0.000001);
            
            System.out.println("CheckDriver compare to following ...");
            
            
        }
        
    }
}
