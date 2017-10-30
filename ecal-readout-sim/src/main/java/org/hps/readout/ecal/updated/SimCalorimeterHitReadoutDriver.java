package org.hps.readout.ecal.updated;

import org.hps.readout.SLICDataReadoutDriver;
import org.lcsim.event.SimCalorimeterHit;

public class SimCalorimeterHitReadoutDriver extends SLICDataReadoutDriver<SimCalorimeterHit> {
	public SimCalorimeterHitReadoutDriver() {
		super(SimCalorimeterHit.class, 0xe0000000);
	}
}
