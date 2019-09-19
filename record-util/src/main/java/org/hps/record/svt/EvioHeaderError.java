package org.hps.record.svt;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents information about errors found in SVT EVIO headers.
 * 
 * @author jeremym
 *
 */
public class EvioHeaderError {
   
    /**
     * The basic error types.
     */
    public enum ErrorType {
        ApvBufferAddress,
        ApvFrameCount,
        ApvRead,
        MultisampleErrorBit,
        Overflow,
        SkipCount,
        Sync
    }
    
    /*
     * A set of static class instances are provided here with default error messages to avoid creating new objects when
     * it isn't necessary.
     */
    
    public static final EvioHeaderError APV_BUFFER_ADDRESS_COUNT = new EvioHeaderError(ErrorType.ApvBufferAddress, 
            "Invalid number of APV buffer addresses.");
        
    public static final EvioHeaderError APV_BUFFER_ADDRESS_MISMATCH = new EvioHeaderError(ErrorType.ApvBufferAddress,
            "The APV buffer addresses in this event do not match");
    
    public static final EvioHeaderError APV_FRAME_COUNT = new EvioHeaderError(ErrorType.ApvFrameCount, 
            "Invalid number of APV frame counts.");
    
    public static final EvioHeaderError APV_FRAME_COUNT_MISMATCH = new EvioHeaderError(ErrorType.ApvFrameCount, 
            "The APV frame counts in this event do not match.");
   
    public static final EvioHeaderError APV_READ_ERRORS = new EvioHeaderError(ErrorType.ApvRead, 
            "Invalid number of read errors.");
    
    public static final EvioHeaderError MULTISAMPLE_ERROR_BIT = new EvioHeaderError(ErrorType.MultisampleErrorBit, 
            "A multisample header error bit was set.");
    
    public static final EvioHeaderError SYNC = new EvioHeaderError(ErrorType.Sync,
            "This header had a sync error.");
    
    public static final EvioHeaderError OVERFLOW = new EvioHeaderError(ErrorType.Overflow,
            "This header had an overflow error.");
    
    public static final EvioHeaderError SKIP_COUNT = new EvioHeaderError(ErrorType.SkipCount,
            "This header had a skipCount.");
    
    private final String debugString;
    
    private final ErrorType errorType;
    
    private final String message;
    
    public static Set<String> getUniqueNames(List<EvioHeaderError> errors) {
        Set<String> uniqueErrorNames = new HashSet<String>();
        for (EvioHeaderError error : errors) {
            uniqueErrorNames.add(error.getType().name());
        }
        return uniqueErrorNames;
    }
    
    public EvioHeaderError(ErrorType errorType, String message) {
        this.errorType = errorType;
        this.message = message;
        this.debugString = "";
    }
    
    public EvioHeaderError(ErrorType errorType, String message, String debugString) {
        this.errorType = errorType;
        this.message = message;
        this.debugString = debugString;
    }    
    
    String getDebugString() {
        return debugString;
    }    
    
    String getMessage() {
        return message;
    }
    
    ErrorType getType() {
        return errorType;
    }        
}
