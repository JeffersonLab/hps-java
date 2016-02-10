package org.hps.run.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.hps.record.epics.EpicsData;
import org.hps.record.epics.EpicsHeader;

/**
 * Implementation of database operations for EPICS data.
 *
 * @author Jeremy McCormick, SLAC
 */
final class EpicsDataDaoImpl implements EpicsDataDao {

    /**
     * Initialize the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(EpicsDataDaoImpl.class.getPackage().getName());
    
    /**
     * The database connection.
     */
    private final Connection connection;

    /**
     * The database interface to get EPICS variable information.
     */
    private final EpicsVariableDao epicsVariableDao;

    /**
     * Create a new DAO implementation for EPICS data.
     *
     * @param connection the database connection
     */
    public EpicsDataDaoImpl(final Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("The connection is null.");
        }
        this.connection = connection;
        this.epicsVariableDao = new EpicsVariableDaoImpl(this.connection);
    }

    /**
     * Create SQL insert string for the EPICS type.
     *
     * @param epicsType the EPICS type
     * @return the SQL insert string for the type
     */
    private String createInsertSql(final EpicsType epicsType) {
        final StringBuffer sb = new StringBuffer();
        sb.append("INSERT INTO " + epicsType.getTableName() + " ( epics_header_id, ");
        final List<EpicsVariable> variables = epicsVariableDao.getEpicsVariables(epicsType);
        for (final EpicsVariable variable : variables) {
            sb.append(variable.getColumnName() + ", ");
        }
        sb.setLength(sb.length() - 2);
        sb.append(" ) VALUES ( ?, ");
        for (int i = 0; i < variables.size(); i++) {
            sb.append("?, ");
        }
        sb.setLength(sb.length() - 2);
        sb.append(" )");
        return sb.toString();
    }

    /**
     * Delete all EPICS data for a run from the database.
     * <p>
     * Only the <code>epics_header</code> records are deleted and the child records
     * are deleted automatically via a <code>CASCADE</code>.
     *
     * @param run the run number
     */
    @Override
    public void deleteEpicsData(final EpicsType epicsType, final int run) {
        PreparedStatement selectHeaderIds = null;
        PreparedStatement deleteEpicsData = null;
        PreparedStatement deleteHeader = null;
        try {
            selectHeaderIds = connection.prepareStatement("SELECT id FROM epics_headers WHERE run = ?");
            selectHeaderIds.setInt(1, run);
            final ResultSet headerResultSet = selectHeaderIds.executeQuery();
            deleteEpicsData = connection.prepareStatement("DELETE FROM " + epicsType.getTableName()
                    + " WHERE epics_header_id = ?");
            deleteHeader = connection.prepareStatement("DELETE FROM epics_headers WHERE id = ?");
            final Set<Integer> headerIds = new HashSet<Integer>();
            while (headerResultSet.next()) {
                headerIds.add(headerResultSet.getInt("id"));
            }
            for (final Integer headerId : headerIds) {
                deleteEpicsData.setInt(1, headerId);
                int rowsAffected = deleteEpicsData.executeUpdate();
                if (rowsAffected == 0) {
                    throw new SQLException("Deletion of EPICS data failed; no rows affected.");
                }
                deleteHeader.setInt(1, headerId);
                rowsAffected = deleteHeader.executeUpdate();
                if (rowsAffected == 0) {
                    throw new SQLException("Deletion of EPICS header failed; no rows affected.");
                }
            }

        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (selectHeaderIds != null) {
                try {
                    selectHeaderIds.close();
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
            if (deleteEpicsData != null) {
                try {
                    deleteEpicsData.close();
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
            if (deleteHeader != null) {
                try {
                    deleteHeader.close();
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Get EPICS data by run.
     *
     * @param run the run number
     * @param epicsType the type of EPICS data (2s or 20s)
     * @return the EPICS data
     */
    @Override
    public List<EpicsData> getEpicsData(final EpicsType epicsType, final int run) {
        final List<EpicsData> epicsDataList = new ArrayList<EpicsData>();
        PreparedStatement selectHeader = null;
        PreparedStatement selectEpicsData = null;
        try {
            final List<EpicsVariable> variables = epicsVariableDao.getEpicsVariables(epicsType);
            selectEpicsData = connection.prepareStatement("SELECT * FROM " + epicsType.getTableName() 
                    + " LEFT JOIN epics_headers ON " + epicsType.getTableName() + ".epics_header_id = epics_headers.id"
                    + " WHERE epics_headers.run = ? ORDER BY epics_headers.sequence");
            selectEpicsData.setInt(1, run);
            ResultSet resultSet = selectEpicsData.executeQuery();
            while (resultSet.next()) {
                EpicsData epicsData = new EpicsData();
                final int headerRun = resultSet.getInt("epics_headers.run");
                final int sequence = resultSet.getInt("epics_headers.sequence");
                final int timestamp = resultSet.getInt("epics_headers.timestamp");
                final EpicsHeader header = new EpicsHeader(new int[] {headerRun, sequence, timestamp});
                epicsData.setEpicsHeader(header);
                for (final EpicsVariable variable : variables) {
                    final double value = resultSet.getDouble(variable.getColumnName());
                    epicsData.setValue(variable.getVariableName(), value);
                }
                epicsDataList.add(epicsData);
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (selectHeader != null) {
                try {
                    selectHeader.close();
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
            if (selectEpicsData != null) {
                try {
                    selectEpicsData.close();
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return epicsDataList;
    }

    /**
     * Insert a list of EPICS data into the database.
     * <p>
     * By default, the run number from the header will be used, but it will be overridden
     * if it does not match the <code>run</code> argument.  (There are a few data files
     * where the run in the EPICS header is occassionally wrong.)
     *
     * @param epicsDataList the list of EPICS data
     */
    @Override
    public void insertEpicsData(final List<EpicsData> epicsDataList, int run) {
        if (epicsDataList.isEmpty()) {
            throw new IllegalArgumentException("The EPICS data list is empty.");
        }
        PreparedStatement insertHeaderStatement = null;
        try {
            insertHeaderStatement = connection.prepareStatement(
                    "INSERT INTO epics_headers (run, sequence, timestamp) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            for (final EpicsData epicsData : epicsDataList) {
                final EpicsHeader epicsHeader = epicsData.getEpicsHeader();
                if (epicsHeader == null) {
                    throw new IllegalArgumentException("The EPICS data is missing a header.");
                }
                insertHeaderStatement.setInt(1, run); /* Don't use run from bank as it is sometimes wrong! */
                insertHeaderStatement.setInt(2, epicsHeader.getSequence());
                insertHeaderStatement.setInt(3, epicsHeader.getTimestamp());
                LOGGER.finer("creating EPICs record with run = " + run + " ; seq = " 
                        + epicsHeader.getSequence() + "; ts = " + epicsHeader.getTimestamp());
                final int rowsCreated = insertHeaderStatement.executeUpdate();
                if (rowsCreated == 0) {
                    throw new SQLException("Creation of EPICS header record failed; no rows affected.");
                }
                int headerId = 0;
                try (ResultSet generatedKeys = insertHeaderStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        headerId = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Creation of EPICS header record failed; no ID obtained.");
                    }
                }
                final EpicsType epicsType = EpicsType.getEpicsType(epicsData);
                final String insertSql = this.createInsertSql(epicsType);
                final List<EpicsVariable> variables = epicsVariableDao.getEpicsVariables(epicsType);
                final PreparedStatement insertStatement = connection.prepareStatement(insertSql);
                insertStatement.setInt(1, headerId);
                int parameterIndex = 2;
                for (final EpicsVariable variable : variables) {
                    final String variableName = variable.getVariableName();
                    double value = 0;
                    if (epicsData.hasKey(variableName)) {
                        value = epicsData.getValue(variableName);
                    }
                    insertStatement.setDouble(parameterIndex, value);
                    ++parameterIndex;
                }
                final int dataRowsCreated = insertStatement.executeUpdate();
                if (dataRowsCreated == 0) {
                    throw new SQLException("Creation of EPICS data failed; no rows affected.");
                }
                LOGGER.finer("inserted EPICS data with run = " + run + "; seq = " + epicsHeader.getSequence() + "; ts = " 
                        + epicsHeader.getTimestamp());
                insertStatement.close();
            }
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (insertHeaderStatement != null) {
                try {
                    insertHeaderStatement.close();
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
