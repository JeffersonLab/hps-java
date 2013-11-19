package org.lcsim.hps.conditions.svt;

import java.util.ArrayList;

import org.lcsim.hps.util.Pair;

/**
 * A simple collection of {@link SvtTimeShift} objects.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtTimeShiftCollection extends ArrayList<SvtTimeShift> {

    SvtTimeShiftCollection find(Pair<Integer,Integer> pair) {
        SvtTimeShiftCollection timeShifts = new SvtTimeShiftCollection();
        int fpga = pair.getFirstElement();
        int hybrid = pair.getSecondElement();
        for (SvtTimeShift timeShift : this) {
            if (timeShift.getFpga() == fpga && timeShift.getHybrid() == hybrid) {
                timeShifts.add(timeShift);
            }
        }
        return timeShifts;
    }
}
