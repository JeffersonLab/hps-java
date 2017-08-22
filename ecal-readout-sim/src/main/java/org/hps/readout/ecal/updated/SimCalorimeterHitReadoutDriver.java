package org.hps.readout.ecal.updated;

import org.lcsim.event.SimCalorimeterHit;

public class SimCalorimeterHitReadoutDriver extends SLICDataReadoutDriver<SimCalorimeterHit> {
	public SimCalorimeterHitReadoutDriver() {
		super(SimCalorimeterHit.class);
	}
}
