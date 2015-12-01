package org.hps.run.database;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Implementation of database operations for {@link RunSummary} objects in the run database.
 *
 * @author Jeremy McCormick, SLAC
 */
final class RunSummaryDaoImpl implements RunSummaryDao {

    /**
     * Expected number of string banks in trigger config.
     */
    private static final int TRIGGER_CONFIG_LEN = 4;

    /**
     * Delete by run number.
     */
    private static final String DELETE = "DELETE FROM run_summaries WHERE run = ?";
        
    /**
     * Insert a record for a run.
     */
    private static final String INSERT = "INSERT INTO run_summaries (run, nevents, nfiles, prestart_timestamp,"
            + " go_timestamp, end_timestamp, trigger_rate, trigger_config_name, trigger_config1, trigger_config2," 
            + " trigger_config3, trigger_config4, ti_time_offset, livetime_clock, livetime_fcup_tdc, livetime_fcup_trg,"
            + " target, notes, created) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
                     
    /**
     * Select record by run number.
     */
    private static final String SELECT = "SELECT * FROM run_summaries WHERE run = ?";
        
    /**
     * Update information for a run.
     */
    private static final String UPDATE = "UPDATE run_summaries SET nevents = ?, nfiles = ?, prestart_timestamp = ?,"
            + " go_timestamp = ?, end_timestamp = ?, trigger_rate = ?, trigger_config_name = ?, trigger_config1 = ?,"
            + " trigger_config2 = ?, trigger_config3 = ?, trigger_config4 = ?, ti_time_offset = ?, livetime_clock = ?,"
            + " livetime_fcup_tdc = ?, livetime_fcup_trg = ?, target = ?, notes = ?, created WHERE run = ?";

    /**
     * Initialize the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(RunSummaryDaoImpl.class.getPackage().getName());

    /**
     * The database connection.
     */
    private final Connection connection;
  
    /**
     * Create a new DAO object for run summary information.
     *
     * @param connection the database connection
     */
    RunSummaryDaoImpl(final Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("The connection is null.");
        }
        try {
            if (connection.isClosed()) {
                throw new IllegalArgumentException("The connection is closed.");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        this.connection = connection;
    }

    /**
     * Delete a run summary by run number.
     *
     * @param run the run number
     */
    @Override
    public void deleteRunSummary(final int run) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(DELETE);
            preparedStatement.setInt(1, run);
            preparedStatement.executeUpdate();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Delete a run summary but not its objects.
     *
     * @param runSummary the run summary object
     */
    @Override
    public void deleteRunSummary(final RunSummary runSummary) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(DELETE);
            preparedStatement.setInt(1, runSummary.getRun());
            preparedStatement.executeUpdate();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Get the list of run numbers.
     *
     * @return the list of run numbers
     */
    @Override
    public List<Integer> getRuns() {
        final List<Integer> runs = new ArrayList<Integer>();
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = this.connection.prepareStatement("SELECT distinct(run) FROM run_summaries ORDER BY run");
            final ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                final Integer run = resultSet.getInt(1);
                runs.add(run);
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return runs;
    }
   
    /**
     * Get a run summary.
     *
     * @param run the run number
     * @return the run summary object
     */
    @Override
    public RunSummary getRunSummary(final int run) {
        PreparedStatement statement = null;
        RunSummaryImpl runSummary = null;
        try {
            statement = this.connection.prepareStatement(SELECT);
            statement.setInt(1, run);
            final ResultSet resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                throw new IllegalArgumentException("Run " + run + " does not exist in database.");
            }
            runSummary = new RunSummaryImpl(run);
            runSummary.setTotalEvents(resultSet.getInt("nevents"));
            runSummary.setTotalFiles(resultSet.getInt("nfiles"));
            runSummary.setPrestartTimestamp(resultSet.getInt("prestart_timestamp"));
            runSummary.setGoTimestamp(resultSet.getInt("go_timestamp"));
            runSummary.setEndTimestamp(resultSet.getInt("end_timestamp"));
            runSummary.setTriggerRate(resultSet.getDouble("trigger_rate"));
            runSummary.setTriggerConfigName(resultSet.getString("trigger_config_name"));
            Map<Integer, String> triggerConfigData = createTriggerConfigData(resultSet);
            if (!triggerConfigData.isEmpty()) {
                runSummary.setTriggerConfigData(triggerConfigData);
            } 
            runSummary.setTiTimeOffset(resultSet.getLong("ti_time_offset"));
            runSummary.setLivetimeClock(resultSet.getDouble("livetime_clock"));
            runSummary.setLivetimeFcupTdc(resultSet.getDouble("livetime_fcup_tdc"));
            runSummary.setLivetimeFcupTrg(resultSet.getDouble("livetime_fcup_trg"));
            runSummary.setTarget(resultSet.getString("target"));
            runSummary.setNotes(resultSet.getString("notes"));
            runSummary.setCreated(resultSet.getTimestamp("created"));
            runSummary.setUpdated(resultSet.getTimestamp("updated"));
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return runSummary;
    }

    /**
     * Create trigger config data from result set.
     * 
     * @param resultSet the result set with the run summary record
     * @return the trigger config data as a map of bank number to string data
     * @throws SQLException if there is an error querying the database
     */
    private Map<Integer, String> createTriggerConfigData(final ResultSet resultSet) throws SQLException {
        Map<Integer, String> triggerConfigData = new LinkedHashMap<Integer, String>();
        Clob clob = resultSet.getClob("trigger_config1");            
        if (clob != null) {
            triggerConfigData.put(RunSummary.TRIGGER_CONFIG1, clob.getSubString(1, (int) clob.length()));
        }
        clob = resultSet.getClob("trigger_config2");
        if (clob != null) {
            triggerConfigData.put(RunSummary.TRIGGER_CONFIG2, clob.getSubString(1, (int) clob.length()));
        }
        clob = resultSet.getClob("trigger_config3");
        if (clob != null) {
            triggerConfigData.put(RunSummary.TRIGGER_CONFIG3, clob.getSubString(1, (int) clob.length()));
        }
        clob = resultSet.getClob("trigger_config4");
        if (clob != null) {
            triggerConfigData.put(RunSummary.TRIGGER_CONFIG4, clob.getSubString(1, (int) clob.length()));
        }
        return triggerConfigData;
    }
      
    /**
     * Insert a run summary.
     *
     * @param runSummary the run summary object
     */
    @Override
    public void insertRunSummary(final RunSummary runSummary) {
        PreparedStatement preparedStatement = null;        
        try {
            preparedStatement = connection.prepareStatement(INSERT);                       
            preparedStatement.setInt(1, runSummary.getRun());
            preparedStatement.setInt(2, runSummary.getTotalEvents());
            preparedStatement.setInt(3, runSummary.getTotalFiles());
            preparedStatement.setInt(4, runSummary.getPrestartTimestamp());
            preparedStatement.setInt(5, runSummary.getGoTimestamp());
            preparedStatement.setInt(6, runSummary.getEndTimestamp());
            preparedStatement.setDouble(7, runSummary.getTriggerRate());
            preparedStatement.setString(8, runSummary.getTriggerConfigName());
            Map<Integer, String> triggerData = runSummary.getTriggerConfigData();
            prepareTriggerData(preparedStatement, triggerData);
            preparedStatement.setLong(13, runSummary.getTiTimeOffset());
            preparedStatement.setDouble(14, runSummary.getLivetimeClock());
            preparedStatement.setDouble(15, runSummary.getLivetimeFcupTdc());
            preparedStatement.setDouble(16, runSummary.getLivetimeFcupTrg());
            preparedStatement.setString(17, runSummary.getTarget());
            preparedStatement.setString(18, runSummary.getNotes());
            LOGGER.fine(preparedStatement.toString());
            preparedStatement.executeUpdate();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Set trigger config data on prepared statement.
     * @param preparedStatement the prepared statement
     * @param triggerData the trigger config data
     * @throws SQLException if there is an error querying the database
     */
    private void prepareTriggerData(PreparedStatement preparedStatement, Map<Integer, String> triggerData)
            throws SQLException {
        if (triggerData != null && !triggerData.isEmpty()) {
            if (triggerData.size() != TRIGGER_CONFIG_LEN) {
                throw new IllegalArgumentException("The trigger config data has the wrong length.");
            }
            preparedStatement.setBytes(9, triggerData.get(RunSummary.TRIGGER_CONFIG1).getBytes());
            preparedStatement.setBytes(10, triggerData.get(RunSummary.TRIGGER_CONFIG2).getBytes());
            preparedStatement.setBytes(11, triggerData.get(RunSummary.TRIGGER_CONFIG3).getBytes());
            preparedStatement.setBytes(12, triggerData.get(RunSummary.TRIGGER_CONFIG4).getBytes());
        } else {
            preparedStatement.setBytes(9, null);
            preparedStatement.setBytes(10, null);
            preparedStatement.setBytes(11, null);
            preparedStatement.setBytes(12, null);
        }
    }
   
    /**
     * Return <code>true</code> if a run summary exists in the database for the run number.
     *
     * @param run the run number
     * @return <code>true</code> if run exists in the database
     */
    @Override
    public boolean runExists(final int run) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement("SELECT run FROM run_summaries where run = ?");
            preparedStatement.setInt(1, run);
            final ResultSet rs = preparedStatement.executeQuery();
            return rs.first();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Update a run summary.
     *
     * @param runSummary the run summary to update
     */
    @Override
    public void updateRunSummary(final RunSummary runSummary) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(UPDATE);                       
            preparedStatement.setInt(1, runSummary.getTotalEvents());
            preparedStatement.setInt(2, runSummary.getTotalFiles());
            preparedStatement.setInt(3, runSummary.getPrestartTimestamp());
            preparedStatement.setInt(4, runSummary.getGoTimestamp());
            preparedStatement.setInt(5, runSummary.getEndTimestamp());
            preparedStatement.setDouble(6, runSummary.getTriggerRate());
            preparedStatement.setString(7, runSummary.getTriggerConfigName());
            Map<Integer, String> triggerData = runSummary.getTriggerConfigData();
            if (triggerData != null && !triggerData.isEmpty()) {
                if (triggerData.size() != 4) {
                    throw new IllegalArgumentException("The trigger config data has the wrong length.");
                }
                preparedStatement.setBytes(8, triggerData.get(RunSummary.TRIGGER_CONFIG1).getBytes());
                preparedStatement.setBytes(9, triggerData.get(RunSummary.TRIGGER_CONFIG2).getBytes());
                preparedStatement.setBytes(10, triggerData.get(RunSummary.TRIGGER_CONFIG3).getBytes());
                preparedStatement.setBytes(11, triggerData.get(RunSummary.TRIGGER_CONFIG4).getBytes());
            } else {
                preparedStatement.setBytes(8, null);
                preparedStatement.setBytes(9, null);
                preparedStatement.setBytes(10, null);
                preparedStatement.setBytes(11, null);
            }
            preparedStatement.setLong(12, runSummary.getTiTimeOffset());
            preparedStatement.setDouble(13, runSummary.getLivetimeClock());
            preparedStatement.setDouble(14, runSummary.getLivetimeFcupTdc());
            preparedStatement.setDouble(15, runSummary.getLivetimeFcupTrg());
            preparedStatement.setString(16, runSummary.getTarget());
            preparedStatement.setString(17, runSummary.getNotes());
            preparedStatement.setInt(18, runSummary.getRun());
            LOGGER.fine(preparedStatement.toString());
            preparedStatement.executeUpdate();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }      
}
