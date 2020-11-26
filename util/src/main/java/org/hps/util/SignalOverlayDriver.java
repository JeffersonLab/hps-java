package org.hps.util;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;

import org.lcsim.event.EventHeader;
import org.lcsim.event.EventHeader.LCMetaData;
import org.lcsim.lcio.LCIOReader;
import org.lcsim.util.Driver;

/**
 * Overlay signal collections from a side event stream every N events.
 */
public class SignalOverlayDriver extends Driver {

    private static Logger LOGGER = Logger.getLogger(SignalOverlayDriver.class.getPackage().getName());

    private Queue<String> signalFileNames = new LinkedList<String>();
    private int eventSpacing = 250;
    private int sequence = 0;
    LCIOReader reader = null;

    class EndOfDataException extends Exception {
        private static final long serialVersionUID = 1L;
    }

    public void setSignalFile(String signalFileName) {
        this.signalFileNames.add(signalFileName);
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
            LOGGER.fine("Processing background event: " + event.getEventNumber());
            if (this.sequence % this.eventSpacing == 0) {
                EventHeader signalEvent = this.readNextEvent();
                LOGGER.fine("Read signal event: " + signalEvent.getEventNumber());
                Set<List> lists = signalEvent.getLists();
                for (List list : lists) {
                    LCMetaData meta = signalEvent.getMetaData(list);
                    String collName = meta.getName();
                    if (!event.hasItem(collName)) {
                        event.put(collName, list, meta.getType(), meta.getFlags());
                    } else {
                        event.get(meta.getClass(), collName).addAll(list);
                    }
                    LOGGER.info("Added " + list.size() + " objects to collection: " + collName);
                }
            }
            ++this.sequence;
        } catch (EndOfDataException e) {
            throw new RuntimeException("Ran out of signal events!");
        }
    }
}

