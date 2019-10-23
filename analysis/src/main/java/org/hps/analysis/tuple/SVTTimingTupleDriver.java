package org.hps.analysis.tuple;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.ecal.cluster.TriggerTime;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.recon.tracking.ShapeFitParameters;
import org.hps.recon.tracking.TrackData;
import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackerHitUtils;
import org.hps.recon.utils.TrackClusterMatcher;
import org.hps.record.triggerbank.SSPData;
import org.lcsim.detector.converter.compact.subdetector.SvtStereoLayer;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.Cluster;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;

// driver for investigating the SVT timing (from single strips, strip clusters, 3d hits, and tracks)
// each entry in the tuple is a single track
public class SVTTimingTupleDriver extends MCTupleMaker {

    private static Logger LOGGER = Logger.getLogger(SVTTimingTupleDriver.class.getPackage().getName());

    String finalStateParticlesColName = "FinalStateParticles";
    String mcParticlesColName = "MCParticle";
    String readoutHitCollectionName = "EcalReadoutHits";//these are in ADC counts
    String calibratedHitCollectionName = "EcalCalHits";//these are in energy
    String clusterCollectionName = "EcalClustersCorr";
    private String helicalTrackHitCollectionName = "HelicalTrackHits";
    private String rotatedTrackHitCollectionName = "RotatedHelicalTrackHits";
    private String unconstrainedV0CandidatesColName = "UnconstrainedV0Candidates";
    private String beamConV0CandidatesColName = "BeamspotConstrainedV0Candidates";
    private String targetV0ConCandidatesColName = "TargetConstrainedV0Candidates";
    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    private String fittedTrackerHitCollectionName = "SVTFittedRawTrackerHits";
    private String trackerHitCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    String[] fpQuantNames = {"nEle_per_Event", "nPos_per_Event", "nPhoton_per_Event", "nUnAssociatedTracks_per_Event", "avg_delX_at_ECal", "avg_delY_at_ECal", "avg_E_Over_P", "avg_mom_beam_elec", "sig_mom_beam_elec"};
    private TrackClusterMatcher matcher = new TrackClusterMatcher();
    //private Map<SiSensor, Map<Integer, Hep3Vector>> stripPositions = new HashMap<SiSensor, Map<Integer, Hep3Vector>>(); 
    private List<HpsSiSensor> sensors = null;
    private Map<Integer, List<SvtStereoLayer>> topStereoLayers = new HashMap<Integer, List<SvtStereoLayer>>();
    private Map<Integer, List<SvtStereoLayer>> bottomStereoLayers = new HashMap<Integer, List<SvtStereoLayer>>();
    // Constants
    public static final double SENSOR_LENGTH = 98.33; // mm
    public static final double SENSOR_WIDTH = 38.3399; // mm
    private static final String SUBDETECTOR_NAME = "Tracker";
    boolean doSkim = false;
    TrackerHitUtils trackerHitUtils = new TrackerHitUtils();
    //some counters
    int nRecoEvents = 0;
    int nTotEle = 0;
    int nTotPos = 0;
    int nTotPhotons = 0;
    int nTotUnAss = 0;
    int nTotAss = 0;
    //some summers
    double sumdelX = 0.0;
    double sumdelY = 0.0;
    double sumEoverP = 0.0;
    // double beamEnergy = 1.05; //GeV
    double clTimeMin = 30;//ns
    double clTimeMax = 50;//ns
    double deltaTimeMax = 4;//ns
    double coplanMean = 180;//degrees
    double coplanWidth = 10;//degrees
    double esumMin = 0.4;
    double esumMax = 1.2;
    double phot_nom_x = 42.52;//nominal photon position (px=0)
    boolean requirePositron = false;
    double maxPairs = 5;
    boolean requireSuperFiducial = false;
    int nbins = 50;
    double B_FIELD = 0.23;//Tesla

    protected final Map<Integer, String> layerMap = new HashMap<Integer, String>();

    double[] hthHitTime = new double[6];
    double[] SiClHitTime = new double[12];
    double[] SiHitClHitTime = new double[12];

    protected double[] beamSize = {0.001, 0.130, 0.050}; //rough estimate from harp scans during engineering run production running
    // Beam position variables.
    // The beamPosition array is in the tracking frame
    /* TODO get the beam position from the conditions db */
    protected double[] beamPosition = {0.0, 0.0, 0.0}; //

    double minPhi = -0.25;
    double maxPhi = 0.25;
    boolean isFirst = true;

    @Override
    boolean passesCuts() {
        return true;
    }

    private enum Constraint {

        /**
         * Represents a fit with no constraints.
         */
        UNCONSTRAINED,
        /**
         * Represents a fit with beam spot constraints.
         */
        BS_CONSTRAINED,
        /**
         * Represents a fit with target constraints.
         */
        TARGET_CONSTRAINED
    }

    String detName;

    /**
     * The B field map
     */
    FieldMap bFieldMap = null;
    /**
     * Position of the Ecal face
     */
    private double ecalPosition = 0; // mm

    /**
     * Z position to start extrapolation from
     */
    double extStartPos = 700; // mm

    /**
     * The extrapolation step size
     */
    double stepSize = 5.0; // mm
    /**
     * Name of the constant denoting the position of the Ecal face in the
     * compact description.
     */
    private static final String ECAL_POSITION_CONSTANT_NAME = "ecal_dface";

    @Override
    protected void setupVariables() {
        tupleVariables.clear();
        String[] mcVars = new String[]{"apMass/D", "vtxXMC/D", "vtxYMC/D", "vtxZMC/D",
            "pEleXMC/D", "pEleYMC/D", "pEleZMC/D",
            "pPosXMC/D", "pPosYMC/D", "pPosZMC/D",
            "pApXMC/D", "pApYMC/D", "pApZMC/D",
            "apTarX/D", "apTarY/D", "apOrigX/D", "apOrigY/D", "apOrigZ/D"};
        tupleVariables.addAll(Arrays.asList(mcVars));

        String[] StripHitVars = new String[]{"StrL1aTime/D", "StrL1sTime/D", "StrL2aTime/D", "StrL2sTime/D", "StrL3aTime/D", "StrL3sTime/D",
            "StrL4aTime/D", "StrL4sTime/D", "StrL5aTime/D", "StrL5sTime/D", "StrL6aTime/D", "StrL6sTime/D",
            "StrL1aChiSq/D", "StrL1sChiSq/D", "StrL2aChiSq/D", "StrL2sChiSq/D", "StrL3aChiSq/D", "StrL3sChiSq/D",
            "StrL4aChiSq/D", "StrL4sChiSq/D", "StrL5aChiSq/D", "StrL5sChiSq/D", "StrL6aChiSq/D", "StrL6sChiSq/D",
            "StrL1aAmp/D", "StrL1sAmp/D", "StrL2aAmp/D", "StrL2sAmp/D", "StrL3aAmp/D", "StrL3sAmp/D",
            "StrL4aAmp/D", "StrL4sAmp/D", "StrL5aAmp/D", "StrL5sAmp/D", "StrL6aAmp/D", "StrL6sAmp/D"};
//         "StrL4aToBoSlHo/b", "StrL4sToBoSlHo/b",
//            "StrL5aToBoSlHo/b", "StrL5sToBoSlHo/b", "StrL6aToBoSlHo/b", "StrL6sToBoSlHo/b"};
        tupleVariables.addAll(Arrays.asList(StripHitVars));

        String[] SiClusterHitVars = new String[]{"ClL1aTime/D", "ClL1sTime/D", "ClL2aTime/D", "ClL2sTime/D", "ClL3aTime/D", "ClL3sTime/D",
            "ClL4aTime/D", "ClL4sTime/D", "ClL5aTime/D", "ClL5sTime/D", "ClL6aTime/D", "ClL6sTime/D", "ClL1aNHit/I", "ClL1sNHit/I",
            "ClL2aNHit/I", "ClL2sNHit/I", "ClL3aNHit/I", "ClL3sNHit/I", "ClL4aNHit/I", "ClL4sNHit/I", "ClL5aNHit/I", "ClL5sNHit/I",
            "ClL6aNHit/I", "ClL6sNHit/I",};
        tupleVariables.addAll(Arrays.asList(SiClusterHitVars));

        String[] ThreeDClusterHitVars = new String[]{"Cl3dL1Time/D", "Cl3dL2Time/D", "Cl3dL3Time/D",
            "Cl3dL4Time/D", "Cl3dL5Time/D", "Cl3dL6Time/D"};
        tupleVariables.addAll(Arrays.asList(ThreeDClusterHitVars));

        String[] TrackVars = new String[]{"TrTime/D", "TrPx/D", "TrCharge/I", "TrPy/D", "TrPz/D",
            "TrOmegaErr/D", "TrD0Err/D", "TrPhi0Err/D", "TrSlopeErr/D", "TrZ0Err/D",
            "TrOmega/D", "TrD0/D", "TrPhi0/D", "TrSlope/D", "TrZ0/D", "ECalClTime"};
        tupleVariables.addAll(Arrays.asList(TrackVars));

        String[] EventVars = new String[]{"rfT1/D", "rfT1/D", "TrigTime/D", "RFJitter/D"};
        tupleVariables.addAll(Arrays.asList(EventVars));

        addEventVariables();
        addVertexVariables();
        addParticleVariables("ele");
        addParticleVariables("pos");
        String[] newVars = new String[]{"minPositiveIso/D", "minNegativeIso/D", "minIso/D"};
        tupleVariables.addAll(Arrays.asList(newVars));
        addMCTridentVariables();

    }

    protected void detectorChanged(Detector detector) {

        super.detectorChanged(detector);
        layerMap.put(1, "L1");
        layerMap.put(2, "L2");
        layerMap.put(3, "L3");
        layerMap.put(4, "L4");
        layerMap.put(5, "L5");
        layerMap.put(6, "L6");
        detName = detector.getName();
        double maxFactor = 1.5;
        double feeMomentumCut = 0.75; //this number, multiplied by the beam energy, is the actual cut
        B_FIELD = detector.getFieldMap().getField(new BasicHep3Vector(0, 0, 500)).y();
        // Get the field map from the detector object
        bFieldMap = detector.getFieldMap();
        // Get the position of the Ecal from the compact description
        ecalPosition = detector.getConstants().get(ECAL_POSITION_CONSTANT_NAME).getValue();
        // Get the position of the Ecal from the compact description
        ecalPosition = detector.getConstants().get(ECAL_POSITION_CONSTANT_NAME).getValue();
        LOGGER.setLevel(Level.ALL);
        LOGGER.info("B_FIELD=" + B_FIELD);
        LOGGER.info("Setting up the plotter");

    }

    @Override
    public void process(EventHeader event) {

        if (!event.hasCollection(LCRelation.class, fittedTrackerHitCollectionName)) {
            System.out.println("No fitted hits???");
            return;
        }

        List<LCRelation> fittedTrackerHits = event.get(LCRelation.class, fittedTrackerHitCollectionName);
        RelationalTable rthtofit = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
        for (LCRelation hit : fittedTrackerHits)
            rthtofit.add(FittedRawTrackerHit.getRawTrackerHit(hit), FittedRawTrackerHit.getShapeFitParameters(hit));

        TIData triggerData = null;
        if (event.hasCollection(GenericObject.class, "TriggerBank"))
            for (GenericObject data : event.get(GenericObject.class, "TriggerBank"))
                if (AbstractIntData.getTag(data) == TIData.BANK_TAG)
                    triggerData = new TIData(data);

        if (!event.hasCollection(TrackData.class, TrackData.TRACK_DATA_COLLECTION)
                || !event.hasCollection(LCRelation.class, TrackData.TRACK_DATA_RELATION_COLLECTION)) {
            System.out.println("No TrackData");
            return;
        }

//
//        List<TrackData> trkDataList = (List<TrackData>) event.get(TrackData.class, TrackData.TRACK_DATA_COLLECTION);
//        List<LCRelation> trkDataRelationList = (List<LCRelation>) event.get(LCRelation.class, TrackData.TRACK_DATA_RELATION_COLLECTION);
////
//        if (!event.hasCollection(ReconstructedParticle.class, finalStateParticlesColName)) {
//            if (debug)
//                LOGGER.info(finalStateParticlesColName + " collection not found???");
//            return;
//        }
//
//        if (!event.hasCollection(MCParticle.class, mcParticlesColName)) {
//            if (debug)
//                LOGGER.info(mcParticlesColName + " collection not found???");
//            return;
//        }
        //check to see if this event is from the correct trigger (or "all");
        if (triggerData != null && !matchTriggerType(triggerData))
            return;

        nRecoEvents++;
        if (debug)
            LOGGER.info("##########  Start of SVTTimngTuple   ##############");
        List<ReconstructedParticle> finalStateParticles = event.get(ReconstructedParticle.class, finalStateParticlesColName);
        if (debug)
            LOGGER.info("This events has " + finalStateParticles.size() + " final state particles");
        List<ReconstructedParticle> unconstrainedV0List = event.get(ReconstructedParticle.class, unconstrainedV0CandidatesColName);
        if (debug)
            LOGGER.info("This events has " + unconstrainedV0List.size() + " unconstrained V0s");
        List<ReconstructedParticle> BSCV0List = event.get(ReconstructedParticle.class, beamConV0CandidatesColName);
        if (debug)
            LOGGER.info("This events has " + BSCV0List.size() + " BSC V0s");

        if (event.hasCollection(MCParticle.class, mcParticlesColName)) {
            List<MCParticle> MCParticleList = event.get(MCParticle.class, mcParticlesColName);

            //find electron and positron MCParticles
            Hep3Vector vertexPositionMC = null;
            double apMassMC = -9999;
            MCParticle ap = null;
            MCParticle eleMC = null;
            MCParticle posMC = null;

            for (MCParticle mcp : MCParticleList)
                if (mcp.getPDGID() == 622 && mcp.getDaughters().size() == 2) {
                    ap = mcp;
                    vertexPositionMC = mcp.getEndPoint();
                    apMassMC = mcp.getMass();
                    List<MCParticle> daugList = mcp.getDaughters();
                    for (MCParticle daug : daugList)
                        if (daug.getPDGID() == 11)
                            eleMC = daug;
                        else if (daug.getPDGID() == -11)
                            posMC = daug;
                }
        }
//        if (eleMC == null || posMC == null) {
//            if (debug)
//                LOGGER.info("Couldn't find the MC e+e- from A'?????  Quitting.");
//            return;
//        }
        double bestMom = 99999;
        int layer;
        String layerLabel;
        String typeLabel;
        String axStLabel;
        String tString;
        System.out.println("Event has " + finalStateParticles.size() + " final state particles");
        for (ReconstructedParticle fsp : finalStateParticles) {
            tupleMap.clear();
            //if (!TrackType.isGBL(fsp.getType())) // we only care about GBL vertices

            if (event.hasCollection(GenericObject.class, "TriggerBank")) {
                List<GenericObject> triggerList = event.get(GenericObject.class, "TriggerBank");
                for (GenericObject data : triggerList)
                    if (AbstractIntData.getTag(data) == SSPData.BANK_TAG) {
                        SSPData sspData = new SSPData(data);
                        double trigTime = sspData.getTime();
                        tupleMap.put("TrigTime/D", trigTime);
                    }
            }

            if (event.hasCollection(GenericObject.class, "RFHits")) {
                List<GenericObject> rfTimes = event.get(GenericObject.class, "RFHits");
                if (rfTimes.size() > 0) {
                    tupleMap.put("rfT1/D", rfTimes.get(0).getDoubleVal(0));
                    tupleMap.put("rfT2/D", rfTimes.get(0).getDoubleVal(1));
                }
            }
            //does the track have a matched cluster?              
            System.out.println("# clusters = " + fsp.getClusters().size());
            System.out.println("# tracks = " + fsp.getTracks().size());
            if (fsp.getTracks().size() != 1)
                continue;

            if (!TrackType.isGBL(fsp.getTracks().get(0).getType())) // we only care about GBL tracks
                continue;
            System.out.println("Found GBL Track");
//            System.out.println("Got a cluster and track");
            Track trk = fsp.getTracks().get(0);
            if (trk.getTrackerHits().size() != 6)
                continue;
            System.out.println("Got 6-hit track");
            if (fsp.getClusters().size() == 1) {
                Cluster clu = fsp.getClusters().get(0);
                double cltime = ClusterUtilities.findSeedHit(clu).getTime();
                tupleMap.put("ECalClTime", cltime);
            }
            //include track stuff
            typeLabel = "Tr";
            //   TrackData trkData = (TrackData) TrackData.getTrackData(event, trk);
            double trkTime = TrackData.getTrackTime(TrackData.getTrackData(event, trk));
            tString = typeLabel + "Time/D";
            tupleMap.put(tString, trkTime);
            double trkMom = fsp.getMomentum().z();
            tString = typeLabel + "Pz/D";
            tupleMap.put(tString, trkMom);
            trkMom = fsp.getMomentum().x();
            tString = typeLabel + "Px/D";
            tupleMap.put(tString, trkMom);
            trkMom = fsp.getMomentum().y();
            tString = typeLabel + "Py/D";
            tupleMap.put(tString, trkMom);
            double trkCharge = fsp.getCharge();
            tString = typeLabel + "Pz/I";
            tupleMap.put(tString, trkCharge);

            List<TrackerHit> trHits = trk.getTrackerHits();
            for (TrackerHit trHit : trHits) {

                HelicalTrackCross hth = (HelicalTrackCross) trHit; //need to run full recon for this!!!
                //stuff with 3d hits
                typeLabel = "Cl3d";
                layer = moduleNumber(hth.Layer());
                layerLabel = layerMap.get(layer);
                double hthTime = hth.getTime();
                tString = typeLabel + layerLabel + "Time/D";
                tupleMap.put(tString, hthTime);
                //                
                List<HelicalTrackStrip> htsList = hth.getStrips();
                for (HelicalTrackStrip hts : htsList) {
                    //do strip cluster stuff
                    typeLabel = "Cl";
                    layer = moduleNumber(hts.layer());
                    layerLabel = layerMap.get(layer);

                    List<RawTrackerHit> rthList = hts.rawhits();
                    RawTrackerHit rthDummy = (RawTrackerHit) hts.rawhits().get(0);
                    HpsSiSensor sensor = ((HpsSiSensor) rthDummy.getDetectorElement());

                    axStLabel = "s";
                    if (sensor.isAxial())
                        axStLabel = "a";
                    tString = typeLabel + layerLabel + axStLabel + "Time/D";
                    double htsTime = hts.time();
                    tupleMap.put(tString, htsTime);
                    double htsNHit = hts.rawhits().size();
                    tString = typeLabel + layerLabel + axStLabel + "NHit/D";
                    tupleMap.put(tString, htsNHit);
                    //  
                    RawTrackerHit rthSeed = null;
                    GenericObject parsSeed = null;
                    double peak = -99999;
                    for (RawTrackerHit rth : rthList) {
                        GenericObject pars = (GenericObject) rthtofit.to(rth);
                        // get the highest amplitude hit

                        double amp = ShapeFitParameters.getAmp(pars);
                        if (amp > peak) {
                            peak = amp;
                            parsSeed = pars;
                            rthSeed = rth;
                        }
                    }
                    //found the seed; now do strip hit stuff
                    typeLabel = "Str";
                    double t0 = ShapeFitParameters.getT0(parsSeed);
                    double amp = ShapeFitParameters.getAmp(parsSeed);
                    double chiProb = ShapeFitParameters.getChiProb(parsSeed);
                    tString = typeLabel + layerLabel + axStLabel + "Time/D";
                    tupleMap.put(tString, t0);
                    tString = typeLabel + layerLabel + axStLabel + "Amp/D";
                    tupleMap.put(tString, amp);
                    tString = typeLabel + layerLabel + axStLabel + "ChiSq/D";
                    tupleMap.put(tString, chiProb);
                }
            }

            if (event.hasCollection(TriggerTime.class, "TriggerTime")) {
                if (debug)
                    System.out.println("Getting TriggerTime Object");
                List<TriggerTime> jitterList = event.get(TriggerTime.class, "TriggerTime");
                if (debug)
                    System.out.println("TriggerTime List Size = " + jitterList.size());
                TriggerTime jitterObject = jitterList.get(0);
                double jitter = jitterObject.getDoubleVal();
                if (debug)
                    System.out.println("RF time jitter " + jitter);
                tupleMap.put("RFJitter/D", jitter);

            } else
                System.out.println("Requested RF Time correction but TriggerTime Collection doesn't exist!!!");
            if (tupleWriter != null) {
                System.out.println("writing tuple");
                writeTuple();
            }
        }

    }

    private int moduleNumber(int layer) {
        return (int) (layer + 1) / 2;
    }

}
