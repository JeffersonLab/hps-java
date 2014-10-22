package org.hps.conditions.config;

/**
 * <p>
 * This is a utility class for setting up the conditions system with Test Run 2012 data
 * for test cases in this package and sub-packages.
 * <p>
 * The detector is set to <i>HPS-conditions-test</i>, which is a test detector without
 * real data associated to it. There are a few files used in the test cases that use this
 * detector.
 * <p>
 * The run number is initially set to <i>1351</i> which is one of the "good runs".
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class TestRunReadOnlyConfiguration extends ResourceConfiguration {

    // Default test detector and run number for test cases not using real data.
    private static final String detectorName = "HPS-conditions-test";
    private static final int runNumber = 1351;
    
    private static final String config = "/org/hps/conditions/config/conditions_database_testrun_2012.xml";
    private static final String prop = "/org/hps/conditions/config/conditions_dev.properties";
   
    public TestRunReadOnlyConfiguration() {
        super(config, prop);
    }
    
    /**
     * Class constructor.
     * @param setup True to setup the conditions manager and the detector.  
     */
    public TestRunReadOnlyConfiguration(boolean setup) {
        super(config, prop);
        if (setup) {
            setup();            
            load(detectorName, runNumber);
        }
    }               
}