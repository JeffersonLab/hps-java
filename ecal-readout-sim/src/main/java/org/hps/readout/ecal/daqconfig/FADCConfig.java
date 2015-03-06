package org.hps.readout.ecal.daqconfig;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;

/**
 * Class <code>FADCConfig</code> stores FADC configuration settings
 * parsed from the an EVIO file. This class manages the following
 * properties:
 * <ul>
 * <li>FADC Mode</li>
 * <li>NSA</li>
 * <li>NSB</li>
 * <li>FADC Window Width</li>
 * <li>FADC Window Time Offset</li>
 * <li>Max Pulses per Channel Window</li>
 * <li>Crystal Gains</li>
 * <li>Crystal Thresholds</li>
 * <li>Crystal Pedestals</li>
 * </ul>
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class FADCConfig extends IDAQConfig {
	// Store basic FADC information.
	private int mode        = -1;
	private int nsa         = -1;
	private int nsb         = -1;
	private int windowWidth = -1;
	private int offset      = -1;
	private int maxPulses       = -1;
	
	// Store a map of calorimeter channel number to crystal indices.
	private Map<Point, Integer> indexChannelMap = new HashMap<Point, Integer>();
	
	// Store crystal calibrations and energy conversion factors.
	private float[] gains = new float[443];
	private float[] pedestals = new float[443];
	private int[] thresholds = new int[443];
	
	@Override
	void loadConfig(EvioDAQParser parser) {
		// Store the basic FADC information.
		mode = parser.fadcMODE;
		nsa = parser.fadcNSA;
		nsb = parser.fadcNSB;
		windowWidth = parser.fadcWIDTH;
		offset = parser.fadcOFFSET;
		maxPulses = parser.fadcNPEAK;
		
		// Get the channel collection from the database.
		DatabaseConditionsManager database = DatabaseConditionsManager.getInstance();
		EcalChannelCollection channels = database.getCachedConditions(EcalChannelCollection.class, "ecal_channels").getCachedData();
		
		// Create a mapping of calorimeter crystal positions to their
		// corresponding channels. Also place the mapped values into
		// an array by their channel ID.
		indexChannelMap.clear();
		for(EcalChannel ecalChannel : channels) {
			// Map the channel IDs to the crystal position.
			int channel = ecalChannel.getChannelId();
			int ix = ecalChannel.getX();
			int iy = ecalChannel.getY();
			indexChannelMap.put(new Point(ix, iy), new Integer(channel));
			
			// Place the mapped values into the arrays.
			gains[ecalChannel.getChannelId()]      = parser.GAIN.get(ecalChannel);
			pedestals[ecalChannel.getChannelId()]  = parser.PEDESTAL.get(ecalChannel);
			thresholds[ecalChannel.getChannelId()] = parser.THRESHOLD.get(ecalChannel);
		}
	}
	
	/**
	 * Gets the gain for a crystal.
	 * @param channel - The channel object corresponding to the crystal.
	 * @return Returns the gain as a <code>float</code> in units of MeV
	 * per ADC.
	 */
	public float getGain(EcalChannel channel) {
		return getGain(channel.getChannelId());
	}
	
	/**
	 * Gets the gain for a crystal.
	 * @param channelID - The channel ID corresponding to the crystal.
	 * @return Returns the gain as a <code>float</code> in units of MeV
	 * per ADC.
	 */
	public float getGain(int channelID) {
		validateChannelID(channelID);
		return gains[channelID];
	}
	
	/**
	 * Gets the gain for a crystal.
	 * @param ix - The x-index of the crystal.
	 * @param iy - The y-index of the crystal.
	 * @return Returns the gain as a <code>float</code> in units of MeV
	 * per ADC.
	 */
	public float getGain(int ix, int iy) {
		return getGain(new Point(ix, iy));
	}
	
	/**
	 * Gets the gain for a crystal.
	 * @param ixy - The a point representing the x/y-indices of the
	 * crystal.
	 * @return Returns the gain as a <code>float</code> in units of MeV
	 * per ADC.
	 */
	public float getGain(Point ixy) {
		return getGain(indexChannelMap.get(ixy));
	}
	
	/**
	 * Gets the maximum number of pulses that the FADC will look for
	 * in a channel's window.
	 * @return Returns the maximum number of pulses.
	 */
	public int getMaxPulses() {
		return maxPulses;
	}
	
	/**
	 * Gets the mode in which FADC data is written.
	 * @return Returns the mode as an <code>int</code>; either 1, 3,
	 * or 7.
	 */
	public int getMode() {
		return mode;
	}
	
	/**
	 * Gets the number of samples (4 ns clock-cycles) that a pulse will
	 * be integrated by the FADC after a threshold-crossing event.
	 * @return Returns the samples as an <code>int</code> in clock-cycles.
	 */
	public int getNSA() {
		return nsa;
	}
	
	/**
	 * Gets the number of samples (4 ns clock-cycles) that a pulse will
	 * be integrated by the FADC before a threshold-crossing event.
	 * @return Returns the samples as an <code>int</code> in clock-cycles.
	 */
	public int getNSB() {
		return nsb;
	}
	
	/**
	 * Gets the pedestal for a crystal.
	 * @param channel - The channel object corresponding to the crystal.
	 * @return Returns the pedestal as a <code>float</code> in units
	 * of ADC.
	 */
	public float getPedestal(EcalChannel channel) {
		return getPedestal(channel.getChannelId());
	}
	
	/**
	 * Gets the pedestal for a crystal.
	 * @param channelID - The channel ID corresponding to the crystal.
	 * @return Returns the pedestal as a <code>float</code> in units
	 * of ADC.
	 */
	public float getPedestal(int channelID) {
		validateChannelID(channelID);
		return pedestals[channelID];
	}
	
	/**
	 * Gets the pedestal for a crystal.
	 * @param ix - The x-index of the crystal.
	 * @param iy - The y-index of the crystal.
	 * @return Returns the pedestal as a <code>float</code> in units
	 * of ADC.
	 */
	public float getPedestal(int ix, int iy) {
		return getPedestal(new Point(ix, iy));
	}
	
	/**
	 * Gets the pedestal for a crystal.
	 * @param ixy - The a point representing the x/y-indices of the
	 * crystal.
	 * @return Returns the pedestal as a <code>float</code> in units
	 * of ADC.
	 */
	public float getPedestal(Point ixy) {
		return getPedestal(indexChannelMap.get(ixy));
	}
	
	/**
	 * Gets the threshold for a crystal.
	 * @param channel - The channel object corresponding to the crystal.
	 * @return Returns the threshold as a <code>int</code> in units
	 * of ADC.
	 */
	public int getThreshold(EcalChannel channel) {
		return getThreshold(channel.getChannelId());
	}
	
	/**
	 * Gets the threshold for a crystal.
	 * @param channelID - The channel ID corresponding to the crystal.
	 * @return Returns the threshold as a <code>int</code> in units
	 * of ADC.
	 */
	public int getThreshold(int channelID) {
		validateChannelID(channelID);
		return thresholds[channelID];
	}
	
	/**
	 * Gets the threshold for a crystal.
	 * @param ix - The x-index of the crystal.
	 * @param iy - The y-index of the crystal.
	 * @return Returns the threshold as a <code>int</code> in units
	 * of ADC.
	 */
	public int getThreshold(int ix, int iy) {
		return getThreshold(new Point(ix, iy));
	}
	
	/**
	 * Gets the threshold for a crystal.
	 * @param ixy - The a point representing the x/y-indices of the
	 * crystal.
	 * @return Returns the threshold as a <code>int</code> in units
	 * of ADC.
	 */
	public int getThreshold(Point ixy) {
		return getThreshold(indexChannelMap.get(ixy));
	}
	
	/**
	 * Gets the length of the readout window for the FADC in nanoseconds.
	 * @return Returns the window length.
	 */
	public int getWindowWidth() {
		return windowWidth;
	}
	
	/**
	 * Gets the time in nanoseconds that the readout window is offset.
	 * @return Returns the offset time in nanoseconds.
	 */
	public int getWindowOffset() {
		return offset;
	}
	
	@Override
	public void printConfig() {
		System.out.println("FADC Configuration:");
		System.out.printf("\tMode          :: %d%n", mode);
		System.out.printf("\tNSA           :: %d%n", nsa);
		System.out.printf("\tNSB           :: %d%n", nsb);
		System.out.printf("\tWindow Width  :: %d%n", windowWidth);
		System.out.printf("\tWindow Offset :: %d%n", offset);
	}
	
	/**
	 * Throws an exception if the argument channel ID is not within
	 * the allowed range.
	 * @param channelID - The channel ID to validate.
	 */
	private static final void validateChannelID(int channelID) {
		if(channelID < 1 || channelID > 442) {
			throw new IndexOutOfBoundsException(String.format("Channel ID \"%d\" is invalid. Channel IDs must be between 1 and 442.", channelID));
		}
	}
}
