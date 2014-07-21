package org.hps.users.omoreno;

import hep.aida.IPlotter;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.util.ArrayList;
import java.util.List;

import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.base.BaseTrack;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver; 
import org.lcsim.util.aida.AIDA;

/**
 * Analysis driver used to make plots of extrapolation residuals.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id$
 *
 */
public class ExtrapolationAnalysis  extends Driver {

	AIDA aida = null;
	List<IPlotter> plotters;

	Hep3Vector bField = null;
	
	boolean verbose = false;
	
	// Collection Names
	String matchedEcalScoringPlaneHitsCollectionName = "MatchedTrackerHitsEcal";
	String trackToScoringPlaneHitRelationsName = "TrackToEcalScoringPlaneHitRelations";
	String trackToMCParticleRelationsName = "TrackToMCParticleRelations";
	
	/**
	 * Enable/disable verbose mode
	 * 
	 * @param verbose : true to enable, false otherwise
	 */
	public void setVerbose(boolean verbose){
		this.verbose = verbose; 
	}
	
	public void detectorChanged(Detector detector){
		
		// Get the magnetic field from the geometry
		bField = detector.getFieldMap().getField(new BasicHep3Vector(0,0,0));
		
		//-----------------------//
		//--- Setup all plots ---//
		//-----------------------//
		
		// Setup AIDA
		aida = AIDA.defaultInstance();
		aida.tree().cd("/");
	
		// Instantiate a list to hold the collection of plotters
		plotters = new ArrayList<IPlotter>();
		IPlotter plotter = null; 
		
		//--- Plots of scoring plane positions ---//
		//----------------------------------------//
		plotter = PlotUtils.setupPlotter("Positions of Scoring plane hits matched to tracks", 2, 2);
		PlotUtils.setup1DRegion(plotter, "Scoring plane hit position - x", 0, "x (mm)",
				aida.histogram1D("Scoring plane hit position - x", 100, -400, 400));
		PlotUtils.setup1DRegion(plotter, "Scoring plane hit position - y", 1, "y (mm)",
				aida.histogram1D("Scoring plane hit position - y", 100, -200, 200));
		PlotUtils.setup1DRegion(plotter, "Scoring plane hit position - z", 2, "z (mm)",
				aida.histogram1D("Scoring plane hit position - z", 100, 1000, 1500));
		PlotUtils.setup2DRegion(plotter, "Scoring plane hit position - x-y", 3, "x (mm)", "y (mm)", 
				aida.histogram2D("Scoring plane hit position - x-y", 100, -400, 400, 100, -200, 200));
		plotters.add(plotter);	

		//--- Plots of residuals at scoring plane ---//
		//-------------------------------------------//
		plotter = PlotUtils.setupPlotter("Residuals at scoring plane", 3, 2);
		PlotUtils.setup1DRegion(plotter, "Top tracks - Bend plane residuals at scoring plane", 0, "x_{ep} - x_{sp} (mm)",
				aida.histogram1D("Top electron tracks - Bend plane residual at scoring plane", 60, -30, 30));
		PlotUtils.setup1DRegion(plotter, "Top tracks - Bend plane residuals at scoring plane", 0, "x_{ep} - x_{sp} (mm)",
				aida.histogram1D("Top positron tracks - Bend plane residual at scoring plane", 60, -30, 30));
		PlotUtils.setup1DRegion(plotter, "Bottom tracks - Bend plane residuals at scoring plane", 1, "x_{ep} - x_{sp} (mm)",
				aida.histogram1D("Bottom electron tracks - Bend plane residuals at scoring plane", 60, -30, 30));
		PlotUtils.setup1DRegion(plotter, "Bottom tracks - Bend plane residuals at scoring plane", 1, "x_{ep} - x_{sp} (mm)",
				aida.histogram1D("Bottom positron tracks - Bend plane residuals at scoring plane", 60, -30, 30));
		PlotUtils.setup1DRegion(plotter, "Top tracks - Non-bend plane residuals at scoring plane", 2,"y_{ep} - y_{sp} (mm)", 
				aida.histogram1D("Top electron tracks - Non-bend plane residuals at scoring plane", 30, -15, 15));
		PlotUtils.setup1DRegion(plotter, "Top tracks - Non-bend plane residuals at scoring plane", 2,"y_{ep} - y_{sp} (mm)", 
				aida.histogram1D("Top positron tracks - Non-bend plane residuals at scoring plane", 30, -15, 15));
		PlotUtils.setup1DRegion(plotter, "Bottom tracks - Non-bend plane residuals at scoring plane", 3,"y_{ep} - y_{sp} (mm)", 
				aida.histogram1D("Bottom electron tracks - Non-bend plane residuals at scoring plane", 30, -15, 15));
		PlotUtils.setup1DRegion(plotter, "Bottom tracks - Non-bend plane residuals at scoring plane", 3,"y_{ep} - y_{sp} (mm)", 
				aida.histogram1D("Bottom positron tracks - Non-bend plane residuals at scoring plane", 30, -15, 15));
		PlotUtils.setup1DRegion(plotter, "Top tracks - z residuals at scoring plane", 4, "z_{ep} - z_{sp} (mm)",
				aida.histogram1D("Top electron tracks - z residuals at scoring plane", 10, -5, 5));
		PlotUtils.setup1DRegion(plotter, "Top tracks - z residuals at scoring plane", 4, "z_{ep} - z_{sp} (mm)",
				aida.histogram1D("Top positron tracks - z residuals at scoring plane", 10, -5, 5));
		PlotUtils.setup1DRegion(plotter, "Bottom tracks - z residuals at scoring plane", 5, "z_{ep} - z_{sp} (mm)",
				aida.histogram1D("Bottom electron tracks - z residuals at scoring plane", 10, -5, 5));
		PlotUtils.setup1DRegion(plotter, "Bottom tracks - z residuals at scoring plane", 5, "z_{ep} - z_{sp} (mm)",
				aida.histogram1D("Bottom positron tracks - z residuals at scoring plane", 10, -5, 5));
		plotters.add(plotter);	
		
		//--- Plots of residuals at target ---//
		//------------------------------------//
		plotter = PlotUtils.setupPlotter("Residuals at target", 3, 2);
		PlotUtils.setup1DRegion(plotter, "Top tracks - Bend plane residuals at target", 0, "x_{ep} - x_{t} (mm)",
				aida.histogram1D("Top electron tracks - Bend plane residual at target", 40, -4, 4));
		PlotUtils.setup1DRegion(plotter, "Top tracks - Bend plane residuals at target", 0, "x_{ep} - x_{t} (mm)",
				aida.histogram1D("Top positron tracks - Bend plane residual at target", 40, -4, 4));
		PlotUtils.setup1DRegion(plotter, "Bottom tracks - Bend plane residuals at target", 1, "x_{ep} - x_{t} (mm)",
				aida.histogram1D("Bottom electron tracks - Bend plane residuals at target", 40, -4, 4));
		PlotUtils.setup1DRegion(plotter, "Bottom tracks - Bend plane residuals at target", 1, "x_{ep} - x_{t} (mm)",
				aida.histogram1D("Bottom positron tracks - Bend plane residuals at target", 40, -4, 4));
		PlotUtils.setup1DRegion(plotter, "Top tracks - Non-bend plane residuals at target", 2, "y_{ep} - y_{t} (mm)",
				aida.histogram1D("Top electron tracks - Non-bend plane residuals at target", 20, -2, 2));
		PlotUtils.setup1DRegion(plotter, "Top tracks - Non-bend plane residuals at target", 2, "y_{ep} - y_{t} (mm)",
				aida.histogram1D("Top positron tracks - Non-bend plane residuals at target", 20, -2, 2));
		PlotUtils.setup1DRegion(plotter, "Bottom tracks - Non-bend plane residuals at target", 3, "y_{ep} - y_{t} (mm)",
				aida.histogram1D("Bottom electron tracks - Non-bend plane residuals at target", 20, -2, 2));
		PlotUtils.setup1DRegion(plotter, "Bottom tracks - Non-bend plane residuals at target", 3, "y_{ep} - y_{t} (mm)",
				aida.histogram1D("Bottom positron tracks - Non-bend plane residuals at target", 20, -2, 2));
		PlotUtils.setup1DRegion(plotter, "Top tracks - z residuals at target", 4, "z_{ep} - z_{t} (mm)",
				aida.histogram1D("Top electron tracks - z residuals at target", 50, -5, 5));
		PlotUtils.setup1DRegion(plotter, "Top tracks - z residuals at target", 4, "z_{ep} - z_{t} (mm)",
				aida.histogram1D("Top positron tracks - z residuals at target", 50, -5, 5));
		PlotUtils.setup1DRegion(plotter, "Bottom tracks - z residuals at target", 5, "z_{ep} - z_{t} (mm)",
				aida.histogram1D("Bottom electron tracks - z residuals at target", 50, -5, 5));
		PlotUtils.setup1DRegion(plotter, "Bottom tracks - z residuals at target", 5, "z_{ep} - z_{t} (mm)",
				aida.histogram1D("Bottom positron tracks - z residuals at target", 50, -5, 5));
		plotters.add(plotter);	
		
		//--- Plot of residuals at scoring plane vs momentum ---//
		//------------------------------------------------------//
		plotter = PlotUtils.setupPlotter("Residuals vs Momentum", 2, 2);
		PlotUtils.setup2DRegion(plotter, "Bend plane residuals vs momentum at scoring plane", 0, 
				"Momentum (GeV)", "x_{ep} - x_{sp} (mm)",
				aida.histogram2D("Bend plane residuals vs momentum at scoring plane", 5, 0, 2.5, 60, -30, 30));
		PlotUtils.setup2DRegion(plotter, "Non-bend plane residuals vs momentum at scoring plane", 1, 
				"Momentum (GeV)", "y_{ep} - y_{sp} (mm)",
				aida.histogram2D("Non-bend plane residuals vs momentum at scoring plane", 5, 0, 2.5, 60, -30, 30));
		PlotUtils.setup2DRegion(plotter, "Bend plane residuals vs momentum at target", 2, 
				"Momentum (GeV)", "x_{ep} - x_{t} (mm)",
				aida.histogram2D("Bend plane residuals vs momentum at target", 5, 0, 2.5, 60, -3, 3));
		PlotUtils.setup2DRegion(plotter, "Non-bend plane residuals vs momentum at target", 3, 
				"Momentum (GeV)", "y_{ep} - y_{t} (mm)",
				aida.histogram2D("Non-bend plane residuals vs momentum at target", 5, 0, 2.5, 50, -2.5, 2.5));
		plotters.add(plotter);
		
		for(IPlotter iPlotter : plotters){
			iPlotter.show(); 
		}
	}
	
	public void process(EventHeader event){
	
		// If the event doesn't contain an LCRelation between a track and its 
		// corresponding ECal scoring plane hit, skip the event.
		if(!event.hasCollection(LCRelation.class, trackToScoringPlaneHitRelationsName)) return;
		
		List<LCRelation> trackToScoringPlaneHitRelations = event.get(LCRelation.class, trackToScoringPlaneHitRelationsName);
		
		for(LCRelation trackToScoringPlaneHitRelation : trackToScoringPlaneHitRelations){
		
			// Get the track
			Track track = (Track) trackToScoringPlaneHitRelation.getFrom();
			
			// Get the track momentum
			double[] momentum = BaseTrackState.computeMomentum(track.getTrackStates().get(0), bField.y());
			double p = Math.sqrt(momentum[0]*momentum[0] + momentum[1]*momentum[1] + momentum[2]*momentum[2]);
			this.printVerbose("Track momentum: " + p);
			
			// Get the corresponding scoring plane hit
			SimTrackerHit scoringPlaneHit = (SimTrackerHit) trackToScoringPlaneHitRelation.getTo();
			Hep3Vector scoringPlaneHitPosition = scoringPlaneHit.getPositionVec();
			this.printVerbose("Scoring plane hit position: " + scoringPlaneHitPosition.toString());
			
			// Fill the scoring plane position histograms
			aida.histogram1D("Scoring plane hit position - x").fill(scoringPlaneHitPosition.x());
			aida.histogram1D("Scoring plane hit position - y").fill(scoringPlaneHitPosition.y());
			aida.histogram1D("Scoring plane hit position - z").fill(scoringPlaneHitPosition.z());
			aida.histogram2D("Scoring plane hit position - x-y").fill(scoringPlaneHitPosition.x(), scoringPlaneHitPosition.y());
		
			// Extrapolate the track to the scoring plane position
			Hep3Vector trackPositionAtScoringPlane = TrackUtils.extrapolateTrack(track, scoringPlaneHitPosition.z());
			this.printVerbose("Extrapolated track position: " + trackPositionAtScoringPlane.toString());
				
			// Find the residual between the extrapolated track position and the scoring plane hit position
			double deltaX = trackPositionAtScoringPlane.x() - scoringPlaneHitPosition.x();
			double deltaY = trackPositionAtScoringPlane.y() - scoringPlaneHitPosition.y();
			// This should be 0 but it serves as a sanity check.
			double deltaZ = trackPositionAtScoringPlane.z() - scoringPlaneHitPosition.z();
		
			if(track.getTrackerHits().get(0).getPosition()[2] > 0){
				if(track.getTrackStates().get(0).getOmega() > 0){
					aida.histogram1D("Top positron tracks - Bend plane residual at scoring plane").fill(deltaX);
					aida.histogram1D("Top positron tracks - Non-bend plane residuals at scoring plane").fill(deltaY);
					aida.histogram1D("Top positron tracks - z residuals at scoring plane").fill(deltaZ);	
				} else { 
					aida.histogram1D("Top electron tracks - Bend plane residual at scoring plane").fill(deltaX);
					aida.histogram1D("Top electron tracks - Non-bend plane residuals at scoring plane").fill(deltaY);
					aida.histogram1D("Top electron tracks - z residuals at scoring plane").fill(deltaZ);	
				}
			} else { 
				if(track.getTrackStates().get(0).getOmega() > 0){
					aida.histogram1D("Bottom positron tracks - Bend plane residuals at scoring plane").fill(deltaX);
					aida.histogram1D("Bottom positron tracks - Non-bend plane residuals at scoring plane").fill(deltaY);
					aida.histogram1D("Bottom positron tracks - z residuals at scoring plane").fill(deltaZ);
				} else { 
					aida.histogram1D("Bottom electron tracks - Bend plane residuals at scoring plane").fill(deltaX);
					aida.histogram1D("Bottom electron tracks - Non-bend plane residuals at scoring plane").fill(deltaY);
					aida.histogram1D("Bottom electron tracks - z residuals at scoring plane").fill(deltaZ);
				}
			}

			aida.histogram2D("Bend plane residuals vs momentum at scoring plane").fill(p, deltaX);
			aida.histogram2D("Non-bend plane residuals vs momentum at scoring plane").fill(p, deltaY);
		}
		
		if(!event.hasCollection(LCRelation.class, trackToMCParticleRelationsName)) return;
		
		List<LCRelation> trackToMCParticleRelations = event.get(LCRelation.class, trackToMCParticleRelationsName);
		
		for(LCRelation trackToMCParticleRelation : trackToMCParticleRelations){
			
			// Get the track
			Track track = (Track) trackToMCParticleRelation.getFrom();
			
			// Get the track momentum
			double[] momentum = BaseTrackState.computeMomentum(track.getTrackStates().get(0), bField.y());
			double p = Math.sqrt(momentum[0]*momentum[0] + momentum[1]*momentum[1] + momentum[2]*momentum[2]);
			this.printVerbose("Track momentum: " + p);

			// Get the corresponding MC particle
			MCParticle particle = (MCParticle) trackToMCParticleRelation.getTo();
			
			// Extrapolate the track to the origin
			Hep3Vector trackPositionAtOrigin = TrackUtils.extrapolateTrack(track, particle.getOriginZ());
			
			// Find the residual between the extrapolated track and the position of the scoring plane at the origin
			double deltaX = trackPositionAtOrigin.x() - particle.getOriginX();
			double deltaY = trackPositionAtOrigin.y() - particle.getOriginY();
			double deltaZ = trackPositionAtOrigin.z() - particle.getOriginZ();
			
			if(track.getTrackerHits().get(0).getPosition()[2] > 0){
				if(track.getTrackStates().get(0).getOmega() > 0){
					aida.histogram1D("Top positron tracks - Bend plane residual at target").fill(deltaX);
					aida.histogram1D("Top positron tracks - Non-bend plane residuals at target").fill(deltaY);
					aida.histogram1D("Top positron tracks - z residuals at target").fill(deltaZ);
				} else { 
					aida.histogram1D("Top electron tracks - Bend plane residual at target").fill(deltaX);
					aida.histogram1D("Top electron tracks - Non-bend plane residuals at target").fill(deltaY);
					aida.histogram1D("Top electron tracks - z residuals at target").fill(deltaZ);
				}
			} else { 
				
				if(track.getTrackStates().get(0).getOmega() > 0){
					aida.histogram1D("Bottom positron tracks - Bend plane residuals at target").fill(deltaX);
					aida.histogram1D("Bottom positron tracks - Non-bend plane residuals at target").fill(deltaY);
					aida.histogram1D("Bottom positron tracks - z residuals at target").fill(deltaZ);
				} else { 
					aida.histogram1D("Bottom electron tracks - Bend plane residuals at target").fill(deltaX);
					aida.histogram1D("Bottom electron tracks - Non-bend plane residuals at target").fill(deltaY);
					aida.histogram1D("Bottom electron tracks - z residuals at target").fill(deltaZ);
				}
			}
			
			aida.histogram2D("Bend plane residuals vs momentum at target").fill(p, deltaX);
			aida.histogram2D("Non-bend plane residuals vs momentum at target").fill(p, deltaY);
		}
	}
	
	/**
	 * Print a message if verbose has been enabled.
	 *  
	 * @param message : message to print.
	 */
	private void printVerbose(String message){
		if(verbose)
			System.out.println(this.getClass().getSimpleName() + ": " + message);
	}
	
}
