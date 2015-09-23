package org.hps.users.jeremym;

import java.util.ArrayList;
import java.util.List;

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
     * Calculate the luminosity in coulomb given a list of EPICS data.
     *
     * @param epicsData the list of EPICS data
     * @return the calculated luminosity
     */
    private static double calculateLuminosity(final List<EpicsData> epicsDataList) {
        if (epicsDataList.isEmpty()) {
            throw new RuntimeException("The EPICS data list is empty.");
        }
        double integratedLuminosity = 0;
        for (int i = 0; i < epicsDataList.size(); i++) {
            if (i != epicsDataList.size() - 1) {

                // Get a pair of EPICS records.
                final EpicsData epicsDataStart = epicsDataList.get(i);
                final EpicsData epicsDataEnd = epicsDataList.get(i + 1);

                // Calculate elapsed time between the EPICS events.
                int timeLength = epicsDataEnd.getEpicsHeader().getTimestamp()
                        - epicsDataStart.getEpicsHeader().getTimestamp();

                if (timeLength == 0) {
                    // Force at least 1 second time resolution.
                    timeLength = 1;
                }

                // Get average current over the time period.
                final double averageCurrent = (epicsDataStart.getValue(EPICS_VARIABLE) + epicsDataEnd
                        .getValue(EPICS_VARIABLE)) / 2.;

                // Add the current for the time period to the integrated luminosity total.
                integratedLuminosity += timeLength * averageCurrent;
            }
        }
        // Convert from nano coulomb to coulomb.
        integratedLuminosity *= 10e-9;
        return integratedLuminosity;
    }

    /**
     * The list of EPICS data accumulated during the job.
     */
    private final List<EpicsData> epicsDataList = new ArrayList<EpicsData>();

    /**
     * The final measurement of integrated
     */
    private double luminosity;

    /**
     * End of data hook which performs luminosity calculation.
     */
    @Override
    public void endOfData() {
        if (epicsDataList.isEmpty()) {
            throw new RuntimeException("The EPICS data list is empty.");
        }
        luminosity = calculateLuminosity(this.epicsDataList);
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
                epicsDataList.add(epicsData);
            }
        }
    }
}
