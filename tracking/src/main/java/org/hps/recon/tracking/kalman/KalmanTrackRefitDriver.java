package org.hps.recon.tracking.kalman;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ejml.data.DMatrixRMaj;

import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.MaterialSupervisor.ScatteringDetectorVolume;
import org.hps.recon.tracking.MaterialSupervisor.SiStripPlane;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseLCRelation;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;

/**
 * Refits existing Kalman tracks with a subset of their own hits, dropping the hits on a
 * steering-configurable list of SVT layers. The refit uses the identical hit content otherwise
 * (same strip clusters, same field map, same material model), so a track-by-track comparison of
 * the parent and refit isolates the contribution of the vetoed layers to the momentum
 * measurement. A parent-to-refit LCRelation collection is written for exact matching.
 *
 * Typical use: run after KalmanPatRecDriver in the same job, once with an empty veto list
 * (control: refit should closely reproduce the parent) and once with e.g. the last SVT layer
 * pair vetoed. Requires the strip-cluster collection in the event, so it must run inside a
 * reconstruction job, not on skims that drop StripClusterer_SiTrackerHitStrip1D.
 *
 * Layers are identified by HpsSiSensor.getLayerNumber(), i.e. 1-14 for the 2019+ SVT, counted
 * from the target: L7 axial+stereo = "13 14", L1 pair = "1 2".
 */
public class KalmanTrackRefitDriver extends Driver {

    private String inputTrackCollectionName = "KalmanFullTracks";
    private String outputTrackCollectionName = "KalmanRefitTracks";
    private String relationCollectionName = "KalmanRefitToParentRelations";
    private String vetoLayers = "";           // Space-separated sensor layer numbers (1-14) to drop
    private Set<Integer> vetoSet = new HashSet<Integer>();
    private int numKalmanIteration = 2;       // Kalman fit iterations for the refit
    private int minHits = 6;                  // Minimum hits remaining after the veto to attempt a refit
    private boolean refitUnvetoedTracks = true; // If false, skip tracks that lost no hits to the veto

    private KalmanInterface KI;
    private KalmanParams kPar;
    private org.lcsim.geometry.FieldMap fm;
    private MaterialSupervisor _materialManager;
    private ArrayList<SiStripPlane> detPlanes;
    private double bField;
    private Logger logger;

    private int nRefit;
    private int nFailed;
    private int nTooFewHits;

    public void setInputTrackCollectionName(String v) { inputTrackCollectionName = v; }
    public void setOutputTrackCollectionName(String v) { outputTrackCollectionName = v; }
    public void setRelationCollectionName(String v) { relationCollectionName = v; }
    public void setNumKalmanIteration(int v) { numKalmanIteration = v; }
    public void setMinHits(int v) { minHits = v; }
    public void setRefitUnvetoedTracks(boolean v) { refitUnvetoedTracks = v; }

    public void setVetoLayers(String v) {
        vetoLayers = v;
        vetoSet.clear();
        for (String tok : vetoLayers.trim().split("\\s+")) {
            if (tok.isEmpty()) continue;
            vetoSet.add(Integer.valueOf(tok));
        }
        System.out.format("KalmanTrackRefitDriver: vetoing sensor layers %s for collection %s\n",
                vetoSet.toString(), outputTrackCollectionName);
    }

    @Override
    public void detectorChanged(Detector det) {
        logger = Logger.getLogger(KalmanTrackRefitDriver.class.getName());
        _materialManager = new MaterialSupervisor();
        _materialManager.buildModel(det);
        fm = det.getFieldMap();

        detPlanes = new ArrayList<SiStripPlane>();
        for (ScatteringDetectorVolume vol : _materialManager.getMaterialVolumes()) {
            detPlanes.add((SiStripPlane) vol);
        }
        bField = TrackUtils.getBField(det).magnitude();

        kPar = new KalmanParams();
        KI = new KalmanInterface(kPar, fm);
        KI.createSiModules(detPlanes);

        logger.config(String.format("KalmanTrackRefitDriver: %s -> %s, veto layers %s, minHits %d",
                inputTrackCollectionName, outputTrackCollectionName, vetoSet.toString(), minHits));
    }

    @Override
    public void process(EventHeader event) {
        List<Track> outputTracks = new ArrayList<Track>();
        List<LCRelation> relations = new ArrayList<LCRelation>();

        if (event.hasCollection(Track.class, inputTrackCollectionName)) {
            KI.setRunNumber(event.getRunNumber());
            int evtNumb = event.getEventNumber();

            for (Track trk : event.get(Track.class, inputTrackCollectionName)) {
                List<TrackerHit> hits = trk.getTrackerHits();

                List<TrackerHit> kept = new ArrayList<TrackerHit>(hits.size());
                for (TrackerHit hit : hits) {
                    HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement();
                    if (!vetoSet.contains(sensor.getLayerNumber())) kept.add(hit);
                }
                if (!refitUnvetoedTracks && kept.size() == hits.size()) continue;
                if (kept.size() < minHits) {
                    nTooFewHits++;
                    continue;
                }

                // Seed the refit from the parent's perigee state, pivot-transformed to the first kept hit.
                TrackState ts = trk.getTrackStates().get(0);
                double c = 2.99793e8; // Speed of light in m/s
                double alpha = 1000.0 * 1.0e9 / (c * bField);
                double[] params = { ts.getD0(), ts.getPhi(), ts.getOmega(), ts.getZ0(), ts.getTanLambda() };
                Vec kalParams = new Vec(5, KalmanInterface.unGetLCSimParams(params, alpha));
                Vec oldPivot = KalmanInterface.vectorGlbToKalman(ts.getReferencePoint());

                TrackerHit firstHit = kept.get(0);
                for (TrackerHit hit : kept) {
                    if (hit.getPosition()[2] < firstHit.getPosition()[2]) firstHit = hit;
                }
                Vec newPivot = KalmanInterface.vectorGlbToKalman(firstHit.getPosition());
                double bLocal = KalmanInterface.getField(newPivot, fm).mag();
                double alphaLocal = 1000.0 * 1.0e9 / (c * bLocal);
                kalParams = HelixState.pivotTransform(newPivot, kalParams, oldPivot, alphaLocal, 0.);
                DMatrixRMaj cov = new DMatrixRMaj(KalmanInterface.ungetLCSimCov(ts.getCovMatrix(), alphaLocal));

                KalmanTrackFit2 ktf2 = KI.createKalmanTrackFit(evtNumb, kalParams, newPivot, cov, kept, numKalmanIteration);
                if (ktf2 == null || !ktf2.success || ktf2.tkr == null) {
                    nFailed++;
                    KI.clearInterface();
                    continue;
                }
                KalTrack refit = ktf2.tkr;
                if (!refit.originHelix()) {
                    logger.log(Level.FINE, "KalmanTrackRefitDriver: origin propagation failed for refit track");
                    nFailed++;
                    KI.clearInterface();
                    continue;
                }
                Track refitTrack = KI.createTrack(refit, true);
                if (refitTrack == null) {
                    nFailed++;
                    KI.clearInterface();
                    continue;
                }
                outputTracks.add(refitTrack);
                relations.add(new BaseLCRelation(trk, refitTrack));
                nRefit++;
                KI.clearInterface();
            }
        }

        int flag = 1 << LCIOConstants.TRBIT_HITS;
        event.put(outputTrackCollectionName, outputTracks, Track.class, flag);
        event.put(relationCollectionName, relations, LCRelation.class, 0);
    }

    @Override
    public void endOfData() {
        System.out.format("KalmanTrackRefitDriver (%s): %d tracks refit, %d fit failures, %d skipped with < %d hits after veto of layers %s\n",
                outputTrackCollectionName, nRefit, nFailed, nTooFewHits, minHits, vetoSet.toString());
    }
}
