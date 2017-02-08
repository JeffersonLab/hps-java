package org.hps.users.mgraham;

import java.io.IOException;
import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.lcio.LCIOWriter;
import org.lcsim.util.Driver;

/**
 * org.lcsim.util.LCIODriver but only write events that pass some condition
 * (e.g. has a reconstructed track)
 */
public class MyLCIOWriterDriver extends Driver {

    private String outputFile;
    private LCIOWriter writer;
    private String trackCollectionName = "MatchedTracks";
    private int nTracks = 1; //only ==# tracks for now...change this later...
    private double d0Cut = 9999;
    private double z0Cut = 9999;
    private double pCutMin = -9999;
    private double pCutMax = 9999;

    public MyLCIOWriterDriver() {
    }

    public void setOutputFilePath(String output) {
        this.outputFile = output;
    }

    public void setNTracks(int ntrk) {
        this.nTracks = ntrk;
    }

    public void setD0Cut(double cut) {
        this.d0Cut = cut;
    }

    public void setZ0Cut(double cut) {
        this.z0Cut = cut;
    }

    public void setPCutMax(double cut) {
        this.pCutMax = cut;
    }

    public void setPCutMin(double cut) {
        this.pCutMin = cut;
    }

    public void setTrackCollectionNamePath(String trackCollection) {
        this.trackCollectionName = trackCollection;
    }

    private void setupWriter() {
        // Cleanup existing writer.
        if (writer != null)
            try {
                writer.flush();
                writer.close();
                writer = null;
            } catch (IOException x) {
                System.err.println(x.getMessage());
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
        if (event.get(Track.class, trackCollectionName).size() != nTracks)
            return;

        List<Track> tracks = event.get(Track.class, trackCollectionName);

        for (Track trk : tracks) {
            //if any of the tracks fail the cuts, return...
            if (Math.abs(trk.getTrackStates().get(0).getD0()) > d0Cut)
                return;
            if (Math.abs(trk.getTrackStates().get(0).getZ0()) > z0Cut)
                return;
            if (Math.abs(trk.getTrackStates().get(0).getMomentum()[0]) < pCutMin)
                return;
            if (Math.abs(trk.getTrackStates().get(0).getMomentum()[0]) > pCutMax)
                return;
        }
        //if I got here I want to save the event.
        try {
            writer.write(event);
        } catch (IOException x) {
            throw new RuntimeException("Error writing LCIO file", x);
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
