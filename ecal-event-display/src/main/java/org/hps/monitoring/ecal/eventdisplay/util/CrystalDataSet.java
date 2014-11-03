package org.hps.monitoring.ecal.eventdisplay.util;

import java.awt.Point;

/**
 * Class <code>CrystalDataSet</code> contains all of the hardware data
 * for a single calorimeter crystal as defined in the crystal hardware
 * reference sheet.
 * 
 * @author Kyle McCarty
 */
public class CrystalDataSet {
    // Data points.
    private final Point crystalIndex;
    private final short apd;
    private final PreamplifierNumber preamp;
    private final short ledChannel;
    private final byte[] ledDriver;
    private final byte fadcSlot;
    private final byte fadcChannel;
    private final byte splitterNum;
    private final byte hvGroup;
    private final byte jout;
    private final String mb;
    private final short channel;
    private final int gain;
    
    /**
     * Defines the data set.
     * @param ix - The crystal's x-index in the LCSimcoordinate system.
     * @param iy - The crystal's y-index in the LCSimcoordinate system.
     * @param apd - The number of the APD attached to the crystal.
     * @param preamp - The number of the crystal's premaplifier. This
     * may also include a reference wire color.
     * @param ledChannel - The channel number for the crystal's LED.
     * @param ledDriver
     * @param fadcSlot - The FADC slot the crystal occupies.
     * @param fadcChannel - The channel number through which the crystal
     * communicates with the FADC.
     * @param splitter
     * @param hvGroup - The high voltage group for which the crystal
     * is a member.
     * @param jout
     * @param mb
     * @param channel
     * @param gain - The crystal's gain.
     */
    public CrystalDataSet(int ix, int iy, int apd, String preamp, int ledChannel,
            double ledDriver, int fadcSlot, int fadcChannel, int splitter, int hvGroup,
            int jout, String mb, int channel, int gain) {
        // Define crystal indices.
        crystalIndex = new Point(ix, iy);
        
        // Define the general properties.
        this.apd = (short) apd;
        this.ledChannel = (short) ledChannel;
        this.fadcSlot = (byte) fadcSlot;
        this.fadcChannel = (byte) fadcChannel;
        this.splitterNum = (byte) splitter;
        this.hvGroup = (byte) hvGroup;
        this.jout = (byte) jout;
        this.channel = (short) channel;
        this.gain = gain;
        this.mb = mb;
        
        // Define the LED driver.
        this.ledDriver = new byte[2];
        this.ledDriver[0] = (byte) Math.floor(ledDriver);
        this.ledDriver[1] = (byte) ((ledDriver - this.ledDriver[0]) * 10);
        
        // Handle the preamplifier number.
        StringBuffer num = new StringBuffer();
        StringBuffer col = new StringBuffer();
        for(char c : preamp.toCharArray()) {
            if(Character.isDigit(c)) { num.append(c); }
            else { col.append(c); }
        }
        int number = Integer.parseInt(num.toString());
        String color = null;
        if(col.length() != 0) { color = col.toString(); }
        this.preamp = new PreamplifierNumber(number, color);
    }
    
    /**
     * Gets the crystal's positional indices in the LCSim coordinate
     * system.
     * @return Returns the crystal's positional indices as a <code>
     * Point</code> object.
     */
    public Point getCrystalIndex() { return crystalIndex; }
    
    /**
     * Gets the crystal's x-index in the LCSim coordinate system.
     * @return Returns the crystal's x-index as an <code>int</code>
     * primitive.
     */
    public int getCrystalXIndex() { return crystalIndex.x; }
    
    /**
     * Gets the crystal's y-index in the LCSim coordinate system.
     * @return Returns the crystal's y-index as an <code>int</code>
     * primitive.
     */
    public int getCrystalYIndex() { return crystalIndex.y; }
    
    /**
     * Gets the number of the APD attached to the crystal.
     * @return Returns the crystal's APD number as an <code>int</code>
     * primitive.
     */
    public int getAPDNumber() { return apd; }
    
    /**
     * Gets the crystal's preamplifier reference data.
     * @return Returns the preamplifier reference as a <code>
     * PreamplifierNumber</code> object.
     */
    public PreamplifierNumber getPreamplifierNumber() { return preamp; }
    
    /**
     * Gets the crystal's LED channel.
     * @return Returns the LED channel as an <code>int</code> primitive.
     */
    public int getLEDChannel() { return ledChannel; }
    
    public double getLEDDriver() {
        return ((double) ledDriver[0]) + ((double) ledDriver[1] / 10);
    }
    
    /**
     * Gets the crystal's FADC slot.
     * @return Returns the FADC slot as an <code>int</code> primitive.
     */
    public int getFADCSlot() { return fadcSlot; }
    
    /**
     * Gets the crystal's FADC channel.
     * @return Returns the FADC channel as an <code>int</code> primitive.
     */
    public int getFADCChannel() { return fadcChannel; }
    
    public int getSplitterNumber() { return splitterNum; }
    
    /**
     * Gets the crystal's high voltage group.
     * @return Returns the high voltage group number as an <code>int
     * </code> primitive.
     */
    public int getHighVoltageGroup() { return hvGroup; }
    
    public int getJout() { return jout; }
    
    public String getMB() { return mb; }
    
    public int getChannel() { return channel; }
    
    /**
     * Gets the crystal's gain.
     * @return Returns the gain as an <code>int</code> primitive.
     */
    public int getGain() { return gain; }
    
    /**
     * Class <code>PreamplifierNumber</code> represents the number
     * of a crystal's preamplifier. It can also contain a reference
     * wire color if necessary.
     * 
     * @author Kyle McCarty
     */
    public class PreamplifierNumber {
        private final short number;
        private final String color;
        
        /**
         * Initializes a preamplifier number with no reference wire
         * color.
         * @param number - The preamplifier's number.
         */
        public PreamplifierNumber(int number) { this(number, null); }
        
        /**
         * Initializes a preamplifier number with the specified reference
         * wire color.
         * @param number - The preamplifier's number.
         * @param color - The reference wire color.
         */
        public PreamplifierNumber(int number, String color) {
            this.number = (short) number;
            this.color = color;
        }
        
        /**
         * Gets the number of the preamplifier.
         * @return Returns the preamplifier number as an <code>int
         * </code> primitive.
         */
        public int getNumber() { return number; }
        
        /**
         * Gets the reference wire color associated with the crystal's
         * preamplifier if it exists.
         * @return Returns the reference wire color as a <code>String
         * </code> object or <code>null</code> if it does not exist.
         */
        public String getColor() { return color; }
        
        @Override
        public String toString() {
            if(color == null) { return "" + number; }
            else { return number + " (" + color + ")"; }
        }
    }
}