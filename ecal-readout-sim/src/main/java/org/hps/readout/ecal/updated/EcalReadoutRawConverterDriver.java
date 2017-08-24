package org.hps.readout.ecal.updated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.recon.ecal.EcalRawConverter;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;

public class EcalReadoutRawConverterDriver extends ReadoutDriver {
	/**
	 * Sets the name of the input {@link
	 * org.lcsim.event.RawCalorimeterHit RawCalorimeterHit}
	 * collection.
	 */
	private String inputCollectionName = "EcalRawHits";
	/**
	 * Sets the name of the output {@link
	 * org.lcsim.event.CalorimeterHit CalorimeterHit} collection.
	 */
	private String outputCollectionName = "EcalCorrectedHits";
	/**
	 * The converter object responsible for processing raw hits into
	 * proper {@link org.lcsim.event.CalorimeterHit CalorimeterHit}
	 * objects.
	 */
	private EcalRawConverter converter = new EcalRawConverter();
	/**
	 * Cached copy of the calorimeter conditions. All calorimeter
	 * conditions should be called from here, rather than by directly
	 * accessing the database manager.
	 */
	private EcalConditions ecalConditions = null;
	/**
	 * Tracks the current local time in nanoseconds for this driver.
	 */
	private double localTime = 2.0;
	
	// TODO: What is a "bad" crystal?
	private boolean applyBadCrystalMap = true;
	
	@Override
	public void startOfData() {
		// Set the dependencies for the driver and register its
		// output collections with the data management driver.
		addDependency(inputCollectionName);
		ReadoutDataManager.registerCollection(outputCollectionName, this, CalorimeterHit.class);
	}
	
	@Override
	public void detectorChanged(Detector detector) {
		// Reset the converter calorimeter conditions.
		// TODO: The detector object is not actually used by the converter...
		converter.setDetector(detector);
		
		// Cache the calorimeter conditions object.
		ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();
	}
	
	@Override
	public void process(EventHeader event) {
		// Check the data management driver to determine whether the
		// input collection is available or not.
		if(!ReadoutDataManager.checkCollectionStatus(inputCollectionName, localTime)) {
			return;
		}
		
		Collection<RawCalorimeterHit> rawHits = ReadoutDataManager.getData(localTime, localTime + 2.0, inputCollectionName, RawCalorimeterHit.class);
		System.out.println("New Raw Converter -- Event " + event.getEventNumber() +" -- Current Time is " + localTime);
		System.out.println("\tSaw Raw Hits:");
		if(rawHits.isEmpty()) { System.out.println("\t\tNone!"); }
		for(RawCalorimeterHit hit : rawHits) {
			System.out.println("\t\tRaw hit with amplitude " + hit.getAmplitude() + " and time " + hit.getTimeStamp() + " on channel " + hit.getCellID() + ".");
		}
		
		// Increment the local time.
		localTime += 4.0;
		
		// TODO: The readout manager needs to know this.
		int flags = 0;
		flags += 1 << LCIOConstants.RCHBIT_TIME; //store hit time
		flags += 1 << LCIOConstants.RCHBIT_LONG; //store hit position; this flag has no effect for RawCalorimeterHits
		
		List<CalorimeterHit> newHits = new ArrayList<CalorimeterHit>();
		for(RawCalorimeterHit hit : rawHits) {
			CalorimeterHit newHit;
			newHit = converter.HitDtoA(event, hit, 0.0);
			if(!(applyBadCrystalMap && isBadCrystal(newHit))) {
				newHits.add(newHit);
			}
		}
		
		System.out.println("\tMade Calorimeter Hits:");
		if(newHits.isEmpty()) { System.out.println("\t\tNone!"); }
		for(CalorimeterHit hit : newHits) {
			System.out.println("\t\tCalorimeter hit with energy " + hit.getRawEnergy() + " and time " + hit.getTime() + " on channel " + hit.getCellID() + ".");
		}
		ReadoutDataManager.addData(outputCollectionName, newHits, CalorimeterHit.class);
		
		// TODO: What is function of the "readout name"?
		//event.put(outputCollectionName, newHits, CalorimeterHit.class, flags, "ecalReadoutName");
	}
	
	@Override
	protected double getTimeDisplacement() {
		return 0;
	}
	
	/**
	 * Gets the channel parameters for a given channel ID.
	 * @param cellID - The <code>long</code> ID value that represents
	 * the channel. This is typically acquired from the method {@link
	 * org.lcsim.event.CalorimeterHit#getCellID() getCellID()} in a
	 * {@link org.lcsim.event.CalorimeterHit CalorimeterHit} object.
	 * @return Returns the channel parameters for the channel as an
	 * {@link org.hps.conditions.ecal.EcalChannelConstants
	 * EcalChannelConstants} object.
	 */
	private EcalChannelConstants findChannel(long cellID) {
		return ecalConditions.getChannelConstants(ecalConditions.getChannelCollection().findGeometric(cellID));
	}
	
	/**
	 * Indicates whether or not the channel on which a hit occurs is
	 * a "bad" channel according to the conditions database.
	 * @param hit - The hit to check.
	 * @return Returns <code>true</code> if the hit channel is
	 * flagged as "bad" and <code>false</code> otherwise.
	 */
	private boolean isBadCrystal(CalorimeterHit hit) {
		return findChannel(hit.getCellID()).isBadChannel();
	}
	
	/**
	 * Indicates whether or not data from channels flagged as "bad"
	 * in the conditions system should be ignored. <code>true</code>
	 * indicates that they should be ignored, and <code>false</code>
	 * that they should not.
	 * @param apply - <code>true</code> indicates that "bad" channels
	 * will be ignored and <code>false</code> that they will not.
	 */
	public void setApplyBadCrystalMap(boolean state) {
		applyBadCrystalMap = state;
	}
	
	/**
	 * Sets the name of the input collection containing the objects
	 * of type {@link org.lcsim.event.RawCalorimeterHit
	 * RawCalorimeterHit} that are output by the digitization driver.
	 * This is <code>"EcalRawHits"</code> by default.
	 * @param collection - The name of the input raw hit collection.
	 */
	public void setInputCollectionName(String collection) {
		inputCollectionName = collection;
	}
	
	/**
	 * Sets the number of integration samples that should be included
	 * in a pulse integral after the threshold-crossing event.
	 * @param samples - The number of samples, where a sample is a
	 * 4 ns clock-cycle.
	 */
	public void setNsa(int samples) {
		converter.setNSA(4 * samples);
	}
	
	/**
	 * Sets the number of integration samples that should be included
	 * in a pulse integral before the threshold-crossing event.
	 * @param samples - The number of samples, where a sample is a
	 * 4 ns clock-cycle.
	 */
	public void setNsb(int samples) {
		converter.setNSB(4 * samples);
	}
	
	/**
	 * Sets the name of the output collection containing the objects
	 * of type {@link org.lcsim.event.CalorimeterHit CalorimeterHit}
	 * that are output by this driver. This is
	 * <code>"EcalCorrectedHits"</code> by default.
	 * @param collection - The name of the output hit collection.
	 */
	public void setOutputCollectionName(String collection) {
		outputCollectionName = collection;
	}
}