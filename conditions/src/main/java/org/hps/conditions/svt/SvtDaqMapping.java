package org.hps.conditions.svt;

import org.hps.conditions.AbstractConditionsObject;
import org.hps.conditions.ConditionsObjectCollection;
import org.hps.util.Pair;

public final class SvtDaqMapping extends AbstractConditionsObject {

    public static class SvtDaqMappingCollection extends ConditionsObjectCollection<SvtDaqMapping> {

        /**
         * Flag values for top or bottom half.
         */
        public static final String TOP_HALF = "T";
        public static final String BOTTOM_HALF = "B";

        /**
         * Get a DAQ pair (FEB ID, FEB Hybrid ID) by SVT volume and layer number.
         * @param svtHalf Value indicating top or bottom half of detector.
         * @param layerNumber The layer number.
         * @return The DAQ pair for the half and layer number or null if does not exist.
         */
        Pair<Integer, Integer> getDaqPair(String SvtHalf, int layerNumber) {
            for (SvtDaqMapping object : this.getObjects()) {
                if (object.getSvtHalf() == SvtHalf && object.getLayerNumber() == layerNumber) {
                    return new Pair<Integer, Integer>(object.getFebID(), object.getFebHybridID());
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
            buff.append("FEB ID: ");
            buff.append(" ");
            buff.append("FEB Hybrid ID: ");
            buff.append(" ");
            buff.append("Hybrid ID: ");
            buff.append(" ");
            buff.append("SVT half: ");
            buff.append(" ");
            buff.append("Layer");
            buff.append(" ");
            buff.append("Orientation: ");
            buff.append(" ");
            buff.append('\n');
            buff.append("----------------------");
            buff.append('\n');
            for (SvtDaqMapping object : getObjects()) {
            	buff.append(object.getFebID());
                buff.append("    ");
            	buff.append(object.getFebHybridID());
                buff.append("    ");
            	buff.append(object.getHybridID());
                buff.append("    ");
                buff.append(object.getSvtHalf());
                buff.append("    ");
                buff.append(String.format("%-2d", object.getLayerNumber()));
                buff.append("    ");
                buff.append(object.getOrientation());
                buff.append("    ");
                buff.append('\n');
            }
            return buff.toString();
        }
    }
    
    public int getFebID() { 
    	return getFieldValue("feb_id");
    }
    
    public int getFebHybridID() { 
    	return getFieldValue("feb_hybrid_id");
    }
    
    public int getHybridID() { 
    	return getFieldValue("hybrid_id");
    }
    
    public String getSvtHalf() {
        return getFieldValue("svt_half");
    }

    public int getLayerNumber() {
        return getFieldValue("layer");
    }

    public String getOrientation() { 
    	return getFieldValue("orientation");
    }
}
