package org.hps.analysis.vertex;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.io.IOException;
import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hps.analysis.dataquality.DataQualityMonitor;

import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.TrackerHitUtils;
import org.hps.recon.utils.TrackClusterMatcher;
import org.hps.recon.vertexing.BilliorTrack;
import org.hps.recon.vertexing.BilliorVertex;
import org.hps.recon.vertexing.BilliorVertexer;
import org.lcsim.detector.converter.compact.subdetector.SvtStereoLayer;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.lcio.LCIOWriter;

/**
 *
 */
public class VertexDebugger extends DataQualityMonitor {

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

    IHistogram1D elePx;
    IHistogram1D elePy;
    IHistogram1D elePz;
    IHistogram1D elePzBeam;
    IHistogram1D elePzBeamTop;
    IHistogram1D elePzBeamBottom;
    IHistogram1D elePTop;
    IHistogram1D elePBottom;
    IHistogram1D eleClEne;

    IHistogram1D posPx;
    IHistogram1D posPy;
    IHistogram1D posPz;
    IHistogram1D posPTop;
    IHistogram1D posPBottom;
    IHistogram1D posClEne;

    IHistogram2D delElePx;
    IHistogram2D delElePy;
    IHistogram2D delElePz;

    IHistogram2D delPosPx;
    IHistogram2D delPosPy;
    IHistogram2D delPosPz;

    IHistogram2D delXvsVtxZ;
    IHistogram2D delYvsVtxZ;
    IHistogram2D delZvsVtxZ;
    IHistogram2D delMvsVtxZ;
    IHistogram2D delPvsVtxZ;
    IHistogram2D openAnglevsVtxZ;
    IHistogram2D openAnglevsVtxZShift;
    IHistogram2D openAnglevsVtxZBSC;
    IHistogram2D openAnglevsVtxZBSCShift;

    IHistogram2D delMvsVtxZPatch;
    IHistogram2D delMUncvsPatchvsZ;

    IHistogram2D delElePxReco;
    IHistogram2D delElePyReco;
    IHistogram2D delElePzReco;

    IHistogram2D delPosPxReco;
    IHistogram2D delPosPyReco;
    IHistogram2D delPosPzReco;
    IHistogram2D chiSqVtxZ;

    IHistogram2D delElePxShift;
    IHistogram2D delElePyShift;
    IHistogram2D delElePzShift;

    IHistogram2D delPosPxShift;
    IHistogram2D delPosPyShift;
    IHistogram2D delPosPzShift;

    IHistogram2D delXvsVtxZShift;
    IHistogram2D delYvsVtxZShift;
    IHistogram2D delZvsVtxZShift;
    IHistogram2D delMvsVtxZShift;
    IHistogram2D delPvsVtxZShift;
    IHistogram2D chiSqVtxZShift;

    IHistogram2D delElePxBSC;
    IHistogram2D delElePyBSC;
    IHistogram2D delElePzBSC;

    IHistogram2D delPosPxBSC;
    IHistogram2D delPosPyBSC;
    IHistogram2D delPosPzBSC;

    IHistogram2D delXvsVtxZBSC;
    IHistogram2D delYvsVtxZBSC;
    IHistogram2D delZvsVtxZBSC;
    IHistogram2D delMvsVtxZBSC;
    IHistogram2D delPvsVtxZBSC;
    IHistogram2D chiSqVtxZBSC;

    IHistogram2D delElePxBSCShift;
    IHistogram2D delElePyBSCShift;
    IHistogram2D delElePzBSCShift;
    IHistogram2D chiSqVtxZBSCShift;

    IHistogram2D delPosPxBSCShift;
    IHistogram2D delPosPyBSCShift;
    IHistogram2D delPosPzBSCShift;

    IHistogram2D delXvsVtxZBSCShift;
    IHistogram2D delYvsVtxZBSCShift;
    IHistogram2D delZvsVtxZBSCShift;
    IHistogram2D delMvsVtxZBSCShift;
    IHistogram2D delPvsVtxZBSCShift;

    IHistogram2D delZShiftMinusNoShiftvsVtxZBSC;
    IHistogram2D delZShiftMinusNoShiftvsVtxZUnc;

    IHistogram2D delXShiftMinusNoShiftvsVtxZBSC;
    IHistogram2D delXShiftMinusNoShiftvsVtxZUnc;

    IHistogram2D delYShiftMinusNoShiftvsVtxZBSC;
    IHistogram2D delYShiftMinusNoShiftvsVtxZUnc;

    IHistogram2D delXShiftMinusNoShiftvsdelYBSC;
    IHistogram2D delXShiftMinusNoShiftvsdelYUnc;

    IHistogram2D delYShiftMinusNoShiftvsdelZBSC;
    IHistogram2D delYShiftMinusNoShiftvsdelZUnc;

    IHistogram2D delXShiftMinusNoShiftvsdelZBSC;
    IHistogram2D delXShiftMinusNoShiftvsdelZUnc;

    IHistogram2D delMShiftMinusNoShiftvsVtxZBSC;
    IHistogram2D delMShiftMinusNoShiftvsVtxZUnc;

    IHistogram2D delChiSqShiftMinusNoShiftvsVtxZBSC;
    IHistogram2D delChiSqShiftMinusNoShiftvsVtxZUnc;

    String eleTopGood = "ElectronInTopGood/";
    String posTopGood = "PositronInTopGood/";
    String eleTopBad = "ElectronInTopBad/";
    String posTopBad = "PositronInTopBad/";

    IHistogram2D delXvsVtxZeleTopGood;
    IHistogram2D delYvsVtxZeleTopGood;
    IHistogram2D delZvsVtxZeleTopGood;
    IHistogram2D delMvsVtxZeleTopGood;
    IHistogram2D delPvsVtxZeleTopGood;
    IHistogram2D chiSqVtxZeleTopGood;

    IHistogram2D delXvsVtxZposTopGood;
    IHistogram2D delYvsVtxZposTopGood;
    IHistogram2D delZvsVtxZposTopGood;
    IHistogram2D delMvsVtxZposTopGood;
    IHistogram2D delPvsVtxZposTopGood;
    IHistogram2D chiSqVtxZposTopGood;

    IHistogram2D delXvsVtxZeleTopBad;
    IHistogram2D delYvsVtxZeleTopBad;
    IHistogram2D delZvsVtxZeleTopBad;
    IHistogram2D delMvsVtxZeleTopBad;
    IHistogram2D delPvsVtxZeleTopBad;
    IHistogram2D chiSqVtxZeleTopBad;

    IHistogram2D delXvsVtxZposTopBad;
    IHistogram2D delYvsVtxZposTopBad;
    IHistogram2D delZvsVtxZposTopBad;
    IHistogram2D delMvsVtxZposTopBad;
    IHistogram2D delPvsVtxZposTopBad;
    IHistogram2D chiSqVtxZposTopBad;

    IHistogram2D delXvsVtxZeleTopGoodShift;
    IHistogram2D delYvsVtxZeleTopGoodShift;
    IHistogram2D delZvsVtxZeleTopGoodShift;
    IHistogram2D delMvsVtxZeleTopGoodShift;
    IHistogram2D delPvsVtxZeleTopGoodShift;
    IHistogram2D chiSqVtxZeleTopGoodShift;

    IHistogram2D delXvsVtxZposTopGoodShift;
    IHistogram2D delYvsVtxZposTopGoodShift;
    IHistogram2D delZvsVtxZposTopGoodShift;
    IHistogram2D delMvsVtxZposTopGoodShift;
    IHistogram2D delPvsVtxZposTopGoodShift;
    IHistogram2D chiSqVtxZposTopGoodShift;

    IHistogram2D delXvsVtxZeleTopBadShift;
    IHistogram2D delYvsVtxZeleTopBadShift;
    IHistogram2D delZvsVtxZeleTopBadShift;
    IHistogram2D delMvsVtxZeleTopBadShift;
    IHistogram2D delPvsVtxZeleTopBadShift;
    IHistogram2D chiSqVtxZeleTopBadShift;

    IHistogram2D delXvsVtxZposTopBadShift;
    IHistogram2D delYvsVtxZposTopBadShift;
    IHistogram2D delZvsVtxZposTopBadShift;
    IHistogram2D delMvsVtxZposTopBadShift;
    IHistogram2D delPvsVtxZposTopBadShift;
    IHistogram2D chiSqVtxZposTopBadShift;

    String elePxChargePosGood = "ElectronPxChargePosGood/";
    String elePxChargeNegGood = "ElectronPxChargeNegGood/";
    String elePxChargePosBad = "ElectronPxChargePosBad/";
    String elePxChargeNegBad = "ElectronPxChargeNegBad/";

    IHistogram2D delXvsVtxZelePxChargePosGood;
    IHistogram2D delYvsVtxZelePxChargePosGood;
    IHistogram2D delZvsVtxZelePxChargePosGood;
    IHistogram2D delMvsVtxZelePxChargePosGood;
    IHistogram2D delPvsVtxZelePxChargePosGood;
    IHistogram2D chiSqVtxZelePxChargePosGood;

    IHistogram2D delXvsVtxZelePxChargePosBad;
    IHistogram2D delYvsVtxZelePxChargePosBad;
    IHistogram2D delZvsVtxZelePxChargePosBad;
    IHistogram2D delMvsVtxZelePxChargePosBad;
    IHistogram2D delPvsVtxZelePxChargePosBad;
    IHistogram2D chiSqVtxZelePxChargePosBad;

    IHistogram2D delXvsVtxZelePxChargeNegGood;
    IHistogram2D delYvsVtxZelePxChargeNegGood;
    IHistogram2D delZvsVtxZelePxChargeNegGood;
    IHistogram2D delMvsVtxZelePxChargeNegGood;
    IHistogram2D delPvsVtxZelePxChargeNegGood;
    IHistogram2D chiSqVtxZelePxChargeNegGood;

    IHistogram2D delXvsVtxZelePxChargeNegBad;
    IHistogram2D delYvsVtxZelePxChargeNegBad;
    IHistogram2D delZvsVtxZelePxChargeNegBad;
    IHistogram2D delMvsVtxZelePxChargeNegBad;
    IHistogram2D delPvsVtxZelePxChargeNegBad;
    IHistogram2D chiSqVtxZelePxChargeNegBad;

    IHistogram2D delXvsVtxZelePxChargePosGoodShift;
    IHistogram2D delYvsVtxZelePxChargePosGoodShift;
    IHistogram2D delZvsVtxZelePxChargePosGoodShift;
    IHistogram2D delMvsVtxZelePxChargePosGoodShift;
    IHistogram2D delPvsVtxZelePxChargePosGoodShift;
    IHistogram2D chiSqVtxZelePxChargePosGoodShift;

    IHistogram2D delXvsVtxZelePxChargePosBadShift;
    IHistogram2D delYvsVtxZelePxChargePosBadShift;
    IHistogram2D delZvsVtxZelePxChargePosBadShift;
    IHistogram2D delMvsVtxZelePxChargePosBadShift;
    IHistogram2D delPvsVtxZelePxChargePosBadShift;
    IHistogram2D chiSqVtxZelePxChargePosBadShift;

    IHistogram2D delXvsVtxZelePxChargeNegGoodShift;
    IHistogram2D delYvsVtxZelePxChargeNegGoodShift;
    IHistogram2D delZvsVtxZelePxChargeNegGoodShift;
    IHistogram2D delMvsVtxZelePxChargeNegGoodShift;
    IHistogram2D delPvsVtxZelePxChargeNegGoodShift;
    IHistogram2D chiSqVtxZelePxChargeNegGoodShift;

    IHistogram2D delXvsVtxZelePxChargeNegBadShift;
    IHistogram2D delYvsVtxZelePxChargeNegBadShift;
    IHistogram2D delZvsVtxZelePxChargeNegBadShift;
    IHistogram2D delMvsVtxZelePxChargeNegBadShift;
    IHistogram2D delPvsVtxZelePxChargeNegBadShift;
    IHistogram2D chiSqVtxZelePxChargeNegBadShift;

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

    public void setFinalStateParticlesColName(String fsp) {
        this.finalStateParticlesColName = fsp;
    }

    public void setOutputFilePath(String output) {
        this.outputFile = output;
    }

    public void setNoTracksFilePath(String output) {
        this.notrackFile = output;
    }

    public void setDoSkim(boolean doit) {
        this.doSkim = doit;
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
        aida.tree().cd("/");


        /*  Final State Particle Quantities   */
 /*  plot electron & positron momentum separately  */
        elePx = aida.histogram1D("Electron Px (GeV)", nbins, -0.1 * beamEnergy, 0.200 * beamEnergy);
        elePy = aida.histogram1D("Electron Py (GeV)", nbins, -0.1 * beamEnergy, 0.1 * beamEnergy);
        elePz = aida.histogram1D("Electron Pz (GeV)", nbins, 0, beamEnergy * maxFactor);

        posPx = aida.histogram1D("Positron Px (GeV)", nbins, -0.1 * beamEnergy, 0.200 * beamEnergy);
        posPy = aida.histogram1D("Positron Py (GeV)", nbins, -0.1 * beamEnergy, 0.1 * beamEnergy);
        posPz = aida.histogram1D("Positron Pz (GeV)", nbins, 0, beamEnergy * maxFactor);

        delElePxReco = aida.histogram2D("(MC-Reco) Electron Px (GeV) vs MC Z Vertex", nbins, -0.02, 0.02, nbins, -10, 100);
        delElePyReco = aida.histogram2D("(MC-Reco) Electron Py (GeV) vs MC Z Vertex", nbins, -0.02, 0.02, nbins, -10, 100);
        delElePzReco = aida.histogram2D("(MC-Reco) Electron Pz (GeV) vs MC Z Vertex", nbins, -0.1, 0.1, nbins, -10, 100);

        delPosPxReco = aida.histogram2D("(MC-Reco) Positron Px (GeV) vs MC Z Vertex", nbins, -0.02, 0.02, nbins, -10, 100);
        delPosPyReco = aida.histogram2D("(MC-Reco) Positron Py (GeV) vs MC Z Vertex", nbins, -0.02, 0.02, nbins, -10, 100);
        delPosPzReco = aida.histogram2D("(MC-Reco) Positron Pz (GeV) vs MC Z Vertex", nbins, -0.1, 0.1, nbins, -10, 100);

        delElePx = aida.histogram2D("(MC-Fitted) Electron Px (GeV) vs MC Z Vertex", nbins, -0.02, 0.02, nbins, -10, 100);
        delElePy = aida.histogram2D("(MC-Fitted) Electron Py (GeV) vs MC Z Vertex", nbins, -0.02, 0.02, nbins, -10, 100);
        delElePz = aida.histogram2D("(MC-Fitted) Electron Pz (GeV) vs MC Z Vertex", nbins, -0.1, 0.1, nbins, -10, 100);

        delPosPx = aida.histogram2D("(MC-Fitted) Positron Px (GeV) vs MC Z Vertex", nbins, -0.02, 0.02, nbins, -10, 100);
        delPosPy = aida.histogram2D("(MC-Fitted) Positron Py (GeV) vs MC Z Vertex", nbins, -0.02, 0.02, nbins, -10, 100);
        delPosPz = aida.histogram2D("(MC-Fitted) Positron Pz (GeV) vs MC Z Vertex", nbins, -0.1, 0.1, nbins, -10, 100);

        delXvsVtxZ = aida.histogram2D("(MC-Fitted) X vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delYvsVtxZ = aida.histogram2D("(MC-Fitted) Y vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delZvsVtxZ = aida.histogram2D("(MC-Fitted) Z vs MC Z Vertex", nbins, -50, 50, nbins, -10, 100);
        delMvsVtxZ = aida.histogram2D("(MC-Fitted) Mass vs MC Z Vertex", nbins, -0.04, 0.04, nbins, -10, 100);
        delPvsVtxZ = aida.histogram2D("(MC-Fitted) P vs MC Z Vertex", nbins, -1, 1, nbins, -10, 100);
        openAnglevsVtxZ = aida.histogram2D("(MC-Fitted) OpenAngle vs MC Z Vertex", nbins, 0.0, 0.2, nbins, -10, 100);
        chiSqVtxZ = aida.histogram2D("Fitted Chi Sq vs MC Z Vertex", nbins, 0, 10, nbins, -10, 100);

        delElePxShift = aida.histogram2D("Shifted (MC-Fitted) Electron Px (GeV) vs MC Z Vertex", nbins, -0.02, 0.02, nbins, -10, 100);
        delElePyShift = aida.histogram2D("Shifted (MC-Fitted) Electron Py (GeV) vs MC Z Vertex", nbins, -0.02, 0.02, nbins, -10, 100);
        delElePzShift = aida.histogram2D("Shifted (MC-Fitted) Electron Pz (GeV) vs MC Z Vertex", nbins, -0.1, 0.1, nbins, -10, 100);

        delPosPxShift = aida.histogram2D("Shifted (MC-Fitted) Positron Px (GeV) vs MC Z Vertex", nbins, -0.02, 0.02, nbins, -10, 100);
        delPosPyShift = aida.histogram2D("Shifted (MC-Fitted) Positron Py (GeV) vs MC Z Vertex", nbins, -0.02, 0.02, nbins, -10, 100);
        delPosPzShift = aida.histogram2D("Shifted (MC-Fitted) Positron Pz (GeV) vs MC Z Vertex", nbins, -0.1, 0.1, nbins, -10, 100);

        delZvsVtxZShift = aida.histogram2D("Shifted (MC-Fitted) Z vs MC Z Vertex", nbins, -50, 50, nbins, -10, 100);
        delXvsVtxZShift = aida.histogram2D("Shifted (MC-Fitted) X vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delYvsVtxZShift = aida.histogram2D("Shifted (MC-Fitted) Y vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delMvsVtxZShift = aida.histogram2D("Shifted (MC-Fitted) Mass vs MC Z Vertex", nbins, -0.04, 0.04, nbins, -10, 100);
        delPvsVtxZShift = aida.histogram2D("Shifted (MC-Fitted) P vs MC Z Vertex", nbins, -1, 1, nbins, -10, 100);
        chiSqVtxZShift = aida.histogram2D("Shifted Chi Sq vs MC Z Vertex", nbins, 0, 10, nbins, -10, 100);
        openAnglevsVtxZShift = aida.histogram2D("Shifted (MC-Fitted) OpenAngle vs MC Z Vertex", nbins, 0.0, 0.2, nbins, -10, 100);

        delElePxBSC = aida.histogram2D("BSC (MC-Fitted) Electron Px (GeV) vs MC Z Vertex", nbins, -0.02, 0.02, nbins, -10, 100);
        delElePyBSC = aida.histogram2D("BSC (MC-Fitted) Electron Py (GeV) vs MC Z Vertex", nbins, -0.02, 0.02, nbins, -10, 100);
        delElePzBSC = aida.histogram2D("BSC (MC-Fitted) Electron Pz (GeV) vs MC Z Vertex", nbins, -0.1, 0.1, nbins, -10, 100);

        delPosPxBSC = aida.histogram2D("BSC (MC-Fitted) Positron Px (GeV) vs MC Z Vertex", nbins, -0.02, 0.02, nbins, -10, 100);
        delPosPyBSC = aida.histogram2D("BSC (MC-Fitted) Positron Py (GeV) vs MC Z Vertex", nbins, -0.02, 0.02, nbins, -10, 100);
        delPosPzBSC = aida.histogram2D("BSC (MC-Fitted) Positron Pz (GeV) vs MC Z Vertex", nbins, -0.1, 0.1, nbins, -10, 100);

        delXvsVtxZBSC = aida.histogram2D("BSC (MC-Fitted) X vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delYvsVtxZBSC = aida.histogram2D("BSC (MC-Fitted) Y vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delZvsVtxZBSC = aida.histogram2D("BSC (MC-Fitted) Z vs MC Z Vertex", nbins, -50, 50, nbins, -10, 100);
        delMvsVtxZBSC = aida.histogram2D("BSC (MC-Fitted) Mass vs MC Z Vertex", nbins, -0.04, 0.04, nbins, -10, 100);
        delPvsVtxZBSC = aida.histogram2D("BSC (MC-Fitted) P vs MC Z Vertex", nbins, -1, 1, nbins, -10, 100);
        chiSqVtxZBSC = aida.histogram2D("BSC Chi Sq vs MC Z Vertex", nbins, 0, 25, nbins, -10, 100);
        openAnglevsVtxZBSC = aida.histogram2D("BSC (MC-Fitted) OpenAngle vs MC Z Vertex", nbins, 0.0, 0.2, nbins, -10, 100);

        delElePxBSCShift = aida.histogram2D("BSCShift (MC-Fitted) Electron Px (GeV) vs MC Z Vertex", nbins, -0.02, 0.02, nbins, -10, 100);
        delElePyBSCShift = aida.histogram2D("BSCShift (MC-Fitted) Electron Py (GeV) vs MC Z Vertex", nbins, -0.02, 0.02, nbins, -10, 100);
        delElePzBSCShift = aida.histogram2D("BSCShift (MC-Fitted) Electron Pz (GeV) vs MC Z Vertex", nbins, -0.1, 0.1, nbins, -10, 100);

        delPosPxBSCShift = aida.histogram2D("BSCShift (MC-Fitted) Positron Px (GeV) vs MC Z Vertex", nbins, -0.02, 0.02, nbins, -10, 100);
        delPosPyBSCShift = aida.histogram2D("BSCShift (MC-Fitted) Positron Py (GeV) vs MC Z Vertex", nbins, -0.02, 0.02, nbins, -10, 100);
        delPosPzBSCShift = aida.histogram2D("BSCShift (MC-Fitted) Positron Pz (GeV) vs MC Z Vertex", nbins, -0.1, 0.1, nbins, -10, 100);

        delXvsVtxZBSCShift = aida.histogram2D("BSCShift (MC-Fitted) X vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delYvsVtxZBSCShift = aida.histogram2D("BSCShift (MC-Fitted) Y vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delZvsVtxZBSCShift = aida.histogram2D("BSCShift (MC-Fitted) Z vs MC Z Vertex", nbins, -50, 50, nbins, -10, 100);
        delMvsVtxZBSCShift = aida.histogram2D("BSCShift (MC-Fitted) Mass vs MC Z Vertex", nbins, -0.04, 0.04, nbins, -10, 100);
        delPvsVtxZBSCShift = aida.histogram2D("BSCShift (MC-Fitted) P vs MC Z Vertex", nbins, -1, 1, nbins, -10, 100);
        chiSqVtxZBSCShift = aida.histogram2D("BSCShift Chi Sq vs MC Z Vertex", nbins, 0, 25, nbins, -10, 100);
        openAnglevsVtxZBSCShift = aida.histogram2D("BCSShifted (MC-Fitted) OpenAngle vs MC Z Vertex", nbins, 0.0, 0.2, nbins, -10, 100);

        delMvsVtxZPatch = aida.histogram2D("(MC-Patch) Mass vs MC Z Vertex", nbins, -0.04, 0.04, nbins, -10, 100);
        delMUncvsPatchvsZ = aida.histogram2D("(Patch-Reco) Mass vs MC Z Vertex", nbins, -0.04, 0.04, nbins, -10, 100);

        delZShiftMinusNoShiftvsVtxZBSC = aida.histogram2D("BSC (Shift-No Shift) Z vs MC Z Vertex", nbins * 100, -50, 50, nbins, -10, 100);
        delZShiftMinusNoShiftvsVtxZUnc = aida.histogram2D("Unconstrained (Shift-No Shift) Z vs MC Z Vertex", nbins * 100, -50, 50, nbins, -10, 100);

        delXShiftMinusNoShiftvsVtxZBSC = aida.histogram2D("BSC (Shift-No Shift) X vs MC Z Vertex", nbins * 10, -1, 1, nbins, -10, 100);
        delXShiftMinusNoShiftvsVtxZUnc = aida.histogram2D("Unconstrained (Shift-No Shift) X vs MC Z Vertex", nbins * 10, -1, 1, nbins, -10, 100);

        delYShiftMinusNoShiftvsVtxZBSC = aida.histogram2D("BSC (Shift-No Shift) Y vs MC Z Vertex", nbins * 10, -1, 1, nbins, -10, 100);
        delYShiftMinusNoShiftvsVtxZUnc = aida.histogram2D("Unconstrained (Shift-No Shift) Y vs MC Z Vertex", nbins * 10, -1, 1, nbins, -10, 100);

        delXShiftMinusNoShiftvsdelYBSC = aida.histogram2D("BSC (Shift-No Shift) X vs Delta Y Vertex", nbins * 10, -1, 1, nbins * 10, -1, 1);
        delXShiftMinusNoShiftvsdelYUnc = aida.histogram2D("Unconstrained (Shift-No Shift) X vs Delta Y Vertex", nbins * 10, -1, 1, nbins * 10, -1, 1);

        delYShiftMinusNoShiftvsdelZBSC = aida.histogram2D("BSC (Shift-No Shift) Y vs Delta Z Vertex", nbins * 10, -1, 1, nbins * 10, -10, 10);
        delYShiftMinusNoShiftvsdelZUnc = aida.histogram2D("Unconstrained (Shift-No Shift) Y vs Delta Z Vertex", nbins * 10, -1, 1, nbins * 10, -10, 10);
        delXShiftMinusNoShiftvsdelZBSC = aida.histogram2D("BSC (Shift-No Shift) X vs Delta Z Vertex", nbins * 10, -1, 1, nbins * 10, -10, 10);
        delXShiftMinusNoShiftvsdelZUnc = aida.histogram2D("Unconstrained (Shift-No Shift) X vs Delta Z Vertex", nbins * 10, -1, 1, nbins * 10, -10, 10);

        delMShiftMinusNoShiftvsVtxZBSC = aida.histogram2D("BSC (Shift-No Shift) Mass vs MC Z Vertex", nbins * 10, -0.04, 0.04, nbins, -10, 100);
        delMShiftMinusNoShiftvsVtxZUnc = aida.histogram2D("Unconstrained (Shift-No Shift) Mass vs MC Z Vertex", nbins * 10, -0.04, 0.04, nbins, -10, 100);
        delChiSqShiftMinusNoShiftvsVtxZBSC = aida.histogram2D("BSC (Shift-No Shift) ChiSq vs MC Z Vertex", nbins * 10, -10, 10, nbins, -10, 100);
        delChiSqShiftMinusNoShiftvsVtxZUnc = aida.histogram2D("Unconstrained (Shift-No Shift) ChiSq vs MC Z Vertex", nbins * 10, -10, 10, nbins, -10, 100);

        delXvsVtxZeleTopGood = aida.histogram2D(eleTopGood + "(MC-Fitted) X vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delYvsVtxZeleTopGood = aida.histogram2D(eleTopGood + "(MC-Fitted) Y vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delZvsVtxZeleTopGood = aida.histogram2D(eleTopGood + "(MC-Fitted) Z vs MC Z Vertex", nbins, -50, 50, nbins, -10, 100);
        delMvsVtxZeleTopGood = aida.histogram2D(eleTopGood + "(MC-Fitted) Mass vs MC Z Vertex", nbins, -0.04, 0.04, nbins, -10, 100);
        delPvsVtxZeleTopGood = aida.histogram2D(eleTopGood + "(MC-Fitted) P vs MC Z Vertex", nbins, -1, 1, nbins, -10, 100);
        chiSqVtxZeleTopGood = aida.histogram2D(eleTopGood + "Fitted Chi Sq vs MC Z Vertex", nbins, 0, 10, nbins, -10, 100);

        delXvsVtxZposTopGood = aida.histogram2D(posTopGood + "(MC-Fitted) X vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delYvsVtxZposTopGood = aida.histogram2D(posTopGood + "(MC-Fitted) Y vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delZvsVtxZposTopGood = aida.histogram2D(posTopGood + "(MC-Fitted) Z vs MC Z Vertex", nbins, -50, 50, nbins, -10, 100);
        delMvsVtxZposTopGood = aida.histogram2D(posTopGood + "(MC-Fitted) Mass vs MC Z Vertex", nbins, -0.04, 0.04, nbins, -10, 100);
        delPvsVtxZposTopGood = aida.histogram2D(posTopGood + "(MC-Fitted) P vs MC Z Vertex", nbins, -1, 1, nbins, -10, 100);
        chiSqVtxZposTopGood = aida.histogram2D(posTopGood + "Fitted Chi Sq vs MC Z Vertex", nbins, 0, 10, nbins, -10, 100);

        delXvsVtxZeleTopBad = aida.histogram2D(eleTopBad + "(MC-Fitted) X vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delYvsVtxZeleTopBad = aida.histogram2D(eleTopBad + "(MC-Fitted) Y vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delZvsVtxZeleTopBad = aida.histogram2D(eleTopBad + "(MC-Fitted) Z vs MC Z Vertex", nbins, -50, 50, nbins, -10, 100);
        delMvsVtxZeleTopBad = aida.histogram2D(eleTopBad + "(MC-Fitted) Mass vs MC Z Vertex", nbins, -0.04, 0.04, nbins, -10, 100);
        delPvsVtxZeleTopBad = aida.histogram2D(eleTopBad + "(MC-Fitted) P vs MC Z Vertex", nbins, -1, 1, nbins, -10, 100);
        chiSqVtxZeleTopBad = aida.histogram2D(eleTopBad + "Fitted Chi Sq vs MC Z Vertex", nbins, 0, 10, nbins, -10, 100);

        delXvsVtxZposTopBad = aida.histogram2D(posTopBad + "(MC-Fitted) X vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delYvsVtxZposTopBad = aida.histogram2D(posTopBad + "(MC-Fitted) Y vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delZvsVtxZposTopBad = aida.histogram2D(posTopBad + "(MC-Fitted) Z vs MC Z Vertex", nbins, -50, 50, nbins, -10, 100);
        delMvsVtxZposTopBad = aida.histogram2D(posTopBad + "(MC-Fitted) Mass vs MC Z Vertex", nbins, -0.04, 0.04, nbins, -10, 100);
        delPvsVtxZposTopBad = aida.histogram2D(posTopBad + "(MC-Fitted) P vs MC Z Vertex", nbins, -1, 1, nbins, -10, 100);
        chiSqVtxZposTopBad = aida.histogram2D(posTopBad + "Fitted Chi Sq vs MC Z Vertex", nbins, 0, 10, nbins, -10, 100);

        delXvsVtxZeleTopGoodShift = aida.histogram2D(eleTopGood + "Shifted (MC-Fitted) X vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delYvsVtxZeleTopGoodShift = aida.histogram2D(eleTopGood + "Shifted (MC-Fitted) Y vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delZvsVtxZeleTopGoodShift = aida.histogram2D(eleTopGood + "Shifted (MC-Fitted) Z vs MC Z Vertex", nbins, -50, 50, nbins, -10, 100);
        delMvsVtxZeleTopGoodShift = aida.histogram2D(eleTopGood + "Shifted (MC-Fitted) Mass vs MC Z Vertex", nbins, -0.04, 0.04, nbins, -10, 100);
        delPvsVtxZeleTopGoodShift = aida.histogram2D(eleTopGood + "Shifted (MC-Fitted) P vs MC Z Vertex", nbins, -1, 1, nbins, -10, 100);
        chiSqVtxZeleTopGoodShift = aida.histogram2D(eleTopGood + "Shifted Chi Sq vs MC Z Vertex", nbins, 0, 10, nbins, -10, 100);

        delXvsVtxZposTopGoodShift = aida.histogram2D(posTopGood + "Shifted (MC-Fitted) X vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delYvsVtxZposTopGoodShift = aida.histogram2D(posTopGood + "Shifted (MC-Fitted) Y vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delZvsVtxZposTopGoodShift = aida.histogram2D(posTopGood + "Shifted (MC-Fitted) Z vs MC Z Vertex", nbins, -50, 50, nbins, -10, 100);
        delMvsVtxZposTopGoodShift = aida.histogram2D(posTopGood + "Shifted (MC-Fitted) Mass vs MC Z Vertex", nbins, -0.04, 0.04, nbins, -10, 100);
        delPvsVtxZposTopGoodShift = aida.histogram2D(posTopGood + "Shifted (MC-Fitted) P vs MC Z Vertex", nbins, -1, 1, nbins, -10, 100);
        chiSqVtxZposTopGoodShift = aida.histogram2D(posTopGood + "Shifted Fitted Chi Sq vs MC Z Vertex", nbins, 0, 10, nbins, -10, 100);

        delXvsVtxZeleTopBadShift = aida.histogram2D(eleTopBad + "Shifted (MC-Fitted) X vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delYvsVtxZeleTopBadShift = aida.histogram2D(eleTopBad + "Shifted (MC-Fitted) Y vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delZvsVtxZeleTopBadShift = aida.histogram2D(eleTopBad + "Shifted (MC-Fitted) Z vs MC Z Vertex", nbins, -50, 50, nbins, -10, 100);
        delMvsVtxZeleTopBadShift = aida.histogram2D(eleTopBad + "Shifted (MC-Fitted) Mass vs MC Z Vertex", nbins, -0.04, 0.04, nbins, -10, 100);
        delPvsVtxZeleTopBadShift = aida.histogram2D(eleTopBad + "Shifted (MC-Fitted) P vs MC Z Vertex", nbins, -1, 1, nbins, -10, 100);
        chiSqVtxZeleTopBadShift = aida.histogram2D(eleTopBad + "Shifted Chi Sq vs MC Z Vertex", nbins, 0, 10, nbins, -10, 100);

        delXvsVtxZposTopBadShift = aida.histogram2D(posTopBad + "Shifted (MC-Fitted) X vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delYvsVtxZposTopBadShift = aida.histogram2D(posTopBad + "Shifted (MC-Fitted) Y vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delZvsVtxZposTopBadShift = aida.histogram2D(posTopBad + "Shifted (MC-Fitted) Z vs MC Z Vertex", nbins, -50, 50, nbins, -10, 100);
        delMvsVtxZposTopBadShift = aida.histogram2D(posTopBad + "Shifted (MC-Fitted) Mass vs MC Z Vertex", nbins, -0.04, 0.04, nbins, -10, 100);
        delPvsVtxZposTopBadShift = aida.histogram2D(posTopBad + "Shifted (MC-Fitted) P vs MC Z Vertex", nbins, -1, 1, nbins, -10, 100);
        chiSqVtxZposTopBadShift = aida.histogram2D(posTopBad + "Shifted Fitted Chi Sq vs MC Z Vertex", nbins, 0, 10, nbins, -10, 100);

/////////////////////      histrograms selected for sign(px_electron)*charge  /////////////////////        
        delXvsVtxZelePxChargePosGood = aida.histogram2D(elePxChargePosGood + "(MC-Fitted) X vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delYvsVtxZelePxChargePosGood = aida.histogram2D(elePxChargePosGood + "(MC-Fitted) Y vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delZvsVtxZelePxChargePosGood = aida.histogram2D(elePxChargePosGood + "(MC-Fitted) Z vs MC Z Vertex", nbins, -50, 50, nbins, -10, 100);
        delMvsVtxZelePxChargePosGood = aida.histogram2D(elePxChargePosGood + "(MC-Fitted) Mass vs MC Z Vertex", nbins, -0.04, 0.04, nbins, -10, 100);
        delPvsVtxZelePxChargePosGood = aida.histogram2D(elePxChargePosGood + "(MC-Fitted) P vs MC Z Vertex", nbins, -1, 1, nbins, -10, 100);
        chiSqVtxZelePxChargePosGood = aida.histogram2D(elePxChargePosGood + "Fitted Chi Sq vs MC Z Vertex", nbins, 0, 10, nbins, -10, 100);

        delXvsVtxZelePxChargePosBad = aida.histogram2D(elePxChargePosBad + "(MC-Fitted) X vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delYvsVtxZelePxChargePosBad = aida.histogram2D(elePxChargePosBad + "(MC-Fitted) Y vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delZvsVtxZelePxChargePosBad = aida.histogram2D(elePxChargePosBad + "(MC-Fitted) Z vs MC Z Vertex", nbins, -50, 50, nbins, -10, 100);
        delMvsVtxZelePxChargePosBad = aida.histogram2D(elePxChargePosBad + "(MC-Fitted) Mass vs MC Z Vertex", nbins, -0.04, 0.04, nbins, -10, 100);
        delPvsVtxZelePxChargePosBad = aida.histogram2D(elePxChargePosBad + "(MC-Fitted) P vs MC Z Vertex", nbins, -1, 1, nbins, -10, 100);
        chiSqVtxZelePxChargePosBad = aida.histogram2D(elePxChargePosBad + "Fitted Chi Sq vs MC Z Vertex", nbins, 0, 10, nbins, -10, 100);

        delXvsVtxZelePxChargePosGoodShift = aida.histogram2D(elePxChargePosGood + "Shifted (MC-Fitted) X vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delYvsVtxZelePxChargePosGoodShift = aida.histogram2D(elePxChargePosGood + "Shifted (MC-Fitted) Y vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delZvsVtxZelePxChargePosGoodShift = aida.histogram2D(elePxChargePosGood + "Shifted (MC-Fitted) Z vs MC Z Vertex", nbins, -50, 50, nbins, -10, 100);
        delMvsVtxZelePxChargePosGoodShift = aida.histogram2D(elePxChargePosGood + "Shifted (MC-Fitted) Mass vs MC Z Vertex", nbins, -0.04, 0.04, nbins, -10, 100);
        delPvsVtxZelePxChargePosGoodShift = aida.histogram2D(elePxChargePosGood + "Shifted (MC-Fitted) P vs MC Z Vertex", nbins, -1, 1, nbins, -10, 100);
        chiSqVtxZelePxChargePosGoodShift = aida.histogram2D(elePxChargePosGood + "Shifted Chi Sq vs MC Z Vertex", nbins, 0, 10, nbins, -10, 100);

        delXvsVtxZelePxChargePosBadShift = aida.histogram2D(elePxChargePosBad + "Shifted (MC-Fitted) X vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delYvsVtxZelePxChargePosBadShift = aida.histogram2D(elePxChargePosBad + "Shifted (MC-Fitted) Y vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delZvsVtxZelePxChargePosBadShift = aida.histogram2D(elePxChargePosBad + "Shifted (MC-Fitted) Z vs MC Z Vertex", nbins, -50, 50, nbins, -10, 100);
        delMvsVtxZelePxChargePosBadShift = aida.histogram2D(elePxChargePosBad + "Shifted (MC-Fitted) Mass vs MC Z Vertex", nbins, -0.04, 0.04, nbins, -10, 100);
        delPvsVtxZelePxChargePosBadShift = aida.histogram2D(elePxChargePosBad + "Shifted (MC-Fitted) P vs MC Z Vertex", nbins, -1, 1, nbins, -10, 100);
        chiSqVtxZelePxChargePosBadShift = aida.histogram2D(elePxChargePosBad + "Shifted Chi Sq vs MC Z Vertex", nbins, 0, 10, nbins, -10, 100);

        delXvsVtxZelePxChargeNegGood = aida.histogram2D(elePxChargeNegGood + "(MC-Fitted) X vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delYvsVtxZelePxChargeNegGood = aida.histogram2D(elePxChargeNegGood + "(MC-Fitted) Y vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delZvsVtxZelePxChargeNegGood = aida.histogram2D(elePxChargeNegGood + "(MC-Fitted) Z vs MC Z Vertex", nbins, -50, 50, nbins, -10, 100);
        delMvsVtxZelePxChargeNegGood = aida.histogram2D(elePxChargeNegGood + "(MC-Fitted) Mass vs MC Z Vertex", nbins, -0.04, 0.04, nbins, -10, 100);
        delPvsVtxZelePxChargeNegGood = aida.histogram2D(elePxChargeNegGood + "(MC-Fitted) P vs MC Z Vertex", nbins, -1, 1, nbins, -10, 100);
        chiSqVtxZelePxChargeNegGood = aida.histogram2D(elePxChargeNegGood + "Fitted Chi Sq vs MC Z Vertex", nbins, 0, 10, nbins, -10, 100);

        delXvsVtxZelePxChargeNegBad = aida.histogram2D(elePxChargeNegBad + "(MC-Fitted) X vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delYvsVtxZelePxChargeNegBad = aida.histogram2D(elePxChargeNegBad + "(MC-Fitted) Y vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delZvsVtxZelePxChargeNegBad = aida.histogram2D(elePxChargeNegBad + "(MC-Fitted) Z vs MC Z Vertex", nbins, -50, 50, nbins, -10, 100);
        delMvsVtxZelePxChargeNegBad = aida.histogram2D(elePxChargeNegBad + "(MC-Fitted) Mass vs MC Z Vertex", nbins, -0.04, 0.04, nbins, -10, 100);
        delPvsVtxZelePxChargeNegBad = aida.histogram2D(elePxChargeNegBad + "(MC-Fitted) P vs MC Z Vertex", nbins, -1, 1, nbins, -10, 100);
        chiSqVtxZelePxChargeNegBad = aida.histogram2D(elePxChargeNegBad + "Fitted Chi Sq vs MC Z Vertex", nbins, 0, 10, nbins, -10, 100);

        delXvsVtxZelePxChargeNegGoodShift = aida.histogram2D(elePxChargeNegGood + "Shifted (MC-Fitted) X vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delYvsVtxZelePxChargeNegGoodShift = aida.histogram2D(elePxChargeNegGood + "Shifted (MC-Fitted) Y vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delZvsVtxZelePxChargeNegGoodShift = aida.histogram2D(elePxChargeNegGood + "Shifted (MC-Fitted) Z vs MC Z Vertex", nbins, -50, 50, nbins, -10, 100);
        delMvsVtxZelePxChargeNegGoodShift = aida.histogram2D(elePxChargeNegGood + "Shifted (MC-Fitted) Mass vs MC Z Vertex", nbins, -0.04, 0.04, nbins, -10, 100);
        delPvsVtxZelePxChargeNegGoodShift = aida.histogram2D(elePxChargeNegGood + "Shifted (MC-Fitted) P vs MC Z Vertex", nbins, -1, 1, nbins, -10, 100);
        chiSqVtxZelePxChargeNegGoodShift = aida.histogram2D(elePxChargeNegGood + "Shifted Chi Sq vs MC Z Vertex", nbins, 0, 10, nbins, -10, 100);

        delXvsVtxZelePxChargeNegBadShift = aida.histogram2D(elePxChargeNegBad + "Shifted (MC-Fitted) X vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delYvsVtxZelePxChargeNegBadShift = aida.histogram2D(elePxChargeNegBad + "Shifted (MC-Fitted) Y vs MC Z Vertex", nbins, -2, 2, nbins, -10, 100);
        delZvsVtxZelePxChargeNegBadShift = aida.histogram2D(elePxChargeNegBad + "Shifted (MC-Fitted) Z vs MC Z Vertex", nbins, -50, 50, nbins, -10, 100);
        delMvsVtxZelePxChargeNegBadShift = aida.histogram2D(elePxChargeNegBad + "Shifted (MC-Fitted) Mass vs MC Z Vertex", nbins, -0.04, 0.04, nbins, -10, 100);
        delPvsVtxZelePxChargeNegBadShift = aida.histogram2D(elePxChargeNegBad + "Shifted (MC-Fitted) P vs MC Z Vertex", nbins, -1, 1, nbins, -10, 100);
        chiSqVtxZelePxChargeNegBadShift = aida.histogram2D(elePxChargeNegBad + "Shifted Chi Sq vs MC Z Vertex", nbins, 0, 10, nbins, -10, 100);

    }

    @Override
    public void process(EventHeader event) {
        /*  make sure everything is there */

//        List<CalorimeterHit> hits;
//        if (event.hasCollection(CalorimeterHit.class, calibratedHitCollectionName))
//            hits = event.get(CalorimeterHit.class, calibratedHitCollectionName);
//        else
//            return; //this might be a non-data event        
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
        if (!matchTrigger(event))
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

        Hep3Vector delPEleMC = VecOp.sub(eleMC.getMomentum(), electron.getMomentum());
        Hep3Vector delPPosMC = VecOp.sub(posMC.getMomentum(), positron.getMomentum());

        // Covert the tracks to BilliorTracks.
        BilliorTrack electronBTrack = toBilliorTrack(electron.getTracks().get(0));
        BilliorTrack positronBTrack = toBilliorTrack(positron.getTracks().get(0));
        // Generate a candidate vertex and particle.
        if (debug)
            LOGGER.info("Unconstrained R=(0,0,0)  ##############");

        BilliorVertex vtxFit = fitVertex(Constraint.UNCONSTRAINED, electronBTrack, positronBTrack);
        Hep3Vector vtxPos = vtxFit.getPosition();
        Hep3Vector pEleFit = vtxFit.getFittedMomentum(0);
        Hep3Vector pPosFit = vtxFit.getFittedMomentum(1);

        double delx = vertexPositionMC.x() - vtxPos.x();
        double dely = vertexPositionMC.y() - vtxPos.y();
        double delz = vertexPositionMC.z() - vtxPos.z();
        LOGGER.info("vertexMC z=" + vertexPositionMC.z() + " vtxPos z = " + vtxPos.z());
        double delm = apMassMC - vtxFit.getInvMass();
        double mUnc = vtxFit.getInvMass();
        double chisq = vtxFit.getChi2();
//        double delpEle = eleMC.getMomentum().magnitude() - electron.getMomentum().magnitude();
//        double delpPos = posMC.getMomentum().magnitude() - positron.getMomentum().magnitude();
        Hep3Vector delPEleFit = new BasicHep3Vector(eleMC.getMomentum().x() - pEleFit.y(),
                eleMC.getMomentum().y() - pEleFit.z(), eleMC.getMomentum().z() - pEleFit.x());
        Hep3Vector delPPosFit = new BasicHep3Vector(posMC.getMomentum().x() - pPosFit.y(),
                eleMC.getMomentum().y() - pPosFit.z(), posMC.getMomentum().z() - pPosFit.x());

        if (delz < 10) {//only fill if the V0 looks like it got the track from A' (in which I say deltaZ<10mm)
            delXvsVtxZ.fill(delx, vertexPositionMC.z());
            delYvsVtxZ.fill(dely, vertexPositionMC.z());
            delZvsVtxZ.fill(delz, vertexPositionMC.z());
            delPvsVtxZ.fill(delPEleFit.magnitude() * Math.signum(delPEleFit.x()), vertexPositionMC.z());
            delPvsVtxZ.fill(delPPosFit.magnitude() * Math.signum(delPPosFit.x()), vertexPositionMC.z());
            delMvsVtxZ.fill(delm, vertexPositionMC.z());
            chiSqVtxZ.fill(chisq, vertexPositionMC.z());
            
             double cosOpen = getCosOpenX(pEleFit,pPosFit);     

            openAnglevsVtxZ.fill(cosOpen, vertexPositionMC.z());

            delElePxReco.fill(delPEleMC.x()/eleMC.getMomentum().magnitude(), vertexPositionMC.z());
            delElePyReco.fill(delPEleMC.y(), vertexPositionMC.z());
            delElePzReco.fill(delPEleMC.z(), vertexPositionMC.z());
            delPosPxReco.fill(delPPosMC.x()/posMC.getMomentum().magnitude(), vertexPositionMC.z());
            delPosPyReco.fill(delPPosMC.y(), vertexPositionMC.z());
            delPosPzReco.fill(delPPosMC.z(), vertexPositionMC.z());

            delElePx.fill((eleMC.getMomentum().x() - pEleFit.y())/eleMC.getMomentum().magnitude(), vertexPositionMC.z());
            delElePy.fill(eleMC.getMomentum().y() - pEleFit.z(), vertexPositionMC.z());
            delElePz.fill(eleMC.getMomentum().z() - pEleFit.x(), vertexPositionMC.z());
            delPosPx.fill((posMC.getMomentum().x() - pPosFit.y())/posMC.getMomentum().magnitude(), vertexPositionMC.z());
            delPosPy.fill(posMC.getMomentum().y() - pPosFit.z(), vertexPositionMC.z());
            delPosPz.fill(posMC.getMomentum().z() - pPosFit.x(), vertexPositionMC.z());

            // patch the track parameters at the found vertex
            if (debug)
                LOGGER.info("Patch R=(0,0,0)  ##############");
            patchVertex(electron, positron, vtxFit);
            double mPatch = vtxFit.getInvMass();
            delMvsVtxZPatch.fill(apMassMC - mPatch, vertexPositionMC.z());
            delMUncvsPatchvsZ.fill(mPatch - mUnc, vertexPositionMC.z());
//////////////////////////////              
// ok, take the initial tracks and move the reference point to Vz (==Vx in tracking frame)
            // first, get the x,y,z of the track at the perigee
//            double[] newRef = {vtxPos.z(), vtxPos.x(), vtxPos.y()};
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
            ////////////////////////////////            
//           get the new fitter
            if (debug)
                LOGGER.info("Unconstrained R=(" + newRef[0] + "," + newRef[1] + "," + newRef[2] + ") ##############");
            BilliorVertex vtxFitShift = fitVertex(Constraint.UNCONSTRAINED, electronBTrackShift, positronBTrackShift);
            Hep3Vector vtxPosShift = vtxFitShift.getPosition();
            Hep3Vector pEleFitShift = vtxFitShift.getFittedMomentum(0);
            Hep3Vector pPosFitShift = vtxFitShift.getFittedMomentum(1);
//            Hep3Vector delPEleShift = VecOp.sub(eleMC.getMomentum(), pEleFitShift);
//            Hep3Vector delPPosShift = VecOp.sub(posMC.getMomentum(), pPosFitShift);
            Hep3Vector delPEleFitShift = new BasicHep3Vector(eleMC.getMomentum().x() - pEleFitShift.y(),
                    eleMC.getMomentum().y() - pEleFitShift.z(), eleMC.getMomentum().z() - pEleFitShift.x());
            Hep3Vector delPPosFitShift = new BasicHep3Vector(posMC.getMomentum().x() - pPosFitShift.y(),
                    eleMC.getMomentum().y() - pPosFitShift.z(), posMC.getMomentum().z() - pPosFitShift.x());
            double delxShift = vertexPositionMC.x() - (vtxPosShift.x() + vtxPos.x());//add on old x-value
            double delyShift = vertexPositionMC.y() - (vtxPosShift.y());//reference isn't shifted in y
            double delzShift = vertexPositionMC.z() - (vtxPosShift.z() + vtxPos.z());//add on old z-value
            LOGGER.info("UnConstrained shifted vertexMC z=" + vertexPositionMC.z() + " re-fit vtxPos z = " + (vtxPosShift.z() + vtxPos.z()));
            LOGGER.info("delzShift = " + delzShift);
            double delmShift = apMassMC - vtxFitShift.getInvMass();
            double mUncShift = vtxFitShift.getInvMass();
            double chisqShift = vtxFitShift.getChi2();
            delXvsVtxZShift.fill(delxShift, vertexPositionMC.z());
            delYvsVtxZShift.fill(delyShift, vertexPositionMC.z());
            delZvsVtxZShift.fill(delzShift, vertexPositionMC.z());
            delPvsVtxZShift.fill(delPEleFitShift.magnitude(), vertexPositionMC.z());
            delPvsVtxZShift.fill(delPPosFitShift.magnitude(), vertexPositionMC.z());
            delMvsVtxZShift.fill(delmShift, vertexPositionMC.z());
            chiSqVtxZShift.fill(chisqShift, vertexPositionMC.z());
            delElePxShift.fill((eleMC.getMomentum().x() - pEleFitShift.y())/eleMC.getMomentum().magnitude(), vertexPositionMC.z());
            delElePyShift.fill(eleMC.getMomentum().y() - pEleFitShift.z(), vertexPositionMC.z());
            delElePzShift.fill(eleMC.getMomentum().z() - pEleFitShift.x(), vertexPositionMC.z());
            delPosPxShift.fill((posMC.getMomentum().x() - pPosFitShift.y())/posMC.getMomentum().magnitude(), vertexPositionMC.z());
            delPosPyShift.fill(posMC.getMomentum().y() - pPosFitShift.z(), vertexPositionMC.z());
            delPosPzShift.fill(posMC.getMomentum().z() - pPosFitShift.x(), vertexPositionMC.z());
            
            double cosOpenShift = getCosOpenX(pEleFitShift,pPosFitShift); 

            openAnglevsVtxZShift.fill(cosOpenShift, vertexPositionMC.z());

            //    Ok...same thing with beam-spot constrained
            if (debug)
                LOGGER.info("Constrained R=(0,0,0) ##############");
            BilliorVertex vtxFitBSC = fitVertex(Constraint.BS_CONSTRAINED, electronBTrack, positronBTrack, new BasicHep3Vector(beamPosition));
            Hep3Vector vtxPosBSC = vtxFitBSC.getPosition();
            Hep3Vector pEleFitBSC = vtxFitBSC.getFittedMomentum(0);
            Hep3Vector pPosFitBSC = vtxFitBSC.getFittedMomentum(1);
//            Hep3Vector delPEleBSC = VecOp.sub(eleMC.getMomentum(), pEleFitBSC);
//            Hep3Vector delPPosBSC = VecOp.sub(posMC.getMomentum(), pPosFitBSC);
            Hep3Vector delPEleFitBSC = new BasicHep3Vector(eleMC.getMomentum().x() - pEleFitBSC.y(),
                    eleMC.getMomentum().y() - pEleFitBSC.z(), eleMC.getMomentum().z() - pEleFitBSC.x());
            Hep3Vector delPPosFitBSC = new BasicHep3Vector(posMC.getMomentum().x() - pPosFitBSC.y(),
                    eleMC.getMomentum().y() - pPosFitBSC.z(), posMC.getMomentum().z() - pPosFitBSC.x());
            double delxBSC = vertexPositionMC.x() - (vtxPosBSC.x());
            double delyBSC = vertexPositionMC.y() - (vtxPosBSC.y());
            double delzBSC = vertexPositionMC.z() - (vtxPosBSC.z());
            LOGGER.info("Constrained R=0  vertexMC z=" + vertexPositionMC.z() + " re-fit vtxPos z = " + (vtxPosBSC.z()));
            LOGGER.info("delzBSC = " + delzBSC);
            double delmBSC = apMassMC - vtxFitBSC.getInvMass();
            double mUncBSC = vtxFitBSC.getInvMass();
            double chisqBSC = vtxFitBSC.getChi2();
            delXvsVtxZBSC.fill(delxBSC, vertexPositionMC.z());
            delYvsVtxZBSC.fill(delyBSC, vertexPositionMC.z());
            delZvsVtxZBSC.fill(delzBSC, vertexPositionMC.z());
            delPvsVtxZBSC.fill(delPEleFitBSC.magnitude(), vertexPositionMC.z());
            delPvsVtxZBSC.fill(delPPosFitBSC.magnitude(), vertexPositionMC.z());
            delMvsVtxZBSC.fill(delmBSC, vertexPositionMC.z());
            chiSqVtxZBSC.fill(chisqBSC, vertexPositionMC.z());
            delElePxBSC.fill((eleMC.getMomentum().x() - pEleFitBSC.y())/eleMC.getMomentum().magnitude(), vertexPositionMC.z());
            delElePyBSC.fill(eleMC.getMomentum().y() - pEleFitBSC.z(), vertexPositionMC.z());
            delElePzBSC.fill(eleMC.getMomentum().z() - pEleFitBSC.x(), vertexPositionMC.z());
            delPosPxBSC.fill((posMC.getMomentum().x() - pPosFitBSC.y())/posMC.getMomentum().magnitude(), vertexPositionMC.z());
            delPosPyBSC.fill(posMC.getMomentum().y() - pPosFitBSC.z(), vertexPositionMC.z());
            delPosPzBSC.fill(posMC.getMomentum().z() - pPosFitBSC.x(), vertexPositionMC.z());
             double cosOpenBSC = getCosOpenX(pEleFitBSC,pPosFitBSC); 
            openAnglevsVtxZBSC.fill(cosOpenBSC, vertexPositionMC.z());

            //    Ok...same thing with beam-spot constrained
            Hep3Vector beamRelToVtx = new BasicHep3Vector(-vtxPos.z() + beamPosition[0], -vtxPos.x() + beamPosition[1], -vtxPos.y() + beamPosition[2]);
            if (debug)
                LOGGER.info("Constrained R=(" + newRef[0] + "," + newRef[1] + "," + newRef[2] + ") ##############");
            BilliorVertex vtxFitBSCShift = fitVertex(Constraint.BS_CONSTRAINED, electronBTrackShift, positronBTrackShift, beamRelToVtx);
            Hep3Vector vtxPosBSCShift = vtxFitBSCShift.getPosition();
            Hep3Vector pEleFitBSCShift = vtxFitBSCShift.getFittedMomentum(0);
            Hep3Vector pPosFitBSCShift = vtxFitBSCShift.getFittedMomentum(1);
//            Hep3Vector delPEleBSCShift = VecOp.sub(eleMC.getMomentum(), pEleFitBSCShift);
//            Hep3Vector delPPosBSCShift = VecOp.sub(posMC.getMomentum(), pPosFitBSCShift);
            Hep3Vector delPEleFitBSCShift = new BasicHep3Vector(eleMC.getMomentum().x() - pEleFitBSCShift.y(),
                    eleMC.getMomentum().y() - pEleFitBSCShift.z(), eleMC.getMomentum().z() - pEleFitBSCShift.x());
            Hep3Vector delPPosFitBSCShift = new BasicHep3Vector(posMC.getMomentum().x() - pPosFitBSCShift.y(),
                    eleMC.getMomentum().y() - pPosFitBSCShift.z(), posMC.getMomentum().z() - pPosFitBSCShift.x());
            double delxBSCShift = vertexPositionMC.x() - (vtxPosBSCShift.x() + vtxPos.x());//add on old z-value
            double delyBSCShift = vertexPositionMC.y() - (vtxPosBSCShift.y());//reference isn't shifted in y
            double delzBSCShift = vertexPositionMC.z() - (vtxPosBSCShift.z() + vtxPos.z());//add on old z-value
            LOGGER.info("Constrained shifted vertexMC z=" + vertexPositionMC.z() + " re-fit vtxPos z = " + (vtxPosBSCShift.z() + vtxPos.z()));
            LOGGER.info("delzBSCShif = " + delzBSCShift);
            double delmBSCShift = apMassMC - vtxFitBSCShift.getInvMass();
            double mUncBSCShift = vtxFitBSCShift.getInvMass();
            double chisqBSCShift = vtxFitBSCShift.getChi2();
            delXvsVtxZBSCShift.fill(delxBSCShift, vertexPositionMC.z());
            delYvsVtxZBSCShift.fill(delyBSCShift, vertexPositionMC.z());
            delZvsVtxZBSCShift.fill(delzBSCShift, vertexPositionMC.z());
            delPvsVtxZBSCShift.fill(delPEleFitBSCShift.magnitude(), vertexPositionMC.z());
            delPvsVtxZBSCShift.fill(delPPosFitBSCShift.magnitude(), vertexPositionMC.z());
            delMvsVtxZBSCShift.fill(delmBSCShift, vertexPositionMC.z());
            chiSqVtxZBSCShift.fill(chisqBSCShift, vertexPositionMC.z());
            delElePxBSCShift.fill((eleMC.getMomentum().x() - pEleFitBSCShift.y())/eleMC.getMomentum().magnitude(), vertexPositionMC.z());
            delElePyBSCShift.fill(eleMC.getMomentum().y() - pEleFitBSCShift.z(), vertexPositionMC.z());
            delElePzBSCShift.fill(eleMC.getMomentum().z() - pEleFitBSCShift.x(), vertexPositionMC.z());
            delPosPxBSCShift.fill((posMC.getMomentum().x() - pPosFitBSCShift.y())/posMC.getMomentum().magnitude(), vertexPositionMC.z());
            delPosPyBSCShift.fill(posMC.getMomentum().y() - pPosFitBSCShift.z(), vertexPositionMC.z());
            delPosPzBSCShift.fill(posMC.getMomentum().z() - pPosFitBSCShift.x(), vertexPositionMC.z());

             double cosOpenBSCShift = getCosOpenX(pEleFitBSCShift,pPosFitBSCShift); 
            openAnglevsVtxZBSCShift.fill(cosOpenBSCShift, vertexPositionMC.z());

            delZShiftMinusNoShiftvsVtxZBSC.fill(delzBSC - delzBSCShift, vertexPositionMC.z());
            delZShiftMinusNoShiftvsVtxZUnc.fill(delz - delzShift, vertexPositionMC.z());

            delXShiftMinusNoShiftvsVtxZBSC.fill(delxBSC - delxBSCShift, vertexPositionMC.z());
            delXShiftMinusNoShiftvsVtxZUnc.fill(delx - delxShift, vertexPositionMC.z());

            delYShiftMinusNoShiftvsVtxZBSC.fill(delyBSC - delyBSCShift, vertexPositionMC.z());
            delYShiftMinusNoShiftvsVtxZUnc.fill(dely - delyShift, vertexPositionMC.z());

            delXShiftMinusNoShiftvsdelYBSC.fill(delxBSC - delxBSCShift, delyBSC - delyBSCShift);
            delXShiftMinusNoShiftvsdelYUnc.fill(delx - delxShift, dely - delyShift);

            delYShiftMinusNoShiftvsdelZBSC.fill(delyBSC - delyBSCShift, delzBSC - delzBSCShift);
            delYShiftMinusNoShiftvsdelZUnc.fill(dely - delyShift, delz - delzShift);

            delXShiftMinusNoShiftvsdelZBSC.fill(delxBSC - delxBSCShift, delzBSC - delzBSCShift);
            delXShiftMinusNoShiftvsdelZUnc.fill(delx - delxShift, delz - delzShift);

            delMShiftMinusNoShiftvsVtxZBSC.fill(delmBSC - delmBSCShift, vertexPositionMC.z());
            delMShiftMinusNoShiftvsVtxZUnc.fill(delm - delmShift, vertexPositionMC.z());

            delChiSqShiftMinusNoShiftvsVtxZBSC.fill(chisqBSC - chisqBSCShift, vertexPositionMC.z());
            delChiSqShiftMinusNoShiftvsVtxZUnc.fill(chisq - chisqShift, vertexPositionMC.z());

            if (Math.abs(dely - delyShift) < 0.005)
                if (eleMC.getMomentum().y() > 0) {
                    delXvsVtxZeleTopGood.fill(delx, vertexPositionMC.z());
                    delYvsVtxZeleTopGood.fill(dely, vertexPositionMC.z());
                    delZvsVtxZeleTopGood.fill(delz, vertexPositionMC.z());
                    delPvsVtxZeleTopGood.fill(delPEleFit.magnitude() * Math.signum(delPEleFit.x()), vertexPositionMC.z());
                    delPvsVtxZeleTopGood.fill(delPPosFit.magnitude() * Math.signum(delPPosFit.x()), vertexPositionMC.z());
                    delMvsVtxZeleTopGood.fill(delm, vertexPositionMC.z());
                    chiSqVtxZeleTopGood.fill(chisq, vertexPositionMC.z());

                    delXvsVtxZeleTopGoodShift.fill(delxShift, vertexPositionMC.z());
                    delYvsVtxZeleTopGoodShift.fill(delyShift, vertexPositionMC.z());
                    delZvsVtxZeleTopGoodShift.fill(delzShift, vertexPositionMC.z());
                    delPvsVtxZeleTopGoodShift.fill(delPEleFitShift.magnitude() * Math.signum(delPEleFitShift.x()), vertexPositionMC.z());
                    delPvsVtxZeleTopGoodShift.fill(delPPosFitShift.magnitude() * Math.signum(delPPosFitShift.x()), vertexPositionMC.z());
                    delMvsVtxZeleTopGoodShift.fill(delmShift, vertexPositionMC.z());
                    chiSqVtxZeleTopGoodShift.fill(chisqShift, vertexPositionMC.z());
                } else {
                    delXvsVtxZposTopGood.fill(delx, vertexPositionMC.z());
                    delYvsVtxZposTopGood.fill(dely, vertexPositionMC.z());
                    delZvsVtxZposTopGood.fill(delz, vertexPositionMC.z());
                    delPvsVtxZposTopGood.fill(delPEleFit.magnitude() * Math.signum(delPEleFit.x()), vertexPositionMC.z());
                    delPvsVtxZposTopGood.fill(delPPosFit.magnitude() * Math.signum(delPPosFit.x()), vertexPositionMC.z());
                    delMvsVtxZposTopGood.fill(delm, vertexPositionMC.z());
                    chiSqVtxZposTopGood.fill(chisq, vertexPositionMC.z());

                    delXvsVtxZposTopGoodShift.fill(delxShift, vertexPositionMC.z());
                    delYvsVtxZposTopGoodShift.fill(delyShift, vertexPositionMC.z());
                    delZvsVtxZposTopGoodShift.fill(delzShift, vertexPositionMC.z());
                    delPvsVtxZposTopGoodShift.fill(delPEleFitShift.magnitude() * Math.signum(delPEleFitShift.x()), vertexPositionMC.z());
                    delPvsVtxZposTopGoodShift.fill(delPPosFitShift.magnitude() * Math.signum(delPPosFitShift.x()), vertexPositionMC.z());
                    delMvsVtxZposTopGoodShift.fill(delmShift, vertexPositionMC.z());
                    chiSqVtxZposTopGoodShift.fill(chisqShift, vertexPositionMC.z());
                }
            else if (eleMC.getMomentum().y() > 0) {
                delXvsVtxZeleTopBad.fill(delx, vertexPositionMC.z());
                delYvsVtxZeleTopBad.fill(dely, vertexPositionMC.z());
                delZvsVtxZeleTopBad.fill(delz, vertexPositionMC.z());
                delPvsVtxZeleTopBad.fill(delPEleFit.magnitude() * Math.signum(delPEleFit.x()), vertexPositionMC.z());
                delPvsVtxZeleTopBad.fill(delPPosFit.magnitude() * Math.signum(delPPosFit.x()), vertexPositionMC.z());
                delMvsVtxZeleTopBad.fill(delm, vertexPositionMC.z());
                chiSqVtxZeleTopBad.fill(chisq, vertexPositionMC.z());

                delXvsVtxZeleTopBadShift.fill(delxShift, vertexPositionMC.z());
                delYvsVtxZeleTopBadShift.fill(delyShift, vertexPositionMC.z());
                delZvsVtxZeleTopBadShift.fill(delzShift, vertexPositionMC.z());
                delPvsVtxZeleTopBadShift.fill(delPEleFitShift.magnitude() * Math.signum(delPEleFitShift.x()), vertexPositionMC.z());
                delPvsVtxZeleTopBadShift.fill(delPPosFitShift.magnitude() * Math.signum(delPPosFitShift.x()), vertexPositionMC.z());
                delMvsVtxZeleTopBadShift.fill(delmShift, vertexPositionMC.z());
                chiSqVtxZeleTopBadShift.fill(chisqShift, vertexPositionMC.z());
            } else {
                delXvsVtxZposTopBad.fill(delx, vertexPositionMC.z());
                delYvsVtxZposTopBad.fill(dely, vertexPositionMC.z());
                delZvsVtxZposTopBad.fill(delz, vertexPositionMC.z());
                delPvsVtxZposTopBad.fill(delPEleFit.magnitude() * Math.signum(delPEleFit.x()), vertexPositionMC.z());
                delPvsVtxZposTopBad.fill(delPPosFit.magnitude() * Math.signum(delPPosFit.x()), vertexPositionMC.z());
                delMvsVtxZposTopBad.fill(delm, vertexPositionMC.z());
                chiSqVtxZposTopBad.fill(chisq, vertexPositionMC.z());

                delXvsVtxZposTopBadShift.fill(delxShift, vertexPositionMC.z());
                delYvsVtxZposTopBadShift.fill(delyShift, vertexPositionMC.z());
                delZvsVtxZposTopBadShift.fill(delzShift, vertexPositionMC.z());
                delPvsVtxZposTopBadShift.fill(delPEleFitShift.magnitude() * Math.signum(delPEleFitShift.x()), vertexPositionMC.z());
                delPvsVtxZposTopBadShift.fill(delPPosFitShift.magnitude() * Math.signum(delPPosFitShift.x()), vertexPositionMC.z());
                delMvsVtxZposTopBadShift.fill(delmShift, vertexPositionMC.z());
                chiSqVtxZposTopBadShift.fill(chisqShift, vertexPositionMC.z());
            }

            if (Math.abs(dely - delyShift) < 0.005)
                if (eleMC.getMomentum().x() * eleMC.getCharge() > 0) {
                    delXvsVtxZelePxChargePosGood.fill(delx, vertexPositionMC.z());
                    delYvsVtxZelePxChargePosGood.fill(dely, vertexPositionMC.z());
                    delZvsVtxZelePxChargePosGood.fill(delz, vertexPositionMC.z());
                    delPvsVtxZelePxChargePosGood.fill(delPEleFit.magnitude() * Math.signum(delPEleFit.x()), vertexPositionMC.z());
                    delPvsVtxZelePxChargePosGood.fill(delPPosFit.magnitude() * Math.signum(delPPosFit.x()), vertexPositionMC.z());
                    delMvsVtxZelePxChargePosGood.fill(delm, vertexPositionMC.z());
                    chiSqVtxZelePxChargePosGood.fill(chisq, vertexPositionMC.z());

                    delXvsVtxZelePxChargePosGoodShift.fill(delxShift, vertexPositionMC.z());
                    delYvsVtxZelePxChargePosGoodShift.fill(delyShift, vertexPositionMC.z());
                    delZvsVtxZelePxChargePosGoodShift.fill(delzShift, vertexPositionMC.z());
                    delPvsVtxZelePxChargePosGoodShift.fill(delPEleFitShift.magnitude() * Math.signum(delPEleFitShift.x()), vertexPositionMC.z());
                    delPvsVtxZelePxChargePosGoodShift.fill(delPPosFitShift.magnitude() * Math.signum(delPPosFitShift.x()), vertexPositionMC.z());
                    delMvsVtxZelePxChargePosGoodShift.fill(delmShift, vertexPositionMC.z());
                    chiSqVtxZelePxChargePosGoodShift.fill(chisqShift, vertexPositionMC.z());
                } else {
                    delXvsVtxZelePxChargeNegGood.fill(delx, vertexPositionMC.z());
                    delYvsVtxZelePxChargeNegGood.fill(dely, vertexPositionMC.z());
                    delZvsVtxZelePxChargeNegGood.fill(delz, vertexPositionMC.z());
                    delPvsVtxZelePxChargeNegGood.fill(delPEleFit.magnitude() * Math.signum(delPEleFit.x()), vertexPositionMC.z());
                    delPvsVtxZelePxChargeNegGood.fill(delPPosFit.magnitude() * Math.signum(delPPosFit.x()), vertexPositionMC.z());
                    delMvsVtxZelePxChargeNegGood.fill(delm, vertexPositionMC.z());
                    chiSqVtxZelePxChargeNegGood.fill(chisq, vertexPositionMC.z());

                    delXvsVtxZelePxChargeNegGoodShift.fill(delxShift, vertexPositionMC.z());
                    delYvsVtxZelePxChargeNegGoodShift.fill(delyShift, vertexPositionMC.z());
                    delZvsVtxZelePxChargeNegGoodShift.fill(delzShift, vertexPositionMC.z());
                    delPvsVtxZelePxChargeNegGoodShift.fill(delPEleFitShift.magnitude() * Math.signum(delPEleFitShift.x()), vertexPositionMC.z());
                    delPvsVtxZelePxChargeNegGoodShift.fill(delPPosFitShift.magnitude() * Math.signum(delPPosFitShift.x()), vertexPositionMC.z());
                    delMvsVtxZelePxChargeNegGoodShift.fill(delmShift, vertexPositionMC.z());
                    chiSqVtxZelePxChargeNegGoodShift.fill(chisqShift, vertexPositionMC.z());
                }
            else if (eleMC.getMomentum().x() * eleMC.getCharge() > 0) {
                delXvsVtxZelePxChargePosBad.fill(delx, vertexPositionMC.z());
                delYvsVtxZelePxChargePosBad.fill(dely, vertexPositionMC.z());
                delZvsVtxZelePxChargePosBad.fill(delz, vertexPositionMC.z());
                delPvsVtxZelePxChargePosBad.fill(delPEleFit.magnitude() * Math.signum(delPEleFit.x()), vertexPositionMC.z());
                delPvsVtxZelePxChargePosBad.fill(delPPosFit.magnitude() * Math.signum(delPPosFit.x()), vertexPositionMC.z());
                delMvsVtxZelePxChargePosBad.fill(delm, vertexPositionMC.z());
                chiSqVtxZelePxChargePosBad.fill(chisq, vertexPositionMC.z());

                delXvsVtxZelePxChargePosBadShift.fill(delxShift, vertexPositionMC.z());
                delYvsVtxZelePxChargePosBadShift.fill(delyShift, vertexPositionMC.z());
                delZvsVtxZelePxChargePosBadShift.fill(delzShift, vertexPositionMC.z());
                delPvsVtxZelePxChargePosBadShift.fill(delPEleFitShift.magnitude() * Math.signum(delPEleFitShift.x()), vertexPositionMC.z());
                delPvsVtxZelePxChargePosBadShift.fill(delPPosFitShift.magnitude() * Math.signum(delPPosFitShift.x()), vertexPositionMC.z());
                delMvsVtxZelePxChargePosBadShift.fill(delmShift, vertexPositionMC.z());
                chiSqVtxZelePxChargePosBadShift.fill(chisqShift, vertexPositionMC.z());
            } else {
                delXvsVtxZelePxChargeNegBad.fill(delx, vertexPositionMC.z());
                delYvsVtxZelePxChargeNegBad.fill(dely, vertexPositionMC.z());
                delZvsVtxZelePxChargeNegBad.fill(delz, vertexPositionMC.z());
                delPvsVtxZelePxChargeNegBad.fill(delPEleFit.magnitude() * Math.signum(delPEleFit.x()), vertexPositionMC.z());
                delPvsVtxZelePxChargeNegBad.fill(delPPosFit.magnitude() * Math.signum(delPPosFit.x()), vertexPositionMC.z());
                delMvsVtxZelePxChargeNegBad.fill(delm, vertexPositionMC.z());
                chiSqVtxZelePxChargeNegBad.fill(chisq, vertexPositionMC.z());

                delXvsVtxZelePxChargeNegBadShift.fill(delxShift, vertexPositionMC.z());
                delYvsVtxZelePxChargeNegBadShift.fill(delyShift, vertexPositionMC.z());
                delZvsVtxZelePxChargeNegBadShift.fill(delzShift, vertexPositionMC.z());
                delPvsVtxZelePxChargeNegBadShift.fill(delPEleFitShift.magnitude() * Math.signum(delPEleFitShift.x()), vertexPositionMC.z());
                delPvsVtxZelePxChargeNegBadShift.fill(delPPosFitShift.magnitude() * Math.signum(delPPosFitShift.x()), vertexPositionMC.z());
                delMvsVtxZelePxChargeNegBadShift.fill(delmShift, vertexPositionMC.z());
                chiSqVtxZelePxChargeNegBadShift.fill(chisqShift, vertexPositionMC.z());
            }

            try {
                writer.write(event);
            } catch (IOException ex) {
                Logger.getLogger(VertexDebugger.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    private void setupWriter() {
        // Cleanup existing writer.
        if (writer != null)
            try {
                writer.flush();
                writer.close();
                writer = null;
            } catch (IOException x) {
                System.err.println(x.getMessage());
            }

        // Setup new writer.
        try {
            writer = new LCIOWriter(outputFile);
        } catch (IOException x) {
            throw new RuntimeException("Error creating writer", x);
        }

        try {
            writer.reOpen();
        } catch (IOException x) {
            throw new RuntimeException("Error rewinding LCIO file", x);
        }
    }

    private void setupNoTrackWriter() {
        // Cleanup existing writer.
        if (notrackwriter != null)
            try {
                notrackwriter.flush();
                notrackwriter.close();
                notrackwriter = null;
            } catch (IOException x) {
                System.err.println(x.getMessage());
            }

        // Setup new writer.
        try {
            notrackwriter = new LCIOWriter(notrackFile);
        } catch (IOException x) {
            throw new RuntimeException("Error creating writer", x);
        }

        try {
            notrackwriter.reOpen();
        } catch (IOException x) {
            throw new RuntimeException("Error rewinding LCIO file", x);
        }
    }

    protected void startOfData() {
        setupWriter();
//        setupNoTrackWriter();
    }

    @Override
    public void endOfData() {
//        try {
//            writer.close();
//        } catch (IOException x) {
//            throw new RuntimeException("Error rewinding LCIO file", x);
//        }
//        try {
//            notrackwriter.close();
//        } catch (IOException x) {
//            throw new RuntimeException("Error rewinding LCIO file", x);
//        }
    }

    private BilliorVertex fitVertex(Constraint constraint, BilliorTrack electron, BilliorTrack positron) {
        return fitVertex(constraint, electron, positron, null);
    }

    private BilliorVertex fitVertex(Constraint constraint, BilliorTrack electron, BilliorTrack positron, Hep3Vector v0) {
        // Create a vertex fitter from the magnetic field.

        BilliorVertexer vtxFitter = new BilliorVertexer(B_FIELD);
        // TODO: The beam size should come from the conditions database.
        vtxFitter.setBeamSize(beamSize);
//        vtxFitter.setBeamPosition(beamPosition);

        vtxFitter.setDebug(true);

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
    
    private double getCosOpenX(Hep3Vector p1, Hep3Vector p2){   // p vectors are in tracking frame...return open angle in X-detector!!!       
         Hep3Vector pEleX=new BasicHep3Vector(p1.y(), 0.0, p1.x());
             Hep3Vector pPosX=new BasicHep3Vector(p2.y(), 0.0, p2.x());
          return Math.acos(VecOp.dot(pEleX,pPosX)/(pEleX.magnitude()*pPosX.magnitude()));
    }

}
