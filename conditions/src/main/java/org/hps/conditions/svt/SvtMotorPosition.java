package org.hps.conditions.svt;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;

/**
 * Represents the SVT motor position as a time-based condition.
 * 
 * @author Jeremy McCormick, SLAC
 */
@Table(names  = "svt_motor_positions")
public class SvtMotorPosition extends BaseConditionsObject {
    
    /**
     * Collection implementation.
     */
    public class SvtMotorPositionCollection extends BaseConditionsObjectCollection<SvtMotorPosition> {        
    }
    
    /**
     * The start date as a Unix timestamp in milliseconds (GMT). 
     * 
     * @return the start date as a Unix timestamp
     */
    @Field(names = {"start"})
    public Long getStart() {
        return getFieldValue("start");
    }

    /**
     * The end date as a Unix timestamp in milliseconds (GMT). 
     * 
     * @return the end date as a Unix timestamp
     */
    @Field(names = {"end"})
    public Long getEnd() {
        return getFieldValue("end");
    }

    /**
     * The top position.
     *  
     * @return the top motor position
     */
    @Field(names = {"top"})
    public Double getTop() {
        return getFieldValue("top");
    }
    
    /**
     * The bottom position.
     * 
     * @return the bottom motor position
     */
    @Field(names = {"bottom"})
    public Double getBottom() {
        return getFieldValue("bottom");
    }
}
