package org.hps.recon.utils;

import org.lcsim.event.Cluster;
import org.lcsim.event.Track;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;

import org.lcsim.geometry.subdetector.HPSEcal3;
import org.hps.record.StandardCuts;

import java.util.List;



public interface TrackClusterMatcherInter {

    public List<ReconstructedParticle> matchTracksToClusters(EventHeader event, List<List<Track>> trackCollections, List<Cluster> clusters, StandardCuts cuts, int flipSign, boolean useCorrectedClusterPositions, HPSEcal3 ecal, boolean isMC);

    //public boolean isPossibleMatch(Cluster cluster, Track track, EventHeader event);
    
    //public double getMatchingCriteria(Cluster cluster, ReconstructedParticle particle, EventHeader event);

    public ReconstructedParticle addTrackToParticle(Track track, int flipSign);

}

