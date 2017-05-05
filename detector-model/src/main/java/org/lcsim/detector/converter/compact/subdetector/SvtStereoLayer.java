package org.lcsim.detector.converter.compact.subdetector;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;

/**
 * A class describing an SVT stereo pair. 
 */
public class SvtStereoLayer {

    // Layer number to which the stereo pair belongs to
    private int stereoLayerNumber = 0; 
    
    // The axial and stereo sensors
    private HpsSiSensor axialSensor = null; 
    private HpsSiSensor stereoSensor = null;
        
    /**
     * Class constructor.
     * 
     * @param stereoLayerNumber  Layer number to which the stereo pair belongs
     * @param firstSensor  The first sensor in the stereo layer 
     * @param secondSensor The second sensor in the stereo layer
     */
    public SvtStereoLayer(int stereoLayerNumber, HpsSiSensor firstSensor, HpsSiSensor secondSensor){
        this.stereoLayerNumber = stereoLayerNumber;
        if(firstSensor.isAxial()){
            this.axialSensor = firstSensor; 
            this.stereoSensor = secondSensor; 
        } else { 
            this.axialSensor = secondSensor; 
            this.stereoSensor = firstSensor; 
        }
    }
    
    
    /**
     * Get the axial sensor of the stereo pair
     * 
     * @return Axial sensor. Returns null if it hasn't been set yet.
     */
    public HpsSiSensor getAxialSensor(){
        return axialSensor; 
    }
    
    /**
     * Get the stereo sensor of the stereo pair
     * 
     * @return Stereo sensor. Returns null if it hasn't been set yet.
     */
    public HpsSiSensor getStereoSensor(){
        return stereoSensor; 
    }

    /**
     * Get the layer number to which the stereo pair belongs to.
     * 
     * @return stereo layer number
     */
    public int getLayerNumber(){
        return stereoLayerNumber; 
    }
    
    /**
     * Return a string describing the stereo pair
     * 
     * @return stereo pair description
     */
    @Override
    public String toString(){
        StringBuffer buffer = new StringBuffer();
        buffer.append("[ Stereo Pair ]: Layer number: " + this.getLayerNumber() + "\n");
        buffer.append("\t\tAxial Sensor: ");
        buffer.append(axialSensor == null ? "None" : axialSensor.getName());
        buffer.append("\tStereo Sensor: ");
        buffer.append(stereoSensor == null ? "None" : stereoSensor.getName());
        return buffer.toString(); 
    }

}

