package org.hps.readout.ecal.updated;

import org.lcsim.event.MCParticle;

public class MCParticleReadoutDriver extends SLICDataReadoutDriver<MCParticle> {
	public MCParticleReadoutDriver() {
		super(MCParticle.class);
		setCollectionName("MCParticle");
	}
}