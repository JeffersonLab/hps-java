package org.hps.digi.nospacing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.hps.readout.ReadoutDriver;
import org.hps.readout.rawconverter.AbstractMode3RawConverter;
import org.hps.readout.util.collection.LCIOCollectionFactory;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;
import org.hps.readout.ReadoutDataManager;
/**
 * <code>RawConverterNoSpacingReadoutDriver</code> processes ADC hit data
 * objects and converts them to energy hit objects. It serves as an
 * interface to a {@link
 * org.hps.readout.rawconverter.AbstractMode3RawConverter
 * AbstractMode3RawConverter} object, where the actual conversion is
 * performed.
 * <br/><br/>
 * <code>RawConverterNoSpacingReadoutDriver</code> itself is abstract - it
 * requires that implementing classes handle any subdetector-specific
 * functionality.
 */
public abstract class RawConverterNoSpacingReadoutDriver extends ReadoutDriver {
    /**
     * Sets the name of the input {@link
     * org.lcsim.event.RawCalorimeterHit RawCalorimeterHit}
     * collection.
     */
    private String inputCollectionName;
    
    /**
     * Sets the name of the output {@link
     * org.lcsim.event.CalorimeterHit CalorimeterHit} collection.
     */
    private String outputCollectionName;
    
    /**
     * Tracks the current local time in nanoseconds for this driver.
     */
    private double localTime = 0.0;

    //size to look for hits in 4ns clock ticks
    private double EVENT_WINDOW=48; 
    
    /**
     * Indicates whether channels that are marked as "bad" in the
     * conditions database should be skipped when producing hits.
     */
    protected boolean skipBadChannels = false;

    protected boolean checkInput = false;
    
    protected RawConverterNoSpacingReadoutDriver(String defaultInputCollectionName, String defaultOutputCollectionName) {
        inputCollectionName = defaultInputCollectionName;
        outputCollectionName = defaultOutputCollectionName;
    }
    
    @Override
    public final void detectorChanged(Detector detector) {
        // Allow implementing drivers to catch the detector changed
        // event, if needed.
        updateDetectorDependentParameters(detector);
        
        // Update the converter.
        getConverter().updateDetector(detector);
        
        // Update the readout name for the managed collection.
        ReadoutDataManager.updateCollectionReadoutName(outputCollectionName, CalorimeterHit.class, getSubdetectorReadoutName(detector));
    }
    
    @Override
    public final void process(EventHeader event) {
        // Check the data management driver to determine whether the
        // input collection is available or not.
        if(checkInput&&!ReadoutDataManager.checkCollectionStatus(inputCollectionName, localTime + 4.0)) {
	    //	    System.out.println(this.getClass().getName()+":: checkInput or Collection status Failed");
            return;
        }
        
        // Get all of the raw hits in the current clock-cycle.
	//        Collection<RawCalorimeterHit> rawHits = ReadoutDataManager.getData(localTime, localTime + 4.0, inputCollectionName, RawCalorimeterHit.class);
        Collection<RawCalorimeterHit> rawHits = ReadoutDataManager.getData(ReadoutDataManager.getCurrentTime(), ReadoutDataManager.getCurrentTime() + 4.0*EVENT_WINDOW, inputCollectionName, RawCalorimeterHit.class);
	//	System.out.println("RawConverter:: "+ inputCollectionName+" local time = "+localTime+" number of seeds = "+rawHits.size());
        // Increment the local time.
	//        localTime += 4.0;
        
        // Pass the raw hits to the raw converter to obtain proper
        // calorimeter hits. In readout, raw hits are always Mode-3,
        // so there is no need to check the form.
        List<CalorimeterHit> newHits = new ArrayList<CalorimeterHit>();
        
        for(RawCalorimeterHit hit : rawHits) {
            // Convert the raw hit.
            CalorimeterHit newHit = getConverter().convertHit(hit, 0.0);
            
            // If the hit is on a bad channel, and these are set to
            // be skipped, ignore the hit. Otherwise, add it to the
            // output list.
            if(skipBadChannels && isBadChannel(newHit.getCellID())) {
                continue;
            }
            
            // Add the new hit.
            newHits.add(newHit);
        }
	//        System.out.println("RawConverter:: "+ outputCollectionName+" adding new hits with size = "+newHits.size()+" at time = "+localTime);
        // Add the calorimeter hit collection to the data manager.
        ReadoutDataManager.addData(outputCollectionName, newHits, CalorimeterHit.class);
        // Increment the local time for real.
	localTime += 4.0*125;
		
    }
    
    @Override
    public void startOfData() {
        // Set the LCIO flags for the output collection. Flags are
        // set to store the hit time and hit position respectively.
        int flags = 0;
        flags += 1 << LCIOConstants.RCHBIT_TIME;
        flags += 1 << LCIOConstants.RCHBIT_LONG;
        
        // Define the LCSim collection parameters for this driver's
        // output.
        LCIOCollectionFactory.setCollectionName(outputCollectionName);
        LCIOCollectionFactory.setProductionDriver(this);
        LCIOCollectionFactory.setFlags(flags);
        
        // Set the dependencies for the driver and register its
        // output collections with the data management driver.
        addDependency(inputCollectionName);
        
        // Register the output collection.
        ReadoutDataManager.registerCollection(LCIOCollectionFactory.produceLCIOCollection(CalorimeterHit.class), isPersistent(),
                getReadoutWindowBefore(), getReadoutWindowAfter());
    }
    
    /**
     * Gets the {@link org.hps.readout.ReadoutRawConverter
     * ReadoutRawConverter} object used to convert hits for this
     * subdetector.
     * @return Returns the raw converter.
     */
    protected abstract AbstractMode3RawConverter getConverter();
    
    /**
     * Gets the readout name for this subdetector from the geometry.
     * @param detector - The geometry object.
     * @return Returns the subdetector readout name.
     */
    protected abstract String getSubdetectorReadoutName(Detector detector);
    
    @Override
    protected final double getTimeDisplacement() {
        return 0;
    }

    @Override
    protected final double getTimeNeededForLocalOutput() {
        return 0;
    }
    
    /**
     * Indicates whether or not the channel on which a hit occurs is
     * a "bad" channel according to the conditions database.
     * @param hit - The hit to check.
     * @return Returns <code>true</code> if the hit channel is
     * flagged as "bad" and <code>false</code> otherwise.
     * @throws UnsupportedOperationException Occurs if the
     * subdetector represented by the driver does not support bad
     * channel exclusion.
     */
    protected boolean isBadChannel(long channelID) {
        throw new UnsupportedOperationException("Driver \"" + getClass().getSimpleName() + "\" does not support bad channel exclusion.");
    }
    
    /**
     * Updates any detector-specific parameters needed by the
     * implementing class.
     * @param detector - The current detector geometry.
     */
    protected abstract void updateDetectorDependentParameters(Detector detector);
    
    /**
     * Sets the name of the input collection containing the objects
     * of type {@link org.lcsim.event.RawCalorimeterHit
     * RawCalorimeterHit} that are output by the digitization driver.
     * @param collection - The name of the input raw hit collection.
     */
    public void setInputCollectionName(String collection) {
        inputCollectionName = collection;
    }
    
    /**
     * Sets the number of integration samples that should be included
     * in a pulse integral after the threshold-crossing event.
     * @param samples - The number of samples, where a sample is a
     * 4 ns clock-cycle.
     */
    public void setNumberSamplesAfter(int samples) {
        getConverter().setNumberSamplesAfter(4 * samples);
    }
    
    /**
     * Sets the number of integration samples that should be included
     * in a pulse integral before the threshold-crossing event.
     * @param samples - The number of samples, where a sample is a
     * 4 ns clock-cycle.
     */
    public void setNumberSamplesBefore(int samples) {
        getConverter().setNumberSamplesBefore(4 * samples);
    }
    
    /**
     * Sets factor of unit conversion for returned value of the method 
     * <code>AbstractBaseRawConverter::adcToEnergy()</code>.
     * @param factor of unit conversion
     */
    public void setFactorUnitConversion(double factor) {
        getConverter().setFactorUnitConversion(factor);
    }
    
    /**
     * Sets the name of the output collection containing the objects
     * of type {@link org.lcsim.event.CalorimeterHit CalorimeterHit}
     * that are output by this driver.
     * @param collection - The name of the output hit collection.
     */
    public void setOutputCollectionName(String collection) {
        outputCollectionName = collection;
    }
    
    /**
     * Indicates whether or not data from channels flagged as "bad"
     * in the conditions system should be ignored. <code>true</code>
     * indicates that they should be ignored, and <code>false</code>
     * that they should not.
     * @param apply - <code>true</code> indicates that "bad" channels
     * will be ignored and <code>false</code> that they will not.
     * @throws UnsupportedOperationException Occurs if the
     * subdetector represented by the driver does not support bad
     * channel exclusion.
     */
    public void setSkipBadChannels(boolean state) {
        throw new UnsupportedOperationException("Driver \"" + getClass().getSimpleName() + "\" does not support bad channel exclusion.");
    }
    
    /**
     * Sets the size of the ADC buffer. This is needed for proper
     * handling of Mode-3 hits in the raw converter.
     * @param window - The buffer size in units of 4 ns clock-cycles.
     */
    public void setReadoutWindow(int window) {
        getConverter().setWindowSamples(window);
    }
}
