package org.hps.evio;

import java.util.List;

import org.hps.readout.svt.SvtHeaderDataInfo;
import org.hps.util.Pair;
import org.jlab.coda.jevio.BaseStructure;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.HpsTestRunSiSensor;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Subdetector;

/**
 *  Test run SVT EVIO reader used to convert SVT bank integer data to LCIO
 *  objects.
 * 
 *  @author Omar Moreno <omoreno1@ucsc.edu>
 *  @date November 20, 2014
 *
 */
public class TestRunSvtEvioReader extends AbstractSvtEvioReader {

    //-----------------//
    //--- Constants ---//
    //-----------------//

    private static final int DATA_HEADER_LENGTH = 7;
    private static final int DATA_TAIL_LENGTH = 1; 
    private static final int MAX_FPGA_ID = 6;
    private static final int ROC_BANK_TAG = 3;
    private static final int ROC_BANK_NUMBER = -1; 
    
    /** 
     * Default Constructor
     */
    public TestRunSvtEvioReader() { }; 
    
    /**
     *  Get the minimum SVT ROC bank tag in the event.
     *
     *  @return Minimum SVT ROC bank tag
     */
    @Override
    protected int getMinRocBankTag() { 
        return ROC_BANK_TAG; 
    }

    /**
     *  Get the maximum SVT ROC bank tag in the event.
     *
     *  @return Maximum SVT ROC bank tag
     */
    @Override 
    protected int getMaxRocBankTag() { 
        return ROC_BANK_TAG; 
    }
    
    /**
     *  Get the SVT ROC bank number of the bank encapsulating the SVT samples.
     * 
     *  @return SVT ROC bank number 
     */
    @Override
    protected int getRocBankNumber() { 
        return ROC_BANK_NUMBER; 
    }
    
    /**
     *  Get the number of 32 bit integers composing the data block header. For
     *  the test run, the header consisted of 7 32 bit integers.
     *
     *  @return The header length. 
     */
    @Override
    protected int getDataHeaderLength() {
        return DATA_HEADER_LENGTH;
    }
    
    /**
     *  Get the number of 32 bit integers composing the data block tail.  For
     *  the test run, the tail consisted of a single 32 bit integer.
     * 
     *  @return The tail length
     */
    @Override
    protected int getDataTailLength() {
        return DATA_TAIL_LENGTH;
    }

    /**
     *  A method to setup a mapping between a DAQ pair (FPGA/Hybrid) and the 
     *  corresponding sensor.
     *
     *  @param subdetector : The tracker {@link Subdetector} object
     */
    // TODO: This can probably be done when the conditions are loaded.
    @Override
    protected void setupDaqMap(Subdetector subdetector) { 
    
        List<HpsTestRunSiSensor> sensors 
            = subdetector.getDetectorElement().findDescendants(HpsTestRunSiSensor.class);
        for (HpsTestRunSiSensor sensor : sensors) { 
            Pair<Integer, Integer> daqPair 
                = new Pair<Integer, Integer>(sensor.getFpgaID(), sensor.getHybridID());
            daqPairToSensor.put(daqPair, sensor);
        }
        this.isDaqMapSetup = true;
    }

    /**
     *  Get the sensor associated with a set of samples.  The sample block of
     *  data is used to extract the FPGA ID and Hybrid ID corresponding to 
     *  the samples. 
     *
     *  @param data : sample block of data
     *  @return The sensor associated with a set of sample 
     */
    @Override
    protected HpsSiSensor getSensor(int[] data) {
        
        /*this.printDebug("FEB ID: " + SvtEvioUtils.getFpgaID(data) 
                + " Hybrid ID: " + SvtEvioUtils.getHybridID(data));*/

        Pair<Integer, Integer> daqPair 
            = new Pair<Integer, Integer>(SvtEvioUtils.getFpgaID(data),
                                         SvtEvioUtils.getHybridID(data));
        return daqPairToSensor.get(daqPair);
    }

    /**
     *  Check whether a data bank is valid i.e. contains SVT samples only.  For
     *  the test run, a valid data bank has a tag in the range 0-6.
     * 
     *  @param dataBank - An EVIO bank containing integer data
     *  @return true if the bank is valid, false otherwise
     * 
     */
    @Override
    protected boolean isValidDataBank(BaseStructure dataBank) { 
        if (dataBank.getHeader().getTag() < 0 
                || dataBank.getHeader().getTag() >= MAX_FPGA_ID) return false; 
        return true; 
    }
    
    /**
     * Check whether the samples are valid.
     * 
     * @param data : sample block of data
     * @return true if the samples are valid, false otherwise
     */
    protected boolean isValidSampleSet(int[] data) { 
        return true;        
    }
    
    /**
     *  Make a {@link RawTrackerHit} from a set of samples.
     * 
     *  @param data : sample block of data
     *  @return A raw hit
     */
    @Override
    protected RawTrackerHit makeHit(int[] data) {
        //this.printDebug("Channel: " + SvtEvioUtils.getTestRunChannelNumber(data));
        return makeHit(data, SvtEvioUtils.getTestRunChannelNumber(data));
    }

    @Override
    protected SvtHeaderDataInfo extractSvtHeader(int num, int[] data) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void checkSvtHeaderData(SvtHeaderDataInfo header)
            throws SvtEvioHeaderException {
        // TODO Auto-generated method stub
        
    }

 

    
}
