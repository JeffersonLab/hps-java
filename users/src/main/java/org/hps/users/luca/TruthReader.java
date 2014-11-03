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
import org.lcsim.event.SimCalorimeterHit; 
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.Driver;
import hep.aida.*;

import java.io.FileWriter;
import org.lcsim.event.CalorimeterHit;
/**
 * This driver is supposed to read the truth information out of a SLIC output about the energy deposition in a given crystal
 * @author Luca
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
