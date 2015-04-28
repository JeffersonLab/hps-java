package org.hps.conditions.beam;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;

/**
 * Beam-related detector conditions, including current, position in X and Y, and energy.
 * <p>
 * Unless otherwise stated, these are assumed to be average values for an entire run.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@Table(names = {"beam"})
@Converter(multipleCollectionsAction = MultipleCollectionsAction.ERROR)
public final class BeamConditions extends BaseConditionsObject {

    /**
     * Collection implementation for this class.
     */
    @SuppressWarnings("serial")
    public static final class BeamConditionsCollection extends BaseConditionsObjectCollection<BeamConditions> {
    }

    /**
     * No arg constructor.
     */
    public BeamConditions() {
    }

    /**
     * Get the average beam current (nA). A value of 0 indicates there was no beam. A null value means it was not
     * recorded.
     * 
     * @return the beam current (nA)
     */
    @Field(names = {"current"})
    public Double getCurrent() {
        return getFieldValue("current");
    }

    /**
     * Get the average beam position in X (mm).
     * 
     * @return the beam position (mm)
     */
    @Field(names = {"position_x"})
    public Double getPositionX() {
        return getFieldValue("position_x");
    }

    /**
     * Get the average beam position in Y (mm).
     * 
     * @return the beam position (mm)
     */
    @Field(names = {"position_y"})
    public Double getPositionY() {
        return getFieldValue("position_y");
    }

    /**
     * Get the beam energy (GeV). A value of 0 indicates there was no beam. A null value means it was not recorded.
     * 
     * @return the beam energy
     */
    @Field(names = {"energy"})
    public Double getEnergy() {
        return getFieldValue("energy");
    }
}
