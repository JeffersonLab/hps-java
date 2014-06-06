package org.hps.monitoring.subsys;

import java.util.ArrayList;
import java.util.List;

/**
 * The implementation of {@link SystemStatus}.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SystemStatusImpl implements SystemStatus {

    StatusCode code = SystemStatus.StatusCode.UNKNOWN;
    long lastChangedMillis;
    List<SystemStatusListener> listeners = new ArrayList<SystemStatusListener>();
    
    SystemStatusImpl() {
        setCurrentTime();
    }

    @Override
    public StatusCode getStatusCode() {
        return code;
    }

    @Override
    public void setStatusCode(StatusCode code) {
        this.code = code;
        setCurrentTime();
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
    
    void notifyListeners() {
        for (SystemStatusListener listener : listeners) {
            listener.statusChanged(this);
        }
    }
    
    private void setCurrentTime() {
        this.lastChangedMillis = System.currentTimeMillis();
    }
}
