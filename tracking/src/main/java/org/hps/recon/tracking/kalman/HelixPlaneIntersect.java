package kalman;

class HelixPlaneIntersect { // Calculates intersection of a helix with an arbitrary plane

    Plane p;
    Vec a;
    Vec X0;
    private double h;
    private double c;
    double alpha;
    private FieldMap fM;
    
    HelixPlaneIntersect() {
        c = 2.99793e8;
        h = 1.0;
    }
    
    // Expensive Runge Kutta integration extrapolation to the plane through a non-uniform field
    // When close to the plane, then a helix is used to find the exact intersection
    Vec rkIntersect(Plane P, Vec X0, Vec P0, double Q, FieldMap fM) {
        // Find the straight-line distance to the plane for starters
        Vec r = X0.dif(P.X());
        double dPerp = Math.abs(r.dot(P.T()));
        Vec pHat = P0.unitVec();
        double distance = dPerp / pHat.dot(P.T());
        if (distance < 0.) {
            System.out.format("HelixPlaneIntersect:rkIntersect: there will be no intersection. distance=%12.5f\n", distance);
            return X0;
        }

        RungeKutta4 rk4 = new RungeKutta4(Q, h, fM);
        double[] d = rk4.integrate(X0, P0, distance);
        Vec X1 = new Vec(d[0], d[1], d[2]);
        Vec P1 = new Vec(d[3], d[4], d[5]);
        //X1.print("point close to the plane in rkIntersect");

        // Transform to the local B-field reference frame at this location
        Vec B = fM.getField(X1);
        double Bmag = B.mag();
        this.alpha = 1.0e12 / (c * Bmag);
        Vec t = B.unitVec(Bmag);
        Vec yhat = new Vec(0., 1.0, 0.);
        Vec u = yhat.cross(t).unitVec();
        Vec v = t.cross(u);
        RotMatrix R = new RotMatrix(u, v, t);
        Vec P1local = R.rotate(P1);
        Vec X1local = new Vec(0., 0., 0.);
        Vec helix = pToHelix(X1local, P1local, Q);
        Plane pLocal = P.toLocal(R, X1);

        //helix.print("helix parameters close to the plane in rkIntersect");
        double phiInt = planeIntersect(helix, X1local, alpha, pLocal); // helix intersection
        if (Double.isNaN(phiInt)) {
            System.out.format("HelixPlaneIntersect:rkIntersect: there is no intersection.\n");
            return X0;
        }
        //System.out.format("HelixPlaneIntersect:rkIntersect, delta-phi to the intersection is %12.5e\n", phiInt);
        Vec xInt = atPhi(phiInt);
        return R.inverseRotate(xInt).sum(X1); // return value in global coordinates
    }

    // Given the momentum and charge at a location, return the parameters of the helix,
    // assuming a reference frame in which the magnetic field is in the z direction!
    // The new pivot point is the location provided, so rho0 and z0 will always be
    // zero.
    Vec pToHelix(Vec x, Vec p, double Q) {
        double E = p.mag();
        Vec t = p.unitVec(E);
        double tanl = t.v[2] / Math.sqrt(1.0 - t.v[2] * t.v[2]);
        double pt = E / Math.sqrt(1.0 + tanl * tanl);
        double K = Q / pt;
        double phi0 = Math.atan2(-t.v[0], t.v[1]);
        return new Vec(0., phi0, K, 0., tanl);
    }

    // Find the intersection of a helix with a plane.
    double planeIntersect(Vec a, Vec pivot, double alpha, Plane p) { // Plane p is assumed to be defined in the local helix reference frame
        /*
         * if (verbose) { System.out.format("StateVector.planeIntersect:\n"); pIn.print("of measurement global");
         * p.print("of measurement local"); a.print("helix parameters"); X0.print("pivot"); //Rot.print("from global to local coordinates");
         * //origin.print("origin of local coordinates"); System.out.format(" alpha=%10.7f, radius=%10.5f\n", alpha, alpha/a.v[2]); }
         */
        // Take as a starting guess the solution for the case that the plane orientation
        // is exactly y-hat.
        //System.out.format("HelixPlaneIntersection:planeIntersect, alpha=%f10.5\n", alpha);
        this.alpha = alpha;
        double arg = (a.v[2] / alpha) * ((a.v[0] + (alpha / a.v[2])) * Math.sin(a.v[1]) - (p.X().v[1] - pivot.v[1]));
        double phi0 = -a.v[1] + Math.asin(arg);
        // if (verbose) System.out.format(" StateVector.planeIntersect: arg=%10.7f,
        // phi=%10.7f\n", arg, phi0);
        this.a = a;
        this.X0 = pivot;
        this.p = p;
        if (Double.isNaN(phi0) || p.T().v[1] == 1.0) {
            return phi0;
        }

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
            System.out.format("ZeroF.rtsafe: initial guess needs to be bracketed\n");
            return xGuess;
        }
        fl = S(x1);
        fh = S(x2);
        if ((fl > 0.0 && fh > 0.0) || (fl < 0.0 && fh < 0.0)) {
            System.out.format("ZeroFind.rtsafe: root is not bracketed in zero finding, fl=%12.5e, fh=%12.5e, alpha=%10.6f\n", fl, fh, alpha);
            // p.print("internal plane");
            // a.print("internal helix parameters");
            // X0.print("internal pivot");
            return xGuess;
        }
        if (fl == 0.)
            return x1;
        if (fh == 0.)
            return x2;
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
                if (xl == rts)
                    return rts;
            } else {
                dxold = dx;
                dx = f / df; // Newton-Raphson method
                temp = rts;
                rts -= dx;
                if (temp == rts)
                    return rts;
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
        Vec dXdPhi = new Vec((alpha / a.v[2]) * Math.sin(a.v[1] + phi), -(alpha / a.v[2]) * Math.cos(a.v[1] + phi),
                                        -(alpha / a.v[2]) * a.v[4]);
        return p.T().dot(dXdPhi);
    }

    private double S(double phi) {
        return (atPhi(phi).dif(p.X())).dot(p.T());
    }

    private Vec atPhi(double phi) { // point on the helix at the angle phi
        double x = X0.v[0] + (a.v[0] + (alpha / a.v[2])) * Math.cos(a.v[1]) - (alpha / a.v[2]) * Math.cos(a.v[1] + phi);
        double y = X0.v[1] + (a.v[0] + (alpha / a.v[2])) * Math.sin(a.v[1]) - (alpha / a.v[2]) * Math.sin(a.v[1] + phi);
        double z = X0.v[2] + a.v[3] - (alpha / a.v[2]) * phi * a.v[4];
        return new Vec(x, y, z);
    }

}
