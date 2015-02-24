package org.hps.monitoring.drivers.svt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;

import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class GblTrackingReconstructionPlots extends Driver {
    private Logger logger = getLogger();
    private AIDA aida = AIDA.defaultInstance();
    private String outputPlots = null;
    private final String trackCollectionName = "MatchedTracks";
    private final String gblTrackCollectionName = "GblTracks";
    IHistogram1D nTracks;
    IHistogram1D nTracksGbl;
    IHistogram1D nTracksDiff;
    IPlotter plotter1;
    
    
    public GblTrackingReconstructionPlots() {
        // TODO Auto-generated constructor stub
        logger.setLevel(Level.WARNING);
    }

    public void setOutputPlots(String output) {
        this.outputPlots = output;
    }
    
    protected void detectorChanged(Detector detector) {
        aida.tree().cd("/");
        IAnalysisFactory fac = aida.analysisFactory();
        
        nTracks = aida.histogram1D("Seed tracks per event", 3, 0, 3);
        nTracksGbl = aida.histogram1D("Gbl tracks per event", 3, 0, 3);
        nTracksDiff = aida.histogram1D("Tracks per event Seed-Gbl", 6, -3, 3);
        plotter1 = fac.createPlotterFactory().create("Number of tracks per event");
        plotter1.setTitle("Other");
        //plotterFrame.addPlotter(plotter222);
        IPlotterStyle style1 = plotter1.style();
        style1.dataStyle().fillStyle().setColor("yellow");
        style1.dataStyle().errorBarStyle().setVisible(false);
        plotter1.createRegions(3, 1);
        plotter1.region(0).plot(nTracks);
        plotter1.region(1).plot(nTracksGbl);
        plotter1.region(2).plot(nTracksDiff);
        plotter1.show();

        
    }
    
    protected void process(EventHeader event) {
        
        List<Track> tracks;
        if(event.hasCollection(Track.class, trackCollectionName)) {
            tracks = event.get(Track.class, trackCollectionName);
        } else {
           logger.warning("no seed track collection");
           tracks = new ArrayList<Track>();
        }
        List<Track> gblTracks;
        if(event.hasCollection(Track.class, gblTrackCollectionName)) {
            gblTracks = event.get(Track.class, gblTrackCollectionName);
        } else {
           logger.warning("no gbl track collection");
           gblTracks = new ArrayList<Track>();
        }
        nTracks.fill(tracks.size());
        nTracksGbl.fill(gblTracks.size());
        nTracksDiff.fill(tracks.size()-gblTracks.size());
        
        
    }
    
    
    public void endOfData() {
        if (outputPlots != null) {
            try {
                aida.saveAs(outputPlots);
            } catch (IOException ex) {
                logger.log(Level.SEVERE,"aid problem saving file",ex);
            }
        }
        //plotterFrame.dispose();
        //topFrame.dispose();
        //bottomFrame.dispose();
    }

    
    
}
