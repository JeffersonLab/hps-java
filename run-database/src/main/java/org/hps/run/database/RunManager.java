package org.hps.run.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.database.ConnectionParameters;
import org.lcsim.conditions.ConditionsEvent;
import org.lcsim.conditions.ConditionsListener;
import org.lcsim.util.log.DefaultLogFormatter;
import org.lcsim.util.log.LogUtil;

/**
 * Manages read-only access to the run database and creates a {@link RunSummary} for a specific run.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class RunManager implements ConditionsListener {

    /**
     * The default connection parameters for read-only access to the run database.
     */
    private static ConnectionParameters DEFAULT_CONNECTION_PARAMETERS = new ConnectionParameters("hpsuser",
            "darkphoton", "hps_run_db", "hpsdb.jlab.org");

    /**
     * The singleton instance of the RunManager.
     */
    private static RunManager INSTANCE;

    /**
     * The class's logger.
     */
    private static Logger LOGGER = LogUtil.create(RunManager.class, new DefaultLogFormatter(), Level.ALL);

    /**
     * Get the global instance of the {@link RunManager}.
     *
     * @return the global instance of the {@link RunManager}
     */
    public static RunManager getRunManager() {
        if (INSTANCE == null) {
            INSTANCE = new RunManager();
        }
        return INSTANCE;
    }

    /**
     * The active database connection.
     */
    private Connection connection;

    /**
     * The database connection parameters, initially set to the default parameters.
     */
    private final ConnectionParameters connectionParameters = DEFAULT_CONNECTION_PARAMETERS;

    /**
     * The run number; the -1 value indicates that this has not been set externally yet.
     */
    private int run = -1;

    /**
     * The {@link RunSummary} for the current run.
     */
    private RunSummary runSummary = null;

    /**
     * Close the database connection.
     */
    private void closeConnection() {
        if (!(this.connection == null)) {
            try {
                if (!this.connection.isClosed()) {
                    this.connection.close();
                }
            } catch (final SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Load new run information when conditions have changed.
     */
    @Override
    public synchronized void conditionsChanged(final ConditionsEvent conditionsEvent) {
        this.setRun(conditionsEvent.getConditionsManager().getRun());
    }

    /**
     * Get the current run number.
     *
     * @return the run number
     */
    public int getRun() {
        return run;
    }

    /**
     * Get the complete list of run numbers from the database.
     *
     * @return the complete list of run numbers
     */
    List<Integer> getRuns() {
        return new RunSummaryDaoImpl(this.connection).getRuns();
    }

    /**
     * Get the current {@link RunSummary}.
     *
     * @return the current {@link RunSummary} or <code>null</code> if it is not set
     */
    public RunSummary getRunSummary() {
        return this.runSummary;
    }

    /**
     * Open a new database connection from the connection parameters if the current one is closed or <code>null</code>.
     * <p>
     * This method does nothing if the connection is already open.
     */
    private void openConnection() {
        try {
            if (this.connection == null || this.connection.isClosed()) {
                LOGGER.info("creating database connection");
                this.connection = connectionParameters.createConnection();
            } else {
                LOGGER.warning("connection already open");
            }
        } catch (final SQLException e) {
            throw new RuntimeException("Error opening database connection.", e);
        }
    }

    /**
     * Set the connection externally if using a database other than the default one at JLAB.
     *
     * @param connection the database connection
     */
    public void setConnection(final Connection connection) {
        this.connection = connection;
    }

    /**
     * Set the run number and then load the applicable {@link RunSummary} from the database.
     *
     * @param run the run number
     */
    public synchronized void setRun(final int run) {

        // Check if run number is valid.
        if (run < 0) {
            throw new IllegalArgumentException("invalid run number: " + run);
        }

        // Setup the database connection.
        this.openConnection();

        // Initialize database interface.
        final RunSummaryDao runSummaryDao = new RunSummaryDaoImpl(this.connection);

        // Set the current run number.
        this.run = run;

        // Does the current run exist in the database?
        if (runSummaryDao.runSummaryExists(this.getRun())) {
            LOGGER.info("run " + run + " found in database");
            try {
                // Read the records from the database and convert into complex Java object.
                this.runSummary = runSummaryDao.readFullRunSummary(this.getRun());
            } catch (final Exception e) {
                // There was some unknown error when reading in the run records.
                LOGGER.log(Level.SEVERE, "Error reading from run database.", e);
                throw new RuntimeException(e);
            }
        } else {
            // Run is not in the database.
            LOGGER.warning("run database record does not exist for run " + run);
        }

        // Close the database connection.
        this.closeConnection();
    }
    
    /**
     * Get the database connection.
     * 
     * @return the database connection
     */
    public RunDatabaseDaoFactory getDaoFactory() {
        return new RunDatabaseDaoFactory(this.connection);
    }
}
