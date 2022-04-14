package org.hps.recon.tracking.kalman;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.conditions.beam.BeamEnergy.BeamEnergyCollection;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.AbstractSvtDaqMapping;
import org.hps.conditions.svt.SvtChannel;
import org.hps.conditions.svt.SvtChannel.SvtChannelCollection;
import org.hps.conditions.svt.SvtConditions;
import org.hps.conditions.svt.SvtDaqMapping;
import org.hps.conditions.svt.SvtDaqMapping.SvtDaqMappingCollection;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.gbl.matrix.Matrix;
import org.lcsim.detector.solids.Box;
import org.lcsim.detector.solids.LineSegment3D;
import org.lcsim.detector.solids.Polygon3D;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.TrackState;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.hps.recon.tracking.TrackIntersectData;
import hep.physics.matrix.SymmetricMatrix;
import org.hps.recon.tracking.TrackStateUtils;


import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.ITree;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;
import org.hps.util.Pair;
//import org.apache.commons.math3.util.Pair;


/**
 * Driver used to compute SVT hit efficiencies at each sensor as a function of
 * strip and y Unbiased Hit Residuals are also computed TODO Cleanup Code, add
 */
public class SvtHitEfficiencyKalman extends Driver {

    // Plotting
    protected AIDA aida = AIDA.defaultInstance();
    ITree tree;
    IHistogramFactory histogramFactory;
    boolean debug=false; 
    // List of Sensors
    private List<HpsSiSensor> sensors = null;

    // List of Histograms
    Map<String, IHistogram1D> numberOfTracksChannel = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerChannel = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyChannel = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksU = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerU = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyU = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksP = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerP = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyP = new HashMap<String, IHistogram1D>();

    Map<String, IHistogram1D> numberOfTracksChannelEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerChannelEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyChannelEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksUEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerUEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyUEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksPEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerPEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPEle = new HashMap<String, IHistogram1D>();

    Map<String, IHistogram1D> numberOfTracksChannelPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerChannelPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyChannelPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksUPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerUPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyUPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksPPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerPPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPPos = new HashMap<String, IHistogram1D>();
    /*
    Map<String, IHistogram1D> numberOfTracksChannelCorrected = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyChannelCorrected = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksUCorrected = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyUCorrected = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksPCorrected = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPCorrected = new HashMap<String, IHistogram1D>();

    Map<String, IHistogram1D> numberOfTracksChannelCorrectedEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyChannelCorrectedEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksUCorrectedEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyUCorrectedEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksPCorrectedEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPCorrectedEle = new HashMap<String, IHistogram1D>();

    Map<String, IHistogram1D> numberOfTracksChannelCorrectedPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyChannelCorrectedPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksUCorrectedPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyUCorrectedPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksPCorrectedPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPCorrectedPos = new HashMap<String, IHistogram1D>();
    */
    Map<String, IHistogram1D> TotalEff = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> TotalEffEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> TotalEffPos = new HashMap<String, IHistogram1D>();
    /*
    Map<String, IHistogram1D> TotalCorrectedEff = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> TotalCorrectedEffEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> TotalCorrectedEffPos = new HashMap<String, IHistogram1D>();
    */
    Map<String, IHistogram1D> hitEfficiencyChannelerr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyUerr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPerr = new HashMap<String, IHistogram1D>();
    /*
    Map<String, IHistogram1D> hitEfficiencyChannelCorrectederr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyUCorrectederr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPCorrectederr = new HashMap<String, IHistogram1D>();
    */
    Map<String, IHistogram1D> hitEfficiencyChannelEleerr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyUEleerr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPEleerr = new HashMap<String, IHistogram1D>();
    /*
    Map<String, IHistogram1D> hitEfficiencyChannelCorrectedEleerr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyUCorrectedEleerr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPCorrectedEleerr = new HashMap<String, IHistogram1D>();
    */
    Map<String, IHistogram1D> hitEfficiencyChannelPoserr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyUPoserr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPPoserr = new HashMap<String, IHistogram1D>();
    /*
    Map<String, IHistogram1D> hitEfficiencyChannelCorrectedPoserr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyUCorrectedPoserr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPCorrectedPoserr = new HashMap<String, IHistogram1D>();
    */
    Map<String, IHistogram1D> TotalEfferr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> TotalEffEleerr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> TotalEffPoserr = new HashMap<String, IHistogram1D>();
    /*
    Map<String, IHistogram1D> TotalCorrectedEfferr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> TotalCorrectedEffEleerr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> TotalCorrectedEffPoserr = new HashMap<String, IHistogram1D>();
    */
    Map<String, IHistogram1D> errorU = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram2D> errorUvsU = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> errorUvsV = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> errorUvsP = new HashMap<String, IHistogram2D>();

    Map<String, IHistogram1D> errorUEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram2D> errorUvsUEle = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> errorUvsVEle = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> errorUvsPEle = new HashMap<String, IHistogram2D>();

    Map<String, IHistogram1D> errorUPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram2D> errorUvsUPos = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> errorUvsVPos = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> errorUvsPPos = new HashMap<String, IHistogram2D>();

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

    // Histogram Settings
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

    // Collection Strings
    private String stripHitOutputCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private String TrackCollectionName = "KalmanFullTracks";
    private String kalIntersectsName="KFUnbiasInt";
    private String kalIntersectsRelsName="KFUnbiasIntRelations";
    // Bfield
    protected static double bfield;
    FieldMap bFieldMap = null;

    private static final String SUBDETECTOR_NAME = "Tracker";

    String outputFileName = "channelEff.txt";
    boolean cleanFEE = false;
    int thinLayCutoff=4; //thin layers = 0,1,2,3
    double nSig = 5;
    boolean maskBadChannels = false;
    int chanExtd = 0;

    // Daq map
    SvtChannelCollection channelMap;
    SvtDaqMappingCollection daqMap;

    public void setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
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

    // Beam Energy
    double ebeam;

    public void detectorChanged(Detector detector) {

        aida.tree().cd("/");
        tree = aida.tree();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);

        // Grab channel map and daq map from conditions database
        DatabaseConditionsManager mgr = DatabaseConditionsManager.getInstance();
        SvtConditions svtConditions = mgr.getCachedConditions(SvtConditions.class, "svt_conditions").getCachedData();

        channelMap = svtConditions.getChannelMap();
        daqMap = svtConditions.getDaqMap();

        // Set Beam Energy
        BeamEnergyCollection beamEnergyCollection = this.getConditionsManager()
                .getCachedConditions(BeamEnergyCollection.class, "beam_energies").getCachedData();
        ebeam = beamEnergyCollection.get(0).getBeamEnergy();

        bfield = TrackUtils.getBField(detector).magnitude();
        System.out.println("bfield = "+bfield);
        bFieldMap = detector.getFieldMap();

        // Get the HpsSiSensor objects from the tracker detector element
        sensors = detector.getSubdetector(SUBDETECTOR_NAME).getDetectorElement().findDescendants(HpsSiSensor.class);
        
        // If the detector element had no sensors associated with it, throw
        // an exception
        if (sensors.size() == 0) {
            throw new RuntimeException("No sensors were found in this detector.");
        }

        // Setup Plots
        D0.put(atIP, histogramFactory.createHistogram1D("D0 " + atIP, nBins, minD0, maxD0));
        Z0.put(atIP, histogramFactory.createHistogram1D("Z0 " + atIP, nBins, minZ0, maxZ0));
        Tanlambda.put(atIP, histogramFactory.createHistogram1D("TanLambda " + atIP, nBins, minTLambda, maxTLambda));
        Phi0.put(atIP, histogramFactory.createHistogram1D("Phi0 " + atIP, nBins, minPhi0, maxPhi0));
        Omega.put(atIP, histogramFactory.createHistogram1D("Omega " + atIP, nBins, minOmega, maxOmega));

        D0_err.put(atIP, histogramFactory.createHistogram1D("D0 Error " + atIP, nBins, minD0Err, maxD0Err));
        Z0_err.put(atIP, histogramFactory.createHistogram1D("Z0 Error " + atIP, nBins, minZ0Err, maxZ0Err));
        Tanlambda_err.put(atIP,
                histogramFactory.createHistogram1D("TanLambda Error " + atIP, nBins, minTLambdaErr, maxTLambdaErr));
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
            int layer=sensor.getLayerNumber();
            int minHOTs=8; 
            int maxHOTs=14; 
            // histogram orginization:  <layer>/<nhits>/<hname> 
            // <nhits> is the number of hits on track -1 if the layer we are looking at has a hit on track!
            for (int hot=minHOTs; hot<maxHOTs; hot++){
                String dirName="layer"+layer+"/nHits"+hot+"/";
                String mapName=sensorName+"-"+hot; 
                aida.tree().cd("/");
                aida.tree().mkdirs(dirName);
                System.out.println("making tree directory with name = "+dirName); 
                numberOfTracksChannel.put(mapName, histogramFactory.createHistogram1D(dirName+"Number of Tracks Channel " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                numberOfTracksWithHitOnMissingLayerChannel.put(mapName, histogramFactory.createHistogram1D(dirName+"Number of Tracks With Hit Channel " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                //                hitEfficiencyChannel.put(mapName, histogramFactory.createHistogram1D("HitEfficiency Channel " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                numberOfTracksU.put(mapName, histogramFactory.createHistogram1D(dirName+"Number of Tracks U " + sensorName, nBins, -maxU, maxU));
                numberOfTracksWithHitOnMissingLayerU.put(mapName, histogramFactory.createHistogram1D(dirName+"Number of Tracks With Hit U " + sensorName, nBins, -maxU, maxU));
                //                hitEfficiencyU.put(mapName,histogramFactory.createHistogram1D(dirName+"HitEfficiency U " + sensorName, nBins, -maxU, maxU));
                numberOfTracksP.put(mapName,histogramFactory.createHistogram1D(dirName+"Number of Tracks P " + sensorName, nBins, 0, 1.3 * ebeam));
                numberOfTracksWithHitOnMissingLayerP.put(mapName, histogramFactory.createHistogram1D(dirName+"Number of Tracks With Hit P " + sensorName, nBins, 0, 1.3 * ebeam));
                //                hitEfficiencyP.put(mapName,histogramFactory.createHistogram1D(dirName+"HitEfficiency P " + sensorName, nBins, 0, 1.3 * ebeam));
                
                numberOfTracksChannelEle.put(mapName, histogramFactory.createHistogram1D(dirName+"Number of Tracks Channel Ele " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                numberOfTracksWithHitOnMissingLayerChannelEle.put(mapName, histogramFactory.createHistogram1D(dirName+"Number of Tracks With Hit Channel Ele " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                //                hitEfficiencyChannelEle.put(mapName, histogramFactory.createHistogram1D(dirName+"HitEfficiency Channel Ele " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                numberOfTracksUEle.put(mapName,  histogramFactory.createHistogram1D(dirName+"Number of Tracks U Ele " + sensorName, nBins, -maxU, maxU));
                numberOfTracksWithHitOnMissingLayerUEle.put(mapName, histogramFactory.createHistogram1D(dirName+"Number of Tracks With Hit U Ele " + sensorName, nBins, -maxU, maxU));
                //hitEfficiencyUEle.put(mapName,histogramFactory.createHistogram1D(dirName+"HitEfficiency U Ele " + sensorName, nBins, -maxU, maxU));
                numberOfTracksPEle.put(mapName,histogramFactory.createHistogram1D(dirName+"Number of Tracks P Ele " + sensorName, nBins, 0, 1.3 * ebeam));
                numberOfTracksWithHitOnMissingLayerPEle.put(mapName, histogramFactory.createHistogram1D(dirName+"Number of Tracks With Hit P Ele " + sensorName, nBins, 0, 1.3 * ebeam));
                //                hitEfficiencyPEle.put(mapName, histogramFactory.createHistogram1D(dirName+"HitEfficiency P Ele " + sensorName, nBins, 0, 1.3 * ebeam));
                
                numberOfTracksChannelPos.put(mapName, histogramFactory.createHistogram1D(dirName+"Number of Tracks Channel Pos " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                numberOfTracksWithHitOnMissingLayerChannelPos.put(mapName,histogramFactory.createHistogram1D(dirName+"Number of Tracks With Hit Channel Pos " + sensorName,nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                //                hitEfficiencyChannelPos.put(mapName, histogramFactory.createHistogram1D(dirName+"HitEfficiency Channel Pos " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                numberOfTracksUPos.put(mapName, histogramFactory.createHistogram1D(dirName+"Number of Tracks U Pos " + sensorName, nBins, -maxU, maxU));
                numberOfTracksWithHitOnMissingLayerUPos.put(mapName, histogramFactory.createHistogram1D(dirName+"Number of Tracks With Hit U Pos " + sensorName, nBins, -maxU, maxU));
                //hitEfficiencyUPos.put(mapName, histogramFactory.createHistogram1D(dirName+"HitEfficiency U Pos " + sensorName, nBins, -maxU, maxU));
                numberOfTracksPPos.put(mapName,histogramFactory.createHistogram1D(dirName+"Number of Tracks P Pos " + sensorName, nBins, 0, 1.3 * ebeam));
                numberOfTracksWithHitOnMissingLayerPPos.put(mapName, histogramFactory.createHistogram1D(dirName+"Number of Tracks With Hit P Pos " + sensorName, nBins, 0, 1.3 * ebeam));
                //hitEfficiencyPPos.put(mapName,histogramFactory.createHistogram1D(dirName+"HitEfficiency P Pos " + sensorName, nBins, 0, 1.3 * ebeam));
                /*
                  numberOfTracksChannelCorrected.put(sensorName,
                    histogramFactory.createHistogram1D(dirName+"Number of Tracks Channel Corrected " + sensorName,
                            nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                            hitEfficiencyChannelCorrected.put(sensorName,
                            histogramFactory.createHistogram1D(dirName+"HitEfficiency Channel Corrected " + sensorName,
                            nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                            numberOfTracksUCorrected.put(sensorName, histogramFactory
                            .createHistogram1D(dirName+"Number of Tracks U Corrected " + sensorName, nBins, -maxU, maxU));
                            hitEfficiencyUCorrected.put(sensorName,
                            histogramFactory.createHistogram1D(dirName+"HitEfficiency U Corrected " + sensorName, nBins, -maxU, maxU));
                            numberOfTracksPCorrected.put(sensorName, histogramFactory
                            .createHistogram1D(dirName+"Number of Tracks P Corrected " + sensorName, nBins, 0, 1.3 * ebeam));
                            hitEfficiencyPCorrected.put(sensorName, histogramFactory
                            .createHistogram1D(dirName+"HitEfficiency P Corrected " + sensorName, nBins, 0, 1.3 * ebeam));
                            
                            numberOfTracksChannelCorrectedEle.put(sensorName,
                            histogramFactory.createHistogram1D(dirName+"Number of Tracks Channel Corrected Ele " + sensorName,
                            nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                            hitEfficiencyChannelCorrectedEle.put(sensorName,
                            histogramFactory.createHistogram1D(dirName+"HitEfficiency Channel Corrected Ele " + sensorName,
                            nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                            numberOfTracksUCorrectedEle.put(sensorName, histogramFactory
                            .createHistogram1D(dirName+"Number of Tracks U Corrected Ele " + sensorName, nBins, -maxU, maxU));
                            hitEfficiencyUCorrectedEle.put(sensorName, histogramFactory
                            .createHistogram1D(dirName+"HitEfficiency U Corrected Ele " + sensorName, nBins, -maxU, maxU));
                            numberOfTracksPCorrectedEle.put(sensorName, histogramFactory
                            .createHistogram1D(dirName+"Number of Tracks P Corrected Ele " + sensorName, nBins, 0, 1.3 * ebeam));
                            hitEfficiencyPCorrectedEle.put(sensorName, histogramFactory
                            .createHistogram1D(dirName+"HitEfficiency P Corrected Ele " + sensorName, nBins, 0, 1.3 * ebeam));
                            
                            numberOfTracksChannelCorrectedPos.put(sensorName,
                            histogramFactory.createHistogram1D(dirName+"Number of Tracks Channel Corrected Pos " + sensorName,
                            nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                            hitEfficiencyChannelCorrectedPos.put(sensorName,
                            histogramFactory.createHistogram1D(dirName+"HitEfficiency Channel Corrected Pos " + sensorName,
                            nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                            numberOfTracksUCorrectedPos.put(sensorName, histogramFactory
                            .createHistogram1D(dirName+"Number of Tracks U Corrected Pos " + sensorName, nBins, -maxU, maxU));
                            hitEfficiencyUCorrectedPos.put(sensorName, histogramFactory
                            .createHistogram1D(dirName+"HitEfficiency U Corrected Pos " + sensorName, nBins, -maxU, maxU));
                            numberOfTracksPCorrectedPos.put(sensorName, histogramFactory
                            .createHistogram1D(dirName+"Number of Tracks P Corrected Pos " + sensorName, nBins, 0, 1.3 * ebeam));
                            hitEfficiencyPCorrectedPos.put(sensorName, histogramFactory
                            .createHistogram1D(dirName+"HitEfficiency P Corrected Pos " + sensorName, nBins, 0, 1.3 * ebeam));
                */
                /*
                TotalEff.put(mapName, histogramFactory.createHistogram1D(dirName+"Total Eff " + sensorName, 1, 0, 1));
                TotalEffEle.put(mapName, histogramFactory.createHistogram1D(dirName+"Total Eff Ele " + sensorName, 1, 0, 1));
                TotalEffPos.put(mapName, histogramFactory.createHistogram1D(dirName+"Total Eff Pos " + sensorName, 1, 0, 1));
                */
                /*
                  TotalCorrectedEff.put(sensorName,
                  histogramFactory.createHistogram1D(dirName+"Total Corrected Eff " + sensorName, 1, 0, 1));
                  TotalCorrectedEffEle.put(sensorName,
                  histogramFactory.createHistogram1D(dirName+"Total Corrected Eff Ele " + sensorName, 1, 0, 1));
                  TotalCorrectedEffPos.put(sensorName,
                  histogramFactory.createHistogram1D(dirName+"Total Corrected Eff Pos " + sensorName, 1, 0, 1));
                */
                /*
                hitEfficiencyChannelerr.put(mapName, histogramFactory.createHistogram1D(
                                                                                           "HitEfficiency Channel Error " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                hitEfficiencyUerr.put(mapName,
                                      histogramFactory.createHistogram1D(dirName+"HitEfficiency U Error " + sensorName, nBins, -maxU, maxU));
                hitEfficiencyPerr.put(mapName,
                                      histogramFactory.createHistogram1D(dirName+"HitEfficiency P Error " + sensorName, nBins, 0, 1.3 * ebeam));
                */
                /*
                  hitEfficiencyChannelCorrectederr.put(sensorName,
                  histogramFactory.createHistogram1D(dirName+"HitEfficiency Channel Corrected Error " + sensorName,
                  nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                  hitEfficiencyUCorrectederr.put(sensorName, histogramFactory
                  .createHistogram1D(dirName+"HitEfficiency U Corrected Error " + sensorName, nBins, -maxU, maxU));
                  hitEfficiencyPCorrectederr.put(sensorName, histogramFactory
                  .createHistogram1D(dirName+"HitEfficiency P Corrected Error " + sensorName, nBins, 0, 1.3 * ebeam));
                */
                /*
                hitEfficiencyChannelEleerr.put(mapName,
                                               histogramFactory.createHistogram1D(dirName+"HitEfficiency Channel Ele Error " + sensorName,
                                                                                  nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                hitEfficiencyUEleerr.put(mapName,
                                         histogramFactory.createHistogram1D(dirName+"HitEfficiency U Ele Error " + sensorName, nBins, -maxU, maxU));
                hitEfficiencyPEleerr.put(mapName, histogramFactory
                                         .createHistogram1D(dirName+"HitEfficiency P Ele Error " + sensorName, nBins, 0, 1.3 * ebeam));
                */
                /*
                  hitEfficiencyChannelCorrectedEleerr.put(sensorName,
                  histogramFactory.createHistogram1D(dirName+"HitEfficiency Channel Corrected Ele Error " + sensorName,
                  nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                  hitEfficiencyUCorrectedEleerr.put(sensorName, histogramFactory
                  .createHistogram1D(dirName+"HitEfficiency U Corrected Ele Error " + sensorName, nBins, -maxU, maxU));
                  hitEfficiencyPCorrectedEleerr.put(sensorName, histogramFactory
                  .createHistogram1D(dirName+"HitEfficiency P Corrected Ele Error " + sensorName, nBins, 0, 1.3 * ebeam));
                */
                /*
                hitEfficiencyChannelPoserr.put(mapName, histogramFactory.createHistogram1D(
                                                                                              "HitEfficiency Channel Pos Error " + sensorName, nChan, -chanExtd, nChan + chanExtd));
                hitEfficiencyUPoserr.put(mapName,
                                         histogramFactory.createHistogram1D(dirName+"HitEfficiency U Pos Error " + sensorName, nBins, -maxU, maxU));
                hitEfficiencyPPoserr.put(mapName, histogramFactory
                                         .createHistogram1D(dirName+"HitEfficiency P Pos Error " + sensorName, nBins, 0, 1.3 * ebeam));
                */
                /*
                  hitEfficiencyChannelCorrectedPoserr.put(sensorName,
                  histogramFactory.createHistogram1D(dirName+"HitEfficiency Channel Corrected Pos Error " + sensorName,
                  nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                  hitEfficiencyUCorrectedPoserr.put(sensorName, histogramFactory
                  .createHistogram1D(dirName+"HitEfficiency U Corrected Pos Error " + sensorName, nBins, -maxU, maxU));
                  hitEfficiencyPCorrectedPoserr.put(sensorName, histogramFactory
                  .createHistogram1D(dirName+"HitEfficiency P Corrected Pos Error " + sensorName, nBins, 0, 1.3 * ebeam));
                */
                /*
                TotalEfferr.put(mapName, histogramFactory.createHistogram1D(dirName+"Total Eff Error " + sensorName, 1, 0, 1));
                TotalEffEleerr.put(mapName,
                                   histogramFactory.createHistogram1D(dirName+"Total Eff Ele Error " + sensorName, 1, 0, 1));
                TotalEffPoserr.put(mapName,
                                   histogramFactory.createHistogram1D(dirName+"Total Eff Pos Error " + sensorName, 1, 0, 1));
                */
                /*
                  TotalCorrectedEfferr.put(mapName,
                  histogramFactory.createHistogram1D(dirName+"Total Corrected Eff Error " + sensorName, 1, 0, 1));
                  TotalCorrectedEffEleerr.put(mapName,
                  histogramFactory.createHistogram1D(dirName+"Total Corrected Eff Ele Error " + sensorName, 1, 0, 1));
                  TotalCorrectedEffPoserr.put(mapName,
                  histogramFactory.createHistogram1D(dirName+"Total Corrected Eff Pos Error " + sensorName, 1, 0, 1));
                */
                /*
                errorU.put(mapName, histogramFactory.createHistogram1D(dirName+"Error U " + sensorName, nBins, 0, maxYerror));
                errorUvsV.put(mapName, histogramFactory.createHistogram2D(dirName+"Error U vs V " + sensorName, 2 * nBins, minV,
                                                                             maxV, nBins, 0, maxYerror));
                errorUvsU.put(mapName, histogramFactory.createHistogram2D(dirName+"Error U vs U " + sensorName, 2 * nBins, -maxU,
                                                                             maxU, nBins, 0, maxYerror));
                errorUvsP.put(mapName, histogramFactory.createHistogram2D(dirName+"Error U vs P " + sensorName, nBins, 0,
                                                                             1.3 * ebeam, nBins, 0, maxYerror));
                
                errorUEle.put(mapName,
                              histogramFactory.createHistogram1D(dirName+"Error U Electrons " + sensorName, nBins, 0, maxYerror));
                errorUvsVEle.put(mapName, histogramFactory.createHistogram2D(dirName+"Error U vs V Electrons " + sensorName,
                                                                                2 * nBins, minV, maxV, nBins, 0, maxYerror));
                errorUvsUEle.put(mapName, histogramFactory.createHistogram2D(dirName+"Error U vs U Electrons " + sensorName,
                                                                                2 * nBins, -maxU, maxU, nBins, 0, maxYerror));
                errorUvsPEle.put(mapName, histogramFactory.createHistogram2D(dirName+"Error U vs P Electrons " + sensorName,
                                                                                nBins, 0, 1.3 * ebeam, nBins, 0, maxYerror));
                
                errorUPos.put(mapName,
                              histogramFactory.createHistogram1D(dirName+"Error U Electrons " + sensorName, nBins, 0, maxYerror));
                errorUvsVPos.put(mapName, histogramFactory.createHistogram2D(dirName+"Error U vs V Positrons " + sensorName,
                                                                                2 * nBins, minV, maxV, nBins, 0, maxYerror));
                errorUvsUPos.put(mapName, histogramFactory.createHistogram2D(dirName+"Error U vs U Positrons " + sensorName,
                                                                                2 * nBins, -maxU, maxU, nBins, 0, maxYerror));
                errorUvsPPos.put(mapName, histogramFactory.createHistogram2D(dirName+"Error U vs P Positrons " + sensorName,
                                                                                nBins, 0, 1.3 * ebeam, nBins, 0, maxYerror));
                */
                /*
                  D0.put(sensorName, histogramFactory.createHistogram1D(dirName+"D0 " + sensorName, nBins, minD0, maxD0));
                  Z0.put(sensorName, histogramFactory.createHistogram1D(dirName+"Z0 " + sensorName, nBins, minZ0, maxZ0));
                  Tanlambda.put(sensorName,
                  histogramFactory.createHistogram1D(dirName+"TanLambda " + sensorName, nBins, minTLambda, maxTLambda));
                  Phi0.put(sensorName, histogramFactory.createHistogram1D(dirName+"Phi0 " + sensorName, nBins, minPhi0, maxPhi0));
                  Omega.put(sensorName, histogramFactory.createHistogram1D(dirName+"Omega " + sensorName, nBins, minOmega, maxOmega));
                  
                  D0_err.put(sensorName,
                  histogramFactory.createHistogram1D(dirName+"D0 Error " + sensorName, nBins, minD0Err, maxD0Err));
                  Z0_err.put(sensorName,
                  histogramFactory.createHistogram1D(dirName+"Z0 Error " + sensorName, nBins, minZ0Err, maxZ0Err));
                  Tanlambda_err.put(sensorName, histogramFactory.createHistogram1D(dirName+"TanLambda Error " + sensorName, nBins,
                  minTLambdaErr, maxTLambdaErr));
                  Phi0_err.put(sensorName,
                  histogramFactory.createHistogram1D(dirName+"Phi0 Error " + sensorName, nBins, minPhi0Err, maxPhi0Err));
                  Omega_err.put(sensorName,
                  histogramFactory.createHistogram1D(dirName+"Omega Error " + sensorName, nBins, minOmegaErr, maxOmegaErr));
                */
            }
        }
    }

    public void process(EventHeader event) {
        aida.tree().cd("/");

        // Grab all Kalman Tracks in the event
        List<Track> tracks = event.get(Track.class, TrackCollectionName);
        List<LCRelation> kalIntersectsRelations = event.get(LCRelation.class, kalIntersectsRelsName);
        // Grab all the clusters in the event
        List<SiTrackerHitStrip1D> stripHits = event.get(SiTrackerHitStrip1D.class, stripHitOutputCollectionName);

        for (Track track : tracks) {
            TrackIntersectData trkInts = null;
            for (LCRelation intRel : kalIntersectsRelations) {
                if (intRel.getTo() == track) {
                    trkInts = (TrackIntersectData) intRel.getFrom();
                }
            }
            if (trkInts == null) {
                System.out.println("Couldn't find intersect data to go with the track???");
                continue;
            }
            List<TrackState> TStates = track.getTrackStates();
            if(debug)
                for(TrackState ts: TStates )
                    System.out.println("Track State::  location = "+ts.getLocation()
                                       +"; reference x = "+ts.getReferencePoint()[0]
                                       +"; reference y = "+ts.getReferencePoint()[1]
                                       +"; reference z = "+ts.getReferencePoint()[2]);
            
            TrackState tStateIP = TStates.get(0);
            double[] covAtIP = TStates.get(0).getCovMatrix();
            SymmetricMatrix LocCovAtIP = new SymmetricMatrix(5, covAtIP, true);
            
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
            
            Hep3Vector p = toHep3(tStateIP.getMomentum());
            double q = -track.getCharge(); // HelicalTrackFit flips sign of charge
            int nHitsOnTrack=track.getTrackerHits().size();
            //System.out.println("Track has "+trkInts.getNInt()+" integers in layers list ");
            for (int l = 0; l < trkInts.getNInt()-1; l++) {
                int layer=trkInts.getLayer(l);
                Double[] interTmp=trkInts.getIntersect(l);
                Double[] intersect=flipInter(interTmp);
                double intersectU=intersect[0];
                float sigmaU=(float)Math.sqrt(trkInts.getSigma(l));
                boolean lyrHasHit=layerHasHit(track,layer);
                //System.out.println("layer = "+layer+" u-intersection = "+intersectU+" +/- "+sigmaU+"  has hit? "+lyrHasHit);
                // Fill track states and errors at IP
                //Get all track states for this track


                if (cleanFEE) {
                    // Require track to be an electron
                    if (q < 0) {
                        continue;
                    }

                    // Select around the FEE momentum peak
                    if (p.magnitude() < 0.75 * ebeam || p.magnitude() > 1.25 * ebeam) {
                        continue;
                    }
                }

                // See if track is within acceptance of unused layer
                Pair<HpsSiSensor,Integer> sensorChanAccPair = getSensorChanInAcceptance(track,intersect, layer);

                // Set axial and stereo sensors of the missing layer
                HpsSiSensor sensor = sensorChanAccPair.getFirstElement();

                String sensorName = sensor.getName();
                int hot=nHitsOnTrack;
                if(lyrHasHit)
                    hot=hot-1; 
                if(hot<8)
                    continue; 
                String mapName=sensorName+"-"+hot; 

                // Compute the channel where the track extrapolates to in each sensor
                int chan = sensorChanAccPair.getSecondElement();

                double trackP = toHep3(track.getTrackStates().get(0).getMomentum()).magnitude();

                double weight = findWeight(intersectU, sigmaU, sensor);
                
                // Fill the denominator of the efficiency histos
                numberOfTracksChannel.get(mapName).fill(chan);
                numberOfTracksU.get(mapName).fill(intersectU);
                numberOfTracksP.get(mapName).fill(trackP);
                /*
                numberOfTracksChannelCorrected.get(mapName).fill(chan, weight);
                numberOfTracksUCorrected.get(mapName).fill(intersectU, weight);
                numberOfTracksPCorrected.get(mapName).fill(trackP, weight);
                */
                // Fill electron histograms
                if (q < 0) {
                    numberOfTracksChannelEle.get(mapName).fill(chan);
                    numberOfTracksUEle.get(mapName).fill(intersectU);
                    numberOfTracksPEle.get(mapName).fill(trackP);
                    /*
                    numberOfTracksChannelCorrectedEle.get(mapName).fill(chan, weight);
                    numberOfTracksUCorrectedEle.get(mapName).fill(intersectU, weight);
                    numberOfTracksPCorrectedEle.get(mapName).fill(trackP, weight);
                    */
                } // Fill positron histograms
                else {
                    numberOfTracksChannelPos.get(mapName).fill(chan);
                    numberOfTracksUPos.get(mapName).fill(intersectU);
                    numberOfTracksPPos.get(mapName).fill(trackP);
                    /*
                    numberOfTracksChannelCorrectedPos.get(mapName).fill(chan, weight);
                    numberOfTracksUCorrectedPos.get(mapName).fill(intersectU, weight);
                    numberOfTracksPCorrectedPos.get(mapName).fill(trackP, weight);
                    */
                }

                // Fill the error histos
                /*
                errorU.get(mapName).fill(sigmaU);
                errorUvsV.get(mapName).fill(intersect[1], sigmaU);
                errorUvsU.get(mapName).fill(intersectU, sigmaU);
                errorUvsP.get(mapName).fill(p.magnitude(), sigmaU);

                if (q < 0) {
                    errorUEle.get(mapName).fill(sigmaU);
                    errorUvsVEle.get(mapName).fill(intersect[1], sigmaU);
                    errorUvsUEle.get(mapName).fill(intersectU, sigmaU);
                    errorUvsPEle.get(mapName).fill(p.magnitude(), sigmaU);
                } else {
                    errorUPos.get(mapName).fill(sigmaU);
                    errorUvsVPos.get(mapName).fill(intersect[1], sigmaU);
                    errorUvsUPos.get(mapName).fill(intersectU, sigmaU);
                    errorUvsPPos.get(mapName).fill(p.magnitude(), sigmaU);
                }
                */
                // If layer has a hit included in track, fill the numerator efficiency histograms
                if (lyrHasHit) {
                    numberOfTracksWithHitOnMissingLayerChannel.get(mapName).fill(chan);
                    numberOfTracksWithHitOnMissingLayerU.get(mapName).fill(intersectU);
                    numberOfTracksWithHitOnMissingLayerP.get(mapName).fill(trackP);
                    if (q < 0) {
                        numberOfTracksWithHitOnMissingLayerChannelEle.get(mapName).fill(chan);
                        numberOfTracksWithHitOnMissingLayerUEle.get(mapName).fill(intersectU);
                        numberOfTracksWithHitOnMissingLayerPEle.get(mapName).fill(trackP);
                    } else {
                        numberOfTracksWithHitOnMissingLayerChannelPos.get(mapName).fill(chan);
                        numberOfTracksWithHitOnMissingLayerUPos.get(mapName).fill(intersectU);
                        numberOfTracksWithHitOnMissingLayerPPos.get(mapName).fill(trackP);
                    }
                }
            }
        }
    }

    private Double[] flipInter(Double[] tmp){
        Double[] thisguy={tmp[1],tmp[0],tmp[2]};
        return thisguy; 
    }

    // Computes weight based on the number of sigmas (u error) the track
    // extrapolates from the edge of the sensor
    private double findWeight(double y, double yErr, HpsSiSensor sensor) {
        double readoutPitch = sensor.getReadoutStripPitch();
        int nChan = sensor.getNumberOfChannels();
        boolean firstChan = firstChanIsEdge(sensor);
        double height = readoutPitch * nChan;
        double distanceToEdge = 0;
        if (firstChan) {
            distanceToEdge = height / 2 - y;
        } else {
            distanceToEdge = height / 2 + y;
        }
        double nSig = distanceToEdge / yErr;
        return computeGaussInt(nSig, 1000);
    }

    // Computes gaussian integral numerically from -inf to nSig
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

    // Gaussian function
    private double Gauss(double x, double mean, double sigma) {
        return 1 / (Math.sqrt(2 * Math.PI * Math.pow(sigma, 2)))
                * Math.exp(-Math.pow(x - mean, 2) / (2 * Math.pow(sigma, 2)));
    }

    // Some sensors have channel 0 closest to the beam
    // Others have channel 640 closest to the beam
    // Use this function to find out which one your sensor is!
    private boolean firstChanIsEdge(HpsSiSensor sensor) {
        int layer = (sensor.getLayerNumber() + 1) / 2;
        if (layer > 0 && layer < 4) {
            if (sensor.isAxial()) {
                return false;
            } else {
                return true;
            }
        } else if (sensor.isAxial()) {
            if (sensor.getSide().matches("ELECTRON")) {
                return false;
            } else {
                return true;
            }
        } else if (!sensor.getSide().matches("ELECTRON")) {
            return false;
        } else {
            return true;
        }
    }

    // Converts position into sensor frame
    private Hep3Vector globalToSensor(Hep3Vector trkpos, HpsSiSensor sensor) {
        SiSensorElectrodes electrodes = sensor.getReadoutElectrodes(ChargeCarrier.HOLE);
        if (electrodes == null) {
            electrodes = sensor.getReadoutElectrodes(ChargeCarrier.ELECTRON);
            System.out.println("Charge Carrier is NULL");
        }
        if (electrodes == null){
            System.out.println("Electrodes are null???");
        }
        if(debug)System.out.println("Transforming globalToLocal::  global x ="+ trkpos.x()+" y = "+trkpos.y()+" z ="+ trkpos.z());
        
        return electrodes.getGlobalToLocal().transformed(trkpos);
    }

    // Returns channel number of a given position in the sensor frame
    private int getChan(Hep3Vector pos, HpsSiSensor sensor) {   
        SiSensorElectrodes electrodes = sensor.getReadoutElectrodes(ChargeCarrier.HOLE);
        if(sensor.getLayerNumber()<5){
            int row=electrodes.getRowNumber(pos);
            int col=electrodes.getColumnNumber(pos);
            return electrodes.getCellID(row,col);
        }else{
            return electrodes.getCellID(pos);            
        }
        //double readoutPitch = sensor.getReadoutStripPitch();
        //int nChan = sensor.getNumberOfChannels();
        //double height = readoutPitch * nChan;
        //      if(sensor.getLayerNumber()<thinLayCutoff)
        //    return (int)((height / 2 - pos.x()) / readoutPitch);
        //return (int) ((height / 2 - pos.x()) / readoutPitch);
    }

    // Converts double array into Hep3Vector
    private Hep3Vector toHep3(double[] arr) {
        return new BasicHep3Vector(arr[0], arr[1], arr[2]);
    }

    // Return the HpsSiSensor for a given top/bottom track, layer, axial/stereo, and
    // slot/hole
    private HpsSiSensor getSensor(Track track, int layer) {
        layer = layer + 1;
        //System.out.println("Getting sensor for layer = " + layer);
        double outerLayer = 9; 
        double tanLambda = track.getTrackStates().get(0).getTanLambda();
        boolean trkIsTop=tanLambda > 0;
        for (HpsSiSensor sensor : sensors) {
            if(sensor.getLayerNumber() != layer) 
                continue;  // bail...wrong sensor layer

            boolean sensorIsTop=sensor.isTopLayer();
            if(trkIsTop != sensorIsTop)
                continue; // comparing top/bottom...bail

            if (layer < outerLayer && layer > 0) 
                return sensor;//if we are in inner layers, we found sensor!

            //if we got here, must be in outer layers
            boolean isTrackHole=isTrackHole(track,layer);  // this looks for a track state at either hole or slot...not foolproof!
            boolean isSensorSlot=sensor.getSide().matches("ELECTRON");//ok, dumb but I forgot string for hole side...probably "POSITRON" 
            if(isTrackHole==isSensorSlot)
                continue;  /// bail if track hole/slot don't match sensor
            return sensor; //if we get here, we found an outer sensor          
        }
        if(debug)System.out.println("getSensor::  sensor not found!!!!");
        return null;  // we shouldn't ever get here!
    }

  //Return the HpsSiSensor for a given top/bottom track, layer, axial/stereo, and slot/hole
    private HpsSiSensor getSensor(Track track, int layer, boolean isAxial, boolean isHole) {
        double tanLambda = track.getTrackStates().get(0).getTanLambda();
        int outerLayer = 9;
        boolean trkIsTop=tanLambda > 0;
        for (HpsSiSensor sensor : sensors) {
            int senselayer = sensor.getLayerNumber();  //the "layer" input here has already been incremented so goes 1-14, just like the getLayerNumber() returns
            if (senselayer != layer)
                continue;
            boolean sensorIsTop=sensor.isTopLayer();
            if ((tanLambda > 0 && !sensor.isTopLayer()) || (tanLambda < 0 && sensor.isTopLayer()))
                continue;           
            if (layer < outerLayer && layer > 0)
                return sensor;
            else {
                if ((!sensor.getSide().matches("ELECTRON") && isHole) || (sensor.getSide().matches("ELECTRON") && !isHole))
                    continue;
                return sensor;
            }
        }
        if(debug)System.out.println("getSensor PickEm:: this is just getting the top/bottom/axial/hole sensor...should never happen");
        return null;
    }
    

    private Matrix Hep3ToMatrix(Hep3Matrix mat) {
        int Nrows = mat.getNRows();
        int Ncolumns = mat.getNColumns();
        Matrix matrix = new Matrix(Nrows, Ncolumns);
        for (int i = 0; i < Nrows; i++) {
            for (int j = 0; j < Ncolumns; j++) {
                matrix.set(i, j, mat.e(i, j));
            }
        }
        return matrix;
    }

    private int getUnusedSvtLayer(List<TrackerHit> stereoHits) {
        int[] svtLayer = new int[6];

        // Loop over all of the stereo hits associated with the track
        for (TrackerHit stereoHit : stereoHits) {

            // Retrieve the sensor associated with one of the hits. This will
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
        for (int layer = 0; layer < svtLayer.length; layer++) {
            if (svtLayer[layer] == 0) {
                return (layer + 1);
            }
        }
        return -1;
    }

    private Pair<HpsSiSensor,Integer> getSensorChanInAcceptance(Track track, Double[] localPos,
            int layer) {
        HpsSiSensor sensor = getSensor(track, layer);
        double[] dumbthing = new double[]{localPos[0], localPos[1], localPos[2]};
        Hep3Vector locPosVec = new BasicHep3Vector(dumbthing);
        Integer chanInAcc = new Integer(this.getChan(locPosVec, sensor));

        return new Pair<>(sensor, chanInAcc);

    }

    // Checks to see if track is in acceptance of sensor. Computes within sensor
    // frame
    // Also return channel number of the position
    public Pair<Integer, Boolean> sensorContainsTrack(Hep3Vector localPos, HpsSiSensor sensor) {
        int nChan = sensor.getNumberOfChannels();
        int chan = getChan(localPos, sensor);
        boolean inAcceptance=true; 
        if(localPos.x()<-888)
            inAcceptance=false;              
        return new Pair<>(chan, inAcceptance);
    }

    //Checks to see if track is in acceptance of sensor. Computes within sensor frame
    //Also return channel number of the position
    /*
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
    */

    boolean layerHasHit(Track track, int layer){
        layer=layer+1;
        for (TrackerHit hit:  track.getTrackerHits()){
            // Retrieve the sensor associated with one of the hits.  This will
            // be used to retrieve the layer number
            HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement();
            if(sensor.getLayerNumber() == layer)
                return true; 
        }
        return false;
    }

    
    //Get the track state at the previous sensor
    private boolean isTrackHole(Track track, int unusedLay) {
        boolean isTop = track.getTrackStates().get(0).getTanLambda() > 0;  
        int layer = unusedLay;
        HpsSiSensor sensorHole = getSensor(track, layer, isTop, true);
        HpsSiSensor sensorSlot = getSensor(track, layer, isTop, false);
        HelicalTrackFit htf = TrackUtils.getHTF(track.getTrackStates().get(0));        
        // extrapolate to hole sensor
        Hep3Vector trackHolePosGlobal = TrackStateUtils.getLocationAtSensor(htf, sensorHole, bfield);
        if(trackHolePosGlobal==null){
            System.out.println("isTrackHole::  trackHolePosGlobal is null ... setting isTrackHole=false");
            return false;
        }
        Hep3Vector trackHolePos = globalToSensor(trackHolePosGlobal, sensorHole);
        if(debug)
            System.out.println("isTrackHole::  global position at Hole:  x ="+trackHolePosGlobal.x()+
                               "; y = "+trackHolePosGlobal.y()+
                               "; z = "+trackHolePosGlobal.z());
        if(debug)
            System.out.println("isTrackHole::  local position at Hole:  x ="+trackHolePos.x()+
                               "; y = "+trackHolePos.y()+
                               "; z = "+trackHolePos.z());
        //    do slot sensor
        Hep3Vector trackSlotPosGlobal = TrackStateUtils.getLocationAtSensor(htf, sensorSlot, bfield);
        if(trackSlotPosGlobal==null){
            System.out.println("isTrackHole::  trackSlotPosGlobal is null ... setting isTrackSlot=false");
            return false;
        }
        Hep3Vector trackSlotPos = globalToSensor(trackSlotPosGlobal, sensorSlot);
        if(debug)
            System.out.println("isTrackHole::  global position at Slot:  x ="+trackSlotPosGlobal.x()+
                               "; y = "+trackSlotPosGlobal.y()+
                               "; z = "+trackSlotPosGlobal.z());

      
        if(debug)
            System.out.println("isTrackHole::  local position at Slot:  x ="+trackSlotPos.x()+
                               "; y = "+trackSlotPos.y()+
                               "; z = "+trackSlotPos.z()); 
        double yHole=Math.abs(trackHolePos.y());
        double ySlot=Math.abs(trackSlotPos.y());
        
        if(yHole<ySlot)
            return true;
        else
            return false; 

        //           TrackState tState = TrackStateUtils.getTrackStateAtSensor(track, sensorHole.getMillepedeId());
        //if (tState != null)
        //    return true;  // it is indeed hole side
        
        //tState = TrackStateUtils.getTrackStateAtSensor(track, sensorSlot.getMillepedeId());
        //if(tState!=null)
        //    return false; //it is slot side, not hole side 
        //whelp, no track state here...return false anyway
        //           if(debug)System.out.println("isTrackHole::  neither slot nor hole had track state.  This shouldn't happen");
        //        return false; 
    }

    //Checks to see if track is within acceptance of both axial and stereo sensors at a given layer
    //Also returns channel number of the intersection
    /*
    private boolean isTrackHole(Track track, TrackState tState, int layer, boolean axial, Hep3Vector p, FieldMap fieldMap) {

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
    */

    // Returns the horizontal length of the sensor
    protected double getSensorLength(HpsSiSensor sensor) {
        double length = 0;

        // Get the faces normal to the sensor
        final List<Polygon3D> faces = ((Box) sensor.getGeometry().getLogicalVolume().getSolid())
                .getFacesNormalTo(new BasicHep3Vector(0, 0, 1));
        for (final Polygon3D face : faces) {
            // Loop through the edges of the sensor face and find the longest one
            final List<LineSegment3D> edges = face.getEdges();
            for (final LineSegment3D edge : edges) {
                if (edge.getLength() > length) {
                    length = edge.getLength();
                }
            }
        }
        return length;
    }

    
 // Returns the horizontal length of the sensor
    protected double getSensorWidth(HpsSiSensor sensor) {
        double length = 0;
        double width = 0; 

        // Get the faces normal to the sensor
        final List<Polygon3D> faces = ((Box) sensor.getGeometry().getLogicalVolume().getSolid())
                .getFacesNormalTo(new BasicHep3Vector(0, 0, 1));
        for (final Polygon3D face : faces) {
            // Loop through the edges of the sensor face and find the longest one
            final List<LineSegment3D> edges = face.getEdges();
            for (final LineSegment3D edge : edges) {
                if (edge.getLength() > length) {
                    length = edge.getLength();
                }
            }
        }
        return length;
    }

    public void endOfData() {
        System.out.println("End of Data. Computing Hit Efficiencies");

        // Setup text files to output efficiencies for each channel
        PrintWriter out = null;
        try {
            out = new PrintWriter(outputFileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        
        // skip this...not useful
        if(1==1) {
            return; 
        }

        // Compute efficiencies and errors as a function of channel, u, and momentum
        for (HpsSiSensor sensor : sensors) {
            String sensorName = sensor.getName();
            int nChan = sensor.getNumberOfChannels();
            for (int i = 0; i < nChan + chanExtd * 2; i++) {
                int chan = i - chanExtd;
                if (numberOfTracksChannel.get(sensorName).binHeight(i) != 0
                        && numberOfTracksChannel.get(sensorName).binHeight(i) != 0) {
                    hitEfficiencyChannel.get(sensorName).fill(chan,
                            numberOfTracksWithHitOnMissingLayerChannel.get(sensorName).binHeight(i)
                            / (double) numberOfTracksChannel.get(sensorName).binHeight(i));
                    hitEfficiencyChannelerr.get(sensorName).fill(chan,
                            Math.sqrt(
                                    1 / (double) numberOfTracksWithHitOnMissingLayerChannel.get(sensorName).binHeight(i)
                                    + 1 / (double) numberOfTracksChannel.get(sensorName).binHeight(i)));
                    /*
                    hitEfficiencyChannelCorrected.get(sensorName).fill(chan,
                            numberOfTracksWithHitOnMissingLayerChannel.get(sensorName).binHeight(i)
                            / (double) numberOfTracksChannelCorrected.get(sensorName).binHeight(i));
                    hitEfficiencyChannelCorrectederr.get(sensorName).fill(chan,
                            Math.sqrt(
                                    1 / (double) numberOfTracksWithHitOnMissingLayerChannel.get(sensorName).binHeight(i)
                                    + 1 / (double) numberOfTracksChannel.get(sensorName).binHeight(i)));
                    */
                }
                if (numberOfTracksChannelEle.get(sensorName).binHeight(i) != 0
                        && numberOfTracksChannelEle.get(sensorName).binHeight(i) != 0) {
                    hitEfficiencyChannelEle.get(sensorName).fill(chan,
                            numberOfTracksWithHitOnMissingLayerChannelEle.get(sensorName).binHeight(i)
                            / (double) numberOfTracksChannelEle.get(sensorName).binHeight(i));
                    hitEfficiencyChannelEleerr.get(sensorName).fill(chan, Math.sqrt(
                            1 / (double) numberOfTracksWithHitOnMissingLayerChannelEle.get(sensorName).binHeight(i)
                            + 1 / (double) numberOfTracksChannelEle.get(sensorName).binHeight(i)));
                    /*
                    hitEfficiencyChannelCorrectedEle.get(sensorName).fill(chan,
                            numberOfTracksWithHitOnMissingLayerChannelEle.get(sensorName).binHeight(i)
                            / (double) numberOfTracksChannelCorrectedEle.get(sensorName).binHeight(i));
                    hitEfficiencyChannelCorrectedEleerr.get(sensorName).fill(chan, Math.sqrt(
                            1 / (double) numberOfTracksWithHitOnMissingLayerChannelEle.get(sensorName).binHeight(i)
                            + 1 / (double) numberOfTracksChannelCorrectedEle.get(sensorName).binHeight(i)));
                    */
                }
                if (numberOfTracksChannelPos.get(sensorName).binHeight(i) != 0
                        && numberOfTracksChannelPos.get(sensorName).binHeight(i) != 0) {
                    hitEfficiencyChannelPos.get(sensorName).fill(chan,
                            numberOfTracksWithHitOnMissingLayerChannelPos.get(sensorName).binHeight(i)
                            / (double) numberOfTracksChannelPos.get(sensorName).binHeight(i));
                    hitEfficiencyChannelPoserr.get(sensorName).fill(chan, Math.sqrt(
                            1 / (double) numberOfTracksWithHitOnMissingLayerChannelPos.get(sensorName).binHeight(i)
                            + 1 / (double) numberOfTracksChannelPos.get(sensorName).binHeight(i)));
                    /*
                    hitEfficiencyChannelCorrectedPos.get(sensorName).fill(chan,
                            numberOfTracksWithHitOnMissingLayerChannelPos.get(sensorName).binHeight(i)
                            / (double) numberOfTracksChannelCorrectedPos.get(sensorName).binHeight(i));
                    hitEfficiencyChannelCorrectedPoserr.get(sensorName).fill(chan, Math.sqrt(
                            1 / (double) numberOfTracksWithHitOnMissingLayerChannelPos.get(sensorName).binHeight(i)
                            + 1 / (double) numberOfTracksChannelCorrectedPos.get(sensorName).binHeight(i)));
                    */
                }
            }
            for (int i = 0; i < nBins; i++) {
                double yMax = sensor.getNumberOfChannels() * sensor.getReadoutStripPitch() / 2;
                double yMin = -yMax;
                double y = (yMax - yMin) / (double) nBins * (i + 0.5) + yMin;
                double pMax = 1.3 * ebeam;
                double pMin = 0;
                double p = (pMax - pMin) / (double) nBins * (i + 0.5) + pMin;
                if (numberOfTracksU.get(sensorName).binHeight(i) != 0
                        && numberOfTracksU.get(sensorName).binHeight(i) != 0) {
                    hitEfficiencyU.get(sensorName).fill(y,
                            numberOfTracksWithHitOnMissingLayerU.get(sensorName).binHeight(i)
                            / (double) numberOfTracksU.get(sensorName).binHeight(i));
                    hitEfficiencyUerr.get(sensorName).fill(y,
                            Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerU.get(sensorName).binHeight(i)
                                    + 1 / (double) numberOfTracksU.get(sensorName).binHeight(i)));
                    /*
                    hitEfficiencyUCorrected.get(sensorName).fill(y,
                            numberOfTracksWithHitOnMissingLayerU.get(sensorName).binHeight(i)
                            / (double) numberOfTracksUCorrected.get(sensorName).binHeight(i));
                    hitEfficiencyUCorrectederr.get(sensorName).fill(y,
                            Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerU.get(sensorName).binHeight(i)
                                    + 1 / (double) numberOfTracksUCorrected.get(sensorName).binHeight(i)));
                    */
                }
                if (numberOfTracksUEle.get(sensorName).binHeight(i) != 0
                        && numberOfTracksUEle.get(sensorName).binHeight(i) != 0) {
                    hitEfficiencyUEle.get(sensorName).fill(y,
                            numberOfTracksWithHitOnMissingLayerUEle.get(sensorName).binHeight(i)
                            / (double) numberOfTracksUEle.get(sensorName).binHeight(i));
                    hitEfficiencyUEleerr.get(sensorName).fill(y,
                            Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerUEle.get(sensorName).binHeight(i)
                                    + 1 / (double) numberOfTracksUEle.get(sensorName).binHeight(i)));
                    /*
                      hitEfficiencyUCorrectedEle.get(sensorName).fill(y,
                            numberOfTracksWithHitOnMissingLayerUEle.get(sensorName).binHeight(i)
                            / (double) numberOfTracksUCorrectedEle.get(sensorName).binHeight(i));
                    hitEfficiencyUCorrectedEleerr.get(sensorName).fill(y,
                            Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerUEle.get(sensorName).binHeight(i)
                                    + 1 / (double) numberOfTracksUCorrectedEle.get(sensorName).binHeight(i)));
                    */
                }
                if (numberOfTracksUPos.get(sensorName).binHeight(i) != 0
                        && numberOfTracksUPos.get(sensorName).binHeight(i) != 0) {
                    hitEfficiencyUPos.get(sensorName).fill(y,
                            numberOfTracksWithHitOnMissingLayerUPos.get(sensorName).binHeight(i)
                            / (double) numberOfTracksUPos.get(sensorName).binHeight(i));
                    hitEfficiencyUPoserr.get(sensorName).fill(y,
                            Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerUPos.get(sensorName).binHeight(i)
                                    + 1 / (double) numberOfTracksUPos.get(sensorName).binHeight(i)));
                    /*
                    hitEfficiencyUCorrectedPos.get(sensorName).fill(y,
                            numberOfTracksWithHitOnMissingLayerUPos.get(sensorName).binHeight(i)
                            / (double) numberOfTracksUCorrectedPos.get(sensorName).binHeight(i));
                    hitEfficiencyUCorrectedPoserr.get(sensorName).fill(y,
                            Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerUPos.get(sensorName).binHeight(i)
                                    + 1 / (double) numberOfTracksUCorrectedPos.get(sensorName).binHeight(i)));
                    */
                }
                if (numberOfTracksP.get(sensorName).binHeight(i) != 0
                        && numberOfTracksP.get(sensorName).binHeight(i) != 0) {
                    hitEfficiencyP.get(sensorName).fill(p,
                            numberOfTracksWithHitOnMissingLayerP.get(sensorName).binHeight(i)
                            / (double) numberOfTracksP.get(sensorName).binHeight(i));
                    hitEfficiencyPerr.get(sensorName).fill(p,
                            Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerP.get(sensorName).binHeight(i)
                                    + 1 / (double) numberOfTracksP.get(sensorName).binHeight(i)));
                    /*
                    hitEfficiencyPCorrected.get(sensorName).fill(p,
                            numberOfTracksWithHitOnMissingLayerP.get(sensorName).binHeight(i)
                            / (double) numberOfTracksPCorrected.get(sensorName).binHeight(i));
                    hitEfficiencyPCorrectederr.get(sensorName).fill(p,
                            Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerP.get(sensorName).binHeight(i)
                                    + 1 / (double) numberOfTracksPCorrected.get(sensorName).binHeight(i)));
                    */
                }
                if (numberOfTracksPEle.get(sensorName).binHeight(i) != 0
                        && numberOfTracksPEle.get(sensorName).binHeight(i) != 0) {
                    hitEfficiencyPEle.get(sensorName).fill(p,
                            numberOfTracksWithHitOnMissingLayerPEle.get(sensorName).binHeight(i)
                            / (double) numberOfTracksPEle.get(sensorName).binHeight(i));
                    hitEfficiencyPEleerr.get(sensorName).fill(p,
                            Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerPEle.get(sensorName).binHeight(i)
                                    + 1 / (double) numberOfTracksPEle.get(sensorName).binHeight(i)));
                    /*
                    hitEfficiencyPCorrectedEle.get(sensorName).fill(p,
                            numberOfTracksWithHitOnMissingLayerPEle.get(sensorName).binHeight(i)
                            / (double) numberOfTracksPCorrectedEle.get(sensorName).binHeight(i));
                    hitEfficiencyPCorrectedEleerr.get(sensorName).fill(p,
                            Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerPEle.get(sensorName).binHeight(i)
                                    + 1 / (double) numberOfTracksPCorrectedEle.get(sensorName).binHeight(i)));
                    */
                }
                if (numberOfTracksPPos.get(sensorName).binHeight(i) != 0
                        && numberOfTracksPPos.get(sensorName).binHeight(i) != 0) {
                    hitEfficiencyPPos.get(sensorName).fill(p,
                            numberOfTracksWithHitOnMissingLayerPPos.get(sensorName).binHeight(i)
                            / (double) numberOfTracksPPos.get(sensorName).binHeight(i));
                    hitEfficiencyPPoserr.get(sensorName).fill(p,
                            Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerPPos.get(sensorName).binHeight(i)
                                    + 1 / (double) numberOfTracksPPos.get(sensorName).binHeight(i)));
                    /*
                    hitEfficiencyPCorrectedPos.get(sensorName).fill(p,
                            numberOfTracksWithHitOnMissingLayerPPos.get(sensorName).binHeight(i)
                            / (double) numberOfTracksPCorrectedPos.get(sensorName).binHeight(i));
                    hitEfficiencyPCorrectedPoserr.get(sensorName).fill(p,
                            Math.sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerPPos.get(sensorName).binHeight(i)
                                    + 1 / (double) numberOfTracksPCorrectedPos.get(sensorName).binHeight(i)));
                    */
                }
            }
            double totalEff = 0;
            double totalEffEle = 0;
            double totalEffPos = 0;
            double totalEfferr = 0;
            double totalEffEleerr = 0;
            double totalEffPoserr = 0;
            /*
            double totalEffCorrected = 0;
            double totalEffEleCorrected = 0;
            double totalEffPosCorrected = 0;
            double totalEfferrCorrected = 0;
            double totalEffEleerrCorrected = 0;
            double totalEffPoserrCorrected = 0;
            */
            // Calculate total efficiencies for each sensor
            if (numberOfTracksChannel.get(sensorName).sumAllBinHeights() != 0
                    && numberOfTracksChannel.get(sensorName).sumAllBinHeights() != 0) {
                totalEff = numberOfTracksWithHitOnMissingLayerChannel.get(sensorName).sumAllBinHeights()
                        / (double) numberOfTracksChannel.get(sensorName).sumAllBinHeights();
                totalEfferr = Math
                        .sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerChannel.get(sensorName).sumAllBinHeights()
                                + 1 / (double) numberOfTracksChannel.get(sensorName).sumAllBinHeights());
                /*
                  totalEffCorrected = numberOfTracksWithHitOnMissingLayerChannel.get(sensorName).sumAllBinHeights()
                        / (double) numberOfTracksChannelCorrected.get(sensorName).sumAllBinHeights();
                totalEfferrCorrected = Math
                        .sqrt(1 / (double) numberOfTracksWithHitOnMissingLayerChannel.get(sensorName).sumAllBinHeights()
                                + 1 / (double) numberOfTracksChannelCorrected.get(sensorName).sumAllBinHeights());
                */
            }
            if (numberOfTracksChannelEle.get(sensorName).sumAllBinHeights() != 0
                    && numberOfTracksChannelEle.get(sensorName).sumAllBinHeights() != 0) {
                totalEffEle = numberOfTracksWithHitOnMissingLayerChannelEle.get(sensorName).sumAllBinHeights()
                        / (double) numberOfTracksChannelEle.get(sensorName).sumAllBinHeights();
                totalEffEleerr = Math.sqrt(
                        1 / (double) numberOfTracksWithHitOnMissingLayerChannelEle.get(sensorName).sumAllBinHeights()
                        + 1 / (double) numberOfTracksChannelEle.get(sensorName).sumAllBinHeights());
                /*
                totalEffEleCorrected = numberOfTracksWithHitOnMissingLayerChannelEle.get(sensorName).sumAllBinHeights()
                        / (double) numberOfTracksChannelCorrectedEle.get(sensorName).sumAllBinHeights();
                totalEffEleerrCorrected = Math.sqrt(
                        1 / (double) numberOfTracksWithHitOnMissingLayerChannelEle.get(sensorName).sumAllBinHeights()
                        + 1 / (double) numberOfTracksChannelCorrectedEle.get(sensorName).sumAllBinHeights());
                */
            }
            if (numberOfTracksChannelPos.get(sensorName).sumAllBinHeights() != 0
                    && numberOfTracksChannelPos.get(sensorName).sumAllBinHeights() != 0) {
                totalEffPos = numberOfTracksWithHitOnMissingLayerChannelPos.get(sensorName).sumAllBinHeights()
                        / (double) numberOfTracksChannelPos.get(sensorName).sumAllBinHeights();
                totalEffPoserr = Math.sqrt(
                        1 / (double) numberOfTracksWithHitOnMissingLayerChannelPos.get(sensorName).sumAllBinHeights()
                        + 1 / (double) numberOfTracksChannelPos.get(sensorName).sumAllBinHeights());
                /*
                totalEffPosCorrected = numberOfTracksWithHitOnMissingLayerChannelPos.get(sensorName).sumAllBinHeights()
                        / (double) numberOfTracksChannelCorrectedPos.get(sensorName).sumAllBinHeights();
                totalEffPoserrCorrected = Math.sqrt(
                        1 / (double) numberOfTracksWithHitOnMissingLayerChannelPos.get(sensorName).sumAllBinHeights()
                        + 1 / (double) numberOfTracksChannelCorrectedPos.get(sensorName).sumAllBinHeights());
                */
            }

            TotalEff.get(sensorName).fill(0, totalEff);
            TotalEffEle.get(sensorName).fill(0, totalEffEle);
            TotalEffPos.get(sensorName).fill(0, totalEffPos);
            TotalEfferr.get(sensorName).fill(0, totalEfferr);
            TotalEffEleerr.get(sensorName).fill(0, totalEffEleerr);
            TotalEffPoserr.get(sensorName).fill(0, totalEffPoserr);
            /*
            TotalCorrectedEff.get(sensorName).fill(0, totalEffCorrected);
            TotalCorrectedEffEle.get(sensorName).fill(0, totalEffEleCorrected);
            TotalCorrectedEffPos.get(sensorName).fill(0, totalEffPosCorrected);
            TotalCorrectedEfferr.get(sensorName).fill(0, totalEfferrCorrected);
            TotalCorrectedEffEleerr.get(sensorName).fill(0, totalEffEleerrCorrected);
            TotalCorrectedEffPoserr.get(sensorName).fill(0, totalEffPoserrCorrected);
            */
            // Print out efficiency and corrected efficiency for each sensor with error
            System.out.println(sensorName + " Total Efficiency = " + totalEff + " with error " + totalEfferr);
            //            System.out.println(sensorName + " Total Corrected Efficiency = " + totalEffCorrected + " with error "
            //                    + totalEfferrCorrected);

            // Output efficiencies as a function of channel in a text file
            org.hps.util.Pair<Integer, Integer> daqPair = getDaqPair(daqMap, sensor);
            Collection<SvtChannel> channels = channelMap.find(daqPair);
            for (SvtChannel channel : channels) {
                int chanID = channel.getChannelID();
                int chan = channel.getChannel();
                if (chan < hitEfficiencyChannel.get(sensorName).axis().bins()) {
                    double eff = hitEfficiencyChannel.get(sensorName).binHeight(chan);
                    //System.out.println(chanID + ", " + eff);
                    out.println(chanID + ", " + eff);
                }
            }
        }
        out.close();
    }

    static org.hps.util.Pair<Integer, Integer> getDaqPair(SvtDaqMappingCollection daqMap, HpsSiSensor sensor) {

        final String svtHalf = sensor.isTopLayer() ? AbstractSvtDaqMapping.TOP_HALF : AbstractSvtDaqMapping.BOTTOM_HALF;
        for (final SvtDaqMapping object : daqMap) {
            if (svtHalf.equals(object.getSvtHalf()) && object.getLayerNumber() == sensor.getLayerNumber()
                    && object.getSide().equals(sensor.getSide())) {
                return new org.hps.util.Pair<Integer, Integer>(object.getFebID(), object.getFebHybridID());
            }
        }
        return null;
    }
}
