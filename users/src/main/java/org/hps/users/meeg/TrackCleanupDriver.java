package org.hps.users.meeg;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.util.Driver;

/**
 * Remove final state particles with bad track-cluster time matching, and
 * vertices with shared hits.
 */
public class TrackCleanupDriver extends Driver {

    private final String trackColName = "MatchedTracks";
//    private final String finalStateParticlesColName = "FinalStateParticles";
//    private final String[] v0ColNames = {"UnconstrainedV0Candidates", "BeamspotConstrainedV0Candidates", "TargetConstrainedV0Candidates"};

//    private double fsDeltaT = 43.5;
//    private double fsDeltaTCut = -1;
//    private double maxTrackDt = -1;
//    private boolean discardUnmatchedTracks = false;
//
//    public void setDiscardUnmatchedTracks(boolean discardUnmatchedTracks) {
//        this.discardUnmatchedTracks = discardUnmatchedTracks;
//    }
//
//    /**
//     * Center value for (cluster - track) time cut.
//     *
//     * @param fsDeltaT
//     */
//    public void setFsDeltaT(double fsDeltaT) {
//        this.fsDeltaT = fsDeltaT;
//    }
//
//    /**
//     * Cut window half-width for (cluster - track) time cut. Negative value
//     * disables this cut.
//     *
//     * @param fsDeltaTCut
//     */
//    public void setFsDeltaTCut(double fsDeltaTCut) {
//        this.fsDeltaTCut = fsDeltaTCut;
//    }
//
//    /**
//     * Cut window half-width for (track - track) time cut. Negative value
//     * disables this cut.
//     *
//     * @param maxTrackDt
//     */
//    public void setMaxTrackDt(double maxTrackDt) {
//        this.maxTrackDt = maxTrackDt;
//    }

    @Override
    public void process(EventHeader event) {
        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);

        List<Track> tracks = event.get(Track.class, trackColName);
        
        {
            Iterator<Track> iter = tracks.iterator();
            trackLoop:
            while (iter.hasNext()) {
                Track track = iter.next();
                for (TrackerHit hit : track.getTrackerHits()) {
                    Set<TrackerHit> htsList = hitToStrips.allFrom(hitToRotated.from(hit));
                    for (TrackerHit strip : htsList) {
//                        System.out.println(hittostrip.allTo(strip).size());
                        Set<HelicalTrackHit> sharedCrosses = hitToStrips.allTo(strip);
                        if (sharedCrosses.size() > 1) {
//                            this.getLogger().warning(String.format("removing track with possible ghost hit"));
                            iter.remove();
                            continue trackLoop;
                        }
                    }
                }
            }
        }
        
//        for (Iterator<ReconstructedParticle> iter = event.get(ReconstructedParticle.class, finalStateParticlesColName).listIterator(); iter.hasNext();) {
//            ReconstructedParticle fs = iter.next();
//            if (discardUnmatchedTracks && fs.getClusters().isEmpty()) {//track without cluster, discard
//                iter.remove();
//                continue;
//            }
//
//            if (fs.getTracks().isEmpty()) {//cluster without track (photon), keep
//                continue;
//            }
//
//            if (!fs.getClusters().isEmpty() && !fs.getTracks().isEmpty()) {
//                double deltaT = ClusterUtilities.getSeedHitTime(fs.getClusters().get(0)) - TrackUtils.getTrackTime(fs.getTracks().get(0), hitToStrips, hitToRotated);
//                if (fsDeltaTCut > 0 && Math.abs(deltaT - fsDeltaT) > fsDeltaTCut) {//bad track-cluster time match, discard
//                    iter.remove();
//                }
//            }
//        }
//
//        Set<ReconstructedParticle> fsParticles = new HashSet<ReconstructedParticle>(event.get(ReconstructedParticle.class, finalStateParticlesColName));
//
//        for (String colName : v0ColNames) {
//            v0Loop:
//            for (Iterator<ReconstructedParticle> iter = event.get(ReconstructedParticle.class, colName).listIterator(); iter.hasNext();) {
//                ReconstructedParticle v0 = iter.next();
//
//                ReconstructedParticle[] particles = new ReconstructedParticle[2];
//                for (ReconstructedParticle particle : v0.getParticles()) //                tracks.addAll(particle.getTracks());  //add add electron first, then positron...down below
//                {
//                    if (particle.getCharge() < 0) {
//                        particles[0] = particle;
//                    } else if (particle.getCharge() > 0) {
//                        particles[1] = particle;
//                    } else {
//                        throw new RuntimeException("expected only electron and positron in vertex, got something with charge 0");
//                    }
//                }
//                if (particles[0] == null || particles[1] == null) {
//                    throw new RuntimeException("vertex needs e+ and e- but is missing one or both");
//                }
//                double deltaT = TrackUtils.getTrackTime(particles[0].getTracks().get(0), hitToStrips, hitToRotated) - TrackUtils.getTrackTime(particles[1].getTracks().get(0), hitToStrips, hitToRotated); //electron time - positron time
//
//                if (hasSharedStrips(v0, hitToStrips, hitToRotated)) {
//                    iter.remove();
//                    continue;
//                }
//                if (maxTrackDt > 0 && Math.abs(deltaT) > maxTrackDt) {
//                    iter.remove();
//                    continue;
//                }
//                for (ReconstructedParticle particle : v0.getParticles()) {
//                    if (!fsParticles.contains(particle)) {
//                        iter.remove();
//                        continue v0Loop;
//                    }
//                }
//            }
//        }
    }

//    private static boolean hasSharedStrips(ReconstructedParticle vertex, RelationalTable hittostrip, RelationalTable hittorotated) {
//        return hasSharedStrips(vertex.getParticles().get(0), vertex.getParticles().get(1), hittostrip, hittorotated);
//    }
//
//    private static boolean hasSharedStrips(ReconstructedParticle fs1, ReconstructedParticle fs2, RelationalTable hittostrip, RelationalTable hittorotated) {
//        return TrackUtils.hasSharedStrips(fs1.getTracks().get(0), fs2.getTracks().get(0), hittostrip, hittorotated);
//    }
}
