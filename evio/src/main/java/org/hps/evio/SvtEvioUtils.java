package org.hps.evio;

/**
 *	A set of static utility methods used to decode SVT data.
 * 
 * 	@author Omar Moreno <omoreno1@ucsc.edu>
 *	@date November 20, 2014
 */
public class SvtEvioUtils {

	//-----------------//
	//--- Constants ---//
	//-----------------//
	
	private static final int TOTAL_SAMPLES = 6;
    public static final int  SAMPLE_MASK = 0xFFFF;
	
	//--- Test Run ---//
	private static final int TEST_RUN_SAMPLE_HEADER_INDEX = 0; 
    public static final int  FPGA_MASK = 0xFFFF;
    public static final int  HYBRID_MASK = 0x3;
    public static final int  TEST_RUN_CHANNEL_MASK = 0x7F;

    //--- Engineering Run ---//
    private static final int ENG_RUN_SAMPLE_HEADER_INDEX = 3; 
    private static final int FEB_MASK = 0xFF;
    private static final int FEB_HYBRID_MASK = 0x3;
    private static final int ENG_RUN_CHANNEL_MASK = 0x7F; 
    
    /**
     * 	Extract and return the FPGA ID associated with the samples.
     * 	Note: This method should only be used when looking at test run data.
     * 
	 *	@param data - sample block of data
     * 	@return An FPGA ID in the range 0-7
     */
	public static int getFpgaID(int[] data) { 
		return data[TEST_RUN_SAMPLE_HEADER_INDEX] & FPGA_MASK; 
	}

    /**
     * 	Extract and return the hybrid ID associated with the samples 
     * 	Note: This method should only be used when looking at test run data.
     * 
	 * 	@param data : sample block of data
     * 	@return A hybrid number in the range 0-2
     */
    public static int getHybridID(int[] data) {
        return (data[TEST_RUN_SAMPLE_HEADER_INDEX] >>> 28) & HYBRID_MASK;
    }
	
    /**
     *	Extract and return the channel number associated with the samples
     * 	Note: This method should only be used when looking at test run data.
     * 
	 * 	@param data : sample block of data
     * 	@return A channel number in the range 0-127
     */
    public static int getTestRunChannelNumber(int[] data) {
        return (data[TEST_RUN_SAMPLE_HEADER_INDEX] >>> 16) & TEST_RUN_CHANNEL_MASK;
    }
    
    /**
     *	Extract and return the front end board (FEB) ID associated with the
     *	samples
     *	
	 * 	@param data : sample block of data
	 * 	@return A FEB ID in the range 0-10
     */
    public static int getFebID(int[] data) { 
    	return (data[ENG_RUN_SAMPLE_HEADER_INDEX] >> 8) & FEB_MASK; 
    }
    
    /**
     * 	Extract and return the front end board (FEB) hybrid ID associated with 
     * 	the samples
	 *
	 * 	@param data : sample block of data
	 * 	@return A FEB hybrid ID in the range 0-3
     */
    public static int getFebHybridID(int[] data) { 
    	return (data[ENG_RUN_SAMPLE_HEADER_INDEX] >> 26) & FEB_HYBRID_MASK; 
    }

    /**
     *	Extract and return the channel number associated with the samples
     * 
	 * 	@param data : sample block of data
     * 	@return A channel number in the range 0-127
     */
    public static int getChannelNumber(int[] data) {
        return (data[ENG_RUN_SAMPLE_HEADER_INDEX] >>> 16) & ENG_RUN_CHANNEL_MASK;
    }
    
    /**
     * 	Extract and return the nth SVT sample.
     * 
     * 	@param sampleN : The sample number of interest. Valid values are 0 to 5
	 * 	@param data : sample block of data
     * 	@throws RuntimeException if the sample number is out of range
     * 	@return ADC value of the nth sample
     * 
     */
    public static int getSample(int sampleN, int[] data) {

        switch (sampleN) {
            case 0:
                return data[1] & SAMPLE_MASK;
            case 1:
                return (data[1] >>> 16) & SAMPLE_MASK;
            case 2:
                return data[2] & SAMPLE_MASK;
            case 3:
                return (data[2] >>> 16) & SAMPLE_MASK;
            case 4:
                return data[3] & SAMPLE_MASK;
            case 5:
                return (data[3] >>> 16) & SAMPLE_MASK;
            default:
                throw new RuntimeException("Invalid sample number! Valid range of values for n are from 0 - 5");
        }
    }

    /**
     *	Extract and return all SVT samples as an array 
     * 
	 * 	@param data : sample block of data
     * 	@return An array containing all SVT Shaper signal samples
     */
    public static short[] getSamples(int[] data) {
        short[] samples = new short[TOTAL_SAMPLES];
        // Get all SVT Samples
        for (int sampleN = 0; sampleN < TOTAL_SAMPLES; sampleN++) {
            samples[sampleN] = (short) getSample(sampleN, data);
        }
        return samples;
    }

    /**
     *	Private constructor to prevent the class from being instantiated.
     */
    private SvtEvioUtils(){}; 
}
