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
 */
public interface SystemStatus {
          
    /**
     * Get the sub-system e.g. "SVT".
     * @return The assigned sub-system.
     */
    Subsystem getSubsystem();
    
    /**
     * Get the current status code.
     * @return The current status code.
     */
    StatusCode getStatusCode();
    
    /**
     * Get the description of the system status.
     * This is used to differentiate multiple monitoring points
     * on the same sub-system so it could be something like "SVT occupancy rates".
     * @return The description of the system status.
     */
    String getDescription();
    
    /**
     * Get the current message.
     * @return The current message
     */
    String getMessage();
    
    /**
     * Set the current status code and message, which will cause the last changed 
     * time to be set and the listeners to be notified of the status change.
     * @param code The new status code.
     */
    void setStatus(StatusCode code, String message);
    
    /**
     * Get the time when the system status last changed.
     * @return The time when the system status changed.
     */
    long getLastChangedMillis();
    
    /**
     * Add a listener to receive notification when the status changes.
     * @param listener The listener object.
     */
    void addListener(SystemStatusListener listener);
    
    /**
     * Set whether this status is active.  Inactive statuses will not be
     * updated in the GUI.  This can be changed "on the fly" in the system status panel.
     * Listeners will NOT be notified of state changes when active is <code>False</code>.     
     */
    void setActive(boolean active);
    
    /**
     * True if the status is active.
     * @return True if status is active.
     */
    boolean isActive();
    
    /**
     * True if the status can be cleared.
     * @return True if status can be cleared.
     */
    boolean isClearable();
}
