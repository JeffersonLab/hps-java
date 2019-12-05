package org.hps.analysis.tuple;

import hep.physics.matrix.Matrix;
import hep.physics.matrix.MatrixOp;
import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

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
import org.hps.recon.particle.ReconParticleDriver;
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.recon.tracking.TrackStateUtils;
import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.gbl.GBLKinkData;
import org.hps.recon.vertexing.BilliorVertex;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.LCRelation;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.Vertex;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.util.Driver;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.TrackerHit;

import java.util.Collection;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;

/**
 * sort of an interface for DQM analysis drivers creates the DQM database
 * manager, checks whether row exists in db etc
 *
 * @author mgraham on Apr 15, 2014 update mgraham on May 15, 2014 to include
 * calculateEndOfRunQuantities & printDQMData i.e. useful methods
 */
public abstract class TupleMaker extends Driver {

    protected boolean debug = false;

    protected String tupleFile = null;
    protected PrintWriter tupleWriter = null;
    protected final List<String> tupleVariables = new ArrayList<String>();
    protected final Map<String, Double> tupleMap = new HashMap<String, Double>();

    protected String triggerType = "all";// allowed types are "" (blank) or "all", singles0, singles1, pairs0,pairs1
    private boolean applyBeamRotation = true;
    protected boolean isGBL = true;
    private final String finalStateParticlesColName = "FinalStateParticles";

    protected double bfield;
    protected FieldMap bFieldMap = null;

    protected static List<HpsSiSensor> sensors;
    protected static Subdetector trackerSubdet;
    private static final String SUBDETECTOR_NAME = "Tracker";
    protected String CandidatesColName = "V0Candidates";
    protected String VerticesColName = "V0Vertices";
    protected TIData triggerData;
    protected final BasicHep3Matrix beamAxisRotation = BasicHep3Matrix.identity();
    protected double ebeam = Double.NaN;
    protected int nLay = 6;
    protected int tupleevent = 0;
    protected int nTrackingLayers = nLay;
    protected double[] beamSize = {0.001, 0.130, 0.050}; //rough estimate from harp scans during engineering run production running
    private double[] extrapTrackXTopAxial = new double[nLay];
    private double[] extrapTrackXTopStereo = new double[nLay];
    private double[] extrapTrackXBotAxial = new double[nLay];
    private double[] extrapTrackXBotStereo = new double[nLay];
    private double[] extrapTrackYTopAxial = new double[nLay];
    private double[] extrapTrackYTopStereo = new double[nLay];
    private double[] extrapTrackYBotAxial = new double[nLay];
    private double[] extrapTrackYBotStereo = new double[nLay];
    private double[] extrapTrackXSensorTopAxial = new double[nLay];
    private double[] extrapTrackXSensorTopStereo = new double[nLay];
    private double[] extrapTrackXSensorBotAxial = new double[nLay];
    private double[] extrapTrackXSensorBotStereo = new double[nLay];
    private double[] extrapTrackYSensorTopAxial = new double[nLay];
    private double[] extrapTrackYSensorTopStereo = new double[nLay];
    private double[] extrapTrackYSensorBotAxial = new double[nLay];
    private double[] extrapTrackYSensorBotStereo = new double[nLay];
    private double[] extrapTrackYErrorSensorTopAxial = new double[nLay];
    private double[] extrapTrackYErrorSensorTopStereo = new double[nLay];
    private double[] extrapTrackYErrorSensorBotAxial = new double[nLay];
    private double[] extrapTrackYErrorSensorBotStereo = new double[nLay];
    List<ReconstructedParticle> unConstrainedV0List = null;
    List<ReconstructedParticle> bsConstrainedV0List = null;
    List<ReconstructedParticle> tarConstrainedV0List = null;
    List<Vertex> unConstrainedV0VerticeList = null;
    Map<ReconstructedParticle, BilliorVertex> cand2vert = null;
    Map<ReconstructedParticle, ReconstructedParticle> unc2bsc = null;
    Map<ReconstructedParticle, ReconstructedParticle> unc2tar = null;
    boolean cutTuple = true;

    abstract boolean passesCuts();
    
    public void setIsGBL(boolean isgbl) {
        this.isGBL = isgbl;
    }
    
    public void setCutTuple(boolean input) {
        cutTuple = input;
    }

    public void setCandidatesColName(String input) {
        this.CandidatesColName = input;
    }

    public void setVerticesColName(String input) {
        this.VerticesColName = input;
    }
    
    public void setNLay(int nLay) {
        this.nLay = nLay;
    }
    
    public void setNTrackingLayers(int nTrackingLayers) {
        this.nTrackingLayers = nTrackingLayers;
    }

    public void setApplyBeamRotation(boolean applyBeamRotation) {
        this.applyBeamRotation = applyBeamRotation;
    }

    public void setEbeam(double ebeam) {
        this.ebeam = ebeam;
    }

    public void setTriggerType(String type) {
        this.triggerType = type;
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
        bFieldMap = detector.getFieldMap();

        if (Double.isNaN(ebeam)) {
            try {
                BeamEnergy.BeamEnergyCollection beamEnergyCollection = this.getConditionsManager()
                        .getCachedConditions(BeamEnergy.BeamEnergyCollection.class, "beam_energies").getCachedData();
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
        sensors = detector.getSubdetector(SUBDETECTOR_NAME).getDetectorElement().findDescendants(HpsSiSensor.class);
        trackerSubdet = detector.getSubdetector(SUBDETECTOR_NAME);
    }

    protected boolean setupCollections(EventHeader event) {
        String unconstrainedV0CandidatesColName = "Unconstrained" + CandidatesColName;
        String unconstrainedV0VerticesColName = "Unconstrained" + VerticesColName;
        String beamspotConstrainedV0CandidatesColName = "BeamspotConstrained" + CandidatesColName;
        String targetConstrainedV0CandidatesColName = "TargetConstrained" + CandidatesColName;
        
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
        
        triggerData = checkTrigger(event);
        if (triggerData == null)
            return false;
        

        return true;
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
        // tupleMap.clear();
    }

    public void setTupleFile(String tupleFile) {
        this.tupleFile = tupleFile;
        // for (String variable : tupleVariables) {
        // tupleWriter.format("%s:", variable);
        // }
        // tupleWriter.println();
    }

    public TIData checkTrigger(EventHeader event) {
        TIData triggerData = null;
        if (event.hasCollection(GenericObject.class, "TriggerBank")) {
            for (GenericObject data : event.get(GenericObject.class, "TriggerBank")) {
                if (AbstractIntData.getTag(data) == TIData.BANK_TAG) {
                    triggerData = new TIData(data);
                }
            }
        }
        //check to see if this event is from the correct trigger (or "all");
        if (triggerData != null && !matchTriggerType(triggerData)) {
            return null;
        }
        return triggerData;
    }
    
    protected boolean fillBasicTuple(EventHeader event, TIData triggerData, ReconstructedParticle uncV0, boolean isMoller) {
        boolean isOK = true;

        if (isGBL != TrackType.isGBL(uncV0.getType())) {
            return false;
        }
        
        fillEventVariables(event, triggerData);

        double minPositiveIso = 0;
        double minNegativeIso = 0;
        if (isMoller) {
            ReconstructedParticle top = uncV0.getParticles().get(ReconParticleDriver.MOLLER_TOP);
            ReconstructedParticle bot = uncV0.getParticles().get(ReconParticleDriver.MOLLER_BOT);
            if (top.getCharge() != -1 || bot.getCharge() != -1) {
                throw new RuntimeException("incorrect charge on v0 daughters");
            }
            fillParticleVariables(event, top, "top");
            fillParticleVariables(event, bot, "bot");
            minPositiveIso = Math.min(tupleMap.get("topMinPositiveIso/D"), tupleMap.get("botMinPositiveIso/D"));
            minNegativeIso = Math.min(Math.abs(tupleMap.get("topMinNegativeIso/D")), Math.abs(tupleMap.get("botMinNegativeIso/D")));
            
        }
        else {
            ReconstructedParticle electron = uncV0.getParticles().get(ReconParticleDriver.ELECTRON);
            ReconstructedParticle positron = uncV0.getParticles().get(ReconParticleDriver.POSITRON);
            if (electron.getCharge() != -1 || positron.getCharge() != 1) {
                throw new RuntimeException("incorrect charge on v0 daughters");
            }
            TrackState eleTSTweaked = fillParticleVariables(event, electron, "ele");
            TrackState posTSTweaked = fillParticleVariables(event, positron, "pos");
            minPositiveIso = Math.min(tupleMap.get("eleMinPositiveIso/D"), tupleMap.get("posMinPositiveIso/D"));
            minNegativeIso = Math.min(Math.abs(tupleMap.get("eleMinNegativeIso/D")), Math.abs(tupleMap.get("posMinNegativeIso/D")));
        }
        double minIso = Math.min(minPositiveIso, minNegativeIso);
        tupleMap.put("minPositiveIso/D", minPositiveIso);
        tupleMap.put("minNegativeIso/D", minNegativeIso);
        tupleMap.put("minIso/D", minIso);

        fillVertexVariables("unc", uncV0, false);
        if (unc2bsc != null) {
            ReconstructedParticle temp = unc2bsc.get(uncV0);
            if (temp == null)
                isOK = false;
            else
                fillVertexVariables("bsc", temp, false);
        }
        if (unc2bsc != null) {
            ReconstructedParticle temp = unc2tar.get(uncV0);
            if (temp == null)
                isOK = false;
            fillVertexVariables("tar", temp, false);
        }

        return isOK;

    }
    
    protected void addEventVariables() {
        String[] newVars = new String[] {"run/I", "event/I", "tupleevent/I", "nPos/I", "nCl/I", "isCalib/B", "isPulser/B",
                "isSingle0/B", "isSingle1/B", "isPair0/B", "isPair1/B", "evTime/D", "evTx/I", "evTy/I", "rfT1/D",
                "rfT2/D", "nEcalHits/I", "nSVTHits/I", "n3DSVTHits/I", "nEcalCl/I", "nEcalClele/I", "nEcalClpos/I", "nEcalClpho/I",
                "nEcalClEleSide/I", "nEcalClPosSide/I", "nSVTHitsL1/I", "nSVTHitsL2/I", "nSVTHitsL3/I", "nSVTHitsL4/I",
                "nSVTHitsL5/I", "nSVTHitsL6/I", "nSVTHitsL1b/I", "nSVTHitsL2b/I", "nSVTHitsL3b/I", "nSVTHitsL4b/I",
                "nSVTHitsL5b/I", "nSVTHitsL6b/I", "topL1HitX/D", "topL1HitY/D", "botL1HitX/D", "botL1HitY/D"};
        tupleVariables.addAll(Arrays.asList(newVars));
    }

    protected void addVertexVariables() {
        addVertexVariables(true, true, true);
    }
    protected void addVertexVariables(boolean doBsc, boolean doTar, boolean doVzc) {
        String[] newVars = new String[] {"uncPX/D", "uncPY/D", "uncPZ/D", "uncP/D", "uncVX/D", "uncVY/D", "uncVZ/D",
                "uncChisq/D", "uncM/D", "uncMErr/D", "uncChisqProb/D", "uncCovXX/D", "uncCovXY/D", "uncCovXZ/D", "uncCovYX/D", "uncCovYY/D",
                "uncCovYZ/D", "uncCovZX/D", "uncCovZY/D", "uncCovZZ/D", "uncElePX/D", "uncElePY/D", "uncElePZ/D",
                "uncPosPX/D", "uncPosPY/D", "uncPosPZ/D", "uncEleP/D", "uncPosP/D", "uncEleWtP/D", "uncPosWtP/D", "uncWtM/D",
                "uncMom/D","uncMomX/D","uncMomY/D","uncMomZ/D","uncMomErr/D","uncMomXErr/D","uncMomYErr/D","uncMomZErr/D",
                "uncTargProjX/D","uncTargProjY/D","uncTargProjXErr/D","uncTargProjYErr/D","uncPosX/D","uncPosY/D","uncPosZ/D"};
        tupleVariables.addAll(Arrays.asList(newVars));
        if (doBsc) {
            String[] newVars2 = new String[] {"bscPX/D", "bscPY/D", "bscPZ/D", "bscP/D", "bscVX/D", "bscVY/D", "bscVZ/D",
                    "bscChisq/D", "bscM/D", "bscMErr/D", "bscChisqProb/D", "bscCovXX/D", "bscCovXY/D", "bscCovXZ/D", "bscCovYX/D", "bscCovYY/D",
                    "bscCovYZ/D", "bscCovZX/D", "bscCovZY/D", "bscCovZZ/D", "bscElePX/D", "bscElePY/D", "bscElePZ/D",
                    "bscPosPX/D", "bscPosPY/D", "bscPosPZ/D", "bscEleP/D", "bscPosP/D", "bscEleWtP/D", "bscPosWtP/D", "bscWtM/D",
                    "bscMom/D","bscMomX/D","bscMomY/D","bscMomZ/D","bscMomErr/D","bscMomXErr/D","bscMomYErr/D","bscMomZErr/D",
                    "bscTargProjX/D","bscTargProjY/D","bscTargProjXErr/D","bscTargProjYErr/D","bscPosX/D","bscPosY/D","bscPosZ/D"};
            tupleVariables.addAll(Arrays.asList(newVars2));
        }
        if (doTar) {
            String[] newVars3 = new String[] {"tarPX/D", "tarPY/D", "tarPZ/D", "tarP/D", "tarVX/D", "tarVY/D", "tarVZ/D",
                    "tarChisq/D", "tarM/D", "tarChisqProb/D", "tarElePX/D", "tarElePY/D", "tarElePZ/D", "tarPosPX/D", "tarPosPY/D", "tarPosPZ/D", "tarEleP/D", "tarPosP/D", "tarEleWtP/D", "tarPosWtP/D", "tarWtM/D"};
            tupleVariables.addAll(Arrays.asList(newVars3));
        }
        if (doVzc) {
            String[] newVars4 = new String[] {"vzcPX/D", "vzcPY/D", "vzcPZ/D", "vzcP/D", "vzcVX/D", "vzcVY/D", "vzcVZ/D",
                    "vzcChisq/D", "vzcM/D", "vzcElePX/D", "vzcElePY/D", "vzcElePZ/D", "vzcPosPX/D", "vzcPosPY/D", "vzcPosPZ/D", "vzcEleP/D",
                    "vzcPosP/D", "vzcEleWtP/D", "vzcPosWtP/D", "vzcWtM/D"};
            tupleVariables.addAll(Arrays.asList(newVars4));
        }
    }

    protected void addParticleVariables(String prefix) {
        addParticleVariables(prefix, true, true, true);
    }

    protected void addParticleVariables(String prefix, boolean doTrkExtrap, boolean doRaw, boolean doIso) {
        String[] newVars = new String[] {"PX/D", "PY/D", "PZ/D", "P/D", "TrkChisq/D", "TrkHits/I", "TrkType/I",
                "TrkT/D", "TrkTsd/D", "TrkZ0/D", "TrkLambda/D", "TrkD0/D", "TrkPhi/D", "TrkOmega/D", "TrkEcalX/D",
                "TrkEcalY/D", "HasL1/B", "HasL2/B", "HasL3/B", "HasL4/B", "HasL5/B", "HasL6/B", "FirstHitX/D",
                "FirstHitY/D", "FirstHitT1/D", "FirstHitT2/D", "FirstHitDEDx1/D", "FirstHitDEDx2/D",
                "FirstClusterSize1/I", "FirstClusterSize2/I", "NHitsShared/I", "HitsSharedP/D", "LambdaKink0/D", "LambdaKink1/D",
                "LambdaKink2/D", "LambdaKink3/D", "LambdaKink4/D", "LambdaKink5/D", "LambdaKink6/D", "PhiKink0/D", "PhiKink1/D", 
                "PhiKink2/D", "PhiKink3/D", "PhiKink4/D", "PhiKink5/D", "PhiKink6/D","NTrackHits/I",  "HitsSharedP/D", 
                "MaxHitsShared/I", "SharedTrkChisq/D", "SharedTrkEcalX/D", "SharedTrkEcalY/D", "MatchChisq/D", "ClT/D",
                "ClE/D", "ClSeedE/D", "ClX/D", "ClY/D", "ClZ/D", "ClHits/I", "Clix/I", "Cliy/I", "UncorrClT/D",
                "UncorrClE/D", "UncorrClX/D", "UncorrClY/D", "UncorrClZ/D", "TrkD0Err/D", "TrkZ0Err/D", "TrkLambdaErr/D", "TrkPhiErr/D", "TrkOmegaErr/D"};
        for (int i = 0; i < newVars.length; i++) {
            newVars[i] = prefix + newVars[i];
        }
        tupleVariables.addAll(Arrays.asList(newVars));


        if (doRaw) {
            String[] newVars1 = new String[] {"RawMaxAmplL1/D",
                    "RawT0L1/D", "RawChisqL1/D", "RawTDiffL1/D", "RawMaxAmplL2/D", "RawT0L2/D", "RawChisqL2/D",
                    "RawTDiffL2/D", "RawMaxAmplL3/D", "RawT0L3/D", "RawChisqL3/D", "RawTDiffL3/D"};
            for (int i = 0; i < newVars1.length; i++) {
                newVars1[i] = prefix + newVars1[i];
            }
            tupleVariables.addAll(Arrays.asList(newVars1));
        }

        if (doTrkExtrap) {
            /*String[] newVars2 = new String[] {
                    "TrkExtrpXAxialTopL0/D", "TrkExtrpXStereoTopL0/D", "TrkExtrpXAxialBotL0/D", "TrkExtrpXStereoBotL0/D", "TrkExtrpYAxialTopL0/D",
                    "TrkExtrpYStereoTopL0/D", "TrkExtrpYAxialBotL0/D", "TrkExtrpYStereoBotL0/D", "TrkExtrpXAxialTopL1/D",
                    "TrkExtrpXStereoTopL1/D", "TrkExtrpXAxialBotL1/D", "TrkExtrpXStereoBotL1/D", "TrkExtrpYAxialTopL1/D",
                    "TrkExtrpYStereoTopL1/D", "TrkExtrpYAxialBotL1/D", "TrkExtrpYStereoBotL1/D", "TrkExtrpXAxialTopL2/D",
                    "TrkExtrpXStereoTopL2/D", "TrkExtrpXAxialBotL2/D", "TrkExtrpXStereoBotL2/D", "TrkExtrpYAxialTopL2/D",
                    "TrkExtrpYStereoTopL2/D", "TrkExtrpYAxialBotL2/D", "TrkExtrpYStereoBotL2/D", "TrkExtrpXAxialTopL3/D",
                    "TrkExtrpXStereoTopL3/D", "TrkExtrpXAxialBotL3/D", "TrkExtrpXStereoBotL3/D", "TrkExtrpYAxialTopL3/D",
                    "TrkExtrpYStereoTopL3/D", "TrkExtrpYAxialBotL3/D", "TrkExtrpYStereoBotL3/D", "TrkExtrpXAxialTopL4/D",
                    "TrkExtrpXStereoTopL4/D", "TrkExtrpXAxialBotL4/D", "TrkExtrpXStereoBotL4/D", "TrkExtrpYAxialTopL4/D",
                    "TrkExtrpYStereoTopL4/D", "TrkExtrpYAxialBotL4/D", "TrkExtrpYStereoBotL4/D", "TrkExtrpXAxialTopL5/D",
                    "TrkExtrpXStereoTopL5/D", "TrkExtrpXAxialBotL5/D", "TrkExtrpXStereoBotL5/D", "TrkExtrpYAxialTopL5/D",
                    "TrkExtrpYStereoTopL5/D", "TrkExtrpYAxialBotL5/D", "TrkExtrpYStereoBotL5/D", "TrkExtrpXAxialTopL6/D",
                    "TrkExtrpXStereoTopL6/D", "TrkExtrpXAxialBotL6/D", "TrkExtrpXStereoBotL6/D", "TrkExtrpYAxialTopL6/D",
                    "TrkExtrpYStereoTopL6/D", "TrkExtrpYAxialBotL6/D", "TrkExtrpYStereoBotL6/D",
                    "TrkExtrpXSensorAxialTopL0/D", "TrkExtrpXSensorStereoTopL0/D", "TrkExtrpXSensorAxialBotL0/D", "TrkExtrpXSensorStereoBotL0/D", "TrkExtrpYSensorAxialTopL0/D",
                    "TrkExtrpYSensorStereoTopL0/D", "TrkExtrpYSensorAxialBotL0/D", "TrkExtrpYSensorStereoBotL0/D", "TrkExtrpXSensorAxialTopL1/D",
                    "TrkExtrpXSensorStereoTopL1/D", "TrkExtrpXSensorAxialBotL1/D", "TrkExtrpXSensorStereoBotL1/D", "TrkExtrpYSensorAxialTopL1/D",
                    "TrkExtrpYSensorStereoTopL1/D", "TrkExtrpYSensorAxialBotL1/D", "TrkExtrpYSensorStereoBotL1/D", "TrkExtrpXSensorAxialTopL2/D",
                    "TrkExtrpXSensorStereoTopL2/D", "TrkExtrpXSensorAxialBotL2/D", "TrkExtrpXSensorStereoBotL2/D", "TrkExtrpYSensorAxialTopL2/D",
                    "TrkExtrpYSensorStereoTopL2/D", "TrkExtrpYSensorAxialBotL2/D", "TrkExtrpYSensorStereoBotL2/D", "TrkExtrpXSensorAxialTopL3/D",
                    "TrkExtrpXSensorStereoTopL3/D", "TrkExtrpXSensorAxialBotL3/D", "TrkExtrpXSensorStereoBotL3/D", "TrkExtrpYSensorAxialTopL3/D",
                    "TrkExtrpYSensorStereoTopL3/D", "TrkExtrpYSensorAxialBotL3/D", "TrkExtrpYSensorStereoBotL3/D", "TrkExtrpXSensorAxialTopL4/D",
                    "TrkExtrpXSensorStereoTopL4/D", "TrkExtrpXSensorAxialBotL4/D", "TrkExtrpXSensorStereoBotL4/D", "TrkExtrpYSensorAxialTopL4/D",
                    "TrkExtrpYSensorStereoTopL4/D", "TrkExtrpYSensorAxialBotL4/D", "TrkExtrpYSensorStereoBotL4/D", "TrkExtrpXSensorAxialTopL5/D",
                    "TrkExtrpXSensorStereoTopL5/D", "TrkExtrpXSensorAxialBotL5/D", "TrkExtrpXSensorStereoBotL5/D", "TrkExtrpYSensorAxialTopL5/D",
                    "TrkExtrpYSensorStereoTopL5/D", "TrkExtrpYSensorAxialBotL5/D", "TrkExtrpYSensorStereoBotL5/D", "TrkExtrpXSensorAxialTopL6/D",
                    "TrkExtrpXSensorStereoTopL6/D", "TrkExtrpXSensorAxialBotL6/D", "TrkExtrpXSensorStereoBotL6/D", "TrkExtrpYSensorAxialTopL6/D",
                    "TrkExtrpYSensorStereoTopL6/D", "TrkExtrpYSensorAxialBotL6/D", "TrkExtrpYSensorStereoBotL6/D"};*/
            /*String[] newVars2 = new String[] {
                    "TrkExtrpXAxialTopL", "TrkExtrpXStereoTopL", "TrkExtrpXAxialBotL", "TrkExtrpXStereoBotL", "TrkExtrpYAxialTopL",
                    "TrkExtrpYStereoTopL", "TrkExtrpYAxialBotL", "TrkExtrpYStereoBotL","TrkExtrpXSensorAxialTopL", "TrkExtrpXSensorStereoTopL", 
                    "TrkExtrpXSensorAxialBotL", "TrkExtrpXSensorStereoBotL", "TrkExtrpYSensorAxialTopL",
                    "TrkExtrpYSensorStereoTopL", "TrkExtrpYSensorAxialBotL", "TrkExtrpYSensorStereoBotL"};*/
            for(int i = 0; i < nTrackingLayers*2; i++){
                String layer = Integer.toString(i + 1);
                String[] newVars2 = new String[] {
                        "TrkExtrpXAxialTopL", "TrkExtrpXStereoTopL", "TrkExtrpXAxialBotL", "TrkExtrpXStereoBotL", 
                        "TrkExtrpYAxialTopL","TrkExtrpYStereoTopL", "TrkExtrpYAxialBotL", "TrkExtrpYStereoBotL",
                        "TrkExtrpXSensorAxialTopL", "TrkExtrpXSensorStereoTopL", "TrkExtrpXSensorAxialBotL", "TrkExtrpXSensorStereoBotL",
                        "TrkExtrpYSensorAxialTopL","TrkExtrpYSensorStereoTopL", "TrkExtrpYSensorAxialBotL", "TrkExtrpYSensorStereoBotL",
                        "TrkExtrpYErrorSensorAxialTopL","TrkExtrpYErrorSensorStereoTopL", "TrkExtrpYErrorSensorAxialBotL", "TrkExtrpYErrorSensorStereoBotL"};
                for(int j = 0; j < newVars2.length; j++){
                    newVars2[j] = prefix + newVars2[j] + layer + "/D";
                }
                tupleVariables.addAll(Arrays.asList(newVars2));
            }
            /*for (int i = 0; i < newVars2.length; i++) {
                newVars2[i] = prefix + newVars2[i];
            }*/
            //tupleVariables.addAll(Arrays.asList(newVars2));
        }

        if (doIso) {
            String[] newVars3 = new String[] {"IsoStereo/D", "IsoAxial/D", "MinPositiveIso/D", "MinNegativeIso/D", "MinNegativeIsoL2/D", "MinPositiveIsoL2/D", "IsoStereoL2/D", "IsoAxialL2/D"};
            for (int i = 0; i < newVars3.length; i++) {
                newVars3[i] = prefix + newVars3[i];
            }
            tupleVariables.addAll(Arrays.asList(newVars3));
        }
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

    protected void fillEventVariablesHits(EventHeader event) {
        if (event.hasCollection(GenericObject.class, "RFHits")) {
            List<GenericObject> rfTimes = event.get(GenericObject.class, "RFHits");
            if (rfTimes.size() > 0) {
                tupleMap.put("rfT1/D", rfTimes.get(0).getDoubleVal(0));
                tupleMap.put("rfT2/D", rfTimes.get(0).getDoubleVal(1));
            }
        }
        
        List<LCRelation> fittedHits = event.get(LCRelation.class, "SVTFittedRawTrackerHits");
        tupleMap.put("nSVTHits/I", (double) fittedHits.size());

        int[] nLhits = {0,0,0,0,0,0};
        int[] nLbhits = {0,0,0,0,0,0};
        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, "SVTRawTrackerHits");
        for (RawTrackerHit rHit : rawHits) {
            HpsSiSensor sensor = (HpsSiSensor) rHit.getDetectorElement();
            int layer = sensor.getLayerNumber();
            int i = ((sensor.getLayerNumber() + 1) / 2) - 1;
            
            if (layer % 2 == 0) {
                //bottom hit
                nLbhits[i]++;
            }
            else {
                nLhits[i]++;
            }
        }

        for (int k = 1; k<7; k++) {
            String putMe = String.format("nSVTHitsL%d/I", k);
            tupleMap.put(putMe, (double) nLhits[k-1]);
            putMe = String.format("nSVTHitsL%db/I", k);
            tupleMap.put(putMe, (double) nLbhits[k-1]);
        }
        
        double topL1HitX = 9999;
        double topL1HitY = 9999;
        double botL1HitX = 9999;
        double botL1HitY = -9999;

        // Get the collection of 3D hits from the event. This collection
        // contains all 3D hits in the event and not just those associated
        // with a track.
        List<TrackerHit> hits = event.get(TrackerHit.class, "RotatedHelicalTrackHits");
        tupleMap.put("n3DSVTHits/I", (double) hits.size());

        // Loop over the collection of 3D hits in the event and map them to
        // their corresponding layer.
        for (TrackerHit hit : hits) {
            // Retrieve the sensor associated with one of the hits. This will
            // be used to retrieve the layer number
            HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement();

            // Retrieve the layer number by using the sensor
            int layer = (sensor.getLayerNumber() + 1) / 2;

            // If hit isn't in layer one, skip it.
            // You can also create another list which contains just layer 1 hits ...
            if (layer != 1)
                continue;

            if (sensor.isTopLayer() && topL1HitY > hit.getPosition()[2]) {
                topL1HitY = hit.getPosition()[2];
                topL1HitX = hit.getPosition()[1];

            }
            if (sensor.isBottomLayer() && botL1HitY < hit.getPosition()[2]) {
                botL1HitY = hit.getPosition()[2];
                botL1HitX = hit.getPosition()[1];

            }
        }
        tupleMap.put("topL1HitX/D", topL1HitX);
        tupleMap.put("topL1HitY/D", topL1HitY);
        tupleMap.put("botL1HitX/D", botL1HitX);
        tupleMap.put("botL1HitY/D", botL1HitY);
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
    
    protected void fillEventVariables(EventHeader event, TIData triggerData) {
        
        tupleMap.put("run/I", (double) event.getRunNumber());
        tupleMap.put("event/I", (double) event.getEventNumber());
        tupleMap.put("tupleevent/I", (double) tupleevent);
        tupleevent++;
        List<ReconstructedParticle> fspList = event.get(ReconstructedParticle.class, finalStateParticlesColName);
        int npos = 0;
        int ncl = 0;
        for (ReconstructedParticle fsp : fspList) {
            if (isGBL != TrackType.isGBL(fsp.getType())) {
                continue;
            }
            if (fsp.getCharge() > 0) {
                npos++;
            }
            ncl = fsp.getClusters().size();
        }
        tupleMap.put("nPos/I", (double) npos);
        tupleMap.put("nCl/I", (double) ncl);

        fillEventVariablesTrigger(event, triggerData);
        fillEventVariablesECal(event);
        fillEventVariablesHits(event);

    }

    protected TrackState fillParticleVariables(EventHeader event, ReconstructedParticle particle, String prefix) {
        return fillParticleVariables(event, particle, prefix, true, true, true);
    }
    
    private void tupleMapTrkExtrap(int lay, String prefix) {
        String putMe = String.format("%sTrkExtrpXAxialTopL%d/D", prefix, 7-lay);
        tupleMap.put(putMe, extrapTrackXTopAxial[nLay-lay]);        
        putMe = String.format("%sTrkExtrpYAxialTopL%d/D", prefix, 7-lay);
        tupleMap.put(putMe, extrapTrackYTopAxial[nLay-lay]);
        putMe = String.format("%sTrkExtrpXStereoTopL%d/D", prefix, 7-lay);
        tupleMap.put(putMe, extrapTrackXTopStereo[nLay-lay]);
        putMe = String.format("%sTrkExtrpYStereoTopL%d/D", prefix, 7-lay);
        tupleMap.put(putMe, extrapTrackYTopStereo[nLay-lay]);
        putMe = String.format("%sTrkExtrpXAxialBotL%d/D", prefix, 7-lay);
        tupleMap.put(putMe, extrapTrackXBotAxial[nLay-lay]);
        putMe = String.format("%sTrkExtrpYAxialBotL%d/D", prefix, 7-lay);
        tupleMap.put(putMe, extrapTrackYBotAxial[nLay-lay]);
        putMe = String.format("%sTrkExtrpXStereoBotL%d/D", prefix, 7-lay);
        tupleMap.put(putMe, extrapTrackXBotStereo[nLay-lay]);
        putMe = String.format("%sTrkExtrpYStereoBotL%d/D", prefix, 7-lay);
        tupleMap.put(putMe, extrapTrackYBotStereo[nLay-lay]);
        
        String putMe2 = String.format("%sTrkExtrpXSensorAxialTopL%d/D", prefix, 7-lay);
        tupleMap.put(putMe2, extrapTrackXSensorTopAxial[nLay-lay]);        
        putMe2 = String.format("%sTrkExtrpYSensorAxialTopL%d/D", prefix, 7-lay);
        tupleMap.put(putMe2, extrapTrackYSensorTopAxial[nLay-lay]);
        putMe2 = String.format("%sTrkExtrpXSensorStereoTopL%d/D", prefix, 7-lay);
        tupleMap.put(putMe2, extrapTrackXSensorTopStereo[nLay-lay]);
        putMe2 = String.format("%sTrkExtrpYSensorStereoTopL%d/D", prefix, 7-lay);
        tupleMap.put(putMe2, extrapTrackYSensorTopStereo[nLay-lay]);
        putMe2 = String.format("%sTrkExtrpXSensorAxialBotL%d/D", prefix, 7-lay);
        tupleMap.put(putMe2, extrapTrackXSensorBotAxial[nLay-lay]);
        putMe2 = String.format("%sTrkExtrpYSensorAxialBotL%d/D", prefix, 7-lay);
        tupleMap.put(putMe2, extrapTrackYSensorBotAxial[nLay-lay]);
        putMe2 = String.format("%sTrkExtrpXSensorStereoBotL%d/D", prefix, 7-lay);
        tupleMap.put(putMe2, extrapTrackXSensorBotStereo[nLay-lay]);
        putMe2 = String.format("%sTrkExtrpYSensorStereoBotL%d/D", prefix, 7-lay);
        tupleMap.put(putMe2, extrapTrackYSensorBotStereo[nLay-lay]);
   
        String putMe3 = String.format("%sTrkExtrpYErrorSensorAxialTopL%d/D", prefix, 7-lay);
        tupleMap.put(putMe3, extrapTrackYErrorSensorTopAxial[nLay-lay]);
        putMe3 = String.format("%sTrkExtrpYErrorSensorStereoTopL%d/D", prefix, 7-lay);
        tupleMap.put(putMe3, extrapTrackYErrorSensorTopStereo[nLay-lay]);
        putMe3 = String.format("%sTrkExtrpYErrorSensorAxialBotL%d/D", prefix, 7-lay);
        tupleMap.put(putMe3, extrapTrackYErrorSensorBotAxial[nLay-lay]);
        putMe3 = String.format("%sTrkExtrpYErrorSensorStereoBotL%d/D", prefix, 7-lay);
        tupleMap.put(putMe3, extrapTrackYErrorSensorBotStereo[nLay-lay]);
    }

    private void fillParticleVariablesTrkExtrap(String prefix, Track track) {
        TrackState trackState = track.getTrackStates().get(0);
        extrapTrackXTopAxial = new double[nLay];
        extrapTrackXTopStereo = new double[nLay];
        extrapTrackXBotAxial = new double[nLay];
        extrapTrackXBotStereo = new double[nLay];
        extrapTrackYTopAxial = new double[nLay];
        extrapTrackYTopStereo = new double[nLay];
        extrapTrackYBotAxial = new double[nLay];
        extrapTrackYBotStereo = new double[nLay];
        
        extrapTrackXSensorTopAxial = new double[nLay];
        extrapTrackXSensorTopStereo = new double[nLay];
        extrapTrackXSensorBotAxial = new double[nLay];
        extrapTrackXSensorBotStereo = new double[nLay];
        extrapTrackYSensorTopAxial = new double[nLay];
        extrapTrackYSensorTopStereo = new double[nLay];
        extrapTrackYSensorBotAxial = new double[nLay];
        extrapTrackYSensorBotStereo = new double[nLay];
        
        extrapTrackYErrorSensorTopAxial = new double[nLay];
        extrapTrackYErrorSensorTopStereo = new double[nLay];
        extrapTrackYErrorSensorBotAxial = new double[nLay];
        extrapTrackYErrorSensorBotStereo = new double[nLay];
        
        // initialize
        for (int i=0; i<nLay; i++) {
            extrapTrackXTopAxial[i] = -9999;
            extrapTrackXTopStereo[i] = -9999;
            extrapTrackXBotAxial[i] = -9999;
            extrapTrackXBotStereo[i] = -9999;
            extrapTrackYTopAxial[i] = -9999;
            extrapTrackYTopStereo[i] = -9999;
            extrapTrackYBotAxial[i] = -9999;
            extrapTrackYBotStereo[i] = -9999;
            
            extrapTrackXSensorTopAxial[i] = -9999;
            extrapTrackXSensorTopStereo[i] = -9999;
            extrapTrackXSensorBotAxial[i] = -9999;
            extrapTrackXSensorBotStereo[i] = -9999;
            extrapTrackYSensorTopAxial[i] = -9999;
            extrapTrackYSensorTopStereo[i] = -9999;
            extrapTrackYSensorBotAxial[i] = -9999;
            extrapTrackYSensorBotStereo[i] = -9999;
            
            extrapTrackYErrorSensorTopAxial[i] = -9999;
            extrapTrackYErrorSensorTopStereo[i] = -9999;
            extrapTrackYErrorSensorBotAxial[i] = -9999;
            extrapTrackYErrorSensorBotStereo[i] = -9999;
        }

        for (HpsSiSensor sensor : sensors) {
            int i = ((sensor.getLayerNumber() + 1) / 2) - 1;

            // try using TrackState at sensor
            Hep3Vector extrapPos = null;
            if ((trackState.getTanLambda() > 0 && sensor.isTopLayer()) || (trackState.getTanLambda() < 0 && sensor.isBottomLayer())) {
                extrapPos = TrackUtils.extrapolateTrackPositionToSensor(track, sensor, sensors, bfield);
            }

            if (extrapPos != null) {
                if (trackState.getTanLambda() > 0 && sensor.isTopLayer()) {
                    if (sensor.isAxial()) {
                        extrapTrackXTopAxial[i] = extrapPos.x();
                        extrapTrackYTopAxial[i] = extrapPos.y();
                        extrapTrackXSensorTopAxial[i] = TrackUtils.globalToSensor(extrapPos,sensor).y();
                        extrapTrackYSensorTopAxial[i] = TrackUtils.globalToSensor(extrapPos,sensor).x();
                        extrapTrackYErrorSensorTopAxial[i] = computeExtrapErrorY(track);
                    } else {
                        extrapTrackXTopStereo[i] = extrapPos.x();
                        extrapTrackYTopStereo[i] = extrapPos.y();
                        extrapTrackXSensorTopStereo[i] = TrackUtils.globalToSensor(extrapPos,sensor).y();
                        extrapTrackYSensorTopStereo[i] = TrackUtils.globalToSensor(extrapPos,sensor).x();
                        extrapTrackYErrorSensorTopStereo[i] = computeExtrapErrorY(track);
                    }
                }
                if (trackState.getTanLambda() < 0 && sensor.isBottomLayer()) {
                    if (sensor.isAxial()) {
                        extrapTrackXBotAxial[i] = extrapPos.x();
                        extrapTrackYBotAxial[i] = extrapPos.y();
                        extrapTrackXSensorBotAxial[i] = TrackUtils.globalToSensor(extrapPos,sensor).y();
                        extrapTrackYSensorBotAxial[i] = TrackUtils.globalToSensor(extrapPos,sensor).x();
                        extrapTrackYErrorSensorBotAxial[i] = computeExtrapErrorY(track);
                    } else {
                        extrapTrackXBotStereo[i] = extrapPos.x();
                        extrapTrackYBotStereo[i] = extrapPos.y();
                        extrapTrackXSensorBotStereo[i] = TrackUtils.globalToSensor(extrapPos,sensor).y();
                        extrapTrackYSensorBotStereo[i] = TrackUtils.globalToSensor(extrapPos,sensor).x();
                        extrapTrackYErrorSensorBotStereo[i] = computeExtrapErrorY(track);
                    }
                }
            }
        }

        for (int i=nLay;i>0;i--) {
            tupleMapTrkExtrap(i, prefix);
        }
    }
    
    //Computes track extrapolation error in sensor frame
    //This probably needs to be fixed
    private double computeExtrapErrorY(Track track){
        TrackState tState = track.getTrackStates().get(0);
        HelicalTrackFit hlc_trk_fit = TrackUtils.getHTF(tState);
        double p = hlc_trk_fit.p(bfield);;
        double beta = 1;
        double z = 1;
        double t = 0.007;
        double d = 100;
        double error = 0.0136/(beta*p)*z*Math.sqrt(t)*(1+0.038*Math.log(t));
        return error * d;
    }

    private void fillParticleVariablesIso(String prefix, Hep3Vector pRot, Double[] iso) {

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
                // break;
            }
            if (iso[2 * i + 2] != null) {
                if (pRot.y() < 0) {
                    isoStereoL2 = iso[2 * i + 2];
                    isoAxialL2 = iso[2 * i + 3];
                } else {
                    isoStereoL2 = iso[2 * i + 3];
                    isoAxialL2 = iso[2 * i + 2];
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

        tupleMap.put(prefix + "IsoStereo/D", isoStereo);
        tupleMap.put(prefix + "IsoAxial/D", isoAxial);
        tupleMap.put(prefix + "IsoStereoL2/D", isoStereoL2);
        tupleMap.put(prefix + "IsoAxialL2/D", isoAxialL2);
        tupleMap.put(prefix + "MinPositiveIso/D", minPositiveIso);
        tupleMap.put(prefix + "MinNegativeIso/D", minNegativeIso);
        tupleMap.put(prefix + "MinPositiveIsoL2/D", minPositiveIsoL2);
        tupleMap.put(prefix + "MinNegativeIsoL2/D", minNegativeIsoL2);
    }

    private void fillParticleVariablesRaw(String prefix, List<TrackerHit> allTrackHits, Map<RawTrackerHit, LCRelation> fittedRawTrackerHitMap) {
        double rawHitTime[] = new double[nLay];
        double rawHitTDiff[] = new double[nLay];
        double rawHitMaxAmpl[] = new double[nLay];
        double rawHitChisq[] = new double[nLay];

        int nTrackHits = 0;
        for (TrackerHit iTrackHit : allTrackHits) {
            List<RawTrackerHit> allRawHits = iTrackHit.getRawHits();

            int sz = 0;
            double t0 = 0;
            double amplmax = 0;
            double chi2 = 0;
            double t0min = 0;
            double t0max = 0;
            for (RawTrackerHit iRawHit : allRawHits) {
                // 0=T0, 1=T0 error, 2=amplitude, 3=amplitude error, 4=chi2 of fit
                GenericObject fitPar = FittedRawTrackerHit.getShapeFitParameters(fittedRawTrackerHitMap
                        .get(iRawHit));
                sz++;
                if (sz == 1) {
                    t0min = fitPar.getDoubleVal(0);
                    t0max = fitPar.getDoubleVal(0);
                }
                if (t0min > fitPar.getDoubleVal(0)) {
                    t0min = fitPar.getDoubleVal(0);
                }
                if (t0max < fitPar.getDoubleVal(0)) {
                    t0max = fitPar.getDoubleVal(0);
                }
                if (amplmax < fitPar.getDoubleVal(2)) {
                    amplmax = fitPar.getDoubleVal(2);
                    chi2 = fitPar.getDoubleVal(4);
                    t0 = fitPar.getDoubleVal(0);

                }// end if
            }  // end loop over raw hits

            rawHitTime[nTrackHits] = t0;
            rawHitTDiff[nTrackHits] = t0max - t0min;
            rawHitMaxAmpl[nTrackHits] = amplmax;
            rawHitChisq[nTrackHits] = chi2;
            nTrackHits++;
        }// end loop over track hits

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
        
    }

    //Overload this method to get custom kinks to GBL track relation
    protected TrackState fillParticleVariables(EventHeader event, ReconstructedParticle particle, String prefix, boolean doTrkExtrap, boolean doRaw, boolean doIso){
        return fillParticleVariables(event, particle, prefix, doTrkExtrap, doRaw, doIso, "");
    }
    protected TrackState fillParticleVariables(EventHeader event, ReconstructedParticle particle, String prefix, boolean doTrkExtrap, boolean doRaw, boolean doIso, String KinkToGBLRelations) {
        TrackState trackState = null;
        if (particle == null)
            return trackState;
        if (particle.getTracks().isEmpty())
            return trackState;

        List<Track> allTracks = event.get(Track.class, "GBLTracks");
        Track track = particle.getTracks().get(0);
        trackState = track.getTrackStates().get(0);
        double [] cov = trackState.getCovMatrix();
        TrackState baseTrackState = new BaseTrackState(trackState.getParameters(), trackState.getReferencePoint(),
                trackState.getCovMatrix(), trackState.getLocation(), bfield);
        Hep3Vector pRot = VecOp.mult(beamAxisRotation, CoordinateTransformations
                .transformVectorToDetector(new BasicHep3Vector(baseTrackState.getMomentum())));

        if (doTrkExtrap) 
            fillParticleVariablesTrkExtrap(prefix, track);

        if (doIso) {
            Double[] iso = TrackUtils.getIsolations(track, TrackUtils.getHitToStripsTable(event),
                    TrackUtils.getHitToRotatedTable(event), nLay);
            fillParticleVariablesIso(prefix, pRot, iso);
        }

        double trkT = TrackUtils.getTrackTime(track, TrackUtils.getHitToStripsTable(event),
                TrackUtils.getHitToRotatedTable(event));
        double trkTsd = TrackUtils.getTrackTimeSD(track, TrackUtils.getHitToStripsTable(event),
                TrackUtils.getHitToRotatedTable(event));

        // Find track state at ECal, or nearest previous track state
        if (doTrkExtrap) {
            TrackState tsAtEcal = TrackStateUtils.getTrackStateAtECal(track);
            Hep3Vector atEcal = new BasicHep3Vector(Double.NaN, Double.NaN, Double.NaN);
            if (tsAtEcal != null) {
                atEcal = new BasicHep3Vector(tsAtEcal.getReferencePoint());
                atEcal = CoordinateTransformations.transformVectorToDetector(atEcal);
            }
            tupleMap.put(prefix + "TrkEcalX/D", atEcal.x());
            tupleMap.put(prefix + "TrkEcalY/D", atEcal.y());
        }

        Hep3Vector firstHitPosition = VecOp.mult(
                beamAxisRotation,
                CoordinateTransformations.transformVectorToDetector(new BasicHep3Vector(track.getTrackerHits()
                        .get(0).getPosition())));
        GenericObject kinks = GBLKinkData.getKinkData(event, track);
        if(KinkToGBLRelations != ""){
            GenericObject truthkinks = RefitTrackTruthTupleDriver.getKinkData(event, track, KinkToGBLRelations);
            if(truthkinks != null)
                kinks = truthkinks;
        }

        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);

        double hitTimes[] = new double[2];
        double hitdEdx[] = new double[2];
        int hitClusterSize[] = new int[2];

        TrackerHit hit = track.getTrackerHits().get(0);
        Collection<TrackerHit> htsList = hitToStrips.allFrom(hitToRotated.from(hit));
        for (TrackerHit hts : htsList) {
            int layer = ((HpsSiSensor) ((RawTrackerHit) hts.getRawHits().get(0)).getDetectorElement())
                    .getLayerNumber();
            hitTimes[layer % 2] = hts.getTime();
            hitdEdx[layer % 2] = hts.getdEdx();
            hitClusterSize[layer % 2] = hts.getRawHits().size();
        }

        List<TrackerHit> allTrackHits = track.getTrackerHits();
        int nTrackHits = allTrackHits.size();
        boolean[] hasHits = {false, false, false, false, false, false};
        for (TrackerHit temp : allTrackHits) {
            // Retrieve the sensor associated with one of the hits. This will
            // be used to retrieve the layer number
            HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) temp.getRawHits().get(0)).getDetectorElement();

            // Retrieve the layer number by using the sensor
            int layer = (sensor.getLayerNumber() + 1) / 2 -1;
            hasHits[layer] = true;
        }

        // ////////////////////////////////////////////////////////////////////////
        if (doRaw) {
            // Get the list of fitted hits from the event
            List<LCRelation> fittedHits = event.get(LCRelation.class, "SVTFittedRawTrackerHits");

            // Map the fitted hits to their corresponding raw hits
            Map<RawTrackerHit, LCRelation> fittedRawTrackerHitMap = new HashMap<RawTrackerHit, LCRelation>();

            for (LCRelation fittedHit : fittedHits) {
                fittedRawTrackerHitMap.put(FittedRawTrackerHit.getRawTrackerHit(fittedHit), fittedHit);
            }
            fillParticleVariablesRaw(prefix, allTrackHits, fittedRawTrackerHitMap);
        }

        // shared
        Track trackShared = TrackUtils.mostSharedHitTrack(track, allTracks);
        TrackState trackStateShared = trackShared.getTrackStates().get(0);
        TrackState baseTrackStateShared = new BaseTrackState(trackStateShared.getParameters(), trackStateShared.getReferencePoint(),
                trackStateShared.getCovMatrix(), trackStateShared.getLocation(), bfield);
        Hep3Vector pRotShared = VecOp.mult(beamAxisRotation, CoordinateTransformations
                .transformVectorToDetector(new BasicHep3Vector(baseTrackStateShared.getMomentum())));
        double momentumOfShared = pRotShared.magnitude();
        int maxShared = TrackUtils.numberOfSharedHits(track, trackShared);

        // shared at ECal
        if (doTrkExtrap) {
            TrackState tsAtEcal = TrackStateUtils.getTrackStateAtECal(trackShared);
            Hep3Vector atEcalShared = new BasicHep3Vector(Double.NaN, Double.NaN, Double.NaN);
            if (tsAtEcal != null) {
                atEcalShared = new BasicHep3Vector(tsAtEcal.getReferencePoint());
                atEcalShared = CoordinateTransformations.transformVectorToDetector(atEcalShared);
            }
            tupleMap.put(prefix + "SharedTrkEcalX/D", atEcalShared.x());
            tupleMap.put(prefix + "SharedTrkEcalY/D", atEcalShared.y());
        }

        tupleMap.put(prefix + "NTrackHits/I", (double) nTrackHits);

        tupleMap.put(prefix + "PX/D", pRot.x());
        tupleMap.put(prefix + "PY/D", pRot.y());
        tupleMap.put(prefix + "PZ/D", pRot.z());
        tupleMap.put(prefix + "P/D", pRot.magnitude());
        tupleMap.put(prefix + "TrkZ0/D", trackState.getZ0());
        tupleMap.put(prefix + "TrkLambda/D", trackState.getTanLambda());
        tupleMap.put(prefix + "TrkD0/D", trackState.getD0());
        tupleMap.put(prefix + "TrkPhi/D", trackState.getPhi());
        tupleMap.put(prefix + "TrkOmega/D", trackState.getOmega());
        tupleMap.put(prefix + "TrkD0Err/D", Math.sqrt(cov[0]));
        tupleMap.put(prefix + "TrkZ0Err/D", Math.sqrt(cov[9]));
        tupleMap.put(prefix + "TrkLambdaErr/D", Math.sqrt(cov[14]));
        tupleMap.put(prefix + "TrkPhiErr/D", Math.sqrt(cov[2]));
        tupleMap.put(prefix + "TrkOmegaErr/D", Math.sqrt(cov[5]));

        tupleMap.put(prefix + "TrkChisq/D", track.getChi2());
        tupleMap.put(prefix + "TrkHits/I", (double) track.getTrackerHits().size());
        tupleMap.put(prefix + "TrkType/I", (double) particle.getType());
        tupleMap.put(prefix + "TrkT/D", trkT);
        tupleMap.put(prefix + "TrkTsd/D", trkTsd);
        tupleMap.put(prefix + "HasL1/B", hasHits[0] ? 1.0 : 0.0);
        tupleMap.put(prefix + "HasL2/B", hasHits[1] ? 1.0 : 0.0);
        tupleMap.put(prefix + "HasL3/B", hasHits[2] ? 1.0 : 0.0);
        tupleMap.put(prefix + "HasL4/B", hasHits[3] ? 1.0 : 0.0);
        tupleMap.put(prefix + "HasL5/B", hasHits[4] ? 1.0 : 0.0);
        tupleMap.put(prefix + "HasL6/B", hasHits[5] ? 1.0 : 0.0);
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

        tupleMap.put(prefix + "LambdaKink0/D", kinks != null ? GBLKinkData.getLambdaKink(kinks, 0) : 0);
        tupleMap.put(prefix + "LambdaKink1/D", kinks != null ? GBLKinkData.getLambdaKink(kinks, 1) : 0);
        tupleMap.put(prefix + "LambdaKink2/D", kinks != null ? GBLKinkData.getLambdaKink(kinks, 2) : 0);
        tupleMap.put(prefix + "LambdaKink3/D", kinks != null ? GBLKinkData.getLambdaKink(kinks, 3) : 0);
        tupleMap.put(prefix + "LambdaKink4/D", kinks != null ? GBLKinkData.getLambdaKink(kinks, 4) : 0);
        tupleMap.put(prefix + "LambdaKink5/D", kinks != null ? GBLKinkData.getLambdaKink(kinks, 5) : 0);
        tupleMap.put(prefix + "LambdaKink6/D", kinks != null ? GBLKinkData.getLambdaKink(kinks, 6) : 0);
        tupleMap.put(prefix + "PhiKink0/D", kinks != null ? GBLKinkData.getPhiKink(kinks, 0) : 0);
        tupleMap.put(prefix + "PhiKink1/D", kinks != null ? GBLKinkData.getPhiKink(kinks, 1) : 0);
        tupleMap.put(prefix + "PhiKink2/D", kinks != null ? GBLKinkData.getPhiKink(kinks, 2) : 0);
        tupleMap.put(prefix + "PhiKink3/D", kinks != null ? GBLKinkData.getPhiKink(kinks, 3) : 0);
        tupleMap.put(prefix + "PhiKink4/D", kinks != null ? GBLKinkData.getPhiKink(kinks, 4) : 0);
        tupleMap.put(prefix + "PhiKink5/D", kinks != null ? GBLKinkData.getPhiKink(kinks, 5) : 0);
        tupleMap.put(prefix + "PhiKink6/D", kinks != null ? GBLKinkData.getPhiKink(kinks, 6) : 0);

        tupleMap.put(prefix + "MatchChisq/D", particle.getGoodnessOfPID());

        if (!particle.getClusters().isEmpty()) {
            fillParticleVariablesClusters(prefix, particle, event);
        }

        return trackState;
    }

    private void fillParticleVariablesClusters(String prefix, ReconstructedParticle particle, EventHeader event) {
        if (particle == null)
            return;
        Cluster cluster = particle.getClusters().get(0);
        if (cluster==null)
            return;
        
        tupleMap.put(prefix + "ClT/D", ClusterUtilities.getSeedHitTime(cluster));
        tupleMap.put(prefix + "ClE/D", cluster.getEnergy());
        tupleMap.put(prefix + "ClSeedE/D", ClusterUtilities.findSeedHit(cluster).getCorrectedEnergy());
        tupleMap.put(prefix + "ClX/D", cluster.getPosition()[0]);
        tupleMap.put(prefix + "ClY/D", cluster.getPosition()[1]);
        tupleMap.put(prefix + "ClZ/D", cluster.getPosition()[2]);
        tupleMap.put(prefix + "ClHits/I", (double) cluster.getCalorimeterHits().size());
        tupleMap.put(prefix + "Clix/I", (double) ClusterUtilities.findSeedHit(cluster)
                .getIdentifierFieldValue("ix"));
        tupleMap.put(prefix + "Cliy/I", (double) ClusterUtilities.findSeedHit(cluster)
                .getIdentifierFieldValue("iy"));

        // find the uncorrected cluster corresponding to this cluster
        Cluster uncorrCluster = null;
        for (Cluster clust : event.get(Cluster.class, "EcalClusters")) {
            if (clust.getCalorimeterHits().get(0).getCellID() == cluster.getCalorimeterHits().get(0).getCellID()) {
                uncorrCluster = clust;
                break;
            }
        }
        if (uncorrCluster != null) {
            tupleMap.put(prefix + "UncorrClT/D", ClusterUtilities.getSeedHitTime(uncorrCluster));
            tupleMap.put(prefix + "UncorrClE/D", uncorrCluster.getEnergy());
            tupleMap.put(prefix + "UncorrClX/D", uncorrCluster.getPosition()[0]);
            tupleMap.put(prefix + "UncorrClY/D", uncorrCluster.getPosition()[1]);
            tupleMap.put(prefix + "UncorrClZ/D", uncorrCluster.getPosition()[2]);
        }
    }


    protected void fillVertexCov(String prefix, ReconstructedParticle theV0) {
        if (theV0 == null)
            return;
        
        Matrix theCov = MatrixOp.mult(MatrixOp.mult(beamAxisRotation, theV0.getStartVertex().getCovMatrix()),
                MatrixOp.transposed(beamAxisRotation));

        tupleMap.put(prefix + "CovXX/D", theCov.e(0, 0));
        tupleMap.put(prefix + "CovXY/D", theCov.e(0, 1));
        tupleMap.put(prefix + "CovXZ/D", theCov.e(0, 2));
        tupleMap.put(prefix + "CovYX/D", theCov.e(1, 0));
        tupleMap.put(prefix + "CovYY/D", theCov.e(1, 1));
        tupleMap.put(prefix + "CovYZ/D", theCov.e(1, 2));
        tupleMap.put(prefix + "CovZX/D", theCov.e(2, 0));
        tupleMap.put(prefix + "CovZY/D", theCov.e(2, 1));
        tupleMap.put(prefix + "CovZZ/D", theCov.e(2, 2));
    }
    
    protected void fillVertexVariables(String prefix, ReconstructedParticle theV0, boolean isMoller) {
        String[] mollerParticleNames = {"Top", "Bot"};
        String[] v0ParticleNames = {"Ele", "Pos"};
        
        if (theV0 == null)
            return;
        
        BilliorVertex vtxFit = null;
        if(cand2vert != null){
            if(cand2vert.containsKey(theV0)){
                vtxFit = cand2vert.get(theV0);
            }
        }

        if(vtxFit != null){
            Hep3Vector v0Pos = vtxFit.getPosition();

            tupleMap.put(prefix + "PosX/D", v0Pos.x());
            tupleMap.put(prefix + "PosY/D", v0Pos.y());
            tupleMap.put(prefix + "PosZ/D", v0Pos.z());
            tupleMap.put(prefix + "Mom/D", vtxFit.getParameters().get("V0P"));
            tupleMap.put(prefix + "MomX/D", vtxFit.getParameters().get("V0Px"));
            tupleMap.put(prefix + "MomY/D", vtxFit.getParameters().get("V0Py"));
            tupleMap.put(prefix + "MomZ/D", vtxFit.getParameters().get("V0Pz"));
            tupleMap.put(prefix + "MomErr/D", vtxFit.getParameters().get("V0PErr"));
            tupleMap.put(prefix + "MomXErr/D", vtxFit.getParameters().get("V0PxErr"));
            tupleMap.put(prefix + "MomYErr/D", vtxFit.getParameters().get("V0PyErr"));
            tupleMap.put(prefix + "MomZErr/D", vtxFit.getParameters().get("V0PzErr"));
            tupleMap.put(prefix + "TargProjX/D", vtxFit.getParameters().get("V0TargProjX"));
            tupleMap.put(prefix + "TargProjY/D", vtxFit.getParameters().get("V0TargProjY"));
            tupleMap.put(prefix + "TargProjXErr/D", vtxFit.getParameters().get("V0TargProjXErr"));
            tupleMap.put(prefix + "TargProjYErr/D", vtxFit.getParameters().get("V0TargProjYErr"));
            tupleMap.put(prefix + "MErr/D", vtxFit.getParameters().get("invMassError"));
        }
        
        fillVertexCov(prefix, theV0);
        
        ReconstructedParticle particle1 = theV0.getParticles().get(0); //v0:  electron,   moller:  top
        ReconstructedParticle particle2 = theV0.getParticles().get(1); //v0:  positron,   moller:  bot
        int nClusters1 = particle1.getClusters().size();
        int nClusters2 = particle2.getClusters().size();
        
        Hep3Vector momRot = VecOp.mult(beamAxisRotation, theV0.getMomentum());
        Hep3Vector theVtx = VecOp.mult(beamAxisRotation, theV0.getStartVertex().getPosition()); 

        tupleMap.put(prefix + "PX/D", momRot.x());
        tupleMap.put(prefix + "PY/D", momRot.y());
        tupleMap.put(prefix + "PZ/D", momRot.z());
        tupleMap.put(prefix + "P/D", momRot.magnitude());
        tupleMap.put(prefix + "VX/D", theVtx.x());
        tupleMap.put(prefix + "VY/D", theVtx.y());
        tupleMap.put(prefix + "VZ/D", theVtx.z());
        tupleMap.put(prefix + "Chisq/D", theV0.getStartVertex().getChi2());
        tupleMap.put(prefix + "M/D", theV0.getMass());
        tupleMap.put(prefix + "ChisqProb/D", theV0.getStartVertex().getProbability());
        
        String particleNames[] = isMoller ? mollerParticleNames : v0ParticleNames;
        
        
        tupleMap.put(prefix + particleNames[0] + "PX/D", theV0.getStartVertex().getParameters().get("p1X"));
        tupleMap.put(prefix + particleNames[0] + "PY/D", theV0.getStartVertex().getParameters().get("p1Y"));
        tupleMap.put(prefix + particleNames[0] + "PZ/D", theV0.getStartVertex().getParameters().get("p1Z"));
        tupleMap.put(
                prefix + particleNames[0] + "P/D",
                Math.sqrt(Math.pow(theV0.getStartVertex().getParameters().get("p1X"), 2)
                        + Math.pow(theV0.getStartVertex().getParameters().get("p1Y"), 2)
                        + Math.pow(theV0.getStartVertex().getParameters().get("p1Z"), 2)));
        tupleMap.put(prefix+particleNames[1] + "PX/D", theV0.getStartVertex().getParameters().get("p2X"));
        tupleMap.put(prefix+particleNames[1] + "PY/D", theV0.getStartVertex().getParameters().get("p2Y"));
        tupleMap.put(prefix+particleNames[1] + "PZ/D", theV0.getStartVertex().getParameters().get("p2Z"));
        tupleMap.put(
                prefix+particleNames[1] + "P/D",
                Math.sqrt(Math.pow(theV0.getStartVertex().getParameters().get("p2X"), 2)
                        + Math.pow(theV0.getStartVertex().getParameters().get("p2Y"), 2)
                        + Math.pow(theV0.getStartVertex().getParameters().get("p2Z"), 2)));

        if (nClusters1>0) {
            tupleMap.put(
                    prefix+particleNames[0] + "WtP/D",
                    MassCalculator.combinedMomentum(
                            particle1.getClusters().get(0),
                            particle1.getTracks().get(0),
                            Math.sqrt(Math.pow(theV0.getStartVertex().getParameters().get("p1X"), 2)
                                    + Math.pow(theV0.getStartVertex().getParameters().get("p1Y"), 2)
                                    + Math.pow(theV0.getStartVertex().getParameters().get("p1Z"), 2))));

            if (nClusters2>0) {
                tupleMap.put(prefix+particleNames[1] + "WtP/D", MassCalculator.combinedMomentum(particle2.getClusters().get(0), particle2
                        .getTracks().get(0), Math.sqrt(Math.pow(theV0.getStartVertex().getParameters().get("p2X"), 2)
                                + Math.pow(theV0.getStartVertex().getParameters().get("p2Y"), 2)
                                + Math.pow(theV0.getStartVertex().getParameters().get("p2Z"), 2))));
                tupleMap.put(prefix+"WtM/D", MassCalculator.combinedMass(particle1.getClusters().get(0), particle2
                        .getClusters().get(0), theV0));
            }
            else {
                tupleMap.put(
                        prefix+particleNames[1] + "WtP/D",
                        Math.sqrt(Math.pow(theV0.getStartVertex().getParameters().get("p2X"), 2)
                                + Math.pow(theV0.getStartVertex().getParameters().get("p2Y"), 2)
                                + Math.pow(theV0.getStartVertex().getParameters().get("p2Z"), 2)));
                tupleMap.put(prefix+"WtM/D",
                        MassCalculator.combinedMass(particle1.getClusters().get(0), particle2.getTracks().get(0), theV0));
            }
        }
        if (nClusters2 > 0 && nClusters1 == 0) {// e+ has cluster, e- does not

            tupleMap.put(
                    prefix+particleNames[1] + "WtP/D",
                    MassCalculator.combinedMomentum(
                            particle2.getClusters().get(0),
                            particle2.getTracks().get(0),
                            Math.sqrt(Math.pow(theV0.getStartVertex().getParameters().get("p2X"), 2)
                                    + Math.pow(theV0.getStartVertex().getParameters().get("p2Y"), 2)
                                    + Math.pow(theV0.getStartVertex().getParameters().get("p2Z"), 2))));
            tupleMap.put(
                    prefix+particleNames[0] + "WtP/D",
                    Math.sqrt(Math.pow(theV0.getStartVertex().getParameters().get("p1X"), 2)
                            + Math.pow(theV0.getStartVertex().getParameters().get("p1Y"), 2)
                            + Math.pow(theV0.getStartVertex().getParameters().get("p1Z"), 2)));
            tupleMap.put(prefix+"WtM/D",
                    MassCalculator.combinedMass(particle1.getTracks().get(0), particle2.getClusters().get(0), theV0));
        }
        if (nClusters2 == 0 && nClusters1 == 0) {

            tupleMap.put(
                    prefix+particleNames[0] + "WtP/D",
                    Math.sqrt(Math.pow(theV0.getStartVertex().getParameters().get("p1X"), 2)
                            + Math.pow(theV0.getStartVertex().getParameters().get("p1Y"), 2)
                            + Math.pow(theV0.getStartVertex().getParameters().get("p1Z"), 2)));
            tupleMap.put(
                    prefix+particleNames[1] + "WtP/D",
                    Math.sqrt(Math.pow(theV0.getStartVertex().getParameters().get("p2X"), 2)
                            + Math.pow(theV0.getStartVertex().getParameters().get("p2Y"), 2)
                            + Math.pow(theV0.getStartVertex().getParameters().get("p2Z"), 2)));
            tupleMap.put(prefix+"WtM/D", theV0.getMass());
        }    
    }

    /**
     * find the v0 (or moller) candidate in one collection that 
     * corresponds to the v0 (or moller) candidate in the other collection
     * @param list1
     * @param list2
     * @return
     */
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
}
