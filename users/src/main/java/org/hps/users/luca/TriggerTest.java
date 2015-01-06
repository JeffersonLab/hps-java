// import the required classes
package org.hps.users.luca;

//import hep.aida.ITupleColumn.String;
import hep.aida.IHistogram1D;

import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;


// the class has to be derived from the driver class
public class TriggerTest extends Driver {

 AIDA aida = AIDA.defaultInstance();
 IHistogram1D elettroni=aida.histogram1D("elettroni", 150, 0.0,3.0);
 IHistogram1D positroni=aida.histogram1D("positroni", 150, 0.0,3.0);
    
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
     {elettroni.fill(particle.getEnergy());
     }
     if(particle.getPDGID()==-11)
     {positroni.fill(particle.getEnergy());
     }
     //System.out.println(particle.getPDGID());
 } 

   
  }
  
}