package org.hps.run.database;

import java.sql.Connection;

import org.hps.record.triggerbank.TriggerConfigData;

// TODO: add EPICs and scaler update

public class DatabaseUpdater {
    
    private Connection connection;
    private TriggerConfigData triggerConfig;
    private RunSummary runSummary;
    private boolean updateExisting = false;
                      
    DatabaseUpdater(Connection connection) {
        this.connection = connection;
    }
    
    void setTriggerConfigData(TriggerConfigData triggerConfig) {
        this.triggerConfig = triggerConfig;
    }
    
    void setRunSummary(RunSummary runSummary) {
        this.runSummary = runSummary;
    }
    
    void setUpdateExisting(boolean updateExisting) {
        this.updateExisting = updateExisting;
    }

    void update() {

        int run = runSummary.getRun();
        
        final DaoProvider runFactory = new DaoProvider(connection);
        final RunSummaryDao runSummaryDao = runFactory.getRunSummaryDao();
        
        RunManager runManager = new RunManager();
        runManager.setRun(runSummary.getRun());
        if (runManager.runExists()) {
            if (updateExisting) {
                runSummaryDao.updateRunSummary(runSummary);
            } else {
                throw new RuntimeException("Run already exists and updates are not allowed.");
            }
        } else {
            runSummaryDao.insertRunSummary(runSummary);
        }        
        
        final TriggerConfigDao configDao = runFactory.getTriggerConfigDao();
        if (configDao.getTriggerConfig(run) != null) {
            if (updateExisting) {
                configDao.deleteTriggerConfig(run);
            } else {
                throw new RuntimeException("Run already exists and updates are not allowed.");
            }
        }
        configDao.insertTriggerConfig(this.triggerConfig, run);
    }
}
