/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon.vertexing;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.particle.properties.ParticlePropertyManager;
import hep.physics.particle.properties.ParticlePropertyProvider;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.BasicHepLorentzVector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.HepLorentzVector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.hps.recon.tracking.HelixConverter;
import org.hps.recon.tracking.StraightLineTrack;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseMCParticle;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelixParamCalculator;
import org.lcsim.math.chisq.ChisqProb;
import org.lcsim.recon.tracking.seedtracker.SeedStrategy;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author partridge
 */
public class AnalysisDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    int nevt = 0;
    private List<SeedStrategy> _slist = new ArrayList<SeedStrategy>();

    public AnalysisDriver() {
    }

    
    public void process(EventHeader event) {

        //  Increment the event counter
        nevt++;

        //  Set the magnetic field
        double bfield = 1.0;
        double xref = 50.;

        Random rn = new Random();

        for (int iloop = 0; iloop < 10000; iloop++) {

            double p0 = 0.5 + rn.nextDouble();
            double py0 = 0.1 * p0 * rn.nextGaussian();
            double pz0 = 0.1 * p0 * rn.nextGaussian();
            double px0 = Math.sqrt(p0 * p0 - py0 * py0 - pz0 * pz0);
            double len = 20. * rn.nextDouble();
            double vx = len * px0 / p0;
            double vy = len * py0 / p0;
            double vz = len * pz0 / p0;
            Hep3Vector vertex = new BasicHep3Vector(vx, vy, vz);

            double py1 = 0.5 * py0 + 0.05 * p0 * (rn.nextDouble() - 0.5);
            double pz1 = 0.5 * pz0 + 0.05 * p0 * (rn.nextDouble() - 0.5);
            double px1 = 0.5 * px0 + 0.05 * p0 * (rn.nextDouble() - 0.5);
            Hep3Vector p1 = new BasicHep3Vector(px1, py1, pz1);
            double m = 0.000511;
            double e1 = Math.sqrt(p1.magnitudeSquared() + m * m);
            HepLorentzVector p4v1 = new BasicHepLorentzVector(e1, p1);
            double ox1 = xref;
            double oy1 = vy + py1 * (ox1 - vx) / px1;
            double oz1 = vz + pz1 * (ox1 - vx) / px1;
            Hep3Vector origin1 = new BasicHep3Vector(ox1, oy1, oz1);

            double py2 = py0 - py1;
            double pz2 = pz0 - pz1;
            double px2 = px0 - px1;
            Hep3Vector p2 = new BasicHep3Vector(px2, py2, pz2);
            double e2 = Math.sqrt(p2.magnitudeSquared() + m * m);
            HepLorentzVector p4v2 = new BasicHepLorentzVector(e2, p2);
            double ox2 = xref;
            double oy2 = vy + py2 * (ox2 - vx) / px2;
            double oz2 = vz + pz2 * (ox2 - vx) / px2;
            Hep3Vector origin2 = new BasicHep3Vector(ox2, oy2, oz2);

            ParticlePropertyProvider ptp = ParticlePropertyManager.getParticlePropertyProvider();
            BaseMCParticle mcp1 = new BaseMCParticle(origin1, p4v1, ptp.get(11), BaseMCParticle.FINAL_STATE, 0.);
            BaseMCParticle mcp2 = new BaseMCParticle(origin2, p4v2, ptp.get(-11), BaseMCParticle.FINAL_STATE, 0.);

            HelixParamCalculator hp1 = new HelixParamCalculator(mcp1, bfield);
            HelixParamCalculator hp2 = new HelixParamCalculator(mcp2, bfield);

            double[] pars1 = new double[5];
            double[] pars2 = new double[5];
            double omega1 = hp1.getMCOmega();
            double omega2 = hp2.getMCOmega();
            double domega = 1.e-2 * (omega1 + omega2) / 2.;
            double dDCA = 1.e-2;
            double dphi0 = 1.e-4;
            double dz0 = 1.e-2;
            double dslope = 1.e-4;
            SymmetricMatrix cov = new SymmetricMatrix(5);
            cov.setElement(HelicalTrackFit.curvatureIndex, HelicalTrackFit.curvatureIndex, domega * domega);
            cov.setElement(HelicalTrackFit.dcaIndex, HelicalTrackFit.dcaIndex, dDCA * dDCA);
            cov.setElement(HelicalTrackFit.phi0Index, HelicalTrackFit.phi0Index, dphi0 * dphi0);
            cov.setElement(HelicalTrackFit.z0Index, HelicalTrackFit.z0Index, dz0 * dz0);
            cov.setElement(HelicalTrackFit.slopeIndex, HelicalTrackFit.slopeIndex, dslope * dslope);
            pars1[HelicalTrackFit.curvatureIndex] = omega1 + domega * rn.nextGaussian();
            pars1[HelicalTrackFit.dcaIndex] = hp1.getDCA() + dDCA * rn.nextGaussian();
            pars1[HelicalTrackFit.phi0Index] = hp1.getPhi0() + dphi0 * rn.nextGaussian();
            pars1[HelicalTrackFit.z0Index] = hp1.getZ0() + dz0 * rn.nextGaussian();
            pars1[HelicalTrackFit.slopeIndex] = hp1.getSlopeSZPlane() + dslope * rn.nextGaussian();
            pars2[HelicalTrackFit.curvatureIndex] = omega2 + domega * rn.nextGaussian();
            pars2[HelicalTrackFit.dcaIndex] = hp2.getDCA() + dDCA * rn.nextGaussian();
            pars2[HelicalTrackFit.phi0Index] = hp2.getPhi0() + dphi0 * rn.nextGaussian();
            pars2[HelicalTrackFit.z0Index] = hp2.getZ0() + dz0 * rn.nextGaussian();
            pars2[HelicalTrackFit.slopeIndex] = hp2.getSlopeSZPlane() + dslope * rn.nextGaussian();

            double[] chisq = new double[2];
            int[] ndof = new int[2];

            HelicalTrackFit h1 = new HelicalTrackFit(pars1, cov, chisq, ndof, null, null);
            HelicalTrackFit h2 = new HelicalTrackFit(pars2, cov, chisq, ndof, null, null);

            //  Create a list of straight line tracks
            List<StraightLineTrack> sltlist = new ArrayList<StraightLineTrack>();

            HelixConverter converter = new HelixConverter(xref);
            sltlist.add(converter.Convert(h1));
            sltlist.add(converter.Convert(h2));

            VertexFitter vtxfit = new VertexFitter(xref);
            boolean success = vtxfit.VertexFit(sltlist);
            if (success) {
                VertexFit vfit = vtxfit.getFit();
                aida.histogram1D("Chi square of vertex fit", 100, 0., 10.).fill(vfit.chisq());
                Hep3Vector vtx = vfit.vtx();
                SymmetricMatrix vtxcov = vfit.covariance();
                double dvx = Math.sqrt(vtxcov.diagonal(0));
                double dvy = Math.sqrt(vtxcov.diagonal(1));
                double dvz = Math.sqrt(vtxcov.diagonal(2));
                aida.histogram1D("Vertex position x", 100, -5., 25.).fill(vtx.x());
                aida.histogram1D("Vertex position y", 100, -5., 5.).fill(vtx.y());
                aida.histogram1D("Vertex position z", 100, -5., 5.).fill(vtx.z());
                aida.histogram1D("Vertex fit x residual", 100, -3., 3.).fill(vtx.x() - vertex.x());
                aida.histogram1D("Vertex fit y residual", 100, -0.5, 0.5).fill(vtx.y() - vertex.y());
                aida.histogram1D("Vertex fit z residual", 100, -0.5, 0.5).fill(vtx.z() - vertex.z());
                aida.histogram1D("Vertex x pull", 100, -4., 4.).fill((vtx.x() - vertex.x()) / dvx);
                aida.histogram1D("Vertex y pull", 100, -4., 4.).fill((vtx.y() - vertex.y()) / dvy);
                aida.histogram1D("Vertex z pull", 100, -4., 4.).fill((vtx.z() - vertex.z()) / dvz);
                aida.histogram1D("Vertex position x uncertainty", 100, 0., 1.0).fill(dvx);
                aida.histogram1D("Vertex position y uncertainty", 100, 0., 0.1).fill(dvy);
                aida.histogram1D("Vertex position z uncertainty", 100, 0., 0.1).fill(dvz);
                aida.histogram1D("Confidence Level", 50, 0., 1.).fill(ChisqProb.gammp(1, vfit.chisq()));
            }
        }
        return;
    }
}