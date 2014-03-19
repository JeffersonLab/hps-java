package org.hps.conditions.svt;

import org.hps.conditions.ConditionsObjectCollection;
import org.hps.conditions.ConditionsTableMetaData;
import org.lcsim.hps.util.Pair;

/**
 * This class establishes the mapping between layer numbers and DAQ pair (FPGA, hybrid)
 * for the top and bottom halves of the detector. 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtDaqMappingCollection extends ConditionsObjectCollection<SvtDaqMapping> {
    
    /**
     * Flag values for top or bottom half.
     */
    public static final int TOP_HALF = 0;
    public static final int BOTTOM_HALF = 1;
    
    public SvtDaqMappingCollection(ConditionsTableMetaData tableMetaData, int collectionId, boolean isReadOnly) {
        super(tableMetaData, collectionId, isReadOnly);
    }
      
    /**
     * Get a DAQ pair (FPGA, hybrid) by top/bottom number and layer number.
     * @param half Value indicating top or bottom half of detector.
     * @param layerNumber The layer number.
     * @return The DAQ pair for the half and layer number or null if does not exist.
     */
    Pair<Integer,Integer> get(int half, int layerNumber) {
        for (SvtDaqMapping object : this.getObjects()) {
            if (object.getHalf() == half && object.getLayerNumber() == layerNumber) {
                return new Pair<Integer, Integer>(object.getFpgaNumber(), object.getHybridNumber());
            }
        }
        return null;
    }
    
    /**
     * Convert this object to a string.
     * @return This object converted to a string.
     */
    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append("half");
        buff.append(" ");
        buff.append("layer");
        buff.append(" ");
        buff.append("fpga");
        buff.append(" ");
        buff.append("hybrid");
        buff.append('\n');
        buff.append("----------------------");
        buff.append('\n');
        for (SvtDaqMapping object : getObjects()) {
            buff.append(object.getHalf());
            buff.append("    ");
            buff.append(String.format("%-2d", object.getLayerNumber()));
            buff.append("    ");                
            buff.append(object.getFpgaNumber());
            buff.append("    ");
            buff.append(object.getHybridNumber());
            buff.append('\n');
        }        
        return buff.toString();
    }    
}