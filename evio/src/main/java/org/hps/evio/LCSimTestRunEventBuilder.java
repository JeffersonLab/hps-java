package org.hps.evio;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.readout.ecal.TriggerData;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.conditions.ConditionsEvent;
import org.lcsim.conditions.ConditionsListener;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseLCSimEvent;

/**
 * Build LCSim events from EVIO data.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: LCSimTestRunEventBuilder.java,v 1.24 2013/03/01 01:30:25 meeg
 * Exp $
 */
public class LCSimTestRunEventBuilder implements LCSimEventBuilder, ConditionsListener {

    // Names of subdetectors.
//    private String trackerName;
    // Detector conditions object.
	//protected Detector detector;
    // Debug flag.
	String detectorName = null;
    protected boolean debug = false;
    ECalEvioReader ecalReader = null;
    SVTEvioReader svtReader = null;
    protected int run = 0; //current run number, taken from prestart and end events
    protected long time = 0; //most recent event time (ns), taken from prestart and end events, and trigger banks (if any)
    protected int sspCrateBankTag = 0x1; //bank ID of the crate containing the SSP
    protected int sspBankTag = 0xe106; //SSP bank's tag

    public LCSimTestRunEventBuilder() {
        ecalReader = new ECalEvioReader(0x1, 0x2);
        svtReader = new SVTEvioReader();
    }

    @Override
    public void setDetectorName(String detectorName) {
    	this.detectorName = detectorName;
    }

    @Override
    public void setDebug(boolean debug) {
        this.debug = debug;
        ecalReader.setDebug(debug);
    }

    public void setEcalHitCollectionName(String ecalHitCollectionName) {
        ecalReader.setHitCollectionName(ecalHitCollectionName);
    }

    @Override
    public void readEvioEvent(EvioEvent evioEvent) {
        if (EventConstants.isSyncEvent(evioEvent)) {
            int[] data = evioEvent.getIntData();
            int seconds = data[0];
            System.out.println("Sync event: time " + seconds + " - " + new Date(((long) seconds) * 1000) + ", event count since last sync " + data[1] + ", event count so far " + data[2] + ", status " + data[3]);
        } else if (EventConstants.isPreStartEvent(evioEvent)) {
            int[] data = evioEvent.getIntData();
            int seconds = data[0];
            time = ((long) seconds) * 1000000000;
            run = data[1];
            System.out.println("Prestart event: time " + seconds + " - " + new Date(((long) seconds) * 1000) + ", run " + run + ", run type " + data[2]);
        } else if (EventConstants.isGoEvent(evioEvent)) {
            int[] data = evioEvent.getIntData();
            int seconds = data[0];
            time = ((long) seconds) * 1000000000;
            System.out.println("Go event: time " + seconds + " - " + new Date(((long) seconds) * 1000) + ", event count so far " + data[2]);
        } else if (EventConstants.isPauseEvent(evioEvent)) {
            int[] data = evioEvent.getIntData();
            int seconds = data[0];
            time = ((long) seconds) * 1000000000;
            System.out.println("Pause event: time " + seconds + " - " + new Date(((long) seconds) * 1000) + ", event count so far " + data[2]);
        } else if (EventConstants.isEndEvent(evioEvent)) {
            int[] data = evioEvent.getIntData();
            int seconds = data[0];
            time = ((long) seconds) * 1000000000;
            run = 0;
            System.out.println("End event: time " + seconds + " - " + new Date(((long) seconds) * 1000) + ", event count " + data[2]);
        }
    }

    @Override
    public EventHeader makeLCSimEvent(EvioEvent evioEvent) {
        if (!isPhysicsEvent(evioEvent)) {
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

    @Override
    public boolean isPhysicsEvent(EvioEvent evioEvent) {
        return (evioEvent.getHeader().getTag() == EventConstants.PHYSICS_EVENT_TAG);
    }

    protected EventHeader getEventData(EvioEvent evioEvent) {
        int[] eventID = null;
        //array of length 3: {event number, trigger code, readout status}

        List<TriggerData> triggerList = getTriggerData(evioEvent);

        if (evioEvent.getChildCount() > 0) {
            for (BaseStructure bank : evioEvent.getChildren()) {
                if (bank.getHeader().getTag() == EventConstants.EVENTID_BANK_TAG) {
                    eventID = bank.getIntData();
                }
            }
        }

        if (eventID == null) {
            System.out.println("No event ID bank found");
            eventID = new int[3];
        } else {
            if (debug) {
                System.out.println("Read EVIO event number " + eventID[0]);
            }
            if (eventID[1] != 1) {
                System.out.println("Trigger code is usually 1; got " + eventID[1]);
            }
            if (eventID[2] != 0) {
                System.out.println("Readout status is usually 0; got " + eventID[2]);
            }
        }

        if (triggerList.isEmpty()) {
            System.out.println("No trigger bank found");
        } else if (triggerList.size() > 1) {
            System.out.println("Found multiple trigger banks");
        }

        // Create a new LCSimEvent.
        //EventHeader lcsimEvent = new BaseLCSimEvent(run, eventID[0], detector.getDetectorName(), time);
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
