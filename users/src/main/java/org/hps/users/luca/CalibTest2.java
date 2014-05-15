package org.hps.users.luca;

//import hep.aida.ITupleColumn.String;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import java.io.IOException;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.lang.String;
import java.lang.Math;
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
import org.lcsim.util.aida.AIDA;
/**
 * <code>CalbTest2</code> reads the requested information from a SLIC output (non-reconstructed) slcio file and print
 * the results into a text format that can be read offline
 * @author Luca
 */





// the class has to be derived from the driver class
public class CalibTest2 extends Driver {
private FileWriter writer;
String outputFileName = "elettrons.txt";
private AIDA aida = AIDA.defaultInstance();
    IHistogram1D thetaPlot = aida.histogram1D("theta", 100, 0.0, 0.3);
    IHistogram2D pulses = aida.histogram2D("pulses",100,-3.0,3.0,100,-3.0,3.0);
    IHistogram1D eneMCPlot = aida.histogram1D("energia elettroni coulomb", 100, 0.0, 3);
    IHistogram1D enetuttiPlot = aida.histogram1D("energia tutti", 100, 0.0, 0.3);
     IHistogram1D eneTuttiEmenoPlot = aida.histogram1D("energia tutti elettroni", 100, 0.0, 0.3);
public void setOutputFileName(String outputFileName){
this.outputFileName = outputFileName;
}
 @Override   
public void startOfData(){
try{
    //initialize the writer
    writer=new FileWriter(outputFileName);
    //Clear the file
    writer.write("");
}
catch(IOException e ){
System.err.println("Error initializing output file for event display.");
}
}
 @Override   
public void endOfData(){
try{
//close the file writer.
    writer.close();
    }
catch(IOException e){
    System.err.println("Error closing utput file for event display.");
}
}
    int counter=0;
    double theta=0; //theta angle
    double PTOT;//total quadrimomentum
  // constructor
//  public CalibTest2() {
 // }
    
  //  overwrite the process method
  @Override
  protected void process(EventHeader event) {
    // Get the list of mc particles from the event
    
     if(event.hasCollection(MCParticle.class,"MCParticle"))
     {
         List<MCParticle> mcParticles = event.get(MCParticle.class,"MCParticle");
         
 // try{
    for (MCParticle particle : mcParticles)
    { enetuttiPlot.fill(particle.getEnergy());
       if(particle.getPDGID()>0)
       {    eneTuttiEmenoPlot.fill(particle.getEnergy());
             if(particle.getEnergy()> 2.1)
           { 
           PTOT=Math.sqrt(particle.getPX()*particle.getPX() + particle.getPY()*particle.getPY()+particle.getPZ()*particle.getPZ() );
           theta=Math.acos(particle.getPZ()/PTOT);
           counter++;
           //writer.append(theta+" "+particle.getPX()+" "+particle.getPY()+"\n");
           thetaPlot.fill(theta);
           pulses.fill(particle.getPX(),particle.getPY());
           eneMCPlot.fill(particle.getEnergy());
           }
      // }
     
     //System.out.println(particle.getPDGID());
     } 
  }
     }
  
 /* catch(IOException e ){
  System.err.println("Error writing tooutput for event display");
  }*/
  //System.out.println("ho contato" + counter + "elettroni. \n");
    
   
   
  }//end of Process
  
}//end of driver