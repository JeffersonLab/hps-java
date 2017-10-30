package org.hps.readout;

import org.lcsim.event.MCParticle;

public class MCParticleReadoutDriver extends SLICDataReadoutDriver<MCParticle> {
	public MCParticleReadoutDriver() {
		super(MCParticle.class);
		setCollectionName("MCParticle");
	}
}