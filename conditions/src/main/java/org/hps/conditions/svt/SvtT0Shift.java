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
public final class SvtT0Shift extends AbstractConditionsObject {

    public static class SvtT0ShiftCollection extends ConditionsObjectCollection<SvtT0Shift> {

        SvtT0ShiftCollection find(Pair<Integer, Integer> pair) {
            SvtT0ShiftCollection t0Shifts = new SvtT0ShiftCollection();
            int febID = pair.getFirstElement();
            int febHybridID = pair.getSecondElement();
            for (SvtT0Shift timeShift : getObjects()) {
                if (timeShift.getFebID() == febID && timeShift.getFebHybridID() == febHybridID) {
                    try {
                        t0Shifts.add(timeShift);
                    } catch (ConditionsObjectException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            return t0Shifts;
        }
    }

    /**
     * Get the FPGA number.
     * @return The FPGA number.
     */
    int getFebID() {
        return getFieldValue("feb_id");
    }

    /**
     * Get the hybrid number.
     * @return The hybrid number.
     */
    int getFebHybridID() {
        return getFieldValue("feb_hybrid_id");
    }

    /**
     * Get the time shift.
     * @return The time shift.
     */
    double getT0Shift() {
        return getFieldValue("t0_shift");
    }
}
