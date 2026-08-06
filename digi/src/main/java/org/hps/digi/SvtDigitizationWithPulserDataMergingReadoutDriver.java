package org.hps.digi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtTimingConstants;
import org.hps.readout.svt.HPSSVTConstants;
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
public class SvtDigitizationWithPulserDataMergingReadoutDriver extends ReadoutDriver {
    //-----------------//
    //--- Constants ---//
    //-----------------//
    private static final String SVT_SUBDETECTOR_NAME = "Tracker";
    private PulseShape shape = new PulseShape.FourPole();
    
    private SimTrackerHitReadoutDriver readoutDriver = new SimTrackerHitReadoutDriver();
    private SiSensorSim siSimulation = new CDFSiSensorSim();
    private Map<SiSensor, PriorityQueue<StripHit>[]> hitMap = new HashMap<SiSensor, PriorityQueue<StripHit>[]>();
    private Map<SiSensor, PriorityQueue<StripHit>[]> pulserHitMap = new HashMap<SiSensor, PriorityQueue<StripHit>[]>();
    private List<HpsSiSensor> sensors = null;
    
    // readout period time offset in ns
    private double readoutOffset = 0.0;
    private double readoutLatency = 280.0;
    private double pileupCutoff = 300.0;
    private double simHitTimeOffset=0.0;
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
    
    // ------------------------------------------------------------------
    // Truth-relation diagnostics.
    //
    // An MC strip hit only reaches SVTTrueHitRelations if it passes two
    // independent gates in getOnTriggerData():
    //   G1  totalContrib > 4.0 * meanNoise   -- attaches hit.simHits to the channel
    //   G2  readoutCuts(hit)                 -- if this fails the raw hit is dropped
    //                                           and the relations are never built
    // G1 sees only the MC contribution, so it cannot depend on the pulser overlay.
    // G2 is evaluated on the combined waveform, and on a channel carrying a pulser
    // hit that waveform is real data whose baseline need not agree with the pedestal
    // that samplesAboveThreshold() subtracts. Every counter below is therefore
    // indexed [0] = no pulser hit on this channel, [1] = pulser hit on this channel,
    // so the two populations can be compared directly.
    // ------------------------------------------------------------------
    private boolean debug = false;
    private int debugMaxPrint = 200;
    private int debugPrinted = 0;

    private long nChannelsSeen = 0;
    private long nChannelsWithPulser = 0;
    private long nPulserQueueEmptyNonNull = 0;   // would make poll() return null at L668

    private final long[] nMCStripHits = new long[2];
    private final long[] nTruthGatePass = new long[2];
    private final long[] nTruthGateFail = new long[2];
    private final long[] nSimHitsLostTruthGate = new long[2];

    private final long[] nHitsReadoutPass = new long[2];
    private final long[] nHitsReadoutFail = new long[2];
    private final long[] nHitsReadoutFailWithTruth = new long[2];
    private final long[] nSimHitsLostReadoutCut = new long[2];
    private final long[] nRelationsWritten = new long[2];

    // totalContrib / meanNoise, binned: <1, 1-2, 2-4, 4-8, 8-16, >=16
    private final long[][] truthRatioBins = new long[2][6];

    // Mean offset of the pulser waveform from the conditions-DB pedestal, sampled
    // before any MC contribution is added. A negative value means the overlaid data
    // sits below the pedestal that the threshold cut subtracts, which eats into the
    // MC signal's headroom over threshold.
    private double sumPulserBaselineOffset = 0;
    private double sumPulserBaselineOffsetSq = 0;
    private long nPulserBaselineChannels = 0;

    //-------------------------//
    //--- Hit origin labels ---//
    //-------------------------//

    /**
     * Opt-in provenance labelling for the output raw hits.
     *
     * The absence of a truth relation on a raw hit is ambiguous: it can mean the
     * channel carried only overlaid pulser data, or that an MC particle did deposit
     * charge there but fell below the truth gate G1 and so had its relation dropped.
     * Those two cases need different fixes, so they are separated here by recording
     * what actually contributed to each channel, independent of G1.
     *
     * Two extra relation collections are written:
     *
     *   SVTHitOriginPulser    (RawTrackerHit out) -> (RawTrackerHit pulser source)
     *   SVTHitOriginMCContrib (RawTrackerHit out) -> (MCParticle depositing charge)
     *
     * The MC-contribution relation is written whether or not G1 passed. Combined with
     * the existing SVTTrueHitRelations this gives a complete category per hit:
     *
     *   pulser  mcContrib  truthRel   category
     *     -         -          -      NOISE                 pedestal and noise only
     *     -         Y          Y      MC_PURE               MC only, truth kept
     *     -         Y          -      MC_PURE_SUBTHRESH     MC only, truth lost at G1
     *     Y         -          -      PULSER_PURE           overlaid data only
     *     Y         Y          Y      MERGED                MC + data, truth kept
     *     Y         Y          -      MERGED_SUBTHRESH      MC + data, truth lost at G1
     *
     * MERGED_SUBTHRESH is the category that distinguishes a genuine relation-propagation
     * bug in the merging from tracks simply picking up real pulser hits.
     *
     * Off by default: when enabled the MCParticle collection is widened to cover the
     * sub-threshold contributors, so the production output is only bit-identical with
     * this disabled.
     */
    private boolean writeHitOriginCollections = false;
    private String hitOriginPulserCollection = "SVTHitOriginPulser";
    private String hitOriginMCContribCollection = "SVTHitOriginMCContrib";

    private LCIOCollection<LCRelation> hitOriginPulserCollectionParams;
    private LCIOCollection<LCRelation> hitOriginMCContribCollectionParams;

    private static final int ORIGIN_NOISE             = 0;
    private static final int ORIGIN_MC_PURE           = 1;
    private static final int ORIGIN_MC_PURE_SUBTHRESH = 2;
    private static final int ORIGIN_PULSER_PURE       = 3;
    private static final int ORIGIN_MERGED            = 4;
    private static final int ORIGIN_MERGED_SUBTHRESH  = 5;
    private static final String[] ORIGIN_NAME = {
        "NOISE            ", "MC_PURE          ", "MC_PURE_SUBTHRESH",
        "PULSER_PURE      ", "MERGED           ", "MERGED_SUBTHRESH "
    };
    private final long[] nHitsByOrigin = new long[6];
    private final long[] nSimHitsByOrigin = new long[6];

    // Collection Names
    private String outputCollection = "SVTRawTrackerHits";
    private String relationCollection = "SVTTrueHitRelations";

    private LCIOCollection<RawTrackerHit> trackerHitCollectionParams;
    private LCIOCollection<LCRelation> truthRelationsCollectionParams;
    private LCIOCollection<SimTrackerHit> truthHitsCollectionParams;
    /**
     * The name of the input {@link org.lcsim.event.RawTrackerHit
     * RawTrackerHit} collection from pulser data.
     */
    private String pulserDataCollectionName = "SVTRawTrackerHits";

    public SvtDigitizationWithPulserDataMergingReadoutDriver() {
        add(readoutDriver);
    }
    
    /**
     * Indicates whether or not noise should be simulated when analog
     * hits are generated.
     * @param addNoise - <code>true</code> adds noise simulation to
     * analog hits, while <code>false</code> uses only contributions
     * from pulses generated from truth data.
     */
    /**
     * Enables the truth-relation diagnostics. Counters are accumulated regardless;
     * this additionally prints per-hit lines for the first {@link #debugMaxPrint}
     * MC strip hits that lose their truth relation.
     * @param debug - <code>true</code> to enable diagnostic printout.
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Sets how many individual truth-loss records are printed when debug is enabled.
     * @param debugMaxPrint - Maximum number of per-hit lines.
     */
    public void setDebugMaxPrint(int debugMaxPrint) {
        this.debugMaxPrint = debugMaxPrint;
    }

    /**
     * Write the per-hit provenance relation collections described above. Off by
     * default; enabling it also widens the output MCParticle collection to cover
     * sub-threshold contributors.
     */
    public void setWriteHitOriginCollections(boolean writeHitOriginCollections) {
        this.writeHitOriginCollections = writeHitOriginCollections;
    }

    public void setHitOriginPulserCollection(String name) {
        this.hitOriginPulserCollection = name;
    }

    public void setHitOriginMCContribCollection(String name) {
        this.hitOriginMCContribCollection = name;
    }

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
     * org.hps.readout.svt.SvtDigitizationWithPulserDataMergingReadoutDriver#setSamplesAboveThreshold(int)
     * setSamplesAboveThreshold(int)} and the noise threshold may be
     * set with the method {@link
     * org.hps.readout.svt.SvtDigitizationWithPulserDataMergingReadoutDriver#setNoiseThreshold(double)
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
     * org.hps.readout.svt.SvtDigitizationWithPulserDataMergingReadoutDriver#setEnableThresholdCut(boolean)
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
     * org.hps.readout.svt.SvtDigitizationWithPulserDataMergingReadoutDriver#setEnableThresholdCut(boolean)
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
     * Set the time offset for the SimTrackerHit
     * inside the APV25 window
     * @param simHitTimeOffset - The offset to use
     */
    public void setSimHitTimeOffset(double simHitTimeOffset) {
        this.simHitTimeOffset = simHitTimeOffset;
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
    /**
     * Sets the name of the input pulser data collection name.
     * @param collection - The collection name.
     */
    public void setPulserDataCollectionName(String collection) {
        this.pulserDataCollectionName = collection;
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
                    int nChans=640;
                if(sensor.getNumberOfChannels()==510)
                    nChans=512;
                //really dumb way to account for channels not read out
                PriorityQueue<StripHit>[] hitQueues = new PriorityQueue[nChans]; 
                PriorityQueue<StripHit>[] pulserHitQueues = new PriorityQueue[nChans];
                hitMap.put(sensor, hitQueues);
                pulserHitMap.put(sensor, pulserHitQueues);
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
        // get the pulser hits
        Collection<RawTrackerHit> rawHits = ReadoutDataManager.getData(ReadoutDataManager.getCurrentTime(), ReadoutDataManager.getCurrentTime() + 2.0, pulserDataCollectionName, RawTrackerHit.class);                  
        // Generate the truth hits.
        List<StripHit> stripHits = doSiSimulation();         
        List<StripHit> pulserStripHits=makePulserStripHits(rawHits);
       
        // If pile-up is to be simulated, process the hits into hit
        // queues. These hit queues are not integrated at this stage,
        // and are instead only handled at readout, as they are not
        // used downstream in the simulation.
        if(!noPileup) {
            // Process each of the pulser hits
            for (StripHit pulserHit : pulserStripHits) {
                // Get the sensor and channel for the pulser hit.
                HpsSiSensor sensor = (HpsSiSensor) pulserHit.sensor;
                int channel = pulserHit.channel;
                // Queue the hit in the processing queue appropriate
                // to its sensor and channel.
                PriorityQueue<StripHit>[] pulserHitQueues = pulserHitMap.get(sensor);
                if(pulserHitQueues[channel] == null) {
                    pulserHitQueues[channel] = new PriorityQueue<StripHit>();
                }
                pulserHitQueues[channel].add(pulserHit);
            }
            
            // Process each of the truth hits
            for (StripHit stripHit : stripHits) {
                // Get the sensor and channel for the truth hit.
                HpsSiSensor sensor = (HpsSiSensor)stripHit.sensor;
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
                PriorityQueue<StripHit>[] pulserHitQueues = pulserHitMap.get(sensor);                
                // Check each hit to see if it is still in-time.
                for(int i = 0; i < pulserHitQueues.length; i++) {
                    if(pulserHitQueues[i] != null) {
                        // Remove old hits.
                        while(!pulserHitQueues[i].isEmpty() && pulserHitQueues[i].peek().time < ReadoutDataManager.getCurrentTime() - (readoutLatency + pileupCutoff)) {
                            pulserHitQueues[i].poll();
                        }                        
                        // If the queue is empty, remove it.
                        if(pulserHitQueues[i].isEmpty()) { pulserHitQueues[i] = null; }
                    }
                }

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
        addDependency(pulserDataCollectionName);
        // Define the LCSim on-trigger collection parameters.
        LCIOCollectionFactory.setCollectionName(outputCollection);
        LCIOCollectionFactory.setProductionDriver(this);
        LCIOCollectionFactory.setFlags(1 << LCIOConstants.TRAWBIT_ID1);
        LCIOCollectionFactory.setReadoutName(readout);
        trackerHitCollectionParams = LCIOCollectionFactory.produceLCIOCollection(RawTrackerHit.class);
        
        LCIOCollectionFactory.setCollectionName(relationCollection);
        LCIOCollectionFactory.setProductionDriver(this);
        truthRelationsCollectionParams = LCIOCollectionFactory.produceLCIOCollection(LCRelation.class);

        if(writeHitOriginCollections) {
            LCIOCollectionFactory.setCollectionName(hitOriginPulserCollection);
            LCIOCollectionFactory.setProductionDriver(this);
            hitOriginPulserCollectionParams = LCIOCollectionFactory.produceLCIOCollection(LCRelation.class);

            LCIOCollectionFactory.setCollectionName(hitOriginMCContribCollection);
            LCIOCollectionFactory.setProductionDriver(this);
            hitOriginMCContribCollectionParams = LCIOCollectionFactory.produceLCIOCollection(LCRelation.class);
        }
        
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
     * org.hps.readout.svt.SvtDigitizationWithPulserDataMergingReadoutDriver.StripHit StripHit}
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
                            time += hit.getTime()+simHitTimeOffset;
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
    
    private List<StripHit> makePulserStripHits(Collection<RawTrackerHit> rawHits) {
        // Create a list to store the simulated hit objects.
        List<StripHit> stripHits = new ArrayList<StripHit>();
        for (RawTrackerHit hit: rawHits){            
            SiSensor sensor=(SiSensor) hit.getDetectorElement();
            int strip = hit.getIdentifierFieldValue("strip");
            double time=ReadoutDataManager.getCurrentTime();
            stripHits.add(new StripHit(sensor, strip, time, hit));
        }
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
     * org.hps.readout.svt.SvtDigitizationWithPulserDataMergingReadoutDriver#samplesAboveThreshold(RawTrackerHit)
     * samplesAboveThreshold(RawTrackerHit)}, {@link
     * org.hps.readout.svt.SvtDigitizationWithPulserDataMergingReadoutDriver#pileupCut(RawTrackerHit)
     * pileupCut(RawTrackerHit)}, and {@link
     * org.hps.readout.svt.SvtDigitizationWithPulserDataMergingReadoutDriver#badChannelCut(RawTrackerHit)
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

        // Provenance relations, only populated when writeHitOriginCollections is set.
        List<LCRelation> hitOriginPulserRelations = new ArrayList<LCRelation>();
        List<LCRelation> hitOriginMCContribRelations = new ArrayList<LCRelation>();
        // Contributors that failed G1 and so appear in no truth hit. They still need to
        // reach the output MCParticle collection or the relations above would dangle.
        Set<MCParticle> extraContribParticles = new java.util.HashSet<MCParticle>();
        // Calculate time of first sample

        double firstSample = Math.floor(((triggerTime + 256) - readoutLatency - readoutOffset) / HPSSVTConstants.SAMPLING_INTERVAL)
                * HPSSVTConstants.SAMPLING_INTERVAL + readoutOffset;
        
        List<StripHit> processedHits = new ArrayList<StripHit>();
        
        for(SiSensor sensor : sensors) {
            // Get the hit queues for the current sensor.
            PriorityQueue<StripHit>[] hitQueues = hitMap.get(sensor);
            PriorityQueue<StripHit>[] pulserHitQueues = pulserHitMap.get(sensor);
            
            // Iterate over the hit queue channels.
            for(int channel = 0; channel < hitQueues.length; channel++) {
                // Unless noise should be added, there is nothing to
                // process on an empty hit queue. Skip it.
                if(!addNoise && (hitQueues[channel] == null || hitQueues[channel].isEmpty()) &&  (pulserHitQueues[channel] == null || pulserHitQueues[channel].isEmpty())){
                    continue;
                }
                
                // Create a buffer to hold the extracted response for
                // the channel.
                double[] signal = new double[6];

                nChannelsSeen++;
                if(pulserHitQueues[channel] != null && pulserHitQueues[channel].isEmpty()) {
                    // poll() below returns null on an empty queue, so this counts how often
                    // the != null test at the next line is not sufficient on its own.
                    nPulserQueueEmptyNonNull++;
                }

                //do the pulser hit first...if there is a pulser hit, don't add pedestal or noise to mc hit
                boolean hasPulserHit=false; // flag if this channel has a pulser hit
                // Kept in scope so the output hit can be related back to the data hit
                // it was merged with.
                RawTrackerHit pulserSourceHit = null;
                if(pulserHitQueues[channel] != null){
                    StripHit ph=pulserHitQueues[channel].poll();
                    RawTrackerHit rth=ph.getRawTrackerHit();
                    pulserSourceHit = rth;
                    hasPulserHit=true;
                    short[] samples =rth.getADCValues();
                    for(int sampleN = 0; sampleN < 6; sampleN++) {
                        signal[sampleN] = samples[sampleN];
                    }
                }

                // Index every counter by whether this channel carries pulser data.
                final int pi = hasPulserHit ? 1 : 0;
                if(hasPulserHit) {
                    nChannelsWithPulser++;
                    // Record where the overlaid data baseline sits relative to the pedestal
                    // that samplesAboveThreshold() will subtract. Sampled here, before the
                    // MC contribution is added below.
                    double offset = 0;
                    for(int sampleN = 0; sampleN < 6; sampleN++) {
                        offset += signal[sampleN] - ((HpsSiSensor) sensor).getPedestal(channel, sampleN);
                    }
                    offset /= 6;
                    sumPulserBaselineOffset += offset;
                    sumPulserBaselineOffsetSq += offset * offset;
                    nPulserBaselineChannels++;
                }

                if(!hasPulserHit){
                    // Create a buffer to hold the extracted signal for
                    // the channel. Populate it with the appropriate
                    // pedestal values.
                    for(int sampleN = 0; sampleN < 6; sampleN++) {
                        signal[sampleN] = ((HpsSiSensor) sensor).getPedestal(channel, sampleN);
                    }
                    
                    // If noise should be added, do so.
                    if(addNoise) {
                        addNoise(sensor, channel, signal);
                    }
                }
                
                // Create a list to store truth SVT hits.
                List<SimTrackerHit> simHits = new ArrayList<SimTrackerHit>();
                // Every sim hit that deposited charge on this channel, whether or not it
                // passed G1. simHits above is the G1-surviving subset.
                List<SimTrackerHit> allContribSimHits = new ArrayList<SimTrackerHit>();

                // If there is data in the mc hit queues, process it.
                if(hitQueues[channel] != null) {
                    for(StripHit hit : hitQueues[channel]) {
                        processedHits.add(hit);
                        allContribSimHits.addAll(hit.simHits);

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
                        // ---- G1 accounting (no behaviour change) ----
                        nMCStripHits[pi]++;
                        double truthRatio = (meanNoise > 0) ? (totalContrib / meanNoise) : -1;
                        if(truthRatio >= 0) {
                            int b;
                            if(truthRatio < 1)       { b = 0; }
                            else if(truthRatio < 2)  { b = 1; }
                            else if(truthRatio < 4)  { b = 2; }
                            else if(truthRatio < 8)  { b = 3; }
                            else if(truthRatio < 16) { b = 4; }
                            else                     { b = 5; }
                            truthRatioBins[pi][b]++;
                        }

                        if(totalContrib > 4.0 * meanNoise) {
                            simHits.addAll(hit.simHits);
                            nTruthGatePass[pi]++;
                        } else {
                            nTruthGateFail[pi]++;
                            nSimHitsLostTruthGate[pi] += hit.simHits.size();
                            if(debug && debugPrinted < debugMaxPrint) {
                                debugPrinted++;
                                System.out.println("[SvtDigiTruth] G1 FAIL  pulser=" + hasPulserHit
                                        + "  sensor=" + sensor.getName() + " ch=" + channel
                                        + "  totalContrib=" + totalContrib
                                        + "  meanNoise=" + meanNoise
                                        + "  ratio=" + truthRatio
                                        + "  nSimHits=" + hit.simHits.size());
                            }
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
                    nHitsReadoutPass[pi]++;
                    // Add the hit to the readout hits collection.
                    hits.add(hit);
                    // Associate the truth hits with the raw hit and
                    // add them to the truth hits collection.
                    for(SimTrackerHit simHit : hit.getSimTrackerHits()) {
                        LCRelation hitRelation = new BaseLCRelation(hit, simHit);
                        trueHitRelations.add(hitRelation);
                        truthHits.add(simHit);
                        nRelationsWritten[pi]++;
                    }

                    // ---- provenance labelling ----
                    // Classify by what actually contributed charge, independent of G1,
                    // so that "no truth relation" resolves into a specific cause.
                    final boolean mcContrib = !allContribSimHits.isEmpty();
                    final boolean truthKept = !simHits.isEmpty();
                    final int origin;
                    if(!mcContrib) {
                        origin = hasPulserHit ? ORIGIN_PULSER_PURE : ORIGIN_NOISE;
                    } else if(hasPulserHit) {
                        origin = truthKept ? ORIGIN_MERGED : ORIGIN_MERGED_SUBTHRESH;
                    } else {
                        origin = truthKept ? ORIGIN_MC_PURE : ORIGIN_MC_PURE_SUBTHRESH;
                    }
                    nHitsByOrigin[origin]++;
                    nSimHitsByOrigin[origin] += allContribSimHits.size();

                    if(writeHitOriginCollections) {
                        if(pulserSourceHit != null) {
                            hitOriginPulserRelations.add(new BaseLCRelation(hit, pulserSourceHit));
                        }
                        // One relation per distinct contributing particle. Duplicates are
                        // dropped so the collection size counts particles, not sim hits.
                        Set<MCParticle> seen = new java.util.HashSet<MCParticle>();
                        for(SimTrackerHit simHit : allContribSimHits) {
                            MCParticle p = simHit.getMCParticle();
                            if(p == null || !seen.add(p)) { continue; }
                            hitOriginMCContribRelations.add(new BaseLCRelation(hit, p));
                            ReadoutDataManager.addParticleParents(p, extraContribParticles);
                        }
                    }
                } else {
                    // ---- G2 accounting. Truth attached here is discarded with the hit. ----
                    nHitsReadoutFail[pi]++;
                    if(!simHits.isEmpty()) {
                        nHitsReadoutFailWithTruth[pi]++;
                        nSimHitsLostReadoutCut[pi] += simHits.size();
                        if(debug && debugPrinted < debugMaxPrint) {
                            debugPrinted++;
                            double baseline = 0;
                            int nAbove = 0;
                            for(int sampleN = 0; sampleN < 6; sampleN++) {
                                double ped = ((HpsSiSensor) sensor).getPedestal(channel, sampleN);
                                double nse = ((HpsSiSensor) sensor).getNoise(channel, sampleN);
                                baseline += samples[sampleN] - ped;
                                if(samples[sampleN] - ped > nse * noiseThreshold) { nAbove++; }
                            }
                            baseline /= 6;
                            System.out.println("[SvtDigiTruth] G2 FAIL  pulser=" + hasPulserHit
                                    + "  sensor=" + sensor.getName() + " ch=" + channel
                                    + "  meanSampleMinusPed=" + baseline
                                    + "  nSamplesAboveThresh=" + nAbove
                                    + "/" + samplesAboveThreshold
                                    + "  badChannel=" + !badChannelCut(hit)
                                    + "  nSimHitsLost=" + simHits.size());
                        }
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
        // Sub-threshold contributors are referenced by the provenance relations but have
        // no truth hit, so they would otherwise be missing from the written collection.
        if(writeHitOriginCollections) {
            truthParticles.addAll(extraContribParticles);
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
        Collection<TriggeredLCIOData<?>> eventOutput = new ArrayList<TriggeredLCIOData<?>>(7);
        eventOutput.add(hitCollection);
        eventOutput.add(truthParticleData);
        eventOutput.add(truthHitCollection);
        eventOutput.add(truthRelationCollection);
        eventOutput.add(timestampData);

        if(writeHitOriginCollections) {
            TriggeredLCIOData<LCRelation> originPulserData =
                    new TriggeredLCIOData<LCRelation>(hitOriginPulserCollectionParams);
            originPulserData.getData().addAll(hitOriginPulserRelations);
            eventOutput.add(originPulserData);

            TriggeredLCIOData<LCRelation> originMCContribData =
                    new TriggeredLCIOData<LCRelation>(hitOriginMCContribCollectionParams);
            originMCContribData.getData().addAll(hitOriginMCContribRelations);
            eventOutput.add(originMCContribData);
        }

        // Return the event output.
        return eventOutput;
    }
    
    @Override
    public void endOfData() {
        String[] tag = { "no-pulser", "pulser   " };
        System.out.println();
        System.out.println("================ SvtDigitization truth-relation summary ================");
        System.out.println("  channels processed        : " + nChannelsSeen);
        System.out.println("  channels with pulser hit  : " + nChannelsWithPulser
                + fraction(nChannelsWithPulser, nChannelsSeen));
        System.out.println("  pulser queues non-null but empty (poll() -> null) : " + nPulserQueueEmptyNonNull);
        if(nPulserBaselineChannels > 0) {
            double mean = sumPulserBaselineOffset / nPulserBaselineChannels;
            double var = sumPulserBaselineOffsetSq / nPulserBaselineChannels - mean * mean;
            System.out.println("  pulser baseline - DB pedestal [ADC] : mean=" + mean
                    + "  rms=" + (var > 0 ? Math.sqrt(var) : 0.0)
                    + "   (negative eats MC headroom over threshold)");
        }
        System.out.println();
        System.out.println("  G1 = truth gate (totalContrib > 4*meanNoise)");
        System.out.println("  G2 = readoutCuts on the combined waveform");
        for(int pi = 0; pi < 2; pi++) {
            System.out.println("  ---- " + tag[pi] + " ----");
            System.out.println("    MC strip hits            : " + nMCStripHits[pi]);
            System.out.println("    G1 pass / fail           : " + nTruthGatePass[pi] + " / " + nTruthGateFail[pi]
                    + fraction(nTruthGateFail[pi], nMCStripHits[pi]) + " fail");
            System.out.println("    sim hits lost at G1      : " + nSimHitsLostTruthGate[pi]);
            System.out.println("    raw hits G2 pass / fail  : " + nHitsReadoutPass[pi] + " / " + nHitsReadoutFail[pi]);
            System.out.println("    G2 failures carrying truth : " + nHitsReadoutFailWithTruth[pi]);
            System.out.println("    sim hits lost at G2      : " + nSimHitsLostReadoutCut[pi]);
            System.out.println("    relations written        : " + nRelationsWritten[pi]);
            long lost = nSimHitsLostTruthGate[pi] + nSimHitsLostReadoutCut[pi];
            System.out.println("    total sim hits lost      : " + lost
                    + fraction(lost, lost + nRelationsWritten[pi]));
            StringBuilder sb = new StringBuilder("    totalContrib/meanNoise   : ");
            String[] edges = { "<1", "1-2", "2-4", "4-8", "8-16", ">=16" };
            for(int b = 0; b < 6; b++) {
                sb.append(edges[b]).append("=").append(truthRatioBins[pi][b]).append("  ");
            }
            System.out.println(sb.toString());
        }
        System.out.println();
        System.out.println("  ---- provenance of the raw hits that reached readout ----");
        long totOrigin = 0;
        for(int o = 0; o < nHitsByOrigin.length; o++) { totOrigin += nHitsByOrigin[o]; }
        for(int o = 0; o < nHitsByOrigin.length; o++) {
            System.out.println("    " + ORIGIN_NAME[o] + " : " + nHitsByOrigin[o]
                    + fraction(nHitsByOrigin[o], totOrigin)
                    + "   contributing sim hits = " + nSimHitsByOrigin[o]);
        }
        long untruthed = nHitsByOrigin[ORIGIN_NOISE] + nHitsByOrigin[ORIGIN_PULSER_PURE]
                + nHitsByOrigin[ORIGIN_MC_PURE_SUBTHRESH] + nHitsByOrigin[ORIGIN_MERGED_SUBTHRESH];
        long mcLostTruth = nHitsByOrigin[ORIGIN_MC_PURE_SUBTHRESH] + nHitsByOrigin[ORIGIN_MERGED_SUBTHRESH];
        System.out.println("    hits with no truth relation : " + untruthed
                + fraction(untruthed, totOrigin));
        System.out.println("      of which MC did contribute : " + mcLostTruth
                + fraction(mcLostTruth, untruthed)
                + "   <- relation lost, not absent");
        System.out.println("    origin collections written  : " + writeHitOriginCollections);
        System.out.println("=======================================================================");
        System.out.println();
        super.endOfData();
    }

    /** Formats a count as a parenthesised fraction of a total, or "" if the total is zero. */
    private static String fraction(long num, long den) {
        if(den <= 0) { return ""; }
        return String.format("  (%.4f)", (double) num / den);
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
        RawTrackerHit pulserHit;
        boolean isPulser=false;

        public StripHit(SiSensor sensor, int channel, double amplitude, double time, Set<SimTrackerHit> simHits) {
            this.sensor = sensor;
            this.channel = channel;
            this.amplitude = amplitude;
            this.time = time;
            this.simHits = simHits;
            this.isPulser=false;
        }

        public StripHit(SiSensor sensor, int channel, double time, RawTrackerHit pulserHit){
            this.sensor = sensor;
            this.channel = channel;
            this.pulserHit=pulserHit;
            this.time=time;
            this.isPulser=false;
        }

        public  boolean getIsPulser(){return this.isPulser;}
        public RawTrackerHit getRawTrackerHit(){return this.pulserHit;}
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
