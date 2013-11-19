package org.lcsim.hps.evio;

import hep.physics.event.generator.MCEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.hps.conditions.CalibrationDriver;
import org.lcsim.hps.conditions.QuietBaseLCSimEvent;
import org.lcsim.hps.readout.ecal.ReadoutTimestamp;
import org.lcsim.hps.readout.ecal.TriggerDriver;
import org.lcsim.hps.util.ClockSingleton;
import org.lcsim.util.Driver;
import org.lcsim.lcio.LCIOWriter;

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
    //MC collections from the last 500n'th event (trident or preselected trigger event)
    List<MCParticle> mcParticles500 = null;
    List<SimTrackerHit> trackerHits500 = null;
    List<SimCalorimeterHit> ecalHits500 = null;
    static final String ecalCollectionName = "EcalHits";
    static final String trackerCollectionName = "TrackerHits";

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
        }
        if (ClockSingleton.getClock() % 500 == 0) {
            if(event.hasCollection(MCParticle.class)) { 
                mcParticles500 = event.getMCParticles();            
                ecalHits500 = event.getSimCalorimeterHits(ecalCollectionName);
                trackerHits500 = event.getSimTrackerHits(trackerCollectionName);
            } else {
                mcParticles500 = null;
                ecalHits500 = null;
                trackerHits500 = null;
            }
        }


        if (TriggerDriver.triggerBit()) {
            EventHeader lcsimEvent = new QuietBaseLCSimEvent(CalibrationDriver.runNumber(), event.getEventNumber(), event.getDetectorName());
            events.add(lcsimEvent);
            System.out.println("Creating LCIO event " + eventNum);
            if (mcParticles500 == null || mcParticles500.isEmpty()) {
                lcsimEvent.put(MCEvent.MC_PARTICLES, mcParticles);
                lcsimEvent.put(ecalCollectionName, ecalHits, SimCalorimeterHit.class, 0xe0000000);
                lcsimEvent.put(trackerCollectionName, trackerHits, SimTrackerHit.class, 0xc0000000);
                System.out.println("Adding " + mcParticles.size() + " MCParticles, " + ecalHits.size() + " SimCalorimeterHits, " + trackerHits.size() + " SimTrackerHits");
            } else {
                lcsimEvent.put(MCEvent.MC_PARTICLES, mcParticles500);
                lcsimEvent.put(ecalCollectionName, ecalHits500, SimCalorimeterHit.class, 0xe0000000);
                lcsimEvent.put(trackerCollectionName, trackerHits500, SimTrackerHit.class, 0xc0000000);
                System.out.println("Adding " + mcParticles500.size() + " MCParticles, " + ecalHits500.size() + " SimCalorimeterHits, " + trackerHits500.size() + " SimTrackerHits");
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
            System.out.println("writing filled LCIO event, event " + queuedEvent.getEventNumber());
            events.poll();
            // Write this event.
            try {
                lcioWriter.write(queuedEvent);
                ++eventsWritten;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}