package org.hps.recon.filtering;

import java.util.Arrays;
import org.lcsim.event.EventHeader;

/**
 * Accept only events where all of the specified flags exist and have a value of
 * 1.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: $
 */
public class EventFlagFilter extends EventReconFilter {

    String[] flagNames = {"svt_bias_good", "svt_position_good", "svt_burstmode_noise_good", "svt_event_header_good", "svt_latency_good"};

    private boolean _debug;

    public void setDebug(boolean b)
    {
        _debug =b;
    }
    public void setFlagNames(String[] flagNames) {
        this.flagNames = flagNames;
    }

    @Override
    public void process(EventHeader event) {
        incrementEventProcessed();
        if (flagNames != null) {
            for (String flagName : flagNames) {
                int[] flag = event.getIntegerParameters().get(flagName);
                if(_debug)
                {
                    System.out.println("Run "+event.getRunNumber()+" Event: "+event.getEventNumber()+" "+flagName+" "+Arrays.toString(flag));
                }
                if (flag == null || flag[0] == 0) {
                    skipEvent();
                }
            }
        }
        incrementEventPassed();
    }
}
