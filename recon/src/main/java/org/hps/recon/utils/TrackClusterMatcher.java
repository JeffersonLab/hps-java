package org.hps.recon.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.ITree;
import hep.aida.ref.rootwriter.RootFileStore;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import org.lcsim.event.Cluster;
import org.lcsim.event.Track;

import org.hps.recon.tracking.TrackUtils;

/**
 * Utility used to determine if a track and cluster are matched.
 * 
 * @author <a href="mailto:moreno1@ucsc.edu">Omar Moreno</a>
 */
public class TrackClusterMatcher {

    // Plotting
    private ITree tree; 
    private IHistogramFactory histogramFactory; 
    private Map<String, IHistogram1D> plots1D;
    private Map<String, IHistogram2D> plots2D;
    
    /** Flag used to determine if plots are enabled/disabled */
    boolean enablePlots = false; 

    /**
     * These cuts are set at +/- 3 sigma extracted from Gaussian fits to the 
     * track-cluster residual distributions.  The data used to determine these
     * limits is run 5772, singles1 triggers. 
     */
    private double topClusterTrackMatchDeltaXLow = -7.61; // mm 
    private double topClusterTrackMatchDeltaXHigh = 12.; // mm 
    private double bottomClusterTrackMatchDeltaXLow = -13.75; // mm 
    private double bottomClusterTrackMatchDeltaXHigh = 6.0; // mm 

    private double topClusterTrackMatchDeltaYLow = -14; // mm 
    private double topClusterTrackMatchDeltaYHigh = 14; // mm 
    private double bottomClusterTrackMatchDeltaYLow = -14; // mm 
    private double bottomClusterTrackMatchDeltaYHigh = 14; // mm 

    /** Constructor */
    TrackClusterMatcher() {};

    /**
     * Enable/disable booking, filling of Ecal cluster and extrapolated track 
     * position plots.
     * 
     * @param enablePlots : true to enable, false to disable
     */
    public void enablePlots(boolean enablePlots) { 
        this.enablePlots = enablePlots;
        if (enablePlots == true) this.bookHistograms();
    }
    
    /**
     * Set the window in which the x residual of the extrapolated bottom track 
     * position at the Ecal and the Ecal cluster position must be within to be
     * considered a 'good match'
     *  
     * @param xLow
     * @param xHigh
     */
    public void setBottomClusterTrackDxCuts(double xLow, double xHigh) {
        this.topClusterTrackMatchDeltaXLow = xLow;
        this.topClusterTrackMatchDeltaXHigh = xHigh;
    }

    /**
     * Set the window in which the y residual of the extrapolated bottom track
     * position at the Ecal and the Ecal cluster position must be within to be 
     * considered a 'good match'
     *  
     * @param yLow
     * @param yHigh
     */
    public void setBottomClusterTrackDyCuts(double yLow, double yHigh) {
        this.topClusterTrackMatchDeltaYLow = yLow;
        this.topClusterTrackMatchDeltaYHigh = yHigh;
    }
    
    /**
     * Set the window in which the x residual of the extrapolated top track position
     * at the Ecal and the Ecal cluster position must be within to be considered
     * a 'good match'
     *  
     * @param xLow
     * @param xHigh
     */
    public void setTopClusterTrackDxCuts(double xLow, double xHigh) {
        this.topClusterTrackMatchDeltaXLow = xLow;
        this.topClusterTrackMatchDeltaXHigh = xHigh;
    }

    /**
     * Set the window in which the y residual of the extrapolated top track position
     * at the Ecal and the Ecal cluster position must be within to be considered
     * a 'good match'
     *  
     * @param yLow
     * @param yHigh
     */
    public void setTopClusterTrackDyCuts(double yLow, double yHigh) {
        this.topClusterTrackMatchDeltaYLow = yLow;
        this.topClusterTrackMatchDeltaYHigh = yHigh;
    }

    /**
     * Determine if a track is matched to a cluster.  Currently, this is 
     * determined by checking that the track and cluster are within the same
     * detector volume of each other and that the extrapolated track position
     * is within some defined distance of the cluster.
     *  
     * @param cluster : The Ecal cluster to check
     * @param track : The SVT track to check
     * @return true if all cuts are pased, false otherwise. 
     */
    public boolean isMatch(Cluster cluster, Track track) { 

        // Check that the track and cluster are in the same detector volume.
        // If not, there is no way they can be a match.
        if ((track.getTrackStates().get(0).getTanLambda() > 0 && cluster.getPosition()[1] < 0)
                || (track.getTrackStates().get(0).getTanLambda() < 0 && cluster.getPosition()[1] > 0 )) return false;

        // Get the cluster position
        Hep3Vector clusterPosition = new BasicHep3Vector(cluster.getPosition());

        // Extrapolate the track to the Ecal cluster position
        // TODO: At some point, this needs to use the fringe field
        Hep3Vector trackPosAtEcal = TrackUtils.extrapolateTrack(track, clusterPosition.z());

        // Calculate the difference between the cluster position at the Ecal and
        // the track in both the x and y directions
        double deltaX = clusterPosition.x() - trackPosAtEcal.x(); 
        double deltaY = clusterPosition.y() - trackPosAtEcal.y();
        
        if (enablePlots) {
            if (track.getTrackStates().get(0).getTanLambda() > 0) { 
                
                plots1D.get("Ecal cluster x - track x @ Ecal - top - all").fill(deltaX); 
                plots2D.get("Ecal cluster x v track x @ Ecal - top - all").fill(clusterPosition.x(), 
                                trackPosAtEcal.x()); 
                plots1D.get("Ecal cluster y - track y @ Ecal - top - all").fill(deltaY); 
                plots2D.get("Ecal cluster y v track y @ Ecal - top - all").fill(clusterPosition.y(), 
                                trackPosAtEcal.y()); 
            
            } else if (track.getTrackStates().get(0).getTanLambda() < 0) { 
                
                plots1D.get("Ecal cluster x - track x @ Ecal - bottom - all").fill(deltaX); 
                plots2D.get("Ecal cluster x v track x @ Ecal - bottom - all").fill(clusterPosition.x(), 
                                trackPosAtEcal.x()); 
                plots1D.get("Ecal cluster y - track y @ Ecal - bottom - all").fill(deltaY); 
                plots2D.get("Ecal cluster y v track y @ Ecal - bottom - all").fill(clusterPosition.y(), 
                                trackPosAtEcal.y()); 
            }
        }

        // Check that dx and dy between the extrapolated track and cluster 
        // positions is reasonable.  Different requirements are imposed on 
        // top and bottom tracks in order to account for offsets.
        if ((track.getTrackStates().get(0).getTanLambda() > 0 && (deltaX > topClusterTrackMatchDeltaXHigh ||
                        deltaX < topClusterTrackMatchDeltaXLow)) || 
            (track.getTrackStates().get(0).getTanLambda() < 0 && (deltaX > bottomClusterTrackMatchDeltaXHigh ||
                        deltaX < bottomClusterTrackMatchDeltaXLow))) return false; 

        if ((track.getTrackStates().get(0).getTanLambda() > 0 && (deltaY > topClusterTrackMatchDeltaYHigh ||
                        deltaY < topClusterTrackMatchDeltaYLow)) || 
            (track.getTrackStates().get(0).getTanLambda() < 0 && (deltaY > bottomClusterTrackMatchDeltaYHigh ||
                        deltaY < bottomClusterTrackMatchDeltaYLow))) return false; 

        if (enablePlots) {
            if (track.getTrackStates().get(0).getTanLambda() > 0) { 
                
                plots1D.get("Ecal cluster x - track x @ Ecal - top - matched").fill(deltaX); 
                plots2D.get("Ecal cluster x v track x @ Ecal - top - matched").fill(clusterPosition.x(), 
                                trackPosAtEcal.x()); 
                plots1D.get("Ecal cluster y - track y @ Ecal - top - matched").fill(deltaY); 
                plots2D.get("Ecal cluster y v track y @ Ecal - top - matched").fill(clusterPosition.y(), 
                                trackPosAtEcal.y()); 
            
            } else if (track.getTrackStates().get(0).getTanLambda() < 0) { 
                
                plots1D.get("Ecal cluster x - track x @ Ecal - bottom - matched").fill(deltaX); 
                plots2D.get("Ecal cluster x v track x @ Ecal - bottom - matched").fill(clusterPosition.x(), 
                                trackPosAtEcal.x()); 
                plots1D.get("Ecal cluster y - track y @ Ecal - bottom - matched").fill(deltaY); 
                plots2D.get("Ecal cluster y v track y @ Ecal - bottom - matched").fill(clusterPosition.y(), 
                                trackPosAtEcal.y()); 
            }
        }
        
        // If all cuts are pased, return true.
        return true;
    }
   
    /** Book histograms of Ecal cluster x/y vs extrapolated track x/y */
    private void bookHistograms() { 
       
        plots1D = new HashMap<String, IHistogram1D>();
        plots2D = new HashMap<String, IHistogram2D>();
        
        tree = IAnalysisFactory.create().createTreeFactory().create();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);
        
        //--- All tracks and clusters ---//
        //-------------------------------//
        
        //--- Top ---//
        plots1D.put("Ecal cluster x - track x @ Ecal - top - all", 
            histogramFactory.createHistogram1D("Ecal cluster x - track x @ Ecal - top - all", 200, -200, 200));

        plots2D.put("Ecal cluster x v track x @ Ecal - top - all", 
            histogramFactory.createHistogram2D("Ecal cluster x v track x @ Ecal - top - all", 200, -200, 200, 200, -200, 200));
        
        plots1D.put("Ecal cluster y - track y @ Ecal - top - all", 
            histogramFactory.createHistogram1D("Ecal cluster y - track y @ Ecal - top - all", 100, -100, 100));

        plots2D.put("Ecal cluster y v track y @ Ecal - top - all", 
            histogramFactory.createHistogram2D("Ecal cluster y v track  @ Ecal - top - all", 100, -100, 100, 100, -100, 100));
   
        //--- Bottom ---//
        plots1D.put("Ecal cluster x - track x @ Ecal - bottom - all", 
            histogramFactory.createHistogram1D("Ecal cluster x - track x @ Ecal - top - all", 200, -200, 200));

        plots2D.put("Ecal cluster x v track x @ Ecal - bottom - all", 
            histogramFactory.createHistogram2D("Ecal cluster x v track x @ Ecal - top - all", 200, -200, 200, 200, -200, 200));
        
        plots1D.put("Ecal cluster y - track y @ Ecal - bottom - all", 
            histogramFactory.createHistogram1D("Ecal cluster y - track y @ Ecal - top - all", 100, -100, 100));

        plots2D.put("Ecal cluster y v track y @ Ecal - bottom - all", 
            histogramFactory.createHistogram2D("Ecal cluster y v track  @ Ecal - top - all", 100, -100, 100, 100, -100, 100));

        //--- Matched tracks ---//
        //----------------------//
        
        //--- Top ---//
        plots1D.put("Ecal cluster x - track x @ Ecal - top - matched", 
            histogramFactory.createHistogram1D("Ecal cluster x - track x @ Ecal - top - matched", 200, -200, 200));

        plots2D.put("Ecal cluster x v track x @ Ecal - top - matched", 
            histogramFactory.createHistogram2D("Ecal cluster x v track x @ Ecal - top - matched", 200, -200, 200, 200, -200, 200));
        
        plots1D.put("Ecal cluster y - track y @ Ecal - top - matched", 
            histogramFactory.createHistogram1D("Ecal cluster y - track y @ Ecal - top - matched", 100, -100, 100));

        plots2D.put("Ecal cluster y v track y @ Ecal - top - matched", 
            histogramFactory.createHistogram2D("Ecal cluster y v track  @ Ecal - top - matched", 100, -100, 100, 100, -100, 100));
   
        //--- Bottom ---//
        plots1D.put("Ecal cluster x - track x @ Ecal - bottom - matched", 
            histogramFactory.createHistogram1D("Ecal cluster x - track x @ Ecal - top - matched", 200, -200, 200));

        plots2D.put("Ecal cluster x v track x @ Ecal - bottom - matched", 
            histogramFactory.createHistogram2D("Ecal cluster x v track x @ Ecal - top - matched", 200, -200, 200, 200, -200, 200));
        
        plots1D.put("Ecal cluster y - track y @ Ecal - bottom - matched", 
            histogramFactory.createHistogram1D("Ecal cluster y - track y @ Ecal - top - matched", 100, -100, 100));

        plots2D.put("Ecal cluster y v track y @ Ecal - bottom - matched", 
            histogramFactory.createHistogram2D("Ecal cluster y v track  @ Ecal - top - matched", 100, -100, 100, 100, -100, 100));
        
    }
    
    /** Save the histograms to a ROO file */
    public void saveHistograms() { 
        
        String rootFile = "track_cluster_matching_plots.root";
        RootFileStore store = new RootFileStore(rootFile);
        try {
            store.open();
            store.add(tree);
            store.close(); 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
