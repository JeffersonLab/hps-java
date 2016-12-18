package org.hps.recon.tracking;

import java.util.List;

import org.lcsim.detector.converter.compact.subdetector.HpsTracker2;
import org.lcsim.detector.converter.compact.subdetector.SvtStereoLayer;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.base.BaseTrack;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.util.Driver;

/**
 * Driver used to tweak the track parameters in a collection by a user
 * specified amount.
 * 
 * @author <a href="mailto:omoreno@slac.stanford.edu">Omar Moreno</a>
 */
public final class TrackTweakDriver extends Driver {

    /** 
     * Name of the constant denoting the position of the Ecal face in the 
     * compact description.
     */
    private static final String ECAL_POSITION_CONSTANT_NAME = "ecal_dface";

    /** Name of the SVT subdetector volume. */
    private static final String SUBDETECTOR_NAME = "Tracker";
    
    /** The B field map */
    FieldMap bFieldMap = null;
    
    /** The magnitude of the B field used.  */
    private double bField = 0.24; // Tesla
    
    /** Position along the beamline of the Ecal face */
    private double ecalPosition = 0; // mm
   
    /** Z position to start extrapolation from */
    private double extStartPos = 700; // mm

    /** The extrapolation step size */ 
    private double stepSize = 5.0; // mm
    
    /** Top SVT layer 1 z position */
    private double topLayer1Z = 0;
    
    /** Bot SVT layer 1 z position */
    private double botLayer1Z = 0;

    /** Top SVT layer 2 z position */
    private double topLayer2Z = 0;
    
    /** Bot SVT layer 2 z position */
    private double botLayer2Z = 0;
    
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
      
        // Get the field map from the detector object
        bFieldMap = detector.getFieldMap(); 
        
        // Get the B-field from the geometry description 
        bField = TrackUtils.getBField(detector).magnitude();
        
        // Get the position of the Ecal from the compact description
        ecalPosition = detector.getConstants().get(ECAL_POSITION_CONSTANT_NAME).getValue();
    
        // Get the stereo layers from the geometry and build the stereo
        // layer maps
        List<SvtStereoLayer> stereoLayers 
            = ((HpsTracker2) detector.getSubdetector(SUBDETECTOR_NAME).getDetectorElement()).getStereoPairs();

        // Loop through all of the stereo layers and find the midpoint between
        // the sensors of layers 1 & 2.  This will be used to set the track 
        // states at those layers.
        for (SvtStereoLayer stereoLayer : stereoLayers) { 
            System.out.println("Layer: " + stereoLayer.getLayerNumber());
            if (stereoLayer.getLayerNumber() > 2) continue;
            
            HpsSiSensor axialSensor = stereoLayer.getAxialSensor();
            HpsSiSensor stereoSensor = stereoLayer.getStereoSensor();
            
            double axialZ = axialSensor.getGeometry().getPosition().z();
            double stereoZ = stereoSensor.getGeometry().getPosition().z(); 
            double z = (axialZ + stereoZ)/2;
            
            if (stereoLayer.getLayerNumber() == 1) {
                if (axialSensor.isTopLayer()) topLayer1Z = z; 
                else botLayer1Z = z;
            } else if(stereoLayer.getLayerNumber() == 2) {
                if (axialSensor.isTopLayer()) topLayer2Z = z; 
                else botLayer2Z = z;
            }
        }
    }
    
    @Override
    public void process(EventHeader event) {
    
        // If the event doesn't have the specified collection of tracks, throw
        // an exception.
        if (!event.hasCollection(Track.class, trackCollectionName)) {
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
        
           // Extrapolate the tweaked track to the face of the Ecal and get the
           // track state
           TrackState stateIP = TrackUtils.getTrackStateAtLocation(track, TrackState.AtIP);
           if (stateIP == null) { 
               throw new RuntimeException("IP track state for GBL track was not found");
           }
           TrackState stateEcalIP = TrackUtils.extrapolateTrackUsingFieldMap(stateIP, extStartPos, ecalPosition, stepSize, bFieldMap);
           
           // Replace the existing track state at the Ecal
           int ecalTrackStateIndex = track.getTrackStates().indexOf(TrackUtils.getTrackStateAtLocation(track, TrackState.AtCalorimeter));
           track.getTrackStates().set(ecalTrackStateIndex, stateEcalIP);
            
           // Get the track state at the first layer
           double layer1Z = trackState.getTanLambda() > 0 ? topLayer1Z : botLayer1Z; 
           TrackState stateLayer1 = TrackUtils.extrapolateTrackUsingFieldMap(stateIP, extStartPos, layer1Z, stepSize, bFieldMap);
           track.getTrackStates().add(stateLayer1);

           // Get the track state at the first layer
           double layer2Z = trackState.getTanLambda() > 0 ? topLayer2Z : botLayer2Z; 
           TrackState stateLayer2 = TrackUtils.extrapolateTrackUsingFieldMap(stateIP, extStartPos, layer2Z, stepSize, bFieldMap);
           track.getTrackStates().add(stateLayer2);
        }
    
        for (String collection : removeCollections) { 
            event.remove(collection);
        }
    }
}
