package org.hps.analysis.dataquality;

import hep.aida.IAnalysisFactory;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hps.recon.ecal.triggerbank.AbstractIntData;
import org.hps.recon.ecal.triggerbank.TIData;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
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
    int nTot2Ele = 0;
    //some summers
    double sumMass = 0.0;
    double sumVx = 0.0;
    double sumVy = 0.0;
    double sumVz = 0.0;
    double sumChi2 = 0.0;

    IHistogram2D pEleVspPos;
    IHistogram2D pEleVspPosWithCut;
    IHistogram2D pyEleVspyPos;
    IHistogram2D pxEleVspxPos;
    IHistogram2D pEleVspEle;
    IHistogram2D pyEleVspyEle;
    IHistogram2D pxEleVspxEle;
      IHistogram2D pEleVspEleNoBeam;
    IHistogram2D pyEleVspyEleNoBeam;
    IHistogram2D pxEleVspxEleNoBeam;
    IHistogram2D massVsVtxZ;
    IHistogram2D VtxYVsVtxZ;
    IHistogram2D VtxXVsVtxZ;
    IHistogram2D VtxXVsVtxY;

    IHistogram1D sumChargeHisto;
    IHistogram1D numChargeHisto;

    boolean debug = false;
    private String plotDir = "V0Monitoring/";

    double beamEnergy = 1.05; //GeV
    double maxFactor = 1.25;
    double feeMomentumCut = 0.8; //GeV

    double v0ESumMinCut = 0.8 * beamEnergy;
     double v0ESumMaxCut = 1.1 * beamEnergy;
    double v0MaxPCut = 1.1;//GeV

    @Override
    protected void detectorChanged(Detector detector) {
        System.out.println("V0Monitoring::detectorChanged  Setting up the plotter");
        aida.tree().cd("/");

        /*  V0 Quantities   */
        /*  Mass, vertex, chi^2 of fit */
        /*  unconstrained */
        IHistogram1D unconMass = aida.histogram1D(plotDir + triggerType + "/" + "Unconstrained Mass (GeV)", 100, 0, 0.200);
        IHistogram1D unconVx = aida.histogram1D(plotDir + triggerType + "/" + "Unconstrained Vx (mm)", 50, -10, 10);
        IHistogram1D unconVy = aida.histogram1D(plotDir + triggerType + "/" + "Unconstrained Vy (mm)", 50, -10, 10);
        IHistogram1D unconVz = aida.histogram1D(plotDir + triggerType + "/" + "Unconstrained Vz (mm)", 50, -50, 50);
        IHistogram1D unconChi2 = aida.histogram1D(plotDir + triggerType + "/" + "Unconstrained Chi2", 25, 0, 25);
        /* beamspot constrained */

        IHistogram1D nV0 = aida.histogram1D(plotDir + triggerType + "/" + "Number of V0 per event", 10, 0, 10);
        IHistogram1D bsconMass = aida.histogram1D(plotDir + triggerType + "/" + "BS Constrained Mass (GeV)", 100, 0, 0.200);
        IHistogram1D bsconVx = aida.histogram1D(plotDir + triggerType + "/" + "BS Constrained Vx (mm)", 50, -10, 10);
        IHistogram1D bsconVy = aida.histogram1D(plotDir + triggerType + "/" + "BS Constrained Vy (mm)", 50, -10, 10);
        IHistogram1D bsconVz = aida.histogram1D(plotDir + triggerType + "/" + "BS Constrained Vz (mm)", 50, -50, 50);
        IHistogram1D bsconChi2 = aida.histogram1D(plotDir + triggerType + "/" + "BS Constrained Chi2", 25, 0, 25);
        /* target constrained */
        IHistogram1D tarconMass = aida.histogram1D(plotDir + triggerType + "/" + "Target Constrained Mass (GeV)", 100, 0, 0.200);
        IHistogram1D tarconVx = aida.histogram1D(plotDir + triggerType + "/" + "Target Constrained Vx (mm)", 50, -1, 1);
        IHistogram1D tarconVy = aida.histogram1D(plotDir + triggerType + "/" + "Target Constrained Vy (mm)", 50, -1, 1);
        IHistogram1D tarconVz = aida.histogram1D(plotDir + triggerType + "/" + "Target Constrained Vz (mm)", 50, -10, 10);
        IHistogram1D tarconChi2 = aida.histogram1D(plotDir + triggerType + "/" + "Target Constrained Chi2", 25, 0, 25);
        pEleVspPos = aida.histogram2D(plotDir + triggerType + "/" + "P(e) vs P(p)", 50, 0, beamEnergy * maxFactor, 50, 0, beamEnergy * maxFactor);
        pEleVspPosWithCut = aida.histogram2D(plotDir + triggerType + "/" + "P(e) vs P(p): Radiative", 50, 0, beamEnergy * maxFactor, 50, 0, beamEnergy * maxFactor);
        pyEleVspyPos = aida.histogram2D(plotDir + triggerType + "/" + "Py(e) vs Py(p)", 50, -0.04, 0.04, 50, -0.04, 0.04);
        pxEleVspxPos = aida.histogram2D(plotDir + triggerType + "/" + "Px(e) vs Px(p)", 50, -0.02, 0.06, 50, -0.02, 0.06);
        massVsVtxZ = aida.histogram2D(plotDir + triggerType + "/" + "Mass vs Vz", 50, 0, 0.15, 50, -50, 80);
        VtxXVsVtxZ = aida.histogram2D(plotDir + triggerType + "/" + "Vx vs Vz", 100, -10, 10, 100, -50, 80);
        VtxYVsVtxZ = aida.histogram2D(plotDir + triggerType + "/" + "Vy vs Vz", 100, -5, 5, 100, -50, 80);
        VtxXVsVtxY = aida.histogram2D(plotDir + triggerType + "/" + "Vx vs Vy", 100, -10, 10, 100, -5, 5);
        pEleVspEle = aida.histogram2D(plotDir + triggerType + "/" + "2 Electron: P(e) vs P(e)", 50, 0, beamEnergy * maxFactor, 50, 0, beamEnergy * maxFactor);
        pyEleVspyEle = aida.histogram2D(plotDir + triggerType + "/" + "2 Electron:Py(e) vs Py(e)", 50, -0.04, 0.04, 50, -0.04, 0.04);
        pxEleVspxEle = aida.histogram2D(plotDir + triggerType + "/" + "2 Electron:Px(e) vs Px(e)", 50, -0.02, 0.06, 50, -0.02, 0.06);
           pEleVspEleNoBeam = aida.histogram2D(plotDir + triggerType + "/" + "2 Electron: P(e) vs P(e) NoBeam", 50, 0, beamEnergy * maxFactor, 50, 0, beamEnergy * maxFactor);
        pyEleVspyEleNoBeam = aida.histogram2D(plotDir + triggerType + "/" + "2 Electron:Py(e) vs Py(e) NoBeam", 50, -0.04, 0.04, 50, -0.04, 0.04);
        pxEleVspxEleNoBeam = aida.histogram2D(plotDir + triggerType + "/" + "2 Electron:Px(e) vs Px(e) NoBeam", 50, -0.02, 0.06, 50, -0.02, 0.06);
        sumChargeHisto = aida.histogram1D(plotDir + triggerType + "/" + "Total Charge of  Event", 5, -2, 3);
        numChargeHisto = aida.histogram1D(plotDir + triggerType + "/" + "Number of Charged Particles", 6, 0, 6);
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

          //check to see if this event is from the correct trigger (or "all");
        if (!matchTrigger(event))
            return;
        
        nRecoEvents++;

        List<ReconstructedParticle> unonstrainedV0List = event.get(ReconstructedParticle.class, unconstrainedV0CandidatesColName);
        for (ReconstructedParticle uncV0 : unonstrainedV0List) {
            Vertex uncVert = uncV0.getStartVertex();
            aida.histogram1D(plotDir + triggerType + "/" + "Unconstrained Vx (mm)").fill(uncVert.getPosition().x());
            aida.histogram1D(plotDir + triggerType + "/" + "Unconstrained Vy (mm)").fill(uncVert.getPosition().y());
            aida.histogram1D(plotDir + triggerType + "/" + "Unconstrained Vz (mm)").fill(uncVert.getPosition().z());
            aida.histogram1D(plotDir + triggerType + "/" + "Unconstrained Mass (GeV)").fill(uncV0.getMass());
            aida.histogram1D(plotDir + triggerType + "/" + "Unconstrained Chi2").fill(uncVert.getChi2());

            aida.histogram2D(plotDir + triggerType + "/" + "Mass vs Vz").fill(uncV0.getMass(), uncVert.getPosition().z());
            VtxXVsVtxZ.fill(uncVert.getPosition().x(), uncVert.getPosition().z());
            VtxYVsVtxZ.fill(uncVert.getPosition().y(), uncVert.getPosition().z());
            VtxXVsVtxY.fill(uncVert.getPosition().x(), uncVert.getPosition().y());

            //this always has 2 tracks. 
            List<ReconstructedParticle> trks = uncV0.getParticles();
//            Track ele = trks.get(0).getTracks().get(0);
//            Track pos = trks.get(1).getTracks().get(0);
//            //if track #0 has charge>0 it's the electron!  This seems mixed up, but remember the track 
//            //charge is assigned assuming a positive B-field, while ours is negative
//            if (trks.get(0).getCharge() > 0) {
//                pos = trks.get(0).getTracks().get(0);
//                ele = trks.get(1).getTracks().get(0);
//            }
//            aida.histogram2D(plotDir + triggerType + "/" + "P(e) vs P(p)").fill(getMomentum(ele), getMomentum(pos));
//            aida.histogram2D(plotDir + triggerType + "/" + "Px(e) vs Px(p)").fill(ele.getTrackStates().get(0).getMomentum()[1], pos.getTrackStates().get(0).getMomentum()[1]);
//            aida.histogram2D(plotDir + triggerType + "/" + "Py(e) vs Py(p)").fill(ele.getTrackStates().get(0).getMomentum()[2], pos.getTrackStates().get(0).getMomentum()[2]);
            ReconstructedParticle ele = trks.get(0);
            ReconstructedParticle pos = trks.get(1);
            //ReconParticles have the charge correct. 
            if (trks.get(0).getCharge() > 0) {
                pos = trks.get(0);
                ele = trks.get(1);
            }
            double pe = ele.getMomentum().magnitude();
            double pp = pos.getMomentum().magnitude();
            aida.histogram2D(plotDir + triggerType + "/" + "P(e) vs P(p)").fill(pe, pp);
            aida.histogram2D(plotDir + triggerType + "/" + "Px(e) vs Px(p)").fill(ele.getMomentum().x(), pos.getMomentum().x());
            aida.histogram2D(plotDir + triggerType + "/" + "Py(e) vs Py(p)").fill(ele.getMomentum().y(), pos.getMomentum().y());
            if (pe < v0MaxPCut && pp < v0MaxPCut && (pe + pp) > v0ESumMinCut&&(pe + pp)<v0ESumMaxCut)//enrich radiative-like events
                aida.histogram2D(plotDir + triggerType + "/" + "P(e) vs P(p): Radiative").fill(pe, pp);
        }

        List<ReconstructedParticle> beamConstrainedV0List = event.get(ReconstructedParticle.class, beamConV0CandidatesColName);
        aida.histogram1D(plotDir + triggerType + "/" + "Number of V0 per event").fill(beamConstrainedV0List.size());
        for (ReconstructedParticle bsV0 : beamConstrainedV0List) {
            nTotV0++;
            Vertex bsVert = bsV0.getStartVertex();
            aida.histogram1D(plotDir + triggerType + "/" + "BS Constrained Vx (mm)").fill(bsVert.getPosition().x());
            aida.histogram1D(plotDir + triggerType + "/" + "BS Constrained Vy (mm)").fill(bsVert.getPosition().y());
            aida.histogram1D(plotDir + triggerType + "/" + "BS Constrained Vz (mm)").fill(bsVert.getPosition().z());
            aida.histogram1D(plotDir + triggerType + "/" + "BS Constrained Mass (GeV)").fill(bsV0.getMass());
            aida.histogram1D(plotDir + triggerType + "/" + "BS Constrained Chi2").fill(bsVert.getChi2());
            sumMass += bsV0.getMass();
            sumVx += bsVert.getPosition().x();
            sumVy += bsVert.getPosition().y();
            sumVz += bsVert.getPosition().z();
            sumChi2 += bsVert.getChi2();
        }

        List<ReconstructedParticle> targetConstrainedV0List = event.get(ReconstructedParticle.class, targetV0ConCandidatesColName);
        for (ReconstructedParticle tarV0 : targetConstrainedV0List) {
            Vertex tarVert = tarV0.getStartVertex();
            aida.histogram1D(plotDir + triggerType + "/" + "Target Constrained Vx (mm)").fill(tarVert.getPosition().x());
            aida.histogram1D(plotDir + triggerType + "/" + "Target Constrained Vy (mm)").fill(tarVert.getPosition().y());
            aida.histogram1D(plotDir + triggerType + "/" + "Target Constrained Vz (mm)").fill(tarVert.getPosition().z());
            aida.histogram1D(plotDir + triggerType + "/" + "Target Constrained Mass (GeV)").fill(tarV0.getMass());
            aida.histogram1D(plotDir + triggerType + "/" + "Target Constrained Chi2").fill(tarVert.getChi2());
        }
        List<ReconstructedParticle> finalStateParticles = event.get(ReconstructedParticle.class, finalStateParticlesColName);
        if (debug)
            System.out.println("This events has " + finalStateParticles.size() + " final state particles");

        ReconstructedParticle ele1 = null;
        ReconstructedParticle ele2 = null;
        int sumCharge = 0;
        int numChargedParticles = 0;
        for (ReconstructedParticle fsPart : finalStateParticles) {
            if (debug)
                System.out.println("PDGID = " + fsPart.getParticleIDUsed() + "; charge = " + fsPart.getCharge() + "; pz = " + fsPart.getMomentum().x());
            double charge = fsPart.getCharge();
            sumCharge += charge;
            if (charge != 0) {
                numChargedParticles++;
                if (charge < 1)
                    if (ele1 == null)
                        ele1 = fsPart;
                    else
                        ele2 = fsPart;
            }
        }
        sumChargeHisto.fill(sumCharge);
        numChargeHisto.fill(numChargedParticles);

        if (ele1 != null && ele2 != null) {
            pEleVspEle.fill(ele1.getMomentum().magnitude(), ele2.getMomentum().magnitude());
            pyEleVspyEle.fill(ele1.getMomentum().y(), ele2.getMomentum().y());
            pxEleVspxEle.fill(ele1.getMomentum().x(), ele2.getMomentum().x());
            if(ele1.getMomentum().magnitude()<0.85&&ele2.getMomentum().magnitude()<0.85){
                 pEleVspEleNoBeam.fill(ele1.getMomentum().magnitude(), ele2.getMomentum().magnitude());
            pyEleVspyEleNoBeam.fill(ele1.getMomentum().y(), ele2.getMomentum().y());
            pxEleVspxEleNoBeam.fill(ele1.getMomentum().x(), ele2.getMomentum().x());
            }
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
        IHistogram1D bsconVx = aida.histogram1D(plotDir + triggerType + "/" + "BS Constrained Vx (mm)");
        IHistogram1D bsconVy = aida.histogram1D(plotDir + triggerType + "/" + "BS Constrained Vy (mm)");
        IHistogram1D bsconVz = aida.histogram1D(plotDir + triggerType + "/" + "BS Constrained Vz (mm)");
        double[] init = {50.0, 0.0, 0.2, 1.0, 0.0};
        IFitResult resVx = fitVertexPosition(bsconVx, fitter, init, "range=\"(-0.5,0.5)\"");
        double[] init2 = {50.0, 0.0, 0.04, 1.0, 0.0};
        IFitResult resVy = fitVertexPosition(bsconVy, fitter, init2, "range=\"(-0.2,0.2)\"");
        double[] init3 = {50.0, 0.0, 3.0, 1.0, 0.0};
        IFitResult resVz = fitVertexPosition(bsconVz, fitter, init3, "range=\"(-6,6)\"");

        if (resVx != null && resVy != null & resVz != null) {
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
            if (outputPlots)
                try {
                    plotter.writeToFile(outputPlotDir + "vertex.png");
                } catch (IOException ex) {
                    Logger.getLogger(V0Monitoring.class.getName()).log(Level.SEVERE, null, ex);
                }

//        monitoredQuantityMap.put(fpQuantNames[2], sumVx / nTotV0);
//        monitoredQuantityMap.put(fpQuantNames[3], sumVy / nTotV0);
//        monitoredQuantityMap.put(fpQuantNames[4], sumVz / nTotV0);
            monitoredQuantityMap.put(beamConV0CandidatesColName+" "+ triggerType+" " +fpQuantNames[2], parsVx[1]);
            monitoredQuantityMap.put(beamConV0CandidatesColName+" "+ triggerType+" " +fpQuantNames[3], parsVy[1]);
            monitoredQuantityMap.put(beamConV0CandidatesColName+" "+ triggerType+" " +fpQuantNames[4], parsVz[1]);
            monitoredQuantityMap.put(beamConV0CandidatesColName+" "+ triggerType+" " +fpQuantNames[5], parsVx[2]);
            monitoredQuantityMap.put(beamConV0CandidatesColName+" "+ triggerType+" " +fpQuantNames[6], parsVy[2]);
            monitoredQuantityMap.put(beamConV0CandidatesColName+" "+ triggerType+" " +fpQuantNames[7], parsVz[2]);
        }
        monitoredQuantityMap.put(beamConV0CandidatesColName+" "+ triggerType+" " +fpQuantNames[0], (double) nTotV0 / nRecoEvents);
        monitoredQuantityMap.put(beamConV0CandidatesColName+" "+ triggerType+" " +fpQuantNames[1], sumMass / nTotV0);
        monitoredQuantityMap.put(beamConV0CandidatesColName+" "+ triggerType+" " +fpQuantNames[8], sumChi2 / nTotV0);

    }

    @Override
    public void printDQMStrings() {
        for (int i = 0; i < 9; i++)//TODO:  do this in a smarter way...loop over the map
            System.out.println("ALTER TABLE dqm ADD " + fpQuantNames[i] + " double;");
    }

    IFitResult fitVertexPosition(IHistogram1D h1d, IFitter fitter, double[] init, String range
    ) {
        IFitResult ifr = null;
        try {
            ifr = fitter.fit(h1d, "g+p1", init, range);
        } catch (RuntimeException ex) {
            System.out.println(this.getClass().getSimpleName() + ":  caught exception in fitGaussian");
        }
        return ifr;
    }

    private double getMomentum(Track trk) {

        double px = trk.getTrackStates().get(0).getMomentum()[0];
        double py = trk.getTrackStates().get(0).getMomentum()[1];
        double pz = trk.getTrackStates().get(0).getMomentum()[2];
        return Math.sqrt(px * px + py * py + pz * pz);
    }

}
