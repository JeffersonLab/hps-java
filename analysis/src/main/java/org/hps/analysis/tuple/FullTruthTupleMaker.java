package org.hps.analysis.tuple;

//import hep.physics.vec.BasicHep3Vector;
//import hep.physics.vec.Hep3Vector;
//import hep.physics.vec.VecOp;

import java.util.Arrays;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;

//import org.apache.commons.math3.util.Pair;
import org.hps.analysis.MC.MCFullDetectorTruth;
//import org.hps.analysis.MC.TrackTruthMatching;
//import org.lcsim.detector.IDetectorElement;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
//import org.lcsim.geometry.IDDecoder;

public abstract class FullTruthTupleMaker extends MCTupleMaker {
    
    private String trackerHitsCollectionName = "TrackerHits";
    private String inactiveTrackerHitsCollectionName = "TrackerHits_Inactive";
    private String ecalHitsCollectionName = "EcalHits";
    private final String mcParticleCollectionName = "MCParticle";

    public void setTrackerHitsCollectionName(String trackerHitsCollectionName) {
        this.trackerHitsCollectionName = trackerHitsCollectionName;
    }
    
    public void setInactiveTrackerHitsCollectionName(String inactiveTrackerHitsCollectionName) {
        this.inactiveTrackerHitsCollectionName = inactiveTrackerHitsCollectionName;
    }
    
    public void setEcalHitsCollectionName(String ecalHitsCollectionName) {
        this.ecalHitsCollectionName = ecalHitsCollectionName;
    }
    
    protected void addEventVariables() {
        super.addEventVariables();
        String[] newVars = new String[] {"run/I", "event/I", "tupleevent/I"};
        tupleVariables.addAll(Arrays.asList(newVars));
    }
    
    protected void addFullTruthVertexVariables() {
        addMCParticleVariables("ele");
        addMCParticleVariables("pos");
        addEcalTruthVariables("ele");
        addEcalTruthVariables("pos");
        addSVTTruthVariables("ele");
        addSVTTruthVariables("pos");
    }
    
    protected void addFullMCTridentVariables() {
        addMCTridentVariables();
        addEcalTruthVariables("triEle1");
        addEcalTruthVariables("triEle2");
        addEcalTruthVariables("triPos");
        addSVTTruthVariables("triEle1");
        addSVTTruthVariables("triEle2");
        addSVTTruthVariables("triPos");
    }
    
    protected void addFullMCWabVariables() {
        addMCTridentVariables();
        addEcalTruthVariables("wabEle1");
        addEcalTruthVariables("wabEle2");
        addEcalTruthVariables("wabPos");
        addSVTTruthVariables("wabEle1");
        addSVTTruthVariables("wabEle2");
        addSVTTruthVariables("wabPos");
    }
    
    protected void fillMCFullTruthVariables(EventHeader event){
        if (!event.hasCollection(SimTrackerHit.class, trackerHitsCollectionName)) {
            return;
        }
        if (!event.hasCollection(SimTrackerHit.class, inactiveTrackerHitsCollectionName)) {
            return;
        }
        if (!event.hasCollection(SimCalorimeterHit.class, ecalHitsCollectionName)) {
            return;
        }
        if (!event.hasCollection(MCParticle.class, mcParticleCollectionName)) {
            return;
        }
    
        // get objects and collections from event header
        /*List<SimTrackerHit> trackerHits = event.get(SimTrackerHit.class, trackerHitsCollectionName);
        List<SimTrackerHit> trackerHits_Inactive = event.get(SimTrackerHit.class, inactiveTrackerHitsCollectionName);
        List<SimCalorimeterHit> calHits = event.get(SimCalorimeterHit.class, ecalHitsCollectionName);
        List<MCParticle> particles = event.get(MCParticle.class, mcParticleCollectionName);        
        IDDecoder trackerDecoder = event.getMetaData(trackerHits).getIDDecoder();
        IDDecoder calDecoder = event.getMetaData(calHits).getIDDecoder();
            
        // maps of particles to hits
        //Map<MCParticle, List<SimTrackerHit>> trackerHitMap = new HashMap<MCParticle, List<SimTrackerHit>>();
        //Map<MCParticle, List<SimCalorimeterHit>> calHitMap = new HashMap<MCParticle, List<SimCalorimeterHit>>();
        
        Map<MCParticle, List<SimTrackerHit>> trackerHitMap = MCFullDetectorTruth.BuildTrackerHitMap(trackerHits);
        Map<MCParticle, List<SimTrackerHit>> trackerInHitMap = MCFullDetectorTruth.BuildTrackerHitMap(trackerHits_Inactive);
        Map<MCParticle, List<SimCalorimeterHit>> calHitMap = MCFullDetectorTruth.BuildCalHitMap(calHits);
        
        for (Entry<MCParticle, List<SimTrackerHit>> entry : trackerInHitMap.entrySet()) {
            
            MCParticle p = entry.getKey();
            List<SimTrackerHit> hits = entry.getValue();
            
            String MCprefix = MCFullDetectorTruth.MCParticleType(p, particles, ebeam);
            boolean isTri = MCprefix == "triEle1" || MCprefix == "triEle2" || MCprefix == "triPos";
            boolean isWab = MCprefix == "wabEle1" || MCprefix == "wabEle2" || MCprefix == "wabPos";
            
            if(!isTri && !isWab) continue;
            
            //if (isTri) System.out.println("Is Trident!");
            //if (isWab) System.out.println("Is Wab!");
            
            Hep3Vector startPosition = p.getOrigin();
            Hep3Vector startMomentum = p.getMomentum();

            // loop over particle's hits
            for (SimTrackerHit hit : hits) {
                
                trackerDecoder.setID(hit.getCellID64());
            
                String layerprefix = MCFullDetectorTruth.trackHitLayer(hit);
                String layerprefix_1 = MCFullDetectorTruth.trackHitLayer_1(hit);
                String prefix = MCprefix + layerprefix;
                String prefix_1 = MCprefix + layerprefix_1;
                
                Hep3Vector endPosition = hit.getPositionVec();
                Hep3Vector endMomentum = new BasicHep3Vector(hit.getMomentum()[0], hit.getMomentum()[1], hit.getMomentum()[2]);;
                
                if(hit == hits.get(0)){
                    startPosition = endPosition;
                    startMomentum = endMomentum;
                    continue;
                }
            
                double q = p.getCharge();
                Hep3Vector extrapPos = MCFullDetectorTruth.extrapolateTrackPosition(endPosition,endMomentum,startPosition,5,bFieldMap,q);
                Hep3Vector extrapP = MCFullDetectorTruth.extrapolateTrackMomentum(endPosition,endMomentum,startPosition,5,bFieldMap,q);
                
                if(extrapP == null) continue;
        
                Hep3Vector startProt = VecOp.mult(beamAxisRotation, startMomentum);
                Hep3Vector extrapProt = VecOp.mult(beamAxisRotation, VecOp.neg(extrapP));
                Hep3Vector endPositionrot = VecOp.mult(beamAxisRotation, endPosition);
            
                double thetaX = MCFullDetectorTruth.deltaThetaX(extrapProt,startProt);
                double thetaY = MCFullDetectorTruth.deltaThetaY(extrapProt,startProt);
                
                tupleMap.put(prefix+"svthitX/D", endPositionrot.x());
                tupleMap.put(prefix+"svthitY/D", endPositionrot.y());
                tupleMap.put(prefix+"svthitZ/D", endPositionrot.z());
                tupleMap.put(prefix_1+"svthitPx/D", startProt.x());
                tupleMap.put(prefix_1+"svthitPy/D", startProt.y());
                tupleMap.put(prefix_1+"svthitPz/D", startProt.z());
                tupleMap.put(prefix_1+"thetaX/D", thetaX);
                tupleMap.put(prefix_1+"thetaY/D", thetaY);
                tupleMap.put(prefix_1+"residualX/D", startPosition.x()-extrapPos.x());
                tupleMap.put(prefix_1+"residualY/D", startPosition.y()-extrapPos.y());
                
                startPosition = endPosition;
                startMomentum = endMomentum;
            }            
        }
        
        // loop over entries mapping particles to sim tracker hits
        for (Entry<MCParticle, List<SimTrackerHit>> entry : trackerHitMap.entrySet()) {
                
            MCParticle p = entry.getKey();
            List<SimTrackerHit> hits = entry.getValue();
            
            String MCprefix = MCFullDetectorTruth.MCParticleType(p, particles, ebeam);
            //System.out.println(MCprefix);
            boolean isTri = MCprefix == "triEle1" || MCprefix == "triEle2" || MCprefix == "triPos";
            boolean isWab = MCprefix == "wabEle1" || MCprefix == "wabEle2" || MCprefix == "wabPos";
            
            if(!isTri && !isWab) continue;
            
            //if (isTri) System.out.println("Is Trident!");
            //if (isWab) System.out.println("Is Wab!");
            
            
            Hep3Vector startPosition = p.getOrigin();
            Hep3Vector startMomentum = p.getMomentum();

            // loop over particle's hits
            for (SimTrackerHit hit : hits) {
                
                trackerDecoder.setID(hit.getCellID64());
            
                String layerprefix = MCFullDetectorTruth.trackHitLayer(hit);
                String layerprefix_1 = MCFullDetectorTruth.trackHitLayer_1(hit);
                String prefix = MCprefix + layerprefix;
                String prefix_1 = MCprefix + layerprefix_1;
                
                Hep3Vector endPosition = hit.getPositionVec();
                Hep3Vector endMomentum = new BasicHep3Vector(hit.getMomentum()[0], hit.getMomentum()[1], hit.getMomentum()[2]);;
                
                if(hit == hits.get(0)){
                    startPosition = endPosition;
                    startMomentum = endMomentum;
                    continue;
                }
            
                double q = p.getCharge();
                Hep3Vector extrapPos = MCFullDetectorTruth.extrapolateTrackPosition(endPosition,endMomentum,startPosition,5,bFieldMap,q);
                Hep3Vector extrapP = MCFullDetectorTruth.extrapolateTrackMomentum(endPosition,endMomentum,startPosition,5,bFieldMap,q);
                
                if(extrapP == null) continue;
        
                Hep3Vector startProt = VecOp.mult(beamAxisRotation, startMomentum);
                Hep3Vector extrapProt = VecOp.mult(beamAxisRotation, VecOp.neg(extrapP));
                Hep3Vector endPositionrot = VecOp.mult(beamAxisRotation, endPosition);
            
                double thetaX = MCFullDetectorTruth.deltaThetaX(extrapProt,startProt);
                double thetaY = MCFullDetectorTruth.deltaThetaY(extrapProt,startProt);
                
                tupleMap.put(prefix+"svthitX/D", endPositionrot.x());
                tupleMap.put(prefix+"svthitY/D", endPositionrot.y());
                tupleMap.put(prefix+"svthitZ/D", endPositionrot.z());
                tupleMap.put(prefix_1+"svthitPx/D", startProt.x());
                tupleMap.put(prefix_1+"svthitPy/D", startProt.y());
                tupleMap.put(prefix_1+"svthitPz/D", startProt.z());
                tupleMap.put(prefix_1+"thetaX/D", thetaX);
                tupleMap.put(prefix_1+"thetaY/D", thetaY);
                tupleMap.put(prefix_1+"residualX/D", startPosition.x()-extrapPos.x());
                tupleMap.put(prefix_1+"residualY/D", startPosition.y()-extrapPos.y());
                
                startPosition = endPosition;
                startMomentum = endMomentum;
            }            
        }
        
     // loop over entries mapping particles to sim cal hits
        for (Entry<MCParticle, List<SimCalorimeterHit>> entry : calHitMap.entrySet()) {
            MCParticle p = entry.getKey();
            String MCprefix = MCFullDetectorTruth.MCParticleType(p, particles, ebeam);
            List<SimCalorimeterHit> hits = entry.getValue();
            int i = 0;
            for (SimCalorimeterHit hit : hits) {
                calDecoder.setID(hit.getCellID());
                int ix = calDecoder.getValue("ix");
                int iy = calDecoder.getValue("iy");
                String HitNum = Integer.toString(i);
                String prefix = MCprefix + "Hit" + HitNum;
                tupleMap.put(prefix+"ecalhitIx/D", (double) ix);
                tupleMap.put(prefix+"ecalhitIy/D", (double) iy);
                tupleMap.put(prefix+"ecalhitX/D", hit.getPositionVec().x());
                tupleMap.put(prefix+"ecalhitY/D", hit.getPositionVec().y());
                tupleMap.put(prefix+"ecalhitZ/D", hit.getPositionVec().z());
                tupleMap.put(prefix+"ecalhitEnergy/D", hit.getCorrectedEnergy());
                i++;
            }
        }   */
    }
    
    protected void fillFullVertexTruth(EventHeader event, Track eleTrack, Track posTrack){
        if (!event.hasCollection(SimTrackerHit.class, trackerHitsCollectionName)) {
            return;
        }
        if (!event.hasCollection(SimTrackerHit.class, inactiveTrackerHitsCollectionName)) {
            return;
        }
        if (!event.hasCollection(SimCalorimeterHit.class, ecalHitsCollectionName)) {
            return;
        }
        if (!event.hasCollection(MCParticle.class, mcParticleCollectionName)) {
            return;
        }
        MCFullDetectorTruth eleFullTruth = new MCFullDetectorTruth(event, eleTrack, bFieldMap, sensors, trackerSubdet);
        MCFullDetectorTruth posFullTruth = new MCFullDetectorTruth(event, posTrack, bFieldMap, sensors, trackerSubdet);
        
        MCParticle ele = eleFullTruth.getMCParticle();
        MCParticle pos = posFullTruth.getMCParticle();
        
        if(ele != null)
            fillTruth("ele",eleFullTruth);
        else
            tupleMap.put("eleHasTruthMatch/I", (double) 0);
        
        if(pos != null)
            fillTruth("pos",posFullTruth);
        else
            tupleMap.put("posHasTruthMatch/I", (double) 0);
    }
        
    public void fillTruth(String MCprefix,MCFullDetectorTruth partTruth){
        fillMCParticleVariables(MCprefix, partTruth.getMCParticle());
        tupleMap.put(MCprefix+"HasTruthMatch/I", (double) 1);
        tupleMap.put(MCprefix+"NTruthHits/I", (double) partTruth.getNHits());
        tupleMap.put(MCprefix+"NGoodTruthHits/I", (double) partTruth.getNBadHits());
        tupleMap.put(MCprefix+"NBadTruthHits/I", (double) partTruth.getNBadHits());
        tupleMap.put(MCprefix+"Purity/D", partTruth.getPurity());
        String isTop = "t";
        if(!partTruth.isTop())
            isTop = "b";
        for(int i = 0; i < nLay*2; i++){
            int layer = i + 1;
            tupleMap.put(MCprefix+"L"+Integer.toString(layer)+isTop+"NTruthParticles/I", (double) partTruth.getNumberOfMCParticles(layer));
            if(partTruth.getHitList(layer) != null)
                tupleMap.put(MCprefix+"L"+Integer.toString(layer)+isTop+"IsGoodTruthHit/I", (double) ((partTruth.getHitList(layer)) ? 1 : 0));

            String prefix = MCprefix + "L" + Integer.toString(layer);
            if(partTruth.isTop())
                prefix = prefix + "t";
            else
                prefix = prefix + "b";
            if(partTruth.getActiveHitPosition(layer) != null){
                tupleMap.put(prefix+"svthitX/D", partTruth.getActiveHitPosition(layer).x());
                tupleMap.put(prefix+"svthitY/D", partTruth.getActiveHitPosition(layer).y());
                tupleMap.put(prefix+"svthitZ/D", partTruth.getActiveHitPosition(layer).z());
            }
            if(partTruth.getActiveHitMomentum(layer) != null){
                tupleMap.put(prefix+"svthitPx/D", partTruth.getActiveHitMomentum(layer).x());
                tupleMap.put(prefix+"svthitPy/D", partTruth.getActiveHitMomentum(layer).y());
                tupleMap.put(prefix+"svthitPz/D", partTruth.getActiveHitMomentum(layer).z());
            }
            if(partTruth.getActiveHitScatter(layer) != null){
                tupleMap.put(prefix+"thetaX/D", partTruth.getActiveHitScatter(layer)[0]);
                tupleMap.put(prefix+"thetaY/D", partTruth.getActiveHitScatter(layer)[1]);
            }
            if(partTruth.getActiveHitResidual(layer) != null){
                tupleMap.put(prefix+"residualX/D", partTruth.getActiveHitResidual(layer)[0]);
                tupleMap.put(prefix+"residualY/D", partTruth.getActiveHitResidual(layer)[1]);
            }
            
            if(partTruth.getInactiveHitPosition(layer) != null){
                tupleMap.put(prefix+"InsvthitX/D", partTruth.getInactiveHitPosition(layer).x());
                tupleMap.put(prefix+"InsvthitY/D", partTruth.getInactiveHitPosition(layer).y());
                tupleMap.put(prefix+"InsvthitZ/D", partTruth.getInactiveHitPosition(layer).z());
            }
            if(partTruth.getInactiveHitMomentum(layer) != null){
                tupleMap.put(prefix+"InsvthitPx/D", partTruth.getInactiveHitMomentum(layer).x());
                tupleMap.put(prefix+"InsvthitPy/D", partTruth.getInactiveHitMomentum(layer).y());
                tupleMap.put(prefix+"InsvthitPz/D", partTruth.getInactiveHitMomentum(layer).z());
            }
            if(partTruth.getInactiveHitScatter(layer) != null){
                tupleMap.put(prefix+"InthetaX/D", partTruth.getInactiveHitScatter(layer)[0]);
                tupleMap.put(prefix+"InthetaY/D", partTruth.getInactiveHitScatter(layer)[1]);
            }
            if(partTruth.getInactiveHitResidual(layer) != null){
                tupleMap.put(prefix+"InresidualX/D", partTruth.getInactiveHitResidual(layer)[0]);
                tupleMap.put(prefix+"InresidualY/D", partTruth.getInactiveHitResidual(layer)[1]);
            }
        }
        
        for (int i = 0; i < partTruth.getEcalNHits(); i++) {
            String HitNum = Integer.toString(i);
            String prefix = MCprefix + "Hit" + HitNum;
            if(partTruth.getEcalHitIndex(i) != null){
                tupleMap.put(prefix+"ecalhitIx/I", (double) partTruth.getEcalHitIndex(i)[0]);
                tupleMap.put(prefix+"ecalhitIy/I", (double) partTruth.getEcalHitIndex(i)[1]);
            }
            if(partTruth.getEcalHitPosition(i) != null){
                tupleMap.put(prefix+"ecalhitX/D", partTruth.getEcalHitPosition(i).x());
                tupleMap.put(prefix+"ecalhitY/D", partTruth.getEcalHitPosition(i).y());
                tupleMap.put(prefix+"ecalhitZ/D", partTruth.getEcalHitPosition(i).z());
            }
            tupleMap.put(prefix+"ecalhitEnergy/D", partTruth.getEcalHitEnergy(i));
        }
    }
}
