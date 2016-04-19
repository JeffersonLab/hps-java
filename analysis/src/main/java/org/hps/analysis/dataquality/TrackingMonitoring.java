package org.hps.analysis.dataquality;

import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.gbl.GBLKinkData;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.geometry.Detector;
import org.lcsim.util.aida.AIDA;

/**
 * DQM driver for reconstructed track quantities plots things like number of
 * tracks/event, chi^2, track parameters (d0/z0/theta/phi/curvature)
 *
 * @author mgraham on Mar 28, 2014
 */
// TODO:  Add some quantities for DQM monitoring:  e.g. <tracks>, <hits/track>, etc
public class TrackingMonitoring extends DataQualityMonitor {

    private static Logger LOGGER = Logger.getLogger(SvtMonitoring.class.getPackage().getName());

    private String helicalTrackHitCollectionName = "HelicalTrackHits";
    private final String rotatedTrackHitCollectionName = "RotatedHelicalTrackHits";
    private final String helicalTrackHitRelationsCollectionName = "HelicalTrackHitRelations";
    private final String rotatedHelicalTrackHitRelationsCollectionName = "RotatedHelicalTrackHitRelations";
    private String trackCollectionName = "MatchedTracks";
    private final String trackerName = "Tracker";
    private static final String nameStrip = "Tracker_TestRunModule_";
    String ecalSubdetectorName = "Ecal";
    String ecalCollectionName = "EcalClusters";
    private Detector detector = null;
    private List<HpsSiSensor> sensors;

    int nEvents = 0;
    int nTotTracks = 0;
    int nTotHits = 0;
    double sumd0 = 0;
    double sumz0 = 0;
    double sumslope = 0;
    double sumchisq = 0;
    private final String plotDir = "Tracks/";
    private final String positronDir = "Positrons/";
    private final String electronDir = "Electrons/";
    private final String topDir = "Top/";
    private final String botDir = "Bottom/";
    private final String hthplotDir = "HelicalTrackHits/";
    private final String timeresidDir = "HitTimeResiduals/";
    private final String kinkDir = "Kinks/";
    String[] trackingQuantNames = {"avg_N_tracks", "avg_N_hitsPerTrack", "avg_d0", "avg_z0", "avg_absslope", "avg_chi2"};
    int nmodules = 6;
    IHistogram1D[] hthTop = new IHistogram1D[nmodules];
    IHistogram1D[] hthBot = new IHistogram1D[nmodules];
    IHistogram2D[] xvsyTop = new IHistogram2D[nmodules];
    IHistogram2D[] xvsyBot = new IHistogram2D[nmodules];
    IHistogram2D[] xvsyOnTrackTop = new IHistogram2D[nmodules];
    IHistogram2D[] xvsyOnTrackBot = new IHistogram2D[nmodules];
    IHistogram2D[] timevstimeTop = new IHistogram2D[nmodules];
    IHistogram2D[] timevstimeBot = new IHistogram2D[nmodules];
    IHistogram2D[] timevstimeOnTrackTop = new IHistogram2D[nmodules];
    IHistogram2D[] timevstimeOnTrackBot = new IHistogram2D[nmodules];
    IHistogram1D[] deltaTOnTrackTop = new IHistogram1D[nmodules];
    IHistogram1D[] deltaTOnTrackBot = new IHistogram1D[nmodules];

    IHistogram1D trkChi2;
    IHistogram1D nTracks;
    IHistogram1D trkd0;
    IHistogram1D trkphi;
    IHistogram1D trkomega;
    IHistogram1D trklam;
    IHistogram1D trkz0;
    IHistogram1D nHits;
    IHistogram1D trackMeanTime;
    IHistogram1D trackRMSTime;
    IHistogram2D trackNhitsVsChi2;
    IHistogram2D trackChi2RMSTime;
    IHistogram1D seedRMSTime;
    IHistogram2D trigTimeTrackTime;
    IHistogram1D trigTime;

    IHistogram1D trkXAtECALTop;
    IHistogram1D trkXAtECALBot;
    IHistogram1D trkYAtECALTop;
    IHistogram1D trkYAtECALBot;
    IHistogram2D trkAtECAL;

    IHistogram1D trkChi2Pos;
    IHistogram1D trkChi2Ele;
    IHistogram1D trkChi2Top;
    IHistogram1D trkChi2Bot;

    IHistogram1D nTracksPos;
    IHistogram1D nTracksEle;
    IHistogram1D nTracksTop;
    IHistogram1D nTracksBot;

    IHistogram1D trkd0Pos;
    IHistogram1D trkd0Ele;
    IHistogram1D trkd0Top;
    IHistogram1D trkd0Bot;

    IHistogram1D trkphiPos;
    IHistogram1D trkphiEle;
    IHistogram1D trkphiTop;
    IHistogram1D trkphiBot;

    IHistogram1D trkomegaPos;
    IHistogram1D trkomegaEle;
    IHistogram1D trkomegaTop;
    IHistogram1D trkomegaBot;

    IHistogram1D trklamPos;
    IHistogram1D trklamEle;
    IHistogram1D trklamTop;
    IHistogram1D trklamBot;

    IHistogram1D trkz0Pos;
    IHistogram1D trkz0Ele;
    IHistogram1D trkz0Top;
    IHistogram1D trkz0Bot;

    IHistogram1D nHitsPos;
    IHistogram1D nHitsEle;
    IHistogram1D nHitsTop;
    IHistogram1D nHitsBot;

    IHistogram1D trkTimePos;
    IHistogram1D trkTimeEle;
    IHistogram1D trkTimeTop;
    IHistogram1D trkTimeBot;

    IHistogram2D d0VsPhi0;
    IHistogram2D d0Vsomega;
    IHistogram2D d0Vslambda;
    IHistogram2D d0Vsz0;
    IHistogram2D phi0Vsomega;
    IHistogram2D phi0Vslambda;
    IHistogram2D phi0Vsz0;
    IHistogram2D omegaVslambda;
    IHistogram2D omegaVsz0;
    IHistogram2D lamdbaVsz0;

    IHistogram2D chi2VsD0;
    IHistogram2D chi2VsPhi0;
    IHistogram2D chi2VsOmega;
    IHistogram2D chi2VsLambda;
    IHistogram2D chi2VsZ0;

    IHistogram2D beamAngleXY;
    IHistogram2D beamAngleThetaPhi;

    IHistogram1D L1Iso;
    IHistogram1D L12Iso;
    IHistogram2D d0VsL1Iso;
    IHistogram2D d0VsL12Iso;

    double d0Cut = 5.0;
    double phiCut = 0.2;
    double omegaCut = 0.0005;
    double lambdaCut = 0.1;
    double z0Cut = 1.0;
    double timeCut = 24.0;

    public void setHelicalTrackHitCollectionName(String helicalTrackHitCollectionName) {
        this.helicalTrackHitCollectionName = helicalTrackHitCollectionName;
    }

    public void setTrackCollectionName(String trackCollectionName) {
        this.trackCollectionName = trackCollectionName;
    }

    @Override
    protected void detectorChanged(Detector detector) {
        this.detector = detector;
        aida.tree().cd("/");

        trkChi2 = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "Track Chi2", 200, 0, 100.0);
        nTracks = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "Tracks per Event", 6, 0, 6);
        trkd0 = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "d0", 100, -d0Cut, d0Cut);
        trkphi = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "sinphi", 100, -phiCut, phiCut);
        trkomega = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "omega", 100, -omegaCut, omegaCut);
        trklam = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "tan(lambda)", 100, -lambdaCut, lambdaCut);
        trkz0 = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "z0", 100, -z0Cut, z0Cut);
        nHits = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "Hits per Track", 4, 3, 7);
        trackMeanTime = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "Mean time of hits on track", 100, -timeCut, timeCut);
        trackRMSTime = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "RMS time of hits on track", 100, 0., 15.);
        trackChi2RMSTime = aida.histogram2D(plotDir + trackCollectionName + "/" + triggerType + "/" + "Track chi2 vs. RMS time of hits", 100, 0., 15., 25, 0, 25.0);
        trackNhitsVsChi2 = aida.histogram2D(plotDir + trackCollectionName + "/" + triggerType + "/" + "Track nhits vs. chi2", 100, 0, 100.0, 4, 3, 7);
        trigTimeTrackTime = aida.histogram2D(plotDir + trackCollectionName + "/" + triggerType + "/" + "Trigger phase vs. mean time of hits", 100, -timeCut, timeCut, 6, 0, 24.0);
        trigTime = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "Trigger phase", 6, 0., 24.);
        seedRMSTime = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "RMS time of hits on seed layers", 100, 0., 15.);
        trkXAtECALTop = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "Track X at ECAL: Top", 100, -250, 250);
        trkXAtECALBot = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "Track X at ECAL: Bot", 100, -250, 250);
        trkYAtECALTop = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "Track Y at ECAL: Top", 100, 0, 100);
        trkYAtECALBot = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "Track Y at ECAL: Bot", 100, 0, 100);
        trkAtECAL = aida.histogram2D(plotDir + trackCollectionName + "/" + triggerType + "/" + "Track XY at ECAL", 100, -250, 250, 100, -100, 100);
        for (int i = 1; i <= nmodules; i++) {
            xvsyTop[i - 1] = aida.histogram2D(hthplotDir + triggerType + "/" + "Module " + i + " Top", 250, -100, 150, 30, 0, 60);
            xvsyBot[i - 1] = aida.histogram2D(hthplotDir + triggerType + "/" + "Module " + i + " Bottom", 250, -100, 150, 30, 0, 60);
            xvsyOnTrackTop[i - 1] = aida.histogram2D(hthplotDir + triggerType + "/" + "Module " + i + " Top, Hits On Tracks", 250, -100, 150, 30, 0, 60);
            xvsyOnTrackBot[i - 1] = aida.histogram2D(hthplotDir + triggerType + "/" + "Module " + i + " Bottom, Hits On Tracks", 250, -100, 150, 30, 0, 60);
            timevstimeTop[i - 1] = aida.histogram2D(hthplotDir + triggerType + "/" + "Module " + i + " Top: Hit Times", 30, -15, 15, 30, -15, 15);
            timevstimeBot[i - 1] = aida.histogram2D(hthplotDir + triggerType + "/" + "Module " + i + " Bottom: Hit Times", 30, -15, 15, 30, -15, 15);
            hthTop[i - 1] = aida.histogram1D(hthplotDir + triggerType + "/" + "Module " + i + " Top: Track Hits", 25, 0, 25);
            hthBot[i - 1] = aida.histogram1D(hthplotDir + triggerType + "/" + "Module " + i + " Bottom: Track Hits", 25, 0, 25);
            timevstimeOnTrackTop[i - 1] = aida.histogram2D(hthplotDir + triggerType + "/" + "Module " + i + " Top: Hit Times, Hits On Tracks", 30, -15, 15, 30, -15, 15);
            timevstimeOnTrackBot[i - 1] = aida.histogram2D(hthplotDir + triggerType + "/" + "Module " + i + " Bottom: Hit Times, Hits On Tracks", 30, -15, 15, 30, -15, 15);
            deltaTOnTrackTop[i - 1] = aida.histogram1D(hthplotDir + triggerType + "/" + "Module " + i + " Top: Hit Time Differences, Hits On Tracks", 50, -25, 25);
            deltaTOnTrackBot[i - 1] = aida.histogram1D(hthplotDir + triggerType + "/" + "Module " + i + " Bottom: Hit Time Differences, Hits On Tracks", 50, -25, 25);
        }

        trkChi2Pos = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + positronDir + "Track Chi2", 25, 0, 25.0);
        nTracksPos = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + positronDir + "Tracks per Event", 6, 0, 6);
        trkd0Pos = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + positronDir + "d0", 100, -d0Cut, d0Cut);
        trkphiPos = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + positronDir + "sinphi", 100, -phiCut, phiCut);
        trkomegaPos = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + positronDir + "omega", 100, -omegaCut, omegaCut);
        trklamPos = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + positronDir + "tan(lambda)", 100, -lambdaCut, lambdaCut);
        trkz0Pos = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + positronDir + "z0", 100, -z0Cut, z0Cut);
        nHitsPos = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + positronDir + "Hits per Track", 4, 3, 7);
        trkTimePos = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + positronDir + "Mean time of hits on track", 100, -timeCut, timeCut);

        trkChi2Ele = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + electronDir + "Track Chi2", 25, 0, 25.0);
        nTracksEle = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + electronDir + "Tracks per Event", 6, 0, 6);
        trkd0Ele = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + electronDir + "d0", 100, -d0Cut, d0Cut);
        trkphiEle = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + electronDir + "sinphi", 100, -phiCut, phiCut);
        trkomegaEle = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + electronDir + "omega", 100, -omegaCut, omegaCut);
        trklamEle = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + electronDir + "tan(lambda)", 100, -lambdaCut, lambdaCut);
        trkz0Ele = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + electronDir + "z0", 100, -z0Cut, z0Cut);
        nHitsEle = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + electronDir + "Hits per Track", 4, 3, 7);
        trkTimeEle = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + electronDir + "Mean time of hits on track", 100, -timeCut, timeCut);

        trkChi2Top = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + topDir + "Track Chi2", 25, 0, 25.0);
        nTracksTop = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + topDir + "Tracks per Event", 6, 0, 6);
        trkd0Top = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + topDir + "d0", 100, -d0Cut, d0Cut);
        trkphiTop = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + topDir + "sinphi", 100, -phiCut, phiCut);
        trkomegaTop = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + topDir + "omega", 100, -omegaCut, omegaCut);
        trklamTop = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + topDir + "tan(lambda)", 100, 0.0, lambdaCut);
        trkz0Top = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + topDir + "z0", 100, -z0Cut, z0Cut);
        nHitsTop = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + topDir + "Hits per Track", 4, 3, 7);
        trkTimeTop = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + topDir + "Mean time of hits on track", 100, -timeCut, timeCut);

        trkChi2Bot = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + botDir + "Track Chi2", 25, 0, 25.0);
        nTracksBot = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + botDir + "Tracks per Event", 6, 0, 6);
        trkd0Bot = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + botDir + "d0", 100, -d0Cut, d0Cut);
        trkphiBot = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + botDir + "sinphi", 100, -phiCut, phiCut);
        trkomegaBot = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + botDir + "omega", 100, -omegaCut, omegaCut);
        trklamBot = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + botDir + "tan(lambda)", 100, 0, lambdaCut);
        trkz0Bot = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + botDir + "z0", 100, -z0Cut, z0Cut);
        nHitsBot = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + botDir + "Hits per Track", 4, 3, 7);
        trkTimeBot = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + botDir + "Mean time of hits on track", 100, -timeCut, timeCut);

        //correlation plots
        d0VsPhi0 = aida.histogram2D(plotDir + trackCollectionName + "/" + triggerType + "/" + "d0 vs sinphi", 50, -d0Cut, d0Cut, 50, -phiCut, phiCut);
        d0Vsomega = aida.histogram2D(plotDir + trackCollectionName + "/" + triggerType + "/" + "d0 vs omega", 50, -d0Cut, d0Cut, 50, -omegaCut, omegaCut);
        d0Vslambda = aida.histogram2D(plotDir + trackCollectionName + "/" + triggerType + "/" + "d0 vs tan(lambda)", 50, -d0Cut, d0Cut, 50, -lambdaCut, lambdaCut);
        d0Vsz0 = aida.histogram2D(plotDir + trackCollectionName + "/" + triggerType + "/" + "d0 vs z0", 50, -d0Cut, d0Cut, 50, -z0Cut, z0Cut);

        phi0Vsomega = aida.histogram2D(plotDir + trackCollectionName + "/" + triggerType + "/" + "phi0 vs omega", 50, -phiCut, phiCut, 50, -omegaCut, omegaCut);
        phi0Vslambda = aida.histogram2D(plotDir + trackCollectionName + "/" + triggerType + "/" + "phi0 vs tan(lambda)", 50, -phiCut, phiCut, 50, -lambdaCut, lambdaCut);
        phi0Vsz0 = aida.histogram2D(plotDir + trackCollectionName + "/" + triggerType + "/" + "phi0 vs z0", 50, -phiCut, phiCut, 50, -z0Cut, z0Cut);

        omegaVslambda = aida.histogram2D(plotDir + trackCollectionName + "/" + triggerType + "/" + "omega vs tan(lambda)", 50, -omegaCut, omegaCut, 50, -lambdaCut, lambdaCut);
        omegaVsz0 = aida.histogram2D(plotDir + trackCollectionName + "/" + triggerType + "/" + "omega vs z0", 50, -omegaCut, omegaCut, 50, -z0Cut, z0Cut);

        lamdbaVsz0 = aida.histogram2D(plotDir + trackCollectionName + "/" + triggerType + "/" + "lambda vs z0", 50, -lambdaCut, lambdaCut, 50, -z0Cut, z0Cut);

        chi2VsD0 = aida.histogram2D(plotDir + trackCollectionName + "/" + triggerType + "/" + "chi2 vs d0", 50, -d0Cut, d0Cut, 50, 0.0, 50.0);
        chi2VsPhi0 = aida.histogram2D(plotDir + trackCollectionName + "/" + triggerType + "/" + "chi2 vs sinphi", 50, -phiCut, phiCut, 50, 0.0, 50.0);
        chi2VsOmega = aida.histogram2D(plotDir + trackCollectionName + "/" + triggerType + "/" + "chi2 vs omega", 50, -omegaCut, omegaCut, 50, 0.0, 50.0);
        chi2VsLambda = aida.histogram2D(plotDir + trackCollectionName + "/" + triggerType + "/" + "chi2 vs lambda", 50, -lambdaCut, lambdaCut, 50, 0.0, 50.0);
        chi2VsZ0 = aida.histogram2D(plotDir + trackCollectionName + "/" + triggerType + "/" + "chi2 vs z0", 50, -z0Cut, z0Cut, 50, 0.0, 50.0);

        beamAngleXY = aida.histogram2D(plotDir + trackCollectionName + "/" + triggerType + "/" + "angles around beam axis: theta_y vs theta_x", 100, -0.1, 0.1, 100, -0.1, 0.1);
        beamAngleThetaPhi = aida.histogram2D(plotDir + trackCollectionName + "/" + triggerType + "/" + "angles around beam axis: theta vs phi", 100, -Math.PI, Math.PI, 100, 0, 0.25);

        L1Iso = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "L1 isolation", 100, -5.0, 5.0);
        L12Iso = aida.histogram1D(plotDir + trackCollectionName + "/" + triggerType + "/" + "L1-2 isolation", 100, -5.0, 5.0);
        d0VsL1Iso = aida.histogram2D(plotDir + trackCollectionName + "/" + triggerType + "/" + "d0 vs L1 isolation", 100, -5.0, 5.0, 50, -d0Cut, d0Cut);
        d0VsL12Iso = aida.histogram2D(plotDir + trackCollectionName + "/" + triggerType + "/" + "d0 vs L1-2 isolation", 100, -5.0, 5.0, 50, -d0Cut, d0Cut);

        // Make a list of SiSensors in the SVT.
        sensors = this.detector.getSubdetector(trackerName).getDetectorElement().findDescendants(HpsSiSensor.class);

        // Setup the occupancy plots.
        aida.tree().cd("/");
        for (HpsSiSensor sensor : sensors) {
            //IHistogram1D occupancyPlot = aida.histogram1D(sensor.getName().replaceAll("Tracker_TestRunModule_", ""), 640, 0, 639);
            IHistogram1D hitTimeResidual = PlotAndFitUtilities.createSensorPlot(plotDir + trackCollectionName + "/" + triggerType + "/" + timeresidDir + "hitTimeResidual_", sensor, 100, -20, 20);
            IHistogram1D lambdaKink = PlotAndFitUtilities.createSensorPlot(plotDir + trackCollectionName + "/" + triggerType + "/" + kinkDir + "lambdaKink_", sensor, 100, -5e-3, 5e-3);
            IHistogram1D phiKink = PlotAndFitUtilities.createSensorPlot(plotDir + trackCollectionName + "/" + triggerType + "/" + kinkDir + "phiKink_", sensor, 100, -5e-3, 5e-3);
            IHistogram2D lambdaKink2D = PlotAndFitUtilities.createSensorPlot2D(plotDir + trackCollectionName + "/" + triggerType + "/" + kinkDir + "lambdaKinkVsOmega_", sensor, 100, -omegaCut, omegaCut, 100, -5e-3, 5e-3);
            IHistogram2D phiKink2D = PlotAndFitUtilities.createSensorPlot2D(plotDir + trackCollectionName + "/" + triggerType + "/" + kinkDir + "phiKinkVsOmega_", sensor, 100, -omegaCut, omegaCut, 100, -5e-3, 5e-3);
        }
    }

    @Override
    public void process(EventHeader event) {

        aida.tree().cd("/");

        if (!event.hasCollection(LCRelation.class, helicalTrackHitRelationsCollectionName) || !event.hasCollection(LCRelation.class, rotatedHelicalTrackHitRelationsCollectionName)) {
            return;
        }
        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);

        if (!event.hasCollection(TrackerHit.class, helicalTrackHitCollectionName)) {
            return;
        }

        //check to see if this event is from the correct trigger (or "all");
        if (!matchTrigger(event)) {
            return;
        }
        int[] topHits = {0, 0, 0, 0, 0, 0};
        int[] botHits = {0, 0, 0, 0, 0, 0};
        List<TrackerHit> hth = event.get(TrackerHit.class, helicalTrackHitCollectionName);
        for (TrackerHit hit : hth) {
            int layer = ((RawTrackerHit) hit.getRawHits().get(0)).getLayerNumber();
            int module = layer / 2 + 1;

            Collection<TrackerHit> htsList = hitToStrips.allFrom(hit);
            double hitTimes[] = new double[2];
            for (TrackerHit hts : htsList) {
                int stripLayer = ((HpsSiSensor) ((RawTrackerHit) hts.getRawHits().get(0)).getDetectorElement()).getLayerNumber();
                hitTimes[stripLayer % 2] = hts.getTime();
            }

            if (hit.getPosition()[1] > 0) {
                topHits[module - 1]++;
                xvsyTop[module - 1].fill(hit.getPosition()[0], hit.getPosition()[1]);
                timevstimeTop[module - 1].fill(hitTimes[0], hitTimes[1]);
            } else {
                botHits[module - 1]++;
                xvsyBot[module - 1].fill(hit.getPosition()[0], Math.abs(hit.getPosition()[1]));
                timevstimeBot[module - 1].fill(hitTimes[0], hitTimes[1]);
            }
        }

        for (int i = 0; i < nmodules; i++) {
            hthTop[i].fill(topHits[i]);
            hthBot[i].fill(botHits[i]);
        }

        if (!event.hasCollection(Track.class, trackCollectionName)) {
            nTracks.fill(0);
            return;
        }
        nEvents++;
        List<Track> tracks = event.get(Track.class, trackCollectionName);
        nTotTracks += tracks.size();
        nTracks.fill(tracks.size());
        int cntEle = 0;
        int cntPos = 0;
        int cntTop = 0;
        int cntBot = 0;
        for (Track trk : tracks) {
            Hep3Vector trackPosAtEcalFace = TrackUtils.getTrackPositionAtEcal(trk);
            double xAtECal = trackPosAtEcalFace.x();
            double yAtECal = trackPosAtEcalFace.y();
            if (yAtECal > 0) {
                trkXAtECALTop.fill(xAtECal);
                trkYAtECALTop.fill(yAtECal);
            } else {
                trkXAtECALBot.fill(xAtECal);
                trkYAtECALBot.fill(Math.abs(yAtECal));
            }
            trkAtECAL.fill(xAtECal, yAtECal);
            nTotHits += trk.getTrackerHits().size();

            double d0 = trk.getTrackStates().get(0).getD0();
            double sinphi0 = Math.sin(trk.getTrackStates().get(0).getPhi());
            double omega = trk.getTrackStates().get(0).getOmega();
            double lambda = trk.getTrackStates().get(0).getTanLambda();
            double z0 = trk.getTrackStates().get(0).getZ0();
            trkChi2.fill(trk.getChi2());
            nHits.fill(trk.getTrackerHits().size());
            trackNhitsVsChi2.fill(trk.getChi2(), trk.getTrackerHits().size());
            trkd0.fill(trk.getTrackStates().get(0).getD0());
            trkphi.fill(Math.sin(trk.getTrackStates().get(0).getPhi()));
            trkomega.fill(trk.getTrackStates().get(0).getOmega());
            trklam.fill(trk.getTrackStates().get(0).getTanLambda());
            trkz0.fill(trk.getTrackStates().get(0).getZ0());
            d0VsPhi0.fill(d0, sinphi0);
            d0Vsomega.fill(d0, omega);
            d0Vslambda.fill(d0, lambda);
            d0Vsz0.fill(d0, z0);
            phi0Vsomega.fill(sinphi0, omega);
            phi0Vslambda.fill(sinphi0, lambda);
            phi0Vsz0.fill(sinphi0, z0);
            omegaVslambda.fill(omega, lambda);
            omegaVsz0.fill(omega, z0);
            lamdbaVsz0.fill(lambda, z0);
            chi2VsD0.fill(d0, trk.getChi2());
            chi2VsPhi0.fill(sinphi0, trk.getChi2());
            chi2VsOmega.fill(omega, trk.getChi2());
            chi2VsLambda.fill(lambda, trk.getChi2());
            chi2VsZ0.fill(z0, trk.getChi2());

            BasicHep3Matrix rot = new BasicHep3Matrix();
            rot.setActiveEuler(Math.PI / 2, -0.0305, -Math.PI / 2);
            Hep3Vector dir = CoordinateTransformations.transformVectorToDetector(HelixUtils.Direction(TrackUtils.getHTF(trk), 0));
            Hep3Vector dirRotated = VecOp.mult(rot, dir);

            double beamPhi = Math.atan2(dirRotated.y(), dirRotated.x());
            double beamTheta = Math.acos(dirRotated.z());

            beamAngleXY.fill(dirRotated.x(), dirRotated.y());
            beamAngleThetaPhi.fill(beamPhi, beamTheta);

            Double[] isolations = TrackUtils.getIsolations(trk, hitToStrips, hitToRotated);
            double l1Iso = Double.MAX_VALUE;
            double l12Iso = Double.MAX_VALUE;

            for (int i = 0; i < isolations.length; i++) {
                if (isolations[i] != null) {
                    if (i == 0 || i == 1) {
                        if (Math.abs(isolations[i]) < Math.abs(l1Iso)) {
                            l1Iso = isolations[i];
                        }
                    }
                    if (i < 4) {
                        if (Math.abs(isolations[i]) < Math.abs(l12Iso)) {
                            l12Iso = isolations[i];
                        }
                    }
                }
            }
            L1Iso.fill(l1Iso);
            L12Iso.fill(l12Iso);
            d0VsL1Iso.fill(l1Iso, d0);
            d0VsL12Iso.fill(l12Iso, d0);

            int nStrips = 0;
            int nSeedStrips = 0;
            double meanTime = 0;
            double meanSeedTime = 0;

            List<TrackerHit> stripHits = new ArrayList<TrackerHit>();

            for (TrackerHit hit : trk.getTrackerHits()) {
                Collection<TrackerHit> htsList = hitToStrips.allFrom(hitToRotated.from(hit));
                stripHits.addAll(htsList);
                double hitTimes[] = new double[2];
                for (TrackerHit hts : htsList) {
                    int stripLayer = ((HpsSiSensor) ((RawTrackerHit) hts.getRawHits().get(0)).getDetectorElement()).getLayerNumber();
                    hitTimes[stripLayer % 2] = hts.getTime();

                    nStrips++;
                    meanTime += hts.getTime();
                    int layer = ((HpsSiSensor) ((RawTrackerHit) hts.getRawHits().get(0)).getDetectorElement()).getLayerNumber();
                    if (layer <= 6) {
                        nSeedStrips++;
                        meanSeedTime += hts.getTime();
                    }
                }
                int module = ((RawTrackerHit) hit.getRawHits().get(0)).getLayerNumber() / 2 + 1;

                if (hit.getPosition()[2] > 0) {
                    xvsyOnTrackTop[module - 1].fill(hit.getPosition()[1], hit.getPosition()[2]);
                    timevstimeOnTrackTop[module - 1].fill(hitTimes[0], hitTimes[1]);
                    deltaTOnTrackTop[module - 1].fill(hitTimes[0] - hitTimes[1]);
                } else {
                    xvsyOnTrackBot[module - 1].fill(hit.getPosition()[1], Math.abs(hit.getPosition()[2]));
                    timevstimeOnTrackBot[module - 1].fill(hitTimes[0], hitTimes[1]);
                    deltaTOnTrackBot[module - 1].fill(hitTimes[0] - hitTimes[1]);
                }
            }
            meanTime /= nStrips;
            meanSeedTime /= nSeedStrips;

            double rmsTime = 0;
            double rmsSeedTime = 0;

            stripHits = TrackUtils.sortHits(stripHits);
            for (TrackerHit hts : stripHits) {
                rmsTime += Math.pow(hts.getTime() - meanTime, 2);
                HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) hts.getRawHits().get(0)).getDetectorElement();
                int layer = sensor.getLayerNumber();
                if (layer <= 6) {
                    rmsSeedTime += Math.pow(hts.getTime() - meanSeedTime, 2);
                }
                PlotAndFitUtilities.getSensorPlot(plotDir + trackCollectionName + "/" + triggerType + "/" + timeresidDir + "hitTimeResidual_", sensor).fill((hts.getTime() - meanTime) * nStrips / (nStrips - 1)); //correct residual for bias
            }
            rmsTime = Math.sqrt(rmsTime / nStrips);
            trackMeanTime.fill(meanTime);
            trackRMSTime.fill(rmsTime);
            trackChi2RMSTime.fill(rmsTime, trk.getChi2());
            trigTimeTrackTime.fill(meanTime, event.getTimeStamp() % 24);
            trigTime.fill(event.getTimeStamp() % 24);

            rmsSeedTime = Math.sqrt(rmsSeedTime / nSeedStrips);
            seedRMSTime.fill(rmsSeedTime);

            GenericObject kinkData = GBLKinkData.getKinkData(event, trk);
            if (kinkData != null) {
                for (int i = 0; i < stripHits.size(); i++) {
                    TrackerHit hts = stripHits.get(i);
                    HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) hts.getRawHits().get(0)).getDetectorElement();
//                    int layer = sensor.getLayerNumber();
                    double lambdaKink = GBLKinkData.getLambdaKink(kinkData, i);
                    double phiKink = GBLKinkData.getPhiKink(kinkData, i);
//                    System.out.format("%d %d %f %f\n", i, layer, lambdaKink, phiKink);

                    PlotAndFitUtilities.getSensorPlot(plotDir + trackCollectionName + "/" + triggerType + "/" + kinkDir + "lambdaKink_", sensor).fill(lambdaKink);
                    PlotAndFitUtilities.getSensorPlot(plotDir + trackCollectionName + "/" + triggerType + "/" + kinkDir + "phiKink_", sensor).fill(phiKink);
                    PlotAndFitUtilities.getSensorPlot2D(plotDir + trackCollectionName + "/" + triggerType + "/" + kinkDir + "lambdaKinkVsOmega_", sensor).fill(trk.getTrackStates().get(0).getOmega(), lambdaKink);
                    PlotAndFitUtilities.getSensorPlot2D(plotDir + trackCollectionName + "/" + triggerType + "/" + kinkDir + "phiKinkVsOmega_", sensor).fill(trk.getTrackStates().get(0).getOmega(), phiKink);
                }
            }

            if (trk.getTrackStates().get(0).getOmega() < 0) {//positrons
                trkChi2Pos.fill(trk.getChi2());
                nHitsPos.fill(trk.getTrackerHits().size());
                trkd0Pos.fill(trk.getTrackStates().get(0).getD0());
                trkphiPos.fill(Math.sin(trk.getTrackStates().get(0).getPhi()));
                trkomegaPos.fill(trk.getTrackStates().get(0).getOmega());
                trklamPos.fill(trk.getTrackStates().get(0).getTanLambda());
                trkz0Pos.fill(trk.getTrackStates().get(0).getZ0());
                trkTimePos.fill(meanTime);
                cntPos++;
            } else {
                trkChi2Ele.fill(trk.getChi2());
                nHitsEle.fill(trk.getTrackerHits().size());
                trkd0Ele.fill(trk.getTrackStates().get(0).getD0());
                trkphiEle.fill(Math.sin(trk.getTrackStates().get(0).getPhi()));
                trkomegaEle.fill(trk.getTrackStates().get(0).getOmega());
                trklamEle.fill(trk.getTrackStates().get(0).getTanLambda());
                trkz0Ele.fill(trk.getTrackStates().get(0).getZ0());
                trkTimeEle.fill(meanTime);
                cntEle++;
            }

            if (trk.getTrackStates().get(0).getTanLambda() < 0) {
                trkChi2Bot.fill(trk.getChi2());
                nHitsBot.fill(trk.getTrackerHits().size());
                trkd0Bot.fill(trk.getTrackStates().get(0).getD0());
                trkphiBot.fill(Math.sin(trk.getTrackStates().get(0).getPhi()));
                trkomegaBot.fill(trk.getTrackStates().get(0).getOmega());
                trklamBot.fill(Math.abs(trk.getTrackStates().get(0).getTanLambda()));
                trkz0Bot.fill(trk.getTrackStates().get(0).getZ0());
                trkTimeBot.fill(meanTime);

                cntBot++;
            } else {
                trkChi2Top.fill(trk.getChi2());
                nHitsTop.fill(trk.getTrackerHits().size());
                trkd0Top.fill(trk.getTrackStates().get(0).getD0());
                trkphiTop.fill(Math.sin(trk.getTrackStates().get(0).getPhi()));
                trkomegaTop.fill(trk.getTrackStates().get(0).getOmega());
                trklamTop.fill(Math.abs(trk.getTrackStates().get(0).getTanLambda()));
                trkz0Top.fill(trk.getTrackStates().get(0).getZ0());
                trkTimeTop.fill(meanTime);
                cntTop++;
            }

            sumd0 += trk.getTrackStates().get(0).getD0();
            sumz0 += trk.getTrackStates().get(0).getZ0();
            sumslope += Math.abs(trk.getTrackStates().get(0).getTanLambda());
            sumchisq += trk.getChi2();

//            System.out.format("%d seed strips, RMS time %f\n", nSeedStrips, rmsSeedTime);
//            System.out.format("%d strips, mean time %f, RMS time %f\n", nStrips, meanTime, rmsTime);
        }
        nTracksTop.fill(cntTop);
        nTracksBot.fill(cntBot);
        nTracksPos.fill(cntPos);
        nTracksEle.fill(cntEle);
    }

    @Override
    public void calculateEndOfRunQuantities() {
        IFitFactory fitFactory = AIDA.defaultInstance().analysisFactory().createFitFactory();
        IFitter fitter = fitFactory.createFitter("chi2");

        for (HpsSiSensor sensor : sensors) {
            //IHistogram1D occupancyPlot = aida.histogram1D(sensor.getName().replaceAll("Tracker_TestRunModule_", ""), 640, 0, 639);
            IHistogram1D hitTimeResidual = PlotAndFitUtilities.getSensorPlot(plotDir + trackCollectionName + "/" + triggerType + "/" + timeresidDir + "hitTimeResidual_", sensor);
            IFitResult result = fitGaussian(hitTimeResidual, fitter, "range=\"(-20.0,20.0)\"");
            if (result != null) {
                System.out.format("%s\t%f\t%f\t%d\t%d\t%f\n", getNiceSensorName(sensor), result.fittedParameters()[1], result.fittedParameters()[2], sensor.getFebID(), sensor.getFebHybridID(), sensor.getT0Shift());
            }
        }

        monitoredQuantityMap.put(trackCollectionName + " " + triggerType + " " + trackingQuantNames[0], (double) nTotTracks / nEvents);
        monitoredQuantityMap.put(trackCollectionName + " " + triggerType + " " + trackingQuantNames[1], (double) nTotHits / nTotTracks);
        monitoredQuantityMap.put(trackCollectionName + " " + triggerType + " " + trackingQuantNames[2], sumd0 / nTotTracks);
        monitoredQuantityMap.put(trackCollectionName + " " + triggerType + " " + trackingQuantNames[3], sumz0 / nTotTracks);
        monitoredQuantityMap.put(trackCollectionName + " " + triggerType + " " + trackingQuantNames[4], sumslope / nTotTracks);
        monitoredQuantityMap.put(trackCollectionName + " " + triggerType + " " + trackingQuantNames[5], sumchisq / nTotTracks);
    }

    IFitResult fitGaussian(IHistogram1D h1d, IFitter fitter, String range) {
        double[] init = {h1d.maxBinHeight(), h1d.mean(), h1d.rms()};
        IFitResult ifr = null;
        try {
            ifr = fitter.fit(h1d, "g", init, range);
        } catch (RuntimeException ex) {
            LOGGER.info(this.getClass().getSimpleName() + ":  caught exception in fitGaussian");
        }
        return ifr;
//        double[] init = {20.0, 0.0, 1.0, 20, -1};
//        return fitter.fit(h1d, "g+p1", init, range);
    }

    @Override
    public void printDQMData() {
        LOGGER.info("ReconMonitoring::printDQMData");
        for (Map.Entry<String, Double> entry : monitoredQuantityMap.entrySet()) {
            LOGGER.info(entry.getKey() + " = " + entry.getValue());
        }
    }

    @Override
    public void printDQMStrings() {
        for (Map.Entry<String, Double> entry : monitoredQuantityMap.entrySet()) {
            LOGGER.info("ALTER TABLE dqm ADD " + entry.getKey() + " double;");
        }
    }

    private String getNiceSensorName(HpsSiSensor sensor) {
        return sensor.getName().replaceAll(nameStrip, "")
                .replace("module", "mod")
                .replace("layer", "lyr")
                .replace("sensor", "sens");
    }

}
