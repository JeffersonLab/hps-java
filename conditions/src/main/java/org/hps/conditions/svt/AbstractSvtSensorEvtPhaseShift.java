package org.hps.conditions.svt;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
//import org.hps.conditions.database.Field;
import org.hps.util.Pair;

/**
 * Abstract class providing some of the basic functionality used to relate a t0 shift value with either a FEB ID/FEB
 * hybrid ID or an FPGA ID/hybrid ID.
 */
public abstract class AbstractSvtSensorEvtPhaseShift extends BaseConditionsObject {

    /**
     * The collection implementation for this class.
     *
     * @param <T> the type of the object in this collection which extends {@link AbstractSvtSensorEvtPhaseShift}
     */
    @SuppressWarnings("serial")
    public static abstract class AbstractSvtSensorEvtPhaseShiftCollection<T extends AbstractSvtSensorEvtPhaseShift> extends
            BaseConditionsObjectCollection<T> {

        /**
         * Get the t0 shift associated with a given DAQ pair
         *
         * @param pair the DAQ pair for a given sensor
         * @return the t0 shift associated with the DAQ pair or null if not found
         */
        public abstract T getT0PhaseShift(final Pair<Integer, Integer> pair);
    }
  
}
