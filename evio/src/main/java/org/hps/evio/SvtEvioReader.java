package org.hps.evio;

import java.util.List;

import org.hps.evio.SvtEvioExceptions.SvtEvioHeaderException;
import org.hps.evio.SvtEvioExceptions.SvtEvioReaderException;
import org.hps.record.svt.SvtHeaderDataInfo;
import org.hps.util.Pair;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Subdetector;
import org.lcsim.lcio.LCIOUtil;

/**
 *  SVT EVIO reader used to convert SVT bank integer data to LCIO objects.
 * 
 *  @author Omar Moreno <omoreno1@ucsc.edu>
 *  @data February 03, 2015
 *
 */
public class SvtEvioReader extends AbstractSvtEvioReader {

    //-----------------//
    //--- Constants ---//
    //-----------------//
    private static final int DATA_HEADER_LENGTH = 1;
    private static final int DATA_TAIL_LENGTH = 1; 
    public static final int MIN_ROC_BANK_TAG = 51;
    public static final int MAX_ROC_BANK_TAG = 66;
    private static final int ROC_BANK_NUMBER = 0; 
    
    /**
     *  Get the minimum SVT ROC bank tag in the event.
     *
     *  @return Minimum SVT ROC bank tag
     */
    @Override
    protected int getMinRocBankTag() { 
        return MIN_ROC_BANK_TAG; 
    }

    /**
     *  Get the maximum SVT ROC bank tag in the event.
     *
     *  @return Maximum SVT ROC bank tag
     */
    @Override 
    protected int getMaxRocBankTag() { 
        return MAX_ROC_BANK_TAG; 
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
     *  Get the number of 32 bit integers composing the data block header
     *
     *  @return The header length
     */
    @Override
    protected int getDataHeaderLength() {
        return DATA_HEADER_LENGTH;
    }

    /**
     *  Get the number of 32 bit integers composing the data block tail (the 
     *  data inserted after all sample blocks in a data block)
     * 
     *  @return The tail length 
     */
    @Override
    protected int getDataTailLength() {
        return DATA_TAIL_LENGTH;
    }

    /**
     *  A method to setup a mapping between a DAQ pair (FEB/FEB Hybrid) and the 
     *  corresponding sensor.
     *
     *  @param subdetector : The tracker {@link Subdetector} object
     */
    // TODO: This can probably be done when the conditions are loaded.
    @Override
    protected void setupDaqMap(Subdetector subdetector) {
    
        List<HpsSiSensor> sensors 
            = subdetector.getDetectorElement().findDescendants(HpsSiSensor.class);
        for (HpsSiSensor sensor : sensors) { 
            Pair<Integer, Integer> daqPair 
                = new Pair<Integer, Integer>(sensor.getFebID(), sensor.getFebHybridID());
        logger.fine("FEB ID: " + sensor.getFebID() 
                  + " Hybrid ID: " + sensor.getFebHybridID());
            daqPairToSensor.put(daqPair, sensor);
        }
        this.isDaqMapSetup = true; 
    }

    /**
     *  Get the sensor associated with a set of samples.  The sample block of
     *  data is used to extract the FEB ID and FEB Hybrid ID corresponding to 
     *  the samples. 
     *
     *  @param data : sample block of data
     *  @return The sensor associated with a set of sample 
     */
    @Override
    protected HpsSiSensor getSensor(int[] data) {
        
        //logger.fine("FEB ID: " + SvtEvioUtils.getFebID(data) 
        //          + " Hybrid ID: " + SvtEvioUtils.getFebHybridID(data));
        
        Pair<Integer, Integer> daqPair 
            = new Pair<Integer, Integer>(SvtEvioUtils.getFebIDFromMultisample(data), 
                                         SvtEvioUtils.getFebHybridIDFromMultisample(data));
        
        return daqPairToSensor.get(daqPair);
    }
    
    /**
     *  Check whether a data bank is valid i.e. contains SVT samples only.  For
     *  the engineering run, a valid data bank has a tag of 3.
     * 
     *  @param dataBank - An EVIO bank containing integer data
     *  @return true if the bank is valid, false otherwise
     * 
     */
    @Override
    protected boolean isValidDataBank(BaseStructure dataBank) { 
        
        // The SVT configuration is stored in a bank with tag equal to 57614.
        // All other event banks are invalid
        if (dataBank.getHeader().getTag() == 57614) { 
            
            // Store the event bank for processing later.
            eventBanks.add(dataBank);
            
            return false;
        } else if (dataBank.getHeader().getTag() != 3) return false; 
        
        return true; 
    }
    
    /**
     * Check whether the samples are valid. Specifically, check if the samples
     * are APV header or tails.
     * 
     * @param data : sample block of data
     * @return true if the samples are valid, false otherwise
     */
    protected boolean isValidSampleSet(int[] data) {
        return !(SvtEvioUtils.isMultisampleHeader(data) || SvtEvioUtils.isMultisampleTail(data));        
    }
   
    /**
     *  Process an EVIO event and extract all information relevant to the SVT.
     *  
     *  @param event - EVIO event to process
     *  @param lcsimEvent - LCSim event to put collections into 
     *  @return true if the EVIO was processed successfully, false otherwise 
     * @throws SvtEvioReaderException 
     */
    @Override
    public boolean processEvent(EvioEvent event, EventHeader lcsimEvent) throws SvtEvioReaderException {
        
        // Make RawTrackerHits.  This will also search for and store banks containing
        // the configuration of the SVT.
        boolean success = super.processEvent(event, lcsimEvent);
        
        /*logger.fine("Event contains " + eventBanks.size() + " event banks");
        // Loop through all of the event banks and process them
        for (BaseStructure eventBank : eventBanks) { 
           logger.fine(eventBank.toString());
           if (eventBank.getHeader().getTag() == 57614) { 
               logger.fine("Configuration bank found");
               String[] stringData = eventBank.getStringData();
               logger.fine("String data size: " + stringData.length);
               System.out.println("Configuration: ");
               for (String stringDatum : stringData) {
                  System.out.println("Data: " + stringDatum); 
               }
           }
        }*/
        
        // Clear out the event banks after they have been processed
        eventBanks.clear();
        
        return success;
    }
    
    /**
     *  Make a {@linkplain RawTrackerHit} from a set of samples.
     * 
     *  @param data : sample block of data
     *  @return A {@linkplain RawTrackerHit}
     */
    @Override
    protected RawTrackerHit makeHit(int[] data) {
         
        //logger.fine("Channel: " + SvtEvioUtils.getPhysicalChannelNumber(data));
        return makeHit(data, SvtEvioUtils.getPhysicalChannelNumber(data));
    }


    @Override
    protected void processSvtHeaders(List<SvtHeaderDataInfo> headers, EventHeader lcsimEvent)
            throws SvtEvioHeaderException {
        // TODO Auto-generated method stub
        
    }
    
    
}
