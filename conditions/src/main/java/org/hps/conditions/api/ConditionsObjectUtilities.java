package org.hps.conditions.api;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.hps.conditions.database.DatabaseConditionsManager;

/**
 * This is a collection of utility methods for {@link ConditionsObject}.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class ConditionsObjectUtilities {
    
    /**
     * Static instance of conditions manager.
     */
    private static final DatabaseConditionsManager MANAGER = DatabaseConditionsManager.getInstance();

    /**
     * Default input date format from text data.
     */
    private static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
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
                return DEFAULT_DATE_FORMAT.parse(value);
            } catch (ParseException e) {
                throw new ConditionsObjectException("Error parsing date.", e);
            }
        } else {
            return value;
        }
    }
                
    /**
     * Create a new conditions collection from the table name.
     * 
     * @param tableName the name of the table
     * @return the new conditions collection
     * @throws ConditionsObjectException if there is an error creating the collection
     */
    public static ConditionsObjectCollection<?> newCollection(String tableName) throws ConditionsObjectException {
        TableMetaData tableInfo = TableRegistry.getTableRegistry().findByTableName(tableName);
        ConditionsObjectCollection<?> collection = tableInfo.newCollection();
        collection.setConnection(MANAGER.getConnection());
        return collection; 
    }
    
    /**
     * Create a new conditions object from the table name.
     * 
     * @param tableName the name of the table
     * @return the new conditions object
     * @throws ConditionsObjectException if there is an error creating the object
     */
    public static ConditionsObject newObject(String tableName) throws ConditionsObjectException {
        TableMetaData tableInfo = TableRegistry.getTableRegistry().findByTableName(tableName);
        ConditionsObject object = tableInfo.newObject();
        object.setConnection(MANAGER.getConnection());
        return object;
    }
    
    /**
     * Do not allow class to be instantiated.
     */
    private ConditionsObjectUtilities() {
    }
}
