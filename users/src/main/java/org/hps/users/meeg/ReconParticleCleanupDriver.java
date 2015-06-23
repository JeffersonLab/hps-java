package org.hps.users.meeg;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.RelationalTable;
import org.lcsim.util.Driver;

/**
 * Remove final state particles with bad track-cluster time matching, and
 * vertices with shared tracks.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: $
 */
public class ReconParticleCleanupDriver extends Driver {

    private final String finalStateParticlesColName = "FinalStateParticles";
    private final String unconstrainedV0CandidatesColName = "UnconstrainedV0Candidates";
    private final String beamConV0CandidatesColName = "BeamspotConstrainedV0Candidates";
    private final String targetV0ConCandidatesColName = "TargetConstrainedV0Candidates";
    private double fsDeltaT = 43.5;
    private double fsDeltaTCut = 5.0;

    /**
     * Center value for (cluster - track) time cut.
     *
     * @param fsDeltaT
     */
    public void setFsDeltaT(double fsDeltaT) {
        this.fsDeltaT = fsDeltaT;
    }

    /**
     * Cut window half-width for (cluster - track) time cut.
     *
     * @param fsDeltaTCut
     */
    public void setFsDeltaTCut(double fsDeltaTCut) {
        this.fsDeltaTCut = fsDeltaTCut;
    }

    @Override
    public void process(EventHeader event) {
        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);

        for (Iterator<ReconstructedParticle> iter = event.get(ReconstructedParticle.class, finalStateParticlesColName).listIterator(); iter.hasNext();) {
            ReconstructedParticle fs = iter.next();
            if (fs.getClusters().isEmpty()) {//track without cluster, discard
                iter.remove();
                continue;
            }

            if (fs.getTracks().isEmpty()) {//cluster without track (photon), keep
                continue;
            }

            double deltaT = ClusterUtilities.getSeedHitTime(fs.getClusters().get(0)) - TrackUtils.getTrackTime(fs.getTracks().get(0), hitToStrips, hitToRotated);
            if (Math.abs(deltaT - fsDeltaT) > fsDeltaTCut) {//bad track-cluster time match, discard
                iter.remove();
            }
        }

        Set<ReconstructedParticle> fsParticles = new HashSet<ReconstructedParticle>(event.get(ReconstructedParticle.class, finalStateParticlesColName));

        v0Loop:
        for (Iterator<ReconstructedParticle> iter = event.get(ReconstructedParticle.class, unconstrainedV0CandidatesColName).listIterator(); iter.hasNext();) {
            ReconstructedParticle v0 = iter.next();
            if (hasSharedStrips(v0, hitToStrips, hitToRotated)) {
                iter.remove();
                continue;
            }
            for (ReconstructedParticle particle : v0.getParticles()) {
                if (!fsParticles.contains(particle)) {
                    iter.remove();
                    continue v0Loop;
                }
            }
        }

        v0Loop:
        for (Iterator<ReconstructedParticle> iter = event.get(ReconstructedParticle.class, beamConV0CandidatesColName).listIterator(); iter.hasNext();) {
            ReconstructedParticle v0 = iter.next();
            if (hasSharedStrips(v0, hitToStrips, hitToRotated)) {
                iter.remove();
                continue;
            }
            for (ReconstructedParticle particle : v0.getParticles()) {
                if (!fsParticles.contains(particle)) {
                    iter.remove();
                    continue v0Loop;
                }
            }
        }

        v0Loop:
        for (Iterator<ReconstructedParticle> iter = event.get(ReconstructedParticle.class, targetV0ConCandidatesColName).listIterator(); iter.hasNext();) {
            ReconstructedParticle v0 = iter.next();
            if (hasSharedStrips(v0, hitToStrips, hitToRotated)) {
                iter.remove();
                continue;
            }
            for (ReconstructedParticle particle : v0.getParticles()) {
                if (!fsParticles.contains(particle)) {
                    iter.remove();
                    continue v0Loop;
                }
            }
        }
    }

    private static boolean hasSharedStrips(ReconstructedParticle vertex, RelationalTable hittostrip, RelationalTable hittorotated) {
        return hasSharedStrips(vertex.getParticles().get(0), vertex.getParticles().get(1), hittostrip, hittorotated);
    }

    private static boolean hasSharedStrips(ReconstructedParticle fs1, ReconstructedParticle fs2, RelationalTable hittostrip, RelationalTable hittorotated) {
        return TrackUtils.hasSharedStrips(fs1.getTracks().get(0), fs2.getTracks().get(0), hittostrip, hittorotated);
    }

}
