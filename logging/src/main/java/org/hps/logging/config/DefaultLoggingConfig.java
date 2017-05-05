package org.hps.logging.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

/**
 * Read the default logging configuration and load it into the global log manager.
 */
public class DefaultLoggingConfig {

    /**
     * Class constructor which reads in a logging properties file from a classpath resource.
     */
    public DefaultLoggingConfig() {
        InputStream inputStream = DefaultLoggingConfig.class.getResourceAsStream("logging.properties");
        try {
            LogManager.getLogManager().readConfiguration(inputStream);
        } catch (SecurityException | IOException e) {
            throw new RuntimeException("Initialization of default logging configuration failed.", e);
        }
    }
    
    /**
     * Initialize default logging if java system properties are not set.
     */
    public static void initialize() {
        if (System.getProperty("java.util.logging.config.class") == null &&
                System.getProperty("java.util.logging.config.file") == null) {
            // Config is only read in if there is not an externally set class or file already.
            new DefaultLoggingConfig();
        }
    }
}
