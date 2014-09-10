package org.hps.conditions.config;

/**
 * <p>
 * This is a static utility class for setting up the conditions system for test cases in
 * this package and sub-packages.
 * <p>
 * It uses the SLAC Test Run 2012 conditions database, with a relative reference to a file
 * containing connection parameters in the hps-conditions module. The XML configuration is
 * read from a classpath resource in the same module.
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
    private static final String prop = "/org/hps/conditions/config/conditions_database_testrun_2012_connection.properties";
   
    public TestRunReadOnlyConfiguration(boolean setup) {
        super(config, prop);
        if (setup) {
            setup();            
            load(detectorName, runNumber);
        }
    }               
}