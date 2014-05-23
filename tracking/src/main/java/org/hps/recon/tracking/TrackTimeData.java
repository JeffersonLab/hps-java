package org.hps.recon.tracking;

import java.util.List;
import java.util.ArrayList;

import org.lcsim.event.GenericObject;

/**
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id$
 *
 */
public class TrackTimeData implements GenericObject {

	List<Float> trackTimeData = new ArrayList<Float>(); 
	List<Double> t0Residuals = new ArrayList<Double>(); 
	List<Integer> layers = new ArrayList<Integer>(); 
	
	
	/**
	 * Default Ctor
	 * 
	 * @param trackTime : The mean t0 time of all hits of a track
	 * @param trackerVolume : The SVT volume to which the track used to calculate
	 * 						  the track time corresponds to.
	 *
	 */
	public TrackTimeData(float trackerVolume, double trackTime, List<Integer> layers, List<Double> t0Residuals){
		trackTimeData.add(trackerVolume);
		trackTimeData.add((float) trackTime);
		this.layers.addAll(layers);
		this.t0Residuals.addAll(t0Residuals);
	}
	
	/**
	 *	 
	 * 
	 * @param layer : 
	 * @param t0Residual : 
	 * 
	 */
	public void addResidual(int layer, double t0Residual){
		layers.add(layer); 
		t0Residuals.add(t0Residual);
	}

	/**
	 * 
	 */
	@Override
	public double getDoubleVal(int index) {
		return t0Residuals.get(index);
	}

	/**
	 * 
	 */
	@Override
	public float getFloatVal(int index) {
		return trackTimeData.get(index);
	}

	/**
	 * 
	 */
	@Override
	public int getIntVal(int index) {
		return layers.get(index);
	}

	/**
	 * 
	 */
	@Override
	public int getNDouble() {
		return t0Residuals.size();
	}

	/**
	 * 
	 */
	@Override
	public int getNFloat() {
		return trackTimeData.size();
	}

	/**
	 * 
	 */
	@Override
	public int getNInt() {
		return layers.size();
	}

	/**
	 * 
	 */
	@Override
	public boolean isFixedSize() {
		return false;
	}
}
