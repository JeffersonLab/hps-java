package org.hps.recon.tracking.gbl;

import hep.physics.matrix.SymmetricMatrix;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
    public void Process(EventHeader event, Map<GblTrajectory, Track> gblTrajectories, double bfield) {
        
        List<Track> tracks = new ArrayList<Track>();
        
        for(Entry<GblTrajectory, Track> entry : gblTrajectories.entrySet()) {
            
            GblTrajectory traj = entry.getKey();
            Track seed = entry.getValue();

            //  Initialize the reference point to the origin
            double[] ref = new double[] {0., 0., 0.};
            SeedTrack seedTrack = (SeedTrack) seed;
            SeedCandidate trackseed = seedTrack.getSeedCandidate();

            //  Create a new SeedTrack (SeedTrack extends BaseTrack)
            SeedTrack trk = new SeedTrack();

            //  Add the hits to the track
            for (HelicalTrackHit hit : trackseed.getHits()) {
                trk.addHit((TrackerHit) hit);
            }

            //  Retrieve the helix and save the relevant bits of helix info
            HelicalTrackFit helix = trackseed.getHelix();
            double gblParameters[] = getGblCorrectedHelixParameters(helix,traj);
            trk.setTrackParameters(gblParameters, bfield); // Sets first TrackState.
            //trk.setTrackParameters(helix.parameters(), bfield); // Sets first TrackState.
            SymmetricMatrix gblCovariance = getGblCorrectedHelixCovariance(helix, traj);
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
            HelicalTrackFit helix, GblTrajectory traj) {
        // TODO Actually implement this method
        return helix.covariance();
    }

    /**
     * Compute the updated helix parameters.
     * @param helix - original seed track
     * @param traj - fitted GBL trajectory
     * @return corrected parameters.
     */
    private double[] getGblCorrectedHelixParameters(HelicalTrackFit helix, GblTrajectory traj) {
        //TODO Actually implement this method
        return helix.parameters();
    }
    
    public void setTrkCollectionName(String name) {
        _TrkCollectionName = name;
    }

   
   
}
