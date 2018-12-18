package org.hps.analysis.MC;

import java.util.ArrayList;
import java.util.List;



//import org.hps.recon.particle.ReconParticleDriver;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
//import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
//import org.lcsim.event.Vertex;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.util.Driver;

/**
 * Driver to skim selected events from LCIO files based on 
 * track picking up the wrong hit in the first 2 layers
 * This driver can only be run on MC with full truth.
 *
 * @author Matt Solt
 *
 * @version $Id:
 */
public class IsolationRefit extends Driver{

    private final String unconstrainedV0CandidatesColName = "UnconstrainedV0Candidates";
    private final String trackColName = "GBLTracks";
    private final String badTrackColName = "GBLTracks_bad";
    //private final String simhitOutputColName = "SimTrackerHits_truth";
    private final String simhitOutputColName = "TrackerHits_truth";
    private final String trackOutputColName = "GBLTracks_out";
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
        
        System.out.println("New Event!");
        List<Track> allTracks = event.get(Track.class, trackColName);
        List<Track> badTracks = new ArrayList<Track>();
        List<SimTrackerHit> truthHits = new ArrayList<SimTrackerHit>();
        for(Track track:allTracks){
            MCFullDetectorTruth truthMatch = new MCFullDetectorTruth(event, track, bFieldMap, sensors, trackerSubdet);
            if(truthMatch.getMCParticle() == null){
                continue;
            }
            if((truthMatch.getPurity() < 1.0)){
                continue;
            }
            
            truthHits = truthMatch.getActiveHitListMCParticle();
            badTracks.add(track);
            //event.put(simhitOutputColName, truthHits, SimTrackerHit.class, 0);
            //event.put(badTrackColName, badTracks, Track.class, 0);
            break;
        }
        event.put(simhitOutputColName, truthHits, SimTrackerHit.class, 0);
        event.put(badTrackColName, badTracks, Track.class, 0);
            //event.put(badTrackColName, badTracks, Track.class, 0);
        /*List<ReconstructedParticle> unConstrainedV0List = event.get(ReconstructedParticle.class, unconstrainedV0CandidatesColName);

        for (ReconstructedParticle uncV0 : unConstrainedV0List) {

            ReconstructedParticle electron = uncV0.getParticles().get(ReconParticleDriver.ELECTRON);
            ReconstructedParticle positron = uncV0.getParticles().get(ReconParticleDriver.POSITRON);
            
            Track eleTrack = electron.getTracks().get(0);
            Track posTrack = positron.getTracks().get(0);
            
            MCFullDetectorTruth eleFullTruth = new MCFullDetectorTruth(event, eleTrack, bFieldMap, sensors, trackerSubdet);
            MCFullDetectorTruth posFullTruth = new MCFullDetectorTruth(event, posTrack, bFieldMap, sensors, trackerSubdet);
            
            if(eleFullTruth.getMCParticle() == null || posFullTruth.getMCParticle() == null){
                continue;
            }
            
            boolean eleGoodHits = false;
            boolean posGoodHits = false;
            
            if(eleFullTruth != null){
                boolean eleL1GoodHit = false;
                boolean eleL2GoodHit = false;
                if(eleFullTruth.getHitList(1) != null){
                    eleL1GoodHit = eleFullTruth.getHitList(1);
                }
                if(eleFullTruth.getHitList(2) != null){
                    eleL2GoodHit = eleFullTruth.getHitList(2);
                }
                eleGoodHits = eleL1GoodHit && eleL2GoodHit;
            }
            
            if(posFullTruth != null){
                boolean posL1GoodHit = false;
                boolean posL2GoodHit = false;
                if(posFullTruth.getHitList(1) != null){
                    posL1GoodHit = posFullTruth.getHitList(1);
                }
                if(posFullTruth.getHitList(2) != null){
                    posL2GoodHit = posFullTruth.getHitList(2);
                }
                posGoodHits = posL1GoodHit && posL2GoodHit;
            }
            
            boolean badEvent = !eleGoodHits || !posGoodHits;
            
            if(!badEvent){
                continue;
            }
            
            List<SimTrackerHit> eleHits = eleFullTruth.getActiveHitListMCParticle();
            List<SimTrackerHit> posHits = posFullTruth.getActiveHitListMCParticle();
            
            
        }*/
    }

    @Override
    protected void endOfData(){
        System.out.println("End of Data");
    }

}