package org.hps.conditions;

import static org.hps.conditions.database.TableConstants.ECAL_CONDITIONS;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.conditions.ecal.EcalDetectorSetup;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 * This abstract {@link org.lcsim.util.Driver} contains the general methods used
 * to set up {@link DatabaseConditionsManager} and load the conditions onto a
 * detector.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public abstract class AbstractConditionsDriver extends Driver {

    // Static instance of the manager.
    static DatabaseConditionsManager manager;

    private String ecalSubdetectorName = "Ecal";
    protected String svtSubdetectorName = "Tracker";

    boolean loadSvtConditions = true;
    boolean loadEcalConditions = true;
    
    protected AbstractConditionsDriver() {
        ConditionsManager currentManager = ConditionsManager.defaultInstance();
        if (currentManager instanceof DatabaseConditionsManager) {
            manager = DatabaseConditionsManager.getInstance();
        } else {        
            manager = new DatabaseConditionsManager();
        }        
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
    protected abstract void loadSvtConditions(Detector detector);

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