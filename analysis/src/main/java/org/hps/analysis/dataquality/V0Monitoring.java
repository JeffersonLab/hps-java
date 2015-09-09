package org.hps.analysis.dataquality;

import hep.aida.IAnalysisFactory;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
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
 * position an mass
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

    /*  V0 Quantities   */
    /*  Mass, vertex, chi^2 of fit */
    /*  unconstrained */
    IHistogram1D unconMass;
    IHistogram1D unconVx;
    IHistogram1D unconVy;
    IHistogram1D unconVz;
    IHistogram1D unconChi2;
    /* beamspot constrained */

    IHistogram1D nV0;

    IHistogram1D v0Time;
    IHistogram1D v0Dt;
    IHistogram2D trigTimeV0Time;
    IHistogram1D trigTime;

    IHistogram1D bsconMass;
    IHistogram1D bsconVx;
    IHistogram1D bsconVy;
    IHistogram1D bsconVz;
    IHistogram1D bsconChi2;
    /* target constrained */
    IHistogram1D tarconMass;
    IHistogram1D tarconVx;
    IHistogram1D tarconVy;
    IHistogram1D tarconVz;
    IHistogram1D tarconChi2;

    IHistogram2D pEleVspPos;
    IHistogram2D pEleVspPosWithCut;
    IHistogram2D pyEleVspyPos;
    IHistogram2D pxEleVspxPos;

    IHistogram2D massVsVtxZ;
    IHistogram2D VtxYVsVtxZ;
    IHistogram2D VtxXVsVtxZ;
    IHistogram2D VtxXVsVtxY;
    IHistogram2D L1IsoVsVz;

    IHistogram2D pEleVspEle;
    IHistogram2D pyEleVspyEle;
    IHistogram2D pxEleVspxEle;
    IHistogram2D pEleVspEleNoBeam;
    IHistogram2D pyEleVspyEleNoBeam;
    IHistogram2D pxEleVspxEleNoBeam;
    IHistogram2D pEleVspEleMoller;
    IHistogram2D pEleVsthetaMoller;
    IHistogram2D thetaEleVsthetaMoller;
    IHistogram2D pEleVspEleBeamBeam;
    IHistogram2D pEleVsthetaBeamBeam;
    IHistogram2D thetaEleVsthetaBeamBeam;

    IHistogram1D mollerMass;
    IHistogram1D mollerMassVtxCut;
    IHistogram1D mollerVx;
    IHistogram1D mollerVy;
    IHistogram1D mollerVz;
    IHistogram1D mollerVzVtxCut;
    IHistogram2D mollerXVsVtxZ;
    IHistogram2D mollerYVsVtxZ;
    IHistogram2D mollerXVsVtxY;

    IHistogram1D sumChargeHisto;
    IHistogram1D numChargeHisto;

    private final String plotDir = "V0Monitoring/";

    double beamEnergy = 1.05; //GeV
    double maxFactor = 1.25;
    double feeMomentumCut = 0.8; //GeV

    double v0ESumMinCut = 0.8 * beamEnergy;
    double v0ESumMaxCut = 1.25 * beamEnergy;
    double v0MaxPCut = 1.1;//GeV
    double molPSumMin = 0.85;
    double molPSumMax = 1.3;
    double beambeamCut = 0.85;
    double thetaMax = 0.06;
    double thetaMin = 0.015;

    @Override
    protected void detectorChanged(Detector detector) {
        System.out.println("V0Monitoring::detectorChanged  Setting up the plotter");
        aida.tree().cd("/");
        String xtra = "Extras";
        String trkType = "SeedTrack/";
        if (isGBL)
            trkType = "GBLTrack/";
        /*  V0 Quantities   */
        /*  Mass, vertex, chi^2 of fit */
        /*  unconstrained */
        unconMass = aida.histogram1D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "Invariant Mass (GeV)", 100, 0, 0.200);
        unconVx = aida.histogram1D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "Vx (mm)", 50, -10, 10);
        unconVy = aida.histogram1D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "Vy (mm)", 50, -10, 10);
        unconVz = aida.histogram1D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "Vz (mm)", 50, -50, 50);
        unconChi2 = aida.histogram1D(plotDir + trkType + triggerType + "/" + unconstrainedV0CandidatesColName + "/" + "Chi2", 25, 0, 25);
        /* beamspot constrained */
        bsconMass = aida.histogram1D(plotDir + trkType + triggerType + "/" + beamConV0CandidatesColName + "/" + "Mass (GeV)", 100, 0, 0.200);
        bsconVx = aida.histogram1D(plotDir + trkType + triggerType + "/" + beamConV0CandidatesColName + "/"  + "Vx (mm)", 50, -10, 10);
        bsconVy = aida.histogram1D(plotDir + trkType + triggerType + "/" + beamConV0CandidatesColName + "/" + "Vy (mm)", 50, -10, 10);
        bsconVz = aida.histogram1D(plotDir + trkType + triggerType + "/" + beamConV0CandidatesColName + "/" + "Vz (mm)", 50, -50, 50);
        bsconChi2 = aida.histogram1D(plotDir + trkType + triggerType + "/" + beamConV0CandidatesColName + "/" + "Chi2", 25, 0, 25);
        /* target constrained */
        tarconMass = aida.histogram1D(plotDir + trkType + triggerType + "/" + targetV0ConCandidatesColName + "/" + "Mass (GeV)", 100, 0, 0.200);
        tarconVx = aida.histogram1D(plotDir + trkType + triggerType + "/" + targetV0ConCandidatesColName + "/" + "Vx (mm)", 50, -1, 1);
        tarconVy = aida.histogram1D(plotDir + trkType + triggerType + "/" + targetV0ConCandidatesColName + "/" + "Vy (mm)", 50, -1, 1);
        tarconVz = aida.histogram1D(plotDir + trkType + triggerType + "/" + targetV0ConCandidatesColName + "/" + "Vz (mm)", 50, -10, 10);
        tarconChi2 = aida.histogram1D(plotDir + trkType + triggerType + "/" + targetV0ConCandidatesColName + "/" + "Chi2", 25, 0, 25);

        nV0 = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "Number of V0 per event", 10, 0, 10);
        v0Time = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "V0 mean time", 100, -25, 25);
        v0Dt = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "V0 time difference", 100, -25, 25);
        trigTimeV0Time = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra  + "/" + "Trigger phase vs. V0 mean time", 100, -25, 25, 6, 0, 24);
        trigTime = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "Trigger phase", 6, 0, 24);

        pEleVspPos = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "P(e) vs P(p)", 50, 0, beamEnergy * maxFactor, 50, 0, beamEnergy * maxFactor);
        pEleVspPosWithCut = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "P(e) vs P(p): Radiative", 50, 0, beamEnergy * maxFactor, 50, 0, beamEnergy * maxFactor);
        pyEleVspyPos = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "Py(e) vs Py(p)", 50, -0.04, 0.04, 50, -0.04, 0.04);
        pxEleVspxPos = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "Px(e) vs Px(p)", 50, -0.02, 0.06, 50, -0.02, 0.06);
        massVsVtxZ = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "Mass vs Vz", 50, 0, 0.15, 50, -50, 80);
        VtxXVsVtxZ = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "Vx vs Vz", 100, -10, 10, 100, -50, 80);
        VtxYVsVtxZ = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "Vy vs Vz", 100, -5, 5, 100, -50, 80);
        VtxXVsVtxY = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "Vx vs Vy", 100, -10, 10, 100, -5, 5);
        L1IsoVsVz = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "L1 Isolation vs Vz", 50, -50, 80, 100, 0.0, 5.0);
        pEleVspEle = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/P(e) vs P(e)", 50, 0, beamEnergy * maxFactor, 50, 0, beamEnergy * maxFactor);
        pyEleVspyEle = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Py(e) vs Py(e)", 50, -0.04, 0.04, 50, -0.04, 0.04);
        pxEleVspxEle = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Px(e) vs Px(e)", 50, -0.02, 0.06, 50, -0.02, 0.06);
        pEleVspEleNoBeam = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/P(e) vs P(e) NoBeam", 50, 0, beambeamCut, 50, 0, beambeamCut);
        pEleVspEleMoller = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/P(e) vs P(e) Moller", 50, 0, beambeamCut, 50, 0, beambeamCut);
        pEleVspEleBeamBeam = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/P(e) vs P(e) BeamBeam", 50, beambeamCut, beamEnergy * maxFactor, 50, beambeamCut, beamEnergy * maxFactor);
        pyEleVspyEleNoBeam = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Py(e) vs Py(e) NoBeam", 50, -0.04, 0.04, 50, -0.04, 0.04);
        pxEleVspxEleNoBeam = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Px(e) vs Px(e) NoBeam", 50, -0.02, 0.06, 50, -0.02, 0.06);
        sumChargeHisto = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "Total Charge of  Event", 5, -2, 3);
        numChargeHisto = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "Number of Charged Particles", 6, 0, 6);

        pEleVsthetaMoller = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/P(e) vs Theta Moller", 50, 0, beambeamCut, 50, thetaMin, thetaMax);
        thetaEleVsthetaMoller = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Theta vs Theta Moller", 50, thetaMin, thetaMax, 50, thetaMin, thetaMax);
        pEleVsthetaBeamBeam = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/P(e) vs Theta BeamBeam", 50, beambeamCut, beamEnergy * maxFactor, 50, thetaMin, thetaMax);
        thetaEleVsthetaBeamBeam = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Theta vs Theta BeamBeam", 50, thetaMin, thetaMax, 50, thetaMin, thetaMax);

        mollerMass = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Moller Mass (GeV)", 100, 0, 0.100);
        mollerMassVtxCut = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Moller Mass (GeV): VtxCut", 100, 0, 0.100);
        mollerVx = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Moller Vx (mm)", 50, -10, 10);
        mollerVy = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Moller Vy (mm)", 50, -2, 2);
        mollerVz = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Moller Vz (mm)", 50, -50, 50);
        mollerVzVtxCut = aida.histogram1D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Moller Vz (mm): VtxCut", 50, -50, 50);
        mollerXVsVtxZ = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Moller Vx vs Vz", 100, -5, 5, 100, -50, 50);
        mollerYVsVtxZ = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Moller Vy vs Vz", 100, -2, 2, 100, -50, 50);
        mollerXVsVtxY = aida.histogram2D(plotDir + trkType + triggerType + "/" + xtra + "/" + "2 Electron/Moller Vx vs Vy", 100, -5, 5, 100, -2, 2);
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

        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);

        List<ReconstructedParticle> unonstrainedV0List = event.get(ReconstructedParticle.class, unconstrainedV0CandidatesColName);
        for (ReconstructedParticle uncV0 : unonstrainedV0List) {

            Vertex uncVert = uncV0.getStartVertex();
            unconVx.fill(uncVert.getPosition().x());
            unconVy.fill(uncVert.getPosition().y());
            unconVz.fill(uncVert.getPosition().z());
            unconMass.fill(uncV0.getMass());
            unconChi2.fill(uncVert.getChi2());

            massVsVtxZ.fill(uncV0.getMass(), uncVert.getPosition().z());
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
            if (trks.get(0).getCharge() < 0 && trks.get(1).getCharge() > 0) {

                Double[] eleIso = TrackUtils.getIsolations(ele.getTracks().get(0), hitToStrips, hitToRotated);
                Double[] posIso = TrackUtils.getIsolations(pos.getTracks().get(0), hitToStrips, hitToRotated);
                if (eleIso[0] != null && posIso[0] != null) {
                    double eleL1Iso = Math.min(eleIso[0], eleIso[1]);
                    double posL1Iso = Math.min(posIso[0], posIso[1]);
                    double minL1Iso = Math.min(eleL1Iso, posL1Iso);
                    L1IsoVsVz.fill(uncVert.getPosition().z(), minL1Iso);
                }

                double pe = ele.getMomentum().magnitude();
                double pp = pos.getMomentum().magnitude();
                pEleVspPos.fill(pe, pp);
                pxEleVspxPos.fill(ele.getMomentum().x(), pos.getMomentum().x());
                pyEleVspyPos.fill(ele.getMomentum().y(), pos.getMomentum().y());
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
        nV0.fill(beamConstrainedV0List.size());
        for (ReconstructedParticle bsV0 : beamConstrainedV0List) {
            nTotV0++;
            Vertex bsVert = bsV0.getStartVertex();
            bsconVx.fill(bsVert.getPosition().x());
            bsconVy.fill(bsVert.getPosition().y());
            bsconVz.fill(bsVert.getPosition().z());
            bsconMass.fill(bsV0.getMass());
            bsconChi2.fill(bsVert.getChi2());
            sumMass += bsV0.getMass();
            sumVx += bsVert.getPosition().x();
            sumVy += bsVert.getPosition().y();
            sumVz += bsVert.getPosition().z();
            sumChi2 += bsVert.getChi2();
        }

        List<ReconstructedParticle> targetConstrainedV0List = event.get(ReconstructedParticle.class, targetV0ConCandidatesColName);
        for (ReconstructedParticle tarV0 : targetConstrainedV0List) {
            Vertex tarVert = tarV0.getStartVertex();
            tarconVx.fill(tarVert.getPosition().x());
            tarconVy.fill(tarVert.getPosition().y());
            tarconVz.fill(tarVert.getPosition().z());
            tarconMass.fill(tarV0.getMass());
            tarconChi2.fill(tarVert.getChi2());
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
                    else if (!hasSharedStrips(ele1, fsPart, hitToStrips, hitToRotated))
                        ele2 = fsPart;
            }
        }
        sumChargeHisto.fill(sumCharge);
        numChargeHisto.fill(numChargedParticles);

        if (ele1 != null && ele2 != null) {
            Hep3Vector p1 = ele1.getMomentum();
            Hep3Vector p2 = ele2.getMomentum();
            Hep3Vector beamAxis = new BasicHep3Vector(Math.sin(0.0305), 0, Math.cos(0.0305));
            double theta1 = Math.acos(VecOp.dot(p1, beamAxis) / p1.magnitude());
            double theta2 = Math.acos(VecOp.dot(p2, beamAxis) / p2.magnitude());
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
                BilliorVertex bv = fitVertex(btEle1, btEle2);
//                System.out.println("ee vertex: "+bv.toString());
                mollerMass.fill(bv.getParameters().get("invMass"));
                mollerVx.fill(bv.getPosition().x());
                mollerVy.fill(bv.getPosition().y());
                mollerVz.fill(bv.getPosition().z());
                mollerXVsVtxZ.fill(bv.getPosition().x(), bv.getPosition().z());
                mollerYVsVtxZ.fill(bv.getPosition().y(), bv.getPosition().z());
                mollerXVsVtxY.fill(bv.getPosition().x(), bv.getPosition().y());
                if (Math.abs(bv.getPosition().x()) < 2
                        && Math.abs(bv.getPosition().y()) < 0.5) {
                    mollerMassVtxCut.fill(bv.getParameters().get("invMass"));
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

    private BilliorVertex fitVertex(BilliorTrack electron, BilliorTrack positron) {
        // Create a vertex fitter from the magnetic field.
        double bField = 0.24;
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
