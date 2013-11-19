package org.lcsim.hps.recon.tracking.apv25;

//--- Java ---//
import java.util.HashMap;
import java.util.Map;

//--- org.lcsim ---//
import org.lcsim.hps.recon.tracking.apv25.HPSAPV25.APV25Channel.APV25AnalogPipeline;
import org.lcsim.util.aida.AIDA;

//--- hps-java ---//
import org.lcsim.hps.util.ClockSingleton;
import org.lcsim.hps.util.RingBuffer;

/**
 *
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id: HPSAPV25.java,v 1.8 2012/08/03 00:05:26 meeg Exp $
 */
public class HPSAPV25 {

	//--- APV25 parameters ---//
	//------------------------//
	private static final double APV25_FRONT_END_GAIN = 100.0; // [mV/MIP]
	private static final double APV25_MULTIPLEXER_GAIN = 1;   // mA/MIP
	private static final double APV25_NOISE_INTERCEPT_PEAK = 270; // e- RMS
	private static final double APV25_NOISE_INTERCEPT_DECON = 396;  //e- RMS
	private static final double APV25_NOISE_SLOPE_PEAK = 36;       // e- rms/pF
	private static final double APV25_NOISE_SLOPE_DECON = 59.4;       // e- rms/pF
	
	// Number of electron-hole pairs created by a min. ionizing particle 
	// in 300 micrometers of Si
	private static final int MIP = 25000;  // electron-hole pairs
	
	// Total number of channels per APV25 chip
	private static final int APV25_CHANNELS = 128;
	
	// APV25 trigger bit
	public static boolean readoutBit = false;
	
	// 
	private int apv25ShapingTime = 35; // [ns] 
	private int apv25SamplingTime = 24; // [ns]
	private double analogDCLevel = 0;  // [mA] (Pedestal)
	public int apv25ClockCycle = 0;
	private String apv25Mode = "multi-peak";
	
	// APV25 Channel
	private APV25Channel channel;
	
	// Histograms
	protected AIDA aida = AIDA.defaultInstance();

	/**
	 * Constructor
	 */
	public HPSAPV25() {
		// Create a single instance of an APV25 channel
		channel = new APV25Channel();

	}

	//--- Methods ---//
	//---------------//
	/**
	 * Set the APV25 shaping time
	 * 
	 * @param shapingTime : APV25 shaping time
	 */
	public void setShapingTime(int shapingTime) {
		apv25ShapingTime = shapingTime;
	}

	/**
	 * Set the operating mode of the APV25 to either "peak", 
	 * "deconvolution", or "multi-peak".
	 * 
	 * @param mode : APV25 operating mode 
	 */
	public void setAPV25Mode(String mode) {
		apv25Mode = mode;
	}

	/**
	 * Set the time interval at which the shaper output is sampled
	 * 
	 * @param sampleTime : time interval
	 */
	public void setSamplingTime(int sampleTime) {
		apv25SamplingTime = sampleTime;
	}

	/**
	 * 
	 */
	public void setAnalogDCLevel(double dcLevel) {
		analogDCLevel = dcLevel;
	}

	/**
	 * Return an instance of an APV25 channel. Currently, there is only a 
	 * single instance of the channel ( instead of 128 that the actual chip
	 * has).  However, the analog buffers of each channels are distinct and 
	 * are stored in a sorted map for later use.
	 *
	 * @return an instance of APV25Channel
	 */
	public APV25Channel getChannel() {
		return channel;
	}

	/**
	 * Inject charge into a channel and shape the signal.  The resulting 
	 * shaper signal is then sampled into the analog pipeline
	 * 
	 * @param charge   : Total charge being injected
	 * @param pipeline : Analog pipeline associated with a channel
	 */
	public void injectCharge(double charge, double noiseRMS, APV25AnalogPipeline pipeline) {
		
		// Shape the injected charge
		getChannel().shapeSignal(charge);

		// Sample the resulting shaper signal
		getChannel().sampleShaperSignal(pipeline, noiseRMS);
	}

	/**
	 * Increment the position of the trigger and writer pointers of all
	 * channels
	 * 
	 * @param analogPipelineMap : 
	 */
	public void incrementAllPointerPositions(Map<Integer, APV25AnalogPipeline> analogPipelineMap) {
		// Loop through all of the channels and increment the position of
		// all the trigger and writer pointers
		for (Map.Entry<Integer, APV25AnalogPipeline> entry : analogPipelineMap.entrySet()) {
			entry.getValue().step();
		}
	}

	/**
	 * 
	 */
	public int getSamplingTime() {
		return apv25SamplingTime;
	}

	/**
	 * Increment the APV25 clock cycle by one
	 */
	public void stepAPV25Clock() {
		apv25ClockCycle += 1;
	}

	/**
	 * 
	 */
	public Map<Integer, double[]> APV25Multiplexer(
			Map<Integer, APV25AnalogPipeline> pipelineMap) {

		Map<Integer /* chip */, double[]> apv25Map = new HashMap<Integer, double[]>();

		// The address of the APV25.  There is only a single address for all
		// chips
		double[] apv25Address = {4.0, 4.0, 4.0, 4.0, 4.0, 4.0, 4.0, 4.0};
		double[] output;

		// Create the data streams
		for (int chipIndex = 0; chipIndex < Math.ceil((double) pipelineMap.size() / APV25_CHANNELS); chipIndex++) {
			apv25Map.put(chipIndex, createOutputArray(apv25Address, -4));
		}

		// Loop over all channels and readout the cells which the
		// trigger pointer points to
		for (int channelN = 0; channelN < pipelineMap.size(); channelN++) {
			output = apv25Map.get((int) Math.floor(channelN / APV25_CHANNELS));
			output[channelN % 128 + 12] += (pipelineMap.get(channelN).readOut() / APV25_FRONT_END_GAIN) * APV25_MULTIPLEXER_GAIN + analogDCLevel;
		}
		return apv25Map;
	}

	/**
	 * 
	 */
	private double[] createOutputArray(double[] address, double error) {

		// Create the array which will contain the output format. The array values
		// will range from -4 microAmps to 4 microAmps.
		double[] output = new double[141];
		for (int index = 0; index < output.length; index++) {
			output[ index] = -4.0;  // microAmps
		}

		// Header
		double[] header = {4.0, 4.0, 4.0};

		// Fill the array with the header, address and error bit and tick
		System.arraycopy(header, 0, output, 0, 3);
		output[header.length] = error;
		System.arraycopy(address, 0, output, 4, 8);
		output[ output.length - 1] = 4.0;

		return output;
	}

	//------------------------------------------//
	//               APV25 Channel            //
	//-----------------------------------------//
	public class APV25Channel {

		// Analog pipeline length
		private static final int ANALOG_PIPELINE_LENGTH = 192;
		// Shaper signal
		private APV25ShaperSignal shaperSignal;

		/**
		 * Constructor 
		 */
		public APV25Channel() {
		}

		/**
		 * Shape the injected charge
		 * 
		 * @param charge 
		 */
		private void shapeSignal(double charge) {
			shaperSignal = new APV25ShaperSignal(charge);
		}

		/**
		 * Return the noise in electrons for a given strip capacitance
		 * 
		 * @param capacitance : strip capacitance in pF
		 * @return noise in electrons
		 */
		public double computeNoise(double capacitance) {
			if (apv25Mode.equals("peak") || apv25Mode.equals("multi-peak"))
				return APV25_NOISE_INTERCEPT_PEAK + APV25_NOISE_SLOPE_PEAK * capacitance;
			else return APV25_NOISE_INTERCEPT_DECON + APV25_NOISE_SLOPE_DECON * capacitance;
		}

		/**
		 * Sample the shaper signal and fill the analog pipeline.
		 * 
		 * @param channel : Channel number
		 */
		private void sampleShaperSignal(
				APV25AnalogPipeline pipeline, double noiseRMS) {

			// Obtain the beam time
			double beam_time = ClockSingleton.getTime();

			// Fill the analog pipeline starting with the cell to which 
			// the writer pointer is pointing to.  Signals arriving within 
			// the same bucket of length apv25SamplingTime will be shifted
			// in time depending on when they arrive.
			for (int cell = 0; cell < ANALOG_PIPELINE_LENGTH; cell++) {

				// Time at which the shaper signal will be sampled
				int sample_time = cell * apv25SamplingTime
						- ((int) beam_time) % apv25SamplingTime;
				// Sample the shaper signal
				double sample = shaperSignal.getAmplitudeAtTime(sample_time);

				pipeline.addToCell(cell, sample);
			}
		}

		//-----------------------------------//
		//---     APV25 Shaper Signal     ---//
		//-----------------------------------//
		/**
		 * 
		 */
		private class APV25ShaperSignal {

			// Shaper signal maximum amplitude
			private double maxAmp = 0;

			/**
			 * Constructor
			 * 
			 * @param charge : input charge into the channel
			 */
			APV25ShaperSignal(double charge) {

				maxAmp = (charge / MIP) * APV25_FRONT_END_GAIN;  // mV

				//--->
				aida.histogram1D("Shaper Signal Max Amplitude", 100, 0, 500).fill(maxAmp);
				//--->
			}

			/**
			 * Get the amplitude at a time t
			 * 
			 * @param time : time at which the shaper signal is to be
			 *               sampled
			 */
			private double getAmplitudeAtTime(double time) {
				return maxAmp * (Math.max(0, time) / apv25ShapingTime) * Math.exp(1 - (time / apv25ShapingTime));
			}
		}

		//-------------------------------------//
		//---    APV25 Analog Pipeline      ---//
		//-------------------------------------//
		// Note that the buffer is modeled after a circular buffer
		public class APV25AnalogPipeline extends RingBuffer {
			private int _trigger_latency = (int) Math.floor(270 / apv25SamplingTime);

			public APV25AnalogPipeline() { super(ANALOG_PIPELINE_LENGTH); }

			/**
			 * Set the trigger latency
			 * 
			 * @param latency : trigger latency in [ns]
			 */
			public void setTriggerLatency(int latency) {
				_trigger_latency = (int) Math.floor(latency / apv25SamplingTime);
			}

			private double readOut() {
				double pipelineValue = currentValue();
				array[0] = 0;
				return pipelineValue;
			}

			@Override
			public void addToCell(int position, double element) {
				if (position + _trigger_latency > size) {
					return;
				}
				super.addToCell(position + _trigger_latency, element);
			}

			public void printAnalogPipeline() {
				System.out.print("[ ");
				for (int index = 0; index < size; index++) {
					if (index == ptr) {
						System.out.print("TP====>");
					}
					if (index == ptr + _trigger_latency) {
						System.out.print("WP====>");
					}
					System.out.print(array[index] + ", ");
				}
				System.out.println("] ");
			}
		}
	}
}
