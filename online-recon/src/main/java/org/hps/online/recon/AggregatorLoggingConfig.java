package org.hps.online.recon;

import java.io.IOException;

public class AggregatorLoggingConfig extends ServerLoggingConfig {

    public AggregatorLoggingConfig() throws IOException {
        super("aggregator.log");
    }
}
