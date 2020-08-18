package org.hps.digi;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;

import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.base.BaseRawTrackerHit;
import org.lcsim.lcio.LCIOReader;
import org.lcsim.util.Driver;

/**
 * <p>
 * Driver to overlay from another event stream onto an EventHeader,
 * adding ADC traces from RawTrackerHit objects with the same IDs,
 * and then replacing the collection that exists.
 * </p>
 *
 * <p>
 * Example collections from 2019 data...
 * </p>
 *
 * <pre>
 * Collection Name                Collection Type
 * --------------------------------------------------
 * HodoReadoutHits                RawTrackerHit
 * EcalReadoutHits                RawTrackerHit
 * FADCGenericHits                LCGenericObject
 * VTPBank                        LCGenericObject
 * SVTRawTrackerHits              RawTrackerHit
 * TSBank                         LCGenericObject
 * TriggerBank                    LCGenericObject
 * --------------------------------------------------
 * </pre>
 *
 * Only RawTrackerHit collections are handled for now.
 *
 * Hit times are hard-coded to zero (they are zero in the data as well).
 *
 * @author Jeremy McCormick, SLAC
 */
public class DataOverlayDriver extends Driver {

    private static Logger LOGGER = Logger.getLogger(DataOverlayDriver.class.getPackage().getName());

    private final Queue<String> inputFilePaths = new LinkedList<String>();
    LCIOReader reader = null;

    /**
     * Collections names to read.
     */
    static List<String> COLLECTION_NAMES =
            Arrays.asList("EcalReadoutHits", "HodoReadoutHits", "SVTRawTrackerHits");

    /**
     * Thrown when events are exhausted from the overlay stream.
     */
    @SuppressWarnings("serial")
    class EndOfDataException extends Exception {
    }

    public void setInputFile(String inputFile) {
        this.inputFilePaths.add(inputFile);
    }

    public void setInputFiles(List<String> inputFiles) {
        this.inputFilePaths.addAll(inputFiles);
    }

    public void setInputFileList(String inputFileListPath) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(inputFileListPath));
            String line = reader.readLine();
            while (line != null) {
                line = reader.readLine().trim();
                if (line.length() > 0) {
                    inputFilePaths.add(line);
                }
            }
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private EventHeader readNextEvent() throws EndOfDataException {
        EventHeader event = null;
        try {
            LOGGER.info("Reading next event...");
            event = this.reader.read();
        } catch (EOFException eof) {
            openNextFile();
            try {
                LOGGER.info("Reading first event in new file...");
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

    @Override
    protected void process(EventHeader event) {
        try {
            LOGGER.fine("Processing event: " + event.getEventNumber());
            EventHeader overlayEvent = this.readNextEvent();
            LOGGER.fine("Read overlay event: " + event.getEventNumber());
            this.overlayEvent(overlayEvent, event);
        } catch (EndOfDataException e) {
            throw new RuntimeException("Ran out of overlay events!");
        }
    }

    private void overlayEvent(EventHeader overlay, EventHeader event) {
        LOGGER.fine("Overlaying event " + overlay.getEventNumber() + " onto primary event " + event.getEventNumber());
        for (String collName : COLLECTION_NAMES) {
            List<RawTrackerHit> overlayHits = overlay.get(RawTrackerHit.class, collName);
            LOGGER.finer("Overlay " + collName + ": " + overlayHits.size());
            List<RawTrackerHit> hits = event.get(RawTrackerHit.class, collName);
            LOGGER.finer("Primary " + collName + ": " + hits.size());
            List<RawTrackerHit> newHits = this.mergeRawTrackerHitCollections(overlayHits, hits);
            LOGGER.finer("Merged " + collName + ": " + newHits.size() + '\n');
            event.put(collName, newHits, RawTrackerHit.class, event.getMetaData(hits).getFlags());
        }
    }

    static private void addHits(List<RawTrackerHit> hits, Map<Long, List<RawTrackerHit>> map) {
        for (RawTrackerHit hit : hits) {
            Long id = hit.getCellID();
            if (!map.containsKey(id)) {
                map.put(id, new ArrayList<RawTrackerHit>());
            }
            map.get(id).add(hit);
        }
    }

    private List<RawTrackerHit> mergeRawTrackerHitCollections(List<RawTrackerHit> coll1, List<RawTrackerHit> coll2) {
        List<RawTrackerHit> newColl = new ArrayList<RawTrackerHit>();
        Map<Long, List<RawTrackerHit>> hitMap = new HashMap<Long, List<RawTrackerHit>>();
        addHits(coll1, hitMap);
        addHits(coll2, hitMap);
        for (Long id : hitMap.keySet()) {
            List<RawTrackerHit> hits = hitMap.get(id);
            if (hits.size() == 1) {
                newColl.add(hits.get(0));
                LOGGER.info("Added single hit: " + String.format("0x%08X", hits.get(0).getCellID()));
            } else {
                RawTrackerHit combinedHit = combineHits(hits);
                newColl.add(combinedHit);
                LOGGER.info("Added " + hits.size() + " hits to: " + String.format("0x%08X", combinedHit.getCellID()));
            }
        }
        return newColl;
    }

    private RawTrackerHit combineHits(List<RawTrackerHit> hits) {
        short[] adcValues = new short[hits.get(0).getADCValues().length];
        long id = hits.get(0).getCellID();
        for (RawTrackerHit hit : hits) {
            for (int i = 0; i < adcValues.length; i++) {
                adcValues[0] += hit.getADCValues()[i];
            }
        }
        return new BaseRawTrackerHit(id, 0, adcValues);
    }

    @Override
    protected void startOfData() {
        if (this.inputFilePaths.size() == 0) {
            throw new RuntimeException("No input file paths were provided!");
        }
        String filePath = this.inputFilePaths.remove();
        try {
            LOGGER.info("Opening overlay file: " + filePath);
            reader = new LCIOReader(new File(filePath));
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
            throw new RuntimeException(e);
        }
        if (inputFilePaths.size() == 0) {
            throw new EndOfDataException();
        }
        String path = inputFilePaths.remove();
        LOGGER.info("Opening next file: " + path);
        try {
            reader = new LCIOReader(new File(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

