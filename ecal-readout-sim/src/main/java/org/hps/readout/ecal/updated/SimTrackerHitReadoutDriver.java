package org.hps.readout.ecal.updated;

import org.hps.readout.SLICDataReadoutDriver;
import org.lcsim.event.SimTrackerHit;

public class SimTrackerHitReadoutDriver extends SLICDataReadoutDriver<SimTrackerHit> {
	public SimTrackerHitReadoutDriver() {
		super(SimTrackerHit.class, 0xc0000000);
	}
}