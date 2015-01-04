package org.hps.recon.vertexing;

import org.hps.recon.tracking.BeamlineConstants;
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.Track;

/**
 * Class that vertexes two tracks using straight line vertexing class.
 *
 * @author phansson
 */
public class TwoTrackVertexer extends TwoLineVertexer {
    
    public TwoTrackVertexer() {
    }

    public void setTracks(Track track1,Track track2) {
        A1 = CoordinateTransformations.transformVectorToTracking(TrackUtils.extrapolateTrack(track1, BeamlineConstants.HARP_POSITION_TESTRUN-100));
        A2 = CoordinateTransformations.transformVectorToTracking(TrackUtils.extrapolateTrack(track1, BeamlineConstants.HARP_POSITION_TESTRUN+100));
        B1 = CoordinateTransformations.transformVectorToTracking(TrackUtils.extrapolateTrack(track2, BeamlineConstants.HARP_POSITION_TESTRUN-100));
        B2 = CoordinateTransformations.transformVectorToTracking(TrackUtils.extrapolateTrack(track2, BeamlineConstants.HARP_POSITION_TESTRUN+100));
    }
    
}
