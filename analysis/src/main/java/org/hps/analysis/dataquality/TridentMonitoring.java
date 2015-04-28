package org.hps.analysis.dataquality;

import hep.aida.IAnalysisFactory;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.physics.vec.Hep3Vector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.Vertex;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.geometry.Detector;

/**
 * DQM driver V0 particles (i.e. e+e- pars) plots things like number of vertex
 * position an mass
 *
 * @author mgraham on May 14, 2014
 *
 */
public class TridentMonitoring extends DataQualityMonitor {

    private final String helicalTrackHitRelationsCollectionName = "HelicalTrackHitRelations";
    private final String rotatedHelicalTrackHitRelationsCollectionName = "RotatedHelicalTrackHitRelations";
    private double ebeam = 2.2;
    String finalStateParticlesColName = "FinalStateParticles";
    String unconstrainedV0CandidatesColName = "UnconstrainedV0Candidates";
    String beamConV0CandidatesColName = "BeamspotConstrainedV0Candidates";
    String targetV0ConCandidatesColName = "TargetConstrainedV0Candidates";
    String trackListName = "MatchedTracks";
    String[] fpQuantNames = {"nV0_per_Event", "avg_BSCon_mass", "avg_BSCon_Vx", "avg_BSCon_Vy", "avg_BSCon_Vz", "sig_BSCon_Vx", "sig_BSCon_Vy", "sig_BSCon_Vz", "avg_BSCon_Chi2"};

    boolean debug = false;
    private String plotDir = "TridentMonitoring/";
    IHistogram2D trackTime2D;
    IHistogram1D trackTimeDiff;
    IHistogram2D vertexMassMomentum;
    IHistogram2D vertexedTrackMomentum2D;
    IHistogram2D vertexPxPy;
    IHistogram1D goodVertexMass;

    //clean up event first
    int nTrkMax = 3;
    int nPosMax = 1;
    //v0 cuts   
    double v0Chi2 = 10;
    double v0PzMax = 1.1 * ebeam;//GeV 
    double v0PzMin = 0.1;// GeV
    double v0PyMax = 0.2;//GeV absolute value
    double v0PxMax = 0.2;//GeV absolute value
    double v0VzMax = 5.0;// mm from target...someday make mass dependent
    double v0VyMax = 0.5;// mm from target...someday make mass dependent
    double v0VxMax = 0.5;// mm from target...someday make mass dependent
    //  track quality cuts
    double trkChi2 = 10;
    double trkPzMax = 0.9 * ebeam;//GeV
    double trkPzMin = 0.1;//GeV
    double trkPyMax = 0.2;//GeV absolute value
    double trkPxMax = 0.2;//GeV absolute value
    double trkTimeDiff = 5.0;
//cluster matching
    boolean reqCluster = true;
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

    @Override
    protected void detectorChanged(Detector detector) {
        System.out.println("TridentMonitoring::detectorChanged  Setting up the plotter");
        aida.tree().cd("/");

        /*  V0 Quantities   */
        /*  Mass, vertex, chi^2 of fit */
        /* beamspot constrained */
//        IHistogram1D nV0 = aida.histogram1D(plotDir + "Number of V0 per event", 10, 0, 10);
//        IHistogram1D bsconMass = aida.histogram1D(plotDir + "BS Constrained Mass (GeV)", 100, 0, 0.200);
//        IHistogram1D bsconVx = aida.histogram1D(plotDir + "BS Constrained Vx (mm)", 50, -1, 1);
//        IHistogram1D bsconVy = aida.histogram1D(plotDir + "BS Constrained Vy (mm)", 50, -1, 1);
//        IHistogram1D bsconVz = aida.histogram1D(plotDir + "BS Constrained Vz (mm)", 50, -10, 10);
//        IHistogram1D bsconChi2 = aida.histogram1D(plotDir + "BS Constrained Chi2", 25, 0, 25);
//        /* target constrained */
//        IHistogram1D tarconMass = aida.histogram1D(plotDir + "Target Constrained Mass (GeV)", 100, 0, 0.200);
//        IHistogram1D tarconVx = aida.histogram1D(plotDir + "Target Constrained Vx (mm)", 50, -1, 1);
//        IHistogram1D tarconVy = aida.histogram1D(plotDir + "Target Constrained Vy (mm)", 50, -1, 1);
//        IHistogram1D tarconVz = aida.histogram1D(plotDir + "Target Constrained Vz (mm)", 50, -10, 10);
//        IHistogram1D tarconChi2 = aida.histogram1D(plotDir + "Target Constrained Chi2", 25, 0, 25);
        trackTimeDiff = aida.histogram1D(plotDir + "Track time difference", 100, -25, 25);
        trackTime2D = aida.histogram2D(plotDir + "Track time vs. track time", 100, -50, 100, 100, -50, 100);
        vertexMassMomentum = aida.histogram2D(plotDir + "Vertex mass vs. vertex momentum", 100, 0, 4.0, 100, 0, 1.0);
        vertexedTrackMomentum2D = aida.histogram2D(plotDir + "Positron vs. electron momentum", 100, 0, 2.5, 100, 0, 2.5);
        vertexPxPy = aida.histogram2D(plotDir + "Vertex Py vs. Px", 100, -0.1, 0.2, 100, -0.1, 0.1);
        goodVertexMass = aida.histogram1D(plotDir + "Good vertex mass", 100, 0, 0.5);

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

        nRecoEvents++;

        RelationalTable hittostrip = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> hitrelations = event.get(LCRelation.class, helicalTrackHitRelationsCollectionName);
        for (LCRelation relation : hitrelations)
            if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                hittostrip.add(relation.getFrom(), relation.getTo());

        RelationalTable hittorotated = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> rotaterelations = event.get(LCRelation.class, rotatedHelicalTrackHitRelationsCollectionName);
        for (LCRelation relation : rotaterelations)
            if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                hittorotated.add(relation.getFrom(), relation.getTo());

        List<Track> trks = event.get(Track.class, trackListName);
        int ntracks = trks.size();
        if (ntracks > nTrkMax || ntracks < 2)
            return;
        List<ReconstructedParticle> fspList = event.get(ReconstructedParticle.class, finalStateParticlesColName);
        int npos = 0;
        for (ReconstructedParticle fsp : fspList)
            if (fsp.getCharge() > 0)
                npos++;
        if (npos < 1 || npos > nPosMax)
            return;

        nPassBasicCuts++;//passed some basic event-level cuts...

        List<ReconstructedParticle> targetConstrainedV0List = event.get(ReconstructedParticle.class, targetV0ConCandidatesColName);
        for (ReconstructedParticle tarV0 : targetConstrainedV0List) {
            Vertex tarVert = tarV0.getStartVertex();
//  v0 & vertex-quality cuts
            Hep3Vector v0Mom = tarV0.getMomentum();
            if (v0Mom.z() > v0PzMax || v0Mom.z() < v0PzMin)
                break;
            if (Math.abs(v0Mom.y()) > v0PyMax)
                break;
            if (Math.abs(v0Mom.x()) > v0PxMax)
                break;
            Hep3Vector v0Vtx = tarVert.getPosition();
            if (Math.abs(v0Vtx.z()) > v0VzMax)
                break;
            if (Math.abs(v0Vtx.y()) > v0VyMax)
                break;
            if (Math.abs(v0Vtx.x()) > v0VxMax)
                break;

            List<Track> tracks = new ArrayList<Track>();
            ReconstructedParticle electron = null, positron = null;
            for (ReconstructedParticle particle : tarV0.getParticles()) {
                tracks.addAll(particle.getTracks());
                if (particle.getCharge() > 0)
                    positron = particle;
                else if (particle.getCharge() < 0)
                    electron = particle;
                else
                    throw new RuntimeException("expected only electron and positron in vertex, got something with charge 0");
            }
            if (tracks.size() != 2)
                throw new RuntimeException("expected two tracks in vertex, got " + tracks.size());
            List<Double> trackTimes = new ArrayList<Double>();
            for (Track track : tracks) {
                int nStrips = 0;
                double meanTime = 0;
                for (TrackerHit hit : track.getTrackerHits()) {
                    Collection<TrackerHit> htsList = hittostrip.allFrom(hittorotated.from(hit));
                    for (TrackerHit hts : htsList) {
                        nStrips++;
                        meanTime += hts.getTime();
                    }
                }
                meanTime /= nStrips;
                trackTimes.add(meanTime);
            }
            trackTime2D.fill(trackTimes.get(0), trackTimes.get(1));
            trackTimeDiff.fill(trackTimes.get(0) - trackTimes.get(1));
            boolean trackTimeDiffCut = Math.abs(trackTimes.get(0) - trackTimes.get(1)) < trkTimeDiff;
            boolean pCut = electron.getMomentum().magnitude() > trkPzMin && positron.getMomentum().magnitude() > trkPzMin;
            boolean pTotCut = tarV0.getMomentum().magnitude() > v0PzMin && tarV0.getMomentum().magnitude() < v0PzMax;
            if (trackTimeDiffCut) {
                vertexMassMomentum.fill(tarV0.getMomentum().magnitude(), tarV0.getMass());
                vertexedTrackMomentum2D.fill(electron.getMomentum().magnitude(), positron.getMomentum().magnitude());
                if (pCut && pTotCut) {
                    vertexPxPy.fill(tarV0.getMomentum().x(), tarV0.getMomentum().y());
                    goodVertexMass.fill(tarV0.getMass());
                }
            }
//            System.out.println(tarV0.getTracks())
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
