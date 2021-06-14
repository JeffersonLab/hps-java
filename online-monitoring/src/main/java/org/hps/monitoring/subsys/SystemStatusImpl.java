package org.hps.monitoring.subsys;

import java.util.ArrayList;
import java.util.List;

/**
 * The implementation of {@link SystemStatus}.
 */
public final class SystemStatusImpl implements SystemStatus {

    
    StatusCode code = StatusCode.UNKNOWN;
    Subsystem systemName;
    String message;
    String description;
    long lastChangedMillis;
    boolean active = true;
    boolean clearable;
    
    List<SystemStatusListener> listeners = new ArrayList<SystemStatusListener>();

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
    
    /**
     * Copy constructor from implementation class.
     * The list of listeners is NOT copied.  
     * @param status The status to copy.
     */
    public SystemStatusImpl(SystemStatusImpl status) {
        this.code = status.code;
        this.systemName = status.systemName;
        this.message = status.message;
        this.description = status.description;
        this.lastChangedMillis = status.lastChangedMillis;
        this.active = status.active;
        this.clearable = status.clearable;
    }
    
    /**
     * Copy constructor from interface.
     * The list of listeners is NOT copied.  
     * @param status The status to copy.
     */
    public SystemStatusImpl(SystemStatus status) {
        this.code = status.getStatusCode();
        this.systemName = status.getSubsystem();
        this.message = status.getMessage();
        this.description = status.getDescription();
        this.lastChangedMillis = status.getLastChangedMillis();
        this.active = status.isActive();
        this.clearable = status.isClearable();
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
    synchronized public void setStatus(StatusCode code, String message) {
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
    synchronized void notifyListeners() {
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
