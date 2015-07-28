package org.hps.conditions.ecal;

import org.hps.conditions.api.ConditionsSeries;
import org.hps.conditions.ecal.EcalBadChannel.EcalBadChannelCollection;
import org.hps.conditions.ecal.EcalCalibration.EcalCalibrationCollection;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalGain.EcalGainCollection;
import org.hps.conditions.ecal.EcalTimeShift.EcalTimeShiftCollection;

/**
 * This class loads all Test Run ECAL conditions into an {@link EcalConditions} object from the database.
 * <p>
 * The default names are overridden to use tables that contain only Test Run data.
 *
 * @author Jeremy McCormick, SLAC
 * @author Omar Moreno, UCSC
 */
public final class TestRunEcalConditionsConverter extends EcalConditionsConverter {

    /**
     * Get the collections of {@link EcalBadChannel} objects for Test Run.
     *
     * @param manager the conditions manager
     * @return the Test Run bad channel collections
     */
    @Override
    protected ConditionsSeries<EcalBadChannel, EcalBadChannelCollection> getEcalBadChannelSeries() {
        return getDatabaseConditionsManager().getConditionsSeries(EcalBadChannelCollection.class, "test_run_ecal_bad_channels");
    }

    /**
     * Get the {@link EcalCalibration} collection for Test Run.
     *
     * @param manager the conditions manager
     * @return the Test Run ECAL calibration collection
     */
    @Override
    protected EcalCalibrationCollection getEcalCalibrationCollection() {
        return getDatabaseConditionsManager().getCachedConditions(EcalCalibrationCollection.class, "test_run_ecal_calibrations")
                .getCachedData();
    }

    /**
     * Get the {@link EcalChannel} collection for Test Run.
     *
     * @param manager the conditions manager
     * @return the Test Run ECAL channel collection
     */
    @Override
    protected EcalChannelCollection getEcalChannelCollection() {
        return getDatabaseConditionsManager().getCachedConditions(EcalChannelCollection.class, "test_run_ecal_channels").getCachedData();
    }

    /**
     * Get the {@link EcalGain} collection for Test Run.
     *
     * @param manager the conditions manager
     * @return the Test Run ECAL gain collection
     */
    @Override
    protected EcalGainCollection getEcalGainCollection() {
        return getDatabaseConditionsManager().getCachedConditions(EcalGainCollection.class, "test_run_ecal_gains").getCachedData();
    }

    /**
     * Get the {@link EcalTimeShift} collection for Test Run.
     *
     * @param manager the conditions manager
     * @return the Test Run ECAL time shift collection
     */
    @Override
    protected EcalTimeShiftCollection getEcalTimeShiftCollection() {
        return getDatabaseConditionsManager().getCachedConditions(EcalTimeShiftCollection.class, "test_run_ecal_time_shifts").getCachedData();
    }
}
