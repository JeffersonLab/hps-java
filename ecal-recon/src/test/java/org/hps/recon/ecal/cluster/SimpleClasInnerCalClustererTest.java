package org.hps.recon.ecal.cluster;

import java.net.URL;
import java.util.logging.Level;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

/**
 * This test runs the {@link SimpleClasInnerCalClusterer} on some mock data.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
// FIXME: This test needs some assertions!
public class SimpleClasInnerCalClustererTest extends TestCase {
    
    static final String fileLocation = "http://www.lcsim.org/test/hps-java/MockDataReconTest.slcio";

    public void testLegacyClusterer() throws Exception {
        
        DatabaseConditionsManager.getInstance();
        
        LCSimLoop loop = new LCSimLoop();       
        loop.setLCIORecordSource(new FileCache().getCachedFile(new URL(fileLocation)));        
        ClusterDriver clusterDriver = new ClusterDriver();
        clusterDriver.getLogger().setLevel(Level.ALL);
        clusterDriver.setClusterer("SimpleClasInnerCalClusterer");
        clusterDriver.setInputHitCollectionName("EcalHits");
        clusterDriver.setOutputClusterCollectionName(getClass().getSimpleName() + "Clusters");
        clusterDriver.setRaiseErrorNoHitCollection(true);
        loop.add(clusterDriver);        
        loop.loop(100);
    }
}
