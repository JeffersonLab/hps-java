package org.hps.users.holly;

import java.io.File;
import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.job.EventMarkerDriver;
import org.lcsim.util.Driver;
import org.lcsim.util.loop.LCSimLoop;

public class ECalClusterICTest extends TestCase {
	
    static String hitCollectionName = "EcalHits";//MC data
    //static String hitCollectionName = "EcalCalHits";//Test Run data 
    static String clusterCollectionName = "EcalClusters";
//    static String fileName = "/Users/air/Documents/Ecal_Input/electron_100k_0p5GeV.slcio";
    //static String fileName = "/Users/air/Desktop/cosmic_22aout/CosmicSetupA/A_1p25/A_1p25_0.slcio"; 
  //  static String fileName = "/Users/air/Desktop/mock24_6Octv3.slcio";
    static String fileName = "/Users/air/Documents/Ecal_Input/testcase.slcio";
    
    public void testECalClusterer() throws IOException {
      
    	EcalClusterIC clusterer = new EcalClusterIC();//changed
        //EventDisplayOutputDriver clusterer = new EventDisplayOutputDriver();
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
