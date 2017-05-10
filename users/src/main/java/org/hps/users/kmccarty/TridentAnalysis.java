package org.hps.users.kmccarty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hps.recon.tracking.TrackUtils;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

public class TridentAnalysis extends Driver {
	private static final double CLUSTERED_N_SIGMA_MAX = 3;
	private static final double UNCLUSTERED_N_SIGMA_MIN = 5;
	private static final int TRACK_SHARED_HITS_MAX = 2;
	private static final double ELECTRON_FEE_THRESHOLD = 1.700;
	
	private final AIDA aida = AIDA.defaultInstance();
	
	// Define duplicate track analysis plots.
	private IHistogram1D trackChiSquared = aida.histogram1D("Trident Selection/Duplicate Elimination/Track #frac{#chi^{2}}{n_{DF}}",
			100, 0, 10);
	private IHistogram1D trackSharedHits = aida.histogram1D("Trident Selection/Duplicate Elimination/Track Shared Hits",
			11, -0.5, 10.5);
	private IHistogram1D trackMultiplicity = aida.histogram1D("Trident Selection/Duplicate Elimination/Track Multiplicity",
			11, -0.5, 10.5);
	private IHistogram1D trackChiSquaredFiltered = aida.histogram1D(
			"Trident Selection/Duplicate Elimination/Track #frac{#chi^2}{n_{DF}} (Filtered)",
			100, 0, 10);
	
	// Define cluster/track matching analysis plots.
	private static final String[] TYPE_NAMES = { "All ", "Fiducial ", "Edge " };
	private IHistogram1D[] matchingNSigma = new IHistogram1D[TYPE_NAMES.length];
	private IHistogram1D[] matchingDr = new IHistogram1D[TYPE_NAMES.length];
	private IHistogram2D[] matchingDxDy = new IHistogram2D[TYPE_NAMES.length];
	private IHistogram2D[] matchingClusterPosition = new IHistogram2D[TYPE_NAMES.length];
	private IHistogram2D[] matchingTrackPosition = new IHistogram2D[TYPE_NAMES.length];
	private IHistogram2D[] matchingDrNSigma = new IHistogram2D[TYPE_NAMES.length];
	
	// Define general particle plots.
	private IHistogram1D particleElectronMomentum = aida.histogram1D("Trident Selection/Particles/e^{-} Momentum", 2600, 0, 2.6);
	private IHistogram1D particlePositronMomentum = aida.histogram1D("Trident Selection/Particles/e^{+} Momentum", 2600, 0, 2.6);
	private IHistogram2D particleElectronPosition = aida.histogram2D("Trident Selection/Particles/e^{-} Track Position",
			800, -400, 400, 200, -100, 100);
	private IHistogram2D particlePositronPosition = aida.histogram2D("Trident Selection/Particles/e^{+} Track Position",
			800, -400, 400, 200, -100, 100);
	private IHistogram1D particleElectronXPosition = aida.histogram1D("Trident Selection/Particles/e^{-} x-Track Position",
			800, -400, 400);
	private IHistogram1D particlePositronXPosition = aida.histogram1D("Trident Selection/Particles/e^{+} Track x-Position",
			800, -400, 400);
	
	// Define trident properties plots.
	private IHistogram1D tridentElectronMomentum = aida.histogram1D("Trident Selection/Tridents/Trident e^{-} Momentum", 2600, 0, 2.6);
	private IHistogram1D tridentPositronMomentum = aida.histogram1D("Trident Selection/Tridents/Trident e^{+} Momentum", 2600, 0, 2.6);
	private IHistogram1D tridentMomentumSum = aida.histogram1D("Trident Selection/Tridents/Trident Momentum Sum", 2600, 0, 2.6);
	private IHistogram2D tridentElectronPosition = aida.histogram2D("Trident Selection/Tridents/Trident e^{-} Track Position",
			800, -400, 400, 200, -100, 100);
	private IHistogram2D tridentPositronPosition = aida.histogram2D("Trident Selection/Tridents/Trident e^{+} Track Position",
			800, -400, 400, 200, -100, 100);
	private IHistogram1D tridentElectronXPosition = aida.histogram1D("Trident Selection/Tridents/Trident e^{-} x-Track Position",
			800, -400, 400);
	private IHistogram1D tridentPositronXPosition = aida.histogram1D("Trident Selection/Tridents/Trident e^{+} Track x-Position",
			800, -400, 400);
	private IHistogram1D tridentTimeCoincidence = aida.histogram1D("Trident Selection/Tridents/Trident Time Coincidence", 800, -10, 10);
	private IHistogram1D tridentInvariantMass = aida.histogram1D("Trident Selection/Tridents/Trident Invariant Mass", 1000, 0, 0.100);
	
	@Override
	public void startOfData() {
		final String dir = "Trident Selection/Cluster-Track Matching/";
		for(int i = 0; i < TYPE_NAMES.length; i++) {
			matchingNSigma[i] = aida.histogram1D(dir + TYPE_NAMES[i] + "n_{#sigma}",
					300, 0, 30);
			matchingDr[i] = aida.histogram1D(dir + TYPE_NAMES[i] + "#Deltar",
					300, 0, 30);
			matchingDxDy[i] = aida.histogram2D(dir + TYPE_NAMES[i] + "Cluster-Track #Deltay vs. #Deltax",
					400, -20, 20, 300, -15, 15);
			matchingClusterPosition[i] = aida.histogram2D(dir + TYPE_NAMES[i] + "Cluster Position",
					800, -400, 400, 200, -100, 100);
			matchingTrackPosition[i] = aida.histogram2D(dir + TYPE_NAMES[i] + "Track Position",
					800, -400, 400, 200, -100, 100);
			matchingDrNSigma[i] = aida.histogram2D(dir + TYPE_NAMES[i] + "n_{#sigma} vs. #Deltar",
					300, 0, 30, 200, 0, 20);
		}
	}
	
	@Override
	public void process(EventHeader event) {
		// Get the particle selection, if it exists.
		List<ReconstructedParticle> particles = null;
		if(event.hasCollection(ReconstructedParticle.class, "FinalStateParticles")) {
			particles = event.get(ReconstructedParticle.class, "FinalStateParticles");
		} else { particles = new ArrayList<ReconstructedParticle>(); }
		
		
		
		// ==== Remove Duplicate Tracks ===========================================================
		// ========================================================================================
		
		// Duplicate particles must be removed. A duplicate particle
		// is defined as any particle that shares three or more hits
		// with any other particle. Only the particle that has the
		// best χ² values should be retained.
		List<Integer> duplicates = new ArrayList<Integer>();
		boolean[] wasRemoved = new boolean[particles.size()];
		Set<TrackerHit> sharedTrackerHitSet = new HashSet<TrackerHit>();
		List<ReconstructedParticle> filteredParticles = new ArrayList<ReconstructedParticle>();
		
		// Iterate over all particles. The particle from the first
		// loop is the "main" particle to which the others are to be
		// compared.
		mainParticleLoop:
		for(int i = 0; i < particles.size(); i++) {
			// Particles that do not have tracks are not analyzable.
			if(!hasTracks(particles.get(i))) {
				continue mainParticleLoop;
			}
			
			// Record the χ²/n_DF for each particle.
			trackChiSquared.fill(getChiSquaredNDF(particles.get(i)));
			
			// Particles that are flagged for removal should be
			// skipped.
			if(wasRemoved[i]) {
				continue mainParticleLoop;
			}
			
			// The main particle automatically shares more hits than
			// the threshold with itself.
			duplicates.add(i);
			
			// Store a set of the main particle's tracker hits for
			// comparison with other particles.
			for(TrackerHit hit : getTrackerHits(particles.get(i))) {
				sharedTrackerHitSet.add(hit);
			}
			
			// Iterate over the remaining particles. Particles before
			// the "main" particle have already been considered and
			// may be skipped without loss.
			compareParticleLoop:
			for(int j = i + 1; j < particles.size(); j++) {
				// Particles that are flagged for removal or that do
				// not define tracks must be skipped.
				if(wasRemoved[j] || !hasTracks(particles.get(j))) {
					continue compareParticleLoop;
				}
				
				// Compare the particle's hits with the main hits of
				// the main particle.
				int sharedHits = 0;
				for(TrackerHit hit : getTrackerHits(particles.get(j))) {
					if(sharedTrackerHitSet.contains(hit)) {
						sharedHits++;
					}
				}
				
				// Check how many hits are shared. If this exceeds
				// the value set by TRACK_SHARED_HITS_MAX, it is to
				// be considered a duplicate.
				if(sharedHits > TRACK_SHARED_HITS_MAX) {
					duplicates.add(j);
					trackSharedHits.fill(sharedHits);
				}
			} // end compareParticleLoop
			
			// Plot the number of duplicate tracks.
			trackMultiplicity.fill(duplicates.size());
			
			// If only one particle is in the duplicates list, then
			// the particle does not share enough hits with any other
			// particle and may be retained.
			if(duplicates.size() == 1) {
				filteredParticles.add(particles.get(i));
				trackChiSquaredFiltered.fill(getChiSquaredNDF(particles.get(i)));
			}
			
			// If more than one particle is in the list, select the
			// particle with the lowest χ²/n_DF. All of the remaining
			// particles should be marked as "removed."
			else {
				// Store the main particle as the initial best fit
				// track and calculate its χ²/n_DF.
				int bestParticle = i;
				double bestX2 = getChiSquaredNDF(particles.get(bestParticle));
				
				// Iterate over the remaining particles and compare
				// them to the current best particle.
				for(int k = 1; k < duplicates.size(); k++) {
					// Duplicate candidates can not match with any
					// other particles (as all possible candidates
					// are already selected) and should be flagged as
					// "removed" to avoid unnecessary comparisons in
					// subsequent main particle iterations. This is
					// not needed for the main particle, as it is not
					// considered a second time.
					wasRemoved[duplicates.get(k)] = true;
					
					// Calculate the comparison χ²/n_DF.
					double compX2 = getChiSquaredNDF(particles.get(duplicates.get(k)));
					
					// If the new particle has a lower χ²/n_DF, it is
					// the new best particle. Update the pointers for
					// the best particle.
					if(compX2 < bestX2) {
						bestParticle = duplicates.get(k);
						bestX2 = compX2;
					}
				}
				
				// The best particle at the end of the comparison is
				// the one to keep.
				filteredParticles.add(particles.get(bestParticle));
				trackChiSquaredFiltered.fill(bestX2);
			}
			
			// Clear the storage objects.
			duplicates.clear();
			sharedTrackerHitSet.clear();
		} // end mainParticleLoop
		
		
		
		// ==== Separate Clustered Tracks =========================================================
		// ========================================================================================
		
		// Generate analysis plots for particles based on the track
		// and cluster positioning.
		clusteringLoop:
		for(ReconstructedParticle particle : filteredParticles) {
			// These plots do not apply to tracks without clusters.
			if(!hasClusters(particle)) {
				continue clusteringLoop;
			}
			
			// Get the particle nσ and populate the nσ distribution.
			double nSigma = particle.getGoodnessOfPID();
			double[] clusterPosition = getCluster(particle).getPosition();
			Hep3Vector trackPosition = TrackUtils.getTrackPositionAtEcal(getTrack(particle));
			double dx = trackPosition.x() - clusterPosition[0];
			double dy = trackPosition.y() - clusterPosition[1];
			double dr = Math.sqrt((dx * dx) + (dy * dy));
			int[] indices = { 0, TriggerModule.inFiducialRegion(getCluster(particle)) ? 1 : 2 };
			for(int index : indices) {
				matchingNSigma[index].fill(nSigma);
				matchingDr[index].fill(dr);
				matchingDrNSigma[index].fill(dr, nSigma);
				matchingTrackPosition[index].fill(trackPosition.x(), trackPosition.y());
				matchingClusterPosition[index].fill(clusterPosition[0], clusterPosition[1]);
				matchingDxDy[index].fill(dx, dy);
			}
		}
		
		
		
		// ==== Get Positrons and Electrons =======================================================
		// ========================================================================================
		
		List<ReconstructedParticle> positrons = new ArrayList<ReconstructedParticle>();
		List<ReconstructedParticle> electrons = new ArrayList<ReconstructedParticle>();
		for(ReconstructedParticle particle : filteredParticles) {
			if(getChiSquaredNDF(particle) > 1.5) {
				continue;
			}
			
			if(particle.getCharge() > 0) {
				positrons.add(particle);
			} else if(particle.getCharge() < 0) {
				electrons.add(particle);
			}
		}
		
		
		
		// ==== Form Trident Pairs ================================================================
		// ========================================================================================
		
		for(ReconstructedParticle positron : positrons) {
			// Get the positron position.
			Hep3Vector positronPosition = TrackUtils.getTrackPositionAtEcal(getTrack(positron));
			
			// Fill the general particle plots.
			particlePositronXPosition.fill(positronPosition.x());
			particlePositronMomentum.fill(positron.getMomentum().magnitude());
			particlePositronPosition.fill(positronPosition.x(), positronPosition.y());
			
			for(ReconstructedParticle electron : electrons) {
				// Get the electron position.
				Hep3Vector electronPosition = TrackUtils.getTrackPositionAtEcal(getTrack(electron));
				
				// Fill the general electron plots.
				particleElectronXPosition.fill(electronPosition.x());
				particleElectronMomentum.fill(electron.getMomentum().magnitude());
				particleElectronPosition.fill(electronPosition.x(), electronPosition.y());
				
				// Require that there exist a top and bottom track.
				if(!((positronPosition.y() > 0 && electronPosition.y() < 0)
						|| (positronPosition.y() < 0 && electronPosition.y() > 0))) {
					continue;
				}
				
				// Eliminate FEE electrons.
				if(electron.getMomentum().magnitude() >= ELECTRON_FEE_THRESHOLD) {
					continue;
				}
				
				// Apply a time coincidence cut.
				if(Math.abs(getTrackTime(positron, event) - getTrackTime(electron, event)) > 3) {
					continue;
				}
				
				tridentPositronXPosition.fill(positronPosition.x());
				tridentElectronXPosition.fill(electronPosition.x());
				tridentPositronMomentum.fill(positron.getMomentum().magnitude());
				tridentElectronMomentum.fill(electron.getMomentum().magnitude());
				tridentPositronPosition.fill(positronPosition.x(), positronPosition.y());
				tridentElectronPosition.fill(electronPosition.x(), electronPosition.y());
				tridentMomentumSum.fill(VecOp.add(positron.getMomentum(), electron.getMomentum()).magnitude());
				tridentTimeCoincidence.fill(getTrackTime(positron, event) - getTrackTime(electron, event));
				tridentInvariantMass.fill(getInvariantMass(electron, positron));
			}
		}
	}
	
	private static final double getInvariantMass(ReconstructedParticle electron, ReconstructedParticle positron) {
		// (E1 + E2)² - |p1 + p2|²
		return Math.pow(electron.getMomentum().magnitude() + positron.getMomentum().magnitude(), 2)
				- VecOp.add(electron.getMomentum(), positron.getMomentum()).magnitudeSquared();
	}
	
	private static final double getTrackTime(ReconstructedParticle particle, EventHeader event) {
		return TrackUtils.getTrackTime(getTrack(particle), TrackUtils.getHitToStripsTable(event), TrackUtils.getHitToRotatedTable(event));
	}
	
	private static final double getChiSquaredNDF(ReconstructedParticle particle) {
		if(particle.getTracks().isEmpty()) {
			throw new NullPointerException("Argument particle does not define a track!");
		} else {
			return particle.getTracks().get(0).getChi2() / getTrackerHits(particle).size();
		}
	}
	
	private static final boolean hasTracks(ReconstructedParticle particle) {
		return !particle.getTracks().isEmpty();
	}
	
	private static final boolean hasClusters(ReconstructedParticle particle) {
		return !particle.getClusters().isEmpty();
	}
	
	private static final List<TrackerHit> getTrackerHits(ReconstructedParticle particle) {
		if(particle.getTracks().isEmpty()) {
			throw new NullPointerException("Argument particle does not define a track!");
		} else {
			return particle.getTracks().get(0).getTrackerHits();
		}
	}
	
	private static final Track getTrack(ReconstructedParticle particle) {
		if(particle.getTracks().isEmpty()) {
			throw new NullPointerException("Argument particle does not define a track!");
		} else {
			return particle.getTracks().get(0);
		}
	}
	
	private static final Cluster getCluster(ReconstructedParticle particle) {
		if(particle.getClusters().isEmpty()) {
			throw new NullPointerException("Argument particle does not define a cluster!");
		} else {
			return particle.getClusters().get(0);
		}
	}
}