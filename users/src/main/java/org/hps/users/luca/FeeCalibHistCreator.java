/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.users.luca;
import hep.aida.IHistogram1D;

import java.util.ArrayList;
import java.util.List;

import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalConditions;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Subdetector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
/**
 *
 * @author Luca
 */
public class FeeCalibHistCreator extends Driver {
    
double energyThreshold=0;
    protected String clusterCollectionName = "EcalClustersGTP";
    
    
    private EcalConditions ecalConditions = null;
    final private String ecalName = "Ecal";
    private Subdetector ecal;
    private EcalChannelCollection channels= null;
    double[] gain=new double[442];
    
    //Declaration of histograms array
    AIDA aida = AIDA.defaultInstance();
    ArrayList<IHistogram1D> GTPHists = new ArrayList<IHistogram1D>(442);
    ArrayList<IHistogram1D> GTPSeedHists = new ArrayList<IHistogram1D>(442);
    
    
    public void setEnergyThreshold (double threshold){
    this.energyThreshold=threshold;
       }
  
  
  
  

   @Override   
public void startOfData(){
   //initialize histograms  
      for(int t=0; t<442; t++){
      String cristallo=String.valueOf(t+1);  
      String GTPhistName="GTP Cluster Energy(Run)" + cristallo;
      String GTPSeedHistName="GTP Seed Energy(Run)"+ cristallo;
      
      
      IHistogram1D GTPseedhisto=aida.histogram1D(GTPSeedHistName, 200, 0.0,2.5);
      IHistogram1D GTPclushisto=aida.histogram1D(GTPhistName, 200, 0.0,2.5);
     
      GTPHists.add(GTPclushisto);
      GTPSeedHists.add(GTPseedhisto);
   
      }
    
    
}



    @Override
    public void endOfData(){
 
} 
    @Override
    public void process (EventHeader event){
        
        //here it writes the GTP clusters info
        if(event.hasCollection(Cluster.class,"EcalClustersGTP"))
        {List<Cluster> clusters= event.get(Cluster.class,"EcalClustersGTP");
         for(Cluster cluster : clusters){
           int idBack;
           
           
           idBack=getDBID(cluster);
           
           
           //riempio gli istogrammi
           if(cluster.getEnergy()>energyThreshold){
           GTPHists.get(idBack -1).fill(cluster.getEnergy());
           GTPSeedHists.get(idBack-1).fill(cluster.getCalorimeterHits().get(0).getCorrectedEnergy());
           }         
         }
        }
       
    }
    
    
  public int getDBID ( Cluster cluster ){
    int xx=  cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
    int yy=cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
    int XOFFSET=23;
    int YOFFSET=5;
    int ix = xx<0 ? xx+XOFFSET : xx+XOFFSET-1;
    int iy = yy<0 ? yy+YOFFSET : yy+YOFFSET-1;
    int dbid = ix + 2*XOFFSET*(YOFFSET*2-iy-1) + 1;
    if      (yy ==  1 && xx>-10){ dbid-=9;}
    else if (yy == -1 && xx<-10) {dbid-=9;}
    else if (yy < 0){dbid-=18;}
   return dbid;
}
  
public int getDBID ( CalorimeterHit hit ){
    int xx=  hit.getIdentifierFieldValue("ix");
    int yy=  hit.getIdentifierFieldValue("iy");
    int XOFFSET=23;
    int YOFFSET=5;
    int ix = xx<0 ? xx+XOFFSET : xx+XOFFSET-1;
    int iy = yy<0 ? yy+YOFFSET : yy+YOFFSET-1;
    int dbid = ix + 2*XOFFSET*(YOFFSET*2-iy-1) + 1;
    if      (yy ==  1 && xx>-10){ dbid-=9;}
    else if (yy == -1 && xx<-10) {dbid-=9;}
    else if (yy < 0){dbid-=18;}
   return dbid;
}  
    
}