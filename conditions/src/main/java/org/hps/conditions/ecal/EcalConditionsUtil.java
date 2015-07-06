package org.hps.conditions.ecal;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel.GeometryId;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.identifier.Identifier;

/**
 * This is a set of utility methods for the ECAL that use the database conditions system.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class EcalConditionsUtil {

    /**
     * The combined ECAL conditions object.
     */
    private final EcalConditions conditions;

    /**
     * Constructor which will find the ECAL conditions from the static conditions manager instance.
     */
    public EcalConditionsUtil() {
        this.conditions = DatabaseConditionsManager.getInstance().getEcalConditions();
    }

    /**
     * Constructor which uses external reference to conditions object.
     *
     * @param conditions the ECAL conditions object
     */
    public EcalConditionsUtil(final EcalConditions conditions) {
        this.conditions = conditions;
    }

    /**
     * Find a channel object from a cell ID, e.g. from a <code>CalorimeterHit</code>.
     *
     * @param helper the identifier helper of the hit
     * @param cellId the cell ID of the hit
     * @return the corresponding ECAL channel found from the physical (geometric) ID information
     */
    public EcalChannel findChannel(final IIdentifierHelper helper, final long cellId) {

        // Make an ID object from the hit ID.
        final IIdentifier id = new Identifier(cellId);

        // Get the geometric field values which are subdetector ID and crystal indices.
        final int system = helper.getValue(id, "system");
        final int x = helper.getValue(id, "ix");
        final int y = helper.getValue(id, "iy");

        // Create an ID to searching in the channel collection.
        final GeometryId geometryId = new GeometryId(helper, new int[] {system, x, y});

        // Find and return the channel object.
        return this.conditions.getChannelCollection().findChannel(geometryId);
    }

    /**
     * Get the DAQ crate number from a cell ID.
     *
     * @param helper the identifier helper of the hit
     * @param cellId the cell ID of the hit
     * @return the crate number of the channel
     */
    public int getCrate(final IIdentifierHelper helper, final long cellId) {
        return this.findChannel(helper, cellId).getCrate();
    }

    /**
     * Get the DAQ slot number from a cell ID.
     *
     * @param helper the identifier helper of the hit
     * @param cellId the cell ID of the hit
     * @return the slot number of the channel
     */
    public int getSlot(final IIdentifierHelper helper, final long cellId) {
        return this.findChannel(helper, cellId).getSlot();
    }

}
