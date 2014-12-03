// import the required classes
package org.hps.users.luca;

//import hep.aida.ITupleColumn.String;

import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.util.Driver;
import hep.aida.IHistogram1D;
import org.lcsim.util.aida.AIDA;
// the class has to be derived from the driver class
public class CalibTest extends Driver {
//int counter=0;
AIDA aida = AIDA.defaultInstance();
IHistogram1D electronEne=aida.histogram1D("Electrons' energy spectrum",300, 0, 3);
IHistogram1D positronEne=aida.histogram1D("Protons' energy spectrum",300, 0, 3);
  // constructor
 // public CalibTest() {
  //}
    
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
     {electronEne.fill(particle.getEnergy());
     }
     else if(particle.getPDGID()==-11)
     {positronEne.fill(particle.getEnergy());
     }
     
     //System.out.println(particle.getPDGID());
 } 
//System.out.println("ho contato" + counter + "elettroni. \n");
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