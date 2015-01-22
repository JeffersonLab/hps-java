package org.hps.record.evio;

import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

/**
 * <p>
 * This is an {@link EvioEventProcessor} for initializing the conditions system
 * from EVIO events.
 * <p>
 * The {@link #startRun(EvioEvent)} method will setup conditions from the pre start
 * events.
 * <p>
 * The {@link #process(EvioEvent)} method will setup conditions from a head bank
 * if it is present in the event.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EvioDetectorConditionsProcessor extends EvioEventProcessor {

    private String detectorName;
    
    public EvioDetectorConditionsProcessor(String detectorName) {
        if (detectorName == null) {
            throw new IllegalArgumentException("The detectorName argument is null.");
        }
        this.detectorName = detectorName;
    }
    
    @Override
    public void process(EvioEvent evioEvent) throws Exception {
        // Get the head head bank from event.
        BaseStructure headBank = EvioEventUtilities.getHeadBank(evioEvent);
        
        // Is the head bank present?
        if (headBank != null) { 
                                
            // Get the run number from the head bank.
            int runNumber = headBank.getIntData()[1];                    

            // Initialize the conditions system from the detector name and run number.
            try {
                ConditionsManager.defaultInstance().setDetector(detectorName, runNumber);
            } catch (ConditionsNotFoundException e) {
                throw new RuntimeException("Error setting up conditions from EVIO head bank.", e);
            }                   
        } 
    }

    @Override
    public void startRun(EvioEvent evioEvent) {
        System.out.println("EvioDetectorConditionsProcessor.startRun");
        if (EvioEventUtilities.isPreStartEvent(evioEvent)) {
            // Get the pre start event's data bank.
            int[] data = EvioEventUtilities.getControlEventData(evioEvent);
            
            // Get the run number from the bank.
            int runNumber = data[1];            
            
            // Initialize the conditions system from the detector name and run number.
            try {
                System.out.println("  setting up conditions from pre start: " + detectorName + " #" + runNumber);
                ConditionsManager.defaultInstance().setDetector(detectorName, runNumber);
            } catch (ConditionsNotFoundException e) {
                throw new RuntimeException("Error setting up conditions from EVIO pre start event.", e);
            }
        }
    }
}