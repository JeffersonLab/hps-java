package org.hps.recon.tracking;

import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.base.BaseTrack;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 * Driver used to tweak the track parameters in a collection by a user
 * specified amount.
 * 
 * @author <a href="mailto:omoreno@slac.stanford.edu">Omar Moreno</a>
 */
public final class TrackTweakDriver extends Driver {

    /** The magnitude of the B field used.  */
    private double bField = 0.24; // Tesla
    
    /** Name of the collection of tracks to apply corrections to. */
    private String trackCollectionName = "GBLTracks";
   
    /** 
     * The track parameter corrections that will be applied to all top 
     * tracks. 
     */
    private double[] topTrackCorrection = {0, 0, 0, 0, 0};

    
    /** 
     * The track parameter corrections that will be applied to all bottom 
     * tracks. 
     */
    private double[] botTrackCorrection = {0, 0, 0, 0, 0};
   
    /** List of collections to remove from an event. */
    private String[] removeCollections = {};
    
    /** Default constructor */
    public TrackTweakDriver() {}
   
    /** @param topZ0Correction Z0 correction to apply to top tracks. */
    public void setTopZ0Correction(double topZ0Correction) {
        topTrackCorrection[HelicalTrackFit.z0Index] = topZ0Correction;
    }

    /** 
     * @param topLambdaorrection tan(lambda) correction to apply to top 
     *        tracks. 
     */
    public void setTopLambdaCorrection(double topLambdaCorrection) {
        topTrackCorrection[HelicalTrackFit.slopeIndex] = topLambdaCorrection;
    }

    /** @param topD0Correction D0 correction to apply to top tracks. */
    public void setTopD0Correction(double topD0Correction) {
        topTrackCorrection[HelicalTrackFit.dcaIndex] = topD0Correction;
    }

    /** @param topPhiCorrection phi0 correction to apply to top tracks. */
    public void setTopPhiCorrection(double topPhiCorrection) {
        topTrackCorrection[HelicalTrackFit.phi0Index] = topPhiCorrection;
    }

    /** @param topOmegaCorrection 1/R correction to apply to top tracks. */
    public void setTopOmegaCorrection(double topOmegaCorrection) {
        topTrackCorrection[HelicalTrackFit.curvatureIndex] = topOmegaCorrection;
    }

    /** @param botZ0Correction Z0 correction to apply to bottom tracks. */
    public void setBotZ0Correction(double botZ0Correction) {
        botTrackCorrection[HelicalTrackFit.z0Index] = botZ0Correction;
    }

    /** 
     * @param botLambdaorrection tan(lambda) correction to apply to bottom 
     *        tracks. 
     */
    public void setBotLambdaCorrection(double botLambdaCorrection) {
        botTrackCorrection[HelicalTrackFit.slopeIndex] = botLambdaCorrection;
    }

    /** @param botD0Correction D0 correction to apply to bottom tracks. */
    public void setBotD0Correction(double botD0Correction) {
        botTrackCorrection[HelicalTrackFit.dcaIndex] = botD0Correction;
    }

    /** @param botPhiCorrection phi0 correction to apply to bottom tracks. */
    public void setBotPhiCorrection(double botPhiCorrection) {
        botTrackCorrection[HelicalTrackFit.phi0Index] = botPhiCorrection;
    }

    /** @param botOmegaCorrection 1/R correction to apply to bottom tracks. */
    public void setBotOmegaCorrection(double botOmegaCorrection) {
        botTrackCorrection[HelicalTrackFit.curvatureIndex] = botOmegaCorrection;
    }
    
    /**
     * Specify the collections that will be removed from the event.  This 
     * is meant to be used in cases where the tweaked tracks are used to
     * regenerate other collections e.g. V0Candidates to replace exisiting
     * collections. 
     *  
     * @param collections Collections that will be removed from the event. 
     */
    public void setRemoveCollections(String[] removeCollections) { 
       this.removeCollections = removeCollections; 
    }
   
    @Override
    protected void detectorChanged(Detector detector) {
       
        /** Get the B-field from the geometry description */
        bField = TrackUtils.getBField(detector).magnitude();
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
           
           // Get the track state at the target
           TrackState trackState = track.getTrackStates().get(0);
           
           // Loop through the track parameters and apply the corrections
           double[] tweakedTrackParameters = new double[trackState.getParameters().length];
           for (int param_index = 0; param_index < tweakedTrackParameters.length; ++param_index) {
               tweakedTrackParameters[param_index] 
                       = trackState.getParameter(param_index) 
                           + ((trackState.getTanLambda() > 0) ? topTrackCorrection[param_index] : botTrackCorrection[param_index]);
           }
           // Override the old track parameters with the tweaked parameters
           ((BaseTrack) track).setTrackParameters(tweakedTrackParameters, bField);
        }
       
        for (String collection : removeCollections) { 
            event.remove(collection);
        }
    }
}
