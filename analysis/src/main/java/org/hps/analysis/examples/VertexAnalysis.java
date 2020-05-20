package org.hps.analysis.examples;

import static java.lang.Math.sqrt;
import java.util.List;
import java.util.Map;
import org.hps.recon.tracking.TrackType;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Vertex;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.fourvec.Lorentz4Vector;
import org.lcsim.util.fourvec.Momentum4Vector;

/**
 *
 * @author Norman A. Graf
 */
public class VertexAnalysis extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    String vertexCollectionName = "UnconstrainedV0Vertices";
    String[] names = {"X", "Y", "Z"};
    double[] p1 = new double[4];
    double[] p2 = new double[4];
    double[] pV = new double[3];
    double emass = 0.000511;
    double kmass = 0.493667;
    double phimass = 1.019;
    double emass2 = emass * emass;
    double kmass2 = kmass * kmass;
    boolean debug = false;

    protected void process(EventHeader event) {
        List<ReconstructedParticle> V0List = event.get(ReconstructedParticle.class, "UnconstrainedV0Candidates");
        if (V0List.size() != 2) {
            return;
        }
        ReconstructedParticle V0 = V0List.get(1);
        List<ReconstructedParticle> trks = V0.getParticles();
        ReconstructedParticle ele = trks.get(0);
        ReconstructedParticle pos = trks.get(1);

        boolean noClusters = false;
        boolean oneCluster = false;
        boolean twoClusters = false;
        if (TrackType.isGBL(ele.getTracks().get(0).getType())) {
            noClusters = (ele.getClusters().size() == 0 && pos.getClusters().size() == 0);
            oneCluster = ((ele.getClusters().size() == 1 && pos.getClusters().size() == 0) || (ele.getClusters().size() == 0 && pos.getClusters().size() == 1));
            twoClusters = (ele.getClusters().size() == 1 && pos.getClusters().size() == 1);
        }
        List<Vertex> vertices = event.get(Vertex.class, vertexCollectionName);
        for (Vertex v : vertices) {
            Map<String, Double> vals = v.getParameters();
            // System.out.println(vals);
            p1[0] = vals.get("p1X");
            p1[1] = vals.get("p1Y");
            p1[2] = vals.get("p1Z");
            p2[0] = vals.get("p2X");
            p2[1] = vals.get("p2Y");
            p2[2] = vals.get("p2Z");
            double vop = vals.get("V0P");
            pV[0] = vals.get("V0Px");
            pV[1] = vals.get("V0Py");
            pV[2] = vals.get("V0Pz");
            double k1 = 0;
            double k2 = 0.;
            for (int i = 0; i < 3; ++i) {
                k1 += p1[i] * p1[i];
                k2 += p2[i] * p2[i];
            }
            k1 = sqrt(k1 + kmass2);
            k2 = sqrt(k2 + kmass2);
            Momentum4Vector kvec1 = new Momentum4Vector(p1[0], p1[1], p1[2], k1);
            Momentum4Vector kvec2 = new Momentum4Vector(p2[0], p2[1], p2[2], k2);
            Lorentz4Vector kksum = kvec1.plus(kvec2);
            double kkmass = kksum.mass();
            double invMass = vals.get("invMass");
//            if (debug) {
//                System.out.println("mass: " + eemass + " invMass: " + invMass + " delta: " + (invMass - eemass));
//            }
            aida.histogram1D("vertex invariant mass", 100, 0., 0.4).fill(invMass);
            aida.histogram1D("vertex invariant mass K+K-", 100, 0.95, 1.95).fill(kkmass);
            aida.histogram1D("vertex invariant mass phi search", 100, 0.98, 1.06).fill(kkmass);
            if (noClusters) {
                aida.histogram1D("vertex invariant mass phi search no clusters", 100, 0.98, 1.06).fill(kkmass);
            }
            if (oneCluster) {
                aida.histogram1D("vertex invariant mass phi search one cluster", 100, 0.98, 1.06).fill(kkmass);
            }
            if (twoClusters) {
                aida.histogram1D("vertex invariant mass phi search two clusters", 100, 0.98, 1.06).fill(kkmass);
            }

        }
    }
}
