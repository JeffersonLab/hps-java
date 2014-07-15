package org.hps.monitoring.drivers.ecal;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.geometry.Subdetector;
import org.lcsim.util.Driver;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: ECalCellIDPrintDriver.java,v 1.1 2012/05/01 15:06:38 meeg Exp $
 */
public class ECalCellIDPrintDriver extends Driver {

	Subdetector ecal;
	IDDecoder dec;
	String ecalName = "Ecal";
	String ecalCollectionName = "EcalReadoutHits";
	String outputFileName;
	PrintWriter outputStream = null;

	public ECalCellIDPrintDriver() {
	}

	public void setEcalCollectionName(String ecalCollectionName) {
		this.ecalCollectionName = ecalCollectionName;
	}

	public void setEcalName(String ecalName) {
		this.ecalName = ecalName;
	}

	public void setOutputFileName(String outputFileName) {
		this.outputFileName = outputFileName;
	}

	public void startOfData() {
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

	public void detectorChanged(Detector detector) {
		// Get the Subdetector.
		ecal = (Subdetector) detector.getSubdetector(ecalName);
		dec = ecal.getIDDecoder();
	}

	public void process(EventHeader event) {
		// Get the list of ECal hits.
		if (event.hasCollection(RawCalorimeterHit.class, ecalCollectionName)) {
			List<RawCalorimeterHit> hits = event.get(RawCalorimeterHit.class, ecalCollectionName);
			//outputStream.println("Reading RawCalorimeterHit from event " + event.getEventNumber());
			for (RawCalorimeterHit hit : hits) {
				dec.setID(hit.getCellID());
				outputStream.printf("x=%d\ty=%d\n", dec.getValue("ix"), dec.getValue("iy"));
			}
		}
		if (event.hasCollection(RawTrackerHit.class, ecalCollectionName)) {
			List<RawTrackerHit> hits = event.get(RawTrackerHit.class, ecalCollectionName);
			//outputStream.println("Reading RawCalorimeterHit from event " + event.getEventNumber());
			for (RawTrackerHit hit : hits) {
				dec.setID(hit.getCellID());
				outputStream.printf("x=%d\ty=%d\n", dec.getValue("ix"), dec.getValue("iy"));
			}
		}
	}
}