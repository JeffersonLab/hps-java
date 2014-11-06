package org.hps.monitoring.ecal.eventdisplay.ui;

import org.hps.monitoring.ecal.eventdisplay.event.Cluster;
import org.hps.monitoring.ecal.eventdisplay.event.EcalHit;

/**
 * Abstract class <code>PassiveViewer</code> represents a <code>Viewer
 * </code> implementation which updates based on information passed to
 * it by an external source.
 * 
 * @author Kyle McCarty
 */
public abstract class PassiveViewer extends Viewer {
    private static final long serialVersionUID = -7479125553259270894L;
    
    /**
     * Adds a new hit to the display.
     * @param hit - The hit to be added.
     */
    public abstract void addHit(EcalHit hit);
    
    /**
     * Adds a new cluster to the display.
     * @param cluster - The cluster to be added.
     */
    public abstract void addCluster(Cluster cluster);
    
    /**
     * Clears any hits or clusters that have been added to the viewer.
     * Note that this does not automatically update the displayed panel.
     * <code>updateDisplay</code> must be called separately.
     */
    public abstract void resetDisplay();
    
    /**
     * Sets the upper and lower bounds of for the calorimeter display's
     * color mapping scale.
     * @param min - The lower bound.
     * @param max - The upper bound.
     */
    public void setScale(double min, double max) { //A.C. I modified these to double since ecalPanel methods use double
        ecalPanel.setScaleMinimum(min);
        ecalPanel.setScaleMaximum(max);
    }
    
    /**
     * Sets the upper bound for the calorimeter display's color mapping
     * scale.
     * @param max - The upper bound.
     */
    public void setScaleMaximum(double max) { ecalPanel.setScaleMaximum(max); } //A.C. I modified these to double since ecalPanel methods use double
    
    /**
     * Sets the lower bound for the calorimeter display's color mapping
     * scale.
     * @param min - The lower bound.
     */
    public void setScaleMinimum(double min) { ecalPanel.setScaleMinimum(min); } //A.C. I modified these to double since ecalPanel methods use double
    
    /**
     * Displays the hits and clusters added by the <code>addHit</code>
     * and <code>addCluster</code> methods.
     */
    public abstract void updateDisplay();
}