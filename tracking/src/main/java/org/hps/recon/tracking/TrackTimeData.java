package org.hps.recon.tracking;

import java.util.List;
import java.util.ArrayList;

import org.lcsim.event.GenericObject;

/**
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public class TrackTimeData implements GenericObject {

    List<Float> trackTimeData = new ArrayList<Float>();
    List<Double> t0Residuals = new ArrayList<Double>();
    List<Integer> layers = new ArrayList<Integer>();

    // Constants
    private final static int SVT_VOLUME_INDEX = 0;
    private final static int TRACK_TIME_INDEX = 1;

    /**
     * Default Ctor
     * 
     * @param trackTime : The mean t0 time of all hits of a track
     * @param trackerVolume : The SVT volume to which the track used to calculate the track time corresponds to.
     */
    public TrackTimeData(float trackerVolume, double trackTime, List<Integer> layers, List<Double> t0Residuals) {
        trackTimeData.add(trackerVolume);
        trackTimeData.add((float) trackTime);
        this.layers.addAll(layers);
        this.t0Residuals.addAll(t0Residuals);
    }

    /**
     * @param layer :
     * @param t0Residual :
     */
    private void addResidual(int layer, double t0Residual) {
        layers.add(layer);
        t0Residuals.add(t0Residual);
    }

    /**
     * 
     */
    public double getTrackTime() {
        return trackTimeData.get(TRACK_TIME_INDEX);
    }

    /**
     * 
     * 
     */
    public double getT0Residual(int layer) {
        return this.getDoubleVal(layer);
    }

    /**
     * 
     */
    public double getClusterTime(int layer) {
        return this.getTrackTime() - this.getT0Residual(layer);
    }

    /**
     * 
     * 
     */
    public boolean isTopSvtVolume() {
        return (trackTimeData.get(SVT_VOLUME_INDEX) == 0) ? true : false;
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
