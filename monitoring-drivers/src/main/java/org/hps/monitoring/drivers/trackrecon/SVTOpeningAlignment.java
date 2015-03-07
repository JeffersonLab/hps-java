package org.hps.monitoring.drivers.trackrecon;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author mgraham
 */
public class SVTOpeningAlignment extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    private String helicalTrackHitCollectionName = "HelicalTrackHits";
    private String rotatedTrackHitCollectionName = "RotatedHelicalTrackHits";
    private String l1to3CollectionName = "L1to3Tracks";
    private String l4to6CollectionName = "L4to6Tracks";
    private String outputPlots = null;
    IPlotter plotterTop;
    IPlotter plotterBot;
    IHistogram1D nTracks46Top;
    IHistogram1D nTracks13Top;
    IHistogram1D nTracks46Bot;
    IHistogram1D nTracks13Bot;

    public SVTOpeningAlignment() {
    }

    public void setOutputPlots(String output) {
        this.outputPlots = output;
    }

    public void setHelicalTrackHitCollectionName(String helicalTrackHitCollectionName) {
        this.helicalTrackHitCollectionName = helicalTrackHitCollectionName;
    }

    public void setL1to3CollectionName(String trackCollectionName) {
        this.l1to3CollectionName = trackCollectionName;
    }

    public void setL4to6CollectionName(String trackCollectionName) {
        this.l4to6CollectionName = trackCollectionName;
    }

    @Override
    protected void detectorChanged(Detector detector) {
        aida.tree().cd("/");

        IAnalysisFactory fac = aida.analysisFactory();
        plotterTop = fac.createPlotterFactory().create("HPS Tracking Plots");
        plotterTop.setTitle("Momentum");
        IPlotterStyle style = plotterTop.style();
        style.dataStyle().fillStyle().setColor("yellow");
        style.dataStyle().errorBarStyle().setVisible(false);
        plotterTop.createRegions(3, 3);
        //plotterFrame.addPlotter(plotter);

        nTracks13Top = aida.histogram1D("Number of L1-3 Tracks: Top ", 7, 0, 7.0);
        nTracks46Top = aida.histogram1D("Number of L4-6 Tracks: Top ", 7, 0, 7.0);

        IHistogram1D deld0Top = aida.histogram1D("Delta d0: Top", 50, -20.0, 20.0);
        IHistogram1D delphiTop = aida.histogram1D("Delta sin(phi): Top", 50, -0.1, 0.1);
        IHistogram1D delwTop = aida.histogram1D("Delta curvature: Top", 50, -0.0002, 0.0002);
        IHistogram1D dellamdaTop = aida.histogram1D("Delta slope: Top", 50, -0.02, 0.02);
        IHistogram1D delz0Top = aida.histogram1D("Delta y0: Top", 50, -5, 5.0);

        plotterTop.region(0).plot(deld0Top);
        plotterTop.region(3).plot(delphiTop);
        plotterTop.region(6).plot(delwTop);
        plotterTop.region(1).plot(dellamdaTop);
        plotterTop.region(4).plot(delz0Top);
        plotterTop.region(2).plot(nTracks13Top);
        plotterTop.region(5).plot(nTracks46Top);

        plotterBot = fac.createPlotterFactory().create("HPS Tracking Plots");
        plotterBot.setTitle("Momentum");
        IPlotterStyle styleBot = plotterBot.style();
        styleBot.dataStyle().fillStyle().setColor("yellow");
        styleBot.dataStyle().errorBarStyle().setVisible(false);
        plotterBot.createRegions(3, 3);
        //plotterFrame.addPlotter(plotter);

        nTracks13Bot = aida.histogram1D("Number of L1-3 Tracks: Bot ", 7, 0, 7.0);
        nTracks46Bot = aida.histogram1D("Number of L4-6 Tracks: Bot ", 7, 0, 7.0);

        IHistogram1D deld0Bot = aida.histogram1D("Delta d0: Bot", 50, -20.0, 20.0);
        IHistogram1D delphiBot = aida.histogram1D("Delta sin(phi): Bot", 50, -0.1, 0.1);
        IHistogram1D delwBot = aida.histogram1D("Delta curvature: Bot", 50, -0.0002, 0.0002);
        IHistogram1D dellamdaBot = aida.histogram1D("Delta slope: Bot", 50, -0.02, 0.02);
        IHistogram1D delz0Bot = aida.histogram1D("Delta y0: Bot", 50, -5, 5.0);

        plotterBot.region(0).plot(deld0Bot);
        plotterBot.region(3).plot(delphiBot);
        plotterBot.region(6).plot(delwBot);
        plotterBot.region(1).plot(dellamdaBot);
        plotterBot.region(4).plot(delz0Bot);
        plotterBot.region(2).plot(nTracks13Bot);
        plotterBot.region(5).plot(nTracks46Bot);

    }

    @Override
    public void process(EventHeader event) {
        aida.tree().cd("/");
        if (!event.hasCollection(HelicalTrackHit.class, helicalTrackHitCollectionName))
            return;

        if (!event.hasCollection(Track.class, l1to3CollectionName))
            return;

        if (!event.hasCollection(Track.class, l4to6CollectionName))
            return;

        List<Track> l1to3tracks = event.get(Track.class, l1to3CollectionName);
        List<Track> l4to6tracks = event.get(Track.class, l4to6CollectionName);

        List<Track> l1to3tracksTop = splitTrackList(l1to3tracks, true);
        List<Track> l1to3tracksBot = splitTrackList(l1to3tracks, false);
        List<Track> l4to6tracksTop = splitTrackList(l4to6tracks, true);
        List<Track> l4to6tracksBot = splitTrackList(l4to6tracks, false);

        nTracks13Top.fill(l1to3tracksTop.size());
        nTracks13Bot.fill(l1to3tracksBot.size());
        nTracks46Top.fill(l4to6tracksTop.size());
        nTracks46Bot.fill(l4to6tracksBot.size());

        for (Track trk46 : l4to6tracksTop) {
            TrackState ts46 = trk46.getTrackStates().get(0);
            for (Track trk13 : l1to3tracksTop) {
                TrackState ts13 = trk13.getTrackStates().get(0);
                aida.histogram1D("Delta d0: Top").fill(ts46.getD0() - ts13.getD0());
                aida.histogram1D("Delta sin(phi): Top").fill(Math.sin(ts46.getPhi()) - Math.sin(ts13.getPhi()));
                aida.histogram1D("Delta curvature: Top").fill(ts46.getOmega() - ts13.getOmega());
                aida.histogram1D("Delta y0: Top").fill(ts46.getZ0() - ts13.getZ0());
                aida.histogram1D("Delta slope: Top").fill(ts46.getTanLambda() - ts13.getTanLambda());
            }
        }

        for (Track trk46 : l4to6tracksBot) {
            TrackState ts46 = trk46.getTrackStates().get(0);
            for (Track trk13 : l1to3tracksBot) {
                TrackState ts13 = trk13.getTrackStates().get(0);
                aida.histogram1D("Delta d0: Bot").fill(ts46.getD0() - ts13.getD0());
                aida.histogram1D("Delta sin(phi): Bot").fill(Math.sin(ts46.getPhi()) - Math.sin(ts13.getPhi()));
                aida.histogram1D("Delta curvature: Bot").fill(ts46.getOmega() - ts13.getOmega());
                aida.histogram1D("Delta y0: Bot").fill(ts46.getZ0() - ts13.getZ0());
                aida.histogram1D("Delta slope: Bot").fill(ts46.getTanLambda() - ts13.getTanLambda());
            }
        }

    }

    @Override
    public void endOfData() {
        if (outputPlots != null)
            try {
                plotterTop.writeToFile(outputPlots + "-deltasTop.gif");
                plotterBot.writeToFile(outputPlots + "-deltasBottom.gif");
            } catch (IOException ex) {
                Logger.getLogger(TrackingReconPlots.class.getName()).log(Level.SEVERE, null, ex);
            }
    }

    private List<Track> splitTrackList(List<Track> trks, boolean doTop) {
        List<Track> tracksHalf = new ArrayList<Track>();
        boolean isTop = false;
        boolean isBot = false;
        for (Track trk : trks) {
            isTop = false;
            isBot = false;
            for (TrackerHit hit : trk.getTrackerHits())
                if (hit.getPosition()[2] > 0)//remember, non-bend in tracking frame is z-direction
                    isTop = true;
                else
                    isBot = true;
            if (isTop == true && isBot != true && doTop == true)  //if we want top tracks and all hits are in top
                tracksHalf.add(trk);
            if (isBot == true && isTop != true && doTop == false) //if we want bottom tracks and all hits are in bottom
                tracksHalf.add(trk);
        }
        return tracksHalf;
    }
}
