/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.users.meeg;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.geometry.Subdetector;
import org.lcsim.util.Driver;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: HPSEcalDigitalPrintDriver.java,v 1.5 2012/04/27 22:13:52 meeg Exp $
 */
public class HPSEcalDigitalPrintDriver extends Driver {

	Subdetector ecal;
	IDDecoder dec;
	String ecalName = "Ecal";
	String ecalReadoutName = "EcalHits";
	String ecalCollectionName = "EcalRawHits";
	String outputFileName;
	PrintWriter outputStream = null;
	int timeScale = 1;
	int flags;

	public HPSEcalDigitalPrintDriver() {
	}

	public void setTimeScale(int timeScale) {
		this.timeScale = timeScale;
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
		if (ecalCollectionName == null) {
			throw new RuntimeException("The parameter ecalCollectionName was not set!");
		}

		if (ecalName == null) {
			throw new RuntimeException("The parameter ecalName was not set!");
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
				outputStream.printf("%d\t%d\t%d\t%d\n", dec.getValue("ix"), dec.getValue("iy"), hit.getTimeStamp() * timeScale, hit.getAmplitude());
			}
		}
	}
}