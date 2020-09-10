package org.hps.recon.tracking.kalman;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.hps.recon.tracking.MaterialSupervisor.SiStripPlane;
import org.lcsim.event.TrackState;

// Code to propagate an HPS track state to a given plane, through the non-uniform field.
// All of the internal methods used are from the Kalman package.
public class PropagatedTrackState {
    private static final double c = 2.99793e8;
    private static final double SVTcenter = 505.57;
    private static final boolean debug = false;
    private double conFac;
    private List<SiStripPlane> detPlanes;
    private double alphaCenter;
    private TrackState trackState;
    private Logger logger;
    HelixPlaneIntersect hpi;
    private double [] direction;
    private double [] location;
    private Plane destinationPlane;
    private HelixState newHelixState;
    private Vec xPlane;
    private static RotMatrix toHpsGbl;
    private static DMatrixRMaj tempM;
    private static DMatrixRMaj Ft;
    private static boolean initialized;
      
    PropagatedTrackState (TrackState stateHPS, double [] location, double [] direction, List<SiStripPlane> detPlanes, org.lcsim.geometry.FieldMap fM) {
        logger = Logger.getLogger(PropagatedTrackState.class.getName());
        // stateHPS = HPS track state to be propagated
        // location = 3-D point on the plane to which the track is to be propagated
        // direction = direction cosines of the plane (should be more or less perpendicular to the beam axis)
        // detPlanes = complete list of HPS silicon detector planes. If the list is empty, then no MCS is accounted for
        this.detPlanes = detPlanes;
        hpi = new HelixPlaneIntersect();
        this.direction = new double[3];
        this.location = new double[3];
        for (int i=0; i<3; ++i) {
            this.direction[i] = direction[i];
            this.location[i] = location[i];
        }
        
        if (!initialized) {
            toHpsGbl = new RotMatrix();
            toHpsGbl.M[0][0] = 1.0;
            toHpsGbl.M[1][2] = -1.0;
            toHpsGbl.M[2][1] = 1.0;
            tempM = new DMatrixRMaj(5,5);
            Ft = new DMatrixRMaj(5,5);
            initialized = true;
        }
        
        if (debug) {
            System.out.format("Entering PropagatedTrackState for location %10.6f %10.6f %10.6f\n",location[0],location[1],location[2]);
            printTrackState(stateHPS,"initial");
        }
        
        // Convert everything to Kalman coordinates and track parameters        
        Vec refPnt = KalmanInterface.vectorGlbToKalman(stateHPS.getReferencePoint());
        Vec localB = KalmanInterface.getField(refPnt, fM);
        conFac = 1.0e12 / c;
        double B = localB.mag();
        double alpha = conFac / B;
        if (debug) {
            refPnt.print("TrackState reference point");
            localB.print(String.format("local B field with alpha=%10.6f",alpha));
        }
        
        Vec centerB = KalmanInterface.getField(new Vec(0., SVTcenter, 0.), fM);
        alphaCenter = conFac/ centerB.mag();
        if (debug) centerB.print(String.format("B field at HPS center with alpha=%10.6f",alphaCenter));

        Vec localT = localB.unitVec(B);
        Vec yhat = new Vec(0., 1.0, 0.);
        Vec u = yhat.cross(localT).unitVec();
        Vec v = localT.cross(u);
        RotMatrix Rot = new RotMatrix(u, v, localT);  // Rotation to coordinate system aligned with field
        if (debug) {
            Rot.print("to local B-field coordinates");
            Rot.multiply(Rot.invert()).print("unit matrix?");
        }
        
        Vec helixParams = new Vec(5,KalmanInterface.unGetLCSimParams(stateHPS.getParameters(), alphaCenter));
        DMatrixRMaj helixCov = new DMatrixRMaj(KalmanInterface.ungetLCSimCov(stateHPS.getCovMatrix(), alphaCenter));
        Vec pivotOrig = new Vec(0.,0.,0.);
        if (debug) {
            helixParams.print("helix parameters for pivot at origin");
            helixCov.print("covariance of helix parameters");
        }
        
        // Transform helix to a pivot point on the helix itself (so rho0 and z0 become zero)
        // First, find the angle through which the helix turns to get to a plane containing the referencePoint
        // This would not be necessary if we could assume that the referencePoint is exactly on the helix
        Plane refPln = new Plane(refPnt, new Vec(0.,1.,0.));
        double phi = hpi.planeIntersect(helixParams, pivotOrig, alphaCenter, refPln);
        if (Double.isNaN(phi)) {
            logger.fine("There is no intersection of the helix with a plane at reference point " + refPnt.toString());
            return;
        }
        if (debug) System.out.format("Helix turning angle to the reference point = %10.6f\n", phi);
        
        Vec newPivot = HelixState.atPhi(pivotOrig, helixParams, phi, alphaCenter);
        if (debug) {
            refPnt.print("TrackState reference point");
            newPivot.print("Intersection with plane at TrackState reference point");
            Vec pMom = HelixState.getMom(phi, helixParams);
            pMom.print(String.format("initial momentum at the TrackState reference point, p=%10.6f",pMom.mag()));
        }
        Vec helixParamsPivoted = HelixState.pivotTransform(newPivot, helixParams, pivotOrig, alphaCenter, 0.);
        if (debug) {
            helixParamsPivoted.print("helix parameters with pivot on the helix");
            Vec pMom = HelixState.getMom(0., helixParamsPivoted);
            pMom.print(String.format("initial momentum from pivoted helix, p=%10.6f",pMom.mag()));
        }
        DMatrixRMaj F = HelixState.makeF(helixParamsPivoted, helixParams, alpha);
       
        // Then rotate the helix into the B-field reference frame
        DMatrixRMaj fRot = new DMatrixRMaj(5,5);
        Vec newHelixParams = HelixState.rotateHelix(helixParamsPivoted, Rot, fRot); 
        CommonOps_DDRM.mult(F, fRot, Ft); 
        CommonOps_DDRM.multTransB(helixCov, Ft, tempM);
        DMatrixRMaj newHelixCov = new DMatrixRMaj(5,5);
        CommonOps_DDRM.mult(Ft, tempM, newHelixCov);   
        if (debug) {
            newHelixParams.print("helix parameters rotated into the local B-field frame");
            Vec pMom= HelixState.getMom(0., newHelixParams);
            pMom.print(String.format("initial local momentum using rotated helix, p=%10.6f",pMom.mag()));
            Vec pMomGlb = Rot.inverseRotate(pMom);
            pMomGlb.print(String.format("initial momentum using rotated helix, p=%10.6f",pMomGlb.mag()));
        }
        
        // Now we can make a Kalman HelixState
        // The newPivot becomes the origin of the field coordinate system, in which the pivot is at (0,0,0)
        HelixState helixState = new HelixState(newHelixParams, new Vec(0.,0.,0.), newPivot, newHelixCov, B, localT);
        if (debug) {
            helixState.print("at the starting point");
            double phiTst = helixState.planeIntersect(refPln);
            if (!Double.isNaN(phiTst)) {
                Vec intLcl = helixState.atPhi(phiTst);
                intLcl.print("local intersection of helixState");
                Vec intGlb = helixState.toGlobal(intLcl);
                intGlb.print("global intersection of helixState");
            }
        }
        
        Vec pntEnd = KalmanInterface.vectorGlbToKalman(location);
        Vec dirEnd = KalmanInterface.vectorGlbToKalman(direction);
        if (debug) {
            pntEnd.print("destination point");
            dirEnd.print("destination direction");
        }
        
        // Put together a list of scattering planes
        ArrayList<Double> yScat = new ArrayList<Double>();
        ArrayList<Double> XL = new ArrayList<Double>();
        /*
        double [] yv = {103.69,111.75,203.81,211.87,303.76,311.63,505.57,513.08,705.47,713.07,897.,912.89}; // For stand-alone test
        double rho = 2.329; // Density of silicon in g/cm^2
        double radLen = (21.82 / rho) * 10.0; // Radiation length of silicon in millimeters
        for (int i=0; i<12; ++i) {
            yScat.add(yv[i]);
            XL.add(0.3/radLen);
        }
        */
        for (SiStripPlane svtPlane : detPlanes) {
            double[] pointOnPlane = svtPlane.getSensor().getGeometry().getLocalToGlobal().getTranslation().getTranslationVector().v();
            Vec pointOnPlaneTransformed = KalmanInterface.vectorGlbToKalman(pointOnPlane);
            double zKal = pointOnPlaneTransformed.v[2];
            double tanl = helixParams.v[4];
            if (debug) {
                pointOnPlaneTransformed.print("point on plane");
                System.out.format("  zKal=%10.6f,  tanl=%10.6f\n", zKal, tanl);
            }
            if (zKal*tanl < 0.) continue;  // wrong half of the SVT
            double yKal = pointOnPlaneTransformed.v[1];
            double yDir = pntEnd.v[1] - yKal;
            
            if (yDir > 0.) {
                if (yKal < refPnt.v[1] - 2.5 || yKal > pntEnd.v[1]) continue;
            } else {
                if (yKal > refPnt.v[1] - 2.5 || yKal < pntEnd.v[1]) continue;
            }
            
            yScat.add(yKal);
            XL.add(svtPlane.getThicknessInRL());
            if (debug) {
                System.out.format("PropagatedTrackState: scatter at point %s with XL=%9.3f\n", 
                        pointOnPlaneTransformed.toString(), svtPlane.getThicknessInRL());
            }
        }
        
        // Propagate the HelixState to the destination plane       
        destinationPlane = new Plane(pntEnd, dirEnd);
        if (debug) destinationPlane.print("destination");
        newHelixState = helixState.propagateRungeKutta(destinationPlane, yScat, XL, fM);
        if (debug) { 
            newHelixState.print("new, at destination");
            newHelixState.getRKintersection().print("Runge-Kutta intersection with plane");
        }
        
        // Finally turn the HelixState into an HPS TrackState        
        trackState = newHelixState.toTrackState(alphaCenter, destinationPlane, TrackState.AtOther);
        if (debug) printTrackState(trackState,"final");
    }
    
    public TrackState getTrackState() {
        return trackState;
    }
    
    public double [] getIntersection() {
        double phi = newHelixState.planeIntersect(destinationPlane);
        if (Double.isNaN(phi)) {
            logger.fine("getIntersection: there is no intersection with the plane " + destinationPlane.toString());
            phi = 0.;
        }
        xPlane = newHelixState.atPhi(phi);
        Vec xPlaneGlb = newHelixState.toGlobal(xPlane);
        return KalmanInterface.vectorKalmanToGlb(xPlaneGlb);
    }
    
    public double [][] getIntersectionCov() {
        Vec helixAtInt = getIntersectionHelix();
        DMatrixRMaj F = newHelixState.makeF(helixAtInt);
        CommonOps_DDRM.multTransB(newHelixState.C, F, tempM);
        DMatrixRMaj covAtInt = new DMatrixRMaj(5,5);
        CommonOps_DDRM.mult(F, tempM, covAtInt);
        double [][] Dx = KalTrack.DxTOa(helixAtInt);
        double [][] Cx = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Cx[i][j] = 0.;
                for (int k = 0; k < 5; k++) {
                    for (int l = 0; l < 5; l++) {
                        Cx[i][j] += Dx[i][k] * covAtInt.unsafe_get(k,l) * Dx[j][l];
                    }
                }
            }
        }
        SquareMatrix Clocal = new SquareMatrix(3,Cx);
        SquareMatrix Cglobal = Clocal.inverseRotate(newHelixState.Rot);
        SquareMatrix Chps = Cglobal.rotate(toHpsGbl);        
        return Chps.M;
    }
    
    public void print(String s) {
        System.out.format("Dump of PropagatedTrackState %s at plane %s\n", s, destinationPlane.toString());
        printTrackState(trackState,"internal");
        newHelixState.print("new, internal");
        Vec intrst = new Vec(3,getIntersection());
        SquareMatrix intrstCov = new SquareMatrix(3,getIntersectionCov());
        intrst.print("intersection");
        intrstCov.print("intersection covariance");
        System.out.format("End of PropagatedTrackState dump\n");
    }
  
    public static void printTrackState(TrackState trackState, String s) {
        double [] r = trackState.getReferencePoint();
        System.out.format("Dump of TrackState %s at location %d ref. pnt %10.6f %10.6f %10.6f\n", 
                s, trackState.getLocation(), r[0], r[1], r[2]);
        double [] a = trackState.getParameters();
        System.out.format("    Helix parameters = %10.6f %10.6f %10.6f %10.6f %10.6f\n", a[0],a[1],a[2],a[3],a[4]);
        System.out.format("    Covariance matrix:\n");
        double [] C = trackState.getCovMatrix();
        System.out.format("    %12.4e\n", C[0]);
        System.out.format("    %12.4e %12.4e\n", C[1], C[2]);
        System.out.format("    %12.4e %12.4e %12.4e\n", C[3], C[4], C[5]);
        System.out.format("    %12.4e %12.4e %12.4e %12.4e\n", C[6], C[7], C[8], C[9]);
        System.out.format("    %12.4e %12.4e %12.4e %12.4e %12.4e\n", C[10], C[11], C[12], C[13], C[14]);
    }
    
    public double [] getLocation() {
        return location;
    }
    
    public double [] getDirection() {
        return direction;
    }
    
    public List<SiStripPlane> getDetPlanes() {
        return detPlanes;
    }
    
    private Vec getIntersectionHelix() {
        if (xPlane == null) getIntersection();
        Vec helixAtInt = newHelixState.pivotTransform(xPlane);
        return helixAtInt;
    }
    
}
