package org.hps.users.kmccarty;

import java.util.List;

import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.util.Driver;

/**
 * Class <code>CountTriggersDriver</code> counts the number times the
 * TI trigger bit was active for each trigger type and outputs the
 * result in text at the end of the data processing run.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class CountTriggersDriver extends Driver {
	// Store programmable parameters.
	private String bankCollectionName = "TriggerBank";
	
	// Track the number of triggers seen for each trigger type.
	private int[] triggers = new int[6];
	private static final int PULSER   = 0;
	private static final int SINGLES0 = 1;
	private static final int SINGLES1 = 2;
	private static final int PAIR0    = 3;
	private static final int PAIR1    = 4;
	private static final int COSMIC   = 5;
	
	/**
	 * Outputs the total number of triggers seen for each trigger type.
	 */
	@Override
	public void endOfData() {
		System.out.println("Trigger Counts:");
		System.out.printf("Singles 0 :: %d%n", triggers[SINGLES0]);
		System.out.printf("Singles 1 :: %d%n", triggers[SINGLES1]);
		System.out.printf("Pair 0    :: %d%n", triggers[PAIR0]);
		System.out.printf("Pair 1    :: %d%n", triggers[PAIR1]);
		System.out.printf("Pulser    :: %d%n", triggers[PULSER]);
		System.out.printf("Cosmic    :: %d%n", triggers[COSMIC]);
	}
	
	/**
	 * Checks whether a trigger of each given type was seen by the TI
	 * for each event and increments the total trigger count for that
	 * type as appropriate.
	 */
	@Override
	public void process(EventHeader event) {
		// Extract the TI bank from the data stream.
		TIData tiBank = null;
		if(event.hasCollection(GenericObject.class, bankCollectionName)) {
			// Get the bank list.
			List<GenericObject> bankList = event.get(GenericObject.class, bankCollectionName);
			
			// Search through the banks and get the TI bank.
			for(GenericObject obj : bankList) {
				if(AbstractIntData.getTag(obj) == TIData.BANK_TAG) {
					tiBank = new TIData(obj);
				}
			}
		}
		
		// If there is no TI bank, the event can not be processed.
		if(tiBank == null) {
			return;
		}
		
		// Otherwise, increment the relevant trigger counts.
		if(tiBank.isPulserTrigger()) { triggers[PULSER]++; }
		else if(tiBank.isSingle0Trigger()) { triggers[SINGLES0]++; }
		else if(tiBank.isSingle1Trigger()) { triggers[SINGLES1]++; }
		else if(tiBank.isPair0Trigger()) { triggers[PAIR0]++; }
		else if(tiBank.isPair1Trigger()) { triggers[PAIR1]++; }
		else if(tiBank.isCalibTrigger()) { triggers[COSMIC]++; }
	}
}