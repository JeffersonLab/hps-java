package org.hps.monitoring.subsys;

import java.util.ArrayList;
import java.util.List;

/**
 * The implementation of {@link SystemStatus}.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class SystemStatusImpl implements SystemStatus {

    StatusCode code = SystemStatus.StatusCode.UNKNOWN;
    long lastChangedMillis;
    String message;
    List<SystemStatusListener> listeners = new ArrayList<SystemStatusListener>();
    final SystemName systemName;
    final String description;
    boolean active = true;
    
    public SystemStatusImpl(SystemName systemName, String description) {
        this.systemName = systemName;
        this.description = description;
        setLastChangedTime();
        SystemStatusRegistry.getSystemStatusRegistery().register(this);
    }
    
    @Override
    public SystemName getSystemName() {
        return systemName;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public StatusCode getStatusCode() {
        return code;
    }

    @Override
    public void setStatusCode(StatusCode code, String message) {
        this.code = code;
        this.message = message;
        setLastChangedTime();
        if (isActive())
            notifyListeners();
    }

    @Override
    public void addListener(SystemStatusListener listener) {
        this.listeners.add(listener);
    }
    
    @Override
    public long getLastChangedMillis() {
        return lastChangedMillis;
    }

    /**
     * Notify listeners of changes to the system status.
     */
    void notifyListeners() {
        for (SystemStatusListener listener : listeners) {
            listener.statusChanged(this);
        }
    }
    
    private void setLastChangedTime() {
        this.lastChangedMillis = System.currentTimeMillis();
    }
 
    @Override
    public void setActive(boolean masked) {
        this.active = masked;
    }
    
    @Override
    public boolean isActive() {
        return active;
    }
}
