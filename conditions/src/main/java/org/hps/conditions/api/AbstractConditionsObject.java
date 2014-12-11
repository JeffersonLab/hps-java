package org.hps.conditions.api;

import java.util.Map.Entry;

/**
 * The abstract implementation of {@link ConditionsObject}.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public abstract class AbstractConditionsObject implements ConditionsObject {

    protected int rowId = -1;
    protected FieldValueMap fieldValues;

    /** 
     * Constructor for sub-classing.
     */
    protected AbstractConditionsObject() {
        fieldValues = new FieldValueMap();
    }

    public int getRowId() {
        return rowId;
    }

    public boolean isNew() {
        return rowId == -1;
    }

   
    public void setFieldValue(String key, Object value) {
        fieldValues.put(key, value);
    }

    public void setFieldValues(FieldValueMap fieldValues) {
        this.fieldValues = fieldValues;
    }

    public <T> T getFieldValue(Class<T> klass, String field) {
        return klass.cast(fieldValues.get(field));
    }
    
    public FieldValueMap getFieldValues() {
        return this.fieldValues;
    }

    @SuppressWarnings("unchecked")
    public <T> T getFieldValue(String field) {
        return (T) fieldValues.get(field);
    }

    public void setRowId(int rowId) throws ConditionsObjectException {
        if (!isNew()) {
            throw new ConditionsObjectException("The row ID cannot be reassigned on an existing object.");
        }
        this.rowId = rowId;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getRowId());
        sb.append('\t');
        for (Entry<String, Object> entries : this.getFieldValues().entrySet()) {
            sb.append(entries.getValue());
            sb.append('\t');
        }
        return sb.toString();
    }
}