package org.hps.analysis.tuple;

import hep.physics.matrix.Matrix;
import hep.physics.matrix.MatrixOp;
import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.HepLorentzVector;
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
import org.hps.analysis.MC.MCFullDetectorTruth;
import org.hps.analysis.MC.TrackTruthMatching;
import org.hps.conditions.beam.BeamEnergy;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.particle.HpsReconParticleDriver;
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.recon.tracking.TrackStateUtils;
//import org.hps.recon.tracking.TrackStateUtils;
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
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.util.Driver;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.TrackerHit;

import java.util.Collection;
import java.util.Map.Entry;

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

    protected String triggerType = "all";// allowed types are "" (blank) or "all", singles0, singles1, pairs0,pairs1
    public boolean isGBL = false;
    private boolean applyBeamRotation = true;

    private final String finalStateParticlesColName = "FinalStateParticles";
    private final String trackHitMCRelationsCollectionName = "RotatedHelicalTrackMCRelations";
    protected double bfield;
    protected FieldMap bFieldMap = null;
    private final double[] beamSize = {0.001, 0.130, 0.050}; // rough estimate from harp scans during engineering run
    // production running
    private final double[] beamPos = {0.0, 0.0, 0.0};
    private final double[] vzcBeamSize = {0.001, 100, 100};
    private static List<HpsSiSensor> sensors;
    private static final String SUBDETECTOR_NAME = "Tracker";
    protected final BasicHep3Matrix beamAxisRotation = BasicHep3Matrix.identity();
    protected double ebeam = Double.NaN;
    private int nLay = 6;
    private int tupleevent = 0;
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
    private int nEcalHit = 3;


    public void setNLay(int nLay) {
        this.nLay = nLay;
    }
    
    public void setNEcalHit(int nEcalHit) {
        this.nEcalHit = nEcalHit;
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
        bFieldMap = detector.getFieldMap();

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
        String[] newVars = new String[] {"run/I", "event/I", "tupleevent/I", "nPos/I", "nCl/I", "isCalib/B", "isPulser/B",
                "isSingle0/B", "isSingle1/B", "isPair0/B", "isPair1/B", "evTime/D", "evTx/I", "evTy/I", "rfT1/D",
                "rfT2/D", "nEcalHits/I", "nSVTHits/I", "nEcalCl/I", "nEcalClele/I", "nEcalClpos/I", "nEcalClpho/I",
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
                "uncChisq/D", "uncM/D", "uncCovXX/D", "uncCovXY/D", "uncCovXZ/D", "uncCovYX/D", "uncCovYY/D",
                "uncCovYZ/D", "uncCovZX/D", "uncCovZY/D", "uncCovZZ/D", "uncElePX/D", "uncElePY/D", "uncElePZ/D",
                "uncPosPX/D", "uncPosPY/D", "uncPosPZ/D", "uncEleP/D", "uncPosP/D", "uncEleWtP/D", "uncPosWtP/D", "uncWtM/D"};
        tupleVariables.addAll(Arrays.asList(newVars));
        if (doBsc) {
            String[] newVars2 = new String[] {"bscPX/D", "bscPY/D", "bscPZ/D", "bscP/D", "bscVX/D", "bscVY/D", "bscVZ/D",
                    "bscChisq/D", "bscM/D", "bscElePX/D", "bscElePY/D", "bscElePZ/D", "bscPosPX/D", "bscPosPY/D", "bscPosPZ/D", "bscEleP/D", "bscPosP/D", 
                    "bscEleWtP/D", "bscPosWtP/D", "bscWtM/D"};
            tupleVariables.addAll(Arrays.asList(newVars2));
        }
        if (doTar) {
            String[] newVars3 = new String[] {"tarPX/D", "tarPY/D", "tarPZ/D", "tarP/D", "tarVX/D", "tarVY/D", "tarVZ/D",
                    "tarChisq/D", "tarM/D", "tarElePX/D", "tarElePY/D", "tarElePZ/D", "tarPosPX/D", "tarPosPY/D", "tarPosPZ/D", "tarEleP/D", "tarPosP/D", "tarEleWtP/D", "tarPosWtP/D", "tarWtM/D"};
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
                "FirstClusterSize1/I", "FirstClusterSize2/I", "NHitsShared/I", "HitsSharedP/D", "LambdaKink1/D",
                "LambdaKink2/D", "LambdaKink3/D", "PhiKink1/D", "PhiKink2/D", "PhiKink3/D", "NTrackHits/I",  
                "HitsSharedP/D", "MaxHitsShared/I", "SharedTrkChisq/D", "SharedTrkEcalX/D", "SharedTrkEcalY/D", "MatchChisq/D", "ClT/D",
                "ClE/D", "ClSeedE/D", "ClX/D", "ClY/D", "ClZ/D", "ClHits/I", "Clix/I", "Cliy/I", "UncorrClT/D",
                "UncorrClE/D", "UncorrClX/D", "UncorrClY/D", "UncorrClZ/D"};
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
            String[] newVars2 = new String[] {
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
                    "TrkExtrpYSensorStereoTopL6/D", "TrkExtrpYSensorAxialBotL6/D", "TrkExtrpYSensorStereoBotL6/D"};
            for (int i = 0; i < newVars2.length; i++) {
                newVars2[i] = prefix + newVars2[i];
            }
            tupleVariables.addAll(Arrays.asList(newVars2));
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
    
    protected void fillTruthEventVariables(EventHeader event) {
        tupleMap.put("run/I", (double) event.getRunNumber());
        tupleMap.put("event/I", (double) event.getEventNumber());
        //tupleMap.put("tupleevent/I", (double) tupleevent);
        //tupleevent++;
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
                    } else {
                        extrapTrackXTopStereo[i] = extrapPos.x();
                        extrapTrackYTopStereo[i] = extrapPos.y();
                        extrapTrackXSensorTopStereo[i] = TrackUtils.globalToSensor(extrapPos,sensor).y();
                        extrapTrackYSensorTopStereo[i] = TrackUtils.globalToSensor(extrapPos,sensor).x();
                    }
                }
                if (trackState.getTanLambda() < 0 && sensor.isBottomLayer()) {
                    if (sensor.isAxial()) {
                        extrapTrackXBotAxial[i] = extrapPos.x();
                        extrapTrackYBotAxial[i] = extrapPos.y();
                        extrapTrackXSensorBotAxial[i] = TrackUtils.globalToSensor(extrapPos,sensor).y();
                        extrapTrackYSensorBotAxial[i] = TrackUtils.globalToSensor(extrapPos,sensor).x();
                    } else {
                        extrapTrackXBotStereo[i] = extrapPos.x();
                        extrapTrackYBotStereo[i] = extrapPos.y();
                        extrapTrackXSensorBotStereo[i] = TrackUtils.globalToSensor(extrapPos,sensor).y();
                        extrapTrackYSensorBotStereo[i] = TrackUtils.globalToSensor(extrapPos,sensor).x();
                    }
                }
            }
        }

        for (int i=nLay;i>0;i--) {
            tupleMapTrkExtrap(i, prefix);
        }
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

    protected TrackState fillParticleVariables(EventHeader event, ReconstructedParticle particle, String prefix, boolean doTrkExtrap, boolean doRaw, boolean doIso) {
        TrackState trackState = null;
        if (particle.getTracks().isEmpty())
            return trackState;

        List<Track> allTracks = event.get(Track.class, "GBLTracks");
        Track track = particle.getTracks().get(0);
        trackState = track.getTrackStates().get(0);
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

        tupleMap.put(prefix + "LambdaKink1/D", kinks != null ? GBLKinkData.getLambdaKink(kinks, 1) : 0);
        tupleMap.put(prefix + "LambdaKink2/D", kinks != null ? GBLKinkData.getLambdaKink(kinks, 2) : 0);
        tupleMap.put(prefix + "LambdaKink3/D", kinks != null ? GBLKinkData.getLambdaKink(kinks, 3) : 0);
        tupleMap.put(prefix + "PhiKink1/D", kinks != null ? GBLKinkData.getPhiKink(kinks, 1) : 0);
        tupleMap.put(prefix + "PhiKink2/D", kinks != null ? GBLKinkData.getPhiKink(kinks, 2) : 0);
        tupleMap.put(prefix + "PhiKink3/D", kinks != null ? GBLKinkData.getPhiKink(kinks, 3) : 0);

        tupleMap.put(prefix + "MatchChisq/D", particle.getGoodnessOfPID());


        if (!particle.getClusters().isEmpty()) {
            fillParticleVariablesClusters(prefix, particle, event);
        }

        return trackState;
    }

    private void fillParticleVariablesClusters(String prefix, ReconstructedParticle particle, EventHeader event) {
        Cluster cluster = particle.getClusters().get(0);
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

    private void fillVertexVariablesHelper(String prefix, BilliorVertexer vtxFitter, List<BilliorTrack> billiorTracks,
            ReconstructedParticle electron, ReconstructedParticle positron, boolean storeCov) {
        int nEleClusters = electron.getClusters().size();
        int nPosClusters = positron.getClusters().size();

        BilliorVertex theVertex = vtxFitter.fitVertex(billiorTracks);
        ReconstructedParticle theV0 = HpsReconParticleDriver.makeReconstructedParticle(electron, positron, theVertex);
        Hep3Vector momRot = VecOp.mult(beamAxisRotation, theV0.getMomentum());
        Hep3Vector theVtx = VecOp.mult(beamAxisRotation, theV0.getStartVertex().getPosition());
        if (storeCov) {
            Matrix uncCov = MatrixOp.mult(MatrixOp.mult(beamAxisRotation, theV0.getStartVertex().getCovMatrix()),
                    MatrixOp.transposed(beamAxisRotation));

            tupleMap.put("uncCovXX/D", uncCov.e(0, 0));
            tupleMap.put("uncCovXY/D", uncCov.e(0, 1));
            tupleMap.put("uncCovXZ/D", uncCov.e(0, 2));
            tupleMap.put("uncCovYX/D", uncCov.e(1, 0));
            tupleMap.put("uncCovYY/D", uncCov.e(1, 1));
            tupleMap.put("uncCovYZ/D", uncCov.e(1, 2));
            tupleMap.put("uncCovZX/D", uncCov.e(2, 0));
            tupleMap.put("uncCovZY/D", uncCov.e(2, 1));
            tupleMap.put("uncCovZZ/D", uncCov.e(2, 2));
        }

        tupleMap.put(prefix + "PX/D", momRot.x());
        tupleMap.put(prefix + "PY/D", momRot.y());
        tupleMap.put(prefix + "PZ/D", momRot.z());
        tupleMap.put(prefix + "P/D", momRot.magnitude());
        tupleMap.put(prefix + "VX/D", theVtx.x());
        tupleMap.put(prefix + "VY/D", theVtx.y());
        tupleMap.put(prefix + "VZ/D", theVtx.z());
        tupleMap.put(prefix + "Chisq/D", theV0.getStartVertex().getChi2());
        tupleMap.put(prefix + "M/D", theV0.getMass());
        tupleMap.put(prefix + "ElePX/D", theV0.getStartVertex().getParameters().get("p1X"));
        tupleMap.put(prefix + "ElePY/D", theV0.getStartVertex().getParameters().get("p1Y"));
        tupleMap.put(prefix + "ElePZ/D", theV0.getStartVertex().getParameters().get("p1Z"));
        tupleMap.put(
                prefix + "EleP/D",
                Math.sqrt(Math.pow(theV0.getStartVertex().getParameters().get("p1X"), 2)
                        + Math.pow(theV0.getStartVertex().getParameters().get("p1Y"), 2)
                        + Math.pow(theV0.getStartVertex().getParameters().get("p1Z"), 2)));
        tupleMap.put(prefix+"PosPX/D", theV0.getStartVertex().getParameters().get("p2X"));
        tupleMap.put(prefix+"PosPY/D", theV0.getStartVertex().getParameters().get("p2Y"));
        tupleMap.put(prefix+"PosPZ/D", theV0.getStartVertex().getParameters().get("p2Z"));
        tupleMap.put(
                prefix+"PosP/D",
                Math.sqrt(Math.pow(theV0.getStartVertex().getParameters().get("p2X"), 2)
                        + Math.pow(theV0.getStartVertex().getParameters().get("p2Y"), 2)
                        + Math.pow(theV0.getStartVertex().getParameters().get("p2Z"), 2)));

        if (nEleClusters>0) {
            tupleMap.put(
                    prefix+"EleWtP/D",
                    MassCalculator.combinedMomentum(
                            electron.getClusters().get(0),
                            electron.getTracks().get(0),
                            Math.sqrt(Math.pow(theV0.getStartVertex().getParameters().get("p1X"), 2)
                                    + Math.pow(theV0.getStartVertex().getParameters().get("p1Y"), 2)
                                    + Math.pow(theV0.getStartVertex().getParameters().get("p1Z"), 2))));

            if (nPosClusters>0) {
                tupleMap.put(prefix+"PosWtP/D", MassCalculator.combinedMomentum(positron.getClusters().get(0), positron
                        .getTracks().get(0), Math.sqrt(Math.pow(theV0.getStartVertex().getParameters().get("p2X"), 2)
                                + Math.pow(theV0.getStartVertex().getParameters().get("p2Y"), 2)
                                + Math.pow(theV0.getStartVertex().getParameters().get("p2Z"), 2))));
                tupleMap.put(prefix+"WtM/D", MassCalculator.combinedMass(electron.getClusters().get(0), positron
                        .getClusters().get(0), theV0));
            }
            else {
                tupleMap.put(
                        prefix+"PosWtP/D",
                        Math.sqrt(Math.pow(theV0.getStartVertex().getParameters().get("p2X"), 2)
                                + Math.pow(theV0.getStartVertex().getParameters().get("p2Y"), 2)
                                + Math.pow(theV0.getStartVertex().getParameters().get("p2Z"), 2)));
                tupleMap.put(prefix+"WtM/D",
                        MassCalculator.combinedMass(electron.getClusters().get(0), positron.getTracks().get(0), theV0));
            }
        }
        if (nPosClusters > 0 && nEleClusters == 0) {// e+ has cluster, e- does not

            tupleMap.put(
                    prefix+"PosWtP/D",
                    MassCalculator.combinedMomentum(
                            positron.getClusters().get(0),
                            positron.getTracks().get(0),
                            Math.sqrt(Math.pow(theV0.getStartVertex().getParameters().get("p2X"), 2)
                                    + Math.pow(theV0.getStartVertex().getParameters().get("p2Y"), 2)
                                    + Math.pow(theV0.getStartVertex().getParameters().get("p2Z"), 2))));
            tupleMap.put(
                    prefix+"EleWtP/D",
                    Math.sqrt(Math.pow(theV0.getStartVertex().getParameters().get("p1X"), 2)
                            + Math.pow(theV0.getStartVertex().getParameters().get("p1Y"), 2)
                            + Math.pow(theV0.getStartVertex().getParameters().get("p1Z"), 2)));
            tupleMap.put(prefix+"WtM/D",
                    MassCalculator.combinedMass(electron.getTracks().get(0), positron.getClusters().get(0), theV0));
        }
        if (nPosClusters == 0 && nEleClusters == 0) {

            tupleMap.put(
                    prefix+"EleWtP/D",
                    Math.sqrt(Math.pow(theV0.getStartVertex().getParameters().get("p1X"), 2)
                            + Math.pow(theV0.getStartVertex().getParameters().get("p1Y"), 2)
                            + Math.pow(theV0.getStartVertex().getParameters().get("p1Z"), 2)));
            tupleMap.put(
                    prefix+"PosWtP/D",
                    Math.sqrt(Math.pow(theV0.getStartVertex().getParameters().get("p2X"), 2)
                            + Math.pow(theV0.getStartVertex().getParameters().get("p2Y"), 2)
                            + Math.pow(theV0.getStartVertex().getParameters().get("p2Z"), 2)));
            tupleMap.put(prefix+"WtM/D", theV0.getMass());
        }    
    }

    protected void fillVertexVariables(EventHeader event, List<BilliorTrack> billiorTracks,
            ReconstructedParticle electron, ReconstructedParticle positron) {
        fillVertexVariables(event, billiorTracks, electron, positron, true, true, true);
    }

    protected void fillVertexVariables(EventHeader event, List<BilliorTrack> billiorTracks,
            ReconstructedParticle electron, ReconstructedParticle positron, boolean doBsc, boolean doTar, boolean doVzc) {

        BilliorVertexer vtxFitter = new BilliorVertexer(TrackUtils.getBField(event.getDetector()).y());
        vtxFitter.setBeamSize(beamSize);
        vtxFitter.setBeamPosition(beamPos);

        //Unconstrained
        vtxFitter.doBeamSpotConstraint(false);
        fillVertexVariablesHelper("unc", vtxFitter, billiorTracks, electron, positron, true);

        // others
        if (doBsc) {
            vtxFitter.doBeamSpotConstraint(true);
            fillVertexVariablesHelper("bsc", vtxFitter, billiorTracks, electron, positron, false);
        }

        if (doTar) {
            vtxFitter.doBeamSpotConstraint(true);
            vtxFitter.doTargetConstraint(true);
            fillVertexVariablesHelper("tar", vtxFitter, billiorTracks, electron, positron, false);
        }

        if (doVzc) {
            vtxFitter.setBeamSize(vzcBeamSize);
            vtxFitter.doBeamSpotConstraint(true);
            vtxFitter.doTargetConstraint(true);
            fillVertexVariablesHelper("vzc", vtxFitter, billiorTracks, electron, positron, false);
        }
    }

    protected void addMCTridentVariables() {
        addMCParticleVariables("tri");
        addMCParticleVariables("triEle1");
        addMCParticleVariables("triEle2");
        addMCParticleVariables("triPos");
        addMCPairVariables("triPair1");
        addMCPairVariables("triPair2");
    }
    
    protected void addMCWabVariables() {
        addMCParticleVariables("wab");
        addMCParticleVariables("wabEle1");
        addMCParticleVariables("wabEle2");
        addMCParticleVariables("wabPos");
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
    
    
    protected void addFullTruthVertexVariables() {
        addMCParticleVariables("ele");
        addMCParticleVariables("pos");
        addEcalTruthVariables("ele");
        addEcalTruthVariables("pos");
        addSVTTruthVariables("ele");
        addSVTTruthVariables("pos");
    }

    protected void addMCParticleVariables(String prefix) {
        String[] newVars = new String[] {"StartX/D", "StartY/D", "StartZ/D", "EndX/D", "EndY/D", "EndZ/D", "PX/D",
                "PY/D", "PZ/D", "P/D", "M/D", "E/D","pdgid/I","parentID/I","HasTruthMatch/I","NTruthHits/I","NBadTruthHits/I","Purity/D"};

        for (int i = 0; i < newVars.length; i++) {
            newVars[i] = prefix + newVars[i];
        }
        tupleVariables.addAll(Arrays.asList(newVars));
    }
    
    protected void addMCSVTVariables(String prefix) {
        String[] newVars = new String[] {"svthitX/D","svthitY/D","svthitZ/D",
                "svthitPx/D","svthitPy/D","svthitPz/D","thetaX/D","thetaY/D","residualX/D","residualY/D"};
        for (int i = 0; i < newVars.length; i++) {
            newVars[i] = prefix + newVars[i];
        }
        tupleVariables.addAll(Arrays.asList(newVars));
    }
    
    protected void addMCEcalVariables(String prefix) {
        String[] newVars = new String[]{
                "ecalhitIx/D","ecalhitIy/D","ecalhitX/D","ecalhitY/D","ecalhitZ/D",
                "ecalhitEnergy/D"};
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
            String layer = Integer.toString(i+1);
            addMCSVTVariables(prefix+"L"+layer+"t");
            addMCSVTVariables(prefix+"L"+layer+"b");
            addMCSVTVariables(prefix+"L"+layer+"tIn");
            addMCSVTVariables(prefix+"L"+layer+"bIn");
        }
    }

    protected void addMCPairVariables(String prefix) {
        String[] newVars = new String[] {"PX/D", "PY/D", "PZ/D", "P/D", "M/D", "E/D"};

        for (int i = 0; i < newVars.length; i++) {
            newVars[i] = prefix + newVars[i];
        }
        tupleVariables.addAll(Arrays.asList(newVars));
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

    protected void fillMCPairVariables(String prefix, MCParticle ele, MCParticle pos) {
        HepLorentzVector vtx = VecOp.add(ele.asFourVector(), pos.asFourVector());
        Hep3Vector vtxP = VecOp.mult(beamAxisRotation, vtx.v3());
        tupleMap.put(prefix + "PX/D", vtxP.x());
        tupleMap.put(prefix + "PY/D", vtxP.y());
        tupleMap.put(prefix + "PZ/D", vtxP.z());
        tupleMap.put(prefix + "P/D", vtxP.magnitude());
        tupleMap.put(prefix + "M/D", vtx.magnitude());
        tupleMap.put(prefix + "E/D", vtx.t());
    }

    protected void fillMCTridentVariables(EventHeader event) {
        List<MCParticle> MCParticles = event.getMCParticles();

        MCParticle trident = null;

        MCParticle ele1 = null;// highest-energy electron daughter
        MCParticle ele2 = null;// second-highest-energy electron daughter (if any)
        MCParticle pos = null;// highest-energy positron daughter

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
    
    protected void fillMCWabVariables(EventHeader event) {
        List<MCParticle> MCParticles = event.getMCParticles();

        MCParticle wab = null;

        MCParticle ele1 = null;// conversion electron daughter
        MCParticle ele2 = null;// recoil wab electron
        MCParticle pos = null;// conversion positron daughter

        List<MCParticle> wabParticles = null;

        for (MCParticle particle : MCParticles) {
            if (particle.getPDGID() == 22 && particle.getGeneratorStatus() == 1 && particle.getDaughters().size() == 2) {
                double wabEnergy = particle.getEnergy();
                for(MCParticle p : MCParticles){
                    if(p.getPDGID() != 11 || !p.getParents().isEmpty()) continue;
                    double eleEnergy = p.getEnergy();
                    double energy = wabEnergy + eleEnergy;
                    if(energy < 0.98 * ebeam || energy > 1.02 * ebeam) continue;
                    ele2 = p;
                    wab = particle;
                    wabParticles = wab.getDaughters();
                    break;
                }
            }
        }
        if (wab == null) {
            return;
        }

        fillMCParticleVariables("wab", wab);

        for (MCParticle particle : wabParticles) {
            if(particle.getPDGID() == 11){
                pos = particle;
            }
            if(particle.getPDGID() == -11){
                ele1 = particle;
            }
        }

        if (ele1 != null) {
            fillMCParticleVariables("wabEle1", ele1);
        }
        if (ele2 != null) {
            fillMCParticleVariables("wabEle2", ele2);
        }
        if (pos != null) {
            fillMCParticleVariables("wabPos", pos);
        }
    }
    
    protected void fillMCFullTruthVariables(EventHeader event){
        fillMCTridentVariables(event);

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
        
        Map<MCParticle, List<SimTrackerHit>> trackerHitMap = MCFullDetectorTruth.BuildTrackerHitMap(trackerHits);
        Map<MCParticle, List<SimTrackerHit>> trackerInHitMap = MCFullDetectorTruth.BuildTrackerHitMap(trackerHits_Inactive);
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
