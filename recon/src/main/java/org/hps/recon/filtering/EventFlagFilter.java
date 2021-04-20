package org.hps.recon.filtering;

import java.util.logging.Logger;

import org.lcsim.event.EventHeader;

/**
 * Accept only events where all of the specified flags exist and have a value of 1.
 */
public class EventFlagFilter extends EventReconFilter {

    private static Logger LOGGER = Logger.getLogger(EventFlagFilter.class.getPackage().getName());
    
    String[] flagNames = {"svt_bias_good", "svt_position_good", "svt_burstmode_noise_good", "svt_event_header_good", "svt_latency_good", "svt_readout_overlap_good"};

    public void setFlagNames(String[] flagNames) {
        this.flagNames = flagNames;
    }

    @Override
    public void process(EventHeader event) {
        incrementEventProcessed();
        if (flagNames != null) {
            for (String flagName : flagNames) {
                int[] flag = event.getIntegerParameters().get(flagName);
                //System.out.println("flag <" + flagName + ">");
                if (flag == null || flag[0] == 0) {
                    LOGGER.fine("Skipping event <" + event.getEventNumber() + "> from flag <" + flagName + ">");
                    //System.out.println("Skipping event <" + event.getEventNumber() + "> from flag <" + flagName + ">");
                    skipEvent();
                }
            }
        }
        LOGGER.fine("Event passed");
        incrementEventPassed();
    }
}
