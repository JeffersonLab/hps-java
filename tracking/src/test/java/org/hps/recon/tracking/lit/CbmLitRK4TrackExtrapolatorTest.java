package org.hps.recon.tracking.lit;

import java.util.Arrays;
import junit.framework.TestCase;

/**
 *
 * @author ngraf
 */
public class CbmLitRK4TrackExtrapolatorTest extends TestCase {

    /**
     * Test of Extrapolate method, of class CbmLitRK4TrackExtrapolator.
     */
    public void testCbmLitRK4TrackExtrapolator() {
        // create a simple magnetic field
        double By = -1.;//-80.; //!?
        TestMagneticField field = new TestMagneticField(0., By, 0.);
        double fieldZMax = 70.;
        field.setZMax(fieldZMax);

        // create a track parameter
        CbmLitTrackParam parIn = new CbmLitTrackParam();
        double pmom = 2.;
        double[] pars = new double[5];
        pars[0] = 0.; //x
        pars[1] = 0.; //y
        pars[2] = 0.0; // x'
        pars[3] = 0.0; // y'
        pars[4] = 1.0 / pmom; // q/p
        parIn.SetStateVector(pars);
        parIn.SetZ(0.);

        CbmLitTrackParam parOut = new CbmLitTrackParam();

        // path should be a circle for z < fieldZMax
        // path should be a straight line for z > fieldZMax
        // create an extrapolator...
        CbmLitRK4TrackExtrapolator extrap = new CbmLitRK4TrackExtrapolator(field);

        // test extrapolation to a z position...
        double z = 37.;
        extrap.Extrapolate(parIn, parOut, z, null);
        System.out.println("extrap to z= "+z+" "+parOut.GetX() + " " + parOut.GetY() + " " + parOut.GetZ());

        // test extrapolation to a Z Plane at this z position
        double x0 = .1;
        CartesianThreeVector pos = new CartesianThreeVector(0., 0., z);
        CartesianThreeVector eta = new CartesianThreeVector(0., 0., 1.);
        
        DetectorPlane p = new DetectorPlane("p1", pos, eta, x0, 0.);
        CbmLitTrackParam planePar = new CbmLitTrackParam(parIn);
        extrap.Extrapolate(planePar, p, null);
        System.out.println("extrap to plane at z= "+z+" "+planePar.GetX() + " " + planePar.GetY() + " " + planePar.GetZ());

        assertEquals(parOut.GetX(), planePar.GetX(), .00001);
        
        //TODO test non ZPlane
        //TODO test non-uniform field propagation
        
        
        // now let's test continuous extrapolation...
        double[] zees = new double[100];
        for (int i = 0; i < 100; ++i) {
            zees[i] = i + 1.;
        }

        double[] xx = new double[100];
        double[] zz = new double[100];
        double[] ww = new double[100];
        Arrays.fill(ww, .01);
        int nCircleFit = 0;
        for (int i = 0; i < 100; ++i) {
            extrap.Extrapolate(parIn, parOut, zees[i], null);
            System.out.println(parOut.GetX() + " " + parOut.GetY() + " " + parOut.GetZ());
            if (i < fieldZMax) {
                xx[i] = parOut.GetX();
                zz[i] = parOut.GetZ();
                nCircleFit++;
            }
            parIn = parOut;
        }
        CircleFitter fitter = new CircleFitter();
        fitter.setreferenceposition(0., 0.);
        boolean OK = fitter.fit(xx, zz, ww, nCircleFit);
        //if(OK) System.out.println("Fit OK");
        CircleFit cf = fitter.getfit();
        System.out.println(cf);
        //TODO  put some assert statements here
        //TODO add in a linear fit for field free regions.
        //TODO add in a linear fit and tests for y
        System.out.println("input momentum: " + pmom + " fitted momentum (-0.000299*B/curvature) " + -0.000299792458 * By / cf.curvature());
        
    }
}

class TestMagneticField implements CbmLitField {

    private double[] _fieldValue = new double[3];
    private double[] _zeroField = new double[3];
    private double _zmax = Double.MAX_VALUE;

    public TestMagneticField(double Bx, double By, double Bz) {
        _fieldValue[0] = Bx;
        _fieldValue[1] = By;
        _fieldValue[2] = Bz;
    }

    public TestMagneticField(double[] fieldValue) {
        System.arraycopy(fieldValue, 0, _fieldValue, 0, 3);
    }

    public void setZMax(double z) {
        _zmax = z;
    }

    public void GetFieldValue(double x, double y, double z, double[] field) {
        if (z < _zmax) {
            System.arraycopy(_fieldValue, 0, field, 0, 3);
        } else {
            System.arraycopy(_fieldValue, 0, _zeroField, 0, 3);
        }
    }
}

class CircleFitter {
    /*
     **********************************************************************
     * SUBROUTINE CIRCLF(XREF,YREF,XX,YY,WW,NP,MODE,IERROR) ********** ****** *
     * * Non-iterative circle fit (V. Karimaki/1991) * * XREF,YREF = reference
     * point coordinates (IN) * XX,YY = arrays of measured coordinates (IN) * WW
     * = array of weigths (IN) * NP = number of points (IN) * MODE = 1: solve
     * parameters plus error matrix (IN) * 2: point removal/adding and error
     * matrix * 3: propagation, parameters and error matrix * -1,-2,-3: as 1,2,3
     * but without error matrix * IERROR = error flag (=0 if fit OK) (OUT) * *
     * Fit results in COMMON/CIRCFI/: * RHO = fitted curvature (OUT) * PHI =
     * fitted direction (OUT) * DCA = fitted distance to (XREF,YREF) (OUT) *
     * CHICIR = chi squared of the fit (OUT) * XPCA,YPCA = point on circle
     * closest to XREF,YREF (OUT) * COVRFD(6) = covariance matrix of RHO,PHI,DCA
     * (OUT) * in lower triangular form * * NOTES: 1. Weights should be negative
     * for point removal * 2. MODE = +-2 possible only if COMMON/CIRCFI/ fully *
     * restored or if call is subsequent to MODE=+-1 * 3. MODE = +-3 possible
     * only if COMMON/CIRCFI/ is * restored with the old parameters
     * RHO,...,COVRFD(6) *
     * --------------------------------------------------------------------*
     */

    private double _xref; // x reference position;
    private double _yref; // y reference position;
    private double _rho; // curvature
    private double _phi; // phi angle at (_xref, _yref)
    private double _dca; // distance of closest approach to (_xref, _yref)
    private double _chicir; // chi-squared of circle fit
    private double _xpca;
    private double _ypca;
    private double[] _covrfd;
    private double _xx0;
    private double _yy0;
    private double S1, S2, S3, S4, S5, S6, S7, S8, S9;

    /**
     * Default Constructor sets reference point to (0,0)
     *
     */
    public CircleFitter() {
        _xref = 0.;
        _yref = 0.;

        _covrfd = new double[6];
    }

    /**
     * Set the reference point for the fit.
     *
     * @param xref x position of the reference point
     * @param yref y position of the reference point
     */
    public void setreferenceposition(double xref, double yref) {
        _xref = xref;
        _yref = yref;
    }

    /**
     * Fit the data points
     *
     * @param XX array of x positions
     * @param YY array of y positions
     * @param WW array of weights
     * @param NP number of points
     * @return true if fit is successful
     */
    public boolean fit(double[] XX, double[] YY, double[] WW, int NP) {
        //     COMMON /CIRCFI/ RHO,PHI,DCA,CHICIR,XPCA,YPCA,COVRFD(6)
        //    +,          XX0,YY0,S1,S2,S3,S4,S5,S6,S7,S8,S9

        double SR, HSR, S1I, XMEAN, YMEAN, RRMEAN, XC, YC, WT, WX, WY, RR, WR;
        double COV1, COV2, COV3, COV4, COV5, COV6, Y2FI, X2FI, HAPFIT, DELFIT, RHOF, DFT;
        double FIFIT, SINF, COSF, SA, SB, SG, SAA, APU, SXYR, ROD1, ROD2, SINF2, COSF2, SIN;

        // check that we have enough points for a reasonable fit
        if (NP < 3) {
            return false;
        }
        //initialize some variables
        S1 = 0.;
        S2 = 0.;
        S3 = 0.;
        S4 = 0.;
        S5 = 0.;
        S6 = 0.;
        S7 = 0.;
        S8 = 0.;
        S9 = 0.;
        //   IMOD=IABS(MODE)
        //   IF (IMOD.LT.2) THEN
        int M3 = NP / 3;
        // _xx0, _yy0 is a local origin
        _xx0 = XX[M3];
        _yy0 = YY[M3];
        // DIRTX, DIRTY for direction test
        double DIRTX = _xx0 - XX[0];
        double DIRTY = _yy0 - YY[0];
        //     ELSE       //mod=3, i.e prop errors only
        //        DIRTX=COS(_phi)
        //        DIRTY=SIN(_phi)
        //     ENDIF
        //     IF (IMOD.EQ.3)                   GO TO  50
        // calculate sums for fit

        for (int IP = 0; IP < NP; ++IP) {
            XC = XX[IP] - _xx0;
            YC = YY[IP] - _yy0;
            WT = WW[IP];
            WX = WT * XC;
            WY = WT * YC;
            RR = XC * XC + YC * YC;
            WR = WT * RR;
            S1 += WT;
            S2 += WX;
            S3 += WY;
            S4 += WX * XC;
            S5 += WX * YC;
            S6 += WY * YC;
            S7 += WX * RR;
            S8 += WY * RR;
            S9 += WR * RR;
        }
        if (S1 < 0.) {
            return false;
        }
        //
        //--- Solve the fitted parameters
        //
        S1I = 1. / S1;
        SR = S4 + S6;
        HSR = 0.5 * SR;
        XMEAN = S1I * S2;
        YMEAN = S1I * S3;
        RRMEAN = S1I * SR;
        COV1 = S1I * (S4 - S2 * XMEAN);
        COV2 = S1I * (S5 - S2 * YMEAN);
        COV3 = S1I * (S6 - S3 * YMEAN);
        COV4 = S1I * (S7 - S2 * RRMEAN);
        COV5 = S1I * (S8 - S3 * RRMEAN);
        COV6 = S1I * (S9 - SR * RRMEAN);
        if (COV6 < 0.) {
            return false;
        }
        Y2FI = 2. * (COV2 * COV6 - COV4 * COV5);
        X2FI = COV6 * (COV1 - COV3) - COV4 * COV4 + COV5 * COV5;
        FIFIT = 0.5 * Math.atan2(Y2FI, X2FI);
        COSF = Math.cos(FIFIT);
        SINF = Math.sin(FIFIT);
        HAPFIT = (SINF * COV4 - COSF * COV5) / COV6;
        DELFIT = -HAPFIT * RRMEAN + SINF * XMEAN - COSF * YMEAN;
        APU = Math.sqrt(1. - 4. * HAPFIT * DELFIT);
        RHOF = 2. * HAPFIT / APU;
        DFT = 2. * DELFIT / (1. + APU);
        ROD1 = 1. / APU;
        ROD2 = ROD1 * ROD1;
        SINF2 = SINF * SINF;
        COSF2 = COSF * COSF;
        double SINFF = 2. * SINF * COSF;
        SA = SINF * S2 - COSF * S3;
        SAA = SINF2 * S4 - SINFF * S5 + COSF2 * S6;
        SXYR = SINF * S7 - COSF * S8;
        //
        _rho = RHOF;
        _phi = FIFIT;
        _dca = DFT;
        _chicir = ROD2 * (-DELFIT * SA - HAPFIT * SXYR + SAA);
        _xpca = _xx0 + _dca * SINF;
        _ypca = _yy0 - _dca * COSF;
        //
        //--- Error estimation ro,fi,d
        //
        //IF (MODE.GT.0) THEN
        SB = COSF * S2 + SINF * S3;
        SG = (SINF2 - COSF2) * S5 + SINF * COSF * (S4 - S6);
        double W1 = .25 * S9 - DFT * (SXYR - DFT * (SAA + HSR - DFT * (SA - .25 * DFT * S1)));
        double W2 = -ROD1 * (0.5 * (COSF * S7 + SINF * S8) - DFT * (SG - 0.5 * DFT * SB));
        double W3 = ROD2 * (COSF2 * S4 + SINFF * S5 + SINF2 * S6);
        double W4 = RHOF * (-0.5 * SXYR + DFT * SAA) + ROD1 * HSR - 0.5 * DFT * ((2. * ROD1 + RHOF * DFT) * SA - DFT * ROD1 * S1);
        double W5 = ROD1 * RHOF * SG - ROD2 * SB;
        double W6 = RHOF * (RHOF * SAA - 2. * ROD1 * SA) + ROD2 * S1;
        double SD1 = W3 * W6 - W5 * W5;
        double SD2 = -W2 * W6 + W4 * W5;
        double SD3 = W2 * W5 - W3 * W4;
        double DETINV = 1. / (W1 * SD1 + W2 * SD2 + W4 * SD3);
        _covrfd[0] = DETINV * SD1;
        _covrfd[1] = DETINV * SD2;
        _covrfd[2] = DETINV * (W1 * W6 - W4 * W4);
        _covrfd[3] = DETINV * SD3;
        _covrfd[4] = DETINV * (W2 * W4 - W1 * W5);
        _covrfd[5] = DETINV * (W1 * W3 - W2 * W2);
        double XDERO = 0.5 * (RHOF * SXYR - 2. * ROD1 * SAA + (1. + ROD1) * DFT * SA);
        double EDERO = DFT * XDERO;
        double EDEDI = RHOF * XDERO;
        double DROF = _covrfd[0] * EDERO + _covrfd[3] * EDEDI;
        double DFIF = _covrfd[1] * EDERO + _covrfd[4] * EDEDI;
        double DDIF = _covrfd[3] * EDERO + _covrfd[5] * EDEDI;
        _rho += DROF;
        _dca += DDIF;
        _phi += DFIF;
        _chicir = (1. + _rho * _dca) * (1. + _rho * _dca) * _chicir / ROD2;
        //  ENDIF
        //   50 CONTINUE   propagation of parameters and errors
        // IF (IMOD.EQ.3) THEN
        //   SINF=SIN(_phi)
        //   COSF=COS(_phi)
        // ENDIF
        propagate(_xref, _yref, SINF, COSF, DIRTX, DIRTY);
        return true;

    }

    /**
     * Get the results of the fit.
     *
     * @return the results of the fit in a CircleFit object
     */
    public CircleFit getfit() {
        return new CircleFit(_xref, _yref, _rho, _phi, _dca, _chicir, _covrfd);
    }

    void propagate(double x, double y, double SINF, double COSF, double DIRTX, double DIRTY) {
        //
        //--- Propagate parameters to  XREF,YREF
        //
        double[] XJACOB = new double[9];
        double ROD1;
        // first set _xref, _yref
        setreferenceposition(x, y);
        double XMOVE = _xpca - _xref;
        double YMOVE = _ypca - _yref;
        ROD1 = 1. + _rho * _dca;
        double DPERP = XMOVE * SINF - YMOVE * COSF;
        double DPARA = XMOVE * COSF + YMOVE * SINF;
        double ZEE = DPERP * DPERP + DPARA * DPARA;
        double AA = 2. * DPERP + _rho * ZEE;
        double UU = Math.sqrt(1. + _rho * AA);
        double SQ1AI = 1. / (1. + UU);
        double BB = _rho * XMOVE + SINF;
        double CC = -_rho * YMOVE + COSF;
        _phi = Math.atan2(BB, CC);
        _dca = AA * SQ1AI;
        //
        //--- Propagate error matrix to XREF,YREF
        //
        //IF (MODE.GT.0) THEN
        double VV = 1. + _rho * DPERP;
        double XEE = 1. / (CC * CC + BB * BB);
        double XLA = 0.5 * AA * SQ1AI * SQ1AI / UU;
        double XMU = SQ1AI / UU + _rho * XLA;
        XJACOB[0] = 1.;
        XJACOB[1] = 0.;
        XJACOB[2] = 0.;
        XJACOB[3] = XEE * DPARA;
        XJACOB[4] = XEE * ROD1 * VV;
        XJACOB[5] = -XJACOB[3] * _rho * _rho;
        XJACOB[6] = XMU * ZEE - XLA * AA;
        XJACOB[7] = 2. * XMU * ROD1 * DPARA;
        XJACOB[8] = 2. * XMU * VV;
        // overwrite _covrfd in place
        abatranspose(XJACOB, _covrfd);
        SINF = Math.sin(_phi);
        COSF = Math.cos(_phi);
        //ENDIF
        //
        //--- check direction
        //
        double DIRTES = COSF * DIRTX + SINF * DIRTY;
        if (DIRTES < 0.) {
            _phi = _phi + Math.PI;
            COSF = -COSF;
            SINF = -SINF;
            _dca = -_dca;
            _rho = -_rho;
            _covrfd[1] = -_covrfd[1]; //V rho-phi
            _covrfd[4] = -_covrfd[4]; //V phi-d
        }
        if (_phi >= 2. * Math.PI) {
            _phi -= 2. * Math.PI;
        }
        if (_phi < 0.) {
            _phi += 2. * Math.PI;
        }
        _xpca = _xref + _dca * SINF;
        _ypca = _yref - _dca * COSF;
    }

    /**
     * Propagate the fit parameters to a new reference point
     *
     * @param x x position of the new reference point
     * @param y y position of the new reference point
     */
    public CircleFit propagatefit(double x, double y) {
        double DIRTX = Math.cos(_phi);
        double DIRTY = Math.sin(_phi);
        double SINF = Math.sin(_phi);
        double COSF = Math.cos(_phi);
        propagate(x, y, SINF, COSF, DIRTX, DIRTY);
        return new CircleFit(_xref, _yref, _rho, _phi, _dca, _chicir, _covrfd);
    }

    private void abatranspose(double[] A, double[] B) {
        /*
         * SUBROUTINE TRASA3(A,B) * MATRIX OPERATION ABA'-->B FOR 3X3 MATRICES *
         * B is a packed symmetric matrix * A is 3x3 matrix packed row-wise (A'
         * means transpose) *
         * ---------------------------------------------------------------*
         */
        double[] C = new double[6];
        double E1, E2, E3, E4, E5, E6, E7, E8, E9;
        //for (int i=0; i < 6; ++i) C[i] = B[i];
        System.arraycopy(B, 0, C, 0, 6);
        E1 = A[0] * C[0] + A[1] * C[1] + A[2] * C[3];
        E2 = A[0] * C[1] + A[1] * C[2] + A[2] * C[4];
        E3 = A[0] * C[3] + A[1] * C[4] + A[2] * C[5];
        E4 = A[3] * C[0] + A[4] * C[1] + A[5] * C[3];
        E5 = A[3] * C[1] + A[4] * C[2] + A[5] * C[4];
        E6 = A[3] * C[3] + A[4] * C[4] + A[5] * C[5];
        E7 = A[6] * C[0] + A[7] * C[1] + A[8] * C[3];
        E8 = A[6] * C[1] + A[7] * C[2] + A[8] * C[4];
        E9 = A[6] * C[3] + A[7] * C[4] + A[8] * C[5];
        B[0] = A[0] * E1 + A[1] * E2 + A[2] * E3;
        B[1] = A[3] * E1 + A[4] * E2 + A[5] * E3;
        B[2] = A[3] * E4 + A[4] * E5 + A[5] * E6;
        B[3] = A[6] * E1 + A[7] * E2 + A[8] * E3;
        B[4] = A[6] * E4 + A[7] * E5 + A[8] * E6;
        B[5] = A[6] * E7 + A[7] * E8 + A[8] * E9;
    }
    /*
     * public static void main(String[] args) { CircleFitter fitter = new
     * CircleFitter(); double[] xx = new double[40]; double[] yy = new
     * double[40]; double[] ww = new double[40];
     *
     * double[] pulls = new double[3]; double[][] pulsum = new double[2][3];
     * double[] pullerr = new double[3]; int event = 0; try { FileReader file =
     * new FileReader("fort.4"); BufferedReader buff = new BufferedReader(file);
     * boolean eof = false; while(!eof) { ++event; for (int i = 0; i<40; ++i) {
     * String line = buff.readLine(); if (line == null) { eof = true; break; }
     * else { //System.out.println(line); StringTokenizer st = new
     * StringTokenizer(line); xx[i] = Double.parseDouble(st.nextToken()); yy[i]
     * = Double.parseDouble(st.nextToken()); ww[i] =
     * Double.parseDouble(st.nextToken()); } } if(!eof) { System.out.println("
     * Event "+ event); boolean OK=fitter.fit(xx,yy,ww,40); if(OK) { //
     * System.out.println(fitter.getfit()); CircleFit cf = fitter.getfit();
     * double[] covmat = cf.cov(); double ROERR=Math.sqrt(covmat[0]); double
     * FIERR=Math.sqrt(covmat[2]); double DCERR=Math.sqrt(covmat[5]);
     *
     * pulls[0]=(0.01-cf.curvature())/ROERR; pulls[1]=(1.23-cf.phi())/FIERR;
     * pulls[2]=(0.1-cf.dca())/DCERR;
     *
     * for(int i =0; i<3;++i) { // System.out.println("pulls["+i+"]=
     * "+pulls[i]); pulsum[0][i]+=pulls[i]; pulsum[1][i]+=pulls[i]*pulls[i]; }
     * //test propagate method... double XTEST = 0.1*Math.sin(1.23); double
     * YTEST = -0.1*Math.cos(1.23); XTEST = 0.; YTEST = 0.;
     * System.out.println("XTEST= "+XTEST+", YTEST= "+YTEST);
     * fitter.propagatefit(XTEST, YTEST); CircleFit cf2 = fitter.getfit();
     * System.out.println(cf); System.out.println(cf2); } eof = true; } }
     *
     * }
     * catch (IOException e) { System.out.println("Error -- " + e.toString()); }
     * double FACTO=1./Math.sqrt((double)event); // //--- Calculate means and
     * std's of the pull values //
     *
     * for (int i=0; i<3; ++i) { pulsum[0][i]=pulsum[0][i]/(double)event;
     * pulsum[1][i]=Math.sqrt((pulsum[1][i])/(double)event-(pulsum[0][i]*pulsum[0][i]));
     * pullerr[i]=FACTO*pulsum[1][i]; } for (int i = 0; i<3; ++i) { double TEST
     * = Math.abs(pulsum[0][i]/pullerr[i]); System.out.println(pulsum[0][i]+"
     * "+pullerr[i]+" "+pulsum[1][i]+" "+TEST); } }
     */
}

/**
 * ************** END OF CIRCLE FITTING CODE ********************
 */
class CircleFit {

    private double _xref; // x reference position;
    private double _yref; // y reference position;
    private double _rho; // curvature
    private double _phi; // phi angle at (_xref, _yref)
    private double _dca; // distance of closest approach to (_xref, _yref)
    private double _chisq; // chi-squared of circle fit
    private double[] _covrfd; // covariance matrix of fit
    // parameters in lower triangular form
    //Constructor

    public CircleFit(double x, double y, double rho, double phi, double dca, double chi2, double[] cov) {
        _xref = x;
        _yref = y;
        _rho = rho;
        _phi = phi;
        _dca = dca;
        _chisq = chi2;
        _covrfd = cov;
    }

    /**
     * The phi angle at the point of reference.
     *
     * @return phi
     */
    public double phi() {
        return _phi;
    }

    /**
     * The curvature of the fit.
     *
     * @return the fit curvature.
     */
    public double curvature() {
        return _rho;
    }

    /**
     * The x position of the reference point
     *
     * @return x reference point.
     */
    public double xref() {
        return _xref;
    }

    /**
     * The y position of the reference point.
     *
     * @return y reference point.
     */
    public double yref() {
        return _yref;
    }

    /**
     * The distance of closest approach to the reference point.
     *
     * @return distance of closest approach to (xref, yref)
     */
    public double dca() {
        return _dca;
    }

    /**
     * The chi-squared for the fit.
     *
     * @return chi-squared for the circle fit.
     */
    public double chisq() {
        return _chisq;
    }

    /**
     * The covariance matrix of fit
     *
     * @return covariance matrix of fit
     */
    public double[] cov() {
        double[] tmp = new double[6];
        System.arraycopy(_covrfd, 0, tmp, 0, 6);
        return tmp;
    }

    /**
     * String representation of this object
     *
     * @return string representation of this object.
     */
    public String toString() {
        return "CircleFit at x= " + _xref + ", y= " + _yref + System.getProperty("line.separator") + "      with dca= " + _dca + ", curvature= " + _rho + " and phi= " + _phi;
        //return "CircleFit at x= "+_xref+", y= "+_yref+"\n      with dca= "+_dca+", curvature= "+_rho+", phi= "+_phi+", chsq= "+_chisq;

    }
    /*
     * public static void main(String[] args) { double[] cov =
     * {1.,0.,0.,1.,0.,1.}; CircleFit cf = new CircleFit(0.,0.,.01,.3,0.1,12.,
     * cov); System.out.println(cf); double[] covmat = cf.cov(); for(int
     * i=0;i<covmat.length;++i) { System.out.println(covmat[i]); } }
     */
}
