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
 * @author mac
 */


public class ClusterReader extends Driver {
 protected String clusterCollectionName = "EcalClusters";   
  
  @Override
 public void process (EventHeader event){
   
          
           
           
     
     //get the clusters from the event
     if(event.hasCollection(Cluster.class, "EcalClusters")) {
        List<Cluster> clusterList =event.get(Cluster.class,clusterCollectionName );
     
        for(Cluster cluster : clusterList){
           List<CalorimeterHit> hits = cluster.getCalorimeterHits();
        if(cluster.getEnergy()>1.5){System.out.println("Ha energia maggiore! \n");}
        else{System.out.println("ha energia minore! /n");}
        
        }
        
        
        
        
     }
    
}
}
