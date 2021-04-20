package org.hps.conditions.svt;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;

/**
 * This class represents the svt readout phases and crostalk time windows
 */
@Table(names = {"svt_readout_sync_phases"})
public final class SvtReadoutSyncPhase extends BaseConditionsObject {

    /**
     * Collection implementation for {@link SvtReadoutSyncPhase} objects.
     */
    @SuppressWarnings("serial")
    public static class SvtReadoutSyncPhaseCollection extends BaseConditionsObjectCollection<SvtReadoutSyncPhase> {
    }

    /**
     * Get the channel ID.
     *
     * @return The channel ID.
     */
    @Field(names = {"trigDel"})
    public Integer getTrigDel() {
        return this.getFieldValue(Integer.class, "trigDel");
    }

    /**
     * Get phase0.
     *
     * @return The phase0 value.
     */
    @Field(names = {"phase0"})
    public Integer getPhase0() {
        return this.getFieldValue(Integer.class, "phase0");
    }

    /**
     * Get phase1.
     *
     * @return The phase1 value.
     */
    @Field(names = {"phase1"})
    public Integer getPhase1() {
        return this.getFieldValue(Integer.class, "phase1");
    }

    /**
     * Get cut0L.
     *
     * @return The cut0L value.
     */
    @Field(names = {"cut0L"})
    public Integer getCut0L() {
        return this.getFieldValue(Integer.class, "cut0L");
    }

    /**
     * Get cut0H.
     *
     * @return The cut0H value.
     */
    @Field(names = {"cut0H"})
    public Integer getCut0H() {
        return this.getFieldValue(Integer.class, "cut0H");
    }

    /**
     * Get cut1L.
     *
     * @return The cut0L value.
     */
    @Field(names = {"cut1L"})
    public Integer getCut1L() {
        return this.getFieldValue(Integer.class, "cut1L");
    }

    /**
     * Get cut1H.
     *
     * @return The cut1H value.
     */
    @Field(names = {"cut1H"})
    public Integer getCut1H() {
        return this.getFieldValue(Integer.class, "cut1H");
    }
}
