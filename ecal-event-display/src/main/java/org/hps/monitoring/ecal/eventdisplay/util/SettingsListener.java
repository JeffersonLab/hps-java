package org.hps.monitoring.ecal.eventdisplay.util;

import java.util.EventListener;

/**
 * Interface <code>SettingsListener</code> tracks changes that occur
 * in the settings of a <code>CalorimeterPanel</code> object.
 */
public interface SettingsListener extends EventListener {
    /**
     * Indicates that a setting has changed.
     * @param e - An event representing the change.
     */
    public void settingChanged(SettingsEvent e);
}