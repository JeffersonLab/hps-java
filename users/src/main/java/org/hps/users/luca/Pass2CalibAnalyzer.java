package org.hps.users.luca;

import hep.aida.IHistogram1D;

import java.util.ArrayList;
import java.util.List;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.Subdetector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
/**
 *
 */
public class Pass2CalibAnalyzer extends Driver  {
    double energyThreshold=0;
    protected String clusterCollectionName = "EcalClustersGTP";
    protected String clusterCollectionName2 = "EcalClustersIC";
    private EcalConditions ecalConditions = null;
    final private String ecalName = "Ecal";
    private Subdetector ecal;
    private EcalChannelCollection channels= null;
    double[] gain=new double[442];
    
    //istogrammu
    AIDA aida = AIDA.defaultInstance();
   
    ArrayList<IHistogram1D> ICHists = new ArrayList<IHistogram1D>(442);
    ArrayList<IHistogram1D> ICSeedHists = new ArrayList<IHistogram1D>(442);
    ArrayList<IHistogram1D> GTPHists = new ArrayList<IHistogram1D>(442);
    ArrayList<IHistogram1D> GTPSeedHists = new ArrayList<IHistogram1D>(442);
    
    //double[] mygains={0.17,0.181,0.155,0.196,0.178,0.182,0.187,0.172,0.198,0.175,0.191,0.22864,0.219093,0.224226,0.187811,0.205535,0.217383,0.206476,0.197209,0.209896,0.229483,0.225912,0.167822,0.227948,0.232701,0.205919,0.180198,0.176,0.179,0.18,0.191,0.187,0.12,0.12,0.218,0.204,0.105,0.118,0.194,0.171,0.148,0.12,0.193,0.196,0.115,0.126,0.188,0.183,0.182,0.176,0.183,0.174,0.178,0.169,0.207395,0.245306,0.222105,0.220242,0.21169,0.208066,0.193918,0.212912,0.206663,0.185503,0.215672,0.233315,0.179415,0.197367,0.229924,0.223594,0.195459,0.20715,0.216598,0.212833,0.182,0.183,0.173,0.139,0.116,0.12,0.232,0.375,0.117,0.123,0.183,0.175,0.119,0.121,0.216,0.207,0.105,0.112,0.2,0.177,0.174,0.184,0.168,0.165,0.171,0.168,0.16,0.237196,0.216983,0.191974,0.223878,0.227687,0.190862,0.200952,0.220365,0.223872,0.231075,0.229662,0.231524,0.211966,0.22114,0.214275,0.202543,0.214011,0.223159,0.196,0.184,0.169,0.179,0.168,0.115,0.119,0.196,0.194,0.12,0.114,0.178,0.183,0.126,0.126,0.171,0.215,0.116,0.119,0.189,0.165,0.164,0.19,0.178,0.166,0.181,0.171,0.240076,0.209051,0.24329,0.225717,0.241676,0.221112,0.244575,0.203469,0.239822,0.232453,0.2686,0.2045,0.210024,0.180782,0.205848,0.236478,0.221362,0.215202,0.220609,0.196,0.173,0.179,0.187,0.181,0.107,0.102,0.201,0.214,0.107,0.123,0.167,0.189,0.115,0.116,0.2,0.188,0.098,0.124,0.159,0.174,0.167,0.204,0.178,0.176,0.176,0.18,0.167,0.188952,0.20986,0.211494,0.227115,0.326532,0.236429,0.188189,0.18143,0.211053,0.173,0.176,0.172,0.174,0.193,0.133,0.122,0.188,0.212,0.127,0.113,0.186,0.171,0.133,0.125,0.176,0.197,0.094,0.113,0.189,0.168,0.183,0.168,0.195,0.2,0.19,0.183,0.229883,0.207711,0.236336,0.185985,0.235952,0.215961,0.205741,0.146512,0.159686,0.213107,0.192,0.151,0.115,0.187,0.202,0.131,0.139,0.204,0.174,0.128,0.122,0.181,0.184,0.133,0.127,0.179,0.19,0.173,0.183,0.185,0.173,0.166,0.178,0.187,0.183,0.194,0.198,0.224021,0.262922,0.191143,0.188164,0.246592,0.229807,0.228564,0.246168,0.271616,0.208531,0.2492,0.234327,0.18215,0.204872,0.197765,0.224463,0.131451,0.12417,0.18939,0.201661,0.113,0.117,0.2,0.186,0.137,0.121,0.177,0.165,0.13,0.133,0.174,0.166,0.132,0.131,0.19,0.162,0.177,0.177,0.178,0.187,0.195,0.175,0.19,0.211,0.182,0.162,0.220277,0.191209,0.218454,0.213889,0.224527,0.235918,0.190844,0.241915,0.211924,0.257578,0.201244,0.211065,0.217196,0.232198,0.215907,0.214647,0.120287,0.144153,0.208125,0.182,0.124,0.219,0.185,0.182,0.101,0.126,0.189,0.173,0.118,0.114,0.172,0.179,0.147,0.119,0.186,0.176,0.159,0.173,0.187,0.181,0.172,0.167,0.18,0.175,0.171,0.182,0.187,0.200199,0.216328,0.191837,0.209114,0.25346,0.211998,0.182641,0.200406,0.204201,0.191989,0.246661,0.23143,0.149329,0.185368,0.203758,0.13947,0.15364,0.17,0.178,0.124,0.115,0.185,0.2,0.122,0.107,0.189,0.185,0.111,0.128,0.177,0.177,0.116,0.114,0.198,0.179,0.188,0.191,0.19,0.184,0.187,0.186,0.197,0.171,0.199,0.192,0.178,0.165,0.207939,0.198116,0.189537,0.229394,0.230347,0.220014,0.205517,0.184679,0.220733,0.208228,0.240808,0.21955,0.247955,0.212554,0.135822,0.146155,0.186,0.178,0.138,0.12,0.197,0.188,0.113,0.114,0.165,0.184,0.098,0.119,0.18,0.168,0.14,0.113,0.182,0.187,0.183,0.166};
    
    public void setEnergyThreshold (double threshold){
    this.energyThreshold=threshold;
       }
  
  
  
  
  
@Override
    public void detectorChanged(Detector detector) {
        // Get the Subdetector.
        ecal = detector.getSubdetector(ecalName);
        
      /*  // ECAL combined conditions object.
        ecalConditions = ConditionsManager.defaultInstance()
                .getCachedConditions(EcalConditions.class, TableConstants.ECAL_CONDITIONS).getCachedData();*/        
                
        ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();
         channels = ecalConditions.getChannelCollection();
    }  
   @Override   
public void startOfData(){
   //initialize histograms  
      for(int t=0; t<442; t++){
      String cristallo=String.valueOf(t+1);  
      String GTPhistName="GTP Cluster Energy(Run)" + cristallo;
      String GTPSeedHistName="GTP Seed Energy(Run)"+ cristallo;
      String IChistName="IC Cluster Energy(Run)" + cristallo;
      String ICSeedHistName="IC Seed Energy(Run)"+ cristallo;
      
      IHistogram1D GTPseedhisto=aida.histogram1D(GTPSeedHistName, 200, 0.0,2.5);
      IHistogram1D GTPclushisto=aida.histogram1D(GTPhistName, 200, 0.0,2.5);
      IHistogram1D ICseedhisto=aida.histogram1D(ICSeedHistName, 200, 0.0,2.5);
      IHistogram1D ICclushisto=aida.histogram1D(IChistName, 200, 0.0,2.5);
     
      GTPHists.add(GTPclushisto);
      GTPSeedHists.add(GTPseedhisto);
      ICHists.add(ICclushisto);
      ICSeedHists.add(ICseedhisto);
      }
    
   
}



    @Override
    public void endOfData(){
  
} 
    @Override
    public void process (EventHeader event){
        
       // EcalConditions ecalConditions = ConditionsManager.defaultInstance().getCachedConditions(EcalConditions.class, TableConstants.ECAL_CONDITIONS).getCachedData();
       // EcalChannelCollection channels = ecalConditions.getChannelCollection();
        //here it writes the GTP clusters info
        if(event.hasCollection(Cluster.class,"EcalClustersGTP"))
        {List<Cluster> clusters= event.get(Cluster.class,"EcalClustersGTP");
         for(Cluster cluster : clusters){
           int idBack;
           
           
           idBack=getDBID(cluster);
           //EcalChannelCollection channels = ecalConditions.getChannelCollection();
           EcalChannel channel = channels.findGeometric(cluster.getCalorimeterHits().get(0).getCellID());
           EcalChannelConstants channelConstants = ecalConditions.getChannelConstants(channel);
           //System.out.println(channelConstants.getGain().getGain() + " ot asil cristallo " + idBack+  " \n ");
           gain[idBack-1]=channelConstants.getGain().getGain();
          /* try{
            writer.append(idBack + " " + cluster.getEnergy()+ " " + cluster.getSize() + " " + cluster.getCalorimeterHits().get(0).getCorrectedEnergy() + " " + cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix")+" " +cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy") +  " " + channelConstants.getGain().getGain() + "\n");
            }
           catch(IOException e ){System.err.println("Error writing to output for event display");}   
           */
           
           //riempio gli istogrammi (Se Ecl>1.3)
           if(cluster.getEnergy()>=1.3)
             {
              GTPHists.get(idBack -1).fill(cluster.getEnergy());
              GTPSeedHists.get(idBack-1).fill(cluster.getCalorimeterHits().get(0).getCorrectedEnergy());
             }
         }//end of cluster for
         
         
        }
        //here it writes the ICCluster info
        if(event.hasCollection(Cluster.class,"EcalClustersIC"))
        {List<Cluster> clusters= event.get(Cluster.class,"EcalClustersIC");
        ClusterUtilities.sortReconClusterHits(clusters); 
         for(Cluster cluster : clusters){
         EcalChannel channel = channels.findGeometric(cluster.getCalorimeterHits().get(0).getCellID());
         EcalChannelConstants channelConstants = ecalConditions.getChannelConstants(channel);    
           int idBack;
           int idFront;
          // idFront=getCrystalFront(cluster);
           idBack=getDBID(cluster);
          /* try{
            writer2.append(idBack + " " + cluster.getEnergy()+ " " + cluster.getSize() + " " + cluster.getCalorimeterHits().get(0).getCorrectedEnergy() + " " + cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix")+" " +cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy") + " " + channelConstants.getGain().getGain() + "\n");
            }
           catch(IOException e ){System.err.println("Error writing to output for event display");}   
         }*/
           
          ICHists.get(idBack -1).fill(cluster.getEnergy());
          ICSeedHists.get(idBack-1).fill(cluster.getCalorimeterHits().get(0).getCorrectedEnergy()); 
           
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
    
 
}//end of class
