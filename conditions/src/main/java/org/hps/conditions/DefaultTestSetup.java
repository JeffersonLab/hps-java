package org.hps.conditions;

/**
 * <p>
 * This is a static utility class for setting up the conditions system for test cases
 * in this package and sub-packages.
 * </p>
 * <p>
 * It uses the SLAC Test Run 2012 conditions database, with a relative reference to a file
 * containing connection parameters in the hps-conditions module.  The XML configuration
 * is read from a classpath resource in the same module.
 * </p>
 * <p>
 * The detector is set to <i>HPS-conditions-test</i>, which is a test detector without real data
 * associated to it.  There are a few files used in the test cases that use this detector.
 * </p>
 * <p>
 * The run number is initially set to <i>1351</i> which is one of the "good runs".
 * </p>
 * <p>
 * Full setup can be performed with this method chain:
 * <code>
 * DatabaseConditionsManager manager = new DefaultTestSetup().configure().setup();
 * </code>
 * </p>
 * <p>
 * To only configure the system without setting up detector and run, use the following:
 * <code>
 * new DefaultTestSetup().configure();
 * </code>
 * </p>
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class DefaultTestSetup {

    static String _detectorName = "HPS-conditions-test";
    static int _runNumber = 1351;
    static String _connectionProperties = "./src/main/config/conditions_database_testrun_2012_connection.properties";
    static String _conditionsConfig = "/org/hps/conditions/config/conditions_database_testrun_2012.xml";
    
    DatabaseConditionsManager _conditionsManager;
    boolean _wasConfigured = false;
    
    public DefaultTestSetup configure() {        
        _conditionsManager = DatabaseConditionsManager.createInstance();
        _conditionsManager.setConnectionProperties(_connectionProperties);
        _conditionsManager.configure(_conditionsConfig);
        _wasConfigured = true;
        return this;
    }
    
    public DatabaseConditionsManager setup() {
        if (!_wasConfigured)
            configure();
        _conditionsManager.setDetectorName(_detectorName);
        _conditionsManager.setRunNumber(_runNumber);
        _conditionsManager.setup();
        return _conditionsManager;
    }
}
