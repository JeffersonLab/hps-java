package org.hps.analysis.MC;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Pair;
import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.MultipleScattering;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.gbl.GBLKinkData;
import org.hps.recon.tracking.gbl.MakeGblTracks;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseLCRelation;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;

/**
 * A Driver which refits tracks using GBL, but only refit tracks with different L1 hits from TrackRefitWithFirstLayerHits. 
 * This is adapted from the nominal GBLRefitterDriver.
 * 
 * @author Matt Solt
 */
public class FirstHitGBLRefitterDriver extends Driver {

    private String inputCollectionName = "Tracks_refit";
    private String outputCollectionName = "GBLTracks_refit";
    private String trackRelationCollectionName = "RefitToGBLTrackRelations";
    private String helicalTrackHitRelationsCollectionName = "HelicalTrackHitRelations";
    private String rawHitCollectionName = "SVTRawTrackerHits";
    private String kinkDataCollectionName = "GBLKinkData_refit";
    private String kinkDataRelationsName = "GBLKinkDataRelations_refit";
    private String trackToMCParticleRelationsName = "TrackTruthToMCParticleRelations";
    private String trackToTrackRefitRelationsName = "GBLTrackToGBLTrackRefitRelations";
    private String refitTrackToTrackRelationsName = "GBLTrackToTrackRefitRelations";
    

    private double bfield;
    private final MultipleScattering _scattering = new MultipleScattering(new MaterialSupervisor());
    private boolean storeTrackStates = true;

    private List<HpsSiSensor> sensors = null;
    FieldMap bFieldMap = null;
    private static final String SUBDETECTOR_NAME = "Tracker";
    protected static Subdetector trackerSubdet;
    
    public void setStoreTrackStates(boolean input) {
        storeTrackStates = input;
    }

    public boolean getStoreTrackStates() {
        return storeTrackStates;
    }

    public void setInputCollectionName(String inputCollectionName) {
        this.inputCollectionName = inputCollectionName;
    }

    public void setOutputCollectionName(String outputCollectionName) {
        this.outputCollectionName = outputCollectionName;
    }
    
    public void setTrackRelationCollectionName(String trackRelationCollectionName) {
        this.trackRelationCollectionName = trackRelationCollectionName;
    }
    
    public void setHelicalTrackHitRelationsCollectionName(String helicalTrackHitRelationsCollectionName) {
        this.helicalTrackHitRelationsCollectionName = helicalTrackHitRelationsCollectionName;
    }
    
    public void setRawHitCollectionName(String rawHitCollectionName) {
        this.rawHitCollectionName = rawHitCollectionName;
    }
    
    public void setKinkDataCollectionName(String kinkDataCollectionName) {
        this.kinkDataCollectionName = kinkDataCollectionName;
    }
    
    public void setKinkDataRelationsName(String kinkDataRelationsName) {
        this.kinkDataRelationsName = kinkDataRelationsName;
    }
    
    public void setTrackToTrackRefitRelationsName(String trackToTrackRefitRelationsName) {
        this.trackToTrackRefitRelationsName = trackToTrackRefitRelationsName;
    }
    
    public void setRefitTrackToTrackRelationsName(String refitTrackToTrackRelationsName) {
        this.refitTrackToTrackRelationsName = refitTrackToTrackRelationsName;
    }

    @Override
    protected void detectorChanged(Detector detector) {
        bfield = Math.abs(TrackUtils.getBField(detector).magnitude());
        _scattering.getMaterialManager().buildModel(detector);
        _scattering.setBField(bfield); // only absolute of B is needed as it's used for momentum calculation only
        
        bFieldMap = detector.getFieldMap();
        
        // Get the HpsSiSensor objects from the tracker detector element
        sensors = detector.getSubdetector(SUBDETECTOR_NAME)
                          .getDetectorElement().findDescendants(HpsSiSensor.class);
        
        trackerSubdet = detector.getSubdetector(SUBDETECTOR_NAME);
    }

    @Override
    protected void process(EventHeader event) {
        if (!event.hasCollection(Track.class, inputCollectionName)){
            System.out.println("System has no input collection");
            return;
        }

        setupSensors(event);
        List<Track> tracks = event.get(Track.class, inputCollectionName);
        List<LCRelation> refitTrackToTrackRelations = event.get(LCRelation.class, refitTrackToTrackRelationsName);
        RelationalTable hitToStrips = getHitToStripsTable(event,helicalTrackHitRelationsCollectionName);

        List<Track> refittedTracks = new ArrayList<Track>();
        List<LCRelation> trackRelations = new ArrayList<LCRelation>();

        List<GBLKinkData> kinkDataCollection = new ArrayList<GBLKinkData>();
        List<LCRelation> kinkDataRelations = new ArrayList<LCRelation>();
        
        List<LCRelation> trackToMCParticleRelations = new ArrayList<LCRelation>();
        List<LCRelation> trackToTrackRefitRelations = new ArrayList<LCRelation>();

        Map<Track, Track> inputToRefitted = new HashMap<Track, Track>();
        for (Track track : tracks) {
            if (getStripHits(track, hitToStrips).size() == 0)
                continue;

            Pair<Track, GBLKinkData> newTrack = MakeGblTracks.refitTrack(TrackUtils.getHTF(track), getStripHits(track, hitToStrips), track.getTrackerHits(), 5, track.getType(), _scattering, bfield, storeTrackStates);
            if (newTrack == null)
                continue;

            Track gblTrk = newTrack.getFirst();

            refittedTracks.add(gblTrk);
            trackRelations.add(new BaseLCRelation(track, gblTrk));
            inputToRefitted.put(track, gblTrk);
            MCFullDetectorTruth truthMatch = new MCFullDetectorTruth(event, track, bFieldMap, sensors, trackerSubdet);
            if(truthMatch != null){
                MCParticle p = truthMatch.getMCParticle();
                trackToMCParticleRelations.add(new BaseLCRelation(gblTrk,p));
            }
            kinkDataCollection.add(newTrack.getSecond());
            kinkDataRelations.add(new BaseLCRelation(newTrack.getSecond(), gblTrk));
            Track oldgblTrk = null;
            for(LCRelation rel:refitTrackToTrackRelations){
                if(rel.getTo().equals(track)){
                    oldgblTrk = (Track) rel.getFrom();
                    break;
                }
            }
            trackToTrackRefitRelations.add(new BaseLCRelation(oldgblTrk,gblTrk));
        }

        // Put the tracks back into the event and exit
        int flag = 1 << LCIOConstants.TRBIT_HITS;
        event.put(outputCollectionName, refittedTracks, Track.class, flag);
        event.put(trackRelationCollectionName, trackRelations, LCRelation.class, 0);
        event.put(kinkDataCollectionName, kinkDataCollection, GBLKinkData.class, 0);
        event.put(kinkDataRelationsName, kinkDataRelations, LCRelation.class, 0);
        event.put(trackToMCParticleRelationsName, trackToMCParticleRelations, LCRelation.class, 0);
        event.put(trackToTrackRefitRelationsName, trackToTrackRefitRelations, LCRelation.class, 0);
    }

    private void setupSensors(EventHeader event) {
        List<RawTrackerHit> rawTrackerHits = null;
        if (event.hasCollection(RawTrackerHit.class, rawHitCollectionName))
            rawTrackerHits = event.get(RawTrackerHit.class, rawHitCollectionName);
        if (event.hasCollection(RawTrackerHit.class, "RawTrackerHitMaker_RawTrackerHits"))
            rawTrackerHits = event.get(RawTrackerHit.class, "RawTrackerHitMaker_RawTrackerHits");

        EventHeader.LCMetaData meta = event.getMetaData(rawTrackerHits);
        // Get the ID dictionary and field information.
        IIdentifierDictionary dict = meta.getIDDecoder().getSubdetector().getDetectorElement().getIdentifierHelper().getIdentifierDictionary();
        int fieldIdx = dict.getFieldIndex("side");
        int sideIdx = dict.getFieldIndex("strip");
        for (RawTrackerHit hit : rawTrackerHits) {
            // if sensor already has a DetectorElement, skip it
            if (hit.getDetectorElement() != null)
                continue;

            // The "side" and "strip" fields needs to be stripped from the ID for sensor lookup.
            IExpandedIdentifier expId = dict.unpack(hit.getIdentifier());
            expId.setValue(fieldIdx, 0);
            expId.setValue(sideIdx, 0);
            IIdentifier strippedId = dict.pack(expId);
            // Find the sensor DetectorElement.
            List<IDetectorElement> des = DetectorElementStore.getInstance().find(strippedId);
            if (des == null || des.size() == 0)
                throw new RuntimeException("Failed to find any DetectorElements with stripped ID <0x" + Long.toHexString(strippedId.getValue()) + ">.");
            else if (des.size() == 1)
                hit.setDetectorElement((SiSensor) des.get(0));
            else
                // Use first sensor found, which should work unless there are sensors with duplicate IDs.
                for (IDetectorElement de : des)
                    if (de instanceof SiSensor) {
                        hit.setDetectorElement((SiSensor) de);
                        break;
                    }
            // No sensor was found.
            if (hit.getDetectorElement() == null)
                throw new RuntimeException("No sensor was found for hit with stripped ID <0x" + Long.toHexString(strippedId.getValue()) + ">.");
        }
    }
    
    //These functions are adapted from the same ones in TrackUtils, but without the "cache"
    static RelationalTable getHitToStripsTable(EventHeader event,String HelicalTrackHitRelationsCollectionName) {
        RelationalTable hitToStrips = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> hitrelations = event.get(LCRelation.class, HelicalTrackHitRelationsCollectionName);
        for (LCRelation relation : hitrelations)
            if (relation != null && relation.getFrom() != null && relation.getTo() != null){
                hitToStrips.add(relation.getFrom(), relation.getTo());
            }
        return hitToStrips;
    }

    static RelationalTable getHitToRotatedTable(EventHeader event, String RotatedHelicalTrackHitRelationsCollectionName) {
        RelationalTable hitToRotated = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> rotaterelations = event.get(LCRelation.class, RotatedHelicalTrackHitRelationsCollectionName);
        for (LCRelation relation : rotaterelations)
            if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                hitToRotated.add(relation.getFrom(), relation.getTo());
            }
        return hitToRotated;
    }
    
    public static List<TrackerHit> getStripHits(Track track, RelationalTable hitToStrips) {
        List<TrackerHit> hits = new ArrayList<TrackerHit>();
        for (TrackerHit hit : track.getTrackerHits()) {
            hits.addAll(hitToStrips.allFrom(hit));
        }
        return hits;
    }
}