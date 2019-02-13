package org.hps.analysis.tuple;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.recon.particle.ReconParticleDriver;
import org.hps.recon.tracking.TrackType;
import org.hps.recon.vertexing.BilliorVertex;
import org.hps.record.triggerbank.TIData;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
//import org.lcsim.event.TrackState;
import org.lcsim.event.Vertex;

public class RefitTrackTruthTupleDriver extends TupleMaker {
    
    private final String trackColName = "GBLTracks";
    private final String badTrackColName = "GBLTracks_bad";
    private final String simhitOutputColName = "TrackerHits_truth";
    private final String truthMatchOutputColName = "MCFullDetectorTruth";
    private final String trackToTruthMatchRelationsOutputColName = "TrackBadToMCParticleRelations";
    
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
    
    protected void setupVariables() {
        tupleVariables.clear();
        addEventVariables();
        addVertexVariables("Bad");
        addVertexVariables("Truth");
        //addParticleVariables("ele");
        //addParticleVariables("pos");
        addParticleVariables("eleBad");
        addParticleVariables("posBad");
        addParticleVariables("eleTruth");
        addParticleVariables("posTruth");
        //TridentFullTupleDriver.addFullTruthVertexVariables();
        addTruthRefitVariables();

        String[] newVars = new String[]{"minPositiveIso/D", "minNegativeIso/D", "minIso/D"};
        tupleVariables.addAll(Arrays.asList(newVars));

    }

    @Override
    public void process(EventHeader event) {
        setupCollections(event);
        addTruthRefitVariables();
        //unConstrainedV0List = event.get(ReconstructedParticle.class,"UnconstrainedV0Candidates_bad");
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
        String[] newVars = new String[] {"nTracksBad/I","nTracksTruth/I","nTracksBadEvent/I","nTracksTruthEvent/I"};
        tupleVariables.addAll(Arrays.asList(newVars));
    }
    
    private void fillTruthRefitVariables(EventHeader event, ReconstructedParticle uncV0) {
        List<Track> tracksBad = event.get(Track.class,badTrackColName);
        List<Track> tracksTruth = event.get(Track.class,"GBLTracks_truth");
        /*List<ReconstructedParticle> uncBad = event.get(ReconstructedParticle.class,"UnconstrainedV0Candidates_bad");
        List<ReconstructedParticle> uncBad2 = event.get(ReconstructedParticle.class,"UnconstrainedV0Candidates_bad2");
        List<ReconstructedParticle> uncTruth = event.get(ReconstructedParticle.class,"UnconstrainedV0Candidates_truth");
        List<ReconstructedParticle> uncTruth2 = event.get(ReconstructedParticle.class,"UnconstrainedV0Candidates_truth2");
        List<ReconstructedParticle> bscBad = event.get(ReconstructedParticle.class,"BeamspotConstrainedV0Candidates_bad");
        List<ReconstructedParticle> bscBad2 = event.get(ReconstructedParticle.class,"BeamspotConstrainedV0Candidates_bad2");
        List<ReconstructedParticle> bscTruth = event.get(ReconstructedParticle.class,"BeamspotConstrainedV0Candidates_truth");
        List<ReconstructedParticle> bscTruth2 = event.get(ReconstructedParticle.class,"BeamspotConstrainedV0Candidates_truth2");*/
        
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
        //fillFullVertexTruth(event,electron.getTracks().get(0),positron.getTracks().get(0));
        
        minPositiveIso = Math.min(tupleMap.get("eleBadMinPositiveIso/D"), tupleMap.get("posBadMinPositiveIso/D"));
        minNegativeIso = Math.min(Math.abs(tupleMap.get("eleBadMinNegativeIso/D")), Math.abs(tupleMap.get("posBadMinNegativeIso/D")));
        
        double minIso = Math.min(minPositiveIso, minNegativeIso);
        tupleMap.put("minPositiveIso/D", minPositiveIso);
        tupleMap.put("minNegativeIso/D", minNegativeIso);
        tupleMap.put("minIso/D", minIso);
        
        fillVertexVariables("Badunc", uncV0, false);
        
        if (unc2Truth != null) {
            ReconstructedParticle temp = unc2Truth.get(uncV0);
            if (temp != null){
                ReconstructedParticle electronTruth = temp.getParticles().get(ReconParticleDriver.ELECTRON);
                ReconstructedParticle positronTruth = temp.getParticles().get(ReconParticleDriver.POSITRON);
            
                fillParticleVariables(event, electronTruth, "eleTruth");
                fillParticleVariables(event, positronTruth, "posTruth");
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

            // BaseCluster c1 = (BaseCluster) ecalClusters.get(0);

            // System.out.println("Cluster pid:\t"+((BaseCluster) c1).getParticleId());

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
        
        unc2Truth = correlateBadAndTruthCollections(unConstrainedV0List,unConstrainedV0TruthList);
        
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
            List<ReconstructedParticle> listFrom, List<ReconstructedParticle> listTo) {
        Map<ReconstructedParticle, ReconstructedParticle> map = new HashMap();
        
        for(ReconstructedParticle p1 : listFrom){
            //p1.getParticles().get(0).getTracks().get(0)
            //p1.getParticles().get(1).getTracks().get(0)
            for(ReconstructedParticle p2 : listTo){
                map.put(p1, p2);
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
    
    @Override
    boolean passesCuts() {
        return true;
    }
}
