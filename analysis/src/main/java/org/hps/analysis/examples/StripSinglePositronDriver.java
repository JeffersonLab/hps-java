package org.hps.analysis.examples;

import java.util.List;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.util.Driver;

/**
 * Simple Driver to select events containing a single positron This is defined
 * as one ReconstructedParticle which has been identified as a positron with a
 * track which has greater than or equal the numberOfHitsOnTrack (default is 6).
 * By default, only one ReconstructedParticle is allowed per event.
 *
 * @author Norman A Graf
 *
 * TODO Move to recon.filtering
 */
public class StripSinglePositronDriver extends Driver {

    private boolean _writeRunAndEventNumbers = false;
    private double _energyCut = 0.;
    private int _nHitsOnTrack = 6;
    private int _nReconstructedParticles = 1;

    private int _numberOfEventsWritten = 0;

    /**
     *
     * @param event
     */
    @Override
    protected void process(EventHeader event) {
        boolean skipEvent = true;
        List<ReconstructedParticle> rps = event.get(ReconstructedParticle.class, "FinalStateParticles");
        if (rps.size() <= _nReconstructedParticles) {
            for (ReconstructedParticle rp : rps) {
                double energy = rp.getEnergy();
                int pdgId = rp.getParticleIDUsed().getPDG();
                if (pdgId == -11) {
                    if (energy >= _energyCut && rp.getTracks().get(0).getTrackerHits().size() >= _nHitsOnTrack) // positron
                    {
                        skipEvent = false;
                    }
                }
            }
        }
        if (skipEvent) {
            throw new Driver.NextEventException();
        } else {
            if (_writeRunAndEventNumbers) {
                System.out.println(event.getRunNumber() + " " + event.getEventNumber());
            }
            _numberOfEventsWritten++;
        }
    }

    /**
     * Positrons having energy below the cut will be rejected.
     *
     * @param cut
     */
    public void setEnergyCut(double cut) {
        _energyCut = cut;
    }

    /**
     * Tracks having fewer than the number of hits will be rejected.
     *
     * @param cut
     */
    public void setNumberOfHitsOnTrack(int cut) {
        _nHitsOnTrack = cut;
    }

    /**
     * Events having more than the number of ReconstructedParticles will be
     * rejected.
     *
     * @param cut
     */
    public void setNumberOfReconstructedParticles(int cut) {
        _nReconstructedParticles = cut;
    }

    /**
     * Write out run and event numbers of events passing the cuts if desired
     *
     * @param b
     */
    public void setWriteRunAndEventNumbers(boolean b) {
        _writeRunAndEventNumbers = b;
    }

    @Override
    protected void endOfData() {
        System.out.println("Wrote " + _numberOfEventsWritten + " events");
    }
}
