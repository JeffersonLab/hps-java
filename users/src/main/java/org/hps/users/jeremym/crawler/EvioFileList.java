package org.hps.users.jeremym.crawler;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;
import org.lcsim.util.log.LogUtil;

class EvioFileList extends ArrayList<File> {

    private static final Logger LOGGER = LogUtil.create(EvioFileList.class);

    void cache() {
        LOGGER.info("running cache commands ...");
        for (final File file : this) {
            EvioFileUtilities.cache(file);
        }
        LOGGER.info("done running cache commands");
    }

    int computeTotalEvents() {
        LOGGER.info("computing total events ...");
        int totalEvents = 0;
        for (final File file : this) {
            EvioReader reader = null;
            try {
                reader = new EvioReader(file, false);
                totalEvents += reader.getEventCount();
            } catch (EvioException | IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        LOGGER.info("done computing total events");
        return totalEvents;
    }

    File first() {
        return this.get(0);
    }

    File last() {
        return this.get(this.size() - 1);
    }

    void sort() {
        final List<File> fileList = new ArrayList<File>(this);
        Collections.sort(fileList, new EvioFileSequenceComparator());
        this.clear();
        this.addAll(fileList);
    }

    void insert(final Connection connection, final int run) throws SQLException {
        LOGGER.info("updating file list ...");
        PreparedStatement filesStatement = null;
        filesStatement = connection
                .prepareStatement("INSERT INTO run_log_files (run, directory, name) VALUES(?, ?, ?)");
        LOGGER.info("inserting files from run " + run + " into database");
        for (final File file : this) {
            LOGGER.info("creating update statement for " + file.getPath());
            filesStatement.setInt(1, run);
            filesStatement.setString(2, file.getParentFile().getPath());
            filesStatement.setString(3, file.getName());
            LOGGER.info("executing statement: " + filesStatement);
            filesStatement.executeUpdate();
            // connection.commit();
        }
        LOGGER.info("run_log_files was updated!");
    }
}