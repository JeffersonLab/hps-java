package org.lcsim.hps.monitoring.svt;

import hep.aida.*;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.Hep3Vector;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.*;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.hps.event.BeamlineConstants;
import org.lcsim.hps.monitoring.deprecated.AIDAFrame;
import org.lcsim.hps.monitoring.deprecated.Resettable;
import org.lcsim.hps.recon.ecal.HPSEcalCluster;
import org.lcsim.hps.recon.vertexing.HelixConverter;
import org.lcsim.hps.recon.vertexing.StraightLineTrack;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.event.LCIOParameters.ParameterName;
import org.lcsim.fit.helicaltrack.*;
import org.lcsim.hps.recon.tracking.*;
import org.lcsim.hps.recon.tracking.HPSSVTCalibrationConstants.ChannelConstants;

/**
 *
 * @author mgraham
 */
public class TrackingReconstructionPlots extends Driver implements Resettable {

    //private AIDAFrame plotterFrame;
    //private AIDAFrame topFrame;
    //private AIDAFrame bottomFrame;
    private AIDA aida = AIDA.defaultInstance();
    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    private String fittedTrackerHitCollectionName = "SVTFittedRawTrackerHits";
    private String trackerHitCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private String helicalTrackHitCollectionName = "HelicalTrackHits";
    private String rotatedTrackHitCollectionName = "RotatedHelicalTrackHits";
    private String helicalTrackHitRelationsCollectionName = "HelicalTrackHitRelations";
    private String trackCollectionName = "MatchedTracks";
    private String trackerName = "Tracker";
    String ecalSubdetectorName = "Ecal";
    String ecalCollectionName = "EcalClusters";
    private Detector detector = null;
    IDDecoder dec;
    private int eventCount;
    private List<SiSensor> sensors;
    private String outputPlots = null;
    IPlotter plotter;
    IPlotter plotter2;
    IPlotter plotter22;
    IPlotter plotter222;
    IPlotter plotter3;
    IPlotter plotter3_1;
    IPlotter plotter3_2;
    IPlotter plotter4;
    IPlotter plotter5;
    IPlotter plotter5_1;
    IPlotter plotter55;
    IPlotter plotter6;
    IPlotter plotter7;
    IPlotter top1;
    IPlotter top2;
    IPlotter top3;
    IPlotter top4;
    IPlotter bot1;
    IPlotter bot2;
    IPlotter bot3;
    IPlotter bot4;
    double zEcal = 1500;
    double zAtDownStrPairSpec = 914.0; //mm
    double zAtColl = -1500;
    IHistogram1D trkPx;
    IHistogram1D nTracks;
    HPSShaperFitAlgorithm _shaper = new DumbShaperFit();

    @Override
    protected void detectorChanged(Detector detector) {
        this.detector = detector;
        aida.tree().cd("/");
        //plotterFrame = new AIDAFrame();
        //plotterFrame.setTitle("HPS Tracking Plots");

        //topFrame = new AIDAFrame();
        //topFrame.setTitle("Top Tracking Plots");
        //bottomFrame = new AIDAFrame();
        //bottomFrame.setTitle("Bottom Tracking Plots");

        sensors = detector.getSubdetector(trackerName).getDetectorElement().findDescendants(SiSensor.class);

        IAnalysisFactory fac = aida.analysisFactory();
        plotter = fac.createPlotterFactory().create("HPS Tracking Plots");
        plotter.setTitle("Momentum");
        IPlotterStyle style = plotter.style();
        style.dataStyle().fillStyle().setColor("yellow");
        style.dataStyle().errorBarStyle().setVisible(false);
        plotter.createRegions(2, 2);
        //plotterFrame.addPlotter(plotter);

        trkPx = aida.histogram1D("Track Momentum (Px)", 25, -0.25, 0.25);
        IHistogram1D trkPy = aida.histogram1D("Track Momentum (Py)", 25, -0.1, 0.1);
        IHistogram1D trkPz = aida.histogram1D("Track Momentum (Pz)", 25, 0, 3.5);
        IHistogram1D trkChi2 = aida.histogram1D("Track Chi2", 25, 0, 25.0);

        plotter.region(0).plot(trkPx);
        plotter.region(1).plot(trkPy);
        plotter.region(2).plot(trkPz);
        plotter.region(3).plot(trkChi2);
        
        plotter.show();

//   ******************************************************************


        top1 = fac.createPlotterFactory().create("Top Tracking Plots");
        top1.setTitle("Top Momentum");
        IPlotterStyle stop1 = top1.style();
        stop1.dataStyle().fillStyle().setColor("green");
        stop1.dataStyle().errorBarStyle().setVisible(false);
        top1.createRegions(2, 2);
        //topFrame.addPlotter(top1);

        IHistogram1D toptrkPx = aida.histogram1D("Top Track Momentum (Px)", 25, -0.25, 0.25);
        IHistogram1D toptrkPy = aida.histogram1D("Top Track Momentum (Py)", 25, -0.1, 0.1);
        IHistogram1D toptrkPz = aida.histogram1D("Top Track Momentum (Pz)", 25, 0, 3.5);
        IHistogram1D toptrkChi2 = aida.histogram1D("Top Track Chi2", 25, 0, 25.0);

        top1.region(0).plot(toptrkPx);
        top1.region(1).plot(toptrkPy);
        top1.region(2).plot(toptrkPz);
        top1.region(3).plot(toptrkChi2);
        
        top1.show();


        bot1 = fac.createPlotterFactory().create("Bottom Tracking Plots");
        bot1.setTitle("Bottom Momentum");
        IPlotterStyle sbot1 = bot1.style();
        sbot1.dataStyle().fillStyle().setColor("blue");
        sbot1.dataStyle().errorBarStyle().setVisible(false);
        bot1.createRegions(2, 2);
        //bottomFrame.addPlotter(bot1);

        IHistogram1D bottrkPx = aida.histogram1D("Bottom Track Momentum (Px)", 25, -0.25, 0.25);
        IHistogram1D bottrkPy = aida.histogram1D("Bottom Track Momentum (Py)", 25, -0.1, 0.1);
        IHistogram1D bottrkPz = aida.histogram1D("Bottom Track Momentum (Pz)", 25, 0, 3.5);
        IHistogram1D bottrkChi2 = aida.histogram1D("Bottom Track Chi2", 25, 0, 25.0);

        bot1.region(0).plot(bottrkPx);
        bot1.region(1).plot(bottrkPy);
        bot1.region(2).plot(bottrkPz);
        bot1.region(3).plot(bottrkChi2);

        bot1.show();
        
//   ******************************************************************

        IHistogram1D trkd0 = aida.histogram1D("d0 ", 25, -100.0, 100.0);
        IHistogram1D trkphi = aida.histogram1D("sinphi ", 25, -0.2, 0.2);
        IHistogram1D trkomega = aida.histogram1D("omega ", 25, -0.0025, 0.0025);
        IHistogram1D trklam = aida.histogram1D("tan(lambda) ", 25, -0.1, 0.1);
        IHistogram1D trkz0 = aida.histogram1D("z0 ", 25, -100.0, 100.0);

        plotter22 = fac.createPlotterFactory().create("HPS Track Params");
        plotter22.setTitle("Track parameters");
        //plotterFrame.addPlotter(plotter22);
        IPlotterStyle style22 = plotter22.style();
        style22.dataStyle().fillStyle().setColor("yellow");
        style22.dataStyle().errorBarStyle().setVisible(false);
        plotter22.createRegions(2, 3);
        plotter22.region(0).plot(trkd0);
        plotter22.region(1).plot(trkphi);
        plotter22.region(2).plot(trkomega);
        plotter22.region(3).plot(trklam);
        plotter22.region(4).plot(trkz0);


        plotter2 = fac.createPlotterFactory().create("HPS Tracking Plots");
        plotter2.setTitle("Track extrapolation");
        //plotterFrame.addPlotter(plotter2);
        IPlotterStyle style2 = plotter2.style();
        style2.dataStyle().fillStyle().setColor("yellow");
        style2.dataStyle().errorBarStyle().setVisible(false);
        plotter2.createRegions(2, 4);
        IHistogram1D xAtConverter = aida.histogram1D("X (mm) @ Z=-60cm", 50, -50, 50);
        IHistogram1D yAtConverter = aida.histogram1D("Y (mm) @ Z=-60cm", 50, -20, 20);
        IHistogram1D xAtColl = aida.histogram1D("X (mm) @ Z=-150cm", 50, -200, 200);
        IHistogram1D yAtColl = aida.histogram1D("Y (mm) @ Z=-150cm", 50, -200, 200);
        IHistogram1D xAtEcal = aida.histogram1D("X (mm) @ ECAL", 50, -500, 500);
        IHistogram1D yAtEcal = aida.histogram1D("Y (mm) @ ECAL", 50, -100, 100);
        IHistogram1D xAtEcal2 = aida.histogram1D("X (mm) @ ECAL (Pz>1)", 50, -500, 500);
        IHistogram1D yAtEcal2 = aida.histogram1D("Y (mm) @ ECAL (Pz>1)", 50, -100, 100);

        plotter2.region(0).plot(xAtConverter);
        plotter2.region(4).plot(yAtConverter);
        plotter2.region(1).plot(xAtColl);
        plotter2.region(5).plot(yAtColl);
        plotter2.region(2).plot(xAtEcal);
        plotter2.region(6).plot(yAtEcal);
        plotter2.region(3).plot(xAtEcal2);
        plotter2.region(7).plot(yAtEcal2);

        plotter222 = fac.createPlotterFactory().create("HPS Tracking Plots");
        plotter222.setTitle("Other");
        //plotterFrame.addPlotter(plotter222);
        IPlotterStyle style222 = plotter222.style();
        style222.dataStyle().fillStyle().setColor("yellow");
        style222.dataStyle().errorBarStyle().setVisible(false);
        plotter222.createRegions(2, 3);

        IHistogram1D nHits = aida.histogram1D("Hits per Track", 2, 4, 6);
        IHistogram1D amp = aida.histogram1D("Amp (HitOnTrack)", 50, 0, 5000);
        IHistogram1D ampcl = aida.histogram1D("Amp (CluOnTrack)", 50, 0, 5000);
        IHistogram1D amp2 = aida.histogram1D("Amp Pz>1000 (HitOnTrack)", 50, 0, 5000);
        IHistogram1D ampcl2 = aida.histogram1D("Amp Pz>1000 (CluOnTrack)", 50, 0, 5000);
        nTracks = aida.histogram1D("Tracks per Event", 3, 0, 3);

        plotter222.region(0).plot(nHits);
        plotter222.region(3).plot(nTracks);
        plotter222.region(1).plot(amp);
        plotter222.region(4).plot(amp2);
        plotter222.region(2).plot(ampcl);
        plotter222.region(5).plot(ampcl2);


        plotter3 = fac.createPlotterFactory().create("HPS Residual Plots");
        plotter3.setTitle("Residuals");
        //plotterFrame.addPlotter(plotter3);
        IPlotterStyle style3 = plotter3.style();
        style3.dataStyle().fillStyle().setColor("yellow");
        style3.dataStyle().errorBarStyle().setVisible(false);
        plotter3.createRegions(5, 2);

        double minResidY = -1.5;
        double maxResidY = 1.5;

        double minResidX = -5;
        double maxResidX = 5;

        IHistogram1D mod1ResX = aida.histogram1D("Module 1 Residual X(mm)", 25, minResidX, maxResidX);
        IHistogram1D mod1ResY = aida.histogram1D("Module 1 Residual Y(mm)", 25, minResidY, maxResidY);

        IHistogram1D mod2ResX = aida.histogram1D("Module 2 Residual X(mm)", 25, minResidX, maxResidX);
        IHistogram1D mod2ResY = aida.histogram1D("Module 2 Residual Y(mm)", 25, minResidY, maxResidY);

        IHistogram1D mod3ResX = aida.histogram1D("Module 3 Residual X(mm)", 25, minResidX, maxResidX);
        IHistogram1D mod3ResY = aida.histogram1D("Module 3 Residual Y(mm)", 25, minResidY, maxResidY);

        IHistogram1D mod4ResX = aida.histogram1D("Module 4 Residual X(mm)", 25, minResidX, maxResidX);
        IHistogram1D mod4ResY = aida.histogram1D("Module 4 Residual Y(mm)", 25, minResidY, maxResidY);

        IHistogram1D mod5ResX = aida.histogram1D("Module 5 Residual X(mm)", 25, minResidX, maxResidX);
        IHistogram1D mod5ResY = aida.histogram1D("Module 5 Residual Y(mm)", 25, minResidY, maxResidY);

        plotter3.region(0).plot(mod1ResX);
        plotter3.region(2).plot(mod2ResX);
        plotter3.region(4).plot(mod3ResX);
        plotter3.region(6).plot(mod4ResX);
        plotter3.region(8).plot(mod5ResX);

        plotter3.region(1).plot(mod1ResY);
        plotter3.region(3).plot(mod2ResY);
        plotter3.region(5).plot(mod3ResY);
        plotter3.region(7).plot(mod4ResY);
        plotter3.region(9).plot(mod5ResY);


        plotter3_1 = fac.createPlotterFactory().create("HPS Residual Plots (Single hit per layer)");
        plotter3_1.setTitle("Residuals (Top)");
        //plotterFrame.addPlotter(plotter3_1);
        IPlotterStyle style3_1 = plotter3_1.style();
        style3_1.dataStyle().fillStyle().setColor("yellow");
        style3_1.dataStyle().errorBarStyle().setVisible(false);
        plotter3_1.createRegions(5, 2);

        IHistogram1D mod1ResX_Top = aida.histogram1D("Module 1 Residual X(mm) Top", 25, minResidX, maxResidX);
        IHistogram1D mod1ResY_Top = aida.histogram1D("Module 1 Residual Y(mm) Top", 25, minResidY, maxResidY);

        IHistogram1D mod2ResX_Top = aida.histogram1D("Module 2 Residual X(mm) Top", 25, minResidX, maxResidX);
        IHistogram1D mod2ResY_Top = aida.histogram1D("Module 2 Residual Y(mm) Top", 25, minResidY, maxResidY);

        IHistogram1D mod3ResX_Top = aida.histogram1D("Module 3 Residual X(mm) Top", 25, minResidX, maxResidX);
        IHistogram1D mod3ResY_Top = aida.histogram1D("Module 3 Residual Y(mm) Top", 25, minResidY, maxResidY);

        IHistogram1D mod4ResX_Top = aida.histogram1D("Module 4 Residual X(mm) Top", 25, minResidX, maxResidX);
        IHistogram1D mod4ResY_Top = aida.histogram1D("Module 4 Residual Y(mm) Top", 25, minResidY, maxResidY);

        IHistogram1D mod5ResX_Top = aida.histogram1D("Module 5 Residual X(mm) Top", 25, minResidX, maxResidX);
        IHistogram1D mod5ResY_Top = aida.histogram1D("Module 5 Residual Y(mm) Top", 25, minResidY, maxResidY);

        plotter3_1.region(0).plot(mod1ResX_Top);
        plotter3_1.region(2).plot(mod2ResX_Top);
        plotter3_1.region(4).plot(mod3ResX_Top);
        plotter3_1.region(6).plot(mod4ResX_Top);
        plotter3_1.region(8).plot(mod5ResX_Top);

        plotter3_1.region(1).plot(mod1ResY_Top);
        plotter3_1.region(3).plot(mod2ResY_Top);
        plotter3_1.region(5).plot(mod3ResY_Top);
        plotter3_1.region(7).plot(mod4ResY_Top);
        plotter3_1.region(9).plot(mod5ResY_Top);


        plotter3_2 = fac.createPlotterFactory().create("HPS Residual Plots (Single strip cluster per layer)");
        plotter3_2.setTitle("Residuals (Bottom)");
        //plotterFrame.addPlotter(plotter3_2);
        IPlotterStyle style3_2 = plotter3_2.style();
        style3_2.dataStyle().fillStyle().setColor("yellow");
        style3_2.dataStyle().errorBarStyle().setVisible(false);
        plotter3_2.createRegions(5, 2);

        IHistogram1D mod1ResX_Bottom = aida.histogram1D("Module 1 Residual X(mm) Bottom", 25, minResidX, maxResidX);
        IHistogram1D mod1ResY_Bottom = aida.histogram1D("Module 1 Residual Y(mm) Bottom", 25, minResidY, maxResidY);

        IHistogram1D mod2ResX_Bottom = aida.histogram1D("Module 2 Residual X(mm) Bottom", 25, minResidX, maxResidX);
        IHistogram1D mod2ResY_Bottom = aida.histogram1D("Module 2 Residual Y(mm) Bottom", 25, minResidY, maxResidY);

        IHistogram1D mod3ResX_Bottom = aida.histogram1D("Module 3 Residual X(mm) Bottom", 25, minResidX, maxResidX);
        IHistogram1D mod3ResY_Bottom = aida.histogram1D("Module 3 Residual Y(mm) Bottom", 25, minResidY, maxResidY);

        IHistogram1D mod4ResX_Bottom = aida.histogram1D("Module 4 Residual X(mm) Bottom", 25, minResidX, maxResidX);
        IHistogram1D mod4ResY_Bottom = aida.histogram1D("Module 4 Residual Y(mm) Bottom", 25, minResidY, maxResidY);

        IHistogram1D mod5ResX_Bottom = aida.histogram1D("Module 5 Residual X(mm) Bottom", 25, minResidX, maxResidX);
        IHistogram1D mod5ResY_Bottom = aida.histogram1D("Module 5 Residual Y(mm) Bottom", 25, minResidY, maxResidY);

        plotter3_2.region(0).plot(mod1ResX_Bottom);
        plotter3_2.region(2).plot(mod2ResX_Bottom);
        plotter3_2.region(4).plot(mod3ResX_Bottom);
        plotter3_2.region(6).plot(mod4ResX_Bottom);
        plotter3_2.region(8).plot(mod5ResX_Bottom);

        plotter3_2.region(1).plot(mod1ResY_Bottom);
        plotter3_2.region(3).plot(mod2ResY_Bottom);
        plotter3_2.region(5).plot(mod3ResY_Bottom);
        plotter3_2.region(7).plot(mod4ResY_Bottom);
        plotter3_2.region(9).plot(mod5ResY_Bottom);




        plotter4 = fac.createPlotterFactory().create("HPS Track and ECal Plots");
        plotter4.setTitle("Track and ECal Correlations");
        //plotterFrame.addPlotter(plotter4);
        IPlotterStyle style4 = plotter4.style();
        style4.setParameter("hist2DStyle", "colorMap");
        style4.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        style4.dataStyle().fillStyle().setColor("yellow");
        style4.dataStyle().errorBarStyle().setVisible(false);
        plotter4.createRegions(2, 3);

        IHistogram2D eVsP = aida.histogram2D("Energy Vs Momentum", 50, 0, 500, 50, 0, 3000);
        IHistogram1D eOverP = aida.histogram1D("Energy Over Momentum", 50, 0, 2);

        IHistogram1D distX = aida.histogram1D("deltaX", 50, -400, 400);
        IHistogram1D distY = aida.histogram1D("deltaY", 50, -40, 40);

//        IHistogram1D distX2 = aida.histogram1D("deltaX (Pz>1)", 50, -400, 400);
//        IHistogram1D distY2 = aida.histogram1D("deltaY (Pz>1)", 50, -40, 40);

        IHistogram2D xEcalVsTrk = aida.histogram2D("X ECal Vs Track", 100, -400, 400, 100, -400, 400);
        IHistogram2D yEcalVsTrk = aida.histogram2D("Y ECal Vs Track", 100, -100, 100, 100, -100, 100);

        plotter4.region(0).plot(eVsP);
        plotter4.region(3).plot(eOverP);
        plotter4.region(1).plot(distX);
        plotter4.region(4).plot(distY);
        plotter4.region(2).plot(xEcalVsTrk);
        plotter4.region(5).plot(yEcalVsTrk);


        //   ******************************************************************


        top2 = fac.createPlotterFactory().create("Top ECal Plots");
        top2.setTitle("Top ECal Correlations");
        IPlotterStyle stop2 = top2.style();
        stop2.dataStyle().fillStyle().setColor("green");
        stop2.dataStyle().errorBarStyle().setVisible(false);
        stop2.setParameter("hist2DStyle", "colorMap");
        stop2.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        top2.createRegions(2, 3);
        //topFrame.addPlotter(top2);

        IHistogram2D topeVsP = aida.histogram2D("Top Energy Vs Momentum", 50, 0, 500, 50, 0, 3000);
        IHistogram1D topeOverP = aida.histogram1D("Top Energy Over Momentum", 50, 0, 2);

        IHistogram1D topdistX = aida.histogram1D("Top deltaX", 50, -400, 400);
        IHistogram1D topdistY = aida.histogram1D("Top deltaY", 50, -40, 40);
        

        IHistogram2D topxEcalVsTrk = aida.histogram2D("Top X ECal Vs Track", 100, -400, 400, 100, -400, 400);
        IHistogram2D topyEcalVsTrk = aida.histogram2D("Top Y ECal Vs Track", 100, 0, 100, 100, 0, 100);


        top2.region(0).plot(topeVsP);
        top2.region(3).plot(topeOverP);
        top2.region(1).plot(topdistX);
        top2.region(4).plot(topdistY);
        top2.region(2).plot(topxEcalVsTrk);
        top2.region(5).plot(topyEcalVsTrk);


        bot2 = fac.createPlotterFactory().create("Bottom ECal Plots");
        bot2.setTitle("Bottom ECal Correlations");
        IPlotterStyle sbot2 = bot2.style();
        sbot2.dataStyle().fillStyle().setColor("green");
        sbot2.dataStyle().errorBarStyle().setVisible(false);
        sbot2.setParameter("hist2DStyle", "colorMap");
        sbot2.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        bot2.createRegions(2, 3);
        //bottomFrame.addPlotter(bot2);

        IHistogram2D BottomeVsP = aida.histogram2D("Bottom Energy Vs Momentum", 50, 0, 500, 50, 0, 3000);
        IHistogram1D BottomeOverP = aida.histogram1D("Bottom Energy Over Momentum", 50, 0, 2);

        IHistogram1D BottomdistX = aida.histogram1D("Bottom deltaX", 50, -400, 400);
        IHistogram1D BottomdistY = aida.histogram1D("Bottom deltaY", 50, -40, 40);
        
        
        IHistogram2D BottomxEcalVsTrk = aida.histogram2D("Bottom X ECal Vs Track", 100, -400, 400, 100, -400, 400);
        IHistogram2D BottomyEcalVsTrk = aida.histogram2D("Bottom Y ECal Vs Track", 100, -100, 0, 100, -100, 0);


        bot2.region(0).plot(BottomeVsP);
        bot2.region(3).plot(BottomeOverP);
        bot2.region(1).plot(BottomdistX);
        bot2.region(4).plot(BottomdistY);
        bot2.region(2).plot(BottomxEcalVsTrk);
        bot2.region(5).plot(BottomyEcalVsTrk);

        
//   ******************************************************************

        top3 = fac.createPlotterFactory().create("Top ECal Plots");
        top3.setTitle("Top ECal More Correlations");
        IPlotterStyle stop3 = top3.style();
        stop3.dataStyle().fillStyle().setColor("green");
        stop3.dataStyle().errorBarStyle().setVisible(false);
        stop3.setParameter("hist2DStyle", "colorMap");
        stop3.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        top3.createRegions(1, 2);
        //topFrame.addPlotter(top3);
        
        IHistogram2D topdistXvsX = aida.histogram2D("Top deltaX vs X", 51,-400,400, 25, -400, 400);
        IHistogram2D topdistYvsY = aida.histogram2D("Top deltaY vs Y", 51,0,100, 25, -40, 40);
        
        top3.region(0).plot(topdistXvsX);
        top3.region(1).plot(topdistYvsY);
        
        
        bot3 = fac.createPlotterFactory().create("Bottom ECal Plots");
        bot3.setTitle("Bottom ECal More Correlations");
        IPlotterStyle sbot3 = bot3.style();
        sbot3.dataStyle().fillStyle().setColor("green");
        sbot3.dataStyle().errorBarStyle().setVisible(false);
        sbot3.setParameter("hist2DStyle", "colorMap");
        sbot3.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        bot3.createRegions(1, 2);
        //bottomFrame.addPlotter(bot3);
        
        
        IHistogram2D botdistXvsX = aida.histogram2D("Bottom deltaX vs X", 51,-400,400, 25, -400, 400);
        IHistogram2D botdistYvsY = aida.histogram2D("Bottom deltaY vs Y", 51,-100,0, 25, -40, 40);
        
        
        bot3.region(0).plot(botdistXvsX);
        bot3.region(1).plot(botdistYvsY);
        
        
//   ******************************************************************


        plotter5 = fac.createPlotterFactory().create("HPS Hit Positions");
        plotter5.setTitle("Hit Positions:  Top");
        //plotterFrame.addPlotter(plotter5);
        IPlotterStyle style5 = plotter5.style();
        style5.setParameter("hist2DStyle", "colorMap");
        style5.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        style5.dataStyle().fillStyle().setColor("yellow");
        style5.dataStyle().errorBarStyle().setVisible(false);
        plotter5.createRegions(1, 2);

        IHistogram1D charge = aida.histogram1D("Charge", 3, -1, 1);

        IHistogram2D l1Pos = aida.histogram2D("Layer 1 HTH Position:  Top", 50, -55, 55, 55, -25, 25);
        IHistogram2D l7Pos = aida.histogram2D("Layer 7 HTH Position:  Top", 50, -55, 55, 55, -25, 25);

        plotter5.region(0).plot(l1Pos);
        plotter5.region(1).plot(l7Pos);

        plotter5_1 = fac.createPlotterFactory().create("HPS Hit Positions");
        plotter5_1.setTitle("Hit Positions:  Bottom");
        //plotterFrame.addPlotter(plotter5_1);
        IPlotterStyle style5_1 = plotter5_1.style();
        style5_1.setParameter("hist2DStyle", "colorMap");
        style5_1.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        style5_1.dataStyle().fillStyle().setColor("yellow");
        style5_1.dataStyle().errorBarStyle().setVisible(false);
        plotter5_1.createRegions(1, 2);

        IHistogram2D l1PosBot = aida.histogram2D("Layer 1 HTH Position:  Bottom", 50, -55, 55, 55, -25, 25);
        IHistogram2D l7PosBot = aida.histogram2D("Layer 7 HTH Position:  Bottom", 50, -55, 55, 55, -25, 25);
        plotter5_1.region(0).plot(l1PosBot);
        plotter5_1.region(1).plot(l7PosBot);

        plotter55 = fac.createPlotterFactory().create("HPS Hit Positions");
        plotter55.setTitle("Helical Track Hits");
        //plotterFrame.addPlotter(plotter55);
        IPlotterStyle style55 = plotter55.style();
        style55.dataStyle().fillStyle().setColor("Green");
        style55.dataStyle().errorBarStyle().setVisible(false);
        style55.dataStyle().markerStyle().setSize(20);
        plotter55.createRegions(1, 2);

        IProfile avgLayersTopPlot = aida.profile1D("Number of Stereo Hits per layer in Top Half", 5, 1, 11);
        IProfile avgLayersBottomPlot = aida.profile1D("Number of Stereo Hits per layer in Bottom Half", 5, 1, 11);


        plotter55.region(0).plot(avgLayersTopPlot);
        plotter55.region(1).plot(avgLayersBottomPlot);


        plotter6 = fac.createPlotterFactory().create("HPS ECAL Hit Positions");
        plotter6.setTitle("ECAL Positions");
        //plotterFrame.addPlotter(plotter6);
        IPlotterStyle style6 = plotter6.style();
        style6.setParameter("hist2DStyle", "colorMap");
        style6.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        style6.dataStyle().fillStyle().setColor("yellow");
        style6.dataStyle().errorBarStyle().setVisible(false);
        plotter6.createRegions(4, 2);


        IHistogram2D topECal = aida.histogram2D("Top ECal Cluster Position", 50, -400, 400, 10, 0, 100);
        IHistogram2D botECal = aida.histogram2D("Bottom ECal Cluster Position", 50, -400, 400, 10, -100, 0);
        IHistogram2D topECal1 = aida.histogram2D("Top ECal Cluster Position (>0 tracks)", 50, -400, 400, 10, 0, 100);
        IHistogram2D botECal1 = aida.histogram2D("Bottom ECal Cluster Position (>0 tracks)", 50, -400, 400, 10, -100, 0);
        IHistogram2D topECal2 = aida.histogram2D("Top ECal Cluster Position (E>100,>0 tracks)", 50, -400, 400, 10, 0, 100);
        IHistogram2D botECal2 = aida.histogram2D("Bottom ECal Cluster Position (E>100,>0 tracks)", 50, -400, 400, 10, -100, 0);
        IHistogram2D topECal3 = aida.histogram2D("Top ECal Cluster Position w_E (E>100,>0 tracks)", 50, -400, 400, 10, 0, 100);
        IHistogram2D botECal3 = aida.histogram2D("Bottom ECal Cluster Position w_E (E>100,>0 tracks)", 50, -400, 400, 10, -100, 0);


        plotter6.region(0).plot(topECal);
        plotter6.region(1).plot(botECal);
        plotter6.region(2).plot(topECal1);
        plotter6.region(3).plot(botECal1);
        plotter6.region(4).plot(topECal2);
        plotter6.region(5).plot(botECal2);
        plotter6.region(6).plot(topECal3);
        plotter6.region(7).plot(botECal3);

        //plotterFrame.pack();
        //plotterFrame.setVisible(true);

        //topFrame.pack();
        //topFrame.setVisible(true);

        //bottomFrame.pack();
        //bottomFrame.setVisible(true);

        plotter7 = fac.createPlotterFactory().create("HPS ECAL Hit Positions");
        plotter7.setTitle("Basic Misc Stuff");
        //plotterFrame.addPlotter(plotter7);
        IPlotterStyle style7 = plotter7.style();
        style7.setParameter("hist2DStyle", "colorMap");
        style7.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        style7.dataStyle().fillStyle().setColor("yellow");
        style7.dataStyle().errorBarStyle().setVisible(false);
        plotter7.createRegions(2, 2);

        IHistogram2D quadrants = aida.histogram2D("Charge vs Slope", 2, -1, 1, 2, -1, 1);
        plotter7.region(0).plot(quadrants);


    }

    public TrackingReconstructionPlots() {
    }

    public void setOutputPlots(String output) {
        this.outputPlots = output;
    }

    public void setRawTrackerHitCollectionName(String rawTrackerHitCollectionName) {
        this.rawTrackerHitCollectionName = rawTrackerHitCollectionName;
    }

    public void setFittedTrackerHitCollectionName(String fittedTrackerHitCollectionName) {
        this.fittedTrackerHitCollectionName = fittedTrackerHitCollectionName;
    }

    public void setTrackerHitCollectionName(String trackerHitCollectionName) {
        this.trackerHitCollectionName = trackerHitCollectionName;
    }

    public void setHelicalTrackHitCollectionName(String helicalTrackHitCollectionName) {
        this.helicalTrackHitCollectionName = helicalTrackHitCollectionName;
    }

    public void setTrackCollectionName(String trackCollectionName) {
        this.trackCollectionName = trackCollectionName;
    }

    @Override
    public void process(EventHeader event) {
        aida.tree().cd("/");
        if (!event.hasCollection(HelicalTrackHit.class, helicalTrackHitCollectionName)) {
//            System.out.println(helicalTrackHitCollectionName + " does not exist; skipping event");
            return;
        }

        List<HelicalTrackHit> rotList = event.get(HelicalTrackHit.class, rotatedTrackHitCollectionName);
        for (HelicalTrackHit hth : rotList) {
            HelicalTrackCross htc = (HelicalTrackCross) hth;
//            System.out.println("TrackingReconstructionPlots::original helical track position = "+hth.getPosition()[0]+","+hth.getPosition()[1]+","+hth.getPosition()[2]);
//            System.out.println("TrackingReconstructionPlots::corrected helical track position = "+htc.getCorrectedPosition().toString());
        }
        List<HelicalTrackHit> hthList = event.get(HelicalTrackHit.class, helicalTrackHitCollectionName);
        int[] layersTop = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        int[] layersBot = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        for (HelicalTrackHit hth : hthList) {
            HelicalTrackCross htc = (HelicalTrackCross) hth;
//            System.out.println("TrackingReconstructionPlots::original helical track position = "+hth.getPosition()[0]+","+hth.getPosition()[1]+","+hth.getPosition()[2]);
//            System.out.println("TrackingReconstructionPlots::corrected helical track position = "+htc.getCorrectedPosition().toString());
            //These Helical Track Hits are in the JLAB frame
//            htc.resetTrackDirection();
            double x = htc.getPosition()[0];
            double y = htc.getPosition()[1];
            SiSensor sensor = ((SiSensor) ((RawTrackerHit) htc.getRawHits().get(0)).getDetectorElement());
            if (SvtUtils.getInstance().isTopLayer(sensor)) {
                layersTop[htc.Layer() - 1]++;
                Hep3Vector sensorPos = ((SiSensor) ((RawTrackerHit) htc.getRawHits().get(0)).getDetectorElement()).getGeometry().getPosition();
                if (htc.Layer() == 1) {
//                    System.out.println(sensorPos.toString());
//                    System.out.println("Hit X = " + x + "; Hit Y = " + y);
//                    aida.histogram2D("Layer 1 HTH Position:  Top").fill(x - sensorPos.x(), y - sensorPos.y());
                }
//                if (htc.Layer() == 7)
//                    aida.histogram2D("Layer 7 HTH Position:  Top").fill(x - sensorPos.x(), y - sensorPos.y());
            } else {
                layersBot[htc.Layer() - 1]++;
                Hep3Vector sensorPos = ((SiSensor) ((RawTrackerHit) htc.getRawHits().get(0)).getDetectorElement()).getGeometry().getPosition();
                if (htc.Layer() == 1) {
//                    System.out.println(sensorPos.toString());
//                    System.out.println("Hit X = " + x + "; Hit Y = " + y);
//                    aida.histogram2D("Layer 1 HTH Position:  Bottom").fill(x - sensorPos.x(), y - sensorPos.y());
                }
//                if (htc.Layer() == 7)
//                    aida.histogram2D("Layer 7 HTH Position:  Bottom").fill(x - sensorPos.x(), y - sensorPos.y());
            }
        }
        for (int i = 0; i < 10; i++) {
            aida.profile1D("Number of Stereo Hits per layer in Top Half").fill(i + 1, layersTop[i]);
            aida.profile1D("Number of Stereo Hits per layer in Bottom Half").fill(i + 1, layersBot[i]);
        }
        if (!event.hasCollection(Track.class, trackCollectionName)) {
//            System.out.println(trackCollectionName + " does not exist; skipping event");
            aida.histogram1D("Number Tracks/Event").fill(0);
            return;
        }



        List<Track> tracks = event.get(Track.class, trackCollectionName);
        nTracks.fill(tracks.size());
        if (event.hasCollection(HPSEcalCluster.class, ecalCollectionName)) {
            List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, ecalCollectionName);
            for (HPSEcalCluster cluster : clusters) {
                //System.out.println("cluser position = ("+cluster.getPosition()[0]+","+cluster.getPosition()[1]+") with energy = "+cluster.getEnergy());
                if (cluster.getPosition()[1] > 0)
                    aida.histogram2D("Top ECal Cluster Position").fill(cluster.getPosition()[0], cluster.getPosition()[1]);
                if (cluster.getPosition()[1] < 0)
                    aida.histogram2D("Bottom ECal Cluster Position").fill(cluster.getPosition()[0], cluster.getPosition()[1]);

                if (tracks.size() > 0) {
                    if (cluster.getPosition()[1] > 0)
                        aida.histogram2D("Top ECal Cluster Position (>0 tracks)").fill(cluster.getPosition()[0], cluster.getPosition()[1]);
                    if (cluster.getPosition()[1] < 0)
                        aida.histogram2D("Bottom ECal Cluster Position (>0 tracks)").fill(cluster.getPosition()[0], cluster.getPosition()[1]);

                    if (cluster.getEnergy() > 100) {
                        if (cluster.getPosition()[1] > 0) {
                            aida.histogram2D("Top ECal Cluster Position (E>100,>0 tracks)").fill(cluster.getPosition()[0], cluster.getPosition()[1]);
                            aida.histogram2D("Top ECal Cluster Position w_E (E>100,>0 tracks)").fill(cluster.getPosition()[0], cluster.getPosition()[1], cluster.getEnergy());
                        }
                        if (cluster.getPosition()[1] < 0) {
                            aida.histogram2D("Bottom ECal Cluster Position (E>100,>0 tracks)").fill(cluster.getPosition()[0], cluster.getPosition()[1]);
                            aida.histogram2D("Bottom ECal Cluster Position w_E (E>100,>0 tracks)").fill(cluster.getPosition()[0], cluster.getPosition()[1], cluster.getEnergy());
                        }
                    }
                }



            }
        }


        List<SiTrackerHitStrip1D> stripHits = event.get(SiTrackerHitStrip1D.class, "StripClusterer_SiTrackerHitStrip1D");
        int stripClustersPerLayerTop[] = getStripClustersPerLayer(stripHits, "up");
        //int stripClustersPerLayerBottom[] = getStripClustersPerLayer(stripHits,"down");

        boolean hasSingleStripClusterPerLayer = singleStripClusterPerLayer(stripClustersPerLayerTop);

        for (Track trk : tracks) {

            boolean isSingleHitPerLayerTrack = singleTrackHitPerLayer(trk);

            aida.histogram1D("Track Momentum (Px)").fill(trk.getPY());
            aida.histogram1D("Track Momentum (Py)").fill(trk.getPZ());
            aida.histogram1D("Track Momentum (Pz)").fill(trk.getPX());
            aida.histogram1D("Track Chi2").fill(trk.getChi2());

            aida.histogram1D("Hits per Track").fill(trk.getTrackerHits().size());
            SeedTrack stEle = (SeedTrack) trk;
            SeedCandidate seedEle = stEle.getSeedCandidate();
            HelicalTrackFit ht = seedEle.getHelix();
            HelixConverter converter = new HelixConverter(0);
            StraightLineTrack slt = converter.Convert(ht);

            Hep3Vector posAtEcal = TrackUtils.getTrackPositionAtEcal(trk);

            aida.histogram1D("X (mm) @ Z=-60cm").fill(slt.getYZAtX(BeamlineConstants.HARP_POSITION_TESTRUN)[0]);  //this is y in the tracker frame
            aida.histogram1D("Y (mm) @ Z=-60cm").fill(slt.getYZAtX(BeamlineConstants.HARP_POSITION_TESTRUN)[1]);  //this is z in the tracker frame
            //double sECAL = HelixUtils.PathToXPlane(ht, zEcal, 3000, 1).get(0);
            aida.histogram1D("X (mm) @ Z=-150cm").fill(slt.getYZAtX(zAtColl)[0]);
            aida.histogram1D("Y (mm) @ Z=-150cm").fill(slt.getYZAtX(zAtColl)[1]);


            //Straight line after field-region???
            //HelixConverter converterEcal = new HelixConverter(zAtDownStrPairSpec);
            //StraightLineTrack sltEcal = converterEcal.Convert(ht);
//            double sECAL = HelixUtils.PathToXPlane(ht, zEcal, 3000, 1).get(0);
//            Hep3Vector posonhelix = HelixUtils.PointOnHelix(ht, sECAL);//position in tracker coordinates!

            aida.histogram1D("X (mm) @ ECAL").fill(posAtEcal.x());
            aida.histogram1D("Y (mm) @ ECAL").fill(posAtEcal.y());
            if (trk.getPX() > 1.0) {
                aida.histogram1D("X (mm) @ ECAL (Pz>1)").fill(posAtEcal.x());
                aida.histogram1D("Y (mm) @ ECAL (Pz>1)").fill(posAtEcal.y());
            }
            aida.histogram1D("d0 ").fill(trk.getTrackParameter(ParameterName.d0.ordinal()));
            aida.histogram1D("sinphi ").fill(Math.sin(trk.getTrackParameter(ParameterName.phi0.ordinal())));
            aida.histogram1D("omega ").fill(trk.getTrackParameter(ParameterName.omega.ordinal()));
            aida.histogram1D("tan(lambda) ").fill(trk.getTrackParameter(ParameterName.tanLambda.ordinal()));
            aida.histogram1D("z0 ").fill(trk.getTrackParameter(ParameterName.z0.ordinal()));

            int isTop = -1;
            if (trk.getTrackerHits().get(0).getPosition()[2] > 0)
                isTop = 0;//make plot look pretty
            int charge = trk.getCharge();
            if (charge > 0)
                charge = 0;//make plot look pretty
//            System.out.println("Charge = " + charge + "; isTop = " + isTop);
            aida.histogram2D("Charge vs Slope").fill(charge, isTop);
            if (isTop == 0) {
                aida.histogram1D("Top Track Momentum (Px)").fill(trk.getPY());
                aida.histogram1D("Top Track Momentum (Py)").fill(trk.getPZ());
                aida.histogram1D("Top Track Momentum (Pz)").fill(trk.getPX());
                aida.histogram1D("Top Track Chi2").fill(trk.getChi2());
            } else {
                aida.histogram1D("Bottom Track Momentum (Px)").fill(trk.getPY());
                aida.histogram1D("Bottom Track Momentum (Py)").fill(trk.getPZ());
                aida.histogram1D("Bottom Track Momentum (Pz)").fill(trk.getPX());
                aida.histogram1D("Bottom Track Chi2").fill(trk.getChi2());
            }
            List<TrackerHit> hitsOnTrack = trk.getTrackerHits();
            for (TrackerHit hit : hitsOnTrack) {
                HelicalTrackHit htc = (HelicalTrackHit) hit;
                HelicalTrackCross htcross = (HelicalTrackCross) htc;
                double sHit = ht.PathMap().get(htc);
                Hep3Vector posonhelix = HelixUtils.PointOnHelix(ht, sHit);

                double yTr = posonhelix.y();
                double zTr = posonhelix.z();
                int layer = htc.Layer();
                String modNum = "Module X ";
                if (layer == 1)
                    modNum = "Module 1 ";
                if (layer == 3)
                    modNum = "Module 2 ";
                if (layer == 5)
                    modNum = "Module 3 ";
                if (layer == 7)
                    modNum = "Module 4 ";
                if (layer == 9)
                    modNum = "Module 5 ";
                SymmetricMatrix cov = htc.getCorrectedCovMatrix();

                aida.histogram1D(modNum + "Residual X(mm)").fill(htcross.getCorrectedPosition().y() - yTr);//these hits should be rotated track hits already
                aida.histogram1D(modNum + "Residual Y(mm)").fill(htcross.getCorrectedPosition().z() - zTr);//these hits should be rotated track hits already
                if (hit.getPosition()[2] > 0) {
                    aida.histogram1D(modNum + "Residual X(mm) Top").fill(htcross.getCorrectedPosition().y() - yTr);//these hits should be rotated track hits already
                    aida.histogram1D(modNum + "Residual Y(mm) Top").fill(htcross.getCorrectedPosition().z() - zTr);//these hits should be rotated track hits already

                }
                if (hit.getPosition()[2] < 0) {
                    aida.histogram1D(modNum + "Residual X(mm) Bottom").fill(htcross.getCorrectedPosition().y() - yTr);//these hits should be rotated track hits already
                    aida.histogram1D(modNum + "Residual Y(mm) Bottom").fill(htcross.getCorrectedPosition().z() - zTr);//these hits should be rotated track hits already

                }
                SiSensor sensor = ((SiSensor) ((RawTrackerHit) htc.getRawHits().get(0)).getDetectorElement());
                double x = htcross.getCorrectedPosition().y();
                double y = htcross.getCorrectedPosition().z();
                if (SvtUtils.getInstance().isTopLayer(sensor)) {
                    layersTop[htc.Layer() - 1]++;
                    Hep3Vector sensorPos = ((SiSensor) ((RawTrackerHit) htc.getRawHits().get(0)).getDetectorElement()).getGeometry().getPosition();
                    if (htc.Layer() == 1) {
//                    System.out.println(sensorPos.toString());
//                    System.out.println("Hit X = " + x + "; Hit Y = " + y);
                        aida.histogram2D("Layer 1 HTH Position:  Top").fill(x - sensorPos.x(), y - sensorPos.y());
                    }
                    if (htc.Layer() == 7)
                        aida.histogram2D("Layer 7 HTH Position:  Top").fill(x - sensorPos.x(), y - sensorPos.y());
                } else {
                    layersBot[htc.Layer() - 1]++;
                    Hep3Vector sensorPos = ((SiSensor) ((RawTrackerHit) htc.getRawHits().get(0)).getDetectorElement()).getGeometry().getPosition();
                    if (htc.Layer() == 1) {
//                    System.out.println(sensorPos.toString());
//                    System.out.println("Hit X = " + x + "; Hit Y = " + y);
                        aida.histogram2D("Layer 1 HTH Position:  Bottom").fill(x - sensorPos.x(), y - sensorPos.y());
                    }
                    if (htc.Layer() == 7)
                        aida.histogram2D("Layer 7 HTH Position:  Bottom").fill(x - sensorPos.x(), y - sensorPos.y());
                }
/*
                List<RawTrackerHit> rawHits = hit.getRawHits();                
                for (RawTrackerHit rawHit : rawHits) {
                    ChannelConstants constants = HPSSVTCalibrationConstants.getChannelConstants((SiSensor) rawHit.getDetectorElement(), rawHit.getIdentifierFieldValue("strip"));
                    HPSShapeFitParameters fit = _shaper.fitShape(rawHit, constants);
                    double amp = fit.getAmp();
                    
                    aida.histogram1D("Amp (HitOnTrack)").fill(amp);
                    if (trk.getPX() > 1)
                        aida.histogram1D("Amp Pz>1000 (HitOnTrack)").fill(amp);
                }                
                */

               for(HelicalTrackStrip hts:htcross.getStrips()){
                   double clusterSum=0;                 
                   for(RawTrackerHit rawHit: (List<RawTrackerHit>)hts.rawhits()){
                       ChannelConstants constants = HPSSVTCalibrationConstants.getChannelConstants((SiSensor) rawHit.getDetectorElement(), rawHit.getIdentifierFieldValue("strip"));
                        HPSShapeFitParameters fit = _shaper.fitShape(rawHit, constants);
                        double amp = fit.getAmp();
                        clusterSum+=amp;
                         aida.histogram1D("Amp (HitOnTrack)").fill(amp);
                    if (trk.getPX() > 1)
                        aida.histogram1D("Amp Pz>1000 (HitOnTrack)").fill(amp);
                   }
                   aida.histogram1D("Amp (CluOnTrack)").fill(clusterSum);
                      if (trk.getPX() > 1)
                    aida.histogram1D("Amp Pz>1000 (CluOnTrack)").fill(clusterSum);
                }
            }
            List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, ecalCollectionName);
            HPSEcalCluster clust = findClosestCluster(posAtEcal, clusters);

            //           if (clust != null) {
            if (clust != null) {
                
                posAtEcal = TrackUtils.extrapolateTrack(trk,clust.getPosition()[2]);//.positionAtEcal();

                aida.histogram2D("Energy Vs Momentum").fill(clust.getEnergy(), trk.getPX() * 1000.0);
                aida.histogram1D("Energy Over Momentum").fill(clust.getEnergy() / (trk.getPX() * 1000.0));
                aida.histogram1D("deltaX").fill(clust.getPosition()[0] - posAtEcal.x());
                aida.histogram1D("deltaY").fill(clust.getPosition()[1] - posAtEcal.y());
//                if (trk.getPX() > 1.0) {
//                    aida.histogram1D("deltaX (Pz>1)").fill(clust.getPosition()[0] - posAtEcal.y());
//                    aida.histogram1D("deltaY (Pz>1)").fill(clust.getPosition()[1] - posAtEcal.z());
//                }
                aida.histogram2D("X ECal Vs Track").fill(clust.getPosition()[0], posAtEcal.x());
                aida.histogram2D("Y ECal Vs Track").fill(clust.getPosition()[1], posAtEcal.y());
                if (isTop == 0) {
                    aida.histogram2D("Top Energy Vs Momentum").fill(clust.getEnergy(), trk.getPX() * 1000.0);
//                    aida.histogram2D("Top Energy Vs Momentum").fill(posAtEcal.y(), trk.getPX() * 1000.0);
                    aida.histogram1D("Top Energy Over Momentum").fill(clust.getEnergy() / (trk.getPX() * 1000.0));
                    aida.histogram1D("Top deltaX").fill(clust.getPosition()[0] - posAtEcal.x());
                    aida.histogram1D("Top deltaY").fill(clust.getPosition()[1] - posAtEcal.y());
                    aida.histogram2D("Top deltaX vs X").fill(clust.getPosition()[0],clust.getPosition()[0] - posAtEcal.x());
                    aida.histogram2D("Top deltaY vs Y").fill(clust.getPosition()[1],clust.getPosition()[1] - posAtEcal.y());
                    aida.histogram2D("Top X ECal Vs Track").fill(clust.getPosition()[0], posAtEcal.x());
                    aida.histogram2D("Top Y ECal Vs Track").fill(clust.getPosition()[1], posAtEcal.y());
                } else {
                    aida.histogram2D("Bottom Energy Vs Momentum").fill(clust.getEnergy(), trk.getPX() * 1000.0);
                    aida.histogram1D("Bottom Energy Over Momentum").fill(clust.getEnergy() / (trk.getPX() * 1000.0));
                    aida.histogram1D("Bottom deltaX").fill(clust.getPosition()[0] - posAtEcal.x());
                    aida.histogram1D("Bottom deltaY").fill(clust.getPosition()[1] - posAtEcal.y());
                    aida.histogram2D("Bottom deltaX vs X").fill(clust.getPosition()[0],clust.getPosition()[0] - posAtEcal.x());
                    aida.histogram2D("Bottom deltaY vs Y").fill(clust.getPosition()[1],clust.getPosition()[1] - posAtEcal.y());
                    aida.histogram2D("Bottom X ECal Vs Track").fill(clust.getPosition()[0], posAtEcal.x());
                    aida.histogram2D("Bottom Y ECal Vs Track").fill(clust.getPosition()[1], posAtEcal.y());
                }

            }

        }
    }

    public int[] getTrackHitsPerLayer(Track trk) {
        int n[] = {0, 0, 0, 0, 0};
        List<TrackerHit> hitsOnTrack = trk.getTrackerHits();
        int layer;
        for (TrackerHit hit : hitsOnTrack) {
            HelicalTrackHit htc = (HelicalTrackHit) hit;
//            if (htc.getPosition()[2] < 0) {
            layer = htc.Layer();
            layer = (layer - 1) / 2;
            n[layer] = n[layer] + 1;
//            }
        }

        return n;
    }

    public boolean singleTrackHitPerLayer(Track track) {
        int hitsPerLayer[] = getTrackHitsPerLayer(track);
        for (int i = 0; i < 5; ++i) {
            if (hitsPerLayer[i] != 1) {
                return false;
            }
        }
        return true;
    }

    public boolean singleStripClusterPerLayer(int hitsPerLayer[]) {
        //This includes both axial and stereo separately 
        // so for a hit in each double layer we need 10 hits
        for (int i = 0; i < 10; ++i) {
            if (hitsPerLayer[i] != 1) {
                return false;
            }
        }
        return true;
    }

    public int[] getStripClustersPerLayer(List<SiTrackerHitStrip1D> trackerHits, String side) {
        String si_side;
        String name;
        int l;
        int i;
        int n[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        boolean ddd = false;

        if (ddd) {
            System.out.println("Get # hits per layer on side \"" + side + "\"");
        }

        for (SiTrackerHitStrip1D stripCluster : trackerHits) {

            if (ddd) {
                System.out.println("Processing stripCluster " + stripCluster.toString());
            }


            if (!"".equals(side)) {
                String s;
                if (stripCluster.getPosition()[1] >= 0.0)
                    s = "up";
                else
                    s = "down";
                if (!s.equals(side))
                    continue;
            }

            name = stripCluster.getSensor().getName();
            if (name.length() < 14) {
                System.err.println("This name is too short!!");
                throw new RuntimeException("SiSensor name " + name + " is invalid?");
            }

            if (ddd) {
                System.out.println("sensor name  " + name);
            }

            //String str_l = name.substring(13);
            String str_l = name.substring(name.indexOf("layer") + 5, name.indexOf("_module"));
            l = Integer.parseInt(str_l);

            if (ddd) {
                System.out.println("sensor name  " + name + " --> layer " + l);
            }

            if (l < 1 || l > 10) {
                System.out.println("This layer doesn't exist?");
                throw new RuntimeException("SiSensor name " + name + " is invalid?");
            }

            n[l - 1] = n[l - 1] + 1;

        }

        return n;
    }

    @Override
    public void endOfData() {
        if (outputPlots != null)
            try {
                aida.saveAs(outputPlots);
            } catch (IOException ex) {
                Logger.getLogger(TrackingReconstructionPlots.class.getName()).log(Level.SEVERE, null, ex);
            }
        //plotterFrame.dispose();
        //topFrame.dispose();
        //bottomFrame.dispose();
    }

    private HPSEcalCluster findClosestCluster(Hep3Vector posonhelix, List<HPSEcalCluster> clusters) {
        HPSEcalCluster closest = null;
        double minDist = 9999;
        for (HPSEcalCluster cluster : clusters) {
            double[] clPos = cluster.getPosition();
            double clEne = cluster.getEnergy();
            double dist = Math.sqrt(Math.pow(clPos[0] - posonhelix.x(), 2) + Math.pow(clPos[1] - posonhelix.y(), 2)); //coordinates!!!
//            double dist = Math.sqrt(Math.pow(clPos[1] - posonhelix.z(), 2)); //coordinates!!!
            if (dist < minDist && clEne > 50) {
                closest = cluster;
                minDist = dist;
            }
//                    if(cluster.getEnergy()/10>500)
        }
//        System.out.println("Found a cluster..." + minDist);

        return closest;

    }

    @Override
    public void reset() {
        aida.histogram1D("Track Momentum (Px)").reset();
        aida.histogram1D("Track Momentum (Py)").reset();
        aida.histogram1D("Track Momentum (Pz)").reset();
        aida.histogram1D("Track Chi2").reset();
        aida.histogram1D("Tracks per Event").reset();
        aida.histogram1D("X @ Z=-60cm").reset();
        aida.histogram1D("Y @ Z=-60cm").reset();
        aida.histogram1D("Hits per Track").reset();
        aida.histogram1D("Module 1 Residual X(mm)").reset();
        aida.histogram1D("Module 1 Residual Y(mm)").reset();
        aida.histogram1D("Module 2 Residual X(mm)").reset();
        aida.histogram1D("Module 2 Residual Y(mm)").reset();
        aida.histogram1D("Module 3 Residual X(mm)").reset();
        aida.histogram1D("Module 3 Residual Y(mm)").reset();
        aida.histogram1D("Module 4 Residual X(mm)").reset();
        aida.histogram1D("Module 4 Residual Y(mm)").reset();
        aida.histogram1D("Module 5 Residual X(mm)").reset();
        aida.histogram1D("Module 5 Residual Y(mm)").reset();
        aida.histogram1D("Module 1 Residual X(mm) Top").reset();
        aida.histogram1D("Module 1 Residual Y(mm) Top").reset();
        aida.histogram1D("Module 2 Residual X(mm) Top").reset();
        aida.histogram1D("Module 2 Residual Y(mm) Top").reset();
        aida.histogram1D("Module 3 Residual X(mm) Top").reset();
        aida.histogram1D("Module 3 Residual Y(mm) Top").reset();
        aida.histogram1D("Module 4 Residual X(mm) Top").reset();
        aida.histogram1D("Module 4 Residual Y(mm) Top").reset();
        aida.histogram1D("Module 5 Residual X(mm) Top").reset();
        aida.histogram1D("Module 5 Residual Y(mm) Top").reset();
        aida.histogram1D("Module 1 Residual X(mm) Bottom").reset();
        aida.histogram1D("Module 1 Residual Y(mm) Bottom").reset();
        aida.histogram1D("Module 2 Residual X(mm) Bottom").reset();
        aida.histogram1D("Module 2 Residual Y(mm) Bottom").reset();
        aida.histogram1D("Module 3 Residual X(mm) Bottom").reset();
        aida.histogram1D("Module 3 Residual Y(mm) Bottom").reset();
        aida.histogram1D("Module 4 Residual X(mm) Bottom").reset();
        aida.histogram1D("Module 4 Residual Y(mm) Bottom").reset();
        aida.histogram1D("Module 5 Residual X(mm) Bottom").reset();
        aida.histogram1D("Module 5 Residual Y(mm) Bottom").reset();

        aida.histogram2D("Energy Vs Momentum").reset();
        aida.histogram1D("Energy Over Momentum").reset();
        aida.histogram1D("deltaX").reset();
        aida.histogram1D("deltaY").reset();

        aida.histogram1D("Amp (HitOnTrack)").reset();

        aida.histogram1D("d0 ").reset();
        aida.histogram1D("sinphi ").reset();
        aida.histogram1D("omega ").reset();
        aida.histogram1D("tan(lambda) ").reset();
        aida.histogram1D("z0 ").reset();
    }
}
