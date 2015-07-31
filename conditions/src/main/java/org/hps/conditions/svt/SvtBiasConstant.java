package org.hps.conditions.svt;

import java.util.Date;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;

/**
 * 
 * Encapsulates an SVT bias constant, which is range in time where bias was ON.
 * 
 * @author Per Hansson Adrian, SLAC
 */
@Table(names  = "svt_bias_constants")
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED)
public final class SvtBiasConstant extends BaseConditionsObject {

    /**
     * The collection implementation for {@link SvtBiasConstant}.
     */
    @SuppressWarnings("serial")
    public static class SvtBiasConstantCollection extends BaseConditionsObjectCollection<SvtBiasConstant> {
        
        /**
         * Find bias constant by date.
         * 
         * @param date the offset
         * @return the constant containing the date or <code>null</code> otherwise.
         * 
         */
        public SvtBiasConstant find(Date date) {
            for (SvtBiasConstant constant : this) {
                if(date.getTime() >= constant.getStart() && date.getTime() <= constant.getEnd()) {
                    return constant;
                }
            }
            return null;
        }
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
     * The bias value.
     *  
     * @return the bias value
     */
    @Field(names = {"value"})
    public Double getValue() {
        return getFieldValue("value");
    }
}
