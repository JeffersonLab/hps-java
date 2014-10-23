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
import org.hps.conditions.svt.SvtBadChannel.SvtBadChannelCollection;
import org.hps.conditions.svt.SvtCalibration.SvtCalibrationCollection;
import org.hps.conditions.svt.SvtChannel.SvtChannelCollection;
import org.hps.conditions.svt.SvtGain.SvtGainCollection;
import org.hps.conditions.svt.SvtShapeFitParameters.SvtShapeFitParametersCollection;
import org.hps.conditions.svt.SvtT0Shift.SvtT0ShiftCollection;


/**
 * Abstract class providing some of the common methods used in creating
 * SVT conditions objects from the database.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 *
 * @param <T>  SVT conditions object type
 */
public abstract class AbstractSvtConditionsConverter<T extends AbstractSvtConditions> implements ConditionsConverter<T> {
	
	private TableMetaData metaData = null; 
	private String tableName = null; 

	protected T conditions;
	
    /**
     * Create and return the SVT conditions object.
     * @param manager The current conditions manager.
     * @param name The conditions key, which is ignored for now.
     */
	public T getData(ConditionsManager manager, String name){
	
    	DatabaseConditionsManager dbConditionsManager = (DatabaseConditionsManager) manager;
    	
        SvtChannelCollection channels = this.getSvtChannelMap(dbConditionsManager);

        // Create the SVT conditions object to use to encapsulate SVT condition collections
        conditions.setChannelMap(channels);
        
        SvtCalibrationCollection calibrations = this.getCalibrations(dbConditionsManager);
        for (SvtCalibration calibration : calibrations.getObjects()) {
            SvtChannel channel = conditions.getChannelMap().findChannel(calibration.getChannelID());
            conditions.getChannelConstants(channel).setCalibration(calibration);
        }

        SvtShapeFitParametersCollection shapeFitParametersCollection = this.getShapeFitParameters(dbConditionsManager);
        for (SvtShapeFitParameters shapeFitParameters : shapeFitParametersCollection.getObjects()) {
            SvtChannel channel = conditions.getChannelMap().findChannel(shapeFitParameters.getChannelID());
            conditions.getChannelConstants(channel).setShapeFitParameters(shapeFitParameters);
        }
		
        try { 
        	
        	SvtBadChannelCollection badChannels = this.getBadChannels(dbConditionsManager);
        	for (SvtBadChannel badChannel : badChannels.getObjects()) {
        		SvtChannel channel = conditions.getChannelMap().findChannel(badChannel.getChannelId());
        		conditions.getChannelConstants(channel).setBadChannel(true);
        	}
        } catch (RuntimeException e) { 
        	System.out.println("[ " + conditions.getClass().getSimpleName() + "]: A set of bad channels were not found!");
        }

        SvtGainCollection channelGains = this.getChannelGains(dbConditionsManager);
        for (SvtGain channelGain : channelGains.getObjects()) {
            int channelId = channelGain.getChannelID();
            SvtChannel channel = conditions.getChannelMap().findChannel(channelId);
            conditions.getChannelConstants(channel).setGain(channelGain);
        }

        SvtT0ShiftCollection t0Shifts = this.getT0Shits(dbConditionsManager);
        conditions.setTimeShifts(t0Shifts);

        return conditions;
	}
	
    protected SvtChannelCollection getSvtChannelMap(DatabaseConditionsManager dbConditionsManager){
    	
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
    
    protected SvtCalibrationCollection getCalibrations(DatabaseConditionsManager dbConditionsManager){
    	
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
    
    protected SvtShapeFitParametersCollection getShapeFitParameters(DatabaseConditionsManager dbConditionsManager){
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
    
    // FIXME: This should be changed to catch a conditions record not found exception instead of 
  	// 		  a runtime exception.
    protected SvtBadChannelCollection getBadChannels(DatabaseConditionsManager dbConditionsManager)
    	throws RuntimeException {
        // Get the table name containing the SVT bad channel map from the 
        // database configuration.  If it doesn't exist, use the default value.
        metaData = dbConditionsManager.findTableMetaData(SvtBadChannelCollection.class);
    	if(metaData != null){
    		tableName = metaData.getTableName();
    	} else { 
    		tableName = SVT_BAD_CHANNELS; 
    	}
    	
        // Add bad channels.
        SvtBadChannelCollection badChannels = dbConditionsManager.getCachedConditions(SvtBadChannelCollection.class, tableName).getCachedData();
    
        return badChannels;
    }
    
    protected SvtGainCollection getChannelGains(DatabaseConditionsManager dbConditionsManager){
    	
        // Get the table name containing the SVT gains from the database
        // configuration.  If it doesn't exist, use the default value.
        metaData = dbConditionsManager.findTableMetaData(SvtGainCollection.class);
    	if(metaData != null){
    		tableName = metaData.getTableName();
    	} else { 
    		tableName = SVT_GAINS; 
    	}
        
        // Add gains by channel.
        SvtGainCollection gains = dbConditionsManager.getCachedConditions(SvtGainCollection.class, tableName).getCachedData();
    
        return gains;
    }
    
    protected SvtT0ShiftCollection getT0Shits(DatabaseConditionsManager dbConditionsManager){

    	// Get the table name containing the SVT t0 shifts. If it doesn't 
        // exist, use the default value. 
        metaData = dbConditionsManager.findTableMetaData(SvtT0ShiftCollection.class);
    	if(metaData != null){
    		tableName = metaData.getTableName();
    	} else { 
    		tableName = SVT_TIME_SHIFTS; 
    	}	
        // Set the t0 shifts by sensor.
        SvtT0ShiftCollection t0Shifts = dbConditionsManager.getCachedConditions(SvtT0ShiftCollection.class, tableName).getCachedData();
    
        return t0Shifts;
    }
}
