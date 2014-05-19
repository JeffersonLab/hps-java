package org.hps.conditions;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.hps.conditions.ConditionsObject.FieldValueMap;

/**
 * Some static utility methods for <tt>ConditionsObject</tt> and related classes.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
final class ConditionsObjectUtil {
    
    private ConditionsObjectUtil() {
    }

    static final ConditionsObject createConditionsObject(ResultSet resultSet, TableMetaData tableMetaData) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int rowId = resultSet.getInt(1);
        int ncols = metaData.getColumnCount();
        FieldValueMap fieldValues = new FieldValueMap();
        for (int i = 2; i <= ncols; i++) {
            fieldValues.put(metaData.getColumnName(i), resultSet.getObject(i));
        }
        ConditionsObject newObject = null;
        try {
            newObject = tableMetaData.getObjectClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        try {
            newObject.setRowId(rowId);
        } catch (ConditionsObjectException e) {
            throw new RuntimeException(e);
        }
        try {
            newObject.setTableMetaData(tableMetaData);
        } catch (ConditionsObjectException e) {
            throw new RuntimeException(e);
        }
        newObject.setFieldValues(fieldValues);
        return newObject;
    }    
    
    static final ConditionsObjectCollection createCollection(TableMetaData tableMetaData) {
        ConditionsObjectCollection collection;
        try {
            collection = tableMetaData.getCollectionClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return collection;
    }
    
}