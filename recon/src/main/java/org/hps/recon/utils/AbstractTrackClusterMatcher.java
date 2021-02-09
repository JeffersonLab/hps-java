package org.hps.recon.utils;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.ITree;
import hep.aida.ref.rootwriter.RootFileStore;

import org.lcsim.event.Track;
import org.lcsim.event.Cluster;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;







public abstract class AbstractTrackClusterMatcher implements TrackClusterMatcherInter {

    // Plotting
    protected ITree tree;
    protected IHistogramFactory histogramFactory;
    protected Map<String, IHistogram1D> plots1D;
    protected Map<String, IHistogram2D> plots2D;
    protected String rootFile = "track_cluster_matching_plots.root";
    protected String trackCollectionName;

    public void setTrackCollectionName(String trackCollectionName){
        this.trackCollectionName = trackCollectionName;
    }   

    /**
     * Flag used to determine if plots are enabled/disabled
     */
    protected boolean enablePlots = false;


    AbstractTrackClusterMatcher() {
    }

    public boolean isMatch(Cluster cluster, Track track){

        return true;
    }

    public double getDistance(Cluster cluster, Track track){

        // Get the cluster position
        Hep3Vector cPos = new BasicHep3Vector(cluster.getPosition());

        // Extrapolate the track to the Ecal cluster position
        Hep3Vector tPos = null;
        TrackState trackStateAtEcal = null;
        if (this.useAnalyticExtrapolator) {
            tPos = TrackUtils.extrapolateTrack(track, cPos.z());
        } else {
            if(trackCollectionName.contains("GBLTracks")){
                trackStateAtEcal = TrackUtils.getTrackStateAtECal(track);
                tPos = new BasicHep3Vector(trackStateAtEcal.getReferencePoint());
                tPos = CoordinateTransformations.transformVectorToDetector(tPos);
            }       

            if(trackCollectionName.contains("KalmanFullTracks")){
                trackStateAtEcal = track.getTrackStates().get(track.getTrackStates().size()-1);
                tpos = new BasicHep3Vector(ts_ecal.getReferencePoint());
                tpos = CoordinateTransformations.transformVectorToDetector(tpos);

            }
        }

        return Math.sqrt(Math.pow(cPos.x()-tPos.x(),2)+Math.pow(cPos.y()-tPos.y(),2));


    }


    public void bookHistograms(){
        plots1D = new HashMap<String, IHistogram1D>();
        plots2D = new HashMap<String, IHistogram2D>();

        tree = IAnalysisFactory.create().createTreeFactory().create();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);

    }

    protected void saveHistograms(){

        RootFileStore store = new RootFileStore(rootFile);
        try {
            store.open();
            store.add(tree);
            store.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    protected void setRootFileName(String filename){
        rootFile = filename;

    }


}
