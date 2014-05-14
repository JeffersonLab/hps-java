package org.hps.users.luca;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.lcio.LCIOReader;
import org.lcsim.lcio.LCIOWriter;
import org.lcsim.util.Driver;

public class LCIOReaderScript2 extends Driver {
    
    String OutputName;
    public void setOutputName(String outputName){
    this.OutputName=outputName;}
    
  /* @Override
   public void endOfData(){
   System.out.println("fine dei dati");
   }
     */   
    @Override
       public void process(EventHeader event) {
            File outputFile = new File(OutputName);
           
           //Create an LCIO writer to output the new file
           LCIOWriter writer = null;
           try { writer = new LCIOWriter(outputFile);}
           catch(IOException e)
           {
           System.exit(1);}
            //get the event number to print a status update.
           int num =event.getEventNumber();
           if(num % 10000 == 0){System.out.println("Parsing event" + num + ".");}
           
           //see if the particle collection exist
           if(event.hasCollection(MCParticle.class,"MCParticle")){
              
               
               //get the MCparticle collection from the event
               
               ArrayList<MCParticle> particleList = (ArrayList<MCParticle>) event.get(MCParticle.class,"MCParticle");
               
               //Remove the MCParticle collection from event.
               event.remove("MCParticle");
               
               //make a nre list for good particles which pass some test
               ArrayList<MCParticle> goodParticles =new ArrayList<>();
               
               //sort trhough the list of MCParticle objects in the full list and add good ones to the good list
              for(MCParticle p : particleList){
                  if(p.getEnergy()>=2.1){goodParticles.add(p);}
              }
              
              //Write the good particles back to the event.
              event.put("MCParticle", goodParticles);
              
              //Write the event back out to the new file
              
               try{writer.write(event);}
               catch(IOException e){System.exit(1);}
              //close the writer
        
               
               try{writer.close();}
        catch(IOException e ){System.exit(1);}            
           }
           
           
       }
       
       
        
}
