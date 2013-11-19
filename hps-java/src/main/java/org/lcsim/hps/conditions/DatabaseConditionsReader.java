package org.lcsim.hps.conditions;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsReader;

/**
 * <p>
 * This a rewritten version of Dima's ExampleDatabaseConditionsReader that attempts to handle 
 * the conditions and their meta data in a fully generic fashion.
 * </p>
 * 
 * <p>
 * In order to override the default database connection parameters, the system property
 * <code>hps.conditions.db.configuration</code> should point to a properties file defining 
 * the variables read by ConnectionParameters (see that class for details).  Otherwise, the 
 * defaults will be used to connect to a test database at SLAC.
 * <p>
 * 
 * <p>
 * Setting custom connection properties would look something like the following from the CL:
 * </p>
 * 
 * <p><code>java -Dhps.conditions.db.configuration=/path/to/my/config.prop [...]</code></p>
 * 
 * <p>
 * Currently, this class should "know" directly about all the converters that are needed for loading
 * conditions data via the <code>registerConditionsConverters</code> method.  
 * </p>
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: DatabaseConditionsReader.java,v 1.21 2013/10/18 06:08:55 jeremy Exp $ 
 */
public class DatabaseConditionsReader extends ConditionsReader {
        
    /** Database connection. */
    private Connection connection = null;    
    
    /** Base ConditionsReader for getting the Detector. */
    private final ConditionsReader reader;
    
    /** The current run number to determine if conditions are already loaded. */
    private int currentRun = Integer.MIN_VALUE;
    
    /** Converter for making ConditionsRecord objects from the database. */
    ConditionsRecordConverter conditionsRecordConverter = new ConditionsRecordConverter();
    
    /** The logger for printing messages. */
    static Logger logger = null;

    /**
     * Class constructor taking a ConditionsReader.  This constructor is automatically called 
     * by the ConditionsManager when this type of reader has been requested via the detector 
     * properties file.
     * 
     * @param reader The basic ConditionsReader allowing access to the detector.
     */
    public DatabaseConditionsReader(ConditionsReader reader) {
        this.reader = reader;        
        
        setupLogger();
    }
    
    /**
     * Setup the logger.
     */
    private final void setupLogger() {
        if (logger == null) {
            logger = Logger.getLogger(this.getClass().getSimpleName());
            logger.setUseParentHandlers(false);
            logger.setLevel(Level.ALL);
            ConsoleHandler handler = new ConsoleHandler();
            handler.setFormatter(new ConditionsFormatter());
            logger.addHandler(handler);
        }
    }
    
    /**
     * Simple log formatter for this class.
     */
    private static final class ConditionsFormatter extends Formatter {
        
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();
            sb.append(record.getLoggerName() + " [ " + record.getLevel() + " ] " + record.getMessage() + '\n');
            return sb.toString();
        }
    }
    
    /**
     * Update conditions for possibly new detector and run number.  
     * @param manager The current conditions manager.
     * @param detectorName The detector name.
     * @param run The run number.
     */
    public boolean update(ConditionsManager manager, String detectorName, int run) throws IOException {
                
        logger.info("updating detector <" + detectorName + "> for run <" + run + "> ...");
        
        // Check if conditions are already cached for the run.
        if (run == currentRun) {
            logger.warning("Conditions already cached for run <" + run + ">.");
            return false;
        }
            
        // Register the converters on the manager.         
        // FIXME: This should really only happen once instead of being called here every time.
        ConditionsConverterRegister.register(manager);
                
        // Open a connection to the database.
        connection = ConnectionManager.getConnectionManager().createConnection();
        
        // Cache the ConditionsRecords.
        try {
            setup(run);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
                               
        // Close the database connection.
        try {
            connection.close();
            connection = null;
        } catch (SQLException x) {
            throw new IOException("Failed to close connection", x);
        }
                
        return true;
    }

    /**
     * Close the base reader.
     */
    public void close() throws IOException {
        reader.close();
    }

    /**
     * Implementation of ConditionReader API method.
     * @return An InputStream with the conditions for <code>type</code>.
     */
    public InputStream open(String name, String type) throws IOException {
        return reader.open(name, type);
    }
     
    /**
     * This will cache the ConditionsRecords for the run.
     * @param run The run number.
     * @throws SQLException
     * @throws IOException
     */
    private final void setup(int run) throws SQLException, IOException {
        ConditionsRecord.find(run);
    }           
}
