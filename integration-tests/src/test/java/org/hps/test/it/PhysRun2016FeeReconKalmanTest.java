package org.hps.test.it;

import java.util.List;

import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.tracking.TrackData;
import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackUtils;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.math.chisq.ChisqProb;

import hep.aida.IHistogram1D;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

/**
 * Test FEE reconstruction for 2016 physics run
 */
public class PhysRun2016FeeReconKalmanTest extends ReconTest {

    static final String DETECTOR = "HPS-PhysicsRun2016-v5-3-fieldmap_v4_globalAlign";
    static final String TEST_FILE_NAME = "hps_007796_feeskim.evio";
    static final String STEERING = "/org/hps/steering/recon/PhysicsRun2016FullRecon_KF_TrackClusterMatcher.lcsim";
    static final int NEVENTS = 5000;
    static final long MAX_EVENT_TIME = -1L;

    public PhysRun2016FeeReconKalmanTest() {
        super(PhysRun2016FeeReconKalmanTest.class,
                DETECTOR,
                TEST_FILE_NAME,
                STEERING,
                NEVENTS,
                new PlotDriver(),
                DEFAULT_TOLERANCE,
                MAX_EVENT_TIME,
                true,
                true);
    }

    private static class PlotDriver extends RefDriver {

        IHistogram1D trkChisqNdfTop = aida.histogram1D("Top Track Chisq per DoF", 100, 0., 100.);
        IHistogram1D trkChisqProbTop = aida.histogram1D("Top Track Chisq Prob", 100, 0., 1.);
        IHistogram1D trkNhitsTop = aida.histogram1D("Top Track Number of Hits", 15, -0.5, 14.5);
        IHistogram1D trkMomentumTop = aida.histogram1D("Top Track Momentum", 200, 0., 3.);
        IHistogram1D trkMomentumTopLT10 = aida.histogram1D("Top <10 Hit Track Momentum", 200, 0., 3.);
        IHistogram1D trkMomentumTopGE10 = aida.histogram1D("Top >=10 Hit Track Momentum", 200, 0., 3.);
        IHistogram1D trkdEdXTopLT10 = aida.histogram1D("Top <10 Hit Track dEdx", 100, 0., .00015);
        IHistogram1D trkdEdXTopGE10 = aida.histogram1D("Top >=10 Hit Track dEdx", 100, 0., .00015);
        IHistogram1D trkthetaTop = aida.histogram1D("Top Track theta", 100, 0.01, 0.05);
        IHistogram1D trkX0Top = aida.histogram1D("Top Track X0", 100, -0.5, 0.5);
        IHistogram1D trkY0Top = aida.histogram1D("Top Track Y0", 100, -5.0, 5.0);
        IHistogram1D trkZ0Top = aida.histogram1D("Top Track Z0", 100, -1.0, 1.0);

        IHistogram1D trkChisqNdfBottom = aida.histogram1D("Bottom Track Chisq per DoF", 100, 0., 100.);
        IHistogram1D trkChisqProbBottom = aida.histogram1D("Bottom Track Chisq Prob", 100, 0., 1.);
        IHistogram1D trkNhitsBottom = aida.histogram1D("Bottom Track Number of Hits", 15, -0.5, 14.5);
        IHistogram1D trkMomentumBottom = aida.histogram1D("Bottom Track Momentum", 200, 0., 3.);
        IHistogram1D trkMomentumBottomLT10 = aida.histogram1D("Bottom <10 Hit Track Momentum", 200, 0., 3.);
        IHistogram1D trkMomentumBottomGE10 = aida.histogram1D("Bottom >=10 Hit Track Momentum", 200, 0., 3.);
        IHistogram1D trkdEdXBottomLT10 = aida.histogram1D("Bottom <10 Hit Track dEdx", 100, 0., .00015);
        IHistogram1D trkdEdXBottomGE10 = aida.histogram1D("Bottom >=10 Hit Track dEdx", 100, 0., .00015);
        IHistogram1D trkthetaBottom = aida.histogram1D("Bottom Track theta", 100, 0.01, 0.05);
        IHistogram1D trkX0Bottom = aida.histogram1D("Bottom Track X0", 100, -0.5, 0.5);
        IHistogram1D trkY0Bottom = aida.histogram1D("Bottom Track Y0", 100, -5.0, 5.0);
        IHistogram1D trkZ0Bottom = aida.histogram1D("Bottom Track Z0", 100, -1.0, 1.0);

        final String finalStateParticlesColName = "OtherElectrons";

        Double beamEnergy = 2.306;

        // Set min seed energy value
        final double seedCut = 1.2;

        // Set min cluster energy value
        double clusterCut = 1.6;

        // Minimum number of hits per cluster
        int minHits = 5;

        // Seed time cuts
        double ctMin = 55.;
        double ctMax = 61.;

        protected void process(EventHeader event) {

            super.process(event);

            // only keep singles triggers:
            if (!event.hasCollection(GenericObject.class, "TriggerBank")) {
                return;
            }
            boolean isSingles = false;
            for (GenericObject gob : event.get(GenericObject.class, "TriggerBank")) {
                if (!(AbstractIntData.getTag(gob) == TIData.BANK_TAG)) {
                    continue;
                }
                TIData tid = new TIData(gob);
                if (tid.isSingle0Trigger() || tid.isSingle1Trigger()) {
                    isSingles = true;
                    break;
                }
            }
            if (!isSingles) {
                return;
            }
            if (!event.hasCollection(ReconstructedParticle.class, finalStateParticlesColName)) {
                return;
            }
            List<ReconstructedParticle> rpList = event.get(ReconstructedParticle.class, finalStateParticlesColName);
            for (ReconstructedParticle rp : rpList) {
                if (TrackType.isGBL(rp.getType())) {
                    continue;
                }
                if (rp.getMomentum().magnitude() > 1.5 * beamEnergy) {
                    continue;
                }
                // require both track and cluster
                if (rp.getClusters().size() != 1) {
                    continue;
                }

                if (rp.getTracks().size() != 1) {
                    continue;
                }

                Track t = rp.getTracks().get(0);
                double p = rp.getMomentum().magnitude();
                Cluster c = rp.getClusters().get(0);
                // debug diagnostics to set cuts
                if (debug) {
                    aida.cloud1D("clusterSeedHit energy").fill(ClusterUtilities.findSeedHit(c).getCorrectedEnergy());
                    aida.cloud1D("cluster nHits").fill(c.getCalorimeterHits().size());
                    aida.cloud2D("clusterSeedHit energy vs p").fill(p, ClusterUtilities.findSeedHit(c).getCorrectedEnergy());
                    aida.cloud2D("cluster nHits vs p").fill(p, c.getCalorimeterHits().size());
                    aida.cloud2D("cluster time vs p").fill(p, ClusterUtilities.getSeedHitTime(c));
                }
                double ct = ClusterUtilities.getSeedHitTime(c);

                if (c.getEnergy() > clusterCut
                        && ClusterUtilities.findSeedHit(c).getCorrectedEnergy() > seedCut
                        && c.getCalorimeterHits().size() >= minHits
                        && ct > ctMin
                        && ct < ctMax) {
                    double chiSquared = t.getChi2();
                    int ndf = t.getNDF();
                    double chi2Ndf = t.getChi2() / t.getNDF();
                    double chisqProb = ChisqProb.gammp(ndf, chiSquared);
                    int nHits = t.getTrackerHits().size();
                    double dEdx = t.getdEdx();
                    //rotate into physiscs frame of reference
                    Hep3Vector rprot = VecOp.mult(BEAM_AXIS_ROTATION, rp.getMomentum());
                    double theta = Math.acos(rprot.z() / rprot.magnitude());

                    // debug diagnostics to set cuts
                    if (debug) {
                        aida.cloud1D("Track chisq per df").fill(chiSquared / ndf);
                        aida.cloud1D("Track chisq prob").fill(chisqProb);
                        //aida.cloud1D("Track nHits").fill(t.getTrackerHits().size());
                        aida.cloud1D("Track momentum").fill(p);
                        aida.cloud1D("Track deDx").fill(t.getdEdx());
                        aida.cloud1D("Track theta").fill(theta);
                        aida.cloud2D("Track theta vs p").fill(theta, p);
                        aida.cloud1D("rp x0").fill(TrackUtils.getX0(t));
                        aida.cloud1D("rp y0").fill(TrackUtils.getY0(t));
                        aida.cloud1D("rp z0").fill(TrackUtils.getZ0(t));
                    }

                    double trackDataTime = TrackData.getTrackTime(TrackData.getTrackData(event, t));
                    aida.cloud1D("track data time").fill(trackDataTime);
                    if (isTopTrack(t)) {
                        trkChisqNdfTop.fill(chi2Ndf);
                        trkChisqProbTop.fill(chisqProb);
                        trkNhitsTop.fill(nHits);
                        trkMomentumTop.fill(p);
                        trkthetaTop.fill(theta);
                        trkX0Top.fill(TrackUtils.getX0(t));
                        trkY0Top.fill(TrackUtils.getY0(t));
                        trkZ0Top.fill(TrackUtils.getZ0(t));

                        if (nHits < 10) {
                            trkMomentumTopLT10.fill(p);
                            trkdEdXTopLT10.fill(dEdx);
                        } else if (nHits >=10 ) {
                            trkMomentumTopGE10.fill(p);
                            trkdEdXTopGE10.fill(dEdx);
                        }
                    } else {
                        trkChisqNdfBottom.fill(chi2Ndf);
                        trkChisqProbBottom.fill(chisqProb);
                        trkNhitsBottom.fill(nHits);
                        trkMomentumBottom.fill(p);
                        trkthetaBottom.fill(theta);
                        trkX0Bottom.fill(TrackUtils.getX0(t));
                        trkY0Bottom.fill(TrackUtils.getY0(t));
                        trkZ0Bottom.fill(TrackUtils.getZ0(t));

                        if (nHits<10 ) {
                            trkMomentumBottomLT10.fill(p);
                            trkdEdXBottomLT10.fill(dEdx);
                        } else if (nHits>=10) {
                            trkMomentumBottomGE10.fill(p);
                            trkdEdXBottomGE10.fill(dEdx);
                        }
                    }
                } // end of cluster cuts
            }
        }
    }
}
