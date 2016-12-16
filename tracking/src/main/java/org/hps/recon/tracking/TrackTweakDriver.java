package org.hps.recon.tracking;

import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.event.base.BaseTrack;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.util.Driver;

/**
 * Driver used to tweak the track parameters in a collection by a user
 * specified amount.
 * 
 * @author <a href="mailto:omoreno@slac.stanford.edu">Omar Moreno</a>
 */
public final class TrackTweakDriver extends Driver {

    /** Name of the collection of tracks to tweak. */
    private String trackCollectionName = "GBLTracks";
    
    private final double[] topTrackCorrection = {0, 0, 0, 0, 0};
    private final double[] botTrackCorrection = {0, 0, 0, 0, 0};
    
    /** Default constructor */
    public TrackTweakDriver() {}
    
    public void setTopZ0Correction(double topZ0Correction) {
        topTrackCorrection[HelicalTrackFit.z0Index] = topZ0Correction;
    }

    public void setTopLambdaCorrection(double topLambdaCorrection) {
        topTrackCorrection[HelicalTrackFit.slopeIndex] = topLambdaCorrection;
    }

    public void setTopD0Correction(double topD0Correction) {
        topTrackCorrection[HelicalTrackFit.dcaIndex] = topD0Correction;
    }

    public void setTopPhiCorrection(double topPhiCorrection) {
        topTrackCorrection[HelicalTrackFit.phi0Index] = topPhiCorrection;
    }

    public void setTopOmegaCorrection(double topOmegaCorrection) {
        topTrackCorrection[HelicalTrackFit.curvatureIndex] = topOmegaCorrection;
    }

    public void setBotZ0Correction(double botZ0Correction) {
        botTrackCorrection[HelicalTrackFit.z0Index] = botZ0Correction;
    }

    public void setBotLambdaCorrection(double botLambdaCorrection) {
        botTrackCorrection[HelicalTrackFit.slopeIndex] = botLambdaCorrection;
    }

    public void setBotD0Correction(double botD0Correction) {
        botTrackCorrection[HelicalTrackFit.dcaIndex] = botD0Correction;
    }

    public void setBotPhiCorrection(double botPhiCorrection) {
        botTrackCorrection[HelicalTrackFit.phi0Index] = botPhiCorrection;
    }

    public void setBotOmegaCorrection(double botOmegaCorrection) {
        botTrackCorrection[HelicalTrackFit.curvatureIndex] = botOmegaCorrection;
    }
    
    @Override
    public void process(EventHeader event) {
    
        // If the event doesn't have the specified collection of tracks, throw
        // an exception.
        if(!event.hasCollection(Track.class, trackCollectionName)) {
            throw new RuntimeException("Track collection " + trackCollectionName + " doesn't exist");
        }
        
        // Get the collection of tracks from the event
        List<Track> tracks = event.get(Track.class, trackCollectionName);
        
        // Loop through all tracks in an event and tweak the track parameters
        for (Track track : tracks) { 
            
           BaseTrack tweakedTrack = (BaseTrack) track;
           double[] tweakedTrackParameters = tweakedTrack.getTrackParameters();
           tweakedTrack.setTrackParameters(tweakedTrackParameters, 0);
        } 
    }
}
