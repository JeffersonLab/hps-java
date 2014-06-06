package org.hps.monitoring.subsys;

/**
 * The system status describes the state of a system, e.g. whether
 * it is okay or some level of error has occurred.  Listeners can 
 * be registered which will be notified whenever the status changes,
 * in order to update a GUI, trip an alarm, etc.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public interface SystemStatus {

    /**
     * Code that represents the system's overall status.        
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
        
        StatusCode(int code, String name, String description) {
            this.code = code;
            this.name = name;
        }
        
        int getRawCode() {
            return code;
        }
        
        String getName() {
            return name;
        }                       
    }
    
    /**
     * Get the current status code.
     * @return The current status code.
     */
    StatusCode getStatusCode();

    /**
     * Set the current status code, which will cause the last changed 
     * time to be set and the listeners to be notified.
     * @param code The new status code.
     */
    void setStatusCode(StatusCode code);
    
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
