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
import hep.physics.vec.Hep3Vector;

public class HodoscopeTestDriver extends Driver {
	private static final int PHOTON = 22;
	private static final int ELECTRON = 11;
	private static final int POSITRON = -11;
	private static final int HODOSCOPE_AFTSCORE_POSITION = 1114;
	private static final int HODOSCOPE_FORESCORE_POSITION = 1107;
	private static final double[] BEAM_ANGLE = { Math.sin(0.03), 0, Math.cos(0.03) };
	
	private static final int ALL_PARTICLES = 0;
	private static final int P_GE_1MEV = 1;
	private static final int P_GE_200MEV = 1;
	
	private AIDA aida = AIDA.defaultInstance();
	
	private IHistogram1D initParticleMomentum = aida.histogram1D("Initial Particle Momentum", 460, 0.000, 4.600);
	private IHistogram1D initParticleMomentumX = aida.histogram1D("Initial Particle x-Momentum", 400, -0.100, 0.100);
	private IHistogram1D initParticleMomentumY = aida.histogram1D("Initial Particle y-Momentum", 400, -0.100, 0.100);
	private IHistogram1D initParticleMomentumZ = aida.histogram1D("Initial Particle z-Momentum", 460, -0.100, 4.600);
	
	private IHistogram1D foreParticleMomentum = aida.histogram1D("Hodoscope Fore Particle Momentum", 460, 0.000, 4.600);
	private IHistogram1D foreParticleMomentumX = aida.histogram1D("Hodoscope Fore Particle x-Momentum", 200, 0.000, 0.100);
	private IHistogram1D foreParticleMomentumY = aida.histogram1D("Hodoscope Fore Particle y-Momentum", 200, 0.000, 0.100);
	private IHistogram1D foreParticleMomentumZ = aida.histogram1D("Hodoscope Fore Particle z-Momentum", 460, 0.000, 4.600);
	
	private IHistogram1D aftParticleMomentum = aida.histogram1D("Hodoscope Aft Particle Momentum", 460, 0.000, 4.600);
	private IHistogram1D aftParticleMomentumX = aida.histogram1D("Hodoscope Aft Particle x-Momentum", 200, 0.000, 0.100);
	private IHistogram1D aftParticleMomentumY = aida.histogram1D("Hodoscope Aft Particle y-Momentum", 200, 0.000, 0.100);
	private IHistogram1D aftParticleMomentumZ = aida.histogram1D("Hodoscope Aft Particle z-Momentum", 460, 0.000, 4.600);
	
	private IHistogram1D aftParticleReduxMomentum = aida.histogram1D("Hodoscope Aft Repeat Particle Momentum", 2*460, -4.600, 4.600);
	private IHistogram1D aftParticleReduxMomentumX = aida.histogram1D("Hodoscope Aft Repeat Particle x-Momentum", 400, -0.100, 0.100);
	private IHistogram1D aftParticleReduxMomentumY = aida.histogram1D("Hodoscope Aft Repeat Particle y-Momentum", 400, -0.100, 0.100);
	private IHistogram1D aftParticleReduxMomentumZ = aida.histogram1D("Hodoscope Aft Repeat Particle z-Momentum", 2*460, -4.600, 4.600);
	
	private IHistogram1D[] originDecayPositionZ = new IHistogram1D[3];
	private IHistogram2D[] originDecayPositionYZ = new IHistogram2D[3];
	private IHistogram2D[] originDecayPositionXZ = new IHistogram2D[3];
	
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
	
	private IHistogram2D[][] electronHodoscopePosition = new IHistogram2D[3][3];
	private IHistogram2D[][] positronHodoscopePosition = new IHistogram2D[3][3];
	private IHistogram2D[][] chargedHodoscopePosition = new IHistogram2D[3][3];
	
	private IHistogram2D[][] electronEcalPosition = new IHistogram2D[3][3];
	private IHistogram2D[][] positronEcalPosition = new IHistogram2D[3][3];
	private IHistogram2D[][] particleEcalPosition = new IHistogram2D[3][3];
	
	private IHistogram1D electronInitParticleMomentum = aida.histogram1D("Electron Initial Momentum", 250, 0, 2.5);
	private IHistogram1D positronInitParticleMomentum = aida.histogram1D("Positron Initial Momentum", 250, 0, 2.5);
	
	private IHistogram1D[] electronHodoParticleMomentum = new IHistogram1D[3];
	private IHistogram1D[] positronHodoParticleMomentum = new IHistogram1D[3];
	private IHistogram1D[] chargedHodoParticleMomentum = new IHistogram1D[3];
	private IHistogram1D[] electronEcalParticleMomentum = new IHistogram1D[3];
	private IHistogram1D[] positronEcalParticleMomentum = new IHistogram1D[3];
	private IHistogram1D[] particleEcalParticleMomentum = new IHistogram1D[3];
	
	private IHistogram2D[][] originEcalPositronPositionXZ = new IHistogram2D[3][3];
	private IHistogram2D[][] originEcalElectronPositionXZ = new IHistogram2D[3][3];
	private IHistogram2D[][] originEcalPositronOriginPosition = new IHistogram2D[3][3];
	
	private IHistogram1D[] initPhotonTheta = new IHistogram1D[3];
	private IHistogram2D initPhotonThetaVSEnergy = aida.histogram2D("Initial Photon Momentum vs. Theta",
			3000, 0.0, 30.0, 260*5, 0.000, 2.600);

	private IHistogram2D positronEnergyTDiff = aida.histogram2D("Positron Energy vs. Dt", 460, 0.000, 4.600, 5000, 0, 5);
	private IHistogram2D positronEnergyXDiff = aida.histogram2D("Positron Energy vs. Dx", 460, 0.000, 4.600, 250, -100, 100);
	private IHistogram2D positronEnergyYDiff = aida.histogram2D("Positron Energy vs. Dy", 460, 0.000, 4.600, 250, -100, 100);
	private IHistogram2D electronEnergyXDiff = aida.histogram2D("Electron Energy vs. Dx", 460, 0.000, 4.600, 250, -100, 100);
	private IHistogram2D electronEnergyYDiff = aida.histogram2D("Electron Energy vs. Dy", 460, 0.000, 4.600, 250, -100, 100);
	
	/*
	private IHistogram1D[] positronTDiff = new IHistogram1D[3];
	private IHistogram2D[] positronXDiff = new IHistogram2D[3];
	private IHistogram2D[] positronYDiff = new IHistogram2D[3];
	private IHistogram1D[] electronTDiff = new IHistogram1D[3];
	private IHistogram2D[] electronXDiff = new IHistogram2D[3];
	private IHistogram2D[] electronYDiff = new IHistogram2D[3];
	*/
	
	private IHistogram1D positronTDiff = aida.histogram1D("Positron Hodoscope-Calorimeter Time Coincidence", 5000, 0, 5);
	private IHistogram2D positronTDiff2D = aida.histogram2D("Positron Hodoscope t vs. Calorimeter t", 5000, 0, 5, 5000, 0, 5);
	private IHistogram2D positronXDiff = aida.histogram2D("Positron Hodoscope x vs. Calorimeter x", 800, -400, 400, 800, -400, 400);
	private IHistogram2D positronYDiff = aida.histogram2D("Positron Hodoscope y vs. Calorimeter y", 400, -200, 200, 400, -200, 200);
	private IHistogram1D electronTDiff = aida.histogram1D("Electron Hodoscope-Calorimeter Time Coincidence", 500, 0, 5);
	private IHistogram2D electronXDiff = aida.histogram2D("Electron Hodoscope x vs. Calorimeter x", 800, -400, 400, 800, -400, 400);
	private IHistogram2D electronYDiff = aida.histogram2D("Electron Hodoscope y vs. Calorimeter y", 400, -200, 200, 400, -200, 200);
	
	private Map<Integer, Double> countMap = new HashMap<Integer, Double>();
	private int ecalHitsSeen = 0;
	private int hodoHitsSeen = 0;
	private int hodoAftHitsSeen = 0;
	private int hodoForeHitsSeen = 0;
	
	private int totalEvents = 0;
	private int forwardParticles = 0;
	private int backwardParticles = 0;
	private int chargedParticles = 0;
	private int chargedParticlesNoGap = 0;
	
	private final Map<Integer, String> particleIDNameMap = new HashMap<Integer, String>();
	
	@Override
	public void startOfData() {
		// Map particle data group ID to particle names.
		particleIDNameMap.put(211, "Pion+");
		particleIDNameMap.put(-211, "Pion-");
		particleIDNameMap.put(2112, "Neutron");
		particleIDNameMap.put(2212, "Proton");
		particleIDNameMap.put(22,   "Photon");
		particleIDNameMap.put(11,   "Electron");
		particleIDNameMap.put(-11,  "Positron");
		particleIDNameMap.put(12,   "Neutrino");
		particleIDNameMap.put(111,  "Pion0");
		particleIDNameMap.put(13,   "Muon");
		particleIDNameMap.put(-13,  "Anti-Muon");
		particleIDNameMap.put(14,   "Mu Neutrino");
		particleIDNameMap.put(-14,  "Anti-Mu Neutrino");
		
		// Generate calorimeter and hodoscope distributions for all
		// particles, particles outside the vacuum box "hot-zone," and
		// particles inside the vacuum box "hot-zone."
		String[] suffix = { "", " [VBO]", " [VBE]" };
		String[] energy = { "", " (|p| > 1 MeV)", " (|p| > 200 MeV)" };
		for(int i = 0; i < 3; i++) {
			// Process energy-dependent plots.
			for(int j = 0; j < 3; j++) {
				// Define the hodoscope position plots.
				electronHodoscopePosition[i][j] = aida.histogram2D("Electron Hodoscope Position" + energy[j] + suffix[i],
						900, -450, 450, 300, -150, 150);
				positronHodoscopePosition[i][j] = aida.histogram2D("Positron Hodoscope Position" + energy[j] + suffix[i],
						900, -450, 450, 300, -150, 150);
				chargedHodoscopePosition[i][j] = aida.histogram2D("Charged Particle Hodoscope Position" + energy[j] + suffix[i],
						900, -450, 450, 300, -150, 150);
				
				// Define the calorimeter position plots.
				electronEcalPosition[i][j] = aida.histogram2D("Electron Calorimeter Position" + energy[j] + suffix[i],
						900, -450, 450, 300, -150, 150);
				positronEcalPosition[i][j] = aida.histogram2D("Positron Calorimeter Position" + energy[j] + suffix[i],
						900, -450, 450, 300, -150, 150);
				particleEcalPosition[i][j] = aida.histogram2D("All Particle Calorimeter Position" + energy[j] + suffix[i],
						900, -450, 450, 300, -150, 150);
				
				// Originating particle distributions.
				originEcalPositronPositionXZ[i][j] = aida.histogram2D("Positron Calorimeter x-Position vs. z-Position" + energy[j]
						+ suffix[i], 240, -600, 600, 320, 0.000, 1600);
				originEcalElectronPositionXZ[i][j] = aida.histogram2D("Electron Calorimeter x-Position vs. z-Position" + energy[j]
						+ suffix[i], 240, -600, 600, 320, 0.000, 1600);
				originEcalPositronOriginPosition[i][j] = aida.histogram2D("Positron x-Origin vs. Calorimeter x-Position" + energy[j]
						+ suffix[i], 320, 0.000, 1600, 240, -600, 600);
			}
			
			// Define particle momentum plots.
			electronHodoParticleMomentum[i] = aida.histogram1D("Electron Hodoscope Momentum" + suffix[i], 250, 0, 2.5);
			positronHodoParticleMomentum[i] = aida.histogram1D("Positron Hodoscope Momentum" + suffix[i], 250, 0, 2.5);
			chargedHodoParticleMomentum[i] = aida.histogram1D("Charged Particle Hodoscope Momentum" + suffix[i], 250, 0, 2.5);
			electronEcalParticleMomentum[i] = aida.histogram1D("Electron Calorimeter Momentum" + suffix[i], 250, 0, 2.5);
			positronEcalParticleMomentum[i] = aida.histogram1D("Positron Calorimeter Momentum" + suffix[i], 250, 0, 2.5);
			particleEcalParticleMomentum[i] = aida.histogram1D("All Particle Calorimeter Momentum" + suffix[i], 250, 0, 2.5);
			
			// ENERGY-SEPARATED PLOTS -- i represents energy here, not
			// vacuum box exclusion.
			
			// Define initial particle angle.
			initPhotonTheta[i] = aida.histogram1D("Initial Photon Theta" + energy[i], 3000, 0.0, 30.0);
			
			// Define initial particle decay origins.
			originDecayPositionZ[i] = aida.histogram1D("Secondary Particle Decay z-Position" + energy[i], 320, 0.000, 1600);
			originDecayPositionYZ [i]= aida.histogram2D("Secondary Particle Decay y-Position vs. z-Position" + energy[i],
					160, -400, 400, 320, 0.000, 1600);
			originDecayPositionXZ[i] = aida.histogram2D("Secondary Particle Decay x-Position vs. z-Position" + energy[i],
					240, -600, 600, 320, 0.000, 1600);
		}
	}
	
	@Override
	public void process(EventHeader event) {
		// Get collections.
		List<MCParticle> particles = getCollection(event, "MCParticle", MCParticle.class);
		List<SimTrackerHit> ecalHits = getCollection(event, "TrackerHitsECal", SimTrackerHit.class);
		List<SimTrackerHit> hodoscopeHits = getCollection(event, "HodoscopeHits", SimTrackerHit.class);
		List<SimTrackerHit> hodoscopeAftHits = getCollection(event, "HodoscopeAftHits", SimTrackerHit.class);
		List<SimTrackerHit> hodoscopeForeHits = getCollection(event, "HodoscopeForeHits", SimTrackerHit.class);
		
		// Track number of entries to each collection.
		ecalHitsSeen += ecalHits.size();
		hodoHitsSeen += hodoscopeHits.size();
		hodoAftHitsSeen += hodoscopeAftHits.size();
		hodoForeHitsSeen += hodoscopeForeHits.size();
		
		// Test for Monte Carlo particles in the SVT.
		List<SimTrackerHit> svtHits = event.get(SimTrackerHit.class, "TrackerHits");
		for(SimTrackerHit svtHit : svtHits) {
			boolean hasParticle = (svtHit.getMCParticle() != null);
			System.out.println("Has MC Particle: " + hasParticle);
			System.out.println("Layer: " + svtHit.getLayer());
			System.out.println("Layer Number: " + svtHit.getLayerNumber());
			System.out.println("\n\n");
		}
		
		// ==== Calorimeter Scoring Plane Plots =========================================
		// ==============================================================================
		
		// Plot the distribution on the calorimeter face.
		for(SimTrackerHit ecalHit : ecalHits) {
			// Calculate general particle coordinates.
			double ox = ecalHit.getMCParticle().getOriginX();
			double oz = ecalHit.getMCParticle().getOriginZ();
			double magP = getMagnitude(ecalHit.getMomentum());
			boolean[] inVaccumBoxExclusionZone = {
					true,
					(oz > 300 && oz < 1375) && (ox > -400 && ox < -150) && oz < 1340,
					!((oz > 300 && oz < 1375) && (ox > -400 && ox < -150) && oz < 1340)
			};
			boolean[] meetsPThreshold = {
					true,
					(magP >= 0.001),
					(magP >= 0.200)
			};
			
			// Process plots for each vacuum box exclusion zone status.
			for(int i = 0; i < 3; i++) {
				if(!inVaccumBoxExclusionZone[i]) { continue; }
				
				// Process plots for each momentum threshold.
				for(int j = 0; j < 3; j++) {
					// Populate all particle plots.
					particleEcalParticleMomentum[i].fill(magP);
					
					// Process plots with momentum cuts as needed.
					if(meetsPThreshold[j]) {
						particleEcalPosition[i][j].fill(ecalHit.getPosition()[0], ecalHit.getPosition()[1]);
					}
					
					// Populate electron plots.
					if(ecalHit.getMCParticle().getPDGID() == ELECTRON) {
						// Process plots that do not have momentum cuts.
						electronEcalParticleMomentum[i].fill(magP);
						
						// Process plots with momentum cuts as needed.
						if(meetsPThreshold[j]) {
							electronEcalPosition[i][j].fill(ecalHit.getPosition()[0], ecalHit.getPosition()[1]);
							if(ecalHit.getMCParticle().getProductionTime() != 0) {
								originEcalElectronPositionXZ[i][j].fill(
										ecalHit.getMCParticle().getOriginX(),
										ecalHit.getMCParticle().getOriginZ()
								);
							}
						}
					}
					
					// Populate positron plots.
					else if(ecalHit.getMCParticle().getPDGID() == POSITRON) {
						// Process plots that do not have momentum cuts.
						positronEcalParticleMomentum[i].fill(magP);
						
						// Process plots with momentum cuts as needed.
						if(meetsPThreshold[j]) {
							positronEcalPosition[i][j].fill(ecalHit.getPosition()[0], ecalHit.getPosition()[1]);
							if(ecalHit.getMCParticle().getProductionTime() != 0) {
								originEcalPositronPositionXZ[i][j].fill(
										ecalHit.getMCParticle().getOriginX(),
										ecalHit.getMCParticle().getOriginZ()
								);
								originEcalPositronOriginPosition[i][j].fill(ecalHit.getMCParticle().getOriginZ(), ecalHit.getPosition()[0]);
							}
						}
					}
				}
			}
		}
		
		// ==== Initial Particle and General Decay Plots ================================
		// ==============================================================================
		
		// Plot information concerning all particles.
		for(MCParticle particle : particles) {
			// Plot the decay positions for particles. These should
			// only include non-initial-state particles.
			if(particle.getProductionTime() != 0) {
				// Fill particle decay position plots.
				boolean[] thresh = { true, particle.getMomentum().magnitude() > 0.001, particle.getMomentum().magnitude() > 0.200 };
				for(int i = 0; i < 3; i++) {
					if(thresh[i]) {
						originDecayPositionXZ[i].fill(particle.getOriginX(), particle.getOriginZ());
						originDecayPositionYZ[i].fill(particle.getOriginY(), particle.getOriginZ());
						originDecayPositionZ[i].fill(particle.getOriginZ());
					}
				}
				
				// Plot the production angle of initial photons.
				if(particle.getPDGID() == PHOTON) {
					// Production angle is p*<sin(0.03), 0, cos(0.03)> == |p|cos(θ).
					double angle = Math.acos(dotProduct(particle.getMomentum(), BEAM_ANGLE) / particle.getMomentum().magnitude());
					initPhotonTheta[ALL_PARTICLES].fill(angle);
					if(thresh[1]) { initPhotonTheta[P_GE_1MEV].fill(angle); }
					if(thresh[2]) { initPhotonTheta[P_GE_200MEV].fill(angle); }
					initPhotonThetaVSEnergy.fill(angle, particle.getMomentum().magnitude());
				}
			}
			
			// Plot information for initial state particles.
			else {
				initParticleMomentum.fill(particle.getMomentum().magnitude());
				initParticleMomentumX.fill(particle.getMomentum().x());
				initParticleMomentumY.fill(particle.getMomentum().y());
				initParticleMomentumZ.fill(particle.getMomentum().z());
				if(particle.getPDGID() == POSITRON) {
					positronInitParticleMomentum.fill(particle.getMomentum().magnitude());
				} else if(particle.getPDGID() == ELECTRON) {
					electronInitParticleMomentum.fill(particle.getMomentum().magnitude());
				}
			}
		}
		
		// ==== Hodoscope Scoring Plane Plots ===========================================
		// ==============================================================================
		
		// Map the particles that pass through the calorimeter scoring
		// plane to their scoring plane hit object.
		Map<MCParticle, SimTrackerHit> ecalHitMap = new HashMap<MCParticle, SimTrackerHit>();
		for(SimTrackerHit ecalHit : ecalHits) {
			ecalHitMap.put(ecalHit.getMCParticle(), ecalHit);
		}
		
		// Track the number of particles of each type that are seen.
		for(SimTrackerHit foreHit : hodoscopeForeHits) {
			// Check the vacuum box exclusion zone.
			double ox = foreHit.getMCParticle().getOriginX();
			double oz = foreHit.getMCParticle().getOriginZ();
			boolean[] thresh = {
					true,
					foreHit.getMCParticle().getMomentum().magnitude() > 0.001,
					foreHit.getMCParticle().getMomentum().magnitude() > 0.200
			};
			boolean[] inVaccumBoxExclusionZone = {
					true,
					(oz > 300 && oz < 1375) && (ox > -400 && ox < -150) && oz < 1340,
					!((oz > 300 && oz < 1375) && (ox > -400 && ox < -150) && oz < 1340)
			};
			
			// Count the number of charged particles.
			if(foreHit.getMCParticle().getCharge() != 0) {
				chargedParticles++;
				if(Math.abs(foreHit.getPosition()[1]) > 17.295) { chargedParticlesNoGap++; }
			}
			
			// Populate vacuum box plots as appropriate.
			for(int i = 0; i < 3; i++) {
				if(!inVaccumBoxExclusionZone[i]) { continue; }
				
				// Plot the distribution of all charged particles over 1 MeV.
				if(foreHit.getMCParticle().getCharge() != 0) {
					chargedHodoParticleMomentum[i].fill(getMagnitude(foreHit.getMomentum()));
					chargedHodoscopePosition[i][ALL_PARTICLES].fill(foreHit.getPosition()[0], foreHit.getPosition()[1]);
					if(thresh[1]) {
						chargedHodoscopePosition[i][1].fill(foreHit.getPosition()[0], foreHit.getPosition()[1]);
					}
					if(thresh[2]) {
						chargedHodoscopePosition[i][2].fill(foreHit.getPosition()[0], foreHit.getPosition()[1]);
					}
				}
				
				// Plot electron and positron positions.
				if(foreHit.getMCParticle().getPDGID() == ELECTRON) {
					electronHodoscopePosition[i][ALL_PARTICLES].fill(foreHit.getPosition()[0], foreHit.getPosition()[1]);
					electronHodoParticleMomentum[i].fill(getMagnitude(foreHit.getMomentum()));
					if(thresh[1]) { electronHodoscopePosition[i][1].fill(foreHit.getPosition()[0], foreHit.getPosition()[1]); }
					if(thresh[2]) { electronHodoscopePosition[i][2].fill(foreHit.getPosition()[0], foreHit.getPosition()[1]); }
				} else if(foreHit.getMCParticle().getPDGID() == POSITRON) {
					positronHodoscopePosition[i][ALL_PARTICLES].fill(foreHit.getPosition()[0], foreHit.getPosition()[1]);
					if(thresh[1]) { positronHodoscopePosition[i][1].fill(foreHit.getPosition()[0], foreHit.getPosition()[1]); }
					if(thresh[2]) { positronHodoscopePosition[i][2].fill(foreHit.getPosition()[0], foreHit.getPosition()[1]); }
					positronHodoParticleMomentum[i].fill(getMagnitude(foreHit.getMomentum()));
				}
			}
			
			// Track the number of particles by type.
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
						positronEnergyTDiff.fill(getMagnitude(foreHit.getMomentum()), Math.abs(ecalHit.getTime() - foreHit.getTime()));
						positronTDiff.fill(Math.abs(ecalHit.getTime() - foreHit.getTime()));
						positronTDiff2D.fill(ecalHit.getTime(), foreHit.getTime());
						positronXDiff.fill(ecalHit.getPosition()[0], foreHit.getPosition()[0]);
						positronYDiff.fill(ecalHit.getPosition()[1], foreHit.getPosition()[1]);
						positronEnergyXDiff.fill(getMagnitude(foreHit.getMomentum()), foreHit.getPosition()[0] - ecalHit.getPosition()[0]);
						positronEnergyYDiff.fill(getMagnitude(foreHit.getMomentum()), foreHit.getPosition()[1] - ecalHit.getPosition()[1]);
					} else if(foreHit.getMCParticle().getPDGID() == ELECTRON) {
						electronTDiff.fill(Math.abs(ecalHit.getTime() - foreHit.getTime()));
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
		System.out.println("\n");
		double rate = 1.0 * chargedParticles / (totalEvents * Math.pow(10, -9)) / Math.pow(10, 3);
		double rateNoGap = 1.0 * chargedParticlesNoGap / (totalEvents * Math.pow(10, -9)) / Math.pow(10, 3);
		System.out.printf("Rate: %d particles / (2 ns * %d events) = %f kHz%n", chargedParticles, totalEvents, rate);
		System.out.printf("Rate (No Gap): %d particles / (2 ns * %d events) = %f kHz%n", chargedParticlesNoGap, totalEvents, rateNoGap);
		System.out.println("\nParticle Counts:");
		for(Entry<Integer, Double> entry : countMap.entrySet()) {
			System.out.printf("\t%-16s :: %.0f%n",
					particleIDNameMap.get(entry.getKey()) != null ? particleIDNameMap.get(entry.getKey()) : Integer.toString(entry.getKey()),
					entry.getValue());
		}
	}
	
	private static final double dotProduct(Hep3Vector v0, double[] v1) {
		if(v1.length != 3) {
			throw new IllegalArgumentException("Dot products may only be performed on vectors of equal length.");
		}
		
		return (v0.x() * v1[0]) + (v0.y() * v1[1]) + (v0.z() * v1[2]);
	}
	
	private static final double getMagnitude(double[] v) {
		double squareSum = 0;
		for(double vi : v) {
			squareSum += Math.pow(vi, 2);
		}
		return Math.sqrt(squareSum);
	}
	
	private static final <E> List<E> getCollection(EventHeader event, String collectionName, Class<E> type) {
		List<E> collection;
		if(event.hasCollection(type, collectionName)) {
			collection = event.get(type, collectionName);
		} else {
			collection = new java.util.ArrayList<E>(0);
		}
		return collection;
	}
}
