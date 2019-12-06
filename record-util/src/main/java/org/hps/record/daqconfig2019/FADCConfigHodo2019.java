package org.hps.record.daqconfig2019;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator; 
import java.util.Set;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.hodoscope.HodoscopeChannel;
import org.hps.conditions.hodoscope.HodoscopeChannel.HodoscopeChannelCollection;

/**
 * Class <code>FADCConfigHodo2019</code> stores Hodoscope FADC configuration settings
 * parsed from the an EVIO file. This class manages the following
 * properties:
 * <ul>
 * <li>FADC Mode</li>
 * <li>NSA</li>
 * <li>NSB</li>
 * <li>FADC Window Width</li>
 * <li>FADC Window Time Offset</li>
 * <li>Max Pulses per Channel Window</li>
 * <li>Gains</li>
 * <li>Thresholds</li>
 * <li>Pedestals</li>
 * </ul>
 * 
 * @author Tongtong Cao <caot@jlab.org>
 */
public class FADCConfigHodo2019 extends IDAQConfig2019 {
    // Store basic FADC information.
    private int nsa         = -1;
    private int nsb         = -1;
    private int windowWidth = -1;
    private int offset      = -1;
    
    // Store a map of hodoscope channel number to hodoscope geometry indices, where indices are built by (ix, iy, ilayer, ihole).
    private Map<int[], Integer> indexChannelMap = new HashMap<int[], Integer>();
    
    // Store channel calibrations and energy conversion factors.
    private float[] gains = new float[33];
    private int[] thresholds = new int[33];
    private float[] pedestals = new float[33];
    
    // Store the hodoscope condition table for converting between
    // geometric IDs and channel objects.
    private HodoscopeChannelCollection geoMap = null;
    
    @Override
    void loadConfig(EvioDAQParser2019 parser) {
        // Store the basic FADC information.
        nsa = parser.fadcNSAHodo;
        nsb = parser.fadcNSBHodo;
        windowWidth = parser.fadcWIDTHHodo;
        offset = parser.fadcOFFSETHodo;
        
        // Get the channel collection from the database.
        DatabaseConditionsManager database = DatabaseConditionsManager.getInstance();
        geoMap = database.getCachedConditions(HodoscopeChannelCollection.class, "hodo_channels").getCachedData();
        
        // Create a mapping of hodoscope channel positions to their
        // corresponding channels. Also place the mapped values into
        // an array by their channel ID.
        indexChannelMap.clear();
        for(HodoscopeChannel hodoChannel : geoMap) {
            // Map the channel IDs to the crystal position.
            int channel = hodoChannel.getChannelId();
            int ix = hodoChannel.getIX();
            int iy = hodoChannel.getIY();
            int ilayer = hodoChannel.getLayer();
            int ihole = hodoChannel.getHole();
            indexChannelMap.put(new int[]{ix, iy, ilayer, ihole}, new Integer(channel));
            
            // Place the mapped values into the arrays.
            gains[hodoChannel.getChannelId()]      = parser.GAINHODO.get(hodoChannel);
            pedestals[hodoChannel.getChannelId()]  = parser.PEDESTALHODO.get(hodoChannel);
            thresholds[hodoChannel.getChannelId()] = parser.THRESHOLDHODO.get(hodoChannel);
        }
    }
    
    /**
     * Gets the gain for a channel.
     * @param channel - The channel object corresponding to the hodoscope channel.
     * @return Returns the gain as a <code>float</code> in units of MeV
     * per ADC.
     */
    public float getGain(HodoscopeChannel channel) {
        return getGain(channel.getChannelId());
    }
    
    /**
     * Gets the gain for a channel.
     * @param channelID - The channel ID corresponding to the hodoscope channel.
     * @return Returns the gain as a <code>float</code> in units of MeV
     * per ADC.
     */
    public float getGain(int channelID) {
        validateChannelID(channelID);
        return gains[channelID];
    }
    
    /**
     * Gets the gain for a channel.
     * @param ix - The x-index of the channel.
     * @param iy - The y-index of the channel.
     * @param ilayer - The layer-index of the channel.
     * @param ihole - The hole-index of the channel.
     * @return Returns the gain as a <code>float</code> in units of MeV
     * per ADC.
     */
    public float getGain(int ix, int iy, int ilayer, int ihole) {
        return getGain(new int[]{ix, iy, ilayer, ihole});
    }
    
    /**
     * Gets the gain for a hodoscope.
     * @param cellID - The geometric ID for the hodoscope channel.
     * @return Returns the gain as a <code>float</code> in units of MeV
     * per ADC.
     */
    public float getGain(long cellID) {
        return getGain(geoMap.findGeometric(cellID));
    }
    
    /**
     * Gets the gain for a hodoscope.
     * @param iGeo - The array representing the x/y/layer/hole-indices of the
     * hodoscope channel.
     * @return Returns the gain as a <code>float</code> in units of MeV
     * per ADC.
     */
    public float getGain(int[] iGeo) {
        // Get the channel index.
        Integer index = indexChannelMap.get(iGeo);
        
        // If the channel index was defined, return the pedestal.
        if(index != null) { return getGain(index); }
        else {
            throw new IllegalArgumentException(String.format("Channel (%3d, %3d, %3d, %3d) does not exist.", iGeo[0], iGeo[1], iGeo[2], iGeo[3]));
        }
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
     * Gets the pedestal for a channel.
     * @param channel - The channel object corresponding to the hodoscope channel.
     * @return Returns the pedestal as a <code>float</code> in units
     * of ADC.
     */
    public float getPedestal(HodoscopeChannel channel) {
        return getPedestal(channel.getChannelId());
    }
    
    /**
     * Gets the pedestal for a channel.
     * @param channelID - The channel ID corresponding to the hodoscope channel.
     * @return Returns the pedestal as a <code>float</code> in units
     * of ADC.
     */
    public float getPedestal(int channelID) {
        validateChannelID(channelID);
        return pedestals[channelID];
    }
    
    /**
     * Gets the pedestal for a hodoscope .
     * @param ix - The x-index of the channel.
     * @param iy - The y-index of the channel.
     * @param ilayer - The layer-index of the channel.
     * @param ihole - The hole-index of the channel.
     * @return Returns the pedestal as a <code>float</code> in units
     * of ADC.
     */
    public float getPedestal(int ix, int iy, int ilayer, int ihole) {
        return getPedestal(new int[]{ix, iy, ilayer, ihole});
    }
    
    /**
     * Gets the pedestal for a channel.
     * @param cellID - The geometric ID for the hodoscope channel.
     * @return Returns the pedestal as a <code>float</code> in units
     * of ADC.
     */
    public float getPedestal(long cellID) {
        return getPedestal(geoMap.findGeometric(cellID));
    }
    
    /**
     * Gets the pedestal for a channel.
     * @param iGeo - The array representing the x/y/layer/hole-indices of the
     * hodoscope channel.
     * @return Returns the pedestal as a <code>float</code> in units
     * of ADC.
     */
    public float getPedestal(int[] iGeo) {
        // Get the channel index.
        Integer index = indexChannelMap.get(iGeo);
        
        // If the channel index was defined, return the pedestal.
        if(index != null) { return getPedestal(index); }
        else {
            throw new IllegalArgumentException(String.format("Channel (%3d, %3d, %3d, %3d) does not exist.", iGeo[0], iGeo[1], iGeo[2], iGeo[3]));
        }
    }
    
    /**
     * Gets the threshold for a channel.
     * @param channel - The channel object corresponding to the hodoscope channel.
     * @return Returns the threshold as a <code>int</code> in units
     * of ADC.
     */
    public int getThreshold(HodoscopeChannel channel) {
        return getThreshold(channel.getChannelId());
    }
    
    /**
     * Gets the threshold for a channel.
     * @param channelID - The channel ID corresponding to the hodoscope channel.
     * @return Returns the threshold as a <code>int</code> in units
     * of ADC.
     */
    public int getThreshold(int channelID) {
        validateChannelID(channelID);
        return thresholds[channelID];
    }
    
    /**
     * Gets the threshold for a hodoscope .
     * @param ix - The x-index of the channel.
     * @param iy - The y-index of the channel.
     * @param ilayer - The layer-index of the channel.
     * @param ihole - The hole-index of the channel.
     * @return Returns the threshold as a <code>int</code> in units
     * of ADC.
     */
    public int getThreshold(int ix, int iy, int ilayer, int ihole) {
        return getThreshold(new int[]{ix, iy, ilayer, ihole});
    }
    
    /**
     * Gets the threshold for a crystal.
     * @param cellID - The geometric ID for the crystal.
     * @return Returns the threshold as a <code>int</code> in units
     * of ADC.
     */
    public int getThreshold(long cellID) {
        return getThreshold(geoMap.findGeometric(cellID));
    }
    
    /**
     * Gets the threshold for a crystal.
     * @param iGeo - The array representing the x/y/layer/hole-indices of the
     * crystal.
     * @return Returns the threshold as a <code>int</code> in units
     * of ADC.
     */
    public int getThreshold(int[] iGeo) {
        // Get the channel index.
        Integer index = indexChannelMap.get(iGeo);
        // If the channel index was defined, return the pedestal.
        if(index != null) { return getThreshold(index); }
        else {
            throw new IllegalArgumentException(String.format("Channel (%3d, %3d, %3d, %3d) does not exist.", iGeo[0], iGeo[1], iGeo[2], iGeo[3]));
        }
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
    public void printConfig(PrintStream ps) {
        // Print the basic configuration information.
        ps.println("FADC Configuration:");
        ps.printf("\tNSA           :: %d%n", nsa);
        ps.printf("\tNSB           :: %d%n", nsb);
        ps.printf("\tWindow Width  :: %d%n", windowWidth);
        ps.printf("\tWindow Offset :: %d%n", offset);
        
        // Output the pedestal/gain write-out header.
        ps.println("\tix\tiy\tlayer\thole\tPedestal (ADC)\tGain (MeV/ADC)\tThreshold (ADC)");
        
        Set<int[]> iGeoSet = indexChannelMap.keySet();
        Iterator<int[]> iterator = iGeoSet.iterator();                
        while (iterator.hasNext()) {    
            int[] iGeo = iterator.next();  
            int channelID = indexChannelMap.get(iGeo);
            ps.printf("\t%3d\t%3d\t%3d\t%3d\t%8.3f\t%8.3f\t%4d%n", iGeo[0], iGeo[1], iGeo[2], iGeo[3],
                    getPedestal(channelID), getGain(channelID), getThreshold(channelID));  
        }        
    }
    
    /**
     * Throws an exception if the argument channel ID is not within
     * the allowed range.
     * @param channelID - The channel ID to validate.
     */
    private static final void validateChannelID(int channelID) {
        if(channelID < 1 || channelID > 32) {
            throw new IndexOutOfBoundsException(String.format("Channel ID \"%d\" is invalid. Hodoscope Channel IDs must be between 1 and 32.", channelID));
        }
    }
}
