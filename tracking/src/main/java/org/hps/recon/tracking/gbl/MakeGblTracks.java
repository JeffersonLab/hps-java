package org.hps.recon.tracking.gbl;

import static org.hps.recon.tracking.gbl.GBLOutput.getPerToClPrj;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.math3.util.Pair;
import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.gbl.matrix.Matrix;
import org.hps.recon.tracking.gbl.matrix.SymMatrix;
import org.hps.recon.tracking.gbl.matrix.Vector;
import org.lcsim.constants.Constants;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseTrack;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;

/**
 * A class that creates track objects from fitted GBL trajectories and adds them
 * into the event.
 *
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class MakeGblTracks {

    private String _TrkCollectionName = "GBLTracks";
    private static Logger LOGGER = Logger.getLogger(MakeGblTracks.class.getPackage().getName());

    /**
     * Creates a new instance of MakeTracks.
     */
    public MakeGblTracks() {
    }

    public void setDebug(boolean debug) {
        if (debug) {
            LOGGER.setLevel(Level.INFO);
        } else {
            LOGGER.setLevel(Level.OFF);
        }
    }

    /**
     * Process a Gbl track and store it into the event
     *
     * @param event event header
     * @param track Gbl trajectory
     * @param seed SeedTrack
     * @param bfield magnetic field (used to turn curvature into momentum)
     */
    public void Process(EventHeader event, List<FittedGblTrajectory> gblTrajectories, double bfield) {

        List<Track> tracks = new ArrayList<Track>();

        LOGGER.info("adding " + gblTrajectories.size() + " of fitted GBL tracks to the event");

        for (FittedGblTrajectory fittedTraj : gblTrajectories) {

            SeedTrack seedTrack = (SeedTrack) fittedTraj.get_seed();
            SeedCandidate trackseed = seedTrack.getSeedCandidate();

            //  Create a new Track
            Track trk = makeCorrectedTrack(fittedTraj, trackseed.getHelix(), seedTrack.getTrackerHits(), seedTrack.getType(), bfield);

            //  Add the track to the list of tracks
            tracks.add(trk);
        }

        LOGGER.info("adding " + Integer.toString(tracks.size()) + " Gbl tracks to event with " + event.get(Track.class, "MatchedTracks").size() + " matched tracks");

        // Put the tracks back into the event and exit
        int flag = 1 << LCIOConstants.TRBIT_HITS;
        event.put(_TrkCollectionName, tracks, Track.class, flag);
    }

    public static Track makeCorrectedTrack(FittedGblTrajectory fittedTraj, HelicalTrackFit helix, List<TrackerHit> trackHits, int trackType, double bfield) {
        //  Initialize the reference point to the origin
        double[] ref = new double[]{0., 0., 0.};

        //  Create a new SeedTrack
        BaseTrack trk = new BaseTrack();

        //  Add the hits to the track
        for (TrackerHit hit : trackHits) {
            trk.addHit(hit);
        }

        // Set state at vertex
        Pair<double[], SymmetricMatrix> correctedHelixParams = getGblCorrectedHelixParameters(helix, fittedTraj.get_traj(), bfield, FittedGblTrajectory.GBLPOINT.IP);
        trk.setTrackParameters(correctedHelixParams.getFirst(), bfield);// hack to set the track charge
        trk.getTrackStates().clear();

        TrackState stateVertex = new BaseTrackState(correctedHelixParams.getFirst(), ref, correctedHelixParams.getSecond().asPackedArray(true), TrackState.AtIP, bfield);
        trk.getTrackStates().add(stateVertex);

        // Set state at last point
        Pair<double[], SymmetricMatrix> correctedHelixParamsLast = getGblCorrectedHelixParameters(helix, fittedTraj.get_traj(), bfield, FittedGblTrajectory.GBLPOINT.LAST);
        TrackState stateLast = new BaseTrackState(correctedHelixParamsLast.getFirst(), ref, correctedHelixParamsLast.getSecond().asPackedArray(true), TrackState.AtLastHit, bfield);
        trk.getTrackStates().add(stateLast);

        // Set other info needed
        trk.setChisq(fittedTraj.get_chi2());
        trk.setNDF(fittedTraj.get_ndf());
        trk.setFitSuccess(true);
        trk.setRefPointIsDCA(true);
        trk.setTrackType(TrackType.setGBL(trackType, true));

        //  Add the track to the list of tracks
//            tracks.add(trk);
        LOGGER.info(String.format("helix chi2 %f ndf %d gbl chi2 %f ndf %d\n", helix.chisqtot(), helix.ndf()[0] + helix.ndf()[1], trk.getChi2(), trk.getNDF()));
        if (LOGGER.getLevel().intValue() <= Level.INFO.intValue()) {
            for (int i = 0; i < 5; ++i) {
                LOGGER.info(String.format("param %d: %.10f -> %.10f    helix-gbl= %f", i, helix.parameters()[i], trk.getTrackParameter(i), helix.parameters()[i] - trk.getTrackParameter(i)));
            }
        }
        return trk;
    }

    /**
     * Compute the updated helix parameters and covariance matrix at a given
     * point along the trajectory.
     *
     * @param helix - original seed track
     * @param traj - fitted GBL trajectory
     * @param point - the point along the track where the result is computed.
     * @return corrected parameters.
     */
    public static Pair<double[], SymmetricMatrix> getGblCorrectedHelixParameters(HelicalTrackFit helix, GblTrajectory traj, double bfield, FittedGblTrajectory.GBLPOINT point) {

        // get seed helix parameters
        double qOverP = helix.curvature() / (Constants.fieldConversion * Math.abs(bfield) * Math.sqrt(1 + Math.pow(helix.slope(), 2)));
        double d0 = -1.0 * helix.dca(); // correct for different sign convention of d0 in perigee frame
        double z0 = helix.z0();
        double phi0 = helix.phi0();
        double lambda = Math.atan(helix.slope());

        LOGGER.info("GblPoint: " + point.toString() + "( " + point.name() + ")");
        LOGGER.info(String.format("original helix: d0=%f, z0=%f, omega=%f, tanlambda=%f, phi0=%f, p=%f", helix.dca(), helix.z0(), helix.curvature(), helix.slope(), helix.phi0(), helix.p(Math.abs(bfield))));
        LOGGER.info("original helix covariance:\n" + helix.covariance());

        // get corrections from GBL fit
        Vector locPar = new Vector(5);
        SymMatrix locCov = new SymMatrix(5);
        int pointIndex;
        if (point.compareTo(FittedGblTrajectory.GBLPOINT.IP) == 0) {
            pointIndex = 1;
        } else if (point.compareTo(FittedGblTrajectory.GBLPOINT.LAST) == 0) {
            pointIndex = traj.getNumPoints();
        } else {
            throw new RuntimeException("This GBLPOINT " + point.toString() + "( " + point.name() + ") is not valid");
        }

        traj.getResults(pointIndex, locPar, locCov); // vertex point
//        locCov.print(10, 8);
        double qOverPCorr = locPar.get(FittedGblTrajectory.GBLPARIDX.QOVERP.getValue());
        double xTPrimeCorr = locPar.get(FittedGblTrajectory.GBLPARIDX.XTPRIME.getValue());
        double yTPrimeCorr = locPar.get(FittedGblTrajectory.GBLPARIDX.YTPRIME.getValue());
        double xTCorr = locPar.get(FittedGblTrajectory.GBLPARIDX.XT.getValue());
        double yTCorr = locPar.get(FittedGblTrajectory.GBLPARIDX.YT.getValue());

        LOGGER.info((helix.slope() > 0 ? "top: " : "bot ") + "qOverPCorr " + qOverPCorr + " xtPrimeCorr " + xTPrimeCorr + " yTPrimeCorr " + yTPrimeCorr + " xTCorr " + xTCorr + " yTCorr " + yTCorr);

        // calculate new d0 and z0
//        Hep3Matrix perToClPrj = traj.get_track_data().getPrjPerToCl();
        Hep3Matrix perToClPrj = getPerToClPrj(helix);

        Hep3Matrix clToPerPrj = VecOp.inverse(perToClPrj);
        Hep3Vector corrPer = VecOp.mult(clToPerPrj, new BasicHep3Vector(xTCorr, yTCorr, 0.0));

        //d0
        double d0_corr = corrPer.y();
        double dca_gbl = -1.0 * (d0 + d0_corr);

        //z0
        double z0_corr = corrPer.z();
        double z0_gbl = z0 + z0_corr;

        //calculate new slope
        double lambda_gbl = lambda + yTPrimeCorr;
        double slope_gbl = Math.tan(lambda_gbl);

        // calculate new curvature
        double qOverP_gbl = qOverP + qOverPCorr;
//        double pt_gbl = (1.0 / qOverP_gbl) * Math.cos(lambda_gbl);
//        double C_gbl = Constants.fieldConversion * Math.abs(bfield) / pt_gbl;
        double C_gbl = Constants.fieldConversion * Math.abs(bfield) * qOverP_gbl / Math.cos(lambda_gbl);

        //calculate new phi0
        double phi0_gbl = phi0 + xTPrimeCorr - corrPer.x() * C_gbl;

        LOGGER.info("qOverP=" + qOverP + " qOverPCorr=" + qOverPCorr + " qOverP_gbl=" + qOverP_gbl + " ==> pGbl=" + 1.0 / qOverP_gbl + " C_gbl=" + C_gbl);

        LOGGER.info(String.format("corrected helix: d0=%f, z0=%f, omega=%f, tanlambda=%f, phi0=%f, p=%f", dca_gbl, z0_gbl, C_gbl, slope_gbl, phi0_gbl, Math.abs(1 / qOverP_gbl)));

        /*
         // Strandlie, Wittek, NIMA 566, 2006
         Matrix covariance_gbl = new Matrix(5, 5);
         //helpers
         double Bz = -Constants.fieldConversion * Math.abs(bfield); // TODO sign convention and should it be it scaled from Telsa?
         double p = Math.abs(1 / qOverP_gbl);
         double q = Math.signum(qOverP_gbl);
         double tanLambda = Math.tan(lambda_gbl);
         double cosLambda = Math.cos(lambda_gbl);
         //        Hep3Vector B = new BasicHep3Vector(0, 0, Bz); // TODO sign convention?
         Hep3Vector H = new BasicHep3Vector(0, 0, 1);
         Hep3Vector T = HelixUtils.Direction(helix, 0.);
         Hep3Vector HcrossT = VecOp.cross(H, T);
         double alpha = HcrossT.magnitude(); // this should be Bvec cross TrackDir/|B|
         double Q = Bz * q / p;
         Hep3Vector Z = new BasicHep3Vector(0, 0, 1);
         Hep3Vector J = VecOp.mult(1. / VecOp.cross(T, Z).magnitude(), VecOp.cross(T, Z));
         Hep3Vector K = Z;
         Hep3Vector U = VecOp.mult(-1, J);
         Hep3Vector V = VecOp.cross(T, U);
         Hep3Vector I = VecOp.cross(J, K);
         Hep3Vector N = VecOp.mult(1 / alpha, VecOp.cross(H, T));
         double UdotI = VecOp.dot(U, I);
         double NdotV = VecOp.dot(N, V);
         double NdotU = VecOp.dot(N, U);
         double TdotI = VecOp.dot(T, I);
         double VdotI = VecOp.dot(V, I);
         double VdotK = VecOp.dot(V, K);
         covariance_gbl.set(HelicalTrackFit.dcaIndex, FittedGblTrajectory.GBLPARIDX.XT.getValue(), VdotK / TdotI);
         covariance_gbl.set(HelicalTrackFit.phi0Index, FittedGblTrajectory.GBLPARIDX.XTPRIME.getValue(), 1);
         covariance_gbl.set(HelicalTrackFit.phi0Index, FittedGblTrajectory.GBLPARIDX.XT.getValue(), -alpha * Q * UdotI * NdotU / (cosLambda * TdotI));
         covariance_gbl.set(HelicalTrackFit.phi0Index, FittedGblTrajectory.GBLPARIDX.YT.getValue(), -alpha * Q * VdotI * NdotU / (cosLambda * TdotI));
         covariance_gbl.set(HelicalTrackFit.curvatureIndex, FittedGblTrajectory.GBLPARIDX.QOVERP.getValue(), -1 * Bz / cosLambda);
         //        covariance_gbl.set(HelicalTrackFit.curvatureIndex, FittedGblTrajectory.GBLPARIDX.XTPRIME.getValue(), 0);
         covariance_gbl.set(HelicalTrackFit.curvatureIndex, FittedGblTrajectory.GBLPARIDX.YTPRIME.getValue(), -1 * q * Bz * tanLambda / (p * cosLambda));
         covariance_gbl.set(HelicalTrackFit.curvatureIndex, FittedGblTrajectory.GBLPARIDX.XT.getValue(), q * Bz * alpha * Q * tanLambda * UdotI * NdotV / (p * cosLambda * TdotI));
         covariance_gbl.set(HelicalTrackFit.curvatureIndex, FittedGblTrajectory.GBLPARIDX.YT.getValue(), q * Bz * alpha * Q * tanLambda * VdotI * NdotV / (p * cosLambda * TdotI));
         covariance_gbl.set(HelicalTrackFit.z0Index, FittedGblTrajectory.GBLPARIDX.YT.getValue(), -1 / TdotI);
         covariance_gbl.set(HelicalTrackFit.slopeIndex, FittedGblTrajectory.GBLPARIDX.YTPRIME.getValue(), -1);
         covariance_gbl.set(HelicalTrackFit.slopeIndex, FittedGblTrajectory.GBLPARIDX.XT.getValue(), alpha * Q * UdotI * NdotV / TdotI);
         covariance_gbl.set(HelicalTrackFit.slopeIndex, FittedGblTrajectory.GBLPARIDX.YT.getValue(), alpha * Q * VdotI * NdotV / TdotI);

         covariance_gbl.print(15, 13);
         */
        // Sho's magic
        Matrix jacobian = new Matrix(5, 5);
        jacobian.set(HelicalTrackFit.dcaIndex, FittedGblTrajectory.GBLPARIDX.XT.getValue(), -clToPerPrj.e(1, 0));
        jacobian.set(HelicalTrackFit.dcaIndex, FittedGblTrajectory.GBLPARIDX.YT.getValue(), -clToPerPrj.e(1, 1));
        jacobian.set(HelicalTrackFit.phi0Index, FittedGblTrajectory.GBLPARIDX.XTPRIME.getValue(), 1.0);
        jacobian.set(HelicalTrackFit.phi0Index, FittedGblTrajectory.GBLPARIDX.YT.getValue(), clToPerPrj.e(0, 1) * C_gbl);
        jacobian.set(HelicalTrackFit.curvatureIndex, FittedGblTrajectory.GBLPARIDX.QOVERP.getValue(), Constants.fieldConversion * Math.abs(bfield) / Math.cos(lambda_gbl));
        jacobian.set(HelicalTrackFit.curvatureIndex, FittedGblTrajectory.GBLPARIDX.YTPRIME.getValue(), Constants.fieldConversion * Math.abs(bfield) * qOverP_gbl * Math.tan(lambda_gbl) / Math.cos(lambda_gbl));
        jacobian.set(HelicalTrackFit.z0Index, FittedGblTrajectory.GBLPARIDX.XT.getValue(), clToPerPrj.e(2, 0));
        jacobian.set(HelicalTrackFit.z0Index, FittedGblTrajectory.GBLPARIDX.YT.getValue(), clToPerPrj.e(2, 1));
        jacobian.set(HelicalTrackFit.slopeIndex, FittedGblTrajectory.GBLPARIDX.YTPRIME.getValue(), Math.pow(Math.cos(lambda_gbl), -2.0));

//        jacobian.print(15, 13);
        Matrix helixCovariance = jacobian.times(locCov.times(jacobian.transpose()));
        SymmetricMatrix cov = new SymmetricMatrix(5);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                if (i >= j) {
                    cov.setElement(i, j, helixCovariance.get(i, j));
                }
            }
        }
        LOGGER.info("corrected helix covariance:\n" + cov);

        double parameters_gbl[] = new double[5];
        parameters_gbl[HelicalTrackFit.dcaIndex] = dca_gbl;
        parameters_gbl[HelicalTrackFit.phi0Index] = phi0_gbl;
        parameters_gbl[HelicalTrackFit.curvatureIndex] = C_gbl;
        parameters_gbl[HelicalTrackFit.z0Index] = z0_gbl;
        parameters_gbl[HelicalTrackFit.slopeIndex] = slope_gbl;

        return new Pair<double[], SymmetricMatrix>(parameters_gbl, cov);
    }

    public void setTrkCollectionName(String name) {
        _TrkCollectionName = name;
    }

}
