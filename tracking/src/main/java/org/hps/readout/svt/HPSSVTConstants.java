package org.hps.readout.svt;

public class HPSSVTConstants {

    public static final int SVT_TOTAL_FPGAS = 7;
    public static final int TOTAL_APV25_PER_HYBRID = 5;
    public static final int TOTAL_APV25_CHANNELS = 128;
    public static final int TOTAL_HYBRIDS_PER_FPGA = 3;
    public static final int TOTAL_TEMPS_PER_HYBRID = 4;

    // The Rear Transition Module gain
    public static final double RTM_GAIN = 1.5;

    // Total number of strips per sensor
    public static final int TOTAL_STRIPS_PER_SENSOR = 639;

    // Total number of shaper signal samples obtained
    public static final int TOTAL_NUMBER_OF_SAMPLES = 6;

    // MASKs used to encode and decode the SVT data
    public static final int TEMP_MASK = 0xFFFF;
    public static final int HYBRID_MASK = 0x3;
    public static final int APV_MASK = 0x7;
    public static final int CHANNEL_MASK = 0x7F;
    public static final int FPGA_MASK = 0xFFFF;
    public static final int SAMPLE_MASK = 0x3FFF;

    // Temperature constants
    public static final double MIN_TEMP = -50;
    public static final double MAX_TEMP = 150;
    public static final int ADC_TEMP_COUNT = 4096;
    public static final double TEMP_K0 = 273.15;
    public static final double CONST_A = 0.03448533;
    public static final double BETA = 3750.;
    public static final double V_MAX = 2.5;
    public static final double V_REF = 2.5;
    public static final double R_DIV = 10000.;
    public static final double TEMP_INC = 0.01;

    // Length of the APV25 analog pipeline
    public static final int ANALOG_PIPELINE_LENGTH = 192;
    // Total number of channels an APV25 ASIC contains
    public static final int CHANNELS = 128;
    // Number of electron-hole pairs created by a min. ionizing particle
    // in 300 micrometers of Si
    public static final int MIP = 25000; // electrons
    // Time intervals at which an APV25 shaper signal is sampled at
    public static final double SAMPLING_INTERVAL = 24.0; // [ns]
    // The APV25 gain after multiplexing
    public static final double MULTIPLEXER_GAIN = 1;
    // The APV25 front end gain
    public static final double FRONT_END_GAIN = 100.0; //

}
