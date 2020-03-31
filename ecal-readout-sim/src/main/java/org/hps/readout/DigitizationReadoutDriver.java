package org.hps.readout;

import static org.hps.recon.ecal.EcalUtils.fallTime;
import static org.hps.recon.ecal.EcalUtils.maxVolt;
import static org.hps.recon.ecal.EcalUtils.nBit;
import static org.hps.recon.ecal.EcalUtils.riseTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hps.readout.util.DoubleRingBuffer;
import org.hps.readout.util.IntegerRingBuffer;
import org.hps.readout.util.ObjectRingBuffer;
import org.hps.readout.util.collection.LCIOCollection;
import org.hps.readout.util.collection.LCIOCollectionFactory;
import org.hps.readout.util.collection.TriggeredLCIOData;
import org.hps.recon.ecal.EcalUtils;
import org.hps.util.RandomGaussian;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.base.BaseCalorimeterHit;
import org.lcsim.event.base.BaseLCRelation;
import org.lcsim.event.base.BaseRawCalorimeterHit;
import org.lcsim.event.base.BaseRawTrackerHit;
import org.lcsim.event.base.BaseSimCalorimeterHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.lcio.LCIOConstants;

/**
 * Class <code>DigitizationReadoutDriver</code> performs digitization
 * of truth hits from SLIC by converting them into emulated pulses
 * and then performing pulse integration. The results are output in
 * the form of {@link org.lcsim.event.RawCalorimeterHit
 * RawCalorimeterHit} objects.
 * <br/><br/>
 * The truth hit information is retained by also producing an output
 * collection of {@link org.lcsim.event.LCRelation LCRelation}
 * objects linking the raw hits to the original {@link
 * org.lcsim.event.SimCalorimeterHit SimCalorimeterHit} objects from
 * which they were generated.
 * <br/><br/>
 * <code>DigitizationReadoutDriver</code> is itself abstract. It is
 * designed with the intent to function for both the hodoscope and
 * the calorimeter. As such, it requires its implementing classes to
 * handle certain subdetector-specific tasks.
 * 
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public abstract class DigitizationReadoutDriver<D extends Subdetector> extends ReadoutDriver {
    
    // ==============================================================
    // ==== LCIO Collections ========================================
    // ==============================================================
    
    /**
     * Specifies the name of the subdetector geometry object.
     */
    private String geometryName = null;
    /**
     * The name of the input {@link org.lcsim.event.SimCalorimeterHit
     * SimCalorimeterHit} truth hit collection from SLIC.
     */
    private String truthHitCollectionName = null;
    /**
     * The name of the digitized output {@link
     * org.lcsim.event.RawCalorimeterHit RawCalorimeterHit}
     * collection.
     */
    private String outputHitCollectionName = null;
    /**
     * The name of the {@link org.lcsim.event.LCRelation LCRelation}
     * collection that links output raw hits to the SLIC truth hits
     * from which they were generated.
     */
    private String truthRelationsCollectionName = null;
    /**
     * The name of the {@link org.lcsim.event.LCRelation LCRelation}
     * collection that links output raw hits to the SLIC truth hits
     * from which they were generated. This collection is output for
     * trigger path hits, and is never persisted.
     */
    private String triggerTruthRelationsCollectionName = null;
    /**
     * The name of the collection which contains readout hits. The
     * class type of this collection will vary based on which mode
     * the simulation is set to emulate.
     */
    private String readoutCollectionName = null;
    
    // ==============================================================
    // ==== Driver Options ==========================================
    // ==============================================================
    
    /**
     * Indicates whether or not noise should be simulated when
     * converting truth energy depositions to the voltage amplitudes.
     */
    private boolean addNoise = true;
    /**
     * Defines the number of photoelectrons per MeV of truth energy
     * for the purpose of noise calculation.
     */
    private double pePerMeV = Double.NaN;
    /**
     * Defines a fixed gain to be used for all subdetector channels.
     * A negative value will result in gains being pulled from the
     * conditions database for the run instead. Units are in MeV/ADC.
     */
    private double fixedGain = -1;
    /**
     * Defines the pulse shape to use when simulating detector truth
     * energy deposition response.
     */
    private PulseShape pulseShape = PulseShape.ThreePole;
    /**
     * Defines the pulse time parameter. This influences the shape of
     * a pulse generated from truth energy depositions and will vary
     * depending on the form of pulse selected. Units are in ns.
     */
    private double tp = Double.NaN;
    /**
     * Defines the ADC threshold needed to initiate pulse integration
     * for raw hit creation.
     */
    private int integrationThreshold = 18;
    /**
     * Defines the number of integration samples that should be
     * included in the pulse integral from before the sample that
     * exceeds the integration threshold.
     */
    private int numSamplesBefore = 5;
    /**
     * Defines the number of integration samples that should be
     * included in the pulse integral from after the sample that
     * exceeds the integration threshold.
     * Threshold-crossing sample is part of NSA.
     */
    private int numSamplesAfter = 25;
    /**
     * The format in which readout hits should be output.
     */
    private int mode = 1;
    /**
     * Specifies whether trigger path hit truth information should be
     * included in the driver output.
     */
    private boolean writeTriggerTruth = false;
    /**
     * Specifies whether readout path truth information should be
     * included in the driver output.
     */
    private boolean writeTruth = false;
    
    // ==============================================================
    // ==== Driver Parameters =======================================
    // ==============================================================
    
    /**
     * Defines the length in nanoseconds of a hardware sample.
     */
    private static final double READOUT_PERIOD = 4.0;
    /**
     * Serves as an internal clock variable for the driver. This is
     * used to track the number of clock-cycles (1 per {@link
     * org.hps.readout.ecal.updated.DigitizationReadoutDriver#READOUT_PERIOD
     * READOUT_PERIOD}).
     */
    private int readoutCounter = 0;
    /**
     * A buffer for storing pulse amplitudes representing the signals
     * from the preamplifiers. These are stored in units of Volts
     * with no pedestal. One buffer exists for each subdetector
     * channel.
     */
    private Map<Long, DoubleRingBuffer> voltageBufferMap = new HashMap<Long, DoubleRingBuffer>();
    /**
     * Buffers the truth information for each sample period so that
     * truth relations can be retained upon readout.
     */
    private Map<Long, ObjectRingBuffer<SimCalorimeterHit>> truthBufferMap = new HashMap<Long, ObjectRingBuffer<SimCalorimeterHit>>();
    /**
     * A buffer for storing ADC values representing the converted
     * voltage values from the voltage buffers. These are stored in
     * units of ADC and include a pedestal. One buffer exists for
     * each subdetector channel.
     */
    private Map<Long, IntegerRingBuffer> adcBufferMap = new HashMap<Long, IntegerRingBuffer>();
    
    /**
     * Stores the subdetector geometry object.
     */
    private D geometry = null;
    /**
     * Stores the total ADC sums for each subdetector channel that is
     * currently undergoing integration.
     */
    private Map<Long, Integer> channelIntegrationSumMap = new HashMap<Long, Integer>();
    /**
     * Stores the total ADC sums for each subdetector channel that is
     * currently undergoing integration.
     */
    private Map<Long, Set<SimCalorimeterHit>> channelIntegrationTruthMap = new HashMap<Long, Set<SimCalorimeterHit>>();
    /**
     * Stores the time at which integration began on a given channel.
     * This is used to track when the integration period has ended.
     */
    private Map<Long, Integer> channelIntegrationTimeMap = new HashMap<Long, Integer>();
    // TODO: Give this documentation.
    private Map<Long, List<Integer>> channelIntegrationADCMap = new HashMap<Long, List<Integer>>();
    /**
     * Defines the time offset of objects produced by this driver
     * from the actual true time that they should appear.
     */
    private double localTimeOffset = 0;
    /**
     * Stores the minimum length of that must pass before a new hit
     * may be integrated on a given channel.
     * Unit: clock-cycle
     */
    private static final int CHANNEL_INTEGRATION_DEADTIME = 8;
    /**
     * Defines the total time range around the trigger time in which
     * hits are output into the readout LCIO file. The trigger time
     * position within this range is determined by {@link
     * org.hps.readout.ecal.updated.DigitizationReadoutDriver#readoutOffset
     * readoutOffset}.
     */
    private int readoutWindow = 100;
    /**
     * Sets how far from the beginning of the readout window trigger
     * time should occur. A value of x, for instance would result in
     * a window that starts at <code>triggerTime - x</code> and
     * extends for a total time <code>readoutWindow</code>.
     */
    private int readoutOffset = 36;
    /**
     * Defines the LCSim collection data for the trigger hits that
     * are produced by this driver when it is emulating Mode-1 or
     * Mode-3.
     */
    private LCIOCollection<RawTrackerHit> mode13HitCollectionParams;
    /**
     * Defines the LCSim collection data for the trigger hits that
     * are produced by this driver when it is emulating Mode-7.
     */
    private LCIOCollection<RawCalorimeterHit> mode7HitCollectionParams;
    /**
     * Defines the LCSim collection data that links SLIC truth hits
     * to the their corresponding simulated output hit.
     */
    private LCIOCollection<LCRelation> truthRelationsCollectionParams;
    
    /**
     * Flag to point out that new integration could be started at a sample 
     * between <code>CHANNEL_INTEGRATION_DEADTIME</code> and <code>numSamplesAfter</code>  
     * for the case <CHANNEL_INTEGRATION_DEADTIME> is less than <code>numSamplesAfter</code> 
     */
    private Map<Long, Boolean> flagStartNewIntegration = new HashMap<>();
    
    /**
     * Since new integration could happen between <code>CHANNEL_INTEGRATION_DEADTIME</code> and <code>numSamplesAfter</code>, 
     * integration time needs to be assigned as parameter of <code>ReadoutDataManager.addData()</code>.
     * Global displacement is 0 for dependency. 
     */
    private double integrationTime = Double.NaN;
    
    
    // ==============================================================
    // ==== To Be Re-Worked =========================================
    // ==============================================================
    // TODO: We should be able to define these based on the integration parameters.
    private static final int BUFFER_LENGTH = 100;
    private static final int PIPELINE_LENGTH = 2000;
    
    @Override
    public void startOfData() {
        // Validate that all the collection names are defined.
        if(truthHitCollectionName == null || outputHitCollectionName == null || truthRelationsCollectionName == null
                || triggerTruthRelationsCollectionName == null || readoutCollectionName == null) {
            throw new RuntimeException("One or more collection names is not defined!");
        }
        
        // Calculate the correct time offset. This is a function of
        // the integration samples and the output delay.
        // Threshold-crossing sample is part of NSA.
        localTimeOffset = 4 * numSamplesAfter;
        
        // Validate that a real mode was selected.
        if(mode != 1 && mode != 3 && mode != 7) {
            throw new IllegalArgumentException("Error: Mode " + mode + " is not a supported output mode.");
        }
        
        // Add the driver dependencies.
        addDependency(truthHitCollectionName);
        
        // Define the LCSim collection parameters for this driver's
        // output. Note: Since these are not persisted, the flags and
        // readout name are probably not necessary.
        LCIOCollectionFactory.setCollectionName(outputHitCollectionName);
        LCIOCollectionFactory.setProductionDriver(this);
        LCIOCollectionFactory.setFlags((0 + (1 << LCIOConstants.CHBIT_LONG) + (1 << LCIOConstants.RCHBIT_ID1)));
        LCIOCollectionFactory.setReadoutName(truthHitCollectionName);
        LCIOCollection<RawCalorimeterHit> hitCollectionParams = LCIOCollectionFactory.produceLCIOCollection(RawCalorimeterHit.class);
        ReadoutDataManager.registerCollection(hitCollectionParams, false);
        
        LCIOCollectionFactory.setCollectionName(triggerTruthRelationsCollectionName);
        LCIOCollectionFactory.setProductionDriver(this);
        LCIOCollection<LCRelation> triggerTruthCollectionParams = LCIOCollectionFactory.produceLCIOCollection(LCRelation.class);
        ReadoutDataManager.registerCollection(triggerTruthCollectionParams, false);
        
        // Define the LCSim collection data for the on-trigger output.
        LCIOCollectionFactory.setCollectionName(readoutCollectionName);
        LCIOCollectionFactory.setProductionDriver(this);
        mode13HitCollectionParams = LCIOCollectionFactory.produceLCIOCollection(RawTrackerHit.class);
        
        LCIOCollectionFactory.setCollectionName(readoutCollectionName);
        LCIOCollectionFactory.setProductionDriver(this);
        LCIOCollectionFactory.setFlags(1 << LCIOConstants.RCHBIT_TIME);
        mode7HitCollectionParams = LCIOCollectionFactory.produceLCIOCollection(RawCalorimeterHit.class);
        
        LCIOCollectionFactory.setCollectionName(truthRelationsCollectionName);
        LCIOCollectionFactory.setProductionDriver(this);
        truthRelationsCollectionParams = LCIOCollectionFactory.produceLCIOCollection(LCRelation.class);
        
        // Run the superclass method.
        super.startOfData();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void detectorChanged(Detector detector) {
        // Throw an error if the geometry name is not set.
        if(geometryName == null) {
            throw new RuntimeException("Subdetector name is not defined!");
        }
        
        // Get the readout name from the subdetector geometry data.
        geometry = (D) detector.getSubdetector(geometryName);
        
        // Update the output LCIO collections data.
        LCIOCollectionFactory.setReadoutName(geometry.getReadout().getName());
        mode13HitCollectionParams = LCIOCollectionFactory.cloneCollection(mode13HitCollectionParams);
        LCIOCollectionFactory.setReadoutName(geometry.getReadout().getName());
        mode7HitCollectionParams = LCIOCollectionFactory.cloneCollection(mode7HitCollectionParams);
        
        // Reinstantiate the buffers.
        resetBuffers();
    }
    
    @Override
    public void process(EventHeader event) {
        
        /*
         * As a first step, truth energy depositions from SLIC must
         * be obtained and converted into voltage pulse amplitudes.
         * A buffer is maintained for the pulse amplitudes for each
         * subdetector channel, which must then be populated by these
         * values for the current simulation time.
         */
        
        // Get current SLIC truth energy depositions.
        Collection<SimCalorimeterHit> hits = ReadoutDataManager.getData(ReadoutDataManager.getCurrentTime(), ReadoutDataManager.getCurrentTime() + 2.0,
                truthHitCollectionName, SimCalorimeterHit.class);
        
        // Add the truth hits to the truth hit buffer. The buffer is
        // only incremented when the ADC buffer is incremented, which
        // is handled below.
        for(SimCalorimeterHit hit : hits) {
            // Store the truth data.
            ObjectRingBuffer<SimCalorimeterHit> hitBuffer = truthBufferMap.get(hit.getCellID());
            hitBuffer.addToCell(0, hit);
        }
        
        // Truth depositions must then be converted to voltage pulse
        // amplitudes and added to the buffer. Noise is added here as
        // well, if desired.
        for(CalorimeterHit hit : hits) {
            // Get the buffer for the current truth hit's channel.
            DoubleRingBuffer voltageBuffer = voltageBufferMap.get(hit.getCellID());
            
            // Get the truth hit energy deposition.
            double energyAmplitude = hit.getRawEnergy();
            
            // If noise should be added, calculate a random value for
            // the noise and add it to the truth energy deposition.
            if(addNoise) {
                // Calculate a randomized noise value.
                double noiseSigma = Math.sqrt(Math.pow(getNoiseConditions(hit.getCellID()) * getGainConditions(hit.getCellID()) * EcalUtils.MeV, 2)
                        + hit.getRawEnergy() * EcalUtils.MeV / pePerMeV);
                double noise = RandomGaussian.getGaussian(0, noiseSigma);
                
                // Increment the truth energy deposition by this amount.
                energyAmplitude += noise;
            }
            
            // Check to see if the hit time seems valid. This is done
            // by calculating the time of the next readout cycle in
            // ns and subtracting the time of the current hit (with
            // adjustment for simulation time passed) from it. If the
            // hit would fall in a previous readout cycle, something
            // is probably wrong.
            if(READOUT_PERIOD + readoutTime() - (ReadoutDataManager.getCurrentTime() + hit.getTime()) >= READOUT_PERIOD) {
                throw new RuntimeException("Error: Trying to add a hit to the analog pipeline, but the time seems incorrect.");
            }
            
            // Simulate the pulse for each position in the preamp
            // pulse buffer for the subdetector channel on which the
            // hit occurred.
            for(int i = 0; i < BUFFER_LENGTH; i++) {
                // Calculate the voltage deposition for the current
                // buffer time.
                double voltageDeposition = energyAmplitude * pulseAmplitude((i + 1) * READOUT_PERIOD + readoutTime()
                        - (ReadoutDataManager.getCurrentTime() + hit.getTime()) - getTimeShiftConditions(hit.getCellID()), hit.getCellID());
                
                // Increase the current buffer time's voltage value
                // by the calculated amount.
                voltageBuffer.addToCell(i, voltageDeposition);
            }
        }
        
        /*
         * Once the preamplifier buffers have been updated with the
         * digitized pulses generated from the truth energy data, it
         * is next necessary to integrate hits from the pulses. Hit
         * integration is only performed once per readout period. The
         * readout period, defined by the hardware, is by default 4
         * nanoseconds.
         */
        
        // Check whether the appropriate amount of time has passed to
        // perform another integration step. If so, create a list to
        // contain any newly integrated hits and perform integration.
        List<RawCalorimeterHit> newHits = null;
        List<LCRelation> newTruthRelations = null;
        while(ReadoutDataManager.getCurrentTime() - readoutTime() + ReadoutDataManager.getBeamBunchSize() >= READOUT_PERIOD) {
            if(newHits == null) { newHits = new ArrayList<RawCalorimeterHit>(); }
            if(newTruthRelations == null) { newTruthRelations = new ArrayList<LCRelation>(); }
            readHits(newHits, newTruthRelations);
            readoutCounter++;
        }
    }
    
    // TODO: Document this.
    private void readHits(List<RawCalorimeterHit> newHits, List<LCRelation> newTruthRelations) {
        // Perform hit integration as needed for each subdetector
        // channel in the buffer map.
        for(Long cellID : voltageBufferMap.keySet()) {
            // Get the preamplifier pulse buffer for the channel.
            DoubleRingBuffer voltageBuffer = voltageBufferMap.get(cellID);
            
            // Get the ADC buffer for the channel.
            IntegerRingBuffer adcBuffer = adcBufferMap.get(cellID);
            adcBuffer.stepForward();
            
            // Scale the current value of the preamplifier buffer
            // to a 12-bit ADC value where the maximum represents
            // a value of maxVolt.
            double currentValue = voltageBuffer.getValue() * ((Math.pow(2, nBit) - 1) / maxVolt);
            
            // Get the pedestal for the channel.
            int pedestal = (int) Math.round(getPedestalConditions(cellID));
            
            // An ADC value is not allowed to exceed 4095. If a
            // larger value is observed, 4096 (overflow) is given
            // instead. (This corresponds to >2 Volts.)
            int digitizedValue = Math.min((int) Math.round(pedestal + currentValue), (int) Math.pow(2, nBit));
            
            // Write this value to the ADC buffer.
            adcBuffer.setValue(digitizedValue);
            
            // Store the pedestal subtracted value so that it may
            // be checked against the integration threshold.
            int pedestalSubtractedValue = digitizedValue - pedestal;
            
            // Get the total ADC value that has been integrated
            // on this channel.
            Integer sum = channelIntegrationSumMap.get(cellID);
            
            // If any readout hits exist on this channel, add the
            // current ADC values to them.
            
            // If the ADC sum is undefined, then there is not an
            // ongoing integration. If the pedestal subtracted
            // value is also over the integration threshold, then
            // integration should be initiated.
            if(sum == null && pedestalSubtractedValue > integrationThreshold) {
                // Store the current local time in units of
                // events (4 ns). This will indicate when the
                // integration started and, in turn, should end.
                channelIntegrationTimeMap.put(cellID, readoutCounter);
                
                // Integrate the ADC values for a number of
                // samples defined by NSB and threshold
                // crossing sample. 
                int sumBefore = 0;
                for(int i = 0; i <= numSamplesBefore; i++) {
                    sumBefore += adcBuffer.getValue(-(numSamplesBefore - i));
                }
                
                // This will represent the total integral sum at
                // the current point in time. Store it in the sum
                // buffer so that it may be incremented later as
                // additional samples are read.
                channelIntegrationSumMap.put(cellID, sumBefore);
                
                // Collect and store truth information for trigger
                // path hits.
                channelIntegrationADCMap.put(cellID, new ArrayList<Integer>());
                
                // Get the truth information in the
                // integration samples for this channel.
                Set<SimCalorimeterHit> truthHits = new HashSet<SimCalorimeterHit>();
                for(int i = 0; i < numSamplesBefore + 4; i++) {
                    channelIntegrationADCMap.get(cellID).add(adcBuffer.getValue(-(numSamplesBefore - i)));
                    truthHits.addAll(truthBufferMap.get(cellID).getValue(-(numSamplesBefore - i)));
                }
                
                // Store all the truth hits that occurred in
                // the truth buffer in the integration period
                // for this channel as well. These will be
                // passed through the chain to allow for the
                // accessing of truth information during the
                // trigger simulation.
                channelIntegrationTruthMap.put(cellID, truthHits);
            }
            
            // If the integration sum is defined, then pulse
            // integration is ongoing.
            if(sum != null) {
                // Three cases are treated separataly
                // Case 1: CHANNEL_INTEGRATION_DEADTIME > numSamplesAfter
                // Case 2: CHANNEL_INTEGRATION_DEADTIME == numSamplesAfter 
                // Case 3: CHANNEL_INTEGRATION_DEADTIME < numSamplesAfter
                if(CHANNEL_INTEGRATION_DEADTIME > numSamplesAfter) { // Case 1
                    //Continue integration until NSA, the threshold-crossing sample has been added before.
                    if (channelIntegrationTimeMap.get(cellID) + numSamplesAfter - 1 >= readoutCounter) { 
                        channelIntegrationADCMap.get(cellID).add(adcBuffer.getValue(0));

                        // Add the new ADC sample.
                        channelIntegrationSumMap.put(cellID, sum + adcBuffer.getValue(0));

                        // Add the new truth information, if trigger
                        // path truth output is enabled.
                        if (writeTriggerTruth) {
                            channelIntegrationTruthMap.get(cellID).addAll(truthBufferMap.get(cellID).getValue(0));
                        }
                    }

                    // If integration is complete, a hit may be added
                    // to data manager.
                    else if (channelIntegrationTimeMap.get(cellID) + numSamplesAfter - 1 == readoutCounter - 1) {//At NSA + 1, hit is added into data manager
                        // Add a new calorimeter hit.
                        RawCalorimeterHit newHit = new BaseRawCalorimeterHit(cellID, sum,
                                64 * channelIntegrationTimeMap.get(cellID));
                        newHits.add(newHit);
                        // Cycle-clock for events is 2 ns, while cycle-clock for samples is 4 ns                        
                        integrationTime = channelIntegrationTimeMap.get(cellID) * 4 + 2;
                        // Add the truth relations for this hit, if
                        // trigger path truth is enabled.
                        if (writeTriggerTruth) {
                            Set<SimCalorimeterHit> truthHits = channelIntegrationTruthMap.get(cellID);
                            for (SimCalorimeterHit truthHit : truthHits) {
                                newTruthRelations.add(new BaseLCRelation(newHit, truthHit));
                            }
                        }
                    }

                    // Do not clear the channel for integration until  deadtime has passed.
                    // The threshold-crossing sample counts as the first sample in the deadtime.
                    else if (channelIntegrationTimeMap.get(cellID) + CHANNEL_INTEGRATION_DEADTIME - 1 <= readoutCounter
                            - 1) { // No new integration until over deadtime
                        channelIntegrationSumMap.remove(cellID);
                    }
                } // Case 1 ends
                else if(CHANNEL_INTEGRATION_DEADTIME == numSamplesAfter){ // Case 2
                    // Continue integration until NSA, the threshold-crossing sample has been added before.
                    if (channelIntegrationTimeMap.get(cellID) + numSamplesAfter - 1 >= readoutCounter) {
                        channelIntegrationADCMap.get(cellID).add(adcBuffer.getValue(0));

                        // Add the new ADC sample.
                        channelIntegrationSumMap.put(cellID, sum + adcBuffer.getValue(0));

                        // Add the new truth information, if trigger
                        // path truth output is enabled.
                        if (writeTriggerTruth) {
                            channelIntegrationTruthMap.get(cellID).addAll(truthBufferMap.get(cellID).getValue(0));
                        }
                    }  
                    // If integration is complete, a hit may be added
                    // to data manager.
                    else if (channelIntegrationTimeMap.get(cellID) + numSamplesAfter - 1 == readoutCounter - 1) {//At NSA + 1, hit is added into data manager
                        // Add a new calorimeter hit.
                        RawCalorimeterHit newHit = new BaseRawCalorimeterHit(cellID, sum,
                                64 * channelIntegrationTimeMap.get(cellID));
                        newHits.add(newHit);
                        // Cycle-clock for events is 2 ns, while cycle-clock for samples is 4 ns 
                        integrationTime = channelIntegrationTimeMap.get(cellID) * 4 + 2;

                        // Add the truth relations for this hit, if
                        // trigger path truth is enabled.
                        if (writeTriggerTruth) {
                            Set<SimCalorimeterHit> truthHits = channelIntegrationTruthMap.get(cellID);
                            for (SimCalorimeterHit truthHit : truthHits) {
                                newTruthRelations.add(new BaseLCRelation(newHit, truthHit));
                            }
                        }
                        channelIntegrationSumMap.remove(cellID);
                    }
                } // Case 2 ends
                else { // Case 3
                    if (channelIntegrationTimeMap.get(cellID) + CHANNEL_INTEGRATION_DEADTIME - 1 >= readoutCounter) {
                        // Continue integration until CHANNEL_INTEGRATION_DEADTIME
                        channelIntegrationADCMap.get(cellID).add(adcBuffer.getValue(0));

                        // Add the new ADC sample.
                        channelIntegrationSumMap.put(cellID, sum + adcBuffer.getValue(0));

                        // Add the new truth information, if trigger
                        // path truth output is enabled.
                        if (writeTriggerTruth) {
                            channelIntegrationTruthMap.get(cellID).addAll(truthBufferMap.get(cellID).getValue(0));
                        }
                        
                        // If sample at the end of deadtime is less than threshold, new integration could be started from next sample
                        if(channelIntegrationTimeMap.get(cellID) + CHANNEL_INTEGRATION_DEADTIME == readoutCounter && pedestalSubtractedValue <= integrationThreshold)                            
                            flagStartNewIntegration.put(cellID, true);                           
                    }  
                    else if (channelIntegrationTimeMap.get(cellID) + numSamplesAfter - 1 >= readoutCounter) {
                        if(flagStartNewIntegration.get(cellID) == true) { // Flag for previous sample is true
                            if(pedestalSubtractedValue <= integrationThreshold) { // If sample is less than threshold, then do not start new integration
                                channelIntegrationADCMap.get(cellID).add(adcBuffer.getValue(0));

                                // Add the new ADC sample.
                                channelIntegrationSumMap.put(cellID, sum + adcBuffer.getValue(0));

                                // Add the new truth information, if trigger
                                // path truth output is enabled.
                                if (writeTriggerTruth) {
                                    channelIntegrationTruthMap.get(cellID).addAll(truthBufferMap.get(cellID).getValue(0));
                                }                                
                            }
                            else { // if sample is larger than threshold, a hit is added into data manager and start new integration
                                // Add a new calorimeter hit.
                                RawCalorimeterHit newHit = new BaseRawCalorimeterHit(cellID, sum,
                                        64 * channelIntegrationTimeMap.get(cellID));
                                newHits.add(newHit);
                                integrationTime = channelIntegrationTimeMap.get(cellID) * 4 + 2;

                                // Add the truth relations for this hit, if
                                // trigger path truth is enabled.
                                if (writeTriggerTruth) {
                                    Set<SimCalorimeterHit> truthHits = channelIntegrationTruthMap.get(cellID);
                                    for (SimCalorimeterHit truthHit : truthHits) {
                                        newTruthRelations.add(new BaseLCRelation(newHit, truthHit));
                                    }
                                }                                                                
            
                                //Start new integration
                                channelIntegrationTimeMap.put(cellID, readoutCounter);
                                flagStartNewIntegration.put(cellID, false);
                                
                                // Integrate the ADC values for a number of
                                // samples defined by NSB from before threshold
                                // crossing. Note that this stops one sample
                                // before the current sample. This current sample
                                // is handled in the subsequent code block.
                                int sumBefore = 0;
                                for(int i = 0; i <= numSamplesBefore; i++) {
                                    sumBefore += adcBuffer.getValue(-(numSamplesBefore - i));
                                }
                                
                                // This will represent the total integral sum at
                                // the current point in time. Store it in the sum
                                // buffer so that it may be incremented later as
                                // additional samples are read.
                                channelIntegrationSumMap.put(cellID, sumBefore);
                                
                                // Collect and store truth information for trigger
                                // path hits.
                                channelIntegrationADCMap.put(cellID, new ArrayList<Integer>());
                                
                                // Get the truth information in the
                                // integration samples for this channel.
                                Set<SimCalorimeterHit> truthHits = new HashSet<SimCalorimeterHit>();
                                for(int i = 0; i < numSamplesBefore + 4; i++) {
                                    channelIntegrationADCMap.get(cellID).add(adcBuffer.getValue(-(numSamplesBefore - i)));
                                    truthHits.addAll(truthBufferMap.get(cellID).getValue(-(numSamplesBefore - i)));
                                }
                                
                                // Store all the truth hits that occurred in
                                // the truth buffer in the integration period
                                // for this channel as well. These will be
                                // passed through the chain to allow for the
                                // accessing of truth information during the
                                // trigger simulation.
                                channelIntegrationTruthMap.put(cellID, truthHits);                                                                    
                            }                                                          
                        }
                        else { // Flag for previous sample is false
                            channelIntegrationADCMap.get(cellID).add(adcBuffer.getValue(0));

                            // Add the new ADC sample.
                            channelIntegrationSumMap.put(cellID, sum + adcBuffer.getValue(0));

                            // Add the new truth information, if trigger
                            // path truth output is enabled.
                            if (writeTriggerTruth) {
                                channelIntegrationTruthMap.get(cellID).addAll(truthBufferMap.get(cellID).getValue(0));
                            }
                            if(pedestalSubtractedValue <= integrationThreshold)
                                flagStartNewIntegration.put(cellID, true);                                                       
                        }  
                    }
                    else if (channelIntegrationTimeMap.get(cellID) + numSamplesAfter - 1 == readoutCounter - 1) {//If reach NSA + 1, hit is added into data manager, and flag is set as false
                        // Add a new calorimeter hit.
                        RawCalorimeterHit newHit = new BaseRawCalorimeterHit(cellID, sum,
                                64 * channelIntegrationTimeMap.get(cellID));
                        newHits.add(newHit);
                        integrationTime = channelIntegrationTimeMap.get(cellID) * 4 + 2;

                        // Add the truth relations for this hit, if
                        // trigger path truth is enabled.
                        if (writeTriggerTruth) {
                            Set<SimCalorimeterHit> truthHits = channelIntegrationTruthMap.get(cellID);
                            for (SimCalorimeterHit truthHit : truthHits) {
                                newTruthRelations.add(new BaseLCRelation(newHit, truthHit));
                            }
                        }
                        channelIntegrationSumMap.remove(cellID);
                        flagStartNewIntegration.put(cellID, false);
                    }
                } // Case 3 ends                 
            }
            
            // Step to the next entry in the voltage buffer.
            voltageBuffer.clearValue();
            voltageBuffer.stepForward();
            
            // Step the truth buffer for this channel forward.
            // The new cell should be cleared of any old values.
            truthBufferMap.get(cellID).stepForward();
            truthBufferMap.get(cellID).clearValue();
        }
        
        // Write the trigger path output data to the readout data
        // manager. Truth data is optional.
        
    
        ReadoutDataManager.addData(outputHitCollectionName, integrationTime, newHits, RawCalorimeterHit.class);
        if(writeTriggerTruth) {
            ReadoutDataManager.addData(triggerTruthRelationsCollectionName, integrationTime, newTruthRelations, LCRelation.class);
        }
    }
    
    /**
     * Finds all root particles associated with the interactions that
     * created the argument particle.
     * @param particle - The particle.
     * @return Returns a {@link java.util.List List} containing each
     * particle object in the argument particle's particle tree which
     * has no parent particle.
     */
    private static final List<MCParticle> getRootParticleList(MCParticle particle) {
        // If the particle has no parents, it should be added to the
        // list and the list returned.
        if(particle.getParents().isEmpty()) {
            List<MCParticle> list = new ArrayList<MCParticle>(1);
            list.add(particle);
            return list;
        }
        
        // If there is only one parent, just return the results from
        // that parent.
        else if(particle.getParents().size() == 1) {
            return getRootParticleList(particle.getParents().get(0));
        }
        
        // Otherwise, run the method on each parent particle and
        // return the results from that instead.
        else {
            // Store the parent particle roots.
            List<MCParticle> list = new ArrayList<MCParticle>();
            
            // Get the root particles for each parent and add them to
            // the list.
            for(MCParticle parent : particle.getParents()) {
                List<MCParticle> parentParticles = getRootParticleList(parent);
                list.addAll(parentParticles);
            }
            
            // Return the compiled particle list.
            return list;
        }
    }
    
    /**
     * Flattens the particle tree to a set containing both the root
     * particle and any particles that are descended from it.
     * @param root - The root of the particle tree.
     * @return Returns a set containing the argument particle and all
     * of its descendants.
     */
    private static final Set<MCParticle> getParticleTreeAsSet(MCParticle root) {
        // Create a set to store the particle tree.
        Set<MCParticle> particleSet = new HashSet<MCParticle>();
        
        // Add the root particle to the tree, and then recursively
        // add any daughter particles to the tree.
        particleSet.add(root);
        addDaughtersToSet(root, particleSet);
        
        // Return the particle set.
        return particleSet;
    }
    
    /**
     * Adds all the daughter particles of the argument to the set.
     * Daughters of each daughter particle are then recursively added
     * to the set as well.
     * @param particle - The particle to add.
     * @param set - The set to which to add the particle.
     */
    private static final void addDaughtersToSet(MCParticle particle, Set<MCParticle> set) {
        // Add each daughter particle to the set, and recursively add
        // its daughters as well.
        for(MCParticle daughter : particle.getDaughters()) {
            set.add(daughter);
            addDaughtersToSet(daughter, set);
        }
    }
    
    /**
     * Gets a {@link java.util.Set Set} containing all valid channel
     * IDs for the relevant subdetector geometry.
     * @return Returns a <code>Set</code> containing all possible
     * channel IDs.
     */
    protected abstract Set<Long> getChannelIDs();
    
    /**
     * Gets the gain for the indicated subdetector channel.
     * @param channelID - The channel ID.
     * @return Returns the value of the gain in units of ADC/MeV as a
     * <code>double</code>.
     */
    protected abstract double getGainConditions(long channelID);
    
    /**
     * Gets the noise sigma for the indicated subdetector channel.
     * @param channelID - The channel ID.
     * @return Returns the value of the noise sigma as a
     * <code>double</code>.
     */
    protected abstract double getNoiseConditions(long channelID);
    
    /**
     * Gets the <code>int</code> flag used to denote the appropriate
     * subdetector in relation to a readout timestamp.
     * @return Returns the timestamp flag as an <code>int</code>.
     */
    protected abstract int getTimestampFlag();
    
    @Override
    protected Collection<TriggeredLCIOData<?>> getOnTriggerData(double triggerTime) {
        // Create a list to store the extra collections.
        List<TriggeredLCIOData<?>> collectionsList = null;
        if(writeTruth) {
            collectionsList = new ArrayList<TriggeredLCIOData<?>>(5);
        } else {
            collectionsList = new ArrayList<TriggeredLCIOData<?>>(2);
        }
        
        // Readout drivers need to produce readout timestamps to
        // specify when they occurred in terms of simulation time.
        // The readout timestamp for the subdetector data should be
        // defined as the start simulation time of the ADC buffer.
        ReadoutTimestamp timestamp = new ReadoutTimestamp(getTimestampFlag(), triggerTime - (readoutOffset * 4) + 4);
        
        // Make the readout timestamp collection parameters object.
        LCIOCollectionFactory.setCollectionName(ReadoutTimestamp.collectionName);
        LCIOCollection<ReadoutTimestamp> timestampCollection = LCIOCollectionFactory.produceLCIOCollection(ReadoutTimestamp.class);
        TriggeredLCIOData<ReadoutTimestamp> timestampData = new TriggeredLCIOData<ReadoutTimestamp>(timestampCollection);
        timestampData.getData().add(timestamp);
        collectionsList.add(timestampData);
        
        // Instantiate some lists to store truth data, if truth is to
        // be output.
        List<SimCalorimeterHit> triggerTruthHits = null;
        List<LCRelation> triggerTruthRelations = null;
        if(writeTruth) {
            triggerTruthHits = new ArrayList<SimCalorimeterHit>();
            triggerTruthRelations = new ArrayList<LCRelation>();
        }
        
        // Get the appropriate collection of readout hits and output
        // them to the readout data manager.
        if(mode == 7) {
            List<RawCalorimeterHit> readoutHits = getMode7Hits(triggerTime);
            TriggeredLCIOData<RawCalorimeterHit> readoutData = new TriggeredLCIOData<RawCalorimeterHit>(mode7HitCollectionParams);
            readoutData.getData().addAll(readoutHits);
            collectionsList.add(readoutData);
        } else {
            List<RawTrackerHit> readoutHits = null;
            if(mode == 1) { readoutHits = getMode1Hits(triggerTime); }
            else { readoutHits = getMode3Hits(triggerTime); }
            TriggeredLCIOData<RawTrackerHit> readoutData = new TriggeredLCIOData<RawTrackerHit>(mode13HitCollectionParams);
            readoutData.getData().addAll(readoutHits);
            collectionsList.add(readoutData);
            
            // FIXME: Truth information is currently only supported for Mode-1 operation.
            if(writeTruth && mode == 1) {
                for(RawTrackerHit hit : readoutHits) {
                    Collection<SimCalorimeterHit> truthHits = getTriggerTruthValues(hit.getCellID(), triggerTime);
                    triggerTruthHits.addAll(truthHits);
                    for(CalorimeterHit truthHit : truthHits) {
                        triggerTruthRelations.add(new BaseLCRelation(hit, truthHit));
                    }
                }
            }
        }
        
        // Add the truth collections if they exist.
        if(writeTruth) {
            // Add the truth hits to the output collection.
            LCIOCollection<SimCalorimeterHit> truthHitCollection = ReadoutDataManager.getCollectionParameters(truthHitCollectionName, SimCalorimeterHit.class);
            TriggeredLCIOData<SimCalorimeterHit> truthData = new TriggeredLCIOData<SimCalorimeterHit>(truthHitCollection);
            truthData.getData().addAll(triggerTruthHits);
            collectionsList.add(truthData);
            
            // MC particles need to be extracted from the truth hits
            // and included in the readout data to ensure that the
            // full truth chain is available.
            Set<MCParticle> truthParticles = new java.util.HashSet<MCParticle>();
            for(SimCalorimeterHit simHit : triggerTruthHits) {
                for(int i = 0; i < simHit.getMCParticleCount(); i++) {
                    List<MCParticle> rootParticles = getRootParticleList(simHit.getMCParticle(i));
                    for(MCParticle rootParticle : rootParticles) {
                        truthParticles.addAll(getParticleTreeAsSet(rootParticle));
                    }
                }
            }
            
            // Create the truth MC particle collection.
            LCIOCollection<MCParticle> truthParticleCollection = ReadoutDataManager.getCollectionParameters("MCParticle", MCParticle.class);
            TriggeredLCIOData<MCParticle> truthParticleData = new TriggeredLCIOData<MCParticle>(truthParticleCollection);
            truthParticleData.getData().addAll(truthParticles);
            collectionsList.add(truthParticleData);
            
            // Add the truth relations to the output data.
            TriggeredLCIOData<LCRelation> truthRelations = new TriggeredLCIOData<LCRelation>(truthRelationsCollectionParams);
            truthRelations.getData().addAll(triggerTruthRelations);
            collectionsList.add(truthRelations);
        }
        
        // Return the extra trigger collections.
        return collectionsList;
    }
    
    /**
     * Gets the pedestal for the indicated subdetector channel.
     * @param channelID - The channel ID.
     * @return Returns the value of the pedestal in units of ADC as a
     * <code>double</code>.
     */
    protected abstract double getPedestalConditions(long channelID);
    
    @Override
    protected boolean isPersistent() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    protected double getReadoutWindowAfter() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    protected double getReadoutWindowBefore() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    protected double getTimeDisplacement() {
        return localTimeOffset;
    }
    
    @Override
    protected double getTimeNeededForLocalOutput() {
        return (readoutWindow - readoutOffset) * 4.0;
    }
    
    /**
     * Gets the time shift for the indicated subdetector channel.
     * @param channelID - The channel ID.
     * @return Returns the value of the time shift in units of ns as
     * a <code>double</code>.
     */
    protected abstract double getTimeShiftConditions(long channelID);
    
    /**
     * Gets the subdetector geometry object.
     * @return Returns the subdetector geometry object. This will be
     * an object of parameterized type <code>D</code>, which will is
     * a subclass of {@link org.lcsim.geometry.compact.Subdetector
     * Subdetector}.
     */
    protected D getSubdetector() {
        return geometry;
    }
    
    /**
     * Clones an object of type {@link org.lcsim.event.CalorimeterHit
     * CalorimeterHit} and returns a copy that is shifted in time by
     * the specified amount.
     * @param hit - The hit to clone.
     * @param newTime - The new time for the hit.
     * @return Returns a time-shifted hit as an object of type {@link
     * org.lcsim.event.CalorimeterHit CalorimeterHit}, unless the
     * input hit was a {@link org.lcsim.event.SimCalorimeterHit
     * SimCalorimeterHit} object, in which case the truth information
     * will be retained.
     */
    private static final CalorimeterHit cloneHitToTime(CalorimeterHit hit, double newTime) {
        if(hit instanceof SimCalorimeterHit) {
            // Cast the hit to a simulated calorimeter hit.
            SimCalorimeterHit simHit = (SimCalorimeterHit) hit;
            
            // Create the necessary data objects to clone the
            // hit.
            int[] pdgs = new int[simHit.getMCParticleCount()];
            float[] times = new float[simHit.getMCParticleCount()];
            float[] energies = new float[simHit.getMCParticleCount()];
            Object[] particles = new Object[simHit.getMCParticleCount()];
            for(int i = 0; i < simHit.getMCParticleCount(); i++) {
                particles[i] = simHit.getMCParticle(i);
                pdgs[i] = simHit.getMCParticle(i).getPDGID();
                
                // Note -- Despite returning the value for these
                // methods as a double, they are actually stored
                // internally as floats, so this case is always safe.
                // Note -- Hit times are calculated based on the time
                // of each of the contributing truth particles. This
                // means that we have to give a fake truth time to
                // actually get the correct hit time.
                times[i] = (float) newTime;
                energies[i] = (float) simHit.getContributedEnergy(i);
            }
            
            // Create the new hit and shift its time position.
            BaseSimCalorimeterHit cloneHit = new BaseSimCalorimeterHit(simHit.getCellID(), simHit.getRawEnergy(), newTime,
                    particles, energies, times, pdgs, simHit.getMetaData());
            
            // Return the hit.
            return cloneHit;
        } else {
            return new BaseCalorimeterHit(hit.getRawEnergy(), hit.getCorrectedEnergy(), hit.getEnergyError(), newTime,
                    hit.getCellID(), hit.getPositionVec(), hit.getType(), hit.getMetaData());
        }
    }
    
    /**
     * Gets the value of the pulse-shape Guassian function for the
     * given parameters.
     * @param t
     * @param sig
     * @return Returns the value of the function as a
     * <code>double</code>.
     */
    private static final double funcGaus(double t, double sig) {
        return Math.exp(-t * t / (2 * sig * sig));
    }
    
    /**
     * Generates the hits which should be output for a given trigger
     * time in Mode-1 format.
     * @param triggerTime - The trigger time.
     * @return Returns the readout hits for the given trigger time as
     * Mode-1 hits.
     */
    private List<RawTrackerHit> getMode1Hits(double triggerTime) {
        // Create a list to store the Mode-1 hits.
        List<RawTrackerHit> hits = new ArrayList<RawTrackerHit>();
        
        // Iterate over each channel.
        for(Long cellID : adcBufferMap.keySet()) {
            // Get the ADC values at the time of the trigger.
            short[] adcValues = getTriggerADCValues(cellID, triggerTime);
            
            // Iterate across the ADC values. If the ADC value is
            // sufficiently high to produce a hit, then it should be
            // written out.
            boolean isAboveThreshold = false;
            for(int i = 0; i < adcValues.length; i++) {
                // Check that there is a threshold-crossing at some
                // point in the ADC buffer.
                if(adcValues[i] > getPedestalConditions(cellID) + integrationThreshold) {
                    isAboveThreshold = true;
                    break;
                }
            }
            
            // If so, create a new hit and add it to the list.
            if(isAboveThreshold) {
                hits.add(new BaseRawTrackerHit(cellID, 0, adcValues));
            }
        }
        
        // Return the hits.
        return hits;
    }
    
    /**
     * Generates the hits which should be output for a given trigger
     * time in Mode-3 format.
     * @param triggerTime - The trigger time.
     * @return Returns the readout hits for the given trigger time as
     * Mode-3 hits.
     */
    private List<RawTrackerHit> getMode3Hits(double triggerTime) {
        // Create a list to store the Mode-3 hits.
        List<RawTrackerHit> hits = new ArrayList<RawTrackerHit>();
        
        // Iterate across the ADC values and extract Mode-3 hits.
        for(Long cellID : adcBufferMap.keySet()) {
            int pointerOffset = 0;
            int numSamplesToRead = 0;
            int thresholdCrossing = 0;
            short[] adcValues = null;
            short[] window = getTriggerADCValues(cellID, triggerTime);
            
            for(int i = 0; i < ReadoutDataManager.getReadoutWindow(); i++) {
                if(numSamplesToRead != 0) {
                    adcValues[adcValues.length - numSamplesToRead] = window[i - pointerOffset];
                    numSamplesToRead--;
                    if (numSamplesToRead == 0) {
                        hits.add(new BaseRawTrackerHit(cellID, thresholdCrossing, adcValues));
                    }
                } else if ((i == 0 || window[i - 1] <= getPedestalConditions(cellID) + integrationThreshold) && window[i]
                        > getPedestalConditions(cellID) + integrationThreshold) {
                    thresholdCrossing = i;
                    pointerOffset = Math.min(numSamplesBefore, i);
                    numSamplesToRead = pointerOffset + Math.min(numSamplesAfter, ReadoutDataManager.getReadoutWindow() - i - pointerOffset - 1);
                    adcValues = new short[numSamplesToRead];
                }
            }
        }
        
        // Return the hits.
        return hits;
    }
    
    /**
     * Generates the hits which should be output for a given trigger
     * time in Mode-7 format.
     * @param triggerTime - The trigger time.
     * @return Returns the readout hits for the given trigger time as
     * Mode-7 hits.
     */
    private List<RawCalorimeterHit> getMode7Hits(double triggerTime) {
        // Create a list to store the Mode-7 hits.
        List<RawCalorimeterHit> hits = new ArrayList<RawCalorimeterHit>();
        
        // Iterate across the ADC values and extract Mode-7 hits.
        for(Long cellID : adcBufferMap.keySet()) {
            int adcSum = 0;
            int pointerOffset = 0;
            int numSamplesToRead = 0;
            int thresholdCrossing = 0;
            short[] window = getTriggerADCValues(cellID, triggerTime);
            
            // Generate Mode-7 hits.
            if(window != null) {
                for(int i = 0; i < ReadoutDataManager.getReadoutWindow(); i++) {
                    if (numSamplesToRead != 0) {
                        adcSum += window[i - pointerOffset];
                        numSamplesToRead--;
                        if(numSamplesToRead == 0) {
                            hits.add(new BaseRawCalorimeterHit(cellID, adcSum, 64 * thresholdCrossing));
                        }
                    } else if((i == 0 || window[i - 1] <= getPedestalConditions(cellID) + integrationThreshold)
                            && window[i] > getPedestalConditions(cellID) + integrationThreshold) {
                        thresholdCrossing = i;
                        pointerOffset = Math.min(numSamplesBefore, i);
                        numSamplesToRead = pointerOffset + Math.min(numSamplesAfter, ReadoutDataManager.getReadoutWindow() - i - pointerOffset - 1);
                        adcSum = 0;
                    }
                }
            }
        }
        
        // Return the hits.
        return hits;
    }
    
    private int getReadoutLatency(double triggerTime) {
        return ((int) ((ReadoutDataManager.getCurrentTime() - triggerTime) / 4.0)) + readoutOffset;
    }
    
    /**
     * Gets the ADC values for the trigger readout window for the
     * requested cell ID and returns them as a <code>short</code>
     * primitive array.
     * @param cellID - The ID for the channel of the requested ADC
     * value array.
     * @param triggerTime - The time of the trigger to be written.
     * @return Returns the ADC values in a time range equal to the
     * readout window positioned around the trigger time as array of
     * <code>short</code> primitives.
     */
    private short[] getTriggerADCValues(long cellID, double triggerTime) {
        // Calculate the offset between the current position and the
        // trigger time.
        int readoutLatency = getReadoutLatency(triggerTime);
        
        // Get the ADC pipeline.
        IntegerRingBuffer pipeline = adcBufferMap.get(cellID);
        
        // Extract the ADC values for the requested channel.
        short[] adcValues = new short[readoutWindow];
        for(int i = 0; i < readoutWindow; i++) {
            adcValues[i] = (short) pipeline.getValue(-(readoutLatency - i - 1)).intValue();
        }
        
        // Return the result.
        return adcValues;
    }
    
    /**
     * Gets a list of all truth hits that occurred in the ADC output
     * window around a given trigger time from the truth buffer.
     * @param cellID - The channel ID.
     * @param triggerTime - The trigger time.
     * @return Returns all truth hits that occurred within the ADC
     * readout window around the trigger time for the specified
     * channel.
     */
    private Collection<SimCalorimeterHit> getTriggerTruthValues(long cellID, double triggerTime) {
        // Calculate the offset between the current position and the
        // trigger time.
        int readoutLatency = getReadoutLatency(triggerTime);
        
        // Get the truth pipeline.
        ObjectRingBuffer<SimCalorimeterHit> pipeline = truthBufferMap.get(cellID);
        
        // Extract the truth for the requested channel. Note that one
        // extra sample is included over the range of ADC samples as
        // sometimes, the truth hit occurs a little earlier than may
        // be expected due to a delay from pulse propagation.
        double baseHitTime = 0;
        List<SimCalorimeterHit> channelHits = new ArrayList<SimCalorimeterHit>();
        for(int i = 0; i < readoutWindow + 4; i++) {
            // Hit times should be specified with respect to the
            // start of the readout window.
            for(SimCalorimeterHit hit : pipeline.getValue(-(readoutLatency - i))) {
                channelHits.add((SimCalorimeterHit) cloneHitToTime(hit, baseHitTime));
            }
            
            // Increment the base hit time.
            baseHitTime += 4.0;
        }
        
        // Return the result.
        return channelHits;
    }
    
    /**
     * Returns pulse amplitude at the given time (relative to hit time). Gain is
     * applied.
     *
     * @param time Units of ns. Relative to hit time (negative=before the start
     * of the pulse).
     * @param cellID Crystal ID as returned by hit.getCellID().
     * @return Amplitude, units of volts/GeV.
     */
    private double pulseAmplitude(double time, long cellID) {
        //normalization constant from cal gain (MeV/integral bit) to amplitude gain (amplitude bit/GeV)
        // Determine the gain. Gain may either be fixed across all
        // channels, or be obtained from the conditions database
        // depending on the behavior defined in the steering file.
        // The gain should also be normalized.
        double gain;
        if(fixedGain > 0) {
            gain = READOUT_PERIOD / (fixedGain * EcalUtils.MeV * ((Math.pow(2, nBit) - 1) / maxVolt));
        } else {
            gain = READOUT_PERIOD / (getGainConditions(cellID) * EcalUtils.MeV * ((Math.pow(2, nBit) - 1) / maxVolt));
        }
        
        // Calculate the correct pulse amplitude and return it.
        return gain * pulseAmplitude(time, pulseShape, tp);
    }
    
    /**
     * Calculates the amplitude of a pulse at the given time, where
     * the time is relative to the hit time, and for a given pulse
     * shape.
     * @param time - The time in the pulse. This is in units of ns
     * and is relative to the hit time. A negative value represents
     * the pulse shape before the hit occurs.
     * @param shape - The type of pulse for which the calculation is
     * to be performed.
     * @param shapingTime - A fitting parameter that influences the
     * shape of the pulse.
     * @return Returns the pulse amplitude in units of inverse ns.
     * The amplitude is normalized so that the pulse integral is one.
     */
    private static final double pulseAmplitude(double time, PulseShape shape, double shapingTime) {
        // There can not be a pulse response from a hit that has not
        // occurred yet, so any time before zero must produce a pulse
        // amplitude of zero as well.
        if(time <= 0.0) {
            return 0.0;
        }
        
        // Perform the calculation appropriate to the specified pulse
        // shape.
        switch (shape) {
            case CRRC:
                // Peak Location: tp
                // Peak Value:    1/(tp * e)
                return ((time / (shapingTime * shapingTime)) * Math.exp(-time / shapingTime));
            case DoubleGaussian:
                // According to measurements, the output signal can
                // be fitted by two Gaussians: one for the rise of
                // the signal and one for the fall.
                // Peak Location: 3 * riseTime
                // Peak Value:    1/norm
                double norm = ((riseTime + fallTime) / 2) * Math.sqrt(2 * Math.PI); //to ensure the total integral is equal to 1: = 33.8
                return funcGaus(time - 3 * riseTime, (time < 3 * riseTime) ? riseTime : fallTime) / norm;
            case ThreePole:
                // Peak Location: 2 * tp
                // Peak Value:    2/(tp * e^2)
                return ((time * time / (2 * shapingTime * shapingTime * shapingTime)) * Math.exp(-time / shapingTime));
            default:
                return 0.0;
        }
    }
    
    /**
     * Gets the local time for this driver.
     * @return Returns the local time for this driver.
     */
    private double readoutTime() {
        return readoutCounter * READOUT_PERIOD;
    }
    
    /**
     * Resets the driver buffers to their default values.
     * @return Returns <code>true</code> if the buffers were reset
     * successfully, and <code>false</code> if they were not.
     */
    private void resetBuffers() {
        // Reset each of the buffer maps.
        adcBufferMap.clear();
        truthBufferMap.clear();
        voltageBufferMap.clear();
        
        // Get the set of all possible channel IDs.
        Set<Long> cells = getChannelIDs();
        
        // Insert a new buffer for each channel ID.
        for(Long cellID : cells) {
            voltageBufferMap.put(cellID, new DoubleRingBuffer(BUFFER_LENGTH));
            truthBufferMap.put(cellID, new ObjectRingBuffer<SimCalorimeterHit>(PIPELINE_LENGTH));
            adcBufferMap.put(cellID, new IntegerRingBuffer(PIPELINE_LENGTH, (int) Math.round(getPedestalConditions(cellID))));
            
            truthBufferMap.get(cellID).stepForward();
            
            flagStartNewIntegration.put(cellID, false);
        }
    }
    
    /**
     * Sets whether randomized noise should be added to SLIC truth
     * energy depositions when simulating subdetector hits. This is
     * <code>true</code> by default.
     * @param state - <code>true</code> means that noise will be
     * added and <code>false</code> that it will not.
     */
    public void setAddNoise(boolean state) {
        addNoise = state;
    }
    
    /**
     * Defines the name of the subdetector geometry object.
     * @param ecalName - The subdetector name.
     */
    public void setGeometryName(String value) {
        geometryName = value;
    }
    
    /**
     * Sets a single uniform value for the gain on all channels. This
     * will override the conditions database value. If set negative,
     * the conditions database values will be used instead. Gains are
     * defined in units of MeV/ADC. This defaults to <code>-1</code>.
     * @param value - The uniform gain to be employed across all
     * channels in units of MeV/ADC. A negative value indicates to
     * use the conditions database values.
     */
    public void setFixedGain(double value) {
        fixedGain = value;
    }
    
    /**
     * Sets the threshold that a pulse sample must exceed before
     * pulse integration may commence. Units are in ADC and the
     * default value is 12 ADC.
     * @param value - The pulse integration threshold, in units of
     * ADC.
     */
    public void setIntegrationThreshold(int value) {
        integrationThreshold = value;
    }
    
    /**
     * Sets the name of the input truth his collection name.
     * @param collection - The collection name.
     */
    public void setInputHitCollectionName(String collection) {
        truthHitCollectionName = collection;
    }
    
    /**
     * Sets the operational mode of the simulation. This affects the
     * form of the readout hit output. Mode may be set to the values
     * 1, 3, or 7.
     * @param value - The operational mode.
     */
    public void setMode(int value) {
        mode = value;
    }
    
    /**
     * Defines the number of samples from after a threshold-crossing
     * pulse sample that should be included in the pulse integral.
     * Units are in clock-cycles (4 ns samples) and the default value
     * is 20 samples.
     * @param value - The number of samples.
     */
    public void setNumberSamplesAfter(int value) {
        numSamplesAfter = value;
    }
    
    /**
     * Defines the number of samples from before a threshold-crossing
     * pulse sample that should be included in the pulse integral.
     * Units are in clock-cycles (4 ns samples) and the default value
     * is 5 samples.
     * @param value - The number of samples.
     */
    public void setNumberSamplesBefore(int value) {
        numSamplesBefore = value;
    }
    
    /**
     * Sets the name of the hits produced by this driver for use in
     * the trigger simulation.<br/><br/>
     * Note that this is not the name of the collection output when a
     * trigger occurs. For this value, see the method {@link
     * org.hps.readout.ecal.updated.DigitizationReadoutDriver#setReadoutHitCollectionName(String)
     * setReadoutHitCollectionName(String)} instead.
     * @param collection - The collection name.
     */
    public void setOutputHitCollectionName(String collection) {
        outputHitCollectionName = collection;
    }
    
    @Override
    public void setPersistent(boolean state) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Sets the number of photoelectrons per MeV of deposited energy.
     * This value is used in the simulation of subdetector hit noise
     * due to photoelectron statistics.
     * @param value - The number of photoelectrons per MeV.
     */
    public void setPhotoelectronsPerMeV(double value) {
        pePerMeV = value;
    }
    
    /**
     * Sets the pulse-shape model used to simulate pre-amplifier
     * pulses. The default value is <code>ThreePole</code>.
     * @param pulseShape - The name of the pulse shape model that is
     * to be employed. Valid options are <code>ThreePole</code>,
     * <code>DoubleGaussian</code>, or <code>CRRC</code>.
     */
    public void setPulseShape(String pulseShape) {
        this.pulseShape = PulseShape.valueOf(pulseShape);
    }
    
    /**
     * Sets the shaper time parameter for pulse simulation. The value
     * depends on the pulse shape selected. For the default pulse
     * shape <code>ThreePole</code>, it is equal to the RC, or half
     * the peaking time (9.6 ns).
     * @param value The pulse time parameter in units of nanoseconds.
     */
    public void setPulseTimeParameter(double value) {
        tp = value;
    }
    
    /**
     * Sets the name of the triggered hit output collection. This
     * collection will hold all hits produced when a trigger occurs.
     * <br/><br/>
     * Note that this collection is different from the hits produced
     * for internal usage by the readout simulation.  For this value,
     * see the method {@link
     * org.hps.readout.ecal.updated.DigitizationReadoutDriver#setOutputHitCollectionName(String)
     * setOutputHitCollectionName(String)} instead.
     * @param collection - The collection name.
     */
    public void setReadoutHitCollectionName(String collection) {
        readoutCollectionName = collection;
    }
    
    /**
     * Sets the number of samples by which readout hit pulse-crossing
     * samples should be offset. Units are in clock-cycles (intervals
     * of 4 ns).
     * @param value - The offset of the pulse-crossing sample in
     * units of clock-cycles (4 ns intervals).
     */
    public void setReadoutOffset(int value) {
        readoutOffset = value;
    }
    
    /**
     * Sets the size of the readout window, in units of 4 ns samples.
     * @param value - The readout window.
     */
    public void setReadoutWindow(int value) {
        readoutWindow = value;
    }
    
    @Override
    public void setReadoutWindowAfter(double value) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setReadoutWindowBefore(double value) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Sets the name of the collection which contains the relations
     * between truth hits from SLIC and the calorimeter hit output.
     * This is specifically for the trigger path hits.
     * @param collection - The collection name.
     */
    public void setTriggerPathTruthRelationsCollectionName(String collection) {
        triggerTruthRelationsCollectionName = collection;
    }
    
    /**
     * Sets the name of the collection which contains the relations
     * between truth hits from SLIC and the calorimeter hit output.
     * This is specifically for the readout path hits.
     * @param collection - The collection name.
     */
    public void setTruthRelationsCollectionName(String collection) {
        truthRelationsCollectionName = collection;
    }
    
    /**
     * Sets whether subdetector truth data for trigger path hits is
     * to be produced or not.
     * @param state - <code>true</code> indicates that the truth data
     * should be created, and <code>false</code> that it should not.
     */
    public void setWriteTriggerPathTruth(boolean state) {
        writeTriggerTruth = state;
    }
    
    /**
     * Sets whether subdetector truth data for readout path hits is
     * to be written to the output LCIO file or not.
     * @param state - <code>true</code> indicates that the truth data
     * should be written, and <code>false</code> that it should not.
     */
    public void setWriteTruth(boolean state) {
        writeTruth = state;
    }
    
    /**
     * Enumerable <code>PulseShape</code> defines the allowed types
     * of pulses that may be used to emulate the subdetector response
     * to incident energy.
     * 
     * @author Sho Uemura <meeg@slac.stanford.edu>
     */
    public enum PulseShape {
        CRRC, DoubleGaussian, ThreePole
    }
}