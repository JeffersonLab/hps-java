package org.hps.monitoring.subsys;

/**
 * The <tt>SystemStatus</tt> describes the state of a system, e.g. whether it is okay 
 * or some level of error has occurred.  Listeners can be registered on these objects, 
 * which will be notified whenever the status changes, in order to update a GUI component, 
 * trip an alarm, etc.
 * 
 * There is one <tt>SystemStatus</tt> object for each quantity to be monitored
 * on a sub-system.  New objects are not created when the status changes.  Instead,
 * the <tt>StatusCode</tt> is changed with a custom message describing the new state.
 * Listeners are updated whenever the status is changed.  It is up to the notified
 * object to determine what to do when the state changes.  
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public interface SystemStatus {

    /**
     * Code that represents the status of the monitoring quantity.    
     */
    enum StatusCode {
        
        /** Status encodings with a string name and descriptor. */
        OKAY   ("System appears to be working."),
        UNKNOWN("Status is not known."),
        OFFLINE("System is currently offline."),               
        WARNING("There is a non-fatal warning."),
        ERROR  ("A serious error has occurred."),
        ALARM  ("A fatal error has tripped an alarm."),
        HALT   ("The system should be immediately halted.");
        
        String description;
        
        StatusCode(String description) {
            this.description = description;
        }
                                
        /**
         * Get the description of the status.
         * @return The description of the status.
         */
        public String description() {
            return description;
        }
    }
    
    enum SystemName {        
        ET,
        ECAL,
        SVT,
        TRIGGER;        
    }
    
    /**
     * Get the name of the sub-system e.g. "SVT".
     */
    String getSystemName();
    
    /**
     * Get the current status code.
     * @return The current status code.
     */
    StatusCode getStatusCode();
    
    /**
     * Get the current message.
     * @return The current message
     */
    String getMessage();
    
    /**
     * Get the description of the system status.
     * This is used to differentiate multiple monitoring points
     * on the same sub-system so it could be something like "SVT occupancy rates".
     * @return The description of the system status.
     */
    String getDescription();

    /**
     * Set the current status code, which will cause the last changed 
     * time to be set and the listeners to be notified.
     * @param code The new status code.
     */
    void setStatusCode(StatusCode code, String message);
    
    /**
     * Get the time when the system status last changed.
     * @return The time when the system status changed.
     */
    long getLastChangedMillis();
    
    /**
     * Add a listener to receive notification when the system
     * status changes.
     * @param listener The listener object.
     */
    void addListener(SystemStatusListener listener);
    
    /**
     * Set whether this status is active.  
     * Listeners will not be notified of state changes when active is <code>False</code>.     
     */
    void setActive(boolean active);
    
    /**
     * True if the status is active.
     * @return True if status is active.
     */
    boolean isActive();
}
