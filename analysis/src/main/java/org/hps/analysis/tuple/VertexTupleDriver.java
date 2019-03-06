package org.hps.analysis.tuple;

//import hep.physics.matrix.Matrix;
import org.hps.analysis.vertex.*;
//import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
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
//import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.TrackUtils;
//import static org.hps.recon.tracking.TrackUtils.getMomentum;
//import static org.hps.recon.tracking.TrackUtils.getPoint;
import org.hps.recon.tracking.TrackerHitUtils;
import org.hps.recon.utils.TrackClusterMatcher;
import org.hps.recon.vertexing.BilliorTrack;
import org.hps.recon.vertexing.BilliorVertex;
import org.hps.recon.vertexing.BilliorVertexer;
import org.lcsim.detector.converter.compact.subdetector.SvtStereoLayer;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
//import org.lcsim.event.Cluster;
//import org.lcsim.event.LCIOParameters.ParameterName;
import org.lcsim.event.MCParticle;
import org.lcsim.event.TrackerHit;
//import org.lcsim.event.base.BaseTrackState;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
//import org.lcsim.fit.helicaltrack.HelixParamCalculator;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
//import org.lcsim.recon.vertexing.billoir.Vertex;

public class VertexTupleDriver extends MCTupleMaker {

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

    double matchEleP = 0.5;  //0.5 GeV Match

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
            "apTarX/D", "apTarY/D", "apOrigX/D", "apOrigY/D", "apOrigZ/D",
            "matchPos/D","matchEle/D"};
        tupleVariables.addAll(Arrays.asList(mcVars));

        String[] refitUncVars = new String[]{"mUnc/D", "vtxXUnc/D", "vtxYUnc/D", "vtxZUnc/D",
            "pEleXUnc/D", "pEleYUnc/D", "pEleZUnc/D",
            "pPosXUnc/D", "pPosYUnc/D", "pPosZUnc/D", "chisqUnc/D","probUnc/D",
            "mErrUnc/D", "vtxXErrUnc/D", "vtxYErrUnc/D", "vtxZErrUnc/D",
            "pEleXErrUnc/D", "pEleYErrUnc/D", "pEleZErrUnc/D",
            "pPosXErrUnc/D", "pPosYErrUnc/D", "pPosZErrUnc/D"};
        tupleVariables.addAll(Arrays.asList(refitUncVars));

        String[] fitParsUncVars = new String[]{"thetaFitEleUnc/D", "phivFitEleUnc/D", "rhoFitEleUnc/D",
            "thetaFitErrEleUnc/D", "phivFitErrEleUnc/D", "rhoFitErrEleUnc/D",
            "thetaFitPosUnc/D", "phivFitPosUnc/D", "rhoFitPosUnc/D",
            "thetaFitErrPosUnc/D", "phivFitErrPosUnc/D", "rhoFitErrPosUnc/D"};
        tupleVariables.addAll(Arrays.asList(fitParsUncVars));

        String[] refitBSCVars = new String[]{"mBSC/D", "vtxXBSC/D", "vtxYBSC/D", "vtxZBSC/D",
            "pEleXBSC/D", "pEleYBSC/D", "pEleZBSC/D",
            "pPosXBSC/D", "pPosYBSC/D", "pPosZBSC/D", "chisqBSC/D","probBSC/D",
            "mErrBSC/D", "vtxXErrBSC/D", "vtxYErrBSC/D", "vtxZErrBSC/D",
            "pEleXErrBSC/D", "pEleYErrBSC/D", "pEleZErrBSC/D",
            "pPosXErrBSC/D", "pPosYErrBSC/D", "pPosZErrBSC/D"};
        tupleVariables.addAll(Arrays.asList(refitBSCVars));

        String[] trackPars = new String[]{"eleOmegaErr/D", "eleD0Err/D", "elePhi0Err/D", "eleSlopeErr/D", "eleZ0Err/D",
            "posOmegaErr/D", "posD0Err/D", "posPhi0Err/D", "posSlopeErr/D", "posZ0Err/D",
            "eleOmega/D", "eleD0/D", "elePhi0/D", "eleSlope/D", "eleZ0/D",
            "posOmega/D", "posD0/D", "posPhi0/D", "posSlope/D", "posZ0/D",
            "eleMCOmega/D", "eleMCD0/D", "eleMCPhi0/D", "eleMCSlope/D", "eleMCZ0/D",
            "posMCOmega/D", "posMCD0/D", "posMCPhi0/D", "posMCSlope/D", "posMCZ0/D",
            "eleMCNewCalcOmega/D", "eleMCNewCalcD0/D", "eleMCNewCalcPhi0/D", "eleMCNewCalcSlope/D", "eleMCNewCalcZ0/D",
            "posMCNewCalcOmega/D", "posMCNewCalcD0/D", "posMCNewCalcPhi0/D", "posMCNewCalcSlope/D", "posMCNewCalcZ0/D",
            "eleTrkChisq/D", "posTrkChisq/D", "elePxFromTrack/D", "elePyFromTrack/D", "elePzFromTrack/D",
            "posPxFromTrack/D", "posPyFromTrack/D", "posPzFromTrack/D"
        };

        String[] v0UncVars = new String[]{"v0MomXUnc/D", "v0MomYUnc/D", "v0MomZUnc/D",
            "v0MomXErrUnc/D", "v0MomYErrUnc/D", "v0MomZErrUnc/D",
            "v0XYTarXUnc/D", "v0XYTarYUnc/D", "v0XYTarXErrUnc/D", "v0XYTarYErrUnc/D"};
        tupleVariables.addAll(Arrays.asList(v0UncVars));
        
        String[] v0BSCVars = new String[]{"v0MomXBSC/D", "v0MomYBSC/D", "v0MomZBSC/D",
            "v0MomXErrBSC/D", "v0MomYErrBSC/D", "v0MomZErrBSC/D",
            "v0XYTarXBSC/D", "v0XYTarYBSC/D", "v0XYTarXErrBSC/D", "v0XYTarYErrBSC/D"};
        tupleVariables.addAll(Arrays.asList(v0BSCVars));

        tupleVariables.addAll(Arrays.asList(trackPars));

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

        tupleMap.clear();
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
        if (eleMC == null || posMC == null) {
            if (debug)
                LOGGER.info("Couldn't find the MC e+e- from A'?????  Quitting.");
            return;
        }

        if (debug) {
            LOGGER.info("Found A' MC with vertex at :" + vertexPositionMC.x() + "; " + vertexPositionMC.y() + "; " + vertexPositionMC.z());
            LOGGER.info("Found A' MC with mass = " + apMassMC);
            LOGGER.info("Found A' MC electron momentum = " + eleMC.getMomentum().x() + "; " + eleMC.getMomentum().y() + "; " + eleMC.getMomentum().z());
            LOGGER.info("Found A' MC positron momentum = " + posMC.getMomentum().x() + "; " + posMC.getMomentum().y() + "; " + posMC.getMomentum().z());
        }

        Hep3Vector pEleMC = eleMC.getMomentum();
        Hep3Vector pPosMC = posMC.getMomentum();
        Hep3Vector pApMC = ap.getMomentum();
        Hep3Vector apStart = ap.getOrigin();
        double[] apTarXY = getTargetXY(pApMC, vertexPositionMC);
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
        tupleMap.put("pApXMC/D", pApMC.x());
        tupleMap.put("pApYMC/D", pApMC.y());
        tupleMap.put("pApZMC/D", pApMC.z());
        tupleMap.put("apTarX/D", apTarXY[0]);
        tupleMap.put("apTarY/D", apTarXY[1]);
        tupleMap.put("apOrigX/D", apStart.x());  //these AP origin variables should be at Z~0 (the target) and XY with the width of beamspot
        tupleMap.put("apOrigY/D", apStart.y());
        tupleMap.put("apOrigZ/D", apStart.z());


        fillEventVariables(event, triggerData);
        fillMCTridentVariables(event);
        int index=0;
        boolean foundEventMatch=false;
        for (ReconstructedParticle v0 : unconstrainedV0List) {
            if(foundEventMatch)
                break;
            
            List<ReconstructedParticle> daug=v0.getParticles();
          
            if (daug.size() != 2) {
                System.out.println("Not 2 daughters in this V0???");
                continue;
            }
            Track electron = daug.get(0).getTracks().get(0);
            Track positron = daug.get(1).getTracks().get(0);
            if (electron.getCharge() > 1) {//wrong charge...swap them
                Track tmp = electron;
                electron = positron;
                positron = tmp;
            }
            double[] eleP = electron.getTrackStates().get(0).getMomentum();
            double[] posP = positron.getTrackStates().get(0).getMomentum();
            double elePMag = pMag(eleP);

            //  try to match reco'ed electron and positron to the v0
            double matchPos = 0.0;
            double matchEle = 0.0;
            double eleMCPmag=eleMC.getMomentum().magnitude();
            System.out.println("Positron Track P() = ("+posP[0]+","+posP[1]+","+posP[2]+")");
            System.out.println("Positron MC    P() = ("+posMC.getPX()+","+posMC.getPY()+","+posMC.getPZ()+")");
            System.out.println("Electron Track P() = ("+eleP[0]+","+eleP[1]+","+eleP[2]+")");
            System.out.println("Electron MC    P() = ("+eleMC.getPX()+","+eleMC.getPY()+","+eleMC.getPZ()+")");
            if (posP[2] * posMC.getPY() > 0) //dumb matching..make sure in same half
                matchPos = 1.0;
            if (eleP[2] * eleMC.getPY() > 0 && Math.abs(eleMC.getMomentum().magnitude() - elePMag) < matchEleP) //dumb matching..make sure in same half and has reasonable momentum
                matchEle = 1.0;   
            foundEventMatch=(matchPos==1.0)&&(matchEle==1.0);
            if(foundEventMatch)
                System.out.println("Found an e+e- pair match"+elePMag+"; "+eleMCPmag);
            if (!foundEventMatch) {
                System.out.println("This e+e- pair didn't match");
                if (index+1<unconstrainedV0List.size()) //if this is not the last V0 in the list, go to next v0 and try to find e+e- match; otherwise, go ahead and save info for this v0
                    continue;   
            }
                      
            tupleMap.put("matchEle/D", matchEle);
            tupleMap.put("matchPos/D", matchPos);
            BilliorVertex vtxUnc = (BilliorVertex) v0.getStartVertex();
            Hep3Vector vtxPosUnc = vtxUnc.getPosition();
            Hep3Vector pEleUnc = vtxUnc.getFittedMomentum(0);
            Hep3Vector pPosUnc = vtxUnc.getFittedMomentum(1);
            double mUncUnc = vtxUnc.getInvMass();
            double chisqUnc = vtxUnc.getChi2();
            Hep3Vector v0Mom = vtxUnc.getV0Momentum();
            Hep3Vector v0MomError = vtxUnc.getV0MomentumError();
            double[] v0AtTarget = vtxUnc.getV0TargetXY();
            double[] v0AtTargetError = vtxUnc.getV0TargetXYError();
            
            tupleMap.put("mUnc/D", mUncUnc);
            tupleMap.put("vtxXUnc/D", vtxPosUnc.x());
            tupleMap.put("vtxYUnc/D", vtxPosUnc.y());
            tupleMap.put("vtxZUnc/D", vtxPosUnc.z());
            tupleMap.put("pEleXUnc/D", pEleUnc.x());
            tupleMap.put("pEleYUnc/D", pEleUnc.y());
            tupleMap.put("pEleZUnc/D", pEleUnc.z());
            tupleMap.put("pPosXUnc/D", pPosUnc.x());
            tupleMap.put("pPosYUnc/D", pPosUnc.y());
            tupleMap.put("pPosZUnc/D", pPosUnc.z());
            tupleMap.put("chisqUnc/D", chisqUnc);
            tupleMap.put("probUnc/D",vtxUnc.getProbability());

            tupleMap.put("mErrUnc/D", vtxUnc.getInvMassError());
            tupleMap.put("vtxXErrUnc/D", vtxUnc.getPositionError().x());
            tupleMap.put("vtxYErrUnc/D", vtxUnc.getPositionError().y());
            tupleMap.put("vtxZErrUnc/D", vtxUnc.getPositionError().z());
            tupleMap.put("pEleXErrUnc/D", vtxUnc.getFittedMomentumError(0).x());
            tupleMap.put("pEleYErrUnc/D", vtxUnc.getFittedMomentumError(0).y());
            tupleMap.put("pEleZErrUnc/D", vtxUnc.getFittedMomentumError(0).z());
            tupleMap.put("pPosXErrUnc/D", vtxUnc.getFittedMomentumError(1).x());
            tupleMap.put("pPosYErrUnc/D", vtxUnc.getFittedMomentumError(1).y());
            tupleMap.put("pPosZErrUnc/D", vtxUnc.getFittedMomentumError(1).z());
            
            tupleMap.put("v0MomXErrUnc/D", v0MomError.x());
            tupleMap.put("v0MomYErrUnc/D", v0MomError.y());
            tupleMap.put("v0MomZErrUnc/D", v0MomError.z());
            tupleMap.put("v0XYTarXErrUnc/D", v0AtTargetError[0]);
            tupleMap.put("v0XYTarYErrUnc/D", v0AtTargetError[1]);
            tupleMap.put("v0MomXUnc/D", v0Mom.x());
            tupleMap.put("v0MomYUnc/D", v0Mom.y());
            tupleMap.put("v0MomZUnc/D", v0Mom.z());
            //System.out.println("VertexDebugTuple::v0 projection X = " + v0AtTarget[0] + "; Y = " + v0AtTarget[1]);
            tupleMap.put("v0XYTarXUnc/D", v0AtTarget[0]);
            tupleMap.put("v0XYTarYUnc/D", v0AtTarget[1]);
            
            ReconstructedParticle  bscv0=BSCV0List.get(index);
            
            BilliorVertex vtxBSC = (BilliorVertex) bscv0.getStartVertex();
            Hep3Vector vtxPosBSC = vtxBSC.getPosition();
            Hep3Vector pEleBSC = vtxBSC.getFittedMomentum(0);
            Hep3Vector pPosBSC = vtxBSC.getFittedMomentum(1);
            double mBSCBSC = vtxBSC.getInvMass();
            double chisqBSC = vtxBSC.getChi2();
            Hep3Vector v0MomBSC = vtxBSC.getV0Momentum();
            Hep3Vector v0MomErrorBSC = vtxBSC.getV0MomentumError();
            double[] v0AtTargetBSC = vtxBSC.getV0TargetXY();
            double[] v0AtTargetErrorBSC = vtxBSC.getV0TargetXYError();
            tupleMap.put("mBSC/D", mBSCBSC);
            tupleMap.put("vtxXBSC/D", vtxPosBSC.x());
            tupleMap.put("vtxYBSC/D", vtxPosBSC.y());
            tupleMap.put("vtxZBSC/D", vtxPosBSC.z());
            tupleMap.put("pEleXBSC/D", pEleBSC.x());
            tupleMap.put("pEleYBSC/D", pEleBSC.y());
            tupleMap.put("pEleZBSC/D", pEleBSC.z());
            tupleMap.put("pPosXBSC/D", pPosBSC.x());
            tupleMap.put("pPosYBSC/D", pPosBSC.y());
            tupleMap.put("pPosZBSC/D", pPosBSC.z());
            tupleMap.put("chisqBSC/D", chisqBSC);
            tupleMap.put("probBSC/D",vtxBSC.getProbability());
            tupleMap.put("mErrBSC/D", vtxBSC.getInvMassError());
            tupleMap.put("vtxXErrBSC/D", vtxBSC.getPositionError().x());
            tupleMap.put("vtxYErrBSC/D", vtxBSC.getPositionError().y());
            tupleMap.put("vtxZErrBSC/D", vtxBSC.getPositionError().z());
            tupleMap.put("pEleXErrBSC/D", vtxBSC.getFittedMomentumError(0).x());
            tupleMap.put("pEleYErrBSC/D", vtxBSC.getFittedMomentumError(0).y());
            tupleMap.put("pEleZErrBSC/D", vtxBSC.getFittedMomentumError(0).z());
            tupleMap.put("pPosXErrBSC/D", vtxBSC.getFittedMomentumError(1).x());
            tupleMap.put("pPosYErrBSC/D", vtxBSC.getFittedMomentumError(1).y());
            tupleMap.put("pPosZErrBSC/D", vtxBSC.getFittedMomentumError(1).z());
            
            tupleMap.put("v0MomXErrBSC/D", v0MomErrorBSC.x());
            tupleMap.put("v0MomYErrBSC/D", v0MomErrorBSC.y());
            tupleMap.put("v0MomZErrBSC/D", v0MomErrorBSC.z());
            tupleMap.put("v0XYTarXErrBSC/D", v0AtTargetErrorBSC[0]);
            tupleMap.put("v0XYTarYErrBSC/D", v0AtTargetErrorBSC[1]);
            tupleMap.put("v0MomXBSC/D", v0MomBSC.x());
            tupleMap.put("v0MomYBSC/D", v0MomBSC.y());
            tupleMap.put("v0MomZBSC/D", v0MomBSC.z());
           // System.out.println("VertexDebugTuple::v0 projection X = " + v0AtTargetBSC[0] + "; Y = " + v0AtTargetBSC[1]);
            tupleMap.put("v0XYTarXBSC/D", v0AtTargetBSC[0]);
            tupleMap.put("v0XYTarYBSC/D", v0AtTargetBSC[1]);
            index++;
        }

        if (tupleWriter != null){
            System.out.println("!!!!!!!!!!!!!!!!!       VertexTupleDriver::DONE WITH THIS EVENT           !!!!!!!!!!!!!!!!");
            writeTuple();
        }
    }

    private BilliorVertex fitVertex(VertexTupleDriver.Constraint constraint, BilliorTrack electron, BilliorTrack positron) {
        return fitVertex(constraint, electron, positron, null);
    }

    private BilliorVertex fitVertex(VertexTupleDriver.Constraint constraint, BilliorTrack electron, BilliorTrack positron, Hep3Vector v0) {
        // Create a vertex fitter from the magnetic field.

        BilliorVertexer vtxFitter = new BilliorVertexer(B_FIELD);
        // TODO: The beam size should come from the conditions database.
        vtxFitter.setBeamSize(beamSize);
        vtxFitter.setDebug(false);

//  mg: 5/11/2018  use ReferencePosition now instead of beam position...
//        if (v0 != null)
//            vtxFitter.setBeamPosition(v0.v());
        if (v0 != null)
            vtxFitter.setReferencePosition(v0.v());

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

    private void writeTrackerHits(List<TrackerHit> hits, String ofile) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(ofile);
        writer.println("x/D:y/D:z/D");
        for (TrackerHit hit : hits)
            writer.println(hit.getPosition()[0] + " " + hit.getPosition()[1] + " " + hit.getPosition()[2] + " ");
        writer.close();
    }

    private void writePoint(Hep3Vector hit, String ofile) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(ofile);
        writer.println("x/D:y/D:z/D");
        writer.println(hit.x() + " " + hit.y() + " " + hit.z() + " ");
        writer.close();
    }

    private double[] getTargetXY(Hep3Vector mom, Hep3Vector vertex) {
        double sX = mom.x() / mom.z();
        double sY = mom.y() / mom.z();
        double vX = vertex.x();
        double vY = vertex.y();
        double vZ = vertex.z();
        double delZ = -vZ;
        double[] tXY = {delZ * sX + vX, delZ * sY + vY};
        return tXY;

    }

    private double pMag(double[] p) {
        return Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]);
    }

}
