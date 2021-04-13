package org.hps.record.svt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hps.record.svt.SvtEvioExceptions.*;

/**
 * Static functions to check integrity of the SVT header data.
 */
public class SvtEventHeaderChecker {

    private static Logger LOGGER = Logger.getLogger(SvtEventHeaderChecker.class.getPackage().getName());
    static {
        LOGGER.setLevel(Level.INFO);
    }
    
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
    //public static List<SvtEvioHeaderException>  checkSvtHeaders(List<SvtHeaderDataInfo> headers) throws SvtEvioHeaderApvBufferAddressException, SvtEvioHeaderApvFrameCountException, SvtEvioHeaderMultisampleErrorBitException, SvtEvioHeaderApvReadErrorException, SvtEvioHeaderSyncErrorException, SvtEvioHeaderOFErrorException, SvtEvioHeaderSkipCountException  {
    public static List<SvtEvioHeaderException>  checkSvtHeaders(List<SvtHeaderDataInfo> headers) {
          
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
        // create a list to hold all active exceptions
        List<SvtEvioHeaderException> exceptions = new ArrayList<SvtEvioHeaderException>();
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
                    exceptions.add(new SvtEvioHeaderApvBufferAddressException("Invalid number of APV buffer addresses."));

                if( frameCounts.length != 6)
                    exceptions.add( new SvtEvioHeaderApvFrameCountException("Invalid number of APV frame counts."));

                if( readError.length != 6)
                    exceptions.add( new SvtEvioHeaderApvFrameCountException("Invalid number of read errors."));

                // Check for error bit
                if( multisampleHeaderTailerrorBit != 0) {
                    exceptions.add( new SvtEvioHeaderMultisampleErrorBitException("A multisample header error bit was set for " + 
                                                                        getMultisampleDebugString(headerDataInfo, SvtEvioUtils.getMultisampleTailWord(multisampleHeader)) + 
                                                                        getDebugString(bufAddresses, frameCounts, readError))); 
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
                        exceptions.add( new SvtEvioHeaderApvBufferAddressException("The APV buffer addresses in this event do not match " + 
                                                                            getMultisampleDebugString(headerDataInfo, SvtEvioUtils.getMultisampleTailWord(multisampleHeader)) +
                                                                            getDebugString(bufAddresses, frameCounts, readError) +
                                                                            " compared to " +
                                                                            getDebugString(bufferAddresses, firstFrameCounts, readError))); 
                    }

                    // Check that apv frame count match
                    if( !Arrays.equals(firstFrameCounts, frameCounts)) {
                        exceptions.add( new SvtEvioHeaderApvFrameCountException("The APV frame counts in this event do not match " + 
                                getMultisampleDebugString(headerDataInfo, SvtEvioUtils.getMultisampleTailWord(multisampleHeader)) +
                                getDebugString(bufAddresses, frameCounts, readError) +
                                " compared to " +
                                getDebugString(bufferAddresses, firstFrameCounts, readError))); 
                    }
                }

                // Check that the APV frame counts are incrementing
                // remember to take into account the 2-bit rollover (duh!)

                count = -1;
                for (int iFrame=0; iFrame<frameCounts.length; ++iFrame) {
                    LOGGER.fine("frame count " + iFrame + "  " + frameCounts[iFrame]  + " ( " + Integer.toHexString( frameCounts[iFrame]) + " )");

                    if( frameCounts[iFrame] > 15  ||  (count < 15 && frameCounts[iFrame] < count) || ( count == 15 && frameCounts[iFrame] != 0 ) ) {
                        exceptions.add( new SvtEvioHeaderApvFrameCountException("The APV frame counts in this events are invalid " + 
                                getMultisampleDebugString(headerDataInfo, SvtEvioUtils.getMultisampleTailWord(multisampleHeader)) +
                                getDebugString(bufAddresses, frameCounts, readError))); 
                    }
                    count = frameCounts[iFrame];
                }

                for (int iReadError=0; iReadError<readError.length; ++iReadError) {
                    LOGGER.fine("read error " + iReadError + "  " + readError[iReadError]  + " ( " + Integer.toHexString( readError[iReadError]) + " )");
                    if( readError[iReadError] != 1)  {// active low
                        exceptions.add( new SvtEvioHeaderApvReadErrorException("Read error occurred " + 
                                getMultisampleDebugString(headerDataInfo, SvtEvioUtils.getMultisampleTailWord(multisampleHeader)) +
                                getDebugString(bufAddresses, frameCounts, readError))); 
                    }
                }


            } // multisampleheaders
            
            
            // Check the header data
            // Parts of this get its input from the multisample which has already been checked
            // therefore I don't expect these to happen.
            List<SvtEvioHeaderException> svtHeaderDataExceptions = checkSvtHeaderData(headerDataInfo);
            
            exceptions.addAll(svtHeaderDataExceptions);

            

        }
        
        return exceptions;

    }
    
    //public static List<SvtEvioHeaderException> checkSvtHeaderData(SvtHeaderDataInfo header) throws SvtEvioHeaderSyncErrorException, SvtEvioHeaderOFErrorException, SvtEvioHeaderSkipCountException {
    public static List<SvtEvioHeaderException> checkSvtHeaderData(SvtHeaderDataInfo header)  {
        
        int tail = header.getTail();
        LOGGER.fine("checkSvtHeaderData tail " + tail + "( " + Integer.toHexString(tail) + " ) " +
                                                 " errorbit   " +  Integer.toHexString(SvtEvioUtils.getSvtTailSyncErrorBit(tail)) +
                                                 " OFerrorbit " +  Integer.toHexString(SvtEvioUtils.getSvtTailOFErrorBit(tail)) + 
                                                 " checkSvtHeaderData skipcount  " +  Integer.toHexString(SvtEvioUtils.getSvtTailMultisampleSkipCount(tail)));
        
        List<SvtEvioHeaderException> exceptions = new ArrayList<SvtEvioHeaderException>();
        
        if( SvtEvioUtils.getSvtTailSyncErrorBit(tail) != 0) {
            exceptions.add( new SvtEvioHeaderSyncErrorException("This SVT header had a SyncError " + getHeaderDebugString(header)));
        }
        else if( SvtEvioUtils.getSvtTailOFErrorBit(tail) != 0) {
            exceptions.add( new SvtEvioHeaderOFErrorException("This header had a OverFlowError " + getHeaderDebugString(header)));
        }
        else if( SvtEvioUtils.getSvtTailMultisampleSkipCount(tail) != 0) {
            exceptions.add( new SvtEvioHeaderSkipCountException("This header had a skipCount " + SvtEvioUtils.getSvtTailMultisampleSkipCount(tail) + " error " + getHeaderDebugString(header)));
        }
        if(exceptions.size() == 0)
            LOGGER.fine("checkSvtHeaderData passed all I guess");
        else
            LOGGER.fine("checkSvtHeaderData problem found");
        return exceptions;
    }
    
    
    private static String getHeaderDebugString(SvtHeaderDataInfo headerDataInfo) {
        return headerDataInfo.toString();
    }

    private static String getMultisampleDebugString(SvtHeaderDataInfo headerDataInfo, int multisampleHeaderTailWord) {
        String s = getHeaderDebugString(headerDataInfo) + 
                " multisample: feb " + SvtEvioUtils.getFebIDFromMultisampleTail(multisampleHeaderTailWord) + 
                " hybrid " + SvtEvioUtils.getFebHybridIDFromMultisampleTail(multisampleHeaderTailWord) + 
                " apv " + SvtEvioUtils.getApvFromMultisampleTail(multisampleHeaderTailWord) + " ";
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
    
    
    public static int getDAQComponentFromExceptionMsg(SvtEvioHeaderException exception, String type) {
        //Clean up character return before proceeding, I don't know why the regexp fails but whatever
        
        String str;
        int charRet_idx = exception.getMessage().indexOf('\n');
        if(charRet_idx != -1) str = exception.getMessage().substring(0, exception.getMessage().indexOf('\n'));
        else str = exception.getMessage();
        
        // now match
        Matcher m = Pattern.compile(".*\\s" + type + "\\s(\\d+)\\s.*").matcher(str);
        int id = -1;
        try {
            if(m.matches())
                id = Integer.parseInt(m.group(1));
            else 
                id = -2;            
        } catch (Exception e) {
            throw new RuntimeException("exception when matching \"" + str + "\" with \"" + m.pattern().toString() + "\"", e);
        }
        //System.out.println("got " + id + " from " + m.pattern().toString() + " for \""+ str + "\"");
        return id;
    }    
    
    public static String getSvtEvioHeaderExceptionCompactMessage(SvtEvioHeaderException e) {
        String str = "Exception type " + e.getClass().getSimpleName();
        int roc = getDAQComponentFromExceptionMsg(e, "num");
        if(roc<0) throw new RuntimeException("Got " + roc + " from matching \"" + e.getMessage()  + " seem to have failed. Shouldn't happen?");
        int feb = getDAQComponentFromExceptionMsg(e, "feb");
        int hybrid = getDAQComponentFromExceptionMsg(e, "hybrid");
        int apv = getDAQComponentFromExceptionMsg(e, "apv");
        str += " for roc " + roc + " feb " + feb + " hybrid " + hybrid + " apv " + apv;
        return str;
    }
    
    public static String getSvtEvioHeaderExceptionName(SvtEvioHeaderException e) {
        return e.getClass().getSimpleName();
    }
    
    public static List<String> getSvtEvioHeaderExceptionNames(List<SvtEvioHeaderException> exceptions) {
        List<String> l = new ArrayList<String>();
        for (SvtEvioHeaderException e : exceptions) {
            String name = getSvtEvioHeaderExceptionName(e);
            if( !l.contains(name)) l.add(name);
        }
        return l;
    }
    
    
    /**
     * Private construction to avoid class being instantiated
     */
    private SvtEventHeaderChecker() {
    }

    

    
}
