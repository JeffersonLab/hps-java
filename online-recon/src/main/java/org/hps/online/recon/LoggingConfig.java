package org.hps.online.recon;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

/**
 * Load log properties resource for online reconstruction
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
