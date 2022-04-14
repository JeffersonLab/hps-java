package org.hps.recon.tracking;

import java.util.List;
import java.util.ArrayList;

import org.lcsim.event.GenericObject;

public class TrackIntersectData implements GenericObject {
    
    List<Double> intersects = new ArrayList<Double>();
    List<Float>  sigmas    = new ArrayList<Float>();
    List<Integer> layers = new ArrayList<Integer>();
    
    /**
     * Default Ctor
     * 
     * @param trackerVolume : The SVT volume to which the track used to calculate
     *                        the residuals corresponds to.
     */
    public TrackIntersectData(int trackerVolume, List<Integer> layers, List<Double> intersects, List<Float> sigmas){
        this.layers.addAll(layers);
        this.layers.add(trackerVolume);
        this.intersects.addAll(intersects);
        this.sigmas.addAll(sigmas);
    }

    /**
     * 
     */
    @Override
    public double getDoubleVal(int index) {
        return intersects.get(index);
    }
    
    /**
     * 
     */
    @Override
    public float getFloatVal(int index) {
        return sigmas.get(index);
    }
    
    /**
     * 
     */
    @Override
    public int getIntVal(int index) {
        return layers.get(index);
    }


    public int getLayer(int index){
        return layers.get(index);
    }

    public float getSigma(int index) {
        return sigmas.get(index);
    }

    public Double[] getIntersect(int index) {        
        return new Double[]{intersects.get(3*index),intersects.get(3*index+1),intersects.get(3*index+2)};
    }


    /**
     * 
     */
    @Override
    public int getNDouble() {
        return intersects.size();
    }

    /**
     * 
     */
    @Override
    public int getNFloat() {
        return sigmas.size();
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
