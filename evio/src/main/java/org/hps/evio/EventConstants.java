package org.hps.evio;


public final class EventConstants {

    //event type tag    
    public static final int EVENT_BANK_NUM = 0xCC;
    
    // These correspond to ROC (readout crate) IDs from the DAQ.
    public static final int ECAL_TOP_BANK_TAG = 0x1;
    public static final int ECAL_BOTTOM_BANK_TAG = 0x2;
    public static final int SVT_BANK_TAG = 0x3;
    public static final int TEST_RUN_SVT_BANK_TAG = 0x3;
    
    // These values are put into the number field of the banks.
    public static final int SVT_BANK_NUMBER = 1;
    public static final int ECAL_BANK_NUMBER = 1;
    public static final int TRIGGER_BANK_NUMBER = 1;
    public static final int ECAL_RAW_MODE = 1;
    public static final int ECAL_PULSE_MODE = 2;
    public static final int ECAL_PULSE_INTEGRAL_MODE = 3; //FADC mode 3
    public static final int ECAL_PULSE_INTEGRAL_HIGHRESTDC_MODE = 4; //FADC mode 7

    public static final int HODO_RAW_MODE = 1;
    public static final int HODO_PULSE_MODE = 2;
    public static final int HODO_PULSE_INTEGRAL_MODE = 3; //FADC mode 3
    public static final int HODO_PULSE_INTEGRAL_HIGHRESTDC_MODE = 4; //FADC mode 7

    
    // The composite data format for window ecal data. MODE 1
    public static final String ECAL_WINDOW_FORMAT = "c,i,l,N(c,Ns)";
    
    // The composite data format for pulse ecal data.
    public static final String ECAL_PULSE_FORMAT = "c,i,l,N(c,N(c,Ns))";
    
    // The composite data format for pulse integral ecal data. MODE 3
    public static final String ECAL_PULSE_INTEGRAL_FORMAT = "c,i,l,N(c,N(s,i))";
    
    // The composite data format for pulse integral ecal data. MODE 7
    public static final String ECAL_PULSE_INTEGRAL_HIGHRESTDC_FORMAT = "c,i,l,N(c,N(s,i,s,s))";
    
    // The tag for ECal window data.
    public static final int ECAL_WINDOW_BANK_TAG = 0xe101;

    // The tag for FADC Mode1 data.
    // This is more generic name, will be good if later, ECal also will use this name instead of the "ECAL_WINDOW_BANK_TAG"
    public static final int FADC_MODE1_BANK_TAG = 0xe101; 
    
    // The tag for ECal pulse data.
    public static final int ECAL_PULSE_BANK_TAG = 0xe10F; 

    // The tag for FADC pulse data.
    public static final int FADC_PULSE_BANK_TAG = 0xe10F; 
    
    // The tag for ECal pulse integral data mode 3
    public static final int ECAL_PULSE_INTEGRAL_BANK_TAG = 0xe103;

    // The tag for FADC pulse integral data mode 3
    public static final int FADC_PULSE_INTEGRAL_BANK_TAG = 0xe103;
    
    // The tag for ECal pulse integral data mode 7
    public static final int ECAL_PULSE_INTEGRAL_HIGHRESTDC_BANK_TAG = 0xe102;

    // The tag for ECal pulse integral data mode 7
    public static final int FADC_PULSE_INTEGRAL_HIGHRESTDC_BANK_TAG = 0xe102;
    
    // The tag for trigger data.
    public static final int TRIGGER_BANK_TAG = 0xe106;
    public static final int SVT_TOTAL_NUMBER_FPGAS = 8;
    public static final int MC_TIME = 2019686400; //Unix time (in seconds) used for Monte Carlo data 
}
