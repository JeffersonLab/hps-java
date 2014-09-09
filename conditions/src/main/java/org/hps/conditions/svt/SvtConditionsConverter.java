package org.hps.conditions.svt;

import static org.hps.conditions.TableConstants.SVT_BAD_CHANNELS;
import static org.hps.conditions.TableConstants.SVT_CALIBRATIONS;
import static org.hps.conditions.TableConstants.SVT_CHANNELS;
import static org.hps.conditions.TableConstants.SVT_DAQ_MAP;
import static org.hps.conditions.TableConstants.SVT_GAINS;
import static org.hps.conditions.TableConstants.SVT_PULSE_PARAMETERS;
import static org.hps.conditions.TableConstants.SVT_TIME_SHIFTS;

import org.hps.conditions.DatabaseConditionsManager;
import org.hps.conditions.TableMetaData;
import org.hps.conditions.svt.SvtBadChannel.SvtBadChannelCollection;
import org.hps.conditions.svt.SvtCalibration.SvtCalibrationCollection;
import org.hps.conditions.svt.SvtChannel.SvtChannelCollection;
import org.hps.conditions.svt.SvtDaqMapping.SvtDaqMappingCollection;
import org.hps.conditions.svt.SvtGain.SvtGainCollection;
import org.hps.conditions.svt.SvtPulseParameters.SvtPulseParametersCollection;
import org.hps.conditions.svt.SvtTimeShift.SvtTimeShiftCollection;
import org.lcsim.conditions.ConditionsConverter;
import org.lcsim.conditions.ConditionsManager;

/**
 * This class creates an {@link SvtConditions} object from the database, based on the
 * current run number known by the conditions manager.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * $Id$
 */
public final class SvtConditionsConverter implements ConditionsConverter<SvtConditions> {

	private TableMetaData metaData = null; 
	private String tableName = null; 
	
    /**
     * Create and return the SVT conditions object.
     * @param manager The current conditions manager.
     * @param name The conditions key, which is ignored for now.
     */
    public SvtConditions getData(ConditionsManager manager, String name) {
    
    	DatabaseConditionsManager dbConditionsManager = (DatabaseConditionsManager) manager;
    	
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
        
        // Create the SVT conditions object to use to encapsulate SVT condition collections
        SvtConditions conditions = new SvtConditions(channels);

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
        SvtCalibrationCollection calibrations = manager.getCachedConditions(SvtCalibrationCollection.class, tableName).getCachedData();
        for (SvtCalibration calibration : calibrations.getObjects()) {
            SvtChannel channel = conditions.getChannelMap().findChannel(calibration.getChannelId());
            conditions.getChannelConstants(channel).setCalibration(calibration);
        }

        // Get the table name containing the SVT pulse shape parameters from 
        // the database configuration.  If it doesn't exist, use the default value.
        metaData = dbConditionsManager.findTableMetaData(SvtPulseParametersCollection.class);
    	if(metaData != null){
    		tableName = metaData.getTableName();
    	} else { 
    		tableName = SVT_PULSE_PARAMETERS; 
    	}
        // Add pulse parameters by channel.
        SvtPulseParametersCollection pulseParametersCollection = manager.getCachedConditions(SvtPulseParametersCollection.class, tableName).getCachedData();
        for (SvtPulseParameters pulseParameters : pulseParametersCollection.getObjects()) {
            SvtChannel channel = conditions.getChannelMap().findChannel(pulseParameters.getChannelId());
            conditions.getChannelConstants(channel).setPulseParameters(pulseParameters);
        }

        // Get the table name containing the SVT bad channel map from the 
        // database configuration.  If it doesn't exist, use the default value.
        metaData = dbConditionsManager.findTableMetaData(SvtBadChannelCollection.class);
    	if(metaData != null){
    		tableName = metaData.getTableName();
    	} else { 
    	}
    		tableName = SVT_BAD_CHANNELS; 
    	
        // Add bad channels.
        // FIXME: This should be changed to catch a conditions record not found exception instead of 
    	// 		  a runtime exception.
    	try { 
        	SvtBadChannelCollection badChannels = manager.getCachedConditions(SvtBadChannelCollection.class, tableName).getCachedData();
        	for (SvtBadChannel badChannel : badChannels.getObjects()) {
        		SvtChannel channel = conditions.getChannelMap().findChannel(badChannel.getChannelId());
        		conditions.getChannelConstants(channel).setBadChannel(true);
        	}
        } catch(RuntimeException e){
        	e.printStackTrace();
        }

        // Get the table name containing the SVT gains from the database
        // configuration.  If it doesn't exist, use the default value.
        metaData = dbConditionsManager.findTableMetaData(SvtGainCollection.class);
    	if(metaData != null){
    		tableName = metaData.getTableName();
    	} else { 
    		tableName = SVT_GAINS; 
    	}
        
        // Add gains by channel.
        SvtGainCollection gains = manager.getCachedConditions(SvtGainCollection.class, tableName).getCachedData();
        for (SvtGain object : gains.getObjects()) {
            int channelId = object.getChannelID();
            SvtChannel channel = conditions.getChannelMap().findChannel(channelId);
            conditions.getChannelConstants(channel).setGain(object);
        }

        // Get the table name containing the SVT t0 shifts. If it doesn't 
        // exist, use the default value. 
        metaData = dbConditionsManager.findTableMetaData(SvtTimeShiftCollection.class);
    	if(metaData != null){
    		tableName = metaData.getTableName();
    	} else { 
    		tableName = SVT_TIME_SHIFTS; 
    	}	
        // Set the t0 shifts by sensor.
        SvtTimeShiftCollection t0Shifts = manager.getCachedConditions(SvtTimeShiftCollection.class, tableName).getCachedData();
        conditions.setTimeShifts(t0Shifts);

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