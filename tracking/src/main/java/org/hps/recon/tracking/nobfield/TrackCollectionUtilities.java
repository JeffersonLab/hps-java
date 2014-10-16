package org.hps.recon.tracking.nobfield;

import java.util.ArrayList;
import java.util.List;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;

/**
 * * Some methods which manipulate track collections in various ways
 *
 * @author mgraham -- date created 7/9/2014
 */
public class TrackCollectionUtilities {

    public static boolean pruneTrackList(List<Track> tracklist, Track newseed, int nMaxOverlap) {
//        if(diag!=null) diag.fireMergeStartDiagnostics(newseedlist);

        //  Assume the new seed is better than all duplicates
        boolean best = true;

        //  Create a list of duplicates that are inferior
        List<Track> duplist = new ArrayList<Track>();

        //  Loop over all existing seeds
        for (Track seed : tracklist) {
            //first, check if they are identical...don't do anything if they are
            if (seed.equals(newseed))
                continue;

            //  See if the new seed is considered a duplicate of the current seed        
            boolean dupe = isDuplicate(newseed, seed, nMaxOverlap);

            if (dupe) {

                //  Check if the new seed is better than the existing seed
                boolean better = isBetter(newseed, seed);

                if (better)

                    //  If the new seed is better, add the existing seed to the list for deletion
                    duplist.add(seed);
                else {

                    //  If the new seed is inferior to an existing seed, leave everything unchanged
                    best = false;
                    break;
                }
            }
        }
     

        return best;
    }

    public static boolean isDuplicate(Track seed1, Track seed2,int nMaxOverlap) {
        int nduplicate = 0;
        for (TrackerHit hit1 : seed1.getTrackerHits())
            for (TrackerHit hit2 : seed2.getTrackerHits())
                if (hit1 == hit2) {
                    nduplicate++;
                    if (nduplicate > nMaxOverlap)
                        return true;
                }
        return false;
    }

    public static boolean isBetter(Track newseed, Track oldseed) {

        int hitdif = newseed.getTrackerHits().size() - oldseed.getTrackerHits().size();
        double chisqdif = newseed.getChi2() - oldseed.getChi2();
        if (hitdif > 1)
            return true;
        if (hitdif == 1) {
            return true;//for now just keep the longer tracks...
//            return chisqdif < strategy.getBadHitChisq();
        }
        if (hitdif == 0)
            return chisqdif < 0.;
        if (hitdif == -1) {
            return false;
            //           return chisqdif < -strategy.getBadHitChisq();
        }
        return false;
    }
}
