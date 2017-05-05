package org.hps.analysis.dataquality;

import hep.aida.IAnalysisFactory;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.VecOp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
// import org.hps.UnusedImportCheckstyleViolation
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
import org.lcsim.recon.tracking.seedtracker.SeedTrack;

/**
 * DQM driver V0 particles (i.e. e+e- pars) plots things like number of vertex
 * position an mass.
 */
public class V0Monitoring extends DataQualityMonitor {

    private static Logger LOGGER = Logger.getLogger(V0Monitoring.class.getPackage().getName());

    private String finalStateParticlesColName = "FinalStateParticles";
    private String unconstrainedV0CandidatesColName = "UnconstrainedV0Candidates";
    private String beamConV0CandidatesColName = "BeamspotConstrainedV0Candidates";
    private String targetV0ConCandidatesColName = "TargetConstrainedV0Candidates";
    private String[] fpQuantNames = {"nV0_per_Event", "avg_BSCon_mass", "avg_BSCon_Vx", "avg_BSCon_Vy", "avg_BSCon_Vz", "sig_BSCon_Vx", "sig_BSCon_Vy", "sig_BSCon_Vz", "avg_BSCon_Chi2"};
    //some counters
    private int nRecoEvents = 0;
    private int nTotV0 = 0;
    private int nTot2Ele = 0;
    //some summers
    private double sumMass = 0.0;
    private double sumVx = 0.0;
    private double sumVy = 0.0;
    private double sumVz = 0.0;
    private double sumChi2 = 0.0;

    /*  V0 Quantities   */
    /*  Mass, vertex, chi^2 of fit */
    /*  unconstrained */
    private IHistogram1D unconMass;
    private IHistogram1D unconVx;
    private IHistogram1D unconVy;
    private IHistogram1D unconVz;
    private IHistogram1D unconChi2;
    private IHistogram2D unconVzVsChi2;
    private IHistogram2D unconChi2VsTrkChi2;
    /* beamspot constrained */

    private IHistogram1D nV0;

    private IHistogram1D v0Time;
    private IHistogram1D v0Dt;
    private IHistogram2D trigTimeV0Time;
    private IHistogram1D trigTime;

    private IHistogram1D bsconMass;
    private IHistogram1D bsconVx;
    private IHistogram1D bsconVy;
    private IHistogram1D bsconVz;
    private IHistogram1D bsconChi2;
    private IHistogram2D bsconVzVsChi2;
    private IHistogram2D bsconChi2VsTrkChi2;
    /* target constrained */
    private IHistogram1D tarconMass;
    private IHistogram1D tarconVx;
    private IHistogram1D tarconVy;
    private IHistogram1D tarconVz;
    private IHistogram1D tarconChi2;
    private IHistogram2D tarconVzVsChi2;
    private IHistogram2D tarconChi2VsTrkChi2;

    private IHistogram2D pEleVspPos;
    private IHistogram1D pEle;
    private IHistogram1D pPos;

    private IHistogram2D pEleVspPosWithCut;
    private IHistogram2D pyEleVspyPos;
    private IHistogram2D pxEleVspxPos;

    private IHistogram2D VtxZVsMass;
    private IHistogram2D VtxYVsVtxZ;
    private IHistogram2D VtxXVsVtxZ;
    private IHistogram2D VtxXVsVtxY;
    private IHistogram2D VtxXVsVtxPx;
    private IHistogram2D VtxYVsVtxPy;
    private IHistogram2D VtxZVsVtxPx;
    private IHistogram2D VtxZVsVtxPy;
    private IHistogram2D VtxZVsVtxPz;

    private IHistogram2D VtxZVsL1Iso;
    private IHistogram2D VtxZVsTrkChi2;

    private IHistogram2D pEleVspEle;
    private IHistogram2D phiEleVsphiEle;
    private IHistogram2D pyEleVspyEle;
    private IHistogram2D pxEleVspxEle;
    private IHistogram2D pEleVspEleNoBeam;
    private IHistogram2D pyEleVspyEleNoBeam;
    private IHistogram2D pxEleVspxEleNoBeam;
    private IHistogram2D pEleVspEleMoller;
    private IHistogram2D pEleVsthetaMoller;
    private IHistogram2D thetaEleVsthetaMoller;
    private IHistogram2D pEleVspEleBeamBeam;
    private IHistogram2D pEleVsthetaBeamBeam;
    private IHistogram2D thetaEleVsthetaBeamBeam;

    private IHistogram1D mollerMass;
    private IHistogram1D mollerMassVtxCut;
    private IHistogram1D mollerVx;
    private IHistogram1D mollerVy;
    private IHistogram1D mollerVz;
    private IHistogram1D mollerVzVtxCut;
    private IHistogram2D mollerXVsVtxZ;
    private IHistogram2D mollerYVsVtxZ;
    private IHistogram2D mollerXVsVtxY;

    private IHistogram1D mollerUx;
    private IHistogram1D mollerUy;



    private IHistogram1D sumChargeHisto;
    private IHistogram1D numChargeHisto;

    private final String plotDir = "V0Monitoring/";

    private final BasicHep3Matrix beamAxisRotation = new BasicHep3Matrix();

    private double maxFactor = 1.25;


    private double thetaMax = 0.06;
    private double thetaMin = 0.015;



    private double feeMomentumCut, v0ESumMinCut, v0MaxPCut, v0ESumMaxCut,
    molPSumMin, molPSumMax, beambeamCut;



    @Override
    protected void detectorChanged(Detector detector) {

        super.detectorChanged(detector);
        
        feeMomentumCut = 0.75*beamEnergy; //GeV

        v0ESumMinCut = 0.8 * beamEnergy;
        v0ESumMaxCut = 1.25 * beamEnergy;
        
        v0MaxPCut = 1.05*beamEnergy;//GeV
        molPSumMin = 0.80*beamEnergy;
        molPSumMax = 1.25*beamEnergy;
        beambeamCut = 0.80*beamEnergy;



        beamAxisRotation.setActiveEuler(Math.PI / 2, -0.0305, -Math.PI / 2);

        LOGGER.info("Setting up the plotter");
        aida.tree().cd("/");
        String xtra = "Extras";
        String trkType = "SeedTrack/";
        if (isGBL)
            trkType = "GBLTrack/";

        double maxMass = .2*beamEnergy;
        double maxMassMoller = .1*Math.sqrt(beamEnergy);
        /*  V0 Quantities   */
        /*  Mass, vertex, chi^2 of fit */
        /*  unconstrained */
        unconMass = aida.histogram1D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "Invariant Mass (GeV)", 100, 0, maxMass);
        unconVx = aida.histogram1D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "Vx (mm)", 50, -10, 10);
        unconVy = aida.histogram1D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "Vy (mm)", 50, -10, 10);
        unconVz = aida.histogram1D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "Vz (mm)", 50, -50, 50);
        unconChi2 = aida.histogram1D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "Chi2", 25, 0, 25);
        unconVzVsChi2 = aida.histogram2D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "Vz vs. Chi2", 25, 0, 25, 50, -50, 50);
        unconChi2VsTrkChi2 = aida.histogram2D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "Chi2 vs. total track chi2", 50, 0, 50, 50, 0, 25);
        /* beamspot constrained */
        bsconMass = aida.histogram1D(plotDir + trkType + triggerType + "/" + beamConV0CandidatesColName + "/" + "Mass (GeV)", 100, 0, maxMass);
        bsconVx = aida.histogram1D(plotDir + trkType + triggerType + "/" + beamConV0CandidatesColName + "/" + "Vx (mm)", 50, -10, 10);
        bsconVy = aida.histogram1D(plotDir + trkType + triggerType + "/" + beamConV0CandidatesColName + "/" + "Vy (mm)", 50, -10, 10);
        bsconVz = aida.histogram1D(plotDir + trkType + triggerType + "/" + beamConV0CandidatesColName + "/" + "Vz (mm)", 50, -50, 50);
        bsconChi2 = aida.histogram1D(plotDir + trkType + triggerType + "/" + beamConV0CandidatesColName + "/" + "Chi2", 25, 0, 25);
        bsconVzVsChi2 = aida.histogram2D(plotDir + trkType + triggerType + "/" + beamConV0CandidatesColName + "/" + "Vz vs. Chi2", 25, 0, 25, 50, -50, 50);
        bsconChi2VsTrkChi2 = aida.histogram2D(plotDir + trkType + triggerType + "/" + beamConV0CandidatesColName + "/" + "Chi2 vs. total track chi2", 50, 0, 50, 50, 0, 25);
        /* target constrained */
        tarconMass = aida.histogram1D(plotDir + trkType + triggerType + "/" + targetV0ConCandidatesColName + "/" + "Mass (GeV)", 100, 0, maxMass);
        tarconVx = aida.histogram1D(plotDir + trkType + triggerType + "/" + targetV0ConCandidatesColName + "/" + "Vx (mm)", 50, -1, 1);
        tarconVy = aida.histogram1D(plotDir + trkType + triggerType + "/" + targetV0ConCandidatesColName + "/" + "Vy (mm)", 50, -1, 1);
        tarconVz = aida.histogram1D(plotDir + trkType + triggerType + "/" + targetV0ConCandidatesColName + "/" + "Vz (mm)", 50, -10, 10);
        tarconChi2 = aida.histogram1D(plotDir + trkType + triggerType + "/" + targetV0ConCandidatesColName + "/" + "Chi2", 25, 0, 25);
        tarconVzVsChi2 = aida.histogram2D(plotDir + trkType + triggerType + "/" + targetV0ConCandidatesColName + "/" + "Vz vs. Chi2", 25, 0, 25, 50, -50, 50);
        tarconChi2VsTrkChi2 = aida.histogram2D(plotDir + trkType + triggerType + "/" + targetV0ConCandidatesColName + "/" + "Chi2 vs. total track chi2", 50, 0, 50, 50, 0, 25);

        nV0 = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "Number of V0 per event", 10, 0, 10);
        v0Time = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "V0 mean time", 100, -25, 25);
        v0Dt = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "V0 time difference", 100, -25, 25);
        trigTimeV0Time = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "Trigger phase vs. V0 mean time", 100, -25, 25, 6, 0, 24);
        trigTime = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "Trigger phase", 6, 0, 24);

        pEleVspPos = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "P(e) vs P(p)", 50, 0, beamEnergy * maxFactor, 50, 0, beamEnergy * maxFactor);


        pEle = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "P(e)", 50, 0, beamEnergy * maxFactor);
        pPos = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "P(p)", 50, 0, beamEnergy * maxFactor);

        pEleVspPosWithCut = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "P(e) vs P(p): Radiative", 50, 0, beamEnergy * maxFactor, 50, 0, beamEnergy * maxFactor);
        pyEleVspyPos = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "Py(e) vs Py(p)", 50, -0.04*beamEnergy, 0.04*beamEnergy, 50, -0.04*beamEnergy, 0.04*beamEnergy);
        pxEleVspxPos = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "Px(e) vs Px(p)", 50, -0.04*beamEnergy, 0.04*beamEnergy, 50, -0.04*beamEnergy, 0.04*beamEnergy);
        VtxZVsMass = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "Vz vs Mass", 50, 0, maxMass, 50, -50, 80);
        VtxXVsVtxZ = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "Vx vs Vz", 100, -10, 10, 100, -50, 80);
        VtxYVsVtxZ = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "Vy vs Vz", 100, -5, 5, 100, -50, 80);
        VtxXVsVtxY = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "Vx vs Vy", 100, -10, 10, 100, -5, 5);
        VtxXVsVtxPx = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "Vx vs Px", 100, -0.1, 0.1, 100, -10, 10);
        VtxYVsVtxPy = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "Vy vs Py", 100, -0.1, 0.1, 100, -5, 5);
        VtxZVsVtxPx = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "Vz vs Px", 100, -0.1, 0.1, 100, -50, 80);
        VtxZVsVtxPy = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "Vz vs Py", 100, -0.1, 0.1, 100, -50, 80);
        VtxZVsVtxPz = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "Vz vs Pz", 100, 0.0, beamEnergy * maxFactor, 100, -50, 80);
        VtxZVsL1Iso = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "Vz vs L1 Isolation", 100, 0.0, 5.0, 50, -50, 80);
        VtxZVsTrkChi2 = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "Vz vs Track Chi2", 50, 0, 50, 50, -50, 80);
        phiEleVsphiEle = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/phi(e) vs phi(e)", 50, -Math.PI, Math.PI, 50, -Math.PI, Math.PI);
        pyEleVspyEle = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Py(e) vs Py(e)", 50, -0.04*beamEnergy, 0.04*beamEnergy, 50, -0.04*beamEnergy, 0.04*beamEnergy);
        pxEleVspxEle = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Px(e) vs Px(e)", 50, -0.02*beamEnergy, 0.06*beamEnergy, 50, -0.02*beamEnergy, 0.06*beamEnergy);
        
        // electron vs electron momentum with different cuts
        // 1) no cut
        // 2) cut out FEE 
        // 3) cut out FEE and also cut on momentum sum
        // 4) cut out everything except FEE coincidentals
        pEleVspEle = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/P(e) vs P(e)", 50, 0, beamEnergy * maxFactor, 50, 0, beamEnergy * maxFactor);
        pEleVspEleNoBeam = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/P(e) vs P(e) NoBeam", 50, 0, beambeamCut, 50, 0, beambeamCut);
        pEleVspEleMoller = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/P(e) vs P(e) Moller", 50, 0, beambeamCut, 50, 0, beambeamCut);
        pEleVspEleBeamBeam = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/P(e) vs P(e) BeamBeam", 50, beambeamCut, beamEnergy * maxFactor, 50, beambeamCut, beamEnergy * maxFactor);
        
        pyEleVspyEleNoBeam = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Py(e) vs Py(e) NoBeam", 50, -0.04*beamEnergy, 0.04*beamEnergy, 50, -0.04*beamEnergy, 0.04*beamEnergy);
        pxEleVspxEleNoBeam = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Px(e) vs Px(e) NoBeam", 50, -0.02*beamEnergy, 0.06*beamEnergy, 50, -0.02*beamEnergy, 0.06*beamEnergy);
        sumChargeHisto = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "Total Charge of  Event", 5, -2, 3);
        numChargeHisto = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "Number of Charged Particles", 6, 0, 6);

        pEleVsthetaMoller = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/P(e) vs Theta Moller", 50, 0, beambeamCut, 50, thetaMin, thetaMax);
        thetaEleVsthetaMoller = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Theta vs Theta Moller", 50, thetaMin, thetaMax, 50, thetaMin, thetaMax);
        pEleVsthetaBeamBeam = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/P(e) vs Theta BeamBeam", 50, beambeamCut, beamEnergy * maxFactor, 50, thetaMin, thetaMax);
        thetaEleVsthetaBeamBeam = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Theta vs Theta BeamBeam", 50, thetaMin, thetaMax, 50, thetaMin, thetaMax);

        mollerMass = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Moller Mass (GeV)", 100, 0, maxMassMoller);
        mollerMassVtxCut = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Moller Mass (GeV): VtxCut", 100, 0, maxMassMoller);
        mollerVx = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Moller Vx (mm)", 50, -10, 10);
        mollerVy = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Moller Vy (mm)", 50, -2, 2);
        mollerVz = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Moller Vz (mm)", 50, -50, 50);
        mollerVzVtxCut = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Moller Vz (mm): VtxCut", 50, -50, 50);
        mollerXVsVtxZ = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Moller Vx vs Vz", 100, -5, 5, 100, -50, 50);
        mollerYVsVtxZ = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Moller Vy vs Vz", 100, -2, 2, 100, -50, 50);
        mollerXVsVtxY = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Moller Vx vs Vy", 100, -5, 5, 100, -2, 2);

        mollerUx = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Moller Pair Momentum Direction Ux", 100, .015, .045);
        mollerUy = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Moller Pair Momentum Direction Uy", 100, -.01, .01);

        mollerHiP = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/P(high)", 100, 0, beamEnergy*maxFactor);
        mollerLoP = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/P(low)", 100, 0, beamEnergy*maxFactor);

        mollerEitherP = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/P(either)", 100, 0, beamEnergy*maxFactor);
        mollerPsum = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Psum", 100, 0, beamEnergy*maxFactor);

    }

    private IHistogram1D mollerHiP, mollerLoP, mollerEitherP, mollerPsum;

    @Override
    public void process(EventHeader event) {
        /*  make sure everything is there */
        if (!event.hasCollection(ReconstructedParticle.class, finalStateParticlesColName))
            throw new IllegalArgumentException("Collection missing: " + finalStateParticlesColName);
        if (!event.hasCollection(ReconstructedParticle.class, unconstrainedV0CandidatesColName))
            throw new IllegalArgumentException("Collection missing: " + unconstrainedV0CandidatesColName);
        if (!event.hasCollection(ReconstructedParticle.class, beamConV0CandidatesColName))
            throw new IllegalArgumentException("Collection missing: " + beamConV0CandidatesColName);
        if (!event.hasCollection(ReconstructedParticle.class, targetV0ConCandidatesColName))
            throw new IllegalArgumentException("Collection missing: " + targetV0ConCandidatesColName);

        //check to see if this event is from the correct trigger (or "all");
        if (!matchTrigger(event))
            return;

        nRecoEvents++;

        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);

        List<ReconstructedParticle> unonstrainedV0List = event.get(ReconstructedParticle.class, unconstrainedV0CandidatesColName);
        for (ReconstructedParticle uncV0 : unonstrainedV0List) {
            if (isGBL != TrackType.isGBL(uncV0.getType()))
                continue;
            Vertex uncVert = uncV0.getStartVertex();
            Hep3Vector pVtxRot = VecOp.mult(beamAxisRotation, uncV0.getMomentum());
            Hep3Vector vtxPosRot = VecOp.mult(beamAxisRotation, uncVert.getPosition());
            double theta = Math.acos(pVtxRot.z() / pVtxRot.magnitude());
            double phi = Math.atan2(pVtxRot.y(), pVtxRot.x());
            unconVx.fill(vtxPosRot.x());
            unconVy.fill(vtxPosRot.y());
            unconVz.fill(vtxPosRot.z());
            unconMass.fill(uncV0.getMass());
            unconChi2.fill(uncVert.getChi2());
            unconVzVsChi2.fill(uncVert.getChi2(), vtxPosRot.z());
            unconChi2VsTrkChi2.fill(Math.max(uncV0.getParticles().get(0).getTracks().get(0).getChi2(), uncV0.getParticles().get(1).getTracks().get(0).getChi2()), uncVert.getChi2());

            VtxZVsMass.fill(uncV0.getMass(), vtxPosRot.z());
            VtxXVsVtxZ.fill(vtxPosRot.x(), vtxPosRot.z());
            VtxYVsVtxZ.fill(vtxPosRot.y(), vtxPosRot.z());
            VtxXVsVtxY.fill(vtxPosRot.x(), vtxPosRot.y());
            VtxXVsVtxPx.fill(pVtxRot.x(), vtxPosRot.x());
            VtxYVsVtxPy.fill(pVtxRot.y(), vtxPosRot.y());
            VtxZVsVtxPx.fill(pVtxRot.x(), vtxPosRot.z());
            VtxZVsVtxPy.fill(pVtxRot.y(), vtxPosRot.z());
            VtxZVsVtxPz.fill(pVtxRot.z(), vtxPosRot.z());

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
            //            aida.histogram2D(plotDir + trkType + triggerType + "/" + "P(e) vs P(p)").fill(getMomentum(ele), getMomentum(pos));
            //            aida.histogram2D(plotDir + trkType + triggerType + "/" + "Px(e) vs Px(p)").fill(ele.getTrackStates().get(0).getMomentum()[1], pos.getTrackStates().get(0).getMomentum()[1]);
            //            aida.histogram2D(plotDir + trkType + triggerType + "/" + "Py(e) vs Py(p)").fill(ele.getTrackStates().get(0).getMomentum()[2], pos.getTrackStates().get(0).getMomentum()[2]);
            ReconstructedParticle ele = trks.get(0);
            ReconstructedParticle pos = trks.get(1);
            //ReconParticles have the charge correct. 
            if (trks.get(0).getCharge() > 0) {
                pos = trks.get(0);
                ele = trks.get(1);
            }
            if (ele.getCharge() < 0 && pos.getCharge() > 0) {
                VtxZVsTrkChi2.fill(Math.max(uncV0.getParticles().get(0).getTracks().get(0).getChi2(), uncV0.getParticles().get(1).getTracks().get(0).getChi2()), uncVert.getPosition().z());

                Double[] eleIso = TrackUtils.getIsolations(ele.getTracks().get(0), hitToStrips, hitToRotated);
                Double[] posIso = TrackUtils.getIsolations(pos.getTracks().get(0), hitToStrips, hitToRotated);
                if (eleIso[0] != null && posIso[0] != null) {
                    double eleL1Iso = Math.min(Math.abs(eleIso[0]), Math.abs(eleIso[1]));
                    double posL1Iso = Math.min(Math.abs(posIso[0]), Math.abs(posIso[1]));
                    double minL1Iso = Math.min(eleL1Iso, posL1Iso);
                    VtxZVsL1Iso.fill(minL1Iso, uncVert.getPosition().z());
                }

                double pe = ele.getMomentum().magnitude();
                double pp = pos.getMomentum().magnitude();
                Hep3Vector pEleRot = VecOp.mult(beamAxisRotation, ele.getMomentum());
                Hep3Vector pPosRot = VecOp.mult(beamAxisRotation, pos.getMomentum());

                pEleVspPos.fill(pe, pp);
                pEle.fill(pe);
                pPos.fill(pp);


                pxEleVspxPos.fill(pEleRot.x(), pPosRot.x());
                pyEleVspyPos.fill(pEleRot.y(), pPosRot.y());
                if (pe < v0MaxPCut && pp < v0MaxPCut && (pe + pp) > v0ESumMinCut && (pe + pp) < v0ESumMaxCut)//enrich radiative-like events

                pEleVspPosWithCut.fill(pe, pp);
            }

            double eleT = TrackUtils.getTrackTime(ele.getTracks().get(0), hitToStrips, hitToRotated);
            double posT = TrackUtils.getTrackTime(pos.getTracks().get(0), hitToStrips, hitToRotated);
            double meanT = (eleT + posT) / 2.0;
            v0Time.fill(meanT);
            v0Dt.fill(eleT - posT);
            trigTimeV0Time.fill(meanT, event.getTimeStamp() % 24);
            trigTime.fill(event.getTimeStamp() % 24);
        }

        List<ReconstructedParticle> beamConstrainedV0List = event.get(ReconstructedParticle.class, beamConV0CandidatesColName);
        //nV0.fill(beamConstrainedV0List.size());
        int v0Count = 0;
        for (ReconstructedParticle bsV0 : beamConstrainedV0List) {

            if (isGBL != TrackType.isGBL(bsV0.getType()))
                continue;
            v0Count++;
            nTotV0++;
            Vertex bsVert = bsV0.getStartVertex();
            Hep3Vector vtxPosRot = VecOp.mult(beamAxisRotation, bsVert.getPosition());
            bsconVx.fill(vtxPosRot.x());
            bsconVy.fill(vtxPosRot.y());
            bsconVz.fill(vtxPosRot.z());
            bsconMass.fill(bsV0.getMass());
            bsconChi2.fill(bsVert.getChi2());
            bsconVzVsChi2.fill(bsVert.getChi2(), vtxPosRot.z());
            bsconChi2VsTrkChi2.fill(Math.max(bsV0.getParticles().get(0).getTracks().get(0).getChi2(), bsV0.getParticles().get(1).getTracks().get(0).getChi2()), bsVert.getChi2());
            sumMass += bsV0.getMass();
            sumVx += vtxPosRot.x();
            sumVy += vtxPosRot.y();
            sumVz += vtxPosRot.z();
            sumChi2 += bsVert.getChi2();
        }
        nV0.fill(v0Count);
        List<ReconstructedParticle> targetConstrainedV0List = event.get(ReconstructedParticle.class, targetV0ConCandidatesColName);
        for (ReconstructedParticle tarV0 : targetConstrainedV0List) {

            if (isGBL != TrackType.isGBL(tarV0.getType()))
                continue;

            Vertex tarVert = tarV0.getStartVertex();
            Hep3Vector vtxPosRot = VecOp.mult(beamAxisRotation, tarVert.getPosition());
            tarconVx.fill(vtxPosRot.x());
            tarconVy.fill(vtxPosRot.y());
            tarconVz.fill(vtxPosRot.z());
            tarconMass.fill(tarV0.getMass());
            tarconChi2.fill(tarVert.getChi2());
            tarconVzVsChi2.fill(tarVert.getChi2(), vtxPosRot.z());
            tarconChi2VsTrkChi2.fill(Math.max(tarV0.getParticles().get(0).getTracks().get(0).getChi2(), tarV0.getParticles().get(1).getTracks().get(0).getChi2()), tarVert.getChi2());
        }
        List<ReconstructedParticle> finalStateParticles = event.get(ReconstructedParticle.class, finalStateParticlesColName);
        if (debug)
            LOGGER.info("This events has " + finalStateParticles.size() + " final state particles");

        ReconstructedParticle ele1 = null;
        ReconstructedParticle ele2 = null;
        int sumCharge = 0;
        int numChargedParticles = 0;
        for (ReconstructedParticle fsPart : finalStateParticles) {
            if (isGBL != TrackType.isGBL(fsPart.getType()))
                continue;
            if (debug)
                LOGGER.info("PDGID = " + fsPart.getParticleIDUsed() + "; charge = " + fsPart.getCharge() + "; pz = " + fsPart.getMomentum().x());
            double charge = fsPart.getCharge();
            sumCharge += charge;
            if (charge != 0) {
                numChargedParticles++;
                if (charge < 1)
                    if (ele1 == null)
                        ele1 = fsPart;
                    else if (!hasSharedStrips(ele1, fsPart, hitToStrips, hitToRotated))
                        ele2 = fsPart;
            }
        }
        sumChargeHisto.fill(sumCharge);
        numChargeHisto.fill(numChargedParticles);

        if (ele1 != null && ele2 != null) {
            Hep3Vector p1 = VecOp.mult(beamAxisRotation, ele1.getMomentum());
            Hep3Vector p2 = VecOp.mult(beamAxisRotation, ele2.getMomentum());
            //            Hep3Vector beamAxis = new BasicHep3Vector(Math.sin(0.0305), 0, Math.cos(0.0305));
            //            LOGGER.info(p1);
            //            LOGGER.info(VecOp.mult(rot, p1));

            double theta1 = Math.acos(p1.z() / p1.magnitude());
            double theta2 = Math.acos(p2.z() / p2.magnitude());
            double phi1 = Math.atan2(p1.y(), p1.x());
            double phi2 = Math.atan2(p2.y(), p2.x());
            phiEleVsphiEle.fill(phi1, phi2);
            pEleVspEle.fill(ele1.getMomentum().magnitude(), ele2.getMomentum().magnitude());
            pyEleVspyEle.fill(ele1.getMomentum().y(), ele2.getMomentum().y());
            pxEleVspxEle.fill(ele1.getMomentum().x(), ele2.getMomentum().x());
            //remove beam electrons
            if (ele1.getMomentum().magnitude() < beambeamCut && ele2.getMomentum().magnitude() < beambeamCut) {
                pEleVspEleNoBeam.fill(ele1.getMomentum().magnitude(), ele2.getMomentum().magnitude());
                pyEleVspyEleNoBeam.fill(ele1.getMomentum().y(), ele2.getMomentum().y());
                pxEleVspxEleNoBeam.fill(ele1.getMomentum().x(), ele2.getMomentum().x());
            }
            //look at beam-beam events
            if (ele1.getMomentum().magnitude() > beambeamCut && ele2.getMomentum().magnitude() > beambeamCut) {
                pEleVspEleBeamBeam.fill(ele1.getMomentum().magnitude(), ele2.getMomentum().magnitude());
                pEleVsthetaBeamBeam.fill(p1.magnitude(), theta1);
                pEleVsthetaBeamBeam.fill(p2.magnitude(), theta2);
                thetaEleVsthetaBeamBeam.fill(theta1, theta2);
            }

            //look at "Moller" events (if that's what they really are
            if (ele1.getMomentum().magnitude() + ele2.getMomentum().magnitude() > molPSumMin
                    && ele1.getMomentum().magnitude() + ele2.getMomentum().magnitude() < molPSumMax
                    && (p1.magnitude() < beambeamCut && p2.magnitude() < beambeamCut)) {

                Track ele1trk = ele1.getTracks().get(0);
                Track ele2trk = ele2.getTracks().get(0);
                SeedTrack stEle1 = TrackUtils.makeSeedTrackFromBaseTrack(ele1trk);
                SeedTrack stEle2 = TrackUtils.makeSeedTrackFromBaseTrack(ele2trk);
                BilliorTrack btEle1 = new BilliorTrack(stEle1.getSeedCandidate().getHelix());
                BilliorTrack btEle2 = new BilliorTrack(stEle2.getSeedCandidate().getHelix());
                BilliorVertex bv = fitVertex(btEle1, btEle2, TrackUtils.getBField(event.getDetector()).magnitude());
                //                LOGGER.info("ee vertex: "+bv.toString());
                double invMass = bv.getParameters().get("invMass");
                mollerMass.fill(invMass);
                mollerVx.fill(bv.getPosition().x());
                mollerVy.fill(bv.getPosition().y());
                mollerVz.fill(bv.getPosition().z());
                mollerXVsVtxZ.fill(bv.getPosition().x(), bv.getPosition().z());
                mollerYVsVtxZ.fill(bv.getPosition().y(), bv.getPosition().z());
                mollerXVsVtxY.fill(bv.getPosition().x(), bv.getPosition().y());

                double ux = (ele1.getMomentum().x()+ele2.getMomentum().x())/(ele1.getMomentum().z()+ele2.getMomentum().z());
                double uy = (ele1.getMomentum().y()+ele2.getMomentum().y())/(ele1.getMomentum().z()+ele2.getMomentum().z());
                mollerUx.fill(ux);
                mollerUy.fill(uy);

                //higher and lower energy electrons in moller pair
                double pt1 = ele1.getMomentum().magnitude();
                double pt2 = ele2.getMomentum().magnitude();
                double ph = (pt1>pt2) ? pt1 : pt2;
                double pl = (pt1>pt2) ? pt2 : pt1;

                mollerHiP.fill(ph);
                mollerLoP.fill(pl);

                mollerEitherP.fill(ph);
                mollerEitherP.fill(pl);
                mollerPsum.fill(pt1+pt2);


                if (Math.abs(bv.getPosition().x()) < 2
                        && Math.abs(bv.getPosition().y()) < 0.5) {
                    mollerMassVtxCut.fill(invMass);
                    mollerVzVtxCut.fill(bv.getPosition().z());
                }
                pEleVspEleMoller.fill(p1.magnitude(), p2.magnitude());
                pEleVsthetaMoller.fill(p1.magnitude(), theta1);
                pEleVsthetaMoller.fill(p2.magnitude(), theta2);
                thetaEleVsthetaMoller.fill(theta1, theta2);
            }
        }
    }

    @Override
    public void printDQMData() {
        LOGGER.info("V0Monitoring::printDQMData");
        for (Entry<String, Double> entry : monitoredQuantityMap.entrySet())
            LOGGER.info(entry.getKey() + " = " + entry.getValue());
        LOGGER.info("*******************************");
    }

    /**
     * Calculate the averages here and fill the map
     */
    @Override
    public void calculateEndOfRunQuantities() {

        IAnalysisFactory analysisFactory = IAnalysisFactory.create();
        IFitFactory fitFactory = analysisFactory.createFitFactory();
        IFitter fitter = fitFactory.createFitter("chi2");
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
                LOGGER.info("Vertex Fit Parameters:  " + resVx.fittedParameterNames()[i] + " = " + parsVx[i] + "; " + parsVy[i] + "; " + parsVz[i]);

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
                monitoredQuantityMap.put(beamConV0CandidatesColName + " " + triggerType + " " + fpQuantNames[2], parsVx[1]);
                monitoredQuantityMap.put(beamConV0CandidatesColName + " " + triggerType + " " + fpQuantNames[3], parsVy[1]);
                monitoredQuantityMap.put(beamConV0CandidatesColName + " " + triggerType + " " + fpQuantNames[4], parsVz[1]);
                monitoredQuantityMap.put(beamConV0CandidatesColName + " " + triggerType + " " + fpQuantNames[5], parsVx[2]);
                monitoredQuantityMap.put(beamConV0CandidatesColName + " " + triggerType + " " + fpQuantNames[6], parsVy[2]);
                monitoredQuantityMap.put(beamConV0CandidatesColName + " " + triggerType + " " + fpQuantNames[7], parsVz[2]);
        }
        monitoredQuantityMap.put(beamConV0CandidatesColName + " " + triggerType + " " + fpQuantNames[0], (double) nTotV0 / nRecoEvents);
        monitoredQuantityMap.put(beamConV0CandidatesColName + " " + triggerType + " " + fpQuantNames[1], sumMass / nTotV0);
        monitoredQuantityMap.put(beamConV0CandidatesColName + " " + triggerType + " " + fpQuantNames[8], sumChi2 / nTotV0);

    }

    @Override
    public void printDQMStrings() {
        for (int i = 0; i < 9; i++)//TODO:  do this in a smarter way...loop over the map
            LOGGER.info("ALTER TABLE dqm ADD " + fpQuantNames[i] + " double;");
    }

    private IFitResult fitVertexPosition(IHistogram1D h1d, IFitter fitter, double[] init, String range
    ) {
        IFitResult ifr = null;
        try {
            ifr = fitter.fit(h1d, "g+p1", init, range);
        } catch (RuntimeException ex) {
            LOGGER.info(this.getClass().getSimpleName() + ":  caught exception in fitGaussian");
        }
        return ifr;
    }

    private BilliorVertex fitVertex(BilliorTrack electron, BilliorTrack positron, double bField) {
        // Create a vertex fitter from the magnetic field.
        double[] beamSize = {0.001, 0.2, 0.02};
        BilliorVertexer vtxFitter = new BilliorVertexer(bField);
        // TODO: The beam size should come from the conditions database.
        vtxFitter.setBeamSize(beamSize);

        // Perform the vertexing based on the specified constraint.
        vtxFitter.doBeamSpotConstraint(false);

        // Add the electron and positron tracks to a track list for
        // the vertex fitter.
        List<BilliorTrack> billiorTracks = new ArrayList<BilliorTrack>();

        billiorTracks.add(electron);

        billiorTracks.add(positron);

        // Find and return a vertex based on the tracks.
        return vtxFitter.fitVertex(billiorTracks);
    }

    private static boolean hasSharedStrips(ReconstructedParticle vertex, RelationalTable hittostrip, RelationalTable hittorotated) {
        return hasSharedStrips(vertex.getParticles().get(0), vertex.getParticles().get(1), hittostrip, hittorotated);
    }

    private static boolean hasSharedStrips(ReconstructedParticle fs1, ReconstructedParticle fs2, RelationalTable hittostrip, RelationalTable hittorotated) {
        return TrackUtils.hasSharedStrips(fs1.getTracks().get(0), fs2.getTracks().get(0), hittostrip, hittorotated);
    }
}
