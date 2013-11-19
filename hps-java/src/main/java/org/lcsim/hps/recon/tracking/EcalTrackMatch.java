package org.lcsim.hps.recon.tracking;

import hep.physics.vec.Hep3Vector;
import java.util.List;
import org.lcsim.event.Cluster;
import org.lcsim.event.Track;

/**
 *
 * @author phansson
 */
public class EcalTrackMatch {

//    public static final double crystalSizeX = (13.3 + 16.0) / 2;
//    public static final double crystalSizeY = (13.3 + 16.0) / 2;
//    private double RADIUS = crystalSizeX; 
//    private String trackCollectionName = "MatchedTracks";
    Cluster cluster;
    Track matchedTrack;
    private boolean debug = false;

    public EcalTrackMatch() {
        cluster = null;
        matchedTrack = null;
    }

    public EcalTrackMatch(boolean deb) {
        cluster = null;
        matchedTrack = null;
        debug = deb;

    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public Track getMatchedTrack() {
        return matchedTrack;
    }

    public void setCluster(Cluster cl) {
        this.cluster = cl;
    }

    public double dX(Track track) {
        return dist(track, 0);
    }

    public double dY(Track track) {
        return dist(track, 1);
    }

    public double dist(Track track, int dir) {
        Hep3Vector trk_pos = TrackUtils.getTrackPositionAtEcal(track);
        double dx;
        if (dir == 0) {
            dx = cluster.getPosition()[0] - trk_pos.x();
        } else {
            dx = cluster.getPosition()[1] - trk_pos.y();
        }
        if (debug) {
            System.out.println("dist = " + dx + " from cluster to track in " + (dir == 0 ? "X" : "Y") + " to track at " + trk_pos.x() + "," + trk_pos.y() + "," + trk_pos.z());
        }
        return dx;
    }

    public double dR(Track track) {
        Hep3Vector trk_pos = TrackUtils.getTrackPositionAtEcal(track);
        double dx = dX(track);
        double dy = dY(track);
        double dr = Math.sqrt(dx * dx + dy * dy);
        if (debug) {
            System.out.println("dR = " + dr + " to track at " + trk_pos.toString());
        }
        return dr;
    }

    public void match(List<Track> tracks) {
        matchedTrack = null;
        if (debug) {
            System.out.println("Matching cluster at " + cluster.getPosition()[0] + "," + cluster.getPosition()[1] + "," + cluster.getPosition()[2] + " with " + tracks.size() + " tracks.");
        }
        //get the position of the cluster anc compare to tracks at the ecal face
        double dr;
//        SeedTrack trk;
//        Hep3Vector trk_pos;
        double drmin = 999999.9;
        for (Track track : tracks) {
            dr = dR(track);
            if (dr < drmin) {
                drmin = dr;
                matchedTrack = track;
            }
        }
        if (debug) {
            if (matchedTrack == null) {
                System.out.println("No matched track was found");
            } else {
                System.out.println("Matched a track with dr " + dR(matchedTrack));
            }
        }
    }

    public boolean match(List<Track> tracks, double drmax) {
        match(tracks);
        return isMatched(drmax);
    }

    public boolean isMatched(double rad_max) {
        if (matchedTrack == null) {
            return false;
        }
        double dr = dR(matchedTrack);
        return (dr < rad_max ? true : false);
    }

    public boolean isMatchedY(double max) {
        if (matchedTrack == null) {
            return false;
        }
        double dy = dY(matchedTrack);
        return (Math.abs(dy) < max);
    }

    public double getDistanceToTrack() {
        return matchedTrack == null ? -1 : dR(matchedTrack);

    }

    public double getDistanceToTrackInX() {
        return matchedTrack == null ? -1 : dX(matchedTrack);

    }

    public double getDistanceToTrackInY() {
        return matchedTrack == null ? -1 : dY(matchedTrack);

    }
}
