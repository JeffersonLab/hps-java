package org.hps.analysis.vertex;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.Hep3Vector;
import java.io.IOException;
import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import org.hps.recon.tracking.TrackType;
import org.lcsim.detector.RotationPassiveXYZ;
import org.lcsim.detector.Transform3D;
import org.lcsim.detector.Translation3D;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.Vertex;
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

    private AIDA aida = AIDA.defaultInstance();

    protected void process(EventHeader event) {
        List<MCParticle> mcparts = event.getMCParticles();
        MCParticle aprime = null;
        for (MCParticle mcp : mcparts) {
            if (mcp.getPDGID() == 622) {
                aprime = mcp;
            }
        }
        if (debug) {
            System.out.println("aprime " + aprime);
        }
        Hep3Vector aprimeVtx = aprime.getEndPoint();
        double aprimeMass = aprime.getMass();
        List<Vertex> vertices = event.get(Vertex.class, vertexCollectionName);
        for (Vertex v : vertices) {
            aida.tree().cd("/");
            ReconstructedParticle rp = v.getAssociatedParticle();
            int type = rp.getType();
            if (TrackType.isGBL(type)) {
                SymmetricMatrix cov = v.getCovMatrix();
                Hep3Vector pos = v.getPosition();

            // try some transforms here...
//            rotMat.rotate(cov);
//            rotMat.rotate(pos);
//            xformMat.transform(aprimeVtx);
                if (debug) {
                    System.out.println("cov matrix: \n" + cov);
                    System.out.println("vertex pos: \n" + pos);
                }
                Map<String, Double> vals = v.getParameters();
                //System.out.println(vals);
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
                aida.cloud1D("vertex chi2 prop").fill(v.getProbability());
                aida.cloud1D("vertex fitted invariant mass").fill(rp.getMass());
                aida.cloud1D("vertex fitted mass - MC mass").fill(rp.getMass() - aprimeMass);
                aida.cloud2D("vertex fitted mass - MC mass vs MC z").fill(aprimeVtx.z(), rp.getMass() - aprimeMass);
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
                analyzeParticle("electron", electron);
                analyzeParticle("positron", positron);
                aida.tree().cd("/");
            }// only analyze GBL tracks
        }
    }

    private void analyzeParticle(String dir, ReconstructedParticle p) {
        aida.tree().mkdirs(dir);
        aida.tree().cd(dir);
        //
        Track t = p.getTracks().get(0);
        double chiSquared = t.getChi2();
        int ndf = t.getNDF();
        double chisqProb = ChisqProb.gammp(ndf, chiSquared);
        aida.cloud1D("Track chisq per df").fill(chiSquared / ndf);
        aida.cloud1D("Track chisq prob").fill(chisqProb);
        aida.cloud1D("Track nHits").fill(t.getTrackerHits().size());
        aida.tree().cd("..");
    }

    @Override
    protected void endOfData() {
        try {
            aida.saveAs("APrimeMCVertexAnalysisDriver_" + myDate() + ".aida");
        } catch (IOException ex) {
            ex.printStackTrace();
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
}
