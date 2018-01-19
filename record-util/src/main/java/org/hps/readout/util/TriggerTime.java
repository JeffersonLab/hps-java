package org.hps.readout.util;

import org.hps.readout.ReadoutDriver;

/**
 * Class <code>TriggerTime</code> stores the time at which a trigger
 * occurs in terms of simulation time, and also the time at which a
 * trigger should be written to an output file. Additionally, the
 * triggering driver is also tracked.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class TriggerTime implements Comparable<TriggerTime> {
	/**
	 * The simulation time (corrected for time offsets) at which a
	 * trigger occurs.
	 */
	private final double time;
	/**
	 * The driver that produced the trigger.
	 */
	private final ReadoutDriver trigger;
	/**
	 * The time at which the data manager should write the trigger to
	 * the output file.
	 */
	private final double writeTime;
	
	/**
	 * Instantiates a new <code>TriggerTime</code> object.
	 * @param time - The simulation time (corrected for time offsets)
	 * at which a trigger occurs.
	 * @param writeTime - The time at which the data manager should
	 * write the trigger to the output file.
	 * @param trigger - The driver that produced the trigger.
	 */
	public TriggerTime(double time, double writeTime, ReadoutDriver trigger) {
		this.time = time;
		this.writeTime = writeTime;
		this.trigger = trigger;
	}
	
	@Override
	public int compareTo(TriggerTime tt) {
		if(writeTime != tt.writeTime) {
			return Double.compare(writeTime, tt.writeTime);
		} else if(time != tt.time) {
			return Double.compare(time, tt.time);
		} else {
			return 0;
		}
	}
	
	/**
	 * Gets the driver class which produced the trigger.
	 * @return Returns the class of the trigger driver which created
	 * the trigger.
	 */
	public ReadoutDriver getTriggeringDriver() {
		return trigger;
	}
	
	/**
	 * Gets the simulation time at which the trigger occurred. This
	 * value is corrected for the time offsets introduced by any
	 * relevant drivers.
	 * @return Returns the simulation time of the trigger in units of
	 * nanoseconds.
	 */
	public double getTriggerTime() {
		return time;
	}
	
	/**
	 * Gets the simulation time at which the {@link
	 * org.hps.readout.ReadoutDataManager ReadoutDataManager} should
	 * write the readout event to the output LCIO file.
	 * @return Returns the write time for this trigger in units of
	 * nanoseconds.
	 */
	public double getTriggerWriteTime() {
		return writeTime;
	}
}