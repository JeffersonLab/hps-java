package org.hps.recon.tracking.kalman;

class HelixPlaneIntersect { // Calculates intersection of a helix with a nearly arbitrary plane
    // Coordinates: the beam is ~ in the y direction
    // the B field is ~ along the +z direction
    // Si detectors are ~ in the xz plane

    Plane p;
    Vec a;
    Vec X0;
    private double h;
    private double c;
    double alpha;

    HelixPlaneIntersect() {
        c = 2.99793e8;
        h = 1.0; // Integration step size. It is not optimized and can probably be set significantly larger to save time.
    }

    // Runge Kutta integration extrapolation to a plane through a non-uniform field
    // When close to the plane, then a helix is used to find the exact intersection
    Vec rkIntersect(Plane P, Vec X0, Vec P0in, double Qin, org.lcsim.geometry.FieldMap fM, Vec pInt) {
        // P definition of the plane to which to extrapolate
        // X0 3-D starting point for the extrapolation
        // P0 3-momentum at the start of the extrapolation
        // Q sign of the particle charge (+1 or -1)
        // fM HPS field map
        // pInt return value for the momentum at the intersection
        // the function return value is the 3-D intersection point

        // Find the straight-line distance to the plane for an initial guess at the distance
        Vec r = P.X().dif(X0);
        double Q = Qin;
        Vec P0 = P0in;
        boolean backwards = false;
        if (r.dot(P0) < 0.) {
            //System.out.format("HelixPlaneIntersect:rkIntersect: need to propagate backwards.\n");
            Q = -Qin;
            P0 = P0in.scale(-1.0); // Propagate the particle backwards in time
            backwards = true;
        }
        double dPerp = r.dot(P.T());
        Vec pHat = P0.unitVec();
        double distance = Math.abs(dPerp / pHat.dot(P.T()));

        RungeKutta4 rk4 = new RungeKutta4(Q, h, fM);
        //X0.print("rkIntersect start location, global coords");
        //P0.print("rkIntersect start momentum, global coords");
        double[] d = rk4.integrate(X0, P0, distance);
        Vec X1 = new Vec(d[0], d[1], d[2]);
        Vec P1 = null;
        if (backwards) {
            P1 = new Vec(-d[3], -d[4], -d[5]);
            Q = Qin;
        } else {
            P1 = new Vec(d[3], d[4], d[5]);
        }
        //X1.print("point close to the plane in rkIntersect, global coords");
        //P1.print("rkIntersect final momentum, global coords");

        // Transform to the local B-field reference frame at this location
        Vec B = KalmanInterface.getField(X1, fM);
        double Bmag = B.mag();
        this.alpha = 1.0e12 / (c * Bmag);
        Vec t = B.unitVec(Bmag);
        Vec yhat = new Vec(0., 1.0, 0.);
        Vec u = yhat.cross(t).unitVec();
        Vec v = t.cross(u);
        RotMatrix R = new RotMatrix(u, v, t);
        //System.out.format("B field at the final location=%10.6f, t=%10.8f %10.8f %10.8f\n", Bmag,t.v[0],t.v[1],t.v[2]);
        //R.print("to local B field frame");
        Vec P1local = R.rotate(P1);
        Vec X1local = new Vec(0., 0., 0.);
        //P1local.print("momentum in local B-field coords");
        //X1local.print("point on helix in local B-field system");
        Vec helix = pToHelix(X1local, P1local, Q);
        Plane pLocal = P.toLocal(R, X1);
        //helix.print("local helix");
        //pLocal.print("target plane in local B-field coords");

        // helix.print("helix parameters close to the plane in rkIntersect");
        // Note: this call to planeIntersect fills in X0 and a
        double phiInt = planeIntersect(helix, X1local, alpha, pLocal); // helix intersection
        if (Double.isNaN(phiInt)) {
            //System.out.format("HelixPlaneIntersect:rkIntersect: there is no intersection.\n");
            pInt.v[0] = P0.v[0];
            pInt.v[1] = P0.v[1];
            pInt.v[2] = P0.v[2];
            return X0;
        }
        //System.out.format("HelixPlaneIntersect:rkIntersect, delta-phi to the intersection is %12.5e\n", phiInt);
        Vec xInt = StateVector.atPhi(X1local, helix, phiInt, alpha);
        //StateVector.getMom(phiInt, helix).print("pInt local");
        Vec temp = R.inverseRotate(StateVector.getMom(phiInt, helix));
        //xInt.print("xInt, local coordinates intersection with plane");
        pInt.v[0] = temp.v[0];
        pInt.v[1] = temp.v[1];
        pInt.v[2] = temp.v[2];
        return R.inverseRotate(xInt).sum(X1); // return value in global coordinates
    }

    // Given the momentum and charge at a location, return the parameters of the helix,
    // assuming a reference frame in which the magnetic field is in the z direction!
    // The new pivot point is the location provided, so rho0 and z0 will always be
    // zero.
    static Vec pToHelix(Vec x, Vec p, double Q) {
        double E = p.mag();
        Vec t = p.unitVec(E);
        double tanl = t.v[2] / Math.sqrt(1.0 - t.v[2] * t.v[2]);
        double pt = E / Math.sqrt(1.0 + tanl * tanl);
        double K = Q / pt;
        double phi0 = Math.atan2(-t.v[0], t.v[1]);
        return new Vec(0., phi0, K, 0., tanl);
    }

    // Find the intersection of a helix with a plane.
    double planeIntersect(Vec a, Vec pivot, double alpha, Plane p) {
        // p: Plane assumed to be defined in the local helix reference frame
        // a: vector of 5 helix parameters
        // alpha: 10^12/c/B

        // Take as a starting guess the solution for the case that the plane orientation is exactly y-hat.
        // System.out.format("HelixPlaneIntersection:planeIntersect, alpha=%f10.5\n", alpha);
        this.alpha = alpha;
        double arg = (a.v[2] / alpha) * ((a.v[0] + (alpha / a.v[2])) * Math.sin(a.v[1]) - (p.X().v[1] - pivot.v[1]));
        double phi0 = -a.v[1] + Math.asin(arg);
        // if (verbose) System.out.format(" StateVector.planeIntersect: arg=%10.7f,
        // phi=%10.7f\n", arg, phi0);
        this.a = a;
        this.X0 = pivot;
        this.p = p;
        if (Double.isNaN(phi0) || p.T().v[1] == 1.0) return phi0;

        double dphi = 0.1;
        double accuracy = 0.0000001;
        // Iterative solution for a general plane orientation
        double phi = rtSafe(phi0, phi0 - dphi, phi0 + dphi, accuracy);
        // System.out.format("HelixPlaneIntersect.planeIntersect: phi0=%12.10f,phi=%12.10f\n", phi0, phi);
        return phi;
    }

    // Safe Newton-Raphson zero finding from Numerical Recipes in C
    double rtSafe(double xGuess, double x1, double x2, double xacc) {
        // Here xGuess is a starting guess for the phi angle of the helix intersection
        // x1 and x2 give a range for the value of the solution
        // xacc specifies the accuracy needed
        // The output is an accurate result for the phi of the intersection
        double df, dx, dxold, f, fh, fl;
        double temp, xh, xl, rts;
        int MAXIT = 100;

        if (xGuess <= x1 || xGuess >= x2) {
            System.out.format("HelixPlaneIntersect.rtsafe: initial guess needs to be bracketed\n");
            return xGuess;
        }
        fl = S(x1);
        fh = S(x2);
        int nTry = 0;
        while (fl*fh > 0.0) {
            if (nTry == 5) {
                //System.out.format("HelixPlaneIntersect.rtsafe: root is not bracketed in zero finding, fl=%12.5e, fh=%12.5e, alpha=%10.6f, x1=%12.5f x2=%12.5f xGuess=%12.5f\n", fl, fh, alpha, x1, x2, xGuess);
                return xGuess;
            }
            x1 -= 0.1;
            x2 += 0.1;
            fl = S(x1);
            fh = S(x2);
            nTry++;
        }
        //if (nTry > 0) System.out.format("HelixPlaneIntersect.rtsafe: %d tries needed to bracket solution.\n", nTry);
        if (fl == 0.) return x1;
        if (fh == 0.) return x2;
        if (fl < 0.0) {
            xl = x1;
            xh = x2;
        } else {
            xh = x1;
            xl = x2;
        }
        rts = xGuess;
        dxold = Math.abs(x2 - x1);
        dx = dxold;
        f = S(rts);
        df = dSdPhi(rts);
        for (int j = 1; j <= MAXIT; j++) {
            if ((((rts - xh) * df - f) * ((rts - xl) * df - f) > 0.0) || (Math.abs(2.0 * f) > Math.abs(dxold * df))) {
                dxold = dx;
                dx = 0.5 * (xh - xl); // Use bisection if the Newton-Raphson method is going bonkers
                rts = xl + dx;
                if (xl == rts) return rts;
            } else {
                dxold = dx;
                dx = f / df; // Newton-Raphson method
                temp = rts;
                rts -= dx;
                if (temp == rts) return rts;
            }
            if (Math.abs(dx) < xacc) {
                // System.out.format("ZeroFind.rtSafe: solution converged in %d iterations.\n",
                // j);
                return rts;
            }
            f = S(rts);
            df = dSdPhi(rts);
            if (f < 0.0) {
                xl = rts;
            } else {
                xh = rts;
            }
        }
        System.out.format("ZeroFind.rtsafe: maximum number of iterations exceeded.\n");
        return rts;
    }

    private double dSdPhi(double phi) {
        Vec dXdPhi = new Vec((alpha / a.v[2]) * Math.sin(a.v[1] + phi), -(alpha / a.v[2]) * Math.cos(a.v[1] + phi), -(alpha / a.v[2]) * a.v[4]);
        return p.T().dot(dXdPhi);
    }

    private double S(double phi) {
        return (StateVector.atPhi(X0, a, phi, alpha).dif(p.X())).dot(p.T());
    }
}
