package org.hps.recon.tracking.kalman;

import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.math.util.FastMath;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

/**
 * Kalman fit measurement site, one for each silicon-strip detector with hits
 */
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
    DMatrixRMaj H; // Derivatives of the transformation from state vector to measurement
    double arcLength; // Arc length from the previous measurement
    private double conFac; // Conversion from B to alpha
    double alpha;
    double radLen; // radiation length in silicon
    private double dEdx; // in GeV/mm
    private KalmanParams kPar;
    double B;
    final static private boolean debug = false;
    private static Logger logger;
    private static DMatrixRMaj tempV;
    private static boolean initialized;

    // Note: I can remove the concept of a dummy layer and make all layers equivalent, except that the non-physical ones
    // will never have a hit and thus will be handled the same as physical layers that lack hits
    
    void print(String s) {
        System.out.format("%s", this.toString(s));
    }

    String toString(String s) {
        String str;
        if (m.Layer < 0) {
            str = String.format("\n****Dump of dummy measurement site %d %s;  ", thisSite, s);
        } else {
            str = String.format("\n****Dump of measurement site %d %s;  ", thisSite, s);
        }
        if (smoothed) {
            str=str+"    This site has been smoothed\n";
        } else if (filtered) {
            str=str+"    This site has been filtered\n";
        } else if (predicted) { 
            str=str+"    This site has been predicted\n"; 
        }
        str=str+String.format("    Hit ID=%d, maximum allowed residual=%10.5f\n", hitID, kPar.mxResid[1]);
        str = str + m.toString("for this site");
        double B = KalmanInterface.getField(m.p.X(), m.Bfield).mag();
        Vec tB = KalmanInterface.getField(m.p.X(), m.Bfield).unitVec();
        str=str+String.format("    Magnetic field strength=%10.6f;   alpha=%10.6f\n", B, alpha);
        str = str + tB.toString("magnetic field direction") + "\n";
        str=str+String.format("    chi^2 increment=%12.4e\n", chi2inc);
        str=str+String.format("    x scattering angle=%10.8f, y scattering angle=%10.8f\n", scatX(), scatZ());
        if (predicted) str = str + aP.toString("predicted");
        if (filtered) str = str + aF.toString("filtered");
        if (smoothed) str = str + aS.toString("smoothed");
        if (H != null) str = str + "matrix of the transformation from state vector to measurement:" + H.toString();
        str=str+String.format("      Assumed electron dE/dx in GeV/mm = %10.6f;  Detector thickness=%10.6f\n", dEdx, m.thickness);
        if (m.Layer < 0) {
            str=str+String.format("End of dump of dummy measurement site %d<<\n", thisSite);
        } else {
            str=str+String.format("End of dump of measurement site %d<<\n", thisSite);
        }
        return str;
    }

    MeasurementSite(int thisSite, SiModule data, KalmanParams kPar) {
        this.thisSite = thisSite;
        this.kPar = kPar;
        this.m = data;       
        hitID = -1;
        double c = 2.99793e8; // Speed of light in m/s
        conFac = 1.0e12 / c;
        Vec Bfield = KalmanInterface.getField(m.p.X(), m.Bfield);
        B = Bfield.mag();
        alpha = conFac / B; // Convert from 1/pt in 1/GeV to curvature in mm
        predicted = false;
        filtered = false;
        smoothed = false;
        double rho = 2.329; // Density of silicon in g/cm^3
        radLen = (21.82 / rho) * 10.0; // Radiation length of silicon in millimeters
        double sp = 0.002; // Estar collision stopping power for electrons in silicon at about a GeV, in GeV cm2/g
        dEdx = -0.1 * sp * rho; // in GeV/mm
        chi2inc = 0.;
        H = new DMatrixRMaj(5,1);
        if (!initialized) {
            tempV = new DMatrixRMaj(5,1);
            logger = Logger.getLogger(MeasurementSite.class.getName());
            initialized = true;
        }
    }

    double scatX() { // scattering angle in the x,y plane for the filtered state vector
        if (aP == null || aF == null) return -999.;
        Vec p1 = aP.helix.getMom(0.);
        double t1 = FastMath.atan2(p1.v[0], p1.v[1]);
        Vec p2 = aF.helix.getMom(0.);
        double t2 = FastMath.atan2(p2.v[0], p2.v[1]);
        return t1 - t2;
    }

    double scatZ() { // scattering angle in the z,y plane for the filtered state vector
        if (aP == null || aF == null) return -999.;
        Vec p1 = aP.helix.getMom(0.);
        double t1 = FastMath.atan2(p1.v[2], p1.v[1]);
        Vec p2 = aF.helix.getMom(0.);
        double t2 = FastMath.atan2(p2.v[2], p2.v[1]);
        return t1 - t2;
    }

    int makePrediction(StateVector pS, int hitNumber, boolean sharingOK, boolean pickup) {
        SiModule mPs = null;
        return makePrediction(pS, mPs, hitNumber, sharingOK, pickup, false);
    }

    int makePrediction(StateVector pS, SiModule mPs, int hitNumber, boolean sharingOK, boolean pickup) {
        return makePrediction(pS, mPs, hitNumber, sharingOK, false, false);
    }

    int makePrediction(StateVector pS, SiModule mPs, int hitNumber, boolean sharingOK, boolean pickup, boolean checkBounds) {
        double [] dT = {-1000., 1000.};
        return makePrediction(pS, mPs, hitNumber, sharingOK, pickup, checkBounds, dT, 0);
    }
    
    int makePrediction(StateVector pS, SiModule mPs, int hitNumber, boolean sharingOK, boolean pickup, boolean checkBounds, double [] tRange, int trial) {
        return makePrediction(pS, mPs, hitNumber, sharingOK, pickup, checkBounds, tRange, trial, false);
    }

    int makePrediction(StateVector pS, SiModule mPs, int hitNumber, boolean sharingOK, boolean pickup, boolean checkBounds, double [] tRange, int trial, boolean verbose2) { // Create predicted state vector by propagating from previous site
        // pS = state vector that we are predicting from
        // mPS = Si module that we are predicting from, if any
        // tRange = allowed time range [tmin,tmax] for picking up a hit
        // minE = minimum hit energy for it to be picked up, unless no other hit exists
        // sharingOK = whether to allow sharing of a hit between multiple tracks
        // pickup = whether we are doing pattern recognition here and need to pick up hits to add to the track
        int returnFlag = 0;
        double phi = pS.helix.planeIntersect(m.p);
        if (debug) verbose2 = true;
        if (Double.isNaN(phi)) { // There may be no intersection if the momentum is too low!
            if (debug) {
                System.out.format("MeasurementSite.makePrediction: no intersection of helix with the plane exists. Site=%d\n", thisSite);
                m.p.print("of intersection");
                pS.print("missing plane");
            }
            return -1;
        }

        Vec X0 = pS.helix.atPhi(phi); // Intersection point in local field coordinate system of pS
        if (debug) {
            pS.helix.a.print("helix parameters in makePrediction");
            X0.print("intersection in local coordinates in makePrediction");
            pS.helix.toGlobal(X0).print("intersection in global coordinates in makePrediction");
            Plane pRot = m.p.toLocal(pS.helix.Rot, pS.helix.origin);
            double check = (X0.dif(pRot.X())).dot(pRot.T());
            System.out.format("MeasurementSite.makePrediction: dot product of vector in plane with plane direction=%12.8e, should be zero\n",
                    check);
        }

        double deltaE = 0.; // dEdx*thickness/ct;

        Vec origin = m.p.X();
        Vec Bfield = KalmanInterface.getField(pS.helix.toGlobal(X0), m.Bfield);
        double B = Bfield.mag();
        Vec tB = Bfield.unitVec(B);
        if (debug) {
            origin.print("new origin in MeasurementSite.makePrediction");
            Bfield.print("B field at pivot in MeasurementSite.makePrediction");
        }

        // Move pivot point to X0 to generate the predicted helix
        // First we need the momentum direction to calculate how much silicon we pass through
        Vec pMom = pS.helix.Rot.inverseRotate(pS.helix.getMom(0.));
        double XL;
        if (mPs == null) {
            XL = 0.;
            arcLength = 0.;
        } else {
            double ct = pMom.unitVec().dot(mPs.p.T()); // cos(theta) at the **previous** site
            double radius = alpha/pS.helix.a.v[2];
            XL = mPs.thickness / radLen / Math.abs(ct); // Si scattering thickness at previous site
            arcLength = -radius*phi*FastMath.sqrt(1.0 + pS.helix.a.v[4] * pS.helix.a.v[4]);
            if (debug) {
                double dx = m.p.X().v[0]-mPs.p.X().v[0];
                double dy = m.p.X().v[1]-mPs.p.X().v[1];
                double dz = m.p.X().v[2]-mPs.p.X().v[2];
                double distance = FastMath.sqrt(dx*dx + dy*dy + dz*dz);
                System.out.format("MeasurementSite.predict: arc length=%10.5f, distance=%10.5f\n", arcLength, distance);
            }
        }
        aP = pS.predict(thisSite, X0, B, tB, origin, XL, deltaE);
        
        // This calculates the H corresponding to using aP in h( , , ). It is kind of trivial, because
        // aP are helix parameters for a pivot right at the predicted intersection point, on the helix. 
        // Hence the prediction at that point does not depend on the helix parameters at all.
        buildH(aP, H);
        CommonOps_DDRM.mult(aP.helix.C, H, tempV);
        double Rextrap = CommonOps_DDRM.dot(H, tempV);
        
        // Check whether the intersection is within the bounds of the detector, with some margin
        // If not, then the pattern recognition may look in another detector in the layer.
        // Don't do the check if the hit number is already specified. Also, the check will not
        // be called for in the last sensor of the layer, because we need to run the Kalman prediction
        // to this layer before moving to the next, even if there is no hit.       
        double tol = kPar.edgeTolerance + FastMath.sqrt(Rextrap); // Tolerance on the check, in mm
        if (debug) System.out.format("MeasurementSite.predict: edge tolerance = %10.4f mm\n", tol);
        Vec rLocal = null;
        if (checkBounds && hitNumber < 0) {
            Vec rGlobal = pS.helix.toGlobal(X0); // Transform from field coordinates to global coordinates
            rLocal = m.toLocal(rGlobal); // Rotate into the detector coordinate system
            if (rLocal.v[0] < m.xExtent[0] - tol || rLocal.v[0] > m.xExtent[1] + tol) return -2;
            if (rLocal.v[1] < m.yExtent[0] - tol || rLocal.v[1] > m.yExtent[1] + tol) return -2;
        }
        
        if (debug) {
            pS.helix.a.print("original helix in MeasurementSite.makePrediction");
            pS.helix.X0.print("original helix pivot point");
            //pS.toGlobal(pS.X0).print("original pivot in global coordinates");
            //if (mPs != null) mPs.toLocal(pS.toGlobal(pS.X0)).print("original pivot in detector coordinates");
            aP.helix.a.print("pivot transformed helix in MeasurementSite.makePrediction");
            aP.helix.X0.print("transformed helix pivot point");
            //aP.toGlobal(aP.X0).print("transformed pivot in global coordinates");
            //m.toLocal(aP.toGlobal(aP.X0)).print("transformed pivot in detector coordinates");
            // double phi2 = aP.planeIntersect(m.p);
            // System.out.format("MeasurementSite.makePrediction: phi2=%12.9f\n", phi2);
            // Vec X02 = aP.atPhi(phi2);
            // X02.print("intersection in local coordinates from new helix");
            // aP.toGlobal(X02).print("intersection in global coordinates from new helix");
            System.out.format("MeasurementSite.makePrediction: old helix intersects plane at phi=%10.7f\n", phi);
            Vec rGlobalOld = pS.helix.toGlobal(pS.helix.atPhi(phi));
            rGlobalOld.print("global intersection with old helix from measurementSite.makePrediction");
            double phiNew = aP.helix.planeIntersect(m.p); // This angle should always be zero
            System.out.format("MeasurementSite.makePrediction: new helix intersects plane at phi=%10.7f\n", phiNew);
            Vec rGlobal = aP.helix.toGlobal(aP.helix.atPhi(phiNew)); // This should be equal to rGlobalOld
            rGlobal.print("global intersection with new helix from MeasurementSite.makePrediction");
        }

        aP.mPred = h(pS, m, phi);

        // Test of the H vector, by comparing with a numerical difference. If this is done with the H
        // calculated from aP, comparing with h calculated from aP, then the difference is always zero.
        // By comparing with the less trivial case of h and H calculated from pS, we verify here that
        // the derivatives are correct.
        
        if (debug) {
            System.out.format("H corresponding to aP ");
            H.print();
            DMatrixRMaj H3m = new DMatrixRMaj(5,1);
            buildH(pS, H3m);
            Vec H3 = StateVector.mToVec(H3m);
            H3.print("H corresponding to pS");
            double mPredaP = h(aP, m); // This should give exactly the same result as for aP.mPred above
            System.out.format("Predicted m: from pS=%10.5f;  from aP=%10.5f\n", mPredaP, aP.mPred);

            StateVector tS = pS.copy();
            Vec da = new Vec(5);
            double[] del = { 0.009, -0.005, -0.01, 0.013, 0.011 };
            for (int i = 0; i < 5; i++) { da.v[i] = pS.helix.a.v[i] * del[i]; }
            tS.helix.a = tS.helix.a.sum(da);
            double dxTrue = h(tS, m) - h(pS, m);
            double dxH = H3.dot(da);
            System.out.format("Measurementsite.predict: dm True=%10.5f,   dm approx by H=%10.5f\n\n", dxTrue, dxH);
        }        

        // Loop over hits and find the one that is closest, unless the hit has been specified
        int nHits = m.hits.size();
        if (nHits > 0) {
            double cut = kPar.mxResid[trial];
            int theHit = hitNumber;
            if (theHit < 0 && pickup) {
                // Don't pick up a hit if the track projection is outside of the bounds of the strip
                // This is especially serious for stereo layers
                if (rLocal == null) {
                    Vec rGlobal = pS.helix.toGlobal(X0); // Transform from field coordinates to global coordinates
                    rLocal = m.toLocal(rGlobal);   // Rotate into the detector coordinate system
                }
                if (rLocal.v[0] > m.xExtent[0] - tol && rLocal.v[0] < m.xExtent[1] + tol) {
                    int theHitAll = theHit;
                    double minResid = 999.;
                    double minResidAll = 999.;
                    for (int i = 0; i < nHits; i++) {
                        Measurement mHit = m.hits.get(i);
                        if (mHit.tracks.size() > 0) continue; // Skip used hits
                        //if (m.hits.get(i).tksMC.size()==0) {  // For cheating, using MC truth to avoid noise hits.
                        //    continue;
                        //}
                        if (m.split) { // Innermost 2019 layers have strips split in the middle into separate channels
                            if (rLocal.v[0] * mHit.x < 0.) { // The track is on the wrong side
                                if (Math.abs(rLocal.v[0]) > tol) continue;
                            }
                        }
                        double residual = Math.abs(mHit.v - aP.mPred);
                        double ctv = (mHit.v - aP.mPred)/mHit.sigma;
                        if (debug) {
                            System.out.format("  MeasurementSite.makePrediction: Found unused hit, residual=%10.5f, sigmas=%10.5f, cut=%10.5f\n", residual, ctv, kPar.mxResid[trial]); 
                            System.out.format("        Hit energy=%10.4f, cut = %10.4f\n",m.hits.get(i).energy, kPar.minSeedE[m.Layer]);
                        }
                        if (residual < minResid) {
                            if (m.hits.get(i).energy > kPar.minSeedE[m.Layer]) {
                                theHit = i;
                                minResid = residual;
                            }
                        }
                        if (residual < minResidAll) {
                            theHitAll = i;
                            minResidAll = residual;
                        }
                    }
                    double sigmas = 999.;
                    if (theHitAll >= 0) {
                        sigmas = minResidAll/m.hits.get(theHitAll).sigma;
                    }
                    if (theHitAll < 0 || sigmas > cut) {  // Look for good shared hits only if there is no other option
                        if (sharingOK) {
                            double minResid2 = minResidAll;
                            for (int i = 0; i < nHits; i++) {
                                double residual = m.hits.get(i).v - aP.mPred;
                                if (debug) {                         
                                    double ctv = residual/m.hits.get(i).sigma;
                                    System.out.format("  MeasurementSite.makePrediction: Found used hit, residual=%10.5f, sigmas=%10.5f, cut=%10.5f\n", residual, ctv, kPar.mxResid[trial]); 
                                }
                                if (Math.abs(residual) < minResid2 && m.hits.get(i).energy > kPar.minSeedE[m.Layer]) {   // Shared hits should never be low energy
                                    theHit = i;
                                    minResid2 = Math.abs(residual);
                                    cut = kPar.mxResidShare;
                                    if (debug) System.out.format("MeasurementSite.makePrediction: shared hit candidate %d with residual %10.4f\n", theHit, residual);
                                }
                            }
                        }
                    } else {
                        if (theHit < 0) {   // A low-pulse-height hit is the only option
                            theHit = theHitAll;
                        } else if (theHit != theHitAll){
                            double cutVal = minResid/m.hits.get(theHit).sigma;
                            if (debug) {
                                System.out.format("MeasurementSite.makePrediction: hit with ph=%8.2f versus ph=%8.2f\n", m.hits.get(theHitAll).energy,m.hits.get(theHit).energy);
                                System.out.format("  Residuals = %10.4f for %d and %10.4f for %d, cutVal=%10.4f\n", minResidAll, theHit, minResid, theHitAll, cutVal);
                            }
                            if (cutVal < cut) {  // A high-ph hit and a low-ph hit are within the cut. Now we have to decide. . .
                                if (cutVal > kPar.mxResidShare) { // If the high-ph hit is really good, just keep it
                                    if (minResidAll/minResid < kPar.lowPhThresh) { // the low-ph hit is attractive enough that we will take it
                                        if (debug) {
                                            System.out.format("MeasurementSite.makePrediction: choosing hit with ph=%8.2f over hit with ph=%8.2f\n", m.hits.get(theHitAll).energy,m.hits.get(theHit).energy);
                                            System.out.format("  Residuals = %10.4f for %d and %10.4f for %d, ratio=%9.4f\n", minResidAll, theHit, minResid, theHitAll, minResidAll/minResid);
                                        }
                                        theHit = theHitAll;
                                    }
                                }
                            } else {  // Maybe the low-pulse-height hit will pass
                                theHit = theHitAll;
                            }
                        }
                    }
                }
            }
            if (theHit >= 0) {
                aP.r = m.hits.get(theHit).v - aP.mPred;
                if (debug) {
                    if (hitNumber >= 0) {
                        System.out.format("MeasurementSite.makePrediction: specified hit=%d with residual=%10.7f\n", theHit, aP.r);
                    } else {
                        System.out.format("MeasurementSite.makePrediction: selected hit=%d with minimum residual=%10.7f\n", theHit, aP.r);
                    }
                    System.out.format("MeasurementSite.makePrediction: intersection with old helix is at phi=%10.7f, z=%10.7f\n", phi,
                            aP.mPred);
                    double phi2 = aP.helix.planeIntersect(m.p); // This should always be zero
                    double mPred2 = h(aP, m, phi2);
                    System.out.format("MeasurementSite.makePrediction: intersection with new helix is at phi=%10.7f, z=%10.7f\n", phi2, mPred2);
                }

                aP.R = m.hits.get(theHit).sigma * m.hits.get(theHit).sigma + Rextrap;

                chi2inc = aP.r * aP.r / aP.R;
                double cutVal = Math.abs(aP.r / m.hits.get(theHit).sigma);
                if (verbose2) {
                    System.out.format("  MeasurementSite.makePrediction: Lyr %d det %d: do we add hit with residual=%10.5f, err=%10.5f, chi2inc=%10.5f, %10.5f<%10.5f?, t=%8.2f\n",
                            m.Layer, m.detector, aP.r, FastMath.sqrt(aP.R), chi2inc, cutVal, cut, m.hits.get(theHit).time);
                }

                if (hitNumber >= 0) { // Use the hit no matter what if it was handed to us
                    hitID = theHit;
                    returnFlag = 1;
                    if (verbose2) System.out.format("  MeasurementSite.makePrediction: adding given hit number %d\n", hitID);
                } else {
                    double hitTime = m.hits.get(theHit).time;
                    if ((!Double.isNaN(chi2inc) && cutVal < cut) && hitTime >= tRange[0] && hitTime <= tRange[1]) { 
                        hitID = theHit;
                        returnFlag = 1;
                        if (verbose2) System.out.format("  MeasurementSite.makePrediction: adding hit number %d with t=%8.2f\n", hitID, hitTime);
                    } else {
                        chi2inc = 0.;
                        if (verbose2) System.out.format("   MeasurementSite.makePrediction: rejecting hit %d with t=%8.2f cutval=%10.6f, t-range=%9.3f-%9.3f\n", hitID, hitTime,cutVal,tRange[0],tRange[1]);
                    }
                }
                if (debug) {
                    System.out.format("MeasurementSite.makePrediction: chi2 increment=%12.5e, hitID=%d, theHit=%d, mxResid=%12.5e, rF=%d\n",
                            chi2inc, hitID, theHit, cut, returnFlag);
                }
            }
        }

        predicted = true;

        return returnFlag; // -2 for extrapolation not within detector, -1 for error, 1 for a hit was used, 0 no hit used
    }

    boolean filter() { // Produce the filtered state vector for this site
        if (!predicted) {
            System.out.format("******MeasurementSite.filter: Warning, this site is not in the correct state!\n");
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
        double phiF = aF.helix.planeIntersect(m.p);

        // double phiCheck = aF.planeIntersect(m.p);
        // System.out.format("MeasurementSite.filter: phi = %10.7f, phi check = %10.7f\n",phiF, phiCheck);
        final boolean verbose2 = false;
        if (Double.isNaN(phiF)) { // There may be no intersection if the momentum is too low!
            if (verbose2) {
                System.out.format("MeasurementSite.filter: no intersection of helix with the plane exists at layer %d detector %d\n", m.Layer, m.detector);
                aP.helix.a.print("predicted helix parameters");
                aF.helix.a.print("Filtered helix parameters");
                m.p.print("for the intersection");
            }
            return false;
        }
        aF.mPred = h(aF, m, phiF);
        aF.r = hit.v - aF.mPred;

        // Recalculate H using the filtered state vector (this makes a minor difference)
        buildH(aF, H);

        // Another numerical test of the derivatives in H
        if (debug) {
            StateVector tS = aF.copy();
            Vec da = new Vec(5);
            double[] del = { 0.009, -0.005, -0.01, 0.013, 0.011 };
            for (int i = 0; i < 5; i++) { da.v[i] = aF.helix.a.v[i] * del[i]; }
            tS.helix.a = tS.helix.a.sum(da);
            double dxTrue = h(tS, m) - aF.mPred;
            Vec Hv = StateVector.mToVec(H);
            double dxH = Hv.dot(da);
            System.out.format("Measurementsite.predict: dm True=%10.5f,   dm approx by H=%10.5f\n\n", dxTrue, dxH);
        }

        // Calculate the filtered covariance of the residual
        CommonOps_DDRM.mult(aF.helix.C, H, tempV);
        aF.R = V - CommonOps_DDRM.dot(H, tempV);

        //System.out.format("MeasurmentSite.filter: R=%10.8f\n", aF.R);
        if (aF.R < 0) {
            if (verbose2) { System.out.format("MeasurementSite.filter: covariance of residual %12.4e is negative\n", aF.R); }
            aF.R = V;
        }

        chi2inc = (aF.r * aF.r) / aF.R;

        if (verbose2) {
            System.out.format("  MeasurementSite.filter: hit=%10.6f pred=%10.6f resid=%10.7f err=%10.7f chi2inc=%10.6f\n", hit.v, aF.mPred, aF.r, FastMath.sqrt(aF.R), chi2inc); 
        }
        filtered = true;
        return true;
    }

    // Inverse Kalman filter: remove this site from the smoothed track fit
    boolean removeHit() {
        if (hitID < 0) return false;
        hitID = -1;
        chi2inc = 0.;
        smoothed = false;
        filtered = false;
        return true;
    }

    Measurement addHit(KalTrack tkr, double cut, double mxTdif, int oldID) {
        if (aP == null) {
            logger.log(Level.WARNING, "******MeasurementSite.addHit: Warning, this site is not in the correct state!");
            // this.print("in the wrong state for hit addition");
            return null;
        }
        double mPred = 0.;
        boolean firstHit = true;
        double chi2incMin = 9999.;
        int theHit = -1;
        double [] tRange = {tkr.tMax - mxTdif, tkr.tMin + mxTdif};
        hitList: for (int hitidx = 0; hitidx < m.hits.size(); hitidx++) {
            if (debug) System.out.format("MeasurementSite.addHit: trying hit %d.  OldID=%d\n", hitidx, oldID);
            if (hitidx == oldID) {
                continue; // don't add a hit that was just removed
            }
            Measurement hit = m.hits.get(hitidx);
            if (debug) hit.print("to try");
            for (KalTrack tkOther: hit.tracks) {
                if (tkOther != tkr) continue hitList; // ignore already used hits
            }
            if (firstHit) {
                double phiS = aP.helix.planeIntersect(m.p);

                // double phiCheck = aF.planeIntersect(m.p);
                // System.out.format("MeasurementSite.filter: phi = %10.7f, phi check = %10.7f\n",phiF, phiCheck);
                if (Double.isNaN(phiS)) { // There may be no intersection if the momentum is too low!
                    break;
                }
                mPred = h(aP, m, phiS);
                firstHit = false;
            }
            double residual = hit.v - mPred;
            double var = hit.sigma * hit.sigma;
            double chi2inc = (residual * residual) / var;
            if (debug) System.out.format("MeasurementSite.addHit:residual=%10.5f, variance=%10.5f, chi2inc=%10.5f\n", residual, var, chi2inc);
            if (chi2inc < chi2incMin) {
                chi2incMin = chi2inc;
                theHit = hitidx;
            }
        }
        if (theHit >= 0) {
            Measurement hit = m.hits.get(theHit);
            if (chi2incMin < kPar.mxResid[0]*kPar.mxResid[0] && hit.time < tRange[1] && hit.time > tRange[0]) {
                hitID = theHit;
                if (filter()) {
                    if (debug) System.out.format("MeasurementSite.addHit: chi2inc from filter = %10.5f\n", chi2inc);
                    if (chi2inc < cut) {
                        if (debug) {
                            System.out.format("MeasurementSite.addHit: success! Adding hit %d on layer %d detector %d  hit=", hitID, m.Layer, m.detector);
                            hit.print("short");
                            System.out.format("\n");
                        }
                        return m.hits.get(hitID);
                    } else {
                        hitID = -1;
                    }
                }
            }
        }
        return null;
    }

    // Produce the smoothed state vector for this site
    boolean smooth(MeasurementSite nS) {
        // nS is the next site in the filtering chain (i.e. the previous site that was smoothed)
        if (!filtered) {
            logger.log(Level.WARNING, "******MeasurementSite.smooth: Warning, this site is not in the correct state!");
            return false;
        }        

        this.aS = this.aF.smooth(nS.aS, nS.aP);
        if (hitID < 0) { 
            smoothed = true;
            return true; 
        }

        Measurement hit = this.m.hits.get(hitID);
        double V = hit.sigma * hit.sigma;
        double phiS = aS.helix.planeIntersect(m.p);

        if (Double.isNaN(phiS)) { // This should almost never happen!
            logger.log(Level.FINE, "MeasurementSite.smooth: no intersection of helix with the plane exists.");
            return false;
        }
        aS.mPred = h(aS, m, phiS);
        aS.r = hit.v - aS.mPred;

        // Recalculate H using the smoothed helix parameters. Usually this makes little difference, but with the
        // non-uniform field this seems to reduce tails significantly in residuals of the last SVT layers.
        buildH(aS, H);

        CommonOps_DDRM.mult(aS.helix.C, H, tempV);
        aS.R = V - CommonOps_DDRM.dot(H, tempV);
        if (aS.R < 0) {
            if (debug) System.out.format("MeasurementSite.smooth, measurement covariance %12.4e is negative\n", aS.R);
            //aS.print("the smoothed state");
            //nS.print("the next site in the chain");
            aS.R = 0.25*V;  // A negative covariance makes no sense, hence this fudge
        }

        chi2inc = (aS.r * aS.r) / aS.R;

        if (debug) System.out.format("  MeasurementSite.smooth: hit=%10.6f pred=%10.6f resid=%10.7f err=%10.7f chi2inc=%8.5f", 
                hit.v, aS.mPred, aS.r, FastMath.sqrt(aS.R), chi2inc);
        this.smoothed = true;
        return true;
    }

    double h(StateVector pS, SiModule siM) {// Predict the measurement for a helix passing through this plane
        double phi = pS.helix.planeIntersect(siM.p);
        if (Double.isNaN(phi)) {
            logger.log(Level.FINE, "MeasurementSite.h: warning, no intersection of helix with the plane exists.");
            phi = 0.;
        }
        return h(pS, siM, phi);
    }

    double h(StateVector pS, SiModule siM, double phi) { // Shortcut call in case phi is already known
        Vec rGlobal = pS.helix.toGlobal(pS.helix.atPhi(phi));
        Vec rLocal = siM.toLocal(rGlobal); // Rotate into the detector coordinate system
        if (debug) {
            rGlobal.print("MeasurementSite.h: global intersection");
            rLocal.print("MeasurementSite.h: local intersection");
            System.out.format("MeasurementSite.h: phi=%12.5e, detector coordinate out of the plane = %10.7f, h=%10.7f\n", phi, rLocal.v[2],
                    rLocal.v[1]);
        }
        return rLocal.v[1];
    }

    // Create the derivative matrix for prediction of the measurement from the helix
    private void buildH(StateVector S, DMatrixRMaj H) {
        double phi = S.helix.planeIntersect(m.p);

        if (Double.isNaN(phi)) { // There may be no intersection if the momentum is too low, but highly unlikely here.
            logger.log(Level.FINE, "MeasurementSite.buildH: no intersection of helix with the plane exists.");
            return;
        }
        if (debug) {
            System.out.format("MeasurementSite.buildH: phi=%10.7f\n", phi);
            // S.print("given to buildH");
            // R.print("in buildH");
            // p.print("in buildH");
        }
        Vec dxdphi = new Vec((alpha / S.helix.a.v[2]) * FastMath.sin(S.helix.a.v[1] + phi), -(alpha / S.helix.a.v[2]) * FastMath.cos(S.helix.a.v[1] + phi),
                -(alpha / S.helix.a.v[2]) * S.helix.a.v[4]);
        double[][] dxda = new double[3][5];
        dxda[0][0] = FastMath.cos(S.helix.a.v[1]);
        dxda[1][0] = FastMath.sin(S.helix.a.v[1]);
        dxda[0][1] = -(S.helix.a.v[0] + alpha / S.helix.a.v[2]) * FastMath.sin(S.helix.a.v[1]) + (alpha / S.helix.a.v[2]) * FastMath.sin(S.helix.a.v[1] + phi);
        dxda[1][1] = (S.helix.a.v[0] + alpha / S.helix.a.v[2]) * FastMath.cos(S.helix.a.v[1]) - (alpha / S.helix.a.v[2]) * FastMath.cos(S.helix.a.v[1] + phi);
        dxda[0][2] = -(alpha / (S.helix.a.v[2] * S.helix.a.v[2])) * (FastMath.cos(S.helix.a.v[1]) - FastMath.cos(S.helix.a.v[1] + phi));
        dxda[1][2] = -(alpha / (S.helix.a.v[2] * S.helix.a.v[2])) * (FastMath.sin(S.helix.a.v[1]) - FastMath.sin(S.helix.a.v[1] + phi));
        dxda[2][2] = (alpha / (S.helix.a.v[2] * S.helix.a.v[2])) * S.helix.a.v[4] * phi;
        dxda[2][3] = 1.0;
        dxda[2][4] = -(alpha / S.helix.a.v[2]) * phi;

        // System.out.format(" Matrix dxda:\n");
        double[] dphida = new double[5];
        double denom = m.p.T().dot(dxdphi);
        for (int i = 0; i < 5; i++) {
            // System.out.format(" %10.6f, %10.6f, %10.6f\n", dxda[0][i], dxda[1][i],
            // dxda[2][i]);
            for (int j = 0; j < 3; j++) { dphida[i] -= m.p.T().v[j] * dxda[j][i]; }
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
        RotMatrix Rt = m.Rinv.multiply(S.helix.Rot.invert());
        for (int i = 0; i < 5; i++) { 
            double HHi = 0;
            for (int j = 0; j < 3; j++) { 
                HHi += Rt.M[1][j] * DxDa[j][i]; 
            }
            H.unsafe_set(i, 0, HHi);
        }

        // Testing the derivatives
        if (debug) {
            StateVector sVp = S.copy();
            double daRel[] = { 0.01, 0.03, -0.02, 0.05, -0.01 };
            for (int i = 0; i < 5; i++) { sVp.helix.a.v[i] = sVp.helix.a.v[i] * (1.0 + daRel[i]); }
            Vec da = new Vec(S.helix.a.v[0] * daRel[0], S.helix.a.v[1] * daRel[1], S.helix.a.v[2] * daRel[2], S.helix.a.v[3] * daRel[3], S.helix.a.v[4] * daRel[4]);
            double phi1 = phi;
            Vec x1Global = S.helix.toGlobal(S.helix.atPhi(phi1));
            Vec x1Local = m.toLocal(x1Global);
            double phi2 = sVp.helix.planeIntersect(m.p);
            Vec x2Global = sVp.helix.toGlobal(sVp.helix.atPhi(phi2));
            double dot2 = x2Global.dif(m.p.X()).dot(m.p.T());
            double dot1 = x1Global.dif(m.p.X()).dot(m.p.T());
            System.out.format("h derivative testing: dot1=%10.8f, dot2=%10.8f, both should be zero\n", dot1, dot2);
            Vec x2Local = m.toLocal(x2Global);

            S.helix.a.print("Test helix parameters");
            // m.p.print("of the measurement");
            sVp.helix.a.print("Modified helix parameters");
            System.out.format("Phi1=%10.7f,  Phi2=%10.7f\n", phi1, phi2);
            x1Global.print("x1");
            x2Global.print("x2");
            for (int j = 0; j < 3; j++) {
                double dx = 0.;
                for (int i = 0; i < 5; i++) { dx += DxDa[j][i] * da.v[i]; }
                System.out.format("j=%d dx=%10.7f,   dxExact=%10.7f\n", j, dx, x2Global.v[j] - x1Global.v[j]);
            }

            double dmExact = x2Local.v[1] - x1Local.v[1];
            double mP1 = h(S, m);
            double mP2 = h(sVp, m);
            x1Local.print("x1Local");
            x2Local.print("x2Local");
            System.out.format("mP1=%10.5f,   mP2=%10.5f\n", mP1, mP2);
            double dm = 0.;
            for (int i = 0; i < 5; i++) { dm += H.get(i,0) * da.v[i]; }
            System.out.format("Test of H matrix: dm=%10.8f,  dmExact=%10.8f\n\n", dm, dmExact);
        }
    }

    // Comparator functions for sorting measurement sites by layer number
    static Comparator<MeasurementSite> SiteComparatorUp = new Comparator<MeasurementSite>() {
        public int compare(MeasurementSite s1, MeasurementSite s2) {
            int lyr1 = s1.m.Layer;
            int lyr2 = s2.m.Layer;
            return lyr1 - lyr2;
        }
    };
    static Comparator<MeasurementSite> SiteComparatorDn = new Comparator<MeasurementSite>() {
        public int compare(MeasurementSite s1, MeasurementSite s2) {
            int lyr1 = s1.m.Layer;
            int lyr2 = s2.m.Layer;
            return lyr2 - lyr1;
        }
    };
}
