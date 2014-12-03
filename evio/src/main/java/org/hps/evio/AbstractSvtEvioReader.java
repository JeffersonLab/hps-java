package org.hps.evio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.StructureFinder;
import org.jlab.coda.jevio.BaseStructure;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Subdetector;
import org.lcsim.lcio.LCIOUtil;

import org.hps.util.Pair;

/**
 * Abstract SVT EVIO reader used to convert SVT bank sample blocks to
 * {@link RawTrackerHit}s.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @date November 20, 2014
 *
 */
public abstract class AbstractSvtEvioReader extends EvioReader {

	// A Map from DAQ pair (FPGA/Hybrid or FEB ID/FEB Hybrid ID) to the
	// corresponding sensor
	protected Map<Pair<Integer /* FPGA */, Integer /* Hybrid */>,
                  HpsSiSensor /* Sensor */> daqPairToSensor 
                      = new HashMap<Pair<Integer, Integer>, HpsSiSensor>();

	// Flag indicating whether the DAQ map has been setup
	protected boolean isDaqMapSetup = false;

	// Collections and names
	private static final String SVT_HIT_COLLECTION_NAME = "SVTRawTrackerHits";
	List<RawTrackerHit> rawHits = new ArrayList<RawTrackerHit>();

	// Constants
	private static final int MIN_DATA_BANK_TAG = 0;
	private static final String SUBDETECTOR_NAME = "Tracker";
	private static final String READOUT_NAME = "TrackerHits";

	/**
	 *	Get the maximum data bank tag in the event.
	 *
	 * @return Maximum data bank tag
	 */
	abstract protected int getMaxDataBankTag();

	/**
	 *	Get the SVT bank tag
	 *
	 *	@return SVT bank tag 
	 */
	abstract protected int getSvtBankTag();

	/**
	 *	Get the number of 32 bit integers composing the data block header
	 *
	 *	@return The header length
	 */
	abstract protected int getDataHeaderLength();

	/**
	 *	Get the number of 32 bit integers composing the data block tail (the 
	 *	data inserted after all sample blocks in a data block)
	 * 
	 *	@return The tail length 
	 */
	abstract protected int getDataTailLength();

	/**
	 *	A method to setup a mapping between a DAQ pair 
	 *	(FPGA/Hybrid or FEB ID/FEB Hybrid ID) and the corresponding sensor.
	 *
	 *	@param subdetector - The tracker {@link Subdetector} object
	 */
	// TODO: This can probably be done when the conditions are loaded.
	abstract protected void setupDaqMap(Subdetector subdetector);

	/**
	 *	Get the sensor associated with a set of samples  
	 *
	 *	@param data - sample block of data
	 *	@return The sensor associated with a set of sample 
	 */
	abstract protected HpsSiSensor getSensor(int[] data);

	/**
	 * 	Make a {@link RawTrackerHit} from a set of samples.
	 * 
	 *	@param data - sample block of data
	 * 	@return A raw hit
	 */
	abstract protected RawTrackerHit makeHit(int[] data);

	/**
	 *	Process an EVIO event and extract all information relevant to the SVT.
	 *	
	 *	@param event - EVIO event to process
	 *	@param lcsimEvent - LCSim event to put collections into 
	 *	@return true if the EVIO was processed successfully, false otherwise 
	 */
	public boolean processEvent(EvioEvent event, EventHeader lcsimEvent) {
		this.makeHits(event, lcsimEvent);
		return true;
	}

	/**
	 *	Make {@link RawTrackerHit}s out of all sample sets in an SVT EVIO bank
	 *	and put them into an LCSim event.
	 *
	 *	
	 *	@param event - EVIO event to process
	 * 	@param lcsimEvent - LCSim event to put collections into 
	 * 	@return true if the raw hits were created successfully, false otherwise 
	 */
	public boolean makeHits(EvioEvent event, EventHeader lcsimEvent) {

		// Setup the DAQ map if it's not setup
		if (!this.isDaqMapSetup)
			this.setupDaqMap(lcsimEvent.getDetector().getSubdetector(
					SUBDETECTOR_NAME));

		// Clear the list of raw tracker hits
		rawHits.clear();

		// Get the SVT data banks encapsulated by the physics event. There
		// should only be a single SVT bank that contains all physics data.
		// FIXME: Change the tag name so it's clear that we are referring to
		// the data bank
		List<BaseStructure> svtBanks = StructureFinder.getMatchingStructures(
				event, this.getSvtBankTag());

		// If there wasn't any SVT banks found, return false
		if (svtBanks.isEmpty())
			return false;

		// Check that the SVT bank contains data banks. If not, throw an
		// exception
		if (svtBanks.get(0).getChildCount() == 0) {
			throw new RuntimeException("[ " + this.getClass().getSimpleName()
					+ " ]: SVT bank doesn't contain any data banks.");
		}

		// Loop over the SVT data banks
		for (BaseStructure dataBank : svtBanks.get(0).getChildren()) {

			// Get the bank tag and check whether it's within the allowable
			// ranges. If not, throw an exception
			int dataBankTag = dataBank.getHeader().getTag();
			if (dataBankTag < MIN_DATA_BANK_TAG
					|| dataBankTag >= this.getMaxDataBankTag()) {
				throw new RuntimeException("[ "
						+ this.getClass().getSimpleName()
						+ " ]: Unexpected data bank tag:  " + dataBankTag);
			}

			// Get the int data encapsulated by the data bank
			int[] data = dataBank.getIntData();

			// Check that a complete set of samples exist
			int sampleCount = data.length - this.getDataHeaderLength()
					- this.getDataTailLength();
			System.out.println("[ " + this.getClass().getSimpleName()
					+ " ]: Sample count: " + sampleCount);
			if (sampleCount % 4 != 0) {
				throw new RuntimeException("[ "
						+ this.getClass().getSimpleName()
						+ " ]: Size of samples array is not divisible by 4");
			}

			// Loop through all of the samples and make hits
			for (int samplesN = this.getDataHeaderLength(); samplesN < sampleCount; samplesN += 4) {

				int[] samples = new int[4];
				System.arraycopy(data, samplesN, samples, 0, samples.length);
				rawHits.add(this.makeHit(samples));
			}
		}

		// Turn on 64-bit cell ID.
		int flag = LCIOUtil.bitSet(0, 31, true);
		// Add the collection of raw hits to the LCSim event
		lcsimEvent.put(SVT_HIT_COLLECTION_NAME, rawHits, RawTrackerHit.class,
				flag, READOUT_NAME);

		return true;
	}
}
