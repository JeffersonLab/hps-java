package org.hps.conditions.ecal;

import org.hps.conditions.api.ConditionsSeries;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalBadChannel.EcalBadChannelCollection;
import org.hps.conditions.ecal.EcalCalibration.EcalCalibrationCollection;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalGain.EcalGainCollection;
import org.hps.conditions.ecal.EcalTimeShift.EcalTimeShiftCollection;

/**
 * This class loads all Test Run ECAL conditions into an {@link EcalConditions} object
 * from the database.
 * <p>
 * The default names are overridden to use tables that contain Test Run data, only. 
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 * @author <a href="mailto:omoreno1@ucsc.edu">Omar Moreno</a>
 */
public final class TestRunEcalConditionsConverter extends EcalConditionsConverter {

    /**
     * Get the {@link EcalChannel} collection for Test Run.
     * @return The Test Run ECAL channel collection.
     */
    protected EcalChannelCollection getEcalChannelCollection(DatabaseConditionsManager manager) {
        return manager.getCachedConditions(EcalChannelCollection.class, "test_run_ecal_channels").getCachedData();
    }
    /**
     * Get the {@link EcalGain} collection for Test Run.
     * @return The Test Run ECAL gain collection.
     */     
    protected EcalGainCollection getEcalGainCollection(DatabaseConditionsManager manager) {
        return manager.getCachedConditions(EcalGainCollection.class, "test_run_ecal_gains").getCachedData();
    }
    
    /**
     * Get the collections of {@link EcalBadChannel} objects for Test Run.
     * @return The Test Run bad channel collections.
     */
    protected ConditionsSeries<EcalBadChannel, EcalBadChannelCollection> getEcalBadChannelSeries(DatabaseConditionsManager manager) {
        return manager.getConditionsSeries(EcalBadChannelCollection.class, "test_run_ecal_bad_channels");
    }

    /**
     * Get the {@link EcalCalibration} collection for Test Run.
     * @return The Test Run ECAL calibration collection. 
     */
    protected EcalCalibrationCollection getEcalCalibrationCollection(DatabaseConditionsManager manager) {
        return manager.getCachedConditions(EcalCalibrationCollection.class, "test_run_ecal_calibrations").getCachedData();
    }

    /**
     * Get the {@link EcalTimeShift} collection for Test Run.
     * @return The Test Run ECAL time shift collection.
     */
    protected EcalTimeShiftCollection getEcalTimeShiftCollection(DatabaseConditionsManager manager) {
        return manager.getCachedConditions(EcalTimeShiftCollection.class, "test_run_ecal_time_shifts").getCachedData();
    }
}
