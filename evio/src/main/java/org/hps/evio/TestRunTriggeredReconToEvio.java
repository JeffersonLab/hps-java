package org.hps.evio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.hps.conditions.DatabaseConditionsManager;
import org.hps.conditions.deprecated.CalibrationDriver;
import org.hps.readout.ecal.ReadoutTimestamp;
import org.hps.readout.ecal.TriggerableDriver;
import org.jlab.coda.jevio.DataType;
import org.jlab.coda.jevio.EventBuilder;
import org.jlab.coda.jevio.EventWriter;
import org.jlab.coda.jevio.EvioBank;
import org.jlab.coda.jevio.EvioException;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;

/**
 * This class takes raw data generated from MC and converts it to EVIO. The goal
 * is to make this look like data which will come off the actual ET ring during
 * the test run.
 *
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class TestRunTriggeredReconToEvio extends TriggerableDriver {

    private EventWriter writer;
    private String rawCalorimeterHitCollectionName = "EcalReadoutHits";
    private String evioOutputFile = "TestRunData.evio";
    private Queue<QueuedEtEvent> builderQueue = null;
    private int eventsWritten = 0;
    private int eventNum = 0;
    private ECalHitWriter ecalWriter = null;
    private SVTHitWriter svtWriter = null;
    private TriggerDataWriter triggerWriter = null;
    private List<HitWriter> writers = null;
    private int ecalMode = EventConstants.ECAL_PULSE_INTEGRAL_MODE;
    
    Detector detector;

    public TestRunTriggeredReconToEvio() {
        setTriggerDelay(0);
    }
    
    @Override
    public void detectorChanged(Detector detector) {    	
    	//ecalWriter.setDetector(detector);
        if(detector == null) System.out.println("detectorChanged, Detector == null");
        else System.out.println("detectorChanged, Detector != null");
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

    public void setEvioOutputFile(String evioOutputFile) {
        this.evioOutputFile = evioOutputFile;
    }

    public void setRawCalorimeterHitCollectionName(String rawCalorimeterHitCollectionName) {
        this.rawCalorimeterHitCollectionName = rawCalorimeterHitCollectionName;
        if (ecalWriter != null) {
            ecalWriter.setHitCollectionName(rawCalorimeterHitCollectionName);
        }
    }

    @Override
    protected void startOfData() {
        super.startOfData();
        try {
            writer = new EventWriter(evioOutputFile);
        } catch (EvioException e) {
            throw new RuntimeException(e);
        }

        writePrestartEvent();
        this.detector = DatabaseConditionsManager.getInstance().getDetectorObject();

        writers = new ArrayList<HitWriter>();

        ecalWriter = new ECalHitWriter();
        if(detector == null) System.out.println("Detector == null");
        else System.out.println("Detector != null");
        //ecalWriter.setDetector(detector);
        ecalWriter.setMode(ecalMode);
        ecalWriter.setHitCollectionName(rawCalorimeterHitCollectionName);
        ecalWriter.setDetector(detector);
        writers.add(ecalWriter);

        svtWriter = new SVTHitWriter();
        writers.add(svtWriter);

        triggerWriter = new TriggerDataWriter();
        writers.add(triggerWriter);

        builderQueue = new LinkedList<QueuedEtEvent>();
    }

    @Override
    protected void endOfData() {
        System.out.println(this.getClass().getSimpleName() + " - wrote " + eventsWritten + " EVIO events in job; " + builderQueue.size() + " incomplete events in queue.");
        writer.close();
    }

    @Override
    protected void process(EventHeader event) {
        checkTrigger(event);

        for (int i = 0; i < writers.size(); i++) {
            HitWriter evioWriter = writers.get(i);
            if (evioWriter.hasData(event)) {
                System.out.println(evioWriter.getClass().getSimpleName() + ": writing data, event " + event.getEventNumber());
                EventBuilder builder = null;

                for (QueuedEtEvent queuedEvent : builderQueue) {
                    if (!queuedEvent.getRead(i)) {
                        builder = queuedEvent.getBuilder();
                        queuedEvent.setRead(i);
                        break;
                    }
                }
                if (builder == null) {
                    throw new RuntimeException("no queued ET events waiting for an " + evioWriter.getClass().getSimpleName() + " bank");
                }
                // Write data.
                evioWriter.writeData(event, builder);
            }
        }

        while (!builderQueue.isEmpty() && builderQueue.peek().banksFilled()) {
            System.out.println("writing filled ET event, event " + event.getEventNumber());
            QueuedEtEvent queuedEvent = builderQueue.poll();
            // Write this EVIO event.
            writeEvioEvent(queuedEvent);
        }
    }

    private void writeEvioEvent(QueuedEtEvent event) {
        System.out.printf("Writing event %d with %d bytes\n", event.getEventNum(), event.getBuilder().getEvent().getTotalBytes());
        try {
            writer.writeEvent(event.getBuilder().getEvent());
            ++eventsWritten;
        } catch (EvioException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writePrestartEvent() {
        // Make a new EVIO event.
        EventBuilder builder = new EventBuilder(EventConstants.PRESTART_EVENT_TAG, DataType.UINT32, EventConstants.EVENT_BANK_NUM);
        int[] prestartData = new int[3];
        prestartData[0] = EventConstants.MC_TIME; //Unix time in seconds - this value for MC data
        prestartData[1] = CalibrationDriver.runNumber(); //run number
        prestartData[2] = 0; //run type

        try {
            builder.appendIntData(builder.getEvent(), prestartData);
        } catch (EvioException e) {
            throw new RuntimeException(e);
        }
        builder.setAllHeaderLengths();
        try {
            writer.writeEvent(builder.getEvent());
            ++eventsWritten;
        } catch (EvioException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void processTrigger(EventHeader event) {
        // Make a new EVIO event.
        EventBuilder builder = new EventBuilder(EventConstants.PHYSICS_EVENT_TAG, DataType.BANK, EventConstants.EVENT_BANK_NUM);
        builderQueue.add(new QueuedEtEvent(builder, writers.size(), eventNum));
        EvioBank eventIDBank = new EvioBank(EventConstants.EVENTID_BANK_TAG, DataType.UINT32, 0);
        int[] eventID = new int[3];
        eventID[0] = event.getEventNumber();
        eventID[1] = 1; //trigger type
        eventID[2] = 0; //status

        eventNum++;
        try {
            eventIDBank.appendIntData(eventID);
            builder.addChild(builder.getEvent(), eventIDBank);
        } catch (EvioException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getTimestampType() {
        return ReadoutTimestamp.SYSTEM_TRIGGERTIME;
    }

    private class QueuedEtEvent {

        private final EventBuilder builder;
        public boolean readSVT = false;
        public boolean readECal = false;
        private boolean[] readData = null;
        private final int eventNum;

        public QueuedEtEvent(EventBuilder builder, int numData, int eventNum) {
            this.builder = builder;
            readData = new boolean[numData];
            this.eventNum = eventNum;
        }

        public int getEventNum() {
            return eventNum;
        }

        public void setRead(int i) {
            readData[i] = true;
        }

        public boolean getRead(int i) {
            return readData[i];
        }

        public EventBuilder getBuilder() {
            return builder;
        }

        public boolean banksFilled() {
            for (boolean x : readData) {
                if (!x) {
                    return false;
                }
            }
            return true;
        }
    }
}
