package org.hps.recon.tracking;

import java.util.List;
import java.util.ArrayList;

import org.lcsim.event.GenericObject;

/**
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @author PF <pbutti@slac.stanford.edu>
 * @version $Id$
 * 
 */
public class TrackResidualsData implements GenericObject {
    
    List<Double> residuals = new ArrayList<Double>();
    List<Float>  sigmas    = new ArrayList<Float>();
    List<Integer> layers = new ArrayList<Integer>();
    
    /**
     * Default Ctor
     * 
     * @param trackerVolume : The SVT volume to which the track used to calculate
     *                        the residuals corresponds to.
     */
    public TrackResidualsData(int trackerVolume, List<Integer> layers, List<Double> residuals, List<Float> sigmas){
        this.layers.addAll(layers);
        this.layers.add(trackerVolume);
        this.residuals.addAll(residuals);
        this.sigmas.addAll(sigmas);
    }

    /**
     * 
     */
    @Override
    public double getDoubleVal(int index) {
        return residuals.get(index);
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

    /**
     * 
     */
    @Override
    public int getNDouble() {
        return residuals.size();
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
