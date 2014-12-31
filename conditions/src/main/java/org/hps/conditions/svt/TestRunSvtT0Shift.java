package org.hps.conditions.svt;

import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;
import org.hps.util.Pair;

/**
 * This class is a container that allows associating a t0 shift with a specific
 * sensor by FPGA ID and hybrid ID.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
@Table(names = {"test_run_svt_t0_shifts"})
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED)
public final class TestRunSvtT0Shift extends AbstractSvtT0Shift {

    public static class TestRunSvtT0ShiftCollection extends AbstractSvtT0Shift.AbstractSvtT0ShiftCollection<TestRunSvtT0Shift> {

        /**
         * Get the {@link TestRunSvtT0Shift} associated with a given DAQ pair
         * 
         * @param DAQ pair for a given sensor
         * @return The {@link TestRunSvtT0Shift} associated with the DAQ pair.
         *         If a t0 shift for a given DAQ pair can't be found, it returns
         *         null.
         */
        @Override
        public TestRunSvtT0Shift getT0Shift(Pair<Integer, Integer> pair) {

            int fpgaID = pair.getFirstElement();
            int hybridID = pair.getSecondElement();
            for (TestRunSvtT0Shift t0Shift : this) {
                if (t0Shift.getFpgaID() == fpgaID && t0Shift.getHybridID() == hybridID) {
                    return t0Shift;
                }
            }
            return null;
        }
    }

    /**
     * Get the FPGA ID.
     * 
     * @return The FPGA ID.
     */
    @Field(names = {"fpga"})
    public int getFpgaID() {
        return getFieldValue("fpga");
    }

    /**
     * Get the hybrid ID.
     * 
     * @return The hybrid ID.
     */
    @Field(names = {"hybrid"})
    public int getHybridID() {
        return getFieldValue("hybrid");
    }
}
