package org.hps.rundb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of database interface for EPICS variable information in the run database.
 */
final class EpicsVariableDaoImpl implements EpicsVariableDao {

    /**
     * The database connection.
     */
    private final Connection connection;

    /**
     * Create the object for accessing the db.
     *
     * @param connection the database connection
     */
    public EpicsVariableDaoImpl(final Connection connection) {
        this.connection = connection;
    }

    /**
     * Get the full list of EPICs variables.
     *
     * @return the full list of EPICS variables
     */
    @Override
    public List<EpicsVariable> getEpicsVariables() {
        final List<EpicsVariable> epicsVariables = new ArrayList<EpicsVariable>();
        Statement statement = null;
        try {
            statement = connection.createStatement();
            final ResultSet resultSet = statement.executeQuery("SELECT * FROM epics_variables");
            while (resultSet.next()) {
                final EpicsVariable epicsVariable = new EpicsVariable(resultSet.getString("variable"),
                        resultSet.getString("column_name"), resultSet.getString("description"),
                        resultSet.getInt("epics_type"));
                epicsVariables.add(epicsVariable);
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
        return epicsVariables;
    }

    /**
     * Get a list of EPICs variables by type.
     *
     * @param variableType the EPICS variable type
     * @return the list of variables
     */
    @Override
    public List<EpicsVariable> getEpicsVariables(final EpicsType variableType) {
        final List<EpicsVariable> epicsVariables = new ArrayList<EpicsVariable>();
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement("SELECT * FROM epics_variables WHERE epics_type = ?");
            preparedStatement.setInt(1, variableType.getTypeCode());
            final ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                final EpicsVariable epicsVariable = new EpicsVariable(resultSet.getString("variable"),
                        resultSet.getString("column_name"), resultSet.getString("description"),
                        resultSet.getInt("epics_type"));
                epicsVariables.add(epicsVariable);
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
        return epicsVariables;
    }

}
