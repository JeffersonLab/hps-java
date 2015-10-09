package org.hps.logging.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

/**
 * Read the test logging configuration and load it into the global log manager.
 * <p>
 * This configuration will cause only warnings and errors to print.
 * 
 * @author Jeremy McCormick, SLAC
 */
public class TestLoggingConfig {

    /**
     * Class constructor which reads in a logging properties file from a classpath resource.
     */
    public TestLoggingConfig() {
        InputStream inputStream = TestLoggingConfig.class.getResourceAsStream("test_logging.properties");
        try {
            LogManager.getLogManager().readConfiguration(inputStream);
        } catch (SecurityException | IOException e) {
            throw new RuntimeException("Initialization of test logging configuration failed.", e);
        }
    }
}
