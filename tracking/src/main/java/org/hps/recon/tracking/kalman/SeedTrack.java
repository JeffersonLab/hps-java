package org.hps.recon.tracking.kalman;

import java.util.ArrayList;
import java.util.Comparator;

import org.apache.commons.math.util.FastMath;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
/**
 * 
 * Fit a line/parabola approximation of a helix to a set of measurement points.
 * The line and parabola are fit simultaneously in order to handle properly the stereo layers.
 * The pivot of the helix is assumed to be the origin of the coordinate system, but the
 * coordinate system in which the helix is described may be rotated slightly with respect to
 * the global coordinates, in order to optimize its alignment with the field.
 * @author Robert Johnson
 *
 */
class SeedTrack {
    boolean success;
    ArrayList<KalHit> hits; // Save information for the hit used in each layer
    private double drho, phi0, K, dz, tanl; // Helix parameters derived from the line/parabola fits
    private Vec hParm; // Final helix parameters rotated into the field frame
    // private RotMatrix Rot; // Orthogonal transformation from global to helix
    // (B-field) coordinates
    private DMatrixRMaj C; // Covariance of helix parameters
    private DMatrixRMaj CovA;  // Covariance of the 5 linear fit polynomial coefficients
    private double R, xc, yc; // Radius and center of the "helix"
    private double Bavg; // Average B field
    private double alpha; // Conversion constant from 1/pt to helix radius
    double yOrigin;
    double chi2;
    private Plane p0; // x,z plane at the target location
    int Nbending;
    int Nnonbending;
    
    private static int nHits;
    private static double [][] M;
    private static double[] xMC = null;
    private static double[] zMC = null;
    private static double[] yMC = null;
    private static double[] mTrue = null;
    private static double[] y; // Global y coordinates of measurements (along beam direction)
    private static double[] v; // Measurement value (i.e. position of the hit strip)
    private static double[] s; // Uncertainty in the measurement (spatial resolution of the SSD)
    private static double[][] delta;
    private static double[][] R2;
    private static DMatrixRMaj Mint;
    private static double [][] A;
    private static double [] B;
    private static DMatrixRMaj a;  // Solution vector (line coefficients followed by parabola coefficients
    private static double [] vpred;
    private static final boolean debug = false; // Set true to generate lots of debug printout
    //private static int nCalls;

    double getAlpha() {
        return alpha;
    }

    void print(String s) {
        System.out.format("%s", this.toString(s));
    }
    
    String toString(String s) {
        String str;
        if (success) {
            str = String.format("Seed track %s: B=%10.7f helix= %10.6f, %10.6f, %10.6f, %10.6f, %10.6f\n", s, Bavg, drho, phi0, K, dz, tanl);
            str=str+String.format("  Number of hits in the bending plane=%d; in the non-bending plane=%d\n", Nbending, Nnonbending);
            str=str+hParm.toString("helix parameters rotated into magnetic field frame")+"\n";
            str=str+String.format("  Note that these parameters are with respect to a pivot point 0. %10.7f 0.\n", yOrigin);
            double yP = p0.X().v[1];
            double[] pivot = { 0., yP, 0. };
            double[] a = this.pivotTransform(pivot);
            str=str+String.format("    helix parameters transformed to y=%8.2f: %10.6f, %10.6f, %10.6f, %10.6f, %10.6f\n", yP, a[0], a[1], a[2],
                    a[3], a[4]);
            str=str+String.format("  seed track hits:");
            for (int j = 0; j < hits.size(); j++) str=str+String.format(" %d: %10.5f, ", hits.get(j).module.Layer, hits.get(j).hit.v);
            str=str+"\n";
            Vec pInt = planeIntersection(p0);
            double xzDist = FastMath.sqrt(pInt.v[0]*pInt.v[0] + pInt.v[2]*pInt.v[2]);
            str=str+String.format("  Distance from origin in X,Z at y= %8.2f is %10.5f. Intersection=%s\n", yP, xzDist, pInt.toString());
            str = str + "Covariance: " + C.toString();
        } else {
            str = String.format("Seed track %s fit unsuccessful.\n", s);
        }
        return str;
    }

    // Older interface
    SeedTrack(ArrayList<SiModule> data, // List of Si modules with data
            double yOrigin, // New origin along beam to use for the fit
            ArrayList<int[]> hitList, // Element 0= index of Si module; Element 1= hit number
            boolean verbose // Set true for lots of debug printout
    ) {
        ArrayList<KalHit> theHits = new ArrayList<KalHit>(hitList.size());
        for (int i = 0; i < hitList.size(); i++) {
            SiModule thisSi = data.get(hitList.get(i)[0]);
            KalHit tmpHit = new KalHit(thisSi, thisSi.hits.get(hitList.get(i)[1]));
            theHits.add(tmpHit);
        }
        double yTarget = 0.;
        SeedTracker(theHits, yOrigin, yTarget);
    }

    // Newer interface
    SeedTrack(ArrayList<KalHit> hitList, double yOrigin, double yTarget) {
        SeedTracker(hitList, yOrigin, yTarget);
    }

    
    private void SeedTracker(ArrayList<KalHit> hitList, double yOrigin, double yTarget) {
        // yOrigin is the location along the beam about which we fit the seed helix
        // yTarget is the location along the beam of the target itself
        //if (nCalls < 3) debug = true;
        //else debug = false;
        //nCalls++;

        p0 = new Plane(new Vec(0., yTarget, 0.), new Vec(0., 1., 0.));  // create plane at the target position
        this.yOrigin = yOrigin;
        
        // Store the list of hits with the instance, for comparing different instances later
        hits = new ArrayList<KalHit>(hitList.size());
        for (KalHit hit : hitList) {
            hits.add(hit);
        }

        // Fit a straight line in the non-bending plane and a parabola in the bending plane
        // Don't allocate space for debugging arrays unless debugging is really happening
        if (debug) {
            System.out.format("Entering SeedTrack, yOrigin=%10.7f\n", yOrigin);
            for (KalHit hit : hitList) hit.print(" in SeedTrack ");
            xMC = new double[hitList.size()]; // Global x coordinates of measurements (bending plane)
            zMC = new double[hitList.size()]; // Global z coordinates of measurements (along B field direction)
            yMC = new double[hitList.size()];
            mTrue = new double[hitList.size()];
        }
        // reallocate the static working arrays only if necessary
        if (hitList.size() != nHits) {
            nHits = hitList.size();
            y = new double[nHits]; // Global y coordinates of measurements (along beam direction)
            v = new double[nHits]; // Measurement value (i.e. position of the hit strip)
            s = new double[nHits]; // Uncertainty in the measurement (spatial resolution of the SSD)
            vpred = new double[nHits];
            delta = new double[nHits][3];
            R2 = new double[nHits][3];
            M = new double[5][5];
            Mint = new DMatrixRMaj(5);
            A = new double[5][5];
            B = new double[5];
            a = new DMatrixRMaj(5);
        }
        
        int N = 0;
        Nbending = 0;
        Nnonbending = 0;

        // First find the average field
        Vec Bvec = new Vec(0., 0., 0.);
        for (KalHit pnt : hitList) {
            SiModule thisSi = pnt.module;
            Vec thisB = KalmanInterface.getField(thisSi.toGlobal(new Vec(0., 0., 0.)), thisSi.Bfield);
            Bvec = Bvec.sum(thisB);
        }
        double sF = 1.0 / ((double) nHits);
        Bvec = Bvec.scale(sF);
        Bavg = Bvec.mag();
        if (debug) { System.out.format("*** SeedTrack: nHits=%d, Bavg=%10.5e\n", nHits, Bavg); }
        double c = 2.99793e8; // Speed of light in m/s
        alpha = 1000.0 * 1.0e9 / (c * Bavg); // Convert from pt in GeV to curvature in mm

        N = 0;
        for (KalHit itr : hitList) {
            SiModule thisSi = itr.module;
            if (!thisSi.isStereo) {
                Nnonbending++;
            } else {
                Nbending++;
            }
            Measurement m = itr.hit;
            Vec pnt = new Vec(0., m.v, 0.);
            pnt = thisSi.toGlobal(pnt);
            if (debug) {
                if (thisSi.isStereo) {
                    System.out.format("Layer %d detector %d, Stereo measurement %d = %10.7f\n", thisSi.Layer, thisSi.detector, N,
                            m.v);
                } else {
                    System.out.format("Layer %d detector %d, Axial measurement %d = %10.7f\n", thisSi.Layer, thisSi.detector, N,
                            m.v);
                }
                pnt.print("point global");
            }
            y[N] = pnt.v[1] - yOrigin;
            if (debug) {
                xMC[N] = m.rGlobal.v[0];
                yMC[N] = m.rGlobal.v[1] - yOrigin;
                zMC[N] = m.rGlobal.v[2];
                mTrue[N] = m.vTrue;
            }
            v[N] = m.v;
            s[N] = m.sigma;
            for (int j = 0; j < 3; j++) {
                if (j == 1) {
                    delta[N][j] = thisSi.p.X().v[j] - yOrigin;
                } else {
                    delta[N][j] = thisSi.p.X().v[j];
                }
                R2[N][j] = thisSi.Rinv.M[1][j];
            }
            N++;
        }
        if (Nnonbending < 2) {
            System.out.format("SeedTrack: not enough points in the non-bending plane; N=%d\n", Nnonbending);
            success = false;
            return;
        }
        if (Nbending < 3) {
            System.out.format("SeedTrack: not enough points in the bending plane; N=%d\n", Nbending);
            success = false;
            return;
        }
        if (debug) {
            System.out.format("SeedTrack: data in global coordinates: y, yMC, zMC, xMC, m, mTrue, check, sigma, R2[0..2]\n");
            for (int i = 0; i < N; i++) {
                double vcheck = R2[i][0] * (xMC[i] - delta[i][0]) + R2[i][2] * (zMC[i] - delta[i][2]) + R2[i][1] * (yMC[i] - delta[i][1]);
                System.out.format("%d  %10.6f  %10.6f  %10.6f  %10.6f   %10.6f   %10.6f   %10.6f   %10.6f %10.7f %10.7f %10.7f\n", i,
                        y[i], yMC[i], zMC[i], xMC[i], v[i], mTrue[i], vcheck, s[i], R2[i][0], R2[i][1], R2[i][2]);
            }
        }

        // Here we do the 5-parameter linear fit:
        LinearHelixFit(N, y, v, s, delta, R2);
        if (debug) printFit(N, xMC, y, zMC, v, s);

        // Now, derive the helix parameters and covariance from the two fits
        double sgn = -1.0; // Careful with this sign--->selects the correct half of the circle! B in +z direction only
        Vec coef = new Vec(a.unsafe_get(2,0), a.unsafe_get(3,0), a.unsafe_get(4,0)); // Parabola coefficients

        double[] circleParams = parabolaToCircle(sgn, coef);
        phi0 = circleParams[1];
        K = circleParams[2];
        drho = circleParams[0];
        double dphi0da = (2.0 * coef.v[2] / coef.v[1]) * square(FastMath.sin(phi0));
        double dphi0db = FastMath.sin(phi0) * FastMath.cos(phi0) / coef.v[1] - square(FastMath.sin(phi0));
        double dphi0dc = (2.0 * coef.v[0] / coef.v[1]) * square(FastMath.sin(phi0));
        double temp = xc * FastMath.tan(phi0) / FastMath.cos(phi0);

        double slope = a.unsafe_get(1,0);
        double intercept = a.unsafe_get(0,0);
        tanl = slope * FastMath.cos(phi0);
        dz = intercept + drho * tanl * FastMath.tan(phi0);
        if (debug) {
            System.out.format("SeedTrack: dz=%12.5e   tanl=%12.5e\n", dz, tanl);
            System.out.format("     R=%10.6f, xc=%10.6f, yc=%10.6f\n", R, xc, yc);
            System.out.format("     phi0=%10.7f,  K=%10.6f,   drho=%10.6f\n", phi0, K, drho);
        }
        // Some derivatives to transform the covariance from line and parabola
        // coefficients to helix parameters
        M[0][2] = 1.0 / FastMath.cos(phi0) + temp * dphi0da;
        M[0][3] = -(coef.v[1] / (2.0 * coef.v[2])) / FastMath.cos(phi0) + temp * dphi0db;
        M[0][4] = -(sgn + (1.0 - 0.5 * coef.v[1] * coef.v[1]) / FastMath.cos(phi0)) / (2.0 * coef.v[2] * coef.v[2]) + temp * dphi0dc;
        M[1][2] = dphi0da;
        M[1][3] = dphi0db;
        M[1][4] = dphi0dc;
        M[2][4] = -2.0 * alpha * sgn;
        M[3][0] = 1.0;
        M[3][1] = drho * FastMath.sin(phi0);
        M[4][1] = FastMath.cos(phi0);
        DMatrixRMaj D = new DMatrixRMaj(M);
        CommonOps_DDRM.multTransB(CovA,D,Mint);
        C = new DMatrixRMaj(5);
        CommonOps_DDRM.mult(D, Mint, C);
        if (debug) { 
            System.out.println("line/parabola to helix derivatives:");
            D.print();
        }

        // Note that the non-bending plane is assumed to be y,z (B field in z
        // direction), and the track is assumed to start out more-or-less
        // in the y direction, so that phi0 should be close to zero. phi0 at 90 degrees
        // will give trouble here!

        // Rotate the result into the frame of the B field at the specified origin
        KalHit itr = hitList.get(0);
        SiModule firstSi = itr.module;
        Vec firstB = KalmanInterface.getField(new Vec(0., yOrigin, 0.), firstSi.Bfield);
        if (Math.abs(firstB.v[1] / firstB.v[2]) > 0.002) {
            Vec zhat = firstB.unitVec();
            Vec yhat = new Vec(0., 1., 0.);
            Vec xhat = (yhat.cross(zhat)).unitVec();
            yhat = zhat.cross(xhat);
            RotMatrix Rot = new RotMatrix(xhat, yhat, zhat);

            hParm = rotateHelix(new Vec(drho, phi0, K, dz, tanl), Rot);
            if (debug) {
                firstB.print("Seedtrack, B field");
                Rot.print("Seedtrack, rotation matrix");
                hParm.print("Seedtrack, rotated helix");
            }
        } else {
            hParm = new Vec(drho, phi0, K, dz, tanl);
            if (debug) { hParm.print("Seedtrack, rotated helix"); }
        }

        success = true;
    }

    private double square(double x) {
        return x * x;
    }

    Vec helixParams() { // Return the fitted helix parameters
        return hParm;
    }

    DMatrixRMaj covariance() { // Return covariance matrix of the fitted helix parameters
        return C;
    }

    Vec solution() { // Return the 5 polynomial coefficients
        return new Vec(a.unsafe_get(0, 0),a.unsafe_get(1, 0),a.unsafe_get(2, 0),a.unsafe_get(3, 0),a.unsafe_get(4, 0));
    }

    double B() { // Return the average field
        return Bavg;
    }

    SquareMatrix solutionCovariance() { // Return covariance of the polynomial coefficients
        SquareMatrix CM = new SquareMatrix(5);
        for (int i=0; i<5; ++i) {
            for (int j=0; j<5; ++j) {
                CM.M[i][j] = CovA.unsafe_get(i,j);
            }
        }
        return CM;
    }

    Vec solutionErrors() { // Return errors on the polynomial coefficients (for testing)
        return new Vec(FastMath.sqrt(CovA.unsafe_get(0,0)), FastMath.sqrt(CovA.unsafe_get(1,1)), FastMath.sqrt(CovA.unsafe_get(2,2)), 
                FastMath.sqrt(CovA.unsafe_get(3,3)), FastMath.sqrt(CovA.unsafe_get(4,4)));
    }

    Vec errors() { // Return errors on the helix parameters
        return new Vec(FastMath.sqrt(C.unsafe_get(0,0)), FastMath.sqrt(C.unsafe_get(1,1)), FastMath.sqrt(C.unsafe_get(2,2)), 
                FastMath.sqrt(C.unsafe_get(3,3)), FastMath.sqrt(C.unsafe_get(4,4)));
    }

    private double[] parabolaToCircle(double sgn, Vec coef) { // Utility to convert from parabola coefficients to circle
                                                              // (i.e. helix)
                                                              // parameters drho, phi0, and K
        R = -sgn / (2.0 * coef.v[2]);
        yc = sgn * R * coef.v[1];
        xc = coef.v[0] - sgn * R * (1.0 - 0.5 * coef.v[1] * coef.v[1]);
        double[] r = new double[3];
        r[1] = FastMath.atan2(yc, xc);
        if (R < 0.) { r[1] += Math.PI; }
        if (r[1] > Math.PI) r[1] -= 2.0 * Math.PI;
        r[2] = alpha / R;
        r[0] = xc / FastMath.cos(r[1]) - R;
        if (debug) {
            System.out.format("parabolaToCircle:     R=%10.6f, xc=%10.6f, yc=%10.6f, drho=%10.7f, phi0=%10.7f, K=%10.7f\n", R, xc, yc, r[0],
                    r[1], r[2]);
            coef.print("parabola coefficients");
            double phi02 = FastMath.atan(-coef.v[1] / (2.0 * coef.v[0] * coef.v[2] + (1.0 - coef.v[1] * coef.v[1])));
            System.out.format("phi02 = %10.7f\n", phi02);
        }
        return r;
    }

    // Transformation of a helix from one B-field frame to another, by rotation R
    private Vec rotateHelix(Vec a, RotMatrix R) {
        // a = 5 helix parameters
        // R = 3 by 3 rotation matrix
        // The rotation is easily applied to the momentum vector, so first we transform
        // from helix parameters
        // to momentum, apply the rotation, and then transform back to helix parameters.
        Vec p_prime = R.rotate(HelixState.aTOp(a));
        double Q = Math.signum(a.v[2]);
        Vec a_prime = pTOa(p_prime, Q);
        return new Vec(a.v[0], a_prime.v[0], a_prime.v[1], a.v[3], a_prime.v[2]);
    }

    // Transform from momentum at helix starting point back to the helix parameters
    private static Vec pTOa(Vec p, Double Q) {
        double phi0 = FastMath.atan2(-p.v[0], p.v[1]);
        double K = Q / FastMath.sqrt(p.v[0] * p.v[0] + p.v[1] * p.v[1]);
        double tanl = p.v[2] / FastMath.sqrt(p.v[0] * p.v[0] + p.v[1] * p.v[1]);
        if (phi0 > Math.PI) phi0 = phi0 - 2.0 * Math.PI;
        // Note: the following only makes sense when a.v[0] and a.v[3] (drho and dz) are
        // both zero, i.e. pivot is on the helix
        return new Vec(phi0, K, tanl);
    }

    // Comparator function for sorting seed tracks by curvature
    static Comparator<SeedTrack> curvatureComparator = new Comparator<SeedTrack>() {
        public int compare(SeedTrack t1, SeedTrack t2) {
            double K1 = Math.abs(t1.helixParams().v[2]);
            double K2 = Math.abs(t2.helixParams().v[2]);
            if (K1 > K2) {
                return 1;
            } else {
                return -1;
            }
        }
    };

    // Comparator function for sorting seeds by distance from origin in x,z plane at the target
    static Comparator<SeedTrack> dRhoComparator = new Comparator<SeedTrack>() {
        public int compare(SeedTrack t1, SeedTrack t2) {
            Vec pInt1 = t1.planeIntersection(t1.p0);
            Vec pInt2 = t2.planeIntersection(t2.p0);

            Double pInt1_mag = new Double(pInt1.mag());
            Double pInt2_mag = new Double(pInt2.mag());
                        
            return pInt1_mag.compareTo(pInt2_mag);          
        }
    };

    //Check if two seeds are compatible given a certain relative threshold
    boolean isCompatibleTo(SeedTrack st, double rel_eps) {
        Vec st_hp = st.helixParams();
        boolean compatible = true;
        for (int i=0; i<5; i++) {
            if (Math.abs((st_hp.v[i] - hParm.v[i])/hParm.v[i]) > rel_eps) {
                compatible = false;
                break;
            }
        }
        return compatible;
    }
    
    Vec planeIntersection(Plane p) {
        double arg = (K / alpha) * ((drho + (alpha / K)) * FastMath.sin(phi0) - (p.X().v[1] - yOrigin));
        double phiInt = -phi0 + FastMath.asin(arg);
        return atPhi(phiInt);
    }

    private Vec atPhi(double phi) { // point on the helix at the angle phi
        double x = (drho + (alpha / K)) * FastMath.cos(phi0) - (alpha / K) * FastMath.cos(phi0 + phi);
        double y = yOrigin + (drho + (alpha / K)) * FastMath.sin(phi0) - (alpha / K) * FastMath.sin(phi0 + phi);
        double z = dz - (alpha / K) * phi * tanl;
        return new Vec(x, y, z);
    }

    double[] pivotTransform(double[] pivot) {
        Vec X0 = new Vec(0., yOrigin, 0.);
        double xC = X0.v[0] + (drho + alpha / K) * FastMath.cos(phi0); // Center of the helix circle
        double yC = X0.v[1] + (drho + alpha / K) * FastMath.sin(phi0);

        // Transformed helix parameters
        double[] aP = new double[5];
        aP[2] = K;
        aP[4] = tanl;
        if (K > 0) {
            aP[1] = FastMath.atan2(yC - pivot[1], xC - pivot[0]);
        } else {
            aP[1] = FastMath.atan2(pivot[1] - yC, pivot[0] - xC);
        }
        aP[0] = (xC - pivot[0]) * FastMath.cos(aP[1]) + (yC - pivot[1]) * FastMath.sin(aP[1]) - alpha / K;
        aP[3] = X0.v[2] - pivot[2] + dz - (alpha / K) * (aP[1] - phi0) * tanl;

        return aP;
    }
    
    private void LinearHelixFit(int N, double[] y, double[] v, double[] s, double[][] delta, double[][] R2) {
        // N = number of measurement points (detector layers) to fit. This must be no less than 5, with at least 2 axial and 2 stereo
        // y = nominal location of each measurement plane along the beam axis
        // v = measurement value in the detector coordinate system, perpendicular to the strips
        // s = one sigma error estimate on each measurement
        // delta = offset of the detector coordinate system from the global system (minus the nominal y value along the beam axis)
        // R2 = 2nd row of the general rotation from the global system to the local detector system

        A[0][0] = 0.;
        A[0][1] = 0.;
        A[0][2] = 0.;
        A[0][3] = 0.;
        A[0][4] = 0.;
        A[1][1] = 0.;
        A[1][4] = 0.;
        A[2][2] = 0.;
        A[2][3] = 0.;
        A[2][4] = 0.;
        A[3][4] = 0.;
        A[4][4] = 0.;
        B[0] = 0.;
        B[1] = 0.;
        B[2] = 0.;
        B[3] = 0.;
        B[4] = 0.;
        for (int i = 0; i < N; i++) {
            double R10 = R2[i][0];
            double R11 = R2[i][1];
            double R12 = R2[i][2];
            double w = 1.0 / (s[i] * s[i]);

            A[0][0] += w * R12 * R12;
            A[0][1] += w * y[i] * R12 * R12;
            A[0][2] += w * R12 * R10;
            A[0][3] += w * y[i] * R12 * R10;
            A[0][4] += w * y[i] * y[i] * R12 * R10;
            A[1][1] += w * y[i] * y[i] * R12 * R12;
            A[1][4] += w * y[i] * y[i] * y[i] * R12 * R10;
            A[2][2] += w * R10 * R10;
            A[2][3] += w * y[i] * R10 * R10;
            A[2][4] += w * y[i] * y[i] * R10 * R10;
            A[3][4] += w * y[i] * y[i] * y[i] * R10 * R10;
            A[4][4] += w * y[i] * y[i] * y[i] * y[i] * R10 * R10;
            double vcorr = (v[i] + R10 * delta[i][0] + R12 * delta[i][2] + R11 * (delta[i][1] - y[i]));
            B[0] += vcorr * R12 * w;
            B[1] += vcorr * y[i] * R12 * w;
            B[2] += vcorr * R10 * w;
            B[3] += vcorr * y[i] * R10 * w;
            B[4] += vcorr * y[i] * y[i] * R10 * w;
        }
        A[1][0] = A[0][1];
        A[1][2] = A[0][3];
        A[1][3] = A[0][4];
        A[2][0] = A[0][2];
        A[2][1] = A[1][2];
        A[3][0] = A[0][3];
        A[3][1] = A[1][3];
        A[3][2] = A[2][3];
        A[3][3] = A[2][4];
        A[4][0] = A[0][4];
        A[4][1] = A[1][4];
        A[4][2] = A[2][4];
        A[4][3] = A[3][4];

        CovA = new DMatrixRMaj(A);
        DMatrixRMaj V = new DMatrixRMaj(B);
        
        if (debug) {
            System.out.format("SeedTrack:LinearHelixFit, A matrix= ");
            CovA.print();
            System.out.format("SeedTrack:LinearHelixFit, B vector= ");
            V.print();
        }

        // Do the calculation
        CommonOps_DDRM.invert(CovA);
        CommonOps_DDRM.mult(CovA,V,a);

        // Add up the chi-squared
        chi2 = 0.;
        for (int i = 0; i < N; i++) {
            double R10 = R2[i][0];
            double R11 = R2[i][1];
            double R12 = R2[i][2];
            vpred[i] = R10 * (evaluateParabola(y[i]) - delta[i][0]) + R12 * (evaluateLine(y[i]) - delta[i][2]) + R11 * (y[i] - delta[i][1]);
            //   R10 -0.000146  ePyi -4.094654  deltai0 -22.794523  R12 -0.999999 eLyi 22.174849  deltai2 32.839151  R11 -0.001407  yi 912.877681  deltai1 912.892707
            //System.out.printf("  R10 %f  ePyi %f  deltai0 %f  R12 %f eLyi %f  deltai2 %f  R11 %f  yi %f  deltai1 %f \n", R10, evaluateParabola(y[i]), delta[i][0], R12, evaluateLine(y[i]), delta[i][2], R11, y[i], delta[i][1]);
            double err = (v[i] - vpred[i]) / s[i];
            chi2 += err * err;
        }
    }
    
    private double evaluateLine(double y) {
        return a.unsafe_get(0,0) + a.unsafe_get(1,0) * y;
    }

    private double evaluateParabola(double y) {
        return a.unsafe_get(2,0) + (a.unsafe_get(3,0) + a.unsafe_get(4,0) * y) * y;
    }
    
    private void printFit(int N, double[] x, double[] y, double[] z, double[] v, double[] s) {
        System.out.format("LinearHelixFit: parabola a=%10.7f   b=%10.7f   c=%10.7f\n", a.get(2,0), a.get(3,0), a.get(4,0));
        System.out.format("LinearHelixFit:     line a=%10.7f   b=%10.7f\n", a.get(0,0), a.get(1,0));
        System.out.println("LinearHelixFit covariance:");
        CovA.print();
        System.out.format(
                "LinearHelixFit: i  xMC   xpred       y           zMC       zpred        v    v predicted   residual   sigmas       chi^2=%8.3f\n",
                chi2);
        for (int i = 0; i < N; i++) {
            double xpred = evaluateParabola(y[i]);
            double zpred = evaluateLine(y[i]);
            System.out.format("        %d   %10.7f %10.7f %10.7f %10.7f %10.7f %10.7f %10.7f %10.7f %10.7f\n", i, x[i], xpred, y[i], z[i],
                    zpred, v[i], vpred[i], v[i] - vpred[i], (v[i] - vpred[i]) / s[i]);
        }
    }
}
