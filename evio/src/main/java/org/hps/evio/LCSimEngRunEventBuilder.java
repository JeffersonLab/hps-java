package org.hps.evio;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hps.readout.ecal.triggerbank.SSPData;
import org.hps.readout.ecal.triggerbank.TIData;
import org.hps.readout.ecal.triggerbank.AbstractIntData;
import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.event.EventHeader;

/**
 * Build LCSim events from EVIO data.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class LCSimEngRunEventBuilder extends LCSimTestRunEventBuilder {

    public LCSimEngRunEventBuilder() {
        ecalReader.setTopBankTag(0x25);
        ecalReader.setBotBankTag(0x27);
        sspCrateBankTag = 0x2E; //A.C. modification after Sergey's confirmation
        sspBankTag = 0xe10c;
        intBanks = new ArrayList<IntBankDefinition>();
        intBanks.add(new IntBankDefinition(SSPData.class, new int[]{sspCrateBankTag, sspBankTag}));
        intBanks.add(new IntBankDefinition(TIData.class, new int[]{sspCrateBankTag, 0xe10a}));
        // ecalReader = new ECalEvioReader(0x25, 0x27);
        // svtReader = new SVTEvioReader();
    }

    @Override
    protected long getTime(List<AbstractIntData> triggerList) {
        for (AbstractIntData data : triggerList) {
            if (data instanceof TIData) {
                TIData tiData = (TIData) data;
                return tiData.getTime() * 4;
            }
        }
        return 0;
    }

    @Override
    public EventHeader makeLCSimEvent(EvioEvent evioEvent) {
        if (!EvioEventUtilities.isPhysicsEvent(evioEvent)) {
            throw new RuntimeException("Not a physics event: event tag " + evioEvent.getHeader().getTag());
        }

        // Create a new LCSimEvent.
        EventHeader lcsimEvent = getEventData(evioEvent);

        // Make RawCalorimeterHit collection, combining top and bottom section
        // of ECal into one list.
        try {
            ecalReader.makeHits(evioEvent, lcsimEvent);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error making ECal hits", e);
        }

        // Commented out for now while SVT is not implemented.  --JM
        // Make SVT RawTrackerHits
        // try {
        // svtReader.makeHits(evioEvent, lcsimEvent);
        // } catch (Exception e) {
        // Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,
        // "Error making SVT hits", e);
        // }
        return lcsimEvent;
    }
}
