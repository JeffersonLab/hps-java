package org.hps.recon.tracking.kalman;

import java.util.Comparator;

//Kalman fit measurement site, one for each silicon-strip detector that the track crosses
class MeasurementSite {
    SiModule m; // Si detector hit data
    int hitID; // hit used on the track (-1 if none)
    int thisSite; // Index of this measurement site
    StateVector aP; // Predicted state vector
    boolean predicted; // True if the predicted state vector has been built
    StateVector aF; // Filtered state vector
    boolean filtered; // True if the filtered state vector has been built
    StateVector aS; // Smoothed state vector
    boolean smoothed; // True if the smoothed state vector has been built
    double chi2inc; // chi^2 increment for this site
    Vec H; // Derivatives of the transformation from state vector to measurement
    private double conFac; // Conversion from B to alpha
    private double alpha;
    private double XL; // Thickness of the detector in radiation lengths
    private double dEdx; // in GeV/mm
    private double mxResid; // Maximum residual for adding a hit
    private double mxResidShare; // Maximum residual for a shared hit
    private boolean verbose;
    double B;

    // Note: I can remove the concept of a dummy layer and make all layers equivalent, except that the non-physical ones
    // will never have a hit and thus will be handled the same as physical layers that lack hits

    void print(String s) {
        if (m.Layer < 0) {
            System.out.format("\n****Dump of dummy measurement site %d %s;  ", thisSite, s);
        } else {
            System.out.format("\n****Dump of measurement site %d %s;  ", thisSite, s);
        }
        if (smoothed) {
            System.out.format("    This site has been smoothed\n");
        } else if (filtered) {
            System.out.format("    This site has been filtered\n");
        } else if (predicted) {
            System.out.format("    This site has been predicted\n");
        }
        System.out.format("    Hit ID=%d, maximum residual=%12.5e\n", hitID, mxResid);
        m.print("for this site");
        double B = m.Bfield.getField(m.p.X()).mag();
        Vec tB = m.Bfield.getField(m.p.X()).unitVec();
        System.out.format("    Magnetic field strength=%10.6f;   alpha=%10.6f\n", B, alpha);
        tB.print("magnetic field direction");
        System.out.format("    chi^2 increment=%12.4e\n", chi2inc);
        if (predicted)
            aP.print("predicted");
        if (filtered)
            aF.print("filtered");
        if (smoothed)
            aS.print("smoothed");
        if (H != null)
            H.print("matrix of the transformation from state vector to measurement");
        System.out.format("      Detector thickness=%10.7f radiation lengths\n", XL);
        System.out.format("      Assumed electron dE/dx in GeV/mm = %10.6f;  Detector thickness=%10.6f\n", dEdx, m.thickness);
        if (m.Layer < 0) {
            System.out.format("End of dump of dummy measurement site %d<<\n", thisSite);
        } else {
            System.out.format("End of dump of measurement site %d<<\n", thisSite);
        }
    }

    MeasurementSite(int thisSite, SiModule data, double mxResid, double mxResidShare) {
        this.thisSite = thisSite;
        this.mxResid = mxResid;
        this.mxResidShare = mxResidShare;
        this.m = data;
        hitID = -1;
        double c = 2.99793e8; // Speed of light in m/s
        conFac = 1.0e12 / c;
        Vec Bfield = m.Bfield.getField(m.p.X());
        B = Bfield.mag();
        alpha = conFac / B; // Convert from pt in GeV to curvature in mm
        predicted = false;
        filtered = false;
        smoothed = false;
        double rho = 2.329; // Density of silicon in g/cm^2
        double radLen = (21.82 / rho) * 10.0; // Radiation length of silicon in millimeters
        XL = m.thickness / radLen;
        double sp = 0.002; // Estar collision stopping power for electrons in silicon at about a GeV, in GeV cm2/g
        dEdx = -0.1 * sp * rho; // in GeV/mm
        chi2inc = 0.;
        verbose = false;
    }

    int makePrediction(StateVector pS, int hitNumber, boolean sharingOK, boolean pickup) { // Create predicted state vector by propagating from previous site
        verbose = pS.verbose;
        int returnFlag = 0;
        double phi = pS.planeIntersect(m.p);
        if (Double.isNaN(phi)) { // There may be no intersection if the momentum is too low!
            if (verbose)
                System.out.format("MeasurementSite.makePrediction: no intersection of helix with the plane exists. Site=%d\n", thisSite);
            return -1;
        }

        Vec X0 = pS.atPhi(phi); // Intersection point in local coordinate system of pS
        if (verbose) {
            pS.a.print("helix parameters in makePrediction");
            X0.print("intersection in local coordinates in makePrediction");
            pS.toGlobal(X0).print("intersection in global coordinates in makePrediction");
            Plane pRot = m.p.toLocal(pS.Rot, pS.origin);
            double check = (X0.dif(pRot.X())).dot(pRot.T());
            System.out.format("MeasurementSite.makePrediction: dot product of vector in plane with plane direction=%12.8e, should be zero\n", check);
        }
        Vec pMom = pS.Rot.inverseRotate(pS.getMom(phi));
        double ct = pMom.unitVec().dot(m.p.T());

        double deltaE = 0.; // dEdx*thickness/ct;

        Vec origin = m.p.X();
        Vec Bfield = m.Bfield.getField(pS.toGlobal(X0));
        double B = Bfield.mag();
        Vec tB = Bfield.unitVec(B);
        if (verbose) {
            origin.print("new origin in MeasurementSite.makePrediction");
            Bfield.print("B field at pivot in MeasurementSite.makePrediction");
        }

        // Move pivot point to X0 to generate the predicted helix
        aP = pS.predict(thisSite, X0, B, tB, origin, XL / Math.abs(ct), deltaE);
        if (verbose) {
            pS.a.print("original helix in MeasurementSite.makePrediction");
            aP.a.print("pivot transformed helix in MeasurementSite.makePrediction");
            // double phi2 = aP.planeIntersect(m.p);
            // System.out.format("MeasurementSite.makePrediction: phi2=%12.9f\n", phi2);
            // Vec X02 = aP.atPhi(phi2);
            // X02.print("intersection in local coordinates from new helix");
            // aP.toGlobal(X02).print("intersection in global coordinates from new helix");
        }

        // if (verbose) {
        // System.out.format("MeasurementSite.makePrediction: old helix intersects plane
        // at phi=%10.7f\n", phi);
        // Vec rGlobalOld = pS.toGlobal(pS.atPhi(phi));
        // rGlobalOld.print("global intersection with old helix from
        // measurementSite.makePrediction");
        // double phiNew = aP.planeIntersect(m.p);
        // System.out.format("MeasurementSite.makePrediction: new helix intersections
        // plane at phi=%10.7f\n", phiNew);
        // Vec rGlobal = aP.toGlobal(aP.atPhi(phiNew));
        // rGlobal.print("global intersection with new helix from
        // MeasurementSite.makePrediction");
        // }

        aP.mPred = h(pS, phi);
        H = new Vec(5, buildH(aP));

        // Loop over hits and find the one that is closest, unless the hit has been specified
        int nHits = m.hits.size();
        if (nHits > 0) {
            double minResid = 999.;
            double cut = mxResid;
            int theHit = hitNumber;
            if (theHit < 0 && pickup) {
                for (int i = 0; i < nHits; i++) {
                    if (m.hits.get(i).tracks.size() > 0)
                        continue; // Skip used hits
                    double residual = m.hits.get(i).v - aP.mPred;
                    if (Math.abs(residual) < minResid) {
                        theHit = i;
                        minResid = Math.abs(residual);
                    }
                }
                double minResid2 = 999.;
                if (theHit < 0 && sharingOK) {
                    for (int i = 0; i < nHits; i++) {
                        double residual = m.hits.get(i).v - aP.mPred;
                        if (Math.abs(residual) < minResid2) {
                            theHit = i;
                            minResid2 = Math.abs(residual);
                        }
                    }
                    cut = mxResidShare;
                }
            }
            if (theHit >= 0) {
                aP.r = m.hits.get(theHit).v - aP.mPred;
                if (verbose) {
                    if (hitNumber >= 0) {
                        System.out.format("MeasurementSite.makePrediction: specified hit=%d with residual=%10.7f\n", theHit, minResid);
                    } else {
                        System.out.format("MeasurementSite.makePrediction: selected hit=%d with minimum residual=%10.7f\n", theHit, minResid);
                    }
                    System.out.format("MeasurementSite.makePrediction: intersection with old helix is at phi=%10.7f, z=%10.7f\n", phi, aP.mPred);
                    double phi2 = aP.planeIntersect(m.p); // This should always be zero
                    double mPred2 = h(aP, phi2);
                    System.out.format("MeasurementSite.makePrediction: intersection with new helix is at phi=%10.7f, z=%10.7f\n", phi2, mPred2);
                }

                aP.R = m.hits.get(0).sigma * m.hits.get(0).sigma - H.dot(H.leftMultiply(aP.C)); // Can be negative if aP.C is not reasonable (e.g. huge)
                if (verbose) {
                    H.print("H in MeasurementSite.makePrediction");
                    Vec H2 = new Vec(5, buildH(pS));
                    H2.print("H made using old statevector");
                    aP.C.print("covariance");
                    double exRes = m.hits.get(0).sigma * m.hits.get(0).sigma - H2.dot(H2.leftMultiply(pS.C));
                    System.out.format("MeasurementSite.makePrediction: expected residual = %12.5e; from old state vector = %12.5e, sigma=%12.5e\n", aP.R, exRes, m.hits.get(0).sigma);
                }

                if (aP.R > 0.) {
                    chi2inc = aP.r * aP.r / aP.R;
                } else {
                    chi2inc = aP.r * aP.r / (aP.R * aP.R);
                }
                if (Math.abs(aP.r / m.hits.get(0).sigma) < cut || hitNumber >= 0) { // Use the hit no matter what if it was handed to us
                    hitID = theHit;
                    returnFlag = 1;
                } else {
                    chi2inc = 0.;
                }
                if (verbose) {
                    System.out.format("MeasurementSite.makePrediction: chi2 increment=%12.5e, hitID=%d, theHit=%d, mxResid=%12.5e\n", chi2inc, hitID, theHit, cut);
                }
            }
        }

        predicted = true;

        return returnFlag;
    }

    boolean filter() { // Produce the filtered state vector for this site

        // debug
        double phi = aP.planeIntersect(m.p);
        System.out.format("    phiNew=%10.7f\n", phi);
        Vec xGlobal = aP.atPhi(phi);
        xGlobal.print("xGlobal");
        Vec xLocal = m.R.rotate(xGlobal.dif(m.p.X()));
        xLocal.print("MeasurementSite.filter: local intersection");
        // end debug

        if (smoothed || filtered || !predicted) {
            System.out.format("******MeasurementSite.filter: Warning, this site is not in the correct state!\n");
            // this.print("in the wrong state for filtering");
        }

        // For dummy layers or layers with no hits just copy the predicted state
        if (hitID < 0) {
            aF = aP;
            filtered = true;
            chi2inc = 0.;
            return true;
        }

        Measurement hit = m.hits.get(hitID);
        double V = hit.sigma * hit.sigma;
        aF = aP.filter(H, V);
        double phiF = aF.planeIntersect(m.p);

        // double phiCheck = aF.planeIntersect(m.p);
        // System.out.format("MeasurementSite.filter: phi = %10.7f, phi check = %10.7f\n",phiF, phiCheck);
        if (Double.isNaN(phiF)) { // There may be no intersection if the momentum is too low!
            //if (verbose)
            System.out.format("MeasurementSite.filter: no intersection of helix with the plane exists. Site=%d\n", thisSite);
            aF.a.print("helix parameters (state vector)");
            m.p.print("for the intersection");
            //return false;
        }
        aF.mPred = h(aF, phiF);
        aF.r = hit.v - aF.mPred;

        // Vec HF = new Vec(5, buildH(aF)); // No need to recalculate H from the filtered helix parameters
        // HF.print("filtered H");
        // H.print("predicted H");
        aF.R = V - H.dot(H.leftMultiply(aF.C));
        if (aF.R < 0) {
            if (verbose)
                System.out.format("MeasurementSite.filter: covariance of residual %12.4e is negative\n", aF.R);
            aF.R = V;
        }

        chi2inc = (aF.r * aF.r) / aF.R;

        filtered = true;
        return true;
    }

    // Inverse Kalman filter: remove this site from the smoothed track fit
    boolean removeHit() {
        if (!smoothed) {
            System.out.format("******MeasurementSite.removeHit: Warning, this site is not in the correct state!\n");
            // this.print("in the wrong state for hit removal");
            return false;
        }
        if (hitID < 0) {
            return false;
        }
        // aS.print("in removeHit before");
        // Measurement hit = m.hits.get(hitID);
        // double V = hit.sigma * hit.sigma;
        // aS = aS.inverseFilter(H, V);
        // aS.print("in removeHit after");
        hitID = -1;

        return true;
    }

    Measurement addHit(double cut, int oldID) {
        StateVector a = null;
        if (smoothed) {
            a = aS;
        } else if (filtered) {
            a = aF;
        } else {
            System.out.format("******MeasurementSite.addHit: Warning, this site is not in the correct state!\n");
            // this.print("in the wrong state for hit addition");
            return null;
        }
        double mPred = 0.;
        boolean firstHit = true;
        double chi2incMin = 9999.;
        int theHit = -1;
        for (int hitidx = 0; hitidx < m.hits.size(); hitidx++) {
            if (hitidx == oldID)
                continue; // don't add a hit that was just removed
            Measurement hit = m.hits.get(hitidx);
            if (hit.tracks.size() > 0)
                continue;
            if (firstHit) {
                double phiS = a.planeIntersect(m.p);

                // double phiCheck = aF.planeIntersect(m.p);
                // System.out.format("MeasurementSite.filter: phi = %10.7f, phi check = %10.7f\n",phiF, phiCheck);
                if (Double.isNaN(phiS)) { // There may be no intersection if the momentum is too low!
                    break;
                }
                mPred = h(a, phiS);
                firstHit = false;
            }
            double residual = hit.v - mPred;
            double var = hit.sigma * hit.sigma; // - H.dot(H.leftMultiply(a.C));
            double chi2inc = (residual * residual) / var;
            if (chi2inc < chi2incMin) {
                chi2incMin = chi2inc;
                theHit = hitidx;
            }
        }
        if (chi2incMin < cut) {
            hitID = theHit;
            this.chi2inc = chi2incMin;
            return m.hits.get(theHit);
        }
        return null;
    }

    // Produce the smoothed state vector for this site
    boolean smooth(MeasurementSite nS) {
        // nS is the next site in the filtering chain (i.e. the previous site that was smoothed)

        if (smoothed || !filtered) {
            System.out.format("******MeasurementSite.smooth: Warning, this site is not in the correct state!\n");
            // this.print("in the wrong state for smoothing");
        }

        this.aS = this.aF.smooth(nS.aS, nS.aP);
        if (hitID < 0) {
            return true;
        }

        Measurement hit = this.m.hits.get(hitID);
        double V = hit.sigma * hit.sigma;
        double phiS = aS.planeIntersect(m.p);

        if (Double.isNaN(phiS)) { // This should almost never happen!
            System.out.format("MeasurementSite.smooth: no intersection of helix with the plane exists.\n");
            return false;
        }
        this.aS.mPred = this.h(aS, phiS);
        this.aS.r = hit.v - this.aS.mPred;

        // Vec HS = new Vec(5, buildH(aS)); // It's not necessary to recalculate H with the smoothed helix parameters
        // H.print("old H");
        // HS.print("new H");

        this.aS.R = V - this.H.dot(this.H.leftMultiply(this.aS.C));
        if (this.aS.R < 0) {
            System.out.format("MeasurementSite.smooth, measurement covariance %12.4e is negative\n", this.aS.R);
            aS.print("the smoothed state");
            nS.print("the next site in the chain");
        }

        this.chi2inc = (this.aS.r * this.aS.r) / this.aS.R;

        this.smoothed = true;
        return true;
    }

    double h(StateVector pS, double phi) { // Predict the measurement for a helix passing through this plane
        Vec rGlobal = pS.toGlobal(pS.atPhi(phi));
        if (verbose)
            rGlobal.print("MeasurementSite.h: global intersection");
        Vec rLocal = m.toLocal(rGlobal); // Rotate into the detector coordinate system
        if (verbose) {
            rLocal.print("MeasurementSite.h: local intersection");
            System.out.format("MeasurementSite.h: phi=%12.5e, detector coordinate out of the plane = %10.7f, h=%10.7f\n", phi, rLocal.v[2], rLocal.v[1]);
        }
        return rLocal.v[1];
    }

    // Create the derivative matrix for prediction of the measurement from the helix
    private double[] buildH(StateVector S) {
        double[] HH = new double[5];
        double phi = S.planeIntersect(m.p);

        if (Double.isNaN(phi)) { // There may be no intersection if the momentum is too low!
            if (verbose)
                System.out.format("MeasurementSite.buildH: no intersection of helix with the plane exists.\n");
            return HH;
        }
        if (verbose) {
            System.out.format("MeasurementSite.buildH: phi=%10.7f\n", phi);
            // S.print("given to buildH");
            // R.print("in buildH");
            // p.print("in buildH");
        }
        Vec dxdphi = new Vec((alpha / S.a.v[2]) * Math.sin(S.a.v[1] + phi), -(alpha / S.a.v[2]) * Math.cos(S.a.v[1] + phi), -(alpha / S.a.v[2]) * S.a.v[4]);
        double[][] dxda = new double[3][5];
        dxda[0][0] = Math.cos(S.a.v[1]);
        dxda[1][0] = Math.sin(S.a.v[1]);
        dxda[0][1] = -(S.a.v[0] + alpha / S.a.v[2]) * Math.sin(S.a.v[1]) + (alpha / S.a.v[2]) * Math.sin(S.a.v[1] + phi);
        dxda[1][1] = (S.a.v[0] + alpha / S.a.v[2]) * Math.cos(S.a.v[1]) - (alpha / S.a.v[2]) * Math.cos(S.a.v[1] + phi);
        dxda[0][2] = -(alpha / (S.a.v[2] * S.a.v[2])) * (Math.cos(S.a.v[1]) - Math.cos(S.a.v[1] + phi));
        dxda[1][2] = -(alpha / (S.a.v[2] * S.a.v[2])) * (Math.sin(S.a.v[1]) - Math.sin(S.a.v[1] + phi));
        dxda[2][2] = (alpha / (S.a.v[2] * S.a.v[2])) * S.a.v[4] * phi;
        dxda[2][3] = 1.0;
        dxda[2][4] = -(alpha / S.a.v[2]) * phi;

        // System.out.format(" Matrix dxda:\n");
        double[] dphida = new double[5];
        double denom = m.p.T().dot(dxdphi);
        for (int i = 0; i < 5; i++) {
            // System.out.format(" %10.6f, %10.6f, %10.6f\n", dxda[0][i], dxda[1][i],
            // dxda[2][i]);
            for (int j = 0; j < 3; j++) {
                dphida[i] -= m.p.T().v[j] * dxda[j][i];
            }
            dphida[i] = dphida[i] / denom;
        }
        // System.out.format(" dphida=%10.7f, %10.7f, %10.7f, %10.7f, %10.7f\n",dphida[0], dphida[1], dphida[2], dphida[3], dphida[4]);
        double[][] DxDa = new double[3][5];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 3; j++) {
                DxDa[j][i] = dxdphi.v[j] * dphida[i] + dxda[j][i];
                // if (verbose) System.out.format(" %d %d DxDa=%10.7f\n", j,i,DxDa[j][i]);
            }
        }
        RotMatrix Rt = m.Rinv.multiply(S.Rot.invert());
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 3; j++) {
                HH[i] += Rt.M[1][j] * DxDa[j][i];
            }
        }

        // Testing the derivatives
        if (verbose) {
            StateVector sVp = S.copy();
            double daRel[] = { 0.01, 0.03, -0.02, 0.05, -0.01 };
            for (int i = 0; i < 5; i++) {
                sVp.a.v[i] = sVp.a.v[i] * (1.0 + daRel[i]);
            }
            Vec da = new Vec(S.a.v[0] * daRel[0], S.a.v[1] * daRel[1], S.a.v[2] * daRel[2], S.a.v[3] * daRel[3], S.a.v[4] * daRel[4]);
            double phi1 = phi;
            Vec x1Global = S.toGlobal(S.atPhi(phi1));
            Vec x1Local = m.toLocal(x1Global);
            double phi2 = sVp.planeIntersect(m.p);
            Vec x2Global = sVp.toGlobal(sVp.atPhi(phi2));
            double dot2 = x2Global.dif(m.p.X()).dot(m.p.T());
            double dot1 = x1Global.dif(m.p.X()).dot(m.p.T());
            System.out.format("h derivative testing: dot1=%10.8f, dot2=%10.8f, both should be zero\n", dot1, dot2);
            Vec x2Local = m.toLocal(x2Global);

            S.a.print("Test helix parameters");
            // m.p.print("of the measurement");
            sVp.a.print("Modified helix parameters");
            System.out.format("Phi1=%10.7f,  Phi2=%10.7f\n", phi1, phi2);
            x1Global.print("x1");
            x2Global.print("x2");
            for (int j = 0; j < 3; j++) {
                double dx = 0.;
                for (int i = 0; i < 5; i++) {
                    dx += DxDa[j][i] * da.v[i];
                }
                System.out.format("j=%d dx=%10.7f,   dxExact=%10.7f\n", j, dx, x2Global.v[j] - x1Global.v[j]);
            }

            double dmExact = x2Local.v[1] - x1Local.v[1];
            double dm = 0.;
            for (int i = 0; i < 5; i++) {
                dm += HH[i] * da.v[i];
            }
            System.out.format("Test of H matrix: dm=%10.8f,  dmExact=%10.8f\n", dm, dmExact);
        }

        return HH;
    }

    // Comparator function for sorting tracks by quality
    static Comparator<MeasurementSite> SiteComparatorUp = new Comparator<MeasurementSite>() {
        public int compare(MeasurementSite s1, MeasurementSite s2) {
            int lyr1 = s1.m.Layer;
            int lyr2 = s2.m.Layer;
            if (lyr1 < lyr2) {
                return -1;
            } else if (lyr1 > lyr2) {
                return 1;
            } else {
                double a1 = s1.m.stereo;
                if (a1 == 0.) {
                    return -1;
                } else {
                    return 1;
                }
            }
        }
    };
    static Comparator<MeasurementSite> SiteComparatorDn = new Comparator<MeasurementSite>() {
        public int compare(MeasurementSite s1, MeasurementSite s2) {
            int lyr1 = s1.m.Layer;
            int lyr2 = s2.m.Layer;
            if (lyr1 < lyr2) {
                return 1;
            } else if (lyr1 > lyr2) {
                return -1;
            } else {
                double a1 = s1.m.stereo;
                if (a1 == 0.) {
                    return 1;
                } else {
                    return -1;
                }
            }
        }
    };
}
