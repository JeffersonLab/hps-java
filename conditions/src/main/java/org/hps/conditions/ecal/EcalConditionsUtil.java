package org.hps.conditions.ecal;

import org.hps.conditions.DatabaseConditionsManager;
import org.hps.conditions.TableConstants;
import org.hps.conditions.ecal.EcalChannel.GeometryId;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.identifier.Identifier;

/**
 * This is a set of utility methods for the ECAL that use the database conditions system.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class EcalConditionsUtil {

    private EcalConditions conditions;

    /**
     * Constructor which uses external reference to conditions object.
     * @param conditions The ECAL conditions object.
     */
    public EcalConditionsUtil(EcalConditions conditions) {
        this.conditions = conditions;
    }

    /**
     * Constructor which will find the ECAL conditions from the static conditions manager
     * instance.
     */
    public EcalConditionsUtil() {
        conditions = DatabaseConditionsManager.getInstance().getConditionsData(EcalConditions.class, TableConstants.ECAL_CONDITIONS);
    }

    /**
     * Find a channel object from a cell ID, e.g. from Monte Carlo data.
     * @param helper The identifier helper of the hit.
     * @param cellId The cell ID of the hit.
     * @return The corresponding ECAL channel found from the physical ID information.
     */
    EcalChannel findChannel(IIdentifierHelper helper, long cellId) {

        // Make an ID object from hit ID.
        IIdentifier id = new Identifier(cellId);

        // Get physical field values.
        int system = helper.getValue(id, "system");
        int x = helper.getValue(id, "ix");
        int y = helper.getValue(id, "iy");

        // Create an ID to search for in channel collection.
        GeometryId geometryId = new GeometryId(helper, new int[] { system, x, y });

        // Find the ECAL channel and return the crate number.
        return conditions.getChannelCollection().findChannel(geometryId);
    }

    /**
     * Get the DAQ crate number from a cell ID.
     * @param helper The identifier helper of the hit.
     * @param cellId The cell ID of the hit.
     * @return The crate number of the channel.
     */
    int getCrate(IIdentifierHelper helper, long cellId) {
        return findChannel(helper, cellId).getCrate();
    }

    /**
     * Get the DAQ slot number from a cell ID.
     * @param helper The identifier helper of the hit.
     * @param cellId The cell ID of the hit.
     * @return The slot number of the channel.
     */
    int getSlot(IIdentifierHelper helper, long cellId) {
        return findChannel(helper, cellId).getSlot();
    }

}
