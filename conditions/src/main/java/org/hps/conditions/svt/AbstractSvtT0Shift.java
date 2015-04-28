package org.hps.conditions.svt;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Field;
import org.hps.util.Pair;

/**
 * Abstract class providing some of the basic functionality used to relate a t0 shift value with either a FEB ID/FEB
 * hybrid ID or an FPGA ID/hybrid ID.
 * 
 * @author <a href="mailto:omoreno1@ucsc.edu">Omar Moreno</a>
 */
public abstract class AbstractSvtT0Shift extends BaseConditionsObject {

    /**
     * The collection implementation for this class.
     * 
     * @param <T> the type of the object in this collection which extends {@link AbstractSvtT0Shift}
     */
    @SuppressWarnings("serial")
    public static abstract class AbstractSvtT0ShiftCollection<T extends AbstractSvtT0Shift> extends
            BaseConditionsObjectCollection<T> {

        /**
         * Get the t0 shift associated with a given DAQ pair
         *
         * @param pair the DAQ pair for a given sensor
         * @return the t0 shift associated with the DAQ pair or null if not found
         */
        public abstract T getT0Shift(final Pair<Integer, Integer> pair);
    }

    /**
     * Get the t0 shift.
     *
     * @return the t0 shift
     */
    @Field(names = {"t0_shift"})
    public final Double getT0Shift() {
        return getFieldValue("t0_shift");
    }
}
