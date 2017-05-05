package org.hps.users.luca;
import hep.aida.IHistogram1D;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
/**
 * This driver is supposed to read the truth information out of a SLIC output about the energy deposition in a given crystal
 */
public class TruthReader extends Driver {
 AIDA aida = AIDA.defaultInstance();
 IHistogram1D Ene=aida.histogram1D("Clusters energy with Luca's trigger",300, 0, 1);
 private FileWriter writer;
 @Override
    public void startOfData() {
    try{
    //initialize the writers
    writer=new FileWriter("verita.txt");
    writer.write("");
    }   
    catch(IOException e ){ System.err.println("Error initializing output file for event display.");}
    
    }
 @Override  
 public void process (EventHeader event){
     if(event.hasCollection(SimCalorimeterHit.class, "EcalHits")){
     List<SimCalorimeterHit> hits=event.get(SimCalorimeterHit.class, "EcalHits");
        for(SimCalorimeterHit hit : hits){
            try{writer.append(hit.getCorrectedEnergy() + " " + hit.getRawEnergy() + " " + hit.getContributedEnergy(0) + "\n");}
            catch(IOException e ){System.out.println("non riesco a scrivere perché sei stupido");}
     
        }  
     
     }
         
 }
    
}
