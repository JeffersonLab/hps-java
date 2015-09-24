/**
 * 
 */
package org.hps.evio;

import java.util.List;

import org.hps.readout.svt.SvtHeaderDataInfo;
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
    protected SvtHeaderDataInfo extractSvtHeader(int num, int[] data) {
        // Extract the header information
        int svtHeader = SvtEvioUtils.getSvtHeader(data);
        // Extract the tail information
        int svtTail = SvtEvioUtils.getSvtTail(data);
        return new SvtHeaderDataInfo(num, svtHeader, svtTail);
        
    }

    @Override
    protected void checkSvtHeaderData(SvtHeaderDataInfo header) throws SvtEvioHeaderException {
        int tail = header.getTail();
        if( SvtEvioUtils.getSvtTailSyncErrorBit(tail) != 0) {
            throw new SvtEvioHeaderException("This header had a SyncError");
        }
        else if( SvtEvioUtils.getSvtTailOFErrorBit(tail) != 0) {
            throw new SvtEvioHeaderException("This header had a OverFlowError");
        }
        else if( SvtEvioUtils.getSvtTailMultisampleSkipCount(tail) != 0) {
            throw new SvtEvioHeaderException("This header had a skipCount " + SvtEvioUtils.getSvtTailMultisampleSkipCount(tail));
        }
    }
    
    @Override
    protected void addSvtHeadersToEvents(List<SvtHeaderDataInfo> headers, EventHeader lcsimEvent) {
    // Turn on 64-bit cell ID.
    int flag = LCIOUtil.bitSet(0, 31, true);
    // Add the collection of raw hits to the LCSim event
    lcsimEvent.put(SVT_HEADER_COLLECTION_NAME, headers, SvtHeaderDataInfo.class, flag);

}

    @Override
    protected void checkSvtSampleCount(int sampleCount, SvtHeaderDataInfo headerData) throws SvtEvioHeaderException {
        if( sampleCount != SvtEvioUtils.getSvtTailMultisampleCount(headerData.getTail())*4)
            throw new SvtEvioHeaderException("multisample count is not consistent with bank size.");
    }

    @Override
    protected void setMultiSampleHeaders(SvtHeaderDataInfo headerData, int[] multisampleHeaders) {
        headerData.setMultisampleHeaders(multisampleHeaders);
    }
    
    @Override
    protected void extractMultisampleTail(int[] multisample, int index, int[] multisampleHeaders) {
        if( SvtEvioUtils.isMultisampleHeader(multisample) ) 
            multisampleHeaders[index] = SvtEvioUtils.getMultisampleTailWord(multisample);
    }
    
    
}
