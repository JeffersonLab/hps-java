package org.hps.analysis.examples;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.util.Driver;

/**
 * Example driver showing how to retrieve MC truth information from an lcsim event.
 * 
 * @author Jeremy McCormick, SLAC
 */
public class MCTruthExampleDriver extends Driver {
    
    public void process(EventHeader event) {        
        
        System.out.println("MCTruthExampleDriver: Process event " + event.getEventNumber());
        
        // get objects and collections from event header
        List<SimTrackerHit> trackerHits = event.get(SimTrackerHit.class, "TrackerHits");
        List<SimCalorimeterHit> calHits = event.get(SimCalorimeterHit.class, "EcalHits");
        List<MCParticle> particles = event.get(MCParticle.class, "MCParticle");        
        IDDecoder trackerDecoder = event.getMetaData(trackerHits).getIDDecoder();
        IDDecoder calDecoder = event.getMetaData(calHits).getIDDecoder();
        
        // maps of particles to hits
        Map<MCParticle, List<SimTrackerHit>> trackerHitMap = new HashMap<MCParticle, List<SimTrackerHit>>();
        Map<MCParticle, List<SimCalorimeterHit>> calHitMap = new HashMap<MCParticle, List<SimCalorimeterHit>>();
        
        // map particle to a list of its sim tracker hits
        for (SimTrackerHit hit : trackerHits) {
            MCParticle p = hit.getMCParticle();
            if (p == null) {
                throw new RuntimeException("Tracker hit points to null MCParticle!");
            }
            if (trackerHitMap.get(p) == null) {
                trackerHitMap.put(p, new ArrayList<SimTrackerHit>());
            }
            trackerHitMap.get(p).add(hit);
        }        
        
        System.out.println(">>>> Tracker Hits <<<<");
        
        // loop over entries mapping particles to sim tracker hits
        for (Entry<MCParticle, List<SimTrackerHit>> entry : trackerHitMap.entrySet()) {
            
            MCParticle p = entry.getKey();
            List<SimTrackerHit> hits = entry.getValue();
            
            // Print particle info
            printMCParticle(p, particles);                        
            
            // loop over particle's hits
            for (SimTrackerHit hit : hits) {
                
                // layer number of the hit
                trackerDecoder.setID(hit.getCellID64());
                int layer = trackerDecoder.getValue("layer");
                
                // position of the hit
                Hep3Vector pos = hit.getPositionVec();
                
                // hit momentum (really direction as it is a unit vector)
                Hep3Vector hitP = new BasicHep3Vector(hit.getMomentum()[0], hit.getMomentum()[1], hit.getMomentum()[2]);
                
                System.out.println("  layer: " + layer + "; " +
                        "position: " + pos + "; " +
                        "direction: " + hitP);
                
                // scattering angle???
            }            
        }
                
        // map particle to a list of its sim cal hits
        for (SimCalorimeterHit hit : calHits) {
            int nmc = hit.getMCParticleCount();
            for (int i = 0; i < nmc; i++) {
                MCParticle p = hit.getMCParticle(i);
                if (p == null) {
                    throw new RuntimeException("Cal hit points to null MCParticle!");
                }
                if (calHitMap.get(p) == null) {
                    calHitMap.put(p, new ArrayList<SimCalorimeterHit>());
                }
                calHitMap.get(p).add(hit);
            }
        }
        
        System.out.println(">>>> Cal Hits <<<<");
        
        // loop over entries mapping particles to sim cal hits
        for (Entry<MCParticle, List<SimCalorimeterHit>> entry : calHitMap.entrySet()) {
            MCParticle p = entry.getKey();
            List<SimCalorimeterHit> hits = entry.getValue();
            printMCParticle(p, particles);
            for (SimCalorimeterHit hit : hits) {
                calDecoder.setID(hit.getCellID());
                int ix = calDecoder.getValue("ix");
                int iy = calDecoder.getValue("iy");
                System.out.println("  ix: " + ix + "; iy: " + iy + "; position: " + hit.getPositionVec() + "; energy: " + hit.getCorrectedEnergy());
            }
        }
        
        System.out.println();
    }
    
    void printMCParticle(MCParticle p, List<MCParticle> particles) {
        
        // particle PDG ID
        int pdgid = p.getPDGID();
        
        // particle energy
        double energy = p.getEnergy();
        
        // momentum at origin
        Hep3Vector momentum = new BasicHep3Vector(p.getPX(), p.getPY(), p.getPZ());
        
        // origin
        Hep3Vector vertex = p.getOrigin();
        
        // end point
        Hep3Vector endPoint = p.getEndPoint();
        
        // parent index in particle list
        Integer parIdx = null;
        
        // find parent's index in the particle collection
        if (p.getParents().size() > 0) {
            MCParticle parent = p.getParents().get(0);
            for (int i = 0; i < particles.size(); i++) {
                if (particles.get(i) == parent) {
                    parIdx = i;
                    break;
                }
            }
        }
        
        // print particle info
        System.out.println("PDG ID: " + pdgid + "; " +
                "energy: " + energy + "; " +
                "momentum: " + momentum + "; " +
                "vertex: " + vertex + "; " +
                "endPoint: " + endPoint + "; " +
                "parentIdx: " + parIdx);
    }
}