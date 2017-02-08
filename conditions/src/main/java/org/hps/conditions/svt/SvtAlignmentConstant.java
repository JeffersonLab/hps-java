package org.hps.conditions.svt;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;

/**
 * Encapsulates an SVT alignment constant, which is an encoded, string key with a double value representing the
 * translation or rotation of a detector component.
 * <p>
 * The format of the keys is ABCDE where:<br>
 * 
 * <pre>
 * A == half == [1,2]
 * B == alignment type == [1,2]
 * C == unit axis == [1,2,3]
 * DE == module number == [01-10]
 * </pre>
 * <p>
 * The key naming conventions are from the <a href="http://www.desy.de/~blobel/mptalks.html">Millipede</a> package.
 */
@Table(names = "svt_alignments")
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_UPDATED)
public final class SvtAlignmentConstant extends BaseConditionsObject {

    /**
     * Collection implementation for {@link SvtAlignmentConstant}.
     */
    @SuppressWarnings("serial")
    public static class SvtAlignmentConstantCollection extends BaseConditionsObjectCollection<SvtAlignmentConstant> {

        /**
         * Find an alignment constant by its parameter ID.
         * 
         * @param id the parameter ID
         * @return the first object with matching parameter ID or <code>null</code> if not found
         */
        public SvtAlignmentConstant find(final int id) {
            for (final SvtAlignmentConstant constant : this) {
                if (constant.getParameter().equals(id)) {
                    return constant;
                }
            }
            return null;
        }

    };

    /**
     * Get the alignment constant's encoded, raw value.
     *
     * @return the alignment constant's key
     */
    @Field(names = {"parameter"})
    public Integer getParameter() {
        return this.getFieldValue("parameter");
    }

    /**
     * Get the the alignment constant's value, which is always a single double.
     *
     * @return the alignment constant's value as a double
     */
    @Field(names = {"value"})
    public Double getValue() {
        return this.getFieldValue("value");
    }

    /**
     * Convert this object to a string.
     *
     * @return this object converted to a string
     */
    @Override
    public String toString() {
        return "SvtAlignmentConstant parameter = " + this.getParameter() + "; value = " + this.getValue();
    }
}
