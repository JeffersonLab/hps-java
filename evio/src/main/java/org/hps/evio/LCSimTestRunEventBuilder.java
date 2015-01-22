package org.hps.evio;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hps.readout.ecal.triggerbank.AbstractIntData;
import org.hps.readout.ecal.triggerbank.TestRunTriggerData;
import org.hps.record.LCSimEventBuilder;
import org.hps.record.evio.EvioEventConstants;
import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.conditions.ConditionsEvent;
import org.lcsim.conditions.ConditionsListener;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseLCSimEvent;
import org.lcsim.util.log.LogUtil;

/**
 * Build LCSim events from Test Run 2012 EVIO data.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class LCSimTestRunEventBuilder implements LCSimEventBuilder, ConditionsListener {

    ECalEvioReader ecalReader = null;
    AbstractSvtEvioReader svtReader = null;
    protected long time = 0; //most recent event time (ns), taken from prestart and end events, and trigger banks (if any)
    protected int sspCrateBankTag = 0x1; //bank ID of the crate containing the SSP
    protected int sspBankTag = 0xe106; //SSP bank's tag
    protected static Logger logger = LogUtil.create(LCSimTestRunEventBuilder.class);
    protected List<IntBankDefinition> intBanks = null;

    public LCSimTestRunEventBuilder() {
        ecalReader = new ECalEvioReader(0x1, 0x2);
        svtReader = new TestRunSvtEvioReader();
        intBanks = new ArrayList<IntBankDefinition>();
        intBanks.add(new IntBankDefinition(TestRunTriggerData.class, new int[]{sspCrateBankTag, sspBankTag}));
        logger.setLevel(Level.FINE);
    }

    public void setEcalHitCollectionName(String ecalHitCollectionName) {
        ecalReader.setHitCollectionName(ecalHitCollectionName);
    }

    @Override
    public void readEvioEvent(EvioEvent evioEvent) {
        if (EvioEventUtilities.isSyncEvent(evioEvent)) {
            int[] data = EvioEventUtilities.getControlEventData(evioEvent);
            int seconds = data[0];
            logger.info("Sync event: time " + seconds + " - " + new Date(((long) seconds) * 1000) + ", event count since last sync " + data[1] + ", event count so far " + data[2] + ", status " + data[3]);
        } else if (EvioEventUtilities.isPreStartEvent(evioEvent)) {
            int[] data = EvioEventUtilities.getControlEventData(evioEvent);
            if (data != null) {
                int seconds = data[0];
                time = ((long) seconds) * 1000000000;
                int run = data[1];
                logger.info("Prestart event: time " + seconds + " - " + new Date(((long) seconds) * 1000) + ", run " + run + ", run type " + data[2]);
            }
        } else if (EvioEventUtilities.isGoEvent(evioEvent)) {
            int[] data = EvioEventUtilities.getControlEventData(evioEvent);
            if (data != null) {
                int seconds = data[0];
                time = ((long) seconds) * 1000000000;
                logger.info("Go event: time " + seconds + " - " + new Date(((long) seconds) * 1000) + ", event count so far " + data[2]);
            }
        } else if (EvioEventUtilities.isPauseEvent(evioEvent)) {
            int[] data = EvioEventUtilities.getControlEventData(evioEvent);
            int seconds = data[0];
            time = ((long) seconds) * 1000000000;
            logger.info("Pause event: time " + seconds + " - " + new Date(((long) seconds) * 1000) + ", event count so far " + data[2]);
        } else if (EvioEventUtilities.isEndEvent(evioEvent)) {
            int[] data = EvioEventUtilities.getControlEventData(evioEvent);
            int seconds = data[0];
            time = ((long) seconds) * 1000000000;
            //run = 0;
            logger.info("End event: time " + seconds + " - " + new Date(((long) seconds) * 1000) + ", event count " + data[2]);
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
            for (BaseStructure bank : evioEvent.getChildren()) {
                if (bank.getHeader().getTag() == EvioEventConstants.EVENTID_BANK_TAG) {
                    eventID = bank.getIntData();
                }
            }
        }

        if (eventID == null) {
            logger.warning("No event ID bank found");
            eventID = new int[3];
        } else {
            logger.finest("Read EVIO event number " + eventID[0]);
            if (eventID[1] != 1) {
                logger.warning("Trigger code is usually 1; got " + eventID[1]);
            }
            if (eventID[2] != 0) {
                logger.warning("Readout status is usually 0; got " + eventID[2]);
            }
        }

        time = getTime(triggerList);

        // Create a new LCSimEvent.
        EventHeader lcsimEvent = new BaseLCSimEvent(
                ConditionsManager.defaultInstance().getRun(),
                eventID[0],
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
                    AbstractIntData data = (AbstractIntData) def.dataClass.getConstructor(int[].class).newInstance(bank.getIntData());
                    triggerList.add(data);
                } catch (Exception ex) {
                    Logger.getLogger(LCSimTestRunEventBuilder.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                logger.finest("No trigger bank found of type " + def.dataClass.getSimpleName());
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

    protected class IntBankDefinition {

        int[] bankTags;
        Class<? extends AbstractIntData> dataClass;

        public IntBankDefinition(Class dataClass, int[] bankTags) {
            this.bankTags = bankTags;
            this.dataClass = dataClass;
        }

        public BaseStructure findBank(EvioEvent evioEvent) {
            BaseStructure currentBank = evioEvent;
            searchLoop:
            for (int bankTag : bankTags) {
                if (currentBank.getChildCount() > 0) {
                    for (BaseStructure childBank : currentBank.getChildren()) {
                        if (childBank.getHeader().getTag() == bankTag) { //found a bank with the right tag; step inside this bank and conitnue searching
                            currentBank = childBank;
                            continue searchLoop;
                        }
                    }
                    return null; //didn't find a bank with the right tag, give up
                } else { //bank has no children, give up
                    return null;
                }
            }
            return currentBank; // matched every tag, so this is the bank we want
        }
    }
}
