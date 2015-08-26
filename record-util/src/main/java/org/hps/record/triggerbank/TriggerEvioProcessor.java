package org.hps.record.triggerbank;

import org.hps.record.evio.EvioEventProcessor;
import org.hps.record.run.TriggerConfig;
import org.hps.record.triggerbank.AbstractIntData.IntBankDefinition;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: $
 */
public class TriggerEvioProcessor extends EvioEventProcessor {

    private final IntBankDefinition headBankDefinition;
    private final IntBankDefinition tiBankDefinition;
    private long minOffset = 0;
    private long maxOffset = 0;

    public TriggerEvioProcessor() {
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

    public TriggerConfig getTriggerConfig() {
        TriggerConfig triggerConfig = new TriggerConfig();
        long offsetRange = maxOffset - minOffset;
        if (offsetRange > 0.99 * 1e9 && offsetRange < 1.01 * 1e9) {
            triggerConfig.setTiTimeOffset(minOffset);
        } else {
            triggerConfig.setTiTimeOffset(0);
        }
        return triggerConfig;
    }
}
