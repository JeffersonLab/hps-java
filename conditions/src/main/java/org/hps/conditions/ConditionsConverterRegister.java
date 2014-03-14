package org.hps.conditions;

import org.hps.conditions.beam.BeamCurrentConverter;
import org.hps.conditions.ecal.EcalBadChannelConverter;
import org.hps.conditions.ecal.EcalCalibrationConverter;
import org.hps.conditions.ecal.EcalChannelMapConverter;
import org.hps.conditions.ecal.EcalConditionsConverter;
import org.hps.conditions.ecal.EcalGainConverter;
import org.hps.conditions.svt.SvtPulseParametersConverter;
import org.hps.conditions.svt.SvtBadChannelConverter;
import org.hps.conditions.svt.SvtCalibrationConverter;
import org.hps.conditions.svt.SvtChannelCollectionConverter;
import org.hps.conditions.svt.SvtConditionsConverter;
import org.hps.conditions.svt.SvtDaqMapConverter;
import org.hps.conditions.svt.SvtGainConverter;
import org.hps.conditions.svt.SvtTimeShiftConverter;
import org.lcsim.conditions.ConditionsManager;

/**
 * This class registers the full set of conditions converters onto the manager.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
class ConditionsConverterRegister {
    
    /**
     * This method will register all the conditions converters onto the given manager.
     * @param manager The conditions manager.
     */
    static void register(ConditionsManager manager) {
        
        // Create the table meta data registry.
        ConditionsTableRegistry tableRegistry = new ConditionsTableRegistry();
        tableRegistry.registerDefaultTableMetaData();
        
        // Create the object factory for SVT.
        ConditionsObjectFactory factory = 
                new BasicConditionsObjectFactory(ConnectionManager.getConnectionManager(), tableRegistry);
                
        // ConditionsRecords with validity meta data.
        manager.registerConditionsConverter(new ConditionsRecordConverter(factory));

        // SVT combined conditions.
        manager.registerConditionsConverter(new SvtConditionsConverter(factory));
        
        // SVT gains.  
        manager.registerConditionsConverter(new SvtGainConverter(factory));
        
        // SVT pulse parameters.
        manager.registerConditionsConverter(new SvtPulseParametersConverter(factory));
        
        // SVT calibrations.
        manager.registerConditionsConverter(new SvtCalibrationConverter(factory));
        
        // SVT channel map.
        manager.registerConditionsConverter(new SvtChannelCollectionConverter(factory));

        // SVT time shift by sensor.
        manager.registerConditionsConverter(new SvtTimeShiftConverter(factory));
        
        // SVT bad channels.
        manager.registerConditionsConverter(new SvtBadChannelConverter(factory));       
        
        // /\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/
        // TODO: Remaining to convert to new API...
        // /\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/  
                
        // ECAL bad channels.
        manager.registerConditionsConverter(new EcalBadChannelConverter(factory));
        
        // ECAL channel map.
        manager.registerConditionsConverter(new EcalChannelMapConverter(factory));
        
        // ECAL gains.
        manager.registerConditionsConverter(new EcalGainConverter(factory));
                
        // ECAL calibrations.
        manager.registerConditionsConverter(new EcalCalibrationConverter(factory));
                        
        // ECAL combined conditions.
        manager.registerConditionsConverter(new EcalConditionsConverter(factory));
        
        // Beam current condition.
        manager.registerConditionsConverter(new BeamCurrentConverter(factory));        
        
        ///////////////////////////////////////////////////////////////////////////////
        // This one will be a pain so convert to new API last once others are working.
        // SVT DAQ map.
        manager.registerConditionsConverter(new SvtDaqMapConverter(factory));
    }
}
