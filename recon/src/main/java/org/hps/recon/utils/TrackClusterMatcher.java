package org.hps.recon.utils;

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

   /*
    * These cuts are set at +/- 3 sigma extracted from Gaussian fits to the 
    * track-cluster residual distributions.
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
    * Determine if a track is matched to a cluster.  Currently, this is 
    * determined by checking that the track and cluster are within the same
    * detector volume of each other and that the extrapolated track position
    * is within some defined distance of the cluster.
    *  
    * @param cluster : The Ecal cluster to check
    * @param track : The SVT track to check
    * @return true if all cuts are pased, false otherwise. 
    */
   private boolean isMatch(Cluster cluster, Track track) { 
       
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
      
       // If all cuts are pased, return true.
       return true;
   }
}
