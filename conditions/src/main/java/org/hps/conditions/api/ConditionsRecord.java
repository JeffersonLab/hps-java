package org.hps.conditions.api;

import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.hps.conditions.database.ConditionsRecordConverter;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;

/**
 * This class represents a single record from the primary conditions data table, which defines the validity range for a
 * specific collection of conditions objects.
 *
 * @author Jeremy McCormick, SLAC
 */
@Table(names = {"conditions"})
@Converter(converter = ConditionsRecordConverter.class)
public final class ConditionsRecord extends BaseConditionsObject {

    /**
     * The concrete collection implementation, including sorting utilities.
     */
    public static class ConditionsRecordCollection extends BaseConditionsObjectCollection<ConditionsRecord> {

        /**
         * Compare conditions records by creation date.
         */
        private static class CreatedComparator implements Comparator<ConditionsRecord> {

            /**
             * Compare the creation dates of two conditions records.
             *
             * @param c1 The first conditions record.
             * @param c2 The second conditions record.
             * @return -1, 0, or 1 if first date is less than, equal to, or greater than the second date.
             */
            @Override
            public int compare(final ConditionsRecord c1, final ConditionsRecord c2) {
                final Date date1 = c1.getCreated();
                final Date date2 = c2.getCreated();
                if (date1.before(date2)) {
                    return -1;
                } else if (date1.after(date2)) {
                    return 1;
                }
                return 0;
            }
        }

        /**
         * Compare conditions records by their key (table name).
         */
        private static class KeyComparator implements Comparator<ConditionsRecord> {

            /**
             * Compare the keys (names) of two conditions records.
             *
             * @param c1 The first conditions record.
             * @param c2 The second conditions record.
             * @return -1, 0, or 1 if first name is less than, equal to, or greater than the second (using alphabetic
             *         comparison).
             */
            @Override
            public int compare(final ConditionsRecord c1, final ConditionsRecord c2) {
                return c1.getName().compareTo(c2.getName());
            }
        }

        /**
         * Compare conditions records by run start.
         */
        private static class RunStartComparator implements Comparator<ConditionsRecord> {

            /**
             * Compare the run start numbers of two conditions records.
             *
             * @param c1 The first conditions record.
             * @param c2 The second conditions record.
             * @return -1, 0, or 1 if first run number is less than, equal to, or greater than the second.
             */
            @Override
            public int compare(final ConditionsRecord c1, final ConditionsRecord c2) {
                if (c1.getRunStart() < c2.getRunStart()) {
                    return -1;
                } else if (c1.getRunStart() > c2.getRunStart()) {
                    return 1;
                }
                return 0;
            }
        }

        /**
         * Compare conditions records by updated date.
         */
        private static class UpdatedComparator implements Comparator<ConditionsRecord> {

            /**
             * Compare the updated dates of two conditions records.
             *
             * @param c1 The first conditions record.
             * @param c2 The second conditions record.
             * @return -1, 0, or 1 if first date is less than, equal to, or greater than the second date.
             */
            @Override
            public int compare(final ConditionsRecord c1, final ConditionsRecord c2) {
                final Date date1 = c1.getUpdated();
                final Date date2 = c2.getUpdated();
                if (date1.before(date2)) {
                    return -1;
                } else if (date1.after(date2)) {
                    return 1;
                }
                return 0;
            }
        }

        /**
         * Add a <code>ConditionsRecord</code> to the collection.
         *
         * @param object the <code>ConditionsRecord</code> to add to the collection
         * @return <code>true</code> if object was added
         */
        @Override
        // FIXME: Not sure this method needs to exist.
        public boolean add(final ConditionsRecord object) throws ConditionsObjectException {
            if (object == null) {
                throw new IllegalArgumentException("The object argument is null.");
            }
            final boolean added = this.getObjects().add(object);
            if (!added) {
                throw new RuntimeException("Failed to add object.");
            }
            return added;
        }

        /**
         * Find a sub-set of the records matching the key name.
         * <p>
         * These will generally map to names of tables.
         *
         * @param key the key to find
         */
        public ConditionsRecordCollection findByKey(final String key) {
            final ConditionsRecordCollection collection = new ConditionsRecordCollection();
            for (final ConditionsRecord record : this) {
                if (record.getName().equals(key)) {
                    try {
                        collection.add(record);
                    } catch (final ConditionsObjectException e) {
                        throw new RuntimeException("Error adding record to new collection.", e);
                    }
                }
            }
            return collection;
        }

        /**
         * Get the unique conditions keys from the records in this collection.
         *
         * @return the set of unique conditions keys
         */
        public final Set<String> getConditionsKeys() {
            final Set<String> conditionsKeys = new HashSet<String>();
            for (final ConditionsRecord record : this) {
                conditionsKeys.add(record.getName());
            }
            return conditionsKeys;
        }

        /**
         * Sort the collection in place by creation date.
         */
        public final void sortByCreated() {
            this.sort(new CreatedComparator());
        }

        /**
         * Sort the collection in place by key.
         */
        public final void sortByKey() {
            this.sort(new KeyComparator());
        }

        /**
         * Sort the collection in place by run start.
         */
        public final void sortByRunStart() {
            this.sort(new RunStartComparator());
        }

        /**
         * Sort the collection in place by updated date.
         */
        public final void sortByUpdated() {
            this.sort(new UpdatedComparator());
        }

        /**
         * Sort and return the collection by creation date.
         *
         * @return the sorted collection
         */
        public final ConditionsRecordCollection sortedByCreated() {
            return (ConditionsRecordCollection) this.sorted(new CreatedComparator());
        }

        /**
         * Sort and return the collection by key (table name).
         *
         * @return the sorted collection
         */
        public final ConditionsRecordCollection sortedByKey() {
            return (ConditionsRecordCollection) this.sorted(new KeyComparator());
        }

        /**
         * Sort and return the collection by run start number.
         *
         * @return the sorted collection
         */
        public final ConditionsRecordCollection sortedByRunStart() {
            return (ConditionsRecordCollection) this.sorted(new RunStartComparator());
        }

        /**
         * Sort and return the collection by updated date.
         *
         * @return the sorted collection
         */
        public final ConditionsRecordCollection sortedByUpdated() {
            return (ConditionsRecordCollection) this.sorted(new UpdatedComparator());
        }
    }

    /**
     * Class constructor.
     * <p>
     * Creates an empty conditions record.
     */
    public ConditionsRecord() {
    }

    /**
     * Copy constructor.
     *
     * @param record the <code>ConditionsRecord</code> to copy
     */
    public ConditionsRecord(final ConditionsRecord record) {
        this.setFieldValue("collection_id", record.getCollectionId());
        this.setFieldValue("run_start", record.getRunStart());
        this.setFieldValue("run_end", record.getRunEnd());
        this.setFieldValue("name", record.getName());
        this.setFieldValue("table_name", record.getTableName());
        this.setFieldValue("notes", record.getNotes());
        this.setFieldValue("tag", record.getTag());
        this.setFieldValue("created", record.getCreated());
        this.setFieldValue("created_by", record.getCreatedBy());
    }

    /**
     * Fully qualified class constructor.
     *
     * @param collectionId the ID of the associated conditions collection
     * @param runStart the starting run number
     * @param runEnd the ending run number
     * @param name the name of the conditions set (usually the same as table name but can be different)
     * @param tableName the name of the conditions data table
     * @param notes text notes about this record
     * @param tag the conditions tag for grouping this record with others
     */
    public ConditionsRecord(final int collectionId, final int runStart, final int runEnd, final String name,
            final String tableName, final String notes, final String tag) {
        this.setFieldValue("collection_id", collectionId);
        this.setFieldValue("run_start", runStart);
        this.setFieldValue("run_end", runEnd);
        this.setFieldValue("name", name);
        this.setFieldValue("table_name", tableName);
        this.setFieldValue("notes", notes);
        this.setFieldValue("tag", tag);
        this.setFieldValue("created", new Date());
        this.setFieldValue("created_by", System.getProperty("user.name"));
    }

    /**
     * Get the date this record was created.
     *
     * @return the date this record was created
     */
    @Field(names = {"created"})
    public Date getCreated() {
        return this.getFieldValue("created");
    }

    /**
     * Get the name of the user who created this record.
     *
     * @return the name of the person who created the record
     */
    @Field(names = {"created_by"})
    public String getCreatedBy() {
        return this.getFieldValue("created_by");
    }

    /**
     * Get the name of these conditions. This is called the "key" in the table meta data to distinguish it from
     * "table name" but it is usually the same value.
     *
     * @return the name of the conditions
     */
    @Field(names = {"name"})
    public String getName() {
        return this.getFieldValue("name");
    }

    /**
     * Get the notes.
     *
     * @return the notes about this condition
     */
    @Field(names = {"notes"})
    public String getNotes() {
        return this.getFieldValue("notes");
    }

    /**
     * Get the ending run number.
     *
     * @return the ending run number
     */
    @Field(names = {"run_end"})
    public Integer getRunEnd() {
        return this.getFieldValue("run_end");
    }

    /**
     * Get the starting run number.
     *
     * @return the starting run number
     */
    @Field(names = {"run_start"})
    public Integer getRunStart() {
        return this.getFieldValue("run_start");
    }

    /**
     * Get the name of the table containing the actual raw conditions data.
     *
     * @return the name of the table with the conditions data
     */
    @Field(names = {"table_name"})
    public String getTableName() {
        return this.getFieldValue("table_name");
    }

    /**
     * Get the string tag associated with these conditions.
     *
     * @return The string tag.
     */
    @Field(names = {"tag"})
    public String getTag() {
        return this.getFieldValue("tag");
    }

    /**
     * Get the date this record was last updated.
     *
     * @return the date this record was updated
     */
    @Field(names = {"updated"})
    public Date getUpdated() {
        return this.getFieldValue("updated");
    }

    /**
     * Convert this record to a human readable string, one field per line.
     *
     * @return this object represented as a string
     */
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("id: " + this.getRowId() + '\n');
        sb.append("name: " + this.getName() + '\n');
        sb.append("runStart: " + this.getRunStart() + '\n');
        sb.append("runEnd: " + this.getRunEnd() + '\n');
        sb.append("tableName: " + this.getTableName() + '\n');
        sb.append("collectionId: " + this.getCollectionId() + '\n');
        sb.append("updated: " + this.getUpdated() + '\n');
        sb.append("created: " + this.getCreated() + '\n');
        sb.append("tag: " + this.getTag() + '\n');
        sb.append("createdBy: " + this.getCreatedBy() + '\n');
        sb.append("notes: " + this.getNotes() + '\n');
        return sb.toString();
    }
}
