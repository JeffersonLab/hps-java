/**
 * 
 */
package org.hps.evio;

import java.util.List;

import org.hps.record.svt.SvtEventHeaderChecker;
import org.hps.record.svt.SvtHeaderDataInfo;
import org.hps.record.svt.SvtEvioExceptions.SvtEvioHeaderApvBufferAddressException;
import org.hps.record.svt.SvtEvioExceptions.SvtEvioHeaderApvFrameCountException;
import org.hps.record.svt.SvtEvioExceptions.SvtEvioHeaderApvReadErrorException;
import org.hps.record.svt.SvtEvioExceptions.SvtEvioHeaderException;
import org.hps.record.svt.SvtEvioExceptions.SvtEvioHeaderMultisampleErrorBitException;
import org.hps.record.svt.SvtEvioExceptions.SvtEvioHeaderOFErrorException;
import org.hps.record.svt.SvtEvioExceptions.SvtEvioHeaderSkipCountException;
import org.hps.record.svt.SvtEvioExceptions.SvtEvioHeaderSyncErrorException;
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
    
        // Check that the SVT header data is valid
        // Catch the exceptions locally, add stuff to the event, then throw it again
        // and handle it outside
        
        // if we want we can control the behavior here depending on if we want the run to stop
        
        try {
            SvtEventHeaderChecker.checkSvtHeaders(headers);
        } catch (SvtEvioHeaderApvBufferAddressException
                | SvtEvioHeaderApvFrameCountException
                | SvtEvioHeaderMultisampleErrorBitException
                | SvtEvioHeaderApvReadErrorException
                | SvtEvioHeaderSyncErrorException
                | SvtEvioHeaderOFErrorException
                | SvtEvioHeaderSkipCountException e) {

            // add skimming flag
            SvtEventFlagger.voidAddHeaderCheckResultToMetaData(false, lcsimEvent);
            
            // add stuff to the event meta data
            SvtEventFlagger.AddHeaderInfoToMetaData(headers, lcsimEvent);
            
            // then throw the exception again to be caught in the event builder
            if(throwHeaderExceptions)
                throw new SvtEvioHeaderException(e);
            else {
                LOGGER.info("caught SvtEvioHeaderException exception for event " + lcsimEvent.getEventNumber());
                return; // need to return to prevent me from setting the GOOD flag outside the caught exceptions.
            }
        }
        
        // add skimming flag - the header is OK since I would never get here otherwise
        SvtEventFlagger.voidAddHeaderCheckResultToMetaData(true, lcsimEvent);
        
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
