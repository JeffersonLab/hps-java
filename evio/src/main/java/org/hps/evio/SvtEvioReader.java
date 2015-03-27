package org.hps.evio;

import java.util.List;

import org.jlab.coda.jevio.BaseStructure;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Subdetector;
import org.hps.util.Pair;

/**
 *	SVT EVIO reader used to convert SVT bank integer data to LCIO objects.
 * 
 * 	@author Omar Moreno <omoreno1@ucsc.edu>
 * 	@data February 03, 2015
 *
 */
public final class SvtEvioReader extends AbstractSvtEvioReader {

	//-----------------//
	//--- Constants ---//
	//-----------------//
	private static final int DATA_HEADER_LENGTH = 1;
	private static final int DATA_TAIL_LENGTH = 1; 
	private static final int MIN_ROC_BANK_TAG = 51;
	private static final int MAX_ROC_BANK_TAG = 66;
	private static final int ROC_BANK_NUMBER = 0; 
	
	/**
	 *	Get the minimum SVT ROC bank tag in the event.
	 *
	 *	@return Minimum SVT ROC bank tag
	 */
	@Override
	protected int getMinRocBankTag() { 
		return MIN_ROC_BANK_TAG; 
	}

	/**
	 *	Get the maximum SVT ROC bank tag in the event.
	 *
	 *	@return Maximum SVT ROC bank tag
	 */
	@Override 
	protected int getMaxRocBankTag() { 
		return MAX_ROC_BANK_TAG; 
	}
	
	/**
	 *	Get the SVT ROC bank number of the bank encapsulating the SVT samples.
	 * 
	 *	@return SVT ROC bank number 
	 */
	@Override
	protected int getRocBankNumber() { 
		return ROC_BANK_NUMBER; 
	}

	/**
	 *	Get the number of 32 bit integers composing the data block header
	 *
	 *	@return The header length
	 */
	@Override
	protected int getDataHeaderLength() {
		return DATA_HEADER_LENGTH;
	}

	/**
	 *	Get the number of 32 bit integers composing the data block tail (the 
	 *	data inserted after all sample blocks in a data block)
	 * 
	 *	@return The tail length 
	 */
	@Override
	protected int getDataTailLength() {
		return DATA_TAIL_LENGTH;
	}

	/**
	 *	A method to setup a mapping between a DAQ pair (FEB/FEB Hybrid) and the 
	 *	corresponding sensor.
	 *
	 *	@param subdetector : The tracker {@link Subdetector} object
	 */
	// TODO: This can probably be done when the conditions are loaded.
	@Override
	protected void setupDaqMap(Subdetector subdetector) {
	
		List<HpsSiSensor> sensors 
			= subdetector.getDetectorElement().findDescendants(HpsSiSensor.class);
		for (HpsSiSensor sensor : sensors) { 
			Pair<Integer, Integer> daqPair 
				= new Pair<Integer, Integer>(sensor.getFebID(), sensor.getFebHybridID());
		logger.fine("FEB ID: " + sensor.getFebID() 
				  + " Hybrid ID: " + sensor.getFebHybridID());
			daqPairToSensor.put(daqPair, sensor);
		}
		this.isDaqMapSetup = true; 
	}

	/**
	 *	Get the sensor associated with a set of samples.  The sample block of
	 *	data is used to extract the FEB ID and FEB Hybrid ID corresponding to 
	 *	the samples. 
	 *
	 *	@param data : sample block of data
	 *	@return The sensor associated with a set of sample 
	 */
	@Override
	protected HpsSiSensor getSensor(int[] data) {
		
		logger.fine("FEB ID: " + SvtEvioUtils.getFebID(data) 
				  + " Hybrid ID: " + SvtEvioUtils.getFebHybridID(data));
		
		Pair<Integer, Integer> daqPair 
			= new Pair<Integer, Integer>(SvtEvioUtils.getFebID(data), 
										 SvtEvioUtils.getFebHybridID(data));
		
		return daqPairToSensor.get(daqPair);
	}
	
	/**
	 *	Check whether a data bank is valid i.e. contains SVT samples only.  For
	 *	the engineering run, a valid data bank has a tag of 1.
	 * 
	 * 	@param dataBank - An EVIO bank containing integer data
	 * 	@return true if the bank is valid, false otherwise
	 * 
	 */
	@Override
	protected boolean isValidDataBank(BaseStructure dataBank) { 
		if (dataBank.getHeader().getTag() != 3) return false; 
		return true; 
	}

	/**
	 * 	Make a {@link RawTrackerHit} from a set of samples.
	 * 
	 *	@param data : sample block of data
	 * 	@return A raw hit
	 */
	@Override
	protected RawTrackerHit makeHit(int[] data) {
		return makeHit(data, SvtEvioUtils.getChannelNumber(data));
	}
}
