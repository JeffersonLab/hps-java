/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.hps.users.luca;


import org.lcsim.util.Driver;
import hep.aida.IHistogram1D;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Luca
 */
public class CalibClusterAnalyzerEngRun extends Driver  {
    double energyThreshold=0;
    protected String clusterCollectionName = "EcalClusters";
    private FileWriter writer;
//  private FileWriter writer2;
    String outputFileName = "CalibClusterAnalizerEngRun.txt";
 // String outputFileName2 = "LucaTriggerHits.txt";
    public void setEnergyThreshold (double threshold){
    this.energyThreshold=threshold;
    }
  
  public void setOutputFileName(String outputFileName){
  this.outputFileName = outputFileName;
  }
   @Override   
public void startOfData(){
    try{
    //initialize the writers
    writer=new FileWriter(outputFileName);
   // writer2=new FileWriter(outputFileName2);
    //Clear the files
    writer.write("");
   // writer2.write("");
    }
    catch(IOException e ){
    System.err.println("Error initializing output file for event display.");
    }
}



public void endOfData(){
  
    try{
//close the file writer.
    writer.close();
    //writer2.close();
    }
catch(IOException e){
    System.err.println("Error closing utput file for event display.");
}
} 
    @Override
    public void process (EventHeader event){
        
        if(event.hasCollection(Cluster.class,"EcalClusters"))
        {List<Cluster> clusters= event.get(Cluster.class,"EcalClusters");
        for(Cluster cluster : clusters){
        int id;
        id=getCrystal(cluster);
        try{
         writer.append(id + " " + cluster.getEnergy()+ " " + cluster.getSize() + " " + cluster.getCalorimeterHits().get(0).getRawEnergy() + " " + cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix")+" " +cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy")+ cluster.getCalorimeterHits().get(0).getCellID() + "\n");
        }
        catch(IOException e ){System.err.println("Error writing to output for event display");}   
      
        }
        
        
        }
    
    }
    
    
 public int getCrystal (Cluster cluster){
 int x,y,id=0;
 x= cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
 y= cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
 
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
 x= hit.getIdentifierFieldValue("ix");
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
}
