/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.hps.users.luca;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import java.io.IOException;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
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
  
    private FileWriter writer;
    String outputFileName = "HitEnePos.txt";
    
    
    int counter=0;
  AIDA aida = AIDA.defaultInstance();
  IHistogram1D enePlot = aida.histogram1D("energia cluster", 100, 0.0,3.0);
  IHistogram2D positionPlot = aida.histogram2D("Posizione cluster", 60,-350,350,10,-100,100);
  IHistogram1D dimPlot = aida.histogram1D("dimensione cluster", 12, 0.0,12);
  IHistogram1D dimPlot2 = aida.histogram1D("dimensione cluster con taglio", 12, 0.0,12);
  IHistogram1D eneSeedHitPlot = aida.histogram1D("energia seedHit", 100, 0.0,3.0);
   IHistogram1D eneHitPlot = aida.histogram1D("energia dHit", 100, 0.2,3.0);
  IHistogram3D enePosPlot = aida.histogram3D("energia cluster vs posizione",350,-350,350,100,-100,100, 100, 0.0,3.0);
   double[] position;
 
   public void setOutputFileName(String outputFileName){
this.outputFileName = outputFileName;
}
  @Override   
public void startOfData(){
try{
    //initialize the writer
    writer=new FileWriter(outputFileName);
    //Clear the file
    writer.write("");
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
    }
catch(IOException e){
    System.err.println("Error closing utput file for event display.");
}
} 
   
 @Override  
 public void process (EventHeader event){
   
     
     
     if(event.hasCollection(CalorimeterHit.class,"EcalCorrectedHits"))
     {
         List<CalorimeterHit> hits = event.get(CalorimeterHit.class,"EcalCorrectedHits");
         for(CalorimeterHit hit : hits){
            if(hit.getRawEnergy()>0.6 &&hit.getRawEnergy()<2.3){
             eneHitPlot.fill(hit.getRawEnergy());
            
            }
         }
     
     }
     
     if(event.hasCollection(HPSEcalCluster.class, "EcalClusters")) {
            List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, "EcalClusters");
          
            if(clusters.size() > 0) {
                counter++;
                for(HPSEcalCluster cluster : clusters){
                
                    
                    positionPlot.fill(cluster.getPosition()[0], cluster.getPosition()[1]);
                    enePlot.fill(cluster.getEnergy());
                    dimPlot.fill(cluster.getSize());
                    eneSeedHitPlot.fill(cluster.getSeedHit().getRawEnergy());
                    position=cluster.getPosition();
                  
                    if(cluster.getSeedHit().getRawEnergy()>1)
                    {dimPlot2.fill(cluster.getSize());}
                    /* try{
                    //writer.append(position[0] + " " + position[1] + " " +cluster.getSeedHit().getRawEnergy()+ "\n");
                    }
                    
                    catch(IOException e ){System.err.println("Error writing to output for event display.");}*/


                }
                
               
            }
           }
        
 
 }

 
 
 
 
 
 
 
 
 
 
 } //chiusura driver  
    
    
    
