package org.hps.conditions.config;

import org.hps.conditions.DatabaseConditionsManager;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

public abstract class AbstractConfiguration {

    protected DatabaseConditionsManager manager;
    
    public abstract AbstractConfiguration setup();
    
    public final void load(String detectorName, int runNumber) {
        try {
            manager.setDetector(detectorName, runNumber);
        } catch (ConditionsNotFoundException e) {
            throw new RuntimeException(e);
        }   
    }           
}
