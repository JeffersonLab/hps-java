package org.hps.record.svt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hps.record.svt.EvioHeaderError.ErrorType;

/**
 * Fork of {@link SvtEventHeaderChecker} to remove memory leaks and make other improvements.
 * 
 * Changes to original class:
 * <ul>
 * <li>Allow class to be instantiated so methods can be called non-statically.</li>
 * <li>Change primary public method to be non-static.</li>
 * <li>Instead of directly generating exceptions, which uses an enormous amount of memory generating tracebacks, 
 *     use a more lightweight class to represent the errors.</li>
 * <li>Where possible, use static instances of error checking classes so they don't need to be continually recreated.</li>
 * <li>Make generation of large debug strings optional. Errors are reported generically without debug strings when this is turned off.</li>
 * <li>Remove all log messages so the class runs silently; caller can print returned errors to the log, if necessary.</li>
 * </ul>
 * 
 * @author jeremym
 */
public class SvtEventHeaderCheckerNew {
        
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

    private boolean generateDebugStrings = true;

    public SvtEventHeaderCheckerNew() {
    }

    public SvtEventHeaderCheckerNew(boolean generateDebugStrings) {
        this.generateDebugStrings = generateDebugStrings;
    }

    public List<EvioHeaderError> getHeaderErrors(List<SvtHeaderDataInfo> headers) {

        int[] firstBufferAddresses = new int[6];
        int[] firstFrameCounts = new int[6];
        boolean firstHeader = true;
        int[] multisampleHeader;
        int[] bufferAddresses;
        int[] frameCounts;
        int[] readError;
        int count;
        int multisampleHeaderTailErrorBit;

        List<EvioHeaderError> errors = new ArrayList<EvioHeaderError>();

        for (SvtHeaderDataInfo headerDataInfo : headers) {

            // Check the multisample header information
            int nMultisampleHeaders = headerDataInfo.getNumberOfMultisampleHeaders();
            for (int iMultisampleHeader = 0; iMultisampleHeader < nMultisampleHeaders; iMultisampleHeader++) {

                multisampleHeader = SvtHeaderDataInfo.getMultisampleHeader(iMultisampleHeader, headerDataInfo);

                // get multisample tail error bit
                multisampleHeaderTailErrorBit = SvtEvioUtils
                        .getErrorBitFromMultisampleHeader(SvtEvioUtils.getMultisampleTailWord(multisampleHeader));

                // get buffer addresses
                bufferAddresses = SvtEvioUtils.getApvBufferAddresses(multisampleHeader);

                // get frame counts
                frameCounts = SvtEvioUtils.getApvFrameCount(multisampleHeader);

                // check if there was any read errors
                readError = SvtEvioUtils.getApvReadErrors(multisampleHeader);

                if (bufferAddresses.length != 6) {
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
                                        + getDebugString(bufferAddresses, frameCounts, readError)));
                    } else {
                        errors.add(EvioHeaderError.MULTISAMPLE_ERROR_BIT);
                    }
                }

                // Get a reference for comparison
                if (firstHeader) {
                    System.arraycopy(bufferAddresses, 0, firstBufferAddresses, 0, bufferAddresses.length);
                    System.arraycopy(frameCounts, 0, firstFrameCounts, 0, frameCounts.length);
                    firstHeader = false;
                } else {

                    // Check that apv buffer addresses match
                    if (!Arrays.equals(firstBufferAddresses, bufferAddresses)) {
                        if (this.generateDebugStrings) {
                            errors.add(new EvioHeaderError(ErrorType.ApvBufferAddress,
                                    EvioHeaderError.APV_BUFFER_ADDRESS_MISMATCH.getMessage(),
                                    getMultisampleDebugString(headerDataInfo,
                                            SvtEvioUtils.getMultisampleTailWord(multisampleHeader))
                                            + getDebugString(bufferAddresses, frameCounts, readError) + " compared to "
                                            + getDebugString(firstBufferAddresses, firstFrameCounts, readError)));
                        } else {
                            errors.add(EvioHeaderError.APV_BUFFER_ADDRESS_MISMATCH);
                        }

                    }

                    // Check that apv frame count match
                    if (!Arrays.equals(firstFrameCounts, frameCounts)) {
                        if (this.generateDebugStrings) {
                            errors.add(new EvioHeaderError(ErrorType.ApvBufferAddress,
                                    EvioHeaderError.APV_FRAME_COUNT_MISMATCH.getMessage(),
                                    getMultisampleDebugString(headerDataInfo,
                                            SvtEvioUtils.getMultisampleTailWord(multisampleHeader))
                                            + getDebugString(bufferAddresses, frameCounts, readError) + " compared to "
                                            + getDebugString(firstBufferAddresses, firstFrameCounts, readError)));
                        } else {
                            errors.add(EvioHeaderError.APV_FRAME_COUNT_MISMATCH);
                        }
                    }
                }

                // Check that the APV frame counts are incrementing
                // remember to take into account the 2-bit rollover (duh!)
                count = -1;
                for (int iFrame = 0; iFrame < frameCounts.length; ++iFrame) {
                    if (isApvFrameCountGood(frameCounts, count, iFrame)) {
                        if (this.generateDebugStrings) {
                            errors.add(new EvioHeaderError(ErrorType.ApvFrameCount,
                                    EvioHeaderError.APV_FRAME_COUNT.getMessage(),
                                    getMultisampleDebugString(headerDataInfo,
                                            SvtEvioUtils.getMultisampleTailWord(multisampleHeader))
                                            + getDebugString(bufferAddresses, frameCounts, readError)));
                        } else {
                            // FIXME: Is this error type specific enough here?
                            errors.add(EvioHeaderError.APV_FRAME_COUNT);
                        }
                    }
                    count = frameCounts[iFrame];
                }

                for (int iReadError = 0; iReadError < readError.length; ++iReadError) {
                    if (readError[iReadError] != 1) {// active low
                        if (this.generateDebugStrings) {
                            errors.add(
                                    new EvioHeaderError(ErrorType.ApvRead, EvioHeaderError.APV_READ_ERRORS.getMessage(),
                                            getMultisampleDebugString(headerDataInfo,
                                                    SvtEvioUtils.getMultisampleTailWord(multisampleHeader))
                                                    + getDebugString(bufferAddresses, frameCounts, readError)));                        
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

    /**
     * Return true if APV frame count has error.
     * @param frameCounts
     * @param count
     * @param iFrame
     * @return
     */
    private static boolean isApvFrameCountGood(int[] frameCounts, int count, int iFrame) {
        return frameCounts[iFrame] > 15 || (count < 15 && frameCounts[iFrame] < count)
                || (count == 15 && frameCounts[iFrame] != 0);
    }
    
    private void addSvtHeaderDataErrors(SvtHeaderDataInfo header, List<EvioHeaderError> errors)  {
        
        int tail = header.getTail();

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
