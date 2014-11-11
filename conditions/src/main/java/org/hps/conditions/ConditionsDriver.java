package org.hps.conditions;

import static org.hps.conditions.TableConstants.SVT_CONDITIONS;

import org.hps.conditions.svt.SvtConditions;
import org.hps.conditions.svt.SvtDetectorSetup;
import org.lcsim.geometry.Detector;

/**
 * This {@link org.lcsim.util.Driver} is a subclass of
 * {@link AbstractConditionsDriver} which creates the default
 * {@link DatabaseConditionsManager} for using database conditions
 * at runtime.
 *
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ConditionsDriver extends AbstractConditionsDriver {

    /**
     * Default constructor which uses super class constructor for initialization.
     */
    public ConditionsDriver() {
        super();
    }

    /**
     * Load the {@link SvtConditions} set onto the <code>HpsSiSensor</code> objects.
     * @param detector The detector to update.
     */
    @Override
    protected void loadSvtConditions(Detector detector) {
        SvtConditions conditions = manager.getCachedConditions(SvtConditions.class, SVT_CONDITIONS).getCachedData();
        SvtDetectorSetup loader = new SvtDetectorSetup();
        loader.load(detector.getSubdetector(svtSubdetectorName), conditions);
    }
}
