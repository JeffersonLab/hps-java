package org.hps.analysis.vertex;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.Hep3Vector;

import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.Vertex;
import org.lcsim.util.Driver;

import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.tracking.TrackData;
import org.hps.recon.tracking.TrackStateUtils;
import org.hps.recon.vertexing.BilliorVertex;

import org.lcsim.event.GenericObject;
import org.lcsim.event.LCRelation;

/**
 * Analysis driver that writes a flat ntuple with information from all four
 * three-prong vertex collections:
 * <ul>
 *   <li><b>bsbeam</b> – KF fit with beamspot position + beam 4-momentum constraints</li>
 *   <li><b>unc</b>    – KF fit with no additional constraints</li>
 *   <li><b>bs</b>     – KF fit with beamspot position constraint only</li>
 *   <li><b>mom</b>    – KF fit with beam momentum constraint only (no beamspot)</li>
 * </ul>
 *
 * <p>One row is written per three-prong candidate (ordered by the bsbeam collection).
 * The three daughter particles are stored in the order electron (higher-|p|), positron,
 * recoil electron (lower-|p|), matching the ordering used in HpsReconParticleDriver.
 *
 * <p>The output file uses a colon-separated header line followed by
 * tab-separated data lines, compatible with ROOT's {@code TTree::ReadFile}.
 *
 * <p>Settable parameters (steering file):
 * <ul>
 *   <li>{@code outputFile}        – output file path (default: ThreeProngTuple.txt)</li>
 *   <li>{@code bsbeamCandColName} – beamspot+momentum candidate collection</li>
 *   <li>{@code uncCandColName}    – unconstrained candidate collection</li>
 *   <li>{@code bsCandColName}     – beamspot-only candidate collection</li>
 *   <li>{@code momCandColName}    – momentum-only candidate collection</li>
 * </ul>
 */
public class ThreeProngTupleDriver extends Driver {

    // ---- Configurable parameters -------------------------------------------

    private String outputFile        = "ThreeProngTuple.txt";

    // Candidate collection names (set to match HpsReconParticleDriver defaults)
    private String bsbeamCandColName = "ThreeProngCandidates";
    private String uncCandColName    = "ThreeProngKFUnconstrainedCandidates";
    private String bsCandColName     = "ThreeProngKFBSCandidates";
    private String momCandColName    = "ThreeProngKFMomCandidates";

    // Predicted-track vertex collection names
    private String predEleColName = "ThreeProngPredictedEle";
    private String predPosColName = "ThreeProngPredictedPos";
    private String predRecColName = "ThreeProngPredictedRec";

    // ---- Internal state ----------------------------------------------------

    private PrintWriter writer;
    private final List<String> columns = new ArrayList<>();

    /** Sentinel value written when data is unavailable. */
    private static final double DUMMY = -9999.0;
    private static final String COL_SEP  = ":";   // header separator
    private static final String DATA_SEP = "\t";  // data separator

    // Labels for the four fit types and the three daughter particles
    private static final String[] FIT_PREFIXES  = {"bsbeam", "unc", "bs", "mom"};
    private static final String[] TRK_PREFIXES  = {"ele",    "pos", "rec"};
    private static final String[] TRK_LABELS    = {"Ele",    "Pos", "Rec"};

    private long nProcessed = 0;
    private long nWritten   = 0;

    // ---- Setters -----------------------------------------------------------

    public void setOutputFile(String f)        { this.outputFile        = f; }
    public void setBsbeamCandColName(String s) { this.bsbeamCandColName = s; }
    public void setUncCandColName(String s)    { this.uncCandColName    = s; }
    public void setBsCandColName(String s)     { this.bsCandColName     = s; }
    public void setMomCandColName(String s)    { this.momCandColName    = s; }
    public void setPredEleColName(String s)    { this.predEleColName    = s; }
    public void setPredPosColName(String s)    { this.predPosColName    = s; }
    public void setPredRecColName(String s)    { this.predRecColName    = s; }

    // ---- Driver lifecycle --------------------------------------------------

    @Override
    protected void startOfData() {
        defineColumns();
        try {
            writer = new PrintWriter(outputFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("ThreeProngTupleDriver: cannot open output file: " + outputFile, e);
        }
        // Write header as colon-separated column names (TupleMaker convention)
        StringBuilder hdr = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) hdr.append(COL_SEP);
            hdr.append(columns.get(i));
        }
        writer.println(hdr.toString());
    }

    @Override
    protected void process(EventHeader event) {
        nProcessed++;

        if (!event.hasCollection(ReconstructedParticle.class, bsbeamCandColName))
            return;

        List<ReconstructedParticle> bsbeamCands =
                event.get(ReconstructedParticle.class, bsbeamCandColName);
        if (bsbeamCands.isEmpty())
            return;

        // Retrieve companion collections; may be missing or shorter if a fit failed
        List<ReconstructedParticle> uncCands   = getSafely(event, uncCandColName);
        List<ReconstructedParticle> bsCands    = getSafely(event, bsCandColName);
        List<ReconstructedParticle> momCands   = getSafely(event, momCandColName);

        // Retrieve predicted-track vertex collections (one Vertex per candidate, per predicted track)
        List<Vertex> predEleVerts = getSafelyVertices(event, predEleColName);
        List<Vertex> predPosVerts = getSafelyVertices(event, predPosColName);
        List<Vertex> predRecVerts = getSafelyVertices(event, predRecColName);

        int nCand = bsbeamCands.size();

        for (int i = 0; i < nCand; i++) {
            ReconstructedParticle bsbeamCand = bsbeamCands.get(i);
            ReconstructedParticle uncCand    = safeGet(uncCands,   i);
            ReconstructedParticle bsCand     = safeGet(bsCands,    i);
            ReconstructedParticle momCand    = safeGet(momCands,   i);

            List<Double> vals = new ArrayList<>();

            // -- Event info --------------------------------------------------
            add(vals, (double) event.getRunNumber());
            add(vals, (double) event.getEventNumber());
            add(vals, (double) nCand);

            // -- Vertex fit info for each constraint type --------------------
            //    Order matches FIT_PREFIXES: bsbeam, unc, bs, mom
            for (ReconstructedParticle cand : Arrays.asList(bsbeamCand, uncCand, bsCand, momCand)) {
                addVertexVars(vals, cand);
            }

            // -- Input track parameters (same for all fits; use bsbeam daughters) --
            List<ReconstructedParticle> daughters = bsbeamCand.getParticles();
            for (int di = 0; di < 3; di++) {
                ReconstructedParticle d = (di < daughters.size()) ? daughters.get(di) : null;
                addTrackInputVars(vals, d, event);
            }

            // -- Predicted-track quantities (from dedicated predicted vertex collections) --
            addPredictedVars(vals, safeGetVertex(predEleVerts, i));
            addPredictedVars(vals, safeGetVertex(predPosVerts, i));
            addPredictedVars(vals, safeGetVertex(predRecVerts, i));

            writeRow(vals);
            nWritten++;
        }
    }

    @Override
    protected void endOfData() {
        if (writer != null) writer.close();
        System.out.printf("ThreeProngTupleDriver: processed %d events, wrote %d candidates to %s%n",
                nProcessed, nWritten, outputFile);
    }

    // ---- Column definition -------------------------------------------------

    /**
     * Defines the ordered list of column names (with type suffix /D or /I).
     * The order here must exactly match the order values are added in process().
     */
    private void defineColumns() {
        columns.clear();

        // Event-level
        columns.add("run/I");
        columns.add("event/I");
        columns.add("nCand/I");

        // Per-fit vertex and fitted-momentum variables
        for (String pfx : FIT_PREFIXES) {
            // Vertex position (detector frame: x=horizontal, y=vertical, z=beam)
            columns.add(pfx + "VtxX/D");
            columns.add(pfx + "VtxY/D");
            columns.add(pfx + "VtxZ/D");
            // Vertex position errors (from fitted covariance diagonal)
            columns.add(pfx + "VtxXErr/D");
            columns.add(pfx + "VtxYErr/D");
            columns.add(pfx + "VtxZErr/D");
            // Fit quality
            columns.add(pfx + "Chi2/D");
            columns.add(pfx + "Prob/D");
            columns.add(pfx + "InvMass/D");
            // Fitted momenta + uncertainties for each track (detector frame)
            for (String lbl : TRK_LABELS) {
                columns.add(pfx + lbl + "FitPX/D");
                columns.add(pfx + lbl + "FitPY/D");
                columns.add(pfx + lbl + "FitPZ/D");
                columns.add(pfx + lbl + "FitP/D");
                // Fitted momentum uncertainties (sqrt of diagonal momentum covariance)
                columns.add(pfx + lbl + "FitPXErr/D");
                columns.add(pfx + lbl + "FitPYErr/D");
                columns.add(pfx + lbl + "FitPZErr/D");
                // Fitted helix parameters for this track (post-fit, same ordering as input)
                columns.add(pfx + lbl + "FitD0/D");
                columns.add(pfx + lbl + "FitPhi0/D");
                columns.add(pfx + lbl + "FitOmega/D");
                columns.add(pfx + lbl + "FitZ0/D");
                columns.add(pfx + lbl + "FitTanL/D");
                // Fitted helix parameter uncertainties (sqrt of diagonal post-fit covariance)
                columns.add(pfx + lbl + "FitD0Err/D");
                columns.add(pfx + lbl + "FitPhi0Err/D");
                columns.add(pfx + lbl + "FitOmegaErr/D");
                columns.add(pfx + lbl + "FitZ0Err/D");
                columns.add(pfx + lbl + "FitTanLErr/D");
            }
            // Total fitted momentum and its uncertainties (uncorrelated sum in quadrature)
            columns.add(pfx + "TotFitPX/D");
            columns.add(pfx + "TotFitPY/D");
            columns.add(pfx + "TotFitPZ/D");
            columns.add(pfx + "TotFitP/D");
            columns.add(pfx + "TotFitPXErr/D");
            columns.add(pfx + "TotFitPYErr/D");
            columns.add(pfx + "TotFitPZErr/D");
        }

        // Per-track input parameters (same for all fits; ele=harder e-, pos=e+, rec=softer e-)
        for (String pfx : TRK_PREFIXES) {
            // Reconstructed momentum (from ReconstructedParticle, detector frame)
            columns.add(pfx + "InPX/D");
            columns.add(pfx + "InPY/D");
            columns.add(pfx + "InPZ/D");
            columns.add(pfx + "InP/D");
            // Input 3-momentum uncertainties propagated from helix parameter covariance.
            // Uses analytical Jacobian: det frame (x,y,z) = trk frame (y,z,x).
            // d0 and z0 do not affect 3-momentum so only phi0, omega, tanL contribute.
            columns.add(pfx + "InPXErr/D");
            columns.add(pfx + "InPYErr/D");
            columns.add(pfx + "InPZErr/D");
            // Helix parameters at perigee
            columns.add(pfx + "D0/D");
            columns.add(pfx + "Phi0/D");
            columns.add(pfx + "Omega/D");
            columns.add(pfx + "Z0/D");
            columns.add(pfx + "TanL/D");
            // Helix parameter errors (sqrt of covariance diagonal)
            columns.add(pfx + "D0Err/D");
            columns.add(pfx + "Phi0Err/D");
            columns.add(pfx + "OmegaErr/D");
            columns.add(pfx + "Z0Err/D");
            columns.add(pfx + "TanLErr/D");
            // Track quality and timing
            columns.add(pfx + "TrkChi2/D");
            columns.add(pfx + "NHits/I");
            columns.add(pfx + "IsTop/I");       // 1 = top half (tanLambda > 0), 0 = bottom
            columns.add(pfx + "TrkTime/D");     // track time from KFTrackData
            // Calorimeter cluster (if matched)
            columns.add(pfx + "ClTime/D");
            columns.add(pfx + "ClE/D");
        }

        // Predicted-track quantities from the dedicated predicted-track vertex collections.
        // For each track, the fitter estimates what that track's momentum should be
        // given the vertex and the other two tracks plus the beam momentum constraint.
        // All momenta are in the detector frame (X=HPS_X, Y=HPS_Y, Z=HPS_Z).
        for (String lbl : TRK_LABELS) {
            columns.add("pred" + lbl + "PX/D");        // predicted momentum
            columns.add("pred" + lbl + "PY/D");
            columns.add("pred" + lbl + "PZ/D");
            columns.add("pred" + lbl + "P/D");
            columns.add("pred" + lbl + "PXErr/D");     // predicted momentum uncertainties
            columns.add("pred" + lbl + "PYErr/D");
            columns.add("pred" + lbl + "PZErr/D");
            columns.add("pred" + lbl + "TanL/D");      // predicted tanLambda and phi (+ uncertainties)
            columns.add("pred" + lbl + "TanLErr/D");
            columns.add("pred" + lbl + "Phi/D");
            columns.add("pred" + lbl + "PhiErr/D");
            columns.add("pred" + lbl + "ActPX/D");     // actual (fitted) momentum
            columns.add("pred" + lbl + "ActPY/D");
            columns.add("pred" + lbl + "ActPZ/D");
            columns.add("pred" + lbl + "ActP/D");
            columns.add("pred" + lbl + "ActPXErr/D");  // actual momentum uncertainties
            columns.add("pred" + lbl + "ActPYErr/D");
            columns.add("pred" + lbl + "ActPZErr/D");
            columns.add("pred" + lbl + "ActTanL/D");   // actual tanLambda and phi (+ uncertainties)
            columns.add("pred" + lbl + "ActTanLErr/D");
            columns.add("pred" + lbl + "ActPhi/D");
            columns.add("pred" + lbl + "ActPhiErr/D");
            columns.add("pred" + lbl + "ResX/D");      // residual: predicted - actual
            columns.add("pred" + lbl + "ResY/D");
            columns.add("pred" + lbl + "ResZ/D");
            columns.add("pred" + lbl + "Chi2/D");
            columns.add("pred" + lbl + "Ndf/D");
        }
    }

    // ---- Variable-filling helpers ------------------------------------------

    /**
     * Appends vertex position, errors, chi2, probability, invariant mass,
     * fitted track momenta, and total fitted momentum for one fit type.
     */
    private void addVertexVars(List<Double> vals, ReconstructedParticle cand) {
        if (cand == null) {
            // Fill all slots for this fit type with DUMMY.
            // Slots per track: px,py,pz,|p| + pxErr,pyErr,pzErr + d0,phi0,omega,z0,tanL + 5 errors = 17
            int nSlots = 9              // vtx x/y/z + errors + chi2 + prob + invMass
                       + 3 * 17        // 3 tracks × (4 mom + 3 momErr + 5 fitPar + 5 fitParErr)
                       + 7;            // total px,py,pz,|p| + pxErr,pyErr,pzErr
            for (int k = 0; k < nSlots; k++) add(vals, DUMMY);
            return;
        }

        Vertex v = cand.getStartVertex();
        BilliorVertex bv = toBilliorVertex(v);

        // All named quantities are read from the vertex parameters map — the same mechanism
        // used for vXErr, invMass, and the predicted-track quantities.  This is reliable
        // because BilliorVertex.getParameters() merges _customParameters into the returned map,
        // and setParameter() is called unconditionally by KalmanVertexFitterGainMatrix.fitVertex.
        Map<String, Double> params = v.getParameters();

        // Position
        Hep3Vector pos = v.getPosition();
        add(vals, pos.x());
        add(vals, pos.y());
        add(vals, pos.z());

        // Position errors
        add(vals, getParam(params, "vXErr"));
        add(vals, getParam(params, "vYErr"));
        add(vals, getParam(params, "vZErr"));

        // Fit quality
        add(vals, v.getChi2());
        add(vals, v.getProbability());
        add(vals, getParam(params, "invMass"));

        // Fitted momenta, their uncertainties, and post-fit helix parameters per track
        double totPX = 0, totPY = 0, totPZ = 0;
        double totPXErrSq = 0, totPYErrSq = 0, totPZErrSq = 0;
        for (int ti = 0; ti < 3; ti++) {
            // Fitted momentum (stored in _fittedMomentum map and returned by getFittedMomentum)
            Hep3Vector p = (bv != null) ? bv.getFittedMomentum(ti) : null;
            if (p != null) {
                add(vals, p.x());
                add(vals, p.y());
                add(vals, p.z());
                add(vals, p.magnitude());
                totPX += p.x();
                totPY += p.y();
                totPZ += p.z();
            } else {
                add(vals, DUMMY); add(vals, DUMMY); add(vals, DUMMY); add(vals, DUMMY);
            }

            // Fitted momentum uncertainties — stored as custom params by the KF fitter
            String mpfx = "fitMom" + ti + "_";
            double pxErr = getParam(params, mpfx + "pxErr");
            double pyErr = getParam(params, mpfx + "pyErr");
            double pzErr = getParam(params, mpfx + "pzErr");
            add(vals, pxErr);
            add(vals, pyErr);
            add(vals, pzErr);
            if (pxErr != DUMMY) totPXErrSq += pxErr * pxErr;
            if (pyErr != DUMMY) totPYErrSq += pyErr * pyErr;
            if (pzErr != DUMMY) totPZErrSq += pzErr * pzErr;

            // Post-fit helix parameters and their uncertainties — stored as custom params
            String tpfx = "fitTrk" + ti + "_";
            add(vals, getParam(params, tpfx + "d0"));
            add(vals, getParam(params, tpfx + "phi0"));
            add(vals, getParam(params, tpfx + "omega"));
            add(vals, getParam(params, tpfx + "z0"));
            add(vals, getParam(params, tpfx + "tanL"));
            add(vals, getParam(params, tpfx + "d0Err"));
            add(vals, getParam(params, tpfx + "phi0Err"));
            add(vals, getParam(params, tpfx + "omegaErr"));
            add(vals, getParam(params, tpfx + "z0Err"));
            add(vals, getParam(params, tpfx + "tanLErr"));
        }

        // Total fitted momentum and its uncertainties (tracks treated as uncorrelated)
        add(vals, totPX);
        add(vals, totPY);
        add(vals, totPZ);
        add(vals, Math.sqrt(totPX * totPX + totPY * totPY + totPZ * totPZ));
        add(vals, Math.sqrt(totPXErrSq));
        add(vals, Math.sqrt(totPYErrSq));
        add(vals, Math.sqrt(totPZErrSq));
    }

    /**
     * Appends input track parameters for one daughter particle:
     * reco momentum + uncertainties, helix parameters + errors, track quality, cluster info.
     *
     * <p>Input 3-momentum uncertainties are obtained by propagating the helix parameter
     * covariance through the analytic Jacobian.  In the HPS detector frame (det x = trk y,
     * det y = trk z, det z = trk x) the Jacobian rows for (phi0, omega, tanL) are:
     * <pre>
     *   ∂px_det/∂(phi0,omega,tanL) = ( pz_det, -px_det/omega, 0      )
     *   ∂py_det/∂(phi0,omega,tanL) = ( 0,      -py_det/omega, pt_trk )
     *   ∂pz_det/∂(phi0,omega,tanL) = (-px_det, -pz_det/omega, 0      )
     * </pre>
     * where pt_trk = sqrt(px_det² + pz_det²).  d0 and z0 do not enter.
     */
    private void addTrackInputVars(List<Double> vals, ReconstructedParticle d, EventHeader event) {
        int nSlots = 4   // reco px, py, pz, |p|
                   + 3   // input momentum errors: pxErr, pyErr, pzErr
                   + 5   // d0, phi0, omega, z0, tanL
                   + 5   // parameter errors
                   + 4   // trkChi2, nHits, isTop, trkTime
                   + 2;  // cluster time, energy
        if (d == null || d.getTracks().isEmpty()) {
            for (int k = 0; k < nSlots; k++) add(vals, DUMMY);
            return;
        }

        // Reconstructed momentum (detector frame, from ReconstructedParticle)
        Hep3Vector pm = d.getMomentum();
        double px = pm.x(), py = pm.y(), pz = pm.z();
        add(vals, px);
        add(vals, py);
        add(vals, pz);
        add(vals, pm.magnitude());

        // Helix parameters at perigee
        Track trk = d.getTracks().get(0);
        List<TrackState> states = TrackStateUtils.getTrackStatesAtLocation(trk, TrackState.AtPerigee);
        if (states == null || states.isEmpty()) {
            for (int k = 0; k < nSlots - 4; k++) add(vals, DUMMY);
            return;
        }
        TrackState ts = states.get(0);
        double[] p5 = ts.getParameters();
        // LCIO ordering: [d0, phi0, omega, z0, tanLambda]
        double d0    = p5[0];
        double phi0  = p5[1];
        double omega = p5[2];
        double z0    = p5[3];
        double tanL  = p5[4];

        // Input 3-momentum uncertainties via Jacobian propagation from helix covariance.
        // LCIO packs the lower triangle row-by-row:
        //   index 0=(0,0), 1=(1,0), 2=(1,1), 3=(2,0), 4=(2,1), 5=(2,2),
        //         6=(3,0), 7=(3,1), 8=(3,2), 9=(3,3), 10=(4,0)...14=(4,4)
        SymmetricMatrix cov = new SymmetricMatrix(5, ts.getCovMatrix(), true);
        // Jacobian: rows = (px_det, py_det, pz_det), cols = (d0, phi0, omega, z0, tanL)
        // Only phi0 (col 1), omega (col 2), tanL (col 4) are non-zero.
        double pt_trk = Math.sqrt(px * px + pz * pz); // transverse momentum in tracking (x,z) plane
        double omegaSafe = (Math.abs(omega) > 1e-10) ? omega : 1e-10; // guard division by zero
        double J[][] = new double[3][5];
        // px_det row  (col: d0, phi0, omega, z0, tanL)
        J[0][1] =  pz;              J[0][2] = -px / omegaSafe;
        // py_det row
        J[1][2] = -py / omegaSafe; J[1][4] =  pt_trk;
        // pz_det row
        J[2][1] = -px;             J[2][2] = -pz / omegaSafe;
        // σ²(p_i) = Σ_jk J[i][j] * cov[j][k] * J[i][k]
        double[] pErrSq = new double[3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 5; j++) {
                if (J[i][j] == 0.0) continue;
                for (int k = 0; k < 5; k++) {
                    if (J[i][k] == 0.0) continue;
                    pErrSq[i] += J[i][j] * cov.e(j, k) * J[i][k];
                }
            }
        }
        add(vals, Math.sqrt(Math.abs(pErrSq[0])));  // px_det error
        add(vals, Math.sqrt(Math.abs(pErrSq[1])));  // py_det error
        add(vals, Math.sqrt(Math.abs(pErrSq[2])));  // pz_det error

        add(vals, d0);
        add(vals, phi0);
        add(vals, omega);
        add(vals, z0);
        add(vals, tanL);

        // Parameter errors from covariance diagonal
        add(vals, Math.sqrt(Math.abs(cov.e(0, 0))));  // d0 error
        add(vals, Math.sqrt(Math.abs(cov.e(1, 1))));  // phi0 error
        add(vals, Math.sqrt(Math.abs(cov.e(2, 2))));  // omega error
        add(vals, Math.sqrt(Math.abs(cov.e(3, 3))));  // z0 error
        add(vals, Math.sqrt(Math.abs(cov.e(4, 4))));  // tanL error

        // Track quality and timing
        add(vals, trk.getChi2());
        add(vals, (double) trk.getTrackerHits().size());
        add(vals, tanL > 0 ? 1.0 : 0.0);  // isTop: 1 if upper half of detector

        // Track time from KFTrackData (filled by TrackDataDriver; DUMMY if collection absent)
        double trkTime = DUMMY;
        try {
            if (event.hasCollection(LCRelation.class, TrackData.TRACK_DATA_RELATION_COLLECTION)) {
                GenericObject td = TrackData.getTrackData(event, trk);
                if (td != null) trkTime = TrackData.getTrackTime(td);
            }
        } catch (Exception e) { /* leave DUMMY */ }
        add(vals, trkTime);

        // Calorimeter cluster (if matched)
        if (d.getClusters() != null && !d.getClusters().isEmpty()) {
            Cluster cl = d.getClusters().get(0);
            add(vals, ClusterUtilities.getSeedHitTime(cl));
            add(vals, cl.getEnergy());
        } else {
            add(vals, DUMMY);
            add(vals, DUMMY);
        }
    }

    // ---- Utility methods ---------------------------------------------------

    /** Write a row of values to the output file. */
    private void writeRow(List<Double> vals) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vals.size(); i++) {
            if (i > 0) sb.append(DATA_SEP);
            String col = columns.get(i);
            double v   = vals.get(i);
            if (col.endsWith("/I")) {
                sb.append(Math.round(v));
            } else {
                sb.append(String.format("%g", v));
            }
        }
        writer.println(sb.toString());
    }

    /** Safely convert an LCIO Vertex to a BilliorVertex. */
    private static BilliorVertex toBilliorVertex(Vertex v) {
        if (v instanceof BilliorVertex) return (BilliorVertex) v;
        // Reconstruct from LCIO parameters (handles file-read-back case)
        try { return new BilliorVertex(v); } catch (Exception e) { return null; }
    }

    /**
     * Appends predicted-track variables for one predicted vertex:
     * predicted momentum ± uncertainties, predicted tanLambda + phi ± uncertainties,
     * actual momentum ± uncertainties, actual tanLambda + phi ± uncertainties,
     * residuals, chi2, and ndf. Fills 27 slots; writes DUMMY for all if null.
     *
     * <p>All momenta are in the HPS detector frame (X=horizontal, Y=vertical, Z=beam).
     * Track angles are:
     * <pre>
     *   phi       = atan2(px_det, pz_det)          azimuthal in the bending (x-z) plane
     *   tanLambda = py_det / sqrt(px_det² + pz_det²)   dip angle tangent
     * </pre>
     * Uncertainties on phi and tanLambda are propagated through the full 3×3 momentum
     * covariance stored in the predicted vertex parameters.
     */
    private void addPredictedVars(List<Double> vals, Vertex v) {
        final int N_SLOTS = 27;
        if (v == null) {
            for (int k = 0; k < N_SLOTS; k++) add(vals, DUMMY);
            return;
        }
        Map<String, Double> p = v.getParameters();

        // ---- Predicted momentum ----
        double predPx = getParam(p, "predictedPx");
        double predPy = getParam(p, "predictedPy");
        double predPz = getParam(p, "predictedPz");
        add(vals, predPx);
        add(vals, predPy);
        add(vals, predPz);
        add(vals, (predPx != DUMMY) ? Math.sqrt(predPx*predPx + predPy*predPy + predPz*predPz) : DUMMY);

        // Predicted momentum uncertainties (sqrt of covariance diagonal)
        add(vals, sqrtParam(p, "predictedMomCovXX"));
        add(vals, sqrtParam(p, "predictedMomCovYY"));
        add(vals, sqrtParam(p, "predictedMomCovZZ"));

        // Predicted tanLambda, phi, and their uncertainties
        double[] predAngles = phiTanLFromP(predPx, predPy, predPz, p, "predictedMomCov");
        add(vals, predAngles[0]);  // tanL
        add(vals, predAngles[1]);  // tanLErr
        add(vals, predAngles[2]);  // phi
        add(vals, predAngles[3]);  // phiErr

        // ---- Actual (fitted) momentum ----
        double actPx = getParam(p, "actualPx");
        double actPy = getParam(p, "actualPy");
        double actPz = getParam(p, "actualPz");
        add(vals, actPx);
        add(vals, actPy);
        add(vals, actPz);
        add(vals, (actPx != DUMMY) ? Math.sqrt(actPx*actPx + actPy*actPy + actPz*actPz) : DUMMY);

        // Actual momentum uncertainties (sqrt of covariance diagonal)
        add(vals, sqrtParam(p, "actualMomCovXX"));
        add(vals, sqrtParam(p, "actualMomCovYY"));
        add(vals, sqrtParam(p, "actualMomCovZZ"));

        // Actual tanLambda, phi, and their uncertainties
        double[] actAngles = phiTanLFromP(actPx, actPy, actPz, p, "actualMomCov");
        add(vals, actAngles[0]);  // tanL
        add(vals, actAngles[1]);  // tanLErr
        add(vals, actAngles[2]);  // phi
        add(vals, actAngles[3]);  // phiErr

        // ---- Residuals ----
        add(vals, getParam(p, "residualPx"));
        add(vals, getParam(p, "residualPy"));
        add(vals, getParam(p, "residualPz"));

        // ---- Fit quality ----
        add(vals, v.getChi2());
        add(vals, getParam(p, "ndf"));
    }

    /**
     * Compute phi = atan2(px, pz) and tanLambda = py / pT from 3-momentum in detector
     * frame (X=horizontal, Y=vertical, Z=beam), and propagate uncertainties through the
     * full 3×3 momentum covariance stored in {@code params} under keys
     * {@code covPrefix + "XX/XY/XZ/YY/YZ/ZZ"}.
     *
     * @return [tanLambda, tanLambdaErr, phi, phiErr]; elements are DUMMY if inputs are
     *         invalid or covariance is unavailable.
     */
    private double[] phiTanLFromP(double px, double py, double pz,
                                   Map<String, Double> params, String covPrefix) {
        if (px == DUMMY || py == DUMMY || pz == DUMMY)
            return new double[]{DUMMY, DUMMY, DUMMY, DUMMY};

        double pT2 = px * px + pz * pz;
        double pT  = Math.sqrt(pT2);
        if (pT < 1e-9)
            return new double[]{DUMMY, DUMMY, DUMMY, DUMMY};

        double tanL = py / pT;
        double phi  = Math.atan2(px, pz);

        double tanLErr = DUMMY, phiErr = DUMMY;

        double covXX = getParam(params, covPrefix + "XX");
        double covYY = getParam(params, covPrefix + "YY");
        double covZZ = getParam(params, covPrefix + "ZZ");
        double covXY = getParam(params, covPrefix + "XY");
        double covXZ = getParam(params, covPrefix + "XZ");
        double covYZ = getParam(params, covPrefix + "YZ");

        if (covXX != DUMMY && covYY != DUMMY && covZZ != DUMMY
                && covXY != DUMMY && covXZ != DUMMY && covYZ != DUMMY) {
            // phi = atan2(px, pz) → ∂phi/∂[px,py,pz] = [pz/pT², 0, -px/pT²]
            double JphiX = pz / pT2;
            double JphiZ = -px / pT2;
            double varPhi = JphiX * JphiX * covXX
                          + JphiZ * JphiZ * covZZ
                          + 2.0 * JphiX * JphiZ * covXZ;
            phiErr = (varPhi >= 0) ? Math.sqrt(varPhi) : DUMMY;

            // tanL = py/pT → ∂tanL/∂[px,py,pz] = [-py*px/pT³, 1/pT, -py*pz/pT³]
            double pT3 = pT * pT2;
            double JtX = -py * px / pT3;
            double JtY =  1.0 / pT;
            double JtZ = -py * pz / pT3;
            double varTanL = JtX*JtX*covXX + JtY*JtY*covYY + JtZ*JtZ*covZZ
                           + 2.0*JtX*JtY*covXY + 2.0*JtX*JtZ*covXZ + 2.0*JtY*JtZ*covYZ;
            tanLErr = (varTanL >= 0) ? Math.sqrt(varTanL) : DUMMY;
        }
        return new double[]{tanL, tanLErr, phi, phiErr};
    }

    /** Return sqrt of a covariance parameter, or DUMMY if absent or negative. */
    private static double sqrtParam(Map<String, Double> params, String key) {
        if (!params.containsKey(key)) return DUMMY;
        double val = params.get(key);
        return val >= 0 ? Math.sqrt(val) : DUMMY;
    }

    /** Return param value or DUMMY if key is absent. */
    private static double getParam(Map<String, Double> params, String key) {
        return params.containsKey(key) ? params.get(key) : DUMMY;
    }

    /** Append a double value to the list. */
    private static void add(List<Double> vals, double v) {
        vals.add(v);
    }

    /** Get a ReconstructedParticle collection from the event, or null if absent. */
    private static List<ReconstructedParticle> getSafely(EventHeader event, String name) {
        if (event.hasCollection(ReconstructedParticle.class, name))
            return event.get(ReconstructedParticle.class, name);
        return null;
    }

    /** Get a Vertex collection from the event, or null if absent. */
    private static List<Vertex> getSafelyVertices(EventHeader event, String name) {
        if (event.hasCollection(Vertex.class, name))
            return event.get(Vertex.class, name);
        return null;
    }

    /** Return element i of the list, or null if the list is null or too short. */
    private static ReconstructedParticle safeGet(List<ReconstructedParticle> list, int i) {
        return (list != null && i < list.size()) ? list.get(i) : null;
    }

    /** Return Vertex element i of the list, or null if the list is null or too short. */
    private static Vertex safeGetVertex(List<Vertex> list, int i) {
        return (list != null && i < list.size()) ? list.get(i) : null;
    }
}
