package org.hps.recon.ecal;

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
	int mode; //A.C. this is the field I use, in case of REAL data, to record which FADC mode was used (ECAL_PULSE_INTEGRAL3_MODE or ECAL_PULSE_INTEGRAL7_MODE)
	short amplLow,amplHigh;

	public HPSRawCalorimeterHit(long cellID, int amplitude, int timeStamp, int windowSize) { //A.C. I do not change this, since I did not write it!
		this.cellID = cellID;
		this.amplitude = amplitude;
		this.timeStamp = timeStamp;
		this.windowSize = windowSize;
		
		//A part from init the fields..
		this.mode = -1;
		this.amplLow=0;
		this.amplHigh=0;
	}
	
	public HPSRawCalorimeterHit(long cellID, int amplitude, int timeStamp,int windowSize,short amplLow,short amplHigh,int mode) {
		this.cellID = cellID;
		this.amplitude = amplitude;
		this.timeStamp = timeStamp;
		this.windowSize = 0;
		this.amplLow = amplLow;
		this.amplHigh = amplHigh;
		this.mode = mode;
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
	
	public int getMode(){
		return mode;
	}

	public short getAmplLow(){
		return amplLow;
	}
	
	public short getAmplHigh(){
		return amplHigh;
	}
}