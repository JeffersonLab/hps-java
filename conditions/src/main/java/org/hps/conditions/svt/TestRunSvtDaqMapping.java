package org.hps.conditions.svt;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.HpsTestRunSiSensor;
import org.hps.conditions.AbstractConditionsObject;
import org.hps.conditions.ConditionsObjectCollection;
import org.hps.util.Pair;

/**
 * This class encapsulates the Test run SVT DAQ map.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public class TestRunSvtDaqMapping extends AbstractConditionsObject {

	public static class TestRunSvtDaqMappingCollection extends ConditionsObjectCollection<TestRunSvtDaqMapping> {
	
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
		 * Get a test run DAQ pair (FPGA and Hybrid ID) for the given 
		 * {@linkplain HpsTestRunSiSensor}
		 * 
		 * @param  sensor A sensor of type {@link HpsTestRunSiSensor}
		 * @return The DAQ pair associated with the sensor 
		 */
		Pair<Integer, Integer> getDaqPair(HpsSiSensor sensor){
			
			String svtHalf = sensor.isTopLayer() ? TOP_HALF : BOTTOM_HALF; 
			for(TestRunSvtDaqMapping daqMapping : this.getObjects()){
				
				if(svtHalf.equals(daqMapping.getSvtHalf())
						&& daqMapping.getLayerNumber() == sensor.getLayerNumber()){
					
					return new Pair<Integer, Integer>(daqMapping.getFpgaID(), 
													  daqMapping.getHybridID());
				}
			}
			return null;
		}
		
        /**
         * Get the orientation of a sensor using the FPGA and Hybrid ID.
         * If the FPGA and Hybrid ID combination is not found, return null.
         * 
         * @param daqPair (Pair<FPGA ID, Hybrid ID>) for a given sensor
         * @return If a daqPair is found, return an "A" if the sensor 
         * 		   orientation is Axial, an "S" if the orientation is Stereo or
         * 		   null if the daqPair doesn't exist.
         */
        public String getOrientation(Pair<Integer, Integer> daqPair){
        	
        	for(TestRunSvtDaqMapping daqMapping : this.getObjects()){
        		
        		if(daqPair.getFirstElement() == ((TestRunSvtDaqMapping) daqMapping).getFpgaID()
        				&& daqPair.getSecondElement() == ((TestRunSvtDaqMapping) daqMapping).getHybridID()){
        			return daqMapping.getOrientation(); 
        		}
        	}
        	return null;
        }
		
		
        /**
         * Convert {@link TestRunSvtDaqMapping} to a string.
         * @return This object converted to a string.
         */
        public String toString() {
        	StringBuffer buffer = new StringBuffer(); 
        	buffer.append("FPGA ID: ");
            buffer.append(" ");
            buffer.append("Hybrid ID: ");
            buffer.append(" ");
            buffer.append("SVT half: ");
            buffer.append(" ");
            buffer.append("Layer");
            buffer.append(" ");
            buffer.append("Orientation: ");
            buffer.append(" ");
            buffer.append('\n');
            buffer.append("----------------------");
            buffer.append('\n');
            for (TestRunSvtDaqMapping daqMapping : getObjects()){
            	TestRunSvtDaqMapping testRunDaqMapping = (TestRunSvtDaqMapping) daqMapping; 
            	buffer.append(testRunDaqMapping.getFpgaID());
                buffer.append("    ");
            	buffer.append(testRunDaqMapping.getHybridID());
                buffer.append("    ");
                buffer.append(testRunDaqMapping.getSvtHalf());
                buffer.append("    ");
                buffer.append(String.format("%-2d", testRunDaqMapping.getLayerNumber()));
                buffer.append("    ");
                buffer.append(testRunDaqMapping.getOrientation());
                buffer.append("    ");
                buffer.append('\n');
            }
        	return buffer.toString();
        }
	}
	
	public int getFpgaID(){
		return getFieldValue("fpga");
	}
	
	public int getHybridID(){
		return getFieldValue("hybrid");
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
