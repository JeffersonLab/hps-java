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
    
    /**
     * This method updates a new detector with SVT and ECal conditions data.
     */
    public void detectorChanged(Detector detector) {
        loadSvtConditions(detector);       
        loadEcalConditions(detector);
    }
    
    /**
     * Load the SVT conditions onto the Detector.
     * @param detector The detector to update.
     */
    private void loadSvtConditions(Detector detector) {        
        ConditionsManager manager = ConditionsManager.defaultInstance();        
        SvtConditions conditions = manager.getCachedConditions(SvtConditions.class, SVT_CONDITIONS).getCachedData();                   
        SvtConditionsLoader loader = new SvtConditionsLoader();
        loader.load(detector, conditions);
    }    
    
    /** 
     * Load the ECal conditions onto the Detector.
     * @param detector The detector to update.
     */ 
    private void loadEcalConditions(Detector detector) {
        ConditionsManager manager = ConditionsManager.defaultInstance();
        EcalConditions conditions = manager.getCachedConditions(EcalConditions.class, ECAL_CONDITIONS).getCachedData();
        EcalConditionsLoader loader = new EcalConditionsLoader();
        loader.load(detector, conditions);
    }
}
