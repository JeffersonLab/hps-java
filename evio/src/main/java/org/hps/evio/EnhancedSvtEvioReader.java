/**
 * 
 */
package org.hps.evio;

import java.util.List;

import org.hps.readout.svt.SvtErrorBitData;
import org.hps.readout.svt.SvtHeaderData;
import org.lcsim.event.EventHeader;
import org.lcsim.lcio.LCIOUtil;

/**
 * 
 * SVT EVIO reader extending {@link SvtEvioReader} and adds SVT error bit information to the event.
 * 
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
final public class EnhancedSvtEvioReader extends SvtEvioReader {
    
    public static final String SVT_ERROR_BIT_COLLECTION_NAME = "SvtErrorBits";
    public static final String SVT_HEADER_COLLECTION_NAME = "SvtHeaders";

    /**
     * 
     */
    public EnhancedSvtEvioReader() {
        super();
    }
    
    @Override
    protected SvtErrorBitData extractErrorBit(int[] multisample) {
        
        if( !SvtEvioUtils.isApvHeader(multisample) ) 
            throw new RuntimeException("Need the APV header in order to extract the error bit.");

        int errorBit = SvtEvioUtils.getErrorBit(multisample);
        int rce = SvtEvioUtils.getFpgaID(multisample);
        int feb = SvtEvioUtils.getFebID(multisample);
        int hybrid = SvtEvioUtils.getFebHybridID(multisample);
        int apv = SvtEvioUtils.getApv(multisample);
        
        return  new SvtErrorBitData(rce, feb, hybrid, apv, errorBit);
    }

    @Override
    protected void addErrorBitsToEvent(List<SvtErrorBitData> errorBits,
            EventHeader lcsimEvent) {
        // Turn on 64-bit cell ID.
        int flag = LCIOUtil.bitSet(0, 31, true);
        // Add the collection of raw hits to the LCSim event
        lcsimEvent.put(SVT_ERROR_BIT_COLLECTION_NAME, errorBits, SvtErrorBitData.class, flag);

        
    }    
    
    @Override
    protected SvtHeaderData extractSvtHeader(int num, int[] data) {
        // Extract the header information
        int svtHeader = SvtEvioUtils.getSvtHeader(data);
        int svtDataType = SvtEvioUtils.getSvtDataType(svtHeader);
        int eventCount = SvtEvioUtils.getSvtDataEventCounter(svtHeader);
        // Extract the tail information
        int svtTail = SvtEvioUtils.getSvtTail(data);
        int overflowError = SvtEvioUtils.getSvtTailOFErrorBit(svtTail);
        int syncError = SvtEvioUtils.getSvtTailSyncErrorBit(svtTail);
        int skipCount = SvtEvioUtils.getSvtTailMultisampleSkipCount(svtTail);
        int multisampleCount = SvtEvioUtils.getSvtTailMultisampleCount(svtTail);
        return new SvtHeaderData(num, eventCount, overflowError, syncError, skipCount, multisampleCount);
    }
    
    @Override
    protected void addSvtHeadersToEvents(List<SvtHeaderData> headers,
            EventHeader lcsimEvent) {
        // Turn on 64-bit cell ID.
        int flag = LCIOUtil.bitSet(0, 31, true);
        // Add the collection of raw hits to the LCSim event
        lcsimEvent.put(SVT_HEADER_COLLECTION_NAME, headers, SvtHeaderData.class, flag);

    }

}
