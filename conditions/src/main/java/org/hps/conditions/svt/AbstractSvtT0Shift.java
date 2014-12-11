package org.hps.conditions.svt;

import org.hps.conditions.api.AbstractConditionsObject;
import org.hps.conditions.api.AbstractConditionsObjectCollection;
import org.hps.util.Pair;

/**
 * Abstract class providing some of the basic functionality used to relate a t0
 * shift value with either a FEB ID/FEB hybrid ID or an FPGA ID/hybrid ID.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public class AbstractSvtT0Shift extends AbstractConditionsObject {

    public static abstract class AbstractSvtT0ShiftCollection<T extends AbstractSvtT0Shift> extends AbstractConditionsObjectCollection<T> {

        /**
         * Get the t0 shift associated with a given DAQ pair
         * 
         * @param DAQ pair for a given sensor
         * @return The t0 shift associated with the DAQ pair. If a t0 shift for
         *         a given DAQ pair can't be found, it returns null.
         */
        public abstract T getT0Shift(Pair<Integer, Integer> pair);

    }

    /**
     * Get the t0 shift.
     * 
     * @return The t0 shift.
     */
    public double getT0Shift() {
        return getFieldValue("t0_shift");
    }
}
