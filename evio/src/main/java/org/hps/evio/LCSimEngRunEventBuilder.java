package org.hps.evio;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.trigger.TiTimeOffset;
import org.hps.record.epics.EpicsData;
import org.hps.record.epics.EpicsEvioProcessor;
import org.hps.record.evio.EvioEventUtilities;
import org.hps.record.scalers.ScalerData;
import org.hps.record.scalers.ScalersEvioProcessor;
import org.hps.record.svt.SvtEvioExceptions.SvtEvioReaderException;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.AbstractIntData.IntBankDefinition;
import org.hps.record.triggerbank.HeadBankData;
import org.hps.record.triggerbank.SSPData;
import org.hps.record.triggerbank.TDCData;
import org.hps.record.triggerbank.TIData;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.conditions.ConditionsEvent;
import org.lcsim.event.EventHeader;

/**
 * This is the {@link org.hps.record.LCSimEventBuilder} implementation for the
 * Engineering Run and the Commissioning Run for converting EVIO to LCIO events.
 * <p>
 * It has several modifications from the Test Run builder including different
 * values for certain bank tags.
 * <p>
 * Additionally, this builder will write DAQ config information, EPICS control
 * data, and scalar bank data into the output LCSim events if these banks are
 * present in the EVIO data.
 */
public class LCSimEngRunEventBuilder extends BaseEventBuilder {

    /**
     * Setup logger.
     */
    private static final Logger LOGGER = Logger.getLogger(LCSimEngRunEventBuilder.class.getPackage().getName());

    /**
     * EVIO processor for extracting EPICS data.
     */
    protected final EpicsEvioProcessor epicsProcessor = new EpicsEvioProcessor();

    /**
     * EVIO processor for extracting scaler data.
     */
    protected final ScalersEvioProcessor scalerProcessor = new ScalersEvioProcessor();

    /**
     * Writes event flags describing the SVT state.
     */
    protected SvtEventFlagger svtEventFlagger;

    /**
     * Reads trigger config.
     */
    protected TriggerConfigEvioReader triggerConfigReader = null;

    /**
     * Modulus of TI timestamp offset (units of nanoseconds).
     */
    private final long timestampCycle = 24 * 6 * 35;

    /**
     * The current TI time offset in nanoseconds from the run manager.
     */
    private Long currentTiTimeOffset = null;

    /**
     * Class constructor.
     */
    public LCSimEngRunEventBuilder() {
        ecalReader.setTopBankTag(0x25);
        ecalReader.setBotBankTag(0x27);
        ecalReader.setRfBankTag(0x2e);
        //
        // Note: The hodoReader has these initializations in LCSimTestRunEventBuilder.conditionsChanged
        //       Since the conditions tell us whether we have a hodoscope or not.
        //
        svtReader = new AugmentedSvtEvioReader();
        sspCrateBankTag = 0x2E; // A.C. modification after Sergey's confirmation
        sspBankTag = 0xe10c;
        intBanks = new ArrayList<IntBankDefinition>();
        intBanks.add(new IntBankDefinition(SSPData.class, new int[]{sspCrateBankTag, sspBankTag}));
        intBanks.add(new IntBankDefinition(TIData.class, new int[]{sspCrateBankTag, 0xe10a}));
        intBanks.add(new IntBankDefinition(HeadBankData.class, new int[]{sspCrateBankTag, 0xe10f}));
        intBanks.add(new IntBankDefinition(TDCData.class, new int[]{0x3a, 0xe107}));
        triggerConfigReader = new TriggerConfigEvioReader();
        svtEventFlagger = new SvtEventFlagger();
    }

    @Override
    public void conditionsChanged(final ConditionsEvent conditionsEvent) {

        super.conditionsChanged(conditionsEvent);
        if(svtEventFlagger !=null) svtEventFlagger.initialize();

        // Set TI time offset from run database.
        currentTiTimeOffset = Long.valueOf(0);
        DatabaseConditionsManager mgr = (DatabaseConditionsManager) conditionsEvent.getConditionsManager();
        if (mgr.hasConditionsRecord("ti_time_offsets")) {
            TiTimeOffset t = mgr.getCachedConditions(TiTimeOffset.class, "ti_time_offsets").getCachedData();
            currentTiTimeOffset = t.getValue();
        }
    }

    /**
     * Get the time from the TI data with time offset applied from run database.
     *
     * @param triggerList the TI data list
     */
    @Override
    protected long getTime(final List<AbstractIntData> triggerList) {
        long tiTimeOffset = 0;
        if (currentTiTimeOffset != null) {
            tiTimeOffset = (currentTiTimeOffset / timestampCycle) * timestampCycle;
        }
        for (final AbstractIntData data : triggerList) {
            if (data instanceof TIData) {
                final TIData tiData = (TIData) data;
                return tiData.getTime() + tiTimeOffset;
            }
        }
        return 0;
    }

    /**
     * Make an lcsim event from EVIO data.
     *
     * @param evioEvent the input EVIO event
     */
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
        try {
            triggerConfigReader.getDAQConfig(evioEvent, lcsimEvent);
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "DAQ CONFIG BROKEN.", e);
        }

        // Make RawCalorimeterHit collection, combining top and bottom section
        // of ECal into one list.
        try {
            ecalReader.makeHits(evioEvent, lcsimEvent);
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Error making ECal hits.", e);
        }

        // Make RawHodoscopeHit collection, combining top and bottom section
        // of Hodo into one list.
        try {
            if (hodoReader != null) {  // Skip if no hodoscope in this run period.
                hodoReader.makeHits(evioEvent, lcsimEvent);
            }
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Error making Hodo hits.", e);
        }

        // Make VTP collection, combining top and bottom section
        // into one list.
        try {
            vtpReader.makeHits(evioEvent, lcsimEvent);
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Error reading VTP bank", e);
        }
        
        // Make SVT RawTrackerHits.
        try {
            svtReader.makeHits(evioEvent, lcsimEvent);
        } catch (final SvtEvioReaderException e) {
            LOGGER.log(Level.SEVERE, "Error making SVT hits for run " + lcsimEvent.getRunNumber() + " event " + lcsimEvent.getEventNumber() + ". Don't stop!", e);
        }
        
        // Write the current EPICS data into this event.
        this.writeEpicsData(lcsimEvent);

        // Write scalers into the event, if they exist in the EVIO data.
        this.writeScalerData(evioEvent, lcsimEvent);

        //
        if (svtEventFlagger != null) this.svtEventFlagger.writeFlags(lcsimEvent);

        return lcsimEvent;
    }

    /**
     * Pre-read an EVIO event.
     *
     * @param evioEvent the EVIO event
     */
    @Override
    public void readEvioEvent(final EvioEvent evioEvent) {
        super.readEvioEvent(evioEvent);

        // Create EPICS data if this is an EPICS control event.
        if (EvioEventUtilities.isEpicsEvent(evioEvent)) {
            LOGGER.fine("creating data from EPICS event");
            epicsProcessor.process(evioEvent);
        }
    }

    /**
     * Write {@link org.hps.record.epics.EpicsData} into the event.
     *
     * @param lcsimEvent the lcsim event
     */
    private void writeEpicsData(final EventHeader lcsimEvent) {

        // Get EpicsData from processor that was already activated (usually it is null).
        final EpicsData epicsData = epicsProcessor.getEpicsData();

        // Was new EpicsData created?
        if (epicsData != null) {
            LOGGER.fine("writing EPICS data to lcsim event " + lcsimEvent.getEventNumber());

            // Write to the collection in the lcsim event.
            epicsData.write(lcsimEvent);

            // Reset the processor.
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

        // Activate the EVIO scalers processor.
        scalerProcessor.process(evioEvent);

        // Get ScalerData from the processor.
        final ScalerData scalerData = scalerProcessor.getCurrentScalerData();

        // Was new ScalerData created?
        if (scalerData != null) {

            LOGGER.fine("writing scaler data to lcsim event " + lcsimEvent.getEventNumber() + " from EVIO event "
                    + evioEvent.getEventNumber());

            // Write the current scalar data to the event.
            scalerProcessor.getCurrentScalerData().write(lcsimEvent);
        }
    }
}
