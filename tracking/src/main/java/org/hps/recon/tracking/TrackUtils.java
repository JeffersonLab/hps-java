package org.hps.recon.tracking;

import hep.physics.matrix.BasicMatrix;
import hep.physics.matrix.Matrix;
import hep.physics.matrix.MatrixOp;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import static java.lang.Math.abs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.Pair;
import org.apache.commons.math.util.FastMath;
import org.hps.recon.tracking.EventQuality.Quality;
import org.hps.recon.tracking.gbl.HelicalTrackStripGbl;
import org.hps.util.RK4integrator;
import org.lcsim.constants.Constants;

import static org.lcsim.constants.Constants.fieldConversion;
import org.lcsim.detector.DetectorElement;

import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.solids.Box;
import org.lcsim.detector.solids.GeomOp3D;
import org.lcsim.detector.solids.Point3D;
import org.lcsim.detector.solids.Polygon3D;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelixParamCalculator;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.fit.helicaltrack.MultipleScatter;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.geometry.subdetector.BarrelEndcapFlag;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.spacegeom.CartesianVector;
import org.lcsim.spacegeom.SpaceVector;
import org.lcsim.util.swim.Helix;
import org.lcsim.util.swim.Line;
import org.lcsim.util.swim.Trajectory;

import org.ejml.data.DMatrix3;

/**
 * Assorted helper functions for the track and helix objects in lcsim. Re-use as
 * much of HelixUtils as possible.
 */
public class TrackUtils {

    public enum RunPeriod {
        EngRun2015,
        EngRun2016,
        PhysRun2019,
        PhysRun2021;
    }

    /**
     * Private constructor to make class static only
     */
    private TrackUtils() {
    }

    public static Hep3Vector extrapolateTrackPositionToSensor(Track track, HpsSiSensor sensor, List<HpsSiSensor> sensors, double bfield) {
        int i = ((sensor.getLayerNumber() + 1) / 2) - 1;
        Hep3Vector extrapPos = TrackStateUtils.getLocationAtSensor(track, sensor, bfield);
        if (extrapPos == null) {
            // no TrackState at this sensor available
            // try to get last available TrackState-at-sensor
            TrackState tmp = TrackStateUtils.getPreviousTrackStateAtSensor(track, sensors, i + 1);
            if (tmp != null) {
                extrapPos = TrackStateUtils.getLocationAtSensor(tmp, sensor, bfield);
            }
            if (extrapPos == null) // now try using TrackState at IP
            {
                extrapPos = TrackStateUtils.getLocationAtSensor(TrackStateUtils.getTrackStateAtIP(track), sensor, bfield);
            }
        }
        return extrapPos;
    }

    //    public static Hep3Vector extrapolateTrackPositionToSensorRK(TrackState ts, SiStripPlane sens, Hep3Vector startPos, FieldMap fM) {
    //        if (ts == null || sens == null)
    //            return null;
    //        HpsSiSensor sensor = (HpsSiSensor) (sens.getSensor());
    //        if ((ts.getTanLambda() > 0 && sensor.isTopLayer()) || (ts.getTanLambda() < 0 && sensor.isBottomLayer())) {
    //            // distance to extrapolate
    //            double dPerp = Math.abs(VecOp.dot(VecOp.sub(startPos, sens.origin()), sens.normal()));
    //            Hep3Vector pHat = VecOp.mult(1.0 / startPos.magnitude(), startPos);
    //            double distance = dPerp / VecOp.dot(pHat, sens.normal());
    //            TrackState bts = extrapolateTrackUsingFieldMapRK(ts, startPos, distance, 0, fM, new BasicHep3Vector(0, 0, 0));
    //            return getHelixPlaneIntercept(getHTF(bts), sens.normal(), sens.origin(), fM.getField(sens.origin()).y());
    //        }
    //        return null;
    //    }
    //
    //    public static Hep3Vector extrapolateTrackPositionToSensorRK(TrackState ts, SiStripPlane sens1, SiStripPlane sens2, FieldMap fM) {
    //        HpsSiSensor sensor1 = (HpsSiSensor) (sens1.getSensor());
    //        double bfieldY = fM.getField(sens1.origin()).y();
    //        Hep3Vector startPos = TrackStateUtils.getLocationAtSensor(ts, sensor1, bfieldY);
    //        return extrapolateTrackPositionToSensorRK(ts, sens2, startPos, fM);
    //    }
    /**
     * Extrapolate track to a position along the x-axis. Turn the track into a
     * helix object in order to use HelixUtils.
     *
     * @param track
     * @param x
     * @return the position along the x-axis
     */
    public static Hep3Vector extrapolateHelixToXPlane(Track track, double x) {
        return extrapolateHelixToXPlane(getHTF(track), x);
    }

    public static Hep3Vector extrapolateHelixToXPlane(TrackState track, double x) {
        return extrapolateHelixToXPlane(getHTF(track), x);
    }

    public static double[] getParametersFromPointAndMomentum(Hep3Vector point, Hep3Vector momentum, int charge, double BField) {

        double px = momentum.x();
        double py = momentum.y();
        double pz = momentum.z();

        //wrong =
        double R = charge * momentum.magnitude() / (Constants.fieldConversion * BField);

        //correct
        //double pt = Math.sqrt(px*px + py*py);
        //double R = charge * pt / (Constants.fieldConversion * BField);
        double tanL = calculateTanLambda(pz, momentum.magnitude());
        double phi = calculatePhi(px, py);
        //reference position is at x=pointX, y=pointY, z=0
        //so dca=0, z0=pointZ
        double dca = 0;
        double z0 = point.z();

        double[] params = new double[5];
        params[HelicalTrackFit.phi0Index] = phi;
        params[HelicalTrackFit.curvatureIndex] = 1.0 / R;
        params[HelicalTrackFit.dcaIndex] = dca;
        params[HelicalTrackFit.slopeIndex] = tanL;
        params[HelicalTrackFit.z0Index] = z0;

        return params;
    }

    /*      
    *   mg 9/11/2017  ... get the perigee parameters from a known point on the helix and momentum at that point
    *   I though this was done elsewhere, but stuff like the HelixParamCaluculator seems to give crappy answers
    *   this is particularly useful for getting the helix parameters for MC truth
     */
    @Deprecated
    public static double[] getParametersFromPointAndMomentum(Hep3Vector point, Hep3Vector momentum, int charge, double BField, boolean writeIt) throws FileNotFoundException {

        //print out the original trajectory
        if (writeIt) {
            writeTrajectory(momentum, point, charge, BField, "orig-point-and-mom.txt");
        }
        //first, get the curvature
        //Calculate theta, the of the helix projected into an SZ plane, from the z axis
        double px = momentum.x();
        double py = momentum.y();
        double pz = momentum.z();
        double pt = Math.sqrt(px * px + py * py);
        //double p = Math.sqrt(pt * pt + pz * pz);
        //        double cth = pz / p;
        //        double theta = Math.acos(cth);
        //        System.out.println("pt = "+pt+"; costh = "+cth);

        //Calculate Radius of the Helix
        double R = charge * pt / (Constants.fieldConversion * BField);
        //Slope in the Dz/Ds sense, tanL Calculation
        double tanL = pz / pt;
        //  Azimuthal direction at point
        //double phi = Math.atan2(py, px);

        double phi = FastMath.atan2(py, px);

        //reference position is at x=pointX, y=pointY, z=0
        //so dca=0, z0=pointZ
        double dca = 0;
        double z0 = point.z();

        double[] params = new double[5];
        params[HelicalTrackFit.phi0Index] = phi;
        params[HelicalTrackFit.curvatureIndex] = 1 / R;
        params[HelicalTrackFit.dcaIndex] = dca;
        params[HelicalTrackFit.slopeIndex] = tanL;
        params[HelicalTrackFit.z0Index] = z0;
        //System.out.println("Orig  :  d0 = " + params[HelicalTrackFit.dcaIndex] + "; phi0 = " + params[HelicalTrackFit.phi0Index] + "; curvature = " + params[HelicalTrackFit.curvatureIndex] + "; z0 = " + params[HelicalTrackFit.z0Index] + "; slope = " + params[HelicalTrackFit.slopeIndex]);

        //ok, now shift these to the new reference frame, recalculating the new perigee parameters        
        double[] oldReferencePoint = {point.x(), point.y(), 0};
        double[] newReferencePoint = {0, 0, 0};

        //System.out.println("MC origin : x = " + point.x() + "; y = " + point.y() + ";  z = " + point.z());
        double[] newParameters = getParametersAtNewRefPoint(newReferencePoint, oldReferencePoint, params);
        //System.out.println("New  :  d0 = " + newParameters[HelicalTrackFit.dcaIndex] + "; phi0 = " + newParameters[HelicalTrackFit.phi0Index] + "; curvature = " + newParameters[HelicalTrackFit.curvatureIndex] + "; z0 = " + newParameters[HelicalTrackFit.z0Index] + "; slope = " + newParameters[HelicalTrackFit.slopeIndex]);
        //print the trajectory after shift.  Should be the same!!!        
        if (writeIt) {
            writeTrajectory(getMomentum(newParameters[HelicalTrackFit.curvatureIndex], newParameters[HelicalTrackFit.phi0Index], newParameters[HelicalTrackFit.slopeIndex], BField), getPoint(newParameters[HelicalTrackFit.dcaIndex], newParameters[HelicalTrackFit.phi0Index], newParameters[HelicalTrackFit.z0Index]), charge, BField, "final-point-and-mom.txt");
        }

        return newParameters;
    }

    /**
     * Change reference point of helix (following L3 Internal Note 1666.)
     *
     * @param newRefPoint - The new reference point in XY
     */
    public static double[] getParametersAtNewRefPoint(double[] newRefPoint, TrackState trkState) {
        return getParametersAtNewRefPoint(newRefPoint, trkState.getReferencePoint(), trkState.getParameters());
    }

    /**
     * Change reference point of helix (following L3 Internal Note 1666.)
     *
     * @param newRefPoint - The new reference point in XY
     */
    public static double[] getParametersAtNewRefPoint(double[] newRefPoint, HpsHelicalTrackFit helicalTrackFit) {
        return getParametersAtNewRefPoint(newRefPoint, helicalTrackFit.getRefPoint(), helicalTrackFit.parameters());
    }

    /**
     * Change reference point of helix (following L3 Internal Note 1666.)
     *
     * @param newRefPoint - The new reference point in XY
     */
    public static double[] getParametersAtNewRefPoint(double[] newRefPoint, double[] __refPoint, double[] parameters) {

        double phi0 = parameters[HelicalTrackFit.phi0Index];
        double curvature = parameters[HelicalTrackFit.curvatureIndex];
        double dca = parameters[HelicalTrackFit.dcaIndex];
        double slope = parameters[HelicalTrackFit.slopeIndex];
        double z0 = parameters[HelicalTrackFit.z0Index];

        double dx = newRefPoint[0] - __refPoint[0];
        double dy = newRefPoint[1] - __refPoint[1];
        double phinew = phi0;
        double dcanew = dca;
        double z0new = z0;

        // take care of phi0 range if needed (this matters for dphi below I
        // think)
        // L3 defines it in the range [-pi,pi]
        while (phi0 > Math.PI / 2) {
            phi0 -= Math.PI;
        }
        while (phi0 < -Math.PI / 2) {
            phi0 += Math.PI;
        }

        double sinphi = Math.sin(phi0);
        double cosphi = Math.cos(phi0);

        if (curvature != 0) {
            double R = 1.0 / curvature;
            // calculate new phi
            //phinew = Math.atan2(sinphi - dx / (R - dca), cosphi + dy / (R - dca));
            //System.out.println("PF::DEBUG::phinew:="+phinew);
            phinew = FastMath.atan2(sinphi - dx / (R - dca), cosphi + dy / (R - dca));

            // difference in phi
            // watch out for ambiguity        
            double dphi = phinew - phi0;
            if (Math.abs(dphi) > Math.PI) {
                System.out.println("TrackUtils::WARNING::dphi is large " + dphi + " from phi0 " + phi0 + " and phinew " + phinew + " take care of the ambiguity!!??");
            }

            // calculate new dca
            dcanew += dx * sinphi - dy * cosphi + (dx * cosphi + dy * sinphi) * Math.tan(dphi / 2.);

            // path length from old to new point
            double s = -1.0 * dphi / curvature;
            double dz = 0.;
            if (newRefPoint.length == 3) {
                dz = newRefPoint[2] - __refPoint[2];
            }
            // new z0
            z0new += s * slope - dz;
            //z0new += s * slope;
        } else {
            dcanew += dx * sinphi - dy * cosphi;
            double dz = newRefPoint[2] - __refPoint[2];
            double s = Math.sqrt(dz * dz + dy * dy + dx * dx);
            z0new += s * slope * -1.0 * Math.signum(dz);
        }

        // new array
        double[] params = new double[5];
        params[HelicalTrackFit.phi0Index] = phinew;
        params[HelicalTrackFit.curvatureIndex] = curvature;
        params[HelicalTrackFit.dcaIndex] = dcanew;
        params[HelicalTrackFit.slopeIndex] = slope;
        params[HelicalTrackFit.z0Index] = z0new;
        return params;
    }

    /*
     *  get the helix perigee parameters covariance matrix at the new reference point. 
     *  this only covers translations of the reference point...not rotations or "stretching" of the axes
     *  mgraham 7/3/2017
     */
    public static SymmetricMatrix getCovarianceAtNewRefPoint(double[] newRefPoint, double[] __refPoint, double[] par, SymmetricMatrix helixcov) {

        double dx = newRefPoint[0] - __refPoint[0];
        double dy = newRefPoint[1] - __refPoint[1];
        double dz = newRefPoint[2] - __refPoint[2];

        double d0 = par[HelicalTrackFit.dcaIndex];
        double phi0 = par[HelicalTrackFit.phi0Index];
        double rho = par[HelicalTrackFit.curvatureIndex];
        double z0 = par[HelicalTrackFit.z0Index];
        double tanLambda = par[HelicalTrackFit.slopeIndex];

        while (phi0 > Math.PI) {
            phi0 -= Math.PI * 2;
        }
        while (phi0 < -Math.PI / 2) {
            phi0 += Math.PI;
        }

        BasicMatrix jac = new BasicMatrix(5, 5);
        //
        //        //the jacobian elements below are copied & pasted from mg's mathemematica notebook 7/5/17
        jac.setElement(0, 0, (Power(rho, 2) * (-1 + d0 * rho) * Power(Sec((phi0 - ArcTan((dy * rho) / (1 - d0 * rho) + Cos(phi0), (dx * rho) / (-1 + d0 * rho) + Sin(phi0))) / 2.), 2) * Power(dx * Cos(phi0) + dy * Sin(phi0), 2) + 2 * (1 - d0 * rho) * (Power(dy * rho + Cos(phi0) - d0 * rho * Cos(phi0), 2) + Power(dx * rho + (-1 + d0 * rho) * Sin(phi0), 2))) / (2. * (1 - d0 * rho) * (Power(dy * rho + Cos(phi0) - d0 * rho * Cos(phi0), 2) + Power(dx * rho + (-1 + d0 * rho) * Sin(phi0), 2))));

        jac.setElement(0, 1, dx * Cos(phi0) + dy * Sin(phi0) - (rho * Power(Sec((phi0 - ArcTan((dy * rho) / (1 - d0 * rho) + Cos(phi0), (dx * rho) / (-1 + d0 * rho) + Sin(phi0))) / 2.), 2) * (dx * Cos(phi0) + dy * Sin(phi0)) * ((Power(dx, 2) + Power(dy, 2)) * rho + (dy - d0 * dy * rho) * Cos(phi0) + dx * (-1 + d0 * rho) * Sin(phi0))) / (2. * (1 - 2 * d0 * rho + Power(d0, 2) * Power(rho, 2) + Power(dx, 2) * Power(rho, 2) + Power(dy, 2) * Power(rho, 2) - 2 * dy * rho * (-1 + d0 * rho) * Cos(phi0) + 2 * dx * rho * (-1 + d0 * rho) * Sin(phi0))) + (dy * Cos(phi0) - dx * Sin(phi0)) * Tan((-phi0 + ArcTan((dy * rho) / (1 - d0 * rho) + Cos(phi0), (dx * rho) / (-1 + d0 * rho) + Sin(phi0))) / 2.));

        jac.setElement(0, 2, -(Power(Sec((-phi0 + ArcTan((dy * rho) / (1 - d0 * rho) + Cos(phi0), (dx * rho) / (-1 + d0 * rho) + Sin(phi0))) / 2.), 2) * Power(dx * Cos(phi0) + dy * Sin(phi0), 2)) / (2. * (1 - 2 * d0 * rho + Power(d0, 2) * Power(rho, 2) + Power(dx, 2) * Power(rho, 2) + Power(dy, 2) * Power(rho, 2) - 2 * dy * rho * (-1 + d0 * rho) * Cos(phi0) + 2 * dx * rho * (-1 + d0 * rho) * Sin(phi0))));

        jac.setElement(1, 0, -((Power(rho, 2) * (dx * Cos(phi0) + dy * Sin(phi0))) / (1 - 2 * d0 * rho + Power(d0, 2) * Power(rho, 2) + Power(dx, 2) * Power(rho, 2) + Power(dy, 2) * Power(rho, 2) - 2 * dy * rho * (-1 + d0 * rho) * Cos(phi0) + 2 * dx * rho * (-1 + d0 * rho) * Sin(phi0))));

        jac.setElement(1, 1, ((-1 + d0 * rho) * (-1 + d0 * rho - dy * rho * Cos(phi0) + dx * rho * Sin(phi0))) / (1 - 2 * d0 * rho + Power(d0, 2) * Power(rho, 2) + Power(dx, 2) * Power(rho, 2) + Power(dy, 2) * Power(rho, 2) - 2 * dy * rho * (-1 + d0 * rho) * Cos(phi0) + 2 * dx * rho * (-1 + d0 * rho) * Sin(phi0)));

        jac.setElement(1, 2, -((dx * Cos(phi0) + dy * Sin(phi0)) / (1 - 2 * d0 * rho + Power(d0, 2) * Power(rho, 2) + Power(dx, 2) * Power(rho, 2) + Power(dy, 2) * Power(rho, 2) - 2 * dy * rho * (-1 + d0 * rho) * Cos(phi0) + 2 * dx * rho * (-1 + d0 * rho) * Sin(phi0))));

        jac.setElement(3, 0, (rho * tanLambda * (dx * Cos(phi0) + dy * Sin(phi0))) / (1 - 2 * d0 * rho + Power(d0, 2) * Power(rho, 2) + Power(dx, 2) * Power(rho, 2) + Power(dy, 2) * Power(rho, 2) - 2 * dy * rho * (-1 + d0 * rho) * Cos(phi0) + 2 * dx * rho * (-1 + d0 * rho) * Sin(phi0)));

        jac.setElement(3, 1, (tanLambda * ((Power(dx, 2) + Power(dy, 2)) * rho + (dy - d0 * dy * rho) * Cos(phi0) + dx * (-1 + d0 * rho) * Sin(phi0))) / (1 - 2 * d0 * rho + Power(d0, 2) * Power(rho, 2) + Power(dx, 2) * Power(rho, 2) + Power(dy, 2) * Power(rho, 2) - 2 * dy * rho * (-1 + d0 * rho) * Cos(phi0) + 2 * dx * rho * (-1 + d0 * rho) * Sin(phi0)));

        jac.setElement(3, 2, (tanLambda * (ArcTan((dy * rho) / (1 - d0 * rho) + Cos(phi0), (dx * rho) / (-1 + d0 * rho) + Sin(phi0)) + (dx * rho * Cos(phi0) + dy * rho * Sin(phi0) - phi0 * (Power(dy * rho + Cos(phi0) - d0 * rho * Cos(phi0), 2) + Power(dx * rho + (-1 + d0 * rho) * Sin(phi0), 2))) / (Power(dy * rho + Cos(phi0) - d0 * rho * Cos(phi0), 2) + Power(dx * rho + (-1 + d0 * rho) * Sin(phi0), 2)))) / Power(rho, 2));

        jac.setElement(3, 4, (phi0 - ArcTan((dy * rho) / (1 - d0 * rho) + Cos(phi0), (dx * rho) / (-1 + d0 * rho) + Sin(phi0))) / rho);

        jac.setElement(2, 2, 1);
        jac.setElement(3, 3, 1);
        jac.setElement(4, 4, 1);

        Matrix covMatrix = new BasicMatrix(helixcov);
        Matrix jacT = MatrixOp.transposed(jac);
        Matrix first = MatrixOp.mult(covMatrix, jacT);
        Matrix newcov = MatrixOp.mult(jac, first);
        //System.out.println(newcov.getNColumns() + " x  " + newcov.getNRows());

        return new SymmetricMatrix(newcov);
    }

    /**
     * Extrapolate helix to a position along the x-axis. Re-use HelixUtils.
     *
     * @param htf
     * @param x
     * @return the position along the x-axis
     */
    public static Hep3Vector extrapolateHelixToXPlane(HelicalTrackFit htf, double x) {
        double s = HelixUtils.PathToXPlane(htf, x, 0., 0).get(0);
        return HelixUtils.PointOnHelix(htf, s);
    }

    // ==========================================================================
    // Helper functions for track parameters and commonly used derived variables
    // first set are there for backwards comp.
    public static double getPhi(Track track, Hep3Vector position) {
        return getPhi(track.getTrackStates().get(0), position);
    }

    public static double getX0(Track track) {
        return getX0(track.getTrackStates().get(0));
    }

    public static double getR(Track track) {
        return getR(track.getTrackStates().get(0));
    }

    public static double getY0(Track track) {
        return getY0(track.getTrackStates().get(0));
    }

    public static double getDoca(Track track) {
        return getDoca(track.getTrackStates().get(0));
    }

    public static double getPhi0(Track track) {
        return getPhi0(track.getTrackStates().get(0));
    }

    public static double getZ0(Track track) {
        return getZ0(track.getTrackStates().get(0));
    }

    public static double getTanLambda(Track track) {
        return getTanLambda(track.getTrackStates().get(0));
    }

    public static double getSinTheta(Track track) {
        return 1 / Math.sqrt(1 + Math.pow(getTanLambda(track), 2));
    }

    public static double getCosTheta(Track track) {
        return getTanLambda(track) / Math.sqrt(1 + Math.pow(getTanLambda(track), 2));
    }

    public static double getPhi(TrackState track, Hep3Vector position) {
        double x = Math.sin(getPhi0(track)) - (1 / getR(track)) * (position.x() - getX0(track));
        double y = Math.cos(getPhi0(track)) + (1 / getR(track)) * (position.y() - getY0(track));
        return FastMath.atan2(x, y);// mg 9/20/17...I think this is the wrong order...should be atan2(y,x)
    }

    public static double getX0(TrackState track) {
        return -1 * getDoca(track) * Math.sin(getPhi0(track));
    }

    public static double getX0(double doca, double phi0) {
        return -1 * doca * Math.sin(phi0);
    }

    public static double getR(TrackState track) {
        return 1.0 / track.getOmega();
    }

    public static double getY0(TrackState track) {
        return getDoca(track) * Math.cos(getPhi0(track));
    }

    public static double getY0(double doca, double phi0) {
        return doca * Math.cos(phi0);
    }

    public static double getDoca(TrackState track) {
        return track.getD0();
    }

    public static double getPhi0(TrackState track) {
        return track.getPhi();
    }

    public static double getZ0(TrackState track) {
        return track.getZ0();
    }

    public static Hep3Vector getPoint(double doca, double phi0, double z0) {
        return new BasicHep3Vector(getX0(doca, phi0), getY0(doca, phi0), z0);
    }

    public static double getTanLambda(TrackState track) {
        return track.getTanLambda();
    }

    public static double getSinTheta(TrackState track) {
        return 1 / Math.sqrt(1 + Math.pow(getTanLambda(track), 2));
    }

    public static double getCosTheta(TrackState track) {
        return getTanLambda(track) / Math.sqrt(1 + Math.pow(getTanLambda(track), 2));
    }

    public static int getCharge(Track track) {
        return -(int) Math.signum(getR(track));
    }

    // ==========================================================================
    /**
     * Calculate the point of interception between the helix and a plane in
     * space. Uses an iterative procedure. This function makes assumptions on
     * the sign and convecntion of the B-field. Be careful.
     *
     * @param helfit - helix
     * @param unit_vec_normal_to_plane - unit vector normal to the plane
     * @param point_on_plane - point on the plane
     * @param bfield - magnetic field value
     * @return point at intercept
     */
    public static Hep3Vector getHelixPlaneIntercept(HelicalTrackFit helfit, Hep3Vector unit_vec_normal_to_plane, Hep3Vector point_on_plane, double bfield) {
        return getHelixPlaneIntercept(helfit, unit_vec_normal_to_plane, point_on_plane, bfield, 0);
    }

    public static Hep3Vector getHelixPlaneIntercept(HelicalTrackFit helfit, Hep3Vector unit_vec_normal_to_plane, Hep3Vector point_on_plane, double bfield, double initial_s) {
        boolean debug = false;
        // Hep3Vector B = new BasicHep3Vector(0, 0, -1);
        // WTrack wtrack = new WTrack(helfit, -1.0*bfield); //
        if (Math.abs(point_on_plane.x() - helfit.xc()) > Math.abs(helfit.R())) {
            return null;
        }
        Hep3Vector B = new BasicHep3Vector(0, 0, 1);
        DMatrix3 B_ejml = new DMatrix3(B.x(), B.y(), B.z());
        DMatrix3 unit_vec_normal_to_plane_ejml = new DMatrix3(unit_vec_normal_to_plane.x(), unit_vec_normal_to_plane.y(), unit_vec_normal_to_plane.z());
        DMatrix3 point_on_plane_ejml = new DMatrix3(point_on_plane.x(), point_on_plane.y(), point_on_plane.z());

        WTrack wtrack = new WTrack(helfit, bfield); //
        if (initial_s != 0 && initial_s != Double.NaN) //wtrack.setTrackParameters(wtrack.getHelixParametersAtPathLength(initial_s, B));
        {
            wtrack.setTrackParameters(wtrack.getHelixParametersAtPathLength_ejml(initial_s, B_ejml));
        }
        if (debug) {
            System.out.printf("getHelixPlaneIntercept:find intercept between plane defined by point on plane %s, unit vec %s, bfield %.3f, h=%s and WTrack \n%s \n", point_on_plane.toString(), unit_vec_normal_to_plane.toString(), bfield, B.toString(), wtrack.toString());
        }
        try {
            //Hep3Vector intercept_point = wtrack.getHelixAndPlaneIntercept(point_on_plane, unit_vec_normal_to_plane, B);
            Hep3Vector intercept_point = wtrack.getHelixAndPlaneIntercept_ejml(point_on_plane_ejml, unit_vec_normal_to_plane_ejml, B_ejml);
            if (debug) {
                System.out.printf("getHelixPlaneIntercept: found intercept point at %s\n", intercept_point.toString());
            }
            return intercept_point;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Calculate the point of interception between the helix and a plane in
     * space. Uses an iterative procedure.
     *
     * @param helfit - helix
     * @param strip - strip cluster that will define the plane
     * @param bfield - magnetic field value
     * @return point at intercept
     */
    public static Hep3Vector getHelixPlaneIntercept(HelicalTrackFit helfit, HelicalTrackStripGbl strip, double bfield) {
        Hep3Vector point_on_plane = strip.origin();
        if (Math.abs(point_on_plane.x() - helfit.xc()) > Math.abs(helfit.R())) {
            return null;
        }
        Hep3Vector unit_vec_normal_to_plane = VecOp.cross(strip.u(), strip.v());// strip.w();
        double s_origin = HelixUtils.PathToXPlane(helfit, point_on_plane.x(), 0., 0).get(0);
        Hep3Vector intercept_point = getHelixPlaneIntercept(helfit, unit_vec_normal_to_plane, point_on_plane, bfield, s_origin);
        return intercept_point;
    }

    /**
     * Get position of a track extrapolated to the HARP in the HPS test run 2012
     *
     * @param track
     * @return position at HARP
     */
    @Deprecated
    public static Hep3Vector getTrackPositionAtHarp(Track track) {
        return extrapolateTrack(track, BeamlineConstants.HARP_POSITION_TESTRUN);
    }

    //******* This block can be generalized! *******//
    //Default step size
    public static BaseTrackState getTrackExtrapAtVtxSurfRK(Track trk, FieldMap fM, double distanceZ) {
        return getTrackExtrapAtVtxSurfRK(trk, fM, 0, distanceZ);
    }

    //For the moment I use IP, but I should use first sensor!!
    public static BaseTrackState getTrackExtrapAtVtxSurfRK(Track trk, FieldMap fM, double stepSize, double distanceZ) {
        BaseTrackState ts = (BaseTrackState) TrackStateUtils.getTrackStateAtFirst(trk);
        if (ts != null) {
            return getTrackExtrapAtVtxSurfRK(ts, fM, stepSize, distanceZ);
        }
        return null;
    }

    public static BaseTrackState getTrackExtrapAtVtxSurfRK(TrackState ts, FieldMap fM, double stepSize, double distanceZ) {
        //Change of charge
        Hep3Vector startPos = extrapolateHelixToXPlane(ts, 0.);
        Hep3Vector startPosTrans = CoordinateTransformations.transformVectorToDetector(startPos);
        double charge = -1.0 * Math.signum(getR(ts));

        //Extrapolate
        org.hps.util.Pair<Hep3Vector, Hep3Vector> RKresults = extrapolateTrackUsingFieldMapRK(ts, startPosTrans, distanceZ, stepSize, fM);
        //Position
        Hep3Vector posTrans = CoordinateTransformations.transformVectorToTracking(RKresults.getFirstElement());
        //Momentum
        Hep3Vector momTrans = CoordinateTransformations.transformVectorToTracking(RKresults.getSecondElement());

        double bFieldY = fM.getField(RKresults.getFirstElement()).y();

        //Correct if it didn't arrive to it
        Hep3Vector finalPos = posTrans;
        if (RKresults.getFirstElement().z() != distanceZ) {
            Hep3Vector mom = RKresults.getSecondElement();
            double dz = distanceZ - RKresults.getFirstElement().z();
            double dy = dz * mom.y() / mom.z();
            double dx = dz * mom.x() / mom.z();
            Hep3Vector dPos = new BasicHep3Vector(dx, dy, dz);
            finalPos = CoordinateTransformations.transformVectorToTracking(VecOp.add(dPos, RKresults.getFirstElement()));
        }
        bFieldY = fM.getField(CoordinateTransformations.transformVectorToDetector(finalPos)).y();
        double[] params = getParametersFromPointAndMomentum(finalPos, momTrans, (int) charge, bFieldY);
        BaseTrackState bts = new BaseTrackState(params, bFieldY);
        bts.setReferencePoint(finalPos.v());
        bts.setLocation(TrackState.AtVertex);
        return bts;
    }

    /**
     * Get position of a track extrapolated to the ECAL face in the HPS test run
     * 2012
     *
     * @param track
     * @return position at ECAL
     */
    public static BaseTrackState getTrackExtrapAtEcal(Track track, FieldMap fieldMap, int runNumber) {
        TrackState stateAtLast = TrackUtils.getTrackStateAtLocation(track, TrackState.AtLastHit);
        if (stateAtLast == null) {
            return null;
        }
        return getTrackExtrapAtEcal(stateAtLast, fieldMap, runNumber);
    }

    public static BaseTrackState getTrackExtrapAtEcal(TrackState track, FieldMap fieldMap, int runNumber) {
        // extrapolateTrackUsingFieldMap(TrackState track, double startPositionX, double endPosition, double stepSize, FieldMap fieldMap)
        double zAtEcal = BeamlineConstants.ECAL_FACE;
        if (4441 < runNumber && runNumber < 8100)
            zAtEcal = BeamlineConstants.ECAL_FACE_ENGINEERING_RUNS;
        
        BaseTrackState bts = extrapolateTrackUsingFieldMap(track, BeamlineConstants.DIPOLE_EDGE_ENG_RUN, zAtEcal, 5.0, fieldMap);
        bts.setLocation(TrackState.AtCalorimeter);
        return bts;
    }

    public static BaseTrackState getTrackExtrapAtEcalRK(TrackState ts, FieldMap fM, int runNumber) {
        return getTrackExtrapAtEcalRK(ts, fM, 0, runNumber);
    }

    public static BaseTrackState getTrackExtrapAtEcalRK(TrackState ts, FieldMap fM, double stepSize, int runNumber) {

        double zAtEcal = BeamlineConstants.ECAL_FACE;
        if (4441 < runNumber && runNumber < 8100)
            zAtEcal = BeamlineConstants.ECAL_FACE_ENGINEERING_RUNS;
 
        Hep3Vector startPos = extrapolateHelixToXPlane(ts, BeamlineConstants.DIPOLE_EDGE_ENG_RUN);
        Hep3Vector startPosTrans = CoordinateTransformations.transformVectorToDetector(startPos);
        double distanceZ = zAtEcal - BeamlineConstants.DIPOLE_EDGE_ENG_RUN;
        double charge = -1.0 * Math.signum(getR(ts));

        org.hps.util.Pair<Hep3Vector, Hep3Vector> RKresults = extrapolateTrackUsingFieldMapRK(ts, startPosTrans, distanceZ, stepSize, fM);
        double bFieldY = fM.getField(RKresults.getFirstElement()).y();
        Hep3Vector posTrans = CoordinateTransformations.transformVectorToTracking(RKresults.getFirstElement());
        Hep3Vector momTrans = CoordinateTransformations.transformVectorToTracking(RKresults.getSecondElement());

        Hep3Vector finalPos = posTrans;
        if (RKresults.getFirstElement().z() != zAtEcal) {
            Hep3Vector mom = RKresults.getSecondElement();
            double dz = zAtEcal - RKresults.getFirstElement().z();
            double dy = dz * mom.y() / mom.z();
            double dx = dz * mom.x() / mom.z();
            Hep3Vector dPos = new BasicHep3Vector(dx, dy, dz);
            finalPos = CoordinateTransformations.transformVectorToTracking(VecOp.add(dPos, RKresults.getFirstElement()));
        }
        bFieldY = fM.getField(CoordinateTransformations.transformVectorToDetector(finalPos)).y();
        double[] params = getParametersFromPointAndMomentum(finalPos, momTrans, (int) charge, bFieldY);
        BaseTrackState bts = new BaseTrackState(params, bFieldY);
        bts.setReferencePoint(finalPos.v());
        bts.setLocation(TrackState.AtCalorimeter);
        return bts;
    }

    public static BaseTrackState getTrackExtrapAtEcalRK(Track trk, FieldMap fM, double stepSize, int runNumber) {
        BaseTrackState ts = (BaseTrackState) TrackStateUtils.getTrackStateAtLast(trk);
        if (ts != null) {
            return getTrackExtrapAtEcalRK(ts, fM, stepSize, runNumber);
        }
        return null;
    }

    public static BaseTrackState getTrackExtrapAtEcalRK(Track trk, FieldMap fM, int runNumber) {
        return getTrackExtrapAtEcalRK(trk, fM, 0, runNumber);
    }

    /**
     * Get track extrapolation to hodoscope layers
     *
     */
    public static BaseTrackState getTrackExtrapAtHodoRK(TrackState ts, FieldMap fM, int hodoLayer) {
        return getTrackExtrapAtHodoRK(ts, fM, 0, hodoLayer);
    }

    public static BaseTrackState getTrackExtrapAtHodoRK(TrackState ts, FieldMap fM, double stepSize, int hodoLayer) {
        Hep3Vector startPos = extrapolateHelixToXPlane(ts, BeamlineConstants.DIPOLE_EDGE_ENG_RUN);
        Hep3Vector startPosTrans = CoordinateTransformations.transformVectorToDetector(startPos);
        double distZHodo = BeamlineConstants.HODO_L1_ZPOS;
        int hodoTrackStateIndex = 0;
        if (hodoLayer == 2) {
            distZHodo = BeamlineConstants.HODO_L2_ZPOS; //            hodoTrackStateIndex = 7;
        }
        double distanceZ = distZHodo - BeamlineConstants.DIPOLE_EDGE_ENG_RUN;
        double charge = -1.0 * Math.signum(getR(ts));

        org.hps.util.Pair<Hep3Vector, Hep3Vector> RKresults = extrapolateTrackUsingFieldMapRK(ts, startPosTrans, distanceZ, stepSize, fM);
        double bFieldY = fM.getField(RKresults.getFirstElement()).y();
        Hep3Vector posTrans = CoordinateTransformations.transformVectorToTracking(RKresults.getFirstElement());
        Hep3Vector momTrans = CoordinateTransformations.transformVectorToTracking(RKresults.getSecondElement());

        Hep3Vector finalPos = posTrans;
        if (RKresults.getFirstElement().z() != distZHodo) {
            Hep3Vector mom = RKresults.getSecondElement();
            double dz = distZHodo - RKresults.getFirstElement().z();
            double dy = dz * mom.y() / mom.z();
            double dx = dz * mom.x() / mom.z();
            Hep3Vector dPos = new BasicHep3Vector(dx, dy, dz);
            finalPos = CoordinateTransformations.transformVectorToTracking(VecOp.add(dPos, RKresults.getFirstElement()));
        }
        bFieldY = fM.getField(CoordinateTransformations.transformVectorToDetector(finalPos)).y();
        double[] params = getParametersFromPointAndMomentum(finalPos, momTrans, (int) charge, bFieldY);
        BaseTrackState bts = new BaseTrackState(params, bFieldY);
        bts.setReferencePoint(finalPos.v());
        bts.setLocation(hodoTrackStateIndex);
        return bts;
    }

    public static BaseTrackState getTrackExtrapAtHodoRK(Track trk, FieldMap fM, double stepSize, int hodoLayer) {
        BaseTrackState ts = (BaseTrackState) TrackStateUtils.getTrackStateAtLast(trk);
        if (ts != null) {
            return getTrackExtrapAtHodoRK(ts, fM, stepSize, hodoLayer);
        }
        return null;
    }

    public static BaseTrackState getTrackExtrapAtHodoRK(Track trk, FieldMap fM, int hodoLayer) {
        return getTrackExtrapAtHodoRK(trk, fM, 0, hodoLayer);
    }

    public static BaseTrackState getTrackExtrapAtTargetRK(Track track, double target_z, double[] beamPosition, FieldMap fM, double stepSize) {

        TrackState ts = track.getTrackStates().get(0);
 
        //if track passed to extrapolateHelixToXPlane, uses first track state
        //by default, else if trackstate is passed, uses trackstate params.
        //Forms HTF, projects to location, then returns point on helix
        Hep3Vector startPos = extrapolateHelixToXPlane(ts, 0.0);
        Hep3Vector startPosTrans = CoordinateTransformations.transformVectorToDetector(startPos);
        double distanceZ = target_z;
        double charge = -1.0 * Math.signum(getR(ts));

        //extrapolateTrackUsingFieldMapRK gets HTF of input track/trackstate
        //if track is passed, defaults to first track state
        //if trackstate is passed, uses trackstate params
        org.hps.util.Pair<Hep3Vector, Hep3Vector> RKresults = extrapolateTrackUsingFieldMapRK(ts, startPosTrans, distanceZ, stepSize, fM);
        double bFieldY = fM.getField(RKresults.getFirstElement()).y();
        Hep3Vector posTrans = CoordinateTransformations.transformVectorToTracking(RKresults.getFirstElement());
        Hep3Vector momTrans = CoordinateTransformations.transformVectorToTracking(RKresults.getSecondElement());

        Hep3Vector finalPos = posTrans;
        if (RKresults.getFirstElement().z() != target_z) {
            Hep3Vector mom = RKresults.getSecondElement();
            double dz = target_z - RKresults.getFirstElement().z();
            double dy = dz * mom.y() / mom.z();
            double dx = dz * mom.x() / mom.z();
            Hep3Vector dPos = new BasicHep3Vector(dx, dy, dz);
            finalPos = CoordinateTransformations.transformVectorToTracking(VecOp.add(dPos, RKresults.getFirstElement()));
        }
        bFieldY = fM.getField(CoordinateTransformations.transformVectorToDetector(finalPos)).y();
        //params are calculated with respect to ref = {trackX, trackY, z=0)
        double[] params = getParametersFromPointAndMomentum(finalPos, momTrans, (int) charge, bFieldY);
        BaseTrackState bts = new BaseTrackState(params, bFieldY);
        //reference point is set to track position in X Y Z
        bts.setReferencePoint(new double[]{finalPos.x(), finalPos.y(), 0.0});
        //Define new reference point, to which track parameters are calc wrt
        double[] newRef = {target_z, beamPosition[0], beamPosition[1]};
        params = getParametersAtNewRefPoint(newRef, bts);
        bts.setParameters(params, bFieldY);
        //Reference point records final position of track.
        //This does not hold the reference point to which the track params are
        //calculated from 
        bts.setReferencePoint(finalPos.v());
        bts.setLocation(TrackState.LastLocation);

        //Get covariance matrix at target. This does not use RK extrap, but
        //simple change of reference point
        SymmetricMatrix originCovMatrix = new SymmetricMatrix(5, ts.getCovMatrix(),true);
        SymmetricMatrix covtrans = getCovarianceAtNewRefPoint(newRef, ts.getReferencePoint(), ts.getParameters(), originCovMatrix); 
        bts.setCovMatrix(covtrans.asPackedArray(true));

        return bts;
    }

    /**
     * Extrapolate track to given position. For backwards compatibility.
     *
     * @param track - to be extrapolated
     * @param z
     * @return extrapolated position
     */
    @Deprecated
    public static Hep3Vector extrapolateTrack(Track track, double z) {
        return extrapolateTrack(track.getTrackStates().get(0), z);
    }

    @Deprecated
    public static Hep3Vector getTrackPositionAtEcal(Track track, int runNumber) {
        double zAtEcal = BeamlineConstants.ECAL_FACE;
        if (4441 < runNumber && runNumber < 8100)
            zAtEcal = BeamlineConstants.ECAL_FACE_ENGINEERING_RUNS;
        
        return extrapolateTrack(track, zAtEcal);
    }

    @Deprecated
    public static Hep3Vector getTrackPositionAtEcal(TrackState track, int runNumber) {
        double zAtEcal = BeamlineConstants.ECAL_FACE;
        if (4441 < runNumber && runNumber < 8100)
            zAtEcal = BeamlineConstants.ECAL_FACE_ENGINEERING_RUNS;
        
        return extrapolateTrack(track, zAtEcal);
    }

    /**
     * Extrapolate track to given position.
     *
     * @param track - to be extrapolated
     * @param z
     * @return extrapolated position
     */
    @Deprecated
    public static Hep3Vector extrapolateTrack(TrackState track, double z) {

        Hep3Vector trackPosition;
        double dz;
        if (z >= BeamlineConstants.DIPOLE_EDGE_ENG_RUN) {
            trackPosition = extrapolateHelixToXPlane(track, BeamlineConstants.DIPOLE_EDGE_ENG_RUN);
            dz = z - BeamlineConstants.DIPOLE_EDGE_ENG_RUN;
        } else if (z <= BeamlineConstants.DIPOLE_EDGELOW_TESTRUN) {
            trackPosition = extrapolateHelixToXPlane(track, BeamlineConstants.DIPOLE_EDGELOW_TESTRUN);
            dz = z - trackPosition.x();
        } else {
            Hep3Vector detVecTracking = extrapolateHelixToXPlane(track, z);
            // System.out.printf("detVec %s\n", detVecTracking.toString());
            return new BasicHep3Vector(detVecTracking.y(), detVecTracking.z(), detVecTracking.x());
        }

        // Get the track azimuthal angle
        double phi = getPhi(track, trackPosition);

        // Find the distance to the point of interest
        double r = dz / (getSinTheta(track) * Math.cos(phi));
        double dx = r * getSinTheta(track) * Math.sin(phi);
        double dy = r * getCosTheta(track);

        // Find the track position at the point of interest
        double x = trackPosition.y() + dx;
        double y = trackPosition.z() + dy;

        return new BasicHep3Vector(x, y, z);
    }

    /**
     * Extrapolate track to given position, using dipole position from geometry.
     *
     * @param track - position along the x-axis of the helix in lcsim
     * coordinates
     * @return extrapolated position
     */
    public static Hep3Vector extrapolateTrack(Track track, double z, Detector detector) {

        Hep3Vector trackPosition;
        // <constant name="dipoleMagnetPositionZ" value="45.72*cm"/>
        // <constant name="dipoleMagnetLength" value="108*cm"/>

        double magnetLength = detector.getConstants().get("dipoleMagnetLength").getValue();
        double magnetZ = detector.getConstants().get("dipoleMagnetPositionZ").getValue();
        double magnetDownstreamEdge = magnetZ + magnetLength / 2;
        double magnetUpstreamEdge = magnetZ - magnetLength / 2;
        if (z >= magnetDownstreamEdge) {
            trackPosition = extrapolateHelixToXPlane(track, magnetDownstreamEdge);
        } else if (z <= magnetUpstreamEdge) {
            trackPosition = extrapolateHelixToXPlane(track, magnetUpstreamEdge);
        } else {
            Hep3Vector detVecTracking = extrapolateHelixToXPlane(track, z);
            // System.out.printf("detVec %s\n", detVecTracking.toString());
            return new BasicHep3Vector(detVecTracking.y(), detVecTracking.z(), detVecTracking.x());
        }
        double dz = z - trackPosition.x();

        // Get the track azimuthal angle
        double phi = getPhi(track, trackPosition);

        // Find the distance to the point of interest
        double r = dz / (getSinTheta(track) * Math.cos(phi));
        double dx = r * getSinTheta(track) * Math.sin(phi);
        double dy = r * getCosTheta(track);

        // Find the track position at the point of interest
        double x = trackPosition.y() + dx;
        double y = trackPosition.z() + dy;

        return new BasicHep3Vector(x, y, z);
    }

    /**
     * Extrapolate helix to given position
     *
     * @param helix - to be extrapolated
     * @param z - position along the x-axis of the helix in lcsim coordiantes
     * @return the extrapolated position
     */
    @Deprecated
    public static Hep3Vector extrapolateTrack(HelicalTrackFit helix, double z) {
        SeedTrack trk = new SeedTrack();
        // bfield = Math.abs((detector.getFieldMap().getField(new
        // BasicHep3Vector(0, 0, 0)).y()));
        double bfield = 0.;
        // Here we aren't really using anything related to momentum so B-field
        // is not important
        trk.setTrackParameters(helix.parameters(), bfield); // Sets first
        // TrackState.
        trk.setCovarianceMatrix(helix.covariance()); // Modifies first
        // TrackState.
        trk.setChisq(helix.chisqtot());
        trk.setNDF(helix.ndf()[0] + helix.ndf()[1]);
        return TrackUtils.extrapolateTrack(trk, z);
    }

    /**
     * @param helix input helix object
     * @param origin of the plane to intercept
     * @param normal of the plane to intercept
     * @param eps criteria on the distance to the plane before stopping
     * iteration
     * @return position in space at the intercept of the plane
     */
    @Deprecated
    public static Hep3Vector getHelixPlanePositionIter(HelicalTrackFit helix, Hep3Vector origin, Hep3Vector normal, double eps) {
        boolean debug = false;
        if (debug) {
            System.out.printf("--- getHelixPlanePositionIter ---\n");
            System.out.printf("Target origin [%.10f %.10f %.10f] normal [%.10f %.10f %.10f]\n", origin.x(), origin.y(), origin.z(), normal.x(), normal.y(), normal.z());
            System.out.printf("%.10f %.10f %.10f %.10f %.10f\n", helix.dca(), helix.z0(), helix.phi0(), helix.slope(), helix.R());
        }
        double x = origin.x();
        double d = 9999.9;
        double dx = 0.0;
        int nIter = 0;
        Hep3Vector pos = null;

        while (Math.abs(d) > eps && nIter < 50) {
            // Calculate position on helix at x
            pos = getHelixPosAtX(helix, x + dx);
            // Check if we are on the plane
            d = VecOp.dot(VecOp.sub(pos, origin), normal);
            dx += -1.0 * d / 2.0;
            if (debug) {
                System.out.printf("%d d %.10f pos [%.10f %.10f %.10f] dx %.10f\n", nIter, d, pos.x(), pos.y(), pos.z(), dx);
            }
            nIter += 1;
        }
        return pos;
    }

    /*
     * Calculates the point on the helix at a given point along the x-axis The
     * normal of the plane is in the same x-y plane as the circle.
     * 
     * @param helix
     * 
     * @param x point along x-axis
     * 
     * @return point on helix at x-coordinate
     */
    @Deprecated
    private static Hep3Vector getHelixPosAtX(HelicalTrackFit helix, double x) {
        // double C = (double)Math.round(helix.curvature()*1000000)/1000000;
        // double R = 1.0/C;
        double R = helix.R();
        double dca = helix.dca();
        double z0 = helix.z0();
        double phi0 = helix.phi0();
        double slope = helix.slope();
        // System.out.printf("%.10f %.10f %.10f %.10f %.10f\n",dca,z0,phi0,slope,R);

        double xc = (R - dca) * Math.sin(phi0);
        double sinPhi = (xc - x) / R;
        double phi_at_x = Math.asin(sinPhi);
        double dphi_at_x = phi_at_x - phi0;
        if (dphi_at_x > Math.PI) {
            dphi_at_x -= 2.0 * Math.PI;
        }
        if (dphi_at_x < -Math.PI) {
            dphi_at_x += 2.0 * Math.PI;
        }
        double s_at_x = -1.0 * dphi_at_x * R;
        double y = dca * Math.cos(phi0) - R * Math.cos(phi0) + R * Math.cos(phi_at_x);
        double z = z0 + s_at_x * slope;
        BasicHep3Vector pos = new BasicHep3Vector(x, y, z);
        // System.out.printf("pos %s xc %f phi_at_x %f dphi_at_x %f s_at_x %f\n",
        // pos.toString(),xc,phi_at_x,dphi_at_x,s_at_x);
        Hep3Vector posXCheck = TrackUtils.extrapolateHelixToXPlane(helix, x);
        if (VecOp.sub(pos, posXCheck).magnitude() > 0.0000001) {
            throw new RuntimeException(String.format("ERROR the helix propagation equations do not agree? (%f,%f,%f) vs (%f,%f,%f) in HelixUtils", pos.x(), pos.y(), pos.z(), posXCheck.x(), posXCheck.y(), posXCheck.z()));
        }
        return pos;
    }

    /**
     *
     */
    public static double findTriangleArea(double x0, double y0, double x1, double y1, double x2, double y2) {
        return .5 * (x1 * y2 - y1 * x2 - x0 * y2 + y0 * x2 + x0 * y1 - y0 * x1);
    }

    /**
     *
     */
    public static boolean sensorContainsTrack(Hep3Vector trackPosition, SiSensor sensor) {
        boolean debug = false;
        // final double tolerance = 1.5;

        ITransform3D localToGlobal = sensor.getGeometry().getLocalToGlobal();

        Box sensorSolid = (Box) sensor.getGeometry().getLogicalVolume().getSolid();
        Polygon3D sensorFace = sensorSolid.getFacesNormalTo(new BasicHep3Vector(0, 0, 1)).get(0);

        List<Point3D> vertices = new ArrayList<Point3D>();
        for (Point3D vertex : sensorFace.getVertices()) {
            localToGlobal.transform(vertex);
            vertices.add(new Point3D(vertex.x(), vertex.y(), trackPosition.z()));
        }

        Point3D trackPositionPoint = new Point3D(trackPosition);
        Polygon3D transformedSensorFace;
        try {
            transformedSensorFace = new Polygon3D(vertices);
        } catch (RuntimeException ex) {
            return false;
        }

        if (debug) {
            System.out.println("sensorContainsTrack:  Vertex 1 Position: " + vertices.get(0).toString());
            System.out.println("sensorContainsTrack:  Vertex 2 Position: " + vertices.get(1).toString());
            System.out.println("sensorContainsTrack:  Vertex 3 Position: " + vertices.get(2).toString());
            System.out.println("sensorContainsTrack:  Vertex 4 Position: " + vertices.get(3).toString());
            System.out.println("sensorContainsTrack:  Track Position: " + trackPositionPoint.toString());

            System.out.printf("Track-to-vertex Position: %f \n", GeomOp3D.distanceBetween(trackPositionPoint, transformedSensorFace));
        }

        // return (Math.abs(distance) < tolerance);
        return GeomOp3D.intersects(trackPositionPoint, transformedSensorFace);
    }

    public static int[] getHitsInTopBottom(Track track) {
        int n[] = {0, 0};
        List<TrackerHit> hitsOnTrack = track.getTrackerHits();
        for (TrackerHit hit : hitsOnTrack) {
            HelicalTrackHit hth = (HelicalTrackHit) hit;
            // ===> if (SvtUtils.getInstance().isTopLayer((SiSensor)
            // ((RawTrackerHit) hth.getRawHits().get(0)).getDetectorElement()))
            // {
            HpsSiSensor sensor = ((HpsSiSensor) ((RawTrackerHit) hth.getRawHits().get(0)).getDetectorElement());
            if (sensor.isTopLayer()) {
                n[0] = n[0] + 1;
            } else {
                n[1] = n[1] + 1;
            }
        }
        return n;
    }

    public static boolean isTopTrack(Track track, int minhits) {
        return isTopOrBottomTrack(track, minhits) == 1;
    }

    public static boolean isBottomTrack(Track track, int minhits) {
        return isTopOrBottomTrack(track, minhits) == 0;
    }

    public static int isTopOrBottomTrack(Track track, int minhits) {
        int nhits[] = getHitsInTopBottom(track);
        if (nhits[0] >= minhits && nhits[1] == 0) {
            return 1;
        } else if (nhits[1] >= minhits && nhits[0] == 0) {
            return 0;
        } else {
            return -1;
        }
    }

    public static boolean hasTopBotHit(Track track) {
        int nhits[] = getHitsInTopBottom(track);
        return nhits[0] > 0 && nhits[1] > 0;
    }

    // 3D hits shared
    public static boolean isSharedHit(TrackerHit hit, List<Track> othertracks) {
        // HelicalTrackHit hth = (HelicalTrackHit) hit;
        TrackerHit hth = hit;
        for (Track track : othertracks) {
            List<TrackerHit> hitsOnTrack = track.getTrackerHits();
            for (TrackerHit loop_hit : hitsOnTrack) {
                // HelicalTrackHit loop_hth = (HelicalTrackHit) loop_hit;
                TrackerHit loop_hth = loop_hit;
                if (hth.equals(loop_hth)) // System.out.printf("share hit at layer %d at %s (%s) with track w/ chi2=%f\n",hth.Layer(),hth.getCorrectedPosition().toString(),loop_hth.getCorrectedPosition().toString(),track.getChi2());
                {
                    return true;
                }
            }
        }
        return false;
    }

    // 3D hits shared
    public static boolean isSharedHit(TrackerHit hit, Track track) {
        // HelicalTrackHit hth = (HelicalTrackHit) hit;
        TrackerHit hth = hit;
        List<TrackerHit> hitsOnTrack = track.getTrackerHits();
        for (TrackerHit loop_hit : hitsOnTrack) {
            // HelicalTrackHit loop_hth = (HelicalTrackHit) loop_hit;
            TrackerHit loop_hth = loop_hit;
            if (hth.equals(loop_hth)) // System.out.printf("share hit at layer %d at %s (%s) with track w/ chi2=%f\n",hth.Layer(),hth.getCorrectedPosition().toString(),loop_hth.getCorrectedPosition().toString(),track.getChi2());
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Number of shared 3D hits between tracks
     *
     * @param track
     * @param tracklist
     * @return number of 3D hits shared on a track
     */
    public static int numberOfSharedHits(Track track, List<Track> tracklist) {
        List<Track> tracks = new ArrayList<Track>();
        // System.out.printf("%d tracks in event\n",tracklist.size());
        // System.out.printf("look for another track with chi2=%f and px=%f \n",track.getChi2(),track.getTrackStates().get(0).getMomentum()[0]);
        for (Track t : tracklist) {
            // System.out.printf("add track with chi2=%f and px=%f ?\n",t.getChi2(),t.getTrackStates().get(0).getMomentum()[0]);
            if (t.equals(track)) // System.out.printf("NOPE\n");
            {
                continue;
            }
            // System.out.printf("YEPP\n");
            tracks.add(t);
        }
        List<TrackerHit> hitsOnTrack = track.getTrackerHits();
        int n_shared = 0;
        for (TrackerHit hit : hitsOnTrack) {
            if (isSharedHit(hit, tracks)) {
                ++n_shared;
            }
        }
        return n_shared;
    }

    /**
     * Number of shared 3D hits between two tracks
     *
     * @param track
     * @param tracklist
     * @return number of 3D hits shared between two tracks
     */
    public static int numberOfSharedHits(Track track1, Track track2) {
        if (track1.equals(track2)) {
            return 0;
        } else {
            List<TrackerHit> hitsOnTrack = track1.getTrackerHits();
            int n_shared = 0;
            for (TrackerHit hit : hitsOnTrack) {
                if (isSharedHit(hit, track2)) {
                    ++n_shared;
                }
            }
            return n_shared;
        }
    }

    /**
     * Number of shared 3D hits between tracks
     *
     * @param track
     * @param tracklist
     * @return the track associated with the most shared hits
     */
    public static Track mostSharedHitTrack(Track track, List<Track> tracklist) {
        List<Track> tracks = new ArrayList<Track>();
        // System.out.printf("%d tracks in event\n",tracklist.size());
        // System.out.printf("look for another track with chi2=%f and px=%f \n",track.getChi2(),track.getTrackStates().get(0).getMomentum()[0]);
        for (Track t : tracklist) {
            // System.out.printf("add track with chi2=%f and px=%f ?\n",t.getChi2(),t.getTrackStates().get(0).getMomentum()[0]);
            if (t.equals(track)) // System.out.printf("NOPE\n");
            {
                continue;
            }
            // System.out.printf("YEPP\n");
            tracks.add(t);
        }
        // loop through track list to find the most shared hits between any
        // track
        int mostShared = 0;
        Track sharedTrk = track;
        for (Track tt : tracks) {
            if (mostShared < numberOfSharedHits(track, tt)) {
                mostShared = numberOfSharedHits(track, tt);
                sharedTrk = tt;
            }
        }

        return sharedTrk;
    }

    public static boolean hasSharedHits(Track track, List<Track> tracklist) {
        return numberOfSharedHits(track, tracklist) != 0;
    }

    public static void cut(int cuts[], EventQuality.Cut bit) {
        cuts[0] = cuts[0] | (1 << bit.getValue());
    }

    public static boolean isGoodTrack(Track track, List<Track> tracklist, EventQuality.Quality trk_quality) {
        int cuts = passTrackSelections(track, tracklist, trk_quality);
        return cuts == 0;
    }

    public static int passTrackSelections(Track track, List<Track> tracklist, EventQuality.Quality trk_quality) {
        int cuts[] = {0};
        if (trk_quality.compareTo(Quality.NONE) != 0) {
            if (track.getTrackStates().get(0).getMomentum()[0] < EventQuality.instance().getCutValue(EventQuality.Cut.PZ, trk_quality)) {
                cut(cuts, EventQuality.Cut.PZ);
            }
            if (track.getChi2() >= EventQuality.instance().getCutValue(EventQuality.Cut.CHI2, trk_quality)) {
                cut(cuts, EventQuality.Cut.CHI2);
            }
            if (numberOfSharedHits(track, tracklist) > ((int) Math.round(EventQuality.instance().getCutValue(EventQuality.Cut.SHAREDHIT, trk_quality)))) {
                cut(cuts, EventQuality.Cut.SHAREDHIT);
            }
            if (hasTopBotHit(track)) {
                cut(cuts, EventQuality.Cut.TOPBOTHIT);
            }
            if (track.getTrackerHits().size() < ((int) Math.round(EventQuality.instance().getCutValue(EventQuality.Cut.NHITS, trk_quality)))) {
                cut(cuts, EventQuality.Cut.NHITS);
            }
        }
        return cuts[0];
    }

    public static boolean isTopTrack(HelicalTrackFit htf) {
        return htf.slope() > 0;
    }

    public static boolean isBottomTrack(HelicalTrackFit htf) {
        return !isTopTrack(htf);
    }

    /**
     * Transform MCParticle into a Helix object. Note that it produces the helix
     * parameters at nominal x=0 and assumes that there is no field at x<0
     *
     * @param mcp MC particle to be transformed
     * @return {@link HelicalTrackFit} object based on the MC particle
     */
    public static HelicalTrackFit getHTF(MCParticle mcp, double Bz) {
        return getHTF(mcp, mcp.getOrigin(), Bz);
    }

    /**
     * Transform MCParticle into a {@link HelicalTrackFit} object. Note that it
     * produces the {@link HelicalTrackFit} parameters at nominal reference
     * point at origin and assumes that there is no field at x<0
     *
     * @param mcp - MC particle to be transformed
     * @param origin - origin to be used for the track
     * @return {@link HelicalTrackFit} object based on the MC particle
     */
    public static HelicalTrackFit getHTF(MCParticle mcp, Hep3Vector origin, double Bz) {
        boolean debug = false;

        if (debug) {
            System.out.printf("getHTF\nmcp org %s origin used %s mc p %s\n", mcp.getOrigin().toString(), origin.toString(), mcp.getMomentum().toString());
        }

        Hep3Vector org = CoordinateTransformations.transformVectorToTracking(origin);
        Hep3Vector p = CoordinateTransformations.transformVectorToTracking(mcp.getMomentum());

        if (debug) {
            System.out.printf("mcp org %s mc p %s (trans)\n", org.toString(), p.toString());
        }

        // Move to x=0 if needed
        double targetX = BeamlineConstants.DIPOLE_EDGELOW_TESTRUN;
        if (org.x() < targetX) {
            double dydx = p.y() / p.x();
            double dzdx = p.z() / p.x();
            double delta_x = targetX - org.x();
            double y = delta_x * dydx + org.y();
            double z = delta_x * dzdx + org.z();
            double x = org.x() + delta_x;
            if (Math.abs(x - targetX) > 1e-8) {
                throw new RuntimeException("Error: origin is not zero!");
            }
            org = new BasicHep3Vector(x, y, z);
            // System.out.printf("org %s p %s -> org %s\n",
            // old.toString(),p.toString(),org.toString());
        }

        if (debug) {
            System.out.printf("mcp org %s mc p %s (trans2)\n", org.toString(), p.toString());
        }

        HelixParamCalculator helixParamCalculator = new HelixParamCalculator(p, org, -1 * ((int) mcp.getCharge()), Bz);
        double par[] = new double[5];
        par[HelicalTrackFit.dcaIndex] = helixParamCalculator.getDCA();
        par[HelicalTrackFit.slopeIndex] = helixParamCalculator.getSlopeSZPlane();
        par[HelicalTrackFit.phi0Index] = helixParamCalculator.getPhi0();
        par[HelicalTrackFit.curvatureIndex] = 1.0 / helixParamCalculator.getRadius();
        par[HelicalTrackFit.z0Index] = helixParamCalculator.getZ0();
        HelicalTrackFit htf = getHTF(par);
        if (debug) {
            System.out.printf("d0 %f z0 %f R %f phi %f lambda %s\n", htf.dca(), htf.z0(), htf.R(), htf.phi0(), htf.slope());
        }
        return htf;
    }

    public static HelicalTrackFit getHTF(Track track) {
        if (track.getClass().isInstance(SeedTrack.class)) {
            return ((SeedTrack) track).getSeedCandidate().getHelix();
        } else {
            double[] chisq = {track.getChi2(), 0};
            int[] ndf = {track.getNDF(), 0};
            TrackState ts = track.getTrackStates().get(0);
            double par[] = ts.getParameters();
            SymmetricMatrix cov = new SymmetricMatrix(5, ts.getCovMatrix(), true);
            HelicalTrackFit htf = new HelicalTrackFit(par, cov, chisq, ndf, null, null);
            return htf;
        }
    }

    public static HelicalTrackFit getHTF(double par[]) {
        // need to have matrix that makes sense? Really?
        SymmetricMatrix cov = new SymmetricMatrix(5);
        for (int i = 0; i < cov.getNRows(); ++i) {
            cov.setElement(i, i, 1.);
        }
        HelicalTrackFit htf = new HelicalTrackFit(par, cov, new double[2], new int[2], null, null);
        return htf;
    }

    public static HelicalTrackFit getHTF(TrackState state) {
        double par[] = state.getParameters();
        SymmetricMatrix cov = new SymmetricMatrix(5, state.getCovMatrix(), true);
        HelicalTrackFit htf = new HelicalTrackFit(par, cov, new double[2], new int[2], null, null);
        return htf;
    }

    public static MCParticle getMatchedTruthParticle(Track track) {
        boolean debug = false;

        Map<MCParticle, Integer> particlesOnTrack = new LinkedHashMap<MCParticle, Integer>();

        if (debug) {
            System.out.printf("getMatchedTruthParticle: getmatched mc particle from %d tracker hits on the track \n", track.getTrackerHits().size());
        }

        for (TrackerHit hit : track.getTrackerHits()) {
            List<MCParticle> mcps = ((HelicalTrackHit) hit).getMCParticles();
            if (mcps == null) {
                System.out.printf("getMatchedTruthParticle: warning, this hit (layer %d pos=%s) has no mc particles.\n", ((HelicalTrackHit) hit).Layer(), ((HelicalTrackHit) hit).getCorrectedPosition().toString());
            } else {
                if (debug) {
                    System.out.printf("getMatchedTruthParticle: this hit (layer %d pos=%s) has %d mc particles.\n", ((HelicalTrackHit) hit).Layer(), ((HelicalTrackHit) hit).getCorrectedPosition().toString(), mcps.size());
                }
                for (MCParticle mcp : mcps) {
                    if (!particlesOnTrack.containsKey(mcp)) {
                        particlesOnTrack.put(mcp, 0);
                    }
                    int c = particlesOnTrack.get(mcp);
                    particlesOnTrack.put(mcp, c + 1);
                }
            }
        }
        if (debug) {
            System.out.printf("Track p=[ %f, %f, %f] \n", track.getTrackStates().get(0).getMomentum()[0], track.getTrackStates().get(0).getMomentum()[1], track.getTrackStates().get(0).getMomentum()[1]);
            System.out.printf("Found %d particles\n", particlesOnTrack.size());
            for (Map.Entry<MCParticle, Integer> entry : particlesOnTrack.entrySet()) {
                System.out.printf("%d hits assigned to %d p=%s \n", entry.getValue(), entry.getKey().getPDGID(), entry.getKey().getMomentum().toString());
            }
        }
        Map.Entry<MCParticle, Integer> maxEntry = null;
        for (Map.Entry<MCParticle, Integer> entry : particlesOnTrack.entrySet()) {
            if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0) {
                maxEntry = entry; // if ( maxEntry != null ) { //
            }        // if(entry.getValue().compareTo(maxEntry.getValue())
        }        // < 0) continue; //}
        // maxEntry = entry;
        if (debug) {
            if (maxEntry != null) {
                System.out.printf("Matched particle with pdgId=%d and mom %s to track with charge %d and momentum [%f %f %f]\n", maxEntry.getKey().getPDGID(), maxEntry.getKey().getMomentum().toString(), track.getCharge(), track.getTrackStates().get(0).getMomentum()[0], track.getTrackStates().get(0).getMomentum()[1], track.getTrackStates().get(0).getMomentum()[2]);
            } else {
                System.out.printf("No truth particle found on this track\n");
            }
        }
        return maxEntry == null ? null : maxEntry.getKey();
    }

    /*
     * try to make a seed track from a base track ...some things are
     * irretrivable (tracking strategy).. and some things I just don't care much
     * about to dig out (det name)
     */
    public static SeedTrack makeSeedTrackFromBaseTrack(Track track) {

        TrackState trkState = track.getTrackStates().get(0);
        // first make the HelicalTrackFit Object
        double[] covArray = trkState.getCovMatrix();
        SymmetricMatrix cov = new SymmetricMatrix(5, covArray, true);
        double[] chisq = {track.getChi2(), 0};
        int[] ndf = {track.getNDF(), 0};
        Map<HelicalTrackHit, Double> smap = new HashMap<>(); // just leave these
        // empty
        Map<HelicalTrackHit, MultipleScatter> msmap = new HashMap<>();// just
        // leave
        // these
        // empty
        double[] pars = {trkState.getD0(), trkState.getPhi(), trkState.getOmega(), trkState.getZ0(), trkState.getTanLambda()};
        HelicalTrackFit htf = new HelicalTrackFit(pars, cov, chisq, ndf, smap, msmap);
        // now get the hits and make them helicaltrackhits
        List<TrackerHit> rth = track.getTrackerHits();
        List<HelicalTrackHit> hth = new ArrayList<>();
        for (TrackerHit hit : rth) {
            hth.add(makeHelicalTrackHitFromTrackerHit(hit));
        }
        // SeedCandidate(List<HelicalTrackHit> , SeedStrategy strategy,
        // HelicalTrackFit helix, double bfield) ;
        SeedCandidate scand = new SeedCandidate(hth, null, htf, 0.24);
        SeedTrack st = new SeedTrack();
        st.setSeedCandidate(scand);
        return st;
    }

    /*
     * cast TrackerHit as HTH...this is pretty dumb; just a rearrangment of
     * information in TrackerHit. The important information that's in HTH but
     * not in Tracker hit is the HelicalTrackCrosses (and from there the
     * individual strip clusters) is lost; some work to get them back.
     */
    public static HelicalTrackHit makeHelicalTrackHitFromTrackerHit(TrackerHit hit) {
        Hep3Vector pos = new BasicHep3Vector(hit.getPosition());
        SymmetricMatrix hitcov = new SymmetricMatrix(3, hit.getCovMatrix(), true);
        double dedx = hit.getdEdx();
        double time = hit.getTime();
        int type = hit.getType();
        List rhits = hit.getRawHits();
        String detname = "Foobar";
        int layer = 666;
        BarrelEndcapFlag beflag = BarrelEndcapFlag.BARREL;
        return new HelicalTrackHit(pos, hitcov, dedx, time, type, rhits, detname, layer, beflag);
    }

    private static Pair<EventHeader, RelationalTable> hitToStripsCache = null;

    public static RelationalTable getHitToStripsTable(EventHeader event, String HelicalTrackHitRelationsCollectionName) {
        if (hitToStripsCache == null || hitToStripsCache.getFirst() != event) {
            RelationalTable hitToStrips = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
            //List<LCRelation> hitrelations = event.get(LCRelation.class, "HelicalTrackHitRelations");
            if (!event.hasCollection(LCRelation.class, HelicalTrackHitRelationsCollectionName)) {
                return null;
            }
            List<LCRelation> hitrelations = event.get(LCRelation.class, HelicalTrackHitRelationsCollectionName);
            for (LCRelation relation : hitrelations) {
                if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                    hitToStrips.add(relation.getFrom(), relation.getTo());
                }
            }
            hitToStripsCache = new Pair<EventHeader, RelationalTable>(event, hitToStrips);
        }
        return hitToStripsCache.getSecond();
    }

    public static RelationalTable getHitToStripsTable(EventHeader event) {
        return getHitToStripsTable(event, "HelicalTrackHitRelations");
    }

    private static Pair<EventHeader, RelationalTable> hitToRotatedCache = null;

    public static RelationalTable getHitToRotatedTable(EventHeader event, String RotatedHelicalTrackHitRelationsCollectionName) {
        //        if (hitToRotatedCache != null)
        //            System.out.println("getHitToRotatedTable:  Already have a hitToRotatedCache");
        //        if (hitToRotatedCache != null && hitToRotatedCache.getFirst() == event)
        //            System.out.println("getHitToRotatedTable:  getFirst()==event");

        if (hitToRotatedCache == null || hitToRotatedCache.getFirst() != event) {
            //          System.out.println("getHitToRotatedTable:  making new table");
            RelationalTable hitToRotated = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
            //List<LCRelation> rotaterelations = event.get(LCRelation.class, "RotatedHelicalTrackHitRelations");
            if (!event.hasCollection(LCRelation.class, RotatedHelicalTrackHitRelationsCollectionName)) {
                return null;
            }
            List<LCRelation> rotaterelations = event.get(LCRelation.class, RotatedHelicalTrackHitRelationsCollectionName);
            for (LCRelation relation : rotaterelations) {
                if (relation != null && relation.getFrom() != null && relation.getTo() != null) //                   System.out.println("getHitToRotatedTable:  adding a relation to hitToRotated");
                {
                    hitToRotated.add(relation.getFrom(), relation.getTo());
                }
            }
            hitToRotatedCache = new Pair<EventHeader, RelationalTable>(event, hitToRotated);
        }
        //       System.out.println("getHitToRotatedTable: returning hitToRotatedCache with size = " + hitToRotatedCache.getSecond().size());
        return hitToRotatedCache.getSecond();
    }

    public static RelationalTable getHitToRotatedTable(EventHeader event) {
        return getHitToRotatedTable(event, "RotatedHelicalTrackHitRelations");
    }

    public static double getTrackTime(Track track, RelationalTable hitToStrips, RelationalTable hitToRotated) {
        double meanTime = 0;
        List<TrackerHit> stripHits = getStripHits(track, hitToStrips, hitToRotated);
        for (TrackerHit hit : stripHits) {
            meanTime += hit.getTime();
        }
        meanTime /= stripHits.size();
        return meanTime;
    }

    public static double getTrackTimeSD(Track track, RelationalTable hitToStrips, RelationalTable hitToRotated) {
        double meanTime = getTrackTime(track, hitToStrips, hitToRotated);
        List<TrackerHit> stripHits = getStripHits(track, hitToStrips, hitToRotated);

        double sdTime = 0;
        for (TrackerHit hit : stripHits) {
            sdTime += Math.pow(meanTime - hit.getTime(), 2);
        }
        sdTime = Math.sqrt(sdTime / stripHits.size());

        return sdTime;
    }

    public static List<TrackerHit> getStripHits(Track track, RelationalTable hitToStrips, RelationalTable hitToRotated) {
        List<TrackerHit> hits = new ArrayList<TrackerHit>();
        for (TrackerHit hit : track.getTrackerHits()) {
            hits.addAll(hitToStrips.allFrom(hitToRotated.from(hit)));
        }

        return hits;
    }

    public static List<TrackerHit> sortHits(Collection<TrackerHit> hits) {
        List<TrackerHit> hitList = new ArrayList<TrackerHit>(hits);
        Collections.sort(hitList, new LayerComparator());
        return hitList;
    }

    public static class LayerComparator implements Comparator<TrackerHit> {

        @Override
        public int compare(TrackerHit o1, TrackerHit o2) {
            return Integer.compare(TrackUtils.getLayer(o1), TrackUtils.getLayer(o2));
        }
    }

    /**
     * Number of 2D hits shared between tracks
     *
     * @param track1
     * @param track2
     * @param hitToStrips
     * @param hitToRotated
     * @return
     */
    public static int numberOfSharedStrips(Track track1, Track track2, RelationalTable hitToStrips, RelationalTable hitToRotated) {
        Set<TrackerHit> track1hits = new HashSet<TrackerHit>(getStripHits(track1, hitToStrips, hitToRotated));
        int nShared = 0;
        for (TrackerHit hit : track2.getTrackerHits()) {
            for (TrackerHit hts : (Set<TrackerHit>) hitToStrips.allFrom(hitToRotated.from(hit))) {
                if (track1hits.contains(hts)) {
                    nShared++;
                }
            }
        }
        return nShared;
    }

    /**
     * Tells if there are shared 2D hits between tracks
     *
     * @param track1
     * @param track2
     * @param hitToStrips
     * @param hitToRotated
     * @return
     */
    public static boolean hasSharedStrips(Track track1, Track track2, RelationalTable hitToStrips, RelationalTable hitToRotated) {
        int nShared = numberOfSharedStrips(track1, track2, hitToStrips, hitToRotated);
        if (nShared == 0) {
            return false;
        } else {
            return true;
        }
    }

    public static int getLayer(TrackerHit strip) {
        if (strip == null) {
            System.out.println("Strip is null?????");
        }
        if (strip.getRawHits() == null) {
            System.out.println("No raw hits associated with this strip????");
        }

        return ((RawTrackerHit) strip.getRawHits().get(0)).getLayerNumber();
    }

    /**
     * Compute strip isolation, defined as the minimum distance to another strip
     * in the same sensor. Strips are only checked if they formed valid crosses
     * with the other strip in the cross (passing time and tolerance cuts).
     *
     * @param strip The strip whose isolation is being calculated.
     * @param otherStrip The other strip in the stereo hit.
     * @param hitToStrips
     * @param hitToRotated
     * @return Double_MAX_VALUE if no other strips found.
     */
    public static double getIsolation(TrackerHit strip, TrackerHit otherStrip, RelationalTable hitToStrips, RelationalTable hitToRotated) {
        double nearestDistance = 99999999.0;
        for (TrackerHit cross : (Set<TrackerHit>) hitToStrips.allTo(otherStrip)) {
            for (TrackerHit crossStrip : (Set<TrackerHit>) hitToStrips.allFrom(cross)) {
                if (crossStrip != strip && crossStrip != otherStrip) {
                    int stripMin = Integer.MAX_VALUE;
                    int stripMax = Integer.MIN_VALUE;
                    int crossMin = Integer.MAX_VALUE;
                    int crossMax = Integer.MIN_VALUE;
                    for (Object rth : strip.getRawHits()) {
                        int hitStrip = ((RawTrackerHit) rth).getIdentifierFieldValue("strip");
                        stripMin = Math.min(stripMin, hitStrip);
                        stripMax = Math.max(stripMax, hitStrip);
                    }
                    for (Object rth : crossStrip.getRawHits()) {
                        int hitStrip = ((RawTrackerHit) rth).getIdentifierFieldValue("strip");
                        crossMin = Math.min(crossMin, hitStrip);
                        crossMax = Math.max(crossMax, hitStrip);
                    }
                    if (stripMin - crossMax <= 1 && crossMin - stripMax <= 1) {
                        continue; // adjacent strips don't count
                    }
                    Hep3Vector stripPosition = new BasicHep3Vector(strip.getPosition());
                    Hep3Vector crossStripPosition = new BasicHep3Vector(crossStrip.getPosition());
                    double distance = VecOp.sub(stripPosition, crossStripPosition).magnitude();
                    if (Math.abs(stripPosition.y()) > Math.abs(crossStripPosition.y())) {
                        distance = -distance;
                    }
                    // System.out.format("%s, %s, %s, %f\n", stripPosition,
                    // crossStripPosition, VecOp.sub(stripPosition,
                    // crossStripPosition), distance);
                    // if (distance<=0.0601) continue; //hack to avoid counting
                    // adjacent strips that didn't get clustered together
                    // if (distance<0.1)
                    // System.out.format("%d, %d, %f\n",strip.getRawHits().size(),crossStrip.getRawHits().size(),
                    // distance);
                    // if (distance < 0.1) {
                    // System.out.format("%s, %s, %s, %f\n", stripPosition,
                    // crossStripPosition, VecOp.sub(stripPosition,
                    // crossStripPosition), distance);
                    // }
                    if (Math.abs(distance) < Math.abs(nearestDistance)) {
                        nearestDistance = distance;
                    }
                }
            }
        }
        return nearestDistance;
    }

    /**
     * Make an array with isolations for all 12 strip layers. If the track
     * doesn't use a layer, the isolation is null; if there is no other strip
     * hit in a layer, the isolation is Double.MAX_VALUE.
     *
     * @param trk
     * @param hitToStrips
     * @param hitToRotated
     * @return isolations for all 12 strip layers
     */
    public static Double[] getIsolations(Track trk, RelationalTable hitToStrips, RelationalTable hitToRotated, int layers) {
        Double[] isolations = new Double[2 * layers];
        for (TrackerHit hit : trk.getTrackerHits()) {
            Set<TrackerHit> htsList = hitToStrips.allFrom(hitToRotated.from(hit));
            TrackerHit[] strips = new TrackerHit[2];
            htsList.toArray(strips);
            if (strips[0] == null) {
                continue;
            }
            isolations[TrackUtils.getLayer(strips[0]) - 1] = TrackUtils.getIsolation(strips[0], strips[1], hitToStrips, hitToRotated);
            if (strips[1] == null) {
                continue;
            }
            isolations[TrackUtils.getLayer(strips[1]) - 1] = TrackUtils.getIsolation(strips[1], strips[0], hitToStrips, hitToRotated);
        }
        return isolations;
    }

    public static Double[] getIsolations(Track trk, RelationalTable hitToStrips, RelationalTable hitToRotated) {
        return getIsolations(trk, hitToStrips, hitToRotated, 7);
    }

    /**
     * Backward compatibility function for {@code extrapolateTrackUsingFieldMap}
     * .
     */
    public static BaseTrackState extrapolateTrackUsingFieldMap(Track track, double startPositionX, double endPositionX, double stepSize, FieldMap fieldMap) {
        TrackState stateAtIP = null;
        for (TrackState state : track.getTrackStates()) {
            if (state.getLocation() == TrackState.AtIP) {
                stateAtIP = state;
            }
        }
        if (stateAtIP == null) {
            throw new RuntimeException("No track state at IP was found so this function shouldn't be used.");
        }

        // Extrapolate this track state
        return extrapolateTrackUsingFieldMap(stateAtIP, startPositionX, endPositionX, stepSize, fieldMap);
    }

    /**
     * Iteratively extrapolates a track to a specified value of x (z in detector
     * frame) using the full 3D field map.
     *
     * @param track The {@link Track} object to extrapolate.
     * @param startPositionX The position from which to start the extrapolation
     * from. The track will be extrapolated to this point using a constant
     * field.
     * @param endPositionX The position to extrapolate the track to.
     * @param stepSize The step size determining how far a track will be
     * extrapolated after every iteration.
     * @param fieldMap The 3D field map
     * @return A {@link TrackState} at the final extrapolation point. Note that
     * the "Tracking" frame is used for the reference point coordinate system.
     */
    public static BaseTrackState extrapolateTrackUsingFieldMap(TrackState track, double startPositionX, double endPosition, double stepSize, FieldMap fieldMap) {
        return extrapolateTrackUsingFieldMap(track, startPositionX, endPosition, stepSize, 0.005, fieldMap);
    }

    public static org.hps.util.Pair<Hep3Vector, Hep3Vector> extrapolateTrackUsingFieldMapRK(TrackState ts, Hep3Vector startPosition, double distanceZ, double stepSize, FieldMap fM) {

        // Get the momentum of the track
        HelicalTrackFit helicalTrackFit = TrackUtils.getHTF(ts);
        double pathToStart = HelixUtils.PathToXPlane(helicalTrackFit, startPosition.z(), 0., 0).get(0);
        double bFieldY = fM.getField(new BasicHep3Vector(0, 0, 500)).y();
        double p = Math.abs(helicalTrackFit.p(bFieldY));
        Hep3Vector helixDirection = HelixUtils.Direction(helicalTrackFit, pathToStart);
        Hep3Vector p0Trans = CoordinateTransformations.transformVectorToDetector(VecOp.mult(p, helixDirection));

        double distance = distanceZ / VecOp.cosTheta(p0Trans);
        if (stepSize == 0) {
            stepSize = distance / 100.0;
        }

        double charge = -1.0 * Math.signum(getR(ts));
        RK4integrator RKint = new RK4integrator(charge, stepSize, fM);

        return RKint.integrate(startPosition, p0Trans, distance);
    }

    public static BaseTrackState extrapolateTrackUsingFieldMap(TrackState track, double startPositionX, double endPosition, double stepSize, double epsilon, FieldMap fieldMap) {
        // Start by extrapolating the track to the approximate point where the
        // fringe field begins.
        Hep3Vector currentPosition = TrackUtils.extrapolateHelixToXPlane(track, startPositionX);
        HelicalTrackFit helicalTrackFit = TrackUtils.getHTF(track);
        double q = Math.signum(track.getOmega());
        double startPosition = currentPosition.x();

        // Calculate the path length to the start position
        double pathToStart = HelixUtils.PathToXPlane(helicalTrackFit, startPosition, 0., 0).get(0);

        // Get the momentum of the track
        double bFieldY = fieldMap.getField(new BasicHep3Vector(0, 0, 500)).y();
        double p = Math.abs(helicalTrackFit.p(bFieldY));
        // Get a unit vector giving the track direction at the start
        Hep3Vector helixDirection = HelixUtils.Direction(helicalTrackFit, pathToStart);
        // Calculate the momentum vector at the start
        Hep3Vector currentMomentum = VecOp.mult(p, helixDirection);

        // HACK: LCSim doesn't deal well with negative fields so they are
        // turned to positive for tracking purposes. As a result,
        // the charge calculated using the B-field, will be wrong
        // when the field is negative and needs to be flipped.
        if (bFieldY < 0) {
            q = q * (-1);
        }

        // Swim the track through the B-field until the end point is reached
        Hep3Vector currentPositionDet = null;

        double distance = endPosition - currentPosition.x();
        if (stepSize == 0) {
            stepSize = distance / 100.0;
        }
        double sign = Math.signum(distance);
        distance = Math.abs(distance);

        while (distance > epsilon) {
            // The field map coordinates are in the detector frame so the
            // extrapolated track position needs to be transformed from the
            // track frame to detector.
            currentPositionDet = CoordinateTransformations.transformVectorToDetector(currentPosition);

            // Get the field at the current position along the track.
            bFieldY = fieldMap.getField(currentPositionDet).y();

            // Get a trajectory (Helix or Line objects) created with the
            // track parameters at the current position.
            Trajectory trajectory = TrackUtils.getTrajectory(currentMomentum, new org.lcsim.spacegeom.SpacePoint(currentPosition), q, bFieldY);

            // Using the new trajectory, extrapolated the track by a step and
            // update the extrapolated position.
            Hep3Vector currentPositionTry = trajectory.getPointAtDistance(stepSize);

            if ((Math.abs(endPosition - currentPositionTry.x()) > epsilon) && (Math.signum(endPosition - currentPositionTry.x()) != sign)) // went too far, try again with smaller step-size
            {
                if (Math.abs(stepSize) > 0.001) {
                    stepSize /= 2.0;
                    continue;
                } else {
                    break;
                }
            }
            currentPosition = currentPositionTry;

            distance = Math.abs(endPosition - currentPosition.x());
            // Calculate the momentum vector at the new position. This will
            // be used when creating the trajectory that will be used to
            // extrapolate the track in the next iteration.
            currentMomentum = VecOp.mult(currentMomentum.magnitude(), trajectory.getUnitTangentAtLength(stepSize));
        }

        double[] params = getParametersFromPointAndMomentum(currentPosition, currentMomentum, (int) q, bFieldY);

        // Create a track state at the extrapolation point
        BaseTrackState trackState = new BaseTrackState(params, bFieldY);
        trackState.setReferencePoint(currentPosition.v());
        return trackState;
    }

    public static double calculatePhi(double x, double y, double xc, double yc, double sign) {
        return FastMath.atan2(y - yc, x - xc) - sign * Math.PI / 2;
    }

    public static double calculatePhi(double px, double py) {
        return FastMath.atan2(py, px);
    }

    public static double calculateTanLambda(double pz, double p) {
        return FastMath.atan2(pz, p);
    }

    public static double calculateCurvature(double p, double q, double B) {
        return q * B / p;
    }

    /**
     *
     *
     * @param p0
     * @param r0
     * @param q
     * @param B
     * @return the created trajectory
     */
    public static Trajectory getTrajectory(Hep3Vector p0, org.lcsim.spacegeom.SpacePoint r0, double q, double B) {
        SpaceVector p = new CartesianVector(p0.v());
        double phi = FastMath.atan2(p.y(), p.x());
        double lambda = FastMath.atan2(p.z(), p.rxy());
        double field = B * fieldConversion;

        if (q != 0 && field != 0) {
            double radius = p.rxy() / (q * field);
            // System.out.println("[GetTrajectory] : Current Radius: " +
            // radius);
            return new Helix(r0, radius, phi, lambda);
        } else {
            return new Line(r0, phi, lambda);
        }
    }

    /**
     * Port of Track.getTrackState(int location) from the C++ LCIO API.
     *
     * @param trk A track.
     * @param location A TrackState location constant
     * @return The first matching TrackState; null if none is found.
     */
    public static TrackState getTrackStateAtLocation(Track trk, int location) {
        for (TrackState state : trk.getTrackStates()) {
            if (state.getLocation() == location) {
                return state;
            }
        }
        return null;
    }

    public static TrackState getTrackStateAtTarget(Track trk){
        return getTrackStateAtLocation(trk, TrackState.LastLocation);
    }

    public static TrackState getTrackStateAtECal(Track trk) {
        return getTrackStateAtLocation(trk, TrackState.AtCalorimeter);
    }

    public static TrackState getTrackStateAtHodoL1(Track trk) {
        for (TrackState state : trk.getTrackStates()) {
            if (state.getReferencePoint()[0] == BeamlineConstants.HODO_L1_ZPOS) {
                return state;
            }
        }
        return null;
    }

    public static TrackState getTrackStateAtHodoL2(Track trk) {
        for (TrackState state : trk.getTrackStates()) {
            if (state.getReferencePoint()[0] == BeamlineConstants.HODO_L2_ZPOS) {
                return state;
            }
        }
        return null;
    }

    public static Hep3Vector getBField(Detector detector) {
        return detector.getFieldMap().getField(new BasicHep3Vector(0., 0., 500.0));
    }

    //the methods below take Mathematica methods and convert to Java.
    //this removes errors introduced by doing copy-paste-convert-to-Java 
    public static double Sec(double arg) {
        if (Math.cos(arg) != 0.0) {
            return 1 / Math.cos(arg);
        } else {
            return 0;
        }
    }

    public static double Power(double arg, double pow) {
        return Math.pow(arg, pow);
    }

    public static double Cos(double arg) {
        return Math.cos(arg);
    }

    public static double Sin(double arg) {
        return Math.sin(arg);
    }

    public static double Tan(double arg) {
        return Math.tan(arg);
    }

    public static double ArcTan(double x, double y) {
        return FastMath.atan2(y, x);//Java takes the x,y in opposite order
    }

    public static Hep3Vector getMomentum(double omega, double phi0, double tanL, double magneticField) {
        if (abs(omega) < 0.0000001) {
            omega = 0.0000001;
        }
        double Pt = abs((1. / omega) * magneticField * Constants.fieldConversion);
        double px = Pt * Math.cos(phi0);
        double py = Pt * Math.sin(phi0);
        double pz = Pt * tanL;
        return new BasicHep3Vector(px, py, pz);
    }

    /*
    *  mgraham --  9/21/2017
    *  this method writes the track trajectory (x,y,z) to a root-readable text file
    *  useful for debugging shifting reference points
     */
    public static void writeTrajectory(Hep3Vector momentum, Hep3Vector position, int q, double bField, String outFile) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(outFile);
        Trajectory trajectory = getTrajectory(momentum, new org.lcsim.spacegeom.SpacePoint(position), q, bField);
        writer.println("x/D:y/D:z/D");
        double maxLength = 800;//mm
        double startLength = -50;//mm
        for (double alpha = startLength; alpha < maxLength; alpha += 1) {
            Hep3Vector point = trajectory.getPointAtDistance(alpha);
            writer.println(point.x() + "  " + point.y() + "  " + point.z());
        }
        writer.close();
    }

    //This method transforms vector from tracking detector frame to sensor frame
    public static Hep3Vector globalToSensor(Hep3Vector trkpos, HpsSiSensor sensor) {
        SiSensorElectrodes electrodes = sensor.getReadoutElectrodes(ChargeCarrier.HOLE);
        if (electrodes == null) {
            electrodes = sensor.getReadoutElectrodes(ChargeCarrier.ELECTRON);
            System.out.println("Charge Carrier is NULL");
        }
        return electrodes.getGlobalToLocal().transformed(trkpos);
    }

    /**
     *
     */
    public static boolean detectorElementContainsPoint(Hep3Vector trackPosition, DetectorElement sensor, double tolerance) {
        boolean debug = false;

        ITransform3D localToGlobal = sensor.getGeometry().getLocalToGlobal();

        Box sensorSolid = (Box) sensor.getGeometry().getLogicalVolume().getSolid();
        Polygon3D sensorFace = sensorSolid.getFacesNormalTo(new BasicHep3Vector(0, 0, 1)).get(0);

        List<Point3D> vertices = new ArrayList<Point3D>();
        for (Point3D vertex : sensorFace.getVertices()) {
            localToGlobal.transform(vertex);
            vertices.add(new Point3D(vertex.x(), vertex.y(), trackPosition.z()));
        }

        Point3D trackPositionPoint = new Point3D(trackPosition);
        Polygon3D transformedSensorFace;
        try {
            transformedSensorFace = new Polygon3D(vertices);
        } catch (RuntimeException ex) {
            return false;
        }

        if (debug) {
            System.out.println("sensorContainsTrack:  Vertex 1 Position: " + vertices.get(0).toString());
            System.out.println("sensorContainsTrack:  Vertex 2 Position: " + vertices.get(1).toString());
            System.out.println("sensorContainsTrack:  Vertex 3 Position: " + vertices.get(2).toString());
            System.out.println("sensorContainsTrack:  Vertex 4 Position: " + vertices.get(3).toString());
            System.out.println("sensorContainsTrack:  Track Position: " + trackPositionPoint.toString());

            System.out.printf("Track-to-vertex Position: %f \n", GeomOp3D.distanceBetween(trackPositionPoint, transformedSensorFace));
        }
        double distance = GeomOp3D.distanceBetween(trackPositionPoint, transformedSensorFace);
        return (Math.abs(distance) < tolerance);
        //return GeomOp3D.intersects(trackPositionPoint, transformedSensorFace);
    }

    public static boolean detectorElementContainsPoint(Hep3Vector trackPosition, DetectorElement sensor) {
        return detectorElementContainsPoint(trackPosition, sensor, 0.0);
    }

    //This method return the layer number and the module number of a sensor given the volume and the millepedeID 
    public static Pair<Integer, Integer> getLayerSide(int volume, int millepedeID) {

        Integer retLy = null;
        Integer retMod = null;
        //top
        if (volume == 1) {
            if (millepedeID < 9) {
                retLy = millepedeID;
                retMod = 0;
            } else {
                if (millepedeID == 9 || millepedeID == 10) {
                    retLy = millepedeID;
                    retMod = 0;
                } else if (millepedeID == 11 | millepedeID == 12) {
                    retLy = millepedeID - 2;
                    retMod = 2;
                } else if (millepedeID == 13 | millepedeID == 14) {
                    retLy = millepedeID - 2;
                    retMod = 0;
                } else if (millepedeID == 15 | millepedeID == 16) {
                    retLy = millepedeID - 4;
                    retMod = 2;
                } else if (millepedeID == 17 | millepedeID == 18) {
                    retLy = millepedeID - 4;
                    retMod = 0;
                } else if (millepedeID == 19 | millepedeID == 20) {
                    retLy = millepedeID - 6;
                    retMod = 2;
                }
            }
        } //bottom
        else {
            if (millepedeID < 9) {
                retLy = millepedeID;
                retMod = 1;
            } else {
                if (millepedeID == 9 || millepedeID == 10) {
                    retLy = millepedeID;
                    retMod = 1;
                } else if (millepedeID == 11 | millepedeID == 12) {
                    retLy = millepedeID - 2;
                    retMod = 3;
                } else if (millepedeID == 13 | millepedeID == 14) {
                    retLy = millepedeID - 2;
                    retMod = 1;
                } else if (millepedeID == 15 | millepedeID == 16) {
                    retLy = millepedeID - 4;
                    retMod = 3;
                } else if (millepedeID == 17 | millepedeID == 18) {
                    retLy = millepedeID - 4;
                    retMod = 1;
                } else if (millepedeID == 19 | millepedeID == 20) {
                    retLy = millepedeID - 6;
                    retMod = 3;
                }
            }
        }

        return new Pair<Integer, Integer>(retLy, retMod);
    }
    //This methods checks if a track has only hole hits in the back of the detector
    //return true if all back layers have hole hits, false if all back layers have slot hits

    public static boolean isHoleTrack(Track trk) {

        boolean holeTrack = false;

        TrackState trackState = trk.getTrackStates().get(0);
        boolean isTop = true;
        if (trackState.getTanLambda() < 0) {
            isTop = false;
        }

        //System.out.println("--------------");
        for (TrackerHit hit : trk.getTrackerHits()) {

            int stripLayer = ((HpsSiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement()).getLayerNumber();
            int hpslayer = (stripLayer + 1) / 2;
            String side = ((HpsSiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement()).getSide();

            if (isTop) {
                if (hpslayer == 5 || hpslayer == 6 || hpslayer == 7) {
                    if (side == "ELECTRON") {
                        holeTrack = true;
                    }
                }
            } else {
                if (hpslayer == 5 || hpslayer == 6 || hpslayer == 7) {
                    if (side == "ELECTRON") {
                        holeTrack = true;
                    }
                }
            }

            String moduleName = ((HpsSiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement()).getName();
            int hpsmodule = ((HpsSiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement()).getModuleNumber();
            int MPID = ((HpsSiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement()).getMillepedeId();

            //System.out.println("Hit on track :: " + moduleName);
            //System.out.println("Layer="+hpslayer+" Module=" + hpsmodule +" MPID=" + MPID+" side="+side +" top=" +isTop);
        }
        //System.out.println("Track is hole=" + holeTrack);
        //System.out.println("==========");

        return holeTrack;
    }
}
