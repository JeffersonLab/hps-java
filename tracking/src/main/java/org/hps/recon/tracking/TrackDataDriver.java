package org.hps.recon.tracking;

import hep.physics.vec.Hep3Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseLCRelation;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.util.Driver;

/**
 * Driver used to persist additional {@link Track} information via a
 * {@link GenericObject}.
 *
 * @author Omar Moreno, UCSC
 * @author Sho Uemura, SLAC
 */
public final class TrackDataDriver extends Driver {

    /** logger **/
    private static Logger LOGGER  = Logger.getLogger(TrackDataDriver.class.getPackage().getName());
    
    
    /** The B field map */
    FieldMap bFieldMap = null;
    
    /** Collection Names */
    
    /** Collection name of TrackResidualData objects */
    private static final String TRK_RESIDUALS_COL_NAME = "TrackResiduals";
    
    /** 
     * Collection name of LCRelations between a Track and Rotated 
     * HelicalTrackHits 
     */
    private static final String ROTATED_HTH_REL_COL_NAME = "RotatedHelicalTrackHitRelations";
    
    /** Collection name of Rotated HelicalTrackHits */
    private static final String ROTATED_HTH_COL_NAME = "RotatedHelicalTrackHits";
    
    /** 
     * Collection name of LCRelations between a Track and  TrackResidualsData
     * objects.
     */
    private static final String TRK_RESIDUALS_REL_COL_NAME = "TrackResidualsRelations";

    /** 
     * Name of the constant denoting the position of the Ecal face in the 
     * compact description.
     */
    private static final String ECAL_POSITION_CONSTANT_NAME = "ecal_dface";
   
    /** Position of the Ecal face */
    private double ecalPosition = 0; // mm
   
    /** Z position to start extrapolation from */
    double extStartPos = 700; // mm
   
    /** The extrapolation step size */ 
    double stepSize = 5.0; // mm
    
    /** Default constructor */
    public TrackDataDriver() {
    }
   
    /**
     * Set the position along Z where the extrapolation of a track should
     * begin.
     * 
     * @param extStartPoint Position along Z where the extrapolation should 
     *                      begin
     */
    void setExtrapolationStartPosition(double extStartPos) { 
        this.extStartPos = extStartPos; 
    }
   
    /**
     * Set the extrapolation length between iterations through the magnetic 
     * field.
     * 
     * @param stepSize The extrapolation length between iterations in mm. 
     */
    void setStepSize(double stepSize) { 
        this.stepSize = stepSize; 
    }
    
    /**
     * Method called by the framework when a new {@link Detector} geometry is
     * loaded. This method is called at the beginning of every run and 
     * provides access to the {@link Detector} object itself.
     * 
     * @param detector LCSim {@link Detector} geometry 
     */     
    protected void detectorChanged(Detector detector) { 
       
        // Get the field map from the detector object
        bFieldMap = detector.getFieldMap(); 
        
        // Get the position of the Ecal from the compact description
        ecalPosition = detector.getConstants().get(ECAL_POSITION_CONSTANT_NAME).getValue();
    }
   
    /**
     * Method called by the framework to process the event.
     * 
     * @param event : LCSim event
     */
    protected void process(EventHeader event) {

        // Check if the event contains a collection of the type Track. If it
        // doesn't skip the event.
        if (!event.hasCollection(Track.class))
            return;

        // Get all collections of the type Track from the event. This is
        // required since the event contains a track collection for each of the
        // different tracking strategies.
        List<List<Track>> trackCollections = event.get(Track.class);

        // Get the collection of LCRelations relating RotatedHelicalTrackHits to
        // HelicalTrackHits
        List<LCRelation> rotatedHthToHthRelations = event.get(LCRelation.class, ROTATED_HTH_REL_COL_NAME);
        BaseRelationalTable hthToRotatedHth = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE,
                        RelationalTable.Weighting.UNWEIGHTED);
        hthToRotatedHth.addRelations(rotatedHthToHthRelations);

        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);

        List<HelicalTrackHit> rotatedHths = event.get(HelicalTrackHit.class, ROTATED_HTH_COL_NAME);

        // Create a container that will be used to store all TrackData objects.
        List<TrackData> trackDataCollection = new ArrayList<TrackData>();

        // Create a container that will be used to store all LCRelations between
        // a TrackData object and the corresponding Track
        List<LCRelation> trackDataRelations = new ArrayList<LCRelation>();

        // Create a collection to hold the track residuals
        List<TrackResidualsData> trackResidualsCollection = new ArrayList<TrackResidualsData>();

        // Create a collection of LCRelations between a track and the track
        // residuals
        List<LCRelation> trackToTrackResidualsRelations = new ArrayList<LCRelation>();

        double xResidual = 0;
        double yResidual = 0;

        float totalT0 = 0;
        float totalHits = 0;
        float trackTime = 0;

        int trackerVolume = -1;

        boolean isFirstHit = true;

        HpsSiSensor sensor = null;
        Hep3Vector stereoHitPosition = null;
        Hep3Vector trackPosition = null;
        HelicalTrackHit helicalTrackHit = null;

        List<Double> t0Residuals = new ArrayList<Double>();
        List<Double> trackResidualsX = new ArrayList<Double>();
        List<Float> trackResidualsY = new ArrayList<Float>();
        List<Integer> sensorLayers = new ArrayList<Integer>();
        List<Integer> stereoLayers = new ArrayList<Integer>();

        // Loop over each of the track collections retrieved from the event
        for (List<Track> tracks : trackCollections) {
            
            

            // Loop over all the tracks in the event
            for (Track track : tracks) {

                totalT0 = 0;
                totalHits = 0;
                t0Residuals.clear();
                sensorLayers.clear();
                trackResidualsX.clear();
                trackResidualsY.clear();
                stereoLayers.clear();
                isFirstHit = true;

                //
                // Change the position of a HelicalTrackHit to be the corrected
                // one.
                // FIXME: Now that multiple track collections are being used, 
                //        which track should be used to apply the correction? 
                //        The best track from each of the collections?  
                //        How is the "best track" defined? --OM
                //
                // Loop over all stereo hits comprising a track
                for (TrackerHit rotatedStereoHit : track.getTrackerHits()) {

                    // Add the stereo layer number associated with the track
                    // residual
                    stereoLayers.add(((HelicalTrackHit) rotatedStereoHit).Layer());

                    // Extrapolate the track to the stereo hit position and
                    // calculate track residuals
                    stereoHitPosition = ((HelicalTrackHit) rotatedStereoHit).getCorrectedPosition();
                    trackPosition = TrackUtils.extrapolateTrack(track, stereoHitPosition.x());
                    xResidual = trackPosition.x() - stereoHitPosition.y();
                    yResidual = trackPosition.y() - stereoHitPosition.z();
                    trackResidualsX.add(xResidual);
                    trackResidualsY.add((float) yResidual);

                    //
                    // Change the persisted position of both 
                    // RotatedHelicalTrackHits and HelicalTrackHits to the
                    // corrected position.
                    //
                    
                    // Get the HelicalTrackHit corresponding to the 
                    // RotatedHelicalTrackHit associated with a track
                    helicalTrackHit = (HelicalTrackHit) hthToRotatedHth.from(rotatedStereoHit);
                    ((HelicalTrackHit) rotatedStereoHit).setPosition(stereoHitPosition.v());
                    stereoHitPosition = CoordinateTransformations.transformVectorToDetector(stereoHitPosition);
                    helicalTrackHit.setPosition(stereoHitPosition.v());

                    // Loop over the clusters comprising the stereo hit
                    for (HelicalTrackStrip cluster : ((HelicalTrackCross) rotatedStereoHit).getStrips()) {

                        totalT0 += cluster.time();
                        totalHits++;

                        if (isFirstHit) {
                            sensor = (HpsSiSensor) ((RawTrackerHit) cluster.rawhits().get(0)).getDetectorElement();
                            if (sensor.isTopLayer()) {
                                trackerVolume = 0;
                            } else if (sensor.isBottomLayer()) {
                                trackerVolume = 1;
                            }
                            isFirstHit = false;
                        }
                    }
                }

                //
                // Add a track state that contains the extrapolated track position and 
                // parameters at the face of the Ecal.
                //
                LOGGER.info("Extrapolating track with type " + Integer.toString(track.getType()) );

                // Extrapolate the track to the face of the Ecal and get the TrackState
                if( TrackType.isGBL(track.getType())) {
                    TrackState stateLast = null;
                    TrackState stateIP = null;
                    for(int ist= 0; ist < track.getTrackStates().size(); ist++) {
                        if( track.getTrackStates().get(ist).getLocation() == TrackState.AtLastHit ) 
                            stateLast = track.getTrackStates().get(ist);
                        if( track.getTrackStates().get(ist).getLocation() == TrackState.AtIP ) 
                            stateIP = track.getTrackStates().get(ist);
                    }
                    if( stateLast == null)
                        throw new RuntimeException("last hit track state for GBL track was not found");
                    TrackState stateEcal = TrackUtils.extrapolateTrackUsingFieldMap(stateLast, extStartPos, ecalPosition, stepSize, bFieldMap);
                    track.getTrackStates().add(stateEcal);
                    //if( stateIP == null)
                    //    throw new RuntimeException("IP track state for GBL track was not found");
                    //TrackState stateEcalIP = TrackUtils.extrapolateTrackUsingFieldMap(stateIP, extStartPos, ecalPosition, stepSize, bFieldMap);
                    //track.getTrackStates().add(stateEcalIP);
                   
                } else {
                    LOGGER.info("Extrapolate seed track to ECal from vertex");
                    TrackState state = TrackUtils.extrapolateTrackUsingFieldMap(track, extStartPos, ecalPosition, stepSize, bFieldMap);
                    track.getTrackStates().add(state);
                }
                
                LOGGER.info(Integer.toString(track.getTrackStates().size()) +  " track states for this track at this point:");
                for(TrackState state : track.getTrackStates()) {
                    String s = "type " + Integer.toString(track.getType()) + " location " + Integer.toString(state.getLocation()) + " refPoint (" + state.getReferencePoint()[0] + " " + state.getReferencePoint()[1] + " " + state.getReferencePoint()[2] + ") " + " params: ";
                    for(int i=0;i<5;++i) s += String.format(" %f", state.getParameter(i));
                    LOGGER.info(s);
                }
                
                
                // The track time is the mean t0 of hits on a track
                trackTime = totalT0 / totalHits;
             
                // Calculate the track isolation constants for each of the 
                // layers
                Double[] isolations = TrackUtils.getIsolations(track, hitToStrips, hitToRotated);
                double qualityArray[] = new double[isolations.length];
                for (int i = 0; i < isolations.length; i++) {
                    qualityArray[i] = isolations[i] == null ? -99999999.0 : isolations[i];
                }
                
                // Create a new TrackData object and add it to the event
                TrackData trackData = new TrackData(trackerVolume, trackTime, qualityArray);
                trackDataCollection.add(trackData);
                trackDataRelations.add(new BaseLCRelation(trackData, track));

                // Create a new TrackResidualsData object and add it to the event
                TrackResidualsData trackResiduals = new TrackResidualsData((int) trackerVolume, stereoLayers,
                                trackResidualsX, trackResidualsY);
                trackResidualsCollection.add(trackResiduals);
                trackToTrackResidualsRelations.add(new BaseLCRelation(trackResiduals, track));
            }
        }
        
        // Add all collections to the event
        event.put(TrackData.TRACK_DATA_COLLECTION, trackDataCollection, TrackTimeData.class, 0);
        event.put(TrackData.TRACK_DATA_RELATION_COLLECTION, trackDataRelations, LCRelation.class, 0);
        event.put(TRK_RESIDUALS_COL_NAME, trackResidualsCollection, TrackResidualsData.class, 0);
        event.put(TRK_RESIDUALS_REL_COL_NAME, trackToTrackResidualsRelations, LCRelation.class, 0);
    }
}
