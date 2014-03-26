package org.lcsim.hps.users.omoreno;

//--- java ---//
//--- hep ---//
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.List;

import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.Cluster;
//--- lcsim ---//
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
//--- hps-java ---//

/**
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id: ReconstructedParticleChecker.java,v 1.3 2013/10/14 22:58:03 phansson Exp $
 *
 */
public class ReconstructedParticleChecker extends Driver {
	
	private AIDA aida; 
	private List<IPlotter> plotters = new ArrayList<IPlotter>(); 

	IHistogram1D xPositionResidual;
	IHistogram1D yPositionResidual;
	IHistogram1D zPositionResidual;
	IHistogram1D r;
	
	// Collection Names
	private String finalStateParticlesCollectionName = "FinalStateParticles";
	
	boolean debug = true; 
	int plotterIndex = 0; 
	
	public ReconstructedParticleChecker(){} 
	
	protected void detectorChanged(Detector detector){
		super.detectorChanged(detector);
		
		// Setup AIDA
		aida = AIDA.defaultInstance(); 
		aida.tree().cd("/");
		
		plotters.add(PlotUtils.setupPlotter("Track-Cluster Position Residual", 2, 2));
		xPositionResidual = aida.histogram1D("x Residual", 100, -100, 100);
		yPositionResidual = aida.histogram1D("y Residual", 100, -100, 100);
		zPositionResidual = aida.histogram1D("z Residual", 100, -100, 100);
		r = aida.histogram1D("r", 100, -100, 100);
		PlotUtils.setup1DRegion(plotters.get(plotterIndex), "x Residual", 0, "delta x [mm]", xPositionResidual);
		PlotUtils.setup1DRegion(plotters.get(plotterIndex), "y Residual", 1, "delta y [mm]", yPositionResidual);
		PlotUtils.setup1DRegion(plotters.get(plotterIndex), "z Residual", 2, "delta z [mm]", zPositionResidual);
		PlotUtils.setup1DRegion(plotters.get(plotterIndex), "r", 3, "r [mm]", r);
		
		
		for(IPlotter plotter : plotters){
			plotter.show(); 
		}
	}
	
	public void process(EventHeader event){
		
		// If the event doesn't contain any final state reconstructed 
		// particles, skip the event 
		if(!event.hasCollection(ReconstructedParticle.class, finalStateParticlesCollectionName)){
			this.printDebug("Event does not contain ReconstructedParticles");
			return;
		}
		
		// Get the collections of reconstructed final state particles from the
		// event
		List<ReconstructedParticle> finalStateParticles 
			= event.get(ReconstructedParticle.class, finalStateParticlesCollectionName); 
				
	
		// Loop over all of the reconstructed particles in the event
		for(ReconstructedParticle finalStateParticle : finalStateParticles){
			
			// Get the list of clusters from the event
			List<Cluster> ecalClusters = finalStateParticle.getClusters(); 
				this.printDebug("Number of Ecal clusters: " + ecalClusters.size()); 
			if(ecalClusters.isEmpty()){
				this.printDebug("Number of Ecal clusters: " + ecalClusters.size()); 
				this.printDebug("List of Ecal cluster is empty ... skipping");
				continue; 
			}
			
			// Get the list of tracks from the event
			List<Track> tracks = finalStateParticle.getTracks(); 
			if(tracks.isEmpty()){
				this.printDebug("List of tracks is empty ... skipping");
				continue; 
			}
		
			Hep3Vector ecalPosition = new BasicHep3Vector(ecalClusters.get(0).getPosition()); 
			Hep3Vector trackPositionAtEcal = TrackUtils.extrapolateTrack(tracks.get(0),ecalPosition.z()); 
			xPositionResidual.fill(trackPositionAtEcal.x() - ecalPosition.x());
			yPositionResidual.fill(trackPositionAtEcal.y() - ecalPosition.y());
			zPositionResidual.fill(trackPositionAtEcal.z() - ecalPosition.z());
			r.fill(VecOp.sub(trackPositionAtEcal, ecalPosition).magnitude());
		}
		
	}
	
	private void printDebug(String debugMessage){
		if(debug)
			System.out.println(this.getClass().getSimpleName() + ": " + debugMessage); 
	}
}
