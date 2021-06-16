package org.hps.monitoring.ecal.eventdisplay.util;

import java.awt.Color;

/**
 * Interface <code>ColorMap</code> maps some value to a color.
 */
public interface ColorMap<T> {
    /**
     * <b>getColor</b><br/><br/>
     * <code>public Color <b>getColor</b>(T value)</code><br/><br/>
     * Determines the color representing the indicated value.
     * @param value - The value to relate to a color.
     * @return Returns a <code>Color</code> object associated with the argument value.
     **/
    public Color getColor(T value);
}
