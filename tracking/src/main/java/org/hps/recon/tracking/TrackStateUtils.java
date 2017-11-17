package org.hps.recon.tracking;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.geometry.FieldMap;
import org.lcsim.util.swim.Trajectory;

/**
 * Utilities for retrieving TrackStates.
 *
 * @author Miriam Diamond
 *
 */

public class TrackStateUtils {

    public static Hep3Vector getLocationExtrapolated(TrackState ts, HpsSiSensor sensor, Hep3Vector startPosition, double epsilon, FieldMap fieldMap) {
        HelicalTrackFit helicalTrackFit = TrackUtils.getHTF(ts);

        // start position: x along beam line (tracking frame!)
        Hep3Vector currentPosition = startPosition;
        // Calculate the path length to the start position
        double pathToStart = HelixUtils.PathToXPlane(helicalTrackFit, startPosition.x(), 0., 0).get(0);

        // Get the momentum of the track
        double bFieldY = fieldMap.getField(new BasicHep3Vector(0, 0, startPosition.x())).y();
        double p = Math.abs(helicalTrackFit.p(bFieldY));
        // Get a unit vector giving the track direction at the start
        Hep3Vector helixDirection = HelixUtils.Direction(helicalTrackFit, pathToStart);
        // Calculate the momentum vector at the start
        Hep3Vector currentMomentum = VecOp.mult(p, helixDirection);

        // HACK: LCSim doesn't deal well with negative fields so they are
        // turned to positive for tracking purposes. As a result,
        // the charge calculated using the B-field, will be wrong
        // when the field is negative and needs to be flipped.
        double q = Math.signum(ts.getOmega());
        if (bFieldY < 0)
            q = q * (-1);

        // Swim the track through the B-field until the end point is reached
        double endPositionX = sensor.getGeometry().getPosition().z();
        Hep3Vector currentPositionDet = null;

        double distance = endPositionX - currentPosition.x();
        double stepSize = distance / 100.0;
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

            if ((Math.abs(endPositionX - currentPositionTry.x()) > epsilon) && (Math.signum(endPositionX - currentPositionTry.x()) != sign)) {
                // went too far, try again with smaller step-size
                if (Math.abs(stepSize) > 0.001) {
                    stepSize /= 2.0;
                    continue;
                } else {
                    break;
                }
            }
            currentPosition = currentPositionTry;

            distance = Math.abs(endPositionX - currentPosition.x());
            // Calculate the momentum vector at the new position. This will
            // be used when creating the trajectory that will be used to
            // extrapolate the track in the next iteration.
            currentMomentum = VecOp.mult(currentMomentum.magnitude(), trajectory.getUnitTangentAtLength(stepSize));
        }

        return CoordinateTransformations.transformVectorToDetector(currentPosition);
    }

    public static Hep3Vector getLocationAtSensor(Track track, HpsSiSensor sensor, double bfield) {
        int millepedeID = sensor.getMillepedeId();

        // try to get trackstate at sensor directly
        TrackState tsAtSensor = TrackStateUtils.getTrackStateAtSensor(track, millepedeID);
        if (tsAtSensor != null)
            return getLocationAtSensor(tsAtSensor, sensor, bfield);

        return null;
    }

    public static Hep3Vector getLocationAtSensor(TrackState ts, HpsSiSensor sensor, double bfield) {
        return getLocationAtSensor(TrackUtils.getHTF(ts), sensor, bfield);
    }

    public static Hep3Vector getLocationAtSensor(HelicalTrackFit htf, HpsSiSensor sensor, double bfield) {
        // get origin and normal of sensor, in global tracking coordinates
        Hep3Vector point_on_plane = sensor.getGeometry().getPosition();
        Hep3Vector pointInTrackingFrame = CoordinateTransformations.transformVectorToTracking(point_on_plane);
        Hep3Vector w = sensor.getGeometry().getLocalToGlobal().rotated(new BasicHep3Vector(0, 0, 1));
        Hep3Vector wInTrackingFrame = CoordinateTransformations.transformVectorToTracking(w);
        wInTrackingFrame = VecOp.unit(wInTrackingFrame);

        // get helix intercept: in global tracking coordinates
        double s_origin = HelixUtils.PathToXPlane(htf, pointInTrackingFrame.x(), 0., 0).get(0);
        Hep3Vector theInt = TrackUtils.getHelixPlaneIntercept(htf, wInTrackingFrame, pointInTrackingFrame, bfield, s_origin);

        // return in global detector coordinates
        if (theInt != null)
            return CoordinateTransformations.transformVectorToDetector(theInt);
        return null;
    }

    public static List<TrackState> getTrackStatesAtLocation(List<TrackState> trackStates, int location) {
        List<TrackState> result = new ArrayList<TrackState>();
        for (TrackState state : trackStates) {
            if (state.getLocation() == location) {
                result.add(state);
            }
        }
        return result;
    }

    public static List<TrackState> getTrackStatesAtLocation(Track trk, int location) {
        return getTrackStatesAtLocation(trk.getTrackStates(), location);
    }

    public static TrackState getTrackStateAtECal(Track trk) {
        return getTrackStateAtECal(trk.getTrackStates());
    }

    public static TrackState getTrackStateAtECal(List<TrackState> trackStates) {
        List<TrackState> result = getTrackStatesAtLocation(trackStates, TrackState.AtCalorimeter);
        if (result.size() == 1)
            return result.get(0);
        return null;
    }

    public static TrackState getTrackStateAtIP(Track trk) {
        return getTrackStateAtIP(trk.getTrackStates());
    }

    public static TrackState getTrackStateAtIP(List<TrackState> trackStates) {
        List<TrackState> result = getTrackStatesAtLocation(trackStates, TrackState.AtIP);
        if (result.size() == 1)
            return result.get(0);
        return null;
    }

    public static TrackState getTrackStateAtFirst(Track trk) {
        return getTrackStateAtFirst(trk.getTrackStates());
    }

    public static TrackState getTrackStateAtFirst(List<TrackState> trackStates) {
        List<TrackState> result = getTrackStatesAtLocation(trackStates, TrackState.AtFirstHit);
        if (result.size() == 1)
            return result.get(0);
        return null;
    }

    public static TrackState getTrackStateAtLast(Track trk) {
        return getTrackStateAtLast(trk.getTrackStates());
    }

    public static TrackState getTrackStateAtLast(List<TrackState> trackStates) {
        List<TrackState> result = getTrackStatesAtLocation(trackStates, TrackState.AtLastHit);
        if (result.size() == 1)
            return result.get(0);
        return null;
    }

    public static TrackState getTrackStateAtVertex(Track trk) {
        return getTrackStateAtVertex(trk.getTrackStates());
    }

    public static TrackState getTrackStateAtVertex(List<TrackState> trackStates) {
        List<TrackState> result = getTrackStatesAtLocation(trackStates, TrackState.AtVertex);
        if (result.size() == 1)
            return result.get(0);
        return null;
    }

    // valid TrackStates at sensor all have location code 0... except at first sensor (code AtFirstHit) and last sensor (code AtLastHit)
    // there are 18 track states at sensors, consecutive in the List
    // if track doesn't hit a sensor, the corresponding TrackState in List is invalid, and is a dummy with location code -1
    public static TrackState getTrackStateAtSensor(List<TrackState> trackStates, int sensorNum) {
        int first = -1;
        int last = -1;
        boolean foundFirst = false;
        boolean foundLast = false;
        TrackState result = null;

        for (TrackState state : trackStates) {
            if (!foundFirst)
                first++;
            last++;
            if (state.getLocation() == TrackState.AtFirstHit) {
                foundFirst = true;
            }
            if (state.getLocation() == TrackState.AtLastHit) {
                if (!foundFirst)
                    return null;
                foundLast = true;
                break;
            }
        }

        if (foundFirst && foundLast) {
            if ((first - 1 + sensorNum) <= last) {
                result = trackStates.get(first - 1 + sensorNum);
                if (result.getLocation() != TrackState.AtOther && result.getLocation() != TrackState.AtFirstHit && result.getLocation() != TrackState.AtLastHit)
                    return null;
            }
        }

        return result;
    }

    public static TrackState getTrackStateAtSensor(Track trk, int sensorNum) {
        return getTrackStateAtSensor(trk.getTrackStates(), sensorNum);
    }

}
