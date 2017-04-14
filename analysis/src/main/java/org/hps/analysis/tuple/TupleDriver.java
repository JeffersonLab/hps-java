package org.hps.analysis.tuple;

import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.HepLorentzVector;
import hep.physics.vec.VecOp;

import hep.physics.matrix.Matrix;
import hep.physics.matrix.MatrixOp;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hps.analysis.ecal.MassCalculator;
import org.hps.conditions.beam.BeamEnergy;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.particle.HpsReconParticleDriver;
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.gbl.GBLKinkData;
import org.hps.recon.vertexing.BilliorTrack;
import org.hps.recon.vertexing.BilliorVertex;
import org.hps.recon.vertexing.BilliorVertexer;
import org.hps.record.triggerbank.TIData;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.TrackerHit;
import java.util.Collection;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.RawTrackerHit;

/**
 * sort of an interface for DQM analysis drivers creates the DQM database
 * manager, checks whether row exists in db etc
 *
 * @author mgraham on Apr 15, 2014 update mgraham on May 15, 2014 to include
 * calculateEndOfRunQuantities & printDQMData i.e. useful methods
 */
public abstract class TupleDriver extends Driver {

    protected boolean debug = false;

    protected String tupleFile = null;
    protected PrintWriter tupleWriter = null;
    protected final List<String> tupleVariables = new ArrayList<String>();
    protected final Map<String, Double> tupleMap = new HashMap<String, Double>();
    protected boolean cutTuple = false;

    protected String triggerType = "all";//allowed types are "" (blank) or "all", singles0, singles1, pairs0,pairs1
    public boolean isGBL = false;
    private boolean applyBeamRotation = true;

    private final String finalStateParticlesColName = "FinalStateParticles";
    protected double bfield;
    private final double[] beamSize = {0.001, 0.130, 0.050}; //rough estimate from harp scans during engineering run production running
    private final double[] beamPos = {0.0, 0.0, 0.0};
    private final double[] vzcBeamSize = {0.001, 100, 100};
    private final double[] topTrackCorrection = {0, 0, 0, 0, 0};
    private final double[] botTrackCorrection = {0, 0, 0, 0, 0};
    private final double[] topPos = {45.5,92.0,192.0};
    private final double[] botPos = {54.5,107.5,207.5};
    protected final BasicHep3Matrix beamAxisRotation = BasicHep3Matrix.identity();
    protected double ebeam = Double.NaN;
    private int nLay = 6;

    public void setNLay(int nLay) {
        this.nLay = nLay;
    }
    
    public void setApplyBeamRotation(boolean applyBeamRotation) {
        this.applyBeamRotation = applyBeamRotation;
    }

    public void setEbeam(double ebeam) {
        this.ebeam = ebeam;
    }

    public void setBeamSizeX(double beamSizeX) {
        this.beamSize[1] = beamSizeX;
    }

    public void setBeamSizeY(double beamSizeY) {
        this.beamSize[2] = beamSizeY;
    }

    public void setBeamPosX(double beamPosX) {
        this.beamPos[1] = beamPosX;
    }

    public void setBeamPosY(double beamPosY) {
        this.beamPos[2] = beamPosY;
    }

    public void setBeamPosZ(double beamPosZ) {
        this.beamPos[0] = beamPosZ;
    }

    public void setTopDZ0(double topDZ0) {
        topTrackCorrection[HelicalTrackFit.z0Index] = topDZ0;
    }

    public void setTopDLambda(double topDLambda) {
        topTrackCorrection[HelicalTrackFit.slopeIndex] = topDLambda;
    }

    public void setTopDD0(double topDD0) {
        topTrackCorrection[HelicalTrackFit.dcaIndex] = topDD0;
    }

    public void setTopDPhi(double topDPhi) {
        topTrackCorrection[HelicalTrackFit.phi0Index] = topDPhi;
    }

    public void setTopDOmega(double topDOmega) {
        topTrackCorrection[HelicalTrackFit.curvatureIndex] = topDOmega;
    }

    public void setBotDZ0(double botDZ0) {
        botTrackCorrection[HelicalTrackFit.z0Index] = botDZ0;
    }

    public void setBotDLambda(double botDLambda) {
        botTrackCorrection[HelicalTrackFit.slopeIndex] = botDLambda;
    }

    public void setBotDD0(double botDD0) {
        botTrackCorrection[HelicalTrackFit.dcaIndex] = botDD0;
    }

    public void setBotDPhi(double botDPhi) {
        botTrackCorrection[HelicalTrackFit.phi0Index] = botDPhi;
    }

    public void setBotDOmega(double botDOmega) {
        botTrackCorrection[HelicalTrackFit.curvatureIndex] = botDOmega;
    }

    public void setTriggerType(String type) {
        this.triggerType = type;
    }

    public void setIsGBL(boolean isgbl) {
        this.isGBL = isgbl;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    abstract protected void setupVariables();

    @Override
    protected void detectorChanged(Detector detector) {
        if (applyBeamRotation) {
            beamAxisRotation.setActiveEuler(Math.PI / 2, -0.0305, -Math.PI / 2);
        }
        bfield = TrackUtils.getBField(detector).magnitude();

        if (Double.isNaN(ebeam)) {
            try {
                BeamEnergy.BeamEnergyCollection beamEnergyCollection
                        = this.getConditionsManager().getCachedConditions(BeamEnergy.BeamEnergyCollection.class, "beam_energies").getCachedData();
                ebeam = beamEnergyCollection.get(0).getBeamEnergy();
            } catch (Exception e) {
            }
        }
        setupVariables();
        if (tupleFile != null) {
            try {
                tupleWriter = new PrintWriter(tupleFile);
            } catch (FileNotFoundException e) {
                tupleWriter = null;
            }
            tupleWriter.println(StringUtils.join(tupleVariables, ":"));
        }
    }

    @Override
    public void endOfData() {
        if (tupleWriter != null) {
            tupleWriter.close();
        }
    }

    protected boolean matchTriggerType(TIData triggerData) {
        if (triggerType.contentEquals("") || triggerType.contentEquals("all")) {
            return true;
        }
        if (triggerData.isSingle0Trigger() && triggerType.contentEquals("singles0")) {
            return true;
        }
        if (triggerData.isSingle1Trigger() && triggerType.contentEquals("singles1")) {
            return true;
        }
        if (triggerData.isPair0Trigger() && triggerType.contentEquals("pairs0")) {
            return true;
        }
        if (triggerData.isPair1Trigger() && triggerType.contentEquals("pairs1")) {
            return true;
        }
        return false;
    }

    protected void writeTuple() {
        for (String variable : tupleVariables) {
            Double value = tupleMap.get(variable);
            if (value == null || Double.isNaN(value)) {
                value = -9999.0;
            }
            if (variable.endsWith("/I") || variable.endsWith("/B")) {
                tupleWriter.format("%d\t", Math.round(value));
            } else {
                tupleWriter.format("%g\t", value);
            }
        }
        tupleWriter.println();
//        tupleMap.clear();
    }

    public void setTupleFile(String tupleFile) {
        this.tupleFile = tupleFile;
//        for (String variable : tupleVariables) {
//            tupleWriter.format("%s:", variable);
//        }
//        tupleWriter.println();
    }

    /**
     * apply loose cuts to the tuple (cuts to be defined in the specific DQM
     * driver)
     *
     * @param cutTuple
     */
    public void setCutTuple(boolean cutTuple) {
        this.cutTuple = cutTuple;
    }

    protected void addEventVariables() {
        String[] newVars = new String[]{"run/I", "event/I",
            "nTrk/I", "nPos/I","nCl/I",
            "isCalib/B", "isPulser/B", "isSingle0/B", "isSingle1/B", "isPair0/B", "isPair1/B","evTime/D",
            "evTx/I","evTy/I","rfT1/D","rfT2/D",
            "nEcalHits/I", "nSVTHits/I", "nEcalCl/I", "nEcalClele/I","nEcalClpos/I","nEcalClpho/I","nEcalClEleSide/I","nEcalClPosSide/I",
            "nSVTHitsL1/I","nSVTHitsL2/I","nSVTHitsL3/I","nSVTHitsL4/I","nSVTHitsL5/I","nSVTHitsL6/I",
            "nSVTHitsL1b/I","nSVTHitsL2b/I","nSVTHitsL3b/I","nSVTHitsL4b/I","nSVTHitsL5b/I","nSVTHitsL6b/I",
            "topL1HitX/D","topL1HitY/D","botL1HitX/D","botL1HitY/D"};
        tupleVariables.addAll(Arrays.asList(newVars));
    }

    protected void addVertexVariables() {
        String[] newVars = new String[]{"uncPX/D", "uncPY/D", "uncPZ/D", "uncP/D",
            "uncVX/D", "uncVY/D", "uncVZ/D", "uncChisq/D", "uncM/D",
            "bscPX/D", "bscPY/D", "bscPZ/D", "bscP/D",
            "bscVX/D", "bscVY/D", "bscVZ/D", "bscChisq/D", "bscM/D",
            "tarPX/D", "tarPY/D", "tarPZ/D", "tarP/D",
            "tarVX/D", "tarVY/D", "tarVZ/D", "tarChisq/D", "tarM/D",
            "vzcPX/D", "vzcPY/D", "vzcPZ/D", "vzcP/D",
            "vzcVX/D", "vzcVY/D", "vzcVZ/D", "vzcChisq/D", "vzcM/D",
            "uncCovXX/D","uncCovXY/D","uncCovXZ/D",
            "uncCovYX/D","uncCovYY/D","uncCovYZ/D",
            "uncCovZX/D","uncCovZY/D","uncCovZZ/D",
            "uncElePX/D", "uncElePY/D", "uncElePZ/D", "uncPosPX/D", "uncPosPY/D", "uncPosPZ/D", "uncEleP/D", "uncPosP/D",
            "bscElePX/D", "bscElePY/D", "bscElePZ/D", "bscPosPX/D", "bscPosPY/D", "bscPosPZ/D", "bscEleP/D", "bscPosP/D",
            "tarElePX/D", "tarElePY/D", "tarElePZ/D", "tarPosPX/D", "tarPosPY/D", "tarPosPZ/D", "tarEleP/D", "tarPosP/D",
            "vzcElePX/D", "vzcElePY/D", "vzcElePZ/D", "vzcPosPX/D", "vzcPosPY/D", "vzcPosPZ/D", "vzcEleP/D", "vzcPosP/D",
            "uncEleWtP/D", "uncPosWtP/D", "bscEleWtP/D", "bscPosWtP/D", "tarEleWtP/D", "tarPosWtP/D", 
            "vzcEleWtP/D", "vzcPosWtP/D",
            "uncWtM/D", "bscWtM/D", "tarWtM/D", "vzcWtM/D"};
        tupleVariables.addAll(Arrays.asList(newVars));
    }

    protected void addParticleVariables(String prefix) {
        String[] newVars = new String[]{"PX/D", "PY/D", "PZ/D", "P/D",
            "TrkChisq/D", "TrkHits/I", "TrkType/I", "TrkT/D", "TrkTsd/D",
            "TrkZ0/D", "TrkLambda/D", "TrkD0/D", "TrkPhi/D", "TrkOmega/D",
            "TrkEcalX/D", "TrkEcalY/D",
            "HasL1/B", "HasL2/B", "HasL3/B","HasL4/B", "HasL5/B", "HasL6/B",
            "FirstHitX/D", "FirstHitY/D",
            "FirstHitT1/D", "FirstHitT2/D",
            "FirstHitDEDx1/D", "FirstHitDEDx2/D",
            "FirstClusterSize1/I", "FirstClusterSize2/I",
            "NHitsShared/I","HitsSharedP/D",
            "LambdaKink1/D", "LambdaKink2/D", "LambdaKink3/D",
            "PhiKink1/D", "PhiKink2/D", "PhiKink3/D",
            "IsoStereo/D", "IsoAxial/D",
            "MinPositiveIso/D", "MinNegativeIso/D",
            "TrkExtrpXL0/D", "TrkExtrpYL0/D",
            "TrkExtrpXL1/D", "TrkExtrpYL1/D",
            "TrkExtrpXL2/D", "TrkExtrpYL2/D",
            "RawMaxAmplL1/D", "RawT0L1/D", "RawChisqL1/D","RawTDiffL1/D",
            "RawMaxAmplL2/D", "RawT0L2/D", "RawChisqL2/D","RawTDiffL2/D",
            "RawMaxAmplL3/D", "RawT0L3/D", "RawChisqL3/D","RawTDiffL3/D",
            "NTrackHits/I", "HitsSharedP/D","MaxHitsShared/I",
            "MinNegativeIsoL2/D","MinPositiveIsoL2/D","IsoStereoL2/D","IsoAxialL2/D",
            "SharedTrkChisq/D","SharedTrkEcalX/D","SharedTrkEcalY/D",
            "MatchChisq/D", "ClT/D", "ClE/D", "ClSeedE/D", "ClX/D", "ClY/D", "ClZ/D", "ClHits/I", "Clix/I","Cliy/I"};

        for (int i = 0; i < newVars.length; i++) {
            newVars[i] = prefix + newVars[i];
        }
        tupleVariables.addAll(Arrays.asList(newVars));
    }

    protected void fillEventVariables(EventHeader event, TIData triggerData) {
        tupleMap.put("run/I", (double) event.getRunNumber());
        tupleMap.put("event/I", (double) event.getEventNumber());
        List<ReconstructedParticle> fspList = event.get(ReconstructedParticle.class, finalStateParticlesColName);
        int npos = 0;
        int ntrk = 0;
        int ncl = 0;
        for (ReconstructedParticle fsp : fspList) {
            if (isGBL != TrackType.isGBL(fsp.getType())) {
                continue;
            }
            /*if (fsp.getClusters().isEmpty()){
             continue;
             }*/
            if (fsp.getCharge() != 0) {
                ntrk++;
            }
            if (fsp.getCharge() > 0) {
                npos++;
            }
            ncl = fsp.getClusters().size();
        }

        tupleMap.put("nTrk/I", (double) ntrk);
        tupleMap.put("nPos/I", (double) npos);
        tupleMap.put("nCl/I", (double) ncl);

        if (triggerData != null) {
            tupleMap.put("isCalib/B", triggerData.isCalibTrigger() ? 1.0 : 0.0);
            tupleMap.put("isPulser/B", triggerData.isPulserTrigger() ? 1.0 : 0.0);
            tupleMap.put("isSingle0/B", triggerData.isSingle0Trigger() ? 1.0 : 0.0);
            tupleMap.put("isSingle1/B", triggerData.isSingle1Trigger() ? 1.0 : 0.0);
            tupleMap.put("isPair0/B", triggerData.isPair0Trigger() ? 1.0 : 0.0);
            tupleMap.put("isPair1/B", triggerData.isPair1Trigger() ? 1.0 : 0.0);
        }
       
        if (event.hasCollection(GenericObject.class, "TriggerTime")){
            if (event.get(GenericObject.class, "TriggerTime") != null){
                List <GenericObject> triggT = event.get(GenericObject.class, "TriggerTime");
                tupleMap.put("evTime/D", triggT.get(0).getDoubleVal(0));
                tupleMap.put("evTx/I", (double) triggT.get(0).getIntVal(0));
                tupleMap.put("evTy/I", (double) triggT.get(0).getIntVal(1));
            }
        }
        if (event.hasCollection(GenericObject.class, "RFHits")) {
            List<GenericObject> rfTimes = event.get(GenericObject.class, "RFHits");
            if (rfTimes.size()>0){
                tupleMap.put("rfT1/D",rfTimes.get(0).getDoubleVal(0));
                tupleMap.put("rfT2/D",rfTimes.get(0).getDoubleVal(1));
            }
        }
        if (event.hasCollection(CalorimeterHit.class,"EcalCalHits")){
            List<CalorimeterHit> ecalHits = event.get(CalorimeterHit.class,"EcalCalHits");
            tupleMap.put("nEcalHits/I", (double) ecalHits.size());   
        }
        
        if (event.hasCollection(Cluster.class,"EcalClustersCorr")){
            List<Cluster> ecalClusters = event.get(Cluster.class,"EcalClustersCorr");
            tupleMap.put("nEcalCl/I", (double) ecalClusters.size());  
            
            int nEle = 0;
            int nPos = 0;
            int nPho = 0;
            int nEleSide = 0;
            int nPosSide = 0;
            
            //BaseCluster c1 = (BaseCluster) ecalClusters.get(0);
            
            //System.out.println("Cluster pid:\t"+((BaseCluster) c1).getParticleId());

            for (Cluster cc : ecalClusters){
                if (cc.getParticleId()==11){
                    nEle++;
                }
                if (cc.getParticleId()==-11){
                    nPos++;
                }
                if (cc.getParticleId()==22){
                    nPho++;
                }
                if (cc.getPosition()[0]<0){
                    nEleSide++;
                }
                if (cc.getPosition()[0]>0){
                    nPosSide++;
                }
            }
            
            tupleMap.put("nEcalClele/I", (double) nEle);
            tupleMap.put("nEcalClpos/I", (double) nPos);
            tupleMap.put("nEcalClpho/I", (double) nPho);
            tupleMap.put("nEcalClEleSide/I", (double) nEleSide);
            tupleMap.put("nEcalClPosSide/I", (double) nPosSide);
            
            
        }
        
        ////////////////////////////////////////////////////////////////////////
        //All of the following is specifically for getting raw tracker hit info 
        ////////////////////////////////////////////////////////////////////////
        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, "SVTRawTrackerHits");       
  
         //Get the list of fitted hits from the event
         List<LCRelation> fittedHits = event.get(LCRelation.class, "SVTFittedRawTrackerHits");
         tupleMap.put("nSVTHits/I", (double) fittedHits.size());

         //Map the fitted hits to their corresponding raw hits
         Map<RawTrackerHit, LCRelation> fittedRawTrackerHitMap = new HashMap<RawTrackerHit, LCRelation>();
         
         for (LCRelation fittedHit : fittedHits) {
             fittedRawTrackerHitMap.put(FittedRawTrackerHit.getRawTrackerHit(fittedHit), fittedHit);
         }
         
         int nL1hits = 0;
         int nL2hits = 0;
         int nL3hits = 0;
         int nL4hits = 0;     
         int nL5hits = 0;     
         int nL6hits = 0; 
         int nL1bhits = 0;
         int nL2bhits = 0;
         int nL3bhits = 0;
         int nL4bhits = 0;     
         int nL5bhits = 0;     
         int nL6bhits = 0; 
         for (RawTrackerHit rHit : rawHits) {  
             
             HpsSiSensor sensor = (HpsSiSensor) rHit.getDetectorElement();
             if (sensor.getLayerNumber()==1){
                 nL1hits++;
             }
             else if (sensor.getLayerNumber()==2){
                 nL1bhits++;
             }
             else if (sensor.getLayerNumber()==3){
                 nL2hits++;
             }
             else if (sensor.getLayerNumber()==4){
                 nL2bhits++;
             }
             else if (sensor.getLayerNumber()==5){
                 nL3hits++;
             }
             else if (sensor.getLayerNumber()==6){
                 nL3bhits++;
             }
             else if (sensor.getLayerNumber()==7){
                 nL4hits++;
             }
             else if (sensor.getLayerNumber()==8){
                 nL4bhits++;
             }
             else if (sensor.getLayerNumber()==9){
                 nL5hits++;
             }
             else if (sensor.getLayerNumber()==10){
                 nL5bhits++;
             }
             else if (sensor.getLayerNumber()==11){
                 nL6hits++;
             }
             else if (sensor.getLayerNumber()==12){
                 nL6bhits++;
             }
         }
         
         tupleMap.put("nSVTHitsL1/I", (double) nL1hits);
         tupleMap.put("nSVTHitsL2/I", (double) nL2hits);
         tupleMap.put("nSVTHitsL3/I", (double) nL3hits);
         tupleMap.put("nSVTHitsL4/I", (double) nL4hits);
         tupleMap.put("nSVTHitsL5/I", (double) nL5hits);
         tupleMap.put("nSVTHitsL6/I", (double) nL6hits);
         tupleMap.put("nSVTHitsL1b/I", (double) nL1bhits);
         tupleMap.put("nSVTHitsL2b/I", (double) nL2bhits);
         tupleMap.put("nSVTHitsL3b/I", (double) nL3bhits);
         tupleMap.put("nSVTHitsL4b/I", (double) nL4bhits);
         tupleMap.put("nSVTHitsL5b/I", (double) nL5bhits);
         tupleMap.put("nSVTHitsL6b/I", (double) nL6bhits);
             
         
         
         double topL1HitX = 9999;
         double topL1HitY = 9999;
         double botL1HitX = 9999;
         double botL1HitY = -9999;

         String stereoHitCollectionName = "RotatedHelicalTrackHits";
         
         // Get the collection of 3D hits from the event. This collection
         // contains all 3D hits in the event and not just those associated
         // with a track.
         List<TrackerHit> hits = event.get(TrackerHit.class, stereoHitCollectionName);
              
         // Loop over the collection of 3D hits in the event and map them to 
         // their corresponding layer.       
         for (TrackerHit hit : hits) {
             // Retrieve the sensor associated with one of the hits.  This will
             // be used to retrieve the layer number
             HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement();

             // Retrieve the layer number by using the sensor
             int layer = (sensor.getLayerNumber() + 1)/2;
          
             // If hit isn't in layer one, skip it. 
             // You can also create another list which contains just layer 1 hits ...
             if (layer != 1) continue;
          
             if (sensor.isTopLayer() && topL1HitY>hit.getPosition()[2]){
                 topL1HitY = hit.getPosition()[2];
                 topL1HitX = hit.getPosition()[1];
                
             }
             if (sensor.isBottomLayer() && botL1HitY<hit.getPosition()[2]){
                 botL1HitY = hit.getPosition()[2];
                 botL1HitX = hit.getPosition()[1];

             }   
             
             // To check if hit is in top or bottom, use the sensor
          //sensor.isTopLayer()
      }
         tupleMap.put("topL1HitX/D", topL1HitX);
         tupleMap.put("topL1HitY/D", topL1HitY);
         tupleMap.put("botL1HitX/D", botL1HitX);
         tupleMap.put("botL1HitY/D", botL1HitY);
             /*
             // Get the hit amplitude
             double amplitude = FittedRawTrackerHit.getAmp(fittedRawTrackerHitMap.get(rHit));
   
             // Get the t0 of the hit
             double t0 = FittedRawTrackerHit.getT0(fittedRawTrackerHitMap.get(rHit));
             GenericObject fitPar = FittedRawTrackerHit.getShapeFitParameters(fittedRawTrackerHitMap.get(rHit));
        
             HpsSiSensor sensor = (HpsSiSensor) rHit.getDetectorElement();
             
             //int febhid = sensor.getFebHybridID();
             //int febid = sensor.getFebID();
             int ln = sensor.getLayerNumber();
             //int mid = sensor.getMillepedeId();
             //int modn = sensor.getModuleNumber();
             //double tshift = sensor.getT0Shift();
             //int id = sensor.getSensorID();
         }
         */  
    }

//    protected TrackState fillParticleVariablesT(EventHeader event, ReconstructedParticle particle, String prefix) {
//        Track track = particle.getTracks().get(0);
//        TrackState trackState = track.getTrackStates().get(0);
//        double[] param = new double[5];
//        for (int i = 0; i < 5; i++) {
//            param[i] = trackState.getParameters()[i] + ((trackState.getTanLambda() > 0) ? topTrackCorrection[i] : botTrackCorrection[i]);
//        }
////            Arrays.
//        TrackState tweakedTrackState = new BaseTrackState(param, trackState.getReferencePoint(), trackState.getCovMatrix(), trackState.getLocation(), bfield);
//        Hep3Vector pRot = VecOp.mult(beamAxisRotation, CoordinateTransformations.transformVectorToDetector(new BasicHep3Vector(tweakedTrackState.getMomentum())));
//
//        Double[] iso = TrackUtils.getIsolations(track, TrackUtils.getHitToStripsTable(event), TrackUtils.getHitToRotatedTable(event));
//        double minPositiveIso = 9999;
//        double minNegativeIso = 9999;
//        double isoStereo = -9999, isoAxial = -9999;
//        for (int i = 0; i < 6; i++) {
//            if (iso[2 * i] != null) {
//                if (pRot.y() < 0) {
//                    isoStereo = iso[2 * i];
//                    isoAxial = iso[2 * i + 1];
//                } else {
//                    isoAxial = iso[2 * i];
//                    isoStereo = iso[2 * i + 1];
//                }
//                for (int j = 2 * i; j < 2 * i + 2; j++) {
//                    if (iso[j] < 100) {
//                        if (iso[j] > 0) {
//                            if (minPositiveIso > 100 || iso[j] < minPositiveIso) {
//                                minPositiveIso = iso[j];
//                            }
//                        } else {
//                            if (minNegativeIso > 100 || iso[j] > minNegativeIso) {
//                                minNegativeIso = iso[j];
//                            }
//                        }
//                    }
//                }
//                break;
//            }
//        }
//        double trkT = TrackUtils.getTrackTime(track, TrackUtils.getHitToStripsTable(event), TrackUtils.getHitToRotatedTable(event));
//        Hep3Vector atEcal = TrackUtils.getTrackPositionAtEcal(tweakedTrackState);
//        Hep3Vector firstHitPosition = VecOp.mult(beamAxisRotation, CoordinateTransformations.transformVectorToDetector(new BasicHep3Vector(track.getTrackerHits().get(0).getPosition())));
//        GenericObject kinks = GBLKinkData.getKinkData(event, track);
//
//        tupleMap.put(prefix + "PX/D", pRot.x());
//        tupleMap.put(prefix + "PY/D", pRot.y());
//        tupleMap.put(prefix + "PZ/D", pRot.z());
//        tupleMap.put(prefix + "P/D", pRot.magnitude());
//        tupleMap.put(prefix + "TrkZ0/D", tweakedTrackState.getZ0());
//        tupleMap.put(prefix + "TrkLambda/D", tweakedTrackState.getTanLambda());
//        tupleMap.put(prefix + "TrkD0/D", tweakedTrackState.getD0());
//        tupleMap.put(prefix + "TrkPhi/D", tweakedTrackState.getPhi());
//        tupleMap.put(prefix + "TrkOmega/D", tweakedTrackState.getOmega());
//        tupleMap.put(prefix + "TrkEcalX/D", atEcal.x());
//        tupleMap.put(prefix + "TrkEcalY/D", atEcal.y());
//        tupleMap.put(prefix + "TrkChisq/D", track.getChi2());
//        tupleMap.put(prefix + "TrkHits/I", (double) track.getTrackerHits().size());
//        tupleMap.put(prefix + "TrkType/I", (double) particle.getType());
//        tupleMap.put(prefix + "TrkT/D", trkT);
//        tupleMap.put(prefix + "HasL1/B", iso[0] != null ? 1.0 : 0.0);
//        tupleMap.put(prefix + "HasL2/B", iso[2] != null ? 1.0 : 0.0);
//        tupleMap.put(prefix + "HasL3/B", iso[4] != null ? 1.0 : 0.0);
//        tupleMap.put(prefix + "FirstHitX/D", firstHitPosition.x());
//        tupleMap.put(prefix + "FirstHitY/D", firstHitPosition.y());
//        tupleMap.put(prefix + "LambdaKink1/D", GBLKinkData.getLambdaKink(kinks, 1));
//        tupleMap.put(prefix + "LambdaKink2/D", GBLKinkData.getLambdaKink(kinks, 2));
//        tupleMap.put(prefix + "LambdaKink3/D", GBLKinkData.getLambdaKink(kinks, 3));
//        tupleMap.put(prefix + "PhiKink1/D", GBLKinkData.getPhiKink(kinks, 1));
//        tupleMap.put(prefix + "PhiKink2/D", GBLKinkData.getPhiKink(kinks, 2));
//        tupleMap.put(prefix + "PhiKink3/D", GBLKinkData.getPhiKink(kinks, 3));
//        tupleMap.put(prefix + "IsoStereo/D", isoStereo);
//        tupleMap.put(prefix + "IsoAxial/D", isoAxial);
//        tupleMap.put(prefix + "MinPositiveIso/D", minPositiveIso);
//        tupleMap.put(prefix + "MinNegativeIso/D", minNegativeIso);
//        tupleMap.put(prefix + "MatchChisq/D", particle.getGoodnessOfPID());
//        if (!particle.getClusters().isEmpty()) {
//            Cluster cluster = particle.getClusters().get(0);
//            tupleMap.put(prefix + "ClT/D", ClusterUtilities.getSeedHitTime(cluster));
//            tupleMap.put(prefix + "ClE/D", cluster.getEnergy());
//            tupleMap.put(prefix + "ClSeedE/D", ClusterUtilities.findSeedHit(cluster).getCorrectedEnergy());
//            tupleMap.put(prefix + "ClX/D", cluster.getPosition()[0]);
//            tupleMap.put(prefix + "ClY/D", cluster.getPosition()[1]);
//            tupleMap.put(prefix + "ClZ/D", cluster.getPosition()[2]);
//            tupleMap.put(prefix + "ClHits/I", (double) cluster.getCalorimeterHits().size());
//        }
//
//        return tweakedTrackState;
//    }
    
    protected TrackState fillParticleVariables(EventHeader event, ReconstructedParticle particle, String prefix) {
        TrackState returnTrackState = null;

        if (!particle.getTracks().isEmpty()) {
            List<Track> allTracks = event.get(Track.class, "GBLTracks");    
            
            //System.out.println("number of gbl tracks"+allTracks.size());
            
            Track track = particle.getTracks().get(0);
            TrackState trackState = track.getTrackStates().get(0);
            Hep3Vector extrapTrackPosL0;
            Hep3Vector extrapTrackPosL1;
            Hep3Vector extrapTrackPosL2;
            if(trackState.getTanLambda() > 0){
                extrapTrackPosL0 = TrackUtils.extrapolateTrack(track,topPos[0]);
                extrapTrackPosL1 = TrackUtils.extrapolateTrack(track,topPos[1]);
                extrapTrackPosL2 = TrackUtils.extrapolateTrack(track,topPos[2]);
            }
            else{
                extrapTrackPosL0 = TrackUtils.extrapolateTrack(track,botPos[0]);
                extrapTrackPosL1 = TrackUtils.extrapolateTrack(track,botPos[1]);
                extrapTrackPosL2 = TrackUtils.extrapolateTrack(track,botPos[2]);
            }
            double[] param = new double[5];
            for (int i = 0; i < 5; i++) {
                param[i] = trackState.getParameters()[i] + ((trackState.getTanLambda() > 0) ? topTrackCorrection[i] : botTrackCorrection[i]);
            }
            //            Arrays.
            TrackState tweakedTrackState = new BaseTrackState(param, trackState.getReferencePoint(), trackState.getCovMatrix(), trackState.getLocation(), bfield);
            Hep3Vector pRot = VecOp.mult(beamAxisRotation, CoordinateTransformations.transformVectorToDetector(new BasicHep3Vector(tweakedTrackState.getMomentum())));

            Double[] iso = TrackUtils.getIsolations(track, TrackUtils.getHitToStripsTable(event), TrackUtils.getHitToRotatedTable(event), nLay);
            double minPositiveIso = 9999;
            double minPositiveIsoL2 = 9999;
            double minNegativeIso = 9999;
            double minNegativeIsoL2 = 9999;
            double isoStereo = -9999, isoAxial = -9999;
            double isoStereoL2 = -9999, isoAxialL2 = -9999;
            for (int i = 0; i < nLay; i++) {
                if (iso[2 * i] != null) {
                    if (pRot.y() < 0) {
                        isoStereo = iso[2 * i];
                        isoAxial = iso[2 * i + 1];
                    } else {
                        isoAxial = iso[2 * i];
                        isoStereo = iso[2 * i + 1];
                    }
                    for (int j = 2 * i; j < 2 * i + 2; j++) {
                        if (iso[j] < 100) {
                            if (iso[j] > 0) {
                                if (minPositiveIso > 100 || iso[j] < minPositiveIso) {
                                    minPositiveIso = iso[j];
                                }
                            } else {
                                if (minNegativeIso > 100 || iso[j] > minNegativeIso) {
                                    minNegativeIso = iso[j];
                                }
                            }
                        }
                    }
                  //  break;
                }
                if (iso[2 * i + 2]!=null){
                    if (pRot.y() < 0) {
                        isoStereoL2 = iso[2 * i +2];
                        isoAxialL2 = iso[2 * i +3];
                    } else {
                        isoStereoL2 = iso[2 * i +3];
                        isoAxialL2 = iso[2 * i +2];
                    } 
                    for (int j = 2 * i + 2; j < 2 * i + 4; j++) {
                        if (iso[j] < 100) {
                            if (iso[j] > 0) {
                                if (minPositiveIsoL2 > 100 || iso[j] < minPositiveIsoL2) {
                                    minPositiveIsoL2 = iso[j];
                                }
                            } else {
                                if (minNegativeIsoL2 > 100 || iso[j] > minNegativeIsoL2) {
                                    minNegativeIsoL2 = iso[j];
                                }
                            }
                        }
                    }
                }
                break;       
            }
                
            ////////////////////
           
            
            ///////////////////////////
            double trkT = TrackUtils.getTrackTime(track, TrackUtils.getHitToStripsTable(event), TrackUtils.getHitToRotatedTable(event));
            double trkTsd = TrackUtils.getTrackTimeSD(track, TrackUtils.getHitToStripsTable(event), TrackUtils.getHitToRotatedTable(event));

            Hep3Vector atEcal = TrackUtils.getTrackPositionAtEcal(tweakedTrackState);
            Hep3Vector firstHitPosition = VecOp.mult(beamAxisRotation, CoordinateTransformations.transformVectorToDetector(new BasicHep3Vector(track.getTrackerHits().get(0).getPosition())));
            GenericObject kinks = GBLKinkData.getKinkData(event, track);

            RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
            RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);

            double hitTimes[] = new double[2];
            double hitdEdx[] = new double[2];
            int hitClusterSize[] = new int[2];

            TrackerHit hit = track.getTrackerHits().get(0);
            Collection<TrackerHit> htsList = hitToStrips.allFrom(hitToRotated.from(hit));
            for (TrackerHit hts : htsList) {
                int layer = ((HpsSiSensor) ((RawTrackerHit) hts.getRawHits().get(0)).getDetectorElement()).getLayerNumber();
                hitTimes[layer % 2] = hts.getTime();
                hitdEdx[layer % 2] = hts.getdEdx();
                hitClusterSize[layer % 2] = hts.getRawHits().size();
            }

            //////////////////////////////////////////////////////////////////////////
            double rawHitTime[] = new double[nLay];
            double rawHitTDiff[] = new double[nLay];
            double rawHitMaxAmpl[] = new double[nLay];
            double rawHitChisq[] = new double[nLay];
            int nTrackHits = 0;
            List <TrackerHit> allTrackHits = track.getTrackerHits();
            for (TrackerHit iTrackHit : allTrackHits){
                List <RawTrackerHit> allRawHits = iTrackHit.getRawHits();
        
                //Get the list of fitted hits from the event
                List<LCRelation> fittedHits = event.get(LCRelation.class, "SVTFittedRawTrackerHits");

                //Map the fitted hits to their corresponding raw hits
                Map<RawTrackerHit, LCRelation> fittedRawTrackerHitMap = new HashMap<RawTrackerHit, LCRelation>();
            
                for (LCRelation fittedHit : fittedHits) {
                    fittedRawTrackerHitMap.put(FittedRawTrackerHit.getRawTrackerHit(fittedHit), fittedHit);
                }
            
               
                int sz = 0;
                double t0 = 0;
                double amplmax = 0;
                double chi2 = 0;
                double t0min = 0;
                double t0max = 0;
                for (RawTrackerHit iRawHit : allRawHits){
                  //0=T0, 1=T0 error, 2=amplitude, 3=amplitude error, 4=chi2 of fit 
                    GenericObject fitPar = FittedRawTrackerHit.getShapeFitParameters(fittedRawTrackerHitMap.get(iRawHit));
                    sz++;
                    if (sz==1){
                        t0min = fitPar.getDoubleVal(0);
                        t0max = fitPar.getDoubleVal(0);
                    }
                    if (t0min > fitPar.getDoubleVal(0)){
                        t0min = fitPar.getDoubleVal(0);
                    }
                    if (t0max < fitPar.getDoubleVal(0)){
                        t0max = fitPar.getDoubleVal(0);
                    } 
                    if (amplmax < fitPar.getDoubleVal(2)){
                        amplmax = fitPar.getDoubleVal(2);
                        chi2 = fitPar.getDoubleVal(4);
                        t0 = fitPar.getDoubleVal(0);

                    }//end if
                }  //end loop over raw hits
                //System.out.println("\t nTrackHits\t"+nTrackHits+"\t t0\t"+t0+"\ttdiff\t"+t0max+"\tampl\t"+amplmax+
                //        "\tchi2\t"+chi2);
                rawHitTime[nTrackHits] = t0;
                rawHitTDiff[nTrackHits] = t0max - t0min;
                rawHitMaxAmpl[nTrackHits] = amplmax;
                rawHitChisq[nTrackHits] = chi2;
                nTrackHits ++;          
            }//end loop over track hits
            int allShared = TrackUtils.numberOfSharedHits(track, allTracks);
            Track trackShared = TrackUtils.mostSharedHitTrack(track,allTracks);
            ///calculate the shared track momentum:
            TrackState trackStateShared = trackShared.getTrackStates().get(0);
            double[] paramShared = new double[5];
            for (int i = 0; i < 5; i++) {
                paramShared[i] = trackStateShared.getParameters()[i] + ((trackStateShared.getTanLambda() > 0) ? topTrackCorrection[i] : botTrackCorrection[i]);
            }
            //            Arrays.
            TrackState tweakedTrackStateShared = new BaseTrackState(paramShared, trackStateShared.getReferencePoint(), trackStateShared.getCovMatrix(), trackStateShared.getLocation(), bfield);
            Hep3Vector pRotShared = VecOp.mult(beamAxisRotation, CoordinateTransformations.transformVectorToDetector(new BasicHep3Vector(tweakedTrackStateShared.getMomentum())));
            //////////////////////////////////////            
            double momentumOfShared = pRotShared.magnitude();
            int maxShared = TrackUtils.numberOfSharedHits(track, trackShared);
            Hep3Vector atEcalShared = TrackUtils.getTrackPositionAtEcal(tweakedTrackStateShared);

            //if (track == trackShared){System.out.println("Tracks are same!");}
            //System.out.println("momentum of shared:\t"+momentumOfShared+"\t max shared \t"+maxShared+"\tall shared\t"+allShared);       
            ///////////////////////////////////////////////////////////////////////////////////////
        
            tupleMap.put(prefix + "RawMaxAmplL1/D", rawHitMaxAmpl[0]);
            tupleMap.put(prefix + "RawT0L1/D", rawHitTime[0]);
            tupleMap.put(prefix + "RawChisqL1/D", rawHitChisq[0]);
            tupleMap.put(prefix + "RawTDiffL1/D", rawHitTDiff[0]);
            tupleMap.put(prefix + "RawMaxAmplL2/D", rawHitMaxAmpl[1]);
            tupleMap.put(prefix + "RawT0L2/D", rawHitTime[1]);
            tupleMap.put(prefix + "RawChisqL2/D", rawHitChisq[1]);
            tupleMap.put(prefix + "RawTDiffL2/D", rawHitTDiff[1]);
            tupleMap.put(prefix + "RawMaxAmplL3/D", rawHitMaxAmpl[2]);
            tupleMap.put(prefix + "RawT0L3/D", rawHitTime[2]);
            tupleMap.put(prefix + "RawChisqL3/D", rawHitChisq[2]);
            tupleMap.put(prefix + "RawTDiffL3/D", rawHitTDiff[2]);
            tupleMap.put(prefix + "NTrackHits/I", (double) nTrackHits);    
            
            tupleMap.put(prefix + "PX/D", pRot.x());
            tupleMap.put(prefix + "PY/D", pRot.y());
            tupleMap.put(prefix + "PZ/D", pRot.z());
            tupleMap.put(prefix + "P/D", pRot.magnitude());
            tupleMap.put(prefix + "TrkZ0/D", tweakedTrackState.getZ0());
            tupleMap.put(prefix + "TrkLambda/D", tweakedTrackState.getTanLambda());
            tupleMap.put(prefix + "TrkD0/D", tweakedTrackState.getD0());
            tupleMap.put(prefix + "TrkPhi/D", tweakedTrackState.getPhi());
            tupleMap.put(prefix + "TrkOmega/D", tweakedTrackState.getOmega());
            tupleMap.put(prefix + "TrkEcalX/D", atEcal.x());
            tupleMap.put(prefix + "TrkEcalY/D", atEcal.y());
            tupleMap.put(prefix + "TrkChisq/D", track.getChi2());
            tupleMap.put(prefix + "TrkHits/I", (double) track.getTrackerHits().size());
            tupleMap.put(prefix + "TrkType/I", (double) particle.getType());
            tupleMap.put(prefix + "TrkT/D", trkT);
            tupleMap.put(prefix + "TrkTsd/D", trkTsd);
            tupleMap.put(prefix + "HasL1/B", iso[0] != null ? 1.0 : 0.0);
            tupleMap.put(prefix + "HasL2/B", iso[2] != null ? 1.0 : 0.0);
            tupleMap.put(prefix + "HasL3/B", iso[4] != null ? 1.0 : 0.0);
            tupleMap.put(prefix + "HasL4/B", iso[6] != null ? 1.0 : 0.0);
            tupleMap.put(prefix + "HasL5/B", iso[8] != null ? 1.0 : 0.0);
            tupleMap.put(prefix + "HasL6/B", iso[10] != null ? 1.0 : 0.0);
            tupleMap.put(prefix + "FirstHitX/D", firstHitPosition.x());
            tupleMap.put(prefix + "FirstHitY/D", firstHitPosition.y());
            tupleMap.put(prefix + "FirstHitT1/D", hitTimes[0]);
            tupleMap.put(prefix + "FirstHitT2/D", hitTimes[1]);
            tupleMap.put(prefix + "FirstHitDEDx1/D", hitdEdx[0]);
            tupleMap.put(prefix + "FirstHitDEDx2/D", hitdEdx[1]);
            tupleMap.put(prefix + "FirstClusterSize1/I", (double) hitClusterSize[0]);
            tupleMap.put(prefix + "FirstClusterSize2/I", (double) hitClusterSize[1]);
            tupleMap.put(prefix + "NHitsShared/I", (double) TrackUtils.numberOfSharedHits(track, allTracks));
            tupleMap.put(prefix + "HitsSharedP/D", momentumOfShared);
            tupleMap.put(prefix + "MaxHitsShared/I", (double) maxShared);
            tupleMap.put(prefix + "SharedTrkChisq/D", trackShared.getChi2());          
            tupleMap.put(prefix + "SharedTrkEcalX/D", atEcalShared.x());
            tupleMap.put(prefix + "SharedTrkEcalY/D", atEcalShared.y());    
            
            tupleMap.put(prefix + "LambdaKink1/D", kinks != null ? GBLKinkData.getLambdaKink(kinks, 1) : 0);
            tupleMap.put(prefix + "LambdaKink2/D", kinks != null ? GBLKinkData.getLambdaKink(kinks, 2) : 0);
            tupleMap.put(prefix + "LambdaKink3/D", kinks != null ? GBLKinkData.getLambdaKink(kinks, 3) : 0);
            tupleMap.put(prefix + "PhiKink1/D", kinks != null ? GBLKinkData.getPhiKink(kinks, 1) : 0);
            tupleMap.put(prefix + "PhiKink2/D", kinks != null ? GBLKinkData.getPhiKink(kinks, 2) : 0);
            tupleMap.put(prefix + "PhiKink3/D", kinks != null ? GBLKinkData.getPhiKink(kinks, 3) : 0);
            tupleMap.put(prefix + "IsoStereo/D", isoStereo);
            tupleMap.put(prefix + "IsoAxial/D", isoAxial);
            tupleMap.put(prefix + "IsoStereoL2/D", isoStereoL2);
            tupleMap.put(prefix + "IsoAxialL2/D", isoAxialL2);
            tupleMap.put(prefix + "MinPositiveIso/D", minPositiveIso);
            tupleMap.put(prefix + "MinNegativeIso/D", minNegativeIso);
            tupleMap.put(prefix + "MinPositiveIsoL2/D", minPositiveIsoL2);
            tupleMap.put(prefix + "MinNegativeIsoL2/D", minNegativeIsoL2);
            tupleMap.put(prefix + "TrkExtrpXL0/D", extrapTrackPosL0.x());
            tupleMap.put(prefix + "TrkExtrpYL0/D", extrapTrackPosL0.y());
            tupleMap.put(prefix + "TrkExtrpXL1/D", extrapTrackPosL1.x());
            tupleMap.put(prefix + "TrkExtrpYL1/D", extrapTrackPosL1.y());
            tupleMap.put(prefix + "TrkExtrpXL2/D", extrapTrackPosL2.x());
            tupleMap.put(prefix + "TrkExtrpYL2/D", extrapTrackPosL2.y());
            tupleMap.put(prefix + "MatchChisq/D", particle.getGoodnessOfPID());
           

            returnTrackState = tweakedTrackState;
        }

        if (!particle.getClusters().isEmpty()) {
            Cluster cluster = particle.getClusters().get(0);
            tupleMap.put(prefix + "ClT/D", ClusterUtilities.getSeedHitTime(cluster));
            tupleMap.put(prefix + "ClE/D", cluster.getEnergy());
            tupleMap.put(prefix + "ClSeedE/D", ClusterUtilities.findSeedHit(cluster).getCorrectedEnergy());
            tupleMap.put(prefix + "ClX/D", cluster.getPosition()[0]);
            tupleMap.put(prefix + "ClY/D", cluster.getPosition()[1]);
            tupleMap.put(prefix + "ClZ/D", cluster.getPosition()[2]);
            tupleMap.put(prefix + "ClHits/I", (double) cluster.getCalorimeterHits().size());
            tupleMap.put(prefix + "Clix/I", (double) ClusterUtilities.findSeedHit(cluster).getIdentifierFieldValue("ix"));
            tupleMap.put(prefix + "Cliy/I", (double) ClusterUtilities.findSeedHit(cluster).getIdentifierFieldValue("iy"));
        }

        return returnTrackState;

        //return particle;    
    }

    protected void fillVertexVariables(EventHeader event, List<BilliorTrack> billiorTracks, ReconstructedParticle electron, ReconstructedParticle positron) {
        BilliorVertexer vtxFitter = new BilliorVertexer(TrackUtils.getBField(event.getDetector()).y());
        vtxFitter.setBeamSize(beamSize);
        vtxFitter.setBeamPosition(beamPos);

        vtxFitter.doBeamSpotConstraint(false);
        BilliorVertex uncVertex = vtxFitter.fitVertex(billiorTracks);
        ReconstructedParticle uncV0 = HpsReconParticleDriver.makeReconstructedParticle(electron, positron, uncVertex);
        Hep3Vector uncMomRot = VecOp.mult(beamAxisRotation, uncV0.getMomentum());
        Hep3Vector uncVtx = VecOp.mult(beamAxisRotation, uncV0.getStartVertex().getPosition());

        vtxFitter.doBeamSpotConstraint(true);
        BilliorVertex bsconVertex = vtxFitter.fitVertex(billiorTracks);
        ReconstructedParticle bscV0 = HpsReconParticleDriver.makeReconstructedParticle(electron, positron, bsconVertex);
        Hep3Vector bscMomRot = VecOp.mult(beamAxisRotation, bscV0.getMomentum());
        Hep3Vector bscVtx = VecOp.mult(beamAxisRotation, bscV0.getStartVertex().getPosition());
        Matrix uncCov = MatrixOp.mult(MatrixOp.mult(beamAxisRotation,uncV0.getStartVertex().getCovMatrix()),MatrixOp.transposed(beamAxisRotation));

        vtxFitter.doTargetConstraint(true);
        BilliorVertex tarVertex = vtxFitter.fitVertex(billiorTracks);
        ReconstructedParticle tarV0 = HpsReconParticleDriver.makeReconstructedParticle(electron, positron, tarVertex);
        Hep3Vector tarMomRot = VecOp.mult(beamAxisRotation, tarV0.getMomentum());
        Hep3Vector tarVtx = VecOp.mult(beamAxisRotation, tarV0.getStartVertex().getPosition());

        vtxFitter.setBeamSize(vzcBeamSize);
        vtxFitter.doTargetConstraint(true);
        BilliorVertex vzcVertex = vtxFitter.fitVertex(billiorTracks);
        ReconstructedParticle vzcV0 = HpsReconParticleDriver.makeReconstructedParticle(electron, positron, vzcVertex);
        Hep3Vector vzcMomRot = VecOp.mult(beamAxisRotation, vzcV0.getMomentum());
        Hep3Vector vzcVtx = VecOp.mult(beamAxisRotation, vzcV0.getStartVertex().getPosition());

        tupleMap.put("uncPX/D", uncMomRot.x());
        tupleMap.put("uncPY/D", uncMomRot.y());
        tupleMap.put("uncPZ/D", uncMomRot.z());
        tupleMap.put("uncP/D", uncMomRot.magnitude());
        tupleMap.put("uncVX/D", uncVtx.x());
        tupleMap.put("uncVY/D", uncVtx.y());
        tupleMap.put("uncVZ/D", uncVtx.z());
        tupleMap.put("uncChisq/D", uncV0.getStartVertex().getChi2());
        tupleMap.put("uncM/D", uncV0.getMass());
        tupleMap.put("uncElePX/D", uncV0.getStartVertex().getParameters().get("p1X"));
        tupleMap.put("uncElePY/D", uncV0.getStartVertex().getParameters().get("p1Y"));
        tupleMap.put("uncElePZ/D", uncV0.getStartVertex().getParameters().get("p1Z"));
        tupleMap.put("uncEleP/D", Math.sqrt(Math.pow(uncV0.getStartVertex().getParameters().get("p1X"), 2) + Math.pow(uncV0.getStartVertex().getParameters().get("p1Y"), 2)
                + Math.pow(uncV0.getStartVertex().getParameters().get("p1Z"), 2)));
        tupleMap.put("uncPosPX/D", uncV0.getStartVertex().getParameters().get("p2X"));
        tupleMap.put("uncPosPY/D", uncV0.getStartVertex().getParameters().get("p2Y"));
        tupleMap.put("uncPosPZ/D", uncV0.getStartVertex().getParameters().get("p2Z"));
        tupleMap.put("uncPosP/D", Math.sqrt(Math.pow(uncV0.getStartVertex().getParameters().get("p2X"), 2) + Math.pow(uncV0.getStartVertex().getParameters().get("p2Y"), 2)
                + Math.pow(uncV0.getStartVertex().getParameters().get("p2Z"), 2)));
        tupleMap.put("uncCovXX/D", uncCov.e(0,0));
        tupleMap.put("uncCovXY/D", uncCov.e(0,1));
        tupleMap.put("uncCovXZ/D", uncCov.e(0,2));
        tupleMap.put("uncCovYX/D", uncCov.e(1,0));
        tupleMap.put("uncCovYY/D", uncCov.e(1,1));
        tupleMap.put("uncCovYZ/D", uncCov.e(1,2));
        tupleMap.put("uncCovZX/D", uncCov.e(2,0));
        tupleMap.put("uncCovZY/D", uncCov.e(2,1));
        tupleMap.put("uncCovZZ/D", uncCov.e(2,2));

        tupleMap.put("bscPX/D", bscMomRot.x());
        tupleMap.put("bscPY/D", bscMomRot.y());
        tupleMap.put("bscPZ/D", bscMomRot.z());
        tupleMap.put("bscP/D", bscMomRot.magnitude());
        tupleMap.put("bscVX/D", bscVtx.x());
        tupleMap.put("bscVY/D", bscVtx.y());
        tupleMap.put("bscVZ/D", bscVtx.z());
        tupleMap.put("bscChisq/D", bscV0.getStartVertex().getChi2());
        tupleMap.put("bscM/D", bscV0.getMass());
        tupleMap.put("bscElePX/D", bscV0.getStartVertex().getParameters().get("p1X"));
        tupleMap.put("bscElePY/D", bscV0.getStartVertex().getParameters().get("p1Y"));
        tupleMap.put("bscElePZ/D", bscV0.getStartVertex().getParameters().get("p1Z"));
        tupleMap.put("bscEleP/D", Math.sqrt(Math.pow(bscV0.getStartVertex().getParameters().get("p1X"), 2) + Math.pow(bscV0.getStartVertex().getParameters().get("p1Y"), 2)
                + Math.pow(bscV0.getStartVertex().getParameters().get("p1Z"), 2)));
        tupleMap.put("bscPosPX/D", bscV0.getStartVertex().getParameters().get("p2X"));
        tupleMap.put("bscPosPY/D", bscV0.getStartVertex().getParameters().get("p2Y"));
        tupleMap.put("bscPosPZ/D", bscV0.getStartVertex().getParameters().get("p2Z"));
        tupleMap.put("bscPosP/D", Math.sqrt(Math.pow(bscV0.getStartVertex().getParameters().get("p2X"), 2) + Math.pow(bscV0.getStartVertex().getParameters().get("p2Y"), 2)
                + Math.pow(bscV0.getStartVertex().getParameters().get("p2Z"), 2)));

        tupleMap.put("tarPX/D", tarMomRot.x());
        tupleMap.put("tarPY/D", tarMomRot.y());
        tupleMap.put("tarPZ/D", tarMomRot.z());
        tupleMap.put("tarP/D", tarMomRot.magnitude());
        tupleMap.put("tarVX/D", tarVtx.x());
        tupleMap.put("tarVY/D", tarVtx.y());
        tupleMap.put("tarVZ/D", tarVtx.z());
        tupleMap.put("tarChisq/D", tarV0.getStartVertex().getChi2());
        tupleMap.put("tarM/D", tarV0.getMass());
        tupleMap.put("tarElePX/D", tarV0.getStartVertex().getParameters().get("p1X"));
        tupleMap.put("tarElePY/D", tarV0.getStartVertex().getParameters().get("p1Y"));
        tupleMap.put("tarElePZ/D", tarV0.getStartVertex().getParameters().get("p1Z"));
        tupleMap.put("tarEleP/D", Math.sqrt(Math.pow(tarV0.getStartVertex().getParameters().get("p1X"), 2) + Math.pow(tarV0.getStartVertex().getParameters().get("p1Y"), 2)
                + Math.pow(tarV0.getStartVertex().getParameters().get("p1Z"), 2)));
        tupleMap.put("tarPosPX/D", tarV0.getStartVertex().getParameters().get("p2X"));
        tupleMap.put("tarPosPY/D", tarV0.getStartVertex().getParameters().get("p2Y"));
        tupleMap.put("tarPosPZ/D", tarV0.getStartVertex().getParameters().get("p2Z"));
        tupleMap.put("tarPosP/D", Math.sqrt(Math.pow(tarV0.getStartVertex().getParameters().get("p2X"), 2) + Math.pow(tarV0.getStartVertex().getParameters().get("p2Y"), 2)
                + Math.pow(tarV0.getStartVertex().getParameters().get("p2Z"), 2)));

        tupleMap.put("vzcPX/D", vzcMomRot.x());
        tupleMap.put("vzcPY/D", vzcMomRot.y());
        tupleMap.put("vzcPZ/D", vzcMomRot.z());
        tupleMap.put("vzcP/D", vzcMomRot.magnitude());
        tupleMap.put("vzcVX/D", vzcVtx.x());
        tupleMap.put("vzcVY/D", vzcVtx.y());
        tupleMap.put("vzcVZ/D", vzcVtx.z());
        tupleMap.put("vzcChisq/D", vzcV0.getStartVertex().getChi2());
        tupleMap.put("vzcM/D", vzcV0.getMass());
        tupleMap.put("vzcElePX/D", vzcV0.getStartVertex().getParameters().get("p1X"));
        tupleMap.put("vzcElePY/D", vzcV0.getStartVertex().getParameters().get("p1Y"));
        tupleMap.put("vzcElePZ/D", vzcV0.getStartVertex().getParameters().get("p1Z"));
        tupleMap.put("vzcEleP/D", Math.sqrt(Math.pow(vzcV0.getStartVertex().getParameters().get("p1X"), 2) + Math.pow(vzcV0.getStartVertex().getParameters().get("p1Y"), 2)
                + Math.pow(vzcV0.getStartVertex().getParameters().get("p1Z"), 2)));
        tupleMap.put("vzcPosPX/D", vzcV0.getStartVertex().getParameters().get("p2X"));
        tupleMap.put("vzcPosPY/D", vzcV0.getStartVertex().getParameters().get("p2Y"));
        tupleMap.put("vzcPosPZ/D", vzcV0.getStartVertex().getParameters().get("p2Z"));
        tupleMap.put("vzcPosP/D", Math.sqrt(Math.pow(vzcV0.getStartVertex().getParameters().get("p2X"), 2) + Math.pow(vzcV0.getStartVertex().getParameters().get("p2Y"), 2)
                + Math.pow(vzcV0.getStartVertex().getParameters().get("p2Z"), 2)));

        //////////////////////////////////////////////////////////////////////////////////////////
        int nEleClusters = electron.getClusters().size();
        int nPosClusters = positron.getClusters().size();

        if (nEleClusters > 0) {

            tupleMap.put("uncEleWtP/D", MassCalculator.combinedMomentum(electron.getClusters().get(0), electron.getTracks().get(0),
                    Math.sqrt(Math.pow(uncV0.getStartVertex().getParameters().get("p1X"), 2) + Math.pow(uncV0.getStartVertex().getParameters().get("p1Y"), 2)
                            + Math.pow(uncV0.getStartVertex().getParameters().get("p1Z"), 2))));
            tupleMap.put("vzcEleWtP/D", MassCalculator.combinedMomentum(electron.getClusters().get(0), electron.getTracks().get(0),
                    Math.sqrt(Math.pow(vzcV0.getStartVertex().getParameters().get("p1X"), 2) + Math.pow(vzcV0.getStartVertex().getParameters().get("p1Y"), 2)
                            + Math.pow(vzcV0.getStartVertex().getParameters().get("p1Z"), 2))));
            tupleMap.put("tarEleWtP/D", MassCalculator.combinedMomentum(electron.getClusters().get(0), electron.getTracks().get(0),
                    Math.sqrt(Math.pow(tarV0.getStartVertex().getParameters().get("p1X"), 2) + Math.pow(tarV0.getStartVertex().getParameters().get("p1Y"), 2)
                            + Math.pow(tarV0.getStartVertex().getParameters().get("p1Z"), 2))));
            tupleMap.put("bscEleWtP/D", MassCalculator.combinedMomentum(electron.getClusters().get(0), electron.getTracks().get(0),
                    Math.sqrt(Math.pow(bscV0.getStartVertex().getParameters().get("p1X"), 2) + Math.pow(bscV0.getStartVertex().getParameters().get("p1Y"), 2)
                            + Math.pow(bscV0.getStartVertex().getParameters().get("p1Z"), 2))));

            if (nPosClusters > 0) {

                tupleMap.put("vzcPosWtP/D", MassCalculator.combinedMomentum(positron.getClusters().get(0), positron.getTracks().get(0),
                        Math.sqrt(Math.pow(vzcV0.getStartVertex().getParameters().get("p2X"), 2) + Math.pow(vzcV0.getStartVertex().getParameters().get("p2Y"), 2)
                                + Math.pow(vzcV0.getStartVertex().getParameters().get("p2Z"), 2))));
                tupleMap.put("tarPosWtP/D", MassCalculator.combinedMomentum(positron.getClusters().get(0), positron.getTracks().get(0),
                        Math.sqrt(Math.pow(tarV0.getStartVertex().getParameters().get("p2X"), 2) + Math.pow(tarV0.getStartVertex().getParameters().get("p2Y"), 2)
                                + Math.pow(tarV0.getStartVertex().getParameters().get("p2Z"), 2))));
                tupleMap.put("bscPosWtP/D", MassCalculator.combinedMomentum(positron.getClusters().get(0), positron.getTracks().get(0),
                        Math.sqrt(Math.pow(bscV0.getStartVertex().getParameters().get("p2X"), 2) + Math.pow(bscV0.getStartVertex().getParameters().get("p2Y"), 2)
                                + Math.pow(bscV0.getStartVertex().getParameters().get("p2Z"), 2))));
                tupleMap.put("uncPosWtP/D", MassCalculator.combinedMomentum(positron.getClusters().get(0), positron.getTracks().get(0),
                        Math.sqrt(Math.pow(uncV0.getStartVertex().getParameters().get("p2X"), 2) + Math.pow(uncV0.getStartVertex().getParameters().get("p2Y"), 2)
                                + Math.pow(uncV0.getStartVertex().getParameters().get("p2Z"), 2))));

                tupleMap.put("vzcWtM/D", MassCalculator.combinedMass(electron.getClusters().get(0), positron.getClusters().get(0), vzcV0));
                tupleMap.put("tarWtM/D", MassCalculator.combinedMass(electron.getClusters().get(0), positron.getClusters().get(0), tarV0));
                tupleMap.put("bscWtM/D", MassCalculator.combinedMass(electron.getClusters().get(0), positron.getClusters().get(0), bscV0));
                tupleMap.put("uncWtM/D", MassCalculator.combinedMass(electron.getClusters().get(0), positron.getClusters().get(0), uncV0));

            } else {//e- has cluster, e+ does not
                tupleMap.put("vzcPosWtP/D", Math.sqrt(Math.pow(vzcV0.getStartVertex().getParameters().get("p2X"), 2) + Math.pow(vzcV0.getStartVertex().getParameters().get("p2Y"), 2)
                        + Math.pow(vzcV0.getStartVertex().getParameters().get("p2Z"), 2)));
                tupleMap.put("bscPosWtP/D", Math.sqrt(Math.pow(bscV0.getStartVertex().getParameters().get("p2X"), 2) + Math.pow(bscV0.getStartVertex().getParameters().get("p2Y"), 2)
                        + Math.pow(bscV0.getStartVertex().getParameters().get("p2Z"), 2)));
                tupleMap.put("tarPosWtP/D", Math.sqrt(Math.pow(tarV0.getStartVertex().getParameters().get("p2X"), 2) + Math.pow(tarV0.getStartVertex().getParameters().get("p2Y"), 2)
                        + Math.pow(tarV0.getStartVertex().getParameters().get("p2Z"), 2)));
                tupleMap.put("uncPosWtP/D", Math.sqrt(Math.pow(uncV0.getStartVertex().getParameters().get("p2X"), 2) + Math.pow(uncV0.getStartVertex().getParameters().get("p2Y"), 2)
                        + Math.pow(uncV0.getStartVertex().getParameters().get("p2Z"), 2)));
                tupleMap.put("vzcWtM/D", MassCalculator.combinedMass(electron.getClusters().get(0), positron.getTracks().get(0), vzcV0));
                tupleMap.put("tarWtM/D", MassCalculator.combinedMass(electron.getClusters().get(0), positron.getTracks().get(0), tarV0));
                tupleMap.put("bscWtM/D", MassCalculator.combinedMass(electron.getClusters().get(0), positron.getTracks().get(0), bscV0));
                tupleMap.put("uncWtM/D", MassCalculator.combinedMass(electron.getClusters().get(0), positron.getTracks().get(0), uncV0));
            }

        }

        if (nPosClusters > 0 && nEleClusters == 0) {//e+ has cluster, e- does not
            tupleMap.put("vzcPosWtP/D", MassCalculator.combinedMomentum(positron.getClusters().get(0), positron.getTracks().get(0),
                    Math.sqrt(Math.pow(vzcV0.getStartVertex().getParameters().get("p2X"), 2) + Math.pow(vzcV0.getStartVertex().getParameters().get("p2Y"), 2)
                            + Math.pow(vzcV0.getStartVertex().getParameters().get("p2Z"), 2))));
            tupleMap.put("tarPosWtP/D", MassCalculator.combinedMomentum(positron.getClusters().get(0), positron.getTracks().get(0),
                    Math.sqrt(Math.pow(tarV0.getStartVertex().getParameters().get("p2X"), 2) + Math.pow(tarV0.getStartVertex().getParameters().get("p2Y"), 2)
                            + Math.pow(tarV0.getStartVertex().getParameters().get("p2Z"), 2))));
            tupleMap.put("bscPosWtP/D", MassCalculator.combinedMomentum(positron.getClusters().get(0), positron.getTracks().get(0),
                    Math.sqrt(Math.pow(bscV0.getStartVertex().getParameters().get("p2X"), 2) + Math.pow(bscV0.getStartVertex().getParameters().get("p2Y"), 2)
                            + Math.pow(bscV0.getStartVertex().getParameters().get("p2Z"), 2))));
            tupleMap.put("uncPosWtP/D", MassCalculator.combinedMomentum(positron.getClusters().get(0), positron.getTracks().get(0),
                    Math.sqrt(Math.pow(uncV0.getStartVertex().getParameters().get("p2X"), 2) + Math.pow(uncV0.getStartVertex().getParameters().get("p2Y"), 2)
                            + Math.pow(uncV0.getStartVertex().getParameters().get("p2Z"), 2))));
            tupleMap.put("vzcEleWtP/D", Math.sqrt(Math.pow(vzcV0.getStartVertex().getParameters().get("p1X"), 2) + Math.pow(vzcV0.getStartVertex().getParameters().get("p1Y"), 2)
                    + Math.pow(vzcV0.getStartVertex().getParameters().get("p1Z"), 2)));
            tupleMap.put("bscEleWtP/D", Math.sqrt(Math.pow(bscV0.getStartVertex().getParameters().get("p1X"), 2) + Math.pow(bscV0.getStartVertex().getParameters().get("p1Y"), 2)
                    + Math.pow(bscV0.getStartVertex().getParameters().get("p1Z"), 2)));
            tupleMap.put("tarEleWtP/D", Math.sqrt(Math.pow(tarV0.getStartVertex().getParameters().get("p1X"), 2) + Math.pow(tarV0.getStartVertex().getParameters().get("p1Y"), 2)
                    + Math.pow(tarV0.getStartVertex().getParameters().get("p1Z"), 2)));
            tupleMap.put("uncEleWtP/D", Math.sqrt(Math.pow(uncV0.getStartVertex().getParameters().get("p1X"), 2) + Math.pow(uncV0.getStartVertex().getParameters().get("p1Y"), 2)
                    + Math.pow(uncV0.getStartVertex().getParameters().get("p1Z"), 2)));

            tupleMap.put("vzcWtM/D", MassCalculator.combinedMass(electron.getTracks().get(0), positron.getClusters().get(0), vzcV0));
            tupleMap.put("tarWtM/D", MassCalculator.combinedMass(electron.getTracks().get(0), positron.getClusters().get(0), tarV0));
            tupleMap.put("bscWtM/D", MassCalculator.combinedMass(electron.getTracks().get(0), positron.getClusters().get(0), bscV0));
            tupleMap.put("uncWtM/D", MassCalculator.combinedMass(electron.getTracks().get(0), positron.getClusters().get(0), uncV0));
        }

        if (nPosClusters == 0 && nEleClusters == 0) {
            tupleMap.put("vzcEleWtP/D", Math.sqrt(Math.pow(vzcV0.getStartVertex().getParameters().get("p1X"), 2) + Math.pow(vzcV0.getStartVertex().getParameters().get("p1Y"), 2)
                    + Math.pow(vzcV0.getStartVertex().getParameters().get("p1Z"), 2)));
            tupleMap.put("vzcPosWtP/D", Math.sqrt(Math.pow(vzcV0.getStartVertex().getParameters().get("p2X"), 2) + Math.pow(vzcV0.getStartVertex().getParameters().get("p2Y"), 2)
                    + Math.pow(vzcV0.getStartVertex().getParameters().get("p2Z"), 2)));
            tupleMap.put("vzcWtM/D", vzcV0.getMass());
            tupleMap.put("tarEleWtP/D", Math.sqrt(Math.pow(tarV0.getStartVertex().getParameters().get("p1X"), 2) + Math.pow(tarV0.getStartVertex().getParameters().get("p1Y"), 2)
                    + Math.pow(tarV0.getStartVertex().getParameters().get("p1Z"), 2)));
            tupleMap.put("tarPosWtP/D", Math.sqrt(Math.pow(tarV0.getStartVertex().getParameters().get("p2X"), 2) + Math.pow(tarV0.getStartVertex().getParameters().get("p2Y"), 2)
                    + Math.pow(tarV0.getStartVertex().getParameters().get("p2Z"), 2)));
            tupleMap.put("tarWtM/D", tarV0.getMass());
            tupleMap.put("bscEleWtP/D", Math.sqrt(Math.pow(bscV0.getStartVertex().getParameters().get("p1X"), 2) + Math.pow(bscV0.getStartVertex().getParameters().get("p1Y"), 2)
                    + Math.pow(bscV0.getStartVertex().getParameters().get("p1Z"), 2)));
            tupleMap.put("bscPosWtP/D", Math.sqrt(Math.pow(bscV0.getStartVertex().getParameters().get("p2X"), 2) + Math.pow(bscV0.getStartVertex().getParameters().get("p2Y"), 2)
                    + Math.pow(bscV0.getStartVertex().getParameters().get("p2Z"), 2)));
            tupleMap.put("bscWtM/D", bscV0.getMass());
            tupleMap.put("uncEleWtP/D", Math.sqrt(Math.pow(uncV0.getStartVertex().getParameters().get("p1X"), 2) + Math.pow(uncV0.getStartVertex().getParameters().get("p1Y"), 2)
                    + Math.pow(uncV0.getStartVertex().getParameters().get("p1Z"), 2)));
            tupleMap.put("uncPosWtP/D", Math.sqrt(Math.pow(uncV0.getStartVertex().getParameters().get("p2X"), 2) + Math.pow(uncV0.getStartVertex().getParameters().get("p2Y"), 2)
                    + Math.pow(uncV0.getStartVertex().getParameters().get("p2Z"), 2)));
            tupleMap.put("uncWtM/D", uncV0.getMass());

        }

        //////////////////////////////////////////////////////////////////////////////
    }

    protected void addMCTridentVariables() {
        addMCParticleVariables("tri");
        addMCParticleVariables("triEle1");
        addMCParticleVariables("triEle2");
        addMCParticleVariables("triPos");
        addMCPairVariables("triPair1");
        addMCPairVariables("triPair2");
    }

    protected void addMCParticleVariables(String prefix) {
        String[] newVars = new String[]{"StartX/D", "StartY/D", "StartZ/D",
            "EndX/D", "EndY/D", "EndZ/D",
            "PX/D", "PY/D", "PZ/D", "P/D", "M/D", "E/D"};

        for (int i = 0; i < newVars.length; i++) {
            newVars[i] = prefix + newVars[i];
        }
        tupleVariables.addAll(Arrays.asList(newVars));
    }

    protected void addMCPairVariables(String prefix) {
        String[] newVars = new String[]{"PX/D", "PY/D", "PZ/D", "P/D", "M/D", "E/D"};

        for (int i = 0; i < newVars.length; i++) {
            newVars[i] = prefix + newVars[i];
        }
        tupleVariables.addAll(Arrays.asList(newVars));
    }

    protected void fillMCParticleVariables(String prefix, MCParticle particle) {
//        System.out.format("%d %x\n", particle.getGeneratorStatus(), particle.getSimulatorStatus().getValue());
        Hep3Vector start = VecOp.mult(beamAxisRotation, particle.getOrigin());
        Hep3Vector end;
        try {
            end = VecOp.mult(beamAxisRotation, particle.getEndPoint());
        } catch (RuntimeException e) {
            end = null;
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
    }

    protected void fillMCPairVariables(String prefix, MCParticle ele, MCParticle pos) {
        HepLorentzVector vtx = VecOp.add(ele.asFourVector(), pos.asFourVector());
        Hep3Vector vtxP = VecOp.mult(beamAxisRotation, vtx.v3());
        tupleMap.put(prefix + "PX/D", vtxP.x());
        tupleMap.put(prefix + "PY/D", vtxP.y());
        tupleMap.put(prefix + "PY/D", vtxP.z());
        tupleMap.put(prefix + "P/D", vtxP.magnitude());
        tupleMap.put(prefix + "M/D", vtx.magnitude());
        tupleMap.put(prefix + "E/D", vtx.t());
    }

    protected void fillMCTridentVariables(EventHeader event) {
        List<MCParticle> MCParticles = event.getMCParticles();

        MCParticle trident = null;

        MCParticle ele1 = null;//highest-energy electron daughter
        MCParticle ele2 = null;//second-highest-energy electron daughter (if any)
        MCParticle pos = null;//highest-energy positron daughter

        List<MCParticle> tridentParticles = null;

        for (MCParticle particle : MCParticles) {
            if (particle.getPDGID() == 622) {
                trident = particle;
                tridentParticles = particle.getDaughters();
                break;
            }
        }
        if (trident == null) {
            return;
        }

        fillMCParticleVariables("tri", trident);

        for (MCParticle particle : tridentParticles) {
            switch (particle.getPDGID()) {
                case -11:
                    if (pos == null || particle.getEnergy() > pos.getEnergy()) {
                        pos = particle;
                    }
                    break;
                case 11:
                    if (ele1 == null || particle.getEnergy() > ele1.getEnergy()) {
                        ele2 = ele1;
                        ele1 = particle;
                    } else if (ele2 == null || particle.getEnergy() > ele2.getEnergy()) {
                        ele2 = particle;
                    }
                    break;
            }
        }

        if (ele1 != null) {
            fillMCParticleVariables("triEle1", ele1);
        }
        if (ele2 != null) {
            fillMCParticleVariables("triEle2", ele2);
        }
        if (pos != null) {
            fillMCParticleVariables("triPos", pos);
        }

        if (pos != null && ele1 != null) {
            fillMCPairVariables("triPair1", ele1, pos);
            if (ele2 != null) {
                fillMCPairVariables("triPair2", ele2, pos);
            }
        }

    }
}
