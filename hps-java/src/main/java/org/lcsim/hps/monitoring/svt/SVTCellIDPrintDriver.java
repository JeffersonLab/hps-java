package org.lcsim.hps.monitoring.svt;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.hps.recon.tracking.HPSSVTData;
import org.lcsim.util.Driver;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: HPSEcalDigitalPrintDriver.java,v 1.5 2012/04/27 22:13:52 meeg
 * Exp $
 */
public class SVTCellIDPrintDriver extends Driver {

	String rawTrackerHitCollectionName = "SVTData";
	String outputFileName;
	PrintWriter outputStream = null;

	public SVTCellIDPrintDriver() {
	}

	public void setRawTrackerHitCollectionName(String rawTrackerHitCollectionName) {
		this.rawTrackerHitCollectionName = rawTrackerHitCollectionName;
	}

	public void setOutputFileName(String outputFileName) {
		this.outputFileName = outputFileName;
	}

	public void startOfData() {
		if (rawTrackerHitCollectionName == null) {
			throw new RuntimeException("The parameter ecalCollectionName was not set!");
		}

		if (outputFileName != null) {
			try {
				outputStream = new PrintWriter(outputFileName);
			} catch (IOException ex) {
				throw new RuntimeException("Invalid outputFilePath!");
			}
		} else {
			outputStream = new PrintWriter(System.out, true);
		}
	}

	public void process(EventHeader event) {
		// Get the list of ECal hits.
		if (event.hasCollection(HPSSVTData.class, rawTrackerHitCollectionName)) {
			List<HPSSVTData> hits = event.get(HPSSVTData.class, rawTrackerHitCollectionName);
			//outputStream.println("Reading RawCalorimeterHit from event " + event.getEventNumber());
			for (HPSSVTData hit : hits) {
				outputStream.printf("FPGA=%d\thybrid=%d\tchannel=%d\n", hit.getFPGAAddress(), hit.getHybridNumber(), hit.getChannelNumber());
			}
		}
		if (event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName)) {
			List<RawTrackerHit> hits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);
			//outputStream.println("Reading RawCalorimeterHit from event " + event.getEventNumber());
			for (RawTrackerHit hit : hits) {
				outputStream.printf("name=%s\tside=%d\tstrip=%d\n", hit.getDetectorElement().getName(), hit.getIdentifierFieldValue("side"), hit.getIdentifierFieldValue("strip"));
			}
		}
	}
}