package org.hps.evio;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.recon.ecal.triggerbank.AbstractIntData;
import org.hps.recon.ecal.triggerbank.SSPData;
import org.hps.recon.ecal.triggerbank.TIData;
import org.hps.recon.ecal.triggerbank.TDCData;
import org.hps.record.epics.EpicsEvioProcessor;
import org.hps.record.epics.EpicsScalarData;
import org.hps.record.evio.EvioEventUtilities;
import org.hps.record.scalars.ScalarData;
import org.hps.record.scalars.ScalarsEvioProcessor;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.event.EventHeader;

/**
 * This is the {@link org.hps.record.LCSimEventBuilder} implementation for the
 * Engineering Run and the Commissioning Run.
 * <p>
 * It has several modifications from the Test Run builder including different
 * values for certain bank tags.
 * <p>
 * Additionally, this builder will write DAQ config information, EPICS control
 * data, and scalar bank data into the output LCSim events if these banks are
 * present in the EVIO data.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class LCSimEngRunEventBuilder extends LCSimTestRunEventBuilder {

    TriggerConfigEvioReader triggerConfigReader = null;

    EpicsEvioProcessor epicsProcessor = new EpicsEvioProcessor();
    EpicsScalarData epicsData;

    ScalarsEvioProcessor scalarProcessor = new ScalarsEvioProcessor();
    ScalarData scalarData;

    public LCSimEngRunEventBuilder() {
        ecalReader.setTopBankTag(0x25);
        ecalReader.setBotBankTag(0x27);
        ecalReader.setRfBankTag(0x2e);
        svtReader = new SvtEvioReader();
        sspCrateBankTag = 0x2E; // A.C. modification after Sergey's confirmation
        sspBankTag = 0xe10c;
        intBanks = new ArrayList<IntBankDefinition>();
        intBanks.add(new IntBankDefinition(SSPData.class, new int[]{sspCrateBankTag, sspBankTag}));
        intBanks.add(new IntBankDefinition(TIData.class, new int[]{sspCrateBankTag, 0xe10a}));
        intBanks.add(new IntBankDefinition(TDCData.class, new int[]{0x2d, 0xe107}));
        intBanks.add(new IntBankDefinition(TDCData.class, new int[]{0x3a, 0xe107}));
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

        // Create EPICS data if this is an EPICS control event.
        if (EvioEventUtilities.isEpicsEvent(evioEvent)) {
            createEpicsScalarData(evioEvent);
        }
    }

    /**
     * Create and cache an {@link org.hps.record.epics.EpicsScalarData} object.
     *
     * @param evioEvent The EVIO event data.
     */
    void createEpicsScalarData(EvioEvent evioEvent) {
        epicsProcessor.process(evioEvent);
        epicsData = epicsProcessor.getEpicsScalarData();
    }

    /**
     * Write EVIO scalar data into the LCSim event, if it exists.
     *
     * @param evioEvent The EVIO event data.
     * @param lcsimEvent The output LCSim event.
     */
    void writeScalarData(EvioEvent evioEvent, EventHeader lcsimEvent) {
        scalarProcessor.process(evioEvent);
        if (scalarProcessor.getScalarData() != null) {
            scalarProcessor.getScalarData().write(lcsimEvent);
        }
    }

    @Override
    public EventHeader makeLCSimEvent(EvioEvent evioEvent) {
        if (!EvioEventUtilities.isPhysicsEvent(evioEvent)) {
            throw new RuntimeException("Not a physics event: event tag " + evioEvent.getHeader().getTag());
        }

        // Create a new LCSimEvent.
        EventHeader lcsimEvent = getEventData(evioEvent);

        // Put DAQ Configuration info into lcsimEvent.
        triggerConfigReader.getDAQConfig(evioEvent, lcsimEvent);

        // Make RawCalorimeterHit collection, combining top and bottom section
        // of ECal into one list.
        try {
            ecalReader.makeHits(evioEvent, lcsimEvent);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error making ECal hits", e);
        }

        // FIXME: This is commented out for now while SVT is not integrated.
        // Make SVT RawTrackerHits.
        // try {
        // svtReader.makeHits(evioEvent, lcsimEvent);
        // } catch (Exception e) {
        // Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error making SVT hits", e);
        // }
        // Write the current EPICS data into this event.
        if (epicsData != null) {
            epicsData.write(lcsimEvent);
            epicsData = null;
        }

        // Write scalars into the event, if they exist in this EVIO data.
        writeScalarData(evioEvent, lcsimEvent);

        return lcsimEvent;
    }
}
