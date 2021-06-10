package org.hps.recon.filtering;

import static java.lang.Math.abs;
import java.util.ArrayList;
import java.util.List;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.record.epics.EpicsData;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.geometry.subdetector.HPSEcal3.NeighborMap;

/**
 * Class to strip off Moller candidates.
 *
 * Currently the tight selection is defined as: e- e- events with tracks matched
 * to each cluster. Neither electron can be a full-energy candidate (momentum
 * less than _fullEnergyCut [0.85GeV]) but the momentum sum must be consistent
 * with the beam energy (greater than _mollerMomentumSumMin and less than
 * _mollerMomentumSumMax). The Ecal cluster times must be within _timingCut
 * [2.5ns] of each other.
 * 
 * One can also set calorimeter-only cuts if one is interested in understanding
 * tracking issues.
 */
public class MollerCandidateFilter extends EventReconFilter
{

    private boolean _keepEpicsDataEvents = false;

    // the following are for the tight cuts
    private boolean _tight = false;
    private String _mollerCandidateCollectionName = "TargetConstrainedMollerCandidates";
    private double _mollerMomentumSumMin = 0.85;
    private double _mollerMomentumSumMax = 1.3;
    private double _fullEnergyCut = 0.85;
    //the following are for the calorimeter cluster cuts
    private String _mollerCandidateClusterCollectionName = "EcalClustersCorr";
    //first, single cluster cuts
    private double _clusterTimeLo = 40;
    private double _clusterTimeHi = 48;
    private double _clusterMaxX = 0.;
    // pair cuts
    private double _clusterXSumLo = -175.;
    private double _clusterXSumHi = -145.;
    private double _clusterXDiffLo = -80.;
    private double _clusterXDiffHi = 80.;
    private double _clusterESumLo = 0.85;
    private double _clusterESumHi = 1.1;
    private double _clusterEDiffLo = -0.3;
    private double _clusterEDiffHi = 0.3;

    private HPSEcal3 ecal;
    private NeighborMap neighborMap;

    private double _clusterDeltaTimeCut = 2.5;

    

    @Override
    protected void process(EventHeader event)
    {
        incrementEventProcessed();
        if (_keepEpicsDataEvents) {
            // don't drop any events with EPICS data:
            final EpicsData data = EpicsData.read(event);
            if (data != null) {
                incrementEventPassed();
                return;
            }
        }
        // tight requires two electron final state particles with a vertex fit 
        if (_tight) {
            if (!event.hasCollection(ReconstructedParticle.class, _mollerCandidateCollectionName)) {
                skipEvent();
            }
            List<ReconstructedParticle> mollerCandidates = event.get(ReconstructedParticle.class, _mollerCandidateCollectionName);
            if (mollerCandidates.size() == 0) {
                skipEvent();
            }
            boolean goodPair = false;
            for (ReconstructedParticle rp : mollerCandidates) {
                ReconstructedParticle e1 = null;
                ReconstructedParticle e2 = null;
                
                List<ReconstructedParticle> electrons = rp.getParticles();
                if (electrons.size() != 2) {
                    //skipEvent();
                    continue;
                }
                // require both electrons to be associated with an ECal cluster
                e1 = electrons.get(0);
                if (e1.getClusters().size() == 0) {
                    //skipEvent();
                    continue;
                }
                e2 = electrons.get(1);
                if (e2.getClusters().size() == 0) {
                    //skipEvent();
                    continue;
                }
                // remove full energy electrons
                double p1 = e1.getMomentum().magnitude();
                if (p1 > _fullEnergyCut) {
                    //skipEvent();
                    continue;
                }
                double p2 = e2.getMomentum().magnitude();
                if (p2 > _fullEnergyCut) {
                    //skipEvent();
                    continue;
                }
                // require momentum sum to be approximately the beam energy
                double pSum = p1 + p2;
                if (pSum < _mollerMomentumSumMin || pSum > _mollerMomentumSumMax) {
                    //skipEvent();
                    continue;
                }
                // calorimeter cluster timing cut
                // first CalorimeterHit in the list is the seed crystal
                double t1 = e1.getClusters().get(0).getCalorimeterHits().get(0).getTime();
                double t2 = e2.getClusters().get(0).getCalorimeterHits().get(0).getTime();
                if (abs(t1 - t2) > _clusterDeltaTimeCut) {
                    //skipEvent();
                    continue;
                }
                goodPair = true;
                break;
            }
            if(!goodPair)
                skipEvent();
        } // end of tight selection cuts
        else // apply only calorimeter-based cuts
        {
            if (!event.hasCollection(Cluster.class, _mollerCandidateClusterCollectionName)) {
                skipEvent();
            }
            List<Cluster> clusters = event.get(Cluster.class, _mollerCandidateClusterCollectionName);
            List<Cluster> goodClusters = new ArrayList<Cluster>();
            for (Cluster c : clusters) {
                // check that cluster is on electron side
                if (c.getPosition()[0] > _clusterMaxX) {
                    continue;
                }
                // check that cluster is in time window
                CalorimeterHit seedHit = c.getCalorimeterHits().get(0);
                double t = seedHit.getTime();
                if (t < _clusterTimeLo || t > _clusterTimeHi) {
                    continue;
                }
//                // remove edge clusters
//                int nNeighbors = neighborMap.get(seedHit.getCellID()).size();
//                if (nNeighbors < 8) {
//                    continue;
//                }
                // if we got here we have a "good" cluster
                goodClusters.add(c);
            } // end of loop looking for good clusters
            if (goodClusters.size() < 2) {
                skipEvent();
            }
            // should now have at least two good clusters, start looking at pairs
            boolean goodPair = false;
            out:
            for (int i = 0; i < goodClusters.size() - 1; ++i) {
                for (int j = i + 1; j < goodClusters.size(); ++j) {
                    Cluster c1 = goodClusters.get(i);
                    Cluster c2 = goodClusters.get(j);
                    double[] pos1 = c1.getPosition();
                    double[] pos2 = c2.getPosition();
                    // require clusters to be on opposite sides of y=0 (top/bottom)
                    if (pos1[1] * pos2[1] > 0) {
                        continue;
                    }
                    // cut on x position sum
                    if (pos1[0] + pos2[0] < _clusterXSumLo || pos1[0] + pos2[0] > _clusterXSumHi) {
                        continue;
                    }
                    // cut on x position diff
                    // TODO resolve this source of bias (should be abs)
                    if (pos1[0] - pos2[0] < _clusterXDiffLo || pos1[0] - pos2[0] > _clusterXDiffHi) {
                        continue;
                    }
                    // require energy sum to be close to beam energy
                    double e1 = c1.getEnergy();
                    double e2 = c2.getEnergy();
                    if (e1 + e2 < _clusterESumLo || e1 + e2 > _clusterESumHi) {
                        continue;
                    }
                    // require energy difference to be small
                    // TODO resolve this source of bias (should be abs)
                    if (e1 - e2 < _clusterEDiffLo || e1 - e2 > _clusterEDiffHi) {
                        continue;
                    }
                    // require both cluster times to be the same
                    double t1 = c1.getCalorimeterHits().get(0).getTime();
                    double t2 = c2.getCalorimeterHits().get(0).getTime();
                    if (abs(t1 - t2) > _clusterDeltaTimeCut) {
                        continue;
                    }
                    // if we got here we have a good pair
                    goodPair = true;
                    break out;
                } // end of j loop
            } // end of i loop
            if (!goodPair) {
                skipEvent();
            }
        }// end of calorimeter-only selection block
        incrementEventPassed();
    }

    /**
     * Maximum difference in Calorimeter Cluster Seed Hit times [ns]
     *
     * @param d
     */
    public void setClusterDeltaTimeCut(double d)
    {
        _clusterDeltaTimeCut = d;
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

    /**
     * Setting a tight constraint requires one and only one candidate in the
     * event
     *
     * @param b
     */
    public void setTightConstraint(boolean b)
    {
        _tight = b;
    }

    /**
     * Setting this true keeps ALL events containing EPICS data
     *
     * @param b
     */
    public void setKeepEpicsDataEvents(boolean b)
    {
        _keepEpicsDataEvents = b;
    }

    // Calorimeter-only selection cuts
    /**
     * Name of Moller Candidate Calorimeter Cluster Collection Name
     *
     * @param s
     */
    public void setMollerCandidateClusterCollectionName(String s)
    {
        _mollerCandidateClusterCollectionName = s;
    }

    // Individual Cluster selection cuts
    /**
     * Minimum value for the Cluster time [ns]
     *
     * @param d
     */
    public void setClusterTimeLo(double d)
    {
        _clusterTimeLo = d;
    }

    /**
     * Maximum value for the Cluster time [ns]
     *
     * @param d
     */
    public void setClusterTimeHi(double d)
    {
        _clusterTimeHi = d;
    }

    // position
    /**
     * Maximum value for the Cluster x position [mm]
     *
     * @param d
     */
    public void setClusterMaxX(double d)
    {
        _clusterMaxX = d;
    }

    // Cluster Pair selection cuts
    //position
    /**
     * Minimum value for the Cluster x position sum [mm]
     *
     * @param d
     */
    public void setClusterXSumLo(double d)
    {
        _clusterXSumLo = d;
    }

    /**
     * Maximum value for the Cluster x position sum [mm]
     *
     * @param d
     */
    public void setClusterXSumHi(double d)
    {
        _clusterXSumHi = d;
    }

    /**
     * Minimum value for the Cluster x position difference [mm]
     *
     * @param d
     */
    public void setClusterXDiffLo(double d)
    {
        _clusterXDiffLo = d;
    }

    /**
     * Maximum value for the Cluster x position difference [mm]
     *
     * @param d
     */
    public void setClusterXDiffHi(double d)
    {
        _clusterXDiffHi = d;
    }

    // energy
    /**
     * Minimum value for the Cluster Energy sum [mm]
     *
     * @param d
     */
    public void setClusterESumLo(double d)
    {
        _clusterESumLo = d;
    }

    /**
     * Maximum value for the Cluster Energy sum [mm]
     *
     * @param d
     */
    public void setClusterESumHi(double d)
    {
        _clusterESumHi = d;
    }

    /**
     * Minimum value for the Cluster Energy difference [mm]
     *
     * @param d
     */
    public void setClusterEDiffLo(double d)
    {
        _clusterEDiffLo = d;
    }

    /**
     * Maximum value for the Cluster Energy difference [mm]
     *
     * @param d
     */
    public void setClusterEDiffHi(double d)
    {
        _clusterEDiffHi = d;
    }

    protected void detectorChanged(Detector detector){
    
        super.detectorChanged(detector);
        ecal = (HPSEcal3) DatabaseConditionsManager.getInstance().getDetectorObject().getSubdetector("Ecal");
        neighborMap = ecal.getNeighborMap();
        
    }
}
