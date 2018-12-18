package org.hps.analysis.MC;

import java.util.List;

import org.hps.recon.particle.ReconParticleDriver;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
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
public class BadHitLcioEventSkimmer extends Driver{

    private boolean skipEvent = true;
    private int _numberOfEventsWritten;
    private final String unconstrainedV0CandidatesColName = "UnconstrainedV0Candidates";
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
        skipEvent = true;
        
        List<ReconstructedParticle> unConstrainedV0List = event.get(ReconstructedParticle.class, unconstrainedV0CandidatesColName);

        for (ReconstructedParticle uncV0 : unConstrainedV0List) {

            ReconstructedParticle electron = uncV0.getParticles().get(ReconParticleDriver.ELECTRON);
            ReconstructedParticle positron = uncV0.getParticles().get(ReconParticleDriver.POSITRON);
            
            Track eleTrack = electron.getTracks().get(0);
            Track posTrack = positron.getTracks().get(0);
            
            MCFullDetectorTruth eleFullTruth = new MCFullDetectorTruth(event, eleTrack, bFieldMap, sensors, trackerSubdet);
            MCFullDetectorTruth posFullTruth = new MCFullDetectorTruth(event, posTrack, bFieldMap, sensors, trackerSubdet);
            
            
            boolean eleGoodHits = false;
            boolean posGoodHits = false;
            
            if(eleFullTruth.getMCParticle() == null || posFullTruth.getMCParticle() == null){
                continue;
            }

            if(eleFullTruth != null){
                boolean eleL1GoodHit = false;
                boolean eleL2GoodHit = false;
                boolean eleL3GoodHit = false;
                boolean eleL4GoodHit = false;
                if(eleFullTruth.getHitList(1) != null){
                    eleL1GoodHit = eleFullTruth.getHitList(1);
                }
                if(eleFullTruth.getHitList(2) != null){
                    eleL2GoodHit = eleFullTruth.getHitList(2);
                }
                if(eleFullTruth.getHitList(3) != null){
                    eleL3GoodHit = eleFullTruth.getHitList(3);
                }
                if(eleFullTruth.getHitList(4) != null){
                    eleL4GoodHit = eleFullTruth.getHitList(4);
                }
                eleGoodHits = eleL1GoodHit && eleL2GoodHit && eleL3GoodHit && eleL4GoodHit;
            }
            
            if(posFullTruth != null){
                boolean posL1GoodHit = false;
                boolean posL2GoodHit = false;
                boolean posL3GoodHit = false;
                boolean posL4GoodHit = false;
                if(posFullTruth.getHitList(1) != null){
                    posL1GoodHit = posFullTruth.getHitList(1);
                }
                if(posFullTruth.getHitList(2) != null){
                    posL2GoodHit = posFullTruth.getHitList(2);
                }
                if(posFullTruth.getHitList(3) != null){
                    posL3GoodHit = posFullTruth.getHitList(3);
                }
                if(posFullTruth.getHitList(4) != null){
                    posL4GoodHit = posFullTruth.getHitList(4);
                }
                posGoodHits = posL1GoodHit && posL2GoodHit && posL3GoodHit && posL4GoodHit;
            }
            
            boolean badEvent = !eleGoodHits || !posGoodHits;
            
            if(badEvent){
                skipEvent = false;
                break;
            }
        }
        
        if (skipEvent) {
            throw new Driver.NextEventException();
        } else {
            _numberOfEventsWritten++;
        }
    }

    @Override
    protected void endOfData(){
        System.out.println("Selected " + _numberOfEventsWritten + " events");
    }

}