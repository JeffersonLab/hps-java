package org.lcsim.hps.users.mgraham;

import java.io.IOException;

import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.lcio.LCIOWriter;
import org.lcsim.util.Driver;

/**
 *
 * @author mgraham Created 5/17/2012 really the same as
 * org.lcsim.util.LCIODriver but only write events that pass some condition
 * (e.g. has a reconstructed track)
 */
public class MyLCIOWriterDriver extends Driver {

    private String outputFile;
    private LCIOWriter writer;
    private String trackCollectionName = "MatchedTracks";

    public MyLCIOWriterDriver() {
    }

    public void setOutputFilePath(String output) {
        this.outputFile = output;
    }
    
      public void setTrackCollectionNamePath(String trackCollection) {
        this.trackCollectionName = trackCollection;
    }

    private void setupWriter() {
        // Cleanup existing writer.
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
                writer = null;
            } catch (IOException x) {
                System.err.println(x.getMessage());
            }
        }

        // Setup new writer.
        try {
            writer = new LCIOWriter(outputFile);
        } catch (IOException x) {
            throw new RuntimeException("Error creating writer", x);
        }


        try {
            writer.reOpen();
        } catch (IOException x) {
            throw new RuntimeException("Error rewinding LCIO file", x);
        }
    }

    protected void startOfData() {
        setupWriter();
    }

    protected void endOfData() {
        try {
            writer.close();
        } catch (IOException x) {
            throw new RuntimeException("Error rewinding LCIO file", x);
        }
    }

    protected void process(EventHeader event) {
        if (event.get(Track.class, trackCollectionName).size()>1) {
            System.out.println("found a two track event...writing to lcio file");
            try {
                
                writer.write(event);
            } catch (IOException x) {
                throw new RuntimeException("Error writing LCIO file", x);
            }
        }
    }

    protected void suspend() {
        try {
            writer.flush();
        } catch (IOException x) {
            throw new RuntimeException("Error flushing LCIO file", x);
        }
    }
}
