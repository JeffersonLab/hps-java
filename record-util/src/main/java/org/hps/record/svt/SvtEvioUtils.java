package org.hps.record.svt;

import java.util.ArrayList;
import java.util.List;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.DataType;
import org.jlab.coda.jevio.StructureType;

/**
 * A set of static utility methods used to decode SVT data.
 */
public class SvtEvioUtils {

    //-----------------//
    //--- Constants ---//
    //-----------------//
    
    private static final int TOTAL_SAMPLES = 6;
    public static final int  SAMPLE_MASK = 0xFFFF;
    private static final int APV_HEADER_DATA_READ_ERROR_MASK = 0x1; //[0:0]
    private static final int APV_HEADER_BUFFER_ADDRESS_MASK  = 0xFF; //[8:1]
    private static final int APV_HEADER_DATA_FRAME_COUNT_MASK = 0xF; //[12:9]
    private static final int APV_HEADER_DATA_APV_NR_MASK = 0x3; //[15:13]
      
    // TODO: Move these to constants class
    public static final int APV25_PER_HYBRID = 5;
    public static final int CHANNELS_PER_APV25 = 128;
    
    //--- Test Run ---//
    private static final int TEST_RUN_SAMPLE_HEADER_INDEX = 0; 
    public static final int  FPGA_MASK = 0xFFFF;
    public static final int  HYBRID_MASK = 0x3;
    public static final int  TEST_RUN_CHANNEL_MASK = 0x7F;

    //--- Engineering Run ---//
    private static final int ENG_RUN_SAMPLE_HEADER_INDEX = 3; 
    private static final int FEB_MASK = 0xFF;
    private static final int FEB_HYBRID_MASK = 0x3;
    private static final int ENG_RUN_APV_MASK = 0x7;
    private static final int ENG_RUN_CHANNEL_MASK = 0x7F;
    private static final int ENG_RUN_APV_HEADER_MASK = 0x1;
    private static final int ENG_RUN_APV_TAIL_MASK = 0x1;
    private static final int ENG_RUN_ERROR_BIT_MASK = 0x1; 

    /**
     *  Extract and return the FPGA ID associated with the samples.
     *  Note: This method should only be used when looking at test run data.
     * 
     *  @param data - sample block of data
     *  @return An FPGA ID in the range 0-7
     */
    public static int getFpgaID(int[] data) { 
        return data[TEST_RUN_SAMPLE_HEADER_INDEX] & FPGA_MASK; 
    }

    /**
     *  Extract and return the hybrid ID associated with the samples 
     *  Note: This method should only be used when looking at test run data.
     * 
     *  @param data : sample block of data
     *  @return A hybrid number in the range 0-2
     */
    public static int getHybridID(int[] data) {
        return (data[TEST_RUN_SAMPLE_HEADER_INDEX] >>> 28) & HYBRID_MASK;
    }
    
    /**
     *  Extract and return the channel number associated with the samples
     *  Note: This method should only be used when looking at test run data.
     * 
     *  @param data : sample block of data
     *  @return A channel number in the range 0-127
     */
    public static int getTestRunChannelNumber(int[] data) {
        return (data[TEST_RUN_SAMPLE_HEADER_INDEX] >>> 16) & TEST_RUN_CHANNEL_MASK;
    }
    
    /**
     *  Extract and return the front end board (FEB) ID associated with the
     *  multisample
     *  
     *  @param multisample : a header multisample 
     *  @return A FEB ID in the range 0-10
     */
    public static int getFebIDFromMultisample(int[] multisample) { 
        return getFebIDFromMultisampleTail(getMultisampleTailWord(multisample));
    }

    /**
     *  Extract and return the front end board (FEB) ID associated with the
     *  multisample tail
     *  
     *  @param multisampleTail : a multisample header
     *  @return A FEB ID in the range 0-10
     */
    public static int getFebIDFromMultisampleTail(int multisampleTail) { 
        return (multisampleTail >>> 8) & FEB_MASK; 
    }
    
    /**
     *  Extract and return the front end board (FEB) hybrid ID associated with 
     *  the multisample
     *
     *  @param multisample : a header multisample
     *  @return A FEB hybrid ID in the range 0-3
     */
    public static int getFebHybridIDFromMultisample(int[] multisample) { 
        return getFebHybridIDFromMultisampleTail(getMultisampleTailWord(multisample));
    }
    
    /**
     *  Extract and return the front end board (FEB) hybrid ID associated with 
     *  the multisample tail 
     *
     *  @param multisampleTail : a  multisample tail
     *  @return A FEB hybrid ID in the range 0-3
     */
    public static int getFebHybridIDFromMultisampleTail(int multisampleTail) { 
        return (multisampleTail >>> 26) & FEB_HYBRID_MASK; 
    }
    

    /**
     *  Extract and return the APV ID associated with the multisample.
     *  
     *  @param multisample : a multisample of data
     *  @return An APV ID in the range of 0-4
     */
    public static int getApvFromMultiSample(int[] multisample) { 
        return getApvFromMultisampleTail(getMultisampleTailWord(multisample)); 
    }
    
    /**
     * Extract and return the APV ID from the multisample tail.
     * @param multisampleTail - tail word of a multisample
     * @return the apv id in the range of 0-4
     */
    public static int getApvFromMultisampleTail(int multisampleTail) {
        return (multisampleTail >>> 23) & ENG_RUN_APV_MASK; 
        
    }
    
    /**
     *  Extract and return the channel number associated with the samples
     * 
     *  @param data : sample block of data
     *  @return A channel number in the range 0-127
     */
    public static int getChannelNumber(int[] data) {
        return (data[ENG_RUN_SAMPLE_HEADER_INDEX] >>> 16) & ENG_RUN_CHANNEL_MASK;
    }
   
    /**
     *  Extract the physical channel number associated with the samples
     *  
     *  @param data : sample block of data
     *  @return A channel number in the range 0-639
     *  @throws RuntimeException if the physical channel number is out of range
     */
    public static int getPhysicalChannelNumber(int[] data) {

        // Extract the channel number from the data
        int channel = SvtEvioUtils.getChannelNumber(data);
        
        // Extract the APV ID from the data
        int apv = SvtEvioUtils.getApvFromMultiSample(data);
    
        // Get the physical channel number
        int physicalChannel = (APV25_PER_HYBRID - apv - 1) * CHANNELS_PER_APV25 + channel;
       
        // Check that the physical channel number is valid.  If not, throw an exception
        if (physicalChannel < 0 || physicalChannel >= APV25_PER_HYBRID * CHANNELS_PER_APV25) {
            throw new RuntimeException("Physical channel " + physicalChannel + " is outside of valid range!");
        }
        return physicalChannel;
    }
    
    /**
     *  Check if the samples are APV headers
     *  
     * 
     *  @param multisample : sample block of data
     *  @return true if the samples belong to APV headers, false otherwise
     */
    public static boolean isMultisampleHeader(int[] multisample) {
        if (((multisample[ENG_RUN_SAMPLE_HEADER_INDEX] >>> 30) & ENG_RUN_APV_HEADER_MASK) == 1) return true;
        return false;
    }
    
    /**
     * Get the multisample tail word from a multisample.
     * @param multisample - multisample of  data
     * @return the tail word
     */
    public static int getMultisampleTailWord(int[] multisample) {
        return multisample[ENG_RUN_SAMPLE_HEADER_INDEX];
    }
    
    
    /**
     *  Check if the samples are APV tails
     *  
     * 
     *  @param multisample : sample block of data
     *  @return true if the samples belong to APV tails, false otherwise
     */
    public static boolean isMultisampleTail(int[] multisample) {
        if (((multisample[ENG_RUN_SAMPLE_HEADER_INDEX] >>> 29) & ENG_RUN_APV_TAIL_MASK) == 1) return true;
        return false;
    }
    
    public static int getSvtHeader(int[] data) {
        return data[0];
    }
    
    public static int getSvtDataType(int header) {
        return (header >> 24);
    }
    
    public static int getSvtDataEventCounter(int header) {
        return (header & 0xFFFFFF);
    }
    
    public static int getSvtTail(int[] data) {
        return data[data.length-1];
    }
    
    public static int getSvtTailOFErrorBit(int tail) {
        return (tail >> 27);
    }

    public static int getSvtTailSyncErrorBit(int tail) {
        return (tail >> 26) & 0x1;
    }

    public static int getSvtTailMultisampleCount(int tail) {
        return tail & 0xFFF;
    }

    public static int getSvtTailMultisampleSkipCount(int tail) {
        return (tail >> 12) & 0xFFF;
    }

    
    /**
     *  Extract the error bit from a multisample.
     *
     *  @param multisample : multisample of data
     *  @return value of the error bit.  This is non-zero if there is an error.
     */
    public static int getMultisampleErrorBit(int[] multisample) { 
        if( !isMultisampleHeader(multisample) )
            throw new RuntimeException("Need ApvHeader multisample in order to extract the error bit from the tail word.");
        return (getMultisampleTailWord(multisample) >>> 28) & ENG_RUN_ERROR_BIT_MASK; 
    }
    
    /**
     *  Extract the error bit from the multisample header.
     *
     *  @param multisampleHeader : multisample of data
     *  @return value of the error bit.  This is non-zero if there is an error.
     */
    public static int getErrorBitFromMultisampleHeader(int multisampleHeader) { 
        return (multisampleHeader >>> 28) & ENG_RUN_ERROR_BIT_MASK; 
    }

    /**
     *  Extract and return the nth SVT sample.
     * 
     *  @param sampleN : The sample number of interest. Valid values are 0 to 5
     *  @param data : sample block of data
     *  @throws RuntimeException if the sample number is out of range
     *  @return ADC value of the nth sample
     * 
     */
    public static int getSample(int sampleN, int[] data) {

        switch (sampleN) {
            case 0:
                return data[0] & SAMPLE_MASK;
            case 1:
                return (data[0] >>> 16) & SAMPLE_MASK;
            case 2:
                return data[1] & SAMPLE_MASK;
            case 3:
                return (data[1] >>> 16) & SAMPLE_MASK;
            case 4:
                return data[2] & SAMPLE_MASK;
            case 5:
                return (data[2] >>> 16) & SAMPLE_MASK;
            default:
                throw new RuntimeException("Invalid sample number! Valid range of values for n are from 0 - 5");
        }
    }
    
    
    /**
     *  Extract and return the nth SVT APV buffer address.
     * 
     *  @param sampleN : The apv address of interest. Valid values are 0 to 5?
     *  @param data : a multisample header
     *  @throws RuntimeException if the apv address number is out of range
     *  @return address of the nth apv
     * 
     */
    public static int getApvBufferAddress(int sampleN, int[] data) {

        switch (sampleN) {
            case 0:
                return (data[0] >>> 1) & APV_HEADER_BUFFER_ADDRESS_MASK;
            case 1:
                return (data[0] >>> 17) & APV_HEADER_BUFFER_ADDRESS_MASK;
            case 2:
                return (data[1] >>> 1) & APV_HEADER_BUFFER_ADDRESS_MASK;
            case 3:
                return (data[1] >>> 17) & APV_HEADER_BUFFER_ADDRESS_MASK;
            case 4:
                return (data[2] >>> 1) & APV_HEADER_BUFFER_ADDRESS_MASK;
            case 5:
                return (data[2] >>> 17) & APV_HEADER_BUFFER_ADDRESS_MASK;
            default:
                throw new RuntimeException("Invalid address number! Valid range of values for n are from 0 - 5");
        }
    }
    
    /**
     *  Extract and return the nth SVT APV read error.
     * 
     *  @param sampleN : The apv read error of interest. Valid values are 0 to 5.
     *  @param data : a multisample header
     *  @throws RuntimeException if the apv number is out of range
     *  @return read error of the nth apv
     * 
     */
    public static int getApvReadErrors(int sampleN, int[] data) {
        switch (sampleN) {
            case 0:
                return data[0] & APV_HEADER_DATA_READ_ERROR_MASK;
            case 1:
                return (data[0] >>> 16) & APV_HEADER_DATA_READ_ERROR_MASK;
            case 2:
                return data[1] & APV_HEADER_DATA_READ_ERROR_MASK;
            case 3:
                return (data[1] >>> 16) & APV_HEADER_DATA_READ_ERROR_MASK;
            case 4:
                return data[2] & APV_HEADER_DATA_READ_ERROR_MASK;
            case 5:
                return (data[2] >>> 16) & APV_HEADER_DATA_READ_ERROR_MASK;
            default:
                throw new RuntimeException("Invalid address number! Valid range of values for n are from 0 - 5");
        }
    }
    
    /**
     *  Extract and return the nth SVT APV read error.
     * 
     *  @param sampleN : The apv read error of interest. Valid values are 0 to 5.
     *  @param data : a multisample header
     *  @throws RuntimeException if the apv number is out of range
     *  @return read error of the nth apv
     * 
     */
    public static int getApvFrameCount(int sampleN, int[] data) {
        switch (sampleN) {
            case 0:
                return (data[0] >>> 9) & APV_HEADER_DATA_FRAME_COUNT_MASK;
            case 1:
                return (data[0] >>> 25) & APV_HEADER_DATA_FRAME_COUNT_MASK;
            case 2:
                return (data[1] >>> 9) & APV_HEADER_DATA_FRAME_COUNT_MASK;
            case 3:
                return (data[1] >>> 25) & APV_HEADER_DATA_FRAME_COUNT_MASK;
            case 4:
                return (data[2] >>> 9) & APV_HEADER_DATA_FRAME_COUNT_MASK;
            case 5:
                return (data[2] >>> 25) & APV_HEADER_DATA_FRAME_COUNT_MASK;
            default:
                throw new RuntimeException("Invalid address number! Valid range of values for n are from 0 - 5");
        }
    }
    

    /**
     *  Extract and return all SVT samples as an array 
     * 
     *  @param data : sample block of data
     *  @return An array containing all SVT Shaper signal samples
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
     *  Extract and return all SVT APV buffer addresses as an array 
     * 
     *  @param multisampleHeader : multisample header
     *  @return An array containing all SVT APV buffer addresses
     */
    public static int[] getApvBufferAddresses(int[] multisampleHeader) {
        int[] samples = new int[TOTAL_SAMPLES];
        // Get all SVT Samples
        for (int sampleN = 0; sampleN < TOTAL_SAMPLES; sampleN++) {
            samples[sampleN] = getApvBufferAddress(sampleN, multisampleHeader);
        }
        return samples;
    }
    
    /**
     *  Extract and return all SVT APV read errorsas an array 
     * 
     *  @param multisampleHeader : multisample header
     *  @return An array containing all SVT APV read errors
     */
    public static int[] getApvReadErrors(int[] multisampleHeader) {
        int[] samples = new int[TOTAL_SAMPLES];
        // Get all SVT Samples
        for (int sampleN = 0; sampleN < TOTAL_SAMPLES; sampleN++) {
            samples[sampleN] = getApvReadErrors(sampleN, multisampleHeader);
        }
        return samples;
    }
    
    /**
     *  Extract and return all SVT APV read errorsas an array 
     * 
     *  @param multisampleHeader : multisample header
     *  @return An array containing all SVT APV read errors
     */
    public static int[] getApvFrameCount(int[] multisampleHeader) {
        int[] samples = new int[TOTAL_SAMPLES];
        // Get all SVT Samples
        for (int sampleN = 0; sampleN < TOTAL_SAMPLES; sampleN++) {
            samples[sampleN] = getApvFrameCount(sampleN, multisampleHeader);
        }
        return samples;
    }

    /**
     *  Retrieve all the banks in an event that match the given tag in their
     *  header and are not data banks. 
     *
     *  @param evioEvent : The event/bank being queried
     *  @param tag : The tag to match
     *  @return A collection of all bank structures that pass the filter 
     *          provided by the event
     */
    public static List<BaseStructure> getROCBanks(BaseStructure evioEvent, int minROCTag, int maxROCTag) {
        List<BaseStructure> matchingBanks = new ArrayList<BaseStructure>();
        if (evioEvent.getChildCount() > 0) {
            for (BaseStructure childBank : evioEvent.getChildrenList()) {
                if (childBank.getStructureType() == StructureType.BANK
                        && childBank.getHeader().getDataType() == DataType.ALSOBANK
                        && childBank.getHeader().getTag() >= minROCTag
                        && childBank.getHeader().getTag() <= maxROCTag) {
                    matchingBanks.add(childBank);
                }
            }
        }
        return matchingBanks;
    }

    public static List<BaseStructure> getDataBanks(BaseStructure evioEvent, int minROCTag, int maxROCTag, int minDataTag, int maxDataTag) {
        List<BaseStructure> rocBanks = getROCBanks(evioEvent, minROCTag, maxROCTag);
        List<BaseStructure> matchingBanks = new ArrayList<BaseStructure>();
        for (BaseStructure rocBank : rocBanks) {
            if (rocBank.getChildCount() > 0) {
                for (BaseStructure childBank : rocBank.getChildrenList()) {
                    if (childBank.getHeader().getTag() >= minDataTag
                            && childBank.getHeader().getTag() <= maxDataTag) {
                        matchingBanks.add(childBank);
                    }
                }
            }
        }
        return matchingBanks;
    }
    
    public static List<int[]> getMultisamples(int[] data, int sampleCount, int headerLength) {
        List<int[]> sampleList = new ArrayList<int[]>();
        // Loop through all of the samples and make hits
        for (int samplesN = 0; samplesN < sampleCount; samplesN += 4) {
            int[] samples = new int[4];
            System.arraycopy(data, headerLength + samplesN, samples, 0, samples.length);
            sampleList.add(samples);
        }
        return sampleList;
    }

    /**
     *  Private constructor to prevent the class from being instantiated.
     */
    private SvtEvioUtils(){}; 
}
