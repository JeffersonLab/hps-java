package org.hps.evio;

import java.io.IOException;

import org.jlab.coda.jevio.DataType;
import org.jlab.coda.jevio.EventBuilder;
import org.jlab.coda.jevio.EventWriter;
import org.jlab.coda.jevio.EvioBank;
import org.jlab.coda.jevio.EvioException;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

/**
 * This class takes raw data generated from MC and converts it to EVIO. The goal
 * is to make this look like data which will come off the actual ET ring during
 * the test run.
 *
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class TestRunReconToEvio extends Driver {

	EventWriter writer;
	String rawCalorimeterHitCollectionName = "EcalDigitizedHits";
	String evioOutputFile = "TestRunData.evio";
	EventBuilder builder = null;
	private int eventsWritten = 0;
	ECalHitWriter ecalWriter = null;
	SVTHitWriter svtWriter = null;

	public TestRunReconToEvio() {
	}

	public void setEvioOutputFile(String evioOutputFile) {
		this.evioOutputFile = evioOutputFile;
	}

	public void setRawCalorimeterHitCollectionName(String rawCalorimeterHitCollectionName) {
		this.rawCalorimeterHitCollectionName = rawCalorimeterHitCollectionName;
		if (ecalWriter != null) {
			ecalWriter.setHitCollectionName(rawCalorimeterHitCollectionName);
		}
	}

	protected void startOfData() {
		try {
			writer = new EventWriter(evioOutputFile);
		} catch (EvioException e) {
			throw new RuntimeException(e);
		}

		ecalWriter = new ECalHitWriter();
		ecalWriter.setHitCollectionName(rawCalorimeterHitCollectionName);

		svtWriter = new SVTHitWriter();
	}

	protected void endOfData() {
		System.out.println(this.getClass().getSimpleName() + " - wrote " + eventsWritten + " EVIO events in job.");
		writer.close();		
	}

	protected void process(EventHeader event) {

		if (!svtWriter.hasData(event)) {
			return;
		}

		// Make a new EVIO event.
		builder = new EventBuilder(0, DataType.BANK, event.getEventNumber());

		// Write SVTData.
		svtWriter.writeData(event, builder);

		// Write RawCalorimeterHit collection.
		ecalWriter.writeData(event, builder);
//		writeRawCalorimeterHits(event);

		// Write this EVIO event.
		writeEvioEvent();
	}

	private void writeEvioEvent() {
		EvioBank eventIDBank = new EvioBank(EventConstants.EVENTID_BANK_TAG, DataType.UINT32, 0);
		int[] eventID = new int[3];
		eventID[0] = eventsWritten;
		eventID[1] = 0; //trigger type
		eventID[2] = 0; //status

		try {
			eventIDBank.appendIntData(eventID);
			builder.addChild(builder.getEvent(), eventIDBank);
		} catch (EvioException e) {
			throw new RuntimeException(e);
		}
		builder.setAllHeaderLengths();
		try {
			writer.writeEvent(builder.getEvent());
			++eventsWritten;
		} catch (EvioException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}