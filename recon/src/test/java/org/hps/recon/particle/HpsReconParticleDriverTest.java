package org.hps.recon.particle;

import java.util.List; 
import java.util.ArrayList; 

import junit.framework.TestCase;

import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track; 
import org.lcsim.event.base.BaseCalorimeterHit;
import org.lcsim.event.base.BaseTrack; 

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import org.hps.recon.particle.HpsReconParticleDriver; 
import org.hps.recon.ecal.HPSEcalCluster;


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
	List<ReconstructedParticle> particleTracks; 
	HpsReconParticleDriver particleDriver = null; 
	
   	public void setUp() throws Exception {
   		
   		System.out.println("\n#=== Creating Ideal Tracks ===#\n");
   
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
   		
   		System.out.println("\n[ Track ] Top:  \n" + topTrack.toString());
   		
   		BaseTrack bottomTrack = new BaseTrack();
   		trackParameters[BaseTrack.D0] = 0.19691;
   		trackParameters[BaseTrack.OMEGA] = 1.005e-4; 
   		trackParameters[BaseTrack.PHI] = 6.2447; 
   		trackParameters[BaseTrack.TANLAMBDA] = -0.024134; 
   		trackParameters[BaseTrack.Z0] = -0.040231; 
   		bottomTrack.setTrackParameters(trackParameters, B_FIELD);

   		System.out.println("\n[ Track ] Bottom: \n" + bottomTrack.toString());

   		// Add the tracks to the list of tracks that will be used for test
   		// purposes.
   		tracks.add(topTrack);
   		tracks.add(bottomTrack);
   		
   		System.out.println("\n#=== Creating Ideal Ecal Clusters ===#\n");
   		
   		// Create a pair of ideal clusters to match the e+e- pairs created
   		// above.  Since the properties of a cluster cannot be modified 
   		// directly via setter methods, first create a CalorimeterHit and
   		// then use that to create a cluster.
   		Hep3Vector topHitPosition = new BasicHep3Vector(190.27, 69.729, 1422.8);
   		BaseCalorimeterHit topHit 
   			= new BaseCalorimeterHit(.4600, .4600, 0, 0, 0, topHitPosition, 0);
   		
   		System.out.println("\n[ Calorimeter Hit ] Top: \n" + topHit.toString());
   		
   		HPSEcalCluster topCluster = new HPSEcalCluster(topHit);
   		
   		System.out.print("\n[ Cluster ] Top: " + topCluster.toString());
   		System.out.println(" and position= ["  + topCluster.getPosition()[0] + ", " 
   											   + topCluster.getPosition()[1] + ", " 
   											   + topCluster.getPosition()[2] + " ]");
   		
   		Hep3Vector bottomHitPosition = new BasicHep3Vector(-148.46, -39.27, 1430.5);
   		BaseCalorimeterHit bottomHit 
   			= new BaseCalorimeterHit(1.1420, 1.1420, 0, 0, 0, bottomHitPosition, 0);

   		System.out.println("\n[ Calorimeter Hit ] Bottom:\n " + bottomHit.toString());
   		
   		HPSEcalCluster bottomCluster = new HPSEcalCluster(bottomHit);
   		
   		System.out.print("\n[ Cluster ] bottom: " + bottomCluster.toString());
   		System.out.println(" and position= [ " + topCluster.getPosition()[0] + ", " 
   											   + topCluster.getPosition()[1] + ", " 
   											   + topCluster.getPosition()[2] + " ]");

   		// Add the clusters to the list of clusters that will be used for test
   		// purposes.
   		clusters.add(topCluster);
   		clusters.add(bottomCluster);
   		
   		particleDriver = new HpsReconParticleDriver(); 
   	}
   	
   	public void testMakeReconstructedParticles(){
   		
   		System.out.println("\n#=== Running makeReconstructedParticles Test ===#");
    	
    	
    	// Create two ReconstructedParticles with tracks only
    	List<HPSEcalCluster> emptyClusters = new ArrayList<HPSEcalCluster>(); 
    	particleTracks = particleDriver.makeReconstructedParticles(emptyClusters, tracks);
    
    	assertTrue("More particles than expected were created.", particleTracks.size() == 2);
    	
    	System.out.println("\nThe number of ReconstructedParticles created: " + particleTracks.size());
    	
    	//
    	// Check that the momentum of the ReconstructedParticles was set properly
    	//
    	Hep3Vector topMomentum = new BasicHep3Vector(tracks.get(0).getTrackStates().get(0).getMomentum());
    	assertTrue("The momentum of the track and ReconstructedParticle don't match! Top track p = " 
    				+ topMomentum.toString() + " Recon particle p = " + particleTracks.get(0).getMomentum().toString(),
    				particleTracks.get(0).getMomentum().equals(topMomentum));
    	
    	System.out.println("The momentum of the first ReconstructedParticle: " + particleTracks.get(0).getMomentum().toString());
    	
    	Hep3Vector bottomMomentum = new BasicHep3Vector(tracks.get(1).getTrackStates().get(0).getMomentum());
    	assertTrue("The momentum of track and ReconstructedParticle don't march! Bottom track p = "
    			    + bottomMomentum.toString() + " Recon particle p = " + particleTracks.get(1).getMomentum().toString(),
    			    particleTracks.get(1).getMomentum().equals(bottomMomentum));
    	
    	System.out.println("The momentum of the second ReconstructedParticle: " + particleTracks.get(1).getMomentum().toString());

    	//
    	// Check that the charge of the ReconstructedParticles was set properly
    	//
    	for(int particleN = 0; particleN < particleTracks.size(); particleN++){
    		assertTrue("The charge of the ReconstructedParticle is equal to zero.", Math.abs(particleTracks.get(particleN).getCharge()) != 0);
    		
    		System.out.println("The charge of ReconstructedParticle number " + particleN + ": " + particleTracks.get(particleN).getCharge());
    	}
    	
   	}
   	
   	public void testVertexParticles(){
   	
   	}
}
