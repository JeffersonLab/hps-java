package org.hps.online.recon.logging;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.log4j.BasicConfigurator;
import org.hps.online.recon.Server;

/**
 * Logging config for the online reconstruction {@link org.hps.online.recon.Server}
 */
public class ServerLoggingConfig {

    private static final String LOG_FILE_PATH = System.getProperty("user.dir") + File.separator + "logs";

    /**
     * Edit this and recompile the module to change the server log level
     */
    private static final Level LEVEL = Level.FINER;

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

        // Fix stupid log4j warnings and turn all messages off
        BasicConfigurator.configure();
        org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);

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
        Logger logger = Logger.getLogger(Server.class.getPackage().getName());
        logger.setLevel(LEVEL);
        rootLogger.setLevel(Level.WARNING);
        System.out.println("Server log file: " + logFile.getAbsolutePath());
    }
}
