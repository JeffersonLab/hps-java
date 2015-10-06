/**
 * 
 */
package org.hps.evio;

import java.util.List;

import org.hps.evio.SvtEvioExceptions.*;
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
    protected void processSvtHeaders(List<SvtHeaderDataInfo> headers, EventHeader lcsimEvent) throws SvtEvioHeaderException {
        // Check that the SVT header data is valid
        // Catch the exception locally, add stuff to the event, then throw it again
        // and handle it outside
        try {
        
            SvtEventHeaderChecker.checkSvtHeaders(headers);
        
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
    
    
    protected void addSvtHeadersToEventEventCollection(List<SvtHeaderDataInfo> headers, EventHeader lcsimEvent) {
        // Turn on 64-bit cell ID.
        int flag = LCIOUtil.bitSet(0, 31, true);
        // Add the collection of raw hits to the LCSim event
        lcsimEvent.put(SVT_HEADER_COLLECTION_NAME, headers, SvtHeaderDataInfo.class, flag);

    }
        
    


}
