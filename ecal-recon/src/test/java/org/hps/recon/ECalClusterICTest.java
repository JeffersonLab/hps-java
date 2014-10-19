package org.hps.recon;

import java.io.File;
import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.hps.recon.ecal.EcalClusterIC;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.job.EventMarkerDriver;
import org.lcsim.util.Driver;
import org.lcsim.util.loop.LCSimLoop;

/**
 * This test runs the new clusterer (EcalClusterIC) using the input file below. 
 * 
 * @author Holly Szumila
 *
 */
public class ECalClusterICTest extends TestCase {
	
    static String hitCollectionName = "EcalHits";
    static String clusterCollectionName = "EcalClusters";
    static String fileName = "/Users/air/Documents/Ecal_Input/testcase.slcio";
    
    public void testECalClusterer() throws IOException {
      
    	EcalClusterIC clusterer = new EcalClusterIC();
    	clusterer.setEcalCollectionName(hitCollectionName);
        clusterer.setClusterCollectionName(clusterCollectionName);
        LCSimLoop readLoop = new LCSimLoop();
        readLoop.add(new EventMarkerDriver());
        readLoop.add(clusterer);
        readLoop.add(new PrintClustersDriver());
        readLoop.setLCIORecordSource(new File(fileName));
        readLoop.loop(100000);
    
    }
    
    
    static class PrintClustersDriver extends Driver {
       
        int nclusters;
        int nevents;
        int icluster;
    	
    	public void process(EventHeader event){
            ++nevents;

            nclusters += event.get(Cluster.class, clusterCollectionName).size();
            icluster = event.get(Cluster.class, clusterCollectionName).size();
            
            if (icluster>0){
            	System.out.println(" cluster per event = "+ icluster);   
            }
//           List<Cluster> clusters = event.get(Cluster.class, clusterCollectionName); 
        }
    	
    	public void endOfData() {
            System.out.println("PrintClustersDriver got the following ...");
            System.out.println("  nevents = " + nevents);
            System.out.println("  nclusters = " + nclusters);
            System.out.println("  <nclusters / nevents> = " + ((double)nclusters / (double)nevents));
        }
    	
    	
    }
    
}
