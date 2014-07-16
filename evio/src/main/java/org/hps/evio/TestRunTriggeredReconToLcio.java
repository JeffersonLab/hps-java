package org.hps.evio;

import hep.physics.event.generator.MCEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.hps.conditions.deprecated.CalibrationDriver;
import org.hps.conditions.deprecated.QuietBaseLCSimEvent;
import org.hps.readout.ecal.ClockSingleton;
import org.hps.readout.ecal.ReadoutTimestamp;
import org.hps.readout.ecal.TriggerDriver;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.lcio.LCIOWriter;
import org.lcsim.util.Driver;

/**
 * This class takes raw data generated from MC and converts it to EVIO. The goal
 * is to make this look like data which will come off the actual ET ring during
 * the test run.
 *
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class TestRunTriggeredReconToLcio extends Driver {

    String rawCalorimeterHitCollectionName = "EcalReadoutHits";
    String outputFile = "TestRunData.slcio";
    private int eventsWritten = 0;
    private int eventNum = 0;
    //interval for trigger candidates (tridents, A'), if used
    private int triggerSpacing = 250;
    private boolean rejectBackground = false;
//    HPSEcalConditions ecalIDConverter = null;
    ECalHitWriter ecalWriter = null;
    SVTHitWriter svtWriter = null;
    TriggerDataWriter triggerWriter = null;
    List<HitWriter> writers = null;
    LCIOWriter lcioWriter = null;
    Queue<EventHeader> events = null;
    private int ecalMode = EventConstants.ECAL_PULSE_INTEGRAL_MODE;
    List<MCParticle> mcParticles = null;
    List<SimTrackerHit> trackerHits = null;
    List<SimCalorimeterHit> ecalHits = null;
    List<SimTrackerHit> ecalScoringPlaneHits = null;
    //MC collections from the last 500n'th event (trident or preselected trigger event)
    List<MCParticle> triggerMCParticles = null;
    List<SimTrackerHit> triggerTrackerHits = null;
    List<SimCalorimeterHit> triggerECalHits = null;
    List<SimTrackerHit> triggerECalScoringPlaneHits = null;
    static final String ecalCollectionName = "EcalHits";
    static final String trackerCollectionName = "TrackerHits";
    private String relationCollectionName = "SVTTrueHitRelations";
    String ecalScoringPlaneHitsCollectionName = "TrackerHitsECal";
    
    
    public TestRunTriggeredReconToLcio() {
    }

    public void setEcalMode(int ecalMode) {
        this.ecalMode = ecalMode;
        if (ecalMode != EventConstants.ECAL_WINDOW_MODE && ecalMode != EventConstants.ECAL_PULSE_MODE && ecalMode != EventConstants.ECAL_PULSE_INTEGRAL_MODE) {
            throw new IllegalArgumentException("invalid mode " + ecalMode);
        }
        if (ecalWriter != null) {
            ecalWriter.setMode(ecalMode);
        }
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public void setTriggerSpacing(int triggerSpacing) {
        this.triggerSpacing = triggerSpacing;
    }

    public void setRejectBackground(boolean rejectBackground) {
        this.rejectBackground = rejectBackground;
    }

    public void setRawCalorimeterHitCollectionName(String rawCalorimeterHitCollectionName) {
        this.rawCalorimeterHitCollectionName = rawCalorimeterHitCollectionName;
        if (ecalWriter != null) {
            ecalWriter.setHitCollectionName(rawCalorimeterHitCollectionName);
        }
    }

    @Override
    protected void startOfData() {
        writers = new ArrayList<HitWriter>();

        ecalWriter = new ECalHitWriter();
        ecalWriter.setMode(ecalMode);
        ecalWriter.setHitCollectionName(rawCalorimeterHitCollectionName);
        writers.add(ecalWriter);

        svtWriter = new SVTHitWriter();
        writers.add(svtWriter);

        triggerWriter = new TriggerDataWriter();
        writers.add(triggerWriter);

        try {
            lcioWriter = new LCIOWriter(new File(outputFile));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        events = new LinkedList<EventHeader>();
    }

    @Override
    protected void endOfData() {
        System.out.println(this.getClass().getSimpleName() + " - wrote " + eventsWritten + " events in job; " + events.size() + " incomplete events in queue.");
        try {
            lcioWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void process(EventHeader event) {
        if (event.hasCollection(SimCalorimeterHit.class, ecalCollectionName) && !event.getSimCalorimeterHits(ecalCollectionName).isEmpty()) {
            mcParticles = event.getMCParticles();
            ecalHits = event.getSimCalorimeterHits(ecalCollectionName);
            trackerHits = event.getSimTrackerHits(trackerCollectionName);
           	if(event.hasCollection(SimTrackerHit.class, ecalScoringPlaneHitsCollectionName)){
           		ecalScoringPlaneHits = event.get(SimTrackerHit.class, ecalScoringPlaneHitsCollectionName);
           		System.out.println("Number of Ecal scoring plane hits: " + ecalScoringPlaneHits.size());
           	}
        }
        if (ClockSingleton.getClock() % triggerSpacing == 0) {
            if (event.hasCollection(MCParticle.class)) {
                triggerMCParticles = event.getMCParticles();
                triggerECalHits = event.getSimCalorimeterHits(ecalCollectionName);
                triggerTrackerHits = event.getSimTrackerHits(trackerCollectionName);
                if(event.hasCollection(SimTrackerHit.class, ecalScoringPlaneHitsCollectionName)){
                	triggerECalScoringPlaneHits = event.get(SimTrackerHit.class, ecalScoringPlaneHitsCollectionName);
                	System.out.println("Number of triggered Ecal scoring plane hits: " + ecalScoringPlaneHits.size());
                }
            } else {
                triggerMCParticles = null;
                triggerECalHits = null;
                triggerTrackerHits = null;
                triggerECalScoringPlaneHits = null; 
            }
        }


        if (TriggerDriver.triggerBit()) {
            EventHeader lcsimEvent = new QuietBaseLCSimEvent(CalibrationDriver.runNumber(), event.getEventNumber(), event.getDetectorName());
            events.add(lcsimEvent);
            System.out.println("Creating LCIO event " + eventNum);
            if (triggerMCParticles == null || triggerMCParticles.isEmpty()) {
                lcsimEvent.put(MCEvent.MC_PARTICLES, mcParticles);
                lcsimEvent.put(ecalCollectionName, ecalHits, SimCalorimeterHit.class, 0xe0000000);
                lcsimEvent.put(trackerCollectionName, trackerHits, SimTrackerHit.class, 0xc0000000);
                System.out.println("Adding " +  mcParticles.size() + " MCParticles, " + ecalHits.size() + " SimCalorimeterHits, " + trackerHits.size() + " SimTrackerHits");
                if(ecalScoringPlaneHits != null){
                	lcsimEvent.put(ecalScoringPlaneHitsCollectionName, ecalScoringPlaneHits, SimTrackerHit.class, 0);
                	System.out.println("Adding " + ecalScoringPlaneHits.size() + " ECalTrackerHits");
                }
            } else {
                lcsimEvent.put(MCEvent.MC_PARTICLES, triggerMCParticles);
                lcsimEvent.put(ecalCollectionName, triggerECalHits, SimCalorimeterHit.class, 0xe0000000);
                lcsimEvent.put(trackerCollectionName, triggerTrackerHits, SimTrackerHit.class, 0xc0000000);
                System.out.println("Adding " +  triggerMCParticles.size() + " MCParticles, " + triggerECalHits.size() + " SimCalorimeterHits, " + triggerTrackerHits.size() + " SimTrackerHits");
                if(triggerECalScoringPlaneHits != null){
                	lcsimEvent.put(ecalScoringPlaneHitsCollectionName, triggerECalScoringPlaneHits, SimTrackerHit.class, 0);
                	System.out.println("Adding " + triggerECalScoringPlaneHits.size() + " ECalTrackerHits");
                }
            }
            lcsimEvent.put(ReadoutTimestamp.collectionName, event.get(ReadoutTimestamp.class, ReadoutTimestamp.collectionName));
            ++eventNum;
        }

        writerLoop:
        for (HitWriter hitWriter : writers) {
            if (hitWriter.hasData(event)) {
                System.out.println(hitWriter.getClass().getSimpleName() + ": writing data, event " + event.getEventNumber());

                for (EventHeader queuedEvent : events) {
                    if (!hitWriter.hasData(queuedEvent)) {
                        // Write data.
                        hitWriter.writeData(event, queuedEvent);
                        continue writerLoop;
                    }
                }

                throw new RuntimeException("no queued events waiting for an " + hitWriter.getClass().getSimpleName() + " bank");
            }
        }

        eventLoop:
        while (!events.isEmpty()) {
            EventHeader queuedEvent = events.peek();
            for (HitWriter hitWriter : writers) {
                if (!hitWriter.hasData(queuedEvent)) {
                    break eventLoop;
                }
            }
            events.poll();

            boolean writeThisEvent = true;
            if (rejectBackground && queuedEvent.hasCollection(LCRelation.class, relationCollectionName)) {
                writeThisEvent = false;
                List<LCRelation> trueHitRelations = queuedEvent.get(LCRelation.class, relationCollectionName);
                List<SimTrackerHit> trueHits = queuedEvent.getSimTrackerHits(trackerCollectionName);
                for (LCRelation relation : trueHitRelations) {
                    if (trueHits.contains((SimTrackerHit) relation.getTo())) {
                        writeThisEvent = true;
                        break;
                    }
                }
            }

            if (writeThisEvent) {
                // Write this event.
                System.out.println("writing filled LCIO event, event " + queuedEvent.getEventNumber());
                try {
                    lcioWriter.write(queuedEvent);
                    ++eventsWritten;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                System.out.println("rejecting filled LCIO event, event " + queuedEvent.getEventNumber() + " contains no SVT hits from truth particles");
            }
        }
    }
}