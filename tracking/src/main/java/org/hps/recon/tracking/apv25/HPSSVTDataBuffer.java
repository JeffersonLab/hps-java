
package org.hps.recon.tracking.apv25;

import static org.hps.conditions.deprecated.HPSSVTConstants.SVT_TOTAL_FPGAS;
import static org.hps.conditions.deprecated.HPSSVTConstants.TEMP_MASK;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Constants
import org.hps.recon.tracking.FpgaData;
import org.hps.recon.tracking.HPSSVTData;

/**
 *
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id: HPSSVTDataBuffer.java,v 1.1 2013/03/15 21:05:28 meeg Exp $ 
 */
public class HPSSVTDataBuffer {
    
    // Map the FPGA to the data emerging from it
    private Map<Integer, List<Integer>> fpgaToData = new HashMap<Integer, List<Integer>>();
	
    // Singleton
    private static final HPSSVTDataBuffer instance = new HPSSVTDataBuffer();
    private static int eventNumber = 0;
    
    int[] header = new int[6];
    int temp = FpgaData.temperatureToInt(23.0); // C
    
    boolean debug = false;
        
    /**
     * Default constructor; Set to private to prevent instantiation
     */
    private HPSSVTDataBuffer(){
        //
        for(int fpgaNumber = 0; fpgaNumber <= SVT_TOTAL_FPGAS; fpgaNumber++) 
        	fpgaToData.put(fpgaNumber, new ArrayList<Integer>());
    }
    
    /**
     * Add data to SVT buffer
     * 
     * @param svtData : List of SVT data packets
     * @param fpga : FPGA from which the data emerges from
     */
    public static void addToBuffer(List<HPSSVTData> svtData, int fpga){
        // If the FPGA data block is empty, add header information and data, otherwise
    	// just add the data
    	instance.encapsulateSVTData(svtData, fpga);        
    }
    
    /**
     * Readout data stored in the SVT buffer
     * 
     * @param fpga : FPGA from which data is to be read from
     * @return data : An FPGA data packet 
     *
     */
    public static int[] readoutBuffer(int fpga){
    	// Add the event number to the beginning
    	instance.addEventNumber(fpga);
    	
    	// Add the tail to the data
    	instance.addTail(fpga);
    	
    	// Copy the data in the map so that the buffer can be cleared
    	int[] data = new int[instance.fpgaToData.get(fpga).size()]; 
    	int index = 0;
    	for(Integer datum : instance.fpgaToData.get(fpga)){
    		data[index] = datum;
    		index++;
    	}
    	
    	// Clear the buffer
        instance.fpgaToData.get(fpga).clear();
        
        // Return the 
        return data;
    }
    
    /**
     * Encapsulate SVT data by FPGA
     * 
     * @param svtData : List of SVT data packets
     * @param fpga : FPGA from which the data emerges from
     */
    private void encapsulateSVTData(List<HPSSVTData> svtData, int fpga){
    	// Ignore FPGA 7 for now 
    	if(fpga == 7) return;
    	
    	// If the FPGA data block is empty, add the header information and increment the event number
    	if(instance.fpgaToData.get(fpga).isEmpty()){
    		
    		// Insert the temperature information. All temperatures are currently
    		// set to 23 C
    		header[0] = (header[0] &= ~TEMP_MASK) | (temp & TEMP_MASK);
    		header[0] = (header[0] &= ~(TEMP_MASK << 16)) | ((temp & TEMP_MASK) << 16);
        
    		header[1] = (header[1] &= ~TEMP_MASK) | (temp & TEMP_MASK);
    		header[1] = (header[1] &= ~(TEMP_MASK << 16)) | ((temp & TEMP_MASK) << 16);
        
    		header[2] = (header[2] &= ~TEMP_MASK) | (temp & TEMP_MASK);
    		header[2] = (header[2] &= ~(TEMP_MASK << 16)) | ((temp & TEMP_MASK) << 16);
       
    		header[3] = (header[3] &= ~TEMP_MASK) | (temp & TEMP_MASK);
    		header[3] = (header[3] &= ~(TEMP_MASK << 16)) | ((temp & TEMP_MASK) << 16);
     
    		header[4] = (header[4] &= ~TEMP_MASK) | (temp & TEMP_MASK);
    		header[4] = (header[4] &= ~(TEMP_MASK << 16)) | ((temp & TEMP_MASK) << 16);
        
    		header[5] = (header[5] &= ~TEMP_MASK) | (temp & TEMP_MASK);
    		header[5] = (header[5] &= ~(TEMP_MASK << 16)) | ((temp & TEMP_MASK) << 16);
        
    		for(int index = 0; index < header.length; index++) fpgaToData.get(fpga).add(header[index]);
    	
    		eventNumber++;
    	}

        // Add all samples emerging from this FPGA
        if(!svtData.isEmpty()){
        	for(HPSSVTData svtDatum : svtData){ 
            	if(debug){
            		System.out.println("FPGA: " + svtDatum.getFPGAAddress() + " Hybrid: " + svtDatum.getHybridNumber() + " APV: " 
            							+ svtDatum.getAPVNumber() + " Channel: " + svtDatum.getChannelNumber());
            	}
        		for(int index = 0; index < svtDatum.getData().length; index++){
        			fpgaToData.get(fpga).add(svtDatum.getData()[index]);
        		}
        	}
        }
    }
    
    /**
     * Add a tail to the FPGA data packet.  In real data, this may be 
     * non-zero which would indicate an error.
     * 
     * @param fpga : FPGA from which the data emerges from
     */
    private void addTail(int fpga){
    	
    	// For now just make it zero
    	instance.fpgaToData.get(fpga).add(0);
    }
    
    /**
     * Add an SVT event number to the top of the FPGA data packet.
     * 
     * @param fpga : FPGA from which the data emerges from
     */
    private void addEventNumber(int fpga){
    	
    	instance.fpgaToData.get(fpga).add(0, eventNumber);
    }
}
