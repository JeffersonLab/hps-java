package org.hps.readout;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.base.BaseMCParticle;
import org.lcsim.event.base.BaseSimCalorimeterHit;

public class TruthReadoutDriver extends ReadoutDriver {
    private static final Set<Double> processedBunches = new HashSet<Double>();
    private static final Set<MCParticle> outputParticles = new HashSet<MCParticle>();
    private static final Set<SimCalorimeterHit> outputCalorimeterHits = new HashSet<SimCalorimeterHit>();
    private static Map<MCParticle, MCParticle> particleMap = new HashMap<MCParticle, MCParticle>();
    private static Map<SimCalorimeterHit, SimCalorimeterHit> calorimeterHitMap = new HashMap<SimCalorimeterHit, SimCalorimeterHit>();
    
    public static final void addBeamBunch(double bunchTime, double triggerTime) {
        // If this beam bunch has already been processed, skip it.
        if(processedBunches.contains(Double.valueOf(bunchTime))) {
            return;
        }
        
        // Get the calorimeter truth hits from this beam bunch.
        Collection<SimCalorimeterHit> baseHits = ReadoutDataManager.getData(bunchTime, bunchTime + ReadoutDataManager.getBeamBunchSize(), "EcalHits", SimCalorimeterHit.class);
        
        // Each hits needs to be cloned to a time-corrected hit. This
        // is done by making a copy of the truth hit with the same
        // properties, but it (and its particles) are time-shifted so
        // that the times are in reference to the trigger time rather
        // than the beam bunch time.
        for(SimCalorimeterHit truthHit : baseHits) {
            outputCalorimeterHits.add(cloneHit(truthHit, bunchTime - triggerTime));
        }
    }
    
    public static final SimCalorimeterHit getTimeShiftedHit(SimCalorimeterHit hit) {
        return calorimeterHitMap.get(hit);
    }
    
    public static final MCParticle getTimeShiftedParticle(MCParticle particle) {
        return particleMap.get(particle);
    }
    
    private static final SimCalorimeterHit cloneHit(SimCalorimeterHit base, double timeOffset) {
        // Process the particles into new time-corrected particles.
        for(int i = 0; i < base.getMCParticleCount(); i++) {
            // Get the root particle for the energy contribution.
            MCParticle rootParticle = getRootParticle(base.getMCParticle(i));
            
            // Convert the particle such that its time is in relation
            // to the trigger time instead of the bunch time.
            MCParticle convertedParticle = getCorrectedParticle(rootParticle, timeOffset);
            
            // Add the full particle tree to the list of output
            // particles.
            storeParticleTree(convertedParticle);
        }
        
        // Get the data needed for the new hit from the base hit.
        Object[] particles = new Object[base.getMCParticleCount()];
        float[] energies = new float[base.getMCParticleCount()];
        float[] times = new float[base.getMCParticleCount()];
        int[] pdgs = new int[base.getMCParticleCount()];
        for(int i = 0; i < base.getMCParticleCount(); i++) {
            // Most parameters are not changed.
            pdgs[i] = base.getPDG(i);
            times[i] = (float) base.getContributedTime(i);
            energies[i] = (float) base.getContributedEnergy(i);
            
            // Particles should include the trigger time offset
            // particles that were created above. When they are made,
            // they are placed into a particle map. This can be used
            // here to link the original particle to the new version.
            particles[i] = particleMap.get(base.getMCParticle(i));
        }
        
        // Create the new cloned hit.
        SimCalorimeterHit clone = new BaseSimCalorimeterHit(base.getCellID(), base.getRawEnergy(), base.getTime(),
                particles, energies, times, pdgs, base.getMetaData());
        
        // Shift the time of the hit by the specified offset.
        ((BaseSimCalorimeterHit) clone).shiftTime(timeOffset);
        
        // Add it to the hit map.
        calorimeterHitMap.put(base, clone);
        
        // Return the hit.
        return clone;
    }
    
    private static final MCParticle getCorrectedParticle(MCParticle base, double offset) {
        // If the particle has already been converted, it will exist
        // in the particle map and does not need to be converted a
        // second time.
        if(particleMap.containsKey(base)) {
            return particleMap.get(base);
        }
        
        // Otherwise, a new particle should be created.
        BaseMCParticle clone = new BaseMCParticle(base.getOrigin(), base.asFourVector(), base.getType(), base.getGeneratorStatus(), base.getProductionTime());
        clone.setCharge(base.getCharge());
        clone.setEndPoint(base.getEndPoint());
        clone.setMass(base.getMass());
        clone.setMomentumAtEndpoint(base.getMomentumAtEndpoint());
        clone.setProductionTime(offset + base.getProductionTime());
        clone.setSimulatorStatus(base.getSimulatorStatus());
        
        // Add the particle to the particle map.
        particleMap.put(base, clone);
        
        // Clone each of the daughter particles in the same manner,
        // and add them to this particle.
        for(MCParticle daughter : base.getDaughters()) {
            clone.addDaughter(getCorrectedParticle(daughter, offset));
        }
        
        // Return the clone.
        return clone;
    }
    
    private static final MCParticle getRootParticle(MCParticle particle) {
        MCParticle rootParticle = particle;
        while(particle.getParents().size() == 1) {
            rootParticle = particle.getParents().get(0);
        }
        
        if(particle.getParents().isEmpty()) {
            return rootParticle;
        } else {
            throw new IllegalArgumentException("Error: Particle has " + particle.getParents().size() + " parents -- expected either 0 or 1.");
        }
    }
    
    private static final void storeParticleTree(MCParticle root) {
        outputParticles.add(root);
        addChildrenToSet(root, outputParticles);
    }
    
    private static final void addChildrenToSet(MCParticle root, Set<MCParticle> particleSet) {
        for(MCParticle daughter : root.getDaughters()) {
            particleSet.add(daughter);
            if(!daughter.getDaughters().isEmpty()) {
                addChildrenToSet(daughter, particleSet);
            }
        }
    }
    
    @Override
    protected double getTimeDisplacement() {
        return 0;
    }
    
    @Override
    protected double getTimeNeededForLocalOutput() {
        return 0;
    }
}