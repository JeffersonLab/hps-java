package org.hps.monitoring.ecal.eventdisplay.util;

import java.awt.AWTEvent;
import java.awt.Point;

import org.hps.monitoring.ecal.eventdisplay.ui.Viewer;

/**
 * Class <code>CrystalEvent</code> represents some event that occurred
 * with respect to a crystal. It is thrown when a crystal either gains
 * or loses focus or is clicked. Crystal ID indices are always with
 * respect to the panel coordinate system.
 */
public class CrystalEvent extends AWTEvent {
    private static final long serialVersionUID = 77198267255387212L;
    // Stores the location of the triggering crystal.
    private final Point crystal;
    // The AWTEvent id for this event.
    private static final int AWT_ID = AWTEvent.RESERVED_ID_MAX + 10;
    
    /**
     * <b>CrystalEvent</b><br/><br/>
     * <code>public <b>CrystalEvent</b>(Viewer parent, Point triggerCrystal)</code><br/><br/>
     * Creates a crystal event for the indicated crystal and triggering
     * component.
     * @param source - The triggering component.
     * @param triggerCrystal - The crystal associated with the event.
     * @throws IllegalArgumentException Occurs if the associated crystal
     * is <code>null</code>.
     */
    public CrystalEvent(Viewer source, Point triggerCrystal) throws IllegalArgumentException {
        // Run the superclass constructor.
        super(source, AWT_ID);
        
        // Make sure that the trigger crystal is not null.
        if(triggerCrystal == null) {
            throw new IllegalArgumentException("Crystal events can not occur with respect to non-exstant crystals.");
        }
        
        // Define the event parameters.
        crystal = triggerCrystal;
    }
    
    /**
     * <b>getCrystalID</b><br/><br/>
     * <code>public Point <b>getCrystalID</b>()</code><br/><br/>
     * Indicates the panel indices at which the crystal is located.
     * @return Returns the crystal's panel indices as a <code>Point
     * </code> object.
     */
    public Point getCrystalID() { return crystal; }
}
