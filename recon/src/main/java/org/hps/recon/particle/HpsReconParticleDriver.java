package org.hps.recon.particle;

import static java.lang.Math.sqrt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;

import org.hps.conditions.beam.BeamPosition;
import org.hps.conditions.beam.BeamPosition.BeamPositionCollection;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.vertexing.BilliorTrack;
import org.hps.recon.vertexing.BilliorVertex;
import org.hps.recon.vertexing.BilliorVertexer;
import org.hps.recon.vertexing.KalmanVertexFitterGainMatrix;
import org.hps.recon.vertexing.KalmanVertexFitterGainMatrix.TrackParams;
import org.hps.record.StandardCuts;
import org.hps.recon.tracking.TrackStateUtils;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.Vertex;
import org.lcsim.event.base.BaseReconstructedParticle;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.geometry.Detector;

import hep.physics.matrix.Matrix;
import hep.physics.matrix.SymmetricMatrix;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.BasicHepLorentzVector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.HepLorentzVector;
import hep.physics.vec.VecOp;

/**
 * The main HPS implementation of ReconParticleDriver. Method generates V0
 * candidates and does vertex fits.
 */
public class HpsReconParticleDriver extends ReconParticleDriver {

    private Logger LOGGER = Logger.getLogger(HpsReconParticleDriver.class.getPackage().getName());

    /**
     * LCIO collection name for Moller candidate particles generated without
     * constraints.
     */
    protected String unconstrainedMollerCandidatesColName = "UnconstrainedMollerCandidates";
    /**
     * LCIO collection name for Moller candidate particles generated with beam
     * spot constraints.
     */
    protected String beamConMollerCandidatesColName = "BeamspotConstrainedMollerCandidates";

    /**
     * LCIO collection name for Moller candidate particles generated with target
     * constraints.
     */
    protected String targetConMollerCandidatesColName = "TargetConstrainedMollerCandidates";
    /**
     * LCIO collection name for Moller candidate vertices generated without
     * constraints.
     */
    protected String unconstrainedMollerVerticesColName = "UnconstrainedMollerVertices";
    /**
     * LCIO collection name for Moller candidate vertices generated with beam
     * spot constraints.
     */
    protected String beamConMollerVerticesColName = "BeamspotConstrainedMollerVertices";
    /**
     * LCIO collection name for Moller candidate vertices generated with target
     * constraints.
     */
    protected String targetConMollerVerticesColName = "TargetConstrainedMollerVertices";

    /**
     * Stores reconstructed Moller candidate particles generated without
     * constraints.
     */
    protected List<ReconstructedParticle> unconstrainedMollerCandidates;
    /**
     * Stores reconstructed Moller candidate particles generated with beam spot
     * constraints.
     */
    protected List<ReconstructedParticle> beamConMollerCandidates;
    /**
     * Stores reconstructed Moller candidate particles generated with target
     * constraints.
     */
    protected List<ReconstructedParticle> targetConMollerCandidates;
    /**
     * Stores reconstructed Moller candidate vertices generated without
     * constraints.
     */
    protected List<Vertex> unconstrainedMollerVertices;
    /**
     * Stores reconstructed Moller candidate vertices generated with beam spot
     * constraints.
     */
    protected List<Vertex> beamConMollerVertices;
    /**
     * Stores reconstructed Moller candidate vertices generated with target
     * constraints.
     */
    protected List<Vertex> targetConMollerVertices;

    // converted V0 collections
    protected String unconstrainedVcCandidatesColName = null;

    protected String unconstrainedVcVerticesColName = null;

    protected List<ReconstructedParticle> unconstrainedVcCandidates;

    protected List<Vertex> unconstrainedVcVertices;
    
    // three-prong (e-e-e+) collections
    protected String  threeProngCandidatesColName = "ThreeProngCandidates";

    protected String  threeProngVerticesColName = "ThreeProngVertices";

    protected List<ReconstructedParticle> threeProngCandidates;

    protected List<Vertex> threeProngVertices;

    // Predicted track vertex collections (fit N-1 tracks, predict Nth)
    protected String threeProngPredictedEleColName = "ThreeProngPredictedEle";
    protected String threeProngPredictedPosColName = "ThreeProngPredictedPos";
    protected String threeProngPredictedRecColName = "ThreeProngPredictedRec";

    protected List<Vertex> threeProngPredictedEle;
    protected List<Vertex> threeProngPredictedPos;
    protected List<Vertex> threeProngPredictedRec;


    private boolean makeConversionCols = true;

    private boolean makeMollerCols = true;

    private boolean includeUnmatchedTracksInFSP = true;

    private boolean makeThreeProngCols = true; 
    
    /**
     * Whether to read beam positions from the conditions database. By default
     * this is turned off.
     */
    private boolean useBeamPositionConditions = false;

    /**
     * Whether to use a fixed Z position for the 2016 run. By default this is
     * turned on.
     */
    private boolean useFixedVertexZPosition = true;

    /**
     * The actual beam position passed to the vertex fitter.
     *
     * This can come from the parent class's default or steering settings, or
     * the conditions database, depending on flag settings.
     */
    private double[] beamPositionToUse = new double[3];
    private boolean requireClustersForV0 = true;
    private boolean requireClustersForThreeProng = true;

    /** Beam energy for three-prong momentum constraint (GeV). Settable from steering file. */
    private double threeProngBeamEnergy = 3.7;
    /** Beam rotation angle for three-prong momentum constraint (rad). Settable from steering file. */
    private double threeProngBeamRotAngle = -0.030;
    /** Max pairwise track time difference for three-prong candidates (ns). */
    //    private double threeProngMaxTrackDt = 2.5;
    private double threeProngMaxTrackDt = 5.0;
    /** Max pairwise cluster time difference for three-prong candidates (ns). */
    private double threeProngMaxClusterDt = 4.0;
    /** Require all three tracks in three-prong candidates to be truth matched to MC particles. */
    private boolean threeProngRequireTruthMatch = false;
    /** Set of tracks that have truth matches, populated per event. */
    private Set<Track> truthMatchedTracks = new HashSet<Track>();

    // Three-prong cutflow counters
    private long tp_nCombinations = 0;
    private long tp_nRecoilSofter = 0;
    private long tp_nTrackTypeMatch = 0;
    private long tp_nMinMomentum = 0;
    private long tp_nMaxMomentum = 0;
    private long tp_nTrackChi2 = 0;
    private long tp_nTrackTime = 0;
    private long tp_nDPhiXZ = 0;
    private long tp_nTruthMatch = 0;
    private long tp_nNotAllSameHalf = 0;
    private long tp_nClusterExists = 0;
    private long tp_nClusterRequirement = 0;
    private long tp_nClusterTime = 0;
    private long tp_nVertexP = 0;
    private long tp_nVertexChisq = 0;
    private long tp_nSaved = 0;

    private long nProcessed = 0; 
    
    /**
     * Represents a type of constraint for vertex fitting.
     *
     */
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
        TARGET_CONSTRAINED,
        /**
         * Represents a fit to 3-prong
         */
        THREEPRONG
    }
    // #dof for fit for each Constraint
    private static final int[] DOF = {1, 3, 4, 9};

    private boolean _patchVertexTrackParameters = false;
    private boolean _storeCovTrkMomList = false;

    public HpsReconParticleDriver() {
        super();
    }

    protected void detectorChanged(Detector detector) {

        // Make sure super-class setup is activated.
        super.detectorChanged(detector);

        // Setup optional usage of beam positions from database.
        final DatabaseConditionsManager mgr = DatabaseConditionsManager.getInstance();
        if (this.useBeamPositionConditions && mgr.hasConditionsRecord("beam_positions")) {
            LOGGER.config("Using beam position from conditions database");
            BeamPositionCollection beamPositions
                    = mgr.getCachedConditions(BeamPositionCollection.class, "beam_positions").getCachedData();
            BeamPosition beamPositionCond = beamPositions.get(0);
            beamPositionToUse = new double[]{
                beamPositionCond.getPositionZ(),
                beamPositionCond.getPositionX(),
                beamPositionCond.getPositionY()
            };
            if (this.useFixedVertexZPosition) {
                LOGGER.config("Using fixed Z position: " + this.beamPosition[0]);
                beamPositionToUse[0] = this.beamPosition[0];
            }
        } else {
            LOGGER.config("Using beam position from steering file or default");
            beamPositionToUse = beamPosition;
        }
        LOGGER.config("Using beam position [ Z, X, Y ]: " + String.format("[ %f, %f, %f ]",
                beamPositionToUse[0], beamPositionToUse[1], beamPositionToUse[2]));
    }

    public void setMaxMollerP(double input) {
        if (cuts == null) {
            cuts = new StandardCuts(beamEnergy);
        }
        cuts.setMaxMollerP(input);
    }

    public void setMinMollerP(double input) {
        if (cuts == null) {
            cuts = new StandardCuts(beamEnergy);
        }
        cuts.setMinMollerP(input);
    }

    public void setMaxVertexClusterDt(double input) {
        if (cuts == null) {
            cuts = new StandardCuts(beamEnergy);
        }
        cuts.setMaxVertexClusterDt(input);
    }

    public void setMaxVertexP(double input) {
        if (cuts == null) {
            cuts = new StandardCuts(beamEnergy);
        }
        cuts.setMaxVertexP(input);
    }

    public void setMinMollerChisqProb(double input) {
        if (cuts == null) {
            cuts = new StandardCuts(beamEnergy);
        }
        cuts.setMinMollerChisqProb(input);
    }

    public void setMinVertexChisqProb(double input) {
        if (cuts == null) {
            cuts = new StandardCuts(beamEnergy);
        }
        cuts.setMinVertexChisqProb(input);
    }

    public void setIncludeUnmatchedTracksInFSP(boolean setUMTrks) {
        includeUnmatchedTracksInFSP = setUMTrks;
    }

    /**
     * This method used to activate usage of the internal map of run numbers to
     * beam positions.
     *
     * Now, it activates usage of the beam positions from the conditions
     * database instead. This should result in the same behavior as before, and
     * steering files should not need to be updated (though eventually they
     * should be).
     *
     * @deprecated Use {@link #setUseBeamPositionConditions(boolean)} instead.
     */
    @Deprecated
    public void setUseInternalVertexXYPositions(boolean b) {
        // Changed this method to activate reading of conditions from the database. --JM
        this.useBeamPositionConditions = b;
        LOGGER.warning("The method HpsReconParticleDriver.setUseInternalVertexXYPositions() is deprecated.  Use setUseBeamPositionConditions() instead.");
    }

    /**
     * Set whether to use beam positions from the database.
     *
     * @param b True to use beam positions from the database
     */
    public void setUseBeamPositionConditions(boolean b) {
        this.useBeamPositionConditions = b;
    }

    public void setStoreCovTrkMomList(boolean b) {
        _storeCovTrkMomList = b;
    }

    public void setRequireClustersForV0(boolean b) {
        this.requireClustersForV0 = b;
    }

    public void setThreeProngBeamEnergy(double e) {
        this.threeProngBeamEnergy = e;
    }

    public void setThreeProngBeamRotAngle(double a) {
        this.threeProngBeamRotAngle = a;
    }

    public void setThreeProngMaxTrackDt(double dt) {
        this.threeProngMaxTrackDt = dt;
    }

    public void setThreeProngMaxClusterDt(double dt) {
        this.threeProngMaxClusterDt = dt;
    }

    public void setThreeProngRequireTruthMatch(boolean b) {
        this.threeProngRequireTruthMatch = b;
    }

    public void setUnconstrainedMollerCandidatesColName(String s)
    {
        unconstrainedMollerCandidatesColName = s;
    }
    
    public void setUnconstrainedMollerVerticesColName(String s)
    {
        unconstrainedMollerVerticesColName = s;
    }
    
    public void setBeamConMollerCandidatesColName(String s)
    {
        beamConMollerCandidatesColName = s;
    }
    
    public void setBeamConMollerVerticesColName(String s)
    {
        beamConMollerVerticesColName = s;
    }
    
    public void setTargetConMollerCandidatesColName(String s)
    {
        targetConMollerCandidatesColName = s;
    }
    
    public void setTargetConMollerVerticesColName(String s)
    {
        targetConMollerVerticesColName = s;
    }

    public void setThreeProngCandidatesColName(String s)
    {
        threeProngCandidatesColName = s;
    }

    public void setThreeProngVerticesColName(String s)
    {
        threeProngVerticesColName = s;
    }

    /**
     * Processes the track and cluster collections in the event into
     * reconstructed particles and V0 candidate particles and vertices. These
     * reconstructed particles are then stored in the event.
     *
     * @param event - The event to process.
     */
    @Override
    protected void process(EventHeader event) {
        //beamPositionToUse = beamPosition;
        /*
        int runNumber = event.getRunNumber();
        if (useInternalVertexXYPositions && beamPositionMap.containsKey(runNumber)) {
            beamPositionToUse = beamPositionMap.get(runNumber);
            // only use one target z position
            beamPositionToUse[0] = beamPosition[0];
        }
         */

        nProcessed++;
        
        if (makeMollerCols) {
            unconstrainedMollerCandidates = new ArrayList<ReconstructedParticle>();
            beamConMollerCandidates = new ArrayList<ReconstructedParticle>();
            targetConMollerCandidates = new ArrayList<ReconstructedParticle>();
            unconstrainedMollerVertices = new ArrayList<Vertex>();
            beamConMollerVertices = new ArrayList<Vertex>();
            targetConMollerVertices = new ArrayList<Vertex>();
        }

        if (makeConversionCols) {
            unconstrainedVcCandidates = new ArrayList<ReconstructedParticle>();
            unconstrainedVcVertices = new ArrayList<Vertex>();
        }

        if (makeThreeProngCols){
            threeProngCandidates = new ArrayList<ReconstructedParticle>();
            threeProngVertices = new ArrayList<Vertex>();
            threeProngPredictedEle = new ArrayList<Vertex>();
            threeProngPredictedPos = new ArrayList<Vertex>();
            threeProngPredictedRec = new ArrayList<Vertex>();
        }

        // Build truth-matched track set for this event
        truthMatchedTracks.clear();
        if (threeProngRequireTruthMatch) {
            for (String trackColName : trackCollectionNames) {
                String relColName = trackColName + "ToMCParticleRelations";
                if (event.hasCollection(LCRelation.class, relColName)) {
                    List<LCRelation> relations = event.get(LCRelation.class, relColName);
                    for (LCRelation relation : relations) {
                        if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                            truthMatchedTracks.add((Track) relation.getFrom());
                        }
                    }
                }
            }
        }

        super.process(event);

        if (makeMollerCols) {
            event.put(unconstrainedMollerCandidatesColName, unconstrainedMollerCandidates, ReconstructedParticle.class, 0);
            event.put(beamConMollerCandidatesColName, beamConMollerCandidates, ReconstructedParticle.class, 0);
            event.put(targetConMollerCandidatesColName, targetConMollerCandidates, ReconstructedParticle.class, 0);
            event.put(unconstrainedMollerVerticesColName, unconstrainedMollerVertices, Vertex.class, 0);
            event.put(beamConMollerVerticesColName, beamConMollerVertices, Vertex.class, 0);
            event.put(targetConMollerVerticesColName, targetConMollerVertices, Vertex.class, 0);

        }
        if (makeConversionCols) {
            event.put(unconstrainedVcCandidatesColName, unconstrainedVcCandidates, ReconstructedParticle.class, 0);
            event.put(unconstrainedVcVerticesColName, unconstrainedVcVertices, Vertex.class, 0);
        }

        if (makeThreeProngCols) {
            event.put(threeProngCandidatesColName, threeProngCandidates, ReconstructedParticle.class, 0);
            event.put(threeProngVerticesColName, threeProngVertices, Vertex.class, 0);
            event.put(threeProngPredictedEleColName, threeProngPredictedEle, Vertex.class, 0);
            event.put(threeProngPredictedPosColName, threeProngPredictedPos, Vertex.class, 0);
            event.put(threeProngPredictedRecColName, threeProngPredictedRec, Vertex.class, 0);
        }
    }

    public void setMakeConversionCols(boolean input) {
        makeConversionCols = input;
    }

    public void setMakeMollerCols(boolean input) {
        makeMollerCols = input;
    }

    public void setStoreVertexCovars(boolean input) {
        _storeCovTrkMomList = input;
    }

    @Override
    protected List<ReconstructedParticle> particleCuts(List<ReconstructedParticle> finalStateParticles) {
        List<ReconstructedParticle> goodFinalStateParticles = new ArrayList<ReconstructedParticle>();
        for (ReconstructedParticle part : finalStateParticles) {
            // good electrons
            if (part.getCharge() == -1) {
                if (part.getMomentum().magnitude() < cuts.getMaxElectronP()) {
                    if (includeUnmatchedTracksInFSP || part.getGoodnessOfPID() < cuts.getMaxMatchChisq()) {
                        goodFinalStateParticles.add(part);
                    }
                }
            } // good positrons
            else if (part.getCharge() == 1) {
                if (includeUnmatchedTracksInFSP || part.getGoodnessOfPID() < cuts.getMaxMatchChisq()) {
                    goodFinalStateParticles.add(part);
                }
            } // photons
            else {
                goodFinalStateParticles.add(part);
            }
        }
        return goodFinalStateParticles;
    }

    public void findV0s(List<ReconstructedParticle> electrons, List<ReconstructedParticle> positrons) {
        List<ReconstructedParticle> goodElectrons = particleCuts(electrons);
        List<ReconstructedParticle> goodPositrons = particleCuts(positrons);
        for (ReconstructedParticle positron : goodPositrons) {
            for (ReconstructedParticle electron : goodElectrons) {
                // Don't vertex a GBL track with a SeedTrack.
                if (TrackType.isGBL(positron.getType()) != TrackType.isGBL(electron.getType())) {
                    continue;
                }

                // Make V0 candidates
                try {
                    this.makeV0Candidates(electron, positron);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    System.out.println("HpsReconParticleDriver::makeV0Candidates fails:: skipping ele/pos pair.");
                    continue;
                }
            }
        }
    }

    public void findMollers(List<ReconstructedParticle> electrons) {
        List<ReconstructedParticle> topElectrons = new ArrayList<ReconstructedParticle>();
        List<ReconstructedParticle> botElectrons = new ArrayList<ReconstructedParticle>();

        for (ReconstructedParticle electron : electrons) {
            if (electron.getTracks().get(0).getTrackStates().get(0).getTanLambda() > 0) {
                topElectrons.add(electron);
            } else {
                botElectrons.add(electron);
            }
        }
// ---- This requirement is too strict, and cuts out a lot of events.
//      It can cut *all* moller events if both KF and GBL are used.
//
//        if (topElectrons.size() > 1 || botElectrons.size() > 1) {
//            return;
//        }

        // Iterate over the collection of electrons and create e-e- pairs 
        for (ReconstructedParticle topElectron : topElectrons) {
            for (ReconstructedParticle botElectron : botElectrons) {
                // Don't vertex a GBL track with a SeedTrack or KF track.
                if (TrackType.isGBL(topElectron.getType()) != TrackType.isGBL(botElectron.getType())) {
                    continue;
                }

                // Only vertex two particles if at least one strategy found both tracks. Take out this check once we reduce the number of tracks.
                //if ((topElectron.getType() & botElectron.getType() & 0x1f) == 0)
                //    continue;
                // Make Moller candidates
                this.makeMollerCandidates(topElectron, botElectron);
            }
        }
    }

    public void findThreeProngs(List<ReconstructedParticle> electrons, List<ReconstructedParticle> positrons) {
        for (ReconstructedParticle positron : positrons) {
            for (int i=0; i<electrons.size(); i++) {
                for (int k=0; k<electrons.size();k++){
                    if(i==k)// don't pick the same electron
                        continue;
                    ReconstructedParticle electron=electrons.get(i);
                    ReconstructedParticle recoil=electrons.get(k);
                    tp_nCombinations++;
                    //force the recoil to be softest electron
                    if(recoil.getMomentum().magnitude()>=electron.getMomentum().magnitude())
                        continue;
                    tp_nRecoilSofter++;
                    // Don't vertex a GBL track with a SeedTrack.  This isn't really needed anymore.
                    if (TrackType.isGBL(positron.getType()) != TrackType.isGBL(electron.getType())) {
                        continue;
                    }
                    if (TrackType.isGBL(positron.getType()) != TrackType.isGBL(recoil.getType())) {
                        continue;
                    }
                    tp_nTrackTypeMatch++;


                    // Not all in same half of detector
                    boolean eleIsTop = (electron.getTracks().get(0).getTrackStates().get(0).getTanLambda() > 0);
                    boolean posIsTop = (positron.getTracks().get(0).getTrackStates().get(0).getTanLambda() > 0);
                    boolean recIsTop = (recoil.getTracks().get(0).getTrackStates().get(0).getTanLambda() > 0);
                    boolean notAllSame = !((eleIsTop && posIsTop && recIsTop) || (!eleIsTop && !posIsTop && !recIsTop));
                    if (!notAllSame)
                        continue;
                    tp_nNotAllSameHalf++;
                    
                    // Three-prong selection cuts
                    double eleP = electron.getMomentum().magnitude();
                    double posP = positron.getMomentum().magnitude();
                    double recP = recoil.getMomentum().magnitude();

                    
                    // All tracks must have momentum > 0.4 GeV
                    if (eleP < 0.4 || posP < 0.4 || recP < 0.4)
                        continue;
                    tp_nMinMomentum++;

                    // Electron and recoil must have momentum < 2.9 GeV
                    if (eleP > 2.9 || recP > 2.9)
                        continue;
                    tp_nMaxMomentum++;

                    // All tracks must have chi2 < 50
                    if (electron.getTracks().get(0).getChi2() > 50.0 ||
                        positron.getTracks().get(0).getChi2() > 50.0 ||
                        recoil.getTracks().get(0).getChi2() > 50.0)
                        continue;
                    tp_nTrackChi2++;

                    // Track times must be within 10 ns of each other
                    // Use average tracker hit time as proxy for track time
                    double eleTime = avgHitTime(electron.getTracks().get(0));
                    double posTime = avgHitTime(positron.getTracks().get(0));
                    double recTime = avgHitTime(recoil.getTracks().get(0));
                    if (Math.abs(eleTime - posTime) > threeProngMaxTrackDt ||
                        Math.abs(eleTime - recTime) > threeProngMaxTrackDt ||
                        Math.abs(posTime - recTime) > threeProngMaxTrackDt)
                        continue;
                    tp_nTrackTime++;

                    // Require (e+e-) pair and recoil e- to be approximately back-to-back
                    // in the azimuthal angle phi = atan2(py, px).
                    // For back-to-back: phi_pair - phi_recoil ~ pi
                    // Rotate momenta to beam frame to account for beam rotation about y
                    Hep3Vector eleMom = rotateToBeamFrame(electron.getMomentum());
                    Hep3Vector posMom = rotateToBeamFrame(positron.getMomentum());
                    Hep3Vector recMom = rotateToBeamFrame(recoil.getMomentum());

                    // Pairing 1: (electron + positron) vs recoil
                    double phiPair1 = Math.atan2(eleMom.y() + posMom.y(), eleMom.x() + posMom.x());
                    double phiRec1 = Math.atan2(recMom.y(), recMom.x());
                    double dPhi1 = Math.abs(phiPair1 - phiRec1);

                    // Pairing 2: (recoil + positron) vs electron
                    double phiPair2 = Math.atan2(recMom.y() + posMom.y(), recMom.x() + posMom.x());
                    double phiEle2 = Math.atan2(eleMom.y(), eleMom.x());
                    double dPhi2 = Math.abs(phiPair2 - phiEle2);

                    // At least one pairing must be approximately back-to-back (dPhi ~ pi)
                    // Cut relaxed for now to study the distribution
                    if (Math.abs(dPhi1 - Math.PI) > 0.3 && Math.abs(dPhi2 - Math.PI) > 0.3)
                        continue;
                    tp_nDPhiXZ++;

                    // Require all three tracks to be truth matched
                    if (threeProngRequireTruthMatch) {
                        if (!truthMatchedTracks.contains(electron.getTracks().get(0)) ||
                            !truthMatchedTracks.contains(positron.getTracks().get(0)) ||
                            !truthMatchedTracks.contains(recoil.getTracks().get(0)))
                            continue;
                    }
                    tp_nTruthMatch++;

                

                    // Require cluster matching was attempted for the positron
                    if (positron.getClusters() == null)
                        continue;
                    
                    tp_nClusterExists++;


                    // Require either the electron or recoil to have a cluster (in addition to positron)
                   
                    boolean eleHasCluster = electron.getClusters() != null && !electron.getClusters().isEmpty();
                    boolean posHasCluster = !positron.getClusters().isEmpty();
                    boolean recHasCluster = recoil.getClusters() != null && !recoil.getClusters().isEmpty();

                    if (requireClustersForThreeProng && (!posHasCluster || (!eleHasCluster && !recHasCluster)))
                        continue;
                    tp_nClusterRequirement++;

                    double posClusTime=ClusterUtilities.getSeedHitTime(positron.getClusters().get(0));

                    if(Math.abs(posClusTime - posTime-cuts.getTrackClusterTimeOffset()) > 15.0)
                        continue;
                    if(Math.abs(posClusTime - eleTime-cuts.getTrackClusterTimeOffset()) > 100.0 &&
                       Math.abs(posClusTime - recTime-cuts.getTrackClusterTimeOffset()) > 100.0)
                        continue;
                    
                    tp_nClusterTime++;
                    /*
                    if (requireClustersForThreeProng) {
                        boolean failClusterTime = false;

                        if (eleHasCluster) {
                            double eleClusTime = ClusterUtilities.getSeedHitTime(electron.getClusters().get(0));
                            if (Math.abs(eleClusTime - posClusTime) > threeProngMaxClusterDt) {
                                failClusterTime = true;
                            }
                            if (!failClusterTime && recHasCluster) {
                                double recClusTime = ClusterUtilities.getSeedHitTime(recoil.getClusters().get(0));
                                if (Math.abs(recClusTime - posClusTime) > threeProngMaxClusterDt) {
                                    failClusterTime = true;
                                }
                                if (Math.abs(eleClusTime - recClusTime) > threeProngMaxClusterDt) {
                                    failClusterTime = true;
                                }
                            }
                        } else if (recHasCluster) {
                            double recClusTime = ClusterUtilities.getSeedHitTime(recoil.getClusters().get(0));
                            if (Math.abs(recClusTime - posClusTime) > threeProngMaxClusterDt) {
                                failClusterTime = true;
                            }
                        }
                        if (failClusterTime)
                            continue;
                    }
                    tp_nClusterTime++;
                    */
                    // Make three-prong candidates
                    try {
                        this.makeThreeProngCandidates(electron, positron, recoil);
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                        System.out.println("HpsReconParticleDriver::makeThreeProngCandidates fails:: skipping ele/pos pair.");
                        continue;
                    }
                }
            }
        }
    }
    /**
     * Creates reconstructed V0 candidate particles and vertices for electron
     * positron pairs using no constraints, beam constraints, and target
     * constraints. These are saved to the appropriate lists in the super class.
     *
     * @param electrons - The list of electrons.
     * @param positrons - The list of positrons.
     */
    @Override
    protected void findVertices(List<ReconstructedParticle> electrons, List<ReconstructedParticle> positrons) {

        // Iterate over the positrons and electrons to perform vertexing
        // on the pairs.
        findV0s(electrons, positrons);
        if (makeMollerCols) {
            findMollers(electrons);
        }
        if (makeThreeProngCols){
            findThreeProngs(electrons,positrons); 
        }
    }

    /**
     * Sets the default LCIO collection names if they are not already defined
     * previously.
     */
    @Override
    protected void startOfData() {
        super.startOfData();
        // If the LCIO collection names have not been defined, assign
        // them to the default names.
        if (unconstrainedVcCandidatesColName == null) {
            unconstrainedVcCandidatesColName = unconstrainedV0CandidatesColName.replaceAll("V0", "Vc");
        }
        if (unconstrainedVcVerticesColName == null) {
            unconstrainedVcVerticesColName = unconstrainedV0VerticesColName.replaceAll("V0", "Vc");
        }
    }

    /**
     * Fits a vertex from an electron/positron track pair using the indicated
     * constraint.
     *
     * @param constraint - The constraint type to use.
     * @param electron - The electron track.
     * @param positron - The positron track.
     * @return Returns the reconstructed vertex as a <code>BilliorVertex
     * </code> object. mg--8/14/17--add the displaced vertex refit for the
     * UNCONSTRAINED and BS_CONSTRAINED fits
     */
    private BilliorVertex fitVertex(Constraint constraint, ReconstructedParticle electron, ReconstructedParticle positron) {

        // Covert the tracks to BilliorTracks.
        BilliorTrack electronBTrack = toBilliorTrack(electron.getTracks().get(0));
        BilliorTrack positronBTrack = toBilliorTrack(positron.getTracks().get(0));

	// Create a vertex fitter from the magnetic field.
        // Note that the vertexing code uses the tracking frame coordinates
        // HPS X => TRACK Y
        // HPS Y => TRACK Z
        // HPS Z => TRACK X
	//first get the field @ perigee reference from the first tracks state (doesn't matter which one)
	//	double bLocal=(electron.getTracks().get(0)).getTrackStates().get(TrackState.AtPerigee).getBLocal();
	double bLocal=TrackStateUtils.getTrackStatesAtLocation(electron.getTracks().get(0),TrackState.AtPerigee).get(0).getBLocal();
	//	System.out.println("For Vertexer:  "+TrackStateUtils.getTrackStatesAtLocation(electron.getTracks().get(0),TrackState.AtPerigee).get(0).toString());
	// if we are using GBL tracks (trackType=0), set bLocal to bField (i.e. at SVT center)
	if(trackType==0)
	    bLocal=bField;

	//set up vertexer with field
	//	System.out.println("track type = "+trackType+"; setting vertex field = "+bLocal); 
        BilliorVertexer vtxFitter = new BilliorVertexer(bLocal);
        // TODO: The beam size should come from the conditions database.
        vtxFitter.setBeamSize(beamSize);
        vtxFitter.setBeamPosition(beamPositionToUse);
        vtxFitter.setStoreCovTrkMomList(_storeCovTrkMomList);
        vtxFitter.setDebug(debug);

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
        billiorTracks.add(electronBTrack);
        billiorTracks.add(positronBTrack);

        // Find a vertex based on the tracks.
        BilliorVertex vtx = vtxFitter.fitVertex(billiorTracks);

        int minLayEle = 6;
        int minLayPos = 6;
        List<TrackerHit> allTrackHits = electron.getTracks().get(0).getTrackerHits();
        for (TrackerHit temp : allTrackHits) {
            // Retrieve the sensor associated with one of the hits. This will
            // be used to retrieve the layer number
            HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) temp.getRawHits().get(0)).getDetectorElement();

            // Retrieve the layer number by using the sensor
            int layer = (sensor.getLayerNumber() + 1) / 2;
            if (layer < minLayEle) {
                minLayEle = layer;
            }
        }
        allTrackHits = positron.getTracks().get(0).getTrackerHits();
        for (TrackerHit temp : allTrackHits) {
            // Retrieve the sensor associated with one of the hits. This will
            // be used to retrieve the layer number
            HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) temp.getRawHits().get(0)).getDetectorElement();

            // Retrieve the layer number by using the sensor
            int layer = (sensor.getLayerNumber() + 1) / 2;
            if (layer < minLayPos) {
                minLayPos = layer;
            }
        }
        vtx.setLayerCode(minLayPos + minLayEle);
        vtx.setProbability(DOF[constraint.ordinal()]);

        // mg 8/14/17 
        // if this is an unconstrained or BS constrained vertex, propogate the 
        // tracks to the vertex found in previous fit and do fit again
        //  ...  this is required because the vertex fit assumes trajectories 
        // change linearly about the reference point (which we initially guess to be 
        // (0,0,0) while for long-lived decays there is significant curvature
        if (constraint == Constraint.BS_CONSTRAINED || constraint == Constraint.UNCONSTRAINED) {
            List<ReconstructedParticle> recoList = new ArrayList<ReconstructedParticle>();
            recoList.add(electron);
            recoList.add(positron);
            List<BilliorTrack> shiftedTracks = shiftTracksToVertex(recoList, vtx.getPosition());
//            if (constraint == Constraint.BS_CONSTRAINED) {
//                Hep3Vector beamRelToNewRef = new BasicHep3Vector(-vtx.getPosition().z() + beamPosition[0], -vtx.getPosition().x() + beamPosition[1], 0);
////                vtxFitter.setBeamPosition(beamRelToNewRef.v());
//                //mg 5/11/2018:  use referencePostion, separate from beam position  
//                vtxFitter.setReferencePosition(beamRelToNewRef.v());
//            }
            //mg 5/11/2018:  use referencePosition, separate from beam position  
            Hep3Vector newRefPoint = new BasicHep3Vector(vtx.getPosition().z(), vtx.getPosition().x(), 0);
            vtxFitter.setReferencePosition(newRefPoint.v());

            BilliorVertex vtxNew = vtxFitter.fitVertex(shiftedTracks);
            vtxNew.setLayerCode(vtx.getLayerCode());
            vtxNew.setProbability(DOF[constraint.ordinal()]);
            return vtxNew;
        } else {
            return vtx;
        }
    }

    /**
     * Fits a vertex from an electron/positron/recoil track trio using the 
     * three-prong constraint indicated
     *
     * @param constraint - The constraint type to use.
     * @param electron - The electron track.
     * @param positron - The positron track.
     * @param recoil - The recoil track. (which is just another electron
     * @return Returns the reconstructed vertex as a <code>BilliorVertex
     * </code> object.
     */
    private RealMatrix toRealMatrix(Matrix m) {
        int rows = m.getNRows();
        int cols = m.getNColumns();
        RealMatrix rm = new Array2DRowRealMatrix(rows, cols);
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                rm.setEntry(i, j, m.e(i, j));
        return rm;
    }

    private BilliorVertex fitVertex(Constraint constraint, ReconstructedParticle electron, ReconstructedParticle positron,  ReconstructedParticle recoil) {

        // Build TrackParams directly from TrackState helix parameters (d0, phi0, omega, z0, tanLambda)
        // NOT from BilliorTrack which uses a different parameter ordering (eps, z0, theta, phi0, curvature)
        List<TrackParams> trkParList = new ArrayList<TrackParams>();
        ReconstructedParticle[] particles = {electron, positron, recoil};
        for (ReconstructedParticle part : particles) {
            TrackState ts = TrackStateUtils.getTrackStatesAtLocation(part.getTracks().get(0), TrackState.AtPerigee).get(0);
            double[] p = ts.getParameters();
            SymmetricMatrix cov = new SymmetricMatrix(5, ts.getCovMatrix(), true);
            trkParList.add(new TrackParams(p[0], p[1], p[2], p[3], p[4], toRealMatrix(cov)));
        }

        // Get the magnetic field
        double bLocal=TrackStateUtils.getTrackStatesAtLocation(electron.getTracks().get(0),TrackState.AtPerigee).get(0).getBLocal();
        if(trackType==0)
            bLocal=bField;

        if (debug) {
            Hep3Vector eleMom = electron.getMomentum();
            Hep3Vector posMom = positron.getMomentum();
            Hep3Vector recMom = recoil.getMomentum();
            System.out.println("=== HpsReconParticleDriver::fitVertex (3-prong) ===");
            System.out.printf("  Electron ReconParticle momentum: [%.4f, %.4f, %.4f] |p|=%.4f%n",
                              eleMom.x(), eleMom.y(), eleMom.z(), eleMom.magnitude());
            System.out.printf("  Positron ReconParticle momentum: [%.4f, %.4f, %.4f] |p|=%.4f%n",
                              posMom.x(), posMom.y(), posMom.z(), posMom.magnitude());
            System.out.printf("  Recoil   ReconParticle momentum: [%.4f, %.4f, %.4f] |p|=%.4f%n",
                              recMom.x(), recMom.y(), recMom.z(), recMom.magnitude());
            double totalP = VecOp.add(eleMom, VecOp.add(posMom, recMom)).magnitude();
            System.out.printf("  Total |p| = %.4f%n", totalP);
            String[] labels = {"electron", "positron", "recoil  "};
            for (int i = 0; i < 3; i++) {
                TrackParams tp = trkParList.get(i);
                System.out.printf("  TrackParams %s: d0=%.4f phi0=%.4f omega=%.6f z0=%.4f tanL=%.4f%n",
                                  labels[i], tp.d0, tp.phi0, tp.omega, tp.z0, tp.tanLambda);
            }
            System.out.printf("  bLocal = %.4f%n", bLocal);
            System.out.println("=== End HpsReconParticleDriver input ===");
        }

        // Set up Kalman vertex fitter

        KalmanVertexFitterGainMatrix vtxFitter = new KalmanVertexFitterGainMatrix(bLocal);
        vtxFitter.setBeamSize(beamSize);
        vtxFitter.setBeamPosition(beamPositionToUse);
        vtxFitter.setStoreCovTrkMomList(_storeCovTrkMomList);
        //        vtxFitter.setDebug(debug);
        vtxFitter.setDebug(true);
        vtxFitter.setBeamEnergy(threeProngBeamEnergy);
        vtxFitter.setBeamRotAngle(threeProngBeamRotAngle);

        // Fit with beam momentum constraint for THREEPRONG, unconstrained otherwise
        boolean useBeamConstraint = (constraint == Constraint.THREEPRONG);
        System.out.println("Fitting a three prong candidate");
        BilliorVertex vtx = vtxFitter.fitVertex(trkParList, useBeamConstraint);
        System.out.println("...done fitting");

        // For THREEPRONG, also do predicted track fits for each track
        if (constraint == Constraint.THREEPRONG) {
            // Set up beam momentum constraint for predicted track fits
            // Tracking frame: X=HPS_Z (beam), Y=HPS_X (horizontal), Z=HPS_Y (vertical)
            double pBeam = threeProngBeamEnergy;  // Assume electron mass ~ 0, so p ~ E
            double cosR = Math.cos(threeProngBeamRotAngle);
            double sinR = Math.sin(threeProngBeamRotAngle);
            // Beam momentum in tracking frame (same convention as fitVertex):
            double pxBeam_trk = pBeam * cosR;   // tracking X = HPS Z
            double pyBeam_trk = -pBeam * sinR;  // tracking Y = HPS X (negative!)
            double pzBeam_trk = 0.0;            // tracking Z = HPS Y
            double[] beamP = new double[]{pxBeam_trk, pyBeam_trk, pzBeam_trk};
            RealVector totalMomentumConstraint = MatrixUtils.createRealVector(beamP);

            System.out.printf("DEBUG fitWithPredictedTrack: beam momentum (tracking) = [%.4f, %.4f, %.4f]%n",
                pxBeam_trk, pyBeam_trk, pzBeam_trk);

            // Small uncertainty on beam momentum
            double beamPSigma = 0.05;  // 50 MeV spread
            RealMatrix totalMomentumConstraintCov = MatrixUtils.createRealDiagonalMatrix(
                new double[]{beamPSigma * beamPSigma, beamPSigma * beamPSigma, beamPSigma * beamPSigma});

            // Vertex constraint from beam position
            RealVector vertexConstraint = MatrixUtils.createRealVector(beamPositionToUse);
            RealMatrix vertexConstraintCov = MatrixUtils.createRealDiagonalMatrix(
                new double[]{beamSize[0] * beamSize[0], beamSize[1] * beamSize[1], beamSize[2] * beamSize[2]});

            String[] trackLabels = new String[]{"Ele", "Pos", "Rec"};
            List<Vertex>[] predCollections = new List[]{threeProngPredictedEle, threeProngPredictedPos, threeProngPredictedRec};

            // Fit with each track predicted
            for (int predictIdx = 0; predictIdx < 3; predictIdx++) {
                KalmanVertexFitterGainMatrix.PredictedTrackResult predResult =
                    vtxFitter.fitWithPredictedTrack(
                        trkParList, predictIdx,
                        vertexConstraint, vertexConstraintCov,
                        totalMomentumConstraint, totalMomentumConstraintCov,
                        null, null,  // no mass constraint
                        10, 1e-6);

                // Convert to BilliorVertex and add to appropriate collection
                String label = trackLabels[predictIdx];
                BilliorVertex predVtx = vtxFitter.predictedResultToBilliorVertex(
                    predResult, predictIdx, "ThreeProngPredicted" + label);
                predCollections[predictIdx].add(predVtx);

                // Also store summary info in the main vertex parameters
                vtx.setParameter("p" + label + "X_pred", predResult.predictedMomentum.getEntry(0));
                vtx.setParameter("p" + label + "Y_pred", predResult.predictedMomentum.getEntry(1));
                vtx.setParameter("p" + label + "Z_pred", predResult.predictedMomentum.getEntry(2));
                vtx.setParameter("p" + label + "X_actual", predResult.actualMomentum.getEntry(0));
                vtx.setParameter("p" + label + "Y_actual", predResult.actualMomentum.getEntry(1));
                vtx.setParameter("p" + label + "Z_actual", predResult.actualMomentum.getEntry(2));
                vtx.setParameter("p" + label + "X_res", predResult.momentumResidual.getEntry(0));
                vtx.setParameter("p" + label + "Y_res", predResult.momentumResidual.getEntry(1));
                vtx.setParameter("p" + label + "Z_res", predResult.momentumResidual.getEntry(2));
                vtx.setParameter("chi2_pred" + label, predResult.chi2);
                vtx.setParameter("ndf_pred" + label, (double) predResult.ndf);

                if (debug) {
                    System.out.printf("Predicted track %s: p_pred=(%.4f, %.4f, %.4f), p_actual=(%.4f, %.4f, %.4f), chi2=%.2f%n",
                        label,
                        predResult.predictedMomentum.getEntry(0), predResult.predictedMomentum.getEntry(1), predResult.predictedMomentum.getEntry(2),
                        predResult.actualMomentum.getEntry(0), predResult.actualMomentum.getEntry(1), predResult.actualMomentum.getEntry(2),
                        predResult.chi2);
                }
            }
        }

        // Compute layer code
        int minLayEle = 6;
        int minLayPos = 6;
        List<TrackerHit> allTrackHits = electron.getTracks().get(0).getTrackerHits();
        for (TrackerHit temp : allTrackHits) {
            HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) temp.getRawHits().get(0)).getDetectorElement();
            int layer = (sensor.getLayerNumber() + 1) / 2;
            if (layer < minLayEle) {
                minLayEle = layer;
            }
        }
        allTrackHits = positron.getTracks().get(0).getTrackerHits();
        for (TrackerHit temp : allTrackHits) {
            HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) temp.getRawHits().get(0)).getDetectorElement();
            int layer = (sensor.getLayerNumber() + 1) / 2;
            if (layer < minLayPos) {
                minLayPos = layer;
            }
        }
        vtx.setLayerCode(minLayPos + minLayEle);
        vtx.setProbability(DOF[constraint.ordinal()]);

        return vtx;
    }
    /**
     *
     */
    private void makeV0Candidates(ReconstructedParticle electron, ReconstructedParticle positron) {

        //boolean eleIsTop = (electron.getTracks().get(0).getTrackerHits().get(0).getPosition()[2] > 0);
        //boolean posIsTop = (positron.getTracks().get(0).getTrackerHits().get(0).getPosition()[2] > 0);
        //This eleIsTop/posIsTop logic is valid for all track types [both Helix+GBL and Kalman]
        boolean eleIsTop = (electron.getTracks().get(0).getTrackStates().get(0).getTanLambda() > 0);
        boolean posIsTop = (positron.getTracks().get(0).getTrackStates().get(0).getTanLambda() > 0);

        if ((eleIsTop == posIsTop) && (!makeConversionCols)) {
            return;
        }
        //does this just require that cluster matching was attempted?  
        if (electron.getClusters() == null || positron.getClusters() == null) {
            return;
        }
        if (requireClustersForV0 && (electron.getClusters().isEmpty() || positron.getClusters().isEmpty())) {
            return;
        }
        if (requireClustersForV0) {
            double eleClusTime = ClusterUtilities.getSeedHitTime(electron.getClusters().get(0));
            double posClusTime = ClusterUtilities.getSeedHitTime(positron.getClusters().get(0));

            if (Math.abs(eleClusTime - posClusTime) > cuts.getMaxVertexClusterDt()) {
                return;
            }
        }
        // Handle UNCONSTRAINED case, to make decisions whether we store the vertexes.
        // This is done here so that we either store all types, or none, but never a mix.
        BilliorVertex vtxFit = fitVertex(Constraint.UNCONSTRAINED, electron, positron);

        ReconstructedParticle candidate = makeReconstructedParticle(electron, positron, vtxFit);

        if (candidate.getMomentum().magnitude() > cuts.getMaxVertexP()) {
            return;
        }

        if (candidate.getStartVertex().getProbability() < cuts.getMinVertexChisqProb()) {
            return;
        }

        // patch the track parameters at the found vertex
        if (_patchVertexTrackParameters) {
            patchVertex(vtxFit);
        }
        if (eleIsTop != posIsTop) {
            unconstrainedV0Vertices.add(vtxFit);
            unconstrainedV0Candidates.add(candidate);
        } else {
            unconstrainedVcVertices.add(vtxFit);
            unconstrainedVcCandidates.add(candidate);
        }

        // Create candidate particles for the other two constraints.
        for (Constraint constraint : Constraint.values()) {
            if (constraint == Constraint.UNCONSTRAINED || constraint == Constraint.THREEPRONG) {
                continue;
            }
            // Generate a candidate vertex and particle.
            vtxFit = fitVertex(constraint, electron, positron);

            candidate = makeReconstructedParticle(electron, positron, vtxFit);

            // Add the other candidate vertex and particle to the
            // appropriate LCIO collection.
            switch (constraint) {

                case BS_CONSTRAINED:
                    if (eleIsTop != posIsTop) {
                        beamConV0Vertices.add(vtxFit);
                        beamConV0Candidates.add(candidate);
                    }
                    break;

                case TARGET_CONSTRAINED:
                    if (eleIsTop != posIsTop) {
                        targetConV0Vertices.add(vtxFit);
                        targetConV0Candidates.add(candidate);
                    }
                    break;

            }
        }
    }

    /**
     *
     */
    private void makeMollerCandidates(ReconstructedParticle topElectron, ReconstructedParticle botElectron) {

        // Create candidate particles for each constraint.
        for (Constraint constraint : Constraint.values()) {
            if (constraint == Constraint.THREEPRONG) {
                continue;
            }

            // Generate a candidate vertex and particle.
            BilliorVertex vtxFit = fitVertex(constraint, topElectron, botElectron);
            ReconstructedParticle candidate = makeReconstructedParticle(topElectron, botElectron, vtxFit);
            if (candidate.getMomentum().magnitude() > cuts.getMaxMollerP() || candidate.getMomentum().magnitude() < cuts.getMinMollerP()) {
                continue;
            }
            if (candidate.getStartVertex().getProbability() < cuts.getMinMollerChisqProb()) {
                continue;
            }
            // Add the candidate vertex and particle to the
            // appropriate LCIO collection.
            switch (constraint) {

                case UNCONSTRAINED:
                    // patch the track parameters at the found vertex
                    if (_patchVertexTrackParameters) {
                        patchVertex(vtxFit);
                    }
                    unconstrainedMollerVertices.add(vtxFit);
                    unconstrainedMollerCandidates.add(candidate);
                    break;

                case BS_CONSTRAINED:
                    beamConMollerVertices.add(vtxFit);
                    beamConMollerCandidates.add(candidate);
                    break;

                case TARGET_CONSTRAINED:
                    targetConMollerVertices.add(vtxFit);
                    targetConMollerCandidates.add(candidate);
                    break;

            }
        }
    }
  /**
     *
     */
    private void makeThreeProngCandidates(ReconstructedParticle electron, ReconstructedParticle positron, ReconstructedParticle recoil) {

        BilliorVertex vtxFit = fitVertex(Constraint.THREEPRONG, electron, positron,recoil);

        ReconstructedParticle candidate = makeReconstructedParticle(electron, positron, recoil, vtxFit);

        if (candidate.getMomentum().magnitude() > cuts.getMaxVertexP()) {
            return;
        }
        tp_nVertexP++;

        if (candidate.getStartVertex().getProbability() < cuts.getMinVertexChisqProb()) {
            return;
        }
        tp_nVertexChisq++;

        // patch the track parameters at the found vertex
        if (_patchVertexTrackParameters) {
            patchVertex(vtxFit);
        }
        threeProngVertices.add(vtxFit);
        threeProngCandidates.add(candidate);
        tp_nSaved++;         
    }
    /**
     * Creates a reconstructed V0 candidate particle from an electron, positron,
     * and billior vertex.
     *
     * @param electron - The electron.
     * @param positron - The positron.
     * @param vtxFit - The billior vertex.
     * @return Returns a reconstructed particle with properties generated from
     * the child particles and vertex given as an argument.
     */
    public static ReconstructedParticle makeReconstructedParticle(ReconstructedParticle electron, ReconstructedParticle positron, BilliorVertex vtxFit) {

        // Create a new reconstructed particle to represent the V0
        // candidate and populate it with the electron and positron.
        ReconstructedParticle candidate = new BaseReconstructedParticle();
        ((BaseReconstructedParticle) candidate).setStartVertex(vtxFit);
        candidate.addParticle(electron);
        candidate.addParticle(positron);

        // Set the type of the V0 particle.  This will only be needed for pass 2.
        ((BaseReconstructedParticle) candidate).setType(electron.getType());

        // TODO: The calculation of the total fitted momentum should be
        //       done within BilloirVertex.
        // Calculate the candidate particle momentum and associate it
        // with the reconstructed candidate particle.
        ((BaseReconstructedParticle) candidate).setMass(vtxFit.getParameters().get("invMass"));
        Hep3Vector fittedMomentum = new BasicHep3Vector(vtxFit.getParameters().get("p1X"),
                vtxFit.getParameters().get("p1Y"),
                vtxFit.getParameters().get("p1Z"));
        fittedMomentum = VecOp.add(fittedMomentum, new BasicHep3Vector(vtxFit.getParameters().get("p2X"),
                vtxFit.getParameters().get("p2Y"),
                vtxFit.getParameters().get("p2Z")));
        //mg 10/24/2017...billiorvertex now returns momentum in JLAB frame
        //         fittedMomentum = CoordinateTransformations.transformVectorToDetector(fittedMomentum);

        // If both the electron and positron have an associated Ecal cluster,
        // calculate the total energy and assign it to the V0 particle
        double v0Energy = 0;
        if (!electron.getClusters().isEmpty() && !positron.getClusters().isEmpty()) {
            v0Energy += electron.getClusters().get(0).getEnergy();
            v0Energy += positron.getClusters().get(0).getEnergy();
        }

        HepLorentzVector fourVector = new BasicHepLorentzVector(v0Energy, fittedMomentum);
        //((BasicHepLorentzVector) fourVector).setV3(fourVector.t(), fittedMomentum);
        ((BaseReconstructedParticle) candidate).set4Vector(fourVector);

        // Set the charge of the particle
        double particleCharge = electron.getCharge() + positron.getCharge();
        ((BaseReconstructedParticle) candidate).setCharge(particleCharge);

        // VERBOSE :: Output the fitted momentum data.
//        printDebug("Fitted momentum in tracking frame: " + fittedMomentum.toString());
//        printDebug("Fitted momentum in detector frame: " + fittedMomentum.toString());
        // Add the ReconstructedParticle to the vertex.
        vtxFit.setAssociatedParticle(candidate);

        // Set the vertex position as the reference point of the V0 particle
        ((BaseReconstructedParticle) candidate).setReferencePoint(vtxFit.getPosition());

        // Return the V0 candidate.
        return candidate;
    }

     /**
     * Creates a reconstructed 3-prong candidate particle from an electron, positron, recoil
     * and billior vertex.
     *
     * @param electron - The electron.
     * @param positron - The positron.
     * @param recoil - The recoil.
     * @param vtxFit - The billior vertex.
     * @return Returns a reconstructed particle with properties generated from
     * the child particles and vertex given as an argument.
     */
    public static ReconstructedParticle makeReconstructedParticle(ReconstructedParticle electron, ReconstructedParticle positron, ReconstructedParticle recoil,BilliorVertex vtxFit) {

        // Create a new reconstructed particle to represent the V0
        // candidate and populate it with the electron and positron.
        ReconstructedParticle candidate = new BaseReconstructedParticle();
        ((BaseReconstructedParticle) candidate).setStartVertex(vtxFit);
        candidate.addParticle(electron);
        candidate.addParticle(positron);
        candidate.addParticle(recoil);

        // Set the type of the V0 particle.  This will only be needed for pass 2.
        ((BaseReconstructedParticle) candidate).setType(electron.getType());

        // TODO: The calculation of the total fitted momentum should be
        //       done within BilloirVertex.
        // Calculate the candidate particle momentum and associate it
        // with the reconstructed candidate particle.
        ((BaseReconstructedParticle) candidate).setMass(vtxFit.getParameters().get("invMass"));
        Hep3Vector fittedMomentum = new BasicHep3Vector(vtxFit.getParameters().get("p1X"),
                vtxFit.getParameters().get("p1Y"),
                vtxFit.getParameters().get("p1Z"));
        fittedMomentum = VecOp.add(fittedMomentum, new BasicHep3Vector(vtxFit.getParameters().get("p2X"),
                vtxFit.getParameters().get("p2Y"),
                vtxFit.getParameters().get("p2Z")));
        fittedMomentum = VecOp.add(fittedMomentum, new BasicHep3Vector(vtxFit.getParameters().get("p3X"),
                vtxFit.getParameters().get("p3Y"),
                vtxFit.getParameters().get("p3Z")));
        // If both the electron and positron have an associated Ecal cluster,
        // calculate the total energy and assign it to the V0 particle
        double v0Energy = 0;
        if (!electron.getClusters().isEmpty() && !positron.getClusters().isEmpty()&&!recoil.getClusters().isEmpty()) {
            v0Energy += electron.getClusters().get(0).getEnergy();
            v0Energy += positron.getClusters().get(0).getEnergy();
            v0Energy += recoil.getClusters().get(0).getEnergy();
        }

        HepLorentzVector fourVector = new BasicHepLorentzVector(v0Energy, fittedMomentum);
        //((BasicHepLorentzVector) fourVector).setV3(fourVector.t(), fittedMomentum);
        ((BaseReconstructedParticle) candidate).set4Vector(fourVector);

        // Set the charge of the particle
        double particleCharge = electron.getCharge() + positron.getCharge()+recoil.getCharge();
        ((BaseReconstructedParticle) candidate).setCharge(particleCharge);

        // VERBOSE :: Output the fitted momentum data.
//        printDebug("Fitted momentum in tracking frame: " + fittedMomentum.toString());
//        printDebug("Fitted momentum in detector frame: " + fittedMomentum.toString());
        // Add the ReconstructedParticle to the vertex.
        vtxFit.setAssociatedParticle(candidate);

        // Set the vertex position as the reference point of the V0 particle
        ((BaseReconstructedParticle) candidate).setReferencePoint(vtxFit.getPosition());

        // Return the V0 candidate.
        return candidate;
    }
    
    /**
     * Converts a <code>Track</code> object to a <code>BilliorTrack
     * </code> object.
     *
     * @param track - The original track.
     * @return Returns the original track as a <code>BilliorTrack
     * </code> object.
     */
    private BilliorTrack toBilliorTrack(Track track) {
        // Generate and return the billior track.
        return new BilliorTrack(track);
    }

    /**
     * Converts a <code>TrackState</code> object to a <code>BilliorTrack
     * </code> object.
     *
     * @param track - The original track state
     * @return Returns the original track as a <code>BilliorTrack
     * </code> object.
     */
    private BilliorTrack toBilliorTrack(TrackState trackstate) {
        // Generate and return the billior track.
        return new BilliorTrack(trackstate, 0, 0); // track state doesn't store chi^2 info (stored in the Track object)
    }

    public void setPatchVertexTrackParameters(boolean b) {
        _patchVertexTrackParameters = b;
    }

    private void patchVertex(BilliorVertex v) {
        ReconstructedParticle rp = v.getAssociatedParticle();
        List<ReconstructedParticle> parts = rp.getParticles();
        ReconstructedParticle rp1 = parts.get(0);
        ReconstructedParticle rp2 = parts.get(1);
        //p1
        Track et = rp1.getTracks().get(0);
        double etrackMom = rp1.getMomentum().magnitude();
        HelicalTrackFit ehtf = TrackUtils.getHTF(et);
        // propagate this to the vertex z position...
        // Note that HPS y is lcsim z
        double es = HelixUtils.PathToZPlane(ehtf, v.getPosition().y());
        //Hep3Vector epointOnTrackAtVtx = HelixUtils.PointOnHelix(ehtf, es);
        Hep3Vector edirOfTrackAtVtx = HelixUtils.Direction(ehtf, es);
        Hep3Vector emomAtVtx = VecOp.mult(etrackMom, VecOp.unit(edirOfTrackAtVtx));
        //p2
        Track pt = rp2.getTracks().get(0);
        double ptrackMom = rp2.getMomentum().magnitude();
        HelicalTrackFit phtf = TrackUtils.getHTF(pt);
        // propagate this to the vertex z position...
        // Note that HPS y is lcsim z
        double ps = HelixUtils.PathToZPlane(phtf, v.getPosition().y());
        //Hep3Vector ppointOnTrackAtVtx = HelixUtils.PointOnHelix(phtf, ps);
        Hep3Vector pdirOfTrackAtVtx = HelixUtils.Direction(phtf, ps);
        Hep3Vector pmomAtVtx = VecOp.mult(ptrackMom, VecOp.unit(pdirOfTrackAtVtx));

        double mass = invMass(emomAtVtx, pmomAtVtx);
        v.setVertexTrackParameters(emomAtVtx, pmomAtVtx, mass);
    }

    private double invMass(Hep3Vector p1, Hep3Vector p2) {
        double me2 = 0.000511 * 0.000511;
        double esum = sqrt(p1.magnitudeSquared() + me2) + sqrt(p2.magnitudeSquared() + me2);
        double pxsum = p1.x() + p2.x();
        double pysum = p1.y() + p2.y();
        double pzsum = p1.z() + p2.z();

        double psum = Math.sqrt(pxsum * pxsum + pysum * pysum + pzsum * pzsum);
        double evtmass = esum * esum - psum * psum;

        if (evtmass > 0) {
            return Math.sqrt(evtmass);
        } else {
            return -99;
        }
    }

    private List<BilliorTrack> shiftTracksToVertex(List<ReconstructedParticle> particles, Hep3Vector vtxPos) {
        ///     Ok...shift the reference point....        
        double[] newRef = {vtxPos.z(), vtxPos.x(), 0.0};//the  TrackUtils.getParametersAtNewRefPoint method only shifts in xy tracking frame
        List<BilliorTrack> newTrks = new ArrayList<BilliorTrack>();
        for (ReconstructedParticle part : particles) {
	    BaseTrackState oldTS=(BaseTrackState)TrackStateUtils.getTrackStatesAtLocation(part.getTracks().get(0),TrackState.AtPerigee).get(0);
	    //	    BaseTrackState oldTS = (BaseTrackState) part.getTracks().get(0).getTrackStates().get(TrackState.AtPerigee);
            double[] newParams = TrackUtils.getParametersAtNewRefPoint(newRef, oldTS);
            SymmetricMatrix newCov = TrackUtils.getCovarianceAtNewRefPoint(newRef, oldTS.getReferencePoint(), oldTS.getParameters(), new SymmetricMatrix(5, oldTS.getCovMatrix(), true));
            //mg...I don't like this re-casting, but toBilliorTrack only takes Track as input
	    //make the state with bfield = field at original z-position because TrackUtils.getParametersAtNewRefPoint does not change curvature!
            BaseTrackState newTS = new BaseTrackState(newParams, newRef, newCov.asPackedArray(true), TrackState.AtPerigee, oldTS.getBLocal());
            BilliorTrack electronBTrackShift = this.toBilliorTrack(newTS);
            newTrks.add(electronBTrackShift);
        }
        return newTrks;
    }

    /**
     * Compute average tracker hit time for a track.
     */
    @Override
    protected void endOfData() {
        super.endOfData();
        System.out.printf("%-40s %10d%n", "number of processed events", nProcessed);
        System.out.println("\n===== Three-Prong Cutflow Summary =====");
        System.out.printf("%-40s %10d%n", "Total combinations", tp_nCombinations);
        System.out.printf("%-40s %10d%n", "Recoil softer than electron", tp_nRecoilSofter);
        System.out.printf("%-40s %10d%n", "Track type match", tp_nTrackTypeMatch);
        System.out.printf("%-40s %10d%n", "Not all same half", tp_nNotAllSameHalf);
        System.out.printf("%-40s %10d%n", "Min momentum (> 0.4 GeV)", tp_nMinMomentum);
        System.out.printf("%-40s %10d%n", "Max momentum (ele/rec < 2.9 GeV)", tp_nMaxMomentum);
        System.out.printf("%-40s %10d%n", "Track chi2 (< 30)", tp_nTrackChi2);
        System.out.printf("%-40s %10d%n", String.format("Track time (< %.1f ns)", threeProngMaxTrackDt), tp_nTrackTime);
        System.out.printf("%-40s %10d%n", "DPhi_xz back-to-back", tp_nDPhiXZ);
        System.out.printf("%-40s %10d%n", "Truth match (all 3 tracks)", tp_nTruthMatch);
        System.out.printf("%-40s %10d%n", "Cluster exists (pos not null)", tp_nClusterExists);
        System.out.printf("%-40s %10d%n", "Cluster requirement (pos + ele/rec)", tp_nClusterRequirement);
        System.out.printf("%-40s %10d%n", String.format("Cluster time cut (< %.1f ns)", threeProngMaxClusterDt), tp_nClusterTime);
        System.out.printf("%-40s %10d%n", "Vertex momentum cut", tp_nVertexP);
        System.out.printf("%-40s %10d%n", "Vertex chi2 prob cut", tp_nVertexChisq);
        System.out.printf("%-40s %10d%n", "Saved candidates", tp_nSaved);
        System.out.println("========================================\n");
    }

    private double avgHitTime(Track track) {
        List<TrackerHit> hits = track.getTrackerHits();
        if (hits == null || hits.isEmpty())
            return 0.0;
        double sum = 0.0;
        for (TrackerHit hit : hits) {
            sum += hit.getTime();
        }
        return sum / hits.size();
    }

    /**
     * Rotate a vector about the y-axis by the beam rotation angle.
     * This transforms from detector coordinates to beam coordinates.
     */
    private Hep3Vector rotateToBeamFrame(Hep3Vector v) {
        double cosTheta = Math.cos(threeProngBeamRotAngle);
        double sinTheta = Math.sin(threeProngBeamRotAngle);
        double xRot = v.x() * cosTheta + v.z() * sinTheta;
        double yRot = v.y();
        double zRot = -v.x() * sinTheta + v.z() * cosTheta;
        return new BasicHep3Vector(xRot, yRot, zRot);
    }

}
