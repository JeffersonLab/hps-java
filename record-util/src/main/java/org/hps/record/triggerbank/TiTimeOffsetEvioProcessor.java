package org.hps.record.triggerbank;

import org.hps.record.evio.EvioEventProcessor;
import org.hps.record.triggerbank.AbstractIntData.IntBankDefinition;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;

/**
 * Extract trigger time offset from EVIO data.
 * 
 * @author Sho Uemura <meeg@slac.stanford.edu>
 */
public class TiTimeOffsetEvioProcessor extends EvioEventProcessor {

    private final IntBankDefinition headBankDefinition;
    private final IntBankDefinition tiBankDefinition;
    private long minOffset = 0;
    private long maxOffset = 0;

    public TiTimeOffsetEvioProcessor() {
        headBankDefinition = new IntBankDefinition(HeadBankData.class, new int[]{0x2e, 0xe10f});
        tiBankDefinition = new IntBankDefinition(TIData.class, new int[]{0x2e, 0xe10a});
    }

    @Override
    public void process(final EvioEvent evioEvent) {
        BaseStructure headBank = headBankDefinition.findBank(evioEvent);
        BaseStructure tiBank = tiBankDefinition.findBank(evioEvent);
        if (headBank != null && tiBank != null) {
            int[] headData = headBank.getIntData();
            int thisTimestamp = headData[3];
            TIData tiData = new TIData(tiBank.getIntData());
            if (thisTimestamp != 0) {
                long offset = thisTimestamp * 1000000000L - tiData.getTime();
                if (minOffset == 0 || minOffset > offset) {
                    minOffset = offset;
                }
                if (maxOffset == 0 || maxOffset < offset) {
                    maxOffset = offset;
                }
            }
        }
    }
    
    public void updateTriggerConfig(TriggerConfigInt triggerConfig) {
        long offsetRange = maxOffset - minOffset;
        if (offsetRange > 0.99 * 1e9 && offsetRange < 1.01 * 1e9) {
            triggerConfig.put(TriggerConfigVariable.TI_TIME_OFFSET.name(), minOffset);
        } else {
            triggerConfig.put(TriggerConfigVariable.TI_TIME_OFFSET.name(), 0L);
        }
    }
}
