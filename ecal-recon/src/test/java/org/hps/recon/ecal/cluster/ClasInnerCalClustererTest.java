package org.hps.recon.ecal.cluster;

import java.net.URL;
import java.util.List;
import java.util.logging.Level;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

/**
 * This test runs the {@link SimpleClasInnerCalClusterer} on some mock data
 * and does some basic sanity checks on the output clusters.  It uses the
 * first 50 events from a mock data sample, all of which should have at 
 * least one output cluster.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ClasInnerCalClustererTest extends TestCase {
    
    static final String fileLocation = "http://www.lcsim.org/test/hps-java/MockDataReconTest.slcio";

    static String outputClusterCollectionName = ClasInnerCalClustererTest.class.getSimpleName() + "Clusters";
    static double maxEnergy = 1.35;
    static double minEnergy = 0.3;
    
    public void testLegacyClusterer() throws Exception {
        
        DatabaseConditionsManager.getInstance();
        
        LCSimLoop loop = new LCSimLoop();       
        loop.setLCIORecordSource(new FileCache().getCachedFile(new URL(fileLocation)));        
        ClasInnerCalClusterDriver clusterDriver = new ClasInnerCalClusterDriver();
        clusterDriver.getLogger().setLevel(Level.ALL);
        clusterDriver.setInputHitCollectionName("EcalHits");
        clusterDriver.setOutputClusterCollectionName(outputClusterCollectionName);
        clusterDriver.setRejectedHitCollectionName("RejectedHits");
        clusterDriver.setRaiseErrorNoHitCollection(true);
        loop.add(clusterDriver); 
        loop.add(new ClusterCheckDriver());
        loop.loop(50);
    }
    
    static class ClusterCheckDriver extends Driver {
                
        public void process(EventHeader event) {
            List<Cluster> clusters = event.get(Cluster.class, outputClusterCollectionName);
            assertTrue("There were no clusters created.", !clusters.isEmpty());
            for (Cluster cluster : clusters) {
                assertTrue("The cluster energy is invalid.", cluster.getEnergy() > 0);
                assertTrue("The cluster has no hits.", !cluster.getCalorimeterHits().isEmpty());
                assertTrue("The cluster energy is too low.", cluster.getEnergy() > minEnergy);
                assertTrue("The cluster energy is too high.", cluster.getEnergy() < maxEnergy);
            }
        }             
    }
}
