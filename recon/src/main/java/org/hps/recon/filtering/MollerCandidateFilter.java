package org.hps.recon.filtering;

import static java.lang.Math.abs;
import java.util.List;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;

/**
 * Class to strip off Moller candidates. Currently defined as: e- e- events with
 * tracks matched to clusters. Neither electron can be a full-energy candidate
 * (momentum less than _fullEnergyCut [0.85GeV]) but the momentum sum must be
 * consistent with the beam energy (greater than _mollerMomentumSumMin and less
 * than _mollerMomentumSumMax). The Ecal cluster times must be within _timingCut
 * [2.5ns] of each other.
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class MollerCandidateFilter extends EventReconFilter
{

    private String _mollerCandidateCollectionName = "TargetConstrainedMollerCandidates";
    private double _mollerMomentumSumMin = 0.85;
    private double _mollerMomentumSumMax = 1.3;
    private double _fullEnergyCut = 0.85;
    private double _clusterTimingCut = 2.5;

    @Override
    protected void process(EventHeader event)
    {
        incrementEventProcessed();
        if (!event.hasCollection(ReconstructedParticle.class, _mollerCandidateCollectionName)) {
            skipEvent();
        }
        List<ReconstructedParticle> mollerCandidates = event.get(ReconstructedParticle.class, _mollerCandidateCollectionName);
        if (mollerCandidates.size() == 0) {
            skipEvent();
        }

        for (ReconstructedParticle rp : mollerCandidates) {

            ReconstructedParticle e1 = null;
            ReconstructedParticle e2 = null;

            List<ReconstructedParticle> electrons = rp.getParticles();
            if (electrons.size() != 2) {
                skipEvent();
            }
            // require both electrons to be associated with an ECal cluster
            e1 = electrons.get(0);
            if (e1.getClusters().size() == 0) {
                skipEvent();
            }
            e2 = electrons.get(1);
            if (e2.getClusters().size() == 0) {
                skipEvent();
            }
            // remove full energy electrons
            double p1 = e1.getMomentum().magnitude();
            if (p1 > _fullEnergyCut) {
                skipEvent();
            }
            double p2 = e2.getMomentum().magnitude();
            if (p2 > _fullEnergyCut) {
                skipEvent();
            }

            // require momentum sum to be approximately the beam energy
            double pSum = p1 + p2;
            if (pSum < _mollerMomentumSumMin || pSum > _mollerMomentumSumMax) {
                skipEvent();
            }

            // calorimeter cluster timing cut
            // first CalorimeterHit in the list is the seed crystal
            double t1 = e1.getClusters().get(0).getCalorimeterHits().get(0).getTime();
            double t2 = e2.getClusters().get(0).getCalorimeterHits().get(0).getTime();

            if (abs(t1 - t2) > _clusterTimingCut) {
                skipEvent();
            }
            incrementEventPassed();
        }
    }

    /**
     * Maximum difference in Calorimeter Cluster Seed Hit times [ns]
     *
     * @param d
     */
    public void setClusterTimingCut(double d)
    {
        _clusterTimingCut = d;
    }

    /**
     * Name of Moller Candidate ReconstructedParticle Collection Name
     *
     * @param s
     */
    public void setMollerCandidateCollectionName(String s)
    {
        _mollerCandidateCollectionName = s;
    }

    /**
     * Minimum value for the sum of the two electron momenta [GeV]
     *
     * @param d
     */
    public void setMollerMomentumSumMin(double d)
    {
        _mollerMomentumSumMin = d;
    }

    /**
     * Maximum value for the sum of the two electron momenta [GeV]
     *
     * @param d
     */
    public void setMollerMomentumSumMax(double d)
    {
        _mollerMomentumSumMax = d;
    }

    /**
     * Maximum value for each of two electron momenta (removes full energy
     * electrons) [GeV]
     *
     * @param d
     */
    public void setMollerMomentumMax(double d)
    {
        _fullEnergyCut = d;
    }
}
