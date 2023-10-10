package org.hps.record.triggerbank;

import org.hps.record.evio.EvioEventProcessor;
import org.hps.record.triggerbank.AbstractIntData.IntBankDefinition;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;

/**
 * Extract trigger time offset from EVIO data.
 */
public class TiTimeOffsetEvioProcessor extends EvioEventProcessor {

    private final IntBankDefinition headBankDefinition;
    private long maxOffset = 0;

    private final int maxOutliers = 10;
    private final double maxRange = 1.1e9;
    private long minOffset = 0;

    private final double minRange = 0.99e9;
    private int nOutliers = 0;
    private final IntBankDefinition tiBankDefinition;

    public TiTimeOffsetEvioProcessor() {
        headBankDefinition = new IntBankDefinition(HeadBankData.class, new int[] {0x2e, 0xe10f});
        tiBankDefinition = new IntBankDefinition(TIData.class, new int[] {0x2e, 0xe10a});
    }

    @Override
    public void process(final EvioEvent evioEvent) {
        final BaseStructure headBank = headBankDefinition.findBank(evioEvent);
        final BaseStructure tiBank = tiBankDefinition.findBank(evioEvent);
        if (headBank != null && tiBank != null) {
            final int[] headData = headBank.getIntData();
            final int thisTimestamp = headData[3];
            final TIData tiData = new TIData(tiBank.getIntData());
            if (thisTimestamp != 0) {
                final long offset = thisTimestamp * 1000000000L - tiData.getTime();
                if (minOffset == 0 || minOffset > offset) {
                    if (maxOffset - offset < maxRange) {
                        minOffset = offset;
                    } else {
                        nOutliers++;
                    }
                }
                if (maxOffset == 0 || maxOffset < offset) {
                    if (offset - minOffset < maxRange) {
                        maxOffset = offset;
                    } else {
                        nOutliers++;
                    }
                }
            }
        }
    }
    
    public long getMinOffset() {
        return this.minOffset;
    }
    
    public long getMaxOffset() {
        return this.maxOffset;
    }
    
    public int getNumOutliers() {
        return this.nOutliers;
    }
    
    public long getTiTimeOffset() {
        final long offsetRange = maxOffset - minOffset;
        if (offsetRange > minRange && nOutliers < maxOutliers) {
            return minOffset;
        } else {
            return 0L;
        }
    }

    public void updateTriggerConfig(final TriggerConfig triggerConfig) {
        long tiTimeOffset = getTiTimeOffset();
        triggerConfig.put(TriggerConfigVariable.TI_TIME_OFFSET, tiTimeOffset);
    }
}
