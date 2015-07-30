package org.hps.evio;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.recon.ecal.triggerbank.AbstractIntData;
import org.hps.recon.ecal.triggerbank.HeadBankData;
import org.hps.recon.ecal.triggerbank.SSPData;
import org.hps.recon.ecal.triggerbank.TDCData;
import org.hps.recon.ecal.triggerbank.TIData;
import org.hps.record.epics.EpicsData;
import org.hps.record.epics.EpicsEvioProcessor;
import org.hps.record.evio.EventTagBitMask;
import org.hps.record.evio.EvioEventUtilities;
import org.hps.record.scalers.ScalerData;
import org.hps.record.scalers.ScalerParameters;
import org.hps.record.scalers.ScalersEvioProcessor;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.event.EventHeader;
import org.lcsim.util.log.DefaultLogFormatter;
import org.lcsim.util.log.LogUtil;

/**
 * This is the {@link org.hps.record.LCSimEventBuilder} implementation for the Engineering Run and the Commissioning Run
 * for converting EVIO to LCIO events.
 * <p>
 * It has several modifications from the Test Run builder including different values for certain bank tags.
 * <p>
 * Additionally, this builder will write DAQ config information, EPICS control data, and scalar bank data into the
 * output LCSim events if these banks are present in the EVIO data.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class LCSimEngRunEventBuilder extends LCSimTestRunEventBuilder {

    private static final Logger LOGGER = LogUtil.create(LCSimEngRunEventBuilder.class, new DefaultLogFormatter(),
            Level.INFO);

    private EpicsData epicsData;

    private final EpicsEvioProcessor epicsProcessor = new EpicsEvioProcessor();

    private ScalerData scalerData;

    private final ScalersEvioProcessor scalerProcessor = new ScalersEvioProcessor();

    private TriggerConfigEvioReader triggerConfigReader = null;

    public LCSimEngRunEventBuilder() {
        ecalReader.setTopBankTag(0x25);
        ecalReader.setBotBankTag(0x27);
        ecalReader.setRfBankTag(0x2e);
        svtReader = new SvtEvioReader();
        sspCrateBankTag = 0x2E; // A.C. modification after Sergey's confirmation
        sspBankTag = 0xe10c;
        intBanks = new ArrayList<IntBankDefinition>();
        intBanks.add(new IntBankDefinition(SSPData.class, new int[] {sspCrateBankTag, sspBankTag}));
        intBanks.add(new IntBankDefinition(TIData.class, new int[] {sspCrateBankTag, 0xe10a}));
        intBanks.add(new IntBankDefinition(HeadBankData.class, new int[] {sspCrateBankTag, 0xe10f}));
        intBanks.add(new IntBankDefinition(TDCData.class, new int[] {0x3a, 0xe107}));
        // ecalReader = new ECalEvioReader(0x25, 0x27);
        triggerConfigReader = new TriggerConfigEvioReader();
    }

    /**
     * Create and cache an {@link org.hps.record.epics.EpicsData} object.
     *
     * @param evioEvent The EVIO event data.
     */
    private void createEpicsData(final EvioEvent evioEvent) {
        epicsProcessor.process(evioEvent);
        epicsData = epicsProcessor.getEpicsData();
    }

    @Override
    protected long getTime(final List<AbstractIntData> triggerList) {
        for (final AbstractIntData data : triggerList) {
            if (data instanceof TIData) {
                final TIData tiData = (TIData) data;
                return tiData.getTime();
            }
        }
        return 0;
    }

    @Override
    public EventHeader makeLCSimEvent(final EvioEvent evioEvent) {

        LOGGER.finest("creating LCSim event from EVIO event " + evioEvent.getEventNumber());

        if (!EvioEventUtilities.isPhysicsEvent(evioEvent)) {
            throw new RuntimeException("Not a physics event: event tag " + evioEvent.getHeader().getTag());
        }

        // Create a new LCSimEvent.
        final EventHeader lcsimEvent = this.getEventData(evioEvent);
        LOGGER.finest("created new LCSim event " + lcsimEvent.getEventNumber());

        // Put DAQ Configuration info into lcsimEvent.
        triggerConfigReader.getDAQConfig(evioEvent, lcsimEvent);

        // Make RawCalorimeterHit collection, combining top and bottom section
        // of ECal into one list.
        try {
            ecalReader.makeHits(evioEvent, lcsimEvent);
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Error making ECal hits.", e);
        }

        // Make SVT RawTrackerHits.
        try {
            svtReader.makeHits(evioEvent, lcsimEvent);
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Error making SVT hits.", e);
        }

        // Write the current EPICS data into this event.
        this.writeEpicsData(lcsimEvent);

        // Write scalers into the event, if they exist in the EVIO data.
        this.writeScalerData(evioEvent, lcsimEvent);

        // Write scaler parameters into every event header.
        if (EventTagBitMask.SYNC.isEventTag(evioEvent)) {
            LOGGER.fine("event " + evioEvent.getEventNumber() + " is a sync event");
        }
        this.writeScalerParameters(lcsimEvent);

        return lcsimEvent;
    }

    @Override
    public void readEvioEvent(final EvioEvent evioEvent) {
        super.readEvioEvent(evioEvent);

        // Create EPICS data if this is an EPICS control event.
        if (EvioEventUtilities.isEpicsEvent(evioEvent)) {
            LOGGER.fine("creating data from EPICS event");
            this.createEpicsData(evioEvent);
        }
    }

    /**
     * Write {@link org.hps.record.epics.EpicsData} into the event.
     *
     * @param lcsimEvent the lcsim event
     */
    private void writeEpicsData(final EventHeader lcsimEvent) {
        if (epicsProcessor.getEpicsData() != null) {
            LOGGER.fine("writing EPICS data to lcsim event " + lcsimEvent.getEventNumber());
            epicsProcessor.getEpicsData().write(lcsimEvent);
            epicsProcessor.reset();
        }
    }

    /**
     * Write EVIO scaler data into the LCSim event, if it exists.
     *
     * @param evioEvent The EVIO event data.
     * @param lcsimEvent The output LCSim event.
     */
    private void writeScalerData(final EvioEvent evioEvent, final EventHeader lcsimEvent) {

        // Find scaler data in EVIO.
        scalerProcessor.process(evioEvent);

        if (scalerProcessor.getScalerData() != null) {

            LOGGER.fine("writing scaler data to lcsim event " + lcsimEvent.getEventNumber() + " from EVIO event "
                    + evioEvent.getEventNumber());

            // Write the current scalar data to the event.
            scalerProcessor.getScalerData().write(lcsimEvent);

            // Save the current scaler data for writing to lcsim event header.
            scalerData = scalerProcessor.getScalerData();
        }
    }

    /**
     * Write {@link org.hps.record.scalers.ScalerParameters} into the event.
     *
     * @param lcsimEvent the lcsim event
     */
    private void writeScalerParameters(final EventHeader lcsimEvent) {
        final ScalerParameters scalerParameters = new ScalerParameters();
        if (epicsData != null) {
            scalerParameters.readEpicsData(epicsData);
        }
        if (scalerData != null) {
            scalerParameters.readScalerData(scalerData);
        }
        scalerParameters.write(lcsimEvent);
    }
}
