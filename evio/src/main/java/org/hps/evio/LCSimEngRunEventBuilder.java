package org.hps.evio;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.recon.ecal.triggerbank.AbstractIntData;
import org.hps.recon.ecal.triggerbank.SSPData;
import org.hps.recon.ecal.triggerbank.TIData;
import org.hps.record.epics.EpicsEvioProcessor;
import org.hps.record.epics.EpicsScalarData;
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
    
    TriggerConfigEvioReader triggerConfigReader = null;
    
    EpicsEvioProcessor epicsProcessor = new EpicsEvioProcessor();
    EpicsScalarData epicsData;
    
    public LCSimEngRunEventBuilder() {        
        ecalReader.setTopBankTag(0x25);
        ecalReader.setBotBankTag(0x27);        
        svtReader = new SvtEvioReader();         
        sspCrateBankTag = 0x2E; //A.C. modification after Sergey's confirmation
        sspBankTag = 0xe10c;
        intBanks = new ArrayList<IntBankDefinition>();
        intBanks.add(new IntBankDefinition(SSPData.class, new int[]{sspCrateBankTag, sspBankTag}));
        intBanks.add(new IntBankDefinition(TIData.class, new int[]{sspCrateBankTag, 0xe10a}));
        // ecalReader = new ECalEvioReader(0x25, 0x27);        
        triggerConfigReader = new TriggerConfigEvioReader();
    }

    @Override
    protected long getTime(List<AbstractIntData> triggerList) {
        for (AbstractIntData data : triggerList) {
            if (data instanceof TIData) {
                TIData tiData = (TIData) data;
                return tiData.getTime();
            }
        }
        return 0;
    }
    
    @Override
    public void readEvioEvent(EvioEvent evioEvent) {
        super.readEvioEvent(evioEvent);
        if (EvioEventUtilities.isEpicsEvent(evioEvent)) {
            createEpicsScalarData(evioEvent);
        }
    }
    
    void createEpicsScalarData(EvioEvent evioEvent) {
        epicsProcessor.process(evioEvent);
        epicsData = epicsProcessor.getEpicsScalarData();
    }

    @Override
    public EventHeader makeLCSimEvent(EvioEvent evioEvent) {
        if (!EvioEventUtilities.isPhysicsEvent(evioEvent)) {
            throw new RuntimeException("Not a physics event: event tag " + evioEvent.getHeader().getTag());
        }

        // Create a new LCSimEvent.
        EventHeader lcsimEvent = getEventData(evioEvent);
     
        // Put DAQ Configuration info into lcsimEvent
        triggerConfigReader.getDAQConfig(evioEvent,lcsimEvent);
        
        // Make RawCalorimeterHit collection, combining top and bottom section
        // of ECal into one list.
        try {
            ecalReader.makeHits(evioEvent, lcsimEvent);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error making ECal hits", e);
        }
        
        // Write EPICS bank into the subsequent physics event.
        if (epicsData != null) {
            epicsData.write(lcsimEvent);
            epicsData = null;
        }

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