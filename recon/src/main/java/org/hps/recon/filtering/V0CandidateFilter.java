package org.hps.recon.filtering;

import static java.lang.Math.abs;
import java.util.List;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.particle.ReconParticleDriver;
import org.hps.record.epics.EpicsData;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;

/**
 * Class to strip off trident candidates. Currently defined as: e+ e- events
 * with tracks. If the tight constraint is enabled, tracks must be matched to
 * clusters and the Ecal cluster times must be within _timingCut [2.5ns] of each
 * other.
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class V0CandidateFilter extends EventReconFilter {

    private String _V0CandidateCollectionName = "TargetConstrainedV0Candidates";
    private double _clusterTimingCut = 2.5;

    private boolean _tight = false;
    private boolean _keepEpicsDataEvents = false;

    @Override
    protected void process(EventHeader event) {
        incrementEventProcessed();
        if (_keepEpicsDataEvents) {
            // don't drop any events with EPICS data:
            final EpicsData data = EpicsData.read(event);
            if (data != null) {
                incrementEventPassed();
                return;
            }
        }
        if (!event.hasCollection(ReconstructedParticle.class, _V0CandidateCollectionName)) {
            skipEvent();
        }
        List<ReconstructedParticle> V0Candidates = event.get(ReconstructedParticle.class, _V0CandidateCollectionName);
        if (V0Candidates.isEmpty()) {
            skipEvent();
        }

        // tight requires ONLY ONE real vertex fit 
        if (_tight) {
            if (V0Candidates.size() != 2) {
                skipEvent();
            }
            for (ReconstructedParticle rp : V0Candidates) {

                ReconstructedParticle electron;
                ReconstructedParticle positron;

                List<ReconstructedParticle> fsParticles = rp.getParticles();
                if (fsParticles.size() != 2) {
                    skipEvent();
                }
                // require both electrons to be associated with an ECal cluster
                electron = fsParticles.get(ReconParticleDriver.ELECTRON);
                if (electron.getClusters().isEmpty()) {
                    skipEvent();
                }
                positron = fsParticles.get(ReconParticleDriver.POSITRON);
                if (positron.getClusters().isEmpty()) {
                    skipEvent();
                }

                // calorimeter cluster timing cut
                // first CalorimeterHit in the list is the seed crystal
                double t1 = ClusterUtilities.getSeedHitTime(electron.getClusters().get(0));
                double t2 = ClusterUtilities.getSeedHitTime(positron.getClusters().get(0));

                if (abs(t1 - t2) > _clusterTimingCut) {
                    skipEvent();
                }
            }
        }
        incrementEventPassed();
    }

    /**
     * Maximum difference in Calorimeter Cluster Seed Hit times [ns]
     *
     * @param d
     */
    public void setClusterTimingCut(double d) {
        _clusterTimingCut = d;
    }

    /**
     * Name of V0 Candidate ReconstructedParticle Collection Name
     *
     * @param s
     */
    public void setV0CandidateCollectionName(String s) {
        _V0CandidateCollectionName = s;
    }

    /**
     * Setting a tight constraint requires one and only one candidate in the
     * event
     *
     * @param b
     */
    public void setTightConstraint(boolean b) {
        _tight = b;
    }

    /**
     * Setting this true keeps ALL events containing EPICS data
     *
     * @param b
     */
    public void setKeepEpicsDataEvents(boolean b) {
        _keepEpicsDataEvents = b;
    }
}
