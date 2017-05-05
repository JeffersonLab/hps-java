package org.hps.users.luca;

import org.lcsim.util.Driver;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.Subdetector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.util.aida.AIDA;
/**
 *
 */
public class ratetest extends Driver {
    
    private FileWriter writer;
    String outputFileName = "ratetest.txt";
    
    private EcalConditions ecalConditions = null;
    final private String ecalName = "Ecal";
    private Subdetector ecal;
    private EcalChannelCollection channels= null;
    AIDA aida = AIDA.defaultInstance();
    
    
    IHistogram1D c110=aida.histogram1D("110", 200,0,2.5 );
    IHistogram1D c18=aida.histogram1D("18", 200,0,2.5 );
    IHistogram1D c60=aida.histogram1D("60", 200,0,2.5 );
    IHistogram1D c322=aida.histogram1D("322", 200,0,2.5 );
    IHistogram1D c414=aida.histogram1D("414", 200,0,2.5 );
    IHistogram1D c364=aida.histogram1D("364", 200,0,2.5 );
    
    







public void setOutputFileName(String outputFileName){
this.outputFileName = outputFileName;
}

@Override
public void startOfData(){
    


}


@Override
public void endOfData(){


   
}
  
    @Override
    public void process (EventHeader event){
        
    if (event.hasCollection(GenericObject.class, "TriggerBank")) {
            List<GenericObject> triggerList = event.get(GenericObject.class, "TriggerBank");
            for (GenericObject data : triggerList)
                if (AbstractIntData.getTag(data) == TIData.BANK_TAG) {
                    TIData triggerData = new TIData(data);
                    if (!triggerData.isSingle1Trigger())//only process singles0 triggers...

                        return;
                }
        } else //if (debug)
            System.out.println(this.getClass().getSimpleName() + ":  No trigger bank found...running over all trigger types");
     
    
     if(event.hasCollection(Cluster.class,"EcalClusters")){
        
        
        List<Cluster> clusters= event.get(Cluster.class,"EcalClusters");
        
         for(Cluster cluster : clusters){
             int id=getDBID(cluster);
             if(cluster.getEnergy()>0.65 && cluster.getSize()==1){
             if(id==110){c110.fill(cluster.getEnergy());}
             
             }
             
         }
     }//end of if has collection
    
    }//end pf process
    
    
    
    
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