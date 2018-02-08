package org.hps.analysis.tuple;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hps.analysis.MC.MCFullDetectorTruth;
import org.hps.analysis.MC.TrackTruthMatching;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.geometry.IDDecoder;

public abstract class FullTruthTupleMaker extends MCTupleMaker {
    
    private final String trackHitMCRelationsCollectionName = "RotatedHelicalTrackMCRelations";
    
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
        // get objects and collections from event header
        List<SimTrackerHit> trackerHits = event.get(SimTrackerHit.class, "TrackerHits");
        List<SimTrackerHit> trackerHits_Inactive = event.get(SimTrackerHit.class, "TrackerHits_Inactive");
        List<SimCalorimeterHit> calHits = event.get(SimCalorimeterHit.class, "EcalHits");
        List<MCParticle> particles = event.get(MCParticle.class, "MCParticle");        
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
        }   
    }
    
    
    protected void fillFullVertexTruth(EventHeader event, Track eleTrack, Track posTrack){
        List<SimTrackerHit> trackerHits = event.get(SimTrackerHit.class, "TrackerHits");
        List<SimTrackerHit> trackerHits_Inactive = event.get(SimTrackerHit.class, "TrackerHits_Inactive");
        List<SimCalorimeterHit> calHits = event.get(SimCalorimeterHit.class, "EcalHits");
        //List<MCParticle> particles = event.get(MCParticle.class, "MCParticle");        
        IDDecoder trackerDecoder = event.getMetaData(trackerHits).getIDDecoder();
        IDDecoder calDecoder = event.getMetaData(calHits).getIDDecoder();
        
        RelationalTable hittomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> mcrelations = event.get(LCRelation.class, trackHitMCRelationsCollectionName);
        for (LCRelation relation : mcrelations)
            if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                hittomc.add(relation.getFrom(), relation.getTo());
        
        Map<MCParticle, List<SimCalorimeterHit>> calHitMap = MCFullDetectorTruth.BuildCalHitMap(calHits);
        Map<MCParticle, Map<String, List<SimTrackerHit>>> comboTrackerHitMap = MCFullDetectorTruth.BuildComboTrackerHitMap(trackerHits,trackerHits_Inactive);
    
        TrackTruthMatching eleTruth = new TrackTruthMatching(eleTrack, hittomc);
        TrackTruthMatching posTruth = new TrackTruthMatching(posTrack, hittomc);
        
        //System.out.println(eleTruth + "  " + posTruth);
        
        MCParticle ele = eleTruth.getMCParticle();
        MCParticle pos = posTruth.getMCParticle();
        
        //if(ele == null) System.out.println("Ele no match");
        //if(pos == null) System.out.println("Pos no match");
        
        if(ele == null && pos == null){
            //System.out.println("Ele or Pos is null!");
            return;
        }
        
        if(ele != null){
            String MCprefix = "ele";
            tupleMap.put(MCprefix+"HasTruthMatch/I", (double) 1);
            tupleMap.put(MCprefix+"NTruthHits/I", (double) eleTruth.getNHits());
            tupleMap.put(MCprefix+"NBadTruthHits/I", (double) eleTruth.getNBadHits());
            tupleMap.put(MCprefix+"Purity/D", eleTruth.getPurity());
            fillMCParticleVariables("ele", ele);
            for (Entry<MCParticle, Map<String, List<SimTrackerHit>>> entry : comboTrackerHitMap.entrySet()) {
                MCParticle p = entry.getKey();
                if (ele != p) continue;
                List<SimTrackerHit> hits_act = entry.getValue().get("active");
                List<SimTrackerHit> hits_in = entry.getValue().get("inactive");
                
                Hep3Vector startPosition = p.getOrigin();
                Hep3Vector startMomentum = p.getMomentum();
                
                int i = 0;
                int k = 0;
                String layerprefix_1 = "";
             // loop over particle's hits
                boolean inactiveprev = false;
                for (SimTrackerHit hit : hits_act) {
                    boolean inactive = false;
                    SimTrackerHit hit_act = hit;
                    do{
                        k++;
                        inactive = false;
                        trackerDecoder.setID(hit.getCellID64());
                        if(hits_in != null){
                            int j = 0;
                            for(SimTrackerHit hit_in:hits_in){
                                j++;
                                if(i >= j) continue;
                                if(hit_in.getLayer() >= hit_act.getLayer()) continue;
                                hit = hit_in;
                                inactive = true;
                                i = j;
                                break;
                            }
                        }
                        
                        if(!inactive) hit = hit_act;
                
                        String layerprefix = MCFullDetectorTruth.trackHitLayer(hit);
                        //String layerprefix_1 = MCFullDetectorTruth.trackHitLayer_1(hit);
                        String prefix = "";
                        String prefix_1 = "";
                        if(!inactive){
                            prefix = MCprefix + layerprefix;
                        }
                        else{
                            prefix = MCprefix + layerprefix + "In";
                        }
                        if(!inactiveprev){
                            prefix_1 = MCprefix + layerprefix_1;
                        }
                        else{
                            prefix_1 = MCprefix + layerprefix_1 +"In";
                        }

                        Hep3Vector endPosition = hit.getPositionVec();
                        Hep3Vector endMomentum = new BasicHep3Vector(hit.getMomentum()[0], hit.getMomentum()[1], hit.getMomentum()[2]);
                        
                        inactiveprev = inactive;
                        layerprefix_1 = layerprefix;
                        
                        if(k == 1){
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
                    } while(inactive);
                }
                break;
            }
            

            for (Entry<MCParticle, List<SimCalorimeterHit>> entry : calHitMap.entrySet()) {
                MCParticle p = entry.getKey();
                if (ele != p) continue;
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
                break;
            }
        }
        else{
            tupleMap.put("eleHasTruthMatch/I", (double) 0);
        }
        
        if(pos != null){
            String MCprefix = "pos";
            tupleMap.put(MCprefix+"HasTruthMatch/I", (double) 1);
            tupleMap.put(MCprefix+"HasTruthMatch/I", (double) 1);
            tupleMap.put(MCprefix+"NTruthHits/I", (double) posTruth.getNHits());
            tupleMap.put(MCprefix+"NBadTruthHits/I", (double) posTruth.getNBadHits());
            tupleMap.put(MCprefix+"Purity/D", posTruth.getPurity());
            fillMCParticleVariables("pos", pos);
            
            for (Entry<MCParticle, Map<String, List<SimTrackerHit>>> entry : comboTrackerHitMap.entrySet()) {
                MCParticle p = entry.getKey();
                if (pos != p) continue;
                List<SimTrackerHit> hits_act = entry.getValue().get("active");
                List<SimTrackerHit> hits_in = entry.getValue().get("inactive");
                
                Hep3Vector startPosition = p.getOrigin();
                Hep3Vector startMomentum = p.getMomentum();
                
                int i = 0;
                int k = 0;
                String layerprefix_1 = "";
             // loop over particle's hits
                boolean inactiveprev = false;
                for (SimTrackerHit hit : hits_act) {
                    boolean inactive = false;
                    SimTrackerHit hit_act = hit;
                    do{
                        k++;
                        inactive = false;
                        trackerDecoder.setID(hit.getCellID64());
                        if(hits_in != null){
                            int j = 0;
                            for(SimTrackerHit hit_in:hits_in){
                                j++;
                                if(i >= j) continue;
                                if(hit_in.getLayer() >= hit_act.getLayer()) continue;
                                hit = hit_in;
                                inactive = true;
                                i = j;
                                break;
                            }
                        }
                        
                        if(!inactive) hit = hit_act;
                
                        String layerprefix = MCFullDetectorTruth.trackHitLayer(hit);
                        //String layerprefix_1 = MCFullDetectorTruth.trackHitLayer_1(hit);
                        String prefix = "";
                        String prefix_1 = "";
                        if(!inactive){
                            prefix = MCprefix + layerprefix;
                        }
                        else{
                            prefix = MCprefix + layerprefix + "In";
                        }
                        if(!inactiveprev){
                            prefix_1 = MCprefix + layerprefix_1;
                        }
                        else{
                            prefix_1 = MCprefix + layerprefix_1 +"In";
                        }

                        Hep3Vector endPosition = hit.getPositionVec();
                        Hep3Vector endMomentum = new BasicHep3Vector(hit.getMomentum()[0], hit.getMomentum()[1], hit.getMomentum()[2]);
                        
                        inactiveprev = inactive;
                        layerprefix_1 = layerprefix;
                        
                        if(k == 1){
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
                    } while(inactive);
                }
                break;
            }
            for (Entry<MCParticle, List<SimCalorimeterHit>> entry : calHitMap.entrySet()) {
                MCParticle p = entry.getKey();
                if (pos != p) continue;
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
                break;
            }
        }
        else{
            tupleMap.put("posHasTruthMatch/I", (double) 0);
        }
        
    }

}
