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
import hep.aida.IHistogram1D;
import java.io.FileWriter;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.MCParticle;
/**
 *
 * @author mac
 */
public class CalibClusterAnalizer extends Driver {
 
    protected String clusterCollectionName = "EcalClusters";  
    AIDA aida = AIDA.defaultInstance();
    IHistogram1D eneTuttiPlot = aida.histogram1D("All Clusters Energy", 300, 0.0,3.0);
    IHistogram1D SeedHitPlot = aida.histogram1D("SeedHit Energy", 300, 0.0,3.0);
    
    
    @Override
    public void process (EventHeader event){
        
        if(event.hasCollection(Cluster.class,"EcalClusters"))
        {List<Cluster> clusters= event.get(Cluster.class,"EcalClusters");
        for(Cluster cluster : clusters){
        eneTuttiPlot.fill(cluster.getEnergy());
       SeedHitPlot.fill(HPSEcalCluster.getSeedHit(cluster).getCorrectedEnergy());
        }
        
        
        }
    
    }
    
}
