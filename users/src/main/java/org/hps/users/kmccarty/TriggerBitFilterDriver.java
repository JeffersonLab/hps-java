package org.hps.users.kmccarty;

import java.util.List;

import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.util.Driver;

/**
 * Checks whether an event contains a TI bit flag, and if not, orders
 * LCSim to ignore all subsequent drivers for this event. Note that
 * this driver should be placed <i>before</i> any drivers that need
 * to be skipped in the driver chain in order to work properly.
 */
public class TriggerBitFilterDriver extends Driver {
	// Store the LCIO collection names.
	private String bankCollectionName = null;
	
	// Store which triggers to filter.
	private boolean selectPulser   = false;
	private boolean selectSingles0 = false;
	private boolean selectSingles1 = false;
	private boolean selectPair0    = false;
	private boolean selectPair1    = false;
	
	/**
	 * Checks that the mandatory parameters have been set.
	 */
	@Override
	public void startOfData() {
		// Require that bank collection name be set.
		if(bankCollectionName == null) {
			throw new IllegalArgumentException("Trigger bank collection name must be defined.");
		}
	}
	
	/**
	 * Subsequent drivers should only be processed if the desired bit(s)
	 * is (are) active. Method checks to ensure that this condition is
	 * true, and if not, instructs LCSim to ignore subsequent drivers.
	 * @param event - The object containing event data.
	 */
	@Override
	public void process(EventHeader event) {
        // Get the TI bank.
		TIData tiBank = null;
        if(event.hasCollection(GenericObject.class, bankCollectionName)) {
            // Get the bank list.
            List<GenericObject> bankList = event.get(GenericObject.class, bankCollectionName);
            
            // Search through the banks and get the SSP and TI banks.
            for(GenericObject obj : bankList) {
                // If this is an SSP bank, parse it.
                if(AbstractIntData.getTag(obj) == TIData.BANK_TAG) {
                    tiBank = new TIData(obj);
                }
            }
        }
        
        // Determine if any of the selected trigger bits are present.
        boolean passEvent = false;
        if(selectPulser && tiBank.isPulserTrigger()) {
        	passEvent = true;
        } else if(selectSingles0 && tiBank.isSingle0Trigger()) {
        	passEvent = true;
        } else if(selectSingles1 && tiBank.isSingle1Trigger()) {
        	passEvent = true;
        } else if(selectPair0 && tiBank.isPair0Trigger()) {
        	passEvent = true;
        } else if(selectPair1 && tiBank.isPair1Trigger()) {
        	passEvent = true;
        }
        
        // If any selected trigger bit is present, the event may proceed.
        // Otherwise, do not process the event in any downstream drivers.
        if(!passEvent) { throw new NextEventException(); }
	}
	
	/**
	 * Sets the name of the LCIO collection containing the TI bank.
	 * @param collection - The LCIO collection name.
	 */
	public void setBankCollectionName(String collection) {
		bankCollectionName = collection;
	}
	
	/**
	 * Sets whether pulser triggers should be passed.
	 * @param state - <code>true</code> means this trigger type should
	 * be allowed through the filter. If the value is <code>false</code>,
	 * this type of trigger will not be selected, but will also not
	 * disallow the event from passing if another selected trigger type
	 * is present.
	 */
	public void setSelectPulserTriggers(boolean state) {
		selectPulser = state;
	}
	
	/**
	 * Sets whether singles 1 triggers should be passed.
	 * @param state - <code>true</code> means this trigger type should
	 * be allowed through the filter. If the value is <code>false</code>,
	 * this type of trigger will not be selected, but will also not
	 * disallow the event from passing if another selected trigger type
	 * is present.
	 */
	public void setSelectSingles0Triggers(boolean state) {
		selectSingles0 = state;
	}
	
	/**
	 * Sets whether singles 1 triggers should be passed.
	 * @param state - <code>true</code> means this trigger type should
	 * be allowed through the filter. If the value is <code>false</code>,
	 * this type of trigger will not be selected, but will also not
	 * disallow the event from passing if another selected trigger type
	 * is present.
	 */
	public void setSelectSingles1Triggers(boolean state) {
		selectSingles1 = state;
	}
	
	/**
	 * Sets whether pair 0 triggers should be passed.
	 * @param state - <code>true</code> means this trigger type should
	 * be allowed through the filter. If the value is <code>false</code>,
	 * this type of trigger will not be selected, but will also not
	 * disallow the event from passing if another selected trigger type
	 * is present.
	 */
	public void setSelectPair0Triggers(boolean state) {
		selectPair0 = state;
	}
	
	/**
	 * Sets whether pair 1 triggers should be passed.
	 * @param state - <code>true</code> means this trigger type should
	 * be allowed through the filter. If the value is <code>false</code>,
	 * this type of trigger will not be selected, but will also not
	 * disallow the event from passing if another selected trigger type
	 * is present.
	 */
	public void setSelectPair1Triggers(boolean state) {
		selectPair1 = state;
	}
}