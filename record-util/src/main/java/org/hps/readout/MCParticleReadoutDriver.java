package org.hps.readout;

import org.lcsim.event.MCParticle;

/**
 * <code>MCParticleReadoutDriver</code> handles SLIC objects in input
 * Monte Carlo files of type {@link org.lcsim.event.MCParticle
 * MCParticle}.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @see org.hps.readout.SLICDataReadoutDriver
 */
public class MCParticleReadoutDriver extends SLICDataReadoutDriver<MCParticle> {
	/**
	 * Instantiate an instance of {@link
	 * org.hps.readout.SLICDataReadoutDriver SLICDataReadoutDriver}
	 * for objects of type {@link org.lcsim.event.MCParticle
	 * MCParticle}. These do not require any special LCIO flags.
	 */
	public MCParticleReadoutDriver() {
		super(MCParticle.class);
		setCollectionName("MCParticle");
	}
}