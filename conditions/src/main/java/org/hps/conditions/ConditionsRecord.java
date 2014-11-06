package org.hps.conditions;

import java.util.Date;

/**
 * This class represents a single record from the primary conditions data table, which
 * defines the validity range for a specific collection of conditions objects.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
// TODO: Check behavior of select, delete, update and insert operations.
// TODO: Override default behavior of methods in super class that don't make sense.
public final class ConditionsRecord extends AbstractConditionsObject {

    /**
     * Collection type.
     */
    public static class ConditionsRecordCollection extends ConditionsObjectCollection<ConditionsRecord> {
        /**
         * Since ConditionsRecord collections are always "mixed", meaning the collection
         * ID values are usually all going to be different, the default behavior of the
         * super class is overridden.
         */
        public void add(ConditionsRecord record) {
            objects.add(record);
        }

        // FIXME: Should probably override more methods here that don't make sense for
        // this type!
    }

    protected ConditionsRecord() {
    }

    /**
     * Get the starting run number.
     * @return The starting run number.
     */
    public int getRunStart() {
        return getFieldValue("run_start");
    }

    /**
     * Get the ending run number.
     * @return The ending run number.
     */
    public int getRunEnd() {
        return getFieldValue("run_end");
    }

    /**
     * Get the date this record was last updated.
     * @return The date this record was updated.
     */
    public Date getUpdated() {
        return getFieldValue("updated");
    }

    /**
     * Get the date this record was created.
     * @return The date this record was created.
     */
    public Date getCreated() {
        return getFieldValue("created");
    }

    /**
     * Get the name of the user who created this record.
     * @return The name of the person who created the record.
     */
    public String getCreatedBy() {
        return getFieldValue("created_by");
    }

    /**
     * Get the notes.
     * @return The notes about this condition.
     */
    public String getNotes() {
        return getFieldValue("notes");
    }

    /**
     * Get the name of these conditions, which should be unique by run number. This is
     * called the "key" in the table meta data to distinguish it from "table name".
     * @return The name of the conditions.
     */
    public String getName() {
        return getFieldValue("name");
    }

    /**
     * Get the name of the table containing the actual raw conditions data.
     * @return The name of the table with the conditions data.
     */
    public String getTableName() {
        return getFieldValue("table_name");
    }

    /**
     * Get the collection ID, overriding this method from the parent class.
     * @return The collection ID.
     */
    public int getCollectionId() {
        return getFieldValue("collection_id");
    }
    
    /**
     * Get the string tag associated with these conditions.
     * @return The string tag.
     */
    public String getTag() {
        return getFieldValue("tag");
    }

    /**
     * Convert this record to a human readable string, one field per line.
     * @return This object represented as a string.
     */
    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append("id: " + getRowId() + '\n');
        buff.append("name: " + getName() + '\n');
        buff.append("runStart: " + getRunStart() + '\n');
        buff.append("runEnd: " + getRunEnd() + '\n');
        buff.append("tableName: " + getTableName() + '\n');
        buff.append("collectionId: " + getCollectionId() + '\n');
        buff.append("updated: " + getUpdated() + '\n');
        buff.append("created: " + getCreated() + '\n');
        buff.append("tag: " + getTag() + '\n');
        buff.append("createdBy: " + getCreatedBy() + '\n');
        buff.append("notes: " + getNotes() + '\n');
        return buff.toString();
    }
}