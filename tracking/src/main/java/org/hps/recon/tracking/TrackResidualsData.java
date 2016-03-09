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
public class TrackResidualsData implements GenericObject {

    List<Double> trackResidualsX = new ArrayList<Double>();
    List<Float> trackResidualsY = new ArrayList<Float>();
    List<Integer> layers = new ArrayList<Integer>();
    
    /**
     * Default Ctor
     * 
     * @param trackerVolume : The SVT volume to which the track used to calculate
     *                        the residuals corresponds to.
     */
    public TrackResidualsData(int trackerVolume, List<Integer> layers, List<Double> trackResidualsX, List<Float> trackResidualsY){
        this.layers.addAll(layers);
        this.layers.add(trackerVolume);
        this.trackResidualsX.addAll(trackResidualsX);
        this.trackResidualsY.addAll(trackResidualsY);
    }

    /**
     * 
     * @return tracker volume : 0 if top 1 if bottom
     */
    public int getTrackerVolume(){
        return layers.get(layers.size() - 1);
    }
    
    /**
     * 
     */
    @Override
    public double getDoubleVal(int index) {
        return trackResidualsX.get(index);
    }

    /**
     * 
     */
    @Override
    public float getFloatVal(int index) {
        return trackResidualsY.get(index);
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
        return trackResidualsX.size();
    }

    /**
     * 
     */
    @Override
    public int getNFloat() {
        return trackResidualsY.size();
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
