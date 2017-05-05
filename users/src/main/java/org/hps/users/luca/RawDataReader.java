package org.hps.users.luca;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.util.Driver;

/**
 *
 */
public class RawDataReader extends Driver{
    
    private FileWriter writer;
    private FileWriter writer2;
    String outputFileName = "raw1.txt";
    String outputFileName2 = "raw2.txt";
    String rawCollectionName="EcalReadoutHits";
    String ecalReadoutName = "EcalHits";
    String ecalCollectionName = "EcalCorrectedHits";
    double scale = 1.0;
//    double pedestal = 0.0;
    double period = 4.0;
    double dt = 0.0;
     public void setScale(double scale) {
        this.scale = scale;
    }

    public void setEcalCollectionName(String ecalCollectionName) {
        this.ecalCollectionName = ecalCollectionName;
    }

    public void setRawCollectionName(String rawCollectionName) {
        this.rawCollectionName = rawCollectionName;
    }
    
    public void setOutputFileName(String outputFileName){
    this.outputFileName = outputFileName;
}
    @Override
    public void startOfData() {
        if (ecalCollectionName == null) {
            throw new RuntimeException("The parameter ecalCollectionName was not set!");
        }
        
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
    public void process(EventHeader event) {
        
        if (event.hasCollection(RawCalorimeterHit.class, rawCollectionName)) {
            // Get the list of ECal hits
            
            List<RawCalorimeterHit> hits = event.get(RawCalorimeterHit.class, rawCollectionName);
            IDDecoder dec = event.getMetaData(hits).getIDDecoder();
            for(RawCalorimeterHit hit : hits){ 
                dec.setID(hit.getCellID());
                int ix = dec.getValue("ix");
                int iy = dec.getValue("iy");
                int id=getCrystal(ix,iy);
                try{
                 writer.append(id+" "+hit.getAmplitude() + " \n");
                }
                catch(IOException e){
    System.err.println("Error closing utput file for event display.");
}
         
            }
        }
        else{System.out.println("NUOOO \n");}
        
        if(event.hasCollection(CalorimeterHit.class, "EcalHits")){
        List<CalorimeterHit> hits = event.get(CalorimeterHit.class, "EcalHits");
        for(CalorimeterHit hit : hits){
        int id=getCrystal(hit);
        try{
            writer2.append(id + " " + hit.getRawEnergy() + "\n");
        }
        catch(IOException e){System.err.println("Non riesco a scrivere raw2.");}    
        }
        }
        else{System.out.println("NUOOO hits \n");}
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
    
    public int getCrystal (int ix, int iy){
 
 int x,y,id=0;
 x= (-1)*ix;
 y= iy;
 
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
