package org.hps.readout.svt;

import java.util.ArrayList;
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
import org.hps.recon.tracking.PulseShape;
import org.hps.util.RandomGaussian;

public class SVTReadoutDriver extends ReadoutDriver {
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
    private double noiseThreshold = 2.0;
    private int samplesAboveThreshold = 3;
    private boolean enableThresholdCut = true;
    private boolean enablePileupCut = true;
    private boolean dropBadChannels = true;

    // Collection Names
    private String outputCollection = "SVTRawTrackerHits";
    private String relationCollection = "SVTTrueHitRelations";

    public SVTReadoutDriver() {
    	// TODO: Both of these need to reworked.
        add(readoutDriver);
        // TODO: Handle this variable.
        //triggerDelay = 100.0;
    }
	
	@Override
	public void detectorChanged(Detector detector) {
		// Run the superclass detector changed method.
		// TODO: Is this necessary?
		super.detectorChanged(detector);
		
		// Get the collection of all SiSensors from the SVT.
		sensors = detector.getSubdetector(SVT_SUBDETECTOR_NAME).getDetectorElement().findDescendants(HpsSiSensor.class);
		
		// TODO: This probably needs to be modified for the new framework.
		String[] readouts = { readout };
		readoutDriver.setCollections(readouts);
		
		// Reset the sensor queues if pile-up is enabled. Sensor
		// queues are automatically cleared as appropriate if there
		// is no pile-up, so this is not needed for that case.
		if(!noPileup) {
			for(HpsSiSensor sensor : sensors) {
				@SuppressWarnings("unchecked")
				PriorityQueue<StripHit>[] hitQueues = new PriorityQueue[sensor.getNumberOfChannels()];
				hitMap.put(sensor, hitQueues);
			}
		}
		
		// If database timing conditions are to be employed, get them
		// from the conditions database.
		if(useTimingConditions) {
			SvtTimingConstants timingConstants
					= DatabaseConditionsManager.getInstance().getCachedConditions(SvtTimingConstants.SvtTimingConstantsCollection.class,
							"svt_timing_constants").getCachedData().get(0);
			readoutOffset = 4 * (timingConstants.getOffsetPhase() + 3);
			readoutLatency = 248.0 + timingConstants.getOffsetTime();
		}
	}
	
	@Override
	public void process(EventHeader event) {
		// TODO: Probably need to use the SimTrackerHitReadoutDriver process stuff here, too.
		
		// Produce a list of StripHit objects by performing the
		// charge deposition simulation.
		List<StripHit> stripHits = doSiSimulation();
		
		// If pile-up simulation is disabled, then the event should
		// be treated individually, without influence from adjacent
		// event data.
		if(!noPileup) {
			// Add the current hits to the processing queues.
			for(StripHit stripHit : stripHits) {
				// Get the processing queue for this sensor.
				PriorityQueue<StripHit>[] hitQueues = hitMap.get(stripHit.sensor);
				
				// If it does not exist yet, instantiate it.
				if(hitQueues[stripHit.channel] == null) {
					hitQueues[stripHit.channel] = new PriorityQueue<StripHit>();
				}
				
				// Add the StripHit objects.
				hitQueues[stripHit.channel].add(stripHit);
			}
			
			// Remove any hits that are out-of-time from each sensor
			// processing queue.
			for(SiSensor sensor : sensors) {
				// Get the queue for the current sensor.
				PriorityQueue<StripHit>[] hitQueues = hitMap.get(sensor);
				
				// Iterate through the queues.
				for(int i = 0; i < hitQueues.length; i++) {
					// Undefined queues do not require pruning.
					if(hitQueues[i] != null) {
						// Remove any hits that are out-of-time.
						while(!hitQueues[i].isEmpty() && hitQueues[i].peek().time < ReadoutDataManager.getCurrentTime() - (readoutLatency + pileupCutoff)) {
							hitQueues[i].poll();
						}
						
						// If the queue is now empty, set it to null.
						if(hitQueues[i].isEmpty()) {
							hitQueues[i] = null;
						}
					}
				}
			}
			
			// If an ECal trigger is received, make hits from pipelines
			// TODO: What exactly did checkTrigger(event) even do?
			//checkTrigger(event);
		}
		
		// If pile-up is to be simulated, the effects of the present
		// hits must be added to the hit buffers.
		else {
			// Create a list to hold the analog data.
			List<RawTrackerHit> hits = new ArrayList<RawTrackerHit>();
			
			// Iterate over the hits and add them to the appropriate
			// analog buffer.
			for(StripHit stripHit : stripHits) {
				HpsSiSensor sensor = (HpsSiSensor) stripHit.sensor;
				int channel = stripHit.channel;
				double amplitude = stripHit.amplitude;
				short[] samples = new short[6];
				
				// The hit response should have a base value of the
				// pedestal. Define a sample buffer and populate it
				// thusly.
				double[] signal = new double[6];
				for(int sampleN = 0; sampleN < 6; sampleN++) {
					signal[sampleN] = sensor.getPedestal(channel, sampleN);
				}
				
				// Add noise the sample buffer, if appropriate.
				if(addNoise) {
					addNoise(sensor, channel, signal);
				}
				
				// Add to each sample buffer the appropriate signal
				// based on the hit amplitude.
				for(int sampleN = 0; sampleN < 6; sampleN++) {
					double time = sampleN * HPSSVTConstants.SAMPLING_INTERVAL - timeOffset;
					shape.setParameters(channel, (HpsSiSensor) sensor);
					signal[sampleN] += amplitude * shape.getAmplitudePeakNorm(time);
					samples[sampleN] = (short) Math.round(signal[sampleN]);
				}
				
				// Create a new hit from the sample buffer.
				long channel_id = sensor.makeChannelID(channel);
				RawTrackerHit hit = new BaseRawTrackerHit(0, channel_id, samples, new ArrayList<SimTrackerHit>(stripHit.simHits), sensor);
				
				// Add the new hit to the list of hits, if it passes
				// the readout cuts.
				if(readoutCuts(hit)) {
					hits.add(hit);
				}
			}
			
			// Set the event collection flags.
			// TODO: This should be handled earlier as part of the collection declaration in the new form.
			int flags = 1 << LCIOConstants.TRAWBIT_ID1;
			event.put(outputCollection, hits, RawTrackerHit.class, flags, readout);
		}
	}
	
	// TODO: Convert this to onTrigger
	protected void processTrigger(EventHeader event) {
		// No special trigger actions are necessary if there is no
		// pile-up simulation.
		if(noPileup) { return; }
		
		// Create a list to hold the analog data and truth relations.
		List<RawTrackerHit> hits = new ArrayList<RawTrackerHit>();
		List<LCRelation> trueHitRelations = new ArrayList<LCRelation>();
		// Calculate time of first sample
		// TODO: More getCurrentTime()
		double firstSample = Math.floor((ReadoutDataManager.getCurrentTime() - readoutLatency - readoutOffset)
				/ HPSSVTConstants.SAMPLING_INTERVAL) * HPSSVTConstants.SAMPLING_INTERVAL + readoutOffset;
		
		for(SiSensor sensor : sensors) {
			PriorityQueue<StripHit>[] hitQueues = hitMap.get(sensor);
			for(int channel = 0; channel < hitQueues.length; channel++) {
				if(!addNoise && (hitQueues[channel] == null || hitQueues[channel].isEmpty())) {
					continue;
				}
				double[] signal = new double[6];
				for(int sampleN = 0; sampleN < 6; sampleN++) {
				    signal[sampleN] = ((HpsSiSensor) sensor).getPedestal(channel, sampleN);
				}
				if(addNoise) {
					addNoise(sensor, channel, signal);
				}
				
				List<SimTrackerHit> simHits = new ArrayList<SimTrackerHit>();
				
				if(hitQueues[channel] != null) {
					for(StripHit hit : hitQueues[channel]) {
						double totalContrib = 0;
						double meanNoise = 0;
						for(int sampleN = 0; sampleN < 6; sampleN++) {
							double sampleTime = firstSample + sampleN * HPSSVTConstants.SAMPLING_INTERVAL;
							shape.setParameters(channel, (HpsSiSensor) sensor);
							double signalAtTime = hit.amplitude * shape.getAmplitudePeakNorm(sampleTime - hit.time);
							totalContrib += signalAtTime;
							signal[sampleN] += signalAtTime;
							meanNoise += ((HpsSiSensor) sensor).getNoise(channel, sampleN);
						}
						meanNoise /= 6;
						// Compare to the mean noise of the six samples instead
						if(totalContrib > 4.0 * meanNoise) {
							simHits.addAll(hit.simHits);
						}
					}
				}
				
				short[] samples = new short[6];
				for(int sampleN = 0; sampleN < 6; sampleN++) {
					samples[sampleN] = (short) Math.round(signal[sampleN]);
				}
				long channel_id = ((HpsSiSensor) sensor).makeChannelID(channel);
				RawTrackerHit hit = new BaseRawTrackerHit(0, channel_id, samples, simHits, sensor);
				if(readoutCuts(hit)) {
					hits.add(hit);
					for(SimTrackerHit simHit : hit.getSimTrackerHits()) {
					    LCRelation hitRelation = new BaseLCRelation(hit, simHit);
					    trueHitRelations.add(hitRelation);
					}
				}
			}
		}
		
		int flags = 1 << LCIOConstants.TRAWBIT_ID1;
		event.put(outputCollection, hits, RawTrackerHit.class, flags, readout);
		event.put(relationCollection, trueHitRelations, LCRelation.class, 0);
	}
    
	// TODO: Handle these.
	/*
    @Override
    public int getTimestampType() {
        return ReadoutTimestamp.SYSTEM_TRACKER;
    }

    @Override
    public double readoutDeltaT() {
        double triggerTime = ReadoutDataManager.getCurrentTime() + triggerDelay;
        // Calculate time of first sample
        double firstSample = Math.floor((triggerTime - readoutLatency - readoutOffset) / HPSSVTConstants.SAMPLING_INTERVAL) * HPSSVTConstants.SAMPLING_INTERVAL + readoutOffset;

        return firstSample;
    }
    */
	
	/**
	 * Adds randomized Gaussian noise to each sample in the signal
	 * array according to the noise parameters defined for the given
	 * sensor and channel.
	 * @param sensor - The sensor on which to simulate the noise.
	 * @param channel - The channel on which to simulate the noise.
	 * @param signal - The signal array, where each entry represents
	 * a sample.
	 */
	private void addNoise(SiSensor sensor, int channel, double[] signal) {
		for(int sampleN = 0; sampleN < 6; sampleN++) {
			signal[sampleN] += RandomGaussian.getGaussian(0, ((HpsSiSensor) sensor).getNoise(channel, sampleN));
		}
	}
	
	/**
	 * Checks whether the data from the channel on which the argument
	 * tracker hit occurred should be used.
	 * @param hit - The tracker hit to check.
	 * @return Returns <code>true</code> if the channel data should
	 * be used and <code>false</code> otherwise.
	 */
	private boolean badChannelCut(RawTrackerHit hit) {
		HpsSiSensor sensor = (HpsSiSensor) hit.getDetectorElement();
		int channel = hit.getIdentifierFieldValue("strip");
		return !sensor.isBadChannel(channel);
	}

	/**
	 * Extracts a list of {@link
	 * org.hps.readout.svt.SVTReadoutDriver.StripHit StripHit}
	 * objects from the charge deposition simulation.
	 * @return Returns the simulated <code>StripHit</code> objects
	 * in a {@link java.util.List List} collection.
	 */
	private List<StripHit> doSiSimulation() {
		// Create a list to store all simulated hits.
		List<StripHit> stripHits = new ArrayList<StripHit>();
		
		// Loop over all sensors to extract hit data.
		for(SiSensor sensor : sensors) {
			// Set the sensor that is to be used in the charge
			// deposition simulation.
			siSimulation.setSensor(sensor);
			
			// Perform the charge deposition simulation and process
			// the results.
			Map<ChargeCarrier, SiElectrodeDataCollection> electrodeDataMap = siSimulation.computeElectrodeData();
			for(ChargeCarrier carrier : ChargeCarrier.values()) {
				// If the sensor is capable of collecting the given
				// charge carrier then obtain the electrode data for
				// the sensor.
				if(sensor.hasElectrodesOnSide(carrier)) {
					// Get the electrode data, if it exists.
					SiElectrodeDataCollection electrodeDataCol = electrodeDataMap.get(carrier);
					
					// If there is no electrode data available, then
					// create a new instance of electrode data.
					if(electrodeDataCol == null) {
					    electrodeDataCol = new SiElectrodeDataCollection();
					}
					
					// Loop over all the sensor channels.
					for(Integer channel : electrodeDataCol.keySet()) {
						// Get the electrode data for this channel
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
						final double resistorValue = 100; // Ohms
						final double inputStageGain = 1.5;
						// FIXME: This should use the gains instead
						double amplitude = (charge / HPSSVTConstants.MIP) * resistorValue * inputStageGain * Math.pow(2, 14) / 2000;
						
						// Add the new hit to the list.
						stripHits.add(new StripHit(sensor, channel, amplitude, time, simHits));
					}
				}
			}
			
			// Clear the sensors of all deposited charge
			siSimulation.clearReadout();
		}
		
		// Return the simulated hits.
		return stripHits;
	}
	
	/**
	 * Requires that the second, third, and fourth sample in the hit
	 * be each higher than its predecessor. If an earlier sample is
	 * larger than the immediately subsequent sample, it is likely
	 * that the sample data contains pile-up from an earlier hit.
	 * @param hit - The hit to check.
	 * @return Returns <code>true</code> if the pile-up conditions
	 * are satisfied, and <code>false</code> otherwise.
	 */
	private boolean pileupCut(RawTrackerHit hit) {
		short[] samples = hit.getADCValues();
		return (samples[2] > samples[1] || samples[3] > samples[2]);
	}
	
	/**
	 * Performs the readout cuts(threshold cut, pile-up cut, and bad
	 * channels cut) on a {@link org.lcsim.event.RawTrackerHit
	 * RawTrackerHit} object and indicates whether it passes. This is
	 * equivalent to called {@link
	 * org.hps.readout.svt.SVTReadoutDriver#samplesAboveThreshold(RawTrackerHit)
	 * samplesAboveThreshold(RawTrackerHit)}, {@link
	 * org.hps.readout.svt.SVTReadoutDriver#pileupCut(RawTrackerHit)
	 * pileupCut(RawTrackerHit)}, and {@link 
	 * org.hps.readout.svt.SVTReadoutDriver#badChannelCut(RawTrackerHit)
	 * badChannelCut(RawTrackerHit)} individually, as appropriate.
	 * @param hit - The tracker hit to test.
	 * @return Returns <code>true</code> if the tracker hit passes
	 * all of the enabled readout cuts, and <code>false</code> if it
	 * does not.
	 */
	private boolean readoutCuts(RawTrackerHit hit) {
		// Check that the tracker hit passes each cut, if that cut is
		// enabled. If it does not, it fails the readout cuts.
		if(enableThresholdCut && !samplesAboveThreshold(hit)) {
			return false;
		} if(enablePileupCut && !pileupCut(hit)) {
			return false;
		} if(dropBadChannels && !badChannelCut(hit)) {
			return false;
		}
		
		// If it does, return true.
		return true;
	}
	
	/**
	 * Requires that a minimum number of samples exceed a minimum
	 * noise threshold.
	 * @param hit - The hit to check.
	 * @return Returns <code>true</code> if at least the minimum
	 * number of samples exceed the noise threshold, and
	 * <code>false</code> otherwise.
	 */
	private boolean samplesAboveThreshold(RawTrackerHit hit) {
		// Get the sensor and channel on which the hit occurred.
		HpsSiSensor sensor = (HpsSiSensor) hit.getDetectorElement();
		int channel = hit.getIdentifierFieldValue("strip");
		
		// Track the pedestal and noise for the channel. Variables
		// are declared externally to the loop to prevent allotting
		// additional memory for each loop instance.
		double pedestal;
		double noise;
		
		// Track the number of samples that are above the threshold.
		// A minimum number of said samples is required to pass the
		// cut.
		int count = 0;
		
		// Get the samples and iterate over them. Check whether or
		// not each sample, after accounting for pedestal and noise,
		// is above the required threshold.
		short[] samples = hit.getADCValues();
		for(int sampleN = 0; sampleN < samples.length; sampleN++) {
			// Get the pedestal and noise values for the sample.
			pedestal = sensor.getPedestal(channel, sampleN);
			noise = sensor.getNoise(channel, sampleN);
			
			// If the sample is sufficiently large, increment the
			// number of acceptable samples.
			if(samples[sampleN] - pedestal > noise * noiseThreshold) {
				count++;
			}
		}
		
		// Indicate whether there are sufficient samples above the
		// threshold to pass the cut.
		return count >= samplesAboveThreshold;
	}
    
    /**
     * Sets whether noise should be added to hits during pulse 
     * emulation. This only applies if {@link
     * org.hps.readout.svt.SVTReadoutDriver#setNoPileup(boolean)
     * setNoPileup(boolean)} is set to true.
     * @param addNoise - <code>true</code> enables noise and
	 * <code>false</code> disables it.
     */
	public void setAddNoise(boolean addNoise) {
		this.addNoise = addNoise;
	}
	
	/**
	 * Sets whether hits from bad channels should be processed.
	 * @param dropBadChannels - <code>true</code> means that hits
	 * from bad channels will be ignored, and <code>false</code> that
	 * they will be retained.
	 */
	public void setDropBadChannels(boolean dropBadChannels) {
		this.dropBadChannels = dropBadChannels;
	}
	
	/**
	 * Specifies whether hits should be checked for signs of pile-up
	 * before the are reported.
	 * @param enablePileupCut - <code>true</code> enables the cut and
	 * <code>false</code> disables it.
	 */
	public void setEnablePileupCut(boolean enablePileupCut) {
		this.enablePileupCut = enablePileupCut;
	}
	
	/**
	 * Specifies whether hits should be required to have a minimum
	 * number of samples over the noise threshold or not. The sample
	 * count and noise threshold can be set by the methods {@link
	 * org.hps.readout.svt.SVTReadoutDriver#setSamplesAboveThreshold(int)
	 * setSamplesAboveThreshold(int)} and {@link
	 * org.hps.readout.svt.SVTReadoutDriver#setNoiseThreshold(double)
	 * setNoiseThreshold(double)} respectively.
	 * @param enableThresholdCut - <code>true</code> enables the cut
	 * and <code>false</code> disables it.
	 */
	public void setEnableThresholdCut(boolean enableThresholdCut) {
		this.enableThresholdCut = enableThresholdCut;
	}
	
	/**
	 * Defines the noise threshold for testing whether a sample is
	 * above threshold or not. This is used in conjunction with
	 * {@link
	 * org.hps.readout.svt.SVTReadoutDriver#setSamplesAboveThreshold(int)
	 * setSamplesAboveThreshold(int)}. {@link
	 * org.hps.readout.svt.SVTReadoutDriver#setEnableThresholdCut(boolean)
	 * setEnableThresholdCut(boolean)} must be set <code>true</code>
	 * to enable this functionality.
	 * @param noiseThreshold - The noise threshold.
	 */
	public void setNoiseThreshold(double noiseThreshold) {
		this.noiseThreshold = noiseThreshold;
	}
	
	/**
	 * Sets whether or not to simulate pile-up.
	 * @param noPileup - <code>true</code> means that pile-up will be
	 * simulated, and <code>false</code> that it will not
	 */
	public void setNoPileup(boolean noPileup) {
		this.noPileup = noPileup;
	}
	
	/**
	 * Sets the pulse shape to be used by the simulation. Allowed
	 * values are "CR-RC" and "FourPole". The default is "FourPole".
	 * @param pulseShape - The pulse shape to use.
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
	
	// TODO: This probably should just be excised.
	public void setReadoutLatency(double readoutLatency) {
		this.readoutLatency = readoutLatency;
	}
	
	/**
	 * Sets the number of samples in a hit that must exceed the noise
	 * threshold in order for the hit to pass.  This is used in
	 * conjunction with {@link
	 * org.hps.readout.svt.SVTReadoutDriver#setNoiseThreshold(double)
	 * setNoiseThreshold(double)}. {@link
	 * org.hps.readout.svt.SVTReadoutDriver#setEnableThresholdCut(boolean)
	 * setEnableThresholdCut(boolean)} must be set <code>true</code>
	 * to enable this functionality.
	 * @param samplesAboveThreshold - The number of samples that must
	 * be above threshold.
	 */
	public void setSamplesAboveThreshold(int samplesAboveThreshold) {
		this.samplesAboveThreshold = samplesAboveThreshold;
	}
	
	/**
	 * Sets whether to use timing conditions from the conditions
	 * database. The default is false.
	 * @param useTimingConditions - <code>true</code> uses timing
	 * conditions from the database, and <code>false</code> does not.
	 */
	public void setUseTimingConditions(boolean useTimingConditions) {
		this.useTimingConditions = useTimingConditions;
	}
	
    /**
     * Represents a pulse amplitude occurring on some channel on
     * some sensor. Class stores the relevant parameters defining the
     * hit.
     */
	private class StripHit implements Comparable<Object> {
		/** The sensor on which the hit occurred. */
		SiSensor sensor;
		/** The channel on which the hit occurred. */
		int channel;
		/** The pulse amplitude of the hit. */
		double amplitude;
		/** The time at which the hit occurred. */
		double time;
		/**
		 * Stores a set of the SLiC truth hits which generated this
		 * <code>StripHit</code>.
		 */
		Set<SimTrackerHit> simHits;
		
		/**
		 * Instantiates a new <code>StripHit</code> object with the
		 * specified values.
		 * @param sensor - The sensor on which the hit occurred.
		 * @param channel - The channel on which the hit occurred.
		 * @param amplitude - The hit pulse amplitude.
		 * @param time - The hit time.
		 * @param simHits - The truth hits that generated the hit.
		 */
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
		// TODO Auto-generated method stub
		return 0;
	}
}