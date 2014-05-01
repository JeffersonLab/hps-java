package org.hps.recon.particle;

import java.util.List; 
import java.util.ArrayList; 

import junit.framework.TestCase;

import org.hps.recon.ecal.HPSEcalCluster;
import org.lcsim.event.Track; 
import org.lcsim.event.base.BaseCalorimeterHit;
import org.lcsim.event.base.BaseTrack; 

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

/**
 *
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id$
 */
public class HpsReconParticleDriverTest extends TestCase { 

	private static final double B_FIELD = 0.5; // Tesla
  	double[] trackParameters = new double[5];
 	List<Track> tracks = new ArrayList<Track>(); 
	List<HPSEcalCluster> clusters = new ArrayList<HPSEcalCluster>(); 
	
   	public void setup() throws Exception {
   
   		// Create a pair of ideal e+e- tracks in opposite detector volumes.
   		// The e+ track is created on the bottom half of the detector while
   		// the e- track is created on the top half.
   		BaseTrack topTrack = new BaseTrack(); 
   		trackParameters[BaseTrack.D0] = 0.41051;
   		trackParameters[BaseTrack.OMEGA] = -2.2584e-4; 
   		trackParameters[BaseTrack.PHI] = 6.2626; 
   		trackParameters[BaseTrack.TANLAMBDA] = 0.046548; 
   		trackParameters[BaseTrack.Z0] = .23732; 
   		topTrack.setTrackParameters(trackParameters, B_FIELD);
   		
   		BaseTrack bottomTrack = new BaseTrack();
   		trackParameters[BaseTrack.D0] = 0.19691;
   		trackParameters[BaseTrack.OMEGA] = 1.005e-4; 
   		trackParameters[BaseTrack.PHI] = 6.2447; 
   		trackParameters[BaseTrack.TANLAMBDA] = -0.024134; 
   		trackParameters[BaseTrack.Z0] = -0.040231; 
   		bottomTrack.setTrackParameters(trackParameters, B_FIELD);

   		// Add the tracks to the list of tracks that will be used for test
   		// purposes.
   		tracks.add(topTrack);
   		tracks.add(bottomTrack);
   		
   		// Create a pair of ideal clusters to match the e+e- pairs created
   		// above.  Since the properties of a cluster cannot be modified 
   		// directly via setter methods, first create a CalorimeterHit and
   		// then use that to create a cluster.
   		Hep3Vector topHitPosition = new BasicHep3Vector(190.27, 69.729, 1422.8);
   		BaseCalorimeterHit topHit 
   			= new BaseCalorimeterHit(.4600, .4600, 0, 0, 0, topHitPosition, 0);
   		HPSEcalCluster topCluster = new HPSEcalCluster(topHit);
   		
   		Hep3Vector bottomHitPosition = new BasicHep3Vector(-148.46, -39.27, 1430.5);
   		BaseCalorimeterHit bottomHit 
   			= new BaseCalorimeterHit(1.1420, 1.1420, 0, 0, 0, bottomHitPosition, 0);
   		HPSEcalCluster bottomCluster = new HPSEcalCluster(bottomHit);
   		
   		// Add the clusters to the list of clusters that will be used for test
   		// purposes.
   		clusters.add(topCluster);
   		clusters.add(bottomCluster);
   		
   	}
   	
   	public void testMakeReconstructedParticles(){
    	
    	HpsReconParticleDriver particleDriver = new HpsReconParticleDriver(); 
    	particleDriver.makeReconstructedParticles(clusters, tracks);
   	
   	}

}
