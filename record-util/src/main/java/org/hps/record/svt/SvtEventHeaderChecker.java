/**
 * 
 */
package org.hps.record.svt;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.hps.record.svt.SvtEvioExceptions.*;


/**
 * 
 * Static functions to check integrity of the SVT header data.
 * 
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class SvtEventHeaderChecker {

    private static Logger LOGGER = Logger.getLogger(SvtEventHeaderChecker.class.getPackage().getName());
    
        /**
     * Check the integrity of the SVT header information.
     * @param headers - headers to check
         * @throws SvtEvioHeaderApvBufferAddressException 
         * @throws SvtEvioHeaderApvFrameCountException 
         * @throws SvtEvioHeaderMultisampleErrorBitException 
         * @throws SvtEvioHeaderApvReadErrorException 
         * @throws SvtEvioHeaderSkipCountException 
         * @throws SvtEvioHeaderOFErrorException 
         * @throws SvtEvioHeaderSyncErrorException 
     * @throws SvtEvioHeaderException
     */
    public static void checkSvtHeaders(List<SvtHeaderDataInfo> headers) throws SvtEvioHeaderApvBufferAddressException, SvtEvioHeaderApvFrameCountException, SvtEvioHeaderMultisampleErrorBitException, SvtEvioHeaderApvReadErrorException, SvtEvioHeaderSyncErrorException, SvtEvioHeaderOFErrorException, SvtEvioHeaderSkipCountException  {
        LOGGER.fine("check " + headers.size() + " headers  ");
        int[] bufferAddresses = new int[6];
        int[] firstFrameCounts = new int[6];
        boolean firstHeader = true;
        int[] multisampleHeader;
        int[] bufAddresses;
        int[] frameCounts;
        int[] readError;
        int count;
        int multisampleHeaderTailerrorBit;
        for( SvtHeaderDataInfo headerDataInfo : headers ) {
            LOGGER.fine("checking header: " + headerDataInfo.toString());


            // Check the multisample header information            
            int nMultisampleHeaders = headerDataInfo.getNumberOfMultisampleHeaders();
            for(int iMultisampleHeader = 0; iMultisampleHeader < nMultisampleHeaders; iMultisampleHeader++) {
                LOGGER.fine("iMultisampleHeader " + iMultisampleHeader);

                multisampleHeader = SvtHeaderDataInfo.getMultisampleHeader(iMultisampleHeader, headerDataInfo);

                // get multisample tail error bit
                multisampleHeaderTailerrorBit = SvtEvioUtils.getErrorBitFromMultisampleHeader(SvtEvioUtils.getMultisampleTailWord(multisampleHeader));
                
                // get buffer addresses
                bufAddresses = SvtEvioUtils.getApvBufferAddresses(multisampleHeader);

                // get frame counts
                frameCounts = SvtEvioUtils.getApvFrameCount(multisampleHeader);

                // check if there was any read errors
                readError = SvtEvioUtils.getApvReadErrors(multisampleHeader);

                if( bufAddresses.length != 6)
                    throw new SvtEvioHeaderApvBufferAddressException("Invalid number of APV buffer addresses.");

                if( frameCounts.length != 6)
                    throw new SvtEvioHeaderApvFrameCountException("Invalid number of APV frame counts.");

                if( readError.length != 6)
                    throw new SvtEvioHeaderApvFrameCountException("Invalid number of read errors.");

                // Check for error bit
                if( multisampleHeaderTailerrorBit != 0) {
                    throw new SvtEvioHeaderMultisampleErrorBitException("A multisample header error bit was set for " + 
                                                                        getMultisampleDebugString(headerDataInfo, SvtEvioUtils.getMultisampleTailWord(multisampleHeader)) + 
                                                                        getDebugString(bufAddresses, frameCounts, readError)); 
                }

                // print debug
                LOGGER.fine(getMultisampleDebugString(headerDataInfo, SvtEvioUtils.getMultisampleTailWord(multisampleHeader)) + 
                        getDebugString(bufAddresses, frameCounts, readError));

                // Get a reference for comparison 
                if(firstHeader) {

                    System.arraycopy(bufAddresses, 0, bufferAddresses, 0, bufAddresses.length); 

                    System.arraycopy(frameCounts, 0, firstFrameCounts, 0, frameCounts.length); 

                    firstHeader = false;
                }
                else {

                    // Check that apv buffer addresses match
                    if( !Arrays.equals(bufferAddresses, bufAddresses)) {
                        throw new SvtEvioHeaderApvBufferAddressException("The APV buffer addresses in this event do not match " + 
                                                                            getMultisampleDebugString(headerDataInfo, SvtEvioUtils.getMultisampleTailWord(multisampleHeader)) +
                                                                            getDebugString(bufAddresses, frameCounts, readError) +
                                                                            " compared to " +
                                                                            getDebugString(bufferAddresses, firstFrameCounts, readError)); 
                    }

                    // Check that apv frame count match
                    if( !Arrays.equals(firstFrameCounts, frameCounts)) {
                        throw new SvtEvioHeaderApvFrameCountException("The APV frame counts in this event do not match " + 
                                getMultisampleDebugString(headerDataInfo, SvtEvioUtils.getMultisampleTailWord(multisampleHeader)) +
                                getDebugString(bufAddresses, frameCounts, readError) +
                                " compared to " +
                                getDebugString(bufferAddresses, firstFrameCounts, readError)); 
                    }
                }

                // Check that the APV frame counts are incrementing
                // remember to take into account the 2-bit rollover (duh!)

                count = -1;
                for (int iFrame=0; iFrame<frameCounts.length; ++iFrame) {
                    LOGGER.fine("frame count " + iFrame + "  " + frameCounts[iFrame]  + " ( " + Integer.toHexString( frameCounts[iFrame]) + " )");

                    if( frameCounts[iFrame] > 15  ||  (count < 15 && frameCounts[iFrame] < count) || ( count == 15 && frameCounts[iFrame] != 0 ) ) {
                        throw new SvtEvioHeaderApvFrameCountException("The APV frame counts in this events are invalid " + 
                                getMultisampleDebugString(headerDataInfo, SvtEvioUtils.getMultisampleTailWord(multisampleHeader)) +
                                getDebugString(bufAddresses, frameCounts, readError)); 
                    }
                    count = frameCounts[iFrame];
                }

                for (int iReadError=0; iReadError<readError.length; ++iReadError) {
                    LOGGER.fine("read error " + iReadError + "  " + readError[iReadError]  + " ( " + Integer.toHexString( readError[iReadError]) + " )");
                    if( readError[iReadError] != 1)  {// active low
                        throw new SvtEvioHeaderApvReadErrorException("Read error occurred " + 
                                getMultisampleDebugString(headerDataInfo, SvtEvioUtils.getMultisampleTailWord(multisampleHeader)) +
                                getDebugString(bufAddresses, frameCounts, readError)); 
                    }
                }


            } // multisampleheaders
            
            
            // Check the header data
            // Parts of this get its input from the multisample which has already been checked
            // therefore I don't expect these to happen.
            checkSvtHeaderData(headerDataInfo);

            

        }

    }
    
    public static void checkSvtHeaderData(SvtHeaderDataInfo header) throws SvtEvioHeaderSyncErrorException, SvtEvioHeaderOFErrorException, SvtEvioHeaderSkipCountException {
        int tail = header.getTail();
        LOGGER.fine("checkSvtHeaderData tail " + tail + "( " + Integer.toHexString(tail) + " ) " +
                                                 " errorbit   " +  Integer.toHexString(SvtEvioUtils.getSvtTailSyncErrorBit(tail)) +
                                                 " OFerrorbit " +  Integer.toHexString(SvtEvioUtils.getSvtTailOFErrorBit(tail)) + 
                                                 " checkSvtHeaderData skipcount  " +  Integer.toHexString(SvtEvioUtils.getSvtTailMultisampleSkipCount(tail)));
        if( SvtEvioUtils.getSvtTailSyncErrorBit(tail) != 0) {
            throw new SvtEvioExceptions.SvtEvioHeaderSyncErrorException("This SVT header had a SyncError " + header.toString());
        }
        else if( SvtEvioUtils.getSvtTailOFErrorBit(tail) != 0) {
            throw new SvtEvioHeaderOFErrorException("This header had a OverFlowError " + header.toString());
        }
        else if( SvtEvioUtils.getSvtTailMultisampleSkipCount(tail) != 0) {
            throw new SvtEvioHeaderSkipCountException("This header had a skipCount " + SvtEvioUtils.getSvtTailMultisampleSkipCount(tail) + " error " + header.toString());
        }
        LOGGER.fine("checkSvtHeaderData passed all I guess");
    }
    
    
    private static String getMultisampleDebugString(SvtHeaderDataInfo headerDataInfo, int multisampleHeaderTailWord) {
        String s = " header" + headerDataInfo.toString() +
                " multisample: feb " + SvtEvioUtils.getFebIDFromMultisampleTail(multisampleHeaderTailWord) + 
                " hybrid " + SvtEvioUtils.getFebHybridIDFromMultisampleTail(multisampleHeaderTailWord) + 
                " apv " + SvtEvioUtils.getApvFromMultisampleTail(multisampleHeaderTailWord);
        return s;
    }
    
    private static String getDebugString(int[] bufAddresses, int[] frameCounts, int[] readError ) {
        String s = "";
        for (int i=0; i<bufAddresses.length; ++i)
            s+="\nbuffer address " + i + "  " + bufAddresses[i]  + " ( " + Integer.toHexString( bufAddresses[i]) + " )";
        for (int i=0; i<frameCounts.length; ++i) 
            s+="\nframe count    " + i + "  " + frameCounts[i]  + " ( " + Integer.toHexString( frameCounts[i]) + " )";
        for (int i=0; i<readError.length; ++i) 
            s+="\nread error     " + i + "  " + readError[i]  + " ( " + Integer.toHexString( readError[i]) + " )";
        return s;
    }
    
    /**
     * Private construction to avoid class being instantiated
     */
    private SvtEventHeaderChecker() {
    }

    

    
}
