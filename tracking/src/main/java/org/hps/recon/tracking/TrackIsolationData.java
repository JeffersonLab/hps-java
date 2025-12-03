package org.hps.recon.tracking;

import java.util.List;
import java.util.ArrayList;

import org.lcsim.event.GenericObject;

public class TrackIsolationData implements GenericObject {
    
    List<Double> isolations = new ArrayList<Double>();
    List<Float>  isot0    = new ArrayList<Float>();
    List<Integer> layers = new ArrayList<Integer>();
    
    /**
     * Default Ctor
     * 
     * @param trackerVolume : The SVT volume to which the track used to calculate
     *                        the residuals corresponds to.
     */
    public TrackIsolationData(int trackerVolume, List<Integer> layers, List<Double> isolations, List<Float> isot0){
        this.layers.addAll(layers);
        this.layers.add(trackerVolume);
        this.isolations.addAll(isolations);
        this.isot0.addAll(isot0);
    }

    /**
     * 
     */
    @Override
    public double getDoubleVal(int index) {
        return isolations.get(index);
    }
    
    /**
     * 
     */
    @Override
    public float getFloatVal(int index) {
        return isot0.get(index);
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
        return isolations.size();
    }

    /**
     * 
     */
    @Override
    public int getNFloat() {
        return isot0.size();
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
