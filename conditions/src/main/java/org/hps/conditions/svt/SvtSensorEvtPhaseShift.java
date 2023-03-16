package org.hps.conditions.svt;

import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;
import org.hps.util.Pair;

/**
 * This class is a data holder for associating a t0 time shift with a specific sensor and event phase by DAQ pair (FEB ID and FEB hybrid
 * ID).
 */
@Table(names = {"svt_t0_sensor_phase_shifts"})
public final class SvtSensorEvtPhaseShift extends AbstractSvtSensorEvtPhaseShift {

    /**
     * Concrete collection implementation for {@link SvtSensorEvtPhaseShift}.
     */
    @SuppressWarnings("serial")
    public static class SvtSensorEvtPhaseShiftCollection extends AbstractSvtSensorEvtPhaseShift.AbstractSvtSensorEvtPhaseShiftCollection<SvtSensorEvtPhaseShift> {

        /**
         * Get the {@link SvtSensorEvtPhaseShift} associated with a given DAQ pair.
         *
         * @param pair DAQ pair for a given sensor
         * @return the {@link SvtSensorEvtPhaseShift} associated with the DAQ pair or null if does not exist
         */
        @Override
        public SvtSensorEvtPhaseShift getT0PhaseShift(final Pair<Integer, Integer> pair) {
            final int febID = pair.getFirstElement();
            final int febHybridID = pair.getSecondElement();
            for (final SvtSensorEvtPhaseShift t0PhaseShift : this) {
                if (t0PhaseShift.getFebID() == febID && t0PhaseShift.getFebHybridID() == febHybridID) {
                    return t0PhaseShift;
                }
            }
            return null;
        }
    }

    /**
     * Get the FEB hybrid ID.
     *
     * @return the FEB hybrid ID
     */
    @Field(names = {"feb_hybrid_id"})
    public Integer getFebHybridID() {
        return this.getFieldValue("feb_hybrid_id");
    }

    /**
     * Get the FEB ID.
     *
     * @return the FEB ID
     */
    @Field(names = {"feb_id"})
    public Integer getFebID() {
        return this.getFieldValue("feb_id");
    }

    /**
     * Get the t0 shift for phase0
     *
     * @return the t0 shift for phase0
     */
    @Field(names = {"phase0_shift"})
    public final Double getPhase0Shift() {
        return this.getFieldValue("phase0_shift");
    }


    /**
     * Get the t0 shift for phase1
     *
     * @return the t0 shift for phase1
     */
    @Field(names = {"phase0_shift"})
    public final Double getPhase1Shift() {
        return this.getFieldValue("phase1_shift");
    }

    /**
     * Get the t0 shift for phase2
     *
     * @return the t0 shift for phase2
     */
    @Field(names = {"phase2_shift"})
    public final Double getPhase2Shift() {
        return this.getFieldValue("phase2_shift");
    }

    /**
     * Get the t0 shift for phase3
     *
     * @return the t0 shift for phase3
     */
    @Field(names = {"phase3_shift"})
    public final Double getPhase3Shift() {
        return this.getFieldValue("phase3_shift");
    }

    /**
     * Get the t0 shift for phase4
     *
     * @return the t0 shift for phase4
     */
    @Field(names = {"phase4_shift"})
    public final Double getPhase4Shift() {
        return this.getFieldValue("phase4_shift");
    }

    /**
     * Get the t0 shift for phase5
     *
     * @return the t0 shift for phase5
     */
    @Field(names = {"phase5_shift"})
    public final Double getPhase5Shift() {
        return this.getFieldValue("phase5_shift");
    }

    public final Double[] getPhaseShifts(){        
        Double shifts[]={this.getPhase0Shift(), 
                         this.getPhase1Shift(), 
                         this.getPhase2Shift(), 
                         this.getPhase3Shift(), 
                         this.getPhase4Shift(), 
                         this.getPhase5Shift()};
                  
        return shifts;
    }
}
