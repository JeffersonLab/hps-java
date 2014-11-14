package org.hps.conditions;

import static org.hps.conditions.TableConstants.SVT_CONDITIONS;

import org.hps.conditions.svt.TestRunSvtConditions;
import org.hps.conditions.svt.TestRunSvtDetectorSetup;
import org.lcsim.geometry.Detector;

/**
 * This {@link org.lcsim.util.Driver} is a subclass of
 * {@link AbstractConditionsDriver} and specifies the XML configuration 
 * for the Test Run database.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class TestRunConditionsDriver extends AbstractConditionsDriver {

    
    public TestRunConditionsDriver() {       
        super();
        
        // Override the default configuration with one specific to Test Run conditions.
        manager.setXmlConfig("/org/hps/conditions/config/conditions_database_testrun_2012.xml");
    }

    /**
     * Load the {@link TestRunSvtConditions} set onto <code>HpsTestRunSiSensor</code>.
     * @param detector The detector to update.
     */
    @Override
    protected void loadSvtConditions(Detector detector) {
        TestRunSvtConditions conditions = manager.getCachedConditions(TestRunSvtConditions.class, SVT_CONDITIONS).getCachedData();
        TestRunSvtDetectorSetup loader = new TestRunSvtDetectorSetup();
        loader.load(detector.getSubdetector(svtSubdetectorName), conditions);
    }
}
