package org.lcsim.hps.evio;

import java.util.List;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.DataType;
import org.jlab.coda.jevio.EventBuilder;
import org.jlab.coda.jevio.EvioBank;
import org.jlab.coda.jevio.EvioException;
import org.lcsim.event.EventHeader;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: TriggerDataWriter.java,v 1.1 2012/08/03 23:14:39 meeg Exp $
 */
public class TriggerDataWriter implements HitWriter {

    @Override
    public boolean hasData(EventHeader event) {
        return event.hasCollection(TriggerData.class, TriggerData.TRIG_COLLECTION);
    }

    @Override
    public void writeData(EventHeader event, EventBuilder builder) {
        // Make a new bank for this crate.
        BaseStructure topBank = null;
        // Does this bank already exist?
        for (BaseStructure bank : builder.getEvent().getChildren()) {
            if (bank.getHeader().getTag() == EventConstants.ECAL_TOP_BANK_TAG) {
                topBank = bank;
                break;
            }
        }
        // If they don't exist, make them.
        if (topBank == null) {
            topBank = new EvioBank(EventConstants.ECAL_TOP_BANK_TAG, DataType.BANK, EventConstants.ECAL_BANK_NUMBER);
            try {
                builder.addChild(builder.getEvent(), topBank);
            } catch (EvioException e) {
                throw new RuntimeException(e);
            }
        }


        List<TriggerData> triggerList = event.get(TriggerData.class, TriggerData.TRIG_COLLECTION);
        EvioBank triggerBank = new EvioBank(EventConstants.TRIGGER_BANK_TAG, DataType.UINT32, EventConstants.TRIGGER_BANK_NUMBER);
        try {
            triggerBank.appendIntData(triggerList.get(0).getBank());
            builder.addChild(topBank, triggerBank);
        } catch (EvioException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writeData(EventHeader event, EventHeader toEvent) {
        toEvent.put(TriggerData.TRIG_COLLECTION, event.get(TriggerData.class, TriggerData.TRIG_COLLECTION));
    }
}
