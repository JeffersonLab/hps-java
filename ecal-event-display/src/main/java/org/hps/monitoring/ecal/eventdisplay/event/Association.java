package org.hps.monitoring.ecal.eventdisplay.event;

import java.awt.Color;
import java.awt.Point;

/**
 * Class <code>Association</code> tells the <code>CalorimeterPanel</code> to 
 * highlight the child crystal in the given color whenever the parent crystal is selected.
 */
public final class Association {
    private final Point parent;
    private final Point child;
    private final Color highlight;
    
    /**
     * <b>Association</b><br/><br/>
     * <code>public <b>Association</b>(Point parentCrystal, Point childCrystal, Color highlightColor)</code><br/><br/>
     * Creates an association between a child crystal and a parent
     * crystal.
     * @param parentCrystal - The crystal with which the child crystal
     * is connected.
     * @param childCrystal - The connected crystal.
     * @param highlightColor - The color in which the child crystal
     * should be highlighted.
     */
    public Association(Point parentCrystal, Point childCrystal, Color highlightColor) {
        parent = parentCrystal;
        child = childCrystal;
        highlight = highlightColor;
    }
    
    /**
     * <b>getChildCrystal</b><br/><br/>
     * <code>public Point <b>getChildCrystal</b>()</code><br/><br/>
     * Indicates the indices for the child crystal.
     * @return Returns the child crystal's indices in a <code>Point
     * </code> object.
     */
    public Point getChildCrystal() { return child; }
    
    /**
     * <b>getHighlight</b><br/><br/>
     * <code>public Color <b>getHighlight</b>()</code><br/><br/>
     * Gets the color with which the child crystal should be highlighted
     * whenever the parent crystal is selected.
     * @return Returns the highlight color as a <code>Color</code> object.
     */
    public Color getHighlight() { return highlight; }
    
    /**
     * <b>getParentCrystal</b><br/><br/>
     * <code>public Point <b>getParentCrystal</b>()</code><br/><br/>
     * Indicates the indices for the parent crystal with which the
     * child crystal is connected.
     * @return Returns the parent crystal's indices in a <code>Point
     * </code> object.
     */
    public Point getParentCrystal() { return parent; }
}
