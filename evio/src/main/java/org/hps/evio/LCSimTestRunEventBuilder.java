package org.hps.evio;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.record.LCSimEventBuilder;
import org.hps.record.evio.EvioEventConstants;
import org.hps.record.evio.EvioEventUtilities;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.AbstractIntData.IntBankDefinition;
import org.hps.record.triggerbank.TestRunTriggerData;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.conditions.ConditionsEvent;
import org.lcsim.conditions.ConditionsListener;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseLCSimEvent;

/**
 * Build LCSim events from Test Run 2012 EVIO data.
 */
public class LCSimTestRunEventBuilder implements LCSimEventBuilder, ConditionsListener {

    protected EcalEvioReader ecalReader = null;
    protected AbstractSvtEvioReader svtReader = null;
    protected long time = 0; //most recent event time (ns), taken from prestart and end events, and trigger banks (if any)
    protected int sspCrateBankTag = 0x1; //bank ID of the crate containing the SSP
    protected int sspBankTag = 0xe106; //SSP bank's tag
    protected static Logger LOGGER = Logger.getLogger(LCSimTestRunEventBuilder.class.getPackage().getName());
    protected List<IntBankDefinition> intBanks = null;

    public LCSimTestRunEventBuilder() {
        ecalReader = new EcalEvioReader(0x1, 0x2);
        svtReader = new TestRunSvtEvioReader();
        intBanks = new ArrayList<IntBankDefinition>();
        intBanks.add(new IntBankDefinition(TestRunTriggerData.class, new int[]{sspCrateBankTag, sspBankTag}));
    }

    public void setEcalHitCollectionName(String ecalHitCollectionName) {
        ecalReader.setHitCollectionName(ecalHitCollectionName);
    }

    @Override
    public void readEvioEvent(EvioEvent evioEvent) {
        if (EvioEventUtilities.isSyncEvent(evioEvent)) {
            int[] data = EvioEventUtilities.getControlEventData(evioEvent);
            int seconds = data[0];
            LOGGER.info("Sync event: time " + seconds + " - " + new Date(((long) seconds) * 1000) + ", event count since last sync " + data[1] + ", event count so far " + data[2] + ", status " + data[3]);
        } else if (EvioEventUtilities.isPreStartEvent(evioEvent)) {
            int[] data = EvioEventUtilities.getControlEventData(evioEvent);
            if (data != null) {
                int seconds = data[0];
                time = ((long) seconds) * 1000000000;
                int run = data[1];
                LOGGER.info("Prestart event: time " + seconds + " - " + new Date(((long) seconds) * 1000) + ", run " + run + ", run type " + data[2]);
            }
        } else if (EvioEventUtilities.isGoEvent(evioEvent)) {
            int[] data = EvioEventUtilities.getControlEventData(evioEvent);
            if (data != null) {
                int seconds = data[0];
                time = ((long) seconds) * 1000000000;
                LOGGER.info("Go event: time " + seconds + " - " + new Date(((long) seconds) * 1000) + ", event count so far " + data[2]);
            }
        } else if (EvioEventUtilities.isPauseEvent(evioEvent)) {
            int[] data = EvioEventUtilities.getControlEventData(evioEvent);
            int seconds = data[0];
            time = ((long) seconds) * 1000000000;
            LOGGER.info("Pause event: time " + seconds + " - " + new Date(((long) seconds) * 1000) + ", event count so far " + data[2]);
        } else if (EvioEventUtilities.isEndEvent(evioEvent)) {
            int[] data = EvioEventUtilities.getControlEventData(evioEvent);
            int seconds = data[0];
            time = ((long) seconds) * 1000000000;
            //run = 0;
            LOGGER.info("End event: time " + seconds + " - " + new Date(((long) seconds) * 1000) + ", event count " + data[2]);
        }
    }

    @Override
    public EventHeader makeLCSimEvent(EvioEvent evioEvent) {
        if (!EvioEventUtilities.isPhysicsEvent(evioEvent)) {
            throw new RuntimeException("Not a physics event: event tag " + evioEvent.getHeader().getTag());
        }

        // Create a new LCSimEvent.
        EventHeader lcsimEvent = getEventData(evioEvent);

        // Make RawCalorimeterHit collection, combining top and bottom section of ECal into one list.
        try {
            ecalReader.makeHits(evioEvent, lcsimEvent);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error making ECal hits", e);
        }

        // Make SVT RawTrackerHits
        try {
            svtReader.makeHits(evioEvent, lcsimEvent);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error making SVT hits", e);
        }

        return lcsimEvent;
    }

    protected EventHeader getEventData(EvioEvent evioEvent) {
        int[] eventID = null;
        //array of length 3: {event number, trigger code, readout status}

        List<AbstractIntData> triggerList = getTriggerData(evioEvent);

        if (evioEvent.getChildCount() > 0) {
            for (BaseStructure bank : evioEvent.getChildrenList()) {
                if (bank.getHeader().getTag() == EvioEventConstants.EVENTID_BANK_TAG) {
                    eventID = bank.getIntData();
                }
            }
        }

        if (eventID == null) {
            // FIXME: Should be a fatal error if this happens?  JM
            LOGGER.warning("no event ID bank found");
            eventID = new int[3];
        } else {
            LOGGER.finest("read EVIO event number " + eventID[0]);
            // Stop hardcoding event tags.
            if (eventID[2] != 0) {
                LOGGER.warning("Readout status is usually 0 but got " + eventID[2]);
            }
        }

        time = getTime(triggerList);
        
        if (eventID[0] != evioEvent.getEventNumber()) {
            LOGGER.finest("EVIO event number " + evioEvent.getEventNumber() + " does not match " + eventID[0] + " from event ID bank");
        }
        
        // Create a new LCSimEvent.
        EventHeader lcsimEvent = new BaseLCSimEvent(
                ConditionsManager.defaultInstance().getRun(),
                eventID[0],
                // FIXME: This should be used instead for event number.  JM
                // evioEvent.getEventNumber(),
                ConditionsManager.defaultInstance().getDetector(),
                time);

        lcsimEvent.put("TriggerBank", triggerList, AbstractIntData.class, 0);
        
        return lcsimEvent;
    }

    protected long getTime(List<AbstractIntData> triggerList) {
        for (AbstractIntData data : triggerList) {
            if (data instanceof TestRunTriggerData) {
                return (((TestRunTriggerData) data).getTime()) * 1000000000L;
            }
        }
        return 0;
    }

    protected List<AbstractIntData> getTriggerData(EvioEvent evioEvent) {
        List<AbstractIntData> triggerList = new ArrayList<AbstractIntData>();

        for (IntBankDefinition def : intBanks) {
            BaseStructure bank = def.findBank(evioEvent);
            if (bank != null) { //returns null if no banks found
                try {
                    AbstractIntData data = (AbstractIntData) def.getDataClass().getConstructor(int[].class).newInstance(bank.getIntData());
                    triggerList.add(data);
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                }
            } else {
                LOGGER.finest("No trigger bank found of type " + def.getDataClass().getSimpleName());
            }
        }
        return triggerList;
    }

//    protected TriggerData makeTriggerData(int[] data) {
//        TriggerData triggerData = new TriggerData(data);
//        time = ((long) triggerData.getTime()) * 1000000000;
//        return triggerData;
//    }
    @Override
    public void conditionsChanged(ConditionsEvent conditionsEvent) {
        ecalReader.initialize();
    }
}
