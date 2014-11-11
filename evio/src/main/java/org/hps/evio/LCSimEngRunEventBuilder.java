package org.hps.evio;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.readout.ecal.SSPData;
import org.hps.readout.ecal.TriggerData;
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
		sspCrateBankTag = 0x25;
		sspBankTag = 0xe10c;
		// ecalReader = new ECalEvioReader(0x25, 0x27);
		// svtReader = new SVTEvioReader();
	}

	protected TriggerData makeTriggerData(int[] data) {
		TriggerData triggerData = new SSPData(data);
		time = ((long) triggerData.getTime()) * 4;
		return triggerData;
	}

	@Override
	public EventHeader makeLCSimEvent(EvioEvent evioEvent) {
		if (!isPhysicsEvent(evioEvent)) {
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
