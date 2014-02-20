package org.hps.users.holly;

import java.io.File;
import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;
import main.java.org.hps.users.holly.EcalClusterIC;

import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.job.EventMarkerDriver;
import org.lcsim.util.Driver;
import org.lcsim.util.loop.LCSimLoop;

public class ECalClustererTest extends TestCase {

    static String hitCollectionName = "EcalHits";
    static String clusterCollectionName = "EcalClusters";
    static String fileName = "/Users/air/Documents/testcase.slcio";
      
    public void testECalClusterer() throws IOException {
       
        
        EcalClusterIC clusterer = new EcalClusterIC();
        clusterer.setEcalCollectionName(hitCollectionName);
        clusterer.setClusterCollectionName(clusterCollectionName);
        
        LCSimLoop readLoop = new LCSimLoop();
        readLoop.add(new EventMarkerDriver());
        readLoop.add(clusterer);
        readLoop.add(new PrintClustersDriver());
        readLoop.setLCIORecordSource(new File(fileName));
        readLoop.loop(100);
    
    }
    
    static class PrintClustersDriver extends Driver {
        public void process(EventHeader event){
            List<Cluster> clusters = event.get(Cluster.class, clusterCollectionName);            
          //  System.out.println("Number of clusters: "+clusters.size()); 

        }
    }
    
}
