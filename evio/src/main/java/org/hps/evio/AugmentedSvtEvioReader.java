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


    protected void checkSvtHeaderData(SvtHeaderDataInfo header) throws SvtEvioHeaderException {
        int tail = header.getTail();
        if(logger.getLevel().intValue() >= Level.FINE.intValue()) {
            logger.fine("checkSvtHeaderData tail " + tail + "( " + Integer.toHexString(tail) + " )");
            logger.fine("checkSvtHeaderData errorbit   " +  Integer.toHexString(SvtEvioUtils.getSvtTailSyncErrorBit(tail)));
            logger.fine("checkSvtHeaderData OFerrorbit " +  Integer.toHexString(SvtEvioUtils.getSvtTailOFErrorBit(tail)));
            logger.fine("checkSvtHeaderData skipcount  " +  Integer.toHexString(SvtEvioUtils.getSvtTailMultisampleSkipCount(tail)));
        }
        if( SvtEvioUtils.getSvtTailSyncErrorBit(tail) != 0) {
            throw new SvtEvioHeaderSyncErrorException("This SVT header had a SyncError " + header.toString());
        }
        else if( SvtEvioUtils.getSvtTailOFErrorBit(tail) != 0) {
            throw new SvtEvioHeaderOFErrorException("This header had a OverFlowError " + header.toString());
        }
        else if( SvtEvioUtils.getSvtTailMultisampleSkipCount(tail) != 0) {
            throw new SvtEvioHeaderSkipCountException("This header had a skipCount " + SvtEvioUtils.getSvtTailMultisampleSkipCount(tail) + " error " + header.toString());
        }
        logger.fine("checkSvtHeaderData passed all I guess");
    }
    
    
    protected void addSvtHeadersToEventEventCollection(List<SvtHeaderDataInfo> headers, EventHeader lcsimEvent) {
        // Turn on 64-bit cell ID.
        int flag = LCIOUtil.bitSet(0, 31, true);
        // Add the collection of raw hits to the LCSim event
        lcsimEvent.put(SVT_HEADER_COLLECTION_NAME, headers, SvtHeaderDataInfo.class, flag);

    }

        
    
    private String getMultisampleDebugString(SvtHeaderDataInfo headerDataInfo, int multisampleHeaderTailWord) {
        String s = " header" + headerDataInfo.toString() +
                " multisample: feb " + SvtEvioUtils.getFebIDFromMultisampleTail(multisampleHeaderTailWord) + 
                " hybrid " + SvtEvioUtils.getFebHybridIDFromMultisampleTail(multisampleHeaderTailWord) + 
                " apv " + SvtEvioUtils.getApvFromMultisampleTail(multisampleHeaderTailWord);
        return s;
    }
    
    private String getDebugString(int[] bufAddresses, int[] frameCounts, int[] readError ) {
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
     * Check the integrity of the SVT header information.
     * @param headers - headers to check
     * @throws SvtEvioHeaderException
     */
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


            // Check the multisample header information            
            int nMultisampleHeaders = headerDataInfo.getNumberOfMultisampleHeaders();
            for(int iMultisampleHeader = 0; iMultisampleHeader < nMultisampleHeaders; iMultisampleHeader++) {
                logger.fine("iMultisampleHeader " + iMultisampleHeader);

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
                                                                        this.getMultisampleDebugString(headerDataInfo, SvtEvioUtils.getMultisampleTailWord(multisampleHeader)) + 
                                                                        this.getDebugString(bufAddresses, frameCounts, readError)); 
                }

                // print debug
                if(logger.getLevel().intValue() >= Level.FINE.intValue()) {
                    logger.fine(this.getMultisampleDebugString(headerDataInfo, SvtEvioUtils.getMultisampleTailWord(multisampleHeader)) + 
                                this.getDebugString(bufAddresses, frameCounts, readError));
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
                        throw new SvtEvioHeaderApvBufferAddressException("The APV buffer addresses in this event do not match " + 
                                                                            this.getMultisampleDebugString(headerDataInfo, SvtEvioUtils.getMultisampleTailWord(multisampleHeader)) +
                                                                            this.getDebugString(bufAddresses, frameCounts, readError) +
                                                                            " compared to " +
                                                                            this.getDebugString(bufferAddresses, firstFrameCounts, readError)); 
                    }

                    // Check that apv frame count match
                    if( !Arrays.equals(firstFrameCounts, frameCounts)) {
                        throw new SvtEvioHeaderApvFrameCountException("The APV frame counts in this event do not match " + 
                                this.getMultisampleDebugString(headerDataInfo, SvtEvioUtils.getMultisampleTailWord(multisampleHeader)) +
                                this.getDebugString(bufAddresses, frameCounts, readError) +
                                " compared to " +
                                this.getDebugString(bufferAddresses, firstFrameCounts, readError)); 
                    }
                }

                // Check that the APV frame counts are incrementing
                // remember to take into account the 2-bit rollover (duh!)

                count = -1;
                for (int iFrame=0; iFrame<frameCounts.length; ++iFrame) {
                    logger.fine("frame count " + iFrame + "  " + frameCounts[iFrame]  + " ( " + Integer.toHexString( frameCounts[iFrame]) + " )");

                    if( frameCounts[iFrame] > 15  ||  (count < 15 && frameCounts[iFrame] < count) || ( count == 15 && frameCounts[iFrame] != 0 ) ) {
                        throw new SvtEvioHeaderApvFrameCountException("The APV frame counts in this events are invalid " + 
                                this.getMultisampleDebugString(headerDataInfo, SvtEvioUtils.getMultisampleTailWord(multisampleHeader)) +
                                this.getDebugString(bufAddresses, frameCounts, readError)); 
                    }
                    count = frameCounts[iFrame];
                }

                for (int iReadError=0; iReadError<readError.length; ++iReadError) {
                    logger.fine("read error " + iReadError + "  " + readError[iReadError]  + " ( " + Integer.toHexString( readError[iReadError]) + " )");
                    if( readError[iReadError] != 1)  {// active low
                        throw new SvtEvioHeaderApvReadErrorException("Read error occurred " + 
                                this.getMultisampleDebugString(headerDataInfo, SvtEvioUtils.getMultisampleTailWord(multisampleHeader)) +
                                this.getDebugString(bufAddresses, frameCounts, readError)); 
                    }
                }


            } // multisampleheaders
            
            
            // Check the header data
            // Parts of this get its input from the multisample which has already been checked
            // therefore I don't expect these to happen.
            this.checkSvtHeaderData(headerDataInfo);

            

        }

    }
    
    @Override
    protected void processSvtHeaders(List<SvtHeaderDataInfo> headers, EventHeader lcsimEvent) throws SvtEvioHeaderException {
        // Check that the SVT header data is valid
        // Catch the exception locally, add stuff to the event, then throw it again
        // and handle it outside
        try {
        
            this.checkSvtHeaders(headers);
        
        } catch(SvtEvioHeaderException e) {

            // add skimming flag
            SvtEventFlagger.voidAddHeaderCheckResultToMetaData(false, lcsimEvent);
            
            // add stuff to the event meta data
            SvtEventFlagger.AddHeaderInfoToMetaData(headers, lcsimEvent);
            
            // then throw the exception again to be caught in the event builder
            throw new SvtEvioHeaderException(e);
            
        } 

        // add skimming flag - the header is OK since I would never get here otherwise
        SvtEventFlagger.voidAddHeaderCheckResultToMetaData(true, lcsimEvent);
        
        // Add SVT header data to the event
        //this.addSvtHeadersToEventEventCollection(headers, lcsimEvent);

    }

}
