package org.hps.readout.ecal;

import java.util.LinkedList;
import java.util.Queue;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

/**
 * A driver that accepts triggers from TriggerDriver.
 * To implement, write your own processTrigger(), and call checkTrigger() somewhere in process().
 * You might want to set your own default latency in your constructor.
 * readoutDeltaT() and isLive() are meant to be overridden if you're doing something unusual.
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: TriggerableDriver.java,v 1.3 2013/03/20 01:03:32 meeg Exp $
 */
public abstract class TriggerableDriver extends Driver {

    private Queue<Double> triggerTimestamps = new LinkedList<Double>();
    protected double triggerDelay = 0.0; // [ns]

    public void setTriggerDelay(double triggerDelay) {
        this.triggerDelay = triggerDelay;
    }

    /**
     * 
     * @return time reference for hits written by this driver in response to a trigger
     */
    public double readoutDeltaT() {
        return ClockSingleton.getTime() + triggerDelay;
    }

    @Override
    public void startOfData() {
        TriggerDriver.addTriggerable(this);
    }

    protected abstract void processTrigger(EventHeader event);

    protected void checkTrigger(EventHeader event) {
        if (triggerTimestamps.peek() != null && ClockSingleton.getTime() >= triggerTimestamps.peek()) {
            processTrigger(event);
            triggerTimestamps.remove();
        }
    }

    public void addTrigger() {
        triggerTimestamps.add(ClockSingleton.getTime() + triggerDelay);
    }

    public boolean isLive() {
        return true;
    }
    
    public abstract int getTimestampType();
}
