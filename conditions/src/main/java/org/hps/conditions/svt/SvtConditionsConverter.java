package org.hps.conditions.svt;

import org.lcsim.conditions.ConditionsManager;
import org.hps.conditions.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtChannel.SvtChannelCollection;
import org.hps.conditions.svt.SvtDaqMapping.SvtDaqMappingCollection;
import org.hps.conditions.svt.SvtT0Shift.SvtT0ShiftCollection;

/**
 * This class creates an {@link SvtConditions} object from the database, based on the
 * current run number known by the conditions manager.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public final class SvtConditionsConverter extends AbstractSvtConditionsConverter<SvtConditions> {

	public SvtConditionsConverter(){
		this.conditions = new SvtConditions();
	}
	
    /**
     * Create and return an {@link SvtConditions} object 
     * 
     * @param manager The current conditions manager.
     * @param name The conditions key, which is ignored for now.
     */
    @Override
    public SvtConditions getData(ConditionsManager manager, String name) {
    	
    	DatabaseConditionsManager dbConditionsManager = (DatabaseConditionsManager) manager;

    	// Get the channel map from the conditions database
        SvtChannelCollection channels = dbConditionsManager.getCollection(SvtChannelCollection.class); 

        // Create the SVT conditions object to use to encapsulate SVT condition collections
        conditions.setChannelMap(channels);
       
    	// Get the DAQ map from the conditions database
    	SvtDaqMappingCollection daqMap= dbConditionsManager.getCollection(SvtDaqMappingCollection.class);
        conditions.setDaqMap(daqMap);
        
        // Get the collection of T0 shifts from the conditions database
        SvtT0ShiftCollection t0Shifts = dbConditionsManager.getCollection(SvtT0ShiftCollection.class);
        conditions.setT0Shifts(t0Shifts);
        
        conditions = super.getData(manager, name);
        
        return conditions;
    }

    /**
     * Get the type handled by this converter.
     * @return The type handled by this converter.
     */
    @Override
    public Class<SvtConditions> getType() {
        return SvtConditions.class;
    }
}