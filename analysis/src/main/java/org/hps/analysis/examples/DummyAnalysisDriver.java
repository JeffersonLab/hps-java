package org.hps.analysis.examples;

import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 * This is a skeleton that can be used to create a user analysis Driver in LCSim.
 */
public class DummyAnalysisDriver extends Driver {    
    
    /**
     * This argument does nothing.
     */
    int anArgument = 42;
    
    /**
     * Your Driver should have a public constructor.
     */
    public DummyAnalysisDriver() {
        getLogger().info("Hello DummyAnalysisDriver!");
    }
    
    /**
     * This is an example set method which can be accessed through LCSim steering file arguments.
     * @param anArgument
     */
    public void setAnArgument(int anArgument) {
        this.anArgument = anArgument;
        getLogger().info("anArgument was set to " + anArgument);
    }
    
    /**
     * Process a single event.  
     * Your analysis code should go in here.
     * @param event The LCSim event to process.
     */
    public void process(EventHeader event) {
        getLogger().info("This is event #" + event.getEventNumber() + " in run # " + event.getRunNumber() + ".");
    }
    
    /**
     * Initialization code should go here that doesn't need the conditions system or Detector.
     */
    public void startOfData() {
        getLogger().info("start of data");
    }
    
    /**
     * Driver setup should go here that needs information from the conditions system or Detector.
     * @param detector The LCSim Detector object.
     */
    public void detectorChanged(Detector detector) {
        getLogger().info("detector changed");
    }
    
    /**
     * End of job calculations or cleanup should go here.
     */
    public void endOfData() {
        getLogger().info("end of data...goodbye!");
    }    
}