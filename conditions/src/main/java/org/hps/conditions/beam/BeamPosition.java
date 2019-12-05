package org.hps.conditions.beam;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;

/**
 * Beam x, y, and z position in millimeters.
 * 
 * @author jeremym
 */
@Table(names = {"beam_positions"})
public final class BeamPosition extends BaseConditionsObject {

    public static final class BeamPositionCollection extends BaseConditionsObjectCollection<BeamPosition> {
    }

    /**
     * Get beam position in X (mm).
     * @return Beam position in X (mm)
     */
    @Field(names = {"x"})
    public Double getPositionX() {
        return this.getFieldValue("x");
    }
    
    /**
     * Get beam position in Y (mm).
     * @return Beam position in Y (mm)
     */
    @Field(names = {"y"})
    public Double getPositionY() {
        return this.getFieldValue("y");
    }
    
    /**
     * Get beam position in Z (mm).
     * @return Beam position in Z (mm)
     */
    @Field(names = {"z"})
    public Double getPositionZ() {
        return this.getFieldValue("z");
    }
}