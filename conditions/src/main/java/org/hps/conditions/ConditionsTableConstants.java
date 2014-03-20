package org.hps.conditions;

/**
 * This is a static set of constants defining default table names and lookup key values for conditions data.
 * The actual table names for the conditions data are defined externally in an XML configuration file read by the 
 * {@link DatabaseConditionsManager}.
 */
public final class ConditionsTableConstants {
    
    /** Prevent instantiation of this class, which is only a holder of static constant values. */
    private ConditionsTableConstants() {}
    
    /** Conditions key for ConditionsRecord. */
    public static final String CONDITIONS_RECORD = "conditions_record";

    /** Conditions key for combined ECal conditions. */
    public static final String ECAL_CONDITIONS = "ecal_conditions";
    
    /** Table with ECal channel data. */
    public static final String ECAL_CHANNELS = "ecal_channels";

    /** Conditions key for ECal gain data. */
    public static final String ECAL_GAINS = "ecal_gains";
    
    /** Conditions key for ECal bad channel set. */
    public static final String ECAL_BAD_CHANNELS = "ecal_bad_channels";
    
    /** Conditions key for ECal calibration information. */
    public static final String ECAL_CALIBRATIONS = "ecal_calibrations";
    
    /** Conditions key for combined ECal conditions. */
    public static final String SVT_CONDITIONS = "svt_conditions";
    
    /** Table with SVT channel data. */
    public static final String SVT_CHANNELS = "svt_channels";
    
    /** Table with the SVT DAQ map. */
    public static final String SVT_DAQ_MAP = "svt_daq_map";
            
    /** Conditions key for SVT calibration data. */ 
    public static final String SVT_CALIBRATIONS = "svt_calibrations";
    
    /** Conditions key for SVT bad channels. */
    public static final String SVT_BAD_CHANNELS = "svt_bad_channels";
    
    /** Conditions key for SVT pulse parameters. */
    public static final String SVT_PULSE_PARAMETERS = "svt_pulse_parameters";
    
    /** Conditions key for SVT gain data. */
    public static final String SVT_GAINS = "svt_gains";
        
    /** Conditions key for SVT time shifts by sensor. */
    public static final String SVT_TIME_SHIFTS = "svt_time_shifts";
    
    /** Conditions key for integrated beam current. */
    public static final String BEAM_CURRENT = "beam_current";
}
