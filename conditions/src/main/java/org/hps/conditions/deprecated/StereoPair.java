package org.hps.conditions.deprecated;

import org.lcsim.detector.tracker.silicon.SiSensor;

/**
 * A class describing an SVT stereo pair.
 * 
 * @author Per Hansson <phansson@slac.stanford.edu>
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id: StereoPair.java,v 1.3 2013/07/30 00:53:07 omoreno Exp $ 
 */
public class StereoPair {

	
	// Layer number to which the stereo pair belongs to
	private int layerNumber = -1; 
	
	// The axial and stereo sensors
	private SiSensor axialSensor = null; 
	private SiSensor stereoSensor = null;
	
	// The detector volume in which the stereo pair resides 
	public enum detectorVolume{ Top, Bottom }; 
	private detectorVolume volume = detectorVolume.Top; 
	
	/**
	 * Default Ctor
	 * 
	 * @param layerNumber : Layer number to which the stereo pair belongs to
	 */
	public StereoPair(int layerNumber){
		this.layerNumber = layerNumber; 
	}
	
	/**
	 * Ctor
	 * 
	 * @param layerNumber : Layer number to which the stereo pair belongs to
	 * @param volume : detector volume (detectorVolume.Top or detector Volume.Bottom) in 
	 * 					which the stereo pair resides 
	 * @param axialSensor 
	 * @param stereoSensor
	 */
	public StereoPair(int layerNumber, detectorVolume volume, SiSensor axialSensor, SiSensor stereoSensor){
		this.layerNumber = layerNumber; 
		this.volume = volume; 
		this.axialSensor = axialSensor; 
		this.stereoSensor = stereoSensor; 
	}

	/**
	 * Set the axial sensor of stereo pair
	 * 
	 * @param axialSensor 
	 */
	public void setAxialSensor(SiSensor axialSensor){
		this.axialSensor = axialSensor;
	}
	
	/**
	 * Set the stereo sensor of the stereo pair
	 * 
	 * @param stereoSensor
	 */
	public void setStereoSensor(SiSensor stereoSensor){
		this.stereoSensor = stereoSensor; 
	}
	
	/**
	 * Set the SVT volume in which the stereo pair resides
	 * 
	 * @param volume : detectorVolume.Top or detectorVolume.Bottom
	 */
	public void setDetectorVolume(detectorVolume volume){
		this.volume = volume; 
	}
	
	/**
	 * Get the axial sensor of the stereo pair
	 * 
	 * @return Axial sensor. Returns null if it hasn't been set yet.
	 */
	public SiSensor getAxialSensor(){
		return axialSensor; 
	}
	
	/**
	 * Get the stereo sensor of the stereo pair
	 * 
	 * @return Stereo sensor. Returns null if it hasn't been set yet.
	 */
	public SiSensor getStereoSensor(){
		return stereoSensor; 
	}

	/**
	 * Get the layer number to which the stereo pair belongs to
	 * 
	 * @return layer number
	 */
	public int getLayerNumber(){
		return layerNumber; 
	}
	
	/**
	 * Get the SVT volume in which the stereo pair resides
	 * 
	 * @return volume : detectorVolume.Top or detectorVolume.Bottom
	 */
	public detectorVolume getDetectorVolume(){
		return volume; 
	}
	
	/**
	 * Return a string describing the stereo pair
	 * 
	 * @return stereo pair description
	 */
	@Override
	public String toString(){
		String axialName = axialSensor == null ? "None" : axialSensor.getName();
		String stereoName = stereoSensor == null ? "None" : stereoSensor.getName();
		String description = "Stereo Pair:\n";
		description += "\tLayer Number: " + this.getLayerNumber();
		description += "\t\tAxial Sensor: " + axialName;
		description += "\t\tStereo Sensor: " + stereoName;
		return description; 
		
	}
}
