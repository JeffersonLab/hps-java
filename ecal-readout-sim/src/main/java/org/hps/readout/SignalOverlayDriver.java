package org.hps.readout;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;

import org.lcsim.event.EventHeader;
import org.lcsim.event.EventHeader.LCMetaData;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.lcio.LCIOReader;
import org.lcsim.util.Driver;

/**
 * Overlay signal event collections every N events, applying optional
 * filters to reject unwanted events.
 */
public class SignalOverlayDriver extends Driver {

    private static Logger LOGGER =
            Logger.getLogger(SignalOverlayDriver.class.getPackage().getName());
    private Queue<String> signalFileNames = new LinkedList<String>();
    private int eventSpacing = 250;
    private int sequence = 0;
    private int nRejected = 0;
    private int nSignal = 0;
    private String ecalHitCollection = "EcalHits";
    private String hodoHitCollection = "HodoscopeHits";

    LCIOReader reader = null;

    interface EventFilter {
        public boolean accept(EventHeader event);
    }

    class EcalEnergyFilter implements EventFilter {

        double eCut = 0;

        public EcalEnergyFilter(double eCut) {
            this.eCut = eCut;
        }

        public boolean accept(EventHeader event) {
            List<SimCalorimeterHit> ecalHits = event.get(SimCalorimeterHit.class,
                    SignalOverlayDriver.this.ecalHitCollection);
            double totalE = 0;
            for (SimCalorimeterHit hit : ecalHits) {
                totalE += hit.getRawEnergy();
            }
            LOGGER.fine("ECAL totalE = " + totalE);
            return totalE > eCut;
        }
    }

    class HodoHitFilter implements EventFilter {

        int nHits = 0;

        public HodoHitFilter(int nHits) {
            this.nHits = nHits;
        }

        public boolean accept(EventHeader event) {
            List<SimTrackerHit> hodoHits = event.get(SimTrackerHit.class,
                    SignalOverlayDriver.this.hodoHitCollection);
            return hodoHits.size() >= nHits;
        }
    }

    private Set<EventFilter> eventFilters = new HashSet<EventFilter>();

    class EndOfDataException extends Exception {
        private static final long serialVersionUID = 1L;
    }

    public void setMinHodoHits(int nHits) {
        eventFilters.add(new HodoHitFilter(nHits));
    }

    public void setHodoHitCollection(String hodoHitCollection) {
        this.hodoHitCollection = hodoHitCollection;
    }

    public void setEcalEnergyCut(double eCut) {
        eventFilters.add(new EcalEnergyFilter(eCut));
    }

    public void setEcalHitCollection(String ecalHitCollection) {
        this.ecalHitCollection = ecalHitCollection;
    }

    public void setSignalFile(String signalFileName) {
        this.signalFileNames.add(signalFileName);
    }

    public void setSignalFileList(String signalFileList) {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(signalFileList));
            String line = reader.readLine();
            while (line != null) {
                line = reader.readLine();
                signalFileNames.add(line);
            }
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setEventSpacing(int eventSpacing) {
        if (eventSpacing <= 0) {
            throw new IllegalArgumentException("Bad value for event spacing: " + eventSpacing);
        }
        this.eventSpacing = eventSpacing;
    }

    @Override
    protected void startOfData() {
        if (this.signalFileNames.size() == 0) {
            throw new RuntimeException("No signal files were provided!");
        }
        String filePath = this.signalFileNames.remove();
        try {
            LOGGER.info("Opening signal file: " + filePath);
            File file = new File(filePath);
            if (!file.exists()) {
                throw new RuntimeException("File does not exist: " + file.getName());
            }
            reader = new LCIOReader(file);
        } catch (IOException e) {
            LOGGER.severe("Failed to read from: " + filePath);
            throw new RuntimeException(e);
        }
    }

    private void openNextFile() throws EndOfDataException {
        try {
            LOGGER.info("Closing previous reader...");
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (signalFileNames.size() == 0) {
            throw new EndOfDataException();
        }
        String path = signalFileNames.remove();
        LOGGER.info("Opening next file: " + path);
        try {
            reader = new LCIOReader(new File(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private EventHeader readNextEvent() throws EndOfDataException {
        EventHeader event = null;
        try {
            LOGGER.fine("Reading next event...");
            event = this.reader.read();
        } catch (EOFException eof) {
            openNextFile();
            try {
                LOGGER.fine("Reading first event in new file...");
                event = this.reader.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            if (event == null) {
                throw new RuntimeException("Failed to read next event.");
            }
        }
        return event;
    }

    private boolean accept(EventHeader event) {
        for (EventFilter filter : eventFilters) {
            if (!filter.accept(event)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected void process(EventHeader event) {
        LOGGER.fine("Processing background event: " + event.getEventNumber());

        // Overlay signal event
        if (this.sequence % this.eventSpacing == 0) {

            // Find the next acceptable signal event
            EventHeader signalEvent = null;
            try {
                signalEvent = this.readNextEvent();
                while (true) {
                    if (accept(signalEvent)) break;
                    LOGGER.fine("Rejected signal event: " + signalEvent.getEventNumber());
                    nRejected += 1;
                    signalEvent = this.readNextEvent();
                }
            } catch (EndOfDataException e) {
                throw new RuntimeException("Ran out of signal events!", e);
            }
            LOGGER.fine("Read signal event: " + signalEvent.getEventNumber());

            // Overlay signal collections onto background event
            Set<List> lists = signalEvent.getLists();
            for (List list : lists) {
                LCMetaData meta = signalEvent.getMetaData(list);
                String collName = meta.getName();
                if (!event.hasItem(collName)) {
                    event.put(collName, list, meta.getType(), meta.getFlags());
                } else {
                    event.get(meta.getClass(), collName).addAll(list);
                }
                LOGGER.finer("Added " + list.size() + " objects to collection: " + collName);
            }

            ++nSignal;
        }

        ++this.sequence;
    }

    protected void endOfData() {
        System.out.println("End event sequence: " + this.sequence);
        System.out.println("Signal events rejected: " + nRejected);
        System.out.println("Signal events overlaid: " + nSignal);
    }
}

