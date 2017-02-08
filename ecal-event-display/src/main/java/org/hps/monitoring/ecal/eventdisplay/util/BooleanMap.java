package org.hps.monitoring.ecal.eventdisplay.util;

import java.awt.Color;

/**
 * Class <code>BooleanMap</code> defines an implementation of the <code>
 * ColorMap</code> interface which maps values to colors based on whether
 * or not the values pass a boolean comparison.
 */
public final class BooleanMap implements ColorMap<Double> {
    // The color to display for values which pass the boolean check.
    private Color activeColor = new Color(255, 50, 50);
    // The color to display for values that fail the boolean check.
    private Color inactiveColor = Color.WHITE;
    // The critical value against which the boolean check is performed.
    private double value = 0.0;
    // The type of this boolean scale.
    private final BooleanType boolType;
    
    /**
     * <b>BooleanMap</b><br/><br/>
     * <code>public <b>BooleanMap</b>(BooleanType type, double comparisonValue)</code><br/><br/>
     * Defines a <code>ColorScale</code> which maps values to colors
     * based on a boolean comparison.
     * @param type - The type of boolean comparison to perform.
     * @param comparisonValue - The value against which the comparison
     * should be made.
     */
    public BooleanMap(BooleanType type, double comparisonValue) {
        // Make sure the comparison type is not null.
        if(type == null) { throw new IllegalArgumentException("Boolean comparison type can not be null."); }
        
        // Define the critical value and the boolean type.
        value = comparisonValue;
        boolType = type;
    }
    
    /**
     * <b>BooleanMap</b><br/><br/>
     * <code>public <b>BooleanMap</b>(BooleanType type, double comparisonValue,
     *          Color activeColor)</code><br/><br/>
     * Defines a <code>ColorScale</code> which maps values to colors
     * based on a boolean comparison.
     * @param type - The type of boolean comparison to perform.
     * @param comparisonValue - The value against which the comparison
     * should be made.
     * @param activeColor - The color in which values that pass the
     * comparison should be displayed.
     */
    public BooleanMap(BooleanType type, double comparisonValue, Color activeColor) {
        // Set the critical value and the boolean type.
        this(type, comparisonValue);
        
        // Set the active color.
        this.activeColor = activeColor;
    }
    
    /**
     * <b>BooleanMap</b><br/><br/>
     * <code>public <b>BooleanMap</b>(BooleanType type, double comparisonValue,
     *          Color activeColor, Color inactiveColor)</code><br/><br/>
     * Defines a <code>ColorScale</code> which maps values to colors
     * based on a boolean comparison.
     * @param type - The type of boolean comparison to perform.
     * @param comparisonValue - The value against which the comparison
     * should be made.
     * @param activeColor - The color in which values that pass the
     * comparison should be displayed.
     * @param inactiveColor - The color in which values that fail the
     * comparison should be displayed.
     */
    public BooleanMap(BooleanType type, double comparisonValue, Color activeColor, Color inactiveColor) {
        // Set the critical value and the boolean type.
        this(type, comparisonValue);
        
        // Set the active and inactive colors.
        this.activeColor = activeColor;
        this.inactiveColor = inactiveColor;
    }
    
    public Color getColor(Double value) {
        // If the argument is null, treat it is zero.
        if(value == null) { value = 0.0; }
        
        // If it passes the boolean comparison, return the active color.
        if(passes(value)) { return activeColor; }
        
        // Otherwise, return the inactive color.
        else { return inactiveColor; }
    }
    
    /**
     * <b>getActiveColor</b><br/><br/>
     * <code>public Color <b>getActiveColor</b>()</code><br/><br/>
     * Gets the color used by the scale for values which pass the
     * boolean comparison.
     * @return Returns the color as a <code>Color</code> object.
     */
    public Color getActiveColor() { return activeColor; }
    
    /**
     * <b>getBooleanType</b><br/><br/>
     * <code>public BooleanType <b>getBooleanType</b>()</code><br/><br/>
     * Indicates what type of boolean comparison is performed by this
     * scale.
     * @return Returns the type of comparison as a <code>BooleanType
     * </code> enumerable.
     */
    public BooleanType getBooleanType() { return boolType; }
    
    /**
     * <b>getComparisonValue</b><br/><br/>
     * <code>public double <b>getComparisonValue</b>()</code><br/><br/>
     * Gets the value against which the boolean comparisons are
     * performed.
     * @return Returns the value which is compared against.
     */
    public double getComparisonValue() { return value; }
    
    /**
     * <b>getInactiveColor</b><br/><br/>
     * <code>public Color <b>getInactiveColor</b>()</code><br/><br/>
     * Gets the color used by the scale for values which fail the
     * boolean comparison.
     * @return Returns the color as a <code>Color</code> object.
     */
    public Color getInactiveColor() { return inactiveColor; }
    
    /**
     * <b>setComparisonValue</b><br/><br/>
     * <code>public void <b>setComparisonValue</b>(double value)</code><br/><br/>
     * Sets the value against which the boolean comparison is performed.
     * @param value - The value to compare against.
     */
    public void setComparisonValue(double value) { this.value = value; }
    
    /**
     * <b>passes</b><br/><br/>
     * <code>private boolean <b>passes</b>(double d)</code><br/><br/>
     * Determines whether a given external value passes the boolean
     * check or not.
     * @param d - The external value to compare.
     * @return Returns <code>true</code> if the value passes the boolean
     * check and <code>false</code> if it does not.
     */
    private boolean passes(double d) {
        // Perform the appropriate comparison. Note that the default
        // case is included to satisfy the compiler -- it should not
        // ever actually be used.
        switch(boolType) {
            case EQUAL_TO:
                return d == value;
            case NOT_EQUAL_TO:
                return d != value;
            case GREATER_THAN:
                return d > value;
            case LESS_THAN:
                return d < value;
            case GREATER_THAN_OR_EQUAL_TO:
                return d >= value;
            case LESS_THAN_OR_EQUAL_TO:
                return d<= value;
            default:
                return false;
        }
    }
    
    /**
     * Enumerable <code>BooleanType</code> defines the type of boolean
     * comparison that is to be performed by the scale.
     */
    public enum BooleanType {
        /**
         * <b>EQUAL_TO</b><br/><br/>
         * Performs the boolean check:<br/><br/>
         * <code>[External Value] == [Comparison Value]</code>
         */
        EQUAL_TO,
        
        /**
         * <b>NOT_EQUAL_TO</b><br/><br/>
         * Performs the boolean check:<br/><br/>
         * <code>[External Value] != [Comparison Value]</code>
         */
        NOT_EQUAL_TO,
        
        /**
         * <b>GREATER_THAN</b><br/><br/>
         * Performs the boolean check:<br/><br/>
         * <code>[External Value] > [Comparison Value]</code>
         */
        GREATER_THAN,
        
        /**
         * <b>LESS_THAN</b><br/><br/>
         * Performs the boolean check:<br/><br/>
         * <code>[External Value] < [Comparison Value]</code>
         */
        LESS_THAN,
        
        /**
         * <b>GREATER_THAN_OR_EQUAL_TO</b><br/><br/>
         * Performs the boolean check:<br/><br/>
         * <code>[External Value] >= [Comparison Value]</code>
         */
        GREATER_THAN_OR_EQUAL_TO,
        
        /**
         * <b>LESS_THAN_OR_EQUAL_TO</b><br/><br/>
         * Performs the boolean check:<br/><br/>
         * <code>[External Value] <= [Comparison Value]</code>
         */
        LESS_THAN_OR_EQUAL_TO
    };
}
