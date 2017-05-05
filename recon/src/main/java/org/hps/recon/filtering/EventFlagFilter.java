package org.hps.recon.filtering;

import org.lcsim.event.EventHeader;

/**
 * Accept only events where all of the specified flags exist and have a value of 1.
 */
public class EventFlagFilter extends EventReconFilter {

    String[] flagNames = {"svt_bias_good", "svt_position_good", "svt_burstmode_noise_good", "svt_event_header_good", "svt_latency_good"};

    public void setFlagNames(String[] flagNames) {
        this.flagNames = flagNames;
    }

    @Override
    public void process(EventHeader event) {
        incrementEventProcessed();
        if (flagNames != null) {
            for (String flagName : flagNames) {
                int[] flag = event.getIntegerParameters().get(flagName);
                if (flag == null || flag[0] == 0) {
                    skipEvent();
                }
            }
        }
        incrementEventPassed();
    }
}
