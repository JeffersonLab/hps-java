package org.hps.conditions.svt;

import org.hps.conditions.api.ConditionsSeries;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtBadChannel.SvtBadChannelCollection;
import org.hps.conditions.svt.SvtCalibration.SvtCalibrationCollection;
import org.hps.conditions.svt.SvtGain.SvtGainCollection;
import org.hps.conditions.svt.SvtShapeFitParameters.SvtShapeFitParametersCollection;
import org.hps.conditions.svt.TestRunSvtChannel.TestRunSvtChannelCollection;
import org.hps.conditions.svt.TestRunSvtDaqMapping.TestRunSvtDaqMappingCollection;
import org.hps.conditions.svt.TestRunSvtT0Shift.TestRunSvtT0ShiftCollection;
import org.lcsim.conditions.ConditionsManager;

/**
 * Converter for combined Test Run SVT conditions {@link TestRunSvtConditions} object.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class TestRunSvtConditionsConverter extends AbstractSvtConditionsConverter<TestRunSvtConditions> {

    /**
     * Class constructor.
     */
    public TestRunSvtConditionsConverter() {
        this.conditions = new TestRunSvtConditions();
    }

    /**
     * Get the Test Run {@link SvtShapeFitParametersCollection}.
     *
     * @param manager the conditions manager
     * @return the Test Run {@link SvtShapeFitParametersCollection}
     */
    protected SvtShapeFitParametersCollection getSvtShapeFitParametersCollection(
            final DatabaseConditionsManager manager) {
        return manager.getCachedConditions(SvtShapeFitParametersCollection.class,
                "test_run_svt_shape_fit_parameters").getCachedData();
    }

    /**
     * Get the Test Run series of {@link SvtBadChannelCollection} objects
     *
     * @param manager the conditions manager
     * @return the Test Run bad channel collections
     */
    protected ConditionsSeries<SvtBadChannel, SvtBadChannelCollection> getSvtBadChannelSeries(
            final DatabaseConditionsManager manager) {
        return manager.getConditionsSeries(SvtBadChannelCollection.class, "test_run_svt_bad_channels");
    }

    /**
     * Get the Test Run {@link SvtCalibrationCollection}.
     *
     * @param manager the conditions manager
     * @return the Test Run {@link SvtCalibrationCollection}
     */
    protected SvtCalibrationCollection getSvtCalibrationCollection(final DatabaseConditionsManager manager) {
        return manager.getCachedConditions(SvtCalibrationCollection.class, "test_run_svt_calibrations").getCachedData();
    }

    /**
     * Get the Test Run {@link SvtGainCollection}.
     *
     * @param manager the conditions manager
     * @return the Test Run {@link SvtGainCollection}
     */
    protected SvtGainCollection getSvtGainCollection(final DatabaseConditionsManager manager) {
        return manager.getCachedConditions(SvtGainCollection.class, "test_run_svt_gains").getCachedData();
    }

    /**
     * Create and return an {@link TestRunSvtConditions} object.
     *
     * @param manager The current conditions manager.
     * @param name The conditions key, which is ignored for now.
     */
    @Override
    public TestRunSvtConditions getData(final ConditionsManager manager, final String name) {

        final DatabaseConditionsManager dbConditionsManager = (DatabaseConditionsManager) manager;

        // Get the channel map from the conditions database
        final TestRunSvtChannelCollection channels = dbConditionsManager.getCachedConditions(
                TestRunSvtChannelCollection.class, "test_run_svt_channels").getCachedData();

        // Create the SVT conditions object to use to encapsulate SVT condition
        // collections
        conditions.setChannelMap(channels);

        // Get the DAQ map from the conditions database
        final TestRunSvtDaqMappingCollection daqMap = dbConditionsManager.getCachedConditions(
                TestRunSvtDaqMappingCollection.class, "test_run_svt_daq_map").getCachedData();
        conditions.setDaqMap(daqMap);

        // Get the collection of T0 shifts from the conditions database
        final TestRunSvtT0ShiftCollection t0Shifts = dbConditionsManager.getCachedConditions(
                TestRunSvtT0ShiftCollection.class, "test_run_svt_t0_shifts").getCachedData();
        conditions.setT0Shifts(t0Shifts);

        conditions = super.getData(manager, name);

        return conditions;
    }

    /**
     * Get the type handled by this converter.
     * @return The type handled by this converter.
     */
    @Override
    public Class<TestRunSvtConditions> getType() {
        return TestRunSvtConditions.class;
    }
}
