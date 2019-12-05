package org.hps.analysis.MC;

import org.lcsim.event.MCParticle;

public class TruthContribution implements Comparable<TruthContribution> {
    private final int pdgID;
    private final float truthTime;
    private final float truthEnergy;
    private final MCParticle truthParticle;
    
    public TruthContribution(MCParticle particle, float energy, float time, int pdgID) {
        this.pdgID = pdgID;
        this.truthTime = time;
        this.truthEnergy = energy;
        this.truthParticle = particle;
    }
    
    @Override
    public int compareTo(TruthContribution arg0) {
        if(Float.compare(truthEnergy, arg0.truthEnergy) != 0) {
            return Float.compare(truthEnergy, arg0.truthEnergy);
        } else if(Float.compare(truthTime, arg0.truthTime) != 0) {
            return Float.compare(truthTime, arg0.truthTime);
        } else {
            return Integer.compare(pdgID, arg0.pdgID);
        }
    }
    
    public int getPDGID() { return pdgID; }
    
    public float getTime() { return truthTime; }
    
    public float getEnergy() { return truthEnergy; }
    
    public MCParticle getParticle() { return truthParticle; }
}