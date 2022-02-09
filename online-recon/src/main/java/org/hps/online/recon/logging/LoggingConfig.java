package org.hps.online.recon.logging;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

import org.apache.log4j.BasicConfigurator;

/**
 * Logging configuration for online reconstruction stations and other miscellaneous components
 */
public class LoggingConfig {

    public LoggingConfig(String propName) {
        setup(propName);
    }

    public LoggingConfig() {
        setup("logging.properties");
    }

    private void setup(String propName) {

        // Fix stupid log4j warnings and turn all messages off
        BasicConfigurator.configure();
        org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);

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
