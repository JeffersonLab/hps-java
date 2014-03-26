package org.hps.recon.vertexing;

/**
 * @version $Id: BFitter.java,v 1.1 2011/06/01 17:10:13 jeremy Exp $
 * @version Billior Fitter used in the HPS Java package. Meant to simulate
 * the multiple scatters of the detector
 */

// Performs a Kalman fit to a list of tracks and returns
// a Vertex object
import Jama.util.Maths;
import static java.lang.Math.sin;
import static java.lang.Math.cos;
import static java.lang.Math.tan;
import static java.lang.Math.atan;
import static java.lang.Math.atan2;
import static java.lang.Math.sqrt;
import static java.lang.Math.PI;
import java.util.List;
import org.lcsim.event.Track;
import org.lcsim.recon.vertexing.billoir.Vertex;
import org.lcsim.recon.vertexing.billoir.Perigee;


import Jama.Matrix;
import org.lcsim.spacegeom.SpacePoint;


public class BFitter implements VFitter {
    // the value of the magnetic field in the vicinity of the vertex
    // default is a constant field along the z axis
    private double _bField;

    // constructor
    public BFitter(double bField) {
        _bField = bField;
    }

    // Function copied from trf to avoid unnecessary dependency on that package.  --JM
    public static double fmod1( double value, double range )
    {
        double tmp = value%range;
        if ( tmp < 0.0 ) return tmp + Math.abs(range);
        return tmp;
    }


    // fitter
    private Vertex fit(int ntrk, boolean fitwb, boolean[] invtx, double[][] par, double[][] wgt, double[] xyz) {
        return pxfvtx(ntrk, fitwb, invtx, par, wgt, xyz);
    }


    public Vertex fit(List<Track> tracks, SpacePoint initialPosition, boolean withBeamConstraint) {
        int ntrk = tracks.size();
        boolean[] isInVtx = new boolean[ntrk];
        for (int i=0; i<ntrk; i++) {
            isInVtx[i] = true;
        }
        double[][] parameters = new double[5][ntrk];
        double[][] errors = new double[15][ntrk];
        for (Track iTrack : tracks) {
            double[] iOldParams = iTrack.getTrackParameters();
            Matrix jacobi = new Matrix(getJacobi(iOldParams));
            Matrix olderrors = Maths.toJamaMatrix(iTrack.getErrorMatrix());

            double theta = PI/2 - atan(iOldParams[4]);
//            double[] newparams = new double[]{iOldParams[0], iOldParams[3], theta, iOldParams[1], iOldParams[2]};
            double[] newparams = new double[]{-iOldParams[0], iOldParams[3], theta, iOldParams[1], iOldParams[2]};

            double[] iErrors = flattenMatrix(jacobi.times(olderrors).times(jacobi.transpose()).getArray());
            int iTrackIndex = tracks.indexOf(iTrack);
//            System.out.println("Track # "+iTrackIndex);
//            System.out.println(olderrors.toString());
            for (int i=0; i<iErrors.length; ++i) {
                errors[i][iTrackIndex] = iErrors[i];
 //               System.out.println("error "+i + " = " + errors[i][iTrackIndex]);
            }
            for (int i=0; i<newparams.length; ++i) {
                parameters[i][iTrackIndex] = newparams[i];
 //                System.out.println("parameter "+i + " = " + parameters[i][iTrackIndex]);
            }
        }
        return pxfvtx(ntrk, withBeamConstraint, isInVtx, parameters, errors, initialPosition.getCartesianArray());
    }
    /**
     * Conversion matrix from org.lcsim track parameters (old) to internal parameters
     */
    private double[][] getJacobi(double[] old) {
        double[][] jacobi = new double[][]{
//               {-old[0], 0, 0, 0, 0}
//               , {0, 0, 0, old[1], 0}
//               , {0, 0, 0, 0, old[2]}
//               , {0, old[3], 0, 0, 0}
//               , {0, 0, -1/(1+old[4]*old[4]), 0, 0}
//                 {-1, 0, 0, 0, 0}
//               , {0, 0, 0, 1, 0}
//               , {0, 0, 0, 0, -1/(1+old[4]*old[4])}
//               , {0, 1, 0, 0, 0}
//               , {0, 0, 1, 0, 0}
               {-1, 0, 0, 0, 0}
               , {0, 0, 0, 1, 0}
               , {0, 0, 0, 0, 1}
               , {0, 1, 0, 0, 0}
               , {0, 0, -1/(1+old[4]*old[4]), 0, 0}
        };


        return jacobi;
    }

    private double[] flattenMatrix(double[][] matrix) {
        int length = matrix.length;
        double[] result = new double[length*(length+1)/2];
        int count = 0;
        for (int i=0; i<length; ++i)
            for (int j=0; j<=i; ++j)
                result[count++] = matrix[i][j];
        return result;
    }

    private double[] pxmi5(double[] wgt) {

        // ***********************************************************************
        // * *
        // * *
        // * inversion of a (5x5) symmetric positive matrix (cholesky method) *
        // * internal computation in double precision *
        // * check on positivity (ierr=2 if wgt not positive) *
        // * *
        // ***********************************************************************
        // *
        double[] cov = new double[15];
        double t11, t12, t13, t14, t15;
        double t22, t23, t24, t25;
        double t33, t34, t35;
        double t44, t45;
        double t55;
        double s12, s13, s14, s15;
        double s23, s24, s25;
        double s34, s35;
        double s45;
        if (wgt[0] < 0.)
            throw new IllegalArgumentException("Bad weight matrix!");

        t11 = 1. / sqrt(wgt[0]);
        s12 = wgt[1] * t11;
        s13 = wgt[3] * t11;
        s14 = wgt[6] * t11;
        s15 = wgt[10] * t11;
        //
        t22 = wgt[2] - s12 * s12;
        if (t22 < 0.)
            throw new IllegalArgumentException("Bad weight matrix!");

        t22 = 1. / sqrt(t22);
        s23 = (wgt[4] - s12 * s13) * t22;
        s24 = (wgt[7] - s12 * s14) * t22;
        s25 = (wgt[11] - s12 * s15) * t22;
        //
        t33 = wgt[5] - s13 * s13 - s23 * s23;
        if (t33 < 0.)
            throw new IllegalArgumentException("Bad weight matrix!");

        t33 = 1. / sqrt(t33);
        s34 = (wgt[8] - s13 * s14 - s23 * s24) * t33;
        s35 = (wgt[12] - s13 * s15 - s23 * s25) * t33;
        //
        t44 = wgt[9] - s14 * s14 - s24 * s24 - s34 * s34;
        if (t44 < 0.)
            throw new IllegalArgumentException("Bad weight matrix!");

        t44 = 1. / sqrt(t44);
        s45 = (wgt[13] - s14 * s15 - s24 * s25 - s34 * s35) * t44;
        //
        t55 = wgt[14] - s15 * s15 - s25 * s25 - s35 * s35 - s45 * s45;
        if (t55 < 0.)
            throw new IllegalArgumentException("Bad weight matrix!");

        t55 = 1. / sqrt(t55);
        //
        t45 = -t44 * (s45 * t55);
        t34 = -t33 * (s34 * t44);
        t35 = -t33 * (s34 * t45 + s35 * t55);
        t23 = -t22 * (s23 * t33);
        t24 = -t22 * (s23 * t34 + s24 * t44);
        t25 = -t22 * (s23 * t35 + s24 * t45 + s25 * t55);
        t12 = -t11 * (s12 * t22);
        t13 = -t11 * (s12 * t23 + s13 * t33);
        t14 = -t11 * (s12 * t24 + s13 * t34 + s14 * t44);
        t15 = -t11 * (s12 * t25 + s13 * t35 + s14 * t45 + s15 * t55);
        //
        cov[0] = t11 * t11 + t12 * t12 + t13 * t13 + t14 * t14 + t15 * t15;
        cov[1] = t12 * t22 + t13 * t23 + t14 * t24 + t15 * t25;
        cov[2] = t22 * t22 + t23 * t23 + t24 * t24 + t25 * t25;
        cov[3] = t13 * t33 + t14 * t34 + t15 * t35;
        cov[4] = t23 * t33 + t24 * t34 + t25 * t35;
        cov[5] = t33 * t33 + t34 * t34 + t35 * t35;
        cov[6] = t14 * t44 + t15 * t45;
        cov[7] = t24 * t44 + t25 * t45;
        cov[8] = t34 * t44 + t35 * t45;
        cov[9] = t44 * t44 + t45 * t45;
        cov[10] = t15 * t55;
        cov[11] = t25 * t55;
        cov[12] = t35 * t55;
        cov[13] = t45 * t55;
        cov[14] = t55 * t55;
        return cov;
    }

    private double fwdch2(double[] wgt, double d1, double d2, double d3, double d4, double d5) {
        // ***********************************************************************
        // * *
        // * computation of dt*wgt*d *
        // * wgt is a (5x5) symmetric matrix ; d is a 5-vector *
        // * *
        // ***********************************************************************
        // *
        return wgt[0]
                * d1
                * d1
                + wgt[2]
                * d2
                * d2
                + wgt[5]
                * d3
                * d3
                + wgt[9]
                * d4
                * d4
                + wgt[14]
                * d5
                * d5
                + 2
                * (d2 * d1 * wgt[1] + d3 * (d1 * wgt[3] + d2 * wgt[4]) + d4 * (d1 * wgt[6] + d2 * wgt[7] + d3 * wgt[8]) + d5
                        * (d1 * wgt[10] + d2 * wgt[11] + d3 * wgt[12] + d4 * wgt[13]));
    }

    // ************************************************************************
    // * *
    public Perigee perigee(double[] rawdat, double xb, double yb) {
        // * *
        // ************************************************************************
        // name : perigee
        //
        // created : 30-jun-1998 author : norman a. graf
        // based on pxfxpe 7-jun-1989 author : p. billoir (cdf)
        //
        // function : compute from input data (with cylindrical or cartesian
        // coordinates)
        // the "perigee" parameters (point of closest approach to the
        // reference axis) and the covariance matrix on them
        // for error propagation, an approximation is made, assuming
        // that the r coordinate of reference point is small w.r.t.
        // the radius of curvature
        // no account is taken for multiple scattering, so that the
        // error matrix is underestimated if the data reference point
        // is outside the beam pipe (except at high momentum)
        //
        // references : none
        //
        // arguments : input : dat(1:21) : tix array
        // dat(1:3) : coord. of a point on the track
        // if "cylindrical" coord. : r,r*phi,z
        // if "cartesian" coord. : x,y,z
        // dat(4:5) : theta, phi of the tangent at this point
        // dat[5] : 1/p with geometrical sign
        // dat(7:21) : covariance matrix on 5 "free" param. :
        // rphi,z,theta,phi,1/p if "cylind." coord. (r fixed)
        // x,y,theta,phi,1/p if "cartes." coord. (z fixed)
        //
        // xb,yb : coordinates of the reference axis
        //
        // output : par(1:5) : "perigee" parameters
        // 1 : epsilon (impact par. in xy projection, with sign)
        // 2 : z coordinate
        // 3 : theta angle
        // 4 : phi angle
        // 5 : 1/r (r = radius of curvature, with sign)
        // cov(1:15) : covariance matrix of par
        // wgt(1:15) : weight matrix of par (inverse of cov)
        //
        // errors : ierr = 1 : incorrect data in tkr array
        // (for example not cylindrical coordinates)
        // 2 : cov not positive (wgt not computed)
        //
        // ************************************************************************
        //
        //
        // conversion from d0 cylindrical coordinates to mine...
        //
        double sthet, sthet2, cthet, gamma;
        double d0der11, d0der43, d0der45, d0der55;
        // double[] rawdat = new double [21];
        //
        double cosf, sinf, r0, rcosb, d11, d12, d21, d22, cov1, cov3, cov4, cov7, cov11;
        // to define the magnetic field - values to be provided :
        // FIXME this needs to be replaced by the official org.lcsim constant
        double consa = 0.0003;
        double bmag = _bField;
        double consb = consa * bmag;

        // consa = velocity of light (here in GeV/(mm.T) ; depends on units)
        // bmag = magnetic field along z axis
        //
        //
        double[] par = new double[5];
        double[] cov = new double[15];
        double[] wgt = new double[15];
        double sgn;
        double[] dat = new double[21];
        //
        // local variables
        // __________________
        //
        int ierr, ii;
        double capphi, cotth, dphi, rdphi, rtrk, xc, x0, yc, y0;
        double der1, der2, der11, der14, der15, der23, der45;
        //
        //
        // executable statements
        // ________________________
        //
        double twopi = 2. * PI;
        double halfpi = PI / 2.;

        int icypl = 1;
        // icypl = 1 if "cylindrical" coordinates, 0 if "plane" coordinates
        //
        // convert here...
        //
        //
        // ::: parameters r, rphi, z, theta, phi, 1/p
        //
        dat[0] = rawdat[0]; // r
        dat[1] = rawdat[0] * rawdat[1]; // r*phi
        dat[2] = rawdat[2]; // z
        dat[3] = atan(1. / rawdat[4]); // theta
        if (rawdat[4] < 0)
            dat[3] = dat[3] + PI;
        dat[4] = rawdat[1] + rawdat[3]; // phi (recall beta= phi(dir)-phi(pos) )
        dat[5] = -rawdat[5] * sin(dat[3]); // 1/p (recall 1/pt = 1/(p*sin(theta))
        //
        // ::: covariance matrix
        // ::: there are seven terms in the conversion from
        //
        // phi rphi
        // z z
        // alfa to theta
        // cot(theta) phi
        // q/pt 1/p
        //
        // d0der14 = 1
        // d0der22 = 1
        // d0der34 = -1
        //
        sthet = sin(dat[3]);
        sthet2 = sthet * sthet;
        cthet = cos(dat[3]);
        gamma = -rawdat[5] * cthet * sthet2;

        d0der11 = rawdat[0];
        d0der43 = -sthet2;
        d0der45 = -rawdat[5] * cthet * sthet2;
        d0der55 = sthet;
        //
        dat[6] = rawdat[6] * rawdat[0] * rawdat[0];
        dat[7] = rawdat[7] * rawdat[0];
        dat[8] = rawdat[8];
        dat[9] = -rawdat[12] * rawdat[0] * sthet2;
        dat[10] = -rawdat[13] * sthet2;
        dat[11] = rawdat[15] * sthet2 * sthet2;
        dat[12] = (rawdat[6] - rawdat[9]) * rawdat[0];
        dat[13] = rawdat[7] - rawdat[10];
        dat[14] = (rawdat[14] - rawdat[12]) * sthet2;
        dat[15] = rawdat[6] - 2 * rawdat[9] + rawdat[11];
        dat[16] = - (rawdat[16] * sthet + rawdat[12] * gamma) * rawdat[0];
        dat[17] = -rawdat[17] * sthet - rawdat[13] * gamma;
        dat[18] = (rawdat[19] * sthet + rawdat[15] * gamma) * sthet2;
        dat[19] = (rawdat[14] - rawdat[12]) * gamma + (rawdat[18] - rawdat[16]) * sthet;
        dat[20] = (rawdat[15] * gamma + 2 * rawdat[19] * sthet) * gamma + rawdat[20] * sthet2;

        //
        // icypl = 1 if "cylindrical" coordinates, 0 if "plane" coordinates
        //
        if ( (icypl == 1 && dat[0] == 0.) || dat[3] == 0. || dat[5] == 0.) {
            System.err.println("******error in perigee*******");
            throw new IllegalArgumentException("Choke!");
        }

        //
        // now computation of "perigee" parameters
        //
        capphi = dat[1] / dat[0];
        if (icypl == 1) {
            x0 = dat[0] * cos(capphi) - xb;
            y0 = dat[0] * sin(capphi) - yb;
        } else {
            x0 = dat[0] - xb;
            y0 = dat[1] - yb;
        }
        rtrk = sin(dat[3]) / (consb * dat[5]);
        cosf = cos(dat[4]);
        sinf = sin(dat[4]);
        xc = x0 - rtrk * sinf;
        yc = y0 + rtrk * cosf;
        // epsilon (impact parameter in xy projection, with geometrical sign)
        sgn = 1.;
        if (rtrk < 0.)
            sgn = -1.;
        par[0] = rtrk - sgn * sqrt(xc * xc + yc * yc);
        // phi at perigee (range 0,2*PI)
        par[3] = atan2(yc, xc) + PI + sgn * halfpi;
        if (par[3] < 0.)
            par[3] = par[3] + twopi;
        else if (par[3] > twopi)
            par[3] = par[3] - twopi;
        // variation of phi from reference point of tkr to perigee (range -pi,+pi)
        dphi = fmod1(par[3] - dat[4] + twopi + PI, twopi) - PI;
        rdphi = rtrk * dphi;
        cotth = 1. / tan(dat[3]);
        // z of perigee
        par[1] = dat[2] + cotth * rdphi;
        // theta and 1/r at perigee (unchanged)
        par[2] = dat[3];
        par[4] = 1. / rtrk;
        //
        // computation of covariance matrix
        //
        for (ii = 0; ii < 15; ++ii) {
            cov[ii] = dat[6 + ii];
        }
        //
        // transformation from 1/p to 1/r = consb * (1/p) / sin(theta)
        //
        der1 = -cotth * par[4];
        der2 = par[4] / dat[5];
        cov[14] = der1 * der1 * cov[5] + 2 * der1 * der2 * cov[12] + der2 * der2 * cov[14];
        cov[10] = der1 * cov[3] + der2 * cov[10];
        cov[11] = der1 * cov[4] + der2 * cov[11];
        cov[12] = der1 * cov[5] + der2 * cov[12];
        cov[13] = der1 * cov[8] + der2 * cov[13];
        //
        // if cartesian coordinates transformation from (x,y) to (r*phi,z)
        // (neglecting curvature effects )
        //
        if (icypl == 0) {
            r0 = sqrt(dat[0] * dat[0] + dat[1] * dat[1]);
            rcosb = dat[0] * cosf + dat[1] * sinf;
            d11 = -r0 * sinf / rcosb;
            d12 = r0 * cosf / rcosb;
            d21 = -dat[0] * cotth / rcosb;
            d22 = -dat[1] * cotth / rcosb;
            cov1 = d11 * d11 * cov[0] + 2 * d11 * d12 * cov[1] + d12 * d12 * cov[2];
            cov3 = d21 * d21 * cov[0] + 2 * d21 * d22 * cov[1] + d22 * d22 * cov[2];
            cov[1] = d11 * d21 * cov[0] + (d12 * d21 + d11 * d22) * cov[1] + d12 * d22 * cov[2];
            cov[0] = cov1;
            cov[2] = cov3;
            cov4 = d11 * cov[3] + d12 * cov[4];
            cov[4] = d21 * cov[3] + d22 * cov[4];
            cov[3] = cov4;
            cov7 = d11 * cov[6] + d12 * cov[7];
            cov[7] = d21 * cov[6] + d22 * cov[7];
            cov[6] = cov7;
            cov11 = d11 * cov[10] + d12 * cov[11];
            cov[11] = d21 * cov[10] + d22 * cov[11];
            cov[10] = cov11;
        }
        //
        // transformation from (r*phi,z0,theta,phi0,1/r)
        // to (epsilon,zp,theta,phip,1/r)
        //
        // approximation for derivatives d(epsilon)/d(r*phi), d(epsilon)/d(phi0),
        // d(epsilon)/d(1/r), d(zp)/d(z0) and d(phip)/d(1/r)
        // the other ones are exactly or approximately 1 (diagonal terms)
        // or 0 (non-diagonal terms)
        //
        der11 = rdphi / sqrt(x0 * x0 + y0 * y0);
        der14 = -rdphi;
        der15 = -rdphi * rdphi / 2.;
        der23 = - (1. + cotth * cotth) * rdphi;
        der45 = rdphi;
        //
        cov[0] = der11
                * der11
                * cov[0]
                + 2
                * der11
                * (der14 * cov[6] + der15 * cov[10])
                + der14
                * (der14 * cov[9] + 2 * der15 * cov[13])
                + der15
                * der15
                * cov[14];
        cov[1] = der11
                * (cov[1] + der23 * cov[3])
                + der14
                * (cov[7] + der23 * cov[8])
                + der15
                * (cov[11] + der23 * cov[12]);
        cov[2] = cov[2] + 2 * der23 * cov[4] + der23 * der23 * cov[5];
        cov[3] = der11 * cov[3] + der14 * cov[8] + der15 * cov[12];
        cov[4] = cov[4] + der23 * cov[5];
        cov[6] = der11
                * (cov[6] + der45 * cov[10])
                + der14
                * (cov[9] + der45 * cov[13])
                + der15
                * (cov[13] + der45 * cov[14]);
        cov[7] = cov[7] + der23 * cov[8] + der45 * (cov[11] + der23 * cov[12]);
        cov[8] = cov[8] + der45 * cov[12];
        cov[9] = cov[9] + 2 * der45 * cov[13] + der45 * der45 * cov[14];
        cov[10] = der11 * cov[10] + der14 * cov[13] + der15 * cov[14];
        cov[11] = cov[11] + der23 * cov[12];
        cov[13] = cov[13] + der45 * cov[14];
        //
        // invert matrix cov to get wgt
        //
        wgt = pxmi5(cov);
        //
        return new Perigee(par, cov, wgt);

    }

    private double[] pxmi3(double[] wgt) {
        double[][] x = new double[3][3];
        x[0][0] = wgt[0];
        x[0][1] = wgt[1];
        x[1][0] = wgt[1];
        x[0][2] = wgt[2];
        x[2][0] = wgt[2];
        x[1][1] = wgt[3];
        x[1][2] = wgt[4];
        x[2][1] = wgt[4];
        x[2][2] = wgt[5];
        Matrix xMat = new Matrix(x);
        double[][] y = xMat.inverse().getArray();
        return new double[] {y[0][0], y[0][1], y[0][2], y[1][1], y[1][2], y[2][2]};
    }

    private double[] pxmi3_old(double[] wgt) {
        // ***********************************************************************
        // * *
        // * *
        // * inversion of a (3x3) symmetric positive matrix (cholesky method) *
        // * internal computation in double precision *
        // * check on positivity (ierr=2 if wgt not positive) *
        // * cov (output) may overwrite wgt (input) *
        // * *
        // ***********************************************************************
        // *

        double[] cov = new double[6];
        double t11, t12, t13;
        double t21, t22, t23;
        double t33;
        double s12, s13;
        double s23;

        if (wgt[0] < 0.)
            throw new IllegalArgumentException("Bad weight matrix!");

        t11 = 1. / sqrt(wgt[0]);
        s12 = wgt[1] * t11;
        s13 = wgt[3] * t11;
        //
        t22 = wgt[2] - s12 * s12;
        if (t22 < 0.)
            throw new IllegalArgumentException("Bad weight matrix!");

        t22 = 1. / sqrt(t22);
        s23 = (wgt[4] - s12 * s13) * t22;
        //
        t33 = wgt[5] - s13 * s13 - s23 * s23;
        if (t33 < 0.)
            throw new IllegalArgumentException("Bad weight matrix!");

        t33 = 1. / sqrt(t33);
        //
        t23 = -t22 * (s23 * t33);
        t12 = -t11 * (s12 * t22);
        t13 = -t11 * (s12 * t23 + s13 * t33);
        //
        cov[0] = t11 * t11 + t12 * t12 + t13 * t13;
        cov[1] = t12 * t22 + t13 * t23;
        cov[2] = t22 * t22 + t23 * t23;
        cov[3] = t13 * t33;
        cov[4] = t23 * t33;
        cov[5] = t33 * t33;
        return cov;
    }

    // ************************************************************************
    // * *
    public Vertex pxfvtx(int ntrk, boolean fitwb, boolean[] invtx, double[][] par, double[][] wgt, double[] xyz) {
        // * *
        // ************************************************************************
        // * name : pxfvtx
        // *
        // * created : 5-jan-1989 author : p. billoir (cdf)
        // *
        // * function : perform a full vertex vertex fit with weighted mean
        // * of tracks considered as ellipsoids in 5d space of
        // * "perigee" parameters (epsilon,zp,theta,phi,1/r)
        // *
        // ******** 1-12-90 changes in pxfvtx routine : p. billoir
        // * - to allow the use of the beam spot constraint (logical var. fitwb)
        // * - to know the contribution of individual tracks to the total chi2.
        // *
        // * references : none
        // *
        // * arguments : input : ntrk : number of tracks
        // * fitwb = .true. if we want the beam spot
        // * constraint
        // * invtx(i)=.true. to use the track i in the
        // * vertex fit
        // * par(1:5,1:ntrk) : "perigee" parameters
        // * 1 : epsilon (impact par. in xy projection, with sign)
        // * 2 : z coordinate
        // * 3 : theta angle
        // * 4 : phi angle
        // * 5 : 1/r (r = radius of curvature, with sign)
        // * wgt(1:15,1:ntrk) : weight matrix on par
        // * xyz(1:3) : approximate coordinates of the vertex
        // *
        // * output : xyzf(1:3) : fitted coordinates of the vertex
        // * parf(1:3,1:ntrk) : fitted parameters at the vertex
        // * 1 : theta
        // * 2 : phi
        // * 3 : 1/r
        // * vcov(1:6) : covariance matrix on xyz(vertex)
        // * tcov(1:6,1:ntrk) : covariance matrix on parf
        // * chi2 : chi2 of the fit
        // * chi2tr(i)=contribution of track i to the
        // * total chi2
        // *
        // * errors : ierr = 1 : too many tracks (ntrk > ntrmax)
        // * ierr = 2 : covariance or weight matrix not positive
        // *
        // *************************************************************************
        // *
        // *
        // * "beam spot" description (common to be provided by the user)
        // * beamoy(1-3) are the mean beam spot positions in x,y and z.
        // * sigxbe,sigybe,sigzbe are the beam spread in x,y,z
        // common/pxcpro/beamoy[2],sigxbe,sigybe,sigzbe
        // double beamoy,sigxbe,sigybe,sigzbe
        // *
        // formal parameters
        // *____________________
        // *
        // int i,j,ierr,ntrk,ntrmax
        // *
        int ntrmax = 1000;

        double[] xyzf = new double[3];
        double[][] parf = new double[3][ntrk];
        double[] vcov = new double[6];
        double[][] tcov = new double[6][ntrk];
        double chi2;

        // *
        // * local variables
        // *__________________
        // *
        double[] wa = new double[6];
        double[][] wb = new double[9][ntrk];
        double[][] wc = new double[6][ntrk];
        double[][] wci = new double[6][ntrk];
        double[][] wbci = new double[9][ntrk];
        double[] tv = new double[3];
        double[][] tt = new double[3][ntrk];
        double[] dxyz = new double[3];
        double[] phiv = new double[ntrk];
        double[] eps = new double[ntrk];
        double[] zp = new double[ntrk];
        double[] deps = new double[ntrk];
        double[] dzp = new double[ntrk];
        double cotth, cosf, sinf, uu, vv, d11, d12, d21, d22, d41, d42, e12, e13, e21, e22, e23, e43, dw11, dw12, dw13, dw14, dw15, dw21, dw22, dw23, dw24, dw25, dw31, dw32, dw33, dw34, dw35, ew11, ew12, ew13, ew14, ew15, ew21, ew22, ew23, ew24, ew25, ew31, ew32, ew33, ew34, ew35, chi2i, epsf, zpf, phif;

        double fwdch2;
        double[] chi2tr = new double[ntrk];

        // needs to be set
        double sigxbe = 100.0;
        double sigybe = .01;
        double sigzbe = .01;
        double beamoy[] = { 0., 0., 0. };
        //
        // *
        // * executable statements
        // *________________________
        // *

        // *
        // * loop over the tracks
        // *
        chi2i = 0.;
        for (int i = 0; i < ntrk; ++i) {
            if (invtx[i]) {
                // *
                // * starting conditions :
                // * "perigee" parameters eps and zp if the trajectory goes through xyz
                // * and its theta,phi,1/r at perigee are equal to the values at input
                // *
                cotth = 1. / tan(par[2][i]);
                uu = xyz[0] * cos(par[3][i]) + xyz[1] * sin(par[3][i]);
                vv = xyz[1] * cos(par[3][i]) - xyz[0] * sin(par[3][i]);
                eps[i] = -vv + .5 * uu * uu * par[4][i];
                zp[i] = xyz[2] - uu * cotth;
                // * phi at vertex with these parameters
                phiv[i] = par[3][i] + uu * par[4][i];
                cosf = cos(phiv[i]);
                sinf = sin(phiv[i]);
                // *
                // * contribution of this track to chi2 with initial values
                // *
                deps[i] = par[0][i] - eps[i];
                dzp[i] = par[1][i] - zp[i];
                chi2i = chi2i
                        + wgt[0][i]
                        * deps[i]
                        * deps[i]
                        + 2
                        * wgt[1][i]
                        * deps[i]
                        * dzp[i]
                        + wgt[2][i]
                        * dzp[i]
                        * dzp[i];
                // *
                // * derivatives (deriv1) of perigee param. w.r.t. x,y,z (vertex)
                d11 = sinf;
                d12 = -cosf;
                d21 = -cosf * cotth;
                d22 = -sinf * cotth;
                d41 = -cosf * par[4][i];
                d42 = -sinf * par[4][i];
                // *
                // * matrix dw = (deriv1)t * weight
                dw11 = d11 * wgt[0][i] + d21 * wgt[1][i] + d41 * wgt[6][i];
                dw12 = d11 * wgt[1][i] + d21 * wgt[2][i] + d41 * wgt[7][i];
                dw13 = d11 * wgt[3][i] + d21 * wgt[4][i] + d41 * wgt[8][i];
                dw14 = d11 * wgt[6][i] + d21 * wgt[7][i] + d41 * wgt[9][i];
                dw15 = d11 * wgt[10][i] + d21 * wgt[11][i] + d41 * wgt[13][i];
                dw21 = d12 * wgt[0][i] + d22 * wgt[1][i] + d42 * wgt[6][i];
                dw22 = d12 * wgt[1][i] + d22 * wgt[2][i] + d42 * wgt[7][i];
                dw23 = d12 * wgt[3][i] + d22 * wgt[4][i] + d42 * wgt[8][i];
                dw24 = d12 * wgt[6][i] + d22 * wgt[7][i] + d42 * wgt[9][i];
                dw25 = d12 * wgt[10][i] + d22 * wgt[11][i] + d42 * wgt[13][i];
                dw31 = wgt[1][i];
                dw32 = wgt[2][i];
                dw33 = wgt[4][i];
                dw34 = wgt[7][i];
                dw35 = wgt[11][i];
                // *
                // * summation of dw * dpar to vector tv
                tv[0] = tv[0] + dw11 * deps[i] + dw12 * dzp[i];
                tv[1] = tv[1] + dw21 * deps[i] + dw22 * dzp[i];
                tv[2] = tv[2] + dw31 * deps[i] + dw32 * dzp[i];
                // *
                // * derivatives (deriv2) of perigee param. w.r.t. theta,phi,1/r (vertex)
                e12 = uu;
                e13 = -.5 * uu * uu;
                //e21 = -uu * (1. + cotth * cotth);//I think this sign is wrong
                  e21 = uu * (1. + cotth * cotth);//I think this sign is wrong
                e22 = -vv * cotth;
                e23 = uu * vv * cotth;
                e43 = -uu;
                // *
                // * matrix ew = (deriv2)t * weight
                ew11 = e21 * wgt[1][i] + wgt[3][i];
                ew12 = e21 * wgt[2][i] + wgt[4][i];
                ew13 = e21 * wgt[4][i] + wgt[5][i];
                ew14 = e21 * wgt[7][i] + wgt[8][i];
                ew15 = e21 * wgt[11][i] + wgt[12][i];
                ew21 = e12 * wgt[0][i] + e22 * wgt[1][i] + wgt[6][i];
                ew22 = e12 * wgt[1][i] + e22 * wgt[2][i] + wgt[7][i];
                ew23 = e12 * wgt[3][i] + e22 * wgt[4][i] + wgt[8][i];
                ew24 = e12 * wgt[6][i] + e22 * wgt[7][i] + wgt[9][i];
                ew25 = e12 * wgt[10][i] + e22 * wgt[11][i] + wgt[13][i];
                ew31 = e13 * wgt[0][i] + e23 * wgt[1][i] + e43 * wgt[6][i] + wgt[10][i];
                ew32 = e13 * wgt[1][i] + e23 * wgt[2][i] + e43 * wgt[7][i] + wgt[11][i];
                ew33 = e13 * wgt[3][i] + e23 * wgt[4][i] + e43 * wgt[8][i] + wgt[12][i];
                ew34 = e13 * wgt[6][i] + e23 * wgt[7][i] + e43 * wgt[9][i] + wgt[13][i];
                ew35 = e13 * wgt[10][i] + e23 * wgt[11][i] + e43 * wgt[13][i] + wgt[14][i];
                // *
                // * computation of vector tt = ew * dpar
                tt[0][i] = ew11 * deps[i] + ew12 * dzp[i];
                tt[1][i] = ew21 * deps[i] + ew22 * dzp[i];
                tt[2][i] = ew31 * deps[i] + ew32 * dzp[i];
                // *
                // * summation of (deriv1)t * weight * (deriv1) to matrix wa
                wa[0] = wa[0] + dw11 * d11 + dw12 * d21 + dw14 * d41;
                wa[1] = wa[1] + dw11 * d12 + dw12 * d22 + dw14 * d42;
                wa[2] = wa[2] + dw21 * d12 + dw22 * d22 + dw24 * d42;
                wa[3] = wa[3] + dw12;
                wa[4] = wa[4] + dw22;
                wa[5] = wa[5] + dw32;
                // *
                // * computation of matrix wb = (deriv1)t * weight * (deriv2)
                wb[0][i] = dw12 * e21 + dw13;
                wb[1][i] = dw22 * e21 + dw23;
                wb[2][i] = dw32 * e21 + dw33;
                wb[3][i] = dw11 * e12 + dw12 * e22 + dw14;
                wb[4][i] = dw21 * e12 + dw22 * e22 + dw24;
                wb[5][i] = dw31 * e12 + dw32 * e22 + dw34;
                wb[6][i] = dw11 * e13 + dw12 * e23 + dw14 * e43 + dw15;
                wb[7][i] = dw21 * e13 + dw22 * e23 + dw24 * e43 + dw25;
                wb[8][i] = dw31 * e13 + dw32 * e23 + dw34 * e43 + dw35;
                // *
                // * computation of matrix wc = (deriv2)t * weight * (deriv2)
                wc[0][i] = ew12 * e21 + ew13;
                wc[1][i] = ew22 * e21 + ew23;
                wc[3][i] = ew32 * e21 + ew33;
                wc[2][i] = ew21 * e12 + ew22 * e22 + ew24;
                wc[4][i] = ew31 * e12 + ew32 * e22 + ew34;
                wc[5][i] = ew31 * e13 + ew32 * e23 + ew34 * e43 + ew35;
                // *
                // * computation of matrices wci = (wc)**(-1) and wbci = wb * wci

                // Not very efficient here...
                double[] tmp = new double[6];
                for (int j = 0; j < 6; ++j) {
                    tmp[j] = wc[j][i];
//                    System.out.println("tmp[" + j + "]= " + tmp[j]);
                }
                double[] tmpinv = new double[6];
                tmpinv = pxmi3(tmp);
                for (int j = 0; j < 6; ++j) {
                    wci[j][i] = tmpinv[j];
                }
                // wci[0][i] = pxmi3(wc[0][i]);

                wbci[0][i] = wb[0][i] * wci[0][i] + wb[3][i] * wci[1][i] + wb[6][i] * wci[3][i];
                wbci[1][i] = wb[1][i] * wci[0][i] + wb[4][i] * wci[1][i] + wb[7][i] * wci[3][i];
                wbci[2][i] = wb[2][i] * wci[0][i] + wb[5][i] * wci[1][i] + wb[8][i] * wci[3][i];
                wbci[3][i] = wb[0][i] * wci[1][i] + wb[3][i] * wci[2][i] + wb[6][i] * wci[4][i];
                wbci[4][i] = wb[1][i] * wci[1][i] + wb[4][i] * wci[2][i] + wb[7][i] * wci[4][i];
                wbci[5][i] = wb[2][i] * wci[1][i] + wb[5][i] * wci[2][i] + wb[8][i] * wci[4][i];
                wbci[6][i] = wb[0][i] * wci[3][i] + wb[3][i] * wci[4][i] + wb[6][i] * wci[5][i];
                wbci[7][i] = wb[1][i] * wci[3][i] + wb[4][i] * wci[4][i] + wb[7][i] * wci[5][i];
                wbci[8][i] = wb[2][i] * wci[3][i] + wb[5][i] * wci[4][i] + wb[8][i] * wci[5][i];
                // *
                // * subtraction of wbci * (wb)t from matrix wa
                wa[0] = wa[0] - wbci[0][i] * wb[0][i] - wbci[3][i] * wb[3][i] - wbci[6][i] * wb[6][i];
                wa[1] = wa[1] - wbci[0][i] * wb[1][i] - wbci[3][i] * wb[4][i] - wbci[6][i] * wb[7][i];
                wa[2] = wa[2] - wbci[1][i] * wb[1][i] - wbci[4][i] * wb[4][i] - wbci[7][i] * wb[7][i];
                wa[3] = wa[3] - wbci[0][i] * wb[2][i] - wbci[3][i] * wb[5][i] - wbci[6][i] * wb[8][i];
                wa[4] = wa[4] - wbci[1][i] * wb[2][i] - wbci[4][i] * wb[5][i] - wbci[7][i] * wb[8][i];
                wa[5] = wa[5] - wbci[2][i] * wb[2][i] - wbci[5][i] * wb[5][i] - wbci[8][i] * wb[8][i];
                // *
                // * subtraction of wbci * tt from vector tv
                tv[0] = tv[0] - wbci[0][i] * tt[0][i] - wbci[3][i] * tt[1][i] - wbci[6][i] * tt[2][i];
                tv[1] = tv[1] - wbci[1][i] * tt[0][i] - wbci[4][i] * tt[1][i] - wbci[7][i] * tt[2][i];
                tv[2] = tv[2] - wbci[2][i] * tt[0][i] - wbci[5][i] * tt[1][i] - wbci[8][i] * tt[2][i];
                // *
            }// check on whether to use track
        }// loop over tracks

        // beam constraint
        if (fitwb) {
            wa[0] += 1. / (sigxbe * sigxbe);
            wa[2] += 1. / (sigybe * sigybe);
            wa[5] += 1. / (sigzbe * sigzbe);
            tv[0] += (beamoy[0] - xyz[0]) / (sigxbe * sigxbe);
            tv[1] += (beamoy[1] - xyz[1]) / (sigybe * sigybe);
            tv[2] += (beamoy[2] - xyz[2]) / (sigzbe * sigzbe);
        }
        // *
        // * solution of the linear system
        // *
        // * covariance matrix on vertex
        vcov = pxmi3(wa);
        for (int ii = 0; ii < vcov.length; ++ii) {
//            System.out.println("vcov[" + ii + "]= " + vcov[ii]);
        }
        //
        // *
        // * corrections to vertex coordinates
        dxyz[0] = vcov[0] * tv[0] + vcov[1] * tv[1] + vcov[3] * tv[2];
        dxyz[1] = vcov[1] * tv[0] + vcov[2] * tv[1] + vcov[4] * tv[2];
        dxyz[2] = vcov[3] * tv[0] + vcov[4] * tv[1] + vcov[5] * tv[2];
        xyzf[0] = xyz[0] + dxyz[0];
        xyzf[1] = xyz[1] + dxyz[1];
        xyzf[2] = xyz[2] + dxyz[2];
        // *
        // * corrections to track parameters and covariance matrices
        // *
        for (int i = 0; i < ntrk; ++i) {
            if (invtx[i]) {
                // *
                // * variation on par is wci * tt - (wbci)t * dxyz
                parf[0][i] = par[2][i]
                        + wci[0][i]
                        * tt[0][i]
                        + wci[1][i]
                        * tt[1][i]
                        + wci[3][i]
                        * tt[2][i]
                        - wbci[0][i]
                        * dxyz[0]
                        - wbci[1][i]
                        * dxyz[1]
                        - wbci[2][i]
                        * dxyz[2];
                parf[1][i] = phiv[i]
                        + wci[1][i]
                        * tt[0][i]
                        + wci[2][i]
                        * tt[1][i]
                        + wci[4][i]
                        * tt[2][i]
                        - wbci[3][i]
                        * dxyz[0]
                        - wbci[4][i]
                        * dxyz[1]
                        - wbci[5][i]
                        * dxyz[2];
                parf[2][i] = par[4][i]
                        + wci[3][i]
                        * tt[0][i]
                        + wci[4][i]
                        * tt[1][i]
                        + wci[5][i]
                        * tt[2][i]
                        - wbci[6][i]
                        * dxyz[0]
                        - wbci[7][i]
                        * dxyz[1]
                        - wbci[8][i]
                        * dxyz[2];
                // *
                // * covariance matrix of par is wci + (wbci)t * vcov * wbci
                tcov[0][i] = wci[0][i]
                        + wbci[0][i]
                        * (vcov[0] * wbci[0][i] + vcov[1] * wbci[1][i] + vcov[3] * wbci[2][i])
                        + wbci[1][i]
                        * (vcov[1] * wbci[0][i] + vcov[2] * wbci[1][i] + vcov[4] * wbci[2][i])
                        + wbci[2][i]
                        * (vcov[3] * wbci[0][i] + vcov[4] * wbci[1][i] + vcov[5] * wbci[2][i]);

                tcov[1][i] = wci[1][i]
                        + wbci[0][i]
                        * (vcov[0] * wbci[3][i] + vcov[1] * wbci[4][i] + vcov[3] * wbci[5][i])
                        + wbci[1][i]
                        * (vcov[1] * wbci[3][i] + vcov[2] * wbci[4][i] + vcov[4] * wbci[5][i])
                        + wbci[2][i]
                        * (vcov[3] * wbci[3][i] + vcov[4] * wbci[4][i] + vcov[5] * wbci[5][i]);

                tcov[2][i] = wci[2][i]
                        + wbci[3][i]
                        * (vcov[0] * wbci[3][i] + vcov[1] * wbci[4][i] + vcov[3] * wbci[5][i])
                        + wbci[4][i]
                        * (vcov[1] * wbci[3][i] + vcov[2] * wbci[4][i] + vcov[4] * wbci[5][i])
                        + wbci[5][i]
                        * (vcov[3] * wbci[3][i] + vcov[4] * wbci[4][i] + vcov[5] * wbci[5][i]);

                tcov[3][i] = wci[3][i]
                        + wbci[0][i]
                        * (vcov[0] * wbci[6][i] + vcov[1] * wbci[7][i] + vcov[3] * wbci[8][i])
                        + wbci[1][i]
                        * (vcov[1] * wbci[6][i] + vcov[2] * wbci[7][i] + vcov[4] * wbci[8][i])
                        + wbci[2][i]
                        * (vcov[3] * wbci[6][i] + vcov[4] * wbci[7][i] + vcov[5] * wbci[8][i]);

                tcov[4][i] = wci[4][i]
                        + wbci[3][i]
                        * (vcov[0] * wbci[6][i] + vcov[1] * wbci[7][i] + vcov[3] * wbci[8][i])
                        + wbci[4][i]
                        * (vcov[1] * wbci[6][i] + vcov[2] * wbci[7][i] + vcov[4] * wbci[8][i])
                        + wbci[5][i]
                        * (vcov[3] * wbci[6][i] + vcov[4] * wbci[7][i] + vcov[5] * wbci[8][i]);

                tcov[5][i] = wci[5][i]
                        + wbci[6][i]
                        * (vcov[0] * wbci[6][i] + vcov[1] * wbci[7][i] + vcov[3] * wbci[8][i])
                        + wbci[7][i]
                        * (vcov[1] * wbci[6][i] + vcov[2] * wbci[7][i] + vcov[4] * wbci[8][i])
                        + wbci[8][i]
                        * (vcov[3] * wbci[6][i] + vcov[4] * wbci[7][i] + vcov[5] * wbci[8][i]);

                // *
            } // check on whether to use track
        } // loop over tracks

        // *
        // * chi2 with fitted values
        // *
        chi2 = 0.;
        if (fitwb)
            chi2 += ( (xyzf[0] - beamoy[0]) / sigxbe)
                    * ( (xyzf[0] - beamoy[0]) / sigxbe)
                    + ( (xyzf[1] - beamoy[1]) / sigybe)
                    * ( (xyzf[1] - beamoy[1]) / sigybe)
                    + ( (xyzf[2] - beamoy[2]) / sigzbe)
                    * ( (xyzf[2] - beamoy[2]) / sigzbe);

        for (int i = 0; i < ntrk; ++i) {
            if (invtx[i]) {
                uu = xyzf[0] * cos(parf[1][i]) + xyzf[1] * sin(parf[1][i]);
                vv = xyzf[1] * cos(parf[1][i]) - xyzf[0] * sin(parf[1][i]);
                epsf = -vv - .5 * uu * uu * parf[2][i];
                zpf = xyzf[2] - uu / tan(parf[0][i]);
                phif = parf[1][i] - uu * parf[2][i];
                // not very efficient here...
                double[] tmp = new double[15];
                for (int j = 0; j < 15; ++j) {
                    tmp[j] = wgt[j][i];
                }
                chi2tr[i] = fwdch2(tmp, epsf - par[0][i], zpf - par[1][i], parf[0][i] - par[2][i], phif - par[3][i],
                        parf[2][i] - par[4][i]);
                chi2 += chi2tr[i];
            }
        }
//        System.out.println("chi2= " + chi2);
        return new Vertex(ntrk, xyzf, parf, vcov, tcov, chi2, chi2tr);

    }

}
