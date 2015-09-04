package org.hps.conditions.api;

import org.hps.conditions.api.ConditionsRecord.ConditionsRecordCollection;
import org.hps.conditions.database.ConditionsTagConverter;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;

/**
 * Conditions tag specifying a set of grouped {@link ConditionsRecord} objects by their row IDs in the database along 
 * with a tag name.
 * <p>
 * The run numbers of conditions in the tag with the same key are allowed to overlap.  In this case, the 
 * disambiguation is performed at run-time within the job using a {@link org.hps.conditions.database.MultipleCollectionsAction}.
 *  
 * @author Jeremy McCormick, SLAC
 */
@Table(names = {"conditions_tags"})
@Converter(converter = ConditionsTagConverter.class)
public final class ConditionsTag extends BaseConditionsObject {
    
    static {
        /* 
         * HACK: Remove collection_id from the list of fields; it is defined by the BaseConditionsObject super-class
         * but not defined in this class's table schema.
         */
        TableRegistry.getTableRegistry().findByObjectType(ConditionsTag.class).get(0).removeField("collection_id");
    }

    /**
     * Collection of tag records.
     */
    public static final class ConditionsTagCollection extends BaseConditionsObjectCollection<ConditionsTag> {

        /**
         * Return <code>true</code> if the collection contains this conditions record (it is in the tag).
         * 
         * @param conditionsRecord the conditions record
         * @return <code>true</code> if the collection contains this conditions record
         */
        public boolean contains(final ConditionsRecord conditionsRecord) {
            for (final ConditionsTag tagRecord : this.getObjects()) {
                if (tagRecord.getConditionsId().equals(conditionsRecord.getRowId())) {
                    return true;
                }
            }
            return false;
        }
        
        /**
         * Return a new filtered collection containing only those records that are referenced by this tag.
         * 
         * @param conditionsRecordCollection the conditions records to filter
         * @return the filtered collection
         */
        public ConditionsRecordCollection filter(final ConditionsRecordCollection conditionsRecordCollection) {
            final ConditionsRecordCollection tagConditionsRecordCollection = new ConditionsRecordCollection();
            for (final ConditionsRecord record : conditionsRecordCollection) {
                if (this.contains(record)) {
                    try {
                        tagConditionsRecordCollection.add(record);
                    } catch (final ConditionsObjectException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            return tagConditionsRecordCollection;
        }
    }

    /**
     * Create a conditions tag record.
     * 
     * @param conditionsId the referenced conditions record
     * @param tag the tag name
     */
    public ConditionsTag(final int conditionsId, final String tag) {
        this.setFieldValue("conditions_id", conditionsId);
        this.setFieldValue("tag", tag);
    }

    /**
     * Get the ID of the referenced conditions record.
     * 
     * @return the ID of the referenced conditions record
     */
    @Field(names = {"conditions_id"})
    public Integer getConditionsId() {
        return this.getFieldValue("conditions_id");
    }

    /**
     * Get the tag name.
     * 
     * @return the tag name
     */
    @Field(names = {"tag"})
    public String getTag() {
        return this.getFieldValue("tag");
    }
}
