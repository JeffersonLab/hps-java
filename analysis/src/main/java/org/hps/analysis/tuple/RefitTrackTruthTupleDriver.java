package org.hps.analysis.tuple;

import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.analysis.MC.MCFullDetectorTruth;
import org.hps.recon.particle.ReconParticleDriver;
import org.hps.recon.tracking.TrackType;
import org.hps.recon.vertexing.BilliorVertex;
import org.hps.record.triggerbank.TIData;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.Vertex;
import org.lcsim.event.base.BaseRelationalTable;

public class RefitTrackTruthTupleDriver extends TupleMaker {
    
    private final String trackColName = "GBLTracks";
    private final String badTrackColName = "GBLTracks_bad";
    private final String simhitOutputColName = "TrackerHits_truth";
    private final String truthMatchOutputColName = "MCFullDetectorTruth";
    private final String trackToMCParticleRelationsName = "TrackTruthToMCParticleRelations";
    private final String trackBadToTruthMatchRelationsOutputColName = "TrackBadToMCParticleRelations";
    private final String trackToTruthMatchRelationsOutputColName = "TrackToMCParticleRelations";
    private final String otherParticleRelationsColName = "TrackBadToOtherParticleRelations";
    private String trackerHitsCollectionName = "TrackerHits";
    private String inactiveTrackerHitsCollectionName = "TrackerHits_Inactive";
    private String ecalHitsCollectionName = "EcalHits";
    private final String mcParticleCollectionName = "MCParticle";
    private final String badMCParticleRelationsColName = "TrackBadToMCParticleBadRelations";
    
    private List<ReconstructedParticle> unConstrainedV0List = null;
    private List<ReconstructedParticle> bsConstrainedV0List = null;
    private List<ReconstructedParticle> tarConstrainedV0List = null;
    private List<Vertex> unConstrainedV0VerticeList = null;
    private List<ReconstructedParticle> unConstrainedV0TruthList = null;
    private List<ReconstructedParticle> bsConstrainedV0TruthList = null;
    private List<ReconstructedParticle> tarConstrainedV0TruthList = null;
    private List<Vertex> unConstrainedV0VerticeTruthList = null;
    private Map<ReconstructedParticle, BilliorVertex> cand2vert = null;
    private Map<ReconstructedParticle, BilliorVertex> cand2vertTruth = null;
    private Map<ReconstructedParticle, ReconstructedParticle> unc2bsc = null;
    private Map<ReconstructedParticle, ReconstructedParticle> unc2tar = null;
    private Map<ReconstructedParticle, ReconstructedParticle> unc2Truth = null;
    private Map<ReconstructedParticle, ReconstructedParticle> unc2bscTruth = null;
    private Map<ReconstructedParticle, ReconstructedParticle> unc2tarTruth = null;
    private int truthVertexHasMatch = 0;
    private int nEcalHit = 2;
    
    @Override
    protected void setupVariables() {
        tupleVariables.clear();
        addEventVariables();
        addVertexVariables("Bad");
        addVertexVariables("Truth");
        addParticleVariables("eleBad");
        addParticleVariables("posBad");
        addParticleVariables("eleTruth");
        addParticleVariables("posTruth");
        addParticleVariables("othereleTrack");
        addParticleVariables("otherposTrack");
        addFullTruthVertexVariables();
        addTruthRefitVariables();
        addMCParticleVariables("otherPos");
        addMCParticleVariables("otherEle");

        String[] newVars = new String[]{"minPositiveIso/D", "minNegativeIso/D", "minIso/D"};
        tupleVariables.addAll(Arrays.asList(newVars));

    }

    @Override
    public void process(EventHeader event) {
        setupCollections(event);
        for (ReconstructedParticle uncV0 : unConstrainedV0List) {
            tupleMap.clear();
            boolean isOK = fillBasicRefitTuple(event, triggerData, uncV0);
            fillTruthRefitVariables(event,uncV0);
            if (tupleWriter != null && isOK) {
                writeTuple();
            }
        }

    }

    private void addTruthRefitVariables() {
        String[] newVars = new String[] {"nTracksBad/I","nTracksTruth/I","nTracksBadEvent/I","nTracksTruthEvent/I","truthVertexHasMatch/I"};
        tupleVariables.addAll(Arrays.asList(newVars));
    }
    
    private void fillTruthRefitVariables(EventHeader event, ReconstructedParticle uncV0) {
        List<Track> tracksBad = event.get(Track.class,badTrackColName);
        List<Track> tracksTruth = event.get(Track.class,"GBLTracks_truth");
        
        int nTracksBad = 0;
        int nTracksTruth = 0;
        List<Track> tracks = uncV0.getTracks();
        for(Track track:tracks){
            for(Track trackBad:tracksBad){
                if(trackBad.equals(track)){
                    nTracksBad++;
                }
            }
            for(Track trackTruth:tracksTruth){
                if(trackTruth.equals(track)){
                    nTracksTruth++;
                }
            }
        }
        
        tupleMap.put("nTracksBadEvent/I", (double) tracksBad.size());
        tupleMap.put("nTracksTruthEvent/I", (double) tracksTruth.size());
        tupleMap.put("nTracksBad/I", (double) nTracksBad);
        tupleMap.put("nTracksTruth/I", (double) nTracksTruth);
        tupleMap.put("truthVertexHasMatch/I", (double) truthVertexHasMatch);
    }
    
    private boolean fillBasicRefitTuple(EventHeader event, TIData triggerData, ReconstructedParticle uncV0) {
        boolean isOK = true;

        if (isGBL != TrackType.isGBL(uncV0.getType())) {
            return false;
        }
        
        tupleMap.put("run/I", (double) event.getRunNumber());
        tupleMap.put("event/I", (double) event.getEventNumber());
        tupleMap.put("tupleevent/I", (double) tupleevent);
        tupleevent++;

        fillEventVariablesTrigger(event, triggerData);
        fillEventVariablesECal(event);
        fillEventVariablesHits(event);

        double minPositiveIso = 0;
        double minNegativeIso = 0;
        
        
        ReconstructedParticle electron = uncV0.getParticles().get(ReconParticleDriver.ELECTRON);
        ReconstructedParticle positron = uncV0.getParticles().get(ReconParticleDriver.POSITRON);
        if (electron.getCharge() != -1 || positron.getCharge() != 1) {
            throw new RuntimeException("incorrect charge on v0 daughters");
        }

        fillParticleVariables(event, electron, "eleBad");
        fillParticleVariables(event, positron, "posBad");
        List<LCRelation> badTrackRelation = event.get(LCRelation.class,otherParticleRelationsColName);

        for(LCRelation rel : badTrackRelation){
            if(((Track) rel.getFrom()).equals(electron.getTracks().get(0))){
                ReconstructedParticle badEleTrk = (ReconstructedParticle) rel.getTo();
                fillParticleVariables(event,badEleTrk,"othereleTrack");
            }
            if(((Track) rel.getFrom()).equals(positron.getTracks().get(0))){
                ReconstructedParticle badPosTrk = (ReconstructedParticle) rel.getTo();
                fillParticleVariables(event,badPosTrk,"otherposTrack");
            }
        }
        fillFullVertexTruth(event, electron.getTracks().get(0),positron.getTracks().get(0));
        
        minPositiveIso = Math.min(tupleMap.get("eleBadMinPositiveIso/D"), tupleMap.get("posBadMinPositiveIso/D"));
        minNegativeIso = Math.min(Math.abs(tupleMap.get("eleBadMinNegativeIso/D")), Math.abs(tupleMap.get("posBadMinNegativeIso/D")));
        
        double minIso = Math.min(minPositiveIso, minNegativeIso);
        tupleMap.put("minPositiveIso/D", minPositiveIso);
        tupleMap.put("minNegativeIso/D", minNegativeIso);
        tupleMap.put("minIso/D", minIso);
        
        fillVertexVariables("Badunc", uncV0, false);
        
        List<LCRelation> badMCParticleRelation = event.get(LCRelation.class,badMCParticleRelationsColName);
        MCParticle badEle = null;
        MCParticle badPos = null;
        for(LCRelation rel : badMCParticleRelation){
            if(((Track) rel.getFrom()).equals(electron.getTracks().get(0))){
                badEle = (MCParticle) rel.getTo();
            }
            else if(((Track) rel.getFrom()).equals(positron.getTracks().get(0))){
                badPos = (MCParticle) rel.getTo();
            }
            else{
                continue;
            }
        }
        if(badEle != null){
            fillMCParticleVariables("otherEle", badEle);
        }
        if(badPos != null){
            fillMCParticleVariables("otherPos", badPos);
        }

        if (unc2Truth != null) {
            ReconstructedParticle temp = unc2Truth.get(uncV0);
            if(!unConstrainedV0TruthList.isEmpty() && unc2Truth.isEmpty()){
                truthVertexHasMatch = 0;
                System.out.println("Truth vertex has NO MATCH!!");
            }
            else{
                truthVertexHasMatch = 1;
            }
            if (temp != null){
                ReconstructedParticle electronTruth = temp.getParticles().get(ReconParticleDriver.ELECTRON);
                ReconstructedParticle positronTruth = temp.getParticles().get(ReconParticleDriver.POSITRON);
                
                fillParticleVariables(event, electronTruth, "eleTruth", true, true, true, "GBLKinkDataRelations_truth");
                fillParticleVariables(event, positronTruth, "posTruth", true, true, true, "GBLKinkDataRelations_truth");
                fillVertexVariables("Truthunc", temp, false);
            }
            if (unc2bscTruth != null) {
                ReconstructedParticle temp2 = unc2bscTruth.get(temp);
                if (temp2 != null){
                    fillVertexVariables("Truthbsc", temp2, false);
                }
            }
            if (unc2tarTruth != null) {
                ReconstructedParticle temp2 = unc2tarTruth.get(temp);
                if (temp2 != null){
                    fillVertexVariables("Truthtar", temp2, false);
                }
            }
        }

        if (unc2bsc != null) {
            ReconstructedParticle temp = unc2bsc.get(uncV0);
            if (temp == null)
                isOK = false;
            else{
                fillVertexVariables("Badbsc", temp, false);
            }
        }
        if (unc2tar != null) {
            ReconstructedParticle temp = unc2tar.get(uncV0);
            if (temp == null)
                isOK = false;
            fillVertexVariables("Badtar", temp, false);
        }

        return isOK;
    }
    
    private void fillEventVariablesECal(EventHeader event) {
        if (event.hasCollection(CalorimeterHit.class, "EcalCalHits")) {
            List<CalorimeterHit> ecalHits = event.get(CalorimeterHit.class, "EcalCalHits");
            tupleMap.put("nEcalHits/I", (double) ecalHits.size());
        }
        
        if (event.hasCollection(Cluster.class, "EcalClustersCorr")) {
            List<Cluster> ecalClusters = event.get(Cluster.class, "EcalClustersCorr");
            tupleMap.put("nEcalCl/I", (double) ecalClusters.size());

            int nEle = 0;
            int nPos = 0;
            int nPho = 0;
            int nEleSide = 0;
            int nPosSide = 0;

            for (Cluster cc : ecalClusters) {
                if (cc.getParticleId() == 11) {
                    nEle++;
                }
                if (cc.getParticleId() == -11) {
                    nPos++;
                }
                if (cc.getParticleId() == 22) {
                    nPho++;
                }
                if (cc.getPosition()[0] < 0) {
                    nEleSide++;
                }
                if (cc.getPosition()[0] > 0) {
                    nPosSide++;
                }
            }

            tupleMap.put("nEcalClele/I", (double) nEle);
            tupleMap.put("nEcalClpos/I", (double) nPos);
            tupleMap.put("nEcalClpho/I", (double) nPho);
            tupleMap.put("nEcalClEleSide/I", (double) nEleSide);
            tupleMap.put("nEcalClPosSide/I", (double) nPosSide);

        }
    }
    
    private void fillEventVariablesTrigger(EventHeader event, TIData triggerData) {
        if (triggerData != null) {
            tupleMap.put("isCalib/B", triggerData.isCalibTrigger() ? 1.0 : 0.0);
            tupleMap.put("isPulser/B", triggerData.isPulserTrigger() ? 1.0 : 0.0);
            tupleMap.put("isSingle0/B", triggerData.isSingle0Trigger() ? 1.0 : 0.0);
            tupleMap.put("isSingle1/B", triggerData.isSingle1Trigger() ? 1.0 : 0.0);
            tupleMap.put("isPair0/B", triggerData.isPair0Trigger() ? 1.0 : 0.0);
            tupleMap.put("isPair1/B", triggerData.isPair1Trigger() ? 1.0 : 0.0);
        }

        if (event.hasCollection(GenericObject.class, "TriggerTime")) {
            if (event.get(GenericObject.class, "TriggerTime") != null) {
                List<GenericObject> triggT = event.get(GenericObject.class, "TriggerTime");
                tupleMap.put("evTime/D", triggT.get(0).getDoubleVal(0));
                tupleMap.put("evTx/I", (double) triggT.get(0).getIntVal(0));
                tupleMap.put("evTy/I", (double) triggT.get(0).getIntVal(1));
            }
        }
    }
    
    protected boolean setupCollections(EventHeader event) {
        String unconstrainedV0CandidatesColName = "UnconstrainedV0Candidates_bad";
        String unconstrainedV0VerticesColName = "UnconstrainedV0Vertices_bad";
        String beamspotConstrainedV0CandidatesColName = "BeamspotConstrainedV0Candidates_bad";
        String targetConstrainedV0CandidatesColName = "TargetConstrainedV0Candidates_bad";
        String unconstrainedV0CandidatesTruthColName = "UnconstrainedV0Candidates_truth";
        String unconstrainedV0VerticesTruthColName = "UnconstrainedV0Vertices_truth";
        String beamspotConstrainedV0CandidatesTruthColName = "BeamspotConstrainedV0Candidates_truth";
        String targetConstrainedV0CandidatesTruthColName = "TargetConstrainedV0Candidates_truth";
        
        /*  make sure everything is there */
        if (!event.hasCollection(ReconstructedParticle.class, unconstrainedV0CandidatesColName)) {
            return false;
        }
        if (!event.hasCollection(ReconstructedParticle.class, beamspotConstrainedV0CandidatesColName)) {
            beamspotConstrainedV0CandidatesColName = null;
        }
        if (!event.hasCollection(ReconstructedParticle.class, targetConstrainedV0CandidatesColName)) {
            targetConstrainedV0CandidatesColName = null;
        }
        
        unConstrainedV0List = event.get(ReconstructedParticle.class, unconstrainedV0CandidatesColName);

        if (unconstrainedV0VerticesColName != null) {
            unConstrainedV0VerticeList = event.get(Vertex.class, unconstrainedV0VerticesColName);
            cand2vert  = correlateCandidates(unConstrainedV0List, unConstrainedV0VerticeList);
        }
        if (beamspotConstrainedV0CandidatesColName != null) {
            bsConstrainedV0List = event.get(ReconstructedParticle.class, beamspotConstrainedV0CandidatesColName);
            unc2bsc = correlateCollections(unConstrainedV0List, bsConstrainedV0List);
        }
        if (targetConstrainedV0CandidatesColName != null) {
            tarConstrainedV0List = event.get(ReconstructedParticle.class, targetConstrainedV0CandidatesColName);
            unc2tar = correlateCollections(unConstrainedV0List, tarConstrainedV0List);
        }
        
        
        List<LCRelation> trackTruthToMCParticleRelations = event.get(LCRelation.class,trackToMCParticleRelationsName);
        List<LCRelation> trackBadToMCParticleRelations = event.get(LCRelation.class,trackBadToTruthMatchRelationsOutputColName);
        List<LCRelation> trackToMCParticleRelations = event.get(LCRelation.class,trackToTruthMatchRelationsOutputColName);
        unConstrainedV0TruthList = event.get(ReconstructedParticle.class, unconstrainedV0CandidatesTruthColName);
        
        if (unconstrainedV0VerticesTruthColName != null) {
            unConstrainedV0VerticeTruthList = event.get(Vertex.class, unconstrainedV0VerticesTruthColName);
            cand2vertTruth  = correlateCandidates(unConstrainedV0TruthList, unConstrainedV0VerticeTruthList);
        }
        if (beamspotConstrainedV0CandidatesTruthColName != null) {
            bsConstrainedV0TruthList = event.get(ReconstructedParticle.class, beamspotConstrainedV0CandidatesTruthColName);
            unc2bscTruth = correlateCollections(unConstrainedV0TruthList, bsConstrainedV0TruthList);
        }
        if (targetConstrainedV0CandidatesTruthColName != null) {
            tarConstrainedV0TruthList = event.get(ReconstructedParticle.class, targetConstrainedV0CandidatesTruthColName);
            unc2tarTruth = correlateCollections(unConstrainedV0TruthList, tarConstrainedV0TruthList);
        }
        
        unc2Truth = correlateBadAndTruthCollections(unConstrainedV0List,unConstrainedV0TruthList,trackToMCParticleRelations,trackBadToMCParticleRelations,trackTruthToMCParticleRelations);
        
        triggerData = checkTrigger(event);
        if (triggerData == null)
            return false;
        

        return true;
    }

    protected void addVertexVariables() {
        addVertexVariables("");
    }
    
    private void addVertexVariables(String flag) {
        addVertexVariables(true, true, flag);
    }
    
    private void addVertexVariables(boolean doBsc, boolean doTar, String flag) {
        String[] newVars = new String[] {flag+"uncPX/D", flag+"uncPY/D", flag+"uncPZ/D", flag+"uncP/D", flag+"uncVX/D", flag+"uncVY/D", flag+"uncVZ/D",
                flag+"uncChisq/D", flag+"uncM/D", flag+"uncMErr/D", flag+"uncCovXX/D", flag+"uncCovXY/D", flag+"uncCovXZ/D", flag+"uncCovYX/D", flag+"uncCovYY/D",
                flag+"uncCovYZ/D", flag+"uncCovZX/D", flag+"uncCovZY/D", flag+"uncCovZZ/D", flag+"uncElePX/D", flag+"uncElePY/D", flag+"uncElePZ/D",
                flag+"uncPosPX/D", flag+"uncPosPY/D", flag+"uncPosPZ/D", flag+"uncEleP/D", flag+"uncPosP/D", flag+"uncEleWtP/D", flag+"uncPosWtP/D", flag+"uncWtM/D",
                flag+"uncMom/D",flag+"uncMomX/D",flag+"uncMomY/D",flag+"uncMomZ/D",flag+"uncMomErr/D",flag+"uncMomXErr/D",flag+"uncMomYErr/D",flag+"uncMomZErr/D",
                flag+"uncTargProjX/D",flag+"uncTargProjY/D",flag+"uncTargProjXErr/D",flag+"uncTargProjYErr/D",flag+"uncPosX/D",flag+"uncPosY/D",flag+"uncPosZ/D"};
        tupleVariables.addAll(Arrays.asList(newVars));
        if (doBsc) {
            String[] newVars2 = new String[] {flag+"bscPX/D", flag+"bscPY/D", flag+"bscPZ/D", flag+"bscP/D", flag+"bscVX/D", flag+"bscVY/D", flag+"bscVZ/D",
                    flag+"bscChisq/D", flag+"bscM/D", flag+"bscElePX/D", flag+"bscElePY/D", flag+"bscElePZ/D", flag+"bscPosPX/D", flag+"bscPosPY/D", flag+"bscPosPZ/D", flag+"bscEleP/D", flag+"bscPosP/D", 
                    flag+"bscEleWtP/D", flag+"bscPosWtP/D", flag+"bscWtM/D"};
            tupleVariables.addAll(Arrays.asList(newVars2));
        }
        if (doTar) {
            String[] newVars3 = new String[] {flag+"tarPX/D", flag+"tarPY/D", flag+"tarPZ/D", flag+"tarP/D", flag+"tarVX/D", flag+"tarVY/D", flag+"tarVZ/D",
                    flag+"tarChisq/D", flag+"tarM/D", flag+"tarElePX/D", flag+"tarElePY/D", flag+"tarElePZ/D", flag+"tarPosPX/D", flag+"tarPosPY/D", flag+"tarPosPZ/D", flag+"tarEleP/D", flag+"tarPosP/D", flag+"tarEleWtP/D", flag+"tarPosWtP/D", flag+"tarWtM/D"};
            tupleVariables.addAll(Arrays.asList(newVars3));
        }
    }
    
    protected Map<ReconstructedParticle, ReconstructedParticle> correlateCollections(
            List<ReconstructedParticle> listFrom, List<ReconstructedParticle> listTo) {
        Map<ReconstructedParticle, ReconstructedParticle> map = new HashMap();
        
        for(ReconstructedParticle p1 : listFrom){
            for(ReconstructedParticle p2 : listTo){
                if(p1.getParticles().get(0).getTracks().get(0) == p2.getParticles().get(0).getTracks().get(0)
                        && p1.getParticles().get(1).getTracks().get(0) == p2.getParticles().get(1).getTracks().get(0))
                    map.put(p1, p2);
            }
        }
        
        return map;
    }
    
    protected Map<ReconstructedParticle, ReconstructedParticle> correlateBadAndTruthCollections(
            List<ReconstructedParticle> listFrom, List<ReconstructedParticle> listTo, List<LCRelation> trackRel, List<LCRelation> badRel, List<LCRelation> truthRel) {
        Map<ReconstructedParticle, ReconstructedParticle> map = new HashMap();
        
        for(ReconstructedParticle p1 : listFrom){
            Track track1_p1 = p1.getParticles().get(0).getTracks().get(0);
            Track track2_p1 = p1.getParticles().get(1).getTracks().get(0);
            MCParticle p1_p1 = null;
            MCParticle p2_p1 = null;
            for (LCRelation relation : trackRel){
                if (relation != null && relation.getFrom() != null && relation.getTo() != null){
                    if(relation.getFrom().equals(track1_p1)){
                        p1_p1 = (MCParticle) relation.getTo();
                    }
                    if(relation.getFrom().equals(track2_p1))
                        p2_p1 = (MCParticle) relation.getTo();
                }
            }
            if(p1_p1 == null && p2_p1 == null)
                System.out.println("Both BAD vertex tracks are NOT matched to MCParticle");
            for(ReconstructedParticle p2 : listTo){
                Track track1_p2 = p2.getParticles().get(0).getTracks().get(0);
                Track track2_p2 = p2.getParticles().get(1).getTracks().get(0);
                MCParticle p1_p2 = null;
                MCParticle p2_p2 = null;
                for (LCRelation relation : truthRel){
                    if (relation != null && relation.getFrom() != null && relation.getTo() != null){
                        if(relation.getFrom().equals(track1_p2))
                            p1_p2 = (MCParticle) relation.getTo();
                        if(relation.getFrom().equals(track2_p2))
                            p2_p2 = (MCParticle) relation.getTo();
                    }
                }
                if(p1_p2 == null && p2_p2 == null)
                    System.out.println("Both TRUTH vertex tracks are NOT matched to MCParticle");
                for (LCRelation relation : trackRel){
                    if (relation != null && relation.getFrom() != null && relation.getTo() != null){
                        if(relation.getFrom().equals(track1_p2) && p1_p2 == null)
                            p1_p2 = (MCParticle) relation.getTo();
                        if(relation.getFrom().equals(track2_p2)  && p2_p2 == null)
                            p2_p2 = (MCParticle) relation.getTo();
                    }
                }
                if(p1_p1 == null || p2_p1 == null || p1_p2 == null || p2_p2 == null){
                    continue;
                }
                boolean match1 = p1_p1.equals(p1_p2) && p2_p1.equals(p2_p2);
                boolean match2 = p1_p1.equals(p2_p2) && p2_p1.equals(p1_p2);
                if(match1 || match2){
                    map.put(p1, p2);
                }
            }
        }
        
        return map;
    }
    
    protected Map<ReconstructedParticle, BilliorVertex> correlateCandidates(List<ReconstructedParticle> listFrom, List<Vertex> listTo) {
        Map<ReconstructedParticle, BilliorVertex> map = new HashMap();
        
        for(ReconstructedParticle p1 : listFrom){
            for(Vertex p2 : listTo){
                if(p2.getAssociatedParticle().equals(p1)){
                    map.put(p1, new BilliorVertex(p2));
                }
            }
        }      
        return map;
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
    
    protected void fillMCParticleVariables(String prefix, MCParticle particle) {
        // System.out.format("%d %x\n", particle.getGeneratorStatus(), particle.getSimulatorStatus().getValue());
        Hep3Vector start = VecOp.mult(beamAxisRotation, particle.getOrigin());
        Hep3Vector end;
        MCParticle parent;
        try {
            end = VecOp.mult(beamAxisRotation, particle.getEndPoint());
        } catch (RuntimeException e) {
            end = null;
        }
        
        try {
            parent = particle.getParents().get(0);
        } catch (RuntimeException e) {
            parent = null;
        }

        Hep3Vector p = VecOp.mult(beamAxisRotation, particle.getMomentum());

        tupleMap.put(prefix + "StartX/D", start.x());
        tupleMap.put(prefix + "StartY/D", start.y());
        tupleMap.put(prefix + "StartZ/D", start.z());
        if (end != null) {
            tupleMap.put(prefix + "EndX/D", end.x());
            tupleMap.put(prefix + "EndY/D", end.y());
            tupleMap.put(prefix + "EndZ/D", end.z());
        }
        tupleMap.put(prefix + "PX/D", p.x());
        tupleMap.put(prefix + "PY/D", p.y());
        tupleMap.put(prefix + "PZ/D", p.z());
        tupleMap.put(prefix + "P/D", p.magnitude());
        tupleMap.put(prefix + "M/D", particle.getMass());
        tupleMap.put(prefix + "E/D", particle.getEnergy());
        tupleMap.put(prefix + "pdgid/I", (double) particle.getPDGID());
        if(parent != null){
            tupleMap.put(prefix + "parentID/I", (double) parent.getPDGID());
        }
    }
    
    protected void addFullTruthVertexVariables() {
        addMCParticleVariables("ele");
        addMCParticleVariables("pos");
        addEcalTruthVariables("ele");
        addEcalTruthVariables("pos");
        addSVTTruthVariables("ele");
        addSVTTruthVariables("pos");
    }
    
    protected void addMCSVTVariables(String prefix, boolean inactive) {
        String[] newVars = null;
        if(!inactive)
            newVars = new String[] {"svthitX/D","svthitY/D","svthitZ/D",
                "svthitPx/D","svthitPy/D","svthitPz/D","thetaX/D","thetaY/D","residualX/D","residualY/D",
                "NTruthParticles/I","IsGoodTruthHit/I"};
        else{
            newVars = new String[] {"svthitX/D","svthitY/D","svthitZ/D",
                "svthitPx/D","svthitPy/D","svthitPz/D","thetaX/D","thetaY/D","residualX/D","residualY/D"};
        }
        for (int i = 0; i < newVars.length; i++) {
            newVars[i] = prefix + newVars[i];
        }
        tupleVariables.addAll(Arrays.asList(newVars));
    }
    
    protected void addEcalTruthVariables(String prefix){
        for(int i = 0; i < nEcalHit; i++){
            String hit = Integer.toString(i);
            addMCEcalVariables(prefix+"Hit"+hit);
        }
    }

    protected void addSVTTruthVariables(String prefix){    
        for(int i = 0; i < nLay*2; i++){
            if(i + 1 > nTrackingLayers*2)
                break;
            String layer = Integer.toString(i+1);
            addMCSVTVariables(prefix+"L"+layer+"t",false);
            addMCSVTVariables(prefix+"L"+layer+"b",false);
            addMCSVTVariables(prefix+"L"+layer+"tIn",true);
            addMCSVTVariables(prefix+"L"+layer+"bIn",true);
        }
    }
    
    protected void addMCParticleVariables(String prefix) {
        String[] newVars = new String[] {"StartX/D", "StartY/D", "StartZ/D", "EndX/D", "EndY/D", "EndZ/D", "PX/D","PY/D", "PZ/D", 
                "P/D", "M/D", "E/D","pdgid/I","parentID/I","HasTruthMatch/I","NTruthHits/I","NGoodTruthHits/I","NBadTruthHits/I",
                "Purity/D"};

        for (int i = 0; i < newVars.length; i++) {
            newVars[i] = prefix + newVars[i];
        }
        tupleVariables.addAll(Arrays.asList(newVars));
    }
    
    protected void addMCEcalVariables(String prefix) {
        String[] newVars = new String[]{
                "ecalhitIx/I","ecalhitIy/I","ecalhitX/D","ecalhitY/D","ecalhitZ/D",
                "ecalhitEnergy/D"};
        for (int i = 0; i < newVars.length; i++) {
            newVars[i] = prefix + newVars[i];
        }
        tupleVariables.addAll(Arrays.asList(newVars));
    }
    
    public static RelationalTable getKinkDataToTrackTable(EventHeader event, String KinkToGBLCollection) {
        RelationalTable kinkDataToTrack = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY,
                RelationalTable.Weighting.UNWEIGHTED);
        if (event.hasCollection(LCRelation.class, KinkToGBLCollection)) {
            List<LCRelation> relations = event.get(LCRelation.class, KinkToGBLCollection);
            for (LCRelation relation : relations) {
                if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                    kinkDataToTrack.add(relation.getFrom(), relation.getTo());
                }
            }
        }
        return kinkDataToTrack;
    }

    public static GenericObject getKinkData(EventHeader event, Track track, String KinkToGBLCollection) {
        return (GenericObject) getKinkDataToTrackTable(event,KinkToGBLCollection).from(track);
    }
    
    @Override
    boolean passesCuts() {
        return true;
    }
}
