package org.hps.util;

import java.util.ArrayList;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader.LCMetaData;
import org.lcsim.event.base.BaseSimCalorimeterHit;

import hep.physics.vec.Hep3Vector;

public class TruthCalorimeterHit extends BaseSimCalorimeterHit {
    public TruthCalorimeterHit(double rawEnergy, double correctedEnergy, double energyError, double time, long id,
            Hep3Vector positionVec, int type, Object[] mcparts, float[] energies, float[] times, int[] pdgs, LCMetaData metaData) {
        // Instantiate the regular calorimeter hit fields.
        this.rawEnergy = rawEnergy;
        this.correctedEnergy = correctedEnergy;
        this.energyError = energyError;
        this.time = time;
        this.id = id;
        this.positionVec = positionVec;
        this.type = type;
        
        // Set the metadata.
        setMetaData(metaData);
        
        // Store the truth information.
        this.nContributions = mcparts.length;
        this.particle = mcparts;
        this.energyContrib = energies;
        this.times = times;
        this.pdg = pdgs;
        this.steps = new ArrayList<float[]>(nContributions);
    }
    
    public TruthCalorimeterHit(CalorimeterHit baseHit, Object[] mcparts, float[] energies, float[] times, int[] pdgs) {
        this(baseHit.getRawEnergy(), baseHit.getCorrectedEnergy(), baseHit.getEnergyError(), baseHit.getTime(),
                baseHit.getCellID(), baseHit.getPositionVec(), baseHit.getType(), mcparts, energies, times,
                pdgs, baseHit.getMetaData());
    }
    
    public void setTime(float newTime) {
        time = newTime;
    }
    
    @Override
    public double getTime() {
        // SimCalorimeterHit usually calculates time based on the
        // input particles. We would prefer that it simply return the
        // given time, as the time calculation is performed in a
        // different manner here.
        return time;
    }
}