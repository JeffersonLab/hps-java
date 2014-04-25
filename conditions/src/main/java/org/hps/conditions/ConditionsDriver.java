package org.hps.conditions;

import static org.hps.conditions.TableConstants.ECAL_CONDITIONS;
import static org.hps.conditions.TableConstants.SVT_CONDITIONS;

import org.hps.conditions.ecal.EcalConditions;
import org.hps.conditions.ecal.EcalConditionsLoader;
import org.hps.conditions.svt.SvtConditions;
import org.hps.conditions.svt.SvtConditionsLoader;
import org.lcsim.conditions.ConditionsReader;
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
    String _defaultConfigResource = "/org/hps/conditions/config/conditions_database_testrun_2012.xml";

    // Default database connection parameters, which points to the SLAC development
    // database.
    static String _defaultConnectionResource = "/org/hps/conditions/config/conditions_database_testrun_2012_connection.properties";

    String ecalSubdetectorName = "Ecal";
    String svtSubdetectorName = "Tracker";

    /**
     * Constructor which initializes the conditions manager with default connection
     * parameters and configuration.
     */
    public ConditionsDriver() {
        manager = new DatabaseConditionsManager();
        manager.setConnectionResource(_defaultConnectionResource);
        manager.configure(_defaultConfigResource);
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

    /**
     * Set the class of the conditions reader to use.
     */
    public void setConditionsReaderClass(String className) {
        try {
            Object object = Class.forName(className).newInstance();
            ConditionsReader reader = (ConditionsReader) object;
            if (reader != null)
                manager.setBaseConditionsReader(reader);
            else
                throw new IllegalArgumentException("The class " + className + " is not a ConditionsReader.");
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
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
        // Load conditions onto the detector.
        loadSvtConditions(detector);
        loadEcalConditions(detector);
    }

    /**
     * Load the SVT conditions onto the <code>Detector</code>.
     * @param detector The detector to update.
     */
    private void loadSvtConditions(Detector detector) {
        SvtConditions conditions = manager.getCachedConditions(SvtConditions.class, SVT_CONDITIONS).getCachedData();
        SvtConditionsLoader loader = new SvtConditionsLoader();
        loader.load(detector, conditions);
    }

    /**
     * Load the ECal conditions onto the <code>Detector</code>.
     * @param detector The detector to update.
     */
    private void loadEcalConditions(Detector detector) {
        EcalConditions conditions = manager.getCachedConditions(EcalConditions.class, ECAL_CONDITIONS).getCachedData();
        EcalConditionsLoader loader = new EcalConditionsLoader();
        loader.load(detector.getSubdetector(ecalSubdetectorName), conditions);
    }

    public void endOfData() {
        manager.closeConnection();
    }
}
