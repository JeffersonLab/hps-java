package org.hps.users.jeremym;

import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

/**
 * Parent <code>Driver</code> showing how to use the {@link LuminosityDriver}.
 *
 * @author Jeremy McCormick, SLAC
 */
public class LuminosityAnalysisDriver extends Driver {

    /**
     * Reference to the <code>Driver</code> which will perform the calculation.
     */
    private final LuminosityDriver lumiDriver;

    /**
     * Class constructor.
     * <p>
     * Adds an instance of this <code>Driver</code>.
     */
    public LuminosityAnalysisDriver() {
        lumiDriver = new LuminosityDriver();
        this.add(lumiDriver);
    }

    /**
     * End of job hook.
     */
    @Override
    public void endOfData() {
        super.endOfData();
        System.out.println("luminosity for job was " + lumiDriver.getLuminosity() + "C");
    }

    /**
     * Event processing hook.
     *
     * @param eventHeader the event to process
     */
    @Override
    public void process(final EventHeader eventHeader) {
        super.process(eventHeader);
    }
}
