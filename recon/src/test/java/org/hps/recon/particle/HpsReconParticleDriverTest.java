package org.hps.recon.particle;

import java.util.List; 
import java.util.ArrayList; 

import junit.framework.TestCase;

import org.lcsim.event.Cluster;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track; 
import org.lcsim.event.base.BaseCalorimeterHit;
import org.lcsim.event.base.BaseCluster;
import org.lcsim.event.base.BaseTrack; 

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import org.hps.recon.particle.HpsReconParticleDriver; 
import org.hps.recon.tracking.CoordinateTransformations;


/**
 *
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id$
 */
public class HpsReconParticleDriverTest extends TestCase { 

	private static final double B_FIELD = 0.5; // Tesla
  	double[] trackParameters = new double[5];
 	List<Track> tracks = new ArrayList<Track>(); 
	List<Cluster> clusters = new ArrayList<Cluster>();
	List<ReconstructedParticle> particleTracks; 
	HpsReconParticleDriver particleDriver = null; 
	
   	public void setUp() throws Exception {
   		
   		System.out.println("\n#=== Creating Ideal Tracks ===#\n");
   
   		// Create a pair of ideal e+e- tracks in opposite detector volumes.
   		// The e+ track is created on the bottom half of the detector while
   		// the e- track is created on the top half.
   		Track electronTrack = new BaseTrack(); 
   		trackParameters[BaseTrack.D0] = 0.41051;
   		trackParameters[BaseTrack.OMEGA] = -2.2584e-4; 
   		trackParameters[BaseTrack.PHI] = 6.2626; 
   		trackParameters[BaseTrack.TANLAMBDA] = 0.046548; 
   		trackParameters[BaseTrack.Z0] = .23732; 
   		((BaseTrack) electronTrack).setTrackParameters(trackParameters, B_FIELD);
   		
   		System.out.println("\n[ Track ] Electron:  \n" + electronTrack.toString());
   		
   		Track positronTrack = new BaseTrack();
   		trackParameters[BaseTrack.D0] = 0.19691;
   		trackParameters[BaseTrack.OMEGA] = 1.005e-4; 
   		trackParameters[BaseTrack.PHI] = 6.2447; 
   		trackParameters[BaseTrack.TANLAMBDA] = -0.024134; 
   		trackParameters[BaseTrack.Z0] = -0.040231; 
   		((BaseTrack) positronTrack).setTrackParameters(trackParameters, B_FIELD);

   		System.out.println("\n[ Track ] Positron: \n" + positronTrack.toString());

   		// Add the tracks to the list of tracks that will be used for test
   		// purposes.
   		tracks.add(electronTrack);
   		tracks.add(positronTrack);
   		
   		System.out.println("\n#=== Creating Ideal Ecal Clusters ===#\n");
   		
   		// Create a pair of ideal clusters to match the e+e- pairs created
   		// above.  Since the properties of a cluster cannot be modified 
   		// directly via setter methods, first create a CalorimeterHit and
   		// then use that to create a cluster.
   		//Hep3Vector topHitPosition = new BasicHep3Vector(190.27, 69.729, 1422.8);
   		//BaseCalorimeterHit topHit 
   		//	= new BaseCalorimeterHit(.4600, .4600, 0, 0, 0, topHitPosition, 0);
   		
   		//System.out.println("\n[ Calorimeter Hit ] Top: \n" + topHit.toString());
   		
   		Cluster topCluster = new BaseCluster();
   		//((BaseCluster) topCluster).addHit(topHit);
   		
   		
   		System.out.print("\n[ Cluster ] Top: " + topCluster.toString());
   		System.out.println(" and position= ["  + topCluster.getPosition()[0] + ", " 
   											   + topCluster.getPosition()[1] + ", " 
   											   + topCluster.getPosition()[2] + " ]");
   		
   		//Hep3Vector bottomHitPosition = new BasicHep3Vector(-148.46, -39.27, 1430.5);
   		//BaseCalorimeterHit bottomHit 
   		//	= new BaseCalorimeterHit(1.1420, 1.1420, 0, 0, 0, bottomHitPosition, 0);

   		//System.out.println("\n[ Calorimeter Hit ] Bottom:\n " + bottomHit.toString());
   		
   		Cluster bottomCluster = new BaseCluster();
   		//((BaseCluster) bottomCluster).addHit(bottomHit);
   		
   		System.out.print("\n[ Cluster ] bottom: " + bottomCluster.toString());
   		System.out.println(" and position= [ " + topCluster.getPosition()[0] + ", " 
   											   + topCluster.getPosition()[1] + ", " 
   											   + topCluster.getPosition()[2] + " ]");

   		// Add the clusters to the list of clusters that will be used for test
   		// purposes.
   		clusters.add(topCluster);
   		clusters.add(bottomCluster);
   		
   		particleDriver = new HpsReconParticleDriver();
   		particleDriver.setDebug(true);
   	}
   	
   	public void testMakeReconstructedParticles(){
   		
   		System.out.println("\n#=== Running makeReconstructedParticles Test ===#");
    	
    	
    	// Create two ReconstructedParticles with tracks only
    	List<Cluster> emptyClusters = new ArrayList<Cluster>(); 
    	List<List<Track>> trackCollections = new ArrayList<List<Track>>(0);
    	trackCollections.add(tracks);
    	particleTracks = particleDriver.makeReconstructedParticles(emptyClusters, trackCollections);
    
    	//
    	// The list contains two Tracks which should result in two 
    	// ReconstructedParticles.
    	//
    	assertTrue("More particles than expected were created.", particleTracks.size() == 2);
    	System.out.println("\nThe number of ReconstructedParticles created: " + particleTracks.size());
    
    	for(int particleN = 0; particleN < particleTracks.size(); particleN++){
    	
    		//
    		//	Check if the RecontructedParticle track is the same as the track 
    		//	that created it
    		// 
    		assertTrue("The particle track does not match the track that created it",
    					particleTracks.get(particleN).getTracks().get(0).equals(tracks.get(particleN)));
    	
    	
    		//
    		// Check that the charge of the ReconstructedParticles was set properly
    		//
    		assertTrue("The charge of the ReconstructedParticle is equal to zero.", 
    					Math.abs(particleTracks.get(particleN).getCharge()) != 0);
    		System.out.println("The charge of ReconstructedParticle number " + particleN + ": " + particleTracks.get(particleN).getCharge());
    	
    	
    		//
    		// Check that the particle ID was set correctly
    		//
    		assertTrue("The particle ID of the ReconstructedParticle is equal to zero.", 
    				   particleTracks.get(particleN).getParticleIDUsed().getPDG() != 0);
    		System.out.println("The particle ID of ReconstructedParticle number " + particleN + ": " + particleTracks.get(particleN).getParticleIDUsed().getPDG());
    	}
    	
    	//
    	// Check that the momentum of the ReconstructedParticles was set properly 
    	// and rotated to the detector frame.
    	//
    	Hep3Vector electronMomentum = new BasicHep3Vector(tracks.get(0).getTrackStates().get(0).getMomentum());
    	electronMomentum = CoordinateTransformations.transformVectorToDetector(electronMomentum);
    	assertTrue("The momentum of the track and ReconstructedParticle don't match! Top track p = " 
    				+ electronMomentum.toString() + " Recon particle p = " + particleTracks.get(0).getMomentum().toString(),
    				particleTracks.get(0).getMomentum().equals(electronMomentum));
    	
    	System.out.println("The momentum of the first ReconstructedParticle: " + particleTracks.get(0).getMomentum().toString());
    	
    	Hep3Vector positronMomentum = new BasicHep3Vector(tracks.get(1).getTrackStates().get(0).getMomentum());
    	positronMomentum = CoordinateTransformations.transformVectorToDetector(positronMomentum);
    	assertTrue("The momentum of track and ReconstructedParticle don't march! Bottom track p = "
    			    + positronMomentum.toString() + " Recon particle p = " + particleTracks.get(1).getMomentum().toString(),
    			    particleTracks.get(1).getMomentum().equals(positronMomentum));
    	
    	System.out.println("The momentum of the second ReconstructedParticle: " + particleTracks.get(1).getMomentum().toString());
    	
   	}
   	
   	public void testVertexParticles(){
   	
    	// Create two ReconstructedParticles with tracks only
    	//List<Cluster> emptyClusters = new ArrayList<Cluster>(); 
    	//particleTracks = particleDriver.makeReconstructedParticles(emptyClusters, tracks);

    	//List<ReconstructedParticle> electrons = particleTracks.subList(0, 1);
   		//List<ReconstructedParticle> positrons = particleTracks.subList(1, 2);
   	
   		//particleDriver.vertexParticles(electrons, positrons);
   		
   	}
}
