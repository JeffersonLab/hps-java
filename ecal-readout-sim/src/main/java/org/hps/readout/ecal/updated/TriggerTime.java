package org.hps.readout.ecal.updated;

class TriggerTime implements Comparable<TriggerTime> {
	private final double time;
	private final double writeTime;
	private final ReadoutDriver trigger;
	
	TriggerTime(double time, double writeTime, ReadoutDriver trigger) {
		this.time = time;
		this.writeTime = writeTime;
		this.trigger = trigger;
	}
	
	double getTriggerTime() {
		return time;
	}
	
	double getTriggerWriteTime() {
		return writeTime;
	}
	
	ReadoutDriver getTriggeringDriver() {
		return trigger;
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
}