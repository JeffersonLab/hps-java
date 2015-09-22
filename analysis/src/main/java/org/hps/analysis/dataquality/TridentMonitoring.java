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

    String finalStateParticlesColName = "FinalStateParticles";
    String unconstrainedV0CandidatesColName = "UnconstrainedV0Candidates";
    String beamConV0CandidatesColName = "BeamspotConstrainedV0Candidates";
    String targetV0ConCandidatesColName = "TargetConstrainedV0Candidates";
    String trackListName = "MatchedTracks";
    String[] fpQuantNames = {"nV0_per_Event", "avg_BSCon_mass", "avg_BSCon_Vx", "avg_BSCon_Vy", "avg_BSCon_Vz", "sig_BSCon_Vx", "sig_BSCon_Vy", "sig_BSCon_Vz", "avg_BSCon_Chi2"};

    private final String plotDir = "TridentMonitoring/";
    IHistogram2D trackTime2D;
    IHistogram1D trackTimeDiff;
    IHistogram2D vertexMassMomentum;
    IHistogram2D vertexedTrackMomentum2D;
    IHistogram2D pyEleVspyPos;
    IHistogram2D pxEleVspxPos;
    IHistogram2D vertexPxPy;
    IHistogram1D goodVertexMass;
    IHistogram2D goodVertexZVsMass;
    IHistogram1D vertexX;
    IHistogram1D vertexY;
    IHistogram1D vertexZ;
    IHistogram1D vertexPx;
    IHistogram1D vertexPy;
    IHistogram1D vertexPz;
    IHistogram1D vertexU;
    IHistogram1D vertexV;
    IHistogram1D nCand;
//    IHistogram1D vertexW;
    IHistogram2D vertexVZ;
    IHistogram2D vertexZY;

    IHistogram1D deltaP;
    IHistogram1D deltaPRad;
    IHistogram1D sumP;
    IHistogram2D vertexedTrackMomentum2DRad;

    //clean up event first
    int nTrkMax = 5;
    int nPosMax = 1;

    //v0 cuts   
    double v0Chi2 = 10;
    double v0PzMax = 1.25 * ebeam;//GeV 
    double v0PzMin = 0.1;// GeV
    double v0PyMax = 0.2;//GeV absolute value
    double v0PxMax = 0.2;//GeV absolute value
    double v0VzMax = 25.0;// mm from target...someday make mass dependent
    double v0VyMax = 1.0;// mm from target...someday make mass dependent
    double v0VxMax = 2.0;// mm from target...someday make mass dependent
    //  track quality cuts
    double trkChi2 = 10;
    double beamPCut = 0.9;
    double minPCut = 0.05;
    double trkPyMax = 0.2;
    double trkPxMax = 0.2;
    double radCut = 0.8 * ebeam;
    double trkTimeDiff = 16.0;
//cluster matching
    boolean reqCluster = false;
    int nClustMax = 3;
    double eneLossFactor = 0.7; //average E/p roughly
    double eneOverPCut = 0.3; //|(E/p)_meas - (E/p)_mean|<eneOverPCut

//counters
    float nRecoEvents = 0;
    float nPassBasicCuts = 0;
    float nPassV0Cuts = 0;
    float nPassTrkCuts = 0;
    float nPassTimeCuts = 0;

    float nPassClusterCuts = 0;

    public void setEbeam(double ebeam) {
        this.ebeam = ebeam;
    }

    @Override
    protected void detectorChanged(Detector detector) {
        System.out.println("TridentMonitoring::detectorChanged  Setting up the plotter");
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
        vertexVZ = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Vertex Py over Ptot vs. Z", 100, -v0VzMax, v0VzMax, 100, -0.1, 0.1);
        vertexZY = aida.histogram2D(plotDir + trkType + triggerType + "/" + "Vertex Z vs. Y", 100, -v0VyMax, v0VyMax, 100, -v0VzMax, v0VzMax);
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
        ReconstructedParticle bestCandidate;
        List<ReconstructedParticle> unConstrainedV0List = event.get(ReconstructedParticle.class, unconstrainedV0CandidatesColName);
        for (ReconstructedParticle uncV0 : unConstrainedV0List) {
            if (isGBL != TrackType.isGBL(uncV0.getType())) {
                continue;
            }
            Vertex uncVert = uncV0.getStartVertex();
//  v0 & vertex-quality cuts
            Hep3Vector v0MomRot = VecOp.mult(beamAxisRotation, uncV0.getMomentum());
            if (v0MomRot.z() > v0PzMax || v0MomRot.z() < v0PzMin) {
                continue;
            }
            if (Math.abs(v0MomRot.y()) > v0PyMax) {
                continue;
            }
            if (Math.abs(v0MomRot.x()) > v0PxMax) {
                continue;
            }
            Hep3Vector v0Vtx = uncVert.getPosition();
            if (Math.abs(v0Vtx.z()) > v0VzMax) {
                continue;
            }
            if (Math.abs(v0Vtx.y()) > v0VyMax) {
                continue;
            }
            if (Math.abs(v0Vtx.x()) > v0VxMax) {
                continue;
            }
            nPassV0Cuts++;
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
            boolean trackTimeDiffCut = Math.abs(trackTimes.get(0) - trackTimes.get(1)) < trkTimeDiff;

            if (!trackTimeDiffCut) {
                continue;
            }
            nPassTimeCuts++;

            if (electron.getMomentum().y() * positron.getMomentum().y() > 0) {
                continue;
            }
            boolean pMinCut = electron.getMomentum().magnitude() > minPCut && positron.getMomentum().magnitude() > minPCut;
            if (!pMinCut) {
                continue;
            }
            boolean pMaxCut = electron.getMomentum().magnitude() < beamPCut && positron.getMomentum().magnitude() < beamPCut;
            if (!pMaxCut) {
                continue;
            }
            nPassTrkCuts++;

            candidateList.add(uncV0);
        }

        nCand.fill(candidateList.size());
        if (candidateList.isEmpty()) {
            return;
        }
        // pick the best candidate...for now just pick a random one. 
        bestCandidate = candidateList.get((int) (Math.random() * candidateList.size()));

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
        Hep3Vector v0Vtx = uncVert.getPosition();
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
        vertexVZ.fill(v0Vtx.z(), pBestV0Rot.y() / pBestV0Rot.magnitude());
        vertexZY.fill(v0Vtx.y(), v0Vtx.z());
        if (bestCandidate.getMomentum().magnitude() > radCut) {
            vertexedTrackMomentum2DRad.fill(electron.getMomentum().magnitude(), positron.getMomentum().magnitude());
            deltaPRad.fill(positron.getMomentum().magnitude() - electron.getMomentum().magnitude());
        }

    }

    @Override
    public void printDQMData() {
        System.out.println("TridentMonitoring::printDQMData");
        for (Entry<String, Double> entry : monitoredQuantityMap.entrySet()) {
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }
        System.out.println("*******************************");

        System.out.println("TridentMonitoring::Tridend Selection Summary");

        System.out.println("\t\t\tTrident Selection Summary");
        System.out.println("******************************************************************************************");
        System.out.println("Number of Events:\t\t" + nRecoEvents + "\t\t\t" + (nRecoEvents) / nRecoEvents + "\t\t\t" + nRecoEvents / nRecoEvents);
        System.out.println("N(particle) Cuts:\t\t" + nPassBasicCuts + "\t\t\t" + nPassBasicCuts / nRecoEvents + "\t\t\t" + nPassBasicCuts / nRecoEvents);
        System.out.println("V0 Vertex   Cuts:\t\t" + nPassV0Cuts + "\t\t\t" + nPassV0Cuts / nPassBasicCuts + "\t\t\t" + nPassV0Cuts / nRecoEvents);
        System.out.println("Timing    Cuts:\t\t" + nPassTimeCuts + "\t\t\t" + nPassTimeCuts / nPassV0Cuts + "\t\t\t" + nPassTimeCuts / nRecoEvents);
        System.out.println("Tracking    Cuts:\t\t" + nPassTrkCuts + "\t\t\t" + nPassTrkCuts / nPassTimeCuts + "\t\t\t" + nPassTrkCuts / nRecoEvents);
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

    IFitResult fitVertexPosition(IHistogram1D h1d, IFitter fitter, double[] init, String range) {
        return fitter.fit(h1d, "g+p1", init, range);
    }

}
