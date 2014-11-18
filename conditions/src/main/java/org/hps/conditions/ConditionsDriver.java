package org.hps.conditions;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;
import org.lcsim.util.Driver;

/**
 * This {@link org.lcsim.util.Driver} can be used to customize the behavior
 * of the {@link DatabaseConditionsManager}.  It allows the setting of a 
 * detector name and run number, if the user wishes to override the default
 * behavior of the conditions system, which generally reads this information
 * from the LCSim events.  It is not necessary to run this Driver in order
 * to activate the database conditions system. 
 *
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ConditionsDriver extends Driver {

    String detectorName = null;
    DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
    boolean freeze;
    
    /**
     * Default constructor.
     */
    public ConditionsDriver() {
    }
    
    /**
     * Set the name of the detector to use.
     * 
     * @param detectorName The name of the detector.
     */
    public void setDetectorName(String detectorName) {
        this.detectorName = detectorName;
    }
    
    /**
     * Set whether or not the conditions system should be "frozen" after the 
     * detector name and run number are set.  When frozen, the conditions system
     * will ignore subsequent calls to {@link org.lcsim.conditions.ConditionsManager#setDetector(String, int)}
     * and instead use the user supplied detector and run for the whole job.
     * 
     * @param freeze True to freeze the conditions system after it is setup.
     */
    public void setFreeze(boolean freeze) {
        this.freeze = freeze;
    }
    
    /**
     * Set a custom run number to setup the conditions system.
     * In the case where the actual event stream has run numbers that differ from this one,
     * most likely the Driver should be configured to be frozen after setup using
     * {@link #setFreeze(boolean)}.  
     * 
     * The method {@link #setDetectorName(String)} needs to be called before this one
     * or an exception will be thrown.
     * 
     * @param runNumber The user supplied run number for the job.
     */
    public void setRunNumber(int runNumber) {
        if (this.detectorName == null) {
            throw new RuntimeException("The detector name must be set before the run number.");
        }
        try {
            conditionsManager.setDetector(detectorName, runNumber);
        } catch (ConditionsNotFoundException e) {
            throw new RuntimeException("Error setting conditions.", e);
        }
        if (freeze) {
            conditionsManager.freeze();
        }
    }            
    
    /**
     * Set the name of the ECAL subdetector which the conditions manager will use for loading conditions
     * onto the detector.
     * @param ecalName The name of the ECAL subdetector.
     */
    public void setEcalName(String ecalName) {
        conditionsManager.setEcalName(ecalName);
    }
    
    /**
     * Set the name of the SVT subdetector which the conditions manager will use for loading conditions
     * onto the detector.
     * @param svtName The name of the SVT subdetector.
     */
    public void setSvtName(String svtName) {
        conditionsManager.setSvtName(svtName);
    }
}
