package org.hps.conditions.api;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;

import org.hps.conditions.database.DatabaseConditionsManager;

/**
 * This is a collection of utility methods for {@link ConditionsObject}s.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class ConditionsObjectUtilities {

    /**
     * Static instance of conditions manager.
     */
    private static final DatabaseConditionsManager MANAGER = DatabaseConditionsManager.getInstance();

    /**
     * Convert from a raw string into a specific type.
     *
     * @param type the target type
     * @param value the raw value
     * @return the value converter to the given type
     */
    public static Object convertValue(final Class<?> type, final String value) throws ConditionsObjectException {
        if (Integer.class.equals(type)) {
            return Integer.parseInt(value);
        } else if (Double.class.equals(type)) {
            return Double.parseDouble(value);
        } else if (Float.class.equals(type)) {
            return Float.parseFloat(value);
        } else if (Boolean.class.equals(type)) {
            return Boolean.parseBoolean(value);
        } else if (Date.class.equals(type)) {
            try {
                return BaseConditionsObject.DEFAULT_DATE_FORMAT.parse(value);
            } catch (final ParseException e) {
                throw new ConditionsObjectException("Error parsing date.", e);
            }
        } else {
            return value;
        }
    }

    /**
     * Get the class name of a column.
     *
     * @param tableMetaData the table meta data
     * @param columnName the column name
     * @return the class name of a column
     * @throws SQLException if there is a problem querying the database
     */
    public static String getColumnClassName(final TableMetaData tableMetaData, final String columnName)
            throws SQLException {
        final Statement st = MANAGER.getConnection().createStatement();
        final ResultSet rs = st.executeQuery("SELECT * from " + tableMetaData.getTableName() + " LIMIT 1");
        final ResultSetMetaData rsmd = rs.getMetaData();
        for (int column = 1; column <= rsmd.getColumnCount(); column++) {
            if (rsmd.getColumnName(column).equals(columnName)) {
                return rsmd.getColumnClassName(column);
            }
        }
        throw new IllegalArgumentException("unknown columnName: " + columnName);
    }

    /**
     * Create a new conditions collection from the table name.
     *
     * @param tableName the name of the table
     * @return the new conditions collection
     * @throws ConditionsObjectException if there is an error creating the collection
     */
    public static ConditionsObjectCollection<?> newCollection(final String tableName) throws ConditionsObjectException {
        final TableMetaData tableInfo = TableRegistry.getTableRegistry().findByTableName(tableName);
        final ConditionsObjectCollection<?> collection = tableInfo.newCollection();
        collection.setConnection(MANAGER.getConnection());
        collection.setTableMetaData(tableInfo);
        return collection;
    }

    /**
     * Create a new conditions object from the table name.
     *
     * @param tableName the name of the table
     * @return the new conditions object
     * @throws ConditionsObjectException if there is an error creating the object
     */
    public static ConditionsObject newObject(final String tableName) throws ConditionsObjectException {
        final TableMetaData tableInfo = TableRegistry.getTableRegistry().findByTableName(tableName);
        final ConditionsObject object = tableInfo.newObject();
        object.setConnection(MANAGER.getConnection());
        object.setTableMetaData(tableInfo);
        return object;
    }

    /**
     * Setup a prepared statement from {@link ConditionsObject} data.
     *
     * @param statement the SQL <code>PreparedStatement</code>
     * @param object the {@link ConditionsObject} with the data
     * @throws SQLException if there is a problem querying the database
     */
    public static void setupPreparedStatement(final PreparedStatement statement, final ConditionsObject object)
            throws SQLException {
        int column = 1;
        final TableMetaData tableMetaData = object.getTableMetaData();
        final Collection<String> fieldNames = tableMetaData.getFieldNames();
        for (final String fieldName : fieldNames) {
            Object value = object.getFieldValue(fieldName);
            if (value != null) {
                final String className = ConditionsObjectUtilities.getColumnClassName(tableMetaData, fieldName);
                if (className.equals(Timestamp.class.getName())) {
                    value = new Timestamp(Date.class.cast(value).getTime());
                }
            }
            statement.setObject(column, value);
            ++column;
        }
    }

    /**
     * Do not allow class to be instantiated.
     */
    private ConditionsObjectUtilities() {
    }
}
