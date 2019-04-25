package org.hps.recon.particle;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.BasicHepLorentzVector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.HepLorentzVector;
import hep.physics.vec.VecOp;
import static java.lang.Math.sqrt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.Vertex;
import org.lcsim.event.base.BaseReconstructedParticle;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.vertexing.BilliorTrack;
import org.hps.recon.vertexing.BilliorVertex;
import org.hps.recon.vertexing.BilliorVertexer;
import org.hps.record.StandardCuts;
import org.lcsim.event.TrackState;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelixUtils;

/**
 * The main HPS implementation of ReconParticleDriver. Method generates V0
 * candidates and does vertex fits.
 *
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public class HpsReconParticleDriver extends ReconParticleDriver {

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
    ;
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

    private boolean makeConversionCols = true;
    private boolean makeMollerCols = true;
    private boolean includeUnmatchedTracksInFSP = true;
    // for the Pass2 reconstruction of 2016 data, use hard-coded values for the beamspot X-Y 
    private boolean useInternalVertexXYPositions = false;
    private Map<Integer, double[]> beamPositionMap;
    private double[] beamPositionToUse = new double[3];

    /**
     * Represents a type of constraint for vertex fitting.
     *
     * @author Omar Moreno <omoreno1@ucsc.edu>
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
        TARGET_CONSTRAINED
    }
    // #dof for fit for each Constraint
    private static final int[] DOF = {1, 3, 4};

    private boolean _patchVertexTrackParameters = false;
    private boolean _storeCovTrkMomList = false;

    public HpsReconParticleDriver() {
        super();
        beamPositionMap = new HashMap<Integer, double[]>();
        // now populate it
        // Note that the vertexing code uses the tracking frame coordinates
        // HPS X => TRACK Y
        // HPS Y => TRACK Z
        // HPS Z => TRACK X
        // 20190111 Values from Matt Solt's analysis of tuple output from Pass2
        //
        // For completeness, I am including the run-by-run z position as weel. Later we reset this to one
        // global target z position for all of 2016
        beamPositionMap.put(7629, new double[]{-4.17277481802, -0.12993997991, -0.0853344591497});
        beamPositionMap.put(7630, new double[]{-4.14431582882, -0.131667930281, -0.0818403429116});
        beamPositionMap.put(7636, new double[]{-4.21047915591, -0.133674849016, -0.089578068184});
        beamPositionMap.put(7637, new double[]{-4.24645776407, -0.101418909471, -0.0910518478041});
        beamPositionMap.put(7644, new double[]{-4.17901911124, -0.130285615341, -0.0822733438671});
        beamPositionMap.put(7653, new double[]{-4.17260490817, -0.131034318083, -0.0791072417695});
        beamPositionMap.put(7779, new double[]{-4.1787064368, -0.14063872959, -0.0964567926519});
        beamPositionMap.put(7780, new double[]{-4.1728601751, -0.138420200972, -0.0946284667682});
        beamPositionMap.put(7781, new double[]{-4.16985657657, -0.156226289295, -0.0968035162023});
        beamPositionMap.put(7782, new double[]{-4.18257346152, -0.140219484074, -0.109736506045});
        beamPositionMap.put(7783, new double[]{-4.18257346152, -0.140219484074, -0.109736506045});
        beamPositionMap.put(7786, new double[]{-4.12972261841, -0.166377573933, -0.0970139372611});
        beamPositionMap.put(7795, new double[]{-4.21859751579, -0.144244767944, -0.0853166371595});
        beamPositionMap.put(7796, new double[]{-4.20194805564, -0.143712797497, -0.0814142818837});
        beamPositionMap.put(7798, new double[]{-4.23579296792, -0.145096101419, -0.0763395379254});
        beamPositionMap.put(7799, new double[]{-4.21915161348, -0.150063795069, -0.0747834605672});
        beamPositionMap.put(7800, new double[]{-4.21341596102, -0.148070389758, -0.0759533031441});
        beamPositionMap.put(7801, new double[]{-4.22235421469, -0.152015276101, -0.0771084658072});
        beamPositionMap.put(7803, new double[]{-4.34166909052, -0.142164101651, -0.0737517199669});
        beamPositionMap.put(7804, new double[]{-4.32755215514, -0.142501982627, -0.0736984742692});
        beamPositionMap.put(7805, new double[]{-4.34001918433, -0.14128234629, -0.0719415420433});
        beamPositionMap.put(7807, new double[]{-4.2913881367, -0.146538069491, -0.0713110421539});
        beamPositionMap.put(7947, new double[]{-4.11570919432, -0.0910859069129, -0.115216742215});
        beamPositionMap.put(7948, new double[]{-4.15441978567, -0.0686135478054, -0.129060986622});
        beamPositionMap.put(7949, new double[]{-4.15625618167, -0.0732414149365, -0.133909636515});
        beamPositionMap.put(7953, new double[]{-4.1247535341, -0.0498468870412, -0.136667869602});
        beamPositionMap.put(7962, new double[]{-4.18892745552, -0.0888237919098, -0.133084782275});
        beamPositionMap.put(7963, new double[]{-4.21544617772, -0.0746121484095, -0.138791648195});
        beamPositionMap.put(7964, new double[]{-4.22151568434, -0.0767100152439, -0.138029976144});
        beamPositionMap.put(7965, new double[]{-4.21982078088, -0.0633124399662, -0.13548854});
        beamPositionMap.put(7966, new double[]{-4.22967441763, -0.074293601613, -0.136050581329});
        beamPositionMap.put(7968, new double[]{-4.24234161374, -0.0898360310379, -0.1348398567});
        beamPositionMap.put(7969, new double[]{-4.27252423462, -0.0870002501041, -0.133719459818});
        beamPositionMap.put(7970, new double[]{-4.26842346064, -0.0857240792101, -0.116971022199});
        beamPositionMap.put(7972, new double[]{-4.33363167982, 0.0345877733342, -0.100637477098});
        beamPositionMap.put(7976, new double[]{-4.28593685326, 0.0248070264018, -0.102808747635});
        beamPositionMap.put(7982, new double[]{-4.29646985597, 0.0127599017288, -0.101991281778});
        beamPositionMap.put(7983, new double[]{-4.26170058486, 0.0189046639217, -0.107073001527});
        beamPositionMap.put(7984, new double[]{-4.27436464212, 0.0245269396206, -0.108859729825});
        beamPositionMap.put(7985, new double[]{-4.27834263863, 0.0236149343493, -0.114436070246});
        beamPositionMap.put(7986, new double[]{-4.27263205142, 0.0290293298289, -0.129868804995});
        beamPositionMap.put(7987, new double[]{-4.26816324002, 0.021546891022, -0.134582348971});
        beamPositionMap.put(7988, new double[]{-4.25916023097, 0.0288738200402, -0.134675209766});
        beamPositionMap.put(8025, new double[]{-4.26772080184, -0.0374515885847, -0.111794925212});
        beamPositionMap.put(8026, new double[]{-4.26342349557, -0.0130037595274, -0.108269292234});
        beamPositionMap.put(8027, new double[]{-4.26228873413, -0.00643925463781, -0.10675932599});
        beamPositionMap.put(8028, new double[]{-4.24130730501, -0.00747575221052, -0.102814820173});
        beamPositionMap.put(8029, new double[]{-4.22872103491, -0.00447916152702, -0.105249943381});
        beamPositionMap.put(8030, new double[]{-4.23900754195, -0.000357473510209, -0.106146355534});
        beamPositionMap.put(8031, new double[]{-4.20496275068, 0.000100290515539, -0.104054377677});
        beamPositionMap.put(8039, new double[]{-4.22124716174, -0.0011390722464, -0.101544332935});
        beamPositionMap.put(8040, new double[]{-4.2205668431, -0.00181385356273, -0.102991182594});
        beamPositionMap.put(8041, new double[]{-4.28721393166, 0.00197335438837, -0.101551648105});
        beamPositionMap.put(8043, new double[]{-4.22920389672, -0.000848565041975, -0.0995558470643});
        beamPositionMap.put(8044, new double[]{-4.22870291956, -0.00175258909226, -0.0988413432517});
        beamPositionMap.put(8045, new double[]{-4.20258011807, -0.00866029673502, -0.100045336346});
        beamPositionMap.put(8046, new double[]{-4.22290557247, -0.00729779373861, -0.100652118073});
        beamPositionMap.put(8047, new double[]{-4.14601676145, -0.0202991409332, -0.105998899874});
        beamPositionMap.put(8048, new double[]{-4.20679634717, -0.00924325674287, -0.106248419021});
        beamPositionMap.put(8049, new double[]{-4.21496917922, -0.00500571957867, -0.10725973208});
        beamPositionMap.put(8051, new double[]{-4.2126457536, -0.00153038672528, -0.111092588541});
        beamPositionMap.put(8055, new double[]{-4.28341733723, 0.0206565632476, -0.115598253441});
        beamPositionMap.put(8057, new double[]{-4.2882639213, 0.00480487421616, -0.104316741434});
        beamPositionMap.put(8058, new double[]{-4.29698307957, 0.00818999458705, -0.109941003868});
        beamPositionMap.put(8059, new double[]{-4.28762465865, -0.00153129299044, -0.111814005204});
        beamPositionMap.put(8072, new double[]{-4.13924541982, 0.0180721454354, -0.113772583512});
        beamPositionMap.put(8073, new double[]{-4.15278781506, -0.00108521877967, -0.112893566712});
        beamPositionMap.put(8074, new double[]{-4.15571729252, 0.00618781078807, -0.113017354596});
        beamPositionMap.put(8075, new double[]{-4.1733104989, -0.00486744222345, -0.112424119993});
        beamPositionMap.put(8077, new double[]{-4.20683436964, 0.0110201050856, -0.109299859828});
        beamPositionMap.put(8085, new double[]{-4.13876392508, 0.0439497207201, -0.0903205833013});
        beamPositionMap.put(8086, new double[]{-4.16507539815, 0.0597982603734, -0.0910001508689});
        beamPositionMap.put(8087, new double[]{-4.20213132671, 0.0396348079161, -0.0784607661075});
        beamPositionMap.put(8088, new double[]{-4.23374437206, 0.0741295264942, -0.0838311072439});
        beamPositionMap.put(8090, new double[]{-4.18462908099, 0.0224605407948, -0.078660407208});
        beamPositionMap.put(8092, new double[]{-4.23292219117, 0.00789727246464, -0.0745098357754});
        beamPositionMap.put(8094, new double[]{-4.21308308691, 0.00356660582853, -0.072071620408});
        beamPositionMap.put(8095, new double[]{-4.20185037174, 0.00805359635246, -0.0747092315702});
        beamPositionMap.put(8096, new double[]{-4.23251278514, 0.00613811160073, -0.0741564828197});
        beamPositionMap.put(8097, new double[]{-4.19022011872, 0.00740408472403, -0.0735952313026});
        beamPositionMap.put(8098, new double[]{-4.20923479595, 0.00408775878779, -0.0755429310062});
        beamPositionMap.put(8099, new double[]{-4.20773101369, 0.0051498614277, -0.0797183115611});
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

    public void setUseInternalVertexXYPositions(boolean b) {
        useInternalVertexXYPositions = b;
    }
    
    public void setStoreCovTrkMomList(boolean b){
        _storeCovTrkMomList=b;
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
        beamPositionToUse = beamPosition;
        int runNumber = event.getRunNumber();
        if (useInternalVertexXYPositions && beamPositionMap.containsKey(runNumber)) {
            beamPositionToUse = beamPositionMap.get(runNumber);
            // only use one target z position
            beamPositionToUse[0] = beamPosition[0];
        }
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

        super.process(event);

        if (makeMollerCols) {
            event.put(unconstrainedMollerCandidatesColName, unconstrainedMollerCandidates, ReconstructedParticle.class,0);
            event.put(beamConMollerCandidatesColName, beamConMollerCandidates, ReconstructedParticle.class,0);
            event.put(targetConMollerCandidatesColName, targetConMollerCandidates, ReconstructedParticle.class,0);
            event.put(unconstrainedMollerVerticesColName, unconstrainedMollerVertices, Vertex.class,0);
            event.put(beamConMollerVerticesColName, beamConMollerVertices, Vertex.class,0);
            event.put(targetConMollerVerticesColName, targetConMollerVertices, Vertex.class,0);

        }
        if (makeConversionCols) {
            event.put(unconstrainedVcCandidatesColName, unconstrainedVcCandidates, ReconstructedParticle.class,0);
            event.put(unconstrainedVcVerticesColName, unconstrainedVcVertices, Vertex.class,0);
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
                // Only vertex two particles if at least one strategy found both tracks. Take out this check once we reduce the number of tracks.
                // This is dumb so I took it out. - Matt Solt
                /*if ((positron.getType() & electron.getType() & 0x1f) == 0) {
                 continue;
                 }*/

                // Make V0 candidates
                this.makeV0Candidates(electron, positron);
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

        if (topElectrons.size() > 1 || botElectrons.size() > 1) {
            return;
        }

        // Iterate over the collection of electrons and create e-e- pairs 
        for (ReconstructedParticle topElectron : topElectrons) {
            for (ReconstructedParticle botElectron : botElectrons) {
                // Don't vertex a GBL track with a SeedTrack.
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
        BilliorVertexer vtxFitter = new BilliorVertexer(bField);
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
     *
     */
    private void makeV0Candidates(ReconstructedParticle electron, ReconstructedParticle positron) {
        boolean eleIsTop = (electron.getTracks().get(0).getTrackerHits().get(0).getPosition()[2] > 0);
        boolean posIsTop = (positron.getTracks().get(0).getTrackerHits().get(0).getPosition()[2] > 0);

        if ((eleIsTop == posIsTop) && (!makeConversionCols)) {
            return;
        }

        if (electron.getClusters() == null || positron.getClusters() == null) {
            return;
        }
        if (electron.getClusters().isEmpty() || positron.getClusters().isEmpty()) {
            return;
        }
        double eleClusTime = ClusterUtilities.getSeedHitTime(electron.getClusters().get(0));
        double posClusTime = ClusterUtilities.getSeedHitTime(positron.getClusters().get(0));

        if (Math.abs(eleClusTime - posClusTime) > cuts.getMaxVertexClusterDt()) {
            return;
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
            if(constraint == Constraint.UNCONSTRAINED) continue;           // Skip the UNCONSTRAINED case, done already
            
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

            // Generate a candidate vertex and particle.
            BilliorVertex vtxFit = fitVertex(constraint, topElectron, botElectron);
            ReconstructedParticle candidate = makeReconstructedParticle(topElectron, botElectron, vtxFit);
            if (candidate.getMomentum().magnitude() > cuts.getMaxVertexP() || candidate.getMomentum().magnitude() < cuts.getMinMollerP()) {
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
            BaseTrackState oldTS = (BaseTrackState) part.getTracks().get(0).getTrackStates().get(0);
            double[] newParams = TrackUtils.getParametersAtNewRefPoint(newRef, oldTS);
            SymmetricMatrix newCov = TrackUtils.getCovarianceAtNewRefPoint(newRef, oldTS.getReferencePoint(), oldTS.getParameters(), new SymmetricMatrix(5, oldTS.getCovMatrix(), true));
            //mg...I don't like this re-casting, but toBilliorTrack only takes Track as input
            BaseTrackState newTS = new BaseTrackState(newParams, newRef, newCov.asPackedArray(true), TrackState.AtIP, bField);
            BilliorTrack electronBTrackShift = this.toBilliorTrack(newTS);
            newTrks.add(electronBTrackShift);
        }
        return newTrks;
    }

}
