package org.hps.monitoring.drivers.trackrecon;

import hep.aida.IAnalysisFactory;
import hep.aida.IFitFactory;
import hep.aida.IFitter;
import hep.aida.IFunction;
import hep.aida.IFunctionFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;
import hep.aida.ITree;
import hep.physics.matrix.BasicMatrix;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;

import static java.lang.Math.abs;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.util.Pair;
import org.hps.recon.tracking.HpsHelicalTrackFit;
import org.hps.recon.tracking.TrackStateUtils;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.gbl.GblUtils;
import org.hps.recon.tracking.gbl.matrix.Matrix;
import org.lcsim.constants.Constants;
import org.lcsim.detector.solids.Box;
import org.lcsim.detector.solids.LineSegment3D;
import org.lcsim.detector.solids.Polygon3D;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;

import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author mgraham
 */
public class L01UnbiasedResidualsFromL26 extends Driver {

    protected AIDA aida = AIDA.defaultInstance();
    ITree tree;
    IHistogramFactory histogramFactory;
    private String helicalTrackHitCollectionName = "HelicalTrackHits";
    private String rotatedTrackHitCollectionName = "RotatedHelicalTrackHits";
    private String l2to6CollectionName = "L2to6TracksGBL";
//    private String l2to6CollectionName = "GBLTracks";
    private String stripHitOutputCollectionName = "StripClusterer_SiTrackerHitStrip1D";

    //List of Sensors
    private List<HpsSiSensor> sensors = null;
    private static final String SUBDETECTOR_NAME = "Tracker";
    //Bfield
    protected static double bfield;
    FieldMap bFieldMap = null;
    private int nTotTracks = 0;
    private String outputPlots = null;
//    IPlotter plotterTop;
    IPlotter plotterParsTop;
    IPlotter plotterBot;
    IPlotter plotterParsBot;
    IPlotter plotterResid;
    IPlotter plotterHTHResid;

    IHistogram1D nTracks26Top;
    IHistogram1D nTracks26Bot;
    IHistogram1D nHits26Top;
    IHistogram1D nHits26Bot;

    IHistogram1D lambdaTopL26;
    IHistogram1D z0TopL26;
    IHistogram1D phi0TopL26;
    IHistogram1D d0TopL26;
    IHistogram1D omegaTopL26;
    IHistogram1D chisqTopL26;

    IHistogram1D lambdaBotL26;
    IHistogram1D z0BotL26;
    IHistogram1D phi0BotL26;
    IHistogram1D d0BotL26;
    IHistogram1D omegaBotL26;
    IHistogram1D chisqBotL26;

    IHistogram1D residualXHTH0Top;
    IHistogram1D residualXHTH1Top;
    IHistogram1D residualYHTH0Top;
    IHistogram1D residualYHTH1Top;
    IHistogram1D residualXHTH0Bot;
    IHistogram1D residualXHTH1Bot;
    IHistogram1D residualYHTH0Bot;
    IHistogram1D residualYHTH1Bot;

    Map<String, IHistogram1D> residualY = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> errorY = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> pullY = new HashMap<String, IHistogram1D>();

    IPlotterFactory plotterFactory;
    IFunctionFactory functionFactory;
    IFitFactory fitFactory;
    IFunction fd0Top;
    IFunction fphi0Top;
    IFunction fz0Top;
    IFunction flambdaTop;
    IFunction fwTop;

    IFitter jminChisq;

    boolean matchFullTracks = false;
    String fullTrackCollectionName = "s234_c5_e167";
    double targetPosition = -5.0; //mm

    int chanExtd = 0;

    //Histogram Settings
    int nBins = 50;
    double maxPull = 7;
    double minPull = -maxPull;
    double maxRes = 0.5;
    double minRes = -maxRes;
    double maxYerror = 0.1;
    double minResX = -10;
    double maxResX = 10;
    double minResY = -1;
    double maxResY = 1;
    
    boolean onlyElectrons=false;

    public L01UnbiasedResidualsFromL26() {
    }

    public void setOnlyElectrons(boolean onlyElectrons){
        this.onlyElectrons=onlyElectrons;
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

    public void setL2to6CollectionName(String trackCollectionName) {
        this.l2to6CollectionName = trackCollectionName;
    }

    public void setChanExtd(int chanExtd) {
        this.chanExtd = chanExtd;
    }

    @Override
    protected void detectorChanged(Detector detector) {

        aida.tree().cd("/");
        tree = aida.tree();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);

        // Get the HpsSiSensor objects from the tracker detector element
        sensors = detector.getSubdetector(SUBDETECTOR_NAME)
                .getDetectorElement().findDescendants(HpsSiSensor.class);
        bfield = TrackUtils.getBField(detector).magnitude();
        bFieldMap = detector.getFieldMap();

        IAnalysisFactory fac = aida.analysisFactory();
        IPlotterFactory pfac = fac.createPlotterFactory("SVT Alignment");
        functionFactory = aida.analysisFactory().createFunctionFactory(null);
        fitFactory = aida.analysisFactory().createFitFactory();
        jminChisq = fitFactory.createFitter("chi2", "jminuit");

//        plotterTop = pfac.create("Top Layers");
//        IPlotterStyle style = plotterTop.style();
//        style.dataStyle().fillStyle().setColor("yellow");
//        style.dataStyle().errorBarStyle().setVisible(false);
//        style.legendBoxStyle().setVisible(false);
//        style.dataStyle().outlineStyle().setVisible(false);
//        plotterTop.createRegions(3, 3);
        // plotterFrame.addPlotter(plotter);
        IPlotterStyle functionStyle = pfac.createPlotterStyle();
        functionStyle.dataStyle().lineStyle().setColor("red");
        functionStyle.dataStyle().markerStyle().setVisible(true);
        functionStyle.dataStyle().markerStyle().setColor("black");
        functionStyle.dataStyle().markerStyle().setShape("dot");
        functionStyle.dataStyle().markerStyle().setSize(2);

        nTracks26Top = aida.histogram1D("Number of L2-6 Tracks: Top ", 7, 0, 7);
        nTracks26Bot = aida.histogram1D("Number of L2-6 Tracks: Bot ", 7, 0, 7);
        nHits26Top = aida.histogram1D("Number of L2-6 Hits: Top ", 6, 3, 9);
        nHits26Bot = aida.histogram1D("Number of L2-6 Hits: Bot ", 6, 3, 9);

//        plotterTop.region(0).plot(fd0Top, functionStyle);
//        plotterTop.region(3).plot(fphi0Top, functionStyle);
//        plotterTop.region(6).plot(fwTop, functionStyle);
//        plotterTop.region(1).plot(flambdaTop, functionStyle);
//        plotterTop.region(4).plot(fz0Top, functionStyle);
        plotterParsTop = pfac.create("Top Track Pars");
        plotterParsTop.createRegions(2, 4);
        plotterParsBot = pfac.create("Bottom Track Pars");
        plotterParsBot.createRegions(2, 4);
        lambdaTopL26 = aida.histogram1D("slope: Top L2-6", 50, 0, 0.06);
        z0TopL26 = aida.histogram1D("y0: Top L0-3", 50, -2.5, 2.5);
        phi0TopL26 = aida.histogram1D("phi0: Top L2-6", 50, -0.1, 0.15);
        omegaTopL26 = aida.histogram1D("omega: Top L2-6", 50, -0.0006, 0.0006);
        d0TopL26 = aida.histogram1D("d0: Top L2-6", 50, -5.0, 5.0);
        chisqTopL26 = aida.histogram1D("chisq: Top L2-6", 50, 0, 100.0);

        lambdaBotL26 = aida.histogram1D("slope: Bot L2-6", 50, -0.06, 0.0);
        z0BotL26 = aida.histogram1D("y0: Bot L0-3", 50, -2.5, 2.5);
        phi0BotL26 = aida.histogram1D("phi0: Bot L2-6", 50, -0.1, 0.15);
        omegaBotL26 = aida.histogram1D("omega: Bot L2-6", 50, -0.0006, 0.0006);
        d0BotL26 = aida.histogram1D("d0: Bot L2-6", 50, -5.0, 5.0);
        chisqBotL26 = aida.histogram1D("chisq: Bot L2-6", 50, 0, 100.0);

        plotterParsTop.region(0).plot(lambdaTopL26);
        plotterParsTop.region(1).plot(z0TopL26);
        plotterParsTop.region(2).plot(phi0TopL26);
        plotterParsTop.region(3).plot(omegaTopL26);
        plotterParsTop.region(4).plot(d0TopL26);
        plotterParsTop.region(5).plot(nTracks26Top);
        plotterParsTop.region(6).plot(nHits26Top);
        plotterParsTop.region(7).plot(chisqTopL26);

        plotterParsBot.region(0).plot(lambdaBotL26);
        plotterParsBot.region(1).plot(z0BotL26);
        plotterParsBot.region(2).plot(phi0BotL26);
        plotterParsBot.region(3).plot(omegaBotL26);
        plotterParsBot.region(4).plot(d0BotL26);
        plotterParsBot.region(5).plot(nTracks26Bot);
        plotterParsBot.region(6).plot(nHits26Bot);
        plotterParsBot.region(7).plot(chisqBotL26);

//        plotterTop.region(0).plot(nTracks26Top);
//        plotterTop.region(3).plot(nHits26Top);
        plotterParsTop.show();
        plotterParsBot.show();

        plotterResid = pfac.create("L01 Residuals");
        plotterResid.createRegions(2, 4);

        int regionCnt = 0;
        for (HpsSiSensor sensor : sensors) {
            if (sensor.getLayerNumber() > 4)  // only do first 2 modules
                continue;
            String sensorName = sensor.getName();
            int nChan = sensor.getNumberOfChannels();
            double readoutPitch = sensor.getReadoutStripPitch();
            double maxU = nChan * readoutPitch / 2;
            double width = getSensorLength(sensor);
            double maxV = width / 2;
            double minV = -maxV;
            residualY.put(sensorName, histogramFactory.createHistogram1D("Residual U (extrap-hit)" + sensorName, nBins, minRes, maxRes));
            errorY.put(sensorName, histogramFactory.createHistogram1D("Error U " + sensorName, nBins, 0, maxYerror));
            pullY.put(sensorName, histogramFactory.createHistogram1D("U Pulls " + sensorName, nBins, minPull, maxPull));
//            plot(plotterResid, residualY.get(sensorName), null, regionCnt);
            plotterResid.region(regionCnt).plot(residualY.get(sensorName));
//            System.out.println(regionCnt);
            regionCnt++;
        }

        plotterResid.show();

        plotterHTHResid = pfac.create("L01 HTH Residuals");
        plotterHTHResid.createRegions(2, 4);
        residualXHTH0Top = histogramFactory.createHistogram1D("Residual X L0 Top (extrap-hit)", nBins, minResX, maxResX);
        residualXHTH1Top = histogramFactory.createHistogram1D("Residual X L1 Top (extrap-hit)", nBins, minResX, maxResX);
        residualYHTH0Top = histogramFactory.createHistogram1D("Residual Y L0 Top (extrap-hit)", nBins, minResY, maxResY);
        residualYHTH1Top = histogramFactory.createHistogram1D("Residual Y L1 Top (extrap-hit)", nBins, minResY, maxResY);
        residualXHTH0Bot = histogramFactory.createHistogram1D("Residual X L0 Bot (extrap-hit)", nBins, minResX, maxResX);
        residualXHTH1Bot = histogramFactory.createHistogram1D("Residual X L1 Bot (extrap-hit)", nBins, minResX, maxResX);
        residualYHTH0Bot = histogramFactory.createHistogram1D("Residual Y L0 Bot (extrap-hit)", nBins, minResY, maxResY);
        residualYHTH1Bot = histogramFactory.createHistogram1D("Residual Y L1 Bot (extrap-hit)", nBins, minResY, maxResY);
        plotterHTHResid.region(0).plot(residualXHTH0Top);
        plotterHTHResid.region(1).plot(residualXHTH1Top);
        plotterHTHResid.region(2).plot(residualXHTH0Bot);
        plotterHTHResid.region(3).plot(residualXHTH1Bot);
        plotterHTHResid.region(4).plot(residualYHTH0Top);
        plotterHTHResid.region(5).plot(residualYHTH1Top);
        plotterHTHResid.region(6).plot(residualYHTH0Bot);
        plotterHTHResid.region(7).plot(residualYHTH1Bot);
        plotterHTHResid.show();
    }

    @Override
    public void process(EventHeader event) {
        aida.tree().cd("/");
        if (!event.hasCollection(HelicalTrackHit.class, helicalTrackHitCollectionName))
            return;
        if (!event.hasCollection(HelicalTrackHit.class, rotatedTrackHitCollectionName))
            return;
        if (!event.hasCollection(Track.class, l2to6CollectionName))
            return;

        if (matchFullTracks)
            if (!event.hasCollection(Track.class, fullTrackCollectionName))
                return;
        //Grab all the clusters in the event
        List<SiTrackerHitStrip1D> stripHits = event.get(SiTrackerHitStrip1D.class, stripHitOutputCollectionName);
        //get rotated helicaltrackhits
        List<HelicalTrackHit> rotHTH = event.get(HelicalTrackHit.class, rotatedTrackHitCollectionName);

        List<Track> l2to6tracks = event.get(Track.class, l2to6CollectionName);
        List<Track> fulltracks = new ArrayList<Track>();
        if (matchFullTracks)
            fulltracks = event.get(Track.class, fullTrackCollectionName);

        List<Track> l2to6tracksTop = splitTrackList(l2to6tracks, true);
        List<Track> l2to6tracksBot = splitTrackList(l2to6tracks, false);

        nTracks26Top.fill(l2to6tracksTop.size());
        nTracks26Bot.fill(l2to6tracksBot.size());
        double xAtL0AxTop = getSensorX(true, 1, true, false);
        double xAtL0StTop = getSensorX(true, 1, false, false);
        double xAtL1AxTop = getSensorX(true, 2, true, false);
        double xAtL1StTop = getSensorX(true, 2, false, false);
        double xAtL0AxBot = getSensorX(false, 1, true, false);
        double xAtL0StBot = getSensorX(false, 1, false, false);
        double xAtL1AxBot = getSensorX(false, 2, true, false);
        double xAtL1StBot = getSensorX(false, 2, false, false);

        double avtXL0Top = (xAtL0AxBot + xAtL0StBot) / 2.0;
        double avtXL1Top = (xAtL1AxBot + xAtL1StBot) / 2.0;
        double avtXL0Bot = (xAtL0AxBot + xAtL0StBot) / 2.0;
        double avtXL1Bot = (xAtL1AxBot + xAtL1StBot) / 2.0;
        Track matchedTrack = null;
        for (Track trk26 : l2to6tracks) {
            if(onlyElectrons&&trk26.getCharge()<0) //remember, charge in tracks is opposite the real charge
                continue;
            TrackState ts26 = trk26.getTrackStates().get(0);
//            if (matchFullTracks) {
//                matchedTrack = checkFullTrack(fulltracks, trk03, trk46);
//                if (matchedTrack == null)
//                    continue;
//            }
            double x0L26 = TrackUtils.getX0(ts26);
            double slL26 = ts26.getTanLambda();
            double y0L26 = ts26.getZ0();
            double yAtTargetL26 = (targetPosition - x0L26) * slL26 + y0L26;
            double yAtHingeL26 = (414.0 - x0L26) * slL26 + y0L26;
            if (slL26 > 0) {
                nHits26Top.fill(trk26.getTrackerHits().size());
                z0TopL26.fill(ts26.getZ0());
                lambdaTopL26.fill(ts26.getTanLambda());
                phi0TopL26.fill(ts26.getPhi());
                d0TopL26.fill(ts26.getD0());
                omegaTopL26.fill(ts26.getOmega());
                chisqTopL26.fill(trk26.getChi2());
            } else {
                nHits26Bot.fill(trk26.getTrackerHits().size());
                z0BotL26.fill(ts26.getZ0());
                lambdaBotL26.fill(ts26.getTanLambda());
                phi0BotL26.fill(ts26.getPhi());
                d0BotL26.fill(ts26.getD0());
                omegaBotL26.fill(ts26.getOmega());
                chisqBotL26.fill(trk26.getChi2());
            }

            int[] unusedLayers = getUnusedSvtLayer(trk26.getTrackerHits());
            //Get all track states for this track                        
            List<TrackState> TStates = trk26.getTrackStates();
//            System.out.println("Number of track states is = " + TStates.size());

            TrackState tState0 = getTrackState(trk26, 1);
            if (tState0 == null) {
                System.out.println("Couldn't find trackstate at layer 0");
                continue;
            }

//            System.out.println("Found Both Track States!!!");
            //Grab covariance matrix at track states
            double[] covAtIP = TStates.get(0).getCovMatrix();
            SymmetricMatrix LocCovAtIP = new SymmetricMatrix(5, covAtIP, true);

            Hep3Vector p0 = toHep3(tState0.getMomentum());
            double q = -trk26.getCharge();  //HelicalTrackFit flips sign of charge       

            //See if track is within acceptance of both the axial and stereo sensors of the unused layer
            Pair<HpsSiSensor, Pair<Integer, Hep3Vector>> axialSensorPair0 = isWithinSensorAcceptance(trk26, tState0, 1, true, p0, bFieldMap);
            Pair<HpsSiSensor, Pair<Integer, Hep3Vector>> stereoSensorPair0 = isWithinSensorAcceptance(trk26, tState0, 1, false, p0, bFieldMap);
            Pair<HpsSiSensor, Pair<Integer, Hep3Vector>> axialSensorPair1 = isWithinSensorAcceptance(trk26, tState0, 2, true, p0, bFieldMap);
            Pair<HpsSiSensor, Pair<Integer, Hep3Vector>> stereoSensorPair1 = isWithinSensorAcceptance(trk26, tState0, 2, false, p0, bFieldMap);

            //Skip track if it isn't within acceptance of both axial and stereo pairs of a given unused layer
            if (axialSensorPair0 == null || stereoSensorPair0 == null)
                continue;

            if (axialSensorPair1 == null || stereoSensorPair1 == null)
                continue;

            nTotTracks++;

            //Set axial and stereo sensors of the missing layer
            HpsSiSensor axialSensor0 = axialSensorPair0.getFirst();
            HpsSiSensor stereoSensor0 = stereoSensorPair0.getFirst();
            HpsSiSensor axialSensor1 = axialSensorPair1.getFirst();
            HpsSiSensor stereoSensor1 = stereoSensorPair1.getFirst();

            String sensorAxialName0 = axialSensor0.getName();
            String sensorStereoName0 = stereoSensor0.getName();
            String sensorAxialName1 = axialSensor1.getName();
            String sensorStereoName1 = stereoSensor1.getName();

            //Grab the track extrapolations at each sensor
            Hep3Vector axialExtrapPosSensor0 = axialSensorPair0.getSecond().getSecond();
            Hep3Vector stereoExtrapPosSensor0 = stereoSensorPair0.getSecond().getSecond();
            Hep3Vector axialExtrapPosSensor1 = axialSensorPair1.getSecond().getSecond();
            Hep3Vector stereoExtrapPosSensor1 = stereoSensorPair1.getSecond().getSecond();

            //Compute the extrapolation errors in u direction
            //TODO this needs to be done correctly
            double yErrorAxial0 = computeExtrapErrorY(trk26, tState0, axialSensor0, 1)[0];
            double yErrorStereo0 = computeExtrapErrorY(trk26, tState0, stereoSensor0, 1)[0];

            double yErrorAxial1 = computeExtrapErrorY(trk26, tState0, axialSensor1, 2)[0];
            double yErrorStereo1 = computeExtrapErrorY(trk26, tState0, stereoSensor1, 2)[0];

            //Compute the channel where the track extrapolates to in each sensor
            int chanAxial0 = axialSensorPair0.getSecond().getFirst();
            int chanStereo0 = stereoSensorPair0.getSecond().getFirst();
            int chanAxial1 = axialSensorPair1.getSecond().getFirst();
            int chanStereo1 = stereoSensorPair1.getSecond().getFirst();

            double trackP = toHep3(trk26.getTrackStates().get(0).getMomentum()).magnitude();

            double residualAxial0 = 9999;
            double residualStereo0 = 9999;
            double residualAxial1 = 9999;
            double residualStereo1 = 9999;
            //Loop over all reconstructed 1D hits on sensor of interest in the events
            for (SiTrackerHitStrip1D hit : stripHits) {
                //Get the sensor and position of the hit
                HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement();
                double[] hitPos = hit.getPosition();
                //Change to sensor coordinates
                Hep3Vector hitPosSensor = globalToSensor(toHep3(hitPos), sensor);
                //Check to see if the sensor of this hit is the same sensor you expect to see an axial hit
                if (sensorAxialName0 == sensor.getName()) {
                    //Compute the residual between extrapolated track and hit position
                    //Keep the value of the smallest residual
                    double residual = axialExtrapPosSensor0.x() - hitPosSensor.x();
                    if (Math.abs(residual) < Math.abs(residualAxial0))
                        residualAxial0 = residual;
                }
                if (sensorAxialName1 == sensor.getName()) {
                    //Compute the residual between extrapolated track and hit position
                    //Keep the value of the smallest residual
                    double residual = axialExtrapPosSensor1.x() - hitPosSensor.x();
                    if (Math.abs(residual) < Math.abs(residualAxial1))
                        residualAxial1 = residual;
                }
                //Check to see if the sensor of this hit is the same sensor you expect to see a stereo hit
                if (sensorStereoName0 == sensor.getName()) {
                    //Compute the residual between extrapolated track and hit position
                    //Keep the value of the smallest residual
                    double residual = stereoExtrapPosSensor0.x() - hitPosSensor.x();
                    if (Math.abs(residual) < Math.abs(residualStereo0))
                        residualStereo0 = residual;
                }
                if (sensorStereoName1 == sensor.getName()) {
                    //Compute the residual between extrapolated track and hit position
                    //Keep the value of the smallest residual
                    double residual = stereoExtrapPosSensor1.x() - hitPosSensor.x();
                    if (Math.abs(residual) < Math.abs(residualStereo1))
                        residualStereo1 = residual;
                }
            }

            //Fill histograms for residuals and pulls
            residualY.get(sensorAxialName0).fill(residualAxial0);
//            pullY.get(sensorAxialName0).fill(residualAxial0 / yErrorAxial0);
            residualY.get(sensorStereoName0).fill(residualStereo0);
//            pullY.get(sensorStereoName0).fill(residualStereo0 / yErrorStereo0);
            residualY.get(sensorAxialName1).fill(residualAxial1);
//            pullY.get(sensorAxialName1).fill(residualAxial1 / yErrorAxial1);
            residualY.get(sensorStereoName1).fill(residualStereo1);
//            pullY.get(sensorStereoName1).fill(residualStereo1 / yErrorStereo1);

            double residualHTHZ0 = 9999;
            double residualHTHY0 = 9999;
            double residualHTHZ1 = 9999;
            double residualHTHY1 = 9999;
//            extrapolateHelixToXPlane();
            double xAtL0 = 9999;
            double xAtL1 = 9999;
            for (HelicalTrackHit hth : rotHTH) {
                int layer = hth.Layer();
                if (layer > 3)
                    continue;
                if (tState0.getTanLambda() * hth.getPosition()[2] < 0)
                    continue;
                if (tState0.getTanLambda() > 0) {
                    xAtL0 = avtXL0Top;
                    xAtL1 = avtXL1Top;
                } else {
                    xAtL0 = avtXL0Bot;
                    xAtL1 = avtXL1Bot;
                }

                if (layer == 1) {
                    Hep3Vector trPos = TrackUtils.extrapolateHelixToXPlane(tState0, xAtL0);
//                    System.out.println("axial L0 Extrapolation = "+axialExtrapPosSensor0.toString());
                    double resZ = getResidualHTHZ(hth, trPos);
                    double resY = getResidualHTHY(hth, trPos);
                    if (Math.abs(resZ) < Math.abs(residualHTHZ0)) {
                        residualHTHZ0 = resZ;
                        residualHTHY0 = resY;
                    }
                }
                if (layer == 3) {
                    Hep3Vector trPos = TrackUtils.extrapolateHelixToXPlane(tState0, xAtL1);
//                    System.out.println("axial L0 Extrapolation = "+axialExtrapPosSensor0.toString());
                    double resZ = getResidualHTHZ(hth, trPos);
                    double resY = getResidualHTHY(hth, trPos);
                    if (Math.abs(resZ) < Math.abs(residualHTHZ1)) {
                        residualHTHZ1 = resZ;
                        residualHTHY1 = resY;
                    }
                }
            }
//            boolean isTop = false;
//            if (tState0.getTanLambda() > 0)
//                isTop = true;
//            if (residualHTHZ0 == 9999)
//                System.out.println("Didn't find a hit in L0???  " + isTop + "; tracks thus far = " + nTotTracks);
//            if (residualHTHZ1 == 9999)
//                System.out.println("Didn't find a hit in L1???  " + isTop + "; tracks thus far = " + nTotTracks);

            if (tState0.getTanLambda() > 0) {
                residualXHTH0Top.fill(residualHTHY0);
                residualYHTH0Top.fill(residualHTHZ0);
                residualXHTH1Top.fill(residualHTHY1);
                residualYHTH1Top.fill(residualHTHZ1);
            } else {
                residualXHTH0Bot.fill(residualHTHY0);
                residualYHTH0Bot.fill(residualHTHZ0);
                residualXHTH1Bot.fill(residualHTHY1);
                residualYHTH1Bot.fill(residualHTHZ1);
            }

        }
    }

    @Override
    public void endOfData() {
//        if (outputPlots != null
//            try {
//                plotterTop.writeToFile(outputPlots + "-deltasTop.gif");
//                plotterBot.writeToFile(outputPlots + "-deltasBottom.gif");
//            } catch (IOException ex) {
//                Logger.getLogger(TrackingReconPlots.class.getName()).log(Level.SEVERE, null, ex);
//            }
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

    //Get the track state at the previous sensor
    private TrackState getTrackState(Track track, int unusedLay) {
        int layer = -1;
        boolean isTop = track.getTrackStates().get(0).getTanLambda() > 0;
//        System.out.println("Getting track state for layer =  " + unusedLay);
        //If unused layer is L1, then get trackstate at IP
        if (unusedLay == 1)
//            System.out.println("Returning TS at 0");
            return track.getTrackStates().get(0);
        else
            layer = unusedLay - 1;
        HpsSiSensor sensorHole = getSensor(track, layer, isTop, true);
//        System.out.println("sensorHole = " + sensorHole.getName());
        HpsSiSensor sensorSlot = getSensor(track, layer, isTop, false);
//        System.out.println("sensorSlot = " + sensorSlot.getName());
        TrackState tState = TrackStateUtils.getTrackStateAtSensor(track, sensorHole.getMillepedeId());
//        System.out.println("Returning TS at millipede layer = " + layer);
        if (tState == null)
            tState = TrackStateUtils.getTrackStateAtSensor(track, sensorSlot.getMillepedeId());
        return tState;
    }

    //Return the HpsSiSensor for a given top/bottom track, layer, axial/stereo, and slot/hole
    private HpsSiSensor getSensor(Track track, int layer, boolean isAxial, boolean isHole) {
        double tanLambda = track.getTrackStates().get(0).getTanLambda();
        int outerLayer = 4;
//        System.out.println("getSensor:: layer = " + layer + "; isAxial = " + isAxial);
        if (sensors.size() > 36)
            outerLayer = 5;
        for (HpsSiSensor sensor : sensors) {
//            System.out.println(sensor.getName());
            int senselayer = (sensor.getLayerNumber() + 1) / 2;
            if (senselayer != layer)
                continue;
            if ((tanLambda > 0 && !sensor.isTopLayer()) || (tanLambda < 0 && sensor.isTopLayer()))
                continue;
            if ((isAxial && !sensor.isAxial()) || (!isAxial && sensor.isAxial()))
                continue;
            if (layer < outerLayer && layer > 0)
                return sensor;
            else {
                if ((!sensor.getSide().matches("ELECTRON") && isHole) || (sensor.getSide().matches("ELECTRON") && !isHole))
                    continue;
                return sensor;
            }
        }
        return null;
    }

    private double getSensorX(boolean isTop, int layer, boolean isAxial, boolean isHole) {
        double x = -9999;
        int outerLayer = 4;
//        System.out.println("getSensor:: layer = " + layer + "; isAxial = " + isAxial);
        if (sensors.size() > 36)
            outerLayer = 5;
        for (HpsSiSensor sensor : sensors) {
            //           System.out.println(sensor.getName());
            int senselayer = (sensor.getLayerNumber() + 1) / 2;
            if (senselayer != layer)
                continue;
            if ((isTop && !sensor.isTopLayer()) || (!isTop && sensor.isTopLayer()))
                continue;
            if ((isAxial && !sensor.isAxial()) || (!isAxial && sensor.isAxial()))
                continue;
            if (layer < outerLayer && layer > 0)
                return sensor.getGeometry().getPosition().z();
            else {
                if ((!sensor.getSide().matches("ELECTRON") && isHole) || (sensor.getSide().matches("ELECTRON") && !isHole))
                    continue;
                return sensor.getGeometry().getPosition().z();
            }
        }
        return x;
    }

    //Checks to see if track is within acceptance of both axial and stereo sensors at a given layer
    //Also returns channel number of the intersection
    private Pair<HpsSiSensor, Pair<Integer, Hep3Vector>> isWithinSensorAcceptance(Track track, TrackState tState, int layer, boolean axial, Hep3Vector p, FieldMap fieldMap) {

        HpsSiSensor axialSensorHole = getSensor(track, layer, true, true);
        HpsSiSensor axialSensorSlot = getSensor(track, layer, true, false);
        HpsSiSensor stereoSensorHole = getSensor(track, layer, false, true);
        HpsSiSensor stereoSensorSlot = getSensor(track, layer, false, false);
//        System.out.println(axialSensorHole.getName());
//        System.out.println(axialSensorSlot.getName());
//        System.out.println(stereoSensorHole.getName());
//        System.out.println(stereoSensorSlot.getName());
        HelicalTrackFit htf = TrackUtils.getHTF(tState);

        Hep3Vector axialTrackHolePos = TrackStateUtils.getLocationAtSensor(htf, axialSensorHole, bfield);
        Hep3Vector axialTrackSlotPos = TrackStateUtils.getLocationAtSensor(htf, axialSensorSlot, bfield);
        Hep3Vector stereoTrackHolePos = TrackStateUtils.getLocationAtSensor(htf, stereoSensorHole, bfield);
        Hep3Vector stereoTrackSlotPos = TrackStateUtils.getLocationAtSensor(htf, stereoSensorSlot, bfield);

        Pair<Boolean, Pair<Integer, Hep3Vector>> axialHolePair = this.sensorContainsTrack(axialTrackHolePos, axialSensorHole);
        Pair<Boolean, Pair<Integer, Hep3Vector>> axialSlotPair = this.sensorContainsTrack(axialTrackSlotPos, axialSensorSlot);
        Pair<Boolean, Pair<Integer, Hep3Vector>> stereoHolePair = this.sensorContainsTrack(stereoTrackHolePos, stereoSensorHole);
        Pair<Boolean, Pair<Integer, Hep3Vector>> stereoSlotPair = this.sensorContainsTrack(stereoTrackSlotPos, stereoSensorSlot);

        if (axialHolePair.getFirst() && axial)
            return new Pair<>(axialSensorHole, axialHolePair.getSecond());

        if (axialSlotPair.getFirst() && axial)
            return new Pair<>(axialSensorSlot, axialSlotPair.getSecond());

        if (stereoHolePair.getFirst() && !axial)
            return new Pair<>(stereoSensorHole, stereoHolePair.getSecond());

        if (stereoSlotPair.getFirst() && !axial)
            return new Pair<>(stereoSensorSlot, stereoSlotPair.getSecond());

        return null;
    }

    //Checks to see if track is in acceptance of sensor. Computes within sensor frame
    //Also return channel number of the position
    public Pair<Boolean, Pair<Integer, Hep3Vector>> sensorContainsTrack(Hep3Vector trackPosition, HpsSiSensor sensor) {
        Hep3Vector pos = globalToSensor(trackPosition, sensor);
        int nChan = sensor.getNumberOfChannels();
        int chan = getChan(pos, sensor);
        double width = getSensorLength(sensor);
        Pair<Integer, Hep3Vector> pair = new Pair<>(chan, pos);
        if (chan < -this.chanExtd || chan > (nChan + this.chanExtd))
            return new Pair<>(false, pair);
        if (Math.abs(pos.y()) > width / 2)
            return new Pair<>(false, pair);
        return new Pair<>(true, pair);
    }

    //Returns the horizontal length of the sensor
    protected double getSensorLength(HpsSiSensor sensor) {

        double length = 0;
//        System.out.println(sensor.getName());
        // Get the faces normal to the sensor
        final List<Polygon3D> faces = ((Box) sensor.getGeometry().getLogicalVolume().getSolid())
                .getFacesNormalTo(new BasicHep3Vector(0, 0, 1));
        for (final Polygon3D face : faces) {

            // Loop through the edges of the sensor face and find the longest one
            final List<LineSegment3D> edges = face.getEdges();
            for (final LineSegment3D edge : edges)
                if (edge.getLength() > length)
                    length = edge.getLength();
        }
        return length;
    }

    //Converts position into sensor frame
    private Hep3Vector globalToSensor(Hep3Vector trkpos, HpsSiSensor sensor) {
//        System.out.println(sensor.getName());
        SiSensorElectrodes electrodes = sensor.getReadoutElectrodes(ChargeCarrier.HOLE);
        if (electrodes == null) {
            electrodes = sensor.getReadoutElectrodes(ChargeCarrier.ELECTRON);
            System.out.println("Charge Carrier is NULL");
        }
        return electrodes.getGlobalToLocal().transformed(trkpos);
    }

    //Converts double array into Hep3Vector
    private Hep3Vector toHep3(double[] arr) {
        return new BasicHep3Vector(arr[0], arr[1], arr[2]);
    }

    //Returns channel number of a given position in the sensor frame
    private int getChan(Hep3Vector pos, HpsSiSensor sensor) {
        double readoutPitch = sensor.getReadoutStripPitch();
        int nChan = sensor.getNumberOfChannels();
        double height = readoutPitch * nChan;
        return (int) ((height / 2 - pos.x()) / readoutPitch);
    }

    //Computes track extrapolation error in sensor frame
    //This probably needs to be fixed
    private double[] computeExtrapErrorY(Track track, TrackState tState, HpsSiSensor sensor, int unusedLay) {
        Hep3Vector sensorPos = sensor.getGeometry().getPosition();
        double bfac = Constants.fieldConversion * bfield;
        //Grab array of covariance matrix and build 5x5 covariance matrix of track parameters
        double[] cov = tState.getCovMatrix();
        HelicalTrackFit htf = TrackUtils.getHTF(tState);
        SymmetricMatrix LocCov = new SymmetricMatrix(5, cov, true);
        Matrix locCov = new Matrix(5, 5);
        for (int i = 0; i < 5; i++)
            for (int j = 0; j < 5; j++)
                locCov.set(i, j, LocCov.e(i, j));

        // Track direction
        double sinLambda = sin(htf.slope());
        double cosLambda = sqrt(1.0 - sinLambda * sinLambda);

        Hep3Vector hitPos = new BasicHep3Vector(0, 0, 0);

        if (unusedLay != 1) {
            boolean isTop = sensor.isTopLayer();
            boolean isHole = sensor.getSide().matches("ELECTRON");
//            System.out.println(sensor.getName());
            HpsSiSensor prevSensor = getSensor(track, unusedLay - 1, isTop, isHole);
//            System.out.println(prevSensor);
            hitPos = prevSensor.getGeometry().getPosition();
        }

        //Calculate the distance s the particle travels from track state to sensor of interest
        double step1 = HelixUtils.PathToXPlane(htf, hitPos.z(), 0, 0).get(0);
        double step2 = HelixUtils.PathToXPlane(htf, sensorPos.z(), 0, 0).get(0);
        double step = step2 - step1;

        //Grab simple jacobian in lambda phi coordinates
        BasicMatrix jacPointToPoint = GblUtils.gblSimpleJacobianLambdaPhi(step, cosLambda, abs(bfac));

        Matrix jacobian = new Matrix(5, 5);
        for (int i = 0; i < 5; i++)
            for (int j = 0; j < 5; j++)
                jacobian.set(i, j, jacPointToPoint.e(i, j));

        //Grab jacobian to convert from CL to perigee coordinates and vice-versa
        Matrix ClToPerJac = GblUtils.getCLToPerigeeJacobian(htf, new HpsHelicalTrackFit(TrackUtils.getHTF(tState)), bfield);
        Matrix PerToClJac = ClToPerJac.inverse();
        //First convert perigee covariance to CL coordinates, then compute the new covariance matrix propagated to the sensor of interest
        Matrix MsCov = jacobian.times(PerToClJac.times(locCov.times(PerToClJac.transpose())).times(jacobian.transpose()));
        //Transform this covariance matrix back to perigee coordinates to get the new errors of the track parameters
        Matrix helixCovariance = ClToPerJac.times(MsCov.times(ClToPerJac.transpose()));
        //Fill new covariance matrix with covariances in x and y directions (z can be ignored)
        Matrix MsCov2 = new Matrix(3, 3);
        MsCov2.set(0, 0, MsCov.get(3, 3));
        MsCov2.set(0, 1, MsCov.get(3, 4));
        MsCov2.set(1, 0, MsCov.get(4, 3));
        MsCov2.set(1, 1, MsCov.get(4, 4));

        //Tranform the covariance matrix into the sensor frame u,v to get the final covariance matrix
        SiSensorElectrodes electrodes = sensor.getReadoutElectrodes(ChargeCarrier.HOLE);
        Matrix rot = Hep3ToMatrix(electrodes.getGlobalToLocal().getRotation().getRotationMatrix());
        Matrix measMsCov = rot.times(MsCov2.times(rot.transpose()));

        //Fill histograms of track parameter errors
        double d0_err = Math.sqrt(helixCovariance.get(HelicalTrackFit.dcaIndex, HelicalTrackFit.dcaIndex));
        double z0_err = Math.sqrt(helixCovariance.get(HelicalTrackFit.z0Index, HelicalTrackFit.z0Index));
        double tanlambda_err = Math.sqrt(helixCovariance.get(HelicalTrackFit.slopeIndex, HelicalTrackFit.slopeIndex));
        double phi0_err = Math.sqrt(helixCovariance.get(HelicalTrackFit.phi0Index, HelicalTrackFit.phi0Index));
        double omega_err = Math.sqrt(helixCovariance.get(HelicalTrackFit.curvatureIndex, HelicalTrackFit.curvatureIndex));

        String sensorName = sensor.getName();
//        D0_err.get(sensorName).fill(d0_err);
//        Z0_err.get(sensorName).fill(z0_err);
//        Tanlambda_err.get(sensorName).fill(tanlambda_err);
//        Phi0_err.get(sensorName).fill(phi0_err);
//        Omega_err.get(sensorName).fill(omega_err);

        //Calculate errors in the u and v directions
        return new double[]{Math.sqrt(measMsCov.get(0, 0)), Math.sqrt(measMsCov.get(1, 1))};
    }

    private Matrix Hep3ToMatrix(Hep3Matrix mat) {
        int Nrows = mat.getNRows();
        int Ncolumns = mat.getNColumns();
        Matrix matrix = new Matrix(Nrows, Ncolumns);
        for (int i = 0; i < Nrows; i++)
            for (int j = 0; j < Ncolumns; j++)
                matrix.set(i, j, mat.e(i, j));
        return matrix;
    }

    private int[] getUnusedSvtLayer(List<TrackerHit> stereoHits) {
        int[] svtLayer = new int[7];
        int[] unusedLayer = new int[7];
        // Loop over all of the stereo hits associated with the track
        for (TrackerHit stereoHit : stereoHits) {

            // Retrieve the sensor associated with one of the hits.  This will
            // be used to retrieve the layer number
            HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) stereoHit.getRawHits().get(0)).getDetectorElement();

            // Retrieve the layer number by using the sensor
            int layer = (sensor.getLayerNumber() + 1) / 2;

            // If a hit is associated with that layer, increment its 
            // corresponding counter
            svtLayer[layer - 1]++;
        }

        // Loop through the layer counters and find which layer has not been
        // incremented i.e. is unused by the track
        int cnt = 0;
        for (int layer = 0; layer < svtLayer.length; layer++)
            if (svtLayer[layer] == 0) {
                int ulayer = layer + 1;
//                System.out.println("unused layer = " + ulayer);
                unusedLayer[cnt] = ulayer;
                cnt++;
            }
        return unusedLayer;
    }

    private double getResidualHTHZ(HelicalTrackHit hth, Hep3Vector extrPos) {
        return extrPos.z() - hth.getPosition()[2];
    }

    private double getResidualHTHY(HelicalTrackHit hth, Hep3Vector extrPos) {
        return extrPos.y() - hth.getPosition()[1];
    }

}
