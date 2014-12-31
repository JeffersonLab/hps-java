package org.hps.conditions;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;
import org.lcsim.util.Driver;

/**
 * <p>
 * This {@link org.lcsim.util.Driver} can be used to customize the behavior
 * of the {@link DatabaseConditionsManager}.  It allows the setting of a 
 * detector name and run number, as well as other parameters, if the user 
 * wishes to override the default behavior of the conditions system, which
 * is generally activated from LCSim events.  It is not necessary to run this 
 * Driver in order to activate the default database conditions system.  Only 
 * one instance of this Driver should ever be included in a steering file.
 * <p>
 * This is an example of using the Driver in an XML steering file:
 * <pre>
 * {@code
 * <driver name="ConditionsDriver" type="org.hps.conditions.ConditionsDriver">
 *     <detectorName>HPS-TestRun-v5</detectorName>
 *     <runNumber>1351</runNumber>
 *     <xmlConfigResource>/org/hps/conditions/config/conditions_database_testrun_2012.xml</xmlConfigResource>
 *     <ecalName>Ecal</ecalName>
 *     <svtName>Tracker</svtName>
 *     <freeze>true</freeze>
 * </driver>
 * }
 * </pre> 
 * <p>
 * This class is a "special" Driver which must have its initialization occur at the right time.
 * It has a custom initialization method {@link #initialize()} which should be called after 
 * all Driver setup has occurred, but before the job actually begins.  This is so the conditions 
 * system functions properly, including the activation of registered listeners.  The setup is 
 * performed by default in the class {@link org.hps.job.JobManager}, which is used in the 
 * default command line front end of hps-distribution.  If that class is not being used, then
 * the method must be executed manually at the right time to achieve the proper behavior.
 *
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ConditionsDriver extends Driver {

    DatabaseConditionsManager conditionsManager;
    String detectorName = null;
    String ecalName = null;
    String svtName = null;
    String tag = null;
    String xmlConfigResource = null;
    int runNumber = 0;
    boolean freeze;    
    
    /**
     * Default constructor.
     */
    public ConditionsDriver() {
        conditionsManager = new DatabaseConditionsManager();
    }
    
    /**
     * Set the name of the detector to use.
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
     * @param runNumber The user supplied run number for the job.
     */
    public void setRunNumber(int runNumber) {
        this.runNumber = runNumber;
    }            
    
    /**
     * Set the name of the ECAL subdetector which the conditions manager will use for loading conditions
     * onto the detector.
     * @param ecalName The name of the ECAL subdetector.
     */
    public void setEcalName(String ecalName) {
        this.ecalName = ecalName;
    }
       
    /**
     * Set the name of the SVT subdetector which the conditions manager will use for loading conditions
     * onto the detector.
     * @param svtName The name of the SVT subdetector.
     */
    public void setSvtName(String svtName) {
        this.svtName = svtName;
    }
    
    /**
     * Set a tag used to filter ConditionsRecords.
     * @param tag The tag value e.g. "eng_run" etc.
     */
    public void setTag(String tag) {
        this.tag = tag;
    }
    
    /**
     * Set an XML configuration resource.
     * @param xmlConfigResource The XML configuration resource.
     */
    public void setXmlConfigResource(String xmlConfigResource) {
        this.xmlConfigResource = xmlConfigResource;
    }
    
    /**
     * Setup the conditions system based on the Driver parameters.
     * @throws RuntimeException If there is a problem setting up the conditions system.
     */
    public void initialize() {
        if (xmlConfigResource != null) {
            // Set a custom XML configuration resource.
            conditionsManager.setXmlConfig(xmlConfigResource);
        }

        if (ecalName != null) {
            // Set custom ECAL name.
            conditionsManager.setEcalName(ecalName);
        }
        if (svtName != null) {
            // Set custom SVT name.
            conditionsManager.setSvtName(svtName);
        }
        if (tag != null) {
            // Set a tag for filtering ConditionsRecord objects.
            conditionsManager.setTag(tag);
        }
        if (detectorName != null) {
            // The manager can only be initialized here if there is a user supplied detector name.
            try {
                // Initialize the conditions manager.
                conditionsManager.setDetector(detectorName, runNumber);
                if (this.freeze) {
                    // User configured to freeze conditions for the job.
                    conditionsManager.freeze();
                }
            } catch (ConditionsNotFoundException e) {
                throw new RuntimeException("Error initializing conditions from ConditionsDriver.", e);
            }
        }
    }
}
