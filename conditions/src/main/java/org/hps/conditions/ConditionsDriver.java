package org.hps.conditions;

import static org.hps.conditions.TableConstants.SVT_CONDITIONS;

import java.io.File;

import org.hps.conditions.svt.SvtConditions;
import org.hps.conditions.svt.SvtDetectorSetup;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.geometry.Detector;
/**
 * This {@link org.lcsim.util.Driver} is a subclass of
 * {@link AbstractConditionsDriver} and specifies the database connection
 * parameters and configuration for the development database.
 *
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ConditionsDriver extends AbstractConditionsDriver {

    // Default conditions system XML config, which is for the Test Run 2012 database.
    static final String DB_CONFIG = "/org/hps/conditions/config/conditions_dev.xml";

    // Default database connection parameters, which points to the SLAC development database.
    static final String DB_CONNECTION = "/org/hps/conditions/config/conditions_dev.properties";
    
    File connectionFile;

    public ConditionsDriver() {
        if (ConditionsManager.defaultInstance() instanceof DatabaseConditionsManager) {
        	getLogger().config("ConditionsDriver found existing DatabaseConditionsManager.");
            manager = (DatabaseConditionsManager) ConditionsManager.defaultInstance();
        } else {
            manager = new DatabaseConditionsManager();
            manager.setConnectionResource(DB_CONNECTION);
            manager.configure(DB_CONFIG);
            manager.register();
        }
    }
   
    public void setConnectionFile(String connectionFile) {
    	getLogger().config("set connectionFile to " + connectionFile);
    	this.connectionFile = new File(connectionFile);
    	manager.setConnectionProperties(this.connectionFile);
    }
    
    public void setConnectionResource(String connectionResource) {
    	getLogger().config("set connection resource to " + connectionResource);
    	manager.setConnectionResource(connectionResource);
    }
   
    /**
     * Load the {@link SvtConditions} set onto <code>HpsSiSensor</code>.
     * @param detector The detector to update.
     */
    @Override
    protected void loadSvtConditions(Detector detector) {
        SvtConditions conditions = manager.getCachedConditions(SvtConditions.class, SVT_CONDITIONS).getCachedData();
        SvtDetectorSetup loader = new SvtDetectorSetup();
        loader.load(detector.getSubdetector(svtSubdetectorName), conditions);
    }
}
