package org.hps.conditions;

import org.lcsim.conditions.ConditionsManager;
import org.lcsim.geometry.Detector;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.conditions.ecal.EcalConditionsLoader;
import org.hps.conditions.svt.SvtConditions;
import org.hps.conditions.svt.SvtConditionsLoader;
import org.lcsim.util.Driver;

import static org.hps.conditions.TableConstants.SVT_CONDITIONS;
import static org.hps.conditions.TableConstants.ECAL_CONDITIONS;

/**
 * This {@link org.lcsim.util.Driver} loads conditions onto an HPS detector.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ConditionsDriver extends Driver {
    
    // Static instance of the manager.
    static DatabaseConditionsManager _manager;
    
    // Default conditions system XML config, which is for the Test Run 2012 database.
    String _defaultConfigResource = 
            "/org/hps/conditions/config/conditions_database_testrun_2012.xml";    
    
    // Default database connection parameters, which points to the SLAC development database.
    static String _defaultConnectionResource = 
            "/org/hps/conditions/config/conditions_database_testrun_2012_connection.properties";
     
    /**
     * Constructor which initializes the conditions manager
     * without performing any configuration.  This can be 
     * done using the {@link #setConfigResource(String)} and
     * {@link #setConnectionResource(String)} methods.
     */
    public ConditionsDriver() {
        _manager = new DatabaseConditionsManager();
        _manager.register();
    }
    
    /**
     * Set the configuration resource to be used by the manager and update it.
     * @param resource the configuration resource
     */
    public void setConfigResource(String resource) {
        _manager.configure(resource);
    }
    
    /**
     * Set the connection resource to be used by the manager and update it.
     * @param resource the connection resource
     */
    public void setConnectionResource(String resource) {
        _manager.setConnectionResource(resource);
    }
    
    /**
     * Hook for start of data, which will initialize the conditions
     * system if it has not been properly setup yet.     
     */
    // FIXME: Is this the best place for this to happen?  Seems kind of kludgy.
    public void startOfData() {
        if (!_manager.hasConnectionParameters()) {
            _manager.setConnectionResource(_defaultConnectionResource);
        }
        
        if (!_manager.wasConfigured()) {
            _manager.configure(_defaultConfigResource);
        }
    }
        
    /**
     * This method updates a new detector with SVT and ECal conditions data.
     */
    public void detectorChanged(Detector detector) {
        loadSvtConditions(detector);       
        loadEcalConditions(detector);
    }
        
    /**
     * Load the SVT conditions onto the <code>Detector</code>.
     * @param detector The detector to update.
     */
    private void loadSvtConditions(Detector detector) {        
        ConditionsManager manager = ConditionsManager.defaultInstance();        
        SvtConditions conditions = manager.getCachedConditions(SvtConditions.class, SVT_CONDITIONS).getCachedData();                   
        SvtConditionsLoader loader = new SvtConditionsLoader();
        loader.load(detector, conditions);
    }    
    
    /** 
     * Load the ECal conditions onto the <code>Detector</code>.
     * @param detector The detector to update.
     */ 
    private void loadEcalConditions(Detector detector) {
        ConditionsManager manager = ConditionsManager.defaultInstance();
        EcalConditions conditions = manager.getCachedConditions(EcalConditions.class, ECAL_CONDITIONS).getCachedData();
        EcalConditionsLoader loader = new EcalConditionsLoader();
        loader.load(detector, conditions);
    }
}
