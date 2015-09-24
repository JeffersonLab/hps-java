package org.hps.analysis.dataquality;

import hep.aida.IAnalysisFactory;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackUtils;
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
public class TridentMonitoring extends DataQualityMonitor {

    private double ebeam = 1.05;
    private BasicHep3Matrix beamAxisRotation = new BasicHep3Matrix();
    private static final int nCuts = 6;
    private static final int PASS = 0;
    private static final int FAIL = 1;

    private String finalStateParticlesColName = "FinalStateParticles";
    private String unconstrainedV0CandidatesColName = "UnconstrainedV0Candidates";
    private String beamConV0CandidatesColName = "BeamspotConstrainedV0Candidates";
    private String targetV0ConCandidatesColName = "TargetConstrainedV0Candidates";
    private String trackListName = "MatchedTracks";
    private String[] fpQuantNames = {"nV0_per_Event", "avg_BSCon_mass", "avg_BSCon_Vx", "avg_BSCon_Vy", "avg_BSCon_Vz", "sig_BSCon_Vx", "sig_BSCon_Vy", "sig_BSCon_Vz", "avg_BSCon_Chi2"};

    private final String plotDir = "TridentMonitoring/";
    private IHistogram2D trackTime2D;
    private IHistogram1D trackTimeDiff;
    private IHistogram2D vertexMassMomentum;
    private IHistogram2D vertexedTrackMomentum2D;
    private IHistogram2D pyEleVspyPos;
    private IHistogram2D pxEleVspxPos;
    private IHistogram2D vertexPxPy;
    private IHistogram1D goodVertexMass;
    private IHistogram2D goodVertexZVsMass;
    private IHistogram1D vertexX;
    private IHistogram1D vertexY;
    private IHistogram1D vertexZ;
    private IHistogram1D vertexPx;
    private IHistogram1D vertexPy;
    private IHistogram1D vertexPz;
    private IHistogram1D vertexU;
    private IHistogram1D vertexV;
    private IHistogram1D nCand;
//    IHistogram1D vertexW;
//    IHistogram2D vertexVZ;
    private IHistogram2D vertexZY;

    private IHistogram1D[][] cutVertexMass = new IHistogram1D[nCuts][2];
    private IHistogram1D[][] cutVertexZ = new IHistogram1D[nCuts][2];
    private IHistogram2D[][] cutVertexZVsMass = new IHistogram2D[nCuts][2];

    private IHistogram1D deltaP;
    private IHistogram1D deltaPRad;
    private IHistogram1D sumP;
    private IHistogram2D vertexedTrackMomentum2DRad;

    //clean up event first
    private int nTrkMax = 5;
    private int nPosMax = 1;

    private double maxChi2SeedTrack = 7.0;
    private double maxChi2GBLTrack = 15.0;
    private double maxVertChi2 = 7.0;

    //v0 cuts   
    private double v0PzMax = 1.25 * ebeam;//GeV 
    private double v0PzMin = 0.1;// GeV
    private double v0PyMax = 0.2;//GeV absolute value
    private double v0PxMax = 0.2;//GeV absolute value
    private double v0VzMax = 50.0;// mm from target...someday make mass dependent
    private double v0VyMax = 1.0;// mm from target...someday make mass dependent
    private double v0VxMax = 2.0;// mm from target...someday make mass dependent
    //  track quality cuts
    private double beamPCut = 0.85;
    private double minPCut = 0.05;
    private double trkPyMax = 0.2;
    private double trkPxMax = 0.2;
    private double radCut = 0.8 * ebeam;
    private double trkTimeDiff = 16.0;
    
    private double l1IsoMin = 1.0;
//cluster matching
    private boolean reqCluster = false;
    private int nClustMax = 3;
    private double eneLossFactor = 0.7; //average E/p roughly
    private double eneOverPCut = 0.3; //|(E/p)_meas - (E/p)_mean|<eneOverPCut

//counters
    private float nRecoEvents = 0;
    private float nPassBasicCuts = 0;
    private float nPassTrkQualityCuts = 0;
    private float nPassV0QualityCuts = 0;
    private float nPassV0Cuts = 0;
    private float nPassTrkCuts = 0;
    private float nPassTimeCuts = 0;
    private float nPassIsoCuts = 0;

    private float nPassClusterCuts = 0;

    public void setEbeam(double ebeam) {
        this.ebeam = ebeam;
    }

    @Override
    protected void detectorChanged(Detector detector) {
        System.out.println("TridendMonitoring::detectorChanged  Setting up the plotter");
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
        pyEleVspyPos = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Py(e) vs Py(p)", 50, -0.04, 0.04, 50, -0.04, 0.04);
        pxEleVspxPos = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Px(e) vs Px(p)", 50, -0.04, 0.04, 50, -0.04, 0.04);
        trackTimeDiff = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Track time difference", 100, -10, 10);
        trackTime2D = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Track time vs. track time", 100, -10, 10, 100, -10, 10);
        vertexMassMomentum = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Vertex mass vs. vertex momentum", 100, 0, 1.1, 100, 0, 0.1);
        vertexedTrackMomentum2D = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Positron vs. electron momentum", 100, 0, 1.1, 100, 0, 1.1);
        vertexedTrackMomentum2DRad = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Positron vs. electron momentum: Radiative", 100, 0, 1.1, 100, 0, 1.1);
        vertexPxPy = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Vertex Py vs. Px", 100, -0.04, 0.04, 100, -0.04, 0.04);
        goodVertexMass = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Good vertex mass", 100, 0, 0.11);
        goodVertexZVsMass = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Good vertex Z vs. mass", 100, 0, 0.11, 100, -v0VzMax, v0VzMax);
        nCand = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Number of Trident Candidates", 5, 0, 4);
        deltaP = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Positron - electron momentum", 100, -1., 1.0);
        deltaPRad = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Positron - electron momentum", 100, -1., 1.0);
        sumP = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Positron + electron momentum", 100, 0.2, 1.25);
        vertexX = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Vertex X Position (mm)", 100, -v0VxMax, v0VxMax);
        vertexY = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Vertex Y Position (mm)", 100, -v0VyMax, v0VyMax);
        vertexZ = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Vertex Z Position (mm)", 100, -v0VzMax, v0VzMax);
        vertexPx = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Vertex Px (GeV)", 100, -0.1, 0.1);
        vertexPy = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Vertex Py (GeV)", 100, -0.1, 0.1);
        vertexPz = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Vertex Pz (GeV)", 100, 0.0, v0PzMax);
        vertexU = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Vertex Px over Ptot (GeV)", 100, -0.1, 0.1);
        vertexV = aida.histogram1D(plotDir + trkType + triggerType + "/" + "Vertex Py over Ptot (GeV)", 100, -0.1, 0.1);
//        vertexW = aida.histogram1D(plotDir +trkType+ triggerType + "/" + "Vertex Pz overPtot (GeV)", 100, 0.95, 1.0);
//        vertexVZ = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Vertex Py over Ptot vs. Z", 100, -v0VzMax, v0VzMax, 100, -0.1, 0.1);
        vertexZY = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Vertex Z vs. Y", 100, -v0VyMax, v0VyMax, 100, -v0VzMax, v0VzMax);

        for (int i = 0; i < nCuts; i++) {
            for (int pass = 0; pass < 2; pass++) {
                cutVertexZ[i][pass] = aida.histogram1D(String.format("%s%s%s/cut%d/%s: Vertex Z position (mm)", plotDir, trkType, triggerType, i, pass == PASS ? "pass" : "fail"),
                        100, -v0VzMax, v0VzMax);
                cutVertexMass[i][pass] = aida.histogram1D(String.format("%s%s%s/cut%d/%s: Vertex mass (GeV)", plotDir, trkType, triggerType, i, pass == PASS ? "pass" : "fail"),
                        100, 0, 0.1 * ebeam);
                cutVertexZVsMass[i][pass] = aida.histogram2D(String.format("%s%s%s/cut%d/%s: Vertex Z vs. mass", plotDir, trkType, triggerType, i, pass == PASS ? "pass" : "fail"),
                        100, 0, 0.1 * ebeam, 100, -v0VzMax, v0VzMax);
            }
        }
    }

    @Override
    public void process(EventHeader event
    ) {
        /*  make sure everything is there */
        if (!event.hasCollection(ReconstructedParticle.class, finalStateParticlesColName)) {
            return;
        }
        if (!event.hasCollection(ReconstructedParticle.class, unconstrainedV0CandidatesColName)) {
            return;
        }
        if (!event.hasCollection(ReconstructedParticle.class, beamConV0CandidatesColName)) {
            return;
        }
        if (!event.hasCollection(ReconstructedParticle.class, targetV0ConCandidatesColName)) {
            return;
        }
        if (!event.hasCollection(Track.class, trackListName)) {
            return;
        }

        //check to see if this event is from the correct trigger (or "all");
        if (!matchTrigger(event)) {
            return;
        }

        nRecoEvents++;

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
        if (ntrk < 2 || ntrk > nTrkMax) {
            return;
        }
        if (npos < 1 || npos > nPosMax) {
            return;
        }

        nPassBasicCuts++;//passed some basic event-level cuts...

        List<ReconstructedParticle> candidateList = new ArrayList<>();
        List<ReconstructedParticle> vertCandidateList = new ArrayList<>();
        List<ReconstructedParticle> unConstrainedV0List = event.get(ReconstructedParticle.class, unconstrainedV0CandidatesColName);
        for (ReconstructedParticle uncV0 : unConstrainedV0List) {
            if (isGBL != TrackType.isGBL(uncV0.getType())) {
                continue;
            }
            Vertex uncVert = uncV0.getStartVertex();
//  v0 & vertex-quality cuts
            Hep3Vector v0MomRot = VecOp.mult(beamAxisRotation, uncV0.getMomentum());
            Hep3Vector v0Vtx = VecOp.mult(beamAxisRotation, uncVert.getPosition());

            List<Track> tracks = new ArrayList<Track>();
            ReconstructedParticle electron = null, positron = null;
            for (ReconstructedParticle particle : uncV0.getParticles()) //                tracks.addAll(particle.getTracks());  //add add electron first, then positron...down below
            {
                if (particle.getCharge() > 0) {
                    positron = particle;
                } else if (particle.getCharge() < 0) {
                    electron = particle;
                } else {
                    throw new RuntimeException("expected only electron and positron in vertex, got something with charge 0");
                }
            }
            if (electron == null || positron == null) {
                throw new RuntimeException("vertex needs e+ and e- but is missing one or both");
            }
            tracks.add(electron.getTracks().get(0));
            tracks.add(positron.getTracks().get(0));
            if (tracks.size() != 2) {
                throw new RuntimeException("expected two tracks in vertex, got " + tracks.size());
            }
            List<Double> trackTimes = new ArrayList<Double>();
            for (Track track : tracks) {
                trackTimes.add(TrackUtils.getTrackTime(track, hitToStrips, hitToRotated));
            }

            Double[] eleIso = TrackUtils.getIsolations(electron.getTracks().get(0), hitToStrips, hitToRotated);
            Double[] posIso = TrackUtils.getIsolations(positron.getTracks().get(0), hitToStrips, hitToRotated);
            double minL1Iso = -9999;
            if (eleIso[0] != null && posIso[0] != null) {
                double eleL1Iso = Math.min(Math.abs(eleIso[0]), Math.abs(eleIso[1]));
                double posL1Iso = Math.min(Math.abs(posIso[0]), Math.abs(posIso[1]));
                minL1Iso = Math.min(eleL1Iso, posL1Iso);
//                    VtxZVsL1Iso.fill(minL1Iso, uncVert.getPosition().z());
            }

            int cutNum = 0;
            boolean trackQualityCut = Math.max(tracks.get(0).getChi2(), tracks.get(1).getChi2()) < (isGBL ? maxChi2GBLTrack : maxChi2SeedTrack);
            if (!trackQualityCut) {
                cutVertexZ[cutNum][FAIL].fill(v0Vtx.z());
                cutVertexMass[cutNum][FAIL].fill(uncV0.getMass());
                cutVertexZVsMass[cutNum][FAIL].fill(uncV0.getMass(), v0Vtx.z());
                continue;
            }
            cutVertexZ[cutNum][PASS].fill(v0Vtx.z());
            cutVertexMass[cutNum][PASS].fill(uncV0.getMass());
            cutVertexZVsMass[cutNum][PASS].fill(uncV0.getMass(), v0Vtx.z());
            cutNum++;
            nPassTrkQualityCuts++;

            boolean v0QualityCut = uncVert.getChi2() < maxVertChi2;
            if (!v0QualityCut) {
                cutVertexZ[cutNum][FAIL].fill(v0Vtx.z());
                cutVertexMass[cutNum][FAIL].fill(uncV0.getMass());
                cutVertexZVsMass[cutNum][FAIL].fill(uncV0.getMass(), v0Vtx.z());
                continue;
            }
            cutVertexZ[cutNum][PASS].fill(v0Vtx.z());
            cutVertexMass[cutNum][PASS].fill(uncV0.getMass());
            cutVertexZVsMass[cutNum][PASS].fill(uncV0.getMass(), v0Vtx.z());
            cutNum++;
            nPassV0QualityCuts++;

            boolean vertexMomentumCut = v0MomRot.z() < v0PzMax && v0MomRot.z() > v0PzMin && Math.abs(v0MomRot.x()) < v0PxMax && Math.abs(v0MomRot.y()) < v0PyMax;
            boolean vertexPositionCut = Math.abs(v0Vtx.x()) < v0VxMax && Math.abs(v0Vtx.y()) < v0VyMax && Math.abs(v0Vtx.z()) < v0VzMax;
            if (!vertexMomentumCut || !vertexPositionCut) {
                cutVertexZ[cutNum][FAIL].fill(v0Vtx.z());
                cutVertexMass[cutNum][FAIL].fill(uncV0.getMass());
                cutVertexZVsMass[cutNum][FAIL].fill(uncV0.getMass(), v0Vtx.z());
                continue;
            }
            cutVertexZ[cutNum][PASS].fill(v0Vtx.z());
            cutVertexMass[cutNum][PASS].fill(uncV0.getMass());
            cutVertexZVsMass[cutNum][PASS].fill(uncV0.getMass(), v0Vtx.z());
            cutNum++;
            nPassV0Cuts++;

            boolean trackTimeDiffCut = Math.abs(trackTimes.get(0) - trackTimes.get(1)) < trkTimeDiff;

            if (!trackTimeDiffCut) {
                cutVertexZ[cutNum][FAIL].fill(v0Vtx.z());
                cutVertexMass[cutNum][FAIL].fill(uncV0.getMass());
                cutVertexZVsMass[cutNum][FAIL].fill(uncV0.getMass(), v0Vtx.z());
                continue;
            }
            cutVertexZ[cutNum][PASS].fill(v0Vtx.z());
            cutVertexMass[cutNum][PASS].fill(uncV0.getMass());
            cutVertexZVsMass[cutNum][PASS].fill(uncV0.getMass(), v0Vtx.z());
            cutNum++;
            nPassTimeCuts++;

            boolean topBottomCut = electron.getMomentum().y() * positron.getMomentum().y() < 0;
            boolean pMinCut = electron.getMomentum().magnitude() > minPCut && positron.getMomentum().magnitude() > minPCut;
            boolean pMaxCut = electron.getMomentum().magnitude() < beamPCut && positron.getMomentum().magnitude() < beamPCut;
            if (!topBottomCut || !pMaxCut || !pMinCut) {
                cutVertexZ[cutNum][FAIL].fill(v0Vtx.z());
                cutVertexMass[cutNum][FAIL].fill(uncV0.getMass());
                cutVertexZVsMass[cutNum][FAIL].fill(uncV0.getMass(), v0Vtx.z());
                continue;
            }
            cutVertexZ[cutNum][PASS].fill(v0Vtx.z());
            cutVertexMass[cutNum][PASS].fill(uncV0.getMass());
            cutVertexZVsMass[cutNum][PASS].fill(uncV0.getMass(), v0Vtx.z());
            cutNum++;
            nPassTrkCuts++;

            candidateList.add(uncV0);

            boolean isoCut = minL1Iso > l1IsoMin;
            if (!isoCut) {
                cutVertexZ[cutNum][FAIL].fill(v0Vtx.z());
                cutVertexMass[cutNum][FAIL].fill(uncV0.getMass());
                cutVertexZVsMass[cutNum][FAIL].fill(uncV0.getMass(), v0Vtx.z());
                continue;
            }
            cutVertexZ[cutNum][PASS].fill(v0Vtx.z());
            cutVertexMass[cutNum][PASS].fill(uncV0.getMass());
            cutVertexZVsMass[cutNum][PASS].fill(uncV0.getMass(), v0Vtx.z());
            cutNum++;
            nPassIsoCuts++;
            
            vertCandidateList.add(uncV0);
        }

        nCand.fill(candidateList.size());
        if (candidateList.isEmpty()) {
            return;
        }
        // pick the best candidate...for now just pick a random one. 
        ReconstructedParticle bestCandidate = candidateList.get((int) (Math.random() * candidateList.size()));

        //fill some stuff: 
        ReconstructedParticle electron = null, positron = null;
        for (ReconstructedParticle particle : bestCandidate.getParticles()) //                tracks.addAll(particle.getTracks());  //add add electron first, then positron...down below
        {
            if (particle.getCharge() > 0) {
                positron = particle;
            } else if (particle.getCharge() < 0) {
                electron = particle;
            } else {
                throw new RuntimeException("expected only electron and positron in vertex, got something with charge 0");
            }
        }
        if (electron == null || positron == null) {
            throw new RuntimeException("vertex needs e+ and e- but is missing one or both");
        }

        double tEle = TrackUtils.getTrackTime(electron.getTracks().get(0), hitToStrips, hitToRotated);
        double tPos = TrackUtils.getTrackTime(positron.getTracks().get(0), hitToStrips, hitToRotated);
        Hep3Vector pBestV0Rot = VecOp.mult(beamAxisRotation, bestCandidate.getMomentum());
        Hep3Vector pEleRot = VecOp.mult(beamAxisRotation, electron.getMomentum());
        Hep3Vector pPosRot = VecOp.mult(beamAxisRotation, positron.getMomentum());

        trackTime2D.fill(tEle, tPos);
        trackTimeDiff.fill(tEle - tPos);
        vertexMassMomentum.fill(bestCandidate.getMomentum().magnitude(), bestCandidate.getMass());
        vertexedTrackMomentum2D.fill(electron.getMomentum().magnitude(), positron.getMomentum().magnitude());
        pyEleVspyPos.fill(pEleRot.y(), pPosRot.y());
        pxEleVspxPos.fill(pEleRot.x(), pPosRot.x());
        sumP.fill(bestCandidate.getMomentum().magnitude());
        deltaP.fill(positron.getMomentum().magnitude() - electron.getMomentum().magnitude());

        vertexPxPy.fill(pBestV0Rot.x(), pBestV0Rot.y());
        goodVertexMass.fill(bestCandidate.getMass());
        Vertex uncVert = bestCandidate.getStartVertex();
        Hep3Vector v0Vtx = VecOp.mult(beamAxisRotation, uncVert.getPosition());
        goodVertexZVsMass.fill(bestCandidate.getMass(), v0Vtx.z());
        vertexX.fill(v0Vtx.x());
        vertexY.fill(v0Vtx.y());
        vertexZ.fill(v0Vtx.z());
        vertexPx.fill(pBestV0Rot.x());
        vertexPy.fill(pBestV0Rot.y());
        vertexPz.fill(pBestV0Rot.z());
        vertexU.fill(pBestV0Rot.x() / pBestV0Rot.magnitude());
        vertexV.fill(pBestV0Rot.y() / pBestV0Rot.magnitude());
//                    vertexW.fill(bestCandidate.getMomentum().z()/bestCandidate.getMomentum().magnitude());
//        vertexVZ.fill(v0Vtx.z(), pBestV0Rot.y() / pBestV0Rot.magnitude());
        vertexZY.fill(v0Vtx.y(), v0Vtx.z());
        if (bestCandidate.getMomentum().magnitude() > radCut) {
            vertexedTrackMomentum2DRad.fill(electron.getMomentum().magnitude(), positron.getMomentum().magnitude());
            deltaPRad.fill(positron.getMomentum().magnitude() - electron.getMomentum().magnitude());
        }

    }

    @Override
    public void printDQMData() {
        System.out.println("TridendMonitoring::printDQMData");
        for (Entry<String, Double> entry : monitoredQuantityMap.entrySet()) {
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }
        System.out.println("*******************************");

        System.out.println("TridendMonitoring::Tridend Selection Summary");

        System.out.println("\t\t\tTridend Selection Summary");
        System.out.println("******************************************************************************************");
        System.out.println("Number of Events:\t\t" + nRecoEvents + "\t\t\t" + (nRecoEvents) / nRecoEvents + "\t\t\t" + nRecoEvents / nRecoEvents);
        System.out.println("N(particle) Cuts:\t\t" + nPassBasicCuts + "\t\t\t" + nPassBasicCuts / nRecoEvents + "\t\t\t" + nPassBasicCuts / nRecoEvents);
        System.out.println("Trk Quality Cuts:\t\t" + nPassTrkQualityCuts + "\t\t\t" + nPassTrkQualityCuts / nPassBasicCuts + "\t\t\t" + nPassTrkQualityCuts / nRecoEvents);
        System.out.println("V0 Quality  Cuts:\t\t" + nPassV0QualityCuts + "\t\t\t" + nPassV0QualityCuts / nPassTrkQualityCuts + "\t\t\t" + nPassV0QualityCuts / nRecoEvents);
        System.out.println("V0 Vertex   Cuts:\t\t" + nPassV0Cuts + "\t\t\t" + nPassV0Cuts / nPassV0QualityCuts + "\t\t\t" + nPassV0Cuts / nRecoEvents);
        System.out.println("Timing      Cuts:\t\t" + nPassTimeCuts + "\t\t\t" + nPassTimeCuts / nPassV0Cuts + "\t\t\t" + nPassTimeCuts / nRecoEvents);
        System.out.println("Tracking    Cuts:\t\t" + nPassTrkCuts + "\t\t\t" + nPassTrkCuts / nPassTimeCuts + "\t\t\t" + nPassTrkCuts / nRecoEvents);
        
        System.out.println("\t\t\tVertex Selection Summary");
        System.out.println("******************************************************************************************");
        System.out.println("Isolation   Cuts:\t\t" + nPassIsoCuts + "\t\t\t" + nPassIsoCuts / nPassTrkCuts + "\t\t\t" + nPassIsoCuts / nRecoEvents);
        System.out.println("******************************************************************************************");
    }

    /**
     * Calculate the averages here and fill the map
     */
    @Override
    public void calculateEndOfRunQuantities() {

        IAnalysisFactory analysisFactory = IAnalysisFactory.create();
        IFitFactory fitFactory = analysisFactory.createFitFactory();
        IFitter fitter = fitFactory.createFitter("chi2");

    }

    @Override
    public void printDQMStrings() {
        for (int i = 0; i < 9; i++)//TODO:  do this in a smarter way...loop over the map
        {
            System.out.println("ALTER TABLE dqm ADD " + fpQuantNames[i] + " double;");
        }
    }

    IFitResult fitVertexPosition(IHistogram1D h1d, IFitter fitter, double[] init, String range
    ) {
        return fitter.fit(h1d, "g+p1", init, range);
    }

}
