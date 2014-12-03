/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.hps.users.luca;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import java.io.IOException;
import java.util.*;
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

import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.Driver;
import hep.aida.*;

import java.io.FileWriter;
import org.lcsim.event.CalorimeterHit;


/**
 * 
 * @author Luca Colaneri 
 */
public class TriggerAna extends Driver {
    int posx, posy;
    int radius=2;
    int Clustercount=0;
    int clusterWindow=50;
    int TotalCluster=0;
    double timeDifference;
    double energyThreshold=0;
    private LinkedList<ArrayList<HPSEcalCluster>> clusterBuffer;
    protected String clusterCollectionName = "EcalClusters";
    
 //AIDA aida = AIDA.defaultInstance();
//IHistogram1D clusterEne=aida.histogram1D("Clusters energy with Kyle's trigger",300, 0, 3);
    private FileWriter writer;
   // private FileWriter writer2;
    private FileWriter writer3;
    private FileWriter writer4;
    String outputFileName = "KyleTriggerFEE.txt";
  //  String outputFileName2 = "KyleTriggerHits.txt";
    String outputFileName3 = "NoTriggerFEE.txt";
   
   
 
   
   public void setRadius (int radius){
         this.radius=radius;
                 }
    
    public void setEnergyThreshold (double threshold){
    this.energyThreshold=threshold;
    }
   
    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }
 
   public void setOutputFileName(String outputFileName){
this.outputFileName = outputFileName;
}
   public void setOutputFileName3(String outputFileName3){
this.outputFileName3 = outputFileName3;
   }
   public void settimeDifference(double time){
   this.timeDifference=time;
   
   }
  /*
   *
   *
   *
   */
   
   @Override   
public void startOfData(){

    //initialize the clusterbuffer
    clusterBuffer= new LinkedList<ArrayList<HPSEcalCluster>>();
 //populate the clusterbuffer with (2*clusterWindow + 1)
 // empty events, representing the fact that the first few events will not have any events in the past portion of the buffer
    int bufferSize=(2*clusterWindow)+1;
    for(int i = 0;i<bufferSize; i++){
    clusterBuffer.add(new ArrayList<HPSEcalCluster>(0));
    }
    
    
    
    
    try{
    //initialize the writers
    writer=new FileWriter(outputFileName);
    //writer2=new FileWriter(outputFileName2);
   // writer3=new FileWriter(outputFileName3);
    
    //Clear the files
    writer.write("");
   // writer2.write("");
   // writer3.write("");
    
    
    
}
catch(IOException e ){
System.err.println("Error initializing output file for event display.");
}
}
  
@Override
public void endOfData(){
//System.out.println("Ho contato" + TotalCluster + " clusters di cui " + Clustercount + "isolati\n");
    
    try{
//close the file writer.
    writer.close();
 //   writer2.close();
    //writer3.close();
    
    }
catch(IOException e){
    System.err.println("Error closing utput file for event display.");
}
} 
   
 @Override  
 public void process (EventHeader event){
   
         /* if(event.hasCollection(HPSEcalCluster.class, "EcalClusters")) {
        List<HPSEcalCluster> clusterList =event.get(HPSEcalCluster.class,clusterCollectionName );   
            for(HPSEcalCluster cluster : clusterList){
                
                if(cluster.getEnergy()>energyThreshold){
                 int idd=getCrystal(cluster); 
                    try{
                        writer3.append(idd + " " + cluster.getEnergy()+ " " + cluster.getSize() + " " + cluster.getSeedHit().getRawEnergy() + " " + cluster.getSeedHit().getIdentifierFieldValue("ix")+" " +cluster.getSeedHit().getIdentifierFieldValue("iy")+"\n");
                        }
                    catch(IOException e ){System.err.println("Error writing to output for event display");} 
                 }
            }
          
          }
         */  
     
     //get the clusters from the event IF they are triggered
    if(TriggerDriver.triggerBit()){
     if(event.hasCollection(HPSEcalCluster.class, "EcalClusters")) {
        List<HPSEcalCluster> clusterList =event.get(HPSEcalCluster.class,clusterCollectionName );    
             
     //put the clusters in the arraylist
     
     
     for(HPSEcalCluster cluster : clusterList){
        // clusterEne.fill(cluster.getEnergy());
         TotalCluster++;
        int id;
            Clustercount++;
           id=getCrystal(cluster);
           try{
     writer.append(id + " " + cluster.getEnergy()+ " " + cluster.getSize() + " " + cluster.getSeedHit().getRawEnergy() + " " + cluster.getSeedHit().getIdentifierFieldValue("ix")+" " +cluster.getSeedHit().getIdentifierFieldValue("iy"));
     /*for(CalorimeterHit hit : cluster.getCalorimeterHits())
     {writer.append(hit.getRawEnergy()+ " ");
       }*/
     writer.append("\n");
    
     }
     
   catch(IOException e ){System.err.println("Error writing to output for event display");}   
     }
   
     }
     
     
     
    }
     
}
 
 
 public int getCrystal (HPSEcalCluster cluster){
 int x,y,id=0;
 x= (-1)*cluster.getSeedHit().getIdentifierFieldValue("ix");
 y= cluster.getSeedHit().getIdentifierFieldValue("iy");
 
 if(y==5){
 if(x<0)
 {id=x+24;}
 else id= x+23;
 }
 
 else if(y==4)
 {if(x<0){
  id=x+70;}
 else id=x+69;}
 
 else if(y==3)
 {if(x<0){
  id=x+116;}
 else id=x+115;}
 
 else if(y==2)
 {if(x<0){
  id=x+162;}
 else id=x+161;}
 
 else if(y==1)
 {x=-x;
     if(x>0){
  id=-x+208;}
 else if(x==-1){id=208;}
 else if(x<-1) id=-x+198;}
 
  else if(y==-1)
 {x=-x;
     if(x>0){
  id=-x+245;}
 else if(x==-1 )id=245;
 else if(x<-1){id=-x+235;}}
 
 
 else if(y==-2)
 {if(x<0){
  id=x+282;}
 else id=x+281;}
 
  else if(y==-3)
 {if(x<0){
  id=x+328;}
 else id=x+327;}
 
 else if(y==-4)
 {if(x<0){
  id=x+374;}
 else id=x+373;}
 
 else if(y==-5)
 {if(x<0){
  id=x+420;}
 else id=x+419;}
 
 return id;
 
 }
 
 public int getCrystal (CalorimeterHit hit){
 int x,y,id=0;
 x= (-1)*hit.getIdentifierFieldValue("ix");
 y= hit.getIdentifierFieldValue("iy");
 
 if(y==5){
 if(x<0)
 {id=x+24;}
 else id= x+23;
 }
 
 else if(y==4)
 {if(x<0){
  id=x+70;}
 else id=x+69;}
 
 else if(y==3)
 {if(x<0){
  id=x+116;}
 else id=x+115;}
 
 else if(y==2)
 {if(x<0){
  id=x+162;}
 else id=x+161;}
 
 else if(y==1)
 {x=-x;
     if(x>0){
  id=-x+208;}
 else if(x==-1){id=208;}
 else if(x<-1) id=-x+198;}
 
  else if(y==-1)
 {x=-x;
     if(x>0){
  id=-x+245;}
 else if(x==-1 )id=245;
 else if(x<-1){id=-x+235;}}
 
 
 else if(y==-2)
 {if(x<0){
  id=x+282;}
 else id=x+281;}
 
  else if(y==-3)
 {if(x<0){
  id=x+328;}
 else id=x+327;}
 
 else if(y==-4)
 {if(x<0){
  id=x+374;}
 else id=x+373;}
 
 else if(y==-5)
 {if(x<0){
  id=x+420;}
 else id=x+419;}
 
 return id;
 
 }
 
 } //chiusura driver  
    
    
    
