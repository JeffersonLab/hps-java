package org.hps.users.meeg;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hps.recon.filtering.EventReconFilter;
import static org.hps.users.meeg.KinkAnalysisDriver.makeTrackHitMap;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimTrackerHit;

/*
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: KinkAnalysisDriver.java,v 1.3 2013/10/24 18:11:43 meeg Exp $
 */
public class TridentMCFilter extends EventReconFilter {

    @Override
    public void process(EventHeader event) {
        List<MCParticle> MCParticles = event.getMCParticles();

        List<MCParticle> tridentParticles = null;

        for (MCParticle particle : MCParticles) {
            if (particle.getPDGID() == 622) {
                tridentParticles = particle.getDaughters();
            }
        }

        if (tridentParticles == null) {
            skipEvent();
        }

        List<SimTrackerHit> trackerHits = event.get(SimTrackerHit.class, "TrackerHits");

        Map<MCParticle, Map<Integer, SimTrackerHit>> trackHitMap = makeTrackHitMap(trackerHits, null);

        int nElectronsWithTracks = 0, nPositronsWithTracks = 0;
        MCParticle electron = null, positron = null;

        for (MCParticle particle : tridentParticles) {
            Set<Integer> layers = trackHitMap.get(particle).keySet();
            int pairCount = 0;
            for (Integer layer : layers) {
                if (layer % 2 == 0 && layers.contains(layer - 1)) {
                    pairCount++;
                }
            }
            boolean hasTrack = (pairCount >= 5);

            if (hasTrack && particle.getCharge() < 0) {
                nElectronsWithTracks++;
                electron = particle;
            }
            if (hasTrack && particle.getCharge() > 0) {
                nPositronsWithTracks++;
                positron = particle;
            }
        }

        if (electron == null || positron == null) {
            System.out.println("not enough trident daughters with tracks");
            skipEvent();
        }

        if (nElectronsWithTracks > 1 || nPositronsWithTracks > 1) {
            System.out.println("too many trident daughters with tracks");
            skipEvent();
        }

//        double deflection12_ele = KinkAnalysisDriver.deflection(trackHitMap.get(electron), 0, 4);
//        double deflection12_pos = KinkAnalysisDriver.deflection(trackHitMap.get(positron), 0, 4);
        incrementEventPassed();

    }
}
