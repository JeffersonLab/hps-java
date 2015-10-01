/**
 * 
 */
package org.hps.evio;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.hps.record.svt.SvtHeaderDataInfo;
import org.lcsim.event.EventHeader;
import org.lcsim.lcio.LCIOUtil;

/**
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class AugmentedSvtEvioReader extends SvtEvioReader {

    /**
     * 
     */
    public AugmentedSvtEvioReader() {
     super();
    }
    
    
    
    
    @Override
    protected SvtHeaderDataInfo extractSvtHeader(int num, int[] data) {
        // Extract the header information
        int svtHeader = SvtEvioUtils.getSvtHeader(data);
        // Extract the tail information
        int svtTail = SvtEvioUtils.getSvtTail(data);
        return new SvtHeaderDataInfo(num, svtHeader, svtTail);
        
    }

    @Override
    protected void checkSvtHeaderData(SvtHeaderDataInfo header) throws SvtEvioHeaderException {
        int tail = header.getTail();
        if(logger.getLevel().intValue() >= Level.FINE.intValue()) {
            logger.fine("checkSvtHeaderData tail " + tail + "( " + Integer.toHexString(tail) + " )");
            logger.fine("checkSvtHeaderData errorbit   " +  Integer.toHexString(SvtEvioUtils.getSvtTailSyncErrorBit(tail)));
            logger.fine("checkSvtHeaderData OFerrorbit " +  Integer.toHexString(SvtEvioUtils.getSvtTailOFErrorBit(tail)));
            logger.fine("checkSvtHeaderData skipcount  " +  Integer.toHexString(SvtEvioUtils.getSvtTailMultisampleSkipCount(tail)));
        }
        if( SvtEvioUtils.getSvtTailSyncErrorBit(tail) != 0) {
            throw new SvtEvioHeaderApvBufferAddressException("This SVT header had a SyncError");
        }
        else if( SvtEvioUtils.getSvtTailOFErrorBit(tail) != 0) {
            throw new SvtEvioHeaderOFErrorException("This header had a OverFlowError");
        }
        else if( SvtEvioUtils.getSvtTailMultisampleSkipCount(tail) != 0) {
            throw new SvtEvioHeaderSkipCountException("This header had a skipCount " + SvtEvioUtils.getSvtTailMultisampleSkipCount(tail) + " error");
        }
        logger.fine("checkSvtHeaderData passed all I guess");
    }
    
    @Override
    protected void addSvtHeadersToEvents(List<SvtHeaderDataInfo> headers, EventHeader lcsimEvent) {
    // Turn on 64-bit cell ID.
    int flag = LCIOUtil.bitSet(0, 31, true);
    // Add the collection of raw hits to the LCSim event
    lcsimEvent.put(SVT_HEADER_COLLECTION_NAME, headers, SvtHeaderDataInfo.class, flag);

}

    @Override
    protected void checkSvtSampleCount(int sampleCount, SvtHeaderDataInfo headerData) throws SvtEvioHeaderException {
        if( sampleCount != SvtEvioUtils.getSvtTailMultisampleCount(headerData.getTail())*4)
            throw new SvtEvioHeaderException("multisample count is not consistent with bank size.");
    }

    @Override
    protected void setMultiSampleHeaders(SvtHeaderDataInfo headerData, int n, int[] multisampleHeaders) {
        //copy out the headers that are non-zero
        int[] vals = new int[n];
        System.arraycopy(multisampleHeaders, 0, vals, 0, n);
        //logger.info("setMultiSampleHeaders: adding " + vals.length + " multisample headers");
        headerData.setMultisampleHeaders(vals);
    }
    
    @Override
    protected void extractMultisampleHeaderTail(int[] multisample, int index, int[] multisampleHeaders) {
        //logger.fine("extractMultisampleHeaderTail: index " + index);
        if( SvtEvioUtils.isMultisampleHeader(multisample) && !SvtEvioUtils.isMultisampleTail(multisample))
            multisampleHeaders[index] = SvtEvioUtils.getMultisampleTailWord(multisample);
         //else 
         //   logger.fine("extractMultisampleHeaderTail: this is a NOT multisample header at index " + index);
        
    }
    
    @Override
    protected int extractMultisampleHeaderData(int[] samples, int index, int[] multisampleHeaderData) {
        logger.fine("extractMultisampleHeaderData: index " + index);
        if( SvtEvioUtils.isMultisampleHeader(samples) && !SvtEvioUtils.isMultisampleTail(samples) ) {
            logger.fine("extractMultisampleHeaderData: this is a multisample header so add the words to index " + index);
            System.arraycopy(samples, 0, multisampleHeaderData, index, samples.length);
            return samples.length;
        } else {
            logger.fine("extractMultisampleHeaderData: this is a NOT multisample header ");
            return 0;
        }
    }
    
    @Override
    protected void checkSvtHeaders(List<SvtHeaderDataInfo> headers) throws SvtEvioHeaderException {
        logger.fine("check " + headers.size() + " headers  ");
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
            logger.fine("checking header: " + headerDataInfo.toString());
            
            
            // Check the header data
            this.checkSvtHeaderData(headerDataInfo);
            
            int nMultisampleHeaders = headerDataInfo.getNumberOfMultisampleHeaders();
            for(int iMultisampleHeader = 0; iMultisampleHeader < nMultisampleHeaders; iMultisampleHeader++) {
                logger.fine("iMultisampleHeader " + iMultisampleHeader);
                
                multisampleHeader = SvtHeaderDataInfo.getMultisampleHeader(iMultisampleHeader, headerDataInfo);
                
                // get multisample tail error bit and check it
                multisampleHeaderTailerrorBit = SvtEvioUtils.getErrorBitFromMultisampleHeader(SvtEvioUtils.getMultisampleTailWord(multisampleHeader));
                logger.fine("multisampleHeaderTailerrorBit " + multisampleHeaderTailerrorBit + " from multisampleHeaderTail " + SvtEvioUtils.getMultisampleTailWord(multisampleHeader) + " ( " + Integer.toHexString( SvtEvioUtils.getMultisampleTailWord(multisampleHeader) ) + " )");
                if( multisampleHeaderTailerrorBit != 0) 
                    throw new SvtEvioHeaderMultisampleErrorBitException("This multisampleheader had the error bit set.");
                
                
                // get buffer addresses
                bufAddresses = SvtEvioUtils.getApvBufferAddresses(multisampleHeader);
                
                // get frame counts
                frameCounts = SvtEvioUtils.getApvFrameCount(multisampleHeader);


                if( bufAddresses.length != 6)
                    throw new SvtEvioHeaderApvBufferAddressException("Invalid number of APV buffer addresses.");

                if( frameCounts.length != 6)
                    throw new SvtEvioHeaderApvFrameCountException("Invalid number of APV frame counts.");

                if(logger.getLevel().intValue() >= Level.FINE.intValue()) {
                    for (int i=0; i<bufAddresses.length; ++i) {
                        logger.fine("buffer address " + i + "  " + bufAddresses[i]  + " ( " + Integer.toHexString( bufAddresses[i]) + " )");
                    }
                    for (int i=0; i<frameCounts.length; ++i) {
                        logger.fine("frame count   " + i + "  " + frameCounts[i]  + " ( " + Integer.toHexString( frameCounts[i]) + " )");
                    }
                }

                // Get a reference for comparison 
                if(firstHeader) {
                    
                    System.arraycopy(bufAddresses, 0, bufferAddresses, 0, bufAddresses.length); 
                    
                    System.arraycopy(frameCounts, 0, firstFrameCounts, 0, frameCounts.length); 
                                        
                    firstHeader = false;
                }
                else {
                    
                    // Check that apv buffer addresses match
                    if( !Arrays.equals(bufferAddresses, bufAddresses)) {
                        for (int i=0; i<bufAddresses.length; ++i) {
                            logger.info("buffer address " + i + "  " + bufAddresses[i]  + " ( " + Integer.toHexString( bufAddresses[i]) + " )");
                        }
                        for (int i=0; i<bufferAddresses.length; ++i) {
                            logger.info("ref buffer address " + i + "  " + bufferAddresses[i]  + " ( " + Integer.toHexString( bufferAddresses[i]) + " )");
                        }
                        throw new SvtEvioHeaderApvBufferAddressException("The APV buffer addresses in this event do not match!");
                    }
                    
                    // Check that apv frame count match
                    if( !Arrays.equals(firstFrameCounts, frameCounts)) {
                        for (int i=0; i<frameCounts.length; ++i) {
                            logger.info("frame count " + i + "  " + frameCounts[i]  + " ( " + Integer.toHexString( frameCounts[i]) + " )");
                        }
                        for (int i=0; i<firstFrameCounts.length; ++i) {
                            logger.info("ref frame count " + i + "  " + firstFrameCounts[i]  + " ( " + Integer.toHexString( firstFrameCounts[i]) + " )");
                        }
                        throw new SvtEvioHeaderApvFrameCountException("The APV frame counts in this event do not match!");
                    }
                }

                // Check that the APV frame counts are incrementing
                // remember to take into account the 2-bit rollover (duh!)
                
                count = -1;
                for (int i=0; i<frameCounts.length; ++i) {
                    logger.fine("frame count " + i + "  " + frameCounts[i]  + " ( " + Integer.toHexString( frameCounts[i]) + " )");

                    if( frameCounts[i] > 15 ) 
                        throw new SvtEvioHeaderApvFrameCountException("Frame count " + frameCounts[i] + " is larger than 2-bit number?");

                    if( (count < 15 && frameCounts[i] < count) || ( count == 15 && frameCounts[i] != 0 ) ) {
                        //logger.severe("Frame count " + frameCounts[i] + " was not increasing compared to previous " + count + " for APV " + i);
                        throw new SvtEvioHeaderApvFrameCountException("Frame count " + frameCounts[i] + " was not increasing compared to previous " + count + " for index " + i + " ( tailword: " + Integer.toHexString( SvtEvioUtils.getMultisampleTailWord(multisampleHeader) ) + " )");
                    }
                    count = frameCounts[i];
                }

                
                // check if there was any read errors
                readError = SvtEvioUtils.getApvReadErrors(multisampleHeader);
                
                for (int i=0; i<readError.length; ++i) {
                    logger.fine("i " + i + "  " + readError[i]  + " ( " + Integer.toHexString( readError[i]) + " )");
                    if( readError[i] != 1)  // active low
                        throw new SvtEvioHeaderApvReadErrorException("Read error for apv " + i);
                }
                
                
            } // multisampleheaders
        }
        
    }
    
}
