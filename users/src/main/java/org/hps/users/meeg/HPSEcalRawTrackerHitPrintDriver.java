/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.users.meeg;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.geometry.Subdetector;
import org.lcsim.util.Driver;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: HPSEcalRawTrackerHitPrintDriver.java,v 1.3 2012/04/27 22:13:52 meeg Exp $
 */
public class HPSEcalRawTrackerHitPrintDriver extends Driver {

	Subdetector ecal;
	IDDecoder dec;
	String ecalName = "Ecal";
	String ecalReadoutName = "EcalHits";
	String ecalCollectionName = "EcalRawHits";
	String outputFileName;
	PrintWriter outputStream = null;
	int flags;

	public HPSEcalRawTrackerHitPrintDriver() {
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
		if (event.hasCollection(RawTrackerHit.class, ecalCollectionName)) {
			//outputStream.println("Reading RawTrackerHits from event " + event.getEventNumber());
			List<RawTrackerHit> hits = event.get(RawTrackerHit.class, ecalCollectionName);
			for (RawTrackerHit hit : hits) {
				dec.setID(hit.getCellID());
				outputStream.printf("%d\t%d\t%d\t%d\n", dec.getValue("ix"), dec.getValue("iy"), hit.getTime(), hit.getADCValues().length);
				for (int i = 0; i < hit.getADCValues().length; i++) {
					outputStream.printf("%d\n", hit.getADCValues()[i]);
				}
			}
		}
	}
}
