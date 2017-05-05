package org.hps.recon.tracking;

import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.recon.tracking.seedtracker.TrackCheck;

public class HitTimeTrackCheck implements TrackCheck {

    private final double rmsTimeCut;
//    private final int minTrackHits = 10;
    private final int minTrackHits = 6;
    private int seedsChecked = 0;
    private int seedsPassed = 0;
    private int tracksChecked = 0;
    private int tracksPassed = 0;
    private boolean debug = false;

    public HitTimeTrackCheck(double rmsTimeCut) {
        this.rmsTimeCut = rmsTimeCut;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Override
    public boolean checkSeed(SeedCandidate candidate) {
        if (debug) {
            System.out.format("%s: seed with %d hits\n", this.getClass().getSimpleName(), candidate.getHits().size());
        }
        int nStrips = 0;
        double meanTime = 0;
        for (HelicalTrackHit hth : candidate.getHits()) {
            for (HelicalTrackStrip hts : ((HelicalTrackCross) hth).getStrips()) {
                nStrips++;
                meanTime += hts.time();
            }
        }
        meanTime /= nStrips;
        double rmsTime = 0;
        for (HelicalTrackHit hth : candidate.getHits()) {
            for (HelicalTrackStrip hts : ((HelicalTrackCross) hth).getStrips()) {
                rmsTime += Math.pow(hts.time() - meanTime, 2);
//                rmsTime += hts.time() * hts.time();
//                rmsTime += Math.abs(hts.time());
            }
        }
//        if (nStrips<6) return true;
        seedsChecked++;
        if (debug) {
            System.out.format("%s: seed RMS %f on %d hits\n", this.getClass().getSimpleName(), Math.sqrt(rmsTime / nStrips), nStrips);
        }
        boolean passCheck = (rmsTime < minTrackHits * rmsTimeCut * rmsTimeCut);
//        boolean passCheck = (rmsTime < minTrackHits * rmsTimeCut);
        if (passCheck) {
            seedsPassed++;
        }
        if (debug && seedsChecked % 10000 == 0) {
            System.out.format("%s: Checked %d seeds, %d passed (%d failed)\n", this.getClass().getSimpleName(), seedsChecked, seedsPassed, seedsChecked - seedsPassed);
        }
        return passCheck;

    }

    @Override
    public boolean checkTrack(SeedTrack track) {
        if (debug) {
            System.out.format("%s: track with %d hits\n", this.getClass().getSimpleName(), track.getTrackerHits().size());
        }
        tracksChecked++;
        int nStrips = 0;
        double meanTime = 0;
        for (TrackerHit hit : track.getTrackerHits()) {
            for (HelicalTrackStrip hts : ((HelicalTrackCross) hit).getStrips()) {
                nStrips++;
                meanTime += hts.time();
            }
        }
        meanTime /= nStrips;
        double rmsTime = 0;
        for (TrackerHit hit : track.getTrackerHits()) {
            for (HelicalTrackStrip hts : ((HelicalTrackCross) hit).getStrips()) {
                rmsTime += Math.pow(hts.time() - meanTime, 2);
//                rmsTime += hts.time() * hts.time();
//                rmsTime += Math.abs(hts.time());
            }
        }
        rmsTime = Math.sqrt(rmsTime / nStrips);
//        rmsTime = rmsTime / nStrips;
        if (debug) {
            System.out.format("%s: track RMS %f on %d hits\n", this.getClass().getSimpleName(), rmsTime, nStrips);
        }
        boolean passCheck = (rmsTime < rmsTimeCut);
        if (passCheck) {
            tracksPassed++;
        }
        if (debug && tracksChecked % 100 == 0) {
            System.out.format("%s: Checked %d tracks, %d passed (%d failed)\n", this.getClass().getSimpleName(), tracksChecked, tracksPassed, tracksChecked - tracksPassed);
        }
        return passCheck;
    }
}
