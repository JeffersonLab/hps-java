package org.hps.conditions.database;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.hps.conditions.api.ConditionsObject;
import org.hps.conditions.api.FieldValueMap;

/**
 * This is a static utility class for building SQL queries for the conditions system.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
// TODO: Most methods should be converted to use prepared statements, which is more flexible.
public final class QueryBuilder {

    private QueryBuilder() {
    }

    public static String buildSelect(String tableName, int collectionId, String[] fields, String order) {
        StringBuffer buff = new StringBuffer();
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
        if (collectionId != -1)
            buff.append(" WHERE collection_id = " + collectionId);
        if (order != null) {
            buff.append(" ORDER BY " + order);
        }
        return buff.toString();
    }

    public static String buildUpdate(String tableName, int rowId, String[] fields, Object[] values) {
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
    
    public static String buildPreparedInsert(String tableName, ConditionsObject object) {
        if (object.getFieldValues().size() == 0) {
            throw new IllegalArgumentException("The ConditionsObject has no values set.");
        }
        StringBuffer buffer = new StringBuffer();
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

    static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
    
    public static String buildInsert(String tableName, FieldValueMap fieldValues) {
        if (fieldValues.size() == 0) {
            throw new IllegalArgumentException("The FieldValueMap has no values.");
        }
        StringBuffer buff = new StringBuffer();
        buff.append("INSERT INTO " + tableName + " (");
        for (String fieldName : fieldValues.keySet()) {
            buff.append(" " + fieldName + ",");
        }
        buff.setLength(buff.length() - 1);
        buff.append(" ) VALUES (");
        for (Object value : fieldValues.values()) {
            String insertValue = value.toString();
            if (value instanceof Date) {
                buff.append(" STR_TO_DATE( '" + dateFormat.format((Date) value) + "', '%Y-%m-%d %H:%i:%S' ),");
            } else {
                buff.append(" '" + insertValue + "',");
            }
        }
        buff.setLength(buff.length() - 1);
        buff.append(")");
        return buff.toString();
    }

    public static String buildInsert(String tableName, int collectionID, List<String> columnNames, List<List<String>> rows) {
        StringBuffer buff = new StringBuffer();
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

    public static String buildDelete(String tableName, int rowId) {
        if (rowId <= 0)
            throw new IllegalArgumentException("Invalid row ID: " + rowId);
        String query = "DELETE FROM " + tableName + " WHERE id = " + rowId;
        return query;
    }
    
    public static String formatDate(Date date) {
        return dateFormat.format(date);
    }
}