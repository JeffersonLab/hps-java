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
 * Test Moller reconstruction for 2015 engineering run
 */
public class EngRun2015MollerReconTest extends ReconTest {

    static final String DETECTOR = "HPS-EngRun2015-Nominal-v6-0-fieldmap_v3";
    static final String TEST_FILE_NAME = "hps_005772_mollerskim_10k.evio";
    static final String STEERING = "/org/hps/steering/recon/legacy_drivers/EngineeringRun2015FullRecon.lcsim";
    static final int NEVENTS = 1000;
    static final long MAX_EVENT_TIME = -1L;

    public EngRun2015MollerReconTest() {
        super(EngRun2015MollerReconTest.class,
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

    public static class PlotDriver extends RefDriver {

        String[] vertexCollectionNames = {"UnconstrainedMollerVertices",
                "BeamspotConstrainedMollerVertices", "TargetConstrainedMollerVertices"};

        Double beamEnergy = 1.056;
        double psumDelta = 0.06;
        double thetaSumCut = 0.07;
        double trackChi2NdfCut = 8.; //corresponds to chisquared cut of 40 for 5-hit tracks
        double psumMin = (1 - psumDelta) * beamEnergy;
        double psumMax = (1 + psumDelta) * beamEnergy;

        IHistogram1D invMassHist_UnconstrainedMollerVertices = aida.histogram1D("UnconstrainedMollerVertices/Moller Invariant Mass", 200, 0., 0.1);
        IHistogram1D pHist_UnconstrainedMollerVertices = aida.histogram1D("UnconstrainedMollerVertices/Moller Momentum", 200, 0., 3.0);
        IHistogram1D pxHist_UnconstrainedMollerVertices = aida.histogram1D("UnconstrainedMollerVertices/Moller x Momentum", 200, -0.01, 0.01);
        IHistogram1D pyHist_UnconstrainedMollerVertices = aida.histogram1D("UnconstrainedMollerVertices/Moller y Momentum", 200, -0.01, 0.01);
        IHistogram1D pzHist_UnconstrainedMollerVertices = aida.histogram1D("UnconstrainedMollerVertices/Moller z Momentum", 200, 0., 3.0);
        IHistogram1D trkpHist_UnconstrainedMollerVertices = aida.histogram1D("UnconstrainedMollerVertices/Moller Track Momentum", 100, 0.25, 1.75);
        IHistogram1D trkptopHist_UnconstrainedMollerVertices = aida.histogram1D("UnconstrainedMollerVertices/Moller Top Track Momentum", 100, 0.25, 1.75);
        IHistogram1D trkpbotHist_UnconstrainedMollerVertices = aida.histogram1D("UnconstrainedMollerVertices/Moller Bottom Track Momentum", 100, 0.25, 1.75);
        IHistogram1D trkNhitsHist_UnconstrainedMollerVertices = aida.histogram1D("UnconstrainedMollerVertices/Moller Track Number of Hits", 7, -0.5, 6.5);
        IHistogram1D trkChisqHist_UnconstrainedMollerVertices = aida.histogram1D("UnconstrainedMollerVertices/Moller Track Chisq per DoF", 100, 0.0, 20.0);
        IHistogram1D trkChisqProbHist_UnconstrainedMollerVertices = aida.histogram1D("UnconstrainedMollerVertices/Moller Track Chisq Prob", 100, 0.0, 1.0);
        IHistogram1D vtxXHist_UnconstrainedMollerVertices = aida.histogram1D("UnconstrainedMollerVertices/Moller Vertex x", 200, -2.5, 2.5);
        IHistogram1D vtxYHist_UnconstrainedMollerVertices = aida.histogram1D("UnconstrainedMollerVertices/Moller Vertex y", 200, -1.0, 1.0);
        IHistogram1D vtxZHist_UnconstrainedMollerVertices = aida.histogram1D("UnconstrainedMollerVertices/Moller Vertex z", 200, -20.0, 20.0);
        IHistogram1D vtxChisqHist_UnconstrainedMollerVertices = aida.histogram1D("UnconstrainedMollerVertices/Moller Vertex Chisq", 100, 0.0, 100.0);

        IHistogram2D p1vsp2Hist_UnconstrainedMollerVertices = aida.histogram2D("UnconstrainedMollerVertices/Moller p1 vs p2", 200, 0.25, 1.75, 200, 0.25, 1.75);
        IHistogram2D ptopvspbotHist_UnconstrainedMollerVertices = aida.histogram2D("UnconstrainedMollerVertices/Moller p top vs p bottom", 200, 0.25, 1.75, 200, 0.25, 1.75);
        IHistogram2D theta1vstheta2Hist_UnconstrainedMollerVertices = aida.histogram2D("UnconstrainedMollerVertices/Moller theta1 vs theta2", 100, 0.01, 0.05, 100, 0.01, 0.05);
        IHistogram2D pvsthetaHist_UnconstrainedMollerVertices = aida.histogram2D("UnconstrainedMollerVertices/Moller p vs theta", 100, 0.25, 1.75, 100, 0.01, 0.05);
        IHistogram2D xvsyHist_UnconstrainedMollerVertices = aida.histogram2D("UnconstrainedMollerVertices/Moller vertex X vs Y", 250, -2.5, 2.5, 100, -1.0, 1.0);

        IHistogram1D invMassHist_BeamspotConstrainedMollerVertices = aida.histogram1D("BeamspotConstrainedMollerVertices/Moller Invariant Mass", 200, 0., 0.1);
        IHistogram1D pHist_BeamspotConstrainedMollerVertices = aida.histogram1D("BeamspotConstrainedMollerVertices/Moller Momentum", 200, 0., 3.0);
        IHistogram1D pxHist_BeamspotConstrainedMollerVertices = aida.histogram1D("BeamspotConstrainedMollerVertices/Moller x Momentum", 200, -0.01, 0.01);
        IHistogram1D pyHist_BeamspotConstrainedMollerVertices = aida.histogram1D("BeamspotConstrainedMollerVertices/Moller y Momentum", 200, -0.01, 0.01);
        IHistogram1D pzHist_BeamspotConstrainedMollerVertices = aida.histogram1D("BeamspotConstrainedMollerVertices/Moller z Momentum", 200, 0., 3.0);
        IHistogram1D trkpHist_BeamspotConstrainedMollerVertices = aida.histogram1D("BeamspotConstrainedMollerVertices/Moller Track Momentum", 100, 0.25, 1.75);
        IHistogram1D trkptopHist_BeamspotConstrainedMollerVertices = aida.histogram1D("BeamspotConstrainedMollerVertices/Moller Top Track Momentum", 100, 0.25, 1.75);
        IHistogram1D trkpbotHist_BeamspotConstrainedMollerVertices = aida.histogram1D("BeamspotConstrainedMollerVertices/Moller Bottom Track Momentum", 100, 0.25, 1.75);
        IHistogram1D trkNhitsHist_BeamspotConstrainedMollerVertices = aida.histogram1D("BeamspotConstrainedMollerVertices/Moller Track Number of Hits", 7, -0.5, 6.5);
        IHistogram1D trkChisqHist_BeamspotConstrainedMollerVertices = aida.histogram1D("BeamspotConstrainedMollerVertices/Moller Track Chisq per DoF", 100, 0.0, 20.0);
        IHistogram1D trkChisqProbHist_BeamspotConstrainedMollerVertices = aida.histogram1D("BeamspotConstrainedMollerVertices/Moller Track Chisq Prob", 100, 0.0, 1.0);
        IHistogram1D vtxXHist_BeamspotConstrainedMollerVertices = aida.histogram1D("BeamspotConstrainedMollerVertices/Moller Vertex x", 200, -2.5, 2.5);
        IHistogram1D vtxYHist_BeamspotConstrainedMollerVertices = aida.histogram1D("BeamspotConstrainedMollerVertices/Moller Vertex y", 200, -1.0, 1.0);
        IHistogram1D vtxZHist_BeamspotConstrainedMollerVertices = aida.histogram1D("BeamspotConstrainedMollerVertices/Moller Vertex z", 200, -20.0, 20.0);
        IHistogram1D vtxChisqHist_BeamspotConstrainedMollerVertices = aida.histogram1D("BeamspotConstrainedMollerVertices/Moller Vertex Chisq", 100, 0.0, 100.0);

        IHistogram2D p1vsp2Hist_BeamspotConstrainedMollerVertices = aida.histogram2D("BeamspotConstrainedMollerVertices/Moller p1 vs p2", 200, 0.25, 1.75, 200, 0.25, 1.75);
        IHistogram2D ptopvspbotHist_BeamspotConstrainedMollerVertices = aida.histogram2D("BeamspotConstrainedMollerVertices/Moller p top vs p bottom", 200, 0.25, 1.75, 200, 0.25, 1.75);
        IHistogram2D theta1vstheta2Hist_BeamspotConstrainedMollerVertices = aida.histogram2D("BeamspotConstrainedMollerVertices/Moller theta1 vs theta2", 100, 0.01, 0.05, 100, 0.01, 0.05);
        IHistogram2D pvsthetaHist_BeamspotConstrainedMollerVertices = aida.histogram2D("BeamspotConstrainedMollerVertices/Moller p vs theta", 100, 0.25, 1.75, 100, 0.01, 0.05);
        IHistogram2D xvsyHist_BeamspotConstrainedMollerVertices = aida.histogram2D("BeamspotConstrainedMollerVertices/Moller vertex X vs Y", 250, -2.5, 2.5, 100, -1.0, 1.0);

        IHistogram1D invMassHist_TargetConstrainedMollerVertices = aida.histogram1D("TargetConstrainedMollerVertices/Moller Invariant Mass", 200, 0., 0.1);
        IHistogram1D pHist_TargetConstrainedMollerVertices = aida.histogram1D("TargetConstrainedMollerVertices/Moller Momentum", 200, 0., 3.0);
        IHistogram1D pxHist_TargetConstrainedMollerVertices = aida.histogram1D("TargetConstrainedMollerVertices/Moller x Momentum", 200, -0.01, 0.01);
        IHistogram1D pyHist_TargetConstrainedMollerVertices = aida.histogram1D("TargetConstrainedMollerVertices/Moller y Momentum", 200, -0.01, 0.01);
        IHistogram1D pzHist_TargetConstrainedMollerVertices = aida.histogram1D("TargetConstrainedMollerVertices/Moller z Momentum", 200, 0., 3.0);
        IHistogram1D trkpHist_TargetConstrainedMollerVertices = aida.histogram1D("TargetConstrainedMollerVertices/Moller Track Momentum", 100, 0.25, 1.75);
        IHistogram1D trkptopHist_TargetConstrainedMollerVertices = aida.histogram1D("TargetConstrainedMollerVertices/Moller Top Track Momentum", 100, 0.25, 1.75);
        IHistogram1D trkpbotHist_TargetConstrainedMollerVertices = aida.histogram1D("TargetConstrainedMollerVertices/Moller Bottom Track Momentum", 100, 0.25, 1.75);
        IHistogram1D trkNhitsHist_TargetConstrainedMollerVertices = aida.histogram1D("TargetConstrainedMollerVertices/Moller Track Number of Hits", 7, -0.5, 6.5);
        IHistogram1D trkChisqHist_TargetConstrainedMollerVertices = aida.histogram1D("TargetConstrainedMollerVertices/Moller Track Chisq per DoF", 100, 0.0, 20.0);
        IHistogram1D trkChisqProbHist_TargetConstrainedMollerVertices = aida.histogram1D("TargetConstrainedMollerVertices/Moller Track Chisq Prob", 100, 0.0, 1.0);
        IHistogram1D vtxXHist_TargetConstrainedMollerVertices = aida.histogram1D("TargetConstrainedMollerVertices/Moller Vertex x", 200, -2.5, 2.5);
        IHistogram1D vtxYHist_TargetConstrainedMollerVertices = aida.histogram1D("TargetConstrainedMollerVertices/Moller Vertex y", 200, -1.0, 1.0);
        IHistogram1D vtxZHist_TargetConstrainedMollerVertices = aida.histogram1D("TargetConstrainedMollerVertices/Moller Vertex z", 200, -20.0, 20.0);
        IHistogram1D vtxChisqHist_TargetConstrainedMollerVertices = aida.histogram1D("TargetConstrainedMollerVertices/Moller Vertex Chisq", 100, 0.0, 100.0);

        IHistogram2D p1vsp2Hist_TargetConstrainedMollerVertices = aida.histogram2D("TargetConstrainedMollerVertices/Moller p1 vs p2", 200, 0.25, 1.75, 200, 0.25, 1.75);
        IHistogram2D ptopvspbotHist_TargetConstrainedMollerVertices = aida.histogram2D("TargetConstrainedMollerVertices/Moller p top vs p bottom", 200, 0.25, 1.75, 200, 0.25, 1.75);
        IHistogram2D theta1vstheta2Hist_TargetConstrainedMollerVertices = aida.histogram2D("TargetConstrainedMollerVertices/Moller theta1 vs theta2", 100, 0.01, 0.05, 100, 0.01, 0.05);
        IHistogram2D pvsthetaHist_TargetConstrainedMollerVertices = aida.histogram2D("TargetConstrainedMollerVertices/Moller p vs theta", 100, 0.25, 1.75, 100, 0.01, 0.05);
        IHistogram2D xvsyHist_TargetConstrainedMollerVertices = aida.histogram2D("TargetConstrainedMollerVertices/Moller vertex X vs Y", 250, -2.5, 2.5, 100, -1.0, 1.0);

        protected void process(EventHeader event) {
            super.process(event);
            for (String vertexCollectionName : vertexCollectionNames) {

                List<Vertex> vertices = event.get(Vertex.class, vertexCollectionName);
                for (Vertex v : vertices) {
                    aida.tree().cd("/");
                    aida.tree().mkdirs(vertexCollectionName);
                    aida.tree().cd(vertexCollectionName);
                    ReconstructedParticle rp = v.getAssociatedParticle();
                    int type = rp.getType();
                    boolean isGbl = TrackType.isGBL(type);
                    // require GBL tracks in vertex
                    if (isGbl) {
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
                        // require momentum sum to equal beam energy +-
                        double psum = rp1.getMomentum().magnitude() + rp2.getMomentum().magnitude();
                        if (psum < psumMin || psum > psumMax) {
                            continue;
                        }
                        //rotate into physiscs frame of reference
                        Hep3Vector rprot = VecOp.mult(BEAM_AXIS_ROTATION, rp.getMomentum());
                        Hep3Vector p1rot = VecOp.mult(BEAM_AXIS_ROTATION, rp1.getMomentum());
                        Hep3Vector p2rot = VecOp.mult(BEAM_AXIS_ROTATION, rp2.getMomentum());
                        double theta1 = Math.acos(p1rot.z() / p1rot.magnitude());
                        double theta2 = Math.acos(p2rot.z() / p2rot.magnitude());
                        double thetasum = theta1 + theta2;
                        // cut on thetasum
                        if (thetasum > thetaSumCut) {
                            continue;
                        }
                        // cut on Moller pX
                        if (abs(rprot.x()) > 0.01) {
                            continue;
                        }
                        // cut on Moller pY
                        if (abs(rp.getMomentum().y()) > .01) {
                            continue;
                        }
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
                        if (vertexCollectionName.equals("UnconstrainedMollerVertices")) {
                            invMassHist_UnconstrainedMollerVertices.fill(rp.getMass());
                            pHist_UnconstrainedMollerVertices.fill(rp.getMomentum().magnitude());
                            pxHist_UnconstrainedMollerVertices.fill(rprot.x());
                            pyHist_UnconstrainedMollerVertices.fill(rprot.y());
                            pzHist_UnconstrainedMollerVertices.fill(rprot.z());
                            trkpHist_UnconstrainedMollerVertices.fill(p1);
                            trkpHist_UnconstrainedMollerVertices.fill(p2);
                            if (isTopTrack(t1)) {
                                trkptopHist_UnconstrainedMollerVertices.fill(p1);
                                ptopvspbotHist_UnconstrainedMollerVertices.fill(p1, p2);
                            } else {
                                trkpbotHist_UnconstrainedMollerVertices.fill(p1);
                            }
                            if (isTopTrack(t2)) {
                                trkptopHist_UnconstrainedMollerVertices.fill(p2);
                            } else {
                                trkpbotHist_UnconstrainedMollerVertices.fill(p2);
                            }
                            trkNhitsHist_UnconstrainedMollerVertices.fill(t1.getTrackerHits().size());
                            trkNhitsHist_UnconstrainedMollerVertices.fill(t2.getTrackerHits().size());
                            trkChisqHist_UnconstrainedMollerVertices.fill(t1.getChi2() / t1.getNDF());
                            trkChisqHist_UnconstrainedMollerVertices.fill(t2.getChi2() / t2.getNDF());
                            trkChisqProbHist_UnconstrainedMollerVertices.fill(t1ChisqProb);
                            trkChisqProbHist_UnconstrainedMollerVertices.fill(t2ChisqProb);
                            vtxXHist_UnconstrainedMollerVertices.fill(pos.x());
                            vtxYHist_UnconstrainedMollerVertices.fill(pos.y());
                            vtxZHist_UnconstrainedMollerVertices.fill(pos.z());
                            vtxChisqHist_UnconstrainedMollerVertices.fill(v.getChi2());

                            p1vsp2Hist_UnconstrainedMollerVertices.fill(p1, p2);
                            theta1vstheta2Hist_UnconstrainedMollerVertices.fill(theta1, theta2);
                            pvsthetaHist_UnconstrainedMollerVertices.fill(p1, theta1);
                            pvsthetaHist_UnconstrainedMollerVertices.fill(p2, theta2);
                            xvsyHist_UnconstrainedMollerVertices.fill(pos.x(), pos.y());
                        }
                        if (vertexCollectionName.equals("BeamspotConstrainedMollerVertices")) {
                            invMassHist_BeamspotConstrainedMollerVertices.fill(rp.getMass());
                            pHist_BeamspotConstrainedMollerVertices.fill(rp.getMomentum().magnitude());
                            pxHist_BeamspotConstrainedMollerVertices.fill(rprot.x());
                            pyHist_BeamspotConstrainedMollerVertices.fill(rprot.y());
                            pzHist_BeamspotConstrainedMollerVertices.fill(rprot.z());
                            trkpHist_BeamspotConstrainedMollerVertices.fill(p1);
                            trkpHist_BeamspotConstrainedMollerVertices.fill(p2);
                            if (isTopTrack(t1)) {
                                trkptopHist_BeamspotConstrainedMollerVertices.fill(p1);
                                ptopvspbotHist_BeamspotConstrainedMollerVertices.fill(p1, p2);
                            } else {
                                trkpbotHist_BeamspotConstrainedMollerVertices.fill(p1);
                            }
                            if (isTopTrack(t2)) {
                                trkptopHist_BeamspotConstrainedMollerVertices.fill(p2);
                            } else {
                                trkpbotHist_BeamspotConstrainedMollerVertices.fill(p2);
                            }
                            trkNhitsHist_BeamspotConstrainedMollerVertices.fill(t1.getTrackerHits().size());
                            trkNhitsHist_BeamspotConstrainedMollerVertices.fill(t2.getTrackerHits().size());
                            trkChisqHist_BeamspotConstrainedMollerVertices.fill(t1.getChi2() / t1.getNDF());
                            trkChisqHist_BeamspotConstrainedMollerVertices.fill(t2.getChi2() / t2.getNDF());
                            trkChisqProbHist_BeamspotConstrainedMollerVertices.fill(t1ChisqProb);
                            trkChisqProbHist_BeamspotConstrainedMollerVertices.fill(t2ChisqProb);
                            vtxXHist_BeamspotConstrainedMollerVertices.fill(pos.x());
                            vtxYHist_BeamspotConstrainedMollerVertices.fill(pos.y());
                            vtxZHist_BeamspotConstrainedMollerVertices.fill(pos.z());
                            vtxChisqHist_BeamspotConstrainedMollerVertices.fill(v.getChi2());

                            p1vsp2Hist_BeamspotConstrainedMollerVertices.fill(p1, p2);
                            theta1vstheta2Hist_BeamspotConstrainedMollerVertices.fill(theta1, theta2);
                            pvsthetaHist_BeamspotConstrainedMollerVertices.fill(p1, theta1);
                            pvsthetaHist_BeamspotConstrainedMollerVertices.fill(p2, theta2);
                            xvsyHist_BeamspotConstrainedMollerVertices.fill(pos.x(), pos.y());
                        }
                        if (vertexCollectionName.equals("TargetConstrainedMollerVertices")) {
                            invMassHist_TargetConstrainedMollerVertices.fill(rp.getMass());
                            pHist_TargetConstrainedMollerVertices.fill(rp.getMomentum().magnitude());
                            pxHist_TargetConstrainedMollerVertices.fill(rprot.x());
                            pyHist_TargetConstrainedMollerVertices.fill(rprot.y());
                            pzHist_TargetConstrainedMollerVertices.fill(rprot.z());
                            trkpHist_TargetConstrainedMollerVertices.fill(p1);
                            trkpHist_TargetConstrainedMollerVertices.fill(p2);
                            if (isTopTrack(t1)) {
                                trkptopHist_TargetConstrainedMollerVertices.fill(p1);
                                ptopvspbotHist_TargetConstrainedMollerVertices.fill(p1, p2);
                            } else {
                                trkpbotHist_TargetConstrainedMollerVertices.fill(p1);
                            }
                            if (isTopTrack(t2)) {
                                trkptopHist_TargetConstrainedMollerVertices.fill(p2);
                            } else {
                                trkpbotHist_TargetConstrainedMollerVertices.fill(p2);
                            }
                            trkNhitsHist_TargetConstrainedMollerVertices.fill(t1.getTrackerHits().size());
                            trkNhitsHist_TargetConstrainedMollerVertices.fill(t2.getTrackerHits().size());
                            trkChisqHist_TargetConstrainedMollerVertices.fill(t1.getChi2() / t1.getNDF());
                            trkChisqHist_TargetConstrainedMollerVertices.fill(t2.getChi2() / t2.getNDF());
                            trkChisqProbHist_TargetConstrainedMollerVertices.fill(t1ChisqProb);
                            trkChisqProbHist_TargetConstrainedMollerVertices.fill(t2ChisqProb);
                            vtxXHist_TargetConstrainedMollerVertices.fill(pos.x());
                            vtxYHist_TargetConstrainedMollerVertices.fill(pos.y());
                            vtxZHist_TargetConstrainedMollerVertices.fill(pos.z());
                            vtxChisqHist_TargetConstrainedMollerVertices.fill(v.getChi2());

                            p1vsp2Hist_TargetConstrainedMollerVertices.fill(p1, p2);
                            theta1vstheta2Hist_TargetConstrainedMollerVertices.fill(theta1, theta2);
                            pvsthetaHist_TargetConstrainedMollerVertices.fill(p1, theta1);
                            pvsthetaHist_TargetConstrainedMollerVertices.fill(p2, theta2);
                            xvsyHist_TargetConstrainedMollerVertices.fill(pos.x(), pos.y());
                        }
                    } // Loop over GBL-vertices
                } // Loop over vertices
            } // Loop over various vertex collections
        }
    }
}
