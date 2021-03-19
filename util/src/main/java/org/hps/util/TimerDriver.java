package org.hps.util;

import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

/**
 * Lightweight timer driver. Add one copy of this driver to measure time per event; add multiple copies to measure time between two points in the execute list.
 */
public class TimerDriver extends Driver {

    private static long time = Long.MIN_VALUE;
    private long timeElapsed;
    private int count = 0;
    private String name = "unnamed";
    private boolean verbose = false;

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void process(EventHeader event) {
        long oldTime = time;
        time = System.nanoTime();
        if (oldTime != Long.MIN_VALUE) {
            long dt = time - oldTime;
            timeElapsed += dt;
            count++;
            if (verbose) {
                System.out.format("Timer %s: dt %f, timeElapsed %d, count %d, mean time %f ms\n", name, dt / 1.0e6, timeElapsed, count, (timeElapsed / 1.0e6) / count);
            } else {
                System.out.format("Timer %s: dt %f, mean time %f ms\n", name, dt / 1.0e6, (timeElapsed / 1.0e6) / count);
            }
        }
    }
}
