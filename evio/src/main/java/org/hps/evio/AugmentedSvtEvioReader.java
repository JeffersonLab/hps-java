/**
 * 
 */
package org.hps.evio;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.hps.record.svt.EvioHeaderError;
import org.hps.record.svt.SvtEventHeaderCheckerNew;
import org.hps.record.svt.SvtEvioExceptions.SvtEvioHeaderException;
import org.hps.record.svt.SvtHeaderDataInfo;
import org.lcsim.event.EventHeader;

/**
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class AugmentedSvtEvioReader extends SvtEvioReader {

    /**
     * Whether header exceptions should be thrown during processing
     */
    private final boolean throwHeaderExceptions; 
    
    /**
     * Class for checking SVT event headers for various errors.
     */
    private final SvtEventHeaderCheckerNew headerCheck = new SvtEventHeaderCheckerNew();

    /**
     * Constructor, which turns off throwing of header exceptions.
     */
    public AugmentedSvtEvioReader() {
        super();
        this.throwHeaderExceptions = false;
    }
    
    /**
     * Constructor, which allows setting whether header exceptions should be thrown.
     * @param throwHeaderExceptions True if header exceptions should be thrown during processing
     */
    public AugmentedSvtEvioReader(boolean throwHeaderExceptions) {
        super();
        this.throwHeaderExceptions = throwHeaderExceptions;
    }
                  
    @Override
    protected void processSvtHeaders(List<SvtHeaderDataInfo> headers, EventHeader lcsimEvent) throws SvtEvioHeaderException {

        LOGGER.info("Processing " + headers.size() + " SVT headers for run " + lcsimEvent.getRunNumber() + " and event " + lcsimEvent.getEventNumber());
        
        // Get a list of any SVT header errors.
        List<EvioHeaderError> errors = headerCheck.getHeaderErrors(headers);

        if( !errors.isEmpty() ) {

            final int nerrors = errors.size();
                                                                        
            Set<String> errorNames = EvioHeaderError.getUniqueNames(errors);
            LOGGER.warning("Found " + nerrors + " SVT header errors for event " + lcsimEvent.getEventNumber()
                    + " with types: " + errorNames.toString());
 
            // Print all errors to the log if logging level is fine or more.
            if (LOGGER.getLevel().intValue() < Level.INFO.intValue()) {
                LOGGER.fine("Printing all SVT header errors ..." + '\n');
                for (EvioHeaderError error : errors) {
                    LOGGER.fine(error.toString());
                }
            }

            // add event flag
            SvtEventFlagger.voidAddHeaderCheckResultToMetaData(false, lcsimEvent);

            // add stuff to the event meta data
            SvtEventFlagger.AddHeaderInfoToMetaData(headers, lcsimEvent);

            // then throw the first exception again to be caught in the event builder if we want to
            if(throwHeaderExceptions)
                throw new SvtEvioHeaderException(errors.get(0));
            
        } else { 
            
            LOGGER.info("No SVT header errors found for this event.");
            
            // add skimming flag - the header is OK since I would never get here otherwise
            SvtEventFlagger.voidAddHeaderCheckResultToMetaData(true, lcsimEvent);
        }

        // Add SVT header data to the event
        //this.addSvtHeadersToEventEventCollection(headers, lcsimEvent);
    }
    
    // Commented out for now as it is never activated. Does it actually even work anymore? --JM
    /*
    protected void addSvtHeadersToEventEventCollection(List<SvtHeaderDataInfo> headers, EventHeader lcsimEvent) {
        // Turn on 64-bit cell ID.
        int flag = LCIOUtil.bitSet(0, 31, true);
        // Add the collection of raw hits to the LCSim event
        lcsimEvent.put(SVT_HEADER_COLLECTION_NAME, headers, SvtHeaderDataInfo.class, flag);
    } 
    */       
}
