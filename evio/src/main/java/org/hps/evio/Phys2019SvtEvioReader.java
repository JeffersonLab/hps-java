package org.hps.evio;

import java.util.List;
import java.util.ArrayList;

import org.hps.record.svt.SvtEvioUtils;
import org.hps.record.svt.SvtEvioExceptions.SvtEvioHeaderException;
import org.hps.record.svt.SvtEvioExceptions.SvtEvioReaderException;
import org.hps.record.svt.SvtHeaderDataInfo;
import org.hps.util.Pair;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Subdetector;

/**
 * SVT EVIO reader used to convert 2019 Physics Run SVT raw data to LCIO 
 * objects.
 *
 * @author Omar Moreno, SLAC National Accelerator Laboratory 
 */
public class Phys2019SvtEvioReader extends AbstractSvtEvioReader {

    //
    //---------------//
    //   Constants   //
    //---------------//
    //
    private static final int MIN_ROC_BANK_TAG = 0x2; 
    private static final int MAX_ROC_BANK_TAG = 0x3; 
    private static final int DATA_BANK_TAG = 0xe130;


    public static final int CHANNELS_PER_APV25 = 128;

    @Override
    protected int getMinRocBankTag() {
        return MIN_ROC_BANK_TAG;
    }

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

    @Override
    protected int getRocBankNumber() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    protected int getDataHeaderLength() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    protected int getDataTailLength() {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    protected boolean isValidSampleSet(int[] data) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected void processSvtHeaders(List<SvtHeaderDataInfo> headers, EventHeader lcsimEvent)
            throws SvtEvioHeaderException {
        // TODO Auto-generated method stub
        
    }

    /**
     * Make a {@linkplain RawTrackerHit} from a set of samples.
     * 
     * @param data : sample block of data
     * @return A {@linkplain RawTrackerHit}
     */
    @Override
    protected RawTrackerHit makeHit(int[] data) {

        int febID = SvtEvioUtils.getFebIDFromMultisampleTail(data[3]); 
        int pChannel = 100; 
        //System.out.println("[ Phys2019SvtEvioReader ][ makeHit ] FEB ID: " + febID);  
        if ((febID == 0) || (febID == 1)) { 
           
            // 
            int channel = SvtEvioUtils.getChannelNumber(data);

            // Extract the APV ID from the data
            int apv = SvtEvioUtils.getApvFromMultiSample(data);
            if (apv == 0) apv = 1; 
            else if (apv == 1) apv = 0;   

            // Get the physical channel number
            pChannel = apv*CHANNELS_PER_APV25 + channel; 

            if (pChannel < 0 || pChannel > 512) {
                throw new RuntimeException("[ Phys2019SvtEvioReader ][ makeHit ]: Physical channel " +
                        pChannel + " is outside of the valid range!"); 
            }


        } else { 
            pChannel = SvtEvioUtils.getPhysicalChannelNumber(data); 
        }

        //System.out.println("[ Phys2019SvtEvioReader ][ makeHit ]: Channel: " + pChannel);  
        return makeHit(data, pChannel);  
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

    @Override
    protected List<RawTrackerHit> makeHits(int bankNumber, int[] data) throws SvtEvioReaderException {
        
        List< RawTrackerHit > rawHits = new ArrayList< RawTrackerHit >(); 

        List< int[] > multiSamples = extractMultiSamples(0, data);

        for (int[] samples : multiSamples) { 
//            rawHits.add(this.makeHit(samples)); 
            RawTrackerHit hit = this.makeHit(samples);
            if (hit != null) {
                rawHits.add(hit); 
            }
        }

        return rawHits;
    }

    @Override
    protected List<int[]> extractMultiSamples(int sampleCount, int[] data) {
        
        List< int[] > sampleList = new ArrayList< int[] >(); 
        int currentSample = (data.length - 1); 
        
        // Transverse the data starting with the last word and find the tail
        // of the first RSSI frame.
        while ( currentSample != 0) {
            
            // The first few words of the RSSI frame tail is tagged with 0x15.
            // If a tail is found, use it to extract the number of multisamples
            // in the frame.
            if ( ((data[currentSample] >> 27) & 0x1F) == 0x15) { 

                // The tail is four words long.  The number of multisamples
                // is stored in the first word.
                currentSample -= 3; 
                int multiSampleCount = data[currentSample] & 0xFFF; 
                //System.out.println("Total number of multisamples: " + multiSampleCount);

                // Once the number of multi samples is know, copy them and store
                // the in a list so they can be used to make RawTrackerHits.
                for (int imsample = 0; imsample < multiSampleCount; imsample++) { 
                    int[] samples = new int[4];
                    currentSample -= 4; 
                    System.arraycopy(data, currentSample, samples, 0, samples.length);   
                    if (((samples[3] >>> 30) & 0x1) == 1) { 
                        //System.out.println("Found a header."); 
                        continue; 
                    }
                    sampleList.add(samples); 
                }
                //System.out.println("Total number of multisamples extracted: " + sampleList.size()); 
            }

            // If a tail event is found, skip to the next word.
            currentSample -= 1;
        } 
    
        return sampleList; 
    } 
}
