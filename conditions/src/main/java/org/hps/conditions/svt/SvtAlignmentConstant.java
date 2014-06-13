package org.hps.conditions.svt;

import org.hps.conditions.AbstractConditionsObject;

/**
 * <p>
 * Encapsulates an SVT alignment constant, which is an encoded key plus a double value.
 * </p>
 * <p>
 * The format of the keys is ABCDE where:<br>
 * A == half == [1,2]<br>
 * B == alignment type == [1,2]<br>
 * C == unit axis == [1,2,3]<br>
 * DE == module number == [1,10]
 * </p>
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtAlignmentConstant extends AbstractConditionsObject {
    
    /** Top or bottom half. */
    public enum Half {                
        TOP(1), 
        BOTTOM(2);        
        int value;        
        Half(int value) {
            this.value = value;
        }        
        int getValue() {
            return value;
        }
    };    
    
    /** The alignment constant type which is rotation or translation. */
    public enum AlignmentType {        
        ROTATION(1),
        TRANSLATION(2);
        int value;        
        AlignmentType(int value) {
            this.value = value;
        }        
        int getValue() {
            return value;
        }
    };
        
    /** The unit axis. */
    public enum UnitAxis { 
        U(1), 
        V(2), 
        W(3);
        int value;
        UnitAxis(int value) {
            this.value = value;
        }
        int getValue() {
            return value;
        }        
    };
    
    /** Maximum value of module number. */
    private static final int MAX_MODULE_NUMBER = 10;
       
    /**
     * Get the alignment constant's full key with the encoded information.
     * @return the alignment constant's key
     */
    public String getKey() {
        return getFieldValue("key");
    }
    
    /**
     * Get the the alignment constant's value which is always a single double.
     * @return the alignment constant's value as a double
     */
    public double getValue() {
        return getFieldValue("value");
    }
    
    /**
     * Decode the Half value from the key.
     * @return the Half value from the key
     */
    public Half getHalf() {
        int half = Integer.parseInt(getKey().substring(0,0));
        if (half == Half.TOP.getValue()) {
            return Half.TOP;
        } else if (half == Half.TOP.getValue()) {
            return Half.BOTTOM;
        } else {
            throw new IllegalArgumentException("Could not parse valid Half from " + getKey());
        }                              
    }
    
    /**
     * Decode the AlignmentType value from the key.
     * @return the AlignmentType value from the key
     */
    public AlignmentType getAlignmentType() {
        int alignmentType = Integer.parseInt(getKey().substring(1, 1));
        if (alignmentType == AlignmentType.TRANSLATION.getValue()) {
            return AlignmentType.TRANSLATION;
        } else if (alignmentType == AlignmentType.ROTATION.getValue()) {
            return AlignmentType.ROTATION;
        } else {
            throw new IllegalArgumentException("Could not parse valid AlignmentType from " + getKey());
        }
    }
    
    /**
     * Decode the UnitAxis from the key.
     * @return the UnitAxis v
     */
    public UnitAxis getUnitAxis() {
        int unitAxis = Integer.parseInt(getKey().substring(3,3));
        if (unitAxis == UnitAxis.U.getValue()) {
            return UnitAxis.U;
        } else if (unitAxis == UnitAxis.V.getValue()) {
            return UnitAxis.V;
        } else if (unitAxis == UnitAxis.W.getValue()) {
            return UnitAxis.W;
        } else {
            throw new IllegalArgumentException("Could not parse valid UnitAxis from " + getKey());
        }
    }
    
    /**
     * Decode the module number from the key.
     * @return the module number from the key 
     */
    public int getModuleNumber() {
        int moduleNumber = Integer.parseInt(getKey().substring(3, 4));
        if (moduleNumber > MAX_MODULE_NUMBER || moduleNumber == 0) {
            throw new IllegalArgumentException("The decoded module number " + moduleNumber + " is invalid.");
        }
        return moduleNumber;
    }
}