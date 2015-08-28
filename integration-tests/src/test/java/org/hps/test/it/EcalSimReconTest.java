package org.hps.test.it;

import hep.aida.IHistogram1D;

import java.io.File;
import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.hps.job.JobManager;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.test.util.TestFileUrl;
import org.hps.test.util.TestOutputFile;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.loop.LCSimLoop;

/**
 * Run readout simulation and full 2015 Engineering Run reconstruction on ECal MC input data and then check histograms
 * of cluster data.
 *
 * @author Jeremy McCormick, SLAC
 */
public class EcalSimReconTest extends TestCase {

    /**
     * Driver for checking test output.
     *
     * @author Jeremy McCormick, SLAC
     */
    static class EcalSimReconCheckDriver extends Driver {

        /**
         * Setup AIDA histogramming.
         */
        private final static AIDA aida = AIDA.defaultInstance();

        /**
         * Count of clusters per event.
         */
        private final IHistogram1D clusCountH1D = aida.histogram1D("Cluster Count", 20, -0.5, 19.5);

        /**
         * Individual cluster energies.
         */
        private final IHistogram1D clusEnergyH1D = aida.histogram1D("Cluster Energy", 100, -0.5, 9.5);

        /**
         * Highest cluster energy in event.
         */
        private final IHistogram1D clusHighEnergyH1D = aida.histogram1D("Cluster Highest Energy", 100, -0.5, 9.5);

        /**
         * First hit time in highest energy cluster.
         */
        private final IHistogram1D clusTimeH1D = aida.histogram1D("Cluster Time", 500, -0.5, 499.5);
        
        /**
         * Number of clusters found.
         */
        private int clusterCount;

        /**
         * Save histograms and perform checks on statistics.
         */
        @Override
        public void endOfData() {

            // Print some statistics.
            System.out.println("clus count: " + clusterCount);
            System.out.println("mean clus per event: " + clusCountH1D.mean());
            System.out.println("mean clus energy: " + clusEnergyH1D.mean());
            System.out.println("high energy clus mean: " + clusHighEnergyH1D.mean());
            System.out.println("mean clus time: " + clusTimeH1D.mean());

            // Save plots to AIDA file.
            try {
                aida.saveAs(new TestOutputFile(EcalSimReconTest.class, "plots.aida"));
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }

            // Check high cluster energy mean.
            TestCase.assertEquals("High cluster energy does not match.", CLUS_HIGH_MEAN_E, 
                    clusHighEnergyH1D.mean(), 0.0004);

            // Check high cluster time mean.
            TestCase.assertEquals("High cluster mean time does not match.", CLUS_MEAN_T, 
                    clusTimeH1D.mean(), 0.03);
            
            // Check mean number of clusters per event.
            TestCase.assertEquals("Mean number of clusters per event does not match.", CLUS_COUNT_MEAN,
                    clusCountH1D.mean(), 0.002);
            
            // Check total number of clusters.
            TestCase.assertEquals("Number of clusters does not match.", CLUS_COUNT, clusterCount, 1);
        }

        /**
         * Process events and fill histograms from cluster collection.
         */
        @Override
        public void process(final EventHeader event) {
            final List<Cluster> clusters = event.get(Cluster.class, "EcalClustersCorr");
            clusterCount += clusters.size();
            clusCountH1D.fill(clusters.size());
            for (final Cluster cluster : clusters) {
                clusEnergyH1D.fill(cluster.getEnergy());
                if (cluster.getEnergy() == 0) {
                    throw new RuntimeException("Cluster has 0. energy.");
                }                
            }

            final Cluster highClus = ClusterUtilities.findHighestEnergyCluster(clusters);
            clusHighEnergyH1D.fill(highClus.getEnergy());
            clusTimeH1D.fill(highClus.getCalorimeterHits().get(0).getTime());
        }
    }

    /**
     * Expected mean time of primary cluster in nanoseconds.
     */
    private static final double CLUS_MEAN_T = 58.89;

    /**
     * Expected mean of high cluster energy in GeV.
     */
    private static final double CLUS_HIGH_MEAN_E = 1.071;
    
    /**
     * Expected mean number of clusters per event.     
     */
    private static final double CLUS_COUNT_MEAN = 2.95;
    
    /**
     * Expected total number of clusters.
     */
    private static final int CLUS_COUNT = 2549;
  
    /**
     * Steering resource file for running reconstruction.
     */
    private static final String RECON_STEERING = "/org/hps/steering/recon/EngineeringRun2015FullReconMC.lcsim";

    /**
     * Run number for conditions system.
     */
    private static final Integer RUN = 5000;
    
    /**
     * Run the test.
     *
     * @throws Exception if there is an uncaught exception thrown
     */
    public void testEcalSimRecon() throws Exception {

        // Get the input events file.
        final File readoutFile = TestFileUrl.getInputFile(EcalSimReconTest.class, "readout.slcio");
       
        // Run the recon on the readout output.
        final File reconFile = new TestOutputFile(EcalSimReconTest.class, "recon.slcio");
        JobManager job = new JobManager();
        job.addInputFile(readoutFile);
        job.addVariableDefinition("detector", "HPS-EngRun2015-Nominal-v1");
        job.addVariableDefinition("outputFile", reconFile.getPath().replace(".slcio", ""));
        job.addVariableDefinition("run", RUN.toString());
        job.setup(RECON_STEERING);
        job.run();
        System.out.println("recon ran on " + job.getLCSimLoop().getTotalCountableConsumed() + " events");

        // Check the recon output.
        final LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(reconFile);
        final EcalSimReconCheckDriver checkDriver = new EcalSimReconCheckDriver();
        loop.add(checkDriver);
        loop.loop(-1);
        loop.dispose();
        System.out.println("EcalSimReconCheckDriver ran on " + loop.getTotalCountableConsumed() + " events");
    }
}
