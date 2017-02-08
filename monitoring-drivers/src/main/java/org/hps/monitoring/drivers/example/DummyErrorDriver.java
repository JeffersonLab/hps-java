package org.hps.monitoring.drivers.example;

import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 * Driver for testing the error handling in the monitoring app.
 */
public class DummyErrorDriver extends Driver {
    
    boolean throwProcess = true;
    boolean throwEndOfData = false;
    boolean throwStartOfData = false;
    boolean throwDetectorChanged = false;
    
    public void setThrowProcess(boolean throwProcess) {
        this.throwProcess = throwProcess;
    }
    
    public void setThrowEndOfData(boolean throwEndOfData) {
        this.throwEndOfData = throwEndOfData;
    }
    
    public void setThrowStartOfData(boolean throwStartOfData) {
        this.throwStartOfData = throwStartOfData;
    }
    
    public void setThrowDetectorChanged(boolean throwDetectorChanged) {
        this.throwDetectorChanged = throwDetectorChanged;
    }
    
    public void startOfData() {
        if (throwStartOfData)
            throw new RuntimeException("This is a dummy error from the startOfData method.");
    }
    
    public void endOfData() {
        if (throwEndOfData)
            throw new RuntimeException("This is a dummy error from the endOfData method.");
    }
    
    public void process(EventHeader event) {
        if (throwProcess)
            throw new RuntimeException("This is a dummy error from the process method.");
    }
    
    public void detectorChanged(Detector detector) {
        if (throwDetectorChanged)
            throw new RuntimeException("This is a dummy error from the detectorChanged method.");
    }
}