
/**
 *
 */

package org.hps.users.mrsolt;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.ITree;
import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.vertexing.BilliorTrack;
import org.hps.recon.vertexing.BilliorVertex;
import org.hps.recon.vertexing.BilliorVertexer;
import org.lcsim.detector.converter.compact.subdetector.SvtStereoLayer;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.Vertex;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.fit.helicaltrack.HitIdentifier;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class Layer0ReconParticle extends Driver {


    // Use JFreeChart as the default plotting backend
    static { 
        hep.aida.jfree.AnalysisFactory.register();
    }

    // Plotting
    protected AIDA aida = AIDA.defaultInstance();
    ITree tree; 
    IHistogramFactory histogramFactory; 
    IPlotterFactory plotterFactory = IAnalysisFactory.create().createPlotterFactory();
    protected Map<String, IPlotter> plotters = new HashMap<String, IPlotter>(); 
    private Map<Integer, List<SvtStereoLayer>> topStereoLayers = new HashMap<Integer, List<SvtStereoLayer>>();
    private Map<Integer, List<SvtStereoLayer>> bottomStereoLayers = new HashMap<Integer, List<SvtStereoLayer>>();
    private static final String SUBDETECTOR_NAME = "Tracker";
    RelationalTable<SimTrackerHit, MCParticle> _hittomc = null;
    private List<HpsSiSensor> sensors = null;
    private double _bfield;
    private HitIdentifier _ID;
    private int _nlayersTot = 14;
    double eBeam = 1.05;
    double maxFactor = 1.25;
    
    
    IHistogram1D unconMass;
    IHistogram1D unconVx;
    IHistogram1D unconVy;
    IHistogram1D unconVz;
    IHistogram1D unconChi2;
    IHistogram2D unconVzVsChi2;
    IHistogram2D unconChi2VsTrkChi2;
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
    IHistogram2D bsconVzVsChi2;
    IHistogram2D bsconChi2VsTrkChi2;
    /* target constrained */
    IHistogram1D tarconMass;
    IHistogram1D tarconVx;
    IHistogram1D tarconVy;
    IHistogram1D tarconVz;
    IHistogram1D tarconChi2;
    IHistogram2D tarconVzVsChi2;
    IHistogram2D tarconChi2VsTrkChi2;

    IHistogram2D pEleVspPos;
    IHistogram2D pEleVspPosWithCut;
    IHistogram2D pyEleVspyPos;
    IHistogram2D pxEleVspxPos;

    IHistogram2D VtxZVsMass;
    IHistogram2D VtxYVsVtxZ;
    IHistogram2D VtxXVsVtxZ;
    IHistogram2D VtxXVsVtxY;
    IHistogram2D VtxXVsVtxPx;
    IHistogram2D VtxYVsVtxPy;
    IHistogram2D VtxZVsVtxPx;
    IHistogram2D VtxZVsVtxPy;
    IHistogram2D VtxZVsVtxPz;

    IHistogram2D VtxZVsL1Iso;
    IHistogram2D VtxZVsTrkChi2;

    IHistogram2D pEleVspEle;
    IHistogram2D phiEleVsphiEle;
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
    
    double v0ESumMinCut = 0.8 * eBeam;
    double v0ESumMaxCut = 1.25 * eBeam;
    double v0MaxPCut = 1.1;//GeV
    double molPSumMin = 0.85;
    double molPSumMax = 1.3;
    double beambeamCut = 0.85;
    double thetaMax = 0.06;
    double thetaMin = 0.015;
    
    private String stereoHitCollectionName = "HelicalTrackHits";
    private String trackCollectionName = "MatchedTracks";
    private String MCparticleCollectionName = "MCParticle";
    String finalStateParticlesColName = "FinalStateParticles";
    String unconstrainedV0CandidatesColName = "UnconstrainedV0Candidates";
    String beamConV0CandidatesColName = "BeamspotConstrainedV0Candidates";
    String targetV0ConCandidatesColName = "TargetConstrainedV0Candidates";
    private final BasicHep3Matrix beamAxisRotation = new BasicHep3Matrix();
    
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
    
    int num_lay = 7;
    
    //double ebeam = 1.056;
    double zPosTop = 59.3105;
    double zPosBot = 40.843;
    
    
    public void detectorChanged(Detector detector){
        
        beamAxisRotation.setActiveEuler(Math.PI / 2, -0.0305, -Math.PI / 2);
        aida.tree().cd("/");
        tree = aida.tree();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);
        
     // Get the HpsSiSensor objects from the tracker detector element
        sensors = detector.getSubdetector(SUBDETECTOR_NAME)
                          .getDetectorElement().findDescendants(HpsSiSensor.class);
   
        // If the detector element had no sensors associated with it, throw
        // an exception
        if (sensors.size() == 0) {
            throw new RuntimeException("No sensors were found in this detector.");
        }
            
        unconMass = aida.histogram1D(unconstrainedV0CandidatesColName + "/" + "Invariant Mass (GeV)", 100, 0, 0.200);
        unconVx = aida.histogram1D(unconstrainedV0CandidatesColName + "/" + "Vx (mm)", 50, -10, 10);
        unconVy = aida.histogram1D(unconstrainedV0CandidatesColName + "/" + "Vy (mm)", 50, -10, 10);
        unconVz = aida.histogram1D(unconstrainedV0CandidatesColName + "/" + "Vz (mm)", 50, -50, 50);
        unconChi2 = aida.histogram1D(unconstrainedV0CandidatesColName + "/" + "Chi2", 25, 0, 25);
        unconVzVsChi2 = aida.histogram2D(unconstrainedV0CandidatesColName + "/" + "Vz vs. Chi2", 25, 0, 25, 50, -50, 50);
        unconChi2VsTrkChi2 = aida.histogram2D(unconstrainedV0CandidatesColName + "/" + "Chi2 vs. total track chi2", 50, 0, 50, 50, 0, 25);
        /* beamspot constrained */
        bsconMass = aida.histogram1D(beamConV0CandidatesColName + "/" + "Mass (GeV)", 100, 0, 0.200);
        bsconVx = aida.histogram1D(beamConV0CandidatesColName + "/" + "Vx (mm)", 50, -10, 10);
        bsconVy = aida.histogram1D(beamConV0CandidatesColName + "/" + "Vy (mm)", 50, -10, 10);
        bsconVz = aida.histogram1D(beamConV0CandidatesColName + "/" + "Vz (mm)", 50, -50, 50);
        bsconChi2 = aida.histogram1D(beamConV0CandidatesColName + "/" + "Chi2", 25, 0, 25);
        bsconVzVsChi2 = aida.histogram2D(beamConV0CandidatesColName + "/" + "Vz vs. Chi2", 25, 0, 25, 50, -50, 50);
        bsconChi2VsTrkChi2 = aida.histogram2D(beamConV0CandidatesColName + "/" + "Chi2 vs. total track chi2", 50, 0, 50, 50, 0, 25);
        /* target constrained */
        tarconMass = aida.histogram1D(targetV0ConCandidatesColName + "/" + "Mass (GeV)", 100, 0, 0.200);
        tarconVx = aida.histogram1D(targetV0ConCandidatesColName + "/" + "Vx (mm)", 50, -1, 1);
        tarconVy = aida.histogram1D(targetV0ConCandidatesColName + "/" + "Vy (mm)", 50, -1, 1);
        tarconVz = aida.histogram1D(targetV0ConCandidatesColName + "/" + "Vz (mm)", 50, -10, 10);
        tarconChi2 = aida.histogram1D(targetV0ConCandidatesColName + "/" + "Chi2", 25, 0, 25);
        tarconVzVsChi2 = aida.histogram2D(targetV0ConCandidatesColName + "/" + "Vz vs. Chi2", 25, 0, 25, 50, -50, 50);
        tarconChi2VsTrkChi2 = aida.histogram2D(targetV0ConCandidatesColName + "/" + "Chi2 vs. total track chi2", 50, 0, 50, 50, 0, 25);

        nV0 = aida.histogram1D("Number of V0 per event", 10, 0, 10);
        v0Time = aida.histogram1D("V0 mean time", 100, -25, 25);
        v0Dt = aida.histogram1D("V0 time difference", 100, -25, 25);
        trigTimeV0Time = aida.histogram2D("Trigger phase vs. V0 mean time", 100, -25, 25, 6, 0, 24);
        trigTime = aida.histogram1D("Trigger phase", 6, 0, 24);

        pEleVspPos = aida.histogram2D("P(e) vs P(p)", 50, 0, eBeam * maxFactor, 50, 0, eBeam * maxFactor);
        pEleVspPosWithCut = aida.histogram2D("P(e) vs P(p): Radiative", 50, 0, eBeam * maxFactor, 50, 0, eBeam * maxFactor);
        pyEleVspyPos = aida.histogram2D("Py(e) vs Py(p)", 50, -0.04, 0.04, 50, -0.04, 0.04);
        pxEleVspxPos = aida.histogram2D("Px(e) vs Px(p)", 50, -0.04, 0.04, 50, -0.04, 0.04);
        VtxZVsMass = aida.histogram2D("Vz vs Mass", 50, 0, 0.15, 50, -50, 80);
        VtxXVsVtxZ = aida.histogram2D("Vx vs Vz", 100, -10, 10, 100, -50, 80);
        VtxYVsVtxZ = aida.histogram2D("Vy vs Vz", 100, -5, 5, 100, -50, 80);
        VtxXVsVtxY = aida.histogram2D("Vx vs Vy", 100, -10, 10, 100, -5, 5);
        VtxXVsVtxPx = aida.histogram2D("Vx vs Px", 100, -0.1, 0.1, 100, -10, 10);
        VtxYVsVtxPy = aida.histogram2D("Vy vs Py", 100, -0.1, 0.1, 100, -5, 5);
        VtxZVsVtxPx = aida.histogram2D("Vz vs Px", 100, -0.1, 0.1, 100, -50, 80);
        VtxZVsVtxPy = aida.histogram2D("Vz vs Py", 100, -0.1, 0.1, 100, -50, 80);
        VtxZVsVtxPz = aida.histogram2D("Vz vs Pz", 100, 0.0, eBeam * maxFactor, 100, -50, 80);
        VtxZVsL1Iso = aida.histogram2D("Vz vs L1 Isolation", 100, 0.0, 5.0, 50, -50, 80);
        VtxZVsTrkChi2 = aida.histogram2D("Vz vs Track Chi2", 50, 0, 50, 50, -50, 80);
        pEleVspEle = aida.histogram2D("2 Electron/P(e) vs P(e)", 50, 0, eBeam * maxFactor, 50, 0, eBeam * maxFactor);
        phiEleVsphiEle = aida.histogram2D("2 Electron/phi(e) vs phi(e)", 50, -Math.PI, Math.PI, 50, -Math.PI, Math.PI);
        pyEleVspyEle = aida.histogram2D("2 Electron/Py(e) vs Py(e)", 50, -0.04, 0.04, 50, -0.04, 0.04);
        pxEleVspxEle = aida.histogram2D("2 Electron/Px(e) vs Px(e)", 50, -0.02, 0.06, 50, -0.02, 0.06);
        pEleVspEleNoBeam = aida.histogram2D("2 Electron/P(e) vs P(e) NoBeam", 50, 0, beambeamCut, 50, 0, beambeamCut);
        pEleVspEleMoller = aida.histogram2D("2 Electron/P(e) vs P(e) Moller", 50, 0, beambeamCut, 50, 0, beambeamCut);
        pEleVspEleBeamBeam = aida.histogram2D("2 Electron/P(e) vs P(e) BeamBeam", 50, beambeamCut, eBeam * maxFactor, 50, beambeamCut, eBeam * maxFactor);
        pyEleVspyEleNoBeam = aida.histogram2D("2 Electron/Py(e) vs Py(e) NoBeam", 50, -0.04, 0.04, 50, -0.04, 0.04);
        pxEleVspxEleNoBeam = aida.histogram2D("2 Electron/Px(e) vs Px(e) NoBeam", 50, -0.02, 0.06, 50, -0.02, 0.06);
        sumChargeHisto = aida.histogram1D("Total Charge of  Event", 5, -2, 3);
        numChargeHisto = aida.histogram1D("Number of Charged Particles", 6, 0, 6);

        pEleVsthetaMoller = aida.histogram2D("2 Electron/P(e) vs Theta Moller", 50, 0, beambeamCut, 50, thetaMin, thetaMax);
        thetaEleVsthetaMoller = aida.histogram2D("2 Electron/Theta vs Theta Moller", 50, thetaMin, thetaMax, 50, thetaMin, thetaMax);
        pEleVsthetaBeamBeam = aida.histogram2D("2 Electron/P(e) vs Theta BeamBeam", 50, beambeamCut, eBeam * maxFactor, 50, thetaMin, thetaMax);
        thetaEleVsthetaBeamBeam = aida.histogram2D("2 Electron/Theta vs Theta BeamBeam", 50, thetaMin, thetaMax, 50, thetaMin, thetaMax);

        mollerMass = aida.histogram1D("2 Electron/Moller Mass (GeV)", 100, 0, 0.100);
        mollerMassVtxCut = aida.histogram1D("2 Electron/Moller Mass (GeV): VtxCut", 100, 0, 0.100);
        mollerVx = aida.histogram1D("2 Electron/Moller Vx (mm)", 50, -10, 10);
        mollerVy = aida.histogram1D("2 Electron/Moller Vy (mm)", 50, -2, 2);
        mollerVz = aida.histogram1D("2 Electron/Moller Vz (mm)", 50, -50, 50);
        mollerVzVtxCut = aida.histogram1D("2 Electron/Moller Vz (mm): VtxCut", 50, -50, 50);
        mollerXVsVtxZ = aida.histogram2D("2 Electron/Moller Vx vs Vz", 100, -5, 5, 100, -50, 50);
        mollerYVsVtxZ = aida.histogram2D("2 Electron/Moller Vy vs Vz", 100, -2, 2, 100, -50, 50);
        mollerXVsVtxY = aida.histogram2D("2 Electron/Moller Vx vs Vy", 100, -5, 5, 100, -2, 2);

        
        for (IPlotter plotter : plotters.values()) { 
            //plotter.show();
        }
    }
    
    private void mapMCtoSimTrackerHit(List<SimTrackerHit> simTrackerHits) {
        
        // Create a relational table that maps SimTrackerHits to MCParticles
        _hittomc = new BaseRelationalTable<SimTrackerHit, MCParticle>(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);

        // Loop over the SimTrackerHits and fill in the relational table    
        for (SimTrackerHit simhit : simTrackerHits) {   
            if (simhit.getMCParticle() != null)    
                _hittomc.add(simhit, simhit.getMCParticle());
        }
    }
    
    @Override
    public void process(EventHeader event){
        //System.out.println("Pass1");
        aida.tree().cd("/");
        
        List<SimTrackerHit> simHits = event.get(SimTrackerHit.class, "TrackerHits"); 
        this.mapMCtoSimTrackerHit(simHits);
        
        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);
        
        // If the event does not have tracks, skip it
        //if(!event.hasCollection(Track.class, trackCollectionName)) return;
        
        
        
        List<ReconstructedParticle> unonstrainedV0List = event.get(ReconstructedParticle.class, unconstrainedV0CandidatesColName);
        for (ReconstructedParticle uncV0 : unonstrainedV0List) {
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

                /*Double[] eleIso = TrackUtils.getIsolations(ele.getTracks().get(0), hitToStrips, hitToRotated);
                Double[] posIso = TrackUtils.getIsolations(pos.getTracks().get(0), hitToStrips, hitToRotated);
                if (eleIso[0] != null && posIso[0] != null) {
                    double eleL1Iso = Math.min(Math.abs(eleIso[0]), Math.abs(eleIso[1]));
                    double posL1Iso = Math.min(Math.abs(posIso[0]), Math.abs(posIso[1]));
                    double minL1Iso = Math.min(eleL1Iso, posL1Iso);
                    VtxZVsL1Iso.fill(minL1Iso, uncVert.getPosition().z());
                }*/

                double pe = ele.getMomentum().magnitude();
                double pp = pos.getMomentum().magnitude();
                Hep3Vector pEleRot = VecOp.mult(beamAxisRotation, ele.getMomentum());
                Hep3Vector pPosRot = VecOp.mult(beamAxisRotation, pos.getMomentum());

                pEleVspPos.fill(pe, pp);
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
        nV0.fill(beamConstrainedV0List.size());
        for (ReconstructedParticle bsV0 : beamConstrainedV0List) {

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

        List<ReconstructedParticle> targetConstrainedV0List = event.get(ReconstructedParticle.class, targetV0ConCandidatesColName);
        for (ReconstructedParticle tarV0 : targetConstrainedV0List) {

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

        ReconstructedParticle ele1 = null;
        ReconstructedParticle ele2 = null;
        int sumCharge = 0;
        int numChargedParticles = 0;
        for (ReconstructedParticle fsPart : finalStateParticles) {
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
                BilliorVertex bv = fitVertex(btEle1, btEle2);
//                LOGGER.info("ee vertex: "+bv.toString());
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
    public void endOfData(){
        
        
        System.out.println("%===================================================================%");
        System.out.println("%======================  Layer 0 Reconctructed Particle Complete==========================%");
        System.out.println("%===================================================================% \n%");
        System.out.println("% \n%===================================================================%");
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
