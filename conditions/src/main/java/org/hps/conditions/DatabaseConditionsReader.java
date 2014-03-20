package org.hps.conditions;

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
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: DatabaseConditionsReader.java,v 1.21 2013/10/18 06:08:55 jeremy Exp $ 
 */
public class DatabaseConditionsReader extends ConditionsReader {
        
    /** Database connection. */
    private Connection _connection = null;    
    
    /** Base ConditionsReader for getting the Detector. */
    private final ConditionsReader _reader;
    
    /** The current run number to determine if conditions are already loaded. */
    private int _currentRun = Integer.MIN_VALUE;
        
    /** The logger for printing messages. */
    static Logger _logger = null;

    /**
     * Class constructor taking a ConditionsReader.  This constructor is automatically called 
     * by the ConditionsManager when this type of reader has been requested via the detector 
     * properties file.
     * 
     * @param reader The basic ConditionsReader allowing access to the detector.
     */
    public DatabaseConditionsReader(ConditionsReader reader) {
        _reader = reader;
        setupLogger();
    }
    
    /**
     * Setup the logger.
     */
    private final void setupLogger() {
        if (_logger == null) {
            _logger = Logger.getLogger(getClass().getSimpleName());
            _logger.setUseParentHandlers(false);
            _logger.setLevel(Level.ALL);
            ConsoleHandler handler = new ConsoleHandler();
            handler.setFormatter(new ConditionsFormatter());
            _logger.addHandler(handler);
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
                
        _logger.info("updating detector <" + detectorName + "> for run <" + run + "> ...");
        
        // Check if conditions are already cached for the run.
        if (run == _currentRun) {
            _logger.warning("Conditions already cached for run <" + run + ">.");
            return false;
        }
            
        // Register the converters on the manager.         
        // FIXME: This should really only happen once instead of being called here every time.
        //ConditionsConverterRegistery.register(manager);
                
        // Open a connection to the database.
        _connection = ConnectionManager.getConnectionManager().createConnection();
        
        // Cache the ConditionsRecords.
        try {
            setup(run);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
                               
        // Close the database connection.
        try {
            _connection.close();
            _connection = null;
        } catch (SQLException x) {
            throw new IOException("Failed to close connection", x);
        }
                
        return true;
    }

    /**
     * Close the base reader.
     */
    public void close() throws IOException {
        _reader.close();
    }

    /**
     * Implementation of ConditionReader API method.
     * @return An InputStream with the conditions for <code>type</code>.
     */
    public InputStream open(String name, String type) throws IOException {
        return _reader.open(name, type);
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
