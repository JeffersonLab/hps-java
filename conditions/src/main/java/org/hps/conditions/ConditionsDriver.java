package org.hps.conditions;

import static org.hps.conditions.TableConstants.SVT_CONDITIONS;

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

    public ConditionsDriver() {
        if (ConditionsManager.defaultInstance() instanceof DatabaseConditionsManager) {
        	getLogger().config("ConditionsDriver found existing DatabaseConditionsManager.");
            manager = (DatabaseConditionsManager) ConditionsManager.defaultInstance();
        } else {
            manager = new DatabaseConditionsManager();
        }
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
