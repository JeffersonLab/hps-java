package org.hps.analysis.MC;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.base.BaseLCRelation;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.util.Driver;

/**
 * Driver to skim selected events from LCIO files based on 
 * track picking up the wrong hit
 * This driver can only be run on MC readout with full truth.
 *
 * @author Matt Solt
 *
 * @version $Id:
 */
public class IdentifyBadTracksDriver extends Driver{

    //Collection name
    //TODO Make these names configurable by steering file
    private final String trackColName = "GBLTracks";
    private final String badTrackColName = "GBLTracks_bad";
    private final String simhitOutputColName = "TrackerHits_truth";
    private final String trackBadToTruthMatchRelationsOutputColName = "TrackBadToMCParticleRelations";
    private final String trackToTruthMatchRelationsOutputColName = "TrackToMCParticleRelations";

  //List of Sensors
    private List<HpsSiSensor> sensors = null;
    FieldMap bFieldMap = null;
    private static final String SUBDETECTOR_NAME = "Tracker";
    protected static Subdetector trackerSubdet;

    public void detectorChanged(Detector detector){
        
        bFieldMap = detector.getFieldMap();
        
        // Get the HpsSiSensor objects from the tracker detector element
        sensors = detector.getSubdetector(SUBDETECTOR_NAME)
                          .getDetectorElement().findDescendants(HpsSiSensor.class);
        
        trackerSubdet = detector.getSubdetector(SUBDETECTOR_NAME);
    }

    @Override
    protected void process(EventHeader event){
        List<Track> allTracks = event.get(Track.class, trackColName);
        List<Track> badTracks = new ArrayList<Track>();
        List<SimTrackerHit> truthHits = new ArrayList<SimTrackerHit>();
        List<MCFullDetectorTruth> truthMatchWithBadTrack = new ArrayList<MCFullDetectorTruth>();
        List<LCRelation> trackBadToTruthMatchRelations = new ArrayList<LCRelation>();
        List<LCRelation> trackToTruthMatchRelations = new ArrayList<LCRelation>();
        
        //Loop over all tracks
        for(Track track:allTracks){
            //Match the track to a MC truth particle
            MCFullDetectorTruth truthMatch = new MCFullDetectorTruth(event, track, bFieldMap, sensors, trackerSubdet);
            if(truthMatch.getMCParticle() == null){
                continue;
            }
            //Add the truth match to track LC relation
            trackToTruthMatchRelations.add(new BaseLCRelation(track, truthMatch.getMCParticle()));
            
            //Check to see if the truth match contains a hit not associated with the truth particle
            //(i.e. the purity is less than 1.0)
            if((truthMatch.getPurity() == 1.0)){
                continue;
            }
            truthHits = truthMatch.getActiveHitListMCParticle();
            badTracks.add(track);
            truthMatchWithBadTrack.add(truthMatch);
            trackBadToTruthMatchRelations.add(new BaseLCRelation(track, truthMatch.getMCParticle()));
        }

        //Fill the collections
        event.put(simhitOutputColName, truthHits, SimTrackerHit.class, 0);
        event.put(badTrackColName, badTracks, Track.class, 0);
        event.put(trackBadToTruthMatchRelationsOutputColName, trackBadToTruthMatchRelations, LCRelation.class, 0);
        event.put(trackToTruthMatchRelationsOutputColName, trackToTruthMatchRelations, LCRelation.class, 0);
    }
}