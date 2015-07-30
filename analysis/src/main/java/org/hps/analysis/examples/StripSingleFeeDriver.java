package org.hps.analysis.examples;

import java.util.List;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.util.Driver;

/**
 * Simple Driver to select events containing only one Full Energy Electron
 * (FEE). This is defined as one ReconstructedParticle which has been identified
 * as an electron and has an energy equal to or greater than the energyCut
 * (default is 0.85) and a track which has greater than or equal the
 * numberOfHitsOnTrack (default is 6).
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class StripSingleFeeDriver extends Driver
{

    private double _energyCut = 0.85;
    private int _nHitsOnTrack = 6;

    /**
     *
     * @param event
     */
    @Override
    protected void process(EventHeader event)
    {
        boolean skipEvent = true;
        List<ReconstructedParticle> rps = event.get(ReconstructedParticle.class, "FinalStateParticles");
        if (rps.size() == 1) {
            ReconstructedParticle rp = rps.get(0);
            double energy = rp.getEnergy();
            int pdgId = rp.getParticleIDUsed().getPDG();
            if (pdgId == 11) {
                if (energy >= _energyCut && rp.getTracks().get(0).getTrackerHits().size() >= _nHitsOnTrack) // electron
                {
                    skipEvent = false;
                }
            }
        }
    }

    /**
     * Electrons having energy below the cut will be rejected.
     *
     * @param cut
     */
    public void setEnergyCut(double cut)
    {
        _energyCut = cut;
    }

    /**
     * Tracks having fewer than the number of hits will be rejected.
     *
     * @param cut
     */
    public void setNumberOfHitsOnTrack(int cut)
    {
        _nHitsOnTrack = cut;
    }

}
