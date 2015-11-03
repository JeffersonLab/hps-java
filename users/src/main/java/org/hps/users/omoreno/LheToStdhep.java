package org.hps.users.omoreno;


import hep.io.stdhep.StdhepEvent;
import hep.io.stdhep.StdhepWriter;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.DefaultParser;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.lcsim.detector.IRotation3D;
import org.lcsim.detector.RotationGeant;

/**
 * A class to convert LHE events to Stdhep.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id:$
 *
 * TODO: Make this converter more generic.
 */
public class LheToStdhep {
	
	private static final int N_PARTICLE_INDEX = 0;
	private static final int PDG_ID_INDEX = 1;
	private static final int STATUS_INDEX = 2; 
	private static final int FIRST_MOTHER_INDEX = 3; 
	private static final int SECOND_MOTHER_INDEX = 4; 
	private static final int FIRST_DAUGHTER_INDEX = 5; 
	private static final int SECOND_DAUGHTER_INDEX = 6; 
	
	private static double sigmaX = 0.2;
	private static double sigmaY = 0.02;
	private static double sigmaZ = 0.0;
	
	private static double offsetX = 0;
	private static double offsetY = 0;
	private static double offsetZ = 0.03;
	
	static int eventNumber = 0; 
	
	
	public static void main(String[] args) throws IOException{

		String lheFileName = null; 
		String stdhepFileName = "output.stdhep";
		
		// Instantiate te command line parser
		CommandLineParser parser = new DefaultParser(); 

		// Create the Options
		// TODO: Add ability to parse list of files.
		// Allow a user to pass tag.gz files
		Options options = new Options(); 
		options.addOption("i", "input", true, "Input LHE file name");
		options.addOption("o", "output", true, "Output Stdhep file name");
		
		try { 
			// Parse the command line arguments
			CommandLine line = parser.parse(options, args);
			
			// If the file is not specified, notify the user and exit the program
			if(!line.hasOption("i")){
				System.out.println("Please specify an LHE file to process");
				System.exit(0);
			}
			lheFileName = line.getOptionValue("i");
		
			// Check if the user has specified the output file name and that the
			// extension is stdhep.  If not, add the extension
			if(line.hasOption("o")){
				stdhepFileName = line.getOptionValue("o");
			}
		} catch(ParseException e){
			System.out.println("Unexpected exception: " + e.getMessage());
		}
		
		convertToStdhep(lheFileName, stdhepFileName);
		
	}

	/**
	 * 
	 */
	static private void convertToStdhep(String lheFileName, String stdhepFileName) throws IOException{
		List<Element> events = readLhe(lheFileName);

		StdhepWriter writer = new StdhepWriter(
						   	stdhepFileName,
						   	"Import Stdhep Events",
						   	"Imported from LHE generated from MadGraph",
						   	events.size()
							);
		writer.setCompatibilityMode(false);
		
		for(Element event : events){
			writeEvent(event, writer);
		}
		writer.close();
	}

	/**
	 * 
	 */
	private static List<Element> readLhe(String lheFileName){
		
		// Instantiate the SAX parser used to build the JDOM document
		SAXBuilder builder = new SAXBuilder(); 
		
		// Open the lhe file
		File lheFile = new File(lheFileName);
			
		// Parse the lhe file and build the JDOM document
		Document document = null;
		List<Element> eventNodes = null; 
		try {
			
			document = (Document) builder.build(lheFile);
			
			// Get the root node
			Element rootNode = document.getRootElement(); 
			
			// Get a list of all nodes of type event
			eventNodes = rootNode.getChildren("event");
		
		} catch (JDOMException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return eventNodes; 
	}

	/**
	 * 
	 */
	private static void writeEvent(Element event, StdhepWriter writer) throws IOException{
		
		int numberOfParticles = 0; 
		int particleIndex = 0;
		int pdgID[] = null; 
		int particleStatus[] = null;
		int motherParticles[] = null; 
		int daughterParticles[] = null; 
		double particleMomentum[] = null;
		double particleVertex[] = null; 
		
		Random generator = new Random(); 
		
		eventNumber++; 
		
		System.out.println("#================================================#\n#");
		System.out.println("# Event: " + eventNumber);
		
		
		// Get the text within the event element node.  An element node contains
		// information describing the event and it's particles.  The PDG ID of
		// a particle along with it's kinematics is listed on it's own line.
		// In order to parse the information for each particle, the text is 
		// split using the newline character as a delimiter.  
		String[] eventData = event.getTextTrim().split("\n");	
	 
		for(int datumIndex = 0; datumIndex < eventData.length; datumIndex++){
			
			// Split a line by whitespace
			String[] eventTokens = eventData[datumIndex].split("\\s+");
		
			if(datumIndex == 0){
				
				numberOfParticles = Integer.valueOf(eventTokens[N_PARTICLE_INDEX]);
				System.out.println("# Number of particles: " + numberOfParticles + "\n#");
				System.out.println("#================================================#");
		
				// Reset all arrays used to build the Stdhep event
				particleIndex = 0; 
				particleStatus = new int[numberOfParticles];
				pdgID = new int[numberOfParticles];
				motherParticles = new int[numberOfParticles*2];
				daughterParticles = new int[numberOfParticles*2];
				particleMomentum = new double[numberOfParticles*5];
				particleVertex = new double[numberOfParticles*4];
			
				continue;
			}
	
			// Get the PDG ID of the particle
			pdgID[particleIndex] = Integer.valueOf(eventTokens[PDG_ID_INDEX]);
			
			
			System.out.println(">>> PDG ID: " + pdgID[particleIndex]);
			
			// Get the status of the particle (initial state = -1, final state = 1, resonance = 2)
			particleStatus[particleIndex] = Integer.valueOf(eventTokens[STATUS_INDEX]);
			if(particleStatus[particleIndex] == -1) particleStatus[particleIndex] = 3; 
			System.out.println(">>>> Particle Status: " + particleStatus[particleIndex]);
			
			 // Get the mothers of a particle.  If the particle is a trident electron, then assign it
			 // a mother value of 10 so it's distinguishable from the beam electron.
			if(pdgID[particleIndex] == 611){
				motherParticles[particleIndex*2] = 10;
				// If the PDG ID is equal to 611/-611 (trident electron) change it back to 11/-11. 
				// Otherwise, SLIC won't do anything with them.
				pdgID[particleIndex] = 11;
			} else if(pdgID[particleIndex] == -611){
				motherParticles[particleIndex*2] = 10;
				pdgID[particleIndex] = -11; 
			} else {
				motherParticles[particleIndex*2] = Integer.valueOf(eventTokens[FIRST_MOTHER_INDEX]);
			}
	        motherParticles[particleIndex*2 + 1] = Integer.valueOf(eventTokens[SECOND_MOTHER_INDEX]);
	        System.out.println(">>>> Mothers: 1) " + motherParticles[particleIndex*2] + " 2) " + motherParticles[particleIndex*2 + 1]);
	        
	        // Get the daughter particles
	        daughterParticles[particleIndex*2] = Integer.valueOf(eventTokens[FIRST_DAUGHTER_INDEX]);
	        daughterParticles[particleIndex*2 + 1] = Integer.valueOf(eventTokens[SECOND_DAUGHTER_INDEX]);
	        System.out.println(">>>> Daughter: 1) " + daughterParticles[particleIndex*2] + " 2) " + daughterParticles[particleIndex*2 + 1]);
	        
	        // Get the particle momentum, its mass and energy
	        particleMomentum[particleIndex*5] = Double.valueOf(eventTokens[7]);		// px
	        particleMomentum[particleIndex*5 + 1] = Double.valueOf(eventTokens[8]);	// py   
	        particleMomentum[particleIndex*5 + 2] = Double.valueOf(eventTokens[9]); // pz
	        particleMomentum[particleIndex*5 + 3] = Double.valueOf(eventTokens[10]); // Particle Energy
	        particleMomentum[particleIndex*5 + 4] = Double.valueOf(eventTokens[11]); // Particle Mass
	        
	        // Rotate the particle by 30 mrad around the beam axis
	        Hep3Vector rotatedMomentum = 
	        		rotateToDetector(particleMomentum[particleIndex*5], 
	        				particleMomentum[particleIndex*5+1], 
	        				particleMomentum[particleIndex*5+1]);
	        
	        particleMomentum[particleIndex*5] = rotatedMomentum.x();
	        particleMomentum[particleIndex*5 + 1] = rotatedMomentum.y();
	        particleMomentum[particleIndex*5 + 2] = rotatedMomentum.z();

	        // Set the origin of the particle
	        Hep3Vector rotatedVertex = rotateToDetector(sigmaX*generator.nextGaussian() + offsetX, 
	        		sigmaY*generator.nextGaussian() + offsetY, 
	        		sigmaZ*generator.nextGaussian() + offsetZ);
	        particleVertex[particleIndex*4] = rotatedVertex.x();
	        particleVertex[particleIndex*4+1] = rotatedVertex.y();
	        particleVertex[particleIndex*4+2] = rotatedVertex.z();
	        particleVertex[particleIndex*4+3] = 0; 
	        
	        // Increment the particle number
	        particleIndex++;
	        
			System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		}
		
		// Create the Stdhep event and write it 
	    StdhepEvent stdhepEvent = new StdhepEvent(eventNumber, numberOfParticles, particleStatus, 
	    		pdgID, motherParticles, daughterParticles, particleMomentum, particleVertex);
	    writer.writeRecord(stdhepEvent);
	}

	/**
	 * 
	 */
	private static Hep3Vector rotateToDetector(double x, double y, double z){
		IRotation3D rotation = new RotationGeant(0.0, 0.03, 0.0);
		Hep3Vector vector = new BasicHep3Vector(x, y, z);
		return rotation.rotated(vector);
	}

}
