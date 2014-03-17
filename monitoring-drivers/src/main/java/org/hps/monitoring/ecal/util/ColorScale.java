package org.hps.monitoring.ecal.util;

/**
 * The abstract class <code>ColorScale</code> contains shared methods
 * for implementations of the <code>ColorMap</code> interface which
 * map based on position in a range of values.
 * 
 * @author Kyle McCarty
 **/
public abstract class ColorScale implements ColorMap<Double> {
    // Indicates if linear or logarithmic scaling should be used.
    protected boolean linear = true;
    // The minimum value for color scaling.
    protected double min = 0;
    // The maximum value for color scaling.
    protected double max = 1;
    // A scale variable used for mapping logarithmic values.
    protected double scale = 1.0;
    // An efficiency variable used for logarithmic mapping.
    protected double lMin = 0.0;
    // An efficiency variable used for logarithmic mapping.
    protected double lMax = 1.0;
    
    /**
     * <b>getMaximum</b><br/><br/>
     * <code>public double <b>getMaximum</b>()</code><br/><br/>
     * Gets the highest value for which color scaling is performed.
     * @return Returns the maximum value for color scaling as a <code>double</code>.
     **/
    public double getMaximum() { return max; }
    
    /**
     * <b>getMinimum</b><br/><br/>
     * <code>public double <b>getMinimum</b>()</code><br/><br/>
     * Gets the lowest value for which color scaling is performed.
     * @return Returns the minimum value for color scaling as a <code>double</code>.
     **/
    public double getMinimum() { return min; }
    
    /**
     * <b>getScaledMaximum</b><br/><br/>
     * <code>public double <b>getScaledMaximum</b>()</code><br/><br/>
     * Gets the highest for which color scaling is performed, scaled
     * based on scale type.
     * @return Returns the maximum value for color scaling as a <code>
     * double</code>, if scaling is linear, and the logarithm of this
     * value, if scaling is logarithmic.
     */
    public double getScaledMaximum() {
    	if(linear) { return max; }
    	else { return lMax; }
    }
    
    /**
     * <b>getScaledMinimum</b><br/><br/>
     * <code>public double <b>getScaledMinimum</b>()</code><br/><br/>
     * Gets the lowest for which color scaling is performed, scaled
     * based on scale type.
     * @return Returns the minimum value for color scaling as a <code>
     * double</code>, if scaling is linear, and the logarithm of this
     * value, if scaling is logarithmic.
     */
    public double getScaledMinimum() {
    	if(linear) { return min; }
    	else { return lMin; }
    }
    
    /**
     * <b>isLinear</b><br/><br/>
     * <code>public boolean <b>isLinear</b>()</code><br/><br/>
     * Indicates whether this color mapping is linear or not.
     * @return Returns <code>true</code> if this is a linear mapping and <code>false
     * </code> otherwise.
     **/
    public boolean isLinear() { return linear; }
    
    /**
     * <b>isLogairthmic</b><br/><br/>
     * <code>public boolean <b>isLogairthmic</b>()</code><br/><br/>
     * Indicates whether this color mapping is logarithmic or not.
     * @return Returns <code>true</code> if this is a logarithmic mapping and <code>
     * false</code> if it is not.
     **/
    public boolean isLogarithmic() { return !linear; }
    
    /**
     * <b>setMaximum</b><br/><br/>
     * <code>public void <b>setMaximum</b>(double maximum)</code><br/><br/>
     * Sets the value over which no color scaling will be performed.
     * @param maximum - The highest value for which color scaling will be performed.
     **/
    public void setMaximum(double maximum) {
        max = maximum;
        revalidate();
    }
    
    /**
     * <b>setMinimum</b><br/><br/>
     * <code>public void <b>setMinimum</b>(double minimum)</code><br/><br/>
     * Sets the value under which no color scaling will be performed.
     * @param minimum - The lowest value for which color scaling will be performed.
     **/
    public void setMinimum(double minimum) {
        min = minimum;
        revalidate();
    }
    
    /**
     * <b>setScalingLinear</b><br/><br/>
     * <code>public void <b>setScalingLinear</b>()</code><br/><br/>
     * Sets the scaling behavior to linear.
     **/
    public void setScalingLinear() {
        linear = true;
        revalidate();
    }
    
    /**
     * <b>setScalingLogarithmic</b><br/><br/>
     * <code>public void <b>setScalingLogarithmic</b>()</code><br/><br/>
     * Sets the scaling behavior to logarithmic.
     **/
    public void setScalingLogarithmic() {
        linear = false;
        revalidate();
    }
    
    /**
     * <b>revalidate</b><br/><br/>
     * <code>protected void <b>revalidate</b>()</code><br/><br/>
     * Makes any necessary changes whenever a critical value is changed.
     **/
    protected void revalidate() {
        // Ensure that the minimum is not zero in the case of log scaling.
        if (!linear && min == 0) {
            if (max < 0.01) { min = max / 100.0; }
            else { min = 0.01; }
        }
        
        // We only need to revalidate if we are using a logarithmic scale.
        if (!linear) {
            // Determine the scaling variable for logarithmic results.
            double temp = min;
            int steps = 0;
            while (temp < 1) {
                temp = temp * 10;
                steps++;
            }
            scale = Math.pow(10, steps);
            
            // Revalidate the logarithmic variables.
            lMax = Math.log10(scale * max);
            lMin = Math.log10(scale * min);
        }
    }
}
