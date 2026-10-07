package org.hps.analysis.tuple;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.TrackStateUtils;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.vertexing.BilliorTrack;
import org.hps.recon.vertexing.BilliorVertex;
import org.hps.recon.vertexing.BilliorVertexer;
import org.hps.recon.vertexing.NTrackVertexer;
import org.hps.recon.vertexing.TrackConstraintVertexFitter;
import org.hps.recon.vertexing.TrackConstraintVertexFitter.TrackMomentum;
import org.hps.recon.vertexing.TrackConstraintVertexFitter.TrackParams;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

import hep.physics.matrix.SymmetricMatrix;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * Writes a flat ASCII ntuple comparing, per event, the legacy Billoir two-track V0 vertex
 * fit against the Billoir-batch two-track vertex fit
 * ({@code NTrackVertexer}, N=2), for the truth-matched e-/e+ decay pair of an A'
 * signal MC event (PDGID 622 with exactly 2 daughters). Unlike {@code
 * NTrackVertexComparisonTupleDriver} (which also handles the trident topology and its
 * 3-daughter ambiguity), this driver is scoped to the unambiguous A'-pair case only -- events
 * with no such PDGID-622 record (e.g. a trident-only sample) are skipped. As with {@code
 * NTrackVertexComparisonTupleDriver}, this driver does its own track selection and fitting
 * inline -- it reads raw {@code KalmanFullTracks} plus MC truth directly, rather than a
 * pre-built V0 candidate collection from {@code HpsReconParticleDriver} -- so the comparison
 * is independent of any production driver wiring. One row per event (only events where both
 * truth daughters are truth-matched to a reconstructed track). Header line is variable names
 * joined by ":"; data rows are tab-separated, matching the convention used by {@link
 * CascadeVertexTupleDriver}.
 */
public class V0VertexComparisonTupleDriver extends Driver {

    private static final List<String> VARIABLES = Arrays.asList(
            "run/I", "event/I",
            "vtxX/D", "vtxY/D", "vtxZ/D",
            "vtxXErr/D", "vtxYErr/D", "vtxZErr/D",
            "chi2/D", "mass/D",
            "kalUncElePx/D", "kalUncElePy/D", "kalUncElePz/D",
            "kalUncElePxErr/D", "kalUncElePyErr/D", "kalUncElePzErr/D",
            "kalUncPosPx/D", "kalUncPosPy/D", "kalUncPosPz/D",
            "kalUncPosPxErr/D", "kalUncPosPyErr/D", "kalUncPosPzErr/D",
            "eleTruthMatched/I", "posTruthMatched/I", "allTruthMatched/I",
            "apVtxXMC/D", "apVtxYMC/D", "apVtxZMC/D",
            "mcElePx/D", "mcElePy/D", "mcElePz/D",
            "mcPosPx/D", "mcPosPy/D", "mcPosPz/D",
            "recoElePx/D", "recoElePy/D", "recoElePz/D",
            "recoElePxErr/D", "recoElePyErr/D", "recoElePzErr/D",
            "recoPosPx/D", "recoPosPy/D", "recoPosPz/D",
            "recoPosPxErr/D", "recoPosPyErr/D", "recoPosPzErr/D");

    private String trackCollectionName = "KalmanFullTracks";
    private String mcParticlesColName = "MCParticle";
    private String trackToMCParticleRelationsColName = "KalmanFullTracksToMCParticleRelations";
    private String tupleFile = null;
    private PrintWriter tupleWriter = null;
    private double bField;
    private double minTruthMatchPurity = 0.9;

    public void setTrackCollectionName(String trackCollectionName) {
        this.trackCollectionName = trackCollectionName;
    }

    public void setMinTruthMatchPurity(double minTruthMatchPurity) {
        this.minTruthMatchPurity = minTruthMatchPurity;
    }

    public void setMcParticlesColName(String mcParticlesColName) {
        this.mcParticlesColName = mcParticlesColName;
    }

    public void setTrackToMCParticleRelationsColName(String trackToMCParticleRelationsColName) {
        this.trackToMCParticleRelationsColName = trackToMCParticleRelationsColName;
    }

    public void setTupleFile(String tupleFile) {
        this.tupleFile = tupleFile;
    }

    @Override
    protected void detectorChanged(Detector detector) {
        bField = detector.getFieldMap().getField(new BasicHep3Vector(0., 0., -4.3)).y();
        if (tupleFile == null) {
            return;
        }
        try {
            tupleWriter = new PrintWriter(tupleFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not open V0 vertex comparison tuple file " + tupleFile, e);
        }
        tupleWriter.println(String.join(":", VARIABLES));
    }

    @Override
    public void process(EventHeader event) {
        if (tupleWriter == null || !event.hasCollection(Track.class, trackCollectionName)
                || !event.hasCollection(MCParticle.class, mcParticlesColName)
                || !event.hasCollection(LCRelation.class, trackToMCParticleRelationsColName)) {
            return;
        }

        // Truth identification: the A' signal record is PDGID 622 with exactly 2 daughters
        // (the signal e-/e+ pair), sharing one common, uncontaminated production vertex.
        // Scoped to this topology only -- skip events with no such record (e.g. a
        // trident-only sample), unlike NTrackVertexComparisonTupleDriver which also handles
        // the trident 3-daughter case.
        List<MCParticle> mcParticles = event.get(MCParticle.class, mcParticlesColName);
        MCParticle eleMC = null;
        MCParticle posMC = null;
        for (MCParticle mcp : mcParticles) {
            if (mcp.getPDGID() == 622 && mcp.getDaughters().size() == 2) {
                for (MCParticle daughter : mcp.getDaughters()) {
                    if (daughter.getPDGID() == 11) {
                        eleMC = daughter;
                    } else if (daughter.getPDGID() == -11) {
                        posMC = daughter;
                    }
                }
                break;
            }
        }
        if (eleMC == null || posMC == null) {
            return;
        }

        // Truth vertex: eleMC's own getOrigin() is not contaminated by the apMC
        // decay-length-shift bug (see NTrackVertexComparisonTupleDriver's apVtxMC comment).
        Hep3Vector apVtxMC = eleMC.getOrigin();
        Hep3Vector eleMCMom = eleMC.getMomentum();
        Hep3Vector posMCMom = posMC.getMomentum();

        // A single MCParticle can receive relations from more than one Track (e.g. a
        // ghost/duplicate track sharing hits with the true particle), so pick the
        // highest-purity (rel.getWeight()) match rather than whichever relation the
        // collection happens to iterate last.
        Map<MCParticle, Track> mcToTrack = new HashMap<MCParticle, Track>();
        Map<MCParticle, Double> mcToWeight = new HashMap<MCParticle, Double>();
        for (LCRelation rel : event.get(LCRelation.class, trackToMCParticleRelationsColName)) {
            MCParticle mcp = (MCParticle) rel.getTo();
            if (mcp == eleMC || mcp == posMC) {
                double weight = rel.getWeight();
                if (weight < minTruthMatchPurity) {
                    continue;
                }
                Double best = mcToWeight.get(mcp);
                if (best == null || weight > best) {
                    mcToWeight.put(mcp, weight);
                    mcToTrack.put(mcp, (Track) rel.getFrom());
                }
            }
        }

        Track eleTrack = mcToTrack.get(eleMC);
        Track posTrack = mcToTrack.get(posMC);
        boolean eleMatched = eleTrack != null;
        boolean posMatched = posTrack != null;
        if (!eleMatched || !posMatched) {
            return;
        }

        List<Track> tracks = Arrays.asList(eleTrack, posTrack);

        // Raw reconstructed momentum (+ its own, no-vertex-constraint error) at each
        // track's own point of closest approach, before any vertex fit is applied.
        Hep3Vector[] recoEle = getRawMomentumAndError(eleTrack);
        Hep3Vector[] recoPos = getRawMomentumAndError(posTrack);
        Hep3Vector recoEleMom = recoEle[0], recoEleMomErr = recoEle[1];
        Hep3Vector recoPosMom = recoPos[0], recoPosMomErr = recoPos[1];

        List<BilliorTrack> billTracks = new ArrayList<BilliorTrack>();
        for (Track track : tracks) {
            billTracks.add(new BilliorTrack(track));
        }

        // BilliorTrack(Track) copies the AtPerigee track parameters verbatim but drops the
        // TrackState's own (fixed, non-zero) reference point, and BilliorVertexer's linear
        // approximation is only accurate near its assumed reference point -- exactly the gap
        // HpsReconParticleDriver.fitVertex()/shiftTracksToVertex() (production V0 fitting)
        // works around: fit once from the naive (uncorrected) tracks, then re-derive each
        // track's helix parameters at that first-pass vertex via
        // TrackUtils.getParametersAtNewRefPoint/getCovarianceAtNewRefPoint, and refit from
        // there. Mirrored here verbatim for full consistency with the production pattern.
        BilliorVertexer firstPassVertexer = new BilliorVertexer(bField);
        BilliorVertex firstPassVtx = firstPassVertexer.fitVertex(billTracks);

        double[] newRef = {firstPassVtx.getPosition().z(), firstPassVtx.getPosition().x(), 0.0};
        List<BilliorTrack> shiftedTracks = new ArrayList<BilliorTrack>();
        for (Track track : tracks) {
            BaseTrackState oldTs = (BaseTrackState) TrackStateUtils.getTrackStatesAtLocation(track, TrackState.AtPerigee).get(0);
            double[] newParams = TrackUtils.getParametersAtNewRefPoint(newRef, oldTs);
            SymmetricMatrix newCov = TrackUtils.getCovarianceAtNewRefPoint(newRef, oldTs.getReferencePoint(), oldTs.getParameters(),
                    new SymmetricMatrix(5, oldTs.getCovMatrix(), true));
            BaseTrackState newTs = new BaseTrackState(newParams, newRef, newCov.asPackedArray(true), TrackState.AtPerigee, oldTs.getBLocal());
            shiftedTracks.add(new BilliorTrack(newTs, 0, 0));
        }

        BilliorVertexer billiorVertexer = new BilliorVertexer(bField);
        billiorVertexer.setReferencePosition(newRef);
        BilliorVertex billoirVtx = billiorVertexer.fitVertex(shiftedTracks);
        BilliorVertex kalmanVtxUnconstrained = new NTrackVertexer(bField).fitVertexNoBeamConstraint(tracks);

        Hep3Vector vtxPos = billoirVtx.getPosition();
        Hep3Vector vtxPosErr = billoirVtx.getPositionError();

        Map<String, Double> row = new HashMap<String, Double>();
        row.put("run/I", (double) event.getRunNumber());
        row.put("event/I", (double) event.getEventNumber());
        row.put("vtxX/D", vtxPos.x());
        row.put("vtxY/D", vtxPos.y());
        row.put("vtxZ/D", vtxPos.z());
        row.put("vtxXErr/D", vtxPosErr.x());
        row.put("vtxYErr/D", vtxPosErr.y());
        row.put("vtxZErr/D", vtxPosErr.z());
        row.put("chi2/D", billoirVtx.getChi2());
        row.put("mass/D", billoirVtx.getInvMass());
        putTrackMomentum(row, kalmanVtxUnconstrained, 0, "kalUncEle");
        putTrackMomentum(row, kalmanVtxUnconstrained, 1, "kalUncPos");
        row.put("eleTruthMatched/I", eleMatched ? 1.0 : 0.0);
        row.put("posTruthMatched/I", posMatched ? 1.0 : 0.0);
        row.put("allTruthMatched/I", (eleMatched && posMatched) ? 1.0 : 0.0);
        row.put("apVtxXMC/D", apVtxMC.x());
        row.put("apVtxYMC/D", apVtxMC.y());
        row.put("apVtxZMC/D", apVtxMC.z());
        row.put("mcElePx/D", eleMCMom.x());
        row.put("mcElePy/D", eleMCMom.y());
        row.put("mcElePz/D", eleMCMom.z());
        row.put("mcPosPx/D", posMCMom.x());
        row.put("mcPosPy/D", posMCMom.y());
        row.put("mcPosPz/D", posMCMom.z());
        row.put("recoElePx/D", recoEleMom.x());
        row.put("recoElePy/D", recoEleMom.y());
        row.put("recoElePz/D", recoEleMom.z());
        row.put("recoElePxErr/D", recoEleMomErr.x());
        row.put("recoElePyErr/D", recoEleMomErr.y());
        row.put("recoElePzErr/D", recoEleMomErr.z());
        row.put("recoPosPx/D", recoPosMom.x());
        row.put("recoPosPy/D", recoPosMom.y());
        row.put("recoPosPz/D", recoPosMom.z());
        row.put("recoPosPxErr/D", recoPosMomErr.x());
        row.put("recoPosPyErr/D", recoPosMomErr.y());
        row.put("recoPosPzErr/D", recoPosMomErr.z());

        writeRow(row);
    }

    /**
     * Reads track {@code trackIndex}'s fitted momentum + diagonal error off {@code bv} (set
     * unconditionally by {@code TrackConstraintVertexFitter.fitVertex}'s dispatcher for every
     * track, via {@code getFittedMomentum(i)} and the {@code fitMom{i}_pxErr} custom
     * parameters) and writes them into {@code row} under {@code prefix}. No-ops (leaving the
     * sentinel fill in {@code writeRow} to apply) if {@code bv} is a failed-fit placeholder,
     * signaled by the custom-parameter errors being absent.
     */
    private static void putTrackMomentum(Map<String, Double> row, BilliorVertex bv, int trackIndex, String prefix) {
        Hep3Vector p = bv.getFittedMomentum(trackIndex);
        Double pxErr = bv.getCustomParameters().get("fitMom" + trackIndex + "_pxErr");
        Double pyErr = bv.getCustomParameters().get("fitMom" + trackIndex + "_pyErr");
        Double pzErr = bv.getCustomParameters().get("fitMom" + trackIndex + "_pzErr");
        if (p == null || pxErr == null || pyErr == null || pzErr == null) {
            return;
        }
        row.put(prefix + "Px/D", p.x());
        row.put(prefix + "Py/D", p.y());
        row.put(prefix + "Pz/D", p.z());
        row.put(prefix + "PxErr/D", pxErr);
        row.put(prefix + "PyErr/D", pyErr);
        row.put(prefix + "PzErr/D", pzErr);
    }

    /**
     * Raw pre-vertex-fit momentum and its diagonal error of {@code track}, evaluated at
     * its own point of closest approach (no vertex constraint applied), converted to
     * detector frame. The error comes from propagating the track's own helix-parameter
     * covariance through the same momentum Jacobian the vertex fitters use
     * ({@link TrackConstraintVertexFitter#computeRawMomentum}), so it is directly
     * comparable to the kalUncXPxErr columns -- the "before any vertex fit"
     * baseline. Returns {momentum, momentumError}.
     */
    private Hep3Vector[] getRawMomentumAndError(Track track) {
        TrackState ts = TrackStateUtils.getTrackStatesAtLocation(track, TrackState.AtPerigee).get(0);
        double[] par = ts.getParameters();
        SymmetricMatrix sm = new SymmetricMatrix(5, ts.getCovMatrix(), true);
        RealMatrix cov = MatrixUtils.createRealMatrix(5, 5);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                cov.setEntry(i, j, sm.e(i, j));
            }
        }
        TrackParams tp = new TrackParams(par[0], par[1], par[2], par[3], par[4], cov);
        TrackMomentum tm = new TrackConstraintVertexFitter(bField).computeRawMomentum(tp);

        Hep3Vector pDet = CoordinateTransformations.transformVectorToDetector(
                new BasicHep3Vector(tm.p.getEntry(0), tm.p.getEntry(1), tm.p.getEntry(2)));
        SymmetricMatrix pCovTrk = new SymmetricMatrix(3);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                pCovTrk.setElement(i, j, tm.pCov.getEntry(i, j));
            }
        }
        SymmetricMatrix pCovDet = CoordinateTransformations.transformCovarianceToDetector(pCovTrk);
        Hep3Vector pErrDet = new BasicHep3Vector(
                Math.sqrt(Math.abs(pCovDet.e(0, 0))),
                Math.sqrt(Math.abs(pCovDet.e(1, 1))),
                Math.sqrt(Math.abs(pCovDet.e(2, 2))));
        return new Hep3Vector[]{pDet, pErrDet};
    }

    private void writeRow(Map<String, Double> row) {
        for (String variable : VARIABLES) {
            Double value = row.get(variable);
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
    }

    @Override
    public void endOfData() {
        if (tupleWriter != null) {
            tupleWriter.close();
        }
    }
}
