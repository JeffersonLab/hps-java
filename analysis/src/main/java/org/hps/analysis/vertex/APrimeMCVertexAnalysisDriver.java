package org.hps.analysis.vertex;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.io.IOException;
import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.RotationPassiveXYZ;
import org.lcsim.detector.Transform3D;
import org.lcsim.detector.Translation3D;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.Vertex;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.geometry.Detector;
import org.lcsim.math.chisq.ChisqProb;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.fourvec.Lorentz4Vector;
import org.lcsim.util.fourvec.Momentum4Vector;

/**
 *
 * @author Norman A. Graf
 */
public class APrimeMCVertexAnalysisDriver extends Driver {

    String vertexCollectionName = "UnconstrainedV0Vertices";
    String[] names = {"X", "Y", "Z"};
    double[] p1 = new double[4];
    double[] p2 = new double[4];
    double emass = 0.000511;
    double mass2 = emass * emass;

    private static final RotationPassiveXYZ rotMat = new RotationPassiveXYZ(0., 0.01525, 0.);
    private static final Translation3D transMat = new Translation3D(0., 0., -0.5);
    private static final Transform3D xformMat = new Transform3D(transMat, rotMat);

    boolean debug = false;
    boolean writePlots = false;

    private AIDA aida = AIDA.defaultInstance();

    private final BasicHep3Matrix beamAxisRotation = new BasicHep3Matrix();

    protected void detectorChanged(Detector detector) {
        beamAxisRotation.setActiveEuler(Math.PI / 2, -0.0305, -Math.PI / 2);
    }

    protected void process(EventHeader event) {
        List<MCParticle> mcparts = event.getMCParticles();
        MCParticle aprime = null;
        for (MCParticle mcp : mcparts) {
            if (mcp.getPDGID() == 622) {
                aprime = mcp;
            }
        }
        List<MCParticle> daughters = aprime.getDaughters();
        MCParticle positronMC = null;
        MCParticle electronMC = null;
        for (MCParticle mcp : daughters) {
            if (mcp.getPDGID() == 11) {
                electronMC = mcp;
            }
            if (mcp.getPDGID() == -11) {
                positronMC = mcp;
            }
        }
        if (debug) {
            System.out.println("aprime " + aprime);
            System.out.println("electronMC " + electronMC);
            System.out.println("positronMC " + positronMC);
        }
        Hep3Vector aprimeVtx = aprime.getEndPoint();
        Hep3Vector aprimeMom = aprime.getMomentum();
        double aprimeMass = aprime.getMass();

        Hep3Vector electronMCMom = electronMC.getMomentum();
        Hep3Vector positronMCMom = positronMC.getMomentum();

        List<Vertex> vertices = event.get(Vertex.class, vertexCollectionName);
        if (vertices.size() != 0) {
            setupSensors(event);
        }
        for (Vertex v : vertices) {
            aida.tree().cd("/");
            ReconstructedParticle rp = v.getAssociatedParticle();
            int type = rp.getType();
            // only analyze GBL tracks
            if (TrackType.isGBL(type)) {
                // only analyze events where we got the correct e+ and e- pair...
                if (matchesMC(event, v, electronMC, positronMC)) {
                    SymmetricMatrix cov = v.getCovMatrix();
                    Hep3Vector pos = v.getPosition();

                    if (debug) {
                        System.out.println("cov matrix: \n" + cov);
                        System.out.println("vertex pos: \n" + pos);
                    }
                    Map<String, Double> vals = v.getParameters();
                    p1[0] = vals.get("p1X");
                    p1[1] = vals.get("p1Y");
                    p1[2] = vals.get("p1Z");
                    p2[0] = vals.get("p2X");
                    p2[1] = vals.get("p2Y");
                    p2[2] = vals.get("p2Z");
                    double e1 = 0;
                    double e2 = 0.;
                    for (int i = 0; i < 3; ++i) {
                        e1 += p1[i] * p1[i];
                        e2 += p2[i] * p2[i];
                    }
                    e1 = sqrt(e1 + mass2);
                    e2 = sqrt(e2 + mass2);
                    Momentum4Vector vec1 = new Momentum4Vector(p1[0], p1[1], p1[2], e1);
                    Momentum4Vector vec2 = new Momentum4Vector(p2[0], p2[1], p2[2], e2);
                    Lorentz4Vector sum = vec1.plus(vec2);
                    double mass = sum.mass();
                    double invMass = vals.get("invMass");
                    if (debug) {
                        System.out.println("mass: " + mass + " invMass: " + invMass + " delta: " + (invMass - mass));
                    }
                    Hep3Vector vele = new BasicHep3Vector(vals.get("p1Y"), vals.get("p1Z"), vals.get("p1X")); //electron?
                    Hep3Vector vpos = new BasicHep3Vector(vals.get("p2Y"), vals.get("p2Z"), vals.get("p2X")); //positron?

                    aida.cloud1D("type").fill(type);
                    aida.cloud1D("vertex x").fill(pos.x());
                    aida.cloud2D("vertex x vs z").fill(pos.z(), pos.x());

                    aida.cloud1D("vertex MC x").fill(aprimeVtx.x());
                    aida.cloud2D("vertex MC x vs MC z").fill(aprimeVtx.z(), aprimeVtx.x());
                    aida.cloud2D("vertex MC x vs MC y").fill(aprimeVtx.x(), aprimeVtx.y());
                    aida.cloud1D("vertex x residual").fill(pos.x() - aprimeVtx.x());
                    double xPull = (pos.x() - aprimeVtx.x()) / sqrt(cov.diagonal(0));
                    if (abs(xPull) < 10.) {
                        aida.cloud1D("vertex x pull").fill(xPull);
                    }

                    aida.cloud1D("vertex y").fill(pos.y());
                    aida.cloud2D("vertex y vs z").fill(pos.z(), pos.y());
                    aida.cloud1D("vertex MC y").fill(aprimeVtx.y());
                    aida.cloud2D("vertex MC y vs MC z").fill(aprimeVtx.z(), aprimeVtx.y());
                    aida.cloud1D("vertex y residual").fill(pos.y() - aprimeVtx.y());
                    double yPull = (pos.y() - aprimeVtx.y()) / sqrt(cov.diagonal(1));
                    if (abs(yPull) < 10.) {
                        aida.cloud1D("vertex y pull").fill(yPull);
                    }

                    aida.cloud1D("vertex z").fill(pos.z());
                    aida.cloud1D("vertex MC z").fill(aprimeVtx.z());
                    aida.cloud1D("vertex z residual").fill(pos.z() - aprimeVtx.z());
                    double zPull = (pos.z() - aprimeVtx.z()) / sqrt(cov.diagonal(2));
                    if (abs(zPull) < 10.) {
                        aida.cloud1D("vertex z pull").fill(zPull);
                        aida.cloud2D("vertex z pull vs MC z").fill(zPull, aprimeVtx.z());
                        aida.cloud1D("vertex z significance pull lt 10").fill(pos.z() / zPull);
                    }

                    aida.cloud2D("vertex z vs MC z").fill(aprimeVtx.z(), pos.z());

                    aida.cloud1D("vertex chisq").fill(v.getChi2());
                    aida.cloud1D("vertex chi2 prob").fill(v.getProbability());
                    aida.cloud1D("vertex fitted invariant mass").fill(rp.getMass());
                    aida.cloud1D("vertex fitted mass - MC mass").fill(rp.getMass() - aprimeMass);
                    aida.cloud2D("vertex fitted mass - MC mass vs MC z").fill(aprimeVtx.z(), rp.getMass() - aprimeMass);
                    double correctedMass = recalculateMass(v);
                    aida.cloud1D("vertex fixed mass - MC mass").fill(correctedMass - aprimeMass);

                    aida.cloud2D("vertex fixed mass - MC mass vs MC z").fill(aprimeVtx.z(), correctedMass - aprimeMass);

                    List<ReconstructedParticle> parts = rp.getParticles();
                    ReconstructedParticle electron = null;
                    ReconstructedParticle positron = null;
                    for (ReconstructedParticle part : parts) {
                        if (part.getCharge() < 0) {
                            electron = part;
                        }
                        if (part.getCharge() > 0) {
                            positron = part;
                        }
                    }
                    analyzeParticle("electron", electron, electronMC, vele);
                    analyzeParticle("positron", positron, positronMC, vpos);

                    aida.tree().cd("/");
                }// only analyze correct matches
            }// only analyze GBL tracks
        }
    }

    private void analyzeParticle(String dir, ReconstructedParticle p, MCParticle mcp, Hep3Vector v) {
        aida.tree().mkdirs(dir);
        aida.tree().cd(dir);
        Track t = p.getTracks().get(0);
        double trackMom = p.getMomentum().magnitude();
        double chiSquared = t.getChi2();
        int ndf = t.getNDF();
        double chisqProb = ChisqProb.gammp(ndf, chiSquared);
        aida.cloud1D("Track chisq per df").fill(chiSquared / ndf);
        aida.cloud1D("Track chisq prob").fill(chisqProb);
        aida.cloud1D("Track nHits").fill(t.getTrackerHits().size());

        Hep3Vector mom = p.getMomentum();
        Hep3Vector momMC = mcp.getMomentum();
        Hep3Vector vtxMC = mcp.getOrigin();
        if (debug) {
            System.out.println(dir + " vtx  : " + vtxMC);
            System.out.println(dir + " momMC: " + momMC);
            System.out.println(dir + " mom  : " + mom);
            System.out.println("vertexParticle: " + v);
        }
        // following plot demonstrates the problem
        aida.cloud2D(dir + " vtx z vs momPx - momMcPx").fill(vtxMC.z(), mom.x() - momMC.x());
        aida.cloud2D(dir + " vtx z vs momPy - momMcPy").fill(vtxMC.z(), mom.y() - momMC.y());
        aida.cloud2D(dir + " vtx z vs momPz - momMcPz").fill(vtxMC.z(), mom.z() - momMC.z());

        // px resolution clear diverges for electron and positron, indicating the track momentum vector
        // is being extracted from the wrong position...
        // let's try to get the position and momentum at the correct location on the track
        // why is this so complicated!?
        HelicalTrackFit htf = TrackUtils.getHTF(t);

        // propagate this to the vertex z position...
        // Note that HPS y is lcsim z
        double s = HelixUtils.PathToZPlane(htf, vtxMC.y()); //v.y()); //WTF!?
        Hep3Vector pointOnTrackAtVtx = HelixUtils.PointOnHelix(htf, s);
        Hep3Vector dirOfTrackAtVtx = HelixUtils.Direction(htf, s);

        Hep3Vector momAtVtx = VecOp.mult(trackMom, VecOp.unit(dirOfTrackAtVtx));
        if (debug) {
            System.out.println("htf: " + htf);
            System.out.println("Point on track at vtx " + pointOnTrackAtVtx);
            System.out.println("dir of Track at vtx " + dirOfTrackAtVtx);
            System.out.println("momAtVtx " + momAtVtx);
        }

        // does this fix the problem?
        aida.cloud2D(dir + " vtx z vs momAtVtx - momMcPx").fill(vtxMC.z(), momAtVtx.y() - momMC.x());
        aida.cloud2D(dir + " vtx z vs momAtVtx - momMcPy").fill(vtxMC.z(), momAtVtx.z() - momMC.y());
        aida.cloud2D(dir + " vtx z vs momAtVtx - momMcPz").fill(vtxMC.z(), momAtVtx.x() - momMC.z());
        aida.tree().cd("..");
    }

    @Override
    protected void endOfData() {
        if (writePlots) {
            try {
                aida.saveAs("APrimeMCVertexAnalysisDriver_" + myDate() + ".aida");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private String myDate() {
        Calendar cal = new GregorianCalendar();
        Date date = new Date();
        cal.setTime(date);
        DecimalFormat formatter = new DecimalFormat("00");
        String day = formatter.format(cal.get(Calendar.DAY_OF_MONTH));
        String month = formatter.format(cal.get(Calendar.MONTH) + 1);
        return cal.get(Calendar.YEAR) + month + day;
    }

    private double recalculateMass(Vertex v) {
        ReconstructedParticle rp = v.getAssociatedParticle();
        List<ReconstructedParticle> parts = rp.getParticles();
        ReconstructedParticle electron = null;
        ReconstructedParticle positron = null;
        for (ReconstructedParticle part : parts) {
            if (part.getCharge() < 0) {
                electron = part;
            }
            if (part.getCharge() > 0) {
                positron = part;
            }
        }
        //electron
        Track et = electron.getTracks().get(0);
        double etrackMom = electron.getMomentum().magnitude();
        HelicalTrackFit ehtf = TrackUtils.getHTF(et);
        // propagate this to the vertex z position...
        // Note that HPS y is lcsim z
        double es = HelixUtils.PathToZPlane(ehtf, v.getPosition().y());
        Hep3Vector epointOnTrackAtVtx = HelixUtils.PointOnHelix(ehtf, es);
        Hep3Vector edirOfTrackAtVtx = HelixUtils.Direction(ehtf, es);
        Hep3Vector emomAtVtx = VecOp.mult(etrackMom, VecOp.unit(edirOfTrackAtVtx));
        //positron
        Track pt = positron.getTracks().get(0);
        double ptrackMom = positron.getMomentum().magnitude();
        HelicalTrackFit phtf = TrackUtils.getHTF(pt);
        // propagate this to the vertex z position...
        // Note that HPS y is lcsim z
        double ps = HelixUtils.PathToZPlane(phtf, v.getPosition().y());
        Hep3Vector ppointOnTrackAtVtx = HelixUtils.PointOnHelix(phtf, ps);
        Hep3Vector pdirOfTrackAtVtx = HelixUtils.Direction(phtf, ps);
        Hep3Vector pmomAtVtx = VecOp.mult(ptrackMom, VecOp.unit(pdirOfTrackAtVtx));
        
        return invMass(emomAtVtx, pmomAtVtx);
    }

    private double invMass(Hep3Vector p1, Hep3Vector p2) {
        double me2 = 0.000511 * 0.000511;
        double esum = sqrt(p1.magnitudeSquared() + me2) + sqrt(p2.magnitudeSquared() + me2);
        double pxsum = p1.x() + p2.x();
        double pysum = p1.y() + p2.y();
        double pzsum = p1.z() + p2.z();

        double psum = Math.sqrt(pxsum * pxsum + pysum * pysum + pzsum * pzsum);
        double evtmass = esum * esum - psum * psum;

        if (evtmass > 0) {
            return Math.sqrt(evtmass);
        } else {
            return -99;
        }
    }

    private boolean matchesMC(EventHeader event, Vertex v, MCParticle electronMC, MCParticle positronMC) {
        ReconstructedParticle rp = v.getAssociatedParticle();
        List<ReconstructedParticle> parts = rp.getParticles();
        ReconstructedParticle electron = null;
        ReconstructedParticle positron = null;
        for (ReconstructedParticle part : parts) {
            if (part.getCharge() < 0) {
                electron = part;
            }
            if (part.getCharge() > 0) {
                positron = part;
            }
        }
        if (!matches(event, electron, electronMC)) {
            return false;
        }
        if (!matches(event, positron, positronMC)) {
            return false;
        }
        return true;
    }

    private boolean matches(EventHeader event, ReconstructedParticle rp, MCParticle mcp) {
        // make relational table for strip clusters to mc particle
        RelationalTable mcHittomcP = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        RelationalTable rawtomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        if (event.hasCollection(LCRelation.class, "SVTTrueHitRelations")) {
            List<LCRelation> trueHitRelations = event.get(LCRelation.class, "SVTTrueHitRelations");
            for (LCRelation relation : trueHitRelations) {
                if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                    rawtomc.add(relation.getFrom(), relation.getTo());
                }
            }
        }
        Track t = rp.getTracks().get(0);
        List<TrackerHit> hits = t.getTrackerHits();
        for (TrackerHit h : hits) {
            for (RawTrackerHit rth : (List<RawTrackerHit>) h.getRawHits()) {
                Set<SimTrackerHit> simTrackerHits = rawtomc.allFrom(rth);
                if (simTrackerHits != null) {
                    for (SimTrackerHit simHit : simTrackerHits) {
                        MCParticle trackMcp = simHit.getMCParticle();
                        if (!trackMcp.equals(mcp)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
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
}
