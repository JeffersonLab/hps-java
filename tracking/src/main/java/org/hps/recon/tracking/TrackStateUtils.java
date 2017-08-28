package org.hps.recon.tracking;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.event.Track;
import org.lcsim.event.TrackState;

public class TrackStateUtils {
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

    public static TrackState getTrackStateAtSensor(List<TrackState> trackStates, int sensorNum) {
        int first = -1;
        boolean ok = false;
        TrackState result = null;

        // TODO: add more checks here, perhaps using AtLastHit
        for (TrackState state : trackStates) {
            first++;
            if (state.getLocation() == TrackState.AtFirstHit) {
                ok = true;
                break;
            }
        }

        if (ok) {
            if ((first - 1 + sensorNum) < trackStates.size()) {
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
