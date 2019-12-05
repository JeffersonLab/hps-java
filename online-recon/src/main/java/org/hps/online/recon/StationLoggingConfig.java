package org.hps.online.recon;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

/**
 * Load log properties resource for online reconstruction station.
 * 
 * @author jeremym
 */
public class StationLoggingConfig {
    
    private static final String LOG_PROPERTIES_FILE = "station_logging.properties";
    
    /**
     * Class constructor which reads in a logging properties file from a classpath resource.
     */
    public StationLoggingConfig() {
        InputStream inputStream = StationLoggingConfig.class.getResourceAsStream(LOG_PROPERTIES_FILE);
        if (inputStream == null) {
            throw new RuntimeException("Failed to read resource: " + LOG_PROPERTIES_FILE);
        }
        try {
            LogManager.getLogManager().readConfiguration(inputStream);
        } catch (SecurityException | IOException e) {
            throw new RuntimeException("Initialization of station logging configuration failed", e);
        }
    }
}
