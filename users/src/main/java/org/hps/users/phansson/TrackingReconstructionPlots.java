package org.hps.users.phansson;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import hep.aida.IProfile;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.Pair;
import org.hps.readout.ecal.FADCEcalReadoutDriver.PulseShape;
import org.hps.recon.tracking.BeamlineConstants;
import org.hps.recon.tracking.DumbShaperFit;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.recon.tracking.HelixConverter;
import org.hps.recon.tracking.ShapeFitParameters;
import org.hps.recon.tracking.ShaperFitAlgorithm;
import org.hps.recon.tracking.StraightLineTrack;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.gbl.HelicalTrackStripGbl;
import org.hps.recon.tracking.gbl.HpsGblRefitter;
import org.hps.util.BasicLogFormatter;
import org.lcsim.constants.Constants;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCIOParameters.ParameterName;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.geometry.compact.converter.HPSTrackerBuilder;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.log.LogUtil;

/**
 *
 * @author phansson
 */
public class TrackingReconstructionPlots extends Driver {

    static {
        hep.aida.jfree.AnalysisFactory.register();
    }

    
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
    IDDecoder dec;
    private String outputPlots = null;
    IPlotter plotter;
    IPlotter plotter2;
    IPlotter plotter22;
    IPlotter plotter2221;
    IPlotter plotter2222;
    IPlotter plotter222;
    IPlotter plotter22299;
    IPlotter plotter22298;
    IPlotter plotter2224;
    IPlotter plotter2223;
    IPlotter plotter3;
    IPlotter plotter3_1;
    IPlotter plotter3_11;
    IPlotter plotter3_2;
    IPlotter plotter4;
    IPlotter plotter5;
    IPlotter plotter5_1;
    IPlotter plotter55;
    IPlotter plotter6;
    IPlotter plotter66;
    IPlotter plotter7;
    IPlotter plotter8;
    IPlotter plotter9;
    IPlotter top1;
    IPlotter top2;
    IPlotter top3;
    IPlotter top4;
    IPlotter top44;
    IPlotter bot1;
    IPlotter bot2;
    IPlotter bot3;
    IPlotter bot4;
    double zAtColl = -1500;
    IHistogram1D trkPx;
    IHistogram1D nTracks;
    IHistogram1D nTracksTop;
    IHistogram1D nTracksBot;
    ShaperFitAlgorithm _shaper = new DumbShaperFit();
    HelixConverter converter = new HelixConverter(0);
    private boolean showPlots = true;
    private double _bfield;
    private static Logger logger = LogUtil.create(TrackingReconstructionPlots.class, new BasicLogFormatter());

    @Override
    protected void detectorChanged(Detector detector) {
        aida.tree().cd("/");
        List<HpsSiSensor> sensors = new ArrayList<HpsSiSensor>();
        for(HpsSiSensor s : detector.getDetectorElement().findDescendants(HpsSiSensor.class)) {
            if(s.getName().startsWith("module_") && s.getName().endsWith("sensor0")) {
                sensors.add(s);
            }
        }
        logger.info("Found " + sensors.size() + " SiSensors.");       
        
        Hep3Vector bfieldvec = detector.getFieldMap().getField(new BasicHep3Vector(0., 0., 1.));
        _bfield = bfieldvec.y();

        IAnalysisFactory fac = aida.analysisFactory();
        plotter = fac.createPlotterFactory().create("HPS Tracking Plots");
        plotter.setTitle("Momentum");
        IPlotterStyle style = plotter.style();
        style.dataStyle().fillStyle().setColor("yellow");
        style.dataStyle().errorBarStyle().setVisible(false);
        plotter.createRegions(2, 2);
        //plotterFrame.addPlotter(plotter);

        trkPx = aida.histogram1D("Track Momentum (Px)", 25, -0.25, 0.25);
        IHistogram1D trkPy = aida.histogram1D("Track Momentum (Py)", 25, -0.5, 0.5);
        IHistogram1D trkPz = aida.histogram1D("Track Momentum (Pz)", 25, 0, 1.5);
        IHistogram1D trkChi2 = aida.histogram1D("Track Chi2", 25, 0, 25.0);

        plotter.region(0).plot(trkPx);
        plotter.region(1).plot(trkPy);
        plotter.region(2).plot(trkPz);
        plotter.region(3).plot(trkChi2);

        if(showPlots) plotter.show();

//   ******************************************************************
        top1 = fac.createPlotterFactory().create("Top Tracking Plots");
        top1.setTitle("Top Momentum");
        IPlotterStyle stop1 = top1.style();
        stop1.dataStyle().fillStyle().setColor("green");
        stop1.dataStyle().errorBarStyle().setVisible(false);
        top1.createRegions(2, 2);
        //topFrame.addPlotter(top1);

        IHistogram1D toptrkPx = aida.histogram1D("Top Track Momentum (Px)", 25, -0.25, 0.25);
        IHistogram1D toptrkPy = aida.histogram1D("Top Track Momentum (Py)", 25, -0.5, 0.5);
        IHistogram1D toptrkPz = aida.histogram1D("Top Track Momentum (Pz)", 25, 0, 1.5);
        IHistogram1D toptrkChi2 = aida.histogram1D("Top Track Chi2", 25, 0, 25.0);

        top1.region(0).plot(toptrkPx);
        top1.region(1).plot(toptrkPy);
        top1.region(2).plot(toptrkPz);
        top1.region(3).plot(toptrkChi2);

        if(showPlots) top1.show();

        bot1 = fac.createPlotterFactory().create("Bottom Tracking Plots");
        bot1.setTitle("Bottom Momentum");
        IPlotterStyle sbot1 = bot1.style();
        sbot1.dataStyle().fillStyle().setColor("blue");
        sbot1.dataStyle().errorBarStyle().setVisible(false);
        bot1.createRegions(2, 2);
        //bottomFrame.addPlotter(bot1);

        IHistogram1D bottrkPx = aida.histogram1D("Bottom Track Momentum (Px)", 25, -0.25, 0.25);
        IHistogram1D bottrkPy = aida.histogram1D("Bottom Track Momentum (Py)", 25, -0.5, 0.5);
        IHistogram1D bottrkPz = aida.histogram1D("Bottom Track Momentum (Pz)", 25, 0, 1.5);
        IHistogram1D bottrkChi2 = aida.histogram1D("Bottom Track Chi2", 25, 0, 25.0);

        bot1.region(0).plot(bottrkPx);
        bot1.region(1).plot(bottrkPy);
        bot1.region(2).plot(bottrkPz);
        bot1.region(3).plot(bottrkChi2);

        if(showPlots) bot1.show();

//   ******************************************************************
        IHistogram1D trkd0 = aida.histogram1D("d0 ", 25, -10.0, 10.0);
        IHistogram1D trkphi = aida.histogram1D("sinphi ", 25, -0.2, 0.2);
        IHistogram1D trkomega = aida.histogram1D("omega ", 25, -0.0025, 0.0025);
        IHistogram1D trklam = aida.histogram1D("tan(lambda) ", 25, -0.1, 0.1);
        IHistogram1D trkz0 = aida.histogram1D("z0 ", 25, -6.0, 6.0);

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
        
        if(showPlots) plotter22.show();

 //   ******************************************************************

        
         trkd0 = aida.histogram1D("d0 Top", 25, -10.0, 10.0);
         trkphi = aida.histogram1D("sinphi Top", 25, -0.2, 0.2);
         trkomega = aida.histogram1D("omega Top", 25, -0.0025, 0.0025);
         trklam = aida.histogram1D("tan(lambda) Top", 25, -0.1, 0.1);
         trkz0 = aida.histogram1D("z0 Top", 25, -6.0, 6.0);

        plotter2221 = fac.createPlotterFactory().create("HPS Track Params");
        plotter2221.setTitle("Track parameters");
        //plotterFrame.addPlotter(plotter22);
        IPlotterStyle style2221 = plotter2221.style();
        style2221.dataStyle().fillStyle().setColor("yellow");
        style2221.dataStyle().errorBarStyle().setVisible(false);
        plotter2221.createRegions(2, 3);
        plotter2221.region(0).plot(trkd0);
        plotter2221.region(1).plot(trkphi);
        plotter2221.region(2).plot(trkomega);
        plotter2221.region(3).plot(trklam);
        plotter2221.region(4).plot(trkz0);
        
        if(showPlots) plotter2221.show();
        
        
   //   ******************************************************************

        
        trkd0 = aida.histogram1D("d0 Bottom", 25, -10.0, 10.0);
        trkphi = aida.histogram1D("sinphi Bottom", 25, -0.2, 0.2);
        trkomega = aida.histogram1D("omega Bottom", 25, -0.0025, 0.0025);
        trklam = aida.histogram1D("tan(lambda) Bottom", 25, -0.1, 0.1);
        trkz0 = aida.histogram1D("z0 Bottom", 25, -6.0, 6.0);

       plotter2222 = fac.createPlotterFactory().create("HPS Track Params");
       plotter2222.setTitle("Track parameters");
       //plotterFrame.addPlotter(plotter22);
       IPlotterStyle style2222 = plotter2222.style();
       style2222.dataStyle().fillStyle().setColor("yellow");
       style2222.dataStyle().errorBarStyle().setVisible(false);
       plotter2222.createRegions(2, 3);
       plotter2222.region(0).plot(trkd0);
       plotter2222.region(1).plot(trkphi);
       plotter2222.region(2).plot(trkomega);
       plotter2222.region(3).plot(trklam);
       plotter2222.region(4).plot(trkz0);
       
       if(showPlots) plotter2222.show();
        
        
        
   //   ******************************************************************

        
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
        
        if(showPlots) plotter2.show();

   //   ******************************************************************
        
        plotter222 = fac.createPlotterFactory().create("HPS Tracking Plots");
        plotter222.setTitle("HPS Tracking Plots");
        //plotterFrame.addPlotter(plotter222);
        IPlotterStyle style222 = plotter222.style();
        style222.dataStyle().fillStyle().setColor("yellow");
        style222.dataStyle().errorBarStyle().setVisible(false);
        plotter222.createRegions(2, 2);
        
        IHistogram1D nHits = aida.histogram1D("Hits per Track", 4, 3, 7);
        nTracks = aida.histogram1D("Tracks per Event", 3, 0, 3);
        IHistogram1D nHitsCluster = aida.histogram1D("Hits in Cluster (HitOnTrack)", 4, 0, 4);

       
        plotter222.region(0).plot(nHits);
        plotter222.region(1).plot(nTracks);
        plotter222.region(2).plot(nHitsCluster);
        
        if(showPlots) plotter222.show();
   
        
   //   ******************************************************************
        
        plotter22299 = fac.createPlotterFactory().create("HPS Tracking Plots Top");
        plotter22299.setTitle("HPS Tracking Plots Top");
        //plotterFrame.addPlotter(plotter22299);
        IPlotterStyle style22299 = plotter22299.style();
        style22299.dataStyle().fillStyle().setColor("yellow");
        style22299.dataStyle().errorBarStyle().setVisible(false);
        plotter22299.createRegions(2, 2);
        
        IHistogram1D nHitsTop = aida.histogram1D("Hits per Track Top", 4, 3, 7);
        nTracksTop = aida.histogram1D("Tracks per Event Top", 3, 0, 3);
        IHistogram1D nHitsClusterTop = aida.histogram1D("Hits in Cluster (HitOnTrack) Top", 4, 0, 4);

       
        plotter22299.region(0).plot(nHitsTop);
        plotter22299.region(1).plot(nTracksTop);
        plotter22299.region(2).plot(nHitsClusterTop);
        
        if(showPlots) plotter22299.show();
   
//   ******************************************************************
        
        plotter22298 = fac.createPlotterFactory().create("HPS Tracking Plots Bottom");
        plotter22298.setTitle("HPS Tracking Plots Bottom");
        //plotterFrame.addPlotter(plotter22298);
        IPlotterStyle style22298 = plotter22298.style();
        style22298.dataStyle().fillStyle().setColor("yellow");
        style22298.dataStyle().errorBarStyle().setVisible(false);
        plotter22298.createRegions(2, 2);
        
        IHistogram1D nHitsBot = aida.histogram1D("Hits per Track Bot", 4, 3, 7);
        nTracksBot = aida.histogram1D("Tracks per Event Bot", 3, 0, 3);
        IHistogram1D nHitsClusterBot = aida.histogram1D("Hits in Cluster (HitOnTrack) Bot", 4, 0, 4);

       
        plotter22298.region(0).plot(nHitsBot);
        plotter22298.region(1).plot(nTracksBot);
        plotter22298.region(2).plot(nHitsClusterBot);
        
        if(showPlots) plotter22298.show();
   
        
        //   ******************************************************************
        
        
        plotter2223 = fac.createPlotterFactory().create("Cluster Amp Plots");
        plotter2223.setTitle("Other");
        //plotterFrame.addPlotter(plotter222);
        IPlotterStyle style2223 = plotter2223.style();
        style2223.dataStyle().fillStyle().setColor("yellow");
        style2223.dataStyle().errorBarStyle().setVisible(false);
        plotter2223.createRegions(2, 2);
        
       

        IHistogram1D amp = aida.histogram1D("Amp (HitOnTrack)", 50, 0, 5000);
        IHistogram1D ampcl = aida.histogram1D("Cluster Amp (HitOnTrack)", 50, 0, 5000);
        IHistogram1D amp2 = aida.histogram1D("Amp Pz>0.8 (HitOnTrack)", 50, 0, 5000);
        IHistogram1D ampcl2 = aida.histogram1D("Cluster Amp Pz>0.8 (HitOnTrack)", 50, 0, 5000);
      
        
        plotter2223.region(0).plot(amp);
        plotter2223.region(1).plot(amp2);
        plotter2223.region(2).plot(ampcl);
        plotter2223.region(3).plot(ampcl2);
        
        if(showPlots) plotter2223.show();
   
//   ******************************************************************
        
        
        plotter2224 = fac.createPlotterFactory().create("t0 Plots");
        plotter2224.setTitle("Other");
        IPlotterStyle style2224 = plotter2224.style();
        style2224.dataStyle().fillStyle().setColor("yellow");
        style2224.dataStyle().errorBarStyle().setVisible(false);
        plotter2224.createRegions(2, 2);
                
        IHistogram1D t0 = aida.histogram1D("t0 (HitOnTrack)", 50, -100, 100);
        IHistogram1D t0cl = aida.histogram1D("Cluster t0 (HitOnTrack)", 50, -100, 100);
        IHistogram1D t02 = aida.histogram1D("t0 Pz>0.8 (HitOnTrack)", 50, -100, 100);
        IHistogram1D t0cl2 = aida.histogram1D("Cluster t0 Pz>0.8 (HitOnTrack)", 50, -100, 100);
        
        plotter2224.region(0).plot(t0);
        plotter2224.region(1).plot(t0cl);
        plotter2224.region(2).plot(t02);
        plotter2224.region(3).plot(t0cl2);

        if(showPlots) plotter2224.show();
   
        
        //   ******************************************************************
          
        plotter3 = fac.createPlotterFactory().create("HPS Layer Residual Plots");
        plotter3.setTitle("Layer Residuals");
        //plotterFrame.addPlotter(plotter3);
        IPlotterStyle style3 = plotter3.style();
        style3.dataStyle().fillStyle().setColor("yellow");
        style3.dataStyle().errorBarStyle().setVisible(false);
        plotter3.createRegions(6, 2);

       

        IHistogram1D mod1ResX = aida.histogram1D("Layer 1 Residual X(mm)", 25, -1, 1);
        IHistogram1D mod1ResY = aida.histogram1D("Layer 1 Residual Y(mm)", 25, -0.04, 0.04);

        IHistogram1D mod2ResX = aida.histogram1D("Layer 2 Residual X(mm)", 25, -2, 2);
        IHistogram1D mod2ResY = aida.histogram1D("Layer 2 Residual Y(mm)", 25, -1, 1);

        IHistogram1D mod3ResX = aida.histogram1D("Layer 3 Residual X(mm)", 25, -2.5, 2.5);
        IHistogram1D mod3ResY = aida.histogram1D("Layer 3 Residual Y(mm)", 25, -1.5, 1.5);

        IHistogram1D mod4ResX = aida.histogram1D("Layer 4 Residual X(mm)", 25, -3.0, 3.0);
        IHistogram1D mod4ResY = aida.histogram1D("Layer 4 Residual Y(mm)", 25, -2, 2);

        IHistogram1D mod5ResX = aida.histogram1D("Layer 5 Residual X(mm)", 25, -4, 4);
        IHistogram1D mod5ResY = aida.histogram1D("Layer 5 Residual Y(mm)", 25, -3, 3);

        IHistogram1D mod6ResX = aida.histogram1D("Layer 6 Residual X(mm)", 25, -5, 5);
        IHistogram1D mod6ResY = aida.histogram1D("Layer 6 Residual Y(mm)", 25, -3, 3);

        plotter3.region(0).plot(mod1ResX);
        plotter3.region(2).plot(mod2ResX);
        plotter3.region(4).plot(mod3ResX);
        plotter3.region(6).plot(mod4ResX);
        plotter3.region(8).plot(mod5ResX);
        plotter3.region(10).plot(mod6ResX);

        plotter3.region(1).plot(mod1ResY);
        plotter3.region(3).plot(mod2ResY);
        plotter3.region(5).plot(mod3ResY);
        plotter3.region(7).plot(mod4ResY);
        plotter3.region(9).plot(mod5ResY);
        plotter3.region(11).plot(mod6ResY);
               
        if(showPlots) plotter3.show();
        
        
        
        plotter3_11 = fac.createPlotterFactory().create("HPS Strip Residual Plots");
        plotter3_11.setTitle("Strip Residuals (Top)");
        //plotterFrame.addPlotter(plotter3_11);
        IPlotterStyle style3_11 = plotter3_11.style();
        style3_11.dataStyle().fillStyle().setColor("yellow");
        style3_11.dataStyle().errorBarStyle().setVisible(false);
        plotter3_11.createRegions(6, 6);
        int i=0;
        for(HpsSiSensor sensor : sensors) {
            double min = 0.0;
            double max = 0.0;
            if(sensor.getName().contains("L1")) {
                min=-0.04; max=0.04;
            } else if(sensor.getName().contains("L2")) {
                min=-1; max=1;
            } else if(sensor.getName().contains("L3")) {
                min=-1.5; max=1.5;
            } else if(sensor.getName().contains("L4")) {
                min=-3; max=3;
            } else if(sensor.getName().contains("L5")) {
                min=-4; max=4;
            } else if(sensor.getName().contains("L6")) {
                min=-5; max=5;
            } else {
                throw new RuntimeException("Invalid sensor name: " + sensor.getName());
            }
           IHistogram1D resX = aida.histogram1D(sensor.getName() + " strip residual (mm)", 50, min, max);
            plotter3_11.region(i).plot(resX);
            i++;
        }

        if(showPlots) plotter3_11.show();
        

        plotter3_1 = fac.createPlotterFactory().create("HPS Residual Plots (Single hit per layer)");
        plotter3_1.setTitle("Residuals (Top)");
        //plotterFrame.addPlotter(plotter3_1);
        IPlotterStyle style3_1 = plotter3_1.style();
        style3_1.dataStyle().fillStyle().setColor("yellow");
        style3_1.dataStyle().errorBarStyle().setVisible(false);
        plotter3_1.createRegions(6, 2);
        
        IHistogram1D mod1ResX_Top = aida.histogram1D("Layer 1 Residual X(mm) Top", 25, -1, 1);
        IHistogram1D mod1ResY_Top = aida.histogram1D("Layer 1 Residual Y(mm) Top", 25, -0.04, 0.04);

        IHistogram1D mod2ResX_Top = aida.histogram1D("Layer 2 Residual X(mm) Top", 25, -2, 2);
        IHistogram1D mod2ResY_Top = aida.histogram1D("Layer 2 Residual Y(mm) Top", 25, -1, 1);

        IHistogram1D mod3ResX_Top = aida.histogram1D("Layer 3 Residual X(mm) Top", 25, -2.5, 2.5);
        IHistogram1D mod3ResY_Top = aida.histogram1D("Layer 3 Residual Y(mm) Top", 25, -1.5, 1.5);

        IHistogram1D mod4ResX_Top = aida.histogram1D("Layer 4 Residual X(mm) Top", 25, -3.0, 3.0);
        IHistogram1D mod4ResY_Top = aida.histogram1D("Layer 4 Residual Y(mm) Top", 25, -2, 2);

        IHistogram1D mod5ResX_Top = aida.histogram1D("Layer 5 Residual X(mm) Top", 25, -4, 4);
        IHistogram1D mod5ResY_Top = aida.histogram1D("Layer 5 Residual Y(mm) Top", 25, -3, 3);

        IHistogram1D mod6ResX_Top = aida.histogram1D("Layer 6 Residual X(mm) Top", 25, -5, 5);
        IHistogram1D mod6ResY_Top = aida.histogram1D("Layer 6 Residual Y(mm) Top", 25, -3, 3);

        
        plotter3_1.region(0).plot(mod1ResX_Top);
        plotter3_1.region(2).plot(mod2ResX_Top);
        plotter3_1.region(4).plot(mod3ResX_Top);
        plotter3_1.region(6).plot(mod4ResX_Top);
        plotter3_1.region(8).plot(mod5ResX_Top);
        plotter3_1.region(10).plot(mod6ResX_Top);

        plotter3_1.region(1).plot(mod1ResY_Top);
        plotter3_1.region(3).plot(mod2ResY_Top);
        plotter3_1.region(5).plot(mod3ResY_Top);
        plotter3_1.region(7).plot(mod4ResY_Top);
        plotter3_1.region(9).plot(mod5ResY_Top);
        plotter3_1.region(11).plot(mod6ResY_Top);

        if(showPlots) plotter3_1.show();
        
        plotter3_2 = fac.createPlotterFactory().create("HPS Residual Plots (Single strip cluster per layer)");
        plotter3_2.setTitle("Residuals (Bottom)");
        //plotterFrame.addPlotter(plotter3_2);
        IPlotterStyle style3_2 = plotter3_2.style();
        style3_2.dataStyle().fillStyle().setColor("yellow");
        style3_2.dataStyle().errorBarStyle().setVisible(false);
        plotter3_2.createRegions(6, 2);

        IHistogram1D mod1ResX_Bottom = aida.histogram1D("Layer 1 Residual X(mm) Bottom", 25, -1, 1);
        IHistogram1D mod1ResY_Bottom = aida.histogram1D("Layer 1 Residual Y(mm) Bottom", 25, -0.04, 0.04);

        IHistogram1D mod2ResX_Bottom = aida.histogram1D("Layer 2 Residual X(mm) Bottom", 25, -2, 2);
        IHistogram1D mod2ResY_Bottom = aida.histogram1D("Layer 2 Residual Y(mm) Bottom", 25, -1, 1);

        IHistogram1D mod3ResX_Bottom = aida.histogram1D("Layer 3 Residual X(mm) Bottom", 25, -2.5, 2.5);
        IHistogram1D mod3ResY_Bottom = aida.histogram1D("Layer 3 Residual Y(mm) Bottom", 25, -1.5, 1.5);

        IHistogram1D mod4ResX_Bottom = aida.histogram1D("Layer 4 Residual X(mm) Bottom", 25, -3.0, 3.0);
        IHistogram1D mod4ResY_Bottom = aida.histogram1D("Layer 4 Residual Y(mm) Bottom", 25, -2, 2);

        IHistogram1D mod5ResX_Bottom = aida.histogram1D("Layer 5 Residual X(mm) Bottom", 25, -4, 4);
        IHistogram1D mod5ResY_Bottom = aida.histogram1D("Layer 5 Residual Y(mm) Bottom", 25, -3, 3);

        IHistogram1D mod6ResX_Bottom = aida.histogram1D("Layer 6 Residual X(mm) Bottom", 25, -5, 5);
        IHistogram1D mod6ResY_Bottom = aida.histogram1D("Layer 6 Residual Y(mm) Bottom", 25, -3, 3);

        plotter3_2.region(0).plot(mod1ResX_Bottom);
        plotter3_2.region(2).plot(mod2ResX_Bottom);
        plotter3_2.region(4).plot(mod3ResX_Bottom);
        plotter3_2.region(6).plot(mod4ResX_Bottom);
        plotter3_2.region(8).plot(mod5ResX_Bottom);
        plotter3_2.region(10).plot(mod6ResX_Bottom);

        plotter3_2.region(1).plot(mod1ResY_Bottom);
        plotter3_2.region(3).plot(mod2ResY_Bottom);
        plotter3_2.region(5).plot(mod3ResY_Bottom);
        plotter3_2.region(7).plot(mod4ResY_Bottom);
        plotter3_2.region(9).plot(mod5ResY_Bottom);
        plotter3_2.region(11).plot(mod6ResY_Bottom);
        
        if(showPlots) plotter3_2.show();

        plotter4 = fac.createPlotterFactory().create("HPS Track and ECal Plots");
        plotter4.setTitle("Track and ECal Correlations");
        //plotterFrame.addPlotter(plotter4);
        IPlotterStyle style4 = plotter4.style();
        style4.setParameter("hist2DStyle", "colorMap");
        style4.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        style4.dataStyle().fillStyle().setColor("yellow");
        style4.dataStyle().errorBarStyle().setVisible(false);
        plotter4.createRegions(2, 3);

        IHistogram2D eVsP = aida.histogram2D("Energy Vs Momentum", 50, 0, 0.50, 50, 0, 1.5);
        IHistogram1D eOverP = aida.histogram1D("Energy Over Momentum", 50, 0, 2);

        IHistogram1D distX = aida.histogram1D("deltaX", 50, -100, 100);
        IHistogram1D distY = aida.histogram1D("deltaY", 50, -40, 40);

        IHistogram2D xEcalVsTrk = aida.histogram2D("X ECal Vs Track", 100, -400, 400, 100, -400, 400);
        IHistogram2D yEcalVsTrk = aida.histogram2D("Y ECal Vs Track", 100, -100, 100, 100, -100, 100);

        plotter4.region(0).plot(eVsP);
        plotter4.region(3).plot(eOverP);
        plotter4.region(1).plot(distX);
        plotter4.region(4).plot(distY);
        plotter4.region(2).plot(xEcalVsTrk);
        plotter4.region(5).plot(yEcalVsTrk);

        if(showPlots) plotter4.show();

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

        IHistogram2D topeVsP = aida.histogram2D("Top Energy Vs Momentum", 50, 0, 0.500, 50, 0, 1.5);
        IHistogram1D topeOverP = aida.histogram1D("Top Energy Over Momentum", 50, 0, 2);

        IHistogram1D topdistX = aida.histogram1D("Top deltaX", 50, -100, 100);
        IHistogram1D topdistY = aida.histogram1D("Top deltaY", 50, -40, 40);

        IHistogram2D topxEcalVsTrk = aida.histogram2D("Top X ECal Vs Track", 100, -400, 400, 100, -100, 100);
        IHistogram2D topyEcalVsTrk = aida.histogram2D("Top Y ECal Vs Track", 100, 0, 100, 100, 0, 100);

        top2.region(0).plot(topeVsP);
        top2.region(3).plot(topeOverP);
        top2.region(1).plot(topdistX);
        top2.region(4).plot(topdistY);
        top2.region(2).plot(topxEcalVsTrk);
        top2.region(5).plot(topyEcalVsTrk);

        if(showPlots) top2.show();
        
        bot2 = fac.createPlotterFactory().create("Bottom ECal Plots");
        bot2.setTitle("Bottom ECal Correlations");
        IPlotterStyle sbot2 = bot2.style();
        sbot2.dataStyle().fillStyle().setColor("green");
        sbot2.dataStyle().errorBarStyle().setVisible(false);
        sbot2.setParameter("hist2DStyle", "colorMap");
        sbot2.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        bot2.createRegions(2, 3);
        //bottomFrame.addPlotter(bot2);

        IHistogram2D BottomeVsP = aida.histogram2D("Bottom Energy Vs Momentum", 50, 0, 0.500, 50, 0, 1.5);
        IHistogram1D BottomeOverP = aida.histogram1D("Bottom Energy Over Momentum", 50, 0, 2);

        IHistogram1D BottomdistX = aida.histogram1D("Bottom deltaX", 50, -100, 100);
        IHistogram1D BottomdistY = aida.histogram1D("Bottom deltaY", 50, -40, 40);

        IHistogram2D BottomxEcalVsTrk = aida.histogram2D("Bottom X ECal Vs Track", 100, -400, 400, 100, -400, 400);
        IHistogram2D BottomyEcalVsTrk = aida.histogram2D("Bottom Y ECal Vs Track", 100, -100, 0, 100, -100, 0);

        bot2.region(0).plot(BottomeVsP);
        bot2.region(3).plot(BottomeOverP);
        bot2.region(1).plot(BottomdistX);
        bot2.region(4).plot(BottomdistY);
        bot2.region(2).plot(BottomxEcalVsTrk);
        bot2.region(5).plot(BottomyEcalVsTrk);

        if(showPlots) bot2.show();
        
        
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

        IHistogram2D topdistXvsX = aida.histogram2D("Top deltaX vs X", 51, -400, 400, 25, -100, 100);
        IHistogram2D topdistYvsY = aida.histogram2D("Top deltaY vs Y", 51, 0, 100, 25, -40, 40);

        top3.region(0).plot(topdistXvsX);
        top3.region(1).plot(topdistYvsY);

        if(showPlots) top3.show();
        
        bot3 = fac.createPlotterFactory().create("Bottom ECal Plots");
        bot3.setTitle("Bottom ECal More Correlations");
        IPlotterStyle sbot3 = bot3.style();
        sbot3.dataStyle().fillStyle().setColor("green");
        sbot3.dataStyle().errorBarStyle().setVisible(false);
        sbot3.setParameter("hist2DStyle", "colorMap");
        sbot3.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        bot3.createRegions(1, 2);
        //bottomFrame.addPlotter(bot3);

        IHistogram2D botdistXvsX = aida.histogram2D("Bottom deltaX vs X", 51, -400, 400, 25, -100, 100);
        IHistogram2D botdistYvsY = aida.histogram2D("Bottom deltaY vs Y", 51, -100, 0, 25, -40, 40);

        bot3.region(0).plot(botdistXvsX);
        bot3.region(1).plot(botdistYvsY);

        if(showPlots) bot3.show();
        
        //   ******************************************************************
        top4 = fac.createPlotterFactory().create("Track Matching Plots");
        top4.setTitle("Track Matching Plots");
        IPlotterStyle stop4 = top4.style();
        stop4.dataStyle().fillStyle().setColor("green");
        stop4.dataStyle().errorBarStyle().setVisible(false);
        stop4.setParameter("hist2DStyle", "colorMap");
        stop4.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        top4.createRegions(2, 3);
        //topFrame.addPlotter(top4);

        IHistogram1D trackmatchN = aida.histogram1D("Tracks matched", 3, 0, 3);
        IHistogram1D toptrackmatchN = aida.histogram1D("Tracks matched Top", 3, 0, 3);
        IHistogram1D bottrackmatchN = aida.histogram1D("Tracks matched Bottom", 3, 0, 3);
        IHistogram1D trackmatchN2 = aida.histogram1D("Tracks matched (Pz>0.8)", 3, 0, 3);
        IHistogram1D toptrackmatchN2 = aida.histogram1D("Tracks matched Top (Pz>0.8)", 3, 0, 3);
        IHistogram1D bottrackmatchN2 = aida.histogram1D("Tracks matched Bottom (Pz>0.8)", 3, 0, 3);
        
        top4.region(0).plot(trackmatchN);
        top4.region(1).plot(toptrackmatchN);
        top4.region(2).plot(bottrackmatchN);
        top4.region(3).plot(trackmatchN2);
        top4.region(4).plot(toptrackmatchN2);
        top4.region(5).plot(bottrackmatchN2);

        if(showPlots) top4.show();
        
        //   ******************************************************************
        top44 = fac.createPlotterFactory().create("e+e- Plots");
        top44.setTitle("e+e- Plots");
        IPlotterStyle stop44 = top44.style();
        stop44.dataStyle().fillStyle().setColor("green");
        stop44.dataStyle().errorBarStyle().setVisible(false);
        stop44.setParameter("hist2DStyle", "colorMap");
        stop44.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        top44.createRegions(2,4);
        //topFrame.addPlotter(top44);

        IHistogram2D trackPCorr = aida.histogram2D("p(e-) vs p(e+) max", 25, 0, 1.2, 25, 0, 1.2);
        IHistogram1D ne = aida.histogram1D("n(e-)", 3, 0, 3);
        IHistogram1D np = aida.histogram1D("n(e+)", 3, 0, 3);
        IHistogram1D pem = aida.histogram1D("p(e-) max", 25, 0, 1.5);
        IHistogram1D pe = aida.histogram1D("p(e-)", 25, 0, 1.5);
        IHistogram1D ppm = aida.histogram1D("p(e+) max", 25, 0, 1.5);
        IHistogram1D pp = aida.histogram1D("p(e+)", 25, 0, 1.5);
        
        top44.region(0).plot(trackPCorr);
        top44.region(1).plot(ne);
        top44.region(2).plot(np);
        top44.region(3).plot(pe);
        top44.region(4).plot(pp);
        top44.region(5).plot(pem);
        top44.region(6).plot(ppm);
        
        if(showPlots) top44.show();
        
        
        
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
        
        if(showPlots) plotter5.show();
        
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

        if(showPlots) plotter5_1.show();

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

        if(showPlots) plotter55.show();

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
        IHistogram2D topECal2 = aida.histogram2D("Top ECal Cluster Position (E>0.1,>0 tracks)", 50, -400, 400, 10, 0, 100);
        IHistogram2D botECal2 = aida.histogram2D("Bottom ECal Cluster Position (E>0.1,>0 tracks)", 50, -400, 400, 10, -100, 0);
        IHistogram2D topECal3 = aida.histogram2D("Top ECal Cluster Position w_E (E>0.1,>0 tracks)", 50, -400, 400, 10, 0, 100);
        IHistogram2D botECal3 = aida.histogram2D("Bottom ECal Cluster Position w_E (E>0.1,>0 tracks)", 50, -400, 400, 10, -100, 0);

        plotter6.region(0).plot(topECal);
        plotter6.region(1).plot(botECal);
        plotter6.region(2).plot(topECal1);
        plotter6.region(3).plot(botECal1);
        plotter6.region(4).plot(topECal2);
        plotter6.region(5).plot(botECal2);
        plotter6.region(6).plot(topECal3);
        plotter6.region(7).plot(botECal3);
        
        if(showPlots) plotter6.show();
        
        
        plotter66 = fac.createPlotterFactory().create("HPS ECAL Basic Plots");
        plotter66.setTitle("ECAL Basic Plots");
        //plotterFrame.addPlotter(plotter6);
        IPlotterStyle style66 = plotter66.style();
        style66.dataStyle().fillStyle().setColor("yellow");
        style66.dataStyle().errorBarStyle().setVisible(false);
        plotter66.createRegions(2, 2);

        IHistogram1D topECalE = aida.histogram1D("Top ECal Cluster Energy", 50, 0, 2);
        IHistogram1D botECalE = aida.histogram1D("Bottom ECal Cluster Energy", 50, 0, 2);
        IHistogram1D topECalN = aida.histogram1D("Number of Clusters Top", 6, 0, 6);
        IHistogram1D botECalN = aida.histogram1D("Number of Clusters Bot", 6, 0, 6);
        
        plotter66.region(0).plot(topECalE);
        plotter66.region(1).plot(botECalE);
        plotter66.region(2).plot(botECalN);
        plotter66.region(3).plot(topECalN);
        
        if(showPlots) plotter66.show();
        
        
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
        
        if(showPlots) plotter7.show();
        
        plotter8 = fac.createPlotterFactory().create("HPS Strip Hit Multiplicity");
        plotter8.setTitle("Strip Hit Multiplicity");
        //plotterFrame.addPlotter(plotter8);
        IPlotterStyle style8 = plotter8.style();
        style8.dataStyle().fillStyle().setColor("yellow");
        style8.dataStyle().errorBarStyle().setVisible(false);
        plotter8.createRegions(6, 6);
        i=0;
        for(SiSensor sensor : sensors) {
            IHistogram1D resX = aida.histogram1D(sensor.getName() + " strip hits", 10, 0, 10);
            plotter8.region(i).plot(resX);
            i++;
        }

        if(showPlots) plotter8.show();
        
        
        plotter9 = fac.createPlotterFactory().create("HPS Strip Hit On Track Multiplicity");
        plotter9.setTitle("Strip Hit Multiplicity");
        //plotterFrame.addPlotter(plotter9);
        IPlotterStyle style9 = plotter9.style();
        style9.dataStyle().fillStyle().setColor("yellow");
        style9.dataStyle().errorBarStyle().setVisible(false);
        plotter9.createRegions(6, 6);
        i=0;
        for(SiSensor sensor : sensors) {
            IHistogram1D resX = aida.histogram1D(sensor.getName() + " strip hits on track", 3, 0, 3);
            plotter9.region(i).plot(resX);
            i++;
        }

        if(showPlots) plotter9.show();


    }

    public TrackingReconstructionPlots() {
        logger.setLevel(Level.WARNING);
    }

    public void setOutputPlots(String output) {
        this.outputPlots = output;
    }
    
    public void setShowPlots(boolean show) {
        this.showPlots  = show;
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
        List<HelicalTrackHit> hthList = event.get(HelicalTrackHit.class, helicalTrackHitCollectionName);
        int[] layersTop = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        int[] layersBot = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        Map<HpsSiSensor, Integer> stripHits = new HashMap<HpsSiSensor, Integer>();
        for (HelicalTrackHit hth : hthList) {
            HelicalTrackCross htc = (HelicalTrackCross) hth;
            HpsSiSensor sensor = ((HpsSiSensor) ((RawTrackerHit) htc.getRawHits().get(0)).getDetectorElement());
            for(HelicalTrackStrip strip : htc.getStrips()) {
                HpsSiSensor stripsensor = (HpsSiSensor) ((RawTrackerHit)strip.rawhits().get(0)).getDetectorElement();
                if(stripHits.containsKey(stripsensor)) {
                    stripHits.put(stripsensor, stripHits.get(stripsensor) + 1);
                } else {
                    stripHits.put(stripsensor, 0);
                }
            }
            if(sensor.isTopLayer()){
                layersTop[htc.Layer() - 1]++;
            } else {
                layersBot[htc.Layer() - 1]++;
            }
        }
        for(Map.Entry<HpsSiSensor,Integer> sensor : stripHits.entrySet()) {
            aida.histogram1D(sensor.getKey().getName() + " strip hits").fill(sensor.getValue());
        }
        
        for (int i = 0; i < 12; i++) {
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
        int nBotClusters = 0;
        int nTopClusters = 0;
        if (event.hasCollection(Cluster.class, ecalCollectionName)) {
            List<Cluster> clusters = event.get(Cluster.class, ecalCollectionName);
            for (Cluster cluster : clusters) {
             // Get the ix and iy indices for the seed.
                final int ix = cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
                final int iy = cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
 
                //System.out.println("cluser position = ("+cluster.getPosition()[0]+","+cluster.getPosition()[1]+") with energy = "+cluster.getEnergy());
                if (cluster.getPosition()[1] > 0) {
                    nTopClusters++;
                    //System.out.println("cl " + cluster.getPosition()[0] + " " + cluster.getPosition()[1] + "  ix  " + ix + " iy " + iy);
                    aida.histogram2D("Top ECal Cluster Position").fill(cluster.getPosition()[0], cluster.getPosition()[1]);
                    aida.histogram1D("Top ECal Cluster Energy").fill(cluster.getEnergy());
                }
                if (cluster.getPosition()[1] < 0) {
                    nBotClusters++;
                    aida.histogram2D("Bottom ECal Cluster Position").fill(cluster.getPosition()[0], cluster.getPosition()[1]);
                    aida.histogram1D("Bottom ECal Cluster Energy").fill(cluster.getEnergy());
                }

                if (tracks.size() > 0) {
                    if (cluster.getPosition()[1] > 0) {
                        aida.histogram2D("Top ECal Cluster Position (>0 tracks)").fill(cluster.getPosition()[0], cluster.getPosition()[1]);
                    }
                    if (cluster.getPosition()[1] < 0) {
                        aida.histogram2D("Bottom ECal Cluster Position (>0 tracks)").fill(cluster.getPosition()[0], cluster.getPosition()[1]);
                    }

                    if (cluster.getEnergy() > 0.1) {
                        if (cluster.getPosition()[1] > 0) {
                            aida.histogram2D("Top ECal Cluster Position (E>0.1,>0 tracks)").fill(cluster.getPosition()[0], cluster.getPosition()[1]);
                            aida.histogram2D("Top ECal Cluster Position w_E (E>0.1,>0 tracks)").fill(cluster.getPosition()[0], cluster.getPosition()[1], cluster.getEnergy());
                        }
                        if (cluster.getPosition()[1] < 0) {
                            aida.histogram2D("Bottom ECal Cluster Position (E>0.1,>0 tracks)").fill(cluster.getPosition()[0], cluster.getPosition()[1]);
                            aida.histogram2D("Bottom ECal Cluster Position w_E (E>0.1,>0 tracks)").fill(cluster.getPosition()[0], cluster.getPosition()[1], cluster.getEnergy());
                        }
                    }
                }

            }
        }
        
        aida.histogram1D("Number of Clusters Top").fill(nTopClusters);
        aida.histogram1D("Number of Clusters Bot").fill(nBotClusters);
        

            
        

        //List<SiTrackerHitStrip1D> stripHits = event.get(SiTrackerHitStrip1D.class, "StripClusterer_SiTrackerHitStrip1D");
        //int stripClustersPerLayerTop[] = getStripClustersPerLayer(stripHits, "up");
        //int stripClustersPerLayerBottom[] = getStripClustersPerLayer(stripHits,"down");

        //boolean hasSingleStripClusterPerLayer = singleStripClusterPerLayer(stripClustersPerLayerTop);

        Map<Track, Cluster> eCanditates = new HashMap<Track, Cluster>();
        Map<Track, Cluster> pCanditates = new HashMap<Track, Cluster>();
        
        int ntracksTop = 0;
        int ntracksBot = 0;
        
        for (Track trk : tracks) {

            //boolean isSingleHitPerLayerTrack = singleTrackHitPerLayer(trk);

            aida.histogram1D("Track Momentum (Px)").fill(trk.getTrackStates().get(0).getMomentum()[1]);
            aida.histogram1D("Track Momentum (Py)").fill(trk.getTrackStates().get(0).getMomentum()[2]);
            aida.histogram1D("Track Momentum (Pz)").fill(trk.getTrackStates().get(0).getMomentum()[0]);
            aida.histogram1D("Track Chi2").fill(trk.getChi2());

            aida.histogram1D("Hits per Track").fill(trk.getTrackerHits().size());
            SeedTrack stEle = (SeedTrack) trk;
            SeedCandidate seedCandidate = stEle.getSeedCandidate();
            HelicalTrackFit helicalTrackFit = seedCandidate.getHelix();
            StraightLineTrack slt = converter.Convert(helicalTrackFit);

            Hep3Vector posAtEcal = TrackUtils.getTrackPositionAtEcal(trk);
            
            aida.histogram1D("X (mm) @ Z=-60cm").fill(slt.getYZAtX(BeamlineConstants.HARP_POSITION_TESTRUN)[0]);  //this is y in the tracker frame
            aida.histogram1D("Y (mm) @ Z=-60cm").fill(slt.getYZAtX(BeamlineConstants.HARP_POSITION_TESTRUN)[1]);  //this is z in the tracker frame
            aida.histogram1D("X (mm) @ Z=-150cm").fill(slt.getYZAtX(zAtColl)[0]);
            aida.histogram1D("Y (mm) @ Z=-150cm").fill(slt.getYZAtX(zAtColl)[1]);

            aida.histogram1D("X (mm) @ ECAL").fill(posAtEcal.x());
            aida.histogram1D("Y (mm) @ ECAL").fill(posAtEcal.y());
            if (trk.getTrackStates().get(0).getMomentum()[0] > 1.0) {
                aida.histogram1D("X (mm) @ ECAL (Pz>1)").fill(posAtEcal.x());
                aida.histogram1D("Y (mm) @ ECAL (Pz>1)").fill(posAtEcal.y());
            }
            aida.histogram1D("d0 ").fill(trk.getTrackStates().get(0).getParameter(ParameterName.d0.ordinal()));
            aida.histogram1D("sinphi ").fill(Math.sin(trk.getTrackStates().get(0).getParameter(ParameterName.phi0.ordinal())));
            aida.histogram1D("omega ").fill(trk.getTrackStates().get(0).getParameter(ParameterName.omega.ordinal()));
            aida.histogram1D("tan(lambda) ").fill(trk.getTrackStates().get(0).getParameter(ParameterName.tanLambda.ordinal()));
            aida.histogram1D("z0 ").fill(trk.getTrackStates().get(0).getParameter(ParameterName.z0.ordinal()));

            int isTop = -1;
            if (trk.getTrackerHits().get(0).getPosition()[2] > 0) {
                isTop = 0;//make plot look pretty
            }
            int charge = trk.getCharge();
            if (charge > 0) {
                charge = 0;//make plot look pretty
            }//            System.out.println("Charge = " + charge + "; isTop = " + isTop);
            aida.histogram2D("Charge vs Slope").fill(charge, isTop);
            if (isTop == 0) {
                aida.histogram1D("Top Track Momentum (Px)").fill(trk.getTrackStates().get(0).getMomentum()[1]);
                aida.histogram1D("Top Track Momentum (Py)").fill(trk.getTrackStates().get(0).getMomentum()[2]);
                aida.histogram1D("Top Track Momentum (Pz)").fill(trk.getTrackStates().get(0).getMomentum()[0]);
                aida.histogram1D("Top Track Chi2").fill(trk.getChi2());
                
                aida.histogram1D("d0 Top").fill(trk.getTrackStates().get(0).getParameter(ParameterName.d0.ordinal()));
                aida.histogram1D("sinphi Top").fill(Math.sin(trk.getTrackStates().get(0).getParameter(ParameterName.phi0.ordinal())));
                aida.histogram1D("omega Top").fill(trk.getTrackStates().get(0).getParameter(ParameterName.omega.ordinal()));
                aida.histogram1D("tan(lambda) Top").fill(trk.getTrackStates().get(0).getParameter(ParameterName.tanLambda.ordinal()));
                aida.histogram1D("z0 Top").fill(trk.getTrackStates().get(0).getParameter(ParameterName.z0.ordinal()));
                ntracksTop++;
            } else {
                aida.histogram1D("Bottom Track Momentum (Px)").fill(trk.getTrackStates().get(0).getMomentum()[1]);
                aida.histogram1D("Bottom Track Momentum (Py)").fill(trk.getTrackStates().get(0).getMomentum()[2]);
                aida.histogram1D("Bottom Track Momentum (Pz)").fill(trk.getTrackStates().get(0).getMomentum()[0]);
                aida.histogram1D("Bottom Track Chi2").fill(trk.getChi2());

                aida.histogram1D("d0 Bottom").fill(trk.getTrackStates().get(0).getParameter(ParameterName.d0.ordinal()));
                aida.histogram1D("sinphi Bottom").fill(Math.sin(trk.getTrackStates().get(0).getParameter(ParameterName.phi0.ordinal())));
                aida.histogram1D("omega Bottom").fill(trk.getTrackStates().get(0).getParameter(ParameterName.omega.ordinal()));
                aida.histogram1D("tan(lambda) Bottom").fill(trk.getTrackStates().get(0).getParameter(ParameterName.tanLambda.ordinal()));
                aida.histogram1D("z0 Bottom").fill(trk.getTrackStates().get(0).getParameter(ParameterName.z0.ordinal()));
                ntracksBot++;
            }
            List<TrackerHit> hitsOnTrack = trk.getTrackerHits();
            Map<HpsSiSensor, Integer> stripHitsOnTrack = new HashMap<HpsSiSensor, Integer>();
            
            for (TrackerHit hit : hitsOnTrack) {

                HelicalTrackHit htc = (HelicalTrackHit) hit;
                HelicalTrackCross htcross = (HelicalTrackCross) htc;
                double sHit = helicalTrackFit.PathMap().get(htc);
                Hep3Vector posonhelix = HelixUtils.PointOnHelix(helicalTrackFit, sHit);
                boolean isTopLayer = false;
                
                
                
                //HpsSiSensor sensor = ((HpsSiSensor) ((RawTrackerHit)  htc.getRawHits().get(0)).getDetectorElement());
                for(HelicalTrackStrip strip : htcross.getStrips()) {
                    HpsSiSensor sensor =  ((HpsSiSensor) ((RawTrackerHit)  strip.rawhits().get(0)).getDetectorElement());
                    if(sensor.isTopLayer()) isTopLayer = true;
                    else isTopLayer=false;
                    HelicalTrackStripGbl stripGbl = new HelicalTrackStripGbl(strip, true);
                    Map<String, Double> stripResiduals = TrackUtils.calculateLocalTrackHitResiduals(helicalTrackFit, stripGbl, 0.,0.,_bfield);
                    logger.info("Sensor " + sensor.getName() + " ures = " + stripResiduals.get("ures"));
                    aida.histogram1D(sensor.getName() + " strip residual (mm)").fill(stripResiduals.get("ures"));
                    

                    if(stripHitsOnTrack.containsKey(sensor)) {
                        stripHitsOnTrack.put(sensor, stripHitsOnTrack.get(sensor) + 1);
                    } else {
                        stripHitsOnTrack.put(sensor, 1);
                    }
                }
                
                
                   
                
                
                double yTr = posonhelix.y();
                double zTr = posonhelix.z();
                int layer = htc.Layer();
                String modNum = "Layer X ";
                if (layer == 1) {
                    modNum = "Layer 1 ";
                }
                if (layer == 3) {
                    modNum = "Layer 2 ";
                }
                if (layer == 5) {
                    modNum = "Layer 3 ";
                }
                if (layer == 7) {
                    modNum = "Layer 4 ";
                }
                if (layer == 9) {
                    modNum = "Layer 5 ";
                }
                if (layer == 11) {
                    modNum = "Layer 6 ";
                }
                //SymmetricMatrix cov = htc.getCorrectedCovMatrix();

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
                double x = htcross.getCorrectedPosition().y();
                double y = htcross.getCorrectedPosition().z();
                if(isTopLayer) {
                    layersTop[htc.Layer() - 1]++;
                    Hep3Vector sensorPos = ((SiSensor) ((RawTrackerHit) htc.getRawHits().get(0)).getDetectorElement()).getGeometry().getPosition();
                    if (htc.Layer() == 1) {
//                    System.out.println(sensorPos.toString());
//                    System.out.println("Hit X = " + x + "; Hit Y = " + y);
                        aida.histogram2D("Layer 1 HTH Position:  Top").fill(x - sensorPos.x(), y - sensorPos.y());
                    }
                    if (htc.Layer() == 7) {
                        aida.histogram2D("Layer 7 HTH Position:  Top").fill(x - sensorPos.x(), y - sensorPos.y());
                    }
                } else {
                    layersBot[htc.Layer() - 1]++;
                    Hep3Vector sensorPos = ((SiSensor) ((RawTrackerHit) htc.getRawHits().get(0)).getDetectorElement()).getGeometry().getPosition();
                    if (htc.Layer() == 1) {
//                    System.out.println(sensorPos.toString());
//                    System.out.println("Hit X = " + x + "; Hit Y = " + y);
                        aida.histogram2D("Layer 1 HTH Position:  Bottom").fill(x - sensorPos.x(), y - sensorPos.y());
                    }
                    if (htc.Layer() == 7) {
                        aida.histogram2D("Layer 7 HTH Position:  Bottom").fill(x - sensorPos.x(), y - sensorPos.y());
                    }
                }
                
                boolean doAmplitudePlots = true;
                if(doAmplitudePlots) {
                    for (HelicalTrackStrip hts : htcross.getStrips()) {
                        double clusterSum = 0;
                        double clusterT0 = 0;
                        int nHitsCluster = 0;
                                
                        for (RawTrackerHit rawHit : (List<RawTrackerHit>) hts.rawhits()) {
                            if(event.hasCollection(LCRelation.class, "SVTFittedRawTrackerHits")) {
                                List<LCRelation> fittedHits = event.get(LCRelation.class, "SVTFittedRawTrackerHits");
                                for(LCRelation fittedHit : fittedHits) {
                                    if(rawHit.equals((RawTrackerHit)fittedHit.getFrom())) {
                                        double amp = FittedRawTrackerHit.getAmp(fittedHit);
                                        double t0 = FittedRawTrackerHit.getT0(fittedHit);
                                        //System.out.println("to="+t0 + " amp=" + amp);
                                        aida.histogram1D("Amp (HitOnTrack)").fill(amp);
                                        if (trk.getTrackStates().get(0).getMomentum()[0] > 0.8) {
                                            aida.histogram1D("Amp Pz>0.8 (HitOnTrack)").fill(amp);
                                        }
                                        aida.histogram1D("t0 (HitOnTrack)").fill(t0);
                                        if (trk.getTrackStates().get(0).getMomentum()[0] > 0.8) {
                                            aida.histogram1D("t0 Pz>0.8 (HitOnTrack)").fill(t0);
                                        }
                                        clusterSum += amp;
                                        clusterT0 += t0;
                                        nHitsCluster++;
                                    }
                                }     
                                }
                        }
                           
                        aida.histogram1D("Hits in Cluster (HitOnTrack)").fill(nHitsCluster);
                        aida.histogram1D("Cluster Amp (HitOnTrack)").fill(clusterSum);
                        if (trk.getTrackStates().get(0).getMomentum()[0] > 0.8) {
                            aida.histogram1D("Cluster Amp Pz>0.8 (HitOnTrack)").fill(clusterSum);
                        }
                        if(nHitsCluster>0) {
                            aida.histogram1D("Cluster t0 (HitOnTrack)").fill(clusterT0/nHitsCluster);
                            if (trk.getTrackStates().get(0).getMomentum()[0] > 0.8) {
                                aida.histogram1D("Cluster t0 Pz>0.8 (HitOnTrack)").fill(clusterT0/nHitsCluster);
                            }
                        }
                    
                    }
                }
            }
            
            for(Map.Entry<HpsSiSensor,Integer> sensor : stripHitsOnTrack.entrySet()) {
                aida.histogram1D(sensor.getKey().getName() + " strip hits on track").fill(sensor.getValue());
            }
            
            
            Cluster clust = null;
            if(event.hasCollection(Cluster.class,ecalCollectionName)) {
                List<Cluster> clusters = event.get(Cluster.class, ecalCollectionName);

                Cluster cand_clust = findClosestCluster(posAtEcal, clusters);

                if (cand_clust != null) {

                    // track matching requirement
                    if(Math.abs( posAtEcal.x() - cand_clust.getPosition()[0])<30.0 && 
                            Math.abs( posAtEcal.y() - cand_clust.getPosition()[1])<30.0) 
                    {
                        clust = cand_clust;
                        if(trk.getCharge()<0) pCanditates.put(trk, clust);
                        else                  eCanditates.put(trk, clust);

                        posAtEcal = TrackUtils.extrapolateTrack(trk, clust.getPosition()[2]);//.positionAtEcal();

                        aida.histogram2D("Energy Vs Momentum").fill(clust.getEnergy(), trk.getTrackStates().get(0).getMomentum()[0]);
                        aida.histogram1D("Energy Over Momentum").fill(clust.getEnergy() / (trk.getTrackStates().get(0).getMomentum()[0] ));
                        aida.histogram1D("deltaX").fill(clust.getPosition()[0] - posAtEcal.x());
                        aida.histogram1D("deltaY").fill(clust.getPosition()[1] - posAtEcal.y());
                        aida.histogram2D("X ECal Vs Track").fill(clust.getPosition()[0], posAtEcal.x());
                        aida.histogram2D("Y ECal Vs Track").fill(clust.getPosition()[1], posAtEcal.y());

                        if (isTop == 0) {
                            aida.histogram1D("Tracks matched Top").fill(1);
                            if(trk.getTrackStates().get(0).getMomentum()[0] > 0.8){
                                aida.histogram1D("Tracks matched Top (Pz>0.8)").fill(1);
                            }

                            aida.histogram2D("Top Energy Vs Momentum").fill(clust.getEnergy(), trk.getTrackStates().get(0).getMomentum()[0] );
                            //                    aida.histogram2D("Top Energy Vs Momentum").fill(posAtEcal.y(), trk.getTrackStates().get(0).getMomentum()[0]);
                            aida.histogram1D("Top Energy Over Momentum").fill(clust.getEnergy() / (trk.getTrackStates().get(0).getMomentum()[0]));
                            aida.histogram1D("Top deltaX").fill(clust.getPosition()[0] - posAtEcal.x());
                            aida.histogram1D("Top deltaY").fill(clust.getPosition()[1] - posAtEcal.y());
                            aida.histogram2D("Top deltaX vs X").fill(clust.getPosition()[0], clust.getPosition()[0] - posAtEcal.x());
                            aida.histogram2D("Top deltaY vs Y").fill(clust.getPosition()[1], clust.getPosition()[1] - posAtEcal.y());
                            aida.histogram2D("Top X ECal Vs Track").fill(clust.getPosition()[0], posAtEcal.x());
                            aida.histogram2D("Top Y ECal Vs Track").fill(clust.getPosition()[1], posAtEcal.y());
                        } else {
                            aida.histogram1D("Tracks matched Bottom").fill(1);
                            if(trk.getTrackStates().get(0).getMomentum()[0] > 0.8){
                                aida.histogram1D("Tracks matched Bottom (Pz>0.8)").fill(1);
                            }
                            aida.histogram2D("Bottom Energy Vs Momentum").fill(clust.getEnergy(), trk.getTrackStates().get(0).getMomentum()[0] );
                            aida.histogram1D("Bottom Energy Over Momentum").fill(clust.getEnergy() / (trk.getTrackStates().get(0).getMomentum()[0]));
                            aida.histogram1D("Bottom deltaX").fill(clust.getPosition()[0] - posAtEcal.x());
                            aida.histogram1D("Bottom deltaY").fill(clust.getPosition()[1] - posAtEcal.y());
                            aida.histogram2D("Bottom deltaX vs X").fill(clust.getPosition()[0], clust.getPosition()[0] - posAtEcal.x());
                            aida.histogram2D("Bottom deltaY vs Y").fill(clust.getPosition()[1], clust.getPosition()[1] - posAtEcal.y());
                            aida.histogram2D("Bottom X ECal Vs Track").fill(clust.getPosition()[0], posAtEcal.x());
                            aida.histogram2D("Bottom Y ECal Vs Track").fill(clust.getPosition()[1], posAtEcal.y());
                        }
                    }
                } 
            }

            if (clust != null) {
                aida.histogram1D("Tracks matched").fill(0);
                if(trk.getTrackStates().get(0).getMomentum()[0] > 0.8){
                    aida.histogram1D("Tracks matched (Pz>0.8)").fill(0);
                }
                if(isTop == 0) {
                    aida.histogram1D("Tracks matched Top").fill(0);
                    if(trk.getTrackStates().get(0).getMomentum()[0] > 0.8){
                        aida.histogram1D("Tracks matched Top (Pz>0.8)").fill(0);
                    }
                } else {
                    aida.histogram1D("Tracks matched Bottom").fill(0);    
                    if(trk.getTrackStates().get(0).getMomentum()[0] > 0.8){
                        aida.histogram1D("Tracks matched Bottom (Pz>0.8)").fill(0);
                    }
                }
            } else {
                aida.histogram1D("Tracks matched").fill(1);
                if(trk.getTrackStates().get(0).getMomentum()[0] > 0.8){
                    aida.histogram1D("Tracks matched (Pz>0.8)").fill(1);
                }

                if (isTop == 0) {
                    aida.histogram1D("Tracks matched Top").fill(1);
                    if(trk.getTrackStates().get(0).getMomentum()[0] > 0.8){
                        aida.histogram1D("Tracks matched Top (Pz>0.8)").fill(1);
                    }
                } else {
                    aida.histogram1D("Tracks matched Bottom").fill(1);
                    if(trk.getTrackStates().get(0).getMomentum()[0] > 0.8){
                        aida.histogram1D("Tracks matched Bottom (Pz>0.8)").fill(1);
                    }
                }

            }

        }

        nTracksBot.fill(ntracksBot);
        nTracksTop.fill(ntracksTop);
        
        // e+/e-
        Map.Entry<Track, Cluster> ecand_highestP = null;
        double e_pmax = -1;
        Map.Entry<Track, Cluster> pcand_highestP = null;
        double p_pmax = -1;
        for(Map.Entry<Track, Cluster> ecand : eCanditates.entrySet()) {
            double p = getMomentum(ecand.getKey());
            aida.histogram1D("p(e-)").fill(p);
            if(ecand_highestP == null ) {
                ecand_highestP = ecand;
                e_pmax = getMomentum(ecand_highestP.getKey());
             } else {
                if(p > e_pmax) {
                    ecand_highestP = ecand;
                    e_pmax = getMomentum(ecand_highestP.getKey());
                }
             }
        }
        
        for(Map.Entry<Track, Cluster> pcand : pCanditates.entrySet()) {
            double p = getMomentum(pcand.getKey());
            aida.histogram1D("p(e+)").fill(p);
            if(pcand_highestP == null ) {
                pcand_highestP = pcand;
                p_pmax = getMomentum(pcand_highestP.getKey());
             } else {
                if(p > p_pmax) {
                    pcand_highestP = pcand;
                    p_pmax = getMomentum(pcand_highestP.getKey());
                }
             }
        }
        
        aida.histogram1D("n(e-)").fill(eCanditates.size());
        aida.histogram1D("n(e+)").fill(pCanditates.size());
        if(ecand_highestP!=null) {
            aida.histogram1D("p(e-) max").fill(e_pmax);
        }
        if(pcand_highestP!=null) {
            aida.histogram1D("p(e+) max").fill(p_pmax);
        }
        if(ecand_highestP!=null && pcand_highestP!=null) {
            aida.histogram2D("p(e-) vs p(e+) max").fill(e_pmax, p_pmax);
        }
        
        
    }
    
    private double getMomentum(Track trk) {
        double p = Math.sqrt(trk.getTrackStates().get(0).getMomentum()[0]*trk.getTrackStates().get(0).getMomentum()[0] +
                trk.getTrackStates().get(0).getMomentum()[1]*trk.getTrackStates().get(0).getMomentum()[1] +
             trk.getTrackStates().get(0).getMomentum()[2]*trk.getTrackStates().get(0).getMomentum()[2]);
        return p;
    }

    public int[] getTrackHitsPerLayer(Track trk) {
        int n[] = {0, 0, 0, 0, 0, 0};
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
        int n[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
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
                if (stripCluster.getPosition()[1] >= 0.0) {
                    s = "up";
                } else {
                    s = "down";
                }
                if (!s.equals(side)) {
                    continue;
                }
            }

            name = stripCluster.getSensor().getName();
            if (name.length() < 14) {
                System.err.println("This name is too short!!");
                throw new RuntimeException("SiSensor name " + name + " is invalid?");
            }

            if (ddd) {
                System.out.println("sensor name  " + name);
            }
            
            if(name.contains("layer") && name.contains("_module")) {
                //String str_l = name.substring(13);
                String str_l = name.substring(name.indexOf("layer") + 5, name.indexOf("_module"));
                l = Integer.parseInt(str_l);
            }
            else if(name.contains("module") && name.contains("_halfmodule")) {
                int ll = HPSTrackerBuilder.getLayerFromVolumeName(name);
                boolean isAxial = HPSTrackerBuilder.isAxialFromName(name);
                boolean isTopLayer = HPSTrackerBuilder.getHalfFromName(name).equals("top") ? true : false;
                if(isAxial) {
                    if(isTopLayer) {
                        l = 2*ll-1;
                    }
                    else {
                        l = 2*ll;
                    }
                } else {
                    if(isTopLayer) {
                        l = 2*ll;
                    } else {
                        l = 2*ll-1;
                    }
                }
            } else {
                throw new RuntimeException("Cannot get layer from name " + name);
            }
            
            if (ddd) {
                System.out.println("sensor name  " + name + " --> layer " + l);
            }

            if (l < 1 || l > 12) {
                System.out.println("This layer doesn't exist?");
                throw new RuntimeException("SiSensor name " + name + " is invalid?");
            }

            n[l - 1] = n[l - 1] + 1;

        }

        return n;
    }

    @Override
    public void endOfData() {
        if (outputPlots != null) {
            try {
                aida.saveAs(outputPlots);
            } catch (IOException ex) {
                Logger.getLogger(TrackingReconstructionPlots.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        //plotterFrame.dispose();
        //topFrame.dispose();
        //bottomFrame.dispose();
    }


    
    private Cluster findClosestCluster(Hep3Vector posonhelix, List<Cluster> clusters) {
        Cluster closest = null;
        double minDist = 9999;
        for (Cluster cluster : clusters) {
            double[] clPos = cluster.getPosition();
            double clEne = cluster.getEnergy();
            double dist = Math.sqrt(Math.pow(clPos[0] - posonhelix.x(), 2) + Math.pow(clPos[1] - posonhelix.y(), 2)); //coordinates!!!
            double distX = Math.abs(clPos[0] - posonhelix.x());
            double distY = Math.abs(clPos[1] - posonhelix.y());
            if (dist < minDist && clEne > 0.4) {
                closest = cluster;
                minDist = dist;
            }
        }
        return closest;
    }
}
