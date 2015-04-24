package org.hps.analysis.dataquality;

import hep.aida.IAnalysisFactory;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Vertex;
import org.lcsim.geometry.Detector;

/**
 * DQM driver V0 particles (i.e. e+e- pars) plots
 * things like number of vertex position an mass
 *
 * @author mgraham on May 14, 2014
 *
 */
public class V0Monitoring extends DataQualityMonitor {

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
    private String plotDir = "V0Monitoring/";

    @Override
    protected void detectorChanged(Detector detector) {
        System.out.println("V0Monitoring::detectorChanged  Setting up the plotter");
        aida.tree().cd("/");

        /*  V0 Quantities   */
        /*  Mass, vertex, chi^2 of fit */
        /* beamspot constrained */
        IHistogram1D nV0 = aida.histogram1D(plotDir + "Number of V0 per event", 10, 0, 10);
        IHistogram1D bsconMass = aida.histogram1D(plotDir + "BS Constrained Mass (GeV)", 100, 0, 0.200);
        IHistogram1D bsconVx = aida.histogram1D(plotDir + "BS Constrained Vx (mm)", 200, -5, 5);
        IHistogram1D bsconVy = aida.histogram1D(plotDir + "BS Constrained Vy (mm)", 200, -5, 5);
        IHistogram1D bsconVz = aida.histogram1D(plotDir + "BS Constrained Vz (mm)", 200, -50, 50);
        IHistogram1D bsconChi2 = aida.histogram1D(plotDir + "BS Constrained Chi2", 100, 0, 100);
        /* target constrained */
        IHistogram1D tarconMass = aida.histogram1D(plotDir + "Target Constrained Mass (GeV)", 100, 0, 0.200);
        IHistogram1D tarconVx = aida.histogram1D(plotDir + "Target Constrained Vx (mm)", 200, -5, 5);
        IHistogram1D tarconVy = aida.histogram1D(plotDir + "Target Constrained Vy (mm)", 200, -5, 5);
        IHistogram1D tarconVz = aida.histogram1D(plotDir + "Target Constrained Vz (mm)", 200, -50, 50);
        IHistogram1D tarconChi2 = aida.histogram1D(plotDir + "Target Constrained Chi2", 100, 0, 100);

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
        nRecoEvents++;

        List<ReconstructedParticle> beamConstrainedV0List = event.get(ReconstructedParticle.class, beamConV0CandidatesColName);
        aida.histogram1D(plotDir + "Number of V0 per event").fill(beamConstrainedV0List.size());
        for (ReconstructedParticle bsV0 : beamConstrainedV0List) {
            nTotV0++;
            Vertex bsVert = bsV0.getStartVertex();
            aida.histogram1D(plotDir + "BS Constrained Vx (mm)").fill(bsVert.getPosition().x());
            aida.histogram1D(plotDir + "BS Constrained Vy (mm)").fill(bsVert.getPosition().y());
            aida.histogram1D(plotDir + "BS Constrained Vz (mm)").fill(bsVert.getPosition().z());
            aida.histogram1D(plotDir + "BS Constrained Mass (GeV)").fill(bsV0.getMass());
            aida.histogram1D(plotDir + "BS Constrained Chi2").fill(bsVert.getChi2());
            sumMass += bsV0.getMass();
            sumVx += bsVert.getPosition().x();
            sumVy += bsVert.getPosition().y();
            sumVz += bsVert.getPosition().z();
            sumChi2 += bsVert.getChi2();
        }

        List<ReconstructedParticle> targetConstrainedV0List = event.get(ReconstructedParticle.class, targetV0ConCandidatesColName);
        for (ReconstructedParticle tarV0 : targetConstrainedV0List) {
            Vertex tarVert = tarV0.getStartVertex();
            aida.histogram1D(plotDir + "Target Constrained Vx (mm)").fill(tarVert.getPosition().x());
            aida.histogram1D(plotDir + "Target Constrained Vy (mm)").fill(tarVert.getPosition().y());
            aida.histogram1D(plotDir + "Target Constrained Vz (mm)").fill(tarVert.getPosition().z());
            aida.histogram1D(plotDir + "Target Constrained Mass (GeV)").fill(tarV0.getMass());
            aida.histogram1D(plotDir + "Target Constrained Chi2").fill(tarVert.getChi2());
        }
    }

    @Override
    public void printDQMData() {
        System.out.println("V0Monitoring::printDQMData");
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
        IHistogram1D bsconVx = aida.histogram1D(plotDir + "BS Constrained Vx (mm)");
        IHistogram1D bsconVy = aida.histogram1D(plotDir + "BS Constrained Vy (mm)");
        IHistogram1D bsconVz = aida.histogram1D(plotDir + "BS Constrained Vz (mm)");
        double[] init = {50.0, 0.0, 0.2, 1.0, 0.0};
        IFitResult resVx = fitVertexPosition(bsconVx, fitter, init, "range=\"(-0.5,0.5)\"");
        double[] init2 = {50.0, 0.0, 0.04, 1.0, 0.0};
        IFitResult resVy = fitVertexPosition(bsconVy, fitter, init2, "range=\"(-0.2,0.2)\"");
        double[] init3 = {50.0, 0.0, 3.0, 1.0, 0.0};
        IFitResult resVz = fitVertexPosition(bsconVz, fitter, init3, "range=\"(-6,6)\"");

        double[] parsVx = resVx.fittedParameters();
        double[] parsVy = resVy.fittedParameters();
        double[] parsVz = resVz.fittedParameters();

        for (int i = 0; i < 5; i++)
            System.out.println("Vertex Fit Parameters:  " + resVx.fittedParameterNames()[i] + " = " + parsVx[i] + "; " + parsVy[i] + "; " + parsVz[i]);

        IPlotter plotter = analysisFactory.createPlotterFactory().create("Vertex Position");
        plotter.createRegions(1, 3);
        IPlotterStyle pstyle = plotter.style();
        pstyle.legendBoxStyle().setVisible(false);
        pstyle.dataStyle().fillStyle().setColor("green");
        pstyle.dataStyle().lineStyle().setColor("black");
        plotter.region(0).plot(bsconVx);
        plotter.region(0).plot(resVx.fittedFunction());
        plotter.region(1).plot(bsconVy);
        plotter.region(1).plot(resVy.fittedFunction());
        plotter.region(2).plot(bsconVz);
        plotter.region(2).plot(resVz.fittedFunction());
        if(outputPlots){
        try {
            plotter.writeToFile(outputPlotDir +"vertex.png");
        } catch (IOException ex) {
            Logger.getLogger(V0Monitoring.class.getName()).log(Level.SEVERE, null, ex);
        }
        }

        monitoredQuantityMap.put(fpQuantNames[0], (double) nTotV0 / nRecoEvents);
        monitoredQuantityMap.put(fpQuantNames[1], sumMass / nTotV0);
//        monitoredQuantityMap.put(fpQuantNames[2], sumVx / nTotV0);
//        monitoredQuantityMap.put(fpQuantNames[3], sumVy / nTotV0);
//        monitoredQuantityMap.put(fpQuantNames[4], sumVz / nTotV0);
        monitoredQuantityMap.put(fpQuantNames[2], parsVx[1]);
        monitoredQuantityMap.put(fpQuantNames[3], parsVy[1]);
        monitoredQuantityMap.put(fpQuantNames[4], parsVz[1]);
        monitoredQuantityMap.put(fpQuantNames[5], parsVx[2]);
        monitoredQuantityMap.put(fpQuantNames[6], parsVy[2]);
        monitoredQuantityMap.put(fpQuantNames[7], parsVz[2]);

        monitoredQuantityMap.put(fpQuantNames[8], sumChi2 / nTotV0);

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
