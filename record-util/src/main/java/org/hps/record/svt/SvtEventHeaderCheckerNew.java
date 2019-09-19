package org.hps.record.svt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hps.record.svt.EvioHeaderError.ErrorType;
import org.hps.record.svt.SvtEvioExceptions.SvtEvioHeaderApvBufferAddressException;
import org.hps.record.svt.SvtEvioExceptions.SvtEvioHeaderApvFrameCountException;
import org.hps.record.svt.SvtEvioExceptions.SvtEvioHeaderApvReadErrorException;
import org.hps.record.svt.SvtEvioExceptions.SvtEvioHeaderException;
import org.hps.record.svt.SvtEvioExceptions.SvtEvioHeaderMultisampleErrorBitException;
import org.hps.record.svt.SvtEvioExceptions.SvtEvioHeaderOFErrorException;
import org.hps.record.svt.SvtEvioExceptions.SvtEvioHeaderSkipCountException;
import org.hps.record.svt.SvtEvioExceptions.SvtEvioHeaderSyncErrorException;

/**
 * Fork of {@link SvtEventHeaderChecker} to remove memory leaks.
 * 
 * Changes to original class:
 * <ul>
 * <li>Allow class to be instantiated rather than only called statically.</li>
 * <li>Change primary methods to be non-static.</li>
 * <li>Instead of directly generating exceptions which uses an enormous amount of memory, 
 *     use a more lightweight class to represent the errors.</li>
 * <li>Where possible, use static instances of error checking classes so they don't need to be continually recreated.</li>
 * <li>Make generation of large debug strings optional. Errors are reported generically without debug strings when this is turned off.</li>
 * <li>For now, use a class rather than package logger for more granular debugging options.</li>
 * <li>Do not hard-code a default log level. This should be done in prop files from command line instead.</li>
 * </ul>
 * 
 * @author jeremym
 */
public class SvtEventHeaderCheckerNew {

    /*
    private static Logger LOGGER = Logger.getLogger(SvtEventHeaderChecker.class.getPackage().getName());
    static {
        LOGGER.setLevel(Level.INFO);
    }
    */
    
    private static Logger LOGGER = Logger.getLogger(SvtEventHeaderChecker.class.getCanonicalName());

    private static String getHeaderDebugString(SvtHeaderDataInfo headerDataInfo) {
        return headerDataInfo.toString();
    }

    private static String getMultisampleDebugString(SvtHeaderDataInfo headerDataInfo, int multisampleHeaderTailWord) {
        String s = getHeaderDebugString(headerDataInfo) + " multisample: feb "
                + SvtEvioUtils.getFebIDFromMultisampleTail(multisampleHeaderTailWord) + " hybrid "
                + SvtEvioUtils.getFebHybridIDFromMultisampleTail(multisampleHeaderTailWord) + " apv "
                + SvtEvioUtils.getApvFromMultisampleTail(multisampleHeaderTailWord) + " ";
        return s;
    }

    private static String getDebugString(int[] bufAddresses, int[] frameCounts, int[] readError) {
        String s = "";
        for (int i = 0; i < bufAddresses.length; ++i)
            s += "\nbuffer address " + i + "  " + bufAddresses[i] + " ( " + Integer.toHexString(bufAddresses[i]) + " )";
        for (int i = 0; i < frameCounts.length; ++i)
            s += "\nframe count    " + i + "  " + frameCounts[i] + " ( " + Integer.toHexString(frameCounts[i]) + " )";
        for (int i = 0; i < readError.length; ++i)
            s += "\nread error     " + i + "  " + readError[i] + " ( " + Integer.toHexString(readError[i]) + " )";
        return s;
    }

    public static int getDAQComponentFromExceptionMsg(SvtEvioHeaderException exception, String type) {
        // Clean up character return before proceeding, I don't know why the regexp
        // fails but whatever

        String str;
        int charRet_idx = exception.getMessage().indexOf('\n');
        if (charRet_idx != -1)
            str = exception.getMessage().substring(0, exception.getMessage().indexOf('\n'));
        else
            str = exception.getMessage();

        // now match
        Matcher m = Pattern.compile(".*\\s" + type + "\\s(\\d+)\\s.*").matcher(str);
        int id = -1;
        try {
            if (m.matches())
                id = Integer.parseInt(m.group(1));
            else
                id = -2;
        } catch (Exception e) {
            throw new RuntimeException(
                    "exception when matching \"" + str + "\" with \"" + m.pattern().toString() + "\"", e);
        }
        // System.out.println("got " + id + " from " + m.pattern().toString() + " for
        // \""+ str + "\"");
        return id;
    }

    // This is apparently unused in hps-java. --JM
    /*
    public static String getSvtEvioHeaderExceptionCompactMessage(SvtEvioHeaderException e) {
        String str = "Exception type " + e.getClass().getSimpleName();
        int roc = getDAQComponentFromExceptionMsg(e, "num");
        if (roc < 0)
            throw new RuntimeException(
                    "Got " + roc + " from matching \"" + e.getMessage() + " seem to have failed. Shouldn't happen?");
        int feb = getDAQComponentFromExceptionMsg(e, "feb");
        int hybrid = getDAQComponentFromExceptionMsg(e, "hybrid");
        int apv = getDAQComponentFromExceptionMsg(e, "apv");
        str += " for roc " + roc + " feb " + feb + " hybrid " + hybrid + " apv " + apv;
        return str;
    }
    */

    public static String getSvtEvioHeaderExceptionName(SvtEvioHeaderException e) {
        return e.getClass().getSimpleName();
    }

    public static List<String> getSvtEvioHeaderExceptionNames(List<SvtEvioHeaderException> exceptions) {
        List<String> l = new ArrayList<String>();
        for (SvtEvioHeaderException e : exceptions) {
            String name = getSvtEvioHeaderExceptionName(e);
            if (!l.contains(name))
                l.add(name);
        }
        return l;
    }

    private boolean generateDebugStrings = true;

    SvtEventHeaderCheckerNew() {
    }

    SvtEventHeaderCheckerNew(boolean generateDebugStrings) {
        this.generateDebugStrings = generateDebugStrings;
    }

    public List<EvioHeaderError> getHeaderErrors(List<SvtHeaderDataInfo> headers) {

        LOGGER.fine("check " + headers.size() + " headers  ");

        int[] bufferAddresses = new int[6];
        int[] firstFrameCounts = new int[6];
        boolean firstHeader = true;
        int[] multisampleHeader;
        int[] bufAddresses;
        int[] frameCounts;
        int[] readError;
        int count;
        int multisampleHeaderTailErrorBit;

        // create a list to hold all active exceptions
        // List<SvtEvioHeaderException> exceptions = new
        // ArrayList<SvtEvioHeaderException>();

        List<EvioHeaderError> errors = new ArrayList<EvioHeaderError>();

        for (SvtHeaderDataInfo headerDataInfo : headers) {
            LOGGER.fine("checking header: " + headerDataInfo.toString());

            // Check the multisample header information
            int nMultisampleHeaders = headerDataInfo.getNumberOfMultisampleHeaders();
            for (int iMultisampleHeader = 0; iMultisampleHeader < nMultisampleHeaders; iMultisampleHeader++) {
                LOGGER.fine("iMultisampleHeader " + iMultisampleHeader);

                multisampleHeader = SvtHeaderDataInfo.getMultisampleHeader(iMultisampleHeader, headerDataInfo);

                // get multisample tail error bit
                multisampleHeaderTailErrorBit = SvtEvioUtils
                        .getErrorBitFromMultisampleHeader(SvtEvioUtils.getMultisampleTailWord(multisampleHeader));

                // get buffer addresses
                bufAddresses = SvtEvioUtils.getApvBufferAddresses(multisampleHeader);

                // get frame counts
                frameCounts = SvtEvioUtils.getApvFrameCount(multisampleHeader);

                // check if there was any read errors
                readError = SvtEvioUtils.getApvReadErrors(multisampleHeader);

                if (bufAddresses.length != 6) {
                    errors.add(EvioHeaderError.APV_BUFFER_ADDRESS_COUNT);
                }

                if (frameCounts.length != 6) {
                    errors.add(EvioHeaderError.APV_FRAME_COUNT);
                }

                if (readError.length != 6) {
                    errors.add(EvioHeaderError.APV_READ_ERRORS);
                }

                // Check for error bit
                if (multisampleHeaderTailErrorBit != 0) {
                    if (this.generateDebugStrings) {
                        errors.add(new EvioHeaderError(ErrorType.MultisampleErrorBit,
                                EvioHeaderError.MULTISAMPLE_ERROR_BIT.getMessage(),
                                getMultisampleDebugString(headerDataInfo,
                                        SvtEvioUtils.getMultisampleTailWord(multisampleHeader))
                                        + getDebugString(bufAddresses, frameCounts, readError)));
                    } else {
                        errors.add(EvioHeaderError.MULTISAMPLE_ERROR_BIT);
                    }
                }

                // print debug
                LOGGER.finest(getMultisampleDebugString(headerDataInfo,
                        SvtEvioUtils.getMultisampleTailWord(multisampleHeader))
                        + getDebugString(bufAddresses, frameCounts, readError));

                // Get a reference for comparison
                if (firstHeader) {
                    System.arraycopy(bufAddresses, 0, bufferAddresses, 0, bufAddresses.length);
                    System.arraycopy(frameCounts, 0, firstFrameCounts, 0, frameCounts.length);
                    firstHeader = false;
                } else {

                    // Check that apv buffer addresses match
                    if (!Arrays.equals(bufferAddresses, bufAddresses)) {
                        if (this.generateDebugStrings) {
                            errors.add(new EvioHeaderError(ErrorType.ApvBufferAddress,
                                    EvioHeaderError.APV_BUFFER_ADDRESS_MISMATCH.getMessage(),
                                    getMultisampleDebugString(headerDataInfo,
                                            SvtEvioUtils.getMultisampleTailWord(multisampleHeader))
                                            + getDebugString(bufAddresses, frameCounts, readError) + " compared to "
                                            + getDebugString(bufferAddresses, firstFrameCounts, readError)));
                        } else {
                            errors.add(EvioHeaderError.APV_BUFFER_ADDRESS_MISMATCH);
                        }

                    }

                    // Check that apv frame count match
                    if (!Arrays.equals(firstFrameCounts, frameCounts)) {
                        if (this.generateDebugStrings) {
                            errors.add(new EvioHeaderError(
                                    ErrorType.ApvBufferAddress,
                                    EvioHeaderError.APV_FRAME_COUNT_MISMATCH.getMessage(),
                                    getMultisampleDebugString(headerDataInfo,
                                            SvtEvioUtils.getMultisampleTailWord(multisampleHeader)) + 
                                            getDebugString(bufAddresses, frameCounts, readError) + " compared to " + 
                                            getDebugString(bufferAddresses, firstFrameCounts, readError))
                                    );
                        } else {
                            errors.add(EvioHeaderError.APV_FRAME_COUNT_MISMATCH);
                        }
                    }
                }

                // Check that the APV frame counts are incrementing
                // remember to take into account the 2-bit rollover (duh!)

                count = -1;
                for (int iFrame = 0; iFrame < frameCounts.length; ++iFrame) {
                    LOGGER.fine("frame count " + iFrame + "  " + frameCounts[iFrame] + " ( "
                            + Integer.toHexString(frameCounts[iFrame]) + " )");

                    if (frameCounts[iFrame] > 15 || (count < 15 && frameCounts[iFrame] < count)
                            || (count == 15 && frameCounts[iFrame] != 0)) {

                        if (this.generateDebugStrings) {
                            errors.add(
                                    new EvioHeaderError(ErrorType.ApvFrameCount,
                                            EvioHeaderError.APV_FRAME_COUNT.getMessage(),
                                            getMultisampleDebugString(headerDataInfo,
                                                    SvtEvioUtils.getMultisampleTailWord(multisampleHeader))
                                                    + getDebugString(bufAddresses, frameCounts, readError))
                                    );
                        } else {
                            // FIXME: Is this error type specific enough here?
                            errors.add(EvioHeaderError.APV_FRAME_COUNT);
                        }
                    }
                    count = frameCounts[iFrame];
                }

                for (int iReadError = 0; iReadError < readError.length; ++iReadError) {
                    LOGGER.finest("read error " + iReadError + "  " + readError[iReadError] + " ( "
                            + Integer.toHexString(readError[iReadError]) + " )");

                    if (readError[iReadError] != 1) {// active low
                        if (this.generateDebugStrings) {
                            errors.add(
                                    new EvioHeaderError(ErrorType.ApvRead, EvioHeaderError.APV_READ_ERRORS.getMessage(),
                                            getMultisampleDebugString(headerDataInfo,
                                                    SvtEvioUtils.getMultisampleTailWord(multisampleHeader))
                                                    + getDebugString(bufAddresses, frameCounts, readError)));                        
                        } else {
                            errors.add(EvioHeaderError.APV_READ_ERRORS);
                        }
                    }
                }

            } // multisampleheaders

            // Add additional errors from header data.
            addSvtHeaderDataErrors(headerDataInfo, errors);
        }

        return errors;
    }
    
    private void addSvtHeaderDataErrors(SvtHeaderDataInfo header, List<EvioHeaderError> errors)  {
        
        int tail = header.getTail();
        LOGGER.finest("checkSvtHeaderData tail " + tail + "( " + Integer.toHexString(tail) + " ) " +
                                                 " errorbit   " +  Integer.toHexString(SvtEvioUtils.getSvtTailSyncErrorBit(tail)) +
                                                 " OFerrorbit " +  Integer.toHexString(SvtEvioUtils.getSvtTailOFErrorBit(tail)) + 
                                                 " checkSvtHeaderData skipcount  " +  Integer.toHexString(SvtEvioUtils.getSvtTailMultisampleSkipCount(tail)));
               
        // FIXME: Should this really be else/if or can these all occur separately?
        if( SvtEvioUtils.getSvtTailSyncErrorBit(tail) != 0) {
            if (this.generateDebugStrings) {
                errors.add(new EvioHeaderError(ErrorType.Sync, EvioHeaderError.SYNC.getMessage(), 
                        getHeaderDebugString(header)));
            } else {
                errors.add(EvioHeaderError.SYNC);
            }

        } else if(SvtEvioUtils.getSvtTailOFErrorBit(tail) != 0) {
            if (this.generateDebugStrings) {
                errors.add(new EvioHeaderError(ErrorType.Overflow, EvioHeaderError.OVERFLOW.getMessage(), 
                        getHeaderDebugString(header)));
            } else {
                errors.add(EvioHeaderError.OVERFLOW);
            }
        } else if(SvtEvioUtils.getSvtTailMultisampleSkipCount(tail) != 0) {
            if (this.generateDebugStrings) {
                errors.add(new EvioHeaderError(ErrorType.SkipCount, EvioHeaderError.SKIP_COUNT.getMessage(), 
                        SvtEvioUtils.getSvtTailMultisampleSkipCount(tail) + " error " + getHeaderDebugString(header)));               
            } else {
                errors.add(EvioHeaderError.SKIP_COUNT);
            }
        } 
    }
}
