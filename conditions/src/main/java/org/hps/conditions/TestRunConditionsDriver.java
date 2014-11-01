package org.hps.conditions;

/**
 * This {@link org.lcsim.util.Driver} is a subclass of {@link AbstractConditionsDriver}
 * and specifies the database connection parameters and configuration for the
 * test run database. 
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class TestRunConditionsDriver extends AbstractConditionsDriver {

	// Default conditions system XML config, which is for the Test Run 2012 database.
    static final String TEST_RUN_CONFIG = "/org/hps/conditions/config/conditions_database_testrun_2012.xml";

    // Default database connection parameters, which points to the SLAC development database.
    static final String TEST_RUN_CONNECTION = "/org/hps/conditions/config/conditions_dev.properties";

    // Default constructor used to setup the database connection
    public TestRunConditionsDriver(){
        manager = new DatabaseConditionsManager();
        manager.setConnectionResource(TEST_RUN_CONNECTION);
        manager.configure(TEST_RUN_CONFIG);
        manager.register();
    }
}
