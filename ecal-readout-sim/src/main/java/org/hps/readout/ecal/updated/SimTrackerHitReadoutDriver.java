package org.hps.readout.ecal.updated;

import org.lcsim.event.SimTrackerHit;

public class SimTrackerHitReadoutDriver extends SLICDataReadoutDriver<SimTrackerHit> {
	public SimTrackerHitReadoutDriver() {
		super(SimTrackerHit.class);
	}
}