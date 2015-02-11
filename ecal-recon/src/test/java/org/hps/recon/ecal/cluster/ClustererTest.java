package org.hps.recon.ecal.cluster;

import hep.aida.ICloud1D;
import hep.aida.ICloud2D;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.base.BaseCluster;
import org.lcsim.job.EventMarkerDriver;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCIODriver;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * This test does basic sanity checks on the output from the {@link Clusterer} algorithms,
 * and it creates an AIDA file with some useful plots, as well as optionally writes
 * an LCIO file with the event data plus the clusters.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ClustererTest extends TestCase {
    
    static int nEvents = 50;
    static final String fileLocation = "http://www.lcsim.org/test/hps-java/MockDataReconTest.slcio";
    File inputFile;
    File testOutputDir;
    
    static class ClustererTestSetup {
        
        boolean writeLcioFile;
        boolean checkSeedHit;
        boolean applyCorrections;        
        boolean checkHitEnergy;
        boolean checkPropCalc;
        boolean checkPosition;
        boolean calculateProperties;
        boolean sortHits;
        double[] cuts = null;
        ClusterType clusterType; 
        
        ClustererTestSetup(double[] cuts) {
            this.cuts = cuts;            
        }
        
        ClustererTestSetup() {            
        }
        
        ClustererTestSetup writeLcioFile() {
            writeLcioFile = true;
            return this;
        }
        
        ClustererTestSetup checkSeedHit() {
            checkSeedHit = true;
            return this;
        }
                
        ClustererTestSetup sortHits() {
            sortHits = true;
            return this;
        }
        
        ClustererTestSetup checkHitEnergy() {
            checkHitEnergy = true;
            return this;
        }
        
        ClustererTestSetup checkPropCalc() {
            checkPropCalc = true;
            return this;
        }
        
        ClustererTestSetup checkClusterType(ClusterType clusterType) {
            this.clusterType = clusterType;
            return this;
        }
        
        ClustererTestSetup checkPosition() {
            this.checkPosition = true;
            return this;
        }
    }
         
    public void setUp() {
        // Cache the input file.
        try {
            inputFile = new FileCache().getCachedFile(new URL(fileLocation));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        // Create test output directory.
        testOutputDir = new TestOutputFile(getClass().getSimpleName());
        testOutputDir.mkdir();        
        
        // Initialize the conditions system.
        new DatabaseConditionsManager();
        DatabaseConditionsManager.getInstance().setLogLevel(Level.WARNING);
    }
    
    /**
     * Test the recon clustering algorithm, formerly called the IC clusterer.
     */
    public void testReconClusterer() {        
        runClustererTest("ReconClusterer", 
                new ClustererTestSetup()
                    .writeLcioFile()
                    .checkSeedHit()
                    .checkClusterType(ClusterType.RECON)
                    .checkHitEnergy()
                    .checkPosition());
    }
    
    /**
     * Test a simple version of the recon clustering.
     */
    public void testSimpleReconClusterer() {
        runClustererTest("SimpleReconClusterer", 
                new ClustererTestSetup()
                    .writeLcioFile()
                    .checkSeedHit()
                    .checkClusterType(ClusterType.SIMPLE_RECON)
                    .checkHitEnergy()
                    .checkPosition());
    }
    
    /**
     * Test a simplistic NN clustering algorithm.
     */
    public void testNearestNeighborClusterer() {    
        runClustererTest("NearestNeighborClusterer", 
                new ClustererTestSetup(new double[] { 0.0075, 3 })
                    .writeLcioFile()
                    .checkClusterType(ClusterType.NN)
                    .checkHitEnergy()
                    .checkPosition());
    }
    
    /**
     * Test the clustering algorithm from the Test Run proposal document.
     */
    public void testLegacyClusterer() {
        runClustererTest("LegacyClusterer", 
                new ClustererTestSetup()
                    .writeLcioFile()
                    .checkClusterType(ClusterType.LEGACY)
                    .checkHitEnergy()
                    .checkPosition());
    }
    
    /**
     * Test the online version of the GTP algorithm.
     */
    public void testGTPOnlineClusterer() {
        runClustererTest("GTPOnlineClusterer", 
                new ClustererTestSetup()
                    .writeLcioFile().checkSeedHit()
                    .checkClusterType(ClusterType.GTP_ONLINE)
                    .checkHitEnergy()
                    .checkPosition());
    }
    
    /**
     * Test the CTP clustering algorithm.
     */
    public void testCTPClusterer() {
        runClustererTest("CTPClusterer", 
                new ClustererTestSetup()
                    .writeLcioFile()
                    .checkClusterType(ClusterType.CTP)
                    .checkPosition());
    }
    
    /**
     * Test the GTP clustering algorithm.
     */
    public void testGTPClusterer() {
        runClustererTest("GTPClusterer", 
                new ClustererTestSetup()
                    .writeLcioFile()
                    .checkClusterType(ClusterType.GTP)
                    .checkHitEnergy()
                    .checkPosition());
    }
    
    /**
     * Run the standard test for a Clusterer.
     * @param clustererName The name of the Clusterer.
     * @param cuts The cut values.
     * @param writeLcioFile Whether or not to write an LCIO output file.
     */
    private void runClustererTest(String clustererName, ClustererTestSetup setup) {
        
        System.out.println("testing Clusterer " + clustererName + " ...");
                
        // Configure the loop.
        LCSimLoop loop = new LCSimLoop();       
        try {
            // Set the input LCIO file.
            loop.setLCIORecordSource(inputFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }        
        
        // Setup event number print outs.
        EventMarkerDriver eventMarkerDriver = new EventMarkerDriver();
        eventMarkerDriver.setEventInterval(1);
        loop.add(eventMarkerDriver);
        
        // Configure the ClusterDriver and add it to the loop.
        String clusterCollectionName = clustererName + "Clusters";
        ClusterDriver clusterDriver = new ClusterDriver();
        clusterDriver.setClustererName(clustererName);
        if (setup.cuts != null) {
            clusterDriver.setCuts(setup.cuts);
        }
        clusterDriver.getLogger().setLevel(Level.ALL);
        clusterDriver.setInputHitCollectionName("EcalHits");       
        clusterDriver.setOutputClusterCollectionName(clusterCollectionName);
        clusterDriver.setRaiseErrorNoHitCollection(true);
        clusterDriver.setSortHits(setup.sortHits);              
        loop.add(clusterDriver);                         
        
        // This Driver generates plots and the output LCIO file.
        loop.add(new ClusterCheckDriver(clusterCollectionName, setup));
        
        if (setup.writeLcioFile) {
            loop.add(new LCIODriver(testOutputDir.getPath() + File.separator + clustererName + ".slcio"));
        }
        
        // Run the job.
        long startMillis = System.currentTimeMillis();
        try {
            loop.loop(nEvents);
            long elapsedSeconds = (System.currentTimeMillis() - startMillis) / 1000;
            System.out.println(clustererName + " took " + elapsedSeconds + "s for " + 
                    loop.getTotalSupplied() + " events which is " + (double)loop.getTotalSupplied()/(double)elapsedSeconds +
                    " events/s");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        loop.dispose();
    }
           
    /**
     * This Driver will check some of the basic Cluster values (currently just size and energy).
     * It also produces some QA plots for each Clusterer in its own output AIDA dir within the plot tree.
     */
    static class ClusterCheckDriver extends Driver {                
        
        AIDA aida = AIDA.defaultInstance();
        IHistogram1D energyH1D;
        IHistogram1D uncorrectedEnergyH1D;
        IHistogram1D countH1D;
        IHistogram1D sizeH1D;    
        IHistogram1D highestHitEnergyH1D;
        IHistogram1D hitEnergyH1D;
        IHistogram2D rawVsCorrectedH2D;
        IHistogram1D positionXH1D;
        IHistogram1D positionYH1D;
        IHistogram1D positionZH1D;
        IHistogram1D shapeParam1H1D;
        IHistogram1D shapeParam2H1D;
        IHistogram1D shapeParam3H1D;
        ICloud1D ithetaC1D;
        ICloud1D iphiC1D;
        ICloud1D nParticlesC1D;
        ICloud1D particleEnergyC1D;
        IHistogram2D particleVsClusterEnergyH2D;
        IHistogram1D particleMinusClusterEnergyH1D;
        ICloud2D clusterPositionC2D;
        ICloud2D highestParticleEnergyVsClusterEnergyC2D;
        ICloud2D clusterVsHitCountC2d;
        IHistogram1D earliestHitTimeH1D;
        IHistogram1D latestHitTimeH1D;
        String clusterCollectionName;
        String clustererName;
        
        ClustererTestSetup setup;
        
        ClusterCheckDriver(String clusterCollectionName, ClustererTestSetup setup) {
            this.clusterCollectionName = clusterCollectionName;
            this.setup = setup;
        }        
        
        public void startOfData() {
            energyH1D = aida.histogram1D(clusterCollectionName + "/Cluster Energy", 300, 0.0, 3.0);
            uncorrectedEnergyH1D = aida.histogram1D(clusterCollectionName + "/Uncorrected Cluster Energy", 200, 0.0, 2.0);
            countH1D = aida.histogram1D(clusterCollectionName + "/Cluster Count", 20, -0.5, 19.5);
            sizeH1D = aida.histogram1D(clusterCollectionName + "/Cluster Size", 30, 0.5, 30.5);
            highestHitEnergyH1D = aida.histogram1D(clusterCollectionName + "/Highest Hit Energy", 300, 0.0, 1.5);
            hitEnergyH1D = aida.histogram1D(clusterCollectionName + "/Hit Energy", 600, 0.0, 1.5);
            rawVsCorrectedH2D = aida.histogram2D(clusterCollectionName + "/Raw vs Corrected Energy", 100, 0.0, 2.0, 100, 0.0, 2.0);
            positionXH1D = aida.histogram1D(clusterCollectionName + "/Position X", 500, 0., 500.0);
            positionYH1D = aida.histogram1D(clusterCollectionName + "/Position Y", 500, 0., 100.0);
            positionZH1D = aida.histogram1D(clusterCollectionName + "/Position Z", 200, 1400., 1500.0);
            shapeParam1H1D = aida.histogram1D(clusterCollectionName + "/Shape Param 1", 500, -5, 95);
            shapeParam2H1D = aida.histogram1D(clusterCollectionName + "/Shape Param 2", 520, -10, 250);
            shapeParam3H1D = aida.histogram1D(clusterCollectionName + "/Shape Param 3", 520, -10, 250);
            particleVsClusterEnergyH2D = aida.histogram2D(clusterCollectionName + "/MCParticle vs Cluster E", 200, 0, 2.0, 200, 0., 2.0);
            particleMinusClusterEnergyH1D = aida.histogram1D(clusterCollectionName + "/MCParticle Minus Cluster E", 200, -2.0, 2.0);
            ithetaC1D = aida.cloud1D(clusterCollectionName + "/ITheta");
            iphiC1D = aida.cloud1D(clusterCollectionName + "/IPhi");
            clusterPositionC2D = aida.cloud2D(clusterCollectionName + "/Position XY", Integer.MAX_VALUE);
            nParticlesC1D = aida.cloud1D(clusterCollectionName + "/MCParticle Count");
            particleEnergyC1D = aida.cloud1D(clusterCollectionName + "/MCParticle Total Energy");
            highestParticleEnergyVsClusterEnergyC2D = aida.cloud2D(clusterCollectionName + "/Highest Particle E vs Cluster E");
            earliestHitTimeH1D = aida.histogram1D(clusterCollectionName + "/Earliest Hit Time", 500, 0., 500.);
            latestHitTimeH1D = aida.histogram1D(clusterCollectionName + "/Latest Hit Time", 500, 0., 500.);
            clusterVsHitCountC2d = aida.cloud2D(clusterCollectionName + "/Cluster Vs Hit Count");
        }
        
        public void process(EventHeader event) {
            List<Cluster> clusters = event.get(Cluster.class, this.clusterCollectionName);
            for (Cluster cluster : clusters) {
                
                // Test assertions.
                checkCluster(cluster);                
                
                // Fill plots.
                fillClusterPlots(cluster);
            }            
            countH1D.fill(clusters.size());
            clusterVsHitCountC2d.fill(clusters.size(), ClusterUtilities.getHits(clusters).size());
        }

        /**
         * @param cluster
         */
        private void fillClusterPlots(Cluster cluster) {
            energyH1D.fill(cluster.getEnergy());
            double rawEnergy = ClusterUtilities.computeRawEnergy(cluster);
            uncorrectedEnergyH1D.fill(rawEnergy);
            sizeH1D.fill(cluster.getCalorimeterHits().size());
            rawVsCorrectedH2D.fill(rawEnergy, cluster.getEnergy());
            for (CalorimeterHit hit : cluster.getCalorimeterHits()) {
                hitEnergyH1D.fill(hit.getCorrectedEnergy());
            }                
            highestHitEnergyH1D.fill(ClusterUtilities.findHighestEnergyHit(cluster).getCorrectedEnergy());
            positionXH1D.fill(Math.abs(cluster.getPosition()[0]));
            positionYH1D.fill(Math.abs(cluster.getPosition()[1]));
            positionZH1D.fill(Math.abs(cluster.getPosition()[2]));
            if (cluster.getShape() != null) {
                shapeParam1H1D.fill(cluster.getShape()[0]);
                shapeParam2H1D.fill(cluster.getShape()[1]);
                shapeParam3H1D.fill(cluster.getShape()[2]);
            }
            iphiC1D.fill(Math.toDegrees(cluster.getIPhi()));
            ithetaC1D.fill(Math.toDegrees(cluster.getITheta()));                                
            clusterPositionC2D.fill(cluster.getPosition()[0], cluster.getPosition()[1]);
            
            //Map<MCParticle, List<SimCalorimeterHit>> particleHitMap = ClusterUtilities.createParticleHitMap(cluster);
            
            Set<MCParticle> particles = ClusterUtilities.findMCParticles(cluster);
            double particleEnergy = 0;
            double highestParticleEnergy = Double.MIN_VALUE;
            for (MCParticle particle : particles) {
                particleEnergy += particle.getEnergy();
                if (particle.getEnergy() > highestParticleEnergy) {
                    highestParticleEnergy = particle.getEnergy();
                }
            }
            particleVsClusterEnergyH2D.fill(particleEnergy, cluster.getEnergy());
            particleMinusClusterEnergyH1D.fill(particleEnergy - cluster.getEnergy());
            nParticlesC1D.fill(particles.size());
            particleEnergyC1D.fill(particleEnergy);
            highestParticleEnergyVsClusterEnergyC2D.fill(highestParticleEnergy, cluster.getEnergy());

            List<CalorimeterHit> hitsForTime = new ArrayList<CalorimeterHit>();
            hitsForTime.addAll(cluster.getCalorimeterHits());
            Collections.sort(hitsForTime, new CalorimeterHit.TimeComparator());
            earliestHitTimeH1D.fill(hitsForTime.get(0).getTime());
            latestHitTimeH1D.fill(hitsForTime.get(hitsForTime.size() - 1).getTime());
        }

        /**
         * @param cluster
         */
        private void checkCluster(Cluster cluster) {
            assertTrue("The cluster energy is invalid.", cluster.getEnergy() > 0.);
            assertTrue("The cluster has no hits.", !cluster.getCalorimeterHits().isEmpty());
            if (setup.checkSeedHit) {
                assertEquals("First hit is not seed.", cluster.getCalorimeterHits().get(0), ClusterUtilities.findHighestEnergyHit(cluster));
            }
            if (setup.checkPropCalc) {
                assertTrue("Cluster properties not calculated.", !((BaseCluster)cluster).needsPropertyCalculation());
            }
            if (setup.clusterType != null) {
                assertEquals("Cluster type is not correct.", setup.clusterType, ClusterType.getClusterType(cluster.getType()));
            }
            if (setup.checkHitEnergy) {
                for (CalorimeterHit hit : cluster.getCalorimeterHits()) {
                    assertTrue("Hit energy " + hit.getCorrectedEnergy() + " is <= 0.", hit.getCorrectedEnergy() > 0.);
                }
            }
            if (setup.checkPosition) {
                double[] position = cluster.getPosition();
                assertTrue("Position X is invalid.", Math.abs(position[0]) < 400. && position[0] != 0.);
                assertTrue("Position Y is invalid.", Math.abs(position[1]) > 25. && Math.abs(position[1]) < 90.);
                assertTrue("Position Z is invalid.", position[2] > 1385. && position[2] < 1480.);
            }
        }              
    }
    
    /**
     * This method writes the AIDA file to disk from all the tests.
     */
    public void tearDown() {
        try {
            AIDA.defaultInstance().saveAs(testOutputDir.getPath() + File.separator + this.getClass().getSimpleName() + ".aida");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }  
}
