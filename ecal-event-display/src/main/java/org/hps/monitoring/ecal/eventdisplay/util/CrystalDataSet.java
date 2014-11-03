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
    private final Preamplifier preamp;
    private final short ledChannel;
    private final byte[] ledDriver;
    private final byte fadcSlot;
    private final byte fadcChannel;
    private final byte splitterNum;
    private final byte hvGroup;
    private final byte jout;
    private final Motherboard motherboard;
    private final short channel;
    private final int gain;
    
    /**
     * Defines the data set.
     * @param ix - The crystal's x-index in the LCSimcoordinate system.
     * @param iy - The crystal's y-index in the LCSimcoordinate system.
     * @param apd - The number of the APD attached to the crystal.
     * @param preamp - The number of the crystal's preamplifier. This
     * may also include a reference wire color.
     * @param ledChannel - The channel number for the crystal's LED.
     * @param ledDriver
     * @param fadcSlot - The FADC slot the crystal occupies.
     * @param fadcChannel - The channel number through which the crystal
     * communicates with the FADC.
     * @param splitter - The number of the crystal's splitter.
     * @param hvGroup - The high voltage group for which the crystal
     * is a member.
     * @param jout - The crystal's signal channel group.
     * @param mb - The position of the motherboard associated with the
     * crystal.
     * @param channel - The channel associated with the crystal.
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
        this.motherboard = Motherboard.getMotherboard(mb);
        
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
        this.preamp = new Preamplifier(number, color);
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
    public Preamplifier getPreamplifierNumber() { return preamp; }
    
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
    
    /**
     * Gets the number of the splitter associated with the crystal.
     * @return Returns the splitter number as an <code>int</code>
     * primitive.
     */
    public int getSplitterNumber() { return splitterNum; }
    
    /**
     * Gets the crystal's high voltage group.
     * @return Returns the high voltage group number as an <code>int
     * </code> primitive.
     */
    public int getHighVoltageGroup() { return hvGroup; }
    
    /**
     * Gets the signal channel group for the crystal.
     * @return Returns the signal channel group as an <code>int</code>
     * primitive.
     */
    public int getJout() { return jout; }
    
    /**
     * Gets the positional information concerning the motherboard with
     * which the crystal is associated.
     * @return Returns the <code>Motherboard</code> enumerable attached
     * to the crystal.
     */
    public Motherboard getMotherboard() { return motherboard; }
    
    /**
     * Gets the channel number associated with the crystal.
     * @return Gets the channel number as an <code>int</code> primitive.
     */
    public int getChannel() { return channel; }
    
    /**
     * Gets the crystal's gain.
     * @return Returns the gain as an <code>int</code> primitive.
     */
    public int getGain() { return gain; }
    
    /**
     * Class <code>Preamplifier</code> represents the number
     * of a crystal's preamplifier. It can also contain a reference
     * wire color if necessary.
     * 
     * @author Kyle McCarty
     */
    public class Preamplifier implements Comparable<Preamplifier> {
        private final short number;
        private final String color;
        
        /**
         * Initializes a preamplifier with no reference wire color.
         * @param number - The preamplifier's number.
         */
        public Preamplifier(int number) { this(number, null); }
        
        /**
         * Initializes a preamplifier with the specified reference
         * wire color.
         * @param number - The preamplifier's number.
         * @param color - The reference wire color.
         */
        public Preamplifier(int number, String color) {
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
        
        @Override
        public int compareTo(Preamplifier preamp) {
            // Compare the preamplifier numbers.
            int numCompare = Short.compare(number, preamp.number);
            
            // If they are different, return this value.
            if(numCompare != 0) { return numCompare; }
            
            // The color string must now be compared. If both are null,
            // then they are the same.
            if(color == null && preamp.color == null) { return 0; }
            
            // If one is null and the other is not, the null one is
            // always ordered first.
            if(color == null && preamp.color != null) { return -1; }
            else if(color != null && preamp.color == null) { return 1; }
            
            // If neither color is null, compare them traditionally.
            return color.compareTo(preamp.color);
        }
    }
    
    /**
     * Enumerable <code>Motherboard</code> contains convenience methods
     * for defining the motherboard location for a particular crystal.
     * 
     * @author Kyle McCarty
     */
    public enum Motherboard {
        /** The motherboard on the upper, left-hand side. */
        TOP_LEFT(true, true),
        /** The motherboard on the upper, right-hand side. */
        TOP_RIGHT(true, false),
        /** The motherboard on the lower, left-hand side. */
        BOTTOM_LEFT(false, true),
        /** The motherboard on the lower, right-hand side. */
        BOTTOM_RIGHT(false, false);
        
        private final boolean isTop;
        private final boolean isLeft;
        
        /**
         * Instantiates a new motherboard enumerable.
         * @param isTop - Indicates whether the motherboard is on the
         * upper side of the detector.
         * @param isLeft - Indicates whether the motherboard is on the
         * left-hand side of the detector.
         */
        private Motherboard(boolean isTop, boolean isLeft) {
            this.isTop = isTop;
            this.isLeft = isLeft;
        }
        
        /**
         * Indicates whether the motherboard is on the top of the
         * detector or not.
         * @return Returns <code>true</code> if the motherboard is on
         * the upper half of the detector and <code>false</code> if it
         * is not.
         */
        public boolean isTop() { return isTop; }
        
        /**
         * Indicates whether the motherboard is on the bottom of the
         * detector or not.
         * @return Returns <code>true</code> if the motherboard is on
         * the lower half of the detector and <code>false</code> if it
         * is not.
         */
        public boolean isBottom() { return !isTop; }
        
        /**
         * Indicates whether the motherboard is on the left-hand side
         * of the detector or not.
         * @return Returns <code>true</code> if the motherboard is on
         * the left-hand side of the detector and <code>false</code>
         * if it is not.
         */
        public boolean isLeft() { return isLeft; }
        
        /**
         * Indicates whether the motherboard is on the right-hand side
         * of the detector or not.
         * @return Returns <code>true</code> if the motherboard is on
         * the right-hand side of the detector and <code>false</code>
         * if it is not.
         */
        public boolean isRight() { return !isLeft; }
        
        /**
         * Gets the <code>Motherboard</code> enumerable associated
         * with the given textual abbreviation. Valid arguments include
         * "LT", "RT", "LB", and "RB".
         * @param abbreviation - The textual abbreviation for the
         * location of the motherboard.
         * @return Returns the appropriate <code>Motherboard</code>
         * enumerable if given a valid abbreviation and <code>null
         * </code> otherwise.
         */
        public static final Motherboard getMotherboard(String abbreviation) {
            // Abbreviations must be 2 characters in length.
            if(abbreviation.length() != 2) { return null; }
            
            // The first character must be either 'R' or 'L'.
            boolean isLeft;
            if(abbreviation.charAt(0) == 'L') { isLeft = true; }
            else if(abbreviation.charAt(0) == 'R') { isLeft = false; }
            else { return null; }
            
            // The second character must be either 'T' or 'B'.
            boolean isTop;
            if(abbreviation.charAt(1) == 'T') { isTop = true; }
            else if(abbreviation.charAt(1) == 'B') { isTop = false; }
            else { return null; }
            
            // Return the appropriate motherboard enumerable.
            return getMotherboard(isTop, isLeft);
        }
        
        /**
         * Gets the <code>Motherboard</code> enumerable associated
         * with the given position.
         * @param isTop - <code>true</code> indicates the motherboard
         * on the top half of the detector and <code>false</code> the
         * motherboard on the bottom half.
         * @param isLeft - <code>true</code> indicates the motherboard
         * on the left-hand side of the detector and <code>false</code>
         * the motherboard on the right-hand side.
         * @return Returns the appropriate <code>Motherboard</code>
         * enumerable.
         */
        public static final Motherboard getMotherboard(boolean isTop, boolean isLeft) {
            if(isTop) {
                if(isLeft) { return TOP_LEFT; }
                else { return TOP_RIGHT; }
            }
            else {
                if(isLeft) { return BOTTOM_LEFT; }
                else { return BOTTOM_RIGHT; }
            }
        }
        
        @Override
        public String toString() {
            if(isTop) {
                if(isLeft) { return "LT"; }
                else { return "RT"; }
            }
            else {
                if(isLeft) { return "LB"; }
                else { return "RB"; }
            }
        }
    }
}