package kalman;

//State vector (projected, filtered, or smoothed) for the Kalman filter
class StateVector {

    int kUp; // Last site index for which information is used in this state vector
    int kLow; // Index of the site for the present pivot (lower index on a in the formalism)
    Vec a; // Helix parameters at this site, relevant only in the local site coordinates
    Vec X0; // Pivot point of this site; reference point for these helix parameters, in
            // local site coordinates
    RotMatrix Rot; // Rotation from the global coordinates to the local site coordinates aligned
                   // with B field on z axis
    Vec origin; // Origin of the local site coordinates in the global system.
    SquareMatrix C; // Helix covariance matrix at this site
    double mPred; // Filtered or smoothed predicted measurement at site kLow
    double r; // Predicted, filtered, or smoothed residual at site kLow
    double R; // Covariance of residual
    boolean verbose;
    SquareMatrix F; // Propagator matrix to propagate from this site to the next site
    private double B; // Field magnitude
    double alpha; // Conversion from 1/K to radius R
    private HelixPlaneIntersect hpi;
    private double c;

    // Constructor for the initial state vector used to start the Kalman filter.
    StateVector(int site, Vec helixParams, SquareMatrix Cov, Vec pivot, double B, Vec t, Vec origin, boolean verbose) {
        if (verbose) System.out.format("StateVector: constructing an initial state vector\n");
        this.verbose = verbose;
        a = helixParams.copy();
        X0 = pivot.copy();
        this.origin = origin.copy();
        this.B = B;
        c = 2.99793e8; // Speed of light in m/s
        alpha = 1.0e12 / (c * B); // Convert from pt in GeV to curvature in mm
        if (verbose) System.out.format("Creating state vector with alpha=%12.4e\n", alpha);
        kLow = site;
        kUp = kLow;
        C = Cov.copy();
        hpi = new HelixPlaneIntersect();
        Vec yhat = new Vec(0., 1.0, 0.);
        Vec u = yhat.cross(t).unitVec();
        Vec v = t.cross(u);
        Rot = new RotMatrix(u, v, t);
    }

    // Constructor for a new blank state vector with a new B field
    StateVector(int site, double B, Vec t, Vec origin, boolean verbose) {
        // System.out.format("Creating state vector with alpha=%12.4e\n", alpha);
        kLow = site;
        c = 2.99793e8; // Speed of light in m/s
        alpha = 1000.0 * 1.0e9 / (c * B); // Convert from pt in GeV to curvature in mm
        this.B = B;
        hpi = new HelixPlaneIntersect();
        this.verbose = verbose;
        this.origin = origin.copy();
        Vec yhat = new Vec(0., 1.0, 0.);
        Vec u = yhat.cross(t).unitVec();
        Vec v = t.cross(u);
        Rot = new RotMatrix(u, v, t);
    }

    // Constructor for a new completely blank state vector
    StateVector(int site, boolean verbose) {
        kLow = site;
        this.verbose = verbose;
    }

    // Deep copy of the state vector
    StateVector copy() {
        StateVector q = new StateVector(kLow, verbose);
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
        System.out.format(">>>Dump of state vector %s %d  %d, B=%10.7f Tesla\n", s, kUp, kLow, B);
        origin.print("origin of local coordinates");
        Rot.print("from global to field coordinates");
        X0.print("pivot point in local coordinates");
        this.toGlobal(X0).print("pivot point in global coordinates");
        a.print("helix parameters");
        helixErrors().print("helix parameter errors");
        C.print("for the helix covariance");
        if (F != null) F.print("for the propagator");
        double sigmas;
        if (R > 0.) {
            sigmas = r / Math.sqrt(R);
        } else {
            sigmas = 0.;
        }
        System.out.format("Predicted measurement=%10.6f, residual=%10.7f, covariance of residual=%12.4e, std. devs. = %12.4e\n", mPred, r, R, sigmas);
        System.out.format("End of dump of state vector %s %d  %d<<<\n", s, kUp, kLow);
    }

    // Create a predicted state vector by propagating a given helix to a measurement site
    StateVector predict(int newSite, Vec pivot, double B, Vec t, Vec origin, double XL, double deltaE) {
        // newSite = index of the new site
        // pivot = pivot point of the new site in the local coordinates of this state vector
        // B and t = magnitude and direction of the magnetic field at the pivot point, in global coordinates
        // XL = thickness of the scattering material
        // deltaE = energy loss in the scattering material
        // point (makes drho and dz zero)
        StateVector aPrime = new StateVector(newSite, B, t, origin, verbose);
        aPrime.kUp = kUp;

        double E = a.v[2] * Math.sqrt(1.0 + a.v[4] * a.v[4]);
        double deltaEoE = deltaE / E;

        // Transform helix in old coordinate system to new pivot point lying on the next detector plane
        if (deltaE == 0.) {
            aPrime.a = this.pivotTransform(pivot);
        } else {
            aPrime.a = this.pivotTransform(pivot, deltaEoE);
        }
        // if (verbose) aPrime.a.print("pivot transformed helix; should have zero drho and dz");

        F = this.makeF(aPrime.a); // Calculate derivatives of the pivot transform
        if (deltaE != 0.) {
            double factor = 1.0 - deltaEoE;
            for (int i = 0; i < 5; i++) {
                F.M[i][2] *= factor;
            }
        }

        // Transform to the coordinate system of the field at the new site
        // Locate the new pivot on the helix at phi=0 (so drho and dz are zero)
        aPrime.X0 = pivot; // old pivot before helix rotation
        aPrime.X0 = aPrime.toLocal(this.toGlobal(aPrime.atPhi(0.))); // new pivot after helix rotation
        RotMatrix Rt = aPrime.Rot.multiply(this.Rot.invert());
        SquareMatrix fRot = new SquareMatrix(5);
        if (verbose) {
            aPrime.Rot.print("aPrime rotation matrix");
            this.Rot.print("this rotation matrix");
            Rt.print("rotation from old local frame to new local frame");
            aPrime.a.print("StateVector:predict helix before rotation");
        }
        aPrime.a = this.rotateHelix(aPrime.a, Rt, fRot); // Derivative matrix fRot gets filled in here
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
            for (int i = 0; i < 5; i++)
                aPda.a.v[i] = a.v[i] * (1.0 + daRel[i]);
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
                System.out.format("Test of F: Helix parameter %d, deltaExact=%10.8f, delta=%10.8f\n", i, deltaExact, delta);
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
        } else {
            double momentum = (1.0 / a.v[2]) * Math.sqrt(1.0 + a.v[4] * a.v[4]);
            double sigmaMS = (0.0136 / Math.abs(momentum)) * Math.sqrt(XL) * (1.0 + 0.038 * Math.log(XL));
            if (verbose) {
                System.out.format("StateVector.predict: momentum=%12.5e, sigmaMS=%12.5e\n", momentum, sigmaMS);
            }
            Ctot = this.C.sum(this.getQ(sigmaMS));
        }

        // Now propagate the multiple scattering matrix and covariance matrix to the new site
        aPrime.C = Ctot.similarity(F);

        return aPrime;
    }

    // Create a filtered state vector from a predicted state vector
    StateVector filter(Vec H, double V) {

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
            aPrime.C.print("filtered covariance in StateVector.filter");
            // Alternative calculation of filtered covariance (sanity check that it gives the same result):
            SquareMatrix D = C.invert().sum(H.scale(1.0 / V).product(H));
            SquareMatrix Calt = D.invert();
            Calt.print("alternate filtered covariance in StateVector.filter");
            aPrime.C.multiply(D).print("unit matrix??");
        }

        return aPrime;
    }

    // Create a state vector by removing a hit from an existing state vector
    // **** Note---not sure that this is working correctly; not currently used *****
    StateVector inverseFilter(Vec H, double V) {

        StateVector aPrime = copy();

        double denom = -V + H.dot(H.leftMultiply(C));
        Vec Kstar = H.leftMultiply(C).scale(1.0 / denom); // Kalman gain matrix
        if (verbose) {
            System.out.format("StateVector.inverseFilter: V=%12.4e,  denom=%12.4e\n", V, denom);
            Kstar.print("Kalman gain matrix in StateVector.inverseFilter");
            H.print("matrix H in StateVector.inverseFilter");
        }

        aPrime.a = a.sum(Kstar.scale(r));
        SquareMatrix U = new SquareMatrix(5, 1.0);
        aPrime.C = (U.dif(Kstar.product(H))).multiply(C);

        return aPrime;
    }

    // Create a smoothed state vector from the filtered state vector
    StateVector smooth(StateVector snS, StateVector snP) {
        if (verbose) {
            System.out.format("StateVector.smooth of filtered state %d %d, using smoothed state %d %d and predicted state %d %d\n", kLow, kUp, snS.kLow,
                                            snS.kUp, snP.kLow, snP.kUp);
        }
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
    Vec atPhi(double phi) {
        double x = X0.v[0] + (a.v[0] + (alpha / a.v[2])) * Math.cos(a.v[1]) - (alpha / a.v[2]) * Math.cos(a.v[1] + phi);
        double y = X0.v[1] + (a.v[0] + (alpha / a.v[2])) * Math.sin(a.v[1]) - (alpha / a.v[2]) * Math.sin(a.v[1] + phi);
        double z = X0.v[2] + a.v[3] - (alpha / a.v[2]) * phi * a.v[4];
        return new Vec(x, y, z);
    }

    Vec atPhi(Vec X0, Vec a, double phi, double alpha) {
        double x = X0.v[0] + (a.v[0] + (alpha / a.v[2])) * Math.cos(a.v[1]) - (alpha / a.v[2]) * Math.cos(a.v[1] + phi);
        double y = X0.v[1] + (a.v[0] + (alpha / a.v[2])) * Math.sin(a.v[1]) - (alpha / a.v[2]) * Math.sin(a.v[1] + phi);
        double z = X0.v[2] + a.v[3] - (alpha / a.v[2]) * phi * a.v[4];
        return new Vec(x, y, z);
    }

    // Returns the particle momentum at the helix angle phi
    Vec getMom(double phi) {
        double px = -Math.sin(a.v[1] + phi) / Math.abs(a.v[2]);
        double py = Math.cos(a.v[1] + phi) / Math.abs(a.v[2]);
        double pz = a.v[4] / Math.abs(a.v[2]);
        return new Vec(px, py, pz);
    }

    // Calculate the phi angle to propagate on helix to the intersection with a measurement plane
    double planeIntersect(Plane pIn) { // pIn is assumed to be defined in the global reference frame
        Plane p = pIn.toLocal(Rot, origin); // Transform the plane into the B-field local reference frame
        return hpi.planeIntersect(a, X0, alpha, p);
    }

    // Multiple scattering matrix; assume a single thin scattering layer at the beginning of the helix propagation
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

    // Transform the helix covariance to new pivot point (specified in local coordinates)
    SquareMatrix covariancePivotTransform(Vec aP) {
        // aP are the helix parameters for the new pivot point, assumed already to be calculated by pivotTransform()
        // Note that no field rotation is assumed or accounted for here
        SquareMatrix mF = makeF(aP);
        return C.similarity(mF);
    }

    // Transform the helix to a pivot back at the global origin
    Vec pivotTransform() {
        Vec pivot = origin.scale(-1.0);
        return pivotTransform(pivot);
    }

    // Pivot transform of the state vector, from the current pivot to the pivot in the argument (specified in local coordinates)
    Vec pivotTransform(Vec pivot) {
        return pivotTransform(pivot, a, X0, 0.);
    }

    // Pivot transform including energy loss just before
    Vec pivotTransform(Vec pivot, double deltaEoE) {
        return pivotTransform(pivot, a, X0, deltaEoE);
    }

    Vec pivotTransform(Vec pivot, Vec a, Vec X0, double deltaEoE) {
        double K = a.v[2] * (1.0 - deltaEoE); // Lose energy before propagating
        double xC = X0.v[0] + (a.v[0] + alpha / K) * Math.cos(a.v[1]); // Center of the helix circle
        double yC = X0.v[1] + (a.v[0] + alpha / K) * Math.sin(a.v[1]);
        // if (verbose) System.out.format("pivotTransform center=%10.6f, %10.6f\n", xC, yC);

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

        // xC = pivot[0] + (aP[0]+alpha/aP[2])*Math.cos(aP[1]);
        // yC = pivot[1] + (aP[0]+alpha/aP[2])*Math.sin(aP[1]);
        // if (verbose) System.out.format("pivotTransform new center=%10.6f, %10.6f\n", xC, yC);

        return new Vec(5, aP);
    }

    // Propagate a helix by Runge-Kutta itegration to an x,z plane containing the origin.
    public Vec propagateRungeKutta(FieldMap fM, SquareMatrix newCovariance) {

        // boolean verbose = true;

        Vec B = fM.getField(new Vec(0., 0., 0.)); // B field at the origin
        double Bmag = B.mag();
        double alphaOrigin = 1.0e12 / (c * Bmag);
        Vec tB = B.unitVec(Bmag);
        if (verbose) System.out.format("    At origin B=%10.5f, t=%10.6f %10.6f %10.6f\n", Bmag, tB.v[0], tB.v[1], tB.v[2]);
        Vec yhat = new Vec(0., 1.0, 0.);
        Vec uB = yhat.cross(tB).unitVec();
        Vec vB = tB.cross(uB);
        RotMatrix originRot = new RotMatrix(uB, vB, tB); // Rotation from the global system into the B-field system at the origin
        Plane originPlane = new Plane(new Vec(0., 0., 0.), originRot.rotate(new Vec(0., 1., 0.))); // Plane in the B-field coordinate system at the origin

        // Point and momentum on the helix in the B-field system at the first tracking layer
        Vec xLocal = atPhi(0.);
        Vec pLocal = getMom(0.);

        // Position and momentum in the origin B-field system
        Vec X0origin = originRot.rotate(Rot.inverseRotate(xLocal).sum(origin));
        Vec P0origin = originRot.rotate(Rot.inverseRotate(pLocal));
        double Q = Math.signum(a.v[2]);

        Vec pInt = new Vec(3);
        Vec Xplane = hpi.rkIntersect(originPlane, X0origin, P0origin, Q, fM, pInt); // RK propagation to the origin plane

        Vec helixAtIntersect = pTOa(pInt, 0., 0., Q);
        Vec helixAtOrigin = pivotTransform(new Vec(0., 0., 0.), helixAtIntersect, Xplane, 0.);
        if (verbose) {
            System.out.format("\nStateVector.propagateRungeKutta, Q=%8.1f, origin=%10.5f %10.5f %10.5f:\n", Q, origin.v[0], origin.v[1], origin.v[2]);
            System.out.format("    alpha=%10.6f,  alpha at origin=%10.6f\n", alpha, alphaOrigin);
            X0.print("helix pivot");
            a.print("local helix parameters at layer 1");
            xLocal.print("point on helix, local at layer 1");
            pLocal.print("helix momentum, local at layer 1");
            X0origin.print("point on helix, origin system, at layer 1");
            P0origin.print("helix momentum, origin system global at layer 1");
            Xplane.print("RK helix intersection with origin plane");
            pInt.print("RK momentum at helix intersection");
            helixAtIntersect.print("helix at origin-plane intersection");
            helixAtOrigin.print("helix with pivot at origin");
        }

        // The covariance matrix is transformed assuming a simple pivot transform (not Runge Kutta)

        RotMatrix Rt = originRot.multiply(Rot.invert()); // Rotation from 1 B-field frame to another
        SquareMatrix fRot = new SquareMatrix(5);
        Vec rotatedHelix = rotateHelix(a, Rt, fRot);
        Vec X0global = toGlobal(X0);
        Vec X0originSystem = originRot.rotate(X0global);
        Vec dummyHelix = pivotTransform(new Vec(0., 0., 0.), rotatedHelix, X0originSystem, 0.);
        SquareMatrix F = makeF(dummyHelix, a, alphaOrigin);
        SquareMatrix Ft = F.multiply(fRot);
        newCovariance.M = (C.similarity(Ft)).M;
        if (verbose) {
            rotatedHelix.print("rotated helix");
            fRot.print("rotation of helix derivative matrix");
            X0global.print("original helix pivot in the global system");
            X0originSystem.print("the same pivot in the origin B-field system");
            dummyHelix.print("original helix pivot transformed to the origin");
            F.print("pivot transform derivative matrix");
            Ft.print("full derivative matrix");
            C.print("old covariance");
            newCovariance.print("new covariance");
            System.out.format("Exiting StateVector.propagateRungeKutta\n\n");
        }

        return helixAtOrigin;
    }

    // Derivative matrix for the pivot transform (without energy loss or field rotations)
    private SquareMatrix makeF(Vec aP) {
        return makeF(aP, a, alpha);
    }

    // Version of makeF that allows a different starting helix to be provided
    private SquareMatrix makeF(Vec aP, Vec a, double alpha) {
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
        f[3][2] = (alpha / (a.v[2] * a.v[2])) * a.v[4] * (aP.v[1] - a.v[1] - (alpha / a.v[2]) * Math.sin(aP.v[1] - a.v[1]) / (aP.v[0] + alpha / a.v[2]));
        f[3][3] = 1.0;
        f[3][4] = -(alpha / a.v[2]) * (aP.v[1] - a.v[1]);
        f[4][4] = 1.0;

        return new SquareMatrix(5, f);
    }

    // Momentum at the start of the given helix (point closest to the pivot)
    Vec aTOp(Vec a) {
        double px = -Math.sin(a.v[1]) / Math.abs(a.v[2]);
        double py = Math.cos(a.v[1]) / Math.abs(a.v[2]);
        double pz = a.v[4] / Math.abs(a.v[2]);
        if (verbose) {
            a.print("helix parameters in StateVector.aTOp");
            System.out.format("StateVector.aTOp: p=%10.5f %10.5f %10.5f\n", px, py, pz);
        }
        return new Vec(px, py, pz);
    }

    // Transform from momentum at helix starting point back to the helix parameters
    Vec pTOa(Vec p, double drho, double dz, double Q) {
        double phi0 = Math.atan2(-p.v[0], p.v[1]);
        double K = Q / Math.sqrt(p.v[0] * p.v[0] + p.v[1] * p.v[1]);
        double tanl = p.v[2] / Math.sqrt(p.v[0] * p.v[0] + p.v[1] * p.v[1]);
        if (verbose) {
            System.out.format("StateVector pTOa: Q=%5.1f phi0=%10.7f K=%10.6f tanl=%10.7f\n", Q, phi0, K, tanl);
            p.print("input momentum vector in StateVector.pTOa");
        }

        return new Vec(drho, phi0, K, dz, tanl);
    }

    // To transform a space point from global to local coordinates, first subtract
    // <origin> and then rotate by <Rot>.
    Vec toLocal(Vec xGlobal) {
        Vec xLocal = Rot.rotate(xGlobal.dif(origin));
        return xLocal;
    }

    // To transform a space point from local to global coordinates, first rotate by
    // the inverse of <Rot> and then add the <origin>.
    Vec toGlobal(Vec xLocal) {
        Vec xGlobal = Rot.inverseRotate(xLocal).sum(origin);
        return xGlobal;
    }

    // Transformation of helix parameters from one B-field frame to another, by rotation R
    // Warning: the pivot point has to be transformed too! Here we assume that the new pivot point
    // will be on the helix at phi=0, so drho and dz will always be returned as zero.
    Vec rotateHelix(Vec a, RotMatrix R, SquareMatrix fRot) {
        // The rotation is easily applied to the momentum vector, so first we transform from helix parameters
        // to momentum, apply the rotation, and then transform back to helix parameters.
        // The values for fRot, the corresponding derivative matrix, are also calculated and returned.
        Vec p_prime = R.rotate(aTOp(a));

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
        if (verbose) { // derivative test
            Vec da = a.scale(0.005);
            Vec apda = a.sum(da);
            Vec ap = pTOa(R.rotate(aTOp(a)), a);
            Vec apdap = pTOa(R.rotate(aTOp(apda)), apda);
            Vec dap = apdap.dif(ap);
            Vec dap2 = da.leftMultiply(fRot);
            System.out.format("StateVector:rotateHelix: derivative test:\n");
            dap.print("actual difference in helix parameters");
            dap2.print("diff in helix params from derivatives");
        }
        */
        // The parameters drho and dz, being distances between two points, do not change with the rotation.
        // Is this really true? It's not necessarily the same point on the helix, is it, since the helix has changed
        // orientation discretely.
        return pTOa(p_prime, a.v[0], a.v[3], Q);
    }

}
