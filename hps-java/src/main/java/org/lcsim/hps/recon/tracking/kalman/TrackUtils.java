/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lcsim.hps.recon.tracking.kalman;

import hep.physics.matrix.SymmetricMatrix;

import org.lcsim.event.MCParticle;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.recon.tracking.trfbase.TrackError;
import org.lcsim.recon.tracking.trfbase.TrackSurfaceDirection;
import org.lcsim.recon.tracking.trfbase.TrackVector;
import org.lcsim.recon.tracking.trfbase.VTrack;
import org.lcsim.recon.tracking.trfdca.SurfDCA;
import org.lcsim.recon.tracking.trfutil.TRFMath;

import Jama.Matrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import org.lcsim.fit.helicaltrack.HelixParamCalculator;

/**
 * Class for converting lcsim tracks to trf tracks.
 * @author ecfine
 */
public class TrackUtils {

    private double mmTocm = 0.1; // TRF wants distances in cm, not mm.
    public double bz = 0.5;
    private double _epsilon = 1e-4;
    boolean _DEBUG = false;

    // Converts a HelicalTrackFit to a VTrack, prints track params.
    public VTrack makeVTrack(HelicalTrackFit t) {

        // Opposite sign convention.
        double r_signed = -(t.dca());

        TrackVector tv = new TrackVector();
        tv.set(0, r_signed * mmTocm);
        tv.set(1, t.z0() * mmTocm);
        tv.set(2, Math.tan(t.phi0()));
        tv.set(3, t.slope());
        tv.set(4, -t.curvature() / mmTocm / (TRFMath.BFAC * bz)); // curvature to q/pt
        double q = t.pT(bz) / (t.R() * mmTocm * TRFMath.BFAC * bz);
        if ((q / t.pT(bz) + tv.get(4)) > _epsilon) {
            System.out.println("something wrong with curvature? q/pt = "
                    + (q / t.pT(bz)) + ", -tv.get(4) = " + -tv.get(4));
        }
//        SurfDCA s = new SurfDCA((t.dca() * Math.sin(t.phi0())),
//                (-t.dca() * Math.cos(t.phi0())));
        double dcaX = mmTocm * HelixUtils.PointOnHelix(t, 0).x();
        double dcaY = mmTocm * HelixUtils.PointOnHelix(t, 0).y();
        if(_DEBUG)System.out.println("DCA X="+dcaX+"; Y="+dcaY);
//        SurfDCA s = new SurfDCA(dcaX, dcaY);
//        I think this should be 0,0
        SurfDCA s = new SurfDCA(0, 0);

        //mg this is just wrong...
//        SurfDCA s = new SurfDCA(r_signed * mmTocm, 0.);

//        VTrack vt = new VTrack(s, tv);
        VTrack vt = new VTrack(s, tv, TrackSurfaceDirection.TSD_FORWARD);
        if (_DEBUG) {
            System.out.println("making VTrack with: ");
            System.out.println("    r_signed = " + r_signed * mmTocm);
            System.out.println("    z0 = " + t.z0() * mmTocm);
            System.out.println("    tanphi0 =  " + Math.tan(t.phi0()));
            System.out.println("    tanlamda = " + t.slope());
            System.out.println("    q/pt = " + -t.curvature() / mmTocm / (TRFMath.BFAC * bz));
//        System.out.println("    q/pt = " + t.curvature() * 0.299999 * bz);
            System.out.println("from HelicalTrackFit with: ");
            System.out.println("    dca = " + t.dca() + "+/-" + Math.sqrt(t.covariance().e(HelicalTrackFit.dcaIndex, HelicalTrackFit.dcaIndex)));
            System.out.println("    z0 = " + t.z0() + "+/-" + Math.sqrt(t.covariance().e(HelicalTrackFit.z0Index, HelicalTrackFit.z0Index)));
            System.out.println("    phi0 =  " + t.phi0() + "+/-" + Math.sqrt(t.covariance().e(HelicalTrackFit.phi0Index, HelicalTrackFit.phi0Index)));
            System.out.println("    slope = " + t.slope() + "+/-" + Math.sqrt(t.covariance().e(HelicalTrackFit.slopeIndex, HelicalTrackFit.slopeIndex)));
            System.out.println("    curvature = " + t.curvature() + "+/-" + Math.sqrt(t.covariance().e(HelicalTrackFit.curvatureIndex, HelicalTrackFit.curvatureIndex)));
//        System.out.println("SurfDCA at " + t.dca() * Math.sin(t.phi0()) + ", " +
//                -t.dca() * Math.cos(t.phi0()));
        }
        return vt;
    }
    //make a VTrack from an MC particle

    public VTrack makeVTrack(MCParticle mcp) {
        TrackVector tv = new TrackVector();
        HelixParamCalculator helix = new HelixParamCalculator(mcp, bz);
        double r_signed = -(helix.getDCA());
        double curvemc = (-1) / helix.getRadius();
        tv.set(0, r_signed * mmTocm);
        tv.set(1, helix.getZ0() * mmTocm);
        tv.set(2, Math.tan(helix.getPhi0()));
        tv.set(3, helix.getSlopeSZPlane());
        tv.set(4, curvemc / mmTocm / (TRFMath.BFAC * bz)); // curvature to q/pt      
//        SurfDCA s = new SurfDCA(r_signed * mmTocm, 0.);
         SurfDCA s = new SurfDCA(0, 0.);
        VTrack vt = new VTrack(s, tv);
        return vt;

    }

    public void setBZ(double b) {
        bz = b;
    }

    public Hep3Vector getMomentum(VTrack vt){
        double qoverpt=vt.qOverP();
        double phi0=Math.atan(vt.vector().get(2));
        double slope=vt.vector().get(3);
        double Pt = Math.abs(1./qoverpt);
        double px = Pt * Math.cos(phi0);
        double py = Pt * Math.sin(phi0);
        double pz = Pt * slope;
        Hep3Vector p=new BasicHep3Vector(px,py,pz);
 
        return p;
    }

    // Returns a TrackError from a HelicalTrackFit.
    public TrackError getInitalError(HelicalTrackFit t) {
        SymmetricMatrix oldError = t.covariance();
        SymmetricMatrix newError = new SymmetricMatrix(5);
        double edca = oldError.e(HelicalTrackFit.dcaIndex, HelicalTrackFit.dcaIndex);
        newError.setElement(0, 0, edca * mmTocm * mmTocm * 1e6);
        double ez0 = oldError.e(HelicalTrackFit.z0Index, HelicalTrackFit.z0Index);
        newError.setElement(1, 1, ez0 * mmTocm * mmTocm * 1e6);
        double ephi0 = oldError.e(HelicalTrackFit.phi0Index, HelicalTrackFit.phi0Index);
        newError.setElement(2, 2, ephi0 * 1e6);
        double eslope = oldError.e(HelicalTrackFit.slopeIndex, HelicalTrackFit.slopeIndex);
        newError.setElement(3, 3, eslope * 100000);
        double ecurve = oldError.e(HelicalTrackFit.curvatureIndex, HelicalTrackFit.curvatureIndex);
        newError.setElement(4, 4, (ecurve / Math.pow(TRFMath.BFAC * bz * mmTocm, 2)) * 1e6);

//        for (int i = 0; i < error.getNColumns(); i++){
//            double newElement = error.e(i, i) * 10000;
//            error.setElement(i, i, newElement);
//        }
        Matrix errorMatrix = new Matrix(5, 5);
        for (int k = 0; k < 5; k++) {
            for (int j = 0; j < 5; j++) {
                errorMatrix.set(k, j, newError.e(k, j));
            }
        }
        if(_DEBUG) System.out.println("Setting initial error:\n"+errorMatrix.toString());
        TrackError trackerror = new TrackError(errorMatrix);
        return trackerror;
    }
}
