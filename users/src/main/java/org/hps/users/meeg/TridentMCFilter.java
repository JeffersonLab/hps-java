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

    private boolean requireFrontHits = false;
    private double minL12Kink = -1;
    private double maxL12Kink = -1;
    private double minL1Kink = -1;
    private double maxL1Kink = -1;
    private double minL2Kink = -1;
    private double maxL2Kink = -1;

    public void setMinL12Kink(double minL12Kink) {
        this.minL12Kink = minL12Kink;
    }

    public void setMaxL12Kink(double maxL12Kink) {
        this.maxL12Kink = maxL12Kink;
    }

    public void setMinL1Kink(double minL1Kink) {
        this.minL1Kink = minL1Kink;
    }

    public void setMaxL1Kink(double maxL1Kink) {
        this.maxL1Kink = maxL1Kink;
    }

    public void setMinL2Kink(double minL2Kink) {
        this.minL2Kink = minL2Kink;
    }

    public void setMaxL2Kink(double maxL2Kink) {
        this.maxL2Kink = maxL2Kink;
    }

    public void setRequireFrontHits(boolean requireFrontHits) {
        this.requireFrontHits = requireFrontHits;
    }

    @Override
    public void process(EventHeader event) {
        incrementEventProcessed();
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

        particleLoop:
        for (MCParticle particle : tridentParticles) {
            if (!trackHitMap.containsKey(particle)) {
                continue;
            }
            Set<Integer> layers = trackHitMap.get(particle).keySet();
            int pairCount = 0;
            for (Integer layer : layers) {
                if (layer % 2 == 0 && layers.contains(layer - 1)) {
                    pairCount++;
                }
            }
            if (pairCount < 5) {
                continue;
            }
            if (requireFrontHits) {
                for (int i = 1; i < 5; i++) {
                    if (!layers.contains(i)) {
                        continue particleLoop;
                    }
                }
            }

            if (particle.getCharge() < 0) {
                nElectronsWithTracks++;
                electron = particle;
            }
            if (particle.getCharge() > 0) {
                nPositronsWithTracks++;
                positron = particle;
            }
        }

        if (electron == null || positron == null) {
//            System.out.println("not enough trident daughters with tracks");
            skipEvent();
        }

        if (nElectronsWithTracks > 1 || nPositronsWithTracks > 1) {
//            System.out.println("too many trident daughters with tracks");
            skipEvent();
        }

        double deflection12_ele = KinkAnalysisDriver.deflection(trackHitMap.get(electron), 0, 4);
        double deflection12_pos = KinkAnalysisDriver.deflection(trackHitMap.get(positron), 0, 4);
        double deflection1_ele = KinkAnalysisDriver.deflection(trackHitMap.get(electron), 0, 2);
        double deflection1_pos = KinkAnalysisDriver.deflection(trackHitMap.get(positron), 0, 2);
        double deflection2_ele = KinkAnalysisDriver.deflection(trackHitMap.get(electron), 2, 4);
        double deflection2_pos = KinkAnalysisDriver.deflection(trackHitMap.get(positron), 2, 4);
        if (minL12Kink > 0) {
            if (Math.abs(deflection12_ele) < minL12Kink && Math.abs(deflection12_pos) < minL12Kink) {
                skipEvent();
            }
        }
        if (maxL12Kink > 0) {
            if (Math.abs(deflection12_ele) > maxL12Kink || Math.abs(deflection12_pos) > maxL12Kink) {
                skipEvent();
            }
        }
        if (minL1Kink > 0) {
            if (Math.abs(deflection1_ele) < minL1Kink && Math.abs(deflection1_pos) < minL1Kink) {
                skipEvent();
            }
        }
        if (maxL1Kink > 0) {
            if (Math.abs(deflection1_ele) > maxL1Kink || Math.abs(deflection1_pos) > maxL1Kink) {
                skipEvent();
            }
        }
        if (minL2Kink > 0) {
            if (Math.abs(deflection2_ele) < minL2Kink && Math.abs(deflection2_pos) < minL2Kink) {
                skipEvent();
            }
        }
        if (maxL2Kink > 0) {
            if (Math.abs(deflection2_ele) > maxL2Kink || Math.abs(deflection2_pos) > maxL2Kink) {
                skipEvent();
            }
        }

        incrementEventPassed();
    }
}
