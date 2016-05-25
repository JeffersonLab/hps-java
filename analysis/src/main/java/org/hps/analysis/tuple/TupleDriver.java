package org.hps.analysis.tuple;

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
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 * sort of an interface for DQM analysis drivers creates the DQM database
 * manager, checks whether row exists in db etc
 *
 * @author mgraham on Apr 15, 2014 update mgraham on May 15, 2014 to include
 * calculateEndOfRunQuantities & printDQMData i.e. useful methods
 */
public class TupleDriver extends Driver {

    protected boolean debug = false;

    protected PrintWriter tupleWriter = null;
    protected final List<String> tupleVariables = new ArrayList<String>();
    protected final Map<String, Double> tupleMap = new HashMap<String, Double>();
    protected boolean cutTuple = false;

    protected String triggerType = "all";//allowed types are "" (blank) or "all", singles0, singles1, pairs0,pairs1
    public boolean isGBL = false;

    private final String finalStateParticlesColName = "FinalStateParticles";
    protected double bfield;
    private final double[] beamSize = {0.001, 0.130, 0.050}; //rough estimate from harp scans during engineering run production running
    private final double[] beamPos = {0.0, 0.0, 0.0};
    private final double[] vzcBeamSize = {0.001, 100, 100};
    private final double[] topTrackCorrection = {0, 0, 0, 0, 0};
    private final double[] botTrackCorrection = {0, 0, 0, 0, 0};
    protected final BasicHep3Matrix beamAxisRotation = new BasicHep3Matrix();
    protected double ebeam = Double.NaN;

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

    @Override
    protected void detectorChanged(Detector detector) {
        beamAxisRotation.setActiveEuler(Math.PI / 2, -0.0305, -Math.PI / 2);
        bfield = TrackUtils.getBField(detector).magnitude();

        BeamEnergy.BeamEnergyCollection beamEnergyCollection
                = this.getConditionsManager().getCachedConditions(BeamEnergy.BeamEnergyCollection.class, "beam_energies").getCachedData();
        if (Double.isNaN(ebeam)) {
            ebeam = beamEnergyCollection.get(0).getBeamEnergy();
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
            if (value == null) {
                value = -9999.0;
            }
            if (variable.endsWith("/I") || variable.endsWith("/B")) {
                tupleWriter.format("%d\t", Math.round(value));
            } else {
                tupleWriter.format("%f\t", value);
            }
        }
        tupleWriter.println();
//        tupleMap.clear();
    }

    public void setTupleFile(String tupleFile) {
        try {
            tupleWriter = new PrintWriter(tupleFile);
        } catch (FileNotFoundException e) {
            tupleWriter = null;
        }
        tupleWriter.println(StringUtils.join(tupleVariables, ":"));
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
            "isCalib/B", "isPulser/B", "isSingle0/B", "isSingle1/B", "isPair0/B", "isPair1/B"};
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
            "vzcVX/D", "vzcVY/D", "vzcVZ/D", "vzcChisq/D", "vzcM/D"};
        tupleVariables.addAll(Arrays.asList(newVars));
    }

    protected void addParticleVariables(String prefix) {
        String[] newVars = new String[]{"PX/D", "PY/D", "PZ/D", "P/D",
            "TrkChisq/D", "TrkHits/I", "TrkType/I", "TrkT/D",
            "TrkZ0/D", "TrkLambda/D", "TrkD0/D", "TrkPhi/D", "TrkOmega/D",
            "TrkEcalX/D", "TrkEcalY/D",
            "HasL1/B", "HasL2/B", "HasL3/B",
            "FirstHitX/D", "FirstHitY/D",
            "LambdaKink1/D", "LambdaKink2/D", "LambdaKink3/D",
            "PhiKink1/D", "PhiKink2/D", "PhiKink3/D",
            "IsoStereo/D", "IsoAxial/D",
            "MinPositiveIso/D", "MinNegativeIso/D",
            "MatchChisq/D", "ClT/D", "ClE/D", "ClX/D", "ClY/D", "ClZ/D", "ClHits/I"};
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
    }

    protected TrackState fillParticleVariables(EventHeader event, ReconstructedParticle particle, String prefix) {
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
        tupleMap.put(prefix + "LambdaKink1/D", GBLKinkData.getLambdaKink(kinks, 1));
        tupleMap.put(prefix + "LambdaKink2/D", GBLKinkData.getLambdaKink(kinks, 2));
        tupleMap.put(prefix + "LambdaKink3/D", GBLKinkData.getLambdaKink(kinks, 3));
        tupleMap.put(prefix + "PhiKink1/D", GBLKinkData.getPhiKink(kinks, 1));
        tupleMap.put(prefix + "PhiKink2/D", GBLKinkData.getPhiKink(kinks, 2));
        tupleMap.put(prefix + "PhiKink3/D", GBLKinkData.getPhiKink(kinks, 3));
        tupleMap.put(prefix + "IsoStereo/D", isoStereo);
        tupleMap.put(prefix + "IsoAxial/D", isoAxial);
        tupleMap.put(prefix + "MinPositiveIso/D", minPositiveIso);
        tupleMap.put(prefix + "MinNegativeIso/D", minNegativeIso);
        tupleMap.put(prefix + "MatchChisq/D", particle.getGoodnessOfPID());
        if (!particle.getClusters().isEmpty()) {
            Cluster cluster = particle.getClusters().get(0);
            tupleMap.put(prefix + "ClT/D", ClusterUtilities.getSeedHitTime(cluster));
            tupleMap.put(prefix + "ClE/D", cluster.getEnergy());
            tupleMap.put(prefix + "ClX/D", cluster.getPosition()[0]);
            tupleMap.put(prefix + "ClY/D", cluster.getPosition()[1]);
            tupleMap.put(prefix + "ClZ/D", cluster.getPosition()[2]);
            tupleMap.put(prefix + "ClHits/I", (double) cluster.getCalorimeterHits().size());
        }

        return tweakedTrackState;
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

        tupleMap.put("bscPX/D", bscMomRot.x());
        tupleMap.put("bscPY/D", bscMomRot.y());
        tupleMap.put("bscPZ/D", bscMomRot.z());
        tupleMap.put("bscP/D", bscMomRot.magnitude());
        tupleMap.put("bscVX/D", bscVtx.x());
        tupleMap.put("bscVY/D", bscVtx.y());
        tupleMap.put("bscVZ/D", bscVtx.z());
        tupleMap.put("bscChisq/D", bscV0.getStartVertex().getChi2());
        tupleMap.put("bscM/D", bscV0.getMass());

        tupleMap.put("tarPX/D", tarMomRot.x());
        tupleMap.put("tarPY/D", tarMomRot.y());
        tupleMap.put("tarPZ/D", tarMomRot.z());
        tupleMap.put("tarP/D", tarMomRot.magnitude());
        tupleMap.put("tarVX/D", tarVtx.x());
        tupleMap.put("tarVY/D", tarVtx.y());
        tupleMap.put("tarVZ/D", tarVtx.z());
        tupleMap.put("tarChisq/D", tarV0.getStartVertex().getChi2());
        tupleMap.put("tarM/D", tarV0.getMass());

        tupleMap.put("vzcPX/D", vzcMomRot.x());
        tupleMap.put("vzcPY/D", vzcMomRot.y());
        tupleMap.put("vzcPZ/D", vzcMomRot.z());
        tupleMap.put("vzcP/D", vzcMomRot.magnitude());
        tupleMap.put("vzcVX/D", vzcVtx.x());
        tupleMap.put("vzcVY/D", vzcVtx.y());
        tupleMap.put("vzcVZ/D", vzcVtx.z());
        tupleMap.put("vzcChisq/D", vzcV0.getStartVertex().getChi2());
        tupleMap.put("vzcM/D", vzcV0.getMass());
    }
}
