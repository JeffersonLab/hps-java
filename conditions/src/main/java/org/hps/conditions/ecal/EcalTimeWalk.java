package org.hps.conditions.ecal;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;

/**
 * ECal time walk conditions consisting of 5 double parameters for input to time walk correction algorithm.
 *
 * @author Jeremy McCormick, SLAC
 */
@Table(names = {"ecal_time_walk"})
public final class EcalTimeWalk extends BaseConditionsObject {

    /**
     * The collection implementation for the object class.
     */
    public static class EcalTimeWalkCollection extends BaseConditionsObjectCollection<EcalTimeWalk> {
    }

    /**
     * Get parameter 0 value.
     * @return parameter 0 value
     */
    @Field(names = {"p0"})
    public Integer getP0() {
        return this.getFieldValue("p0");
    }
    
    /**
     * Get parameter 1 value.
     * @return parameter 1 value
     */
    @Field(names = {"p1"})
    public Integer getP1() {
        return this.getFieldValue("p1");
    }
    
    /**
     * Get parameter 2 value.
     * @return parameter 2 value
     */
    @Field(names = {"p2"})
    public Integer getP2() {
        return this.getFieldValue("p2");
    }
    
    /**
     * Get parameter 3 value.
     * @return parameter 3 value
     */
    @Field(names = {"p3"})
    public Integer getP3() {
        return this.getFieldValue("p3");
    }
    
    /**
     * Get parameter 4 value.
     * @return parameter 4 value
     */
    @Field(names = {"p4"})
    public Integer getP4() {
        return this.getFieldValue("p4");
    }
}