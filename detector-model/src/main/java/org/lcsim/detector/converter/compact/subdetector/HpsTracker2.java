package org.lcsim.detector.converter.compact.subdetector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.converter.compact.SubdetectorDetectorElement;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;

/**
 * Detector element for <code>HPSTracker2</code> type.
 */
public class HpsTracker2 extends SubdetectorDetectorElement {
    
    private boolean debug = false;
    
    // List of stereo layers composing the SVT
    List<SvtStereoLayer> stereoPairs = new ArrayList<SvtStereoLayer>(); 
    
    public HpsTracker2(String name, IDetectorElement parent) {
        super(name, parent);
    }
    
    /**
     * Get a collection of stereo pairs ({@link SvtStereoLayer}) composing the SVT.
     * 
     * @return List of stereo pairs
     */
    public List<SvtStereoLayer> getStereoPairs(){
        return Collections.unmodifiableList(stereoPairs);
    }

    /**
     * Get the collection of {@link HpsSiSensor} composing the SVT. 
     * 
     * @return List of sensors
     */
    public List<HpsSiSensor> getSensors(){
        List<HpsSiSensor> list = this.findDescendants(HpsSiSensor.class);
        if(debug) {
            System.out.printf("%s: found %d HpsSiSensors\n",getClass().getSimpleName(), list.size());
            System.out.printf("%s: %45s %5s %5s\n",getClass().getSimpleName(), "<name>", "<layerID>", "<moduleID>");
            for(HpsSiSensor sensor : list) {
                System.out.printf("%s: %45s %5d %5d\n",getClass().getSimpleName(), sensor.getName(), sensor.getLayerNumber(), sensor.getModuleNumber());
            }
        }
        return list;
    }
    
    /**
     * Get a {@link HpsSiSensor} by layer and module number.
     * 
     * @param layer The SVT layer number
     * @param module The SVT module number
     * @return Corresponding sensor
     */
    public HpsSiSensor getSensor(int layer, int module){
        for(HpsSiSensor sensor : this.getSensors()){
            if(sensor.getLayerNumber() == layer && sensor.getModuleNumber() == module) 
                return sensor; 
        }
        return null;
    }
    
    /**
     * Get the maximum layer number present in the collection of {@link HpsSiSensor}.
     * 
     * @return maximum layer number
     */
    private int getMaxLayerNumber(){
        int maxLayerNumber = 0;
        for(HpsSiSensor sensor : this.getSensors()){
           if(sensor.getLayerNumber() > maxLayerNumber) maxLayerNumber = sensor.getLayerNumber();
        }
        return maxLayerNumber; 
    }

    /**
     * Get the maximum module number present in the collection of {@link HpsSiSensor}.
     * 
     * @return maximum module number
     */
    private int getMaxModuleNumber(){
        int maxModuleID = 0; 
        for(HpsSiSensor sensor : this.getSensors()){
           if(sensor.getModuleNumber() > maxModuleID) maxModuleID = sensor.getModuleNumber();
        }
        return maxModuleID; 
    }
    
    /**
     * Method that loops through the collection of {@link HpsSiSensor} and creates 
     * stereo layers. A stereo layer is composed of two adjacent sensors (stereo and axial)
     * with the same module number.
     */
    public void createStereoLayers(){

        //System.out.printf("%s: create stereo layers\n",getClass().getSimpleName());
        
        HpsSiSensor firstSensor = null;
        HpsSiSensor secondSensor = null;

        //System.out.printf("%s: %10s %10s %42s %42s\n",getClass().getSimpleName(), "layerID/moduleID", "layerID/moduleID", "sensor1", "sensor2");

        
        for(int layerID = 1; layerID <= this.getMaxLayerNumber(); layerID+=2 ){
            for(int moduleID = 0; moduleID <= this.getMaxModuleNumber(); moduleID++){
    
                firstSensor = this.getSensor(layerID, moduleID); 
                secondSensor = this.getSensor(layerID+1, moduleID);

                //System.out.printf("%s: %10d/%d %10d/%d %42s %42s\n",getClass().getSimpleName(), 
//                        layerID,moduleID, layerID+1, moduleID, 
//                        firstSensor==null?"-":firstSensor.getName(), 
//                        secondSensor==null?"-":secondSensor.getName());
                
                if(firstSensor == null || secondSensor == null) {
                    continue; 
                }
               
                stereoPairs.add(new SvtStereoLayer((layerID+1)/2, firstSensor, secondSensor));
            }
        }
    }
}
