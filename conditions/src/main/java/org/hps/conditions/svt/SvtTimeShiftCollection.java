package org.hps.conditions.svt;

import org.hps.conditions.ConditionsObjectCollection;
import org.hps.conditions.ConditionsTableMetaData;
import org.lcsim.hps.util.Pair;

/**
 * A simple collection of {@link SvtTimeShift} objects.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtTimeShiftCollection extends ConditionsObjectCollection<SvtTimeShift> {

    SvtTimeShiftCollection(ConditionsTableMetaData tableMetaData, int collectionId, boolean isReadOnly) {
        super(tableMetaData, collectionId, isReadOnly);
    }
    
    SvtTimeShiftCollection find(Pair<Integer,Integer> pair) {
        SvtTimeShiftCollection timeShifts = new SvtTimeShiftCollection(this.getTableMetaData(), -1, false);
        int fpga = pair.getFirstElement();
        int hybrid = pair.getSecondElement();
        for (SvtTimeShift timeShift : getObjects()) {
            if (timeShift.getFpga() == fpga && timeShift.getHybrid() == hybrid) {
                timeShifts.add(timeShift);
            }
        }
        return timeShifts;
    }
}
