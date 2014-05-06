/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.hps.users.luca;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IProfile2D;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.hps.readout.ecal.FADCEcalReadoutDriver;
import org.hps.readout.ecal.RingBuffer;
import org.hps.recon.ecal.ECalUtils;
import org.hps.recon.ecal.EcalRawConverterDriver;
import org.hps.recon.ecal.HPSEcalCluster;
import org.hps.recon.ecal.EcalClusterer;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * leggo le informazioni sugli e- coulombiani. speriamo bene
 * @author Luca
 */
public class ElectronAnalysis extends Driver {
    
    //dichiaro la classe per i grafici e quelle per la ricostruzione qui invece che nello steering
    private AIDA aida = AIDA.defaultInstance();
    IHistogram1D enePlot = aida.histogram1D("energia", 1000, 0, 3);
    // IHistogram1D enePlotcorre = aida.histogram1D("energiacorretta", 1000, 0, 3);
    
  //  FADCEcalReadoutDriver readoutDriver= new FADCEcalReadoutDriver();
   // EcalRawConverterDriver converterDriver = new EcalRawConverterDriver();
  // EcalClusterer ecalClusterer = new EcalClusterer();
    ///da qui vanno definiti gli istogrammi
    
    
    //start of data
    
    @Override
    public void startOfData() {
    
    //  ecalClusterer.setEcalName("Ecal");
    
      //ecalClusterer.setEcalCollectionName("EcalCorrectedHits");

        super.startOfData();
    
    
    }
    
    
    
    //inizio processo eventi
    
    @Override
    public void process(EventHeader event){
     /* List<CalorimeterHit> hits = event.get(CalorimeterHit.class, "EcalHits");
      for(CalorimeterHit hit : hits){
          if(hit.getRawEnergy()>2){
      enePlot.fill(hit.getRawEnergy());}
      //enePlotcorre.fill(hit.getCorrectedEnergy());
      }
      */
   if( !event.hasCollection(HPSEcalCluster.class,"EcalClusters"))
    {System.out.println(" no clusters \n");} else {
        System.out.println("yes we cluster! \n");
        }
// for(HPSEcalCluster cluster : clusters){
   // System.out.println(cluster.getSeedHit().getCellID());
   
    //}
    
    
    
    }
    
}
