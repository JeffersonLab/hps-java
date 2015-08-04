package org.hps.record.run;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.database.ConnectionParameters;
import org.lcsim.conditions.ConditionsEvent;
import org.lcsim.conditions.ConditionsListener;
import org.lcsim.util.log.DefaultLogFormatter;
import org.lcsim.util.log.LogUtil;

/**
 * Manages read-only access to the run database and creates a {@link RunSummary} object from the data for a specific
 * run.
 * <p>
 * This class converts database records into {@link RunSummary}, {@link org.hps.record.epics.EpicsData},
 * {@link org.hps.record.scalers.ScalerData}, and {@link org.hps.record.evio.crawler.EvioFileList} objects using their
 * corresponding {@link AbstractRunDatabaseReader} implementation classes.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class RunManager implements ConditionsListener {

    /**
     * The default connection parameters for read-only access to the run database using the standard 'hpsuser' account.
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
    private ConnectionParameters connectionParameters = DEFAULT_CONNECTION_PARAMETERS;

    /**
     * The run number; the -1 value indicates that this has not been set externally yet.
     */
    private int run = -1;

    /**
     * The {@link RunSummary} for the current run.
     */
    private RunSummary runSummary = null;

    void closeConnection() {
        try {
            this.connection.close();
        } catch (final SQLException e) {
            e.printStackTrace();
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
     * Get the database connection.
     *
     * @return the database connection or <code>null</code> if it is not set
     */
    Connection getConnection() {
        return this.connection;
    }

    /**
     * Get the run number.
     *
     * @return the run number
     */
    public int getRun() {
        return run;
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
     * Read information from the run database and create a {@link RunSummary} from it.
     */
    private void readRun() {

        // Read main RunSummary object but not objects that it references.
        final RunSummaryReader runSummaryReader = new RunSummaryReader();
        runSummaryReader.setRun(this.getRun());
        runSummaryReader.setConnection(this.getConnection());
        runSummaryReader.read();
        this.setRunSummary(runSummaryReader.getData());

        // Read EpicsData and set on RunSummary.
        final EpicsDataReader epicsDataReader = new EpicsDataReader();
        epicsDataReader.setRun(this.getRun());
        epicsDataReader.setConnection(this.getConnection());
        epicsDataReader.read();
        this.getRunSummary().setEpicsData(epicsDataReader.getData());

        // Read ScalerData and set on RunSummary.
        final ScalerDataReader scalerDataReader = new ScalerDataReader();
        scalerDataReader.setRun(this.getRun());
        scalerDataReader.setConnection(this.getConnection());
        scalerDataReader.read();
        this.getRunSummary().setScalerData(scalerDataReader.getData());

        // Read ScalerData and set on RunSummary.
        final EvioFileListReader evioFileListReader = new EvioFileListReader();
        evioFileListReader.setRun(this.getRun());
        evioFileListReader.setConnection(this.getConnection());
        evioFileListReader.read();
        this.getRunSummary().setEvioFileList(evioFileListReader.getData());
    }

    /**
     * Check if the current run number exists in the run database.
     *
     * @return <code>true</code> if run exists
     */
    private boolean runExists() throws SQLException {
        PreparedStatement statement = null;
        boolean exists = false;
        try {
            statement = connection.prepareStatement("SELECT run FROM runs where run = ?");
            statement.setInt(1, this.run);
            final ResultSet resultSet = statement.executeQuery();
            exists = resultSet.next();
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return exists;
    }

    /**
     * Set the database connection parameters.
     */
    public void setConnectionParameters(final ConnectionParameters connectionParameters) {
        this.connectionParameters = connectionParameters;
    }

    /**
     * Set the run number and load the applicable {@link RunSummary} from the db.
     *
     * @param run the run number
     */
    synchronized void setRun(final int run) {

        if (run < 0) {
            throw new IllegalArgumentException("invalid run number: " + run);
        }

        try {

            // Setup the database connection.
            if (this.connection == null || this.connection.isClosed()) {
                this.connection = connectionParameters.createConnection();
            }

            // Set the current run number.
            this.run = run;

            // Does the current run exist in the database?
            if (this.runExists()) {
                LOGGER.info("run record found in hps_run_db for " + run);
                try {
                    // Read the records from the database and convert into Java objects.
                    this.readRun();
                } catch (final Exception e) {
                    // There was some unknown error when reading in the run records.
                    LOGGER.log(Level.SEVERE, "Error reading from run database for run: " + run, e);
                    throw new RuntimeException(e);
                }
            } else {
                // Run is not in the database.
                LOGGER.warning("run database record does not exist for run " + run);
            }

            // Close the database connection.
            this.connection.close();

        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Set the current {@link RunSummary}.
     *
     * @param runSummary the current {@link RunSummary}
     */
    void setRunSummary(final RunSummary runSummary) {
        this.runSummary = runSummary;
    }
}
