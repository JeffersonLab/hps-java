package org.hps.conditions.svt;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtChannel.SvtChannelCollection;
import org.hps.conditions.svt.SvtDaqMapping.SvtDaqMappingCollection;
import org.hps.conditions.svt.SvtT0Shift.SvtT0ShiftCollection;
import org.lcsim.conditions.ConditionsManager;

/**
 * This class creates an {@link SvtConditions} object from the database, based
 * on the current run number known by the conditions manager.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 * @author <a href="mailto:omoreno1@ucsc.edu">Omar Moreno</a>
 */
public final class SvtConditionsConverter extends AbstractSvtConditionsConverter<SvtConditions> {

    /**
     * Default constructor.
     */
    public SvtConditionsConverter() {
        this.conditions = new SvtConditions();
    }

    /**
     * Create and return an {@link SvtConditions} object.
     *
     * @param manager the current conditions manager
     * @param name the conditions key, which is ignored for now
     */
    @Override
    public SvtConditions getData(final ConditionsManager manager, final String name) {

        final DatabaseConditionsManager dbConditionsManager = (DatabaseConditionsManager) manager;

        // Get the channel map from the conditions database
        final SvtChannelCollection channels = dbConditionsManager.getCachedConditions(
                SvtChannelCollection.class, "svt_channels").getCachedData();

        // Create the SVT conditions object to use to encapsulate SVT condition
        // collections
        conditions.setChannelMap(channels);

        // Get the DAQ map from the conditions database
        final SvtDaqMappingCollection daqMap = dbConditionsManager.getCachedConditions(
                SvtDaqMappingCollection.class, "svt_daq_map").getCachedData();
        conditions.setDaqMap(daqMap);

        // Get the collection of T0 shifts from the conditions database
        final SvtT0ShiftCollection t0Shifts = dbConditionsManager.getCachedConditions(
                SvtT0ShiftCollection.class, "svt_t0_shifts").getCachedData();
        conditions.setT0Shifts(t0Shifts);

        conditions = super.getData(manager, name);

        return conditions;
    }

    /**
     * Get the type handled by this converter.
     * @return The type handled by this converter.
     */
    @Override
    public Class<SvtConditions> getType() {
        return SvtConditions.class;
    }
}
