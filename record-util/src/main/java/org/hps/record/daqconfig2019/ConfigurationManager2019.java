package org.hps.record.daqconfig2019;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Class <code>ConfigurationManager2019</code> provides static access to
 * the 2019 DAQ configuration that can be parsed from EVIO files. It works
 * in conjunction with the <code>DAQConfigDriver2019</code>, which obtains
 * the configuration parser object when available and passes it to this
 * manager, and <code>EvioDAQParser2019</code>, which parses the EVIO data.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @author Tongtong Cao <caot@jlab.org>
 * @see DAQConfigDriver2019
 * @see EvioDAQParser2019
 */
public class ConfigurationManager2019 {
    // Store the configuration object.
    private static final DAQConfig2019 DAQ_CONFIG = new DAQConfig2019();
    
    // Track whether a DAQ configuration has been read yet.
    private static boolean INITIALIZED = false;
    
    // Store listeners to alert other classes when an update occurs.
    private static final List<ActionListener> AL_LIST = new ArrayList<ActionListener>();
    private static final ActionEvent EVENT = new ActionEvent(DAQ_CONFIG, ActionEvent.RESERVED_ID_MAX + 12, "update");
    
    /**
     * Gets an instance of the DAQ configuration settings object if it
     * exists. If no configuration has been read, this will return
     * <code>null</code> instead.
     * @return Returns the DAQ settings as a <code>DAQConfig</code>
     * object or <code>null</code>.
     */
    public synchronized static final DAQConfig2019 getInstance() {
        if(INITIALIZED) { return DAQ_CONFIG; }
        else { return null; }
    }
    
    /**
     * Adds a listener to track when updates occur in the DAQ settings.
     * @param listener - The listener.
     */
    public synchronized static final void addActionListener(ActionListener listener) {
        if(listener != null) { AL_LIST.add(listener); }
    }
    
    /**
     * Gets the list of all listeners attached to the manager.
     * @return Returns a <code>List</code> collection containing the
     * <code>ActionListener</code> objects attached to the manager.
     */
    public static final List<ActionListener> getActionListeners() {
        return AL_LIST;
    }
    
    /**
     * Indicates whether a DAQ configuration has been received yet.
     * If <code>false</code>, then calls to <code>getInstance</code>
     * will return <code>null</code>.
     * @return Returns <code>true</code> if a DAQ configuration has
     * been read and <code>false</code> otherwise.
     */
    public static final boolean isInitialized() {
        return INITIALIZED;
    }
    
    /**
     * Removes an listener so that it will no longer receive updates
     * when the DAQ configuration changes.
     * @param listener - The listener to remove.
     */
    public synchronized static final void removeActionListener(ActionListener listener) {
        if(listener != null) { AL_LIST.remove(listener); }
    }
    
    /**
     * Updates the DAQ configuration with the given EVIO parser. The
     * manager will also note that it is initialized and inform any
     * associated listeners that an update has occurred.
     * @param parser - The updated DAQ information.
     */
    public synchronized static final void updateConfiguration(EvioDAQParser2019 parser) {
        INITIALIZED = false;
        DAQ_CONFIG.loadConfig(parser);
        INITIALIZED = true;
        updateListeners();
    }
    
    /**
     * Sends an update event to all associated listeners.
     */
    private static final void updateListeners() {
        for(ActionListener al : AL_LIST) {
            al.actionPerformed(EVENT);
        }
    }
}