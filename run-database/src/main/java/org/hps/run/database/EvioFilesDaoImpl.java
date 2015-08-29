package org.hps.run.database;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of database interface for EVIO files in the run database.
 *
 * @author Jeremy McCormick, SLAC
 */
final class EvioFilesDaoImpl implements EvioFilesDao {

    /**
     * SQL query strings.
     */
    private static final class EvioFilesQuery {

        /**
         * Delete files by run number.
         */
        private static final String DELETE_RUN = "DELETE FROM run_files where run = ?";
        /**
         * Insert files by run number.
         */
        private static final String INSERT_RUN = "INSERT INTO run_files (run, directory, name) VALUES(?, ?, ?)";
        /**
         * Select all records.
         */
        private static final String SELECT_ALL = "SELECT directory, name FROM run_files";
        /**
         * Select records by run number.
         */
        private static final String SELECT_RUN = "SELECT directory, name FROM run_files WHERE run = ?";
    }

    /**
     * The database connection.
     */
    private final Connection connection;

    public EvioFilesDaoImpl(final Connection connection) {
        this.connection = connection;
    }

    /**
     * Delete the EVIO file records for a run.
     *
     * @param run the run number
     */
    @Override
    public void deleteEvioFiles(final int run) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(EvioFilesQuery.DELETE_RUN);
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
     * Get all EVIO files from the database.
     *
     * @return all EVIO files from the database
     */
    @Override
    public List<File> getAllEvioFiles() {
        Statement statement = null;
        final List<File> evioFileList = new ArrayList<File>();
        try {
            statement = this.connection.createStatement();
            final ResultSet resultSet = statement.executeQuery(EvioFilesQuery.SELECT_ALL);
            while (resultSet.next()) {
                evioFileList.add(new File(resultSet.getString("directory") + File.separator
                        + resultSet.getString("name")));
            }
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
        return evioFileList;
    }

    /**
     * Get a list of EVIO files by run number.
     *
     * @param run the run number
     * @return the list of EVIO files for the run
     */
    @Override
    public List<File> getEvioFiles(final int run) {
        PreparedStatement preparedStatement = null;
        final List<File> evioFileList = new ArrayList<File>();
        try {
            preparedStatement = this.connection.prepareStatement(EvioFilesQuery.SELECT_RUN);
            preparedStatement.setInt(1, run);
            final ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                evioFileList.add(new File(resultSet.getString("directory") + File.separator
                        + resultSet.getString("name")));
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
        return evioFileList;
    }

    /**
     * Insert the list of files for a run.
     *
     * @param fileList the list of files
     * @param run the run number
     */
    @Override
    public void insertEvioFiles(final List<File> fileList, final int run) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(EvioFilesQuery.INSERT_RUN);
            for (final File file : fileList) {
                preparedStatement.setInt(1, run);
                preparedStatement.setString(2, file.getParentFile().getPath());
                preparedStatement.setString(3, file.getName());
                preparedStatement.executeUpdate();
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
    }
}
