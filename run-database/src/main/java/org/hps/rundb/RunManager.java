package org.hps.rundb;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.database.ConnectionParameters;
import org.hps.record.daqconfig.DAQConfig;
import org.hps.record.epics.EpicsData;
import org.hps.record.scalers.ScalerData;
import org.hps.record.svt.SvtConfigData;
import org.hps.record.triggerbank.TriggerConfigData;
import org.lcsim.conditions.ConditionsEvent;
import org.lcsim.conditions.ConditionsListener;

/**
 * Manages access to the run database.
 *
 * @author jeremym
 */
public final class RunManager implements ConditionsListener {
    
    /**
     * The default connection parameters for read-only access to the run database.
     */
    private static ConnectionParameters DEFAULT_CONNECTION_PARAMETERS = new ConnectionParameters("hpsuser",
            "darkphoton", "hps_run_db_v2", "hpsdb.jlab.org");
    
    /**
     * Database connection parameters.
     */
    private ConnectionParameters connectionParameters = DEFAULT_CONNECTION_PARAMETERS;

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
     * Factory for creating database API objects.
     */
    private final DaoProvider factory;

    /**
     * The run number; the -1 value indicates that this has not been set externally yet.
     */
    private Integer run = null;

    /**
     * Class constructor.     
     * @param connection the database connection
     */
    public RunManager(final Connection connection) {
        try {
            if (connection.isClosed()) {
                throw new RuntimeException("The connection is already closed and cannot be used.");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        this.connection = connection;
        factory = new DaoProvider(this.connection);
        LOGGER.log(Level.INFO, this.connectionParameters.toString());
    }
    
    /**
     * Class constructor using default connection parameters.
     */
    public RunManager() {        
        this.connection = this.connectionParameters.createConnection();
        factory = new DaoProvider(this.connection);
        LOGGER.log(Level.INFO, this.connectionParameters.toString());
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
     * @param conditionsEvent the event with new conditions information
     */
    @Override
    public synchronized void conditionsChanged(final ConditionsEvent conditionsEvent) {
        this.setRun(conditionsEvent.getConditionsManager().getRun());
    }

    /**
     * Return the database connection.     
     * @return the database connection
     */
    public Connection getConnection() {
        return this.connection;
    }

    /**
     * Get the EPICS data for the current run.
     * @param epicsType the type of EPICS data
     * @return the EPICS data for the current run
     */
    public List<EpicsData> getEpicsData(final EpicsType epicsType) {
        return factory.getEpicsDataDao().getEpicsData(epicsType, this.run);
    }

    /**
     * Get the list of EPICS variables definitions.     
     * @param epicsType the type of EPICS data
     * @return the list of EPICS variable definitions
     */
    public List<EpicsVariable> getEpicsVariables(final EpicsType epicsType) {
        return factory.getEpicsVariableDao().getEpicsVariables(epicsType);
    }

    /**
     * Get the full list of run numbers from the database.     
     * @return the complete list of run numbers
     */
    public List<Integer> getRuns() {
        return factory.getRunSummaryDao().getRuns();
    }
  
    /**
     * Get the run summary for the current run.     
     * @return the run summary for the current run
     */
    public RunSummary getRunSummary() {
        return factory.getRunSummaryDao().getRunSummary(this.run);
    }

    /**
     * Get the scaler data for the current run.     
     * @return the scaler data for the current run
     */
    public List<ScalerData> getScalerData() {
        return factory.getScalerDataDao().getScalerData(this.run);
    }
    
    /**
     * Get SVT configuration data.     
     * @return the SVT configuration data
     */
    public List<SvtConfigData> getSvtConfigData() {
        return factory.getSvtConfigDao().getSvtConfigs(this.run);
    }
    
    /**
     * Get the DAQ (trigger) configuration for the run.
     * @return the DAQ configuration for the run
     */
    public DAQConfig getDAQConfig() {
        TriggerConfigData config = factory.getTriggerConfigDao().getTriggerConfig(this.run);
        return config.loadDAQConfig(this.run);
    }   

    /**
     * Return <code>true</code> if the run exists in the database.
     * @return <code>true</code> if the run exists in the database
     */
    public boolean runExists() {      
        return factory.getRunSummaryDao().runSummaryExists(this.run);
    }

    /**
     * Set the run number and then load the applicable {@link RunSummary} from the database.
     * @param run the run number
     */
    public void setRun(final int run) {
        if (this.run == null || run != this.run) {
            LOGGER.info("setting run " + run);
            // Set the run number.
            this.run = run;
        }
    }
    
    /**
     * Get the currently active run number or <code>null</code>.
     * @return the currently active run number of <code>null</code>
     */
    public Integer getRun() {
        return this.run;
    }
    
    /**
     * Create or replace a run summary in the database.
     * @param runSummary the run summary to update
     * @param replaceExisting <code>true</code> to allow an existing run summary to be replaced
     */
    public void updateRunSummary(RunSummary runSummary, boolean replaceExisting) {
        final RunSummaryDao runSummaryDao = factory.getRunSummaryDao();
        RunManager runManager = new RunManager();
        runManager.setRun(runSummary.getRun());
        if (runManager.runExists()) {
            if (replaceExisting) {
                runSummaryDao.updateRunSummary(runSummary);
            } else {
                throw new RuntimeException("Run already exists and replacement is not allowed.");
            }
        } else {
            runSummaryDao.insertRunSummary(runSummary);
        }                
    }
    
    /**
     * Create or replace the trigger config for the run.
     * @param triggerConfig the trigger config
     * @param replaceExisting <code>true</code> to allow an existing trigger to be replaced
     */
    public void updateTriggerConfig(TriggerConfigData triggerConfig, boolean replaceExisting) {
        final TriggerConfigDao configDao = factory.getTriggerConfigDao();
        if (configDao.getTriggerConfig(run) != null) {
            if (replaceExisting) {
                configDao.deleteTriggerConfig(run);
            } else {
                throw new RuntimeException("Run already exists and replacement is not allowed.");
            }
        }
        configDao.insertTriggerConfig(triggerConfig, run);
    }
    
    /**
     * Create or replace EPICS data for the run.
     * @param epicsData the EPICS data
     */
    public void updateEpicsData(List<EpicsData> epicsData) {
        if (epicsData != null && !epicsData.isEmpty()) {
            factory.getEpicsDataDao().insertEpicsData(epicsData, this.run);
        }
    }
    
    /**
     * Create or replace scaler data for the run.
     * @param scalerData the scaler data
     */
    public void updateScalerData(List<ScalerData> scalerData) {
        if (scalerData != null) {
            factory.getScalerDataDao().insertScalerData(scalerData, this.run);
        } 
    }     
    
    /**
     * Delete a run from the database.
     * @param run the run number
     */
    public void deleteRun() {        
        factory.getEpicsDataDao().deleteEpicsData(EpicsType.EPICS_2S, run);
        factory.getEpicsDataDao().deleteEpicsData(EpicsType.EPICS_20S, run);
        factory.getScalerDataDao().deleteScalerData(run);
        factory.getSvtConfigDao().deleteSvtConfigs(run);
        factory.getTriggerConfigDao().deleteTriggerConfig(run);
        factory.getRunSummaryDao().deleteRunSummary(run);
    }
    
}
