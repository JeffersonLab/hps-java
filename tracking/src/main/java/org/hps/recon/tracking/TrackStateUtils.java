package org.hps.recon.tracking;

import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;

/**
 * Utilities for retrieving TrackStates.
 *
 * @author Miriam Diamond
 *
 */

public class TrackStateUtils {

    public static Hep3Vector getLocationAtSensor(Track track, HpsSiSensor sensor, double bfield) {
        int millepedeID = sensor.getMillepedeId();

        // try to get trackstate at sensor directly
        TrackState tsAtSensor = TrackStateUtils.getTrackStateAtSensor(track, millepedeID);
        if (tsAtSensor != null)
            return getLocationAtSensor(tsAtSensor, sensor, bfield);

        //        if not available, check if track has states at any sensor
        List<TrackState> tsAtSensorList = TrackStateUtils.getTrackStatesAtLocation(track, 0);
        if ((tsAtSensorList == null) || tsAtSensorList.isEmpty()) {
            // no track states at sensor available, so use track state at IP
            tsAtSensor = TrackStateUtils.getTrackStateAtIP(track);
        } else {
            // find closest previous trackstate
            while ((tsAtSensor == null) && (millepedeID > 0)) {
                millepedeID--;
                tsAtSensor = TrackStateUtils.getTrackStateAtSensor(track, millepedeID);
            }
        }

        if (tsAtSensor != null)
            return getLocationAtSensor(tsAtSensor, sensor, bfield);

        return null;
    }

    public static Hep3Vector getLocationAtSensor(TrackState ts, HpsSiSensor sensor, double bfield) {
        // get origin of sensor, in global tracking coordinates
        Hep3Vector point_on_plane = sensor.getGeometry().getPosition();
        Hep3Vector pointInTrackingFrame = CoordinateTransformations.transformVectorToTracking(point_on_plane);

        // get u, v, w : in local coordinates
        SiSensorElectrodes electrodes = sensor.getReadoutElectrodes(ChargeCarrier.getCarrier(1));
        if (electrodes == null)
            electrodes = sensor.getReadoutElectrodes(ChargeCarrier.getCarrier(-1));
        ITransform3D fromGlobal = sensor.getGeometry().getGlobalToLocal();
        ITransform3D fromElectrodes = electrodes.getLocalToGlobal();
        Hep3Vector u = fromElectrodes.transformed(electrodes.getMeasuredCoordinate(0));
        Hep3Vector v = fromElectrodes.transformed(electrodes.getUnmeasuredCoordinate(0));
        Hep3Vector w = VecOp.cross(v, u);
        Hep3Vector wInLocal = fromGlobal.transformed(w);

        // make HelicalTrackFit
        HelicalTrackFit htf = TrackUtils.getHTF(ts);

        // get helix intercept: in global tracking coordinates
        Hep3Vector theInt = TrackUtils.getHelixPlaneIntercept(htf, wInLocal, pointInTrackingFrame, bfield);

        // return in global detector coordinates
        return CoordinateTransformations.transformVectorToDetector(theInt);
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
