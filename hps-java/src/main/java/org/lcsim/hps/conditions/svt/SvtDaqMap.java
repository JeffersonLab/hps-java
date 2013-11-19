package org.lcsim.hps.conditions.svt;

import java.util.HashMap;
import java.util.Map;

import org.lcsim.hps.util.Pair;

/**
 * This class establishes the mapping between layer numbers and DAQ pair (FPGA, hybrid)
 * for the top and bottom halves of the detector. 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtDaqMap {

    /**
     * Flag values for top or bottom half.
     * FIXME: This should probably be an enum but it is simpler to use int values for now.  
     */
    static final int TOP = 0;
    static final int BOTTOM = 1;    
      
    /**
     * This is the data structure used for the mapping of layer numbers to DAQ pair by top or bottom half.
     * The mapping is the following: half => layer => pair(fpga, hybrid)
     */
    @SuppressWarnings("serial")
    static private class LayerMap extends HashMap<Integer,HashMap<Integer,Pair<Integer,Integer>>> {        
    }
     
    /**
     * Object that holds the DAQ map data.
     */
    LayerMap layerMap = new LayerMap();
    
    /**
     * Class constructor.
     */
    SvtDaqMap() {
        layerMap.put(TOP, new HashMap<Integer,Pair<Integer,Integer>>());
        layerMap.put(BOTTOM, new HashMap<Integer,Pair<Integer,Integer>>());
    }
     
    /**
     * Add a record to the DAQ map.
     * @param half The value indicating top or bottom half of the detector.
     * @param layerNumber The layer number.
     */
    void add(int half, int layerNumber, Pair<Integer,Integer> pair) {
        layerMap.get(half).put(layerNumber, pair);
    }
    
    /**
     * Get a DAQ pair (FPGA, hybrid) by layer number.
     * @param half Value indicating top or bottom half of detector.
     * @param layerNumber The layer number.
     * @return The DAQ pair for the half and layer number or null if does not exist.
     */
    Pair<Integer,Integer> get(int half, int layerNumber) {
        return layerMap.get(half).get(layerNumber);
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
        for (int half : layerMap.keySet()) {
            Map<Integer,Pair<Integer,Integer>> map = layerMap.get(half);
            for (Map.Entry<Integer, Pair<Integer,Integer>> entry : map.entrySet()) {
                buff.append(half);
                buff.append("    ");
                buff.append(String.format("%-2d", entry.getKey()));
                buff.append("    ");                
                buff.append(entry.getValue().getFirstElement());
                buff.append("    ");
                buff.append(entry.getValue().getSecondElement());
                buff.append('\n');
            }
        }        
        return buff.toString();
    }    
}
