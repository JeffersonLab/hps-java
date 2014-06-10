/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.hps.users.luca;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import java.io.IOException;


import java.util.List;
import java.util.Queue;
import org.hps.readout.ecal.ClockSingleton;
import org.hps.readout.ecal.TriggerDriver;

import org.hps.recon.ecal.ECalUtils;
import org.hps.recon.ecal.HPSEcalCluster;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.Driver;
import hep.aida.*;
import hep.aida.IHistogram3D;
import java.io.FileWriter;
import org.lcsim.event.CalorimeterHit;

/**
 *
 * @author Luca
 */
public class mycluster extends Driver {
  
   /* private FileWriter writer;
    private FileWriter writer2;
    String outputFileName = "ClusterEnePos1.txt";
    String outputFileName2 = "ClusterEnePos2.txt";
    int cx, cy;*/
    
    int counter=0;
  AIDA aida = AIDA.defaultInstance();
  IHistogram1D eneTuttiPlot = aida.histogram1D("All Hits Energy", 300, 0.0,3.0);
  IHistogram1D eneTuttiPlotCut = aida.histogram1D("All Hits Energy E> 1GeV", 300, 0.0,3.0);
  IHistogram1D eneClusterPlot = aida.histogram1D("All Clusters Energy", 300, 0.0,3.0);
  IHistogram1D eneClusterPlotcut = aida.histogram1D("Clusters Energy E>1.4", 300, 0.0,3.0);
   IHistogram1D eneSeedPlot = aida.histogram1D("All Seed Energy ", 300, 0.0,3.0);
   IHistogram1D eneSeedPlotcut = aida.histogram1D(" Seed Energy (Cluster E>1.4)", 300, 0.0,3.0);
  
   
   
 
  /* public void setOutputFileName(String outputFileName){
this.outputFileName = outputFileName;
}
  @Override   
public void startOfData(){
try{
    //initialize the writers
    writer=new FileWriter(outputFileName);
    writer2=new FileWriter(outputFileName2);
    //Clear the files
    writer.write("");
    writer2.write("");
}
catch(IOException e ){
System.err.println("Error initializing output file for event display.");
}
}
  
@Override
public void endOfData(){
try{
//close the file writer.
    writer.close();
    writer2.close();
    }
catch(IOException e){
    System.err.println("Error closing utput file for event display.");
}
} */
   
 @Override  
 public void process (EventHeader event){
   
     
     
     if(event.hasCollection(CalorimeterHit.class,"EcalCorrectedHits"))
     {
         List<CalorimeterHit> hits = event.get(CalorimeterHit.class,"EcalCorrectedHits");
         for(CalorimeterHit hit : hits){
         eneTuttiPlot.fill(hit.getRawEnergy());
         if(hit.getRawEnergy()>=1){
         eneTuttiPlotCut.fill(hit.getRawEnergy());
         }
         }
         
          
  
     }
     
     if(event.hasCollection(HPSEcalCluster.class, "EcalClusters")) {
            List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, "EcalClusters");
          
            if(clusters.size() > 0) {
               for(HPSEcalCluster cluster : clusters){
                   eneClusterPlot.fill(cluster.getEnergy());
                   eneSeedPlot.fill(cluster.getSeedHit().getRawEnergy());
                   if(cluster.getEnergy()>=1.4){
                   eneClusterPlotcut.fill(cluster.getEnergy());
                   eneSeedPlotcut.fill(cluster.getSeedHit().getRawEnergy());
                   }
                  
                }  
            }
           }
        
 
 }

 
 
 
 
 
 
 
 
 
 
 } //chiusura driver  
    
    
    
