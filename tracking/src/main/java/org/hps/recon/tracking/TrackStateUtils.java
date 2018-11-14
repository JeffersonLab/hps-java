package org.hps.recon.tracking;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.List;

import org.hps.recon.tracking.MaterialSupervisor.SiStripPlane;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelixUtils;

/**
 * Utilities for retrieving TrackStates.
 *
 * @author Miriam Diamond
 *
 */

public class TrackStateUtils {

    public static TrackState getTrackStateAtLayer(Track track, List<HpsSiSensor> sensors, int layer) {
        TrackState atIP = getTrackStateAtIP(track);

        for (HpsSiSensor sensor2 : sensors) {
            if ((atIP.getTanLambda() > 0 && sensor2.isTopLayer()) || (atIP.getTanLambda() < 0 && sensor2.isBottomLayer())) {
                if ((sensor2.getLayerNumber() + 1) / 2 == layer) {
                    return getTrackStateAtSensor(track, sensor2.getMillepedeId());
                }
            }
        }

        return null;
    }

    public static TrackState getPreviousTrackStateAtSensor(Track track, List<HpsSiSensor> sensors, int layer) {
        TrackState ts = null;
        for (int i = layer - 1; i > 0; i--) {
            ts = getTrackStateAtLayer(track, sensors, i);
            if (ts != null)
                break;
        }
        return ts;
    }

    public static Hep3Vector getLocationAtSensor(Track track, HpsSiSensor sensor, double bfield) {

        if (track == null || sensor == null)
            return null;

        int millepedeID = sensor.getMillepedeId();
        TrackState tsAtSensor = TrackStateUtils.getTrackStateAtSensor(track, millepedeID);
        if (tsAtSensor != null)
            return getLocationAtSensor(tsAtSensor, sensor, bfield);

        return null;
    }

    public static Hep3Vector getLocationAtSensor(TrackState ts, HpsSiSensor sensor, double bfield) {
        if (ts == null || sensor == null)
            return null;
        if ((ts.getTanLambda() > 0 && sensor.isTopLayer()) || (ts.getTanLambda() < 0 && sensor.isBottomLayer()))
            return getLocationAtSensor(TrackUtils.getHTF(ts), sensor, bfield);
        return null;
    }

    public static SiStripPlane sensorToPlane(HpsSiSensor sens, List<SiStripPlane> planes) {
        for (SiStripPlane plane : planes) {
            HpsSiSensor sensor = (HpsSiSensor) (plane.getSensor());
            if (sensor == sens)
                return plane;
        }
        return null;
    }

    public static Hep3Vector getLocationAtSensor(HelicalTrackFit htf, HpsSiSensor sensor, double bfield) {
        if (htf == null || sensor == null)
            return null;

        // get origin and normal of sensor, in global tracking coordinates
        Hep3Vector point_on_plane = sensor.getGeometry().getPosition();
        if (point_on_plane == null)
            return null;

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
            if ((state.getLocation() == TrackState.AtFirstHit) || (state.getLocation() == -1)) {
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
