package org.hps.recon.tracking.gbl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.Pair;
import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.MultipleScattering;
import org.hps.recon.tracking.TrackUtils;
import org.hps.record.StandardCuts;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseLCRelation;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;

/**
 * A Driver which refits tracks using GBL. Does not require GBL collections to
 * be present in the event.
 */
public class GBLRefitterDriver extends Driver {

    private String inputCollectionName = "MatchedTracks";
    private String outputCollectionName = "GBLTracks";
    private String trackRelationCollectionName = "MatchedToGBLTrackRelations";
    private String helicalTrackHitRelationsCollectionName = "HelicalTrackHitRelations";
    private String rotatedHelicalTrackHitRelationsCollectionName = "RotatedHelicalTrackHitRelations";
    private String rawHitCollectionName = "SVTRawTrackerHits";

    private double bfield;
    private final MultipleScattering _scattering = new MultipleScattering(new MaterialSupervisor());
    private boolean storeTrackStates = false;
    private StandardCuts cuts = new StandardCuts();

    private MilleBinary mille;
    private String milleBinaryFileName = MilleBinary.DEFAULT_OUTPUT_FILE_NAME;
    private boolean writeMilleBinary = false;
    private double writeMilleChi2Cut = 20;
    private boolean includeNoHitScatters = true;
    private boolean enableStandardCuts = false;
    
    //Setting 0 is a single refit, 1 refit twice and so on..
    private int gblRefitIterations = 5; 

    public void setIncludeNoHitScatters(boolean val) {
        includeNoHitScatters = val;
    }
    
    public void setGblRefitIterations(int val) {
        gblRefitIterations = val;
    }

    public void setWriteMilleChi2Cut(int input) {
        writeMilleChi2Cut = input;
    }

    public void setMilleBinaryFileName(String filename) {
        milleBinaryFileName = filename;
    }

    public void setWriteMilleBinary(boolean writeMillepedeFile) {
        writeMilleBinary = writeMillepedeFile;
    }

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

    public void setRotatedHelicalTrackHitRelationsCollectionName(String rotatedHelicalTrackHitRelationsCollectionName) {
        this.rotatedHelicalTrackHitRelationsCollectionName = rotatedHelicalTrackHitRelationsCollectionName;
    }

    public void setRawHitCollectionName(String rawHitCollectionName) {
        this.rawHitCollectionName = rawHitCollectionName;
    }

    public void setMaxTrackChisq(int nhits, double input) {
        cuts.setMaxTrackChisq(nhits, input);
    }

    public void setMaxTrackChisq4hits(double input) {
        cuts.setMaxTrackChisq(4, input);
    }

    public void setMaxTrackChisq5hits(double input) {
        cuts.setMaxTrackChisq(5, input);
    }

    public void setMaxTrackChisq6hits(double input) {
        cuts.setMaxTrackChisq(6, input);
    }

    public void setMaxTrackChisqProb(double input) {
        cuts.changeChisqTrackProb(input);
    }

    public void setEnableStandardCuts(boolean val) {
        System.out.println("GBLRefitterDriver::WARNING:Enabling standardCuts!");
        enableStandardCuts = val;
    }
    

    @Override
    protected void startOfData() {
        if (writeMilleBinary)
            mille = new MilleBinary(milleBinaryFileName);
    }

    @Override
    protected void endOfData() {
        if (writeMilleBinary)
            mille.close();
    }

    @Override
    protected void detectorChanged(Detector detector) {
        bfield = Math.abs(TrackUtils.getBField(detector).magnitude());
        _scattering.getMaterialManager().buildModel(detector);
        _scattering.setBField(bfield); // only absolute of B is needed as it's used for momentum calculation only
    }

    @Override
    protected void process(EventHeader event) {
        if (!event.hasCollection(Track.class, inputCollectionName))
            return;

        setupSensors(event);
        List<Track> tracks = event.get(Track.class, inputCollectionName);
        //       System.out.println("GBLRefitterDriver::process number of tracks = "+tracks.size());
        //RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        //RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);
        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event,helicalTrackHitRelationsCollectionName);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event,rotatedHelicalTrackHitRelationsCollectionName);

        List<Track> refittedTracks = new ArrayList<Track>();
        List<LCRelation> trackRelations = new ArrayList<LCRelation>();

        List<GBLKinkData> kinkDataCollection = new ArrayList<GBLKinkData>();
        List<LCRelation> kinkDataRelations = new ArrayList<LCRelation>();
        
        //Map<Track, Track> inputToRefitted = new HashMap<Track, Track>();
        for (Track track : tracks) {
            List<TrackerHit> temp = TrackUtils.getStripHits(track, hitToStrips, hitToRotated);
            if (temp.size() == 0)
                //               System.out.println("GBLRefitterDriver::process  did not find any strip hits on this track???");
                continue;

            Pair<Pair<Track, GBLKinkData>, FittedGblTrajectory> newTrackTraj = MakeGblTracks.refitTrackWithTraj(TrackUtils.getHTF(track), temp, track.getTrackerHits(), gblRefitIterations, track.getType(), _scattering, bfield, storeTrackStates,includeNoHitScatters);
            if (newTrackTraj == null) {
                System.out.println("GBLRefitterDriver::process() -- Aborted refit of track -- null pointer for newTrackTraj returned from MakeGblTracks.refitTrackWithTraj .");
                continue;
            }
            Pair<Track, GBLKinkData> newTrack = newTrackTraj.getFirst();
            if (newTrack == null)
                continue;
            Track gblTrk = newTrack.getFirst();
            if (writeMilleBinary)
                if (gblTrk.getChi2() < writeMilleChi2Cut)
                    newTrackTraj.getSecond().get_traj().milleOut(mille);

            //System.out.printf("gblTrkNDF %d  gblTrkChi2 %f  getMaxTrackChisq5 %f getMaxTrackChisq6 %f \n", gblTrk.getNDF(), gblTrk.getChi2(), cuts.getMaxTrackChisq(5), cuts.getMaxTrackChisq(6));
            if (enableStandardCuts && (gblTrk.getChi2() > cuts.getMaxTrackChisq(gblTrk.getTrackerHits().size())))
                continue;
            refittedTracks.add(gblTrk);
            trackRelations.add(new BaseLCRelation(track, gblTrk));
            //PF :: unused
            //inputToRefitted.put(track, gblTrk);
            kinkDataCollection.add(newTrack.getSecond());
            kinkDataRelations.add(new BaseLCRelation(newTrack.getSecond(), gblTrk));
        }

        // Put the tracks back into the event and exit
        int flag = 1 << LCIOConstants.TRBIT_HITS;
        event.put(outputCollectionName, refittedTracks, Track.class, flag);
        event.put(trackRelationCollectionName, trackRelations, LCRelation.class, 0);
        event.put(GBLKinkData.DATA_COLLECTION, kinkDataCollection, GBLKinkData.class, 0);
        event.put(GBLKinkData.DATA_RELATION_COLLECTION, kinkDataRelations, LCRelation.class, 0);
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
}
