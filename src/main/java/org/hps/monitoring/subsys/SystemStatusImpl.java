package org.hps.monitoring.subsys;

import java.util.ArrayList;
import java.util.List;

/**
 * The implementation of {@link SystemStatus}.
 */
public final class SystemStatusImpl implements SystemStatus {

    StatusCode code = StatusCode.UNKNOWN;
    long lastChangedMillis;
    String message;
    List<SystemStatusListener> listeners = new ArrayList<SystemStatusListener>();
    final Subsystem systemName;
    final String description;
    boolean active = true;
    final boolean clearable;
    
    /**
     * Fully qualified constructor.
     * @param systemName The enum specifiying the system being monitored.
     * @param description A description of this specific status monitor.
     * @param clearable True if this status can be cleared.
     */
    public SystemStatusImpl(Subsystem systemName, String description, boolean clearable) {
        this.systemName = systemName;
        this.description = description;
        this.clearable = clearable;
        setLastChangedTime();
        SystemStatusRegistry.getSystemStatusRegistery().register(this);
    }
    
    @Override
    public Subsystem getSubsystem() {
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
    public void setStatus(StatusCode code, String message) {
        if (isActive()) {
            this.code = code;
            this.message = message;
            setLastChangedTime();
            notifyListeners();
        }
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
    
    @Override 
    public boolean isClearable() {
        return clearable;
    }
}
