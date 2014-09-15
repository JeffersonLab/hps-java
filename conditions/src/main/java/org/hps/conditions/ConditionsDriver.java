package org.hps.conditions;

import static org.hps.conditions.TableConstants.ECAL_CONDITIONS;
import static org.hps.conditions.TableConstants.SVT_CONDITIONS;

import org.hps.conditions.ecal.EcalConditions;
import org.hps.conditions.ecal.EcalDetectorSetup;
import org.hps.conditions.svt.SvtConditions;
import org.hps.conditions.svt.SvtDetectorSetup;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 * This {@link org.lcsim.util.Driver} sets up the {@link DatabaseConditionsManager} and
 * loads the conditions onto a detector.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class ConditionsDriver extends Driver {

    // Static instance of the manager.
    static DatabaseConditionsManager manager;

    // Default conditions system XML config, which is for the Test Run 2012 database.
    static final String DEFAULT_CONFIG = "/org/hps/conditions/config/conditions_database_testrun_2012.xml";

    // Default database connection parameters, which points to the SLAC development database.
    static final String DEFAULT_CONNECTION = "/org/hps/conditions/config/conditions_database_testrun_2012_connection.properties";

    String ecalSubdetectorName = "Ecal";
    String svtSubdetectorName = "Tracker";
    
    boolean loadSvtConditions = true;
    boolean loadEcalConditions = true;

    /**
     * Constructor which initializes the conditions manager with default connection
     * parameters and configuration.
     */
    public ConditionsDriver() {
        manager = new DatabaseConditionsManager();
        manager.setConnectionResource(DEFAULT_CONNECTION);
        manager.configure(DEFAULT_CONFIG);
        manager.register();
    }

    /**
     * Set the configuration XML resource to be used by the manager.
     * @param resource the configuration resource
     */
    public void setConfigResource(String resource) {
        manager.configure(resource);
    }

    /**
     * Set the connection properties file resource to be used by the manager.
     * @param resource the connection resource
     */
    public void setConnectionResource(String resource) {
        manager.setConnectionResource(resource);
    }
    
    public void setLoadSvtConditions(boolean loadSvtConditions) {
        this.loadSvtConditions = loadSvtConditions;
    }
    
    public void setLoadEcalConditions(boolean loadEcaltConditions) {
        this.loadEcalConditions = loadSvtConditions;
    }
    
    public void setEcalSubdetectorName(String ecalSubdetectorName) {
        this.ecalSubdetectorName = ecalSubdetectorName;
    }

    public void setSvtSubdetectorName(String svtSubdetectorName) {
        this.svtSubdetectorName = svtSubdetectorName;
    }
       
    /**
     * This method updates a new detector with SVT and ECal conditions data.
     */
    public void detectorChanged(Detector detector) {
        // Load SVT conditions onto the detector.
        if (loadSvtConditions)
            loadSvtConditions(detector);
        // Load ECAL conditions onto the detector.
        if (loadEcalConditions)
            loadEcalConditions(detector);
    }

    /**
     * Load the SVT conditions onto the <code>Detector</code>.
     * @param detector The detector to update.
     */
    private void loadSvtConditions(Detector detector) {
        SvtConditions conditions = manager.getCachedConditions(SvtConditions.class, SVT_CONDITIONS).getCachedData();
        SvtDetectorSetup loader = new SvtDetectorSetup();
        loader.load(detector, conditions);
    }

    /**
     * Load the ECal conditions onto the <code>Detector</code>.
     * @param detector The detector to update.
     */
    private void loadEcalConditions(Detector detector) {
        EcalConditions conditions = manager.getCachedConditions(EcalConditions.class, ECAL_CONDITIONS).getCachedData();
        EcalDetectorSetup loader = new EcalDetectorSetup();
        loader.load(detector.getSubdetector(ecalSubdetectorName), conditions);
    }

    public void endOfData() {
        manager.closeConnection();
    }
}
