/**
 * 
 */
package org.hps.evio;

import java.util.List;

import org.hps.record.svt.SvtEventHeaderChecker;
import org.hps.record.svt.SvtEvioExceptions.SvtEvioHeaderException;
import org.hps.record.svt.SvtHeaderDataInfo;
import org.lcsim.event.EventHeader;
import org.lcsim.lcio.LCIOUtil;


/**
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class AugmentedSvtEvioReader extends SvtEvioReader {

    private final static boolean throwHeaderExceptions = false; 
    
    /**
     * 
     */
    public AugmentedSvtEvioReader() {
        super();
    }

    
    
    
    @Override
    protected void processSvtHeaders(List<SvtHeaderDataInfo> headers, EventHeader lcsimEvent) throws SvtEvioHeaderException {


        LOGGER.finest("Process " + headers.size() + " SVT headers for run " + lcsimEvent.getRunNumber() + " and event " + lcsimEvent.getEventNumber());
        
        // Check that the SVT header data is valid
        // Catch the exceptions locally, add stuff to the event, then throw it again
        // and handle it outside
        
        // if we want we can control the behavior here depending on if we want the run to stop
        
        List<SvtEvioHeaderException> exceptions = SvtEventHeaderChecker.checkSvtHeaders(headers);

        if( !exceptions.isEmpty() ) {

            LOGGER.finest("Found " + exceptions.size() + " " + SvtEvioHeaderException.class.getSimpleName() + " exceptions");
            
            // print some debug info 
            
            List<String> exceptionNames = SvtEventHeaderChecker.getSvtEvioHeaderExceptionNames(exceptions);
            String names = "";
            for(String str : exceptionNames) names += str + " ";
            
            LOGGER.info("Caught " + exceptions.size() + " SvtEvioHeaderExceptions for event " + lcsimEvent.getEventNumber() + " of " + exceptionNames.size() + " types: " + names);
            
            //LOGGER.fine("List all of them.\n");
            //int i = 0;
            //for(SvtEvioHeaderException e : exceptions ) {
            //    LOGGER.fine("Exception " + (i++) + " for event " + lcsimEvent.getEventNumber() + ":\n" + e.getMessage());
            //}

            // add event flag
            SvtEventFlagger.voidAddHeaderCheckResultToMetaData(false, lcsimEvent);

            // add stuff to the event meta data
            SvtEventFlagger.AddHeaderInfoToMetaData(headers, lcsimEvent);

            // then throw the first exception again to be caught in the event builder if we want to
            if(throwHeaderExceptions)
                throw new SvtEvioHeaderException(exceptions.get(0));
            
        } else { 
            
            LOGGER.finest("No " + SvtEvioHeaderException.class.getSimpleName() + " exceptions found for this event");
            
            // add skimming flag - the header is OK since I would never get here otherwise
            SvtEventFlagger.voidAddHeaderCheckResultToMetaData(true, lcsimEvent);
            
            
            
        }

        // Add SVT header data to the event
        //this.addSvtHeadersToEventEventCollection(headers, lcsimEvent);

    }
    
    
    protected void addSvtHeadersToEventEventCollection(List<SvtHeaderDataInfo> headers, EventHeader lcsimEvent) {
        // Turn on 64-bit cell ID.
        int flag = LCIOUtil.bitSet(0, 31, true);
        // Add the collection of raw hits to the LCSim event
        lcsimEvent.put(SVT_HEADER_COLLECTION_NAME, headers, SvtHeaderDataInfo.class, flag);

    }
        
    


}
