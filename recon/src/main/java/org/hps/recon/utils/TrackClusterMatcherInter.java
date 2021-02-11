package org.hps.recon.utils;

import org.lcsim.event.Cluster;
import org.lcsim.event.Track;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;


import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.geometry.FieldMap;

import org.hps.record.StandardCuts;

import java.util.List;
import java.util.HashMap;



public interface TrackClusterMatcherInter {

    public HashMap<Track,Cluster> matchTracksToClusters(EventHeader event, List<List<Track>> trackCollections, List<Cluster> clusters, StandardCuts cuts, int flipSign, boolean useCorrectedClusterPositions, HPSEcal3 ecal, boolean isMC);

    public double getMatchQC(Cluster cluster, ReconstructedParticle particle);

    public void setBFieldMap(FieldMap bFieldMap);

    public void setSnapToEdge(boolean val);

    public void initializeParameterization(String fname);

    public void setTrackCollectionName(String trackCollectionName);

    public void enablePlots(boolean enablePlots);

    public void saveHistograms();


}

