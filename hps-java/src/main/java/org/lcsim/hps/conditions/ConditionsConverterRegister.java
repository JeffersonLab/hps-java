package org.lcsim.hps.conditions;

import org.lcsim.conditions.ConditionsManager;
import org.lcsim.hps.conditions.beam.BeamCurrentConverter;
import org.lcsim.hps.conditions.ecal.EcalBadChannelConverter;
import org.lcsim.hps.conditions.ecal.EcalCalibrationConverter;
import org.lcsim.hps.conditions.ecal.EcalChannelMapConverter;
import org.lcsim.hps.conditions.ecal.EcalConditionsConverter;
import org.lcsim.hps.conditions.ecal.EcalGainConverter;
import org.lcsim.hps.conditions.svt.PulseParametersConverter;
import org.lcsim.hps.conditions.svt.SvtBadChannelConverter;
import org.lcsim.hps.conditions.svt.SvtCalibrationConverter;
import org.lcsim.hps.conditions.svt.SvtChannelMapConverter;
import org.lcsim.hps.conditions.svt.SvtConditionsConverter;
import org.lcsim.hps.conditions.svt.SvtDaqMapConverter;
import org.lcsim.hps.conditions.svt.SvtGainConverter;
import org.lcsim.hps.conditions.svt.SvtTimeShiftConverter;

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
        
        // ConditionsRecords with validity meta data.
        manager.registerConditionsConverter(new ConditionsRecordConverter());
        
        // SVT channel map.
        manager.registerConditionsConverter(new SvtChannelMapConverter());
        
        // SVT DAQ map.
        manager.registerConditionsConverter(new SvtDaqMapConverter());
        
        // SVT calibrations.
        manager.registerConditionsConverter(new SvtCalibrationConverter());
        
        // SVT gains.
        manager.registerConditionsConverter(new SvtGainConverter());
        
        // SVT bad channels.
        manager.registerConditionsConverter(new SvtBadChannelConverter());       
       
        // SVT time shift by sensor.
        manager.registerConditionsConverter(new SvtTimeShiftConverter());
                
        // SVT combined conditions.
        manager.registerConditionsConverter(new SvtConditionsConverter());
        
        // ECAL bad channels.
        manager.registerConditionsConverter(new EcalBadChannelConverter());
        
        // ECAL channel map.
        manager.registerConditionsConverter(new EcalChannelMapConverter());
        
        // ECAL gains.
        manager.registerConditionsConverter(new EcalGainConverter());
                
        // ECAL calibrations.
        manager.registerConditionsConverter(new EcalCalibrationConverter());
        
        // ECAL pulse parameters.
        manager.registerConditionsConverter(new PulseParametersConverter());
        
        // ECAL combined conditions.
        manager.registerConditionsConverter(new EcalConditionsConverter());
        
        // Beam current condition.
        manager.registerConditionsConverter(new BeamCurrentConverter());
    }
}
