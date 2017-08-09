package org.hps.recon.tracking;

import hep.physics.matrix.BasicMatrix;
import hep.physics.matrix.Matrix;
import hep.physics.matrix.MatrixOp;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.Pair;
import org.hps.recon.tracking.EventQuality.Quality;
import org.hps.recon.tracking.gbl.HelicalTrackStripGbl;

import static org.lcsim.constants.Constants.fieldConversion;

import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.solids.Box;
import org.lcsim.detector.solids.GeomOp3D;
import org.lcsim.detector.solids.Point3D;
import org.lcsim.detector.solids.Polygon3D;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCIOParameters.ParameterName;
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
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.fit.helicaltrack.HelixParamCalculator;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.fit.helicaltrack.HitUtils;
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

/**
 * Assorted helper functions for the track and helix objects in lcsim. Re-use as
 * much of HelixUtils as possible.
 *
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
// TODO: Switch to tracking/LCsim coordinates for the extrapolation output!
public class TrackUtils {

    /**
     * Private constructor to make class static only
     */
    private TrackUtils() {
    }

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

    /**
     * Change reference point of helix (following L3 Internal Note 1666.)
     *
     * @param newRefPoint
     * - The new reference point in XY
     */
    public static double[] getParametersAtNewRefPoint(double[] newRefPoint, TrackState trkState) {
        return getParametersAtNewRefPoint(newRefPoint, trkState.getReferencePoint(), trkState.getParameters());
    }

    /**
     * Change reference point of helix (following L3 Internal Note 1666.)
     *
     * @param newRefPoint
     * - The new reference point in XY
     */
    public static double[] getParametersAtNewRefPoint(double[] newRefPoint, HpsHelicalTrackFit helicalTrackFit) {
        return getParametersAtNewRefPoint(newRefPoint, helicalTrackFit.getRefPoint(), helicalTrackFit.parameters());
    }

    /**
     * Change reference point of helix (following L3 Internal Note 1666.)
     *
     * @param newRefPoint
     * - The new reference point in XY
     */
    public static double[] getParametersAtNewRefPoint(double[] newRefPoint, double[] __refPoint, double[] parameters) {

        double phi0 = parameters[HelicalTrackFit.phi0Index];
        double curvature = parameters[HelicalTrackFit.curvatureIndex];
        double dca = parameters[HelicalTrackFit.dcaIndex];
        double slope = parameters[HelicalTrackFit.slopeIndex];
        double z0 = parameters[HelicalTrackFit.z0Index];

        // take care of phi0 range if needed (this matters for dphi below I
        // think)
        // L3 defines it in the range [-pi,pi]
        if (phi0 > Math.PI) 
            phi0 -= Math.PI * 2;
        
        double dx = newRefPoint[0] - __refPoint[0];
        double dy = newRefPoint[1] - __refPoint[1];
        double sinphi = Math.sin(phi0);
        double cosphi = Math.cos(phi0);
        double R = 1.0 / curvature;

        // calculate new phi
        double phinew = Math.atan2(sinphi - dx / (R - dca), cosphi + dy / (R - dca));

        // difference in phi
        // watch out for ambiguity        
        double dphi = phinew - phi0;        
        if (Math.abs(dphi) > Math.PI)           
            throw new RuntimeException("dphi is large " + dphi + " from phi0 " + phi0 + " and phinew " + phinew + " take care of the ambiguity!!??");
        
        // calculate new dca
        double dcanew = dca + dx * sinphi - dy * cosphi + (dx * cosphi + dy * sinphi) * Math.tan(dphi / 2.);

        // path length from old to new point
        double s = -1.0 * dphi / curvature;

        // new z0
        double z0new = z0 + s * slope;

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

        if (phi0 > Math.PI) 
            phi0 -= Math.PI * 2;
        
        BasicMatrix jac = new BasicMatrix(5, 5);

        //the jacobian elements below are copied & pasted from mg's mathemematica notebook 7/5/17
        jac.setElement(0, 0, (Power(rho, 2) * (-1 + d0 * rho) * Power(Sec((phi0 - ArcTan((dy * rho) / (1 - d0 * rho) + Cos(phi0), (dx * rho) / (-1 + d0 * rho) + Sin(phi0))) / 2.), 2)
                * Power(dx * Cos(phi0) + dy * Sin(phi0), 2) + 2 * (1 - d0 * rho)
                * (Power(dy * rho + Cos(phi0) - d0 * rho * Cos(phi0), 2) + Power(dx * rho + (-1 + d0 * rho) * Sin(phi0), 2)))
                / (2. * (1 - d0 * rho) * (Power(dy * rho + Cos(phi0) - d0 * rho * Cos(phi0), 2) + Power(dx * rho + (-1 + d0 * rho) * Sin(phi0), 2))));

        jac.setElement(0, 1, dx * Cos(phi0) + dy * Sin(phi0) - (rho * Power(Sec((phi0 - ArcTan((dy * rho) / (1 - d0 * rho) + Cos(phi0), (dx * rho) / (-1 + d0 * rho) + Sin(phi0))) / 2.),
                2) * (dx * Cos(phi0) + dy * Sin(phi0)) * ((Power(dx, 2) + Power(dy, 2)) * rho + (dy - d0 * dy * rho) * Cos(phi0) + dx * (-1 + d0 * rho) * Sin(phi0)))
                / (2. * (1 - 2 * d0 * rho + Power(d0, 2) * Power(rho, 2) + Power(dx, 2) * Power(rho, 2) + Power(dy, 2) * Power(rho, 2)
                - 2 * dy * rho * (-1 + d0 * rho) * Cos(phi0) + 2 * dx * rho * (-1 + d0 * rho) * Sin(phi0)))
                + (dy * Cos(phi0) - dx * Sin(phi0)) * Tan((-phi0 + ArcTan((dy * rho) / (1 - d0 * rho) + Cos(phi0), (dx * rho) / (-1 + d0 * rho) + Sin(phi0))) / 2.));

        jac.setElement(0, 2, -(Power(Sec((-phi0 + ArcTan((dy * rho) / (1 - d0 * rho) + Cos(phi0), (dx * rho) / (-1 + d0 * rho) + Sin(phi0))) / 2.), 2)
                * Power(dx * Cos(phi0) + dy * Sin(phi0), 2))
                / (2. * (1 - 2 * d0 * rho + Power(d0, 2) * Power(rho, 2) + Power(dx, 2) * Power(rho, 2) + Power(dy, 2) * Power(rho, 2)
                - 2 * dy * rho * (-1 + d0 * rho) * Cos(phi0) + 2 * dx * rho * (-1 + d0 * rho) * Sin(phi0))));

        jac.setElement(1, 0, -((Power(rho, 2) * (dx * Cos(phi0) + dy * Sin(phi0)))
                / (1 - 2 * d0 * rho + Power(d0, 2) * Power(rho, 2) + Power(dx, 2) * Power(rho, 2) + Power(dy, 2) * Power(rho, 2)
                - 2 * dy * rho * (-1 + d0 * rho) * Cos(phi0) + 2 * dx * rho * (-1 + d0 * rho) * Sin(phi0))));

        jac.setElement(1, 1, ((-1 + d0 * rho) * (-1 + d0 * rho - dy * rho * Cos(phi0) + dx * rho * Sin(phi0)))
                / (1 - 2 * d0 * rho + Power(d0, 2) * Power(rho, 2) + Power(dx, 2) * Power(rho, 2) + Power(dy, 2) * Power(rho, 2) - 2 * dy * rho * (-1 + d0 * rho) * Cos(phi0)
                + 2 * dx * rho * (-1 + d0 * rho) * Sin(phi0)));

        jac.setElement(1, 2, -((dx * Cos(phi0) + dy * Sin(phi0))
                / (1 - 2 * d0 * rho + Power(d0, 2) * Power(rho, 2) + Power(dx, 2) * Power(rho, 2) + Power(dy, 2) * Power(rho, 2)
                - 2 * dy * rho * (-1 + d0 * rho) * Cos(phi0) + 2 * dx * rho * (-1 + d0 * rho) * Sin(phi0))));

        jac.setElement(3, 0, (rho * tanLambda * (dx * Cos(phi0) + dy * Sin(phi0)))
                / (1 - 2 * d0 * rho + Power(d0, 2) * Power(rho, 2) + Power(dx, 2) * Power(rho, 2) + Power(dy, 2) * Power(rho, 2) - 2 * dy * rho * (-1 + d0 * rho) * Cos(phi0)
                + 2 * dx * rho * (-1 + d0 * rho) * Sin(phi0)));

        jac.setElement(3, 1, (tanLambda * ((Power(dx, 2) + Power(dy, 2)) * rho + (dy - d0 * dy * rho) * Cos(phi0) + dx * (-1 + d0 * rho) * Sin(phi0)))
                / (1 - 2 * d0 * rho + Power(d0, 2) * Power(rho, 2) + Power(dx, 2) * Power(rho, 2) + Power(dy, 2) * Power(rho, 2) - 2 * dy * rho * (-1 + d0 * rho) * Cos(phi0)
                + 2 * dx * rho * (-1 + d0 * rho) * Sin(phi0)));

        jac.setElement(3, 2, (tanLambda * (ArcTan((dy * rho) / (1 - d0 * rho) + Cos(phi0), (dx * rho) / (-1 + d0 * rho) + Sin(phi0))
                + (dx * rho * Cos(phi0) + dy * rho * Sin(phi0) - phi0 * (Power(dy * rho + Cos(phi0) - d0 * rho * Cos(phi0), 2)
                + Power(dx * rho + (-1 + d0 * rho) * Sin(phi0), 2)))
                / (Power(dy * rho + Cos(phi0) - d0 * rho * Cos(phi0), 2) + Power(dx * rho + (-1 + d0 * rho) * Sin(phi0), 2)))) / Power(rho, 2));

        jac.setElement(3, 4, (phi0 - ArcTan((dy * rho) / (1 - d0 * rho) + Cos(phi0), (dx * rho) / (-1 + d0 * rho) + Sin(phi0))) / rho);

        jac.setElement(2, 2, 1);
        jac.setElement(3, 3, 1);
        jac.setElement(4, 4, 1);

        Matrix covMatrix = new BasicMatrix(helixcov);
        Matrix jacT = MatrixOp.transposed(jac);
        Matrix first = MatrixOp.mult(covMatrix, jacT);
        Matrix newcov = MatrixOp.mult(jac, first);
        System.out.println(newcov.getNColumns() + " x  " + newcov.getNRows());

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
        return Math.atan2(x, y);
    }

    public static double getX0(TrackState track) {
        return -1 * getDoca(track) * Math.sin(getPhi0(track));
    }

    public static double getR(TrackState track) {
        return 1.0 / track.getOmega();
    }

    public static double getY0(TrackState track) {
        return getDoca(track) * Math.cos(getPhi0(track));
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
     * @param helfit
     * - helix
     * @param unit_vec_normal_to_plane
     * - unit vector normal to the plane
     * @param point_on_plane
     * - point on the plane
     * @param bfield
     * - magnetic field value
     * @return point at intercept
     */
    public static Hep3Vector getHelixPlaneIntercept(HelicalTrackFit helfit, Hep3Vector unit_vec_normal_to_plane, Hep3Vector point_on_plane, double bfield) {
        return getHelixPlaneIntercept(helfit, unit_vec_normal_to_plane, point_on_plane, bfield, 0);
    }

    public static Hep3Vector getHelixPlaneIntercept(HelicalTrackFit helfit, Hep3Vector unit_vec_normal_to_plane, Hep3Vector point_on_plane, double bfield, double initial_s) {
        boolean debug = false;
        // Hep3Vector B = new BasicHep3Vector(0, 0, -1);
        // WTrack wtrack = new WTrack(helfit, -1.0*bfield); //
        Hep3Vector B = new BasicHep3Vector(0, 0, 1);
        WTrack wtrack = new WTrack(helfit, bfield); //
        if (initial_s != 0)
            wtrack.setTrackParameters(wtrack.getHelixParametersAtPathLength(initial_s, B));
        if (debug)
            System.out.printf("getHelixPlaneIntercept:find intercept between plane defined by point on plane %s, unit vec %s, bfield %.3f, h=%s and WTrack \n%s \n", point_on_plane.toString(), unit_vec_normal_to_plane.toString(), bfield, B.toString(), wtrack.toString());
        try {
            Hep3Vector intercept_point = wtrack.getHelixAndPlaneIntercept(point_on_plane, unit_vec_normal_to_plane, B);
            if (debug)
                System.out.printf("getHelixPlaneIntercept: found intercept point at %s\n", intercept_point.toString());
            return intercept_point;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Calculate the point of interception between the helix and a plane in
     * space. Uses an iterative procedure.
     *
     * @param helfit
     * - helix
     * @param strip
     * - strip cluster that will define the plane
     * @param bfield
     * - magnetic field value
     * @return point at intercept
     */
    public static Hep3Vector getHelixPlaneIntercept(HelicalTrackFit helfit, HelicalTrackStripGbl strip, double bfield) {
        Hep3Vector point_on_plane = strip.origin();
        Hep3Vector unit_vec_normal_to_plane = VecOp.cross(strip.u(), strip.v());// strip.w();
        double s_origin = HelixUtils.PathToXPlane(helfit, strip.origin().x(), 0., 0).get(0);
        Hep3Vector intercept_point = getHelixPlaneIntercept(helfit, unit_vec_normal_to_plane, point_on_plane, bfield, s_origin);
        return intercept_point;
    }

    /*
     * Calculates the point on the helix in the x-y plane at the intercept with
     * plane The normal of the plane is in the same x-y plane as the circle.
     * 
     * @param helix
     * 
     * @param vector normal to plane
     * 
     * @param origin of plane
     * 
     * @return point in the x-y plane of the intercept
     */
    public Hep3Vector getHelixXPlaneIntercept(HelicalTrackFit helix, Hep3Vector w, Hep3Vector origin) {
        throw new RuntimeException("this function is not working properly; don't use it");

        // FInd the intercept point x_int,y_int, between the circle and sensor,
        // which becomes a
        // line in the x-y plane in this case.
        // y_int = k*x_int + m
        // R^2 = (y_int-y_c)^2 + (x_int-x_c)^2
        // solve for x_int
    }

    /**
     * Get position of a track extrapolated to the HARP in the HPS test run 2012
     *
     * @param track
     * @return position at HARP
     */
    public static Hep3Vector getTrackPositionAtHarp(Track track) {
        return extrapolateTrack(track, BeamlineConstants.HARP_POSITION_TESTRUN);
    }

    /**
     * Get position of a track extrapolated to the ECAL face in the HPS test run
     * 2012
     *
     * @param track
     * @return position at ECAL
     */
    public static Hep3Vector getTrackPositionAtEcal(Track track) {
        return extrapolateTrack(track, BeamlineConstants.ECAL_FACE);
    }

    public static Hep3Vector getTrackPositionAtEcal(TrackState track) {
        return extrapolateTrack(track, BeamlineConstants.ECAL_FACE);
    }

    /**
     * Extrapolate track to given position. For backwards compatibility.
     *
     * @param track
     * - to be extrapolated
     * @param z
     * @return extrapolated position
     */
    public static Hep3Vector extrapolateTrack(Track track, double z) {
        return extrapolateTrack(track.getTrackStates().get(0), z);
    }

    /**
     * Extrapolate track to given position.
     *
     * @param track
     * - to be extrapolated
     * @param z
     * @return extrapolated position
     */
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
     * @param track
     * - position along the x-axis of the helix in lcsim coordinates
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
        if (z >= magnetDownstreamEdge)
            trackPosition = extrapolateHelixToXPlane(track, magnetDownstreamEdge);
        else if (z <= magnetUpstreamEdge)
            trackPosition = extrapolateHelixToXPlane(track, magnetUpstreamEdge);
        else {
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
     * @param helix
     * - to be extrapolated
     * @param z
     * - position along the x-axis of the helix in lcsim coordiantes
     * @return the extrapolated position
     */
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
     * @param helix
     * input helix object
     * @param origin
     * of the plane to intercept
     * @param normal
     * of the plane to intercept
     * @param eps
     * criteria on the distance to the plane before stopping
     * iteration
     * @return position in space at the intercept of the plane
     */
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
            if (debug)
                System.out.printf("%d d %.10f pos [%.10f %.10f %.10f] dx %.10f\n", nIter, d, pos.x(), pos.y(), pos.z(), dx);
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
        if (dphi_at_x > Math.PI)
            dphi_at_x -= 2.0 * Math.PI;
        if (dphi_at_x < -Math.PI)
            dphi_at_x += 2.0 * Math.PI;
        double s_at_x = -1.0 * dphi_at_x * R;
        double y = dca * Math.cos(phi0) - R * Math.cos(phi0) + R * Math.cos(phi_at_x);
        double z = z0 + s_at_x * slope;
        BasicHep3Vector pos = new BasicHep3Vector(x, y, z);
        // System.out.printf("pos %s xc %f phi_at_x %f dphi_at_x %f s_at_x %f\n",
        // pos.toString(),xc,phi_at_x,dphi_at_x,s_at_x);
        Hep3Vector posXCheck = TrackUtils.extrapolateHelixToXPlane(helix, x);
        if (VecOp.sub(pos, posXCheck).magnitude() > 0.0000001)
            throw new RuntimeException(String.format("ERROR the helix propagation equations do not agree? (%f,%f,%f) vs (%f,%f,%f) in HelixUtils", pos.x(), pos.y(), pos.z(), posXCheck.x(), posXCheck.y(), posXCheck.z()));
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

    public static Map<String, Double> calculateTrackHitResidual(HelicalTrackHit hth, HelicalTrackFit track, boolean includeMS) {

        boolean debug = false;
        Map<String, Double> residuals = new HashMap<String, Double>();

        Map<HelicalTrackHit, MultipleScatter> msmap = track.ScatterMap();
        double msdrphi = 0;
        double msdz = 0;

        if (includeMS) {
            msdrphi = msmap.get(hth).drphi();
            msdz = msmap.get(hth).dz();
        }

        // Calculate the residuals that are being used in the track fit
        // Start with the bendplane y
        double drphi_res = hth.drphi();
        double wrphi = Math.sqrt(drphi_res * drphi_res + msdrphi * msdrphi);
        // This is the normal way to get s
        double s_wrong = track.PathMap().get(hth);
        // This is how I do it with HelicalTrackFits
        double s = HelixUtils.PathToXPlane(track, hth.x(), 0, 0).get(0);
        // System.out.printf("x %f s %f smap %f\n",hth.x(),s,s_wrong);
        if (Double.isNaN(s)) {
            double xc = track.xc();
            double RC = track.R();
            System.out.printf("calculateTrackHitResidual: s is NaN. p=%.3f RC=%.3f, x=%.3f, xc=%.3f\n", track.p(-0.491), RC, hth.x(), xc);
            return residuals;
        }

        Hep3Vector posOnHelix = HelixUtils.PointOnHelix(track, s);
        double resy = hth.y() - posOnHelix.y();
        double erry = includeMS ? wrphi : drphi_res;

        // Now the residual for the "measurement" direction z
        double resz = hth.z() - posOnHelix.z();
        double dz_res = HitUtils.zres(hth, msmap, track);
        double dz_res2 = hth.getCorrectedCovMatrix().diagonal(2);

        if (Double.isNaN(resy)) {
            System.out.printf("calculateTrackHitResidual: resy is NaN. hit at %s posOnHelix=%s path=%.3f wrong_path=%.3f helix:\n%s\n", hth.getCorrectedPosition().toString(), posOnHelix.toString(), s, s_wrong, track.toString());
            return residuals;
        }

        residuals.put("resy", resy);
        residuals.put("erry", erry);
        residuals.put("drphi", drphi_res);
        residuals.put("msdrphi", msdrphi);

        residuals.put("resz", resz);
        residuals.put("errz", dz_res);
        residuals.put("dz_res", Math.sqrt(dz_res2));
        residuals.put("msdz", msdz);

        if (debug) {
            System.out.printf("calculateTrackHitResidual: HTH hit at (%f,%f,%f)\n", hth.x(), hth.y(), hth.z());
            System.out.printf("calculateTrackHitResidual: helix params d0=%f phi0=%f R=%f z0=%f slope=%f chi2=%f/%f chi2tot=%f\n", track.dca(), track.phi0(), track.R(), track.z0(), track.slope(), track.chisq()[0], track.chisq()[1], track.chisqtot());
            System.out.printf("calculateTrackHitResidual: => resz=%f resy=%f at s=%f\n", resz, resy, s);
            // System.out.printf("calculateTrackHitResidual: resy=%f eresy=%f drphi=%f msdrphi=%f \n",resy,erry,drphi_res,msdrphi);
            // System.out.printf("calculateTrackHitResidual: resz=%f eresz=%f dz_res=%f msdz=%f \n",resz,dz_res,Math.sqrt(dz_res2),msdz);
        }

        return residuals;
    }

    public static Map<String, Double> calculateLocalTrackHitResiduals(Track track, HelicalTrackHit hth, HelicalTrackStrip strip, double bFieldInZ) {
        HelicalTrackStripGbl stripGbl = new HelicalTrackStripGbl(strip, true);
        return calculateLocalTrackHitResiduals(track, hth, stripGbl, bFieldInZ);
    }

    public static Map<String, Double> calculateLocalTrackHitResiduals(Track track, HelicalTrackHit hth, HelicalTrackStripGbl strip, double bFieldInZ) {

        SeedTrack st = (SeedTrack) track;
        SeedCandidate seed = st.getSeedCandidate();
        HelicalTrackFit _trk = seed.getHelix();
        Map<HelicalTrackHit, MultipleScatter> msmap = seed.getMSMap();
        double msdrdphi = msmap.get(hth).drphi();
        double msdz = msmap.get(hth).dz();
        return calculateLocalTrackHitResiduals(_trk, strip, msdrdphi, msdz, bFieldInZ);
    }

    public static Map<String, Double> calculateLocalTrackHitResiduals(HelicalTrackFit _trk, HelicalTrackStrip strip, double bFieldInZ) {
        HelicalTrackStripGbl stripGbl = new HelicalTrackStripGbl(strip, true);
        return calculateLocalTrackHitResiduals(_trk, stripGbl, 0.0, 0.0, bFieldInZ);
    }

    public static Map<String, Double> calculateLocalTrackHitResiduals(HelicalTrackFit _trk, HelicalTrackStripGbl strip, double msdrdphi, double msdz, double bFieldInZ) {

        boolean debug = false;
        boolean includeMS = true;

        if (debug)
            System.out.printf("calculateLocalTrackHitResiduals: for strip on sensor %s \n", ((RawTrackerHit) strip.getStrip().rawhits().get(0)).getDetectorElement().getName());

        Hep3Vector u = strip.u();
        Hep3Vector corigin = strip.origin();

        // Find interception with plane that the strips belongs to
        Hep3Vector trkpos = TrackUtils.getHelixPlaneIntercept(_trk, strip, Math.abs(bFieldInZ));

        if (debug) {
            System.out.printf("calculateLocalTrackHitResiduals: strip u %s origin %s \n", u.toString(), corigin.toString());
            System.out.printf("calculateLocalTrackHitResiduals: found interception point with sensor at %s \n", trkpos.toString());
        }

        if (Double.isNaN(trkpos.x()) || Double.isNaN(trkpos.y()) || Double.isNaN(trkpos.z())) {
            System.out.printf("calculateLocalTrackHitResiduals: failed to get interception point (%s) \n", trkpos.toString());
            System.out.printf("calculateLocalTrackHitResiduals: track params\n%s\n", _trk.toString());
            System.out.printf("calculateLocalTrackHitResiduals: track pT=%.3f chi2=[%.3f][%.3f] \n", _trk.pT(bFieldInZ), _trk.chisq()[0], _trk.chisq()[1]);
            // trkpos = TrackUtils.getHelixPlaneIntercept(_trk, strip,
            // bFieldInZ);
            throw new RuntimeException();
        }

        double xint = trkpos.x();
        double phi0 = _trk.phi0();
        double R = _trk.R();
        double s = HelixUtils.PathToXPlane(_trk, xint, 0, 0).get(0);
        double phi = -s / R + phi0;

        Hep3Vector mserr = new BasicHep3Vector(msdrdphi * Math.sin(phi), msdrdphi * Math.sin(phi), msdz);
        double msuError = VecOp.dot(mserr, u);

        Hep3Vector vdiffTrk = VecOp.sub(trkpos, corigin);
        TrackerHitUtils thu = new TrackerHitUtils(debug);
        Hep3Matrix trkToStrip = thu.getTrackToStripRotation(strip.getStrip());
        Hep3Vector vdiff = VecOp.mult(trkToStrip, vdiffTrk);

        double umc = vdiff.x();
        double vmc = vdiff.y();
        double wmc = vdiff.z();
        double umeas = strip.umeas();
        double uError = strip.du();
        double vmeas = 0;
        double vError = (strip.vmax() - strip.vmin()) / Math.sqrt(12);
        double wmeas = 0;
        double wError = 10.0 / Math.sqrt(12); // 0.001;

        if (debug)
            System.out.printf("calculateLocalTrackHitResiduals: vdiffTrk %s vdiff %s umc %f umeas %f du %f\n", vdiffTrk.toString(), vdiff.toString(), umc, umeas, umeas - umc);

        Map<String, Double> res = new HashMap<String, Double>();
        res.put("ures", umeas - umc);
        res.put("ureserr", includeMS ? Math.sqrt(uError * uError + msuError * msuError) : uError);
        res.put("vres", vmeas - vmc);
        res.put("vreserr", vError);
        res.put("wres", wmeas - wmc);
        res.put("wreserr", wError);

        res.put("vdiffTrky", vdiffTrk.y());

        return res;
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
            if (sensor.isTopLayer())
                n[0] = n[0] + 1;
            else
                n[1] = n[1] + 1;
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
        if (nhits[0] >= minhits && nhits[1] == 0)
            return 1;
        else if (nhits[1] >= minhits && nhits[0] == 0)
            return 0;
        else
            return -1;
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

                    return true;
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
                return true;
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

                continue;
            // System.out.printf("YEPP\n");
            tracks.add(t);
        }
        List<TrackerHit> hitsOnTrack = track.getTrackerHits();
        int n_shared = 0;
        for (TrackerHit hit : hitsOnTrack)
            if (isSharedHit(hit, tracks))
                ++n_shared;
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
        if (track1.equals(track2))
            return 0;
        else {
            List<TrackerHit> hitsOnTrack = track1.getTrackerHits();
            int n_shared = 0;
            for (TrackerHit hit : hitsOnTrack)
                if (isSharedHit(hit, track2))
                    ++n_shared;
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

                continue;
            // System.out.printf("YEPP\n");
            tracks.add(t);
        }
        // loop through track list to find the most shared hits between any
        // track
        int mostShared = 0;
        Track sharedTrk = track;
        for (Track tt : tracks)
            if (mostShared < numberOfSharedHits(track, tt)) {
                mostShared = numberOfSharedHits(track, tt);
                sharedTrk = tt;
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
            if (track.getTrackStates().get(0).getMomentum()[0] < EventQuality.instance().getCutValue(EventQuality.Cut.PZ, trk_quality))
                cut(cuts, EventQuality.Cut.PZ);
            if (track.getChi2() >= EventQuality.instance().getCutValue(EventQuality.Cut.CHI2, trk_quality))
                cut(cuts, EventQuality.Cut.CHI2);
            if (numberOfSharedHits(track, tracklist) > ((int) Math.round(EventQuality.instance().getCutValue(EventQuality.Cut.SHAREDHIT, trk_quality))))
                cut(cuts, EventQuality.Cut.SHAREDHIT);
            if (hasTopBotHit(track))
                cut(cuts, EventQuality.Cut.TOPBOTHIT);
            if (track.getTrackerHits().size() < ((int) Math.round(EventQuality.instance().getCutValue(EventQuality.Cut.NHITS, trk_quality))))
                cut(cuts, EventQuality.Cut.NHITS);
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
     * @param mcp
     * MC particle to be transformed
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
     * @param mcp
     * - MC particle to be transformed
     * @param origin
     * - origin to be used for the track
     * @return {@link HelicalTrackFit} object based on the MC particle
     */
    public static HelicalTrackFit getHTF(MCParticle mcp, Hep3Vector origin, double Bz) {
        boolean debug = false;

        if (debug)
            System.out.printf("getHTF\nmcp org %s origin used %s mc p %s\n", mcp.getOrigin().toString(), origin.toString(), mcp.getMomentum().toString());

        Hep3Vector org = CoordinateTransformations.transformVectorToTracking(origin);
        Hep3Vector p = CoordinateTransformations.transformVectorToTracking(mcp.getMomentum());

        if (debug)
            System.out.printf("mcp org %s mc p %s (trans)\n", org.toString(), p.toString());

        // Move to x=0 if needed
        double targetX = BeamlineConstants.DIPOLE_EDGELOW_TESTRUN;
        if (org.x() < targetX) {
            double dydx = p.y() / p.x();
            double dzdx = p.z() / p.x();
            double delta_x = targetX - org.x();
            double y = delta_x * dydx + org.y();
            double z = delta_x * dzdx + org.z();
            double x = org.x() + delta_x;
            if (Math.abs(x - targetX) > 1e-8)
                throw new RuntimeException("Error: origin is not zero!");
            org = new BasicHep3Vector(x, y, z);
            // System.out.printf("org %s p %s -> org %s\n",
            // old.toString(),p.toString(),org.toString());
        }

        if (debug)
            System.out.printf("mcp org %s mc p %s (trans2)\n", org.toString(), p.toString());

        HelixParamCalculator helixParamCalculator = new HelixParamCalculator(p, org, -1 * ((int) mcp.getCharge()), Bz);
        double par[] = new double[5];
        par[HelicalTrackFit.dcaIndex] = helixParamCalculator.getDCA();
        par[HelicalTrackFit.slopeIndex] = helixParamCalculator.getSlopeSZPlane();
        par[HelicalTrackFit.phi0Index] = helixParamCalculator.getPhi0();
        par[HelicalTrackFit.curvatureIndex] = 1.0 / helixParamCalculator.getRadius();
        par[HelicalTrackFit.z0Index] = helixParamCalculator.getZ0();
        HelicalTrackFit htf = getHTF(par);
        if (debug)
            System.out.printf("d0 %f z0 %f R %f phi %f lambda %s\n", htf.dca(), htf.z0(), htf.R(), htf.phi0(), htf.slope());
        return htf;
    }

    public static HelicalTrackFit getHTF(Track track) {
        if (track.getClass().isInstance(SeedTrack.class))
            return ((SeedTrack) track).getSeedCandidate().getHelix();
        else
            return getHTF(track.getTrackStates().get(0));
    }

    public static HelicalTrackFit getHTF(double par[]) {
        // need to have matrix that makes sense? Really?
        SymmetricMatrix cov = new SymmetricMatrix(5);
        for (int i = 0; i < cov.getNRows(); ++i)
            cov.setElement(i, i, 1.);
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

        Map<MCParticle, Integer> particlesOnTrack = new HashMap<MCParticle, Integer>();

        if (debug)
            System.out.printf("getMatchedTruthParticle: getmatched mc particle from %d tracker hits on the track \n", track.getTrackerHits().size());

        for (TrackerHit hit : track.getTrackerHits()) {
            List<MCParticle> mcps = ((HelicalTrackHit) hit).getMCParticles();
            if (mcps == null)
                System.out.printf("getMatchedTruthParticle: warning, this hit (layer %d pos=%s) has no mc particles.\n", ((HelicalTrackHit) hit).Layer(), ((HelicalTrackHit) hit).getCorrectedPosition().toString());
            else {
                if (debug)
                    System.out.printf("getMatchedTruthParticle: this hit (layer %d pos=%s) has %d mc particles.\n", ((HelicalTrackHit) hit).Layer(), ((HelicalTrackHit) hit).getCorrectedPosition().toString(), mcps.size());
                for (MCParticle mcp : mcps) {
                    if (!particlesOnTrack.containsKey(mcp))
                        particlesOnTrack.put(mcp, 0);
                    int c = particlesOnTrack.get(mcp);
                    particlesOnTrack.put(mcp, c + 1);
                }
            }
        }
        if (debug) {
            System.out.printf("Track p=[ %f, %f, %f] \n", track.getTrackStates().get(0).getMomentum()[0], track.getTrackStates().get(0).getMomentum()[1], track.getTrackStates().get(0).getMomentum()[1]);
            System.out.printf("Found %d particles\n", particlesOnTrack.size());
            for (Map.Entry<MCParticle, Integer> entry : particlesOnTrack.entrySet())
                System.out.printf("%d hits assigned to %d p=%s \n", entry.getValue(), entry.getKey().getPDGID(), entry.getKey().getMomentum().toString());
        }
        Map.Entry<MCParticle, Integer> maxEntry = null;
        for (Map.Entry<MCParticle, Integer> entry : particlesOnTrack.entrySet())
            if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
                maxEntry = entry; // if ( maxEntry != null ) { //
        // if(entry.getValue().compareTo(maxEntry.getValue())
        // < 0) continue; //}
        // maxEntry = entry;
        if (debug)
            if (maxEntry != null)
                System.out.printf("Matched particle with pdgId=%d and mom %s to track with charge %d and momentum [%f %f %f]\n", maxEntry.getKey().getPDGID(), maxEntry.getKey().getMomentum().toString(), track.getCharge(), track.getTrackStates().get(0).getMomentum()[0], track.getTrackStates().get(0).getMomentum()[1], track.getTrackStates().get(0).getMomentum()[2]);
            else
                System.out.printf("No truth particle found on this track\n");
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
        for (TrackerHit hit : rth)
            hth.add(makeHelicalTrackHitFromTrackerHit(hit));
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

    public static RelationalTable getHitToStripsTable(EventHeader event) {
        if (hitToStripsCache == null || hitToStripsCache.getFirst() != event) {
            RelationalTable hitToStrips = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
            List<LCRelation> hitrelations = event.get(LCRelation.class, "HelicalTrackHitRelations");
            for (LCRelation relation : hitrelations)
                if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                    hitToStrips.add(relation.getFrom(), relation.getTo());
            hitToStripsCache = new Pair<EventHeader, RelationalTable>(event, hitToStrips);
        }
        return hitToStripsCache.getSecond();
    }

    private static Pair<EventHeader, RelationalTable> hitToRotatedCache = null;

    public static RelationalTable getHitToRotatedTable(EventHeader event) {
        if (hitToRotatedCache == null || hitToRotatedCache.getFirst() != event) {
            RelationalTable hitToRotated = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
            List<LCRelation> rotaterelations = event.get(LCRelation.class, "RotatedHelicalTrackHitRelations");
            for (LCRelation relation : rotaterelations)
                if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                    hitToRotated.add(relation.getFrom(), relation.getTo());
            hitToRotatedCache = new Pair<EventHeader, RelationalTable>(event, hitToRotated);
        }
        return hitToRotatedCache.getSecond();
    }

    public static double getTrackTime(Track track, RelationalTable hitToStrips, RelationalTable hitToRotated) {
        double meanTime = 0;
        List<TrackerHit> stripHits = getStripHits(track, hitToStrips, hitToRotated);
        for (TrackerHit hit : stripHits)
            meanTime += hit.getTime();
        meanTime /= stripHits.size();
        return meanTime;
    }

    public static double getTrackTimeSD(Track track, RelationalTable hitToStrips, RelationalTable hitToRotated) {
        double meanTime = getTrackTime(track, hitToStrips, hitToRotated);
        List<TrackerHit> stripHits = getStripHits(track, hitToStrips, hitToRotated);

        double sdTime = 0;
        for (TrackerHit hit : stripHits)
            sdTime += Math.pow(meanTime - hit.getTime(), 2);
        sdTime = Math.sqrt(sdTime / stripHits.size());

        return sdTime;
    }

    public static List<TrackerHit> getStripHits(Track track, RelationalTable hitToStrips, RelationalTable hitToRotated) {
        List<TrackerHit> hits = new ArrayList<TrackerHit>();
        for (TrackerHit hit : track.getTrackerHits())
            hits.addAll(hitToStrips.allFrom(hitToRotated.from(hit)));
        return hits;
    }

    public static List<TrackerHit> sortHits(Collection<TrackerHit> hits) {
        List<TrackerHit> hitList = new ArrayList<TrackerHit>(hits);
        Collections.sort(hitList, new LayerComparator());
        return hitList;
    }

    private static class LayerComparator implements Comparator<TrackerHit> {

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
        for (TrackerHit hit : track2.getTrackerHits())
            for (TrackerHit hts : (Set<TrackerHit>) hitToStrips.allFrom(hitToRotated.from(hit)))
                if (track1hits.contains(hts))
                    nShared++;
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
        if (nShared == 0)
            return false;
        else
            return true;
    }

    public static int getLayer(TrackerHit strip) {
        return ((RawTrackerHit) strip.getRawHits().get(0)).getLayerNumber();
    }

    /**
     * Compute strip isolation, defined as the minimum distance to another strip
     * in the same sensor. Strips are only checked if they formed valid crosses
     * with the other strip in the cross (passing time and tolerance cuts).
     *
     * @param strip
     * The strip whose isolation is being calculated.
     * @param otherStrip
     * The other strip in the stereo hit.
     * @param hitToStrips
     * @param hitToRotated
     * @return Double_MAX_VALUE if no other strips found.
     */
    public static double getIsolation(TrackerHit strip, TrackerHit otherStrip, RelationalTable hitToStrips, RelationalTable hitToRotated) {
        double nearestDistance = 99999999.0;
        for (TrackerHit cross : (Set<TrackerHit>) hitToStrips.allTo(otherStrip))
            for (TrackerHit crossStrip : (Set<TrackerHit>) hitToStrips.allFrom(cross))
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
                    if (stripMin - crossMax <= 1 && crossMin - stripMax <= 1)
                        continue; // adjacent strips don't count
                    Hep3Vector stripPosition = new BasicHep3Vector(strip.getPosition());
                    Hep3Vector crossStripPosition = new BasicHep3Vector(crossStrip.getPosition());
                    double distance = VecOp.sub(stripPosition, crossStripPosition).magnitude();
                    if (Math.abs(stripPosition.y()) > Math.abs(crossStripPosition.y()))
                        distance = -distance;
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
                    if (Math.abs(distance) < Math.abs(nearestDistance))
                        nearestDistance = distance;
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
            isolations[TrackUtils.getLayer(strips[0]) - 1] = TrackUtils.getIsolation(strips[0], strips[1], hitToStrips, hitToRotated);
            isolations[TrackUtils.getLayer(strips[1]) - 1] = TrackUtils.getIsolation(strips[1], strips[0], hitToStrips, hitToRotated);
        }
        return isolations;
    }

    public static Double[] getIsolations(Track trk, RelationalTable hitToStrips, RelationalTable hitToRotated) {
        return getIsolations(trk, hitToStrips, hitToRotated, 6);
    }

    /**
     * Backward compatibility function for {@code extrapolateTrackUsingFieldMap}
     * .
     */
    public static TrackState extrapolateTrackUsingFieldMap(Track track, double startPositionX, double endPositionX, double stepSize, FieldMap fieldMap) {
        TrackState stateAtIP = null;
        for (TrackState state : track.getTrackStates())
            if (state.getLocation() == TrackState.AtIP)
                stateAtIP = state;
        if (stateAtIP == null)
            throw new RuntimeException("No track state at IP was found so this function shouldn't be used.");

        // Extrapolate this track state
        return extrapolateTrackUsingFieldMap(stateAtIP, startPositionX, endPositionX, stepSize, fieldMap);
    }

    /**
     * Iteratively extrapolates a track to a specified value of x (z in detector
     * frame) using the full 3D field map.
     *
     * @param track
     * The {@link Track} object to extrapolate.
     * @param startPositionX
     * The position from which to start the extrapolation from. The
     * track will be extrapolated to this point using a constant
     * field.
     * @param endPositionX
     * The position to extrapolate the track to.
     * @param stepSize
     * The step size determining how far a track will be extrapolated
     * after every iteration.
     * @param fieldMap
     * The 3D field map
     * @return A {@link TrackState} at the final extrapolation point. Note that
     * the "Tracking" frame is used for the reference point coordinate
     * system.
     */
    public static TrackState extrapolateTrackUsingFieldMap(TrackState track, double startPositionX, double endPositionX, double stepSize, FieldMap fieldMap) {

        // Start by extrapolating the track to the approximate point where the
        // fringe field begins.
        Hep3Vector currentPosition = TrackUtils.extrapolateHelixToXPlane(track, startPositionX);
        // System.out.println("Track position at start of fringe: " +
        // currentPosition.toString());

        // Get the HelicalTrackFit object associated with the track. This will
        // be used to calculate the path length to the start of the fringe and
        // to find the initial momentum of the track.
        HelicalTrackFit helicalTrackFit = TrackUtils.getHTF(track);

        // Calculate the path length to the start of the fringe field.
        double pathToStart = HelixUtils.PathToXPlane(helicalTrackFit, startPositionX, 0., 0).get(0);

        // Get the momentum of the track and calculate the magnitude. The
        // momentum can be calculate using the track curvature and magnetic
        // field strength in the middle of the analyzing magnet.
        // FIXME: The position of the middle of the analyzing magnet should
        // be retrieved from the compact description.
        double bFieldY = fieldMap.getField(new BasicHep3Vector(0, 0, 500.0)).y();
        double p = Math.abs(helicalTrackFit.p(bFieldY));

        // Get a unit vector giving the track direction at the start of the of
        // the fringe field
        Hep3Vector helixDirection = HelixUtils.Direction(helicalTrackFit, pathToStart);
        // Calculate the momentum vector at the start of the fringe field
        Hep3Vector currentMomentum = VecOp.mult(p, helixDirection);
        // System.out.println("Track momentum vector: " +
        // currentMomentum.toString());

        // Get the charge of the track.
        double q = Math.signum(track.getOmega());
        // HACK: LCSim doesn't deal well with negative fields so they are
        // turned to positive for tracking purposes. As a result,
        // the charge calculated using the B-field, will be wrong
        // when the field is negative and needs to be flipped.
        if (bFieldY < 0)
            q = q * (-1);

        // Swim the track through the B-field until the end point is reached.
        // The position of the track will be incremented according to the step
        // size up to ~90% of the final position. At this point, a finer
        // track size will be used.
        boolean stepSizeChange = false;
        while (currentPosition.x() < endPositionX) {

            // The field map coordinates are in the detector frame so the
            // extrapolated track position needs to be transformed from the
            // track frame to detector.
            Hep3Vector currentPositionDet = CoordinateTransformations.transformVectorToDetector(currentPosition);

            // Get the field at the current position along the track.
            bFieldY = fieldMap.getField(currentPositionDet).y();
            // System.out.println("Field along y (z in detector): " + bField);

            // Get a tracjectory (Helix or Line objects) created with the
            // track parameters at the current position.
            Trajectory trajectory = getTrajectory(currentMomentum, new org.lcsim.spacegeom.SpacePoint(currentPosition), q, bFieldY);

            // Using the new trajectory, extrapolated the track by a step and
            // update the extrapolated position.
            currentPosition = trajectory.getPointAtDistance(stepSize);
            // System.out.println("Current position: " + ((Hep3Vector)
            // currentPosition).toString());

            // Calculate the momentum vector at the new position. This will
            // be used when creating the trajectory that will be used to
            // extrapolate the track in the next iteration.
            currentMomentum = VecOp.mult(currentMomentum.magnitude(), trajectory.getUnitTangentAtLength(stepSize));

            // If the position of the track along X (or z in the detector frame)
            // is at 90% of the total distance, reduce the step size.
            if (currentPosition.x() / endPositionX > .80 && !stepSizeChange) {
                stepSize /= 10;
                // System.out.println("Changing step size: " + stepSize);
                stepSizeChange = true;
            }
        }

        // Calculate the track parameters at the Extrapolation point
        double doca = currentPosition.x() * currentPosition.x() + currentPosition.y() * currentPosition.y();
        double phi = TrackUtils.calculatePhi(currentMomentum.x(), currentMomentum.y());
        double curvature = TrackUtils.calculateCurvature(currentMomentum.magnitude(), q, bFieldY);
        double z = currentPosition.z();
        double tanLambda = TrackUtils.calculateTanLambda(currentMomentum.z(), currentMomentum.magnitude());

        double[] trackParameters = new double[5];
        trackParameters[ParameterName.d0.ordinal()] = doca;
        trackParameters[ParameterName.phi0.ordinal()] = phi;
        trackParameters[ParameterName.omega.ordinal()] = curvature;
        trackParameters[ParameterName.z0.ordinal()] = z;
        trackParameters[ParameterName.tanLambda.ordinal()] = tanLambda;

        // Create a track state at the extrapolation point
        TrackState trackState = new BaseTrackState(trackParameters, currentPosition.v(), track.getCovMatrix(), TrackState.AtCalorimeter, bFieldY);

        return trackState;
    }

    public static double calculatePhi(double x, double y, double xc, double yc, double sign) {
        return Math.atan2(y - yc, x - xc) - sign * Math.PI / 2;
    }

    public static double calculatePhi(double px, double py) {
        return Math.atan2(py, px);
    }

    public static double calculateTanLambda(double pz, double p) {
        return Math.atan2(pz, p);
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
        double phi = Math.atan2(p.y(), p.x());
        double lambda = Math.atan2(p.z(), p.rxy());
        double field = B * fieldConversion;

        if (q != 0 && field != 0) {
            double radius = p.rxy() / (q * field);
            // System.out.println("[GetTrajectory] : Current Radius: " +
            // radius);
            return new Helix(r0, radius, phi, lambda);
        } else
            return new Line(r0, phi, lambda);
    }

    /**
     * Port of Track.getTrackState(int location) from the C++ LCIO API.
     *
     * @param trk
     * A track.
     * @param location
     * A TrackState location constant
     * @return The first matching TrackState; null if none is found.
     */
    public static TrackState getTrackStateAtLocation(Track trk, int location) {
        for (TrackState state : trk.getTrackStates())
            if (state.getLocation() == location)
                return state;
        return null;
    }

    public static TrackState getTrackStateAtECal(Track trk) {
        return getTrackStateAtLocation(trk, TrackState.AtCalorimeter);
    }

    public static Hep3Vector getBField(Detector detector) {
        return detector.getFieldMap().getField(new BasicHep3Vector(0., 0., 500.0));
    }

    //the methods below take Mathematica methods and convert to Java.
    //this removes errors introduced by doing copy-paste-convert-to-Java 
    public static double Sec(double arg) {
        if (Math.cos(arg) != 0.0)
            return 1 / Math.cos(arg);
        else
            return 0;
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
        return Math.atan2(y, x);//Java takes the x,y in opposite order
    }

}
