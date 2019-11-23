package org.hps.analysis.MC;

import hep.physics.vec.Hep3Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.TrackData;
import org.hps.recon.tracking.TrackDataDriver;
import org.hps.recon.tracking.TrackResidualsData;
import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseLCRelation;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.util.Driver;

/**
 * Driver used to persist additional {@link org.lcsim.event.Track} information via a 
 * {@link org.lcsim.event.GenericObject} collection. This driver is only useful for 
 * refitting the truth hits on a track.
 *
 * @author Omar Moreno, UCSC
 * @author Sho Uemura, SLAC
 */
public final class TrackDataDriverWithTruth extends Driver {

    /** logger **/

    private static final Logger LOGGER = Logger.getLogger(TrackDataDriver.class.getPackage().getName());

    /** The B field map */
    FieldMap bFieldMap = null;
    double bField = 0;

    /** Collection Names */
    private String TRK_COLLECTION_NAME = "GBLTracks";
    private String helicalTrackHitRelationsCollectionName = "HelicalTrackHitRelations";
    private String rotatedHelicalTrackHitRelationsCollectionName = "RotatedHelicalTrackHitRelations";

    /** Collection name of TrackResidualData objects */
    private static final String TRK_RESIDUALS_COL_NAME = "TrackResiduals";

    /** 
     * Collection name of LCRelations between a Track and  TrackResidualsData
     * objects.
     */
    private static final String TRK_RESIDUALS_REL_COL_NAME = "TrackResidualsRelations";
    List<HpsSiSensor> sensors = null;

    /** Default constructor */
    public TrackDataDriverWithTruth() {
    }

    public void setTrackCollectionName(String input) {
        TRK_COLLECTION_NAME = input;
    }

    public void setHelicalTrackHitRelationsCollectionName(String helicalTrackHitRelationsCollectionName) {
        this.helicalTrackHitRelationsCollectionName = helicalTrackHitRelationsCollectionName;
    }

    public void setRotatedHelicalTrackHitRelationsCollectionName(String rotatedHelicalTrackHitRelationsCollectionName) {
        this.rotatedHelicalTrackHitRelationsCollectionName = rotatedHelicalTrackHitRelationsCollectionName;
    }
    /**
     * Method called by the framework when a new {@link Detector} geometry is
     * loaded. This method is called at the beginning of every run and 
     * provides access to the {@link Detector} object itself.
     * 
     * @param detector LCSim {@link Detector} geometry 
     */
    @Override
    protected void detectorChanged(Detector detector) {

        // Get the field map from the detector object
        bFieldMap = detector.getFieldMap();
        bField = TrackUtils.getBField(detector).magnitude();
        sensors = detector.getSubdetector("Tracker").getDetectorElement().findDescendants(HpsSiSensor.class);

    }

    /**
     * Method called by the framework to process the event.
     * 
     * @param event : LCSim event
     */
    @Override
    protected void process(EventHeader event) {

        // Check if the event contains a collection of the type Track. If it
        // doesn't skip the event.
        if (!event.hasCollection(Track.class))
            return;

        // Get all collections of the type Track from the event. This is
        // required since the event contains a track collection for each of the
        // different tracking strategies.
        List<List<Track>> trackCollections = null;
        if (TRK_COLLECTION_NAME == null) {
            trackCollections = event.get(Track.class);
        } else {
            trackCollections = new ArrayList<List<Track>>();
            trackCollections.add(event.get(Track.class, TRK_COLLECTION_NAME));
        }

        // Get the collection of LCRelations relating RotatedHelicalTrackHits to
        // HelicalTrackHits

        RelationalTable hitToStrips = TruthGBLRefitterDriver.getHitToStripsTable(event,helicalTrackHitRelationsCollectionName);
        RelationalTable hitToRotated = TruthGBLRefitterDriver.getHitToRotatedTable(event,rotatedHelicalTrackHitRelationsCollectionName);

        //        List<HelicalTrackHit> rotatedHths = event.get(HelicalTrackHit.class, ROTATED_HTH_COL_NAME);

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

        double xResidual;
        double yResidual;

        float totalT0;
        float totalHits;
        float trackTime;

        int trackerVolume = -1;

        boolean isFirstHit;

        Hep3Vector stereoHitPosition;
        Hep3Vector trackPosition;
        HelicalTrackHit helicalTrackHit;

        List<Double> t0Residuals = new ArrayList<Double>();
        List<Double> trackResidualsX = new ArrayList<Double>();
        List<Float> trackResidualsY = new ArrayList<Float>();
        List<Integer> stereoLayers = new ArrayList<Integer>();

        // Loop over each of the track collections retrieved from the event
        for (List<Track> tracks : trackCollections) {

            // Loop over all the tracks in the event
            for (Track track : tracks) {
                totalT0 = 0;
                totalHits = 0;
                t0Residuals.clear();
                trackResidualsX.clear();
                trackResidualsY.clear();
                stereoLayers.clear();

                // Change the position of a HelicalTrackHit to be the corrected one.
                // Loop over all stereo hits comprising a track
                for (TrackerHit rotatedStereoHit : track.getTrackerHits()) {
                    HpsSiSensor sensor = null;
                    isFirstHit = true;
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
                    stereoHitPosition = ((HelicalTrackHit) rotatedStereoHit).getCorrectedPosition();

                    trackPosition = TrackUtils.extrapolateTrackPositionToSensor(track, sensor, sensors, bField);
                    if (trackPosition != null) {
                        // Add the stereo layer number associated with the track residual
                        stereoLayers.add(((HelicalTrackHit) rotatedStereoHit).Layer());
                        // Extrapolate the track to the stereo hit position and calculate track residuals
                        Hep3Vector stereoHitPositionDetector = CoordinateTransformations.transformVectorToDetector(stereoHitPosition);
                        xResidual = trackPosition.x() - stereoHitPositionDetector.x();
                        yResidual = trackPosition.y() - stereoHitPositionDetector.y();
                        trackResidualsX.add(xResidual);
                        trackResidualsY.add((float) yResidual);
                    }

                    // Change the persisted position of both 
                    // RotatedHelicalTrackHits and HelicalTrackHits to the
                    // corrected position.
                    helicalTrackHit = (HelicalTrackHit) hitToRotated.from(rotatedStereoHit);
                    ((HelicalTrackHit) rotatedStereoHit).setPosition(stereoHitPosition.v());
                    stereoHitPosition = CoordinateTransformations.transformVectorToDetector(stereoHitPosition);
                    helicalTrackHit.setPosition(stereoHitPosition.v());

                }

                //
                // Add a track state that contains the extrapolated track position and 
                // parameters at the face of the Ecal.
                //
                LOGGER.fine("Extrapolating track with type " + Integer.toString(track.getType()));

                // Extrapolate the track to the face of the Ecal and get the TrackState
                if (TrackType.isGBL(track.getType())) {
                    TrackState stateEcal = TrackUtils.getTrackExtrapAtEcalRK(track, bFieldMap);
                    if (stateEcal != null)
                        track.getTrackStates().add(stateEcal);
                }

                LOGGER.fine(Integer.toString(track.getTrackStates().size()) + " track states for this track at this point:");
                for (TrackState state : track.getTrackStates()) {
                    String s = "type " + Integer.toString(track.getType()) + " location " + Integer.toString(state.getLocation()) + " refPoint (" + state.getReferencePoint()[0] + " " + state.getReferencePoint()[1] + " " + state.getReferencePoint()[2] + ") " + " params: ";
                    for (int i = 0; i < 5; ++i)
                        s += String.format(" %f", state.getParameter(i));
                    LOGGER.fine(s);
                }

                // The track time is the mean t0 of hits on a track
                trackTime = totalT0 / totalHits;

                // Calculate the track isolation constants for each of the 
                // layers
                Double[] isolations = TrackUtils.getIsolations(track, hitToStrips, hitToRotated, 6);
                double qualityArray[] = new double[isolations.length];
                for (int i = 0; i < isolations.length; i++) {
                    qualityArray[i] = isolations[i] == null ? -99999999.0 : isolations[i];
                }

                // Create a new TrackData object and add it to the event
                TrackData trackData = new TrackData(trackerVolume, trackTime, qualityArray);
                trackDataCollection.add(trackData);
                trackDataRelations.add(new BaseLCRelation(trackData, track));

                // Create a new TrackResidualsData object and add it to the event
                TrackResidualsData trackResiduals = new TrackResidualsData((int) trackerVolume, stereoLayers, trackResidualsX, trackResidualsY);
                trackResidualsCollection.add(trackResiduals);
                trackToTrackResidualsRelations.add(new BaseLCRelation(trackResiduals, track));

            }
        }

        // Add all collections to the event
        event.put(TrackData.TRACK_DATA_COLLECTION, trackDataCollection, TrackData.class, 0);
        event.put(TrackData.TRACK_DATA_RELATION_COLLECTION, trackDataRelations, LCRelation.class, 0);
        event.put(TRK_RESIDUALS_COL_NAME, trackResidualsCollection, TrackResidualsData.class, 0);
        event.put(TRK_RESIDUALS_REL_COL_NAME, trackToTrackResidualsRelations, LCRelation.class, 0);
    }
}