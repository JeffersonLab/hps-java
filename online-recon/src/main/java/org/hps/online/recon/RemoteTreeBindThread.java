package org.hps.online.recon;

import java.util.logging.Level;
import java.util.logging.Logger;

public class RemoteTreeBindThread extends Thread {

    String remoteTreeBind = null;
    Integer maxAttempts = 5;
    InlineAggregator agg = null;

    static Logger LOG = Logger.getLogger(StationProcess.class.getPackage().getName());

    RemoteTreeBindThread(InlineAggregator agg, String remoteTreeBind, Integer maxAttempts) {
        this.agg = agg;
        if (remoteTreeBind == null) {
            throw new IllegalArgumentException("remoteTreeBind was null");
        }
        this.remoteTreeBind=remoteTreeBind;
        if (maxAttempts != null) {
            this.maxAttempts = maxAttempts;
        }
    }

    // If this exits without a connection being made should it interrupt the station process???

    public void run() {
        for (long i = 0; i < this.maxAttempts; i++) {
            long attempt = i + 1;
            try {
                try {
                    Thread.sleep(attempt*5000L);
                } catch (InterruptedException e) {
                    LOG.log(Level.WARNING, "Interrupted", e);
                    break;
                }
                LOG.info("remoteTreeBind connection attempt: " + attempt);
                LOG.info("Adding remote tree bind: " + remoteTreeBind);
                agg.addRemote(remoteTreeBind);
                LOG.info("Done adding remote tree bind: " + remoteTreeBind);
                break;
            } catch (Exception e) {
                LOG.warning("Could not connect to: " + remoteTreeBind);
                if (attempt == this.maxAttempts) {
                    LOG.warning("remoteTreeBind connection attempt timed out without connecting");
                }
            }
        }
    }
}