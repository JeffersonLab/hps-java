package org.hps.record.run;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hps.record.evio.crawler.EvioFileList;

/**
 * Convert run database records from the <i>run_files</i> table into an {@link EvioFileList} object.
 *
 * @author Jeremy McCormick, SLAC
 */
final class EvioFileListReader extends AbstractRunDatabaseReader<EvioFileList> {

    /**
     * The SQL SELECT query string.
     */
    private final String SELECT_SQL = "SELECT directory, name FROM run_files WHERE run = ?";

    /**
     * Read data from the database and convert to an {@link EvioFileList} object.
     */
    @Override
    void read() {
        if (this.getRun() == -1) {
            throw new IllegalStateException("run number is invalid: " + this.getRun());
        }
        if (this.getConnection() == null) {
            throw new IllegalStateException("Connection is not set.");
        }

        PreparedStatement statement = null;
        try {
            statement = this.getConnection().prepareStatement(SELECT_SQL);
            statement.setInt(1, this.getRun());
            final ResultSet resultSet = statement.executeQuery();

            final EvioFileList evioFileList = new EvioFileList();

            while (resultSet.next()) {
                evioFileList.add(new File(resultSet.getString("directory") + File.separator
                        + resultSet.getString("name")));
            }

            this.setData(evioFileList);

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
    }

}
