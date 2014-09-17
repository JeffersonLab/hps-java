package org.hps.conditions.ecal;

import static org.hps.conditions.TableConstants.ECAL_BAD_CHANNELS;
import static org.hps.conditions.TableConstants.ECAL_CALIBRATIONS;
import static org.hps.conditions.TableConstants.ECAL_CHANNELS;
import static org.hps.conditions.TableConstants.ECAL_GAINS;
import static org.hps.conditions.TableConstants.ECAL_TIME_SHIFTS;


import org.lcsim.conditions.ConditionsConverter;
import org.lcsim.conditions.ConditionsManager;

import org.hps.conditions.ecal.EcalBadChannel.EcalBadChannelCollection;
import org.hps.conditions.ecal.EcalCalibration.EcalCalibrationCollection;
import org.hps.conditions.ecal.EcalChannel.ChannelId;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalGain.EcalGainCollection;
import org.hps.conditions.ecal.EcalTimeShift.EcalTimeShiftCollection;
import org.hps.conditions.DatabaseConditionsManager;
import org.hps.conditions.TableMetaData;

/**
 * This class loads all ecal conditions into an {@link EcalConditions} object from the
 * database, based on the current run number known by the conditions manager.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class EcalConditionsConverter implements ConditionsConverter<EcalConditions> {

	private TableMetaData metaData = null; 
	private String tableName = null;
	
    /**
     * Create ECAL conditions object containing all data for the current run.
     */
    public EcalConditions getData(ConditionsManager manager, String name) {

    	DatabaseConditionsManager dbConditionsManager = (DatabaseConditionsManager) manager;

        // Get the table name containing the Ecal channel map from the database
        // configuration.  If it doesn't exist, use the default value.
        metaData = dbConditionsManager.findTableMetaData(EcalChannelCollection.class);
        if(metaData != null){
        	tableName = metaData.getTableName();
        } else { 
        	tableName = ECAL_CHANNELS;  
        }
        // Get the Ecal channel map from the conditions database
        EcalChannelCollection channels = manager.getCachedConditions(EcalChannelCollection.class, tableName).getCachedData();

    	// Create the Ecal conditions object that will be used to encapsulate
        // Ecal conditions collections
        EcalConditions conditions = new EcalConditions();
        
        // Set the channel map.
        conditions.setChannelCollection(channels);

        System.out.println("channel collection size = " + channels.getObjects().size());

        // Get the table name containing the Ecal gains from the database
        // configuration. If it doesn't exist, use the default value.
        metaData = dbConditionsManager.findTableMetaData(EcalGainCollection.class);
        if(metaData != null){
        	tableName = metaData.getTableName();
        } else { 
        	tableName = ECAL_GAINS;  
        }
        // Add the gains
        EcalGainCollection gains = manager.getCachedConditions(EcalGainCollection.class, tableName).getCachedData();
        for (EcalGain gain : gains.getObjects()) {
            ChannelId channelId = new ChannelId(new int[] {gain.getChannelId()});
            EcalChannel channel = channels.findChannel(channelId);
            conditions.getChannelConstants(channel).setGain(gain);
        }

        // Get the table name containing the Ecal bad channel map from the 
        // database configuration. If it doesn't exist, use the default value.
        metaData = dbConditionsManager.findTableMetaData(EcalBadChannelCollection.class);
        if(metaData != null){
        	tableName = metaData.getTableName();
        } else { 
        	tableName = ECAL_BAD_CHANNELS;  
        }
        
        // Add bad channels.
        // FIXME: This should be changed to catch a conditions record not found
        // exception instead of a runtime exception
        try { 
        	EcalBadChannelCollection badChannels = manager.getCachedConditions(EcalBadChannelCollection.class, tableName).getCachedData();
        	for (EcalBadChannel badChannel : badChannels.getObjects()) {
        		ChannelId channelId = new ChannelId(new int[] {badChannel.getChannelId()});
        		EcalChannel channel = channels.findChannel(channelId);
        		conditions.getChannelConstants(channel).setBadChannel(true);
        	}
        } catch(RuntimeException e){
        	e.printStackTrace();
        }

        
        // Get the table name containing the Ecal calibrations (pedestal, noise)
        // from the database configuration. If it doesn't exist, use the default value.
        metaData = dbConditionsManager.findTableMetaData(EcalCalibrationCollection.class);
        if(metaData != null){
        	tableName = metaData.getTableName();
        } else { 
        	tableName = ECAL_CALIBRATIONS;  
        }
        // Add calibrations including pedestal and noise values.
        EcalCalibrationCollection calibrations = manager.getCachedConditions(EcalCalibrationCollection.class, tableName).getCachedData();
        for (EcalCalibration calibration : calibrations.getObjects()) {
            ChannelId channelId = new ChannelId(new int[] {calibration.getChannelId()});
            EcalChannel channel = channels.findChannel(channelId);
            conditions.getChannelConstants(channel).setCalibration(calibration);
        }

        // Get the table name containing the Ecal calibrations (pedestal, noise)
        // from the database configuration. If it doesn't exist, use the default value.
        metaData = dbConditionsManager.findTableMetaData(EcalTimeShiftCollection.class);
        if(metaData != null){
        	tableName = metaData.getTableName();
        } else { 
        	tableName = ECAL_TIME_SHIFTS;  
        }
        // Add time shifts.
        EcalTimeShiftCollection timeShifts = manager.getCachedConditions(EcalTimeShiftCollection.class, tableName).getCachedData();
        for (EcalTimeShift timeShift : timeShifts.getObjects()) {
            ChannelId channelId = new ChannelId(new int[] {timeShift.getChannelId()});
            EcalChannel channel = channels.findChannel(channelId);
            conditions.getChannelConstants(channel).setTimeShift(timeShift);
        }

        // Return the conditions object to caller.
        return conditions;
    }

    /**
     * Get the type handled by this converter.
     * @return The type handled by this converter.
     */
    public Class<EcalConditions> getType() {
        return EcalConditions.class;
    }

}
