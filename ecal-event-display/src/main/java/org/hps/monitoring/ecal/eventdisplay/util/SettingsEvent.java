package org.hps.monitoring.ecal.eventdisplay.util;

import java.awt.AWTEvent;

import org.hps.monitoring.ecal.eventdisplay.ui.CalorimeterPanel;

/**
 * Class <code>SettingsEvent</code> represents a change that has occurred
 * to some setting in a <code>CalorimeterPanel</code> object. It stores
 * both the triggering object and an <code>int</code> primitive which
 * corresponds to the particular setting that triggered the event.
 * 
 * @see CalorimeterPanel
 */
public class SettingsEvent extends AWTEvent {
    // Local variables.
    private static final long serialVersionUID = 1L;
    
    // Event IDs.
    /**
     * Indicates that the panel has changed its scaling to either
     * linear or logarithmic.
     */
    public static final int PROPERTY_SCALE_TYPE = AWTEvent.RESERVED_ID_MAX + 1;
    
    /**
     * Indicates that the panel's x-axis orientation has changed.
     */
    public static final int PROPERTY_X_ORIENTATION = AWTEvent.RESERVED_ID_MAX + 2;
    
    /**
     * Indicates that the panel's y-axis orientation has changed.
     */
    public static final int PROPERTY_Y_ORIENTATION = AWTEvent.RESERVED_ID_MAX + 3;
    
    /**
     * Indicates that the panel's behavior for highlighting crystals
     * that are under the cursor has changed.
     */
    public static final int PROPERTY_HOVER_HIGHLIGHT = AWTEvent.RESERVED_ID_MAX + 4;
    
    /**
     * Indicates that  the panel's behavior for coloring zero-energy
     * crystals has changed.
     */
    public static final int PROPERTY_ZERO_ENERGY_COLOR = AWTEvent.RESERVED_ID_MAX + 5;
    
    /**
     * Indicates that a change has occurred to the panel's scale minimum
     * or maximum value.
     */
    public static final int PROPERTY_SCALE_RANGE = AWTEvent.RESERVED_ID_MAX + 6;
    
    /**
     * Indicates that the panel's energy-to-color map has changed.
     */
    public static final int PROPERTY_SCALE_COLOR_MAP = AWTEvent.RESERVED_ID_MAX + 7;
    
    /**
     * Indicates that the panel's scale has changed visibility.
     */
    public static final int PROPERTY_SCALE_VISIBLE = AWTEvent.RESERVED_ID_MAX + 8;
    
    /**
     * Instantiates a new <code>SettingsEvent</code> object.
     * @param source - The calorimeter panel that triggered the event.
     * @param propertyID - The ID corresponding to the property that
     * has changed value.
     */
    public SettingsEvent(CalorimeterPanel source, int propertyID) {
        super(source, propertyID);
    }
}