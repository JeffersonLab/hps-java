package org.hps.monitoring.drivers.trackrecon;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.Vertex;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author mgraham
 */
public class V0ReconPlots extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    String finalStateParticlesColName = "FinalStateParticles";
    String unconstrainedV0CandidatesColName = "UnconstrainedV0Candidates";
    String beamConV0CandidatesColName = "BeamspotConstrainedV0Candidates";
    String targetV0ConCandidatesColName = "TargetConstrainedV0Candidates";
    //some counters
    int nRecoEvents = 0;
    boolean debug = false;

    IPlotter plotterUncon;
    IPlotter plotter2d;
    String outputPlots;

    @Override
    protected void detectorChanged(Detector detector) {
        System.out.println("V0Monitoring::detectorChanged  Setting up the plotter");

        aida.tree().cd("/");
//        resetOccupancyMap(); // this is for calculating averages         
        IAnalysisFactory fac = aida.analysisFactory();

        plotterUncon = fac.createPlotterFactory().create("HPS Tracking Plots");
        setupPlotter(plotterUncon, "Unconstrained V0s");
        plotterUncon.createRegions(2, 3);

        /*  V0 Quantities   */
        /*  Mass, vertex, chi^2 of fit */
        /* beamspot constrained */
        IHistogram1D nV0 = aida.histogram1D("Number of V0 per event", 5, 0, 5);
        IHistogram1D unconMass = aida.histogram1D("Unconstrained Mass (GeV)", 100, 0, 0.200);
        IHistogram1D unconVx = aida.histogram1D("Unconstrained Vx (mm)", 50, -1, 1);
        IHistogram1D unconVy = aida.histogram1D("Unconstrained Vy (mm)", 50, -0.6, 0.6);
        IHistogram1D unconVz = aida.histogram1D("Unconstrained Vz (mm)", 50, -10, 10);
        IHistogram1D unconChi2 = aida.histogram1D("Unconstrained Chi2", 25, 0, 25);
        plotterUncon.region(0).plot(nV0);
        plotterUncon.region(1).plot(unconMass);
        plotterUncon.region(2).plot(unconChi2);
        plotterUncon.region(3).plot(unconVx);
        plotterUncon.region(4).plot(unconVy);
        plotterUncon.region(5).plot(unconVz);

        plotter2d = fac.createPlotterFactory().create("HPS Tracking Plots");
        setupPlotter(plotter2d, "V0 2D Plots");
        plotter2d.createRegions(2, 2);
        IPlotterStyle style = plotter2d.style();
        style.statisticsBoxStyle().setVisible(false);
        style.setParameter("hist2DStyle", "colorMap");
        style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");

        IHistogram2D pEleVspPos = aida.histogram2D("P(e) vs P(p)", 50, 0, 2.5, 50, 0, 2.5);
        IHistogram2D pyEleVspyPos = aida.histogram2D("Py(e) vs Py(p)", 50, -0.1, 0.1, 50, -0.1, 0.1);
        IHistogram2D pxEleVspxPos = aida.histogram2D("Px(e) vs Px(p)", 50, -0.1, 0.1, 50, -0.1, 0.1);
        IHistogram2D massVsVtxZ = aida.histogram2D("Mass vs Vz", 50, 0, 0.15, 50, -10, 10);
        plotter2d.region(0).plot(pEleVspPos);
        plotter2d.region(1).plot(pxEleVspxPos);
        plotter2d.region(2).plot(massVsVtxZ);
        plotter2d.region(3).plot(pyEleVspyPos);
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

        List<ReconstructedParticle> unConstrainedV0List = event.get(ReconstructedParticle.class, unconstrainedV0CandidatesColName);
        aida.histogram1D("Number of V0 per event").fill(unConstrainedV0List.size());
        for (ReconstructedParticle uncV0 : unConstrainedV0List) {
            Vertex uncVert = uncV0.getStartVertex();
            aida.histogram1D("Unconstrained Vx (mm)").fill(uncVert.getPosition().x());
            aida.histogram1D("Unconstrained Vy (mm)").fill(uncVert.getPosition().y());
            aida.histogram1D("Unconstrained Vz (mm)").fill(uncVert.getPosition().z());
            aida.histogram1D("Unconstrained Mass (GeV)").fill(uncV0.getMass());
            aida.histogram1D("Unconstrained Chi2").fill(uncVert.getChi2());
            aida.histogram2D("Mass vs Vz").fill(uncV0.getMass(), uncVert.getPosition().z());
            //this always has 2 tracks. 
            List<ReconstructedParticle> trks = uncV0.getParticles();
            Track ele = trks.get(0).getTracks().get(0);
            Track pos = trks.get(1).getTracks().get(0);
            //if track #0 has charge>0 it's the electron!  This seems mixed up, but remember the track 
            //charge is assigned assuming a positive B-field, while ours is negative
            if (trks.get(0).getCharge() > 0) {
                pos = trks.get(0).getTracks().get(0);
                ele = trks.get(1).getTracks().get(0);
            }
            aida.histogram2D("P(e) vs P(p)").fill(getMomentum(ele), getMomentum(pos));
            aida.histogram2D("Px(e) vs Px(p)").fill(ele.getTrackStates().get(0).getMomentum()[1], pos.getTrackStates().get(0).getMomentum()[1]);
            aida.histogram2D("Py(e) vs Py(p)").fill(ele.getTrackStates().get(0).getMomentum()[2], pos.getTrackStates().get(0).getMomentum()[2]);
        }
    }

    void setupPlotter(IPlotter plotter, String title) {
        plotter.setTitle(title);
        IPlotterStyle style = plotter.style();
        style.dataStyle().fillStyle().setColor("yellow");
        style.dataStyle().errorBarStyle().setVisible(false);
    }

    public void setOutputPlots(String output) {
        this.outputPlots = output;
    }

    private double getMomentum(Track trk) {

        double px = trk.getTrackStates().get(0).getMomentum()[0];
        double py = trk.getTrackStates().get(0).getMomentum()[1];
        double pz = trk.getTrackStates().get(0).getMomentum()[2];
        return Math.sqrt(px * px + py * py + pz * pz);
    }

    @Override
    public void endOfData() {
        if (outputPlots != null)
            try {
                plotterUncon.writeToFile(outputPlots + "-Unconstrained.gif");
                plotter2d.writeToFile(outputPlots + "-2d.gif");
            } catch (IOException ex) {
                Logger.getLogger(TrackingReconPlots.class.getName()).log(Level.SEVERE, null, ex);
            }

    }

}
