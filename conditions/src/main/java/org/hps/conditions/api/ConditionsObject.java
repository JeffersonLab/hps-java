package org.hps.conditions.api;

public interface ConditionsObject extends DatabaseObject {

    /**
     * @return
     */
    Integer getCollectionId();

    boolean hasValidCollection();

    FieldValues getFieldValues();

    /**
     * @return
     */
    int getRowId();

    /**
     * @param type
     * @param name
     * @return
     */
    <T> T getFieldValue(final Class<T> type, final String name);

    <T> T getFieldValue(final String name);

    void setFieldValues(FieldValues fieldValues);

    /**
     * @param name
     * @param value
     */
    void setFieldValue(String name, Object value);
            
    // void setCollectionId(Integer id);
    
    // void setRowId(Integer id);
}
