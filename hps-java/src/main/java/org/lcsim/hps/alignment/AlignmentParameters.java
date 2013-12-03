package org.lcsim.hps.alignment;

import hep.physics.matrix.BasicMatrix;
import hep.physics.matrix.MatrixOp;
import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.fit.helicaltrack.MultipleScatter;
import org.lcsim.fit.helicaltrack.TrackDirection;
import org.lcsim.hps.event.HPSTransformations;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;

/**
 * Class to calculate and print the residuals and derivatives
 * of the alignment parameters...used as input for MillePede
 * Notation follows the MillePede manual:
 * http://www.desy.de/~blobel/Mptwo.pdf
 *
 * the track is measured in the HelicalTrackFit frame
 * and residuals are in the sensor frame (u,v,w)
 *
 * ordering of track parameters is
 *    double d0 = _trk.dca();
 *    double z0 = _trk.z0();
 *    double slope = _trk.slope();
 *    double phi0 = _trk.phi0();
 *    double R = _trk.R();
 *
 * @author mgraham
 */
public class AlignmentParameters {

    private int _nlc = 5;  //the five track parameters
    private int _ngl = 1; //delta(u) and delta(gamma) for each plane
    private BasicMatrix _dfdq;
    private BasicMatrix _dfdp;
    private HelicalTrackFit _trk;
    private double[] _resid = new double[3];
    private double[] _error = new double[3];
    private int[] _globalLabel = new int[1];
    FileWriter fWriter;
    PrintWriter pWriter;
    Set<SiSensor> _process_sensors = new HashSet<SiSensor>();
    boolean _DEBUG = false;
    double smax = 1e3;

    public AlignmentParameters(String outfile) {
        try {
//open things up
            fWriter = new FileWriter(outfile);
            pWriter = new PrintWriter(fWriter);
        } catch (IOException ex) {
            Logger.getLogger(RunAlignment.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void PrintResidualsAndDerivatives(Track track) {
        SeedTrack st = (SeedTrack) track;
        SeedCandidate seed = st.getSeedCandidate();
        Map<HelicalTrackHit, MultipleScatter> msmap = seed.getMSMap();
        _trk = seed.getHelix();
        List<TrackerHit> hitsOnTrack = track.getTrackerHits();
        for (TrackerHit hit : hitsOnTrack) {
            HelicalTrackHit htc = (HelicalTrackHit) hit;
            double msdrphi = msmap.get(htc).drphi();
            double msdz = msmap.get(htc).dz();
            double sHit = _trk.PathMap().get(htc);
            HelicalTrackCross cross = (HelicalTrackCross) htc;
            List<HelicalTrackStrip> clusterlist = cross.getStrips();
            TrackDirection trkdir = HelixUtils.CalculateTrackDirection(_trk, sHit);
            cross.setTrackDirection(trkdir, _trk.covariance());
            for (HelicalTrackStrip cl : clusterlist) {
                CalculateLocalDerivatives(cl);
                CalculateGlobalDerivatives(cl);
                CalculateResidual(cl, msdrphi, msdz);
//                CalculateResidual(cl, 0,0);
                PrintStripResiduals(cl);
            }
        }
        AddTarget(0.1, 0.02);
    }

    private void CalculateLocalDerivatives(HelicalTrackStrip strip) {
        //get track parameters.
        double d0 = _trk.dca();
        double z0 = _trk.z0();
        double slope = _trk.slope();
        double phi0 = _trk.phi0();
        double R = _trk.R();
//strip origin is defined in the tracking coordinate system (x=beamline)
        double xint = strip.origin().x();
        double s = HelixUtils.PathToXPlane(_trk, xint, smax, _nlc).get(0);
        double phi = s / R - phi0;
        double[][] dfdq = new double[3][5];
        //dx/dq
        //these are wrong for X, but for now it doesn't matter
        dfdq[0][0] = Math.sin(phi0);
        dfdq[0][1] = 0;
        dfdq[0][2] = 0;
        dfdq[0][3] = d0 * Math.cos(phi0) + R * Math.sin(phi0) - s * Math.cos(phi0);
        dfdq[0][4] = (phi - phi0) * Math.cos(phi0);
        double[] mydydq = dydq(R, d0, phi0, xint, s);
        double[] mydzdq = dzdq(R, d0, phi0, xint, slope, s);
        for (int i = 0; i < 5; i++) {
            dfdq[1][i] = mydydq[i];
            dfdq[2][i] = mydzdq[i];
        }

        BasicMatrix dfdqGlobal = FillMatrix(dfdq, 3, 5);
        Hep3Matrix trkToStrip = getTrackToStripRotation(strip);
        _dfdq = (BasicMatrix) MatrixOp.mult(trkToStrip, dfdqGlobal);

        if (_DEBUG) {
            double[] trackpars = {d0, z0, slope, phi0, R, s, xint};
            System.out.println("Strip Origin: ");
            System.out.println(strip.origin());
            System.out.println("trkToStrip Rotation:");
            System.out.println(trkToStrip.toString());
            printDerivatives(trackpars, dfdq);
        }
    }

    private void CalculateGlobalDerivatives(HelicalTrackStrip strip) {
        //1st index = alignment parameter (only u so far)
        //2nd index = residual coordinate (on du so far)

        double[][] dfdpLab = new double[3][1];
        dfdpLab[0][0] = 0; //df/dx
        dfdpLab[1][0] = 0; //df/dy
        dfdpLab[2][0] = 1; //df/dz
        BasicMatrix _dfdpLab = FillMatrix(dfdpLab, 3, 1);
        Hep3Matrix trkToStrip = getTrackToStripRotation(strip);
        _dfdp = (BasicMatrix) MatrixOp.mult(trkToStrip, _dfdpLab);
        if (_DEBUG) {
            System.out.printf("dfdz = %5.5f     %5.5f   %5.5f\n", _dfdp.e(0, 0), _dfdp.e(1, 0), _dfdp.e(2, 0));
        }
        _globalLabel[0] = GetIdentifier(strip);
//         _globalLabel[0] = GetIdentifierModule(strip);

    }

    private void CalculateResidual(HelicalTrackStrip strip, double msdrdphi, double msdz) {

        Hep3Vector u = strip.u();
        Hep3Vector v = strip.v();
        Hep3Vector w = strip.w();
        Hep3Vector corigin = strip.origin();
        double phi0 = _trk.phi0();
        double R = _trk.R();
        double xint = strip.origin().x();
        double s = HelixUtils.PathToXPlane(_trk, xint, smax, _nlc).get(0);
        double phi = s / R - phi0;
        Hep3Vector trkpos = HelixUtils.PointOnHelix(_trk, s);

        //System.out.println("trkpos = "+trkpos.toString());
        //System.out.println("origin = "+corigin.toString());

        Hep3Vector mserr = new BasicHep3Vector(msdrdphi * Math.sin(phi), msdrdphi * Math.sin(phi), msdz);
        Hep3Vector vdiffTrk = VecOp.sub(trkpos, corigin);
        Hep3Matrix trkToStrip = getTrackToStripRotation(strip);
        Hep3Vector vdiff = VecOp.mult(trkToStrip, vdiffTrk);
        double umc = vdiff.x();
        double vmc = vdiff.y();
        double wmc = vdiff.z();
        double umeas = strip.umeas();
        double uError = strip.du();
        double msuError = VecOp.dot(mserr, u);
        double vmeas = 0;
        double vError = (strip.vmax() - strip.vmin()) / Math.sqrt(12);
        double wmeas = 0;
        double wError = 0.001;
        //System.out.println("strip error="+uError+"; ms error ="+msuError);
        _resid[0] = umeas - umc;
        _error[0] = Math.sqrt(uError * uError + msuError * msuError);
        _resid[1] = vmeas - vmc;
        _error[1] = vError;
        _resid[2] = wmeas - wmc;
        _error[2] = wError;
        if (_DEBUG) {
            System.out.println("Strip Origin: ");
            System.out.println(corigin.toString());
            System.out.println("Position on Track:");
            System.out.println(trkpos.toString());
            System.out.println("vdiff :");
            System.out.println(vdiff.toString());
            System.out.println("u :");
            System.out.println(u.toString());
            System.out.println("umeas = " + umeas + "; umc = " + umc);
            System.out.println("udiff = " + _resid[0] + " +/- " + _error[0]);

        }

    }

    public double[] getResidual(Track track, int layer) {
        double[] res = new double[7];
        SeedTrack st = (SeedTrack) track;
        SeedCandidate seed = st.getSeedCandidate();
        Map<HelicalTrackHit, MultipleScatter> msmap = seed.getMSMap();
        _trk = seed.getHelix();
        List<TrackerHit> hitsOnTrack = track.getTrackerHits();
        for (TrackerHit hit : hitsOnTrack) {
            HelicalTrackHit htc = (HelicalTrackHit) hit;
            double sHit = _trk.PathMap().get(htc);
            HelicalTrackCross cross = (HelicalTrackCross) htc;
            List<HelicalTrackStrip> clusterlist = cross.getStrips();
            TrackDirection trkdir = HelixUtils.CalculateTrackDirection(_trk, sHit);
            double msdrphi = msmap.get(htc).drphi();
            double msdz = msmap.get(htc).dz();
            cross.setTrackDirection(trkdir, _trk.covariance());
            for (HelicalTrackStrip cl : clusterlist) {
                if (cl.layer() == layer) {
                    CalculateResidual(cl, msdrphi, msdz);
                    res[0] = _resid[0];
                    res[1] = _resid[1];
                    res[2] = _resid[2];
                    res[3] = _error[0];
                    res[4] = _error[1];
                    res[5] = _error[2];
                    res[6] = layer;
                    if(hit.getPosition()[2]<0)res[6]=layer+10;
                }
            }
        }
        return res;

    }

    public void AddTarget(double beamdy, double beamdz) {
        double[][] dfdp = new double[3][1];
        double d0 = _trk.dca();
        double z0 = _trk.z0();
        double slope = _trk.slope();
        double phi0 = _trk.phi0();
        double R = _trk.R();
        double xint = 0; //target
        double s = HelixUtils.PathToXPlane(_trk, xint, smax, _nlc).get(0);
        Hep3Vector ptAtTarget = HelixUtils.PointOnHelix(_trk, s);
        double[] mydydq = dydq(R, d0, phi0, xint, s);
        double[] mydzdq = dzdq(R, d0, phi0, xint, slope, s);
        _resid[0] = ptAtTarget.z();
        _resid[1] = ptAtTarget.y();
        _resid[2] = ptAtTarget.x();
        _error[0] = beamdz;
        _error[1] = beamdy;
        _error[2] = 666;
        dfdp[0][0] = 1;
        dfdp[1][0] = 0;
        dfdp[2][0] = 0;
        _dfdp = FillMatrix(dfdp, 3, 1);
        _globalLabel[0] = 666;
        pWriter.printf("%4d\n", 666);
        pWriter.printf("%5.5e %5.5e %5.5e\n", _resid[0], _resid[1], _resid[2]);
        pWriter.printf("%5.5e %5.5e %5.5e\n", _error[0], _error[1], _error[2]);
        for (int i = 0; i < _nlc; i++) {
            pWriter.printf("%5.5e %5.5e -1.0\n", mydzdq[i], mydydq[i]);
        }
        for (int j = 0; j < _ngl; j++) {
            pWriter.printf("%5.5e %5.5e %5.5e   %5d\n", _dfdp.e(0, j), _dfdp.e(1, j), _dfdp.e(2, j), _globalLabel[j]);
        }

    }

    private void PrintStripResiduals(HelicalTrackStrip strip) {
        if (_DEBUG) {
            System.out.printf("Strip Layer =  %4d\n", strip.layer());
            System.out.printf("Residuals (u,v,w) : %5.5e %5.5e %5.5e\n", _resid[0], _resid[1], _resid[2]);
            System.out.printf("Errors (u,v,w)    : %5.5e %5.5e %5.5e\n", _error[0], _error[1], _error[2]);
            String[] q = {"d0", "z0", "slope", "phi0", "R"};
            System.out.println("track parameter derivatives");
            for (int i = 0; i < _nlc; i++) {
                System.out.printf("%s     %5.5e %5.5e %5.5e\n", q[i], _dfdq.e(0, i), _dfdq.e(1, i), _dfdq.e(2, i));
            }
            String[] p = {"u-displacement"};
            System.out.println("global parameter derivatives");
            for (int j = 0; j < _ngl; j++) {
                System.out.printf("%s  %5.5e %5.5e %5.5e   %5d\n", p[j], _dfdp.e(0, j), _dfdp.e(1, j), _dfdp.e(2, j), _globalLabel[j]);
            }

        }
        pWriter.printf("%4d\n", strip.layer());
        pWriter.printf("%5.5e %5.5e %5.5e\n", _resid[0], _resid[1], _resid[2]);
        pWriter.printf("%5.5e %5.5e %5.5e\n", _error[0], _error[1], _error[2]);
        for (int i = 0; i < _nlc; i++) {
            pWriter.printf("%5.5e %5.5e %5.5e\n", _dfdq.e(0, i), _dfdq.e(1, i), _dfdq.e(2, i));
        }
        for (int j = 0; j < _ngl; j++) {
            pWriter.printf("%5.5e %5.5e %5.5e   %5d\n", _dfdp.e(0, j), _dfdp.e(1, j), _dfdp.e(2, j), _globalLabel[j]);
        }
    }

    private Hep3Matrix getTrackToStripRotation(HelicalTrackStrip strip) {
        ITransform3D detToStrip = GetGlobalToLocal(strip);
        Hep3Matrix detToStripMatrix = (BasicHep3Matrix) detToStrip.getRotation().getRotationMatrix();
        Hep3Matrix detToTrackMatrix = (BasicHep3Matrix) HPSTransformations.getMatrix();

        if (_DEBUG) {
            System.out.println("gblToLoc translation:");
            System.out.println(detToStrip.getTranslation().toString());
            System.out.println("gblToLoc Rotation:");
            System.out.println(detToStrip.getRotation().toString());
            System.out.println("detToTrack Rotation:");
            System.out.println(detToTrackMatrix.toString());
        }

        return (Hep3Matrix) VecOp.mult(detToStripMatrix, VecOp.inverse(detToTrackMatrix));
    }

    private ITransform3D GetGlobalToLocal(HelicalTrackStrip strip) {
        RawTrackerHit rth = (RawTrackerHit) strip.rawhits().get(0);
        IDetectorElement ide = rth.getDetectorElement();
        SiSensor sensor = ide.findDescendants(SiSensor.class).get(0);
        SiSensorElectrodes electrodes = sensor.getReadoutElectrodes(ChargeCarrier.HOLE);
        return electrodes.getGlobalToLocal();
    }

    private int GetIdentifier(HelicalTrackStrip strip) {
        RawTrackerHit rth = (RawTrackerHit) strip.rawhits().get(0);
        IDetectorElement ide = rth.getDetectorElement();
        SiSensor sensor = ide.findDescendants(SiSensor.class).get(0);
        //       return rth.getIdentifierFieldValue(sensor.getName());
        return sensor.getSensorID();  //individual sensor positions
//        int sid=sensor.getSensorID();
//        int global=1;
//        if(sid>10)global=2;
//        return global;  //return top/bottom plates
    }

    private int GetIdentifierModule(HelicalTrackStrip strip) {
        RawTrackerHit rth = (RawTrackerHit) strip.rawhits().get(0);
        IDetectorElement ide = rth.getDetectorElement();
        SiSensor sensor = ide.findDescendants(SiSensor.class).get(0);
        //       return rth.getIdentifierFieldValue(sensor.getName());
//        return sensor.getSensorID();  //individual sensor positions
        int sid = sensor.getSensorID();
        int gid = -1;
        switch (sid) {
            case 1:
                gid = 1; break;
            case 2:
                gid = 1;break;
            case 3:
                gid = 2;break;
            case 4:
                gid = 2;break;
            case 5:
                gid = 3;break;
            case 6:
                gid = 3;break;
            case 7:
                gid = 4;break;
            case 8:
                gid = 4;break;
            case 9:
                gid = 5;break;
            case 10:
                gid = 5;break;
            case 11:
                gid = 11;break;
            case 12:
                gid = 11;break;
            case 13:
                gid = 12;break;
            case 14:
                gid = 12;break;
            case 15:
                gid = 13;break;
            case 16:
                gid = 13;break;
            case 17:
                gid = 14;break;
            case 18:
                gid = 14;break;
            case 19:
                gid = 15;break;
            case 20:
                gid = 15;break;
        }

        return gid;  //return top/bottom plates
    }

    private BasicMatrix FillMatrix(double[][] array, int nrow, int ncol) {
        BasicMatrix retMat = new BasicMatrix(nrow, ncol);
        for (int i = 0; i < nrow; i++) {
            for (int j = 0; j < ncol; j++) {
                retMat.setElement(i, j, array[i][j]);
            }
        }
        return retMat;
    }

    public void closeFile() throws IOException {
        pWriter.close();
        fWriter.close();
    }

    private double dsdR(double R, double d0, double phi0, double xint) {
        double sqrtTerm = Sqrt(R * R - Math.pow(((d0 - R) * Sin(phi0) + xint), 2));

        double rsign = Math.signum(R);
        double dsdr = (1 / sqrtTerm) * ((-rsign * xint) + (-rsign) * d0 * Sin(phi0)
                + ArcTan(R * Cos(phi0), (-R) * Sin(phi0))
                * sqrtTerm
                - ArcTan(rsign * sqrtTerm, xint + (d0 - R) * Sin(phi0))
                * sqrtTerm);


        if (_DEBUG)
            System.out.println("xint = " + xint + "; dsdr = " + dsdr);
        return dsdr;

    }

    private double dsdphi(double R, double d0, double phi0, double xint) {
        double sqrtTerm = Sqrt(R * R - Math.pow(((d0 - R) * Sin(phi0) + xint), 2));
        double rsign = Math.signum(R);
        double dsdphi = R * (sqrtTerm + rsign * d0 * Cos(phi0) - rsign * R * Cos(phi0)) / sqrtTerm;
        if (_DEBUG)
            System.out.println("xint = " + xint + "; dsdphi = " + dsdphi);
        return dsdphi;
    }

    private double dsdd0(double R, double d0, double phi0, double xint) {
        double sqrtTerm = Sqrt(R * R - Math.pow(((d0 - R) * Sin(phi0) + xint), 2));
        double rsign = Math.signum(R);
        double dsdd0 = rsign * (R * Sin(phi0)) / sqrtTerm;
        if (_DEBUG)
            System.out.println("xint = " + xint + "; dsdd0 = " + dsdd0);
        return dsdd0;
    }

    private double[] dydq(double R, double d0, double phi0, double xint, double s) {
        double[] dy = new double[5];
//        dy[0] = Cos(phi0) + Cot(phi0 - s / R) * Csc(phi0 - s / R) * dsdd0(R, d0, phi0, xint);
        dy[0] = Cos(phi0) - Sec(phi0 - s / R) * Tan(phi0 - s / R) * dsdd0(R, d0, phi0, xint);
        dy[1] = 0;
        dy[2] = 0;
//        dy[3] = (-(d0 - R)) * Sin(phi0) - R * Cot(phi0 - s / R) * Csc(phi0 - s / R) * (1 - dsdphi(R, d0, phi0, xint) / R);
        dy[3] = (-(d0 - R)) * Sin(phi0) + Sec(phi0 - s / R) * Tan(phi0 - s / R) * (R - dsdphi(R, d0, phi0, xint));
        //        dy[4] = -Cos(phi0) + Csc(phi0 - s / R) - R * Cot(phi0 - s / R) * Csc(phi0 - s / R) * (s / (R * R) - dsdR(R, d0, phi0, xint) / R);
        dy[4] = -Cos(phi0) + Sec(phi0 - s / R) + (1 / R) * Sec(phi0 - s / R) * Tan(phi0 - s / R) * (s - R * dsdR(R, d0, phi0, xint));
        return dy;
    }

    private double[] dzdq(double R, double d0, double phi0, double xint, double slope, double s) {
        double[] dz = new double[5];
        dz[0] = slope * dsdd0(R, d0, phi0, xint);
        dz[1] = 1;
        dz[2] = s;
        dz[3] = slope * dsdphi(R, d0, phi0, xint);
        dz[4] = slope * dsdR(R, d0, phi0, xint);
        return dz;
    }

    private double Csc(double val) {
        return 1 / Math.sin(val);
    }

    private double Cot(double val) {
        return 1 / Math.tan(val);
    }

    private double Sec(double val) {
        return 1 / Math.cos(val);
    }

    private double Sin(double val) {
        return Math.sin(val);
    }

    private double Cos(double val) {
        return Math.cos(val);
    }

    private double Tan(double val) {
        return Math.tan(val);
    }

    private double ArcTan(double val1, double val2) {
        return Math.atan2(val1, val2);
    }

    private double Sign(double val) {
        return Math.signum(val);
    }

    private double Sqrt(double val) {
        return Math.sqrt(val);
    }

    private void printDerivatives(double[] trackpars, double[][] dfdq) {
        System.out.println("======================================================");
        System.out.println("s         xint");
        System.out.printf("%5.5f %5.5f\n", trackpars[5], trackpars[6]);
        System.out.println("             d0           z0           slope         phi0          R");
        System.out.printf("Values       %5.5f    %5.5f     %5.5f      %5.5f      %5.5f\n", trackpars[0], trackpars[1], trackpars[2], trackpars[3], trackpars[4]);
        System.out.printf("dzdq         ");
        for (int i = 0; i < 5; i++) {
            System.out.printf("%5.3e   ", dfdq[2][i]);
        }
        System.out.println();
        System.out.printf("dudq         ");
        for (int i = 0; i < _nlc; i++) {
            System.out.printf("%5.3e   ", _dfdq.e(0, i));
        }
        System.out.println();
        System.out.println();
        System.out.printf("dydq         ");
        for (int i = 0; i < 5; i++) {
            System.out.printf("%5.3e   ", dfdq[1][i]);
        }
        System.out.println();
        System.out.printf("dvdq         ");
        for (int i = 0; i < _nlc; i++) {
            System.out.printf("%5.3e   ", _dfdq.e(1, i));
        }
        System.out.println();
        System.out.println();
        System.out.printf("dxdq         ");
        for (int i = 0; i < 5; i++) {
            System.out.printf("%5.3e   ", dfdq[0][i]);
        }
        System.out.println();
        System.out.printf("dwdq         ");
        for (int i = 0; i < _nlc; i++) {
            System.out.printf("%5.3e   ", _dfdq.e(2, i));
        }
        System.out.println();
        //        System.out.println(        _trk.xc()+ "; "+_trk.yc());
//          System.out.println(        _trk.x0()+ "; "+_trk.y0());
    }
}
