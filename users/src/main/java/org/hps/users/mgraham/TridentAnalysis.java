package org.hps.users.mgraham;

import hep.aida.IAnalysisFactory;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import org.hps.analysis.dataquality.DataQualityMonitor;
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
public class TridentAnalysis extends DataQualityMonitor {

    private double ebeam = 2.2;
    String finalStateParticlesColName = "FinalStateParticles";
    String unconstrainedV0CandidatesColName = "UnconstrainedV0Candidates";
    String beamConV0CandidatesColName = "BeamspotConstrainedV0Candidates";
    String targetV0ConCandidatesColName = "TargetConstrainedV0Candidates";
    String trackListName = "MatchedTracks";
    String[] fpQuantNames = {"nV0_per_Event", "avg_BSCon_mass", "avg_BSCon_Vx", "avg_BSCon_Vy", "avg_BSCon_Vz", "sig_BSCon_Vx", "sig_BSCon_Vy", "sig_BSCon_Vz", "avg_BSCon_Chi2"};

    private final String plotDir = "TridentAnalysis/";

    IHistogram2D trackTime2D;
    IHistogram1D trackTimeDiff;
    IHistogram2D vertexMassMomentum;
    IHistogram2D vertexedTrackMomentum2D;
    IHistogram2D pyEleVspyPos;
    IHistogram2D pxEleVspxPos;
    IHistogram2D vertexPxPy;
    IHistogram1D goodVertexMass;
    IHistogram1D vertexX;
    IHistogram1D vertexY;
    IHistogram1D vertexZ;
    IHistogram1D vertexPx;
    IHistogram1D vertexPy;
    IHistogram1D vertexPz;
    IHistogram1D vertexU;
    IHistogram1D vertexV;
//    IHistogram1D vertexW;
    IHistogram2D vertexVZ;
    IHistogram2D vertexZY;

    IHistogram1D tarconMass;
    IHistogram1D tarconVx;
    IHistogram1D tarconVy;
    IHistogram1D tarconVz;
    IHistogram1D tarconChi2;

    IHistogram1D deltaP;
    IHistogram1D deltaPRad;
    IHistogram1D sumP;
    IHistogram2D vertexedTrackMomentum2DRad;

    //clean up event first
    int trackType = 33;
    int nTrkMax = 3;
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
    double trkPzMax = 0.9 * ebeam;//GeV
    double trkPzMin = 0.1;//GeV
    double trkPyMax = 0.2;//GeV absolute value
    double trkPxMax = 0.2;//GeV absolute value
    double trkTimeDiff = 5.0;
//cut for the radiative-enhanced sample    
    double radCut = 0.8 * ebeam;
//cluster matching
    boolean reqCluster = false;
    int nClustMax = 3;
    double eneLossFactor = 0.7; //average E/p roughly
    double eneOverPCut = 0.3; //|(E/p)_meas - (E/p)_mean|<eneOverPCut

//counters
    int nRecoEvents = 0;
    int nPassBasicCuts = 0;
    int nPassV0PCuts = 0;
    int nPassV0VCuts = 0;
    int nPassTrkCuts = 0;

    int nPassClusterCuts = 0;

    public void setEbeam(double ebeam) {
        this.ebeam = ebeam;
    }

    @Override
    protected void detectorChanged(Detector detector) {
        System.out.println("TridentMonitoring::detectorChanged  Setting up the plotter");
        aida.tree().cd("/");
        String triggerType = getTriggerType();
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
        double massMin = 0.125;
        double massMax = 0.175;
        tarconMass = aida.histogram1D(plotDir + triggerType + "/" + "Target Constrained Mass (GeV)", 100, massMin, massMax);
        tarconVx = aida.histogram1D(plotDir + triggerType + "/" + "Target Constrained Vx (mm)", 50, -1, 1);
        tarconVy = aida.histogram1D(plotDir + triggerType + "/" + "Target Constrained Vy (mm)", 50, -1, 1);
        tarconVz = aida.histogram1D(plotDir + triggerType + "/" + "Target Constrained Vz (mm)", 50, -10, 10);
        tarconChi2 = aida.histogram1D(plotDir + "/" + triggerType + "/" + "Target Constrained Chi2", 25, 0, 25);
        pyEleVspyPos = aida.histogram2D(plotDir + triggerType + "/" + "Py(e) vs Py(p)", 50, -0.04, 0.04, 50, -0.04, 0.04);
        pxEleVspxPos = aida.histogram2D(plotDir + triggerType + "/" + "Px(e) vs Px(p)", 50, -0.02, 0.06, 50, -0.02, 0.06);
        trackTimeDiff = aida.histogram1D(plotDir + triggerType + "/" + "Track time difference", 100, -10, 10);
        trackTime2D = aida.histogram2D(plotDir + triggerType + "/" + "Track time vs. track time", 100, -10, 10, 100, -10, 10);
        vertexMassMomentum = aida.histogram2D(plotDir + triggerType + "/" + "Vertex mass vs. vertex momentum", 100, 0, 1.1, 100, 0, 0.1);
        vertexedTrackMomentum2D = aida.histogram2D(plotDir + triggerType + "/" + "Positron vs. electron momentum", 100, 0, 1.1, 100, 0, 1.1);
        vertexedTrackMomentum2DRad = aida.histogram2D(plotDir + triggerType + "/" + "Positron vs. electron momentum: Radiative", 100, 0, 1.1, 100, 0, 1.1);
        vertexPxPy = aida.histogram2D(plotDir + triggerType + "/" + "Vertex Py vs. Px", 100, -0.02, 0.06, 100, -0.04, 0.04);
        goodVertexMass = aida.histogram1D(plotDir + triggerType + "/" + "Unconstrained Mass", 100, massMin, massMax);
        deltaP = aida.histogram1D(plotDir + triggerType + "/" + "Positron - electron momentum", 100, -1., 1.0);
        deltaPRad = aida.histogram1D(plotDir + triggerType + "/" + "Positron - electron momentum", 100, -1., 1.0);
        sumP = aida.histogram1D(plotDir + triggerType + "/" + "Positron + electron momentum", 100, 0.2, 1.25);
        vertexX = aida.histogram1D(plotDir + triggerType + "/" + "Vertex X Position (mm)", 100, -v0VxMax, v0VxMax);
        vertexY = aida.histogram1D(plotDir + triggerType + "/" + "Vertex Y Position (mm)", 100, -v0VyMax, v0VyMax);
        vertexZ = aida.histogram1D(plotDir + triggerType + "/" + "Vertex Z Position (mm)", 100, -v0VzMax, v0VzMax);
        vertexPx = aida.histogram1D(plotDir + triggerType + "/" + "Vertex Px (GeV)", 100, -0.1, 0.1);
        vertexPy = aida.histogram1D(plotDir + triggerType + "/" + "Vertex Py (GeV)", 100, -0.1, 0.1);
        vertexPz = aida.histogram1D(plotDir + triggerType + "/" + "Vertex Pz (GeV)", 100, 0.0, v0PzMax);
        vertexU = aida.histogram1D(plotDir + triggerType + "/" + "Vertex Px over Ptot (GeV)", 100, -0.1, 0.1);
        vertexV = aida.histogram1D(plotDir + triggerType + "/" + "Vertex Py over Ptot (GeV)", 100, -0.1, 0.1);
//        vertexW = aida.histogram1D(plotDir + triggerType + "/" + "Vertex Pz overPtot (GeV)", 100, 0.95, 1.0);
        vertexVZ = aida.histogram2D(plotDir + triggerType + "/" + "Vertex Py over Ptot vs. Z", 100, -v0VzMax, v0VzMax, 100, -0.1, 0.1);
        vertexZY = aida.histogram2D(plotDir + triggerType + "/" + "Vertex Z vs. Y", 100, -v0VyMax, v0VyMax, 100, -v0VzMax, v0VzMax);
    }

    @Override
    public void process(EventHeader event) {
        /*  make sure everything is there */
        if (!event.hasCollection(ReconstructedParticle.class, finalStateParticlesColName))
            return;
        if (!event.hasCollection(ReconstructedParticle.class, unconstrainedV0CandidatesColName))
            return;
        if (!event.hasCollection(ReconstructedParticle.class, beamConV0CandidatesColName))
            return;
        if (!event.hasCollection(ReconstructedParticle.class, targetV0ConCandidatesColName))
            return;
        if (!event.hasCollection(Track.class, trackListName))
            return;

        //check to see if this event is from the correct trigger (or "all");
        if (!matchTrigger(event))
            return;

        nRecoEvents++;

        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);
//
//        List<Track> trks = event.get(Track.class, trackListName);
//        int ntracks = trks.size();
//        if (ntracks > nTrkMax || ntracks < 2)
//            return;
//        List<ReconstructedParticle> fspList = event.get(ReconstructedParticle.class, finalStateParticlesColName);
//        int npos = 0;
//        for (ReconstructedParticle fsp : fspList)
//            if (fsp.getCharge() > 0)
//                npos++;
//        if (npos < 1 || npos > nPosMax)
//            return;

        nPassBasicCuts++;//passed some basic event-level cuts...

        List<ReconstructedParticle> unConstrainedV0List = event.get(ReconstructedParticle.class, unconstrainedV0CandidatesColName);
        for (ReconstructedParticle uncV0 : unConstrainedV0List) {
//            System.out.println("Vertex Track Type = " + uncV0.getType());
            if (uncV0.getType() < trackType) {
//                System.out.println("Vertex Track Type = " + uncV0.getType() + "; cut = " + trackType);
                continue;
            }
            Vertex uncVert = uncV0.getStartVertex();
//  v0 & vertex-quality cuts
//            Hep3Vector v0Mom = uncV0.getMomentum();
            Hep3Vector v0Mom = VecOp.add(uncV0.getParticles().get(1).getMomentum(), uncV0.getParticles().get(0).getMomentum());
 //           System.out.println(v0Mom.toString());
            if (v0Mom.z() > v0PzMax || v0Mom.z() < v0PzMin)
                continue;
            if (Math.abs(v0Mom.y()) > v0PyMax)
                continue;
            if (Math.abs(v0Mom.x()) > v0PxMax)
                continue;
            Hep3Vector v0Vtx = uncVert.getPosition();
 //           System.out.println(v0Vtx.toString());
            if (Math.abs(v0Vtx.z()) > v0VzMax)
                continue;
            if (Math.abs(v0Vtx.y()) > v0VyMax)
                continue;
            if (Math.abs(v0Vtx.x()) > v0VxMax)
                continue;
//            System.out.println("Passed V0 cuts");
            List<Track> tracks = new ArrayList<Track>();
            ReconstructedParticle electron = null, positron = null;
            for (ReconstructedParticle particle : uncV0.getParticles()) //                tracks.addAll(particle.getTracks());  //add add electron first, then positron...down below
                if (particle.getCharge() > 0)
                    positron = particle;
                else if (particle.getCharge() < 0)
                    electron = particle;
                else
                    throw new RuntimeException("expected only electron and positron in vertex, got something with charge 0");
            if (electron == null || positron == null)
                throw new RuntimeException("vertex needs e+ and e- but is missing one or both");
 //           System.out.println("...");
            tracks.add(electron.getTracks().get(0));
            tracks.add(positron.getTracks().get(0));
            if (tracks.size() != 2)
                throw new RuntimeException("expected two tracks in vertex, got " + tracks.size());
            List<Double> trackTimes = new ArrayList<Double>();
            for (Track track : tracks)
                trackTimes.add(TrackUtils.getTrackTime(track, hitToStrips, hitToRotated));
            trackTime2D.fill(trackTimes.get(0), trackTimes.get(1));
            trackTimeDiff.fill(trackTimes.get(0) - trackTimes.get(1));
            boolean trackTimeDiffCut = Math.abs(trackTimes.get(0) - trackTimes.get(1)) < trkTimeDiff;
            boolean pCut = electron.getMomentum().magnitude() > trkPzMin && positron.getMomentum().magnitude() > trkPzMin;
            boolean pTotCut = uncV0.getMomentum().magnitude() > v0PzMin && uncV0.getMomentum().magnitude() < v0PzMax;
//            if (electron.getMomentum().y()* positron.getMomentum().y()<0) continue;
//            System.out.println("Track Time Diff Cut = " + trackTimeDiffCut);
            if (trackTimeDiffCut) {
                vertexMassMomentum.fill(uncV0.getMomentum().magnitude(), uncV0.getMass());
                vertexedTrackMomentum2D.fill(electron.getMomentum().magnitude(), positron.getMomentum().magnitude());
                pyEleVspyPos.fill(electron.getMomentum().y(), positron.getMomentum().y());
                pxEleVspxPos.fill(electron.getMomentum().x(), positron.getMomentum().x());
                sumP.fill(uncV0.getMomentum().magnitude());
                deltaP.fill(positron.getMomentum().magnitude() - electron.getMomentum().magnitude());
                if (uncV0.getMomentum().magnitude() > radCut) {
                    vertexedTrackMomentum2DRad.fill(electron.getMomentum().magnitude(), positron.getMomentum().magnitude());
                    deltaPRad.fill(positron.getMomentum().magnitude() - electron.getMomentum().magnitude());
                }
//                System.out.println("pCut = " + pCut + "; pTotCut=" + pTotCut);

                if (pCut && pTotCut) {
                    vertexPxPy.fill(uncV0.getMomentum().x(), uncV0.getMomentum().y());
                    goodVertexMass.fill(uncV0.getMass());
                    vertexX.fill(v0Vtx.x());
                    vertexY.fill(v0Vtx.y());
                    vertexZ.fill(v0Vtx.z());
                    vertexPx.fill(uncV0.getMomentum().x());
                    vertexPy.fill(uncV0.getMomentum().y());
                    vertexPz.fill(uncV0.getMomentum().z());
                    vertexU.fill(uncV0.getMomentum().x() / uncV0.getMomentum().magnitude());
                    vertexV.fill(uncV0.getMomentum().y() / uncV0.getMomentum().magnitude());
//                    vertexW.fill(uncV0.getMomentum().z()/uncV0.getMomentum().magnitude());
                    vertexVZ.fill(v0Vtx.z(), uncV0.getMomentum().y() / uncV0.getMomentum().magnitude());
                    vertexZY.fill(v0Vtx.y(), v0Vtx.z());
                }
            }
//            System.out.println(tarV0.getTracks())
        }

        List<ReconstructedParticle> targetConstrainedV0List = event.get(ReconstructedParticle.class, targetV0ConCandidatesColName);
        for (ReconstructedParticle targetV0 : targetConstrainedV0List) {
            if (targetV0.getType() < trackType)
                continue;
            Vertex targetVert = targetV0.getStartVertex();
            //  v0 & vertex-quality cuts

            Hep3Vector v0Mom = VecOp.add(targetV0.getParticles().get(1).getMomentum(), targetV0.getParticles().get(0).getMomentum());
            if (v0Mom.z() > v0PzMax || v0Mom.z() < v0PzMin)
                continue;
            if (Math.abs(v0Mom.y()) > v0PyMax)
                continue;
            if (Math.abs(v0Mom.x()) > v0PxMax)
                continue;
            Hep3Vector v0Vtx = targetVert.getPosition();
            if (Math.abs(v0Vtx.z()) > v0VzMax)
                continue;
            if (Math.abs(v0Vtx.y()) > v0VyMax)
                continue;
            if (Math.abs(v0Vtx.x()) > v0VxMax)
                continue;
            tarconMass.fill(targetV0.getMass());
            tarconVx.fill(v0Vtx.x());
            tarconVy.fill(v0Vtx.y());
            tarconVz.fill(v0Vtx.z());
            tarconChi2.fill(targetVert.getChi2());

        }
    }

    @Override
    public void printDQMData() {
        System.out.println("TridentMonitoring::printDQMData");
        for (Entry<String, Double> entry : monitoredQuantityMap.entrySet())
            System.out.println(entry.getKey() + " = " + entry.getValue());
        System.out.println("*******************************");
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

            System.out.println("ALTER TABLE dqm ADD " + fpQuantNames[i] + " double;");
    }

    IFitResult fitVertexPosition(IHistogram1D h1d, IFitter fitter, double[] init, String range) {
        return fitter.fit(h1d, "g+p1", init, range);
    }

}
