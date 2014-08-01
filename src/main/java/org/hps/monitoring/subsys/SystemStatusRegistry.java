package org.hps.monitoring.subsys;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Global registry of all {@link SystemStatus} objects.
 */
public final class SystemStatusRegistry {
    
    static SystemStatusRegistry instance = new SystemStatusRegistry();
    List<SystemStatus> systemStatuses = new ArrayList<SystemStatus>();
    
    private SystemStatusRegistry() {
    }
    
    public static SystemStatusRegistry getSystemStatusRegistery() {
        return instance;
    }
    
    void register(SystemStatus systemStatus) {
        if (!systemStatuses.contains(systemStatus))
            systemStatuses.add(systemStatus);
        else
            throw new IllegalArgumentException("The system status is already registered.");
    }        
    
    public List<SystemStatus> getSystemStatuses() {
        return Collections.unmodifiableList(systemStatuses);
    }
    
    public void clear() {
        systemStatuses.clear();
    }
}
