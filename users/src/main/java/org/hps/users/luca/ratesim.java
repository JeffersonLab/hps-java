/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.users.luca;




import org.lcsim.util.Driver;
import hep.aida.IHistogram1D;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;

import org.hps.conditions.ecal.EcalConditions;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Subdetector;
import org.lcsim.util.aida.AIDA;
/**
 *
 * @author Luca
 */
public class ratesim extends Driver {
    
    private FileWriter writer;
    String outputFileName = "ratesim.txt";
    
    private EcalConditions ecalConditions = null;
    final private String ecalName = "Ecal";
    private Subdetector ecal;
    private EcalChannelCollection channels= null;
    AIDA aida = AIDA.defaultInstance();
    ArrayList<IHistogram1D> Top = new ArrayList<IHistogram1D>(6);
    ArrayList<IHistogram1D> Bot = new ArrayList<IHistogram1D>(6);
    ArrayList<IHistogram1D> TopNoCut = new ArrayList<IHistogram1D>(6);
    ArrayList<IHistogram1D> BotNoCut = new ArrayList<IHistogram1D>(6);
    
    

    ArrayList<IHistogram1D> TopCryEne = new ArrayList<IHistogram1D>(4);
    ArrayList<IHistogram1D> BotCryEne = new ArrayList<IHistogram1D>(4);
    
    IHistogram1D clusTot=aida.histogram1D("ALL", 150,0,1.5 );
    
    IHistogram1D CryId=aida.histogram1D("cry Id",446,1,447);
    double[] timei=new double[6];
    double timef=0;
    double counterTop=0;
    double counterBot=0;
    double counterEcut=0;
    
    double nevents=0;
    
double[] time=new double[6];
double E0=1.1;

//e=1.05
double[] Ymax={45.81,53.75,60.76,66.97,74.05,80.08};
double[] Ymin={38.84,45.81,53.75,60.76,66.97,74.05};
double[] Xmax={-16.036,-15.543,-15.05,-14.556,-14.062,-16.568};
double[] Xmin={-22.54,-23.33,-23.53,-24.02,-24.514,-25.008};


int[] countertop=new int[6];
int[] counterbot=new int[6];
int[]crycountertop=new int[5];
int[]crycounterbot=new int[5];

double[] vertpos=new double[5];




double[] ecuttopmin={0.6,0.6,0.6,0.6,0.6,0.6};
double[] ecuttopmax={1,1,1,1,1,1};

double[] ecutbotmin= {0.6,0.6,0.6,0.6,0.6,0.6};
double[] ecutbotmax={1,1,1,1,1,1};



double vertical;







public void setOutputFileName(String outputFileName){
this.outputFileName = outputFileName;
}

@Override
public void startOfData(){
    System.out.println("mo spacco tutto davero!!!\n");
 //inizializzo il file di uscita
   try{
    //initialize the writers
    writer=new FileWriter(outputFileName);
    writer.write("");
   }
    catch(IOException e ){
    System.err.println("Error initializing output file for event display.");
    } 
    
    
//inizializzo istogrammi
     for(int t=0;t<4;t++){
     String bin=String.valueOf(t+1);  
     
     String topcryenename="CLU in row " + bin;
     
     String bin2=String.valueOf(-(t+1));
    
     String botcryenename="CLU in row " + bin2;
     
     IHistogram1D Topcryenehist=aida.histogram1D(topcryenename,150,0,1.5);
     IHistogram1D botcryenehist=aida.histogram1D(botcryenename, 150,0,1.5);
     TopCryEne.add(Topcryenehist);
     BotCryEne.add(botcryenehist);
     
     crycountertop[t]=0;
     crycounterbot[t]=0;
     }
    
    
      for(int t=0; t<6; t++){
      String bin=String.valueOf(t+1);  
      String top="(TOP) FEE in Bin in " + bin;
      String bot="(BOT) FEE in Bin in "+ bin;
      String tope="(TOP) Cluster Energy in Bin " + bin;
      String bote="(BOT) Cluster Energy in Bin "+ bin;
      String topcryname="(TOP) Crystals in Bin " + bin;
      String botcryname="(BOT) Crystals in Bin " + bin;
      
      
      
      IHistogram1D Toppe=aida.histogram1D(top, 250, 0.0,2.5);
      IHistogram1D Botte=aida.histogram1D(bot, 250, 0.0,2.5);
      IHistogram1D TopNoCute=aida.histogram1D(tope, 250, 0.0,2.5);
      IHistogram1D BotNoCute=aida.histogram1D(bote, 250, 0.0,2.5);
      
      
 
      Top.add(Toppe);
      Bot.add(Botte);
      TopNoCut.add(TopNoCute);
      BotNoCut.add(BotNoCute);
      
      }
    
    
    
for (int i =0;i<6;i++){
    time[i]=0;
    timei[i]=0;
    
    countertop[i]=0;
    counterbot[i]=0;
    
}


}


@Override
public void endOfData(){
double timme=(timef-timei[0])/1000000000;

//for(int i=0;i<6;i++){System.out.println(i + " " + time[i] + "\n");}

try{
writer.append("Total event in file " + nevents + " Total time = "+ timme  +"\n");
writer.append("TOP \n");
for(int i=0;i<6;i++){
 int bin=i+1;   
 time[i]=timef-timei[i];   
writer.append("Bin " + bin + " Total event  " + countertop[i] + " Rate = " + (countertop[i]/time[i])*1000000000 + "\n" );
}


for(int i=0;i<6;i++){
 int bin=i+1;   
 time[i]=timef-timei[i];   
writer.append("Bin " + bin +" Total event BOT " + counterbot[i] + " Rate = " + (counterbot[i]/time[i])*1000000000 + "\n" );
}

writer.append("\n \n \n");
for(int t=0;t<5;t++){
writer.append("events in row  " + t + " = " + crycountertop[t]+ " Rate = " + (crycountertop[t]/timme) + "\n" );
int menot=-t;
writer.append("events in row  " + menot + " = " + crycounterbot[t]+ " Rate = " + (crycounterbot[t]/timme) + "\n" );
}
writer.append("\n TEMPO = " + timme +"\n");

}


catch(IOException e)
    {
    System.err.println("Non ho scritto sul file");
    }

System.out.println("TEMPO =  " + timme + "\n");
/*vertical=vertical-6.5;
for(int i=0;i<7;i++)
{double theta=Math.atan2( (vertical+i*13),1397);
System.out.println("theta = " + theta + "\n");
}
*/

    try
    {
//close the file writer.
    writer.close();
    }
    catch(IOException e)
    {
    System.err.println("Error closing utput file for event display.");
    }
}
  
    @Override
    public void process (EventHeader event){
        
     timef=event.getTimeStamp();
     
     nevents++;
     
     
        /* natha's code for trigger
        List <AbstractIntData> aids = event.get(AbstractIntData.class, "TriggerBank");
        for (AbstractIntData aid : aids) {
            if (aid.getTag() == TIData.BANK_TAG) {
                TIData tt=(TIData)aid;
                if (!tt.isSingle1Trigger()) return;
                        break;
            }
        }
     */ //nathans code for trigger end
     
    
   /*  
  if (event.hasCollection(GenericObject.class, "TriggerBank")) {
            List<GenericObject> triggerList = event.get(GenericObject.class, "TriggerBank");
            for (GenericObject data : triggerList)
                if (AbstractIntData.getTag(data) == TIData.BANK_TAG) {
                    TIData triggerData = new TIData(data);
                    if (!triggerData.isSingle1Trigger())//only process singles1 triggers...

                        return;
                }
        } else //if (debug)
            System.out.println(this.getClass().getSimpleName() + ":  No trigger bank found...running over all trigger types");
  
  
  */
     for(int i=0;i<6;i++){
    if(countertop[i]==0){timei[i]=timef;}}
        
    if(event.hasCollection(Cluster.class,"EcalClustersGTP")){
        
        
        List<Cluster> clusters= event.get(Cluster.class,"EcalClustersGTP");
        
         for(Cluster cluster : clusters){
         
             
          int ID=getDBID(cluster);   
         clusTot.fill(cluster.getEnergy());
         double posY=cluster.getPosition()[1];
         
         double xcl=cluster.getPosition()[0];
         double posX=xcl -(0.0066/Math.sqrt(cluster.getEnergy()) -0.03)*xcl -(0.028*cluster.getEnergy()-0.451/Math.sqrt(cluster.getEnergy())+0.465)*10;
         double cryx=cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
         double cryy=cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
         
         //if(cryy==1){verpos[0]=posY;}
         //da qui righe e colonne
         
        if(cryx==-5||cryx==-6){
            
            for(int i=0;i<5;i++){
                //top
                if(cryy==i+1){
                 if(cluster.getEnergy()>0.6 && cluster.getEnergy()<1.05){
                     
                     TopCryEne.get(i).fill(cluster.getEnergy());
                     crycountertop[i]++;
                    }
                }
           
               //bottom
                if(cryy==-(i+1)){
                    if(cluster.getEnergy()>0.6 && cluster.getEnergy()<1.05){
                    
                    BotCryEne.get(i).fill(cluster.getEnergy());
                    crycounterbot[i]++;
                    }
                }
            }
        } 
        
        
        ///da qui bin angolari 
         
         for(int i=0;i<6;i++){
             //top
             if(posY>Ymin[i]&&posY<=Ymax[i]){
                if(posX<=Xmax[i] &&posX>=Xmin[i]){      
                    TopNoCut.get(i).fill(cluster.getEnergy());
                    if(cluster.getEnergy() > ecuttopmin[i] && cluster.getEnergy() < ecuttopmax[i]){
                        Top.get(i).fill(cluster.getEnergy());
                        countertop[i]++;
                        CryId.fill(ID);
                        
                    }//tagli energia
                } //if su X
             }//if pos y   
             
            //controllo down                 
             else if(posY<= -Ymin[i]&& posY> -Ymax[i]){
                if(posX<Xmax[i] && posX>Xmin[i]){
                    BotNoCut.get(i).fill(cluster.getEnergy());
                    if(cluster.getEnergy()> ecutbotmin[i] && cluster.getEnergy()<ecutbotmax[i]){
                        Bot.get(i).fill(cluster.getEnergy());
                        counterbot[i]++;
                       
                        CryId.fill(ID);
                    }//e cut bot  
                } //xpos    
         }// ypos bot
        }//for sui bin
       
    }//end of for over clusters
    }   //and of if has colelction  
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