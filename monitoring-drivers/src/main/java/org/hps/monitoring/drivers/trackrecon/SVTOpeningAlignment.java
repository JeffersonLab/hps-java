package org.hps.monitoring.drivers.trackrecon;

import static org.hps.monitoring.drivers.trackrecon.PlotAndFitUtilities.fitAndPutParameters;
import hep.aida.IAnalysisFactory;
import hep.aida.IFitFactory;
import hep.aida.IFitter;
import hep.aida.IFunction;
import hep.aida.IFunctionFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hps.recon.tracking.TrackUtils;

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
    IPlotter plotterParsTop;
    IPlotter plotterBot;
    IPlotter plotterParsBot;
    IPlotter plotterHinge;

    IHistogram1D nTracks46Top;
    IHistogram1D nTracks03Top;
    IHistogram1D nTracks46Bot;
    IHistogram1D nTracks03Bot;
    IHistogram1D nHits03Top;
    IHistogram1D nHits46Top;
    IHistogram1D nHits03Bot;
    IHistogram1D nHits46Bot;
    IHistogram1D deld0Top;
    IHistogram1D delphiTop;
    IHistogram1D delwTop;
    IHistogram1D dellambdaTop;
    IHistogram1D delz0Top;
    IHistogram1D lambdaTopL03;
    IHistogram1D z0TopL03;
    IHistogram1D lambdaTopL46;
    IHistogram1D z0TopL46;
    IHistogram1D deld0Bot;
    IHistogram1D delphiBot;
    IHistogram1D delwBot;
    IHistogram1D dellambdaBot;
    IHistogram1D delz0Bot;
    IHistogram1D lambdaBotL03;
    IHistogram1D z0BotL03;
    IHistogram1D lambdaBotL46;
    IHistogram1D z0BotL46;

    IHistogram1D zTargetTopL03;
    IHistogram1D zTargetTopL46;
    IHistogram1D zTargetBotL03;
    IHistogram1D zTargetBotL46;

    IHistogram2D lambdaVsz0TopL03;
    IHistogram2D lambdaVsz0BotL03;
    IHistogram2D lambdaVsz0TopL46;
    IHistogram2D lambdaVsz0BotL46;

    IHistogram1D delYAtHingeTop;
    IHistogram1D delYAtHingeBot;
    IHistogram2D yAtHingeL03VsL46Top;
    IHistogram2D yAtHingeL03VsL46Bot;
    IHistogram2D delYAtHingeVsL03SlopeTop;
    IHistogram2D delYAtHingeVsL46SlopeTop;
    IHistogram2D delYAtHingeVsL03SlopeBot;
    IHistogram2D delYAtHingeVsL46SlopeBot;

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

    boolean matchFullTracks = false;
    String fullTrackCollectionName = "s234_c5_e167";
    double targetPosition = -5.0; //mm

    public SVTOpeningAlignment() {
    }

    public void setMatchFullTracks(boolean match) {
        this.matchFullTracks = match;
    }

    public void setFullTrackCollectionName(String name) {
        this.fullTrackCollectionName = name;
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

        nTracks03Top = aida.histogram1D("Number of L0-3 Tracks: Top ", 7, 0, 7);
        nTracks46Top = aida.histogram1D("Number of L4-6 Tracks: Top ", 7, 0, 7);
        nHits03Top = aida.histogram1D("Number of L0-3 Hits: Top ", 6, 3, 9);
        nHits46Top = aida.histogram1D("Number of L4-6 Hits: Top ", 6, 3, 9);
        deld0Top = aida.histogram1D("Delta d0: Top", 50, -20.0, 20.0);
        delphiTop = aida.histogram1D("Delta sin(phi): Top", 50, -0.1, 0.1);
        delwTop = aida.histogram1D("Delta curvature: Top", 50, -0.0002, 0.0002);
        dellambdaTop = aida.histogram1D("Delta slope: Top", 50, -0.01, 0.01);
        delz0Top = aida.histogram1D("Delta yTarget: Top", 50, -2.5, 2.5);

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
        plotterTop.region(7).plot(nHits03Top);
        plotterTop.region(8).plot(nHits46Top);
        plotterTop.region(0).plot(fd0Top, functionStyle);
        plotterTop.region(3).plot(fphi0Top, functionStyle);
        plotterTop.region(6).plot(fwTop, functionStyle);
        plotterTop.region(1).plot(flambdaTop, functionStyle);
        plotterTop.region(4).plot(fz0Top, functionStyle);
        plotterTop.show();

        plotterParsTop = pfac.create("Top Track Pars");
        plotterParsTop.createRegions(2, 4);
        lambdaTopL03 = aida.histogram1D("slope: Top L0-3", 50, 0, 0.06);
        z0TopL03 = aida.histogram1D("y0: Top L0-3", 50, -2.5, 2.5);
        lambdaVsz0TopL03 = aida.histogram2D("slope vs y0: Top L0-3", 50, -2.5, 2.5, 50, 0.0, 0.06);
        lambdaTopL46 = aida.histogram1D("slope: Top L4-6", 50, 0, 0.06);
        z0TopL46 = aida.histogram1D("y0: Top L4-6", 50, -2.5, 2.5);
        lambdaVsz0TopL46 = aida.histogram2D("slope vs yTarget: Top L4-6", 50, -2.5, 2.5, 50, 0.0, 0.06);
        zTargetTopL03 = aida.histogram1D("yTarget: Top L0-3", 50, -2.5, 2.5);
        zTargetTopL46 = aida.histogram1D("yTarget: Top L4-6", 50, -2.5, 2.5);

        plotterParsTop.region(0).plot(lambdaTopL03);
        plotterParsTop.region(4).plot(lambdaTopL46);
        plotterParsTop.region(1).plot(z0TopL03);
        plotterParsTop.region(5).plot(z0TopL46);
        plotterParsTop.region(2).plot(lambdaVsz0TopL03);
        plotterParsTop.region(6).plot(lambdaVsz0TopL46);
        plotterParsTop.region(3).plot(zTargetTopL03);
        plotterParsTop.region(7).plot(zTargetTopL46);
        plotterParsTop.show();

        plotterBot = pfac.create("Bottom Layers");
        IPlotterStyle styleBot = plotterBot.style();
        styleBot.legendBoxStyle().setVisible(false);
        styleBot.dataStyle().fillStyle().setColor("yellow");
        styleBot.dataStyle().errorBarStyle().setVisible(false);
        styleBot.dataStyle().outlineStyle().setVisible(false);
        plotterBot.createRegions(3, 3);

        nTracks03Bot = aida.histogram1D("Number of L0-3 Tracks: Bot ", 7, 0, 7);
        nTracks46Bot = aida.histogram1D("Number of L4-6 Tracks: Bot ", 7, 0, 7);
        nHits03Bot = aida.histogram1D("Number of L0-3 Hits: Bot ", 6, 3, 9);
        nHits46Bot = aida.histogram1D("Number of L4-6 Hits: Bot ", 6, 3, 9);
        deld0Bot = aida.histogram1D("Delta d0: Bot", 50, -20.0, 20.0);
        delphiBot = aida.histogram1D("Delta sin(phi): Bot", 50, -0.1, 0.1);
        delwBot = aida.histogram1D("Delta curvature: Bot", 50, -0.0002, 0.0002);
        dellambdaBot = aida.histogram1D("Delta slope: Bot", 50, -0.01, 0.01);
        delz0Bot = aida.histogram1D("Delta yTarget: Bot", 50, -2.5, 2.5);

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
        plotterBot.region(7).plot(nHits03Bot);
        plotterBot.region(8).plot(nHits46Bot);
        plotterBot.region(0).plot(fd0Bot, functionStyle);
        plotterBot.region(3).plot(fphi0Bot, functionStyle);
        plotterBot.region(6).plot(fwBot, functionStyle);
        plotterBot.region(1).plot(flambdaBot, functionStyle);
        plotterBot.region(4).plot(fz0Bot, functionStyle);
        plotterBot.show();

        plotterParsBot = pfac.create("Bot Track Pars");
        plotterParsBot.createRegions(2, 4);
        lambdaBotL03 = aida.histogram1D("slope: Bot L0-3", 50, -0.06, 0.0);
        z0BotL03 = aida.histogram1D("y0: Bot L0-3", 50, -2.5, 2.5);
        lambdaVsz0BotL03 = aida.histogram2D("slope vs y0: Bot L0-3", 50, -2.5, 2.5, 50, -0.06, 0.0);
        lambdaBotL46 = aida.histogram1D("slope: Bot L4-6", 50, -0.06, 0.0);
        z0BotL46 = aida.histogram1D("y0: Bot L4-6", 50, -2.5, 2.5);
        lambdaVsz0BotL46 = aida.histogram2D("slope vs yTarget: Bot L4-6", 50, -2.5, 2.5, 50, -0.06, 0.0);
        zTargetBotL03 = aida.histogram1D("yTarget: Bot L0-3", 50, -2.5, 2.5);
        zTargetBotL46 = aida.histogram1D("yTarget: Bot L4-6", 50, -2.5, 2.5);
        plotterParsBot.region(0).plot(lambdaBotL03);
        plotterParsBot.region(4).plot(lambdaBotL46);
        plotterParsBot.region(1).plot(z0BotL03);
        plotterParsBot.region(5).plot(z0BotL46);
        plotterParsBot.region(2).plot(lambdaVsz0BotL03);
        plotterParsBot.region(6).plot(lambdaVsz0BotL46);
        plotterParsBot.region(3).plot(zTargetBotL03);
        plotterParsBot.region(7).plot(zTargetBotL46);
        plotterParsBot.show();

        plotterHinge = pfac.create("Y @ Hinge");
        plotterHinge.createRegions(2, 4);
        delYAtHingeTop = aida.histogram1D("DeltaY at Hinge Top", 50, -1.0, 1.0);
        delYAtHingeBot = aida.histogram1D("DeltaY at Hinge Bottom", 50, -1.0, 1.0);
        yAtHingeL03VsL46Top = aida.histogram2D("Y at Hinge Top L46 vs L03", 50, 0, 20, 50, 0, 20);
        yAtHingeL03VsL46Bot = aida.histogram2D("Y at Hinge Bottom L46 vs L03", 50, -20, 0, 50, -20, 0);
        delYAtHingeVsL03SlopeTop = aida.histogram2D("DeltaY at Hinge vs L03 Slope Top", 50, -1.0, 1.0, 50, 0, 0.06);
        delYAtHingeVsL46SlopeTop = aida.histogram2D("DeltaY at Hinge vs L46 Slope Top", 50, -1.0, 1.0, 50, 0, 0.06);
        delYAtHingeVsL03SlopeBot = aida.histogram2D("DeltaY at Hinge vs L03 Slope Bottom", 50, -1.0, 1.0, 50, -0.060, 0.0);
        delYAtHingeVsL46SlopeBot = aida.histogram2D("DeltaY at Hinge vs L46 Slope Bottom", 50, -1.0, 1.0, 50, -0.06, 0.0);
        plotterHinge.region(0).plot(delYAtHingeTop);
        plotterHinge.region(1).plot(yAtHingeL03VsL46Top);
        plotterHinge.region(2).plot(delYAtHingeVsL03SlopeTop);
        plotterHinge.region(3).plot(delYAtHingeVsL46SlopeTop);

        plotterHinge.region(4).plot(delYAtHingeBot);
        plotterHinge.region(5).plot(yAtHingeL03VsL46Bot);
        plotterHinge.region(6).plot(delYAtHingeVsL03SlopeBot);
        plotterHinge.region(7).plot(delYAtHingeVsL46SlopeBot);
        plotterHinge.show();
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

        if (matchFullTracks)
            if (!event.hasCollection(Track.class, fullTrackCollectionName))
                return;

        List<Track> l0to3tracks = event.get(Track.class, l0to3CollectionName);
        List<Track> l4to6tracks = event.get(Track.class, l4to6CollectionName);
        List<Track> fulltracks = new ArrayList<Track>();
        if (matchFullTracks)
            fulltracks = event.get(Track.class, fullTrackCollectionName);

        List<Track> l0to3tracksTop = splitTrackList(l0to3tracks, true);
        List<Track> l0to3tracksBot = splitTrackList(l0to3tracks, false);
        List<Track> l4to6tracksTop = splitTrackList(l4to6tracks, true);
        List<Track> l4to6tracksBot = splitTrackList(l4to6tracks, false);

        nTracks03Top.fill(l0to3tracksTop.size());
        nTracks03Bot.fill(l0to3tracksBot.size());
        nTracks46Top.fill(l4to6tracksTop.size());
        nTracks46Bot.fill(l4to6tracksBot.size());
//
//        for (Track trk03 : l0to3tracksTop) {
//            nHits03Top.fill(trk03.getTrackerHits().size());
//            TrackState ts = trk03.getTrackStates().get(0);
//            z0TopL03.fill(ts.getZ0());
//            lambdaTopL03.fill(ts.getTanLambda());
//            lambdaVsz0TopL03.fill(ts.getZ0(), ts.getTanLambda());
//        }
//
//        for (Track trk46 : l4to6tracksTop) {
//            nHits46Top.fill(trk46.getTrackerHits().size());
//            TrackState ts = trk46.getTrackStates().get(0);
//            z0TopL46.fill(ts.getZ0());
//            lambdaTopL46.fill(ts.getTanLambda());
//            lambdaVsz0TopL46.fill(ts.getZ0(), ts.getTanLambda());
//
//        }
//        for (Track trk03 : l0to3tracksBot) {
//            nHits03Bot.fill(trk03.getTrackerHits().size());
//            TrackState ts = trk03.getTrackStates().get(0);
//            z0BotL03.fill(ts.getZ0());
//            lambdaBotL03.fill(ts.getTanLambda());
//            lambdaVsz0BotL03.fill(ts.getZ0(), ts.getTanLambda());
//        }
//        for (Track trk46 : l4to6tracksBot) {
//            nHits46Bot.fill(trk46.getTrackerHits().size());
//            TrackState ts = trk46.getTrackStates().get(0);
//            z0BotL46.fill(ts.getZ0());
//            lambdaBotL46.fill(ts.getTanLambda());
//            lambdaVsz0BotL46.fill(ts.getZ0(), ts.getTanLambda());
//        }

        Track matchedTrack = null;
        for (Track trk46 : l4to6tracksTop) {
            TrackState ts46 = trk46.getTrackStates().get(0);
            for (Track trk03 : l0to3tracksTop) {
                if (matchFullTracks) {
                    matchedTrack = checkFullTrack(fulltracks, trk03, trk46);
                    if (matchedTrack == null)
                        continue;
                }
                TrackState ts03 = trk03.getTrackStates().get(0);
                double x0L03 = TrackUtils.getX0(ts03);
                double x0L46 = TrackUtils.getX0(ts46);
                double slL03 = ts03.getTanLambda();
                double slL46 = ts46.getTanLambda();
                double y0L03 = ts03.getZ0();
                double y0L46 = ts46.getZ0();
                double yAtTargetL03 = (targetPosition - x0L03) * slL03 + y0L03;
                double yAtTargetL46 = (targetPosition - x0L46) * slL46 + y0L46;
                double yAtHingeL03 = (414.0 - x0L03) * slL03 + y0L03;
                double yAtHingeL46 = (414.0 - x0L46) * slL46 + y0L46;
                double deltaYAtHinge = yAtHingeL46 - yAtHingeL03;
                double deltaYAtTarget = yAtTargetL46 - yAtTargetL03;
                nHits03Top.fill(trk03.getTrackerHits().size());
                z0TopL03.fill(ts03.getZ0());
                lambdaTopL03.fill(ts03.getTanLambda());
//                lambdaVsz0TopL03.fill(ts03.getZ0(), ts03.getTanLambda());
                lambdaVsz0TopL03.fill(yAtTargetL03, ts03.getTanLambda());

                nHits46Top.fill(trk46.getTrackerHits().size());
                z0TopL46.fill(ts46.getZ0());
                lambdaTopL46.fill(ts46.getTanLambda());
//                lambdaVsz0TopL46.fill(ts46.getZ0(), ts46.getTanLambda());
                lambdaVsz0TopL46.fill(yAtTargetL46, ts46.getTanLambda());
                zTargetTopL03.fill(yAtTargetL03);
                zTargetTopL46.fill(yAtTargetL46);
                deld0Top.fill(ts46.getD0() - ts03.getD0());
                delphiTop.fill(Math.sin(ts46.getPhi()) - Math.sin(ts03.getPhi()));
                delwTop.fill(ts46.getOmega() - ts03.getOmega());
//                delz0Top.fill(ts46.getZ0() - ts03.getZ0());
                delz0Top.fill(deltaYAtTarget);
                dellambdaTop.fill(ts46.getTanLambda() - ts03.getTanLambda());
                delYAtHingeTop.fill(deltaYAtHinge);
                yAtHingeL03VsL46Top.fill(yAtHingeL46, yAtHingeL03);
                delYAtHingeVsL46SlopeTop.fill(deltaYAtHinge, slL46);
                delYAtHingeVsL03SlopeTop.fill(deltaYAtHinge, slL03);

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
                if (matchFullTracks) {
                    matchedTrack = checkFullTrack(fulltracks, trk03, trk46);
                    if (matchedTrack == null)
                        continue;
                }
                TrackState ts03 = trk03.getTrackStates().get(0);
                double x0L03 = TrackUtils.getX0(ts03);
                double x0L46 = TrackUtils.getX0(ts46);
                double slL03 = ts03.getTanLambda();
                double slL46 = ts46.getTanLambda();
                double y0L03 = ts03.getZ0();
                double y0L46 = ts46.getZ0();
                double yAtTargetL03 = (targetPosition - x0L03) * slL03 + y0L03;
                double yAtTargetL46 = (targetPosition - x0L46) * slL46 + y0L46;
                double yAtHingeL03 = (414.0 - x0L03) * slL03 + y0L03;
                double yAtHingeL46 = (414.0 - x0L46) * slL46 + y0L46;
                double deltaYAtHinge = yAtHingeL46 - yAtHingeL03;
                double deltaYAtTarget = yAtTargetL46 - yAtTargetL03;
                nHits03Bot.fill(trk03.getTrackerHits().size());
                z0BotL03.fill(ts03.getZ0());
                lambdaBotL03.fill(ts03.getTanLambda());
//                lambdaVsz0BotL03.fill(ts03.getZ0(), ts03.getTanLambda());
                lambdaVsz0BotL03.fill(yAtTargetL03, ts03.getTanLambda());
                nHits46Bot.fill(trk46.getTrackerHits().size());
                z0BotL46.fill(ts46.getZ0());
                lambdaBotL46.fill(ts46.getTanLambda());
//                lambdaVsz0BotL46.fill(ts46.getZ0(), ts46.getTanLambda());
                lambdaVsz0BotL46.fill(yAtTargetL46, ts46.getTanLambda());

                zTargetBotL03.fill(yAtTargetL03);
                zTargetBotL46.fill(yAtTargetL46);
                deld0Bot.fill(ts46.getD0() - ts03.getD0());
                delphiBot.fill(Math.sin(ts46.getPhi()) - Math.sin(ts03.getPhi()));
                delwBot.fill(ts46.getOmega() - ts03.getOmega());
//                delz0Bot.fill(ts46.getZ0() - ts03.getZ0());
                delz0Bot.fill(deltaYAtTarget);
                dellambdaBot.fill(ts46.getTanLambda() - ts03.getTanLambda());
                delYAtHingeBot.fill(deltaYAtHinge);
                yAtHingeL03VsL46Bot.fill(yAtHingeL46, yAtHingeL03);
                delYAtHingeVsL46SlopeBot.fill(deltaYAtHinge, slL46);
                delYAtHingeVsL03SlopeBot.fill(deltaYAtHinge, slL03);
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

    private Track checkFullTrack(List<Track> fullTracks, Track t03, Track t46) {
        List<TrackerHit> trkHitsL03 = t03.getTrackerHits();
        List<TrackerHit> trkHitsL46 = t46.getTrackerHits();

        for (Track fullTr : fullTracks) {
            List<TrackerHit> trkHitsFull = fullTr.getTrackerHits();
            if (trkHitsFull.containsAll(trkHitsL03) && trkHitsFull.containsAll(trkHitsL46))
                return fullTr;
        }

        return null;
    }
}
