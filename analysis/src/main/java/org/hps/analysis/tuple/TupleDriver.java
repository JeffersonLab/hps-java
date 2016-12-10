package org.hps.analysis.tuple;

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
import org.hps.conditions.beam.BeamEnergy;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.particle.HpsReconParticleDriver;
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.gbl.GBLKinkData;
import org.hps.recon.vertexing.BilliorTrack;
import org.hps.recon.vertexing.BilliorVertex;
import org.hps.recon.vertexing.BilliorVertexer;
import org.hps.record.triggerbank.TIData;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
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
    protected final BasicHep3Matrix beamAxisRotation = BasicHep3Matrix.identity();
    protected double ebeam = Double.NaN;

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
            "nTrk/I", "nPos/I",
            "isCalib/B", "isPulser/B", "isSingle0/B", "isSingle1/B", "isPair0/B", "isPair1/B","evTime/D","evTx/I","evTy/I"};
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
            "uncElePX/D", "uncElePY/D", "uncElePZ/D", "uncPosPX/D", "uncPosPY/D", "uncPosPZ/D", "uncEleP/D", "uncPosP/D",
            "bscElePX/D", "bscElePY/D", "bscElePZ/D", "bscPosPX/D", "bscPosPY/D", "bscPosPZ/D", "bscEleP/D", "bscPosP/D",
            "tarElePX/D", "tarElePY/D", "tarElePZ/D", "tarPosPX/D", "tarPosPY/D", "tarPosPZ/D", "tarEleP/D", "tarPosP/D",
            "vzcElePX/D", "vzcElePY/D", "vzcElePZ/D", "vzcPosPX/D", "vzcPosPY/D", "vzcPosPZ/D", "vzcEleP/D", "vzcPosP/D",
            "uncEleWtP/D", "uncPosWtP/D", "bscEleWtP/D", "bscPosWtP/D", "tarEleWtP/D", "tarPosWtP/D", "vzcEleWtP/D", "vzcPosWtP/D",
            "uncWtM/D", "bscWtM/D", "tarWtM/D", "vzcWtM/D"};
        tupleVariables.addAll(Arrays.asList(newVars));
    }

    protected void addParticleVariables(String prefix) {
        String[] newVars = new String[]{"PX/D", "PY/D", "PZ/D", "P/D",
            "TrkChisq/D", "TrkHits/I", "TrkType/I", "TrkT/D",
            "TrkZ0/D", "TrkLambda/D", "TrkD0/D", "TrkPhi/D", "TrkOmega/D",
            "TrkEcalX/D", "TrkEcalY/D",
            "HasL1/B", "HasL2/B", "HasL3/B",
            "FirstHitX/D", "FirstHitY/D",
            "FirstHitT1/D", "FirstHitT2/D",
            "FirstHitDEDx1/D", "FirstHitDEDx2/D",
            "FirstClusterSize1/I", "FirstClusterSize2/I",
            "LambdaKink1/D", "LambdaKink2/D", "LambdaKink3/D",
            "PhiKink1/D", "PhiKink2/D", "PhiKink3/D",
            "IsoStereo/D", "IsoAxial/D",
            "MinPositiveIso/D", "MinNegativeIso/D",
            "MatchChisq/D", "ClT/D", "ClE/D", "ClSeedE/D", "ClX/D", "ClY/D", "ClZ/D", "ClHits/I"};

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
        }

        tupleMap.put("nTrk/I", (double) ntrk);
        tupleMap.put("nPos/I", (double) npos);

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
            Track track = particle.getTracks().get(0);
            TrackState trackState = track.getTrackStates().get(0);
            double[] param = new double[5];
            for (int i = 0; i < 5; i++) {
                param[i] = trackState.getParameters()[i] + ((trackState.getTanLambda() > 0) ? topTrackCorrection[i] : botTrackCorrection[i]);
            }
//            Arrays.
            TrackState tweakedTrackState = new BaseTrackState(param, trackState.getReferencePoint(), trackState.getCovMatrix(), trackState.getLocation(), bfield);
            Hep3Vector pRot = VecOp.mult(beamAxisRotation, CoordinateTransformations.transformVectorToDetector(new BasicHep3Vector(tweakedTrackState.getMomentum())));

            Double[] iso = TrackUtils.getIsolations(track, TrackUtils.getHitToStripsTable(event), TrackUtils.getHitToRotatedTable(event));
            double minPositiveIso = 9999;
            double minNegativeIso = 9999;
            double isoStereo = -9999, isoAxial = -9999;
            for (int i = 0; i < 6; i++) {
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
                    break;
                }
            }

            double trkT = TrackUtils.getTrackTime(track, TrackUtils.getHitToStripsTable(event), TrackUtils.getHitToRotatedTable(event));
            Hep3Vector atEcal = TrackUtils.getTrackPositionAtEcal(tweakedTrackState);
            Hep3Vector firstHitPosition = VecOp.mult(beamAxisRotation, CoordinateTransformations.transformVectorToDetector(new BasicHep3Vector(track.getTrackerHits().get(0).getPosition())));
            GenericObject kinks = GBLKinkData.getKinkData(event, track);

            RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
            RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);

            double hitTimes[] = new double[2];
            double hitdEdx[] = new double[2];
            int hitClusterSize[] = new int[2];

            track.getTrackerHits().get(0);
            TrackerHit hit = track.getTrackerHits().get(0);
            Collection<TrackerHit> htsList = hitToStrips.allFrom(hitToRotated.from(hit));
            for (TrackerHit hts : htsList) {
                int layer = ((HpsSiSensor) ((RawTrackerHit) hts.getRawHits().get(0)).getDetectorElement()).getLayerNumber();
                hitTimes[layer % 2] = hts.getTime();
                hitdEdx[layer % 2] = hts.getdEdx();
                hitClusterSize[layer % 2] = hts.getRawHits().size();
            }

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
            tupleMap.put(prefix + "HasL1/B", iso[0] != null ? 1.0 : 0.0);
            tupleMap.put(prefix + "HasL2/B", iso[2] != null ? 1.0 : 0.0);
            tupleMap.put(prefix + "HasL3/B", iso[4] != null ? 1.0 : 0.0);
            tupleMap.put(prefix + "FirstHitX/D", firstHitPosition.x());
            tupleMap.put(prefix + "FirstHitY/D", firstHitPosition.y());
            tupleMap.put(prefix + "FirstHitT1/D", hitTimes[0]);
            tupleMap.put(prefix + "FirstHitT2/D", hitTimes[1]);
            tupleMap.put(prefix + "FirstHitDEDx1/D", hitdEdx[0]);
            tupleMap.put(prefix + "FirstHitDEDx2/D", hitdEdx[1]);
            tupleMap.put(prefix + "FirstClusterSize1/I", (double) hitClusterSize[0]);
            tupleMap.put(prefix + "FirstClusterSize2/I", (double) hitClusterSize[1]);
            tupleMap.put(prefix + "LambdaKink1/D", kinks != null ? GBLKinkData.getLambdaKink(kinks, 1) : 0);
            tupleMap.put(prefix + "LambdaKink2/D", kinks != null ? GBLKinkData.getLambdaKink(kinks, 2) : 0);
            tupleMap.put(prefix + "LambdaKink3/D", kinks != null ? GBLKinkData.getLambdaKink(kinks, 3) : 0);
            tupleMap.put(prefix + "PhiKink1/D", kinks != null ? GBLKinkData.getPhiKink(kinks, 1) : 0);
            tupleMap.put(prefix + "PhiKink2/D", kinks != null ? GBLKinkData.getPhiKink(kinks, 2) : 0);
            tupleMap.put(prefix + "PhiKink3/D", kinks != null ? GBLKinkData.getPhiKink(kinks, 3) : 0);
            tupleMap.put(prefix + "IsoStereo/D", isoStereo);
            tupleMap.put(prefix + "IsoAxial/D", isoAxial);
            tupleMap.put(prefix + "MinPositiveIso/D", minPositiveIso);
            tupleMap.put(prefix + "MinNegativeIso/D", minNegativeIso);
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
