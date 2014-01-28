package org.hps.recon.ecal;

import java.io.File;
import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.hps.recon.ecal.EcalClusterer;
import org.lcsim.hps.recon.ecal.EcalClustererCosmics;
import org.lcsim.job.EventMarkerDriver;
import org.lcsim.util.Driver;
import org.lcsim.util.loop.LCSimLoop;

public class ECalClustererTest extends TestCase {

    static String hitCollectionName = "EcalHits";
    static String clusterCollectionName = "EcalClusters";
    
    public void testECalClusterer() throws IOException {
       
        
        EcalClustererCosmics clusterer = new EcalClustererCosmics();
        clusterer.setEcalCollectionName(hitCollectionName);
        clusterer.setClusterCollectionName(clusterCollectionName);
        
        LCSimLoop readLoop = new LCSimLoop();
        readLoop.add(new EventMarkerDriver());
        readLoop.add(clusterer);
        readLoop.add(new PrintClustersDriver());
        readLoop.setLCIORecordSource(new File("/Users/tknelson/Documents/Work/SLAC/HPS/Software/egs_tri_2.2gev_0.00125x0_200na_5e5b_30mr_001_SLIC-v2r11p1_geant4-v9r3p2_QGSP_BERT_HPS-Proposal2014-v3-2pt2.slcio"));

        readLoop.loop(100);
    
    }
    
    static class PrintClustersDriver extends Driver {
        public void process(EventHeader event){
            List<Cluster> clusters = event.get(Cluster.class, clusterCollectionName);            
            System.out.println("Number of clusters: "+clusters.size());            
        }
    }
    
}
