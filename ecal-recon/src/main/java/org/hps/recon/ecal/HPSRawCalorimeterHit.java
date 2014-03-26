package org.hps.recon.ecal;

import java.util.Comparator;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.RawCalorimeterHit;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: HPSRawCalorimeterHit.java,v 1.9 2013/02/25 22:39:24 meeg Exp $
 */
public class HPSRawCalorimeterHit implements RawCalorimeterHit {

	long cellID;
	int amplitude;
	int timeStamp;
	int windowSize;
	CalorimeterHit analogHit = null;

	public HPSRawCalorimeterHit(long cellID, int amplitude, int timeStamp, int windowSize) {
		this.cellID = cellID;
		this.amplitude = amplitude;
		this.timeStamp = timeStamp;
		this.windowSize = windowSize;
	}

    @Override
	public long getCellID() {
		return cellID;
	}

    @Override
	public int getAmplitude() {
		return amplitude;
	}

    @Override
	public int getTimeStamp() {
		return timeStamp;
	}

	public int getWindowSize() {
		return windowSize;
	}

	public CalorimeterHit getAnalogHit() {
		return analogHit;
	}

	public void setAnalogHit(CalorimeterHit analogHit) {
		this.analogHit = analogHit;
	}

	public static class TimeComparator implements Comparator<RawCalorimeterHit> {

		public int compare(RawCalorimeterHit o1, RawCalorimeterHit o2) {
			return o1.getTimeStamp() - o2.getTimeStamp();
		}
	}
}