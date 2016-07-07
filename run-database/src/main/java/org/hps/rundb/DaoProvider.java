package org.hps.rundb;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Provider for creating database API objects for interacting with the run database.
 * 
 * @author jeremym
 */
public final class DaoProvider {

    /**
     * The database connection.
     */
    private final Connection connection;
    
    /* Object DAO interfaces created on demand. */
    private EpicsDataDao epicsDao;
    private EpicsVariableDao epicsVariableDao;
    private RunSummaryDao runSummaryDao;
    private ScalerDataDao scalerDao;
    private SvtConfigDao svtDao;
    private TriggerConfigDao configDao;

    /**
     * Create a new factory.
     *
     * @param connection the database connection
     */
    public DaoProvider(final Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("The connection is null.");
        }
        try {
            if (connection.isClosed()) {
                throw new IllegalStateException("The connection has already been closed.");
            }
        } catch (final SQLException e) {
            throw new IllegalStateException("Error when checking connection status.", e);
        }
        this.connection = connection;
    }

    /**
     * Get the EPICS DAO.
     *
     * @return the EPICS DAO
     */
    public EpicsDataDao getEpicsDataDao() {
        if (epicsDao == null) {
            epicsDao = new EpicsDataDaoImpl(connection); 
        }
        return epicsDao;
    }

    /**
     * Get the EPICS variable DAO.
     *
     * @return the EPICS variable DAO
     */
    public EpicsVariableDao getEpicsVariableDao() {
        if (epicsVariableDao == null) {
            epicsVariableDao = new EpicsVariableDaoImpl(connection); 
        }
        return epicsVariableDao;
    }

    /**
     * Get the run summary DAO.
     *
     * @return the run summary DAO
     */
    public RunSummaryDao getRunSummaryDao() {
        if (runSummaryDao == null) {
            runSummaryDao = new RunSummaryDaoImpl(connection); 
        }
        return runSummaryDao;
    }

    /**
     * Get the scaler data DAO.
     *
     * @return the scaler data DAO
     */
    public ScalerDataDao getScalerDataDao() {
        if (scalerDao == null) {
            scalerDao = new ScalerDataDaoImpl(connection); 
        }
        return scalerDao;
    }
    
    /**
     * Get the SVT config DAO.
     * 
     * @return the SVT config DAO
     */
    public SvtConfigDao getSvtConfigDao() {
        if (svtDao == null) {
            svtDao = new SvtConfigDaoImpl(connection); 
        }
        return svtDao;
    }
    
    /**
     * Get the trigger config DAO.
     * 
     * @return the trigger config DAO
     */
    public TriggerConfigDao getTriggerConfigDao() {
        if (configDao == null) {
            configDao = new TriggerConfigDaoImpl(connection); 
        }
        return configDao;
    }
}
