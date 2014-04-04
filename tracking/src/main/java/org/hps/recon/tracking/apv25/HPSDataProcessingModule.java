package org.hps.recon.tracking.apv25;

//--- Java ---//
import static org.hps.conditions.deprecated.HPSSVTConstants.SVT_TOTAL_FPGAS;
import static org.hps.conditions.deprecated.HPSSVTConstants.TOTAL_APV25_CHANNELS;
import static org.hps.conditions.deprecated.HPSSVTConstants.TOTAL_APV25_PER_HYBRID;
import static org.hps.conditions.deprecated.HPSSVTConstants.TOTAL_HYBRIDS_PER_FPGA;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
//--- org.lcsim ---//
import java.util.Set;

import org.hps.conditions.deprecated.SvtUtils;
import org.hps.readout.svt.SVTData;
import org.lcsim.detector.IReadout;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.base.BaseRawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
//--- Constants ---//

/**
 *
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id: HPSDataProcessingModule.java,v 1.3 2013/03/15 21:05:28 meeg Exp $
 */
public class HPSDataProcessingModule extends Driver {
	
	// A map relating a sensor to all sample blocks collected from that sensor
	Map<SiSensor, Map<Integer, List<Double>>> sensorToSamplesMap;

	// Relate a channel to its six samples
	Map<Integer, List<Double>> channelToBlock;
	
	// Relate a sensor Identifier to the actual sensor
	Map<Long, SiSensor> sensorMap = new HashMap<Long, SiSensor>();
	
	// Collection of all sensors
	Set<SiSensor> sensorSet = new HashSet<SiSensor>();
	
	// Collections of RawTrackerHits
	List<RawTrackerHit> rawHits;        // Cuts are applied
	List<RawTrackerHit> rawHitsNoCuts;  // No cuts are applied to samples
	
	// Collection of all SVT data
	List<SVTData> svtData;
	List<SVTData> svtFpgaData;
	List<Double> samples;

	int numberOfSamples = 0;        // Total number of APV25 samples
	int nSamplesAboveThresh = 3;    // Number of samples above noise threshold
	int pedestal = 1638;            // [ADC counts] For now, all channels have the same pedestal
	int flags = 0;                  //
	int noise = 18;                 // [ADC Counts] RMS noise 
	int noiseThreshold = 3;         // Units of RMS noise
	int physicalChannel;
	
	private boolean thresholdCut = false;       // Apply threshold cut?
	private boolean tailCut = false;            // Apply tail cut?
	private boolean noiseSuppression = false;   // Apply noise suppression?
	boolean debug = false;
	
	double[] apv25DataStream;
	
	String RawTrackerHitsCollectionName = "RawTrackerHits";
	String RawTrackerHitsNoCutsCollectionName = "RawTrackerHitsNoCuts";
	String svtCollectionName = "SVTData";

	/**
	 * Default Constructor
	 */
	public HPSDataProcessingModule() {
		channelToBlock = new HashMap<Integer, List<Double>>();
		sensorToSamplesMap = new HashMap<SiSensor, Map<Integer, List<Double>>>();
		rawHits = new ArrayList<RawTrackerHit>();
		rawHitsNoCuts = new ArrayList<RawTrackerHit>();
		svtData = new ArrayList<SVTData>();
		svtFpgaData = new ArrayList<SVTData>();
	}

	/**
	 * 
	 */
	@Override
	public void detectorChanged(Detector detector) {

		for (SiSensor sensor : SvtUtils.getInstance().getSensors()) {
			// Map a sensor to its corresponding samples
			sensorToSamplesMap.put(sensor, new HashMap<Integer, List<Double>>());
		}
	}

	/**
	 * Set the SVT collection name
	 */
	public void setSvtCollectionName(String svtCollectionName) {
		this.svtCollectionName = svtCollectionName;
	}

	/**
	 * Set the number of samples above threshold a signal must have
	 */
	public void setSamplesAboveThresh(int nSamplesAboveThresh) {
		this.nSamplesAboveThresh = nSamplesAboveThresh;
	}

	/**
	 * Set the noise RMS [ADC Counts]
	 */
	public void setNoise(int noise) {
		this.noise = noise;
	}

	/**
	 * Set the noise threshold in units of RMS noise
	 */
	public void setNoiseThreshold(int noiseThreshold) {
		this.noiseThreshold = noiseThreshold;
	}

	/**
	 * Set the pedestal value for all channels
	 */
	public void setPedestal(int pedestal) {
		this.pedestal = pedestal;
	}

	/**
	 * Enable the threshold cut.  The threshold cut requires a certain number
	 * of samples per hit to be above a noise threshold.
	 */
	public void enableThresholdCut() {
		this.thresholdCut = true;
	}

	/**
	 * Enable the tail cut.  The tail cut requires sample 1 to be greater than
	 * sample 0 or sample 2 to be greater than sample 1. This eliminates 
	 * hits that may arise due to shaper signal tails.
	 */
	public void enableTailCut() {
		this.tailCut = true;
	}

	/**
	 * Enable noise suppression cut.  Requires samples 2 or 3 to be above a
	 * threshold noiseThreshold + noise. 
	 */
	public void enableNoiseSuppressionCut() {
		this.noiseSuppression = true;
	}

	/**
	 * Buffer a sample that has been readout from a sensor.
	 * 
	 * @param sensorToDigitalMap
	 *      A map relating a sensor to the digital samples readout from the 
	 *      sensor
	 */
	public void addSample(Map<SiSensor, Map<Integer, double[]>> sensorToDigitalMap) {

		/*
		 * Integer:  Chip Number
		 * double[]: APV25 Data Analog Data 
		 */

		int physicalChannel;

		// Loop through the list of all sensors
		for (Map.Entry<SiSensor, Map<Integer, double[]>> sensor : sensorToDigitalMap.entrySet()) {

			// Loop through all APV25s
			for (Map.Entry<Integer, double[]> chipData : sensor.getValue().entrySet()) {

				// Copy the sample to avoid concurrent modification
				apv25DataStream = chipData.getValue();

				// Strip the APV25 data stream of all header information
				apv25DataStream = Arrays.copyOfRange(apv25DataStream, 12, apv25DataStream.length - 1);

				// Loop through all channels
				for (int channel = 0; channel < apv25DataStream.length; channel++) {

					physicalChannel = channel + chipData.getKey() * 128;

					// Check if a block has been created for this channel. If not create it
					if (!sensorToSamplesMap.get(sensor.getKey()).containsKey(physicalChannel)) {
						sensorToSamplesMap.get(sensor.getKey()).put(physicalChannel, new ArrayList<Double>(6));
					}
					sensorToSamplesMap.get(sensor.getKey()).get(physicalChannel).add(apv25DataStream[channel]);
				}
			}
		}
		numberOfSamples++;
	}

	/**
	 *  Finds hits that satisfied all required cuts and creates both
	 *  RawTrackerHits and SVTData
	 */
	public void findHits() {

		int fpgaNumber, hybridNumber, apvNumber, rawChannel;
		
		// Clear the list of raw tracker hits
		rawHits.clear();
		rawHitsNoCuts.clear();
		svtData.clear();
		
		// Loop through all sensors and the corresponding blocks
		for (Map.Entry<SiSensor, Map<Integer, List<Double>>> sensor : sensorToSamplesMap.entrySet()) {

			// Get the FPGA number
			fpgaNumber = SvtUtils.getInstance().getFPGA(sensor.getKey());
			if(fpgaNumber > SVT_TOTAL_FPGAS || fpgaNumber < 0)
				throw new RuntimeException("FPGA Number out of range!");
			if(debug) System.out.println(this.getClass().getSimpleName() + ": FPGA Number: " + fpgaNumber);

			// Clear the temporary list
			svtFpgaData.clear();
			
			for (Map.Entry<Integer, List<Double>> samples : sensor.getValue().entrySet()) {
				short[] adc = new short[6];

				// Convert ADC value to a short
				for (int index = 0; index < adc.length; index++) 
					adc[index] = samples.getValue().get(index).shortValue();

				// If a strip had any charge deposited on it, create a RawTrackerHit
				if(!(samplesAboveThreshold(adc) >= 1)){
					samples.getValue().clear();
					continue;
				}
					
				RawTrackerHit rawHit = makeRawTrackerHit(samples.getKey(), sensor.getKey(), adc);
				rawHitsNoCuts.add(rawHit);
				
				// Check if a block has the appropriate number of blocks above threshold
				if (thresholdCut && !(samplesAboveThreshold(adc) >= nSamplesAboveThresh)) {
					samples.getValue().clear();
					continue;
				}

				// Apply the tail cut
				if (tailCut && !tailCut(adc)) {
					samples.getValue().clear();
					continue;
				}

				// Apply noise suppression cut
				if (noiseSuppression && !noiseSuppressionCut(adc)) {
					samples.getValue().clear();
					continue;
				}

				// If all cuts are satisfied, add the hit to the list of hits to be saved
				rawHits.add(rawHit);

				// Get the hybrid number
				hybridNumber = SvtUtils.getInstance().getHybrid(sensor.getKey());
				if(hybridNumber > TOTAL_HYBRIDS_PER_FPGA || hybridNumber < 0)
					throw new RuntimeException("Hybrid number is out of range!");
				//if(debug) System.out.println(this.getClass().getSimpleName() + ": Hybrid Number: " + hybridNumber);

				// Find the APV number. Note that strip numbering is from 639 to 0
				apvNumber = (TOTAL_APV25_PER_HYBRID - 1) - (int) Math.floor(samples.getKey()/128);
				if(apvNumber > TOTAL_APV25_PER_HYBRID || apvNumber < 0) 
					throw new RuntimeException("APV25 Number out of range!");
				//if(debug) System.out.println(this.getClass().getSimpleName() + ": APV Number: " + apvNumber);

				// Find the raw channel number from the physical channel
				rawChannel = samples.getKey() - (TOTAL_APV25_CHANNELS*TOTAL_APV25_PER_HYBRID - 1) 
						+ apvNumber*TOTAL_APV25_CHANNELS + (TOTAL_APV25_CHANNELS - 1); 
				if(rawChannel > TOTAL_APV25_CHANNELS || rawChannel < 0)
					throw new RuntimeException("APV25 Channel " + rawChannel + " out of range!");
				//if(debug) System.out.println(this.getClass().getSimpleName() + ": Raw Channel Number: " + rawChannel);
				
				// Create an svtData packet
				SVTData data = new SVTData(hybridNumber, apvNumber, rawChannel, fpgaNumber, adc);
				svtData.add(data);
				svtFpgaData.add(data);
				
				samples.getValue().clear();
			}

			HPSSVTDataBuffer.addToBuffer(svtFpgaData, fpgaNumber);
		}
		if(debug) System.out.println(this.getClass().getName() + ": Total RawTrackerHits before cuts: " + rawHitsNoCuts.size());
		if(debug) System.out.println(this.getClass().getName() + ": Total RawTrackerHits: " + rawHits.size());
		if(debug) System.out.println(this.getClass().getName() + ": Total SVTData: " + svtData.size());
	}

	/**
	 * Creates a rawTrackerHit
	 * 
	 * @param channelNumber
	 *      Si Strip from which the hit originates from
	 * @param sensor
	 *      Sensor from which the hit originates from
	 * @param adcValues
	 *      Shaper signal samples
	 * @return RawTrackerHit
	 */
	private RawTrackerHit makeRawTrackerHit(Integer channelNumber, SiSensor sensor, short[] adcValues) {
		IReadout ro = sensor.getReadout();

		// No time yet
		int time = 0;
		long cell_id = sensor.makeStripId(channelNumber, 1).getValue();

		RawTrackerHit rawHit = new BaseRawTrackerHit(time, cell_id, adcValues, new ArrayList<SimTrackerHit>(), sensor);

		ro.addHit(rawHit);

		return rawHit;
	}

	/**
	 * Finds how many samples are above a given threshold
	 * 
	 * @param adc
	 *      Shaper signal samples
	 * @return Number of samples above threshold
	 */
	private int samplesAboveThreshold(short[] adc) {
		// Number of samples above threshold
		int nSamplesAboveThreshold = 0;

		for (int sample = 0; sample < adc.length; sample++) {
			if (adc[sample] >= pedestal + noiseThreshold * noise) {
				nSamplesAboveThreshold++;
			}
		}
		return nSamplesAboveThreshold;
	}

	/**
	 * Applies tail cut
	 * 
	 * @param adc
	 *      Shaper signal samples
	 * @return true if the cut is satisfied, false otherwise
	 */
	private boolean tailCut(short[] adc) {
		if (adc[3] > adc[2] || adc[4] > adc[3]) {
			return true;
		}
		return false;
	}

	/**
	 * Applies noise suppression cut
	 * 
	 * @param adc 
	 *      Shaper signal samples
	 * @return true if the cut is satisfied, false otherwise
	 */
	private boolean noiseSuppressionCut(short[] adc) {
		if (adc[3] > pedestal + noiseThreshold * noise || adc[4] > pedestal + noiseThreshold * noise) {
			return true;
		}
		return false;
	}

	/**
	 * 
	 */
	@Override
	public void process(EventHeader event) {
		super.process(event);

		// If six samples have been collected process the data
		if (numberOfSamples == 6) {

			// Find hits
			findHits();

			// Add RawTrackerHits to the event
			event.put(RawTrackerHitsCollectionName, rawHits, RawTrackerHit.class, flags);

			// Add SVTData to event
			System.out.println("Adding SVTData Collection of size: " + svtData.size() + " to the Event");
			event.put(this.svtCollectionName, this.svtData, SVTData.class, 0);

			
			//
			numberOfSamples = 0;
		}
	}
}
