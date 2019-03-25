package org.hps.readout.ecal.updated;

import org.hps.readout.SLICDataReadoutDriver;
import org.lcsim.event.SimTrackerHit;

/**
 * <code>SimTrackerHitReadoutDriver</code> handles SLIC objects in
 * input Monte Carlo files of type {@link
 * org.lcsim.event.SimTrackerHit SimTrackerHit}.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @see org.hps.readout.SLICDataReadoutDriver
 */
public class SimTrackerHitReadoutDriver extends SLICDataReadoutDriver<SimTrackerHit> {
    /**
     * Instantiate an instance of {@link
     * org.hps.readout.SLICDataReadoutDriver SLICDataReadoutDriver}
     * for objects of type {@link
     * org.lcsim.event.SimTrackerHit SimTrackerHit
     * SimCalorimeterHit} and set the appropriate LCIO flags.
     */
    public SimTrackerHitReadoutDriver() {
        super(SimTrackerHit.class, 0xc0000000);
    }
}