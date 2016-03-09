package org.hps.monitoring.ecal.eventdisplay.util;

import java.util.EventListener;

/**
 * Interface <code>CrystalListener</code> receives events from a <code>
 * Viewer</code> component regarding crystals. These include whenever
 * a crystal is activated (i.e. it becomes highlighted), deactivated
 * (i.e. it is no longer highlighted), and clicked. 
 * 
 * @author Kyle McCarty
 */
public interface CrystalListener extends EventListener {
    /**
     * <b>crystalActivated</b><br/><br/>
     * <code>public void <b>crystalActivated</b>(CrystalEvent e)</code><br/><br/>
     * Invoked when a crystal becomes highlighted.
     * @param e - An object describing the event.
     */
    public void crystalActivated(CrystalEvent e);
    
    /**
     * <b>crystalDeactivated</b><br/><br/>
     * <code>public void <b>crystalDeactivated</b>(CrystalEvent e)</code><br/><br/>
     * Invoked when a crystal ceases to be highlighted.
     * @param e - An object describing the event.
     */
    public void crystalDeactivated(CrystalEvent e);
    
    /**
     * <b>crystalClicked</b><br/><br/>
     * <code>public void <b>crystalClicked</b>(CrystalEvent e)</code><br/><br/>
     * Invoked when a crystal is clicked
     * @param e - An object describing the event.
     */
    public void crystalClicked(CrystalEvent e);
}
