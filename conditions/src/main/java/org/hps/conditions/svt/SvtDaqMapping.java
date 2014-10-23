package org.hps.conditions.svt;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;

import org.hps.conditions.AbstractConditionsObject;
import org.hps.conditions.ConditionsObjectCollection;
import org.hps.util.Pair;

/**
 * This class encapsulates the SVT DAQ map.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public class SvtDaqMapping extends AbstractConditionsObject {

    public static class SvtDaqMappingCollection extends ConditionsObjectCollection<SvtDaqMapping> {

        /**
         * Flag values for top or bottom half.
         */
        public static final String TOP_HALF = "T";
        public static final String BOTTOM_HALF = "B";
        
        /**
         * Flag values for axial or stereo sensors 
         */
        public static final String AXIAL = "A";
        public static final String STEREO = "S";

        /**
         * Get a DAQ pair (FEB ID, FEB Hybrid ID) for the given {@link HpsSiSensor}
         * 
		 * @param  sensor A sensor of type {@link HpsSiSensor}
		 * @return The DAQ pair associated with the sensor 
         */
        Pair<Integer, Integer> getDaqPair(HpsSiSensor sensor) {
        	
        	String svtHalf = sensor.isTopLayer() ? TOP_HALF : BOTTOM_HALF;
        	for (SvtDaqMapping object : this.getObjects()) {
        		
        		if(svtHalf.equals(object.getSvtHalf()) 
        				&& object.getLayerNumber() == sensor.getLayerNumber()
        				&& object.getSide().equals(sensor.getSide())) {
                
        			return new Pair<Integer, Integer>(object.getFebID(), object.getFebHybridID());
                } 
            }
            return null;
        }
        
        /**
         * Get the orientation of a sensor using the FEB ID and FEB Hybrid ID.
         * If the FEB ID and FEB Hybrid ID combination is not found, return null.
         * 
         * @param daqPair (Pair<FEB ID, FEB Hybrid ID>) for a given sensor
         * @return If a daqPair is found, return an "A" if the sensor 
         * 		   orientation is Axial, an "S" if the orientation is Stereo or
         * 		   null if the daqPair doesn't exist.
         */
        public String getOrientation(Pair<Integer, Integer> daqPair){
        	
        	for(SvtDaqMapping daqMapping : this.getObjects()){
        		
        		if(daqPair.getFirstElement() == daqMapping.getFebID() && daqPair.getSecondElement() == daqMapping.getFebHybridID()){
        			return daqMapping.getOrientation(); 
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
                buff.append(object.getSvtHalf());
                buff.append("    ");
                buff.append(String.format("%-2d", object.getLayerNumber()));
                buff.append("    ");
                buff.append(object.getSide());
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
    
    public String getSvtHalf() {
        return getFieldValue("svt_half");
    }

    public int getLayerNumber() {
        return getFieldValue("layer");
    }
    
    public String getSide(){
    	return getFieldValue("side");
    }

    public String getOrientation() { 
    	return getFieldValue("orientation");
    }
}
