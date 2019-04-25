package org.hps.readout;

/**
 * Class <code>TriggerDriver</code> is a special subclass of {@link
 * org.hps.readout.ReadoutDriver ReadoutDriver} that is responsible
 * for simulating trigger behavior. It implements additional behavior
 * for handling trigger dead times and issuing triggers to the {@link
 * org.hps.readout.ReadoutDataManager ReadoutDataManager}.<br/><br/>
 * Implementing drivers are responsible for checking if trigger
 * conditions are met. In the event that they are, the method {@link
 * org.hps.readout.TriggerDriver#sendTrigger() sendTrigger()} should
 * be used to issue the trigger to the data manager. This method will
 * automatically check that the dead time condition is met, and will
 * only issue the trigger command in the event that it is, so
 * implementing drivers do not need to check this condition manually.
 * <br/><br/>
 * For usage instructions, please see <code>ReadoutDriver</code>.
 * @author Kyle McCarty <mccarty@jlab.org>
 * @see org.hps.readout.ReadoutDriver
 */
public abstract class TriggerDriver extends ReadoutDriver {
    /**
     * The amount of time that must pass after a trigger before a new
     * trigger can be issued, in units of nanoseconds.
     */
    private double deadTime = 0.0;
    /**
     * The last time at which a trigger was issued to the data
     * manager, in units of nanoseconds.
     */
    private double lastTrigger = Double.NaN;
    
    /**
     * Checks whether the trigger is currently in dead time or not.
     * @return Returns <code>true</code> if the trigger is currently
     * in dead time, and <code>false</code> if it is not and a
     * trigger may be issued.
     */
    protected boolean isInDeadTime() {
        if(Double.isNaN(lastTrigger)) { return false; }
        else { return (lastTrigger + deadTime) > ReadoutDataManager.getCurrentTime(); }
    }
    
    @Override
    protected boolean isPersistent() {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Gets the dead time for this trigger.
     * @return Returns the dead time in units of nanoseconds.
     */
    protected double getDeadTime() {
        return deadTime;
    }
    
    /**
     * Gets the time at which the last trigger occurred.
     * @return Returns the last trigger time in units of nanoseconds,
     * or as {@link java.lang.Double#NaN Double.NaN} if no trigger
     * has occurred yet.
     */
    protected double getLastTriggerTime() {
        return lastTrigger;
    }
    
    @Override
    protected double getReadoutWindowAfter() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    protected double getReadoutWindowBefore() {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Issues a trigger to the data manager so long as the trigger is
     * not presently in dead time.
     */
    protected void sendTrigger() {
        if(!isInDeadTime()) {
            ReadoutDataManager.sendTrigger(this);
            lastTrigger = ReadoutDataManager.getCurrentTime();
        }
    }
    
    /**
     * Sets the dead time for the trigger.
     * @param samples - The amount of time (in events) before another
     * trigger is allowed to occur.
     */
    public void setDeadTime(int samples) {
        deadTime = samples * ReadoutDataManager.getBeamBunchSize();
    }
    
    @Override
    public void setPersistent(boolean state) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setReadoutWindowAfter(double value) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setReadoutWindowBefore(double value) {
        throw new UnsupportedOperationException();
    }
}