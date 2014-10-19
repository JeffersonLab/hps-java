/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.hps.users.luca;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.hps.recon.ecal.HPSRawCalorimeterHit;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.util.Driver;
import org.lcsim.lcio.LCIOConstants;
import java.io.PrintWriter;

/**
 *
 * @author Luca
 */
public class RawDataReader extends Driver{
    
    private FileWriter writer;
    private FileWriter writer2;
    String outputFileName = "raw1.txt";
    String outputFileName2 = "raw2.txt";
    String rawCollectionName;
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
        if (event.hasCollection(HPSRawCalorimeterHit.class, rawCollectionName)) {
            // Get the list of ECal hits.
            List<HPSRawCalorimeterHit> hits = event.get(HPSRawCalorimeterHit.class, rawCollectionName);
            for(HPSRawCalorimeterHit hit : hits){
                try{
                 writer.append(hit.getCellID()+" "+hit.getAmplitude());
                }
                catch(IOException e){
    System.err.println("Error closing utput file for event display.");
}
            try{writer2.append(hit.getAnalogHit().getIdentifierFieldValue("ix") + " " + hit.getAnalogHit().getIdentifierFieldValue("iy") + " " + hit.getAnalogHit().getRawEnergy()+" \n");    
               } 
            
            catch(IOException e){
    System.err.println("Error closing utput file for event display.");
}
            }
        }
        else{System.out.println("NUOOO \n");}
    }
}
