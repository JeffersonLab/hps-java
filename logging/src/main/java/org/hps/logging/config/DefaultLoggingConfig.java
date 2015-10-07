package org.hps.logging.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

/**
 * Read the default logging configuration and load it into the global log manager.
 * 
 * @author Jeremy McCormick, SLAC
 */
public class DefaultLoggingConfig {

    /**
     * Class constructor which reads in a logging properties file from a classpath resource.
     * <p>
     * The default configuration will only be activated if there is no file or class specified 
     * from a system property.
     */
    public DefaultLoggingConfig() {
        // Activate the default config if the logging system properties are not set. 
        if (System.getProperty("java.util.logging.config.class") == null 
                && System.getProperty("java.util.logging.config.file") == null) {
            InputStream inputStream = DefaultLoggingConfig.class.getResourceAsStream("logging.properties");
            try {
                LogManager.getLogManager().readConfiguration(inputStream);
            } catch (SecurityException | IOException e) {
                throw new RuntimeException("Initialization of default logging configuration failed.", e);
            }
        } 
    }

}
