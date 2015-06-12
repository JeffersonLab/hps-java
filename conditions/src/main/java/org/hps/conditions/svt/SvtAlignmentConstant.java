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
 * <pre>
 * A == half == [1,2]
 * B == alignment type == [1,2]
 * C == unit axis == [1,2,3]
 * DE == module number == [01-10]
 * </pre>
 * <p>
 * The key naming conventions are from the <a href="http://www.desy.de/~blobel/mptalks.html">Millipede</a> package.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@Table(names = "svt_alignments")
@Converter(multipleCollectionsAction = MultipleCollectionsAction.LAST_UPDATED)
public final class SvtAlignmentConstant extends BaseConditionsObject {

    /**
     * Collection implementation for {@link SvtAlignmentConstant}.
     */
    @SuppressWarnings("serial")
    public static class SvtAlignmentConstantCollection extends BaseConditionsObjectCollection<SvtAlignmentConstant> {
        
        public SvtAlignmentConstant find(int id) {
            for (SvtAlignmentConstant constant : this) {
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
        //System.out.println("parameter = " + this.getFieldValues().get("parameter") + "; type = " + this.getFieldValues().get("parameter").getClass());
        return getFieldValue("parameter");
    }

    /**
     * Get the the alignment constant's value, which is always a single double.
     *
     * @return the alignment constant's value as a double
     */
    @Field(names = {"value"})
    public Double getValue() {
        return getFieldValue("value");
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
