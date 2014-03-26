package org.hps.recon.vertexing;

import org.hps.conditions.deprecated.BeamlineConstants;
import org.hps.recon.tracking.HPSTransformations;
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
        A1 = HPSTransformations.transformVectorToTracking(TrackUtils.extrapolateTrack(track1, BeamlineConstants.HARP_POSITION_TESTRUN-100));
        A2 = HPSTransformations.transformVectorToTracking(TrackUtils.extrapolateTrack(track1, BeamlineConstants.HARP_POSITION_TESTRUN+100));
        B1 = HPSTransformations.transformVectorToTracking(TrackUtils.extrapolateTrack(track2, BeamlineConstants.HARP_POSITION_TESTRUN-100));
        B2 = HPSTransformations.transformVectorToTracking(TrackUtils.extrapolateTrack(track2, BeamlineConstants.HARP_POSITION_TESTRUN+100));
    }
    
}
