package org.hps.recon.utils;

import org.lcsim.event.Cluster;
import org.lcsim.event.Track;



public interface TrackClusterMatcherInter {

    public boolean isMatch(Cluster cluster, Track track);

    public double getDistance(Cluster cluster, Track track);



}

