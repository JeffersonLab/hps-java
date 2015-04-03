package org.hps.conditions.ecal;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel.GeometryId;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.identifier.Identifier;

/**
 * This is a set of utility methods for the ECAL that use the database
 * conditions system.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class EcalConditionsUtil {

    /**
     * The combined ECAL conditions object.
     */
    private EcalConditions conditions;

    /**
     * Constructor which uses external reference to conditions object.
     * @param conditions The ECAL conditions object.
     */
    public EcalConditionsUtil(final EcalConditions conditions) {
        this.conditions = conditions;
    }

    /**
     * Constructor which will find the ECAL conditions from the static
     * conditions manager instance.
     */
    public EcalConditionsUtil() {
        conditions = DatabaseConditionsManager.getInstance().getEcalConditions();
    }

    /**
     * Find a channel object from a cell ID, e.g. from Monte Carlo data.
     * @param helper The identifier helper of the hit.
     * @param cellId The cell ID of the hit.
     * @return The corresponding ECAL channel found from the physical ID information.
     */
    public EcalChannel findChannel(final IIdentifierHelper helper, final long cellId) {

        // Make an ID object from hit ID.
        final IIdentifier id = new Identifier(cellId);

        // Get physical field values.
        final int system = helper.getValue(id, "system");
        final int x = helper.getValue(id, "ix");
        final int y = helper.getValue(id, "iy");

        // Create an ID to search for in channel collection.
        final GeometryId geometryId = new GeometryId(helper, new int[] { system, x, y });

        // Find the ECAL channel and return the crate number.
        return conditions.getChannelCollection().findChannel(geometryId);
    }

    /**
     * Get the DAQ crate number from a cell ID.
     * @param helper The identifier helper of the hit.
     * @param cellId The cell ID of the hit.
     * @return The crate number of the channel.
     */
    public int getCrate(final IIdentifierHelper helper, final long cellId) {
        return findChannel(helper, cellId).getCrate();
    }

    /**
     * Get the DAQ slot number from a cell ID.
     * @param helper The identifier helper of the hit.
     * @param cellId The cell ID of the hit.
     * @return The slot number of the channel.
     */
    public int getSlot(final IIdentifierHelper helper, final long cellId) {
        return findChannel(helper, cellId).getSlot();
    }

}
