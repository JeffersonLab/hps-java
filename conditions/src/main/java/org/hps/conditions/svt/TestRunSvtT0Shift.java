package org.hps.conditions.svt;

import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;
import org.hps.util.Pair;

/**
 * This class is a container that allows associating a t0 shift with a specific sensor by FPGA ID and hybrid ID.
 *
 * @author Omar Moreno, UCSC
 */
@Table(names = {"test_run_svt_t0_shifts"})
public final class TestRunSvtT0Shift extends AbstractSvtT0Shift {

    /**
     * Collection implementation for {@link TestRunSvtT0Shift} objects.
     */
    @SuppressWarnings("serial")
    public static class TestRunSvtT0ShiftCollection extends
            AbstractSvtT0Shift.AbstractSvtT0ShiftCollection<TestRunSvtT0Shift> {

        /**
         * Get the {@link TestRunSvtT0Shift} associated with a given DAQ pair
         *
         * @param DAQ pair for a given sensor
         * @return the {@link TestRunSvtT0Shift} associated with the DAQ pair or null if does not exist
         */
        @Override
        public TestRunSvtT0Shift getT0Shift(final Pair<Integer, Integer> pair) {
            final int fpgaID = pair.getFirstElement();
            final int hybridID = pair.getSecondElement();
            for (final TestRunSvtT0Shift t0Shift : this) {
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
     * @return the FPGA ID
     */
    @Field(names = {"fpga"})
    public Integer getFpgaID() {
        return this.getFieldValue("fpga");
    }

    /**
     * Get the hybrid ID.
     *
     * @return the hybrid ID
     */
    @Field(names = {"hybrid"})
    public Integer getHybridID() {
        return this.getFieldValue("hybrid");
    }
}
