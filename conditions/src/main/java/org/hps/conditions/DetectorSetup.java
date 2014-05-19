package org.hps.conditions;

import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

/** 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class DetectorSetup {

    // Default conditions manager parameters.
    static final String connectionResource = "/org/hps/conditions/config/conditions_database_testrun_2012_connection.properties";
    static final String conditionsConfig = "/org/hps/conditions/config/conditions_database_testrun_2012.xml";

    String detectorName;
    int runNumber;
    DatabaseConditionsManager conditionsManager;
    boolean wasConfigured = false;

    public DetectorSetup(String detectorName, int runNumber) {
        this.detectorName = detectorName;
        this.runNumber = runNumber;
    }
        
    /**
     * Configure and register the {@link DatabaseConditionsManager} with default
     * parameters.
     * @return an instance of this class for chaining (e.g. to call {@link #setup()}.
     */
    public DetectorSetup configure() {
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