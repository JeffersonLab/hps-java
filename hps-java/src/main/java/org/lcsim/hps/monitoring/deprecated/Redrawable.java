package org.lcsim.hps.monitoring.deprecated;

/**
 * Drivers that will be attached to monitoring system should implement 
 * this if they will only redraw plots every N events, or when redraw() is called.
 * Setting eventRefreshRate = 0 should disable automatic redrawing.
 * @deprecated Individual Drivers should implement this behavior.  No interface should be necessary.
 */
@Deprecated
public interface Redrawable {
    public void redraw();
    public void setEventRefreshRate(int eventRefreshRate);
}
