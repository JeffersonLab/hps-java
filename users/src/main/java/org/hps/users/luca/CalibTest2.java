package org.hps.users.luca;

//import hep.aida.ITupleColumn.String;
import hep.aida.IHistogram1D;

import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * <code>CalbTest2</code> reads the requested information from a SLIC output (non-reconstructed) slcio file and print
 * the results into a text format that can be read offline
 * @author Luca
 */





// the class has to be derived from the driver class
public class CalibTest2 extends Driver {
/*private FileWriter writer;
private FileWriter writer2;
String outputFileName = "coulombelectrons.txt";
String outputFileName2 = "coulombelectronsStopped.txt";*/
private AIDA aida = AIDA.defaultInstance();
   

    IHistogram1D eneMCallPlot = aida.histogram1D("All MCParticles Energy", 300, 0.0, 3);
    IHistogram1D eneEminusPlot = aida.histogram1D("All electrons Energy", 300, 0.0, 3);
     IHistogram1D eneCoulombEPlot = aida.histogram1D("Coulomb Electrons", 300, 0.0, 3);
     IHistogram1D ParticleIdPlot = aida.histogram1D("ParticleId", 100,-50, 50);
/*public void setOutputFileName(String outputFileName){
this.outputFileName = outputFileName;
}*/
/* @Override   
public void startOfData(){
try{
    //initialize the writer
    writer=new FileWriter(outputFileName);
    writer2=new FileWriter(outputFileName2);
    //Clear the file
    writer.write("");
    writer2.write("");
}
catch(IOException e ){
System.err.println("Error initializing output file for event display.");
}
}*/
/* @Override   
public void endOfData(){
try{
//close the file writer.
    writer.close();
    writer2.close();
    }
catch(IOException e){
    System.err.println("Error closing utput file for event display.");
}
}*/
    
    
  //  overwrite the process method
  @Override
  protected void process(EventHeader event) {
    // Get the list of mc particles from the event
    
     if(event.hasCollection(MCParticle.class,"MCParticle"))
     {
         List<MCParticle> mcParticles = event.get(MCParticle.class,"MCParticle");
     
      for(MCParticle particle : mcParticles){
       ParticleIdPlot.fill(particle.getPDGID());
      eneMCallPlot.fill(particle.getEnergy());
      if(particle.getPDGID()==11)
      {eneEminusPlot.fill(particle.getEnergy());}
      if(particle.getPDGID()==11 && particle.getEnergy()>2.150)
      {eneCoulombEPlot.fill(particle.getEnergy());}
    
      
      }
         
     }  
  
  }//end of Process
  
}//end of driver