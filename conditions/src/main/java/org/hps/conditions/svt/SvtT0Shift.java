package org.hps.conditions.svt;

import org.hps.conditions.AbstractConditionsObject;
import org.hps.conditions.ConditionsObjectCollection;
import org.hps.conditions.ConditionsObjectException;
import org.hps.util.Pair;

/**
 * This class is a data holder for associating a t0 shift with a specific 
 * sensor by FEB ID and FEB hybrid ID.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public final class SvtT0Shift extends AbstractSvtT0Shift {

    public static class SvtT0ShiftCollection 
        extends AbstractSvtT0Shift.AbstractSvtT0ShiftCollection<SvtT0Shift> {

        @Override
        public SvtT0Shift getT0Shift(Pair<Integer, Integer> pair) {
            int febID = pair.getFirstElement();
            int febHybridID = pair.getSecondElement();
            for (SvtT0Shift t0Shift : this.getObjects()) {
                if (t0Shift.getFebID() == febID && t0Shift.getFebHybridID() == febHybridID) {
                    return t0Shift;
                }
            }
            return null;
        }
    }

    /**
     * Get the FEB ID.
     * 
     * @return The FEB ID.
     */
    int getFebID() {
        return getFieldValue("feb_id");
    }

    /**
     * Get the FEB hybrid ID.
     *
     * @return The FEB hybrid ID.
     */
    int getFebHybridID() {
        return getFieldValue("feb_hybrid_id");
    }
}
