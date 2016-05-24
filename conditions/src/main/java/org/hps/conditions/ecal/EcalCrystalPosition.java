package org.hps.conditions.ecal;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;

/**
 * Stores information about front and back ECal crystal positions relative to the target.
 * 
 * @author jeremym
 */
@Table(names = {"ecal_crystal_positions"})
public class EcalCrystalPosition extends BaseConditionsObject {

    public static class EcalCrystalPositionCollection extends BaseConditionsObjectCollection<EcalCrystalPosition> {
    }

    @Field(names = {"ecal_channel_id"})
    public int getChannelId() {       
        return this.getFieldValue("ecal_channel_id");
    }

    @Field(names = {"front_x"})
    public double getFrontX() {
        return this.getFieldValue("front_x");
    }

    @Field(names = {"front_y"})
    public double getFrontY() {
        return this.getFieldValue("front_y");
    }

    @Field(names = {"front_z"})
    public double getFrontZ() {
        return this.getFieldValue("front_z");
    }

    @Field(names = {"back_x"})
    public double getBackX() {
        return this.getFieldValue("back_x");
    }

    @Field(names = {"back_y"})
    public double getBackY() {
        return this.getFieldValue("back_y");
    }

    @Field(names = {"back_z"})
    public double getBackZ() {
        return this.getFieldValue("back_z");
    }
}
