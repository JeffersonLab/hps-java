package org.hps.analysis.dataquality;

import hep.aida.IAnalysisFactory;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
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
    String[] fpQuantNames = {"nV0_per_Event", "avg_BSCon_mass", "avg_BSCon_Vx", "avg_BSCon_Vy", "avg_BSCon_Vz", "sig_BSCon_Vx", "sig_BSCon_Vy", "sig_BSCon_Vz", "avg_BSCon_Chi2"};
    //some counters
    int nRecoEvents = 0;
    int nTotV0 = 0;
    //some summers
    double sumMass = 0.0;
    double sumVx = 0.0;
    double sumVy = 0.0;
    double sumVz = 0.0;
    double sumChi2 = 0.0;

    boolean debug = false;
    private String plotDir = "TridentMonitoring/";
    IHistogram2D trackTime2D;
    IHistogram1D trackTimeDiff;
    IHistogram2D vertexMassMomentum;
    IHistogram2D vertexedTrackMomentum2D;
    IHistogram2D vertexPxPy;
    IHistogram1D goodVertexMass;

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
        nRecoEvents++;

        RelationalTable hittostrip = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> hitrelations = event.get(LCRelation.class, helicalTrackHitRelationsCollectionName);
        for (LCRelation relation : hitrelations) {
            if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                hittostrip.add(relation.getFrom(), relation.getTo());
            }
        }

        RelationalTable hittorotated = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> rotaterelations = event.get(LCRelation.class, rotatedHelicalTrackHitRelationsCollectionName);
        for (LCRelation relation : rotaterelations) {
            if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                hittorotated.add(relation.getFrom(), relation.getTo());
            }
        }

        List<ReconstructedParticle> beamConstrainedV0List = event.get(ReconstructedParticle.class, beamConV0CandidatesColName);
//        aida.histogram1D(plotDir + "Number of V0 per event").fill(beamConstrainedV0List.size());
        for (ReconstructedParticle bsV0 : beamConstrainedV0List) {
            nTotV0++;
            Vertex bsVert = bsV0.getStartVertex();
//            aida.histogram1D(plotDir + "BS Constrained Vx (mm)").fill(bsVert.getPosition().x());
//            aida.histogram1D(plotDir + "BS Constrained Vy (mm)").fill(bsVert.getPosition().y());
//            aida.histogram1D(plotDir + "BS Constrained Vz (mm)").fill(bsVert.getPosition().z());
//            aida.histogram1D(plotDir + "BS Constrained Mass (GeV)").fill(bsV0.getMass());
//            aida.histogram1D(plotDir + "BS Constrained Chi2").fill(bsVert.getChi2());
            sumMass += bsV0.getMass();
            sumVx += bsVert.getPosition().x();
            sumVy += bsVert.getPosition().y();
            sumVz += bsVert.getPosition().z();
            sumChi2 += bsVert.getChi2();
        }

        List<ReconstructedParticle> targetConstrainedV0List = event.get(ReconstructedParticle.class, targetV0ConCandidatesColName);
        for (ReconstructedParticle tarV0 : targetConstrainedV0List) {
            Vertex tarVert = tarV0.getStartVertex();
//            aida.histogram1D(plotDir + "Target Constrained Vx (mm)").fill(tarVert.getPosition().x());
//            aida.histogram1D(plotDir + "Target Constrained Vy (mm)").fill(tarVert.getPosition().y());
//            aida.histogram1D(plotDir + "Target Constrained Vz (mm)").fill(tarVert.getPosition().z());
//            aida.histogram1D(plotDir + "Target Constrained Mass (GeV)").fill(tarV0.getMass());
//            aida.histogram1D(plotDir + "Target Constrained Chi2").fill(tarVert.getChi2());
            List<Track> tracks = new ArrayList<Track>();
            ReconstructedParticle electron = null, positron = null;
            for (ReconstructedParticle particle : tarV0.getParticles()) {
                tracks.addAll(particle.getTracks());
                if (particle.getCharge() > 0) {
                    positron = particle;
                } else if (particle.getCharge() < 0) {
                    electron = particle;
                } else {
                    throw new RuntimeException("expected only electron and positron in vertex, got something with charge 0");
                }
            }
            if (tracks.size() != 2) {
                throw new RuntimeException("expected two tracks in vertex, got " + tracks.size());
            }
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
            boolean trackTimeDiffCut = Math.abs(trackTimes.get(0) - trackTimes.get(1)) < 5.0;
            boolean pCut = electron.getMomentum().magnitude() > 0.4 && positron.getMomentum().magnitude() > 0.4;
            boolean pTotCut = tarV0.getMomentum().magnitude() > 0.8 * 2.2 && tarV0.getMomentum().magnitude() < 2.2;
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
        for (Entry<String, Double> entry : monitoredQuantityMap.entrySet()) {
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }
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
//        IHistogram1D bsconVx = aida.histogram1D(plotDir + "BS Constrained Vx (mm)");
//        IHistogram1D bsconVy = aida.histogram1D(plotDir + "BS Constrained Vy (mm)");
//        IHistogram1D bsconVz = aida.histogram1D(plotDir + "BS Constrained Vz (mm)");
//        double[] init = {50.0, 0.0, 0.2, 1.0, 0.0};
//        IFitResult resVx = fitVertexPosition(bsconVx, fitter, init, "range=\"(-0.5,0.5)\"");
//        double[] init2 = {50.0, 0.0, 0.04, 1.0, 0.0};
//        IFitResult resVy = fitVertexPosition(bsconVy, fitter, init2, "range=\"(-0.2,0.2)\"");
//        double[] init3 = {50.0, 0.0, 3.0, 1.0, 0.0};
//        IFitResult resVz = fitVertexPosition(bsconVz, fitter, init3, "range=\"(-6,6)\"");
//
//        double[] parsVx = resVx.fittedParameters();
//        double[] parsVy = resVy.fittedParameters();
//        double[] parsVz = resVz.fittedParameters();
//
//        for (int i = 0; i < 5; i++) {
//            System.out.println("Vertex Fit Parameters:  " + resVx.fittedParameterNames()[i] + " = " + parsVx[i] + "; " + parsVy[i] + "; " + parsVz[i]);
//        }
//
//        IPlotter plotter = analysisFactory.createPlotterFactory().create("Vertex Position");
//        plotter.createRegions(1, 3);
//        IPlotterStyle pstyle = plotter.style();
//        pstyle.legendBoxStyle().setVisible(false);
//        pstyle.dataStyle().fillStyle().setColor("green");
//        pstyle.dataStyle().lineStyle().setColor("black");
//        plotter.region(0).plot(bsconVx);
//        plotter.region(0).plot(resVx.fittedFunction());
//        plotter.region(1).plot(bsconVy);
//        plotter.region(1).plot(resVy.fittedFunction());
//        plotter.region(2).plot(bsconVz);
//        plotter.region(2).plot(resVz.fittedFunction());
//        if (outputPlots) {
//            try {
//                plotter.writeToFile(outputPlotDir + "vertex.png");
//            } catch (IOException ex) {
//                Logger.getLogger(TridentMonitoring.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//
//        monitoredQuantityMap.put(fpQuantNames[0], (double) nTotV0 / nRecoEvents);
//        monitoredQuantityMap.put(fpQuantNames[1], sumMass / nTotV0);
////        monitoredQuantityMap.put(fpQuantNames[2], sumVx / nTotV0);
////        monitoredQuantityMap.put(fpQuantNames[3], sumVy / nTotV0);
////        monitoredQuantityMap.put(fpQuantNames[4], sumVz / nTotV0);
//        monitoredQuantityMap.put(fpQuantNames[2], parsVx[1]);
//        monitoredQuantityMap.put(fpQuantNames[3], parsVy[1]);
//        monitoredQuantityMap.put(fpQuantNames[4], parsVz[1]);
//        monitoredQuantityMap.put(fpQuantNames[5], parsVx[2]);
//        monitoredQuantityMap.put(fpQuantNames[6], parsVy[2]);
//        monitoredQuantityMap.put(fpQuantNames[7], parsVz[2]);
//
//        monitoredQuantityMap.put(fpQuantNames[8], sumChi2 / nTotV0);

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
