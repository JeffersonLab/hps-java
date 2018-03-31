package org.hps.recon.ecal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hps.detector.ecal.EcalCrystal;
import org.hps.util.TruthCalorimeterHit;
import org.lcsim.detector.IGeometryInfo;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.EventHeader.LCMetaData;
import org.lcsim.event.base.BaseCalorimeterHit;

/**
 * This is a simple set of utility methods for creating CalorimeterHit objects. 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class CalorimeterHitUtilities {

    /**
     * This class is purely static.
     */
    private CalorimeterHitUtilities() {        
    }
    
    public static final CalorimeterHit create(double energy, double time, long id, int type) {
        return create(energy, time, id, type, null);
    }
    
    public static final CalorimeterHit create(double energy, double time, long id) {
        return create(energy, time, id, 0, null);
    }    
    
    public static final CalorimeterHit create(double energy, double time, long id, LCMetaData meta) {
        return create(energy, time, id, 0, meta);
    }    
    
    public static final CalorimeterHit create(double energy, double time, long id, int type, LCMetaData metaData) {
        return new BaseCalorimeterHit(energy, energy, 0, time, id, null, type, metaData);
    }    
    
    public static EcalCrystal findCrystal(CalorimeterHit hit) {
        return (EcalCrystal)hit.getSubdetector().getDetectorElement().findDetectorElement(hit.getIdentifier()).get(0);
    }
    
    public static IGeometryInfo findGeometryInfo(CalorimeterHit hit) {
        return findCrystal(hit).getGeometry();
    }
    
    /**
     * Converts a hit of type {@link org.lcsim.event.CalorimeterHit
     * CalorimeterHit} to a {@link org.lcsim.event.SimCalorimeterHit
     * SimCalorimeterHit} object that contains all of the truth data
     * present in the attached {@link java.util.Set Set}.
     * @param baseHit - The generic calorimeter hit object.
     * @param truthData - A collection of all truth data associated
     * with the calorimeter hit object.
     * @return Returns a SimCalorimeterHit object with the same
     * parameters as the generic hit, but with all the compiled truth
     * information from the truth set.
     */
    public static final SimCalorimeterHit convertToTruthHit(CalorimeterHit baseHit, Collection<SimCalorimeterHit> truthData, LCMetaData metaData) {
        // If there is no truth information, just don't write any.
        // Declare a warning, though, so the user knows that this is
        // happening.
        if(truthData == null || truthData.isEmpty()) {
            System.out.println("Warning: Converting hit to truth hit, but no truth data was provided. (Hit time = " + baseHit.getTime() + ".)");
            if(truthData == null) {
                truthData = new java.util.ArrayList<SimCalorimeterHit>(0);
            }
        }
        
        // Extract the truth data into one, singular list.
        List<Integer> pdgs = new ArrayList<Integer>();
        List<Float> times = new ArrayList<Float>();
        List<Float> energies = new ArrayList<Float>();
        List<MCParticle> particles = new ArrayList<MCParticle>();
        for(SimCalorimeterHit truthHit : truthData) {
            // Iterate over the truth data...
            int particleCount = truthHit.getMCParticleCount();
            for(int i = 0; i < particleCount; i++) {
                // Append the truth information for the truth hit to
                // the combined truth information for the new hit.
                pdgs.add(truthHit.getMCParticle(i).getPDGID());
                energies.add((float) truthHit.getContributedEnergy(i));
                particles.add(truthHit.getMCParticle(i));
                
                // Note that contribution times are automatically
                // used to calculate a time in SimCalorimeterHit for
                // the overall hit. This behavior is not desired here
                // and thus all particles are given the "hit time"
                // from the base hit to bypass this.
                times.add((float) baseHit.getTime());
            }
        }
        
        // Convert the lists to arrays.
        MCParticle[] particleTruthArray = particles.toArray(new MCParticle[particles.size()]);
        int[] pdgsTruthArray = new int[particles.size()];
        float[] timeTruthArray = new float[particles.size()];
        float[] energyTruthArray = new float[particles.size()];
        for(int i = 0; i < particles.size(); i++) {
            pdgsTruthArray[i] = pdgs.get(i).intValue();
            timeTruthArray[i] = times.get(i).floatValue();
            energyTruthArray[i] = energies.get(i).floatValue();
        }
        
        // Store the truth information in a new SimCalorimeterHit
        // object, and give it the same properties otherwise as the
        // base hit.
        SimCalorimeterHit newHit = new TruthCalorimeterHit(baseHit, particleTruthArray, energyTruthArray, timeTruthArray, pdgsTruthArray);
        
        // Return the hit.
        return newHit;
    }
}