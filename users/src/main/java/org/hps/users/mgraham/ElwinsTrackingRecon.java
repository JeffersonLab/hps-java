package org.hps.users.mgraham;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.Hep3Vector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.deprecated.BeamlineConstants;
import org.hps.conditions.deprecated.SvtUtils;
import org.hps.recon.ecal.HPSEcalCluster;
import org.hps.recon.tracking.DumbShaperFit;
import org.hps.recon.tracking.HPSShaperFitAlgorithm;
import org.hps.recon.tracking.HPSTrack;
import org.hps.recon.tracking.HelixConverter;
import org.hps.recon.tracking.StraightLineTrack;
import org.hps.util.AIDAFrame;
import org.hps.util.Resettable;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

/**
 *
 * @author elwinm
 */
public class ElwinsTrackingRecon extends Driver implements Resettable {

    private AIDAFrame plotterFrame;
    private AIDAFrame topFrame;
    private AIDAFrame bottomFrame;
    private AIDAFrame chargeFrame;
    private AIDAFrame twotrackFrame;
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
    IPlotter plotter9000;
    IPlotter plotter9001;
    IPlotter plotter9002;
    IPlotter plotter9003;
    IPlotter plotter9004;
    IPlotter plotter9005;
    IPlotter plotter9006;
    IPlotter plotter9007;
    IPlotter plotter9008;
    IPlotter plotter9009;
    IPlotter plotter9010;
    IPlotter plotter9011;
    IPlotter plotter9012;
    IPlotter plotter9013;
    IPlotter plotter9014;
    IPlotter plotter9015;
    IPlotter plotter9016;
    IPlotter plotter9017;
    IPlotter twotrkextra;
    IPlotter twotrkextra2;
    IPlotter threetrack;
    IPlotter top1;
    IPlotter top2;
    IPlotter top3;
    IPlotter top4;
    IPlotter bot1;
    IPlotter bot2;
    IPlotter bot3;
    IPlotter charge;
    IPlotter bot4;
    double zEcal = 1500;
    double zAtDownStrPairSpec = 914.0; //mm
    double zAtColl = -1500;
    IHistogram1D trkPx;
    IHistogram1D nTracks;
    HPSShaperFitAlgorithm _shaper = new DumbShaperFit();

    protected void detectorChanged(Detector detector) {
        this.detector = detector;
        aida.tree().cd("/");
        plotterFrame = new AIDAFrame();
        plotterFrame.setTitle("HPS Tracking Plots");

        twotrackFrame = new AIDAFrame();
        twotrackFrame.setTitle("Two Track Plots");

        sensors = detector.getSubdetector(trackerName).getDetectorElement().findDescendants(SiSensor.class);

        IAnalysisFactory fac = aida.analysisFactory();
        plotter = fac.createPlotterFactory().create("HPS Tracking Plots");
        plotter.setTitle("Momentum");
        IPlotterStyle style = plotter.style();
        style.dataStyle().fillStyle().setColor("yellow");
        style.dataStyle().errorBarStyle().setVisible(false);
        plotter.createRegions(2, 2);
        plotterFrame.addPlotter(plotter);

        trkPx = aida.histogram1D("Track X Momentum", 25, -0.25, 0.25);
        IHistogram1D trkPy = aida.histogram1D("Track Y Momentum", 25, -0.1, 0.1);
        IHistogram1D trkPz = aida.histogram1D("Track Z Momentum", 25, 0, 3.5);
        IHistogram1D trkChi2 = aida.histogram1D("Track Chi2", 25, 0, 25.0);

        plotter.region(0).plot(trkPx);
        plotter.region(1).plot(trkPy);
        plotter.region(2).plot(trkPz);
        plotter.region(3).plot(trkChi2);


        plotter2 = fac.createPlotterFactory().create("HPS Tracking Plots");
        plotter2.setTitle("Track extrapolation");
        plotterFrame.addPlotter(plotter2);
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
        IHistogram1D xAtConvert = aida.histogram1D("X (mm) @ Converter", 50, -50, 50);
        IHistogram1D yAtConvert = aida.histogram1D("Y (mm) @ Converter", 50, -20, 20);

        plotter2.region(0).plot(xAtConverter);
        plotter2.region(4).plot(yAtConverter);
        plotter2.region(1).plot(xAtColl);
        plotter2.region(5).plot(yAtColl);
        plotter2.region(2).plot(xAtEcal);
        plotter2.region(6).plot(yAtEcal);
        plotter2.region(3).plot(xAtConvert);
        plotter2.region(7).plot(yAtConvert);

        twotrkextra = fac.createPlotterFactory().create("Two Trk Extrapolation");
        twotrkextra.setTitle("Stuff");
        plotterFrame.addPlotter(twotrkextra);
        IPlotterStyle styletwo = twotrkextra.style();
        styletwo.dataStyle().fillStyle().setColor("blue");
        styletwo.dataStyle().errorBarStyle().setVisible(false);
        twotrkextra.createRegions(3, 2);
        IHistogram1D x1AtTarget = aida.histogram1D("Trk1 X @ Target", 50, 0, 50);
        IHistogram1D y1AtTarget = aida.histogram1D("Trk1 Y @ Target", 50, -5, 5);
        IHistogram1D x2AtTarget = aida.histogram1D("Trk2 X @ Target", 50, 0, 50);
        IHistogram1D y2AtTarget = aida.histogram1D("Trk2 Y @ Target", 50, -5, 5);
        IHistogram1D distatt = aida.histogram1D("Distance btwn Trks @ Target", 40, 0, 40);
        IHistogram1D zdiff = aida.histogram1D("Z Diff", 40, -.1, .1);

        twotrkextra.region(0).plot(x1AtTarget);
        twotrkextra.region(1).plot(y1AtTarget);
        twotrkextra.region(2).plot(x2AtTarget);
        twotrkextra.region(3).plot(y2AtTarget);
        twotrkextra.region(4).plot(distatt);
        twotrkextra.region(5).plot(zdiff);


        plotter222 = fac.createPlotterFactory().create("HPS Tracking Plots");
        plotter222.setTitle("Other");
        plotterFrame.addPlotter(plotter222);
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


        plotterFrame.pack();
        plotterFrame.setVisible(true);


        twotrkextra2 = fac.createPlotterFactory().create("Two Trk Uncertainties");
        twotrkextra2.setTitle("Uncertainties");
        plotter9000 = fac.createPlotterFactory().create("Two Track Plots");
        plotter9000.setTitle("Two Track Plots Test");
        IPlotterStyle TwoTracks = plotter9000.style();
        plotter9001 = fac.createPlotterFactory().create("Two Track Plots 2");
        plotter9001.setTitle("Two Track Plots Test 2");
        IPlotterStyle TwoTracks1 = plotter9001.style();
        plotter9002 = fac.createPlotterFactory().create("Two Track Plots");
        plotter9002.setTitle("Two Track Plots Test");
        IPlotterStyle TwoTracks2 = plotter9002.style();
        plotter9003 = fac.createPlotterFactory().create("Two Track Plots");
        plotter9003.setTitle("Two Track Plots Test");
        IPlotterStyle TwoTracks3 = plotter9003.style();
        plotter9004 = fac.createPlotterFactory().create("Two Track Plots");
        plotter9004.setTitle("Two Track Plots Test");
        IPlotterStyle TwoTracks4 = plotter9004.style();
        plotter9005 = fac.createPlotterFactory().create("Two Track Plots");
        plotter9005.setTitle("Two Track Plots Test");
        IPlotterStyle TwoTracks5 = plotter9005.style();
        plotter9006 = fac.createPlotterFactory().create("Two Track Versus");
        plotter9006.setTitle("Two Track Versus");
        IPlotterStyle TwoTracks6 = plotter9006.style();
        plotter9007 = fac.createPlotterFactory().create("Two Track Plots");
        plotter9007.setTitle("Two Track Plots Test");
        IPlotterStyle TwoTracks7 = plotter9007.style();
        plotter9008 = fac.createPlotterFactory().create("Two Track Plots");
        plotter9008.setTitle("Two Track Plots Test");
        IPlotterStyle TwoTracks8 = plotter9000.style();
        plotter9009 = fac.createPlotterFactory().create("Two Track Plots");
        plotter9009.setTitle("Two Track Plots Test");
        IPlotterStyle TwoTracks9 = plotter9000.style();
        plotter9010 = fac.createPlotterFactory().create("Two Track Plots");
        plotter9010.setTitle("Two Track Plots Test");
        IPlotterStyle TwoTracks10 = plotter9010.style();
        plotter9011 = fac.createPlotterFactory().create("Two Track Plots");
        plotter9011.setTitle("Two Track Plots Test");
        IPlotterStyle TwoTracks11 = plotter9011.style();
        plotter9012 = fac.createPlotterFactory().create("Two Track Plotz");
        plotter9012.setTitle("Two Track Plotz Test");
        IPlotterStyle TwoTracks12 = plotter9012.style();
        plotter9013 = fac.createPlotterFactory().create("Two Track Plotz");
        plotter9013.setTitle("Two Track Plotz Test");
        IPlotterStyle TwoTracks13 = plotter9013.style();
        plotter9014 = fac.createPlotterFactory().create("Two Track Plotz");
        plotter9014.setTitle("Two Track Plotz Test");
        IPlotterStyle TwoTracks14 = plotter9014.style();
        plotter9015 = fac.createPlotterFactory().create("Two Track Plots");
        plotter9015.setTitle("Two Track Plots Test");
        IPlotterStyle TwoTracks15 = plotter9015.style();
        plotter9016 = fac.createPlotterFactory().create("Two Track Plots");
        plotter9016.setTitle("Test");
        IPlotterStyle TwoTracks16 = plotter9016.style();
        plotter9017 = fac.createPlotterFactory().create("Two Track Plots");
        plotter9017.setTitle("Residuals");
        IPlotterStyle TwoTracks17 = plotter9017.style();
        threetrack = fac.createPlotterFactory().create("Three Track Plots");
        threetrack.setTitle("Invariant Mass");


        TwoTracks.dataStyle().fillStyle().setColor("green");
        TwoTracks.dataStyle().errorBarStyle().setVisible(false);
        TwoTracks.setParameter("hist2DStyle", "colorMap");
        TwoTracks.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        TwoTracks1.dataStyle().fillStyle().setColor("green");
        TwoTracks1.dataStyle().errorBarStyle().setVisible(false);
        TwoTracks1.setParameter("hist2DStyle", "colorMap");
        TwoTracks1.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        TwoTracks2.dataStyle().fillStyle().setColor("green");
        TwoTracks2.dataStyle().errorBarStyle().setVisible(false);
        TwoTracks2.setParameter("hist2DStyle", "colorMap");
        TwoTracks2.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        TwoTracks3.dataStyle().fillStyle().setColor("green");
        TwoTracks3.dataStyle().errorBarStyle().setVisible(false);
        TwoTracks3.setParameter("hist2DStyle", "colorMap");
        TwoTracks3.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        TwoTracks4.dataStyle().fillStyle().setColor("green");
        TwoTracks4.dataStyle().errorBarStyle().setVisible(false);
        TwoTracks4.setParameter("hist2DStyle", "colorMap");
        TwoTracks4.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        TwoTracks5.dataStyle().fillStyle().setColor("green");
        TwoTracks5.dataStyle().errorBarStyle().setVisible(false);
        TwoTracks5.setParameter("hist2DStyle", "colorMap");
        TwoTracks5.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        TwoTracks6.dataStyle().fillStyle().setColor("green");
        TwoTracks6.dataStyle().errorBarStyle().setVisible(false);
        TwoTracks6.setParameter("hist2DStyle", "colorMap");
        TwoTracks6.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        TwoTracks7.dataStyle().fillStyle().setColor("green");
        TwoTracks7.dataStyle().errorBarStyle().setVisible(false);
        TwoTracks7.setParameter("hist2DStyle", "colorMap");
        TwoTracks7.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        TwoTracks8.dataStyle().fillStyle().setColor("green");
        TwoTracks8.dataStyle().errorBarStyle().setVisible(false);
        TwoTracks8.setParameter("hist2DStyle", "colorMap");
        TwoTracks8.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        TwoTracks9.dataStyle().fillStyle().setColor("green");
        TwoTracks9.dataStyle().errorBarStyle().setVisible(false);
        TwoTracks9.setParameter("hist2DStyle", "colorMap");
        TwoTracks9.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        TwoTracks10.dataStyle().fillStyle().setColor("green");
        TwoTracks10.dataStyle().errorBarStyle().setVisible(false);
        TwoTracks10.setParameter("hist2DStyle", "colorMap");
        TwoTracks10.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        TwoTracks11.dataStyle().fillStyle().setColor("green");
        TwoTracks11.dataStyle().errorBarStyle().setVisible(false);
        TwoTracks11.setParameter("hist2DStyle", "colorMap");
        TwoTracks11.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        TwoTracks12.dataStyle().fillStyle().setColor("green");
        TwoTracks12.dataStyle().errorBarStyle().setVisible(false);
        TwoTracks13.dataStyle().fillStyle().setColor("green");
        TwoTracks13.dataStyle().errorBarStyle().setVisible(false);
        TwoTracks14.dataStyle().fillStyle().setColor("green");
        TwoTracks14.dataStyle().errorBarStyle().setVisible(false);
        TwoTracks15.dataStyle().fillStyle().setColor("green");
        TwoTracks15.dataStyle().errorBarStyle().setVisible(false);
        TwoTracks15.setParameter("hist2DStyle", "colorMap");
        TwoTracks15.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        TwoTracks16.dataStyle().fillStyle().setColor("green");
        TwoTracks16.dataStyle().errorBarStyle().setVisible(false);
        TwoTracks16.setParameter("hist2DStyle", "colorMap");
        TwoTracks16.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        IPlotterStyle styletwotwo = twotrkextra2.style();
        styletwotwo.dataStyle().fillStyle().setColor("blue");
        styletwotwo.dataStyle().errorBarStyle().setVisible(false);

        IPlotterStyle threesty = threetrack.style();
        threesty.dataStyle().fillStyle().setColor("blue");
        threesty.dataStyle().errorBarStyle().setVisible(false);



        twotrkextra2.createRegions(3, 2);
        plotter9000.createRegion();
        plotter9001.createRegion();
        plotter9002.createRegions(2, 1);
        plotter9003.createRegion();
        plotter9004.createRegion();
        plotter9005.createRegion();
        plotter9006.createRegions(1, 3);
        plotter9007.createRegions(2, 3);
        plotter9008.createRegions(2, 2);
        plotter9009.createRegions(2, 2);
        plotter9010.createRegions(2, 2);
        plotter9011.createRegions(2, 2);
        plotter9012.createRegions(2, 2);
        //   plotter9013.createRegions(2, 2);
        //    plotter9014.createRegions(2, 2);
        plotter9015.createRegions(2, 2);
        plotter9016.createRegions(2, 2);
        plotter9017.createRegions(2, 2);
        threetrack.createRegion();

        twotrackFrame.addPlotter(plotter9000);
        twotrackFrame.addPlotter(plotter9001);
        twotrackFrame.addPlotter(plotter9002);
        twotrackFrame.addPlotter(plotter9003);
        twotrackFrame.addPlotter(plotter9004);
        twotrackFrame.addPlotter(plotter9005);
        twotrackFrame.addPlotter(plotter9006);
        twotrackFrame.addPlotter(plotter9007);
        twotrackFrame.addPlotter(plotter9008);
        twotrackFrame.addPlotter(plotter9009);
        twotrackFrame.addPlotter(plotter9010);
        twotrackFrame.addPlotter(plotter9011);
        twotrackFrame.addPlotter(plotter9012);
        //   twotrackFrame.addPlotter(plotter9013);
        //  twotrackFrame.addPlotter(plotter9014);
        twotrackFrame.addPlotter(plotter9015);
        twotrackFrame.addPlotter(plotter9016);
        twotrackFrame.addPlotter(plotter9017);
        twotrackFrame.addPlotter(twotrkextra2);
        twotrackFrame.addPlotter(threetrack);

        IHistogram1D trkbins = aida.histogram1D("Track Distributions", 5, -2, 3);
        IHistogram2D twtrkptot = aida.histogram2D("Total P+ vs. P-", 60, 0, 4, 60, 0, 4);
        IHistogram1D sumtrks = aida.histogram1D("Sum of Track's Momentums", 100, -1, 7);
        IHistogram1D invarmass = aida.histogram1D("Invariant Mass", 50, 0, .2);
        IHistogram1D perptrks = aida.histogram1D("Perpendicular Momentum", 100, 0, .1);
        IHistogram2D pyppm = aida.histogram2D("Py+ vs. Py-", 60, -.1, .1, 60, -.1, .1);
        IHistogram2D pzppm = aida.histogram2D("Pz+ vs. Pz-", 60, -.1, .1, 60, -.1, .1);
        IHistogram1D px = aida.histogram1D("Two Track X Momentum", 40, 0, 4);
        IHistogram1D py = aida.histogram1D("Two Track Y Momentum", 40, -.1, .1);
        IHistogram1D pz = aida.histogram1D("Two Track Z Momentum", 40, -.1, .1);
        IHistogram1D chi2 = aida.histogram1D("Tracks Chi2", 25, 0, 25.0);
        IHistogram1D bbpx = aida.histogram1D("Big Bump Track Momenta (Px)", 40, 0, 4);
        IHistogram1D bbpy = aida.histogram1D("Big Bump Track Momenta (Py)", 40, -.1, .1);
        IHistogram1D bbpz = aida.histogram1D("Big Bump Track Momenta (Pz)", 40, -.1, .1);
        IHistogram1D bbchi2 = aida.histogram1D("Big Bump Tracks Chi2", 25, 0, 25.0);
        IHistogram1D spx = aida.histogram1D("Split Track Momenta (Px)", 40, 0, 4);
        IHistogram1D spy = aida.histogram1D("Split Track Momenta (Py)", 40, -.1, .1);
        IHistogram1D spz = aida.histogram1D("Split Track Momenta (Pz)", 40, -.1, .1);
        IHistogram1D schi2 = aida.histogram1D("Split Tracks Chi2", 25, 0, 25.0);
        IHistogram1D bbsumtrks = aida.histogram1D("Big Bump Sum of Track's Momentums", 50, -1, 7);
        IHistogram2D bbpppm = aida.histogram2D("Big Bump P+ vs. P-", 50, 0, 4, 50, 0, 4);
        IHistogram2D lbpppm = aida.histogram2D("Little Bump P+ vs. P-", 50, 0, 4, 50, 0, 4);
        IHistogram1D lbsumtrks = aida.histogram1D("Little Bump Sum of Track's Momentums", 50, -1, 7);
        IHistogram1D lbpx = aida.histogram1D("Little Bump Track Momenta (Px)", 40, 0, 4);
        IHistogram1D lbpy = aida.histogram1D("Little Bump Track Momenta (Py)", 40, -.1, .1);
        IHistogram1D lbpz = aida.histogram1D("Little Bump Track Momenta (Pz)", 40, -.1, .1);
        IHistogram1D lbchi2 = aida.histogram1D("Little Bump Tracks Chi2", 25, 0, 25.0);
        //     IHistogram1D q0spx = aida.histogram1D("Net Charge 0 Split Track Momenta (Px)", 40, 0, 4);
        //     IHistogram1D q0spy = aida.histogram1D("Net Charge 0 Split Track Momenta (Py)", 40, -.1, .1);
        //     IHistogram1D q0spz = aida.histogram1D("Net Charge 0 Split Track Momenta (Pz)", 40, -.1, .1);
        //     IHistogram1D q0schi2 = aida.histogram1D("Net Charge 0 Split Tracks Chi2", 25, 0, 25.0);
        IHistogram2D xyemt = aida.histogram2D("X v Y - e- Top", 50, -30, 50, 50, -35, 30);
        IHistogram2D xzemt = aida.histogram2D("X v Z - e- Top", 50, -30, 50, 50, -800, -450);
        IHistogram2D yzemt = aida.histogram2D("Y v Z - e- Top", 50, -35, 30, 50, -800, -450);
        IHistogram1D qbins = aida.histogram1D("Charge Distributions", 5, -2, 3);
        IHistogram1D lbtp = aida.histogram1D("Little Bump Track Parity", 7, 0, 7);
        IHistogram1D bbtp = aida.histogram1D("Big Bump Track Parity", 7, 0, 7);
        IHistogram1D xvert = aida.histogram1D("XVertex", 40, -30, 50);
        IHistogram1D yvert = aida.histogram1D("YVertex", 40, -35, 30);
        IHistogram1D zvert = aida.histogram1D("ZVertex", 40, -800, -450);
        IHistogram1D dist = aida.histogram1D("Distance btwn Trks @ Solution", 40, 0, 20);
        IHistogram1D xres = aida.histogram1D("X Res Trk1", 40, -0.25, 0.25);
        IHistogram1D yres = aida.histogram1D("Y Res Trk1", 40, -0.25, 0.25);
        IHistogram1D xres2 = aida.histogram1D("X Res Trk2", 40, -0.25, 0.25);
        IHistogram1D yres2 = aida.histogram1D("Y Res Trk2", 40, -0.25, 0.25);
        IHistogram1D unx1 = aida.histogram1D("Uncert X Trk 1", 50, 0, 10);
        IHistogram1D uny1 = aida.histogram1D("Uncert Y Trk 1", 50, 0, 10);
        IHistogram1D unz1 = aida.histogram1D("Uncert Z Trk 1", 50, 0, 40);
        IHistogram1D unx2 = aida.histogram1D("Uncert X Trk 2", 50, 0, 10);
        IHistogram1D uny2 = aida.histogram1D("Uncert Y Trk 2", 50, 0, 10);
        IHistogram1D unz2 = aida.histogram1D("Uncert Z Trk 2", 50, 0, 40);
        IHistogram2D xy = aida.histogram2D("X v Y", 50, -30, 50, 50, -35, 30);
        IHistogram2D xz = aida.histogram2D("X v Z", 50, -30, 50, 50, -800, -450);
        IHistogram2D yz = aida.histogram2D("Y v Z", 50, -35, 30, 50, -800, -450);
        IHistogram2D xyept = aida.histogram2D("X v Y - e+ Top", 50, -30, 50, 50, -35, 30);
        IHistogram2D xzept = aida.histogram2D("X v Z - e+ Top", 50, -30, 50, 50, -800, -450);
        IHistogram2D yzept = aida.histogram2D("Y v Z - e+ Top", 50, -35, 30, 50, -800, -450);
        IHistogram1D three = aida.histogram1D("Three Track Invariant Mass", 50, 0, .4);

        twotrackFrame.pack();
        twotrackFrame.setVisible(true);

        plotter9000.region(0).plot(trkbins);
        plotter9001.region(0).plot(twtrkptot);
        plotter9002.region(0).plot(sumtrks);
        plotter9002.region(1).plot(invarmass);
        plotter9003.region(0).plot(perptrks);
        plotter9004.region(0).plot(pyppm);
        plotter9005.region(0).plot(pzppm);
        plotter9006.region(0).plot(xy);
        plotter9006.region(1).plot(xz);
        plotter9006.region(2).plot(yz);
        plotter9007.region(0).plot(xyemt);
        plotter9007.region(1).plot(xzemt);
        plotter9007.region(2).plot(yzemt);
        plotter9007.region(3).plot(xyept);
        plotter9007.region(4).plot(xzept);
        plotter9007.region(5).plot(yzept);
        plotter9008.region(0).plot(px);
        plotter9008.region(1).plot(py);
        plotter9008.region(2).plot(pz);
        plotter9008.region(3).plot(chi2);
        plotter9009.region(0).plot(bbpx);
        plotter9009.region(1).plot(bbpy);
        plotter9009.region(2).plot(bbpz);
        plotter9009.region(3).plot(bbchi2);
        plotter9010.region(0).plot(spx);
        plotter9010.region(1).plot(spy);
        plotter9010.region(2).plot(spz);
        plotter9010.region(3).plot(schi2);
        plotter9011.region(0).plot(bbsumtrks);
        plotter9011.region(1).plot(bbpppm);
        plotter9011.region(2).plot(lbpppm);
        plotter9011.region(3).plot(lbsumtrks);
        plotter9012.region(0).plot(lbpx);
        plotter9012.region(1).plot(lbpy);
        plotter9012.region(2).plot(lbpz);
        plotter9012.region(3).plot(lbchi2);
        //    plotter9013.region(0).plot(q0spx);
        //    plotter9013.region(1).plot(q0spy);
        //    plotter9013.region(2).plot(q0spz);
        //    plotter9013.region(3).plot(q0schi2);
        plotter9015.region(0).plot(qbins);
        plotter9015.region(1).plot(lbtp);
        plotter9015.region(2).plot(bbtp);
        plotter9016.region(0).plot(xvert);
        plotter9016.region(1).plot(yvert);
        plotter9016.region(2).plot(zvert);
        plotter9016.region(3).plot(dist);
        plotter9017.region(0).plot(xres);
        plotter9017.region(1).plot(yres);
        plotter9017.region(2).plot(xres2);
        plotter9017.region(3).plot(yres2);

        twotrkextra2.region(0).plot(unx1);
        twotrkextra2.region(1).plot(uny1);
        twotrkextra2.region(2).plot(unz1);
        twotrkextra2.region(3).plot(unx2);
        twotrkextra2.region(4).plot(uny2);
        twotrkextra2.region(5).plot(unz2);
        threetrack.region(0).plot(three);



    }

    public ElwinsTrackingRecon() {
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

    public void process(EventHeader event) {
        aida.tree().cd("/");
        if (!event.hasCollection(HelicalTrackHit.class, helicalTrackHitCollectionName)) {
            //       System.out.println(helicalTrackHitCollectionName + " does not exist; skipping event");
            return;
        }
        if (event.get(Track.class, trackCollectionName).size() < 2) {
            //    System.out.println(trackCollectionName + " has less than two tracks; skipping event");
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

        if (!event.hasCollection(Track.class, trackCollectionName)) {
//            System.out.println(trackCollectionName + " does not exist; skipping event");
            //      aida.histogram1D("Number Tracks/Event").fill(0);
            return;
        }


        List<Track> tracks = event.get(Track.class, trackCollectionName);
        nTracks.fill(tracks.size());


        if (tracks.size() == 2) { //uncert can be used here  && (Ytrue || Ytrue2) && (Xtrue || Xtrue2)

            Track trk1 = tracks.get(0);
            Track trk2 = tracks.get(1);
            int isTrk1Top = -1;
            if (trk1.getTrackerHits().get(0).getPosition()[2] > 0) {
                isTrk1Top = 1;
            }
            int isTrk2Top = -1;
            if (trk2.getTrackerHits().get(0).getPosition()[2] > 0) {
                isTrk2Top = 1;
            }
            boolean topbot = false;
            if ((isTrk1Top + isTrk2Top) == 0) {
                topbot = true;
            }

            SeedTrack stEle1 = (SeedTrack) trk1;
            SeedCandidate seedEle1 = stEle1.getSeedCandidate();
            HelicalTrackFit ht1 = seedEle1.getHelix();
            HelixConverter converter1 = new HelixConverter(0);
            StraightLineTrack slt1 = converter1.Convert(ht1);

            SeedTrack stEle2 = (SeedTrack) trk2;
            SeedCandidate seedEle2 = stEle2.getSeedCandidate();
            HelicalTrackFit ht2 = seedEle2.getHelix();
            HelixConverter converter2 = new HelixConverter(0);
            StraightLineTrack slt2 = converter2.Convert(ht2);

            HPSTrack hpstrack1 = new HPSTrack(ht1);
            Hep3Vector[] trkatconver1 = hpstrack1.getPositionAtZMap(100, BeamlineConstants.HARP_POSITION_TESTRUN, 1);
            HPSTrack hpstrack2 = new HPSTrack(ht2);
            Hep3Vector[] trkatconver2 = hpstrack2.getPositionAtZMap(100, BeamlineConstants.HARP_POSITION_TESTRUN, 1);



            List<TrackerHit> hitsOnTrack1 = trk1.getTrackerHits();
            int layer1;
            double y1 = 0;
            double y2 = 0;
            double z1 = 0;
            double z2 = 0;
            double dely1 = 0;
            double dely2 = 0;
            for (TrackerHit hit : hitsOnTrack1) {
                HelicalTrackHit htc1 = (HelicalTrackHit) hit;
                layer1 = htc1.Layer();
                int y1layer = 0;
                int y2layer = 0;
                if (y1 == 0) {
                    y1 = htc1.getPosition()[2]; //
                    z1 = htc1.getPosition()[0]; // z1 is jlab but the get position refers to hps-tracking
                    y1layer = layer1;
                    SymmetricMatrix ErrorHitOne = htc1.getCorrectedCovMatrix();
                    dely1 = Math.sqrt(ErrorHitOne.diagonal(2)); //y in jlab is z in hps
                } else {

                    if ((layer1 > y1layer) && (y2layer == 0)) {
                        y2 = htc1.getPosition()[2]; //
                        z2 = htc1.getPosition()[0]; // see above comments!
                        y2layer = layer1;
                        SymmetricMatrix ErrorHitTwo = htc1.getCorrectedCovMatrix();
                        dely2 = Math.sqrt(ErrorHitTwo.diagonal(2));
                    }

                }

            }
            List<TrackerHit> hitsOnTrack2 = trk2.getTrackerHits();

            double my1 = 0;
            double my2 = 0;
            double mz1 = 0;
            double mz2 = 0;
            double delymy1 = 0;
            double delymy2 = 0;
            int layer2;
            for (TrackerHit hit : hitsOnTrack2) {
                HelicalTrackHit htc2 = (HelicalTrackHit) hit;
//            if (htc.getPosition()[2] < 0) {

                layer2 = htc2.Layer();
                int my1layer = 0;
                int my2layer = 0;
                if (my1 == 0) {
                    my1 = htc2.getPosition()[2]; //see above comments
                    mz1 = htc2.getPosition()[0];
                    my1layer = layer2;
                    SymmetricMatrix ErrorHitOne = htc2.getCorrectedCovMatrix();
                    delymy1 = Math.sqrt(ErrorHitOne.diagonal(2));
                } else {
                    if ((layer2 > my1layer) && (my2layer == 0)) {
                        my2 = htc2.getPosition()[2];
                        mz2 = htc2.getPosition()[0];
                        my2layer = layer2;
                        SymmetricMatrix ErrorHitTwo = htc2.getCorrectedCovMatrix();
                        delymy2 = Math.sqrt(ErrorHitTwo.diagonal(2));
                    }
                }
            }
            //   double dely = .00001; //mm
            double b1;
            double m1;
            double b2;
            double m2;
            boolean check1 = true;
            if (y1 == 0) {
                check1 = false;
            }
            boolean check2 = true;
            if (my1 == 0) {
                check2 = false;
            }
            boolean check3 = true;
            if (my2 == 0) {
                check3 = false;
            }




            double X1 = slt1.getYZAtX(BeamlineConstants.HARP_POSITION_TESTRUN)[0];
            double Y1 = slt1.getYZAtX(BeamlineConstants.HARP_POSITION_TESTRUN)[1];

            //   boolean Y1top = false;
            //    boolean X1plus = false;
            //     boolean Y1bot = false;
            //      boolean X1minus = false;
            boolean X1cent = false;
            boolean Y1cent = false; //for simulation

            if (11 < X1 && X1 < 29) {
                X1cent = true;
            }
            if (-3.5 < Y1 && Y1 < 3.5) {
                Y1cent = true;
            }

            //    if (1 < Y1 && Y1 < 6) { //1 < Y1 && Y1 < 6 +-2.5
            //       Y1top = true;
            //  }
            //     if (11 < X1 && X1 < 29) { // 4 < X1 && X1 < 16 +-6
            //           X1minus = true;
            //      }
            //        if (-5 < Y1 && Y1 < 0) { // -5 < Y1 && Y1 < 0 +-2.5
            //           Y1bot = true;
            //      }
            //       if (11 < X1 && X1 < 29) { // 24 < X1 && X1 < 36 +-6
            //          X1plus = true;
            //     }
            double X2 = slt2.getYZAtX(BeamlineConstants.HARP_POSITION_TESTRUN)[0];
            double Y2 = slt2.getYZAtX(BeamlineConstants.HARP_POSITION_TESTRUN)[1];

            //      boolean Y2top = false; //for data
            //     boolean X2plus = false;
            //    boolean Y2bot = false; //in general
            //      boolean X2minus = false;
            boolean X2cent = false;
            boolean Y2cent = false; //for simulation
            if (11 < X2 && X2 < 29) {
                X2cent = true;
            }
            if (-3.5 < Y2 && Y2 < 3.5) {
                Y2cent = true;
            }
            //       if (1 < Y2 && Y2 < 6) {
            //          Y2top = true;
            //     }
            //      if (11 < X2 && X2 < 29) {
            //           X2minus = true;
            //      }
            //   if (-5 < Y2 && Y2 < 0) {
            //      Y2bot = true;
            // }
            //       if (11 < X2 && X2 < 29) {
            //           X2plus = true;
            //       }


            //      boolean Trk1Top = false;
            //     boolean Trk2Top = false;
            //    boolean Trk1Bot = false;
            //   boolean Trk2Bot = false;
            //  if (isTrk1Top == 1) {
            //             Trk1Top = true;
            //       }
            //     if (isTrk2Top == 1) {
            //       Trk2Top = true;
            //         }
            //       if (isTrk1Top == -1) {
            //         Trk1Bot = true;
            //   }
            //  if (isTrk2Top == -1) {
            //    Trk2Bot = true;
            //      }
            //  boolean Trk1goodTop = false;
            //   boolean Trk2goodTop = false;
            // boolean Trk1goodBot = false;
            //   boolean Trk2goodBot = false;
            //      if (Trk1Top && Y1top) {
            //         Trk1goodTop = true;
            //    }
            //   if (Trk2Top && Y2top) {
            //            Trk2goodTop = true;
            //   }
            //   if (Trk1Bot && Y1bot) {
            //      Trk1goodBot = true;
            //   }
            //   if (Trk2Bot && Y2bot) {
            //      Trk2goodBot = true;
            //  }

            int qtrk1 = trk1.getCharge();
            int qtrk2 = trk2.getCharge();
            boolean pm = false;
            if ((qtrk1 + qtrk2) == 0) {
                pm = true;
            }

            //   boolean Trk1Plus = false;
            //    boolean Trk2Plus = false;
            //      boolean Trk1Minus = false;
            //       boolean Trk2Minus = false;
            //       if (qtrk1 > 0) {
            //           Trk1Plus = true;
            //        } else {
            //             Trk1Minus = true;
            //        }
            //       if (qtrk2 > 0) {
            //           Trk2Plus = true;
            //      } else {
            //          Trk2Minus = true;
            //     }

            //    boolean Trk1goodPlus = false;
            //     boolean Trk2goodPlus = false;
            //     boolean Trk1goodMinus = false;
            //    boolean Trk2goodMinus = false;

            //       if (Trk1Plus && X1plus) {
            //          Trk1goodPlus = true;
            //       }
            //       if (Trk2Plus && X2plus) {
            //           Trk2goodPlus = true;
            //        }
            //       if (Trk1Minus && X1minus) {
            //        Trk1goodMinus = true;
            //       }
            //        if (Trk2Minus && X2minus) {
            //          Trk2goodMinus = true;
            //       }
            if (topbot && pm) {

                double b1p;
                double b2p;
                double m1p;
                double m2p;

                if (check1 && check2 && check3) {
                    if (isTrk1Top == 1) {
                        double zc = -1 * z1 / (z2 - z1);
                        b1 = (zc * (y2 - y1 + .5 * (dely2 + dely1))) + y1 - (.5 * dely1);
                        m1 = (y2 - y1 + .5 * (dely2 + dely1)) / (z2 - z1);
                        m1p = (y2 - y1 - .5 * (dely2 + dely1)) / (z2 - z1);
                        b1p = y1 - (m1p * z1) + (.5 * dely1);
                    } else {
                        double zc = -1 * z1 / (z2 - z1);
                        b1 = (zc * (y2 - y1 - .5 * (dely2 + dely1))) + y1 + (.5 * dely1);
                        m1 = (y2 - y1 - .5 * (dely2 + dely1)) / (z2 - z1);
                        m1p = (y2 - y1 + .5 * (dely2 + dely1)) / (z2 - z1);
                        b1p = y1 - (m1p * z1) - (.5 * dely1);
                    }

                    if (isTrk2Top == 1) {
                        double zc = -1 * mz1 / (mz2 - mz1);
                        b2 = (zc * (my2 - my1 + .5 * (delymy2 + delymy1))) + my1 - (.5 * delymy1);
                        m2 = (my2 - my1 + .5 * (delymy2 + delymy1)) / (mz2 - mz1);
                        m2p = (my2 - my1 - .5 * (delymy2 + delymy1)) / (mz2 - mz1);
                        b2p = my1 - (m2p * mz1) + (.5 * delymy1);
                    } else {
                        double zc = -1 * mz1 / (mz2 - mz1);
                        b2 = (zc * (my2 - my1 - .5 * (delymy2 + delymy1))) + my1 + (.5 * delymy1);
                        m2 = (my2 - my1 - .5 * (delymy2 + delymy1)) / (mz2 - mz1);
                        m2p = (my2 - my1 + .5 * (delymy2 + delymy1)) / (mz2 - mz1);
                        b2p = my1 - (m2p * mz1) - (.5 * delymy1);
                    }
                    //    System.out.println("y1 = " + y1);
                    //    System.out.println("y2 = " + y2);
                    //    System.out.println("y'1 = " + my1);
                    //    System.out.println("y'2 = " + my2);
                    double zi = (b2 - b1) / (m1 - m2);
                    double zr = Math.abs(zi - BeamlineConstants.HARP_POSITION_TESTRUN);
                    double zs = 2 * zr / 100;
                    //      System.out.println("Closest Possible Z to Tracker");
                    //      System.out.println(zi);


                    List<double[]> Trk1 = new ArrayList<double[]>();
                    for (int i = 0; i < 100; i++) {
                        double z = BeamlineConstants.HARP_POSITION_TESTRUN - zr + (zs * i);
                        double[] posvec = new double[3];
                        Hep3Vector[] trk1atz = hpstrack1.getPositionAtZMap(100, z, 1);
                        posvec[0] = trk1atz[0].x();
                        posvec[1] = trk1atz[0].y();
                        posvec[2] = z;

                        Trk1.add(posvec);
                    }
                    //  System.out.println("Vectors ");

                    //  System.out.println(Trk1);

                    double xbar = 0;
                    double ybar = 0;
                    double zbar = 0;
                    double xsqbar = 0;
                    double ysqbar = 0;
                    double zsqbar = 0;
                    int n = 0;
                    for (double[] inttrk : Trk1) {
                        //      System.out.println(inttrk[0]);
                        //    System.out.println(inttrk[1]);
                        //    System.out.println(inttrk[2]);
                        xbar = xbar + inttrk[0];
                        ybar = ybar + inttrk[1];
                        zbar = zbar + inttrk[2];
                        n = n + 1;
                    }
                    //   System.out.println("n " + n);
                    xbar = xbar / n;
                    ybar = ybar / n;
                    zbar = zbar / n;
                    //    System.out.println("Xbar is " + xbar);
                    //    System.out.println("Ybar is " + ybar);
                    //    System.out.println("Zbar is " + zbar);
                    Matrix d;
                    Matrix A = Matrix.random(n, 3);
                    int j1 = 0;
                    for (double[] inttrk : Trk1) {
                        A.set(j1, 0, inttrk[0] - xbar);
                        A.set(j1, 1, inttrk[1] - ybar);
                        A.set(j1, 2, inttrk[2] - zbar);
                        j1++;
                    }

                    //           System.out.println("Matrix A");
                    //           A.print(9, 6);
                    A.svd();
                    SingularValueDecomposition s = A.svd();
                    Matrix S = s.getS();
                    //         System.out.println("S Matrix");
                    //         S.print(9, 6);
                    Matrix V = s.getV();
                    //         System.out.println("V Matrix");
                    //         V.print(9, 6);
                    d = V.getMatrix(0, 2, 0, 0);
                    double[] dd;
                    dd = new double[3];

                    dd[0] = d.get(0, 0);
                    dd[1] = d.get(1, 0);
                    dd[2] = d.get(2, 0);
                    double nd = Math.sqrt((Math.pow(dd[0], 2)) + (Math.pow(dd[1], 2)) + (Math.pow(dd[2], 2)));

                    for (double[] inttrk : Trk1) {
                        double t1 = (inttrk[2] - zbar) / dd[2];
                        double restrk1[];
                        restrk1 = new double[3];
                        restrk1[0] = xbar + (t1) * dd[0] - inttrk[0];
                        restrk1[1] = ybar + (t1) * dd[1] - inttrk[1];
                        restrk1[2] = zbar + (t1) * dd[2] - inttrk[2];
                        aida.histogram1D("X Res Trk1").fill(restrk1[0]);
                        aida.histogram1D("Y Res Trk1").fill(restrk1[1]);
                    }

                    List<double[]> Trk2 = new ArrayList<double[]>();
                    for (int i = 0; i < 100; i++) {
                        double z = BeamlineConstants.HARP_POSITION_TESTRUN - zr + (zs * i);
                        double[] posvec2 = new double[3];
                        Hep3Vector[] trk2atz = hpstrack2.getPositionAtZMap(100, z, 1);
                        posvec2[0] = trk2atz[0].x();
                        posvec2[1] = trk2atz[0].y();
                        posvec2[2] = z;
                        Trk2.add(posvec2);
                        //      System.out.println("Components");
                        //      System.out.println(posvec2[0]);
                        //      System.out.println(posvec2[1]);
                        //      System.out.println(posvec2[2]);
                    }
                    double xbar2 = 0;
                    double ybar2 = 0;
                    double zbar2 = 0;
                    int n2 = 0;
                    for (double[] trk : Trk2) {
                        xbar2 = xbar2 + trk[0];
                        ybar2 = ybar2 + trk[1];
                        zbar2 = zbar2 + trk[2];
                        n2 = n2 + 1;
                    }
                    xbar2 = xbar2 / n2;
                    ybar2 = ybar2 / n2;
                    zbar2 = zbar2 / n2;
                    Matrix d2;
                    Matrix A2 = Matrix.random(n, 3);

                    int j2 = 0;
                    for (double[] inttrk : Trk2) {
                        A2.set(j2, 0, inttrk[0] - xbar2);
                        A2.set(j2, 1, inttrk[1] - ybar2);
                        A2.set(j2, 2, inttrk[2] - zbar2);
                        j2++;
                    }

                    A2.svd();
                    SingularValueDecomposition s2 = A2.svd();
                    Matrix V2 = s2.getV();
                    d2 = V2.getMatrix(0, 2, 0, 0);
                    double[] d22;
                    d22 = new double[3];
                    d22[0] = d2.get(0, 0);
                    d22[1] = d2.get(1, 0);
                    d22[2] = d2.get(2, 0);
                    double nd2 = Math.sqrt((Math.pow(d22[0], 2)) + (Math.pow(d22[1], 2)) + (Math.pow(d22[2], 2)));

                    for (double[] inttrk : Trk2) {
                        double t2 = (inttrk[2] - zbar2) / d22[2];
                        double restrk2[];
                        restrk2 = new double[3];
                        restrk2[0] = xbar2 + (t2) * d22[0] - inttrk[0];
                        restrk2[1] = ybar2 + (t2) * d22[1] - inttrk[1];
                        restrk2[2] = zbar2 + (t2) * d22[2] - inttrk[2];
                        aida.histogram1D("X Res Trk2").fill(restrk2[0]);
                        aida.histogram1D("Y Res Trk2").fill(restrk2[1]);

                    }

                    //solution for intersection below!! 

                    //starting with costant matrix b
                    double x11 = Math.pow(nd, 2) - Math.pow(dd[0], 2);
                    double y11 = Math.pow(nd, 2) - Math.pow(dd[1], 2);
                    double z11 = Math.pow(nd, 2) - Math.pow(dd[2], 2);
                    double x22 = Math.pow(nd2, 2) - Math.pow(d22[0], 2);
                    double y22 = Math.pow(nd2, 2) - Math.pow(d22[1], 2);
                    double z22 = Math.pow(nd2, 2) - Math.pow(d22[2], 2);
                    double xy1 = -1 * dd[0] * dd[1];
                    double xz1 = -1 * dd[0] * dd[2];
                    double yz1 = -1 * dd[1] * dd[2];
                    double xy2 = -1 * d22[0] * d22[1];
                    double xz2 = -1 * d22[0] * d22[2];
                    double yz2 = -1 * d22[1] * d22[2];
                    Matrix Intersect = Matrix.random(6, 3);
                    Intersect.set(0, 0, x11);
                    Intersect.set(1, 0, xy1);
                    Intersect.set(2, 0, xz1);
                    Intersect.set(0, 1, xy1);
                    Intersect.set(1, 1, y11);
                    Intersect.set(2, 1, yz1);
                    Intersect.set(0, 2, xz1);
                    Intersect.set(1, 2, yz1);
                    Intersect.set(2, 2, z11);

                    Intersect.set(3, 0, x22);
                    Intersect.set(4, 0, xy2);
                    Intersect.set(5, 0, xz2);
                    Intersect.set(3, 1, xy2);
                    Intersect.set(4, 1, y22);
                    Intersect.set(5, 1, yz2);
                    Intersect.set(3, 2, xz2);
                    Intersect.set(4, 2, yz2);
                    Intersect.set(5, 2, z22);
                    Matrix b = Matrix.random(6, 1);
                    b.set(0, 0, (x11 * xbar) + (xy1 * ybar) + (xz1 * zbar));
                    b.set(1, 0, (y11 * ybar) + (xy1 * xbar) + (yz1 * zbar));
                    b.set(2, 0, (z11 * zbar) + (xz1 * xbar) + (yz1 * ybar));
                    b.set(3, 0, (x22 * xbar2) + (xy2 * ybar2) + (xz2 * zbar2));
                    b.set(4, 0, (y22 * ybar2) + (xy2 * xbar2) + (yz2 * zbar2));
                    b.set(5, 0, (z22 * zbar2) + (xz2 * xbar2) + (yz2 * ybar2));

                    Intersect.svd();
                    SingularValueDecomposition s3 = Intersect.svd();
                    Matrix Vint = s3.getV();
                    Matrix Uint = s3.getU();
                    Matrix Sint = s3.getS();
                    //       System.out.println("S Matrix");
                    //       Sint.print(9, 6);
                    Matrix VT = Vint.transpose();
                    Matrix UT = Uint.transpose();
                    Matrix SI = Sint.inverse();
                    //      System.out.println("Inverted S Matrix");
                    //      SI.print(9, 6);
                    Matrix C1 = VT.times(SI);
                    Matrix C2 = C1.times(UT);
                    Matrix C = C2.times(b);
                    C.print(9, 6);
                    aida.histogram1D("XVertex").fill(C.get(0, 0));
                    aida.histogram1D("YVertex").fill(C.get(1, 0));
                    aida.histogram1D("ZVertex").fill(C.get(2, 0));

                    aida.histogram2D("X v Y").fill(C.get(0, 0), C.get(1, 0));
                    aida.histogram2D("X v Z").fill(C.get(0, 0), C.get(2, 0));
                    aida.histogram2D("Y v Z").fill(C.get(1, 0), C.get(2, 0));

                    double zint = C.get(2, 0);
                    double t1 = (zint - zbar) / dd[2];
                    double t2 = (zint - zbar2) / d22[2];
                    double postrk1[];
                    postrk1 = new double[3];
                    postrk1[0] = xbar + (t1) * dd[0];
                    postrk1[1] = ybar + (t1) * dd[1];
                    postrk1[2] = zbar + (t1) * dd[2];
                    double postrk2[];
                    postrk2 = new double[3];
                    postrk2[0] = xbar2 + (t2) * d22[0];
                    postrk2[1] = ybar2 + (t2) * d22[1];
                    postrk2[2] = zbar2 + (t2) * d22[2];
                    double distance = Math.sqrt(Math.pow(postrk2[0] - postrk1[0], 2) + Math.pow(postrk2[1] - postrk1[1], 2) + Math.pow(postrk2[2] - postrk1[2], 2));
                    //     double distancex = Math.sqrt(Math.pow(postrk2[0] - postrk1[0], 2));
                    //   double distancey = Math.sqrt(Math.pow(postrk2[1] - postrk1[1], 2));
                    aida.histogram1D("Distance btwn Trks @ Solution").fill(distance);
                    double tt1 = (BeamlineConstants.HARP_POSITION_TESTRUN - zbar) / dd[2]; //target
                    double tt2 = (BeamlineConstants.HARP_POSITION_TESTRUN - zbar2) / d22[2]; //target
                    double postrk1att[];
                    postrk1att = new double[3]; //target
                    postrk1att[0] = xbar + (tt1) * dd[0];
                    postrk1att[1] = ybar + (tt1) * dd[1];
                    postrk1att[2] = zbar + (tt1) * dd[2];
                    double postrk2att[]; //target
                    postrk2att = new double[3];
                    postrk2att[0] = xbar2 + (tt2) * d22[0];
                    postrk2att[1] = ybar2 + (tt2) * d22[1];
                    postrk2att[2] = zbar2 + (tt2) * d22[2];

                    aida.histogram1D("Trk1 X @ Target").fill(postrk1att[0]);
                    aida.histogram1D("Trk1 Y @ Target").fill(postrk1att[1]);
                    aida.histogram1D("Trk2 X @ Target").fill(postrk2att[0]);
                    aida.histogram1D("Trk2 Y @ Target").fill(postrk2att[1]);
                    double distanceatt = Math.sqrt(Math.pow(postrk2att[0] - postrk1att[0], 2) + Math.pow(postrk2att[1] - postrk1att[1], 2) + Math.pow(postrk2att[2] - postrk1att[2], 2));
                    // double zdiff = postrk2att[2] - postrk1att[2];
                    aida.histogram1D("Distance btwn Trks @ Target").fill(distanceatt);
                    // aida.histogram1D("Z Diff").fill(zdiff);

                    //  double y1atz = m1 * zint + b1;
                    //  double y2atz = m2 * zint + b2;
                    //  double y1patz = m2p * zint + b1p;
                    //  double y2patz = m2p * zint + b2p;
                    // double uncerty1 = y1atz - y1patz;
                    // double uncerty2 = y2atz - y2patz;
                    double uncerty = Math.abs((m1 - m2) * zint + (b1 - b2));
                    //   double uncertx1 = Math.sqrt(uncertx1sq);
                    double uncertz1 = Math.sqrt(zr);
                    //   double uncertx2 = Math.sqrt(uncertx2sq);
                    double uncertz2 = Math.sqrt(zr);
                    //   aida.histogram1D("Uncert X Trk 1").fill(uncertx1);
                    aida.histogram1D("Uncert Y Trk 1").fill(uncerty);
                    aida.histogram1D("Uncert Z Trk 1").fill(uncertz1);
                    // aida.histogram1D("Uncert X Trk 2").fill(uncerty2);
                    aida.histogram1D("Uncert Y Trk 2").fill(uncerty);
                    aida.histogram1D("Uncert Z Trk 2").fill(uncertz2);
                    boolean yzbump1 = false;
                    if (-18 < C.get(1, 0) && C.get(1, 0) < -12 && -645 < C.get(2, 0) && C.get(2, 0) < -600) {
                        yzbump1 = true;
                    }
                    boolean yzbump2 = false;
                    if (-4 < C.get(1, 0) && C.get(1, 0) < 4 && -720 < C.get(2, 0) && C.get(2, 0) < -605) {
                        yzbump2 = true;
                    }
                    boolean xybump1 = false;
                    if (-1 < C.get(0, 0) && C.get(0, 0) < 11 && -20 < C.get(1, 0) && C.get(1, 0) < -13) {
                        xybump1 = true;
                    }
                    boolean xybump2 = false;
                    if (11 < C.get(0, 0) && C.get(0, 0) < 25 && -4 < C.get(1, 0) && C.get(1, 0) < 3) {
                        xybump2 = true;
                    }
                    int trksparity = 0;
                    if (isTrk1Top == 1 && qtrk1 == 1) { //top e+ will be far right
                        trksparity = 6;
                    }
                    if (isTrk1Top == 1 && qtrk1 == -1) { //top e- will be right of middle two
                        trksparity = 4;
                    }
                    if (isTrk1Top == -1 && qtrk1 == 1) { // bot e+ will be left of middle two
                        trksparity = 2;
                    }
                    if (isTrk1Top == -1 && qtrk1 == -1) { //bot e- will be far left
                        trksparity = 0;
                    }
                    boolean eplustop = false;
                    if ((isTrk1Top == 1 && qtrk1 == 1) || (isTrk2Top == 1 && qtrk2 == 1)) {
                        eplustop = true;
                    }
                    if (yzbump1 && xybump1) {
                        aida.histogram1D("Little Bump Track Parity").fill(trksparity);
                    }
                    if (yzbump2 && xybump2) {
                        aida.histogram1D("Big Bump Track Parity").fill(trksparity);
                    }
                    if (eplustop) { //read Little bump as e+ top
                        aida.histogram1D("Little Bump Track Momenta (Px)").fill(trk1.getPX());
                        aida.histogram1D("Little Bump Track Momenta (Py)").fill(trk1.getPY());
                        aida.histogram1D("Little Bump Track Momenta (Pz)").fill(trk1.getPZ());
                        aida.histogram1D("Little Bump Tracks Chi2").fill(trk1.getChi2());
                        aida.histogram1D("Little Bump Track Momenta (Px)").fill(trk2.getPX());
                        aida.histogram1D("Little Bump Track Momenta (Py)").fill(trk2.getPY());
                        aida.histogram1D("Little Bump Track Momenta (Pz)").fill(trk2.getPZ());
                        aida.histogram1D("Little Bump Tracks Chi2").fill(trk2.getChi2());
                        aida.histogram1D("Little Bump Sum of Track's Momentums").fill(Math.sqrt(Math.pow((trk1.getPY() + trk2.getPY()), 2) + Math.pow((trk1.getPX() + trk2.getPX()), 2) + Math.pow((trk1.getPZ() + trk2.getPZ()), 2)));
                        double Etrk1sq = (Math.pow(trkatconver1[1].x(), 2) + Math.pow(trkatconver1[1].y(), 2) + Math.pow(trkatconver1[1].z(), 2));
                        double Etrk2sq = (Math.pow(trkatconver2[1].x(), 2) + Math.pow(trkatconver2[1].y(), 2) + Math.pow(trkatconver2[1].z(), 2));
                        double Etrk1 = Math.sqrt(Etrk1sq);
                        double Etrk2 = Math.sqrt(Etrk2sq);
                        double p1dotp2 = (trkatconver1[1].x() * trkatconver2[1].x() + trkatconver1[1].y() * trkatconver2[1].y() + trkatconver1[1].z() * trkatconver2[1].z());
                        aida.histogram1D("Invariant Mass").fill(Math.sqrt(2 * Etrk1 * Etrk2 - 2 * p1dotp2));
                        if (qtrk1 == 1) {
                            aida.histogram2D("Little Bump P+ vs. P-").fill(Math.sqrt((Math.pow((trk1.getPY()), 2) + Math.pow((trk1.getPX()), 2) + Math.pow((trk1.getPZ()), 2))), Math.sqrt((Math.pow((trk2.getPY()), 2) + Math.pow((trk2.getPX()), 2) + Math.pow((trk2.getPZ()), 2))));

                        } else {
                            aida.histogram2D("Little Bump P+ vs. P-").fill(Math.sqrt((Math.pow((trk2.getPY()), 2) + Math.pow((trk2.getPX()), 2) + Math.pow((trk2.getPZ()), 2))), Math.sqrt((Math.pow((trk1.getPY()), 2) + Math.pow((trk1.getPX()), 2) + Math.pow((trk1.getPZ()), 2))));
                        }
                        aida.histogram2D("X v Y - e+ Top").fill(C.get(0, 0), C.get(1, 0));
                        aida.histogram2D("X v Z - e+ Top").fill(C.get(0, 0), C.get(2, 0));
                        aida.histogram2D("Y v Z - e+ Top").fill(C.get(1, 0), C.get(2, 0));
                    } else { //read Big bump as e- top
                        aida.histogram1D("Big Bump Track Momenta (Px)").fill(trk1.getPX());
                        aida.histogram1D("Big Bump Track Momenta (Py)").fill(trk1.getPY());
                        aida.histogram1D("Big Bump Track Momenta (Pz)").fill(trk1.getPZ());
                        aida.histogram1D("Big Bump Tracks Chi2").fill(trk1.getChi2());
                        aida.histogram1D("Big Bump Track Momenta (Px)").fill(trk2.getPX());
                        aida.histogram1D("Big Bump Track Momenta (Py)").fill(trk2.getPY());
                        aida.histogram1D("Big Bump Track Momenta (Pz)").fill(trk2.getPZ());
                        aida.histogram1D("Big Bump Tracks Chi2").fill(trk2.getChi2());
                        aida.histogram1D("Big Bump Sum of Track's Momentums").fill(Math.sqrt(Math.pow((trk1.getPY() + trk2.getPY()), 2) + Math.pow((trk1.getPX() + trk2.getPX()), 2) + Math.pow((trk1.getPZ() + trk2.getPZ()), 2)));
                        double Etrk1sq = (Math.pow(trkatconver1[1].x(), 2) + Math.pow(trkatconver1[1].y(), 2) + Math.pow(trkatconver1[1].z(), 2));
                        double Etrk2sq = (Math.pow(trkatconver2[1].x(), 2) + Math.pow(trkatconver2[1].y(), 2) + Math.pow(trkatconver2[1].z(), 2));
                        double Etrk1 = Math.sqrt(Etrk1sq);
                        double Etrk2 = Math.sqrt(Etrk2sq);
                        double p1dotp2 = (trkatconver1[1].x() * trkatconver2[1].x() + trkatconver1[1].y() * trkatconver2[1].y() + trkatconver1[1].z() * trkatconver2[1].z());
                        aida.histogram1D("Invariant Mass").fill(Math.sqrt(2 * Etrk1 * Etrk2 - 2 * p1dotp2));
                        if (qtrk1 == 1) {
                            aida.histogram2D("Big Bump P+ vs. P-").fill(Math.sqrt((Math.pow((trk1.getPY()), 2) + Math.pow((trk1.getPX()), 2) + Math.pow((trk1.getPZ()), 2))), Math.sqrt((Math.pow((trk2.getPY()), 2) + Math.pow((trk2.getPX()), 2) + Math.pow((trk2.getPZ()), 2))));

                        } else {
                            aida.histogram2D("Big Bump P+ vs. P-").fill(Math.sqrt((Math.pow((trk2.getPY()), 2) + Math.pow((trk2.getPX()), 2) + Math.pow((trk2.getPZ()), 2))), Math.sqrt((Math.pow((trk1.getPY()), 2) + Math.pow((trk1.getPX()), 2) + Math.pow((trk1.getPZ()), 2))));
                        }
                        aida.histogram2D("X v Y - e- Top").fill(C.get(0, 0), C.get(1, 0));
                        aida.histogram2D("X v Z - e- Top").fill(C.get(0, 0), C.get(2, 0));
                        aida.histogram2D("Y v Z - e- Top").fill(C.get(1, 0), C.get(2, 0));
                    }
                }
                boolean check4 = true;
                if (my2 == 0) {
                    check4 = false;
                }

                aida.histogram1D("Track Distributions").fill(isTrk2Top + isTrk1Top);
                aida.histogram1D("Charge Distributions").fill(qtrk1 + qtrk2);
                //      aida.histogram1D("Two Track X Momentum").fill(trk1.getPX());
                //     aida.histogram1D("Two Track Y Momentum").fill(trk1.getPY());
                //      aida.histogram1D("Two Track Z Momentum").fill(trk1.getPZ());
                //      aida.histogram1D("Tracks Chi2").fill(trk1.getChi2());
                //      aida.histogram1D("Two Track X Momentum").fill(trk2.getPX());
                //      aida.histogram1D("Two Track Y Momentum").fill(trk2.getPY());
                //      aida.histogram1D("Two Track Z Momentum").fill(trk2.getPZ());
                //     aida.histogram1D("Tracks Chi2").fill(trk2.getChi2());

                //  if ((isTrk2Top + isTrk1Top) == -2) {
                ///      aida.histogram1D("Bottom-Bottom Track Momenta (Px)").fill(trk1.getPX());
                ///      aida.histogram1D("Bottom-Bottom Track Momenta (Py)").fill(trk1.getPY());
                //      aida.histogram1D("Bottom-Bottom Track Momenta (Pz)").fill(trk1.getPZ());
                ///    aida.histogram1D("Bottom-Bottom Tracks Chi2").fill(trk1.getChi2());
                //   aida.histogram1D("Bottom-Bottom Track Momenta (Px)").fill(trk2.getPX());
                ///            aida.histogram1D("Bottom-Bottom Track Momenta (Py)").fill(trk2.getPY());
                //         aida.histogram1D("Bottom-Bottom Track Momenta (Pz)").fill(trk2.getPZ());
                //       aida.histogram1D("Bottom-Bottom Tracks Chi2").fill(trk2.getChi2());
                //     aida.histogram1D("Charge Distributions Non-Split Tracks").fill(qtrk1 + qtrk2);
                //  }

                if ((isTrk2Top + isTrk1Top) == 0) {
                    aida.histogram1D("Split Track Momenta (Px)").fill(trk1.getPX());
                    aida.histogram1D("Split Track Momenta (Py)").fill(trk1.getPY());
                    aida.histogram1D("Split Track Momenta (Pz)").fill(trk1.getPZ());
                    aida.histogram1D("Split Tracks Chi2").fill(trk1.getChi2());
                    aida.histogram1D("Split Track Momenta (Px)").fill(trk2.getPX());
                    aida.histogram1D("Split Track Momenta (Py)").fill(trk2.getPY());
                    aida.histogram1D("Split Track Momenta (Pz)").fill(trk2.getPZ());
                    aida.histogram1D("Split Tracks Chi2").fill(trk2.getChi2());
                    //     aida.histogram1D("Charge Distributions Split Tracks").fill(qtrk1 + qtrk2);
                }

                if ((isTrk2Top + isTrk1Top) == 2) {
                    aida.histogram1D("Top-Top Track Momenta (Px)").fill(trk1.getPX());
                    aida.histogram1D("Top-Top Track Momenta (Py)").fill(trk1.getPY());
                    aida.histogram1D("Top-Top Track Momenta (Pz)").fill(trk1.getPZ());
                    aida.histogram1D("Top-Top Tracks Chi2").fill(trk1.getChi2());
                    aida.histogram1D("Top-Top Track Momenta (Px)").fill(trk2.getPX());
                    aida.histogram1D("Top-Top Track Momenta (Py)").fill(trk2.getPY());
                    aida.histogram1D("Top-Top Track Momenta (Pz)").fill(trk2.getPZ());
                    aida.histogram1D("Top-Top Tracks Chi2").fill(trk2.getChi2());
                    //     aida.histogram1D("Charge Distributions Non-Split Tracks").fill(qtrk1 + qtrk2);
                }


                if ((qtrk1 + qtrk2) == 0) {
                    aida.histogram1D("Perpendicular Momentum").fill(Math.sqrt(Math.pow((trk1.getPY() + trk2.getPY()), 2) + Math.pow((trk1.getPZ() + trk2.getPZ()), 2)));
                    //   if ((isTrk2Top + isTrk1Top) == -2) {
                    //       aida.histogram1D("Net Charge 0 Bottom-Bottom Track Momenta (Px)").fill(trk1.getPX());
                    //      aida.histogram1D("Net Charge 0 Bottom-Bottom Track Momenta (Py)").fill(trk1.getPY());
                    //      aida.histogram1D("Net Charge 0 Bottom-Bottom Track Momenta (Pz)").fill(trk1.getPZ());
                    //      aida.histogram1D("Net Charge 0 Bottom-Bottom Tracks Chi2").fill(trk1.getChi2());
                    //      aida.histogram1D("Net Charge 0 Bottom-Bottom Track Momenta (Px)").fill(trk2.getPX());
                    //      aida.histogram1D("Net Charge 0 Bottom-Bottom Track Momenta (Py)").fill(trk2.getPY());
                    //     aida.histogram1D("Net Charge 0 Bottom-Bottom Track Momenta (Pz)").fill(trk2.getPZ());
                    //     aida.histogram1D("Net Charge 0 Bottom-Bottom Tracks Chi2").fill(trk2.getChi2());
                    //  }

                    //      if ((isTrk2Top + isTrk1Top) == 0) {
                    ///          aida.histogram1D("Net Charge 0 Split Track Momenta (Px)").fill(trk1.getPX());
                    //         aida.histogram1D("Net Charge 0 Split Track Momenta (Py)").fill(trk1.getPY());
                    //        aida.histogram1D("Net Charge 0 Split Track Momenta (Pz)").fill(trk1.getPZ());
                    //       aida.histogram1D("Net Charge 0 Split Tracks Chi2").fill(trk1.getChi2());
                    //       aida.histogram1D("Net Charge 0 Split Track Momenta (Px)").fill(trk2.getPX());
                    //       aida.histogram1D("Net Charge 0 Split Track Momenta (Py)").fill(trk2.getPY());
                    //       aida.histogram1D("Net Charge 0 Split Track Momenta (Pz)").fill(trk2.getPZ());
                    //       aida.histogram1D("Net Charge 0 Split Tracks Chi2").fill(trk2.getChi2());
                    //   }

//                    if ((isTrk2Top + isTrk1Top) == 2) {
                    //                       aida.histogram1D("Net Charge 0 Top-Top Track Momenta (Px)").fill(trk1.getPX());
                    //                     aida.histogram1D("Net Charge 0 Top-Top Track Momenta (Py)").fill(trk1.getPY());
                    //                   aida.histogram1D("Net Charge 0 Top-Top Track Momenta (Pz)").fill(trk1.getPZ());
                    //                 aida.histogram1D("Net Charge 0 Top-Top Tracks Chi2").fill(trk1.getChi2());
                    //               aida.histogram1D("Net Charge 0 Top-Top Track Momenta (Px)").fill(trk2.getPX());
                    //             aida.histogram1D("Net Charge 0 Top-Top Track Momenta (Py)").fill(trk2.getPY());
                    ///           aida.histogram1D("Net Charge 0 Top-Top Track Momenta (Pz)").fill(trk2.getPZ());
                    //        aida.histogram1D("Net Charge 0 Top-Top Tracks Chi2").fill(trk2.getChi2());
                    //   }
                    if (qtrk1 == 1) {
                        aida.histogram2D("Py+ vs. Py-").fill(trk1.getPY(), trk2.getPY());
                        aida.histogram2D("Pz+ vs. Pz-").fill(trk1.getPZ(), trk2.getPZ());
                        aida.histogram2D("Total P+ vs. P-").fill(Math.sqrt((Math.pow((trk1.getPY()), 2) + Math.pow((trk1.getPX()), 2) + Math.pow((trk1.getPZ()), 2))), Math.sqrt((Math.pow((trk2.getPY()), 2) + Math.pow((trk2.getPX()), 2) + Math.pow((trk2.getPZ()), 2))));

                    } else {
                        aida.histogram2D("Py+ vs. Py-").fill(trk2.getPY(), trk1.getPY());
                        aida.histogram2D("Pz+ vs. Pz-").fill(trk2.getPZ(), trk1.getPZ());
                        aida.histogram2D("Total P+ vs. P-").fill(Math.sqrt((Math.pow((trk2.getPY()), 2) + Math.pow((trk2.getPX()), 2) + Math.pow((trk2.getPZ()), 2))), Math.sqrt((Math.pow((trk1.getPY()), 2) + Math.pow((trk1.getPX()), 2) + Math.pow((trk1.getPZ()), 2))));
                    }
                }
                //   aida.histogram1D("Sum of Track's Momentums").fill(Math.sqrt(Math.pow((trk1.getPY() + trk2.getPY()), 2) + Math.pow((trk1.getPX() + trk2.getPX()), 2) + Math.pow((trk1.getPZ() + trk2.getPZ()), 2)));

                //     double Etrk1sq = (Math.pow(trkatconver1[1].x(), 2) + Math.pow(trkatconver1[1].y(), 2) + Math.pow(trkatconver1[1].z(), 2));
                //     double Etrk2sq = (Math.pow(trkatconver2[1].x(), 2) + Math.pow(trkatconver2[1].y(), 2) + Math.pow(trkatconver2[1].z(), 2));
                //    double Etrk1 = Math.sqrt(Etrk1sq);
                //    double Etrk2 = Math.sqrt(Etrk2sq);
                //    double p1dotp2 = (trkatconver1[1].x() * trkatconver2[1].x() + trkatconver1[1].y() * trkatconver2[1].y() + trkatconver1[1].z() * trkatconver2[1].z());
                //    aida.histogram1D("Invariant Mass").fill(Math.sqrt(2 * Etrk1 * Etrk2 - 2 * p1dotp2));
                //     System.out.println("E1_" + Etrk1sq);
                //     System.out.println("E2_" + Etrk2sq);
                //     System.out.println("dot product_" + p1dotp2);
                //     System.out.println("Mass_" + Math.sqrt(2 * Etrk1 * Etrk2 - 2 * p1dotp2));

            }


        }
        if (tracks.size() == 3) {
            Track trk1 = tracks.get(0);
            Track trk2 = tracks.get(1);
            Track trk3 = tracks.get(2);

            SeedTrack stEle1 = (SeedTrack) trk1;
            SeedCandidate seedEle1 = stEle1.getSeedCandidate();
            HelicalTrackFit ht1 = seedEle1.getHelix();



            SeedTrack stEle2 = (SeedTrack) trk2;
            SeedCandidate seedEle2 = stEle2.getSeedCandidate();
            HelicalTrackFit ht2 = seedEle2.getHelix();

            SeedTrack stEle3 = (SeedTrack) trk3;
            SeedCandidate seedEle3 = stEle3.getSeedCandidate();
            HelicalTrackFit ht3 = seedEle3.getHelix();


            HPSTrack hpstrack1 = new HPSTrack(ht1);
            Hep3Vector[] trkatconver1 = hpstrack1.getPositionAtZMap(100, BeamlineConstants.HARP_POSITION_TESTRUN, 1);
            HPSTrack hpstrack2 = new HPSTrack(ht2);
            Hep3Vector[] trkatconver2 = hpstrack2.getPositionAtZMap(100, BeamlineConstants.HARP_POSITION_TESTRUN, 1);
            HPSTrack hpstrack3 = new HPSTrack(ht3);
            Hep3Vector[] trkatconver3 = hpstrack3.getPositionAtZMap(100, BeamlineConstants.HARP_POSITION_TESTRUN, 1);

            double Etrk1sq = (Math.pow(trkatconver1[1].x(), 2) + Math.pow(trkatconver1[1].y(), 2) + Math.pow(trkatconver1[1].z(), 2));
            double Etrk2sq = (Math.pow(trkatconver2[1].x(), 2) + Math.pow(trkatconver2[1].y(), 2) + Math.pow(trkatconver2[1].z(), 2));
            double Etrk3sq = (Math.pow(trkatconver3[1].x(), 2) + Math.pow(trkatconver3[1].y(), 2) + Math.pow(trkatconver3[1].z(), 2));
            double Etrk1 = Math.sqrt(Etrk1sq);
            double Etrk2 = Math.sqrt(Etrk2sq);
            double Etrk3 = Math.sqrt(Etrk3sq);
            double p1dotp2 = (trkatconver1[1].x() * trkatconver2[1].x() + trkatconver1[1].y() * trkatconver2[1].y() + trkatconver1[1].z() * trkatconver2[1].z());
            double p1dotp3 = (trkatconver1[1].x() * trkatconver3[1].x() + trkatconver1[1].y() * trkatconver3[1].y() + trkatconver1[1].z() * trkatconver3[1].z());
            double p2dotp3 = (trkatconver3[1].x() * trkatconver2[1].x() + trkatconver3[1].y() * trkatconver2[1].y() + trkatconver3[1].z() * trkatconver2[1].z());
            aida.histogram1D("Three Track Invariant Mass").fill(Math.sqrt(2 * (Etrk1 * Etrk2 + Etrk1 * Etrk3 + Etrk3 * Etrk2) - 2 * (p1dotp2 + p1dotp3 + p2dotp3)));
            System.out.println("Mass " + Math.sqrt(2 * Etrk1 * Etrk2 - 2 * p1dotp2));

        }




        for (Track trk : tracks) {

            aida.histogram1D("Track X Momentum").fill(trk.getPY());
            aida.histogram1D("Track Y Momentum").fill(trk.getPZ());
            aida.histogram1D("Track Z Momentum").fill(trk.getPX());
            aida.histogram1D("Track Chi2").fill(trk.getChi2());


            aida.histogram1D("Hits per Track").fill(trk.getTrackerHits().size());
            SeedTrack stEle = (SeedTrack) trk;
            SeedCandidate seedEle = stEle.getSeedCandidate();
            HelicalTrackFit ht = seedEle.getHelix();
            HelixConverter converter = new HelixConverter(0);
            StraightLineTrack slt = converter.Convert(ht);

            HPSTrack hpstrack = new HPSTrack(ht);
            Hep3Vector[] trkatconver = hpstrack.getPositionAtZMap(100, BeamlineConstants.HARP_POSITION_TESTRUN, 1);
            aida.histogram1D("X (mm) @ Converter").fill(trkatconver[0].x()); // y tracker frame?
            aida.histogram1D("Y (mm) @ Converter").fill(trkatconver[0].y()); // z tracker frame?

// See
//            ExtendTrack extend = new ExtendTrack();
//            extend.setTrack(stEle);

            aida.histogram1D("X (mm) @ Z=-60cm").fill(slt.getYZAtX(BeamlineConstants.HARP_POSITION_TESTRUN)[0]);  //this is y in the tracker frame
            aida.histogram1D("Y (mm) @ Z=-60cm").fill(slt.getYZAtX(BeamlineConstants.HARP_POSITION_TESTRUN)[1]);  //this is z in the tracker frame
            aida.histogram1D("X (mm) @ Z=-150cm").fill(slt.getYZAtX(zAtColl)[0]);
            aida.histogram1D("Y (mm) @ Z=-150cm").fill(slt.getYZAtX(zAtColl)[1]);



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

    public void endOfData() {
        System.out.println("Output");
        if (outputPlots != null) {
            try {
                aida.saveAs(outputPlots);
                System.out.println("0 Tracks");
                System.out.println(nTracks.binEntries(0));
                System.out.println("1 Track");
                System.out.println(nTracks.binEntries(1));
                System.out.println("2 Tracks");
                System.out.println(nTracks.binEntries(2));
            } catch (IOException ex) {
                Logger.getLogger(ElwinsTrackingRecon.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        plotterFrame.dispose();
        topFrame.dispose();
        bottomFrame.dispose();
    }

    private HPSEcalCluster findClosestCluster(Hep3Vector posonhelix, List<HPSEcalCluster> clusters) {
        HPSEcalCluster closest = null;
        double minDist = 9999;
        for (HPSEcalCluster cluster : clusters) {
            double[] clPos = cluster.getPosition();
            double clEne = cluster.getEnergy();
//            double dist = Math.sqrt(Math.pow(clPos[0] - posonhelix.y(), 2) + Math.pow(clPos[1] - posonhelix.z(), 2)); //coordinates!!!
            double dist = Math.sqrt(Math.pow(clPos[1] - posonhelix.z(), 2)); //coordinates!!!
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
