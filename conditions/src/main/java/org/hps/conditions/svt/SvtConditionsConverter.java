package org.hps.conditions.svt;

import static org.hps.conditions.TableConstants.SVT_DAQ_MAP;

import org.hps.conditions.DatabaseConditionsManager;
import org.hps.conditions.TableMetaData;
import org.hps.conditions.svt.SvtDaqMapping.SvtDaqMappingCollection;

import org.lcsim.conditions.ConditionsManager;

/**
 * This class creates an {@link SvtConditions} object from the database, based on the
 * current run number known by the conditions manager.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public final class SvtConditionsConverter extends AbstractSvtConditionsConverter<SvtConditions> {

	private TableMetaData metaData = null; 
	private String tableName = null; 

	public SvtConditionsConverter(){
		this.conditions = new SvtConditions();
	}
	
    /**
     * Create and return the SVT conditions object.
     * @param manager The current conditions manager.
     * @param name The conditions key, which is ignored for now.
     */
    public SvtConditions getData(ConditionsManager manager, String name) {
    	conditions = super.getData(manager, name);
    	
    	DatabaseConditionsManager dbConditionsManager = (DatabaseConditionsManager) manager;
    	
    	// Get the table name containing the SVT DAQ map from the database
        // configuration.  If it doesn't exist, use the default value.
        metaData = dbConditionsManager.findTableMetaData(SvtDaqMappingCollection.class);
    	if(metaData != null){
    		tableName = metaData.getTableName();
    	} else { 
    		tableName = SVT_DAQ_MAP; 
    	}
        // Get the DAQ map from the conditions database
        SvtDaqMappingCollection daqMap = manager.getCachedConditions(SvtDaqMappingCollection.class, tableName).getCachedData();
        conditions.setDaqMap(daqMap);
        
        return conditions;
    }

    /**
     * Get the type handled by this converter.
     * @return The type handled by this converter.
     */
    public Class<SvtConditions> getType() {
        return SvtConditions.class;
    }
}