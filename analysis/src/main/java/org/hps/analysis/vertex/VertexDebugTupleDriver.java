package org.hps.analysis.vertex;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.hps.analysis.tuple.TupleDriver;
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.TrackerHitUtils;
import org.hps.recon.utils.TrackClusterMatcher;
import org.hps.recon.vertexing.BilliorTrack;
import org.hps.recon.vertexing.BilliorVertex;
import org.hps.recon.vertexing.BilliorVertexer;
import org.lcsim.detector.converter.compact.subdetector.SvtStereoLayer;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.LCIOParameters.ParameterName;
import org.lcsim.event.MCParticle;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelixParamCalculator;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;

public class VertexDebugTupleDriver extends TupleDriver {

    private static Logger LOGGER = Logger.getLogger(VertexDebugger.class.getPackage().getName());

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

    protected double[] beamSize = {0.001, 0.130, 0.050}; //rough estimate from harp scans during engineering run production running
    // Beam position variables.
    // The beamPosition array is in the tracking frame
    /* TODO get the beam position from the conditions db */
    protected double[] beamPosition = {-5.0, 0.0, 0.0}; //

    double minPhi = -0.25;
    double maxPhi = 0.25;

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
            "pPosXMC/D", "pPosYMC/D", "pPosZMC/D"};
        tupleVariables.addAll(Arrays.asList(mcVars));

        String[] fitUncVars = new String[]{"mFitUnc/D", "vtxXFitUnc/D", "vtxYFitUnc/D", "vtxZFitUnc/D",
            "pEleXFitUnc/D", "pEleYFitUnc/D", "pEleZFitUnc/D",
            "pPosXFitUnc/D", "pPosYFitUnc/D", "pPosZFitUnc/D", "chisqFitUnc/D",
            "mErrFitUnc/D", "vtxXErrFitUnc/D", "vtxYErrFitUnc/D", "vtxZErrFitUnc/D",
            "pEleXErrFitUnc/D", "pEleYErrFitUnc/D", "pEleZErrFitUnc/D",
            "pPosXErrFitUnc/D", "pPosYErrFitUnc/D", "pPosZErrFitUnc/D"};
        tupleVariables.addAll(Arrays.asList(fitUncVars));

        String[] refitUncVars = new String[]{"mRefitUnc/D", "vtxXRefitUnc/D", "vtxYRefitUnc/D", "vtxZRefitUnc/D",
            "pEleXRefitUnc/D", "pEleYRefitUnc/D", "pEleZRefitUnc/D",
            "pPosXRefitUnc/D", "pPosYRefitUnc/D", "pPosZRefitUnc/D", "chisqRefitUnc/D",
            "mErrRefitUnc/D", "vtxXErrRefitUnc/D", "vtxYErrRefitUnc/D", "vtxZErrRefitUnc/D",
            "pEleXErrRefitUnc/D", "pEleYErrRefitUnc/D", "pEleZErrRefitUnc/D",
            "pPosXErrRefitUnc/D", "pPosYErrRefitUnc/D", "pPosZErrRefitUnc/D"};
        tupleVariables.addAll(Arrays.asList(refitUncVars));

        String[] refitUncFromV0Vars = new String[]{"mRefitUncFromV0/D", "vtxXRefitUncFromV0/D", "vtxYRefitUncFromV0/D", "vtxZRefitUncFromV0/D",
            "pEleXRefitUncFromV0/D", "pEleYRefitUncFromV0/D", "pEleZRefitUncFromV0/D",
            "pPosXRefitUncFromV0/D", "pPosYRefitUncFromV0/D", "pPosZRefitUncFromV0/D", "chisqRefitUncFromV0/D",
            "mErrRefitUncFromV0/D", "vtxXErrRefitUncFromV0/D", "vtxYErrRefitUncFromV0/D", "vtxZErrRefitUncFromV0/D",
            "pEleXErrRefitUncFromV0/D", "pEleYErrRefitUncFromV0/D", "pEleZErrRefitUncFromV0/D",
            "pPosXErrRefitUncFromV0/D", "pPosYErrRefitUncFromV0/D", "pPosZErrRefitUncFromV0/D"};
        tupleVariables.addAll(Arrays.asList(refitUncFromV0Vars));

        String[] fitBSCVars = new String[]{"mFitBSC/D", "vtxXFitBSC/D", "vtxYFitBSC/D", "vtxZFitBSC/D",
            "pEleXFitBSC/D", "pEleYFitBSC/D", "pEleZFitBSC/D",
            "pPosXFitBSC/D", "pPosYFitBSC/D", "pPosZFitBSC/D", "chisqFitBSC/D",
            "mErrFitBSC/D", "vtxXErrFitBSC/D", "vtxYErrFitBSC/D", "vtxZErrFitBSC/D",
            "pEleXErrFitBSC/D", "pEleYErrFitBSC/D", "pEleZErrFitBSC/D",
            "pPosXErrFitBSC/D", "pPosYErrFitBSC/D", "pPosZErrFitBSC/D"};
        tupleVariables.addAll(Arrays.asList(fitBSCVars));

        String[] refitBSCVars = new String[]{"mRefitBSC/D", "vtxXRefitBSC/D", "vtxYRefitBSC/D", "vtxZRefitBSC/D",
            "pEleXRefitBSC/D", "pEleYRefitBSC/D", "pEleZRefitBSC/D",
            "pPosXRefitBSC/D", "pPosYRefitBSC/D", "pPosZRefitBSC/D", "chisqRefitBSC/D",
            "mErrRefitBSC/D", "vtxXErrRefitBSC/D", "vtxYErrRefitBSC/D", "vtxZErrRefitBSC/D",
            "pEleXErrRefitBSC/D", "pEleYErrRefitBSC/D", "pEleZErrRefitBSC/D",
            "pPosXErrRefitBSC/D", "pPosYErrRefitBSC/D", "pPosZErrRefitBSC/D"};
        tupleVariables.addAll(Arrays.asList(refitBSCVars));

        String[] refitBSCFromV0Vars = new String[]{"mRefitBSCFromV0/D", "vtxXRefitBSCFromV0/D", "vtxYRefitBSCFromV0/D", "vtxZRefitBSCFromV0/D",
            "pEleXRefitBSCFromV0/D", "pEleYRefitBSCFromV0/D", "pEleZRefitBSCFromV0/D",
            "pPosXRefitBSCFromV0/D", "pPosYRefitBSCFromV0/D", "pPosZRefitBSCFromV0/D", "chisqRefitBSCFromV0/D",
            "mErrRefitBSCFromV0/D", "vtxXErrRefitBSCFromV0/D", "vtxYErrRefitBSCFromV0/D", "vtxZErrRefitBSCFromV0/D",
            "pEleXErrRefitBSCFromV0/D", "pEleYErrRefitBSCFromV0/D", "pEleZErrRefitBSCFromV0/D",
            "pPosXErrRefitBSCFromV0/D", "pPosYErrRefitBSCFromV0/D", "pPosZErrRefitBSCFromV0/D"};
        tupleVariables.addAll(Arrays.asList(refitBSCFromV0Vars));

         String[] trackPars = new String[]{"eleOmegaErr/D","eleD0Err/D","elePhi0Err/D","eleSlopeErr/D", "eleZ0Err/D",  
             "posOmegaErr/D","posD0Err/D","posPhi0Err/D","posSlopeErr/D", "posZ0Err/D",
             "eleOmega/D","eleD0/D","elePhi0/D","eleSlope/D", "eleZ0/D",  
             "posOmega/D","posD0/D","posPhi0/D","posSlope/D", "posZ0/D",
             "eleMCOmega/D","eleMCD0/D","eleMCPhi0/D","eleMCSlope/D", "eleMCZ0/D",  
             "posMCOmega/D","posMCD0/D","posMCPhi0/D","posMCSlope/D", "posMCZ0/D",
             "eleTrkChisq/D","posTrkChisq/D"
         };
        tupleVariables.addAll(Arrays.asList(trackPars));

        
    }

    protected void detectorChanged(Detector detector) {

        super.detectorChanged(detector);
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

        TIData triggerData = null;
        if (event.hasCollection(GenericObject.class, "TriggerBank"))
            for (GenericObject data : event.get(GenericObject.class, "TriggerBank"))
                if (AbstractIntData.getTag(data) == TIData.BANK_TAG)
                    triggerData = new TIData(data);

        if (!event.hasCollection(ReconstructedParticle.class, finalStateParticlesColName)) {
            if (debug)
                LOGGER.info(finalStateParticlesColName + " collection not found???");
            return;
        }

        if (!event.hasCollection(MCParticle.class, mcParticlesColName)) {
            if (debug)
                LOGGER.info(mcParticlesColName + " collection not found???");
            return;
        }

        //check to see if this event is from the correct trigger (or "all");
        if (triggerData != null && !matchTriggerType(triggerData))
            return;

        nRecoEvents++;
        if (debug)
            LOGGER.info("##########  Start of VertexDebugger   ##############");
        List<ReconstructedParticle> finalStateParticles = event.get(ReconstructedParticle.class, finalStateParticlesColName);
        if (debug)
            LOGGER.info("This events has " + finalStateParticles.size() + " final state particles");
        List<ReconstructedParticle> unconstrainedV0List = event.get(ReconstructedParticle.class, unconstrainedV0CandidatesColName);
        if (debug)
            LOGGER.info("This events has " + unconstrainedV0List.size() + " unconstrained V0s");
        List<ReconstructedParticle> BSCV0List = event.get(ReconstructedParticle.class, beamConV0CandidatesColName);
        if (debug)
            LOGGER.info("This events has " + BSCV0List.size() + " BSC V0s");

        List<MCParticle> MCParticleList = event.get(MCParticle.class, mcParticlesColName);

        //find electron and positron MCParticles
        Hep3Vector vertexPositionMC = null;
        double apMassMC = -9999;
        MCParticle eleMC = null;
        MCParticle posMC = null;

        for (MCParticle mcp : MCParticleList)
            if (mcp.getPDGID() == 622 && mcp.getDaughters().size() == 2) {
                vertexPositionMC = mcp.getEndPoint();
                apMassMC = mcp.getMass();
                List<MCParticle> daugList = mcp.getDaughters();
                for (MCParticle daug : daugList)
                    if (daug.getPDGID() == 11)
                        eleMC = daug;
                    else if (daug.getPDGID() == -11)
                        posMC = daug;
            }
        if (eleMC == null || posMC == null) {
            if (debug)
                LOGGER.info("Couldn't find the MC e+e- from A'?????  Quitting.");
            return;
        }

        ReconstructedParticle electron = null;
        ReconstructedParticle positron = null;
        double bestMom = 99999;
        for (ReconstructedParticle fsp : finalStateParticles)
            if (fsp.getCharge() > 0) {//found a positron
                if (fsp.getMomentum().y() * posMC.getPY() > 0) //dumb matching..make sure in same half
                    positron = fsp;
            } else if (fsp.getCharge() < 0)
                if (fsp.getMomentum().y() * eleMC.getPY() > 0 && Math.abs(eleMC.getMomentum().magnitude() - fsp.getMomentum().magnitude()) < bestMom) //dumb matching..make sure in same half
                    electron = fsp;
        if (electron == null || positron == null) {
            if (debug)
                LOGGER.info("Couldn't find MCP matched reconed e+ or e- ?????  Quitting.");
            return;
        }
        //ok..made it this far...now lets do the vertexing.  
        if (debug) {
            LOGGER.info("Found A' MC with vertex at :" + vertexPositionMC.x() + "; " + vertexPositionMC.y() + "; " + vertexPositionMC.z());
            LOGGER.info("Found A' MC with mass = " + apMassMC);
            LOGGER.info("Found A' MC electron momentum = " + eleMC.getMomentum().x() + "; " + eleMC.getMomentum().y() + "; " + eleMC.getMomentum().z());
            LOGGER.info("Found A' MC positron momentum = " + posMC.getMomentum().x() + "; " + posMC.getMomentum().y() + "; " + posMC.getMomentum().z());
        }

        if (debug) {
            LOGGER.info("electron momentum = " + electron.getMomentum().x() + "; " + electron.getMomentum().y() + "; " + electron.getMomentum().z());
            LOGGER.info("positron momentum = " + positron.getMomentum().x() + "; " + positron.getMomentum().y() + "; " + positron.getMomentum().z());
        }

        Hep3Vector pEleMC = eleMC.getMomentum();
        Hep3Vector pPosMC = posMC.getMomentum();

        tupleMap.clear();

        tupleMap.put("apMass/D", apMassMC);
        tupleMap.put("vtxXMC/D", vertexPositionMC.x());
        tupleMap.put("vtxYMC/D", vertexPositionMC.y());
        tupleMap.put("vtxZMC/D", vertexPositionMC.z());
        tupleMap.put("pEleXMC/D", pEleMC.x());
        tupleMap.put("pEleYMC/D", pEleMC.y());
        tupleMap.put("pEleZMC/D", pEleMC.z());
        tupleMap.put("pPosXMC/D", pPosMC.x());
        tupleMap.put("pPosYMC/D", pPosMC.y());
        tupleMap.put("pPosZMC/D", pPosMC.z());

        // Debug the uncertainties on the track
//        double[] cov=electron.getTracks().get(0).getTrackStates().get(0).getCovMatrix();
//        SymmetricMatrix foo= new SymmetricMatrix(5,cov,false);
//     
        // Covert the tracks to BilliorTracks.
        BilliorTrack electronBTrack = toBilliorTrack(electron.getTracks().get(0));
        BilliorTrack positronBTrack = toBilliorTrack(positron.getTracks().get(0));
        // Generate a candidate vertex and particle.
//        if (debug)
//            LOGGER.info("Unconstrained R=(0,0,0)  ##############");

        BilliorVertex vtxFit = fitVertex(VertexDebugTupleDriver.Constraint.UNCONSTRAINED, electronBTrack, positronBTrack);
        Hep3Vector vtxPos = vtxFit.getPosition();

        // Parameter ordering.
        int D0 = ParameterName.d0.ordinal();
        int PHI = ParameterName.phi0.ordinal();
        int OMEGA = ParameterName.omega.ordinal();
        int TANLAMBDA = ParameterName.tanLambda.ordinal();
        int Z0 = ParameterName.z0.ordinal();

        double[] ecov = electron.getTracks().get(0).getTrackStates().get(0).getCovMatrix();
        SymmetricMatrix emat = new SymmetricMatrix(5, ecov, true);
        double[] pcov = positron.getTracks().get(0).getTrackStates().get(0).getCovMatrix();
        SymmetricMatrix pmat = new SymmetricMatrix(5, pcov, true);
     
//        System.out.println("ecov(0) = " + ecov[0]
//                + "; ecov(1) = " + ecov[1]
//                + "; ecov(2) = " + ecov[2]
//                + "; ecov(3) = " + ecov[3]
//                + "; ecov(4) = " + ecov[4]
//                + "; ecov(5) = " + ecov[5]
//                + "; ecov(6) = " + ecov[6]
//                + "; ecov(7) = " + ecov[7]
//                + "; ecov(8) = " + ecov[8]
//                + "; ecov(9) = " + ecov[9]
//                + "; ecov(10) = " + ecov[10]
//                + "; ecov(11) = " + ecov[11]
//                + "; ecov(12) = " + ecov[12]
//                + "; ecov(13) = " + ecov[13]
//                + "; ecov(14) = " + ecov[14]
//        );
//        System.out.println("Matrix : \n" + emat.toString());
//        System.out.println("Matrix : \n" + emattrue.toString());

        //        HelicalTrackFit mcHTF=TrackUtils.getHTF(posMC, B_FIELD);   
        //switch the sign of the B_FIELD like the tracking code needs
//        HelixParamCalculator parCalc = new HelixParamCalculator(CoordinateTransformations.transformVectorToTracking(posMC.getMomentum()), CoordinateTransformations.transformVectorToTracking(posMC.getOrigin()), (int) posMC.getCharge(), -B_FIELD);
        double[] newparCalc = TrackUtils.getParametersFromPointAndMomentum(CoordinateTransformations.transformVectorToTracking(posMC.getOrigin()), CoordinateTransformations.transformVectorToTracking(posMC.getMomentum()), (int) posMC.getCharge(), -B_FIELD);
//        System.out.println("MC Origin:  ox = " + CoordinateTransformations.transformVectorToTracking(posMC.getOrigin()).x()
//                + "; oy = " + CoordinateTransformations.transformVectorToTracking(posMC.getOrigin()).y()
//                + "; oz = " + CoordinateTransformations.transformVectorToTracking(posMC.getOrigin()).z()
//        );
//        System.out.println("MC Momentum:  px = " + CoordinateTransformations.transformVectorToTracking(posMC.getMomentum()).x()
//                + "; py = " + CoordinateTransformations.transformVectorToTracking(posMC.getMomentum()).y()
//                + "; pz = " + CoordinateTransformations.transformVectorToTracking(posMC.getMomentum()).z()
//        );
//        System.out.println("MC HTF:  d0 = " + parCalc.getDCA()
//                + "; phi0 = " + parCalc.getPhi0()
//                + "; curvature = " + 1 / parCalc.getRadius()
//                + "; z0 = " + parCalc.getZ0()
//                + "; slope = " + parCalc.getSlopeSZPlane()
//        );
//        System.out.println("Recon HTF:  d0 = " + electron.getTracks().get(0).getTrackStates().get(0).getD0()
//                + "; phi0 = " + electron.getTracks().get(0).getTrackStates().get(0).getPhi()
//                + "; curvature = " + electron.getTracks().get(0).getTrackStates().get(0).getOmega()
//                + "; z0 = " + electron.getTracks().get(0).getTrackStates().get(0).getZ0()
//                + "; slope = " + electron.getTracks().get(0).getTrackStates().get(0).getTanLambda()
//        );
 
        HelixParamCalculator parCalcP = new HelixParamCalculator(CoordinateTransformations.transformVectorToTracking(posMC.getMomentum()), CoordinateTransformations.transformVectorToTracking(posMC.getOrigin()), (int) posMC.getCharge(), -B_FIELD);
        HelixParamCalculator parCalcM = new HelixParamCalculator(CoordinateTransformations.transformVectorToTracking(eleMC.getMomentum()), CoordinateTransformations.transformVectorToTracking(eleMC.getOrigin()), (int) eleMC.getCharge(), -B_FIELD);
          System.out.println("MC HTF:  d0 = " + parCalcP.getDCA()
                + "; phi0 = " + parCalcP.getPhi0()
                + "; curvature = " + 1 / parCalcP.getRadius()
                + "; z0 = " + parCalcP.getZ0()
                + "; slope = " + parCalcP.getSlopeSZPlane()
        );
        tupleMap.put("eleTrkChisq/D", electronBTrack.chisqtot());
        tupleMap.put("eleOmegaErr/D", Math.sqrt(emat.e(OMEGA, OMEGA)));//omega
        tupleMap.put("eleD0Err/D", Math.sqrt(emat.e(D0, D0)));//doca
        tupleMap.put("elePhi0Err/D", Math.sqrt(emat.e(PHI, PHI)));
        tupleMap.put("eleSlopeErr/D", Math.sqrt(emat.e(TANLAMBDA, TANLAMBDA)));
        tupleMap.put("eleZ0Err/D", Math.sqrt(emat.e(Z0, Z0)));
        tupleMap.put("posTrkChisq/D", positronBTrack.chisqtot());
        tupleMap.put("posOmegaErr/D", Math.sqrt(pmat.e(OMEGA, OMEGA)));//omega
        tupleMap.put("posD0Err/D", Math.sqrt(pmat.e(D0, D0)));//doca
        tupleMap.put("posPhi0Err/D", Math.sqrt(pmat.e(PHI, PHI)));
        tupleMap.put("posSlopeErr/D", Math.sqrt(pmat.e(TANLAMBDA, TANLAMBDA)));
        tupleMap.put("posZ0Err/D", Math.sqrt(pmat.e(Z0, Z0)));

        tupleMap.put("eleOmega/D", electron.getTracks().get(0).getTrackStates().get(0).getOmega());//omega
        tupleMap.put("eleD0/D", electron.getTracks().get(0).getTrackStates().get(0).getD0());//doca
        tupleMap.put("elePhi0/D", electron.getTracks().get(0).getTrackStates().get(0).getPhi());
        tupleMap.put("eleSlope/D", electron.getTracks().get(0).getTrackStates().get(0).getTanLambda());
        tupleMap.put("eleZ0/D", electron.getTracks().get(0).getTrackStates().get(0).getZ0());
        tupleMap.put("posOmega/D", positron.getTracks().get(0).getTrackStates().get(0).getOmega());//omega
        tupleMap.put("posD0/D", positron.getTracks().get(0).getTrackStates().get(0).getD0());//doca
        tupleMap.put("posPhi0/D", positron.getTracks().get(0).getTrackStates().get(0).getPhi());
        tupleMap.put("posSlope/D", positron.getTracks().get(0).getTrackStates().get(0).getTanLambda());
        tupleMap.put("posZ0/D", positron.getTracks().get(0).getTrackStates().get(0).getZ0());

        tupleMap.put("eleMCSlope/D", parCalcM.getSlopeSZPlane());//emct.slope());
        tupleMap.put("eleMCD0/D", parCalcM.getDCA());//emct.dca());
        tupleMap.put("eleMCPhi0/D", parCalcM.getPhi0());//emct.phi0());
        tupleMap.put("eleMCOmega/D", -1*parCalcM.getMCOmega());//emct.curvature());
        tupleMap.put("eleMCZ0/D", parCalcM.getZ0());//emct.z0());
        tupleMap.put("posMCSlope/D", parCalcP.getSlopeSZPlane());//pmct.slope());
        tupleMap.put("posMCD0/D", parCalcP.getDCA());//pmct.dca());
        tupleMap.put("posMCPhi0/D", parCalcP.getPhi0());//pmct.phi0());
        tupleMap.put("posMCOmega/D", -1*parCalcP.getMCOmega());//pmct.curvature());
        tupleMap.put("posMCZ0/D", parCalcP.getZ0());//pmct.z0());

//        System.out.println("MC New:  d0 = " + newparCalc[HelicalTrackFit.dcaIndex]
//                + "; phi0 = " + newparCalc[HelicalTrackFit.phi0Index]
//                + "; curvature = " + newparCalc[HelicalTrackFit.curvatureIndex]
//                + "; z0 = " + newparCalc[HelicalTrackFit.z0Index]
//                + "; slope = " + newparCalc[HelicalTrackFit.slopeIndex]
//        );

        Hep3Vector pEleFit = vtxFit.getFittedMomentum(0);
        Hep3Vector pPosFit = vtxFit.getFittedMomentum(1);
        double mUncFit = vtxFit.getInvMass();
        double chisqFit = vtxFit.getChi2();
        if (debug)
            LOGGER.info("Unconstrained R=0  vertexMC z=" + vertexPositionMC.z() + " re-fit vtxPos z = " + (vtxPos.z()));
        tupleMap.put("mFitUnc/D", mUncFit);
        tupleMap.put("vtxXFitUnc/D", vtxPos.x());
        tupleMap.put("vtxYFitUnc/D", vtxPos.y());
        tupleMap.put("vtxZFitUnc/D", vtxPos.z());
        tupleMap.put("pEleXFitUnc/D", pEleFit.x());
        tupleMap.put("pEleYFitUnc/D", pEleFit.y());
        tupleMap.put("pEleZFitUnc/D", pEleFit.z());
        tupleMap.put("pPosXFitUnc/D", pPosFit.x());
        tupleMap.put("pPosYFitUnc/D", pPosFit.y());
        tupleMap.put("pPosZFitUnc/D", pPosFit.z());
        tupleMap.put("chisqFitUnc/D", chisqFit);

        tupleMap.put("mErrFitUnc/D", vtxFit.getInvMassError());
        tupleMap.put("vtxXErrFitUnc/D", vtxFit.getPositionError().x());
        tupleMap.put("vtxYErrFitUnc/D", vtxFit.getPositionError().y());
        tupleMap.put("vtxZErrFitUnc/D", vtxFit.getPositionError().z());
        tupleMap.put("pEleXErrFitUnc/D", vtxFit.getFittedMomentumError(0).x());
        tupleMap.put("pEleYErrFitUnc/D", vtxFit.getFittedMomentumError(0).y());
        tupleMap.put("pEleZErrFitUnc/D", vtxFit.getFittedMomentumError(0).z());
        tupleMap.put("pPosXErrFitUnc/D", vtxFit.getFittedMomentumError(1).x());
        tupleMap.put("pPosYErrFitUnc/D", vtxFit.getFittedMomentumError(1).y());
        tupleMap.put("pPosZErrFitUnc/D", vtxFit.getFittedMomentumError(1).z());

        //    Ok...same thing with beam-spot constrained
        BilliorVertex vtxFitBSC = fitVertex(VertexDebugTupleDriver.Constraint.BS_CONSTRAINED, electronBTrack, positronBTrack, new BasicHep3Vector(beamPosition));
        Hep3Vector vtxPosBSC = vtxFitBSC.getPosition();
        Hep3Vector pEleFitBSC = vtxFitBSC.getFittedMomentum(0);
        Hep3Vector pPosFitBSC = vtxFitBSC.getFittedMomentum(1);
        LOGGER.info("Constrained R=0  vertexMC z=" + vertexPositionMC.z() + " re-fit vtxPos z = " + (vtxPosBSC.z()));
        double mFitBSC = vtxFitBSC.getInvMass();
        double chisqBSC = vtxFitBSC.getChi2();

        tupleMap.put("mFitBSC/D", mFitBSC);
        tupleMap.put("vtxXFitBSC/D", vtxPosBSC.x());
        tupleMap.put("vtxYFitBSC/D", vtxPosBSC.y());
        tupleMap.put("vtxZFitBSC/D", vtxPosBSC.z());
        tupleMap.put("pEleXFitBSC/D", pEleFitBSC.x());
        tupleMap.put("pEleYFitBSC/D", pEleFitBSC.y());
        tupleMap.put("pEleZFitBSC/D", pEleFitBSC.z());
        tupleMap.put("pPosXFitBSC/D", pPosFitBSC.x());
        tupleMap.put("pPosYFitBSC/D", pPosFitBSC.y());
        tupleMap.put("pPosZFitBSC/D", pPosFitBSC.z());
        tupleMap.put("chisqFitBSC/D", chisqBSC);

        tupleMap.put("mErrFitBSC/D", vtxFitBSC.getInvMassError());
        tupleMap.put("vtxXErrFitBSC/D", vtxFitBSC.getPositionError().x());
        tupleMap.put("vtxYErrFitBSC/D", vtxFitBSC.getPositionError().y());
        tupleMap.put("vtxZErrFitBSC/D", vtxFitBSC.getPositionError().z());
        tupleMap.put("pEleXErrFitBSC/D", vtxFitBSC.getFittedMomentumError(0).x());
        tupleMap.put("pEleYErrFitBSC/D", vtxFitBSC.getFittedMomentumError(0).y());
        tupleMap.put("pEleZErrFitBSC/D", vtxFitBSC.getFittedMomentumError(0).z());
        tupleMap.put("pPosXErrFitBSC/D", vtxFitBSC.getFittedMomentumError(1).x());
        tupleMap.put("pPosYErrFitBSC/D", vtxFitBSC.getFittedMomentumError(1).y());
        tupleMap.put("pPosZErrFitBSC/D", vtxFitBSC.getFittedMomentumError(1).z());

///     Ok...shift the reference point....        
        double[] newRef = {vtxPos.z(), vtxPos.x(), 0.0};//the  TrackUtils.getParametersAtNewRefPoint method only shifts in xy...?
        BaseTrackState eleOldTS = (BaseTrackState) electron.getTracks().get(0).getTrackStates().get(0);
        BaseTrackState posOldTS = (BaseTrackState) positron.getTracks().get(0).getTrackStates().get(0);
        double[] eleShiftPars = TrackUtils.getParametersAtNewRefPoint(newRef, eleOldTS);
        double[] posShiftPars = TrackUtils.getParametersAtNewRefPoint(newRef, posOldTS);
        SymmetricMatrix eleShiftCov = TrackUtils.getCovarianceAtNewRefPoint(newRef, eleOldTS.getReferencePoint(), eleOldTS.getParameters(), new SymmetricMatrix(5, eleOldTS.getCovMatrix(), true));
        SymmetricMatrix posShiftCov = TrackUtils.getCovarianceAtNewRefPoint(newRef, posOldTS.getReferencePoint(), posOldTS.getParameters(), new SymmetricMatrix(5, posOldTS.getCovMatrix(), true));
        BaseTrackState eleShiftTS = new BaseTrackState(eleShiftPars, newRef, eleShiftCov.asPackedArray(true), TrackState.AtIP, B_FIELD);
        BaseTrackState posShiftTS = new BaseTrackState(posShiftPars, newRef, posShiftCov.asPackedArray(true), TrackState.AtIP, B_FIELD);
//            BaseTrackState eleShiftTS = new BaseTrackState(eleShiftPars, newRef, eleOldTS.getCovMatrix(), TrackState.AtIP, B_FIELD);
//            BaseTrackState posShiftTS = new BaseTrackState(posShiftPars, newRef, posOldTS.getCovMatrix(), TrackState.AtIP, B_FIELD);
        BilliorTrack electronBTrackShift = toBilliorTrack(eleShiftTS);
        BilliorTrack positronBTrackShift = toBilliorTrack(posShiftTS);

        //           get the new fitter
        if (debug)
            LOGGER.info("Unconstrained R=(" + newRef[0] + "," + newRef[1] + "," + newRef[2] + ") ##############");
        BilliorVertex vtxFitShift = fitVertex(VertexDebugTupleDriver.Constraint.UNCONSTRAINED, electronBTrackShift, positronBTrackShift);
        Hep3Vector vtxPosRefitUnc = vtxFitShift.getPosition();
        Hep3Vector pEleRefitUnc = vtxFitShift.getFittedMomentum(0);
        Hep3Vector pPosRefitUnc = vtxFitShift.getFittedMomentum(1);
        double mUncRefitUnc = vtxFitShift.getInvMass();
        double chisqRefitUnc = vtxFitShift.getChi2();
        if (debug)
            LOGGER.info("Unconstrained R=shift  vertexMC z=" + vertexPositionMC.z() + " re-fit vtxPos z = " + (vtxPosRefitUnc.z()));

        if (Math.abs(vtxPosRefitUnc.z()) > 0.5) {
            LOGGER.info("Big Shift!!! ");
            LOGGER.info("Electron P = " + pEleFit.x() + ", " + pEleFit.y() + ", " + pEleFit.z());
            LOGGER.info("Positron P = " + pPosFit.x() + ", " + pPosFit.y() + ", " + pPosFit.z());
            LOGGER.info("For -ive pY() component: ");
            SymmetricMatrix badCovOld = new SymmetricMatrix(5, eleOldTS.getCovMatrix(), true);
            SymmetricMatrix badCovNew = eleShiftCov;
            if (pPosFit.y() < 0) {
                badCovOld = new SymmetricMatrix(5, posOldTS.getCovMatrix(), true);
                badCovNew = posShiftCov;
            }
            LOGGER.info(badCovOld.toString());
            LOGGER.info(badCovNew.toString());
        }

        tupleMap.put("mRefitUnc/D", mUncRefitUnc);
        tupleMap.put("vtxXRefitUnc/D", vtxPosRefitUnc.x());
        tupleMap.put("vtxYRefitUnc/D", vtxPosRefitUnc.y());
        tupleMap.put("vtxZRefitUnc/D", vtxPosRefitUnc.z());
        tupleMap.put("pEleXRefitUnc/D", pEleRefitUnc.x());
        tupleMap.put("pEleYRefitUnc/D", pEleRefitUnc.y());
        tupleMap.put("pEleZRefitUnc/D", pEleRefitUnc.z());
        tupleMap.put("pPosXRefitUnc/D", pPosRefitUnc.x());
        tupleMap.put("pPosYRefitUnc/D", pPosRefitUnc.y());
        tupleMap.put("pPosZRefitUnc/D", pPosRefitUnc.z());
        tupleMap.put("chisqRefitUnc/D", chisqRefitUnc);

        tupleMap.put("mErrRefitUnc/D", vtxFitShift.getInvMassError());
        tupleMap.put("vtxXErrRefitUnc/D", vtxFitShift.getPositionError().x());
        tupleMap.put("vtxYErrRefitUnc/D", vtxFitShift.getPositionError().y());
        tupleMap.put("vtxZErrRefitUnc/D", vtxFitShift.getPositionError().z());
        tupleMap.put("pEleXErrRefitUnc/D", vtxFitShift.getFittedMomentumError(0).x());
        tupleMap.put("pEleYErrRefitUnc/D", vtxFitShift.getFittedMomentumError(0).y());
        tupleMap.put("pEleZErrRefitUnc/D", vtxFitShift.getFittedMomentumError(0).z());
        tupleMap.put("pPosXErrRefitUnc/D", vtxFitShift.getFittedMomentumError(1).x());
        tupleMap.put("pPosYErrRefitUnc/D", vtxFitShift.getFittedMomentumError(1).y());
        tupleMap.put("pPosZErrRefitUnc/D", vtxFitShift.getFittedMomentumError(1).z());

        if (unconstrainedV0List.size() == 1) {
            if (mUncRefitUnc != unconstrainedV0List.get(0).getMass())
                LOGGER.info("unconstrained mass mis-match!!!   " + mUncRefitUnc);
            else
                LOGGER.info("unconstrained mass good match!!!   " + mUncRefitUnc);
            BilliorVertex vtxFitShiftFromV0 = (BilliorVertex) unconstrainedV0List.get(0).getStartVertex();

            Hep3Vector vtxPosRefitUncFromV0 = vtxFitShiftFromV0.getPosition();
            Hep3Vector pEleRefitUncFromV0 = vtxFitShiftFromV0.getFittedMomentum(0);
            Hep3Vector pPosRefitUncFromV0 = vtxFitShiftFromV0.getFittedMomentum(1);
            double mUncRefitUncFromV0 = vtxFitShiftFromV0.getInvMass();
            double chisqRefitUncFromV0 = vtxFitShiftFromV0.getChi2();

            tupleMap.put("mRefitUncFromV0/D", mUncRefitUncFromV0);
            tupleMap.put("vtxXRefitUncFromV0/D", vtxPosRefitUncFromV0.x());
            tupleMap.put("vtxYRefitUncFromV0/D", vtxPosRefitUncFromV0.y());
            tupleMap.put("vtxZRefitUncFromV0/D", vtxPosRefitUncFromV0.z());
            tupleMap.put("pEleXRefitUncFromV0/D", pEleRefitUncFromV0.x());
            tupleMap.put("pEleYRefitUncFromV0/D", pEleRefitUncFromV0.y());
            tupleMap.put("pEleZRefitUncFromV0/D", pEleRefitUncFromV0.z());
            tupleMap.put("pPosXRefitUncFromV0/D", pPosRefitUncFromV0.x());
            tupleMap.put("pPosYRefitUncFromV0/D", pPosRefitUncFromV0.y());
            tupleMap.put("pPosZRefitUncFromV0/D", pPosRefitUncFromV0.z());
            tupleMap.put("chisqRefitUncFromV0/D", chisqRefitUncFromV0);

            tupleMap.put("mErrRefitUncFromV0/D", vtxFitShiftFromV0.getInvMassError());
            tupleMap.put("vtxXErrRefitUncFromV0/D", vtxFitShiftFromV0.getPositionError().x());
            tupleMap.put("vtxYErrRefitUncFromV0/D", vtxFitShiftFromV0.getPositionError().y());
            tupleMap.put("vtxZErrRefitUncFromV0/D", vtxFitShiftFromV0.getPositionError().z());
            tupleMap.put("pEleXErrRefitUncFromV0/D", vtxFitShiftFromV0.getFittedMomentumError(0).x());
            tupleMap.put("pEleYErrRefitUncFromV0/D", vtxFitShiftFromV0.getFittedMomentumError(0).y());
            tupleMap.put("pEleZErrRefitUncFromV0/D", vtxFitShiftFromV0.getFittedMomentumError(0).z());
            tupleMap.put("pPosXErrRefitUncFromV0/D", vtxFitShiftFromV0.getFittedMomentumError(1).x());
            tupleMap.put("pPosYErrRefitUncFromV0/D", vtxFitShiftFromV0.getFittedMomentumError(1).y());
            tupleMap.put("pPosZErrRefitUncFromV0/D", vtxFitShiftFromV0.getFittedMomentumError(1).z());
        }

//        Hep3Vector beamRelToVtx = new BasicHep3Vector(-vtxPos.z() + beamPosition[0], -vtxPos.x() + beamPosition[1], -vtxPos.y() + beamPosition[2]);
        Hep3Vector beamRelToNewRef = new BasicHep3Vector(-vtxPos.z() + beamPosition[0], -vtxPos.x() + beamPosition[1], 0);

        if (debug)
            LOGGER.info("Constrained R=(" + newRef[0] + "," + newRef[1] + "," + newRef[2] + ") ##############");
        BilliorVertex vtxFitBSCShift = fitVertex(VertexDebugTupleDriver.Constraint.BS_CONSTRAINED, electronBTrackShift, positronBTrackShift, beamRelToNewRef);
        Hep3Vector vtxPosRefitBSC = vtxFitBSCShift.getPosition();
        Hep3Vector pEleRefitBSC = vtxFitBSCShift.getFittedMomentum(0);
        Hep3Vector pPosRefitBSC = vtxFitBSCShift.getFittedMomentum(1);

        if (debug)
            LOGGER.info("Constrained R=shift  vertexMC z=" + vertexPositionMC.z() + " re-fit vtxPos z = " + (vtxPosRefitBSC.z()));

        double mBSCShift = vtxFitBSCShift.getInvMass();
        double chisqBSCShift = vtxFitBSCShift.getChi2();

        if (BSCV0List.size() == 1) {
            if (mBSCShift != BSCV0List.get(0).getMass())
                LOGGER.info("BSC mass mis-match!!!   " + mBSCShift + ";   " + BSCV0List.get(0).getMass());
            else
                LOGGER.info("BSC mass good match!!!   " + mBSCShift);

            BilliorVertex vtxFitBSCShiftFromV0 = (BilliorVertex) BSCV0List.get(0).getStartVertex();
            Hep3Vector vtxPosRefitBSCFromV0 = vtxFitBSCShiftFromV0.getPosition();
            Hep3Vector pEleRefitBSCFromV0 = vtxFitBSCShiftFromV0.getFittedMomentum(0);
            Hep3Vector pPosRefitBSCFromV0 = vtxFitBSCShiftFromV0.getFittedMomentum(1);
            double mUncRefitBSCFromV0 = vtxFitBSCShiftFromV0.getInvMass();
            double chisqRefitBSCFromV0 = vtxFitBSCShiftFromV0.getChi2();

            tupleMap.put("mRefitBSCFromV0/D", mUncRefitBSCFromV0);
            tupleMap.put("vtxXRefitBSCFromV0/D", vtxPosRefitBSCFromV0.x());
            tupleMap.put("vtxYRefitBSCFromV0/D", vtxPosRefitBSCFromV0.y());
            tupleMap.put("vtxZRefitBSCFromV0/D", vtxPosRefitBSCFromV0.z());
            tupleMap.put("pEleXRefitBSCFromV0/D", pEleRefitBSCFromV0.x());
            tupleMap.put("pEleYRefitBSCFromV0/D", pEleRefitBSCFromV0.y());
            tupleMap.put("pEleZRefitBSCFromV0/D", pEleRefitBSCFromV0.z());
            tupleMap.put("pPosXRefitBSCFromV0/D", pPosRefitBSCFromV0.x());
            tupleMap.put("pPosYRefitBSCFromV0/D", pPosRefitBSCFromV0.y());
            tupleMap.put("pPosZRefitBSCFromV0/D", pPosRefitBSCFromV0.z());
            tupleMap.put("chisqRefitBSCFromV0/D", chisqRefitBSCFromV0);

            tupleMap.put("mErrRefitBSCFromV0/D", vtxFitBSCShiftFromV0.getInvMassError());
            tupleMap.put("vtxXErrRefitBSCFromV0/D", vtxFitBSCShiftFromV0.getPositionError().x());
            tupleMap.put("vtxYErrRefitBSCFromV0/D", vtxFitBSCShiftFromV0.getPositionError().y());
            tupleMap.put("vtxZErrRefitBSCFromV0/D", vtxFitBSCShiftFromV0.getPositionError().z());
            tupleMap.put("pEleXErrRefitBSCFromV0/D", vtxFitBSCShiftFromV0.getFittedMomentumError(0).x());
            tupleMap.put("pEleYErrRefitBSCFromV0/D", vtxFitBSCShiftFromV0.getFittedMomentumError(0).y());
            tupleMap.put("pEleZErrRefitBSCFromV0/D", vtxFitBSCShiftFromV0.getFittedMomentumError(0).z());
            tupleMap.put("pPosXErrRefitBSCFromV0/D", vtxFitBSCShiftFromV0.getFittedMomentumError(1).x());
            tupleMap.put("pPosYErrRefitBSCFromV0/D", vtxFitBSCShiftFromV0.getFittedMomentumError(1).y());
            tupleMap.put("pPosZErrRefitBSCFromV0/D", vtxFitBSCShiftFromV0.getFittedMomentumError(1).z());
        }
        tupleMap.put("mRefitBSC/D", mBSCShift);
        tupleMap.put("vtxXRefitBSC/D", vtxPosRefitBSC.x());
        tupleMap.put("vtxYRefitBSC/D", vtxPosRefitBSC.y());
        tupleMap.put("vtxZRefitBSC/D", vtxPosRefitBSC.z());
        tupleMap.put("pEleXRefitBSC/D", pEleRefitBSC.x());
        tupleMap.put("pEleYRefitBSC/D", pEleRefitBSC.y());
        tupleMap.put("pEleZRefitBSC/D", pEleRefitBSC.z());
        tupleMap.put("pPosXRefitBSC/D", pPosRefitBSC.x());
        tupleMap.put("pPosYRefitBSC/D", pPosRefitBSC.y());
        tupleMap.put("pPosZRefitBSC/D", pPosRefitBSC.z());
        tupleMap.put("chisqRefitBSC/D", chisqBSCShift);

        tupleMap.put("mErrRefitBSC/D", vtxFitBSCShift.getInvMassError());
        tupleMap.put("vtxXErrRefitBSC/D", vtxFitBSCShift.getPositionError().x());
        tupleMap.put("vtxYErrRefitBSC/D", vtxFitBSCShift.getPositionError().y());
        tupleMap.put("vtxZErrRefitBSC/D", vtxFitBSCShift.getPositionError().z());
        tupleMap.put("pEleXErrRefitBSC/D", vtxFitBSCShift.getFittedMomentumError(0).x());
        tupleMap.put("pEleYErrRefitBSC/D", vtxFitBSCShift.getFittedMomentumError(0).y());
        tupleMap.put("pEleZErrRefitBSC/D", vtxFitBSCShift.getFittedMomentumError(0).z());
        tupleMap.put("pPosXErrRefitBSC/D", vtxFitBSCShift.getFittedMomentumError(1).x());
        tupleMap.put("pPosYErrRefitBSC/D", vtxFitBSCShift.getFittedMomentumError(1).y());
        tupleMap.put("pPosZErrRefitBSC/D", vtxFitBSCShift.getFittedMomentumError(1).z());

        if (tupleWriter != null)
            writeTuple();
    }

    private BilliorVertex fitVertex(VertexDebugTupleDriver.Constraint constraint, BilliorTrack electron, BilliorTrack positron) {
        return fitVertex(constraint, electron, positron, null);
    }

    private BilliorVertex fitVertex(VertexDebugTupleDriver.Constraint constraint, BilliorTrack electron, BilliorTrack positron, Hep3Vector v0) {
        // Create a vertex fitter from the magnetic field.

        BilliorVertexer vtxFitter = new BilliorVertexer(B_FIELD);
        // TODO: The beam size should come from the conditions database.
        vtxFitter.setBeamSize(beamSize);
//        vtxFitter.setBeamPosition(beamPosition);

        vtxFitter.setDebug(false);

//            vtxFitter.setV0(v0.v());
        if (v0 != null)
            vtxFitter.setBeamPosition(v0.v());

        // Perform the vertexing based on the specified constraint.
        switch (constraint) {
            case UNCONSTRAINED:
                vtxFitter.doBeamSpotConstraint(false);
                break;
            case BS_CONSTRAINED:
                vtxFitter.doBeamSpotConstraint(true);
                break;
            case TARGET_CONSTRAINED:
                vtxFitter.doTargetConstraint(true);
                break;
        }

        // Add the electron and positron tracks to a track list for
        // the vertex fitter.
        List<BilliorTrack> billiorTracks = new ArrayList<BilliorTrack>();
        billiorTracks.add(electron);
        billiorTracks.add(positron);

        // Find and return a vertex based on the tracks.
        BilliorVertex vtx = vtxFitter.fitVertex(billiorTracks);

        return vtx;
    }

    private BilliorTrack toBilliorTrack(Track track) {
        // Generate and return the billior track.
        return new BilliorTrack(track);
    }

    private BilliorTrack toBilliorTrack(HelicalTrackFit htf) {
        // Generate and return the billior track.
        return new BilliorTrack(htf);
    }

    private BilliorTrack toBilliorTrack(TrackState track) {
        // Generate and return the billior track.
        return new BilliorTrack(track, 0, 0); // track state doesn't store chi^2 info (stored in the Track object)
    }

    // patchVertex written by Norman Graf...I plucked this from HpsReconParticleDriver
    private void patchVertex(ReconstructedParticle electron, ReconstructedParticle positron, BilliorVertex v) {
//        ReconstructedParticle rp = v.getAssociatedParticle();
//        List<ReconstructedParticle> parts = rp.getParticles();
//        ReconstructedParticle electron = null;
//        ReconstructedParticle positron = null;
//        for (ReconstructedParticle part : parts) {
//            if (part.getCharge() < 0)
//                electron = part;
//            if (part.getCharge() > 0)
//                positron = part;
//        }
        //electron
        Track et = electron.getTracks().get(0);
        double etrackMom = electron.getMomentum().magnitude();
        HelicalTrackFit ehtf = TrackUtils.getHTF(et);
        // propagate this to the vertex z position...
        // Note that HPS y is lcsim z  //mg...I don't think this is correct!!!  First, HPS x is lcsim y!    
        //  And I think the vertex is already in the HPS frame!
        double es = HelixUtils.PathToZPlane(ehtf, v.getPosition().y());
//        double es = HelixUtils.PathToXPlane(ehtf, v.getPosition().z(), 10000., 100).get(0);
        LOGGER.info("vertex z=" + v.getPosition().z() + " vertex y = " + v.getPosition().y());
        Hep3Vector epointOnTrackAtVtx = HelixUtils.PointOnHelix(ehtf, es);
        Hep3Vector edirOfTrackAtVtx = HelixUtils.Direction(ehtf, es);
        Hep3Vector emomAtVtx = VecOp.mult(etrackMom, VecOp.unit(edirOfTrackAtVtx));
        //positron
        Track pt = positron.getTracks().get(0);
        double ptrackMom = positron.getMomentum().magnitude();
        HelicalTrackFit phtf = TrackUtils.getHTF(pt);
        // propagate this to the vertex z position...
        // Note that HPS y is lcsim z
        double ps = HelixUtils.PathToZPlane(phtf, v.getPosition().y());
//        double ps = HelixUtils.PathToXPlane(phtf, v.getPosition().z(), 10000., 100).get(0);
        Hep3Vector ppointOnTrackAtVtx = HelixUtils.PointOnHelix(phtf, ps);
        Hep3Vector pdirOfTrackAtVtx = HelixUtils.Direction(phtf, ps);
        Hep3Vector pmomAtVtx = VecOp.mult(ptrackMom, VecOp.unit(pdirOfTrackAtVtx));

        double mass = invMass(emomAtVtx, pmomAtVtx);
        v.setVertexTrackParameters(emomAtVtx, pmomAtVtx, mass);
    }

    // invMass  is probably defined in the code a hundred times...here it is again. 
    private double invMass(Hep3Vector p1, Hep3Vector p2) {
        double me2 = 0.000511 * 0.000511;
        double esum = sqrt(p1.magnitudeSquared() + me2) + sqrt(p2.magnitudeSquared() + me2);
        double pxsum = p1.x() + p2.x();
        double pysum = p1.y() + p2.y();
        double pzsum = p1.z() + p2.z();

        double psum = Math.sqrt(pxsum * pxsum + pysum * pysum + pzsum * pzsum);
        double evtmass = esum * esum - psum * psum;

        if (evtmass > 0)
            return Math.sqrt(evtmass);
        else
            return -99;
    }

    private double getCosOpenX(Hep3Vector p1, Hep3Vector p2) {   // p vectors are in tracking frame...return open angle in X-detector!!!       
        Hep3Vector pEleX = new BasicHep3Vector(p1.y(), 0.0, p1.x());
        Hep3Vector pPosX = new BasicHep3Vector(p2.y(), 0.0, p2.x());
        return Math.acos(VecOp.dot(pEleX, pPosX) / (pEleX.magnitude() * pPosX.magnitude()));
    }

}
