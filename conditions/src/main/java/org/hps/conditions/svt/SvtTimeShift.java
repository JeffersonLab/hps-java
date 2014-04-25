package org.hps.conditions.svt;

import org.hps.conditions.AbstractConditionsObject;
import org.hps.conditions.ConditionsObjectCollection;
import org.hps.conditions.ConditionsObjectException;
import org.hps.util.Pair;

/**
 * This class is a data holder for associating a time shift with a specific sensor by FPGA
 * and hybrid numbers.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class SvtTimeShift extends AbstractConditionsObject {

    public static class SvtTimeShiftCollection extends ConditionsObjectCollection<SvtTimeShift> {

        SvtTimeShiftCollection find(Pair<Integer, Integer> pair) {
            SvtTimeShiftCollection timeShifts = new SvtTimeShiftCollection();
            int fpga = pair.getFirstElement();
            int hybrid = pair.getSecondElement();
            for (SvtTimeShift timeShift : getObjects()) {
                if (timeShift.getFpga() == fpga && timeShift.getHybrid() == hybrid) {
                    try {
                        timeShifts.add(timeShift);
                    } catch (ConditionsObjectException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            return timeShifts;
        }
    }

    /**
     * Get the FPGA number.
     * @return The FPGA number.
     */
    int getFpga() {
        return getFieldValue("fpga");
    }

    /**
     * Get the hybrid number.
     * @return The hybrid number.
     */
    int getHybrid() {
        return getFieldValue("hybrid");
    }

    /**
     * Get the time shift.
     * @return The time shift.
     */
    double getTimeShift() {
        return getFieldValue("time_shift");
    }
}
