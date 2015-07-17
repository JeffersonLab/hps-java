package org.hps.record.run;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lcsim.conditions.ConditionsEvent;
import org.lcsim.conditions.ConditionsListener;
import org.lcsim.util.log.DefaultLogFormatter;
import org.lcsim.util.log.LogUtil;

/**
 * Manages access to the run database and creates a {@link RunSummary} object from the data for a specific run.
 * <p>
 * This class can also convert database records into {@link org.hps.record.epics.EpicsData},
 * {@link org.hps.record.scalers.ScalerData}, and {@link org.hps.record.evio.crawler.EvioFileList} using their
 * {@link AbstractRunDatabaseReader} implementation classes.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class RunManager implements ConditionsListener {

    /**
     * The singleton instance of the RunManager.
     */
    private static RunManager INSTANCE;

    /**
     * The class's logger.
     */
    private static Logger LOGGER = LogUtil.create(RunManager.class, new DefaultLogFormatter(), Level.ALL);

    /**
     * Get the instance of the {@link RunManager}.
     *
     * @return the instance of the {@link RunManager}.
     */
    public static RunManager getRunManager() {
        if (INSTANCE == null) {
            INSTANCE = new RunManager();
        }
        return INSTANCE;
    }

    /**
     * The database connection.
     */
    private Connection connection;

    /**
     * The run number; the -1 value indicates that this has not been set externally yet.
     */
    private int run = -1;

    /**
     * The {@link RunSummary} for the current run.
     */
    private RunSummary runSummary = null;

    @Override
    public void conditionsChanged(final ConditionsEvent conditionsEvent) {
        final int newRun = conditionsEvent.getConditionsManager().getRun();
        LOGGER.info("initializing for run " + newRun + " ...");
        this.setRun(newRun);
        LOGGER.info("done initializing for run " + this.getRun());
    }

    /**
     * Get the database connection.
     *
     * @return the database connection
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
        // Load main RunSummary object.
        final RunSummaryReader runSummaryReader = new RunSummaryReader();
        runSummaryReader.setRun(this.getRun());
        runSummaryReader.setConnection(this.getConnection());
        runSummaryReader.read();
        this.setRunSummary(runSummaryReader.getData());

        // Set EpicsData on RunSummary.
        final EpicsDataReader epicsDataReader = new EpicsDataReader();
        epicsDataReader.setRun(this.getRun());
        epicsDataReader.setConnection(this.getConnection());
        epicsDataReader.read();
        this.getRunSummary().setEpicsData(epicsDataReader.getData());

        // Set ScalerData on RunSummary.
        final ScalerDataReader scalerDataReader = new ScalerDataReader();
        scalerDataReader.setRun(this.getRun());
        scalerDataReader.setConnection(this.getConnection());
        scalerDataReader.read();
        this.getRunSummary().setScalerData(scalerDataReader.getData());

        // Set ScalerData on RunSummary.
        final EvioFileListReader evioFileListReader = new EvioFileListReader();
        evioFileListReader.setRun(this.getRun());
        evioFileListReader.setConnection(this.getConnection());
        evioFileListReader.read();
        this.getRunSummary().setEvioFileList(evioFileListReader.getData());
    }

    /**
     * Set the database connection.
     *
     * @param connection the database connection
     */
    public void setConnection(final Connection connection) {
        this.connection = connection;
    }

    /**
     * Set the run number.
     *
     * @param run the run number
     */
    public void setRun(final int run) {

        // Check status of database connection (must be open).
        try {
            if (this.connection.isClosed()) {
                throw new IllegalStateException("The connection is closed.");
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }

        this.run = run;

        try {
            // Read the run records from the database and convert into Java objects.
            this.readRun();
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Error reading from run database for run: " + run, e);
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
