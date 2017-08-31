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
//import org.hps.recon.particle.ReconParticleDriver;
//import org.hps.recon.tracking.TrackType;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.hps.analysis.tuple.TupleDriver;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.TrackerHitUtils;
import org.hps.recon.utils.TrackClusterMatcher;
import org.hps.recon.vertexing.BilliorTrack;
import org.hps.recon.vertexing.BilliorVertex;
import org.hps.recon.vertexing.BilliorVertexer;
import org.lcsim.detector.converter.compact.subdetector.SvtStereoLayer;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.MCParticle;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.lcio.LCIOWriter;

public class VertexDebugTupleDriver extends TupleDriver {

    private static Logger LOGGER = Logger.getLogger(VertexDebugger.class.getPackage().getName());

    String finalStateParticlesColName = "FinalStateParticles";
    String mcParticlesColName = "MCParticle";
    String readoutHitCollectionName = "EcalReadoutHits";//these are in ADC counts
    String calibratedHitCollectionName = "EcalCalHits";//these are in energy
    String clusterCollectionName = "EcalClustersCorr";
    private String notrackFile;
    private String helicalTrackHitCollectionName = "HelicalTrackHits";
    private String rotatedTrackHitCollectionName = "RotatedHelicalTrackHits";
    String[] fpQuantNames = {"nEle_per_Event", "nPos_per_Event", "nPhoton_per_Event", "nUnAssociatedTracks_per_Event", "avg_delX_at_ECal", "avg_delY_at_ECal", "avg_E_Over_P", "avg_mom_beam_elec", "sig_mom_beam_elec"};
    private String outputFile;
    private LCIOWriter writer;
    private LCIOWriter notrackwriter;
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
    private final String plotDir = "FinalStateParticles/";
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
            "pPosXFitUnc/D", "pPosYFitUnc/D", "pPosZFitUnc/D", "chisqFitUnc/D"};
        tupleVariables.addAll(Arrays.asList(fitUncVars));

        String[] refitUncVars = new String[]{"mRefitUnc/D", "vtxXRefitUnc/D", "vtxYRefitUnc/D", "vtxZRefitUnc/D",
            "pEleXRefitUnc/D", "pEleYRefitUnc/D", "pEleZRefitUnc/D",
            "pPosXRefitUnc/D", "pPosYRefitUnc/D", "pPosZRefitUnc/D", "chisqRefitUnc/D"};
        tupleVariables.addAll(Arrays.asList(refitUncVars));

        String[] fitBSCVars = new String[]{"mFitBSC/D", "vtxXFitBSC/D", "vtxYFitBSC/D", "vtxZFitBSC/D",
            "pEleXFitBSC/D", "pEleYFitBSC/D", "pEleZFitBSC/D",
            "pPosXFitBSC/D", "pPosYFitBSC/D", "pPosZFitBSC/D", "chisqFitBSC/D"};
        tupleVariables.addAll(Arrays.asList(fitBSCVars));

        String[] refitBSCVars = new String[]{"mRefitBSC/D", "vtxXRefitBSC/D", "vtxYRefitBSC/D", "vtxZRefitBSC/D",
            "pEleXRefitBSC/D", "pEleYRefitBSC/D", "pEleZRefitBSC/D",
            "pPosXRefitBSC/D", "pPosYRefitBSC/D", "pPosZRefitBSC/D", "chisqRefitBSC/D"};
        tupleVariables.addAll(Arrays.asList(refitBSCVars));

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
//        for (ReconstructedParticle fsPart1 : finalStateParticles) {
        List<MCParticle> MCParticleList = event.get(MCParticle.class, mcParticlesColName);

        boolean saveEvent = false;
        boolean saveNoTrack = false;
        //find electron and positron MCParticles
        Hep3Vector vertexPositionMC = null;
        double apMassMC = -9999;
        MCParticle eleMC = null;
        MCParticle posMC = null;

        for (MCParticle mcp : MCParticleList)
//             if (debug)
            //               LOGGER.info("MC PDGID = "+mcp.getPDGID()+"; # daughters = "+mcp.getDaughters().size());
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

        // Covert the tracks to BilliorTracks.
        BilliorTrack electronBTrack = toBilliorTrack(electron.getTracks().get(0));
        BilliorTrack positronBTrack = toBilliorTrack(positron.getTracks().get(0));
        // Generate a candidate vertex and particle.
//        if (debug)
//            LOGGER.info("Unconstrained R=(0,0,0)  ##############");

        BilliorVertex vtxFit = fitVertex(VertexDebugTupleDriver.Constraint.UNCONSTRAINED, electronBTrack, positronBTrack);
        Hep3Vector vtxPos = vtxFit.getPosition();
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

        //    Ok...same thing with beam-spot constrained
//        if (debug)
//            LOGGER.info("Constrained R=(0,0,0) ##############");
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
        if(debug) 
              LOGGER.info("Unconstrained R=shift  vertexMC z=" + vertexPositionMC.z() + " re-fit vtxPos z = " + (vtxPosRefitUnc.z()));

        if (Math.abs(vtxPosRefitUnc.z())>0.5){
            LOGGER.info("Big Shift!!! ");
            LOGGER.info("Electron P = "+pEleFit.x()+", "+pEleFit.y()+", "+pEleFit.z());
            LOGGER.info("Positron P = "+pPosFit.x()+", "+pPosFit.y()+", "+pPosFit.z());
            LOGGER.info("For -ive pY() component: ");
            SymmetricMatrix badCovOld= new SymmetricMatrix(5, eleOldTS.getCovMatrix(), true);
              SymmetricMatrix badCovNew=eleShiftCov;
              if(pPosFit.y()<0){
                  badCovOld=new SymmetricMatrix(5, posOldTS.getCovMatrix(), true);
                  badCovNew=posShiftCov;
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

//        Hep3Vector beamRelToVtx = new BasicHep3Vector(-vtxPos.z() + beamPosition[0], -vtxPos.x() + beamPosition[1], -vtxPos.y() + beamPosition[2]);
        Hep3Vector beamRelToNewRef = new BasicHep3Vector(-vtxPos.z() + beamPosition[0], -vtxPos.x() + beamPosition[1],0);

        if (debug)
            LOGGER.info("Constrained R=(" + newRef[0] + "," + newRef[1] + "," + newRef[2] + ") ##############");
        BilliorVertex vtxFitBSCShift = fitVertex(VertexDebugTupleDriver.Constraint.BS_CONSTRAINED, electronBTrackShift, positronBTrackShift, beamRelToNewRef);
        Hep3Vector vtxPosRefitBSC = vtxFitBSCShift.getPosition();
        Hep3Vector pEleRefitBSC = vtxFitBSCShift.getFittedMomentum(0);
        Hep3Vector pPosRefitBSC = vtxFitBSCShift.getFittedMomentum(1);

         if(debug) 
              LOGGER.info("Constrained R=shift  vertexMC z=" + vertexPositionMC.z() + " re-fit vtxPos z = " + (vtxPosRefitBSC.z()));

        double mBSCShift = vtxFitBSCShift.getInvMass();
        double chisqBSCShift = vtxFitBSCShift.getChi2();

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
