package org.hps.analysis.wab;

import java.util.List;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.MCParticle.SimulatorStatus;
import org.lcsim.util.Driver;

/**
 * Driver to select events in which the bremsstrahlung photon has converted in
 * the tracker. Input files should be WAB events, i.e. events with just the
 * incoming electron as particle 0 and the photon as particle 1
 */
public class StripMcWabConversionDriver extends Driver
{

    private int _numberOfEventsWritten = 0;
    private int _numberOfEventsRead = 0;

    protected void process(EventHeader event)
    {
        _numberOfEventsRead++;
        boolean skipEvent = true;
        List<MCParticle> mclist = event.getMCParticles();
        //
        // HPS Wide angle bremsstrahlung events list the electron first, followed by the bremsstrahlung photon.
        // If that changes, so will the next line.
        // TODO add some selection code here to recognize MC WAB events in mixed files.
        // assert that particle 0 is electron, particle 1 is photon
        // assert that sum of particle 0 and 1 has the beam energy and is directed along the beam direction
        //
        MCParticle brem = mclist.get(1);
        SimulatorStatus stat = brem.getSimulatorStatus();
        // select events in which the photon converted in the tracker
        if (stat.isDecayedInTracker()) {
            skipEvent = false;
        }
        if (skipEvent) {
            throw new Driver.NextEventException();
        } else {
            _numberOfEventsWritten++;
        }
    }

    protected void endOfData()
    {
        System.out.println("Wrote " + _numberOfEventsWritten + " events of " + _numberOfEventsRead + " read.");
    }
}
