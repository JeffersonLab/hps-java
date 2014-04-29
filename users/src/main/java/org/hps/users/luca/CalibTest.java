// import the required classes
package org.hps.users.luca;

//import hep.aida.ITupleColumn.String;
import java.io.IOException;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.lang.String;
import org.hps.recon.ecal.ECalUtils;
import org.hps.recon.ecal.HPSEcalCluster;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ParticleID;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.geometry.subdetector.HPSEcal3.NeighborMap;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;
import java.util.List;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.util.Driver;
import org.lcsim.event.base.CalorimeterHitImpl;

// the class has to be derived from the driver class
public class CalibTest extends Driver {
int counter=0;

  // constructor
  public CalibTest() {
  }
    
  //  overwrite the process method
  @Override
  protected void process(EventHeader event) {
    // Get the list of mc particles from the event
    List<MCParticle> mcParticles = event.getMCParticles();
    // Print out the number of mc particles
    //System.out.println("Event " + event.getEventNumber() + " contains " + mcParticles.size() + " mc particles.");
  
    for (MCParticle particle : mcParticles)
 {
     if(particle.getPDGID()==11)
     {if(particle.getEnergy()> 2.1){
         counter++;
       }
     }
     
     //System.out.println(particle.getPDGID());
 } 
System.out.println("ho contato" + counter + "elettroni. \n");
    /*  try
     {
    FileOutputStream prova = new FileOutputStream("prova.txt");
          PrintStream scrivi = new PrintStream(prova);*/
    //if (event.hasCollection(CalorimeterHit.class, "EcalHits")) {
   //         List<CalorimeterHit> hits = event.get(CalorimeterHit.class, "EcalHits");
     
    //        for (CalorimeterHit hit : hits) {
    //        energia=hit.getRawEnergy();
            //scrivi.print(energia );
            
            
            
     //       }
            
            
            
            
          //  }
    // }
   /*  catch (IOException e)
      {
          System.out.println("Errore: " + e);
          System.exit(1);
      }*/
   
   
  }
  
}