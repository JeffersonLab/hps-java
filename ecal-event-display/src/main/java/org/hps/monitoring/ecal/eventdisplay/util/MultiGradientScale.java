package org.hps.monitoring.ecal.eventdisplay.util;
import java.awt.Color;
import java.util.ArrayList;

/**
 * The class <code>MultiGradientScale</code> is an implementation of
 * <code>ColorScale</code> that maps values to a color over several different
 * individual <code>GradientScale</code> objects to allow for multi-color
 * mapping.
 * 
 * @author Kyle McCarty
 **/
public final class MultiGradientScale extends ColorScale {
    // Stores the colors in the map.
    private ArrayList<Color> colorList = new ArrayList<Color>();
    // Stores the component mapping scales.
    private ArrayList<GradientScale> scaleList = new ArrayList<GradientScale>();
    
    /**
     * <b>addColor</b><br/><br/>
     * <code>public void <b>addColor</b>(Color c)</code><br/><br/>
     * Adds a new color to the mapping scale. The first color will be the
     * coldest with subsequent colors being more and more hot.
     * @param c - The color to add.
     **/
    public void addColor(Color c) {
        colorList.add(c);
        revalidate();
    }
    
    public Color getColor(Double value) {
    	// If the value is null, treat it as zero.
    	if(value == null) { value = 0.0; }
    	
        // Get the number of colors and scales.
        int colors = colorList.size();
        int scales = scaleList.size();
        
        // If there are no colors or scales, give black.
        if (colors == 0 && scales == 0) { return Color.BLACK; }
        
        // If there are no scales, but there is a color, give that.
        if (scales == 0 && colors == 1) { return colorList.get(0); }
        
        // Scale the value if logarithmic.
        double sValue;
        if (linear) { sValue = value; }
        else { sValue = Math.log10(scale * value); }
        
        if(value < 1 && (Double.isNaN(sValue) || Double.isInfinite(sValue))) {
        	return scaleList.get(0).getColor(0.0);
        }
        
        // Otherwise, determine which scale should get the value.
        for (GradientScale s : scaleList) {
            if (sValue < s.getMaximum()) {
                return s.getColor(sValue);
            }
        }
        
        // If it didn'tappear in the list, it is the hottest color.
        return colorList.get(colors - 1);
    }
    
    /**
     * <b>removeColor</b><br/><br/>
     * <code>public boolean <b>removeColor</b>(int colorIndex)</code><br/><br/>
     * Removes the nth color from the mapping scale.
     * @param colorIndex - The index of the color to be removed.
     * @return Returns <code>true</code> if the color was removed and
     * <code>false</code> if it was not.
     **/
    public boolean removeColor(int colorIndex) {
        // Only remove the value if the index is valid.
        if (colorIndex >= 0 && colorIndex < colorList.size()) {
            colorList.remove(colorIndex);
            revalidate();
            return true;
        }
        else { return false; }
    }
    
    /**
     * <b>makeRainboowScale</b><br/><br/>
     * <code>public static <b>makeRainbowScale</b>(double minimum, double maximum)</code><br/><br>
     * Creates a <code>MultiGradientScale</code> that maps values from purple,
     * to blue, to cyan, to green, to yellow, and to red at the hottest.
     * @param minimum - The lowest mapped value.
     * @param maximum - The highest mapped value.
     * @return Returns the rainbow color mapping scale.
     **/
    public static MultiGradientScale makeRainbowScale(double minimum, double maximum) {
        int str = 165;
        Color purple = new Color(55, 0, 55);
        Color blue = new Color(0, 0, str);
        Color cyan = new Color(0, str, str);
        Color green = new Color(0, str, 0);
        Color yellow = new Color(str, str, 0);
        Color red = new Color(str, 0, 0);
        
        MultiGradientScale mgs = new MultiGradientScale();
        mgs.addColor(purple);
        mgs.addColor(blue);
        mgs.addColor(cyan);
        mgs.addColor(green);
        mgs.addColor(yellow);
        mgs.addColor(red);
        mgs.setMinimum(minimum);
        mgs.setMaximum(maximum);
        
        return mgs;
    }
    
    protected void revalidate() {
        // Handle the default logarithmic revalidation.
        super.revalidate();
        
        // Redistribute the lists.
        scaleList.clear();
        
        // We need at least colors to make a scale - otherwise, the
        // special cases handle the color.
        int colors = colorList.size();
        if (colors < 2) { return; }
        
        // Otherwise, define the list variables.
        double sStep;
        double sMin;
        if (linear) {
            sStep = (max - min) / (colors - 1);
            sMin = min;
        }
        else {
            sStep = (lMax - lMin) / (colors - 1);
            sMin = lMin;
        }
        double sMax = sMin + sStep;
        
        // Generate a list of scales.
        for (int i = 0; i < (colors - 1); i++) {
            // Make and add a scale.
            GradientScale s = new GradientScale();
            s.setMinimum(sMin);
            s.setMaximum(sMax);
            s.setColdColor(colorList.get(i));
            s.setHotColor(colorList.get(i + 1));
            scaleList.add(s);
            
            // Update the min/max.
            sMin = sMax;
            sMax += sStep;
        }
    }
}
