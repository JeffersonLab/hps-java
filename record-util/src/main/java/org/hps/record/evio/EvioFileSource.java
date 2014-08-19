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
 * A very basic implementation of <tt>AbstractRecordSource</tt> for supplying <tt>EvioEvent</tt>
 * objects to a loop from EVIO files.  Unlike the LCIO record source, it has no rewind or
 * indexing capabilities (for now at least).
 */
public final class EvioFileSource extends AbstractRecordSource {

    EvioEvent currentEvent;
    EvioReader reader;
    List<File> files = new ArrayList<File>();
    int fileIndex = 0;
    boolean atEnd;

    public EvioFileSource(List<File> files) {
        this.files.addAll(files);
        openReader();
    }

    public EvioFileSource(File file) {
        this.files.add(file);
        openReader();
    }

    private void openReader() {
        try {
            System.out.println("Opening reader for file " + files.get(fileIndex) + " ...");
            reader = new EvioReader(files.get(fileIndex), false);
            System.out.println("Done opening file.");
        } catch (EvioException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void closeReader() {
        try {
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object getCurrentRecord() throws IOException {
        return currentEvent;
    }
    
    boolean endOfFiles() {
        return fileIndex > (files.size() - 1);
    }
    
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
    
    @Override
    public boolean supportsCurrent() {
        return true;
    }

    @Override
    public boolean supportsNext() {
        return true;
    }
    
    @Override
    public boolean supportsPrevious() {
        return false;
    }
    
    @Override
    public boolean supportsIndex() {
        return false;
    }
    
    @Override
    public boolean supportsShift() {
        return false;
    }
    
    @Override
    public boolean supportsRewind() {
        return false;
    }
    
    @Override
    public boolean hasCurrent() {
        return currentEvent != null;
    }
    
    @Override
    public boolean hasNext() {
        return !atEnd;
    }
}