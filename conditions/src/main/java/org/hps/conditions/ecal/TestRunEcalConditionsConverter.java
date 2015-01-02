package org.hps.conditions.ecal;

import org.hps.conditions.api.ConditionsSeries;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalBadChannel.EcalBadChannelCollection;
import org.hps.conditions.ecal.EcalCalibration.EcalCalibrationCollection;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalGain.EcalGainCollection;
import org.hps.conditions.ecal.EcalTimeShift.EcalTimeShiftCollection;

/**
 * This class loads all ECal conditions into an {@link EcalConditions} object
 * from the database, based on the current run number known by the conditions
 * manager.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public final class TestRunEcalConditionsConverter extends EcalConditionsConverter {
    
    protected EcalChannelCollection getEcalChannelCollection(DatabaseConditionsManager manager) {
        return manager.getCachedConditions(EcalChannelCollection.class, "test_run_ecal_channels").getCachedData();
    }
    
    protected EcalGainCollection getEcalGainCollection(DatabaseConditionsManager manager) {
        return manager.getCachedConditions(EcalGainCollection.class, "test_run_ecal_gains").getCachedData();
    }
    
    protected ConditionsSeries<EcalBadChannel, EcalBadChannelCollection> getEcalBadChannelSeries(DatabaseConditionsManager manager) {
        return manager.getConditionsSeries(EcalBadChannelCollection.class, "test_run_ecal_bad_channels");
    }
    
    protected EcalCalibrationCollection getEcalCalibrationCollection(DatabaseConditionsManager manager) {
        return manager.getCachedConditions(EcalCalibrationCollection.class, "test_run_ecal_calibrations").getCachedData();
    }
    
    protected EcalTimeShiftCollection getEcalTimeShiftCollection(DatabaseConditionsManager manager) {
        return manager.getCachedConditions(EcalTimeShiftCollection.class, "test_run_ecal_time_shifts").getCachedData();
    }
}
