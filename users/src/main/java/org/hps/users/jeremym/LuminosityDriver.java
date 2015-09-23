  package org.hps.users.jeremym;

import org.hps.record.epics.EpicsData;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

/**
 * Calculate integrated luminosity using EPICS data.
 * <p>
 * This does not calculate luminosity for time period before there is any EPICS data processed in the job, and the time
 * period between the last EPICS event and the end of the job is also ignored.
 * <p>
 * This <code>Driver</code> would be easiest to use as a child of an analysis <code>Driver</code> so that the calculated
 * luminosity can be easily retrieved in the parent's {@link org.lcsim.util.Driver#endOfData()} method.
 *
 * @author Jeremy McCormick, SLAC
 * @see LuminosityAnalysisDriver
 */
public class LuminosityDriver extends Driver {

    /**
     * EPICS variable to use for current measurement.
     */
    private static final String EPICS_VARIABLE = "scaler_calc1";

    /**
     * Conversion from scaler counts to charge.
     */
    private final double SCALER_COUNTS_TO_CHARGE = 905.937;
    
    /**
     * Calculate the luminosity in coulomb given a list of EPICS data.
     *
     * @param epicsData the list of EPICS data
     * @return the calculated luminosity
     */
    private double calculateLuminosity() {      
        double scalerCount = lastEpicsData.getValue(EPICS_VARIABLE) -firstEpicsData.getValue(EPICS_VARIABLE);
        return SCALER_COUNTS_TO_CHARGE * scalerCount;
    }

    /**
     * First EPICS data bank.
     */
    private EpicsData firstEpicsData = null;

    /**
     * Last EPICS data bank.
     */
    private EpicsData lastEpicsData = null;
    
    /**
     * Current bank used for getting the last bank in {@link #endOfData()}.
     */
    private EpicsData currentEpicsData = null;

    /**
     * The final measurement of integrated
     */
    private double luminosity;

    /**
     * End of data hook which performs luminosity calculation.
     */
    @Override
    public void endOfData() {
        lastEpicsData = currentEpicsData;
        luminosity = calculateLuminosity();
    }

    /**
     * Get the calculated luminosity for the job.
     *
     * @return the calculated luminosity for the job
     */
    public double getLuminosity() {
        return luminosity;
    }

    /**
     * Process an event.
     *
     * @param eventHeader the event header
     */
    @Override
    public void process(final EventHeader eventHeader) {
        final EpicsData epicsData = EpicsData.read(eventHeader);
        if (epicsData != null) {
            if (epicsData.hasKey(EPICS_VARIABLE)) {
                System.out.println("adding EPICS data with timestamp " + epicsData.getEpicsHeader().getTimestamp()
                        + " and Faraday cup current " + epicsData.getValue(EPICS_VARIABLE));
                if (firstEpicsData == null) {
                    firstEpicsData = epicsData;
                }
                currentEpicsData = epicsData;
            }
        }
    }
}
