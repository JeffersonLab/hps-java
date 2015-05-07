package org.hps.recon.tracking.gbl;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.recon.tracking.gbl.matrix.SymMatrix;
import org.hps.recon.tracking.gbl.matrix.Vector;
import org.hps.util.BasicLogFormatter;
import org.lcsim.constants.Constants;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.util.log.LogUtil;


/**
 * A class that creates track objects from fitted GBL trajectories and adds them into the event.
 * 
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class MakeGblTracks {


    private String _TrkCollectionName = "GblTracks";
    private static Logger logger = LogUtil.create(MakeGblTracks.class, new BasicLogFormatter());
    
    /**
     * Creates a new instance of MakeTracks.
     */
    public MakeGblTracks() {
         //logger = Logger.getLogger(getClass().getName());
         //logger.setUseParentHandlers(false);
        //Handler handler = new StreamHandler(System.out, new SimpleFormatter());
        //logger.addHandler(handler);
        logger.setLevel(Level.INFO);
//        try {
//            logger.addHandler(new FileHandler(MakeGblTracks.class.getSimpleName()+".log"));
//        } catch (SecurityException | IOException e) {
//            e.printStackTrace();
//        }

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
        
        logger.info("adding " + gblTrajectories.size() + " of fitted GBL tracks to the event");
        
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
            //TODO Use GBL covariance matrix
            //SymmetricMatrix gblCovariance = getGblCorrectedHelixCovariance(helix, fittedTraj, bfield);
            trk.setCovarianceMatrix(helix.covariance()); // Modifies first TrackState.
            trk.setChisq( fittedTraj.get_chi2());
            trk.setNDF(fittedTraj.get_ndf());

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
            logger.info(String.format("helix chi2 %f ndf %d gbl chi2 %f ndf %d\n", helix.chisqtot(), helix.ndf()[0]+helix.ndf()[1], trk.getChi2(), trk.getNDF()));
            if(logger.getLevel().intValue()<= Level.INFO.intValue()) {
                for(int i=0;i<5;++i) {
                    logger.info(String.format("param %d: %.5f -> %.5f    helix-gbl= %f", i, helix.parameters()[i], trk.getTrackParameter(i), helix.parameters()[i]-trk.getTrackParameter(i)));
                }
            }
            
        }

        logger.info("adding " + Integer.toString(tracks.size()) + " Gbl tracks to event with " + event.get(Track.class, "MatchedTracks").size() + " matched tracks");
        
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
        double q = traj.get_seed().getCharge();
        double qOverP = q/p;
        
        // get corrections from GBL fit
        Vector locPar = new Vector(5);
        SymMatrix locCov = new SymMatrix(5);
        traj.get_traj().getResults(1, locPar, locCov); // vertex point
        double qOverPCorr = locPar.get(FittedGblTrajectory.GBLPARIDX.QOVERP.getValue());
        double xTCorr = locPar.get(FittedGblTrajectory.GBLPARIDX.XT.getValue());
        double yTCorr = locPar.get(FittedGblTrajectory.GBLPARIDX.YT.getValue());
        double xTPrimeCorr = locPar.get(FittedGblTrajectory.GBLPARIDX.XTPRIME.getValue());
        double yTPrimeCorr = locPar.get(FittedGblTrajectory.GBLPARIDX.YTPRIME.getValue());
        
        // calculate new d0 and z0
        Hep3Matrix perToClPrj = traj.get_track_data().getPrjPerToCl();
        Hep3Matrix clToPerPrj = VecOp.inverse(perToClPrj);
        Hep3Vector corrPer = VecOp.mult(clToPerPrj, new BasicHep3Vector(xTCorr, yTCorr, 0.0));

        //d0
        double d0_corr = -1.0*corrPer.y(); // correct for different sign convention of d0 in curvilinear frame
        double d0_gbl = d0 + d0_corr;
        
        //z0
        double z0_corr = corrPer.z();
        double z0_gbl = z0 + z0_corr;
        
        //calculate new phi0
        double phi0_gbl = phi0 + xTPrimeCorr;
        
        //calculate new slope
        double lambda_gbl = Math.atan(slope) + yTPrimeCorr;
        double slope_gbl = Math.tan( lambda_gbl );

        // calculate new curvature
        
        double qOverP_gbl = qOverP + qOverPCorr;
        double pt_gbl = Math.abs(1.0/qOverP_gbl) * Math.sin((Math.PI/2.0-lambda_gbl));
        double C_gbl = Constants.fieldConversion * bfield / pt_gbl;
        //make sure sign is not changed
        C_gbl = Math.signum(helix.curvature())*Math.abs(C_gbl); 
        
        logger.info("qOverP="+qOverP+" qOverPCorr="+qOverPCorr+" qOverP_gbl="+qOverP_gbl+" ==> pGbl="+1.0/qOverP_gbl);
        
        
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
