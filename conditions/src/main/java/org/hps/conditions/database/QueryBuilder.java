package org.hps.conditions.database;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.hps.conditions.api.ConditionsObject;
import org.hps.conditions.api.FieldValueMap;

/**
 * This is a static utility class for building SQL queries for the conditions system.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class QueryBuilder {

    /**
     * Dot not allow instantiation.
     */
    private QueryBuilder() {
    }

    /**
     * Build a SQL select query string.
     * @param tableName the name of the table
     * @param collectionId the collection ID
     * @param fields the list of fields
     * @param orderBy the field to order by
     * @return the SQL query string
     */
    static String buildSelect(final String tableName, final int collectionId, final String[] fields,
            final String orderBy) {
        final StringBuffer buff = new StringBuffer();
        buff.append("SELECT ");
        if (fields == null) {
            buff.append("* ");
        } else {
            // Always implicitly include the row ID.
            buff.append("id, ");
            for (String fieldName : fields) {
                buff.append(fieldName + ", ");
            }
            buff.delete(buff.length() - 2, buff.length() - 1);
        }
        buff.append(" FROM " + tableName);
        if (collectionId != -1) {
            buff.append(" WHERE collection_id = " + collectionId);
        }
        if (orderBy != null) {
            buff.append(" ORDER BY " + orderBy);
        }
        return buff.toString();
    }

    /*
    static String buildUpdate(String tableName, int rowId, String[] fields, Object[] values) {
        if (fields.length != values.length)
            throw new IllegalArgumentException("The field and value arrays are different lengths.");
        StringBuffer buff = new StringBuffer();
        buff.append("UPDATE " + tableName + " SET ");
        for (int i = 0; i < fields.length; i++) {
            buff.append(fields[i] + " = '" + values[i] + "', ");
        }
        buff.delete(buff.length() - 2, buff.length() - 1);
        buff.append(" WHERE id = " + rowId);
        return buff.toString();
    }

    public static String buildInsert(String tableName, int collectionId, String[] fields, Object[] values) {
        if (fields.length != values.length)
            throw new IllegalArgumentException("The field and value arrays are different lengths.");
        StringBuffer buff = new StringBuffer();
        buff.append("INSERT INTO " + tableName + "( collection_id");
        for (String field : fields) {
            buff.append(", " + field);
        }
        buff.append(" ) VALUES ( " + collectionId);
        for (Object value : values) {
            buff.append(", " + value);
        }
        buff.append(") ");
        return buff.toString();
    }
    */

    /**
     * Build a prepared insert statement for a conditions object.
     * @param tableName the name of the table
     * @param object the conditions object
     * @return the prepared insert statement
     */
    static String buildPreparedInsert(final String tableName, final ConditionsObject object) {
        if (object.getFieldValues().size() == 0) {
            throw new IllegalArgumentException("The ConditionsObject has no values set.");
        }
        final StringBuffer buffer = new StringBuffer();
        buffer.append("INSERT INTO " + tableName + "( collection_id, ");
        for (String fieldName : object.getFieldValues().keySet()) {
            buffer.append(" " + fieldName + ",");
        }
        buffer.setLength(buffer.length() - 1);
        buffer.append(" ) VALUES ( ?,");
        for (Object value : object.getFieldValues().values()) {
            buffer.append(" ?,");
        }
        buffer.setLength(buffer.length() - 1);
        buffer.append(")");
        return buffer.toString();
    }

    /**
     * Date formatting for insert statement.
     */
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");

    /**
     * Build an insert statement.
     * @param tableName the table name
     * @param fieldValues the field values
     * @return the insert statement
     */
    public static String buildInsert(final String tableName, final FieldValueMap fieldValues) {
        if (fieldValues.size() == 0) {
            throw new IllegalArgumentException("The FieldValueMap has no values.");
        }
        final StringBuffer sb = new StringBuffer();
        sb.append("INSERT INTO " + tableName + " (");
        for (String fieldName : fieldValues.keySet()) {
            sb.append(" " + fieldName + ",");
        }
        sb.setLength(sb.length() - 1);
        sb.append(" ) VALUES (");
        for (Object value : fieldValues.values()) {
            final String insertValue = value.toString();
            if (value instanceof Date) {
                sb.append(" STR_TO_DATE( '" + DATE_FORMAT.format((Date) value) + "', '%Y-%m-%d %H:%i:%S' ),");
            } else {
                sb.append(" '" + insertValue + "',");
            }
        }
        sb.setLength(sb.length() - 1);
        sb.append(")");
        return sb.toString();
    }

    /**
     * Build a SQL insert statement.
     * @param tableName the table name
     * @param collectionID the collection ID
     * @param columnNames the column names
     * @param rows the row data
     * @return the SQL insert statement
     */
    public static String buildInsert(final String tableName, final int collectionID,
            final List<String> columnNames, final List<List<String>> rows) {
        final StringBuffer buff = new StringBuffer();
        buff.append("INSERT INTO " + tableName + " ( collection_id");
        for (String column : columnNames) {
            buff.append(", " + column);
        }
        buff.append(" ) VALUES ");
        for (List<String> values : rows) {
            buff.append("( ");
            buff.append(collectionID);
            for (String value : values) {
                buff.append(", '" + value + "'");
            }
            buff.append("), ");
        }
        buff.setLength(buff.length() - 2);
        return buff.toString();
    }

    /*
    static String buildDelete(String tableName, int rowId) {
        if (rowId <= 0)
            throw new IllegalArgumentException("Invalid row ID: " + rowId);
        String query = "DELETE FROM " + tableName + " WHERE id = " + rowId;
        return query;
    }
    */

    /**
     * Format the date for insert statement.
     * @param date the input date
     * @return the formatted date string
     */
    static String formatDate(final Date date) {
        return DATE_FORMAT.format(date);
    }
}
