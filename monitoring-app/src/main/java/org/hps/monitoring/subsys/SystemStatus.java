package org.hps.monitoring.subsys;

/**
 * The <tt>SystemStatus</tt> describes the state of a system, e.g. whether it is okay 
 * or some level of error has occurred.  Listeners can be registered, which will 
 * be notified whenever the status changes, in order to update a GUI, trip an alarm, etc.
 * 
 * There is one <tt>SystemStatus</tt> object for each quantity to be monitored
 * on a sub-system.  New objects are not created when the status changes.  Instead,
 * the <tt>StatusCode</tt> is changed with a custom message describing the new state.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public interface SystemStatus {

    /**
     * Code that represents the status of the monitoring quantity.    
     */
    enum StatusCode {
        
        OKAY   (0, "okay",    "The system appears to be working."),
        UNKNOWN(1, "unknown", "The status is not known."),
        OFFLINE(2, "offline", "The system is currently offline."),               
        WARNING(3, "warning", "There is a non-fatal warning."),
        ERROR  (4, "error",   "A non-fatal but serious error has occurred."),
        ALARM  (5, "alarm",   "An error has occurred and an alarm should trip."),
        HALT   (6, "halt",    "The system should be immediately halted.");
        
        int code;
        String name;
        String description;
        
        StatusCode(int code, String name, String description) {
            this.code = code;
            this.name = name;
            this.description = description;
        }
        
        int getRawCode() {
            return code;
        }
        
        String getName() {
            return name;
        }                  
        
        String getDescription() {
            return description;
        }
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
}
