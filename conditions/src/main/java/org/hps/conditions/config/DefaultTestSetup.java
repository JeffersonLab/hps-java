package org.hps.conditions.config;

import org.hps.conditions.DatabaseConditionsManager;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

/**
 * <p>
 * This is a static utility class for setting up the conditions system for test cases in
 * this package and sub-packages.
 * </p>
 * <p>
 * It uses the SLAC Test Run 2012 conditions database, with a relative reference to a file
 * containing connection parameters in the hps-conditions module. The XML configuration is
 * read from a classpath resource in the same module.
 * </p>
 * <p>
 * The detector is set to <i>HPS-conditions-test</i>, which is a test detector without
 * real data associated to it. There are a few files used in the test cases that use this
 * detector.
 * </p>
 * <p>
 * The run number is initially set to <i>1351</i> which is one of the "good runs".
 * </p>
 * <p>
 * Full setup can be performed with this method chain: <code>
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

    // Default conditions manager parameters.
    static String connectionResource = "/org/hps/conditions/config/conditions_database_testrun_2012_connection.properties";
    static String conditionsConfig = "/org/hps/conditions/config/conditions_database_testrun_2012.xml";

    // Default test detector and run number for test cases not using real data.
    static String detectorName = "HPS-conditions-test";
    static int runNumber = 1351;

    DatabaseConditionsManager conditionsManager;
    boolean wasConfigured = false;

    /**
     * Configure and register the {@link DatabaseConditionsManager} with default
     * parameters.
     * @return an instance of this class for chaining (e.g. to call {@link #setup()}.
     */
    public DefaultTestSetup configure() {
        conditionsManager = new DatabaseConditionsManager();
        conditionsManager.setConnectionResource(connectionResource);
        conditionsManager.configure(conditionsConfig);
        conditionsManager.register();
        wasConfigured = true;
        return this;
    }

    /**
     * Setup the detector and run number conditions for the conditions manager. This is
     * mostly useful for test cases not using an <code>LCSimLoop</code>.
     * @return the conditions manager
     */
    public DatabaseConditionsManager setup() {
        if (!wasConfigured)
            configure();
        try {
            conditionsManager.setDetector(detectorName, runNumber);
        } catch (ConditionsNotFoundException e) {
            throw new RuntimeException(e);
        }
        return conditionsManager;
    }
}