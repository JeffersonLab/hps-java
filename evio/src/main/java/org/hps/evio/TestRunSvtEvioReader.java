package org.hps.evio;

import java.util.List;

import org.hps.record.svt.SvtEvioExceptions.SvtEvioHeaderException;
import org.hps.record.svt.SvtEvioUtils;
import org.hps.record.svt.SvtHeaderDataInfo;
import org.hps.util.Pair;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.HpsTestRunSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Subdetector;

/**
 * Test run SVT EVIO reader used to convert SVT bank integer data to LCIO
 * objects.
 * 
 * @author Omar Moreno, SLAC National Accelerator Laboratory
 */
public class TestRunSvtEvioReader extends AbstractSvtEvioReader {

    // -----------------//
    // --- Constants ---//
    // -----------------//

    private static final int DATA_HEADER_LENGTH = 7;
    private static final int DATA_TAIL_LENGTH = 1;
    private static final int MAX_FPGA_ID = 6;
    public static final int MIN_DATA_BANK_TAG = 0;
    private static final int ROC_BANK_TAG = 3;
    private static final int ROC_BANK_NUMBER = -1;

    /**
     * Default Constructor
     */
    public TestRunSvtEvioReader() {
    };

    /**
     * Get the minimum SVT ROC bank tag in the event.
     *
     * @return Minimum SVT ROC bank tag
     */
    @Override
    protected int getMinRocBankTag() {
        return ROC_BANK_TAG;
    }

    /**
     * Get the maximum SVT ROC bank tag in the event.
     *
     * @return Maximum SVT ROC bank tag
     */
    @Override
    protected int getMaxRocBankTag() {
        return ROC_BANK_TAG;
    }

    @Override
    protected int getMinDataBankTag() {
        return MIN_DATA_BANK_TAG;
    }

    @Override
    protected int getMaxDataBankTag() {
        return MAX_FPGA_ID;
    }

    /**
     * Get the SVT ROC bank number of the bank encapsulating the SVT samples.
     * 
     * @return SVT ROC bank number
     */
    @Override
    protected int getRocBankNumber() {
        return ROC_BANK_NUMBER;
    }

    /**
     * Get the number of 32 bit integers composing the data block header. For
     * the test run, the header consisted of 7 32 bit integers.
     *
     * @return The header length.
     */
    @Override
    protected int getDataHeaderLength() {
        return DATA_HEADER_LENGTH;
    }

    /**
     * Get the number of 32 bit integers composing the data block tail. For
     * the test run, the tail consisted of a single 32 bit integer.
     * 
     * @return The tail length
     */
    @Override
    protected int getDataTailLength() {
        return DATA_TAIL_LENGTH;
    }

    /**
     * A method to setup a mapping between a DAQ pair (FPGA/Hybrid) and the
     * corresponding sensor.
     *
     * @param subdetector : The tracker {@link Subdetector} object
     */
    // TODO: This can probably be done when the conditions are loaded.
    @Override
    protected void setupDaqMap(Subdetector subdetector) {

        List<HpsTestRunSiSensor> sensors = subdetector.getDetectorElement().findDescendants(HpsTestRunSiSensor.class);
        for (HpsTestRunSiSensor sensor : sensors) {
            Pair<Integer, Integer> daqPair = new Pair<Integer, Integer>(sensor.getFpgaID(), sensor.getHybridID());
            daqPairToSensor.put(daqPair, sensor);
        }
        this.isDaqMapSetup = true;
    }

    /**
     * Get the sensor associated with a set of samples. The sample block of
     * data is used to extract the FPGA ID and Hybrid ID corresponding to
     * the samples.
     *
     * @param data : sample block of data
     * @return The sensor associated with a set of sample
     */
    @Override
    protected HpsSiSensor getSensor(int[] data) {

        /*
         * this.printDebug("FEB ID: " + SvtEvioUtils.getFpgaID(data)
         * + " Hybrid ID: " + SvtEvioUtils.getHybridID(data));
         */

        Pair<Integer, Integer> daqPair = new Pair<Integer, Integer>(SvtEvioUtils.getFpgaID(data),
                SvtEvioUtils.getHybridID(data));
        return daqPairToSensor.get(daqPair);
    }

    /**
     * Check whether the samples are valid.
     * 
     * @param data : sample block of data
     * @return true if the samples are valid, false otherwise
     */
    @Override
    protected boolean isValidSampleSet(int[] data) {
        return true;
    }

    /**
     * Make a {@link RawTrackerHit} from a set of samples.
     * 
     * @param data : sample block of data
     * @return A raw hit
     */
    @Override
    protected RawTrackerHit makeHit(int[] data) {
        // this.printDebug("Channel: " + SvtEvioUtils.getTestRunChannelNumber(data));
        return makeHit(data, SvtEvioUtils.getTestRunChannelNumber(data));
    }

    @Override
    protected void processSvtHeaders(List<SvtHeaderDataInfo> headers, EventHeader lcsimEvent)
            throws SvtEvioHeaderException {
        // TODO Auto-generated method stub

    }

    protected void checkSvtSampleCount(int sampleCount, SvtHeaderDataInfo headerData) throws SvtEvioHeaderException {
        // Overridden from super class to do nothing.
    }

    @Override
    protected List<RawTrackerHit> makeHits(int bankNumber, int[] data) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected List<int[]> extractMultiSamples(int sampleCount, int[] data) {
        // TODO Auto-generated method stub
        return null;
    }
}
