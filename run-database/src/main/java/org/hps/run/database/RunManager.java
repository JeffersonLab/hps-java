package org.hps.run.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

import org.hps.conditions.database.ConnectionParameters;
import org.hps.record.daqconfig.DAQConfig;
import org.hps.record.epics.EpicsData;
import org.hps.record.scalers.ScalerData;
import org.hps.record.svt.SvtConfigData;
import org.lcsim.conditions.ConditionsEvent;
import org.lcsim.conditions.ConditionsListener;

/**
 * Manages read-only access to the run database and creates a {@link RunSummary} for a specific run.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class RunManager implements ConditionsListener {

    /**
     * Simple class for caching data.
     */
    private class DataCache {
        Boolean runExists = null;
        RunSummary runSummary = null;
        List<EpicsData> epicsData = null;
        List<ScalerData> scalerData = null;
        List<SvtConfigData> svtConfigData = null;
        DAQConfig daqConfig = null;
    }

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
     * Initialize the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(RunManager.class.getPackage().getName());

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
     * The data cache of run information.
     */
    private DataCache cache;

    /**
     * Factory for creating database API objects.
     */
    private final RunDatabaseDaoFactory factory;

    /**
     * The run number; the -1 value indicates that this has not been set externally yet.
     */
    private Integer run = null;

    /**
     * Class constructor using default connection parameters.
     */
    public RunManager() {
        this.connection = DEFAULT_CONNECTION_PARAMETERS.createConnection();
        this.openConnection();
        factory = new RunDatabaseDaoFactory(this.connection);
    }

    /**
     * Class constructor.
     *
     * @param connection the database connection
     */
    public RunManager(final Connection connection) {
        this.connection = connection;
        this.openConnection();
        factory = new RunDatabaseDaoFactory(this.connection);
    }

    /**
     * Check if the run number has been set.
     */
    private void checkRunNumber() {
        if (this.run == null) {
            throw new IllegalStateException("The run number was not set.");
        }
    }

    /**
     * Close the database connection.
     */
    public void closeConnection() {
        try {
            if (!this.connection.isClosed()) {
                this.connection.close();
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load new run information when conditions have changed.
     *
     * @param conditionsEvent the event with new conditions information
     */
    @Override
    public synchronized void conditionsChanged(final ConditionsEvent conditionsEvent) {
        this.setRun(conditionsEvent.getConditionsManager().getRun());
    }

    /**
     * Delete a run from the database.
     *
     * @param run the run number
     */
    void deleteRun() {        
        factory.createEpicsDataDao().deleteEpicsData(EpicsType.EPICS_2s, run);
        factory.createEpicsDataDao().deleteEpicsData(EpicsType.EPICS_20s, run);
        factory.createScalerDataDao().deleteScalerData(run);        
        factory.createSvtConfigDao().deleteSvtConfigs(run);
        factory.createTriggerConfigDao().deleteTriggerConfig(run);
        factory.createRunSummaryDao().deleteRunSummary(run);
    }

    /**
     * Return the database connection.
     *
     * @return the database connection
     */
    Connection getConnection() {
        return this.connection;
    }

    /**
     * Get the EPICS data for the current run.
     *
     * @param epicsType the type of EPICS data
     * @return the EPICS data for the current run
     */
    public List<EpicsData> getEpicsData(final EpicsType epicsType) {
        this.checkRunNumber();
        if (this.cache.epicsData == null) {
            LOGGER.info("loading EPICS data for run " + this.run);
            this.cache.epicsData = factory.createEpicsDataDao().getEpicsData(epicsType, this.run);
        }
        return this.cache.epicsData;
    }

    /**
     * Get the EPICS variables.
     *
     * @param epicsType the type of EPICS data
     * @return the EPICS data for the current run
     */
    public List<EpicsVariable> getEpicsVariables(final EpicsType epicsType) {
        return factory.createEpicsVariableDao().getEpicsVariables(epicsType);
    }

    /**
     * Get the current run number.
     *
     * @return the run number
     */
    public int getCurrentRun() {
        return run;
    }

    /**
     * Get the complete list of run numbers from the database.
     *
     * @return the complete list of run numbers
     */
    public List<Integer> getRuns() {
        return new RunSummaryDaoImpl(this.connection).getRuns();
    }
  
    /**
     * Get the run summary for the current run not including its sub-objects like scaler data.
     *
     * @return the run summary for the current run
     */
    public RunSummary getRunSummary() {
        this.checkRunNumber();
        if (this.cache.runSummary == null) {
            this.cache.runSummary = factory.createRunSummaryDao().getRunSummary(this.run);
        }
        return this.cache.runSummary;
    }

    /**
     * Get the scaler data for the current run.
     *
     * @return the scaler data for the current run
     */
    public List<ScalerData> getScalerData() {
        this.checkRunNumber();
        if (this.cache.scalerData == null) {
            LOGGER.info("loading scaler data for run " + this.run);
            this.cache.scalerData = factory.createScalerDataDao().getScalerData(run);
        }
        return this.cache.scalerData;
    }
    
    /**
     * Get SVT configuration data.
     * 
     * @return the SVT configuration data
     */
    public List<SvtConfigData> getSvtConfigData() {
        this.checkRunNumber();
        if (this.cache.svtConfigData == null) {
            LOGGER.info("loading SVT configuration data for run " + this.run);
            this.cache.svtConfigData = factory.createSvtConfigDao().getSvtConfigs(run);
        }
        return this.cache.svtConfigData;
    }
    
    /**
     * Get the DAQ configuration for the run.
     * 
     * @return the DAQ configuration for the run
     */
    public DAQConfig getDAQConfig() {
        this.checkRunNumber();
        if (this.cache.daqConfig == null) {
            TriggerConfig config = factory.createTriggerConfigDao().getTriggerConfig(run);
            cache.daqConfig = config.loadDAQConfig(run);
        }
        return this.cache.daqConfig;
    }
     
    /**
     * Open a new database connection from the connection parameters if the current one is closed or <code>null</code>.
     * <p>
     * This method does nothing if the connection is already open.
     */
    public void openConnection() {
        try {
            if (this.connection.isClosed()) {
                LOGGER.info("creating new database connection");
                this.connection = connectionParameters.createConnection();
            } 
        } catch (final SQLException e) {
            throw new RuntimeException("Error opening database connection.", e);
        }
    }

    /**
     * Return <code>true</code> if the run exists in the database.
     *
     * @return <code>true</code> if the run exists in the database
     */
    public boolean runExists() {
        this.checkRunNumber();
        if (this.cache.runExists == null) {
            this.cache.runExists = factory.createRunSummaryDao().runSummaryExists(this.run);
        }
        return this.cache.runExists;
    }

    /**
     * Return <code>true</code> if the run exists in the database.
     *
     * @param run the run number
     * @return <code>true</code> if the run exists in the database
     */
    boolean runExists(final int run) {
        return factory.createRunSummaryDao().runSummaryExists(run);
    }

    /**
     * Set the run number and then load the applicable {@link RunSummary} from the database.
     *
     * @param run the run number
     */
    public void setRun(final int run) {

        if (this.run == null || run != this.run) {

            LOGGER.info("setting new run " + run);

            // Set the run number.
            this.run = run;

            // Reset the data cache.
            this.cache = new DataCache();
        }
    }
}
