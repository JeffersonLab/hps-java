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
import org.lcsim.util.Driver;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: HPSGenericRawTrackerHitPrintDriver.java,v 1.1 2012/04/10
 * 01:00:13 meeg Exp $
 */
public class HPSGenericRawTrackerHitPrintDriver extends Driver {

	String outputFileName;
	PrintWriter outputStream = null;

	public HPSGenericRawTrackerHitPrintDriver() {
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

	public void process(EventHeader event) {
		// Get the list of ECal hits.
		if (event.hasCollection(RawTrackerHit.class)) {
			//outputStream.println("Reading RawTrackerHits from event " + event.getEventNumber());
			List<List<RawTrackerHit>> listOfLists = event.get(RawTrackerHit.class);
			for (List<RawTrackerHit> hits : listOfLists) {
				outputStream.printf("List with %d RawTrackerHits:\n", hits.size());
				for (RawTrackerHit hit : hits) {
					outputStream.printf("%d\t%d\n", hit.getCellID(), hit.getADCValues().length);
					for (int i = 0; i < hit.getADCValues().length; i++) {
						outputStream.printf("%d\n", hit.getADCValues()[i]);
					}
				}
			}
		}
	}
}
