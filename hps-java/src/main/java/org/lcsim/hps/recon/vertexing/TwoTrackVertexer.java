/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lcsim.hps.recon.vertexing;

import org.lcsim.event.Track;
import org.lcsim.hps.event.BeamlineConstants;
import org.lcsim.hps.event.HPSTransformations;
import org.lcsim.hps.recon.tracking.TrackUtils;

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
