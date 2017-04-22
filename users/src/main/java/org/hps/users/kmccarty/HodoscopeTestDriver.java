package org.hps.users.kmccarty;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

public class HodoscopeTestDriver extends Driver {
	private static final int ELECTRON = 11;
	private static final int POSITRON = -11;
	private static final int HODOSCOPE_AFTSCORE_POSITION = 1114;
	private static final int HODOSCOPE_FORESCORE_POSITION = 1107;
	
	private AIDA aida = AIDA.defaultInstance();
	private IHistogram1D foreParticleMomentum = aida.histogram1D("Hodoscope Fore Particle Momentum", 460, 0.000, 4.600);
	private IHistogram1D foreParticleMomentumX = aida.histogram1D("Hodoscope Fore Particle x-Momentum", 200, 0.000, 0.100);
	private IHistogram1D foreParticleMomentumY = aida.histogram1D("Hodoscope Fore Particle y-Momentum", 200, 0.000, 0.100);
	private IHistogram1D foreParticleMomentumZ = aida.histogram1D("Hodoscope Fore Particle z-Momentum", 460, 0.000, 4.600);
	
	private IHistogram1D aftParticleMomentum = aida.histogram1D("Hodoscope Aft Particle Momentum", 460, 0.000, 4.600);
	private IHistogram1D aftParticleMomentumX = aida.histogram1D("Hodoscope Aft Particle x-Momentum", 200, 0.000, 0.100);
	private IHistogram1D aftParticleMomentumY = aida.histogram1D("Hodoscope Aft Particle y-Momentum", 200, 0.000, 0.100);
	private IHistogram1D aftParticleMomentumZ = aida.histogram1D("Hodoscope Aft Particle z-Momentum", 460, 0.000, 4.600);
	
	private IHistogram1D aftParticleReduxMomentum = aida.histogram1D("Hodoscope Aft Repeat Particle Momentum", 460, 0.000, 4.600);
	private IHistogram1D aftParticleReduxMomentumX = aida.histogram1D("Hodoscope Aft Repeat Particle x-Momentum", 200, 0.000, 0.100);
	private IHistogram1D aftParticleReduxMomentumY = aida.histogram1D("Hodoscope Aft Repeat Particle y-Momentum", 200, 0.000, 0.100);
	private IHistogram1D aftParticleReduxMomentumZ = aida.histogram1D("Hodoscope Aft Repeat Particle z-Momentum", 460, 0.000, 4.600);
	
	private IHistogram1D originDecayPositionZ = aida.histogram1D("Secondary Particle Decay z-Position", 320, 0.000, 1600);
	private IHistogram2D originDecayPositionYZ = aida.histogram2D("Secondary Particle Decay y-Position vs. z-Position",
			160, -400, 400, 320, 0.000, 1600);
	private IHistogram2D originDecayPositionXZ = aida.histogram2D("Secondary Particle Decay x-Position vs. z-Position",
			240, -600, 600, 320, 0.000, 1600);
	
	private IHistogram1D negMomentumMomentum = aida.histogram1D("-|p| Particle Momentum", 460, -2.3, 2.3);
	private IHistogram1D negMomentumMomentumX = aida.histogram1D("-|p| Particle x-Momentum", 200, -0.100, 0.100);
	private IHistogram1D negMomentumMomentumY = aida.histogram1D("-|p| Particle y-Momentum", 200, -0.100, 0.100);
	private IHistogram1D negMomentumMomentumZ = aida.histogram1D("-|p| Particle z-Momentum", 460, -2.3, 2.3);
	private IHistogram2D originNegMomentumPositionYZ = aida.histogram2D("-|p| Particle Decay y-Position vs. z-Position",
			160, -400, 400, 320, 0.000, 1600);
	private IHistogram2D originNegMomentumPositionXZ = aida.histogram2D("-|p| Particle Decay x-Position vs. z-Position",
			240, -600, 600, 320, 0.000, 1600);
	
	private IHistogram1D originBackScatterPositionZ = aida.histogram1D("Back-Scatter Particle Decay z-Position", 320, 0.000, 1600);
	private IHistogram1D parentEnergyBackScatter = aida.histogram1D("Back-Scatter Parent Energy", 920, 0.000, 4.6);
	
	private IHistogram2D electronHodoscopePosition  = aida.histogram2D("Hodoscope Position (Electron)",
			900, -450, 450, 300, -150, 150);
	private IHistogram2D positronHodoscopePosition  = aida.histogram2D("Hodoscope Position (Positron)",
			900, -450, 450, 300, -150, 150);
	
	private IHistogram2D positronEnergyXDiff = aida.histogram2D("Positron Energy vs. Dx", 115, 0.000, 4.600, 125, -100, 100);
	private IHistogram2D positronEnergyYDiff = aida.histogram2D("Positron Energy vs. Dy", 115, 0.000, 4.600, 125, -100, 100);
	private IHistogram2D electronEnergyXDiff = aida.histogram2D("Electron Energy vs. Dx", 115, 0.000, 4.600, 125, -100, 100);
	private IHistogram2D electronEnergyYDiff = aida.histogram2D("Electron Energy vs. Dy", 115, 0.000, 4.600, 125, -100, 100);
	private IHistogram2D positronXDiff = aida.histogram2D("Positron Hodoscope x vs. Calorimeter x", 160, -400, 400, 160, -400, 400);
	private IHistogram2D positronYDiff = aida.histogram2D("Positron Hodoscope y vs. Calorimeter y", 80, -200, 200, 80, -200, 200);
	
	private IHistogram2D electronXDiff = aida.histogram2D("Electron Hodoscope x vs. Calorimeter x", 160, -400, 400, 160, -400, 400);
	private IHistogram2D electronYDiff = aida.histogram2D("Electron Hodoscope y vs. Calorimeter y", 80, -200, 200, 80, -200, 200);
	
	private Map<Integer, Double> countMap = new HashMap<Integer, Double>();
	private int ecalHitsSeen = 0;
	private int hodoHitsSeen = 0;
	private int hodoAftHitsSeen = 0;
	private int hodoForeHitsSeen = 0;
	
	private int totalEvents = 0;
	private int forwardParticles = 0;
	private int backwardParticles = 0;
	
	private final Map<Integer, String> particleIDNameMap = new HashMap<Integer, String>();
	
	@Override
	public void startOfData() {
		particleIDNameMap.put(211, "Pion+");
		particleIDNameMap.put(-211, "Pion-");
		particleIDNameMap.put(2112, "Neutron");
		particleIDNameMap.put(2212, "Proton");
		particleIDNameMap.put(22,   "Photon");
		particleIDNameMap.put(11,   "Electron");
		particleIDNameMap.put(-11,  "Positron");
		particleIDNameMap.put(12,   "Neutrino");
		particleIDNameMap.put(111,  "Pion0");
	}
	
	@Override
	public void process(EventHeader event) {
		// Get the hodoscope object collections.
		List<MCParticle> particles = event.get(MCParticle.class, "MCParticle");
		List<SimTrackerHit> ecalHits = event.get(SimTrackerHit.class, "TrackerHitsECal");
		List<SimTrackerHit> hodoscopeHits = event.get(SimTrackerHit.class, "HodoscopeHits");
		List<SimTrackerHit> hodoscopeAftHits = event.get(SimTrackerHit.class, "HodoscopeAftHits");
		List<SimTrackerHit> hodoscopeForeHits = event.get(SimTrackerHit.class, "HodoscopeForeHits");
		
		ecalHitsSeen += ecalHits.size();
		hodoHitsSeen += hodoscopeHits.size();
		hodoAftHitsSeen += hodoscopeAftHits.size();
		hodoForeHitsSeen += hodoscopeForeHits.size();
		
		// Plot the decay distance of each particle.
		for(MCParticle particle : particles) {
			// Ignore the initial state particles.
			if(particle.getProductionTime() != 0) {
				originDecayPositionXZ.fill(particle.getOriginX(), particle.getOriginZ());
				originDecayPositionYZ.fill(particle.getOriginY(), particle.getOriginZ());
				originDecayPositionZ.fill(particle.getOriginZ());
			}
		}
		
		// Map the particles that pass through the calorimeter scoring
		// plane to their scoring plane hit object.
		Map<MCParticle, SimTrackerHit> ecalHitMap = new HashMap<MCParticle, SimTrackerHit>();
		for(SimTrackerHit ecalHit : ecalHits) {
			ecalHitMap.put(ecalHit.getMCParticle(), ecalHit);
		}
		
		// Track the number of particles of each type that are seen.
		for(SimTrackerHit foreHit : hodoscopeForeHits) {
			if(foreHit.getMCParticle().getPDGID() == ELECTRON) {
				electronHodoscopePosition.fill(foreHit.getPosition()[0], foreHit.getPosition()[1]);
			} else if(foreHit.getMCParticle().getPDGID() == POSITRON) {
				positronHodoscopePosition.fill(foreHit.getPosition()[0], foreHit.getPosition()[1]);
			}
			
			if(countMap.containsKey(foreHit.getMCParticle().getPDGID())) {
				countMap.put(foreHit.getMCParticle().getPDGID(), countMap.get(foreHit.getMCParticle().getPDGID()) + 1);
			} else {
				countMap.put(foreHit.getMCParticle().getPDGID(), 1.0);
			}
		}
		
		// Check for particles that pass through the rear scoring plane
		// more than once. This checks for particles that pass through
		// the hodoscope, and then reflect back into it.
		
		// Count the number of events.
		totalEvents++;
		
		// Track the number of particles that pass through the front
		// of the hodoscope. These should particles that have both a
		// charge and also originated before the hodoscope. (Particles
		// which come from after it are treated as back-scatter.)
		for(SimTrackerHit foreHit : hodoscopeForeHits) {
			if(foreHit.getMCParticle().getOriginZ() < HODOSCOPE_FORESCORE_POSITION && foreHit.getMCParticle().getCharge() != 0) {
				// Plot the forward particle information.
				forwardParticles++;
				foreParticleMomentum.fill(getMagnitude(foreHit.getMomentum()));
				foreParticleMomentumX.fill(foreHit.getMomentum()[0]);
				foreParticleMomentumY.fill(foreHit.getMomentum()[1]);
				foreParticleMomentumZ.fill(foreHit.getMomentum()[2]);
				
				// Plot the particle position difference information.
				if(ecalHitMap.containsKey(foreHit.getMCParticle()) && getMagnitude(foreHit.getMomentum()) > 0.400) {
					SimTrackerHit ecalHit = ecalHitMap.get(foreHit.getMCParticle());
					if(foreHit.getMCParticle().getPDGID() == POSITRON) {
						positronXDiff.fill(ecalHit.getPosition()[0], foreHit.getPosition()[0]);
						positronYDiff.fill(ecalHit.getPosition()[1], foreHit.getPosition()[1]);
						positronEnergyXDiff.fill(getMagnitude(foreHit.getMomentum()), foreHit.getPosition()[0] - ecalHit.getPosition()[0]);
						positronEnergyYDiff.fill(getMagnitude(foreHit.getMomentum()), foreHit.getPosition()[1] - ecalHit.getPosition()[1]);
					} else if(foreHit.getMCParticle().getPDGID() == ELECTRON) {
						electronXDiff.fill(ecalHit.getPosition()[0], foreHit.getPosition()[0]);
						electronYDiff.fill(ecalHit.getPosition()[1], foreHit.getPosition()[1]);
						electronEnergyXDiff.fill(getMagnitude(foreHit.getMomentum()), foreHit.getPosition()[0] - ecalHit.getPosition()[0]);
						electronEnergyYDiff.fill(getMagnitude(foreHit.getMomentum()), foreHit.getPosition()[1] - ecalHit.getPosition()[1]);
					}
				}
			}
		}
		
		// Track particles in the event that pass through the rear
		// scoring plane more than once.
		Set<SimTrackerHit> aftHitSet = new HashSet<SimTrackerHit>();
		
		// Analyze back-scatter particles.
		for(SimTrackerHit aftHit : hodoscopeAftHits) {
			// By definition, the hodoscope only detects particles that
			// are charged. Ignore any others.
			if(aftHit.getMCParticle().getCharge() != 0) {
				// The first class of back-scatter particles are any
				// that originate after the hodoscope. These must have
				// been produced with momentum such that the particle
				// pointed backwards.
				if(aftHit.getMCParticle().getOriginZ() > HODOSCOPE_AFTSCORE_POSITION) {
					// Plot particle momenta.
					backwardParticles++;
					aftParticleMomentum.fill(getMagnitude(aftHit.getMomentum()));
					aftParticleMomentumX.fill(aftHit.getMomentum()[0]);
					aftParticleMomentumY.fill(aftHit.getMomentum()[1]);
					aftParticleMomentumZ.fill(aftHit.getMomentum()[2]);
					
					// Plot the particle's origin z-position and origin
					// parent energy.
					if(aftHit.getMCParticle().getProductionTime() != 0) {
						originBackScatterPositionZ.fill(aftHit.getMCParticle().getOriginZ());
					}
					if(aftHit.getMCParticle().getParents().size() != 0) {
						parentEnergyBackScatter.fill(getMagnitude(aftHit.getMomentum()));
					}
				}
				
				// The second class of back-scatter are particles that
				// pass through the read scoring plane twice, but have
				// an origin earlier than the hodoscope. These must
				// have passed through the hodosocpe initially, and
				// were then scattered back toward it later.
				else {
					// If the particle has already been seen once, it
					// is a real back-scatter particle.
					if(aftHitSet.contains(aftHit)) {
						// Plot particle momenta.
						backwardParticles++;
						aftParticleReduxMomentum.fill(getMagnitude(aftHit.getMomentum()));
						aftParticleReduxMomentumX.fill(aftHit.getMomentum()[0]);
						aftParticleReduxMomentumY.fill(aftHit.getMomentum()[1]);
						aftParticleReduxMomentumZ.fill(aftHit.getMomentum()[2]);
						
						// Plot the particle's origin z-position and origin
						// parent energy.
						if(aftHit.getMCParticle().getProductionTime() != 0) {
							originBackScatterPositionZ.fill(aftHit.getMCParticle().getOriginZ());
						}
						if(aftHit.getMCParticle().getParents().size() != 0) {
							parentEnergyBackScatter.fill(getMagnitude(aftHit.getMomentum()));
						}
					}
					
					// Otherwise, note that it was seen once.
					else {
						aftHitSet.add(aftHit);
					}
				}
				
				// Also, look at where particles with negative momentum
				// in the z-direction originate specifically.
				if(aftHit.getMomentum()[2] < 0) {
					// Ignore initial state particles!
					if(aftHit.getMCParticle().getProductionTime() != 0) {
						negMomentumMomentum.fill(getMagnitude(aftHit.getMomentum()));
						negMomentumMomentumX.fill(aftHit.getMomentum()[0]);
						negMomentumMomentumY.fill(aftHit.getMomentum()[1]);
						negMomentumMomentumZ.fill(aftHit.getMomentum()[2]);
						originNegMomentumPositionXZ.fill(aftHit.getMCParticle().getOriginX(), aftHit.getMCParticle().getOriginZ());
						originNegMomentumPositionYZ.fill(aftHit.getMCParticle().getOriginY(), aftHit.getMCParticle().getOriginZ());
					}
				}
			}
		}
	}
	
	@Override
	public void endOfData() {
		System.out.println("ECal Hits: " + ecalHitsSeen);
		System.out.println("Hodoscope Hits: " + hodoHitsSeen);
		System.out.println("Hodoscope Aft Hits: " + hodoAftHitsSeen);
		System.out.println("Hodoscope Fore Hits: " + hodoForeHitsSeen);
		System.out.println("\n");
		System.out.println("Total Events: " + totalEvents);
		System.out.println("Forward Particles: " + forwardParticles);
		System.out.println("Backward Particles: " + backwardParticles);
		System.out.println("\nParticle Counts:");
		for(Entry<Integer, Double> entry : countMap.entrySet()) {
			System.out.printf("\t%-8s :: %.0f%n",
					particleIDNameMap.get(entry.getKey()) != null ? particleIDNameMap.get(entry.getKey()) : Integer.toString(entry.getKey()),
					entry.getValue());
		}
	}
	
	private static final double getMagnitude(double[] v) {
		double squareSum = 0;
		for(double vi : v) {
			squareSum += Math.pow(vi, 2);
		}
		return Math.sqrt(squareSum);
	}
}
