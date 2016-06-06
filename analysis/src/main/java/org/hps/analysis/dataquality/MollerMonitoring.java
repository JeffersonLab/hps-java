package org.hps.analysis.dataquality;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Logger;
import org.hps.recon.particle.HpsReconParticleDriver;
import org.hps.recon.particle.ReconParticleDriver;
import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.vertexing.BilliorTrack;
import org.hps.recon.vertexing.BilliorVertex;
import org.hps.recon.vertexing.BilliorVertexer;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.Vertex;
import org.lcsim.geometry.Detector;

/**
 * DQM driver V0 particles (i.e. e+e- pars) plots things like number of vertex
 * position an mass
 *
 * @author mgraham on May 14, 2014
 *
 */
public class MollerMonitoring extends DataQualityMonitor {

    private enum Cut {

        TRK_QUALITY("Trk Quality"),
        VTX_QUALITY("Vtx Quality"),
        VERTEX_CUTS("Vtx Cuts"),
        TIMING("Timing"),
        TRACK_CUTS("Trk Cuts"),
        EVENT_QUALITY("Evt Quality"),
        FRONT_HITS("Front Hits");
        private final String name;
        private final static int firstVertexingCut = 6;

        Cut(String name) {
            this.name = name;
        }

        static int bitmask(EnumSet<Cut> flags) {
            int mask = 0;
            for (Cut flag : flags) {
                mask |= 1 << flag.ordinal();
            }
            return mask;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final static Logger LOGGER = Logger.getLogger(MollerMonitoring.class.getPackage().getName());

    private final BasicHep3Matrix beamAxisRotation = new BasicHep3Matrix();
//    private static final int nCuts = 9;
//    private final String[] cutNames = {"Trk Quality",
//        "V0 Quality",
//        "V0 Vertex",
//        "Timing",
//        "Tracking",
//        "Cluster",
//        "Event",
//        "Front Hits",
//        "Isolation"};
//    private int firstVertexingCut = 0;

    private final String finalStateParticlesColName = "FinalStateParticles";
    private final String unconstrainedV0CandidatesColName = "UnconstrainedMollerCandidates";
//    private final String beamConV0CandidatesColName = "BeamspotConstrainedV0Candidates";
//    private final String targetV0ConCandidatesColName = "TargetConstrainedV0Candidates";
//    private final String trackListName = "MatchedTracks";
    private final String[] fpQuantNames = {"nV0_per_Event", "avg_BSCon_mass", "avg_BSCon_Vx", "avg_BSCon_Vy", "avg_BSCon_Vz", "sig_BSCon_Vx", "sig_BSCon_Vy", "sig_BSCon_Vz", "avg_BSCon_Chi2"};

    private final String plotDir = "MollerMonitoring/";

//    private IHistogram2D triTrackTime2D;
//    private IHistogram1D triTrackTimeDiff;
    private IHistogram2D triMassMomentum;
    private IHistogram2D triZVsMomentum;
    private IHistogram2D triTrackMomentum2D;
    private IHistogram2D triPyEleVsPyPos;
    private IHistogram2D triPxEleVsPxPos;
    private IHistogram1D triDeltaP;
    private IHistogram1D triSumP;
    private IHistogram1D triMass;
    private IHistogram2D triZVsMass;
//    private IHistogram1D triX;
//    private IHistogram1D triY;
//    private IHistogram1D triZ;
//    private IHistogram2D triZY;
//    private IHistogram2D triXY;
    private IHistogram1D triPx;
    private IHistogram1D triPy;
    private IHistogram1D triPz;
    private IHistogram2D triPxPy;
    private IHistogram1D triU;
    private IHistogram1D triV;

//    private IHistogram2D vertTrackTime2D;
//    private IHistogram1D vertTrackTimeDiff;
    private IHistogram2D vertMassMomentum;
    private IHistogram2D vertZVsMomentum;
    private IHistogram2D vertTrackMomentum2D;
    private IHistogram2D vertPyEleVsPyPos;
    private IHistogram2D vertPxEleVsPxPos;
    private IHistogram1D vertDeltaP;
    private IHistogram1D vertSumP;
    private IHistogram1D vertMass;
    private IHistogram2D vertZVsMass;
    private IHistogram1D vertX;
    private IHistogram1D vertY;
//    private IHistogram1D vertZ;
    private IHistogram2D vertZY;
    private IHistogram2D vertXY;
    private IHistogram1D vertPx;
    private IHistogram1D vertPy;
    private IHistogram1D vertPz;
    private IHistogram2D vertPxPy;
    private IHistogram1D vertU;
    private IHistogram1D vertV;

    private IHistogram1D nTriCand;
    private IHistogram1D nVtxCand;
//    IHistogram1D vertexW;
//    IHistogram2D vertexVZ;

    //clean up event first
    private final int nTrkMax = 5;
    private final int nPosMax = 1;

    private final double maxChi2SeedTrack = 7.0;
    private double maxChi2GBLTrack = 15.0;
    private double maxUnconVertChi2 = 10.0;
    private double maxBsconVertChi2 = 1000.0; //disable by default

    //v0 plot ranges
    private final double v0PzMax = 1.25;//GeV 
    private final double v0PzMin = 0.1;// GeV
    private final double v0PyMax = 0.01;//GeV absolute value
    private final double v0PxMax = 0.02;//GeV absolute value
    private final double v0VzMax = 50.0;// mm from target...someday make mass dependent
    private final double v0VyMax = 2.0;// mm from target...someday make mass dependent
    private final double v0VxMax = 2.0;// mm from target...someday make mass dependent

    //v0 cuts
    private final double v0PzMaxCut = 1.25;//GeV 
    private final double v0PzMinCut = 0.8;// GeV
    private final double v0PyCut = 0.01;//GeV absolute value
    private final double v0PxCut = 0.02;//GeV absolute value
    private final double v0UnconVzCut = 50.0;// mm from target...someday make mass dependent
    private double v0UnconVyCut = 10.0;// mm from target...someday make mass dependent
    private double v0UnconVxCut = 10.0;// mm from target...someday make mass dependent
    private double v0BsconVyCut = 10.0; //disable by default
    private double v0BsconVxCut = 10.0; //disable by default

//  track quality cuts
    private final double maxTrkPCut = 0.85;
//    private final double minTrkPCut = 0.05;
//    private double trkPyMax = 0.2;
//    private double trkPxMax = 0.2;
    private final double trkTimeDiff = 5.0;
//    private final double clusterTimeDiffCut = 2.5;

//    private double l1IsoMin = 0.5;
    private final double[] beamSize = {0.001, 0.130, 0.050}; //rough estimate from harp scans during engineering run production running
    private final double[] beamPos = {0.0, 0.0, 0.0};
    private final double[] vzcBeamSize = {0.001, 100, 100};

    public void setMaxChi2GBLTrack(double maxChi2GBLTrack) {
        this.maxChi2GBLTrack = maxChi2GBLTrack;
    }

    public void setMaxUnconVertChi2(double maxUnconVertChi2) {
        this.maxUnconVertChi2 = maxUnconVertChi2;
    }

    public void setMaxBsconVertChi2(double maxBsconVertChi2) {
        this.maxBsconVertChi2 = maxBsconVertChi2;
    }

    public void setV0UnconVyCut(double v0UnconVyCut) {
        this.v0UnconVyCut = v0UnconVyCut;
    }

    public void setV0UnconVxCut(double v0UnconVxCut) {
        this.v0UnconVxCut = v0UnconVxCut;
    }

    public void setV0BsconVyCut(double v0BsconVyCut) {
        this.v0BsconVyCut = v0BsconVyCut;
    }

    public void setV0BsconVxCut(double v0BsconVxCut) {
        this.v0BsconVxCut = v0BsconVxCut;
    }

    public void setBeamSizeX(double beamSizeX) {
        this.beamSize[1] = beamSizeX;
    }

    public void setBeamSizeY(double beamSizeY) {
        this.beamSize[2] = beamSizeY;
    }

    public void setBeamPosX(double beamPosX) {
        this.beamPos[1] = beamPosX;
    }

    public void setBeamPosY(double beamPosY) {
        this.beamPos[2] = beamPosY;
    }


    @Override
    protected void detectorChanged(Detector detector) {
        super.detectorChanged(detector);
        
    /*tab*/LOGGER.info("MollerMonitoring::detectorChanged  Setting up the plotter");
        beamAxisRotation.setActiveEuler(Math.PI / 2, -0.0305, -Math.PI / 2);

        aida.tree().cd("/");
        String trkType = "SeedTrack/";
        if (isGBL) {
            trkType = "GBLTrack/";
        }
        /*  V0 Quantities   */
        /*  Mass, vertex, chi^2 of fit */
        /* beamspot constrained */
//        IHistogram1D nV0 = aida.histogram1D(plotDir +  triggerType + "/"+"Number of V0 per event", 10, 0, 10);
//        IHistogram1D bsconMass = aida.histogram1D(plotDir +  triggerType + "/"+"BS Constrained Mass (GeV)", 100, 0, 0.200);
//        IHistogram1D bsconVx = aida.histogram1D(plotDir +  triggerType + "/"+"BS Constrained Vx (mm)", 50, -1, 1);
//        IHistogram1D bsconVy = aida.histogram1D(plotDir +  triggerType + "/"+"BS Constrained Vy (mm)", 50, -1, 1);
//        IHistogram1D bsconVz = aida.histogram1D(plotDir +  triggerType + "/"+"BS Constrained Vz (mm)", 50, -10, 10);
//        IHistogram1D bsconChi2 = aida.histogram1D(plotDir +  triggerType + "/"+"BS Constrained Chi2", 25, 0, 25);
//        /* target constrained */
//        IHistogram1D tarconMass = aida.histogram1D(plotDir +  triggerType + "/"+"Target Constrained Mass (GeV)", 100, 0, 0.200);
//        IHistogram1D tarconVx = aida.histogram1D(plotDir +  triggerType + "/"+ triggerType + "/"+"Target Constrained Vx (mm)", 50, -1, 1);
//        IHistogram1D tarconVy = aida.histogram1D(plotDir +  triggerType + "/"+ triggerType + "/"+"Target Constrained Vy (mm)", 50, -1, 1);
//        IHistogram1D tarconVz = aida.histogram1D(plotDir +  triggerType + "/"+ triggerType + "/"+"Target Constrained Vz (mm)", 50, -10, 10);
//        IHistogram1D tarconChi2 = aida.histogram1D(plotDir +  triggerType + "/"+ triggerType + "/"+"Target Constrained Chi2", 25, 0, 25);
        nTriCand = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Number of Moller Candidates", 5, 0, 4);

//        triTrackTimeDiff = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Trident: Track time difference", 100, -10, 10);
//        triTrackTime2D = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Trident: Track time vs. track time", 100, -10, 10, 100, -10, 10);
        triTrackMomentum2D = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Moller: Bottom vs. top momentum", 100, 0, v0PzMax * beamEnergy, 100, 0, v0PzMax * beamEnergy);
        triDeltaP = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Moller: Bottom - top momentum", 100, -beamEnergy, beamEnergy);
        triSumP = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Moller: Bottom + top momentum", 100, v0PzMin * beamEnergy, v0PzMax * beamEnergy);
        triPyEleVsPyPos = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Moller: Py(t) vs Py(b)", 50, -2 * v0PyMax * beamEnergy, 2 * v0PyMax * beamEnergy, 50, -2 * v0PyMax * beamEnergy, 2 * v0PyMax * beamEnergy);
        triPxEleVsPxPos = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Moller: Px(t) vs Px(b)", 50, -2 * v0PxMax * beamEnergy, 2 * v0PxMax * beamEnergy, 50, -2 * v0PxMax * beamEnergy, 2 * v0PxMax * beamEnergy);

        triMassMomentum = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Moller: Vertex mass vs. vertex momentum", 100, v0PzMin * beamEnergy, v0PzMax * beamEnergy, 100, 0, 0.1 * beamEnergy);
        triZVsMomentum = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Moller: Vertex Z vs. vertex momentum", 100, v0PzMin * beamEnergy, v0PzMax * beamEnergy, 100, -v0VzMax, v0VzMax);
        triMass = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Moller: Vertex mass", 100, 0, 0.1 * beamEnergy);
        triZVsMass = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Moller: Vertex Z vs. mass", 100, 0, 0.1 * beamEnergy, 100, -v0VzMax, v0VzMax);
//        triX = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Trident: Vertex X", 100, -v0VxMax, v0VxMax);
//        triY = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Trident: Vertex Y", 100, -v0VyMax, v0VyMax);
//        triZ = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Trident: Vertex Z", 100, -v0VzMax, v0VzMax);
//        triXY = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Trident: Vertex Y vs. X", 100, -v0VxMax, v0VxMax, 100, -v0VyMax, v0VyMax);
//        triZY = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Trident: Vertex Z vs. Y", 100, -v0VyMax, v0VyMax, 100, -v0VzMax, v0VzMax);
        triPx = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Trident: Vertex Px", 100, -v0PxMax, v0PxMax);
        triPy = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Trident: Vertex Py", 100, -v0PyMax, v0PyMax);
        triPz = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Trident: Vertex Pz", 100, v0PzMin, v0PzMax);
        triPxPy = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Trident: Vertex Py vs. Px", 100, -v0PxMax, v0PxMax, 100, -v0PyMax, v0PyMax);
        triU = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Trident: Vertex Px over Ptot", 100, -0.1, 0.1);
        triV = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Trident: Vertex Py over Ptot", 100, -0.1, 0.1);

//        vertTrackTimeDiff = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Vertex: Track time difference", 100, -10, 10);
//        vertTrackTime2D = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Vertex: Track time vs. track time", 100, -10, 10, 100, -10, 10);
        vertTrackMomentum2D = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Vertex: Bottom vs. top momentum", 100, 0, v0PzMax * beamEnergy, 100, 0, v0PzMax * beamEnergy);
        vertDeltaP = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Vertex: Bottom - top momentum", 100, -beamEnergy, beamEnergy);
        vertSumP = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Vertex: Bottom + top momentum", 100, v0PzMin * beamEnergy, v0PzMax * beamEnergy);
        vertPyEleVsPyPos = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Vertex: Py(t) vs Py(b)", 50, -2 * v0PyMax * beamEnergy, 2 * v0PyMax * beamEnergy, 50, -2 * v0PyMax * beamEnergy, 2 * v0PyMax * beamEnergy);
        vertPxEleVsPxPos = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Vertex: Px(t) vs Px(b)", 50, -2 * v0PxMax * beamEnergy, 2 * v0PxMax * beamEnergy, 50, -2 * v0PxMax * beamEnergy, 2 * v0PxMax * beamEnergy);

        vertMassMomentum = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Vertex: Vertex mass vs. vertex momentum", 100, v0PzMin * beamEnergy, v0PzMax * beamEnergy, 100, 0, 0.1 * beamEnergy);
        vertZVsMomentum = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Vertex: Vertex Z vs. vertex momentum", 100, v0PzMin * beamEnergy, v0PzMax * beamEnergy, 100, -v0VzMax, v0VzMax);
        vertMass = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Vertex: Vertex mass", 100, 0, 0.1 * beamEnergy);
        vertZVsMass = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Vertex: Vertex Z vs. mass", 100, 0, 0.1 * beamEnergy, 100, -v0VzMax, v0VzMax);
        vertX = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Vertex: Vertex X", 100, -v0VxMax, v0VxMax);
        vertY = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Vertex: Vertex Y", 100, -v0VyMax, v0VyMax);
//        vertZ = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Vertex: Vertex Z", 100, -v0VzMax, v0VzMax);
        vertXY = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Vertex: Vertex Y vs. X", 100, -v0VxMax, v0VxMax, 100, -v0VyMax, v0VyMax);
        vertZY = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Vertex: Vertex Z vs. Y", 100, -v0VyMax, v0VyMax, 100, -v0VzMax, v0VzMax);
        vertPx = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Vertex: Vertex Px", 100, -v0PxMax, v0PxMax);
        vertPy = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Vertex: Vertex Py", 100, -v0PyMax, v0PyMax);
        vertPz = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Vertex: Vertex Pz", 100, v0PzMin, v0PzMax);
        vertPxPy = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Vertex: Vertex Py vs. Px", 100, -v0PxMax, v0PxMax, 100, -v0PyMax, v0PyMax);
        vertU = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Vertex: Vertex Px over Ptot", 100, -0.1, 0.1);
        vertV = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Vertex: Vertex Py over Ptot", 100, -0.1, 0.1);

        nVtxCand = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Number of Vertexing Candidates", 5, 0, 4);
    }

    @Override
    public void process(EventHeader event) {
        /*  make sure everything is there */
        if (!event.hasCollection(ReconstructedParticle.class, finalStateParticlesColName)) {
            return;
        }
        if (!event.hasCollection(ReconstructedParticle.class, unconstrainedV0CandidatesColName)) {
            return;
        }
//        if (!event.hasCollection(ReconstructedParticle.class, beamConV0CandidatesColName)) {
//            return;
//        }
//        if (!event.hasCollection(ReconstructedParticle.class, targetV0ConCandidatesColName)) {
//            return;
//        }

        //check to see if this event is from the correct trigger (or "all");
        if (!matchTrigger(event)) {
            return;
        }

        List<ReconstructedParticle> unConstrainedV0List = event.get(ReconstructedParticle.class, unconstrainedV0CandidatesColName);

        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);

        List<ReconstructedParticle> fspList = event.get(ReconstructedParticle.class, finalStateParticlesColName);
        int npos = 0;
        int ntrk = 0;
        for (ReconstructedParticle fsp : fspList) {
            if (isGBL != TrackType.isGBL(fsp.getType())) {
                continue;
            }
            if (fsp.getCharge() != 0) {
                ntrk++;
            }
            if (fsp.getCharge() > 0) {
                npos++;
            }
        }

        List<ReconstructedParticle> candidateList = new ArrayList<>();
        List<ReconstructedParticle> vertCandidateList = new ArrayList<>();
        for (ReconstructedParticle uncV0 : unConstrainedV0List) {
            if (isGBL != TrackType.isGBL(uncV0.getType())) {
                continue;
            }
            Vertex uncVert = uncV0.getStartVertex();
//  v0 & vertex-quality cuts
            Hep3Vector v0MomRot = VecOp.mult(beamAxisRotation, uncV0.getMomentum());
            Hep3Vector v0Vtx = VecOp.mult(beamAxisRotation, uncVert.getPosition());

            List<Track> tracks = new ArrayList<Track>();
            ReconstructedParticle top = uncV0.getParticles().get(ReconParticleDriver.MOLLER_TOP);
            ReconstructedParticle bot = uncV0.getParticles().get(ReconParticleDriver.MOLLER_BOT);
            if (top.getCharge() != -1 || bot.getCharge() != -1) {
                throw new RuntimeException("incorrect charge on v0 daughters");
            }
            tracks.add(top.getTracks().get(0));
            tracks.add(bot.getTracks().get(0));
            if (tracks.size() != 2) {
                throw new RuntimeException("expected two tracks in vertex, got " + tracks.size());
            }

            Double[] topIso = TrackUtils.getIsolations(top.getTracks().get(0), hitToStrips, hitToRotated);
            Double[] botIso = TrackUtils.getIsolations(bot.getTracks().get(0), hitToStrips, hitToRotated);
            double minL1Iso = -9999;
            if (topIso[0] != null && botIso[0] != null) {
                double topL1Iso = Math.min(Math.abs(topIso[0]), Math.abs(topIso[1]));
                double botL1Iso = Math.min(Math.abs(botIso[0]), Math.abs(botIso[1]));
                minL1Iso = Math.min(topL1Iso, botL1Iso);
            }

            double tTop = TrackUtils.getTrackTime(top.getTracks().get(0), hitToStrips, hitToRotated);
            double tBot = TrackUtils.getTrackTime(bot.getTracks().get(0), hitToStrips, hitToRotated);
            Hep3Vector pTopRot = VecOp.mult(beamAxisRotation, top.getMomentum());
            Hep3Vector pBotRot = VecOp.mult(beamAxisRotation, bot.getMomentum());

            Hep3Vector topAtEcal = TrackUtils.getTrackPositionAtEcal(top.getTracks().get(0));
            Hep3Vector botAtEcal = TrackUtils.getTrackPositionAtEcal(bot.getTracks().get(0));

            BilliorVertexer vtxFitter = new BilliorVertexer(TrackUtils.getBField(event.getDetector()).y());
            vtxFitter.setBeamSize(beamSize);
            vtxFitter.setBeamPosition(beamPos);
            List<BilliorTrack> billiorTracks = new ArrayList<BilliorTrack>();
            billiorTracks.add(new BilliorTrack(top.getTracks().get(0)));
            billiorTracks.add(new BilliorTrack(bot.getTracks().get(0)));

            vtxFitter.doBeamSpotConstraint(true);
            BilliorVertex bsconVertex = vtxFitter.fitVertex(billiorTracks);
            ReconstructedParticle bscV0 = HpsReconParticleDriver.makeReconstructedParticle(top, bot, bsconVertex);
            Hep3Vector bscMomRot = VecOp.mult(beamAxisRotation, bscV0.getMomentum());
            Hep3Vector bscVtx = VecOp.mult(beamAxisRotation, bscV0.getStartVertex().getPosition());

            vtxFitter.doTargetConstraint(true);
            BilliorVertex tarVertex = vtxFitter.fitVertex(billiorTracks);
            ReconstructedParticle tarV0 = HpsReconParticleDriver.makeReconstructedParticle(top, bot, tarVertex);
            Hep3Vector tarMomRot = VecOp.mult(beamAxisRotation, tarV0.getMomentum());
            Hep3Vector tarVtx = VecOp.mult(beamAxisRotation, tarV0.getStartVertex().getPosition());

            vtxFitter.setBeamSize(vzcBeamSize);
            vtxFitter.doTargetConstraint(true);
            BilliorVertex vzcVertex = vtxFitter.fitVertex(billiorTracks);
            ReconstructedParticle vzcV0 = HpsReconParticleDriver.makeReconstructedParticle(top, bot, vzcVertex);
            Hep3Vector vzcMomRot = VecOp.mult(beamAxisRotation, vzcV0.getMomentum());
            Hep3Vector vzcVtx = VecOp.mult(beamAxisRotation, vzcV0.getStartVertex().getPosition());

            //start applying cuts
            EnumSet<Cut> bits = EnumSet.noneOf(Cut.class);

            boolean trackQualityCut = Math.max(top.getTracks().get(0).getChi2(), bot.getTracks().get(0).getChi2()) < (isGBL ? maxChi2GBLTrack : maxChi2SeedTrack);
            if (trackQualityCut) {
                bits.add(Cut.TRK_QUALITY);
            }

            boolean v0QualityCut = uncVert.getChi2() < maxUnconVertChi2 && bsconVertex.getChi2() < maxBsconVertChi2;
            if (v0QualityCut) {
                bits.add(Cut.VTX_QUALITY);
            }

            boolean vertexMomentumCut = v0MomRot.z() < v0PzMaxCut * beamEnergy && v0MomRot.z() > v0PzMinCut * beamEnergy && Math.abs(v0MomRot.x()) < v0PxCut * beamEnergy && Math.abs(v0MomRot.y()) < v0PyCut * beamEnergy;
            boolean vertexPositionCut = Math.abs(v0Vtx.x()) < v0UnconVxCut && Math.abs(v0Vtx.y()) < v0UnconVyCut && Math.abs(v0Vtx.z()) < v0UnconVzCut && Math.abs(bscVtx.x()) < v0BsconVxCut && Math.abs(bscVtx.y()) < v0BsconVyCut;
            if (vertexMomentumCut && vertexPositionCut) {
                bits.add(Cut.VERTEX_CUTS);
            }

            boolean trackTimeDiffCut = Math.abs(tTop - tBot) < trkTimeDiff;
            if (trackTimeDiffCut) {
                bits.add(Cut.TIMING);
            }

//            boolean topBottomCut = top.getMomentum().y() * bot.getMomentum().y() < 0;
//            boolean pMinCut = top.getMomentum().magnitude() > minTrkPCut * beamEnergy && bot.getMomentum().magnitude() > minTrkPCut * beamEnergy;
            boolean pMaxCut = top.getMomentum().magnitude() < maxTrkPCut * beamEnergy && bot.getMomentum().magnitude() < maxTrkPCut * beamEnergy;
//            boolean pSumCut = top.getMomentum().magnitude() + bot.getMomentum().magnitude() < maxPSumCut * beamEnergy && top.getMomentum().magnitude() + bot.getMomentum().magnitude() < minPSumCut * beamEnergy;
//            boolean pMaxCut = top.getMomentum().magnitude() < maxTrkPCut * beamEnergy && bot.getMomentum().magnitude() < maxTrkPCut * beamEnergy;
            if (pMaxCut) {
                bits.add(Cut.TRACK_CUTS);
            }

            boolean eventTrkCountCut = ntrk >= 2 && ntrk <= nTrkMax;
            boolean eventPosCountCut = npos >= 1 && npos <= nPosMax;
            if (eventTrkCountCut && eventPosCountCut) {
                bits.add(Cut.EVENT_QUALITY);
            }

            boolean frontHitsCut = topIso[0] != null && botIso[0] != null && topIso[2] != null && botIso[2] != null;
            if (frontHitsCut) {
                bits.add(Cut.FRONT_HITS);
            }

            for (Cut cut : Cut.values()) {
                if (bits.contains(cut)) {
                    if (cut.ordinal() == Cut.firstVertexingCut) {//if we get here, we've passed all non-vertexing cuts
                        candidateList.add(uncV0);
                    }
                } else {
                    break;
                }
            }

            if (bits.containsAll(EnumSet.range(Cut.values()[0], Cut.values()[Cut.firstVertexingCut - 1]))) {
                candidateList.add(uncV0);
            }
            if (bits.equals(EnumSet.allOf(Cut.class))) {
                vertCandidateList.add(uncV0);
            }
        }

        nTriCand.fill(candidateList.size());
        nVtxCand.fill(vertCandidateList.size());

        if (!candidateList.isEmpty()) {
            // pick the best candidate...for now just pick a random one. 
            ReconstructedParticle bestCandidate = candidateList.get((int) (Math.random() * candidateList.size()));

            //fill some stuff: 
            ReconstructedParticle top = bestCandidate.getParticles().get(ReconParticleDriver.MOLLER_TOP);
            ReconstructedParticle bot = bestCandidate.getParticles().get(ReconParticleDriver.MOLLER_BOT);
            if (top.getCharge() != -1 || bot.getCharge() != -1) {
                throw new RuntimeException("vertex needs two e- but is missing one or both");
            }

            Hep3Vector pEleRot = VecOp.mult(beamAxisRotation, top.getMomentum());
            Hep3Vector pPosRot = VecOp.mult(beamAxisRotation, bot.getMomentum());
            Hep3Vector v0Vtx = VecOp.mult(beamAxisRotation, bestCandidate.getStartVertex().getPosition());
            Hep3Vector pBestV0Rot = VecOp.mult(beamAxisRotation, bestCandidate.getMomentum());

//            triTrackTime2D.fill(tEle, tPos);
//            triTrackTimeDiff.fill(tEle - tPos);
            triZVsMomentum.fill(bestCandidate.getMomentum().magnitude(), v0Vtx.z());
            triMassMomentum.fill(bestCandidate.getMomentum().magnitude(), bestCandidate.getMass());
            triTrackMomentum2D.fill(top.getMomentum().magnitude(), bot.getMomentum().magnitude());
            triPyEleVsPyPos.fill(pEleRot.y(), pPosRot.y());
            triPxEleVsPxPos.fill(pEleRot.x(), pPosRot.x());
            triSumP.fill(bestCandidate.getMomentum().magnitude());
            triDeltaP.fill(bot.getMomentum().magnitude() - top.getMomentum().magnitude());

            triPxPy.fill(pBestV0Rot.x(), pBestV0Rot.y());
            triMass.fill(bestCandidate.getMass());
            triZVsMass.fill(bestCandidate.getMass(), v0Vtx.z());
//            triX.fill(v0Vtx.x());
//            triY.fill(v0Vtx.y());
//            triZ.fill(v0Vtx.z());
            triPx.fill(pBestV0Rot.x());
            triPy.fill(pBestV0Rot.y());
            triPz.fill(pBestV0Rot.z());
            triU.fill(pBestV0Rot.x() / pBestV0Rot.magnitude());
            triV.fill(pBestV0Rot.y() / pBestV0Rot.magnitude());
//            triXY.fill(v0Vtx.x(), v0Vtx.y());
//            triZY.fill(v0Vtx.y(), v0Vtx.z());
        }

        if (!vertCandidateList.isEmpty()) {
            // pick the best candidate...for now just pick a random one. 
            ReconstructedParticle bestCandidate = vertCandidateList.get((int) (Math.random() * vertCandidateList.size()));
            Vertex unconVertex = bestCandidate.getStartVertex();

            //fill some stuff: 
            ReconstructedParticle top = bestCandidate.getParticles().get(ReconParticleDriver.MOLLER_TOP);
            ReconstructedParticle bot = bestCandidate.getParticles().get(ReconParticleDriver.MOLLER_BOT);
            if (top.getCharge() != -1 || bot.getCharge() != -1) {
                throw new RuntimeException("vertex needs two e- but is missing one or both");
            }

            Hep3Vector pEleRot = VecOp.mult(beamAxisRotation, top.getMomentum());
            Hep3Vector pPosRot = VecOp.mult(beamAxisRotation, bot.getMomentum());
            Hep3Vector v0Vtx = VecOp.mult(beamAxisRotation, unconVertex.getPosition());
            Hep3Vector pBestV0Rot = VecOp.mult(beamAxisRotation, bestCandidate.getMomentum());

//            vertTrackTime2D.fill(tEle, tPos);
//            vertTrackTimeDiff.fill(tEle - tPos);
            vertZVsMomentum.fill(bestCandidate.getMomentum().magnitude(), v0Vtx.z());
            vertMassMomentum.fill(bestCandidate.getMomentum().magnitude(), bestCandidate.getMass());
            vertTrackMomentum2D.fill(top.getMomentum().magnitude(), bot.getMomentum().magnitude());
            vertPyEleVsPyPos.fill(pEleRot.y(), pPosRot.y());
            vertPxEleVsPxPos.fill(pEleRot.x(), pPosRot.x());
            vertSumP.fill(bestCandidate.getMomentum().magnitude());
            vertDeltaP.fill(bot.getMomentum().magnitude() - top.getMomentum().magnitude());

            vertPxPy.fill(pBestV0Rot.x(), pBestV0Rot.y());
            vertMass.fill(bestCandidate.getMass());
            vertZVsMass.fill(bestCandidate.getMass(), v0Vtx.z());
            vertX.fill(v0Vtx.x());
            vertY.fill(v0Vtx.y());
//            vertZ.fill(v0Vtx.z());
            vertPx.fill(pBestV0Rot.x());
            vertPy.fill(pBestV0Rot.y());
            vertPz.fill(pBestV0Rot.z());
            vertU.fill(pBestV0Rot.x() / pBestV0Rot.magnitude());
            vertV.fill(pBestV0Rot.y() / pBestV0Rot.magnitude());
            vertXY.fill(v0Vtx.x(), v0Vtx.y());
            vertZY.fill(v0Vtx.y(), v0Vtx.z());
        }
    }

    @Override
    public void printDQMStrings() {
        for (int i = 0; i < 9; i++)//TODO:  do this in a smarter way...loop over the map
        {
            LOGGER.info("ALTER TABLE dqm ADD " + fpQuantNames[i] + " double;");
        }
    }
}
