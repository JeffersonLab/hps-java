package org.hps.evio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jlab.coda.jevio.DataType;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.IEvioFilter;
import org.jlab.coda.jevio.IEvioStructure;
import org.jlab.coda.jevio.StructureFinder;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.StructureType;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.base.BaseRawTrackerHit;
import org.lcsim.geometry.Subdetector;
import org.lcsim.lcio.LCIOUtil;
import org.lcsim.util.log.DefaultLogFormatter;
import org.lcsim.util.log.LogUtil;

import org.hps.util.Pair;

/**
 * Abstract SVT EVIO reader used to convert SVT bank sample blocks to
 * {@link RawTrackerHit}s.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @date November 20, 2014
 *
 */
public abstract class AbstractSvtEvioReader extends EvioReader {
    
    
    // Initialize the logger
    protected static Logger logger = LogUtil.create(AbstractSvtEvioReader.class.getName(), 
            new DefaultLogFormatter(), Level.INFO);

    // A Map from DAQ pair (FPGA/Hybrid or FEB ID/FEB Hybrid ID) to the
    // corresponding sensor
    protected Map<Pair<Integer /* FPGA */, Integer /* Hybrid */>,
                  HpsSiSensor /* Sensor */> daqPairToSensor 
                      = new HashMap<Pair<Integer, Integer>, HpsSiSensor>();

    // Flag indicating whether the DAQ map has been setup
    protected boolean isDaqMapSetup = false;

    // Collections and names
    private static final String SVT_HIT_COLLECTION_NAME = "SVTRawTrackerHits";
    List<RawTrackerHit> rawHits = new ArrayList<RawTrackerHit>();

    // Constants
    private static final String SUBDETECTOR_NAME = "Tracker";
    private static final String READOUT_NAME = "TrackerHits";

    /**
     *  Get the minimum SVT ROC bank tag in the event.
     *
     *  @return Minimum SVT ROC bank tag
     */
    abstract protected int getMinRocBankTag(); 
    
    /**
     *  Get the maximum SVT ROC bank tag in the event.
     *
     *  @return Maximum SVT ROC bank tag
     */
    abstract protected int getMaxRocBankTag(); 
    

    /**
     *  Get the SVT ROC bank number of the bank encapsulating the SVT samples.
     * 
     *  @return SVT ROC bank number 
     */
    abstract protected int getRocBankNumber(); 
    
    /**
     *  Get the number of 32 bit integers composing the data block header
     *
     *  @return The header length
     */
    abstract protected int getDataHeaderLength();

    /**
     *  Get the number of 32 bit integers composing the data block tail (the 
     *  data inserted after all sample blocks in a data block)
     * 
     *  @return The tail length 
     */
    abstract protected int getDataTailLength();

    /**
     *  A method to setup a mapping between a DAQ pair 
     *  (FPGA/Hybrid or FEB ID/FEB Hybrid ID) and the corresponding sensor.
     *
     *  @param subdetector - The tracker {@link Subdetector} object
     */
    // TODO: This can probably be done when the conditions are loaded.
    abstract protected void setupDaqMap(Subdetector subdetector);

    /**
     *  Get the sensor associated with a set of samples  
     *
     *  @param data - sample block of data
     *  @return The sensor associated with a set of sample 
     */
    abstract protected HpsSiSensor getSensor(int[] data);


    /**
     *  Check whether a data bank is valid i.e. contains SVT samples only.
     * 
     *  @param dataBank - An EVIO bank containing integer data
     *  @return true if the bank is valid, false otherwise
     */
    abstract protected boolean isValidDataBank(BaseStructure dataBank); 

    /**
     * Check whether the samples are valid
     * 
     * @param data : sample block of data
     * @return true if the samples are valid, false otherwise
     */
    abstract protected boolean isValidSampleSet(int[] data);
    
    /**
     *  Process an EVIO event and extract all information relevant to the SVT.
     *  
     *  @param event - EVIO event to process
     *  @param lcsimEvent - LCSim event to put collections into 
     *  @return true if the EVIO was processed successfully, false otherwise 
     */
    public boolean processEvent(EvioEvent event, EventHeader lcsimEvent) {
        return this.makeHits(event, lcsimEvent);
    }

    /**
     *  Make {@link RawTrackerHit}s out of all sample sets in an SVT EVIO bank
     *  and put them into an LCSim event.
     *
     *  
     *  @param event - EVIO event to process
     *  @param lcsimEvent - LCSim event to put collections into 
     *  @return true if the raw hits were created successfully, false otherwise 
     */
    public boolean makeHits(EvioEvent event, EventHeader lcsimEvent) {

        // Retrieve the ROC banks encapsulated by the physics bank.  The ROC
        // bank range is set in the subclass.
        List<BaseStructure> rocBanks = new ArrayList<BaseStructure>();
        for (int rocBankTag = this.getMinRocBankTag(); 
                rocBankTag <= this.getMaxRocBankTag(); rocBankTag++) { 
            
            logger.fine("Retrieving ROC bank: " + rocBankTag);
            List<BaseStructure> matchingRocBanks = this.getMatchingBanks(event, rocBankTag);
            if (matchingRocBanks == null) { 
                logger.fine("ROC bank " + rocBankTag + " was not found!");
                continue;
            }
            rocBanks.addAll(matchingRocBanks);
        }
        logger.fine("Total ROC banks found: " + rocBanks.size());
        
        // Return false if ROC banks weren't found
        if (rocBanks.isEmpty()) return false;  
    
        // Setup the DAQ map if it's not setup
        if (!this.isDaqMapSetup)
            this.setupDaqMap(lcsimEvent.getDetector().getSubdetector(
                    SUBDETECTOR_NAME));

        // Clear the list of raw tracker hits
        rawHits.clear();

        // Loop over the SVT ROC banks and process all samples
        for (BaseStructure rocBank : rocBanks) { 
            
            logger.fine("ROC bank: " + rocBank.toString());
            
            logger.fine("Processing ROC bank " + rocBank.getHeader().getTag());
            
            // If the ROC bank doesn't contain any data, raise an exception
            if (rocBank.getChildCount() == 0) { 
                throw new RuntimeException("[ " + this.getClass().getSimpleName() 
                        + " ]: SVT bank doesn't contain any data banks.");
            }
            
            // Get the data banks containing the SVT samples.  
            List<BaseStructure> dataBanks = rocBank.getChildren(); 
            logger.fine("Total data banks found: " + dataBanks.size());
            
            // Loop over all of the data banks contained by the ROC banks and 
            // processed them
            for (BaseStructure dataBank : dataBanks) { 
        
                logger.fine("Processing data bank: " + dataBank.toString());
            
                // Check that the bank is valid
                if (!this.isValidDataBank(dataBank)) continue;
                
                // Get the int data encapsulated by the data bank
                int[] data = dataBank.getIntData();
                logger.fine("Total number of integers contained by the data bank: " + data.length);
        
                // Check that a complete set of samples exist
                int sampleCount = data.length - this.getDataHeaderLength()
                        - this.getDataTailLength();
                logger.fine("Total number of  samples: " + sampleCount);
                if (sampleCount % 4 != 0) {
                    throw new RuntimeException("[ "
                            + this.getClass().getSimpleName()
                            + " ]: Size of samples array is not divisible by 4");
                }

                // Loop through all of the samples and make hits
                for (int samplesN = 0; samplesN < sampleCount; samplesN += 4) {

                    int[] samples = new int[4];
                    System.arraycopy(data, this.getDataHeaderLength() + samplesN, samples, 0, samples.length);
                    
                    // If a set of samples is associated with an APV header or tail, skip it
                    if (!this.isValidSampleSet(samples)) continue;
                    rawHits.add(this.makeHit(samples));
                }
            }
        }
        
        logger.fine("Total number of RawTrackerHits created: " + rawHits.size());

        // Turn on 64-bit cell ID.
        int flag = LCIOUtil.bitSet(0, 31, true);
        // Add the collection of raw hits to the LCSim event
        lcsimEvent.put(SVT_HIT_COLLECTION_NAME, rawHits, RawTrackerHit.class, flag, READOUT_NAME);

        return true;
    }

    /**
     *  Make a {@link RawTrackerHit} from a set of samples.
     * 
     *  @param data : sample block of data
     *  @return A raw hit
     */
    protected abstract RawTrackerHit makeHit(int[] data); 
    
    /**
     *  Make a {@link RawTrackerHit} from a set of samples.
     * 
     *  @param data : Sample block of data
     *  @param channel : Channel number associated with these samples
     *  @return A raw hit
     */
    protected RawTrackerHit makeHit(int[] data, int channel) { 

        // Get the sensor associated with this sample
        HpsSiSensor sensor = this.getSensor(data);
        //logger.fine(sensor.toString());
        
        // Use the channel number to create the cell ID
        long cellID = sensor.makeChannelID(channel);
        
        // Set the hit time.  For now this will be zero
        int hitTime = 0;
    
        // Create and return a RawTrackerHit
        return new BaseRawTrackerHit(hitTime, cellID, SvtEvioUtils.getSamples(data), null, sensor);
    }
    
    /**
     *  Retrieve all the banks in an event that match the given tag in their
     *  header and are not data banks. 
     *
     *  @param structure : The event/bank being queried
     *  @param tag : The tag to match
     *  @return A collection of all bank structures that pass the filter 
     *          provided by the event
     */
    protected List<BaseStructure> getMatchingBanks(BaseStructure structure, final int tag) { 
        IEvioFilter filter = new IEvioFilter() { 
            public boolean accept(StructureType type, IEvioStructure struc) { 
                return (type == StructureType.BANK) 
                        && (tag == struc.getHeader().getTag())
                        && (struc.getHeader().getDataType() == DataType.ALSOBANK);
            }
        };
        return StructureFinder.getMatchingStructures(structure, filter);
    }
}
