package org.hps.users.kmccarty;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.physics.vec.Hep3Vector;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class ParticleMCAnalysisDriver extends Driver {
	// Store collection names.
	private String particleCollectionName = "MCParticle";
	
	// Declare plots.
	private AIDA aida = AIDA.defaultInstance();
	private IHistogram1D chargedTracksPlot = aida.histogram1D("MC Analysis/Event Tracks", 10, -0.5, 9.5);
	private IHistogram1D allPlot = aida.histogram1D("MC Analysis/Electron Energy Distribution", 110, 0, 2.2);
	private IHistogram1D electronPlot = aida.histogram1D("MC Analysis/Electron Energy Distribution", 110, 0, 2.2);
	private IHistogram1D positronPlot = aida.histogram1D("MC Analysis/Positron Energy Distribution", 110, 0, 2.2);
	private IHistogram1D momentumXPlot = aida.histogram1D("MC Analysis/Particle x-Momentum Distribution", 110, 0.0, 1.1);
	private IHistogram1D momentumYPlot = aida.histogram1D("MC Analysis/Particle y-Momentum Distribution", 110, 0.0, 1.1);
	private IHistogram1D momentumZPlot = aida.histogram1D("MC Analysis/Particle z-Momentum Distribution", 110, 0.0, 1.1);
	private IHistogram1D anglePlot = aida.histogram1D("MC Analysis/Positron\\Electron Pair Angle Distribution", 90, 0, 180);
	private IHistogram1D momentumSumPlot = aida.histogram1D("MC Analysis/Positron\\Electron Momentum Sum Distribution", 220, 0, 2.2);
	private IHistogram2D momentumPlot = aida.histogram2D("MC Analysis/Particle Momentum Distribution", 110, 0.0, 1.1, 110, 0.0, 1.1);
	private IHistogram2D momentumSum2DPlot = aida.histogram2D("MC Analysis/Positron\\Electron 2D Momentum Distribution", 55, 0, 1.1, 55, 0, 1.1);
	
	@Override
	public void process(EventHeader event) {
		// Skip the event if there is no Monte Carlo collection.
		if(!event.hasCollection(MCParticle.class, particleCollectionName)) {
			return;
		}
		
		// Get the list of Monte Carlo particles.
		List<MCParticle> particleList = event.get(MCParticle.class, particleCollectionName);
		
		// Track the positive and negative particles.
		List<MCParticle> electronList = new ArrayList<MCParticle>();
		List<MCParticle> positronList = new ArrayList<MCParticle>();
		
		// Count the number of particles in the event.
		int chargedParticles = 0;
		
		// Iterate through the particles.
		for(MCParticle particle : particleList) {
			// Look at only t = 0 particles.
			if(particle.getProductionTime() == 0) {
				// Plot the x/y momentum of each particle.
				momentumPlot.fill(particle.getMomentum().x(), particle.getMomentum().y());
				
				// If the particle is charged, increment the charged
				// particle count.
				if(particle.getCharge() > 0) {
					chargedParticles++;
				}
				
				// Get the particle momentum in each direction.
				momentumXPlot.fill(particle.getMomentum().x());
				momentumYPlot.fill(particle.getMomentum().y());
				momentumZPlot.fill(particle.getMomentum().z());
				
				// Populate the general momentum plot.
				allPlot.fill(particle.getMomentum().magnitude());
				momentumPlot.fill(particle.getMomentum().x(), particle.getMomentum().y());
				
				// Store each particle based on its PID and populate
				// the appropriate plot.
				if(particle.getPDGID() == 11) {
					electronList.add(particle);
					electronPlot.fill(particle.getMomentum().magnitude());
				} else if(particle.getPDGID() == -11) {
					positronList.add(particle);
					positronPlot.fill(particle.getMomentum().magnitude());
				}
			}
		}
		
		// Populate the charged particles plot.
		chargedTracksPlot.fill(chargedParticles);
		
		// Form all electron/positron pairs.
		List<MCParticle[]> pairList = new ArrayList<MCParticle[]>();
		for(MCParticle electron : electronList) {
			for(MCParticle positron : positronList) {
				pairList.add(new MCParticle[] { electron, positron });
			}
		}
		
		// Plot the positron/electron pair distributions.
		for(MCParticle[] pair : pairList) {
			anglePlot.fill(getVectorAngle(pair[0].getMomentum(), pair[1].getMomentum()));
			momentumSumPlot.fill(getVectorSum(pair[0].getMomentum(), pair[1].getMomentum()));
			momentumSum2DPlot.fill(pair[0].getMomentum().magnitude(), pair[1].getMomentum().magnitude());
		}
	}
	
	private static final double getVectorSum(Hep3Vector v1, Hep3Vector v2) {
		// Calculate the sum of the sum of the vector components, squared.
		double sum = 0;
		for(int i = 0; i < 3; i++) {
			double elementSum = v1.v()[i] + v2.v()[i];
			sum += (elementSum * elementSum);
		}
		
		// Return the square root of the sum.
		return Math.sqrt(sum);
	}
	
	private static final double getVectorAngle(Hep3Vector v1, Hep3Vector v2) {
		// The vector angle is defined as Acos[(v1 · v2) / (‖v1‖ × ‖v2‖)]
		return Math.acos(getDotProduct(v1, v2) / (v1.magnitude() * v2.magnitude())) / Math.PI * 180.0;
	}
	
	private static final double getDotProduct(Hep3Vector v1, Hep3Vector v2) {
		// Calculate the sum of the vector element products.
		int product = 0;
		for(int i = 0; i < 3; i++) {
			product += (v1.v()[i] * v2.v()[i]);
		}
		
		// Return the result.
		return product;
	}
}