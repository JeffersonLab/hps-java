package org.hps.rundb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Implementation of database operations for {@link RunSummary} objects in the run database.
 *
 * @author jeremym
 */
final class RunSummaryDaoImpl implements RunSummaryDao {

    /**
     * Delete by run number.
     */
    private static final String DELETE = "DELETE FROM run_summaries WHERE run = ?";
        
    /**
     * Insert a record for a run.
     */
    private static final String INSERT = "INSERT INTO run_summaries (run, nevents, nfiles, prestart_timestamp,"
            + " go_timestamp, end_timestamp, trigger_rate, trigger_config_name, ti_time_offset," 
            + " livetime_clock, livetime_fcup_tdc, livetime_fcup_trg, target, notes, created, updated)"
            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
    
    private static final String UPDATE = "UPDATE run_summaries SET nevents = ?, nfiles = ?, prestart_timestamp = ?,"
            + " go_timestamp = ?, end_timestamp = ?, trigger_rate = ?, trigger_config_name = ?, ti_time_offset = ?," 
            + " livetime_clock = ?, livetime_fcup_tdc = ?, livetime_fcup_trg = ?, target = ?, notes = ?, updated = NOW()"
            + " WHERE run = ?";
    
                     
    /**
     * Select record by run number.
     */
    private static final String SELECT = "SELECT * FROM run_summaries WHERE run = ?";
           
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
            runSummary.setTotalEvents(resultSet.getLong("nevents"));
            runSummary.setTotalFiles(resultSet.getInt("nfiles"));
            runSummary.setPrestartTimestamp(resultSet.getInt("prestart_timestamp"));
            runSummary.setGoTimestamp(resultSet.getInt("go_timestamp"));
            runSummary.setEndTimestamp(resultSet.getInt("end_timestamp"));
            runSummary.setTriggerRate(resultSet.getDouble("trigger_rate"));
            runSummary.setTriggerConfigName(resultSet.getString("trigger_config_name"));
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
            preparedStatement.setLong(2, runSummary.getTotalEvents());
            preparedStatement.setInt(3, runSummary.getTotalFiles());
            /* Use setObject on the rest as they may be null. */
            preparedStatement.setObject(4, runSummary.getPrestartTimestamp());
            preparedStatement.setObject(5, runSummary.getGoTimestamp());
            preparedStatement.setObject(6, runSummary.getEndTimestamp());
            preparedStatement.setObject(7, runSummary.getTriggerRate());
            preparedStatement.setObject(8, runSummary.getTriggerConfigName());
            preparedStatement.setObject(9, runSummary.getTiTimeOffset());
            preparedStatement.setObject(10, runSummary.getLivetimeClock());
            preparedStatement.setObject(11, runSummary.getLivetimeFcupTdc());
            preparedStatement.setObject(12, runSummary.getLivetimeFcupTrg());
            preparedStatement.setObject(13, runSummary.getTarget());
            preparedStatement.setObject(14, runSummary.getNotes());
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
    
    @Override
    public void updateRunSummary(RunSummary runSummary) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(UPDATE);                                   
            preparedStatement.setLong(1, runSummary.getTotalEvents());
            preparedStatement.setInt(2, runSummary.getTotalFiles());
            preparedStatement.setObject(3, runSummary.getPrestartTimestamp());
            preparedStatement.setObject(4, runSummary.getGoTimestamp());
            preparedStatement.setObject(5, runSummary.getEndTimestamp());
            preparedStatement.setObject(6, runSummary.getTriggerRate());
            preparedStatement.setObject(7, runSummary.getTriggerConfigName());
            preparedStatement.setObject(8, runSummary.getTiTimeOffset());
            preparedStatement.setObject(9, runSummary.getLivetimeClock());
            preparedStatement.setObject(10, runSummary.getLivetimeFcupTdc());
            preparedStatement.setObject(11, runSummary.getLivetimeFcupTrg());
            preparedStatement.setObject(12, runSummary.getTarget());
            preparedStatement.setObject(13, runSummary.getNotes());
            preparedStatement.setInt(14, runSummary.getRun());
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
     * Return <code>true</code> if a run summary exists in the database for the run number.
     *
     * @param run the run number
     * @return <code>true</code> if run exists in the database
     */
    @Override
    public boolean runSummaryExists(final int run) {
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
}
