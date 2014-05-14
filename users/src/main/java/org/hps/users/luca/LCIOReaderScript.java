package org.hps.users.luca;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.lcio.LCIOReader;
import org.lcsim.lcio.LCIOWriter;
import org.lcsim.util.Driver;

public class LCIOReaderScript extends Driver {
    String InputName;
    String OutputName;
    public void setOutputName(String outputName){
    this.OutputName=outputName;}
    public void setInputName(String inputName){
    this.InputName= inputName;}
	public static void prova (String inputfile, String outputfile) {
            
            /*
		// Make sure there arguments are valid.
		if(args.length != 2) {
			System.err.println("Error: Arguments must be [Input_File] [Output_File]");
			System.exit(1);
		}
		*/
		// Set the input/output files.
		File inputFile = new File(inputfile);
		File outputFile = new File(outputfile);
		
		// Make sure that the input file exists.
		if(!inputFile.canRead()) {
			System.err.println("Error: Input file can not be found.");
			System.exit(1);
		}
		
		// Create an LCIO reader to read it in.
		LCIOReader reader = null;
		try { reader = new LCIOReader(inputFile); }
		catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		// Create an LCIO writer to output the new file.
		LCIOWriter writer = null;
		try { writer = new LCIOWriter(outputFile); }
		catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		// Keep looping through events until there are no more.
		while(true) {
			// Try to get an event.
			EventHeader event = null;
			try { event = reader.read(); }
			catch(IOException e) { }
			
			// If the event is still null, there either was no event
			// or an error occurred.
			if(event == null) { break; }
			
			// Get the event number to print a status update.
			int num = event.getEventNumber();
			if(num % 10000 == 0) { System.out.println("Parsing event " + num + "."); }
			
			// See if the MCParticle collection exists.
			if(event.hasCollection(MCParticle.class, "MCParticle")) {
				// Get the MCParticle collection from the event.
				ArrayList<MCParticle> particleList = (ArrayList<MCParticle>) event.get(MCParticle.class, "MCParticle");
				
				// Remove the MCParticle collection from the event.
				event.remove("MCParticle");
				
				// Make a new list for good particles which pass some test.
				ArrayList<MCParticle> goodParticles = new ArrayList<MCParticle>();
				
				// Sort through the list of MCParticle objects in the
				// full list and add good ones to the good list.
				for(MCParticle p : particleList) {
					if(p.getEnergy() >= 2.1) { goodParticles.add(p); }
				}
				
				// Write the good particles back to the event.
				event.put("MCParticle", goodParticles);
			}
		//	System.out.println("sto scrivendo l'event nel nuovo file \n");
			// Write the event back out to the new file.
			try { writer.write(event); }
			catch(IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
                       // System.out.println("Sono alla ine del while");
                        if(event.getEventNumber()==999999){break;}
		}
		
		// Close the reader and writer.
		try {
			reader.close();
			writer.close();
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
        
   @Override
   public void process (EventHeader event){
   prova(InputName,OutputName);
   }
        
}
