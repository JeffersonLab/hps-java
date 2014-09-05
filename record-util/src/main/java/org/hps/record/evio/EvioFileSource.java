package org.hps.record.evio;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.freehep.record.source.AbstractRecordSource;
import org.freehep.record.source.NoSuchRecordException;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;

/**
 * A basic implementation of an <tt>AbstractRecordSource</tt> for supplying <tt>EvioEvent</tt>
 * objects to a loop from EVIO files.  
 * 
 * Unlike the LCIO record source, it has no rewind or indexing capabilities.
 */
public final class EvioFileSource extends AbstractRecordSource {

    EvioEvent currentEvent;
    EvioReader reader;
    List<File> files = new ArrayList<File>();
    int fileIndex = 0;
    boolean atEnd;

    /**
     * Constructor taking a list of EVIO files.
     * @param files The list of EVIO files.
     */
    public EvioFileSource(List<File> files) {
        this.files.addAll(files);
        openReader();
    }

    /**
     * Constructor taking a single EVIO file.
     * @param file The EVIO file.
     */
    public EvioFileSource(File file) {
        this.files.add(file);
        openReader();
    }

    /**
     * Open the EVIO reader on the current file from the list.
     * @throws RuntimeException if an EvioException or IOException occurs while opening file.
     */
    private void openReader() {
        try {
            System.out.println("Opening reader for file " + files.get(fileIndex) + " ...");
            reader = new EvioReader(files.get(fileIndex), false);
            System.out.println("Done opening file.");
        } catch (EvioException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Close the current reader.
     */
    private void closeReader() {
        try {
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the current record which is an <code>EvioEvent</code>.
     * @return The current record.s
     */
    @Override
    public Object getCurrentRecord() throws IOException {
        return currentEvent;
    }
    
    /**
     * True if there are no more files to open in the list.
     * @return True if there are no more files in the list.
     */
    boolean endOfFiles() {
        return fileIndex > (files.size() - 1);
    }
    
    /**
     * Load the next record.
     * @throws NoSuchRecordException if source is exhausted.
     * @throws IOException if there is an error creating the next EvioEvent.
     */
    @Override
    public void next() throws IOException, NoSuchRecordException {
        for (;;) {
            try {
                currentEvent = reader.parseNextEvent();
            } catch (EvioException e) {
                throw new IOException(e);
            }           
            if (currentEvent == null) {
                closeReader();
                fileIndex++;
                if (!endOfFiles()) {
                    openReader();
                    continue;
                } else {
                    atEnd = true;
                    throw new NoSuchRecordException();
                }
            }
            return;
        }
    }
   
    /**
     * True because source supports loading next record.
     * @return True because source supports loading next record.
     */
    @Override
    public boolean supportsNext() {
        return true;
    }
    
    /**
     * True if there is a current record loaded.
     * @return True if there is a current record loaded.
     */
    @Override
    public boolean hasCurrent() {
        return currentEvent != null;
    }
    
    /**
     * True if there are more records to load.
     * @return True if there are more records to load.
     */
    @Override
    public boolean hasNext() {
        return !atEnd;
    }
}