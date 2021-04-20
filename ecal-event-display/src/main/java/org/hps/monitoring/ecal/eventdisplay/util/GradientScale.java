package org.hps.monitoring.ecal.eventdisplay.util;
import java.awt.Color;

/**
 * The class <code>GradientScale</code> is an implementation of the abstract
 * class <code>ColorScale</code> which represents a simple gradient from one
 * color to another. It will map any argument value that exceeds its maximum
 * value to the hot color and any argument value that is below its minimum value
 * to its cold color. All other argument values will be mapped somewhere between
 * the cold and hot colors using either a linear or logarithmic scale.
 **/
public final class GradientScale extends ColorScale {
    // The color associated with the maximum value.
    private Color hotColor = Color.WHITE;
    // The color associated with the minimum value.
    private Color coldColor = Color.BLACK;
    // Efficiency variable holding the rgb difference between the two
    // colors. This is used to prevent recalculation when mapping values.
    private int[] drgb = { 255, 255, 255 };
    
    public Color getColor(Double value) {
        // If the argument is null, treat it as zero.
        if(value == null) { value = 0.0; }
        
        // If the value is less than the minimum, return the cold color.
        if (value < min) { return coldColor; }
        
        // If the value is greater than the maximum, return the hot color.
        if (value > max) { return hotColor; }
        
        // Otherwise, calculate how far along the gradient the value is.
        double percent;
        if (linear) { percent = (value - min) / (max - min); }
        else {
            double lValue = Math.log10(scale * value);
            percent = (lValue - lMin) / (lMax - lMin);
        }
        
        // Scale the color.
        int dr = (int) Math.round(percent * drgb[0]);
        int dg = (int) Math.round(percent * drgb[1]);
        int db = (int) Math.round(percent * drgb[2]);
        
        // Return the result.
        return new Color(coldColor.getRed() + dr, coldColor.getGreen() + dg, coldColor.getBlue() + db);
    }
    
    /**
     * <b>setColdColor</b><br/><br/>
     * <code>public void <b>setColdColor</b>(Color c)</code><br/><br/>
     * Sets the color associated with the minimum value.
     * @param c - The color to use.
     **/
    public void setColdColor(Color c) {
        coldColor = c;
        revalidateColor();
    }
    
    /**
     * <b>setHotColor</b><br/><br/>
     * <code>public void <b>setHotColor</b>(Color c)</code><br/><br/>
     * Sets the color associated with the maximum value.
     * @param c - The new color to use.
     **/
    public void setHotColor(Color c) {
        hotColor = c;
        revalidateColor();
    }
    
    /**
     * <b>makeGreyScale</b><br/><br/>
     * <code>public static GradientScale <b>makeGreyScale</b>(double minimum, double maximum)</code><br/><br/>
     * Creates a color scale that ranges from black (cold) to white (hot) with
     * the indicated maximum and minimum.
     * @param minimum - The lowest value for color scaling.
     * @param maximum - The highest value for color scaling.
     * @return Returns a <code>GradientScale</code> that maps to grey scale over
     * the indicated range.
     **/
    public static GradientScale makeGreyScale(double minimum, double maximum) {
        GradientScale gs = new GradientScale();
        gs.setMinimum(minimum);
        gs.setMaximum(maximum);
        
        return gs;
    }
    
    /**
     * <b>makeHeatScale</b><br/><br>
     * <code>public static GradientScale <b>makeHeatScale</b>(double minimum, double maximum)</code><br/><br/>
     * Creates a color scale that ranges from black (cold) to red (hot) with the
     * indicated maximum and minimum.
     * @param minimum - The lowest value for color scaling.
     * @param maximum - The highest value for color scaling.
     * @return Returns a <code>GradientScale</code> that maps to a black- to-red
     * gradient over the indicated range.
     **/
    public static GradientScale makeHeatScale(double minimum, double maximum) {
        GradientScale hs = new GradientScale();
        hs.setHotColor(Color.RED);
        hs.setColdColor(Color.BLACK);
        hs.setMinimum(minimum);
        hs.setMaximum(maximum);
        
        return hs;
    }
    
    /**
     * <b>revalidateColor</b><br/><br/>
     * <code>private void <b>revalidateColor</b>()</code><br/><br/>
     * Calculates the differences between the hot and cold colors and sets the
     * class related class variables.
     **/
    private void revalidateColor() {
        drgb[0] = hotColor.getRed() - coldColor.getRed();
        drgb[1] = hotColor.getGreen() - coldColor.getGreen();
        drgb[2] = hotColor.getBlue() - coldColor.getBlue();
    }
}
