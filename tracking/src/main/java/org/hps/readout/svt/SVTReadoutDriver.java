package org.hps.readout.svt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtTimingConstants;

import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.base.BaseLCRelation;
import org.lcsim.event.base.BaseRawTrackerHit;
import org.lcsim.recon.tracking.digitization.sisim.CDFSiSensorSim;
import org.lcsim.recon.tracking.digitization.sisim.SiElectrodeData;
import org.lcsim.recon.tracking.digitization.sisim.SiElectrodeDataCollection;
import org.lcsim.recon.tracking.digitization.sisim.SiSensorSim;
import org.lcsim.recon.tracking.digitization.sisim.config.SimTrackerHitReadoutDriver;
import org.hps.readout.ReadoutDataManager;
import org.hps.readout.ReadoutDriver;
import org.hps.readout.ReadoutTimestamp;
import org.hps.readout.util.collection.LCIOCollection;
import org.hps.readout.util.collection.LCIOCollectionFactory;
import org.hps.readout.util.collection.TriggeredLCIOData;
import org.hps.recon.tracking.PulseShape;
import org.hps.util.RandomGaussian;

/**
 * SVT readout simulation.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 */
public class SVTReadoutDriver extends ReadoutDriver {
    //-----------------//
    //--- Constants ---//
    //-----------------//
    private static final String SVT_SUBDETECTOR_NAME = "Tracker";
    private PulseShape shape = new PulseShape.FourPole();
    
    private SimTrackerHitReadoutDriver readoutDriver = new SimTrackerHitReadoutDriver();
    private SiSensorSim siSimulation = new CDFSiSensorSim();
    private Map<SiSensor, PriorityQueue<StripHit>[]> hitMap = new HashMap<SiSensor, PriorityQueue<StripHit>[]>();
    private List<HpsSiSensor> sensors = null;
    
    // readout period time offset in ns
    private double readoutOffset = 0.0;
    private double readoutLatency = 280.0;
    private double pileupCutoff = 300.0;
    private String readout = "TrackerHits";
    private double timeOffset = 30.0;
    private boolean noPileup = false;
    private boolean addNoise = true;
    
    private boolean useTimingConditions = false;
    
    // cut settings
    private boolean enableThresholdCut = true;
    private int samplesAboveThreshold = 3;
    private double noiseThreshold = 2.0;
    private boolean enablePileupCut = true;
    private boolean dropBadChannels = true;
    
    // Collection Names
    private String outputCollection = "SVTRawTrackerHits";
    private String relationCollection = "SVTTrueHitRelations";
    
    private LCIOCollection<RawTrackerHit> trackerHitCollectionParams;
    private LCIOCollection<LCRelation> truthRelationsCollectionParams;
    private LCIOCollection<SimTrackerHit> truthHitsCollectionParams;
    
    public SVTReadoutDriver() {
        add(readoutDriver);
    }
    
    /**
     * Indicates whether or not noise should be simulated when analog
     * hits are generated.
     * @param addNoise - <code>true</code> adds noise simulation to
     * analog hits, while <code>false</code> uses only contributions
     * from pulses generated from truth data.
     */
    public void setAddNoise(boolean addNoise) {
        this.addNoise = addNoise;
    }
    
    /**
     * Indicates whether hits consistent with pile-up effects should
     * be dropped or not. A hit is considered to be consistent with
     * pile-up effects if its earlier sample indices are larger than
     * the later ones, suggesting that it includes the trailing end
     * of another pulse from earlier in time.
     * @param enablePileupCut - <code>true</code> enables the cut and
     * drops pile-up hits, while <code>false</code> disables the cut
     * and retains them.
     */
    public void setEnablePileupCut(boolean enablePileupCut) {
        this.enablePileupCut = enablePileupCut;
    }
    
    /**
     * Indicates whether noisy analog hits should be retained in
     * readout. Hits are required to have a certain number of samples
     * that exceeds a programmable noise threshold. The required
     * number of samples may be set by the method {@link
     * org.hps.readout.svt.SVTReadoutDriver#setSamplesAboveThreshold(int)
     * setSamplesAboveThreshold(int)} and the noise threshold may be
     * set with the method {@link
     * org.hps.readout.svt.SVTReadoutDriver#setNoiseThreshold(double)
     * setNoiseThreshold(double)}.
     * @param enableThresholdCut - <code>true</code> enables the cut
     * and drops noisy hits, while <code>false</code> disables the
     * cut and retains them.
     */
    public void setEnableThresholdCut(boolean enableThresholdCut) {
        this.enableThresholdCut = enableThresholdCut;
    }
    
    /**
     * Sets the noise threshold used in conjunction with the sample
     * threshold cut. The cut is enabled or disabled via the method
     * {@link
     * org.hps.readout.svt.SVTReadoutDriver#setEnableThresholdCut(boolean)
     * setEnableThresholdCut(boolean)}.
     * @param noiseThreshold - The noise threshold.
     */
    public void setNoiseThreshold(double noiseThreshold) {
        this.noiseThreshold = noiseThreshold;
    }
    
    /**
     * Sets the number of smaples that must be above the noise
     * threshold as employed by the sample threshold cut. The cut is
     * enabled or disabled via the method {@link
     * org.hps.readout.svt.SVTReadoutDriver#setEnableThresholdCut(boolean)
     * setEnableThresholdCut(boolean)}.
     * @param samplesAboveThreshold - The number of samples. Only six
     * samples are used, so values above six will result in every hit
     * being rejected. Values of zero or lower will result in the
     * acceptance of every hit. Threshold cut is inclusive.
     */
    public void setSamplesAboveThreshold(int samplesAboveThreshold) {
        this.samplesAboveThreshold = samplesAboveThreshold;
    }
    
    /**
     * Indicates whether pile-up should be simulated. If set to
     * <code>false</code>, analog hits are generated from the truth
     * hits of a given event individually, with no contribution from
     * neighboring events included. If set to <code>true</code>, data
     * from multiple events is included.
     * @param noPileup - <code>true</code> uses data from neighboring
     * events when generating analog hits, while <code>false</code>
     * uses only contributions from a single event.
     */
    public void setNoPileup(boolean noPileup) {
        this.noPileup = noPileup;
    }
    
    /**
     * Specifies whether analog hits which occur on "bad" channels
     * should be included in readout data or not.
     * @param dropBadChannels - <code>true</code> means that "bad"
     * channel hits will be excluded from readout, while
     * <code>false</code> means that they will be retained.
     */
    public void setDropBadChannels(boolean dropBadChannels) {
        this.dropBadChannels = dropBadChannels;
    }
    
    /**
     * Set the readout latency. This does not directly correspond to
     * any internal function in the readout simulation, but affects
     * what range of SVT ADC values are output around the trigger. It
     * is retained to allow a matching to the hardware function.
     * @param readoutLatency - The readout latency to use.
     */
    public void setReadoutLatency(double readoutLatency) {
        this.readoutLatency = readoutLatency;
    }
    
    /**
     * Sets whether to use manually defined timing conditions, or if
     * they should be loaded from the conditions database.
     * @param useTimingConditions - <code>true</code> uses the values
     * from the database, and <code>false</code> the manually defined
     * values.
     */
    public void setUseTimingConditions(boolean useTimingConditions) {
        this.useTimingConditions = useTimingConditions;
    }
    
    /**
     * Sets the pulse shape to be used when emulating the analog hit
     * response. Valid options are <code>CRRC</code> and
     * <code>FourPole</code>.
     * @param pulseShape - The pulse shape to be used.
     */
    public void setPulseShape(String pulseShape) {
        switch (pulseShape) {
            case "CR-RC":
                shape = new PulseShape.CRRC();
                break;
            case "FourPole":
                shape = new PulseShape.FourPole();
                break;
            default:
                throw new RuntimeException("Unrecognized pulseShape: " + pulseShape);
        }
    }
    
    @Override
    public void detectorChanged(Detector detector) {
        // TODO: What does this "SimTrackerHitReadoutDriver" do?
        String[] readouts = { readout };
        readoutDriver.setCollections(readouts);

        // Get the collection of all silicon sensors from the SVT.
        sensors = detector.getSubdetector(SVT_SUBDETECTOR_NAME).getDetectorElement().findDescendants(HpsSiSensor.class);
        
        // If pile-up simulation is disabled, instantiate all
        // possible processing queues. For the pile-up simulation,
        // these are generated as needed.
        if(!noPileup) {
            for(HpsSiSensor sensor : sensors) {
                @SuppressWarnings("unchecked")
                PriorityQueue<StripHit>[] hitQueues = new PriorityQueue[sensor.getNumberOfChannels()];
                hitMap.put(sensor, hitQueues);
            }
        }
        
        // Load timing conditions from the conditions database, if
        // this is requested.
        if(useTimingConditions) {
            SvtTimingConstants timingConstants = DatabaseConditionsManager.getInstance().getCachedConditions(SvtTimingConstants.SvtTimingConstantsCollection.class, "svt_timing_constants").getCachedData().get(0);
            readoutOffset = 4 * (timingConstants.getOffsetPhase() + 3);
            readoutLatency = 248.0 + timingConstants.getOffsetTime();
        }
    }
    
    @Override
    public void process(EventHeader event) {
        super.process(event);
        
        // Generate the truth hits.
        List<StripHit> stripHits = doSiSimulation();
        
        // If pile-up is to be simulated, process the hits into hit
        // queues. These hit queues are not integrated at this stage,
        // and are instead only handled at readout, as they are not
        // used downstream in the simulation.
        if(!noPileup) {
            //verboseWriter.write("Starting pile-up simulation...");
            
            // Process each of the truth hits.
            for(StripHit stripHit : stripHits) {
                // Get the sensor and channel for the truth hit.
                SiSensor sensor = stripHit.sensor;
                int channel = stripHit.channel;
                
                // Queue the hit in the processing queue appropriate
                // to its sensor and channel.
                PriorityQueue<StripHit>[] hitQueues = hitMap.get(sensor);
                if(hitQueues[channel] == null) {
                    hitQueues[channel] = new PriorityQueue<StripHit>();
                }
                hitQueues[channel].add(stripHit);
            }
            
            // Hits older than a certain time frame should no longer
            // be used for pile-up simulation and should be removed
            // from the processing queues.
            for(SiSensor sensor : sensors) {
                // Get the processing queue for the current sensor.
                PriorityQueue<StripHit>[] hitQueues = hitMap.get(sensor);
                
                // Check each hit to see if it is still in-time.
                for(int i = 0; i < hitQueues.length; i++) {
                    if(hitQueues[i] != null) {
                        // Remove old hits.
                        while(!hitQueues[i].isEmpty() && hitQueues[i].peek().time < ReadoutDataManager.getCurrentTime() - (readoutLatency + pileupCutoff)) {
                            hitQueues[i].poll();
                        }
                        
                        // If the queue is empty, remove it.
                        if(hitQueues[i].isEmpty()) { hitQueues[i] = null; }
                    }
                }
            }
        }
        
        // Otherwise, process the hits for a no pile-up simulation.
        // When no pile-up is simulated, hits are fully processed and
        // output on an event-by-event basis.
        else {
            // Create a list to hold the analog data.
            List<RawTrackerHit> hits = new ArrayList<RawTrackerHit>();
            
            // Process each of the truth hits.
            for(StripHit stripHit : stripHits) {
                // Get the hit parameters.
                HpsSiSensor sensor = (HpsSiSensor) stripHit.sensor;
                short[] samples = new short[6];
                
                // Create a signal buffer and populate it with the
                // appropriate pedestal values.
                double[] signal = new double[6];
                for(int sampleN = 0; sampleN < 6; sampleN++) {
                    signal[sampleN] = sensor.getPedestal(stripHit.channel, sampleN);
                }
                
                // If noise should be added, do so.
                if(addNoise) {
                    addNoise(sensor, stripHit.channel, signal);
                }
                
                // Emulate the pulse response and add it to the
                // sample array.
                for(int sampleN = 0; sampleN < 6; sampleN++) {
                    double time = sampleN * HPSSVTConstants.SAMPLING_INTERVAL - timeOffset;
                    shape.setParameters(stripHit.channel, (HpsSiSensor) sensor);
                    signal[sampleN] += stripHit.amplitude * shape.getAmplitudePeakNorm(time);
                    samples[sampleN] = (short) Math.round(signal[sampleN]);
                }
                
                // Create raw tracker hits from the sample data.
                long channel_id = sensor.makeChannelID(stripHit.channel);
                RawTrackerHit hit = new BaseRawTrackerHit(0, channel_id, samples, new ArrayList<SimTrackerHit>(stripHit.simHits), sensor);
                
                // If the analog hit passes the readout cuts, it may
                // be added to the data stream.
                if(readoutCuts(hit)) { hits.add(hit); }
            }
            
            // Output the processed hits to the LCIO stream.
            ReadoutDataManager.addData(outputCollection, hits, RawTrackerHit.class);
        }
    }
    
    @Override
    public void startOfData() {
        // The output collection is only handled by the readout data
        // manager if no pile-up simulation is included. Otherwise,
        // the driver outputs its own collection at readout.
        if(noPileup) {
            LCIOCollectionFactory.setCollectionName(outputCollection);
            LCIOCollectionFactory.setProductionDriver(this);
            LCIOCollectionFactory.setFlags(1 << LCIOConstants.TRAWBIT_ID1);
            LCIOCollectionFactory.setReadoutName(readout);
            LCIOCollection<RawTrackerHit> noPileUpCollectionParams = LCIOCollectionFactory.produceLCIOCollection(RawTrackerHit.class);
            ReadoutDataManager.registerCollection(noPileUpCollectionParams, true, 8.0, 32.0);
        }
        
        // Define the LCSim on-trigger collection parameters.
        LCIOCollectionFactory.setCollectionName(outputCollection);
        LCIOCollectionFactory.setProductionDriver(this);
        LCIOCollectionFactory.setFlags(1 << LCIOConstants.TRAWBIT_ID1);
        LCIOCollectionFactory.setReadoutName(readout);
        trackerHitCollectionParams = LCIOCollectionFactory.produceLCIOCollection(RawTrackerHit.class);
        
        LCIOCollectionFactory.setCollectionName(relationCollection);
        LCIOCollectionFactory.setProductionDriver(this);
        truthRelationsCollectionParams = LCIOCollectionFactory.produceLCIOCollection(LCRelation.class);
        
        LCIOCollectionFactory.setCollectionName("TrackerHits");
        LCIOCollectionFactory.setFlags(0xc0000000);
        LCIOCollectionFactory.setProductionDriver(this);
        LCIOCollectionFactory.setReadoutName("TrackerHits");
        truthHitsCollectionParams = LCIOCollectionFactory.produceLCIOCollection(SimTrackerHit.class);
        
        // Run the superclass method.
        super.startOfData();
    }
    
    /**
     * Performs a simulation of silicon sensor response and generates
     * a collection of {@link
     * org.hps.readout.svt.SVTReadoutDriver.StripHit StripHit}
     * objects representing the detector response.
     * @return Returns a collection of StripHit objects describing
     * the detector response for the current event.
     */
    private List<StripHit> doSiSimulation() {
        // Create a list to store the simulated hit objects.
        List<StripHit> stripHits = new ArrayList<StripHit>();
        
        // Process each of the SVT sensors.
        for(SiSensor sensor : sensors) {
            // Set the sensor to be used in the charge deposition
            // simulation.
            siSimulation.setSensor(sensor);
            
            // Perform the charge deposition simulation.
            Map<ChargeCarrier, SiElectrodeDataCollection> electrodeDataMap = siSimulation.computeElectrodeData();
            
            // Iterate over all possible charge carriers.
            for(ChargeCarrier carrier : ChargeCarrier.values()) {
                // If the sensor is capable of collecting the given
                // charge carrier, then obtain the electrode data for
                // the sensor.
                if(sensor.hasElectrodesOnSide(carrier)) {
                    // Attempt to obtain electrode data.
                    SiElectrodeDataCollection electrodeDataCol = electrodeDataMap.get(carrier);

                    // If there is no electrode data available create
                    // a new instance of electrode data.
                    if(electrodeDataCol == null) {
                        electrodeDataCol = new SiElectrodeDataCollection();
                    }
                    
                    // Loop over all sensor channels.
                    for(Integer channel : electrodeDataCol.keySet()) {
                        // Get the electrode data for this channel.
                        SiElectrodeData electrodeData = electrodeDataCol.get(channel);
                        Set<SimTrackerHit> simHits = electrodeData.getSimulatedHits();

                        // Compute hit time as the unweighted average
                        // of SimTrackerHit times; this is dumb but
                        // okay since there's generally only one
                        // SimTrackerHit.
                        double time = 0.0;
                        for(SimTrackerHit hit : simHits) {
                            time += hit.getTime();
                        }
                        time /= simHits.size();
                        time += ReadoutDataManager.getCurrentTime();

                        // Get the charge in units of electrons.
                        double charge = electrodeData.getCharge();
                        
                        // Calculate the amplitude.
                        double resistorValue = 100; // Ohms
                        double inputStageGain = 1.5;
                        // FIXME: This should use the gains instead
                        double amplitude = (charge / HPSSVTConstants.MIP) * resistorValue * inputStageGain * Math.pow(2, 14) / 2000;
                        
                        // Generate a StripHit object containing the
                        // simulation data and add it to the list.
                        stripHits.add(new StripHit(sensor, channel, amplitude, time, simHits));
                    }
                }
            }
            
            // Clear the sensors of all deposited charge
            siSimulation.clearReadout();
        }
        
        // Return the collection of StripHit objects.
        return stripHits;
    }
    
    /**
     * Adds a random Gaussian noise signature to the specified signal
     * buffer based on the sensor and channel parameters.
     * @param sensor - The sensor on which the signal buffer occurs.
     * @param channel - The channel on which the signal buffer
     * occurs.
     * @param signal - The signal buffer. This must be an array of
     * size six.
     */
    private void addNoise(SiSensor sensor, int channel, double[] signal) {
        for(int sampleN = 0; sampleN < 6; sampleN++) {
            signal[sampleN] += RandomGaussian.getGaussian(0, ((HpsSiSensor) sensor).getNoise(channel, sampleN));
        }
    }
    
    /**
     * Performs each of the three readout cuts, if they are enabled.
     * This is the equivalent of calling, as appropriate, the methods
     * {@link
     * org.hps.readout.svt.SVTReadoutDriver#samplesAboveThreshold(RawTrackerHit)
     * samplesAboveThreshold(RawTrackerHit)}, {@link
     * org.hps.readout.svt.SVTReadoutDriver#pileupCut(RawTrackerHit)
     * pileupCut(RawTrackerHit)}, and {@link
     * org.hps.readout.svt.SVTReadoutDriver#badChannelCut(RawTrackerHit)
     * badChannelCut(RawTrackerHit)}.
     * @param hit - The analog hit to test.
     * @return Returns <code>true</code> if all enabled cuts are
     * passed, and <code>false</code> otherwise.
     */
    private boolean readoutCuts(RawTrackerHit hit) {
        // Perform each enabled cut.
        if(enableThresholdCut && !samplesAboveThreshold(hit)) {
            return false;
        }
        if(enablePileupCut && !pileupCut(hit)) {
            return false;
        }
        if(dropBadChannels && !badChannelCut(hit)) {
            return false;
        }
        
        // If all enabled cuts are passed, return true.
        return true;
    }
    
    /**
     * Checks whether an analog hit occurred on a "bad" channel.
     * @param hit - The hit to be checked.
     * @return Returns <code>true</code> if the hit <i>did not</i>
     * occur on a bad channel, and <code>false</code> if it did.
     */
    private boolean badChannelCut(RawTrackerHit hit) {
        HpsSiSensor sensor = (HpsSiSensor) hit.getDetectorElement();
        int channel = hit.getIdentifierFieldValue("strip");
        return !sensor.isBadChannel(channel);
    }
    
    /**
     * Attempts to eliminate samples where the pulse starts before
     * the sample array. This is done by requiring the second, third,
     * and fourth samples of the array to be increasing in value with
     * index.
     * @param hit - The hit to check.
     * @return Returns <code>true</code> if the no pile-up condition
     * is met and <code>false</code> if it is not.
     */
    private boolean pileupCut(RawTrackerHit hit) {
        short[] samples = hit.getADCValues();
        return (samples[2] > samples[1] || samples[3] > samples[2]);
    }
    
    /**
     * Attempts to eliminate false hits generated due to noise by
     * requiring that a programmable number of samples exceed a
     * similarly programmable noise threshold.
     * @param hit - The hit to be checked.
     * @return Returns <code>true</code> if the noise threshold count
     * cut is met and <code>false</code> if it is not.
     */
    private boolean samplesAboveThreshold(RawTrackerHit hit) {
        // Get the channel and sensor information for the hit.
        int channel = hit.getIdentifierFieldValue("strip");
        HpsSiSensor sensor = (HpsSiSensor) hit.getDetectorElement();
        
        // Track the noise and pedestal for each sample.
        double noise;
        double pedestal;
        
        // Iterate over the samples and count how many are above the
        // noise threshold.
        int count = 0;
        short[] samples = hit.getADCValues();
        for(int sampleN = 0; sampleN < samples.length; sampleN++) {
            pedestal = sensor.getPedestal(channel, sampleN);
            noise = sensor.getNoise(channel, sampleN);
            if(samples[sampleN] - pedestal > noise * noiseThreshold) {
                count++;
            }
        }
        
        // The cut is passed if enough samples are above the noise
        // threshold to pass the minimum count threshold.
        return count >= samplesAboveThreshold;
    }
    
    @Override
    protected Collection<TriggeredLCIOData<?>> getOnTriggerData(double triggerTime) {
        // No pile-up events are output on an event-by-event basis,
        // and as such, do not output anything at this stage.
        if(noPileup) { return null; }
        
        // Create a list to hold the analog data
        List<RawTrackerHit> hits = new ArrayList<RawTrackerHit>();
        List<SimTrackerHit> truthHits = new ArrayList<SimTrackerHit>();
        List<LCRelation> trueHitRelations = new ArrayList<LCRelation>();
        
        // Calculate time of first sample
        double firstSample = Math.floor(((triggerTime + 256) - readoutLatency - readoutOffset) / HPSSVTConstants.SAMPLING_INTERVAL)
                * HPSSVTConstants.SAMPLING_INTERVAL + readoutOffset;
        
        List<StripHit> processedHits = new ArrayList<StripHit>();
        
        for(SiSensor sensor : sensors) {
            // Get the hit queues for the current sensor.
            PriorityQueue<StripHit>[] hitQueues = hitMap.get(sensor);
            
            // Iterate over the hit queue channels.
            for(int channel = 0; channel < hitQueues.length; channel++) {
                // Unless noise should be added, there is nothing to
                // process on an empty hit queue. Skip it.
                if(!addNoise && (hitQueues[channel] == null || hitQueues[channel].isEmpty())) {
                    continue;
                }
                
                // Create a buffer to hold the extracted signal for
                // the channel. Populate it with the appropriate
                // pedestal values.
                double[] signal = new double[6];
                for(int sampleN = 0; sampleN < 6; sampleN++) {
                    signal[sampleN] = ((HpsSiSensor) sensor).getPedestal(channel, sampleN);
                }
                
                // If noise should be added, do so.
                if(addNoise) {
                    addNoise(sensor, channel, signal);
                }
                
                // Create a list to store truth SVT hits.
                List<SimTrackerHit> simHits = new ArrayList<SimTrackerHit>();
                
                // If there is data in the hit queues, process it.
                if(hitQueues[channel] != null) {
                    for(StripHit hit : hitQueues[channel]) {
                        processedHits.add(hit);
                        
                        // Track the noise and contribution to the
                        // signal from the current hit.
                        double meanNoise = 0;
                        double totalContrib = 0;
                        
                        // Emulate the pulse response for the hit
                        // across all size samples.
                        StringBuffer signalBuffer = new StringBuffer("\t\t\t\tSample Pulse       :: [");
                        for(int sampleN = 0; sampleN < 6; sampleN++) {
                            double sampleTime = firstSample + sampleN * HPSSVTConstants.SAMPLING_INTERVAL;
                            shape.setParameters(channel, (HpsSiSensor) sensor);
                            double signalAtTime = hit.amplitude * shape.getAmplitudePeakNorm(sampleTime - hit.time);
                            totalContrib += signalAtTime;
                            signal[sampleN] += signalAtTime;
                            meanNoise += ((HpsSiSensor) sensor).getNoise(channel, sampleN);
                            
                            signalBuffer.append(signalAtTime + " (" + sampleTime + ")");
                            if(sampleN != 5) {
                                signalBuffer.append("   ");
                            }
                        }
                        signalBuffer.append("]");
                        
                        // TODO: Move this to the noise comparison below.
                        meanNoise /= 6;
                        
                        // Calculate the average noise across all
                        // samples and compare it to the contribution
                        // from the hit. If it exceeds a the noise
                        // threshold, store it as a truth hit.
                        //meanNoise /= 6;
                        if(totalContrib > 4.0 * meanNoise) {
                            simHits.addAll(hit.simHits);
                        }
                    }
                }
                
                // Convert the samples into a short array,
                short[] samples = new short[6];
                for(int sampleN = 0; sampleN < 6; sampleN++) {
                    samples[sampleN] = (short) Math.round(signal[sampleN]);
                }
                
                // Get the proper channel ID.
                long channel_id = ((HpsSiSensor) sensor).makeChannelID(channel);
                
                // Create a new tracker hit.
                RawTrackerHit hit = new BaseRawTrackerHit(0, channel_id, samples, simHits, sensor);
                
                // Only tracker hits that pass the readout cuts may
                // be passed through to readout.
                if(readoutCuts(hit)) {
                    // Add the hit to the readout hits collection.
                    hits.add(hit);
                    
                    // Associate the truth hits with the raw hit and
                    // add them to the truth hits collection.
                    for(SimTrackerHit simHit : hit.getSimTrackerHits()) {
                        LCRelation hitRelation = new BaseLCRelation(hit, simHit);
                        trueHitRelations.add(hitRelation);
                        truthHits.add(simHit);
                    }
                }
            }
        }
        
        // Create the collection data objects for output to the
        // readout event.
        TriggeredLCIOData<RawTrackerHit> hitCollection = new TriggeredLCIOData<RawTrackerHit>(trackerHitCollectionParams);
        hitCollection.getData().addAll(hits);
        TriggeredLCIOData<SimTrackerHit> truthHitCollection = new TriggeredLCIOData<SimTrackerHit>(truthHitsCollectionParams);
        truthHitCollection.getData().addAll(truthHits);
        TriggeredLCIOData<LCRelation> truthRelationCollection = new TriggeredLCIOData<LCRelation>(truthRelationsCollectionParams);
        truthRelationCollection.getData().addAll(trueHitRelations);
        
        // MC particles need to be extracted from the truth hits
        // and included in the readout data to ensure that the
        // full truth chain is available.
        Set<MCParticle> truthParticles = new java.util.HashSet<MCParticle>();
        for(SimTrackerHit simHit : truthHits) {
            ReadoutDataManager.addParticleParents(simHit.getMCParticle(), truthParticles);
        }
        
        // Create the truth MC particle collection.
        LCIOCollectionFactory.setCollectionName("MCParticle");
        LCIOCollectionFactory.setProductionDriver(this);
        LCIOCollection<MCParticle> truthParticleCollection = LCIOCollectionFactory.produceLCIOCollection(MCParticle.class);
        TriggeredLCIOData<MCParticle> truthParticleData = new TriggeredLCIOData<MCParticle>(truthParticleCollection);
        truthParticleData.getData().addAll(truthParticles);
        
        // A trigger timestamp needs to be produced as well.
        ReadoutTimestamp timestamp = new ReadoutTimestamp(ReadoutTimestamp.SYSTEM_TRACKER, firstSample);
        LCIOCollectionFactory.setCollectionName(ReadoutTimestamp.collectionName);
        LCIOCollection<ReadoutTimestamp> timestampCollection = LCIOCollectionFactory.produceLCIOCollection(ReadoutTimestamp.class);
        TriggeredLCIOData<ReadoutTimestamp> timestampData = new TriggeredLCIOData<ReadoutTimestamp>(timestampCollection);
        timestampData.getData().add(timestamp);
        
        // Store them in a single collection.
        Collection<TriggeredLCIOData<?>> eventOutput = new ArrayList<TriggeredLCIOData<?>>(5);
        eventOutput.add(hitCollection);
        eventOutput.add(truthParticleData);
        eventOutput.add(truthHitCollection);
        eventOutput.add(truthRelationCollection);
        eventOutput.add(timestampData);
        
        // Return the event output.
        return eventOutput;
    }
    
    /**
     * Class <code>StripHit</code> is responsible for storing several
     * parameters defining a simulated hit object.
     */
    private class StripHit implements Comparable<Object> {
        SiSensor sensor;
        int channel;
        double amplitude;
        double time;
        Set<SimTrackerHit> simHits;

        public StripHit(SiSensor sensor, int channel, double amplitude, double time, Set<SimTrackerHit> simHits) {
            this.sensor = sensor;
            this.channel = channel;
            this.amplitude = amplitude;
            this.time = time;
            this.simHits = simHits;
        }

        @Override
        public int compareTo(Object o) {
            double deltaT = time - ((StripHit) o).time;
            if(deltaT > 0) {
                return 1;
            } else if(deltaT < 0) {
                return -1;
            } else {
                return 0;
            }
        }
    }
    
    @Override
    protected double getTimeDisplacement() {
        return 0;
    }
    
    @Override
    protected double getTimeNeededForLocalOutput() {
        // TODO: Probably should have some defined value - buffer seems to be filled enough from the ecal delay alone, though.
        return 100;
    }
}