package org.hps;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import junit.framework.TestCase;

import org.hps.job.JobManager;
import org.hps.users.jeremym.MockDataChallengeDiagnosticDriver;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.job.AidaSaveDriver;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

/**
 * This test runs the standard reconstruction on a small set of input events using
 * the new clustering algorithm.
 * 
 * @author Holly Szumila <hvanc001@odu.edu>
 */
public class ReconClusterICTest extends TestCase {

    static final String fileLocation = "http://www.lcsim.org/test/hps-java/MockDataReconTest.slcio";
    
    static final String className = MockDataReconTest.class.getSimpleName();
    static final File outputDir = new File("./target/test-output/" + className);
    static final File outputFile = new File(outputDir.getAbsolutePath() + File.separator + className);
    static final File reconFile = new File(outputFile.getAbsolutePath() + ".slcio");
    static final File aidaFile = new File(outputFile.getAbsolutePath() + ".aida");    

    static final String steeringResource = "/org/hps/steering/users/holly/MockReconClusterICTest.lcsim";    

    static final String clusterCollectionName = "EcalClusters";
    static final String reconstructedParticleCollectionName = "FinalStateParticles";
    static final String trackCollectionName = "MatchedTracks";

    AIDA aida = AIDA.defaultInstance();

    public void setUp() {
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
    
    public void testReconMockData() {

        // Run the reconstruction over input events.
        runRecon();

        // Create the plots.
        createPlots();
    }

    private void runRecon() {
        
        System.out.println("caching file ...");
        System.out.println(fileLocation);
        
        File mockDataFile = null;
        try {
            FileCache cache = new FileCache();
            mockDataFile = cache.getCachedFile(new URL(fileLocation));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("running recon using steering resource " + steeringResource);
        JobManager jobManager = new JobManager();
        jobManager.addVariableDefinition("outputFile", outputFile.getPath());
        jobManager.addInputFile(mockDataFile);
        jobManager.setup(steeringResource);
        jobManager.setNumberOfEvents(100);
        jobManager.run();
    }

    private void createPlots() {
        LCSimLoop loop = new LCSimLoop();
        loop.add(new MockDataChallengeDiagnosticDriver());
        loop.add(new ReconSummaryDriver());
        AidaSaveDriver aidaSaveDriver = new AidaSaveDriver();
        aidaSaveDriver.setOutputFileName(aidaFile.getAbsolutePath());
        loop.add(aidaSaveDriver);        
        try {
            loop.setLCIORecordSource(reconFile);
            loop.loop(-1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
       
    static class ReconSummaryDriver extends Driver {
        
        int ntracks;
        int nparticles;
        int nclusters;
        int nevents;
        
        public void process(EventHeader event) {
            ++nevents;
            ntracks += event.get(Track.class, trackCollectionName).size(); 
            nparticles += event.get(ReconstructedParticle.class, reconstructedParticleCollectionName).size();
            nclusters += event.get(Cluster.class, clusterCollectionName).size();
        }
        
        public void endOfData() {
            System.out.println("CheckDriver got the following ...");
            System.out.println("  nevents = " + nevents);
            System.out.println("  ntracks = " + ntracks);
            System.out.println("  nparticles = " + nparticles);
            System.out.println("  nclusters = " + nclusters);
            System.out.println("  <ntracks / nevents> = " + ((double)ntracks / (double)nevents));
            System.out.println("  <nparticles / nevents> = " + ((double)nparticles / (double)nevents));
            System.out.println("  <nclusters / nevents> = " + ((double)nclusters / (double)nevents));
        }
        
    }
}
