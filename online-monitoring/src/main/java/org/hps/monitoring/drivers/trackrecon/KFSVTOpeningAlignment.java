package org.hps.monitoring.drivers.trackrecon;

import static org.hps.monitoring.drivers.trackrecon.PlotAndFitUtilities.fitAndPutParameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.MaterialSupervisor.SiStripPlane;
import org.hps.recon.tracking.kalman.KalmanInterface;
import org.hps.recon.tracking.kalman.KalmanKinkFit;
import org.hps.recon.tracking.kalman.KalmanParams;
import org.hps.recon.tracking.kalman.Vec;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

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

public class KFSVTOpeningAlignment extends Driver {

    static private AIDA aida = AIDA.defaultInstance();
    
  
    private KalmanInterface KI;
    private static final boolean debug = false;
       
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
    IHistogram1D delz0Top;
    IHistogram1D lambdaTopL03;
    IHistogram1D z0TopL03;
    IHistogram1D lambdaTopL46;
    IHistogram1D z0TopL46;
    IHistogram1D deld0Bot;
    IHistogram1D delphiBot;
    IHistogram1D delwBot;   
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

    private IHistogram1D  hProjAngTop, hAngTop, hChiInTop, hChiOutTop, hDofInTop, hDofOutTop;
    private IHistogram1D  hProjAngBot, hAngBot, hChiInBot, hChiOutBot, hDofInBot, hDofOutBot;
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

    public KFSVTOpeningAlignment() {
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
        deld0Top = aida.histogram1D("Delta d0: Top", 50, -20.0, 20.0);
        delphiTop = aida.histogram1D("Delta sin(phi): Top", 50, -0.1, 0.1);              
        delz0Top = aida.histogram1D("Delta yTarget: Top", 50, -2.5, 2.5);
        
        hChiInTop = aida.histogram1D("Inner helix chi^2",100,0.,100.);
        hDofInTop = aida.histogram1D("Inner helix #dof",10,0.,10.);
        hChiOutTop = aida.histogram1D("Outer helix chi^2",100,0.,100.);
        hDofOutTop = aida.histogram1D("Outer helix #dof",10,0.,10.);

        hAngTop = aida.histogram1D("Kalman kink angle top", 100, 0., 0.15);
        hProjAngTop = aida.histogram1D("Kalman projected kink angle top", 100, -0.03, 0.03);

        fd0Top = functionFactory.createFunctionByName("Gaussian", "G");
        fphi0Top = functionFactory.createFunctionByName("Gaussian", "G");
        fwTop = functionFactory.createFunctionByName("Gaussian", "G");
        flambdaTop = functionFactory.createFunctionByName("Gaussian", "G");
        fz0Top = functionFactory.createFunctionByName("Gaussian", "G");

       
        
        plotterTop.region(0).plot(deld0Top);
        plotterTop.region(3).plot(delphiTop);
        plotterTop.region(6).plot(hProjAngTop);
        plotterTop.region(1).plot(hAngTop);
        plotterTop.region(4).plot(delz0Top);
        plotterTop.region(2).plot(nTracks03Top);
        plotterTop.region(5).plot(nTracks46Top);
        plotterTop.region(7).plot(hDofInTop);
        plotterTop.region(8).plot(hDofOutTop);
        plotterTop.region(0).plot(fd0Top, functionStyle);
        plotterTop.region(3).plot(fphi0Top, functionStyle);
        plotterTop.region(6).plot(fwTop, functionStyle);
        plotterTop.region(1).plot(flambdaTop, functionStyle);
        plotterTop.region(4).plot(fz0Top, functionStyle);
        plotterTop.show();

        
//          plotterParsTop = pfac.create("Top Track Pars");
//          plotterParsTop.createRegions(2, 4); lambdaTopL03 =
//          aida.histogram1D("slope: Top L0-3", 50, 0, 0.06); z0TopL03 =
//          aida.histogram1D("y0: Top L0-3", 50, -2.5, 2.5); lambdaVsz0TopL03 =
//          aida.histogram2D("slope vs y0: Top L0-3", 50, -2.5, 2.5, 50, 0.0, 0.06);
//          lambdaTopL46 = aida.histogram1D("slope: Top L4-6", 50, 0, 0.06); z0TopL46 =
//          aida.histogram1D("y0: Top L4-6", 50, -2.5, 2.5); lambdaVsz0TopL46 =
//          aida.histogram2D("slope vs yTarget: Top L4-6", 50, -2.5, 2.5, 50, 0.0, 0.06);
//          zTargetTopL03 = aida.histogram1D("yTarget: Top L0-3", 50, -2.5, 2.5);
//          zTargetTopL46 = aida.histogram1D("yTarget: Top L4-6", 50, -2.5, 2.5);
//          
//          plotterParsTop.region(0).plot(lambdaTopL03);
//          plotterParsTop.region(4).plot(lambdaTopL46);
//          plotterParsTop.region(1).plot(z0TopL03);
//          plotterParsTop.region(5).plot(z0TopL46);
//          plotterParsTop.region(2).plot(lambdaVsz0TopL03);
//          plotterParsTop.region(6).plot(lambdaVsz0TopL46);
//          plotterParsTop.region(3).plot(zTargetTopL03);
//          plotterParsTop.region(7).plot(zTargetTopL46); plotterParsTop.show();
         
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
        delz0Bot = aida.histogram1D("Delta yTarget: Bot", 50, -2.5, 2.5);       
        hAngBot = aida.histogram1D("Kalman kink angle bottom", 100, 0., 0.15);
        hProjAngBot = aida.histogram1D("Kalman projected kink angle bottom", 100, -0.03, 0.03);
        
        fd0Bot = functionFactory.createFunctionByName("Gaussian", "G");
        fphi0Bot = functionFactory.createFunctionByName("Gaussian", "G");
        fwBot = functionFactory.createFunctionByName("Gaussian", "G");
        flambdaBot = functionFactory.createFunctionByName("Gaussian", "G");
        fz0Bot = functionFactory.createFunctionByName("Gaussian", "G");

        plotterBot.region(0).plot(deld0Bot);
        plotterBot.region(3).plot(delphiBot);
        plotterBot.region(6).plot(hProjAngBot);
        plotterBot.region(1).plot(hAngBot);
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

//        plotterParsBot = pfac.create("Bot Track Pars");
//        plotterParsBot.createRegions(2, 4);
//        lambdaBotL03 = aida.histogram1D("slope: Bot L0-3", 50, -0.06, 0.0);
//        z0BotL03 = aida.histogram1D("y0: Bot L0-3", 50, -2.5, 2.5);
//        lambdaVsz0BotL03 = aida.histogram2D("slope vs y0: Bot L0-3", 50, -2.5, 2.5, 50, -0.06, 0.0);
//        lambdaBotL46 = aida.histogram1D("slope: Bot L4-6", 50, -0.06, 0.0);
//        z0BotL46 = aida.histogram1D("y0: Bot L4-6", 50, -2.5, 2.5);
//        lambdaVsz0BotL46 = aida.histogram2D("slope vs yTarget: Bot L4-6", 50, -2.5, 2.5, 50, -0.06, 0.0);
//        zTargetBotL03 = aida.histogram1D("yTarget: Bot L0-3", 50, -2.5, 2.5);
//        zTargetBotL46 = aida.histogram1D("yTarget: Bot L4-6", 50, -2.5, 2.5);
//        plotterParsBot.region(0).plot(lambdaBotL03);
//        plotterParsBot.region(4).plot(lambdaBotL46);
//        plotterParsBot.region(1).plot(z0BotL03);
//        plotterParsBot.region(5).plot(z0BotL46);
//        plotterParsBot.region(2).plot(lambdaVsz0BotL03);
//        plotterParsBot.region(6).plot(lambdaVsz0BotL46);
//        plotterParsBot.region(3).plot(zTargetBotL03);
//        plotterParsBot.region(7).plot(zTargetBotL46);
//        plotterParsBot.show();
//
//        plotterHinge = pfac.create("Y @ Hinge");
//        plotterHinge.createRegions(2, 4);
//        delYAtHingeTop = aida.histogram1D("DeltaY at Hinge Top", 50, -1.0, 1.0);
//        delYAtHingeBot = aida.histogram1D("DeltaY at Hinge Bottom", 50, -1.0, 1.0);
//        yAtHingeL03VsL46Top = aida.histogram2D("Y at Hinge Top L46 vs L03", 50, 0, 20, 50, 0, 20);
//        yAtHingeL03VsL46Bot = aida.histogram2D("Y at Hinge Bottom L46 vs L03", 50, -20, 0, 50, -20, 0);
//        delYAtHingeVsL03SlopeTop = aida.histogram2D("DeltaY at Hinge vs L03 Slope Top", 50, -1.0, 1.0, 50, 0, 0.06);
//        delYAtHingeVsL46SlopeTop = aida.histogram2D("DeltaY at Hinge vs L46 Slope Top", 50, -1.0, 1.0, 50, 0, 0.06);
//        delYAtHingeVsL03SlopeBot = aida.histogram2D("DeltaY at Hinge vs L03 Slope Bottom", 50, -1.0, 1.0, 50, -0.060, 0.0);
//        delYAtHingeVsL46SlopeBot = aida.histogram2D("DeltaY at Hinge vs L46 Slope Bottom", 50, -1.0, 1.0, 50, -0.06, 0.0);
//        plotterHinge.region(0).plot(delYAtHingeTop);
//        plotterHinge.region(1).plot(yAtHingeL03VsL46Top);
//        plotterHinge.region(2).plot(delYAtHingeVsL03SlopeTop);
//        plotterHinge.region(3).plot(delYAtHingeVsL46SlopeTop);
//
//        plotterHinge.region(4).plot(delYAtHingeBot);
//        plotterHinge.region(5).plot(yAtHingeL03VsL46Bot);
//        plotterHinge.region(6).plot(delYAtHingeVsL03SlopeBot);
//        plotterHinge.region(7).plot(delYAtHingeVsL46SlopeBot);
//        plotterHinge.show();
    }

    @Override
    public void process(EventHeader event) {
        aida.tree().cd("/");
        
        String stripDataRelationsInputCollectionName = "KFGBLStripClusterDataRelations";
        if (!event.hasCollection(LCRelation.class, stripDataRelationsInputCollectionName)) {
            System.out.format("\nKalmanKinkFitDriver: the data collection %s is missing.\n",stripDataRelationsInputCollectionName);
        }
        String trackCollectionName = "KalmanFullTracks";
        if (event.hasCollection(Track.class, trackCollectionName)) {
            List<Track> kalmanFullTracks = event.get(Track.class, trackCollectionName);
            for (Track trk : kalmanFullTracks) {
                if (trk.getNDF() >= 7) {                    
                    KalmanKinkFit knkFt = new KalmanKinkFit(event, KI, trk);
                    if (knkFt.doFits()) {
                        if (knkFt.innerMomentum()[2] < 0.) {
                            hProjAngBot.fill(knkFt.projectedAngle());
                            hAngBot.fill(knkFt.scatteringAngle());
                            hChiInBot.fill(knkFt.innerChi2());
                            hDofInBot.fill(knkFt.innerDOF());
                            hChiOutBot.fill(knkFt.outerChi2());
                            hDofOutBot.fill(knkFt.outerDOF());  
                        } else {
                            hProjAngTop.fill(knkFt.projectedAngle());
                            hAngTop.fill(knkFt.scatteringAngle());
                            hChiInTop.fill(knkFt.innerChi2());
                            hDofInTop.fill(knkFt.innerDOF());
                            hChiOutTop.fill(knkFt.outerChi2());
                            hDofOutTop.fill(knkFt.outerDOF());  
                        }
                            
                        if (debug) {
                            System.out.format("KinkFit: event number %d\n", event.getEventNumber());
                            System.out.format("  KinkFit: chi^2 of inner helix = %8.4f with %d dof\n", knkFt.innerChi2(), knkFt.innerDOF());
                            System.out.format("  KinkFit: chi^2 of outer helix = %8.4f with %d dof\n", knkFt.outerChi2(), knkFt.outerDOF());
                            Vec inHx = new Vec(5,knkFt.innerHelix());
                            Vec outHx = new Vec(5,knkFt.outerHelix());
                            System.out.format("  KinkFit: inner helix = %s\n", inHx.toString());
                            System.out.format("  KinkFit: outer helix = %s\n", outHx.toString());
                            Vec inP = new Vec(3,knkFt.innerMomentum());
                            Vec outP = new Vec(3,knkFt.outerMomentum());
                            System.out.format("  KinkFit: inner momentum = %s\n", inP.toString());
                            System.out.format("  KinkFit: outer momentum = %s\n", outP.toString());
                            System.out.format("  KinkFit: scattering angle = %10.6f radians\n", knkFt.scatteringAngle());
                            System.out.format("  KinkFit: projected scattering angle = %10.6f radians\n", knkFt.projectedAngle());
                        }
                    }
                }
            }
        }
        KI.clearInterface();
        
      

//        // IFunction currentFitFunction = performGaussianFit(deld0Bot, fd0Bot, jminChisq).fittedFunction();;
//        // fd0Bot.setParameters(currentFitFunction.parameters());
//        fitAndPutParameters(deld0Bot, fd0Bot);
//        fitAndPutParameters(delphiBot, fphi0Bot);
//        fitAndPutParameters(delwBot, fwBot);
//        fitAndPutParameters(delz0Bot, fz0Bot);
//        fitAndPutParameters(dellambdaBot, flambdaBot);

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
