package org.hps.monitoring.drivers.trackrecon;

import static org.hps.monitoring.drivers.trackrecon.PlotAndFitUtilities.fitAndPutParameters;
import hep.aida.IAnalysisFactory;
import hep.aida.IFitFactory;
import hep.aida.IFitter;
import hep.aida.IFunction;
import hep.aida.IFunctionFactory;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
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

    static private AIDA aida = AIDA.defaultInstance();
    private String helicalTrackHitCollectionName = "HelicalTrackHits";
    private String rotatedTrackHitCollectionName = "RotatedHelicalTrackHits";
    private String l0to3CollectionName = "L0to3Tracks";
    private String l4to6CollectionName = "L4to6Tracks";
    private String outputPlots = null;
    IPlotter plotterTop;
    IPlotter plotterBot;
    IHistogram1D nTracks46Top;
    IHistogram1D nTracks03Top;
    IHistogram1D nTracks46Bot;
    IHistogram1D nTracks03Bot;
    IHistogram1D deld0Top;
    IHistogram1D delphiTop;
    IHistogram1D delwTop;
    IHistogram1D dellambdaTop;
    IHistogram1D delz0Top;
    IHistogram1D deld0Bot;
    IHistogram1D delphiBot;
    IHistogram1D delwBot;
    IHistogram1D dellambdaBot;
    IHistogram1D delz0Bot;

    IPlotterFactory plotterFactory;
    IFunctionFactory functionFactory;
    IFitFactory fitFactory;
    IFunction fd0Top;
    IFunction fphi0Top;
    IFunction fz0Top;
    IFunction flambdaTop;
    IFunction fwTop;
    IFunction fd0Bot;
    IFunction fphi0Bot;
    IFunction fz0Bot;
    IFunction flambdaBot;
    IFunction fwBot;

    IFitter jminChisq;

    public SVTOpeningAlignment() {
    }

    public void setOutputPlots(String output) {
        this.outputPlots = output;
    }

    public void setHelicalTrackHitCollectionName(String helicalTrackHitCollectionName) {
        this.helicalTrackHitCollectionName = helicalTrackHitCollectionName;
    }

    public void setL0to3CollectionName(String trackCollectionName) {
        this.l0to3CollectionName = trackCollectionName;
    }

    public void setL4to6CollectionName(String trackCollectionName) {
        this.l4to6CollectionName = trackCollectionName;
    }

    @Override
    protected void detectorChanged(Detector detector) {
        aida.tree().cd("/");

        IAnalysisFactory fac = aida.analysisFactory();
        IPlotterFactory pfac = fac.createPlotterFactory("SVT Alignment");
        functionFactory = aida.analysisFactory().createFunctionFactory(null);
        fitFactory = aida.analysisFactory().createFitFactory();
        jminChisq = fitFactory.createFitter("chi2", "jminuit");

        plotterTop = pfac.create("Top Layers");
        IPlotterStyle style = plotterTop.style();
        style.dataStyle().fillStyle().setColor("yellow");
        style.dataStyle().errorBarStyle().setVisible(false);
        style.legendBoxStyle().setVisible(false);
        style.dataStyle().outlineStyle().setVisible(false);
        plotterTop.createRegions(3, 3);
        // plotterFrame.addPlotter(plotter);

        IPlotterStyle functionStyle = pfac.createPlotterStyle();
        functionStyle.dataStyle().lineStyle().setColor("red");
        functionStyle.dataStyle().markerStyle().setVisible(true);
        functionStyle.dataStyle().markerStyle().setColor("black");
        functionStyle.dataStyle().markerStyle().setShape("dot");
        functionStyle.dataStyle().markerStyle().setSize(2);

        nTracks03Top = aida.histogram1D("Number of L0-3 Tracks: Top ", 7, 0, 7.0);
        nTracks46Top = aida.histogram1D("Number of L4-6 Tracks: Top ", 7, 0, 7.0);

        deld0Top = aida.histogram1D("Delta d0: Top", 50, -20.0, 20.0);
        delphiTop = aida.histogram1D("Delta sin(phi): Top", 50, -0.1, 0.1);
        delwTop = aida.histogram1D("Delta curvature: Top", 50, -0.0002, 0.0002);
        dellambdaTop = aida.histogram1D("Delta slope: Top", 50, -0.01, 0.01);
        delz0Top = aida.histogram1D("Delta y0: Top", 50, -2.5, 2.5);

        fd0Top = functionFactory.createFunctionByName("Gaussian", "G");
        fphi0Top = functionFactory.createFunctionByName("Gaussian", "G");
        fwTop = functionFactory.createFunctionByName("Gaussian", "G");
        flambdaTop = functionFactory.createFunctionByName("Gaussian", "G");
        fz0Top = functionFactory.createFunctionByName("Gaussian", "G");

        plotterTop.region(0).plot(deld0Top);
        plotterTop.region(3).plot(delphiTop);
        plotterTop.region(6).plot(delwTop);
        plotterTop.region(1).plot(dellambdaTop);
        plotterTop.region(4).plot(delz0Top);
        plotterTop.region(2).plot(nTracks03Top);
        plotterTop.region(5).plot(nTracks46Top);
        plotterTop.region(0).plot(fd0Top, functionStyle);
        plotterTop.region(3).plot(fphi0Top, functionStyle);
        plotterTop.region(6).plot(fwTop, functionStyle);
        plotterTop.region(1).plot(flambdaTop, functionStyle);
        plotterTop.region(4).plot(fz0Top, functionStyle);
        plotterTop.show();

        plotterBot = pfac.create("Bottom Layers");
        IPlotterStyle styleBot = plotterBot.style();
        styleBot.legendBoxStyle().setVisible(false);
        styleBot.dataStyle().fillStyle().setColor("yellow");
        styleBot.dataStyle().errorBarStyle().setVisible(false);
        styleBot.dataStyle().outlineStyle().setVisible(false);
        plotterBot.createRegions(3, 3);

        nTracks03Bot = aida.histogram1D("Number of L0-3 Tracks: Bot ", 7, 0, 7.0);
        nTracks46Bot = aida.histogram1D("Number of L4-6 Tracks: Bot ", 7, 0, 7.0);

        deld0Bot = aida.histogram1D("Delta d0: Bot", 50, -20.0, 20.0);
        delphiBot = aida.histogram1D("Delta sin(phi): Bot", 50, -0.1, 0.1);
        delwBot = aida.histogram1D("Delta curvature: Bot", 50, -0.0002, 0.0002);
        dellambdaBot = aida.histogram1D("Delta slope: Bot", 50, -0.01, 0.01);
        delz0Bot = aida.histogram1D("Delta y0: Bot", 50, -2.5, 2.5);

        fd0Bot = functionFactory.createFunctionByName("Gaussian", "G");
        fphi0Bot = functionFactory.createFunctionByName("Gaussian", "G");
        fwBot = functionFactory.createFunctionByName("Gaussian", "G");
        flambdaBot = functionFactory.createFunctionByName("Gaussian", "G");
        fz0Bot = functionFactory.createFunctionByName("Gaussian", "G");

        plotterBot.region(0).plot(deld0Bot);
        plotterBot.region(3).plot(delphiBot);
        plotterBot.region(6).plot(delwBot);
        plotterBot.region(1).plot(dellambdaBot);
        plotterBot.region(4).plot(delz0Bot);
        plotterBot.region(2).plot(nTracks03Bot);
        plotterBot.region(5).plot(nTracks46Bot);
        plotterBot.region(0).plot(fd0Bot, functionStyle);
        plotterBot.region(3).plot(fphi0Bot, functionStyle);
        plotterBot.region(6).plot(fwBot, functionStyle);
        plotterBot.region(1).plot(flambdaBot, functionStyle);
        plotterBot.region(4).plot(fz0Bot, functionStyle);
        plotterBot.show();
    }

    @Override
    public void process(EventHeader event) {
        aida.tree().cd("/");
        if (!event.hasCollection(HelicalTrackHit.class, helicalTrackHitCollectionName))
            return;

        if (!event.hasCollection(Track.class, l0to3CollectionName))
            return;

        if (!event.hasCollection(Track.class, l4to6CollectionName))
            return;

        List<Track> l0to3tracks = event.get(Track.class, l0to3CollectionName);
        List<Track> l4to6tracks = event.get(Track.class, l4to6CollectionName);

        List<Track> l0to3tracksTop = splitTrackList(l0to3tracks, true);
        List<Track> l0to3tracksBot = splitTrackList(l0to3tracks, false);
        List<Track> l4to6tracksTop = splitTrackList(l4to6tracks, true);
        List<Track> l4to6tracksBot = splitTrackList(l4to6tracks, false);

        nTracks03Top.fill(l0to3tracksTop.size());
        nTracks03Bot.fill(l0to3tracksBot.size());
        nTracks46Top.fill(l4to6tracksTop.size());
        nTracks46Bot.fill(l4to6tracksBot.size());

        for (Track trk46 : l4to6tracksTop) {
            TrackState ts46 = trk46.getTrackStates().get(0);
            for (Track trk03 : l0to3tracksTop) {
                TrackState ts03 = trk03.getTrackStates().get(0);
                deld0Top.fill(ts46.getD0() - ts03.getD0());
                delphiTop.fill(Math.sin(ts46.getPhi()) - Math.sin(ts03.getPhi()));
                delwTop.fill(ts46.getOmega() - ts03.getOmega());
                delz0Top.fill(ts46.getZ0() - ts03.getZ0());
                dellambdaTop.fill(ts46.getTanLambda() - ts03.getTanLambda());
            }
        }
        fitAndPutParameters(deld0Top, fd0Top);
        fitAndPutParameters(delphiTop, fphi0Top);
        fitAndPutParameters(delwTop, fwTop);
        fitAndPutParameters(delz0Top, fz0Top);
        fitAndPutParameters(dellambdaTop, flambdaTop);

        for (Track trk46 : l4to6tracksBot) {
            TrackState ts46 = trk46.getTrackStates().get(0);
            for (Track trk03 : l0to3tracksBot) {
                TrackState ts03 = trk03.getTrackStates().get(0);
                deld0Bot.fill(ts46.getD0() - ts03.getD0());
                delphiBot.fill(Math.sin(ts46.getPhi()) - Math.sin(ts03.getPhi()));
                delwBot.fill(ts46.getOmega() - ts03.getOmega());
                delz0Bot.fill(ts46.getZ0() - ts03.getZ0());
                dellambdaBot.fill(ts46.getTanLambda() - ts03.getTanLambda());
            }
        }

        // IFunction currentFitFunction = performGaussianFit(deld0Bot, fd0Bot, jminChisq).fittedFunction();;
        // fd0Bot.setParameters(currentFitFunction.parameters());
        fitAndPutParameters(deld0Bot, fd0Bot);
        fitAndPutParameters(delphiBot, fphi0Bot);
        fitAndPutParameters(delwBot, fwBot);
        fitAndPutParameters(delz0Bot, fz0Bot);
        fitAndPutParameters(dellambdaBot, flambdaBot);

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
                if (hit.getPosition()[2] > 0)// remember, non-bend in tracking frame is z-direction
                    isTop = true;
                else
                    isBot = true;
            if (isTop == true && isBot != true && doTop == true)  // if we want top tracks and all hits are in top
                tracksHalf.add(trk);
            if (isBot == true && isTop != true && doTop == false) // if we want bottom tracks and all hits are in bottom
                tracksHalf.add(trk);
        }
        return tracksHalf;
    }
}
