package org.hps.conditions.svt;

import org.lcsim.conditions.ConditionsManager;
import org.hps.conditions.api.ConditionsSeries;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtBadChannel.SvtBadChannelCollection;
import org.hps.conditions.svt.SvtCalibration.SvtCalibrationCollection;
import org.hps.conditions.svt.SvtGain.SvtGainCollection;
import org.hps.conditions.svt.SvtShapeFitParameters.SvtShapeFitParametersCollection;
import org.hps.conditions.svt.TestRunSvtChannel.TestRunSvtChannelCollection;
import org.hps.conditions.svt.TestRunSvtDaqMapping.TestRunSvtDaqMappingCollection;
import org.hps.conditions.svt.TestRunSvtT0Shift.TestRunSvtT0ShiftCollection;

public final class TestRunSvtConditionsConverter extends AbstractSvtConditionsConverter<TestRunSvtConditions> {

    public TestRunSvtConditionsConverter() {
        this.conditions = new TestRunSvtConditions();
    }
    
    protected SvtShapeFitParametersCollection getSvtShapeFitParametersCollection(DatabaseConditionsManager manager) {
        return manager.getCachedConditions(SvtShapeFitParametersCollection.class, "test_run_svt_shape_fit_parameters").getCachedData();
    }
    
    protected ConditionsSeries<SvtBadChannel, SvtBadChannelCollection> getSvtBadChannelSeries(DatabaseConditionsManager manager) {
        return manager.getConditionsSeries(SvtBadChannelCollection.class, "test_run_svt_bad_channels");
    }
    
    protected SvtCalibrationCollection getSvtCalibrationCollection(DatabaseConditionsManager manager) {
        return manager.getCachedConditions(SvtCalibrationCollection.class, "test_run_svt_calibrations").getCachedData();
    }
    
    protected SvtGainCollection getSvtGainCollection(DatabaseConditionsManager manager) {
        return manager.getCachedConditions(SvtGainCollection.class, "test_run_svt_gains").getCachedData();
    }

    /**
     * Create and return an {@link TestRunSvtConditions} object
     * 
     * @param manager The current conditions manager.
     * @param name The conditions key, which is ignored for now.
     */
    @Override
    public TestRunSvtConditions getData(ConditionsManager manager, String name) {

        DatabaseConditionsManager dbConditionsManager = (DatabaseConditionsManager) manager;

        // Get the channel map from the conditions database
        TestRunSvtChannelCollection channels = dbConditionsManager.getCollection(TestRunSvtChannelCollection.class);

        // Create the SVT conditions object to use to encapsulate SVT condition
        // collections
        conditions.setChannelMap(channels);

        // Get the DAQ map from the conditions database
        TestRunSvtDaqMappingCollection daqMap = dbConditionsManager.getCollection(TestRunSvtDaqMappingCollection.class);
        conditions.setDaqMap(daqMap);

        // Get the collection of T0 shifts from the conditions database
        TestRunSvtT0ShiftCollection t0Shifts = dbConditionsManager.getCollection(TestRunSvtT0ShiftCollection.class);
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
