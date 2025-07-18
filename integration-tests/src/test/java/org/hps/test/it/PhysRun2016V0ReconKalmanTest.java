package org.hps.test.it;

import static java.lang.Math.abs;

import java.util.List;

import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.tracking.TrackType;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.Vertex;
import org.lcsim.math.chisq.ChisqProb;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

/**
 * Test V0 reconstruction for 2016 physics run
 */
public class PhysRun2016V0ReconKalmanTest extends ReconTest {

    static final String DETECTOR = "HPS-PhysicsRun2016-v5-3-fieldmap_v4_globalAlign";
    static final String TEST_FILE_NAME = "hps_007796_v0skim.evio";
    static final String STEERING = "/org/hps/steering/recon/PhysicsRun2016FullRecon_KF_TrackClusterMatcher.lcsim";
    static final int NEVENTS = 5000;
    static final long MAX_EVENT_TIME = -1L;

    public PhysRun2016V0ReconKalmanTest() {
        super(PhysRun2016V0ReconKalmanTest.class,
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

        String[] vertexCollectionNames = {
                "UnconstrainedV0Vertices_KF", "BeamspotConstrainedV0Vertices_KF", "TargetConstrainedV0Vertices_KF"};

        Double beamEnergy = 2.306;
        double trackChi2NdfCut = 100.; //corresponds to chisquared cut of 40 for 5-hit tracks

        IHistogram1D invMassHist_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 Invariant Mass", 200, 0., 0.1);
        IHistogram1D pHist_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 Momentum", 200, 0., 3.0);
        IHistogram1D pxHist_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 x Momentum", 200, -0.01, 0.01);
        IHistogram1D pyHist_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 y Momentum", 200, -0.01, 0.01);
        IHistogram1D pzHist_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 z Momentum", 200, 0., 3.0);
        IHistogram1D trkpHist_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 Track Momentum", 100, 0.25, 1.75);
        IHistogram1D trkptopHist_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 Top Track Momentum", 100, 0.25, 1.75);
        IHistogram1D trkpbotHist_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 Bottom Track Momentum", 100, 0.25, 1.75);
        IHistogram1D trkNhitsHist_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 Track Number of Hits", 15, -0.5, 14.5);
        IHistogram1D trkChisqHist_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 Track Chisq per DoF", 100, 0.0, 20.0);
        IHistogram1D trkChisqProbHist_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 Track Chisq Prob", 100, 0.0, 1.0);
        IHistogram1D vtxXHist_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 Vertex x", 200, -2.5, 2.5);
        IHistogram1D vtxYHist_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 Vertex y", 200, -1.0, 1.0);
        IHistogram1D vtxZHist_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 Vertex z", 200, -20.0, 20.0);
        IHistogram1D vtxZHistL1L1_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 Vertex z L1L1", 200, -20.0, 20.0);
        IHistogram1D vtxChisqHist_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 Vertex Chisq", 100, 0.0, 100.0);

        IHistogram2D p1vsp2Hist_UnconstrainedV0Vertices = aida.histogram2D("UnconstrainedV0Vertices/V0 p1 vs p2", 200, 0.25, 1.75, 200, 0.25, 1.75);
        IHistogram2D ptopvspbotHist_UnconstrainedV0Vertices = aida.histogram2D("UnconstrainedV0Vertices/V0 p top vs p bottom", 200, 0.25, 1.75, 200, 0.25, 1.75);
        IHistogram2D theta1vstheta2Hist_UnconstrainedV0Vertices = aida.histogram2D("UnconstrainedV0Vertices/V0 theta1 vs theta2", 100, 0.01, 0.05, 100, 0.01, 0.05);
        IHistogram2D pvsthetaHist_UnconstrainedV0Vertices = aida.histogram2D("UnconstrainedV0Vertices/V0 p vs theta", 100, 0.25, 1.75, 100, 0.01, 0.05);
        IHistogram2D xvsyHist_UnconstrainedV0Vertices = aida.histogram2D("UnconstrainedV0Vertices/V0 vertex X vs Y", 250, -2.5, 2.5, 100, -1.0, 1.0);

        IHistogram1D invMassHist_BeamspotConstrainedV0Vertices = aida.histogram1D("BeamspotConstrainedV0Vertices/V0 Invariant Mass", 200, 0., 0.1);
        IHistogram1D pHist_BeamspotConstrainedV0Vertices = aida.histogram1D("BeamspotConstrainedV0Vertices/V0 Momentum", 200, 0., 3.0);
        IHistogram1D pxHist_BeamspotConstrainedV0Vertices = aida.histogram1D("BeamspotConstrainedV0Vertices/V0 x Momentum", 200, -0.01, 0.01);
        IHistogram1D pyHist_BeamspotConstrainedV0Vertices = aida.histogram1D("BeamspotConstrainedV0Vertices/V0 y Momentum", 200, -0.01, 0.01);
        IHistogram1D pzHist_BeamspotConstrainedV0Vertices = aida.histogram1D("BeamspotConstrainedV0Vertices/V0 z Momentum", 200, 0., 3.0);
        IHistogram1D trkpHist_BeamspotConstrainedV0Vertices = aida.histogram1D("BeamspotConstrainedV0Vertices/V0 Track Momentum", 100, 0.25, 1.75);
        IHistogram1D trkptopHist_BeamspotConstrainedV0Vertices = aida.histogram1D("BeamspotConstrainedV0Vertices/V0 Top Track Momentum", 100, 0.25, 1.75);
        IHistogram1D trkpbotHist_BeamspotConstrainedV0Vertices = aida.histogram1D("BeamspotConstrainedV0Vertices/V0 Bottom Track Momentum", 100, 0.25, 1.75);
        IHistogram1D trkNhitsHist_BeamspotConstrainedV0Vertices = aida.histogram1D("BeamspotConstrainedV0Vertices/V0 Track Number of Hits", 15, -0.5, 14.5);
        IHistogram1D trkChisqHist_BeamspotConstrainedV0Vertices = aida.histogram1D("BeamspotConstrainedV0Vertices/V0 Track Chisq per DoF", 100, 0.0, 20.0);
        IHistogram1D trkChisqProbHist_BeamspotConstrainedV0Vertices = aida.histogram1D("BeamspotConstrainedV0Vertices/V0 Track Chisq Prob", 100, 0.0, 1.0);
        IHistogram1D vtxXHist_BeamspotConstrainedV0Vertices = aida.histogram1D("BeamspotConstrainedV0Vertices/V0 Vertex x", 200, -2.5, 2.5);
        IHistogram1D vtxYHist_BeamspotConstrainedV0Vertices = aida.histogram1D("BeamspotConstrainedV0Vertices/V0 Vertex y", 200, -1.0, 1.0);
        IHistogram1D vtxZHist_BeamspotConstrainedV0Vertices = aida.histogram1D("BeamspotConstrainedV0Vertices/V0 Vertex z", 200, -20.0, 20.0);
        IHistogram1D vtxChisqHist_BeamspotConstrainedV0Vertices = aida.histogram1D("BeamspotConstrainedV0Vertices/V0 Vertex Chisq", 100, 0.0, 100.0);

        IHistogram2D p1vsp2Hist_BeamspotConstrainedV0Vertices = aida.histogram2D("BeamspotConstrainedV0Vertices/V0 p1 vs p2", 200, 0.25, 1.75, 200, 0.25, 1.75);
        IHistogram2D ptopvspbotHist_BeamspotConstrainedV0Vertices = aida.histogram2D("BeamspotConstrainedV0Vertices/V0 p top vs p bottom", 200, 0.25, 1.75, 200, 0.25, 1.75);
        IHistogram2D theta1vstheta2Hist_BeamspotConstrainedV0Vertices = aida.histogram2D("BeamspotConstrainedV0Vertices/V0 theta1 vs theta2", 100, 0.01, 0.05, 100, 0.01, 0.05);
        IHistogram2D pvsthetaHist_BeamspotConstrainedV0Vertices = aida.histogram2D("BeamspotConstrainedV0Vertices/V0 p vs theta", 100, 0.25, 1.75, 100, 0.01, 0.05);
        IHistogram2D xvsyHist_BeamspotConstrainedV0Vertices = aida.histogram2D("BeamspotConstrainedV0Vertices/V0 vertex X vs Y", 250, -2.5, 2.5, 100, -1.0, 1.0);

        IHistogram1D invMassHist_TargetConstrainedV0Vertices = aida.histogram1D("TargetConstrainedV0Vertices/V0 Invariant Mass", 200, 0., 0.1);
        IHistogram1D pHist_TargetConstrainedV0Vertices = aida.histogram1D("TargetConstrainedV0Vertices/V0 Momentum", 200, 0., 3.0);
        IHistogram1D pxHist_TargetConstrainedV0Vertices = aida.histogram1D("TargetConstrainedV0Vertices/V0 x Momentum", 200, -0.01, 0.01);
        IHistogram1D pyHist_TargetConstrainedV0Vertices = aida.histogram1D("TargetConstrainedV0Vertices/V0 y Momentum", 200, -0.01, 0.01);
        IHistogram1D pzHist_TargetConstrainedV0Vertices = aida.histogram1D("TargetConstrainedV0Vertices/V0 z Momentum", 200, 0., 3.0);
        IHistogram1D trkpHist_TargetConstrainedV0Vertices = aida.histogram1D("TargetConstrainedV0Vertices/V0 Track Momentum", 100, 0.25, 1.75);
        IHistogram1D trkptopHist_TargetConstrainedV0Vertices = aida.histogram1D("TargetConstrainedV0Vertices/V0 Top Track Momentum", 100, 0.25, 1.75);
        IHistogram1D trkpbotHist_TargetConstrainedV0Vertices = aida.histogram1D("TargetConstrainedV0Vertices/V0 Bottom Track Momentum", 100, 0.25, 1.75);
        IHistogram1D trkNhitsHist_TargetConstrainedV0Vertices = aida.histogram1D("TargetConstrainedV0Vertices/V0 Track Number of Hits", 15, -0.5, 14.5);
        IHistogram1D trkChisqHist_TargetConstrainedV0Vertices = aida.histogram1D("TargetConstrainedV0Vertices/V0 Track Chisq per DoF", 100, 0.0, 20.0);
        IHistogram1D trkChisqProbHist_TargetConstrainedV0Vertices = aida.histogram1D("TargetConstrainedV0Vertices/V0 Track Chisq Prob", 100, 0.0, 1.0);
        IHistogram1D vtxXHist_TargetConstrainedV0Vertices = aida.histogram1D("TargetConstrainedV0Vertices/V0 Vertex x", 200, -2.5, 2.5);
        IHistogram1D vtxYHist_TargetConstrainedV0Vertices = aida.histogram1D("TargetConstrainedV0Vertices/V0 Vertex y", 200, -1.0, 1.0);
        IHistogram1D vtxZHist_TargetConstrainedV0Vertices = aida.histogram1D("TargetConstrainedV0Vertices/V0 Vertex z", 200, -20.0, 20.0);
        IHistogram1D vtxChisqHist_TargetConstrainedV0Vertices = aida.histogram1D("TargetConstrainedV0Vertices/V0 Vertex Chisq", 100, 0.0, 100.0);

        IHistogram2D p1vsp2Hist_TargetConstrainedV0Vertices = aida.histogram2D("TargetConstrainedV0Vertices/V0 p1 vs p2", 200, 0.25, 1.75, 200, 0.25, 1.75);
        IHistogram2D ptopvspbotHist_TargetConstrainedV0Vertices = aida.histogram2D("TargetConstrainedV0Vertices/V0 p top vs p bottom", 200, 0.25, 1.75, 200, 0.25, 1.75);
        IHistogram2D theta1vstheta2Hist_TargetConstrainedV0Vertices = aida.histogram2D("TargetConstrainedV0Vertices/V0 theta1 vs theta2", 100, 0.01, 0.05, 100, 0.01, 0.05);
        IHistogram2D pvsthetaHist_TargetConstrainedV0Vertices = aida.histogram2D("TargetConstrainedV0Vertices/V0 p vs theta", 100, 0.25, 1.75, 100, 0.01, 0.05);
        IHistogram2D xvsyHist_TargetConstrainedV0Vertices = aida.histogram2D("TargetConstrainedV0Vertices/V0 vertex X vs Y", 250, -2.5, 2.5, 100, -1.0, 1.0);

        protected void process(EventHeader event) {
            super.process(event);
            for (String vertexCollectionName : vertexCollectionNames) {
                List<Vertex> vertices = event.get(Vertex.class, vertexCollectionName);
                for (Vertex v : vertices) {
                    ReconstructedParticle rp = v.getAssociatedParticle();
                    int type = rp.getType();
                    boolean isGbl = TrackType.isGBL(type);
                    // require Kalman tracks in vertex
                    if (!isGbl) {
                        List<ReconstructedParticle> parts = rp.getParticles();
                        ReconstructedParticle rp1 = parts.get(0);
                        ReconstructedParticle rp2 = parts.get(1);
                        // basic sanity check here, remove full energy electrons (fee)
                        if (rp1.getMomentum().magnitude() > 1.5 * beamEnergy || rp2.getMomentum().magnitude() > 1.5 * beamEnergy) {
                            continue;
                        }
                        // require both reconstructed particles to have a track and a cluster
                        if (rp1.getClusters().size() != 1) {
                            continue;
                        }
                        if (rp2.getClusters().size() != 1) {
                            continue;
                        }
                        if (rp1.getTracks().size() != 1) {
                            continue;
                        }
                        if (rp2.getTracks().size() != 1) {
                            continue;
                        }
                        Track t1 = rp1.getTracks().get(0);
                        Track t2 = rp2.getTracks().get(0);
                        Cluster c1 = rp1.getClusters().get(0);
                        Cluster c2 = rp2.getClusters().get(0);
                        double deltaT = ClusterUtilities.getSeedHitTime(c1) - ClusterUtilities.getSeedHitTime(c2);
                        // require cluster times to be coincident within 2 ns
                        if (abs(deltaT) > 2.0) {
                            continue;
                        }
                        //rotate into physiscs frame of reference
                        Hep3Vector rprot = VecOp.mult(BEAM_AXIS_ROTATION, rp.getMomentum());
                        Hep3Vector p1rot = VecOp.mult(BEAM_AXIS_ROTATION, rp1.getMomentum());
                        Hep3Vector p2rot = VecOp.mult(BEAM_AXIS_ROTATION, rp2.getMomentum());
                        double theta1 = Math.acos(p1rot.z() / p1rot.magnitude());
                        double theta2 = Math.acos(p2rot.z() / p2rot.magnitude());
                        double t1ChisqNdf = t1.getChi2() / t1.getNDF();
                        double t2ChisqNdf = t2.getChi2() / t2.getNDF();

                        double t1ChisqProb = ChisqProb.gammp(t1.getNDF(), t1.getChi2());
                        double t2ChisqProb = ChisqProb.gammp(t2.getNDF(), t2.getChi2());
                        // used to cut on prob < 0.995, which corresponds to roughly 3.4
                        // change this to a cut on chi-squared/dof which people are more familiar with.
                        // Omar currently cuts on chi-squared <40(!), irrespective of 5 or 6 hit tracks
                        // let's start at chisq/dof of 8
                        if (t1ChisqNdf > trackChi2NdfCut) {//(t1ChisqProb > 0.995) {
                            continue;
                        }
                        if (t2ChisqNdf > trackChi2NdfCut) {//(t2ChisqProb > 0.995) {
                            continue;
                        }
                        // all cuts passed, let's fill some histograms
                        Hep3Vector pos = v.getPosition();
                        double p1 = rp1.getMomentum().magnitude();
                        double p2 = rp2.getMomentum().magnitude();
                        if (vertexCollectionName.equals("UnconstrainedV0Vertices_KF")) {
                            invMassHist_UnconstrainedV0Vertices.fill(rp.getMass());
                            pHist_UnconstrainedV0Vertices.fill(rp.getMomentum().magnitude());
                            pxHist_UnconstrainedV0Vertices.fill(rprot.x());
                            pyHist_UnconstrainedV0Vertices.fill(rprot.y());
                            pzHist_UnconstrainedV0Vertices.fill(rprot.z());
                            trkpHist_UnconstrainedV0Vertices.fill(p1);
                            trkpHist_UnconstrainedV0Vertices.fill(p2);
                            if (isTopTrack(t1)) {
                                trkptopHist_UnconstrainedV0Vertices.fill(p1);
                                ptopvspbotHist_UnconstrainedV0Vertices.fill(p1, p2);
                            } else {
                                trkpbotHist_UnconstrainedV0Vertices.fill(p1);
                            }
                            if (isTopTrack(t2)) {
                                trkptopHist_UnconstrainedV0Vertices.fill(p2);
                            } else {
                                trkpbotHist_UnconstrainedV0Vertices.fill(p2);
                            }
                            trkNhitsHist_UnconstrainedV0Vertices.fill(t1.getTrackerHits().size());
                            trkNhitsHist_UnconstrainedV0Vertices.fill(t2.getTrackerHits().size());
                            trkChisqHist_UnconstrainedV0Vertices.fill(t1.getChi2() / t1.getNDF());
                            trkChisqHist_UnconstrainedV0Vertices.fill(t2.getChi2() / t2.getNDF());
                            trkChisqProbHist_UnconstrainedV0Vertices.fill(t1ChisqProb);
                            trkChisqProbHist_UnconstrainedV0Vertices.fill(t2ChisqProb);
                            vtxXHist_UnconstrainedV0Vertices.fill(pos.x());
                            vtxYHist_UnconstrainedV0Vertices.fill(pos.y());
                            vtxZHist_UnconstrainedV0Vertices.fill(pos.z());
                            if (hasLayer1Hit(t1) && hasLayer1Hit(t2)) {
                                vtxZHistL1L1_UnconstrainedV0Vertices.fill(pos.z());
                            }
                            vtxChisqHist_UnconstrainedV0Vertices.fill(v.getChi2());

                            p1vsp2Hist_UnconstrainedV0Vertices.fill(p1, p2);
                            theta1vstheta2Hist_UnconstrainedV0Vertices.fill(theta1, theta2);
                            pvsthetaHist_UnconstrainedV0Vertices.fill(p1, theta1);
                            pvsthetaHist_UnconstrainedV0Vertices.fill(p2, theta2);
                            xvsyHist_UnconstrainedV0Vertices.fill(pos.x(), pos.y());
                        }
                        if (vertexCollectionName.equals("BeamspotConstrainedV0Vertices_KF")) {
                            invMassHist_BeamspotConstrainedV0Vertices.fill(rp.getMass());
                            pHist_BeamspotConstrainedV0Vertices.fill(rp.getMomentum().magnitude());
                            pxHist_BeamspotConstrainedV0Vertices.fill(rprot.x());
                            pyHist_BeamspotConstrainedV0Vertices.fill(rprot.y());
                            pzHist_BeamspotConstrainedV0Vertices.fill(rprot.z());
                            trkpHist_BeamspotConstrainedV0Vertices.fill(p1);
                            trkpHist_BeamspotConstrainedV0Vertices.fill(p2);
                            if (isTopTrack(t1)) {
                                trkptopHist_BeamspotConstrainedV0Vertices.fill(p1);
                                ptopvspbotHist_BeamspotConstrainedV0Vertices.fill(p1, p2);
                            } else {
                                trkpbotHist_BeamspotConstrainedV0Vertices.fill(p1);
                            }
                            if (isTopTrack(t2)) {
                                trkptopHist_BeamspotConstrainedV0Vertices.fill(p2);
                            } else {
                                trkpbotHist_BeamspotConstrainedV0Vertices.fill(p2);
                            }
                            trkNhitsHist_BeamspotConstrainedV0Vertices.fill(t1.getTrackerHits().size());
                            trkNhitsHist_BeamspotConstrainedV0Vertices.fill(t2.getTrackerHits().size());
                            trkChisqHist_BeamspotConstrainedV0Vertices.fill(t1.getChi2() / t1.getNDF());
                            trkChisqHist_BeamspotConstrainedV0Vertices.fill(t2.getChi2() / t2.getNDF());
                            trkChisqProbHist_BeamspotConstrainedV0Vertices.fill(t1ChisqProb);
                            trkChisqProbHist_BeamspotConstrainedV0Vertices.fill(t2ChisqProb);
                            vtxXHist_BeamspotConstrainedV0Vertices.fill(pos.x());
                            vtxYHist_BeamspotConstrainedV0Vertices.fill(pos.y());
                            vtxZHist_BeamspotConstrainedV0Vertices.fill(pos.z());
                            vtxChisqHist_BeamspotConstrainedV0Vertices.fill(v.getChi2());

                            p1vsp2Hist_BeamspotConstrainedV0Vertices.fill(p1, p2);
                            theta1vstheta2Hist_BeamspotConstrainedV0Vertices.fill(theta1, theta2);
                            pvsthetaHist_BeamspotConstrainedV0Vertices.fill(p1, theta1);
                            pvsthetaHist_BeamspotConstrainedV0Vertices.fill(p2, theta2);
                            xvsyHist_BeamspotConstrainedV0Vertices.fill(pos.x(), pos.y());
                        }
                        if (vertexCollectionName.equals("TargetConstrainedV0Vertices_KF")) {
                            invMassHist_TargetConstrainedV0Vertices.fill(rp.getMass());
                            pHist_TargetConstrainedV0Vertices.fill(rp.getMomentum().magnitude());
                            pxHist_TargetConstrainedV0Vertices.fill(rprot.x());
                            pyHist_TargetConstrainedV0Vertices.fill(rprot.y());
                            pzHist_TargetConstrainedV0Vertices.fill(rprot.z());
                            trkpHist_TargetConstrainedV0Vertices.fill(p1);
                            trkpHist_TargetConstrainedV0Vertices.fill(p2);
                            if (isTopTrack(t1)) {
                                trkptopHist_TargetConstrainedV0Vertices.fill(p1);
                                ptopvspbotHist_TargetConstrainedV0Vertices.fill(p1, p2);
                            } else {
                                trkpbotHist_TargetConstrainedV0Vertices.fill(p1);
                            }
                            if (isTopTrack(t2)) {
                                trkptopHist_TargetConstrainedV0Vertices.fill(p2);
                            } else {
                                trkpbotHist_TargetConstrainedV0Vertices.fill(p2);
                            }
                            trkNhitsHist_TargetConstrainedV0Vertices.fill(t1.getTrackerHits().size());
                            trkNhitsHist_TargetConstrainedV0Vertices.fill(t2.getTrackerHits().size());
                            trkChisqHist_TargetConstrainedV0Vertices.fill(t1.getChi2() / t1.getNDF());
                            trkChisqHist_TargetConstrainedV0Vertices.fill(t2.getChi2() / t2.getNDF());
                            trkChisqProbHist_TargetConstrainedV0Vertices.fill(t1ChisqProb);
                            trkChisqProbHist_TargetConstrainedV0Vertices.fill(t2ChisqProb);
                            vtxXHist_TargetConstrainedV0Vertices.fill(pos.x());
                            vtxYHist_TargetConstrainedV0Vertices.fill(pos.y());
                            vtxZHist_TargetConstrainedV0Vertices.fill(pos.z());
                            vtxChisqHist_TargetConstrainedV0Vertices.fill(v.getChi2());

                            p1vsp2Hist_TargetConstrainedV0Vertices.fill(p1, p2);
                            theta1vstheta2Hist_TargetConstrainedV0Vertices.fill(theta1, theta2);
                            pvsthetaHist_TargetConstrainedV0Vertices.fill(p1, theta1);
                            pvsthetaHist_TargetConstrainedV0Vertices.fill(p2, theta2);
                            xvsyHist_TargetConstrainedV0Vertices.fill(pos.x(), pos.y());
                        }
                    } //Loop over Kalman-vertices
                } // Loop over vertices
            } // loop over various vertex collections
        }
    }
}
