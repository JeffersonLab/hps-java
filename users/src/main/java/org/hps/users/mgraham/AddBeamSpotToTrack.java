package org.hps.users.mgraham;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import java.util.ArrayList;
import java.util.List;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseLCRelation;
import org.lcsim.fit.helicaltrack.HelicalTrack3DHit;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackFitter;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.BarrelEndcapFlag;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.util.Driver;

/**
 * add the beamspot to an already found track and refit
 * this only works at reconstruction time...it needs the
 * SeedTracks/HelicalTrackHits to work
 *
 * Makes a new Tracks collection called (default) BeamSpotTracks
 *
 * @author mgraham <mgraham@slac.stanford.edu>
 */
public class AddBeamSpotToTrack extends Driver {

    private String trackCollectionName = "MatchedTracks";
    private String bsTrackCollectionName = "BeamSpotTracks";
    private String bsTrackRelationName = "BeamSpotTracksRelation";
    private String ecalCollectionName = "EcalClusters";
    private HelicalTrackFitter _fitter = new HelicalTrackFitter();
//    protected MakeTracks _maketracks;
    double[] _bs = {0, 0, 0};
    double[] _bsCov = {0, 0, 0, 0.2 * 0.2, 0, 0.04 * 0.04};
    private HelicalTrackFitter.FitStatus _status;
    private double bfield = 0.5;

    @Override
    protected void detectorChanged(Detector detector) {

    }

    @Override
    public void process(EventHeader event) {

        if (!event.hasCollection(Track.class, trackCollectionName))
            return;

        List<Track> tracks = event.get(Track.class, trackCollectionName);
        List<SeedTrack> bsTracks = new ArrayList<SeedTrack>();
        List<LCRelation> bsRelations = new ArrayList<LCRelation>();
        for (Track trk : tracks) {
            //make a copy of the track. 
            SeedTrack strk = new SeedTrack((SeedTrack) trk);

            SeedCandidate scand = strk.getSeedCandidate();
            System.out.println("scand has " + scand.getHits().size() + "  hits...");
            SeedCandidate test = new SeedCandidate(scand); //make a new seed candidate
            test.addHit(makeHitOutOfBeamspot(_bs, _bsCov));//and add the hit
            System.out.println("test has " + test.getHits().size() + "  hits...");
            System.out.println("\t\t\tand now scand has " + scand.getHits().size() + "  hits...");

            _status = _fitter.fit(test.getHits(), test.getMSMap(), test.getHelix());
            if (_status != HelicalTrackFitter.FitStatus.Success)
                continue;

            //  Retrieve and save the new helix fit
            test.setHelix(_fitter.getFit());
            SeedTrack bstrack = makeSeedTrack(test);
            bsTracks.add(bstrack);
            bsRelations.add(new BaseLCRelation(bstrack, trk));
            SeedTrack nstrk = new SeedTrack((SeedTrack) trk);

            SeedCandidate nscand = strk.getSeedCandidate();
            System.out.println("\t\t\tand really nscand has " + nscand.getHits().size() + "  hits...");
        }

        //  Make tracks from the final list of track seeds
        int flag = 1 << LCIOConstants.TRBIT_HITS;//I'm not sure what this does, but it's in MakeTracks...mg
        System.out.println("Adding " + bsTracks.size() + " to event ");
        event.put(bsTrackCollectionName, bsTracks, Track.class, flag);
        event.put(bsTrackRelationName, bsRelations, LCRelation.class, flag);
    }

    private HelicalTrackHit makeHitOutOfBeamspot(double[] bs, double[] bsCov) {
        Hep3Vector pos = new BasicHep3Vector(bs);
        SymmetricMatrix cov = new SymmetricMatrix(3);
        cov.setElement(0, 0, bsCov[0]);
        cov.setElement(0, 1, bsCov[1]);
        cov.setElement(0, 2, bsCov[2]);
        cov.setElement(1, 1, bsCov[3]);
        cov.setElement(1, 2, bsCov[4]);
        cov.setElement(2, 2, bsCov[5]);
        double time = 0;
        int lyr = 0;
        BarrelEndcapFlag be = BarrelEndcapFlag.BARREL;
        HelicalTrackHit hit = new HelicalTrack3DHit(pos,
                cov, 0.0, time,
                null, "BeamSpot", lyr, be);
        return hit;
    }

    /*
     *    make a SeedTrack out of a SeedCandidate
     */
    private SeedTrack makeSeedTrack(SeedCandidate trackseed) {
        SeedTrack trk = new SeedTrack();
        //  Initialize the reference point to the origin
        double[] ref = new double[]{0., 0., 0.};
        //  Add the hits to the track
        for (HelicalTrackHit hit : trackseed.getHits())
            trk.addHit((TrackerHit) hit);

        //  Retrieve the helix and save the relevant bits of helix info
        HelicalTrackFit helix = trackseed.getHelix();
        trk.setTrackParameters(helix.parameters(), bfield); // Sets first TrackState.
        trk.setCovarianceMatrix(helix.covariance()); // Modifies first TrackState.
        trk.setChisq(helix.chisqtot());
        trk.setNDF(helix.ndf()[0] + helix.ndf()[1]);

        //  Flag that the fit was successful and set the reference point
        trk.setFitSuccess(true);
        trk.setReferencePoint(ref); // Modifies first TrackState.
        trk.setRefPointIsDCA(true);
        //  Set the SeedCandidate this track is based on
        trk.setSeedCandidate(trackseed);
        return trk;
    }
}
