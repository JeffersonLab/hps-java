package org.hps.recon.tracking.kalman;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hps.util.Pair;

//State vector (projected, filtered, or smoothed) for the Kalman filter
class StateVector {

    int kUp;            // Last site index for which information is used in this state vector
    int kLow;           // Index of the site for the present pivot (lower index on a in the formalism)
    Vec a;              // Helix parameters at this site, relevant only in the local site coordinates
    Vec X0;             // Pivot point of this site; reference point for these helix parameters, in local site coordinates
    RotMatrix Rot;      // Rotation from the global coordinates to the local field coordinates aligned
                        // with B field on z axis
    Vec origin;         // Origin of the local field coordinates in the global system.
    SquareMatrix C;     // Helix covariance matrix at this site
    double mPred;       // Filtered or smoothed predicted measurement at site kLow (filled in MeasurementSite.java)
    double r;           // Predicted, filtered, or smoothed residual at site kLow
    double R;           // Covariance of residual
    private boolean verbose;
    SquareMatrix F;     // Propagator matrix to propagate from this site to the next site
    double B;           // Field magnitude
    double alpha;       // Conversion from 1/K to radius R
    private HelixPlaneIntersect hpi;
    private double c;   // Speed of light
    private Logger logger;

    // Constructor for the initial state vector used to start the Kalman filter.
    StateVector(int site, Vec helixParams, SquareMatrix Cov, Vec pivot, double B, Vec tB, Vec origin) {
        // Here tB is the B field direction, while B is the magnitude
        logger = Logger.getLogger(StateVector.class.getName());
        logger.log(Level.FINEST, "StateVector: constructing an initial state vector\n");
        verbose = logger.getLevel()==Level.FINEST;
        a = helixParams.copy();
        X0 = pivot.copy();
        this.origin = origin.copy();
        this.B = B;
        c = 2.99793e8; // Speed of light in m/s
        alpha = 1.0e12 / (c * B); // Convert from pt in GeV to curvature in mm
        logger.log(Level.FINEST, String.format("Creating state vector with alpha=%12.4e\n", alpha));
        kLow = site;
        kUp = kLow;
        C = Cov.copy();
        hpi = new HelixPlaneIntersect();
        Vec yhat = new Vec(0., 1.0, 0.);
        Vec u = yhat.cross(tB).unitVec();
        Vec v = tB.cross(u);
        Rot = new RotMatrix(u, v, tB);
    }

    // Constructor for a new blank state vector with a new B field
    StateVector(int site, double B, Vec tB, Vec origin) {
        logger = Logger.getLogger(StateVector.class.getName());
        verbose = logger.getLevel()==Level.FINEST;
        logger.log(Level.FINEST, String.format("Creating state vector with alpha=%12.4e\n", alpha));
        kLow = site;
        c = 2.99793e8; // Speed of light in m/s
        alpha = 1000.0 * 1.0e9 / (c * B); // Convert from pt in GeV to curvature in mm
        this.B = B;
        hpi = new HelixPlaneIntersect();
        this.origin = origin.copy();
        Vec yhat = new Vec(0., 1.0, 0.);
        Vec u = yhat.cross(tB).unitVec();
        Vec v = tB.cross(u);
        Rot = new RotMatrix(u, v, tB);
    }

    // Constructor for a new completely blank state vector
    StateVector(int site) {
        kLow = site;
        logger = Logger.getLogger(StateVector.class.getName());
        verbose = logger.getLevel()==Level.FINEST;
    }

    // Deep copy of the state vector
    StateVector copy() {
        StateVector q = new StateVector(kLow);
        q.B = B;
        q.c = c;
        q.alpha = alpha;
        q.Rot = Rot.copy();
        q.kUp = kUp;
        q.a = a.copy();
        q.C = C.copy();
        if (F != null) q.F = F.copy();
        q.X0 = X0.copy();
        q.origin = origin.copy();
        q.mPred = mPred;
        q.R = R;
        q.r = r;
        q.hpi = new HelixPlaneIntersect();
        return q;
    }

    // Debug printout of the state vector
    void print(String s) {
        System.out.format("%s", this.toString(s));
    }
        
    String toString(String s) {
        String str = String.format(">>>Dump of state vector %s %d  %d, B=%10.7f Tesla\n", s, kUp, kLow, B);
        str = str + origin.toString("origin of local field coordinates") + "\n";
        str = str + Rot.toString("from global to field coordinates");
        str = str + X0.toString("pivot point in local field coordinates")+"\n";
        str = str + this.toGlobal(X0).toString("pivot point in global coordinates")+"\n";
        str = str + a.toString("helix parameters")+"\n";
        str = str + helixErrors().toString("helix parameter errors");
        str = str + C.toString("for the helix covariance");
        if (F != null) str = str + F.toString("for the propagator");
        double sigmas;
        if (R > 0.) {
            sigmas = r / Math.sqrt(R);
        } else {
            sigmas = 0.;
        }
        str = str + String.format("Predicted measurement=%10.6f, residual=%10.7f, covariance of residual=%12.4e, std. devs. = %12.4e\n", mPred, r, R,
                sigmas);
        str = str + String.format("End of dump of state vector %s %d  %d<<<\n", s, kUp, kLow);
        return str;
    }

    // Create a predicted state vector by propagating a given helix to a measurement site
    StateVector predict(int newSite, Vec pivot, double B, Vec t, Vec originPrime, double XL, double deltaE) {
        // newSite = index of the new site
        // pivot = pivot point of the new site in the local coordinates of this state vector (i.e. coordinates of the old site)
        // B and t = magnitude and direction of the magnetic field at the pivot point, in global coordinates
        // XL = thickness of the scattering material
        // deltaE = energy loss in the scattering material
        // originPrime = origin of the detector coordinates at the new site in global coordinates

        // This constructs a new blank state vector with pivot and helix parameters undefined as yet
        StateVector aPrime = new StateVector(newSite, B, t, originPrime);
        aPrime.kUp = kUp;
        aPrime.X0 = pivot; // pivot before helix rotation, in coordinate system of the previous site

        double E = a.v[2] * Math.sqrt(1.0 + a.v[4] * a.v[4]);
        double deltaEoE = deltaE / E;

        // Transform helix in old coordinate system to new pivot point lying on the next detector plane
        if (deltaE == 0.) {
            aPrime.a = this.pivotTransform(pivot);
        } else {
            aPrime.a = this.pivotTransform(pivot, deltaEoE);
        }
        if (verbose) { // drho and dz are indeed always zero here
            aPrime.a.print("StateVector predict: pivot transformed helix; should have zero drho and dz");
            a.print("old helix");
            pivot.print("new pivot");
            X0.print("old pivot");
        }

        F = this.makeF(aPrime.a); // Calculate derivatives of the pivot transform
        if (deltaE != 0.) {
            double factor = 1.0 - deltaEoE;
            for (int i = 0; i < 5; i++) { F.M[i][2] *= factor; }
        }

        // Transform to the coordinate system of the field at the new site
        // First, transform the pivot point to the new system
        aPrime.X0 = aPrime.toLocal(this.toGlobal(aPrime.X0));

        // Calculate the matrix for the net rotation from the old site coordinates to the new site coordinates
        RotMatrix Rt = aPrime.Rot.multiply(this.Rot.invert());
        SquareMatrix fRot = new SquareMatrix(5);
        if (verbose) {
            aPrime.Rot.print("aPrime rotation matrix");
            this.Rot.print("this rotation matrix");
            Rt.print("rotation from old local frame to new local frame");
            aPrime.a.print("StateVector:predict helix before rotation");
        }

        // Rotate the helix parameters here. 
        // dz and drho will remain unchanged at zero
        // phi0 and tanl(lambda) change, as does kappa (1/pt). However, |p| should be unchanged by the rotation.
        // This call to rotateHelix also calculates the derivative matrix fRot
        aPrime.a = StateVector.rotateHelix(aPrime.a, Rt, fRot);
        if (verbose) {
            aPrime.a.print("StateVector:predict helix after rotation");
            fRot.print("fRot from StateVector:predict");
        }
        F = fRot.multiply(F);

        // Test the derivatives
        /*
        if (verbose) {
            double daRel[] = { 0.01, 0.03, -0.02, 0.05, -0.01 };
            StateVector aPda = copy();
            for (int i = 0; i < 5; i++) {
                aPda.a.v[i] = a.v[i] * (1.0 + daRel[i]);
            }
            Vec da = aPda.a.dif(a);
            StateVector aPrimeNew = copy();
            aPrimeNew.a = aPda.pivotTransform(pivot);
            RotMatrix RtTmp = Rot.invert().multiply(aPrime.Rot);
            SquareMatrix fRotTmp = new SquareMatrix(5);
            aPrimeNew.a = rotateHelix(aPrimeNew.a, RtTmp, fRotTmp);
            for (int i = 0; i < 5; i++) {
                double deltaExact = aPrimeNew.a.v[i] - aPrime.a.v[i];
                double delta = 0.;
                for (int j = 0; j < 5; j++) {
                    delta += F.M[i][j] * da.v[j];
                }
                System.out.format("Test of F: Helix parameter %d, deltaExact=%10.8f, delta=%10.8f\n", i, deltaExact,
                        delta);
            }
        }
        */

        aPrime.kLow = newSite;
        aPrime.kUp = kUp;

        // Add the multiple scattering contribution for the silicon layer
        // sigmaMS is the rms of the projected scattering angle
        SquareMatrix Ctot;
        if (XL == 0.) {
            Ctot = this.C;
            logger.log(Level.FINEST, String.format("StateVector.predict: XL=%9.6f", XL));
        } else {
            double momentum = (1.0 / a.v[2]) * Math.sqrt(1.0 + a.v[4] * a.v[4]);
            double sigmaMS = projMSangle(momentum, XL);
            logger.log(Level.FINEST, String.format("StateVector.predict: momentum=%12.5e, XL=%9.6f sigmaMS=%12.5e", momentum, XL, sigmaMS));
            Ctot = this.C.sum(this.getQ(sigmaMS));
        }

        // Now propagate the multiple scattering matrix and covariance matrix to the new site
        aPrime.C = Ctot.similarity(F);

        // Temporary test of fine stepping the pivot transform (for uniform field)
        // Verified that taking multiple steps gives the same result for both the helix parameters
        // and the covariance matrix compared to making a single step, for uniform field.
        /*
        int Ntr = 5;
        Vec delta = (pivot.dif(X0)).scale(1.0/(double)Ntr);
        Vec oldPivot = X0;
        Vec oldHelix = a;
        System.out.format("Fine stepping test:\n");
        pivot.print("target new pivot");
        delta.print("delta");
        oldPivot.print("oldPivot");
        oldHelix.print("oldHelix");
        SquareMatrix oldC = Ctot;
        oldC.print("old C");
        for (int itr=0; itr<Ntr; itr++) {
            Vec newPivot = oldPivot.sum(delta);
            Vec newHelix = StateVector.pivotTransform(newPivot, oldHelix, oldPivot, alpha, 0.);
            SquareMatrix Ftmp = StateVector.makeF(newHelix, oldHelix, alpha);
            SquareMatrix newC = oldC.similarity(Ftmp);
            System.out.format("Fine stepping test itr=%d\n", itr);
            newPivot.print("newPivot");
            newHelix.print("newHelix");
            newC.print("newC");
            oldPivot = newPivot;
            oldHelix = newHelix;
            oldC = newC;
        }
        aPrime.a.print("transformed helix");
        aPrime.C.print("transformed covariance");
        */
        return aPrime;
    }

    // Create a filtered state vector from a predicted state vector
    StateVector filter(Vec H, double V) {
        // H = prediction matrix (5-vector)
        // V = hit variance (1/sigma^2)

        StateVector aPrime = copy();
        aPrime.kUp = kLow;

        double denom = V + H.dot(H.leftMultiply(C));
        Vec K = H.leftMultiply(C).scale(1.0 / denom); // Kalman gain matrix
        if (verbose) {
            System.out.format("StateVector.filter: kLow=%d\n", kLow);
            System.out.format("StateVector.filter: V=%12.4e,  denom=%12.4e\n", V, denom);
            K.print("Kalman gain matrix in StateVector.filter");
            H.print("matrix H in StateVector.filter");
            System.out.format("StateVector.filter: k dot H = %10.7f\n", K.dot(H));
            // Alternative calculation of K (sanity check that it gives the same result):
            SquareMatrix D = C.invert().sum(H.scale(1.0 / V).product(H));
            Vec Kalt = H.scale(1.0 / V).leftMultiply(D.invert());
            Kalt.print("alternate Kalman gain matrix");
        }

        aPrime.a = a.sum(K.scale(r));
        SquareMatrix U = new SquareMatrix(5, 1.0);
        aPrime.C = (U.dif(K.product(H))).multiply(C);

        if (verbose) {
            aPrime.C.print("filtered covariance (gain-matrix formalism) in StateVector.filter");
            // Alternative calculation of filtered covariance (sanity check that it gives
            // the same result):
            SquareMatrix D = C.invert().sum(H.scale(1.0 / V).product(H));
            SquareMatrix Calt = D.invert();
            Calt.print("alternate (weighted-means formalism) filtered covariance in StateVector.filter");
            aPrime.C.multiply(D).print("unit matrix??");
            a.print("predicted helix parameters");
            aPrime.a.print("filtered helix parameters (gain matrix formalism)");
        }
        //double R = (1 - H.dot(K))*V;
        //System.out.format("StateVector.filter: R=%10.8f\n", R);

        return aPrime;
    }

    // Modify the state vector by removing the hit information
    Vec inverseFilter(Vec H, double V, SquareMatrix Cnew) {
        double denom = -V + H.dot(H.leftMultiply(C));
        Vec Kstar = H.leftMultiply(C).scale(1.0 / denom); // Kalman gain matrix

        Vec aNew = a.sum(Kstar.scale(r));
        SquareMatrix U = new SquareMatrix(5, 1.0);
        SquareMatrix Cstar = (U.dif(Kstar.product(H))).multiply(C);
        if (verbose) {
            System.out.format("StateVector.inverseFilter: V=%12.4e,  denom=%12.4e\n", V, denom);
            Kstar.print("Kalman gain matrix in StateVector.inverseFilter");
            H.print("matrix H in StateVector.inverseFilter");
            a.print("old helix");
            aNew.print(" new helix in StateVector.inverseFilter");
            C.print("old covariance");
            Cnew.print(" new covariance in StateVector.inverseFilter");
        }
        Cnew.M = Cstar.M;
        return aNew;
    }

    // Create a smoothed state vector from the filtered state vector
    StateVector smooth(StateVector snS, StateVector snP) {
        logger.log(Level.FINEST, String.format("StateVector.smooth of filtered state %d %d, using smoothed state %d %d and predicted state %d %d", kLow, kUp,
                    snS.kLow, snS.kUp, snP.kLow, snP.kUp));
        StateVector sS = this.copy();

        SquareMatrix CnInv = snP.C.invert();
        SquareMatrix A = (C.multiply(sS.F.transpose())).multiply(CnInv);

        Vec diff = snS.a.dif(snP.a);
        sS.a = a.sum(diff.leftMultiply(A));

        SquareMatrix Cdiff = snS.C.dif(snP.C);
        sS.C = C.sum(Cdiff.similarity(A));

        return sS;
    }

    // Returns a point on the helix at the angle phi
    // Warning: the point returned is in the B-Field reference frame
    Vec atPhi(double phi) {
        return atPhi(X0, a, phi, alpha);
        // double x = X0.v[0] + (a.v[0] + (alpha / a.v[2])) * Math.cos(a.v[1]) - (alpha
        // / a.v[2]) * Math.cos(a.v[1] + phi);
        // double y = X0.v[1] + (a.v[0] + (alpha / a.v[2])) * Math.sin(a.v[1]) - (alpha
        // / a.v[2]) * Math.sin(a.v[1] + phi);
        // double z = X0.v[2] + a.v[3] - (alpha / a.v[2]) * phi * a.v[4];
        // return new Vec(x, y, z);
    }

    static Vec atPhi(Vec X0, Vec a, double phi, double alpha) {
        double x = X0.v[0] + (a.v[0] + (alpha / a.v[2])) * Math.cos(a.v[1]) - (alpha / a.v[2]) * Math.cos(a.v[1] + phi);
        double y = X0.v[1] + (a.v[0] + (alpha / a.v[2])) * Math.sin(a.v[1]) - (alpha / a.v[2]) * Math.sin(a.v[1] + phi);
        double z = X0.v[2] + a.v[3] - (alpha / a.v[2]) * phi * a.v[4];
        return new Vec(x, y, z);
    }

    // Returns the particle momentum at the helix angle phi
    // Warning! This is returned in the B-Field coordinate system.
    Vec getMom(double phi) {
        return getMom(phi, a);
    }

    static Vec getMom(double phi, Vec a) {
        double px = -Math.sin(a.v[1] + phi) / Math.abs(a.v[2]);
        double py = Math.cos(a.v[1] + phi) / Math.abs(a.v[2]);
        double pz = a.v[4] / Math.abs(a.v[2]);
        return new Vec(px, py, pz);
    }

    // Calculate the phi angle to propagate on helix to the intersection with a
    // measurement plane
    double planeIntersect(Plane pIn) { // pIn is assumed to be defined in the global reference frame
        Plane p = pIn.toLocal(Rot, origin); // Transform the plane into the B-field local reference frame
        return hpi.planeIntersect(a, X0, alpha, p);
    }

    // Multiple scattering matrix; assume a single thin scattering layer at the
    // beginning of the helix propagation
    private SquareMatrix getQ(double sigmaMS) {
        double[][] q = new double[5][5];

        double V = sigmaMS * sigmaMS;
        q[1][1] = V * (1.0 + a.v[4] * a.v[4]);
        q[2][2] = 0.; // V*(a.v[2]*a.v[2]*a.v[4]*a.v[4]); // These commented terms would be relevant
                      // for a scatter halfway in between planes
        q[2][4] = 0.; // V*(a.v[2]*a.v[4]*(1.0+a.v[4]*a.v[4]));
        q[4][2] = 0.; // q[2][4];
        q[4][4] = V * (1.0 + a.v[4] * a.v[4]) * (1.0 + a.v[4] * a.v[4]);
        // All other elements are zero

        return new SquareMatrix(5, q);
    }

    // Return errors on the helix parameters at the global origin
    Vec helixErrors(Vec aPrime) {
        // aPrime are the helix parameters for a pivot at the global origin, assumed
        // already to be calculated by pivotTransform()
        SquareMatrix tC = covariancePivotTransform(aPrime);
        return new Vec(Math.sqrt(tC.M[0][0]), Math.sqrt(tC.M[1][1]), Math.sqrt(tC.M[2][2]), Math.sqrt(tC.M[3][3]), Math.sqrt(tC.M[4][4]));
    }

    // Return errors on the helix parameters at the present pivot point
    Vec helixErrors() {
        return new Vec(Math.sqrt(C.M[0][0]), Math.sqrt(C.M[1][1]), Math.sqrt(C.M[2][2]), Math.sqrt(C.M[3][3]), Math.sqrt(C.M[4][4]));
    }

    // Transform the helix covariance to new pivot point (specified in local
    // coordinates)
    SquareMatrix covariancePivotTransform(Vec aP) {
        // aP are the helix parameters for the new pivot point, assumed already to be
        // calculated by pivotTransform()
        // Note that no field rotation is assumed or accounted for here
        SquareMatrix mF = makeF(aP);
        return C.similarity(mF);
    }

    // Transform the helix to a pivot back at the global origin
    Vec pivotTransform() {
        Vec pivot = origin.scale(-1.0);
        return pivotTransform(pivot);
    }

    // Pivot transform of the state vector, from the current pivot to the pivot in
    // the argument (specified in local coordinates)
    Vec pivotTransform(Vec pivot) {
        return pivotTransform(pivot, a, X0, alpha, 0.);
    }

    // Pivot transform including energy loss just before
    Vec pivotTransform(Vec pivot, double deltaEoE) {
        return pivotTransform(pivot, a, X0, alpha, deltaEoE);
    }

    static Vec pivotTransform(Vec pivot, Vec a, Vec X0, double alpha, double deltaEoE) {
        double K = a.v[2] * (1.0 - deltaEoE); // Lose energy before propagating
        double xC = X0.v[0] + (a.v[0] + alpha / K) * Math.cos(a.v[1]); // Center of the helix circle
        double yC = X0.v[1] + (a.v[0] + alpha / K) * Math.sin(a.v[1]);
        // if (verbose) System.out.format("pivotTransform center=%13.10f, %13.10f\n",
        // xC, yC);

        // Predicted state vector
        double[] aP = new double[5];
        aP[2] = K;
        aP[4] = a.v[4];
        if (K > 0) {
            aP[1] = Math.atan2(yC - pivot.v[1], xC - pivot.v[0]);
        } else {
            aP[1] = Math.atan2(pivot.v[1] - yC, pivot.v[0] - xC);
        }
        aP[0] = (xC - pivot.v[0]) * Math.cos(aP[1]) + (yC - pivot.v[1]) * Math.sin(aP[1]) - alpha / K;
        aP[3] = X0.v[2] - pivot.v[2] + a.v[3] - (alpha / K) * (aP[1] - a.v[1]) * a.v[4];

        // xC = pivot.v[0] + (aP[0]+alpha/aP[2])*Math.cos(aP[1]);
        // yC = pivot.v[1] + (aP[0]+alpha/aP[2])*Math.sin(aP[1]);

        return new Vec(5, aP);
    }

    Vec propagateRungeKutta(Plane pln, org.lcsim.geometry.FieldMap fM, SquareMatrix newCovariance, double XL) {
        ArrayList<Double> yScat = new ArrayList<Double>();
        return propagateRungeKutta(pln, fM, newCovariance, yScat, XL);
    }
    
    // Propagate a helix by Runge-Kutta integration to an arbitrary plane
    Vec propagateRungeKutta(Plane pln, org.lcsim.geometry.FieldMap fM, SquareMatrix newCovariance, ArrayList<Double> yScat, double XL) {
        // pln = plane to where the extrapolation is taking place in global coordinates.  
        // The origin of pln will be the new helix pivot point.
        // fM  = HPS field map
        // newCovariance = output helix covariance at the new pivot
        // return value = helix parameters at the new pivot
        // yScat = input array of y values where scattering in silicon will take place. Only those between the start and finish points
        //         will be used, so including extras will just waste a bit of CPU time
        // XL    = silicon thickness in radiation lengths
        //boolean verbose = true;
        
        Vec B = KalmanInterface.getField(pln.X(), fM);
        double Bmag = B.mag();
        double alphaOrigin = 1.0e12 / (c * Bmag);
        Vec tB = B.unitVec(Bmag);
        Vec yhat = new Vec(0., 1.0, 0.);
        Vec uB = yhat.cross(tB).unitVec();
        Vec vB = tB.cross(uB);
        RotMatrix originRot = new RotMatrix(uB, vB, tB); // Rotation from the global system into the B-field system at the plane
        
        // Target plane after rotation to the B-field coordinate system 
        Plane originPlane = new Plane(originRot.rotate(pln.X()), originRot.rotate(pln.T()));        
        
        // Point and momentum on the helix in the B-field system near the tracking layer from where we extrapolate
        Vec xLocal = atPhi(0.);
        Vec pLocal = getMom(0.);

        // Position and momentum in the target B-field system
        Vec X0origin = originRot.rotate(Rot.inverseRotate(xLocal).sum(origin));
        Vec P0origin = originRot.rotate(Rot.inverseRotate(pLocal));
        double Q = Math.signum(a.v[2]);

        Vec pInt = new Vec(3);
        Vec Xplane = hpi.rkIntersect(originPlane, X0origin, P0origin, Q, fM, pInt); // RK propagation to the target plane
        Vec helixAtIntersect = pTOa(pInt, 0., 0., Q);
        Vec helixAtOrigin = pivotTransform(originPlane.X(), helixAtIntersect, Xplane, alpha, 0.);
        if (verbose) {
            System.out.format("\nStateVector.propagateRungeKutta, Q=%8.1f, origin=%10.5f %10.5f %10.5f:\n", Q, origin.v[0], origin.v[1],
                    origin.v[2]);
            System.out.format("    At final plane B=%10.5f, t=%10.6f %10.6f %10.6f\n", Bmag, tB.v[0], tB.v[1], tB.v[2]);
            System.out.format("    alpha=%10.6f,  alpha at final plane=%10.6f\n", alpha, alphaOrigin);
            Rot.invert().print("from local system at layer 1 to global");
            originRot.print("to Bfield system at final plane");
            originPlane.print("at origin in B-field system");
            origin.print("origin of local detector system in global coordinates");
            X0.print("helix pivot");
            a.print("local helix parameters at layer 1");
            xLocal.print("point on helix, local at layer 1");
            pLocal.print("helix momentum, local at layer 1");
            X0origin.print("point on helix, origin system, at layer 1");
            P0origin.print("helix momentum, origin system global at layer 1");
            Xplane.print("RK helix intersection with final plane");
            Vec Xglob = originRot.inverseRotate(Xplane);
            Xglob.print("intersection with final plane in global coordinates");
            pInt.print("RK momentum at helix intersection");
            helixAtIntersect.print("helix at final-plane intersection");
            helixAtOrigin.print("helix with pivot at final plane");
        }

        // The covariance matrix is transformed assuming a sequence of pivot transforms (not Runge Kutta)
        Vec transHelix = new Vec(5);
        double stepSize = 25.0;
        // Step from XL to pln.X(), both in global coordinates
        if (!helixStepper(stepSize, yScat, XL, newCovariance, transHelix, pln.X(), fM)) {
            for (int i=0; i<5; ++i) {
                for (int j=0; j<5; ++j) {
                    newCovariance.M[i][j] = this.C.M[i][j];
                }
            }
        } else if (verbose) {
            transHelix.print("helixStepper helix at final plane");
            C.print("original covariance");
            System.out.println("    Errors: ");
            for (int i = 0; i < 5; ++i) { System.out.format(" %10.7f", Math.sqrt(C.M[i][i])); }
            System.out.println("\n");
            newCovariance.print("transformed covariance");
            System.out.println("    Errors: ");
            for (int i = 0; i < 5; ++i) { System.out.format(" %10.7f", Math.sqrt(newCovariance.M[i][i])); }
            System.out.println("\n");
        }
               
        return helixAtOrigin;
    }
    
    // Propagate a helix by Runge-Kutta integration to an x,z plane containing the origin.
    Vec propagateRungeKutta(org.lcsim.geometry.FieldMap fM, SquareMatrix newCovariance, double XL) {
      
        // Plane in the B-field coordinate system at the origin
        Plane originPlane = new Plane(new Vec(0., 0., 0.), new Vec(0., 1., 0.)); 
 
        return propagateRungeKutta(originPlane, fM, newCovariance, XL);
    }

    static double projMSangle(double p, double XL) {
        return (0.0136 / Math.abs(p)) * Math.sqrt(XL) * (1.0 + 0.038 * Math.log(XL));
    }

    boolean helixStepper(double maxStep, ArrayList<Double> yScat, double XL, SquareMatrix Covariance, Vec finalHelix, Vec newOrigin, org.lcsim.geometry.FieldMap fM) {
        // The old and new origin points are in global coordinates. The old helix and old pivot are defined
        // in a coordinate system aligned with the field and centered at the old origin. The returned
        // helix will be in a coordinate system aligned with the local field at the new origin, and the
        // new pivot point will be at the new origin.
        
        // All scattering layers are assumed to be of the same thickness XL, in radiation lengths

        // We assume that the starting StateVector is at a layer with a hit, in which case the Kalman filter has already accounted for
        // multiple scattering at that layer.       
                
        //boolean verbose = true;
        
        double tol = 1.0;  // Tolerance in mm to determine whether a location is on a scattering plane
        int nSteps = (int)(Math.abs(newOrigin.v[1] - this.origin.v[1])/maxStep);
        if (nSteps < 1) nSteps = 1;
        ArrayList<Pair<Double,Boolean>> stepPnts = new ArrayList<Pair<Double,Boolean>>(nSteps+yScat.size());
        double yDistance = newOrigin.v[1] - this.origin.v[1];
        double yStep = yDistance/(double)nSteps;
        
        //stepPnts.add(new Pair<Double,Boolean>(this.origin.v[1],false));
        double yNext = this.origin.v[1];
        double dir = Math.signum(yStep);
        for (int i=0; i<nSteps; ++i) {
            double yLast = yNext;
            yNext += yStep;
            boolean XLnext = false;
            for (double y : yScat) {
                if (dir*y > dir*yLast + tol && dir*y < dir*yNext - tol) { // Add intermediate scattering layer
                    Pair<Double,Boolean> newLayer = new Pair<Double,Boolean>(y,true);
                    stepPnts.add(newLayer);
                } else if (y >= yNext - tol && y <= yNext + tol) {
                    XLnext = true;
                }
            }
            stepPnts.add(new Pair<Double,Boolean>(yNext,XLnext));         
        }
        if (dir > 0.) {
            Collections.sort(stepPnts, StateVector.pairComparator);
        } else {
            Collections.sort(stepPnts, StateVector.pairComparator.reversed());
        }
        
        double localAlpha = alpha;

        RotMatrix RM = Rot;
        Vec newHelix = this.a.copy();
        Vec Pivot = this.X0.copy();
        Vec Origin = this.origin.copy(); // In global coordinates
        SquareMatrix fRot = new SquareMatrix(5);
        SquareMatrix Cov = this.C;
        Vec yhat = new Vec(0., 1., 0.);
        if (verbose) {
            System.out.format("Entering helixStepper for %d steps, B=%10.7f, B direction=%10.7f %10.7f %10.7f\n", 
                    stepPnts.size(), B, RM.M[2][0],RM.M[2][1], RM.M[2][2]);
            for (Pair<Double,Boolean> step : stepPnts) {
                double y = step.getFirstElement();
                boolean ifScat = step.getSecondElement();
                System.out.format("  stepping layer at y=%10.5f, scatter = %b\n", y, ifScat);
            }
            this.origin.print("old origin");
            this.X0.print("old pivot");
            this.a.print("old helix");
            Cov.print("old helix covariance");
            newOrigin.print("new origin");
            RM.print("to transform to the local field frame");
            Plane pln = new Plane(new Vec(0., 0., 0.), yhat);
            Plane plnLocal = pln.toLocal(RM, Origin);
            double dphi = hpi.planeIntersect(newHelix, Pivot, localAlpha, plnLocal); // Find the helix intersection with the plane
            Vec newPivot = atPhi(Pivot, newHelix, dphi, localAlpha);
            newPivot.print("new pivot in origin plane");
            Vec newHelix0 = pivotTransform(newPivot, newHelix, Pivot, localAlpha, 0.);
            newHelix0.print("Pivot transform to final plane in a single step");
        }
        for (int step = 0; step < stepPnts.size(); ++step) {
            Pair<Double, Boolean> thisStep = stepPnts.get(step);
            double yInt = thisStep.getFirstElement();
            boolean thisXL = thisStep.getSecondElement();
            Plane pln = new Plane(new Vec(0., yInt, 0.), yhat); // Make a plane in global coordinates, perpendicular to the y axis
            Plane plnLocal = pln.toLocal(RM, Origin); // Transform the plane to local coordinates
            double dphi = hpi.planeIntersect(newHelix, Pivot, localAlpha, plnLocal); // Find the helix intersection with the plane
            if (Double.isNaN(dphi)) {
                logger.log(Level.WARNING, String.format("No intersection with the plane at step=%d\n", step));
                return false;
            }
            Vec newPivot = atPhi(Pivot, newHelix, dphi, localAlpha);
            Vec newHelixPivoted = pivotTransform(newPivot, newHelix, Pivot, localAlpha, 0.); // Transform the helix pivot to the intersection point
            if (verbose) {
                System.out.format("Step %d, y=%8.3f, dphi=%10.7f, alpha=%10.7e\n", step, yInt, dphi, localAlpha);
                Pivot.print("old pivot");
                newPivot.print("new pivot at intersection of helix with plane");
                // pln.print("target");
                // plnLocal.print("transformed to local coordinates");
                newHelix.print("new helix after pivot transform");
                dphi = hpi.planeIntersect(newHelix, newPivot, localAlpha, plnLocal);
                System.out.format("    New delta-phi=%13.10f; should be zero!\n", dphi);
                Vec newPoint = atPhi(newPivot, newHelix, dphi, localAlpha);
                newPoint.print("new point of intersection, should be same as the old");
                Cov.print("covariance before transform");
            }
            SquareMatrix F = makeF(newHelixPivoted, newHelix, localAlpha);
            newHelix = newHelixPivoted;

            // Rotate the helix into the field system at the new origin
            Origin = RM.inverseRotate(newPivot).sum(Origin); // Make a new coordinate system with origin at the intersection point
            Vec Bfield = KalmanInterface.getField(Origin, fM);
            double Bmag = Bfield.mag();
            localAlpha = 1.0e12 / c / Bmag;
            Vec tB = Bfield.unitVec(Bmag); // Local field at the new origin
            Vec uB = yhat.cross(tB).unitVec();
            Vec vB = tB.cross(uB);
            RotMatrix RMnew = new RotMatrix(uB, vB, tB);
            RotMatrix deltaRM = RMnew.multiply(RM.invert());    // New coordinate system is rotated to align with local field
            newHelix = rotateHelix(newHelix, deltaRM, fRot);    // Rotate the helix into the new local field coordinate system
            SquareMatrix Ft = F.multiply(fRot);              
            Cov = Cov.similarity(Ft);                           // Here we propagate the covariance matrix
            // Add in multiple scattering if we are here passing through a plane with material
            if (thisXL) {
                SquareMatrix Q = null;
                Vec pMom = getMom(0.,newHelix);
                double momentum = pMom.mag();
                double ct = pMom.v[1]/momentum;
                double sigmaMS = projMSangle(momentum, XL/ct);
                Q = this.getQ(sigmaMS);
                Cov = Cov.sum(Q);
            }

            // In the next step the pivot will be right at the origin of the local system
            Pivot.v[0] = 0.;
            Pivot.v[1] = 0.;
            Pivot.v[2] = 0.;
            if (verbose) {
                System.out.format("  helixStepper after step %d, B=%10.7f, B direction=%10.7f %10.7f %10.7f\n", step, Bmag, tB.v[0], tB.v[1],
                        tB.v[2]);
                RMnew.print("new rotation to field frame");
                deltaRM.print("rotation to field system");
                Origin.print("intermediate origin in global system");
                newHelix.print("new helix after rotation");
                Ft.print("transform matrix");
                Cov.print("covariance after rotation");
            }
            RM = RMnew;
        }
        // Finally, move the pivot to the requested point in the final plane
        Vec newOriginLocal = RM.rotate(newOrigin.dif(Origin));
        Vec finalHx = pivotTransform(newOriginLocal, newHelix, new Vec(0., 0., 0.), localAlpha, 0.);
        SquareMatrix F = makeF(finalHx, newHelix, localAlpha);
        Cov = Cov.similarity(F);
        if (verbose) {
            Origin.print("helixStepper origin on helix");
            newOriginLocal.print("final origin in local coordinates");
            newHelix.print("helix before pivot transform");
            finalHx.print("final helix");
            Cov.print("transformed covariance");
        }
        for (int i = 0; i < 5; ++i) { 
            finalHelix.v[i] = finalHx.v[i];
            for (int j = 0; j < 5; ++j) { 
                Covariance.M[i][j] = Cov.M[i][j]; 
            } 
        }
        return true;
    }
    
    // Transform a helix from one pivot to another through a non-uniform B field in several steps
    // Deprecated original version without scattering planes
    Vec helixStepper(int nSteps, SquareMatrix Covariance, Vec newOrigin, org.lcsim.geometry.FieldMap fM) {
        double maxStep = Math.abs(newOrigin.v[1]-origin.v[1])/(double)nSteps;
        ArrayList<Double> yScats = new ArrayList<Double>();
        Vec transHelix = new Vec(5);
        double XL = 0.;
        helixStepper(maxStep, yScats, XL, Covariance, transHelix, newOrigin, fM);
        return transHelix;
    }

    // Comparator function for sorting pairs in helixStepper by y
    static Comparator<Pair<Double,Boolean>> pairComparator = new Comparator<Pair<Double,Boolean>>() {
        public int compare(Pair<Double,Boolean> p1, Pair<Double,Boolean> p2) {
            if (p1.getFirstElement() < p2.getFirstElement()) {
                return -1;
            } else {
                return +1;
            }
        }
    };
    
    // Derivative matrix for the pivot transform (without energy loss or field rotations)
    SquareMatrix makeF(Vec aP) {
        return makeF(aP, a, alpha);
    }

    // Version of makeF that allows a different starting helix to be provided
    static SquareMatrix makeF(Vec aP, Vec a, double alpha) {
        double[][] f = new double[5][5];
        f[0][0] = Math.cos(aP.v[1] - a.v[1]);
        f[0][1] = (a.v[0] + alpha / a.v[2]) * Math.sin(aP.v[1] - a.v[1]);
        f[0][2] = (alpha / (a.v[2] * a.v[2])) * (1.0 - Math.cos(aP.v[1] - a.v[1]));
        f[1][0] = -Math.sin(aP.v[1] - a.v[1]) / (aP.v[0] + alpha / a.v[2]);
        f[1][1] = (a.v[0] + alpha / a.v[2]) * Math.cos(aP.v[1] - a.v[1]) / (aP.v[0] + alpha / a.v[2]);
        f[1][2] = (alpha / (a.v[2] * a.v[2])) * Math.sin(aP.v[1] - a.v[1]) / (aP.v[0] + alpha / a.v[2]);
        f[2][2] = 1.0;
        f[3][0] = (alpha / a.v[2]) * a.v[4] * Math.sin(aP.v[1] - a.v[1]) / (aP.v[0] + alpha / a.v[2]);
        f[3][1] = (alpha / a.v[2]) * a.v[4] * (1.0 - (a.v[0] + alpha / a.v[2]) * Math.cos(aP.v[1] - a.v[1]) / (aP.v[0] + alpha / a.v[2]));
        f[3][2] = (alpha / (a.v[2] * a.v[2])) * a.v[4]
                * (aP.v[1] - a.v[1] - (alpha / a.v[2]) * Math.sin(aP.v[1] - a.v[1]) / (aP.v[0] + alpha / a.v[2]));
        f[3][3] = 1.0;
        f[3][4] = -(alpha / a.v[2]) * (aP.v[1] - a.v[1]);
        f[4][4] = 1.0;

        return new SquareMatrix(5, f);
    }

    // Momentum at the start of the given helix (point closest to the pivot)
    static Vec aTOp(Vec a) {
        double px = -Math.sin(a.v[1]) / Math.abs(a.v[2]);
        double py = Math.cos(a.v[1]) / Math.abs(a.v[2]);
        double pz = a.v[4] / Math.abs(a.v[2]);
        return new Vec(px, py, pz);
    }

    // Transform from momentum at helix starting point back to the helix parameters
    // drho and dz are not modified
    static Vec pTOa(Vec p, double drho, double dz, double Q) {
        double phi0 = Math.atan2(-p.v[0], p.v[1]);
        double K = Q / Math.sqrt(p.v[0] * p.v[0] + p.v[1] * p.v[1]);
        double tanl = p.v[2] / Math.sqrt(p.v[0] * p.v[0] + p.v[1] * p.v[1]);

        return new Vec(drho, phi0, K, dz, tanl);
    }

    // To transform a space point from global to local field coordinates, first subtract
    // <origin> and then rotate by <Rot>.
    Vec toLocal(Vec xGlobal) {
        Vec xLocal = Rot.rotate(xGlobal.dif(origin));
        return xLocal;
    }

    // To transform a space point from local field coordinates to global coordinates, first rotate by
    // the inverse of <Rot> and then add the <origin>.
    Vec toGlobal(Vec xLocal) {
        Vec xGlobal = Rot.inverseRotate(xLocal).sum(origin);
        return xGlobal;
    }

    // Transformation of helix parameters from one B-field frame to another, by rotation R
    // Warning: the pivot point has to be transformed too! Here we assume that the new pivot point
    // will be on the helix at phi=0, so drho and dz will always be returned as zero. Therefore, before
    // calling this routine, make sure that the current pivot point is on the helix (drho=dz=0)
    static Vec rotateHelix(Vec a, RotMatrix R, SquareMatrix fRot) {
        // The rotation is easily applied to the momentum vector, so first we transform from helix parameters
        // to momentum, apply the rotation, and then transform back to helix parameters.
        // The values for fRot, the corresponding derivative matrix, are also calculated and returned.

        boolean verbose = false;

        Vec phlx = aTOp(a); // Momentum at point closest to helix
        Vec p_prime = R.rotate(phlx); // Momentum in new coordinate system
        if (verbose) {
            a.print("input helix, in rotateHelix");
            R.print("in rotateHelix");
            phlx.print("momentum vector in rotateHelix");
            p_prime.print("rotated momentum vector in rotateHelix");
            System.out.format("in rotateHelix: p=%10.7f,  prot=%10.7f\n", phlx.mag(), p_prime.mag());
        }

        SquareMatrix dpda = new SquareMatrix(3);
        dpda.M[0][0] = -Math.cos(a.v[1]) / Math.abs(a.v[2]);
        dpda.M[0][1] = Math.sin(a.v[1]) / (a.v[2] * Math.abs(a.v[2]));
        dpda.M[1][0] = -Math.sin(a.v[1]) / Math.abs(a.v[2]);
        dpda.M[1][1] = -Math.cos(a.v[1]) / (a.v[2] * Math.abs(a.v[2]));
        dpda.M[2][1] = -a.v[4] / (a.v[2] * Math.abs(a.v[2]));
        dpda.M[2][2] = 1. / Math.abs(a.v[2]);

        SquareMatrix dprimedp = new SquareMatrix(3);
        for (int i = 0; i < 3; i++) { 
            for (int j = 0; j < 3; j++) { 
                dprimedp.M[i][j] = R.M[i][j]; 
            } 
        }

        double Q = Math.signum(a.v[2]);
        SquareMatrix dadp = new SquareMatrix(3);
        double pt2 = p_prime.v[0] * p_prime.v[0] + p_prime.v[1] * p_prime.v[1];
        double pt = Math.sqrt(pt2);
        dadp.M[0][0] = -p_prime.v[1] / pt2;
        dadp.M[0][1] = p_prime.v[0] / pt2;
        dadp.M[1][0] = -Q * p_prime.v[0] / (pt2 * pt);
        dadp.M[1][1] = -Q * p_prime.v[1] / (pt2 * pt);
        dadp.M[2][0] = -p_prime.v[0] * p_prime.v[2] / (pt2 * pt);
        dadp.M[2][1] = -p_prime.v[1] * p_prime.v[2] / (pt2 * pt);
        dadp.M[2][2] = 1.0 / (pt);

        SquareMatrix prod = dadp.multiply(dprimedp.multiply(dpda));

        fRot.M[0][0] = 1.0;
        fRot.M[1][1] = prod.M[0][0];
        fRot.M[1][2] = prod.M[0][1];
        fRot.M[1][4] = prod.M[0][2];
        fRot.M[2][1] = prod.M[1][0];
        fRot.M[2][2] = prod.M[1][1];
        fRot.M[2][4] = prod.M[1][2];
        fRot.M[3][3] = 1.0;
        fRot.M[4][1] = prod.M[2][0];
        fRot.M[4][2] = prod.M[2][1];
        fRot.M[4][4] = prod.M[2][2];

        /*
         * if (verbose) { // derivative test Vec da = a.scale(0.005); Vec apda =
         * a.sum(da); Vec ap = pTOa(R.rotate(aTOp(a)), a); Vec apdap =
         * pTOa(R.rotate(aTOp(apda)), apda); Vec dap = apdap.dif(ap); Vec dap2 =
         * da.leftMultiply(fRot);
         * System.out.format("StateVector:rotateHelix: derivative test:\n");
         * dap.print("actual difference in helix parameters");
         * dap2.print("diff in helix params from derivatives"); }
         */
        // The parameters drho and dz are assumed not to change in the rotation. This will be correct
        // if the rotation is about the pivot point, and the pivot is on the helix, in which case
        // dz and drho are both zero. That is not necessarily valid if dz and drho are not zero. If the
        // rotation were only about the z axis it would be, but any other sort of rotation will mix dz and drho.
        //if (Math.abs(a.v[0]) > 0.001 || Math.abs(a.v[3]) > 0.001) {
        //    System.out.format("StateVector.rotateHelix: warning, dz=%10.5f and drho=%10.5f are not zero.\n",a.v[3],a.v[0]);
        //}
        Vec aNew = pTOa(p_prime, a.v[0], a.v[3], Q);
        if (verbose) { aNew.print("rotated helix in rotateHelix"); }
        return aNew;
    }
    
}
