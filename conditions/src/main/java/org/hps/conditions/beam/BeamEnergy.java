package org.hps.conditions.beam;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;

/**
 * Beam current condition with nominal values.
 */
@Table(names = {"beam_energies"})
public final class BeamEnergy extends BaseConditionsObject {

    /**
     * The collection implementation for this class.
     */
    public static final class BeamEnergyCollection extends BaseConditionsObjectCollection<BeamEnergy> {
    }

    /**
     * Get the beam energy [GeV].
     * 
     * @return the beam energy [GeV]
     */
    @Field(names = {"beam_energy"})
    public Double getBeamEnergy() {
        return this.getFieldValue("beam_energy");
    }
}