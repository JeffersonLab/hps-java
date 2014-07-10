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
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.Driver;
import hep.aida.*;
import hep.aida.IHistogram3D;
import java.io.FileWriter;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.MCParticle;

/**
 * 
 * @author Luca Colaneri 
 */
public class HitSeedHitConf extends Driver {
    int posx, posy;
    
    protected String clusterCollectionName = "EcalClusters";
    
 
 
    private FileWriter writer;
    private FileWriter writer2;
    //private FileWriter writer2;
    String outputFileName = "ClusterSeedHitsTrigger.txt";
     String outputFileName2 = "AllHitsTrigger.txt";
   // String outputFileName2 = "ClusterEnePos2.txt";

   
 
   
   
    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }
 
   public void setOutputFileName(String outputFileName){
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
} 
   
 @Override  
 public void process (EventHeader event){
   
          
           
           
     
     //get the clusters from the event
     if(event.hasCollection(HPSEcalCluster.class, "EcalClusters")) {
        List<HPSEcalCluster> clusterList =event.get(HPSEcalCluster.class,clusterCollectionName );    
           for(HPSEcalCluster cluster : clusterList){
           if(cluster.getEnergy()>1.5){
           int id=getCrystal(cluster);
           try{
               writer.append(id + " " + cluster.getSeedHit().getRawEnergy() + " " + cluster.getEnergy()+"\n");
           }
           catch(IOException e ){System.err.println("Error writing to output for event display");}   
           
           
           
           }
           
           }  
     
     }
     
     
     if(event.hasCollection(CalorimeterHit.class, "EcalCorrectedHits")) {
     List<CalorimeterHit> hits=event.get(CalorimeterHit.class,"EcalCorrectedHits");
         for(CalorimeterHit hit : hits){
         int idd=getCrystal(hit);
         try{writer2.append(idd+" "+hit.getRawEnergy()+"\n ");}
         catch(IOException e ){System.err.println("Error writing to output for event display");}
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
 else if(x<-1){id=-x+257;}}
 
 
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
 else if(x<-1){id=-x+257;}}
 
 
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
    
    
    
