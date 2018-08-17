package org.hps.test.it;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.io.IOException;
import static java.lang.Math.abs;
import java.util.List;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.tracking.TrackType;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.Vertex;
import org.lcsim.geometry.Detector;
import org.lcsim.math.chisq.ChisqProb;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Norman A. Graf
 */
public class PhysRun2016V0Recon  extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    private String _aidaFileName = "PhysRun2016V0Recon";
    String[] vertexCollectionNames = {"UnconstrainedV0Vertices", "BeamspotConstrainedV0Vertices", "TargetConstrainedV0Vertices"};
    private final BasicHep3Matrix beamAxisRotation = new BasicHep3Matrix();

    private Double _beamEnergy = 1.056;
    private double _percentFeeCut = 0.8;
    private double _psumDelta = 0.05;
    private double _thetasumCut = 0.07;
    private double _trackChi2NdfCut = 100.; //corresponds to chisquared cut of 40 for 5-hit tracks

    private boolean _dumpRunAndEventNumber = false;

    private IHistogram1D invMassHist_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 Invariant Mass", 200, 0., 0.1);
    private IHistogram1D pHist_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 Momentum", 200, 0., 3.0);
    private IHistogram1D pxHist_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 x Momentum", 200, -0.01, 0.01);
    private IHistogram1D pyHist_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 y Momentum", 200, -0.01, 0.01);
    private IHistogram1D pzHist_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 z Momentum", 200, 0., 3.0);
    private IHistogram1D trkpHist_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 Track Momentum", 100, 0.25, 1.75);
    private IHistogram1D trkptopHist_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 Top Track Momentum", 100, 0.25, 1.75);
    private IHistogram1D trkpbotHist_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 Bottom Track Momentum", 100, 0.25, 1.75);
    private IHistogram1D trkNhitsHist_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 Track Number of Hits", 7, -0.5, 6.5);
    private IHistogram1D trkChisqHist_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 Track Chisq per DoF", 100, 0.0, 20.0);
    private IHistogram1D trkChisqProbHist_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 Track Chisq Prob", 100, 0.0, 1.0);
    private IHistogram1D vtxXHist_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 Vertex x", 200, -2.5, 2.5);
    private IHistogram1D vtxYHist_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 Vertex y", 200, -1.0, 1.0);
    private IHistogram1D vtxZHist_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 Vertex z", 200, -20.0, 20.0);
    private IHistogram1D vtxZHistL1L1_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 Vertex z L1L1", 200, -20.0, 20.0);
    private IHistogram1D vtxChisqHist_UnconstrainedV0Vertices = aida.histogram1D("UnconstrainedV0Vertices/V0 Vertex Chisq", 100, 0.0, 100.0);

    //2D
    private IHistogram2D p1vsp2Hist_UnconstrainedV0Vertices = aida.histogram2D("UnconstrainedV0Vertices/V0 p1 vs p2", 200, 0.25, 1.75, 200, 0.25, 1.75);
    private IHistogram2D ptopvspbotHist_UnconstrainedV0Vertices = aida.histogram2D("UnconstrainedV0Vertices/V0 p top vs p bottom", 200, 0.25, 1.75, 200, 0.25, 1.75);
    private IHistogram2D theta1vstheta2Hist_UnconstrainedV0Vertices = aida.histogram2D("UnconstrainedV0Vertices/V0 theta1 vs theta2", 100, 0.01, 0.05, 100, 0.01, 0.05);
    private IHistogram2D pvsthetaHist_UnconstrainedV0Vertices = aida.histogram2D("UnconstrainedV0Vertices/V0 p vs theta", 100, 0.25, 1.75, 100, 0.01, 0.05);
    private IHistogram2D xvsyHist_UnconstrainedV0Vertices = aida.histogram2D("UnconstrainedV0Vertices/V0 vertex X vs Y", 250, -2.5, 2.5, 100, -1.0, 1.0);

    //
    private IHistogram1D invMassHist_BeamspotConstrainedV0Vertices = aida.histogram1D("BeamspotConstrainedV0Vertices/V0 Invariant Mass", 200, 0., 0.1);
    private IHistogram1D pHist_BeamspotConstrainedV0Vertices = aida.histogram1D("BeamspotConstrainedV0Vertices/V0 Momentum", 200, 0., 3.0);
    private IHistogram1D pxHist_BeamspotConstrainedV0Vertices = aida.histogram1D("BeamspotConstrainedV0Vertices/V0 x Momentum", 200, -0.01, 0.01);
    private IHistogram1D pyHist_BeamspotConstrainedV0Vertices = aida.histogram1D("BeamspotConstrainedV0Vertices/V0 y Momentum", 200, -0.01, 0.01);
    private IHistogram1D pzHist_BeamspotConstrainedV0Vertices = aida.histogram1D("BeamspotConstrainedV0Vertices/V0 z Momentum", 200, 0., 3.0);
    private IHistogram1D trkpHist_BeamspotConstrainedV0Vertices = aida.histogram1D("BeamspotConstrainedV0Vertices/V0 Track Momentum", 100, 0.25, 1.75);
    private IHistogram1D trkptopHist_BeamspotConstrainedV0Vertices = aida.histogram1D("BeamspotConstrainedV0Vertices/V0 Top Track Momentum", 100, 0.25, 1.75);
    private IHistogram1D trkpbotHist_BeamspotConstrainedV0Vertices = aida.histogram1D("BeamspotConstrainedV0Vertices/V0 Bottom Track Momentum", 100, 0.25, 1.75);
    private IHistogram1D trkNhitsHist_BeamspotConstrainedV0Vertices = aida.histogram1D("BeamspotConstrainedV0Vertices/V0 Track Number of Hits", 7, -0.5, 6.5);
    private IHistogram1D trkChisqHist_BeamspotConstrainedV0Vertices = aida.histogram1D("BeamspotConstrainedV0Vertices/V0 Track Chisq per DoF", 100, 0.0, 20.0);
    private IHistogram1D trkChisqProbHist_BeamspotConstrainedV0Vertices = aida.histogram1D("BeamspotConstrainedV0Vertices/V0 Track Chisq Prob", 100, 0.0, 1.0);
    private IHistogram1D vtxXHist_BeamspotConstrainedV0Vertices = aida.histogram1D("BeamspotConstrainedV0Vertices/V0 Vertex x", 200, -2.5, 2.5);
    private IHistogram1D vtxYHist_BeamspotConstrainedV0Vertices = aida.histogram1D("BeamspotConstrainedV0Vertices/V0 Vertex y", 200, -1.0, 1.0);
    private IHistogram1D vtxZHist_BeamspotConstrainedV0Vertices = aida.histogram1D("BeamspotConstrainedV0Vertices/V0 Vertex z", 200, -20.0, 20.0);
    private IHistogram1D vtxChisqHist_BeamspotConstrainedV0Vertices = aida.histogram1D("BeamspotConstrainedV0Vertices/V0 Vertex Chisq", 100, 0.0, 100.0);

    //2D
    private IHistogram2D p1vsp2Hist_BeamspotConstrainedV0Vertices = aida.histogram2D("BeamspotConstrainedV0Vertices/V0 p1 vs p2", 200, 0.25, 1.75, 200, 0.25, 1.75);
    private IHistogram2D ptopvspbotHist_BeamspotConstrainedV0Vertices = aida.histogram2D("BeamspotConstrainedV0Vertices/V0 p top vs p bottom", 200, 0.25, 1.75, 200, 0.25, 1.75);
    private IHistogram2D theta1vstheta2Hist_BeamspotConstrainedV0Vertices = aida.histogram2D("BeamspotConstrainedV0Vertices/V0 theta1 vs theta2", 100, 0.01, 0.05, 100, 0.01, 0.05);
    private IHistogram2D pvsthetaHist_BeamspotConstrainedV0Vertices = aida.histogram2D("BeamspotConstrainedV0Vertices/V0 p vs theta", 100, 0.25, 1.75, 100, 0.01, 0.05);
    private IHistogram2D xvsyHist_BeamspotConstrainedV0Vertices = aida.histogram2D("BeamspotConstrainedV0Vertices/V0 vertex X vs Y", 250, -2.5, 2.5, 100, -1.0, 1.0);

//
    private IHistogram1D invMassHist_TargetConstrainedV0Vertices = aida.histogram1D("TargetConstrainedV0Vertices/V0 Invariant Mass", 200, 0., 0.1);
    private IHistogram1D pHist_TargetConstrainedV0Vertices = aida.histogram1D("TargetConstrainedV0Vertices/V0 Momentum", 200, 0., 3.0);
    private IHistogram1D pxHist_TargetConstrainedV0Vertices = aida.histogram1D("TargetConstrainedV0Vertices/V0 x Momentum", 200, -0.01, 0.01);
    private IHistogram1D pyHist_TargetConstrainedV0Vertices = aida.histogram1D("TargetConstrainedV0Vertices/V0 y Momentum", 200, -0.01, 0.01);
    private IHistogram1D pzHist_TargetConstrainedV0Vertices = aida.histogram1D("TargetConstrainedV0Vertices/V0 z Momentum", 200, 0., 3.0);
    private IHistogram1D trkpHist_TargetConstrainedV0Vertices = aida.histogram1D("TargetConstrainedV0Vertices/V0 Track Momentum", 100, 0.25, 1.75);
    private IHistogram1D trkptopHist_TargetConstrainedV0Vertices = aida.histogram1D("TargetConstrainedV0Vertices/V0 Top Track Momentum", 100, 0.25, 1.75);
    private IHistogram1D trkpbotHist_TargetConstrainedV0Vertices = aida.histogram1D("TargetConstrainedV0Vertices/V0 Bottom Track Momentum", 100, 0.25, 1.75);
    private IHistogram1D trkNhitsHist_TargetConstrainedV0Vertices = aida.histogram1D("TargetConstrainedV0Vertices/V0 Track Number of Hits", 7, -0.5, 6.5);
    private IHistogram1D trkChisqHist_TargetConstrainedV0Vertices = aida.histogram1D("TargetConstrainedV0Vertices/V0 Track Chisq per DoF", 100, 0.0, 20.0);
    private IHistogram1D trkChisqProbHist_TargetConstrainedV0Vertices = aida.histogram1D("TargetConstrainedV0Vertices/V0 Track Chisq Prob", 100, 0.0, 1.0);
    private IHistogram1D vtxXHist_TargetConstrainedV0Vertices = aida.histogram1D("TargetConstrainedV0Vertices/V0 Vertex x", 200, -2.5, 2.5);
    private IHistogram1D vtxYHist_TargetConstrainedV0Vertices = aida.histogram1D("TargetConstrainedV0Vertices/V0 Vertex y", 200, -1.0, 1.0);
    private IHistogram1D vtxZHist_TargetConstrainedV0Vertices = aida.histogram1D("TargetConstrainedV0Vertices/V0 Vertex z", 200, -20.0, 20.0);
    private IHistogram1D vtxChisqHist_TargetConstrainedV0Vertices = aida.histogram1D("TargetConstrainedV0Vertices/V0 Vertex Chisq", 100, 0.0, 100.0);

    //2D
    private IHistogram2D p1vsp2Hist_TargetConstrainedV0Vertices = aida.histogram2D("TargetConstrainedV0Vertices/V0 p1 vs p2", 200, 0.25, 1.75, 200, 0.25, 1.75);
    private IHistogram2D ptopvspbotHist_TargetConstrainedV0Vertices = aida.histogram2D("TargetConstrainedV0Vertices/V0 p top vs p bottom", 200, 0.25, 1.75, 200, 0.25, 1.75);
    private IHistogram2D theta1vstheta2Hist_TargetConstrainedV0Vertices = aida.histogram2D("TargetConstrainedV0Vertices/V0 theta1 vs theta2", 100, 0.01, 0.05, 100, 0.01, 0.05);
    private IHistogram2D pvsthetaHist_TargetConstrainedV0Vertices = aida.histogram2D("TargetConstrainedV0Vertices/V0 p vs theta", 100, 0.25, 1.75, 100, 0.01, 0.05);
    private IHistogram2D xvsyHist_TargetConstrainedV0Vertices = aida.histogram2D("TargetConstrainedV0Vertices/V0 vertex X vs Y", 250, -2.5, 2.5, 100, -1.0, 1.0);

    protected void detectorChanged(Detector detector) {
        beamAxisRotation.setActiveEuler(Math.PI / 2, -0.0305, -Math.PI / 2);
    }

    protected void process(EventHeader event) {
        if (event.getRunNumber() > 7000) {
            _beamEnergy = 2.306;
            _thetasumCut = 0.0475;
        }
        double psumMin = (1 - _psumDelta) * _beamEnergy;
        double psumMax = (1 + _psumDelta) * _beamEnergy;
        setupSensors(event);
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
                    if (rp1.getMomentum().magnitude() > 1.5 * _beamEnergy || rp2.getMomentum().magnitude() > 1.5 * _beamEnergy) {
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
//                    // require momentum sum to equal beam energy +-
//                    double psum = rp1.getMomentum().magnitude() + rp2.getMomentum().magnitude();
//                    if (psum < psumMin || psum > psumMax) {
//                        continue;
//                    }
                    //rotate into physiscs frame of reference
                    Hep3Vector rprot = VecOp.mult(beamAxisRotation, rp.getMomentum());
                    Hep3Vector p1rot = VecOp.mult(beamAxisRotation, rp1.getMomentum());
                    Hep3Vector p2rot = VecOp.mult(beamAxisRotation, rp2.getMomentum());
                    double theta1 = Math.acos(p1rot.z() / p1rot.magnitude());
                    double theta2 = Math.acos(p2rot.z() / p2rot.magnitude());
                    double thetasum = theta1 + theta2;
//                    // cut on thetasum
//                    if (thetasum > _thetasumCut) {
//                        continue;
//                    }
//                    // cut on V0 pX
//                    if (abs(rprot.x()) > 0.01) {
//                        continue;
//                    }
//                    // cut on V0 pY
//                    if (abs(rp.getMomentum().y()) > .01) {
//                        continue;
//                    }
                    double t1ChisqNdf = t1.getChi2() / t1.getNDF();
                    double t2ChisqNdf = t2.getChi2() / t2.getNDF();

                    double t1ChisqProb = ChisqProb.gammp(t1.getNDF(), t1.getChi2());
                    double t2ChisqProb = ChisqProb.gammp(t2.getNDF(), t2.getChi2());
                    // used to cut on prob < 0.995, which corresponds to roughly 3.4
                    // change this to a cut on chi-squared/dof which people are more familiar with.
                    // Omar currently cuts on chi-squared <40(!), irrespective of 5 or 6 hit tracks
                    // let's start at chisq/dof of 8
                    if (t1ChisqNdf > _trackChi2NdfCut) {//(t1ChisqProb > 0.995) {
                        continue;
                    }
                    if (t2ChisqNdf > _trackChi2NdfCut) {//(t2ChisqProb > 0.995) {
                        continue;
                    }
                    // all cuts passed, let's fill some histograms
                    Hep3Vector pos = v.getPosition();
                    double p1 = rp1.getMomentum().magnitude();
                    double p2 = rp2.getMomentum().magnitude();
                    if (vertexCollectionName.equals("UnconstrainedV0Vertices")) {
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
                            if (_dumpRunAndEventNumber) {
                                System.out.println(event.getRunNumber() + " " + event.getEventNumber());
                            }
                        }
                        vtxChisqHist_UnconstrainedV0Vertices.fill(v.getChi2());
// 2D
                        p1vsp2Hist_UnconstrainedV0Vertices.fill(p1, p2);
                        theta1vstheta2Hist_UnconstrainedV0Vertices.fill(theta1, theta2);
                        pvsthetaHist_UnconstrainedV0Vertices.fill(p1, theta1);
                        pvsthetaHist_UnconstrainedV0Vertices.fill(p2, theta2);
                        xvsyHist_UnconstrainedV0Vertices.fill(pos.x(), pos.y());
                    }
                    if (vertexCollectionName.equals("BeamspotConstrainedV0Vertices")) {
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
// 2D
                        p1vsp2Hist_BeamspotConstrainedV0Vertices.fill(p1, p2);
                        theta1vstheta2Hist_BeamspotConstrainedV0Vertices.fill(theta1, theta2);
                        pvsthetaHist_BeamspotConstrainedV0Vertices.fill(p1, theta1);
                        pvsthetaHist_BeamspotConstrainedV0Vertices.fill(p2, theta2);
                        xvsyHist_BeamspotConstrainedV0Vertices.fill(pos.x(), pos.y());
                    }
                    if (vertexCollectionName.equals("TargetConstrainedV0Vertices")) {
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
// 2D
                        p1vsp2Hist_TargetConstrainedV0Vertices.fill(p1, p2);
                        theta1vstheta2Hist_TargetConstrainedV0Vertices.fill(theta1, theta2);
                        pvsthetaHist_TargetConstrainedV0Vertices.fill(p1, theta1);
                        pvsthetaHist_TargetConstrainedV0Vertices.fill(p2, theta2);
                        xvsyHist_TargetConstrainedV0Vertices.fill(pos.x(), pos.y());
                    }
                }//Loop over GBL-vertices
            }// Loop over vertices
        }// loop over various vertex collections
    }

    private void setupSensors(EventHeader event) {
        List<RawTrackerHit> rawTrackerHits = event.get(RawTrackerHit.class, "SVTRawTrackerHits");
        EventHeader.LCMetaData meta = event.getMetaData(rawTrackerHits);
        // Get the ID dictionary and field information.
        IIdentifierDictionary dict = meta.getIDDecoder().getSubdetector().getDetectorElement().getIdentifierHelper().getIdentifierDictionary();
        int fieldIdx = dict.getFieldIndex("side");
        int sideIdx = dict.getFieldIndex("strip");
        for (RawTrackerHit hit : rawTrackerHits) {
            // The "side" and "strip" fields needs to be stripped from the ID for sensor lookup.
            IExpandedIdentifier expId = dict.unpack(hit.getIdentifier());
            expId.setValue(fieldIdx, 0);
            expId.setValue(sideIdx, 0);
            IIdentifier strippedId = dict.pack(expId);
            // Find the sensor DetectorElement.
            List<IDetectorElement> des = DetectorElementStore.getInstance().find(strippedId);
            if (des == null || des.size() == 0) {
                throw new RuntimeException("Failed to find any DetectorElements with stripped ID <0x" + Long.toHexString(strippedId.getValue()) + ">.");
            } else if (des.size() == 1) {
                hit.setDetectorElement((SiSensor) des.get(0));
            } else {
                // Use first sensor found, which should work unless there are sensors with duplicate IDs.
                for (IDetectorElement de : des) {
                    if (de instanceof SiSensor) {
                        hit.setDetectorElement((SiSensor) de);
                        break;
                    }
                }
            }
            // No sensor was found.
            if (hit.getDetectorElement() == null) {
                throw new RuntimeException("No sensor was found for hit with stripped ID <0x" + Long.toHexString(strippedId.getValue()) + ">.");
            }
        }
    }

    public void setTrackChisqNdfCut(double d) {
        _trackChi2NdfCut = d;
    }

    public void setFeeFractionCut(double d) {
        _percentFeeCut = d;
    }

    public void setESumPlusMinusPercentCut(double d) {
        _psumDelta = d;
    }

    public void setDumpRunAndEventNumber(boolean b) {
        _dumpRunAndEventNumber = b;
    }

    public void setAidaFileName(String s) {
        _aidaFileName = s;
    }
    
    @Override
    protected void endOfData() {
      try {
            AIDA.defaultInstance().saveAs(_aidaFileName+".aida");
            AIDA.defaultInstance().saveAs(_aidaFileName+".root");
            //AIDA.defaultInstance().saveAs(testOutputDir.getPath() + File.separator + this.getClass().getSimpleName() + ".root");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }  
    }

    private boolean isTopTrack(Track t) {
        List<TrackerHit> hits = t.getTrackerHits();
        int n[] = {0, 0};
        int nHits = hits.size();
        for (TrackerHit h : hits) {
            HpsSiSensor sensor = ((HpsSiSensor) ((RawTrackerHit) h.getRawHits().get(0)).getDetectorElement());
            if (sensor.isTopLayer()) {
                n[0] += 1;
            } else {
                n[1] += 1;
            }
        }
        if (n[0] == nHits && n[1] == 0) {
            return true;
        }
        if (n[1] == nHits && n[0] == 0) {
            return false;
        }
        throw new RuntimeException("mixed top and bottom hits on same track");
    }

    private boolean hasLayer1Hit(Track t) {
        List<TrackerHit> hits = t.getTrackerHits();
        for (TrackerHit h : hits) {
            HpsSiSensor sensor = ((HpsSiSensor) ((RawTrackerHit) h.getRawHits().get(0)).getDetectorElement());
            if (sensor.getLayerNumber() == 1) {
                return true;
            }
        }
        return false;
    }
}