package org.hps.conditions.svt;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;

/**
 * Conditions object for SVT timing configuration constants, including offset phase and time (in nanoseconds).
 * <p>
 * There will generally be only one of these records per run.
 *
 * @author Jeremy McCormick
 */
@Table(names = {"svt_timing_constants"})
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_CREATED)
public final class SvtTimingConstants extends BaseConditionsObject {

    /**
     * The collection implementation for {@link SvtTimingConstants}.
     */
    @SuppressWarnings("serial")
    public static class SvtTimingConstantsCollection extends BaseConditionsObjectCollection<SvtTimingConstants> {

        /**
         * Find timing constants by offset phase and time.
         *
         * @param offsetPhase the offset phase
         * @param offsetTime the offset time
         * @return the constants that match the params or <code>null</code> if not found
         */
        public SvtTimingConstants find(final int offsetPhase, final double offsetTime) {
            for (final SvtTimingConstants constants : this) {
                if (constants.getOffsetPhase().equals(offsetPhase) && constants.getOffsetTime().equals(offsetTime)) {
                    return constants;
                }
            }
            return null;
        }
    }

    /**
     * The SVT offset phase (ns).
     *
     * @return the SVT offset phase
     */
    @Field(names = {"offset_phase"})
    public Integer getOffsetPhase() {
        return this.getFieldValue("offset_phase");
    }

    /**
     * The SVT offset time (ns).
     *
     * @return the SVT offset time (ns)
     */
    @Field(names = {"offset_time"})
    public Double getOffsetTime() {
        return this.getFieldValue("offset_time");
    }
}
