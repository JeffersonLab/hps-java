/**
 * Driver used to compute SVT hit efficiencies at each sensor
 * as a function of strip and y
 * Unbiased Hit Residuals are also computed
 * TODO Cleanup Code, add comments
 * TODO Move general functions to a different driver
 * TODO Fix u error function
 * TODO Update Track Extrapolations
 */
/**
 * @author mrsolt
 *
 */
package org.hps.svt;

import static java.lang.Math.abs;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogramFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.ITree;
import hep.physics.matrix.BasicMatrix;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;
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
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.apache.commons.math3.util.Pair;
import org.hps.conditions.beam.BeamEnergy.BeamEnergyCollection;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.AbstractSvtDaqMapping;
import org.hps.conditions.svt.SvtChannel;
import org.hps.conditions.svt.SvtConditions;
import org.hps.conditions.svt.SvtDaqMapping;
import org.hps.conditions.svt.SvtDaqMapping.SvtDaqMappingCollection;
import org.hps.conditions.svt.SvtChannel.SvtChannelCollection;
import org.hps.recon.tracking.HpsHelicalTrackFit;
import org.hps.recon.tracking.TrackStateUtils;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.gbl.GblUtils;
import org.hps.recon.tracking.gbl.matrix.Matrix;

public class SVTHitLevelPlots extends Driver {

    // Plotting
    protected AIDA aida = AIDA.defaultInstance();
    ITree tree;
    IHistogramFactory histogramFactory;

    //List of Sensors
    private List<HpsSiSensor> sensors = null;

    //List of Histograms
    Map<String, IHistogram1D> numberOfTracksChannel = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerChannel = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyChannel = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksY = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerY = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyY = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksP = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerP = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyP = new HashMap<String, IHistogram1D>();

    Map<String, IHistogram1D> numberOfTracksChannelEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerChannelEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyChannelEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksYEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerYEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyYEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksPEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerPEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPEle = new HashMap<String, IHistogram1D>();

    Map<String, IHistogram1D> numberOfTracksChannelPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerChannelPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyChannelPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksYPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerYPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyYPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksPPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerPPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPPos = new HashMap<String, IHistogram1D>();

    Map<String, IHistogram1D> numberOfTracksChannelCorrected = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyChannelCorrected = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksYCorrected = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyYCorrected = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksPCorrected = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPCorrected = new HashMap<String, IHistogram1D>();

    Map<String, IHistogram1D> numberOfTracksChannelCorrectedEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyChannelCorrectedEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksYCorrectedEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyYCorrectedEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksPCorrectedEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPCorrectedEle = new HashMap<String, IHistogram1D>();

    Map<String, IHistogram1D> numberOfTracksChannelCorrectedPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyChannelCorrectedPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksYCorrectedPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyYCorrectedPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksPCorrectedPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPCorrectedPos = new HashMap<String, IHistogram1D>();

    Map<String, IHistogram1D> TotalEff = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> TotalEffEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> TotalEffPos = new HashMap<String, IHistogram1D>();

    Map<String, IHistogram1D> TotalCorrectedEff = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> TotalCorrectedEffEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> TotalCorrectedEffPos = new HashMap<String, IHistogram1D>();

    Map<String, IHistogram1D> hitEfficiencyChannelerr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyYerr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPerr = new HashMap<String, IHistogram1D>();

    Map<String, IHistogram1D> hitEfficiencyChannelCorrectederr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyYCorrectederr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPCorrectederr = new HashMap<String, IHistogram1D>();

    Map<String, IHistogram1D> hitEfficiencyChannelEleerr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyYEleerr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPEleerr = new HashMap<String, IHistogram1D>();

    Map<String, IHistogram1D> hitEfficiencyChannelCorrectedEleerr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyYCorrectedEleerr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPCorrectedEleerr = new HashMap<String, IHistogram1D>();

    Map<String, IHistogram1D> hitEfficiencyChannelPoserr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyYPoserr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPPoserr = new HashMap<String, IHistogram1D>();

    Map<String, IHistogram1D> hitEfficiencyChannelCorrectedPoserr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyYCorrectedPoserr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPCorrectedPoserr = new HashMap<String, IHistogram1D>();

    Map<String, IHistogram1D> TotalEfferr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> TotalEffEleerr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> TotalEffPoserr = new HashMap<String, IHistogram1D>();

    Map<String, IHistogram1D> TotalCorrectedEfferr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> TotalCorrectedEffEleerr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> TotalCorrectedEffPoserr = new HashMap<String, IHistogram1D>();

    Map<String, IHistogram1D> residualY = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> errorY = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> pullY = new HashMap<String, IHistogram1D>();

    Map<String, IHistogram2D> residualYvsV = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> errorYvsV = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> pullYvsV = new HashMap<String, IHistogram2D>();

    Map<String, IHistogram2D> residualYvsU = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> errorYvsU = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> pullYvsU = new HashMap<String, IHistogram2D>();

    Map<String, IHistogram1D> residualYEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> errorYEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> pullYEle = new HashMap<String, IHistogram1D>();

    Map<String, IHistogram2D> residualYvsVEle = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> errorYvsVEle = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> pullYvsVEle = new HashMap<String, IHistogram2D>();

    Map<String, IHistogram2D> residualYvsUEle = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> errorYvsUEle = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> pullYvsUEle = new HashMap<String, IHistogram2D>();

    Map<String, IHistogram1D> residualYPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> errorYPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> pullYPos = new HashMap<String, IHistogram1D>();

    Map<String, IHistogram2D> residualYvsVPos = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> errorYvsVPos = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> pullYvsVPos = new HashMap<String, IHistogram2D>();

    Map<String, IHistogram2D> residualYvsUPos = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> errorYvsUPos = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> pullYvsUPos = new HashMap<String, IHistogram2D>();

    Map<String, IHistogram2D> residualYvsP = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> errorYvsP = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> pullYvsP = new HashMap<String, IHistogram2D>();

    Map<String, IHistogram2D> residualYvsPEle = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> errorYvsPEle = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> pullYvsPEle = new HashMap<String, IHistogram2D>();

    Map<String, IHistogram2D> residualYvsPPos = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> errorYvsPPos = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> pullYvsPPos = new HashMap<String, IHistogram2D>();

    Map<String, IHistogram1D> D0 = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> Z0 = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> Tanlambda = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> Phi0 = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> Omega = new HashMap<String, IHistogram1D>();

    Map<String, IHistogram1D> D0_err = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> Z0_err = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> Tanlambda_err = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> Phi0_err = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> Omega_err = new HashMap<String, IHistogram1D>();

    //Histogram Settings
    int nBins = 50;
    double maxPull = 7;
    double minPull = -maxPull;
    double maxRes = 0.5;
    double minRes = -maxRes;
    double maxYerror = 0.1;
    double maxD0 = 5;
    double minD0 = -maxD0;
    double maxZ0 = 10;
    double minZ0 = -maxZ0;
    double maxTLambda = 0.1;
    double minTLambda = -maxTLambda;
    double maxPhi0 = 0.2;
    double minPhi0 = -maxPhi0;
    double maxOmega = 0.001;
    double minOmega = -maxOmega;
    double maxD0Err = 1.5;
    double minD0Err = 0;
    double maxZ0Err = 1;
    double minZ0Err = 0;
    double maxTLambdaErr = 0.005;
    double minTLambdaErr = 0;
    double maxPhi0Err = 0.01;
    double minPhi0Err = 0;
    double maxOmegaErr = 0.0001;
    double minOmegaErr = 0;

    String atIP = "IP";

    //Collection Strings
    private String stripHitOutputCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private String GBLTrackCollectionName = "GBLTracks";

    //Bfield
    protected static double bfield;
    FieldMap bFieldMap = null;

    private static final String SUBDETECTOR_NAME = "Tracker";

    String outputFileName = "channelEff.txt";
    boolean cleanFEE = false;
    int nLay = 6;
    double nSig = 5;
    boolean maskBadChannels = false;
    int chanExtd = 0;

    //Daq map
    SvtChannelCollection channelMap;
    SvtDaqMappingCollection daqMap;

    public void setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
    }

    public void setNLay(int nLay) {
        this.nLay = nLay;
    }

    public void setSig(double nSig) {
        this.nSig = nSig;
    }

    public void setChanExtd(int chanExtd) {
        this.chanExtd = chanExtd;
    }

    public void setMaskBadChannels(boolean maskBadChannels) {
        this.maskBadChannels = maskBadChannels;
    }

    public void setCleanFEE(boolean cleanFEE) {
        this.cleanFEE = cleanFEE;
    }

    //Beam Energy
    double ebeam;

    public void detectorChanged(Detector detector) {

        aida.tree().cd("/");
        tree = aida.tree();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);

        //Grab channel map and daq map from conditions database
        DatabaseConditionsManager mgr = DatabaseConditionsManager.getInstance();
        SvtConditions svtConditions = mgr.getCachedConditions(SvtConditions.class, "svt_conditions").getCachedData();

        channelMap = svtConditions.getChannelMap();
        daqMap = svtConditions.getDaqMap();

        //Set Beam Energy
        BeamEnergyCollection beamEnergyCollection
                = this.getConditionsManager().getCachedConditions(BeamEnergyCollection.class, "beam_energies").getCachedData();
        ebeam = beamEnergyCollection.get(0).getBeamEnergy();

        bfield = TrackUtils.getBField(detector).magnitude();
        bFieldMap = detector.getFieldMap();

        // Get the HpsSiSensor objects from the tracker detector element
        sensors = detector.getSubdetector(SUBDETECTOR_NAME)
                .getDetectorElement().findDescendants(HpsSiSensor.class);

        // If the detector element had no sensors associated with it, throw
        // an exception
        if (sensors.size() == 0)
            throw new RuntimeException("No sensors were found in this detector.");

        //Setup Plots
        D0.put(atIP, histogramFactory.createHistogram1D("D0 " + atIP, nBins, minD0, maxD0));
        Z0.put(atIP, histogramFactory.createHistogram1D("Z0 " + atIP, nBins, minZ0, maxZ0));
        Tanlambda.put(atIP, histogramFactory.createHistogram1D("TanLambda " + atIP, nBins, minTLambda, maxTLambda));
        Phi0.put(atIP, histogramFactory.createHistogram1D("Phi0 " + atIP, nBins, minPhi0, maxPhi0));
        Omega.put(atIP, histogramFactory.createHistogram1D("Omega " + atIP, nBins, minOmega, maxOmega));

        D0_err.put(atIP, histogramFactory.createHistogram1D("D0 Error " + atIP, nBins, minD0Err, maxD0Err));
        Z0_err.put(atIP, histogramFactory.createHistogram1D("Z0 Error " + atIP, nBins, minZ0Err, maxZ0Err));
        Tanlambda_err.put(atIP, histogramFactory.createHistogram1D("TanLambda Error " + atIP, nBins, minTLambdaErr, maxTLambdaErr));
        Phi0_err.put(atIP, histogramFactory.createHistogram1D("Phi0 Error " + atIP, nBins, minPhi0Err, maxPhi0Err));
        Omega_err.put(atIP, histogramFactory.createHistogram1D("Omega Error " + atIP, nBins, minOmegaErr, maxOmegaErr));

        for (HpsSiSensor sensor : sensors) {
            String sensorName = sensor.getName();
            int nChan = sensor.getNumberOfChannels();
            double readoutPitch = sensor.getReadoutStripPitch();
            double maxU = nChan * readoutPitch / 2;
            double width = getSensorLength(sensor);
            double maxV = width / 2;
            double minV = -maxV;
            numberOfTracksChannel.put(sensorName, histogramFactory.createHistogram1D("Number of Tracks Channel " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
            numberOfTracksWithHitOnMissingLayerChannel.put(sensorName, histogramFactory.createHistogram1D("Number of Tracks With Hit Channel " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
            hitEfficiencyChannel.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency Channel " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
            numberOfTracksY.put(sensorName, histogramFactory.createHistogram1D("Number of Tracks Y " + sensorName, nBins, -maxU, maxU));
            numberOfTracksWithHitOnMissingLayerY.put(sensorName, histogramFactory.createHistogram1D("Number of Tracks With Hit Y " + sensorName, nBins, -maxU, maxU));
            hitEfficiencyY.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency Y " + sensorName, nBins, -maxU, maxU));
            numberOfTracksP.put(sensorName, histogramFactory.createHistogram1D("Number of Tracks P " + sensorName, nBins, 0, 1.3 * ebeam));
            numberOfTracksWithHitOnMissingLayerP.put(sensorName, histogramFactory.createHistogram1D("Number of Tracks With Hit P " + sensorName, nBins, 0, 1.3 * ebeam));
            hitEfficiencyP.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency P " + sensorName, nBins, 0, 1.3 * ebeam));

            numberOfTracksChannelEle.put(sensorName, histogramFactory.createHistogram1D("Number of Tracks Channel Ele " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
            numberOfTracksWithHitOnMissingLayerChannelEle.put(sensorName, histogramFactory.createHistogram1D("Number of Tracks With Hit Channel Ele " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
            hitEfficiencyChannelEle.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency Channel Ele " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
            numberOfTracksYEle.put(sensorName, histogramFactory.createHistogram1D("Number of Tracks Y Ele " + sensorName, nBins, -maxU, maxU));
            numberOfTracksWithHitOnMissingLayerYEle.put(sensorName, histogramFactory.createHistogram1D("Number of Tracks With Hit Y Ele " + sensorName, nBins, -maxU, maxU));
            hitEfficiencyYEle.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency Y Ele " + sensorName, nBins, -maxU, maxU));
            numberOfTracksPEle.put(sensorName, histogramFactory.createHistogram1D("Number of Tracks P Ele " + sensorName, nBins, 0, 1.3 * ebeam));
            numberOfTracksWithHitOnMissingLayerPEle.put(sensorName, histogramFactory.createHistogram1D("Number of Tracks With Hit P Ele " + sensorName, nBins, 0, 1.3 * ebeam));
            hitEfficiencyPEle.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency P Ele " + sensorName, nBins, 0, 1.3 * ebeam));

            numberOfTracksChannelPos.put(sensorName, histogramFactory.createHistogram1D("Number of Tracks Channel Pos " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
            numberOfTracksWithHitOnMissingLayerChannelPos.put(sensorName, histogramFactory.createHistogram1D("Number of Tracks With Hit Channel Pos " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
            hitEfficiencyChannelPos.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency Channel Pos " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
            numberOfTracksYPos.put(sensorName, histogramFactory.createHistogram1D("Number of Tracks Y Pos " + sensorName, nBins, -maxU, maxU));
            numberOfTracksWithHitOnMissingLayerYPos.put(sensorName, histogramFactory.createHistogram1D("Number of Tracks With Hit Y Pos " + sensorName, nBins, -maxU, maxU));
            hitEfficiencyYPos.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency Y Pos " + sensorName, nBins, -maxU, maxU));
            numberOfTracksPPos.put(sensorName, histogramFactory.createHistogram1D("Number of Tracks P Pos " + sensorName, nBins, 0, 1.3 * ebeam));
            numberOfTracksWithHitOnMissingLayerPPos.put(sensorName, histogramFactory.createHistogram1D("Number of Tracks With Hit P Pos " + sensorName, nBins, 0, 1.3 * ebeam));
            hitEfficiencyPPos.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency P Pos " + sensorName, nBins, 0, 1.3 * ebeam));

            numberOfTracksChannelCorrected.put(sensorName, histogramFactory.createHistogram1D("Number of Tracks Channel Corrected " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
            hitEfficiencyChannelCorrected.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency Channel Corrected " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
            numberOfTracksYCorrected.put(sensorName, histogramFactory.createHistogram1D("Number of Tracks Y Corrected " + sensorName, nBins, -maxU, maxU));
            hitEfficiencyYCorrected.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency Y Corrected " + sensorName, nBins, -maxU, maxU));
            numberOfTracksPCorrected.put(sensorName, histogramFactory.createHistogram1D("Number of Tracks P Corrected " + sensorName, nBins, 0, 1.3 * ebeam));
            hitEfficiencyPCorrected.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency P Corrected " + sensorName, nBins, 0, 1.3 * ebeam));

            numberOfTracksChannelCorrectedEle.put(sensorName, histogramFactory.createHistogram1D("Number of Tracks Channel Corrected Ele " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
            hitEfficiencyChannelCorrectedEle.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency Channel Corrected Ele " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
            numberOfTracksYCorrectedEle.put(sensorName, histogramFactory.createHistogram1D("Number of Tracks Y Corrected Ele " + sensorName, nBins, -maxU, maxU));
            hitEfficiencyYCorrectedEle.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency Y Corrected Ele " + sensorName, nBins, -maxU, maxU));
            numberOfTracksPCorrectedEle.put(sensorName, histogramFactory.createHistogram1D("Number of Tracks P Corrected Ele " + sensorName, nBins, 0, 1.3 * ebeam));
            hitEfficiencyPCorrectedEle.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency P Corrected Ele " + sensorName, nBins, 0, 1.3 * ebeam));

            numberOfTracksChannelCorrectedPos.put(sensorName, histogramFactory.createHistogram1D("Number of Tracks Channel Corrected Pos " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
            hitEfficiencyChannelCorrectedPos.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency Channel Corrected Pos " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
            numberOfTracksYCorrectedPos.put(sensorName, histogramFactory.createHistogram1D("Number of Tracks Y Corrected Pos " + sensorName, nBins, -maxU, maxU));
            hitEfficiencyYCorrectedPos.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency Y Corrected Pos " + sensorName, nBins, -maxU, maxU));
            numberOfTracksPCorrectedPos.put(sensorName, histogramFactory.createHistogram1D("Number of Tracks P Corrected Pos " + sensorName, nBins, 0, 1.3 * ebeam));
            hitEfficiencyPCorrectedPos.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency P Corrected Pos " + sensorName, nBins, 0, 1.3 * ebeam));

            TotalEff.put(sensorName, histogramFactory.createHistogram1D("Total Eff " + sensorName, 1, 0, 1));
            TotalEffEle.put(sensorName, histogramFactory.createHistogram1D("Total Eff Ele " + sensorName, 1, 0, 1));
            TotalEffPos.put(sensorName, histogramFactory.createHistogram1D("Total Eff Pos " + sensorName, 1, 0, 1));

            TotalCorrectedEff.put(sensorName, histogramFactory.createHistogram1D("Total Corrected Eff " + sensorName, 1, 0, 1));
            TotalCorrectedEffEle.put(sensorName, histogramFactory.createHistogram1D("Total Corrected Eff Ele " + sensorName, 1, 0, 1));
            TotalCorrectedEffPos.put(sensorName, histogramFactory.createHistogram1D("Total Corrected Eff Pos " + sensorName, 1, 0, 1));

            hitEfficiencyChannelerr.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency Channel Error " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
            hitEfficiencyYerr.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency Y Error " + sensorName, nBins, -maxU, maxU));
            hitEfficiencyPerr.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency P Error " + sensorName, nBins, 0, 1.3 * ebeam));

            hitEfficiencyChannelCorrectederr.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency Channel Corrected Error " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
            hitEfficiencyYCorrectederr.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency Y Corrected Error " + sensorName, nBins, -maxU, maxU));
            hitEfficiencyPCorrectederr.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency P Corrected Error " + sensorName, nBins, 0, 1.3 * ebeam));

            hitEfficiencyChannelEleerr.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency Channel Ele Error " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
            hitEfficiencyYEleerr.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency Y Ele Error " + sensorName, nBins, -maxU, maxU));
            hitEfficiencyPEleerr.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency P Ele Error " + sensorName, nBins, 0, 1.3 * ebeam));

            hitEfficiencyChannelCorrectedEleerr.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency Channel Corrected Ele Error " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
            hitEfficiencyYCorrectedEleerr.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency Y Corrected Ele Error " + sensorName, nBins, -maxU, maxU));
            hitEfficiencyPCorrectedEleerr.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency P Corrected Ele Error " + sensorName, nBins, 0, 1.3 * ebeam));

            hitEfficiencyChannelPoserr.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency Channel Pos Error " + sensorName, nChan, -chanExtd, nChan + chanExtd));
            hitEfficiencyYPoserr.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency Y Pos Error " + sensorName, nBins, -maxU, maxU));
            hitEfficiencyPPoserr.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency P Pos Error " + sensorName, nBins, 0, 1.3 * ebeam));

            hitEfficiencyChannelCorrectedPoserr.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency Channel Corrected Pos Error " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
            hitEfficiencyYCorrectedPoserr.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency Y Corrected Pos Error " + sensorName, nBins, -maxU, maxU));
            hitEfficiencyPCorrectedPoserr.put(sensorName, histogramFactory.createHistogram1D("HitEfficiency P Corrected Pos Error " + sensorName, nBins, 0, 1.3 * ebeam));

            TotalEfferr.put(sensorName, histogramFactory.createHistogram1D("Total Eff Error " + sensorName, 1, 0, 1));
            TotalEffEleerr.put(sensorName, histogramFactory.createHistogram1D("Total Eff Ele Error " + sensorName, 1, 0, 1));
            TotalEffPoserr.put(sensorName, histogramFactory.createHistogram1D("Total Eff Pos Error " + sensorName, 1, 0, 1));

            TotalCorrectedEfferr.put(sensorName, histogramFactory.createHistogram1D("Total Corrected Eff Error " + sensorName, 1, 0, 1));
            TotalCorrectedEffEleerr.put(sensorName, histogramFactory.createHistogram1D("Total Corrected Eff Ele Error " + sensorName, 1, 0, 1));
            TotalCorrectedEffPoserr.put(sensorName, histogramFactory.createHistogram1D("Total Corrected Eff Pos Error " + sensorName, 1, 0, 1));

            residualY.put(sensorName, histogramFactory.createHistogram1D("Residual U " + sensorName, nBins, minRes, maxRes));
            errorY.put(sensorName, histogramFactory.createHistogram1D("Error U " + sensorName, nBins, 0, maxYerror));
            pullY.put(sensorName, histogramFactory.createHistogram1D("U Pulls " + sensorName, nBins, minPull, maxPull));

            residualYvsV.put(sensorName, histogramFactory.createHistogram2D("Residual U vs V " + sensorName, 2 * nBins, minV, maxV, nBins, minRes, maxRes));
            errorYvsV.put(sensorName, histogramFactory.createHistogram2D("Error U vs V " + sensorName, 2 * nBins, minV, maxV, nBins, 0, maxYerror));
            pullYvsV.put(sensorName, histogramFactory.createHistogram2D("U Pulls vs V " + sensorName, 2 * nBins, minV, maxV, nBins, minPull, maxPull));

            residualYvsU.put(sensorName, histogramFactory.createHistogram2D("Residual U vs U " + sensorName, 2 * nBins, -maxU, maxU, nBins, minRes, maxRes));
            errorYvsU.put(sensorName, histogramFactory.createHistogram2D("Error U vs U " + sensorName, 2 * nBins, -maxU, maxU, nBins, 0, maxYerror));
            pullYvsU.put(sensorName, histogramFactory.createHistogram2D("U Pulls vs U " + sensorName, 2 * nBins, -maxU, maxU, nBins, minPull, maxPull));

            residualYEle.put(sensorName, histogramFactory.createHistogram1D("Residual U Electron " + sensorName, nBins, minRes, maxRes));
            errorYEle.put(sensorName, histogramFactory.createHistogram1D("Error U Electron " + sensorName, nBins, 0, maxYerror));
            pullYEle.put(sensorName, histogramFactory.createHistogram1D("U Pulls Electron " + sensorName, nBins, minPull, maxPull));

            residualYvsVEle.put(sensorName, histogramFactory.createHistogram2D("Residual U vs V Electron " + sensorName, 2 * nBins, minV, maxV, nBins, minRes, maxRes));
            errorYvsVEle.put(sensorName, histogramFactory.createHistogram2D("Error U vs V Electron " + sensorName, 2 * nBins, minV, maxV, nBins, 0, maxYerror));
            pullYvsVEle.put(sensorName, histogramFactory.createHistogram2D("U Pulls vs V Electron " + sensorName, 2 * nBins, minV, maxV, nBins, minPull, maxPull));

            residualYvsUEle.put(sensorName, histogramFactory.createHistogram2D("Residual U vs U Electron " + sensorName, 2 * nBins, -maxU, maxU, nBins, minRes, maxRes));
            errorYvsUEle.put(sensorName, histogramFactory.createHistogram2D("Error U vs U Electron " + sensorName, 2 * nBins, -maxU, maxU, nBins, 0, maxYerror));
            pullYvsUEle.put(sensorName, histogramFactory.createHistogram2D("U Pulls vs U Electron " + sensorName, 2 * nBins, -maxU, maxU, nBins, minPull, maxPull));

            residualYPos.put(sensorName, histogramFactory.createHistogram1D("Residual U Positron " + sensorName, nBins, minRes, maxRes));
            errorYPos.put(sensorName, histogramFactory.createHistogram1D("Error U Positron " + sensorName, nBins, 0, maxYerror));
            pullYPos.put(sensorName, histogramFactory.createHistogram1D("U Pulls Positron " + sensorName, nBins, minPull, maxPull));

            residualYvsVPos.put(sensorName, histogramFactory.createHistogram2D("Residual U vs V Positron " + sensorName, 2 * nBins, minV, maxV, nBins, minRes, maxRes));
            errorYvsVPos.put(sensorName, histogramFactory.createHistogram2D("Error U vs V Positron " + sensorName, 2 * nBins, minV, maxV, nBins, 0, maxYerror));
            pullYvsVPos.put(sensorName, histogramFactory.createHistogram2D("U Pulls vs V Positron " + sensorName, 2 * nBins, minV, maxV, nBins, minPull, maxPull));

            residualYvsUPos.put(sensorName, histogramFactory.createHistogram2D("Residual U vs U Positron " + sensorName, 2 * nBins, -maxU, maxU, nBins, minRes, maxRes));
            errorYvsUPos.put(sensorName, histogramFactory.createHistogram2D("Error U vs U Positron " + sensorName, 2 * nBins, -maxU, maxU, nBins, 0, maxYerror));
            pullYvsUPos.put(sensorName, histogramFactory.createHistogram2D("U Pulls vs U Positron " + sensorName, 2 * nBins, -maxU, maxU, nBins, minPull, maxPull));

            residualYvsP.put(sensorName, histogramFactory.createHistogram2D("Residual U vs P " + sensorName, nBins, 0, 1.3 * ebeam, nBins, minRes, maxRes));
            errorYvsP.put(sensorName, histogramFactory.createHistogram2D("Error U vs P " + sensorName, nBins, 0, 1.3 * ebeam, nBins, 0, maxYerror));
            pullYvsP.put(sensorName, histogramFactory.createHistogram2D("U Pulls vs P " + sensorName, nBins, 0, 1.3 * ebeam, nBins, minPull, maxPull));

            residualYvsPEle.put(sensorName, histogramFactory.createHistogram2D("Residual U vs P Electron " + sensorName, nBins, 0, 1.3 * ebeam, nBins, minRes, maxRes));
            errorYvsPEle.put(sensorName, histogramFactory.createHistogram2D("Error U vs P Electron " + sensorName, nBins, 0, 1.3 * ebeam, nBins, 0, maxYerror));
            pullYvsPEle.put(sensorName, histogramFactory.createHistogram2D("U Pulls vs P Electron " + sensorName, nBins, 0, 1.3 * ebeam, nBins, minPull, maxPull));

            residualYvsPPos.put(sensorName, histogramFactory.createHistogram2D("Residual U vs P Positron " + sensorName, nBins, 0, 1.3 * ebeam, nBins, minRes, maxRes));
            errorYvsPPos.put(sensorName, histogramFactory.createHistogram2D("Error U vs P Positron " + sensorName, nBins, 0, 1.3 * ebeam, nBins, 0, maxYerror));
            pullYvsPPos.put(sensorName, histogramFactory.createHistogram2D("U Pulls vs P Positron " + sensorName, nBins, 0, 1.3 * ebeam, nBins, minPull, maxPull));

            D0.put(sensorName, histogramFactory.createHistogram1D("D0 " + sensorName, nBins, minD0, maxD0));
            Z0.put(sensorName, histogramFactory.createHistogram1D("Z0 " + sensorName, nBins, minZ0, maxZ0));
            Tanlambda.put(sensorName, histogramFactory.createHistogram1D("TanLambda " + sensorName, nBins, minTLambda, maxTLambda));
            Phi0.put(sensorName, histogramFactory.createHistogram1D("Phi0 " + sensorName, nBins, minPhi0, maxPhi0));
            Omega.put(sensorName, histogramFactory.createHistogram1D("Omega " + sensorName, nBins, minOmega, maxOmega));

            D0_err.put(sensorName, histogramFactory.createHistogram1D("D0 Error " + sensorName, nBins, minD0Err, maxD0Err));
            Z0_err.put(sensorName, histogramFactory.createHistogram1D("Z0 Error " + sensorName, nBins, minZ0Err, maxZ0Err));
            Tanlambda_err.put(sensorName, histogramFactory.createHistogram1D("TanLambda Error " + sensorName, nBins, minTLambdaErr, maxTLambdaErr));
            Phi0_err.put(sensorName, histogramFactory.createHistogram1D("Phi0 Error " + sensorName, nBins, minPhi0Err, maxPhi0Err));
            Omega_err.put(sensorName, histogramFactory.createHistogram1D("Omega Error " + sensorName, nBins, minOmegaErr, maxOmegaErr));
        }
    }

    public void process(EventHeader event) {
        aida.tree().cd("/");

        //Grab all GBL tracks in the event
        List<Track> tracks = event.get(Track.class, GBLTrackCollectionName);

        //Grab all the clusters in the event
        List<SiTrackerHitStrip1D> stripHits = event.get(SiTrackerHitStrip1D.class, stripHitOutputCollectionName);

        for (Track track : tracks) {
            //Grab the unused layer on the track
            int unusedLay = getUnusedSvtLayer(track.getTrackerHits());
            if (unusedLay == -1)
                continue;

            //Get all track states for this track
            List<TrackState> TStates = track.getTrackStates();

            TrackState tState = getTrackState(track, unusedLay);
            if (tState == null)
                continue;

            //Grab covariance matrix at track states
            double[] covAtIP = TStates.get(0).getCovMatrix();
            SymmetricMatrix LocCovAtIP = new SymmetricMatrix(5, covAtIP, true);

            //Fill track states and errors at IP
            D0.get(atIP).fill(TStates.get(0).getD0());            
            Z0.get(atIP).fill(TStates.get(0).getZ0());
            Tanlambda.get(atIP).fill(TStates.get(0).getTanLambda());
            Phi0.get(atIP).fill(TStates.get(0).getPhi());
            Omega.get(atIP).fill(TStates.get(0).getOmega());

            D0_err.get(atIP).fill(Math.sqrt(LocCovAtIP.e(HelicalTrackFit.dcaIndex, HelicalTrackFit.dcaIndex)));
            Z0_err.get(atIP).fill(Math.sqrt(LocCovAtIP.e(HelicalTrackFit.z0Index, HelicalTrackFit.z0Index)));
            Tanlambda_err.get(atIP).fill(Math.sqrt(LocCovAtIP.e(HelicalTrackFit.slopeIndex, HelicalTrackFit.slopeIndex)));
            Phi0_err.get(atIP).fill(Math.sqrt(LocCovAtIP.e(HelicalTrackFit.phi0Index, HelicalTrackFit.phi0Index)));
            Omega_err.get(atIP).fill(Math.sqrt(LocCovAtIP.e(HelicalTrackFit.curvatureIndex, HelicalTrackFit.curvatureIndex)));

            Hep3Vector p = toHep3(tState.getMomentum());
            double q = -track.getCharge();  //HelicalTrackFit flips sign of charge       

            if (cleanFEE) {
                // Require track to be an electron
                if (q < 0)
                    continue;

                // Select around the FEE momentum peak
                if (p.magnitude() < 0.75 * ebeam || p.magnitude() > 1.25 * ebeam)
                    continue;
            }

            //See if track is within acceptance of both the axial and stereo sensors of the unused layer
            Pair<HpsSiSensor, Pair<Integer, Hep3Vector>> axialSensorPair = isWithinSensorAcceptance(track, tState, unusedLay, true, p, bFieldMap);
            Pair<HpsSiSensor, Pair<Integer, Hep3Vector>> stereoSensorPair = isWithinSensorAcceptance(track, tState, unusedLay, false, p, bFieldMap);

            //Skip track if it isn't within acceptance of both axial and stereo pairs of a given unused layer
            if (axialSensorPair == null || stereoSensorPair == null)
                continue;

            //Set axial and stereo sensors of the missing layer
            HpsSiSensor axialSensor = axialSensorPair.getFirst();
            HpsSiSensor stereoSensor = stereoSensorPair.getFirst();

            String sensorAxialName = axialSensor.getName();
            String sensorStereoName = stereoSensor.getName();

            //Grab the track extrapolations at each sensor
            Hep3Vector axialExtrapPosSensor = axialSensorPair.getSecond().getSecond();
            Hep3Vector stereoExtrapPosSensor = stereoSensorPair.getSecond().getSecond();

            //Compute the extrapolation errors in u direction
            //TODO this needs to be done correctly
            double yErrorAxial = computeExtrapErrorY(track, tState, axialSensor, unusedLay)[0];
            double yErrorStereo = computeExtrapErrorY(track, tState, stereoSensor, unusedLay)[0];

            //Compute the channel where the track extrapolates to in each sensor
            int chanAxial = axialSensorPair.getSecond().getFirst();
            int chanStereo = stereoSensorPair.getSecond().getFirst();

            double trackP = toHep3(track.getTrackStates().get(0).getMomentum()).magnitude();

            double weightAxial = findWeight(axialExtrapPosSensor.x(), yErrorAxial, axialSensor);
            double weightStereo = findWeight(stereoExtrapPosSensor.x(), yErrorStereo, stereoSensor);

            //Fill the denominator of the efficiency histos
            numberOfTracksChannel.get(sensorAxialName).fill(chanAxial);
            numberOfTracksChannel.get(sensorStereoName).fill(chanStereo);
            numberOfTracksY.get(sensorAxialName).fill(axialExtrapPosSensor.x());
            numberOfTracksY.get(sensorStereoName).fill(stereoExtrapPosSensor.x());
            numberOfTracksP.get(sensorAxialName).fill(trackP);
            numberOfTracksP.get(sensorStereoName).fill(trackP);

            numberOfTracksChannelCorrected.get(sensorAxialName).fill(chanAxial, weightAxial);
            numberOfTracksChannelCorrected.get(sensorStereoName).fill(chanStereo, weightStereo);
            numberOfTracksYCorrected.get(sensorAxialName).fill(axialExtrapPosSensor.x(), weightAxial);
            numberOfTracksYCorrected.get(sensorStereoName).fill(stereoExtrapPosSensor.x(), weightStereo);
            numberOfTracksPCorrected.get(sensorAxialName).fill(trackP, weightAxial);
            numberOfTracksPCorrected.get(sensorStereoName).fill(trackP, weightStereo);

            //Fill electron histograms
            if (q < 0) {
                numberOfTracksChannelEle.get(sensorAxialName).fill(chanAxial);
                numberOfTracksChannelEle.get(sensorStereoName).fill(chanStereo);
                numberOfTracksYEle.get(sensorAxialName).fill(axialExtrapPosSensor.x());
                numberOfTracksYEle.get(sensorStereoName).fill(stereoExtrapPosSensor.x());
                numberOfTracksPEle.get(sensorAxialName).fill(trackP);
                numberOfTracksPEle.get(sensorStereoName).fill(trackP);

                numberOfTracksChannelCorrectedEle.get(sensorAxialName).fill(chanAxial, weightAxial);
                numberOfTracksChannelCorrectedEle.get(sensorStereoName).fill(chanStereo, weightStereo);
                numberOfTracksYCorrectedEle.get(sensorAxialName).fill(axialExtrapPosSensor.x(), weightAxial);
                numberOfTracksYCorrectedEle.get(sensorStereoName).fill(stereoExtrapPosSensor.x(), weightStereo);
                numberOfTracksPCorrectedEle.get(sensorAxialName).fill(trackP, weightAxial);
                numberOfTracksPCorrectedEle.get(sensorStereoName).fill(trackP, weightStereo);
            } //Fill positron histograms
            else {
                numberOfTracksChannelPos.get(sensorAxialName).fill(chanAxial);
                numberOfTracksChannelPos.get(sensorStereoName).fill(chanStereo);
                numberOfTracksYPos.get(sensorAxialName).fill(axialExtrapPosSensor.x());
                numberOfTracksYPos.get(sensorStereoName).fill(stereoExtrapPosSensor.x());
                numberOfTracksPPos.get(sensorAxialName).fill(trackP);
                numberOfTracksPPos.get(sensorStereoName).fill(trackP);

                numberOfTracksChannelCorrectedPos.get(sensorAxialName).fill(chanAxial, weightAxial);
                numberOfTracksChannelCorrectedPos.get(sensorStereoName).fill(chanStereo, weightStereo);
                numberOfTracksYCorrectedPos.get(sensorAxialName).fill(axialExtrapPosSensor.x(), weightAxial);
                numberOfTracksYCorrectedPos.get(sensorStereoName).fill(stereoExtrapPosSensor.x(), weightStereo);
                numberOfTracksPCorrectedPos.get(sensorAxialName).fill(trackP, weightAxial);
                numberOfTracksPCorrectedPos.get(sensorStereoName).fill(trackP, weightStereo);
            }

            //Fill the error histos
            errorY.get(sensorAxialName).fill(yErrorAxial);
            errorY.get(sensorStereoName).fill(yErrorStereo);
            errorYvsV.get(sensorAxialName).fill(axialExtrapPosSensor.y(), yErrorAxial);
            errorYvsV.get(sensorStereoName).fill(stereoExtrapPosSensor.y(), yErrorStereo);
            errorYvsU.get(sensorAxialName).fill(axialExtrapPosSensor.x(), yErrorAxial);
            errorYvsU.get(sensorStereoName).fill(stereoExtrapPosSensor.x(), yErrorStereo);
            errorYvsP.get(sensorAxialName).fill(p.magnitude(), yErrorAxial);
            errorYvsP.get(sensorStereoName).fill(p.magnitude(), yErrorStereo);

            if (q < 0) {
                errorYEle.get(sensorAxialName).fill(yErrorAxial);
                errorYEle.get(sensorStereoName).fill(yErrorStereo);
                errorYvsVEle.get(sensorAxialName).fill(axialExtrapPosSensor.y(), yErrorAxial);
                errorYvsVEle.get(sensorStereoName).fill(stereoExtrapPosSensor.y(), yErrorStereo);
                errorYvsUEle.get(sensorAxialName).fill(axialExtrapPosSensor.x(), yErrorAxial);
                errorYvsUEle.get(sensorStereoName).fill(stereoExtrapPosSensor.x(), yErrorStereo);
                errorYvsPEle.get(sensorAxialName).fill(p.magnitude(), yErrorAxial);
                errorYvsPEle.get(sensorStereoName).fill(p.magnitude(), yErrorStereo);
            } else {
                errorYPos.get(sensorAxialName).fill(yErrorAxial);
                errorYPos.get(sensorStereoName).fill(yErrorStereo);
                errorYvsVPos.get(sensorAxialName).fill(axialExtrapPosSensor.y(), yErrorAxial);
                errorYvsVPos.get(sensorStereoName).fill(stereoExtrapPosSensor.y(), yErrorStereo);
                errorYvsUPos.get(sensorAxialName).fill(axialExtrapPosSensor.x(), yErrorAxial);
                errorYvsUPos.get(sensorStereoName).fill(stereoExtrapPosSensor.x(), yErrorStereo);
                errorYvsPPos.get(sensorAxialName).fill(p.magnitude(), yErrorAxial);
                errorYvsPPos.get(sensorStereoName).fill(p.magnitude(), yErrorStereo);
            }
            double residualAxial = 9999;
            double residualStereo = 9999;

            //Loop over all reconstructed 1D hits on sensor of interest in the events
            for (SiTrackerHitStrip1D hit : stripHits) {
                //Get the sensor and position of the hit
                HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement();
                double[] hitPos = hit.getPosition();
                //Change to sensor coordinates
                Hep3Vector hitPosSensor = globalToSensor(toHep3(hitPos), sensor);
                //Check to see if the sensor of this hit is the same sensor you expect to see an axial hit
                if (sensorAxialName == sensor.getName()) {
                    //Compute the residual between extrapolated track and hit position
                    //Keep the value of the smallest residual
                    double residual = axialExtrapPosSensor.x() - hitPosSensor.x();
                    if (Math.abs(residual) < Math.abs(residualAxial))
                        residualAxial = residual;
                }
                //Check to see if the sensor of this hit is the same sensor you expect to see a stereo hit
                if (sensorStereoName == sensor.getName()) {
                    //Compute the residual between extrapolated track and hit position
                    //Keep the value of the smallest residual
                    double residual = stereoExtrapPosSensor.x() - hitPosSensor.x();
                    if (Math.abs(residual) < Math.abs(residualStereo))
                        residualStereo = residual;
                }
            }

            //Fill histograms for residuals and pulls
            residualY.get(sensorAxialName).fill(residualAxial);
            pullY.get(sensorAxialName).fill(residualAxial / yErrorAxial);
            residualY.get(sensorStereoName).fill(residualStereo);
            pullY.get(sensorStereoName).fill(residualStereo / yErrorStereo);

            residualYvsV.get(sensorAxialName).fill(axialExtrapPosSensor.y(), residualAxial);
            pullYvsV.get(sensorAxialName).fill(axialExtrapPosSensor.y(), residualAxial / yErrorAxial);
            residualYvsV.get(sensorStereoName).fill(stereoExtrapPosSensor.y(), residualStereo);
            pullYvsV.get(sensorStereoName).fill(stereoExtrapPosSensor.y(), residualStereo / yErrorStereo);

            residualYvsU.get(sensorAxialName).fill(axialExtrapPosSensor.x(), residualAxial);
            pullYvsU.get(sensorAxialName).fill(axialExtrapPosSensor.x(), residualAxial / yErrorAxial);
            residualYvsU.get(sensorStereoName).fill(stereoExtrapPosSensor.x(), residualStereo);
            pullYvsU.get(sensorStereoName).fill(stereoExtrapPosSensor.x(), residualStereo / yErrorStereo);

            residualYvsP.get(sensorAxialName).fill(p.magnitude(), residualAxial);
            residualYvsP.get(sensorStereoName).fill(p.magnitude(), residualAxial);
            pullYvsP.get(sensorAxialName).fill(p.magnitude(), residualAxial / yErrorAxial);
            pullYvsP.get(sensorStereoName).fill(p.magnitude(), residualStereo / yErrorStereo);

            if (q < 0) {
                residualYEle.get(sensorAxialName).fill(residualAxial);
                pullYEle.get(sensorAxialName).fill(residualAxial / yErrorAxial);
                residualYEle.get(sensorStereoName).fill(residualStereo);
                pullYEle.get(sensorStereoName).fill(residualStereo / yErrorStereo);

                residualYvsVEle.get(sensorAxialName).fill(axialExtrapPosSensor.y(), residualAxial);
                pullYvsVEle.get(sensorAxialName).fill(axialExtrapPosSensor.y(), residualAxial / yErrorAxial);
                residualYvsVEle.get(sensorStereoName).fill(stereoExtrapPosSensor.y(), residualStereo);
                pullYvsVEle.get(sensorStereoName).fill(stereoExtrapPosSensor.y(), residualStereo / yErrorStereo);

                residualYvsUEle.get(sensorAxialName).fill(axialExtrapPosSensor.x(), residualAxial);
                pullYvsUEle.get(sensorAxialName).fill(axialExtrapPosSensor.x(), residualAxial / yErrorAxial);
                residualYvsUEle.get(sensorStereoName).fill(stereoExtrapPosSensor.x(), residualStereo);
                pullYvsUEle.get(sensorStereoName).fill(stereoExtrapPosSensor.x(), residualStereo / yErrorStereo);

                residualYvsPEle.get(sensorAxialName).fill(p.magnitude(), residualAxial);
                residualYvsPEle.get(sensorStereoName).fill(p.magnitude(), residualAxial);
                pullYvsPEle.get(sensorAxialName).fill(p.magnitude(), residualAxial / yErrorAxial);
                pullYvsPEle.get(sensorStereoName).fill(p.magnitude(), residualStereo / yErrorStereo);
            } else {
                residualYPos.get(sensorAxialName).fill(residualAxial);
                pullYPos.get(sensorAxialName).fill(residualAxial / yErrorAxial);
                residualYPos.get(sensorStereoName).fill(residualStereo);
                pullYPos.get(sensorStereoName).fill(residualStereo / yErrorStereo);

                residualYvsVPos.get(sensorAxialName).fill(axialExtrapPosSensor.y(), residualAxial);
                pullYvsVPos.get(sensorAxialName).fill(axialExtrapPosSensor.y(), residualAxial / yErrorAxial);
                residualYvsVPos.get(sensorStereoName).fill(stereoExtrapPosSensor.y(), residualStereo);
                pullYvsVPos.get(sensorStereoName).fill(stereoExtrapPosSensor.y(), residualStereo / yErrorStereo);

                residualYvsUPos.get(sensorAxialName).fill(axialExtrapPosSensor.x(), residualAxial);
                pullYvsUPos.get(sensorAxialName).fill(axialExtrapPosSensor.x(), residualAxial / yErrorAxial);
                residualYvsUPos.get(sensorStereoName).fill(stereoExtrapPosSensor.x(), residualStereo);
                pullYvsUPos.get(sensorStereoName).fill(stereoExtrapPosSensor.x(), residualStereo / yErrorStereo);

                residualYvsPPos.get(sensorAxialName).fill(p.magnitude(), residualAxial);
                residualYvsPPos.get(sensorStereoName).fill(p.magnitude(), residualAxial);
                pullYvsPPos.get(sensorAxialName).fill(p.magnitude(), residualAxial / yErrorAxial);
                pullYvsPPos.get(sensorStereoName).fill(p.magnitude(), residualStereo / yErrorStereo);
            }
            //Check to see if residual is within nSig (5 default) of the u error
            //If so, fill the numerator efficiency histograms
            if ((Math.abs(residualAxial) < this.nSig * yErrorAxial)) {
                numberOfTracksWithHitOnMissingLayerChannel.get(sensorAxialName).fill(chanAxial);
                numberOfTracksWithHitOnMissingLayerY.get(sensorAxialName).fill(axialExtrapPosSensor.x());
                numberOfTracksWithHitOnMissingLayerP.get(sensorAxialName).fill(trackP);
                if (q < 0) {
                    numberOfTracksWithHitOnMissingLayerChannelEle.get(sensorAxialName).fill(chanAxial);
                    numberOfTracksWithHitOnMissingLayerYEle.get(sensorAxialName).fill(axialExtrapPosSensor.x());
                    numberOfTracksWithHitOnMissingLayerPEle.get(sensorAxialName).fill(trackP);
                } else {
                    numberOfTracksWithHitOnMissingLayerChannelPos.get(sensorAxialName).fill(chanAxial);
                    numberOfTracksWithHitOnMissingLayerYPos.get(sensorAxialName).fill(axialExtrapPosSensor.x());
                    numberOfTracksWithHitOnMissingLayerPPos.get(sensorAxialName).fill(trackP);
                }
            }

            if ((Math.abs(residualStereo) < this.nSig * yErrorStereo)) {
                numberOfTracksWithHitOnMissingLayerChannel.get(sensorStereoName).fill(chanStereo);
                numberOfTracksWithHitOnMissingLayerY.get(sensorStereoName).fill(stereoExtrapPosSensor.x());
                numberOfTracksWithHitOnMissingLayerP.get(sensorStereoName).fill(trackP);
                if (q < 0) {
                    numberOfTracksWithHitOnMissingLayerChannelEle.get(sensorStereoName).fill(chanStereo);
                    numberOfTracksWithHitOnMissingLayerYEle.get(sensorStereoName).fill(stereoExtrapPosSensor.x());
                    numberOfTracksWithHitOnMissingLayerPEle.get(sensorStereoName).fill(trackP);
                } else {
                    numberOfTracksWithHitOnMissingLayerChannelPos.get(sensorStereoName).fill(chanStereo);
                    numberOfTracksWithHitOnMissingLayerYPos.get(sensorStereoName).fill(stereoExtrapPosSensor.x());
                    numberOfTracksWithHitOnMissingLayerPPos.get(sensorStereoName).fill(trackP);
                }
            }
        }
    }

    //Computes weight based on the number of sigmas (u error) the track extrapolates from the edge of the sensor
    private double findWeight(double y, double yErr, HpsSiSensor sensor) {
        double readoutPitch = sensor.getReadoutStripPitch();
        int nChan = sensor.getNumberOfChannels();
        boolean firstChan = firstChanIsEdge(sensor);
        double height = readoutPitch * nChan;
        double distanceToEdge = 0;
        if (firstChan)
            distanceToEdge = height / 2 - y;
        else
            distanceToEdge = height / 2 + y;
        double nSig = distanceToEdge / yErr;
        return computeGaussInt(nSig, 1000);
    }

    //Computes gaussian integral numerically from -inf to nSig
    private double computeGaussInt(double nSig, int nSteps) {
        double mean = 0;
        double sigma = 1;
        double dx = sigma * nSig / (double) nSteps;
        double integral = 0;
        for (int i = 0; i < nSteps; i++) {
            double x = dx * (i + 0.5) + mean;
            integral += dx * Gauss(x, mean, sigma);
        }
        return integral + 0.5;
    }

    //Gaussian function
    private double Gauss(double x, double mean, double sigma) {
        return 1 / (Math.sqrt(2 * Math.PI * Math.pow(sigma, 2))) * Math.exp(-Math.pow(x - mean, 2) / (2 * Math.pow(sigma, 2)));
    }

    //Some sensors have channel 0 closest to the beam
    //Others have channel 640 closest to the beam
    //Use this function to find out which one your sensor is!
    private boolean firstChanIsEdge(HpsSiSensor sensor) {
        int layer = (sensor.getLayerNumber() + 1) / 2;
        if (layer > 0 && layer < 4)
            if (sensor.isAxial())
                return false;
            else
                return true;
        else
            if (sensor.isAxial())
                if (sensor.getSide().matches("ELECTRON"))
                    return false;
                else
                    return true;
            else
                if (!sensor.getSide().matches("ELECTRON"))
                    return false;
                else
                    return true;
    }

    //Converts position into sensor frame
    private Hep3Vector globalToSensor(Hep3Vector trkpos, HpsSiSensor sensor) {
        SiSensorElectrodes electrodes = sensor.getReadoutElectrodes(ChargeCarrier.HOLE);
        if (electrodes == null) {
            electrodes = sensor.getReadoutElectrodes(ChargeCarrier.ELECTRON);
            System.out.println("Charge Carrier is NULL");
        }
        return electrodes.getGlobalToLocal().transformed(trkpos);
    }

    //Get the track state at the previous sensor
    private TrackState getTrackState(Track track, int unusedLay) {
        int layer = -1;
        boolean isTop = track.getTrackStates().get(0).getTanLambda() > 0;
        //If unused layer is L1, then get trackstate at IP
        if (unusedLay == 1)
            return track.getTrackStates().get(0);
        else
            layer = unusedLay - 1;
        HpsSiSensor sensorHole = getSensor(track, layer, isTop, true);
        HpsSiSensor sensorSlot = getSensor(track, layer, isTop, false);
        TrackState tState = TrackStateUtils.getTrackStateAtSensor(track, sensorHole.getMillepedeId());
        if (tState == null)
            tState = TrackStateUtils.getTrackStateAtSensor(track, sensorSlot.getMillepedeId());
        return tState;
    }

    //Returns channel number of a given position in the sensor frame
    private int getChan(Hep3Vector pos, HpsSiSensor sensor) {
        double readoutPitch = sensor.getReadoutStripPitch();
        int nChan = sensor.getNumberOfChannels();
        double height = readoutPitch * nChan;
        return (int) ((height / 2 - pos.x()) / readoutPitch);
    }

    //Converts double array into Hep3Vector
    private Hep3Vector toHep3(double[] arr) {
        return new BasicHep3Vector(arr[0], arr[1], arr[2]);
    }

    //Return the HpsSiSensor for a given top/bottom track, layer, axial/stereo, and slot/hole
    private HpsSiSensor getSensor(Track track, int layer, boolean isAxial, boolean isHole) {
        double tanLambda = track.getTrackStates().get(0).getTanLambda();
        int outerLayer = 4;
        if (sensors.size() > 36)
            outerLayer = 5;
        for (HpsSiSensor sensor : sensors) {
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
            System.out.println(sensor.getName());
            HpsSiSensor prevSensor = getSensor(track, unusedLay - 1, isTop, isHole);
            System.out.println(prevSensor);
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
        D0_err.get(sensorName).fill(d0_err);
        Z0_err.get(sensorName).fill(z0_err);
        Tanlambda_err.get(sensorName).fill(tanlambda_err);
        Phi0_err.get(sensorName).fill(phi0_err);
        Omega_err.get(sensorName).fill(omega_err);

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

    private int getUnusedSvtLayer(List<TrackerHit> stereoHits) {
        int[] svtLayer = new int[6];

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
        for (int layer = 0; layer < svtLayer.length; layer++)
            if (svtLayer[layer] == 0)
                return (layer + 1);
        return -1;
    }

    //Checks to see if track is within acceptance of both axial and stereo sensors at a given layer
    //Also returns channel number of the intersection
    private Pair<HpsSiSensor, Pair<Integer, Hep3Vector>> isWithinSensorAcceptance(Track track, TrackState tState, int layer, boolean axial, Hep3Vector p, FieldMap fieldMap) {

        HpsSiSensor axialSensorHole = getSensor(track, layer, true, true);
        HpsSiSensor axialSensorSlot = getSensor(track, layer, true, false);
        HpsSiSensor stereoSensorHole = getSensor(track, layer, false, true);
        HpsSiSensor stereoSensorSlot = getSensor(track, layer, false, false);

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

    public void endOfData() {
        System.out.println("End of Data. Computing Hit Efficiencies");

        //Setup text files to output efficiencies for each channel
        PrintWriter out = null;
        try {
            out = new PrintWriter(outputFileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        //Compute efficiencies and errors as a function of channel, u, and momentum
        for (HpsSiSensor sensor : sensors) {
            String sensorName = sensor.getName();
            int nChan = sensor.getNumberOfChannels();
            for (int i = 0; i < nChan + chanExtd * 2; i++) {
                int chan = i - chanExtd;
                if (numberOfTracksChannel.get(sensorName).binHeight(i) != 0 && numberOfTracksChannel.get(sensorName).binHeight(i) != 0) {
                    hitEfficiencyChannel.get(sensorName).fill(chan, numberOfTracksWithHitOnMissingLayerChannel.get(sensorName).binHeight(i) / (double) numberOfTracksChannel.get(sensorName).binHeight(i));
                    hitEfficiencyChannelerr.get(sensorName).fill(chan, Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerChannel.get(sensorName).binHeight(i) + 1 / (double) numberOfTracksChannel.get(sensorName).binHeight(i)));
                    hitEfficiencyChannelCorrected.get(sensorName).fill(chan, numberOfTracksWithHitOnMissingLayerChannel.get(sensorName).binHeight(i) / (double) numberOfTracksChannelCorrected.get(sensorName).binHeight(i));
                    hitEfficiencyChannelCorrectederr.get(sensorName).fill(chan, Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerChannel.get(sensorName).binHeight(i) + 1 / (double) numberOfTracksChannel.get(sensorName).binHeight(i)));
                }
                if (numberOfTracksChannelEle.get(sensorName).binHeight(i) != 0 && numberOfTracksChannelEle.get(sensorName).binHeight(i) != 0) {
                    hitEfficiencyChannelEle.get(sensorName).fill(chan, numberOfTracksWithHitOnMissingLayerChannelEle.get(sensorName).binHeight(i) / (double) numberOfTracksChannelEle.get(sensorName).binHeight(i));
                    hitEfficiencyChannelEleerr.get(sensorName).fill(chan, Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerChannelEle.get(sensorName).binHeight(i) + 1 / (double) numberOfTracksChannelEle.get(sensorName).binHeight(i)));
                    hitEfficiencyChannelCorrectedEle.get(sensorName).fill(chan, numberOfTracksWithHitOnMissingLayerChannelEle.get(sensorName).binHeight(i) / (double) numberOfTracksChannelCorrectedEle.get(sensorName).binHeight(i));
                    hitEfficiencyChannelCorrectedEleerr.get(sensorName).fill(chan, Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerChannelEle.get(sensorName).binHeight(i) + 1 / (double) numberOfTracksChannelCorrectedEle.get(sensorName).binHeight(i)));
                }
                if (numberOfTracksChannelPos.get(sensorName).binHeight(i) != 0 && numberOfTracksChannelPos.get(sensorName).binHeight(i) != 0) {
                    hitEfficiencyChannelPos.get(sensorName).fill(chan, numberOfTracksWithHitOnMissingLayerChannelPos.get(sensorName).binHeight(i) / (double) numberOfTracksChannelPos.get(sensorName).binHeight(i));
                    hitEfficiencyChannelPoserr.get(sensorName).fill(chan, Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerChannelPos.get(sensorName).binHeight(i) + 1 / (double) numberOfTracksChannelPos.get(sensorName).binHeight(i)));
                    hitEfficiencyChannelCorrectedPos.get(sensorName).fill(chan, numberOfTracksWithHitOnMissingLayerChannelPos.get(sensorName).binHeight(i) / (double) numberOfTracksChannelCorrectedPos.get(sensorName).binHeight(i));
                    hitEfficiencyChannelCorrectedPoserr.get(sensorName).fill(chan, Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerChannelPos.get(sensorName).binHeight(i) + 1 / (double) numberOfTracksChannelCorrectedPos.get(sensorName).binHeight(i)));
                }
            }
            for (int i = 0; i < nBins; i++) {
                double yMax = sensor.getNumberOfChannels() * sensor.getReadoutStripPitch() / 2;
                double yMin = -yMax;
                double y = (yMax - yMin) / (double) nBins * (i + 0.5) + yMin;
                double pMax = 1.3 * ebeam;
                double pMin = 0;
                double p = (pMax - pMin) / (double) nBins * (i + 0.5) + pMin;
                if (numberOfTracksY.get(sensorName).binHeight(i) != 0 && numberOfTracksY.get(sensorName).binHeight(i) != 0) {
                    hitEfficiencyY.get(sensorName).fill(y, numberOfTracksWithHitOnMissingLayerY.get(sensorName).binHeight(i) / (double) numberOfTracksY.get(sensorName).binHeight(i));
                    hitEfficiencyYerr.get(sensorName).fill(y, Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerY.get(sensorName).binHeight(i) + 1 / (double) numberOfTracksY.get(sensorName).binHeight(i)));
                    hitEfficiencyYCorrected.get(sensorName).fill(y, numberOfTracksWithHitOnMissingLayerY.get(sensorName).binHeight(i) / (double) numberOfTracksYCorrected.get(sensorName).binHeight(i));
                    hitEfficiencyYCorrectederr.get(sensorName).fill(y, Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerY.get(sensorName).binHeight(i) + 1 / (double) numberOfTracksYCorrected.get(sensorName).binHeight(i)));
                }
                if (numberOfTracksYEle.get(sensorName).binHeight(i) != 0 && numberOfTracksYEle.get(sensorName).binHeight(i) != 0) {
                    hitEfficiencyYEle.get(sensorName).fill(y, numberOfTracksWithHitOnMissingLayerYEle.get(sensorName).binHeight(i) / (double) numberOfTracksYEle.get(sensorName).binHeight(i));
                    hitEfficiencyYEleerr.get(sensorName).fill(y, Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerYEle.get(sensorName).binHeight(i) + 1 / (double) numberOfTracksYEle.get(sensorName).binHeight(i)));
                    hitEfficiencyYCorrectedEle.get(sensorName).fill(y, numberOfTracksWithHitOnMissingLayerYEle.get(sensorName).binHeight(i) / (double) numberOfTracksYCorrectedEle.get(sensorName).binHeight(i));
                    hitEfficiencyYCorrectedEleerr.get(sensorName).fill(y, Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerYEle.get(sensorName).binHeight(i) + 1 / (double) numberOfTracksYCorrectedEle.get(sensorName).binHeight(i)));
                }
                if (numberOfTracksYPos.get(sensorName).binHeight(i) != 0 && numberOfTracksYPos.get(sensorName).binHeight(i) != 0) {
                    hitEfficiencyYPos.get(sensorName).fill(y, numberOfTracksWithHitOnMissingLayerYPos.get(sensorName).binHeight(i) / (double) numberOfTracksYPos.get(sensorName).binHeight(i));
                    hitEfficiencyYPoserr.get(sensorName).fill(y, Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerYPos.get(sensorName).binHeight(i) + 1 / (double) numberOfTracksYPos.get(sensorName).binHeight(i)));
                    hitEfficiencyYCorrectedPos.get(sensorName).fill(y, numberOfTracksWithHitOnMissingLayerYPos.get(sensorName).binHeight(i) / (double) numberOfTracksYCorrectedPos.get(sensorName).binHeight(i));
                    hitEfficiencyYCorrectedPoserr.get(sensorName).fill(y, Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerYPos.get(sensorName).binHeight(i) + 1 / (double) numberOfTracksYCorrectedPos.get(sensorName).binHeight(i)));
                }
                if (numberOfTracksP.get(sensorName).binHeight(i) != 0 && numberOfTracksP.get(sensorName).binHeight(i) != 0) {
                    hitEfficiencyP.get(sensorName).fill(p, numberOfTracksWithHitOnMissingLayerP.get(sensorName).binHeight(i) / (double) numberOfTracksP.get(sensorName).binHeight(i));
                    hitEfficiencyPerr.get(sensorName).fill(p, Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerP.get(sensorName).binHeight(i) + 1 / (double) numberOfTracksP.get(sensorName).binHeight(i)));
                    hitEfficiencyPCorrected.get(sensorName).fill(p, numberOfTracksWithHitOnMissingLayerP.get(sensorName).binHeight(i) / (double) numberOfTracksPCorrected.get(sensorName).binHeight(i));
                    hitEfficiencyPCorrectederr.get(sensorName).fill(p, Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerP.get(sensorName).binHeight(i) + 1 / (double) numberOfTracksPCorrected.get(sensorName).binHeight(i)));
                }
                if (numberOfTracksPEle.get(sensorName).binHeight(i) != 0 && numberOfTracksPEle.get(sensorName).binHeight(i) != 0) {
                    hitEfficiencyPEle.get(sensorName).fill(p, numberOfTracksWithHitOnMissingLayerPEle.get(sensorName).binHeight(i) / (double) numberOfTracksPEle.get(sensorName).binHeight(i));
                    hitEfficiencyPEleerr.get(sensorName).fill(p, Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerPEle.get(sensorName).binHeight(i) + 1 / (double) numberOfTracksPEle.get(sensorName).binHeight(i)));
                    hitEfficiencyPCorrectedEle.get(sensorName).fill(p, numberOfTracksWithHitOnMissingLayerPEle.get(sensorName).binHeight(i) / (double) numberOfTracksPCorrectedEle.get(sensorName).binHeight(i));
                    hitEfficiencyPCorrectedEleerr.get(sensorName).fill(p, Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerPEle.get(sensorName).binHeight(i) + 1 / (double) numberOfTracksPCorrectedEle.get(sensorName).binHeight(i)));
                }
                if (numberOfTracksPPos.get(sensorName).binHeight(i) != 0 && numberOfTracksPPos.get(sensorName).binHeight(i) != 0) {
                    hitEfficiencyPPos.get(sensorName).fill(p, numberOfTracksWithHitOnMissingLayerPPos.get(sensorName).binHeight(i) / (double) numberOfTracksPPos.get(sensorName).binHeight(i));
                    hitEfficiencyPPoserr.get(sensorName).fill(p, Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerPPos.get(sensorName).binHeight(i) + 1 / (double) numberOfTracksPPos.get(sensorName).binHeight(i)));
                    hitEfficiencyPCorrectedPos.get(sensorName).fill(p, numberOfTracksWithHitOnMissingLayerPPos.get(sensorName).binHeight(i) / (double) numberOfTracksPCorrectedPos.get(sensorName).binHeight(i));
                    hitEfficiencyPCorrectedPoserr.get(sensorName).fill(p, Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerPPos.get(sensorName).binHeight(i) + 1 / (double) numberOfTracksPCorrectedPos.get(sensorName).binHeight(i)));
                }
            }
            double totalEff = 0;
            double totalEffEle = 0;
            double totalEffPos = 0;
            double totalEfferr = 0;
            double totalEffEleerr = 0;
            double totalEffPoserr = 0;

            double totalEffCorrected = 0;
            double totalEffEleCorrected = 0;
            double totalEffPosCorrected = 0;
            double totalEfferrCorrected = 0;
            double totalEffEleerrCorrected = 0;
            double totalEffPoserrCorrected = 0;

            //Calculate total efficiencies for each sensor
            if (numberOfTracksChannel.get(sensorName).sumAllBinHeights() != 0 && numberOfTracksChannel.get(sensorName).sumAllBinHeights() != 0) {
                totalEff = numberOfTracksWithHitOnMissingLayerChannel.get(sensorName).sumAllBinHeights() / (double) numberOfTracksChannel.get(sensorName).sumAllBinHeights();
                totalEfferr = Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerChannel.get(sensorName).sumAllBinHeights() + 1 / (double) numberOfTracksChannel.get(sensorName).sumAllBinHeights());
                totalEffCorrected = numberOfTracksWithHitOnMissingLayerChannel.get(sensorName).sumAllBinHeights() / (double) numberOfTracksChannelCorrected.get(sensorName).sumAllBinHeights();
                totalEfferrCorrected = Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerChannel.get(sensorName).sumAllBinHeights() + 1 / (double) numberOfTracksChannelCorrected.get(sensorName).sumAllBinHeights());
            }
            if (numberOfTracksChannelEle.get(sensorName).sumAllBinHeights() != 0 && numberOfTracksChannelEle.get(sensorName).sumAllBinHeights() != 0) {
                totalEffEle = numberOfTracksWithHitOnMissingLayerChannelEle.get(sensorName).sumAllBinHeights() / (double) numberOfTracksChannelEle.get(sensorName).sumAllBinHeights();
                totalEffEleerr = Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerChannelEle.get(sensorName).sumAllBinHeights() + 1 / (double) numberOfTracksChannelEle.get(sensorName).sumAllBinHeights());
                totalEffEleCorrected = numberOfTracksWithHitOnMissingLayerChannelEle.get(sensorName).sumAllBinHeights() / (double) numberOfTracksChannelCorrectedEle.get(sensorName).sumAllBinHeights();
                totalEffEleerrCorrected = Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerChannelEle.get(sensorName).sumAllBinHeights() + 1 / (double) numberOfTracksChannelCorrectedEle.get(sensorName).sumAllBinHeights());
            }
            if (numberOfTracksChannelPos.get(sensorName).sumAllBinHeights() != 0 && numberOfTracksChannelPos.get(sensorName).sumAllBinHeights() != 0) {
                totalEffPos = numberOfTracksWithHitOnMissingLayerChannelPos.get(sensorName).sumAllBinHeights() / (double) numberOfTracksChannelPos.get(sensorName).sumAllBinHeights();
                totalEffPoserr = Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerChannelPos.get(sensorName).sumAllBinHeights() + 1 / (double) numberOfTracksChannelPos.get(sensorName).sumAllBinHeights());
                totalEffPosCorrected = numberOfTracksWithHitOnMissingLayerChannelPos.get(sensorName).sumAllBinHeights() / (double) numberOfTracksChannelCorrectedPos.get(sensorName).sumAllBinHeights();
                totalEffPoserrCorrected = Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerChannelPos.get(sensorName).sumAllBinHeights() + 1 / (double) numberOfTracksChannelCorrectedPos.get(sensorName).sumAllBinHeights());
            }

            TotalEff.get(sensorName).fill(0, totalEff);
            TotalEffEle.get(sensorName).fill(0, totalEffEle);
            TotalEffPos.get(sensorName).fill(0, totalEffPos);
            TotalEfferr.get(sensorName).fill(0, totalEfferr);
            TotalEffEleerr.get(sensorName).fill(0, totalEffEleerr);
            TotalEffPoserr.get(sensorName).fill(0, totalEffPoserr);

            TotalCorrectedEff.get(sensorName).fill(0, totalEffCorrected);
            TotalCorrectedEffEle.get(sensorName).fill(0, totalEffEleCorrected);
            TotalCorrectedEffPos.get(sensorName).fill(0, totalEffPosCorrected);
            TotalCorrectedEfferr.get(sensorName).fill(0, totalEfferrCorrected);
            TotalCorrectedEffEleerr.get(sensorName).fill(0, totalEffEleerrCorrected);
            TotalCorrectedEffPoserr.get(sensorName).fill(0, totalEffPoserrCorrected);

            //Print out efficiency and corrected efficiency for each sensor with error
            System.out.println(sensorName + " Total Efficiency = " + totalEff + " with error " + totalEfferr);
            System.out.println(sensorName + " Total Corrected Efficiency = " + totalEffCorrected + " with error " + totalEfferrCorrected);

            //Output efficiencies as a function of channel in a text file
            org.hps.util.Pair<Integer, Integer> daqPair = getDaqPair(daqMap, sensor);
            Collection<SvtChannel> channels = channelMap.find(daqPair);
            for (SvtChannel channel : channels) {
                int chanID = channel.getChannelID();
                int chan = channel.getChannel();
                if (chan < hitEfficiencyChannel.get(sensorName).axis().bins()) {
                    double eff = hitEfficiencyChannel.get(sensorName).binHeight(chan);
                    System.out.println(chanID + ", " + eff);
                    out.println(chanID + ", " + eff);
                }
            }
        }
        out.close();
    }

    static org.hps.util.Pair<Integer, Integer> getDaqPair(SvtDaqMappingCollection daqMap, HpsSiSensor sensor) {

        final String svtHalf = sensor.isTopLayer() ? AbstractSvtDaqMapping.TOP_HALF : AbstractSvtDaqMapping.BOTTOM_HALF;
        for (final SvtDaqMapping object : daqMap)

            if (svtHalf.equals(object.getSvtHalf()) && object.getLayerNumber() == sensor.getLayerNumber()
                    && object.getSide().equals(sensor.getSide()))

                return new org.hps.util.Pair<Integer, Integer>(object.getFebID(), object.getFebHybridID());
        return null;
    }
}
