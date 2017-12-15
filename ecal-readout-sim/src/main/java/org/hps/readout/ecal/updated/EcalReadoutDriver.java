package org.hps.readout.ecal.updated;

import static org.hps.recon.ecal.EcalUtils.fallTime;
import static org.hps.recon.ecal.EcalUtils.maxVolt;
import static org.hps.recon.ecal.EcalUtils.nBit;
import static org.hps.recon.ecal.EcalUtils.riseTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.readout.ReadoutDataManager;
import org.hps.readout.ReadoutDriver;
import org.hps.readout.TempOutputWriter;
import org.hps.readout.util.DoubleRingBuffer;
import org.hps.readout.util.IntegerRingBuffer;
import org.hps.readout.util.LcsimCollection;
import org.hps.readout.util.LcsimSingleEventCollectionData;
import org.hps.recon.ecal.EcalUtils;
import org.hps.util.RandomGaussian;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.base.BaseRawCalorimeterHit;
import org.lcsim.event.base.BaseRawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.lcio.LCIOConstants;

/**
 * Class <code>EcalReadoutDriver</code>performs digitization of truth
 * hits from SLIC by converting them into emulated pulses and then
 * performing pulse integration. The results are output in the form
 * of {@link org.lcsim.event.RawCalorimeterHit RawCalorimeterHit}
 * objects.<br/><br/>
 * The truth hit information is retained by also producing an output
 * collection of {@link org.lcsim.event.LCRelation LCRelation}
 * objects linking the raw hits to the original {@link
 * org.lcsim.event.SimCalorimeterHit SimCalorimeterHit} objects from
 * which they were generated.
 * 
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class EcalReadoutDriver extends ReadoutDriver {
	
	// ==============================================================
	// ==== LCIO Collections ========================================
	// ==============================================================
	
	/**
	 * Indicates the name of the calorimeter geometry object. This is
	 * needed to allow access to the calorimeter channel listings.
	 */
	private String ecalGeometryName = "Ecal";
	/**
	 * The name of the input {@link org.lcsim.event.SimCalorimeterHit
	 * SimCalorimeterHit} truth hit collection from SLIC.
	 */
	private String truthHitCollectionName = "EcalHits";
	/**
	 * The name of the digitized output {@link
	 * org.lcsim.event.RawCalorimeterHit RawCalorimeterHit}
	 * collection.
	 */
	private String outputHitCollectionName = "EcalRawHits";
	/**
	 * The name of the {@link org.lcsim.event.LCRelation LCRelation}
	 * collection that links output raw hits to the SLIC truth hits
	 * from which they were generated.
	 */
	private String truthRelationCollectionName = "EcalHitTruthRelations";
	
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
	private double pePerMeV = 32.8;
	/**
	 * Defines a fixed gain to be used for all calorimeter channels.
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
	private double tp = 9.6;
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
	 */
	private int numSamplesAfter = 25;
	/**
	 * The format in which readout hits should be output.
	 */
	private int mode = 1;
	
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
	 * org.hps.readout.ecal.updated.EcalReadoutDriver#READOUT_PERIOD
	 * READOUT_PERIOD}).
	 */
	private int readoutCounter = 0;
	/**
	 * A buffer for storing pulse amplitudes representing the signals
	 * from the preamplifiers. These are stored in units of Volts
	 * with no pedestal. One buffer exists for each calorimeter
	 * channel.
	 */
	private Map<Long, DoubleRingBuffer> voltageBufferMap = new HashMap<Long, DoubleRingBuffer>();
	/**
	 * A buffer for storing ADC values representing the converted
	 * voltage values from the voltage buffers. These are stored in
	 * units of ADC and include a pedestal. One buffer exists for
	 * each calorimeter channel.
	 */
	private Map<Long, IntegerRingBuffer> adcBufferMap = new HashMap<Long, IntegerRingBuffer>();
	/**
	 * Defines the calorimeter geometry.
	 */
	private HPSEcal3 calorimeterGeometry = null;
	/**
	 * Stores the total ADC sums for each calorimeter channel that is
	 * currently undergoing integration.
	 */
	private Map<Long, Integer> channelIntegrationSumMap = new HashMap<Long, Integer>();
	/**
	 * Stores the time at which integration began on a given channel.
	 * This is used to track when the integration period has ended.
	 */
	private Map<Long, Integer> channelIntegrationTimeMap = new HashMap<Long, Integer>();
	/**
	 * Defines the time offset of objects produced by this driver
	 * from the actual true time that they should appear.
	 */
	private double localTimeOffset = 0;
	/**
	 * Cached copy of the calorimeter conditions. All calorimeter
	 * conditions should be called from here, rather than by directly
	 * accessing the database manager.
	 */
	private EcalConditions ecalConditions = null;
	/**
	 * Stores the minimum length of that must pass before a new hit
	 * may be integrated on a given channel.
	 */
	private static final int CHANNEL_INTEGRATION_DEADTIME = 32;
	/**
	 * Defines the total time range around the trigger time in which
	 * hits are output into the readout LCIO file. The trigger time
	 * position within this range is determined by {@link
	 * org.hps.readout.ecal.updated.EcalReadoutDriver#readoutOffset
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
	private LcsimCollection<RawTrackerHit> mode13HitCollectionParams;
	/**
	 * Defines the LCSim collection data for the trigger hits that
	 * are produced by this driver when it is emulating Mode-7.
	 */
	private LcsimCollection<RawCalorimeterHit> mode7HitCollectionParams;
	
	// ==============================================================
	// ==== To Be Re-Worked =========================================
	// ==============================================================
	// TODO: We should be able to define these based on the integration parameters.
	private static final int BUFFER_LENGTH = 100;
	private static final int PIPELINE_LENGTH = 2000;
	
	// ==============================================================
	// ==== Debug Output Writers ====================================
	// ==============================================================
	
	/**
	 * Outputs debug comparison data for input hits to a text file.
	 */
	private final TempOutputWriter inputWriter = new TempOutputWriter("raw_hits_input_new.log");
	/**
	 * Outputs debug comparison data for output hits to a text file.
	 */
	private final TempOutputWriter outputWriter = new TempOutputWriter("raw_hits_output_new.log");
	/**
	 * Outputs a debug dump of the ADC buffer to allow comparisons of
	 * the selected readout ADC selection range with the original
	 * driver.
	 */
	private final TempOutputWriter readoutWriter = new TempOutputWriter("raw_hits_readout_new.log");
	/**
	 * Outputs a detailed log of the driver's handling and processing
	 * of input SLIC data and integration of simulated ADC values.
	 */
	private final TempOutputWriter verboseWriter = new TempOutputWriter("raw_hits_verbose_new.log");
	
	@Override
	public void startOfData() {
		// Calculate the correct time offset. This is a function of
		// the integration samples and the output delay.
		localTimeOffset = (4 * numSamplesAfter) + 4;
		
		// Validate that a real mode was selected.
		if(mode != 1 && mode != 3 && mode != 7) {
			throw new IllegalArgumentException("Error: Mode " + mode + " is not a supported output mode.");
		}
		
		// Define the LCSim collection parameters for this driver's
		// output. Note: Since these are not persisted, the flags and
		// readout name are probably not necessary.
		LcsimCollection<RawCalorimeterHit> hitCollectionParams = new LcsimCollection<RawCalorimeterHit>(outputHitCollectionName,
				this, RawCalorimeterHit.class, getTimeDisplacement());
		hitCollectionParams.setFlags((0 + (1 << LCIOConstants.CHBIT_LONG) + (1 << LCIOConstants.RCHBIT_ID1)));
		hitCollectionParams.setReadoutName("EcalHits");
		hitCollectionParams.setPersistent(false);
		
		// Set the dependencies for the driver and register its
		// output collections with the data management driver.
		addDependency(truthHitCollectionName);
		//ReadoutDataManager.registerCollection(truthRelationCollectionName, this, LCRelation.class);
		ReadoutDataManager.registerCollection(hitCollectionParams);
		
		// Define the LCSim collection data for the on-trigger output.
		mode13HitCollectionParams = new LcsimCollection<RawTrackerHit> ("EcalReadoutHits", this, RawTrackerHit.class, 0.0);
		mode7HitCollectionParams = new LcsimCollection<RawCalorimeterHit> ("EcalReadoutHits", this, RawCalorimeterHit.class, 0.0);
		mode7HitCollectionParams.setFlags(1 << LCIOConstants.RCHBIT_TIME);
		
		// DEBUG :: Pass the writers to the superclass writer list.
		writers.add(inputWriter);
		writers.add(outputWriter);
		writers.add(readoutWriter);
		writers.add(verboseWriter);
		
		// Run the superclass method.
		super.startOfData();
		
		// DEBUG :: Write out the basic driver settings.
		verboseWriter.write("Initiating EcalReadoutDriver logging...");
		verboseWriter.write("Variable States:");
		verboseWriter.write(String.format("\t%-30s :: %s", "ecalGeometryName", ecalGeometryName));
		verboseWriter.write(String.format("\t%-30s :: %s", "truthHitCollectionName", truthHitCollectionName));
		verboseWriter.write(String.format("\t%-30s :: %s", "outputHitCollectionName", outputHitCollectionName));
		verboseWriter.write(String.format("\t%-30s :: %s", "truthRelationCollectionName", truthRelationCollectionName));
		verboseWriter.write(String.format("\t%-30s :: %b", "addNoise", addNoise));
		verboseWriter.write(String.format("\t%-30s :: %f", "pePerMeV", pePerMeV));
		verboseWriter.write(String.format("\t%-30s :: %f", "fixedGain", fixedGain));
		verboseWriter.write(String.format("\t%-30s :: %s", "pulseShape", pulseShape.toString()));
		verboseWriter.write(String.format("\t%-30s :: %f", "tp", tp));
		verboseWriter.write(String.format("\t%-30s :: %d", "integrationThreshold", integrationThreshold));
		verboseWriter.write(String.format("\t%-30s :: %d", "numSamplesBefore", numSamplesBefore));
		verboseWriter.write(String.format("\t%-30s :: %d", "numSamplesAfter", numSamplesAfter));
		verboseWriter.write(String.format("\t%-30s :: %d", "mode", mode));
		verboseWriter.write(String.format("\t%-30s :: %f", "READOUT_PERIOD", READOUT_PERIOD));
		verboseWriter.write(String.format("\t%-30s :: %d", "readoutCounter", readoutCounter));
		verboseWriter.write(String.format("\t%-30s :: %f", "localTimeOffset", localTimeOffset));
		verboseWriter.write(String.format("\t%-30s :: %d", "readoutWindow", readoutWindow));
		verboseWriter.write(String.format("\t%-30s :: %d", "readoutOffset", readoutOffset));
		verboseWriter.write(String.format("\t%-30s :: %d", "BUFFER_LENGTH", BUFFER_LENGTH));
		verboseWriter.write(String.format("\t%-30s :: %d", "PIPELINE_LENGTH", PIPELINE_LENGTH));
	}
	
	@Override
	public void detectorChanged(Detector detector) {
		// Get the readout name from the calorimeter geometry data.
		calorimeterGeometry = (HPSEcal3) detector.getSubdetector(ecalGeometryName);
		mode13HitCollectionParams.setReadoutName(calorimeterGeometry.getReadout().getName());
		mode7HitCollectionParams.setReadoutName(calorimeterGeometry.getReadout().getName());
		
		// Update the cached calorimeter conditions.
		ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();
		
		// Reinstantiate the buffers.
		resetBuffers();
	}
	
	@Override
	public void process(EventHeader event) {
		
		/*
		 * As a first step, truth energy depositions from SLIC must
		 * be obtained and converted into voltage pulse amplitudes.
		 * A buffer is maintained for the pulse amplitudes for each
		 * calorimeter channel, which must then be populated by these
		 * values for the current simulation time.
		 */
		
		// Get current SLIC truth energy depositions.
		Collection<CalorimeterHit> hits = ReadoutDataManager.getData(ReadoutDataManager.getCurrentTime(), ReadoutDataManager.getCurrentTime() + 2.0,
				truthHitCollectionName, CalorimeterHit.class);
		
		// DEBUG :: Write the truth hits seen.
		inputWriter.write("> Event " + event.getEventNumber() + " - " + ReadoutDataManager.getCurrentTime() + " (Current) - "
				+ (ReadoutDataManager.getCurrentTime() - ReadoutDataManager.getTotalTimeDisplacement(truthHitCollectionName)) + " (Local)");
		outputWriter.write("> Event " + event.getEventNumber() + " - " + ReadoutDataManager.getCurrentTime() + " (Current) - "
				+ (ReadoutDataManager.getCurrentTime() - ReadoutDataManager.getTotalTimeDisplacement(truthHitCollectionName)) + " (Local)");
		inputWriter.write("Input");
		for(CalorimeterHit hit : hits) {
			inputWriter.write(String.format("%f;%f;%d", hit.getRawEnergy(), hit.getTime(), hit.getCellID()));
		}
		
		// DEBUG :: Write the event header information and truth hit
		//          data to the verbose writer.
		verboseWriter.write("\n\n\nEvent " + event.getEventNumber() + " at time t = " + ReadoutDataManager.getCurrentTime());
		verboseWriter.write("Saw Input Truth Hits:");
		if(hits.isEmpty()) {
			verboseWriter.write("\tNone!");
		} else {
			for(CalorimeterHit hit : hits) {
				verboseWriter.write(String.format("\tHit with %f GeV at time %f in cell %d.", hit.getRawEnergy(), hit.getTime(), hit.getCellID()));
			}
		}
		
		// Truth depositions must then be converted to voltage pulse
		// amplitudes and added to the buffer. Noise is added here as
		// well, if desired.
		for(CalorimeterHit hit : hits) {
			// Get the buffer for the current truth hit's channel.
			DoubleRingBuffer eDepBuffer = voltageBufferMap.get(hit.getCellID());
			
			// Get the truth hit energy deposition.
			double energyAmplitude = hit.getRawEnergy();
			
			// If noise should be added, calculate a random value for
			// the noise and add it to the truth energy deposition.
			if(addNoise) {
				// Get the channel constants for the current channel.
				EcalChannelConstants channelData = findChannel(hit.getCellID());
				
				// Calculate a randomized noise value.
				double noise = Math.sqrt(Math.pow(channelData.getCalibration().getNoise() * channelData.getGain().getGain() * EcalUtils.MeV, 2)
						+ hit.getRawEnergy() * EcalUtils.MeV / pePerMeV);
				
				// Increment the truth energy deposition by this amount.
				energyAmplitude += RandomGaussian.getGaussian(0, noise);
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
			// pulse buffer for the calorimeter channel on which the
			// hit occurred.
			for(int i = 0; i < BUFFER_LENGTH; i++) {
				eDepBuffer.addToCell(i, energyAmplitude * pulseAmplitude((i + 1) * READOUT_PERIOD + readoutTime()
						- (ReadoutDataManager.getCurrentTime() + hit.getTime()) - findChannel(hit.getCellID()).getTimeShift().getTimeShift(), hit.getCellID()));
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
		boolean readHits = false;
		List<RawCalorimeterHit> newHits = null;
		while(ReadoutDataManager.getCurrentTime() - readoutTime() + ReadoutDataManager.getBeamBunchSize() >= READOUT_PERIOD) {
			newHits = new ArrayList<RawCalorimeterHit>();
			readHits = true;
			readoutCounter++;
		}
		
		// Only perform hit integration on each readout period.
		if(readHits) {
			// DEBUG :: Declare that hit integration is processing.
			verboseWriter.write("Starting hit integration...");
			
			// Perform hit integration as needed for each calorimeter
			// channel in the buffer map.
			for(Long cellID : voltageBufferMap.keySet()) {
				// Get the preamplifier pulse buffer for the channel.
				DoubleRingBuffer voltageBuffer = voltageBufferMap.get(cellID);
				
				// Get the ADC buffer for the channel.
				IntegerRingBuffer adcBuffer = adcBufferMap.get(cellID);
				adcBuffer.stepForward();
				
				// Get the calorimeter channel data.
				EcalChannelConstants channelData = findChannel(cellID);
				
				// Scale the current value of the preamplifier buffer
				// to a 12-bit ADC value where the maximum represents
				// a value of maxVolt.
				double currentValue = voltageBuffer.getValue() * ((Math.pow(2, nBit) - 1) / maxVolt);
				
				// Get the pedestal for the channel.
				int pedestal = (int) Math.round(channelData.getCalibration().getPedestal());
				
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
				
				// DEBUG :: Output the calculations for this channel.
				if(currentValue != 0) {
					verboseWriter.write("\tProcessing channel " + cellID);
					verboseWriter.write("\t\tcurrentValue = " + currentValue);
					verboseWriter.write("\t\tpedestal = " + pedestal);
					verboseWriter.write("\t\tdigitizedValue = " + digitizedValue);
					verboseWriter.write("\t\tpedestalSubtractedValue = " + pedestalSubtractedValue);
				}
				
				// If any readout hits exist on this channel, add the
				// current ADC values to them.
				
				// If the ADC sum is undefined, then there is not an
				// ongoing integration. If the pedestal subtracted
				// value is also over the integration threshold, then
				// integration should be initiated.
				if(sum == null && pedestalSubtractedValue > integrationThreshold) {
					// Store the current local time in units of
					// events (2 ns). This will indicate when the
					// integration started and, in turn, should end.
					channelIntegrationTimeMap.put(cellID, readoutCounter - 1);
					
					// Integrate the ADC values for a number of
					// samples defined by NSB from before threshold
					// crossing. Note that this stops one sample
					// before the current sample. This current sample
					// is handled in the subsequent code block.
					int sumBefore = 0;
					for(int i = 0; i < numSamplesBefore; i++) {
					    sumBefore += adcBuffer.getValue(-(numSamplesBefore - i - 1));
					}
					
					// This will represent the total integral sum at
					// the current point in time. Store it in the sum
					// buffer so that it may be incremented later as
					// additional samples are read.
					channelIntegrationSumMap.put(cellID, sumBefore);
					
					// DEBUG :: Indicate that integration has started.
					if(currentValue != 0) {
						verboseWriter.write("\t\tIntegration Start " + cellID);
						verboseWriter.write("\t\tNo on-going integration; pedestal-subtracted value exceeds threshold. ["
								+ pedestalSubtractedValue + " > " + integrationThreshold + "]");
						verboseWriter.write("\t\t\tCurrent value: " + sumBefore);
					}
				} else if(sum == null) {
					// DEBUG :: Indicate that nothing is being done.
					if(currentValue != 0) {
						verboseWriter.write("\t\tNo on-going integration; pedestal-subtracted value does not exceed threshold. ["
								+ pedestalSubtractedValue + " < " + integrationThreshold + "]");
					}
				}
				
				// If the integration sum is defined, then pulse
				// integration is ongoing.
				if(sum != null) {
					// If the current time is less then the total
					// integration period, the current sample should
					// be added to the total sum.
					if(channelIntegrationTimeMap.get(cellID) + numSamplesAfter >= readoutCounter - 1) {
						channelIntegrationSumMap.put(cellID, sum + adcBuffer.getValue(0));
						
						// DEBUG :: Indicate that integration is on-going.
						if(currentValue != 0) {
							verboseWriter.write("\t\tOn-going Integration " + cellID);
							verboseWriter.write("\t\t\tCurrent value: " + (sum + adcBuffer.getValue(0)));
						}
					}
					
					// If integration is complete, a hit may be added
					// to data manager.
					else if(channelIntegrationTimeMap.get(cellID) + numSamplesAfter == readoutCounter - 2) {
						newHits.add(new BaseRawCalorimeterHit(cellID, sum, 64 * channelIntegrationTimeMap.get(cellID)));
						//channelIntegrationSumMap.remove(cellID);
						
						// DEBUG :: Indicate that integration is complete.
						if(currentValue != 0) {
							verboseWriter.write("\t\tIntegration Stop " + cellID);
							verboseWriter.write("\t\t\tFinal value: " + sum);
							verboseWriter.write("\t\t\tHit time: " + (64 * channelIntegrationTimeMap.get(cellID)));
						}
					}
					
					// A channel may only start integrating once per
					// 32 ns period. Do not clear the channel for
					// integration until that time has passed.
					else if(channelIntegrationTimeMap.get(cellID) + CHANNEL_INTEGRATION_DEADTIME <= readoutCounter - 1) {
						channelIntegrationSumMap.remove(cellID);
					}
				}
				
				// Step to the next entry in the voltage buffer.
				voltageBuffer.clearValue();
				voltageBuffer.stepForward();
			}
			
			// DEBUG :: Output the raw hits that were generated in
			//          this event.
			if(newHits != null && !newHits.isEmpty()) {
				verboseWriter.write("\tProduced new raw hits:");
				for(RawCalorimeterHit rawHit : newHits) {
					verboseWriter.write("\t\tRaw hit with amplitude " + rawHit.getAmplitude() + " in channel " + rawHit.getCellID()
							+ " at system time " + rawHit.getTimeStamp() + " (" + (rawHit.getTimeStamp() / 64) + " ns).");
				}
			}
			
			ReadoutDataManager.addData(outputHitCollectionName, newHits, RawCalorimeterHit.class);
			
			// DEBUG :: Write the raw hits seen.
			outputWriter.write("Output");
			for(RawCalorimeterHit hit : newHits) {
				outputWriter.write(String.format("%d;%d;%d", hit.getAmplitude(), hit.getTimeStamp(), hit.getCellID()));
			}
		}
	}
	
	@Override
	protected Collection<LcsimSingleEventCollectionData<?>> getOnTriggerData(double triggerTime) {
		// Create a list to store the extra collections.
		List<LcsimSingleEventCollectionData<?>> collectionsList = new ArrayList<LcsimSingleEventCollectionData<?>>(1);
		
		// Get the appropriate collection of readout hits and output
		// them to the readout data manager.
		if(mode == 7) {
			List<RawCalorimeterHit> readoutHits = getMode7Hits(triggerTime);
			LcsimSingleEventCollectionData<RawCalorimeterHit> readoutData = new LcsimSingleEventCollectionData<RawCalorimeterHit>(mode7HitCollectionParams);
			readoutData.getData().addAll(readoutHits);
			collectionsList.add(readoutData);
		} else {
			List<RawTrackerHit> readoutHits = null;
			if(mode == 1) { readoutHits = getMode1Hits(triggerTime); }
			else { readoutHits = getMode3Hits(triggerTime); }
			LcsimSingleEventCollectionData<RawTrackerHit> readoutData = new LcsimSingleEventCollectionData<RawTrackerHit>(mode13HitCollectionParams);
			readoutData.getData().addAll(readoutHits);
			collectionsList.add(readoutData);
		}
		
		// Return the extra trigger collections.
		return collectionsList;
	}
	
	@Override
	protected double getTimeDisplacement() {
		return localTimeOffset;
	}
	
	@Override
	protected double getTimeNeededForLocalOutput() {
		return readoutWindow - readoutOffset;
	}
	
	/**
	 * Gets the channel parameters for a given channel ID.
	 * @param cellID - The <code>long</code> ID value that represents
	 * the channel. This is typically acquired from the method {@link
	 * org.lcsim.event.CalorimeterHit#getCellID() getCellID()} in a
	 * {@link org.lcsim.event.CalorimeterHit CalorimeterHit} object.
	 * @return Returns the channel parameters for the channel as an
	 * {@link org.hps.conditions.ecal.EcalChannelConstants
	 * EcalChannelConstants} object.
	 */
	private EcalChannelConstants findChannel(long cellID) {
		return ecalConditions.getChannelConstants(ecalConditions.getChannelCollection().findGeometric(cellID));
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
		
		// DEBUG :: Output the trigger time the ADC buffer writer.
		readoutWriter.write("> Trigger " + triggerTime);
		
		// Iterate over each channel.
		for(Long cellID : adcBufferMap.keySet()) {
			// Get the ADC values at the time of the trigger.
			short[] adcValues = getTriggerADCValues(cellID, triggerTime);
			
			// DEBUG :: Store the ADC buffer and the selected readout
			//          range into a buffer to be written out.
            StringBuffer outputData = new StringBuffer();
            outputData.append(Long.toString(cellID) + "\n");
            outputData.append("\tFull Buffer:\n");
            IntegerRingBuffer pipeline = adcBufferMap.get(cellID);
			outputData.append("\t\t");
			//for(int value : pipeline) {
			for(int i = 0; i < pipeline.size(); i++) {
				outputData.append(Long.toString(pipeline.getValue(-i)) + "[" + String.format("%4d", i) + "]");
				outputData.append("    ");
			}
			outputData.append("\n");
			
			outputData.append("\tOutput Range:\n");
			outputData.append("\t\t");
			for(short adcValue : adcValues) {
				outputData.append(adcValue);
				outputData.append("    ");
			}
			outputData.append("\n");
			
			// Get the channel constants for the current channel.
			EcalChannelConstants channelData = findChannel(cellID);
			
			// Iterate across the ADC values. If the ADC value is
			// sufficiently high to produce a hit, then it should be
			// written out.
			boolean isAboveThreshold = false;
			for(int i = 0; i < adcValues.length; i++) {
				// Check that there is a threshold-crossing at some
				// point in the ADC buffer.
				if(adcValues[i] > channelData.getCalibration().getPedestal() + integrationThreshold) {
					isAboveThreshold = true;
					break;
				}
			}
			
			// If so, create a new hit and add it to the list.
			if(isAboveThreshold) {
				hits.add(new BaseRawTrackerHit(cellID, 0, adcValues));
				
				// DEBUG :: Only write out the ADC buffer if a hit is
				//          actually generated.
				readoutWriter.write(outputData.toString() + "\n\n\n");
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
			
			// Get the channel data.
			EcalChannelConstants channelData = findChannel(cellID);
			
			for(int i = 0; i < ReadoutDataManager.getReadoutWindow(); i++) {
				if(numSamplesToRead != 0) {
					adcValues[adcValues.length - numSamplesToRead] = window[i - pointerOffset];
					numSamplesToRead--;
					if (numSamplesToRead == 0) {
					    hits.add(new BaseRawTrackerHit(cellID, thresholdCrossing, adcValues));
					}
				} else if ((i == 0 || window[i - 1] <= channelData.getCalibration().getPedestal() + integrationThreshold) && window[i]
						> channelData.getCalibration().getPedestal() + integrationThreshold) {
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
			
			// Get the channel data.
			EcalChannelConstants channelData = findChannel(cellID);
			
			// Generate Mode-7 hits.
			if(window != null) {
				for(int i = 0; i < ReadoutDataManager.getReadoutWindow(); i++) {
					if (numSamplesToRead != 0) {
						adcSum += window[i - pointerOffset];
						numSamplesToRead--;
						if(numSamplesToRead == 0) {
						    hits.add(new BaseRawCalorimeterHit(cellID, adcSum, 64 * thresholdCrossing));
						}
					} else if((i == 0 || window[i - 1] <= channelData.getCalibration().getPedestal() + integrationThreshold)
							&& window[i] > channelData.getCalibration().getPedestal() + integrationThreshold) {
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
		int readoutLatency = (int) ((ReadoutDataManager.getCurrentTime() - triggerTime) / 4.0) + readoutOffset;
		
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
	 * Returns pulse amplitude at the given time (relative to hit time). Gain is
	 * applied.
	 *
	 * @param time Units of ns. Relative to hit time (negative=before the start
	 * of the pulse).
	 * @param cellID Crystal ID as returned by hit.getCellID().
	 * @return Amplitude, units of volts/GeV.
	 */
	private double pulseAmplitude(double time, long cellID) {
		// Get the channel parameter data.
		EcalChannelConstants channelData = findChannel(cellID);
		
		//normalization constant from cal gain (MeV/integral bit) to amplitude gain (amplitude bit/GeV)
		// Determine the gain. Gain may either be fixed across all
		// channels, or be obtained from the conditions database
		// depending on the behavior defined in the steering file.
		// The gain should also be normalized.
		double gain;
		if(fixedGain > 0) {
			gain = READOUT_PERIOD / (fixedGain * EcalUtils.MeV * ((Math.pow(2, nBit) - 1) / maxVolt));
		} else {
			gain = READOUT_PERIOD / (channelData.getGain().getGain() * EcalUtils.MeV * ((Math.pow(2, nBit) - 1) / maxVolt));
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
		// Make sure that the detector geometry is properly defined.
		if(calorimeterGeometry == null) {
			throw new RuntimeException("Error: Calorimeter geometry definition was not found.");
		}
		
		// Reset each of the buffer maps.
		voltageBufferMap.clear();
		adcBufferMap.clear();
		
		// Get the set of all possible channel IDs.
		Set<Long> cells = calorimeterGeometry.getNeighborMap().keySet();
		
		// Insert a new buffer for each channel ID.
		for(Long cellID : cells) {
			EcalChannelConstants channelData = findChannel(cellID);
			voltageBufferMap.put(cellID, new DoubleRingBuffer(BUFFER_LENGTH));
			adcBufferMap.put(cellID, new IntegerRingBuffer(PIPELINE_LENGTH, (int) Math.round(channelData.getCalibration().getPedestal())));
		}
	}
	
	/**
	 * Sets whether randomized noise should be added to SLIC truth
	 * energy depositions when simulating calorimeter hits. This is
	 * <code>true</code> by default.
	 * @param state - <code>true</code> means that noise will be
	 * added and <code>false</code> that it will not.
	 */
	public void setAddNoise(boolean state) {
		addNoise = state;
	}
	
	/**
	 * Defines the name of the calorimeter geometry specification. By
	 * default, this is <code>"Ecal"</code>.
	 * @param ecalName - The calorimeter name.
	 */
	public void setEcalGeometryName(String value) {
		ecalGeometryName = value;
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
	 * Sets the number of photoelectrons per MeV of deposited energy.
	 * This value is used in the simulation of calorimeter hit noise
	 * due to photoelectron statistics. The 2014 detector has a value
	 * of 32.8 photoelectrons/MeV. This is 32.8 by default.
	 * @param value The number of photoelectrons per MeV.
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
	 * Enumerable <code>PulseShape</code> defines the allowed types
	 * of pulses that may be used to emulate the calorimeter detector
	 * response to incident energy.
	 * @author Sho Uemura <meeg@slac.stanford.edu>
	 */
	public enum PulseShape {
		CRRC, DoubleGaussian, ThreePole
	}
}