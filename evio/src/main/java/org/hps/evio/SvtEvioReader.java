package org.hps.evio;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.hps.record.svt.SvtEvioExceptions.SvtEvioHeaderException;
import org.hps.record.svt.SvtEvioExceptions.SvtEvioReaderException;
import org.hps.record.svt.SvtEvioUtils;
import org.hps.record.svt.SvtHeaderDataInfo;
import org.hps.util.Pair;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Subdetector;

/**
 * SVT EVIO reader used to convert SVT bank integer data to LCIO objects.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public class SvtEvioReader extends AbstractSvtEvioReader {

    /**
     * Initialize the logger.
     */
    private static Logger LOG = Logger.getLogger(SvtEvioReader.class.getPackage().getName());
    
    // -----------------//
    // --- Constants ---//
    // -----------------//
    private static final int DATA_HEADER_LENGTH = 1;
    private static final int DATA_TAIL_LENGTH = 1;
    public static final int MIN_ROC_BANK_TAG = 51;
    public static final int MAX_ROC_BANK_TAG = 66;
    public static final int DATA_BANK_TAG = 3;
    private static final int ROC_BANK_NUMBER = 0;

    // Container for sample headers
    List< SvtHeaderDataInfo > headers = new ArrayList< SvtHeaderDataInfo >(); 

    /**
     * Get the minimum SVT ROC bank tag in the event.
     *
     * @return Minimum SVT ROC bank tag
     */
    @Override
    protected int getMinRocBankTag() {
        return MIN_ROC_BANK_TAG;
    }

    /**
     * Get the maximum SVT ROC bank tag in the event.
     *
     * @return Maximum SVT ROC bank tag
     */
    @Override
    protected int getMaxRocBankTag() {
        return MAX_ROC_BANK_TAG;
    }

    @Override
    protected int getMinDataBankTag() {
        return DATA_BANK_TAG;
    }

    @Override
    protected int getMaxDataBankTag() {
        return DATA_BANK_TAG;
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
     * Get the number of 32 bit integers composing the data block header
     *
     * @return The header length
     */
    @Override
    protected int getDataHeaderLength() {
        return DATA_HEADER_LENGTH;
    }

    /**
     * Get the number of 32 bit integers composing the data block tail (the
     * data inserted after all sample blocks in a data block)
     * 
     * @return The tail length
     */
    @Override
    protected int getDataTailLength() {
        return DATA_TAIL_LENGTH;
    }

    /**
     * A method to setup a mapping between a DAQ pair (FEB/FEB Hybrid) and the
     * corresponding sensor.
     *
     * @param subdetector : The tracker {@link Subdetector} object
     */
    // TODO: This can probably be done when the conditions are loaded.
    @Override
    protected void setupDaqMap(Subdetector subdetector) {

        List<HpsSiSensor> sensors = subdetector.getDetectorElement().findDescendants(HpsSiSensor.class);
        for (HpsSiSensor sensor : sensors) {
            Pair<Integer, Integer> daqPair = new Pair<Integer, Integer>(sensor.getFebID(), sensor.getFebHybridID());
            LOG.info("FEB ID: " + sensor.getFebID() + " Hybrid ID: " + sensor.getFebHybridID());
            daqPairToSensor.put(daqPair, sensor);
        }
        this.isDaqMapSetup = true;
    }

    /**
     * Get the sensor associated with a set of samples. The sample block of
     * data is used to extract the FEB ID and FEB Hybrid ID corresponding to
     * the samples.
     *
     * @param data : sample block of data
     * @return The sensor associated with a set of sample
     */
    @Override
    protected HpsSiSensor getSensor(int[] data) {

        //System.out.println("FEB ID: " + SvtEvioUtils.getFebIDFromMultisample(data)
        // + " Hybrid ID: " + SvtEvioUtils.getFebHybridIDFromMultisample(data));

        Pair<Integer, Integer> daqPair = new Pair<Integer, Integer>(SvtEvioUtils.getFebIDFromMultisample(data),
                SvtEvioUtils.getFebHybridIDFromMultisample(data));

        return daqPairToSensor.get(daqPair);
    }

    /**
     * Check whether the samples are valid. Specifically, check if the samples
     * are APV header or tails.
     * 
     * @param data : sample block of data
     * @return true if the samples are valid, false otherwise
     */
    @Override
    protected boolean isValidSampleSet(int[] data) {
        return !(SvtEvioUtils.isMultisampleHeader(data) || SvtEvioUtils.isMultisampleTail(data));
    }

    /**
     * Process an EVIO event and extract all information relevant to the SVT.
     * 
     * @param event - EVIO event to process
     * @param lcsimEvent - LCSim event to put collections into
     * @return true if the EVIO was processed successfully, false otherwise
     * @throws SvtEvioReaderException
     */
    @Override
    public boolean processEvent(EvioEvent event, EventHeader lcsimEvent) throws SvtEvioReaderException {

        // Make RawTrackerHits. This will also search for and store banks containing
        // the configuration of the SVT.
        boolean success = super.processEvent(event, lcsimEvent);

        return success;
    }

    /**
     * Make a {@linkplain RawTrackerHit} from a set of samples.
     * 
     * @param data : sample block of data
     * @return A {@linkplain RawTrackerHit}
     */
    @Override
    protected RawTrackerHit makeHit(int[] data) {

        //System.out.println("[ SvtEvioReader ][ makeHit ]: Channel: " 
        //        + SvtEvioUtils.getPhysicalChannelNumber(data));
        return makeHit(data, SvtEvioUtils.getPhysicalChannelNumber(data));
    }

    @Override
    protected void processSvtHeaders(List<SvtHeaderDataInfo> headers, EventHeader lcsimEvent)
            throws SvtEvioHeaderException {
        // Not used ...
    }

    @Override
    protected List< int[] > extractMultiSamples(int sampleCount, int[] data) {
        
        List<int[]> sampleList = new ArrayList<int[]>();
        // Loop through all of the samples and make hits
        for (int samplesN = 0; samplesN < sampleCount; samplesN += 4) {
            int[] samples = new int[4];
            System.arraycopy(data, this.getDataHeaderLength() + samplesN, samples, 0, samples.length);
            sampleList.add(samples);
        }
        return sampleList;
    }  
    
    @Override
    public boolean makeHits(EvioEvent event, EventHeader lcsimEvent) 
        throws SvtEvioReaderException {
        
        // First extract the multisamples and headers
        super.makeHits(event, lcsimEvent);

        // Process the headers
        this.processSvtHeaders(headers, lcsimEvent);
        
        // Clear header data list after processing. This was a big memory leak! --JM
        headers.clear();
        
        return true; 
    }

    @Override
    protected List<RawTrackerHit> makeHits(int bankNumber, int[] data) 
        throws SvtEvioReaderException { 
   
        List<RawTrackerHit> rawHits = new ArrayList<RawTrackerHit>();

        // Check that a complete set of samples exists 
        int sampleCount = data.length - this.getDataHeaderLength() - this.getDataTailLength();
        //System.out.println("Total number of  samples: " + sampleCount);
        if (sampleCount % 4 != 0) {
            throw new SvtEvioReaderException("[ " + this.getClass().getSimpleName()
                    + " ]: Size of samples array is not divisible by 4");
        }
        //System.out.println("Total number of multisamples: " + sampleCount);
    
        // Extract header and tail information
        SvtHeaderDataInfo headerData = this.extractSvtHeader(bankNumber, data);
            
        // Check that the multisample count is consistent
        this.checkSvtSampleCount(sampleCount, headerData);
       
        // Add header to list
        headers.add(headerData);

        // Store the multisample headers. Note that the length is not known but
        // can't be longer than the multisample count in other words the data 
        // can be only header multisamples for example.
        int multisampleHeaderData[] = new int[sampleCount];
        int multisampleHeaderIndex = 0;
    
        List< int[] > multiSamples = extractMultiSamples(sampleCount, data);
        
        for (int[] samples : multiSamples) { 
    
            if (SvtEvioUtils.isMultisampleHeader(samples)) {
                
                //System.out.println("[ SvtEvioReader ][ processMultiSamples ]: This is a header multisample for APV "
                //         + SvtEvioUtils.getApvFromMultiSample(samples) + " CH " 
                //         + SvtEvioUtils.getChannelNumber(samples));
                
                // Extract data words from multisample header and update index
                multisampleHeaderIndex += this.extractMultisampleHeaderData(samples, multisampleHeaderIndex, multisampleHeaderData);
            
            } else { 
                
                //System.out.println("[ SvtEvioReader ][ processMultiSamples ]: This is a data multisample for APV "
                //        + SvtEvioUtils.getApvFromMultiSample(samples) + " CH " + SvtEvioUtils.getChannelNumber(samples));
            }
            
            // If a set of samples is associated with an APV header or tail, skip it
            if (!this.isValidSampleSet(samples)) continue;
        
            // Create raw hits and add them to the list of raw hits
            RawTrackerHit hit = this.makeHit(samples);
            if (hit != null) {
                rawHits.add(hit);
            }
        }

        //System.out.println("[ SvtEvioReader ][ processMultiSamples ]: Got " 
        //        + multisampleHeaderIndex + " multisampleHeaderIndex for " + sampleCount + " sampleCount");
        
        // add multisample header tails to header data object
        this.setMultiSampleHeaders(headerData, multisampleHeaderIndex, multisampleHeaderData);
        
        return rawHits; 
    }
}
