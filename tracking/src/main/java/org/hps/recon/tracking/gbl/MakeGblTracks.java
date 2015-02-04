package org.hps.recon.tracking.gbl;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hps.recon.tracking.gbl.HpsGblRefitter.FittedGblTrajectory;
import org.hps.recon.tracking.gbl.matrix.Matrix;
import org.hps.recon.tracking.gbl.matrix.SymMatrix;
import org.hps.recon.tracking.gbl.matrix.Vector;
import org.lcsim.constants.Constants;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;

public class MakeGblTracks {


    private String _TrkCollectionName = "GblTracks";

    /**
     * Creates a new instance of MakeTracks.
     */
    public MakeGblTracks() {
    }
    
    /**
     * Process a Gbl track and store it into the event
     * @param event event header
     * @param track Gbl trajectory
     * @param seed SeedTrack
     * @param bfield magnetic field (used to turn curvature into momentum)
     */
    public void Process(EventHeader event, List<FittedGblTrajectory> gblTrajectories, double bfield) {
        
        List<Track> tracks = new ArrayList<Track>();
        
        for(FittedGblTrajectory fittedTraj : gblTrajectories) {
            
            
            //  Initialize the reference point to the origin
            double[] ref = new double[] {0., 0., 0.};
            SeedTrack seedTrack = (SeedTrack) fittedTraj.get_seed();
            SeedCandidate trackseed = seedTrack.getSeedCandidate();

            //  Create a new SeedTrack (SeedTrack extends BaseTrack)
            SeedTrack trk = new SeedTrack();

            //  Add the hits to the track
            for (HelicalTrackHit hit : trackseed.getHits()) {
                trk.addHit((TrackerHit) hit);
            }

            //  Retrieve the helix and save the relevant bits of helix info
            HelicalTrackFit helix = trackseed.getHelix();
            double gblParameters[] = getGblCorrectedHelixParameters(helix,fittedTraj, bfield);
            trk.setTrackParameters(gblParameters, bfield); // Sets first TrackState.
            //trk.setTrackParameters(helix.parameters(), bfield); // Sets first TrackState.
            SymmetricMatrix gblCovariance = getGblCorrectedHelixCovariance(helix, fittedTraj, bfield);
            trk.setCovarianceMatrix(gblCovariance); // Modifies first TrackState.
            //trk.setCovarianceMatrix(helix.covariance()); // Modifies first TrackState.
            trk.setChisq(helix.chisqtot());
            trk.setNDF(helix.ndf()[0]+helix.ndf()[1]);

            //  Flag that the fit was successful and set the reference point
            trk.setFitSuccess(true);
            trk.setReferencePoint(ref); // Modifies first TrackState.
            trk.setRefPointIsDCA(true);

            //      Set the strategy used to find this track
            trk.setStratetgy(trackseed.getSeedStrategy());

            //  Set the SeedCandidate this track is based on
            trk.setSeedCandidate(trackseed);

            // Check the track - hook for plugging in external constraint
            //if ((_trackCheck != null) && (! _trackCheck.checkTrack(trk))) continue;

            //  Add the track to the list of tracks
            tracks.add((Track) trk);
        }

        // Put the tracks back into the event and exit
        int flag = 1<<LCIOConstants.TRBIT_HITS;
        event.put(_TrkCollectionName, tracks, Track.class, flag);

        return;
    }

    /**
     * Compute the track fit covariance matrix
     * @param helix - original seed track
     * @param traj - fitted GBL trajectory
     * @return covariance matrix.
     */
    private SymmetricMatrix getGblCorrectedHelixCovariance(
            HelicalTrackFit helix, FittedGblTrajectory traj, double bfield) {
        // TODO Actually implement this method
        return helix.covariance();
    }

    /**
     * Compute the updated helix parameters.
     * @param helix - original seed track
     * @param traj - fitted GBL trajectory
     * @return corrected parameters.
     */
    private double[] getGblCorrectedHelixParameters(HelicalTrackFit helix, FittedGblTrajectory traj, double bfield) {
        
        // get seed helix parameters
        double d0 = helix.dca();
        double z0 = helix.z0();
        double phi0 = helix.phi0();
        double slope = helix.slope();
        double p = helix.p(bfield);
        double qOverP = traj.get_seed().getCharge()/p;
        
        // get corrections from GBL fit
        Vector locPar = new Vector(5);
        SymMatrix locCov = new SymMatrix(5);
        traj.get_traj().getResults(1, locPar, locCov); // vertex point
        double qOverPCorr = locPar.get(FittedGblTrajectory.GBLPARIDX.QOVERP.getValue());
        double xTCorr = locPar.get(FittedGblTrajectory.GBLPARIDX.XT.getValue());
        double yTCorr = locPar.get(FittedGblTrajectory.GBLPARIDX.YT.getValue());
        double xTPrimeCorr = locPar.get(FittedGblTrajectory.GBLPARIDX.XTPRIME.getValue());
        double yTPrimeCorr = locPar.get(FittedGblTrajectory.GBLPARIDX.YTPRIME.getValue());
        
        // calculate new d0
        // correct for different sign convention of d0 in curvilinear frame
        Hep3Matrix perToClPrj = traj.get_track_data().getPrjPerToCl();
        Hep3Matrix clToPerPrj = VecOp.inverse(perToClPrj);
        Hep3Vector vec_out = VecOp.mult(clToPerPrj, new BasicHep3Vector(xTCorr, yTCorr, 0.0));
        //double d0_corr = -1.0 * self.track.prjClToPer(self.xTCorr(label),self.yTCorr(label))[1];
        double d0_corr = -1.0*vec_out.y(); // correct for different sign convention of d0 in curvilinear frame
        double d0_gbl = d0 + d0_corr;
        
        // calculate new z0
        //return self.track.prjClToPer(self.xTCorr(label),self.yTCorr(label))[2]
        double z0_corr = vec_out.z();
        double z0_gbl = z0 + z0_corr;
        
        // calculate new curvature
        //      return self.track.qOverP(bfac) + self.curvCorr()
        double qOverP_gbl = qOverP + qOverPCorr;
        double p_gbl = traj.get_seed().getCharge()/qOverP_gbl;
        double pt_gbl = p_gbl * helix.sth();
        double C_gbl = Constants.fieldConversion * bfield / pt_gbl;
        C_gbl = Math.signum(helix.curvature())*Math.abs(C_gbl); // fix sign, don't think I need to do all this
        

        // TODO Is the below really true?
        
        //calculate new phi0
        double phi0_gbl = phi0 + Math.atan(xTPrimeCorr);
        // prorate by dip angle
        phi0_gbl = phi0_gbl/Math.sin(Math.atan(slope));

        //calculate new slope
        // A correction to track direction yT' in curvilinear frame is proportional 
        // to a correction on the dip angle lambda
        double slope_gbl = Math.tan( Math.atan(helix.slope()) + yTPrimeCorr);
        
        
        
        double parameters_gbl[] = new double[5];
        parameters_gbl[HelicalTrackFit.dcaIndex] = d0_gbl;
        parameters_gbl[HelicalTrackFit.z0Index] =  z0_gbl;
        parameters_gbl[HelicalTrackFit.curvatureIndex] = C_gbl;
        parameters_gbl[HelicalTrackFit.slopeIndex] = slope_gbl;
        parameters_gbl[HelicalTrackFit.phi0Index] = phi0_gbl;
        
        return parameters_gbl;
    }
    
    public void setTrkCollectionName(String name) {
        _TrkCollectionName = name;
    }

   
   
}
