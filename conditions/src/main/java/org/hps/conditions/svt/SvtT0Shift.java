package org.hps.conditions.svt;

import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;
import org.hps.util.Pair;

/**
 * This class is a data holder for associating a t0 time shift with a specific sensor by DAQ pair (FEB ID and FEB hybrid
 * ID).
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 * @author <a href="mailto:omoreno1@ucsc.edu">Omar Moreno</a>
 */
@Table(names = {"svt_t0_shifts"})
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED)
public final class SvtT0Shift extends AbstractSvtT0Shift {

    /**
     * Concrete collection implementation for {@link SvtT0Shift}.
     */
    @SuppressWarnings("serial")
    public static class SvtT0ShiftCollection extends AbstractSvtT0Shift.AbstractSvtT0ShiftCollection<SvtT0Shift> {

        /**
         * Get the {@link SvtT0Shift} associated with a given DAQ pair.
         *
         * @param DAQ pair for a given sensor
         * @return the {@link SvtT0Shift} associated with the DAQ pair or null if does not exist
         */
        @Override
        public SvtT0Shift getT0Shift(final Pair<Integer, Integer> pair) {
            final int febID = pair.getFirstElement();
            final int febHybridID = pair.getSecondElement();
            for (final SvtT0Shift t0Shift : this) {
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
     * @return the FEB ID
     */
    @Field(names = {"feb_id"})
    public int getFebID() {
        return getFieldValue("feb_id");
    }

    /**
     * Get the FEB hybrid ID.
     *
     * @return the FEB hybrid ID
     */
    @Field(names = {"feb_hybrid_id"})
    public int getFebHybridID() {
        return getFieldValue("feb_hybrid_id");
    }
}
