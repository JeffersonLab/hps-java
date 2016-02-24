package org.hps.record.evio;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.freehep.record.source.NoSuchRecordException;
import org.hps.record.AbstractRecordQueue;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;

/**
 * A basic implementation of an <code>AbstractRecordSource</code> for supplying <code>EvioEvent</code> objects to a 
 * loop from a list of EVIO files.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class EvioFileSource extends AbstractRecordQueue<EvioEvent> {

    private static final Logger LOGGER = Logger.getLogger(EvioFileSource.class.getPackage().getName());
    
    /**
     * The current event.
     */
    private EvioEvent currentEvent;

    /**
     * The current file index.
     */
    private int fileIndex = 0;

    /**
     * The list of input data files.
     */
    private final List<File> files = new ArrayList<File>();

    /**
     * The reader to use for reading and parsing the EVIO data.
     */
    private EvioReader reader;
    
    /**
     * Whether to continue on parse errors or not.
     */
    private boolean continueOnErrors = false;
   
    /**
     * Constructor taking a single EVIO file.
     *
     * @param file the EVIO file
     */
    public EvioFileSource(final File file) {
        this.files.add(file);
        this.openReader();
    }

    /**
     * Constructor taking a list of EVIO files.
     *
     * @param files the list of EVIO files
     */
    public EvioFileSource(final List<File> files) {
        this.files.addAll(files);
        this.openReader();
    }
    
    /**
     * Set whether to continue on errors or not.
     * @param continueOnErrors <code>true</code> to continue on errors
     */
    public void setContinueOnErrors(boolean continueOnErrors) {
        this.continueOnErrors = continueOnErrors;
    }
    
    /**
     * Close the current reader.
     */
    private void closeReader() {
        try {
            this.reader.close();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return <code>true</code> if there are no more files to open from the list.
     *
     * @return <code>true</code> if there are no more files in the list
     */
    boolean endOfFiles() {
        return this.fileIndex > this.files.size() - 1;
    }

    /**
     * Get the current record which is an <code>EvioEvent</code>.
     *
     * @return the current record
     */
    @Override
    public Object getCurrentRecord() throws IOException {
        return this.currentEvent;
    }

    /**
     * Get the list of files.
     *
     * @return the list of files
     */
    public List<File> getFiles() {
        return this.files;
    }

    /**
     * Return <code>true</code> if there is a current record loaded.
     *
     * @return <code>true</code> if there is a current record loaded.
     */
    @Override
    public boolean hasCurrent() {
        return this.currentEvent != null;
    }

    /**
     * Return <code>true</code> if there is a next record.
     *
     * @return <code>true</code> if there are more records to load
     */
    @Override
    public boolean hasNext() {
        try {
            return this.reader.getNumEventsRemaining() != 0;
        } catch (IOException | EvioException e) {
            throw new RuntimeException("Error getting num remaining events.");
        }
    }

    /**
     * Load the next record.
     *
     * @throws NoSuchRecordException if source is exhausted
     * @throws IOException if there is an error creating the next <code>EvioEvent</code>
     */
    @Override
    public void next() throws IOException, NoSuchRecordException {
        for (;;) {
            try {
                this.currentEvent = this.reader.parseNextEvent();
                if (this.reader.getNumEventsRemaining() == 0 && this.currentEvent == null) {
                    this.closeReader();
                    this.fileIndex++;
                    if (!this.endOfFiles()) {
                        this.openReader();
                    } else {
                        throw new NoSuchRecordException("End of data.");
                    }
                } else {
                    LOGGER.finest("Read EVIO event " + this.currentEvent.getEventNumber() + " okay.");
                    break;
                }                   
            } catch (EvioException | NegativeArraySizeException e) { 
                LOGGER.log(Level.SEVERE, "Error parsing next EVIO event.", e);
                if (!continueOnErrors) {
                    throw new IOException("Fatal error parsing next EVIO event.", e);
                }
            } catch (Exception e) {
                throw new IOException("Error parsing EVIO event.", e);
            }
        }
    }

    /**
     * Open the next file in the list with the reader.
     *
     * @throws RuntimeException if an <code>EvioException</code> or <code>IOException</code> occurs while opening file
     */
    private void openReader() {
        try {
            // FIXME: This should use the reader directly and MSS paths should be transformed externally.
            LOGGER.info("opening EVIO file " + this.files.get(this.fileIndex).getPath() + " ...");
            this.reader = EvioFileUtilities.open(this.files.get(this.fileIndex), true);
        } catch (EvioException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns <code>true</code> to indicate next record capability is supported.
     *
     * @return <code>true</code> to indicate next record capability is supported
     */
    @Override
    public boolean supportsNext() {
        return true;
    }
    
    /**
     * Get the current file being processed.
     * 
     * @return the current file
     */
    File getCurrentFile() {
        return this.files.get(this.fileIndex);
    }

}
