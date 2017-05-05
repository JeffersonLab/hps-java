package org.hps.recon.filtering;

import static java.lang.Math.abs;
import java.util.List;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.particle.ReconParticleDriver;
import org.hps.recon.tracking.TrackData;
import org.hps.recon.tracking.TrackType;
import org.hps.record.epics.EpicsData;
import org.hps.record.scalers.ScalerData;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.geometry.Detector;

/**
 * Class to strip off trident candidates. Currently defined as: e+ e- events
 * with tracks; track and vertex chi2 must be better than values defined by
 * cuts, and track times must be within trackDtCut of each other. If the tight
 * constraint is enabled, tracks must be matched to clusters, the Ecal cluster
 * times must be within _timingCut [2.5ns] of each other, and there must be
 * exactly one V0 passing all cuts.
 *
 * Only GBL vertices are considered.
 */
public class V0CandidateFilter extends EventReconFilter {

    private String _V0CandidateCollectionName = "UnconstrainedV0Candidates";
    private double _clusterTimingCut = 20.0;
    private double v0Chi2Cut = 100.0;
    private double trackChi2Cut = 80.0;
    private double trackDtCut = 20.0;
    private double trackPMax = 0.9;
    private double v0PMax = 1.4;

    private boolean _tight = false;
    private boolean _keepEpicsDataEvents = false;

    @Override
    protected void process(EventHeader event) {
        incrementEventProcessed();
        if (_keepEpicsDataEvents) {
            // don't drop any events with EPICS or scaler data:
            final EpicsData epicsData = EpicsData.read(event);
            if (epicsData != null) {
                incrementEventPassed();
                return;
            }

            final ScalerData scalerData = ScalerData.read(event);
            if (scalerData != null) {
                incrementEventPassed();
                return;
            }
        }
        if (!event.hasCollection(ReconstructedParticle.class, _V0CandidateCollectionName)) {
            skipEvent();
        }
        List<ReconstructedParticle> V0Candidates = event.get(ReconstructedParticle.class, _V0CandidateCollectionName);
        int nV0 = 0; //number of good V0
        for (ReconstructedParticle v0 : V0Candidates) {
            ReconstructedParticle electron = v0.getParticles().get(ReconParticleDriver.ELECTRON);
            ReconstructedParticle positron = v0.getParticles().get(ReconParticleDriver.POSITRON);

            if (!TrackType.isGBL(v0.getType())) { //we only care about GBL vertices
                continue;
            }
            if (v0.getStartVertex().getChi2() > v0Chi2Cut) {
                continue;
            }
            if (electron.getTracks().get(0).getChi2() > trackChi2Cut || positron.getTracks().get(0).getChi2() > trackChi2Cut) {
                continue;
            }
            if (electron.getMomentum().magnitude() > trackPMax || positron.getMomentum().magnitude() > trackPMax) {
                continue;
            }
            if (v0.getMomentum().magnitude() > v0PMax) {
                continue;
            }
            double eleTime = TrackData.getTrackTime(TrackData.getTrackData(event, electron.getTracks().get(0)));
            double posTime = TrackData.getTrackTime(TrackData.getTrackData(event, positron.getTracks().get(0)));
            if (Math.abs(eleTime - posTime) > trackDtCut) {
                continue;
            }
            if (_tight) { // tight requires cluster matches and cluster time cut
                if (electron.getClusters().isEmpty() || positron.getClusters().isEmpty()) {
                    continue;
                }
                // calorimeter cluster timing cut
                // first CalorimeterHit in the list is the seed crystal
                double t1 = ClusterUtilities.getSeedHitTime(electron.getClusters().get(0));
                double t2 = ClusterUtilities.getSeedHitTime(positron.getClusters().get(0));

                if (abs(t1 - t2) > _clusterTimingCut) {
                    continue;
                }
            }
            nV0++;
        }
        if (nV0 == 0) {
            skipEvent();
        }
        // tight requires ONLY ONE candidate vertex
        if (_tight && nV0 != 1) {
            skipEvent();
        }
        incrementEventPassed();
    }

    /**
     * Maximum vertex chi2 for a V0 to be counted.
     *
     * @param v0Chi2Cut default of 10.0.
     */
    public void setV0Chi2Cut(double v0Chi2Cut) {
        this.v0Chi2Cut = v0Chi2Cut;
    }

    /**
     * Maximum track chi2 for a V0 to be counted. A V0 is rejected if either of
     * the final state tracks has a chi2 exceeding the cut.
     *
     * @param trackChi2Cut default of 20.0.
     */
    public void setTrackChi2Cut(double trackChi2Cut) {
        this.trackChi2Cut = trackChi2Cut;
    }

    /**
     * Maximum track time different for a V0 to be counted.
     *
     * @param trackDtCut units of ns, default of 5.0
     */
    public void setTrackDtCut(double trackDtCut) {
        this.trackDtCut = trackDtCut;
    }

    /**
     * Maximum track momentum for a V0 to be counted. A V0 is rejected if either
     * of the final state tracks has momentum exceeding this cut.
     *
     * @param trackPMax units of GeV, default of 0.9
     */
    public void setTrackPMax(double trackPMax) {
        this.trackPMax = trackPMax;
    }

    public void setV0PMax(double v0PMax) {
        this.v0PMax = v0PMax;
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
    
    protected void detectorChanged(Detector detector){
          super.detectorChanged(detector);
      }
}
