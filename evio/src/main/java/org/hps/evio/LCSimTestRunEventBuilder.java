package org.hps.evio;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.readout.ecal.TriggerData;
import org.hps.record.LCSimEventBuilder;
import org.hps.record.evio.EvioEventConstants;
import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.conditions.ConditionsEvent;
import org.lcsim.conditions.ConditionsListener;
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

    String detectorName = null;
    ECalEvioReader ecalReader = null;
    SVTEvioReader svtReader = null;
    protected int run = 0; //current run number, taken from prestart and end events
    protected long time = 0; //most recent event time (ns), taken from prestart and end events, and trigger banks (if any)
    protected int sspCrateBankTag = 0x1; //bank ID of the crate containing the SSP
    protected int sspBankTag = 0xe106; //SSP bank's tag
    protected static Logger logger = LogUtil.create(LCSimTestRunEventBuilder.class);

    public LCSimTestRunEventBuilder() {
        ecalReader = new ECalEvioReader(0x1, 0x2);
        svtReader = new SVTEvioReader();
        logger.setLevel(Level.FINE);
    }

    @Override
    public void setDetectorName(String detectorName) {
        this.detectorName = detectorName;
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
                run = data[1];
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
            run = 0;
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

        List<TriggerData> triggerList = getTriggerData(evioEvent);

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

        if (triggerList.isEmpty()) {
            logger.finest("No trigger bank found");
        } else if (triggerList.size() > 1) {
            logger.finest("Found multiple trigger banks");
        }

        // Create a new LCSimEvent.
        EventHeader lcsimEvent = new BaseLCSimEvent(run, eventID[0], detectorName, time);

        lcsimEvent.put("TriggerBank", triggerList, TriggerData.class, 0);
        return lcsimEvent;
    }

    protected List<TriggerData> getTriggerData(EvioEvent evioEvent) {
        List<TriggerData> triggerList = new ArrayList<TriggerData>();
        if (evioEvent.getChildCount() > 0) {
            for (BaseStructure bank : evioEvent.getChildren()) {
                if (bank.getHeader().getTag() == sspCrateBankTag) {
                    if (bank.getChildCount() > 0) {
                        for (BaseStructure slotBank : bank.getChildren()) {
                            if (slotBank.getHeader().getTag() == sspBankTag) {
//                                TriggerData triggerData = new TriggerData(slotBank.getIntData());
//                                time = ((long) triggerData.getTime()) * 1000000000;
                                triggerList.add(makeTriggerData(slotBank.getIntData()));
                            }
                        }
                    }
                }
            }
        }
        return triggerList;
    }

    protected TriggerData makeTriggerData(int[] data) {
        TriggerData triggerData = new TriggerData(data);
        time = ((long) triggerData.getTime()) * 1000000000;
        return triggerData;
    }

    @Override
    public void conditionsChanged(ConditionsEvent conditionsEvent) {
        ecalReader.initialize();
    }
}
