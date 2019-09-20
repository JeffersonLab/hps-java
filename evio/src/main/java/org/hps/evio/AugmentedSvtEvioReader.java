package org.hps.evio;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.record.svt.EvioHeaderError;
import org.hps.record.svt.SvtEventHeaderCheckerNew;
import org.hps.record.svt.SvtEvioExceptions.SvtEvioHeaderException;
import org.hps.record.svt.SvtHeaderDataInfo;
import org.lcsim.event.EventHeader;

/**
 * This is essentially the same as {@link SvtEvioReader} except that it 
 * performs error checking of the SVT EVIO event headers.
 * 
 * Package logging should be set to FINE to print out any SVT EVIO header
 * errors that are found during data processing.
 * 
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 */
public class AugmentedSvtEvioReader extends SvtEvioReader {

    /**
     * Use a class logger so header error printing can be separated from package messages.
     */
    private static Logger LOGGER = Logger.getLogger(AugmentedSvtEvioReader.class.getCanonicalName());
    
    /**
     * Whether header exceptions should be thrown during processing; off by default.
     */
    private boolean throwHeaderExceptions = false; 
    
    /**
     * Add SVT headers to event; off by default.
     */
    private boolean addHeadersToEvent = false;
    
    /**
     * Class for checking SVT event headers for various errors.
     */
    private final SvtEventHeaderCheckerNew headerCheck = new SvtEventHeaderCheckerNew();

    /**
     * Default constructor.
     */
    public AugmentedSvtEvioReader() {
        super();
    }
    
    /**
     * Set whether to throw exceptions or not
     * @param throwHeaderExceptions True to throw exceptions
     */
    void setThrowHeaderExceptions(boolean throwHeaderExceptions) {
        this.throwHeaderExceptions = throwHeaderExceptions;
    }
    
    /**
     * Set whether to write header data to the event
     * @param addHeadersToEvent True to add headers to the output event
     */
    void setAddHeadersToEvent(boolean addHeadersToEvent) {
        this.addHeadersToEvent = addHeadersToEvent;
    }

    /**
     * Process SVT headers, handling any errors if they occur.
     * @param headers The list of SVT headers
     * @param lcsimEvent The full lcsim event
     */
    @Override
    protected void processSvtHeaders(List<SvtHeaderDataInfo> headers, EventHeader lcsimEvent) throws SvtEvioHeaderException {
        // Note that the superclass method being overridden is not called, because it doesn't do anything.
        
        LOGGER.info("Processing " + headers.size() + " SVT headers for run " + lcsimEvent.getRunNumber() + " and event " + lcsimEvent.getEventNumber());
        
        // Get a list of any SVT header errors.
        List<EvioHeaderError> errors = headerCheck.getHeaderErrors(headers);

        if( !errors.isEmpty() ) {

            final int nerrors = errors.size();
                                                                        
            Set<String> errorNames = EvioHeaderError.getUniqueNames(errors);
            LOGGER.warning("Found " + nerrors + " SVT header errors in event " + lcsimEvent.getEventNumber()
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

        // Optionally add SVT header data to the event
        if (this.addHeadersToEvent) {
            this.addSvtHeadersToEventEventCollection(headers, lcsimEvent);
        }
    }
    
    private void addSvtHeadersToEventEventCollection(List<SvtHeaderDataInfo> headers, EventHeader lcsimEvent) {
        // Add the collection of headers to the LCSim event
        lcsimEvent.put(SVT_HEADER_COLLECTION_NAME, headers, SvtHeaderDataInfo.class, 0);
    }  
}
