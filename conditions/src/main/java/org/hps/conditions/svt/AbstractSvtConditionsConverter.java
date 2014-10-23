package org.hps.conditions.svt;

import static org.hps.conditions.TableConstants.SVT_BAD_CHANNELS;
import static org.hps.conditions.TableConstants.SVT_CALIBRATIONS;
import static org.hps.conditions.TableConstants.SVT_CHANNELS;
import static org.hps.conditions.TableConstants.SVT_DAQ_MAP;
import static org.hps.conditions.TableConstants.SVT_GAINS;
import static org.hps.conditions.TableConstants.SVT_PULSE_PARAMETERS;
import static org.hps.conditions.TableConstants.SVT_TIME_SHIFTS;

import org.lcsim.conditions.ConditionsConverter;
import org.lcsim.conditions.ConditionsManager;
import org.hps.conditions.DatabaseConditionsManager;
import org.hps.conditions.TableMetaData;
import org.hps.conditions.svt.SvtCalibration.SvtCalibrationCollection;
import org.hps.conditions.svt.SvtChannel.SvtChannelCollection;
import org.hps.conditions.svt.SvtShapeFitParameters.SvtShapeFitParametersCollection;


/**
 * Abstract class providing some of the common methods used in creating
 * SVT conditions objects from the database.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 *
 * @param <T>  SVT conditions object type
 */
public abstract class AbstractSvtConditionsConverter<T> implements ConditionsConverter<T> {
	
	private TableMetaData metaData = null; 
	private String tableName = null; 
	
	public abstract T getData(ConditionsManager manager, String name);
		
	
    private SvtChannelCollection getSvtChannelMap(DatabaseConditionsManager dbConditionsManager){
    	
        // Get the table name containing the SVT channel map from the 
    	// database configuration.  If it doesn't exist, use the default value.
    	metaData = dbConditionsManager.findTableMetaData(SvtChannelCollection.class);
    	if(metaData != null){
    		tableName = metaData.getTableName();
    	} else { 
    		tableName = SVT_CHANNELS; 
    	}
    	// Get the SVT channel map from the conditions database
    	SvtChannelCollection channels 
    		= dbConditionsManager.getCachedConditions(SvtChannelCollection.class, tableName).getCachedData();
    
    	return channels;
    }
    
    private SvtCalibrationCollection getCalibrations(DatabaseConditionsManager dbConditionsManager){
    	
        // Get the table name containing the SVT calibrations (baseline, noise)
        // from the database configuration.  If it doesn't exist, use the 
        // default value.
        metaData = dbConditionsManager.findTableMetaData(SvtCalibrationCollection.class);
    	if(metaData != null){
    		tableName = metaData.getTableName();
    	} else { 
    		tableName = SVT_CALIBRATIONS; 
    	}
    	// Get the calibrations from the conditions database
        SvtCalibrationCollection calibrations = dbConditionsManager.getCachedConditions(SvtCalibrationCollection.class, tableName).getCachedData();
    
        return calibrations;
    }
    
    private SvtShapeFitParametersCollection getShapeFitParameters(DatabaseConditionsManager dbConditionsManager){
    	// Get the table name containing the SVT pulse shape parameters from 
    	// the database configuration.  If it doesn't exist, use the default value.
    	metaData = dbConditionsManager.findTableMetaData(SvtShapeFitParametersCollection.class);
    	if(metaData != null){
    		tableName = metaData.getTableName();
    	} else { 
    		tableName = SVT_PULSE_PARAMETERS; 
    	}
    	// Add pulse parameters by channel.
    	SvtShapeFitParametersCollection shapeFitParametersCollection = dbConditionsManager.getCachedConditions(SvtShapeFitParametersCollection.class, tableName).getCachedData();
    
    	return shapeFitParametersCollection;
    }
}
