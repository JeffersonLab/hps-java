package org.hps.online.recon;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Log messages to a file for a server program
 *
 * To change the log level (hard-coded to ALL), edit the log level at:<br/>
 * <code>logger.setLevel(Level.ALL);</a><br/>
 * and recompile the module.
 */
public class ServerLoggingConfig {

    private static final String LOG_FILE_PATH = System.getProperty("user.dir") + File.separator + "logs";

    static {
        System.setProperty(
                "java.util.logging.SimpleFormatter.format",
                "%1$tb %1$td, %1$tY %1$tl:%1$tM:%1$tS.%1$tL [%4$s] %5$s%6$s%n");
    }

    public ServerLoggingConfig(String fileName) throws IOException {
        setup(fileName);
    }

    public ServerLoggingConfig() throws IOException {
        setup("server.log");
    }

    private final void setup(String fileName) throws IOException {
        LogManager.getLogManager().reset();
        Logger rootLogger = Logger.getLogger("");
        File dir = new File(LOG_FILE_PATH);
        if (!dir.exists()) {
            dir.mkdir();
        }
        File logFile = new File(dir.getAbsolutePath() + File.separator + fileName);
        Handler handler = new FileHandler(logFile.getAbsolutePath());
        handler.setLevel(Level.ALL);
        handler.setFormatter(new SimpleFormatter());
        rootLogger.addHandler(handler);
        Logger logger = Logger.getLogger(ServerLoggingConfig.class.getPackage().getName());
        logger.setLevel(Level.ALL);
        rootLogger.setLevel(Level.WARNING);
        //System.out.println("Log file: " + logFile.getAbsolutePath());
    }
}
