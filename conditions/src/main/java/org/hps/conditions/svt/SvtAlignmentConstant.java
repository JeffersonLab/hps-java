package org.hps.conditions.svt;

import org.hps.conditions.api.BaseConditionsObject;
import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.database.Converter;
import org.hps.conditions.database.Field;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.hps.conditions.database.Table;

/**
 * Encapsulates an SVT alignment constant, which is an encoded, string key with a double value 
 * representing the translation or rotation of a detector component.
 * <p>
 * The format of the keys is ABCDE where:<br>
 * <pre>
 * A == half == [1,2]
 * B == alignment type == [1,2]
 * C == unit axis == [1,2,3]
 * DE == module number == [01-10]
 * </pre>
 * <p>
 * The key naming conventions are from the  
 * <a href="http://www.desy.de/~blobel/mptalks.html">Millipede</a> package.
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
    }

    /**
     * Top or bottom half.
     */
    public enum Half {
        /** Top half. */
        TOP(1),
        /** Bottom half. */
        BOTTOM(2);

        /**
         * The integer value designating top or bottom half.
         */
        private int value;

        /**
         * Create from top or bottom value.
         *
         * @param value the value for half
         */
        private Half(final int value) {
            this.value = value;
        }

        /**
         * Get the value.
         *
         * @return the value
         */
        int getValue() {
            return value;
        }
    };

    /**
     * The alignment constant type which is rotation or translation.
     */
    public enum AlignmentType {
        /** Rotation alignment type. */
        ROTATION(1),
        /** Translation alignment type. */
        TRANSLATION(2);

        /**
         * The value of the alignment type constants.
         */
        private int value;

        /**
         * Constructor that has value of constant.
         * @param value the value of the constant
         */
        private AlignmentType(final int value) {
            this.value = value;
        }

        /**
         * Get the value of the constant.
         * @return the value of the constant
         */
        int getValue() {
            return value;
        }
    };

    /**
     * The unit axis which for translations maps to XYZ. (Convention for rotation???)
     */
    public enum UnitAxis {
        /** U unit axis. */
        U(1),
        /** V unit axis. */
        V(2),
        /** W unit axis. */
        W(3);

        /**
         * Value for the constant.
         */
        private int value;

        /**
         * Create from value.
         *
         * @param value the value
         */
        private UnitAxis(final int value) {
            this.value = value;
        }

        /**
         * Get the value
         *
         * @return the value
         */
        int getValue() {
            return value;
        }
    };

    /**
     * Maximum value of the module number.
     */
    private static final int MAX_MODULE_NUMBER = 10;

    /**
     * Get the alignment constant's encoded, raw value.
     *
     * @return the alignment constant's key
     */
    @Field(names = { "parameter" })
    public String getParameter() {
        return getFieldValue("parameter");
    }

    /**
     * Get the the alignment constant's value, which is always a single double.
     *
     * @return the alignment constant's value as a double
     */
    @Field(names = { "value" })
    public double getValue() {
        return getFieldValue("value");
    }

    /**
     * Decode the Half value from the key.
     * 
     * @return the Half value from the key
     * @see {@link SvtAlignmentConstant#Half}
     */
    public Half getHalf() {
        final int half = Integer.parseInt(getParameter().substring(0, 1));
        if (half == Half.TOP.getValue()) {
            return Half.TOP;
        } else if (half == Half.BOTTOM.getValue()) {
            return Half.BOTTOM;
        } else {
            throw new IllegalArgumentException("Could not parse valid Half from " + getParameter());
        }
    }

    /**
     * Decode the AlignmentType value from the key.
     *
     * @return the AlignmentType value from the key
     * @see {@link SvtAlignmentConstant#AlignmentType}
     */
    public AlignmentType getAlignmentType() {
        final int alignmentType = Integer.parseInt(getParameter().substring(1, 2));
        if (alignmentType == AlignmentType.TRANSLATION.getValue()) {
            return AlignmentType.TRANSLATION;
        } else if (alignmentType == AlignmentType.ROTATION.getValue()) {
            return AlignmentType.ROTATION;
        } else {
            throw new IllegalArgumentException("Could not parse valid AlignmentType from " + getParameter());
        }
    }

    /**
     * Decode the UnitAxis from the key.
     *
     * @return the UnitAxis v
     * @see {@link SvtAlignmentConstant#UnitAxis}  
     */
    public UnitAxis getUnitAxis() {
        final int unitAxis = Integer.parseInt(getParameter().substring(2, 3));
        if (unitAxis == UnitAxis.U.getValue()) {
            return UnitAxis.U;
        } else if (unitAxis == UnitAxis.V.getValue()) {
            return UnitAxis.V;
        } else if (unitAxis == UnitAxis.W.getValue()) {
            return UnitAxis.W;
        } else {
            throw new IllegalArgumentException("Could not parse valid UnitAxis from " + getParameter());
        }
    }

    /**
     * Decode the module number from the key.
     *
     * @return the module number from the key
     */
    public int getModuleNumber() {
        final int moduleNumber = Integer.parseInt(getParameter().substring(3, 5));
        if (moduleNumber > MAX_MODULE_NUMBER || moduleNumber == 0) {
            throw new IllegalArgumentException("The decoded module number " + moduleNumber + " is invalid.");
        }
        return moduleNumber;
    }

    /**
     * Convert this object to a string.
     *
     * @return this object converted to a string
     */
    @Override
    public String toString() {
        final StringBuffer buff = new StringBuffer();
        buff.append(super.toString());
        buff.append("half: ").append(getHalf().getValue()).append('\n');
        buff.append("alignment_type: ").append(getAlignmentType().getValue()).append('\n');
        buff.append("unit_axis: ").append(getUnitAxis().getValue()).append('\n');
        buff.append("module_number: ").append(getModuleNumber()).append('\n');
        return buff.toString();
    }
}
