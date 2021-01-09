package org.hps.online.recon.logging;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

/**
 * Logging config for online reconstruction stations and other misc components
 */
public class LoggingConfig {

    public LoggingConfig(String propName) {
        setup(propName);
    }

    public LoggingConfig() {
        setup("logging.properties");
    }

    private void setup(String propName) {
        InputStream inputStream = LoggingConfig.class.getResourceAsStream(propName);
        if (inputStream == null) {
            throw new RuntimeException("Failed to read resource: " + propName);
        }
        try {
            LogManager.getLogManager().readConfiguration(inputStream);
        } catch (SecurityException | IOException e) {
            throw new RuntimeException("Initialization of logging configuration failed", e);
        }
    }
}
