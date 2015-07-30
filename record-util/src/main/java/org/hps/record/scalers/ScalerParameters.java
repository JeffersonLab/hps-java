package org.hps.record.scalers;

import org.hps.record.epics.EpicsData;
import org.lcsim.event.EventHeader;

/**
 * Representation of scaler values from the float parameters in an lcsim event header.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class ScalerParameters {

    /**
     * Name of the float parameters in the map.
     */
    public static final String NAME = "SCALER_PARAMETERS";

    /**
     * Conversion from scaler FCUP threshold values to nano Coulomb.
     */
    private static float NANO_COULOMB = 905.937f;

    /**
     * Size of the data array.
     */
    private static int SIZE = 5;

    /**
     * Read scaler parameters from an lcsim event.
     *
     * @param event the lcsim event
     * @return the scaler parameters
     */
    public static ScalerParameters read(final EventHeader event) {
        if (event.getFloatParameters().get(NAME) == null) {
            throw new IllegalArgumentException("Event is missing scaler parameters array.");
        }
        return new ScalerParameters(event.getFloatParameters().get(NAME));
    }

    /**
     * The float parameter values.
     */
    private float[] values = new float[SIZE];

    /**
     * Public class constructor.
     */
    public ScalerParameters() {
    }

    /**
     * Create parameters from float array.
     *
     * @param values the float array
     */
    ScalerParameters(final float[] values) {
        this.values = values;
    }

    /**
     * Get a scaler parameter value.
     *
     * @param scalerParametersIndex the parameter index
     * @return the scaler parameter value
     */
    public float getValue(final ScalerParametersIndex scalerParametersIndex) {
        return this.values[scalerParametersIndex.ordinal()];
    }

    /**
     * Read scaler information from {@link org.hps.record.epics.EpicsData}.
     *
     * @param epicsData the <code>EpicsData</code> object
     */
    public void readEpicsData(final EpicsData epicsData) {
        if (epicsData.hasKey("hallb_IPM2H02_XPOS")) {
            this.setValue(ScalerParametersIndex.BEAM_POS_X, (float) (double) epicsData.getValue("hallb_IPM2H02_XPOS"));
        }
        if (epicsData.hasKey("hallb_IPM2H02_YPOS")) {
            this.setValue(ScalerParametersIndex.BEAM_POS_Y, (float) (double) epicsData.getValue("hallb_IPM2H02_YPOS"));
        }
        if (epicsData.hasKey("SVT:bias:top:0:v_sens")) {
            this.setValue(ScalerParametersIndex.SVT_BIAS_VOLTAGE,
                    (float) (double) epicsData.getValue("SVT:bias:top:0:v_sens"));
        }
    }

    /**
     * Read scaler information from a {@link ScalerData} object.
     *
     * @param scalerData the <code>ScalerData</code> object
     */
    public void readScalerData(final ScalerData scalerData) {
        this.setValue(ScalerParametersIndex.FCUP_TDC_GATED, scalerData.getValue(ScalerDataIndex.FCUP_TDC_GATED)
                / NANO_COULOMB);
        this.setValue(ScalerParametersIndex.FCUP_TDC_UNGATED, scalerData.getValue(ScalerDataIndex.FCUP_TDC_UNGATED)
                / NANO_COULOMB);
    }

    /**
     * Set a parameter value by its index.
     *
     * @param scalerParametersIndex the parameter index
     * @param value the new value
     */
    void setValue(final ScalerParametersIndex scalerParametersIndex, final float value) {
        this.values[scalerParametersIndex.ordinal()] = value;
    }

    /**
     * Write out scaler parameters to the event header.
     *
     * @param event the lcsim event
     */
    public void write(final EventHeader event) {
        event.getFloatParameters().put(NAME, values);
    }
    
    /**
     * Convert to string.
     * 
     * @return this object converted to a string
     */
    public String toString() {
        StringBuffer buff = new StringBuffer();
        for (ScalerParametersIndex index : ScalerParametersIndex.values()) {
            buff.append(index.name() + " " + this.getValue(index));
            buff.append('\n');
        }
        return buff.toString();
    }
}
