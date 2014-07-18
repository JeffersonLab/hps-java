package org.hps.users.kmccarty;

import java.util.List;

import hep.aida.IHistogram1D;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class PionEnergyDriver extends Driver {
	// Analysis plots.
    AIDA aida = AIDA.defaultInstance();
	IHistogram1D pionEnergy;
	
	// Collection names.
	private String particleCollectionName = "MCParticles";
	
	public void setParticleCollectionName(String particleCollectionName) {
		this.particleCollectionName = particleCollectionName;
	}
	
	public void process(EventHeader event) {
		// Check if there exists a Monte Carlo particle collection.
		if(event.hasCollection(MCParticle.class, particleCollectionName)) {
			// Get the Monte Carlo particles.
			List<MCParticle> particleList = event.get(MCParticle.class, particleCollectionName);
			
			// Iterate over the particles and combine the t = 0 particle
			// energies together to calculate the parent particle energy.
			double parentEnergy = 0.0;
			for(MCParticle particle : particleList) {
				if(particle.getProductionTime() == 0.0) {
					parentEnergy += particle.getEnergy();
				}
			}
			
			// Add the parent energy to the histogram.
			pionEnergy.fill(parentEnergy);
		}
	}
	
	public void startOfData() {
		pionEnergy = aida.histogram1D("Pion Analysis :: Pion Energy Distribution", 110, 0.0, 2.2);
	}
}
