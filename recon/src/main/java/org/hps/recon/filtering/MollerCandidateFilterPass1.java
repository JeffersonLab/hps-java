package org.hps.recon.filtering;

import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;

/**
 * Moller skim driver to use for pass1.  
 * @author spaul
 *
 */
public class MollerCandidateFilterPass1 extends EventReconFilter{
    private String mollerCollectionName = "TargetConstrainedMollerCandidates";
    /**
     * sets the moller candidate collection to use.  
     * @param val
     */
    public void setMollerCollectionName(String val){
        this.mollerCollectionName = val;
    }

    /**
     * sets the minimum momentum of a track, divided by the beam energy
     * @param val 
     */
    public void setTrackMomentumMaxFraction(double val){
        this.trackPMax = val;
    }

    /**
     * sets the minimum momentum of a track, divided by the beam energy
     * @param val 
     */
    public void setTrackMomentumMinFraction(double val){
        this.trackPMin = val;
    }

    /**
     * sets the maximum momentum sum of the pair of tracks, divided by the beam energy
     * @param val 
     */
    public void setMomentumSumMaxFraction(double val){
        this.sumPMax = val;
    }
    /**
     * sets the minimum momentum sum of the pair of tracks, divided by the beam energy
     * @param val 
     */
    public void setMomentumSumMinFraction(double val){
        this.sumPMin = val;
    }

    //All of these cuts are relative to the beam energy.  
    //For example, for 2.306 GeV, an FEE cut of .82 is 1.9 GeV.   
    private double trackPMin = 0;
    private double trackPMax = .82;
    private double sumPMax = 1.3;
    private double sumPMin = .65;

    public void process(EventHeader event){
        incrementEventProcessed();
        List<ReconstructedParticle> mollers = event.get(ReconstructedParticle.class, mollerCollectionName);
        if(mollers.size() == 0)
            skipEvent();



        boolean found = false;
        for(ReconstructedParticle mol : mollers){
            if(mol.getType()<32)
                continue;
            double p1 = mol.getParticles().get(0).getMomentum().magnitude()/beamEnergy;
            double p2 = mol.getParticles().get(1).getMomentum().magnitude()/beamEnergy;
            if(p1 >trackPMin && p1<trackPMax && p2 > trackPMin && p2 < trackPMax && p1 + p2 < sumPMax && p1 + p2 > sumPMin)
                found = true;

        }
        if(!found)
            skipEvent();

        incrementEventPassed();
    }
}
